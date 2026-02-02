package skip.foundation

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

enum class ComparisonResult(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
    orderedAscending(-1),
    orderedSame(0),
    orderedDescending(1);

    @androidx.annotation.Keep
    companion object {

        val ascending: ComparisonResult
            get() = ComparisonResult.orderedAscending
        val same: ComparisonResult
            get() = ComparisonResult.orderedSame
        val descending: ComparisonResult
            get() = ComparisonResult.orderedDescending

        fun init(rawValue: Int): ComparisonResult? {
            return when (rawValue) {
                -1 -> ComparisonResult.orderedAscending
                0 -> ComparisonResult.orderedSame
                1 -> ComparisonResult.orderedDescending
                else -> null
            }
        }
    }
}

fun ComparisonResult(rawValue: Int): ComparisonResult? = ComparisonResult.init(rawValue = rawValue)
