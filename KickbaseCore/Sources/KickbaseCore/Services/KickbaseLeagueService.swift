import Combine
import Foundation
import SwiftUI

// MARK: - Protocols for testability
@MainActor public protocol KickbaseAPIServiceProtocol: AnyObject {
    func getLeagueSelection() async throws -> [String: Any]
    func getLeagueRanking(leagueId: String, matchDay: Int?) async throws -> [String: Any]

    // Player-related
    func getPlayerDetails(leagueId: String, playerId: String) async throws -> [String: Any]
    func getMySquad(leagueId: String) async throws -> [String: Any]
    func getMarketPlayers(leagueId: String) async throws -> [String: Any]
    func getPlayerPerformance(leagueId: String, playerId: String) async throws
        -> PlayerPerformanceResponse
    func getPlayerMarketValue(leagueId: String, playerId: String, timeframe: Int) async throws
        -> [String: Any]
    func getTeamProfile(leagueId: String, teamId: String) async throws -> TeamProfileResponse

    // User stats
    func getMyBudget(leagueId: String) async throws -> [String: Any]
    func getLeagueMe(leagueId: String) async throws -> [String: Any]
}

@MainActor public protocol KickbaseDataParserProtocol: AnyObject {
    func parseLeaguesFromResponse(_ json: [String: Any]) -> [League]
    func parseLeagueRanking(from json: [String: Any], isMatchDayQuery: Bool) -> [LeagueUser]

    // Market / Stats helpers
    func parseMarketValueHistory(from json: [String: Any]) -> MarketValueChange?
    func parseUserStatsFromResponse(_ json: [String: Any], fallbackUser: LeagueUser) -> UserStats

    // Small extract helpers used by PlayerService
    func extractAveragePoints(from playerData: [String: Any]) -> Double
    func extractTotalPoints(from playerData: [String: Any]) -> Int
}

// Conform existing implementations to the protocols
extension KickbaseAPIService: KickbaseAPIServiceProtocol {}
extension KickbaseDataParser: KickbaseDataParserProtocol {}

@MainActor
public class KickbaseLeagueService: ObservableObject {
    private let apiService: KickbaseAPIServiceProtocol
    private let dataParser: KickbaseDataParserProtocol

    public init(apiService: KickbaseAPIServiceProtocol, dataParser: KickbaseDataParserProtocol) {
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
            let json = try await apiService.getLeagueRanking(leagueId: league.id, matchDay: nil)
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

    public func loadMatchDayRanking(for league: League, matchDay: Int) async throws -> [LeagueUser]
    {
        print("🏆 Loading matchday \(matchDay) ranking for: \(league.name)")

        do {
            let json = try await apiService.getLeagueRanking(
                leagueId: league.id, matchDay: matchDay)
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
