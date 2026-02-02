import Foundation

#if os(iOS)
    import UIKit
    import KickbaseCore

    /// AppDelegate für iOS Push Notification Handling
    class AppDelegate: NSObject, UIApplicationDelegate {

        func application(
            _ application: UIApplication,
            didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? =
                nil
        ) -> Bool {
            print("🚀 App launching with AppDelegate")
            return true
        }

        // MARK: - Remote Notification Handling

        func application(
            _ application: UIApplication,
            didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
        ) {
            RemoteNotificationManager.shared.application(
                application,
                didRegisterForRemoteNotificationsWithDeviceToken: deviceToken
            )
        }

        func application(
            _ application: UIApplication,
            didFailToRegisterForRemoteNotificationsWithError error: Error
        ) {
            RemoteNotificationManager.shared.application(
                application,
                didFailToRegisterForRemoteNotificationsWithError: error
            )
        }

        func application(
            _ application: UIApplication,
            didReceiveRemoteNotification userInfo: [AnyHashable: Any],
            fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
        ) {
            RemoteNotificationManager.shared.application(
                application,
                didReceiveRemoteNotification: userInfo,
                fetchCompletionHandler: completionHandler
            )
        }
    }

#endif
