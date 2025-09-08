import Foundation

// MARK: - Authentication Models
struct LoginRequest: Codable {
    let em: String      // email
    let pass: String    // password
    let loy: Bool = false   // loyalty (keep logged in)
    let rep: [String: String] = [:]  // empty rep object
}

struct LoginResponse: Codable {
    let tkn: String     // token
    let user: User
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
struct MarketPlayer: Codable, Identifiable {
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
}

struct MarketSeller: Codable {
    let id: String
    let name: String
}

// MARK: - Gift Models
struct Gift: Codable, Identifiable {
    let id: String
    let type: String
    let amount: Int
    let level: Int
    let collected: Bool
}

// MARK: - Response Wrappers
struct PlayersResponse: Codable {
    let players: [Player]
}

struct MarketResponse: Codable {
    let players: [MarketPlayer]
}

struct GiftsResponse: Codable {
    let gifts: [Gift]
    let nextGift: String?
}

struct LeaguesResponse: Codable {
    let leagues: [League]
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
}

struct MarketValueEntry: Codable {
    let dt: Int    // Datum als Unix-Timestamp
    let mv: Int    // Marktwert am entsprechenden Tag
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
    let dailyChanges: [DailyMarketValueChange] // Neu: Tägliche Änderungen der letzten drei Tage
    
    var isPositive: Bool {
        return absoluteChange > 0
    }
    
    var isNegative: Bool {
        return absoluteChange < 0
    }
}
