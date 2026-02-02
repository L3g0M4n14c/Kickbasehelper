package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.runtime.Composable
import skip.model.StateTracking

@androidx.annotation.Keep
class EmptyModifier: ViewModifier, skip.lib.SwiftProjecting {

    constructor() {
    }

    override fun body(content: View): View {
        return ComposeBuilder { composectx: ComposeContext -> content.Compose(composectx) }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
        val identity: EmptyModifier = EmptyModifier()
    }
}

