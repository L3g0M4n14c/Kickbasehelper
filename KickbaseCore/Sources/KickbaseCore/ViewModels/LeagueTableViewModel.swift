import Combine
import Foundation
import SwiftUI

@MainActor
class LeagueTableViewModel: ObservableObject {
    // MARK: - Enums
    enum TableType {
        case overall
        case matchday
    }

    // MARK: - Published Properties
    @Published var tableType: TableType = .overall
    @Published var selectedMatchDay: Int = 1 {
        didSet {
            print(
                "🔄 ViewModel: selectedMatchDay setter called: oldValue=\(oldValue), newValue=\(selectedMatchDay)"
            )
            // Mark that user selected this explicitly if value changed
            if selectedMatchDay != oldValue {
                userExplicitlySelectedMatchDay = true
            }
            // Automatically reload when matchday changes
            if tableType == .matchday && selectedMatchDay != oldValue {
                print(
                    "🔄 ViewModel: selectedMatchDay changed from \(oldValue) to \(selectedMatchDay), triggering reload"
                )
                Task {
                    await loadMatchDayRanking(matchDay: selectedMatchDay)
                }
            }
        }
    }
    @Published var displayedUsers: [LeagueUser] = []
    @Published var isLoading = false

    // Track if user has manually selected a matchday
    private var userExplicitlySelectedMatchDay = false
    private var lastSelectedLeagueId: String = ""

    // MARK: - Dependencies
    private var kickbaseManager: KickbaseManager?
    private var cancellables: Set<AnyCancellable> = []

    // MARK: - Init
    init(kickbaseManager: KickbaseManager? = nil) {
        self.kickbaseManager = kickbaseManager
    }

    // MARK: - Public Methods
    func setKickbaseManager(_ manager: KickbaseManager) {
        self.kickbaseManager = manager

        // Subscribe to KickbaseManager changes
        cancellables.removeAll()

        // Observe selectedLeague changes - fetch smdc when league changes
        manager.$selectedLeague
            .sink { [weak self] selectedLeague in
                if let league = selectedLeague {
                    print(
                        "🔄 ViewModel: selectedLeague changed to \(league.name), fetching current matchday via smdc"
                    )

                    // Check if this is a new league (not just a navigation return)
                    if self?.lastSelectedLeagueId != league.id {
                        print(
                            "🔄 ViewModel: Different league detected, resetting user selection flag")
                        self?.userExplicitlySelectedMatchDay = false
                        self?.lastSelectedLeagueId = league.id
                    }

                    // Fetch current matchday from API using smdc field
                    Task {
                        if let currentMatchDay = await self?.fetchCurrentMatchDay(
                            leagueId: league.id)
                        {
                            print("📅 ViewModel: Current matchday from API is \(currentMatchDay)")

                            // Only reset selectedMatchDay if user hasn't explicitly selected one yet for this league
                            if !(self?.userExplicitlySelectedMatchDay ?? false) {
                                print(
                                    "📅 ViewModel: User hasn't selected matchday, setting to current \(currentMatchDay)"
                                )
                                self?.selectedMatchDay = currentMatchDay
                            } else {
                                print(
                                    "📅 ViewModel: User has selected matchday, keeping selection \(self?.selectedMatchDay ?? 0)"
                                )
                            }
                        } else {
                            print("⚠️ ViewModel: Could not fetch smdc, keeping default")
                        }
                    }
                }
            }
            .store(in: &cancellables)

        // Observe leagueUsers changes
        let c2 = manager.$leagueUsers
            .sink { [weak self] users in
                if self?.tableType == .overall {
                    self?.displayedUsers = users
                }
            }
        manager.$leagueUsers
            .sink { [weak self] users in
                if self?.tableType == .overall {
                    self?.displayedUsers = users
                }
            }
            .store(in: &cancellables)

        // Observe matchDayUsers changes
        let c3 = manager.$matchDayUsers
            .sink { [weak self] users in
                if self?.tableType == .matchday {
                    self?.displayedUsers = users
                }
            }
        manager.$matchDayUsers
            .sink { [weak self] users in
                if self?.tableType == .matchday {
                    self?.displayedUsers = users
                }
            }
            .store(in: &cancellables)

        // Observe isLoading changes
        let c4 = manager.$isLoading
            .sink { [weak self] isLoading in
                self?.isLoading = isLoading
            }
        manager.$isLoading
            .sink { [weak self] isLoading in
                self?.isLoading = isLoading
            }
            .store(in: &cancellables)
    }

    // MARK: - Private Methods

    /// Fetches the current matchday via smdc from a player in the league
    private func fetchCurrentMatchDay(leagueId: String) async -> Int? {
        // Try to get a player ID to fetch smdc from
        if let teamPlayers = kickbaseManager?.teamPlayers, !teamPlayers.isEmpty {
            if let playerId = teamPlayers.first?.id {
                return await kickbaseManager?.authenticatedPlayerService.getCurrentMatchDay(
                    leagueId: leagueId,
                    playerId: playerId
                )
            }
        }

        // Fallback: try market players
        if let marketPlayers = kickbaseManager?.marketPlayers, !marketPlayers.isEmpty {
            if let playerId = marketPlayers.first?.id {
                return await kickbaseManager?.authenticatedPlayerService.getCurrentMatchDay(
                    leagueId: leagueId,
                    playerId: playerId
                )
            }
        }

        print("⚠️ ViewModel: No players available to fetch smdc")
        return nil
    }

    // MARK: - Computed Properties
    var selectedLeague: League? {
        kickbaseManager?.selectedLeague
    }

    // MARK: - Public Methods

    /// Handle switching between table types
    func switchTableType(to newType: TableType) async {
        tableType = newType

        // Update displayed users based on new table type
        if newType == .overall {
            displayedUsers = kickbaseManager?.leagueUsers ?? []
            userExplicitlySelectedMatchDay = false  // Reset when switching to overall
        } else {
            displayedUsers = kickbaseManager?.matchDayUsers ?? []
        }

        guard let manager = kickbaseManager,
            manager.selectedLeague != nil
        else {
            return
        }

        if newType == .matchday {
            await loadMatchDayRanking(matchDay: selectedMatchDay)
        }
    }

    /// Handle matchday selection
    func selectMatchDay(_ day: Int) async {
        guard day != selectedMatchDay else { return }

        userExplicitlySelectedMatchDay = true
        selectedMatchDay = day
        print("🔄 LeagueTableViewModel: User selected matchday \(day)")

        guard kickbaseManager != nil else {
            return
        }

        await loadMatchDayRanking(matchDay: day)
    }

    /// Load overall ranking
    func loadOverallRanking() async {
        guard let manager = kickbaseManager,
            let league = manager.selectedLeague
        else {
            return
        }

        await manager.loadLeagueRanking(for: league)
    }

    /// Load matchday-specific ranking
    private func loadMatchDayRanking(matchDay: Int) async {
        guard let manager = kickbaseManager,
            let league = manager.selectedLeague
        else {
            return
        }

        print("📡 LeagueTableViewModel: Loading matchday \(matchDay)")
        await manager.loadMatchDayRanking(for: league, matchDay: matchDay)
    }

    /// Refresh current data
    func refresh() async {
        if tableType == .overall {
            await loadOverallRanking()
        } else {
            await loadMatchDayRanking(matchDay: selectedMatchDay)
        }
    }
}
