import Foundation
#if os(iOS)
import BackgroundTasks
import UIKit
#endif

/// Manager for handling background tasks, specifically for daily bonus collection
@MainActor
public class BackgroundTaskManager: ObservableObject {
    public static let shared = BackgroundTaskManager()
    
    private let taskIdentifier = "com.kickbasehelper.bonuscollection"
    private let apiService: KickbaseAPIService
    
    @Published public var lastBonusCollectionDate: Date?
    @Published public var lastBonusCollectionSuccess: Bool = false
    @Published public var lastBonusCollectionError: String?
    
    private init() {
        self.apiService = KickbaseAPIService()
        loadLastCollectionDate()
    }
    
    /// Set the authentication token for background API calls
    public func setAuthToken(_ token: String) {
        apiService.setAuthToken(token)
    }
    
    /// Register the background task on app launch
    public func registerBackgroundTasks() {
        #if os(iOS)
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: taskIdentifier,
            using: nil
        ) { task in
            Task {
                await self.handleBonusCollection(task: task as! BGAppRefreshTask)
            }
        }
        print("✅ Background task registered: \(taskIdentifier)")
        #endif
    }
    
    /// Schedule the next background task execution
    public func scheduleBackgroundTask() {
        #if os(iOS)
        let request = BGAppRefreshTaskRequest(identifier: taskIdentifier)
        
        // Schedule for next day at 6:00 AM
        let calendar = Calendar.current
        var components = calendar.dateComponents([.year, .month, .day], from: Date())
        components.day! += 1
        components.hour = 6
        components.minute = 0
        
        if let nextDate = calendar.date(from: components) {
            request.earliestBeginDate = nextDate
        } else {
            // Fallback: schedule 24 hours from now
            request.earliestBeginDate = Date(timeIntervalSinceNow: 24 * 60 * 60)
        }
        
        do {
            try BGTaskScheduler.shared.submit(request)
            print("✅ Background task scheduled for: \(request.earliestBeginDate?.description ?? "unknown")")
        } catch {
            print("❌ Could not schedule background task: \(error)")
        }
        #endif
    }
    
    /// Handle the background task execution
    #if os(iOS)
    private func handleBonusCollection(task: BGAppRefreshTask) async {
        print("🎯 Background bonus collection started")
        
        // Schedule the next execution
        scheduleBackgroundTask()
        
        // Create a task to execute the bonus collection
        let bonusTask = Task {
            await performBonusCollection()
        }
        
        // Handle expiration
        task.expirationHandler = {
            print("⏰ Background task expired")
            bonusTask.cancel()
        }
        
        // Wait for completion
        let success = await bonusTask.value
        
        // Complete the task
        task.setTaskCompleted(success: success)
        print("✅ Background task completed with success: \(success)")
    }
    #endif
    
    /// Perform the actual bonus collection
    @discardableResult
    public func performBonusCollection() async -> Bool {
        print("💰 Attempting to collect daily bonus...")
        
        do {
            // Check if we already collected today
            if let lastDate = lastBonusCollectionDate,
               Calendar.current.isDateInToday(lastDate) {
                print("ℹ️ Bonus already collected today")
                return true
            }
            
            // Try to collect the bonus
            let response = try await apiService.collectBonus()
            
            // Update state
            lastBonusCollectionDate = Date()
            lastBonusCollectionSuccess = true
            lastBonusCollectionError = nil
            saveLastCollectionDate()
            
            print("✅ Bonus collected successfully: \(response)")
            
            // Send local notification
            await sendSuccessNotification()
            
            return true
        } catch {
            print("❌ Failed to collect bonus: \(error)")
            lastBonusCollectionSuccess = false
            lastBonusCollectionError = error.localizedDescription
            return false
        }
    }
    
    /// Send a local notification on successful bonus collection
    private func sendSuccessNotification() async {
        #if os(iOS)
        let content = UNMutableNotificationContent()
        content.title = "🎁 Bonus gesammelt!"
        content.body = "Dein täglicher Kickbase-Bonus wurde erfolgreich abgeholt."
        content.sound = .default
        
        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: nil
        )
        
        do {
            try await UNUserNotificationCenter.current().add(request)
            print("📱 Notification sent")
        } catch {
            print("❌ Could not send notification: \(error)")
        }
        #endif
    }
    
    /// Request notification permissions
    public func requestNotificationPermission() async -> Bool {
        #if os(iOS)
        do {
            let granted = try await UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .sound, .badge])
            print(granted ? "✅ Notification permission granted" : "❌ Notification permission denied")
            return granted
        } catch {
            print("❌ Error requesting notification permission: \(error)")
            return false
        }
        #else
        return true
        #endif
    }
    
    // MARK: - Persistence
    
    private func saveLastCollectionDate() {
        if let date = lastBonusCollectionDate {
            UserDefaults.standard.set(date, forKey: "lastBonusCollectionDate")
            UserDefaults.standard.set(lastBonusCollectionSuccess, forKey: "lastBonusCollectionSuccess")
            if let error = lastBonusCollectionError {
                UserDefaults.standard.set(error, forKey: "lastBonusCollectionError")
            }
        }
    }
    
    private func loadLastCollectionDate() {
        lastBonusCollectionDate = UserDefaults.standard.object(forKey: "lastBonusCollectionDate") as? Date
        lastBonusCollectionSuccess = UserDefaults.standard.bool(forKey: "lastBonusCollectionSuccess")
        lastBonusCollectionError = UserDefaults.standard.string(forKey: "lastBonusCollectionError")
    }
}
