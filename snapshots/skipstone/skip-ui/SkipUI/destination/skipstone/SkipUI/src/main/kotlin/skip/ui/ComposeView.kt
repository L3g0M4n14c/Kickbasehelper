package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/// Used to directly wrap user Compose content.
///
/// - Seealso: `ComposeBuilder`
@androidx.annotation.Keep
class ComposeView: View, Renderable, skip.lib.SwiftProjecting {
    private val content: @Composable (ComposeContext) -> Unit

    /// Constructor.
    ///
    /// The supplied `content` is the content to compose.
    constructor(content: @Composable (ComposeContext) -> Unit) {
        this.content = content
    }

    constructor(bridgedContent: Any) {
        this.content = { it ->
            (bridgedContent as? ContentComposer)?.Compose(context = it)
        }
    }

    @Composable
    override fun Render(context: ComposeContext): Unit = content(context)

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

/// Encapsulation of Composable content.
interface ContentComposer {
    @Composable
    fun Compose(context: ComposeContext)
}

/// Encapsulation of a Compose modifier.
interface ContentModifier {
    fun modify(view: View): View
}
