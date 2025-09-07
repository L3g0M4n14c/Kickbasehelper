import SwiftUI

struct PlayerDetailView: View {
    let player: TeamPlayer
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    // Header mit Spielerfoto und Namen
                    PlayerHeaderSection(player: player)
                    
                    // Status und Team Information
                    PlayerInfoSection(player: player)
                    
                    // Statistiken
                    PlayerStatsSection(player: player)
                    
                    // Marktwert Information
                    PlayerMarketValueSection(player: player)
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

// MARK: - Header Section
struct PlayerHeaderSection: View {
    let player: TeamPlayer
    
    var body: some View {
        VStack(spacing: 16) {
            // Profilbild (Placeholder)
            AsyncImage(url: URL(string: player.profileBigUrl)) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } placeholder: {
                Image(systemName: "person.circle.fill")
                    .font(.system(size: 80))
                    .foregroundColor(.gray)
            }
            .frame(width: 100, height: 100)
            .clipShape(Circle())
            .overlay(
                Circle()
                    .stroke(Color.gray.opacity(0.3), lineWidth: 2)
            )
            
            // Name und Position
            VStack(spacing: 8) {
                Text(player.fullName)
                    .font(.title)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                
                HStack(spacing: 12) {
                    // Position Badge
                    Text(player.positionName)
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(positionColor(player.position))
                        .cornerRadius(12)
                    
                    // Trikotnummer
                    Text("#\(player.number)")
                        .font(.headline)
                        .fontWeight(.semibold)
                        .foregroundColor(.secondary)
                }
            }
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

// MARK: - Info Section
struct PlayerInfoSection: View {
    let player: TeamPlayer
    
    var body: some View {
        VStack(spacing: 16) {
            // Status und Team
            HStack {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Team")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(player.fullTeamName)
                        .font(.headline)
                        .fontWeight(.medium)
                }
                
                Spacer()
                
                // Status Icons
                VStack(alignment: .trailing, spacing: 8) {
                    Text("Status")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    HStack(spacing: 8) {
                        if player.status == 1 {
                            Label("Verletzt", systemImage: "cross.circle.fill")
                                .foregroundColor(.red)
                                .font(.caption)
                        } else if player.status == 2 {
                            Label("Angeschlagen", systemImage: "pills.fill")
                                .foregroundColor(.orange)
                                .font(.caption)
                        } else {
                            Label("Fit", systemImage: "checkmark.circle.fill")
                                .foregroundColor(.green)
                                .font(.caption)
                        }
                    }
                }
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(12)
        }
    }
}

// MARK: - Stats Section
struct PlayerStatsSection: View {
    let player: TeamPlayer
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Statistiken")
                .font(.title2)
                .fontWeight(.bold)
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 16) {
                StatDetailCard(
                    title: "Durchschnittspunkte",
                    value: String(format: "%.1f", player.averagePoints),
                    icon: "star.fill",
                    color: .orange
                )
                StatDetailCard(
                    title: "Gesamtpunkte",
                    value: "\(player.totalPoints)",
                    icon: "sum",
                    color: .blue
                )
                StatDetailCard(
                    title: "Position",
                    value: player.positionName,
                    icon: "location.fill",
                    color: .purple
                )
                StatDetailCard(
                    title: "Trikotnummer",
                    value: "#\(player.number)",
                    icon: "number",
                    color: .indigo
                )
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

// MARK: - Market Value Section
struct PlayerMarketValueSection: View {
    let player: TeamPlayer
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Marktwert")
                .font(.title2)
                .fontWeight(.bold)
            
            VStack(spacing: 12) {
                // Aktueller Marktwert
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Aktueller Marktwert")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("€\(formatValue(player.marketValue))")
                            .font(.title)
                            .fontWeight(.bold)
                    }
                    
                    Spacer()
                    
                    // Trend Icon
                    if player.marketValueTrend != 0 {
                        VStack(alignment: .trailing, spacing: 4) {
                            Text("Trend")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            HStack(spacing: 4) {
                                Image(systemName: player.marketValueTrend >= 0 ? "arrow.up" : "arrow.down")
                                    .foregroundColor(player.marketValueTrend >= 0 ? .green : .red)
                                Text("€\(formatValue(abs(player.marketValueTrend)))")
                                    .foregroundColor(player.marketValueTrend >= 0 ? .green : .red)
                                    .fontWeight(.medium)
                            }
                        }
                    }
                }
                
                Divider()
                
                // Letzte Änderung
                if player.tfhmvt != 0 {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Letzte Änderung")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            HStack(spacing: 4) {
                                Image(systemName: player.tfhmvt >= 0 ? "arrow.up" : "arrow.down")
                                    .foregroundColor(player.tfhmvt >= 0 ? .green : .red)
                                Text("€\(formatValue(abs(player.tfhmvt)))")
                                    .foregroundColor(player.tfhmvt >= 0 ? .green : .red)
                                    .fontWeight(.medium)
                            }
                        }
                        Spacer()
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
    
    private func formatValue(_ value: Int) -> String {
        if value >= 1000000 {
            return String(format: "%.1fM", Double(value) / 1000000)
        } else if value >= 1000 {
            return String(format: "%.0fk", Double(value) / 1000)
        } else {
            return "\(value)"
        }
    }
}

// MARK: - Stat Detail Card
struct StatDetailCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(color)
            
            Text(value)
                .font(.title2)
                .fontWeight(.bold)
            
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(height: 100)
        .frame(maxWidth: .infinity)
        .background(Color(.systemBackground))
        .cornerRadius(8)
    }
}

// MARK: - Market Player Detail View
struct MarketPlayerDetailView: View {
    let player: MarketPlayer
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    // Header für Marktplayer
                    MarketPlayerHeaderSection(player: player)
                    
                    // Info Section für Marktplayer
                    MarketPlayerInfoSection(player: player)
                    
                    // Verkaufsinformationen - ZUERST anzeigen da es marktspezifisch ist
                    MarketPlayerSaleSection(player: player)
                    
                    // Statistiken für Marktplayer
                    MarketPlayerStatsSection(player: player)
                    
                    // Marktwert-Vergleich
                    MarketPlayerValueComparisonSection(player: player)
                }
                .padding()
            }
            .navigationTitle("Marktplatz-Details")
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

// MARK: - Market Player Header
struct MarketPlayerHeaderSection: View {
    let player: MarketPlayer
    
    var body: some View {
        VStack(spacing: 16) {
            // Profilbild (Placeholder)
            AsyncImage(url: URL(string: player.profileBigUrl)) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } placeholder: {
                Image(systemName: "person.circle.fill")
                    .font(.system(size: 80))
                    .foregroundColor(.gray)
            }
            .frame(width: 100, height: 100)
            .clipShape(Circle())
            .overlay(
                Circle()
                    .stroke(Color.gray.opacity(0.3), lineWidth: 2)
            )
            
            // Name und Position
            VStack(spacing: 8) {
                Text(player.fullName)
                    .font(.title)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                
                HStack(spacing: 12) {
                    // Position Badge
                    Text(player.positionName)
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(positionColor(player.position))
                        .cornerRadius(12)
                    
                    // Trikotnummer
                    Text("#\(player.number)")
                        .font(.headline)
                        .fontWeight(.semibold)
                        .foregroundColor(.secondary)
                }
            }
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

// MARK: - Market Player Info Section
struct MarketPlayerInfoSection: View {
    let player: MarketPlayer
    
    var body: some View {
        VStack(spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Team")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(player.fullTeamName)
                        .font(.headline)
                        .fontWeight(.medium)
                }
                
                Spacer()
                
                // Status Icons
                VStack(alignment: .trailing, spacing: 8) {
                    Text("Status")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    HStack(spacing: 8) {
                        if player.status == 1 {
                            Label("Verletzt", systemImage: "cross.circle.fill")
                                .foregroundColor(.red)
                                .font(.caption)
                        } else if player.status == 2 {
                            Label("Angeschlagen", systemImage: "pills.fill")
                                .foregroundColor(.orange)
                                .font(.caption)
                        } else {
                            Label("Fit", systemImage: "checkmark.circle.fill")
                                .foregroundColor(.green)
                                .font(.caption)
                        }
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

// MARK: - Market Player Stats Section
struct MarketPlayerStatsSection: View {
    let player: MarketPlayer
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Statistiken")
                .font(.title2)
                .fontWeight(.bold)
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 16) {
                StatDetailCard(
                    title: "Durchschnittspunkte",
                    value: String(format: "%.1f", player.averagePoints),
                    icon: "star.fill",
                    color: .orange
                )
                StatDetailCard(
                    title: "Gesamtpunkte",
                    value: "\(player.totalPoints)",
                    icon: "sum",
                    color: .blue
                )
                StatDetailCard(
                    title: "Position",
                    value: player.positionName,
                    icon: "location.fill",
                    color: .purple
                )
                StatDetailCard(
                    title: "Trikotnummer",
                    value: "#\(player.number)",
                    icon: "number",
                    color: .indigo
                )
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

// MARK: - Market Player Sale Section
struct MarketPlayerSaleSection: View {
    let player: MarketPlayer
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Verkaufsinformationen")
                .font(.title2)
                .fontWeight(.bold)
            
            VStack(spacing: 12) {
                // Verkaufspreis
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Verkaufspreis")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("€\(formatValue(player.price))")
                            .font(.title)
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing, spacing: 4) {
                        Text("Angebote")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("\(player.offers)")
                            .font(.headline)
                            .fontWeight(.semibold)
                    }
                }
                
                Divider()
                
                // Verkäufer und Marktwert
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Verkäufer")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(player.seller.name)
                            .font(.headline)
                            .fontWeight(.medium)
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing, spacing: 4) {
                        Text("Marktwert")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("€\(formatValue(player.marketValue))")
                            .font(.headline)
                            .fontWeight(.semibold)
                    }
                }
                
                Divider()
                
                // Ablaufdatum
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Läuft ab")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(formatExpiry(player.expiry))
                            .font(.subheadline)
                            .fontWeight(.medium)
                    }
                    Spacer()
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
    
    private func formatValue(_ value: Int) -> String {
        if value >= 1000000 {
            return String(format: "%.1fM", Double(value) / 1000000)
        } else if value >= 1000 {
            return String(format: "%.0fk", Double(value) / 1000)
        } else {
            return "\(value)"
        }
    }
    
    private func formatExpiry(_ expiry: String) -> String {
        // Einfache Formatierung - kann je nach API-Format angepasst werden
        return expiry.isEmpty ? "Unbekannt" : expiry
    }
}

// MARK: - Market Player Value Comparison Section
struct MarketPlayerValueComparisonSection: View {
    let player: MarketPlayer
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Preis-Marktwert-Vergleich")
                .font(.title2)
                .fontWeight(.bold)
            
            VStack(spacing: 12) {
                // Preisvergleich
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Verkaufspreis")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("€\(formatValue(player.price))")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .center, spacing: 4) {
                        Text("vs.")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        let difference = player.price - player.marketValue
                        let isGoodDeal = difference < 0
                        
                        HStack(spacing: 4) {
                            Image(systemName: isGoodDeal ? "arrow.down.circle.fill" : "arrow.up.circle.fill")
                                .foregroundColor(isGoodDeal ? .green : .red)
                            Text("€\(formatValue(abs(difference)))")
                                .font(.caption)
                                .foregroundColor(isGoodDeal ? .green : .red)
                                .fontWeight(.medium)
                        }
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing, spacing: 4) {
                        Text("Marktwert")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("€\(formatValue(player.marketValue))")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.blue)
                    }
                }
                
                // Bewertung
                let priceDifferencePercent = Double(player.price - player.marketValue) / Double(player.marketValue) * 100
                
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Bewertung")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        if priceDifferencePercent < -10 {
                            Label("Sehr günstiger Deal!", systemImage: "star.fill")
                                .foregroundColor(.green)
                                .font(.subheadline)
                                .fontWeight(.semibold)
                        } else if priceDifferencePercent < 0 {
                            Label("Günstiger Preis", systemImage: "thumbs.up.fill")
                                .foregroundColor(.green)
                                .font(.subheadline)
                        } else if priceDifferencePercent < 10 {
                            Label("Fairer Preis", systemImage: "equal.circle.fill")
                                .foregroundColor(.orange)
                                .font(.subheadline)
                        } else {
                            Label("Teurer als Marktwert", systemImage: "exclamationmark.triangle.fill")
                                .foregroundColor(.red)
                                .font(.subheadline)
                        }
                    }
                    Spacer()
                }
                
                // Trend-Analyse
                if player.marketValueTrend != 0 {
                    Divider()
                    
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Marktwert-Trend")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            
                            HStack(spacing: 4) {
                                Image(systemName: player.marketValueTrend >= 0 ? "chart.line.uptrend.xyaxis" : "chart.line.downtrend.xyaxis")
                                    .foregroundColor(player.marketValueTrend >= 0 ? .green : .red)
                                
                                if player.marketValueTrend > 0 {
                                    Text("Steigend (+€\(formatValue(player.marketValueTrend)))")
                                        .foregroundColor(.green)
                                        .fontWeight(.medium)
                                } else {
                                    Text("Fallend (€\(formatValue(abs(player.marketValueTrend))))")
                                        .foregroundColor(.red)
                                        .fontWeight(.medium)
                                }
                            }
                        }
                        Spacer()
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
    
    private func formatValue(_ value: Int) -> String {
        if value >= 1000000 {
            return String(format: "%.1fM", Double(value) / 1000000)
        } else if value >= 1000 {
            return String(format: "%.0fk", Double(value) / 1000)
        } else {
            return "\(value)"
        }
    }
}

#Preview {
    PlayerDetailView(player: TeamPlayer(
        id: "preview-1",
        firstName: "Max",
        lastName: "Mustermann",
        profileBigUrl: "",
        teamName: "FC Demo",
        teamId: "1",
        position: 4,
        number: 9,
        averagePoints: 5.2,
        totalPoints: 78,
        marketValue: 15000000,
        marketValueTrend: 500000,
        tfhmvt: 250000,
        stl: 0,
        status: 0,
        userOwnsPlayer: true
    ))
}

#Preview("Market Player Detail") {
    MarketPlayerDetailView(player: MarketPlayer(
        id: "preview-market-1",
        firstName: "Robert",
        lastName: "Lewandowski",
        profileBigUrl: "",
        teamName: "FC Barcelona",
        teamId: "1",
        position: 4,
        number: 9,
        averagePoints: 7.2,
        totalPoints: 108,
        marketValue: 25000000,
        marketValueTrend: 1000000,
        price: 23000000,
        expiry: "2025-09-15T18:00:00Z",
        offers: 3,
        seller: MarketSeller(id: "seller1", name: "FC Bayern Fan"),
        stl: 0,
        status: 0
    ))
}
