import Foundation
import SwiftUI

@MainActor
class KickbaseManager: ObservableObject {
    @Published var leagues: [League] = []
    @Published var selectedLeague: League?
    @Published var teamPlayers: [TeamPlayer] = []
    @Published var marketPlayers: [MarketPlayer] = []
    @Published var gifts: [Gift] = []
    @Published var userStats: UserStats?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private var authToken: String?
    private let baseURL = "https://api.kickbase.com"
    
    func setAuthToken(_ token: String) {
        authToken = token
        print("🔑 Auth token set for KickbaseManager")
    }
    
    func loadUserData() async {
        print("📊 Loading user data...")
        await loadLeagues()
        await loadGifts()
        
        // Forciere das Laden der UserStats nach Liga-Auswahl
        if let selectedLeague = selectedLeague {
            print("🔄 Force reloading user stats after league selection...")
            await loadUserStats(for: selectedLeague)
        }
    }
    
    func loadLeagues() async {
        isLoading = true
        errorMessage = nil
        
        print("🏆 Loading leagues...")
        
        guard let token = authToken else {
            print("❌ No auth token available")
            errorMessage = "Kein Authentifizierungstoken verfügbar"
            isLoading = false
            return
        }
        
        // Korrekte Kickbase API v4 Endpunkte basierend auf offizieller Dokumentation
        let endpoints = [
            "/v4/leagues/selection",  // Offizieller Endpunkt laut Dokumentation
            "/leagues/selection",     // Fallback ohne v4 Präfix
            "/v4/leagues"            // Alternative
        ]
        
        for endpoint in endpoints {
            do {
                let url = URL(string: "\(baseURL)\(endpoint)")!
                var request = URLRequest(url: url)
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                request.setValue("application/json", forHTTPHeaderField: "Accept")
                request.setValue("application/json", forHTTPHeaderField: "Content-Type")
                
                print("📤 Trying Request URL: \(url)")
                print("📤 Auth Header: Bearer \(token.prefix(10))...")
                
                let (data, response) = try await URLSession.shared.data(for: request)
                
                if let httpResponse = response as? HTTPURLResponse {
                    print("📊 Status Code (\(endpoint)): \(httpResponse.statusCode)")
                    
                    if let responseString = String(data: data, encoding: .utf8) {
                        print("📥 Response (\(endpoint)): \(responseString.prefix(1000))")
                    }
                    
                    if httpResponse.statusCode == 200 {
                        // Erfolgreiche Antwort - parse die Daten
                        if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                            print("✅ Found working endpoint: \(endpoint)")
                            print("📋 Available keys: \(Array(json.keys))")
                            
                            await parseLeaguesFromResponse(json)
                            isLoading = false
                            return
                        } else {
                            print("⚠️ Could not parse JSON from response")
                            continue
                        }
                    } else if httpResponse.statusCode == 401 {
                        errorMessage = "Authentifizierung fehlgeschlagen. Token möglicherweise abgelaufen."
                        print("❌ Authentication failed - token may be expired")
                        isLoading = false
                        return
                    } else if httpResponse.statusCode == 404 {
                        print("⚠️ Endpoint \(endpoint) not found (404), trying next...")
                        continue
                    } else if httpResponse.statusCode == 403 {
                        print("⚠️ Access forbidden (403) for endpoint \(endpoint)")
                        continue
                    } else {
                        print("⚠️ HTTP \(httpResponse.statusCode) for endpoint \(endpoint)")
                        continue
                    }
                } else {
                    print("⚠️ No HTTP response for endpoint \(endpoint)")
                    continue
                }
            } catch {
                print("❌ Network error with endpoint \(endpoint): \(error.localizedDescription)")
                continue
            }
        }
        
        // Wenn alle Endpunkte fehlschlagen, zeige Fehlermeldung statt Mock-Daten
        print("❌ All API endpoints failed. Check network connection and API status.")
        errorMessage = "Konnte keine Verbindung zur Kickbase API herstellen. Bitte überprüfen Sie Ihre Internetverbindung und versuchen Sie es später erneut."
        isLoading = false
    }
    
    func parseLeaguesFromResponse(_ json: [String: Any]) async {
        print("🔍 Parsing leagues response...")
        print("📋 Raw JSON keys: \(Array(json.keys))")
        print("📋 Raw JSON content: \(json)")
        
        var leaguesArray: [[String: Any]] = []
        
        // Versuche verschiedene mögliche Response-Formate
        if let leagues = json["leagues"] as? [[String: Any]] {
            leaguesArray = leagues
            print("✅ Found leagues array with \(leagues.count) entries")
        } else if let data = json["data"] as? [[String: Any]] {
            leaguesArray = data
            print("✅ Found data array with \(data.count) entries")
        } else if let leagues = json["l"] as? [[String: Any]] {
            leaguesArray = leagues
            print("✅ Found l array with \(leagues.count) entries")
        } else if let it = json["it"] as? [[String: Any]] {
            // "it" könnte "items" bedeuten
            leaguesArray = it
            print("✅ Found it array with \(it.count) entries")
        } else if let anol = json["anol"] as? [[String: Any]] {
            // "anol" ist ein unbekannter Key, versuchen wir es
            leaguesArray = anol
            print("✅ Found anol array with \(anol.count) entries")
        } else if json.keys.contains("id") {
            // Single league response
            leaguesArray = [json]
            print("✅ Found single league response")
        } else {
            // Erweiterte Behandlung für "it" und "anol" Keys
            print("🔍 Checking alternative formats for it/anol keys...")
            
            // Prüfe ob "it" ein Dictionary oder ein anderer Datentyp ist
            if let it = json["it"] {
                print("🔍 Found 'it' key with type: \(type(of: it))")
                
                if let itDict = it as? [String: Any] {
                    print("✅ 'it' is a dictionary with keys: \(Array(itDict.keys))")
                    // Durchsuche das "it" Dictionary nach Arrays oder verwende es direkt als Liga
                    for (key, value) in itDict {
                        if let array = value as? [[String: Any]] {
                            leaguesArray = array
                            print("✅ Found leagues in it[\(key)] with \(array.count) entries")
                            break
                        }
                    }
                    
                    // Falls kein Array gefunden, verwende das Dictionary selbst als Liga
                    if leaguesArray.isEmpty {
                        leaguesArray = [itDict]
                        print("✅ Using 'it' dictionary as single league entry")
                    }
                } else if let itArray = it as? [[String: Any]] {
                    leaguesArray = itArray
                    print("✅ Found 'it' as direct array with \(itArray.count) entries")
                } else if let itString = it as? String {
                    print("ℹ️ 'it' is a string: \(itString)")
                } else {
                    print("⚠️ 'it' has unsupported type: \(type(of: it))")
                }
            }
            
            // Ähnliche Behandlung für "anol"
            if leaguesArray.isEmpty, let anol = json["anol"] {
                print("🔍 Found 'anol' key with type: \(type(of: anol))")
                
                if let anolDict = anol as? [String: Any] {
                    print("✅ 'anol' is a dictionary with keys: \(Array(anolDict.keys))")
                    // Durchsuche das "anol" Dictionary nach Arrays oder verwende es direkt als Liga
                    for (key, value) in anolDict {
                        if let array = value as? [[String: Any]] {
                            leaguesArray = array
                            print("✅ Found leagues in anol[\(key)] with \(array.count) entries")
                            break
                        }
                    }
                    
                    // Falls kein Array gefunden, verwende das Dictionary selbst als Liga
                    if leaguesArray.isEmpty {
                        leaguesArray = [anolDict]
                        print("✅ Using 'anol' dictionary as single league entry")
                    }
                } else if let anolArray = anol as? [[String: Any]] {
                    leaguesArray = anolArray
                    print("✅ Found 'anol' as direct array with \(anolArray.count) entries")
                } else if let anolString = anol as? String {
                    print("ℹ️ 'anol' is a string: \(anolString)")
                } else {
                    print("⚠️ 'anol' has unsupported type: \(type(of: anol))")
                }
            }
            
            // Als letzter Versuch: Suche alle Keys nach Arrays ab
            if leaguesArray.isEmpty {
                print("🔍 Searching all keys for array data...")
                for (key, value) in json {
                    if let array = value as? [[String: Any]] {
                        leaguesArray = array
                        print("✅ Found leagues in [\(key)] with \(array.count) entries")
                        break
                    } else if let dict = value as? [String: Any], !dict.isEmpty {
                        // Falls es ein einzelnes Dictionary ist, könnte es eine Liga sein
                        if key != "it" && key != "anol" { // Nur wenn wir diese bereits geprüft haben
                            leaguesArray = [dict]
                            print("✅ Using dictionary in [\(key)] as single league")
                            break
                        }
                    }
                }
            }
            
            // Falls immer noch nichts gefunden, versuche die gesamte Antwort als Liga zu interpretieren
            if leaguesArray.isEmpty {
                print("⚠️ Could not find league data in known formats")
                print("📋 Attempting to use entire response as single league...")
                
                // Prüfe ob das JSON selbst Liga-ähnliche Daten enthält
                if json.keys.contains("id") || json.keys.contains("name") ||
                   json.keys.contains("i") || json.keys.contains("n") {
                    leaguesArray = [json]
                    print("✅ Using entire response as single league")
                } else {
                    print("❌ Unknown response format. Keys: \(Array(json.keys))")
                    print("📋 Full response structure:")
                    for (key, value) in json {
                        print("   - \(key): \(type(of: value))")
                    }
                    
                    // Fallback auf Mock-Daten
                    await createMockLeagues()
                    return
                }
            }
        }
        
        var parsedLeagues: [League] = []
        
        for (index, leagueData) in leaguesArray.enumerated() {
            print("🔄 Parsing league \(index + 1): \(Array(leagueData.keys))")
            
            // Debug: Zeige alle verfügbaren Keys
            print("📋 Available league keys: \(leagueData.keys.sorted())")
            
            // Parse currentUser mit noch flexibleren Feldnamen (inklusive "it", "anol" Keys)
            var currentUser = LeagueUser(
                id: "unknown",
                name: "Unknown",
                teamName: "Unknown Team",
                budget: 5000000,
                teamValue: 50000000,
                points: 0,
                placement: 1,
                won: 0,
                drawn: 0,
                lost: 0,
                se11: 0,
                ttm: 0
            )
            
            if let userData = leagueData["currentUser"] as? [String: Any] ??
                              leagueData["cu"] as? [String: Any] ??
                              leagueData["user"] as? [String: Any] ??
                              leagueData["it"] as? [String: Any] ??
                              leagueData["anol"] as? [String: Any] {
                
                // Debug: Zeige verfügbare User-Keys
                print("👤 Available user keys: \(userData.keys.sorted())")
                
                // Prüfe verschiedene mögliche Feldnamen für teamName
                let possibleTeamNames = [
                    userData["teamName"] as? String,
                    userData["tn"] as? String,
                    userData["team_name"] as? String,
                    userData["tname"] as? String,
                    userData["club"] as? String,
                    userData["clubName"] as? String,
                    userData["teamname"] as? String
                ].compactMap { $0 }
                
                let teamName = possibleTeamNames.first ?? "Team"
                
                print("🏆 Found team name: '\(teamName)' from keys: \(possibleTeamNames)")
                
                currentUser = LeagueUser(
                    id: userData["id"] as? String ?? userData["i"] as? String ?? "unknown",
                    name: userData["name"] as? String ?? userData["n"] as? String ?? "User",
                    teamName: teamName,
                    budget: userData["budget"] as? Int ?? userData["b"] as? Int ?? 5000000,
                    teamValue: userData["teamValue"] as? Int ?? userData["tv"] as? Int ?? 50000000,
                    points: userData["points"] as? Int ?? userData["p"] as? Int ?? 0,
                    placement: userData["placement"] as? Int ?? userData["pl"] as? Int ?? 1,
                    won: userData["won"] as? Int ?? userData["w"] as? Int ?? 0,
                    drawn: userData["drawn"] as? Int ?? userData["d"] as? Int ?? 0,
                    lost: userData["lost"] as? Int ?? userData["l"] as? Int ?? 0,
                    se11: userData["se11"] as? Int ?? userData["s"] as? Int ?? 0,
                    ttm: userData["ttm"] as? Int ?? userData["t"] as? Int ?? 0
                )
                print("✅ Parsed user: \(currentUser.name) - \(currentUser.teamName)")
            } else {
                print("❌ No user data found in league data")
            }
            
            let league = League(
                id: leagueData["id"] as? String ?? leagueData["i"] as? String ?? UUID().uuidString,
                name: leagueData["name"] as? String ?? leagueData["n"] as? String ?? "Liga \(index + 1)",
                creatorName: leagueData["creatorName"] as? String ?? leagueData["cn"] as? String ?? "",
                adminName: leagueData["adminName"] as? String ?? leagueData["an"] as? String ?? "",
                created: leagueData["created"] as? String ?? leagueData["c"] as? String ?? "",
                season: leagueData["season"] as? String ?? leagueData["s"] as? String ?? "2024/25",
                matchDay: leagueData["matchDay"] as? Int ?? leagueData["md"] as? Int ?? 1,
                currentUser: currentUser
            )
            
            parsedLeagues.append(league)
            print("✅ Parsed league: \(league.name)")
        }
        
        leagues = parsedLeagues
        
        // Auto-select erste Liga falls keine ausgewählt
        if selectedLeague == nil && !leagues.isEmpty {
            selectedLeague = leagues.first
            print("🎯 Auto-selected league: \(leagues.first!.name)")
            await selectLeague(leagues.first!)
        }
        
        print("🏆 Successfully loaded \(leagues.count) leagues")
    }
    
    func createMockLeagues() async {
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
        
        leagues = [mockLeague]
        selectedLeague = mockLeague
        
        // NICHT hier Mock UserStats erstellen - das überschreibt echte API-Daten
        // userStats wird nur durch loadUserStats() gesetzt
        
        // Mock some team players
        teamPlayers = [
            TeamPlayer(
                id: "mock-player-1",
                firstName: "Max",
                lastName: "Mustermann",
                profileBigUrl: "",
                teamName: "FC Demo",
                teamId: "40", // Union Berlin
                position: 4,
                number: 9,
                averagePoints: 5.0,
                totalPoints: 45,
                marketValue: 15000000,
                marketValueTrend: 500000,
                tfhmvt: 250000, // Marktwertänderung seit letztem Update
                stl: 0, // Nicht verletzt
                status: 0,
                userOwnsPlayer: true
            ),
            TeamPlayer(
                id: "mock-player-2",
                firstName: "Hans",
                lastName: "Beispiel",
                profileBigUrl: "",
                teamName: "Demo United",
                teamId: "1", // Bayern München
                position: 3,
                number: 10,
                averagePoints: 4.2,
                totalPoints: 38,
                marketValue: 12000000,
                marketValueTrend: -200000,
                tfhmvt: -150000, // Marktwertänderung seit letztem Update
                stl: 1, // Verletzt (rotes Kreuz anzeigen)
                status: 0,
                userOwnsPlayer: true
            ),
            TeamPlayer(
                id: "mock-player-3",
                firstName: "Peter",
                lastName: "Verletzt",
                profileBigUrl: "",
                teamName: "FC Testverein",
                teamId: "2", // Dortmund
                position: 2,
                number: 5,
                averagePoints: 3.8,
                totalPoints: 30,
                marketValue: 8000000,
                marketValueTrend: -1000000,
                tfhmvt: -500000,
                stl: 1, // Verletzt (rotes Kreuz anzeigen)
                status: 0,
                userOwnsPlayer: true
            ),
            TeamPlayer(
                id: "mock-player-4",
                firstName: "Klaus",
                lastName: "Gesund",
                profileBigUrl: "",
                teamName: "SV Musterstadt",
                teamId: "3", // Leipzig
                position: 1,
                number: 1,
                averagePoints: 4.5,
                totalPoints: 40,
                marketValue: 6000000,
                marketValueTrend: 200000,
                tfhmvt: 100000,
                stl: 0, // Nicht verletzt
                status: 0,
                userOwnsPlayer: true
            )
        ]
        
        // Mock some gifts
        gifts = [
            Gift(id: "mock-gift-1", type: "daily", amount: 50000, level: 1, collected: false),
            Gift(id: "mock-gift-2", type: "daily", amount: 50000, level: 2, collected: false),
            Gift(id: "mock-gift-3", type: "daily", amount: 50000, level: 3, collected: true)
        ]
        
        print("✅ Mock data created successfully (without UserStats override)")
        print("📊 Mock league: \(mockLeague.name)")
        print("👥 Mock team players: \(teamPlayers.count)")
        print("🎁 Mock gifts: \(gifts.count)")
        
        // Lade echte UserStats für die Mock-Liga
        await loadUserStats(for: mockLeague)
    }
    
    func selectLeague(_ league: League) async {
        print("🎯 Selecting league: \(league.name)")
        selectedLeague = league
        
        // Load league-specific data
        await loadUserStats(for: league)
        await loadTeamPlayers(for: league)
        await loadMarketPlayers(for: league)
    }
    
    func loadUserStats(for league: League) async {
        print("📊 Loading user stats for league: \(league.name)")
        
        guard let token = authToken else {
            print("❌ No auth token available for user stats")
            return
        }
        
        // Verwende eine dedizierte URLSession um Cancellation-Probleme zu vermeiden
        let sessionConfig = URLSessionConfiguration.default
        sessionConfig.timeoutIntervalForRequest = 30.0
        sessionConfig.timeoutIntervalForResource = 60.0
        sessionConfig.waitsForConnectivity = true
        let session = URLSession(configuration: sessionConfig)
        
        // Korrigierte Endpoints - Budget-Endpoint wieder priorisiert
        let endpoints = [
            "/v4/leagues/\(league.id)/me/budget",    // Spezifischer Budget-Endpoint
            "/v4/leagues/\(league.id)/me",           // Meine Daten in Liga (v4)
            "/v4/leagues/\(league.id)",              // Liga-Details (enthält oft User-Info)
            "/v4/leagues/\(league.id)/users/me",     // User-spezifische Daten (v4)
            "/v4/user/profile",                      // User-Profil (v4)
            "/v4/user/leagues/\(league.id)",         // User-Liga-Kombination (v4)
            "/v4/user",                              // User-Endpoint (v4)
            "/v4/leagues/\(league.id)/stats",        // Liga-Statistiken (v4)
            "/leagues/\(league.id)/me/budget"        // Budget-Fallback ohne v4
        ]
        
        var lastError: Error?
        
        for (index, endpoint) in endpoints.enumerated() {
            // Prüfe ob die Task noch läuft
            if Task.isCancelled {
                print("⚠️ UserStats loading was cancelled")
                break
            }
            
            do {
                let url = URL(string: "\(baseURL)\(endpoint)")!
                var request = URLRequest(url: url)
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                request.setValue("application/json", forHTTPHeaderField: "Accept")
                request.setValue("application/json", forHTTPHeaderField: "Content-Type")
                request.cachePolicy = .reloadIgnoringLocalCacheData
                
                print("📤 Trying User Stats URL (\(index + 1)/\(endpoints.count)): \(url)")
                
                // Verwende die dedizierte Session
                let (data, response) = try await session.data(for: request)
                
                if let httpResponse = response as? HTTPURLResponse {
                    print("📊 User Stats Status Code (\(endpoint)): \(httpResponse.statusCode)")
                    
                    if let responseString = String(data: data, encoding: .utf8) {
                        print("📥 User Stats Response (\(endpoint)): \(responseString.prefix(800))")
                    }
                    
                    if httpResponse.statusCode == 200 {
                        if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                            print("✅ Found working user stats endpoint: \(endpoint)")
                            print("📋 Available stats keys: \(Array(json.keys))")
                            
                            // Versuche User-Stats aus verschiedenen möglichen Strukturen zu parsen
                            await parseUserStatsFromResponse(json, fallbackUser: league.currentUser)
                            session.invalidateAndCancel()
                            return
                        } else {
                            print("⚠️ Could not parse JSON from stats response")
                            continue
                        }
                    } else if httpResponse.statusCode == 401 {
                        print("❌ Authentication failed for user stats - token may be expired")
                        // Bei 401 alle weiteren Versuche abbrechen
                        break
                    } else if httpResponse.statusCode == 404 {
                        print("⚠️ Stats endpoint \(endpoint) not found (404), trying next...")
                        continue
                    } else if httpResponse.statusCode == 403 {
                        print("⚠️ Access forbidden (403) for stats endpoint \(endpoint)")
                        continue
                    } else if httpResponse.statusCode >= 500 {
                        print("⚠️ Server error (\(httpResponse.statusCode)) for stats endpoint \(endpoint)")
                        continue
                    } else {
                        print("⚠️ HTTP \(httpResponse.statusCode) for stats endpoint \(endpoint)")
                        continue
                    }
                } else {
                    print("⚠️ No HTTP response for stats endpoint \(endpoint)")
                    continue
                }
            } catch {
                lastError = error
                
                // Detaillierte Error-Analyse für -999 (NSURLErrorCancelled)
                if let urlError = error as? URLError {
                    switch urlError.code {
                    case .cancelled:
                        print("❌ Request was cancelled (URLError -999) for endpoint \(endpoint)")
                        print("   This often happens due to task cancellation or app backgrounding")
                        // Bei Cancellation nicht alle Endpunkte abbrechen, sondern weitermachen
                        continue
                    case .notConnectedToInternet:
                        print("   🌐 No internet connection")
                    case .timedOut:
                        print("   ⏰ Request timed out")
                    case .cannotFindHost:
                        print("   🔍 Cannot find host (DNS issue)")
                    case .networkConnectionLost:
                        print("   📡 Network connection lost")
                    case .cannotConnectToHost:
                        print("   🚫 Cannot connect to host (server unreachable)")
                    case .badURL:
                        print("   🔗 Bad URL format")
                    case .unsupportedURL:
                        print("   ❌ Unsupported URL scheme")
                    default:
                        print("❌ Network error with stats endpoint \(endpoint): URLError code: \(urlError.code.rawValue)")
                        print("   Error: \(urlError.localizedDescription)")
                    }
                } else if let nsError = error as NSError? {
                    print("❌ Network error with stats endpoint \(endpoint): \(nsError.localizedDescription)")
                    print("   🔍 NSError domain: \(nsError.domain), code: \(nsError.code)")
                } else {
                    print("❌ Network error with stats endpoint \(endpoint): \(error.localizedDescription)")
                }
                continue
            }
        }
        
        // Session cleanup
        session.invalidateAndCancel()
        
        print("⚠️ All user stats endpoints failed")
        if let error = lastError {
            print("   Last error: \(error.localizedDescription)")
        }
        
        // Fallback: Verwende Liga-User-Daten wenn API fehlschlägt
        print("📊 Using league user data as fallback for user stats")
        userStats = UserStats(
            teamValue: league.currentUser.teamValue,
            teamValueTrend: 0,
            budget: league.currentUser.budget,
            points: league.currentUser.points,
            placement: league.currentUser.placement,
            won: league.currentUser.won,
            drawn: league.currentUser.drawn,
            lost: league.currentUser.lost
        )
        print("📊 Fallback stats applied - Budget: €\(league.currentUser.budget/1000)k, Teamwert: €\(league.currentUser.teamValue/1000)k")
        
        // Debugging: Aktueller Status
        debugCurrentState()
    }
    
    func parseUserStatsFromResponse(_ json: [String: Any], fallbackUser: LeagueUser) async {
        print("🔍 Parsing user stats from response...")
        print("📋 Stats JSON keys: \(Array(json.keys))")
        print("📋 Full JSON response: \(json)")
        
        var statsData: [String: Any] = json
        
        // Prüfe auf verschachtelte Strukturen
        if let user = json["user"] as? [String: Any] {
            print("✅ Found 'user' object")
            statsData = user
        } else if let me = json["me"] as? [String: Any] {
            print("✅ Found 'me' object")
            statsData = me
        } else if let data = json["data"] as? [String: Any] {
            print("✅ Found 'data' object")
            statsData = data
        } else if let team = json["team"] as? [String: Any] {
            print("✅ Found 'team' object")
            statsData = team
        } else if let league = json["league"] as? [String: Any] {
            print("✅ Found 'league' object")
            statsData = league
        }
        
        // Parse mit erweiterten Feldnamen basierend auf Budget-API
        // b = Budget (aktuelles verfügbares Budget)
        // pbas = Previous Budget At Start (ursprüngliches Budget)
        // bs = Budget Start oder Budget Spent
        let teamValue = extractInt(from: statsData, keys: ["teamValue", "tv", "marketValue", "mv", "value"]) ?? fallbackUser.teamValue
        let teamValueTrend = extractInt(from: statsData, keys: ["teamValueTrend", "tvt", "marketValueTrend", "mvt", "trend", "t"]) ?? 0
        let budget = extractInt(from: statsData, keys: ["b", "budget", "money", "cash", "funds"]) ?? fallbackUser.budget
        let points = extractInt(from: statsData, keys: ["points", "p", "totalPoints", "tp"]) ?? fallbackUser.points
        let placement = extractInt(from: statsData, keys: ["placement", "pl", "rank", "position", "pos"]) ?? fallbackUser.placement
        let won = extractInt(from: statsData, keys: ["won", "w", "wins", "victories"]) ?? fallbackUser.won
        let drawn = extractInt(from: statsData, keys: ["drawn", "d", "draws", "ties"]) ?? fallbackUser.drawn
        let lost = extractInt(from: statsData, keys: ["lost", "l", "losses", "defeats"]) ?? fallbackUser.lost
        
        // Debug: Zeige alle verfügbaren Budget-relevanten Felder
        print("🔍 Budget-related fields found:")
        if let b = statsData["b"] { print("   b (Budget): \(b)") }
        if let pbas = statsData["pbas"] { print("   pbas (Previous Budget At Start): \(pbas)") }
        if let bs = statsData["bs"] { print("   bs (Budget Start/Spent): \(bs)") }
        if let teamVal = statsData["teamValue"] { print("   teamValue: \(teamVal)") }
        if let tv = statsData["tv"] { print("   tv (Team Value): \(tv)") }
        if let value = statsData["value"] { print("   value: \(value)") }
        
        userStats = UserStats(
            teamValue: teamValue,
            teamValueTrend: teamValueTrend,
            budget: budget,
            points: points,
            placement: placement,
            won: won,
            drawn: drawn,
            lost: lost
        )
        
        print("✅ User stats parsed successfully:")
        print("   💰 Budget: €\(budget/1000)k (from field: 'b')")
        print("   📈 Teamwert: €\(teamValue/1000)k")
        print("   🔄 Trend: €\(teamValueTrend/1000)k")
        print("   🏆 Punkte: \(points) (Platz \(placement))")
        print("   📊 Bilanz: \(won)S-\(drawn)U-\(lost)N")
        
        // Falls der Teamwert nicht im Budget-Endpoint ist, lade ihn separat
        if teamValue == fallbackUser.teamValue {
            print("⚠️ Team value not found in budget endpoint, will try to load separately")
            // Hier könnten wir einen separaten Teamwert-Endpoint aufrufen
        }
    }
    
    // Hilfsmethode zum Extrahieren von Int-Werten aus verschiedenen möglichen Keys
    private func extractInt(from data: [String: Any], keys: [String]) -> Int? {
        for key in keys {
            if let value = data[key] as? Int {
                return value
            } else if let value = data[key] as? Double {
                return Int(value)
            } else if let value = data[key] as? String, let intValue = Int(value) {
                return intValue
            }
        }
        return nil
    }
    
    // Hilfsmethode zum Extrahieren von String-Werten
    private func extractString(from data: [String: Any], keys: [String]) -> String? {
        for key in keys {
            if let value = data[key] as? String {
                return value
            }
        }
        return nil
    }
    
    // MARK: - Punktzahl-Extraktions-Helper-Funktionen
    
    private func extractTotalPoints(from playerData: [String: Any]) -> Int {
        let possibleKeys = [
            "p",                                 // Hauptfeld für Gesamtpunkte laut User
            "totalPoints", "tp", "points", "pts", "totalPts",
            "gesamtpunkte", "total", "score", "seasonPoints", "sp"
        ]
        
        for key in possibleKeys {
            if let value = playerData[key] as? Int {
                print("   ✅ Found totalPoints in field '\(key)': \(value)")
                return value
            } else if let value = playerData[key] as? Double {
                print("   ✅ Found totalPoints in field '\(key)': \(Int(value))")
                return Int(value)
            } else if let value = playerData[key] as? String, let intValue = Int(value) {
                print("   ✅ Found totalPoints in field '\(key)': \(intValue)")
                return intValue
            }
        }
        
        print("   ⚠️ No totalPoints found in any field")
        return 0 // Fallback wenn keine Punktzahl gefunden wird
    }
    
    private func extractAveragePoints(from playerData: [String: Any]) -> Double {
        let possibleKeys = [
            "averagePoints", "ap", "avgPoints", "durchschnitt",
            "avg", "averageScore", "avgp", "avp"
        ]
        
        for key in possibleKeys {
            if let value = playerData[key] as? Double {
                print("   ✅ Found averagePoints in field '\(key)': \(value)")
                return value
            } else if let value = playerData[key] as? Int {
                print("   ✅ Found averagePoints in field '\(key)': \(Double(value))")
                return Double(value)
            } else if let value = playerData[key] as? String, let doubleValue = Double(value) {
                print("   ✅ Found averagePoints in field '\(key)': \(doubleValue)")
                return doubleValue
            }
        }
        
        print("   ⚠️ No averagePoints found in any field")
        return 0.0 // Fallback wenn keine Durchschnittspunktzahl gefunden wird
    }
    
    func loadTeamPlayers(for league: League) async {
        print("👥 Loading team players (squad) for league: \(league.name)")
        
        guard let token = authToken else {
            print("❌ No auth token available for team players")
            return
        }
        
        // Verwende eine dedizierte URLSession um Cancellation-Probleme zu vermeiden
        let sessionConfig = URLSessionConfiguration.default
        sessionConfig.timeoutIntervalForRequest = 30.0
        sessionConfig.timeoutIntervalForResource = 60.0
        sessionConfig.waitsForConnectivity = true
        let session = URLSession(configuration: sessionConfig)
        
        // Prioritätsliste der Squad/Team Endpoints
        let endpoints = [
            "/v4/leagues/\(league.id)/squad",       // Hauptendpoint für Team/Squad
            "/v4/leagues/\(league.id)/lineup",      // Alternative für Aufstellung
            "/v4/leagues/\(league.id)/me/players",  // Meine Spieler
            "/leagues/\(league.id)/squad",          // Fallback ohne v4
            "/leagues/\(league.id)/lineup",         // Fallback Lineup ohne v4
            "/v4/leagues/\(league.id)/lineups"      // Alternative mit Plural
        ]
        
        var lastError: Error?
        
        for (index, endpoint) in endpoints.enumerated() {
            // Prüfe ob die Task noch läuft
            if Task.isCancelled {
                print("⚠️ Team players loading was cancelled")
                break
            }
            
            do {
                let url = URL(string: "\(baseURL)\(endpoint)")!
                var request = URLRequest(url: url)
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                request.setValue("application/json", forHTTPHeaderField: "Accept")
                request.setValue("application/json", forHTTPHeaderField: "Content-Type")
                request.cachePolicy = .reloadIgnoringLocalCacheData
                
                print("📤 Trying Team Players URL (\(index + 1)/\(endpoints.count)): \(url)")
                
                // Verwende die dedizierte Session
                let (data, response) = try await session.data(for: request)
                
                if let httpResponse = response as? HTTPURLResponse {
                    print("📊 Team Players Status Code (\(endpoint)): \(httpResponse.statusCode)")
                    
                    if let responseString = String(data: data, encoding: .utf8) {
                        print("📥 Team Players Response (\(endpoint)): \(responseString.prefix(800))")
                    }
                    
                    if httpResponse.statusCode == 200 {
                        if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                            print("✅ Found working team players endpoint: \(endpoint)")
                            print("📋 Available team keys: \(Array(json.keys))")
                            
                            await parseTeamPlayersFromResponse(json)
                            session.invalidateAndCancel()
                            return
                        } else {
                            print("⚠️ Could not parse JSON from team players response")
                            continue
                        }
                    } else if httpResponse.statusCode == 401 {
                        print("❌ Authentication failed for team players - token may be expired")
                        break
                    } else if httpResponse.statusCode == 404 {
                        print("⚠️ Team players endpoint \(endpoint) not found (404), trying next...")
                        continue
                    } else if httpResponse.statusCode == 403 {
                        print("⚠️ Access forbidden (403) for team players endpoint \(endpoint)")
                        continue
                    } else if httpResponse.statusCode >= 500 {
                        print("⚠️ Server error (\(httpResponse.statusCode)) for team players endpoint \(endpoint)")
                        continue
                    } else {
                        print("⚠️ HTTP \(httpResponse.statusCode) for team players endpoint \(endpoint)")
                        continue
                    }
                } else {
                    print("⚠️ No HTTP response for team players endpoint \(endpoint)")
                    continue
                }
            } catch {
                lastError = error
                print("❌ Network error with team players endpoint \(endpoint): \(error.localizedDescription)")
                continue
            }
        }
        
        // Session cleanup
        session.invalidateAndCancel()
        
        print("⚠️ All team players endpoints failed")
        if let error = lastError {
            print("   Last error: \(error.localizedDescription)")
        }
        
        print("📦 Keeping existing team players or setting empty array")
        if teamPlayers.isEmpty {
            teamPlayers = []
        }
    }
    
    func parseTeamPlayersFromResponse(_ json: [String: Any]) async {
        print("🔍 Parsing team players from response...")
        print("📋 Team JSON keys: \(Array(json.keys))")
        
        // Debug: Zeige die komplette Response-Struktur
        print("📋 Detaillierte Response-Struktur:")
        for (key, value) in json {
            print("   \(key): \(type(of: value))")
            if let dict = value as? [String: Any] {
                print("     → Dictionary mit Keys: \(Array(dict.keys))")
            } else if let array = value as? [Any] {
                print("     → Array mit \(array.count) Elementen")
                if let firstElement = array.first {
                    print("     → Erstes Element Type: \(type(of: firstElement))")
                    if let firstDict = firstElement as? [String: Any] {
                        print("     → Erstes Element Keys: \(Array(firstDict.keys))")
                    }
                }
            } else {
                print("     → Wert: \(value)")
            }
        }
        
        var playersArray: [[String: Any]] = []
        
        // Erweiterte Suche - versuche ALLE möglichen Strukturen
        print("🔍 Suche nach Spieler-Daten...")
        
        // 1. Direkte Arrays
        if let players = json["players"] as? [[String: Any]] {
            playersArray = players
            print("✅ Found 'players' array with \(players.count) entries")
        } else if let squad = json["squad"] as? [[String: Any]] {
            playersArray = squad
            print("✅ Found 'squad' array with \(squad.count) entries")
        } else if let data = json["data"] as? [[String: Any]] {
            playersArray = data
            print("✅ Found 'data' array with \(data.count) entries")
        }
        
        // 2. Verschachtelte Strukturen
        else if let lineup = json["lineup"] as? [String: Any] {
            print("🔍 Found 'lineup' object, checking nested structure...")
            if let players = lineup["players"] as? [[String: Any]] {
                playersArray = players
                print("✅ Found 'lineup.players' array with \(players.count) entries")
            } else {
                print("📋 Lineup Keys: \(Array(lineup.keys))")
                // Suche in allen lineup keys nach Arrays
                for (key, value) in lineup {
                    if let array = value as? [[String: Any]] {
                        playersArray = array
                        print("✅ Found player array in lineup.\(key) with \(array.count) entries")
                        break
                    }
                }
            }
        }
        
        // 3. Team object
        else if let team = json["team"] as? [String: Any] {
            print("🔍 Found 'team' object, checking nested structure...")
            if let players = team["players"] as? [[String: Any]] {
                playersArray = players
                print("✅ Found 'team.players' array with \(players.count) entries")
            } else {
                print("📋 Team Keys: \(Array(team.keys))")
                // Suche in allen team keys nach Arrays
                for (key, value) in team {
                    if let array = value as? [[String: Any]] {
                        playersArray = array
                        print("✅ Found player array in team.\(key) with \(array.count) entries")
                        break
                    }
                }
            }
        }
        
        // 4. Me object
        else if let me = json["me"] as? [String: Any] {
            print("🔍 Found 'me' object, checking nested structure...")
            if let players = me["players"] as? [[String: Any]] {
                playersArray = players
                print("✅ Found 'me.players' array with \(players.count) entries")
            } else {
                print("📋 Me Keys: \(Array(me.keys))")
                // Suche in allen me keys nach Arrays
                for (key, value) in me {
                    if let array = value as? [[String: Any]] {
                        playersArray = array
                        print("✅ Found player array in me.\(key) with \(array.count) entries")
                        break
                    }
                }
            }
        }
        
        // 5. Fallback: Direkte lineup als Array
        else if let lineup = json["lineup"] as? [[String: Any]] {
            playersArray = lineup
            print("✅ Found 'lineup' as direct array with \(lineup.count) entries")
        }
        
        // 6. Umfassende Suche in ALLEN verschachtelten Objekten
        else {
            print("🔍 Comprehensive search for player arrays in ALL nested structures...")
            for (topKey, topValue) in json {
                print("🔍 Checking top-level key: \(topKey)")
                
                if let nestedDict = topValue as? [String: Any] {
                    print("   → Es ist ein Dictionary mit Keys: \(Array(nestedDict.keys))")
                    for (nestedKey, nestedValue) in nestedDict {
                        if let array = nestedValue as? [[String: Any]], !array.isEmpty {
                            // Prüfe ob das Array Spieler-ähnliche Objekte enthält
                            if let firstItem = array.first {
                                let keys = firstItem.keys
                                let hasPlayerKeys = keys.contains("firstName") || keys.contains("lastName") ||
                                                  keys.contains("name") || keys.contains("position") ||
                                                  keys.contains("fn") || keys.contains("ln") ||
                                                  keys.contains("n") || keys.contains("p")
                                
                                if hasPlayerKeys {
                                    playersArray = array
                                    print("✅ Found player-like array in \(topKey).\(nestedKey) with \(array.count) entries")
                                    print("   → Sample keys: \(Array(keys))")
                                    break
                                } else {
                                    print("   → Array in \(topKey).\(nestedKey) hat keine Player-Keys: \(Array(keys))")
                                }
                            }
                        } else if let array = nestedValue as? [[String: Any]], array.isEmpty {
                            print("   → Leeres Array in \(topKey).\(nestedKey)")
                        }
                    }
                    if !playersArray.isEmpty { break }
                } else if let directArray = topValue as? [[String: Any]], !directArray.isEmpty {
                    // Prüfe ob das direkte Array Spieler-ähnliche Objekte enthält
                    if let firstItem = directArray.first {
                        let keys = firstItem.keys
                        let hasPlayerKeys = keys.contains("firstName") || keys.contains("lastName") ||
                                          keys.contains("name") || keys.contains("position") ||
                                          keys.contains("fn") || keys.contains("ln") ||
                                          keys.contains("n") || keys.contains("p")
                        
                        if hasPlayerKeys {
                            playersArray = directArray
                            print("✅ Found player-like direct array in \(topKey) with \(directArray.count) entries")
                            print("   → Sample keys: \(Array(keys))")
                            break
                        } else {
                            print("   → Direct Array \(topKey) hat keine Player-Keys: \(Array(keys))")
                        }
                    }
                }
            }
        }
        
        if playersArray.isEmpty {
            print("❌ NO PLAYER DATA FOUND IN RESPONSE!")
            print("📋 Complete JSON structure for debugging:")
            if let jsonData = try? JSONSerialization.data(withJSONObject: json, options: .prettyPrinted),
               let jsonString = String(data: jsonData, encoding: .utf8) {
                print(jsonString)
            }
            
            // UI-Update auf Main Thread
            await MainActor.run {
                self.teamPlayers = []
            }
            return
        }
        
        print("🎯 Processing \(playersArray.count) players...")
        var parsedPlayers: [TeamPlayer] = []
        
        for (index, playerData) in playersArray.enumerated() {
            print("🔄 Parsing player \(index + 1): \(Array(playerData.keys))")
            
            let player = parsePlayer(from: playerData)
            parsedPlayers.append(player)
            
            print("✅ Parsed player: \(player.firstName) \(player.lastName) (\(player.teamName))")
        }
        
        // UI-Update auf Main Thread
        await MainActor.run {
            self.teamPlayers = parsedPlayers
            print("✅ UI Updated: Successfully loaded \(parsedPlayers.count) team players from squad API")
            print("👥 Team overview:")
            for player in parsedPlayers {
                print("   - \(player.firstName) \(player.lastName) | \(player.teamName) | €\(player.marketValue/1000)k")
            }
        }
    }
    
    func loadMarketPlayers(for league: League) async {
        print("💰 Loading market players for league: \(league.name)")
        
        guard let token = authToken else {
            print("❌ No auth token available for market players")
            return
        }
        
        // Korrekte Endpunkte basierend auf offizieller API-Dokumentation
        let endpoints = [
            "/v4/leagues/\(league.id)/market",  // Offizieller Endpunkt laut Dokumentation
            "/leagues/\(league.id)/market",     // Fallback ohne v4 Präfix
            "/v4/leagues/\(league.id)/transfers" // Alternative
        ]
        
        for endpoint in endpoints {
            do {
                let url = URL(string: "\(baseURL)\(endpoint)")!
                var request = URLRequest(url: url)
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                request.setValue("application/json", forHTTPHeaderField: "Accept")
                
                print("📤 Trying Market Players URL: \(url)")
                
                let (data, response) = try await URLSession.shared.data(for: request)
                
                if let httpResponse = response as? HTTPURLResponse {
                    print("📊 Market Players Status Code (\(endpoint)): \(httpResponse.statusCode)")
                    
                    if let responseString = String(data: data, encoding: .utf8) {
                        print("📥 Market Players Response: \(responseString.prefix(500))")
                    }
                    
                    if httpResponse.statusCode == 200 {
                        if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                            print("✅ Found working market players endpoint: \(endpoint)")
                            
                            var playersArray: [[String: Any]] = []
                            
                            // Versuche verschiedene mögliche Strukturen basierend auf API-Dokumentation
                            if let players = json["players"] as? [[String: Any]] {
                                playersArray = players
                            } else if let market = json["market"] as? [[String: Any]] {
                                playersArray = market
                            } else if let data = json["data"] as? [[String: Any]] {
                                playersArray = data
                            }
                            
                            if !playersArray.isEmpty {
                                var parsedPlayers: [MarketPlayer] = []
                                
                                for playerData in playersArray {
                                    let seller = MarketSeller(
                                        id: (playerData["seller"] as? [String: Any])?["id"] as? String ?? "",
                                        name: (playerData["seller"] as? [String: Any])?["name"] as? String ?? "Unknown"
                                    )
                                    
                                    let player = MarketPlayer(
                                        id: playerData["id"] as? String ?? "",
                                        firstName: playerData["firstName"] as? String ?? "",
                                        lastName: playerData["lastName"] as? String ?? "",
                                        profileBigUrl: playerData["profileBigUrl"] as? String ?? "",
                                        teamName: playerData["teamName"] as? String ?? "",
                                        teamId: playerData["teamId"] as? String ?? "",
                                        position: playerData["position"] as? Int ?? 0,
                                        number: playerData["number"] as? Int ?? 0,
                                        averagePoints: playerData["averagePoints"] as? Double ?? 0.0,
                                        totalPoints: playerData["totalPoints"] as? Int ?? 0,
                                        marketValue: playerData["marketValue"] as? Int ?? 0,
                                        marketValueTrend: playerData["marketValueTrend"] as? Int ?? 0,
                                        price: playerData["price"] as? Int ?? 0,
                                        expiry: playerData["expiry"] as? String ?? "",
                                        offers: playerData["offers"] as? Int ?? 0,
                                        seller: seller,
                                        stl: playerData["stl"] as? Int ?? 0,
                                        status: playerData["st"] as? Int ?? 0
                                    )
                                    parsedPlayers.append(player)
                                }
                                
                                self.marketPlayers = parsedPlayers
                                print("✅ Loaded \(parsedPlayers.count) market players from API")
                                return
                            }
                        }
                    }
                }
            } catch {
                print("❌ Error with market players endpoint \(endpoint): \(error)")
                continue
            }
        }
        
        print("⚠️ Could not load market players from API - keeping empty array")
        marketPlayers = []
    }
    
    func loadGifts() async {
        print("🎁 Loading gifts...")
        
        guard let token = authToken else {
            print("❌ No auth token available for gifts")
            return
        }
        
        // Korrekte Endpunkte basierend auf offizieller Dokumentation
        let endpoints = [
            "/v4/user/gifts",  // Offizieller Endpunkt laut Dokumentation
            "/user/gifts",     // Fallback ohne v4 Präfix
            "/v4/gifts"        // Alternative
        ]
        
        for endpoint in endpoints {
            do {
                let url = URL(string: "\(baseURL)\(endpoint)")!
                var request = URLRequest(url: url)
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                request.setValue("application/json", forHTTPHeaderField: "Accept")
                
                print("📤 Trying Gifts URL: \(url)")
                
                let (data, response) = try await URLSession.shared.data(for: request)
                
                if let httpResponse = response as? HTTPURLResponse {
                    print("📊 Gifts Status Code (\(endpoint)): \(httpResponse.statusCode)")
                    
                    if let responseString = String(data: data, encoding: .utf8) {
                        print("📥 Gifts Response: \(responseString.prefix(500))")
                    }
                    
                    if httpResponse.statusCode == 200 {
                        if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                            print("✅ Found working gifts endpoint: \(endpoint)")
                            
                            var giftsArray: [[String: Any]] = []
                            
                            // Versuche verschiedene mögliche Strukturen basierend auf API-Dokumentation
                            if let gifts = json["gifts"] as? [[String: Any]] {
                                giftsArray = gifts
                            } else if let data = json["data"] as? [[String: Any]] {
                                giftsArray = data
                            }
                            
                            if !giftsArray.isEmpty {
                                var parsedGifts: [Gift] = []
                                
                                for giftData in giftsArray {
                                    let gift = Gift(
                                        id: giftData["id"] as? String ?? "",
                                        type: giftData["type"] as? String ?? "daily",
                                        amount: giftData["amount"] as? Int ?? 0,
                                        level: giftData["level"] as? Int ?? 0,
                                        collected: giftData["collected"] as? Bool ?? false
                                    )
                                    parsedGifts.append(gift)
                                }
                                
                                self.gifts = parsedGifts
                                print("✅ Loaded \(parsedGifts.count) gifts from API")
                                return
                            }
                        }
                    }
                }
            } catch {
                print("❌ Error with gifts endpoint \(endpoint): \(error)")
                continue
            }
        }
        
        print("⚠️ Could not load gifts from API - keeping empty array")
        gifts = []
    }
    
    func collectGift(id: String) async {
        print("🎁 Collecting gift: \(id)")
        
        guard let token = authToken else {
            print("❌ No auth token available for collecting gift")
            return
        }
        
        // Versuche verschiedene Endpunkte für Gift-Collection
        let endpoints = [
            "/v4/user/gifts/\(id)",
            "/user/gifts/\(id)",
            "/v4/gifts/\(id)/collect"
        ]
        
        for endpoint in endpoints {
            do {
                let url = URL(string: "\(baseURL)\(endpoint)")!
                var request = URLRequest(url: url)
                request.httpMethod = "POST"
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                request.setValue("application/json", forHTTPHeaderField: "Accept")
                
                print("📤 Trying Collect Gift URL: \(url)")
                
                let (data, response) = try await URLSession.shared.data(for: request)
                
                if let httpResponse = response as? HTTPURLResponse {
                    print("📊 Collect Gift Status Code (\(endpoint)): \(httpResponse.statusCode)")
                    
                    if let responseString = String(data: data, encoding: .utf8) {
                        print("📥 Collect Gift Response: \(responseString.prefix(200))")
                    }
                    
                    if httpResponse.statusCode == 200 || httpResponse.statusCode == 201 {
                        print("✅ Gift collected successfully via API: \(endpoint)")
                        
                        // Update gift status locally
                        if let index = gifts.firstIndex(where: { $0.id == id }) {
                            gifts[index] = Gift(
                                id: gifts[index].id,
                                type: gifts[index].type,
                                amount: gifts[index].amount,
                                level: gifts[index].level,
                                collected: true
                            )
                        }
                        return
                    }
                }
            } catch {
                print("❌ Error with collect gift endpoint \(endpoint): \(error)")
                continue
            }
        }
        
        print("⚠️ Could not collect gift via API - all endpoints failed")
    }
    
    func collectAllGifts() async {
        print("🎁 Collecting all available gifts...")
        
        for gift in gifts where !gift.collected {
            await collectGift(id: gift.id)
        }
        
        // Reload gifts to get updated status
        await loadGifts()
    }
    
    // MARK: - Helper Methods
    
    private func parseLeagueUser(from userData: [String: Any]) -> LeagueUser {
        return LeagueUser(
            id: userData["id"] as? String ?? "",
            name: userData["name"] as? String ?? "",
            teamName: userData["teamName"] as? String ?? "",
            budget: userData["budget"] as? Int ?? 0,
            teamValue: userData["teamValue"] as? Int ?? 0,
            points: userData["points"] as? Int ?? 0,
            placement: userData["placement"] as? Int ?? 0,
            won: userData["won"] as? Int ?? 0,
            drawn: userData["drawn"] as? Int ?? 0,
            lost: userData["lost"] as? Int ?? 0,
            se11: userData["se11"] as? Int ?? 0,
            ttm: userData["ttm"] as? Int ?? 0
        )
    }
    
    private func parsePlayer(from playerData: [String: Any]) -> TeamPlayer {
        print("🔍 Parsing individual player data:")
        print("   Available keys: \(Array(playerData.keys))")
        print("   Raw player data: \(playerData)")
        
        // Erweiterte Feldextraktion mit Fallbacks und Debugging
        let apiId = playerData["id"] as? String ?? playerData["i"] as? String ?? ""
        let firstName = playerData["firstName"] as? String ??
                       playerData["fn"] as? String ??
                       playerData["name"] as? String ??
                       playerData["n"] as? String ?? ""
        let lastName = playerData["lastName"] as? String ??
                      playerData["ln"] as? String ??
                      playerData["surname"] as? String ?? ""
        let teamName = playerData["teamName"] as? String ??
                      playerData["tn"] as? String ??
                      playerData["club"] as? String ??
                      playerData["team"] as? String ?? ""
        let teamId = playerData["teamId"] as? String ??
                    playerData["ti"] as? String ??
                    playerData["tid"] as? String ??
                    playerData["clubId"] as? String ?? ""
        let number = playerData["number"] as? Int ??
                    playerData["n"] as? Int ??
                    playerData["jerseyNumber"] as? Int ?? 0
        let position = playerData["position"] as? Int ??
                      playerData["pos"] as? Int ??
                      playerData["p"] as? Int ?? 0
        let marketValue = playerData["marketValue"] as? Int ??
                         playerData["mv"] as? Int ??
                         playerData["value"] as? Int ??
                         playerData["worth"] as? Int ?? 0
        let marketValueTrend = playerData["marketValueTrend"] as? Int ??
                              playerData["mvt"] as? Int ??
                              playerData["trend"] as? Int ??
                              playerData["t"] as? Int ?? 0
        
        // Extrahiere das neue tfhmvt Feld für Marktwertänderung seit letztem Update
        let tfhmvt = playerData["tfhmvt"] as? Int ?? 0
        
        // Extrahiere das st Feld für Status (Verletzung/Krankheit)
        let stStatus = playerData["st"] as? Int ?? 0
        
        let totalPoints = extractTotalPoints(from: playerData)
        let averagePoints = extractAveragePoints(from: playerData)
        
        // Debug: Zeige extrahierte Werte
        print("   🔍 Extracted values:")
        print("     ID: '\(apiId)'")
        print("     First Name: '\(firstName)'")
        print("     Last Name: '\(lastName)'")
        print("     Team Name: '\(teamName)'")
        print("     Number: \(number)")
        print("     Position: \(position)")
        print("     Market Value: \(marketValue)")
        print("     Market Value Trend: \(marketValueTrend)")
        print("     TFHMVT (seit letztem Update): \(tfhmvt)")
        print("     ST Status: \(stStatus)")
        print("     Total Points: \(totalPoints)")
        print("     Average Points: \(averagePoints)")
        
        // Debug: Zeige ALLE verfügbaren Felder in den API-Daten
        print("   📋 ALL API fields available:")
        for (key, value) in playerData.sorted(by: { $0.key < $1.key }) {
            print("     \(key): \(value)")
        }
        
        // Generiere eine eindeutige ID falls die API-ID leer oder doppelt ist
        let uniqueId = apiId.isEmpty ?
            "\(firstName)-\(lastName)-\(teamId)-\(number)-\(UUID().uuidString.prefix(8))" :
            apiId
        
        // Fallback für Namen - verbesserte Logik
        let finalFirstName: String
        let finalLastName: String
        
        if firstName.isEmpty && lastName.isEmpty {
            // Wenn beide Namen fehlen, verwende "Unbekannter Spieler"
            finalFirstName = "Unbekannter"
            finalLastName = "Spieler"
        } else if firstName.isEmpty {
            // Wenn nur der Vorname fehlt, verwende den Nachnamen
            finalFirstName = lastName
            finalLastName = ""
        } else if lastName.isEmpty {
            // Wenn nur der Nachname fehlt, verwende den Vornamen als vollständigen Namen
            finalFirstName = firstName
            finalLastName = ""
        } else {
            // Beide Namen sind vorhanden
            finalFirstName = firstName
            finalLastName = lastName
        }
        
        let finalTeamName = teamName.isEmpty ? "Unknown Team" : teamName
        
        let player = Player(
            id: uniqueId,
            firstName: finalFirstName,
            lastName: finalLastName,
            profileBigUrl: playerData["profileBigUrl"] as? String ??
                          playerData["imageUrl"] as? String ??
                          playerData["photo"] as? String ?? "",
            teamName: finalTeamName,
            teamId: teamId,
            position: position,
            number: number,
            averagePoints: averagePoints,
            totalPoints: totalPoints,
            marketValue: marketValue,
            marketValueTrend: marketValueTrend,
            tfhmvt: tfhmvt,
            stl: playerData["stl"] as? Int ?? 0,
            status: playerData["st"] as? Int ?? 0,
            userOwnsPlayer: playerData["userOwnsPlayer"] as? Bool ??
                           playerData["owned"] as? Bool ??
                           playerData["mine"] as? Bool ?? true
        )
        
        print("   ✅ Created player: \(player.firstName) \(player.lastName) (\(player.teamName)) - €\(player.marketValue/1000)k - TFHMVT: €\(player.tfhmvt/1000)k")
        return player
    }
    
    // MARK: - Debugging Methods
    
    func debugCurrentState() {
        print("🔍 === DEBUGGING CURRENT STATE ===")
        print("🏆 Selected League: \(selectedLeague?.name ?? "None")")
        print("👥 Team Players Count: \(teamPlayers.count)")
        print("📊 UserStats exists: \(userStats != nil)")
        
        // Debug Team Players
        if !teamPlayers.isEmpty {
            print("👥 Team Players Details:")
            for (index, player) in teamPlayers.enumerated() {
                print("   \(index + 1). \(player.firstName) \(player.lastName) (\(player.teamName)) - €\(player.marketValue/1000)k")
            }
        } else {
            print("👥 ❌ No team players loaded")
        }
        
        if let stats = userStats {
            print("   💰 Budget: €\(stats.budget/1000)k")
            print("   📈 Teamwert: €\(stats.teamValue/1000)k")
            print("   🔄 Trend: €\(stats.teamValueTrend/1000)k")
            print("   🏆 Punkte: \(stats.points)")
            print("   📍 Platz: \(stats.placement)")
        } else {
            print("   ❌ No UserStats available")
        }
        
        if let league = selectedLeague {
            print("🔹 League User Fallback:")
            print("   💰 Budget: €\(league.currentUser.budget/1000)k")
            print("   📈 Teamwert: €\(league.currentUser.teamValue/1000)k")
            print("   🏆 Punkte: \(league.currentUser.points)")
        }
        
        print("🔑 Auth token available: \(authToken != nil)")
        print("===============================")
        
        // Debug Team IDs und Namen aus geladenen Spielern
        debugTeamIdsFromPlayers()
    }
    
    func debugTeamIdsFromPlayers() {
        print("🏟️ === TEAM ID DEBUGGING ===")
        
        var uniqueTeams: [String: String] = [:]
        
        // Sammle alle einzigartigen Team-IDs und Namen aus Team-Spielern
        for player in teamPlayers {
            if !player.teamId.isEmpty {
                uniqueTeams[player.teamId] = player.teamName
            }
        }
        
        // Sammle auch aus Markt-Spielern
        for player in marketPlayers {
            if !player.teamId.isEmpty {
                uniqueTeams[player.teamId] = player.teamName
            }
        }
        
        if uniqueTeams.isEmpty {
            print("❌ Keine Team-IDs in geladenen Spielerdaten gefunden")
        } else {
            print("📋 Gefundene Team-IDs in API-Daten:")
            for (id, name) in uniqueTeams.sorted(by: { $0.key < $1.key }) {
                let mappedName = TeamMapping.getTeamName(for: id)
                let status = mappedName != nil ? "✅ GEMAPPT" : "❌ NICHT GEMAPPT"
                print("   ID: \(id) -> API-Name: '\(name)' | Mapping: '\(mappedName ?? "FEHLT")' | Status: \(status)")
            }
            
            // Zeige Mapping-Probleme
            let unmappedIds = uniqueTeams.filter { TeamMapping.getTeamName(for: $0.key) == nil }
            if !unmappedIds.isEmpty {
                print("⚠️ Fehlende Mappings für folgende Team-IDs:")
                for (id, name) in unmappedIds {
                    print("   \"\(id)\": \"\(name)\",")
                }
            }
        }
        
        print("===============================")
    }
    
    // MARK: - Network Testing
    
    func testNetworkConnectivity() async -> Bool {
        print("🌐 Testing network connectivity...")
        
        do {
            let url = URL(string: "https://api.kickbase.com/")!
            var request = URLRequest(url: url)
            request.timeoutInterval = 5.0
            request.httpMethod = "HEAD"  // Minimaler Request
            
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
