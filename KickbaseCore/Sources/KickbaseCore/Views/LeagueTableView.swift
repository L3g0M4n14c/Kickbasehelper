import SwiftUI

@MainActor
struct LeagueTableView: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @StateObject var viewModel = LeagueTableViewModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Segmented picker to switch between table types
                if viewModel.selectedLeague != nil {
                    Picker("", selection: $viewModel.tableType) {
                        Text("Gesamttabelle").tag(LeagueTableViewModel.TableType.overall)
                        Text("Spieltag").tag(LeagueTableViewModel.TableType.matchday)
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal)
                    .padding(.top, 8)
                    .onChange(of: viewModel.tableType) { oldValue, newValue in
                        Task {
                            await viewModel.switchTableType(to: newValue)
                        }
                    }

                    // Matchday selector (shown only when matchday mode is selected)
                    if viewModel.tableType == .matchday {
                        HStack {
                            Text("Spieltag auswählen:")
                                .font(.subheadline)
                                .foregroundColor(.secondary)

                            Picker("Spieltag", selection: $viewModel.selectedMatchDay) {
                                // Show all 34 matchdays for Bundesliga season
                                ForEach(1...34, id: \.self) { day in
                                    Text("Spieltag \(day)").tag(day)
                                }
                            }
                            .pickerStyle(.menu)
                            .onChange(of: viewModel.selectedMatchDay) { oldValue, newValue in
                                Task {
                                    await viewModel.selectMatchDay(newValue)
                                }
                            }
                        }
                        .padding(.horizontal)
                        .padding(.bottom, 8)
                    } else {
                        Spacer()
                            .frame(height: 8)
                    }
                }

                Group {
                    if viewModel.isLoading {
                        ProgressView("Lade Tabelle...")
                    } else if viewModel.displayedUsers.isEmpty {
                        VStack {
                            Image(systemName: "list.number")
                                .font(.system(size: 60))
                                .foregroundColor(.gray)
                            Text("Keine Tabellendaten verfügbar")
                                .font(.headline)
                                .padding(.top)
                            Text(
                                "Bitte wähle eine Liga aus oder aktualisiere die Daten."
                            )
                            .font(.subheadline)
                            .foregroundColor(.gray)
                            .multilineTextAlignment(.center)
                            .padding()
                            Button("Aktualisieren") {
                                Task {
                                    await viewModel.refresh()
                                }
                            }
                        }
                    } else {
                        List {
                            ForEach(Array(viewModel.displayedUsers.enumerated()), id: \.element.id)
                            { index, user in
                                NavigationLink(
                                    destination: UserDetailView(
                                        user: user,
                                        selectedMatchDay: viewModel.tableType == .matchday
                                            ? viewModel.selectedMatchDay : nil
                                    )
                                ) {
                                    LeagueUserRow(user: user, position: index + 1)
                                }
                            }
                            .refreshable {
                                await viewModel.refresh()
                            }
                        }
                    }
                }
            }
            .navigationTitle(
                viewModel.tableType == .overall
                    ? "Tabelle" : "Spieltag \(viewModel.selectedMatchDay)"
            )
            .onAppear {
                viewModel.setKickbaseManager(kickbaseManager)
                Task {
                    await viewModel.loadOverallRanking()
                }
            }
        }
    }
}

struct LeagueUserRow: View {
    let user: LeagueUser
    let position: Int

    var body: some View {
        HStack(spacing: 12) {
            // Position badge
            Text("\(position)")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 32, height: 32)
                .background(positionColor)
                .clipShape(Circle())

            // User info
            VStack(alignment: .leading, spacing: 4) {
                Text(user.name)
                    .font(.headline)
                Text(user.teamName)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            Spacer()

            // Points
            VStack(alignment: .trailing, spacing: 4) {
                Text("\(user.points)")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.primary)
                Text("Punkte")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }

    private var positionColor: Color {
        switch position {
        case 1:
            return .yellow
        case 2:
            return Color(red: 0.75, green: 0.75, blue: 0.75)  // Silver
        case 3:
            return Color(red: 0.8, green: 0.5, blue: 0.2)  // Bronze
        default:
            return .blue
        }
    }
}
