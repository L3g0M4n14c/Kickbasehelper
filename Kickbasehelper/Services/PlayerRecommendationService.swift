import Foundation

@MainActor
class PlayerRecommendationService: ObservableObject {
    private let kickbaseManager: KickbaseManager
    
    // Cache für Empfehlungen
    private var cachedRecommendations: [String: CachedRecommendations] = [:]
    private let cacheValidityDuration: TimeInterval = 300 // 5 Minuten Cache
    
    // Cache für Spieler-Statistiken (smdc, ismc, smc)
    private var playerStatsCache: [String: PlayerMatchStats] = [:]
    
    // Aktueller Spieltag (wird bei generateRecommendations gesetzt)
    private var currentMatchDay: Int = 10
    
    init(kickbaseManager: KickbaseManager) {
        self.kickbaseManager = kickbaseManager
    }
    
    // MARK: - Helper Structures
    
    private struct PlayerMatchStats {
        let smdc: Int      // Aktueller Spieltag
        let ismc: Int      // Spiele auf dem Platz (Startelf + Einwechslung)
        let smc: Int       // Spiele in Startelf (Starting Match Count)
    }
    
    // MARK: - Main Recommendation Functions
    
    func generateRecommendations(for league: League, budget: Int) async throws -> [TransferRecommendation] {
        print("🎯 Generating transfer recommendations for league: \(league.name)")
        
        // Prüfe Cache
        if let cached = cachedRecommendations[league.id],
           Date().timeIntervalSince(cached.timestamp) < cacheValidityDuration {
            print("✅ Returning cached recommendations (\(cached.recommendations.count) players)")
            return cached.recommendations
        }
        
        // Lade aktuelle Team-Spieler und Marktspieler PARALLEL
        async let teamPlayersTask = getTeamPlayers(for: league)
        async let marketPlayersTask = getMarketPlayers(for: league)
        
        let (teamPlayers, marketPlayers) = try await (teamPlayersTask, marketPlayersTask)
        
        // Hole aktuellen Spieltag und Stats von einem beliebigen Spieler
        let firstPlayerId = teamPlayers.first?.id ?? marketPlayers.first?.id
        
        if let playerId = firstPlayerId {
            if let stats = await kickbaseManager.authenticatedPlayerService.getMatchDayStats(leagueId: league.id, playerId: playerId) {
                currentMatchDay = stats.smdc
                // Speichere Stats für diesen Spieler im Cache
                playerStatsCache[playerId] = PlayerMatchStats(smdc: stats.smdc, ismc: stats.ismc, smc: stats.smc)
                print("✅ Current matchday from API: \(currentMatchDay)")
            } else {
                currentMatchDay = 10 // Fallback
                print("⚠️ Could not fetch matchday stats, using fallback: \(currentMatchDay)")
            }
        } else {
            currentMatchDay = 10
            print("⚠️ No players available to fetch matchday, using fallback: \(currentMatchDay)")
        }
        print("✅ Loaded \(teamPlayers.count) team players and \(marketPlayers.count) market players in parallel")
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
            
            return true
        }
        
        print("📊 Pre-filtered from \(marketPlayers.count) to \(qualityMarketPlayers.count) quality players")
        
        // BATCH-PROCESSING: Verarbeite Spieler in Batches
        let batchSize = 50
        var allRecommendations: [TransferRecommendation] = []
        
        for batchStart in stride(from: 0, to: qualityMarketPlayers.count, by: batchSize) {
            let batchEnd = min(batchStart + batchSize, qualityMarketPlayers.count)
            let batch = Array(qualityMarketPlayers[batchStart..<batchEnd])
            
            // Verarbeite Batch parallel
            let batchRecommendations = batch.compactMap { marketPlayer -> TransferRecommendation? in
                let analysis = analyzePlayer(marketPlayer, teamAnalysis: teamAnalysis)
                let recommendation = createRecommendation(marketPlayer: marketPlayer, analysis: analysis, teamAnalysis: teamAnalysis)
                
                // Nur Spieler mit gutem Score behalten
                return recommendation.recommendationScore >= 2.0 ? recommendation : nil
            }
            
            allRecommendations.append(contentsOf: batchRecommendations)
            print("📦 Processed batch \(batchStart/batchSize + 1): \(batchRecommendations.count) recommendations added")
        }
        
        print("✅ Generated \(allRecommendations.count) recommendations")
        
        // Sortiere nach Empfehlungswert und nimm Top 50 für Stats-Loading
        let topRecommendations = allRecommendations.sorted { $0.recommendationScore > $1.recommendationScore }.prefix(50)
        
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
        finalRecommendations = Array(finalRecommendations.sorted { $0.recommendationScore > $1.recommendationScore }.prefix(20))
        
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
    
    private func analyzeTeam(teamPlayers: [TeamPlayer], user: LeagueUser, budget: Int) -> TeamAnalysis {
        var positionCounts: [String: Int] = [:]
        var positionTotalPoints: [String: Double] = [:]
        
        // Analysiere aktuelle Teamzusammensetzung
        for player in teamPlayers {
            let position = String(player.position) // Convert Int to String
            positionCounts[position, default: 0] += 1
            positionTotalPoints[position, default: 0.0] += Double(player.totalPoints)
        }
        
        // Identifiziere schwache Positionen
        let weakPositions = identifyWeakPositions(positionCounts: positionCounts, positionTotalPoints: positionTotalPoints)
        let strengths = identifyStrongPositions(positionCounts: positionCounts, positionTotalPoints: positionTotalPoints)
        
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
    
    private func identifyWeakPositions(positionCounts: [String: Int], positionTotalPoints: [String: Double]) -> [TeamAnalysis.Position] {
        var weakPositions: [TeamAnalysis.Position] = []
        
        // Mindestanzahl Spieler pro Position
        let minPlayersPerPosition: [String: Int] = [
            "1": 1,    // Torwart
            "2": 3,    // Verteidiger
            "3": 6,    // Mittelfeld
            "4": 1     // Sturm
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
        
        return Array(Set(weakPositions)) // Entferne Duplikate
    }
    
    private func identifyStrongPositions(positionCounts: [String: Int], positionTotalPoints: [String: Double]) -> [TeamAnalysis.Position] {
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
    
    private func mapStringToPosition(_ positionStr: String) -> TeamAnalysis.Position? {
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
    
    private func analyzePlayer(_ marketPlayer: MarketPlayer, teamAnalysis: TeamAnalysis) -> PlayerAnalysis {
        let pointsPerGame = marketPlayer.averagePoints
        let valueForMoney = calculateValueForMoney(marketPlayer)
        let formTrend = calculateCurrentForm(marketPlayer)
        let injuryRisk = calculateInjuryRisk(marketPlayer)
        let seasonProjection = calculateSeasonProjection(marketPlayer)
        
        return PlayerAnalysis(
            pointsPerGame: pointsPerGame,
            valueForMoney: valueForMoney,
            formTrend: formTrend,
            injuryRisk: injuryRisk,
            upcomingFixtures: [], // Leer für jetzt
            seasonProjection: seasonProjection
        )
    }
    
    private func calculateCurrentForm(_ marketPlayer: MarketPlayer) -> PlayerAnalysis.FormTrend {
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
    
    private func calculateInjuryRisk(_ marketPlayer: MarketPlayer) -> PlayerAnalysis.InjuryRisk {
        // Basis-Risiko basierend auf Status
        if marketPlayer.status == 8 {
            return .high
        } else if marketPlayer.status == 4 {
            return .medium
        } else {
            return .low
        }
    }
    
    private func calculateSeasonProjection(_ marketPlayer: MarketPlayer) -> SeasonProjection {
        // Fallback-Berechnung ohne Stats (wird später mit echten Stats überschrieben)
        let currentPoints = marketPlayer.totalPoints
        let pointsPerGame = marketPlayer.averagePoints
        
        // Schätze gespielte Spiele aus Punkten
        let estimatedGamesPlayed = pointsPerGame > 0 ? Int(round(Double(currentPoints) / pointsPerGame)) : 0
        let remainingGames = max(34 - estimatedGamesPlayed, 0)
        
        let projectedTotal = currentPoints + Int(pointsPerGame * Double(remainingGames))
        let projectedValueIncrease = marketPlayer.marketValueTrend * remainingGames / 10
        
        // Niedrige Confidence, da keine echten Stats
        let confidence = 0.5
        
        return SeasonProjection(
            projectedTotalPoints: projectedTotal,
            projectedValueIncrease: projectedValueIncrease,
            confidence: confidence
        )
    }
    
    private func calculateSeasonProjectionWithStats(_ marketPlayer: MarketPlayer, stats: PlayerMatchStats) -> SeasonProjection {
        let currentPoints = marketPlayer.totalPoints
        let gamesPlayed = stats.ismc  // Spiele auf dem Platz (Startelf + Einwechslung)
        let pointsPerGame = marketPlayer.averagePoints
        let remainingGames = max(34 - gamesPlayed, 0)
        
        let projectedTotal = currentPoints + Int(pointsPerGame * Double(remainingGames))
        let projectedValueIncrease = marketPlayer.marketValueTrend * remainingGames / 10
        
        // Debug: Zeige Stats
        print("📊 Stats for \(marketPlayer.firstName) \(marketPlayer.lastName):")
        print("   smdc=\(stats.smdc), ismc=\(stats.ismc), smc=\(stats.smc)")
        
        // Confidence basiert auf Spielbeteiligung
        let confidence: Double
        if stats.smdc > 0 && gamesPlayed > 0 {
            let playedRatio = Double(gamesPlayed) / Double(stats.smdc)
            // Bonus für Stammkräfte (smc ~ ismc)
            let starterBonus = Double(stats.smc) / max(Double(stats.ismc), 1.0)
            confidence = min(playedRatio * (0.7 + starterBonus * 0.3), 1.0)
            
            print("   playedRatio=\(String(format: "%.2f", playedRatio)), starterBonus=\(String(format: "%.2f", starterBonus))")
            print("🎯 Confidence for \(marketPlayer.firstName) \(marketPlayer.lastName): \(gamesPlayed) played / \(stats.smdc) matchdays (started: \(stats.smc)) = \(String(format: "%.1f%%", confidence * 100))")
        } else {
            confidence = 0.0
            print("⚠️ No stats available for \(marketPlayer.firstName) \(marketPlayer.lastName)")
        }
        
        return SeasonProjection(
            projectedTotalPoints: projectedTotal,
            projectedValueIncrease: projectedValueIncrease,
            confidence: confidence
        )
    }
    
    /// Lädt Stats für eine Liste von Spielern (parallel, max 10 gleichzeitig)
    private func loadStatsForPlayers(_ recommendations: [TransferRecommendation], leagueId: String) async {
        // Nur Spieler laden, die noch nicht im Cache sind
        let playersToLoad = recommendations.filter { playerStatsCache[$0.player.id] == nil }
        
        guard !playersToLoad.isEmpty else {
            print("✅ All player stats already cached")
            return
        }
        
        print("📥 Loading stats for \(playersToLoad.count) players...")
        
        // Verarbeite in Batches von 10 parallel
        for batchStart in stride(from: 0, to: playersToLoad.count, by: 10) {
            let batchEnd = min(batchStart + 10, playersToLoad.count)
            let batch = Array(playersToLoad[batchStart..<batchEnd])
            
            // Lade Stats parallel
            await withTaskGroup(of: (String, PlayerMatchStats?).self) { group in
                for recommendation in batch {
                    group.addTask {
                        let stats = await self.kickbaseManager.authenticatedPlayerService.getMatchDayStats(
                            leagueId: leagueId,
                            playerId: recommendation.player.id
                        )
                        if let stats = stats {
                            return (recommendation.player.id, PlayerMatchStats(smdc: stats.smdc, ismc: stats.ismc, smc: stats.smc))
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
        }
        
        print("✅ Loaded stats for \(playerStatsCache.count) players total")
    }
    
    private func calculateValueForMoney(_ marketPlayer: MarketPlayer) -> Double {
        guard marketPlayer.price > 0 else { return 0.0 }
        let pointsPerMillion = Double(marketPlayer.totalPoints) / (Double(marketPlayer.price) / 1_000_000.0)
        return pointsPerMillion
    }
    
    // MARK: - Cache Helper
    
    func clearCache() {
        cachedRecommendations.removeAll()
        print("🗑️ Recommendations cache cleared")
    }
    
    func clearCacheForLeague(_ leagueId: String) {
        cachedRecommendations.removeValue(forKey: leagueId)
        print("🗑️ Cache cleared for league: \(leagueId)")
    }
    
    private func createRecommendation(marketPlayer: MarketPlayer, analysis: PlayerAnalysis, teamAnalysis: TeamAnalysis) -> TransferRecommendation {
        let score = calculateRecommendationScore(marketPlayer: marketPlayer, analysis: analysis, teamAnalysis: teamAnalysis)
        let riskLevel = determineRiskLevel(analysis: analysis)
        let priority = determinePriority(score: score, teamAnalysis: teamAnalysis, position: marketPlayer.position)
        let reasons = generateReasons(marketPlayer: marketPlayer, analysis: analysis, teamAnalysis: teamAnalysis)
        
        return TransferRecommendation(
            player: marketPlayer,
            recommendationScore: score,
            reasons: reasons,
            analysis: analysis,
            riskLevel: riskLevel,
            priority: priority
        )
    }
    
    private func calculateRecommendationScore(marketPlayer: MarketPlayer, analysis: PlayerAnalysis, teamAnalysis: TeamAnalysis) -> Double {
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
        if marketPlayer.marketValueTrend > 1000000 {
            score += 2.0  // Stark steigender Marktwert
        } else if marketPlayer.marketValueTrend > 500000 {
            score += 1.0  // Steigender Marktwert
        } else if marketPlayer.marketValueTrend < -1000000 {
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
        
        return max(score, 0.0)
    }
    
    private func mapIntToPosition(_ position: Int) -> TeamAnalysis.Position? {
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
    
    private func determineRiskLevel(analysis: PlayerAnalysis) -> TransferRecommendation.RiskLevel {
        switch analysis.injuryRisk {
        case .high:
            return .high
        case .medium:
            return analysis.formTrend == .declining ? .high : .medium
        case .low:
            return analysis.formTrend == .declining ? .medium : .low
        }
    }
    
    private func determinePriority(score: Double, teamAnalysis: TeamAnalysis, position: Int) -> TransferRecommendation.Priority {
        if let playerPosition = mapIntToPosition(position) {
            if teamAnalysis.weakPositions.contains(playerPosition) && score > 7.0 {
                return .essential
            } else if score > 6.0 {
                return .recommended
            } else {
                return .optional
            }
        }
        return .optional
    }
    
    private func generateReasons(marketPlayer: MarketPlayer, analysis: PlayerAnalysis, teamAnalysis: TeamAnalysis) -> [RecommendationReason] {
        var reasons: [RecommendationReason] = []
        
        // Performance Reasoning
        if analysis.pointsPerGame > 100.0 {
            reasons.append(RecommendationReason(
                type: .performance,
                description: "Starke Leistung mit \(String(format: "%.1f", analysis.pointsPerGame)) Punkten pro Spiel",
                impact: 8.0
            ))
        }
        
        // Value Reasoning
        if analysis.valueForMoney > 30.0 {
            reasons.append(RecommendationReason(
                type: .value,
                description: "Gutes Preis-Leistungs-Verhältnis",
                impact: 7.0
            ))
        }
        
        // Form Reasoning
        switch analysis.formTrend {
        case .improving:
            reasons.append(RecommendationReason(
                type: .form,
                description: "Steigende Form und Marktwert",
                impact: 9.0
            ))
        case .declining:
            reasons.append(RecommendationReason(
                type: .form,
                description: "Achtung: Fallende Form",
                impact: -3.0
            ))
        case .stable:
            break
        }
        
        // Team Need Reasoning
        if let playerPosition = mapIntToPosition(marketPlayer.position) {
            if teamAnalysis.weakPositions.contains(playerPosition) {
                reasons.append(RecommendationReason(
                    type: .teamNeed,
                    description: "Verstärkt schwache Position",
                    impact: 8.5
                ))
            }
        }
        
        return reasons
    }
}

// MARK: - Cache Structure

private struct CachedRecommendations {
    let recommendations: [TransferRecommendation]
    let timestamp: Date
}

