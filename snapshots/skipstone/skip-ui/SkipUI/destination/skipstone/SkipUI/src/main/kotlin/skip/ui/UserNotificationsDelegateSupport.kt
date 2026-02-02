package skip.ui

import skip.lib.*

// Copyright 2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

@androidx.annotation.Keep
class UserNotificationCenterDelegateSupport: UNUserNotificationCenterDelegate, skip.lib.SwiftProjecting {
    internal val didReceive: (UNNotificationResponse, CompletionHandler) -> Unit
    internal val willPresent: (UNNotification, ValueCompletionHandler) -> Unit
    internal val openSettings: (UNNotification?) -> Unit

    constructor(didReceive: (UNNotificationResponse, CompletionHandler) -> Unit, willPresent: (UNNotification, ValueCompletionHandler) -> Unit, openSettings: (UNNotification?) -> Unit) {
        this.didReceive = didReceive
        this.willPresent = willPresent
        this.openSettings = openSettings
    }

    override suspend fun userNotificationCenter(center: UNUserNotificationCenter, didReceive: UNNotificationResponse): Unit = Async.run {
        val notification = didReceive
        kotlin.coroutines.suspendCoroutine { continuation ->
            val completionHandler = CompletionHandler { -> continuation.resumeWith(kotlin.Result.success(Unit)) }
            this.didReceive(notification, completionHandler)
        }
    }

    override suspend fun userNotificationCenter(center: UNUserNotificationCenter, willPresent: UNNotification): UNNotificationPresentationOptions = Async.run l@{
        val notification = willPresent
        return@l kotlin.coroutines.suspendCoroutine { continuation ->
            val completionHandler = ValueCompletionHandler { it ->
                val options = UNNotificationPresentationOptions(rawValue = it as Int)
                continuation.resumeWith(kotlin.Result.success(options))
            }
            this.willPresent(notification, completionHandler)
        }
    }

    override fun userNotificationCenter(center: UNUserNotificationCenter, openSettingsFor: UNNotification?) {
        val notification = openSettingsFor
        openSettings(notification)
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}


