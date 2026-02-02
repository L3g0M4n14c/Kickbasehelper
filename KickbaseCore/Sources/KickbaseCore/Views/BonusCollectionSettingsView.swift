import SwiftUI

/// View to display and manage bonus collection settings
public struct BonusCollectionSettingsView: View {
    @EnvironmentObject private var backgroundTaskManager: BackgroundTaskManager
    @State private var isCollecting = false

    public init() {}

    public var body: some View {
        Form {
            Section(header: Text("Täglicher Bonus").font(.headline)) {
                VStack(alignment: .leading, spacing: 10) {
                    Text("Automatisches Sammeln")
                        .font(.subheadline)
                        .fontWeight(.semibold)

                    Text(
                        "Die App sammelt automatisch jeden Tag deinen Kickbase-Bonus im Hintergrund."
                    )
                    .font(.caption)
                    .foregroundColor(.secondary)
                }
                .padding(.vertical, 5)
            }

            Section(header: Text("Status").font(.headline)) {
                HStack {
                    Text("Letztes Sammeln")
                    Spacer()
                    if let date = backgroundTaskManager.lastBonusCollectionDate {
                        Text(formatDate(date))
                            .foregroundColor(.secondary)
                    } else {
                        Text("Noch nicht gesammelt")
                            .foregroundColor(.secondary)
                    }
                }

                HStack {
                    Text("Status")
                    Spacer()
                    if let lastDate = backgroundTaskManager.lastBonusCollectionDate {
                        if backgroundTaskManager.lastBonusCollectionSuccess {
                            Label("Erfolgreich", systemImage: "checkmark.circle.fill")
                                .foregroundColor(.green)
                        } else {
                            Label("Fehlgeschlagen", systemImage: "xmark.circle.fill")
                                .foregroundColor(.red)
                        }
                    } else {
                        Text("Ausstehend")
                            .foregroundColor(.orange)
                    }
                }

                if let error = backgroundTaskManager.lastBonusCollectionError {
                    VStack(alignment: .leading, spacing: 5) {
                        Text("Fehlerdetails")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(error)
                            .font(.caption)
                            .foregroundColor(.red)
                    }
                    .padding(.vertical, 5)
                }
            }

            Section {
                Button(action: {
                    Task {
                        isCollecting = true
                        await backgroundTaskManager.performBonusCollection()
                        isCollecting = false
                    }
                }) {
                    HStack {
                        if isCollecting {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle())
                        } else {
                            Image(systemName: "gift.fill")
                        }
                        Text("Jetzt sammeln")
                    }
                }
                .disabled(isCollecting || isCollectedToday())

                if isCollectedToday() {
                    Text("Heute bereits gesammelt")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            Section(header: Text("Information").font(.headline)) {
                VStack(alignment: .leading, spacing: 10) {
                    InfoIconRow(
                        icon: "clock.fill",
                        title: "Zeitplan",
                        description: "Täglich um 6:00 Uhr"
                    )

                    InfoIconRow(
                        icon: "bolt.fill",
                        title: "Batterieverbrauch",
                        description: "< 1% pro Tag"
                    )

                    InfoIconRow(
                        icon: "bell.fill",
                        title: "Benachrichtigungen",
                        description: "Bei erfolgreicher Sammlung"
                    )
                }
            }
        }
        .navigationTitle("Bonus-Sammlung")
        #if !os(macOS)
            .navigationBarTitleDisplayMode(.inline)
        #endif
    }

    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        formatter.timeStyle = .short
        formatter.locale = Locale(identifier: "de_DE")
        return formatter.string(from: date)
    }

    private func isCollectedToday() -> Bool {
        guard let lastDate = backgroundTaskManager.lastBonusCollectionDate else {
            return false
        }
        return Calendar.current.isDateInToday(lastDate)
    }
}

/// Helper view for displaying info rows (icon)
public struct InfoIconRow: View {
    let icon: String
    let title: String
    let description: String

    public var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(.blue)
                .frame(width: 24)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

#Preview {
    NavigationView {
        BonusCollectionSettingsView()
            .environmentObject(BackgroundTaskManager.shared)
    }
}
