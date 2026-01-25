import Foundation

// MARK: - Live Event Types
public struct LiveEventTypesResponse: Codable {
    public let types: [LiveEventType]
    public let formulas: [String: String]?
    
    enum CodingKeys: String, CodingKey {
        case types = "it"
        case formulas = "dds"
    }
}

public struct LiveEventType: Codable, Identifiable {
    public let id: Int
    public let name: String
    
    enum CodingKeys: String, CodingKey {
        case id = "i"
        case name = "ti"
    }
}

// MARK: - Live/Match Experience Models
public struct LiveMatchDayResponse: Codable {
    public let players: [LivePlayer]

    enum CodingKeys: String, CodingKey {
        case players = "lp"
    }
}

public struct LivePlayer: Codable, Identifiable {
    public let id: String
    public let name: String  // "n" in API
    public let position: Int
    public let teamId: String
    public let p: Int  // Live points
    public let profileBigUrl: String?
    public let k: [Int]

    public var imageUrl: URL? {
        guard let urlString = profileBigUrl else { return nil }
        if urlString.hasPrefix("http") {
            return URL(string: urlString)
        } else if !urlString.isEmpty {
            let path = urlString.hasPrefix("/") ? String(urlString.dropFirst()) : urlString
            return URL(string: "https://kickbase.com/\(path)")
        }
        return nil
    }

    public var eventIcons: String {
        return k.compactMap { eventId -> String? in
            switch eventId {
            case 1: return "🅰️"
            case 3: return "⚽️"
            case 5: return "🟨🟥"
            case 6: return "🟥"
            case 7: return "❌"
            default: return nil
            }
        }.joined()
    }

    enum CodingKeys: String, CodingKey {
        case id = "i"
        case name = "n"
        case position = "pos"
        case teamId = "tid"
        case p
        case profileBigUrl = "pb"
        case k
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        name = try container.decode(String.self, forKey: .name)
        position = try container.decode(Int.self, forKey: .position)
        teamId = try container.decode(String.self, forKey: .teamId)
        profileBigUrl = try container.decodeIfPresent(String.self, forKey: .profileBigUrl)
        p = try container.decodeIfPresent(Int.self, forKey: .p) ?? 0
        k = try container.decodeIfPresent([Int].self, forKey: .k) ?? []
    }
}

// MARK: - Player Match Detail
public struct PlayerMatchDetailResponse: Codable {
    public let events: [PlayerMatchEvent]

    enum CodingKeys: String, CodingKey {
        case events
    }
}

public struct PlayerMatchEvent: Codable, Identifiable {
    public var id: String {
        return eventId ?? UUID().uuidString
    }

    public let eventId: String?
    public let type: Int?
    public let minute: Int?
    public let points: Int?
    public let name: String?

    public var icon: String {
        switch type {
        case 1: return "🅰️"
        case 2: return "🧤"
        case 3: return "⚽️"
        case 4: return "🟨"
        case 5: return "🟨🟥"
        case 6: return "🟥"
        case 7: return "❌" // Missed Penalty
        case 8: return "💀" // Own Goal
        case 12: return "📺" // VAR
        default: return "🔹" // Generic
        }
    }

    enum CodingKeys: String, CodingKey {
        case eventId = "eid"
        case type = "t"
        case minute = "m"
        case points = "p"
        case name = "n"
    }

    enum V4CodingKeys: String, CodingKey {
        case eventId = "ei"
        case type = "eti"
        case minute = "mt"
        case points = "p"
        case attributes = "att"
    }

    public init(from decoder: Decoder) throws {
        let v4Container = try decoder.container(keyedBy: V4CodingKeys.self)
        let legacyContainer = try decoder.container(keyedBy: CodingKeys.self)

        // Try to decode V4 first
        let v4Type = try v4Container.decodeIfPresent(Int.self, forKey: .type)
        let v4Minute = try v4Container.decodeIfPresent(Int.self, forKey: .minute)
        
        if v4Type != nil || v4Minute != nil {
            self.type = v4Type
            self.minute = v4Minute
            self.eventId = try v4Container.decodeIfPresent(String.self, forKey: .eventId)
            self.points = try v4Container.decodeIfPresent(Int.self, forKey: .points)
            self.name = nil
        } else {
            // Legacy decode
            self.type = try legacyContainer.decodeIfPresent(Int.self, forKey: .type)
            self.minute = try legacyContainer.decodeIfPresent(Int.self, forKey: .minute)
            self.eventId = try legacyContainer.decodeIfPresent(String.self, forKey: .eventId)
            self.points = try legacyContainer.decodeIfPresent(Int.self, forKey: .points)
            self.name = try legacyContainer.decodeIfPresent(String.self, forKey: .name)
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encodeIfPresent(eventId, forKey: .eventId)
        try container.encodeIfPresent(type, forKey: .type)
        try container.encodeIfPresent(minute, forKey: .minute)
        try container.encodeIfPresent(points, forKey: .points)
        try container.encodeIfPresent(name, forKey: .name)
    }
}
