import SwiftUI

struct MainDashboardView: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @EnvironmentObject var authManager: AuthenticationManager
    @State private var selectedTab = 0
    
    var body: some View {
        NavigationView {
            TabView(selection: $selectedTab) {
                // Team Tab mit Punktzahlen
                TeamView()
                    .tabItem {
                        Image(systemName: "person.3.fill")
                        Text("Team")
                    }
                    .tag(0)
                
                // Market Tab
                MarketView()
                    .tabItem {
                        Image(systemName: "cart.fill")
                        Text("Markt")
                    }
                    .tag(1)
                
                // Stats Tab
                StatsView()
                    .tabItem {
                        Image(systemName: "chart.bar.fill")
                        Text("Stats")
                    }
                    .tag(2)
                
                // Lineup Optimizer Tab (ersetzt Gifts Tab)
                LineupOptimizerView()
                    .tabItem {
                        Image(systemName: "person.crop.square.fill.and.at.rectangle")
                        Text("Aufstellung")
                    }
                    .tag(3)
            }
            .navigationTitle(kickbaseManager.selectedLeague?.name ?? "Kickbase Helper")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Logout") {
                        authManager.logout()
                    }
                }
                
                ToolbarItem(placement: .navigationBarLeading) {
                    if kickbaseManager.isLoading {
                        ProgressView()
                            .scaleEffect(0.8)
                    }
                }
            }
        }
        .environmentObject(kickbaseManager)
    }
}

// MARK: - Team View mit prominenten Punktzahlen
struct TeamView: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @State private var sortBy: SortOption = .marketValue
    @State private var searchText = ""
    
    enum SortOption: String, CaseIterable {
        case name = "Name"
        case marketValue = "Marktwert"
        case points = "Punkte"
        case trend = "Trend"
        case position = "Position"
    }
    
    var body: some View {
        VStack(spacing: 0) {
            // Budget und Teamwert Header
            if let stats = kickbaseManager.userStats {
                TeamStatsHeader(stats: stats)
            }
            
            // Search and Sort Controls
            VStack(spacing: 10) {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.gray)
                    TextField("Spieler suchen...", text: $searchText)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                }
                
                HStack {
                    Text("Sortieren:")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Picker("Sortierung", selection: $sortBy) {
                        ForEach(SortOption.allCases, id: \.self) { option in
                            Text(option.rawValue).tag(option)
                        }
                    }
                    .pickerStyle(SegmentedPickerStyle())
                }
            }
            .padding(.horizontal)
            
            // Spielerliste mit Punktzahlen
            List {
                ForEach(filteredAndSortedPlayers) { player in
                    PlayerRowView(player: player)
                }
            }
            .refreshable {
                if let league = kickbaseManager.selectedLeague {
                    await kickbaseManager.loadTeamPlayers(for: league)
                }
            }
        }
        .onAppear {
            if let league = kickbaseManager.selectedLeague {
                Task {
                    await kickbaseManager.loadTeamPlayers(for: league)
                }
            }
        }
    }
    
    private var filteredAndSortedPlayers: [TeamPlayer] {
        let filtered = searchText.isEmpty ? kickbaseManager.teamPlayers :
            kickbaseManager.teamPlayers.filter { player in
                player.firstName.localizedCaseInsensitiveContains(searchText) ||
                player.lastName.localizedCaseInsensitiveContains(searchText) ||
                player.fullTeamName.localizedCaseInsensitiveContains(searchText)
            }
        
        return filtered.sorted(by: { player1, player2 in
            switch sortBy {
            case .name:
                return player1.lastName < player2.lastName
            case .marketValue:
                return player1.marketValue > player2.marketValue
            case .points:
                return player1.totalPoints > player2.totalPoints
            case .trend:
                return player1.marketValueTrend > player2.marketValueTrend
            case .position:
                return player1.position < player2.position
            }
        })
    }
}

// MARK: - Player Row mit prominenten Punktzahlen
struct PlayerRowView: View {
    let player: TeamPlayer
    @State private var showingPlayerDetail = false
    
    var body: some View {
        Button(action: {
            print("🔄 PlayerRowView: Tapped on player \(player.fullName)")
            showingPlayerDetail = true
        }) {
            HStack(spacing: 12) {
                // Position Badge
                PositionBadge(position: player.position)
                
                VStack(alignment: .leading, spacing: 4) {
                    // Name mit Status-Icons
                    HStack(spacing: 4) {
                        Text(player.fullName)
                            .font(.headline)
                            .fontWeight(.medium)
                        
                        // Status-Icons basierend auf st-Feld aus API-Daten anzeigen
                        if player.status == 1 {
                            // Verletzt - rotes Kreuz
                            Image(systemName: "cross.circle.fill")
                                .foregroundColor(.red)
                                .font(.caption)
                        } else if player.status == 2 {
                            // Angeschlagen - Tabletten-Icon
                            Image(systemName: "pills.fill")
                                .foregroundColor(.orange)
                                .font(.caption)
                        }
                    }
                    
                    // Team
                    Text(player.fullTeamName)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                // PUNKTZAHLEN - Durchschnittspunktzahl groß, Gesamtpunktzahl klein
                VStack(alignment: .trailing, spacing: 6) {
                    // Durchschnittspunkte - groß und prominent
                    HStack(spacing: 4) {
                        Image(systemName: "star.fill")
                            .font(.subheadline)
                            .foregroundColor(.orange)
                        Text(String(format: "%.0f", player.averagePoints))
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.primary)
                    }
                    
                    // Gesamtpunkte - kleinere Anzeige
                    HStack(spacing: 4) {
                        Image(systemName: "sum")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        Text("\(player.totalPoints) Gesamt")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                
                VStack(alignment: .trailing, spacing: 4) {
                    // Marktwert
                    Text("€\(formatValue(player.marketValue))")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    
                    // Trend - verwende tfhmvt (Marktwertänderung seit letztem Update)
                    if player.tfhmvt != 0 {
                        HStack(spacing: 2) {
                            Image(systemName: player.tfhmvt >= 0 ? "arrow.up" : "arrow.down")
                                .font(.caption2)
                                .foregroundColor(player.tfhmvt >= 0 ? .green : .red)
                            Text("€\(formatValue(abs(player.tfhmvt)))")
                                .font(.caption)
                                .foregroundColor(player.tfhmvt >= 0 ? .green : .red)
                        }
                    }
                }
            }
            .padding(.vertical, 8)
        }
        .buttonStyle(PlainButtonStyle())
        .sheet(isPresented: $showingPlayerDetail) {
            PlayerDetailView(player: player)
        }
    }
    
    private func formatValue(_ value: Int) -> String {
        if value >= 1000000 {
            return String(format: "%.1fM", Double(value) / 1000000)
        } else if value >= 1000 {
            return String(format: "%.0fk", Double(value) / 1000)
        } else {
            return "\(value)"
        }
    }
}

// MARK: - Position Badge
struct PositionBadge: View {
    let position: Int
    
    private var positionInfo: (String, Color) {
        switch position {
        case 1: return ("TW", .yellow)
        case 2: return ("ABW", .green)
        case 3: return ("MF", .blue)
        case 4: return ("ST", .red)
        default: return ("?", .gray)
        }
    }
    
    var body: some View {
        Text(positionInfo.0)
            .font(.caption)
            .fontWeight(.bold)
            .foregroundColor(.white)
            .frame(width: 32, height: 32)
            .background(positionInfo.1)
            .clipShape(Circle())
    }
}

// MARK: - Team Stats Header mit Gesamtpunkten
struct TeamStatsHeader: View {
    let stats: UserStats
    
    var body: some View {
        HStack(spacing: 20) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Budget")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text("€\(formatValue(stats.budget))")
                    .font(.headline)
                    .fontWeight(.bold)
            }
            
            Spacer()
            
            VStack(alignment: .center, spacing: 4) {
                Text("Meine Punkte")
                    .font(.caption)
                    .foregroundColor(.secondary)
                HStack {
                    Image(systemName: "star.fill")
                        .foregroundColor(.orange)
                    Text("\(stats.points)")
                        .font(.title)
                        .fontWeight(.bold)
                }
            }
            
            Spacer()
            
            VStack(alignment: .trailing, spacing: 4) {
                Text("Teamwert")
                    .font(.caption)
                    .foregroundColor(.secondary)
                HStack {
                    Text("€\(formatValue(stats.teamValue))")
                        .font(.headline)
                        .fontWeight(.bold)
                    if stats.teamValueTrend != 0 {
                        Image(systemName: stats.teamValueTrend >= 0 ? "arrow.up" : "arrow.down")
                            .foregroundColor(stats.teamValueTrend >= 0 ? .green : .red)
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
        .padding(.horizontal)
    }
    
    private func formatValue(_ value: Int) -> String {
        if value >= 1000000 {
            return String(format: "%.1fM", Double(value) / 1000000)
        } else if value >= 1000 {
            return String(format: "%.0fk", Double(value) / 1000)
        } else {
            return "\(value)"
        }
    }
}

// MARK: - Market View mit Punktzahlen
struct MarketView: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    
    var body: some View {
        List {
            ForEach(kickbaseManager.marketPlayers) { player in
                MarketPlayerRowView(player: player)
            }
        }
        .refreshable {
            if let league = kickbaseManager.selectedLeague {
                await kickbaseManager.loadMarketPlayers(for: league)
            }
        }
        .onAppear {
            if let league = kickbaseManager.selectedLeague {
                Task {
                    await kickbaseManager.loadMarketPlayers(for: league)
                }
            }
        }
    }
}

// MARK: - Market Player Row mit Punktzahlen
struct MarketPlayerRowView: View {
    let player: MarketPlayer
    @State private var showingPlayerDetail = false
    
    var body: some View {
        Button(action: {
            print("🔄 MarketPlayerRowView: Tapped on player \(player.fullName)")
            showingPlayerDetail = true
        }) {
            HStack(spacing: 12) {
                // Position Badge
                PositionBadge(position: player.position)
                
                VStack(alignment: .leading, spacing: 4) {
                    // Name mit Status-Icons
                    HStack(spacing: 4) {
                        Text(player.fullName)
                            .font(.headline)
                            .fontWeight(.medium)
                        
                        // Status-Icons basierend auf status-Feld aus API-Daten anzeigen
                        if player.status == 1 {
                            // Verletzt - rotes Kreuz
                            Image(systemName: "cross.circle.fill")
                                .foregroundColor(.red)
                                .font(.caption)
                        } else if player.status == 2 {
                            // Angeschlagen - Tabletten-Icon
                            Image(systemName: "pills.fill")
                                .foregroundColor(.orange)
                                .font(.caption)
                        }
                    }
                    
                    // Team und Verkäufer
                    Text("\(player.fullTeamName) • \(player.seller.name)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                // Punktzahlen für Marktplayer
                VStack(alignment: .trailing, spacing: 4) {
                    // Gesamtpunkte
                    HStack(spacing: 4) {
                        Image(systemName: "star.fill")
                            .font(.caption)
                            .foregroundColor(.orange)
                        Text("\(player.totalPoints)")
                            .font(.headline)
                            .fontWeight(.bold)
                    }
                    
                    // Durchschnittspunkte
                    HStack(spacing: 4) {
                        Image(systemName: "chart.line.uptrend.xyaxis")
                            .font(.caption2)
                            .foregroundColor(.blue)
                        Text(String(format: "%.0f Ø", player.averagePoints))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                
                VStack(alignment: .trailing, spacing: 4) {
                    // Verkaufspreis
                    Text("€\(formatValue(player.price))")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(.green)
                    
                    // Marktwert
                    Text("MW: €\(formatValue(player.marketValue))")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .padding(.vertical, 8)
        }
        .buttonStyle(PlainButtonStyle())
        .sheet(isPresented: $showingPlayerDetail) {
            MarketPlayerDetailView(player: player)
        }
    }
    
    private func formatValue(_ value: Int) -> String {
        if value >= 1000000 {
            return String(format: "%.1fM", Double(value) / 1000000)
        } else if value >= 1000 {
            return String(format: "%.0fk", Double(value) / 1000)
        } else {
            return "\(value)"
        }
    }
}

// MARK: - Stats View mit detaillierten Punktzahl-Statistiken
struct StatsView: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    
    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                if let stats = kickbaseManager.userStats {
                    DetailedStatsView(stats: stats)
                }
                
                // Team-Punktzahl Übersicht
                TeamPointsOverview()
                
                if let league = kickbaseManager.selectedLeague {
                    LeagueInfoView(league: league)
                }
            }
            .padding()
        }
    }
}

// MARK: - Team Punktzahl Übersicht
struct TeamPointsOverview: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Team Punktzahl-Übersicht")
                .font(.title2)
                .fontWeight(.bold)
            
            let totalTeamPoints = kickbaseManager.teamPlayers.reduce(0) { $0 + $1.totalPoints }
            let averageTeamPoints = kickbaseManager.teamPlayers.isEmpty ? 0.0 :
                Double(totalTeamPoints) / Double(kickbaseManager.teamPlayers.count)
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 16) {
                StatCard(
                    title: "Teampunkte Gesamt",
                    value: "\(totalTeamPoints)",
                    icon: "star.fill",
                    color: .orange
                )
                StatCard(
                    title: "Ø pro Spieler",
                    value: String(format: "%.0f", averageTeamPoints),
                    icon: "chart.line.uptrend.xyaxis",
                    color: .blue
                )
                StatCard(
                    title: "Beste Punktzahl",
                    value: "\(kickbaseManager.teamPlayers.map(\.totalPoints).max() ?? 0)",
                    icon: "crown.fill",
                    color: .yellow
                )
                StatCard(
                    title: "Spieleranzahl",
                    value: "\(kickbaseManager.teamPlayers.count)",
                    icon: "person.3.fill",
                    color: .green
                )
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

struct DetailedStatsView: View {
    let stats: UserStats
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Meine Liga-Statistiken")
                .font(.title2)
                .fontWeight(.bold)
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 16) {
                StatCard(title: "Gesamtpunkte", value: "\(stats.points)", icon: "star.fill", color: .orange)
                StatCard(title: "Platzierung", value: "#\(stats.placement)", icon: "trophy.fill", color: .yellow)
                StatCard(title: "Siege", value: "\(stats.won)", icon: "checkmark.circle.fill", color: .green)
                StatCard(title: "Niederlagen", value: "\(stats.lost)", icon: "xmark.circle.fill", color: .red)
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

struct StatCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(color)
            
            Text(value)
                .font(.title)
                .fontWeight(.bold)
            
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(height: 100)
        .frame(maxWidth: .infinity)
        .background(Color(.systemBackground))
        .cornerRadius(8)
    }
}

struct LeagueInfoView: View {
    let league: League
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Liga-Informationen")
                .font(.title2)
                .fontWeight(.bold)
            
            InfoRow(title: "Liga", value: league.name)
            InfoRow(title: "Saison", value: league.season)
            InfoRow(title: "Spieltag", value: "\(league.matchDay)")
            InfoRow(title: "Admin", value: league.adminName)
            InfoRow(title: "Ersteller", value: league.creatorName)
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

struct InfoRow: View {
    let title: String
    let value: String
    
    var body: some View {
        HStack {
            Text(title)
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .fontWeight(.medium)
        }
    }
}

// MARK: - Gifts View
struct GiftsView: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    
    var body: some View {
        List {
            ForEach(kickbaseManager.gifts) { gift in
                GiftRowView(gift: gift) {
                    Task {
                        await kickbaseManager.collectGift(id: gift.id)
                    }
                }
            }
        }
        .refreshable {
            await kickbaseManager.loadGifts()
        }
        .onAppear {
            Task {
                await kickbaseManager.loadGifts()
            }
        }
    }
}

struct GiftRowView: View {
    let gift: Gift
    let onCollect: () -> Void
    
    var body: some View {
        HStack {
            Image(systemName: "gift.fill")
                .foregroundColor(.red)
            
            VStack(alignment: .leading) {
                Text("Level \(gift.level)")
                    .font(.headline)
                Text("€\(gift.amount)")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            if gift.collected {
                Text("Eingesammelt")
                    .font(.caption)
                    .foregroundColor(.secondary)
            } else {
                Button("Sammeln") {
                    onCollect()
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Lineup Optimizer View
struct LineupOptimizerView: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @State private var selectedOptimization: OptimizationType = .averagePoints
    
    enum OptimizationType: String, CaseIterable {
        case averagePoints = "Durchschnittspunkte"
        case totalPoints = "Gesamtpunkte"
    }
    
    // Verfügbare Formationen
    enum Formation: String, CaseIterable {
        case formation442 = "4-4-2"
        case formation424 = "4-2-4"
        case formation343 = "3-4-3"
        case formation433 = "4-3-3"
        case formation532 = "5-3-2"
        case formation352 = "3-5-2"
        case formation541 = "5-4-1"
        case formation451 = "4-5-1"
        case formation361 = "3-6-1"
        case formation523 = "5-2-3"
        
        var positions: (defenders: Int, midfielders: Int, forwards: Int) {
            switch self {
            case .formation442: return (4, 4, 2)
            case .formation424: return (4, 2, 4)
            case .formation343: return (3, 4, 3)
            case .formation433: return (4, 3, 3)
            case .formation532: return (5, 3, 2)
            case .formation352: return (3, 5, 2)
            case .formation541: return (5, 4, 1)
            case .formation451: return (4, 5, 1)
            case .formation361: return (3, 6, 1)
            case .formation523: return (5, 2, 3)
            }
        }
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Header with optimization type selector
                VStack(spacing: 16) {
                    Text("Beste Aufstellung")
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    Picker("Optimierung", selection: $selectedOptimization) {
                        ForEach(OptimizationType.allCases, id: \.self) { type in
                            Text(type.rawValue).tag(type)
                        }
                    }
                    .pickerStyle(SegmentedPickerStyle())
                }
                .padding()
                .background(Color(.systemGray6))
                
                if kickbaseManager.teamPlayers.isEmpty {
                    // Empty state
                    VStack(spacing: 20) {
                        Spacer()
                        
                        Image(systemName: "person.crop.square.fill.and.at.rectangle")
                            .font(.system(size: 60))
                            .foregroundColor(.gray)
                        
                        Text("Keine Spieler geladen")
                            .font(.headline)
                            .foregroundColor(.primary)
                        
                        Text("Lade dein Team, um die beste Aufstellung zu sehen")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                        
                        Button("Team laden") {
                            Task {
                                if let league = kickbaseManager.selectedLeague {
                                    await kickbaseManager.loadTeamPlayers(for: league)
                                }
                            }
                        }
                        .buttonStyle(.borderedProminent)
                        
                        Spacer()
                    }
                } else {
                    // Lineup display
                    ScrollView {
                        VStack(spacing: 20) {
                            // Stats header
                            let optimalResult = getBestPossibleLineup()
                            OptimalLineupStatsView(
                                lineup: optimalResult.lineup,
                                formation: optimalResult.formation,
                                optimizationType: selectedOptimization
                            )
                            
                            // Formation display
                            OptimalLineupFormationView(
                                lineup: optimalResult.lineup,
                                formation: optimalResult.formation
                            )
                            
                            // Reserve players section
                            ReservePlayersView(
                                allPlayers: kickbaseManager.teamPlayers.filter { $0.status != 1 },
                                startingLineup: optimalResult.lineup,
                                optimizationType: selectedOptimization
                            )
                        }
                        .padding()
                    }
                }
            }
            .navigationBarHidden(true)
        }
        .refreshable {
            if let league = kickbaseManager.selectedLeague {
                await kickbaseManager.loadTeamPlayers(for: league)
            }
        }
    }
    
    private func getBestPossibleLineup() -> (lineup: OptimalLineup, formation: Formation) {
        let availablePlayers = kickbaseManager.teamPlayers.filter { $0.status != 1 } // Ausschluss verletzter Spieler
        
        // Gruppiere verfügbare Spieler nach Position
        let goalkeepers = availablePlayers.filter { $0.position == 1 }
        let defenders = availablePlayers.filter { $0.position == 2 }
        let midfielders = availablePlayers.filter { $0.position == 3 }
        let forwards = availablePlayers.filter { $0.position == 4 }
        
        // Muss mindestens einen Torwart haben
        guard !goalkeepers.isEmpty else {
            // Fallback: Verwende alle verfügbaren Spieler
            return createFallbackLineup(from: availablePlayers)
        }
        
        // Finde die beste mögliche Formation basierend auf verfügbaren Spielern
        var bestResult: (lineup: OptimalLineup, formation: Formation, score: Double)?
        
        for formation in Formation.allCases {
            let positions = formation.positions
            
            // Prüfe ob genügend Spieler für diese Formation vorhanden sind
            if defenders.count >= positions.defenders &&
               midfielders.count >= positions.midfielders &&
               forwards.count >= positions.forwards {
                
                let lineup = calculateOptimalLineupForFormation(
                    formation: formation,
                    goalkeepers: goalkeepers,
                    defenders: defenders,
                    midfielders: midfielders,
                    forwards: forwards
                )
                
                let score = calculateLineupScore(lineup)
                
                if bestResult == nil || score > bestResult!.score {
                    bestResult = (lineup, formation, score)
                }
            }
        }
        
        // Falls keine komplette Formation möglich ist, verwende bestmögliche Aufstellung
        if let best = bestResult {
            return (best.lineup, best.formation)
        } else {
            return createBestPossibleLineup(
                goalkeepers: goalkeepers,
                defenders: defenders,
                midfielders: midfielders,
                forwards: forwards
            )
        }
    }
    
    private func calculateOptimalLineupForFormation(
        formation: Formation,
        goalkeepers: [TeamPlayer],
        defenders: [TeamPlayer],
        midfielders: [TeamPlayer],
        forwards: [TeamPlayer]
    ) -> OptimalLineup {
        let positions = formation.positions
        
        // Sortiere Spieler basierend auf gewähltem Kriterium
        let sortedGK: [TeamPlayer]
        let sortedDF: [TeamPlayer]
        let sortedMF: [TeamPlayer]
        let sortedFW: [TeamPlayer]
        
        switch selectedOptimization {
        case .averagePoints:
            sortedGK = goalkeepers.sorted { $0.averagePoints > $1.averagePoints }
            sortedDF = defenders.sorted { $0.averagePoints > $1.averagePoints }
            sortedMF = midfielders.sorted { $0.averagePoints > $1.averagePoints }
            sortedFW = forwards.sorted { $0.averagePoints > $1.averagePoints }
        case .totalPoints:
            sortedGK = goalkeepers.sorted { $0.totalPoints > $1.totalPoints }
            sortedDF = defenders.sorted { $0.totalPoints > $1.totalPoints }
            sortedMF = midfielders.sorted { $0.totalPoints > $1.totalPoints }
            sortedFW = forwards.sorted { $0.totalPoints > $1.totalPoints }
        }
        
        return OptimalLineup(
            goalkeeper: sortedGK.first,
            defenders: Array(sortedDF.prefix(positions.defenders)),
            midfielders: Array(sortedMF.prefix(positions.midfielders)),
            forwards: Array(sortedFW.prefix(positions.forwards))
        )
    }
    
    private func calculateLineupScore(_ lineup: OptimalLineup) -> Double {
        switch selectedOptimization {
        case .averagePoints:
            return lineup.averagePoints
        case .totalPoints:
            return Double(lineup.totalPoints)
        }
    }
    
    private func createBestPossibleLineup(
        goalkeepers: [TeamPlayer],
        defenders: [TeamPlayer],
        midfielders: [TeamPlayer],
        forwards: [TeamPlayer]
    ) -> (lineup: OptimalLineup, formation: Formation) {
        // Verwende so viele Spieler wie möglich, beginnend mit den besten
        let maxDefenders = min(defenders.count, 5) // Max 5 Verteidiger
        let maxMidfielders = min(midfielders.count, 6) // Max 6 Mittelfeldspieler
        let maxForwards = min(forwards.count, 4) // Max 4 Stürmer
        
        // Finde eine passende Formation
        let customFormation: Formation
        switch (maxDefenders, maxMidfielders, maxForwards) {
        case (let d, let m, let f) where d >= 4 && m >= 4 && f >= 2:
            customFormation = .formation442
        case (let d, let m, let f) where d >= 4 && m >= 3 && f >= 3:
            customFormation = .formation433
        case (let d, let m, let f) where d >= 3 && m >= 4 && f >= 3:
            customFormation = .formation343
        case (let d, let m, let f) where d >= 5 && m >= 3 && f >= 2:
            customFormation = .formation532
        case (let d, let m, let f) where d >= 4 && m >= 5 && f >= 1:
            customFormation = .formation451
        default:
            // Fallback auf 4-3-3 mit verfügbaren Spielern
            customFormation = .formation433
        }
        
        let lineup = calculateOptimalLineupForFormation(
            formation: customFormation,
            goalkeepers: goalkeepers,
            defenders: defenders,
            midfielders: midfielders,
            forwards: forwards
        )
        
        return (lineup, customFormation)
    }
    
    private func createFallbackLineup(from players: [TeamPlayer]) -> (lineup: OptimalLineup, formation: Formation) {
        // Notfall: Wenn kein Torwart verfügbar ist, verwende den besten verfügbaren Spieler
        let sortedPlayers: [TeamPlayer]
        
        switch selectedOptimization {
        case .averagePoints:
            sortedPlayers = players.sorted { $0.averagePoints > $1.averagePoints }
        case .totalPoints:
            sortedPlayers = players.sorted { $0.totalPoints > $1.totalPoints }
        }
        
        let lineup = OptimalLineup(
            goalkeeper: sortedPlayers.first,
            defenders: Array(sortedPlayers.dropFirst().prefix(4)),
            midfielders: Array(sortedPlayers.dropFirst(8).prefix(3)),
            forwards: Array(sortedPlayers.dropFirst(11).prefix(3))
        )
        
        return (lineup, .formation433)
    }
}

// MARK: - Optimal Lineup Data Structure
struct OptimalLineup {
    let goalkeeper: TeamPlayer?
    let defenders: [TeamPlayer]
    let midfielders: [TeamPlayer]
    let forwards: [TeamPlayer]
    
    var allPlayers: [TeamPlayer] {
        var players: [TeamPlayer] = []
        if let gk = goalkeeper { players.append(gk) }
        players.append(contentsOf: defenders)
        players.append(contentsOf: midfielders)
        players.append(contentsOf: forwards)
        return players
    }
    
    var totalPoints: Int {
        allPlayers.reduce(0) { $0 + $1.totalPoints }
    }
    
    var averagePoints: Double {
        let total = allPlayers.reduce(0.0) { $0 + $1.averagePoints }
        return allPlayers.isEmpty ? 0.0 : total / Double(allPlayers.count)
    }
    
    var totalMarketValue: Int {
        allPlayers.reduce(0) { $0 + $1.marketValue }
    }
}

// MARK: - Optimal Lineup Stats View
struct OptimalLineupStatsView: View {
    let lineup: OptimalLineup
    let formation: LineupOptimizerView.Formation
    let optimizationType: LineupOptimizerView.OptimizationType
    
    var body: some View {
        VStack(spacing: 16) {
            Text("Aufstellungs-Statistiken")
                .font(.headline)
                .fontWeight(.bold)
            
            // Formation info
            HStack {
                Image(systemName: "rectangle.3.group")
                    .foregroundColor(.blue)
                Text("Formation: \(formation.rawValue)")
                    .font(.subheadline)
                    .fontWeight(.medium)
            }
            .padding(.bottom, 8)
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 12) {
                StatCard(
                    title: "Gesamtpunkte",
                    value: "\(lineup.totalPoints)",
                    icon: "star.fill",
                    color: .orange
                )
                StatCard(
                    title: "Ø Punkte",
                    value: String(format: "%.0f", lineup.averagePoints),
                    icon: "chart.line.uptrend.xyaxis",
                    color: .blue
                )
                StatCard(
                    title: "Teamwert",
                    value: "€\(formatValue(lineup.totalMarketValue))",
                    icon: "eurosign.circle.fill",
                    color: .green
                )
                StatCard(
                    title: "Spieler",
                    value: "\(lineup.allPlayers.count)/11",
                    icon: "person.crop.square.fill.and.at.rectangle",
                    color: .purple
                )
            }
            
            // Warnung wenn nicht genügend Spieler
            if lineup.allPlayers.count < 11 {
                HStack {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundColor(.orange)
                    Text("Nicht genügend Spieler für eine komplette Aufstellung")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.top, 8)
            }
            
            // Kein Torwart Warnung
            if lineup.goalkeeper == nil {
                HStack {
                    Image(systemName: "exclamationmark.circle.fill")
                        .foregroundColor(.red)
                    Text("Kein Torwart verfügbar - bester Spieler als Ersatz eingesetzt")
                        .font(.caption)
                        .foregroundColor(.red)
                }
                .padding(.top, 4)
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
    
    private func formatValue(_ value: Int) -> String {
        if value >= 1000000 {
            return String(format: "%.1fM", Double(value) / 1000000)
        } else if value >= 1000 {
            return String(format: "%.0fk", Double(value) / 1000)
        } else {
            return "\(value)"
        }
    }
}

// MARK: - Optimal Lineup Formation View
struct OptimalLineupFormationView: View {
    let lineup: OptimalLineup
    let formation: LineupOptimizerView.Formation
    
    var body: some View {
        VStack(spacing: 20) {
            Text("Formation (\(formation.rawValue))")
                .font(.headline)
                .fontWeight(.bold)
            
            VStack(spacing: 25) {
                // Forwards
                if !lineup.forwards.isEmpty {
                    HStack(spacing: 8) {
                        ForEach(lineup.forwards, id: \.id) { player in
                            LineupPlayerCard(player: player)
                        }
                    }
                }
                
                // Midfielders
                if !lineup.midfielders.isEmpty {
                    HStack(spacing: 6) {
                        ForEach(lineup.midfielders, id: \.id) { player in
                            LineupPlayerCard(player: player)
                        }
                    }
                }
                
                // Defenders
                if !lineup.defenders.isEmpty {
                    HStack(spacing: 8) {
                        ForEach(lineup.defenders, id: \.id) { player in
                            LineupPlayerCard(player: player)
                        }
                    }
                }
                
                // Goalkeeper
                if let goalkeeper = lineup.goalkeeper {
                    LineupPlayerCard(player: goalkeeper)
                }
            }
            .padding()
            .background(
                LinearGradient(
                    gradient: Gradient(colors: [Color.green.opacity(0.3), Color.green.opacity(0.1)]),
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .cornerRadius(12)
        }
    }
}

// MARK: - Lineup Player Card
struct LineupPlayerCard: View {
    let player: TeamPlayer
    @State private var showingPlayerDetail = false
    
    var body: some View {
        Button(action: {
            showingPlayerDetail = true
        }) {
            VStack(spacing: 4) {
                // Player name - kompakter
                VStack(spacing: 1) {
                    Text(player.firstName)
                        .font(.system(size: 9, weight: .medium))
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                    Text(player.lastName)
                        .font(.system(size: 11, weight: .bold))
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                }
                .foregroundColor(.primary)
                
                // Points - kompakter
                VStack(spacing: 0) {
                    Text(String(format: "%.0f", player.averagePoints))
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.orange)
                    Text("\(player.totalPoints)")
                        .font(.system(size: 8))
                        .foregroundColor(.secondary)
                }
                
                // Status indicator - kleiner
                if player.status == 2 {
                    Image(systemName: "pills.fill")
                        .foregroundColor(.orange)
                        .font(.system(size: 8))
                }
            }
            .frame(width: 50, height: 65)
            .background(Color(.systemBackground))
            .cornerRadius(6)
            .shadow(radius: 1)
        }
        .buttonStyle(PlainButtonStyle())
        .sheet(isPresented: $showingPlayerDetail) {
            PlayerDetailView(player: player)
        }
    }
}

// MARK: - Reserve Players View
struct ReservePlayersView: View {
    let allPlayers: [TeamPlayer]
    let startingLineup: OptimalLineup
    let optimizationType: LineupOptimizerView.OptimizationType
    
    private var reservePlayers: [TeamPlayer] {
        let sortedReserve = allPlayers.filter { player in
            !startingLineup.allPlayers.contains(where: { $0.id == player.id })
        }
        
        // Sortiere nach gewähltem Kriterium
        switch optimizationType {
        case .averagePoints:
            return sortedReserve.sorted { $0.averagePoints > $1.averagePoints }
        case .totalPoints:
            return sortedReserve.sorted { $0.totalPoints > $1.totalPoints }
        }
    }
    
    // Gruppiere Reservespieler nach Position
    private var reservePlayersByPosition: [(String, [TeamPlayer])] {
        let grouped = Dictionary(grouping: reservePlayers) { $0.position }
        
        return [
            ("Torhüter", grouped[1] ?? []),
            ("Abwehr", grouped[2] ?? []),
            ("Mittelfeld", grouped[3] ?? []),
            ("Sturm", grouped[4] ?? [])
        ].filter { !$1.isEmpty }
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Header mit Statistiken
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Reservebank")
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    Text("Beste verfügbare Alternativen")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 2) {
                    Text("\(reservePlayers.count)")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.blue)
                    Text("Spieler")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            if reservePlayers.isEmpty {
                // Empty state
                VStack(spacing: 12) {
                    HStack {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(.green)
                            .font(.title2)
                        
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Optimale Nutzung!")
                                .font(.headline)
                                .foregroundColor(.green)
                            Text("Alle verfügbaren Spieler sind in der Startaufstellung.")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    // Verletzt Spieler Info falls vorhanden
                    let injuredPlayers = allPlayers.filter { $0.status == 1 }
                    if !injuredPlayers.isEmpty {
                        HStack {
                            Image(systemName: "cross.circle.fill")
                                .foregroundColor(.red)
                            Text("\(injuredPlayers.count) verletzte Spieler nicht berücksichtigt")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding(.vertical, 8)
            } else {
                // Reserve Statistiken
                ReserveBenchStatsView(reservePlayers: reservePlayers, optimizationType: optimizationType)
                
                // Positionsweise Gruppierung
                ForEach(reservePlayersByPosition, id: \.0) { positionName, players in
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text(positionName)
                                .font(.headline)
                                .foregroundColor(.primary)
                            
                            Spacer()
                            
                            Text("(\(players.count))")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        
                        LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 8) {
                            ForEach(players, id: \.id) { player in
                                ReservePlayerRow(player: player, showPosition: false)
                            }
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

// MARK: - Reserve Bench Stats
struct ReserveBenchStatsView: View {
    let reservePlayers: [TeamPlayer]
    let optimizationType: LineupOptimizerView.OptimizationType
    
    private var benchStats: (totalPoints: Int, averagePoints: Double, bestPlayer: TeamPlayer?, totalValue: Int) {
        let totalPoints = reservePlayers.reduce(0) { $0 + $1.totalPoints }
        let averagePoints = reservePlayers.isEmpty ? 0.0 : 
            Double(totalPoints) / Double(reservePlayers.count)
        
        let bestPlayer: TeamPlayer?
        switch optimizationType {
        case .averagePoints:
            bestPlayer = reservePlayers.max(by: { $0.averagePoints < $1.averagePoints })
        case .totalPoints:
            bestPlayer = reservePlayers.max(by: { $0.totalPoints < $1.totalPoints })
        }
        
        let totalValue = reservePlayers.reduce(0) { $0 + $1.marketValue }
        
        return (totalPoints, averagePoints, bestPlayer, totalValue)
    }
    
    var body: some View {
        let stats = benchStats
        
        VStack(spacing: 12) {
            Text("Reservebank-Statistiken")
                .font(.subheadline)
                .fontWeight(.semibold)
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 8) {
                BenchStatCard(
                    title: "Gesamt\npunkte",
                    value: "\(stats.totalPoints)",
                    icon: "star.fill",
                    color: .orange
                )
                BenchStatCard(
                    title: "Ø Punkte",
                    value: String(format: "%.0f", stats.averagePoints),
                    icon: "chart.line.uptrend.xyaxis",
                    color: .blue
                )
                BenchStatCard(
                    title: "Bank\nwert",
                    value: "€\(formatValue(stats.totalValue))",
                    icon: "eurosign.circle.fill",
                    color: .green
                )
                if let bestPlayer = stats.bestPlayer {
                    BenchStatCard(
                        title: "Top\nSpieler",
                        value: bestPlayer.lastName,
                        icon: "crown.fill",
                        color: .yellow
                    )
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(8)
    }
    
    private func formatValue(_ value: Int) -> String {
        if value >= 1000000 {
            return String(format: "%.1fM", Double(value) / 1000000)
        } else if value >= 1000 {
            return String(format: "%.0fk", Double(value) / 1000)
        } else {
            return "\(value)"
        }
    }
}

// MARK: - Bench Stat Card
struct BenchStatCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundColor(color)
            
            Text(value)
                .font(.caption)
                .fontWeight(.bold)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            
            Text(title)
                .font(.caption2)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .lineLimit(2)
        }
        .frame(height: 60)
        .frame(maxWidth: .infinity)
        .background(Color(.systemGray6))
        .cornerRadius(6)
    }
}

// MARK: - Reserve Player Row
struct ReservePlayerRow: View {
    let player: TeamPlayer
    let showPosition: Bool
    @State private var showingPlayerDetail = false
    
    init(player: TeamPlayer, showPosition: Bool = true) {
        self.player = player
        self.showPosition = showPosition
    }
    
    var body: some View {
        Button(action: {
            showingPlayerDetail = true
        }) {
            HStack(spacing: 8) {
                // Position Badge - nur anzeigen wenn showPosition true ist
                if showPosition {
                    Text(positionAbbreviation(player.position))
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .frame(width: 20, height: 20)
                        .background(positionColor(player.position))
                        .clipShape(Circle())
                }
                
                // Player Info
                VStack(alignment: .leading, spacing: 1) {
                    HStack(spacing: 3) {
                        Text(player.fullName)
                            .font(.caption)
                            .fontWeight(.medium)
                            .lineLimit(1)
                        
                        // Status indicator
                        if player.status == 2 {
                            Image(systemName: "pills.fill")
                                .foregroundColor(.orange)
                                .font(.system(size: 6))
                        }
                    }
                    
                    Text(player.fullTeamName)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
                
                Spacer()
                
                // Points und Wert
                VStack(alignment: .trailing, spacing: 1) {
                    Text(String(format: "%.0f", player.averagePoints))
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.orange)
                    
                    Text("\(player.totalPoints)")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    
                    Text("€\(formatValue(player.marketValue))")
                        .font(.caption2)
                        .foregroundColor(.green)
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Color(.systemBackground))
            .cornerRadius(6)
            .shadow(radius: 0.5)
        }
        .buttonStyle(PlainButtonStyle())
        .sheet(isPresented: $showingPlayerDetail) {
            PlayerDetailView(player: player)
        }
    }
    
    private func formatValue(_ value: Int) -> String {
        if value >= 1000000 {
            return String(format: "%.1fM", Double(value) / 1000000)
        } else if value >= 1000 {
            return String(format: "%.0fk", Double(value) / 1000)
        } else {
            return "\(value)"
        }
    }
}

// MARK: - Helper functions moved to TeamManagementViews.swift to avoid duplication

#Preview {
    MainDashboardView()
        .environmentObject(KickbaseManager())
        .environmentObject(AuthenticationManager())
}
