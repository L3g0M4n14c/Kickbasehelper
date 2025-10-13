import Foundation
import SwiftUI

@MainActor
class AuthenticationManager: ObservableObject {
    @Published var isAuthenticated = false
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var currentUser: User?
    
    private(set) var accessToken: String?
    private let apiService = KickbaseAPIService()
    
    init() {
        loadStoredToken()
    }
    
    func login(email: String, password: String) async {
        isLoading = true
        errorMessage = nil
        
        do {
            print("🚀 Starting login process...")
            
            let loginResponse = try await apiService.login(email: email, password: password)
            
            print("✅ Login successful!")
            print("🎯 Token: \(loginResponse.tkn.prefix(20))...")
            
            // Set token for API service
            apiService.setAuthToken(loginResponse.tkn)
            self.accessToken = loginResponse.tkn
            
            // Wenn User-Daten in der Login-Response sind, verwende sie
            if let user = loginResponse.user {
                print("👤 User from login: \(user.name) (\(user.email))")
                self.currentUser = user
            } else {
                // Andernfalls erstelle einen Platzhalter-User
                // Die echten User-Daten werden beim Laden der Ligen geholt
                print("⚠️ No user data in login response, creating placeholder")
                self.currentUser = User(
                    id: loginResponse.userId ?? "unknown",
                    name: "User",
                    teamName: "",
                    email: email,
                    budget: 0,
                    teamValue: 0,
                    points: 0,
                    placement: 0,
                    flags: 0
                )
            }
            
            self.isAuthenticated = true
            storeToken(loginResponse.tkn)
            
        } catch let decodingError as DecodingError {
            print("💥 Login decoding error: \(decodingError)")
            
            // Detaillierte Fehlerinformation für Debugging
            switch decodingError {
            case .keyNotFound(let key, let context):
                print("❌ Missing key '\(key.stringValue)' - \(context.debugDescription)")
            case .typeMismatch(let type, let context):
                print("❌ Type mismatch for type '\(type)' - \(context.debugDescription)")
            case .valueNotFound(let type, let context):
                print("❌ Value not found for type '\(type)' - \(context.debugDescription)")
            case .dataCorrupted(let context):
                print("❌ Data corrupted - \(context.debugDescription)")
            @unknown default:
                print("❌ Unknown decoding error")
            }
            
            errorMessage = "Unerwartete Serverantwort. Bitte versuchen Sie es erneut."
        } catch {
            print("💥 Login error: \(error)")
            
            if let apiError = error as? APIError {
                errorMessage = apiError.localizedDescription
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
        
        apiService.setAuthToken(token)
        
        do {
            let userSettings = try await apiService.getUserSettings()
            print("✅ Token validation successful")
            
            // Try to extract user info if available
            if let userData = userSettings["user"] as? [String: Any] ?? userSettings as? [String: Any] {
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
        } catch {
            print("❌ Token validation failed: \(error)")
            logout()
        }
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
