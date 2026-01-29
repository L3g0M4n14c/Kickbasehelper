package skip.ui

import skip.lib.*
import skip.lib.Sequence

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

// NOTE: Keep in sync with SkipSwiftUI.HorizontalAlignment
class HorizontalAlignment {
    internal val key: String

    internal constructor(key: String) {
        this.key = key
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(id: Any) {
        key = ""
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun combineExplicit(values: Sequence<Double?>): Double? {
        fatalError()
    }

    /// Return the equivalent Compose alignment.
    fun asComposeAlignment(): androidx.compose.ui.Alignment.Horizontal {
        when (this) {
            HorizontalAlignment.leading -> return androidx.compose.ui.Alignment.Start.sref()
            HorizontalAlignment.trailing -> return androidx.compose.ui.Alignment.End.sref()
            else -> return androidx.compose.ui.Alignment.CenterHorizontally.sref()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is HorizontalAlignment) return false
        return key == other.key
    }

    @androidx.annotation.Keep
    companion object {

        val center: HorizontalAlignment = HorizontalAlignment(key = "center")
        val leading: HorizontalAlignment = HorizontalAlignment(key = "leading")
        val trailing: HorizontalAlignment = HorizontalAlignment(key = "trailing")
        val listRowSeparatorLeading = HorizontalAlignment(key = "listRowSeparatorLeading")
        val listRowSeparatorTrailing = HorizontalAlignment(key = "listRowSeparatorTrailing")
    }
}

