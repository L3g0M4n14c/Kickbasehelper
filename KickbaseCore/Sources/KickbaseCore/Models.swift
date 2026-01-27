import Foundation
import SwiftUI

// MARK: - Authentication Models
public struct LoginRequest: Codable {
    public let em: String  // email
    public let pass: String  // password
    public let loy: Bool  // loyalty (keep logged in)
    public let rep: [String: String]  // empty rep object

    public init(email: String, password: String, loyalty: Bool = false, rep: [String: String] = [:])
    {
        self.em = email
        self.pass = password
        self.loy = loyalty
        self.rep = rep
    }
}

public struct LoginResponse: Codable {
    public let tkn: String  // token
    public let user: User?  // Optional, da es möglicherweise nicht in der Response ist

    // Alternative Felder, die möglicherweise in der Response sind
    public let leagues: [League]?
    public let userId: String?

    public init(tkn: String, user: User?, leagues: [League]?, userId: String?) {
        self.tkn = tkn
        self.user = user
        self.leagues = leagues
        self.userId = userId
    }

    enum CodingKeys: String, CodingKey {
        case tkn
        case user
        case leagues
        case userId
    }
}

public struct User: Codable, Identifiable {
    public let id: String
    public let name: String
    public let teamName: String
    public let email: String
    public let budget: Int
    public let teamValue: Int
    public let points: Int
    public let placement: Int
    public let flags: Int

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
    public init(from decoder: Decoder) throws {
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
    public init(
        id: String, name: String, teamName: String, email: String, budget: Int, teamValue: Int,
        points: Int, placement: Int, flags: Int
    ) {
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
public struct League: Codable, Identifiable, Hashable, Equatable {
    public let id: String
    public let competitionId: String
    public let name: String
    public let creatorName: String
    public let adminName: String
    public let created: String
    public let season: String
    public let matchDay: Int
    public let currentUser: LeagueUser

    enum CodingKeys: String, CodingKey {
        case id = "i"
        case competitionId = "cpi"
        case name = "n"
        case creatorName = "cn"
        case adminName = "an"
        case created = "c"
        case season = "s"
        case matchDay = "md"
        case currentUser = "cu"
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        // Default to "1" (Bundesliga) if missing
        competitionId = try container.decodeIfPresent(String.self, forKey: .competitionId) ?? "1"
        name = try container.decode(String.self, forKey: .name)
        creatorName = try container.decode(String.self, forKey: .creatorName)
        adminName = try container.decode(String.self, forKey: .adminName)
        created = try container.decode(String.self, forKey: .created)
        season = try container.decode(String.self, forKey: .season)
        matchDay = try container.decode(Int.self, forKey: .matchDay)
        currentUser = try container.decode(LeagueUser.self, forKey: .currentUser)
    }

    // Hashable conformance
    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }

    // Equatable conformance
    public static func == (lhs: League, rhs: League) -> Bool {
        return lhs.id == rhs.id
    }

    public init(
        id: String, competitionId: String = "1", name: String, creatorName: String,
        adminName: String, created: String,
        season: String, matchDay: Int, currentUser: LeagueUser
    ) {
        self.id = id
        self.competitionId = competitionId
        self.name = name
        self.creatorName = creatorName
        self.adminName = adminName
        self.created = created
        self.season = season
        self.matchDay = matchDay
        self.currentUser = currentUser
    }
}

public struct LeagueUser: Codable, Hashable {
    public let id: String
    public let name: String
    public let teamName: String
    public let budget: Int
    public let teamValue: Int
    public let points: Int
    public let placement: Int
    public let won: Int
    public let drawn: Int
    public let lost: Int
    public let se11: Int
    public let ttm: Int
    public let mpst: Int?  // Max Players Same Team - Maximale Anzahl Spieler vom gleichen Team
    public let lineupPlayerIds: [String]  // "lp" - Player IDs of the lineup

    public init(
        id: String, name: String, teamName: String, budget: Int, teamValue: Int, points: Int,
        placement: Int, won: Int, drawn: Int, lost: Int, se11: Int, ttm: Int, mpst: Int?,
        lineupPlayerIds: [String] = []
    ) {
        self.id = id
        self.name = name
        self.teamName = teamName
        self.budget = budget
        self.teamValue = teamValue
        self.points = points
        self.placement = placement
        self.won = won
        self.drawn = drawn
        self.lost = lost
        self.se11 = se11
        self.ttm = ttm
        self.mpst = mpst
        self.lineupPlayerIds = lineupPlayerIds
    }
}

// MARK: - Player Models
public struct Player: Codable, Identifiable {
    public let id: String
    public let firstName: String
    public let lastName: String
    public let profileBigUrl: String
    public let teamName: String
    public let teamId: String
    public let position: Int
    public let number: Int
    public let averagePoints: Double
    public let totalPoints: Int
    public let marketValue: Int
    public let marketValueTrend: Int
    public let tfhmvt: Int  // Marktwertänderung seit letztem Update
    public let prlo: Int  // Profit/Loss since purchase - Gewinn/Verlust seit Kauf
    public let stl: Int  // Neues API-Feld
    public let status: Int
    public let userOwnsPlayer: Bool

    public var fullName: String {
        return "\(firstName) \(lastName)"
    }

    public var imageUrl: URL? {
        if profileBigUrl.hasPrefix("http") {
            return URL(string: profileBigUrl)
        } else if !profileBigUrl.isEmpty {
            // Remove leading slash if present to avoid double slashes
            let path =
                profileBigUrl.hasPrefix("/") ? String(profileBigUrl.dropFirst()) : profileBigUrl
            return URL(string: "https://kickbase.b-cdn.net/" + path)
        }
        return nil
    }

    // Neue computed property für den vollständigen Teamnamen basierend auf teamId
    public var fullTeamName: String {
        return TeamMapping.getTeamName(for: teamId) ?? teamName
    }

    public var positionName: String {
        switch position {
        case 1: return "TW"
        case 2: return "ABW"
        case 3: return "MF"
        case 4: return "ST"
        default: return "?"
        }
    }

    public var positionColor: Color {
        switch position {
        case 1: return .yellow  // TW
        case 2: return .green  // ABW
        case 3: return .blue  // MF
        case 4: return .red  // ST
        default: return .gray
        }
    }

    public init(
        id: String, firstName: String, lastName: String, profileBigUrl: String, teamName: String,
        teamId: String, position: Int, number: Int, averagePoints: Double, totalPoints: Int,
        marketValue: Int, marketValueTrend: Int, tfhmvt: Int, prlo: Int, stl: Int, status: Int,
        userOwnsPlayer: Bool
    ) {
        self.id = id
        self.firstName = firstName
        self.lastName = lastName
        self.profileBigUrl = profileBigUrl
        self.teamName = teamName
        self.teamId = teamId
        self.position = position
        self.number = number
        self.averagePoints = averagePoints
        self.totalPoints = totalPoints
        self.marketValue = marketValue
        self.marketValueTrend = marketValueTrend
        self.tfhmvt = tfhmvt
        self.prlo = prlo
        self.stl = stl
        self.status = status
        self.userOwnsPlayer = userOwnsPlayer
    }
}

// Team Player is the same as Player but used specifically for team context
public typealias TeamPlayer = Player

// MARK: - Market Models
public struct MarketPlayer: Codable, Identifiable, Equatable {
    public let id: String
    public let firstName: String
    public let lastName: String
    public let profileBigUrl: String
    public let teamName: String
    public let teamId: String
    public let position: Int
    public let number: Int
    public let averagePoints: Double
    public let totalPoints: Int
    public let marketValue: Int
    public let marketValueTrend: Int
    public let price: Int
    public let expiry: String
    public let offers: Int
    public let seller: MarketSeller
    public let stl: Int  // Verletzungsstatus aus API-Daten
    public let status: Int  // Status-Feld für Verletzung/Angeschlagen
    public let prlo: Int?  // Profit/Loss since purchase - Gewinn/Verlust seit Kauf
    public let owner: PlayerOwner?  // Optional owner field
    public let exs: Int  // Ablaufdatum als Timestamp für Sortierung

    public var fullName: String {
        return "\(firstName) \(lastName)"
    }

    public var imageUrl: URL? {
        if profileBigUrl.hasPrefix("http") {
            return URL(string: profileBigUrl)
        } else if !profileBigUrl.isEmpty {
            // Remove leading slash if present to avoid double slashes
            let path =
                profileBigUrl.hasPrefix("/") ? String(profileBigUrl.dropFirst()) : profileBigUrl
            return URL(string: "https://kickbase.b-cdn.net/" + path)
        }
        return nil
    }

    // Neue computed property für den vollständigen Teamnamen basierend auf teamId
    public var fullTeamName: String {
        return TeamMapping.getTeamName(for: teamId) ?? teamName
    }

    public var positionName: String {
        switch position {
        case 1: return "TW"
        case 2: return "ABW"
        case 3: return "MF"
        case 4: return "ST"
        default: return "?"
        }
    }

    public var positionColor: Color {
        switch position {
        case 1: return .blue  // Torwart
        case 2: return .green  // Abwehr
        case 3: return .orange  // Mittelfeld
        case 4: return .red  // Sturm
        default: return .gray
        }
    }

    // Equatable conformance
    public static func == (lhs: MarketPlayer, rhs: MarketPlayer) -> Bool {
        return lhs.id == rhs.id
    }

    public init(
        id: String, firstName: String, lastName: String, profileBigUrl: String, teamName: String,
        teamId: String, position: Int, number: Int, averagePoints: Double, totalPoints: Int,
        marketValue: Int, marketValueTrend: Int, price: Int, expiry: String, offers: Int,
        seller: MarketSeller, stl: Int, status: Int, prlo: Int?, owner: PlayerOwner?, exs: Int
    ) {
        self.id = id
        self.firstName = firstName
        self.lastName = lastName
        self.profileBigUrl = profileBigUrl
        self.teamName = teamName
        self.teamId = teamId
        self.position = position
        self.number = number
        self.averagePoints = averagePoints
        self.totalPoints = totalPoints
        self.marketValue = marketValue
        self.marketValueTrend = marketValueTrend
        self.price = price
        self.expiry = expiry
        self.offers = offers
        self.seller = seller
        self.stl = stl
        self.status = status
        self.prlo = prlo
        self.owner = owner
        self.exs = exs
    }
}

public struct MarketSeller: Codable {
    public let id: String
    public let name: String

    public init(id: String, name: String) {
        self.id = id
        self.name = name
    }
}

// MARK: - Player Owner (für das "u" Feld in Marktspielern)
public struct PlayerOwner: Codable {
    public let i: String  // ID des Besitzers
    public let n: String  // Name des Besitzers
    public let uim: String?  // Benutzer-Image URL
    public let isvf: Bool?  // Is verified flag
    public let st: Int?  // Status

    public init(i: String, n: String, uim: String?, isvf: Bool?, st: Int?) {
        self.i = i
        self.n = n
        self.uim = uim
        self.isvf = isvf
        self.st = st
    }

    public var id: String { i }
    public var name: String { n }
    public var userImageUrl: String? { uim }
    public var isVerified: Bool { isvf ?? false }
    public var status: Int { st ?? 0 }
}

// MARK: - Response Wrappers
public struct PlayersResponse: Codable {
    public let players: [Player]
}

public struct MarketResponse: Codable {
    public let players: [MarketPlayer]
}

public struct LeaguesResponse: Codable {
    public let leagues: [League]
}

// MARK: - Performance Models
public struct PlayerPerformanceResponse: Codable {
    public let it: [SeasonPerformance]
}

public struct SeasonPerformance: Codable, Identifiable {
    public let ti: String  // Saison (z.B. "2024/2025")
    public let n: String  // Liga Name (z.B. "Bundesliga")
    public let ph: [MatchPerformance]  // Performance History

    public var id: String { ti }
    public var title: String { ti }
    public var leagueName: String { n }
    public var performances: [MatchPerformance] { ph }
}

public struct MatchPerformance: Codable, Identifiable {
    public let day: Int  // Spieltag
    public let p: Int?  // Punkte (optional, nicht bei zukünftigen Spielen)
    public let mp: String?  // Spielminuten (z.B. "96'", optional)
    public let md: String  // Match Date (ISO String)
    public let t1: String  // Team 1 ID
    public let t2: String  // Team 2 ID
    public let t1g: Int?  // Team 1 Goals (optional)
    public let t2g: Int?  // Team 2 Goals (optional)
    public let pt: String?  // Player Team ID
    public let k: [Int]?  // Kicker Bewertungen (optional)
    public let st: Int  // Status (0=nicht gespielt, 1=?, 3=eingewechselt, 4=nicht im Kader, 5=startelf)
    public let cur: Bool  // Current (aktueller Spieltag?)
    public let mdst: Int  // Match Day Status
    public let ap: Int  // Average Points (Durchschnittspunkte)
    public let tp: Int  // Total Points (Gesamtpunkte)
    public let asp: Int  // Average Season Points
    //let t1im: String          // Team 1 Image
    //let t2im: String          // Team 2 Image

    public var id: String { "\(day)-\(md)" }
    public var matchDay: Int { day }
    public var points: Int { p ?? 0 }
    public var minutesPlayed: String { mp ?? "0'" }
    public var matchDate: String { md }
    public var team1Id: String { t1 }
    public var team2Id: String { t2 }
    public var team1Goals: Int { t1g ?? 0 }
    public var team2Goals: Int { t2g ?? 0 }
    public var playerTeamId: String { pt ?? "" }
    public var kickerRatings: [Int] { k ?? [] }
    public var status: Int { st }
    public var isCurrent: Bool { cur }
    public var matchDayStatus: Int { mdst }
    public var averagePoints: Int { ap }
    public var totalPoints: Int { tp }
    public var averageSeasonPoints: Int { asp }
    //var team1Image: String { t1im }
    //var team2Image: String { t2im }

    // Computed properties
    public var hasPlayed: Bool { p != nil && st > 1 }
    public var wasStartingEleven: Bool { st == 5 }
    public var wasSubstitute: Bool { st == 3 }
    public var wasNotInSquad: Bool { st == 4 }
    public var didNotPlay: Bool { st <= 1 }

    public var statusText: String {
        switch st {
        case 0: return "Nicht gespielt"
        case 1: return "Verletzt/Gesperrt"
        case 3: return "Eingewechselt"
        case 4: return "Nicht im Kader"
        case 5: return "Startelf"
        default: return "Unbekannt"
        }
    }

    public var parsedMatchDate: Date {
        let formatter = ISO8601DateFormatter()
        return formatter.date(from: md) ?? Date()
    }

    public var opponentTeamId: String {
        // Fallback: Wenn pt (playerTeamId) leer ist, verwende eine andere Logik
        guard !playerTeamId.isEmpty else {
            // Wenn keine Player Team ID verfügbar ist, nimm einfach team2 als Gegner
            return t2
        }
        return playerTeamId == t1 ? t2 : t1
    }

    public var opponentTeamName: String {
        return TeamMapping.getTeamName(for: opponentTeamId) ?? "Unbekannt"
    }

    public var isHomeMatch: Bool {
        // Fallback: Wenn pt (playerTeamId) leer ist, nehme an dass es ein Auswärtsspiel ist
        guard !playerTeamId.isEmpty else {
            return false  // Standardmäßig Auswärtsspiel wenn unbekannt
        }
        return playerTeamId == t1
    }

    public var result: String {
        guard let t1g = t1g, let t2g = t2g else { return "-:-" }
        return "\(t1g):\(t2g)"
    }

    // Neue Methoden, die den Spieler-Kontext verwenden
    public func getOpponentTeamId(playerTeamId: String) -> String {
        return playerTeamId == t1 ? t2 : t1
    }

    public func getIsHomeMatch(playerTeamId: String) -> Bool {
        return playerTeamId == t1
    }
}

// MARK: - Team Profile Models
public struct TeamProfileResponse: Codable {
    public let tid: String  // Team ID
    public let tn: String  // Team Name
    public let pl: Int  // Placement (Platzierung)
    public let tv: Int  // Team Value
    public let tw: Int  // Team Wins
    public let td: Int  // Team Draws
    public let tl: Int  // Team Losses
    //let it: [TeamPlayer]? // Team Players (optional)
    public let npt: Int  // Next Point Total
    public let avpcl: Bool  // Available Players Close
}

public struct TeamInfo: Codable, Identifiable {
    public let id: String  // Team ID
    public let name: String  // Team Name
    public let placement: Int  // Platzierung

    public init(from response: TeamProfileResponse) {
        self.id = response.tid
        self.name = response.tn
        self.placement = response.pl
    }
}

// MARK: - Enhanced Performance Models with Team Info
public struct EnhancedMatchPerformance: Identifiable {
    public let basePerformance: MatchPerformance
    public let team1Info: TeamInfo?
    public let team2Info: TeamInfo?
    public let playerTeamInfo: TeamInfo?
    public let opponentTeamInfo: TeamInfo?

    // Delegiere alle Eigenschaften an basePerformance
    public var id: String { basePerformance.id }
    public var matchDay: Int { basePerformance.matchDay }
    public var points: Int { basePerformance.points }
    public var minutesPlayed: String { basePerformance.minutesPlayed }
    public var matchDate: String { basePerformance.matchDate }
    public var team1Id: String { basePerformance.team1Id }
    public var team2Id: String { basePerformance.team2Id }
    public var team1Goals: Int { basePerformance.team1Goals }
    public var team2Goals: Int { basePerformance.team2Goals }
    public var playerTeamId: String { basePerformance.playerTeamId }
    public var kickerRatings: [Int] { basePerformance.kickerRatings }
    public var status: Int { basePerformance.status }
    public var isCurrent: Bool { basePerformance.isCurrent }
    public var matchDayStatus: Int { basePerformance.matchDayStatus }
    public var averagePoints: Int { basePerformance.averagePoints }
    public var totalPoints: Int { basePerformance.totalPoints }
    public var averageSeasonPoints: Int { basePerformance.averageSeasonPoints }
    //var team1Image: String { basePerformance.team1Image }
    //var team2Image: String { basePerformance.team2Image }
    public var hasPlayed: Bool { basePerformance.hasPlayed }
    public var wasStartingEleven: Bool { basePerformance.wasStartingEleven }
    public var wasSubstitute: Bool { basePerformance.wasSubstitute }
    public var wasNotInSquad: Bool { basePerformance.wasNotInSquad }
    public var didNotPlay: Bool { basePerformance.didNotPlay }
    public var statusText: String { basePerformance.statusText }
    public var parsedMatchDate: Date { basePerformance.parsedMatchDate }
    public var opponentTeamId: String { basePerformance.opponentTeamId }
    public var isHomeMatch: Bool { basePerformance.isHomeMatch }
    public var result: String { basePerformance.result }

    // Erweiterte computed properties mit Team-Informationen
    public var team1Name: String {
        return team1Info?.name ?? TeamMapping.getTeamName(for: team1Id) ?? "Unbekannt"
    }

    public var team2Name: String {
        return team2Info?.name ?? TeamMapping.getTeamName(for: team2Id) ?? "Unbekannt"
    }

    public var playerTeamName: String {
        return playerTeamInfo?.name ?? TeamMapping.getTeamName(for: playerTeamId) ?? "Unbekannt"
    }

    public var opponentTeamName: String {
        return opponentTeamInfo?.name ?? TeamMapping.getTeamName(for: opponentTeamId) ?? "Unbekannt"
    }

    public var team1Placement: Int? {
        return team1Info?.placement
    }

    public var team2Placement: Int? {
        return team2Info?.placement
    }

    public var playerTeamPlacement: Int? {
        return playerTeamInfo?.placement
    }

    public var opponentTeamPlacement: Int? {
        return opponentTeamInfo?.placement
    }

    public var matchDescription: String {
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
public struct TeamStats: Codable {
    public let teamValue: Int
    public let teamValueTrend: Int
    public let budget: Int
    public let points: Int
    public let placement: Int
    public let won: Int
    public let drawn: Int
    public let lost: Int
}

public struct UserStats: Codable {
    public let teamValue: Int
    public let teamValueTrend: Int
    public let budget: Int
    public let points: Int
    public let placement: Int
    public let won: Int
    public let drawn: Int
    public let lost: Int
}

// MARK: - Team Mapping
public struct TeamMapping {
    // Wird durch Auto-Discovery gefüllt - siehe discoverAndMapTeams() in KickbaseManager
    static var teamIdToName: [String: String] = [:]

    static var teamNameToId: [String: String] = {
        var reversed: [String: String] = [:]
        for (id, name) in teamIdToName {
            reversed[name] = id
        }
        return reversed
    }()

    public static func getTeamName(for id: String) -> String? {
        return teamIdToName[id]
    }

    public static func getTeamId(for name: String) -> String? {
        return teamNameToId[name]
    }

    public static func getAllTeams() -> [String: String] {
        return teamIdToName
    }

    // Funktion zum Aktualisieren des Mappings durch Auto-Discovery
    public static func updateMapping(with discoveredTeams: [String: String]) {
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
public struct PlayerDetailResponse: Codable {
    public let fn: String?  // First Name
    public let ln: String?  // Last Name
    public let tn: String?  // Team Name
    public let shn: Int?  // Shirt Number (Trikotnummer)
    public let id: String?
    public let position: Int?
    public let number: Int?
    public let averagePoints: Double?
    public let totalPoints: Int?
    public let marketValue: Int?
    public let marketValueTrend: Int?
    public let profileBigUrl: String?
    public let teamId: String?
    public let tfhmvt: Int?
    public let prlo: Int?  // Profit/Loss since purchase - Gewinn/Verlust seit Kauf
    public let stl: Int?
    public let status: Int?
    public let userOwnsPlayer: Bool?
}

// MARK: - Market Value History Models
public struct MarketValueHistoryResponse: Codable {
    public let it: [MarketValueEntry]  // Liste der Marktwert-Einträge
    public let prlo: Int?  // Profit/Loss since purchase - auf Root-Ebene, nicht in den Einträgen
}

public struct MarketValueEntry: Codable {
    public let dt: Int  // Datum als Unix-Timestamp (Tage seit 1.1.1970)
    public let mv: Int  // Marktwert am entsprechenden Tag
    // prlo ist NICHT hier, sondern auf Root-Ebene in MarketValueHistoryResponse
}

// MARK: - Market Value Change Data
public struct DailyMarketValueChange {
    public let date: String
    public let value: Int
    public let change: Int
    public let percentageChange: Double
    public let daysAgo: Int

    public var isPositive: Bool {
        return change > 0
    }

    public var isNegative: Bool {
        return change < 0
    }
}

public struct MarketValueChange {
    public let daysSinceLastUpdate: Int
    public let absoluteChange: Int
    public let percentageChange: Double
    public let previousValue: Int
    public let currentValue: Int
    public let dailyChanges: [DailyMarketValueChange]

    public var isPositive: Bool {
        return absoluteChange > 0
    }

    public var isNegative: Bool {
        return absoluteChange < 0
    }
}

// MARK: - Demo Data Service

/// Service für realistische Demo-Daten für Apple Review
class DemoDataService {

    // MARK: - Demo User

    public static func createDemoUser() -> User {
        return User(
            id: "demo-user-001",
            name: "Demo User",
            teamName: "Demo Team",
            email: "demo@kickbasehelper.app",
            budget: 2_500_000,
            teamValue: 45_000_000,
            points: 287,
            placement: 5,
            flags: 0
        )
    }

    // MARK: - Demo Leagues

    public static func createDemoLeagues() -> [League] {
        let currentUser = LeagueUser(
            id: "demo-user-001",
            name: "Demo User",
            teamName: "Demo Team",
            budget: 2_500_000,
            teamValue: 45_000_000,
            points: 287,
            placement: 5,
            won: 8,
            drawn: 2,
            lost: 5,
            se11: 0,
            ttm: 0,
            mpst: 3
        )

        return [
            League(
                id: "demo-league-001",
                name: "🏆 Bundesliga Classic",
                creatorName: "Demo Admin",
                adminName: "Demo Admin",
                created: "2024-08-01",
                season: "2024/25",
                matchDay: 12,
                currentUser: currentUser
            ),
            League(
                id: "demo-league-002",
                name: "⚽ Friends Challenge",
                creatorName: "Demo Creator",
                adminName: "Demo Creator",
                created: "2024-09-15",
                season: "2024/25",
                matchDay: 10,
                currentUser: LeagueUser(
                    id: "demo-user-001",
                    name: "Demo User",
                    teamName: "Expert Squad",
                    budget: 1_800_000,
                    teamValue: 52_000_000,
                    points: 315,
                    placement: 2,
                    won: 9,
                    drawn: 1,
                    lost: 5,
                    se11: 0,
                    ttm: 0,
                    mpst: 3
                )
            ),
        ]
    }

    // MARK: - Demo Team Players

    public static func createDemoTeamPlayers() -> [TeamPlayer] {
        return [
            // Torwart
            TeamPlayer(
                id: "demo-player-001",
                firstName: "Manuel",
                lastName: "Neuer",
                profileBigUrl: "",
                teamName: "FC Bayern",
                teamId: "1",
                position: 1,  // Torwart
                number: 1,
                averagePoints: 7.2,
                totalPoints: 86,
                marketValue: 8_000_000,
                marketValueTrend: 500_000,
                tfhmvt: 250_000,
                prlo: 7_500_000,
                stl: 0,
                status: 0,
                userOwnsPlayer: true
            ),
            // Abwehr
            TeamPlayer(
                id: "demo-player-002",
                firstName: "Antonio",
                lastName: "Rüdiger",
                profileBigUrl: "",
                teamName: "Real Madrid",
                teamId: "2",
                position: 2,  // Abwehr
                number: 3,
                averagePoints: 6.8,
                totalPoints: 82,
                marketValue: 22_000_000,
                marketValueTrend: 1_000_000,
                tfhmvt: 500_000,
                prlo: 20_000_000,
                stl: 0,
                status: 0,
                userOwnsPlayer: true
            ),
            // Mittelfeld
            TeamPlayer(
                id: "demo-player-003",
                firstName: "Jamal",
                lastName: "Musiala",
                profileBigUrl: "",
                teamName: "FC Bayern",
                teamId: "1",
                position: 3,  // Mittelfeld
                number: 42,
                averagePoints: 7.5,
                totalPoints: 90,
                marketValue: 72_000_000,
                marketValueTrend: 2_000_000,
                tfhmvt: 1_000_000,
                prlo: 68_000_000,
                stl: 0,
                status: 0,
                userOwnsPlayer: true
            ),
            // Stürmer
            TeamPlayer(
                id: "demo-player-004",
                firstName: "Serge",
                lastName: "Gnabry",
                profileBigUrl: "",
                teamName: "FC Bayern",
                teamId: "1",
                position: 4,  // Stürmer
                number: 7,
                averagePoints: 6.9,
                totalPoints: 83,
                marketValue: 48_000_000,
                marketValueTrend: -500_000,
                tfhmvt: -250_000,
                prlo: 45_000_000,
                stl: 0,
                status: 0,
                userOwnsPlayer: true
            ),
            // Bank - Stürmer
            TeamPlayer(
                id: "demo-player-005",
                firstName: "Mathys",
                lastName: "Tel",
                profileBigUrl: "",
                teamName: "FC Bayern",
                teamId: "1",
                position: 4,  // Stürmer
                number: 39,
                averagePoints: 5.2,
                totalPoints: 52,
                marketValue: 28_000_000,
                marketValueTrend: 1_500_000,
                tfhmvt: 750_000,
                prlo: 26_000_000,
                stl: 0,
                status: 0,
                userOwnsPlayer: true
            ),
        ]
    }

    // MARK: - Demo Market Players

    public static func createDemoMarketPlayers() -> [MarketPlayer] {
        return [
            // Hochwertige Spieler auf dem Markt
            MarketPlayer(
                id: "demo-market-001",
                firstName: "Florian",
                lastName: "Wirtz",
                profileBigUrl: "",
                teamName: "Bayer Leverkusen",
                teamId: "5",
                position: 3,  // Mittelfeld
                number: 10,
                averagePoints: 8.1,
                totalPoints: 97,
                marketValue: 95_000_000,
                marketValueTrend: 5_000_000,
                price: 85_000_000,
                expiry: "2025-12-15T23:59:59Z",
                offers: 2,
                seller: MarketSeller(id: "seller-001", name: "Aktiver Spieler"),
                stl: 0,
                status: 1,
                prlo: 82_000_000,
                owner: nil,
                exs: 1_735_689_599
            ),
            MarketPlayer(
                id: "demo-market-002",
                firstName: "Florent",
                lastName: "Inzaghi",
                profileBigUrl: "",
                teamName: "Benfica Lissabon",
                teamId: "6",
                position: 4,  // Stürmer
                number: 9,
                averagePoints: 7.8,
                totalPoints: 94,
                marketValue: 58_000_000,
                marketValueTrend: 2_000_000,
                price: 52_000_000,
                expiry: "2025-12-10T23:59:59Z",
                offers: 4,
                seller: MarketSeller(id: "seller-002", name: "Demo Seller"),
                stl: 0,
                status: 1,
                prlo: 50_000_000,
                owner: nil,
                exs: 1_735_430_399
            ),
            MarketPlayer(
                id: "demo-market-003",
                firstName: "Lamine",
                lastName: "Yamal",
                profileBigUrl: "",
                teamName: "FC Barcelona",
                teamId: "3",
                position: 3,  // Mittelfeld
                number: 27,
                averagePoints: 7.2,
                totalPoints: 86,
                marketValue: 75_000_000,
                marketValueTrend: 3_500_000,
                price: 68_000_000,
                expiry: "2025-12-20T23:59:59Z",
                offers: 1,
                seller: MarketSeller(id: "seller-003", name: "Team Lead"),
                stl: 0,
                status: 1,
                prlo: 65_000_000,
                owner: nil,
                exs: 1_735_862_399
            ),
            MarketPlayer(
                id: "demo-market-004",
                firstName: "Vinícius",
                lastName: "Júnior",
                profileBigUrl: "",
                teamName: "Real Madrid",
                teamId: "2",
                position: 4,  // Stürmer
                number: 20,
                averagePoints: 8.4,
                totalPoints: 100,
                marketValue: 110_000_000,
                marketValueTrend: 4_000_000,
                price: 95_000_000,
                expiry: "2025-12-25T23:59:59Z",
                offers: 0,
                seller: MarketSeller(id: "seller-004", name: "Whale"),
                stl: 0,
                status: 1,
                prlo: 90_000_000,
                owner: nil,
                exs: 1_735_948_799
            ),
            MarketPlayer(
                id: "demo-market-005",
                firstName: "Joshua",
                lastName: "Kimmich",
                profileBigUrl: "",
                teamName: "FC Bayern",
                teamId: "1",
                position: 2,  // Abwehr
                number: 32,
                averagePoints: 6.7,
                totalPoints: 80,
                marketValue: 32_000_000,
                marketValueTrend: -1_000_000,
                price: 28_000_000,
                expiry: "2025-12-05T23:59:59Z",
                offers: 3,
                seller: MarketSeller(id: "seller-005", name: "Casual Player"),
                stl: 0,
                status: 1,
                prlo: 27_000_000,
                owner: nil,
                exs: 1_735_084_799
            ),
        ]
    }

    // MARK: - Demo User Stats

    public static func createDemoUserStats() -> UserStats {
        return UserStats(
            teamValue: 45_000_000,
            teamValueTrend: 500_000,
            budget: 2_500_000,
            points: 287,
            placement: 5,
            won: 8,
            drawn: 2,
            lost: 5
        )
    }

    // MARK: - Demo Market Value History

    public static func createDemoMarketValueHistory() -> MarketValueChange {
        let dailyChanges = [
            DailyMarketValueChange(
                date: "24. Nov",
                value: 45_500_000,
                change: 200_000,
                percentageChange: 0.44,
                daysAgo: 0
            ),
            DailyMarketValueChange(
                date: "23. Nov",
                value: 45_300_000,
                change: 100_000,
                percentageChange: 0.22,
                daysAgo: 1
            ),
            DailyMarketValueChange(
                date: "22. Nov",
                value: 45_200_000,
                change: -300_000,
                percentageChange: -0.66,
                daysAgo: 2
            ),
        ]

        return MarketValueChange(
            daysSinceLastUpdate: 1,
            absoluteChange: 500_000,
            percentageChange: 1.12,
            previousValue: 45_000_000,
            currentValue: 45_500_000,
            dailyChanges: dailyChanges
        )
    }

    // MARK: - Demo Login Response

    public static func createDemoLoginResponse() -> LoginResponse {
        let user = createDemoUser()
        return LoginResponse(
            tkn: "demo-token-\(UUID().uuidString)",
            user: user,
            leagues: createDemoLeagues(),
            userId: user.id
        )
    }
}
