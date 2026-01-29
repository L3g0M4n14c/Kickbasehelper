package skip.ui

import skip.lib.*
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/// Composable to handle sizing and layout in a SwiftUI-like way for containers that compose child content.
///
/// - Seealso: `ComposeFlexibleContainer(...)`
@Composable
fun ComposeContainer(axis: Axis? = null, eraseAxis: Boolean = false, scrollAxes: Axis.Set = Axis.Set.of(), modifier: Modifier = Modifier, fixedWidth: Boolean = false, fillWidth: Boolean = false, fixedHeight: Boolean = false, fillHeight: Boolean = false, then: Modifier = Modifier, content: @Composable (Modifier) -> Unit): Unit = ComposeFlexibleContainer(axis = axis, eraseAxis = eraseAxis, scrollAxes = scrollAxes, modifier = modifier, fixedWidth = fixedWidth, flexibleWidthMax = if (fillWidth) Float.flexibleFill else null, fixedHeight = fixedHeight, flexibleHeightMax = if (fillHeight) Float.flexibleFill else null, then = then, content = content)

/// Composable to handle sizing and layout in a SwiftUI-like way for containers that compose child content.
///
/// In Compose, containers are not perfectly layout neutral. A container that wants to expand must use the proper
/// modifier, rather than relying on its content. Additionally, a single 'fillMaxWidth' child will consume all
/// remaining space, pushing subsequent children out.
///
/// Having to explicitly set a modifier in order to expand within a parent in Compose is problematic for containers that
/// want to fit content. The container only wants to expand if it has content that wants to expand. It can't know this
/// until it composes its content. The code in this function sets triggers on the environment values that we use in
/// flexible layout so that if the container content uses them, the container itself can recompose with the appropriate
/// expansion to match its content. Note that this generally only affects final layout when an expanding child is in a
/// container that is itself in a container, and it has to share space with other members of the parent container.
@Composable
fun ComposeFlexibleContainer(axis: Axis? = null, eraseAxis: Boolean = false, scrollAxes: Axis.Set = Axis.Set.of(), modifier: Modifier = Modifier, fixedWidth: Boolean = false, flexibleWidthIdeal: Float? = null, flexibleWidthMin: Float? = null, flexibleWidthMax: Float? = null, fixedHeight: Boolean = false, flexibleHeightIdeal: Float? = null, flexibleHeightMin: Float? = null, flexibleHeightMax: Float? = null, then: Modifier = Modifier, content: @Composable (Modifier) -> Unit) {
    // Use remembered flexible values to recompose on change
    val contentFlexibleWidthMax = remember { -> mutableStateOf(flexibleWidthMax) }
    val contentFlexibleHeightMax = remember { -> mutableStateOf(flexibleHeightMax) }

    // Create the correct modifier for the current values and content
    var modifier = modifier
    val inheritedLayoutScrollAxes = EnvironmentValues.shared._layoutScrollAxes.sref()
    var totalLayoutScrollAxes = inheritedLayoutScrollAxes.sref()
    if (fixedWidth || flexibleWidthMax?.isFlexibleNonExpandingMax == true || flexibleWidthMin?.isFlexibleNonExpandingMin == true || axis == Axis.vertical) {
        totalLayoutScrollAxes.remove(Axis.Set.horizontal)
    }
    if (!fixedWidth) {
        if (flexibleWidthMax?.isFlexibleExpanding != true && contentFlexibleWidthMax.value?.isFlexibleExpanding == true && inheritedLayoutScrollAxes.contains(Axis.Set.horizontal)) {
            // We must use IntrinsicSize.Max for fills in a scroll direction because Compose's fillMax modifiers
            // have no effect in the scroll direction. Flexible values can influence intrinsic measurement
            val minValue = if (flexibleWidthMin?.isFlexibleNonExpandingMin == true) flexibleWidthMin else null
            val maxValue = if (flexibleWidthMax?.isFlexibleNonExpandingMax == true) flexibleWidthMax else null
            modifier = modifier.flexibleWidth(min = minValue, max = maxValue).width(IntrinsicSize.Max)
        } else {
            val max: Float? = flexibleWidthMax ?: contentFlexibleWidthMax.value
            if (flexibleWidthIdeal != null || flexibleWidthMin != null || max != null) {
                modifier = modifier.flexibleWidth(ideal = flexibleWidthIdeal, min = flexibleWidthMin, max = max)
            }
        }
    }
    if (fixedHeight || flexibleHeightMax?.isFlexibleNonExpandingMax == true || flexibleHeightMin?.isFlexibleNonExpandingMin == true || axis == Axis.horizontal) {
        totalLayoutScrollAxes.remove(Axis.Set.vertical)
    }
    if (!fixedHeight) {
        if (flexibleHeightMax?.isFlexibleExpanding != true && contentFlexibleHeightMax.value?.isFlexibleExpanding == true && inheritedLayoutScrollAxes.contains(Axis.Set.vertical)) {
            // We must use IntrinsicSize.Max for fills in a scroll direction because Compose's fillMax modifiers
            // have no effect in the scroll direction. Flexible values can influence intrinsic measurement
            val minValue = if (flexibleHeightMin?.isFlexibleNonExpandingMin == true) flexibleHeightMin else null
            val maxValue = if (flexibleHeightMax?.isFlexibleNonExpandingMax == true) flexibleHeightMax else null
            modifier = modifier.flexibleHeight(min = minValue, max = maxValue).height(IntrinsicSize.Max)
        } else {
            val max: Float? = flexibleHeightMax ?: contentFlexibleHeightMax.value
            if (flexibleHeightIdeal != null || flexibleHeightMin != null || max != null) {
                modifier = modifier.flexibleHeight(ideal = flexibleHeightIdeal, min = flexibleHeightMin, max = max)
            }
        }
    }

    totalLayoutScrollAxes.formUnion(scrollAxes)
    val inheritedScrollAxes = EnvironmentValues.shared._scrollAxes.sref()
    val totalScrollAxes = inheritedScrollAxes.union(scrollAxes)

    modifier = modifier.then(then)
    EnvironmentValues.shared.setValues(l@{ it ->
        // Setup the initial environment before rendering the container content
        if (axis != null) {
            it.set_layoutAxis(axis)
        } else if (eraseAxis) {
            it.set_layoutAxis(null)
        }
        if (totalLayoutScrollAxes != inheritedLayoutScrollAxes) {
            it.set_layoutScrollAxes(totalLayoutScrollAxes)
        }
        if (totalScrollAxes != inheritedScrollAxes) {
            it.set_scrollAxes(totalScrollAxes)
        }

        // Reset the container layout because this is a new container. A directional container like 'HStack' or 'VStack' will set
        // the correct layout before rendering in the content block below, so that its own children can distribute available space
        it.set_flexibleWidthModifier(null)
        it.set_flexibleHeightModifier(null)

        // Set the 'flexibleWidth' and 'flexibleHeight' blocks to trigger a side effect to update our container's expansion state, which
        // can cause it to recompose and recalculate its own modifier. We must use `SideEffect` or the recomposition never happens
        it.set_flexibleWidth l@{ ideal, min, max ->
            var defaultModifier: Modifier = Modifier
            if (max?.isFlexibleExpanding == true) {
                if (max == Float.flexibleFill) {
                    SideEffect { -> contentFlexibleWidthMax.value = Float.flexibleFill }
                } else if (contentFlexibleWidthMax.value != Float.flexibleFill) {
                    // max must be flexibleSpace or flexibleUnknownWithSpace
                    SideEffect { -> contentFlexibleWidthMax.value = Float.flexibleUnknownWithSpace }
                }
                defaultModifier = Modifier.fillMaxWidth()
            } else if (max != null && contentFlexibleWidthMax.value == null) {
                SideEffect { -> contentFlexibleWidthMax.value = Float.flexibleUnknownNonExpanding }
            }
            return@l EnvironmentValues.shared._flexibleWidthModifier?.invoke(ideal, min, max) ?: defaultModifier.applyNonExpandingFlexibleWidth(ideal = ideal, min = min, max = max)
        }
        it.set_flexibleHeight l@{ ideal, min, max ->
            var defaultModifier: Modifier = Modifier
            if (max?.isFlexibleExpanding == true) {
                if (max == Float.flexibleFill) {
                    SideEffect { -> contentFlexibleHeightMax.value = Float.flexibleFill }
                } else if (contentFlexibleHeightMax.value != Float.flexibleFill) {
                    // max must be flexibleSpace or flexibleUnknownWithSpace
                    SideEffect { -> contentFlexibleHeightMax.value = Float.flexibleUnknownWithSpace }
                }
                defaultModifier = Modifier.fillMaxHeight()
            } else if (max != null && contentFlexibleHeightMax.value == null) {
                SideEffect { -> contentFlexibleHeightMax.value = Float.flexibleUnknownNonExpanding }
            }
            return@l EnvironmentValues.shared._flexibleHeightModifier?.invoke(ideal, min, max) ?: defaultModifier.applyNonExpandingFlexibleHeight(ideal = ideal, min = min, max = max)
        }
        return@l ComposeResult.ok
    }, in_ = { ->
        // Render the container content with the above environment setup
        content(modifier)
    })
}
