package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp


/// Log layout constraints for debugging purposes.
///
/// - Parameter tag: The log tag to use (default: "LogLayout").
/// - Returns: A modifier that logs layout constraints.
internal fun Modifier.logLayout(tag: String = "LogLayout"): Modifier {
    return this.layout l@{ measurable, constraints ->
        android.util.Log.d(tag, "Constraints: minWidth=${constraints.minWidth}, maxWidth=${constraints.maxWidth}, " + "minHeight=${constraints.minHeight}, maxHeight=${constraints.maxHeight}")
        val placeable = measurable.measure(constraints)
        return@l layout(width = placeable.width, height = placeable.height) { -> placeable.place(0, 0) }
    }.onGloballyPositioned { it ->
        val bounds = it.boundsInWindow()
        android.util.Log.d(tag, "Bounds: (top=${bounds.top}, left=${bounds.left}, bottom=${bounds.bottom}, right=${bounds.right}, width=${bounds.width}, height=${bounds.height})")
    }
}

internal class AspectRatioModifier: RenderModifier {
    internal val ratio: Double?
    internal val contentMode: ContentMode

    internal constructor(ratio: Double?, contentMode: ContentMode): super() {
        this.ratio = ratio
        this.contentMode = contentMode
        this.action = { renderable, context ->
            val stripped = renderable.strip()
            if (stripped is Image || stripped is AsyncImage || ratio == null) {
                // Image has its own support for aspect ratios, and we allow the loaded Image in AsyncImage
                // to consume the modifier too
                EnvironmentValues.shared.setValues(l@{ it ->
                    it.set_aspectRatio(Tuple2(ratio, contentMode))
                    return@l ComposeResult.ok
                }, in_ = { -> renderable.Render(context = context) })
            } else {
                var context = context.sref()
                context.modifier = context.modifier.aspectRatio(Float(ratio))
                renderable.Render(context = context)
            }
        }
    }
}

internal class DisabledModifier: EnvironmentModifier {
    internal val disabled: Boolean

    internal constructor(disabled: Boolean): super() {
        this.disabled = disabled
        this.action = l@{ it ->
            it.setisEnabled(!disabled)
            return@l ComposeResult.ok
        }
    }
}

internal class PaddingModifier: RenderModifier {
    internal val insets: EdgeInsets

    internal constructor(insets: EdgeInsets): super(role = ModifierRole.spacing) {
        this.insets = insets.sref()
        this.action = { renderable, context ->
            val stripped = renderable.strip()
            if ((stripped is LazyVGrid || stripped is LazyHGrid || stripped is LazyVStack || stripped is LazyHStack) && renderable.forEachModifier(perform = { it -> if (it.role == ModifierRole.spacing) true else null }) == null) {
                // Certain views apply their padding themselves
                EnvironmentValues.shared.setValues(l@{ it ->
                    it.set_contentPadding(insets)
                    return@l ComposeResult.ok
                }, in_ = { -> renderable.Render(context = context) })
            } else {
                PaddingLayout(content = renderable, padding = insets, context = context)
            }
        }
    }
}

/// Used to mark views with a tag or ID.
internal class TagModifier: RenderModifier {
    internal val value: Any?

    internal constructor(value: Any?, role: ModifierRole): super(role = role) {
        this.value = value.sref()
    }

    @androidx.annotation.Keep
    companion object {

        /// Extract the existing tag modifier view from the given view's modifiers.
        internal fun on(content: Renderable, role: ModifierRole): TagModifier? {
            return content.forEachModifier l@{ it ->
                if (it.role == role) {
                    return@l it as? TagModifier
                } else {
                    return@l null
                }
            }
        }
    }
}

/// Use a special modifier for `zIndex` so that the artificial parent container created by `.frame` can
/// pull the `zIndex` value into its own modifiers.
///
/// Otherwise the extra frame container hides the `zIndex` value from this view's logical parent container.
///
/// - Seealso: `FrameLayout`
internal class ZIndexModifier: RenderModifier {
    private val zIndex: Double
    private var isConsumed = false

    internal constructor(zIndex: Double): super() {
        this.zIndex = zIndex
        this.action = { renderable, context ->
            var context = context.sref()
            if (!isConsumed) {
                context.modifier = context.modifier.zIndex(Float(zIndex))
            }
            renderable.Render(context = context)
        }
    }

    @androidx.annotation.Keep
    companion object {

        /// Move the application of the `zIndex` to the given modifier, erasing it from this view.
        internal fun consume(for_: Renderable, with: Modifier): Modifier {
            val renderable = for_
            val modifier = with
            val matchtarget_0 = renderable.forEachModifier(perform = l@{ it ->
                val matchtarget_1 = it as? ZIndexModifier
                if (matchtarget_1 != null) {
                    val zIndexModifier = matchtarget_1
                    zIndexModifier.isConsumed = true
                    return@l zIndexModifier.zIndex
                } else {
                    return@l null
                }
            })
            if (matchtarget_0 != null) {
                val zIndex = matchtarget_0
                return modifier.zIndex(Float(zIndex))
            } else {
                return modifier
            }
        }
    }
}
