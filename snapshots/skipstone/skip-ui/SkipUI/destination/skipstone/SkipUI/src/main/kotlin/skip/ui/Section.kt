package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.runtime.Composable

@androidx.annotation.Keep
class Section: View, skip.lib.SwiftProjecting {
    internal val header: ComposeBuilder?
    internal val footer: ComposeBuilder?
    internal val content: ComposeBuilder

    constructor(content: () -> View, header: () -> View, footer: () -> View) {
        this.header = ComposeBuilder.from(header)
        this.footer = ComposeBuilder.from(footer)
        this.content = ComposeBuilder.from(content)
    }

    constructor(content: () -> View, footer: () -> View, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) {
        this.header = null
        this.footer = ComposeBuilder.from(footer)
        this.content = ComposeBuilder.from(content)
    }

    constructor(content: () -> View, header: () -> View) {
        this.header = ComposeBuilder.from(header)
        this.footer = null
        this.content = ComposeBuilder.from(content)
    }

    constructor(header: View, content: () -> View) {
        this.header = ComposeBuilder.from({ -> header })
        this.footer = null
        this.content = ComposeBuilder.from(content)
    }

    constructor(content: () -> View) {
        this.header = null
        this.footer = null
        this.content = ComposeBuilder.from(content)
    }

    constructor(titleKey: LocalizedStringKey, content: () -> View): this(content = content, header = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleKey).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleResource: LocalizedStringResource, content: () -> View): this(content = content, header = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleResource).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(title: String, content: () -> View): this(content = content, header = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(verbatim = title).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(titleKey: LocalizedStringKey, isExpanded: Binding<Boolean>, content: () -> View): this(titleKey, content = content) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(titleResource: LocalizedStringResource, isExpanded: Binding<Boolean>, content: () -> View): this(titleResource, content = content) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(title: String, isExpanded: Binding<Boolean>, content: () -> View): this(title, content = content) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(isExpanded: Binding<Boolean>, content: () -> View, header: () -> View): this(content = content, header = header) {
    }

    constructor(bridgedContent: View, bridgedHeader: View?, bridgedFooter: View?) {
        this.content = ComposeBuilder.from { -> bridgedContent }
        this.header = if (bridgedHeader == null) null else ComposeBuilder.from { -> bridgedHeader!! }
        this.footer = if (bridgedFooter == null) null else ComposeBuilder.from { -> bridgedFooter!! }
    }

    @Composable
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val isLazy = EvaluateOptions(options).lazyItemLevel != null
        var renderables: kotlin.collections.MutableList<Renderable> = mutableListOf()
        val headerRenderables = header?.Evaluate(context = context, options = 0)
        if (isLazy) {
            renderables.add(LazySectionHeader(content = headerRenderables?.firstOrNull() ?: EmptyView()))
        } else if (headerRenderables != null) {
            renderables.addAll(headerRenderables)
        }
        renderables.addAll(content.Evaluate(context = context, options = options))
        val footerRenderables = footer?.Evaluate(context = context, options = 0)
        if (isLazy) {
            renderables.add(LazySectionFooter(content = footerRenderables?.firstOrNull() ?: EmptyView()))
        } else if (footerRenderables != null) {
            renderables.addAll(footerRenderables)
        }
        return renderables.sref()
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

