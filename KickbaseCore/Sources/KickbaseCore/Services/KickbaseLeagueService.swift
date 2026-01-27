import Combine
import SwiftUI
import Foundation

@MainActor
public class KickbaseLeagueService: ObservableObject {
    private let apiService: KickbaseAPIService
    private let dataParser: KickbaseDataParser
    
    public init(apiService: KickbaseAPIService, dataParser: KickbaseDataParser) {
        self.apiService = apiService
        self.dataParser = dataParser
    }
    
    // MARK: - League Loading
    
    public func loadLeagues() async throws -> [League] {
        print("🏆 Loading leagues...")
        
        do {
            let json = try await apiService.getLeagueSelection()
            let leagues = dataParser.parseLeaguesFromResponse(json)
            
            if leagues.isEmpty {
                print("⚠️ No leagues found in response")
            }
            return leagues
        } catch {
            print("❌ Failed to load leagues: \(error.localizedDescription)")
            throw error
        }
    }
    
    // MARK: - League Ranking
    
    public func loadLeagueRanking(for league: League) async throws -> [LeagueUser] {
        print("🏆 Loading league ranking for: \(league.name)")
        
        do {
            let json = try await apiService.getLeagueRanking(leagueId: league.id)
            let users = dataParser.parseLeagueRanking(from: json, isMatchDayQuery: false)
            
            // Sort by points descending
            let sortedUsers = users.sorted { $0.points > $1.points }
            
            if sortedUsers.isEmpty {
                print("⚠️ No users found in ranking")
            } else {
                print("✅ Loaded \(sortedUsers.count) users in ranking")
            }
            return sortedUsers
        } catch {
            print("❌ Failed to load league ranking: \(error.localizedDescription)")
            throw error
        }
    }
    
    public func loadMatchDayRanking(for league: League, matchDay: Int) async throws -> [LeagueUser] {
        print("🏆 Loading matchday \(matchDay) ranking for: \(league.name)")
        
        do {
            let json = try await apiService.getLeagueRanking(leagueId: league.id, matchDay: matchDay)
            let users = dataParser.parseLeagueRanking(from: json, isMatchDayQuery: true)
            
            // Sort by points descending
            let sortedUsers = users.sorted { $0.points > $1.points }
            
            if sortedUsers.isEmpty {
                print("⚠️ No users found in matchday ranking")
            } else {
                print("✅ Loaded \(sortedUsers.count) users in matchday ranking")
            }
            return sortedUsers
        } catch {
            print("❌ Failed to load matchday ranking: \(error.localizedDescription)")
            throw error
        }
    }
    
}
