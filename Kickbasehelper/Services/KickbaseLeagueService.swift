import Foundation

@MainActor
class KickbaseLeagueService: ObservableObject {
    private let apiClient: KickbaseAPIClient
    private let dataParser: KickbaseDataParser
    
    init(apiClient: KickbaseAPIClient, dataParser: KickbaseDataParser) {
        self.apiClient = apiClient
        self.dataParser = dataParser
    }
    
    // MARK: - League Loading
    
    func loadLeagues() async throws -> [League] {
        print("🏆 Loading leagues...")
        
        let endpoints = [
            "/v4/leagues/selection",  // Offizieller Endpunkt laut Dokumentation
            "/leagues/selection",     // Fallback ohne v4 Präfix
            "/v4/leagues"            // Alternative
        ]
        
        do {
            let (_, json) = try await apiClient.tryMultipleEndpoints(endpoints: endpoints)
            let leagues = dataParser.parseLeaguesFromResponse(json)
            
            if leagues.isEmpty {
                // Fallback auf Mock-Daten
                return createMockLeagues()
            }
            
            return leagues
        } catch APIError.authenticationFailed {
            throw APIError.authenticationFailed
        } catch {
            print("❌ All API endpoints failed. Check network connection and API status.")
            throw APIError.allEndpointsFailed
        }
    }
    
    // MARK: - Mock Data Creation
    
    private func createMockLeagues() -> [League] {
        print("📝 Creating mock leagues for development...")
        
        let mockLeague = League(
            id: "mock-league-1",
            name: "Demo Liga (Mock)",
            creatorName: "Demo Admin",
            adminName: "Demo Admin",
            created: "2024-08-01T10:00:00Z",
            season: "2024/25",
            matchDay: 3,
            currentUser: LeagueUser(
                id: "mock-user-1",
                name: "Demo User",
                teamName: "Mein Demo Team",
                budget: 8000000,
                teamValue: 75000000,
                points: 150,
                placement: 5,
                won: 10,
                drawn: 5,
                lost: 3,
                se11: 0,
                ttm: 0
            )
        )
        
        print("✅ Mock league created: \(mockLeague.name)")
        return [mockLeague]
    }
}