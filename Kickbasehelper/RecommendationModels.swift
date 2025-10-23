import Foundation

// MARK: - Transfer Recommendation Models

struct TransferRecommendation: Identifiable, Codable {
    let id = UUID()
    let player: MarketPlayer
    let recommendationScore: Double
    let reasons: [RecommendationReason]
    let analysis: PlayerAnalysis
    let riskLevel: RiskLevel
    let priority: Priority

    enum RiskLevel: String, CaseIterable, Codable {
        case low = "Niedrig"
        case medium = "Mittel"
        case high = "Hoch"

        var color: String {
            switch self {
            case .low: return "green"
            case .medium: return "orange"
            case .high: return "red"
            }
        }
    }

    enum Priority: String, CaseIterable, Codable {
        case essential = "Essentiell"
        case recommended = "Empfohlen"
        case optional = "Optional"

        var color: String {
            switch self {
            case .essential: return "red"
            case .recommended: return "orange"
            case .optional: return "blue"
            }
        }
    }
}

struct RecommendationReason: Identifiable, Codable {
    let id = UUID()
    let type: ReasonType
    let description: String
    let impact: Double  // 0-10 scale

    enum ReasonType: String, CaseIterable, Codable {
        case performance = "Leistung"
        case value = "Preis-Leistung"
        case potential = "Potenzial"
        case teamNeed = "Teambedarf"
        case injury = "Verletzungsrisiko"
        case form = "Form"
        case opponent = "Gegner"
    }
}

struct PlayerAnalysis: Codable {
    let pointsPerGame: Double
    let valueForMoney: Double
    let formTrend: FormTrend
    let injuryRisk: InjuryRisk
    let upcomingFixtures: [FixtureAnalysis]
    let seasonProjection: SeasonProjection

    enum FormTrend: String, Codable {
        case improving = "Verbesserung"
        case stable = "Stabil"
        case declining = "Verschlechterung"

        var color: String {
            switch self {
            case .improving: return "green"
            case .stable: return "blue"
            case .declining: return "red"
            }
        }
    }

    enum InjuryRisk: String, Codable {
        case low = "Niedrig"
        case medium = "Mittel"
        case high = "Hoch"

        var color: String {
            switch self {
            case .low: return "green"
            case .medium: return "orange"
            case .high: return "red"
            }
        }
    }
}

struct FixtureAnalysis: Identifiable, Codable {
    let id = UUID()
    let averageDifficulty: Double
    let topTeamOpponents: Int
    let difficultAwayGames: Int
    let totalMatches: Int
}

struct SeasonProjection: Codable {
    let projectedTotalPoints: Int
    let projectedValueIncrease: Int
    let confidence: Double  // 0-1 scale
}

// MARK: - Sale Recommendation Models

enum SaleRecommendationGoal: String, CaseIterable, Codable {
    case balanceBudget = "Budget ausgleichen"
    case improvePosition = "Position verbessern"
    case maxValue = "Maximalen Wert erzielen"
    case reduceRisk = "Risiko reduzieren"
    case raiseCapital = "Kapital aufbringen"

    var description: String {
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

    var icon: String {
        switch self {
        case .balanceBudget: return "balance.3"
        case .improvePosition: return "arrow.up.circle"
        case .maxValue: return "chart.line.uptrend.xyaxis"
        case .reduceRisk: return "exclamationmark.shield"
        case .raiseCapital: return "bitcoinsign.circle"
        }
    }
}

struct ReplacementSuggestion: Identifiable, Codable {
    let id = UUID()
    let player: MarketPlayer
    let reasonForSale: String
    let budgetSavings: Int  // Positive Zahl = wir sparen Geld
    let performanceGain: Double  // Punkte pro Spiel Differenz
    let riskReduction: Double  // 0-1, wie viel Risiko reduziert wird
}

struct SaleRecommendation: Identifiable, Codable {
    let id = UUID()
    let playerToSell: TeamPlayer
    let replacements: [ReplacementSuggestion]
    let goal: SaleRecommendationGoal
    let explanation: String
    let priority: TransferRecommendation.Priority

    var bestReplacement: ReplacementSuggestion? {
        replacements.first
    }
}

// MARK: - Team Analysis Models

struct TeamAnalysis: Codable {
    let weakPositions: [Position]
    let strengths: [Position]
    let budgetConstraints: BudgetAnalysis
    let recommendations: [PositionalRecommendation]

    enum Position: String, CaseIterable, Codable {
        case goalkeeper = "TW"
        case defender = "ABW"
        case midfielder = "MF"
        case striker = "ST"

        var positionNumber: Int {
            switch self {
            case .goalkeeper: return 1
            case .defender: return 2
            case .midfielder: return 3
            case .striker: return 4
            }
        }
    }
}

struct BudgetAnalysis: Codable {
    let availableBudget: Int
    let recommendedSpending: Int
    let maxAffordablePrice: Int
    let emergencyReserve: Int
}

struct PositionalRecommendation: Identifiable, Codable {
    let id = UUID()
    let position: TeamAnalysis.Position
    let priority: Int  // 1-5 scale
    let reasoning: String
    let suggestedPlayers: [String]  // Player IDs
}
