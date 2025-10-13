import Foundation

// MARK: - Authentication Models
struct LoginRequest: Codable {
    let em: String      // email
    let pass: String    // password
    let loy: Bool   // loyalty (keep logged in)
    let rep: [String: String]  // empty rep object
    
    
    init(email: String, password: String, loyalty: Bool = false, rep: [String: String] = [:]) {
        self.em = email
        self.pass = password
        self.loy = loyalty
        self.rep = rep
    }
}

struct LoginResponse: Codable {
    let tkn: String     // token
    let user: User?     // Optional, da es möglicherweise nicht in der Response ist
    
    // Alternative Felder, die möglicherweise in der Response sind
    let leagues: [League]?
    let userId: String?
    
    enum CodingKeys: String, CodingKey {
        case tkn
        case user
        case leagues
        case userId
    }
}

struct User: Codable, Identifiable {
    let id: String
    let name: String
    let teamName: String
    let email: String
    let budget: Int
    let teamValue: Int
    let points: Int
    let placement: Int
    let flags: Int
    
    // Alternative gekürzte Feldnamen aus der API
    enum CodingKeys: String, CodingKey {
        case id = "i"
        case name = "n"
        case teamName = "tn"
        case email = "em"
        case budget = "b"
        case teamValue = "tv"
        case points = "p"
        case placement = "pl"
        case flags = "f"
    }
    
    // Fallback-Initialisierung für fehlende Werte
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Versuche zuerst die verkürzten Namen, dann die langen
        id = try container.decodeIfPresent(String.self, forKey: .id) ?? ""
        name = try container.decodeIfPresent(String.self, forKey: .name) ?? ""
        teamName = try container.decodeIfPresent(String.self, forKey: .teamName) ?? ""
        email = try container.decodeIfPresent(String.self, forKey: .email) ?? ""
        budget = try container.decodeIfPresent(Int.self, forKey: .budget) ?? 0
        teamValue = try container.decodeIfPresent(Int.self, forKey: .teamValue) ?? 0
        points = try container.decodeIfPresent(Int.self, forKey: .points) ?? 0
        placement = try container.decodeIfPresent(Int.self, forKey: .placement) ?? 0
        flags = try container.decodeIfPresent(Int.self, forKey: .flags) ?? 0
    }
    
    // Standard-Initialisierung für manuelle Erstellung
    init(id: String, name: String, teamName: String, email: String, budget: Int, teamValue: Int, points: Int, placement: Int, flags: Int) {
        self.id = id
        self.name = name
        self.teamName = teamName
        self.email = email
        self.budget = budget
        self.teamValue = teamValue
        self.points = points
        self.placement = placement
        self.flags = flags
    }
}

// MARK: - League Models
struct League: Codable, Identifiable, Hashable, Equatable {
    let id: String
    let name: String
    let creatorName: String
    let adminName: String
    let created: String
    let season: String
    let matchDay: Int
    let currentUser: LeagueUser
    
    // Hashable conformance
    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
    
    // Equatable conformance
    static func == (lhs: League, rhs: League) -> Bool {
        return lhs.id == rhs.id
    }
}

struct LeagueUser: Codable, Hashable {
    let id: String
    let name: String
    let teamName: String
    let budget: Int
    let teamValue: Int
    let points: Int
    let placement: Int
    let won: Int
    let drawn: Int
    let lost: Int
    let se11: Int
    let ttm: Int
}

// MARK: - Player Models
struct Player: Codable, Identifiable {
    let id: String
    let firstName: String
    let lastName: String
    let profileBigUrl: String
    let teamName: String
    let teamId: String
    let position: Int
    let number: Int
    let averagePoints: Double
    let totalPoints: Int
    let marketValue: Int
    let marketValueTrend: Int
    let tfhmvt: Int  // Marktwertänderung seit letztem Update
    let prlo: Int    // Profit/Loss since purchase - Gewinn/Verlust seit Kauf
    let stl: Int     // Neues API-Feld
    let status: Int
    let userOwnsPlayer: Bool
    
    var fullName: String {
        return "\(firstName) \(lastName)"
    }
    
    // Neue computed property für den vollständigen Teamnamen basierend auf teamId
    var fullTeamName: String {
        return TeamMapping.getTeamName(for: teamId) ?? teamName
    }
    
    var positionName: String {
        switch position {
        case 1: return "TW"
        case 2: return "ABW"
        case 3: return "MF"
        case 4: return "ST"
        default: return "?"
        }
    }
    
    var positionColor: String {
        switch position {
        case 1: return "yellow"  // TW
        case 2: return "green"   // ABW
        case 3: return "blue"    // MF
        case 4: return "red"     // ST
        default: return "gray"
        }
    }
}

// Team Player is the same as Player but used specifically for team context
typealias TeamPlayer = Player

// MARK: - Market Models
struct MarketPlayer: Codable, Identifiable, Equatable {
    let id: String
    let firstName: String
    let lastName: String
    let profileBigUrl: String
    let teamName: String
    let teamId: String
    let position: Int
    let number: Int
    let averagePoints: Double
    let totalPoints: Int
    let marketValue: Int
    let marketValueTrend: Int
    let price: Int
    let expiry: String
    let offers: Int
    let seller: MarketSeller
    let stl: Int     // Verletzungsstatus aus API-Daten
    let status: Int  // Status-Feld für Verletzung/Angeschlagen
    let prlo: Int?   // Profit/Loss since purchase - Gewinn/Verlust seit Kauf
    let owner: PlayerOwner? // Optional owner field
    let exs: Int     // Ablaufdatum als Timestamp für Sortierung
    
    var fullName: String {
        return "\(firstName) \(lastName)"
    }
    
    // Neue computed property für den vollständigen Teamnamen basierend auf teamId
    var fullTeamName: String {
        return TeamMapping.getTeamName(for: teamId) ?? teamName
    }
    
    var positionName: String {
        switch position {
        case 1: return "TW"
        case 2: return "ABW"
        case 3: return "MF"
        case 4: return "ST"
        default: return "?"
        }
    }
    
    var positionColor: String {
        switch position {
        case 1: return "blue"      // Torwart
        case 2: return "green"     // Abwehr
        case 3: return "orange"    // Mittelfeld
        case 4: return "red"       // Sturm
        default: return "gray"
        }
    }
    
    // Equatable conformance
    static func == (lhs: MarketPlayer, rhs: MarketPlayer) -> Bool {
        return lhs.id == rhs.id
    }
}

struct MarketSeller: Codable {
    let id: String
    let name: String
}

// MARK: - Player Owner (für das "u" Feld in Marktspielern)
struct PlayerOwner: Codable {
    let i: String       // ID des Besitzers
    let n: String       // Name des Besitzers
    let uim: String?    // Benutzer-Image URL
    let isvf: Bool?     // Is verified flag
    let st: Int?        // Status
    
    var id: String { i }
    var name: String { n }
    var userImageUrl: String? { uim }
    var isVerified: Bool { isvf ?? false }
    var status: Int { st ?? 0 }
}

// MARK: - Response Wrappers
struct PlayersResponse: Codable {
    let players: [Player]
}

struct MarketResponse: Codable {
    let players: [MarketPlayer]
}

struct LeaguesResponse: Codable {
    let leagues: [League]
}

// MARK: - Performance Models
struct PlayerPerformanceResponse: Codable {
    let it: [SeasonPerformance]
}

struct SeasonPerformance: Codable, Identifiable {
    let ti: String    // Saison (z.B. "2024/2025")
    let n: String     // Liga Name (z.B. "Bundesliga")
    let ph: [MatchPerformance]  // Performance History
    
    var id: String { ti }
    var title: String { ti }
    var leagueName: String { n }
    var performances: [MatchPerformance] { ph }
}

struct MatchPerformance: Codable, Identifiable {
    let day: Int              // Spieltag
    let p: Int?               // Punkte (optional, nicht bei zukünftigen Spielen)
    let mp: String?           // Spielminuten (z.B. "96'", optional)
    let md: String            // Match Date (ISO String)
    let t1: String            // Team 1 ID
    let t2: String            // Team 2 ID
    let t1g: Int?             // Team 1 Goals (optional)
    let t2g: Int?             // Team 2 Goals (optional)
    let pt: String?            // Player Team ID
    let k: [Int]?             // Kicker Bewertungen (optional)
    let st: Int               // Status (0=nicht gespielt, 1=?, 3=eingewechselt, 4=nicht im Kader, 5=startelf)
    let cur: Bool             // Current (aktueller Spieltag?)
    let mdst: Int             // Match Day Status
    let ap: Int               // Average Points (Durchschnittspunkte)
    let tp: Int               // Total Points (Gesamtpunkte)
    let asp: Int              // Average Season Points
    //let t1im: String          // Team 1 Image
    //let t2im: String          // Team 2 Image
    
    var id: String { "\(day)-\(md)" }
    var matchDay: Int { day }
    var points: Int { p ?? 0 }
    var minutesPlayed: String { mp ?? "0'" }
    var matchDate: String { md }
    var team1Id: String { t1 }
    var team2Id: String { t2 }
    var team1Goals: Int { t1g ?? 0 }
    var team2Goals: Int { t2g ?? 0 }
    var playerTeamId: String { pt ?? "" }
    var kickerRatings: [Int] { k ?? [] }
    var status: Int { st }
    var isCurrent: Bool { cur }
    var matchDayStatus: Int { mdst }
    var averagePoints: Int { ap }
    var totalPoints: Int { tp }
    var averageSeasonPoints: Int { asp }
    //var team1Image: String { t1im }
    //var team2Image: String { t2im }
    
    // Computed properties
    var hasPlayed: Bool { p != nil && st > 1 }
    var wasStartingEleven: Bool { st == 5 }
    var wasSubstitute: Bool { st == 3 }
    var wasNotInSquad: Bool { st == 4 }
    var didNotPlay: Bool { st <= 1 }
    
    var statusText: String {
        switch st {
        case 0: return "Nicht gespielt"
        case 1: return "Verletzt/Gesperrt"
        case 3: return "Eingewechselt"
        case 4: return "Nicht im Kader"
        case 5: return "Startelf"
        default: return "Unbekannt"
        }
    }
    
    var parsedMatchDate: Date {
        let formatter = ISO8601DateFormatter()
        return formatter.date(from: md) ?? Date()
    }
    
    var opponentTeamId: String {
        // Fallback: Wenn pt (playerTeamId) leer ist, verwende eine andere Logik
        guard !playerTeamId.isEmpty else {
            // Wenn keine Player Team ID verfügbar ist, nimm einfach team2 als Gegner
            return t2
        }
        return playerTeamId == t1 ? t2 : t1
    }
    
    var opponentTeamName: String {
        return TeamMapping.getTeamName(for: opponentTeamId) ?? "Unbekannt"
    }
    
    var isHomeMatch: Bool {
        // Fallback: Wenn pt (playerTeamId) leer ist, nehme an dass es ein Auswärtsspiel ist
        guard !playerTeamId.isEmpty else {
            return false  // Standardmäßig Auswärtsspiel wenn unbekannt
        }
        return playerTeamId == t1
    }
    
    var result: String {
        guard let t1g = t1g, let t2g = t2g else { return "-:-" }
        return "\(t1g):\(t2g)"
    }
    
    // Neue Methoden, die den Spieler-Kontext verwenden
    func getOpponentTeamId(playerTeamId: String) -> String {
        return playerTeamId == t1 ? t2 : t1
    }
    
    func getIsHomeMatch(playerTeamId: String) -> Bool {
        return playerTeamId == t1
    }
}

// MARK: - Team Profile Models
struct TeamProfileResponse: Codable {
    let tid: String     // Team ID
    let tn: String      // Team Name
    let pl: Int         // Placement (Platzierung)
    let tv: Int         // Team Value
    let tw: Int         // Team Wins
    let td: Int         // Team Draws
    let tl: Int         // Team Losses
    //let it: [TeamPlayer]? // Team Players (optional)
    let npt: Int        // Next Point Total
    let avpcl: Bool     // Available Players Close
}

struct TeamInfo: Codable, Identifiable {
    let id: String      // Team ID
    let name: String    // Team Name
    let placement: Int  // Platzierung
    
    init(from response: TeamProfileResponse) {
        self.id = response.tid
        self.name = response.tn
        self.placement = response.pl
    }
}

// MARK: - Enhanced Performance Models with Team Info
struct EnhancedMatchPerformance: Identifiable {
    let basePerformance: MatchPerformance
    let team1Info: TeamInfo?
    let team2Info: TeamInfo?
    let playerTeamInfo: TeamInfo?
    let opponentTeamInfo: TeamInfo?
    
    // Delegiere alle Eigenschaften an basePerformance
    var id: String { basePerformance.id }
    var matchDay: Int { basePerformance.matchDay }
    var points: Int { basePerformance.points }
    var minutesPlayed: String { basePerformance.minutesPlayed }
    var matchDate: String { basePerformance.matchDate }
    var team1Id: String { basePerformance.team1Id }
    var team2Id: String { basePerformance.team2Id }
    var team1Goals: Int { basePerformance.team1Goals }
    var team2Goals: Int { basePerformance.team2Goals }
    var playerTeamId: String { basePerformance.playerTeamId }
    var kickerRatings: [Int] { basePerformance.kickerRatings }
    var status: Int { basePerformance.status }
    var isCurrent: Bool { basePerformance.isCurrent }
    var matchDayStatus: Int { basePerformance.matchDayStatus }
    var averagePoints: Int { basePerformance.averagePoints }
    var totalPoints: Int { basePerformance.totalPoints }
    var averageSeasonPoints: Int { basePerformance.averageSeasonPoints }
    //var team1Image: String { basePerformance.team1Image }
    //var team2Image: String { basePerformance.team2Image }
    var hasPlayed: Bool { basePerformance.hasPlayed }
    var wasStartingEleven: Bool { basePerformance.wasStartingEleven }
    var wasSubstitute: Bool { basePerformance.wasSubstitute }
    var wasNotInSquad: Bool { basePerformance.wasNotInSquad }
    var didNotPlay: Bool { basePerformance.didNotPlay }
    var statusText: String { basePerformance.statusText }
    var parsedMatchDate: Date { basePerformance.parsedMatchDate }
    var opponentTeamId: String { basePerformance.opponentTeamId }
    var isHomeMatch: Bool { basePerformance.isHomeMatch }
    var result: String { basePerformance.result }
    
    // Erweiterte computed properties mit Team-Informationen
    var team1Name: String {
        return team1Info?.name ?? TeamMapping.getTeamName(for: team1Id) ?? "Unbekannt"
    }
    
    var team2Name: String {
        return team2Info?.name ?? TeamMapping.getTeamName(for: team2Id) ?? "Unbekannt"
    }
    
    var playerTeamName: String {
        return playerTeamInfo?.name ?? TeamMapping.getTeamName(for: playerTeamId) ?? "Unbekannt"
    }
    
    var opponentTeamName: String {
        return opponentTeamInfo?.name ?? TeamMapping.getTeamName(for: opponentTeamId) ?? "Unbekannt"
    }
    
    var team1Placement: Int? {
        return team1Info?.placement
    }
    
    var team2Placement: Int? {
        return team2Info?.placement
    }
    
    var playerTeamPlacement: Int? {
        return playerTeamInfo?.placement
    }
    
    var opponentTeamPlacement: Int? {
        return opponentTeamInfo?.placement
    }
    
    var matchDescription: String {
        let homeTeam = team1Name
        let awayTeam = team2Name
        let homeGoals = team1Goals
        let awayGoals = team2Goals
        
        if hasPlayed {
            return "\(homeTeam) \(homeGoals):\(awayGoals) \(awayTeam)"
        } else {
            return "\(homeTeam) vs \(awayTeam)"
        }
    }
}

// MARK: - Stats Models
struct TeamStats: Codable {
    let teamValue: Int
    let teamValueTrend: Int
    let budget: Int
    let points: Int
    let placement: Int
    let won: Int
    let drawn: Int
    let lost: Int
}

struct UserStats: Codable {
    let teamValue: Int
    let teamValueTrend: Int
    let budget: Int
    let points: Int
    let placement: Int
    let won: Int
    let drawn: Int
    let lost: Int
}

// MARK: - Team Mapping
struct TeamMapping {
    // Wird durch Auto-Discovery gefüllt - siehe discoverAndMapTeams() in KickbaseManager
    static var teamIdToName: [String: String] = [:]
    
    static var teamNameToId: [String: String] = {
        var reversed: [String: String] = [:]
        for (id, name) in teamIdToName {
            reversed[name] = id
        }
        return reversed
    }()
    
    static func getTeamName(for id: String) -> String? {
        return teamIdToName[id]
    }
    
    static func getTeamId(for name: String) -> String? {
        return teamNameToId[name]
    }
    
    static func getAllTeams() -> [String: String] {
        return teamIdToName
    }
    
    // Funktion zum Aktualisieren des Mappings durch Auto-Discovery
    static func updateMapping(with discoveredTeams: [String: String]) {
        teamIdToName = discoveredTeams
        // Aktualisiere auch das umgekehrte Mapping
        var reversed: [String: String] = [:]
        for (id, name) in teamIdToName {
            reversed[name] = id
        }
        teamNameToId = reversed
    }
}

// MARK: - Player Detail Models
struct PlayerDetailResponse: Codable {
    let fn: String?     // First Name
    let ln: String?     // Last Name
    let tn: String?     // Team Name
    let shn: Int?       // Shirt Number (Trikotnummer)
    let id: String?
    let position: Int?
    let number: Int?
    let averagePoints: Double?
    let totalPoints: Int?
    let marketValue: Int?
    let marketValueTrend: Int?
    let profileBigUrl: String?
    let teamId: String?
    let tfhmvt: Int?
    let prlo: Int?      // Profit/Loss since purchase - Gewinn/Verlust seit Kauf
    let stl: Int?
    let status: Int?
    let userOwnsPlayer: Bool?
}

// MARK: - Market Value History Models
struct MarketValueHistoryResponse: Codable {
    let it: [MarketValueEntry]  // Liste der Marktwert-Einträge
    let prlo: Int?              // Profit/Loss since purchase - auf Root-Ebene, nicht in den Einträgen
}

struct MarketValueEntry: Codable {
    let dt: Int    // Datum als Unix-Timestamp (Tage seit 1.1.1970)
    let mv: Int    // Marktwert am entsprechenden Tag
    // prlo ist NICHT hier, sondern auf Root-Ebene in MarketValueHistoryResponse
}

// MARK: - Market Value Change Data
struct DailyMarketValueChange {
    let date: String
    let value: Int
    let change: Int
    let percentageChange: Double
    let daysAgo: Int
    
    var isPositive: Bool {
        return change > 0
    }
    
    var isNegative: Bool {
        return change < 0
    }
}

struct MarketValueChange {
    let daysSinceLastUpdate: Int
    let absoluteChange: Int
    let percentageChange: Double
    let previousValue: Int
    let currentValue: Int
    let dailyChanges: [DailyMarketValueChange]
    
    var isPositive: Bool {
        return absoluteChange > 0
    }
    
    var isNegative: Bool {
        return absoluteChange < 0
    }
}
