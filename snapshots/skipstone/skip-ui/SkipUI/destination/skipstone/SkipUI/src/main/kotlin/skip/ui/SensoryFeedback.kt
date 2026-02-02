package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import android.os.VibrationEffect

class SensoryFeedback: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    enum class Weight {
        light,
        medium,
        heavy;

        @androidx.annotation.Keep
        companion object {
        }
    }

    enum class Flexibility {
        rigid,
        solid,
        soft;

        @androidx.annotation.Keep
        companion object {
        }
    }

    fun activate() {
        if (systemVibratorService == null) {
            return
        }

        // see: https://developer.android.com/develop/ui/views/haptics/custom-haptic-effects
        val composition = VibrationEffect.startComposition()

        // we create custom haptic feedback; we don't use https://developer.android.com/reference/android/view/HapticFeedbackConstants because many of those constants are only available in API 34+

        // various experimental implementations; we may eventually expose this to the user to be able to configure their "haptics style"
        val impl = 3

        if (impl == 0) {
            when (this) {
                SensoryFeedback.success -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.7f)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.5f)
                }
                SensoryFeedback.warning -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.9f)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.6f)
                }
                SensoryFeedback.error -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 1.0f)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.7f)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.5f)
                }
                SensoryFeedback.selection -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f)
                SensoryFeedback.increase -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.6f)
                SensoryFeedback.decrease -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.6f)
                SensoryFeedback.start -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.7f)
                SensoryFeedback.stop -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.7f)
                SensoryFeedback.alignment -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.4f)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.3f)
                }
                SensoryFeedback.levelChange -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)
                SensoryFeedback.impact -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.8f)
            }
        } else if (impl == 1) {
            when (this) {
                SensoryFeedback.success -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.5f)
                }
                SensoryFeedback.warning -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.7f)
                }
                SensoryFeedback.error -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 1.0f)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 1.0f)
                }
                SensoryFeedback.selection -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
                SensoryFeedback.increase -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.7f)
                }
                SensoryFeedback.decrease -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.7f)
                }
                SensoryFeedback.start -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 1.0f)
                SensoryFeedback.stop -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 1.0f)
                SensoryFeedback.alignment -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.5f)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)
                }
                SensoryFeedback.levelChange -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f)
                SensoryFeedback.impact -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
            }
        } else if (impl == 3) {
            when (this) {
                SensoryFeedback.success -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 100)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f, 200)
                }
                SensoryFeedback.warning -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 1.0f, 100)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 1.0f, 200)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 1.0f, 300)
                }
                SensoryFeedback.error -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 1.0f, 100)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 1.0f, 200)
                }
                SensoryFeedback.selection -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f, 50)
                SensoryFeedback.increase -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 100)
                SensoryFeedback.decrease -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 100)
                SensoryFeedback.start -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 1.0f, 100)
                SensoryFeedback.stop -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 1.0f, 100)
                SensoryFeedback.alignment -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f, 50)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f, 100)
                }
                SensoryFeedback.levelChange -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 100)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 200)
                }
                SensoryFeedback.impact -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 100)
            }
        } else if (impl == 4) {
            when (this) {
                SensoryFeedback.success -> {
                    // iOS success: A strong tap followed by a lighter tap after 100ms
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0) // Strong tap
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 100) // Light tap after 100ms
                }
                SensoryFeedback.warning -> {
                    // iOS warning: A strong, sharp tap followed by a quick fade after 100ms
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 1.0f, 0) // Strong rise
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 1.0f, 100) // Quick fall after 100ms
                }
                SensoryFeedback.error -> {
                    // iOS error: Three sequential taps with decreasing intensity and 100ms delays
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 1.0f, 0) // Strong tap
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.75f, 100) // Medium tap after 100ms
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.5f, 200) // Light tap after 200ms
                }
                SensoryFeedback.selection -> {
                    // iOS selection: A light, subtle tap
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.3f, 0) // Light tap
                }
                SensoryFeedback.increase -> {
                    // iOS increase: A single, sharp tap
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0) // Strong tap
                }
                SensoryFeedback.decrease -> {
                    // iOS decrease: A single, sharp tap (same as increase)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0) // Strong tap
                }
                SensoryFeedback.start -> {
                    // iOS start: A quick rise in intensity
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 1.0f, 0) // Quick rise
                }
                SensoryFeedback.stop -> {
                    // iOS stop: A quick fall in intensity
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 1.0f, 0) // Quick fall
                }
                SensoryFeedback.alignment -> {
                    // iOS alignment: Two light taps in quick succession (50ms delay)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 0) // First tap
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 50) // Second tap after 50ms
                }
                SensoryFeedback.levelChange -> {
                    // iOS levelChange: Two sharp taps in quick succession (100ms delay)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 0) // First tap
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 100) // Second tap after 100ms
                }
                SensoryFeedback.impact -> {
                    // iOS impact: A strong, heavy tap
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 0) // Heavy tap
                }
            }
        }

        systemVibratorService.vibrate(composition.compose())
    }

    override fun equals(other: Any?): Boolean {
        if (other !is SensoryFeedback) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val success = SensoryFeedback(rawValue = 1) // For bridging
        val warning = SensoryFeedback(rawValue = 2) // For bridging
        val error = SensoryFeedback(rawValue = 3) // For bridging
        val selection = SensoryFeedback(rawValue = 4) // For bridging
        val increase = SensoryFeedback(rawValue = 5) // For bridging
        val decrease = SensoryFeedback(rawValue = 6) // For bridging
        val start = SensoryFeedback(rawValue = 7) // For bridging
        val stop = SensoryFeedback(rawValue = 8) // For bridging
        val alignment = SensoryFeedback(rawValue = 9) // For bridging
        val levelChange = SensoryFeedback(rawValue = 10) // For bridging
        val impact = SensoryFeedback(rawValue = 11) // For bridging

        fun impact(weight: SensoryFeedback.Weight = SensoryFeedback.Weight.medium, intensity: Double = 1.0): SensoryFeedback = SensoryFeedback.impact

        fun impact(flexibility: SensoryFeedback.Flexibility, intensity: Double = 1.0): SensoryFeedback = SensoryFeedback.impact
    }
}

