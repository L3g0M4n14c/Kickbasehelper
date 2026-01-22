import Foundation
import SwiftUI

// MARK: - Transfer Recommendation Models

public struct TransferRecommendation: Identifiable {
    public let id: UUID
    public let player: MarketPlayer
    public let recommendationScore: Double
    public let reasons: [RecommendationReason]
    public let analysis: PlayerAnalysis
    public let riskLevel: RiskLevel
    public let priority: Priority

    public init(
        id: UUID = UUID(), player: MarketPlayer, recommendationScore: Double,
        reasons: [RecommendationReason], analysis: PlayerAnalysis, riskLevel: RiskLevel,
        priority: Priority
    ) {
        self.id = id
        self.player = player
        self.recommendationScore = recommendationScore
        self.reasons = reasons
        self.analysis = analysis
        self.riskLevel = riskLevel
        self.priority = priority
    }

    public enum RiskLevel: String, CaseIterable, Codable {
        case low = "Niedrig"
        case medium = "Mittel"
        case high = "Hoch"

        public var color: Color {
            switch self {
            case .low: return .green
            case .medium: return .orange
            case .high: return .red
            }
        }
    }

    public enum Priority: String, CaseIterable, Codable {
        case essential = "Essentiell"
        case recommended = "Empfohlen"
        case optional = "Optional"

        public var color: Color {
            switch self {
            case .essential: return .red
            case .recommended: return .orange
            case .optional: return .blue
            }
        }
    }
}

public struct RecommendationReason: Identifiable {
    public let id: UUID
    public let type: ReasonType
    public let description: String
    public let impact: Double  // 0-10 scale

    public init(id: UUID = UUID(), type: ReasonType, description: String, impact: Double) {
        self.id = id
        self.type = type
        self.description = description
        self.impact = impact
    }

    public enum ReasonType: String, CaseIterable, Codable {
        case performance = "Leistung"
        case value = "Preis-Leistung"
        case potential = "Potenzial"
        case teamNeed = "Teambedarf"
        case injury = "Verletzungsrisiko"
        case form = "Form"
        case opponent = "Gegner"
    }
}

public struct PlayerAnalysis: Codable {
    public let pointsPerGame: Double
    public let valueForMoney: Double
    public let formTrend: FormTrend
    public let injuryRisk: InjuryRisk
    public let upcomingFixtures: [FixtureAnalysis]
    public let seasonProjection: SeasonProjection

    public enum FormTrend: String, Codable {
        case improving = "Verbesserung"
        case stable = "Stabil"
        case declining = "Verschlechterung"

        public var color: Color {
            switch self {
            case .improving: return .green
            case .stable: return .blue
            case .declining: return .red
            }
        }
    }

    public enum InjuryRisk: String, Codable {
        case low = "Niedrig"
        case medium = "Mittel"
        case high = "Hoch"

        public var color: Color {
            switch self {
            case .low: return .green
            case .medium: return .orange
            case .high: return .red
            }
        }
    }
}

public struct FixtureAnalysis: Identifiable, Codable {
    public var id = UUID()
    public let averageDifficulty: Double
    public let topTeamOpponents: Int
    public let difficultAwayGames: Int
    public let totalMatches: Int

    public init(
        averageDifficulty: Double, topTeamOpponents: Int, difficultAwayGames: Int,
        totalMatches: Int
    ) {
        self.averageDifficulty = averageDifficulty
        self.topTeamOpponents = topTeamOpponents
        self.difficultAwayGames = difficultAwayGames
        self.totalMatches = totalMatches
    }

    private enum CodingKeys: String, CodingKey {
        case averageDifficulty, topTeamOpponents, difficultAwayGames, totalMatches
    }
}

public struct SeasonProjection: Codable {
    public let projectedTotalPoints: Int
    public let projectedValueIncrease: Int
    public let confidence: Double  // 0-1 scale
}

// MARK: - Sale Recommendation Models

public enum SaleRecommendationGoal: String, CaseIterable, Codable {
    case balanceBudget = "Budget ausgleichen"
    case improvePosition = "Position verbessern"
    case maxValue = "Maximalen Wert erzielen"
    case reduceRisk = "Risiko reduzieren"
    case raiseCapital = "Kapital aufbringen"

    public var description: String {
        switch self {
        case .balanceBudget:
            return
                "Einen günstigen Spieler verkaufen und durch einen besseren ersetzen um Budget zu sparen"
        case .improvePosition:
            return "Schwachen Spieler verkaufen und durch besseren ersetzen"
        case .maxValue:
            return "Spieler mit höchstem Gewinn verkaufen"
        case .reduceRisk:
            return "Riskante Spieler verkaufen"
        case .raiseCapital:
            return "Schnell Geld für neue Spieler beschaffen"
        }
    }

    public var icon: String {
        switch self {
        case .balanceBudget: return "balance.3"
        case .improvePosition: return "arrow.up.circle"
        case .maxValue: return "chart.line.uptrend.xyaxis"
        case .reduceRisk: return "exclamationmark.shield"
        case .raiseCapital: return "bitcoinsign.circle"
        }
    }
}

public struct ReplacementSuggestion: Identifiable {
    public let id: UUID
    public let player: MarketPlayer
    public let reasonForSale: String
    public let budgetSavings: Int  // Positive Zahl = wir sparen Geld
    public let performanceGain: Double  // Punkte pro Spiel Differenz
    public let riskReduction: Double  // 0-1, wie viel Risiko reduziert wird

    public init(
        id: UUID = UUID(), player: MarketPlayer, reasonForSale: String, budgetSavings: Int,
        performanceGain: Double, riskReduction: Double
    ) {
        self.id = id
        self.player = player
        self.reasonForSale = reasonForSale
        self.budgetSavings = budgetSavings
        self.performanceGain = performanceGain
        self.riskReduction = riskReduction
    }
}

public struct SaleRecommendation: Identifiable {
    public let id: UUID
    public let playerToSell: TeamPlayer
    public let replacements: [ReplacementSuggestion]
    public let goal: SaleRecommendationGoal
    public let explanation: String
    public let priority: TransferRecommendation.Priority

    public init(
        id: UUID = UUID(), playerToSell: TeamPlayer, replacements: [ReplacementSuggestion],
        goal: SaleRecommendationGoal, explanation: String, priority: TransferRecommendation.Priority
    ) {
        self.id = id
        self.playerToSell = playerToSell
        self.replacements = replacements
        self.goal = goal
        self.explanation = explanation
        self.priority = priority
    }

    public var bestReplacement: ReplacementSuggestion? {
        replacements.first
    }
}

// MARK: - Lineup Models

public struct LineupSlot: Identifiable {
    public let id: UUID
    public let slotIndex: Int  // 0-10 oder je nach Formation
    public let positionType: Int  // 1=TW, 2=ABW, 3=MF, 4=ST
    public let ownedPlayerId: String?  // ID des eigenen Spielers an dieser Position
    public let recommendedMarketPlayerId: String?  // ID des Markt-Spielers falls besser
    public let slotScore: Double  // Bewertung für diese Position (nur dieser Spieler)

    public init(
        slotIndex: Int,
        positionType: Int,
        ownedPlayerId: String? = nil,
        recommendedMarketPlayerId: String? = nil,
        slotScore: Double
    ) {
        self.id = UUID()
        self.slotIndex = slotIndex
        self.positionType = positionType
        self.ownedPlayerId = ownedPlayerId
        self.recommendedMarketPlayerId = recommendedMarketPlayerId
        self.slotScore = slotScore
    }

    public var hasBetterMarketOption: Bool {
        return recommendedMarketPlayerId != nil && ownedPlayerId != recommendedMarketPlayerId
    }
}

public struct OptimalLineupResult: Identifiable {
    public let id: UUID
    public let slots: [LineupSlot]
    public let formationName: String
    public let totalLineupScore: Double
    public let isHybridWithMarketPlayers: Bool
    public let marketPlayersNeeded: [String]  // IDs der Markt-Spieler die gekauft werden müssten
    public let totalMarketCost: Int  // Summe der Preise aller benötigten Markt-Spieler
    public let averagePlayerScore: Double  // Durchschnittliche Spielerbewertung

    public init(
        slots: [LineupSlot],
        formationName: String,
        totalLineupScore: Double,
        isHybridWithMarketPlayers: Bool,
        marketPlayersNeeded: [String] = [],
        totalMarketCost: Int = 0,
        averagePlayerScore: Double
    ) {
        self.id = UUID()
        self.slots = slots
        self.formationName = formationName
        self.totalLineupScore = totalLineupScore
        self.isHybridWithMarketPlayers = isHybridWithMarketPlayers
        self.marketPlayersNeeded = marketPlayersNeeded
        self.totalMarketCost = totalMarketCost
        self.averagePlayerScore = averagePlayerScore
    }

    public var ownedPlayerCount: Int {
        slots.filter { $0.ownedPlayerId != nil && !$0.hasBetterMarketOption }.count
    }

    public var marketPlayerCount: Int {
        slots.filter { $0.hasBetterMarketOption }.count
    }
}

public struct LineupComparison: Identifiable {
    public let id: UUID
    public let teamOnlyLineup: OptimalLineupResult
    public let hybridLineup: OptimalLineupResult?

    public init(
        teamOnlyLineup: OptimalLineupResult,
        hybridLineup: OptimalLineupResult? = nil
    ) {
        self.id = UUID()
        self.teamOnlyLineup = teamOnlyLineup
        self.hybridLineup = hybridLineup
    }

    public var performanceGainWithHybrid: Double {
        guard let hybrid = hybridLineup else { return 0 }
        return hybrid.averagePlayerScore - teamOnlyLineup.averagePlayerScore
    }

    public var shouldShowHybrid: Bool {
        return hybridLineup != nil && (hybridLineup?.marketPlayerCount ?? 0) > 0
    }

    public var totalInvestmentNeeded: Int {
        return hybridLineup?.totalMarketCost ?? 0
    }
}

// MARK: - Team Analysis Models

public struct TeamAnalysis: Codable {
    public let weakPositions: [Position]
    public let strengths: [Position]
    public let budgetConstraints: BudgetAnalysis
    public let recommendations: [PositionalRecommendation]

    public enum Position: String, CaseIterable, Codable {
        case goalkeeper = "TW"
        case defender = "ABW"
        case midfielder = "MF"
        case striker = "ST"

        public var positionNumber: Int {
            switch self {
            case .goalkeeper: return 1
            case .defender: return 2
            case .midfielder: return 3
            case .striker: return 4
            }
        }
    }
}

public struct BudgetAnalysis: Codable {
    public let availableBudget: Int
    public let recommendedSpending: Int
    public let maxAffordablePrice: Int
    public let emergencyReserve: Int
}

public struct PositionalRecommendation: Identifiable, Codable {
    public let id = UUID()
    public let position: TeamAnalysis.Position
    public let priority: Int  // 1-5 scale
    public let reasoning: String
    public let suggestedPlayers: [String]  // Player IDs
}
