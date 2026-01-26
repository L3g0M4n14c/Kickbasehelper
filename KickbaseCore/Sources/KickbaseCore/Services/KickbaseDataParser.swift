import Combine
import SwiftUI
import Foundation

@MainActor
public class KickbaseDataParser: ObservableObject {

    // MARK: - Int/String Extraction Helpers

    public func extractInt(from data: [String: Any], keys: [String]) -> Int? {
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

    public func extractString(from data: [String: Any], keys: [String]) -> String? {
        for key in keys {
            if let value = data[key] as? String {
                return value
            }
        }
        return nil
    }

    public func extractDouble(from data: [String: Any], keys: [String]) -> Double? {
        for key in keys {
            if let value = data[key] as? Double {
                return value
            } else if let value = data[key] as? Int {
                return Double(value)
            } else if let value = data[key] as? String, let doubleValue = Double(value) {
                return doubleValue
            }
        }
        return nil
    }

    // MARK: - Punktzahl-Extraktions-Helper-Funktionen

    public func extractTotalPoints(from playerData: [String: Any]) -> Int {
        let possibleKeys = [
            "p",  // Hauptfeld für Gesamtpunkte laut User
            "totalPoints", "tp", "points", "pts", "totalPts",
            "gesamtpunkte", "total", "score", "seasonPoints", "sp",
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
        return 0  // Fallback wenn keine Punktzahl gefunden wird
    }

    public func extractAveragePoints(from playerData: [String: Any]) -> Double {
        let possibleKeys = [
            "averagePoints", "ap", "avgPoints", "durchschnitt",
            "avg", "averageScore", "avgp", "avp",
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
        return 0.0  // Fallback wenn keine Durchschnittspunktzahl gefunden wird
    }

    // MARK: - League Parsing

    public func parseLeaguesFromResponse(_ json: [String: Any]) -> [League] {
        print("🔍 Parsing leagues response...")
        print("📋 Raw JSON keys: \(Array(json.keys))")

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
            leaguesArray = it
            print("✅ Found it array with \(it.count) entries")
        } else if let anol = json["anol"] as? [[String: Any]] {
            leaguesArray = anol
            print("✅ Found anol array with \(anol.count) entries")
        } else if json.keys.contains("id") {
            // Single league response
            leaguesArray = [json]
            print("✅ Found single league response")
        } else {
            // Erweiterte Behandlung für "it" und "anol" Keys
            leaguesArray = findLeaguesInComplexStructure(json)
        }

        var parsedLeagues: [League] = []

        for (index, leagueData) in leaguesArray.enumerated() {
            print("🔄 Parsing league \(index + 1): \(Array(leagueData.keys))")

            let currentUser = parseLeagueUser(from: leagueData)

            let league = League(
                id: leagueData["id"] as? String ?? leagueData["i"] as? String ?? UUID().uuidString,
                name: leagueData["name"] as? String ?? leagueData["n"] as? String
                    ?? "Liga \(index + 1)",
                creatorName: leagueData["creatorName"] as? String ?? leagueData["cn"] as? String
                    ?? "",
                adminName: leagueData["adminName"] as? String ?? leagueData["an"] as? String ?? "",
                created: leagueData["created"] as? String ?? leagueData["c"] as? String ?? "",
                season: leagueData["season"] as? String ?? leagueData["s"] as? String ?? "2024/25",
                matchDay: leagueData["matchDay"] as? Int ?? leagueData["md"] as? Int ?? 1,
                currentUser: currentUser
            )

            parsedLeagues.append(league)
            print("✅ Parsed league: \(league.name)")
        }

        print("🏆 Successfully parsed \(parsedLeagues.count) leagues")
        return parsedLeagues
    }

    private func findLeaguesInComplexStructure(_ json: [String: Any]) -> [[String: Any]] {
        print("🔍 Checking alternative formats for it/anol keys...")

        // Prüfe "it" key
        if let it = json["it"] {
            print("🔍 Found 'it' key with type: \(type(of: it))")

            if let itDict = it as? [String: Any] {
                print("✅ 'it' is a dictionary with keys: \(Array(itDict.keys))")
                for (key, value) in itDict {
                    if let array = value as? [[String: Any]] {
                        print("✅ Found leagues in it[\(key)] with \(array.count) entries")
                        return array
                    }
                }
                return [itDict]
            } else if let itArray = it as? [[String: Any]] {
                print("✅ Found 'it' as direct array with \(itArray.count) entries")
                return itArray
            }
        }

        // Prüfe "anol" key
        if let anol = json["anol"] {
            print("🔍 Found 'anol' key with type: \(type(of: anol))")

            if let anolDict = anol as? [String: Any] {
                print("✅ 'anol' is a dictionary with keys: \(Array(anolDict.keys))")
                for (key, value) in anolDict {
                    if let array = value as? [[String: Any]] {
                        print("✅ Found leagues in anol[\(key)] with \(array.count) entries")
                        return array
                    }
                }
                return [anolDict]
            } else if let anolArray = anol as? [[String: Any]] {
                print("✅ Found 'anol' as direct array with \(anolArray.count) entries")
                return anolArray
            }
        }

        // Suche alle Keys nach Arrays ab
        print("🔍 Searching all keys for array data...")
        for (key, value) in json {
            if let array = value as? [[String: Any]] {
                print("✅ Found leagues in [\(key)] with \(array.count) entries")
                return array
            } else if let dict = value as? [String: Any], !dict.isEmpty {
                if key != "it" && key != "anol" {
                    return [dict]
                }
            }
        }

        // Falls Liga-ähnliche Daten direkt im JSON
        if json.keys.contains("id") || json.keys.contains("name") || json.keys.contains("i")
            || json.keys.contains("n")
        {
            print("✅ Using entire response as single league")
            return [json]
        }

        print("❌ Unknown response format. Keys: \(Array(json.keys))")
        return []
    }

    public func parseLeagueUser(from leagueData: [String: Any]) -> LeagueUser {
        var currentUser = LeagueUser(
            id: "unknown",
            name: "Unknown",
            teamName: "Unknown Team",
            budget: 5_000_000,
            teamValue: 50_000_000,
            points: 0,
            placement: 1,
            won: 0,
            drawn: 0,
            lost: 0,
            se11: 0,
            ttm: 0,
            mpst: 3
        )

        if let userData = leagueData["currentUser"] as? [String: Any] ?? leagueData["cu"]
            as? [String: Any] ?? leagueData["user"] as? [String: Any] ?? leagueData["it"]
            as? [String: Any] ?? leagueData["anol"] as? [String: Any]
        {

            print("👤 Available user keys: \(userData.keys.sorted())")

            // Prüfe verschiedene mögliche Feldnamen für teamName
            let possibleTeamNames = [
                userData["teamName"] as? String,
                userData["tn"] as? String,
                userData["team_name"] as? String,
                userData["tname"] as? String,
                userData["club"] as? String,
                userData["clubName"] as? String,
                userData["teamname"] as? String,
            ].compactMap { $0 }

            let teamName = possibleTeamNames.first ?? "Team"
            print("🏆 Found team name: '\(teamName)' from keys: \(possibleTeamNames)")

            currentUser = LeagueUser(
                id: userData["id"] as? String ?? userData["i"] as? String ?? "unknown",
                name: userData["name"] as? String ?? userData["n"] as? String ?? "User",
                teamName: teamName,
                budget: userData["budget"] as? Int ?? userData["b"] as? Int ?? 5_000_000,
                teamValue: userData["teamValue"] as? Int ?? userData["tv"] as? Int ?? 50_000_000,
                points: userData["points"] as? Int ?? userData["p"] as? Int ?? 0,
                placement: userData["placement"] as? Int ?? userData["pl"] as? Int ?? 1,
                won: userData["won"] as? Int ?? userData["w"] as? Int ?? 0,
                drawn: userData["drawn"] as? Int ?? userData["d"] as? Int ?? 0,
                lost: userData["lost"] as? Int ?? userData["l"] as? Int ?? 0,
                se11: userData["se11"] as? Int ?? userData["s"] as? Int ?? 0,
                ttm: userData["ttm"] as? Int ?? userData["t"] as? Int ?? 0,
                mpst: userData["mpst"] as? Int ?? userData["maxPlayersPerTeam"] as? Int ?? 3
            )
            print("✅ Parsed user: \(currentUser.name) - \(currentUser.teamName)")
        } else {
            print("❌ No user data found in league data")
        }

        return currentUser
    }

    // MARK: - League Ranking Parsing
    
    public func parseLeagueRanking(from json: [String: Any]) -> [LeagueUser] {
        print("🏆 Parsing league ranking...")
        
        // The ranking uses "us" array according to API documentation
        guard let usersArray = json["us"] as? [[String: Any]] else {
            print("⚠️ No users array found in ranking response")
            print("📋 Available keys: \(json.keys.sorted())")
            return []
        }
        
        let users = usersArray.compactMap { userData -> LeagueUser? in
            print("👤 User data keys: \(userData.keys.sorted())")
            
            // Parse each user in the ranking using actual API field names
            let id = extractString(from: userData, keys: ["i", "id"]) ?? "unknown"
            let name = extractString(from: userData, keys: ["n", "name"]) ?? "User"
            // Note: ranking API doesn't include teamName, so we'll use an empty default
            let teamName = extractString(from: userData, keys: ["tn", "teamName"]) ?? ""
            
            let budget = extractInt(from: userData, keys: ["b", "budget"]) ?? 0
            let teamValue = extractInt(from: userData, keys: ["tv", "teamValue"]) ?? 0
            // Use 'sp' (season points) for total points
            let points = extractInt(from: userData, keys: ["sp", "p", "points"]) ?? 0
            // Use 'spl' (season placement) for placement
            let placement = extractInt(from: userData, keys: ["spl", "pl", "placement"]) ?? 0
            
            // These fields don't exist in ranking API, set to 0 as defaults
            let won = 0
            let drawn = 0
            let lost = 0
            
            // se11, ttm may not be in ranking response either
            let se11 = extractInt(from: userData, keys: ["se11", "s"]) ?? 0
            let ttm = extractInt(from: userData, keys: ["ttm", "t"]) ?? 0
            let mpst = extractInt(from: userData, keys: ["mpst", "maxPlayersPerTeam"])
            
            return LeagueUser(
                id: id,
                name: name,
                teamName: teamName,
                budget: budget,
                teamValue: teamValue,
                points: points,
                placement: placement,
                won: won,
                drawn: drawn,
                lost: lost,
                se11: se11,
                ttm: ttm,
                mpst: mpst
            )
        }
        
        print("✅ Parsed \(users.count) users from ranking")
        return users
    }

    // MARK: - User Stats Parsing

    public func parseUserStatsFromResponse(_ json: [String: Any], fallbackUser: LeagueUser) -> UserStats {
        print("🔍 Parsing user stats from response...")
        print("📋 Stats JSON keys: \(Array(json.keys))")

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

        let teamValue =
            extractInt(from: statsData, keys: ["teamValue", "tv", "marketValue", "mv", "value"])
            ?? fallbackUser.teamValue
        let teamValueTrend =
            extractInt(
                from: statsData,
                keys: ["teamValueTrend", "tvt", "marketValueTrend", "mvt", "trend", "t"]) ?? 0
        let budget =
            extractInt(from: statsData, keys: ["b", "budget", "money", "cash", "funds"])
            ?? fallbackUser.budget
        let points =
            extractInt(from: statsData, keys: ["points", "p", "totalPoints", "tp"])
            ?? fallbackUser.points
        let placement =
            extractInt(from: statsData, keys: ["placement", "pl", "rank", "position", "pos"])
            ?? fallbackUser.placement
        let won =
            extractInt(from: statsData, keys: ["won", "w", "wins", "victories"]) ?? fallbackUser.won
        let drawn =
            extractInt(from: statsData, keys: ["drawn", "d", "draws", "ties"]) ?? fallbackUser.drawn
        let lost =
            extractInt(from: statsData, keys: ["lost", "l", "losses", "defeats"])
            ?? fallbackUser.lost

        // Debug: Zeige Budget-relevante Felder
        print("🔍 Budget-related fields found:")
        if let b = statsData["b"] { print("   b (Budget): \(b)") }
        if let pbas = statsData["pbas"] { print("   pbas (Previous Budget At Start): \(pbas)") }
        if let bs = statsData["bs"] { print("   bs (Budget Start/Spent): \(bs)") }

        let userStats = UserStats(
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
        print("   💰 Budget: €\(budget/1000)k")
        print("   📈 Teamwert: €\(teamValue/1000)k")
        print("   🔄 Trend: €\(teamValueTrend/1000)k")
        print("   🏆 Punkte: \(points) (Platz \(placement))")

        return userStats
    }

    // MARK: - Market Value History Parsing

    public func parseMarketValueHistory(from json: [String: Any]) -> MarketValueChange? {
        print("🔍 Parsing market value history from response...")
        print("📋 History JSON keys: \(Array(json.keys))")

        // Extrahiere das prlo-Feld auf der gleichen Ebene wie "it"
        let prloValue = json["prlo"] as? Int
        print("📊 Found PRLO value at root level: \(prloValue ?? 0)")

        // Extrahiere die "it" Liste mit den Marktwert-Einträgen
        guard let itArray = json["it"] as? [[String: Any]] else {
            print("❌ No 'it' array found in market value history response")
            return nil
        }

        print("📊 Found \(itArray.count) market value entries")

        // Konvertiere zu MarketValueEntry Objekten
        var entries: [MarketValueEntry] = []
        for entryData in itArray {
            if let dt = entryData["dt"] as? Int,
                let mv = entryData["mv"] as? Int
            {
                entries.append(MarketValueEntry(dt: dt, mv: mv))
                print("   📈 Entry dt:\(dt) mv:€\(mv/1000)k")
            }
        }

        // Sortiere nach dt (Datum) absteigend
        entries.sort { $0.dt > $1.dt }

        // Berechne die Änderung seit dem letzten Tag
        let currentEntry = entries.first
        let previousEntry = entries.dropFirst().first

        let absoluteChange = (currentEntry?.mv ?? 0) - (previousEntry?.mv ?? 0)
        let percentageChange =
            previousEntry?.mv != 0
            ? (Double(absoluteChange) / Double(previousEntry!.mv)) * 100.0 : 0.0

        let daysDifference = (currentEntry?.dt ?? 0) - (previousEntry?.dt ?? 0)

        // Berechne tägliche Änderungen für die letzten drei Tage
        var dailyChanges: [DailyMarketValueChange] = []
        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .medium
        dateFormatter.timeStyle = .none
        dateFormatter.locale = Locale(identifier: "de_DE")

        let maxDays = min(3, entries.count - 1)
        for i in 0..<maxDays {
            let currentDayEntry = entries[i]
            let previousDayEntry = entries[i + 1]

            let dailyChange = currentDayEntry.mv - previousDayEntry.mv
            let dailyPercentageChange =
                previousDayEntry.mv != 0
                ? (Double(dailyChange) / Double(previousDayEntry.mv)) * 100.0 : 0.0

            let date = Date(timeIntervalSince1970: TimeInterval(currentDayEntry.dt * 24 * 60 * 60))
            let dateString = dateFormatter.string(from: date)

            let dailyMarketValueChange = DailyMarketValueChange(
                date: dateString,
                value: currentDayEntry.mv,
                change: dailyChange,
                percentageChange: dailyPercentageChange,
                daysAgo: i
            )

            dailyChanges.append(dailyMarketValueChange)
        }

        let marketValueChange = MarketValueChange(
            daysSinceLastUpdate: daysDifference,
            absoluteChange: absoluteChange,
            percentageChange: percentageChange,
            previousValue: previousEntry?.mv ?? 0,
            currentValue: currentEntry?.mv ?? 0,
            dailyChanges: dailyChanges
        )

        print("✅ Calculated market value change:")
        print("   📈 Absolute change: €\(absoluteChange/1000)k")
        print("   📊 Percentage change: \(String(format: "%.1f", percentageChange))%")

        return marketValueChange
    }
}
