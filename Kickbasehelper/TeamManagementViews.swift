import SwiftUI

struct TeamTab: View {
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
        NavigationView {
            VStack {
                // Debug Info
                VStack(alignment: .leading, spacing: 5) {
                    Text("Debug Info:")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text("Spieler geladen: \(kickbaseManager.teamPlayers.count)")
                        .font(.caption)
                        .foregroundColor(.blue)
                    Text("Gefilterte Spieler: \(filteredAndSortedPlayers.count)")
                        .font(.caption)
                        .foregroundColor(.green)
                    if let league = kickbaseManager.selectedLeague {
                        Text("Liga: \(league.name)")
                            .font(.caption)
                            .foregroundColor(.orange)
                    } else {
                        Text("Keine Liga ausgewählt")
                            .font(.caption)
                            .foregroundColor(.red)
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 5)
                .background(Color(.systemGray6))
                .cornerRadius(8)
                .padding(.horizontal)
                
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
                
                // Players List or Empty State
                if kickbaseManager.teamPlayers.isEmpty {
                    VStack(spacing: 20) {
                        Spacer()
                        
                        Image(systemName: "person.3.fill")
                            .font(.system(size: 60))
                            .foregroundColor(.gray)
                        
                        Text("Keine Spieler geladen")
                            .font(.headline)
                            .foregroundColor(.primary)
                        
                        Text("Ziehe nach unten um zu aktualisieren oder wähle eine Liga aus")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                        
                        Button("Team neu laden") {
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
                    // Players List
                    List {
                        ForEach(Array(filteredAndSortedPlayers.enumerated()), id: \.offset) { index, player in
                            TeamPlayerRow(teamPlayer: player)
                                .id("\(player.id)-\(index)") // Eindeutige ID durch Index
                        }
                    }
                    .refreshable {
                        if let league = kickbaseManager.selectedLeague {
                            await kickbaseManager.loadTeamPlayers(for: league)
                        }
                    }
                }
            }
            .navigationTitle("Mein Team (\(kickbaseManager.teamPlayers.count))")
            .onAppear {
                print("🎯 TeamTab appeared - Players count: \(kickbaseManager.teamPlayers.count)")
                if kickbaseManager.teamPlayers.isEmpty {
                    print("🔄 TeamTab: No players found, triggering reload...")
                    Task {
                        if let league = kickbaseManager.selectedLeague {
                            await kickbaseManager.loadTeamPlayers(for: league)
                        }
                    }
                }
            }
        }
    }
    
    private var filteredAndSortedPlayers: [TeamPlayer] {
        let filtered = searchText.isEmpty ? kickbaseManager.teamPlayers :
            kickbaseManager.teamPlayers.filter { player in
                player.firstName.localizedCaseInsensitiveContains(searchText) ||
                player.lastName.localizedCaseInsensitiveContains(searchText) ||
                player.teamName.localizedCaseInsensitiveContains(searchText)
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

struct TeamPlayerRow: View {
    let teamPlayer: TeamPlayer
    
    var body: some View {
        HStack(spacing: 12) {
            // Position indicator
            VStack {
                Text(positionAbbreviation(teamPlayer.position))
                    .font(.caption2)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(positionColor(teamPlayer.position))
                    .cornerRadius(4)
                
                Text("\(teamPlayer.number)")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            // Player Info
            VStack(alignment: .leading, spacing: 2) {
                Text(teamPlayer.fullName)
                    .font(.headline)
                    .lineLimit(1)
                
                Text(teamPlayer.teamName)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
            
            Spacer()
            
            // Stats
            VStack(alignment: .trailing, spacing: 2) {
                HStack(spacing: 4) {
                    Text("€\(teamPlayer.marketValue / 1000)k")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    
                    if teamPlayer.marketValueTrend > 0 {
                        Image(systemName: "arrow.up")
                            .foregroundColor(.green)
                            .font(.caption2)
                    } else if teamPlayer.marketValueTrend < 0 {
                        Image(systemName: "arrow.down")
                            .foregroundColor(.red)
                            .font(.caption2)
                    } else {
                        Image(systemName: "minus")
                            .foregroundColor(.gray)
                            .font(.caption2)
                    }
                }
                
                Text("\(teamPlayer.totalPoints) Punkte")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

struct MarketTab: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @State private var searchText = ""
    @State private var selectedPosition: Int = 0 // 0 = All, 1 = TW, 2 = ABW, 3 = MF, 4 = ST
    
    var body: some View {
        NavigationView {
            VStack {
                // Filter Controls
                VStack(spacing: 10) {
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.gray)
                        TextField("Spieler oder Verein suchen...", text: $searchText)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                    }
                    
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 10) {
                            FilterButton(title: "Alle", isSelected: selectedPosition == 0) {
                                selectedPosition = 0
                            }
                            FilterButton(title: "TW", isSelected: selectedPosition == 1) {
                                selectedPosition = 1
                            }
                            FilterButton(title: "ABW", isSelected: selectedPosition == 2) {
                                selectedPosition = 2
                            }
                            FilterButton(title: "MF", isSelected: selectedPosition == 3) {
                                selectedPosition = 3
                            }
                            FilterButton(title: "ST", isSelected: selectedPosition == 4) {
                                selectedPosition = 4
                            }
                        }
                        .padding(.horizontal)
                    }
                }
                .padding(.horizontal)
                
                // Market Players List
                List(filteredMarketPlayers, id: \.id) { player in
                    MarketPlayerRow(marketPlayer: player)
                }
                .refreshable {
                    if let league = kickbaseManager.selectedLeague {
                        await kickbaseManager.loadMarketPlayers(for: league)
                    }
                }
            }
            .navigationTitle("Transfermarkt")
        }
    }
    
    private var filteredMarketPlayers: [MarketPlayer] {
        kickbaseManager.marketPlayers.filter { player in
            let matchesSearch = searchText.isEmpty ||
                player.firstName.localizedCaseInsensitiveContains(searchText) ||
                player.lastName.localizedCaseInsensitiveContains(searchText) ||
                player.teamName.localizedCaseInsensitiveContains(searchText)
            
            let matchesPosition = selectedPosition == 0 || player.position == selectedPosition
            
            return matchesSearch && matchesPosition
        }
        .sorted { $0.price < $1.price }
    }
}

struct MarketPlayerRow: View {
    let marketPlayer: MarketPlayer
    
    var body: some View {
        HStack(spacing: 12) {
            // Position indicator
            Text(marketPlayer.positionName)
                .font(.caption2)
                .fontWeight(.bold)
                .foregroundColor(.white)
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
                .background(positionColor(marketPlayer.position))
                .cornerRadius(4)
            
            // Player Info
            VStack(alignment: .leading, spacing: 2) {
                Text(marketPlayer.fullName)
                    .font(.headline)
                    .lineLimit(1)
                
                HStack {
                    Text(marketPlayer.teamName)
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Spacer()
                    
                    Text("Von: \(marketPlayer.seller.name)")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
            
            Spacer()
            
            // Price and Market Value
            VStack(alignment: .trailing, spacing: 2) {
                Text("€\(marketPlayer.price / 1000)k")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.green)
                
                HStack(spacing: 2) {
                    Text("MW: €\(marketPlayer.marketValue / 1000)k")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    
                    if marketPlayer.marketValueTrend > 0 {
                        Image(systemName: "arrow.up")
                            .foregroundColor(.green)
                            .font(.caption2)
                    } else if marketPlayer.marketValueTrend < 0 {
                        Image(systemName: "arrow.down")
                            .foregroundColor(.red)
                            .font(.caption2)
                    }
                }
            }
        }
        .padding(.vertical, 4)
    }
}

struct GiftsTab: View {
    @ObservedObject var manager: KickbaseManager
    @State private var isCollectingAll = false
    
    var body: some View {
        VStack {
            if manager.isLoading {
                // ...existing code...
            } else {
                VStack(spacing: 16) {
                    // Alle sammeln Button
                    Button(action: {
                        Task {
                            isCollectingAll = true
                            for gift in manager.gifts.filter({ !$0.collected }) {
                                await manager.collectGift(id: gift.id)
                            }
                            isCollectingAll = false
                            await manager.loadGifts()
                        }
                    }) {
                        HStack {
                            Image(systemName: "gift.fill")
                            Text("Alle sammeln (\(manager.gifts.filter { !$0.collected }.count))")
                        }
                        .foregroundColor(.white)
                        .padding()
                        .background(Color.orange)
                        .cornerRadius(10)
                    }
                    .disabled(isCollectingAll || manager.gifts.filter { !$0.collected }.isEmpty)
                    
                    if isCollectingAll {
                        ProgressView("Sammle alle Geschenke...")
                            .progressViewStyle(CircularProgressViewStyle())
                    }
                    
                    // Geschenke Liste
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(manager.gifts, id: \.id) { gift in
                                GiftCard(gift: gift, manager: manager)
                            }
                        }
                        .padding()
                    }
                }
            }
        }
        .navigationTitle("Geschenke")
        .refreshable {
            await manager.loadGifts()
        }
        .onAppear {
            if manager.gifts.isEmpty {
                Task {
                    await manager.loadGifts()
                }
            }
        }
    }
}

struct GiftCard: View {
    let gift: Gift
    @ObservedObject var manager: KickbaseManager
    @State private var isCollecting = false
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("Tagesgeschenk")
                    .font(.headline)
                
                Text("€\(gift.amount, specifier: "%.0f")")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.green)
                
                Text(gift.collected ? "Bereits gesammelt" : "Bereit zum Sammeln")
                    .font(.caption)
                    .foregroundColor(gift.collected ? .gray : .orange)
            }
            
            Spacer()
            
            if !gift.collected {
                Button(action: {
                    Task {
                        isCollecting = true
                        await manager.collectGift(id: gift.id)
                        isCollecting = false
                        await manager.loadGifts()
                    }
                }) {
                    if isCollecting {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .scaleEffect(0.8)
                    } else {
                        Image(systemName: "hand.tap.fill")
                            .foregroundColor(.white)
                    }
                }
                .frame(width: 50, height: 50)
                .background(Color.orange)
                .cornerRadius(25)
                .disabled(isCollecting)
            } else {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.green)
                    .font(.title2)
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

struct FilterButton: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.caption)
                .fontWeight(.medium)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isSelected ? Color.blue : Color(.systemGray5))
                .foregroundColor(isSelected ? .white : .primary)
                .cornerRadius(8)
        }
    }
}

// Helper Functions
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
