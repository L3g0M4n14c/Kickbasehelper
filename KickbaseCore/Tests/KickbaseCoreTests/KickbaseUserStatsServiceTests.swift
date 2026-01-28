import XCTest

@testable import KickbaseCore

@MainActor
final class KickbaseUserStatsServiceTests: XCTestCase {

    class MockAPI: KickbaseAPIServiceProtocol {
        var myBudgetResponse: [String: Any]?
        var leagueMeResponse: [String: Any]?
        var shouldThrowOnBudget = false
        var shouldThrowOnMe = false

        func getLeagueSelection() async throws -> [String: Any] { [:] }
        func getLeagueRanking(leagueId: String, matchDay: Int?) async throws -> [String: Any] {
            [:]
        }
        func getMyBudget(leagueId: String) async throws -> [String: Any] {
            if shouldThrowOnBudget { throw TestError() }
            return myBudgetResponse ?? [:]
        }
        func getLeagueMe(leagueId: String) async throws -> [String: Any] {
            if shouldThrowOnMe { throw TestError() }
            return leagueMeResponse ?? [:]
        }
        // Unused stubs for protocol
        func getPlayerDetails(leagueId: String, playerId: String) async throws -> [String: Any] {
            [:]
        }
        func getMySquad(leagueId: String) async throws -> [String: Any] { [:] }
        func getMarketPlayers(leagueId: String) async throws -> [String: Any] { [:] }
        func getPlayerPerformance(leagueId: String, playerId: String) async throws
            -> PlayerPerformanceResponse
        { PlayerPerformanceResponse(it: []) }
        func getPlayerMarketValue(leagueId: String, playerId: String, timeframe: Int) async throws
            -> [String: Any]
        { [:] }
        func getTeamProfile(leagueId: String, teamId: String) async throws -> TeamProfileResponse {
            TeamProfileResponse(
                tid: "t", tn: "Team", pl: 1, tv: 0, tw: 0, td: 0, tl: 0, npt: 0, avpcl: false)
        }
    }

    class MockParser: KickbaseDataParserProtocol {
        var lastParsedJson: [String: Any]?
        var returnedStats: UserStats = UserStats(
            teamValue: 123, teamValueTrend: 1, budget: 5000, points: 100, placement: 5, won: 10,
            drawn: 2, lost: 3)

        func parseLeaguesFromResponse(_ json: [String: Any]) -> [League] { [] }
        func parseLeagueRanking(from json: [String: Any], isMatchDayQuery: Bool) -> [LeagueUser] {
            []
        }

        func parseUserStatsFromResponse(_ json: [String: Any], fallbackUser: LeagueUser)
            -> UserStats
        {
            lastParsedJson = json
            return returnedStats
        }

        func parseMarketValueHistory(from json: [String: Any]) -> MarketValueChange? { nil }
        func extractAveragePoints(from playerData: [String: Any]) -> Double { 0.0 }
        func extractTotalPoints(from playerData: [String: Any]) -> Int { 0 }
    }

    struct TestError: Error {}

    func testLoadUserStatsUsesBudgetEndpointWhenAvailable() async throws {
        let mock = MockAPI()
        mock.myBudgetResponse = ["budget": 5000, "teamValue": 123000]
        let parser = MockParser()
        parser.returnedStats = UserStats(
            teamValue: 123000, teamValueTrend: 0, budget: 5000, points: 0, placement: 1, won: 0,
            drawn: 0, lost: 0)

        let service = KickbaseUserStatsService(apiService: mock, dataParser: parser)
        let league = League(
            id: "l", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 5000, teamValue: 123000, points: 0,
                placement: 1, won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: nil))

        let stats = try await service.loadUserStats(for: league)
        XCTAssertEqual(stats.budget, 5000)
        XCTAssertEqual(parser.lastParsedJson?["budget"] as? Int, 5000)
    }

    func testLoadUserStatsFallsBackToLeagueMeWhenBudgetFails() async throws {
        let mock = MockAPI()
        mock.shouldThrowOnBudget = true
        mock.leagueMeResponse = ["budget": 7000]
        let parser = MockParser()
        let service = KickbaseUserStatsService(apiService: mock, dataParser: parser)

        let league = League(
            id: "l", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 5000, teamValue: 123000, points: 0,
                placement: 1, won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: nil))

        let stats = try await service.loadUserStats(for: league)
        XCTAssertEqual(parser.lastParsedJson?["budget"] as? Int, 7000)
    }

    func testLoadUserStatsReturnsFallbackWhenAllEndpointsFail() async throws {
        let mock = MockAPI()
        mock.shouldThrowOnBudget = true
        mock.shouldThrowOnMe = true
        let parser = MockParser()
        let service = KickbaseUserStatsService(apiService: mock, dataParser: parser)

        let leagueUser = LeagueUser(
            id: "u", name: "n", teamName: "t", budget: 5555, teamValue: 333000, points: 42,
            placement: 7, won: 4, drawn: 1, lost: 2, se11: 0, ttm: 0, mpst: nil)
        let league = League(
            id: "l", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1, currentUser: leagueUser)

        let stats = try await service.loadUserStats(for: league)
        XCTAssertEqual(stats.budget, 5555)
        XCTAssertEqual(stats.teamValue, 333000)
        XCTAssertEqual(stats.points, 42)
    }
}
