import XCTest

@testable import KickbaseCore

@MainActor
final class PlayerRecommendationIntegrationTests: XCTestCase {

    class MockPlayerService: KickbasePlayerService {
        var teamPlayersCalls = 0
        var marketPlayersCalls = 0

        var teamPlayersResult: [TeamPlayer] = []
        var marketPlayersResult: [MarketPlayer] = []

        override func loadTeamPlayers(for league: League) async throws -> [TeamPlayer] {
            teamPlayersCalls += 1
            return teamPlayersResult
        }

        override func loadMarketPlayers(for league: League) async throws -> [MarketPlayer] {
            marketPlayersCalls += 1
            return marketPlayersResult
        }

        override func getMatchDayStats(leagueId: String, playerId: String) async -> (
            smdc: Int, ismc: Int, smc: Int
        )? {
            return (smdc: 34, ismc: 10, smc: 8)
        }
    }

    func makeTeamPlayer(
        id: String, position: Int, marketValue: Int, avg: Double = 6.0, total: Int = 100
    ) -> TeamPlayer {
        return TeamPlayer(
            id: id, firstName: "F", lastName: "L", profileBigUrl: "", teamName: "T", teamId: "tm",
            position: position, number: 10, averagePoints: avg, totalPoints: total,
            marketValue: marketValue, marketValueTrend: 0, tfhmvt: 0, prlo: 0, stl: 0, status: 0,
            userOwnsPlayer: true)
    }

    func makeMarketPlayer(
        id: String, position: Int, price: Int, avg: Double = 80.0, total: Int = 150
    ) -> MarketPlayer {
        return MarketPlayer(
            id: id, firstName: "M", lastName: "P", profileBigUrl: "", teamName: "T", teamId: "tm2",
            position: position, number: 20, averagePoints: avg, totalPoints: total,
            marketValue: price, marketValueTrend: 0, price: price, expiry: "", offers: 0,
            seller: MarketSeller(id: "other", name: "s"), stl: 0, status: 0, prlo: nil, owner: nil,
            exs: 0)
    }

    func testGenerateRecommendationsUsesCacheAndClearCacheWorks() async throws {
        let mockPlayer = MockPlayerService(
            apiService: KickbaseAPIService(), dataParser: KickbaseDataParser())
        mockPlayer.teamPlayersResult = [
            makeTeamPlayer(id: "p1", position: 3, marketValue: 1_000_000)
        ]
        mockPlayer.marketPlayersResult = [makeMarketPlayer(id: "m1", position: 3, price: 500_000)]

        let manager = KickbaseManager(
            apiService: KickbaseAPIService(), dataParser: KickbaseDataParser(),
            playerService: mockPlayer)
        let service = PlayerRecommendationService(kickbaseManager: manager)

        let league = League(
            id: "L", name: "L", creatorName: "C", adminName: "A", created: "d", season: "s",
            matchDay: 1,
            currentUser: LeagueUser(
                id: "u", name: "n", teamName: "t", budget: 0, teamValue: 0, points: 0, placement: 1,
                won: 0, drawn: 0, lost: 0, se11: 0, ttm: 0, mpst: 3))

        // First call should call underlying services
        _ = try await service.generateRecommendations(for: league, budget: 0)
        XCTAssertEqual(mockPlayer.teamPlayersCalls, 1)
        XCTAssertEqual(mockPlayer.marketPlayersCalls, 1)

        // Second call should use cache (no additional calls)
        _ = try await service.generateRecommendations(for: league, budget: 0)
        XCTAssertEqual(mockPlayer.teamPlayersCalls, 1)
        XCTAssertEqual(mockPlayer.marketPlayersCalls, 1)

        // Clearing cache forces re-fetch
        service.clearCacheForLeague("L")
        _ = try await service.generateRecommendations(for: league, budget: 0)
        XCTAssertEqual(mockPlayer.teamPlayersCalls, 2)
        XCTAssertEqual(mockPlayer.marketPlayersCalls, 2)
    }
}
