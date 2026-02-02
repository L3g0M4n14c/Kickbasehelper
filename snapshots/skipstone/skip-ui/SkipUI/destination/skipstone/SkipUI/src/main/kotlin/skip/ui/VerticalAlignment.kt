package skip.ui

import skip.lib.*
import skip.lib.Sequence

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

// NOTE: Keep in sync with SkipSwiftUI.VerticalAlignment
class VerticalAlignment {
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
    fun asComposeAlignment(): androidx.compose.ui.Alignment.Vertical {
        when (this) {
            VerticalAlignment.bottom -> return androidx.compose.ui.Alignment.Bottom.sref()
            VerticalAlignment.top -> return androidx.compose.ui.Alignment.Top.sref()
            else -> return androidx.compose.ui.Alignment.CenterVertically.sref()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is VerticalAlignment) return false
        return key == other.key
    }

    @androidx.annotation.Keep
    companion object {

        val center = VerticalAlignment(key = "center")
        val top = VerticalAlignment(key = "top")
        val bottom = VerticalAlignment(key = "bottom")
        val firstTextBaseline = VerticalAlignment(key = "firstTextBaseline")
        val lastTextBaseline = VerticalAlignment(key = "lastTextBaseline")
    }
}

