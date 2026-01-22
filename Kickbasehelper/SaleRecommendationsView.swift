import KickbaseCore
import SwiftUI

struct SaleRecommendationsView: View {
    @ObservedObject var kickbaseManager: KickbaseManager
    @EnvironmentObject var ligainsiderService: LigainsiderService
    @StateObject private var recommendationService: PlayerRecommendationService
    @State private var selectedGoal: SaleRecommendationGoal = .balanceBudget
    @State private var recommendations: [SaleRecommendation] = []
    @State private var isLoading = false
    @State private var errorMessage: String?

    init(kickbaseManager: KickbaseManager) {
        self.kickbaseManager = kickbaseManager
        self._recommendationService = StateObject(
            wrappedValue: PlayerRecommendationService(kickbaseManager: kickbaseManager))
    }

    var body: some View {
        VStack(spacing: 0) {
            // Budget Info Header
            if let stats = kickbaseManager.userStats {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Aktuelles Budget")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(formatCurrencyForSales(stats.budget))
                            .font(.headline)
                            .fontWeight(.bold)
                            .foregroundColor(stats.budget >= 0 ? .green : .red)
                    }

                    Spacer()

                    VStack(alignment: .trailing, spacing: 4) {
                        Text("Status")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        HStack(spacing: 4) {
                            Image(
                                systemName: stats.budget >= 0
                                    ? "checkmark.circle.fill" : "exclamationmark.circle.fill"
                            )
                            .foregroundColor(stats.budget >= 0 ? .green : .red)
                            Text(stats.budget >= 0 ? "Im Plus" : "Im Minus")
                                .font(.subheadline)
                                .fontWeight(.semibold)
                        }
                    }
                }
                .padding()
                .background(Color(.systemGray6))
            }

            // Goal Selection
            VStack(spacing: 12) {
                Text("Verkaufs-Empfehlungen")
                    .font(.headline)
                    .padding(.bottom, 4)

                // Ziel-Buttons
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(SaleRecommendationGoal.allCases, id: \.self) { goal in
                            GoalSelectionButton(
                                goal: goal,
                                isSelected: selectedGoal == goal
                            ) {
                                selectedGoal = goal
                                Task {
                                    await loadRecommendations()
                                }
                            }
                        }
                    }
                    .padding(.horizontal)
                }
            }
            .padding()
            .background(Color(.systemGray6))

            // Content
            if isLoading {
                VStack(spacing: 20) {
                    Spacer()
                    ProgressView()
                        .scaleEffect(1.5)
                    Text("Generiere Empfehlungen...")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Spacer()
                }
            } else if let errorMessage = errorMessage {
                VStack(spacing: 15) {
                    Spacer()
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 50))
                        .foregroundColor(.orange)
                    Text("Fehler")
                        .font(.headline)
                    Text(errorMessage)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                    Spacer()
                }
                .padding()
            } else if recommendations.isEmpty {
                VStack(spacing: 15) {
                    Spacer()
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 50))
                        .foregroundColor(.green)
                    Text("Keine Empfehlungen")
                        .font(.headline)
                    Text("Für dieses Ziel gibt es derzeit keine Empfehlungen.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Spacer()
                }
                .padding()
            } else {
                ScrollView {
                    VStack(spacing: 12) {
                        ForEach(recommendations) { recommendation in
                            SaleRecommendationCard(
                                recommendation: recommendation,
                                currentBudget: kickbaseManager.userStats?.budget ?? 0
                            )
                        }
                    }
                    .padding()
                }
            }
        }
        .task {
            await loadRecommendations()
        }
        .onChange(of: kickbaseManager.selectedLeague) { _, _ in
            Task {
                await loadRecommendations()
            }
        }
    }

    private func loadRecommendations() async {
        guard let league = kickbaseManager.selectedLeague else {
            errorMessage = "Keine Liga ausgewählt"
            return
        }

        isLoading = true
        errorMessage = nil
        recommendations = []

        do {
            let teamPlayers = try await recommendationService.getTeamPlayersSync(for: league)
            let marketPlayers = try await recommendationService.getMarketPlayersSync(for: league)
            // Wichtig: Aktuelles Budget aus userStats verwenden, nicht league.currentUser.budget (das ist das Startbudget)
            let budget = kickbaseManager.userStats?.budget ?? league.currentUser.budget

            let recs = await recommendationService.generateSaleRecommendations(
                for: league,
                goal: selectedGoal,
                teamPlayers: teamPlayers,
                marketPlayers: marketPlayers,
                currentBudget: budget
            )

            await MainActor.run {
                recommendations = recs
                isLoading = false
            }
        } catch {
            await MainActor.run {
                errorMessage = error.localizedDescription
                isLoading = false
            }
        }
    }
}

// MARK: - Goal Selection Button

struct GoalSelectionButton: View {
    let goal: SaleRecommendationGoal
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: goal.icon)
                    .font(.system(size: 18))
                Text(goal.rawValue)
                    .font(.caption2)
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
            }
            .frame(minWidth: 70, minHeight: 60)
            .padding(.horizontal, 8)
            .padding(.vertical, 6)
            .background(isSelected ? Color.blue : Color(.systemGray5))
            .foregroundColor(isSelected ? .white : .primary)
            .cornerRadius(10)
        }
    }
}

// MARK: - Sale Recommendation Card

struct SaleRecommendationCard: View {
    let recommendation: SaleRecommendation
    @State private var expandedReplacements = false
    let currentBudget: Int
    @EnvironmentObject var ligainsiderService: LigainsiderService

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Budget Info
            if let bestReplacement = recommendation.bestReplacement,
                bestReplacement.budgetSavings > 0
            {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Aktuelles Budget")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Text(formatCurrencyForSales(currentBudget))
                                .font(.headline)
                                .fontWeight(.bold)
                                .foregroundColor(currentBudget >= 0 ? .green : .red)
                        }

                        Spacer()

                        VStack(alignment: .center, spacing: 2) {
                            Image(systemName: "arrow.right")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .frame(maxWidth: 30)

                        Spacer()

                        VStack(alignment: .trailing, spacing: 2) {
                            Text("Nach Empfehlung")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            let budgetAfter = currentBudget + bestReplacement.budgetSavings
                            Text(formatCurrencyForSales(budgetAfter))
                                .font(.headline)
                                .fontWeight(.bold)
                                .foregroundColor(budgetAfter >= 0 ? .green : .red)
                        }
                    }

                    HStack(spacing: 8) {
                        Image(systemName: "plus.circle.fill")
                            .font(.caption)
                            .foregroundColor(.green)
                        Text("+\(formatCurrencyForSales(bestReplacement.budgetSavings))")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(.green)
                        Spacer()
                    }
                }
                .padding()
                .background(Color.green.opacity(0.05))
                .cornerRadius(10)
            }

            // Header: Spieler zum Verkaufen
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Label("Zum Verkaufen", systemImage: "arrow.up.right.square")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        HStack(spacing: 8) {
                            Text(recommendation.playerToSell.fullName)
                                .font(.headline)
                                .fontWeight(.semibold)

                            // Ligainsider Status Icon
                            let status = ligainsiderService.getPlayerStatus(
                                firstName: recommendation.playerToSell.firstName,
                                lastName: recommendation.playerToSell.lastName)
                            if status != .out {
                                Image(systemName: ligainsiderService.getIcon(for: status))
                                    .foregroundColor(
                                        Color(ligainsiderService.getColor(for: status))
                                    )
                                    .font(.caption)
                            }

                            Text(positionAbbreviation(recommendation.playerToSell.position))
                                .font(.caption2)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(positionColor(recommendation.playerToSell.position))
                                .cornerRadius(4)

                            Spacer()

                            SalesPriorityBadge(priority: recommendation.priority)
                        }
                    }

                    Spacer()

                    VStack(alignment: .trailing, spacing: 2) {
                        Text("Verkaufswert")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        Text("€\(recommendation.playerToSell.marketValue / 1_000_000)M")
                            .font(.headline)
                            .fontWeight(.bold)
                    }
                }

                // Spieler Stats
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Punkte")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        Text("\(recommendation.playerToSell.totalPoints)")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text("Ø/Spiel")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        Text(
                            String(
                                format: "%.1f", Double(recommendation.playerToSell.averagePoints))
                        )
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    }

                    Spacer()

                    if recommendation.playerToSell.status == 1 {
                        HStack(spacing: 4) {
                            Image(systemName: "cross.circle.fill")
                                .foregroundColor(.red)
                            Text("Verletzt")
                                .font(.caption2)
                                .foregroundColor(.red)
                        }
                    } else if recommendation.playerToSell.status == 2 {
                        HStack(spacing: 4) {
                            Image(systemName: "pills.fill")
                                .foregroundColor(.orange)
                            Text("Angeschlagen")
                                .font(.caption2)
                                .foregroundColor(.orange)
                        }
                    }
                }
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(10)

            // Erklärung
            VStack(alignment: .leading, spacing: 6) {
                Label("Warum?", systemImage: "lightbulb")
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(.blue)

                Text(recommendation.explanation)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(3)
            }
            .padding()
            .background(Color.blue.opacity(0.05))
            .cornerRadius(10)

            // Ersatz-Spieler
            if let bestReplacement = recommendation.bestReplacement {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Label("Empfohlener Ersatz", systemImage: "star.fill")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        Spacer()

                        if expandedReplacements {
                            Text("Alle anzeigen (\(recommendation.replacements.count))")
                                .font(.caption2)
                                .foregroundColor(.blue)
                        }
                    }

                    ReplacementPlayerCard(
                        replacement: bestReplacement, originalPlayer: recommendation.playerToSell)

                    // Weitere Optionen (wenn mehr als 1 Ersatz)
                    if recommendation.replacements.count > 1 && expandedReplacements {
                        Divider()
                            .padding(.vertical, 4)

                        ForEach(recommendation.replacements.dropFirst()) { replacement in
                            ReplacementPlayerCard(
                                replacement: replacement,
                                originalPlayer: recommendation.playerToSell)
                        }
                    }

                    // Toggle für mehr Optionen
                    if recommendation.replacements.count > 1 {
                        Button(action: { expandedReplacements.toggle() }) {
                            HStack {
                                Image(
                                    systemName: expandedReplacements ? "chevron.up" : "chevron.down"
                                )
                                Text(
                                    expandedReplacements
                                        ? "Weniger anzeigen"
                                        : "\(recommendation.replacements.count - 1) weitere Option(en)"
                                )
                                Spacer()
                            }
                            .font(.caption)
                            .foregroundColor(.blue)
                        }
                    }
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.05), radius: 2, x: 0, y: 1)
    }
}

// MARK: - Replacement Player Card

struct ReplacementPlayerCard: View {
    let replacement: ReplacementSuggestion
    let originalPlayer: TeamPlayer
    @EnvironmentObject var ligainsiderService: LigainsiderService

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        Text(replacement.player.firstName + " " + replacement.player.lastName)
                            .font(.subheadline)
                            .fontWeight(.semibold)

                        // Ligainsider Status Icon
                        let status = ligainsiderService.getPlayerStatus(
                            firstName: replacement.player.firstName,
                            lastName: replacement.player.lastName)
                        if status != .out {
                            Image(systemName: ligainsiderService.getIcon(for: status))
                                .foregroundColor(
                                    Color(ligainsiderService.getColor(for: status))
                                )
                                .font(.caption)
                        }

                        Text(positionAbbreviation(replacement.player.position))
                            .font(.caption2)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(positionColor(replacement.player.position))
                            .cornerRadius(4)
                    }

                    Text(replacement.player.fullTeamName)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    Text(formatCurrencyForSales(replacement.player.price))
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.green)

                    if replacement.budgetSavings > 0 {
                        Text("-\(formatCurrencyForSales(replacement.budgetSavings))")
                            .font(.caption2)
                            .foregroundColor(.green)
                    }
                }
            }

            // Vergleich
            HStack(spacing: 12) {
                ComparisonItem(
                    title: "Punkte/Spiel",
                    original: String(format: "%.1f", Double(originalPlayer.averagePoints)),
                    replacement: String(format: "%.1f", replacement.player.averagePoints),
                    gain: replacement.performanceGain
                )

                ComparisonItem(
                    title: "Gesamtpunkte",
                    original: "\(originalPlayer.totalPoints)",
                    replacement: "\(replacement.player.totalPoints)",
                    gain: Double(replacement.player.totalPoints - originalPlayer.totalPoints)
                )

                Spacer()
            }
            .font(.caption)
        }
    }
}

struct ComparisonItem: View {
    let title: String
    let original: String
    let replacement: String
    let gain: Double

    var gainColor: Color {
        if gain > 0 { return .green } else if gain < 0 { return .red } else { return .gray }
    }

    var gainIcon: String {
        if gain > 0 {
            return "arrow.up"
        } else if gain < 0 {
            return "arrow.down"
        } else {
            return "minus"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .foregroundColor(.secondary)

            HStack(spacing: 4) {
                Text(original)
                    .foregroundColor(.secondary)

                Image(systemName: gainIcon)
                    .foregroundColor(gainColor)
                    .font(.caption2)

                Text(replacement)
                    .fontWeight(.semibold)
            }
        }
        .padding(6)
        .background(Color(.systemGray6))
        .cornerRadius(6)
    }
}

// MARK: - Helper Functions

func positionAbbreviation(_ position: Int) -> String {
    switch position {
    case 1: return "TW"
    case 2: return "ABW"
    case 3: return "MF"
    case 4: return "ST"
    default: return "?"
    }
}

func positionColor(_ position: Int) -> Color {
    switch position {
    case 1: return .yellow
    case 2: return .green
    case 3: return .blue
    case 4: return .red
    default: return .gray
    }
}

func formatCurrencyForSales(_ value: Int) -> String {
    let absValue = abs(value)
    if absValue >= 1_000_000 {
        return String(format: "€%.1fM", Double(value) / 1_000_000)
    } else if absValue >= 1_000 {
        let kValue = Double(value) / 1_000
        // Wenn es mehr als 1000k ist, in Millionen konvertieren
        if kValue >= 1_000 {
            return String(format: "€%.1fM", kValue / 1_000)
        }
        return String(format: "€%.1fk", kValue)
    } else {
        return "€\(value)"
    }
}

// MARK: - Priority Badge

struct SalesPriorityBadge: View {
    let priority: TransferRecommendation.Priority

    var body: some View {
        HStack(spacing: 3) {
            Image(systemName: priorityIcon)
                .font(.caption2)
            Text(priorityLabel)
                .font(.caption2)
                .fontWeight(.semibold)
        }
        .foregroundColor(priorityTextColor)
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(priorityBgColor)
        .cornerRadius(6)
    }

    var priorityLabel: String {
        switch priority {
        case .essential: return "Essentiell"
        case .recommended: return "Empfohlen"
        case .optional: return "Optional"
        @unknown default: return "Unbekannt"
        }
    }

    var priorityIcon: String {
        switch priority {
        case .essential: return "exclamationmark.2"
        case .recommended: return "exclamationmark"
        case .optional: return "checkmark"
        @unknown default: return "questionmark"
        }
    }

    var priorityTextColor: Color {
        switch priority {
        case .essential: return .red
        case .recommended: return .orange
        case .optional: return .blue
        @unknown default: return .gray
        }
    }

    var priorityBgColor: Color {
        switch priority {
        case .essential: return .red.opacity(0.1)
        case .recommended: return .orange.opacity(0.1)
        case .optional: return .blue.opacity(0.1)
        @unknown default: return .gray.opacity(0.1)
        }
    }
}

#Preview {
    SaleRecommendationsView(kickbaseManager: KickbaseManager())
}
