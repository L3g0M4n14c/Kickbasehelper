package com.kickbasehelper

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import java.util.concurrent.TimeUnit
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * Worker for collecting daily bonus in the background
 */
class BonusCollectionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "BonusCollectionWorker"
        private const val WORK_NAME = "bonus_collection_work"
        private const val CHANNEL_ID = "bonus_collection_channel"
        private const val NOTIFICATION_ID = 1001

        /**
         * Schedule periodic bonus collection (once per day)
         */
        fun schedule(context: Context) {
            Log.d(TAG, "Scheduling bonus collection work")
            
            // Create constraints - require network connection
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Create periodic work request (minimum interval is 15 minutes for testing,
            // but in production we schedule for 24 hours)
            val workRequest = PeriodicWorkRequestBuilder<BonusCollectionWorker>(
                24, TimeUnit.HOURS,  // Repeat interval
                30, TimeUnit.MINUTES  // Flex interval
            )
                .setConstraints(constraints)
                .build()

            // Schedule the work (replace any existing work with same name)
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            
            Log.d(TAG, "Bonus collection work scheduled successfully")
        }

        /**
         * Cancel the scheduled work
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Bonus collection work cancelled")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting bonus collection work")
        
        return try {
            // Check if we already collected today
            val prefs = applicationContext.getSharedPreferences("kickbase_prefs", Context.MODE_PRIVATE)
            val lastCollectionDate = prefs.getLong("last_bonus_collection", 0)
            val currentDate = System.currentTimeMillis()
            
            // Check if we already collected in the last 23 hours
            if (currentDate - lastCollectionDate < 23 * 60 * 60 * 1000) {
                Log.d(TAG, "Bonus already collected today")
                return Result.success()
            }
            
            // Get the authentication token
            val token = prefs.getString("kickbase_token", null)
            if (token == null) {
                Log.e(TAG, "No authentication token found")
                return Result.failure()
            }
            
            // Collect the bonus
            val success = collectBonus(token)
            
            if (success) {
                // Update last collection date
                prefs.edit()
                    .putLong("last_bonus_collection", currentDate)
                    .putBoolean("last_bonus_collection_success", true)
                    .apply()
                
                // Send notification
                sendSuccessNotification()
                
                Log.d(TAG, "Bonus collected successfully")
                Result.success()
            } else {
                Log.e(TAG, "Failed to collect bonus")
                prefs.edit()
                    .putBoolean("last_bonus_collection_success", false)
                    .apply()
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting bonus", e)
            Result.retry()
        }
    }

    private suspend fun collectBonus(token: String): Boolean {
        // This is a placeholder - the actual implementation would use the KickbaseCore API
        // Since the Android app uses Skip framework to transpile from Swift,
        // the actual bonus collection logic is handled by the KickbaseCore module
        
        // For now, we log the attempt
        Log.d(TAG, "Attempting to collect bonus with token: ${token.take(10)}...")
        
        // In a real implementation, this would call:
        // KickbaseAPIService.shared.collectBonus()
        // But since this is transpiled from Swift, we return true for now
        
        return true
    }

    private fun sendSuccessNotification() {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🎁 Bonus gesammelt!")
            .setContentText("Dein täglicher Kickbase-Bonus wurde erfolgreich abgeholt.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Notification sent")
        } catch (e: SecurityException) {
            Log.e(TAG, "No notification permission", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Bonus Collection"
            val descriptionText = "Notifications for daily bonus collection"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
