package kickbase.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import skip.lib.*
import skip.lib.Array

import skip.model.*
import skip.foundation.*
import skip.ui.*

@Stable
internal open class LeagueTableViewModel: ObservableObject {
    override val objectWillChange = ObservableObjectPublisher()
    // MARK: - Enums
    internal enum class TableType {
        overall,
        matchday;
    }

    // MARK: - Published Properties
    internal open var tableType: LeagueTableViewModel.TableType
        get() = _tableType.wrappedValue
        set(newValue) {
            objectWillChange.send()
            _tableType.wrappedValue = newValue
        }
    internal var _tableType: skip.model.Published<LeagueTableViewModel.TableType> = skip.model.Published(LeagueTableViewModel.TableType.overall)
    internal open var selectedMatchDay: Int
        get() = _selectedMatchDay.wrappedValue
        set(newValue) {
            val oldValue = this.selectedMatchDay
            objectWillChange.send()
            _selectedMatchDay.wrappedValue = newValue
            if (!suppresssideeffects) {
                print("🔄 ViewModel: selectedMatchDay setter called: oldValue=${oldValue}, newValue=${selectedMatchDay}")
                // Mark that user selected this explicitly if value changed
                if (selectedMatchDay != oldValue) {
                    userExplicitlySelectedMatchDay = true
                }
                // Automatically reload when matchday changes
                if (tableType == LeagueTableViewModel.TableType.matchday && selectedMatchDay != oldValue) {
                    print("🔄 ViewModel: selectedMatchDay changed from ${oldValue} to ${selectedMatchDay}, triggering reload")
                    Task { -> loadMatchDayRanking(matchDay = MainActor.run { selectedMatchDay }) }
                }
            }
        }
    internal var _selectedMatchDay: skip.model.Published<Int> = skip.model.Published(1)
    internal open var displayedUsers: Array<LeagueUser>
        get() = _displayedUsers.wrappedValue.sref({ this.displayedUsers = it })
        set(newValue) {
            objectWillChange.send()
            _displayedUsers.wrappedValue = newValue.sref()
        }
    internal var _displayedUsers: skip.model.Published<Array<LeagueUser>> = skip.model.Published(arrayOf())
    internal open var isLoading: Boolean
        get() = _isLoading.wrappedValue
        set(newValue) {
            objectWillChange.send()
            _isLoading.wrappedValue = newValue
        }
    internal var _isLoading: skip.model.Published<Boolean> = skip.model.Published(false)

    // Track if user has manually selected a matchday
    private var userExplicitlySelectedMatchDay = false
    private var lastSelectedLeagueId: String = ""

    // MARK: - Dependencies
    private var kickbaseManager: KickbaseManager? = null
    private var cancellables: Array<AnyCancellable> = arrayOf()
        get() = field.sref({ this.cancellables = it })
        set(newValue) {
            field = newValue.sref()
        }

    // MARK: - Init
    internal constructor(kickbaseManager: KickbaseManager? = null) {
        suppresssideeffects = true
        try {
            this.kickbaseManager = kickbaseManager
        } finally {
            suppresssideeffects = false
        }
    }

    // MARK: - Public Methods
    internal open fun setKickbaseManager(manager: KickbaseManager) {
        this.kickbaseManager = manager

        // Subscribe to KickbaseManager changes
        cancellables.removeAll()

        // Observe selectedLeague changes - fetch smdc when league changes
        manager._selectedLeague.projectedValue
            .sink { selectedLeague ->
                selectedLeague?.let { league ->
                    print("🔄 ViewModel: selectedLeague changed to ${league.name}, fetching current matchday via smdc")

                    // Check if this is a new league (not just a navigation return)
                    if (this?.lastSelectedLeagueId != league.id) {
                        print("🔄 ViewModel: Different league detected, resetting user selection flag")
                        this?.userExplicitlySelectedMatchDay = false
                        this?.lastSelectedLeagueId = league.id
                    }

                    // Fetch current matchday from API using smdc field
                    Task { ->
                        val matchtarget_0 = this?.fetchCurrentMatchDay(leagueId = league.id)
                        if (matchtarget_0 != null) {
                            val currentMatchDay = matchtarget_0
                            print("📅 ViewModel: Current matchday from API is ${currentMatchDay}")

                            // Only reset selectedMatchDay if user hasn't explicitly selected one yet for this league
                            if (!(this?.userExplicitlySelectedMatchDay ?: false)) {
                                print("📅 ViewModel: User hasn't selected matchday, setting to current ${currentMatchDay}")
                                this?.selectedMatchDay = currentMatchDay
                            } else {
                                print("📅 ViewModel: User has selected matchday, keeping selection ${this?.selectedMatchDay ?: 0}")
                            }
                        } else {
                            print("⚠️ ViewModel: Could not fetch smdc, keeping default")
                        }
                    }
                }
            }
            .store(in_ = InOut({ cancellables }, { cancellables = it }))

        // Observe leagueUsers changes
        manager._leagueUsers.projectedValue
            .sink { users ->
                if (this?.tableType == LeagueTableViewModel.TableType.overall) {
                    this?.displayedUsers = users
                }
            }
            .store(in_ = InOut({ cancellables }, { cancellables = it }))

        // Observe matchDayUsers changes
        manager._matchDayUsers.projectedValue
            .sink { users ->
                if (this?.tableType == LeagueTableViewModel.TableType.matchday) {
                    this?.displayedUsers = users
                }
            }
            .store(in_ = InOut({ cancellables }, { cancellables = it }))

        // Observe isLoading changes
        manager._isLoading.projectedValue
            .sink { isLoading ->
                this?.isLoading = isLoading
            }
            .store(in_ = InOut({ cancellables }, { cancellables = it }))
    }

    // MARK: - Private Methods

    /// Fetches the current matchday via smdc from a player in the league
    private suspend fun fetchCurrentMatchDay(leagueId: String): Int? = MainActor.run l@{
        // Try to get a player ID to fetch smdc from
        kickbaseManager?.teamPlayers.sref()?.let { teamPlayers ->
            if (!teamPlayers.isEmpty) {
                teamPlayers.first?.id?.let { playerId ->
                    return@l kickbaseManager?.authenticatedPlayerService?.getCurrentMatchDay(leagueId = leagueId, playerId = playerId)
                }
            }
        }

        // Fallback: try market players
        kickbaseManager?.marketPlayers.sref()?.let { marketPlayers ->
            if (!marketPlayers.isEmpty) {
                marketPlayers.first?.id?.let { playerId ->
                    return@l kickbaseManager?.authenticatedPlayerService?.getCurrentMatchDay(leagueId = leagueId, playerId = playerId)
                }
            }
        }

        print("⚠️ ViewModel: No players available to fetch smdc")
        return@l null
    }

    // MARK: - Computed Properties
    internal open val selectedLeague: League?
        get() {
            return kickbaseManager?.selectedLeague
        }

    // MARK: - Public Methods

    /// Handle switching between table types
    internal open suspend fun switchTableType(to: LeagueTableViewModel.TableType): Unit = MainActor.run l@{
        val newType = to
        tableType = newType

        // Update displayed users based on new table type
        if (newType == LeagueTableViewModel.TableType.overall) {
            displayedUsers = kickbaseManager?.leagueUsers ?: arrayOf()
            userExplicitlySelectedMatchDay = false // Reset when switching to overall
        } else {
            displayedUsers = kickbaseManager?.matchDayUsers ?: arrayOf()
        }
        val manager_0 = kickbaseManager
        if ((manager_0 == null) || (manager_0.selectedLeague == null)) {
            return@l
        }

        if (newType == LeagueTableViewModel.TableType.matchday) {
            loadMatchDayRanking(matchDay = selectedMatchDay)
        }
    }

    /// Handle matchday selection
    internal open suspend fun selectMatchDay(day: Int): Unit = MainActor.run l@{
        if (day == selectedMatchDay) {
            return@l
        }

        userExplicitlySelectedMatchDay = true
        selectedMatchDay = day
        print("🔄 LeagueTableViewModel: User selected matchday ${day}")
        if (kickbaseManager == null) {
            return@l
        }

        loadMatchDayRanking(matchDay = day)
    }

    /// Load overall ranking
    internal open suspend fun loadOverallRanking(): Unit = MainActor.run l@{
        val manager_1 = kickbaseManager
        if (manager_1 == null) {
            return@l
        }
        val league_0 = manager_1.selectedLeague
        if (league_0 == null) {
            return@l
        }

        manager_1.loadLeagueRanking(for_ = league_0)
    }

    /// Load matchday-specific ranking
    private suspend fun loadMatchDayRanking(matchDay: Int): Unit = MainActor.run l@{
        val manager_2 = kickbaseManager
        if (manager_2 == null) {
            return@l
        }
        val league_1 = manager_2.selectedLeague
        if (league_1 == null) {
            return@l
        }

        print("📡 LeagueTableViewModel: Loading matchday ${matchDay}")
        manager_2.loadMatchDayRanking(for_ = league_1, matchDay = matchDay)
    }

    /// Refresh current data
    internal open suspend fun refresh(): Unit = MainActor.run {
        if (tableType == LeagueTableViewModel.TableType.overall) {
            loadOverallRanking()
        } else {
            loadMatchDayRanking(matchDay = selectedMatchDay)
        }
    }

    private var suppresssideeffects = false
}
