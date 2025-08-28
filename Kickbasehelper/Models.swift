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
    let status: Int
    let userOwnsPlayer: Bool
    
    var fullName: String {
        return "\(firstName) \(lastName)"
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
    
    var fullName: String {
        return "\(firstName) \(lastName)"
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
