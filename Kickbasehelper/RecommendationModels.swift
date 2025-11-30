import Foundation

// MARK: - Transfer Recommendation Models

struct TransferRecommendation: Identifiable {
    let id: UUID
    let player: MarketPlayer
    let recommendationScore: Double
    let reasons: [RecommendationReason]
    let analysis: PlayerAnalysis
    let riskLevel: RiskLevel
    let priority: Priority

    init(
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

struct RecommendationReason: Identifiable {
    let id: UUID
    let type: ReasonType
    let description: String
    let impact: Double  // 0-10 scale

    init(id: UUID = UUID(), type: ReasonType, description: String, impact: Double) {
        self.id = id
        self.type = type
        self.description = description
        self.impact = impact
    }

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

struct ReplacementSuggestion: Identifiable {
    let id: UUID
    let player: MarketPlayer
    let reasonForSale: String
    let budgetSavings: Int  // Positive Zahl = wir sparen Geld
    let performanceGain: Double  // Punkte pro Spiel Differenz
    let riskReduction: Double  // 0-1, wie viel Risiko reduziert wird

    init(
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

struct SaleRecommendation: Identifiable {
    let id: UUID
    let playerToSell: TeamPlayer
    let replacements: [ReplacementSuggestion]
    let goal: SaleRecommendationGoal
    let explanation: String
    let priority: TransferRecommendation.Priority

    init(
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

    var bestReplacement: ReplacementSuggestion? {
        replacements.first
    }
}

// MARK: - Lineup Models

struct LineupSlot: Identifiable {
    let id: UUID
    let slotIndex: Int  // 0-10 oder je nach Formation
    let positionType: Int  // 1=TW, 2=ABW, 3=MF, 4=ST
    let ownedPlayerId: String?  // ID des eigenen Spielers an dieser Position
    let recommendedMarketPlayerId: String?  // ID des Markt-Spielers falls besser
    let slotScore: Double  // Bewertung für diese Position (nur dieser Spieler)

    init(
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

    var hasBetterMarketOption: Bool {
        return recommendedMarketPlayerId != nil && ownedPlayerId != recommendedMarketPlayerId
    }
}

struct OptimalLineupResult: Identifiable {
    let id: UUID
    let slots: [LineupSlot]
    let formationName: String
    let totalLineupScore: Double
    let isHybridWithMarketPlayers: Bool
    let marketPlayersNeeded: [String]  // IDs der Markt-Spieler die gekauft werden müssten
    let totalMarketCost: Int  // Summe der Preise aller benötigten Markt-Spieler
    let averagePlayerScore: Double  // Durchschnittliche Spielerbewertung

    init(
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

    var ownedPlayerCount: Int {
        slots.filter { $0.ownedPlayerId != nil && !$0.hasBetterMarketOption }.count
    }

    var marketPlayerCount: Int {
        slots.filter { $0.hasBetterMarketOption }.count
    }
}

struct LineupComparison: Identifiable {
    let id: UUID
    let teamOnlyLineup: OptimalLineupResult
    let hybridLineup: OptimalLineupResult?

    init(
        teamOnlyLineup: OptimalLineupResult,
        hybridLineup: OptimalLineupResult? = nil
    ) {
        self.id = UUID()
        self.teamOnlyLineup = teamOnlyLineup
        self.hybridLineup = hybridLineup
    }

    var performanceGainWithHybrid: Double {
        guard let hybrid = hybridLineup else { return 0 }
        return hybrid.averagePlayerScore - teamOnlyLineup.averagePlayerScore
    }

    var shouldShowHybrid: Bool {
        return hybridLineup != nil && (hybridLineup?.marketPlayerCount ?? 0) > 0
    }

    var totalInvestmentNeeded: Int {
        return hybridLineup?.totalMarketCost ?? 0
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
