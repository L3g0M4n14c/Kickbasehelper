package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.runtime.Composable

/// Used to wrap the content of SwiftUI `@ViewBuilders` for rendering by Compose.
@androidx.annotation.Keep
class ComposeBuilder: View, skip.lib.SwiftProjecting {
    private val content: @Composable (ComposeContext) -> ComposeResult

    /// Construct with static content.
    ///
    /// Used primarily when manually constructing views for internal use.
    constructor(view: View) {
        this.content = l@{ context -> return@l view.Compose(context = context) }
    }

    constructor(bridgedViews: Array<View>) {
        this.content = l@{ context ->
            bridgedViews.forEach { it -> it.Compose(context = context) }
            return@l ComposeResult.ok
        }
    }

    /// Constructor.
    ///
    /// The supplied `content` is the content to compose. When transpiling SwiftUI code, this is the logic embedded in the user's `body` and within each container view in
    /// that `body`, as well as within other `@ViewBuilders`.
    ///
    /// - Note: Returning a result from `content` is important. This prevents Compose from recomposing `content` on its own. Instead, a change that would recompose
    ///   `content` elevates to our void `Renderable.Render`. This allows us to prepare for recompositions, e.g. making the proper callbacks to the context's `composer`.
    constructor(content: @Composable (ComposeContext) -> ComposeResult) {
        this.content = content
    }

    @Composable
    override fun Compose(context: ComposeContext): ComposeResult {
        // If there is a composer, allow its result to escape. Otherwise compose in a non-escaping context
        // to avoid unneeded recomposes
        if (context.composer != null) {
            return content(context)
        } else {
            _ComposeContent(context)
            return ComposeResult.ok
        }
    }

    /// Create a non-escaping context to avoid unnecessary recomposition.
    @Composable
    override fun _ComposeContent(context: ComposeContext) {
        content(context)
    }

    @Composable
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val renderables: kotlin.collections.MutableList<Renderable> = mutableListOf()
        val isKeepNonModified = EvaluateOptions(options).isKeepNonModified
        val evalContext = context.content(composer = Composer l@{ view, context ->
            // Note: this logic is also in `ModifiedContent`, but we need to check here as well in case no modifiers are used
            if (isKeepNonModified && !(view is ModifiedContent) && !(view is ForEach) && !(view is Group)) {
                renderables.add(view.asRenderable())
            } else {
                renderables.addAll(view.Evaluate(context = context(false), options = options))
            }
            return@l ComposeResult.ok
        })
        content(evalContext)
        return renderables.sref()
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {

        /// If the result of the given block is a `ComposeBuilder` return it, else create a `ComposeBuilder` whose content is the
        /// resulting view.
        fun from(content: () -> View): ComposeBuilder {
            val view = content()
            return view as? ComposeBuilder ?: ComposeBuilder(view = view)
        }
    }
}
