package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.State
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.Density

/// Use to dismiss the keyboard on scroll.
///
/// - Seealso: `Modifier.scrollDismissesKeyboardMode(_:)`
internal class KeyboardDismissingNestedScrollConnection: NestedScrollConnection {
    internal val keyboardController: State<SoftwareKeyboardController?>
    internal val focusManager: State<FocusManager?>
    internal val imeInsets: WindowInsets
    internal val density: Density

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.Drag) {
            val keyboardIsVisible = imeInsets.getBottom(density) > 0
            if (keyboardIsVisible) {
                keyboardController.value?.hide()
                focusManager.value?.clearFocus()
            }
        }
        return Offset.Zero.sref()
    }

    constructor(keyboardController: State<SoftwareKeyboardController?>, focusManager: State<FocusManager?>, imeInsets: WindowInsets, density: Density) {
        this.keyboardController = keyboardController
        this.focusManager = focusManager
        this.imeInsets = imeInsets.sref()
        this.density = density.sref()
    }
}

