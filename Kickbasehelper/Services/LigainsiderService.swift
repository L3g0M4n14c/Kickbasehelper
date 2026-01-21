import Foundation
import Combine

// MARK: - Models

struct LigainsiderPlayer: Codable, Identifiable, Hashable {
    var id: String { ligainsiderId ?? name }
    let name: String
    let alternative: String? // Name der Alternative
    let ligainsiderId: String? // z.B. "nikola-vasilj_13866"
}

struct LigainsiderMatch: Codable, Identifiable {
    var id: String { homeTeam + (url ?? UUID().uuidString) }
    let homeTeam: String
    let awayTeam: String
    // Aufstellung ist jetzt ein Array von Reihen (z.B. [Torwart, Abwehr, Mittelfeld, Sturm])
    // Jede Reihe ist ein Array von Spielern
    let homeLineup: [[LigainsiderPlayer]]
    let awayLineup: [[LigainsiderPlayer]]
    let url: String?
}

enum LigainsiderStatus {
    case likelyStart // S11 ohne Alternative
    case possibleStart // S11 mit Alternative oder ist Alternative
    case out // Nicht im Kader / nicht gefunden
}

class LigainsiderService: ObservableObject {
    @Published var matches: [LigainsiderMatch] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    // Basis URL
    private let overviewURL = "https://www.ligainsider.de/bundesliga/spieltage/"
    
    // Cache für schnellen Zugriff: LigainsiderId -> LigainsiderPlayer
    // Wir speichern alle Spieler die in S11 oder als Alternative gelistet sind
    private var playerCache: [String: LigainsiderPlayer] = [:]
    // Cache für Alternativen (Namen)
    private var alternativeNames: Set<String> = []
    
    func fetchLineups() {
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                let fetchedMatches = try await scrapeLigaInsider()
                
                // Cache aufbauen
                var newCache: [String: LigainsiderPlayer] = [:]
                var newAlts: Set<String> = []
                
                for match in fetchedMatches {
                    let allRows = match.homeLineup + match.awayLineup
                    for row in allRows {
                        for player in row {
                            // Speichere Hauptspieler
                            if let id = player.ligainsiderId {
                                newCache[id] = player
                            }
                            // Speichere Alternative falls vorhanden
                            if let altName = player.alternative {
                                newAlts.insert(altName.lowercased())
                            }
                        }
                    }
                }
                
                await MainActor.run {
                    self.playerCache = newCache
                    self.alternativeNames = newAlts
                    self.matches = fetchedMatches
                    self.isLoading = false
                    // Backup speichern
                    self.saveToLocal(matches: fetchedMatches)
                }
            } catch {
                await MainActor.run {
                    print("Fehler beim Scrapen: \(error)")
                    self.errorMessage = "Fehler beim Laden: \(error.localizedDescription)"
                    self.isLoading = false
                    // Versuch lokale Daten zu laden
                    self.loadFromLocal()
                }
            }
        }
    }
    
    // Helper für Normalisierung (entfernt Akzente und Sonderzeichen)
    private func normalize(_ text: String) -> String {
        return text.lowercased()
            .folding(options: .diacriticInsensitive, locale: .current)
            .replacingOccurrences(of: "-", with: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    // MARK: - Matching Logic
    
    func getPlayerStatus(firstName: String, lastName: String) -> LigainsiderStatus {
        if matches.isEmpty { return .out } // Noch keine Daten
        
        let normalizedLastName = normalize(lastName)
        let normalizedFirstName = normalize(firstName)
        
        // 1. Suche im Cache via ID (bester Match)
        // Strategie: Wir filtern Cache Keys die den normalisierten Nachnamen enthalten
        
        var foundPlayer: LigainsiderPlayer?
        
        let candidates = playerCache.filter { key, _ in
            let normalizedKey = normalize(key) // key ist z.B. "adam-dzwigala_25807"
            return normalizedKey.contains(normalizedLastName)
        }
        
        if candidates.count == 1 {
            foundPlayer = candidates.first?.value
        } else if candidates.count > 1 {
             let bestMatch = candidates.first(where: { key, _ in 
                 let normalizedKey = normalize(key)
                 return normalizedKey.contains(normalizedFirstName) 
             })
             foundPlayer = bestMatch?.value
        }
        
        // Wenn Spieler gefunden: Check Status
        if let player = foundPlayer {
            if player.alternative != nil {
                return .possibleStart
            }
            return .likelyStart
        }
        
        // 2. Check Alternativen (String Matching)
        let isAlternative = alternativeNames.contains { altName in
            let normalizedAlt = normalize(altName)
            return normalizedLastName == normalizedAlt || normalizedAlt.contains(normalizedLastName)
        }
        
        if isAlternative {
            return .possibleStart
        }
        
        return .out
    }
    
    // Helper für Views
    func getIcon(for status: LigainsiderStatus) -> String {
        switch status {
        case .likelyStart: return "checkmark.circle.fill"
        case .possibleStart: return "questionmark.circle.fill"
        case .out: return "xmark.circle.fill"
        }
    }
    
    func getColor(for status: LigainsiderStatus) -> String { // String Hex oder Color Name
        switch status {
        case .likelyStart: return "green"
        case .possibleStart: return "orange"
        case .out: return "red" // Oder Gray für unauffällig
        }
    }

    
    // MARK: - Native Swift Scraping
    
    private func scrapeLigaInsider() async throws -> [LigainsiderMatch] {
        // 1. Übersicht laden
        print("Lade Übersicht von: \(overviewURL)")
        guard let url = URL(string: overviewURL) else { throw URLError(.badURL) }
        let (data, _) = try await URLSession.shared.data(from: url)
        
        guard let htmlString = String(data: data, encoding: .utf8) else {
            throw URLError(.cannotDecodeContentData)
        }
        
        // 2. Spiel-Links extrahieren
        let linkPattern = #"href=\"(/bundesliga/team/[^"]+/saison-[^"]+)\""#
        var matchLinks: [String] = []
        
        let regex = try NSRegularExpression(pattern: linkPattern, options: [])
        let nsString = htmlString as NSString
        let results = regex.matches(in: htmlString, options: [], range: NSRange(location: 0, length: nsString.length))
        
        for result in results {
            if let range = Range(result.range(at: 1), in: htmlString) {
                let path = String(htmlString[range])
                let fullUrl = "https://www.ligainsider.de" + path
                if !matchLinks.contains(fullUrl) {
                    matchLinks.append(fullUrl)
                }
            }
        }
        
        // Match Pairs bilden
        var matchPairs: [[String]] = []
        var currentPair: [String] = []
        
        for link in matchLinks {
            currentPair.append(link)
            if currentPair.count == 2 {
                matchPairs.append(currentPair)
                currentPair = []
            }
        }
        
        // 3. Details parallel laden
        var finalMatches: [LigainsiderMatch] = []
        
        await withTaskGroup(of: LigainsiderMatch?.self) { group in
            for pair in matchPairs {
                let homeUrl = pair[0]
                let awayUrl = pair[1]
                
                group.addTask {
                    async let homeData = self.fetchTeamData(url: homeUrl)
                    async let awayData = self.fetchTeamData(url: awayUrl)
                    
                    do {
                        let (hName, hLineup) = try await homeData
                        let (aName, aLineup) = try await awayData
                        
                        return LigainsiderMatch(
                            homeTeam: hName,
                            awayTeam: aName,
                            homeLineup: hLineup,
                            awayLineup: aLineup,
                            url: homeUrl
                        )
                    } catch {
                        print("Fehler bei Match Paar: \(error)")
                        return nil
                    }
                }
            }
            
            for await match in group {
                if let match = match {
                    finalMatches.append(match)
                }
            }
        }
        
        return finalMatches
    }
    
    private func fetchTeamData(url: String) async throws -> (String, [[LigainsiderPlayer]]) {
        print("Lade Team Details: \(url)")
        guard let urlObj = URL(string: url) else { return ("Unbekannt", []) }
        let (data, _) = try await URLSession.shared.data(from: urlObj)
        guard let html = String(data: data, encoding: .utf8) else { return ("Unbekannt", []) }
        
        // Teamnamen extrahieren
        // Auf der Match-Seite ist der Title oft "Team A vs Team B | LigaInsider", was zu falschen Anzeigen führt.
        // Besser: Wir suchen speziell nach dem h2 Tag für den Teamnamen in dieser Section.
        var teamName = "Team"
        
        let h2Pattern = #"<h2[^>]*itemprop=["']name["'][^>]*>([^<]+)</h2>"#
        if let h2Regex = try? NSRegularExpression(pattern: h2Pattern, options: .caseInsensitive),
           let match = h2Regex.firstMatch(in: html, options: [], range: NSRange(location: 0, length: html.utf16.count)),
           let range = Range(match.range(at: 1), in: html) {
             teamName = String(html[range]).trimmingCharacters(in: .whitespacesAndNewlines)
        } else if let range = html.range(of: "<title>"), let endRange = html.range(of: "</title>") {
             let title = html[range.upperBound..<endRange.lowerBound]
             // Fallback: Title cleanen. Falls "vs" oder ":" enthalten ist, nehmen wir evtl den falschen String, 
             // aber meistens ist h2 da.
             teamName = title.components(separatedBy: "|").first?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "Team"
        }
        
        var formationRows: [[LigainsiderPlayer]] = []
        
        // Parsing Logik für Aufstellung mit Reihen und Alternativen
        if let startRange = html.range(of: "VORAUSSICHTLICHE AUFSTELLUNG") {
            let contentStart = html[startRange.upperBound...]
            // Limit auf 100k
            let limit = contentStart.index(contentStart.startIndex, offsetBy: min(100000, contentStart.count))
            let searchArea = String(contentStart[..<limit])
            
            // Aufteilen in Rows
            // Wir nutzen 'player_position_row' als Separator
            let rowComponents = searchArea.components(separatedBy: "player_position_row")
            
            // component[0] ist Müll vor der ersten Row
            // Ab component[1] kommen die Reihen
            for i in 1..<rowComponents.count {
                let rowHtml = rowComponents[i]
                
                // Ignorieren wenn es kein "player_position_column" enthält
                if !rowHtml.contains("player_position_column") { continue }
                
                var currentRowPlayers: [LigainsiderPlayer] = []
                
                // Aufteilen in Columns (einzelne Positionen)
                let colComponents = rowHtml.components(separatedBy: "player_position_column")
                
                for j in 1..<colComponents.count {
                    let colHtml = colComponents[j]
                    
                    // Regex um Spielernamen im Link zu finden
                    let playerLinkPattern = #"<a[^>]+href=["']\/([a-z0-9-]+_\d+)\/["'][^>]*>\s*([^<]+)\s*<\/a>"#
                    let regex = try NSRegularExpression(pattern: playerLinkPattern, options: .caseInsensitive)
                    let nsString = colHtml as NSString
                    let results = regex.matches(in: colHtml, options: [], range: NSRange(location: 0, length: nsString.length))
                    
                    var namesInColumn: [(String, String)] = []
                    var seen = Set<String>()
                    
                    for result in results {
                        if result.numberOfRanges > 2 {
                            let slugRange = result.range(at: 1) // id/slug z.B. nikola-vasilj_13866
                            let nameRange = result.range(at: 2) // display text
                            
                            let slug = nsString.substring(with: slugRange)
                            let name = nsString.substring(with: nameRange).trimmingCharacters(in: .whitespacesAndNewlines)
                            
                            if name.count > 1 && !seen.contains(name) {
                                seen.insert(name)
                                // Speichere Namen UND die ID (slug) für besseres Matching
                                namesInColumn.append((name, slug))
                            }
                        }
                    }
                    
                    if let (mainName, mainSlug) = namesInColumn.first {
                        let alternativeName = namesInColumn.count > 1 ? namesInColumn[1].0 : nil
                        currentRowPlayers.append(LigainsiderPlayer(name: mainName, alternative: alternativeName, ligainsiderId: mainSlug))
                    }
                }
                
                if !currentRowPlayers.isEmpty {
                    formationRows.append(currentRowPlayers)
                }
            }
        }
        
        // Fallback falls Parsing fehlschlägt: Leeres Array
        return (teamName, formationRows)
    }
    
    // Backup (Lokal Speichern)
    private func saveToLocal(matches: [LigainsiderMatch]) {
        if let data = try? JSONEncoder().encode(matches),
           let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first?.appendingPathComponent("lineups_v2_backup.json") {
            try? data.write(to: url)
        }
    }
    
    private func loadFromLocal() {
         if let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first?.appendingPathComponent("lineups_v2_backup.json"),
            let data = try? Data(contentsOf: url),
            let matches = try? JSONDecoder().decode([LigainsiderMatch].self, from: data) {
             self.matches = matches
         }
    }
}
