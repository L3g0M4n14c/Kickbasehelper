import SwiftUI

struct TeamTab: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @State private var sortBy: SortOption = .marketValue
    @State private var searchText = ""
    @State private var playersForSale: Set<String> = []
    
    enum SortOption: String, CaseIterable {
        case name = "Name"
        case marketValue = "Marktwert"
        case points = "Punkte"
        case trend = "Trend"
        case position = "Position"
    }
    
    // Berechnung des Gesamtwerts der zum Verkauf ausgewählten Spieler
    private var totalSaleValue: Int {
        return kickbaseManager.teamPlayers
            .filter { playersForSale.contains($0.id) }
            .reduce(0) { $0 + $1.marketValue }
    }
    
    var body: some View {
        NavigationView {
            VStack {
                // Budget-Anzeige mit verkaufbaren Spielern
                if let stats = kickbaseManager.userStats {
                    TeamBudgetHeader(
                        currentBudget: stats.budget,
                        saleValue: totalSaleValue
                    )
                    .padding(.horizontal)
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
                            TeamPlayerRowWithSale(
                                teamPlayer: player,
                                isSelectedForSale: playersForSale.contains(player.id),
                                onToggleSale: { isSelected in
                                    if isSelected {
                                        playersForSale.insert(player.id)
                                    } else {
                                        playersForSale.remove(player.id)
                                    }
                                }
                            )
                            .id("\(player.id)-\(index)")
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

struct TeamPlayerRow: View {
    let teamPlayer: TeamPlayer
    @State private var showingPlayerDetail = false
    
    var body: some View {
        Button(action: {
            print("🔄 TeamPlayerRow: Tapped on player \(teamPlayer.fullName)")
            showingPlayerDetail = true
        }) {
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
                    HStack(spacing: 4) {
                        Text(teamPlayer.fullName)
                            .font(.headline)
                            .lineLimit(1)
                        
                        // Status-Icons basierend auf st-Feld aus API-Daten anzeigen
                        if teamPlayer.status == 1 {
                            // Verletzt - rotes Kreuz
                            Image(systemName: "cross.circle.fill")
                                .foregroundColor(.red)
                                .font(.caption)
                        } else if teamPlayer.status == 2 {
                            // Angeschlagen - Tabletten-Icon
                            Image(systemName: "pills.fill")
                                .foregroundColor(.orange)
                                .font(.caption)
                        } else if teamPlayer.status == 4 {
                            // Aufbautraining - Hantel-Icon
                            Image(systemName: "dumbbell.fill")
                                .foregroundColor(.blue)
                                .font(.caption)
                        }
                    }
                    
                    Text(teamPlayer.fullTeamName)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
                
                Spacer()
                
                // Stats - Durchschnittspunktzahl als große Zahl, Gesamtpunktzahl als kleine Zahl
                VStack(alignment: .trailing, spacing: 2) {
                    Text("\(teamPlayer.averagePoints, specifier: "%.0f")")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.primary)
                    
                    Text("\(teamPlayer.totalPoints) Gesamt")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    HStack(spacing: 4) {
                        Text("€\(teamPlayer.marketValue / 1000)k")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        
                        if teamPlayer.tfhmvt > 0 {
                            Image(systemName: "arrow.up")
                                .foregroundColor(.green)
                                .font(.caption2)
                        } else if teamPlayer.tfhmvt < 0 {
                            Image(systemName: "arrow.down")
                                .foregroundColor(.red)
                                .font(.caption2)
                        } else {
                            Image(systemName: "minus")
                                .foregroundColor(.gray)
                                .font(.caption2)
                        }
                    }
                }
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(PlainButtonStyle())
        .sheet(isPresented: $showingPlayerDetail) {
            PlayerDetailView(player: teamPlayer)
        }
    }
}

struct TeamPlayerRowWithSale: View {
    let teamPlayer: TeamPlayer
    let isSelectedForSale: Bool
    let onToggleSale: (Bool) -> Void
    @State private var showingPlayerDetail = false
    
    var body: some View {
        Button(action: {
            print("🔄 TeamPlayerRow: Tapped on player \(teamPlayer.fullName)")
            showingPlayerDetail = true
        }) {
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
                    HStack(spacing: 4) {
                        Text(teamPlayer.fullName)
                            .font(.headline)
                            .lineLimit(1)
                        
                        // Status-Icons basierend auf st-Feld aus API-Daten anzeigen
                        if teamPlayer.status == 1 {
                            // Verletzt - rotes Kreuz
                            Image(systemName: "cross.circle.fill")
                                .foregroundColor(.red)
                                .font(.caption)
                        } else if teamPlayer.status == 2 {
                            // Angeschlagen - Tabletten-Icon
                            Image(systemName: "pills.fill")
                                .foregroundColor(.orange)
                                .font(.caption)
                        } else if teamPlayer.status == 4 {
                            // Aufbautraining - Hantel-Icon
                            Image(systemName: "dumbbell.fill")
                                .foregroundColor(.blue)
                                .font(.caption)
                        }
                    }
                    
                    Text(teamPlayer.fullTeamName)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
                
                Spacer()
                
                // Stats - Durchschnittspunktzahl als große Zahl, Gesamtpunktzahl als kleine Zahl
                VStack(alignment: .trailing, spacing: 2) {
                    Text("\(teamPlayer.averagePoints, specifier: "%.0f")")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.primary)
                    
                    Text("\(teamPlayer.totalPoints) Gesamt")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    HStack(spacing: 4) {
                        Text("€\(teamPlayer.marketValue / 1000)k")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        
                        if teamPlayer.tfhmvt > 0 {
                            Image(systemName: "arrow.up")
                                .foregroundColor(.green)
                                .font(.caption2)
                        } else if teamPlayer.tfhmvt < 0 {
                            Image(systemName: "arrow.down")
                                .foregroundColor(.red)
                                .font(.caption2)
                        } else {
                            Image(systemName: "minus")
                                .foregroundColor(.gray)
                                .font(.caption2)
                        }
                    }
                }
                
                // Sale Toggle
                Toggle(isOn: Binding(
                    get: { isSelectedForSale },
                    set: { newValue in
                        onToggleSale(newValue)
                    }
                )) {
                    Text("")
                }
                .toggleStyle(SwitchToggleStyle(tint: .blue))
                .frame(width: 50)
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(PlainButtonStyle())
        .sheet(isPresented: $showingPlayerDetail) {
            PlayerDetailView(player: teamPlayer)
        }
    }
}

struct MarketTab: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @State private var searchText = ""
    @State private var selectedPosition: Int = 0 // 0 = All, 1 = TW, 2 = ABW, 3 = MF, 4 = ST
    @State private var isManuallyLoading = false
    @State private var forceRefreshId = UUID()
    
    var body: some View {
        NavigationView {
            VStack {
                // Debug Info (nur in Debug-Builds)
                #if DEBUG
                VStack(alignment: .leading, spacing: 4) {
                    Text("Debug Info:")
                        .font(.caption2)
                        .foregroundColor(.orange)
                    Text("Market Players Count: \(kickbaseManager.marketPlayers.count)")
                        .font(.caption2)
                        .foregroundColor(.orange)
                    Text("Filtered Count: \(filteredMarketPlayers.count)")
                        .font(.caption2)
                        .foregroundColor(.orange)
                    Text("Selected League: \(kickbaseManager.selectedLeague?.name ?? "None")")
                        .font(.caption2)
                        .foregroundColor(.orange)
                    Text("Is Loading: \(kickbaseManager.isLoading)")
                        .font(.caption2)
                        .foregroundColor(.orange)
                    Text("Manual Loading: \(isManuallyLoading)")
                        .font(.caption2)
                        .foregroundColor(.orange)
                    if let error = kickbaseManager.errorMessage {
                        Text("Error: \(error)")
                            .font(.caption2)
                            .foregroundColor(.red)
                    }
                }
                .padding(.horizontal)
                .padding(.bottom, 8)
                #endif
                
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
                
                // Market Players List or Empty State
                if kickbaseManager.isLoading || isManuallyLoading {
                    VStack(spacing: 20) {
                        Spacer()
                        ProgressView("Lade Transfermarkt...")
                            .progressViewStyle(CircularProgressViewStyle())
                        Text("Ladevorgang läuft...")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Spacer()
                    }
                } else if let error = kickbaseManager.errorMessage {
                    VStack(spacing: 20) {
                        Spacer()
                        
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.system(size: 60))
                            .foregroundColor(.red)
                        
                        Text("Fehler beim Laden")
                            .font(.headline)
                            .foregroundColor(.primary)
                        
                        Text(error)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                        
                        Button("Erneut versuchen") {
                            manualReload()
                        }
                        .buttonStyle(.borderedProminent)
                        .disabled(kickbaseManager.selectedLeague == nil)
                        
                        Spacer()
                    }
                } else if kickbaseManager.marketPlayers.isEmpty {
                    VStack(spacing: 20) {
                        Spacer()
                        
                        Image(systemName: "cart.fill")
                            .font(.system(size: 60))
                            .foregroundColor(.gray)
                        
                        Text("Keine Transfermarkt-Spieler verfügbar")
                            .font(.headline)
                            .foregroundColor(.primary)
                        
                        Text("Derzeit sind keine Spieler auf dem Transfermarkt")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                        
                        Button("Aktualisieren") {
                            manualReload()
                        }
                        .buttonStyle(.borderedProminent)
                        .disabled(kickbaseManager.selectedLeague == nil)
                        
                        Spacer()
                    }
                } else {
                    // Market Players List
                    List(filteredMarketPlayers, id: \.id) { player in
                        MarketPlayerRow(marketPlayer: player)
                            .id("\(player.id)-\(forceRefreshId)")
                    }
                    .id(forceRefreshId)
                    .refreshable {
                        await performRefresh()
                    }
                }
            }
            .navigationTitle("Transfermarkt (\(kickbaseManager.marketPlayers.count))")
            .onAppear {
                print("🎯 MarketTab appeared")
                print("   - Market players count: \(kickbaseManager.marketPlayers.count)")
                print("   - Selected league: \(kickbaseManager.selectedLeague?.name ?? "None")")
                print("   - Is loading: \(kickbaseManager.isLoading)")
                
                // Force initial load if needed
                if kickbaseManager.marketPlayers.isEmpty && !kickbaseManager.isLoading {
                    print("🔄 MarketTab: No market players found, triggering reload...")
                    Task {
                        await performInitialLoad()
                    }
                }
            }
            .onChange(of: kickbaseManager.selectedLeague) { oldLeague, newLeague in
                print("🔄 MarketTab: League changed from \(oldLeague?.name ?? "None") to \(newLeague?.name ?? "None")")
                if let newLeague = newLeague {
                    Task {
                        await performInitialLoad()
                    }
                }
            }
            .onChange(of: kickbaseManager.marketPlayers) { oldPlayers, newPlayers in
                print("🔄 MarketTab: Market players changed from \(oldPlayers.count) to \(newPlayers.count)")
                forceRefreshId = UUID()
            }
        }
    }
    
    private func performInitialLoad() async {
        guard let league = kickbaseManager.selectedLeague else {
            print("❌ MarketTab: No league selected for initial load")
            return
        }
        
        print("🔄 MarketTab: Starting initial load for league \(league.name)")
        await kickbaseManager.loadMarketPlayers(for: league)
        print("✅ MarketTab: Initial load completed. Market players count: \(kickbaseManager.marketPlayers.count)")
    }
    
    private func performRefresh() async {
        guard let league = kickbaseManager.selectedLeague else {
            print("❌ MarketTab: No league selected for refresh")
            return
        }
        
        print("🔄 MarketTab: Starting refresh for league \(league.name)")
        await kickbaseManager.loadMarketPlayers(for: league)
        print("✅ MarketTab: Refresh completed. Market players count: \(kickbaseManager.marketPlayers.count)")
    }
    
    private func manualReload() {
        guard let league = kickbaseManager.selectedLeague else {
            print("❌ MarketTab: No league selected for manual reload")
            return
        }
        
        isManuallyLoading = true
        print("🔄 MarketTab: Starting manual reload for league \(league.name)")
        
        Task {
            await kickbaseManager.loadMarketPlayers(for: league)
            await MainActor.run {
                isManuallyLoading = false
                print("✅ MarketTab: Manual reload completed. Market players count: \(kickbaseManager.marketPlayers.count)")
            }
        }
    }
    
    private var filteredMarketPlayers: [MarketPlayer] {
        kickbaseManager.marketPlayers.filter { player in
            let matchesSearch = searchText.isEmpty ||
                player.firstName.localizedCaseInsensitiveContains(searchText) ||
                player.lastName.localizedCaseInsensitiveContains(searchText) ||
                player.fullTeamName.localizedCaseInsensitiveContains(searchText)
            
            let matchesPosition = selectedPosition == 0 || player.position == selectedPosition
            
            return matchesSearch && matchesPosition
        }
        .sorted { $0.price < $1.price }
    }
}

struct MarketPlayerRow: View {
    let marketPlayer: MarketPlayer
    @State private var showingPlayerDetail = false
    
    var body: some View {
        Button(action: {
            print("🔄 MarketPlayerRow: Tapped on player \(marketPlayer.fullName)")
            showingPlayerDetail = true
        }) {
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
                    HStack(spacing: 4) {
                        Text(marketPlayer.fullName)
                            .font(.headline)
                            .lineLimit(1)
                        
                        // Status-Icons basierend auf status-Feld aus API-Daten anzeigen
                        if marketPlayer.status == 1 {
                            // Verletzt - rotes Kreuz
                            Image(systemName: "cross.circle.fill")
                                .foregroundColor(.red)
                                .font(.caption)
                        } else if marketPlayer.status == 2 {
                            // Angeschlagen - Tabletten-Icon
                            Image(systemName: "pills.fill")
                                .foregroundColor(.orange)
                                .font(.caption)
                        } else if marketPlayer.status == 4 {
                            // Aufbautraining - Hantel-Icon
                            Image(systemName: "dumbbell.fill")
                                .foregroundColor(.blue)
                                .font(.caption)
                        }
                    }
                    
                    HStack {
                        Text(marketPlayer.fullTeamName)
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
        .buttonStyle(PlainButtonStyle())
        .sheet(isPresented: $showingPlayerDetail) {
            MarketPlayerDetailView(player: marketPlayer)
        }
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

struct MarketPlayerDetailView: View {
    let player: MarketPlayer
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Player Header
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Text(player.fullName)
                                .font(.largeTitle)
                                .fontWeight(.bold)
                            
                            Spacer()
                            
                            Text(positionAbbreviation(player.position))
                                .font(.headline)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(positionColor(player.position))
                                .cornerRadius(8)
                        }
                        
                        Text(player.fullTeamName)
                            .font(.title3)
                            .foregroundColor(.secondary)
                        
                        if player.status == 1 {
                            HStack {
                                Image(systemName: "cross.circle.fill")
                                    .foregroundColor(.red)
                                Text("Verletzt")
                                    .foregroundColor(.red)
                            }
                            .font(.caption)
                        } else if player.status == 2 {
                            HStack {
                                Image(systemName: "pills.fill")
                                    .foregroundColor(.orange)
                                Text("Angeschlagen")
                                    .foregroundColor(.orange)
                            }
                            .font(.caption)
                        } else if player.status == 4 {
                            HStack {
                                Image(systemName: "dumbbell.fill")
                                    .foregroundColor(.blue)
                                Text("Aufbautraining")
                                    .foregroundColor(.blue)
                            }
                            .font(.caption)
                        }
                    }
                    
                    Divider()
                    
                    // Market Info
                    VStack(alignment: .leading, spacing: 15) {
                        Text("Transferinformationen")
                            .font(.headline)
                        
                        HStack {
                            VStack(alignment: .leading) {
                                Text("Preis")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                Text("€\(player.price / 1000)k")
                                    .font(.title2)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.green)
                            }
                            
                            Spacer()
                            
                            VStack(alignment: .trailing) {
                                Text("Marktwert")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                HStack {
                                    Text("€\(player.marketValue / 1000)k")
                                        .font(.title2)
                                        .fontWeight(.semibold)
                                    
                                    if player.marketValueTrend > 0 {
                                        Image(systemName: "arrow.up")
                                            .foregroundColor(.green)
                                    } else if player.marketValueTrend < 0 {
                                        Image(systemName: "arrow.down")
                                            .foregroundColor(.red)
                                    }
                                }
                            }
                        }
                        
                        VStack(alignment: .leading) {
                            Text("Verkäufer")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Text(player.seller.name)
                                .font(.body)
                        }
                    }
                    
                    Spacer()
                }
                .padding()
            }
            .navigationTitle("Spielerdetails")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Schließen") {
                        dismiss()
                    }
                }
            }
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

struct TeamBudgetHeader: View {
    let currentBudget: Int
    let saleValue: Int
    
    private var totalBudget: Int {
        return currentBudget + saleValue
    }
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("Aktuelles Budget")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text("€\(currentBudget / 1000)k")
                    .font(.headline)
                    .fontWeight(.bold)
            }
            
            Spacer()
            
            VStack(alignment: .trailing, spacing: 4) {
                Text("Budget + Verkäufe")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text("€\(totalBudget / 1000)k")
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(saleValue > 0 ? .green : .primary)
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}
