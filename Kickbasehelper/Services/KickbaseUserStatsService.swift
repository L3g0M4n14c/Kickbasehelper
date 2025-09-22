import Foundation

@MainActor
class KickbaseUserStatsService: ObservableObject {
    private let apiClient: KickbaseAPIClient
    private let dataParser: KickbaseDataParser
    
    init(apiClient: KickbaseAPIClient, dataParser: KickbaseDataParser) {
        self.apiClient = apiClient
        self.dataParser = dataParser
    }
    
    // MARK: - User Stats Loading
    
    func loadUserStats(for league: League) async throws -> UserStats {
        print("📊 Loading user stats for league: \(league.name)")
        
        let endpoints = [
            "/v4/leagues/\(league.id)/me/budget",    // Spezifischer Budget-Endpoint
            "/v4/leagues/\(league.id)/me",           // Meine Daten in Liga (v4)
        ]
        
        do {
            let (_, json) = try await apiClient.tryMultipleEndpoints(endpoints: endpoints)
            return dataParser.parseUserStatsFromResponse(json, fallbackUser: league.currentUser)
        } catch APIError.authenticationFailed {
            throw APIError.authenticationFailed
        } catch {
            print("⚠️ All user stats endpoints failed, using fallback data")
            return createFallbackUserStats(from: league.currentUser)
        }
    }
    
    // MARK: - Fallback Data
    
    private func createFallbackUserStats(from leagueUser: LeagueUser) -> UserStats {
        print("📊 Using league user data as fallback for user stats")
        
        let userStats = UserStats(
            teamValue: leagueUser.teamValue,
            teamValueTrend: 0,
            budget: leagueUser.budget,
            points: leagueUser.points,
            placement: leagueUser.placement,
            won: leagueUser.won,
            drawn: leagueUser.drawn,
            lost: leagueUser.lost
        )
        
        print("📊 Fallback stats applied - Budget: €\(leagueUser.budget/1000)k, Teamwert: €\(leagueUser.teamValue/1000)k")
        return userStats
    }
}
