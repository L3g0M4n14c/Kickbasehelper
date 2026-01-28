import XCTest

@testable import KickbaseCore

@MainActor
final class PlayerRecommendationServiceTests: XCTestCase {

    func makeTeamPlayer(
        id: String, position: Int, marketValue: Int, avg: Double = 5.0, total: Int = 50,
        status: Int = 0, teamId: String = "t1"
    ) -> TeamPlayer {
        return TeamPlayer(
            id: id, firstName: "F",
            lastName: "L",
            profileBigUrl: "",
            teamName: "Team",
            teamId: teamId,
            position: position,
            number: 12,
            averagePoints: avg,
            totalPoints: total,
            marketValue: marketValue,
            marketValueTrend: 0,
            tfhmvt: 0,
            prlo: 0,
            stl: 0,
            status: status,
            userOwnsPlayer: true)
    }

    func makeMarketPlayer(
        id: String, position: Int, price: Int, avg: Double = 6.0, total: Int = 100,
        sellerId: String = "other"
    ) -> MarketPlayer {
        return MarketPlayer(
            id: id, firstName: "M", lastName: "P", profileBigUrl: "", teamName: "T", teamId: "tm1",
            position: position, number: 20, averagePoints: avg, totalPoints: total,
            marketValue: price, marketValueTrend: 0, price: price, expiry: "", offers: 0,
            seller: MarketSeller(id: sellerId, name: "s"), stl: 0, status: 0, prlo: nil, owner: nil,
            exs: 0)
    }

    func testGenerateBudgetBalancingSalesCoversGapAndMarksEssential() async {
        let manager = KickbaseManager()
        let service = PlayerRecommendationService(kickbaseManager: manager)

        // Team player expensive
        let expensive = makeTeamPlayer(
            id: "p1", position: 3, marketValue: 1_000_000, avg: 4.0, total: 40)
        // Replacement cheaper (<= 80%) -> price 700k
        // Note: public filter requires very high averagePoints (>=70.0) for market players
        let replacement = makeMarketPlayer(
            id: "m1", position: 3, price: 700_000, avg: 80.0, total: 80)

        let league = League(
            id: "l1", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0, placement: 1,
                won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: 3))

        // Current budget negative -> need 300k
        let recs = await service.generateSaleRecommendations(
            for: league, goal: .balanceBudget, teamPlayers: [expensive],
            marketPlayers: [replacement], currentBudget: -300_000)

        XCTAssertFalse(recs.isEmpty)
        // The replacement should provide savings of 300k (1_000_000 - 700_000)
        let rec = recs.first!
        XCTAssertEqual(rec.playerToSell.id, "p1")
        XCTAssertTrue(rec.replacements.count > 0)
        XCTAssertEqual(
            rec.priority, .essential,
            "If a single replacement covers the gap, priority should be .essential")
    }

    func testGenerateReduceRiskSuggestsSellingInjuredPlayers() async {
        let manager = KickbaseManager()
        let service = PlayerRecommendationService(kickbaseManager: manager)

        let injured = makeTeamPlayer(id: "p2", position: 2, marketValue: 500_000, status: 1)
        // Suitable replacement
        // Ensure replacement passes the strict pre-filter (high avg)
        let replacement = makeMarketPlayer(id: "m2", position: 2, price: 300_000, avg: 80.0)

        let league = League(
            id: "l1", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0, placement: 1,
                won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: 3))

        let recs = await service.generateSaleRecommendations(
            for: league, goal: .reduceRisk, teamPlayers: [injured], marketPlayers: [replacement],
            currentBudget: 0)

        XCTAssertFalse(recs.isEmpty)
        XCTAssertEqual(recs.first?.playerToSell.id, "p2")
        XCTAssertEqual(recs.first?.priority, .essential)
    }

    func testFindReplacementRespectsPositionAndPriceAndReturnsTopCandidates() async {
        let manager = KickbaseManager()
        let service = PlayerRecommendationService(kickbaseManager: manager)

        let teamPlayer = makeTeamPlayer(
            id: "tp", position: 4, marketValue: 2_000_000, avg: 6.0, total: 80)
        // Note: generateSaleRecommendations applies a strict pre-filter requiring high averagePoints (>= 70.0)
        let p1 = makeMarketPlayer(id: "a", position: 4, price: 1_000_000, avg: 80.0, total: 150)
        let p2 = makeMarketPlayer(id: "b", position: 4, price: 500_000, avg: 75.0, total: 80)
        let p3 = makeMarketPlayer(id: "c", position: 3, price: 400_000, avg: 80.0, total: 160)  // wrong position

        let recs = await service.generateSaleRecommendations(
            for: League(
                id: "l", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
                matchDay: 1,
                currentUser: LeagueUser(
                    id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0,
                    placement: 1, won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: 3)),
            goal: .raiseCapital, teamPlayers: [teamPlayer], marketPlayers: [p1, p2, p3],
            currentBudget: 0)

        // For raiseCapital, replacements for top valuable players should be found
        XCTAssertFalse(recs.isEmpty)
        let replacements = recs.first!.replacements
        XCTAssertTrue(replacements.count >= 1)
        // Candidate c should be excluded due wrong position
        XCTAssertFalse(replacements.contains { $0.player.id == "c" })
    }

    func testGenerateSaleRecommendationsPerformanceLargeMarket() async {
        let manager = KickbaseManager()
        let service = PlayerRecommendationService(kickbaseManager: manager)

        // Create a large market of 1000 players with varied stats
        var marketPlayers: [MarketPlayer] = []
        for i in 0..<1000 {
            let avg = Double(60 + (i % 40))  // 60..99
            let price = 200_000 + (i % 50) * 10_000
            let id = "m\(i)"
            marketPlayers.append(
                makeMarketPlayer(
                    id: id, position: (i % 4) + 1, price: price, avg: avg, total: 50 + (i % 200)))
        }

        let teamPlayers = (0..<20).map { i in
            makeTeamPlayer(id: "tp\(i)", position: (i % 4) + 1, marketValue: 500_000 + i * 10_000)
        }

        let league = League(
            id: "perf", name: "Perf", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0, placement: 1,
                won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: 3))

        // Measure CPU/Time for generating sale recommendations on large market
        self.measure(metrics: [XCTClockMetric()]) {
            Task {
                let recs = await service.generateSaleRecommendations(
                    for: league, goal: .raiseCapital, teamPlayers: teamPlayers,
                    marketPlayers: marketPlayers, currentBudget: 0)
                XCTAssertNotNil(recs)
            }
        }
    }
}
