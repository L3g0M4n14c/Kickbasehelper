import XCTest

@testable import KickbaseCore

@MainActor
final class KickbaseLeagueServiceTests: XCTestCase {

    class MockAPIService: KickbaseAPIServiceProtocol {
        var leagueSelectionResponse: [String: Any]?
        var leagueRankingResponse: [String: Any]?
        var playerDetailsResponse: [String: Any]?
        var squadResponse: [String: Any]?
        var marketPlayersResponse: [String: Any]?
        var performanceResponse: PlayerPerformanceResponse?
        var teamProfileResponse: TeamProfileResponse?
        var myBudgetResponse: [String: Any]?
        var leagueMeResponse: [String: Any]?
        var shouldThrow: Error?

        func getLeagueSelection() async throws -> [String: Any] {
            if let err = shouldThrow { throw err }
            return leagueSelectionResponse ?? [:]
        }

        func getLeagueRanking(leagueId: String, matchDay: Int?) async throws -> [String: Any] {
            if let err = shouldThrow { throw err }
            return leagueRankingResponse ?? [:]
        }

        func getPlayerDetails(leagueId: String, playerId: String) async throws -> [String: Any] {
            if let err = shouldThrow { throw err }
            return playerDetailsResponse ?? [:]
        }

        func getMySquad(leagueId: String) async throws -> [String: Any] {
            if let err = shouldThrow { throw err }
            return squadResponse ?? [:]
        }

        func getMarketPlayers(leagueId: String) async throws -> [String: Any] {
            if let err = shouldThrow { throw err }
            return marketPlayersResponse ?? [:]
        }

        func getPlayerPerformance(leagueId: String, playerId: String) async throws
            -> PlayerPerformanceResponse
        {
            if let err = shouldThrow { throw err }
            return performanceResponse ?? PlayerPerformanceResponse(it: [])
        }

        func getPlayerMarketValue(leagueId: String, playerId: String, timeframe: Int) async throws
            -> [String: Any]
        {
            if let err = shouldThrow { throw err }
            return ["prlo": 0]
        }

        func getTeamProfile(leagueId: String, teamId: String) async throws -> TeamProfileResponse {
            if let err = shouldThrow { throw err }
            return teamProfileResponse
                ?? TeamProfileResponse(
                    tid: "t", tn: "Team", pl: 1, tv: 100000, tw: 0, td: 0, tl: 0, npt: 0,
                    avpcl: false)
        }

        func getMyBudget(leagueId: String) async throws -> [String: Any] {
            if let err = shouldThrow { throw err }
            return myBudgetResponse ?? [:]
        }

        func getLeagueMe(leagueId: String) async throws -> [String: Any] {
            if let err = shouldThrow { throw err }
            return leagueMeResponse ?? [:]
        }
    }

    struct TestError: Error {}

    func testLoadLeaguesReturnsParsedLeagues() async throws {
        // Arrange
        let mockAPI = MockAPIService()
        mockAPI.leagueSelectionResponse = [
            "leagues": [
                [
                    "id": "league-1",
                    "name": "Test League",
                    "creatorName": "Creator",
                    "adminName": "Admin",
                    "created": "2026-01-28",
                    "season": "2025/26",
                    "matchDay": 5,
                    "currentUser": [
                        "id": "u1",
                        "name": "User 1",
                        "teamName": "Team A",
                    ],
                ]
            ]
        ]

        let parser = KickbaseDataParser()
        let service = KickbaseLeagueService(apiService: mockAPI, dataParser: parser)

        // Act
        let leagues = try await service.loadLeagues()

        // Assert
        XCTAssertEqual(leagues.count, 1)
        XCTAssertEqual(leagues.first?.id, "league-1")
        XCTAssertEqual(leagues.first?.name, "Test League")
        XCTAssertEqual(leagues.first?.currentUser.name, "User 1")
    }

    func testLoadLeaguesThrowsOnAPIError() async {
        // Arrange
        let mockAPI = MockAPIService()
        mockAPI.shouldThrow = TestError()
        let parser = KickbaseDataParser()
        let service = KickbaseLeagueService(apiService: mockAPI, dataParser: parser)

        // Act & Assert
        do {
            _ = try await service.loadLeagues()
            XCTFail("Expected error to be thrown")
        } catch {
            // success
        }
    }

    func testLoadLeagueRankingReturnsSortedUsers() async throws {
        // Arrange
        let mockAPI = MockAPIService()
        // Two users with different points
        mockAPI.leagueRankingResponse = [
            "us": [
                ["i": "u1", "n": "User A", "sp": 10],
                ["i": "u2", "n": "User B", "sp": 20],
            ]
        ]

        let parser = KickbaseDataParser()
        let service = KickbaseLeagueService(apiService: mockAPI, dataParser: parser)

        let league = League(
            id: "league-1", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0, placement: 1,
                won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: nil))

        // Act
        let users = try await service.loadLeagueRanking(for: league)

        // Assert: should be sorted descending by points (User B first)
        XCTAssertEqual(users.count, 2)
        XCTAssertEqual(users.first?.id, "u2")
        XCTAssertEqual(users.first?.points, 20)
        XCTAssertEqual(users.last?.id, "u1")
    }

    func testLoadMatchDayRankingUsesMatchDayPoints() async throws {
        // Arrange
        let mockAPI = MockAPIService()
        mockAPI.leagueRankingResponse = [
            "us": [
                ["i": "u1", "n": "User A", "mdp": 5],
                ["i": "u2", "n": "User B", "mdp": 15],
            ]
        ]

        let parser = KickbaseDataParser()
        let service = KickbaseLeagueService(apiService: mockAPI, dataParser: parser)

        let league = League(
            id: "league-1", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0, placement: 1,
                won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: nil))

        // Act
        let users = try await service.loadMatchDayRanking(for: league, matchDay: 3)

        // Assert
        XCTAssertEqual(users.count, 2)
        XCTAssertEqual(users.first?.id, "u2")
        XCTAssertEqual(users.first?.points, 15)
    }

    func testLoadLeaguesParsesDataKeyFormat() async throws {
        // Arrange
        let mockAPI = MockAPIService()
        mockAPI.leagueSelectionResponse = [
            "data": [
                [
                    "id": "league-2",
                    "n": "Data League",
                    "cn": "Creator 2",
                    "an": "Admin 2",
                    "c": "2026-01-01",
                    "s": "2025/26",
                    "md": 2,
                    "cu": ["id": "u2", "n": "User 2", "tn": "Team B"],
                ]
            ]
        ]

        let parser = KickbaseDataParser()
        let service = KickbaseLeagueService(apiService: mockAPI, dataParser: parser)

        // Act
        let leagues = try await service.loadLeagues()

        // Assert
        XCTAssertEqual(leagues.count, 1)
        XCTAssertEqual(leagues.first?.id, "league-2")
        XCTAssertEqual(leagues.first?.name, "Data League")
        XCTAssertEqual(leagues.first?.currentUser.teamName, "Team B")
    }

    func testLoadLeaguesReturnsEmptyWhenNoLeaguesFound() async throws {
        // Arrange
        let mockAPI = MockAPIService()
        mockAPI.leagueSelectionResponse = [:]

        let parser = KickbaseDataParser()
        let service = KickbaseLeagueService(apiService: mockAPI, dataParser: parser)

        // Act
        let leagues = try await service.loadLeagues()

        // Assert
        XCTAssertEqual(leagues.count, 0)
    }

    func testLoadLeagueRankingThrowsOnAPIError() async {
        // Arrange
        let mockAPI = MockAPIService()
        mockAPI.shouldThrow = TestError()
        let parser = KickbaseDataParser()
        let service = KickbaseLeagueService(apiService: mockAPI, dataParser: parser)

        let league = League(
            id: "league-1", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0, placement: 1,
                won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: nil)
        )

        // Act & Assert
        do {
            _ = try await service.loadLeagueRanking(for: league)
            XCTFail("Expected error to be thrown")
        } catch {
            // success
        }
    }

    func testLoadMatchDayRankingThrowsOnAPIError() async {
        // Arrange
        let mockAPI = MockAPIService()
        mockAPI.shouldThrow = TestError()
        let parser = KickbaseDataParser()
        let service = KickbaseLeagueService(apiService: mockAPI, dataParser: parser)

        let league = League(
            id: "league-1", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0, placement: 1,
                won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: nil)
        )

        // Act & Assert
        do {
            _ = try await service.loadMatchDayRanking(for: league, matchDay: 2)
            XCTFail("Expected error to be thrown")
        } catch {
            // success
        }
    }

    func testLineupConversionFromIntArray() async throws {
        // Arrange
        let mockAPI = MockAPIService()
        mockAPI.leagueRankingResponse = [
            "us": [
                ["i": "u1", "n": "User A", "sp": 5, "lp": [1, 2, 3]]
            ]
        ]

        let parser = KickbaseDataParser()
        let service = KickbaseLeagueService(apiService: mockAPI, dataParser: parser)

        let league = League(
            id: "league-1", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0, placement: 1,
                won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: nil))

        // Act
        let users = try await service.loadLeagueRanking(for: league)

        // Assert
        XCTAssertEqual(users.count, 1)
        XCTAssertEqual(users.first?.lineupPlayerIds, ["1", "2", "3"])
    }

    func testLineupConversionFromNSNumberArray() async throws {
        // Arrange
        let mockAPI = MockAPIService()
        mockAPI.leagueRankingResponse = [
            "us": [
                [
                    "i": "u1", "n": "User A", "sp": 5,
                    "lp": [NSNumber(value: 10), NSNumber(value: 20)],
                ]
            ]
        ]

        let parser = KickbaseDataParser()
        let service = KickbaseLeagueService(apiService: mockAPI, dataParser: parser)

        let league = League(
            id: "league-1", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0, placement: 1,
                won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: nil))

        // Act
        let users = try await service.loadLeagueRanking(for: league)

        // Assert
        XCTAssertEqual(users.count, 1)
        XCTAssertEqual(users.first?.lineupPlayerIds, ["10", "20"])
    }

    func testLineupPreservesStringArray() async throws {
        // Arrange
        let mockAPI = MockAPIService()
        mockAPI.leagueRankingResponse = [
            "us": [
                ["i": "u1", "n": "User A", "sp": 5, "lp": ["10", "20"]]
            ]
        ]

        let parser = KickbaseDataParser()
        let service = KickbaseLeagueService(apiService: mockAPI, dataParser: parser)

        let league = League(
            id: "league-1", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0, placement: 1,
                won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: nil))

        // Act
        let users = try await service.loadLeagueRanking(for: league)

        // Assert
        XCTAssertEqual(users.count, 1)
        XCTAssertEqual(users.first?.lineupPlayerIds, ["10", "20"])
    }

    func testPlacementParsingOverallAndMatchday() async throws {
        // Arrange overall
        let mockAPI = MockAPIService()
        mockAPI.leagueRankingResponse = [
            "us": [
                ["i": "u1", "n": "User A", "sp": 30, "spl": 2]
            ]
        ]

        let parser = KickbaseDataParser()
        let service = KickbaseLeagueService(apiService: mockAPI, dataParser: parser)

        let league = League(
            id: "league-1", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0, placement: 1,
                won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: nil))

        // Act overall
        let usersOverall = try await service.loadLeagueRanking(for: league)
        XCTAssertEqual(usersOverall.first?.placement, 2)

        // Arrange matchday
        mockAPI.leagueRankingResponse = [
            "us": [
                ["i": "u1", "n": "User A", "mdp": 12, "mdpl": 3]
            ]
        ]

        // Act matchday
        let usersMatchday = try await service.loadMatchDayRanking(for: league, matchDay: 4)
        XCTAssertEqual(usersMatchday.first?.placement, 3)
    }

    func testLoadLeagueRankingReturnsEmptyWhenNoUsersArray() async throws {
        // Arrange
        let mockAPI = MockAPIService()
        mockAPI.leagueRankingResponse = ["foo": "bar"]

        let parser = KickbaseDataParser()
        let service = KickbaseLeagueService(apiService: mockAPI, dataParser: parser)

        let league = League(
            id: "league-1", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0, placement: 1,
                won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: nil))

        // Act
        let users = try await service.loadLeagueRanking(for: league)

        // Assert
        XCTAssertTrue(users.isEmpty)
    }
}

// Helper to assert async throws nicely
func XCTAssertThrowsErrorAsync(
    _ expression: @autoclosure @escaping () async throws -> Void,
    _ message: @autoclosure () -> String = "",
    file: StaticString = #file, line: UInt = #line
) async {
    do {
        try await expression()
        XCTFail("Expected error to be thrown. \(message())", file: file, line: line)
    } catch {
        // success
    }
}
