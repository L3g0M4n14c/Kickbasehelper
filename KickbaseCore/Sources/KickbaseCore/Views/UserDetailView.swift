import SwiftUI

struct UserDetailView: View {
    let user: LeagueUser
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @State private var userPlayers: [Player] = []
    @State private var isLoading = false
    
    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // User Header Section
                UserHeaderSection(user: user)
                
                // User Stats Section
                UserStatsSection(user: user)
                
                // User Squad Section
                UserSquadSection(players: userPlayers, isLoading: isLoading)
            }
            .padding()
        }
        .navigationTitle(user.name)
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .task {
            guard let league = kickbaseManager.selectedLeague else { return }
            
            isLoading = true
            defer { isLoading = false }
            
            if let players = await kickbaseManager.loadUserSquad(
                leagueId: league.id,
                userId: user.id
            ) {
                userPlayers = players
            }
        }
    }
}

// MARK: - User Header Section
struct UserHeaderSection: View {
    let user: LeagueUser
    
    var body: some View {
        VStack(spacing: 12) {
            // Team Name
            Text(user.teamName)
                .font(.title)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)
            
            // User Name
            Text(user.name)
                .font(.title3)
                .foregroundColor(.secondary)
            
            // Points Badge
            VStack(spacing: 4) {
                Text("\(user.points)")
                    .font(.system(size: 48, weight: .bold))
                    .foregroundColor(.primary)
                Text("Punkte")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.blue.opacity(0.1))
            )
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.systemBackgroundCompat)
                .shadow(radius: 2)
        )
    }
}

// MARK: - User Stats Section
struct UserStatsSection: View {
    let user: LeagueUser
    
    var body: some View {
        VStack(spacing: 16) {
            Text("Statistiken")
                .font(.headline)
                .frame(maxWidth: .infinity, alignment: .leading)
            
            LazyVGrid(columns: [
                GridItem(.flexible()),
                GridItem(.flexible())
            ], spacing: 16) {
                StatCard(title: "Platzierung", value: "\(user.placement).")
                StatCard(title: "Budget", value: formatCurrency(user.budget))
                StatCard(title: "Teamwert", value: formatCurrency(user.teamValue))
                StatCard(title: "Siege", value: "\(user.won)")
                StatCard(title: "Unentschieden", value: "\(user.drawn)")
                StatCard(title: "Niederlagen", value: "\(user.lost)")
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.systemBackgroundCompat)
                .shadow(radius: 2)
        )
    }
    
    private func formatCurrency(_ value: Int) -> String {
        let millions = Double(value) / 1_000_000.0
        return String(format: "%.1fM €", millions)
    }
}

// MARK: - Stat Card
struct StatCard: View {
    let title: String
    let value: String
    
    var body: some View {
        VStack(spacing: 8) {
            Text(value)
                .font(.title2)
                .fontWeight(.bold)
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.blue.opacity(0.1))
        )
    }
}

// MARK: - User Squad Section
struct UserSquadSection: View {
    let players: [Player]
    let isLoading: Bool
    
    var body: some View {
        VStack(spacing: 16) {
            HStack {
                Text("Kader")
                    .font(.headline)
                Spacer()
                if !players.isEmpty {
                    Text("\(players.count) Spieler")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            if isLoading {
                ProgressView("Lade Kader...")
                    .frame(maxWidth: .infinity)
                    .padding()
            } else if players.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "person.3.fill")
                        .font(.system(size: 40))
                        .foregroundColor(.gray)
                    Text("Keine Spieler verfügbar")
                        .font(.subheadline)
                        .foregroundColor(.gray)
                }
                .frame(maxWidth: .infinity)
                .padding()
            } else {
                LazyVStack(spacing: 12) {
                    ForEach(groupedPlayers.sorted(by: { $0.key < $1.key }), id: \.key) { position, positionPlayers in
                        PositionGroupView(
                            positionName: positionName(for: position),
                            players: positionPlayers
                        )
                    }
                }
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.systemBackgroundCompat)
                .shadow(radius: 2)
        )
    }
    
    private var groupedPlayers: [Int: [Player]] {
        Dictionary(grouping: players, by: { $0.position })
    }
    
    private func positionName(for position: Int) -> String {
        switch position {
        case 1: return "Torwart"
        case 2: return "Abwehr"
        case 3: return "Mittelfeld"
        case 4: return "Sturm"
        default: return "Unbekannt"
        }
    }
}

// MARK: - Position Group View
struct PositionGroupView: View {
    let positionName: String
    let players: [Player]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(positionName)
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(.secondary)
            
            ForEach(players) { player in
                UserSquadPlayerRow(player: player)
            }
        }
    }
}

// MARK: - User Squad Player Row
struct UserSquadPlayerRow: View {
    let player: Player
    
    var body: some View {
        HStack(spacing: 12) {
            // Player Image
            AsyncImage(url: player.imageUrl) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } placeholder: {
                ZStack {
                    Circle()
                        .fill(positionColor(player.position).opacity(0.3))
                    Image(systemName: "person.fill")
                        .foregroundColor(positionColor(player.position))
                }
            }
            .frame(width: 50, height: 50)
            .clipShape(Circle())
            
            // Player Info
            VStack(alignment: .leading, spacing: 4) {
                Text(player.fullName)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                HStack(spacing: 8) {
                    Text(player.teamName)
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Text("•")
                        .foregroundColor(.secondary)
                    
                    Text(player.positionName)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            Spacer()
            
            // Player Stats
            VStack(alignment: .trailing, spacing: 4) {
                Text("\(player.totalPoints)")
                    .font(.subheadline)
                    .fontWeight(.bold)
                Text("Pkt.")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.secondarySystemBackgroundCompat)
        )
    }
}

// MARK: - Helper Functions
func positionColor(_ position: Int) -> Color {
    switch position {
    case 1: return .yellow
    case 2: return .green
    case 3: return .blue
    case 4: return .red
    default: return .gray
    }
}
