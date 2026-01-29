package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.runtime.Composable
import skip.model.StateTracking

interface ViewModifier {
    fun body(content: View): View = content

    /// Evaluate renderable content.
    ///
    /// - Warning: Do not give `options` a default value in this function signature. We have seen it cause bugs in which
    ///     the default version of the function is always invoked, ignoring implementor overrides.
    /// - Seealso: `View.Evaluate(context:options:)`
    @Composable
    fun Evaluate(content: View, context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        StateTracking.pushBody()
        val renderables = body(content = content).Evaluate(context = context, options = options)
        StateTracking.popBody()
        return renderables.sref()
    }
}


internal class ViewModifierView: View {
    internal val view: View
    internal val modifier: ViewModifier

    internal constructor(view: View, modifier: ViewModifier) {
        // Don't copy
        this.view = view
        this.modifier = modifier
    }

    @Composable
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> = modifier.Evaluate(content = view, context = context, options = options)
}

