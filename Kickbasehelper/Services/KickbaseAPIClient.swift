import Foundation

@MainActor
class KickbaseAPIClient: ObservableObject {
    private let baseURL = "https://api.kickbase.com"
    private var authToken: String?
    
    func setAuthToken(_ token: String) {
        authToken = token
        print("🔑 Auth token set for KickbaseAPIClient")
    }
    
    func hasAuthToken() -> Bool {
        return authToken != nil
    }
    
    // MARK: - Generic API Request Methods
    
    func makeRequest(
        endpoint: String,
        method: String = "GET",
        body: Data? = nil
    ) async throws -> (Data, HTTPURLResponse) {
        guard let token = authToken else {
            throw APIError.noAuthToken
        }
        
        guard let url = URL(string: "\(baseURL)\(endpoint)") else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.cachePolicy = .reloadIgnoringLocalCacheData
        
        if let body = body {
            request.httpBody = body
        }
        
        print("📤 Making \(method) request to: \(url)")
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.noHTTPResponse
        }
        
        print("📊 Response Status Code: \(httpResponse.statusCode)")
        
        if let responseString = String(data: data, encoding: .utf8) {
            print("📥 Response: \(responseString.prefix(500))")
        }
        
        return (data, httpResponse)
    }
    
    func tryMultipleEndpoints(
        endpoints: [String],
        method: String = "GET",
        body: Data? = nil
    ) async throws -> (Data, [String: Any]) {
        guard let token = authToken else {
            throw APIError.noAuthToken
        }
        
        var lastError: Error?
        
        for (index, endpoint) in endpoints.enumerated() {
            do {
                let (data, httpResponse) = try await makeRequest(
                    endpoint: endpoint,
                    method: method,
                    body: body
                )
                
                if httpResponse.statusCode == 200 {
                    if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                        print("✅ Found working endpoint (\(index + 1)/\(endpoints.count)): \(endpoint)")
                        return (data, json)
                    } else {
                        print("⚠️ Could not parse JSON from endpoint: \(endpoint)")
                        continue
                    }
                } else if httpResponse.statusCode == 401 {
                    throw APIError.authenticationFailed
                } else if httpResponse.statusCode == 404 {
                    print("⚠️ Endpoint \(endpoint) not found (404), trying next...")
                    continue
                } else if httpResponse.statusCode == 403 {
                    print("⚠️ Access forbidden (403) for endpoint \(endpoint)")
                    continue
                } else if httpResponse.statusCode >= 500 {
                    print("⚠️ Server error (\(httpResponse.statusCode)) for endpoint \(endpoint)")
                    continue
                } else {
                    print("⚠️ HTTP \(httpResponse.statusCode) for endpoint \(endpoint)")
                    continue
                }
            } catch {
                lastError = error
                print("❌ Network error with endpoint \(endpoint): \(error.localizedDescription)")
                continue
            }
        }
        
        if let error = lastError {
            throw error
        } else {
            throw APIError.allEndpointsFailed
        }
    }
    
    // MARK: - Network Testing
    
    func testNetworkConnectivity() async -> Bool {
        print("🌐 Testing network connectivity...")
        
        do {
            let url = URL(string: "\(baseURL)/")!
            var request = URLRequest(url: url)
            request.timeoutInterval = 5.0
            request.httpMethod = "HEAD"
            
            let (_, response) = try await URLSession.shared.data(for: request)
            
            if let httpResponse = response as? HTTPURLResponse {
                print("✅ Network test successful - Status: \(httpResponse.statusCode)")
                return true
            } else {
                print("⚠️ Network test - No HTTP response")
                return false
            }
        } catch {
            print("❌ Network test failed: \(error.localizedDescription)")
            return false
        }
    }
}

// MARK: - API Errors

enum APIError: Error, LocalizedError {
    case noAuthToken
    case invalidURL
    case noHTTPResponse
    case authenticationFailed
    case allEndpointsFailed
    case parsingFailed
    case networkError(String)
    
    var errorDescription: String? {
        switch self {
        case .noAuthToken:
            return "Kein Authentifizierungstoken verfügbar"
        case .invalidURL:
            return "Ungültige URL"
        case .noHTTPResponse:
            return "Keine HTTP-Antwort erhalten"
        case .authenticationFailed:
            return "Authentifizierung fehlgeschlagen. Token möglicherweise abgelaufen."
        case .allEndpointsFailed:
            return "Konnte keine Verbindung zur Kickbase API herstellen. Bitte überprüfen Sie Ihre Internetverbindung und versuchen Sie es später erneut."
        case .parsingFailed:
            return "Fehler beim Verarbeiten der Server-Antwort"
        case .networkError(let message):
            return "Netzwerkfehler: \(message)"
        }
    }
}