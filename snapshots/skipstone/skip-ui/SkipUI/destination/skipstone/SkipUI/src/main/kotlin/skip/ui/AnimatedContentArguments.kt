package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

/// Used in our containers to prevent recomposing animated content unnecessarily.
@Stable
internal class AnimatedContentArguments {
    internal val renderables: kotlin.collections.List<Renderable>
    internal val idMap: (Renderable) -> Any?
    internal val ids: kotlin.collections.List<Any>
    internal val rememberedIds: MutableSet<Any>
    internal val newIds: kotlin.collections.List<Any>
    internal val rememberedNewIds: MutableSet<Any>
    internal val isBridged: Boolean

    override fun equals(other: Any?): Boolean {
        if (other !is AnimatedContentArguments) {
            return false
        }
        val lhs = this
        val rhs = other
        if (isBridged) {
            return lhs === rhs
        }
        return lhs.ids == rhs.ids && lhs.rememberedIds == rhs.rememberedIds && lhs.newIds == rhs.newIds && lhs.rememberedNewIds == rhs.rememberedNewIds
    }

    constructor(renderables: kotlin.collections.List<Renderable>, idMap: (Renderable) -> Any?, ids: kotlin.collections.List<Any>, rememberedIds: MutableSet<Any>, newIds: kotlin.collections.List<Any>, rememberedNewIds: MutableSet<Any>, isBridged: Boolean) {
        this.renderables = renderables.sref()
        this.idMap = idMap
        this.ids = ids.sref()
        this.rememberedIds = rememberedIds.sref()
        this.newIds = newIds.sref()
        this.rememberedNewIds = rememberedNewIds.sref()
        this.isBridged = isBridged
    }
}
