import KickbaseCore
import SwiftUI

@main
struct KickbasehelperApp: App {
    @StateObject private var authManager = AuthenticationManager()
    @StateObject private var backgroundTaskManager = BackgroundTaskManager.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authManager)
                .environmentObject(backgroundTaskManager)
                .task {
                    // Initialize background tasks on app launch
                    await MainActor.run {
                        backgroundTaskManager.registerBackgroundTasks()
                    }
                    _ = await backgroundTaskManager.requestNotificationPermission()
                    await MainActor.run {
                        backgroundTaskManager.scheduleBackgroundTask()
                    }
                }
        }
    }
}
