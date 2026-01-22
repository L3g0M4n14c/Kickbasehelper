import KickbaseCore
import SwiftUI

struct LigainsiderView: View {
    // Verwende jetzt den globalen Service
    @EnvironmentObject var service: LigainsiderService

    var body: some View {
        List {
            if service.isLoading {
                ProgressView("Lade Aufstellungen...")
            } else if let error = service.errorMessage {
                Text(error)
                    .foregroundColor(.red)
                Button("Erneut versuchen") {
                    service.fetchLineups()
                }
            } else if service.matches.isEmpty {
                Text(
                    "Keine Aufstellungen gefunden. Überprüfe die Internetverbindung oder ziehe zum Aktualisieren."
                )
                .multilineTextAlignment(.center)
                .padding()
                Button("Laden") {
                    service.fetchLineups()
                }
            } else {
                ForEach(service.matches) { match in
                    LigainsiderMatchRow(match: match)
                }
            }
        }
        .navigationTitle("Voraussichtliche Aufstellungen")
        .onAppear {
            if service.matches.isEmpty {
                service.fetchLineups()
            }
        }
        .toolbar {
            Button(action: { service.fetchLineups() }) {
                Image(systemName: "arrow.clockwise")
            }
        }
    }
}

// MARK: - Row View (List Item)

struct LigainsiderMatchRow: View {
    let match: LigainsiderMatch
    @State private var isExpanded = false

    var body: some View {
        VStack(spacing: 0) {
            // Header: Team vs Team
            Button(action: { withAnimation { isExpanded.toggle() } }) {
                HStack {
                    // Home Team
                    HStack(spacing: 8) {
                        if let logo = match.homeLogo, let url = URL(string: logo) {
                            AsyncImage(url: url) { phase in
                                switch phase {
                                case .empty:
                                    ProgressView()
                                        .frame(width: 30, height: 30)
                                case .success(let image):
                                    image
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: 30, height: 30)
                                case .failure:
                                    Image(systemName: "shield")
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: 30, height: 30)
                                        .foregroundColor(.gray)
                                @unknown default:
                                    EmptyView()
                                }
                            }
                        }
                        Text(match.homeTeam)
                            .font(.headline)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    Text("vs")
                        .foregroundColor(.gray)
                        .font(.caption)

                    // Away Team
                    HStack(spacing: 8) {
                        Text(match.awayTeam)
                            .font(.headline)
                            .multilineTextAlignment(.trailing)

                        if let logo = match.awayLogo, let url = URL(string: logo) {
                            AsyncImage(url: url) { phase in
                                switch phase {
                                case .empty:
                                    ProgressView()
                                        .frame(width: 30, height: 30)
                                case .success(let image):
                                    image
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: 30, height: 30)
                                case .failure:
                                    Image(systemName: "shield")
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: 30, height: 30)
                                        .foregroundColor(.gray)
                                @unknown default:
                                    EmptyView()
                                }
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .trailing)

                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .foregroundColor(.gray)
                }
                .padding(.vertical, 8)
            }
            .buttonStyle(PlainButtonStyle())

            if isExpanded {
                Divider()
                // Pitch Views für beide Teams
                VStack(spacing: 20) {
                    // Heim
                    VStack(alignment: .leading) {
                        Text("Heim: \(match.homeTeam)")
                            .font(.subheadline).bold()
                            .padding(.top, 8)
                        PitchView(rows: match.homeLineup)
                    }

                    Divider()

                    // Gast
                    VStack(alignment: .leading) {
                        Text("Gast: \(match.awayTeam)")
                            .font(.subheadline).bold()
                        PitchView(rows: match.awayLineup)
                    }
                }
                .padding(.vertical)
            }
        }
    }
}

// MARK: - Pitch View (Spielfeld Darstellung)

struct PitchView: View {
    let rows: [[LigainsiderPlayer]]

    var body: some View {
        ZStack {
            // Background (Spielfeldrasen Optik)
            RoundedRectangle(cornerRadius: 12)
                .fill(
                    LinearGradient(
                        gradient: Gradient(colors: [
                            Color.green.opacity(0.8), Color.green.opacity(0.6),
                        ]), startPoint: .top, endPoint: .bottom)
                )
                .overlay(
                    // Spielfeld Linien Andeutung
                    VStack {
                        Divider().background(Color.white.opacity(0.5))
                        Spacer()
                        Circle().stroke(Color.white.opacity(0.3), lineWidth: 2)
                            .frame(width: 80, height: 80)
                        Spacer()
                        Divider().background(Color.white.opacity(0.5))
                    }
                    .padding()
                )

            // Spieler Positionen
            VStack(spacing: 12) {
                // Wir gehen durch die Reihen (GK bis ST)
                // Ligainsider gibt oft GK zuerst. Row1=GK.
                ForEach(Array(rows.enumerated()), id: \.offset) { index, rowPlayers in
                    HStack(spacing: 10) {
                        ForEach(rowPlayers) { player in
                            PlayerPillView(player: player)
                        }
                    }
                }
            }
            .padding(.vertical, 20)
        }
        .frame(minHeight: 250)  // Mindesthöhe für das Spielfeld
    }
}

// MARK: - Player Pill (Einzelner Spieler Marker)

struct PlayerPillView: View {
    let player: LigainsiderPlayer

    var body: some View {
        VStack(spacing: 4) {
            // Spieler Icon / Kreis
            ZStack {
                Circle()
                    .fill(Color.white)
                    .frame(width: 30, height: 30)

                Text(String(player.name.prefix(1)))
                    .font(.caption).bold()
                    .foregroundColor(.black)
            }

            // Name
            Text(player.name)
                .font(.system(size: 10, weight: .semibold))
                .foregroundColor(.white)
                .lineLimit(1)
                .truncationMode(.tail)
                .padding(.horizontal, 4)
                .background(Color.black.opacity(0.4))
                .cornerRadius(4)

            // Alternative anzeigen via Icon (1. Option / 2. Option) falls möglich
            // Da wir hier LigainsiderPlayer haben, kennen wir den Status via 'alternative' property
            // Wenn player.alternative != nil -> Er ist S11, aber hat Alternative (1. Option)
            // Wenn er eine Alternative IST -> Das wissen wir hier im Pill View isoliert nicht sicher,
            // (außer wir checken ob er als Alternative im Match gelistet war - aber PillView kriegt nur Player).
            // ABER: LigainsiderStatus Logik in Service nutzt Cache.
            // Checken wir den Status via Service? PitchView hat keinen Service access direkt, aber LigainsiderView hat environment.
            // Wir fügen EnvironmentObject zu PitchView/PillView hinzu?
            
            if let alt = player.alternative {
                HStack(spacing: 2) {
                    Image(systemName: "1.circle.fill") // 1. Option (Wackelkandidat)
                        .font(.system(size: 8))
                        .foregroundColor(.orange)
                    Text(alt)
                        .font(.system(size: 8))
                        .lineLimit(1)
                }
                .foregroundColor(.white)
                .padding(2)
                .background(Color.black.opacity(0.5)) // Neutralerer Hintergrund
                .cornerRadius(4)
            }
        }
        .frame(minWidth: 60)
    }
}

#Preview {
    LigainsiderView()
}
