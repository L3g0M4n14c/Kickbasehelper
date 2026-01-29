package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

@androidx.annotation.Keep
enum class ContentMode(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, RawRepresentable<Int> {
    fit(0), // For bridging
    fill(1); // For bridging

    @androidx.annotation.Keep
    companion object: CaseIterableCompanion<ContentMode> {
        fun init(rawValue: Int): ContentMode? {
            return when (rawValue) {
                0 -> ContentMode.fit
                1 -> ContentMode.fill
                else -> null
            }
        }

        override val allCases: Array<ContentMode>
            get() = arrayOf(fit, fill)
    }
}

fun ContentMode(rawValue: Int): ContentMode? = ContentMode.init(rawValue = rawValue)

