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
        
        let endpoint = "/v4/leagues/selection"  // Offizieller Endpunkt laut Dokumentation
            
        
        do {
            let (data, httpResponse) = try await apiClient.makeRequest(endpoint: endpoint)
            
            if httpResponse.statusCode == 200 {
                if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                    let leagues = dataParser.parseLeaguesFromResponse(json)
                    
                    if leagues.isEmpty {
                        // Fallback auf Mock-Daten
                        //return createMockLeagues()
                        // TODO Fehlermeldung anzeigen
                    }
                    return leagues
                }
            }
        } catch {
            print("❌ All API endpoints failed. Check network connection and API status.")
            throw APIError.allEndpointsFailed
        }
        return []
    }
    
}
