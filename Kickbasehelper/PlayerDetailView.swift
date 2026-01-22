import KickbaseCore
import SwiftUI

struct PlayerDetailView: View {
    let player: TeamPlayer
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @EnvironmentObject var ligainsiderService: LigainsiderService

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    // Hero Header mit Spielerfoto und allen Grundinformationen
                    PlayerHeroHeader(player: player)
                        .environmentObject(ligainsiderService)

                    // Punktzahl-Performance
                    PlayerPointsSection(player: player)

                    // Marktwert und Finanzen
                    PlayerMarketValueSection(player: player)

                    // Spiele und Gegner (neu)
                    PlayerMatchesSection(player: player)

                    // Marktwertentwicklung der letzten 3 Tage
                    PlayerMarketTrendSection(player: player)

                    // Transfer-Vergleich und Alternativen
                    PlayerAlternativesSection(player: player)
                }
                .padding()
            }
            .background(
                LinearGradient(
                    gradient: Gradient(colors: [
                        positionColor(player.position).opacity(0.1),
                        Color(.systemBackground),
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
    @EnvironmentObject var ligainsiderService: LigainsiderService

    private var heroImageUrl: URL? {
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
        VStack(spacing: 20) {
            // Großes Profilbild mit Position Badge
            ZStack(alignment: .bottomTrailing) {
                AsyncImage(url: heroImageUrl) { image in
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

                // Statusbereich (Verletzung + Ligainsider)
                HStack(spacing: 15) {
                    // Fit-Status Badge
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

                    // Ligainsider Status Badge
                    if !ligainsiderService.matches.isEmpty {
                        let liStatus = ligainsiderService.getPlayerStatus(
                            firstName: player.firstName, lastName: player.lastName)
                        let colorString = ligainsiderService.getColor(for: liStatus)
                        let color =
                            (colorString == "green")
                            ? Color.green : (colorString == "orange" ? Color.orange : Color.red)

                        HStack(spacing: 6) {
                            Image(systemName: ligainsiderService.getIcon(for: liStatus))
                                .foregroundColor(color)
                            Text(
                                liStatus == .likelyStart
                                    ? "S11"
                                    : (liStatus == .startWithAlternative
                                        ? "1. Option"
                                        : (liStatus == .isAlternative ? "2. Option" : "Bank/Out"))
                            )
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundColor(color)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(color.opacity(0.15))
                        .cornerRadius(16)
                    }
                }
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
                                    Image(
                                        systemName: player.tfhmvt >= 0 ? "arrow.up" : "arrow.down"
                                    )
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

            print(
                "✅ Successfully loaded \(recentMatches.count) recent matches and \(upcomingMatches.count) upcoming matches"
            )
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
            let team2Goals = Int(components[1].trimmingCharacters(in: .whitespaces))
        else {
            return .primary  // Fallback wenn Ergebnis nicht parsbar ist
        }

        // Bestimme ob der Spieler in Team 1 oder Team 2 ist
        let isPlayerInTeam1 = match.playerTeamId == match.team1Id

        // Bestimme das Ergebnis aus Sicht des Spielerteams
        let playerTeamGoals = isPlayerInTeam1 ? team1Goals : team2Goals
        let opponentGoals = isPlayerInTeam1 ? team2Goals : team1Goals

        if playerTeamGoals > opponentGoals {
            return .green  // Sieg
        } else if playerTeamGoals < opponentGoals {
            return .red  // Niederlage
        } else {
            return .primary  // Unentschieden (weiß/default)
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
                (
                    day: "Heute", value: player.marketValue, change: player.tfhmvt,
                    changePercent: 0.0
                ),
                (day: "Gestern", value: 0, change: 0, changePercent: 0.0),
                (day: "Vorgestern", value: 0, change: 0, changePercent: 0.0),
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
                                .foregroundColor(
                                    data.change >= 0
                                        ? .green : (data.change < 0 ? .red : .secondary))
                        }
                        .frame(width: 70, alignment: .center)

                        Spacer()

                        // Veränderung prozentual
                        Text(
                            abs(data.changePercent) < 0.1
                                ? "±0%"
                                : String(
                                    format: data.changePercent >= 0 ? "+%.1f%%" : "%.1f%%",
                                    data.changePercent)
                        )
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(
                            abs(data.changePercent) < 0.1
                                ? .secondary : (data.changePercent >= 0 ? .green : .red)
                        )
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
            let selectedLeague = kickbaseManager.selectedLeague
        else {
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
                print(
                    "✅ Successfully loaded market value history with \(history.dailyChanges.count) daily changes"
                )
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
    if value >= 1_000_000 {
        return String(format: "%.1fM €", Double(value) / 1_000_000)
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

// MARK: - Transfer-Vergleich & Alternativen Sektion
struct PlayerAlternativesSection: View {
    let player: TeamPlayer
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @State private var alternatives: [MarketPlayer] = []
    @State private var isLoading = false
    @State private var comparisonMetrics: [ComparisonMetric] = []

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Transfer-Alternativen")
                .font(.headline)
                .fontWeight(.bold)

            if isLoading {
                ProgressView("Lade Alternativen...")
                    .progressViewStyle(CircularProgressViewStyle())
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 20)
            } else if alternatives.isEmpty {
                Text("Keine besseren Alternativen gefunden")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 20)
            } else {
                // Vergleichs-Kennzahlen
                VStack(spacing: 12) {
                    Text("Vergleich mit Alternativen")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.secondary)

                    ComparisonMetricsView(
                        currentPlayer: player,
                        metrics: comparisonMetrics
                    )
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)

                Divider()

                // Alternativen-Liste
                VStack(alignment: .leading, spacing: 12) {
                    Text("Empfohlene Spieler")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.secondary)

                    ForEach(alternatives.prefix(5), id: \.id) { alternative in
                        AlternativePlayerCard(
                            currentPlayer: player,
                            alternativePlayer: alternative
                        )
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 5, x: 0, y: 2)
        .task {
            await loadAlternatives()
        }
    }

    @MainActor
    private func loadAlternatives() async {
        guard let selectedLeague = kickbaseManager.selectedLeague else { return }

        isLoading = true
        print(
            "🔄 Loading alternatives for player: \(player.fullName) (Position: \(player.position))")

        do {
            let loadedAlternatives = try await findBetterAlternatives(
                for: player,
                in: selectedLeague
            )

            alternatives = loadedAlternatives

            // Berechne Vergleichs-Metriken
            if !loadedAlternatives.isEmpty {
                comparisonMetrics = calculateComparisonMetrics(
                    currentPlayer: player,
                    alternatives: loadedAlternatives
                )
            }

            print("✅ Found \(loadedAlternatives.count) alternatives")
        } catch {
            print("❌ Error loading alternatives: \(error)")
        }

        isLoading = false
    }

    private func findBetterAlternatives(
        for player: TeamPlayer,
        in league: League
    ) async throws -> [MarketPlayer] {
        let marketPlayers = try await kickbaseManager.authenticatedPlayerService.loadMarketPlayers(
            for: league)

        // Filter: Gleiche Position
        let samePosition = marketPlayers.filter { $0.position == player.position }

        // Filter: Nicht der aktuelle Spieler selbst
        let filtered = samePosition.filter { $0.id != player.id }

        // Berechne Scores für jeden Kandidaten
        var candidates: [(player: MarketPlayer, score: Double)] = []

        for candidate in filtered {
            let score = calculateAlternativeScore(
                current: player,
                alternative: candidate
            )
            // Nur Kandidaten mit positivem Score berücksichtigen
            if score > 0 {
                candidates.append((player: candidate, score: score))
            }
        }

        // Sortiere nach Score absteigend und nimm Top 5
        return
            candidates
            .sorted { $0.score > $1.score }
            .prefix(5)
            .map { $0.player }
    }

    private func calculateAlternativeScore(
        current: TeamPlayer,
        alternative: MarketPlayer
    ) -> Double {
        var score = 0.0

        // HAUPTKRITERIUM: Absolute Leistung - nur Spieler mit ähnlicher oder besserer Performance
        let totalPointsRatio =
            alternative.totalPoints > 0
            ? Double(alternative.totalPoints) / Double(max(current.totalPoints, 1))
            : 0

        // Wenn Alternative weniger als 80% der Punkte hat, ist sie keine echte Alternative
        if totalPointsRatio < 0.8 {
            return 0.0  // Disqualifizierung
        }

        // 1. Leistungsvergleich (HÖCHSTE PRIORITÄT - 0-15 Punkte)
        let performanceRatio = alternative.averagePoints / max(current.averagePoints, 1.0)
        if performanceRatio > 1.15 {
            score += 15.0  // Deutlich bessere Leistung
        } else if performanceRatio > 1.05 {
            score += 10.0  // Bessere Leistung
        } else if performanceRatio > 0.95 {
            score += 6.0  // Ähnliche Leistung (gute Alternative mit besseren Kriterien)
        } else if performanceRatio > 0.85 {
            score += 2.0  // Etwas schlechter (nur wenn überragende Kriterien)
        } else {
            return 0.0  // Zu viel schlechter - nicht interessant
        }

        // 2. Preis-Leistungs-Verhältnis (SEKUNDÄR - 0-6 Punkte)
        let currentValueForMoney =
            current.marketValue > 0
            ? Double(current.totalPoints) / Double(current.marketValue) * 1_000_000 : 0
        let alternativeValueForMoney =
            alternative.price > 0
            ? Double(alternative.totalPoints) / Double(alternative.price) * 1_000_000 : 0

        let valueRatio = alternativeValueForMoney / max(currentValueForMoney, 0.1)
        if valueRatio > 1.3 {
            score += 6.0  // Deutlich besseres Preis-Leistungs-Verhältnis
        } else if valueRatio > 1.15 {
            score += 4.0  // Besseres Preis-Leistungs-Verhältnis
        } else if valueRatio > 1.0 {
            score += 2.0  // Leicht besseres Preis-Leistungs-Verhältnis
        } else if valueRatio < 0.9 {
            score -= 2.0  // Deutlich schlechteres Preis-Leistungs-Verhältnis
        }

        // 3. Verletzungsstatus (0-5 Punkte)
        if alternative.status == 0 && current.status != 0 {
            score += 5.0  // Großer Bonus wenn Alternative verfügbar und Spieler verletzt
        } else if alternative.status != 0 {
            score -= 3.0  // Penalty wenn Alternative verletzt
        }

        // 4. Marktwert-Trend / Form (0-4 Punkte)
        if alternative.marketValueTrend > current.marketValueTrend + 750000 {
            score += 4.0  // Deutlich bessere Form
        } else if alternative.marketValueTrend > current.marketValueTrend + 250000 {
            score += 2.0  // Bessere Form
        } else if alternative.marketValueTrend < current.marketValueTrend - 750000 {
            score -= 3.0  // Deutlich schlechtere Form
        }

        // 5. Absolute Kostenersparnis (0-3 Punkte - nur wenn günstiger UND gute Leistung)
        let priceDifference = alternative.price - current.marketValue
        if priceDifference < 0 && performanceRatio >= 0.95 {
            // Alternative ist günstiger UND Leistung ähnlich oder besser
            let savings = Double(abs(priceDifference)) / 1_000_000.0
            if savings > 3.0 {
                score += 3.0  // Gute Kostenersparnis
            } else if savings > 1.0 {
                score += 2.0  // Moderate Kostenersparnis
            } else {
                score += 1.0  // Kleine Kostenersparnis
            }
        }

        return max(score, 0.0)
    }

    private func calculateComparisonMetrics(
        currentPlayer: TeamPlayer,
        alternatives: [MarketPlayer]
    ) -> [ComparisonMetric] {
        var metrics: [ComparisonMetric] = []

        // Durchschnittliche Metriken der Alternativen
        let avgPerformance =
            alternatives.map { $0.averagePoints }.reduce(0, +) / max(Double(alternatives.count), 1)
        let avgPoints =
            alternatives.map { $0.totalPoints }.reduce(0, +) / max(alternatives.count, 1)
        let avgPrice = alternatives.map { $0.price }.reduce(0, +) / max(alternatives.count, 1)

        // Performance
        let performanceChange = avgPerformance - currentPlayer.averagePoints
        metrics.append(
            ComparisonMetric(
                name: "Ø Leistung",
                currentValue: currentPlayer.averagePoints,
                alternativeValue: avgPerformance,
                change: performanceChange,
                isPositive: performanceChange >= 0
            ))

        // Gesamtpunkte
        let pointsChange = Double(avgPoints - currentPlayer.totalPoints)
        metrics.append(
            ComparisonMetric(
                name: "Ø Gesamtpunkte",
                currentValue: Double(currentPlayer.totalPoints),
                alternativeValue: Double(avgPoints),
                change: pointsChange,
                isPositive: pointsChange >= 0
            ))

        // Preis
        let priceChange = Double(avgPrice - currentPlayer.marketValue)
        metrics.append(
            ComparisonMetric(
                name: "Ø Marktwert",
                currentValue: Double(currentPlayer.marketValue),
                alternativeValue: Double(avgPrice),
                change: priceChange,
                isPositive: priceChange <= 0  // Bei Preis: niedriger ist besser
            ))

        return metrics
    }
}

// MARK: - Comparison Metric Model
struct ComparisonMetric {
    let name: String
    let currentValue: Double
    let alternativeValue: Double
    let change: Double
    let isPositive: Bool
}

// MARK: - Vergleichs-Metriken View
struct ComparisonMetricsView: View {
    let currentPlayer: TeamPlayer
    let metrics: [ComparisonMetric]

    var body: some View {
        VStack(spacing: 12) {
            ForEach(metrics, id: \.name) { metric in
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(metric.name)
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.secondary)

                        HStack(spacing: 8) {
                            Text(formatMetricValue(metric.currentValue))
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundColor(.primary)

                            Image(systemName: "arrow.right")
                                .font(.caption2)
                                .foregroundColor(.secondary)

                            Text(formatMetricValue(metric.alternativeValue))
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundColor(metric.isPositive ? .green : .red)
                        }
                    }

                    Spacer()

                    VStack(alignment: .trailing, spacing: 4) {
                        HStack(spacing: 2) {
                            Image(systemName: metric.isPositive ? "arrow.up" : "arrow.down")
                                .font(.caption2)
                                .foregroundColor(metric.isPositive ? .green : .red)

                            Text(String(format: "%+.0f", metric.change))
                                .font(.caption2)
                                .fontWeight(.semibold)
                                .foregroundColor(metric.isPositive ? .green : .red)
                        }

                        Text(String(format: "%+.0f%%", (metric.change / metric.currentValue) * 100))
                            .font(.caption2)
                            .foregroundColor(metric.isPositive ? .green : .red)
                    }
                }
                .padding(.vertical, 8)
            }
        }
    }

    private func formatMetricValue(_ value: Double) -> String {
        if value > 1_000_000 {
            return String(format: "%.1fM €", value / 1_000_000)
        } else if value >= 1000 {
            return String(format: "%.0fk €", value / 1_000)
        } else if value > 100 {
            return String(format: "%.0f", value)
        } else {
            return String(format: "%.1f", value)
        }
    }
}

// MARK: - Alternative Player Card
struct AlternativePlayerCard: View {
    let currentPlayer: TeamPlayer
    let alternativePlayer: MarketPlayer

    var improvementPercentage: Double {
        let current = currentPlayer.averagePoints
        let alternative = alternativePlayer.averagePoints
        guard current > 0 else { return 0 }
        return ((alternative - current) / current) * 100
    }

    var valueImprovement: Double {
        let currentValue =
            currentPlayer.marketValue > 0
            ? Double(currentPlayer.totalPoints) / Double(currentPlayer.marketValue) * 1_000_000 : 0
        let altValue =
            alternativePlayer.price > 0
            ? Double(alternativePlayer.totalPoints) / Double(alternativePlayer.price) * 1_000_000
            : 0
        guard currentValue > 0 else { return 0 }
        return ((altValue - currentValue) / currentValue) * 100
    }

    var body: some View {
        VStack(spacing: 12) {
            // Header mit Foto und Namen
            HStack(spacing: 12) {
                AsyncImage(url: alternativePlayer.imageUrl) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } placeholder: {
                    ZStack {
                        Circle()
                            .fill(positionColor(alternativePlayer.position).opacity(0.3))

                        Image(systemName: "person.fill")
                            .font(.system(size: 20))
                            .foregroundColor(positionColor(alternativePlayer.position))
                    }
                }
                .frame(width: 50, height: 50)
                .clipShape(Circle())
                .overlay(
                    Circle()
                        .stroke(positionColor(alternativePlayer.position), lineWidth: 2)
                )

                VStack(alignment: .leading, spacing: 4) {
                    Text(alternativePlayer.fullName)
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .lineLimit(1)

                    HStack(spacing: 4) {
                        Text(alternativePlayer.fullTeamName)
                            .font(.caption2)
                            .foregroundColor(.secondary)

                        Text("•")
                            .foregroundColor(.secondary)

                        Text(alternativePlayer.positionName)
                            .font(.caption2)
                            .fontWeight(.semibold)
                            .foregroundColor(positionColor(alternativePlayer.position))
                    }
                }

                Spacer()

                // Score Badge
                VStack(alignment: .center, spacing: 2) {
                    Text("LEISTUNG")
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)

                    ZStack {
                        Circle()
                            .fill(improvementPercentage >= 0 ? Color.green : Color.orange)

                        Text(String(format: "%+.0f%%", improvementPercentage))
                            .font(.caption2)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                            .lineLimit(1)
                    }
                    .frame(width: 40, height: 40)
                }
            }

            Divider()

            // Vergleichs-Details
            HStack(spacing: 16) {
                // Leistung
                VStack(alignment: .leading, spacing: 4) {
                    Text("Leistung")
                        .font(.caption2)
                        .fontWeight(.semibold)
                        .foregroundColor(.secondary)

                    HStack(spacing: 4) {
                        Text(String(format: "%.1f", currentPlayer.averagePoints))
                            .font(.caption)
                            .foregroundColor(.primary)

                        Text("→")
                            .font(.caption2)
                            .foregroundColor(.secondary)

                        Text(String(format: "%.1f", alternativePlayer.averagePoints))
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                    }
                }

                Divider()

                // Marktwert
                VStack(alignment: .leading, spacing: 4) {
                    Text("Marktwert")
                        .font(.caption2)
                        .fontWeight(.semibold)
                        .foregroundColor(.secondary)

                    HStack(spacing: 4) {
                        Text(formatCurrencyShort(currentPlayer.marketValue))
                            .font(.caption)
                            .foregroundColor(.primary)

                        Text("→")
                            .font(.caption2)
                            .foregroundColor(.secondary)

                        Text(formatCurrencyShort(alternativePlayer.price))
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                    }
                }

                Divider()

                // Status
                VStack(alignment: .trailing, spacing: 4) {
                    Text("Status")
                        .font(.caption2)
                        .fontWeight(.semibold)
                        .foregroundColor(.secondary)

                    HStack(spacing: 4) {
                        Image(systemName: getStatusIcon(alternativePlayer.status))
                            .font(.caption)
                            .foregroundColor(getStatusColor(alternativePlayer.status))

                        Text(getPlayerStatusText(alternativePlayer.status))
                            .font(.caption2)
                            .fontWeight(.semibold)
                            .foregroundColor(getStatusColor(alternativePlayer.status))
                    }
                }
            }
            .padding(8)
            .background(Color(.systemGray6))
            .cornerRadius(8)

            // Value-for-Money Bonus
            if valueImprovement > 10 {
                HStack(spacing: 8) {
                    Image(systemName: "star.fill")
                        .font(.caption)
                        .foregroundColor(.orange)

                    VStack(alignment: .leading, spacing: 2) {
                        Text("Besseres Preis-Leistungs-Verhältnis")
                            .font(.caption2)
                            .fontWeight(.semibold)

                        Text(String(format: "%.0f%% besserer Value", valueImprovement))
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }

                    Spacer()
                }
                .padding(8)
                .background(Color.orange.opacity(0.1))
                .cornerRadius(8)
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }

    private func getStatusIcon(_ status: Int) -> String {
        switch status {
        case 0: return "checkmark.circle.fill"
        case 1: return "cross.circle.fill"
        case 2: return "pills.fill"
        case 3: return "exclamationmark.triangle.fill"
        case 4: return "dumbbell.fill"
        case 8: return "rectangle.fill"
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
        case 8: return .red
        default: return .gray
        }
    }
}

// MARK: - Preview
#Preview {
    PlayerDetailView(
        player: Player(
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
            marketValue: 5_000_000,
            marketValueTrend: 250000,
            tfhmvt: 100000,
            prlo: 500000,
            stl: 0,
            status: 0,
            userOwnsPlayer: true
        )
    )
    .environmentObject(KickbaseManager())
}
