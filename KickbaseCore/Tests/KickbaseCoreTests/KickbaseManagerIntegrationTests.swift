import XCTest

@testable import KickbaseCore

@MainActor
final class KickbaseManagerIntegrationTests: XCTestCase {

    func testLoadLeaguesSetsSelectedLeagueAndIsLoading() async throws {
        // Stubbed API session
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        let session = URLSession(configuration: config)
        let api = KickbaseAPIService(session: session)
        let parser = KickbaseDataParser()

        // Prepare league selection payload
        let payload =
            "{ \"leagues\": [{ \"id\": \"L1\", \"name\": \"X\", \"currentUser\": { \"id\": \"u\", \"name\": \"n\", \"tn\": \"T\" } }] }"
            .data(using: .utf8)!

        URLProtocolMock.requestHandler = { request in
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (resp, payload)
        }

        let manager = KickbaseManager(apiService: api, dataParser: parser)
        XCTAssertTrue(manager.leagues.isEmpty)

        await manager.loadLeagues()

        XCTAssertEqual(manager.leagues.count, 1)
        XCTAssertEqual(manager.selectedLeague?.id, "L1")
        XCTAssertFalse(manager.isLoading)
    }

    func testLoadMatchDayRankingRaceConditionKeepsLatestResult() async throws {
        // Create a mock league service that delays for matchDay 1
        class MockLeagueService: KickbaseLeagueService {
            override func loadMatchDayRanking(for league: League, matchDay: Int) async throws
                -> [LeagueUser]
            {
                if matchDay == 1 {
                    try await Task.sleep(nanoseconds: 200_000_000)  // delay 200ms
                    return [
                        LeagueUser(
                            id: "old", name: "old", teamName: "t", budget: 0, teamValue: 0,
                            points: 1, placement: 1, won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0,
                            mpst: nil)
                    ]
                }
                return [
                    LeagueUser(
                        id: "new", name: "new", teamName: "t", budget: 0, teamValue: 0, points: 2,
                        placement: 1, won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: nil)
                ]
            }
        }

        let mockLeague = MockLeagueService(
            apiService: KickbaseAPIService(), dataParser: KickbaseDataParser())
        let manager = KickbaseManager(
            apiService: KickbaseAPIService(), dataParser: KickbaseDataParser(),
            leagueService: mockLeague)

        let league = League(
            id: "L", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0, placement: 1,
                won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: nil))

        // Fire first (slow) request
        let task1 = Task { await manager.loadMatchDayRanking(for: league, matchDay: 1) }
        // Shortly after fire second (fast) request
        try await Task.sleep(nanoseconds: 50_000_000)
        let task2 = Task { await manager.loadMatchDayRanking(for: league, matchDay: 2) }

        await task1.value
        await task2.value

        // Expect the latest (matchDay 2) to be present
        XCTAssertEqual(manager.matchDayUsers.first?.id, "new")
    }
}
