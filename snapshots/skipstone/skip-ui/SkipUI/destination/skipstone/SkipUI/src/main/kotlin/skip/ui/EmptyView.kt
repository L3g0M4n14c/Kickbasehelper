package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.runtime.Composable

@androidx.annotation.Keep
class EmptyView: View, Renderable, skip.lib.SwiftProjecting {
    constructor() {
    }

    @Composable
    override fun Render(context: ComposeContext) = Unit

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

