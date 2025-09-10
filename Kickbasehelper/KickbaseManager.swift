import Foundation
import SwiftUI

@MainActor
class KickbaseManager: ObservableObject {
    // Published Properties für UI State
    @Published var leagues: [League] = []
    @Published var selectedLeague: League?
    @Published var teamPlayers: [TeamPlayer] = []
    @Published var marketPlayers: [MarketPlayer] = []
    @Published var userStats: UserStats?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    // Services
    private let apiClient: KickbaseAPIClient
    private let dataParser: KickbaseDataParser
    private let leagueService: KickbaseLeagueService
    private let playerService: KickbasePlayerService
    private let userStatsService: KickbaseUserStatsService
    
    init() {
        self.apiClient = KickbaseAPIClient()
        self.dataParser = KickbaseDataParser()
        self.leagueService = KickbaseLeagueService(apiClient: apiClient, dataParser: dataParser)
        self.playerService = KickbasePlayerService(apiClient: apiClient, dataParser: dataParser)
        self.userStatsService = KickbaseUserStatsService(apiClient: apiClient, dataParser: dataParser)
    }
    
    // MARK: - Authentication
    
    func setAuthToken(_ token: String) {
        apiClient.setAuthToken(token)
        print("🔑 Auth token set for KickbaseManager")
    }
    
    // MARK: - Data Loading Coordination
    
    func loadUserData() async {
        print("📊 Loading user data...")
        await loadLeagues()
        
        // Forciere das Laden der UserStats nach Liga-Auswahl
        if let selectedLeague = selectedLeague {
            print("🔄 Force reloading user stats after league selection...")
            await loadUserStats(for: selectedLeague)
        }
    }
    
    func loadLeagues() async {
        isLoading = true
        errorMessage = nil
        
        do {
            let loadedLeagues = try await leagueService.loadLeagues()
            self.leagues = loadedLeagues
            
            // Wähle automatisch die erste Liga aus, wenn noch keine ausgewählt ist
            if selectedLeague == nil && !leagues.isEmpty {
                selectedLeague = leagues.first
            }
            
            print("✅ Loaded \(leagues.count) leagues")
        } catch {
            print("❌ Error loading leagues: \(error)")
            errorMessage = "Fehler beim Laden der Ligen: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    func loadTeamPlayers(for league: League) async {
        isLoading = true
        errorMessage = nil
        
        do {
            let players = try await playerService.loadTeamPlayers(for: league)
            self.teamPlayers = players
            print("✅ Loaded \(players.count) team players")
        } catch {
            print("❌ Error loading team players: \(error)")
            errorMessage = "Fehler beim Laden der Team-Spieler: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    func loadMarketPlayers(for league: League) async {
        isLoading = true
        errorMessage = nil
        
        do {
            let players = try await playerService.loadMarketPlayers(for: league)
            self.marketPlayers = players
            print("✅ Loaded \(players.count) market players")
        } catch {
            print("❌ Error loading market players: \(error)")
            errorMessage = "Fehler beim Laden der Markt-Spieler: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    func loadUserStats(for league: League) async {
        isLoading = true
        errorMessage = nil
        
        do {
            let stats = try await userStatsService.loadUserStats(for: league)
            self.userStats = stats
            print("✅ Loaded user stats")
        } catch {
            print("❌ Error loading user stats: \(error)")
            errorMessage = "Fehler beim Laden der Benutzerstatistiken: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    // MARK: - League Selection
    
    func selectLeague(_ league: League) {
        selectedLeague = league
        
        // Lade Daten für die neue Liga
        Task {
            await loadTeamPlayers(for: league)
            await loadMarketPlayers(for: league)
            await loadUserStats(for: league)
        }
    }
    
    // MARK: - Player Market Value History
    
    func loadPlayerMarketValueHistory(playerId: String, leagueId: String) async -> MarketValueChange? {
        print("📈 Loading market value history for player \(playerId) in league \(leagueId)")
        
        do {
            let history = await playerService.loadPlayerMarketValueHistory(playerId: playerId, leagueId: leagueId)
            if let history = history {
                print("✅ Successfully loaded market value history with \(history.dailyChanges.count) daily changes")
            } else {
                print("⚠️ No market value history returned from player service")
            }
            return history
        } catch {
            print("❌ Error loading player market value history: \(error)")
            return nil
        }
    }
    
    func loadPlayerMarketValueOnDemand(playerId: String, leagueId: String) async -> Int? {
        print("💰 Loading on-demand market value for player \(playerId) in league \(leagueId)")
        
        do {
            let profit = await playerService.loadPlayerMarketValueOnDemand(playerId: playerId, leagueId: leagueId)
            if let profit = profit {
                print("✅ Successfully loaded on-demand profit: €\(profit)")
            } else {
                print("⚠️ No profit value returned from player service")
            }
            return profit
        } catch {
            print("❌ Error loading on-demand market value: \(error)")
            return nil
        }
    }
}
