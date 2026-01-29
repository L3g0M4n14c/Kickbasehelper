package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*

// note that this needs AndroidManifest.xml permission:
// <uses-permission android:name="android.permission.VIBRATE"/>
internal val systemVibratorService = createSystemVibratorService()

private fun createSystemVibratorService(): android.os.Vibrator? {
    val context = ProcessInfo.processInfo.androidContext.sref() // Android-specific extension to get the global Context

    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
        logger.log("vibratorManager: return null due to Android version too old (${android.os.Build.VERSION.SDK_INT})")
        return null
    }
    val vibratorManager_0 = (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager).sref()
    if (vibratorManager_0 == null) {
        logger.log("vibratorManager: returned null")
        return null
    }

    logger.log("vibratorManager: ${vibratorManager_0}")

    // https://developer.android.com/reference/android/os/Vibrator
    return vibratorManager_0.getDefaultVibrator()
}

interface UIFeedbackGenerator {
}

/// UIImpactFeedbackGenerator is used to give user feedback when an impact between UI elements occurs
open class UIImpactFeedbackGenerator: UIFeedbackGenerator {
    private val style: UIImpactFeedbackGenerator.FeedbackStyle

    constructor() {
        this.style = UIImpactFeedbackGenerator.FeedbackStyle.medium
    }

    constructor(style: UIImpactFeedbackGenerator.FeedbackStyle) {
        this.style = style
    }

    /// call when your UI element impacts something else
    open fun impactOccurred() {
        systemVibratorService?.vibrate(style.vibrationEffect)
    }

    /// call when your UI element impacts something else with a specific intensity [0.0, 1.0]
    open fun impactOccurred(intensity: Double) {
        if (intensity <= 0.0) {
            return
        }

        val effect = android.os.VibrationEffect.startComposition()
            .addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, Float(intensity), 0)
            .compose()
        systemVibratorService?.vibrate(effect)
    }

    open fun impactOccurred(intensity: Double, at: CGPoint) {
        val location = at
        impactOccurred(intensity = intensity)
    }

    enum class FeedbackStyle(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
        light(0),
        medium(1),
        heavy(2),

        soft(3),
        rigid(4);

        internal val vibrationEffect: android.os.VibrationEffect
            get() {
                when (this) {
                    UIImpactFeedbackGenerator.FeedbackStyle.light -> return android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK)
                    UIImpactFeedbackGenerator.FeedbackStyle.medium -> return android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK)
                    UIImpactFeedbackGenerator.FeedbackStyle.heavy -> return android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_HEAVY_CLICK)
                    UIImpactFeedbackGenerator.FeedbackStyle.soft -> return android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK)
                    UIImpactFeedbackGenerator.FeedbackStyle.rigid -> return android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK)
                }
            }

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: Int): UIImpactFeedbackGenerator.FeedbackStyle? {
                return when (rawValue) {
                    0 -> FeedbackStyle.light
                    1 -> FeedbackStyle.medium
                    2 -> FeedbackStyle.heavy
                    3 -> FeedbackStyle.soft
                    4 -> FeedbackStyle.rigid
                    else -> null
                }
            }
        }
    }

    @androidx.annotation.Keep
    companion object: CompanionClass() {

        override fun FeedbackStyle(rawValue: Int): UIImpactFeedbackGenerator.FeedbackStyle? = FeedbackStyle.init(rawValue = rawValue)
    }
    open class CompanionClass {
        open fun FeedbackStyle(rawValue: Int): UIImpactFeedbackGenerator.FeedbackStyle? = UIImpactFeedbackGenerator.FeedbackStyle(rawValue = rawValue)
    }
}

/// UINotificationFeedbackGenerator is used to give user feedback when an notification is displayed
open class UINotificationFeedbackGenerator: UIFeedbackGenerator {

    constructor() {
    }

    /// call when a notification is displayed, passing the corresponding type
    open fun notificationOccurred(notificationType: UINotificationFeedbackGenerator.FeedbackType) {
        // amplitude parameter: “The strength of the vibration. This must be a value between 1 and 255”
        systemVibratorService?.vibrate(notificationType.vibrationEffect)
    }

    /// call when a notification is displayed, passing the corresponding type
    open fun notificationOccurred(notificationType: UINotificationFeedbackGenerator.FeedbackType, at: CGPoint) {
        val location = at
        notificationOccurred(notificationType)
    }

    enum class FeedbackType(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
        success(0),
        warning(1),
        error(2);

        internal val vibrationEffect: android.os.VibrationEffect
            get() {
                when (this) {
                    UINotificationFeedbackGenerator.FeedbackType.success -> {
                        return android.os.VibrationEffect.startComposition()
                            .addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 0)
                            .addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 150)
                            .compose()
                    }
                    UINotificationFeedbackGenerator.FeedbackType.warning -> {
                        return android.os.VibrationEffect.startComposition()
                            .addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 0)
                            .addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 150)
                            .compose()
                    }
                    UINotificationFeedbackGenerator.FeedbackType.error -> {
                        return android.os.VibrationEffect.startComposition()
                            .addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, 0.5f, 0)
                            .addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, 0.7f, 100)
                            .addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, 0.9f, 150)
                            .compose()
                    }
                }
            }

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: Int): UINotificationFeedbackGenerator.FeedbackType? {
                return when (rawValue) {
                    0 -> FeedbackType.success
                    1 -> FeedbackType.warning
                    2 -> FeedbackType.error
                    else -> null
                }
            }
        }
    }

    @androidx.annotation.Keep
    companion object: CompanionClass() {

        override fun FeedbackType(rawValue: Int): UINotificationFeedbackGenerator.FeedbackType? = FeedbackType.init(rawValue = rawValue)
    }
    open class CompanionClass {
        open fun FeedbackType(rawValue: Int): UINotificationFeedbackGenerator.FeedbackType? = UINotificationFeedbackGenerator.FeedbackType(rawValue = rawValue)
    }
}


/// UINotificationFeedbackGenerator is used to give user feedback when an notification is displayed
open class UISelectionFeedbackGenerator: UIFeedbackGenerator {

    constructor() {
    }

    /// call when a notification is displayed, passing the corresponding type
    open fun selectionChanged() {
        systemVibratorService?.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK))
    }

    open fun selectionChanged(at: CGPoint) {
        val location = at
        selectionChanged()
    }

    @androidx.annotation.Keep
    companion object: CompanionClass() {
    }
    open class CompanionClass {
    }
}

