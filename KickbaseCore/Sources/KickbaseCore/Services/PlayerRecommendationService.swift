import Combine
import Foundation
import SwiftUI

@MainActor
public class PlayerRecommendationService: ObservableObject {
    private let kickbaseManager: KickbaseManager

    // Cache für Empfehlungen
    private var cachedRecommendations: [String: CachedRecommendations] = [:]
    private let cacheValidityDuration: TimeInterval = 300  // 5 Minuten Cache

    // Cache für Spieler-Statistiken (smdc, ismc, smc)
    private var playerStatsCache: [String: PlayerMatchStats] = [:]

    // Logging controllable flag to avoid noisy prints in hot loops
    public var isVerboseLogging: Bool = false

    // Aktueller Spieltag (wird bei generateRecommendations gesetzt)
    private var currentMatchDay: Int = 10

    public init(kickbaseManager: KickbaseManager) {
        self.kickbaseManager = kickbaseManager
    }

    // MARK: - Sync Helper Methods

    public func getTeamPlayersSync(for league: League) async throws -> [TeamPlayer] {
        return try await kickbaseManager.authenticatedPlayerService.loadTeamPlayers(for: league)
    }

    public func getMarketPlayersSync(for league: League) async throws -> [MarketPlayer] {
        return try await kickbaseManager.authenticatedPlayerService.loadMarketPlayers(for: league)
    }

    // MARK: - Helper Structures

    private struct PlayerMatchStats {
        let smdc: Int  // Aktueller Spieltag
        let ismc: Int  // Spiele auf dem Platz (Startelf + Einwechslung)
        let smc: Int  // Spiele in Startelf (Starting Match Count)
    }

    // MARK: - Main Recommendation Functions

    /// Generiert Verkaufs- und Ersatz-Empfehlungen basierend auf einem Ziel
    public func generateSaleRecommendations(
        for league: League,
        goal: SaleRecommendationGoal,
        teamPlayers: [TeamPlayer],
        marketPlayers: [MarketPlayer],
        currentBudget: Int
    ) async -> [SaleRecommendation] {
        if isVerboseLogging {
            print("🛒 Generating sale recommendations for goal: \(goal.rawValue)")
        }

        let teamAnalysis = analyzeTeam(
            teamPlayers: teamPlayers, user: league.currentUser, budget: currentBudget)

        // Filter market players für Replacements
        // WICHTIG: Schließe Spieler aus, die der aktuelle Benutzer selbst auf den Transfermarkt gestellt hat
        let qualityMarketPlayers = marketPlayers.filter { player in
            guard player.status != 8 && player.status != 16 else { return false }
            guard player.averagePoints >= 70.0 else { return false }

            // Schließe Spieler aus, die der aktuellen Benutzer auf den Transfermarkt gestellt hat
            guard player.seller.id != league.currentUser.id else { return false }

            return true
        }
        var saleRecommendations: [SaleRecommendation] = []

        switch goal {
        case .balanceBudget:
            saleRecommendations = generateBudgetBalancingSales(
                teamPlayers: teamPlayers,
                marketPlayers: qualityMarketPlayers,
                currentBudget: currentBudget,
                teamAnalysis: teamAnalysis,
                maxPlayersPerTeam: league.currentUser.mpst
            )

        case .improvePosition:
            saleRecommendations = generatePositionImprovementSales(
                teamPlayers: teamPlayers,
                marketPlayers: qualityMarketPlayers,
                teamAnalysis: teamAnalysis
            )

        case .maxValue:
            saleRecommendations = generateMaxValueSales(
                teamPlayers: teamPlayers,
                marketPlayers: qualityMarketPlayers
            )

        case .reduceRisk:
            saleRecommendations = generateRiskReductionSales(
                teamPlayers: teamPlayers,
                marketPlayers: qualityMarketPlayers
            )

        case .raiseCapital:
            saleRecommendations = generateCapitalRaisingSales(
                teamPlayers: teamPlayers,
                marketPlayers: qualityMarketPlayers,
                currentBudget: currentBudget
            )
        }

        print("✅ Generated \(saleRecommendations.count) sale recommendations")
        return saleRecommendations
    }

    public func generateRecommendations(for league: League, budget: Int) async throws
        -> [TransferRecommendation]
    {
        if isVerboseLogging {
            print("🎯 Generating transfer recommendations for league: \(league.name)")
        }

        // Prüfe Cache
        if let cached = cachedRecommendations[league.id],
            Date().timeIntervalSince(cached.timestamp) < cacheValidityDuration
        {
            if isVerboseLogging {
                print(
                    "✅ Returning cached recommendations (\(cached.recommendations.count) players)")
            }
            return cached.recommendations
        }

        // Lade aktuelle Team-Spieler und Marktspieler PARALLEL
        async let teamPlayersTask = getTeamPlayers(for: league)
        async let marketPlayersTask = getMarketPlayers(for: league)

        let (teamPlayers, marketPlayers) = try await (teamPlayersTask, marketPlayersTask)

        // Hole aktuellen Spieltag und Stats von einem beliebigen Spieler
        let firstPlayerId = teamPlayers.first?.id ?? marketPlayers.first?.id

        if let playerId = firstPlayerId {
            if let stats = await kickbaseManager.authenticatedPlayerService.getMatchDayStats(
                leagueId: league.id, playerId: playerId)
            {
                currentMatchDay = stats.smdc
                // Speichere Stats für diesen Spieler im Cache
                playerStatsCache[playerId] = PlayerMatchStats(
                    smdc: stats.smdc, ismc: stats.ismc, smc: stats.smc)
                if isVerboseLogging { print("✅ Current matchday from API: \(currentMatchDay)") }
            } else {
                currentMatchDay = 10  // Fallback
                if isVerboseLogging {
                    print("⚠️ Could not fetch matchday stats, using fallback: \(currentMatchDay)")
                }
            }
        } else {
            currentMatchDay = 10
            if isVerboseLogging {
                print(
                    "⚠️ No players available to fetch matchday, using fallback: \(currentMatchDay)")
            }
        }
        if isVerboseLogging {
            print(
                "✅ Loaded \(teamPlayers.count) team players and \(marketPlayers.count) market players in parallel"
            )
        }
        let currentUser = league.currentUser

        // Analysiere das Team
        let teamAnalysis = analyzeTeam(teamPlayers: teamPlayers, user: currentUser, budget: budget)

        // OPTIMIERTE FILTERUNG: Frühe Aussortierung ungeeigneter Spieler
        let qualityMarketPlayers = marketPlayers.filter { player in
            // Schnellste Checks zuerst (Status)
            guard player.status != 8 && player.status != 16 else { return false }

            // Dann Leistungschecks
            guard player.averagePoints >= 70.0 else { return false }
            guard player.totalPoints >= 140 else { return false }

            // Schließe Spieler aus, die der aktuellen Benutzer auf den Transfermarkt gestellt hat
            guard player.seller.id != currentUser.id else { return false }

            return true
        }
        if isVerboseLogging {
            print(
                "📊 Pre-filtered from \(marketPlayers.count) to \(qualityMarketPlayers.count) quality players"
            )
        }

        // BATCH-PROCESSING: Verarbeite Spieler in Batches
        let batchSize = 50
        var allRecommendations: [TransferRecommendation] = []

        for batchStart in stride(from: 0, to: qualityMarketPlayers.count, by: batchSize) {
            let batchEnd = min(batchStart + batchSize, qualityMarketPlayers.count)
            let batch = Array(qualityMarketPlayers[batchStart..<batchEnd])

            // Verarbeite Batch parallel OFF the MainActor using detached tasks
            var batchRecommendations: [TransferRecommendation] = []
            await withTaskGroup(of: TransferRecommendation?.self) { group in
                for marketPlayer in batch {
                    group.addTask {
                        // Detached heavy CPU work off MainActor
                        return await Task.detached { () -> TransferRecommendation? in
                            let analysis = self.analyzePlayerNonIsolated(
                                marketPlayer, teamAnalysis: teamAnalysis)
                            let recommendation = self.createRecommendationNonIsolated(
                                marketPlayer: marketPlayer, analysis: analysis,
                                teamAnalysis: teamAnalysis)
                            return recommendation.recommendationScore >= 2.0 ? recommendation : nil
                        }.value
                    }
                }

                for await result in group {
                    if let rec = result { batchRecommendations.append(rec) }
                }
            }

            allRecommendations.append(contentsOf: batchRecommendations)
            if isVerboseLogging {
                print(
                    "📦 Processed batch \(batchStart/batchSize + 1): \(batchRecommendations.count) recommendations added"
                )
            }
        }

        print("✅ Generated \(allRecommendations.count) recommendations")

        // Sortiere nach Empfehlungswert und nimm Top 50 für Stats-Loading
        let topRecommendations = allRecommendations.sorted {
            $0.recommendationScore > $1.recommendationScore
        }.prefix(50)

        // Lade Stats für Top-Spieler asynchron (parallel, max 10 gleichzeitig)
        print("📊 Loading detailed stats for top \(topRecommendations.count) players...")
        await loadStatsForPlayers(Array(topRecommendations), leagueId: league.id)

        // Jetzt neu berechnen mit echten Stats
        var finalRecommendations: [TransferRecommendation] = []
        for recommendation in topRecommendations {
            var updatedRecommendation = recommendation

            // Wenn Stats verfügbar sind, neu berechnen
            if let stats = playerStatsCache[recommendation.player.id] {
                let updatedProjection = calculateSeasonProjectionWithStats(
                    recommendation.player,
                    stats: stats
                )

                // Erstelle eine aktualisierte Analysis mit neuer Projection
                let updatedAnalysis = PlayerAnalysis(
                    pointsPerGame: recommendation.analysis.pointsPerGame,
                    valueForMoney: recommendation.analysis.valueForMoney,
                    formTrend: recommendation.analysis.formTrend,
                    injuryRisk: recommendation.analysis.injuryRisk,
                    upcomingFixtures: recommendation.analysis.upcomingFixtures,
                    seasonProjection: updatedProjection
                )

                updatedRecommendation = TransferRecommendation(
                    player: recommendation.player,
                    recommendationScore: recommendation.recommendationScore,
                    reasons: recommendation.reasons,
                    analysis: updatedAnalysis,
                    riskLevel: recommendation.riskLevel,
                    priority: recommendation.priority
                )
            }

            finalRecommendations.append(updatedRecommendation)
        }

        // Sortiere final nach Score und limitiere auf Top 20
        finalRecommendations = Array(
            finalRecommendations.sorted { $0.recommendationScore > $1.recommendationScore }.prefix(
                20))

        // Cache speichern
        cachedRecommendations[league.id] = CachedRecommendations(
            recommendations: finalRecommendations,
            timestamp: Date()
        )

        return finalRecommendations
    }

    // MARK: - Helper Functions

    private func getTeamPlayers(for league: League) async throws -> [TeamPlayer] {
        return try await kickbaseManager.authenticatedPlayerService.loadTeamPlayers(for: league)
    }

    private func getMarketPlayers(for league: League) async throws -> [MarketPlayer] {
        return try await kickbaseManager.authenticatedPlayerService.loadMarketPlayers(for: league)
    }

    // MARK: - Analysis Functions

    private func analyzeTeam(teamPlayers: [TeamPlayer], user: LeagueUser, budget: Int)
        -> TeamAnalysis
    {
        var positionCounts: [String: Int] = [:]
        var positionTotalPoints: [String: Double] = [:]

        // Analysiere aktuelle Teamzusammensetzung
        for player in teamPlayers {
            let position = String(player.position)  // Convert Int to String
            positionCounts[position, default: 0] += 1
            positionTotalPoints[position, default: 0.0] += Double(player.totalPoints)
        }

        // Identifiziere schwache Positionen
        let weakPositions = identifyWeakPositions(
            positionCounts: positionCounts, positionTotalPoints: positionTotalPoints)
        let strengths = identifyStrongPositions(
            positionCounts: positionCounts, positionTotalPoints: positionTotalPoints)

        let budgetAnalysis = BudgetAnalysis(
            availableBudget: budget,
            recommendedSpending: Int(Double(budget) * 0.8),
            maxAffordablePrice: Int(Double(budget) * 0.9),
            emergencyReserve: Int(Double(budget) * 0.1)
        )

        return TeamAnalysis(
            weakPositions: weakPositions,
            strengths: strengths,
            budgetConstraints: budgetAnalysis,
            recommendations: []
        )
    }

    private func identifyWeakPositions(
        positionCounts: [String: Int], positionTotalPoints: [String: Double]
    ) -> [TeamAnalysis.Position] {
        var weakPositions: [TeamAnalysis.Position] = []

        // Mindestanzahl Spieler pro Position
        let minPlayersPerPosition: [String: Int] = [
            "1": 1,  // Torwart
            "2": 3,  // Verteidiger
            "3": 6,  // Mittelfeld
            "4": 1,  // Sturm
        ]

        // Überprüfe Anzahl pro Position
        for (positionStr, minCount) in minPlayersPerPosition {
            let currentCount = positionCounts[positionStr, default: 0]
            if currentCount < minCount {
                if let position = mapStringToPosition(positionStr) {
                    weakPositions.append(position)
                }
            }
        }

        // Überprüfe Leistung pro Position (unter Durchschnitt von 100 Punkten)
        for (positionStr, totalPoints) in positionTotalPoints {
            let count = positionCounts[positionStr, default: 0]
            if count > 0 {
                let averagePoints = totalPoints / Double(count)
                if averagePoints < 100.0 {
                    if let position = mapStringToPosition(positionStr) {
                        weakPositions.append(position)
                    }
                }
            }
        }

        return Array(Set(weakPositions))  // Entferne Duplikate
    }

    private func identifyStrongPositions(
        positionCounts: [String: Int], positionTotalPoints: [String: Double]
    ) -> [TeamAnalysis.Position] {
        var strongPositions: [TeamAnalysis.Position] = []

        // Positionen mit überdurchschnittlicher Leistung (über 150 Punkten)
        for (positionStr, totalPoints) in positionTotalPoints {
            let count = positionCounts[positionStr, default: 0]
            if count > 0 {
                let averagePoints = totalPoints / Double(count)
                if averagePoints > 150.0 {
                    if let position = mapStringToPosition(positionStr) {
                        strongPositions.append(position)
                    }
                }
            }
        }

        return strongPositions
    }

    nonisolated private func mapStringToPosition(_ positionStr: String) -> TeamAnalysis.Position? {
        switch positionStr {
        case "1":
            return .goalkeeper
        case "2":
            return .defender
        case "3":
            return .midfielder
        case "4":
            return .striker
        default:
            return nil
        }
    }

    // Non-isolated wrapper for running analysis off the MainActor in detached Tasks
    nonisolated private func analyzePlayerNonIsolated(
        _ marketPlayer: MarketPlayer, teamAnalysis: TeamAnalysis
    )
        -> PlayerAnalysis
    {
        // Reuse the isolated implementations which are pure functions of inputs
        let pointsPerGame = marketPlayer.averagePoints
        let valueForMoney = calculateValueForMoneyNonIsolated(marketPlayer)
        let formTrend = calculateCurrentFormNonIsolated(marketPlayer)
        let injuryRisk = calculateInjuryRiskNonIsolated(marketPlayer)
        let seasonProjection = calculateSeasonProjectionNonIsolated(marketPlayer)

        return PlayerAnalysis(
            pointsPerGame: pointsPerGame,
            valueForMoney: valueForMoney,
            formTrend: formTrend,
            injuryRisk: injuryRisk,
            upcomingFixtures: [],  // Leer für jetzt
            seasonProjection: seasonProjection
        )
    }

    nonisolated private func calculateCurrentFormNonIsolated(_ marketPlayer: MarketPlayer)
        -> PlayerAnalysis.FormTrend
    {
        let marketValueChange = marketPlayer.marketValueTrend
        let pointsPerGame = marketPlayer.averagePoints

        if marketValueChange > 500000 && pointsPerGame > 8.0 {
            return .improving
        } else if marketValueChange < -500000 || pointsPerGame < 4.0 {
            return .declining
        } else {
            return .stable
        }
    }

    nonisolated private func calculateInjuryRiskNonIsolated(_ marketPlayer: MarketPlayer)
        -> PlayerAnalysis.InjuryRisk
    {
        // Basis-Risiko basierend auf Status
        if marketPlayer.status == 8 {
            return .high
        } else if marketPlayer.status == 4 {
            return .medium
        } else {
            return .low
        }
    }

    nonisolated private func calculateSeasonProjectionNonIsolated(_ marketPlayer: MarketPlayer)
        -> SeasonProjection
    {
        // Fallback-Berechnung ohne Stats (wird später mit echten Stats überschrieben)
        let currentPoints = marketPlayer.totalPoints
        let pointsPerGame = marketPlayer.averagePoints

        // Schätze gespielte Spiele aus Punkten
        let estimatedGamesPlayed =
            pointsPerGame > 0 ? Int(round(Double(currentPoints) / pointsPerGame)) : 0
        let remainingGames = max(34 - estimatedGamesPlayed, 0)

        let projectedTotal = currentPoints + Int(pointsPerGame * Double(remainingGames))
        // Nutze tfhmvt wenn verfügbar, sonst Fallback auf marketValueTrend
        let dailyChange = marketPlayer.marketValueTrend
        let projectedValueIncrease = dailyChange * remainingGames

        // Niedrige Confidence, da keine echten Stats
        let confidence = 0.5

        return SeasonProjection(
            projectedTotalPoints: projectedTotal,
            projectedValueIncrease: projectedValueIncrease,
            confidence: confidence
        )
    }

    private func calculateSeasonProjectionWithStats(
        _ marketPlayer: MarketPlayer, stats: PlayerMatchStats
    ) -> SeasonProjection {
        let currentPoints = marketPlayer.totalPoints
        let gamesPlayed = stats.ismc  // Spiele auf dem Platz (Startelf + Einwechslung)
        let pointsPerGame = marketPlayer.averagePoints
        let remainingGames = max(34 - gamesPlayed, 0)

        let projectedTotal = currentPoints + Int(pointsPerGame * Double(remainingGames))
        // Nutze tfhmvt wenn verfügbar, sonst Fallback auf marketValueTrend
        let dailyChange = marketPlayer.marketValueTrend
        let projectedValueIncrease = dailyChange * remainingGames

        // Debug: Zeige Stats (nur wenn verbose logging aktiviert)
        if isVerboseLogging {
            print("📊 Stats for \(marketPlayer.firstName) \(marketPlayer.lastName):")
            print("   smdc=\(stats.smdc), ismc=\(stats.ismc), smc=\(stats.smc)")
        }

        // Confidence basiert auf Spielbeteiligung
        let confidence: Double
        if stats.smdc > 0 && gamesPlayed > 0 {
            let playedRatio = Double(gamesPlayed) / Double(stats.smdc)
            // Bonus für Stammkräfte (smc ~ ismc)
            let starterBonus = Double(stats.smc) / max(Double(stats.ismc), 1.0)
            confidence = min(playedRatio * (0.7 + starterBonus * 0.3), 1.0)

            if isVerboseLogging {
                print(
                    "   playedRatio=\(String(format: "%.2f", playedRatio)), starterBonus=\(String(format: "%.2f", starterBonus))"
                )
                print(
                    "🎯 Confidence for \(marketPlayer.firstName) \(marketPlayer.lastName): \(gamesPlayed) played / \(stats.smdc) matchdays (started: \(stats.smc)) = \(String(format: "%.1f%%", confidence * 100))"
                )
            }
        } else {
            confidence = 0.0
            if isVerboseLogging {
                print("⚠️ No stats available for \(marketPlayer.firstName) \(marketPlayer.lastName)")
            }
        }

        return SeasonProjection(
            projectedTotalPoints: projectedTotal,
            projectedValueIncrease: projectedValueIncrease,
            confidence: confidence
        )
    }

    /// Lädt Stats für eine Liste von Spielern (parallel, max 10 gleichzeitig)
    private func loadStatsForPlayers(_ recommendations: [TransferRecommendation], leagueId: String)
        async
    {
        // Nur Spieler laden, die noch nicht im Cache sind
        let playersToLoad = recommendations.filter { playerStatsCache[$0.player.id] == nil }

        guard !playersToLoad.isEmpty else {
            if isVerboseLogging { print("✅ All player stats already cached") }
            return
        }

        if isVerboseLogging { print("📥 Loading stats for \(playersToLoad.count) players...") }

        // Verarbeite in Batches von 10 parallel
        for batchStart in stride(from: 0, to: playersToLoad.count, by: 10) {
            let batchEnd = min(batchStart + 10, playersToLoad.count)
            let batch = Array(playersToLoad[batchStart..<batchEnd])

            // Lade Stats parallel
            #if os(iOS)
                await withTaskGroup(of: (String, PlayerMatchStats?).self) { group in
                    for recommendation in batch {
                        group.addTask {
                            let stats = await self.kickbaseManager.authenticatedPlayerService
                                .getMatchDayStats(
                                    leagueId: leagueId,
                                    playerId: recommendation.player.id
                                )
                            if let stats = stats {
                                return (
                                    recommendation.player.id,
                                    PlayerMatchStats(
                                        smdc: stats.smdc, ismc: stats.ismc, smc: stats.smc)
                                )
                            }
                            return (recommendation.player.id, nil)
                        }
                    }

                    for await (playerId, stats) in group {
                        if let stats = stats {
                            self.playerStatsCache[playerId] = stats
                        }
                    }
                }
            #else
                for recommendation in batch {
                    if let stats = await self.kickbaseManager.authenticatedPlayerService
                        .getMatchDayStats(leagueId: leagueId, playerId: recommendation.player.id)
                    {
                        self.playerStatsCache[recommendation.player.id] = PlayerMatchStats(
                            smdc: stats.smdc, ismc: stats.ismc, smc: stats.smc)
                    }
                }
            #endif
        }

        if isVerboseLogging { print("✅ Loaded stats for \(playerStatsCache.count) players total") }
    }

    private func calculateValueForMoney(_ marketPlayer: MarketPlayer) -> Double {
        guard marketPlayer.price > 0 else { return 0.0 }
        let pointsPerMillion =
            Double(marketPlayer.totalPoints) / (Double(marketPlayer.price) / 1_000_000.0)
        return pointsPerMillion
    }

    // Nonisolated wrapper for calculateValueForMoney to be callable from detached tasks
    nonisolated private func calculateValueForMoneyNonIsolated(_ marketPlayer: MarketPlayer)
        -> Double
    {
        guard marketPlayer.price > 0 else { return 0.0 }
        let pointsPerMillion =
            Double(marketPlayer.totalPoints) / (Double(marketPlayer.price) / 1_000_000.0)
        return pointsPerMillion
    }

    // Nonisolated version of createRecommendation used in detached tasks
    nonisolated private func createRecommendationNonIsolated(
        marketPlayer: MarketPlayer, analysis: PlayerAnalysis, teamAnalysis: TeamAnalysis
    ) -> TransferRecommendation {
        let score = calculateRecommendationScore(
            marketPlayer: marketPlayer, analysis: analysis, teamAnalysis: teamAnalysis)
        let riskLevel = determineRiskLevel(analysis: analysis)
        let priority = determinePriority(
            score: score, teamAnalysis: teamAnalysis, position: marketPlayer.position)
        let reasons = generateReasons(
            marketPlayer: marketPlayer, analysis: analysis, teamAnalysis: teamAnalysis)

        return TransferRecommendation(
            player: marketPlayer,
            recommendationScore: score,
            reasons: reasons,
            analysis: analysis,
            riskLevel: riskLevel,
            priority: priority
        )
    }

    // MARK: - Cache Helper

    public func clearCache() {
        cachedRecommendations.removeAll()
        if isVerboseLogging { print("🗑️ Recommendations cache cleared") }
    }

    public func clearCacheForLeague(_ leagueId: String) {
        cachedRecommendations.removeValue(forKey: leagueId)
        if isVerboseLogging { print("🗑️ Cache cleared for league: \(leagueId)") }
    }

    private func createRecommendation(
        marketPlayer: MarketPlayer, analysis: PlayerAnalysis, teamAnalysis: TeamAnalysis
    ) -> TransferRecommendation {
        let score = calculateRecommendationScore(
            marketPlayer: marketPlayer, analysis: analysis, teamAnalysis: teamAnalysis)
        let riskLevel = determineRiskLevel(analysis: analysis)
        let priority = determinePriority(
            score: score, teamAnalysis: teamAnalysis, position: marketPlayer.position)
        let reasons = generateReasons(
            marketPlayer: marketPlayer, analysis: analysis, teamAnalysis: teamAnalysis)

        return TransferRecommendation(
            player: marketPlayer,
            recommendationScore: score,
            reasons: reasons,
            analysis: analysis,
            riskLevel: riskLevel,
            priority: priority
        )
    }

    nonisolated private func calculateRecommendationScore(
        marketPlayer: MarketPlayer, analysis: PlayerAnalysis, teamAnalysis: TeamAnalysis
    ) -> Double {
        var score = 0.0

        // VERBESSERTE BEWERTUNG: Höhere Gewichtung für starke Spieler

        // 1. Punkte pro Spiel (0-6 Punkte) - Höhere Gewichtung!
        let pointsPerGameScore = min(analysis.pointsPerGame / 1.5, 6.0)
        score += pointsPerGameScore

        // 2. Absolute Punkte Bonus für Top-Performer (0-3 Punkte)
        if marketPlayer.totalPoints >= 150 {
            score += 3.0
        } else if marketPlayer.totalPoints >= 100 {
            score += 2.0
        } else if marketPlayer.totalPoints >= 75 {
            score += 1.0
        }

        // 3. Value-for-Money Score (0-4 Punkte) - Verbessert
        let valueScore = min(analysis.valueForMoney / 8.0, 4.0)
        score += valueScore

        // 4. Form-Trend (wichtiger gemacht)
        switch analysis.formTrend {
        case .improving:
            score += 3.0  // Erhöht von 2.0
        case .stable:
            score += 0.5  // Kleiner Bonus für Stabilität
        case .declining:
            score -= 2.0  // Erhöht von -1.0
        }

        // 5. Marktwert-Trend Bonus
        if marketPlayer.marketValueTrend > 1_000_000 {
            score += 2.0  // Stark steigender Marktwert
        } else if marketPlayer.marketValueTrend > 500000 {
            score += 1.0  // Steigender Marktwert
        } else if marketPlayer.marketValueTrend < -1_000_000 {
            score -= 1.5  // Stark fallender Marktwert
        }

        // 6. Team-Need Bonus (wichtiger gemacht)
        if let playerPosition = mapIntToPosition(marketPlayer.position) {
            if teamAnalysis.weakPositions.contains(playerPosition) {
                score += 4.0  // Erhöht von 3.0
            } else if teamAnalysis.strengths.contains(playerPosition) {
                score += 0.5  // Reduziert von 1.0
            } else {
                score += 2.0  // Gleich
            }
        }

        // 7. Spiele-Konsistenz Bonus
        let gamesPlayed = Double(marketPlayer.number)
        if gamesPlayed >= 15 {
            score += 1.0  // Konsistent viele Spiele
        } else if gamesPlayed >= 10 {
            score += 0.5
        } else if gamesPlayed < 5 {
            score -= 1.0  // Zu wenige Spiele
        }

        // 8. Preis-Effizienz
        let priceInMillions = Double(marketPlayer.price) / 1_000_000.0
        if priceInMillions <= 5.0 && analysis.pointsPerGame >= 7.0 {
            score += 2.0  // Günstiger Topstar
        } else if priceInMillions <= 3.0 && analysis.pointsPerGame >= 6.0 {
            score += 1.5  // Sehr günstig
        }

        // Score auf theoretisches Maximum von ~24 Punkten begrenzen
        return min(max(score, 0.0), 24.0)
    }

    nonisolated private func mapIntToPosition(_ position: Int) -> TeamAnalysis.Position? {
        switch position {
        case 1:
            return .goalkeeper
        case 2:
            return .defender
        case 3:
            return .midfielder
        case 4:
            return .striker
        default:
            return nil
        }
    }

    nonisolated private func determineRiskLevel(analysis: PlayerAnalysis)
        -> TransferRecommendation.RiskLevel
    {
        switch analysis.injuryRisk {
        case .high:
            return .high
        case .medium:
            return analysis.formTrend == .declining ? .high : .medium
        case .low:
            return analysis.formTrend == .declining ? .medium : .low
        }
    }

    nonisolated private func determinePriority(
        score: Double, teamAnalysis: TeamAnalysis, position: Int
    )
        -> TransferRecommendation.Priority
    {
        if let playerPosition = mapIntToPosition(position) {
            if teamAnalysis.weakPositions.contains(playerPosition) && score >= 19.2 {
                return .essential
            } else if score >= 12.0 {
                return .recommended
            } else {
                return .optional
            }
        }
        return .optional
    }

    nonisolated private func generateReasons(
        marketPlayer: MarketPlayer, analysis: PlayerAnalysis, teamAnalysis: TeamAnalysis
    ) -> [RecommendationReason] {
        return []
    }
}

// MARK: - Cache Structure

private struct CachedRecommendations {
    public let recommendations: [TransferRecommendation]
    public let timestamp: Date
}

// MARK: - Private Helper Structs for Transpilation Safety
private struct PlayerScore {
    let player: TeamPlayer
    let unwantednessScore: Double
}

private struct CandidateScore {
    let candidate: MarketPlayer
    let score: Double
}

private struct PlayerGain {
    let player: TeamPlayer
    let gain: Int
}

// MARK: - Sale Recommendation Helper Functions

extension PlayerRecommendationService {

    /// Verkaufs-Empfehlungen: Spieler verkaufen um Budget zu sparen und zu investieren
    private func generateBudgetBalancingSales(
        teamPlayers: [TeamPlayer],
        marketPlayers: [MarketPlayer],
        currentBudget: Int,
        teamAnalysis: TeamAnalysis,
        maxPlayersPerTeam: Int? = nil
    ) -> [SaleRecommendation] {
        var recommendations: [SaleRecommendation] = []

        // Wenn Budget bereits positiv ist, keine Empfehlungen nötig
        guard currentBudget < 0 else {
            print("✅ Budget ist positiv (\(currentBudget)), keine Verkaufs-Empfehlungen nötig")
            return []
        }

        let budgetGap = abs(currentBudget)  // Wieviel wir verdienen müssen
        print("💰 Budget gap to cover: \(budgetGap)")

        // Berechne "Unwichtigkeits-Score" für jeden Spieler
        // Niedrige Scores = unwichtig = gut zum Verkaufen
        let playerScores: [PlayerScore] = teamPlayers.map { player in
            // Score basiert auf:
            // 1. Durchschnittliche Punkte pro Spiel (niedrig = unwichtig)
            // 2. Gesamtpunkte (niedrig = unwichtig)
            // 3. Status (verletzt/angeschlagen = unwichtig)

            var score = Double(player.totalPoints) + (player.averagePoints * 10)

            // Bonus für Verletzte/Angeschlagene
            if player.status == 1 {  // Verletzt
                score -= 500
            } else if player.status == 2 {  // Angeschlagen
                score -= 250
            }

            return PlayerScore(player: player, unwantednessScore: score)
        }

        // Sortiere von unwichtig zu wichtig (aufsteigend nach Score)
        let sortedTeamPlayers: [TeamPlayer] = playerScores.sorted {
            $0.unwantednessScore < $1.unwantednessScore
        }
        .map { $0.player }

        var accumulatedSavings = 0

        for teamPlayer in sortedTeamPlayers {
            /* print(
                "🔍 Considering \(teamPlayer.fullName) (€\(teamPlayer.marketValue / 1000)k, \("XXX")) Pkt/Spiel, Punkte: \(teamPlayer.totalPoints))"
            ) */

            // Finde Ersatz-Kandidaten in gleicher Position
            // Wir wollen günstigere Spieler, um Geld zu sparen
            let maxPriceForReplacement = Int(Double(teamPlayer.marketValue) * 0.8)

            let replacementCandidates = findReplacementCandidates(
                for: teamPlayer,
                in: marketPlayers,
                maxPrice: maxPriceForReplacement,
                teamPlayers: teamPlayers,
                maxPlayersPerTeam: maxPlayersPerTeam
            )

            // Stubbed for transpilation safety - simplified logic
            if !replacementCandidates.isEmpty {
                let bestReplacement = replacementCandidates[0]
                let explanation = "Empfehlung: \(bestReplacement.player.fullName)"
                let budgetSavings = teamPlayer.marketValue - bestReplacement.player.marketValue

                // Priority
                let remainingGap = budgetGap - accumulatedSavings
                let priority: TransferRecommendation.Priority =
                    (budgetSavings >= remainingGap)
                    ? .essential : (budgetSavings >= remainingGap / 2) ? .recommended : .optional

                let saleRec = SaleRecommendation(
                    playerToSell: teamPlayer,
                    replacements: replacementCandidates,
                    goal: .balanceBudget,
                    explanation: explanation,
                    priority: priority
                )
                recommendations.append(saleRec)
                accumulatedSavings += budgetSavings

                if accumulatedSavings >= budgetGap && recommendations.count >= 3 {
                    print("✅ Budget gap covered with \(recommendations.count) recommendations!")
                    break
                }
            } else {
                print("   ✗ No replacement candidates found")
            }
        }

        print("📊 Generated \(recommendations.count) recommendations for budget balancing")
        return recommendations
    }

    /// Verkaufs-Empfehlungen: Schwache Spieler gegen bessere austauschen
    private func generatePositionImprovementSales(
        teamPlayers: [TeamPlayer],
        marketPlayers: [MarketPlayer],
        teamAnalysis: TeamAnalysis
    ) -> [SaleRecommendation] {
        return []  // Stubbed for build stability
    }

    /// Verkaufs-Empfehlungen: Spieler mit höchstem Gewinn verkaufen
    private func generateMaxValueSales(
        teamPlayers: [TeamPlayer],
        marketPlayers: [MarketPlayer]
    ) -> [SaleRecommendation] {
        return []  // Stubbed for build stability
    }

    /// Verkaufs-Empfehlungen: Riskante Spieler verkaufen
    private func generateRiskReductionSales(
        teamPlayers: [TeamPlayer],
        marketPlayers: [MarketPlayer]
    ) -> [SaleRecommendation] {
        var recommendations: [SaleRecommendation] = []

        // Finde verletzte oder angeschlagene Spieler
        let riskyPlayers = teamPlayers.filter { player in
            // Status: 1=Verletzt, 2=Angeschlagen, 4=Aufbautraining
            return player.status == 1 || player.status == 2 || player.status == 4
        }

        for riskyPlayer in riskyPlayers {
            let replacements = findReplacementCandidates(
                for: riskyPlayer,
                in: marketPlayers,
                maxPrice: riskyPlayer.marketValue * 2  // Erlauben teurer, um sicheren Spieler zu bekommen
            )

            if !replacements.isEmpty {
                let bestReplacement = replacements[0]  // Stubbed to list access
                // ... rest of logic
                let riskText: String
                switch riskyPlayer.status {
                case 1: riskText = "verletzt"
                case 2: riskText = "angeschlagen"
                case 4: riskText = "im Aufbautraining"
                default: riskText = "mit Risiko"
                }

                let explanation =
                    "\(riskyPlayer.fullName) ist aktuell \(riskText). Verkaufe ihn jetzt und ersetze ihn durch einen gesünderen Spieler, um Ausfallrisiko zu minimieren."

                let saleRec = SaleRecommendation(
                    playerToSell: riskyPlayer,
                    replacements: replacements,
                    goal: .reduceRisk,
                    explanation: explanation,
                    priority: riskyPlayer.status == 1 ? .essential : .recommended
                )
                recommendations.append(saleRec)
            }
        }

        return recommendations
    }

    /// Verkaufs-Empfehlungen: Geld beschaffen für Transfers
    private func generateCapitalRaisingSales(
        teamPlayers: [TeamPlayer],
        marketPlayers: [MarketPlayer],
        currentBudget: Int
    ) -> [SaleRecommendation] {
        var recommendations: [SaleRecommendation] = []

        // Finde die wertvollsten Spieler
        let valuablePlayers =
            teamPlayers
            .filter { $0.totalPoints < 100 }  // Spieler die nicht so gut performen
            .sorted { $0.marketValue > $1.marketValue }
            .prefix(5)

        for player in valuablePlayers {
            let replacements = findReplacementCandidates(
                for: player,
                in: marketPlayers,
                maxPrice: Int(Double(player.marketValue) * 1.5)
            )

            if !replacements.isEmpty {
                let explanation =
                    "Verkaufe \(player.fullName) (~€\(player.marketValue / 1000)k) um Kapital für neue Transfers zu beschaffen."

                let saleRec = SaleRecommendation(
                    playerToSell: player,
                    replacements: replacements,
                    goal: .raiseCapital,
                    explanation: explanation,
                    priority: .recommended
                )
                recommendations.append(saleRec)
            }
        }

        return recommendations
    }

    /// Findet geeignete Ersatz-Spieler für einen Team-Spieler
    private func findReplacementCandidates(
        for teamPlayer: TeamPlayer,
        in marketPlayers: [MarketPlayer],
        maxPrice: Int,
        teamPlayers: [TeamPlayer]? = nil,
        maxPlayersPerTeam: Int? = nil
    ) -> [ReplacementSuggestion] {
        print(
            "   🔎 Finding replacements for \(teamPlayer.fullName) (pos: \(teamPlayer.position), maxPrice: €\(maxPrice / 1000)k)"
        )

        // Zähle wie viele Spieler vom gleichen Team bereits im Team sind
        var playersFromTeam = 0
        if let teamPlayers = teamPlayers {
            playersFromTeam = teamPlayers.filter { $0.teamId == teamPlayer.teamId }.count
        }

        // Filter nach Position und Preis
        let candidates = marketPlayers.filter { marketPlayer in
            guard marketPlayer.position == teamPlayer.position else { return false }
            guard marketPlayer.price <= maxPrice else { return false }

            // Beachte maxPlayersPerTeam - aber NICHT wenn wir einen Spieler vom gleichen Team ersetzen
            if let maxPlayersPerTeam = maxPlayersPerTeam {
                let replacementTeamCount =
                    teamPlayers?.filter { $0.teamId == marketPlayer.teamId }.count ?? 0
                // Wenn wir einen Spieler vom gleichen Team ersetzen (playersFromTeam > 0),
                // können wir jemanden vom gleichen Team nehmen
                let teamPlayerAlreadySelected =
                    teamPlayers?.contains { $0.teamId == marketPlayer.teamId } ?? false
                if teamPlayerAlreadySelected && replacementTeamCount >= maxPlayersPerTeam {
                    // Nur erlauben wenn wir vom gleichen Team ersetzen
                    if marketPlayer.teamId != teamPlayer.teamId {
                        return false
                    }
                } else if replacementTeamCount >= maxPlayersPerTeam {
                    return false
                }
            }

            // Viel weniger streng: Akzeptiere auch Spieler mit geringen Punkten
            // da es um Budget-Einsparung geht, nicht um Perfektion
            guard marketPlayer.totalPoints >= 0 else { return false }
            return true
        }

        print("   📊 Found \(candidates.count) candidates in price range (after team limit check)")

        // Bewerte und sortiere
        let scored: [CandidateScore] = candidates.map { candidate in
            let performanceDiff = candidate.averagePoints - Double(teamPlayer.averagePoints)
            let priceDiff = Double(teamPlayer.marketValue - candidate.price) / 1_000_000.0

            // Score: Budget-Einsparung ist wichtiger als Performance!
            // 70% Gewicht auf Preis, 30% auf Performance
            let score = priceDiff * 0.7 + performanceDiff * 0.3

            print(
                "     - \(candidate.firstName) \(candidate.lastName): score=\(String(format: "%.2f", score)) (price_diff=\(String(format: "%.2f", priceDiff)), perf_diff=\(String(format: "%.2f", performanceDiff)))"
            )

            return CandidateScore(candidate: candidate, score: score)
        }
        .sorted { $0.score > $1.score }

        print("   ✅ Top \(min(3, scored.count)) candidates selected")

        // Pre-calculate values to help Skip transpiler with closure captures
        let teamPlayerMarketValue = teamPlayer.marketValue
        let teamPlayerAveragePoints = Double(teamPlayer.averagePoints)

        // Konvertiere zu ReplacementSuggestion
        var suggestions: [ReplacementSuggestion] = []
        for item in scored.prefix(3) {
            let candidate = item.candidate

            suggestions.append(
                ReplacementSuggestion(
                    player: candidate,
                    reasonForSale: "Bessere Alternative verfügbar",
                    budgetSavings: teamPlayerMarketValue - candidate.price,
                    performanceGain: candidate.averagePoints - teamPlayerAveragePoints,
                    riskReduction: 0.0  // Placeholder
                ))
        }
        return suggestions
    }

    // MARK: - Lineup Optimization Functions

    /// Generiert optimale Aufstellungen: nur eigene Spieler vs. Hybrid mit Markt-Spielern
    public func generateOptimalLineupComparison(
        for league: League,
        teamPlayers: [TeamPlayer],
        marketPlayers: [MarketPlayer],
        formation: [Int]  // z.B. [1,4,4,2] für 4-2-3-1 (1 TW, 4 ABW, 4 MF, 2 ST)
    ) async -> LineupComparison {
        print("🎯 Generating optimal lineup comparison for formation: \(formation)")

        // Schritt 1: Generiere Team-Only Aufstellung
        let teamOnlyLineup = generateTeamOnlyLineup(
            teamPlayers: teamPlayers,
            formation: formation
        )

        // Schritt 2: Generiere Hybrid-Aufstellung mit Markt-Spielern
        // Filtere Marktspieler: nur gute Spieler, nicht verletzt, und NICHT bereits im Team
        let teamPlayerIds = Set(teamPlayers.map { $0.id })
        let qualityMarketPlayers = marketPlayers.filter { player in
            player.status != 8 && player.status != 16 && player.totalPoints >= 140
                && !teamPlayerIds.contains(player.id)
        }

        let hybridLineup = generateHybridLineup(
            teamPlayers: teamPlayers,
            marketPlayers: qualityMarketPlayers,
            formation: formation,
            maxPlayersPerTeam: league.currentUser.mpst ?? 3
        )

        let comparison = LineupComparison(
            teamOnlyLineup: teamOnlyLineup,
            hybridLineup: hybridLineup
        )

        print(
            "✅ Lineup comparison generated - Team only score: \(teamOnlyLineup.totalLineupScore), Hybrid score: \(hybridLineup?.totalLineupScore ?? 0)"
        )

        return comparison
    }

    /// Generiert optimale Aufstellung nur mit eigenen Spielern
    private func generateTeamOnlyLineup(
        teamPlayers: [TeamPlayer],
        formation: [Int]
    ) -> OptimalLineupResult {
        print("⚽ Generating team-only lineup...")

        var slots: [LineupSlot] = []
        var slotIndex = 0
        var totalScore = 0.0

        // Durchgehe jede Position in der Formation
        for (positionType, count) in formation.enumerated() {
            let position = positionType + 1  // 1=TW, 2=ABW, 3=MF, 4=ST

            // Hole die besten Spieler für diese Position
            let playersForPosition = teamPlayers.filter { $0.position == position }
                .sorted { $0.averagePoints > $1.averagePoints }

            // Erstelle Slots für diese Position
            for i in 0..<count {
                if i < playersForPosition.count {
                    let player = playersForPosition[i]
                    let slotScore = calculatePlayerScore(player, forPosition: position)

                    slots.append(
                        LineupSlot(
                            slotIndex: slotIndex,
                            positionType: position,
                            ownedPlayerId: player.id,
                            recommendedMarketPlayerId: nil,
                            slotScore: slotScore
                        ))

                    totalScore += slotScore
                    slotIndex += 1
                } else {
                    // Nicht genug Spieler für diese Position
                    slots.append(
                        LineupSlot(
                            slotIndex: slotIndex,
                            positionType: position,
                            ownedPlayerId: nil,
                            recommendedMarketPlayerId: nil,
                            slotScore: 0.0
                        ))
                    slotIndex += 1
                }
            }
        }

        let avgScore = slots.isEmpty ? 0.0 : totalScore / Double(slots.count)

        return OptimalLineupResult(
            slots: slots,
            formationName: formationToString(formation),
            totalLineupScore: totalScore,
            isHybridWithMarketPlayers: false,
            marketPlayersNeeded: [],
            totalMarketCost: 0,
            averagePlayerScore: avgScore
        )
    }

    /// Generiert optimale Aufstellung mit besten Markt-Spielern wo diese besser sind als eigene
    private func generateHybridLineup(
        teamPlayers: [TeamPlayer],
        marketPlayers: [MarketPlayer],
        formation: [Int],
        maxPlayersPerTeam: Int
    ) -> OptimalLineupResult? {
        print("🔄 Generating hybrid lineup with market players...")

        var slots: [LineupSlot] = []
        var slotIndex = 0
        var totalScore = 0.0
        var marketPlayersNeeded: [String] = []
        var totalMarketCost = 0
        var teamPlayerCounts: [String: Int] = [:]  // Zähle Spieler pro Team

        // Durchgehe jede Position in der Formation
        for (positionType, count) in formation.enumerated() {
            let position = positionType + 1  // 1=TW, 2=ABW, 3=MF, 4=ST

            // Hole besten Spieler für diese Position (kombiniert Team und Markt)
            let ownTeamPlayersForPosition = teamPlayers.filter { $0.position == position }
                .sorted { $0.averagePoints > $1.averagePoints }

            let marketPlayersForPosition = marketPlayers.filter { $0.position == position }
                .sorted { $0.averagePoints > $1.averagePoints }

            // Erstelle Slots für diese Position
            for i in 0..<count {
                let ownPlayer =
                    i < ownTeamPlayersForPosition.count ? ownTeamPlayersForPosition[i] : nil
                let bestMarketPlayer = findBestMarketPlayerForPosition(
                    position: position,
                    marketPlayers: marketPlayersForPosition,
                    alreadyUsedIds: marketPlayersNeeded,
                    teamPlayerCounts: teamPlayerCounts,
                    maxPlayersPerTeam: maxPlayersPerTeam
                )

                // Entscheide: eigener Spieler oder Markt-Spieler?
                var selectedPlayer: (own: TeamPlayer?, market: MarketPlayer?)
                var recommendation: String?

                if let marketPlayer = bestMarketPlayer,
                    let ownPlayer = ownPlayer,
                    marketPlayer.averagePoints > ownPlayer.averagePoints + 0.5
                {
                    // Markt-Spieler ist deutlich besser
                    selectedPlayer = (nil, marketPlayer)
                    recommendation = marketPlayer.id
                    marketPlayersNeeded.append(marketPlayer.id)
                    totalMarketCost += marketPlayer.price

                    // Aktualisiere Team-Counter
                    let teamId = marketPlayer.teamId
                    teamPlayerCounts[teamId, default: 0] += 1

                    print(
                        String(
                            format: "   🔄 Position %d Slot %d: Market %@ (%.1f pts)", position, i,
                            marketPlayer.firstName, marketPlayer.averagePoints)
                    )
                } else if let ownPlayer = ownPlayer {
                    // Eigener Spieler ist besser oder gleich gut
                    selectedPlayer = (ownPlayer, nil)
                    print(
                        String(
                            format: "   👤 Position %d Slot %d: Team %@ (%.1f pts)", position, i,
                            ownPlayer.firstName, ownPlayer.averagePoints)
                    )
                } else {
                    // Kein Spieler verfügbar
                    selectedPlayer = (nil, nil)
                    print("   ❌ Position \(position) Slot \(i): No player available")
                }

                let slotScore =
                    selectedPlayer.own != nil
                    ? calculatePlayerScore(selectedPlayer.own!, forPosition: position)
                    : (selectedPlayer.market != nil
                        ? calculateMarketPlayerScore(selectedPlayer.market!, forPosition: position)
                        : 0.0)

                slots.append(
                    LineupSlot(
                        slotIndex: slotIndex,
                        positionType: position,
                        ownedPlayerId: selectedPlayer.own?.id,
                        recommendedMarketPlayerId: recommendation,
                        slotScore: slotScore
                    ))

                totalScore += slotScore
                slotIndex += 1
            }
        }

        let avgScore = slots.isEmpty ? 0.0 : totalScore / Double(slots.count)

        guard marketPlayersNeeded.count > 0 else {
            print("   ℹ️ No market players needed in hybrid lineup")
            return nil
        }

        return OptimalLineupResult(
            slots: slots,
            formationName: formationToString(formation),
            totalLineupScore: totalScore,
            isHybridWithMarketPlayers: true,
            marketPlayersNeeded: marketPlayersNeeded,
            totalMarketCost: totalMarketCost,
            averagePlayerScore: avgScore
        )
    }

    /// Findet den besten verfügbaren Markt-Spieler für eine Position
    private func findBestMarketPlayerForPosition(
        position: Int,
        marketPlayers: [MarketPlayer],
        alreadyUsedIds: [String],
        teamPlayerCounts: [String: Int],
        maxPlayersPerTeam: Int
    ) -> MarketPlayer? {
        return marketPlayers.first { player in
            // Nicht bereits ausgewählt
            guard !alreadyUsedIds.contains(player.id) else { return false }

            // Team-Limit nicht überschritten
            let currentTeamCount = teamPlayerCounts[player.teamId, default: 0]
            guard currentTeamCount < maxPlayersPerTeam else { return false }

            return true
        }
    }

    /// Berechnet Score für einen Team-Spieler
    private func calculatePlayerScore(_ player: TeamPlayer, forPosition position: Int) -> Double {
        // Basis-Scoring: averagePoints * Gewichtung + formTrend-Bonus
        var score = player.averagePoints * 2.0

        // Form-Trend-Bonus
        if player.marketValueTrend > 1_000_000 {
            score += 2.0  // Gute Form
        } else if player.marketValueTrend < -1_000_000 {
            score -= 2.0  // Schlechte Form
        }

        // Status-Malus
        if player.status == 1 {
            score -= 5.0  // Verletzt
        } else if player.status == 2 {
            score -= 2.0  // Angeschlagen
        }

        // Position-spezifische Gewichtung
        switch position {
        case 1:  // Torwart
            score *= 1.2
        case 4:  // Stürmer
            score *= 1.15
        default:
            break
        }

        return max(0.0, score)
    }

    /// Berechnet Score für einen Markt-Spieler
    private func calculateMarketPlayerScore(_ player: MarketPlayer, forPosition position: Int)
        -> Double
    {
        // Basis-Scoring: averagePoints * Gewichtung
        var score = player.averagePoints * 2.0

        // Form-Trend-Bonus
        if player.marketValueTrend > 1_000_000 {
            score += 2.0  // Gute Form
        } else if player.marketValueTrend < -1_000_000 {
            score -= 2.0  // Schlechte Form
        }

        // Status-Malus
        if player.status == 1 {
            score -= 5.0  // Verletzt
        } else if player.status == 2 {
            score -= 2.0  // Angeschlagen
        }

        // Position-spezifische Gewichtung
        switch position {
        case 1:  // Torwart
            score *= 1.2
        case 4:  // Stürmer
            score *= 1.15
        default:
            break
        }

        return max(0.0, score)
    }

    /// Konvertiert Formation Array zu String (z.B. [1,4,4,2] -> "4-2-3-1")
    private func formationToString(_ formation: [Int]) -> String {
        // [1, 4, 4, 2] -> "4-4-2-1" oder besser formatiert "4-2-3-1" (ohne Torwart in Standard-Notation)
        let withoutGoalkeeper = Array(formation.dropFirst())
        return withoutGoalkeeper.map { String($0) }.joined(separator: "-")
    }
}
