import Foundation

@MainActor
class KickbasePlayerService: ObservableObject {
    private let apiClient: KickbaseAPIClient
    private let dataParser: KickbaseDataParser
    
    init(apiClient: KickbaseAPIClient, dataParser: KickbaseDataParser) {
        self.apiClient = apiClient
        self.dataParser = dataParser
    }
    
    // MARK: - Team Players Loading
    
    func loadTeamPlayers(for league: League) async throws -> [TeamPlayer] {
        print("👥 Loading team players (squad) for league: \(league.name)")
        
        let endpoints = [
            "/v4/leagues/\(league.id)/squad",       // Hauptendpoint für Team/Squad
            "/v4/leagues/\(league.id)/lineup",      // Alternative für Aufstellung
            "/v4/leagues/\(league.id)/me/players",  // Meine Spieler
            "/leagues/\(league.id)/squad",          // Fallback ohne v4
            "/leagues/\(league.id)/lineup",         // Fallback Lineup ohne v4
            "/v4/leagues/\(league.id)/lineups"      // Alternative mit Plural
        ]
        
        do {
            let (_, json) = try await apiClient.tryMultipleEndpoints(endpoints: endpoints)
            return await parseTeamPlayersFromResponse(json, league: league)
        } catch APIError.authenticationFailed {
            throw APIError.authenticationFailed
        } catch {
            print("❌ All team players endpoints failed: \(error)")
            throw error
        }
    }
    
    // MARK: - Market Players Loading
    
    func loadMarketPlayers(for league: League) async throws -> [MarketPlayer] {
        print("💰 Loading market players for league: \(league.name)")
        
        let endpoints = [
            "/v4/leagues/\(league.id)/market",  // Offizieller Endpunkt laut Dokumentation
            "/leagues/\(league.id)/market",     // Fallback ohne v4 Präfix
            "/v4/leagues/\(league.id)/transfers" // Alternative
        ]
        
        do {
            let (_, json) = try await apiClient.tryMultipleEndpoints(endpoints: endpoints)
            let marketPlayers = await parseMarketPlayersFromResponse(json, league: league)
            print("✅ Successfully loaded \(marketPlayers.count) market players from API")
            return marketPlayers
        } catch APIError.authenticationFailed {
            throw APIError.authenticationFailed
        } catch {
            print("❌ All market players endpoints failed: \(error)")
            throw error
        }
    }
    
    // MARK: - Player Detail Loading
    
    func loadPlayerDetails(playerId: String, leagueId: String) async -> PlayerDetailResponse? {
        let endpoint = "/v4/leagues/\(leagueId)/players/\(playerId)"
        
        do {
            let (data, httpResponse) = try await apiClient.makeRequest(endpoint: endpoint)
            
            if httpResponse.statusCode == 200 {
                if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                    print("✅ Got player details for ID: \(playerId)")
                    
                    return PlayerDetailResponse(
                        fn: json["fn"] as? String,
                        ln: json["ln"] as? String,
                        tn: json["tn"] as? String,
                        shn: json["shn"] as? Int,
                        id: json["id"] as? String,
                        position: json["position"] as? Int,
                        number: json["number"] as? Int,
                        averagePoints: json["averagePoints"] as? Double,
                        totalPoints: json["totalPoints"] as? Int,
                        marketValue: json["marketValue"] as? Int,
                        marketValueTrend: json["marketValueTrend"] as? Int,
                        profileBigUrl: json["profileBigUrl"] as? String,
                        teamId: json["teamId"] as? String,
                        tfhmvt: json["tfhmvt"] as? Int,
                        prlo: json["prlo"] as? Int,
                        stl: json["stl"] as? Int,
                        status: json["st"] as? Int,
                        userOwnsPlayer: json["userOwnsPlayer"] as? Bool
                    )
                }
            }
        } catch {
            print("❌ Error loading player details for \(playerId): \(error.localizedDescription)")
        }
        
        return nil
    }
    
    // MARK: - Player Performance Loading
    
    func loadPlayerPerformance(playerId: String, leagueId: String) async throws -> PlayerPerformanceResponse? {
        print("📊 Loading player performance for ID: \(playerId)")
        
        let endpoint = "/v4/leagues/\(leagueId)/players/\(playerId)/performance"
        
        do {
            let (data, httpResponse) = try await apiClient.makeRequest(endpoint: endpoint)
            
            if httpResponse.statusCode == 200 {
                // Debug: Log die rohe Response
                if let jsonString = String(data: data, encoding: .utf8) {
                    print("🔍 Raw performance response: \(jsonString.prefix(500))...")
                }
                
                let decoder = JSONDecoder()
                let performanceResponse = try decoder.decode(PlayerPerformanceResponse.self, from: data)
                print("✅ Successfully loaded performance data for player \(playerId)")
                return performanceResponse
            } else {
                print("⚠️ Performance request failed with status: \(httpResponse.statusCode)")
                throw APIError.networkError("HTTP \(httpResponse.statusCode)")
            }
        } catch let DecodingError.keyNotFound(key, context) {
            print("❌ Decoding error - missing key: \(key.stringValue)")
            print("❌ Context: \(context)")
        } catch let DecodingError.typeMismatch(type, context) {
            print("❌ Decoding error - type mismatch for type: \(type)")
            print("❌ Context: \(context)")
        } catch let DecodingError.valueNotFound(type, context) {
            print("❌ Decoding error - value not found for type: \(type)")
            print("❌ Context: \(context)")
        } catch APIError.authenticationFailed {
            throw APIError.authenticationFailed
        } catch {
            print("❌ Error loading player performance for \(playerId): \(error.localizedDescription)")
        }
        
        return nil
    }
    
    // MARK: - Market Value History
    
    func loadPlayerMarketValueHistory(playerId: String, leagueId: String) async -> MarketValueChange? {
        let endpoint = "/v4/leagues/\(leagueId)/players/\(playerId)/marketvalue/365"
        
        do {
            let (data, httpResponse) = try await apiClient.makeRequest(endpoint: endpoint)
            
            if httpResponse.statusCode == 200 {
                if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                    print("✅ Got market value history for player ID: \(playerId)")
                    return dataParser.parseMarketValueHistory(from: json)
                }
            }
        } catch {
            print("❌ Error loading market value history for \(playerId): \(error.localizedDescription)")
        }
        
        return nil
    }
    
    // MARK: - On-Demand Player Market Value Loading
    
    func loadPlayerMarketValueOnDemand(playerId: String, leagueId: String) async -> Int? {
        let endpoint = "/v4/leagues/\(leagueId)/players/\(playerId)/marketvalue/365"
        
        do {
            let (data, httpResponse) = try await apiClient.makeRequest(endpoint: endpoint)
            
            if httpResponse.statusCode == 200 {
                if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                    let prloValue = json["prlo"] as? Int ?? 0
                    print("💰 Found on-demand PRLO value: €\(prloValue/1000)k for player ID: \(playerId)")
                    return prloValue
                }
            }
        } catch {
            print("❌ Error loading on-demand market value for \(playerId): \(error.localizedDescription)")
        }
        
        return nil
    }
    
    // MARK: - Team Profile Loading
    
    func loadTeamProfile(teamId: String, leagueId: String) async -> TeamInfo? {
        print("🏆 Loading team profile for team \(teamId) in league \(leagueId)")
        
        let endpoint = "/v4/leagues/\(leagueId)/teams/\(teamId)/teamprofile"
        
        do {
            let (data, httpResponse) = try await apiClient.makeRequest(endpoint: endpoint)
            
            if httpResponse.statusCode == 200 {
                let decoder = JSONDecoder()
                let teamProfileResponse = try decoder.decode(TeamProfileResponse.self, from: data)
                let teamInfo = TeamInfo(from: teamProfileResponse)
                print("✅ Successfully loaded team profile: \(teamInfo.name) (Platz \(teamInfo.placement))")
                return teamInfo
            } else {
                print("⚠️ Team profile request failed with status: \(httpResponse.statusCode)")
            }
        } catch {
            print("❌ Error loading team profile for \(teamId): \(error.localizedDescription)")
        }
        
        return nil
    }
    
    // MARK: - Enhanced Performance Loading with Team Info (Optimized)
    
    func loadPlayerPerformanceWithTeamInfo(playerId: String, leagueId: String) async throws -> [EnhancedMatchPerformance]? {
        print("📊 Loading optimized player performance with team info for player \(playerId)")
        
        // Lade zunächst die normale Performance
        guard let performance = try await loadPlayerPerformance(playerId: playerId, leagueId: leagueId),
              let currentSeason = performance.it.last else {
            print("⚠️ No performance data available")
            return nil
        }
        
        // Finde den aktuellen Spieltag
        let currentMatchDay = getCurrentMatchDayFromPerformance(currentSeason.ph)
        print("🎯 Current match day identified as: \(currentMatchDay)")
        
        // Filtere nur die relevanten Spiele (letzte 5 + aktuelle + nächste 3)
        let relevantMatches = currentSeason.ph.filter { match in
            let matchDay = match.day
            return matchDay >= (currentMatchDay - 4) && matchDay <= (currentMatchDay + 3)
        }
        
        print("🎯 Filtered to \(relevantMatches.count) relevant matches (days \(currentMatchDay - 4) to \(currentMatchDay + 3))")
        
        // Sammle nur einzigartige Team-IDs aus den relevanten Matches
        var uniqueTeamIds = Set<String>()
        for match in relevantMatches {
            uniqueTeamIds.insert(match.t1)
            uniqueTeamIds.insert(match.t2)
        }
        
        print("🎯 Found \(uniqueTeamIds.count) unique teams in relevant matches: \(Array(uniqueTeamIds))")
        
        // Lade Team-Informationen nur für diese Teams
        var teamInfoCache: [String: TeamInfo] = [:]
        
        for teamId in uniqueTeamIds {
            if let teamInfo = await loadTeamProfile(teamId: teamId, leagueId: leagueId) {
                teamInfoCache[teamId] = teamInfo
                print("✅ Cached team info for \(teamId): \(teamInfo.name)")
            } else {
                print("⚠️ Could not load team info for \(teamId)")
            }
        }
        
        // Erstelle erweiterte Match-Performance Objekte nur für relevante Matches
        var enhancedMatches: [EnhancedMatchPerformance] = []
        
        for match in relevantMatches {
            let team1Info = teamInfoCache[match.t1]
            let team2Info = teamInfoCache[match.t2]
            let playerTeamInfo = teamInfoCache[match.pt ?? ""]
            let opponentTeamInfo = teamInfoCache[match.opponentTeamId]
            
            let enhancedMatch = EnhancedMatchPerformance(
                basePerformance: match,
                team1Info: team1Info,
                team2Info: team2Info,
                playerTeamInfo: playerTeamInfo,
                opponentTeamInfo: opponentTeamInfo
            )
            
            enhancedMatches.append(enhancedMatch)
        }
        
        print("✅ Created \(enhancedMatches.count) enhanced matches with team info (optimized)")
        return enhancedMatches
    }
    
    // MARK: - Helper function to determine current match day
    
    private func getCurrentMatchDayFromPerformance(_ matches: [MatchPerformance], fallbackMatchDay: Int? = nil) -> Int {
        // Strategie 1: Finde den aktuellen Spieltag über "cur" Flag
        if let currentMatch = matches.first(where: { $0.cur == true }) {
            print("🎯 Found current match via 'cur' flag: day \(currentMatch.day)")
            return currentMatch.day
        }
        
        // Strategie 2: Finde den letzten gespielten Spieltag
        let playedMatches = matches.filter { $0.hasPlayed }
        if let lastPlayedMatch = playedMatches.max(by: { $0.day < $1.day }) {
            let currentDay = lastPlayedMatch.day + 1 // Nächster Spieltag nach dem letzten gespielten
            print("🎯 Determined current match day from last played: \(currentDay)")
            return currentDay
        }
        
        // Strategie 3: Fallback - verwende übergebenen matchDay
        if let fallback = fallbackMatchDay {
            print("🎯 Using provided fallback match day: \(fallback)")
            return fallback
        }
        
        // Fallback: Verwende Spieltag 10 als Standardwert
        print("⚠️ Using fallback current match day: 10")
        return 10
    }
    
    // MARK: - Parsing Methods
    
    private func parseTeamPlayersFromResponse(_ json: [String: Any], league: League) async -> [TeamPlayer] {
        print("🔍 Parsing team players from response...")
        print("📋 Team JSON keys: \(Array(json.keys))")
        
        var playersArray: [[String: Any]] = []
        
        // Erweiterte Suche nach Spieler-Daten
        if let players = json["players"] as? [[String: Any]] {
            playersArray = players
            print("✅ Found 'players' array with \(players.count) entries")
        } else if let squad = json["squad"] as? [[String: Any]] {
            playersArray = squad
            print("✅ Found 'squad' array with \(squad.count) entries")
        } else if let data = json["data"] as? [[String: Any]] {
            playersArray = data
            print("✅ Found 'data' array with \(data.count) entries")
        } else {
            // Umfassende Suche in verschachtelten Strukturen
            playersArray = findPlayersInNestedStructure(json)
        }
        
        if playersArray.isEmpty {
            print("❌ NO PLAYER DATA FOUND IN RESPONSE!")
            return []
        }
        
        print("🎯 Processing \(playersArray.count) players...")
        var parsedPlayers: [TeamPlayer] = []
        
        for (index, playerData) in playersArray.enumerated() {
            print("🔄 Parsing player \(index + 1): \(Array(playerData.keys))")
            
            let player = await parsePlayerWithDetails(from: playerData, league: league)
            parsedPlayers.append(player)
            
            print("✅ Parsed player: \(player.firstName) \(player.lastName) (\(player.teamName))")
        }
        
        print("✅ Successfully parsed \(parsedPlayers.count) team players")
        return parsedPlayers
    }
    
    private func findPlayersInNestedStructure(_ json: [String: Any]) -> [[String: Any]] {
        print("🔍 Comprehensive search for player arrays in nested structures...")
        
        for (topKey, topValue) in json {
            if let nestedDict = topValue as? [String: Any] {
                for (nestedKey, nestedValue) in nestedDict {
                    if let array = nestedValue as? [[String: Any]], !array.isEmpty {
                        if let firstItem = array.first {
                            let keys = firstItem.keys
                            let hasPlayerKeys = keys.contains("firstName") || keys.contains("lastName") ||
                                              keys.contains("name") || keys.contains("position") ||
                                              keys.contains("fn") || keys.contains("ln") ||
                                              keys.contains("n") || keys.contains("p")
                            
                            if hasPlayerKeys {
                                print("✅ Found player-like array in \(topKey).\(nestedKey) with \(array.count) entries")
                                return array
                            }
                        }
                    }
                }
            } else if let directArray = topValue as? [[String: Any]], !directArray.isEmpty {
                if let firstItem = directArray.first {
                    let keys = firstItem.keys
                    let hasPlayerKeys = keys.contains("firstName") || keys.contains("lastName") ||
                                      keys.contains("name") || keys.contains("position") ||
                                      keys.contains("fn") || keys.contains("ln") ||
                                      keys.contains("n") || keys.contains("p")
                    
                    if hasPlayerKeys {
                        print("✅ Found player-like direct array in \(topKey) with \(directArray.count) entries")
                        return directArray
                    }
                }
            }
        }
        
        return []
    }
    
    private func parsePlayerWithDetails(from playerData: [String: Any], league: League) async -> TeamPlayer {
        let apiId = playerData["id"] as? String ?? playerData["i"] as? String ?? ""
        
        // Lade Player-Details vom Detail-Endpoint falls ID verfügbar
        var playerDetails: PlayerDetailResponse?
        if !apiId.isEmpty {
            playerDetails = await loadPlayerDetails(playerId: apiId, leagueId: league.id)
        }
        
        // Namen-Extraktion mit Detail-Endpoint Fallback
        let squadName = playerData["name"] as? String ?? playerData["n"] as? String ?? ""
        let firstName: String
        let lastName: String
        
        if let details = playerDetails {
            firstName = details.fn ?? ""
            lastName = details.ln ?? squadName
            print("   ✅ Using names from detail endpoint - fn: '\(firstName)', ln: '\(lastName)'")
        } else {
            firstName = ""
            lastName = squadName
            print("   ⚠️ Using squad data only - treating 'name' as lastName: '\(lastName)'")
        }
        
        // Team-Name aus Detail-Endpoint oder Fallback
        let teamName: String
        if let details = playerDetails, let detailTeamName = details.tn, !detailTeamName.isEmpty {
            teamName = detailTeamName
        } else {
            teamName = playerData["tn"] as? String ?? "Unknown Team"
        }
        
        // Trikotnummer mit Priorität auf Detail-Endpoint
        let shirtNumber: Int
        if let details = playerDetails, let detailShirtNumber = details.shn {
            shirtNumber = detailShirtNumber
        } else {
            shirtNumber = playerData["number"] as? Int ??
                         playerData["n"] as? Int ??
                         playerData["jerseyNumber"] as? Int ?? 0
        }
        
        // Andere Felder extrahieren
        let teamId = playerData["teamId"] as? String ??
                    playerData["ti"] as? String ??
                    playerData["tid"] as? String ??
                    playerData["clubId"] as? String ?? ""
        let position = playerData["position"] as? Int ??
                      playerData["pos"] as? Int ??
                      playerData["p"] as? Int ?? 0
        let marketValue = playerData["marketValue"] as? Int ??
                         playerData["mv"] as? Int ??
                         playerData["value"] as? Int ?? 0
        let marketValueTrend = playerData["marketValueTrend"] as? Int ??
                              playerData["mvt"] as? Int ??
                              playerData["trend"] as? Int ?? 0
        let tfhmvt = playerData["tfhmvt"] as? Int ?? 0
        
        // Namen-Fallback
        let finalFirstName = firstName.isEmpty && lastName.isEmpty ? "Unbekannter" :
                            firstName.isEmpty ? lastName : firstName
        let finalLastName = firstName.isEmpty && lastName.isEmpty ? "Spieler" :
                           firstName.isEmpty ? "" : lastName
        
        let uniqueId = apiId.isEmpty ?
            "\(finalFirstName)-\(finalLastName)-\(teamId)-\(shirtNumber)-\(UUID().uuidString.prefix(8))" :
            apiId
        
        return Player(
            id: uniqueId,
            firstName: finalFirstName,
            lastName: finalLastName,
            profileBigUrl: playerData["profileBigUrl"] as? String ??
                          playerData["imageUrl"] as? String ??
                          playerData["photo"] as? String ?? "",
            teamName: teamName,
            teamId: teamId,
            position: position,
            number: shirtNumber,
            averagePoints: dataParser.extractAveragePoints(from: playerData),
            totalPoints: dataParser.extractTotalPoints(from: playerData),
            marketValue: marketValue,
            marketValueTrend: marketValueTrend,
            tfhmvt: tfhmvt,
            prlo: 0,  // Wird on-demand geladen
            stl: playerData["stl"] as? Int ?? 0,
            status: playerData["st"] as? Int ?? 0,
            userOwnsPlayer: playerData["userOwnsPlayer"] as? Bool ??
                           playerData["owned"] as? Bool ??
                           playerData["mine"] as? Bool ?? true
        )
    }
    
    private func parseMarketPlayersFromResponse(_ json: [String: Any], league: League) async -> [MarketPlayer] {
        print("🔍 === MARKET PLAYER PARSING DEBUG ===")
        print("📋 Market JSON keys: \(Array(json.keys))")
        
        // Detaillierte JSON-Struktur-Analyse
        for (key, value) in json {
            if let array = value as? [[String: Any]] {
                print("📊 Key '\(key)' contains array with \(array.count) items")
                if let firstItem = array.first {
                    print("   First item keys: \(Array(firstItem.keys))")
                }
            } else if let dict = value as? [String: Any] {
                print("📊 Key '\(key)' contains dictionary with keys: \(Array(dict.keys))")
            } else {
                print("📊 Key '\(key)' contains: \(type(of: value))")
            }
        }
        
        var playersArray: [[String: Any]] = []
        
        // Erweiterte Suche nach Marktspielerdaten - jetzt inklusive "it" Array
        if let it = json["it"] as? [[String: Any]] {
            playersArray = it
            print("✅ Found 'it' array with \(it.count) entries")
        } else if let players = json["players"] as? [[String: Any]] {
            playersArray = players
            print("✅ Found 'players' array with \(players.count) entries")
        } else if let market = json["market"] as? [[String: Any]] {
            playersArray = market
            print("✅ Found 'market' array with \(market.count) entries")
        } else if let data = json["data"] as? [[String: Any]] {
            playersArray = data
            print("✅ Found 'data' array with \(data.count) entries")
        } else if let transfers = json["transfers"] as? [[String: Any]] {
            playersArray = transfers
            print("✅ Found 'transfers' array with \(transfers.count) entries")
        } else if let items = json["items"] as? [[String: Any]] {
            playersArray = items
            print("✅ Found 'items' array with \(items.count) entries")
        } else if let list = json["list"] as? [[String: Any]] {
            playersArray = list
            print("✅ Found 'list' array with \(list.count) entries")
        } else {
            // Umfassende Suche in verschachtelten Strukturen
            playersArray = findMarketPlayersInNestedStructure(json)
        }
        
        if playersArray.isEmpty {
            print("❌ NO MARKET PLAYER DATA FOUND IN RESPONSE!")
            print("📋 Available top-level keys: \(Array(json.keys))")
            
            // Zusätzliche Fallback-Suche nach alternativen Feldnamen
            for (key, value) in json {
                if let stringValue = value as? String {
                    print("   \(key): \(stringValue)")
                } else if let numberValue = value as? NSNumber {
                    print("   \(key): \(numberValue)")
                } else if let arrayValue = value as? [Any] {
                    print("   \(key): Array with \(arrayValue.count) elements")
                    if let firstElement = arrayValue.first {
                        print("     First element type: \(type(of: firstElement))")
                        if let dictElement = firstElement as? [String: Any] {
                            print("     First element keys: \(Array(dictElement.keys))")
                        }
                    }
                }
            }
            
            return []
        }
        
        print("🎯 Processing \(playersArray.count) market players...")
        var parsedPlayers: [MarketPlayer] = []
        
        for (index, playerData) in playersArray.enumerated() {
            print("🔄 Parsing market player \(index + 1): \(Array(playerData.keys))")
            
            let player = await parseMarketPlayerWithDetails(from: playerData, league: league)
            parsedPlayers.append(player)
            
            print("✅ Parsed market player: \(player.firstName) \(player.lastName) (€\(player.price/1000)k from \(player.seller.name))")
        }
        
        print("✅ Successfully parsed \(parsedPlayers.count) market players")
        print("🔍 === END MARKET PLAYER PARSING DEBUG ===")
        return parsedPlayers
    }
    
    private func findMarketPlayersInNestedStructure(_ json: [String: Any]) -> [[String: Any]] {
        print("🔍 Comprehensive search for market player arrays in nested structures...")
        
        for (topKey, topValue) in json {
            print("🔎 Checking top-level key: \(topKey)")
            
            if let nestedDict = topValue as? [String: Any] {
                for (nestedKey, nestedValue) in nestedDict {
                    if let array = nestedValue as? [[String: Any]], !array.isEmpty {
                        if let firstItem = array.first {
                            let keys = firstItem.keys
                            let hasMarketKeys = keys.contains("price") || keys.contains("seller") ||
                                              keys.contains("expiry") || keys.contains("offers") ||
                                              keys.contains("firstName") || keys.contains("lastName") ||
                                              keys.contains("prc") || keys.contains("u") ||
                                              keys.contains("n") || keys.contains("fn") || keys.contains("ln")
                            
                            if hasMarketKeys {
                                print("✅ Found market players array at: \(topKey).\(nestedKey) with \(array.count) items")
                                return array
                            }
                        }
                    }
                }
            } else if let array = topValue as? [[String: Any]], !array.isEmpty {
                if let firstItem = array.first {
                    let keys = firstItem.keys
                    let hasMarketKeys = keys.contains("price") || keys.contains("seller") ||
                                      keys.contains("expiry") || keys.contains("offers") ||
                                      keys.contains("firstName") || keys.contains("lastName") ||
                                      keys.contains("prc") || keys.contains("u") ||
                                      keys.contains("n") || keys.contains("fn") || keys.contains("ln")
                    
                    if hasMarketKeys {
                        print("✅ Found market players array at top level: \(topKey) with \(array.count) items")
                        return array
                    }
                }
            }
        }
        
        print("❌ No market player arrays found in nested structure")
        return []
    }
    
    private func parseMarketPlayerWithDetails(from playerData: [String: Any], league: League) async -> MarketPlayer {
        let apiId = playerData["id"] as? String ?? playerData["i"] as? String ?? ""
        
        // Lade Player-Details vom Detail-Endpoint falls ID verfügbar
        var playerDetails: PlayerDetailResponse?
        if !apiId.isEmpty {
            playerDetails = await loadPlayerDetails(playerId: apiId, leagueId: league.id)
        }
        
        // Namen-Extraktion mit Detail-Endpoint Fallback
        let marketName = playerData["n"] as? String ?? playerData["name"] as? String ?? ""
        let firstName: String
        let lastName: String
        
        if let details = playerDetails {
            firstName = details.fn ?? ""
            lastName = details.ln ?? marketName
            print("   ✅ Using names from detail endpoint - fn: '\(firstName)', ln: '\(lastName)'")
        } else {
            // Fallback: Versuche firstName/lastName aus Marktdaten zu extrahieren
            firstName = playerData["firstName"] as? String ?? playerData["fn"] as? String ?? ""
            lastName = playerData["lastName"] as? String ?? playerData["ln"] as? String ?? marketName
            print("   ⚠️ Using market data only - firstName: '\(firstName)', lastName: '\(lastName)'")
        }
        
        // Team-Name aus Detail-Endpoint oder Fallback
        let teamName: String
        if let details = playerDetails, let detailTeamName = details.tn, !detailTeamName.isEmpty {
            teamName = detailTeamName
        } else {
            teamName = playerData["teamName"] as? String ??
                      playerData["tn"] as? String ?? "Unknown Team"
        }
        
        // Seller-Informationen extrahieren
        let seller = MarketSeller(
            id: (playerData["seller"] as? [String: Any])?["id"] as? String ??
                (playerData["u"] as? [String: Any])?["i"] as? String ?? "",
            name: (playerData["seller"] as? [String: Any])?["name"] as? String ??
                  (playerData["u"] as? [String: Any])?["n"] as? String ?? "Unknown"
        )
        
        // Owner-Informationen (falls vorhanden)
        let owner = extractOwnerFromPlayerData(playerData)
        
        // Andere Felder mit Priorität auf Detail-Endpoint
        let position = playerDetails?.position ??
                      playerData["position"] as? Int ??
                      playerData["pos"] as? Int ??
                      playerData["p"] as? Int ?? 0
        
        let number = playerDetails?.number ??
                    playerData["number"] as? Int ??
                    playerData["jerseyNumber"] as? Int ?? 0
        
        let marketValue = playerDetails?.marketValue ??
                         playerData["marketValue"] as? Int ??
                         playerData["mv"] as? Int ?? 0
        
        let marketValueTrend = playerDetails?.marketValueTrend ??
                              playerData["marketValueTrend"] as? Int ??
                              playerData["mvt"] as? Int ?? 0
        
        let averagePoints = playerDetails?.averagePoints ??
                           playerData["averagePoints"] as? Double ??
                           playerData["ap"] as? Double ?? 0.0
        
        let totalPoints = playerDetails?.totalPoints ??
                         playerData["totalPoints"] as? Int ??
                         playerData["p"] as? Int ?? 0
        
        // Namen-Fallback falls beide leer sind
        let finalFirstName = firstName.isEmpty && lastName.isEmpty ? "Unbekannter" :
                            firstName.isEmpty ? lastName : firstName
        let finalLastName = firstName.isEmpty && lastName.isEmpty ? "Spieler" :
                           firstName.isEmpty ? "" : lastName
        
        let uniqueId = apiId.isEmpty ?
            "\(finalFirstName)-\(finalLastName)-\(seller.id)-\(UUID().uuidString.prefix(8))" :
            apiId
        
        return MarketPlayer(
            id: uniqueId,
            firstName: finalFirstName,
            lastName: finalLastName,
            profileBigUrl: playerDetails?.profileBigUrl ??
                          playerData["profileBigUrl"] as? String ??
                          playerData["pim"] as? String ?? "",
            teamName: teamName,
            teamId: playerDetails?.teamId ??
                   playerData["teamId"] as? String ??
                   playerData["tid"] as? String ?? "",
            position: position,
            number: number,
            averagePoints: averagePoints,
            totalPoints: totalPoints,
            marketValue: marketValue,
            marketValueTrend: marketValueTrend,
            price: playerData["price"] as? Int ??
                  playerData["prc"] as? Int ?? 0,
            expiry: playerData["expiry"] as? String ??
                   playerData["dt"] as? String ?? "",
            offers: playerData["offers"] as? Int ??
                   playerData["ofc"] as? Int ?? 0,
            seller: seller,
            stl: playerData["stl"] as? Int ??
                playerData["st"] as? Int ?? 0,
            status: playerDetails?.status ??
                   playerData["status"] as? Int ??
                   playerData["st"] as? Int ?? 0,
            prlo: playerDetails?.prlo ??
                 playerData["prlo"] as? Int,
            owner: owner,
            exs: playerData["exs"] as? Int ?? 0
        )
    }
    
    // MARK: - Helper Functions
    
    private func extractOwnerFromPlayerData(_ playerData: [String: Any]) -> PlayerOwner? {
        guard let ownerData = playerData["u"] as? [String: Any] else {
            return nil
        }
        
        guard let id = ownerData["i"] as? String,
              let name = ownerData["n"] as? String else {
            print("   ⚠️ Owner data missing required fields (i or n)")
            return nil
        }
        
        let owner = PlayerOwner(
            i: id,
            n: name,
            uim: ownerData["uim"] as? String,
            isvf: ownerData["isvf"] as? Bool,
            st: ownerData["st"] as? Int
        )
        
        print("   ✅ Found owner: \(owner.name) (ID: \(owner.id))")
        return owner
    }

    // MARK: - Mock Data
    
    private func createMockTeamPlayers() -> [TeamPlayer] {
        return [
            TeamPlayer(
                id: "mock-player-1",
                firstName: "Max",
                lastName: "Mustermann",
                profileBigUrl: "",
                teamName: "FC Demo",
                teamId: "40",
                position: 4,
                number: 9,
                averagePoints: 5.0,
                totalPoints: 45,
                marketValue: 15000000,
                marketValueTrend: 500000,
                tfhmvt: 250000,
                prlo: 3000000,
                stl: 0,
                status: 0,
                userOwnsPlayer: true
            ),
            TeamPlayer(
                id: "mock-player-2",
                firstName: "Hans",
                lastName: "Beispiel",
                profileBigUrl: "",
                teamName: "Demo United",
                teamId: "1",
                position: 3,
                number: 10,
                averagePoints: 4.2,
                totalPoints: 38,
                marketValue: 12000000,
                marketValueTrend: -200000,
                tfhmvt: -150000,
                prlo: -1500000,
                stl: 1,
                status: 0,
                userOwnsPlayer: true
            )
        ]
    }
    
    private func createMockMarketPlayers() -> [MarketPlayer] {
        return [
            MarketPlayer(
                id: "mock-market-player-1",
                firstName: "John",
                lastName: "Doe",
                profileBigUrl: "",
                teamName: "FC Mock",
                teamId: "50",
                position: 2,
                number: 7,
                averagePoints: 6.0,
                totalPoints: 60,
                marketValue: 20000000,
                marketValueTrend: 1000000,
                price: 18000000,
                expiry: "2023-12-31T23:59:59Z",
                offers: 5,
                seller: MarketSeller(id: "mock-seller-1", name: "Mock Seller"),
                stl: 0,
                status: 1,
                prlo: 3500000,
                owner: PlayerOwner(
                    i: "mock-owner-1",
                    n: "Mock Owner",
                    uim: nil,
                    isvf: true,
                    st: 0
                ),
                exs: 1700000000  // Mock timestamp für Ablaufdatum
            ),
            MarketPlayer(
                id: "mock-market-player-2",
                firstName: "Jane",
                lastName: "Smith",
                profileBigUrl: "",
                teamName: "Mock City",
                teamId: "2",
                position: 1,
                number: 11,
                averagePoints: 7.5,
                totalPoints: 75,
                marketValue: 25000000,
                marketValueTrend: -500000,
                price: 22000000,
                expiry: "2024-01-15T23:59:59Z",
                offers: 3,
                seller: MarketSeller(id: "mock-seller-2", name: "Another Seller"),
                stl: 1,
                status: 1,
                prlo: 4000000,
                owner: nil,
                exs: 1700100000  // Mock timestamp für Ablaufdatum
            )
        ]
    }
}
