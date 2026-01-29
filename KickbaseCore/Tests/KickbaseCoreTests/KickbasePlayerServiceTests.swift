import XCTest

@testable import KickbaseCore

@MainActor
final class KickbasePlayerServiceTests: XCTestCase {

    class MockAPI: KickbaseAPIServiceProtocol {
        var playerDetailsResponse: [String: Any]?
        var marketPlayersResponse: [String: Any]?
        var squadResponse: [String: Any]?
        var performanceResponse: PlayerPerformanceResponse?
        var teamProfileResponse: TeamProfileResponse?
        var shouldThrow: Error?

        func getLeagueSelection() async throws -> [String: Any] { [:] }
        func getLeagueRanking(leagueId: String, matchDay: Int?) async throws -> [String: Any] {
            [:]
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
            return ["prlo": 5000]
        }
        func getTeamProfile(leagueId: String, teamId: String) async throws -> TeamProfileResponse {
            if let err = shouldThrow { throw err }
            return teamProfileResponse
                ?? TeamProfileResponse(
                    tid: "t", tn: "Team", pl: 1, tv: 100000, tw: 0, td: 0, tl: 0, npt: 0,
                    avpcl: false)
        }
        // Unused protocol stubs
        func getMyBudget(leagueId: String) async throws -> [String: Any] { [:] }
        func getLeagueMe(leagueId: String) async throws -> [String: Any] { [:] }
    }

    class MockParser: KickbaseDataParserProtocol {
        var marketValueHistory: MarketValueChange? = MarketValueChange(
            daysSinceLastUpdate: 0, absoluteChange: 0, percentageChange: 0.0, previousValue: 0,
            currentValue: 0, dailyChanges: [])
        func parseLeaguesFromResponse(_ json: [String: Any]) -> [League] { [] }
        func parseLeagueRanking(from json: [String: Any], isMatchDayQuery: Bool) -> [LeagueUser] {
            []
        }
        func parseMarketValueHistory(from json: [String: Any]) -> MarketValueChange? {
            marketValueHistory
        }
        func parseUserStatsFromResponse(_ json: [String: Any], fallbackUser: LeagueUser)
            -> UserStats
        { fatalError() }
        func extractAveragePoints(from playerData: [String: Any]) -> Double { return 0.0 }
        func extractTotalPoints(from playerData: [String: Any]) -> Int { return 0 }
    }

    func testGetMatchDayStatsReturnsTuple() async throws {
        let mock = MockAPI()
        mock.playerDetailsResponse = ["smdc": 7, "ismc": 11, "smc": 9]
        let parser = MockParser()
        let service = KickbasePlayerService(apiService: mock, dataParser: parser)

        let result = await service.getMatchDayStats(leagueId: "l", playerId: "p")
        XCTAssertNotNil(result)
        XCTAssertEqual(result?.smdc, 7)
        XCTAssertEqual(result?.ismc, 11)
        XCTAssertEqual(result?.smc, 9)
    }

    func testGetCurrentMatchDayReturnsSmdc() async throws {
        let mock = MockAPI()
        mock.playerDetailsResponse = ["smdc": 3]
        let parser = MockParser()
        let service = KickbasePlayerService(apiService: mock, dataParser: parser)

        let current = await service.getCurrentMatchDay(leagueId: "l", playerId: "p")
        XCTAssertEqual(current, 3)
    }

    func testLoadPlayerDetailsParsesFields() async throws {
        let mock = MockAPI()
        mock.playerDetailsResponse = [
            "fn": "Max",
            "ln": "Mustermann",
            "tn": "Hertha",
            "position": 2,
            "number": 10,
            "tp": 123,
            "pim": "https://img",
        ]
        let parser = MockParser()
        let service = KickbasePlayerService(apiService: mock, dataParser: parser)

        let details = await service.loadPlayerDetails(playerId: "p", leagueId: "l")
        XCTAssertEqual(details?.fn, "Max")
        XCTAssertEqual(details?.ln, "Mustermann")
        XCTAssertEqual(details?.teamId, nil)
        XCTAssertEqual(details?.profileBigUrl, "https://img")
        XCTAssertEqual(details?.totalPoints, 123)
    }

    func testLoadPlayerMarketValueOnDemandReturnsPrlo() async throws {
        let mock = MockAPI()
        let parser = MockParser()
        let service = KickbasePlayerService(apiService: mock, dataParser: parser)

        let prlo = await service.loadPlayerMarketValueOnDemand(playerId: "p", leagueId: "l")
        XCTAssertEqual(prlo, 5000)
    }

    func testLoadPlayerMarketValueHistoryUsesParser() async throws {
        let mock = MockAPI()
        let parser = MockParser()
        parser.marketValueHistory = MarketValueChange(
            daysSinceLastUpdate: 0, absoluteChange: 100, percentageChange: 0.0, previousValue: 100,
            currentValue: 200,
            dailyChanges: [
                DailyMarketValueChange(
                    date: "d1", value: 100, change: 0, percentageChange: 0.0, daysAgo: 1),
                DailyMarketValueChange(
                    date: "d2", value: 200, change: 100, percentageChange: 100.0, daysAgo: 0),
            ])
        let service = KickbasePlayerService(apiService: mock, dataParser: parser)

        let history = await service.loadPlayerMarketValueHistory(playerId: "p", leagueId: "l")
        XCTAssertNotNil(history)
        XCTAssertEqual(history?.dailyChanges.count, 2)
    }

    func testLoadTeamPlayersParsesFromSquadAndUsesDetailEndpoint() async throws {
        let mock = MockAPI()
        mock.squadResponse = ["squad": [["i": "p1", "n": "Last"]]]
        mock.playerDetailsResponse = ["fn": "First", "ln": "Last", "tn": "Team X"]
        let parser = MockParser()
        let service = KickbasePlayerService(apiService: mock, dataParser: parser)

        let league = League(
            id: "l", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0, placement: 1,
                won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: nil))

        let players = try await service.loadTeamPlayers(for: league)
        XCTAssertEqual(players.count, 1)
        XCTAssertEqual(players.first?.firstName, "First")
        XCTAssertEqual(players.first?.lastName, "Last")
        XCTAssertEqual(players.first?.teamName, "Team X")
    }

    func testMarketParsingIgnoresPimFlagImageAndReturnsEmptyProfileBigUrl() async throws {
        let mock = MockAPI()
        // market response where only 'pim' is present and it's a flag image
        mock.marketPlayersResponse = [
            "it": [
                [
                    "i": "p1",
                    "fn": "Flagy",
                    "ln": "Player",
                    "pim": "https://example.com/flags/de.png",
                    "price": 1000,
                ]
            ]
        ]

        let parser = MockParser()
        let service = KickbasePlayerService(apiService: mock, dataParser: parser)

        let league = League(
            id: "l", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0, placement: 1,
                won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: nil))

        let marketPlayers = try await service.loadMarketPlayers(for: league)
        XCTAssertEqual(marketPlayers.count, 1)
        XCTAssertEqual(marketPlayers.first?.profileBigUrl, "")
    }

    func testMarketParsingAcceptsNonFlagLigainsiderImage() async throws {
        let mock = MockAPI()
        // market response where 'pim' contains a non-flag ligainsider image
        mock.marketPlayersResponse = [
            "it": [
                [
                    "i": "p1",
                    "fn": "Player",
                    "ln": "One",
                    "pim": "https://www.ligainsider.de/images/player123.jpg",
                    "price": 1000,
                ]
            ]
        ]

        let parser = MockParser()
        let service = KickbasePlayerService(apiService: mock, dataParser: parser)

        let league = League(
            id: "l", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0, placement: 1,
                won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: nil))

        let marketPlayers = try await service.loadMarketPlayers(for: league)
        XCTAssertEqual(marketPlayers.count, 1)
        XCTAssertEqual(
            marketPlayers.first?.profileBigUrl, "https://www.ligainsider.de/images/player123.jpg")
    }

    func testLoadPlayerPerformanceWithTeamInfoCachesResults() async throws {
        class CountingMock: MockAPI {
            var playerPerformanceCalls = 0
            var teamProfileCalls = 0

            override func getPlayerPerformance(leagueId: String, playerId: String) async throws
                -> PlayerPerformanceResponse
            {
                playerPerformanceCalls += 1
                return try await super.getPlayerPerformance(leagueId: leagueId, playerId: playerId)
            }

            override func getTeamProfile(leagueId: String, teamId: String) async throws
                -> TeamProfileResponse
            {
                teamProfileCalls += 1
                return try await super.getTeamProfile(leagueId: leagueId, teamId: teamId)
            }
        }

        let mock = CountingMock()

        // Create a minimal performance response with one season and one match
        let match = MatchPerformance(
            day: 1, p: 5, mp: "90'", md: "2025-01-01", t1: "t1", t2: "t2", t1g: 1, t2g: 0, pt: "t1",
            k: [], st: 5, cur: false, mdst: 0, ap: 5, tp: 5, asp: 5)
        let season = SeasonPerformance(ti: "s", n: "L", ph: [match])
        mock.performanceResponse = PlayerPerformanceResponse(it: [season])

        let parser = MockParser()
        let service = KickbasePlayerService(apiService: mock, dataParser: parser)

        // First call should hit the API
        let res1 = try await service.loadPlayerPerformanceWithTeamInfo(playerId: "p", leagueId: "l")
        XCTAssertNotNil(res1)
        XCTAssertEqual(mock.playerPerformanceCalls, 1)

        // Second call should return cached result (no additional API calls)
        let res2 = try await service.loadPlayerPerformanceWithTeamInfo(playerId: "p", leagueId: "l")
        XCTAssertNotNil(res2)
        XCTAssertEqual(mock.playerPerformanceCalls, 1)
    }

    func testLoadPlayerPerformanceHandlesRequestCancelled() async throws {
        class CancelMock: MockAPI {
            override func getPlayerPerformance(leagueId: String, playerId: String) async throws
                -> PlayerPerformanceResponse
            {
                throw APIError.requestCancelled
            }
        }

        let mock = CancelMock()
        let parser = MockParser()
        let service = KickbasePlayerService(apiService: mock, dataParser: parser)

        let res = try await service.loadPlayerPerformanceWithTeamInfo(playerId: "p", leagueId: "l")
        XCTAssertNil(res)
    }

    func testLoadPlayerPerformanceWithTeamInfo_DeduplicatesByDay_KeepFirst() async throws {
        let mock = MockAPI()

        // Create matches: first occurrence for day 5 should be kept
        let matchPlayed = MatchPerformance(
            day: 4, p: 5, mp: "90'", md: "2025-01-04", t1: "t1", t2: "t2", t1g: 1, t2g: 0,
            pt: "t1", k: [], st: 5, cur: false, mdst: 0, ap: 5, tp: 5, asp: 5)
        let matchA = MatchPerformance(
            day: 5, p: nil, mp: nil, md: "2025-01-05-1", t1: "t1", t2: "t2", t1g: nil, t2g: nil,
            pt: "t1", k: nil, st: 0, cur: false, mdst: 0, ap: 0, tp: 0, asp: 0)
        let matchB = MatchPerformance(
            day: 5, p: nil, mp: nil, md: "2025-01-05-2", t1: "t2", t2: "t3", t1g: nil, t2g: nil,
            pt: "t2", k: nil, st: 0, cur: false, mdst: 0, ap: 0, tp: 0, asp: 0)

        let season = SeasonPerformance(ti: "s", n: "L", ph: [matchA, matchB, matchPlayed])
        mock.performanceResponse = PlayerPerformanceResponse(it: [season])

        let parser = MockParser()
        let service = KickbasePlayerService(apiService: mock, dataParser: parser)

        let res = try await service.loadPlayerPerformanceWithTeamInfo(playerId: "p", leagueId: "l")
        XCTAssertNotNil(res)
        let day5Matches = res?.filter { $0.matchDay == 5 } ?? []
        XCTAssertEqual(day5Matches.count, 1)
        XCTAssertEqual(day5Matches.first?.matchDate, "2025-01-05-1")
    }

    func testLoadPlayerPerformanceWithTeamInfo_DeduplicatesByDay_WhenBothCurTrue_KeepFirst()
        async throws
    {
        let mock = MockAPI()

        let matchA = MatchPerformance(
            day: 10, p: nil, mp: nil, md: "2025-02-10-a", t1: "t1", t2: "t2", t1g: nil, t2g: nil,
            pt: "t1", k: nil, st: 0, cur: true, mdst: 0, ap: 0, tp: 0, asp: 0)
        let matchB = MatchPerformance(
            day: 10, p: nil, mp: nil, md: "2025-02-10-b", t1: "t2", t2: "t3", t1g: nil, t2g: nil,
            pt: "t2", k: nil, st: 0, cur: true, mdst: 0, ap: 0, tp: 0, asp: 0)

        let season = SeasonPerformance(ti: "s", n: "L", ph: [matchA, matchB])
        mock.performanceResponse = PlayerPerformanceResponse(it: [season])

        let parser = MockParser()
        let service = KickbasePlayerService(apiService: mock, dataParser: parser)

        let res = try await service.loadPlayerPerformanceWithTeamInfo(playerId: "p", leagueId: "l")
        XCTAssertNotNil(res)
        let day10Matches = res?.filter { $0.matchDay == 10 } ?? []
        XCTAssertEqual(day10Matches.count, 1)
        XCTAssertEqual(day10Matches.first?.matchDate, "2025-02-10-a")
    }
}
