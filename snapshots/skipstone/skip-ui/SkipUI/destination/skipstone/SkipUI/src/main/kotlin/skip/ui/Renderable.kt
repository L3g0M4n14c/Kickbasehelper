package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/// Renders content via Compose.
interface Renderable {
    @Composable
    fun Render(context: ComposeContext)

    /// Whether this renderable specializes for list items.
    ///
    /// - Returns: A tuple containing whether this item specializes rendering for list items and any list item action it applies.
    ///     The given action will become a tap action on the entire list item cell.
    @Composable
    fun shouldRenderListItem(context: ComposeContext): Tuple2<Boolean, (() -> Unit)?> = Tuple2(false, null)

    /// Render as a list item.
    @Composable
    fun RenderListItem(context: ComposeContext, modifiers: kotlin.collections.List<ModifierProtocol>) = Unit

    /// Whether this is an empty view.
    val isSwiftUIEmptyView: Boolean
        get() = strip() is EmptyView

    /// Strip enclosing modifiers, etc.
    fun strip(): Renderable = this.sref()

    /// Perform an action for every modifier.
    ///
    /// The first non-nil value will be returned.
    fun <R> forEachModifier(perform: (ModifierProtocol) -> R?): R? {
        val action = perform
        return null
    }

    /// Represent this `Renderable` as a `View`.
    fun asView(): View {
        return (this as? View ?: ComposeView(content = { it -> this.Render(it) })).sref()
    }
}



