import SwiftUI

struct TransferRecommendationsView: View {
    @ObservedObject var kickbaseManager: KickbaseManager
    @StateObject private var recommendationService: PlayerRecommendationService
    @State private var recommendations: [TransferRecommendation] = []
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var filters = RecommendationFilters()
    @State private var sortOption: SortOption = .recommendationScore
    
    init(kickbaseManager: KickbaseManager) {
        self.kickbaseManager = kickbaseManager
        self._recommendationService = StateObject(wrappedValue: PlayerRecommendationService(kickbaseManager: kickbaseManager))
    }
    
    var body: some View {
        NavigationView {
            VStack {
                if isLoading {
                    ProgressView("Lade Transferempfehlungen...")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if recommendations.isEmpty {
                    emptyStateView
                } else {
                    recommendationsContent
                }
            }
            .navigationTitle("Transfer-Empfehlungen")
            .navigationBarItems(trailing: 
                Button("Aktualisieren") {
                    Task {
                        await loadRecommendations()
                    }
                }
                .disabled(isLoading)
            )
            .task {
                await loadRecommendations()
            }
        }
    }
    
    private var emptyStateView: some View {
        VStack(spacing: 20) {
            Image(systemName: "person.crop.circle.badge.plus")
                .font(.system(size: 60))
                .foregroundColor(.gray)
            
            Text("Keine Empfehlungen verfügbar")
                .font(.title2)
                .fontWeight(.medium)
            
            Text("Wählen Sie eine Liga aus und stellen Sie sicher, dass Transfermarkt-Daten verfügbar sind.")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            if let errorMessage = errorMessage {
                Text("Fehler: \(errorMessage)")
                    .font(.caption)
                    .foregroundColor(.red)
                    .padding()
                    .background(Color.red.opacity(0.1))
                    .cornerRadius(8)
            }
            
            Button("Erneut versuchen") {
                Task {
                    await loadRecommendations()
                }
            }
            .buttonStyle(.bordered)
        }
        .padding()
    }
    
    private var recommendationsContent: some View {
        VStack {
            // Team Analysis Header
            teamAnalysisHeader
            
            // Filters and Sort
            filtersAndSortSection
            
            // Recommendations List
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(filteredAndSortedRecommendations) { recommendation in
                        RecommendationCard(recommendation: recommendation)
                    }
                }
                .padding()
            }
        }
    }
    
    private var teamAnalysisHeader: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Team-Analyse")
                    .font(.headline)
                Spacer()
                Text("Budget: \(formatCurrency(kickbaseManager.selectedLeague?.currentUser.budget ?? 0))")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            if !recommendations.isEmpty {
                // Team summary based on first recommendation's team analysis (simplified)
                HStack {
                    Label("Schwächen erkannt", systemImage: "exclamationmark.triangle")
                        .font(.caption)
                        .foregroundColor(.orange)
                    
                    Spacer()
                    
                    Text("\(recommendations.count) Empfehlungen")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
        .padding(.horizontal)
    }
    
    private var filtersAndSortSection: some View {
        VStack {
            // Filter Controls
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    FilterChip(title: "Alle Positionen", isSelected: filters.positions.isEmpty) {
                        filters.positions.removeAll()
                    }
                    
                    ForEach(TeamAnalysis.Position.allCases, id: \.self) { position in
                        FilterChip(title: position.rawValue, isSelected: filters.positions.contains(position)) {
                            if filters.positions.contains(position) {
                                filters.positions.remove(position)
                            } else {
                                filters.positions.insert(position)
                            }
                        }
                    }
                    
                    FilterChip(title: "Niedriges Risiko", isSelected: filters.maxRisk == .low) {
                        filters.maxRisk = filters.maxRisk == .low ? .high : .low
                    }
                    
                    FilterChip(title: "Nur Empfohlen", isSelected: filters.minPriority == .recommended) {
                        filters.minPriority = filters.minPriority == .recommended ? .optional : .recommended
                    }
                }
                .padding(.horizontal)
            }
            
            // Sort Options
            Picker("Sortierung", selection: $sortOption) {
                Text("Empfehlungswert").tag(SortOption.recommendationScore)
                Text("Preis").tag(SortOption.price)
                Text("Punkte").tag(SortOption.points)
                Text("Preis-Leistung").tag(SortOption.valueForMoney)
                Text("Risiko").tag(SortOption.risk)
            }
            .pickerStyle(SegmentedPickerStyle())
            .padding(.horizontal)
        }
        .padding(.vertical, 8)
    }
    
    private var filteredAndSortedRecommendations: [TransferRecommendation] {
        let filtered = recommendations.filter { recommendation in
            // Position filter
            if !filters.positions.isEmpty {
                let playerPosition = mapIntToPosition(recommendation.player.position)
                if let playerPosition = playerPosition {
                    if !filters.positions.contains(playerPosition) {
                        return false
                    }
                }
            }
            
            // Risk filter
            if recommendation.riskLevel.rawValue > filters.maxRisk.rawValue {
                return false
            }
            
            // Priority filter
            if recommendation.priority.rawValue < filters.minPriority.rawValue {
                return false
            }
            
            // Price filter
            if let maxPrice = filters.maxPrice, recommendation.player.price > maxPrice {
                return false
            }
            
            // Points filter
            if let minPoints = filters.minPoints, recommendation.player.totalPoints < minPoints {
                return false
            }
            
            return true
        }
        
        return filtered.sorted { first, second in
            switch sortOption {
            case .recommendationScore:
                return first.recommendationScore > second.recommendationScore
            case .price:
                return first.player.price < second.player.price
            case .points:
                return first.player.totalPoints > second.player.totalPoints
            case .valueForMoney:
                return first.analysis.valueForMoney > second.analysis.valueForMoney
            case .risk:
                return first.riskLevel.rawValue < second.riskLevel.rawValue
            }
        }
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
    
    private func loadRecommendations() async {
        guard let selectedLeague = kickbaseManager.selectedLeague else {
            errorMessage = "Keine Liga ausgewählt"
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        do {
            let budget = selectedLeague.currentUser.budget
            print("🎯 Loading recommendations with budget: \(budget)")
            let results = try await recommendationService.generateRecommendations(for: selectedLeague, budget: budget)
            await MainActor.run {
                self.recommendations = results
                self.isLoading = false
                print("✅ Loaded \(results.count) recommendations")
            }
        } catch {
            await MainActor.run {
                self.errorMessage = error.localizedDescription
                self.isLoading = false
                print("❌ Error loading recommendations: \(error)")
            }
        }
    }
    
    private func formatCurrency(_ amount: Int) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.locale = Locale(identifier: "de_DE")
        formatter.currencyCode = "EUR"
        return formatter.string(from: NSNumber(value: Double(amount) / 100.0)) ?? "€0"
    }
}

// MARK: - Supporting Views and Models

struct RecommendationCard: View {
    let recommendation: TransferRecommendation
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Player Header
            HStack {
                VStack(alignment: .leading) {
                    Text(recommendation.player.firstName + " " + recommendation.player.lastName)
                        .font(.headline)
                    
                    HStack {
                        Text(recommendation.player.teamName)
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        Spacer()
                        
                        Text(positionName(for: recommendation.player.position))
                            .font(.caption)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(Color.blue.opacity(0.2))
                            .cornerRadius(4)
                    }
                }
                
                Spacer()
                
                VStack(alignment: .trailing) {
                    Text(formatPrice(recommendation.player.price))
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    HStack {
                        RiskBadge(risk: recommendation.riskLevel)
                        PriorityBadge(priority: recommendation.priority)
                    }
                }
            }
            
            // Stats Row
            HStack {
                StatItem(title: "Punkte", value: "\(recommendation.player.totalPoints)")
                StatItem(title: "Ø Punkte", value: String(format: "%.1f", recommendation.analysis.pointsPerGame))
                StatItem(title: "Wert/€M", value: String(format: "%.1f", recommendation.analysis.valueForMoney))
                StatItem(title: "Score", value: String(format: "%.1f", recommendation.recommendationScore))
            }
            
            // Reasons
            if !recommendation.reasons.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Gründe:")
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.secondary)
                    
                    ForEach(recommendation.reasons.prefix(3)) { reason in
                        HStack {
                            Image(systemName: iconForReasonType(reason.type))
                                .font(.caption)
                                .foregroundColor(colorForImpact(reason.impact))
                            
                            Text(reason.description)
                                .font(.caption)
                                .foregroundColor(.secondary)
                            
                            Spacer()
                        }
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.1), radius: 2, x: 0, y: 1)
    }
    
    private func positionName(for position: Int) -> String {
        switch position {
        case 1: return "TW"
        case 2: return "ABW"
        case 3: return "MF"
        case 4: return "ST"
        default: return "?"
        }
    }
    
    private func formatPrice(_ price: Int) -> String {
        return "€\(String(format: "%.1f", Double(price) / 1_000_000.0))M"
    }
    
    private func iconForReasonType(_ type: RecommendationReason.ReasonType) -> String {
        switch type {
        case .performance: return "chart.line.uptrend.xyaxis"
        case .value: return "eurosign.circle"
        case .potential: return "star"
        case .teamNeed: return "person.crop.circle.badge.plus"
        case .injury: return "cross.case"
        case .form: return "waveform.path.ecg"
        case .opponent: return "sportscourt"
        }
    }
    
    private func colorForImpact(_ impact: Double) -> Color {
        if impact > 5 { return .green }
        else if impact > 0 { return .blue }
        else { return .red }
    }
}

struct StatItem: View {
    let title: String
    let value: String
    
    var body: some View {
        VStack {
            Text(title)
                .font(.caption2)
                .foregroundColor(.secondary)
            Text(value)
                .font(.caption)
                .fontWeight(.medium)
        }
        .frame(maxWidth: .infinity)
    }
}

struct RiskBadge: View {
    let risk: TransferRecommendation.RiskLevel
    
    var body: some View {
        Text(risk.rawValue)
            .font(.caption2)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(backgroundColor)
            .foregroundColor(.white)
            .cornerRadius(4)
    }
    
    private var backgroundColor: Color {
        switch risk {
        case .low: return .green
        case .medium: return .orange
        case .high: return .red
        }
    }
}

struct PriorityBadge: View {
    let priority: TransferRecommendation.Priority
    
    var body: some View {
        Text(priority.rawValue)
            .font(.caption2)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(backgroundColor)
            .foregroundColor(.white)
            .cornerRadius(4)
    }
    
    private var backgroundColor: Color {
        switch priority {
        case .essential: return .red
        case .recommended: return .orange
        case .optional: return .blue
        }
    }
}

struct FilterChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.caption)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isSelected ? Color.blue : Color(.systemGray5))
                .foregroundColor(isSelected ? .white : .primary)
                .cornerRadius(16)
        }
    }
}

// MARK: - Supporting Data Models

struct RecommendationFilters {
    var positions: Set<TeamAnalysis.Position> = []
    var maxPrice: Int?
    var minPoints: Int?
    var maxRisk: TransferRecommendation.RiskLevel = .high
    var minPriority: TransferRecommendation.Priority = .optional
}

enum SortOption: String, CaseIterable {
    case recommendationScore = "Empfehlungswert"
    case price = "Preis"
    case points = "Punkte"
    case valueForMoney = "Preis-Leistung"
    case risk = "Risiko"
}

#Preview {
    TransferRecommendationsView(kickbaseManager: KickbaseManager())
}
