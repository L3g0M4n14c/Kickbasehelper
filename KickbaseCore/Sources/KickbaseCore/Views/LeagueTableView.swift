import SwiftUI

struct LeagueTableView: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @State private var tableType: TableType = .overall
    @State private var selectedMatchDay: Int = 1
    
    enum TableType {
        case overall
        case matchday
    }
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Segmented picker to switch between table types
                if let league = kickbaseManager.selectedLeague {
                    Picker("", selection: $tableType) {
                        Text("Gesamttabelle").tag(TableType.overall)
                        Text("Spieltag").tag(TableType.matchday)
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal)
                    .padding(.top, 8)
                    .onChange(of: tableType) { oldValue, newValue in
                        if newValue == .matchday {
                            Task {
                                await kickbaseManager.loadMatchDayRanking(for: league, matchDay: selectedMatchDay)
                            }
                        }
                    }
                    
                    // Matchday selector (shown only when matchday mode is selected)
                    if tableType == .matchday {
                        HStack {
                            Text("Spieltag auswählen:")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            
                            Picker("Spieltag", selection: $selectedMatchDay) {
                                ForEach(1...league.matchDay, id: \.self) { day in
                                    Text("Spieltag \(day)").tag(day)
                                }
                            }
                            .pickerStyle(.menu)
                            .onChange(of: selectedMatchDay) { oldValue, newValue in
                                Task {
                                    await kickbaseManager.loadMatchDayRanking(for: league, matchDay: newValue)
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
                    if kickbaseManager.isLoading {
                        ProgressView("Lade Tabelle...")
                    } else if displayedUsers.isEmpty {
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
                                    if let league = kickbaseManager.selectedLeague {
                                        if tableType == .overall {
                                            await kickbaseManager.loadLeagueRanking(for: league)
                                        } else {
                                            await kickbaseManager.loadMatchDayRanking(for: league, matchDay: selectedMatchDay)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        List {
                            ForEach(Array(displayedUsers.enumerated()), id: \.element.id) { index, user in
                                LeagueUserRow(user: user, position: index + 1)
                            }
                        }
                        .refreshable {
                            if let league = kickbaseManager.selectedLeague {
                                if tableType == .overall {
                                    await kickbaseManager.loadLeagueRanking(for: league)
                                } else {
                                    await kickbaseManager.loadMatchDayRanking(for: league, matchDay: selectedMatchDay)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle(tableType == .overall ? "Tabelle" : "Spieltag \(selectedMatchDay)")
            .onAppear {
                if kickbaseManager.leagueUsers.isEmpty,
                   let league = kickbaseManager.selectedLeague {
                    Task {
                        await kickbaseManager.loadLeagueRanking(for: league)
                    }
                }
                // Set selectedMatchDay to current matchday on appear
                if let league = kickbaseManager.selectedLeague {
                    selectedMatchDay = league.matchDay
                }
            }
        }
    }
    
    private var displayedUsers: [LeagueUser] {
        tableType == .overall ? kickbaseManager.leagueUsers : kickbaseManager.matchDayUsers
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
            return Color(red: 0.75, green: 0.75, blue: 0.75) // Silver
        case 3:
            return Color(red: 0.8, green: 0.5, blue: 0.2) // Bronze
        default:
            return .blue
        }
    }
}
