// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

package skip.ui

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*


import skip.lib.*

/// The result of composing content.
///
/// Reserved for future use. Having a return value also expands recomposition scope. See `ComposeBuilder` for details.
class ComposeResult {

    @androidx.annotation.Keep
    companion object {
        val ok = ComposeResult()
    }
}
