import SwiftUI

struct PlayerDetailView: View {
    let player: TeamPlayer
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var kickbaseManager: KickbaseManager
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    // Hero Header mit Spielerfoto und allen Grundinformationen
                    PlayerHeroHeader(player: player)
                    
                    // Punktzahl-Performance
                    PlayerPointsSection(player: player)
                    
                    // Marktwert und Finanzen
                    PlayerMarketValueSection(player: player)
                    
                    // Spiele und Gegner (neu)
                    PlayerMatchesSection(player: player)
                    
                    // Marktwertentwicklung der letzten 3 Tage
                    PlayerMarketTrendSection(player: player)
                }
                .padding()
            }
            .background(
                LinearGradient(
                    gradient: Gradient(colors: [
                        positionColor(player.position).opacity(0.1),
                        Color(.systemBackground)
                    ]),
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.secondary)
                            .font(.title2)
                    }
                }
            }
        }
    }
}

// MARK: - Hero Header (erweitert mit allen Grunddaten)
struct PlayerHeroHeader: View {
    let player: TeamPlayer
    
    var body: some View {
        VStack(spacing: 20) {
            // Großes Profilbild mit Position Badge
            ZStack(alignment: .bottomTrailing) {
                AsyncImage(url: URL(string: player.profileBigUrl)) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } placeholder: {
                    ZStack {
                        Circle()
                            .fill(positionColor(player.position).opacity(0.3))
                        
                        Image(systemName: "person.fill")
                            .font(.system(size: 50))
                            .foregroundColor(positionColor(player.position))
                    }
                }
                .frame(width: 120, height: 120)
                .clipShape(Circle())
                .overlay(
                    Circle()
                        .stroke(positionColor(player.position), lineWidth: 3)
                )
                
                // Position Badge
                Text(positionAbbreviation(player.position))
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .frame(width: 30, height: 30)
                    .background(positionColor(player.position))
                    .clipShape(Circle())
                    .overlay(
                        Circle()
                            .stroke(Color.white, lineWidth: 2)
                    )
                    .offset(x: 5, y: 5)
            }
            
            // Name und grundlegende Informationen
            VStack(spacing: 12) {
                // Vor- und Nachname
                VStack(spacing: 4) {
                    Text(player.firstName)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Text(player.lastName)
                        .font(.title)
                        .fontWeight(.bold)
                }
                
                // Team, Position und Nummer
                HStack(spacing: 8) {
                    Text(player.fullTeamName)
                        .font(.subheadline)
                        .foregroundColor(.blue)
                        .fontWeight(.medium)
                    
                    Text("•")
                        .foregroundColor(.secondary)
                    
                    Text(player.positionName)
                        .font(.subheadline)
                        .foregroundColor(positionColor(player.position))
                        .fontWeight(.semibold)
                    
                    Text("•")
                        .foregroundColor(.secondary)
                    
                    Text("#\(player.number)")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .fontWeight(.medium)
                }
                
                // Fit-Status Badge (für alle Status-Zustände)
                HStack(spacing: 6) {
                    Image(systemName: getStatusIcon(player.status))
                        .foregroundColor(getStatusColor(player.status))
                    Text("\(getPlayerStatusText(player.status))")
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(getStatusColor(player.status))
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(getStatusColor(player.status).opacity(0.15))
                .cornerRadius(16)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.1), radius: 8, x: 0, y: 4)
    }
    
    // Helper-Funktionen für Status
    private func getStatusIcon(_ status: Int) -> String {
        switch status {
        case 0: return "checkmark.circle.fill"
        case 1: return "cross.circle.fill"
        case 2: return "pills.fill"
        case 3: return "exclamationmark.triangle.fill"
        case 4: return "dumbbell.fill"
        case 8: return "rectangle.fill"  // Rote Karte Symbol
        default: return "questionmark.circle.fill"
        }
    }
    
    private func getStatusColor(_ status: Int) -> Color {
        switch status {
        case 0: return .green
        case 1: return .red
        case 2: return .orange
        case 3: return .red
        case 4: return .blue
        case 8: return .red  // Rote Farbe für Sperre
        default: return .gray
        }
    }
}

// MARK: - Punktzahl-Sektion
struct PlayerPointsSection: View {
    let player: TeamPlayer
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Punktzahl-Performance")
                .font(.headline)
                .fontWeight(.bold)
            
            HStack(spacing: 16) {
                // Durchschnittspunkte
                VStack(spacing: 12) {
                    Image(systemName: "star.fill")
                        .font(.title)
                        .foregroundColor(.orange)
                    
                    VStack(spacing: 4) {
                        Text("\(player.averagePoints, specifier: "%.0f")")
                            .font(.title)
                            .fontWeight(.bold)
                            .foregroundColor(.orange)
                        
                        Text("Ø Punkte")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .textCase(.uppercase)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.orange.opacity(0.1))
                .cornerRadius(12)
                
                // Gesamtpunkte
                VStack(spacing: 12) {
                    Image(systemName: "sum")
                        .font(.title)
                        .foregroundColor(.blue)
                    
                    VStack(spacing: 4) {
                        Text("\(player.totalPoints)")
                            .font(.title)
                            .fontWeight(.bold)
                            .foregroundColor(.blue)
                        
                        Text("Gesamtpunkte")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .textCase(.uppercase)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.blue.opacity(0.1))
                .cornerRadius(12)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 5, x: 0, y: 2)
    }
}

// MARK: - Marktwert-Sektion (ohne 24h Trend)
struct PlayerMarketValueSection: View {
    let player: TeamPlayer
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @State private var actualProfit: Int? = nil
    @State private var isLoadingProfit = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Marktwert & Finanzen")
                .font(.headline)
                .fontWeight(.bold)
            
            VStack(spacing: 12) {
                // Aktueller Marktwert
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Aktueller Marktwert")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        
                        Text(formatCurrency(player.marketValue))
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                    }
                    
                    Spacer()
                }
                
                // Gewinn/Verlust und letzte Änderung (nur wenn Spieler im Besitz)
                if player.userOwnsPlayer {
                    Divider()
                    
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            HStack {
                                Text("Gewinn/Verlust")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                                
                                if isLoadingProfit {
                                    ProgressView()
                                        .scaleEffect(0.6)
                                }
                            }
                            
                            let profitValue = actualProfit ?? player.prlo
                            Text(formatCurrency(profitValue))
                                .font(.headline)
                                .fontWeight(.bold)
                                .foregroundColor(profitValue >= 0 ? .green : .red)
                        }
                        
                        Spacer()
                        
                        if player.tfhmvt != 0 {
                            VStack(alignment: .trailing, spacing: 4) {
                                Text("Letzte Änderung")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                
                                HStack(spacing: 4) {
                                    Image(systemName: player.tfhmvt >= 0 ? "arrow.up" : "arrow.down")
                                        .foregroundColor(player.tfhmvt >= 0 ? .green : .red)
                                    
                                    Text(formatCurrency(abs(player.tfhmvt)))
                                        .font(.subheadline)
                                        .fontWeight(.medium)
                                        .foregroundColor(player.tfhmvt >= 0 ? .green : .red)
                                }
                            }
                        }
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 5, x: 0, y: 2)
        .task {
            if player.userOwnsPlayer && actualProfit == nil {
                await loadActualProfit()
            }
        }
    }
    
    @MainActor
    private func loadActualProfit() async {
        guard let selectedLeague = kickbaseManager.selectedLeague else { return }
        
        isLoadingProfit = true
        print("💰 Loading actual profit for player: \(player.fullName) (ID: \(player.id))")
        
        // Zugriff auf den playerService über KickbaseManager
        if let profit = await kickbaseManager.loadPlayerMarketValueOnDemand(
            playerId: player.id,
            leagueId: selectedLeague.id
        ) {
            actualProfit = profit
            print("✅ Successfully loaded actual profit: €\(profit)")
        } else {
            print("⚠️ Could not load actual profit, using fallback value")
        }
        
        isLoadingProfit = false
    }
}

// MARK: - Spiele und Gegner Sektion (neu)
struct PlayerMatchesSection: View {
    let player: TeamPlayer
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @State private var recentMatches: [EnhancedMatchPerformance] = []
    @State private var upcomingMatches: [EnhancedMatchPerformance] = []
    @State private var isLoading = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Spiele & Gegner")
                .font(.headline)
                .fontWeight(.bold)
            
            if isLoading {
                ProgressView("Lade Spiele...")
                    .progressViewStyle(CircularProgressViewStyle())
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 20)
            } else {
                VStack(spacing: 16) {
                    // Vergangene Spiele
                    if !recentMatches.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Vergangene Spiele")
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundColor(.secondary)
                            
                            ForEach(recentMatches.suffix(5)) { match in
                                MatchRow(match: match, isUpcoming: false)
                            }
                        }
                        
                        if !upcomingMatches.isEmpty {
                            Divider()
                        }
                    }
                    
                    // Zukünftige Spiele
                    if !upcomingMatches.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Kommende Spiele")
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundColor(.secondary)
                            
                            ForEach(upcomingMatches.prefix(3)) { match in
                                MatchRow(match: match, isUpcoming: true)
                            }
                        }
                    }
                    
                    // Hinweis, falls keine Spiele gefunden wurden
                    if recentMatches.isEmpty && upcomingMatches.isEmpty {
                        Text("Keine Spiele verfügbar.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.vertical, 20)
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 5, x: 0, y: 2)
        .task {
            await loadMatches()
        }
    }
    
    @MainActor
    private func loadMatches() async {
        guard let selectedLeague = kickbaseManager.selectedLeague else { return }
        
        isLoading = true
        print("⚽️ Loading enhanced matches for player: \(player.fullName) (ID: \(player.id))")
        
        do {
            // Lade vergangene Spiele mit Team-Info
            let recent = await kickbaseManager.loadPlayerRecentPerformanceWithTeamInfo(
                playerId: player.id,
                leagueId: selectedLeague.id
            )
            recentMatches = recent ?? []
            
            // Lade zukünftige Spiele mit Team-Info
            let upcoming = await kickbaseManager.loadPlayerUpcomingPerformanceWithTeamInfo(
                playerId: player.id,
                leagueId: selectedLeague.id
            )
            upcomingMatches = upcoming ?? []
            
            print("✅ Successfully loaded \(recentMatches.count) recent matches and \(upcomingMatches.count) upcoming matches")
        }
        
        isLoading = false
    }
}

// MARK: - Match Row Component
struct MatchRow: View {
    let match: EnhancedMatchPerformance
    let isUpcoming: Bool
    
    // Hilfsfunktion zur Bestimmung der Ergebnisfarbe
    private func getResultColor() -> Color {
        guard !isUpcoming else { return .secondary }
        
        // Parse das Ergebnis (Format: "2:1" oder ähnlich)
        let components = match.result.split(separator: ":")
        guard components.count == 2,
              let team1Goals = Int(components[0].trimmingCharacters(in: .whitespaces)),
              let team2Goals = Int(components[1].trimmingCharacters(in: .whitespaces)) else {
            return .primary // Fallback wenn Ergebnis nicht parsbar ist
        }
        
        // Bestimme ob der Spieler in Team 1 oder Team 2 ist
        let isPlayerInTeam1 = match.playerTeamId == match.team1Id
        
        // Bestimme das Ergebnis aus Sicht des Spielerteams
        let playerTeamGoals = isPlayerInTeam1 ? team1Goals : team2Goals
        let opponentGoals = isPlayerInTeam1 ? team2Goals : team1Goals
        
        if playerTeamGoals > opponentGoals {
            return .green // Sieg
        } else if playerTeamGoals < opponentGoals {
            return .red // Niederlage
        } else {
            return .primary // Unentschieden (weiß/default)
        }
    }
    
    var body: some View {
        VStack(spacing: 8) {
            HStack {
                // Spieltag
                Text("ST \(match.matchDay)")
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(match.isCurrent ? Color.orange : Color.blue)
                    .cornerRadius(6)
                
                Spacer()
                
                // Datum
                Text(formatMatchDate(match.parsedMatchDate))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            HStack {
                // Home vs Away mit Tabellenplatz
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 4) {
                        Text(match.team1Name)
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(match.playerTeamId == match.team1Id ? .blue : .primary)
                        
                        if let placement = match.team1Placement {
                            Text("(\(placement).)")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    Text("vs")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    HStack(spacing: 4) {
                        Text(match.team2Name)
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundColor(match.playerTeamId == match.team2Id ? .blue : .primary)
                        
                        if let placement = match.team2Placement {
                            Text("(\(placement).)")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                
                Spacer()
                
                // Ergebnis oder Status
                VStack(alignment: .trailing, spacing: 4) {
                    if isUpcoming {
                        Text("-:-")
                            .font(.headline)
                            .fontWeight(.bold)
                            .foregroundColor(.secondary)
                    } else {
                        Text(match.result)
                            .font(.headline)
                            .fontWeight(.bold)
                            .foregroundColor(getResultColor())
                    }
                    
                    // Spieler Status/Punkte
                    if isUpcoming {
                        Text("Geplant")
                            .font(.caption2)
                            .foregroundColor(.orange)
                    } else {
                        HStack(spacing: 4) {
                            Text("\(match.points) Pkt")
                                .font(.caption2)
                                .fontWeight(.medium)
                                .foregroundColor(match.points > 0 ? .green : .secondary)
                            
                            if match.wasStartingEleven {
                                Image(systemName: "star.fill")
                                    .font(.caption2)
                                    .foregroundColor(.orange)
                            } else if match.wasSubstitute {
                                Image(systemName: "arrow.up.circle.fill")
                                    .font(.caption2)
                                    .foregroundColor(.blue)
                            }
                        }
                    }
                }
            }
        }
        .padding(.vertical, 8)
        .padding(.horizontal, 12)
        .background(match.isCurrent ? Color.orange.opacity(0.1) : Color.clear)
        .cornerRadius(8)
    }
}

// Helper Funktion für Datum-Formatierung
private func formatMatchDate(_ date: Date) -> String {
    let formatter = DateFormatter()
    formatter.dateStyle = .short
    formatter.timeStyle = .none
    formatter.locale = Locale(identifier: "de_DE")
    return formatter.string(from: date)
}

private let dateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateStyle = .medium
    formatter.timeStyle = .short
    formatter.locale = Locale(identifier: "de_DE")
    return formatter
}()

// MARK: - Marktwert-Trend der letzten 3 Tage mit echten DailyMarketValueChange Daten
struct PlayerMarketTrendSection: View {
    let player: TeamPlayer
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @State private var marketValueHistory: MarketValueChange?
    @State private var isLoadingHistory = false
    @State private var hasLoaded = false
    
    var marketTrendData: [(day: String, value: Int, change: Int, changePercent: Double)] {
        guard let history = marketValueHistory else {
            // Zeige nur aktuellen Marktwert ohne historische Daten
            return [
                (day: "Heute", value: player.marketValue, change: player.tfhmvt, changePercent: 0.0),
                (day: "Gestern", value: 0, change: 0, changePercent: 0.0),
                (day: "Vorgestern", value: 0, change: 0, changePercent: 0.0)
            ]
        }
        
        // Verwende die letzten 3 Tage aus der echten Historie
        let sortedDailyChanges = history.dailyChanges.sorted { $0.daysAgo < $1.daysAgo }
        let last3Days = Array(sortedDailyChanges.prefix(3))
        
        return last3Days.enumerated().map { index, dailyChange in
            let dayName: String
            switch dailyChange.daysAgo {
            case 0: dayName = "Heute"
            case 1: dayName = "Gestern"
            case 2: dayName = "Vorgestern"
            default: dayName = "\(dailyChange.daysAgo) Tage"
            }
            
            return (
                day: dayName,
                value: dailyChange.value,
                change: dailyChange.change,
                changePercent: dailyChange.percentageChange
            )
        }
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("Marktwertentwicklung")
                    .font(.headline)
                    .fontWeight(.bold)
                
                Spacer()
                
                if isLoadingHistory {
                    ProgressView()
                        .scaleEffect(0.8)
                } else {
                    Text("(3 Tage)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            VStack(spacing: 8) {
                // Header-Zeile
                HStack {
                    Text("Tag")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(.secondary)
                        .frame(width: 70, alignment: .leading)
                    
                    Spacer()
                    
                    Text("Marktwert")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(.secondary)
                        .frame(width: 80, alignment: .center)
                    
                    Spacer()
                    
                    Text("Änderung")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(.secondary)
                        .frame(width: 70, alignment: .center)
                    
                    Spacer()
                    
                    Text("%")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(.secondary)
                        .frame(width: 50, alignment: .trailing)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color(.systemGray6))
                .cornerRadius(8)
                
                // Daten-Zeilen
                ForEach(Array(marketTrendData.enumerated()), id: \.offset) { index, data in
                    HStack {
                        // Tag
                        VStack(alignment: .leading, spacing: 2) {
                            Text(data.day)
                                .font(.subheadline)
                                .fontWeight(.medium)
                                .foregroundColor(index == 0 ? .primary : .secondary)
                            
                            if index == 0 {
                                Text("aktuell")
                                    .font(.caption2)
                                    .foregroundColor(.blue)
                            }
                        }
                        .frame(width: 70, alignment: .leading)
                        
                        Spacer()
                        
                        // Marktwert
                        Text(data.value > 0 ? formatCurrencyShort(data.value) : "-")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .frame(width: 80, alignment: .center)
                            .foregroundColor(data.value > 0 ? .primary : .secondary)
                        
                        Spacer()
                        
                        // Veränderung absolut
                        HStack(spacing: 2) {
                            if data.change != 0 {
                                Image(systemName: data.change >= 0 ? "arrow.up" : "arrow.down")
                                    .font(.caption2)
                                    .foregroundColor(data.change >= 0 ? .green : .red)
                            }
                            
                            Text(data.change == 0 ? "±0" : formatCurrencyShort(abs(data.change)))
                                .font(.caption)
                                .fontWeight(.medium)
                                .foregroundColor(data.change >= 0 ? .green : (data.change < 0 ? .red : .secondary))
                        }
                        .frame(width: 70, alignment: .center)
                        
                        Spacer()
                        
                        // Veränderung prozentual
                        Text(abs(data.changePercent) < 0.1 ? "±0%" : String(format: data.changePercent >= 0 ? "+%.1f%%" : "%.1f%%", data.changePercent))
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundColor(abs(data.changePercent) < 0.1 ? .secondary : (data.changePercent >= 0 ? .green : .red))
                            .frame(width: 50, alignment: .trailing)
                    }
                    .padding(.vertical, 6)
                    .padding(.horizontal, 12)
                    .background(index == 0 ? Color.blue.opacity(0.1) : Color.clear)
                    .cornerRadius(6)
                    .opacity(data.value > 0 ? 1.0 : 0.5)
                }
            }
            
            if marketValueHistory == nil && !isLoadingHistory && hasLoaded {
                Text("Keine Marktwerthistorie verfügbar")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.top, 8)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 5, x: 0, y: 2)
        .task {
            guard !hasLoaded else { return }
            await loadMarketValueHistory()
        }
    }
    
    @MainActor
    private func loadMarketValueHistory() async {
        guard !player.id.isEmpty,
              let selectedLeague = kickbaseManager.selectedLeague else {
            print("⚠️ Cannot load market value history: missing player ID or league")
            hasLoaded = true
            return
        }
        
        isLoadingHistory = true
        print("📈 Loading market value history for player: \(player.fullName) (ID: \(player.id))")
        
        do {
            // Zugriff auf den playerService über KickbaseManager
            let history = await kickbaseManager.loadPlayerMarketValueHistory(
                playerId: player.id,
                leagueId: selectedLeague.id
            )
            
            if let history = history {
                print("✅ Successfully loaded market value history with \(history.dailyChanges.count) daily changes")
                marketValueHistory = history
            } else {
                print("⚠️ No market value history returned from API")
            }
        }
        
        isLoadingHistory = false
        hasLoaded = true
    }
}

// MARK: - Helper Views
struct PlayerInfoCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(color)
            
            Text(value)
                .font(.subheadline)
                .fontWeight(.semibold)
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .minimumScaleFactor(0.8)
            
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(height: 70)
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(color.opacity(0.1))
        .cornerRadius(10)
    }
}

// MARK: - Helper Functions
private func formatCurrency(_ value: Int) -> String {
    let formatter = NumberFormatter()
    formatter.numberStyle = .currency
    formatter.currencyCode = "EUR"
    formatter.maximumFractionDigits = 0
    return formatter.string(from: NSNumber(value: value)) ?? "\(value) €"
}

private func formatCurrencyShort(_ value: Int) -> String {
    if value >= 1000000 {
        return String(format: "%.1fM €", Double(value) / 1000000)
    } else if value >= 1000 {
        return String(format: "%.0fk €", Double(value) / 1000)
    } else {
        return "\(value) €"
    }
}

private func getPlayerStatusText(_ status: Int) -> String {
    switch status {
    case 0:
        return "Verfügbar"
    case 1:
        return "Verletzt"
    case 2:
        return "Angeschlagen"
    case 3:
        return "Gesperrt"
    case 4:
        return "Aufbautraining"
    case 8:
        return "Sperre"  // Neuer Status für Sperre
    default:
        return "Unbekannt"
    }
}

// MARK: - Preview
#Preview {
    PlayerDetailView(player: Player(
        id: "1",
        firstName: "Max",
        lastName: "Mustermann",
        profileBigUrl: "",
        teamName: "Beispiel FC",
        teamId: "1",
        position: 3,
        number: 10,
        averagePoints: 7.5,
        totalPoints: 150,
        marketValue: 5000000,
        marketValueTrend: 250000,
        tfhmvt: 100000,
        prlo: 500000,
        stl: 0,
        status: 0,
        userOwnsPlayer: true
    ))
    .environmentObject(KickbaseManager())
}
