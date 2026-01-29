package skip.ui

import skip.lib.*
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints

@androidx.annotation.Keep
class ViewThatFits: View, Renderable, skip.lib.SwiftProjecting {
    internal val axes: Axis.Set
    internal val content: ComposeBuilder

    constructor(in_: Axis.Set = Axis.Set.of(Axis.Set.horizontal, Axis.Set.vertical), content: () -> View) {
        val axes = in_
        this.axes = axes.sref()
        this.content = ComposeBuilder.from(content)
    }

    constructor(bridgedAxes: Int, bridgedContent: View) {
        this.axes = Axis.Set(rawValue = bridgedAxes)
        this.content = ComposeBuilder.from { -> bridgedContent }
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val candidates = content.Evaluate(context = context, options = 0).filter { it -> !it.isSwiftUIEmptyView }
        if (candidates.isEmpty()) {
            return
        }

        val contentContext = context.content()
        ComposeContainer(modifier = context.modifier) { modifier ->
            Layout(modifier = modifier, content = { ->
                for (candidate in candidates.sref()) {
                    candidate.Render(context = contentContext)
                }
            }) l@{ measurables, constraints ->
                if (measurables.isEmpty()) {
                    return@l layout(width = 0, height = 0) { ->  }
                }

                val maxWidth = constraints.maxWidth.sref()
                val maxHeight = constraints.maxHeight.sref()

                fun fits(measuredWidth: Int, measuredHeight: Int): Boolean {
                    if (axes.contains(Axis.Set.horizontal) && maxWidth != Constraints.Infinity && measuredWidth > maxWidth) {
                        return false
                    }
                    if (axes.contains(Axis.Set.vertical) && maxHeight != Constraints.Infinity && measuredHeight > maxHeight) {
                        return false
                    }
                    return true
                }

                // Determine each candidate's ideal size using intrinsics with an unconstrained cross-axis,
                // then pick the first that fits within the parent constraints in the specified axes.
                var chosenIndex = measurables.size - 1
                for (i in 0 until measurables.size) {
                    val idealWidth = measurables[i].maxIntrinsicWidth(Constraints.Infinity)
                    val idealHeight = measurables[i].maxIntrinsicHeight(Constraints.Infinity)
                    if (fits(measuredWidth = idealWidth, measuredHeight = idealHeight)) {
                        chosenIndex = i
                        break
                    }
                }

                val placeable = measurables[chosenIndex].measure(constraints)
                val layoutWidth = max(constraints.minWidth, placeable.width)
                val layoutHeight = max(constraints.minHeight, placeable.height)
                return@l layout(width = layoutWidth, height = layoutHeight) { -> placeable.placeRelative(x = 0, y = 0) }
            }
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

