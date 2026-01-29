import SwiftUI

struct PlayerMatchDetailView: View {
    let player: LivePlayer  // From LiveModels.swift
    let league: League  // From Models.swift
    @ObservedObject var kickbaseManager: KickbaseManager
    @EnvironmentObject var ligainsiderService: LigainsiderService
    @Environment(\.dismiss) var dismiss

    @State private var details: PlayerMatchDetailResponse?
    @State private var isLoading = false
    @State private var error: String?

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
        NavigationStack {
            Group {
                if isLoading {
                    ProgressView("Lade Details...")
                } else if let error = error {
                    VStack {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.largeTitle)
                            .foregroundColor(.orange)
                        Text(error)
                            .multilineTextAlignment(.center)
                            .padding()
                        Button("Erneut versuchen") {
                            loadDetails()
                        }
                    }
                } else if let details = details {
                    List {
                        // Header
                        Section {
                            HStack {
                                AsyncImage(url: photoUrl) { image in
                                    image.resizable().scaledToFit()
                                } placeholder: {
                                    Image(systemName: "person.fill")
                                        .resizable().scaledToFit()
                                        .foregroundColor(.gray)
                                }
                                .onAppear {
                                    print(
                                        "Loading match player image for \(player.name): \(photoUrl?.absoluteString ?? "nil")"
                                    )
                                }
                                .onChange(of: photoUrl) {
                                    print(
                                        "Match player image URL changed for \(player.name): \(photoUrl?.absoluteString ?? "nil")"
                                    )
                                }
                                .frame(width: 80, height: 80)
                                .clipShape(Circle())

                                VStack(alignment: .leading, spacing: 5) {
                                    Text(player.name)
                                        .font(.title2)
                                        .bold()
                                    Text("Punkte: \(player.p)")
                                        .font(.headline)
                                        .foregroundColor(player.p >= 0 ? .green : .red)
                                }
                            }
                            .padding(.vertical)
                        }

                        // Events
                        Section("Ereignisse") {
                            if details.events.isEmpty {
                                Text("Keine Ereignisse vorhanden")
                                    .foregroundColor(.secondary)
                            } else {
                                ForEach(
                                    details.events.sorted {
                                        ($0.minute ?? 0) > ($1.minute ?? 0)
                                    }
                                ) { event in
                                    if let eventName = resolveEventName(event) {
                                        HStack {
                                            Text("\(event.minute ?? 0)'")
                                                #if !SKIP
                                                    .monospacedDigit()
                                                #endif
                                                .frame(width: 35, alignment: .trailing)
                                                .foregroundColor(.secondary)

                                            Text(event.icon)

                                            Text(eventName)

                                            Spacer()

                                            Text("\(event.points ?? 0)")
                                                .bold()
                                                .foregroundColor(
                                                    (event.points ?? 0) >= 0 ? .green : .red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text("Keine Daten geladen")
                }
            }
            .navigationTitle("Match Details")
            #if os(iOS)
                .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                #if os(iOS)
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("Fertig") {
                            dismiss()
                        }
                    }
                #else
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Fertig") {
                            dismiss()
                        }
                    }
                #endif
            }
            .task {
                if kickbaseManager.eventTypeNames.isEmpty {
                    await kickbaseManager.loadEventDefinitions()
                }
                loadDetails()
            }
        }
    }

    private func loadDetails() {
        isLoading = true
        error = nil
        Task {
            do {
                details = try await kickbaseManager.loadPlayerMatchDetails(
                    leagueId: league.id,
                    competitionId: league.competitionId,
                    playerId: player.id,
                    dayNumber: league.matchDay
                )
            } catch {
                print("Error loading details: \(error)")
                self.error = "Details konnten nicht geladen werden.\n\(error.localizedDescription)"
            }
            isLoading = false
        }
    }

    private func resolveEventName(_ event: PlayerMatchEvent) -> String? {
        if let name = event.name {
            return name
        }
        guard let type = event.type else { return nil }
        return kickbaseManager.eventTypeNames[type]
    }
}
