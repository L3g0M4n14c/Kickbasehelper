import SwiftUI

struct LiveView: View {
    @ObservedObject var kickbaseManager: KickbaseManager
    @EnvironmentObject var authManager: AuthenticationManager
    @State private var selectedPlayer: LivePlayer?

    var body: some View {
        NavigationStack {
            Group {
                if kickbaseManager.isLoading {
                    ProgressView("Lade Live-Daten...")
                } else if kickbaseManager.livePlayers.isEmpty {
                    VStack {
                        Image(systemName: "sportscourt")
                            .font(.system(size: 60))
                            .foregroundColor(.gray)
                        Text("Keine Live-Daten verfügbar")
                            .font(.headline)
                            .padding(.top)
                        if let error = kickbaseManager.errorMessage {
                            Text("Fehler: \(error)")
                                .foregroundColor(.red)
                                .multilineTextAlignment(.center)
                                .padding()
                        }

                        Text(
                            "Möglicherweise läuft gerade kein Spieltag oder die Aufstellung ist leer."
                        )
                        .font(.subheadline)
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                        .padding()
                        Button("Aktualisieren") {
                            Task {
                                await kickbaseManager.loadLivePoints()
                            }
                        }
                    }
                } else {
                    List {
                        // Summary Section
                        Section {
                            HStack {
                                VStack(alignment: .leading) {
                                    Text("Gesamtpunkte")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                    Text("\(calculateTotalPoints())")
                                        .font(.system(size: 34, weight: .bold))
                                        .foregroundColor(
                                            calculateTotalPoints() >= 0 ? .green : .red)
                                }
                                Spacer()
                                VStack(alignment: .trailing) {
                                    Text("Spieler im Einsatz")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                    Text("\(kickbaseManager.livePlayers.count)")
                                        .font(.title2)
                                        .fontWeight(.semibold)
                                }
                            }
                            .padding(.vertical, 8)
                        }

                        // Players List
                        Section(header: Text("Meine Aufstellung")) {
                            ForEach(sortedPlayers) { player in
                                Button {
                                    selectedPlayer = player
                                } label: {
                                    LivePlayerRow(player: player)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .refreshable {
                        await kickbaseManager.loadLivePoints()
                    }
                }
            }
            .sheet(item: $selectedPlayer) { player in
                if let league = kickbaseManager.selectedLeague {
                    PlayerMatchDetailView(
                        player: player,
                        league: league,
                        kickbaseManager: kickbaseManager
                    )
                } else {
                    Text("Keine Liga ausgewählt")
                }
            }
            .task(id: kickbaseManager.selectedLeague?.id) {
                if kickbaseManager.selectedLeague != nil {
                    await kickbaseManager.loadLivePoints()
                }
            }
            .onAppear {
                if kickbaseManager.selectedLeague != nil {
                    Task {
                        await kickbaseManager.loadLivePoints()
                    }
                }
            }
        }
    }

    // Sort players by position (GK -> DEF -> MID -> FWD) then points
    private var sortedPlayers: [LivePlayer] {
        kickbaseManager.livePlayers.sorted {
            if $0.position == $1.position {
                return $0.p > $1.p
            }
            return $0.position < $1.position
        }
    }

    private func calculateTotalPoints() -> Int {
        return kickbaseManager.livePlayers.reduce(0) { $0 + $1.p }
    }
}

struct LivePlayerRow: View {
    let player: LivePlayer

    var body: some View {
        HStack {
            // Player Image or Placeholder
            AsyncImage(url: player.imageUrl) { image in
                image.resizable()
            } placeholder: {
                Image(systemName: "person.fill")
                    .resizable()
                    .padding(8)
                    .foregroundColor(.gray)
                    #if os(iOS)
                        .background(Color(uiColor: .systemGray6))
                    #else
                        .background(Color.gray.opacity(0.2))
                    #endif
            }
            .frame(width: 40, height: 40)
            .clipShape(Circle())

            VStack(alignment: .leading) {
                HStack(spacing: 4) {
                    Text(player.name)
                        .font(.headline)

                    if !player.eventIcons.isEmpty {
                        Text(player.eventIcons)
                            .font(.subheadline)
                    }
                }

                Text(positionName(for: player.position))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            // Points Badge
            Text("\(player.p)")
                .font(.title3)
                .fontWeight(.bold)
                .foregroundColor(player.p >= 0 ? .green : .red)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                #if os(iOS)
                    .background(Color(uiColor: .systemGray6))
                #else
                    .background(Color.gray.opacity(0.2))
                #endif
                .cornerRadius(8)
        }
        .padding(.vertical, 4)
    }

    private func positionName(for position: Int) -> String {
        switch position {
        case 1: return "Torwart"
        case 2: return "Abwehr"
        case 3: return "Mittelfeld"
        case 4: return "Angriff"
        default: return "Spieler"
        }
    }
}
