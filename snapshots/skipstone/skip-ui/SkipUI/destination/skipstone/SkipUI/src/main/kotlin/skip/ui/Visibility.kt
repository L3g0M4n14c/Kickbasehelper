package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

@androidx.annotation.Keep
enum class Visibility(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, RawRepresentable<Int> {
    automatic(0), // For bridging
    visible(1), // For bridging
    hidden(2); // For bridging

    @androidx.annotation.Keep
    companion object: CaseIterableCompanion<Visibility> {
        fun init(rawValue: Int): Visibility? {
            return when (rawValue) {
                0 -> Visibility.automatic
                1 -> Visibility.visible
                2 -> Visibility.hidden
                else -> null
            }
        }

        override val allCases: Array<Visibility>
            get() = arrayOf(automatic, visible, hidden)
    }
}

fun Visibility(rawValue: Int): Visibility? = Visibility.init(rawValue = rawValue)

