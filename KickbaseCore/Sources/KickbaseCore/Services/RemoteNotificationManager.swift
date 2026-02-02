import Foundation

#if os(iOS)
    import UserNotifications
    import UIKit
#endif

/// Manager for handling remote (push) notifications
@MainActor
public class RemoteNotificationManager: NSObject, ObservableObject {
    public static let shared = RemoteNotificationManager()

    private let apiService: KickbaseAPIService
    private let userDefaultsKey = "device_token"

    @Published public var deviceToken: String?
    @Published public var lastPushReceived: Date?

    override private init() {
        self.apiService = KickbaseAPIService()
        super.init()
        loadStoredDeviceToken()
    }

    // MARK: - Device Token Management

    /// Request permission and register for remote notifications
    public func requestRemoteNotificationPermission() {
        #if os(iOS)
            UNUserNotificationCenter.current().requestAuthorization(options: [
                .alert, .sound, .badge,
            ]) { granted, error in
                DispatchQueue.main.async {
                    if granted {
                        UIApplication.shared.registerForRemoteNotifications()
                        print("✅ Remote notification permission granted")
                    } else if let error = error {
                        print(
                            "❌ Remote notification permission denied: \(error.localizedDescription)"
                        )
                    }
                }
            }
        #endif
    }

    /// Store device token when received from iOS
    public func storeDeviceToken(_ token: Data) {
        let tokenString = token.map { String(format: "%02.2hhx", $0) }.joined()
        self.deviceToken = tokenString
        UserDefaults.standard.set(tokenString, forKey: userDefaultsKey)

        print("🔑 Device token stored: \(tokenString.prefix(20))...")

        // Send to server (optional - only if authenticated)
        Task {
            await registerTokenWithBackend(tokenString)
        }
    }

    /// Load stored device token from UserDefaults
    private func loadStoredDeviceToken() {
        if let storedToken = UserDefaults.standard.string(forKey: userDefaultsKey) {
            self.deviceToken = storedToken
            print("🔑 Device token loaded from storage: \(storedToken.prefix(20))...")
        }
    }

    /// Register device token with backend
    private func registerTokenWithBackend(_ token: String) async {
        do {
            // This endpoint needs to be implemented in KickbaseAPIService
            try await apiService.registerDeviceToken(token)
            print("✅ Device token registered with backend")
        } catch {
            print("⚠️ Failed to register device token with backend: \(error.localizedDescription)")
            // Don't fail - token is still stored locally
        }
    }

    // MARK: - Remote Notification Handling

    /// Handle remote notification received in background
    #if os(iOS)
        public func handleRemoteNotification(
            userInfo: [AnyHashable: Any],
            completionHandler: @escaping (UIBackgroundFetchResult) -> Void
        ) {
            print("📬 Remote notification received")

            // Check if this is a silent push (content-available: 1)
            if isSilentPush(userInfo) {
                print("🔇 Silent push detected")
                handleSilentPush(userInfo: userInfo, completionHandler: completionHandler)
            } else {
                print("📢 Regular notification received")
                completionHandler(.noData)
            }
        }
    #endif

    /// Check if notification is a silent push
    private func isSilentPush(_ userInfo: [AnyHashable: Any]) -> Bool {
        guard let aps = userInfo["aps"] as? [String: Any],
            let contentAvailable = aps["content-available"] as? NSNumber
        else {
            return false
        }
        return contentAvailable.intValue == 1
    }

    /// Handle silent push notification
    #if os(iOS)
        private func handleSilentPush(
            userInfo: [AnyHashable: Any],
            completionHandler: @escaping (UIBackgroundFetchResult) -> Void
        ) {
            lastPushReceived = Date()

            // Extract bonus-related data if present
            let bonusId = userInfo["bonus_id"] as? String ?? "unknown"
            print("🎁 Processing bonus notification: \(bonusId)")

            // Delegate to BackgroundTaskManager for actual bonus collection
            Task {
                let success = await BackgroundTaskManager.shared.performBonusCollection()

                // Call completion handler within 30-second timeout
                completionHandler(success ? .newData : .failed)
            }
        }
    #endif
}

// MARK: - UIApplication Delegate Handlers

#if os(iOS)
    /// Helper methods to be called from AppDelegate
    extension RemoteNotificationManager {

        /// Call this from AppDelegate.application(_:didRegisterForRemoteNotificationsWithDeviceToken:)
        @MainActor
        public func application(
            _ application: UIApplication,
            didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
        ) {
            storeDeviceToken(deviceToken)
        }

        /// Call this from AppDelegate.application(_:didFailToRegisterForRemoteNotificationsWithError:)
        @MainActor
        public func application(
            _ application: UIApplication,
            didFailToRegisterForRemoteNotificationsWithError error: Error
        ) {
            print("❌ Failed to register for remote notifications: \(error.localizedDescription)")
        }

        /// Call this from AppDelegate.application(_:didReceiveRemoteNotification:fetchCompletionHandler:)
        @MainActor
        public func application(
            _ application: UIApplication,
            didReceiveRemoteNotification userInfo: [AnyHashable: Any],
            fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
        ) {
            handleRemoteNotification(userInfo: userInfo, completionHandler: completionHandler)
        }
    }
#endif
