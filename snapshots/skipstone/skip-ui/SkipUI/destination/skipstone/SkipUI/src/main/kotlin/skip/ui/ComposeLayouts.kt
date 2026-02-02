package skip.ui

import skip.lib.*
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp

/// Compose a view with the given frame.
@Composable
internal fun FrameLayout(content: Renderable, context: ComposeContext, width: Double?, height: Double?, alignment: Alignment) {
    var modifier = context.modifier
    if (width != null) {
        modifier = modifier.requiredWidth(width.dp)
    }
    if (height != null) {
        modifier = modifier.requiredHeight(height.dp)
    }

    // If our content has a zIndex, we need to pull it into our modifiers so that it applies within the original
    // parent container. Otherwise the Box we use below would hide it
    modifier = ZIndexModifier.consume(for_ = content, with = modifier)

    ComposeContainer(modifier = modifier, fixedWidth = width != null, fixedHeight = height != null) { modifier ->
        val contentContext = context.content()
        Box(modifier = modifier, contentAlignment = alignment.asComposeAlignment()) { -> content.Render(context = contentContext) }
    }
}

/// Compose a view with the given frame.
@Composable
internal fun FrameLayout(content: Renderable, context: ComposeContext, minWidth: Double?, idealWidth: Double?, maxWidth: Double?, minHeight: Double?, idealHeight: Double?, maxHeight: Double?, alignment: Alignment) {
    ComposeFlexibleContainer(modifier = context.modifier, flexibleWidthIdeal = flexibleLayoutFloat(idealWidth), flexibleWidthMin = flexibleLayoutFloat(minWidth), flexibleWidthMax = flexibleLayoutFloat(maxWidth), flexibleHeightIdeal = flexibleLayoutFloat(idealHeight), flexibleHeightMin = flexibleLayoutFloat(minHeight), flexibleHeightMax = flexibleLayoutFloat(maxHeight)) { modifier ->
        val contentContext = context.content()
        Box(modifier = modifier, contentAlignment = alignment.asComposeAlignment()) { -> content.Render(context = contentContext) }
    }
}

private fun flexibleLayoutFloat(value: Double?): Float? {
    if (value == null) {
        return null
    }
    return if (value == Double.infinity) Float.flexibleFill else Float(value)
}

/// Compose a view with the given background.
@Composable
internal fun BackgroundLayout(content: Renderable, context: ComposeContext, background: View, alignment: Alignment) {
    TargetViewLayout(context = context, isOverlay = false, alignment = alignment, target = { it -> content.Render(context = it) }, dependent = { it -> background.Compose(context = it) })
}

/// Compose a view with the given overlay.
@Composable
internal fun OverlayLayout(content: Renderable, context: ComposeContext, overlay: View, alignment: Alignment) {
    TargetViewLayout(context = context, isOverlay = true, alignment = alignment, target = { it -> content.Render(context = it) }, dependent = { it -> overlay.Compose(context = it) })
}

/// Compose a view with the given mask.
/// The mask view's alpha channel is used to clip the content.
@Composable
internal fun MaskLayout(content: Renderable, context: ComposeContext, mask: View, alignment: Alignment) {
    // We use CompositingStrategy.Offscreen to render content and mask into an offscreen buffer
    // Then apply BlendMode.DstIn which keeps the destination (content) only where the source (mask) has alpha
    ComposeContainer(modifier = context.modifier) { modifier ->
        Layout(modifier = modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen), content = { ->
            // First render the content
            content.Render(context = context.content())
            // Then render the mask with DstIn blend mode - this will use mask's alpha to clip content
            ComposeContainer(fixedWidth = true, fixedHeight = true) { maskModifier ->
                Box(modifier = maskModifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen, blendMode = BlendMode.DstIn)) { -> mask.Compose(context = context.content()) }
            }
        }) l@{ measurables, constraints ->
            if (measurables.isEmpty()) {
                return@l layout(width = 0, height = 0) { ->  }
            }
            // Base layout entirely on the content view size
            val contentPlaceable = measurables[0].measure(constraints)
            val maskConstraints = Constraints(maxWidth = contentPlaceable.width, maxHeight = contentPlaceable.height)
            val maskPlaceables = measurables.drop(1).map { it -> it.measure(maskConstraints) }
            layout(width = contentPlaceable.width, height = contentPlaceable.height) { ->
                contentPlaceable.placeRelative(x = 0, y = 0)
                for (maskPlaceable in maskPlaceables.sref()) {
                    val (x, y) = placeContent(width = maskPlaceable.width, height = maskPlaceable.height, inWidth = contentPlaceable.width, inHeight = contentPlaceable.height, alignment = alignment)
                    maskPlaceable.placeRelative(x = x, y = y)
                }
            }
        }
    }
}

@Composable
internal fun TargetViewLayout(context: ComposeContext, isOverlay: Boolean, alignment: Alignment, target: @Composable (ComposeContext) -> Unit, dependent: @Composable (ComposeContext) -> Unit) {
    // ComposeContainer is needed to properly handle content that fills width/height
    ComposeContainer(modifier = context.modifier) { modifier ->
        Layout(modifier = modifier, content = { ->
            target(context.content())
            // Dependent view lays out with fixed bounds dictated by the target view size
            ComposeContainer(fixedWidth = true, fixedHeight = true) { modifier -> dependent(context.content(modifier = modifier)) }
        }) l@{ measurables, constraints ->
            if (measurables.isEmpty()) {
                return@l layout(width = 0, height = 0) { ->  }
            }
            // Base layout entirely on the target view size
            val targetPlaceable = measurables[0].measure(constraints)
            val dependentConstraints = Constraints(maxWidth = targetPlaceable.width, maxHeight = targetPlaceable.height)
            val dependentPlaceables = measurables.drop(1).map { it -> it.measure(dependentConstraints) }
            layout(width = targetPlaceable.width, height = targetPlaceable.height) { ->
                if (!isOverlay) {
                    for (dependentPlaceable in dependentPlaceables.sref()) {
                        val (x, y) = placeContent(width = dependentPlaceable.width, height = dependentPlaceable.height, inWidth = targetPlaceable.width, inHeight = targetPlaceable.height, alignment = alignment)
                        dependentPlaceable.placeRelative(x = x, y = y)
                    }
                }
                targetPlaceable.placeRelative(x = 0, y = 0)
                if (isOverlay) {
                    for (dependentPlaceable in dependentPlaceables.sref()) {
                        val (x, y) = placeContent(width = dependentPlaceable.width, height = dependentPlaceable.height, inWidth = targetPlaceable.width, inHeight = targetPlaceable.height, alignment = alignment)
                        dependentPlaceable.placeRelative(x = x, y = y)
                    }
                }
            }
        }
    }
}

/// Layout the given view to ignore the given safe areas.
@Composable
internal fun IgnoresSafeAreaLayout(content: Renderable, context: ComposeContext, expandInto: Edge.Set) {
    ComposeContainer(modifier = context.modifier) { modifier ->
        IgnoresSafeAreaLayout(expandInto = expandInto, modifier = modifier) { _, _ -> content.Render(context.content()) }
    }
}

/// Layout the given content ignoring the given safe areas.
///
/// - Parameter expandInto: Which safe area edges to expand into, if adjacent. Any expansion will be passed to
///     the given closure as a pixel rect.
/// - Parameter checkEdges: Which edges to check to see if we're against a safe area. Any matching edges will be
///     passed to the given closure.
@Composable
internal fun IgnoresSafeAreaLayout(expandInto: Edge.Set, checkEdges: Edge.Set = Edge.Set.of(), modifier: Modifier = Modifier, target: @Composable (IntRect, Edge.Set) -> Unit) {
    val safeArea_0 = EnvironmentValues.shared._safeArea
    if (safeArea_0 == null) {
        target(IntRect.Zero, Edge.Set.of())
        return
    }

    // Note: We only allow edges we're interested in to affect our internal state and output. This is critical
    // for reducing recompositions, especially during e.g. navigation animations. We also match our internal
    // state to our output to ensure we aren't re-calling the target block when output hasn't changed
    val edgesState = remember { -> mutableStateOf(checkEdges) }
    val edges = edgesState.value.sref()
    var expansionTop = 0
    if (expandInto.contains(Edge.Set.top) && edges.contains(Edge.Set.top)) {
        expansionTop = Int(safeArea_0.safeBoundsPx.top - safeArea_0.presentationBoundsPx.top)
    }
    var expansionBottom = 0
    if (expandInto.contains(Edge.Set.bottom) && edges.contains(Edge.Set.bottom)) {
        expansionBottom = Int(safeArea_0.presentationBoundsPx.bottom - safeArea_0.safeBoundsPx.bottom)
    }
    var expansionLeft = 0
    var expansionRight = 0
    val isRTL = LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    if (isRTL) {
        if (expandInto.contains(Edge.Set.leading) && edges.contains(Edge.Set.leading)) {
            expansionRight = Int(safeArea_0.presentationBoundsPx.right - safeArea_0.safeBoundsPx.right)
        }
        if (expandInto.contains(Edge.Set.trailing) && edges.contains(Edge.Set.trailing)) {
            expansionLeft = Int(safeArea_0.safeBoundsPx.left - safeArea_0.presentationBoundsPx.left)
        }
    } else {
        if (expandInto.contains(Edge.Set.leading) && edges.contains(Edge.Set.leading)) {
            expansionLeft = Int(safeArea_0.safeBoundsPx.left - safeArea_0.presentationBoundsPx.left)
        }
        if (expandInto.contains(Edge.Set.trailing) && edges.contains(Edge.Set.trailing)) {
            expansionRight = Int(safeArea_0.presentationBoundsPx.right - safeArea_0.safeBoundsPx.right)
        }
    }

    var (safeLeft, safeTop, safeRight, safeBottom) = safeArea_0.safeBoundsPx.sref()
    safeLeft -= expansionLeft
    safeTop -= expansionTop
    safeRight += expansionRight
    safeBottom += expansionBottom

    val contentSafeBounds = Rect(top = safeTop, left = safeLeft, bottom = safeBottom, right = safeRight)
    val contentSafeArea = SafeArea(presentation = safeArea_0.presentationBoundsPx, safe = contentSafeBounds, absoluteSystemBars = safeArea_0.absoluteSystemBarEdges)
    EnvironmentValues.shared.setValues(l@{ it ->
        it.set_safeArea(contentSafeArea)
        return@l ComposeResult.ok
    }, in_ = { ->
        Layout(modifier = modifier.onGloballyPositionedInWindow { it ->
            val edges = adjacentSafeAreaEdges(bounds = it, safeArea = safeArea_0, isRTL = isRTL, checkEdges = expandInto.union(checkEdges))
            edgesState.value = edges
        }, content = { ->
            val expansion = IntRect(top = expansionTop, left = expansionLeft, bottom = expansionBottom, right = expansionRight)
            target(expansion, edges.intersection(checkEdges))
        }) l@{ measurables, constraints ->
            if (measurables.isEmpty()) {
                return@l layout(width = 0, height = 0) { ->  }
            }
            val updatedConstraints = constraints.copy(maxWidth = constraints.maxWidth + expansionLeft + expansionRight, maxHeight = constraints.maxHeight + expansionTop + expansionBottom)
            val targetPlaceables = measurables.map { it -> it.measure(updatedConstraints) }
            layout(width = targetPlaceables[0].width, height = targetPlaceables[0].height) { ->
                // Layout will center extra space by default
                val relativeTop = expansionTop - ((expansionTop + expansionBottom) / 2)
                val expansionLeading = if (isRTL) expansionRight else expansionLeft
                val relativeLeading = expansionLeading - ((expansionLeft + expansionRight) / 2)
                for (targetPlaceable in targetPlaceables.sref()) {
                    targetPlaceable.placeRelative(x = -relativeLeading, y = -relativeTop)
                }
            }
        }
    })
}

private fun adjacentSafeAreaEdges(bounds: Rect, safeArea: SafeArea, isRTL: Boolean, checkEdges: Edge.Set): Edge.Set {
    var edges: Edge.Set = Edge.Set.of()
    if (checkEdges.contains(Edge.Set.top) && (bounds.top <= safeArea.safeBoundsPx.top + 0.1)) {
        edges.insert(Edge.Set.top)
    }
    if (checkEdges.contains(Edge.Set.bottom) && (bounds.bottom >= safeArea.safeBoundsPx.bottom - 0.1)) {
        edges.insert(Edge.Set.bottom)
    }
    if (isRTL) {
        if (checkEdges.contains(Edge.Set.leading) && (bounds.right >= safeArea.safeBoundsPx.right - 0.1)) {
            edges.insert(Edge.Set.leading)
        }
        if (checkEdges.contains(Edge.Set.trailing) && (bounds.left <= safeArea.safeBoundsPx.left + 0.1)) {
            edges.insert(Edge.Set.trailing)
        }
    } else {
        if (checkEdges.contains(Edge.Set.leading) && (bounds.left <= safeArea.safeBoundsPx.left + 0.1)) {
            edges.insert(Edge.Set.leading)
        }
        if (checkEdges.contains(Edge.Set.trailing) && (bounds.right >= safeArea.safeBoundsPx.right - 0.1)) {
            edges.insert(Edge.Set.trailing)
        }
    }
    return edges.sref()
}

/// Layout the given view with the given padding.
@Composable
internal fun PaddingLayout(content: Renderable, padding: EdgeInsets, context: ComposeContext) {
    PaddingLayout(padding = padding, context = context) { it -> content.Render(it) }
}

@Composable
internal fun PaddingLayout(padding: EdgeInsets, context: ComposeContext, target: @Composable (ComposeContext) -> Unit) {
    ComposeContainer(modifier = context.modifier) { modifier ->
        val density = LocalDensity.current.sref()
        val topPx = with(density) { -> padding.top.dp.roundToPx() }
        val bottomPx = with(density) { -> padding.bottom.dp.roundToPx() }
        val leadingPx = with(density) { -> padding.leading.dp.roundToPx() }
        val trailingPx = with(density) { -> padding.trailing.dp.roundToPx() }
        Layout(modifier = modifier, content = { -> target(context.content()) }) l@{ measurables, constraints ->
            if (measurables.isEmpty()) {
                return@l layout(width = 0, height = 0) { ->  }
            }
            val updatedConstraints = constraints.copy(minWidth = constraint(constraints.minWidth, subtracting = leadingPx + trailingPx), minHeight = constraint(constraints.minHeight, subtracting = topPx + bottomPx), maxWidth = constraint(constraints.maxWidth, subtracting = leadingPx + trailingPx), maxHeight = constraint(constraints.maxHeight, subtracting = topPx + bottomPx))
            val targetPlaceables = measurables.map { it -> it.measure(updatedConstraints) }
            layout(width = targetPlaceables[0].width + leadingPx + trailingPx, height = targetPlaceables[0].height + topPx + bottomPx) { ->
                for (targetPlaceable in targetPlaceables.sref()) {
                    targetPlaceable.placeRelative(x = leadingPx, y = topPx)
                }
            }
        }
    }
}

/// Layout the given view with the given position.
@Composable
internal fun PositionLayout(content: Renderable, x: Double, y: Double, context: ComposeContext) {
    PositionLayout(x = x, y = y, context = context) { it -> content.Render(it) }
}

@Composable
internal fun PositionLayout(x: Double, y: Double, context: ComposeContext, target: @Composable (ComposeContext) -> Unit) {
    // SwiftUI expands to fill the available space and places within that
    Box(modifier = context.modifier.fillSize()) { ->
        val density = LocalDensity.current.sref()
        val xPx = with(density) { -> x.dp.roundToPx() }
        val yPx = with(density) { -> y.dp.roundToPx() }
        Layout(content = { -> target(context.content()) }) l@{ measurables, constraints ->
            if (measurables.isEmpty()) {
                return@l layout(width = 0, height = 0) { ->  }
            }
            val targetPlaceables = measurables.map { it -> it.measure(constraints) }
            layout(width = targetPlaceables[0].width, height = targetPlaceables[0].height) { ->
                for (targetPlaceable in targetPlaceables.sref()) {
                    targetPlaceable.placeRelative(x = xPx - targetPlaceable.width / 2, y = yPx - targetPlaceable.height / 2)
                }
            }
        }
    }
}

private fun constraint(value: Int, subtracting: Int): Int {
    if (value == Int.MAX_VALUE) {
        return value
    }
    return max(0, value - subtracting)
}

private fun placeContent(width: Int, height: Int, inWidth: Int, inHeight: Int, alignment: Alignment): Tuple2<Int, Int> {
    val centerX = (inWidth - width) / 2
    val centerY = (inHeight - height) / 2
    when (alignment) {
        Alignment.leading, Alignment.leadingFirstTextBaseline, Alignment.leadingLastTextBaseline -> return Tuple2(0, centerY)
        Alignment.trailing, Alignment.trailingFirstTextBaseline, Alignment.trailingLastTextBaseline -> return Tuple2(inWidth - width, centerY)
        Alignment.top -> return Tuple2(centerX, 0)
        Alignment.bottom -> return Tuple2(centerX, inHeight - height)
        Alignment.topLeading -> return Tuple2(0, 0)
        Alignment.topTrailing -> return Tuple2(inWidth - width, 0)
        Alignment.bottomLeading -> return Tuple2(0, inHeight - height)
        Alignment.bottomTrailing -> return Tuple2(inWidth - width, inHeight - height)
        else -> return Tuple2(centerX, centerY)
    }
}

