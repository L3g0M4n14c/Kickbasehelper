import KickbaseCore
import SwiftUI

@main
struct KickbasehelperApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @StateObject private var authManager = AuthenticationManager()
    @StateObject private var backgroundTaskManager = BackgroundTaskManager.shared
    @StateObject private var remoteNotificationManager = RemoteNotificationManager.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authManager)
                .environmentObject(backgroundTaskManager)
                .environmentObject(remoteNotificationManager)
                .task {
                    // Initialize background tasks on app launch
                    await MainActor.run {
                        backgroundTaskManager.registerBackgroundTasks()
                    }
                    _ = await backgroundTaskManager.requestNotificationPermission()
                    await MainActor.run {
                        backgroundTaskManager.scheduleBackgroundTask()
                    }

                    // Initialize remote notifications
                    await MainActor.run {
                        remoteNotificationManager.requestRemoteNotificationPermission()
                    }
                }
        }
    }
}
