import Foundation

@MainActor
class KickbaseUserStatsService: ObservableObject {
    private let apiService: KickbaseAPIService
    private let dataParser: KickbaseDataParser
    
    init(apiService: KickbaseAPIService, dataParser: KickbaseDataParser) {
        self.apiService = apiService
        self.dataParser = dataParser
    }
    
    // MARK: - User Stats Loading
    
    func loadUserStats(for league: League) async throws -> UserStats {
        print("📊 Loading user stats for league: \(league.name)")
        
        do {
            // Versuche zuerst den Budget-Endpoint
            let json = try await apiService.getMyBudget(leagueId: league.id)
            return dataParser.parseUserStatsFromResponse(json, fallbackUser: league.currentUser)
        } catch {
            // Fallback auf Me-Endpoint
            do {
                let json = try await apiService.getLeagueMe(leagueId: league.id)
                return dataParser.parseUserStatsFromResponse(json, fallbackUser: league.currentUser)
            } catch {
                print("⚠️ All user stats endpoints failed, using fallback data")
                return createFallbackUserStats(from: league.currentUser)
            }
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
