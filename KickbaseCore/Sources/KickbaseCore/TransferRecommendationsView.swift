import SwiftUI

struct TransferRecommendationsView: View {
    @ObservedObject var kickbaseManager: KickbaseManager
    @EnvironmentObject var ligainsiderService: LigainsiderService
    @StateObject private var recommendationService: PlayerRecommendationService
    @State private var recommendations: [TransferRecommendation] = []
    @State private var teamAnalysis: TeamAnalysis?
    @State private var isLoading = false
    @State private var loadingMessage = "Analysiere Team und lade Empfehlungen..."
    @State private var errorMessage: String?
    @State private var filters = RecommendationFilters()
    @State private var sortOption: RecommendationSortOption = .recommendationScore
    @State private var showFilterSheet = false
    @State private var selectedRecommendation: TransferRecommendation?

    init(kickbaseManager: KickbaseManager) {
        self.kickbaseManager = kickbaseManager
        self._recommendationService = StateObject(
            wrappedValue: PlayerRecommendationService(kickbaseManager: kickbaseManager))
    }

    var body: some View {
        mainContent
            .sheet(isPresented: $showFilterSheet) {
                FilterSheet(filters: $filters)
            }
            .sheet(item: $selectedRecommendation) { recommendation in
                PlayerDetailSheet(recommendation: recommendation)
            }
    }

    private var sidebarContent: some View {
        VStack(spacing: 0) {
            if isLoading {
                VStack(spacing: 20) {
                    ProgressView()
                        .scaleEffect(1.5)
                    Text(loadingMessage)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if recommendations.isEmpty {
                emptyStateView
            } else {
                recommendationsContent
            }
        }
        .navigationTitle("Transfer-Empfehlungen")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailingCompat) {
                HStack {
                    Menu {
                        Button(action: {
                            Task {
                                await loadRecommendations()
                            }
                        }) {
                            Label("Aktualisieren", systemImage: "arrow.clockwise")
                        }
                        .disabled(isLoading)

                        Button(action: {
                            if let leagueId = kickbaseManager.selectedLeague?.id {
                                recommendationService.clearCacheForLeague(leagueId)
                            }
                            Task {
                                await loadRecommendations()
                            }
                        }) {
                            Label("Cache leeren & neu laden", systemImage: "trash")
                        }
                        .disabled(isLoading)
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }

                    Button(action: { showFilterSheet = true }) {
                        Image(systemName: "slider.horizontal.3")
                    }
                }
            }
        }
        .task {
            await loadRecommendations()
        }
        .sheet(isPresented: $showFilterSheet) {
            FilterSheet(filters: $filters)
        }
    }

    private var mainContent: some View {
        VStack(spacing: 0) {
            if isLoading {
                VStack(spacing: 20) {
                    ProgressView()
                        .scaleEffect(1.5)
                    Text(loadingMessage)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if recommendations.isEmpty {
                emptyStateView
            } else {
                recommendationsContent
            }
        }
        .navigationTitle("Transfer-Empfehlungen")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailingCompat) {
                HStack {
                    Button(action: { showFilterSheet = true }) {
                        Image(systemName: "slider.horizontal.3")
                    }

                    Button("Aktualisieren") {
                        Task {
                            await loadRecommendations()
                        }
                    }
                    .disabled(isLoading)
                }
            }
        }
        .task {
            await loadRecommendations()
        }
    }

    private var defaultDetailView: some View {
        VStack(spacing: 20) {
            Image(systemName: "person.crop.circle.badge.plus")
                .font(.system(size: 80))
                .foregroundColor(.gray)

            Text("Wählen Sie eine Empfehlung")
                .font(.title2)
                .fontWeight(.medium)
                .foregroundColor(.secondary)

            Text("Tippen Sie auf eine Transfer-Empfehlung in der Liste, um Details zu sehen.")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.systemGroupedBackgroundCompat)
    }

    private var emptyStateView: some View {
        VStack(spacing: 20) {
            Image(systemName: "person.crop.circle.badge.plus")
                .font(.system(size: 60))
                .foregroundColor(.gray)

            Text("Keine Empfehlungen verfügbar")
                .font(.title2)
                .fontWeight(.medium)

            Text(
                "Wählen Sie eine Liga aus und stellen Sie sicher, dass Transfermarkt-Daten verfügbar sind."
            )
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
        VStack(spacing: 0) {
            // Enhanced Team Analysis Header
            enhancedTeamAnalysisHeader

            // Quick Filters
            quickFiltersSection

            // Sort Options
            sortingSection

            // Recommendations List
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(filteredAndSortedRecommendations) { recommendation in
                        EnhancedRecommendationCard(recommendation: recommendation) {
                            selectedRecommendation = recommendation
                        }
                    }
                }
                .padding()
            }
        }
    }

    private var enhancedTeamAnalysisHeader: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Team-Analyse")
                    .font(.headline)
                Spacer()
                Text(
                    "Budget: \(formatCurrency(kickbaseManager.selectedLeague?.currentUser.budget ?? 0))"
                )
                .font(.subheadline)
                .foregroundColor(.secondary)
            }

            if let analysis = teamAnalysis {
                // Budget Analysis
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Empfohlene Ausgaben")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(formatCurrency(analysis.budgetConstraints.recommendedSpending))
                            .font(.subheadline)
                            .fontWeight(.semibold)
                    }

                    Spacer()

                    VStack(alignment: .trailing, spacing: 4) {
                        Text("Reserve")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(formatCurrency(analysis.budgetConstraints.emergencyReserve))
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(.green)
                    }
                }

                // Weak Positions
                if !analysis.weakPositions.isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Prioritäre Positionen")
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundColor(.secondary)

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(analysis.weakPositions, id: \.self) { position in
                                    Text(position.rawValue)
                                        .font(.caption)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(Color.orange.opacity(0.2))
                                        .foregroundColor(.orange)
                                        .cornerRadius(8)
                                }
                            }
                            .padding(.horizontal)
                        }
                    }
                }

                // Stats Summary
                HStack {
                    Label(
                        "\(recommendations.count) Empfehlungen",
                        systemImage: "person.crop.circle.badge.plus"
                    )
                    .font(.caption)
                    .foregroundColor(.blue)

                    Spacer()

                    let highPriorityCount = recommendations.filter { $0.priority == .essential }
                        .count
                    if highPriorityCount > 0 {
                        Label(
                            "\(highPriorityCount) dringend",
                            systemImage: "exclamationmark.triangle.fill"
                        )
                        .font(.caption)
                        .foregroundColor(.red)
                    }
                }
            } else {
                // Fallback when no team analysis available
                HStack {
                    Label(
                        "\(recommendations.count) Empfehlungen",
                        systemImage: "person.crop.circle.badge.plus"
                    )
                    .font(.caption)
                    .foregroundColor(.blue)

                    Spacer()

                    let highPriorityCount = recommendations.filter { $0.priority == .essential }
                        .count
                    if highPriorityCount > 0 {
                        Label(
                            "\(highPriorityCount) dringend",
                            systemImage: "exclamationmark.triangle.fill"
                        )
                        .font(.caption)
                        .foregroundColor(.red)
                    }
                }
            }
        }
        .padding()
        .background(Color.systemGray6Compat)
        .cornerRadius(12)
        .padding(.horizontal)
    }

    private var quickFiltersSection: some View {
        Text("Filters Disabled")
            .padding()
    }

    private var sortingSection: some View {
        /*
        Picker("Sortierung", selection: $sortOption) {
            Text("Empfehlungswert").tag(RecommendationSortOption.recommendationScore)
            Text("Preis").tag(RecommendationSortOption.price)
            Text("Punkte").tag(RecommendationSortOption.points)
            Text("Preis-Leistung").tag(RecommendationSortOption.valueForMoney)
            Text("Form-Trend").tag(RecommendationSortOption.formTrend)
            Text("Risiko").tag(RecommendationSortOption.risk)
        }
        #if os(iOS)
            .pickerStyle(SegmentedPickerStyle())
        #endif
        .padding(.horizontal)
        .padding(.bottom, 8)
        */
        EmptyView()
    }

    private var filteredAndSortedRecommendations: [TransferRecommendation] {
        print("🔧 [DEBUG] Filtering \(recommendations.count) recommendations")
        print("🔧 [DEBUG] Current filters:")
        print("   - Positions: \(filters.positions)")
        print("   - Max Risk: \(filters.maxRisk)")
        print("   - Min Priority: \(filters.minPriority)")
        print("   - Form Trend: \(filters.formTrend?.rawValue ?? "nil")")
        print("   - Max Price: \(filters.maxPrice ?? 0)")
        print("   - Min Points: \(filters.minPoints ?? 0)")
        print("   - Min Confidence: \(filters.minConfidence ?? 0.0)")

        let filtered = recommendations.filter { recommendation in
            print(
                "🔧 [DEBUG] Checking recommendation: \(recommendation.player.firstName) \(recommendation.player.lastName)"
            )
            print("   - Position: \(recommendation.player.position)")
            print("   - Risk Level: \(recommendation.riskLevel.rawValue)")
            print("   - Priority: \(recommendation.priority.rawValue)")
            print("   - Form Trend: \(recommendation.analysis.formTrend.rawValue)")

            // Position filter
            if !filters.positions.isEmpty {
                let playerPosition = mapIntToPosition(recommendation.player.position)
                if let playerPosition = playerPosition {
                    if !filters.positions.contains(playerPosition) {
                        print("   ❌ Failed position filter")
                        return false
                    }
                }
            }

            // Risk filter - Korrigiert: Direkte Enum-Vergleiche
            if !isRiskLevelAcceptable(recommendation.riskLevel, maxRisk: filters.maxRisk) {
                print(
                    "   ❌ Failed risk filter (\(recommendation.riskLevel.rawValue) > \(filters.maxRisk.rawValue))"
                )
                return false
            }

            // Priority filter - Korrigiert: Direkte Enum-Vergleiche
            if !isPriorityAcceptable(recommendation.priority, minPriority: filters.minPriority) {
                print(
                    "   ❌ Failed priority filter (\(recommendation.priority.rawValue) < \(filters.minPriority.rawValue))"
                )
                return false
            }

            // Form trend filter
            if let formTrend = filters.formTrend, recommendation.analysis.formTrend != formTrend {
                print("   ❌ Failed form trend filter")
                return false
            }

            // Price filter
            if let maxPrice = filters.maxPrice, recommendation.player.price > maxPrice {
                print("   ❌ Failed price filter")
                return false
            }

            // Points filter
            if let minPoints = filters.minPoints, recommendation.player.totalPoints < minPoints {
                print("   ❌ Failed points filter")
                return false
            }

            // Confidence filter
            if let minConfidence = filters.minConfidence,
                recommendation.analysis.seasonProjection.confidence < minConfidence
            {
                print("   ❌ Failed confidence filter")
                return false
            }

            print("   ✅ Passed all filters")
            return true
        }

        print("🔧 [DEBUG] Filtered to \(filtered.count) recommendations")

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
            case .formTrend:
                return first.analysis.formTrend.rawValue < second.analysis.formTrend.rawValue
            case .risk:
                return getRiskLevelOrder(first.riskLevel) < getRiskLevelOrder(second.riskLevel)
            }
        }
    }

    // MARK: - Helper Functions für Enum-Vergleiche

    private func isRiskLevelAcceptable(
        _ riskLevel: TransferRecommendation.RiskLevel, maxRisk: TransferRecommendation.RiskLevel
    ) -> Bool {
        return getRiskLevelOrder(riskLevel) <= getRiskLevelOrder(maxRisk)
    }

    private func isPriorityAcceptable(
        _ priority: TransferRecommendation.Priority, minPriority: TransferRecommendation.Priority
    ) -> Bool {
        return getPriorityOrder(priority) >= getPriorityOrder(minPriority)
    }

    private func getRiskLevelOrder(_ risk: TransferRecommendation.RiskLevel) -> Int {
        switch risk {
        case .low: return 1
        case .medium: return 2
        case .high: return 3
        }
    }

    private func getPriorityOrder(_ priority: TransferRecommendation.Priority) -> Int {
        switch priority {
        case .optional: return 1
        case .recommended: return 2
        case .essential: return 3
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
            loadingMessage = "Lade Spieldaten..."
            let budget = selectedLeague.currentUser.budget
            print("🎯 Loading recommendations with budget: \(budget)")

            loadingMessage = "Analysiere Spieler..."
            let results = try await recommendationService.generateRecommendations(
                for: selectedLeague, budget: budget)

            loadingMessage = "Bereite Empfehlungen vor..."

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
        return formatter.string(from: NSNumber(value: Double(amount))) ?? "€0"
    }
}

// MARK: - Enhanced Recommendation Card

struct EnhancedRecommendationCard: View {
    let recommendation: TransferRecommendation
    let onTap: () -> Void
    @EnvironmentObject var ligainsiderService: LigainsiderService

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            // Player Header with Enhanced Info
            HStack(alignment: .top, spacing: 12) {
                // Player Photo from Ligainsider
                if let ligaPlayer = ligainsiderService.getLigainsiderPlayer(
                    firstName: recommendation.player.firstName,
                    lastName: recommendation.player.lastName),
                   let imgString = ligaPlayer.imageUrl,
                   let url = URL(string: imgString)
                {
                    #if !SKIP
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .empty:
                            Circle()
                                .fill(Color.gray.opacity(0.3))
                                .frame(width: 50, height: 50)
                                .overlay {
                                    ProgressView()
                                }
                        case .success(let image):
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: 50, height: 50)
                                .clipShape(Circle())
                        case .failure:
                            Circle()
                                .fill(positionColor(for: recommendation.player.position).opacity(0.3))
                                .frame(width: 50, height: 50)
                                .overlay {
                                    Image(systemName: "person.fill")
                                        .foregroundColor(positionColor(for: recommendation.player.position))
                                }
                        @unknown default:
                            EmptyView()
                        }
                    }
                    #else
                    Circle()
                        .fill(positionColor(for: recommendation.player.position).opacity(0.3))
                        .frame(width: 50, height: 50)
                        .overlay {
                            Image(systemName: "person.fill")
                                .foregroundColor(positionColor(for: recommendation.player.position))
                        }
                    #endif
                }
                
                VStack(alignment: .leading, spacing: 6) {
                    // Name with Form Trend
                    HStack {
                        Text(recommendation.player.firstName + " " + recommendation.player.lastName)
                            .font(.headline)
                            .fontWeight(.semibold)
                            .foregroundColor(.primary)

                        // Ligainsider Status Icon
                        let status = ligainsiderService.getPlayerStatus(
                            firstName: recommendation.player.firstName,
                            lastName: recommendation.player.lastName)
                        if status != .out {
                            Image(systemName: ligainsiderService.getIcon(for: status))
                                .foregroundColor(
                                    ligainsiderService.getColor(for: status)
                                )
                                .font(.caption)
                        }

                        // Form Trend Indicator
                        FormTrendBadge(trend: recommendation.analysis.formTrend)
                    }

                    // Team Name
                    Text(recommendation.player.teamName)
                        .font(.caption)
                        .foregroundColor(.secondary)

                    // All Badges in one row (Position, Risk, Priority)
                    HStack(spacing: 6) {
                        // Position Badge
                        Text(positionName(for: recommendation.player.position))
                            .font(.caption)
                            .fontWeight(.medium)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(positionColor(for: recommendation.player.position))
                            .foregroundColor(.white)
                            .cornerRadius(6)

                        RiskBadge(risk: recommendation.riskLevel)
                        PriorityBadge(priority: recommendation.priority)
                    }
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 4) {
                    Text(formatPrice(recommendation.player.price))
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundColor(.primary)
                }
            }
            #if os(iOS)
                .contentShape(Rectangle())
            #endif
            .onTapGesture {
                onTap()
            }

            // Enhanced Stats Section
            VStack(spacing: 8) {
                HStack {
                    EnhancedStatItem(
                        title: "Punkte", value: "\(recommendation.player.totalPoints)",
                        icon: "target")
                    EnhancedStatItem(
                        title: "Ø Punkte",
                        value: String(format: "%.1f", recommendation.analysis.pointsPerGame),
                        icon: "chart.bar")
                    EnhancedStatItem(
                        title: "Wert/€M",
                        value: String(format: "%.1f", recommendation.analysis.valueForMoney),
                        icon: "eurosign.circle")
                    EnhancedStatItem(
                        title: "Score",
                        value: String(format: "%.1f", recommendation.recommendationScore),
                        icon: "star.fill")
                }

                // Season Projection
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Saisonprognose")
                            .font(.caption2)
                            .fontWeight(.medium)
                            .foregroundColor(.secondary)

                        Text(
                            "\(recommendation.analysis.seasonProjection.projectedTotalPoints) Pkt."
                        )
                        .font(.caption)
                        .fontWeight(.medium)
                    }

                    Spacer()

                    // Confidence Indicator
                    HStack {
                        Text("Vertrauen:")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        ConfidenceBadge(
                            confidence: recommendation.analysis.seasonProjection.confidence)
                    }
                }
            }
            .padding(.top, 4)

            // Enhanced Reasons Section
            if !recommendation.reasons.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Gründe für die Empfehlung:")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(.secondary)

                    ForEach(recommendation.reasons.prefix(3)) { reason in
                        HStack(spacing: 8) {
                            Image(systemName: iconForReasonType(reason.type))
                                .font(.caption)
                                .foregroundColor(colorForImpact(reason.impact))

                                #if os(iOS)
                                    .frame(width: 16)
                                #endif

                            Text(reason.description)
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .lineLimit(2)

                            Spacer()

                            // Impact Score
                            Text(String(format: "%.1f", reason.impact))
                                .font(.caption2)
                                .fontWeight(.medium)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(colorForImpact(reason.impact).opacity(0.2))
                                .foregroundColor(colorForImpact(reason.impact))
                                .cornerRadius(4)
                        }
                    }

                    if recommendation.reasons.count > 3 {
                        Text("... und \(recommendation.reasons.count - 3) weitere Gründe")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                            .italic()
                    }
                }
                .padding(.top, 4)
            }
        }
        .padding(16)
        .background(Color.systemBackgroundCompat)
        .cornerRadius(16)
        .shadow(color: Color.black.opacity(0.08), radius: 4, x: 0, y: 2)
        .overlay {
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .strokeBorder(priorityBorderColor(recommendation.priority), lineWidth: 2)
                    .opacity(0.3)
            }
        }
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

    private func positionColor(for position: Int) -> Color {
        switch position {
        case 1: return .blue  // Torwart
        case 2: return .green  // Abwehr
        case 3: return .orange  // Mittelfeld
        case 4: return .red  // Sturm
        default: return .gray
        }
    }

    private func formatPrice(_ price: Int) -> String {
        return "€\(String(format: "%.1f", Double(price) / 1_000_000.0))M"
    }

    private func priorityBorderColor(_ priority: TransferRecommendation.Priority) -> Color {
        switch priority {
        case .essential: return .red
        case .recommended: return .orange
        case .optional: return .blue
        }
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
        if impact >= 7 {
            return .green
        } else if impact >= 4 {
            return .blue
        } else if impact >= 0 {
            return .orange
        } else {
            return .red
        }
    }

    private func formatCurrency(_ amount: Int) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.locale = Locale(identifier: "de_DE")
        formatter.currencyCode = "EUR"
        return formatter.string(from: NSNumber(value: Double(amount))) ?? "€0"
    }
}

// MARK: - Recommendation Player Detail View (für iPad/macOS NavigationSplitView)

struct RecommendationPlayerDetailView: View {
    let recommendation: TransferRecommendation
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @State private var marketValueHistory: MarketValueChange?
    @State private var isLoadingHistory = false

    var latestDailyChange: Int {
        guard let history = marketValueHistory,
            let today = history.dailyChanges.first(where: { $0.daysAgo == 0 })
        else {
            return recommendation.analysis.seasonProjection.projectedValueIncrease
        }
        return today.change
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Player Header
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        VStack(alignment: .leading) {
                            Text(
                                recommendation.player.firstName + " "
                                    + recommendation.player.lastName
                            )
                            .font(.title2)
                            .fontWeight(.bold)

                            Text(recommendation.player.teamName)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }

                        Spacer()

                        VStack(alignment: .trailing) {
                            Text(formatPrice(recommendation.player.price))
                                .font(.title2)
                                .fontWeight(.bold)

                            HStack {
                                RiskBadge(risk: recommendation.riskLevel)
                                PriorityBadge(priority: recommendation.priority)
                            }
                        }
                    }

                    // Score and Rating
                    HStack {
                        VStack(alignment: .leading) {
                            Text("Empfehlungswert")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Text(String(format: "%.1f/24", recommendation.recommendationScore))
                                .font(.headline)
                                .fontWeight(.semibold)
                        }

                        Spacer()

                        // Progress Bar for Score
                        ProgressView(value: recommendation.recommendationScore, total: 24.0)
                            .progressViewStyle(.linear)
                            .tint(scoreColor(recommendation.recommendationScore))

                            #if os(iOS)
                                .frame(width: 100)
                            #endif
                    }
                }
                .padding()
                .background(Color.systemGray6Compat)
                .cornerRadius(12)

                // Detailed Analysis
                VStack(alignment: .leading, spacing: 16) {
                    Text("Detaillierte Analyse")
                        .font(.headline)

                    // Performance Stats
                    HStack {
                        StatDetailItem(
                            title: "Gesamtpunkte", value: "\(recommendation.player.totalPoints)")
                        StatDetailItem(
                            title: "Ø pro Spiel",
                            value: String(format: "%.1f", recommendation.analysis.pointsPerGame))
                        StatDetailItem(
                            title: "Preis-Leistung",
                            value: String(format: "%.1f", recommendation.analysis.valueForMoney))
                    }

                    // Form
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Form")
                            .font(.subheadline)
                            .fontWeight(.medium)

                        HStack {
                            VStack(alignment: .leading) {
                                Text("Form-Trend")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                FormTrendBadge(trend: recommendation.analysis.formTrend)
                            }

                            Spacer()
                        }
                    }

                    // Season Projection
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Saisonprognose")
                            .font(.subheadline)
                            .fontWeight(.medium)

                        HStack {
                            VStack(alignment: .leading) {
                                Text("Erwartete Punkte")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                Text(
                                    "\(recommendation.analysis.seasonProjection.projectedTotalPoints)"
                                )
                                .font(.subheadline)
                                .fontWeight(.semibold)
                            }

                            Spacer()

                            VStack(alignment: .trailing) {
                                Text("Wertsteigerung (täglich)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                Text(formatCurrency(latestDailyChange))
                                    .font(.subheadline)
                                    .fontWeight(.semibold)
                                    .foregroundColor(latestDailyChange > 0 ? .green : .red)
                            }
                        }

                        HStack {
                            Text("Vertrauen:")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            ConfidenceBadge(
                                confidence: recommendation.analysis.seasonProjection.confidence)
                            Spacer()
                        }
                    }
                }
                .padding()
                .background(Color.systemGray6Compat)
                .cornerRadius(12)

                // All Reasons
                VStack(alignment: .leading, spacing: 12) {
                    Text("Alle Empfehlungsgründe")
                        .font(.headline)

                    ForEach(recommendation.reasons) { reason in
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Image(systemName: iconForReasonType(reason.type))
                                    .foregroundColor(colorForImpact(reason.impact))
                                Text(reason.type.rawValue)
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                                Spacer()
                                Text(String(format: "%.1f", reason.impact))
                                    .font(.caption)
                                    .fontWeight(.medium)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(colorForImpact(reason.impact).opacity(0.2))
                                    .foregroundColor(colorForImpact(reason.impact))
                                    .cornerRadius(4)
                            }

                            Text(reason.description)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color.systemGray6Compat)
                        .cornerRadius(8)
                    }
                }
            }
            .padding()
        }
        .navigationTitle("Spieler Details")
        .task {
            await loadMarketValueHistory()
        }
    }

    @MainActor
    private func loadMarketValueHistory() async {
        guard let selectedLeague = kickbaseManager.selectedLeague else { return }
        isLoadingHistory = true
        let history = await kickbaseManager.loadPlayerMarketValueHistory(
            playerId: recommendation.player.id,
            leagueId: selectedLeague.id
        )
        marketValueHistory = history
        isLoadingHistory = false
    }

    private func formatPrice(_ price: Int) -> String {
        return "€\(String(format: "%.1f", Double(price) / 1_000_000.0))M"
    }

    private func formatCurrency(_ amount: Int) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.locale = Locale(identifier: "de_DE")
        formatter.currencyCode = "EUR"
        return formatter.string(from: NSNumber(value: Double(amount))) ?? "€0"
    }

    private func scoreColor(_ score: Double) -> Color {
        if score >= 7 {
            return .green
        } else if score >= 5 {
            return .blue
        } else if score >= 3 {
            return .orange
        } else {
            return .red
        }
    }

    private func colorForInjuryRisk(_ risk: PlayerAnalysis.InjuryRisk) -> Color {
        switch risk {
        case .low: return .green
        case .medium: return .orange
        case .high: return .red
        }
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
        if impact >= 7 {
            return .green
        } else if impact >= 4 {
            return .blue
        } else if impact >= 0 {
            return .orange
        } else {
            return .red
        }
    }
}

struct StatDetailItem: View {
    let title: String
    let value: String

    var body: some View {
        VStack {
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
            Text(value)
                .font(.subheadline)
                .fontWeight(.semibold)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Enhanced Supporting Views

struct EnhancedStatItem: View {
    let title: String
    let value: String
    let icon: String

    var body: some View {
        VStack(spacing: 2) {
            HStack(spacing: 2) {
                Image(systemName: icon)
                    .font(.caption2)
                    .foregroundColor(.secondary)
                Text(title)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            Text(value)
                .font(.caption)
                .fontWeight(.semibold)
        }
        .frame(maxWidth: .infinity)
    }
}

struct FormTrendBadge: View {
    let trend: PlayerAnalysis.FormTrend

    var body: some View {
        HStack(spacing: 2) {
            Image(systemName: iconForTrend(trend))
                .font(.caption2)
            Text(trend.rawValue)
                .font(.caption2)
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 2)
        .background(colorForTrend(trend).opacity(0.2))
        .foregroundColor(colorForTrend(trend))
        .cornerRadius(4)
    }

    private func iconForTrend(_ trend: PlayerAnalysis.FormTrend) -> String {
        switch trend {
        case .improving: return "arrow.up"
        case .stable: return "arrow.right"
        case .declining: return "arrow.down"
        }
    }

    private func colorForTrend(_ trend: PlayerAnalysis.FormTrend) -> Color {
        switch trend {
        case .improving: return .green
        case .stable: return .blue
        case .declining: return .red
        }
    }
}

struct ConfidenceBadge: View {
    let confidence: Double

    var body: some View {
        Text("\(Int(confidence * 100))%")
            .font(.caption2)
            .fontWeight(.medium)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(colorForConfidence(confidence).opacity(0.2))
            .foregroundColor(colorForConfidence(confidence))
            .cornerRadius(4)
    }

    private func colorForConfidence(_ confidence: Double) -> Color {
        if confidence >= 0.8 {
            return .green
        } else if confidence >= 0.6 {
            return .blue
        } else if confidence >= 0.4 {
            return .orange
        } else {
            return .red
        }
    }
}

struct RiskBadge: View {
    let risk: TransferRecommendation.RiskLevel

    var body: some View {
        Text(risk.rawValue)
            .font(.caption2)
            .fontWeight(.medium)
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
            .fontWeight(.medium)
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
        Text(title)
            .font(.caption)
            .fontWeight(.medium)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(isSelected ? Color.blue : Color.systemGray5Compat)
            .foregroundColor(isSelected ? .white : .primary)
            .cornerRadius(16)
            .onTapGesture {
                action()
            }
    }
}

// MARK: - Filter Sheet

struct FilterSheet: View {
    // MARK: - Computed Bindings for Kotlin Compat
    var maxPriceBindingCompat: Binding<String> {
        Binding<String>(
            get: {
                if let extractedValue = filters.maxPrice { return String(extractedValue) }
                return ""
            },
            set: { newValue in
                if let v = Int(newValue) { filters.maxPrice = v } else { filters.maxPrice = nil }
            }
        )
    }

    var minPointsBindingCompat: Binding<String> {
        Binding<String>(
            get: {
                if let extractedValue = filters.minPoints { return String(extractedValue) }
                return ""
            },
            set: { newValue in
                if let v = Int(newValue) { filters.minPoints = v } else { filters.minPoints = nil }
            }
        )
    }

    var minConfidenceBindingCompat: Binding<String> {
        Binding<String>(
            get: {
                if let extractedValue = filters.minConfidence { return String(extractedValue) }
                return ""
            },
            set: { newValue in
                let s = newValue.replacingOccurrences(of: ",", with: ".")
                if let v = Double(s) {
                    filters.minConfidence = v
                } else {
                    filters.minConfidence = nil
                }
            }
        )
    }

    @Binding var filters: RecommendationFilters
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section("Positionen") {
                    /*
                    // Companion object access causes Kotlin errors
                    ForEach(TeamAnalysis.Position.allCases, id: \.self) { position in
                        Button(action: {
                            if filters.positions.contains(position) {
                                filters.positions.remove(position)
                            } else {
                                filters.positions.insert(position)
                            }
                        }) {
                            HStack {
                                Text(position.rawValue)
                                Spacer()
                                if filters.positions.contains(position) {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.blue)
                                }
                            }
                        }
                    }
                    */
                    Text("Filter deaktiviert (Wartung)")
                }

                Section("Risiko & Priorität") {
                    Text("Filters Disabled for Android Build")
                    /*
                                        // Manual iteration to avoid Companion object issues in Kotlin transpile
                                        Picker("Max. Risiko", selection: $filters.maxRisk) {
                                            Text("Low").tag(TransferRecommendation.RiskLevel.low)
                                            Text("Medium").tag(TransferRecommendation.RiskLevel.medium)
                                            Text("High").tag(TransferRecommendation.RiskLevel.high)
                                        }
                    
                                        Picker("Min. Priorität", selection: $filters.minPriority) {
                                            Text("Essential").tag(TransferRecommendation.Priority.essential)
                                             Text("Recommended").tag(TransferRecommendation.Priority.recommended)
                                             Text("Optional").tag(TransferRecommendation.Priority.optional)
                                        }
                    */
                }

                Section("Erweiterte Filter") {
                    /*
                    Picker("Form-Trend", selection: $filters.formTrend) {
                        Text("Alle").tag(PlayerAnalysis.FormTrend?.none)
                        ForEach(
                            [PlayerAnalysis.FormTrend.improving, .stable, .declining], id: \.self
                        ) { trend in
                            Text(trend.rawValue).tag(PlayerAnalysis.FormTrend?.some(trend))
                        }
                    }
                    */
                    Text("Deaktiviert")
                }

                Section("Werte-Filter") {
                    HStack {
                        Text("Max. Preis")
                        Spacer()
                        TextField("€ Millionen", text: self.maxPriceBindingCompat)
                            #if os(iOS)
                                .textFieldStyle(.roundedBorder)
                                .frame(width: 120)
                            #endif
                    }

                    HStack {
                        Text("Min. Punkte")
                        Spacer()
                        TextField("Punkte", text: self.minPointsBindingCompat)
                            #if os(iOS)
                                .textFieldStyle(.roundedBorder)
                                .frame(width: 80)
                            #endif
                    }

                    HStack {
                        Text("Min. Vertrauen")
                        Spacer()
                        TextField("0.0 - 1.0", text: self.minConfidenceBindingCompat)
                            #if os(iOS)
                                .textFieldStyle(.roundedBorder)
                                .frame(width: 100)
                            #endif
                    }
                }
            }
            .navigationTitle("Filter")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeadingCompat) {
                    Button("Zurücksetzen") {
                        filters = RecommendationFilters()
                    }
                }
                ToolbarItem(placement: .navigationBarTrailingCompat) {
                    Button("Fertig") {
                        dismiss()
                    }
                }
            }
        }
    }
}

// MARK: - Player Detail Sheet

struct PlayerDetailSheet: View {
    let recommendation: TransferRecommendation
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @Environment(\.dismiss) private var dismiss
    @State private var marketValueHistory: MarketValueChange?
    @State private var isLoadingHistory = false

    var latestDailyChange: Int {
        guard let history = marketValueHistory,
            let today = history.dailyChanges.first(where: { $0.daysAgo == 0 })
        else {
            return recommendation.analysis.seasonProjection.projectedValueIncrease
        }
        return today.change
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Player Header
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(
                                    recommendation.player.firstName + " "
                                        + recommendation.player.lastName
                                )
                                .font(.title2)
                                .fontWeight(.bold)

                                Text(recommendation.player.teamName)
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }

                            Spacer()

                            VStack(alignment: .trailing) {
                                Text(formatPrice(recommendation.player.price))
                                    .font(.title2)
                                    .fontWeight(.bold)

                                HStack {
                                    RiskBadge(risk: recommendation.riskLevel)
                                    PriorityBadge(priority: recommendation.priority)
                                }
                            }
                        }

                        // Score and Rating
                        HStack {
                            VStack(alignment: .leading) {
                                Text("Empfehlungswert")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                Text(String(format: "%.1f/24", recommendation.recommendationScore))
                                    .font(.headline)
                                    .fontWeight(.semibold)
                            }

                            Spacer()

                            // Progress Bar for Score
                            ProgressView(value: recommendation.recommendationScore, total: 24.0)
                                .progressViewStyle(.linear)
                                .tint(scoreColor(recommendation.recommendationScore))

                                #if os(iOS)
                                    .frame(width: 100)
                                #endif
                        }
                    }
                    .padding()
                    .background(Color.systemGray6Compat)
                    .cornerRadius(12)

                    // Detailed Analysis
                    VStack(alignment: .leading, spacing: 16) {
                        Text("Detaillierte Analyse")
                            .font(.headline)

                        // Performance Stats
                        HStack {
                            StatDetailItem(
                                title: "Gesamtpunkte", value: "\(recommendation.player.totalPoints)"
                            )
                            StatDetailItem(
                                title: "Ø pro Spiel",
                                value: String(format: "%.1f", recommendation.analysis.pointsPerGame)
                            )
                            StatDetailItem(
                                title: "Preis-Leistung",
                                value: String(format: "%.1f", recommendation.analysis.valueForMoney)
                            )
                        }

                        // Form
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Form")
                                .font(.subheadline)
                                .fontWeight(.medium)

                            HStack {
                                VStack(alignment: .leading) {
                                    Text("Form-Trend")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                    FormTrendBadge(trend: recommendation.analysis.formTrend)
                                }

                                Spacer()
                            }
                        }

                        // Season Projection
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Saisonprognose")
                                .font(.subheadline)
                                .fontWeight(.medium)

                            HStack {
                                VStack(alignment: .leading) {
                                    Text("Erwartete Punkte")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                    Text(
                                        "\(recommendation.analysis.seasonProjection.projectedTotalPoints)"
                                    )
                                    .font(.subheadline)
                                    .fontWeight(.semibold)
                                }

                                Spacer()

                                VStack(alignment: .trailing) {
                                    Text("Wertsteigerung (täglich)")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                    Text(formatCurrency(latestDailyChange))
                                        .font(.subheadline)
                                        .fontWeight(.semibold)
                                        .foregroundColor(latestDailyChange > 0 ? .green : .red)
                                }
                            }

                            HStack {
                                Text("Vertrauen:")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                ConfidenceBadge(
                                    confidence: recommendation.analysis.seasonProjection.confidence)
                                Spacer()
                            }
                        }
                    }
                    .padding()
                    .background(Color.systemGray6Compat)
                    .cornerRadius(12)

                    // All Reasons
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Alle Empfehlungsgründe")
                            .font(.headline)

                        ForEach(recommendation.reasons) { reason in
                            VStack(alignment: .leading, spacing: 6) {
                                HStack {
                                    Image(systemName: iconForReasonType(reason.type))
                                        .foregroundColor(colorForImpact(reason.impact))
                                    Text(reason.type.rawValue)
                                        .font(.subheadline)
                                        .fontWeight(.medium)
                                    Spacer()
                                    Text(String(format: "%.1f", reason.impact))
                                        .font(.caption)
                                        .fontWeight(.medium)
                                        .padding(.horizontal, 6)
                                        .padding(.vertical, 2)
                                        .background(colorForImpact(reason.impact).opacity(0.2))
                                        .foregroundColor(colorForImpact(reason.impact))
                                        .cornerRadius(4)
                                }

                                Text(reason.description)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            .padding()
                            .background(Color.systemGray6Compat)
                            .cornerRadius(8)
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("Spieler Details")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailingCompat) {
                    Button("Schließen") {
                        dismiss()
                    }
                }
            }
        }
        .task {
            await loadMarketValueHistory()
        }
    }

    @MainActor
    private func loadMarketValueHistory() async {
        guard let selectedLeague = kickbaseManager.selectedLeague else { return }
        isLoadingHistory = true
        let history = await kickbaseManager.loadPlayerMarketValueHistory(
            playerId: recommendation.player.id,
            leagueId: selectedLeague.id
        )
        marketValueHistory = history
        isLoadingHistory = false
    }

    private func formatPrice(_ price: Int) -> String {
        return "€\(String(format: "%.1f", Double(price) / 1_000_000.0))M"
    }

    private func formatCurrency(_ amount: Int) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.locale = Locale(identifier: "de_DE")
        formatter.currencyCode = "EUR"
        return formatter.string(from: NSNumber(value: Double(amount))) ?? "€0"
    }

    private func scoreColor(_ score: Double) -> Color {
        if score >= 7 {
            return .green
        } else if score >= 5 {
            return .blue
        } else if score >= 3 {
            return .orange
        } else {
            return .red
        }
    }

    private func colorForInjuryRisk(_ risk: PlayerAnalysis.InjuryRisk) -> Color {
        switch risk {
        case .low: return .green
        case .medium: return .orange
        case .high: return .red
        }
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
        if impact >= 7 {
            return .green
        } else if impact >= 4 {
            return .blue
        } else if impact >= 0 {
            return .orange
        } else {
            return .red
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
    var formTrend: PlayerAnalysis.FormTrend?
    var minConfidence: Double?

}

/*
#if !SKIP
#Preview {
    TransferRecommendationsView(kickbaseManager: KickbaseManager())
}
#endif
*/
