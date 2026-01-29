package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@androidx.annotation.Keep
class Divider: View, Renderable, skip.lib.SwiftProjecting {
    constructor() {
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val dividerColor = Color.separator.colorImpl()
        val modifier: Modifier
        when (EnvironmentValues.shared._layoutAxis) {
            Axis.horizontal -> {
                // If in a horizontal container, create a vertical divider
                modifier = Modifier.width(1.dp).then(context.modifier.fillHeight())
            }
            Axis.vertical, null -> modifier = context.modifier
        }
        androidx.compose.material3.Divider(modifier = modifier, color = dividerColor)
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

