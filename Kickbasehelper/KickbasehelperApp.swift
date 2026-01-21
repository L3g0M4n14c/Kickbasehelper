//
//  KickbasehelperApp.swift
//  Kickbasehelper
//
//  Created by Marco Corro on 27.08.25.
//

import SwiftUI
import SwiftData

@main
struct KickbasehelperApp: App {
    var sharedModelContainer: ModelContainer = {
        let schema = Schema([
            Item.self,
        ])
        let modelConfiguration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)

        do {
            return try ModelContainer(for: schema, configurations: [modelConfiguration])
        } catch {
            fatalError("Could not create ModelContainer: \(error)")
        }
    }()
    
    // Globaler Service für Ligainsider
    @StateObject private var ligainsiderService = LigainsiderService()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(ligainsiderService) // Inject into environment
        }
        .modelContainer(sharedModelContainer)
    }
}
