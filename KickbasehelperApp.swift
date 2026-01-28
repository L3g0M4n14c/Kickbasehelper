import KickbaseCore
import SwiftUI

@main
struct KickbasehelperApp: App {
    @StateObject private var authManager = AuthenticationManager()
    @StateObject private var backgroundTaskManager = BackgroundTaskManager.shared

    init() {
        // Register background tasks on app launch
        Task {
            await BackgroundTaskManager.shared.registerBackgroundTasks()
            await BackgroundTaskManager.shared.requestNotificationPermission()
            BackgroundTaskManager.shared.scheduleBackgroundTask()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authManager)
                .environmentObject(backgroundTaskManager)
        }
    }
}
