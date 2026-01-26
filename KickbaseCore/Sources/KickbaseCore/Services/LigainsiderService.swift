import Combine
import Foundation
import SwiftUI

// MARK: - Models

public struct LigainsiderPlayer: Codable, Identifiable, Hashable {
    public var id: String { ligainsiderId ?? name }
    public let name: String
    public let alternative: String?  // Name der Alternative
    public let ligainsiderId: String?  // z.B. "nikola-vasilj_13866"
    public let imageUrl: String?  // URL zum Profilbild
}

public struct LineupRow: Codable, Identifiable {
    public var id = UUID()
    public let players: [LigainsiderPlayer]

    public init(players: [LigainsiderPlayer]) {
        self.players = players
    }
}

public struct LigainsiderMatch: Codable, Identifiable {
    public var id: String { homeTeam + (url ?? UUID().uuidString) }
    public let homeTeam: String
    public let awayTeam: String
    public let homeLogo: String?
    public let awayLogo: String?
    // Aufstellung ist jetzt ein Array von Reihen (z.B. [Torwart, Abwehr, Mittelfeld, Sturm])
    // Jede Reihe ist ein Array von Spielern
    public let homeLineup: [LineupRow]
    public let awayLineup: [LineupRow]
    public let homeSquad: [LigainsiderPlayer]
    public let awaySquad: [LigainsiderPlayer]
    public let url: String?
}

public enum LigainsiderStatus {
    case likelyStart  // S11 ohne Alternative
    case startWithAlternative  // S11 mit Alternative (1. Option)
    case isAlternative  // Ist die Alternative (2. Option)
    case bench  // Auf der Bank / im Kader aber nicht in S11
    case out  // Nicht im Kader / nicht gefunden
}

public class LigainsiderService: ObservableObject {
    @Published public var matches: [LigainsiderMatch] = []
    @Published public var isLoading = false
    @Published public var errorMessage: String?
    @Published public var cacheUpdateTrigger = UUID()  // Triggert Re-Render wenn Cache sich ändert
    @Published public var isLigainsiderReady = false  // true wenn Cache vollständig geladen ist

    // Basis URL
    private let overviewURL = "https://www.ligainsider.de/bundesliga/spieltage/"

    // Cache für schnellen Zugriff: LigainsiderId -> LigainsiderPlayer
    // Wir speichern alle Spieler die in S11 oder als Alternative gelistet sind
    private var playerCache: [String: LigainsiderPlayer] = [:]

    // Public readonly access for debugging
    public var playerCacheCount: Int {
        return playerCache.count
    }

    // Cache für Alternativen (Namen)
    private var alternativeNames: Set<String> = []
    // Cache für Spieler in der Startelf (IDs)
    private var startingLineupIds: Set<String> = []

    public init() {}

    // MARK: - Async variant for initialization (waits for completion)
    public func fetchLineupsAsync() async {
        isLoading = true
        errorMessage = nil

        do {
            let fetchedMatches = try await scrapeLigaInsider()

            // Cache aufbauen
            var newCache: [String: LigainsiderPlayer] = [:]
            var newAlts: Set<String> = []
            var newLineupIds: Set<String> = []

            for match in fetchedMatches {
                // Zuerst Kader zum Cache hinzufügen (für Bilder von Bankspielern/Alternativen)
                let allSquad = match.homeSquad + match.awaySquad
                for player in allSquad {
                    if let id = player.ligainsiderId {
                        newCache[id] = player
                    }
                }

                let allRows = match.homeLineup + match.awayLineup
                for row in allRows {
                    for player in row.players {
                        // Speichere Hauptspieler (überschreibt Kader-Eintrag -> wichtig wegen 'alternative' Property)
                        if let id = player.ligainsiderId {
                            newCache[id] = player
                            newLineupIds.insert(id)  // Markiere als Startelfspieler
                        }
                        // Speichere Alternative falls vorhanden
                        if let altName = player.alternative {
                            newAlts.insert(altName.lowercased())
                        }
                    }
                }
            }

            await MainActor.run {
                // Merge new cache into existing to preserve squad data
                for (id, player) in newCache {
                    // Preserve existing imageUrl if new player doesn't have one
                    if let existingPlayer = self.playerCache[id],
                        let existingImageUrl = existingPlayer.imageUrl,
                        player.imageUrl == nil
                    {
                        // Keep existing image
                        self.playerCache[id] = LigainsiderPlayer(
                            name: player.name,
                            alternative: player.alternative,
                            ligainsiderId: player.ligainsiderId,
                            imageUrl: existingImageUrl
                        )
                    } else {
                        self.playerCache[id] = player
                    }
                }
                // Merge alternatives
                for alt in newAlts {
                    self.alternativeNames.insert(alt)
                }
                // Merge lineup IDs
                for id in newLineupIds {
                    self.startingLineupIds.insert(id)
                }

                print(
                    "[Ligainsider] fetchLineupsAsync complete: newCache had \(newCache.count) players, total playerCache now: \(self.playerCache.count)"
                )
                print("[Ligainsider] Alternative names found: \(self.alternativeNames.count)")
                print("[Ligainsider] Starting lineup IDs: \(self.startingLineupIds.count)")

                self.matches = fetchedMatches
                self.isLoading = false
                // Trigger UI-Updates durch Cache-Signal auf Main Thread
                print("[DEBUG] 🔔 Setting cacheUpdateTrigger on Main Thread")
                self.cacheUpdateTrigger = UUID()
                print("[DEBUG] ✅ cacheUpdateTrigger set: \(self.cacheUpdateTrigger)")
                // Mark as ready - Cache ist vollständig geladen
                self.isLigainsiderReady = true
                print("[DEBUG] ✅ Ligainsider is ready: \(self.playerCache.count) players loaded")

                // Backup speichern (kann im Background sein)
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

    public func fetchLineups() {
        isLoading = true
        errorMessage = nil

        Task {
            await fetchLineupsAsync()
        }
    }

    // Helper für Normalisierung (entfernt Akzente und Sonderzeichen)
    private func normalize(_ text: String) -> String {
        // Manuelle Transliteration für deutsche Umlaute (da IDs oft ae/oe/ue nutzen)
        let manualReplacement = text.lowercased()
            .replacingOccurrences(of: "ä", with: "ae")
            .replacingOccurrences(of: "ö", with: "oe")
            .replacingOccurrences(of: "ü", with: "ue")
            .replacingOccurrences(of: "ß", with: "ss")
            // Manuelle Transliteration für kroatische/slawische Buchstaben
            .replacingOccurrences(of: "ć", with: "c")
            .replacingOccurrences(of: "č", with: "c")
            .replacingOccurrences(of: "š", with: "s")
            .replacingOccurrences(of: "ž", with: "z")
            .replacingOccurrences(of: "đ", with: "d")

        #if !SKIP
            return
                manualReplacement
                .folding(options: .diacriticInsensitive, locale: .current)
                .replacingOccurrences(of: "-", with: " ")
                .trimmingCharacters(in: CharacterSet.whitespacesAndNewlines)
        #else
            // Android: Einfache Normalisierung ohne Diacritic Removal (da folding nicht verfügbar)
            return
                manualReplacement
                .replacingOccurrences(of: "-", with: " ")
                .trimmingCharacters(in: CharacterSet.whitespacesAndNewlines)
        #endif
    }

    // MARK: - Matching Logic

    public func getLigainsiderPlayer(firstName: String, lastName: String) -> LigainsiderPlayer? {
        // Check if cache has any data (not just matches)
        if playerCache.isEmpty {
            print("[MATCHING] ❌ Cache EMPTY for \(firstName) \(lastName)")
            return nil
        }

        let normalizedLastName = normalize(lastName)
        let normalizedFirstName = normalize(firstName)
        print(
            "[MATCHING] 🔍 Searching: '\(firstName)' '\(lastName)' (normalized: '\(normalizedFirstName)' '\(normalizedLastName)') in cache of \(playerCache.count) players"
        )

        // First try: exact lastName match (as separate word)
        let candidates = playerCache.filter { key, _ in
            let normalizedKey = normalize(key)
            // Split by space and underscore to get name parts
            let keyParts = normalizedKey.components(separatedBy: CharacterSet(charactersIn: " _-"))
            return keyParts.contains(normalizedLastName)
        }

        print(
            "   → Step 1: Found \(candidates.count) candidates by last name '\(normalizedLastName)'"
        )
        if candidates.count > 0 && candidates.count <= 3 {
            candidates.forEach { print("[Ligainsider]   - \($0.key)") }
        }

        if candidates.count == 1 {
            print(
                "[Ligainsider] FOUND (exact): \(firstName) \(lastName) -> \(candidates.first?.key ?? "")"
            )
            return candidates.first?.1
        } else if candidates.count > 1 {
            // Multiple matches: use firstName to disambiguate
            let bestMatch = candidates.first(where: { key, _ in
                let normalizedKey = normalize(key)
                let keyParts = normalizedKey.components(
                    separatedBy: CharacterSet(charactersIn: " _-"))
                return keyParts.contains(normalizedFirstName)
            })
            if let match = bestMatch {
                print(
                    "[Ligainsider] FOUND (firstName disamb): \(firstName) \(lastName) -> \(match.key)"
                )
                return match.1
            }
            // If no firstName match, try to find one where lastName is first in the key
            let firstLastNameMatch = candidates.first(where: { key, _ in
                let normalizedKey = normalize(key)
                return normalizedKey.hasPrefix(normalizedLastName)
                    || normalizedKey.hasPrefix(normalizedFirstName)
            })
            if let match = firstLastNameMatch {
                print(
                    "[Ligainsider] FOUND (prefix match): \(firstName) \(lastName) -> \(match.key)")
                return match.1
            }
            if let match = candidates.first {
                print(
                    "[Ligainsider] FOUND (first of many): \(firstName) \(lastName) -> \(match.key)")
                return match.1
            }
        }

        // Fallback: loose contains matching (for partial names)
        print("[Ligainsider] Step2: Trying loose contains matching for '\(normalizedLastName)'")
        let looseCandidates = playerCache.filter { key, _ in
            let normalizedKey = normalize(key)
            return normalizedKey.contains(normalizedLastName)
        }

        print("[Ligainsider] Found \(looseCandidates.count) loose candidates")
        if looseCandidates.count == 1 {
            print(
                "[Ligainsider] FOUND (loose exact): \(firstName) \(lastName) -> \(looseCandidates.first?.key ?? "")"
            )
            return looseCandidates.first?.1
        } else if looseCandidates.count > 1 {
            let bestMatch = looseCandidates.first(where: { key, _ in
                let normalizedKey = normalize(key)
                return normalizedKey.contains(normalizedFirstName)
            })
            if let match = bestMatch {
                print(
                    "[Ligainsider] FOUND (loose firstName): \(firstName) \(lastName) -> \(match.key)"
                )
                return match.1
            }
            print("[Ligainsider] NOT FOUND: Multiple loose matches and no firstName match")
        }

        print("[Ligainsider] NOT FOUND: \(firstName) \(lastName)")
        return nil
    }

    public func getPlayerStatus(firstName: String, lastName: String) -> LigainsiderStatus {
        if matches.isEmpty { return .out }  // Noch keine Daten

        let normalizedLastName = normalize(lastName)
        let normalizedFirstName = normalize(firstName)

        // 1. Zuerst prüfen ob Spieler in alternativeNames ist (wichtig für korrekte Statusanzeige)
        let isAlternative = alternativeNames.contains { altName in
            let normalizedAlt = normalize(altName)
            return normalizedLastName == normalizedAlt || normalizedAlt.contains(normalizedLastName)
        }

        if isAlternative {
            return .isAlternative
        }

        // 2. Suche im Cache via ID (bester Match)
        // Strategie: Wir filtern Cache Keys die den normalisierten Nachnamen enthalten

        var foundPlayer: LigainsiderPlayer?
        var foundId: String?

        let candidates = playerCache.filter { key, _ in
            let normalizedKey = normalize(key)  // key ist z.B. "adam-dzwigala_25807"
            return normalizedKey.contains(normalizedLastName)
        }

        if candidates.count == 1 {
            foundPlayer = candidates.first?.1
            foundId = candidates.first?.0
        } else if candidates.count > 1 {
            let bestMatch = candidates.first(where: { key, _ in
                let normalizedKey = normalize(key)
                return normalizedKey.contains(normalizedFirstName)
            })
            // Fallback: Lockereres Matching bei Statusabfrage
            if let match = bestMatch ?? candidates.first {
                foundPlayer = match.1
                foundId = match.0
            }
        }

        // Wenn Spieler gefunden: Check Status
        if let player = foundPlayer, let id = foundId {
            // Prüfe ob Spieler in der Startelf ist
            if startingLineupIds.contains(id) {
                // Spieler ist in Startelf
                if player.alternative != nil {
                    return .startWithAlternative
                }
                return .likelyStart
            } else {
                // Spieler ist im Kader aber nicht in Startelf -> Bank
                return .bench
            }
        }

        return .out
    }

    // Helper für Views
    public func getIcon(for status: LigainsiderStatus) -> String {
        switch status {
        case .likelyStart: return "checkmark.circle.fill"
        case .startWithAlternative: return "1.circle.fill"
        case .isAlternative: return "2.circle.fill"
        case .bench: return "person.fill.badge.minus"
        case .out: return "xmark.circle.fill"
        }
    }

    public func getColor(for status: LigainsiderStatus) -> Color {
        switch status {
        case .likelyStart: return .green
        case .startWithAlternative: return .orange
        case .isAlternative: return .orange
        case .bench: return .gray
        case .out: return .red
        }
    }

    // MARK: - Squad Scraping

    private let teamSquadPaths: [String: String] = [
        "FC Bayern München": "/fc-bayern-muenchen/1/kader/",
        "Borussia Dortmund": "/borussia-dortmund/14/kader/",
        "RB Leipzig": "/rb-leipzig/43/kader/",
        "Bayer 04 Leverkusen": "/bayer-04-leverkusen/4/kader/",
        "VfB Stuttgart": "/vfb-stuttgart/11/kader/",
        "Eintracht Frankfurt": "/eintracht-frankfurt/5/kader/",
        "VfL Wolfsburg": "/vfl-wolfsburg/24/kader/",
        "SC Freiburg": "/sc-freiburg/8/kader/",
        "1. FC Heidenheim": "/1-fc-heidenheim-1846/1376/kader/",
        "Werder Bremen": "/werder-bremen/6/kader/",
        "FC Augsburg": "/fc-augsburg/80/kader/",
        "TSG Hoffenheim": "/tsg-hoffenheim/30/kader/",
        "1. FSV Mainz 05": "/1-fsv-mainz-05/16/kader/",
        "Borussia M'gladbach": "/borussia-moenchengladbach/13/kader/",
        "1. FC Union Berlin": "/1-fc-union-berlin/62/kader/",
        "VfL Bochum": "/vfl-bochum/29/kader/",
        "FC St. Pauli": "/fc-st-pauli/20/kader/",
        "Holstein Kiel": "/holstein-kiel/321/kader/",
    ]

    // MARK: - Async variant for initialization (waits for completion)
    public func fetchAllSquadsAsync() async {
        print("Starte Kader-Abruf für alle Teams...")
        await withTaskGroup(of: [LigainsiderPlayer].self) { group in
            for (teamName, path) in teamSquadPaths {
                group.addTask {
                    return await self.fetchSquad(path: path, teamName: teamName)
                }
            }

            var accumulatedPlayers: [LigainsiderPlayer] = []
            for await squad in group {
                // Start manually iterating to avoid Sequence type issues
                for player in squad {
                    let p = player as LigainsiderPlayer
                    accumulatedPlayers.append(p)
                }
            }

            await MainActor.run {
                print("Kader-Abruf beendet. Gefundene Spieler: \(accumulatedPlayers.count)")
                for player in accumulatedPlayers {
                    if let id = player.ligainsiderId {
                        // Update playerCache safely
                        if let existing = self.playerCache[id] {
                            // Update image if we found one and existing didn't have one
                            if existing.imageUrl == nil && player.imageUrl != nil {
                                self.playerCache[id] = LigainsiderPlayer(
                                    name: existing.name,  // Keep existing name logic
                                    alternative: existing.alternative,
                                    ligainsiderId: existing.ligainsiderId,
                                    imageUrl: player.imageUrl
                                )
                            }
                        } else {
                            self.playerCache[id] = player
                        }
                    }
                }
            }
        }
    }

    public func fetchAllSquads() {
        Task {
            await fetchAllSquadsAsync()
        }
    }

    private func fetchSquad(path: String, teamName: String) async -> [LigainsiderPlayer] {
        let fullUrl = "https://www.ligainsider.de" + path

        guard let url = URL(string: fullUrl) else { return [] }

        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            guard let html = String(data: data, encoding: .utf8) else { return [] }

            var players: [LigainsiderPlayer] = []
            // Robustere Suche: Wir suchen nach allen Links auf Spielerprofile
            // Pattern: href="/(name)_(id)/"

            let components = html.components(separatedBy: "href=\"/")

            // Wir iterieren mit Index, um auf vorherige Komponenten zugreifen zu können (für Bilder davor)
            for i in 1..<components.count {
                let component = components[i]
                let previousComponent = components[i - 1]

                // 1. Slug extrahieren
                guard let slug = component.substringBefore("\"") else { continue }

                // Validierung
                if !slug.contains("_") { continue }
                if slug.contains("/") { continue }
                let lastChar = slug.last ?? " "
                if !"0123456789".contains(lastChar) { continue }

                // 2. Name extrahieren
                guard let rawName = component.substringBetween(">", "</a>") else { continue }
                let name = removeHtmlTags(rawName).trimmingCharacters(in: .whitespacesAndNewlines)

                if name.isEmpty || name.count > 50 { continue }

                // 3. Bild suchen
                var imageUrl: String?

                // A) Im Link selbst (contentInLink ist rawName content)
                let contentInLink = rawName
                if contentInLink.contains("<img") {
                    if let extracted = contentInLink.substringBetween("src=\"", "\"") {
                        if extracted.contains("ligainsider.de") {
                            imageUrl = extracted
                        }
                    }
                }

                // B) Kurz vor dem Link (im previousComponent)
                if imageUrl == nil {
                    // Simple Suche von hinten im Previous Component
                    // Wir suchen das letzte Vorkommen von src="..." das ligainsider.de enthält
                    // Da findLastRange komplex ist, splitten wir einfach nach src=" und nehmen das letzte was passt
                    let parts = previousComponent.components(separatedBy: "src=\"")
                    if parts.count > 1 {
                        for part in parts.reversed() {
                            if let candidate = part.substringBefore("\""),
                                candidate.contains("ligainsider.de")
                            {
                                imageUrl = candidate
                                break
                            }
                        }
                    }
                }

                if !players.contains(where: { $0.ligainsiderId == slug }) {
                    players.append(
                        LigainsiderPlayer(
                            name: name, alternative: nil, ligainsiderId: slug, imageUrl: imageUrl))
                }
            }

            return players
        } catch {
            print("Fehler Kader \(teamName): \(error)")
            return []
        }
    }

    // MARK: - Native Swift Scraping

    private func scrapeLigaInsider() async throws -> [LigainsiderMatch] {
        // 1. Übersicht laden
        print("Lade Übersicht von: \(overviewURL)")
        guard let url = URL(string: overviewURL) else { throw URLError(.badURL) }
        let sessionResponse = try await URLSession.shared.data(from: url)
        let data = sessionResponse.0

        guard let htmlString = String(data: data, encoding: .utf8) else {
            throw URLError(.cannotDecodeContentData)
        }

        // 2. Spiel-Links extrahieren (String-Parsing statt Regex für Skip-Support)
        var matchLinks: [String] = []

        // Wir suchen nach href="/bundesliga/team/.../saison-..."
        let components = htmlString.splitBy("href=\"")
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

        // 3. Details laden (parallel für bessere Performance)
        print("Starte paralleles Laden der Match-Details...")
        return await withTaskGroup(of: LigainsiderMatch?.self) { group in
            for pair in matchPairs {
                group.addTask {
                    let homeUrl = pair[0]
                    let awayUrl = pair[1]

                    do {
                        let home = try await self.fetchTeamData(url: homeUrl)
                        let away = try await self.fetchTeamData(url: awayUrl)

                        return LigainsiderMatch(
                            homeTeam: home.name,
                            awayTeam: away.name,
                            homeLogo: home.logo,
                            awayLogo: away.logo,
                            homeLineup: home.lineup.map { LineupRow(players: $0) },
                            awayLineup: away.lineup.map { LineupRow(players: $0) },
                            homeSquad: home.squad,
                            awaySquad: away.squad,
                            url: homeUrl
                        )
                    } catch {
                        print("Fehler bei Match Paar: \(error)")
                        return nil
                    }
                }
            }

            var finalMatches: [LigainsiderMatch] = []
            for await match in group {
                if let match = match {
                    finalMatches.append(match)
                }
            }
            return finalMatches
        }
    }

    private struct TeamDataResult {
        let name: String
        let logo: String?
        let lineup: [[LigainsiderPlayer]]
        let squad: [LigainsiderPlayer]
    }

    private func fetchTeamData(url: String) async throws -> TeamDataResult {
        print("Lade Team Details: \(url)")
        guard let urlObj = URL(string: url) else {
            return TeamDataResult(name: "Unbekannt", logo: nil, lineup: [], squad: [])
        }
        let sessionResponse = try await URLSession.shared.data(from: urlObj)
        let data = sessionResponse.0
        guard let html = String(data: data, encoding: .utf8) else {
            return TeamDataResult(name: "Unbekannt", logo: nil, lineup: [], squad: [])
        }

        // Teamnamen extrahieren
        var teamName = "Team"
        var teamLogo: String?

        // Suche nach: <h2 ... itemprop="name">Team Name</h2>
        let nameComponents = html.splitBy("itemprop=\"name\"")
        if nameComponents.count > 1 {
            // Logo suchen im Teil davor (letzte src="..." vor dem Namen)
            // Struktur: <div ...><a ...><img src="..."></a></div> ... <h2 ... itemprop="name">
            let partBeforeName = nameComponents[0]
            // Wir nutzen last components logic um das letzte bild zu finden?
            // "src=\"" explizit suchen
            let srcParts = partBeforeName.components(separatedBy: "src=\"")
            if srcParts.count > 1, let lastPart = srcParts.last,
                let logoUrl = lastPart.substringBefore("\"")
            {
                if logoUrl.contains("ligainsider.de")
                    && (logoUrl.contains("wappen") || logoUrl.contains("images/teams"))
                {
                    teamLogo = logoUrl
                }
            }

            let partAfterName = nameComponents[1]  // > Team name </h2> ...
            if let name = partAfterName.substringBetween(">", "</h2>") {
                teamName = name.trimmingCharacters(in: .whitespacesAndNewlines)
            }
        } else if let title = html.substringBetween("<title>", "</title>") {
            teamName =
                title.components(separatedBy: "|").first?.trimmingCharacters(
                    in: .whitespacesAndNewlines) ?? "Team"
        }

        var formationRows: [[LigainsiderPlayer]] = []
        var allParsedPlayers: [LigainsiderPlayer] = []  // Sammelt ALLE gefundenen Spieler (inkl. Alternativen)

        // Parsing Logik für Aufstellung
        // Support for both Pre-Match (VORAUSSICHTLICHE AUFSTELLUNG) and Live/Post-Match (Voraussichtliche Aufstellung und Ergebnisse) headers
        let headerMarkers = ["VORAUSSICHTLICHE AUFSTELLUNG", "Voraussichtliche Aufstellung"]
        var contentStart: String?

        for marker in headerMarkers {
            if let found = html.substringAfter(marker) {
                contentStart = found
                break
            }
        }

        if let contentStart = contentStart {
            let limit = min(100000, contentStart.count)
            let searchArea = String(contentStart.prefix(limit))

            // Falls das Ende der Aufstellung durch die Legende markiert ist, schneiden wir dort ab
            // Das verhindert, dass Links aus dem Footer (News, Kommentare) fälschlicherweise als Spieler erkannt werden
            var cleanSearchArea = searchArea
            if let beforeLegend = cleanSearchArea.substringBefore("Spieler stand in der Startelf") {
                cleanSearchArea = beforeLegend
            }

            // Aufteilen in Rows
            let rowComponents = cleanSearchArea.splitBy("player_position_row")

            for i in 1..<rowComponents.count {
                let rowHtml = rowComponents[i]
                if !rowHtml.contains("player_position_column") { continue }

                var currentRowPlayers: [LigainsiderPlayer] = []
                let colComponents = rowHtml.splitBy("player_position_column")

                for j in 1..<colComponents.count {
                    let colHtml = colComponents[j]

                    // Extrahiere ALLE Bild URLs in dieser Spalte
                    // Struktur: main player photo + sub_pic photos für Alternativen
                    var allImageUrls: [String] = []
                    let srcComponents = colHtml.splitBy("src=\"")
                    for srcPart in srcComponents.dropFirst() {
                        if let urlCandidate = srcPart.substringBefore("\"") {
                            // Akzeptiere Bilder die "ligainsider.de" enthalten und player/team Pfad haben
                            if urlCandidate.contains("ligainsider.de")
                                && urlCandidate.contains("/player/team/")
                            {
                                allImageUrls.append(urlCandidate)
                            }
                        }
                    }

                    // Suche: <a ... href="/id/" ... >Name</a>
                    let linkComponents = colHtml.splitBy("<a ")

                    var namesInColumn: [(name: String, slug: String, imageUrl: String?)] = []
                    var seen = Set<String>()
                    var usedImageIndices = Set<Int>()  // Track which images we've already matched

                    for linkPart in linkComponents.dropFirst() {
                        // href finden
                        guard let slug = linkPart.substringBetween("href=\"/", "/\"") else {
                            continue
                        }

                        // Slug muss ID enthalten (z.B. name_12345)
                        if !slug.contains("_") { continue }
                        // Darf keine Slashes enthalten (wären Sub-Pfade wie News)
                        if slug.contains("/") { continue }

                        // Check if last char is digit (Skip workaround for .isNumber)
                        if let last = slug.last, !"0123456789".contains(last) {
                            continue
                        }

                        // name finden (zwischen > und </a>)
                        guard let rawName = linkPart.substringBetween(">", "</a>") else { continue }

                        // HTML Tags entfernen bevor getrimmt wird
                        let clearName = removeHtmlTags(rawName)
                        var name = clearName.trimmingCharacters(
                            in: CharacterSet.whitespacesAndNewlines)

                        // Fallback: Wenn Name leer ist (z.B. bei Live-Ansicht wo nur ein IMG Tag drin ist), versuche title/alt attribute zu lesen
                        if name.isEmpty {
                            if let title = rawName.substringBetween("title=\"", "\"") {
                                name = title
                            } else if let alt = rawName.substringBetween("alt=\"", "\"") {
                                name = alt
                            }
                        }

                        if name.isEmpty || name.count > 50 { continue }

                        if name.count > 1 && !seen.contains(name) {
                            seen.insert(name)

                            // Versuche das passende Bild für diesen Spieler zu finden
                            var matchedImageUrl: String?
                            var matchedIndex: Int?

                            // Suche in den verfügbaren Bildern nach einem Match basierend auf dem slug
                            // Kombiniert exaktes Matching und Fallback in einer Iteration für bessere Performance
                            var firstAvailableIndex: Int?
                            for (index, imageUrl) in allImageUrls.enumerated() {
                                // Skip if we've already used this image
                                if usedImageIndices.contains(index) { continue }

                                // Speichere den ersten verfügbaren Index als Fallback
                                if firstAvailableIndex == nil {
                                    firstAvailableIndex = index
                                }

                                let normalizedImageUrl = normalize(imageUrl)
                                let normalizedSlug = normalize(slug)

                                // Extrahiere nur den Namen-Teil des Slugs (vor dem Underscore)
                                let slugNamePart =
                                    normalizedSlug.components(separatedBy: "_").first
                                    ?? normalizedSlug

                                // Prüfe ob der Spielername im Bild-URL vorkommt (z.B. "lars-ritzka" in "lars-ritzka-pauli-25-26.jpg")
                                if normalizedImageUrl.contains(slugNamePart) {
                                    matchedImageUrl = imageUrl
                                    matchedIndex = index
                                    break
                                }
                            }

                            // Fallback: Verwende das erste verfügbare ungenutzte Bild wenn kein Name-Match gefunden wurde
                            if matchedImageUrl == nil, let fallbackIndex = firstAvailableIndex {
                                matchedImageUrl = allImageUrls[fallbackIndex]
                                matchedIndex = fallbackIndex
                            }

                            // Mark the image as used if we found one
                            if let index = matchedIndex {
                                usedImageIndices.insert(index)
                            }

                            namesInColumn.append(
                                (name: name, slug: slug, imageUrl: matchedImageUrl))
                        }
                    }

                    if let firstEntry = namesInColumn.first {
                        let mainName = firstEntry.name
                        let mainSlug = firstEntry.slug
                        let mainImageUrl = firstEntry.imageUrl
                        let alternativeName = namesInColumn.count > 1 ? namesInColumn[1].name : nil
                        currentRowPlayers.append(
                            LigainsiderPlayer(
                                name: mainName,
                                alternative: alternativeName,
                                ligainsiderId: mainSlug,
                                imageUrl: mainImageUrl
                            )
                        )
                    }

                    // Füge ALLE Spieler (Haupt + Alternativen) zur allParsedPlayers Liste hinzu
                    // WICHTIG: Für den Hauptspieler muss das 'alternative' Feld erhalten bleiben für korrekte Statusanzeige
                    // Alternativen werden als separate Spieler ohne alternative-Link gespeichert
                    for (index, playerData) in namesInColumn.enumerated() {
                        let isMainPlayer = (index == 0)
                        let alternativeField =
                            isMainPlayer && namesInColumn.count > 1 ? namesInColumn[1].name : nil

                        allParsedPlayers.append(
                            LigainsiderPlayer(
                                name: playerData.name,
                                alternative: alternativeField,  // Hauptspieler behält alternative-Link
                                ligainsiderId: playerData.slug,
                                imageUrl: playerData.imageUrl
                            )
                        )
                    }
                }

                if !currentRowPlayers.isEmpty {
                    formationRows.append(currentRowPlayers)
                }
            }
        }

        // Kader laden: Kombiniere geparste Spieler mit fetchSquad (für zusätzliche Spieler die nicht in Aufstellung sind)
        let path = URL(string: url)?.path ?? ""
        var fetchedSquad = await fetchSquad(path: path, teamName: teamName)

        // Merge allParsedPlayers mit fetchedSquad
        // Strategie: Priorisiere Spieler mit Bildern aus allParsedPlayers (Aufstellungsseite),
        // behalte aber fetchedSquad-Einträge für Spieler die nicht in der Aufstellung sind
        var squadMap: [String: LigainsiderPlayer] = [:]

        // Zuerst fetchedSquad einfügen (Basis-Daten)
        for player in fetchedSquad {
            if let id = player.ligainsiderId {
                squadMap[id] = player
            }
        }

        // Dann allParsedPlayers einfügen (überschreibt nur wenn wir bessere Daten haben)
        for player in allParsedPlayers {
            if let id = player.ligainsiderId {
                if let existingPlayer = squadMap[id] {
                    // Überschreibe nur wenn der neue Spieler ein Bild hat und der alte nicht
                    if player.imageUrl != nil && existingPlayer.imageUrl == nil {
                        squadMap[id] = player
                    }
                    // Wenn beide Bilder haben, behalte allParsedPlayers (Aufstellungsseite ist aktueller)
                    else if player.imageUrl != nil {
                        squadMap[id] = player
                    }
                } else {
                    // Neuer Spieler, füge hinzu
                    squadMap[id] = player
                }
            }
        }

        let finalSquad = Array(squadMap.values)

        return TeamDataResult(
            name: teamName, logo: teamLogo, lineup: formationRows, squad: finalSquad)
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
                result += String(char)
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
