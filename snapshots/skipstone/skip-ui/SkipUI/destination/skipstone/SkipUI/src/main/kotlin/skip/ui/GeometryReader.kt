package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity

@androidx.annotation.Keep
class GeometryReader: View, Renderable, skip.lib.SwiftProjecting {
    val content: (GeometryProxy) -> View

    constructor(content: (GeometryProxy) -> View) {
        this.content = content
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val rememberedGlobalFramePx = remember { -> mutableStateOf<Rect?>(null) }
        Box(modifier = context.modifier.fillSize().onGloballyPositionedInRoot { it -> rememberedGlobalFramePx.value = it }) { ->
            rememberedGlobalFramePx.value.sref()?.let { globalFramePx ->
                val proxy = GeometryProxy(globalFramePx = globalFramePx, density = LocalDensity.current, safeArea = EnvironmentValues.shared._safeArea)
                content(proxy).Compose(context.content())
            }
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

