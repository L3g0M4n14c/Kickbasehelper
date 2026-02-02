package skip.ui

import skip.lib.*

// Copyright 2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.runtime.Composable
import skip.model.StateTracking

class Glass {

    fun tint(color: Color?): Glass = this

    fun interactive(isEnabled: Boolean = true): Glass = this

    override fun equals(other: Any?): Boolean = other is Glass

    @androidx.annotation.Keep
    companion object {
        val regular: Glass
            get() = Glass()
    }
}

class GlassEffectContainer<Content>: View where Content: View {
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(spacing: Double? = null, content: () -> Content) {
    }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext -> EmptyView().Compose(composectx) }
    }

    @androidx.annotation.Keep
    companion object {
    }
}

class GlassEffectTransition {

    @androidx.annotation.Keep
    companion object {
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val matchedGeometry: GlassEffectTransition
            get() = GlassEffectTransition()

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun matchedGeometry(properties: MatchedGeometryProperties = MatchedGeometryProperties.frame, anchor: UnitPoint = UnitPoint.center): GlassEffectTransition = GlassEffectTransition()

        val identity: GlassEffectTransition
            get() = GlassEffectTransition()
    }
}

