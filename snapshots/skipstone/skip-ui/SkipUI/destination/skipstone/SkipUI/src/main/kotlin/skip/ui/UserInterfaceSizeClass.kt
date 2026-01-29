package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass

enum class UserInterfaceSizeClass(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
    compact(1), // For bridging
    regular(2); // For bridging


    @androidx.annotation.Keep
    companion object {
        fun fromWindowHeightSizeClass(sizeClass: WindowHeightSizeClass): UserInterfaceSizeClass = if (sizeClass == WindowHeightSizeClass.COMPACT) UserInterfaceSizeClass.compact else UserInterfaceSizeClass.regular

        fun fromWindowWidthSizeClass(sizeClass: WindowWidthSizeClass): UserInterfaceSizeClass = if (sizeClass == WindowWidthSizeClass.COMPACT) UserInterfaceSizeClass.compact else UserInterfaceSizeClass.regular

        fun init(rawValue: Int): UserInterfaceSizeClass? {
            return when (rawValue) {
                1 -> UserInterfaceSizeClass.compact
                2 -> UserInterfaceSizeClass.regular
                else -> null
            }
        }
    }
}

fun UserInterfaceSizeClass(rawValue: Int): UserInterfaceSizeClass? = UserInterfaceSizeClass.init(rawValue = rawValue)

