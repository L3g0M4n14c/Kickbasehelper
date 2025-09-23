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
    
    // MARK: - Public Service Access
    
    var authenticatedPlayerService: KickbasePlayerService {
        return playerService
    }
    
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
    
    // MARK: - Player Performance with Team Info
    
    func loadPlayerPerformanceWithTeamInfo(playerId: String, leagueId: String) async throws -> [EnhancedMatchPerformance]? {
        print("📊 Loading enhanced player performance with team info for player \(playerId)")
        
        do {
            let enhancedMatches = try await playerService.loadPlayerPerformanceWithTeamInfo(playerId: playerId, leagueId: leagueId)
            
            if let enhancedMatches = enhancedMatches {
                print("✅ Successfully loaded \(enhancedMatches.count) enhanced matches with team info")
            } else {
                print("⚠️ No enhanced performance data returned")
            }
            
            return enhancedMatches
        } catch {
            print("❌ Error loading enhanced player performance: \(error)")
            throw error
        }
    }
    
    func loadPlayerRecentPerformanceWithTeamInfo(playerId: String, leagueId: String) async -> [EnhancedMatchPerformance]? {
        print("📊 Loading recent enhanced player performance (last 5 match days) for player \(playerId)")
        
        do {
            guard let allEnhancedMatches = try await loadPlayerPerformanceWithTeamInfo(playerId: playerId, leagueId: leagueId) else {
                print("⚠️ No enhanced performance data available for filtering recent matches")
                return nil
            }
            
            // Finde den aktuellen Spieltag (cur = true)
            guard let currentMatch = allEnhancedMatches.first(where: { $0.isCurrent }) else {
                print("⚠️ No current match day found (cur = true)")
                return allEnhancedMatches
            }
            
            let currentMatchDay = currentMatch.matchDay
            print("🎯 Found current match day: \(currentMatchDay)")
            
            // Filtere die letzten 5 Spieltage (inklusive aktueller)
            let recentMatches = allEnhancedMatches.filter { match in
                match.matchDay <= currentMatchDay && match.matchDay > (currentMatchDay - 5)
            }.sorted { $0.matchDay < $1.matchDay }
            
            print("✅ Filtered to \(recentMatches.count) recent enhanced matches (days \(recentMatches.first?.matchDay ?? 0) - \(currentMatchDay))")
            return recentMatches
        } catch {
            print("❌ Error loading recent performance with team info: \(error)")
            return nil
        }
    }
    
    func loadPlayerUpcomingPerformanceWithTeamInfo(playerId: String, leagueId: String) async -> [EnhancedMatchPerformance]? {
        print("📊 Loading upcoming enhanced player performance (next 3 match days) for player \(playerId)")
        
        do {
            guard let allEnhancedMatches = try await loadPlayerPerformanceWithTeamInfo(playerId: playerId, leagueId: leagueId) else {
                print("⚠️ No enhanced performance data available for filtering upcoming matches")
                return nil
            }
            
            // Finde den aktuellen Spieltag (cur = true)
            guard let currentMatch = allEnhancedMatches.first(where: { $0.isCurrent }) else {
                print("⚠️ No current match day found (cur = true)")
                return allEnhancedMatches
            }
            
            let currentMatchDay = currentMatch.matchDay
            print("🎯 Found current match day: \(currentMatchDay)")
            
            // Filtere die nächsten 3 Spieltage (nach dem aktuellen)
            let upcomingMatches = allEnhancedMatches.filter { match in
                match.matchDay > currentMatchDay && match.matchDay <= (currentMatchDay + 3)
            }.sorted { $0.matchDay < $1.matchDay }
            
            print("✅ Filtered to \(upcomingMatches.count) upcoming enhanced matches (days \(currentMatchDay + 1) - \(currentMatchDay + 3))")
            return upcomingMatches
        } catch {
            print("❌ Error loading upcoming performance with team info: \(error)")
            return nil
        }
    }

    // MARK: - Team Discovery and Mapping
    
    func discoverAndMapTeams() async {
        print("🔍 Starting team discovery and mapping...")
        
        guard let selectedLeague = selectedLeague else {
            print("⚠️ No league selected for team discovery")
            return
        }
        
        var discoveredTeams: [String: String] = [:]
        
        // Sammle Team-IDs aus den Team-Spielern
        for player in teamPlayers {
            if !player.teamId.isEmpty && !player.teamName.isEmpty {
                discoveredTeams[player.teamId] = player.teamName
            }
        }
        
        // Sammle Team-IDs aus den Markt-Spielern
        for player in marketPlayers {
            if !player.teamId.isEmpty && !player.teamName.isEmpty {
                discoveredTeams[player.teamId] = player.teamName
            }
        }
        
        // Aktualisiere das Team-Mapping
        TeamMapping.updateMapping(with: discoveredTeams)
        
        print("✅ Discovered and mapped \(discoveredTeams.count) teams:")
        for (id, name) in discoveredTeams {
            print("   \(id): \(name)")
        }
    }
}
