import SwiftUI

struct MainDashboardView: View {
    @EnvironmentObject var authManager: AuthenticationManager
    @EnvironmentObject var kickbaseManager: KickbaseManager
    
    var body: some View {
        TabView {
            DashboardTab()
                .tabItem {
                    Image(systemName: "house")
                    Text("Dashboard")
                }
            
            TeamTab()
                .tabItem {
                    Image(systemName: "person.3")
                    Text("Team")
                }
            
            MarketTab()
                .tabItem {
                    Image(systemName: "cart")
                    Text("Markt")
                }
            
            GiftsTab(manager: kickbaseManager)
                .tabItem {
                    Image(systemName: "gift")
                    Text("Geschenke")
                }
        }
        .task {
            if let token = authManager.accessToken {
                kickbaseManager.setAuthToken(token)
                await kickbaseManager.loadUserData()
            }
        }
    }
}

struct DashboardTab: View {
    @EnvironmentObject var authManager: AuthenticationManager
    @EnvironmentObject var kickbaseManager: KickbaseManager
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    // User Header
                    if let user = authManager.currentUser {
                        UserHeaderCard(user: user)
                    }
                    
                    // League Selection
                    LeagueSelectionCard()
                    
                    // Budget & Team Value (Liga-spezifisch)
                    if kickbaseManager.selectedLeague != nil {
                        BudgetCard()
                    }
                    
                    // Team Stats (Liga-spezifisch)
                    if kickbaseManager.selectedLeague != nil {
                        TeamStatsCard()
                    }
                    
                    // Quick Actions
                    QuickActionsCard()
                    
                    // Daily Rewards Preview
                    DailyRewardsCard()
                    
                    Spacer(minLength: 20)
                }
                .padding()
            }
            .navigationTitle("Dashboard")
            .refreshable {
                if let token = authManager.accessToken {
                    kickbaseManager.setAuthToken(token)
                    await kickbaseManager.loadUserData()
                }
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Logout") {
                        authManager.logout()
                    }
                }
            }
        }
    }
}

struct UserHeaderCard: View {
    let user: User
    
    var body: some View {
        VStack(spacing: 15) {
            HStack {
                VStack(alignment: .leading) {
                    Text(user.name)
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    Text(user.teamName)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                VStack(alignment: .trailing) {
                    Text("\(user.points) Punkte")
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    Text("Platz \(user.placement)")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(15)
    }
}

struct BudgetCard: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    
    var body: some View {
        VStack(spacing: 15) {
            Text("Finanzen")
                .font(.headline)
                .frame(maxWidth: .infinity, alignment: .leading)
            
            if let userStats = kickbaseManager.userStats {
                HStack(spacing: 20) {
                    VStack(alignment: .leading) {
                        Text("Budget")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("€\(userStats.budget / 1000)k")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                    }
                    
                    Rectangle()
                        .frame(width: 1)
                        .foregroundColor(.gray.opacity(0.3))
                    
                    VStack(alignment: .leading) {
                        Text("Teamwert")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("€\(userStats.teamValue / 1000)k")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.blue)
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing) {
                        Text("Belohnungen")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("€\((kickbaseManager.gifts.filter({ !$0.collected }).count * 50000) / 1000)k")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.orange)
                    }
                }
            } else {
                Text("Lade Liga-Daten...")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(15)
    }
}

struct TeamStatsCard: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    
    var body: some View {
        VStack(spacing: 15) {
            Text("Team Statistiken")
                .font(.headline)
                .frame(maxWidth: .infinity, alignment: .leading)
            
            HStack {
                VStack(alignment: .leading) {
                    Text("Spieler")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text("\(kickbaseManager.teamPlayers.count)")
                        .font(.title3)
                        .fontWeight(.semibold)
                }
                
                Spacer()
                
                VStack(alignment: .trailing) {
                    let totalTrend = kickbaseManager.teamPlayers.reduce(0) { $0 + $1.marketValueTrend }
                    let trendColor: Color = totalTrend > 0 ? .green : totalTrend < 0 ? .red : .gray
                    
                    Text("Trend")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    HStack(spacing: 4) {
                        if totalTrend > 0 {
                            Image(systemName: "arrow.up")
                        } else if totalTrend < 0 {
                            Image(systemName: "arrow.down")
                        } else {
                            Image(systemName: "minus")
                        }
                        
                        Text("€\(abs(totalTrend) / 1000)k")
                    }
                    .font(.title3)
                    .fontWeight(.semibold)
                    .foregroundColor(trendColor)
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(15)
    }
}

struct QuickActionsCard: View {
    var body: some View {
        VStack(spacing: 15) {
            Text("Schnellzugriff")
                .font(.headline)
                .frame(maxWidth: .infinity, alignment: .leading)
            
            HStack(spacing: 15) {
                ActionButton(
                    icon: "person.3",
                    title: "Team",
                    color: .blue
                ) {
                    // Action handled by tab selection
                }
                
                ActionButton(
                    icon: "cart",
                    title: "Markt",
                    color: .green
                ) {
                    // Action handled by tab selection
                }
                
                ActionButton(
                    icon: "gift",
                    title: "Geschenke",
                    color: .orange
                ) {
                    // Action handled by tab selection
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(15)
    }
}

struct ActionButton: View {
    let icon: String
    let title: String
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(color)
                
                Text(title)
                    .font(.caption)
                    .foregroundColor(.primary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(Color(.systemBackground))
            .cornerRadius(10)
        }
    }
}

struct DailyRewardsCard: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    
    var body: some View {
        VStack(spacing: 15) {
            HStack {
                Text("Tägliche Belohnungen")
                    .font(.headline)
                
                Spacer()
                
                if kickbaseManager.gifts.filter({ !$0.collected }).count > 0 {
                    Text("\(kickbaseManager.gifts.filter({ !$0.collected }).count) verfügbar")
                        .font(.caption)
                        .foregroundColor(.orange)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.orange.opacity(0.2))
                        .cornerRadius(8)
                }
            }
            
            if kickbaseManager.gifts.isEmpty {
                Text("Keine Belohnungen verfügbar")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
            } else {
                HStack {
                    Text("Verfügbar:")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    Spacer()
                    
                    Text("€\((kickbaseManager.gifts.filter({ !$0.collected }).count * 50000) / 1000)k")
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(.green)
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(15)
    }
}

struct LeagueSelectionCard: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    
    var body: some View {
        VStack(spacing: 15) {
            Text("Liga Auswahl")
                .font(.headline)
                .frame(maxWidth: .infinity, alignment: .leading)
            
            if kickbaseManager.leagues.isEmpty {
                HStack {
                    ProgressView()
                        .scaleEffect(0.8)
                    Text("Lade Ligen...")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 20)
            } else {
                Picker("Liga auswählen", selection: $kickbaseManager.selectedLeague) {
                    ForEach(kickbaseManager.leagues) { league in
                        Text(league.name)
                            .tag(league as League?)
                    }
                }
                .pickerStyle(MenuPickerStyle())
                .frame(maxWidth: .infinity)
                .background(Color(.systemGray5))
                .cornerRadius(10)
                .onChange(of: kickbaseManager.selectedLeague) { oldValue, newValue in
                    if let newLeague = newValue, newLeague.id != oldValue?.id {
                        Task {
                            await kickbaseManager.selectLeague(newLeague)
                        }
                    }
                }
                
                if let selectedLeague = kickbaseManager.selectedLeague {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Aktuelle Liga: \(selectedLeague.name)")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(.primary)
                        
                        HStack {
                            Text("Spieltag:")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Spacer()
                            Text("\(selectedLeague.matchDay)")
                                .font(.caption)
                                .foregroundColor(.primary)
                        }
                        
                        HStack {
                            Text("Saison:")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Spacer()
                            Text(selectedLeague.season)
                                .font(.caption)
                                .foregroundColor(.primary)
                        }
                        
                        HStack {
                            Text("Meine Position:")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Spacer()
                            Text("Platz \(selectedLeague.currentUser.placement)")
                                .font(.caption)
                                .fontWeight(.semibold)
                                .foregroundColor(.primary)
                        }
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(10)
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(15)
    }
}

#Preview {
    MainDashboardView()
        .environmentObject(AuthenticationManager())
        .environmentObject(KickbaseManager())
}
