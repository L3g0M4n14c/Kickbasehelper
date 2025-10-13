import Foundation

@MainActor
class KickbaseLeagueService: ObservableObject {
    private let apiService: KickbaseAPIService
    private let dataParser: KickbaseDataParser
    
    init(apiService: KickbaseAPIService, dataParser: KickbaseDataParser) {
        self.apiService = apiService
        self.dataParser = dataParser
    }
    
    // MARK: - League Loading
    
    func loadLeagues() async throws -> [League] {
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
    
}
