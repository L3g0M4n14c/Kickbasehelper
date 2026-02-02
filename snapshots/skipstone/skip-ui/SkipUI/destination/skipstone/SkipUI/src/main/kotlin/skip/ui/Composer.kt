package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.runtime.Composable

/// Mechanism for a parent to change how a child view is composed.
///
/// Composers are escaping, meaning that if the internal content needs to recompose, the calling context will also recompose.
open class Composer {
    private val compose: (@Composable (View, (Boolean) -> ComposeContext) -> ComposeResult)?

    /// Optionally provide a compose block to execute instead of subclassing.
    ///
    /// - Note: This is a separate method from the default constructor rather than giving `compose` a default value to work around Kotlin runtime
    ///   crashes related to using composable closures.
    internal constructor(compose: @Composable (View, (Boolean) -> ComposeContext) -> ComposeResult) {
        this.compose = compose
    }

    internal constructor() {
        this.compose = null
    }

    @Composable
    open fun Compose(view: View, context: (Boolean) -> ComposeContext): ComposeResult {
        val matchtarget_0 = compose
        if (matchtarget_0 != null) {
            val compose = matchtarget_0
            return compose(view, context)
        } else {
            return ComposeResult.ok
        }
    }

    @androidx.annotation.Keep
    companion object: CompanionClass() {
    }
    open class CompanionClass {
    }
}

