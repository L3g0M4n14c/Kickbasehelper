package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

@androidx.annotation.Keep
class DisclosureGroup: View, Renderable, skip.lib.SwiftProjecting {
    internal val label: ComposeBuilder
    internal val content: ComposeBuilder
    internal val expandedBinding: Binding<Boolean>

    // We cannot support this constructor because we have not been able to get expansion working reliably
    // in Lists without an external Binding
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(content: () -> View, label: () -> View) {
        this.label = ComposeBuilder.from(label)
        this.content = ComposeBuilder.from(content)
        this.expandedBinding = Binding(get = { -> false }, set = { _ ->  })
    }

    constructor(isExpanded: Binding<Boolean>, content: () -> View, label: () -> View) {
        this.label = ComposeBuilder.from(label)
        this.content = ComposeBuilder.from(content)
        this.expandedBinding = isExpanded.sref()
    }

    constructor(getExpanded: () -> Boolean, setExpanded: (Boolean) -> Unit, bridgedContent: View, bridgedLabel: View) {
        this.label = ComposeBuilder.from { -> bridgedLabel }
        this.content = ComposeBuilder.from { -> bridgedContent }
        this.expandedBinding = Binding(get = getExpanded, set = setExpanded)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(titleKey: LocalizedStringKey, content: () -> View) {
        this.label = ComposeBuilder.from({ -> Text(titleKey) })
        this.content = ComposeBuilder.from(content)
        this.expandedBinding = Binding(get = { -> false }, set = { _ ->  })
    }

    constructor(titleResource: LocalizedStringResource, content: () -> View) {
        this.label = ComposeBuilder.from({ -> Text(titleResource) })
        this.content = ComposeBuilder.from(content)
        this.expandedBinding = Binding(get = { -> false }, set = { _ ->  })
    }

    constructor(titleKey: LocalizedStringKey, isExpanded: Binding<Boolean>, content: () -> View): this(isExpanded = isExpanded, content = content, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleKey).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleResource: LocalizedStringResource, isExpanded: Binding<Boolean>, content: () -> View): this(isExpanded = isExpanded, content = content, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleResource).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(label: String, content: () -> View) {
        this.label = ComposeBuilder.from({ -> Text(verbatim = label) })
        this.content = ComposeBuilder.from(content)
        this.expandedBinding = Binding(get = { -> false }, set = { _ ->  })
    }

    constructor(label: String, isExpanded: Binding<Boolean>, content: () -> View): this(isExpanded = isExpanded, content = content, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(verbatim = label).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    @Composable
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val level_0 = EvaluateOptions(options).lazyItemLevel
        if (level_0 == null) {
            return listOf(this)
        }
        if (!expandedBinding.wrappedValue) {
            return listOf(this)
        }
        val renderables = content.EvaluateLazyItems(level = level_0 + 1, context = context)
        return (listOf(this) + renderables).sref()
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val columnArrangement = Arrangement.spacedBy(8.dp, alignment = androidx.compose.ui.Alignment.CenterVertically)
        val contentContext = context.content()
        ComposeContainer(axis = Axis.vertical, modifier = context.modifier, fillWidth = true) { modifier ->
            Column(modifier = modifier, verticalArrangement = columnArrangement, horizontalAlignment = androidx.compose.ui.Alignment.Start) { ->
                RenderLabel(context = contentContext)
                // Note: we can't seem to turn *off* animation when in AnimatedContent, so we've removed the code that
                // tries. We could take a separate code path to avoid AnimatedContent, but then a change in animation
                // status could cause us to lose state
                AnimatedContent(targetState = expandedBinding.wrappedValue) { isExpanded ->
                    if (isExpanded) {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = columnArrangement, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) { -> content.Compose(context = contentContext) }
                    }
                }
            }
        }
    }

    @Composable
    override fun shouldRenderListItem(context: ComposeContext): Tuple2<Boolean, (() -> Unit)?> {
        // Attempting to animate the list expansion and contraction doesn't work well and causes artifacts
        // in other list items
        return Tuple2(true, { -> expandedBinding.wrappedValue = !expandedBinding.wrappedValue })
    }

    @Composable
    override fun RenderListItem(context: ComposeContext, modifiers: kotlin.collections.List<ModifierProtocol>) {
        ModifiedContent.RenderWithModifiers(modifiers, context = context) { it -> RenderLabel(context = it, isListItem = true) }
    }

    @Composable
    internal fun RenderLabel(context: ComposeContext, isListItem: Boolean = false) {
        val contentContext = context.content()
        val isEnabled = EnvironmentValues.shared.isEnabled
        val (foregroundStyle, accessoryColor) = composeStyles(isEnabled = isEnabled, isListItem = isListItem)
        val rotationAngle = Float(if (expandedBinding.wrappedValue) 90 else 0).asAnimatable(context = contentContext)
        val isRTL = EnvironmentValues.shared.layoutDirection == LayoutDirection.rightToLeft
        val modifier: Modifier = if (isEnabled && !isListItem) context.modifier.clickable(onClick = { ->
            withAnimation { -> expandedBinding.wrappedValue = !expandedBinding.wrappedValue }
        }) else context.modifier
        Row(modifier = modifier, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { ->
            Box(modifier = Modifier.padding(end = 8.dp).weight(1.0f)) { ->
                EnvironmentValues.shared.setValues(l@{ it ->
                    if (foregroundStyle != null) {
                        it.set_foregroundStyle(foregroundStyle)
                    }
                    return@l ComposeResult.ok
                }, in_ = { -> label.Compose(context = contentContext) })
            }
            Icon(modifier = Modifier.rotate(rotationAngle.value), imageVector = if (isRTL) Icons.Outlined.KeyboardArrowLeft else Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = accessoryColor)
        }
    }

    @Composable
    private fun composeStyles(isEnabled: Boolean, isListItem: Boolean): Tuple2<ShapeStyle?, androidx.compose.ui.graphics.Color> {
        var foregroundStyle: ShapeStyle? = null
        if (!isListItem) {
            foregroundStyle = (EnvironmentValues.shared._foregroundStyle ?: EnvironmentValues.shared._tint ?: Color.accentColor).sref()
        }
        var accessoryColor = foregroundStyle?.asColor(opacity = 1.0, animationContext = null) ?: EnvironmentValues.shared._tint?.colorImpl?.invoke() ?: Color.accentColor.colorImpl()
        if (!isEnabled) {
            if (isListItem) {
                accessoryColor = MaterialTheme.colorScheme.outlineVariant
            } else {
                val disabledAlpha = ContentAlpha.disabled.sref()
                if (foregroundStyle != null) {
                    foregroundStyle = AnyShapeStyle(foregroundStyle!!, opacity = Double(disabledAlpha))
                }
                accessoryColor = accessoryColor.copy(alpha = disabledAlpha)
            }
        }
        return Tuple2(foregroundStyle.sref(), accessoryColor)
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

class DisclosureGroupStyle: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is DisclosureGroupStyle) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = DisclosureGroupStyle(rawValue = 0)
    }
}

/*
/// The properties of a disclosure group instance.
@available(iOS 16.0, macOS 13.0, *)
@available(tvOS, unavailable)
@available(watchOS, unavailable)
public struct DisclosureGroupStyleConfiguration {

/// A type-erased label of a disclosure group.
public struct Label : View {

/// The type of view representing the body of this view.
///
/// When you create a custom view, Swift infers this type from your
/// implementation of the required ``View/body-swift.property`` property.
public typealias Body = NeverView
public var body: Body { fatalError() }
}

/// The label for the disclosure group.
public let label: DisclosureGroupStyleConfiguration.Label = { fatalError() }()

/// A type-erased content of a disclosure group.
public struct Content : View {

/// The type of view representing the body of this view.
///
/// When you create a custom view, Swift infers this type from your
/// implementation of the required ``View/body-swift.property`` property.
public typealias Body = NeverView
public var body: Body { fatalError() }
}

/// The content of the disclosure group.
public let content: DisclosureGroupStyleConfiguration.Content = { fatalError() }()

/// A binding to a Boolean that indicates whether the disclosure
/// group is expanded.
//    @Binding public var isExpanded: Bool { get { fatalError() } nonmutating set { } }

//    public var $isExpanded: Binding<Bool> { get { fatalError() } }
}
*/
