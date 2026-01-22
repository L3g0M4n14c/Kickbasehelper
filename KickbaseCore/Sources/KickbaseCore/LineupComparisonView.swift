import SwiftUI

struct LineupComparisonView: View {
    let comparison: LineupComparison
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @State private var showTeamOnly = true

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Tab-Selector
                Picker("Ansicht", selection: $showTeamOnly) {
                    Text("Nur eigene Spieler").tag(true)
                    if comparison.shouldShowHybrid {
                        Text("Mit Marktspieler").tag(false)
                    }
                }
                .pickerStyle(.segmented)
                .padding()

                if showTeamOnly {
                    LineupDetailView(
                        lineup: comparison.teamOnlyLineup,
                        teamPlayers: kickbaseManager.teamPlayers,
                        marketPlayers: []
                    )
                } else if comparison.shouldShowHybrid, let hybridLineup = comparison.hybridLineup {
                    LineupDetailView(
                        lineup: hybridLineup,
                        teamPlayers: kickbaseManager.teamPlayers,
                        marketPlayers: kickbaseManager.marketPlayers
                    )

                    // Zusammenfassung der Hybrid-Empfehlungen
                    HybridLineupSummary(
                        comparison: comparison,
                        teamPlayers: kickbaseManager.teamPlayers,
                        marketPlayers: kickbaseManager.marketPlayers
                    )
                }

                Spacer()
            }
            .padding(.horizontal)
        }
        .navigationTitle("Aufstellung optimieren")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct LineupDetailView: View {
    let lineup: OptimalLineupResult
    let teamPlayers: [TeamPlayer]
    let marketPlayers: [MarketPlayer]

    var body: some View {
        VStack(spacing: 16) {
            // Header mit Formation und Scores
            VStack(spacing: 8) {
                Text(lineup.formationName)
                    .font(.title2)
                    .fontWeight(.bold)

                HStack(spacing: 20) {
                    VStack(alignment: .leading) {
                        Text("Gesamtbewertung")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(String(format: "%.1f", lineup.totalLineupScore))
                            .font(.title3)
                            .fontWeight(.bold)
                    }

                    VStack(alignment: .leading) {
                        Text("Ø pro Spieler")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(String(format: "%.1f", lineup.averagePlayerScore))
                            .font(.title3)
                            .fontWeight(.bold)
                    }

                    if lineup.isHybridWithMarketPlayers {
                        VStack(alignment: .leading) {
                            Text("Investment")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Text("€\(lineup.totalMarketCost / 1_000_000)M")
                                .font(.title3)
                                .fontWeight(.bold)
                                .foregroundColor(.blue)
                        }
                    }

                    Spacer()
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(8)
            }

            // Aufstellung visualisiert
            VStack(spacing: 12) {
                Text("Aufstellung")
                    .font(.headline)
                    .frame(maxWidth: .infinity, alignment: .leading)

                // Gruppiere Slots nach Position
                #if !SKIP
                    let slotsByPosition = Dictionary(grouping: lineup.slots) { $0.positionType }
                #else
                    var slotsByPosition: [Int: [LineupSlot]] = [:]
                    for slot in lineup.slots {
                        var list = slotsByPosition[slot.positionType] ?? []
                        list.append(slot)
                        slotsByPosition[slot.positionType] = list
                    }
                #endif
                let positions = [1, 2, 3, 4].filter { slotsByPosition[$0] != nil }

                ForEach(positions, id: \.self) { position in
                    let positionName = positionName(position)
                    let slots = slotsByPosition[position] ?? []

                    VStack(alignment: .leading, spacing: 8) {
                        Text(positionName)
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.secondary)

                        VStack(spacing: 6) {
                            ForEach(slots, id: \.id) { slot in
                                LineupSlotRowView(
                                    slot: slot,
                                    teamPlayers: teamPlayers,
                                    marketPlayers: marketPlayers
                                )
                            }
                        }
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }

    private func positionName(_ position: Int) -> String {
        switch position {
        case 1: return "Torwart (TW)"
        case 2: return "Abwehr (ABW)"
        case 3: return "Mittelfeld (MF)"
        case 4: return "Stürmer (ST)"
        default: return "Unbekannt"
        }
    }
}

struct LineupSlotRowView: View {
    let slot: LineupSlot
    let teamPlayers: [TeamPlayer]
    let marketPlayers: [MarketPlayer]
    @EnvironmentObject var ligainsiderService: LigainsiderService

    var body: some View {
        HStack(spacing: 12) {
            // Score-Indikator
            VStack(alignment: .center, spacing: 2) {
                Text(String(format: "%.0f", slot.slotScore))
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .frame(width: 40, height: 40)
                    .background(scoreColor(slot.slotScore))
                    .cornerRadius(8)
            }

            // Spieler-Info
            if let marketId = slot.recommendedMarketPlayerId,
                let marketPlayer = marketPlayers.first(where: { $0.id == marketId })
            {
                // Markt-Spieler
                VStack(alignment: .leading, spacing: 2) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: 4) {
                                Text(marketPlayer.fullName)
                                    .font(.subheadline)
                                    .fontWeight(.semibold)

                                // Ligainsider Status Icon
                                if !ligainsiderService.matches.isEmpty {
                                    let status = ligainsiderService.getPlayerStatus(
                                        firstName: marketPlayer.firstName,
                                        lastName: marketPlayer.lastName)
                                    if status != .out {
                                        Image(systemName: ligainsiderService.getIcon(for: status))
                                            .foregroundColor(
                                                Color(ligainsiderService.getColor(for: status))
                                            )
                                            .font(.caption2)
                                    }
                                }
                            }

                            Text(marketPlayer.fullTeamName)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }

                        Spacer()

                        // Badge: Markt
                        Text("Markt")
                            .font(.caption2)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.blue)
                            .cornerRadius(4)
                    }

                    HStack {
                        Text("€\(marketPlayer.price / 1_000_000)M")
                            .font(.caption)
                            .foregroundColor(.green)

                        Spacer()

                        Text("\(marketPlayer.averagePoints, specifier: "%.1f") Ø")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            } else if let ownId = slot.ownedPlayerId,
                let ownPlayer = teamPlayers.first(where: { $0.id == ownId })
            {
                // Eigener Spieler
                VStack(alignment: .leading, spacing: 2) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: 4) {
                                Text(ownPlayer.fullName)
                                    .font(.subheadline)
                                    .fontWeight(.semibold)

                                // Ligainsider Status Icon
                                if !ligainsiderService.matches.isEmpty {
                                    let status = ligainsiderService.getPlayerStatus(
                                        firstName: ownPlayer.firstName, lastName: ownPlayer.lastName
                                    )
                                    if status != .out {
                                        Image(systemName: ligainsiderService.getIcon(for: status))
                                            .foregroundColor(
                                                Color(ligainsiderService.getColor(for: status))
                                            )
                                            .font(.caption2)
                                    }
                                }
                            }

                            Text(ownPlayer.fullTeamName)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }

                        Spacer()

                        // Badge: Team
                        Text("Team")
                            .font(.caption2)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.green)
                            .cornerRadius(4)
                    }

                    HStack {
                        Text("€\(ownPlayer.marketValue / 1_000_000)M")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        Spacer()

                        Text("\(ownPlayer.averagePoints, specifier: "%.1f") Ø")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            } else {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Keine Empfehlung")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.secondary)

                    Text("Nicht genug Spieler für diese Position")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            Spacer()
        }
        .padding(.vertical, 8)
        .padding(.horizontal, 10)
        .background(Color(.systemBackground))
        .cornerRadius(8)
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(
                    slot.hasBetterMarketOption ? Color.blue.opacity(0.3) : Color(.systemGray5),
                    lineWidth: 1)
        )
    }

    private func scoreColor(_ score: Double) -> Color {
        if score >= 16 {
            return .green
        } else if score >= 12 {
            return .blue
        } else if score >= 8 {
            return .orange
        } else {
            return .red
        }
    }
}

struct HybridLineupSummary: View {
    let comparison: LineupComparison
    let teamPlayers: [TeamPlayer]
    let marketPlayers: [MarketPlayer]

    var body: some View {
        guard let hybridLineup = comparison.hybridLineup else { return AnyView(EmptyView()) }

        return AnyView(
            VStack(spacing: 16) {
                Text("Hybrid-Aufstellung Übersicht")
                    .font(.headline)
                    .frame(maxWidth: .infinity, alignment: .leading)

                // Verbesserungen
                VStack(spacing: 12) {
                    LineupInfoRow(
                        label: "Leistungsverbesserung",
                        value: String(
                            format: "+%.1f Punkte/Spiel", comparison.performanceGainWithHybrid),
                        valueColor: .green
                    )

                    LineupInfoRow(
                        label: "Benötigte Investition",
                        value: "€\(comparison.totalInvestmentNeeded / 1_000_000)M",
                        valueColor: .blue
                    )

                    LineupInfoRow(
                        label: "Markt-Spieler zum kaufen",
                        value: "\(hybridLineup.marketPlayerCount) Spieler",
                        valueColor: .orange
                    )
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(8)

                // Liste der empfohlenen Markt-Spieler
                if !hybridLineup.marketPlayersNeeded.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Zu kaufende Spieler")
                            .font(.subheadline)
                            .fontWeight(.semibold)

                        VStack(spacing: 6) {
                            ForEach(hybridLineup.marketPlayersNeeded, id: \.self) { playerId in
                                if let player = marketPlayers.first(where: { $0.id == playerId }) {
                                    HStack {
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(player.fullName)
                                                .font(.subheadline)
                                                .fontWeight(.semibold)

                                            Text(player.fullTeamName)
                                                .font(.caption)
                                                .foregroundColor(.secondary)
                                        }

                                        Spacer()

                                        Text("€\(player.price / 1_000_000)M")
                                            .font(.subheadline)
                                            .fontWeight(.semibold)
                                            .foregroundColor(.green)
                                    }
                                    .padding(8)
                                    .background(Color(.systemBackground))
                                    .cornerRadius(6)
                                }
                            }
                        }
                    }
                }

                // Empfehlung-Button
                VStack(spacing: 8) {
                    Text(
                        "Diese Aufstellung bietet eine bessere Gesamtleistung durch strategische Marktzukäufe."
                    )
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(3)

                    HStack(spacing: 12) {
                        Image(systemName: "lightbulb.fill")
                            .foregroundColor(.yellow)

                        Text("Gesamtbudget nach Verkäufen prüfen")
                            .font(.caption)
                            .fontWeight(.semibold)

                        Spacer()
                    }
                    .padding(10)
                    .background(Color.yellow.opacity(0.1))
                    .cornerRadius(6)
                }
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(12)
        )
    }
}

struct LineupInfoRow: View {
    let label: String
    let value: String
    let valueColor: Color

    var body: some View {
        HStack {
            Text(label)
                .font(.subheadline)
                .foregroundColor(.secondary)

            Spacer()

            Text(value)
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(valueColor)
        }
    }
}

#Preview {
    let sampleSlots = [
        LineupSlot(slotIndex: 0, positionType: 1, ownedPlayerId: "1", slotScore: 18.5),
        LineupSlot(slotIndex: 1, positionType: 2, ownedPlayerId: "2", slotScore: 16.2),
    ]

    let sampleLineup = OptimalLineupResult(
        slots: sampleSlots,
        formationName: "4-2-3-1",
        totalLineupScore: 145.0,
        isHybridWithMarketPlayers: false,
        averagePlayerScore: 14.5
    )

    let hybridSlots = [
        LineupSlot(slotIndex: 0, positionType: 1, ownedPlayerId: "1", slotScore: 18.5),
        LineupSlot(slotIndex: 1, positionType: 2, recommendedMarketPlayerId: "m1", slotScore: 17.2),
    ]

    let hybridLineup = OptimalLineupResult(
        slots: hybridSlots,
        formationName: "4-2-3-1",
        totalLineupScore: 152.0,
        isHybridWithMarketPlayers: true,
        marketPlayersNeeded: ["m1"],
        totalMarketCost: 25_000_000,
        averagePlayerScore: 15.2
    )

    let comparison = LineupComparison(
        teamOnlyLineup: sampleLineup,
        hybridLineup: hybridLineup
    )

    return NavigationView {
        LineupComparisonView(comparison: comparison)
            .environmentObject(KickbaseManager())
    }
}
