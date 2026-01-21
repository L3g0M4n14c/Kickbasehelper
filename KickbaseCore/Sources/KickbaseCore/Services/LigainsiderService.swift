import Combine
import Foundation

// MARK: - Models

public struct LigainsiderPlayer: Codable, Identifiable, Hashable {
    public var id: String { ligainsiderId ?? name }
    public let name: String
    public let alternative: String?  // Name der Alternative
    public let ligainsiderId: String?  // z.B. "nikola-vasilj_13866"
}

public struct LigainsiderMatch: Codable, Identifiable {
    public var id: String { homeTeam + (url ?? UUID().uuidString) }
    public let homeTeam: String
    public let awayTeam: String
    // Aufstellung ist jetzt ein Array von Reihen (z.B. [Torwart, Abwehr, Mittelfeld, Sturm])
    // Jede Reihe ist ein Array von Spielern
    public let homeLineup: [[LigainsiderPlayer]]
    public let awayLineup: [[LigainsiderPlayer]]
    public let url: String?
}

public enum LigainsiderStatus {
    case likelyStart  // S11 ohne Alternative
    case possibleStart  // S11 mit Alternative oder ist Alternative
    case out  // Nicht im Kader / nicht gefunden
}

public class LigainsiderService: ObservableObject {
    @Published public var matches: [LigainsiderMatch] = []
    @Published public var isLoading = false
    @Published public var errorMessage: String?

    // Basis URL
    private let overviewURL = "https://www.ligainsider.de/bundesliga/spieltage/"

    // Cache für schnellen Zugriff: LigainsiderId -> LigainsiderPlayer
    // Wir speichern alle Spieler die in S11 oder als Alternative gelistet sind
    private var playerCache: [String: LigainsiderPlayer] = [:]
    // Cache für Alternativen (Namen)
    private var alternativeNames: Set<String> = []

    public init() {}

    public func fetchLineups() {
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
        #if !SKIP
            return text.lowercased()
                .folding(options: .diacriticInsensitive, locale: .current)
                .replacingOccurrences(of: "-", with: " ")
                .trimmingCharacters(in: CharacterSet.whitespacesAndNewlines)
        #else
            // Android: Einfache Normalisierung ohne Diacritic Removal (da folding nicht verfügbar)
            return text.lowercased()
                .replacingOccurrences(of: "-", with: " ")
                .trimmingCharacters(in: CharacterSet.whitespacesAndNewlines)
        #endif
    }

    // MARK: - Matching Logic

    public func getPlayerStatus(firstName: String, lastName: String) -> LigainsiderStatus {
        if matches.isEmpty { return .out }  // Noch keine Daten

        let normalizedLastName = normalize(lastName)
        let normalizedFirstName = normalize(firstName)

        // 1. Suche im Cache via ID (bester Match)
        // Strategie: Wir filtern Cache Keys die den normalisierten Nachnamen enthalten

        var foundPlayer: LigainsiderPlayer?

        let candidates = playerCache.filter { key, _ in
            let normalizedKey = normalize(key)  // key ist z.B. "adam-dzwigala_25807"
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
    public func getIcon(for status: LigainsiderStatus) -> String {
        switch status {
        case .likelyStart: return "checkmark.circle.fill"
        case .possibleStart: return "questionmark.circle.fill"
        case .out: return "xmark.circle.fill"
        }
    }

    public func getColor(for status: LigainsiderStatus) -> String {  // String Hex oder Color Name
        switch status {
        case .likelyStart: return "green"
        case .possibleStart: return "orange"
        case .out: return "red"  // Oder Gray für unauffällig
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

        // 2. Spiel-Links extrahieren (String-Parsing statt Regex für Skip-Support)
        var matchLinks: [String] = []

        // Wir suchen nach href="/bundesliga/team/.../saison-..."
        let components = htmlString.components(separatedBy: "href=\"")
        for component in components.dropFirst() {
            // component beginnt mit dem URL-Pfad
            if let quoteIndex = component.firstIndex(of: "\"") {
                let path = String(component[..<quoteIndex])

                if path.contains("/bundesliga/team/") && path.contains("/saison-") {
                    let fullUrl = "https://www.ligainsider.de" + path
                    if !matchLinks.contains(fullUrl) {
                        matchLinks.append(fullUrl)
                    }
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
        var teamName = "Team"

        // Suche nach: <h2 ... itemprop="name">Team Name</h2>
        let nameComponents = html.components(separatedBy: "itemprop=\"name\"")
        if nameComponents.count > 1 {
            let partAfterName = nameComponents[1]  // > Team name </h2> ...
            if let closingTagIdx = partAfterName.range(of: ">"),
                let headerEndIdx = partAfterName.range(of: "</h2>")
            {

                let start = closingTagIdx.upperBound
                let end = headerEndIdx.lowerBound
                if start < end {
                    teamName = String(partAfterName[start..<end])
                        .trimmingCharacters(in: CharacterSet.whitespacesAndNewlines)
                }
            }
        } else if let titleStart = html.range(of: "<title>"),
            let titleEnd = html.range(of: "</title>", range: titleStart.upperBound..<html.endIndex)
        {
            let title = html[titleStart.upperBound..<titleEnd.lowerBound]
            teamName =
                title.components(separatedBy: "|").first?.trimmingCharacters(
                    in: CharacterSet.whitespacesAndNewlines) ?? "Team"
        }

        var formationRows: [[LigainsiderPlayer]] = []

        // Parsing Logik für Aufstellung
        if let startRange = html.range(of: "VORAUSSICHTLICHE AUFSTELLUNG") {
            let contentStart = html[startRange.upperBound...]
            let limit = contentStart.index(
                contentStart.startIndex, offsetBy: min(100000, contentStart.count))
            let searchArea = String(contentStart[..<limit])

            // Falls das Ende der Aufstellung durch die Legende markiert ist, schneiden wir dort ab
            // Das verhindert, dass Links aus dem Footer (News, Kommentare) fälschlicherweise als Spieler erkannt werden
            var cleanSearchArea = searchArea
            if let legendRange = cleanSearchArea.range(of: "Spieler stand in der Startelf") {
                cleanSearchArea = String(cleanSearchArea[..<legendRange.lowerBound])
            }

            // Aufteilen in Rows
            let rowComponents = cleanSearchArea.components(separatedBy: "player_position_row")

            for i in 1..<rowComponents.count {
                let rowHtml = rowComponents[i]
                if !rowHtml.contains("player_position_column") { continue }

                var currentRowPlayers: [LigainsiderPlayer] = []
                let colComponents = rowHtml.components(separatedBy: "player_position_column")

                for j in 1..<colComponents.count {
                    let colHtml = colComponents[j]

                    // Suche: <a ... href="/id/" ... >Name</a>
                    let linkComponents = colHtml.components(separatedBy: "<a ")

                    var namesInColumn: [(String, String)] = []
                    var seen = Set<String>()

                    for linkPart in linkComponents.dropFirst() {
                        // href finden
                        guard let hrefRange = linkPart.range(of: "href=\"/"),
                            let hrefEnd = linkPart.range(
                                of: "/\"", range: hrefRange.upperBound..<linkPart.endIndex)
                        else { continue }

                        let slug = String(linkPart[hrefRange.upperBound..<hrefEnd.lowerBound])
                        // Slug muss ID enthalten (z.B. name_12345)
                        if !slug.contains("_") { continue }
                        // Darf keine Slashes enthalten (wären Sub-Pfade wie News)
                        if slug.contains("/") { continue }

                        // Check if last char is digit (Skip workaround for .isNumber)
                        if let last = slug.last, !"0123456789".contains(last) {
                            continue
                        }

                        // name finden (zwischen > und </a>)
                        guard let tagClose = linkPart.range(of: ">"),
                            let aClose = linkPart.range(
                                of: "</a>", range: tagClose.upperBound..<linkPart.endIndex)
                        else { continue }

                        let rawName = String(linkPart[tagClose.upperBound..<aClose.lowerBound])
                        // HTML Tags entfernen bevor getrimmt wird
                        let clearName = removeHtmlTags(rawName)
                        let name = clearName.trimmingCharacters(
                            in: CharacterSet.whitespacesAndNewlines)

                        if name.count > 1 && !seen.contains(name) {
                            seen.insert(name)
                            namesInColumn.append((name, slug))
                        }
                    }

                    if let (mainName, mainSlug) = namesInColumn.first {
                        let alternativeName = namesInColumn.count > 1 ? namesInColumn[1].0 : nil
                        currentRowPlayers.append(
                            LigainsiderPlayer(
                                name: mainName, alternative: alternativeName,
                                ligainsiderId: mainSlug))
                    }
                }

                if !currentRowPlayers.isEmpty {
                    formationRows.append(currentRowPlayers)
                }
            }
        }

        return (teamName, formationRows)
    }

    // Helper zum Entfernen von HTML Tags
    private func removeHtmlTags(_ text: String) -> String {
        var result = ""
        var insideTag = false

        for char in text {
            if char == "<" {
                insideTag = true
            } else if char == ">" {
                insideTag = false
            } else if !insideTag {
                result.append(char)
            }
        }
        return result
    }

    // Backup (Lokal Speichern)
    private func saveToLocal(matches: [LigainsiderMatch]) {
        #if !SKIP
            if let data = try? JSONEncoder().encode(matches),
                let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
                    .first?.appendingPathComponent("lineups_v2_backup.json")
            {
                try? data.write(to: url)
            }
        #endif
    }

    private func loadFromLocal() {
        #if !SKIP
            if let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
                .first?.appendingPathComponent("lineups_v2_backup.json"),
                let data = try? Data(contentsOf: url),
                let cached = try? JSONDecoder().decode([LigainsiderMatch].self, from: data)
            {
                self.matches = cached
            }
        #endif
    }
}
