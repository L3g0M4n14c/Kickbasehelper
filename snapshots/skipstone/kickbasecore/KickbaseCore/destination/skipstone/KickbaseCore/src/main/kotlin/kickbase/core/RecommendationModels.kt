package kickbase.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import skip.lib.*
import skip.lib.Array

import skip.foundation.*
import skip.ui.*
import skip.model.*

// MARK: - Transfer Recommendation Models

class TransferRecommendation: Identifiable<UUID> {
    override val id: UUID
    val player: MarketPlayer
    val recommendationScore: Double
    val reasons: Array<RecommendationReason>
    val analysis: PlayerAnalysis
    val riskLevel: TransferRecommendation.RiskLevel
    val priority: TransferRecommendation.Priority

    constructor(id: UUID = UUID(), player: MarketPlayer, recommendationScore: Double, reasons: Array<RecommendationReason>, analysis: PlayerAnalysis, riskLevel: TransferRecommendation.RiskLevel, priority: TransferRecommendation.Priority) {
        this.id = id
        this.player = player
        this.recommendationScore = recommendationScore
        this.reasons = reasons.sref()
        this.analysis = analysis
        this.riskLevel = riskLevel
        this.priority = priority
    }

    @androidx.annotation.Keep
    enum class RiskLevel(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, Codable, RawRepresentable<String> {
        low("Niedrig"),
        medium("Mittel"),
        high("Hoch");

        val color: Color
            get() {
                when (this) {
                    TransferRecommendation.RiskLevel.low -> return Color.green
                    TransferRecommendation.RiskLevel.medium -> return Color.orange
                    TransferRecommendation.RiskLevel.high -> return Color.red
                }
            }

        override fun encode(to: Encoder) {
            val container = to.singleValueContainer()
            container.encode(rawValue)
        }

        @androidx.annotation.Keep
        companion object: CaseIterableCompanion<TransferRecommendation.RiskLevel>, DecodableCompanion<TransferRecommendation.RiskLevel> {
            override fun init(from: Decoder): TransferRecommendation.RiskLevel = RiskLevel(from = from)

            fun init(rawValue: String): TransferRecommendation.RiskLevel? {
                return when (rawValue) {
                    "Niedrig" -> RiskLevel.low
                    "Mittel" -> RiskLevel.medium
                    "Hoch" -> RiskLevel.high
                    else -> null
                }
            }

            override val allCases: Array<TransferRecommendation.RiskLevel>
                get() = arrayOf(low, medium, high)
        }
    }

    @androidx.annotation.Keep
    enum class Priority(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, Codable, RawRepresentable<String> {
        essential("Essentiell"),
        recommended("Empfohlen"),
        optional("Optional");

        val color: Color
            get() {
                when (this) {
                    TransferRecommendation.Priority.essential -> return Color.red
                    TransferRecommendation.Priority.recommended -> return Color.orange
                    TransferRecommendation.Priority.optional -> return Color.blue
                }
            }

        override fun encode(to: Encoder) {
            val container = to.singleValueContainer()
            container.encode(rawValue)
        }

        @androidx.annotation.Keep
        companion object: CaseIterableCompanion<TransferRecommendation.Priority>, DecodableCompanion<TransferRecommendation.Priority> {
            override fun init(from: Decoder): TransferRecommendation.Priority = Priority(from = from)

            fun init(rawValue: String): TransferRecommendation.Priority? {
                return when (rawValue) {
                    "Essentiell" -> Priority.essential
                    "Empfohlen" -> Priority.recommended
                    "Optional" -> Priority.optional
                    else -> null
                }
            }

            override val allCases: Array<TransferRecommendation.Priority>
                get() = arrayOf(essential, recommended, optional)
        }
    }

    @androidx.annotation.Keep
    companion object {

        fun RiskLevel(from: Decoder): TransferRecommendation.RiskLevel {
            val container = from.singleValueContainer()
            val rawValue = container.decode(String::class)
            return RiskLevel(rawValue = rawValue) ?: throw ErrorException(cause = NullPointerException())
        }

        fun Priority(from: Decoder): TransferRecommendation.Priority {
            val container = from.singleValueContainer()
            val rawValue = container.decode(String::class)
            return Priority(rawValue = rawValue) ?: throw ErrorException(cause = NullPointerException())
        }

        fun RiskLevel(rawValue: String): TransferRecommendation.RiskLevel? = RiskLevel.init(rawValue = rawValue)

        fun Priority(rawValue: String): TransferRecommendation.Priority? = Priority.init(rawValue = rawValue)
    }
}

class RecommendationReason: Identifiable<UUID> {
    override val id: UUID
    val type: RecommendationReason.ReasonType
    val description: String
    val impact: Double // 0-10 scale

    constructor(id: UUID = UUID(), type: RecommendationReason.ReasonType, description: String, impact: Double) {
        this.id = id
        this.type = type
        this.description = description
        this.impact = impact
    }

    @androidx.annotation.Keep
    enum class ReasonType(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, Codable, RawRepresentable<String> {
        performance("Leistung"),
        value_("Preis-Leistung"),
        potential("Potenzial"),
        teamNeed("Teambedarf"),
        injury("Verletzungsrisiko"),
        form("Form"),
        opponent("Gegner");

        override fun encode(to: Encoder) {
            val container = to.singleValueContainer()
            container.encode(rawValue)
        }

        @androidx.annotation.Keep
        companion object: CaseIterableCompanion<RecommendationReason.ReasonType>, DecodableCompanion<RecommendationReason.ReasonType> {
            override fun init(from: Decoder): RecommendationReason.ReasonType = ReasonType(from = from)

            fun init(rawValue: String): RecommendationReason.ReasonType? {
                return when (rawValue) {
                    "Leistung" -> ReasonType.performance
                    "Preis-Leistung" -> ReasonType.value_
                    "Potenzial" -> ReasonType.potential
                    "Teambedarf" -> ReasonType.teamNeed
                    "Verletzungsrisiko" -> ReasonType.injury
                    "Form" -> ReasonType.form
                    "Gegner" -> ReasonType.opponent
                    else -> null
                }
            }

            override val allCases: Array<RecommendationReason.ReasonType>
                get() = arrayOf(performance, value_, potential, teamNeed, injury, form, opponent)
        }
    }

    @androidx.annotation.Keep
    companion object {

        fun ReasonType(from: Decoder): RecommendationReason.ReasonType {
            val container = from.singleValueContainer()
            val rawValue = container.decode(String::class)
            return ReasonType(rawValue = rawValue) ?: throw ErrorException(cause = NullPointerException())
        }

        fun ReasonType(rawValue: String): RecommendationReason.ReasonType? = ReasonType.init(rawValue = rawValue)
    }
}

enum class RecommendationSortOption(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<String> {
    recommendationScore("Empfehlungswert"),
    price("Preis"),
    points("Punkte"),
    valueForMoney("Preis-Leistung"),
    formTrend("Form-Trend"),
    risk("Risiko");

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: String): RecommendationSortOption? {
            return when (rawValue) {
                "Empfehlungswert" -> RecommendationSortOption.recommendationScore
                "Preis" -> RecommendationSortOption.price
                "Punkte" -> RecommendationSortOption.points
                "Preis-Leistung" -> RecommendationSortOption.valueForMoney
                "Form-Trend" -> RecommendationSortOption.formTrend
                "Risiko" -> RecommendationSortOption.risk
                else -> null
            }
        }
    }
}

fun RecommendationSortOption(rawValue: String): RecommendationSortOption? = RecommendationSortOption.init(rawValue = rawValue)

@androidx.annotation.Keep
class PlayerAnalysis: Codable {
    val pointsPerGame: Double
    val valueForMoney: Double
    val formTrend: PlayerAnalysis.FormTrend
    val injuryRisk: PlayerAnalysis.InjuryRisk
    val upcomingFixtures: Array<FixtureAnalysis>
    val seasonProjection: SeasonProjection

    @androidx.annotation.Keep
    enum class FormTrend(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): Codable, RawRepresentable<String> {
        improving("Verbesserung"),
        stable("Stabil"),
        declining("Verschlechterung");

        val color: Color
            get() {
                when (this) {
                    PlayerAnalysis.FormTrend.improving -> return Color.green
                    PlayerAnalysis.FormTrend.stable -> return Color.blue
                    PlayerAnalysis.FormTrend.declining -> return Color.red
                }
            }

        override fun encode(to: Encoder) {
            val container = to.singleValueContainer()
            container.encode(rawValue)
        }

        @androidx.annotation.Keep
        companion object: DecodableCompanion<PlayerAnalysis.FormTrend> {
            override fun init(from: Decoder): PlayerAnalysis.FormTrend = FormTrend(from = from)

            fun init(rawValue: String): PlayerAnalysis.FormTrend? {
                return when (rawValue) {
                    "Verbesserung" -> FormTrend.improving
                    "Stabil" -> FormTrend.stable
                    "Verschlechterung" -> FormTrend.declining
                    else -> null
                }
            }
        }
    }

    @androidx.annotation.Keep
    enum class InjuryRisk(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): Codable, RawRepresentable<String> {
        low("Niedrig"),
        medium("Mittel"),
        high("Hoch");

        val color: Color
            get() {
                when (this) {
                    PlayerAnalysis.InjuryRisk.low -> return Color.green
                    PlayerAnalysis.InjuryRisk.medium -> return Color.orange
                    PlayerAnalysis.InjuryRisk.high -> return Color.red
                }
            }

        override fun encode(to: Encoder) {
            val container = to.singleValueContainer()
            container.encode(rawValue)
        }

        @androidx.annotation.Keep
        companion object: DecodableCompanion<PlayerAnalysis.InjuryRisk> {
            override fun init(from: Decoder): PlayerAnalysis.InjuryRisk = InjuryRisk(from = from)

            fun init(rawValue: String): PlayerAnalysis.InjuryRisk? {
                return when (rawValue) {
                    "Niedrig" -> InjuryRisk.low
                    "Mittel" -> InjuryRisk.medium
                    "Hoch" -> InjuryRisk.high
                    else -> null
                }
            }
        }
    }

    constructor(pointsPerGame: Double, valueForMoney: Double, formTrend: PlayerAnalysis.FormTrend, injuryRisk: PlayerAnalysis.InjuryRisk, upcomingFixtures: Array<FixtureAnalysis>, seasonProjection: SeasonProjection) {
        this.pointsPerGame = pointsPerGame
        this.valueForMoney = valueForMoney
        this.formTrend = formTrend
        this.injuryRisk = injuryRisk
        this.upcomingFixtures = upcomingFixtures.sref()
        this.seasonProjection = seasonProjection
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        pointsPerGame("pointsPerGame"),
        valueForMoney("valueForMoney"),
        formTrend("formTrend"),
        injuryRisk("injuryRisk"),
        upcomingFixtures("upcomingFixtures"),
        seasonProjection("seasonProjection");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "pointsPerGame" -> CodingKeys.pointsPerGame
                    "valueForMoney" -> CodingKeys.valueForMoney
                    "formTrend" -> CodingKeys.formTrend
                    "injuryRisk" -> CodingKeys.injuryRisk
                    "upcomingFixtures" -> CodingKeys.upcomingFixtures
                    "seasonProjection" -> CodingKeys.seasonProjection
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(pointsPerGame, forKey = CodingKeys.pointsPerGame)
        container.encode(valueForMoney, forKey = CodingKeys.valueForMoney)
        container.encode(formTrend, forKey = CodingKeys.formTrend)
        container.encode(injuryRisk, forKey = CodingKeys.injuryRisk)
        container.encode(upcomingFixtures, forKey = CodingKeys.upcomingFixtures)
        container.encode(seasonProjection, forKey = CodingKeys.seasonProjection)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.pointsPerGame = container.decode(Double::class, forKey = CodingKeys.pointsPerGame)
        this.valueForMoney = container.decode(Double::class, forKey = CodingKeys.valueForMoney)
        this.formTrend = container.decode(PlayerAnalysis.FormTrend::class, forKey = CodingKeys.formTrend)
        this.injuryRisk = container.decode(PlayerAnalysis.InjuryRisk::class, forKey = CodingKeys.injuryRisk)
        this.upcomingFixtures = container.decode(Array::class, elementType = FixtureAnalysis::class, forKey = CodingKeys.upcomingFixtures)
        this.seasonProjection = container.decode(SeasonProjection::class, forKey = CodingKeys.seasonProjection)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<PlayerAnalysis> {
        override fun init(from: Decoder): PlayerAnalysis = PlayerAnalysis(from = from)

        fun FormTrend(from: Decoder): PlayerAnalysis.FormTrend {
            val container = from.singleValueContainer()
            val rawValue = container.decode(String::class)
            return FormTrend(rawValue = rawValue) ?: throw ErrorException(cause = NullPointerException())
        }

        fun InjuryRisk(from: Decoder): PlayerAnalysis.InjuryRisk {
            val container = from.singleValueContainer()
            val rawValue = container.decode(String::class)
            return InjuryRisk(rawValue = rawValue) ?: throw ErrorException(cause = NullPointerException())
        }

        fun FormTrend(rawValue: String): PlayerAnalysis.FormTrend? = FormTrend.init(rawValue = rawValue)

        fun InjuryRisk(rawValue: String): PlayerAnalysis.InjuryRisk? = InjuryRisk.init(rawValue = rawValue)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class FixtureAnalysis: Identifiable<UUID>, Codable, MutableStruct {
    override var id = UUID()
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    val averageDifficulty: Double
    val topTeamOpponents: Int
    val difficultAwayGames: Int
    val totalMatches: Int

    constructor(averageDifficulty: Double, topTeamOpponents: Int, difficultAwayGames: Int, totalMatches: Int) {
        this.averageDifficulty = averageDifficulty
        this.topTeamOpponents = topTeamOpponents
        this.difficultAwayGames = difficultAwayGames
        this.totalMatches = totalMatches
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        averageDifficulty("averageDifficulty"),
        topTeamOpponents("topTeamOpponents"),
        difficultAwayGames("difficultAwayGames"),
        totalMatches("totalMatches");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): FixtureAnalysis.CodingKeys? {
                return when (rawValue) {
                    "averageDifficulty" -> CodingKeys.averageDifficulty
                    "topTeamOpponents" -> CodingKeys.topTeamOpponents
                    "difficultAwayGames" -> CodingKeys.difficultAwayGames
                    "totalMatches" -> CodingKeys.totalMatches
                    else -> null
                }
            }
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as FixtureAnalysis
        this.id = copy.id
        this.averageDifficulty = copy.averageDifficulty
        this.topTeamOpponents = copy.topTeamOpponents
        this.difficultAwayGames = copy.difficultAwayGames
        this.totalMatches = copy.totalMatches
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = FixtureAnalysis(this as MutableStruct)

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(averageDifficulty, forKey = CodingKeys.averageDifficulty)
        container.encode(topTeamOpponents, forKey = CodingKeys.topTeamOpponents)
        container.encode(difficultAwayGames, forKey = CodingKeys.difficultAwayGames)
        container.encode(totalMatches, forKey = CodingKeys.totalMatches)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.averageDifficulty = container.decode(Double::class, forKey = CodingKeys.averageDifficulty)
        this.topTeamOpponents = container.decode(Int::class, forKey = CodingKeys.topTeamOpponents)
        this.difficultAwayGames = container.decode(Int::class, forKey = CodingKeys.difficultAwayGames)
        this.totalMatches = container.decode(Int::class, forKey = CodingKeys.totalMatches)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<FixtureAnalysis> {
        override fun init(from: Decoder): FixtureAnalysis = FixtureAnalysis(from = from)

        private fun CodingKeys(rawValue: String): FixtureAnalysis.CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class SeasonProjection: Codable {
    val projectedTotalPoints: Int
    val projectedValueIncrease: Int
    val confidence: Double // 0-1 scale

    constructor(projectedTotalPoints: Int, projectedValueIncrease: Int, confidence: Double) {
        this.projectedTotalPoints = projectedTotalPoints
        this.projectedValueIncrease = projectedValueIncrease
        this.confidence = confidence
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        projectedTotalPoints("projectedTotalPoints"),
        projectedValueIncrease("projectedValueIncrease"),
        confidence("confidence");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "projectedTotalPoints" -> CodingKeys.projectedTotalPoints
                    "projectedValueIncrease" -> CodingKeys.projectedValueIncrease
                    "confidence" -> CodingKeys.confidence
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(projectedTotalPoints, forKey = CodingKeys.projectedTotalPoints)
        container.encode(projectedValueIncrease, forKey = CodingKeys.projectedValueIncrease)
        container.encode(confidence, forKey = CodingKeys.confidence)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.projectedTotalPoints = container.decode(Int::class, forKey = CodingKeys.projectedTotalPoints)
        this.projectedValueIncrease = container.decode(Int::class, forKey = CodingKeys.projectedValueIncrease)
        this.confidence = container.decode(Double::class, forKey = CodingKeys.confidence)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<SeasonProjection> {
        override fun init(from: Decoder): SeasonProjection = SeasonProjection(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

// MARK: - Sale Recommendation Models

@androidx.annotation.Keep
enum class SaleRecommendationGoal(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, Codable, RawRepresentable<String> {
    balanceBudget("Budget ausgleichen"),
    improvePosition("Position verbessern"),
    maxValue("Maximalen Wert erzielen"),
    reduceRisk("Risiko reduzieren"),
    raiseCapital("Kapital aufbringen");

    val description: String
        get() {
            when (this) {
                SaleRecommendationGoal.balanceBudget -> return "Einen günstigen Spieler verkaufen und durch einen besseren ersetzen um Budget zu sparen"
                SaleRecommendationGoal.improvePosition -> return "Schwachen Spieler verkaufen und durch besseren ersetzen"
                SaleRecommendationGoal.maxValue -> return "Spieler mit höchstem Gewinn verkaufen"
                SaleRecommendationGoal.reduceRisk -> return "Riskante Spieler verkaufen"
                SaleRecommendationGoal.raiseCapital -> return "Schnell Geld für neue Spieler beschaffen"
            }
        }

    val icon: String
        get() {
            when (this) {
                SaleRecommendationGoal.balanceBudget -> return "balance.3"
                SaleRecommendationGoal.improvePosition -> return "arrow.up.circle"
                SaleRecommendationGoal.maxValue -> return "chart.line.uptrend.xyaxis"
                SaleRecommendationGoal.reduceRisk -> return "exclamationmark.shield"
                SaleRecommendationGoal.raiseCapital -> return "bitcoinsign.circle"
            }
        }

    override fun encode(to: Encoder) {
        val container = to.singleValueContainer()
        container.encode(rawValue)
    }

    @androidx.annotation.Keep
    companion object: CaseIterableCompanion<SaleRecommendationGoal>, DecodableCompanion<SaleRecommendationGoal> {
        override fun init(from: Decoder): SaleRecommendationGoal = SaleRecommendationGoal(from = from)

        fun init(rawValue: String): SaleRecommendationGoal? {
            return when (rawValue) {
                "Budget ausgleichen" -> SaleRecommendationGoal.balanceBudget
                "Position verbessern" -> SaleRecommendationGoal.improvePosition
                "Maximalen Wert erzielen" -> SaleRecommendationGoal.maxValue
                "Risiko reduzieren" -> SaleRecommendationGoal.reduceRisk
                "Kapital aufbringen" -> SaleRecommendationGoal.raiseCapital
                else -> null
            }
        }

        override val allCases: Array<SaleRecommendationGoal>
            get() = arrayOf(balanceBudget, improvePosition, maxValue, reduceRisk, raiseCapital)
    }
}

fun SaleRecommendationGoal(rawValue: String): SaleRecommendationGoal? = SaleRecommendationGoal.init(rawValue = rawValue)

fun SaleRecommendationGoal(from: Decoder): SaleRecommendationGoal {
    val container = from.singleValueContainer()
    val rawValue = container.decode(String::class)
    return SaleRecommendationGoal(rawValue = rawValue) ?: throw ErrorException(cause = NullPointerException())
}

class ReplacementSuggestion: Identifiable<UUID> {
    override val id: UUID
    val player: MarketPlayer
    val reasonForSale: String
    val budgetSavings: Int // Positive Zahl = wir sparen Geld
    val performanceGain: Double // Punkte pro Spiel Differenz
    val riskReduction: Double // 0-1, wie viel Risiko reduziert wird

    constructor(id: UUID = UUID(), player: MarketPlayer, reasonForSale: String, budgetSavings: Int, performanceGain: Double, riskReduction: Double) {
        this.id = id
        this.player = player
        this.reasonForSale = reasonForSale
        this.budgetSavings = budgetSavings
        this.performanceGain = performanceGain
        this.riskReduction = riskReduction
    }

    @androidx.annotation.Keep
    companion object {
    }
}

class SaleRecommendation: Identifiable<UUID> {
    override val id: UUID
    val playerToSell: Player
    val replacements: Array<ReplacementSuggestion>
    val goal: SaleRecommendationGoal
    val explanation: String
    val priority: TransferRecommendation.Priority

    constructor(id: UUID = UUID(), playerToSell: Player, replacements: Array<ReplacementSuggestion>, goal: SaleRecommendationGoal, explanation: String, priority: TransferRecommendation.Priority) {
        this.id = id
        this.playerToSell = playerToSell
        this.replacements = replacements.sref()
        this.goal = goal
        this.explanation = explanation
        this.priority = priority
    }

    val bestReplacement: ReplacementSuggestion?
        get() = replacements.first

    @androidx.annotation.Keep
    companion object {
    }
}

// MARK: - Lineup Models

class LineupSlot: Identifiable<UUID> {
    override val id: UUID
    val slotIndex: Int // 0-10 oder je nach Formation
    val positionType: Int // 1=TW, 2=ABW, 3=MF, 4=ST
    val ownedPlayerId: String? // ID des eigenen Spielers an dieser Position
    val recommendedMarketPlayerId: String? // ID des Markt-Spielers falls besser
    val slotScore: Double // Bewertung für diese Position (nur dieser Spieler)

    constructor(slotIndex: Int, positionType: Int, ownedPlayerId: String? = null, recommendedMarketPlayerId: String? = null, slotScore: Double) {
        this.id = UUID()
        this.slotIndex = slotIndex
        this.positionType = positionType
        this.ownedPlayerId = ownedPlayerId
        this.recommendedMarketPlayerId = recommendedMarketPlayerId
        this.slotScore = slotScore
    }

    val hasBetterMarketOption: Boolean
        get() = recommendedMarketPlayerId != null && ownedPlayerId != recommendedMarketPlayerId

    @androidx.annotation.Keep
    companion object {
    }
}

class OptimalLineupResult: Identifiable<UUID> {
    override val id: UUID
    val slots: Array<LineupSlot>
    val formationName: String
    val totalLineupScore: Double
    val isHybridWithMarketPlayers: Boolean
    val marketPlayersNeeded: Array<String> // IDs der Markt-Spieler die gekauft werden müssten
    val totalMarketCost: Int // Summe der Preise aller benötigten Markt-Spieler
    val averagePlayerScore: Double // Durchschnittliche Spielerbewertung

    constructor(slots: Array<LineupSlot>, formationName: String, totalLineupScore: Double, isHybridWithMarketPlayers: Boolean, marketPlayersNeeded: Array<String> = arrayOf(), totalMarketCost: Int = 0, averagePlayerScore: Double) {
        this.id = UUID()
        this.slots = slots.sref()
        this.formationName = formationName
        this.totalLineupScore = totalLineupScore
        this.isHybridWithMarketPlayers = isHybridWithMarketPlayers
        this.marketPlayersNeeded = marketPlayersNeeded.sref()
        this.totalMarketCost = totalMarketCost
        this.averagePlayerScore = averagePlayerScore
    }

    val ownedPlayerCount: Int
        get() {
            return slots.filter { it -> it.ownedPlayerId != null && !it.hasBetterMarketOption }.count
        }

    val marketPlayerCount: Int
        get() {
            return slots.filter { it -> it.hasBetterMarketOption }.count
        }

    @androidx.annotation.Keep
    companion object {
    }
}

class LineupComparison: Identifiable<UUID> {
    override val id: UUID
    val teamOnlyLineup: OptimalLineupResult
    val hybridLineup: OptimalLineupResult?

    constructor(teamOnlyLineup: OptimalLineupResult, hybridLineup: OptimalLineupResult? = null) {
        this.id = UUID()
        this.teamOnlyLineup = teamOnlyLineup
        this.hybridLineup = hybridLineup
    }

    val performanceGainWithHybrid: Double
        get() {
            val hybrid_0 = hybridLineup
            if (hybrid_0 == null) {
                return 0.0
            }
            return hybrid_0.averagePlayerScore - teamOnlyLineup.averagePlayerScore
        }

    val shouldShowHybrid: Boolean
        get() {
            return hybridLineup != null && (hybridLineup?.marketPlayerCount ?: 0) > 0
        }

    val totalInvestmentNeeded: Int
        get() {
            return hybridLineup?.totalMarketCost ?: 0
        }

    @androidx.annotation.Keep
    companion object {
    }
}

// MARK: - Team Analysis Models

@androidx.annotation.Keep
class TeamAnalysis: Codable {
    val weakPositions: Array<TeamAnalysis.Position>
    val strengths: Array<TeamAnalysis.Position>
    val budgetConstraints: BudgetAnalysis
    val recommendations: Array<PositionalRecommendation>

    @androidx.annotation.Keep
    enum class Position(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, Codable, RawRepresentable<String> {
        goalkeeper("TW"),
        defender("ABW"),
        midfielder("MF"),
        striker("ST");

        val positionNumber: Int
            get() {
                when (this) {
                    TeamAnalysis.Position.goalkeeper -> return 1
                    TeamAnalysis.Position.defender -> return 2
                    TeamAnalysis.Position.midfielder -> return 3
                    TeamAnalysis.Position.striker -> return 4
                }
            }

        override fun encode(to: Encoder) {
            val container = to.singleValueContainer()
            container.encode(rawValue)
        }

        @androidx.annotation.Keep
        companion object: CaseIterableCompanion<TeamAnalysis.Position>, DecodableCompanion<TeamAnalysis.Position> {
            override fun init(from: Decoder): TeamAnalysis.Position = Position(from = from)

            fun init(rawValue: String): TeamAnalysis.Position? {
                return when (rawValue) {
                    "TW" -> Position.goalkeeper
                    "ABW" -> Position.defender
                    "MF" -> Position.midfielder
                    "ST" -> Position.striker
                    else -> null
                }
            }

            override val allCases: Array<TeamAnalysis.Position>
                get() = arrayOf(goalkeeper, defender, midfielder, striker)
        }
    }

    constructor(weakPositions: Array<TeamAnalysis.Position>, strengths: Array<TeamAnalysis.Position>, budgetConstraints: BudgetAnalysis, recommendations: Array<PositionalRecommendation>) {
        this.weakPositions = weakPositions.sref()
        this.strengths = strengths.sref()
        this.budgetConstraints = budgetConstraints
        this.recommendations = recommendations.sref()
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        weakPositions("weakPositions"),
        strengths("strengths"),
        budgetConstraints("budgetConstraints"),
        recommendations("recommendations");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "weakPositions" -> CodingKeys.weakPositions
                    "strengths" -> CodingKeys.strengths
                    "budgetConstraints" -> CodingKeys.budgetConstraints
                    "recommendations" -> CodingKeys.recommendations
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(weakPositions, forKey = CodingKeys.weakPositions)
        container.encode(strengths, forKey = CodingKeys.strengths)
        container.encode(budgetConstraints, forKey = CodingKeys.budgetConstraints)
        container.encode(recommendations, forKey = CodingKeys.recommendations)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.weakPositions = container.decode(Array::class, elementType = TeamAnalysis.Position::class, forKey = CodingKeys.weakPositions)
        this.strengths = container.decode(Array::class, elementType = TeamAnalysis.Position::class, forKey = CodingKeys.strengths)
        this.budgetConstraints = container.decode(BudgetAnalysis::class, forKey = CodingKeys.budgetConstraints)
        this.recommendations = container.decode(Array::class, elementType = PositionalRecommendation::class, forKey = CodingKeys.recommendations)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<TeamAnalysis> {
        override fun init(from: Decoder): TeamAnalysis = TeamAnalysis(from = from)

        fun Position(from: Decoder): TeamAnalysis.Position {
            val container = from.singleValueContainer()
            val rawValue = container.decode(String::class)
            return Position(rawValue = rawValue) ?: throw ErrorException(cause = NullPointerException())
        }

        fun Position(rawValue: String): TeamAnalysis.Position? = Position.init(rawValue = rawValue)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class BudgetAnalysis: Codable {
    val availableBudget: Int
    val recommendedSpending: Int
    val maxAffordablePrice: Int
    val emergencyReserve: Int

    constructor(availableBudget: Int, recommendedSpending: Int, maxAffordablePrice: Int, emergencyReserve: Int) {
        this.availableBudget = availableBudget
        this.recommendedSpending = recommendedSpending
        this.maxAffordablePrice = maxAffordablePrice
        this.emergencyReserve = emergencyReserve
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        availableBudget("availableBudget"),
        recommendedSpending("recommendedSpending"),
        maxAffordablePrice("maxAffordablePrice"),
        emergencyReserve("emergencyReserve");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "availableBudget" -> CodingKeys.availableBudget
                    "recommendedSpending" -> CodingKeys.recommendedSpending
                    "maxAffordablePrice" -> CodingKeys.maxAffordablePrice
                    "emergencyReserve" -> CodingKeys.emergencyReserve
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(availableBudget, forKey = CodingKeys.availableBudget)
        container.encode(recommendedSpending, forKey = CodingKeys.recommendedSpending)
        container.encode(maxAffordablePrice, forKey = CodingKeys.maxAffordablePrice)
        container.encode(emergencyReserve, forKey = CodingKeys.emergencyReserve)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.availableBudget = container.decode(Int::class, forKey = CodingKeys.availableBudget)
        this.recommendedSpending = container.decode(Int::class, forKey = CodingKeys.recommendedSpending)
        this.maxAffordablePrice = container.decode(Int::class, forKey = CodingKeys.maxAffordablePrice)
        this.emergencyReserve = container.decode(Int::class, forKey = CodingKeys.emergencyReserve)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<BudgetAnalysis> {
        override fun init(from: Decoder): BudgetAnalysis = BudgetAnalysis(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class PositionalRecommendation: Identifiable<UUID>, Codable {
    override val id = UUID()
    val position: TeamAnalysis.Position
    val priority: Int // 1-5 scale
    val reasoning: String
    val suggestedPlayers: Array<String> // Player IDs

    constructor(position: TeamAnalysis.Position, priority: Int, reasoning: String, suggestedPlayers: Array<String>) {
        this.position = position
        this.priority = priority
        this.reasoning = reasoning
        this.suggestedPlayers = suggestedPlayers.sref()
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        id("id"),
        position("position"),
        priority("priority"),
        reasoning("reasoning"),
        suggestedPlayers("suggestedPlayers");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "id" -> CodingKeys.id
                    "position" -> CodingKeys.position
                    "priority" -> CodingKeys.priority
                    "reasoning" -> CodingKeys.reasoning
                    "suggestedPlayers" -> CodingKeys.suggestedPlayers
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(id, forKey = CodingKeys.id)
        container.encode(position, forKey = CodingKeys.position)
        container.encode(priority, forKey = CodingKeys.priority)
        container.encode(reasoning, forKey = CodingKeys.reasoning)
        container.encode(suggestedPlayers, forKey = CodingKeys.suggestedPlayers)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.position = container.decode(TeamAnalysis.Position::class, forKey = CodingKeys.position)
        this.priority = container.decode(Int::class, forKey = CodingKeys.priority)
        this.reasoning = container.decode(String::class, forKey = CodingKeys.reasoning)
        this.suggestedPlayers = container.decode(Array::class, elementType = String::class, forKey = CodingKeys.suggestedPlayers)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<PositionalRecommendation> {
        override fun init(from: Decoder): PositionalRecommendation = PositionalRecommendation(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}
