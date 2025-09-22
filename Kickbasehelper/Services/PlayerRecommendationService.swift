import Foundation

@MainActor
class PlayerRecommendationService: ObservableObject {
    private let kickbaseManager: KickbaseManager
    
    init(kickbaseManager: KickbaseManager) {
        self.kickbaseManager = kickbaseManager
    }
    
    // MARK: - Main Recommendation Functions
    
    func generateRecommendations(for league: League, budget: Int) async throws -> [TransferRecommendation] {
        print("🎯 Generating transfer recommendations for league: \(league.name)")
        
        // Lade aktuelle Team-Spieler und Marktspieler
        let teamPlayers = try await getTeamPlayers(for: league)
        let marketPlayers = try await getMarketPlayers(for: league)
        let currentUser = getCurrentUser()
        
        // Analysiere das Team
        let teamAnalysis = analyzeTeam(teamPlayers: teamPlayers, user: currentUser, budget: budget)
        
        // VERBESSERTE FILTERUNG: Nur wirklich gute Spieler (aber nicht zu streng)
        let qualityMarketPlayers = marketPlayers.filter { player in
            // Mindestleistung: 4+ Punkte pro Spiel (reduziert von 5.0)
            let pointsPerGame = Double(player.totalPoints) / max(Double(player.number), 1.0)
            guard pointsPerGame >= 4.0 else { return false }
            
            // Mindestanzahl Spiele (mindestens 3 Spiele gespielt - reduziert von 5)
            guard player.number >= 3 else { return false }
            
            // Nicht verletzt oder gesperrt
            guard player.status != 8 && player.status != 16 else { return false }
            
            // Im Budget
            guard player.price <= budget else { return false }
            
            // Mindestpunktzahl insgesamt (reduziert von 25 auf 15)
            guard player.totalPoints >= 15 else { return false }
            
            return true
        }
        
        print("📊 Filtered from \(marketPlayers.count) to \(qualityMarketPlayers.count) quality players")
        
        var recommendations: [TransferRecommendation] = []
        var scoredPlayers: [(MarketPlayer, Double)] = []
        
        for marketPlayer in qualityMarketPlayers {
            let analysis = analyzePlayer(marketPlayer, teamAnalysis: teamAnalysis)
            let recommendation = createRecommendation(marketPlayer: marketPlayer, analysis: analysis, teamAnalysis: teamAnalysis)
            
            scoredPlayers.append((marketPlayer, recommendation.recommendationScore))
            
            // Temporär alle Spieler mit Score >= 2.0 aufnehmen (weiter reduziert)
            if recommendation.recommendationScore >= 2.0 {
                recommendations.append(recommendation)
            }
        }
        
        // Debug: Zeige die Top-Scorer
        let topScorers = scoredPlayers.sorted { $0.1 > $1.1 }.prefix(5)
        print("🏆 Top 5 scored players:")
        for (player, score) in topScorers {
            print("   \(player.firstName) \(player.lastName): Score \(String(format: "%.2f", score))")
        }
        
        print("✅ Generated \(recommendations.count) recommendations from \(scoredPlayers.count) scored players")
        
        // Sortiere nach Empfehlungswert (höchste zuerst) und limitiere auf Top 20
        return Array(recommendations.sorted { $0.recommendationScore > $1.recommendationScore }.prefix(20))
    }
    
    // MARK: - Helper Functions
    
    private func getTeamPlayers(for league: League) async throws -> [TeamPlayer] {
        return try await kickbaseManager.authenticatedPlayerService.loadTeamPlayers(for: league)
    }
    
    private func getMarketPlayers(for league: League) async throws -> [MarketPlayer] {
        return try await kickbaseManager.authenticatedPlayerService.loadMarketPlayers(for: league)
    }
    
    private func getCurrentUser() -> User {
        // Erstelle einen Standard-User basierend auf der tatsächlichen User-Struktur
        return User(
            id: "current_user",
            name: "Current User",
            teamName: "Mein Team",
            email: "user@example.com",
            budget: 50000000, // Standard-Budget in Cent
            teamValue: 40000000,
            points: 0,
            placement: 1,
            flags: 0
        )
    }
    
    // MARK: - Analysis Functions
    
    private func analyzeTeam(teamPlayers: [TeamPlayer], user: User, budget: Int) -> TeamAnalysis {
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
            "1": 2,    // Torwart
            "2": 4,    // Verteidiger
            "3": 6,    // Mittelfeld
            "4": 3     // Sturm
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
        let pointsPerGame = Double(marketPlayer.totalPoints) / max(Double(marketPlayer.number), 1.0)
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
        let pointsPerGame = Double(marketPlayer.totalPoints) / max(Double(marketPlayer.number), 1.0)
        
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
        let currentPoints = marketPlayer.totalPoints
        let gamesPlayed = max(marketPlayer.number, 1)
        let pointsPerGame = Double(currentPoints) / Double(gamesPlayed)
        let remainingGames = 34 - gamesPlayed
        
        let projectedTotal = currentPoints + Int(pointsPerGame * Double(max(remainingGames, 0)))
        let projectedValueIncrease = marketPlayer.marketValueTrend * max(remainingGames, 0) / 10
        
        return SeasonProjection(
            projectedTotalPoints: projectedTotal,
            projectedValueIncrease: projectedValueIncrease,
            confidence: min(Double(gamesPlayed) / 10.0, 1.0)
        )
    }
    
    private func calculateValueForMoney(_ marketPlayer: MarketPlayer) -> Double {
        let pointsPerMillion = Double(marketPlayer.totalPoints) / (Double(marketPlayer.price) / 1_000_000.0)
        return pointsPerMillion
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
        
        // 6. Verletzungsrisiko (verschärft)
        switch analysis.injuryRisk {
        case .low:
            score += 1.5  // Erhöht von 1.0
        case .medium:
            score -= 0.5  // Verschlechtert
        case .high:
            score -= 3.0  // Verschlechtert von -2.0
        }
        
        // 7. Team-Need Bonus (wichtiger gemacht)
        if let playerPosition = mapIntToPosition(marketPlayer.position) {
            if teamAnalysis.weakPositions.contains(playerPosition) {
                score += 4.0  // Erhöht von 3.0
            } else if teamAnalysis.strengths.contains(playerPosition) {
                score += 0.5  // Reduziert von 1.0
            } else {
                score += 2.0  // Gleich
            }
        }
        
        // 8. Spiele-Konsistenz Bonus
        let gamesPlayed = Double(marketPlayer.number)
        if gamesPlayed >= 15 {
            score += 1.0  // Konsistent viele Spiele
        } else if gamesPlayed >= 10 {
            score += 0.5
        } else if gamesPlayed < 5 {
            score -= 1.0  // Zu wenige Spiele
        }
        
        // 9. Preis-Effizienz (neue Kategorie)
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
        if analysis.pointsPerGame > 8.0 {
            reasons.append(RecommendationReason(
                type: .performance,
                description: "Starke Leistung mit \(String(format: "%.1f", analysis.pointsPerGame)) Punkten pro Spiel",
                impact: 8.0
            ))
        }
        
        // Value Reasoning
        if analysis.valueForMoney > 8.0 {
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
        
        // Injury Risk Reasoning
        switch analysis.injuryRisk {
        case .high:
            reasons.append(RecommendationReason(
                type: .injury,
                description: "⚠️ Verletzungsrisiko beachten",
                impact: -5.0
            ))
        case .medium:
            reasons.append(RecommendationReason(
                type: .injury,
                description: "Mittleres Verletzungsrisiko",
                impact: -2.0
            ))
        case .low:
            break
        }
        
        return reasons
    }
}
