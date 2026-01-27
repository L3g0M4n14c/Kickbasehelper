import SwiftUI

struct StandardNavigationModifier: ViewModifier {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @EnvironmentObject var authManager: AuthenticationManager

    func body(content: Content) -> some View {
        content
            .navigationTitle(kickbaseManager.selectedLeague?.name ?? "Kickbase Helper")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailingCompat) {
                    Button("Logout") {
                        authManager.logout()
                    }
                }

                ToolbarItem(placement: .navigationBarLeadingCompat) {
                    if kickbaseManager.isLoading {
                        ProgressView()
                            .scaleEffect(0.8)
                    }
                }
            }
            #if os(iOS)
                .navigationBarTitleDisplayMode(.inline)
            #endif
    }
}

struct TeamPlayerCounts {
    let total: Int
    let goalkeepers: Int
    let defenders: Int
    let midfielders: Int
    let forwards: Int
}

struct MainDashboardView: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @EnvironmentObject var authManager: AuthenticationManager
    @EnvironmentObject var ligainsiderService: LigainsiderService
    @State private var selectedTab = 0
    @Environment(\.horizontalSizeClass) var horizontalSizeClass

    var body: some View {
        ZStack {
            if !ligainsiderService.isLigainsiderReady {
                // Loading State - warte bis Ligainsider Cache fertig ist
                VStack(spacing: 20) {
                    ProgressView()
                        .scaleEffect(1.5)
                    Text("Ligainsider-Daten werden geladen...")
                        .foregroundColor(.secondary)
                        .font(.headline)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color(.systemBackground))
            } else {
                // Normale Layouts wenn Ligainsider fertig ist
                Group {
                    if horizontalSizeClass == .regular {
                        // iPad Layout - Sidebar Navigation
                        iPadLayout
                    } else {
                        // iPhone Layout - Tab Navigation
                        iPhoneLayout
                    }
                }
                .environmentObject(kickbaseManager)
                .macOSOptimized()
            }
        }
        .onAppear {
            // Automatisches Laden aller Daten beim ersten Start
            Task {
                // 1. Lade ZUERST Ligainsider Daten (mit await für vollständigen Cache)
                print("🔄 MainDashboard: Starting Ligainsider data load...")
                await ligainsiderService.fetchLineupsAsync()
                print(
                    "✅ MainDashboard: Lineups loaded with cache size: \(ligainsiderService.playerCacheCount)"
                )
                // NOTE: fetchAllSquadsAsync() is disabled because:
                // - Squad data is already loaded from fetchLineupsAsync() (match.homeSquad + match.awaySquad)
                // - fetchAllSquadsAsync() was finding 0 players anyway
                // await ligainsiderService.fetchAllSquadsAsync()
                print(
                    "✅ MainDashboard: Ligainsider cache is complete with \(ligainsiderService.playerCacheCount) players"
                )

                // 2. Lade danach die Kickbase-Daten (Team + Market Players)
                print("🔄 MainDashboard: Now loading Kickbase data...")
                await kickbaseManager.loadUserData()

                // 3. Zusätzlich: Lade Team-Daten wenn Liga verfügbar
                if let league = kickbaseManager.selectedLeague {
                    await kickbaseManager.loadTeamPlayers(for: league)
                    await kickbaseManager.loadMarketPlayers(for: league)
                }
                print("✅ MainDashboard: All data loaded completely")
            }
        }
    }

    // iPad-spezifisches Layout mit Sidebar
    private var iPadLayout: some View {
        #if os(iOS)
            NavigationSplitView {
                // Sidebar
                List(
                    selection: Binding<Int?>(
                        get: { selectedTab },
                        set: { selectedTab = $0 ?? 0 }
                    )
                ) {
                    NavigationLink(value: 0) {
                        Label("Team", systemImage: "person.3.fill")
                    }
                    .tag(0)

                    NavigationLink(value: 1) {
                        Label("Markt", systemImage: "cart.fill")
                    }
                    .tag(1)

                    NavigationLink(value: 2) {
                        Label("Verkaufen", systemImage: "dollarsign.circle.fill")
                    }
                    .tag(2)

                    NavigationLink(value: 3) {
                        Label(
                            "Aufstellung", systemImage: "person.crop.square.fill.and.at.rectangle")
                    }
                    .tag(3)

                    NavigationLink(value: 4) {
                        Label("Transfer-Tipps", systemImage: "person.crop.circle.badge.plus")
                    }
                    .tag(4)

                    NavigationLink(value: 5) {
                        Label("Ligainsider", systemImage: "list.bullet.clipboard")
                    }
                    .tag(5)

                    NavigationLink(value: 6) {
                        Label("Tabelle", systemImage: "list.number")
                    }
                    .tag(6)

                    NavigationLink(value: 7) {
                        Label("Live", systemImage: "sportscourt.fill")
                    }
                    .tag(7)
                }
                .navigationTitle("Kickbase Helper")
                .navigationBarTitleDisplayMode(.large)
                .toolbar(content: {
                    ToolbarItem(placement: .navigationBarTrailingCompat) {
                        Button("Logout") {
                            authManager.logout()
                        }
                    }

                    ToolbarItem(placement: .navigationBarLeadingCompat) {
                        if kickbaseManager.isLoading {
                            ProgressView()
                                .scaleEffect(0.8)
                        }
                    }
                })
            } detail: {
                // Detail View
                Group {
                    switch selectedTab {
                    case 0:
                        TeamView()
                    case 1:
                        MarketView()
                    case 2:
                        SalesRecommendationView()
                    case 3:
                        LineupOptimizerView()
                    case 4:
                        TransferRecommendationsView(kickbaseManager: kickbaseManager)
                    case 5:
                        LigainsiderView()
                    case 6:
                        LeagueTableView()
                    case 7:
                        LiveView(kickbaseManager: kickbaseManager)
                            .environmentObject(ligainsiderService)
                    default:
                        TeamView()
                    }
                }
                .navigationTitle(getNavigationTitle())
                .navigationBarTitleDisplayMode(.large)
            }
        #else
            Text("iPad Layout not supported on Android")
        #endif
    }

    // iPhone-spezifisches Layout mit Tabs
    private var iPhoneLayout: some View {
        TabView(selection: $selectedTab) {
            // Team Tab mit Punktzahlen
            NavigationStack {
                TeamView()
                    .modifier(StandardNavigationModifier())
            }
            .tabItem {
                Image(systemName: "person.3.fill")
                Text("Team")
            }
            .tag(0)

            // Market Tab
            NavigationStack {
                MarketView()
                    .modifier(StandardNavigationModifier())
            }
            .tabItem {
                Image(systemName: "cart.fill")
                Text("Markt")
            }
            .tag(1)

            // Sales Recommendation Tab (ersetzt Stats Tab)
            NavigationStack {
                SalesRecommendationView()
                    .modifier(StandardNavigationModifier())
            }
            .tabItem {
                Image(systemName: "dollarsign.circle.fill")
                Text("Verkaufen")
            }
            .tag(2)

            // Lineup Optimizer Tab
            NavigationStack {
                LineupOptimizerView()
                    .modifier(StandardNavigationModifier())
            }
            .tabItem {
                Image(systemName: "person.crop.square.fill.and.at.rectangle")
                Text("Aufstellung")
            }
            .tag(3)

            // Transfer Recommendations Tab
            NavigationStack {
                TransferRecommendationsView(kickbaseManager: kickbaseManager)
                    .modifier(StandardNavigationModifier())
            }
            .tabItem {
                Image(systemName: "person.crop.circle.badge.plus")
                Text("Transfer-Tipps")
            }
            .tag(4)

            // Ligainsider Tab
            NavigationStack {
                LigainsiderView()
                    .modifier(StandardNavigationModifier())
            }
            .tabItem {
                Image(systemName: "list.bullet.clipboard")
                Text("Ligainsider")
            }
            .tag(5)

            // League Table Tab
            NavigationStack {
                LeagueTableView()
                    .modifier(StandardNavigationModifier())
            }
            .tabItem {
                Image(systemName: "list.number")
                Text("Tabelle")
            }
            .tag(6)

            // Live View Tab
            NavigationStack {
                LiveView(kickbaseManager: kickbaseManager)
                    .environmentObject(ligainsiderService)
                    .modifier(StandardNavigationModifier())
            }
            .tabItem {
                Image(systemName: "sportscourt.fill")
                Text("Live")
            }
            .tag(7)
        }
        .onAppear {
            #if os(iOS) && os(iOS)
                // Konfiguriere Navigation Bar Appearance für iPhone (nicht transparent)
                let appearance = UINavigationBarAppearance()
                appearance.configureWithOpaqueBackground()
                appearance.backgroundColor = UIColor.systemBackground
                appearance.titleTextAttributes = [.foregroundColor: UIColor.label]
                appearance.largeTitleTextAttributes = [.foregroundColor: UIColor.label]

                UINavigationBar.appearance().standardAppearance = appearance
                UINavigationBar.appearance().scrollEdgeAppearance = appearance
                UINavigationBar.appearance().compactAppearance = appearance
            #endif
        }
    }

    private func getNavigationTitle() -> String {
        switch selectedTab {
        case 0:
            return "Team"
        case 1:
            return "Markt"
        case 2:
            return "Verkaufen"
        case 3:
            return "Aufstellung"
        case 4:
            return "Transfer-Tipps"
        case 5:
            return "Ligainsider"
        case 6:
            return "Tabelle"
        case 7:
            return "Live"
        default:
            return "Team"
        }
    }
}

// MARK: - Team View mit prominenten Punktzahlen
struct TeamView: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @EnvironmentObject var ligainsiderService: LigainsiderService
    @State private var sortBy: SortOption = .marketValue
    @State private var playersForSale: Set<String> = []
    @State private var showRecommendations = false

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

    // Berechnung der Spieleranzahl nach Positionen (ohne zum Verkauf markierte)
    private var playerCounts: TeamPlayerCounts {
        let availablePlayers = kickbaseManager.teamPlayers.filter {
            !playersForSale.contains($0.id)
        }

        let goalkeepers = availablePlayers.filter { $0.position == 1 }.count
        let defenders = availablePlayers.filter { $0.position == 2 }.count
        let midfielders = availablePlayers.filter { $0.position == 3 }.count
        let forwards = availablePlayers.filter { $0.position == 4 }.count
        let total = availablePlayers.count

        return TeamPlayerCounts(
            total: total,
            goalkeepers: goalkeepers,
            defenders: defenders,
            midfielders: midfielders,
            forwards: forwards
        )
    }

    var body: some View {
        VStack(spacing: 0) {
            // Tab Toggle
            Picker("View", selection: $showRecommendations) {
                Text("Mein Team").tag(false)
                Text("Verkaufs-Tipps").tag(true)
            }
            .pickerStyle(.segmented)
            .padding()

            if showRecommendations {
                // Verkaufs-Empfehlungen View
                SaleRecommendationsView(kickbaseManager: kickbaseManager)
            } else {
                // Original Team View
                VStack(spacing: 0) {
                    // Neue Budget-Anzeige mit Verkaufslogik - mit explizitem padding-bottom
                    if let stats = kickbaseManager.userStats {
                        TeamBudgetHeaderMain(
                            currentBudget: stats.budget,
                            saleValue: totalSaleValue
                        )
                        .padding(.bottom, 16)
                    }

                    // Player Count Overview and Sort Controls - mit separatem padding
                    VStack(spacing: 15) {
                        // Spieleranzahl-Übersicht
                        PlayerCountOverview(playerCounts: playerCounts)

                        // Sort Controls
                        HStack {
                            Text("Sortieren:")
                                .font(.caption)
                                .foregroundColor(.secondary)

                            Picker("Sortierung", selection: $sortBy) {
                                ForEach(SortOption.allCases, id: \.self) { option in
                                    Text(option.rawValue).tag(option)
                                }
                            }
                            .pickerStyle(.segmented)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 8)

                    // Spielerliste mit Verkaufs-Toggles
                    List {
                        ForEach(filteredAndSortedPlayers) { player in
                            PlayerRowViewWithSale(
                                player: player,
                                isSelectedForSale: playersForSale.contains(player.id),
                                onToggleSale: { isSelected in
                                    if isSelected {
                                        playersForSale.insert(player.id)
                                    } else {
                                        playersForSale.remove(player.id)
                                    }
                                }
                            )
                        }
                    }
                    .environmentObject(kickbaseManager)
                    .environmentObject(ligainsiderService)
                    .refreshable {
                        if let league = kickbaseManager.selectedLeague {
                            await kickbaseManager.loadTeamPlayers(for: league)
                        }
                    }
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
        // Entfernung der Suchfunktionalität - zeige alle Spieler
        return kickbaseManager.teamPlayers.sorted(by: { player1, player2 in
            switch sortBy {
            case .name:
                return player1.lastName < player2.lastName
            case .marketValue:
                return player1.marketValue > player2.marketValue
            case .points:
                return player1.totalPoints > player2.totalPoints
            case .trend:
                return player1.tfhmvt > player2.tfhmvt
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

    @EnvironmentObject var kickbaseManager: KickbaseManager
    @EnvironmentObject var ligainsiderService: LigainsiderService

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
                            .font(.subheadline)
                            .fontWeight(.medium)

                        // Ligainsider Status Icon
                        let status = ligainsiderService.getPlayerStatus(
                            firstName: player.firstName, lastName: player.lastName)
                        if status != .out {
                            Image(systemName: ligainsiderService.getIcon(for: status))
                                .foregroundColor(
                                    ligainsiderService.getColor(for: status)
                                )
                                .font(.caption)
                        }

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
                        } else if player.status == 4 {
                            // Aufbautraining - Hantel-Icon
                            Image(systemName: "dumbbell.fill")
                                .foregroundColor(.blue)
                                .font(.caption)
                        } else if player.status == 8 {
                            // Sperre - rote Karte
                            Image(systemName: "rectangle.fill")
                                .foregroundColor(.red)
                                .font(.caption)
                        }
                    }

                    // Team
                    Text(player.fullTeamName)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                // PUNKTZAHLEN - Feste Breite um Umbrüche zu vermeiden
                VStack(alignment: .trailing, spacing: 6) {
                    // Durchschnittspunkte - groß und prominent mit fester Breite
                    HStack(spacing: 4) {
                        Image(systemName: "star.fill")
                            .font(.subheadline)
                            .foregroundColor(.orange)
                        Text(String(format: "%.0f", player.averagePoints))
                            .font(.headline)
                            .fontWeight(.bold)
                            .foregroundColor(.primary)
                            .minimumScaleFactor(0.8)
                            .lineLimit(1)
                            .onAppear {
                                print("📊 Displaying average points: \(player.averagePoints)")
                            }
                    }
                    .frame(minWidth: 60, alignment: .trailing)

                    // Gesamtpunkte - jetzt kleinere Anzeige mit fester Breite
                    HStack(spacing: 4) {
                        Image(systemName: "sum")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        Text("\(player.totalPoints)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .minimumScaleFactor(0.8)
                            .lineLimit(1)
                            .onAppear {
                                print("📊 Displaying total points: \(player.totalPoints)")
                            }
                    }
                    .frame(minWidth: 60, alignment: .trailing)
                }

                VStack(alignment: .trailing, spacing: 4) {
                    // Marktwert mit fester Breite
                    Text("€\(formatValue(player.marketValue))")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .minimumScaleFactor(0.8)
                        .lineLimit(1)
                        .frame(minWidth: 50, alignment: .trailing)

                    // Trend - verwende tfhmvt (Marktwertänderung seit letztem Update)
                    if player.tfhmvt != 0 {
                        HStack(spacing: 2) {
                            Image(systemName: player.tfhmvt >= 0 ? "arrow.up" : "arrow.down")
                                .font(.caption2)
                                .foregroundColor(player.tfhmvt >= 0 ? .green : .red)
                            Text("€\(formatValue(abs(player.tfhmvt)))")
                                .font(.caption)
                                .foregroundColor(player.tfhmvt >= 0 ? .green : .red)
                                .minimumScaleFactor(0.8)
                                .lineLimit(1)
                        }
                        .frame(minWidth: 50, alignment: .trailing)
                    }
                }
            }
            .padding(.vertical, 8)
        }
        #if os(iOS)
            .buttonStyle(PlainButtonStyle())
        #endif
        .sheet(isPresented: $showingPlayerDetail) {
            PlayerDetailView(player: player)
                .environmentObject(kickbaseManager)
                .environmentObject(ligainsiderService)
        }
    }
}

// MARK: - Player Row mit Verkaufs-Toggle
struct PlayerRowViewWithSale: View {
    let player: TeamPlayer
    let isSelectedForSale: Bool
    let onToggleSale: (Bool) -> Void

    @State private var showingPlayerDetail = false
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @EnvironmentObject var ligainsiderService: LigainsiderService

    private var photoUrl: URL? {
        if let ligaPlayer = ligainsiderService.getLigainsiderPlayer(
            firstName: player.firstName, lastName: player.lastName),
            let imgString = ligaPlayer.imageUrl,
            let url = URL(string: imgString)
        {
            return url
        }
        return player.imageUrl
    }

    var body: some View {
        HStack(spacing: 12) {
            // Spieler-Foto oder Position Badge
            if let url = photoUrl {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .empty:
                        PositionBadge(position: player.position)
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: 32, height: 32)
                            .clipShape(Circle())
                    case .failure:
                        PositionBadge(position: player.position)
                    @unknown default:
                        PositionBadge(position: player.position)
                    }
                }
                .frame(width: 32, height: 32)
            } else {
                PositionBadge(position: player.position)
            }

            // Spieler-Info Bereich (klickbar für Details)
            Button(action: {
                showingPlayerDetail = true
            }) {
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        // Name mit Status-Icons
                        HStack(spacing: 4) {
                            Text(player.fullName)
                                .font(.subheadline)
                                .fontWeight(.medium)

                            // Ligainsider Status Icon
                            let status = ligainsiderService.getPlayerStatus(
                                firstName: player.firstName, lastName: player.lastName)
                            if status != .out {
                                Image(systemName: ligainsiderService.getIcon(for: status))
                                    .foregroundColor(
                                        ligainsiderService.getColor(for: status)
                                    )
                                    .font(.caption)
                            }

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
                            } else if player.status == 4 {
                                // Aufbautraining - Hantel-Icon
                                Image(systemName: "dumbbell.fill")
                                    .foregroundColor(.blue)
                                    .font(.caption)
                            } else if player.status == 8 {
                                // Sperre - rote Karte
                                Image(systemName: "rectangle.fill")
                                    .foregroundColor(.red)
                                    .font(.caption)
                            }
                        }

                        // Nur Vereinsname anzeigen
                        HStack(spacing: 4) {
                            Text(player.fullTeamName)
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .fontWeight(.medium)
                        }
                    }

                    Spacer()

                    // PUNKTZAHLEN - Feste Breite um Umbrüche zu vermeiden
                    VStack(alignment: .trailing, spacing: 6) {
                        // Durchschnittspunkte - groß und prominent mit fester Breite
                        HStack(spacing: 4) {
                            Image(systemName: "star.fill")
                                .font(.subheadline)
                                .foregroundColor(.orange)
                            Text(String(format: "%.0f", player.averagePoints))
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .foregroundColor(.primary)
                                .minimumScaleFactor(0.8)
                                .lineLimit(1)
                        }
                        .frame(minWidth: 60, alignment: .trailing)

                        // Gesamtpunkte - jetzt kleinere Anzeige mit fester Breite
                        HStack(spacing: 4) {
                            Image(systemName: "sum")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                            Text("\(player.totalPoints)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .minimumScaleFactor(0.8)
                                .lineLimit(1)
                        }
                        .frame(minWidth: 60, alignment: .trailing)
                    }

                    VStack(alignment: .trailing, spacing: 4) {
                        // Marktwert mit fester Breite
                        Text("€\(formatValue(player.marketValue))")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .minimumScaleFactor(0.8)
                            .lineLimit(1)
                            .frame(minWidth: 50, alignment: .trailing)

                        // Trend - verwende tfhmvt (Marktwertänderung seit letztem Update)
                        if player.tfhmvt != 0 {
                            HStack(spacing: 2) {
                                Image(systemName: player.tfhmvt >= 0 ? "arrow.up" : "arrow.down")
                                    .font(.caption2)
                                    .foregroundColor(player.tfhmvt >= 0 ? .green : .red)
                                Text("€\(formatValue(abs(player.tfhmvt)))")
                                    .font(.caption)
                                    .foregroundColor(player.tfhmvt >= 0 ? .green : .red)
                                    .minimumScaleFactor(0.8)
                                    .lineLimit(1)
                            }
                            .frame(minWidth: 50, alignment: .trailing)
                        }
                    }
                }
            }
            #if os(iOS)
                .buttonStyle(PlainButtonStyle())
            #endif
            .sheet(isPresented: $showingPlayerDetail) {
                PlayerDetailView(player: player)
                    .environmentObject(kickbaseManager)
                    .environmentObject(ligainsiderService)
            }

            // Toggle für Verkauf (separater Bereich)
            Toggle(
                isOn: Binding<Bool>(
                    get: { isSelectedForSale },
                    set: { newValue in
                        onToggleSale(newValue)
                    }
                )
            ) {
                Text("")
            }

            .frame(width: 50, height: 30)
        }
        .padding(.vertical, 8)
        .sheet(isPresented: $showingPlayerDetail) {
            PlayerDetailView(player: player)
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
        .background(Color.systemGray6Compat)
        .cornerRadius(12)
        .padding(.horizontal)
    }
}

// MARK: - Market View mit Punktzahlen und Sortierung
struct MarketView: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @State private var sortBy: MarketSortOption = .price
    @State private var searchText = ""

    enum MarketSortOption: String, CaseIterable {
        case price = "Preis"
        case marketValue = "Marktwert"
        case points = "Punkte"
        case position = "Position"
        case expiry = "Ablaufdatum"
        case offers = "Gebote"
    }

    var body: some View {
        VStack(spacing: 0) {
            // Search and Sort Controls
            VStack(spacing: 10) {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.gray)
                    TextField("Spieler suchen...", text: $searchText)
                        .textFieldStyle(.roundedBorder)
                }

                HStack {
                    Text("Sortieren:")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    Picker("Sortierung", selection: $sortBy) {
                        ForEach(MarketSortOption.allCases, id: \.self) { option in
                            Text(option.rawValue).tag(option)
                        }
                    }
                    .pickerStyle(.segmented)
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)

            List {
                ForEach(filteredAndSortedMarketPlayers) { player in
                    MarketPlayerRowView(player: player)
                }
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

    private var filteredAndSortedMarketPlayers: [MarketPlayer] {
        let filtered =
            searchText.isEmpty
            ? kickbaseManager.marketPlayers
            : kickbaseManager.marketPlayers.filter { player in
                player.firstName.lowercased().contains(searchText.lowercased())
                    || player.lastName.lowercased().contains(searchText.lowercased())
                    || player.fullTeamName.lowercased().contains(searchText.lowercased())
                    || (player.owner?.name.lowercased().contains(searchText.lowercased()) ?? false)
            }

        return filtered.sorted(by: { player1, player2 in
            switch sortBy {
            case .price:
                // Sortierung für Preis basiert nur auf Marktwert
                return player1.marketValue > player2.marketValue
            case .marketValue:
                // Sortierung für Marktwert basiert nur auf Marktwert
                return player1.marketValue > player2.marketValue
            case .points:
                return player1.averagePoints > player2.averagePoints
            case .position:
                return player1.position < player2.position
            case .expiry:
                // Sortierung nach Ablaufdatum orientiert sich am exs-Feld (niedrigster Wert oben)
                return player1.exs < player2.exs
            case .offers:
                return player1.offers > player2.offers
            }
        })
    }

    private func parseExpiryDate(_ dateString: String) -> Date {
        let formatter = ISO8601DateFormatter()
        return formatter.date(from: dateString) ?? Date.distantFuture
    }
}

// MARK: - Market Player Row mit Punktzahlen
struct MarketPlayerRowView: View {
    let player: MarketPlayer
    @State private var showingPlayerDetail = false
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @EnvironmentObject var ligainsiderService: LigainsiderService

    private var photoUrl: URL? {
        if let ligaPlayer = ligainsiderService.getLigainsiderPlayer(
            firstName: player.firstName, lastName: player.lastName),
            let imgString = ligaPlayer.imageUrl,
            let url = URL(string: imgString)
        {
            return url
        }
        return player.imageUrl
    }

    var body: some View {
        Button(action: {
            print("🔄 MarketPlayerRowView: Tapped on player \(player.fullName)")
            showingPlayerDetail = true
        }) {
            HStack(spacing: 12) {
                // Spieler-Foto oder Position Badge
                if let url = photoUrl {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .empty:
                            PositionBadge(position: player.position)
                        case .success(let image):
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: 32, height: 32)
                                .clipShape(Circle())
                        case .failure:
                            PositionBadge(position: player.position)
                        @unknown default:
                            PositionBadge(position: player.position)
                        }
                    }
                    .frame(width: 32, height: 32)
                } else {
                    PositionBadge(position: player.position)
                }

                VStack(alignment: .leading, spacing: 4) {
                    // Name mit Status-Icons
                    HStack(spacing: 4) {
                        Text(player.fullName)
                            .font(.headline)
                            .fontWeight(.medium)

                        // Ligainsider Status Icon
                        let status = ligainsiderService.getPlayerStatus(
                            firstName: player.firstName, lastName: player.lastName)
                        if status != .out {
                            Image(systemName: ligainsiderService.getIcon(for: status))
                                .foregroundColor(
                                    ligainsiderService.getColor(for: status)
                                )
                                .font(.caption)
                        }

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
                        } else if player.status == 4 {
                            // Aufbautraining - Hantel-Icon
                            Image(systemName: "dumbbell.fill")
                                .foregroundColor(.blue)
                                .font(.caption)
                        } else if player.status == 8 {
                            // Sperre - rote Karte
                            Image(systemName: "rectangle.fill")
                                .foregroundColor(.red)
                                .font(.caption)
                        }
                    }

                    // Team und Owner
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(player.fullTeamName)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .onAppear {
                                print("📝 Displaying team '\(player.fullTeamName)'")
                            }

                        // Owner-Information anzeigen, falls vorhanden
                        if let owner = player.owner {
                            HStack(spacing: 4) {
                                Image(systemName: "person.fill")
                                    .font(.caption2)
                                    .foregroundColor(.blue)
                                Text("Besitzer: \(owner.name)")
                                    .font(.caption2)
                                    .foregroundColor(.blue)
                                    .fontWeight(.medium)

                                // Verified badge falls der User verifiziert ist
                                if owner.isVerified {
                                    Image(systemName: "checkmark.seal.fill")
                                        .font(.caption2)
                                        .foregroundColor(.green)
                                }
                            }
                            .onAppear {
                                print(
                                    "👤 Displaying owner: '\(owner.name)' (ID: \(owner.id), verified: \(owner.isVerified))"
                                )
                            }
                        }
                    }
                }

                Spacer()

                // Punktzahlen für Marktplayer - mit fester Breite
                VStack(alignment: .trailing, spacing: 4) {
                    // Durchschnittspunkte - jetzt groß und prominent mit fester Breite
                    HStack(spacing: 4) {
                        Image(systemName: "star.fill")
                            .font(.subheadline)
                            .foregroundColor(.orange)
                        Text(String(format: "%.0f", player.averagePoints))
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.primary)
                            .minimumScaleFactor(0.8)
                            .lineLimit(1)
                    }
                    .frame(minWidth: 60, alignment: .trailing)

                    // Gesamtpunkte - jetzt kleinere Anzeige mit fester Breite
                    HStack(spacing: 4) {
                        Image(systemName: "sum")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                        Text("\(player.totalPoints)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .minimumScaleFactor(0.8)
                            .lineLimit(1)
                    }
                    .frame(minWidth: 60, alignment: .trailing)
                }

                VStack(alignment: .trailing, spacing: 4) {
                    // Verkaufspreis mit fester Breite
                    Text("€\(formatValue(player.price))")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(.green)
                        .minimumScaleFactor(0.8)
                        .lineLimit(1)
                        .frame(minWidth: 50, alignment: .trailing)

                    // Marktwert mit fester Breite
                    Text("MW: €\(formatValue(player.marketValue))")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .minimumScaleFactor(0.8)
                        .lineLimit(1)
                        .frame(minWidth: 50, alignment: .trailing)
                }
            }
            .padding(.vertical, 8)
        }
        #if os(iOS)
            .buttonStyle(PlainButtonStyle())
        #endif
        .sheet(isPresented: $showingPlayerDetail) {
            PlayerDetailView(player: convertMarketPlayerToTeamPlayer(player))
                .environmentObject(kickbaseManager)
                .environmentObject(ligainsiderService)
        }
    }
}

// MARK: - Sales Recommendation View
struct SalesRecommendationView: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @State private var selectedGoal: OptimizationGoal = .balancePositive
    @State private var recommendedSales: [SalesRecommendation] = []
    @State private var selectedSales: Set<String> = []

    enum OptimizationGoal: String, CaseIterable {
        case balancePositive = "Budget ins Plus"
        case maximizeProfit = "Maximaler Profit"
        case keepBestPlayers = "Beste Spieler behalten"
    }

    // Berechnung der Spieleranzahl nach Positionen (ohne ausgewählte Verkäufe)
    private var playerCountsAfterSales: TeamPlayerCounts {
        let remainingPlayers = kickbaseManager.teamPlayers.filter { !selectedSales.contains($0.id) }

        let goalkeepers = remainingPlayers.filter { $0.position == 1 }.count
        let defenders = remainingPlayers.filter { $0.position == 2 }.count
        let midfielders = remainingPlayers.filter { $0.position == 3 }.count
        let forwards = remainingPlayers.filter { $0.position == 4 }.count
        let total = remainingPlayers.count

        return TeamPlayerCounts(
            total: total,
            goalkeepers: goalkeepers,
            defenders: defenders,
            midfielders: midfielders,
            forwards: forwards
        )
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Header
                SalesRecommendationHeader(
                    currentBudget: kickbaseManager.userStats?.budget ?? 0,
                    recommendedSaleValue: recommendedSales.map { $0.expectedValue }.reduce(0, +),
                    selectedSaleValue: selectedSales.compactMap { id in
                        recommendedSales.first(where: { $0.player.id == id })?.expectedValue
                    }.reduce(0, +)
                )

                // Spieleranzahl-Übersicht
                VStack(spacing: 16) {
                    Text("Verbleibende Spieler nach Verkauf")
                        .font(.title2)
                        .fontWeight(.bold)

                    PlayerCountOverview(playerCounts: playerCountsAfterSales)
                }
                .padding()
                .background(Color.systemGray6Compat)
                .cornerRadius(12)

                // Optimierungsziel Auswahl
                VStack(spacing: 16) {
                    Text("Optimierungsziel")
                        .font(.title2)
                        .fontWeight(.bold)

                    Picker("Ziel wählen", selection: $selectedGoal) {
                        ForEach(OptimizationGoal.allCases, id: \.self) { goal in
                            Text(goal.rawValue).tag(goal)
                        }
                    }
                    .pickerStyle(.segmented)
                }
                .padding()
                .background(Color.systemGray6Compat)
                .cornerRadius(12)

                // Verkaufs-Empfehlungen
                SalesRecommendationSummary(
                    recommendations: recommendedSales, optimizationGoal: selectedGoal)

                // Detaillierte Empfehlungen
                VStack(alignment: .leading, spacing: 16) {
                    Text("Detaillierte Empfehlungen")
                        .font(.title2)
                        .fontWeight(.bold)

                    ForEach(recommendedSales) { recommendation in
                        SalesRecommendationRow(
                            recommendation: recommendation,
                            isSelected: selectedSales.contains(recommendation.player.id),
                            onToggle: { isSelected in
                                if isSelected {
                                    selectedSales.insert(recommendation.player.id)
                                } else {
                                    selectedSales.remove(recommendation.player.id)
                                }
                            }
                        )
                    }
                }
                .padding()
                .background(Color.systemGray6Compat)
                .cornerRadius(12)
            }
            .padding()
        }
        .onAppear {
            generateIntelligentRecommendations()
        }
        .onChange(of: selectedGoal) {
            generateIntelligentRecommendations()
        }
    }

    private func generateIntelligentRecommendations() {
        Task {
            let allPlayers = kickbaseManager.teamPlayers
            let currentBudget = kickbaseManager.userStats?.budget ?? 0

            var newRecommendations: [SalesRecommendation] = []

            // Analysiere alle Spieler und erstelle Empfehlungen basierend auf echten Kriterien
            for player in allPlayers {
                if let recommendation = await analyzePlayerForSale(
                    player: player,
                    allPlayers: allPlayers,
                    currentBudget: currentBudget,
                    optimizationGoal: selectedGoal
                ) {
                    newRecommendations.append(recommendation)
                }
            }

            // Sortiere nach Priorität und Impact - beste Verkaufskandidaten zuerst
            newRecommendations.sort { recommendation1, recommendation2 in
                let priority1Value = getPriorityValue(recommendation1.priority)
                let priority2Value = getPriorityValue(recommendation2.priority)
                let impact1Value = getImpactValue(recommendation1.impact)
                let impact2Value = getImpactValue(recommendation2.impact)

                // Erstelle einen kombinierten Score: Hohe Priorität (niedrige Zahl) + Niedriger Impact (niedrige Zahl) = besserer Kandidat
                let score1 = priority1Value + impact1Value
                let score2 = priority2Value + impact2Value

                if score1 != score2 {
                    return score1 < score2  // Niedrigerer Score = besserer Verkaufskandidat
                }

                // Bei gleichem Score: Sekundäre Sortierung nach Optimierungsziel
                switch selectedGoal {
                case .balancePositive:
                    return recommendation1.player.marketValue > recommendation2.player.marketValue
                case .maximizeProfit:
                    return recommendation1.expectedValue > recommendation2.expectedValue
                case .keepBestPlayers:
                    return recommendation1.player.averagePoints
                        < recommendation2.player.averagePoints
                }
            }

            // Update auf Main Thread
            await MainActor.run {
                self.recommendedSales = newRecommendations
            }
        }
    }

    private func analyzePlayerForSale(
        player: TeamPlayer,
        allPlayers: [TeamPlayer],
        currentBudget: Int,
        optimizationGoal: OptimizationGoal
    ) async -> SalesRecommendation? {

        var reasons: [String] = []
        var shouldSell = false
        var priority: SalesRecommendation.Priority = .low

        // 1. KRITISCHE KRITERIEN - Immer verkaufen

        // Verletzte Spieler (Status 1)
        if player.status == 1 {
            reasons.append("Verletzt")
            shouldSell = true
            priority = .high
        }

        // Gesperrte Spieler (Status 8)
        if player.status == 8 {
            reasons.append("Gesperrt")
            shouldSell = true
            priority = .high
        }

        // Spieler im Aufbautraining (Status 4)
        if player.status == 4 {
            reasons.append("Aufbautraining")
            shouldSell = true
            priority = .high
        }

        // 2. PERFORMANCE-ANALYSE & FIXTURE-ANALYSE parallel ausführen
        await withTaskGroup(of: Void.self) { group in
            // Performance-Analyse parallel starten
            group.addTask {
                await self.analyzePlayerPerformance(
                    player: player,
                    reasons: &reasons,
                    shouldSell: &shouldSell,
                    priority: &priority
                )
            }

            // Fixture-Analyse parallel starten
            group.addTask {
                await self.analyzeUpcomingFixtures(
                    player: player,
                    reasons: &reasons,
                    shouldSell: &shouldSell,
                    priority: &priority
                )
            }

            // Auf beide Tasks warten
            await group.waitForAll()
        }

        // 4. POSITIONSLIMITS PRÜFEN
        let positionAnalysis = analyzePositionRedundancy(player: player, allPlayers: allPlayers)
        if positionAnalysis.isRedundant {
            shouldSell = true
            if priority == .low {
                priority = positionAnalysis.isWeakestInPosition ? .medium : .low
            }
        }

        // 5. BUDGET-BASIERTE KRITERIEN (nur wenn Budget negativ)
        if currentBudget < 0 {
            let budgetPressure = abs(currentBudget)

            // Bei hohem Budgetdruck verkaufe teure Spieler
            if player.marketValue >= budgetPressure / 2 {
                reasons.append("Budget im Minus")
                shouldSell = true
                if priority == .low {
                    priority = .medium
                }
            }
        }

        // 6. OPTIMIERUNGSZIEL-SPEZIFISCHE KRITERIEN
        switch optimizationGoal {
        case .balancePositive:
            // Verkaufe Spieler mit schlechtem Preis-Leistungs-Verhältnis
            if isPlayerOverpriced(player: player, allPlayers: allPlayers) {
                reasons.append("Schlechtes Preis-Leistungs-Verhältnis")
                shouldSell = true
            }

        case .maximizeProfit:
            // Verkaufe Spieler mit positiver Marktwertentwicklung
            if player.tfhmvt > 0 && player.tfhmvt > player.marketValue / 10 {
                reasons.append("Hoher Marktwertgewinn")
                shouldSell = true
            }

        case .keepBestPlayers:
            // Verkaufe schwächste Spieler auf der Position
            if isPlayerWeakestInPosition(player: player, allPlayers: allPlayers)
                && positionAnalysis.isRedundant
            {
                reasons.append("Schwächster Spieler auf Position")
                shouldSell = true
            }
        }

        // 7. WEITERE PERFORMANCE-KRITERIEN (nur bei geringer Priorität)
        if priority == .low {
            let teamAveragePoints =
                allPlayers.map(\.averagePoints).reduce(0.0, +) / Double(allPlayers.count)

            if player.averagePoints < teamAveragePoints * 0.6 {
                reasons.append("Schwache Performance")
                shouldSell = true
            }

            // Fallender Marktwert
            if player.tfhmvt < 0 && abs(player.tfhmvt) > player.marketValue / 8 {
                reasons.append("Fallender Marktwert")
                shouldSell = true
            }
        }

        guard shouldSell else { return nil }

        // Aufstellungsimpact berechnen
        let lineupImpact = calculateLineupImpact(player: player, allPlayers: allPlayers)

        return SalesRecommendation(
            player: player,
            reason: reasons.joined(separator: " • "),
            priority: priority,
            expectedValue: player.marketValue,
            impact: lineupImpact
        )
    }

    // MARK: - Performance Analysis Helper
    private func analyzePlayerPerformance(
        player: TeamPlayer,
        reasons: inout [String],
        shouldSell: inout Bool,
        priority: inout SalesRecommendation.Priority
    ) async {
        guard let selectedLeague = kickbaseManager.selectedLeague else { return }

        // Lade die letzten 5 Spiele des Spielers
        if let recentPerformances = await kickbaseManager.loadPlayerRecentPerformanceWithTeamInfo(
            playerId: player.id,
            leagueId: selectedLeague.id
        ) {

            // Analysiere die letzten gespielten Spiele
            let playedGames = recentPerformances.filter { $0.hasPlayed }

            if playedGames.count >= 3 {
                let recentPoints = playedGames.map { $0.points }
                let recentAverage = Double(recentPoints.reduce(0, +)) / Double(recentPoints.count)

                // Vergleiche mit der Saison-Durchschnittsleistung
                if recentAverage < player.averagePoints * 0.6 {
                    reasons.append(
                        "Schwache Form (letzte \(playedGames.count) Spiele: \(String(format: "%.1f", recentAverage)) Pkt.)"
                    )
                    shouldSell = true
                    if priority == .low {
                        priority = .medium
                    }
                }

                // Prüfe auf konstant schlechte Leistung
                let goodGames = recentPoints.filter { Double($0) >= player.averagePoints * 0.8 }
                    .count
                if goodGames == 0 && playedGames.count >= 3 {
                    reasons.append("Keine guten Spiele in letzten \(playedGames.count) Partien")
                    shouldSell = true
                    if priority == .low {
                        priority = .medium
                    }
                }
            }

            // Analysiere Einsatzzeiten und Status
            let startingElevenGames = playedGames.filter { $0.wasStartingEleven }.count
            let substituteGames = playedGames.filter { $0.wasSubstitute }.count
            let notInSquadGames = recentPerformances.filter { $0.wasNotInSquad }.count

            // Spieler verliert Stammplatz
            if playedGames.count >= 3 && startingElevenGames == 0 && substituteGames > 0 {
                reasons.append("Nur noch Einwechselspieler (keine Startelf)")
                shouldSell = true
            }

            // Spieler fällt aus dem Kader
            if notInSquadGames >= 2 {
                reasons.append("Häufig nicht im Kader (\(notInSquadGames) Spiele)")
                shouldSell = true
                if priority == .low {
                    priority = .medium
                }
            }
        }
    }

    // MARK: - Upcoming Fixtures Analysis Helper
    private func analyzeUpcomingFixtures(
        player: TeamPlayer,
        reasons: inout [String],
        shouldSell: inout Bool,
        priority: inout SalesRecommendation.Priority
    ) async {
        guard let selectedLeague = kickbaseManager.selectedLeague else {
            print("⚠️ Fixture-Analyse: Keine Liga ausgewählt für \(player.fullName)")
            return
        }

        print("🔍 Fixture-Analyse startet für \(player.fullName) (Team: \(player.teamId))")

        do {
            // Lade alle Performance-Daten um zukünftige Spiele zu analysieren
            if let allPerformances = try await kickbaseManager.loadPlayerPerformanceWithTeamInfo(
                playerId: player.id,
                leagueId: selectedLeague.id
            ) {
                print(
                    "📊 \(allPerformances.count) Performance-Einträge geladen für \(player.fullName)"
                )

                let currentMatchDay = getCurrentMatchDay(allPerformances: allPerformances)
                print("🗓️ Aktueller Spieltag: \(currentMatchDay)")

                // Finde zukünftige Spiele (noch nicht gespielt)
                let upcomingMatches = allPerformances.filter {
                    !$0.hasPlayed && $0.matchDay >= currentMatchDay
                }
                .sorted { $0.matchDay < $1.matchDay }
                .prefix(3)  // Analysiere die nächsten 3 Spiele

                print(
                    "🎯 \(upcomingMatches.count) zukünftige Spiele gefunden für \(player.fullName)")

                if upcomingMatches.count >= 1 {
                    // Debug: Zeige kommende Spiele - korrigierte Logik mit player.teamId
                    for (index, match) in upcomingMatches.enumerated() {
                        // Verwende die neuen Methoden mit der korrekten playerTeamId aus dem Player-Objekt
                        let opponentTeamId = match.basePerformance.getOpponentTeamId(
                            playerTeamId: player.teamId)
                        let isAwayGame = !match.basePerformance.getIsHomeMatch(
                            playerTeamId: player.teamId)
                        print(
                            "   Spiel \(index + 1): Spieltag \(match.matchDay), Gegner: \(opponentTeamId), Auswärts: \(isAwayGame)"
                        )
                    }

                    let fixtureAnalysis = analyzeFixtureDifficulty(
                        matches: Array(upcomingMatches), playerTeam: player.teamId)
                    print("📈 Fixture-Analyse Ergebnis für \(player.fullName):")
                    print(
                        "   - Durchschnittliche Schwierigkeit: \(String(format: "%.2f", fixtureAnalysis.averageDifficulty))"
                    )
                    print("   - Top-Teams als Gegner: \(fixtureAnalysis.topTeamOpponents)")
                    print("   - Schwere Auswärtsspiele: \(fixtureAnalysis.difficultAwayGames)")

                    // Schwere Fixture-Liste
                    if fixtureAnalysis.averageDifficulty >= 0.7 {
                        let difficultyPercentage = Int(fixtureAnalysis.averageDifficulty * 100)
                        let reason =
                            "Schwere Gegner kommend (\(difficultyPercentage)% Schwierigkeit, \(upcomingMatches.count) Spiele)"
                        reasons.append(reason)
                        shouldSell = true
                        print("✅ Verkaufsgrund hinzugefügt: \(reason)")

                        // Besonders schwer -> höhere Priorität
                        if fixtureAnalysis.averageDifficulty >= 0.8 && priority != .high {
                            priority = .medium
                            print("⬆️ Priorität auf MEDIUM erhöht wegen sehr schwerer Fixtures")
                        }
                    }

                    // Viele Top-6-Teams als Gegner
                    if fixtureAnalysis.topTeamOpponents >= 2 {
                        let reason =
                            "Viele Top-Teams als Gegner (\(fixtureAnalysis.topTeamOpponents) von \(upcomingMatches.count))"
                        reasons.append(reason)
                        shouldSell = true
                        print("✅ Verkaufsgrund hinzugefügt: \(reason)")
                        if priority == .low {
                            priority = .medium
                            print("⬆️ Priorität auf MEDIUM erhöht wegen vieler Top-Teams")
                        }
                    }

                    // Auswärtsspiele-Schwere
                    if fixtureAnalysis.difficultAwayGames >= 2 {
                        let reason =
                            "Schwere Auswärtsspiele (\(fixtureAnalysis.difficultAwayGames) Spiele)"
                        reasons.append(reason)
                        shouldSell = true
                        print("✅ Verkaufsgrund hinzugefügt: \(reason)")
                    }

                    // Positive Indikatoren (gegen Verkauf)
                    if fixtureAnalysis.averageDifficulty <= 0.3 {
                        print("🟢 Sehr einfache kommende Spiele - Verkauf weniger empfehlenswert")
                        // Sehr einfache kommende Spiele - weniger verkaufsbereit
                        if priority == .low && reasons.count <= 2 {
                            print("⬇️ Verkaufsempfehlung reduziert wegen einfacher Fixtures")
                            // Entferne schwächere Verkaufsgründe wenn einfache Spiele kommen
                            return
                        }
                    }
                } else {
                    print("⚠️ Keine zukünftigen Spiele gefunden für \(player.fullName)")
                }
            } else {
                print("❌ Keine Performance-Daten geladen für \(player.fullName)")
            }
        } catch {
            print("❌ Fehler beim Laden der Fixture-Daten für \(player.fullName): \(error)")
        }
    }

    // MARK: - Fixture Difficulty Analysis
    private func analyzeFixtureDifficulty(matches: [EnhancedMatchPerformance], playerTeam: String)
        -> FixtureAnalysis
    {
        var totalDifficulty: Double = 0
        var topTeamOpponents = 0
        var difficultAwayGames = 0

        for match in matches {
            // Verwende die neuen Methoden mit der korrekten playerTeamId
            let opponentTeamId = match.basePerformance.getOpponentTeamId(playerTeamId: playerTeam)
            let isAwayGame = !match.basePerformance.getIsHomeMatch(playerTeamId: playerTeam)

            // Berechne Gegnerstärke basierend auf echter Platzierung aus den Team-Infos
            let opponentStrength = getTeamStrengthFromMatch(
                match: match, opponentTeamId: opponentTeamId)
            let difficultyScore = calculateMatchDifficulty(
                opponentStrength: opponentStrength,
                isAwayGame: isAwayGame
            )

            totalDifficulty += difficultyScore

            // Top-6-Teams zählen (Stärke >= 0.7)
            if opponentStrength >= 0.7 {
                topTeamOpponents += 1
            }

            // Schwere Auswärtsspiele
            if isAwayGame && opponentStrength >= 0.6 {
                difficultAwayGames += 1
            }
        }

        let averageDifficulty = matches.isEmpty ? 0.0 : totalDifficulty / Double(matches.count)

        return FixtureAnalysis(
            averageDifficulty: averageDifficulty,
            topTeamOpponents: topTeamOpponents,
            difficultAwayGames: difficultAwayGames,
            totalMatches: matches.count
        )
    }

    // MARK: - Team Strength from Match Data
    private func getTeamStrengthFromMatch(match: EnhancedMatchPerformance, opponentTeamId: String)
        -> Double
    {
        // Versuche Team-Info aus den geladenen Daten zu finden
        let opponentTeamInfo: TeamInfo?

        if match.team1Id == opponentTeamId {
            opponentTeamInfo = match.team1Info
        } else if match.team2Id == opponentTeamId {
            opponentTeamInfo = match.team2Info
        } else {
            opponentTeamInfo = nil
        }

        // Falls Team-Info verfügbar ist, berechne Stärke basierend auf Platzierung
        if let teamInfo = opponentTeamInfo {
            let strength = calculateTeamStrengthFromPlacement(teamInfo.placement)
            print(
                "🎯 Team \(teamInfo.name) (Platz \(teamInfo.placement)) hat Stärke \(String(format: "%.2f", strength))"
            )
            return strength
        } else {
            // Fallback: mittlere Stärke wenn keine Team-Info verfügbar
            print(
                "⚠️ Keine Team-Info für Team \(opponentTeamId) verfügbar, verwende Fallback-Stärke 0.5"
            )
            return 0.5
        }
    }
}

// MARK: - Sales Recommendation Data Models
struct SalesRecommendation: Identifiable {
    let id = UUID()
    let player: TeamPlayer
    let reason: String
    let priority: Priority
    let expectedValue: Int
    let impact: LineupImpact

    enum Priority: String, CaseIterable {
        case high = "Hoch"
        case medium = "Mittel"
        case low = "Niedrig"

        var color: Color {
            switch self {
            case .high: return .red
            case .medium: return .orange
            case .low: return .green
            }
        }
    }

    enum LineupImpact: String, CaseIterable {
        case minimal = "Minimal"
        case moderate = "Moderat"
        case significant = "Erheblich"

        var color: Color {
            switch self {
            case .minimal: return .green
            case .moderate: return .orange
            case .significant: return .red
            }
        }
    }
}

// MARK: - Sales Recommendation Header
struct SalesRecommendationHeader: View {
    let currentBudget: Int
    let recommendedSaleValue: Int
    let selectedSaleValue: Int

    private var budgetAfterRecommended: Int {
        return currentBudget + recommendedSaleValue
    }

    private var budgetAfterSelected: Int {
        return currentBudget + selectedSaleValue
    }

    var body: some View {
        VStack(spacing: 16) {
            // Aktuelle Budget-Situation
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Aktuelles Budget")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text("€\(formatValueWithSeparators(currentBudget))")
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundColor(currentBudget < 0 ? .red : .green)
                }

                Spacer()

                if currentBudget < 0 {
                    VStack(alignment: .trailing, spacing: 4) {
                        Text("Benötigt")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("€\(formatValueWithSeparators(abs(currentBudget)))")
                            .font(.headline)
                            .fontWeight(.bold)
                            .foregroundColor(.red)
                    }
                }
            }
            .padding()
            .background(currentBudget < 0 ? Color.red.opacity(0.1) : Color.green.opacity(0.1))
            .cornerRadius(12)

            // Empfohlene vs. Ausgewählte Verkäufe
            if recommendedSaleValue > 0 || selectedSaleValue > 0 {
                LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 12) {
                    // Empfohlene Verkäufe
                    VStack(spacing: 8) {
                        Text("Empfohlen")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        VStack(spacing: 4) {
                            Text("€\(formatValueWithSeparators(recommendedSaleValue))")
                                .font(.headline)
                                .fontWeight(.bold)
                                .foregroundColor(.blue)

                            Text("Budget: €\(formatValueWithSeparators(budgetAfterRecommended))")
                                .font(.caption)
                                .foregroundColor(budgetAfterRecommended >= 0 ? .green : .red)
                        }
                    }
                    .padding()
                    .background(Color.systemGray6Compat)
                    .cornerRadius(8)

                    // Ausgewählte Verkäufe
                    VStack(spacing: 8) {
                        Text("Ausgewählt")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        VStack(spacing: 4) {
                            Text("€\(formatValueWithSeparators(selectedSaleValue))")
                                .font(.headline)
                                .fontWeight(.bold)
                                .foregroundColor(.orange)

                            if selectedSaleValue > 0 {
                                Text("Budget: €\(formatValueWithSeparators(budgetAfterSelected))")
                                    .font(.caption)
                                    .foregroundColor(budgetAfterSelected >= 0 ? .green : .red)
                            } else {
                                Text("Keine Auswahl")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    .padding()
                    .background(Color.systemGray6Compat)
                    .cornerRadius(8)
                }
            }
        }
    }

    private func formatValueWithSeparators(_ value: Int) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.groupingSeparator = "."
        formatter.groupingSize = 3

        if let formattedString = formatter.string(from: NSNumber(value: value)) {
            return formattedString
        } else {
            return "\(value)"
        }
    }
}

// MARK: - Sales Recommendation Summary
struct SalesRecommendationSummary: View {
    let recommendations: [SalesRecommendation]
    let optimizationGoal: SalesRecommendationView.OptimizationGoal

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Empfehlungsübersicht")
                .font(.title2)
                .fontWeight(.bold)

            // Strategieerklärung
            VStack(alignment: .leading, spacing: 8) {
                Text("Strategie: \(optimizationGoal.rawValue)")
                    .font(.headline)
                    .foregroundColor(.blue)

                Text(getStrategyDescription(for: optimizationGoal))
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(Color.systemBackgroundCompat)
            .cornerRadius(8)

            // Prioritäten-Übersicht
            let priorityCounts = getPriorityCounts()
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: 12) {
                ForEach(SalesRecommendation.Priority.allCases, id: \.self) { priority in
                    VStack(spacing: 4) {
                        Image(systemName: getPriorityIcon(priority))
                            .font(.title2)
                            .foregroundColor(priority.color)

                        Text("\(priorityCounts[priority] ?? 0)")
                            .font(.title)
                            .fontWeight(.bold)

                        Text(priority.rawValue)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.systemGray6Compat)
                    .cornerRadius(8)
                }
            }
        }
        .padding()
        .background(Color.systemGray6Compat)
        .cornerRadius(12)
    }

    private func getStrategyDescription(for goal: SalesRecommendationView.OptimizationGoal)
        -> String
    {
        switch goal {
        case .balancePositive:
            return
                "Verkaufe Spieler um das Budget ins Plus zu bringen, dabei werden Spielerleistung und Positionsbesetzung berücksichtigt."
        case .maximizeProfit:
            return
                "Verkaufe Spieler mit dem höchsten Gewinn seit dem Kauf, um maximalen Profit zu erzielen."
        case .keepBestPlayers:
            return
                "Verkaufe schwächere Spieler zuerst, um die besten Leistungsträger im Team zu behalten."
        }
    }

    private func getPriorityCounts() -> [SalesRecommendation.Priority: Int] {
        var counts: [SalesRecommendation.Priority: Int] = [:]

        for priority in SalesRecommendation.Priority.allCases {
            counts[priority] = recommendations.filter { $0.priority == priority }.count
        }

        return counts
    }

    private func getPriorityIcon(_ priority: SalesRecommendation.Priority) -> String {
        switch priority {
        case .high: return "exclamationmark.triangle.fill"
        case .medium: return "minus.circle.fill"
        case .low: return "checkmark.circle.fill"
        }
    }
}

// MARK: - Sales Recommendation Row
struct SalesRecommendationRow: View {
    let recommendation: SalesRecommendation
    let isSelected: Bool
    let onToggle: (Bool) -> Void

    @State private var showingPlayerDetail = false
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @EnvironmentObject var ligainsiderService: LigainsiderService

    var body: some View {
        HStack(spacing: 12) {
            // Position Badge
            PositionBadge(position: recommendation.player.position)

            // Spieler-Info Bereich (klickbar für Details)
            Button(action: {
                showingPlayerDetail = true
            }) {
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 6) {
                        // Spieler Name mit Status
                        HStack(spacing: 4) {
                            Text(recommendation.player.fullName)
                                .font(.headline)
                                .fontWeight(.medium)

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

                            // Status-Icons
                            if recommendation.player.status == 2 {
                                Image(systemName: "pills.fill")
                                    .foregroundColor(.orange)
                                    .font(.caption)
                            }
                        }

                        // Team
                        Text(recommendation.player.fullTeamName)
                            .font(.caption)
                            .foregroundColor(.secondary)

                        // Verkaufsgrund
                        Text(recommendation.reason)
                            .font(.caption)
                            .foregroundColor(.blue)
                            .italic()
                    }

                    Spacer()

                    // Stats
                    VStack(alignment: .trailing, spacing: 4) {
                        // Durchschnittspunkte
                        HStack(spacing: 4) {
                            Image(systemName: "star.fill")
                                .font(.caption)
                                .foregroundColor(.orange)
                            Text(String(format: "%.0f", recommendation.player.averagePoints))
                                .font(.subheadline)
                                .fontWeight(.semibold)
                        }

                        // Marktwert
                        Text("€\(formatValue(recommendation.player.marketValue))")
                            .font(.headline)
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                    }

                    VStack(spacing: 8) {
                        // Priorität
                        HStack(spacing: 4) {
                            Circle()
                                .fill(recommendation.priority.color)
                                .frame(width: 8, height: 8)
                            Text(recommendation.priority.rawValue)
                                .font(.caption)
                                .foregroundColor(recommendation.priority.color)
                        }

                        // Aufstellungsimpact
                        HStack(spacing: 4) {
                            Image(systemName: getImpactIcon(recommendation.impact))
                                .font(.caption2)
                                .foregroundColor(recommendation.impact.color)
                            Text(recommendation.impact.rawValue)
                                .font(.caption2)
                                .foregroundColor(recommendation.impact.color)
                        }
                    }
                }
            }
            #if os(iOS)
                .buttonStyle(PlainButtonStyle())
            #endif

            // Toggle (separater Bereich)
            Toggle(
                isOn: Binding<Bool>(
                    get: { isSelected },
                    set: { newValue in
                        onToggle(newValue)
                    }
                )
            ) {
                Text("")
            }

            .frame(width: 50, height: 30)
        }
        .padding(.vertical, 8)
        .sheet(isPresented: $showingPlayerDetail) {
            PlayerDetailView(player: recommendation.player)
                .environmentObject(kickbaseManager)
                .environmentObject(ligainsiderService)
        }
    }

    private func getImpactIcon(_ impact: SalesRecommendation.LineupImpact) -> String {
        switch impact {
        case .minimal: return "checkmark.circle.fill"
        case .moderate: return "minus.circle.fill"
        case .significant: return "exclamationmark.triangle.fill"
        }
    }
}

// MARK: - Lineup Optimizer View
struct LineupOptimizerView: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @EnvironmentObject var ligainsiderService: LigainsiderService
    @State private var selectedOptimization: OptimizationType = .averagePoints
    @State private var lineupComparison: LineupComparison?
    @State private var showOptimalComparison = false
    @State private var isGeneratingComparison = false

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
                .pickerStyle(.segmented)

                // Button für optimale Aufstellung mit Marktspieler
                Button(action: generateOptimalLineupComparison) {
                    HStack {
                        Image(systemName: "sparkles")
                        Text("Mit Marktspieler")
                        Spacer()
                        if isGeneratingComparison {
                            ProgressView()
                                .scaleEffect(0.8)
                        }
                    }
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .padding(.vertical, 10)
                    .padding(.horizontal)
                    .background(Color.blue)
                    .cornerRadius(8)
                }
                .disabled(isGeneratingComparison || kickbaseManager.teamPlayers.isEmpty)
            }
            .padding()
            .background(Color.systemGray6Compat)

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
                            allPlayers: kickbaseManager.teamPlayers.filter {
                                $0.status != 1 && $0.status != 4 && $0.status != 8
                            },
                            startingLineup: optimalResult.lineup,
                            optimizationType: selectedOptimization
                        )
                    }
                    .padding()
                }
            }
        }
        .refreshable {
            if let league = kickbaseManager.selectedLeague {
                await kickbaseManager.loadTeamPlayers(for: league)
            }
        }
        .sheet(item: $lineupComparison) { comparison in
            LineupComparisonView(comparison: comparison)
                .environmentObject(kickbaseManager)
                .environmentObject(ligainsiderService)
        }
    }

    private func generateOptimalLineupComparison() {
        Task {
            isGeneratingComparison = true
            defer { isGeneratingComparison = false }

            do {
                guard let league = kickbaseManager.selectedLeague else {
                    return
                }

                let recommendationService = PlayerRecommendationService(
                    kickbaseManager: kickbaseManager
                )

                // Finde die beste Formation basierend auf verfügbaren Spielern
                let bestFormation = findBestFormation()

                let comparison = await recommendationService.generateOptimalLineupComparison(
                    for: league,
                    teamPlayers: kickbaseManager.teamPlayers,
                    marketPlayers: kickbaseManager.marketPlayers,
                    formation: bestFormation
                )

                await MainActor.run {
                    self.lineupComparison = comparison
                }
            }
        }
    }

    private func findBestFormation() -> [Int] {
        let availablePlayers = kickbaseManager.teamPlayers.filter {
            $0.status != 1 && $0.status != 4 && $0.status != 8
        }

        let goalkeepers = availablePlayers.filter { $0.position == 1 }
        let defenders = availablePlayers.filter { $0.position == 2 }
        let midfielders = availablePlayers.filter { $0.position == 3 }
        let forwards = availablePlayers.filter { $0.position == 4 }

        // Muss mindestens einen Torwart haben
        guard !goalkeepers.isEmpty else {
            return [1, 4, 4, 2]  // Fallback
        }

        // Finde die beste mögliche Formation basierend auf verfügbaren Spielern
        var bestFormation: [Int]?
        var bestScore: Double = 0

        for formation in LineupOptimizerView.Formation.allCases {
            let positions = formation.positions

            // Prüfe ob genügend Spieler für diese Formation vorhanden sind
            if defenders.count >= positions.defenders && midfielders.count >= positions.midfielders
                && forwards.count >= positions.forwards
            {
                // Berechne Score für diese Formation
                let score = Double(defenders.count + midfielders.count + forwards.count)

                if score > bestScore {
                    bestScore = score
                    bestFormation = formationToArray(formation)
                }
            }
        }

        return bestFormation ?? [1, 4, 4, 2]  // Fallback
    }

    private func formationToArray(_ formation: LineupOptimizerView.Formation) -> [Int] {
        let positions = formation.positions
        return [1, positions.defenders, positions.midfielders, positions.forwards]
    }

    private func getBestPossibleLineup() -> (lineup: OptimalLineup, formation: Formation) {
        let availablePlayers = kickbaseManager.teamPlayers.filter {
            $0.status != 1 && $0.status != 4 && $0.status != 8
        }  // Ausschluss verletzter Spieler und Spieler im Aufbautraining

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
            if defenders.count >= positions.defenders && midfielders.count >= positions.midfielders
                && forwards.count >= positions.forwards
            {

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
        let maxDefenders = min(defenders.count, 5)  // Max 5 Verteidiger
        let maxMidfielders = min(midfielders.count, 6)  // Max 6 Mittelfeldspieler
        let maxForwards = min(forwards.count, 4)  // Max 4 Stürmer

        // Finde eine passende Formation
        let customFormation: Formation
        if maxDefenders >= 4 && maxMidfielders >= 4 && maxForwards >= 2 {
            customFormation = .formation442
        } else if maxDefenders >= 4 && maxMidfielders >= 3 && maxForwards >= 3 {
            customFormation = .formation433
        } else if maxDefenders >= 3 && maxMidfielders >= 4 && maxForwards >= 3 {
            customFormation = .formation343
        } else if maxDefenders >= 5 && maxMidfielders >= 3 && maxForwards >= 2 {
            customFormation = .formation532
        } else if maxDefenders >= 4 && maxMidfielders >= 5 && maxForwards >= 1 {
            customFormation = .formation451
        } else {
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

    private func createFallbackLineup(from players: [TeamPlayer]) -> (
        lineup: OptimalLineup, formation: Formation
    ) {
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
        .background(Color.systemGray6Compat)
        .cornerRadius(12)
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
                    gradient: Gradient(colors: [Color.green.opacity(0.3), Color.green.opacity(0.1)]
                    ),
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
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @EnvironmentObject var ligainsiderService: LigainsiderService

    // Plattformspezifische Größen
    private var cardSize: (width: CGFloat, height: CGFloat) {
        #if os(macOS)
            return (width: 90.0, height: 110.0)  // 30% größer auf macOS
        #elseif os(iOS)
            if UIDevice.current.userInterfaceIdiom == UIUserInterfaceIdiom.pad {
                return (width: 80.0, height: 100.0)  // 15% größer auf iPad
            } else {
                return (width: 60.0, height: 80.0)  // 20% größer auf iPhone
            }
        #else
            return (width: 60.0, height: 80.0)
        #endif
    }

    private struct CardFontSizes {
        let firstName: Int
        let lastName: Int
        let avgPoints: Int
        let totalPoints: Int
        let status: Int
    }

    private var fontSizes: CardFontSizes {
        #if os(macOS)
            return CardFontSizes(
                firstName: 14, lastName: 16, avgPoints: 14, totalPoints: 11, status: 12)  // Größere Schriften auf macOS
        #elseif os(iOS)
            if UIDevice.current.userInterfaceIdiom == UIUserInterfaceIdiom.pad {
                return CardFontSizes(
                    firstName: 13, lastName: 15, avgPoints: 13, totalPoints: 10, status: 11)
            } else {
                return CardFontSizes(
                    firstName: 11, lastName: 13, avgPoints: 12, totalPoints: 9, status: 10)
            }
        #else
            return CardFontSizes(
                firstName: 11, lastName: 13, avgPoints: 12, totalPoints: 9, status: 10)
        #endif
    }

    var body: some View {
        Button(action: {
            showingPlayerDetail = true
        }) {
            VStack(spacing: 5) {  // Player name - größer und lesbarer
                VStack(spacing: 2) {
                    Text(player.firstName)
                        .font(.system(size: CGFloat(fontSizes.firstName), weight: .medium))
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                    Text(player.lastName)
                        .font(.system(size: CGFloat(fontSizes.lastName), weight: .bold))
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                }
                .foregroundColor(.primary)

                // Points - größer und prominenter
                VStack(spacing: 1) {
                    Text(String(format: "%.0f", player.averagePoints))
                        .font(.system(size: CGFloat(fontSizes.avgPoints), weight: .bold))
                        .foregroundColor(.orange)
                    Text("\(player.totalPoints)")
                        .font(.system(size: CGFloat(fontSizes.totalPoints)))
                        .foregroundColor(.secondary)
                }

                // Status indicator
                HStack(spacing: 2) {
                    if player.status == 2 {
                        Image(systemName: "pills.fill")
                            .foregroundColor(.orange)
                            .font(.system(size: CGFloat(fontSizes.status)))
                    }

                    // Ligainsider Icon (wenn verfügbar)
                    let status = ligainsiderService.getPlayerStatus(
                        firstName: player.firstName, lastName: player.lastName)
                    if status != .out {
                        Image(systemName: ligainsiderService.getIcon(for: status))
                            .foregroundColor(ligainsiderService.getColor(for: status))
                            .font(.system(size: CGFloat(fontSizes.status)))
                    }
                }
            }
            .frame(
                width: cardSize.width,
                height: cardSize.height
            )
            .background(Color.systemBackgroundCompat)
            .cornerRadius(10)  // Größerer Radius
            .shadow(radius: 1.0)  // Stärkerer Schatten
        }
        #if os(iOS)
            .buttonStyle(PlainButtonStyle())
        #endif
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
        var grouped: [Int: [TeamPlayer]] = [:]
        for player in reservePlayers {
            var list = grouped[player.position] ?? []
            list.append(player)
            grouped[player.position] = list
        }

        return [
            ("Torhüter", grouped[1] ?? []),
            ("Abwehr", grouped[2] ?? []),
            ("Mittelfeld", grouped[3] ?? []),
            ("Sturm", grouped[4] ?? []),
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
                ReserveBenchStatsView(
                    reservePlayers: reservePlayers, optimizationType: optimizationType)

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

                        LazyVGrid(
                            columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 8
                        ) {
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
        .background(Color.systemGray6Compat)
        .cornerRadius(12)
    }
}

// MARK: - Reserve Bench Stats
struct ReserveBenchStatsView: View {
    let reservePlayers: [TeamPlayer]
    let optimizationType: LineupOptimizerView.OptimizationType

    private var benchStats:
        (totalPoints: Int, averagePoints: Double, bestPlayer: TeamPlayer?, totalValue: Int)
    {
        let totalPoints = reservePlayers.reduce(0) { $0 + $1.totalPoints }
        let averagePoints =
            reservePlayers.isEmpty ? 0.0 : Double(totalPoints) / Double(reservePlayers.count)

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
        .background(Color.systemBackgroundCompat)
        .cornerRadius(8)
    }
}

// MARK: - Bench Stat Card
struct BenchStatCard: View {
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
                .lineLimit(2)
        }
        .frame(height: 100)
        .frame(maxWidth: .infinity)
        .background(Color.systemBackgroundCompat)
        .cornerRadius(8)
    }
}

// MARK: - Reserve Player Row
struct ReservePlayerRow: View {
    let player: TeamPlayer
    let showPosition: Bool
    @State private var showingPlayerDetail = false

    @EnvironmentObject var kickbaseManager: KickbaseManager
    @EnvironmentObject var ligainsiderService: LigainsiderService

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

                        // Ligainsider Status Icon
                        let status = ligainsiderService.getPlayerStatus(
                            firstName: player.firstName, lastName: player.lastName)
                        if status != .out {
                            Image(systemName: ligainsiderService.getIcon(for: status))
                                .foregroundColor(
                                    ligainsiderService.getColor(for: status)
                                )
                                .font(.system(size: 8))
                        }

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
            .background(Color.systemBackgroundCompat)
            .cornerRadius(6)
            .shadow(radius: 0.5)
        }
        #if os(iOS)
            .buttonStyle(PlainButtonStyle())
        #endif
        .sheet(isPresented: $showingPlayerDetail) {
            PlayerDetailView(player: player)
                .environmentObject(kickbaseManager)
                .environmentObject(ligainsiderService)
        }
    }

    private func positionAbbreviation(_ position: Int) -> String {
        switch position {
        case 1: return "TW"
        case 2: return "ABW"
        case 3: return "MF"
        case 4: return "ST"
        default: return "?"
        }
    }

    private func positionColor(_ position: Int) -> Color {
        switch position {
        case 1: return .yellow
        case 2: return .green
        case 3: return .blue
        case 4: return .red
        default: return .gray
        }
    }
}

// MARK: - Player Count Overview
struct PlayerCountOverview: View {
    let playerCounts: TeamPlayerCounts

    private func getPositionColor(position: String, count: Int) -> Color {
        let minRequired: Int
        switch position {
        case "TW": minRequired = 1  // Mindestens 1 Torwart
        case "ABW": minRequired = 3  // Mindestens 3 Verteidiger
        case "MF": minRequired = 2  // Mindestens 2 Mittelfeldspieler
        case "ST": minRequired = 1  // Mindestens 1 Stürmer
        default: minRequired = 1
        }

        return count >= minRequired ? .green : .red
    }

    var body: some View {
        HStack {
            // Gesamtanzahl
            PlayerPositionCountView(
                position: "Gesamt",
                count: playerCounts.total,
                color: playerCounts.total >= 11 ? .green : .red
            )

            Spacer()

            // Torhüter
            PlayerPositionCountView(
                position: "TW",
                count: playerCounts.goalkeepers,
                color: getPositionColor(position: "TW", count: playerCounts.goalkeepers)
            )

            Spacer()

            // Verteidiger
            PlayerPositionCountView(
                position: "ABW",
                count: playerCounts.defenders,
                color: getPositionColor(position: "ABW", count: playerCounts.defenders)
            )

            Spacer()

            // Mittelfeldspieler
            PlayerPositionCountView(
                position: "MF",
                count: playerCounts.midfielders,
                color: getPositionColor(position: "MF", count: playerCounts.midfielders)
            )

            Spacer()

            // Stürmer
            PlayerPositionCountView(
                position: "ST",
                count: playerCounts.forwards,
                color: getPositionColor(position: "ST", count: playerCounts.forwards)
            )
        }
        .font(.headline)
        .padding(.horizontal)
        .padding(.vertical, 10)
        .background(Color.systemGray6Compat)
        .cornerRadius(12)
    }
}

// MARK: - Spieleranzahl nach Position
struct PlayerPositionCountView: View {
    let position: String
    let count: Int
    let color: Color

    var body: some View {
        VStack(spacing: 4) {
            Text(position)
                .font(.caption)
                .foregroundColor(.secondary)

            Text("\(count)")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(color)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Team Budget Header für MainDashboardView
struct TeamBudgetHeaderMain: View {
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
                Text("€\(formatValueWithSeparators(currentBudget))")
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(currentBudget < 0 ? .red : .green)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 4) {
                Text("Budget + Verkäufe")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text("€\(formatValueWithSeparators(totalBudget))")
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(totalBudget < 0 ? .red : (saleValue > 0 ? .green : .primary))
            }
        }
        .padding()
        .background(Color.systemGray6Compat)
        .cornerRadius(12)
        .padding(.horizontal)
    }

    private func formatValueWithSeparators(_ value: Int) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.groupingSeparator = "."
        formatter.groupingSize = 3

        if let formattedString = formatter.string(from: NSNumber(value: value)) {
            return formattedString
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
            let averageTeamPoints =
                kickbaseManager.teamPlayers.isEmpty
                ? 0.0 : Double(totalTeamPoints) / Double(kickbaseManager.teamPlayers.count)

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
        .background(Color.systemGray6Compat)
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
                StatCard(
                    title: "Gesamtpunkte", value: "\(stats.points)", icon: "star.fill",
                    color: .orange)
                StatCard(
                    title: "Platzierung", value: "#\(stats.placement)", icon: "trophy.fill",
                    color: .yellow)
                StatCard(
                    title: "Siege", value: "\(stats.won)", icon: "checkmark.circle.fill",
                    color: .green)
                StatCard(
                    title: "Niederlagen", value: "\(stats.lost)", icon: "xmark.circle.fill",
                    color: .red)
            }
        }
        .padding()
        .background(Color.systemGray6Compat)
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
                .lineLimit(2)
        }
        .frame(height: 100)
        .frame(maxWidth: .infinity)
        .background(Color.systemBackgroundCompat)
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

            InfoRow(label: "Liga", value: league.name)
            InfoRow(label: "Saison", value: league.season)
            InfoRow(label: "Spieltag", value: "\(league.matchDay)")
            InfoRow(label: "Admin", value: league.adminName)
            InfoRow(label: "Ersteller", value: league.creatorName)
        }
        .padding()
        .background(Color.systemGray6Compat)
        .cornerRadius(12)
    }
}

// MARK: - Helper Views
struct InfoRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label + ":")
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .fontWeight(.medium)
        }
    }
}

// Konvertiert MarketPlayer zu TeamPlayer für die PlayerDetailView
private func convertMarketPlayerToTeamPlayer(_ marketPlayer: MarketPlayer) -> TeamPlayer {
    return Player(
        id: marketPlayer.id,
        firstName: marketPlayer.firstName,
        lastName: marketPlayer.lastName,
        profileBigUrl: marketPlayer.profileBigUrl,
        teamName: marketPlayer.teamName,
        teamId: marketPlayer.teamId,
        position: marketPlayer.position,
        number: marketPlayer.number,
        averagePoints: marketPlayer.averagePoints,
        totalPoints: marketPlayer.totalPoints,
        marketValue: marketPlayer.marketValue,
        marketValueTrend: marketPlayer.marketValueTrend,
        tfhmvt: 0,  // MarketPlayer hat kein tfhmvt-Feld
        prlo: marketPlayer.prlo ?? 0,
        stl: marketPlayer.stl,
        status: marketPlayer.status,
        userOwnsPlayer: false  // MarketPlayer gehört nicht dem User
    )
}

// MARK: - All Players Row für Sales View
struct AllPlayersRow: View {
    let player: TeamPlayer
    let isSelected: Bool
    let isRecommended: Bool
    let onToggle: (Bool) -> Void
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @EnvironmentObject var ligainsiderService: LigainsiderService
    @State private var showingPlayerDetail = false

    var body: some View {
        HStack(spacing: 12) {
            // Position Badge
            PositionBadge(position: player.position)

            // Spieler-Info Bereich (klickbar für Details)
            Button(action: {
                showingPlayerDetail = true
            }) {
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        // Name mit Status-Icons und Empfehlungsindikator
                        HStack(spacing: 4) {
                            Text(player.fullName)
                                .font(.headline)
                                .fontWeight(.medium)

                            // Ligainsider Status Icon
                            let status = ligainsiderService.getPlayerStatus(
                                firstName: player.firstName, lastName: player.lastName)
                            if status != .out {
                                Image(systemName: ligainsiderService.getIcon(for: status))
                                    .foregroundColor(
                                        ligainsiderService.getColor(for: status)
                                    )
                                    .font(.caption)
                            }

                            // Empfehlungsindikator
                            if isRecommended {
                                Image(systemName: "star.fill")
                                    .foregroundColor(.orange)
                                    .font(.caption)
                            }

                            // Status-Icons
                            if player.status == 1 {
                                Image(systemName: "cross.circle.fill")
                                    .foregroundColor(.red)
                                    .font(.caption)
                            } else if player.status == 2 {
                                Image(systemName: "pills.fill")
                                    .foregroundColor(.orange)
                                    .font(.caption)
                            } else if player.status == 4 {
                                Image(systemName: "dumbbell.fill")
                                    .foregroundColor(.blue)
                                    .font(.caption)
                            } else if player.status == 8 {
                                Image(systemName: "rectangle.fill")
                                    .foregroundColor(.red)
                                    .font(.caption)
                            }
                        }

                        // Team
                        Text(player.fullTeamName)
                            .font(.caption)
                            .foregroundColor(.secondary)

                        // Verkaufsgrund falls empfohlen
                        if isRecommended {
                            Text("Verkaufsempfehlung")
                                .font(.caption)
                                .foregroundColor(.blue)
                                .italic()
                        }
                    }

                    Spacer()

                    // Stats
                    VStack(alignment: .trailing, spacing: 4) {
                        // Durchschnittspunkte
                        HStack(spacing: 4) {
                            Image(systemName: "star.fill")
                                .font(.caption)
                                .foregroundColor(.orange)
                            Text(String(format: "%.0f", player.averagePoints))
                                .font(.subheadline)
                                .fontWeight(.semibold)
                        }

                        // Marktwert
                        Text("€\(formatValue(player.marketValue))")
                            .font(.headline)
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                    }
                }
            }
            #if os(iOS)
                .buttonStyle(PlainButtonStyle())
            #endif

            // Toggle für Verkauf (separater Bereich)
            Toggle(
                isOn: Binding<Bool>(
                    get: { isSelected },
                    set: { newValue in
                        onToggle(newValue)
                    }
                )
            ) {
                Text("")
            }

            .frame(width: 50, height: 30)
        }
        .padding(.vertical, 8)
        .background(isRecommended ? Color.orange.opacity(0.1) : Color.clear)
        .cornerRadius(8)
        .sheet(isPresented: $showingPlayerDetail) {
            PlayerDetailView(player: player)
                .environmentObject(kickbaseManager)
                .environmentObject(ligainsiderService)
        }
    }
}

// MARK: - Helper Functions
private func formatValue(_ value: Int) -> String {
    if value >= 1_000_000 {
        return String(format: "%.1fM", Double(value) / 1_000_000)
    } else if value >= 1000 {
        let kValue = Double(value) / 1000
        // Zeige Dezimalstelle wenn unter 10k, sonst ganz Zahlen
        if kValue < 10 {
            return String(format: "%.1fk", kValue)
        } else {
            return String(format: "%.0fk", kValue)
        }
    } else {
        return "\(value)"
    }
}

private func getPriorityValue(_ priority: SalesRecommendation.Priority) -> Int {
    switch priority {
    case .high: return 1
    case .medium: return 2
    case .low: return 3
    }
}

private func getImpactValue(_ impact: SalesRecommendation.LineupImpact) -> Int {
    switch impact {
    case .minimal: return 1
    case .moderate: return 2
    case .significant: return 3
    }
}

private func analyzePositionRedundancy(player: TeamPlayer, allPlayers: [TeamPlayer]) -> (
    isRedundant: Bool, isWeakestInPosition: Bool
) {
    let playersInSamePosition = allPlayers.filter { $0.position == player.position }

    // Mindestanzahl pro Position
    let minRequired: Int
    switch player.position {
    case 1: minRequired = 1  // Torwart
    case 2: minRequired = 3  // Verteidiger
    case 3: minRequired = 2  // Mittelfeld
    case 4: minRequired = 1  // Sturm
    default: minRequired = 1
    }

    let isRedundant = playersInSamePosition.count > minRequired

    // Prüfe ob der Spieler der schwächste auf seiner Position ist
    let sortedByPerformance = playersInSamePosition.sorted { $0.averagePoints > $1.averagePoints }
    let isWeakestInPosition = sortedByPerformance.last?.id == player.id

    return (isRedundant, isWeakestInPosition)
}

private func isPlayerOverpriced(player: TeamPlayer, allPlayers: [TeamPlayer]) -> Bool {
    let teamAveragePoints =
        allPlayers.map(\.averagePoints).reduce(0.0, +) / Double(allPlayers.count)
    let teamAverageValue =
        Double(allPlayers.map(\.marketValue).reduce(0, +)) / Double(allPlayers.count)

    // Spieler ist überbewertet wenn sein Wert/Punkte-Verhältnis deutlich schlechter ist als der Teamdurchschnitt
    let playerValuePerPoint = Double(player.marketValue) / max(player.averagePoints, 1.0)
    let teamValuePerPoint = teamAverageValue / max(teamAveragePoints, 1.0)

    return playerValuePerPoint > teamValuePerPoint * 1.3  // 30% schlechter als Durchschnitt
}

private func isPlayerWeakestInPosition(player: TeamPlayer, allPlayers: [TeamPlayer]) -> Bool {
    let playersInSamePosition = allPlayers.filter { $0.position == player.position }
    let sortedByPerformance = playersInSamePosition.sorted { $0.averagePoints > $1.averagePoints }

    return sortedByPerformance.last?.id == player.id
}

private func calculateLineupImpact(player: TeamPlayer, allPlayers: [TeamPlayer])
    -> SalesRecommendation.LineupImpact
{
    let positionAnalysis = analyzePositionRedundancy(player: player, allPlayers: allPlayers)

    // Hoher Impact wenn Position nicht redundant ist
    if !positionAnalysis.isRedundant {
        return .significant
    }

    // Mittlerer Impact wenn Spieler überdurchschnittlich ist
    let teamAveragePoints =
        allPlayers.map(\.averagePoints).reduce(0.0, +) / Double(allPlayers.count)
    if player.averagePoints > teamAveragePoints * 1.1 {
        return .moderate
    }

    // Minimaler Impact bei schwachen, redundanten Spielern
    return .minimal
}

// MARK: - Team Strength Calculation (Dynamic based on actual placements)
private func getTeamStrength(teamId: String) -> Double {
    // Fallback: Verwende mittlere Stärke, da wir hier keinen Zugriff auf den Cache haben
    return 0.5
}

// Verbesserte Version: Team-Stärke basierend auf Platzierung berechnen
private func calculateTeamStrengthFromPlacement(_ placement: Int, totalTeams: Int = 18) -> Double {
    // Konvertiere Platzierung in Stärke-Wert (1.0 = bestes Team, 0.0 = schlechtestes Team)
    // Platz 1 = Stärke 1.0, Platz 18 = Stärke ~0.06
    let normalizedPlacement = Double(placement - 1) / Double(totalTeams - 1)
    let strength = 1.0 - normalizedPlacement

    // Minimum-Stärke von 0.1 für das schlechteste Team
    return max(0.1, strength)
}

// MARK: - Match Difficulty Calculation
private func calculateMatchDifficulty(opponentStrength: Double, isAwayGame: Bool) -> Double {
    var difficulty = opponentStrength

    // Auswärtsspiele sind schwieriger
    if isAwayGame {
        difficulty *= 1.2  // 20% schwieriger
    }

    // Begrenze auf maximale Schwierigkeit von 1.0
    return min(1.0, difficulty)
}

// Anhand von isCurrent den aktuellen Spieltag laden.
private func getCurrentMatchDay(allPerformances: [EnhancedMatchPerformance]) -> Int {

    if let currentMatch = allPerformances.first(where: { $0.isCurrent }) {
        return currentMatch.matchDay
    }

    return 1  // Fallback-Wert
}
