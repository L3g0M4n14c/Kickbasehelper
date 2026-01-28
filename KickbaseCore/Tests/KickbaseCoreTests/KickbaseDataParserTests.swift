import XCTest

@testable import KickbaseCore

@MainActor
final class KickbaseDataParserTests: XCTestCase {

    func testExtractIntDoubleStringConversions() {
        let parser = KickbaseDataParser()
        let data: [String: Any] = ["int": 5, "dbl": 3.0, "str": "42"]

        XCTAssertEqual(parser.extractInt(from: data, keys: ["int"]), 5)
        XCTAssertEqual(parser.extractInt(from: data, keys: ["dbl"]), 3)
        XCTAssertEqual(parser.extractInt(from: data, keys: ["str"]), 42)

        XCTAssertEqual(parser.extractDouble(from: data, keys: ["dbl"]), 3.0)
        XCTAssertEqual(parser.extractDouble(from: data, keys: ["int"]), 5.0)
    }

    func testExtractTotalAndAveragePoints() {
        let parser = KickbaseDataParser()

        XCTAssertEqual(parser.extractTotalPoints(from: ["p": 100]), 100)
        XCTAssertEqual(parser.extractTotalPoints(from: ["tp": "55"]), 55)
        XCTAssertEqual(parser.extractTotalPoints(from: [:]), 0)

        XCTAssertEqual(parser.extractAveragePoints(from: ["ap": "2.5"]), 2.5)
        XCTAssertEqual(parser.extractAveragePoints(from: ["avg": 3]), 3.0)
        XCTAssertEqual(parser.extractAveragePoints(from: [:]), 0.0)
    }

    func testParseMarketValueHistoryCalculations() {
        let parser = KickbaseDataParser()

        let json: [String: Any] = [
            "prlo": 5000,
            "it": [
                ["dt": 3, "mv": 300],
                ["dt": 2, "mv": 200],
                ["dt": 1, "mv": 100],
            ],
        ]

        let result = parser.parseMarketValueHistory(from: json)
        XCTAssertNotNil(result)

        XCTAssertEqual(result?.currentValue, 300)
        XCTAssertEqual(result?.previousValue, 200)
        XCTAssertEqual(result?.absoluteChange, 100)
        XCTAssertEqual(Int(round(result?.percentageChange ?? 0.0)), 50)
        XCTAssertEqual(result?.daysSinceLastUpdate, 1)
        XCTAssertEqual(result?.dailyChanges.count, 2)
        XCTAssertEqual(result?.dailyChanges.first?.value, 300)
    }

    func testParseUserStatsHandlesNestedObjectsAndFallback() {
        let parser = KickbaseDataParser()

        let fallback = LeagueUser(
            id: "u", name: "n", teamName: "t", budget: 5000, teamValue: 123000, points: 0,
            placement: 1, won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: nil)

        let jsonUser: [String: Any] = ["user": ["budget": 7000, "teamValue": 111000]]
        let stats = parser.parseUserStatsFromResponse(jsonUser, fallbackUser: fallback)
        XCTAssertEqual(stats.budget, 7000)
        XCTAssertEqual(stats.teamValue, 111000)

        let jsonEmpty: [String: Any] = [:]
        let statsFallback = parser.parseUserStatsFromResponse(jsonEmpty, fallbackUser: fallback)
        XCTAssertEqual(statsFallback.budget, 5000)
        XCTAssertEqual(statsFallback.teamValue, 123000)
    }

    func testParseLeaguesFindsItAndAnolStructures() {
        let parser = KickbaseDataParser()

        // 'it' as dictionary with nested array
        let jsonIt: [String: Any] = ["it": ["group": [["id": "x", "n": "X"]]]]
        let leaguesIt = parser.parseLeaguesFromResponse(jsonIt)
        XCTAssertEqual(leaguesIt.count, 1)
        XCTAssertEqual(leaguesIt.first?.id, "x")
        XCTAssertEqual(leaguesIt.first?.name, "X")

        // 'anol' as direct array
        let jsonAnol: [String: Any] = ["anol": [["id": "y", "n": "Y"]]]
        let leaguesAnol = parser.parseLeaguesFromResponse(jsonAnol)
        XCTAssertEqual(leaguesAnol.count, 1)
        XCTAssertEqual(leaguesAnol.first?.id, "y")
    }

    func testParseLeagueUserTeamNameFallbacks() {
        let parser = KickbaseDataParser()

        let cu1: [String: Any] = ["cu": ["tn": "Team1"]]
        let user1 = parser.parseLeagueUser(from: cu1)
        XCTAssertEqual(user1.teamName, "Team1")

        let cu2: [String: Any] = ["cu": ["clubName": "ClubX"]]
        let user2 = parser.parseLeagueUser(from: cu2)
        XCTAssertEqual(user2.teamName, "ClubX")

        let cu3: [String: Any] = ["cu": [:]]
        let user3 = parser.parseLeagueUser(from: cu3)
        XCTAssertEqual(user3.teamName, "Team")  // default fallback
    }

    func testParseLeagueRankingMatchdayAndOverallPointsSelection() {
        let parser = KickbaseDataParser()

        let jsonOverall: [String: Any] = ["us": [["i": "u1", "n": "U", "sp": 10, "spl": 2]]]
        let usersOverall = parser.parseLeagueRanking(from: jsonOverall, isMatchDayQuery: false)
        XCTAssertEqual(usersOverall.first?.points, 10)
        XCTAssertEqual(usersOverall.first?.placement, 2)

        let jsonMatchday: [String: Any] = ["us": [["i": "u1", "n": "U", "mdp": 7, "mdpl": 3]]]
        let usersMatchday = parser.parseLeagueRanking(from: jsonMatchday, isMatchDayQuery: true)
        XCTAssertEqual(usersMatchday.first?.points, 7)
        XCTAssertEqual(usersMatchday.first?.placement, 3)
    }
}
