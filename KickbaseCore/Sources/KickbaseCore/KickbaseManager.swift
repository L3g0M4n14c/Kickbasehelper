import Foundation
import SwiftUI

@MainActor
public class KickbaseManager: ObservableObject {
    // Published Properties für UI State
    @Published public var leagues: [League] = []
    @Published public var selectedLeague: League?
    @Published public var teamPlayers: [TeamPlayer] = []
    @Published public var livePlayers: [LivePlayer] = []  // New live players property
    @Published public var eventTypeNames: [Int: String] = [:]
    @Published public var marketPlayers: [MarketPlayer] = []
    @Published public var userStats: UserStats?
    @Published public var leagueUsers: [LeagueUser] = []
    @Published public var isLoading = false
    @Published public var errorMessage: String?
    
    // Services
    private let apiService: KickbaseAPIService
    private let dataParser: KickbaseDataParser
    private let leagueService: KickbaseLeagueService
    private let playerService: KickbasePlayerService
    private let userStatsService: KickbaseUserStatsService
    
    // MARK: - Public Service Access
    
    public var authenticatedPlayerService: KickbasePlayerService {
        return playerService
    }
    
    public init() {
        self.apiService = KickbaseAPIService()
        self.dataParser = KickbaseDataParser()
        self.leagueService = KickbaseLeagueService(apiService: apiService, dataParser: dataParser)
        self.playerService = KickbasePlayerService(apiService: apiService, dataParser: dataParser)
        self.userStatsService = KickbaseUserStatsService(
            apiService: apiService, dataParser: dataParser)
    }
    
    // MARK: - Authentication
    
    public func setAuthToken(_ token: String) {
        apiService.setAuthToken(token)
        print("🔑 Auth token set for KickbaseManager")
    }
    
    // MARK: - Data Loading Coordination
    
    public func loadUserData() async {
        print("📊 Loading user data...")
        await loadLeagues()
        
        // Forciere das Laden der UserStats nach Liga-Auswahl
        if let selectedLeague = selectedLeague {
            print("🔄 Force reloading user stats after league selection...")
            await loadUserStats(for: selectedLeague)
        }
    }
    
    public func loadLeagues() async {
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
    
    public func loadTeamPlayers(for league: League) async {
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
    
    public func loadMarketPlayers(for league: League) async {
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

    public func loadLeagueRanking(for league: League) async {
        isLoading = true
        errorMessage = nil

        do {
            let users = try await leagueService.loadLeagueRanking(for: league)
            self.leagueUsers = users
            print("✅ Loaded \(users.count) league users")
        } catch {
            print("❌ Error loading league ranking: \(error)")
            errorMessage = "Fehler beim Laden der Liga-Tabelle: \(error.localizedDescription)"
        }

        isLoading = false
    }

    
    public func loadLivePoints() async {
        guard let league = selectedLeague else {
            errorMessage = "Keine Liga ausgewählt"
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        do {
            print("🔴 Loading live points. Manager ID: \(ObjectIdentifier(self))")
            let response = try await apiService.getMyEleven(leagueId: league.id)
            
            // Force UI update on main thread explicitly
            await MainActor.run {
                self.livePlayers = response.players
                self.isLoading = false
            }
            print("✅ Loaded \(livePlayers.count) live players (on MainActor)")
        } catch {
            print("❌ Error loading live points: \(error)")
            await MainActor.run {
                self.errorMessage = "Fehler beim Laden der Live-Punkte: \(error.localizedDescription)"
                self.isLoading = false
            }
        }
        // Removed outer isLoading = false to avoid race conditions with MainActor.run block above
    }

    // Wrapper for detail view
        public func loadPlayerMatchDetails(
            leagueId: String, competitionId: String, playerId: String, dayNumber: Int
        ) async throws -> PlayerMatchDetailResponse {
            if eventTypeNames.isEmpty {
                await loadEventDefinitions()
            }
            return try await apiService.getPlayerEventHistory(
                competitionId: competitionId, playerId: playerId, dayNumber: dayNumber)
        }
        
        public func loadEventDefinitions() async {
            do {
                let response = try await apiService.getLiveEventTypes()
                var map: [Int: String] = [:]
                
                // 1. Process standard types (it / types)
                for type in response.types {
                    map[type.id] = type.name
                }
                
                // 2. Process formulas (dds) for core events
                if let formulas = response.formulas {
                    for (key, value) in formulas {
                        if let id = Int(key) {
                            // Clean template string for display (e.g. "Goal by {0}" -> "Goal by ...")
                            // For now we use the raw template as the name, leaving semantic resolution for later
                            map[id] = value
                        }
                    }
                }
                
                self.eventTypeNames = map
                print("✅ Loaded \(map.count) event type definitions")
            } catch {
                print("⚠️ Failed to load event types: \(error)")
            }
        }
        
        public func loadUserStats(for league: League) async {
            isLoading = true
            errorMessage = nil
            
            do {
                let stats = try await userStatsService.loadUserStats(for: league)
                self.userStats = stats
                print("✅ Loaded user stats")
            } catch {
                print("❌ Error loading user stats: \(error)")
                errorMessage =
                "Fehler beim Laden der Benutzerstatistiken: \(error.localizedDescription)"
            }
            
            isLoading = false
        }
        
        // MARK: - League Selection
        
        public func selectLeague(_ league: League) {
            selectedLeague = league
            
            // Lade Daten für die neue Liga
            Task {
                await loadTeamPlayers(for: league)
                await loadMarketPlayers(for: league)
                await loadUserStats(for: league)
            }
        }
        
        // MARK: - Player Market Value History
        
        public func loadPlayerMarketValueHistory(playerId: String, leagueId: String) async
        -> MarketValueChange?
        {
            print("📈 Loading market value history for player \(playerId) in league \(leagueId)")
            
            do {
                let history = await playerService.loadPlayerMarketValueHistory(
                    playerId: playerId, leagueId: leagueId)
                if let history = history {
                    print(
                        "✅ Successfully loaded market value history with \(history.dailyChanges.count) daily changes"
                    )
                } else {
                    print("⚠️ No market value history returned from player service")
                }
                return history
            }
        }
        
        public func loadPlayerMarketValueOnDemand(playerId: String, leagueId: String) async -> Int? {
            print("💰 Loading on-demand market value for player \(playerId) in league \(leagueId)")
            
            do {
                let profit = await playerService.loadPlayerMarketValueOnDemand(
                    playerId: playerId, leagueId: leagueId)
                if let profit = profit {
                    print("✅ Successfully loaded on-demand profit: €\(profit)")
                } else {
                    print("⚠️ No profit value returned from player service")
                }
                return profit
            }
        }
        
        // MARK: - Player Performance with Team Info
        
        public func loadPlayerPerformanceWithTeamInfo(playerId: String, leagueId: String) async throws
        -> [EnhancedMatchPerformance]?
        {
            print("📊 Loading enhanced player performance with team info for player \(playerId)")
            
            do {
                let enhancedMatches = try await playerService.loadPlayerPerformanceWithTeamInfo(
                    playerId: playerId, leagueId: leagueId)
                
                if let enhancedMatches = enhancedMatches {
                    print(
                        "✅ Successfully loaded \(enhancedMatches.count) enhanced matches with team info"
                    )
                } else {
                    print("⚠️ No enhanced performance data returned")
                }
                
                return enhancedMatches
            } catch {
                print("❌ Error loading enhanced player performance: \(error)")
                throw error
            }
        }
        
        public func loadPlayerRecentPerformanceWithTeamInfo(playerId: String, leagueId: String) async
        -> [EnhancedMatchPerformance]?
        {
            print(
                "📊 Loading recent enhanced player performance (last 5 match days) for player \(playerId)"
            )
            
            do {
                guard
                    let allEnhancedMatches = try await loadPlayerPerformanceWithTeamInfo(
                        playerId: playerId, leagueId: leagueId)
                else {
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
                
                print(
                    "✅ Filtered to \(recentMatches.count) recent enhanced matches (days \(recentMatches.first?.matchDay ?? 0) - \(currentMatchDay))"
                )
                return recentMatches
            } catch {
                print("❌ Error loading recent performance with team info: \(error)")
                return nil
            }
        }
        
        public func loadPlayerUpcomingPerformanceWithTeamInfo(playerId: String, leagueId: String) async
        -> [EnhancedMatchPerformance]?
        {
            print(
                "📊 Loading upcoming enhanced player performance (next 3 match days) for player \(playerId)"
            )
            
            do {
                guard
                    let allEnhancedMatches = try await loadPlayerPerformanceWithTeamInfo(
                        playerId: playerId, leagueId: leagueId)
                else {
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
                
                print(
                    "✅ Filtered to \(upcomingMatches.count) upcoming enhanced matches (days \(currentMatchDay + 1) - \(currentMatchDay + 3))"
                )
                return upcomingMatches
            } catch {
                print("❌ Error loading upcoming performance with team info: \(error)")
                return nil
            }
        }
        
        // MARK: - Team Discovery and Mapping
        
        public func discoverAndMapTeams() async {
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

