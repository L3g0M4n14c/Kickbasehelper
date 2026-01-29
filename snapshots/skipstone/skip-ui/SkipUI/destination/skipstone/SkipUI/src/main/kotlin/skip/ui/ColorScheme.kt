package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

@androidx.annotation.Keep
enum class ColorScheme(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, RawRepresentable<Int> {
    light(0), // For bridging
    dark(1); // For bridging


    /// Return the material color scheme for this scheme.
    @Composable
    fun asMaterialTheme(): androidx.compose.material3.ColorScheme {
        val context = LocalContext.current.sref()
        val isDarkMode = this == ColorScheme.dark
        // Dynamic color is available on Android 12+
        val isDynamicColor = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
        var colorScheme: androidx.compose.material3.ColorScheme
        if (isDarkMode) {
            colorScheme = if (isDynamicColor) dynamicDarkColorScheme(context) else darkColorScheme()
        } else {
            colorScheme = if (isDynamicColor) dynamicLightColorScheme(context) else lightColorScheme()
        }
        Color.assetAccentColor(colorScheme = if (isDarkMode) ColorScheme.dark else ColorScheme.light)?.let { primary ->
            colorScheme = colorScheme.copy(primary = primary)
        }
        val customization_0 = EnvironmentValues.shared._material3ColorScheme
        if (customization_0 == null) {
            return colorScheme.sref()
        }
        return customization_0(colorScheme, isDarkMode)
    }

    @androidx.annotation.Keep
    companion object: CaseIterableCompanion<ColorScheme> {
        /// Return the color scheme for the current material color scheme.
        @Composable
        fun fromMaterialTheme(colorScheme: androidx.compose.material3.ColorScheme = MaterialTheme.colorScheme): ColorScheme {
            // Material3 doesn't have a built-in light vs dark property, so use the luminance of the background
            return if (colorScheme.background.luminance() > 0.5f) ColorScheme.light else ColorScheme.dark
        }

        fun init(rawValue: Int): ColorScheme? {
            return when (rawValue) {
                0 -> ColorScheme.light
                1 -> ColorScheme.dark
                else -> null
            }
        }

        override val allCases: Array<ColorScheme>
            get() = arrayOf(light, dark)
    }
}

fun ColorScheme(rawValue: Int): ColorScheme? = ColorScheme.init(rawValue = rawValue)

@Composable
fun MaterialColorScheme(scheme: (@Composable (androidx.compose.material3.ColorScheme, Boolean) -> androidx.compose.material3.ColorScheme)?, content: @Composable () -> Unit): Unit = Material3ColorScheme(scheme, content = content)

@Composable
fun Material3ColorScheme(scheme: (@Composable (androidx.compose.material3.ColorScheme, Boolean) -> androidx.compose.material3.ColorScheme)?, content: @Composable () -> Unit) {
    EnvironmentValues.shared.setValues(l@{ it ->
        it.set_material3ColorScheme(scheme)
        return@l ComposeResult.ok
    }, in_ = { -> content() })
}

@androidx.annotation.Keep
internal class PreferredColorSchemePreferenceKey: PreferenceKey<PreferredColorScheme> {

    @androidx.annotation.Keep
    companion object: PreferenceKeyCompanion<PreferredColorScheme> {
        override val defaultValue = PreferredColorScheme(colorScheme = null)

        override fun reduce(value: InOut<PreferredColorScheme>, nextValue: () -> PreferredColorScheme) {
            value.value = nextValue()
        }
    }
}

internal class PreferredColorScheme {
    internal val colorScheme: ColorScheme?

    constructor(colorScheme: ColorScheme? = null) {
        this.colorScheme = colorScheme
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PreferredColorScheme) return false
        return colorScheme == other.colorScheme
    }
}
