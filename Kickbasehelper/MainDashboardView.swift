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
                
                // Gifts Tab
                GiftsView()
                    .tabItem {
                        Image(systemName: "gift.fill")
                        Text("Geschenke")
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
    
    var body: some View {
        VStack(spacing: 0) {
            // Budget und Teamwert Header
            if let stats = kickbaseManager.userStats {
                TeamStatsHeader(stats: stats)
            }
            
            // Spielerliste mit Punktzahlen
            List {
                ForEach(kickbaseManager.teamPlayers) { player in
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
}

// MARK: - Player Row mit prominenten Punktzahlen
struct PlayerRowView: View {
    let player: TeamPlayer
    
    var body: some View {
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
                    Text(String(format: "%.1f", player.averagePoints))
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
    
    var body: some View {
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
                    Text(String(format: "%.1f Ø", player.averagePoints))
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
                    value: String(format: "%.1f", averageTeamPoints),
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

#Preview {
    MainDashboardView()
        .environmentObject(KickbaseManager())
        .environmentObject(AuthenticationManager())
}
