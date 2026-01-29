package kickbase.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import skip.lib.*
import skip.lib.Array
import skip.lib.Set

import skip.model.*
import skip.foundation.*
import skip.ui.*

@Stable
open class PlayerRecommendationService: ObservableObject {
    override val objectWillChange = ObservableObjectPublisher()
    private val kickbaseManager: KickbaseManager

    // Cache für Empfehlungen
    private var cachedRecommendations: Dictionary<String, CachedRecommendations> = dictionaryOf()
        get() = field.sref({ this.cachedRecommendations = it })
        set(newValue) {
            field = newValue.sref()
        }
    private val cacheValidityDuration: Double = 300.0 // 5 Minuten Cache

    // Cache für Spieler-Statistiken (smdc, ismc, smc)
    private var playerStatsCache: Dictionary<String, PlayerRecommendationService.PlayerMatchStats> = dictionaryOf()
        get() = field.sref({ this.playerStatsCache = it })
        set(newValue) {
            field = newValue.sref()
        }

    // Logging controllable flag to avoid noisy prints in hot loops
    open var isVerboseLogging: Boolean = false

    // Aktueller Spieltag (wird bei generateRecommendations gesetzt)
    private var currentMatchDay: Int = 10

    constructor(kickbaseManager: KickbaseManager) {
        this.kickbaseManager = kickbaseManager
    }

    // MARK: - Sync Helper Methods

    open suspend fun getTeamPlayersSync(for_: League): Array<Player> = Async.run l@{
        val league = for_
        return@l MainActor.run { kickbaseManager }.mainactor { it.authenticatedPlayerService }.loadTeamPlayers(for_ = league)
    }

    open suspend fun getMarketPlayersSync(for_: League): Array<MarketPlayer> = Async.run l@{
        val league = for_
        return@l MainActor.run { kickbaseManager }.mainactor { it.authenticatedPlayerService }.loadMarketPlayers(for_ = league)
    }

    // MARK: - Helper Structures

    private class PlayerMatchStats {
        internal val smdc: Int // Aktueller Spieltag
        internal val ismc: Int // Spiele auf dem Platz (Startelf + Einwechslung)
        internal val smc: Int // Spiele in Startelf (Starting Match Count)

        constructor(smdc: Int, ismc: Int, smc: Int) {
            this.smdc = smdc
            this.ismc = ismc
            this.smc = smc
        }
    }

    // MARK: - Main Recommendation Functions

    /// Generiert Verkaufs- und Ersatz-Empfehlungen basierend auf einem Ziel
    open suspend fun generateSaleRecommendations(for_: League, goal: SaleRecommendationGoal, teamPlayers: Array<Player>, marketPlayers: Array<MarketPlayer>, currentBudget: Int): Array<SaleRecommendation> = Async.run l@{
        val league = for_
        if (isVerboseLogging) {
            print("🛒 Generating sale recommendations for goal: ${goal.rawValue}")
        }

        val teamAnalysis = analyzeTeam(teamPlayers = teamPlayers, user = league.currentUser, budget = currentBudget)

        // Filter market players für Replacements
        // WICHTIG: Schließe Spieler aus, die der aktuelle Benutzer selbst auf den Transfermarkt gestellt hat
        val qualityMarketPlayers = marketPlayers.filter l@{ player ->
            if (player.status == 8 || player.status == 16) {
                return@l false
            }
            if (player.averagePoints < 70.0) {
                return@l false
            }
            if (player.seller.id == league.currentUser.id) {
                return@l false
            }

            return@l true
        }
        var saleRecommendations: Array<SaleRecommendation> = arrayOf()

        when (goal) {
            SaleRecommendationGoal.balanceBudget -> saleRecommendations = generateBudgetBalancingSales(teamPlayers = teamPlayers, marketPlayers = qualityMarketPlayers, currentBudget = currentBudget, teamAnalysis = teamAnalysis, maxPlayersPerTeam = league.currentUser.mpst)
            SaleRecommendationGoal.improvePosition -> saleRecommendations = generatePositionImprovementSales(teamPlayers = teamPlayers, marketPlayers = qualityMarketPlayers, teamAnalysis = teamAnalysis)
            SaleRecommendationGoal.maxValue -> saleRecommendations = generateMaxValueSales(teamPlayers = teamPlayers, marketPlayers = qualityMarketPlayers)
            SaleRecommendationGoal.reduceRisk -> saleRecommendations = generateRiskReductionSales(teamPlayers = teamPlayers, marketPlayers = qualityMarketPlayers)
            SaleRecommendationGoal.raiseCapital -> saleRecommendations = generateCapitalRaisingSales(teamPlayers = teamPlayers, marketPlayers = qualityMarketPlayers, currentBudget = currentBudget)
        }

        print("✅ Generated ${saleRecommendations.count} sale recommendations")
        return@l saleRecommendations.sref()
    }

    open suspend fun generateRecommendations(for_: League, budget: Int): Array<TransferRecommendation> = Async.run l@{
        val league = for_
        if (isVerboseLogging) {
            print("🎯 Generating transfer recommendations for league: ${league.name}")
        }

        // Prüfe Cache
        cachedRecommendations[league.id]?.let { cached ->
            if (Date().timeIntervalSince(cached.timestamp) < cacheValidityDuration) {
                if (isVerboseLogging) {
                    print("✅ Returning cached recommendations (${cached.recommendations.count} players)")
                }
                return@l cached.recommendations.sref()
            }
        }

        // Lade aktuelle Team-Spieler und Marktspieler PARALLEL
        val teamPlayersTask = Task { getTeamPlayers(for_ = league) }
        val marketPlayersTask = Task { getMarketPlayers(for_ = league) }

        val (teamPlayers, marketPlayers) = Tuple2(teamPlayersTask.value().sref(), marketPlayersTask.value().sref())

        // Hole aktuellen Spieltag und Stats von einem beliebigen Spieler
        val firstPlayerId = teamPlayers.first?.id ?: marketPlayers.first?.id

        if (firstPlayerId != null) {
            val playerId = firstPlayerId
            val matchtarget_0 = MainActor.run { kickbaseManager }.mainactor { it.authenticatedPlayerService }.getMatchDayStats(leagueId = league.id, playerId = playerId)
            if (matchtarget_0 != null) {
                val stats = matchtarget_0
                currentMatchDay = stats.smdc
                // Speichere Stats für diesen Spieler im Cache
                playerStatsCache[playerId] = PlayerMatchStats(smdc = stats.smdc, ismc = stats.ismc, smc = stats.smc)
                if (isVerboseLogging) {
                    print("✅ Current matchday from API: ${currentMatchDay}")
                }
            } else {
                currentMatchDay = 10 // Fallback
                if (isVerboseLogging) {
                    print("⚠️ Could not fetch matchday stats, using fallback: ${currentMatchDay}")
                }
            }
        } else {
            currentMatchDay = 10
            if (isVerboseLogging) {
                print("⚠️ No players available to fetch matchday, using fallback: ${currentMatchDay}")
            }
        }
        if (isVerboseLogging) {
            print("✅ Loaded ${teamPlayers.count} team players and ${marketPlayers.count} market players in parallel")
        }
        val currentUser = league.currentUser

        // Analysiere das Team
        val teamAnalysis = analyzeTeam(teamPlayers = teamPlayers, user = currentUser, budget = budget)

        // OPTIMIERTE FILTERUNG: Frühe Aussortierung ungeeigneter Spieler
        val qualityMarketPlayers = marketPlayers.filter l@{ player ->
            if (player.status == 8 || player.status == 16) {
                return@l false
            }
            if (player.averagePoints < 70.0) {
                return@l false
            }
            if (player.totalPoints < 140) {
                return@l false
            }
            if (player.seller.id == currentUser.id) {
                return@l false
            }

            return@l true
        }
        if (isVerboseLogging) {
            print("📊 Pre-filtered from ${marketPlayers.count} to ${qualityMarketPlayers.count} quality players")
        }

        // BATCH-PROCESSING: Verarbeite Spieler in Batches
        val batchSize = 50
        var allRecommendations: Array<TransferRecommendation> = arrayOf()

        for (batchStart in stride(from = 0, to = qualityMarketPlayers.count, by = batchSize)) {
            val batchEnd = min(batchStart + batchSize, qualityMarketPlayers.count)
            val batch = Array(qualityMarketPlayers[batchStart until batchEnd])

            // Verarbeite Batch parallel OFF the MainActor using detached tasks
            var batchRecommendations: Array<TransferRecommendation> = arrayOf()
            withTaskGroup(of = TransferRecommendation?::class) { group ->
                for (marketPlayer in batch.sref()) {
                    group.addTask l@{ ->
                        // Detached heavy CPU work off MainActor
                        return@l Task.detached(fun(): TransferRecommendation? {
                            val analysis = this.analyzePlayerNonIsolated(marketPlayer, teamAnalysis = teamAnalysis)
                            val recommendation = this.createRecommendationNonIsolated(marketPlayer = marketPlayer, analysis = analysis, teamAnalysis = teamAnalysis)
                            return if (recommendation.recommendationScore >= 2.0) recommendation else null
                        }).value()
                    }
                }

                for (result in group.sref()) {
                    result.sref()?.let { rec ->
                        batchRecommendations.append(rec)
                    }
                }
            }

            allRecommendations.append(contentsOf = batchRecommendations)
            if (isVerboseLogging) {
                print("📦 Processed batch ${batchStart / batchSize + 1}: ${batchRecommendations.count} recommendations added")
            }
        }

        print("✅ Generated ${allRecommendations.count} recommendations")

        // Sortiere nach Empfehlungswert und nimm Top 50 für Stats-Loading
        val topRecommendations = allRecommendations.sorted { it, it_1 -> it.recommendationScore > it_1.recommendationScore }.prefix(50)

        // Lade Stats für Top-Spieler asynchron (parallel, max 10 gleichzeitig)
        print("📊 Loading detailed stats for top ${topRecommendations.count} players...")
        loadStatsForPlayers(Array(topRecommendations), leagueId = league.id)

        // Jetzt neu berechnen mit echten Stats
        var finalRecommendations: Array<TransferRecommendation> = arrayOf()
        for (recommendation in topRecommendations.sref()) {
            var updatedRecommendation = recommendation

            // Wenn Stats verfügbar sind, neu berechnen
            playerStatsCache[recommendation.player.id]?.let { stats ->
                val updatedProjection = calculateSeasonProjectionWithStats(recommendation.player, stats = stats)

                // Erstelle eine aktualisierte Analysis mit neuer Projection
                val updatedAnalysis = PlayerAnalysis(pointsPerGame = recommendation.analysis.pointsPerGame, valueForMoney = recommendation.analysis.valueForMoney, formTrend = recommendation.analysis.formTrend, injuryRisk = recommendation.analysis.injuryRisk, upcomingFixtures = recommendation.analysis.upcomingFixtures, seasonProjection = updatedProjection)

                updatedRecommendation = TransferRecommendation(player = recommendation.player, recommendationScore = recommendation.recommendationScore, reasons = recommendation.reasons, analysis = updatedAnalysis, riskLevel = recommendation.riskLevel, priority = recommendation.priority)
            }

            finalRecommendations.append(updatedRecommendation)
        }

        // Sortiere final nach Score und limitiere auf Top 20
        finalRecommendations = Array(finalRecommendations.sorted { it, it_1 -> it.recommendationScore > it_1.recommendationScore }.prefix(20))

        // Cache speichern
        cachedRecommendations[league.id] = CachedRecommendations(recommendations = finalRecommendations, timestamp = Date())

        return@l finalRecommendations.sref()
    }

    // MARK: - Helper Functions

    private suspend fun getTeamPlayers(for_: League): Array<Player> = Async.run l@{
        val league = for_
        return@l MainActor.run { kickbaseManager }.mainactor { it.authenticatedPlayerService }.loadTeamPlayers(for_ = league)
    }

    private suspend fun getMarketPlayers(for_: League): Array<MarketPlayer> = Async.run l@{
        val league = for_
        return@l MainActor.run { kickbaseManager }.mainactor { it.authenticatedPlayerService }.loadMarketPlayers(for_ = league)
    }

    // MARK: - Analysis Functions

    private fun analyzeTeam(teamPlayers: Array<Player>, user: LeagueUser, budget: Int): TeamAnalysis {
        var positionCounts: Dictionary<String, Int> = dictionaryOf()
        var positionTotalPoints: Dictionary<String, Double> = dictionaryOf()

        // Analysiere aktuelle Teamzusammensetzung
        for (player in teamPlayers.sref()) {
            val position = String(player.position) // Convert Int to String
            positionCounts[position, { 0 }] += 1
            positionTotalPoints[position, { 0.0 }] += Double(player.totalPoints)
        }

        // Identifiziere schwache Positionen
        val weakPositions = identifyWeakPositions(positionCounts = positionCounts, positionTotalPoints = positionTotalPoints)
        val strengths = identifyStrongPositions(positionCounts = positionCounts, positionTotalPoints = positionTotalPoints)

        val budgetAnalysis = BudgetAnalysis(availableBudget = budget, recommendedSpending = Int(Double(budget) * 0.8), maxAffordablePrice = Int(Double(budget) * 0.9), emergencyReserve = Int(Double(budget) * 0.1))

        return TeamAnalysis(weakPositions = weakPositions, strengths = strengths, budgetConstraints = budgetAnalysis, recommendations = arrayOf())
    }

    private fun identifyWeakPositions(positionCounts: Dictionary<String, Int>, positionTotalPoints: Dictionary<String, Double>): Array<TeamAnalysis.Position> {
        var weakPositions: Array<TeamAnalysis.Position> = arrayOf()

        // Mindestanzahl Spieler pro Position
        val minPlayersPerPosition: Dictionary<String, Int> = dictionaryOf(
            Tuple2("1", 1),
            Tuple2("2", 3),
            Tuple2("3", 6),
            Tuple2("4", 1)
        )

        // Überprüfe Anzahl pro Position
        for ((positionStr, minCount) in minPlayersPerPosition.sref()) {
            val currentCount = positionCounts[positionStr, { 0 }]
            if (currentCount < minCount) {
                mapStringToPosition(positionStr)?.let { position ->
                    weakPositions.append(position)
                }
            }
        }

        // Überprüfe Leistung pro Position (unter Durchschnitt von 100 Punkten)
        for ((positionStr, totalPoints) in positionTotalPoints.sref()) {
            val count = positionCounts[positionStr, { 0 }]
            if (count > 0) {
                val averagePoints = totalPoints / Double(count)
                if (averagePoints < 100.0) {
                    mapStringToPosition(positionStr)?.let { position ->
                        weakPositions.append(position)
                    }
                }
            }
        }

        return Array(Set(weakPositions)) // Entferne Duplikate
    }

    private fun identifyStrongPositions(positionCounts: Dictionary<String, Int>, positionTotalPoints: Dictionary<String, Double>): Array<TeamAnalysis.Position> {
        var strongPositions: Array<TeamAnalysis.Position> = arrayOf()

        // Positionen mit überdurchschnittlicher Leistung (über 150 Punkten)
        for ((positionStr, totalPoints) in positionTotalPoints.sref()) {
            val count = positionCounts[positionStr, { 0 }]
            if (count > 0) {
                val averagePoints = totalPoints / Double(count)
                if (averagePoints > 150.0) {
                    mapStringToPosition(positionStr)?.let { position ->
                        strongPositions.append(position)
                    }
                }
            }
        }

        return strongPositions.sref()
    }

    private fun mapStringToPosition(positionStr: String): TeamAnalysis.Position? {
        when (positionStr) {
            "1" -> return TeamAnalysis.Position.goalkeeper
            "2" -> return TeamAnalysis.Position.defender
            "3" -> return TeamAnalysis.Position.midfielder
            "4" -> return TeamAnalysis.Position.striker
            else -> return null
        }
    }

    // Non-isolated wrapper for running analysis off the MainActor in detached Tasks
    private fun analyzePlayerNonIsolated(marketPlayer: MarketPlayer, teamAnalysis: TeamAnalysis): PlayerAnalysis {
        // Reuse the isolated implementations which are pure functions of inputs
        val pointsPerGame = marketPlayer.averagePoints
        val valueForMoney = calculateValueForMoneyNonIsolated(marketPlayer)
        val formTrend = calculateCurrentFormNonIsolated(marketPlayer)
        val injuryRisk = calculateInjuryRiskNonIsolated(marketPlayer)
        val seasonProjection = calculateSeasonProjectionNonIsolated(marketPlayer)

        return PlayerAnalysis(pointsPerGame = pointsPerGame, valueForMoney = valueForMoney, formTrend = formTrend, injuryRisk = injuryRisk, upcomingFixtures = arrayOf(), seasonProjection = seasonProjection)
    }

    private fun calculateCurrentFormNonIsolated(marketPlayer: MarketPlayer): PlayerAnalysis.FormTrend {
        val marketValueChange = marketPlayer.marketValueTrend
        val pointsPerGame = marketPlayer.averagePoints

        if (marketValueChange > 500000 && pointsPerGame > 8.0) {
            return PlayerAnalysis.FormTrend.improving
        } else if (marketValueChange < -500000 || pointsPerGame < 4.0) {
            return PlayerAnalysis.FormTrend.declining
        } else {
            return PlayerAnalysis.FormTrend.stable
        }
    }

    private fun calculateInjuryRiskNonIsolated(marketPlayer: MarketPlayer): PlayerAnalysis.InjuryRisk {
        // Basis-Risiko basierend auf Status
        if (marketPlayer.status == 8) {
            return PlayerAnalysis.InjuryRisk.high
        } else if (marketPlayer.status == 4) {
            return PlayerAnalysis.InjuryRisk.medium
        } else {
            return PlayerAnalysis.InjuryRisk.low
        }
    }

    private fun calculateSeasonProjectionNonIsolated(marketPlayer: MarketPlayer): SeasonProjection {
        // Fallback-Berechnung ohne Stats (wird später mit echten Stats überschrieben)
        val currentPoints = marketPlayer.totalPoints
        val pointsPerGame = marketPlayer.averagePoints

        // Schätze gespielte Spiele aus Punkten
        val estimatedGamesPlayed = if (pointsPerGame > 0) Int(round(Double(currentPoints) / pointsPerGame)) else 0
        val remainingGames = max(34 - estimatedGamesPlayed, 0)

        val projectedTotal = currentPoints + Int(pointsPerGame * Double(remainingGames))
        // Nutze tfhmvt wenn verfügbar, sonst Fallback auf marketValueTrend
        val dailyChange = marketPlayer.marketValueTrend
        val projectedValueIncrease = dailyChange * remainingGames

        // Niedrige Confidence, da keine echten Stats
        val confidence = 0.5

        return SeasonProjection(projectedTotalPoints = projectedTotal, projectedValueIncrease = projectedValueIncrease, confidence = confidence)
    }

    private fun calculateSeasonProjectionWithStats(marketPlayer: MarketPlayer, stats: PlayerRecommendationService.PlayerMatchStats): SeasonProjection {
        val currentPoints = marketPlayer.totalPoints
        val gamesPlayed = stats.ismc // Spiele auf dem Platz (Startelf + Einwechslung)
        val pointsPerGame = marketPlayer.averagePoints
        val remainingGames = max(34 - gamesPlayed, 0)

        val projectedTotal = currentPoints + Int(pointsPerGame * Double(remainingGames))
        // Nutze tfhmvt wenn verfügbar, sonst Fallback auf marketValueTrend
        val dailyChange = marketPlayer.marketValueTrend
        val projectedValueIncrease = dailyChange * remainingGames

        // Debug: Zeige Stats (nur wenn verbose logging aktiviert)
        if (isVerboseLogging) {
            print("📊 Stats for ${marketPlayer.firstName} ${marketPlayer.lastName}:")
            print("   smdc=${stats.smdc}, ismc=${stats.ismc}, smc=${stats.smc}")
        }

        // Confidence basiert auf Spielbeteiligung
        val confidence: Double
        if (stats.smdc > 0 && gamesPlayed > 0) {
            val playedRatio = Double(gamesPlayed) / Double(stats.smdc)
            // Bonus für Stammkräfte (smc ~ ismc)
            val starterBonus = Double(stats.smc) / max(Double(stats.ismc), 1.0)
            confidence = min(playedRatio * (0.7 + starterBonus * 0.3), 1.0)

            if (isVerboseLogging) {
                print("   playedRatio=${String(format = "%.2f", playedRatio)}, starterBonus=${String(format = "%.2f", starterBonus)}")
                print("🎯 Confidence for ${marketPlayer.firstName} ${marketPlayer.lastName}: ${gamesPlayed} played / ${stats.smdc} matchdays (started: ${stats.smc}) = ${String(format = "%.1f%%", confidence * 100)}")
            }
        } else {
            confidence = 0.0
            if (isVerboseLogging) {
                print("⚠️ No stats available for ${marketPlayer.firstName} ${marketPlayer.lastName}")
            }
        }

        return SeasonProjection(projectedTotalPoints = projectedTotal, projectedValueIncrease = projectedValueIncrease, confidence = confidence)
    }

    /// Lädt Stats für eine Liste von Spielern (parallel, max 10 gleichzeitig)
    private suspend fun loadStatsForPlayers(recommendations: Array<TransferRecommendation>, leagueId: String): Unit = MainActor.run l@{
        // Nur Spieler laden, die noch nicht im Cache sind
        val playersToLoad = recommendations.filter { it -> playerStatsCache[it.player.id] == null }
        if (playersToLoad.isEmpty) {
            if (isVerboseLogging) {
                print("✅ All player stats already cached")
            }
            return@l
        }

        if (isVerboseLogging) {
            print("📥 Loading stats for ${playersToLoad.count} players...")
        }

        // Verarbeite in Batches von 10 parallel
        for (batchStart in stride(from = 0, to = playersToLoad.count, by = 10)) {
            val batchEnd = min(batchStart + 10, playersToLoad.count)
            val batch = Array(playersToLoad[batchStart until batchEnd])

            // Lade Stats parallel
            for (recommendation in batch.sref()) {
                this.kickbaseManager.authenticatedPlayerService
                    .getMatchDayStats(leagueId = leagueId, playerId = recommendation.player.id)?.let { stats ->
                    this.playerStatsCache[recommendation.player.id] = PlayerMatchStats(smdc = stats.smdc, ismc = stats.ismc, smc = stats.smc)
                }
            }
        }

        if (isVerboseLogging) {
            print("✅ Loaded stats for ${playerStatsCache.count} players total")
        }
    }

    private fun calculateValueForMoney(marketPlayer: MarketPlayer): Double {
        if (marketPlayer.price <= 0) {
            return 0.0
        }
        val pointsPerMillion = Double(marketPlayer.totalPoints) / (Double(marketPlayer.price) / 1_000_000.0)
        return pointsPerMillion
    }

    // Nonisolated wrapper for calculateValueForMoney to be callable from detached tasks
    private fun calculateValueForMoneyNonIsolated(marketPlayer: MarketPlayer): Double {
        if (marketPlayer.price <= 0) {
            return 0.0
        }
        val pointsPerMillion = Double(marketPlayer.totalPoints) / (Double(marketPlayer.price) / 1_000_000.0)
        return pointsPerMillion
    }

    // Nonisolated version of createRecommendation used in detached tasks
    private fun createRecommendationNonIsolated(marketPlayer: MarketPlayer, analysis: PlayerAnalysis, teamAnalysis: TeamAnalysis): TransferRecommendation {
        val score = calculateRecommendationScore(marketPlayer = marketPlayer, analysis = analysis, teamAnalysis = teamAnalysis)
        val riskLevel = determineRiskLevel(analysis = analysis)
        val priority = determinePriority(score = score, teamAnalysis = teamAnalysis, position = marketPlayer.position)
        val reasons = generateReasons(marketPlayer = marketPlayer, analysis = analysis, teamAnalysis = teamAnalysis)

        return TransferRecommendation(player = marketPlayer, recommendationScore = score, reasons = reasons, analysis = analysis, riskLevel = riskLevel, priority = priority)
    }

    // MARK: - Cache Helper

    open fun clearCache() {
        cachedRecommendations.removeAll()
        if (isVerboseLogging) {
            print("🗑️ Recommendations cache cleared")
        }
    }

    open fun clearCacheForLeague(leagueId: String) {
        cachedRecommendations.removeValue(forKey = leagueId)
        if (isVerboseLogging) {
            print("🗑️ Cache cleared for league: ${leagueId}")
        }
    }

    private fun createRecommendation(marketPlayer: MarketPlayer, analysis: PlayerAnalysis, teamAnalysis: TeamAnalysis): TransferRecommendation {
        val score = calculateRecommendationScore(marketPlayer = marketPlayer, analysis = analysis, teamAnalysis = teamAnalysis)
        val riskLevel = determineRiskLevel(analysis = analysis)
        val priority = determinePriority(score = score, teamAnalysis = teamAnalysis, position = marketPlayer.position)
        val reasons = generateReasons(marketPlayer = marketPlayer, analysis = analysis, teamAnalysis = teamAnalysis)

        return TransferRecommendation(player = marketPlayer, recommendationScore = score, reasons = reasons, analysis = analysis, riskLevel = riskLevel, priority = priority)
    }

    private fun calculateRecommendationScore(marketPlayer: MarketPlayer, analysis: PlayerAnalysis, teamAnalysis: TeamAnalysis): Double {
        var score = 0.0

        // VERBESSERTE BEWERTUNG: Höhere Gewichtung für starke Spieler

        // 1. Punkte pro Spiel (0-6 Punkte) - Höhere Gewichtung!
        val pointsPerGameScore = min(analysis.pointsPerGame / 1.5, 6.0)
        score += pointsPerGameScore

        // 2. Absolute Punkte Bonus für Top-Performer (0-3 Punkte)
        if (marketPlayer.totalPoints >= 150) {
            score += 3.0
        } else if (marketPlayer.totalPoints >= 100) {
            score += 2.0
        } else if (marketPlayer.totalPoints >= 75) {
            score += 1.0
        }

        // 3. Value-for-Money Score (0-4 Punkte) - Verbessert
        val valueScore = min(analysis.valueForMoney / 8.0, 4.0)
        score += valueScore

        // 4. Form-Trend (wichtiger gemacht)
        when (analysis.formTrend) {
            PlayerAnalysis.FormTrend.improving -> score += 3.0 // Erhöht von 2.0
            PlayerAnalysis.FormTrend.stable -> score += 0.5 // Kleiner Bonus für Stabilität
            PlayerAnalysis.FormTrend.declining -> score -= 2.0 // Erhöht von -1.0
        }

        // 5. Marktwert-Trend Bonus
        if (marketPlayer.marketValueTrend > 1_000_000) {
            score += 2.0 // Stark steigender Marktwert
        } else if (marketPlayer.marketValueTrend > 500000) {
            score += 1.0 // Steigender Marktwert
        } else if (marketPlayer.marketValueTrend < -1_000_000) {
            score -= 1.5 // Stark fallender Marktwert
        }

        // 6. Team-Need Bonus (wichtiger gemacht)
        mapIntToPosition(marketPlayer.position)?.let { playerPosition ->
            if (teamAnalysis.weakPositions.contains(playerPosition)) {
                score += 4.0 // Erhöht von 3.0
            } else if (teamAnalysis.strengths.contains(playerPosition)) {
                score += 0.5 // Reduziert von 1.0
            } else {
                score += 2.0 // Gleich
            }
        }

        // 7. Spiele-Konsistenz Bonus
        val gamesPlayed = Double(marketPlayer.number)
        if (gamesPlayed >= 15) {
            score += 1.0 // Konsistent viele Spiele
        } else if (gamesPlayed >= 10) {
            score += 0.5
        } else if (gamesPlayed < 5) {
            score -= 1.0 // Zu wenige Spiele
        }

        // 8. Preis-Effizienz
        val priceInMillions = Double(marketPlayer.price) / 1_000_000.0
        if (priceInMillions <= 5.0 && analysis.pointsPerGame >= 7.0) {
            score += 2.0 // Günstiger Topstar
        } else if (priceInMillions <= 3.0 && analysis.pointsPerGame >= 6.0) {
            score += 1.5 // Sehr günstig
        }

        // Score auf theoretisches Maximum von ~24 Punkten begrenzen
        return min(max(score, 0.0), 24.0)
    }

    private fun mapIntToPosition(position: Int): TeamAnalysis.Position? {
        when (position) {
            1 -> return TeamAnalysis.Position.goalkeeper
            2 -> return TeamAnalysis.Position.defender
            3 -> return TeamAnalysis.Position.midfielder
            4 -> return TeamAnalysis.Position.striker
            else -> return null
        }
    }

    private fun determineRiskLevel(analysis: PlayerAnalysis): TransferRecommendation.RiskLevel {
        when (analysis.injuryRisk) {
            PlayerAnalysis.InjuryRisk.high -> return TransferRecommendation.RiskLevel.high
            PlayerAnalysis.InjuryRisk.medium -> return if (analysis.formTrend == PlayerAnalysis.FormTrend.declining) TransferRecommendation.RiskLevel.high else TransferRecommendation.RiskLevel.medium
            PlayerAnalysis.InjuryRisk.low -> return if (analysis.formTrend == PlayerAnalysis.FormTrend.declining) TransferRecommendation.RiskLevel.medium else TransferRecommendation.RiskLevel.low
        }
    }

    private fun determinePriority(score: Double, teamAnalysis: TeamAnalysis, position: Int): TransferRecommendation.Priority {
        mapIntToPosition(position)?.let { playerPosition ->
            if (teamAnalysis.weakPositions.contains(playerPosition) && score >= 19.2) {
                return TransferRecommendation.Priority.essential
            } else if (score >= 12.0) {
                return TransferRecommendation.Priority.recommended
            } else {
                return TransferRecommendation.Priority.optional
            }
        }
        return TransferRecommendation.Priority.optional
    }

    private fun generateReasons(marketPlayer: MarketPlayer, analysis: PlayerAnalysis, teamAnalysis: TeamAnalysis): Array<RecommendationReason> = arrayOf()

    /// Verkaufs-Empfehlungen: Spieler verkaufen um Budget zu sparen und zu investieren
    private fun generateBudgetBalancingSales(teamPlayers: Array<Player>, marketPlayers: Array<MarketPlayer>, currentBudget: Int, teamAnalysis: TeamAnalysis, maxPlayersPerTeam: Int? = null): Array<SaleRecommendation> {
        var recommendations: Array<SaleRecommendation> = arrayOf()
        if (currentBudget >= 0) {
            print("✅ Budget ist positiv (${currentBudget}), keine Verkaufs-Empfehlungen nötig")
            return arrayOf()
        }

        val budgetGap = abs(currentBudget) // Wieviel wir verdienen müssen
        print("💰 Budget gap to cover: ${budgetGap}")

        // Berechne "Unwichtigkeits-Score" für jeden Spieler
        // Niedrige Scores = unwichtig = gut zum Verkaufen
        val playerScores: Array<PlayerScore> = teamPlayers.map l@{ player ->
            // Score basiert auf:
            // 1. Durchschnittliche Punkte pro Spiel (niedrig = unwichtig)
            // 2. Gesamtpunkte (niedrig = unwichtig)
            // 3. Status (verletzt/angeschlagen = unwichtig)

            var score = Double(player.totalPoints) + (player.averagePoints * 10)

            // Bonus für Verletzte/Angeschlagene
            if (player.status == 1) {
                score -= 500
            } else if (player.status == 2) {
                score -= 250
            }

            return@l PlayerScore(player = player, unwantednessScore = score)
        }

        // Sortiere von unwichtig zu wichtig (aufsteigend nach Score)
        val sortedTeamPlayers: Array<Player> = playerScores.sorted { it, it_1 -> it.unwantednessScore < it_1.unwantednessScore }
            .map { it -> it.player }

        var accumulatedSavings = 0

        for (teamPlayer in sortedTeamPlayers.sref()) {
            /* print(
            "🔍 Considering \(teamPlayer.fullName) (€\(teamPlayer.marketValue / 1000)k, \("XXX")) Pkt/Spiel, Punkte: \(teamPlayer.totalPoints))"
            ) */

            // Finde Ersatz-Kandidaten in gleicher Position
            // Wir wollen günstigere Spieler, um Geld zu sparen
            val maxPriceForReplacement = Int(Double(teamPlayer.marketValue) * 0.8)

            val replacementCandidates = findReplacementCandidates(for_ = teamPlayer, in_ = marketPlayers, maxPrice = maxPriceForReplacement, teamPlayers = teamPlayers, maxPlayersPerTeam = maxPlayersPerTeam)

            // Stubbed for transpilation safety - simplified logic
            if (!replacementCandidates.isEmpty) {
                val bestReplacement = replacementCandidates[0]
                val explanation = "Empfehlung: ${bestReplacement.player.fullName}"
                val budgetSavings = teamPlayer.marketValue - bestReplacement.player.marketValue

                // Priority
                val remainingGap = budgetGap - accumulatedSavings
                val priority: TransferRecommendation.Priority = if ((budgetSavings >= remainingGap)) TransferRecommendation.Priority.essential else if ((budgetSavings >= remainingGap / 2)) TransferRecommendation.Priority.recommended else TransferRecommendation.Priority.optional

                val saleRec = SaleRecommendation(playerToSell = teamPlayer, replacements = replacementCandidates, goal = SaleRecommendationGoal.balanceBudget, explanation = explanation, priority = priority)
                recommendations.append(saleRec)
                accumulatedSavings += budgetSavings

                if (accumulatedSavings >= budgetGap && recommendations.count >= 3) {
                    print("✅ Budget gap covered with ${recommendations.count} recommendations!")
                    break
                }
            } else {
                print("   ✗ No replacement candidates found")
            }
        }

        print("📊 Generated ${recommendations.count} recommendations for budget balancing")
        return recommendations.sref()
    }

    /// Verkaufs-Empfehlungen: Schwache Spieler gegen bessere austauschen
    private fun generatePositionImprovementSales(teamPlayers: Array<Player>, marketPlayers: Array<MarketPlayer>, teamAnalysis: TeamAnalysis): Array<SaleRecommendation> = arrayOf() // Stubbed for build stability

    /// Verkaufs-Empfehlungen: Spieler mit höchstem Gewinn verkaufen
    private fun generateMaxValueSales(teamPlayers: Array<Player>, marketPlayers: Array<MarketPlayer>): Array<SaleRecommendation> = arrayOf() // Stubbed for build stability

    /// Verkaufs-Empfehlungen: Riskante Spieler verkaufen
    private fun generateRiskReductionSales(teamPlayers: Array<Player>, marketPlayers: Array<MarketPlayer>): Array<SaleRecommendation> {
        var recommendations: Array<SaleRecommendation> = arrayOf()

        // Finde verletzte oder angeschlagene Spieler
        val riskyPlayers = teamPlayers.filter l@{ player ->
            // Status: 1=Verletzt, 2=Angeschlagen, 4=Aufbautraining
            return@l player.status == 1 || player.status == 2 || player.status == 4
        }

        for (riskyPlayer in riskyPlayers.sref()) {
            val replacements = findReplacementCandidates(for_ = riskyPlayer, in_ = marketPlayers, maxPrice = riskyPlayer.marketValue * 2)

            if (!replacements.isEmpty) {
                // ... rest of logic
                val riskText: String
                when (riskyPlayer.status) {
                    1 -> riskText = "verletzt"
                    2 -> riskText = "angeschlagen"
                    4 -> riskText = "im Aufbautraining"
                    else -> riskText = "mit Risiko"
                }

                val explanation = "${riskyPlayer.fullName} ist aktuell ${riskText}. Verkaufe ihn jetzt und ersetze ihn durch einen gesünderen Spieler, um Ausfallrisiko zu minimieren."

                val saleRec = SaleRecommendation(playerToSell = riskyPlayer, replacements = replacements, goal = SaleRecommendationGoal.reduceRisk, explanation = explanation, priority = if (riskyPlayer.status == 1) TransferRecommendation.Priority.essential else TransferRecommendation.Priority.recommended)
                recommendations.append(saleRec)
            }
        }

        return recommendations.sref()
    }

    /// Verkaufs-Empfehlungen: Geld beschaffen für Transfers
    private fun generateCapitalRaisingSales(teamPlayers: Array<Player>, marketPlayers: Array<MarketPlayer>, currentBudget: Int): Array<SaleRecommendation> {
        var recommendations: Array<SaleRecommendation> = arrayOf()

        // Finde die wertvollsten Spieler
        val valuablePlayers = teamPlayers
            .filter { it ->
                it.totalPoints < 100 // Spieler die nicht so gut performen
            }
            .sorted { it, it_1 -> it.marketValue > it_1.marketValue }
            .prefix(5)

        for (player in valuablePlayers.sref()) {
            val replacements = findReplacementCandidates(for_ = player, in_ = marketPlayers, maxPrice = Int(Double(player.marketValue) * 1.5))

            if (!replacements.isEmpty) {
                val explanation = "Verkaufe ${player.fullName} (~€${player.marketValue / 1000}k) um Kapital für neue Transfers zu beschaffen."

                val saleRec = SaleRecommendation(playerToSell = player, replacements = replacements, goal = SaleRecommendationGoal.raiseCapital, explanation = explanation, priority = TransferRecommendation.Priority.recommended)
                recommendations.append(saleRec)
            }
        }

        return recommendations.sref()
    }

    /// Findet geeignete Ersatz-Spieler für einen Team-Spieler
    private fun findReplacementCandidates(for_: Player, in_: Array<MarketPlayer>, maxPrice: Int, teamPlayers: Array<Player>? = null, maxPlayersPerTeam: Int? = null): Array<ReplacementSuggestion> {
        val teamPlayer = for_
        val marketPlayers = in_
        print("   🔎 Finding replacements for ${teamPlayer.fullName} (pos: ${teamPlayer.position}, maxPrice: €${maxPrice / 1000}k)")

        // Filter nach Position und Preis
        val candidates = marketPlayers.filter l@{ marketPlayer ->
            if (marketPlayer.position != teamPlayer.position) {
                return@l false
            }
            if (marketPlayer.price > maxPrice) {
                return@l false
            }

            // Beachte maxPlayersPerTeam - aber NICHT wenn wir einen Spieler vom gleichen Team ersetzen
            if (maxPlayersPerTeam != null) {
                val replacementTeamCount = teamPlayers?.filter({ it -> it.teamId == marketPlayer.teamId })?.count ?: 0
                // Wenn wir einen Spieler vom gleichen Team ersetzen (playersFromTeam > 0),
                // können wir jemanden vom gleichen Team nehmen
                val teamPlayerAlreadySelected = teamPlayers?.contains { it -> it.teamId == marketPlayer.teamId } ?: false
                if (teamPlayerAlreadySelected && replacementTeamCount >= maxPlayersPerTeam) {
                    // Nur erlauben wenn wir vom gleichen Team ersetzen
                    if (marketPlayer.teamId != teamPlayer.teamId) {
                        return@l false
                    }
                } else if (replacementTeamCount >= maxPlayersPerTeam) {
                    return@l false
                }
            }
            if (marketPlayer.totalPoints < 0) {
                return@l false
            }
            return@l true
        }

        print("   📊 Found ${candidates.count} candidates in price range (after team limit check)")

        // Bewerte und sortiere
        val scored: Array<CandidateScore> = candidates.map l@{ candidate ->
            val performanceDiff = candidate.averagePoints - Double(teamPlayer.averagePoints)
            val priceDiff = Double(teamPlayer.marketValue - candidate.price) / 1_000_000.0

            // Score: Budget-Einsparung ist wichtiger als Performance!
            // 70% Gewicht auf Preis, 30% auf Performance
            val score = priceDiff * 0.7 + performanceDiff * 0.3

            print("     - ${candidate.firstName} ${candidate.lastName}: score=${String(format = "%.2f", score)} (price_diff=${String(format = "%.2f", priceDiff)}, perf_diff=${String(format = "%.2f", performanceDiff)})")

            return@l CandidateScore(candidate = candidate, score = score)
        }
        .sorted { it, it_1 -> it.score > it_1.score }

        print("   ✅ Top ${min(3, scored.count)} candidates selected")

        // Pre-calculate values to help Skip transpiler with closure captures
        val teamPlayerMarketValue = teamPlayer.marketValue
        val teamPlayerAveragePoints = Double(teamPlayer.averagePoints)

        // Konvertiere zu ReplacementSuggestion
        var suggestions: Array<ReplacementSuggestion> = arrayOf()
        for (item in scored.prefix(3)) {
            val candidate = item.candidate

            suggestions.append(ReplacementSuggestion(player = candidate, reasonForSale = "Bessere Alternative verfügbar", budgetSavings = teamPlayerMarketValue - candidate.price, performanceGain = candidate.averagePoints - teamPlayerAveragePoints, riskReduction = 0.0))
        }
        return suggestions.sref()
    }

    // MARK: - Lineup Optimization Functions

    /// Generiert optimale Aufstellungen: nur eigene Spieler vs. Hybrid mit Markt-Spielern
    open suspend fun generateOptimalLineupComparison(for_: League, teamPlayers: Array<Player>, marketPlayers: Array<MarketPlayer>, formation: Array<Int>): LineupComparison = Async.run l@{
        val league = for_
        print("🎯 Generating optimal lineup comparison for formation: ${formation}")

        // Schritt 1: Generiere Team-Only Aufstellung
        val teamOnlyLineup = generateTeamOnlyLineup(teamPlayers = teamPlayers, formation = formation)

        // Schritt 2: Generiere Hybrid-Aufstellung mit Markt-Spielern
        // Filtere Marktspieler: nur gute Spieler, nicht verletzt, und NICHT bereits im Team
        val teamPlayerIds = Set(teamPlayers.map { it -> it.id })
        val qualityMarketPlayers = marketPlayers.filter { player -> player.status != 8 && player.status != 16 && player.totalPoints >= 140 && !teamPlayerIds.contains(player.id) }

        val hybridLineup = generateHybridLineup(teamPlayers = teamPlayers, marketPlayers = qualityMarketPlayers, formation = formation, maxPlayersPerTeam = league.currentUser.mpst ?: 3)

        val comparison = LineupComparison(teamOnlyLineup = teamOnlyLineup, hybridLineup = hybridLineup)

        print("✅ Lineup comparison generated - Team only score: ${teamOnlyLineup.totalLineupScore}, Hybrid score: ${hybridLineup?.totalLineupScore ?: 0}")

        return@l comparison
    }

    /// Generiert optimale Aufstellung nur mit eigenen Spielern
    private fun generateTeamOnlyLineup(teamPlayers: Array<Player>, formation: Array<Int>): OptimalLineupResult {
        print("⚽ Generating team-only lineup...")

        var slots: Array<LineupSlot> = arrayOf()
        var slotIndex = 0
        var totalScore = 0.0

        // Durchgehe jede Position in der Formation
        for ((positionType, count) in formation.enumerated()) {
            val position = positionType + 1 // 1=TW, 2=ABW, 3=MF, 4=ST

            // Hole die besten Spieler für diese Position
            val playersForPosition = teamPlayers.filter { it -> it.position == position }
                .sorted { it, it_1 -> it.averagePoints > it_1.averagePoints }

            // Erstelle Slots für diese Position
            for (i in 0 until count) {
                if (i < playersForPosition.count) {
                    val player = playersForPosition[i]
                    val slotScore = calculatePlayerScore(player, forPosition = position)

                    slots.append(LineupSlot(slotIndex = slotIndex, positionType = position, ownedPlayerId = player.id, recommendedMarketPlayerId = null, slotScore = slotScore))

                    totalScore += slotScore
                    slotIndex += 1
                } else {
                    // Nicht genug Spieler für diese Position
                    slots.append(LineupSlot(slotIndex = slotIndex, positionType = position, ownedPlayerId = null, recommendedMarketPlayerId = null, slotScore = 0.0))
                    slotIndex += 1
                }
            }
        }

        val avgScore = if (slots.isEmpty) 0.0 else totalScore / Double(slots.count)

        return OptimalLineupResult(slots = slots, formationName = formationToString(formation), totalLineupScore = totalScore, isHybridWithMarketPlayers = false, marketPlayersNeeded = arrayOf(), totalMarketCost = 0, averagePlayerScore = avgScore)
    }

    /// Generiert optimale Aufstellung mit besten Markt-Spielern wo diese besser sind als eigene
    private fun generateHybridLineup(teamPlayers: Array<Player>, marketPlayers: Array<MarketPlayer>, formation: Array<Int>, maxPlayersPerTeam: Int): OptimalLineupResult? {
        print("🔄 Generating hybrid lineup with market players...")

        var slots: Array<LineupSlot> = arrayOf()
        var slotIndex = 0
        var totalScore = 0.0
        var marketPlayersNeeded: Array<String> = arrayOf()
        var totalMarketCost = 0
        var teamPlayerCounts: Dictionary<String, Int> = dictionaryOf() // Zähle Spieler pro Team

        // Durchgehe jede Position in der Formation
        for ((positionType, count) in formation.enumerated()) {
            val position = positionType + 1 // 1=TW, 2=ABW, 3=MF, 4=ST

            // Hole besten Spieler für diese Position (kombiniert Team und Markt)
            val ownTeamPlayersForPosition = teamPlayers.filter { it -> it.position == position }
                .sorted { it, it_1 -> it.averagePoints > it_1.averagePoints }

            val marketPlayersForPosition = marketPlayers.filter { it -> it.position == position }
                .sorted { it, it_1 -> it.averagePoints > it_1.averagePoints }

            // Erstelle Slots für diese Position
            for (i in 0 until count) {
                val ownPlayer = if (i < ownTeamPlayersForPosition.count) ownTeamPlayersForPosition[i] else null
                val bestMarketPlayer = findBestMarketPlayerForPosition(position = position, marketPlayers = marketPlayersForPosition, alreadyUsedIds = marketPlayersNeeded, teamPlayerCounts = teamPlayerCounts, maxPlayersPerTeam = maxPlayersPerTeam)

                // Entscheide: eigener Spieler oder Markt-Spieler?
                var selectedPlayer: Tuple2<Player?, MarketPlayer?>
                var recommendation: String? = null

                if (bestMarketPlayer != null) {
                    val marketPlayer = bestMarketPlayer
                    if ((ownPlayer != null) && (marketPlayer.averagePoints > ownPlayer.averagePoints + 0.5)) {
                        // Markt-Spieler ist deutlich besser
                        selectedPlayer = Tuple2(null, marketPlayer)
                        recommendation = marketPlayer.id
                        marketPlayersNeeded.append(marketPlayer.id)
                        totalMarketCost += marketPlayer.price

                        // Aktualisiere Team-Counter
                        val teamId = marketPlayer.teamId
                        teamPlayerCounts[teamId, { 0 }] += 1

                        print(String(format = "   🔄 Position %d Slot %d: Market %@ (%.1f pts)", position, i, marketPlayer.firstName, marketPlayer.averagePoints))
                    } else if (ownPlayer != null) {
                        // Eigener Spieler ist besser oder gleich gut
                        selectedPlayer = Tuple2(ownPlayer, null)
                        print(String(format = "   👤 Position %d Slot %d: Team %@ (%.1f pts)", position, i, ownPlayer.firstName, ownPlayer.averagePoints))
                    } else {
                        // Kein Spieler verfügbar
                        selectedPlayer = Tuple2(null, null)
                        print("   ❌ Position ${position} Slot ${i}: No player available")
                    }
                } else if (ownPlayer != null) {
                    // Eigener Spieler ist besser oder gleich gut
                    selectedPlayer = Tuple2(ownPlayer, null)
                    print(String(format = "   👤 Position %d Slot %d: Team %@ (%.1f pts)", position, i, ownPlayer.firstName, ownPlayer.averagePoints))
                } else {
                    // Kein Spieler verfügbar
                    selectedPlayer = Tuple2(null, null)
                    print("   ❌ Position ${position} Slot ${i}: No player available")
                }

                val slotScore = if (selectedPlayer.own != null) calculatePlayerScore(selectedPlayer.own!!, forPosition = position) else (if (selectedPlayer.market != null) calculateMarketPlayerScore(selectedPlayer.market!!, forPosition = position) else 0.0)

                slots.append(LineupSlot(slotIndex = slotIndex, positionType = position, ownedPlayerId = selectedPlayer.own?.id, recommendedMarketPlayerId = recommendation, slotScore = slotScore))

                totalScore += slotScore
                slotIndex += 1
            }
        }

        val avgScore = if (slots.isEmpty) 0.0 else totalScore / Double(slots.count)
        if (marketPlayersNeeded.count <= 0) {
            print("   ℹ️ No market players needed in hybrid lineup")
            return null
        }

        return OptimalLineupResult(slots = slots, formationName = formationToString(formation), totalLineupScore = totalScore, isHybridWithMarketPlayers = true, marketPlayersNeeded = marketPlayersNeeded, totalMarketCost = totalMarketCost, averagePlayerScore = avgScore)
    }

    /// Findet den besten verfügbaren Markt-Spieler für eine Position
    private fun findBestMarketPlayerForPosition(position: Int, marketPlayers: Array<MarketPlayer>, alreadyUsedIds: Array<String>, teamPlayerCounts: Dictionary<String, Int>, maxPlayersPerTeam: Int): MarketPlayer? {
        return marketPlayers.first l@{ player ->
            if (alreadyUsedIds.contains(player.id)) {
                return@l false
            }

            // Team-Limit nicht überschritten
            val currentTeamCount = teamPlayerCounts[player.teamId, { 0 }]
            if (currentTeamCount >= maxPlayersPerTeam) {
                return@l false
            }

            return@l true
        }
    }

    /// Berechnet Score für einen Team-Spieler
    private fun calculatePlayerScore(player: Player, forPosition: Int): Double {
        val position = forPosition
        // Basis-Scoring: averagePoints * Gewichtung + formTrend-Bonus
        var score = player.averagePoints * 2.0

        // Form-Trend-Bonus
        if (player.marketValueTrend > 1_000_000) {
            score += 2.0 // Gute Form
        } else if (player.marketValueTrend < -1_000_000) {
            score -= 2.0 // Schlechte Form
        }

        // Status-Malus
        if (player.status == 1) {
            score -= 5.0 // Verletzt
        } else if (player.status == 2) {
            score -= 2.0 // Angeschlagen
        }

        // Position-spezifische Gewichtung
        for (unusedi in 0..0) {
            when (position) {
                1 -> score *= 1.2
                4 -> score *= 1.15
                else -> break
            }
        }

        return max(0.0, score)
    }

    /// Berechnet Score für einen Markt-Spieler
    private fun calculateMarketPlayerScore(player: MarketPlayer, forPosition: Int): Double {
        val position = forPosition
        // Basis-Scoring: averagePoints * Gewichtung
        var score = player.averagePoints * 2.0

        // Form-Trend-Bonus
        if (player.marketValueTrend > 1_000_000) {
            score += 2.0 // Gute Form
        } else if (player.marketValueTrend < -1_000_000) {
            score -= 2.0 // Schlechte Form
        }

        // Status-Malus
        if (player.status == 1) {
            score -= 5.0 // Verletzt
        } else if (player.status == 2) {
            score -= 2.0 // Angeschlagen
        }

        // Position-spezifische Gewichtung
        for (unusedi in 0..0) {
            when (position) {
                1 -> score *= 1.2
                4 -> score *= 1.15
                else -> break
            }
        }

        return max(0.0, score)
    }

    /// Konvertiert Formation Array zu String (z.B. [1,4,4,2] -> "4-2-3-1")
    private fun formationToString(formation: Array<Int>): String {
        // [1, 4, 4, 2] -> "4-4-2-1" oder besser formatiert "4-2-3-1" (ohne Torwart in Standard-Notation)
        val withoutGoalkeeper = Array(formation.dropFirst())
        return withoutGoalkeeper.map { it -> String(it) }.joined(separator = "-")
    }

    @androidx.annotation.Keep
    companion object: CompanionClass() {
    }
    open class CompanionClass {
    }
}

// MARK: - Cache Structure

private class CachedRecommendations {
    val recommendations: Array<TransferRecommendation>
    val timestamp: Date

    constructor(recommendations: Array<TransferRecommendation>, timestamp: Date) {
        this.recommendations = recommendations.sref()
        this.timestamp = timestamp.sref()
    }
}

// MARK: - Private Helper Structs for Transpilation Safety
private class PlayerScore {
    internal val player: Player
    internal val unwantednessScore: Double

    constructor(player: Player, unwantednessScore: Double) {
        this.player = player
        this.unwantednessScore = unwantednessScore
    }
}

private class CandidateScore {
    internal val candidate: MarketPlayer
    internal val score: Double

    constructor(candidate: MarketPlayer, score: Double) {
        this.candidate = candidate
        this.score = score
    }
}

private class PlayerGain {
    internal val player: Player
    internal val gain: Int

    constructor(player: Player, gain: Int) {
        this.player = player
        this.gain = gain
    }
}
