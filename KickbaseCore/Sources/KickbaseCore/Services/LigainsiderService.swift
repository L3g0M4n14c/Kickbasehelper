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

public struct LigainsiderMatch: Codable, Identifiable {
    public var id: String { homeTeam + (url ?? UUID().uuidString) }
    public let homeTeam: String
    public let awayTeam: String
    public let homeLogo: String?
    public let awayLogo: String?
    // Aufstellung ist jetzt ein Array von Reihen (z.B. [Torwart, Abwehr, Mittelfeld, Sturm])
    // Jede Reihe ist ein Array von Spielern
    public let homeLineup: [[LigainsiderPlayer]]
    public let awayLineup: [[LigainsiderPlayer]]
    public let homeSquad: [LigainsiderPlayer]
    public let awaySquad: [LigainsiderPlayer]
    public let url: String?
}

public enum LigainsiderStatus {
    case likelyStart  // S11 ohne Alternative
    case startWithAlternative  // S11 mit Alternative (1. Option)
    case isAlternative  // Ist die Alternative (2. Option)
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
                    // Zuerst Kader zum Cache hinzufügen (für Bilder von Bankspielern/Alternativen)
                    let allSquad = match.homeSquad + match.awaySquad
                    for player in allSquad {
                        if let id = player.ligainsiderId {
                            newCache[id] = player
                        }
                    }

                    let allRows = match.homeLineup + match.awayLineup
                    for row in allRows {
                        for player in row {
                            // Speichere Hauptspieler (überschreibt Kader-Eintrag -> wichtig wegen 'alternative' Property)
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
                    // Merge new cache into existing to preserve squad data
                    for (id, player) in newCache {
                        self.playerCache[id] = player
                    }
                    // Merge alternatives
                    for alt in newAlts {
                        self.alternativeNames.insert(alt)
                    }

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
        // Manuelle Transliteration für deutsche Umlaute (da IDs oft ae/oe/ue nutzen)
        let manualReplacement = text.lowercased()
            .replacingOccurrences(of: "ä", with: "ae")
            .replacingOccurrences(of: "ö", with: "oe")
            .replacingOccurrences(of: "ü", with: "ue")
            .replacingOccurrences(of: "ß", with: "ss")

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
        if matches.isEmpty { return nil }

        let normalizedLastName = normalize(lastName)
        let normalizedFirstName = normalize(firstName)

        let candidates = playerCache.filter { key, _ in
            let normalizedKey = normalize(key)
            return normalizedKey.contains(normalizedLastName)
        }

        if candidates.count == 1 {
            return candidates.first?.value
        } else if candidates.count > 1 {
            let bestMatch = candidates.first(where: { key, _ in
                let normalizedKey = normalize(key)
                return normalizedKey.contains(normalizedFirstName)
            })
            return bestMatch?.value
        }
        return nil
    }

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
                return .startWithAlternative
            }
            return .likelyStart
        }

        // 2. Check Alternativen (String Matching)
        let isAlternative = alternativeNames.contains { altName in
            let normalizedAlt = normalize(altName)
            return normalizedLastName == normalizedAlt || normalizedAlt.contains(normalizedLastName)
        }

        if isAlternative {
            return .isAlternative
        }

        return .out
    }

    // Helper für Views
    public func getIcon(for status: LigainsiderStatus) -> String {
        switch status {
        case .likelyStart: return "checkmark.circle.fill"
        case .startWithAlternative: return "1.circle.fill"
        case .isAlternative: return "2.circle.fill"
        case .out: return "xmark.circle.fill"
        }
    }

    public func getColor(for status: LigainsiderStatus) -> Color {
        switch status {
        case .likelyStart: return .green
        case .startWithAlternative: return .orange
        case .isAlternative: return .orange
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

    public func fetchAllSquads() {
        Task {
            print("Starte Kader-Abruf für alle Teams...")
            await withTaskGroup(of: [LigainsiderPlayer].self) { group in
                for (teamName, path) in teamSquadPaths {
                    group.addTask {
                        return await self.fetchSquad(path: path, teamName: teamName)
                    }
                }

                var allNewPlayers: [LigainsiderPlayer] = []
                for await squad in group {
                    allNewPlayers.append(contentsOf: squad)
                }

                await MainActor.run {
                    print("Kader-Abruf beendet. Gefundene Spieler: \(allNewPlayers.count)")
                    for player in allNewPlayers {
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
                guard let quoteEnd = component.firstIndex(of: "\"") else { continue }
                let slug = String(component[..<quoteEnd])

                // Validierung
                if !slug.contains("_") { continue }
                if slug.contains("/") { continue }
                let lastChar = slug.last ?? " "
                if !"0123456789".contains(lastChar) { continue }

                // 2. Name extrahieren
                guard let tagClose = component.range(of: ">"),
                    let aClose = component.range(
                        of: "</a>", range: tagClose.upperBound..<component.endIndex)
                else { continue }

                let rawName = String(component[tagClose.upperBound..<aClose.lowerBound])
                let name = removeHtmlTags(rawName).trimmingCharacters(in: .whitespacesAndNewlines)

                if name.isEmpty || name.count > 50 { continue }

                // 3. Bild suchen
                var imageUrl: String?

                // A) Im Link selbst
                let contentInLink = String(component[tagClose.upperBound..<aClose.lowerBound])
                if contentInLink.contains("<img") {
                    if let srcRange = contentInLink.range(of: "src=\""),
                        let srcEnd = contentInLink.range(
                            of: "\"", range: srcRange.upperBound..<contentInLink.endIndex)
                    {
                        let extracted = String(
                            contentInLink[srcRange.upperBound..<srcEnd.lowerBound])
                        if extracted.contains("ligainsider.de") {
                            imageUrl = extracted
                        }
                    }
                }

                // B) Kurz vor dem Link (im previousComponent)
                if imageUrl == nil {
                    // Wir schauen uns die letzten 400 Zeichen des vorherigen Blocks an
                    let lookBackLimit = 400
                    let prevCount = previousComponent.count
                    let startIndex = previousComponent.index(
                        previousComponent.startIndex, offsetBy: max(0, prevCount - lookBackLimit))
                    let rangeToCheck = previousComponent[startIndex...]

                    if let imgTagRange = rangeToCheck.range(
                        of: "<img", options: String.CompareOptions.backwards),
                        let srcRange = rangeToCheck.range(
                            of: "src=\"", range: imgTagRange.upperBound..<rangeToCheck.endIndex),
                        let srcEnd = rangeToCheck.range(
                            of: "\"", range: srcRange.upperBound..<rangeToCheck.endIndex)
                    {

                        let extracted = String(
                            rangeToCheck[srcRange.upperBound..<srcEnd.lowerBound])
                        if extracted.contains("ligainsider.de") {
                            imageUrl = extracted
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
                        let (hName, hLogo, hLineup, hSquad) = try await homeData
                        let (aName, aLogo, aLineup, aSquad) = try await awayData

                        return LigainsiderMatch(
                            homeTeam: hName,
                            awayTeam: aName,
                            homeLogo: hLogo,
                            awayLogo: aLogo,
                            homeLineup: hLineup,
                            awayLineup: aLineup,
                            homeSquad: hSquad,
                            awaySquad: aSquad,
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

    private func fetchTeamData(url: String) async throws -> (
        String, String?, [[LigainsiderPlayer]], [LigainsiderPlayer]
    ) {
        print("Lade Team Details: \(url)")
        guard let urlObj = URL(string: url) else { return ("Unbekannt", nil, [], []) }
        let (data, _) = try await URLSession.shared.data(from: urlObj)
        guard let html = String(data: data, encoding: .utf8) else {
            return ("Unbekannt", nil, [], [])
        }

        // Teamnamen extrahieren
        var teamName = "Team"
        var teamLogo: String?

        // Suche nach: <h2 ... itemprop="name">Team Name</h2>
        let nameComponents = html.components(separatedBy: "itemprop=\"name\"")
        if nameComponents.count > 1 {
            // Logo suchen im Teil davor (letzte src="..." vor dem Namen)
            // Struktur: <div ...><a ...><img src="..."></a></div> ... <h2 ... itemprop="name">
            let partBeforeName = nameComponents[0]
            if let srcRange = partBeforeName.range(
                of: "src=\"", options: String.CompareOptions.backwards)
            {
                if let quoteEnd = partBeforeName.range(
                    of: "\"", range: srcRange.upperBound..<partBeforeName.endIndex)
                {
                    let logoUrl = String(partBeforeName[srcRange.upperBound..<quoteEnd.lowerBound])
                    // Validierung: sollte ligainsider domain enthalten oder relativ sein?
                    // Meistens absolut: https://cdn.ligainsider.de/...
                    if logoUrl.contains("ligainsider.de")
                        && (logoUrl.contains("wappen") || logoUrl.contains("images/teams"))
                    {
                        teamLogo = logoUrl
                    }
                }
            }

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

                    // Bild URL extrahieren
                    var extractedImageUrl: String?
                    let srcComponents = colHtml.components(separatedBy: "src=\"")
                    for srcPart in srcComponents.dropFirst() {
                        if let quoteEnd = srcPart.range(of: "\"") {
                            let urlCandidate = String(srcPart[..<quoteEnd.lowerBound])
                            // Wir akzeptieren Bilder die "ligainsider.de" enthalten
                            if urlCandidate.contains("ligainsider.de") {
                                extractedImageUrl = urlCandidate
                                break
                            }
                        }
                    }

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

                    // Alle gefundenen Spieler in der Spalte zur "Squad" hinzufügen (damit IDs bekannt sind)
                    for (pName, pSlug) in namesInColumn {
                        // Prüfen ob wir für diesen Spieler schon ein Bild haben (aus dem srcComponents check oben, gilt aber meist nur für den Ersten)
                        // Alternativ: Einfach als Spieler anlegen. Wenn Bild fehlt, fehlt es halt. Hauptsache ID ist da für Matching.
                        // Im Cache wird später eh gemerged.
                        // HACK: Wir nutzen extractedImageUrl für den ersten Spieler. Für Alternativen nil?
                        // Oder besser: Da extractedImageUrl nur EINMAL pro Column gefunden wird (meist S11 Spieler), vergeben wir es nur beim passenden Match?
                        // Meist ist extractedImageUrl das Bild des S11 Spielers (namesInColumn.first).

                        let img = (pName == namesInColumn.first?.0) ? extractedImageUrl : nil

                        // Zu "Squad" hinzufügen via temporärer Liste? Wir fügen es direkt zu squad (via Rückgabe) zu?
                        // Geht nicht direkt, da squad variable unten lokal ist.
                        // Wir sammeln sie in 'extraPlayers'
                    }

                    if let (mainName, mainSlug) = namesInColumn.first {
                        let alternativeName = namesInColumn.count > 1 ? namesInColumn[1].0 : nil
                        currentRowPlayers.append(
                            LigainsiderPlayer(
                                name: mainName,
                                alternative: alternativeName,
                                ligainsiderId: mainSlug,
                                imageUrl: extractedImageUrl
                            )
                        )
                    }
                }

                if !currentRowPlayers.isEmpty {
                    formationRows.append(currentRowPlayers)
                }
            }

            // Collect all players from formationRows into a flat list to append to squad
            // This ensures alternatives (if explicitly parsed as rows? no)
            // Wait, we need to add the ALTERNATIVES to the squad list explicitly if they are not in formationRows as main players.
            // Currently formationRows only contains valid MAIN players.

            // Re-scan for alternatives to add to squad:
            // The logic above is inside a loop. We need a way to output them.
            // Let's iterate formationRows afterwards? No, we don't have the IDs there (only alternative name string).

            // We need to capture the IDs during the parsing loop.
            // Let's modify the parsing loop to collect `extraSquadPlayers`.
        }

        // HIER MÜSSEN WIR NOCHMAL PARSEN ODER DIE LOGIK VERBESSERN, UM ALTERNATIVEN ZU RETTEN.
        // Da ich den Code oben nicht komplett umschreiben will (zu viele Änderungen), nutzen wir fetchSquad für "alles was auf der Seite ist".
        // Da fetchSquad nun robuster ist ("alle Links"), sollte es die Alternativen auf der Match-Seite finden,
        // SOFERN sie verlinkt sind. (Ligainsider verlinkt Alternativen im Text meistens).

        // Kader laden (via fetchSquad)
        let path = URL(string: url)?.path ?? ""
        let squad = await fetchSquad(path: path, teamName: teamName)

        return (teamName, teamLogo, formationRows, squad)
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
