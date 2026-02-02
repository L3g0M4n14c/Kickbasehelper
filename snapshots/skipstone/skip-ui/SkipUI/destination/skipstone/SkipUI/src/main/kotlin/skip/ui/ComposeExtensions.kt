package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp

/// Fill available width similar to `.frame(maxWidth: .infinity)`.
///
/// - Seealso: `flexibleWidth(ideal:min:max:)`
@Composable
fun Modifier.fillWidth(): Modifier = flexibleWidth(max = Float.flexibleFill)

/// Fill available height similar to `.frame(maxHeight: .infinity)`.
///
/// - Seealso: `flexibleHeight(ideal:min:max:)`
@Composable
fun Modifier.fillHeight(): Modifier = flexibleHeight(max = Float.flexibleFill)

/// Fill available size similar to `.frame(maxWidth: .infinity, maxHeight: .infinity)`.
///
/// - Seealso: `flexibleWidth(ideal:min:max:)`
/// - Seealso: `flexibleHeight(ideal:min:max:)`
@Composable
fun Modifier.fillSize(): Modifier = fillWidth().fillHeight()

/// SwiftUI-like flexible layout.
///
/// - Seealso: `.frame(minWidth:idealWidth:maxWidth:)`
/// - Warning: Containers with child content should use the `ComposeContainer` composable instead.
@Composable
fun Modifier.flexibleWidth(ideal: Float? = null, min: Float? = null, max: Float? = null): Modifier {
    val matchtarget_0 = EnvironmentValues.shared._flexibleWidth
    if (matchtarget_0 != null) {
        val flexible = matchtarget_0
        return then(flexible(ideal, min, max))
    } else {
        val modifier = if (max == Float.flexibleFill) fillMaxWidth() else this
        return modifier.applyNonExpandingFlexibleWidth(ideal = ideal, min = min, max = max)
    }
}

/// SwiftUI-like flexible layout.
///
///
/// - Seealso: `.frame(minHeight:idealHeight:maxHeight:)`
/// - Warning: Containers with child content should use the `ComposeContainer` composable instead.
@Composable
fun Modifier.flexibleHeight(ideal: Float? = null, min: Float? = null, max: Float? = null): Modifier {
    val matchtarget_1 = EnvironmentValues.shared._flexibleHeight
    if (matchtarget_1 != null) {
        val flexible = matchtarget_1
        return then(flexible(ideal, min, max))
    } else {
        val modifier = if (max == Float.flexibleFill) fillMaxHeight() else this
        return modifier.applyNonExpandingFlexibleHeight(ideal = ideal, min = min, max = max)
    }
}

/// For internal use.
internal fun Modifier.applyNonExpandingFlexibleWidth(ideal: Float? = null, min: Float? = null, max: Float? = null): Modifier {
    if ((min != null) && (min!! > 0f) && (max != null) && (max!! >= 0f)) {
        return requiredWidthIn(min = min!!.dp, max = max!!.dp)
    } else if ((min != null) && (min!! > 0f)) {
        return requiredWidthIn(min = min!!.dp)
    } else if ((max != null) && (max!! >= 0f)) {
        return requiredWidthIn(max = max!!.dp)
    } else {
        return this
    }
}

/// For internal use.
internal fun Modifier.applyNonExpandingFlexibleHeight(ideal: Float? = null, min: Float? = null, max: Float? = null): Modifier {
    if ((min != null) && (min!! > 0f) && (max != null) && (max!! >= 0f)) {
        return requiredHeightIn(min = min!!.dp, max = max!!.dp)
    } else if ((min != null) && (min!! > 0f)) {
        return requiredHeightIn(min = min!!.dp)
    } else if ((max != null) && (max!! >= 0f)) {
        return requiredHeightIn(max = max!!.dp)
    } else {
        return this
    }
}

/// Add padding equivalent to the given safe area.
@Composable
internal fun Modifier.padding(safeArea: SafeArea): Modifier {
    val density = LocalDensity.current.sref()
    val layoutDirection = LocalLayoutDirection.current.sref()
    val top = with(density) { -> (safeArea.safeBoundsPx.top - safeArea.presentationBoundsPx.top).toDp() }
    val left = with(density) { -> (safeArea.safeBoundsPx.left - safeArea.presentationBoundsPx.left).toDp() }
    val bottom = with(density) { -> (safeArea.presentationBoundsPx.bottom - safeArea.safeBoundsPx.bottom).toDp() }
    val right = with(density) { -> (safeArea.presentationBoundsPx.right - safeArea.safeBoundsPx.right).toDp() }
    val start = (if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) right else left).sref()
    val end = (if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) left else right).sref()
    return this.padding(top = top, start = start, bottom = bottom, end = end)
}

/// Invoke the given closure with the modified view's root bounds.
@Composable
internal fun Modifier.onGloballyPositionedInRoot(perform: (Rect) -> Unit): Modifier {
    return this.onGloballyPositioned { it ->
        val bounds = it.boundsInRoot()
        if (bounds != Rect.Zero) {
            perform(bounds)
        }
    }
}

/// Invoke the given closure with the modified view's window bounds.
@Composable
internal fun Modifier.onGloballyPositionedInWindow(perform: (Rect) -> Unit): Modifier {
    return this.onGloballyPositioned { it ->
        val bounds = it.boundsInWindow()
        if (bounds != Rect.Zero) {
            perform(bounds)
        }
    }
}

@Composable
internal fun Modifier.scrollDismissesKeyboardMode(mode: ScrollDismissesKeyboardMode): Modifier {
    if (mode != ScrollDismissesKeyboardMode.immediately && mode != ScrollDismissesKeyboardMode.interactively) {
        return this
    }
    val keyboardController = rememberUpdatedState(LocalSoftwareKeyboardController.current)
    val focusManager = rememberUpdatedState(LocalFocusManager.current)
    val imeInsets = WindowInsets.ime.sref()
    val density = LocalDensity.current.sref()
    val nestedScrollConnection = remember { -> KeyboardDismissingNestedScrollConnection(keyboardController = keyboardController, focusManager = focusManager, imeInsets = imeInsets, density = density) }
    return this.nestedScroll(nestedScrollConnection)
}

/// Convert padding values to edge insets in `dp` units.
@Composable
fun PaddingValues.asEdgeInsets(): EdgeInsets {
    val layoutDirection = (if (EnvironmentValues.shared.layoutDirection == LayoutDirection.rightToLeft) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr).sref()
    val top = Double(calculateTopPadding().value)
    val left = Double(calculateLeftPadding(layoutDirection).value)
    val bottom = Double(calculateBottomPadding().value)
    val right = Double(calculateRightPadding(layoutDirection).value)
    return EdgeInsets(top = top, leading = left, bottom = bottom, trailing = right)
}

