import Foundation
import SwiftUI

@MainActor
class AuthenticationManager: ObservableObject {
    @Published var isAuthenticated = false
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var currentUser: User?
    
    private(set) var accessToken: String?
    private let baseURL = "https://api.kickbase.com"
    
    init() {
        loadStoredToken()
    }
    
    func login(email: String, password: String) async {
        isLoading = true
        errorMessage = nil
        
        do {
            let loginRequest = LoginRequest(email: email, password: password, loyalty: false, rep: [:])
            let url = URL(string: "\(baseURL)/v4/user/login")!
            
            var request = URLRequest(url: url)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.setValue("application/json", forHTTPHeaderField: "Accept")
            request.httpBody = try JSONEncoder().encode(loginRequest)
            
            print("🚀 Sending login request to: \(url)")
            print("📤 Request body: \(String(data: request.httpBody!, encoding: .utf8) ?? "No body")")
            
            let (data, response) = try await URLSession.shared.data(for: request)
            
            if let httpResponse = response as? HTTPURLResponse {
                print("📊 Status Code: \(httpResponse.statusCode)")
                print("📋 Response Headers: \(httpResponse.allHeaderFields)")
                print("📥 Response Data: \(String(data: data, encoding: .utf8) ?? "No data")")
                
                if httpResponse.statusCode == 200 {
                    // Parse JSON flexibly to handle different API formats
                    guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                        print("❌ Failed to parse JSON response")
                        throw AuthError.invalidResponse("Invalid JSON format")
                    }
                    
                    print("🔍 Parsed JSON: \(json)")
                    
                    // Extract token - try different possible field names
                    guard let token = json["tkn"] as? String ??
                                    json["token"] as? String ??
                                    json["accessToken"] as? String else {
                        print("❌ No token found in response")
                        throw AuthError.invalidResponse("No authentication token found")
                    }
                    
                    // Extract user data - try different possible field names
                    guard let userData = json["user"] as? [String: Any] ??
                                       json["u"] as? [String: Any] ??
                                       json["profile"] as? [String: Any] else {
                        print("❌ No user data found in response")
                        throw AuthError.invalidResponse("No user data found")
                    }
                    
                    print("👤 User data: \(userData)")
                    
                    // Create user object with flexible field mapping
                    let user = User(
                        id: userData["id"] as? String ??
                            userData["i"] as? String ??
                            userData["userId"] as? String ?? "",
                        name: userData["name"] as? String ??
                              userData["n"] as? String ??
                              userData["displayName"] as? String ?? "",
                        teamName: userData["teamName"] as? String ??
                                 userData["tn"] as? String ??
                                 userData["team"] as? String ?? "",
                        email: userData["email"] as? String ??
                               userData["em"] as? String ??
                               userData["mail"] as? String ?? email,
                        budget: userData["budget"] as? Int ??
                               userData["b"] as? Int ??
                               userData["money"] as? Int ?? 0,
                        teamValue: userData["teamValue"] as? Int ??
                                  userData["tv"] as? Int ??
                                  userData["value"] as? Int ?? 0,
                        points: userData["points"] as? Int ??
                               userData["p"] as? Int ??
                               userData["score"] as? Int ?? 0,
                        placement: userData["placement"] as? Int ??
                                  userData["pl"] as? Int ??
                                  userData["rank"] as? Int ?? 0,
                        flags: userData["flags"] as? Int ??
                              userData["f"] as? Int ?? 0
                    )
                    
                    print("✅ Login successful!")
                    print("🎯 Token: \(token.prefix(20))...")
                    print("👤 User: \(user.name) (\(user.email))")
                    
                    self.accessToken = token
                    self.currentUser = user
                    self.isAuthenticated = true
                    
                    storeToken(token)
                    
                } else {
                    // Handle error responses
                    var errorMsg = "Login fehlgeschlagen"
                    
                    if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                        if let message = json["message"] as? String ??
                                        json["error"] as? String ??
                                        json["msg"] as? String {
                            errorMsg = message
                        }
                    }
                    
                    print("❌ Server error (\(httpResponse.statusCode)): \(errorMsg)")
                    
                    if httpResponse.statusCode == 400 || httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
                        throw AuthError.invalidCredentials
                    } else {
                        throw AuthError.networkError
                    }
                }
            } else {
                throw AuthError.networkError
            }
            
        } catch {
            print("💥 Login error: \(error)")
            
            if let authError = error as? AuthError {
                errorMessage = authError.localizedDescription
            } else if error is DecodingError {
                errorMessage = "Unerwartete Serverantwort. Bitte versuchen Sie es erneut."
            } else if (error as NSError).domain == NSURLErrorDomain {
                errorMessage = "Netzwerkfehler. Bitte prüfen Sie Ihre Internetverbindung."
            } else {
                errorMessage = "Login fehlgeschlagen: \(error.localizedDescription)"
            }
        }
        
        isLoading = false
    }
    
    func logout() {
        print("👋 Logging out user")
        isAuthenticated = false
        currentUser = nil
        accessToken = nil
        removeStoredToken()
    }
    
    private func storeToken(_ token: String) {
        UserDefaults.standard.set(token, forKey: "kickbase_token")
        print("💾 Token stored securely")
    }
    
    private func loadStoredToken() {
        if let token = UserDefaults.standard.string(forKey: "kickbase_token") {
            accessToken = token
            print("🔑 Found stored token, validating...")
            Task {
                await validateToken()
            }
        }
    }
    
    private func removeStoredToken() {
        UserDefaults.standard.removeObject(forKey: "kickbase_token")
        print("🗑️ Stored token removed")
    }
    
    private func validateToken() async {
        guard let token = accessToken else { return }
        
        // Try different possible profile endpoints
        let possibleEndpoints = [
            "/v4/user/profile",
            "/v4/user/me",
            "/v4/user",
            "/user/profile",
            "/user/me"
        ]
        
        for endpoint in possibleEndpoints {
            let url = URL(string: "\(baseURL)\(endpoint)")!
            var request = URLRequest(url: url)
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            request.setValue("application/json", forHTTPHeaderField: "Accept")
            
            do {
                let (data, response) = try await URLSession.shared.data(for: request)
                
                if let httpResponse = response as? HTTPURLResponse,
                   httpResponse.statusCode == 200 {
                    
                    print("✅ Token validation successful with endpoint: \(endpoint)")
                    
                    // Try to extract user info if available
                    if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                       let userData = json["user"] as? [String: Any] ?? json as? [String: Any] {
                        
                        let user = User(
                            id: userData["id"] as? String ?? "",
                            name: userData["name"] as? String ?? "",
                            teamName: userData["teamName"] as? String ?? "",
                            email: userData["email"] as? String ?? "",
                            budget: userData["budget"] as? Int ?? 0,
                            teamValue: userData["teamValue"] as? Int ?? 0,
                            points: userData["points"] as? Int ?? 0,
                            placement: userData["placement"] as? Int ?? 0,
                            flags: userData["flags"] as? Int ?? 0
                        )
                        
                        self.currentUser = user
                    }
                    
                    self.isAuthenticated = true
                    return
                }
            } catch {
                print("❌ Token validation failed for \(endpoint): \(error)")
                continue
            }
        }
        
        print("❌ Token validation failed for all endpoints")
        logout()
    }
}

enum AuthError: Error, LocalizedError {
    case invalidCredentials
    case networkError
    case invalidResponse(String)
    
    var errorDescription: String? {
        switch self {
        case .invalidCredentials:
            return "Ungültige E-Mail oder Passwort"
        case .networkError:
            return "Netzwerkfehler. Bitte versuchen Sie es erneut."
        case .invalidResponse(let message):
            return "Serverantwort ungültig: \(message)"
        }
    }
}
