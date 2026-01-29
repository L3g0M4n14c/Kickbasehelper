package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

@androidx.annotation.Keep
enum class ControlSize(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, RawRepresentable<Int> {
    mini(0),
    small(1),
    regular(2),
    large(3),
    extraLarge(4);

    @androidx.annotation.Keep
    companion object: CaseIterableCompanion<ControlSize> {
        fun init(rawValue: Int): ControlSize? {
            return when (rawValue) {
                0 -> ControlSize.mini
                1 -> ControlSize.small
                2 -> ControlSize.regular
                3 -> ControlSize.large
                4 -> ControlSize.extraLarge
                else -> null
            }
        }

        override val allCases: Array<ControlSize>
            get() = arrayOf(mini, small, regular, large, extraLarge)
    }
}

fun ControlSize(rawValue: Int): ControlSize? = ControlSize.init(rawValue = rawValue)

