package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@androidx.annotation.Keep
class ZStack: View, Renderable, skip.lib.SwiftProjecting {
    internal val alignment: Alignment
    internal val content: ComposeBuilder
    internal val isBridged: Boolean

    constructor(alignment: Alignment = Alignment.center, content: () -> View) {
        this.alignment = alignment.sref()
        this.content = ComposeBuilder.from(content)
        this.isBridged = false
    }

    constructor(horizontalAlignmentKey: String, verticalAlignmentKey: String, bridgedContent: View) {
        this.alignment = Alignment(horizontal = HorizontalAlignment(key = horizontalAlignmentKey), vertical = VerticalAlignment(key = verticalAlignmentKey))
        this.content = ComposeBuilder.from { -> bridgedContent }
        this.isBridged = true
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val renderables = content.Evaluate(context = context, options = 0).filter { it -> !it.isSwiftUIEmptyView }
        val idMap: (Renderable) -> Any? = { it ->
            TagModifier.on(content = it, role = ModifierRole.id)?.value
        }
        val ids = renderables.mapNotNull(idMap)
        val rememberedIds = remember { -> mutableSetOf<Any>() }
        val newIds = ids.filter { it -> !rememberedIds.contains(it) }
        val rememberedNewIds = remember { -> mutableSetOf<Any>() }

        rememberedNewIds.addAll(newIds)
        rememberedIds.clear()
        rememberedIds.addAll(ids)

        if (ids.size < renderables.size) {
            rememberedNewIds.clear()
            val contentContext = context.content()
            ComposeContainer(eraseAxis = true, modifier = context.modifier) { modifier ->
                Box(modifier = modifier, contentAlignment = alignment.asComposeAlignment()) { ->
                    for (renderable in renderables.sref()) {
                        renderable.Render(context = contentContext)
                    }
                }
            }
        } else {
            ComposeContainer(eraseAxis = true, modifier = context.modifier) { modifier ->
                val arguments = AnimatedContentArguments(renderables = renderables, idMap = idMap, ids = ids, rememberedIds = rememberedIds, newIds = newIds, rememberedNewIds = rememberedNewIds, isBridged = isBridged)
                RenderAnimatedContent(context = context, modifier = modifier, arguments = arguments)
            }
        }
    }

    @Composable
    private fun RenderAnimatedContent(context: ComposeContext, modifier: Modifier, arguments: AnimatedContentArguments) {
        AnimatedContent(modifier = modifier, targetState = arguments.renderables, transitionSpec = { ->
            EnterTransition.None.togetherWith(ExitTransition.None).using(SizeTransform(clip = false) { initialSize, targetSize ->
                if (initialSize.width <= 0 || initialSize.height <= 0) {
                    // When starting at zero size, immediately go to target size so views animate into proper place
                    snap()
                } else if (targetSize.width > initialSize.width || targetSize.height > initialSize.height) {
                    // Animate expansion so views slide into place
                    tween()
                } else {
                    // Delay contraction to give old view time to leave
                    snap(delayMillis = Int(defaultAnimationDuration * 1000))
                }
            })
        }, contentKey = { it -> it.map(arguments.idMap) }, content = { state ->
            val animation = Animation.current(isAnimating = transition.isRunning)
            if (animation == null) {
                arguments.rememberedNewIds.clear()
            }
            Box(contentAlignment = alignment.asComposeAlignment()) { ->
                for (renderable in state.sref()) {
                    val id = arguments.idMap(renderable)
                    var modifier: Modifier = Modifier
                    if ((animation != null) && (arguments.newIds.contains(id) || arguments.rememberedNewIds.contains(id) || !arguments.ids.contains(id))) {
                        val transition = TransitionModifier.transition(for_ = renderable) ?: OpacityTransition.shared
                        val spec = animation.asAnimationSpec()
                        val enter = transition.asEnterTransition(spec = spec)
                        val exit = transition.asExitTransition(spec = spec)
                        modifier = modifier.animateEnterExit(enter = enter, exit = exit)
                    }
                    renderable.Render(context = context.content(modifier = modifier))
                }
            }
        }, label = "ZStack")
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

/*
/// An overlaying container that you can use in conditional layouts.
///
/// This layout container behaves like a ``ZStack``, but conforms to the
/// ``Layout`` protocol so you can use it in the conditional layouts that you
/// construct with ``AnyLayout``. If you don't need a conditional layout, use
/// ``ZStack`` instead.
@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
@frozen public struct ZStackLayout : Layout {
/// The alignment of subviews.
public var alignment: Alignment { get { fatalError() } }

/// Creates a stack with the specified alignment.
///
/// - Parameters:
///   - alignment: The guide for aligning the subviews in this stack
///     on both the x- and y-axes.
@inlinable public init(alignment: Alignment = .center) { fatalError() }

/// The type defining the data to animate.
public typealias AnimatableData = EmptyAnimatableData
public var animatableData: AnimatableData { get { fatalError() } set { } }

/// Cached values associated with the layout instance.
///
/// If you create a cache for your custom layout, you can use
/// a type alias to define this type as your data storage type.
/// Alternatively, you can refer to the data storage type directly in all
/// the places where you work with the cache.
///
/// See ``makeCache(subviews:)-23agy`` for more information.
public typealias Cache = Void

public func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout Void) -> CGSize {
fatalError()
}

public func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout Void) {
fatalError()
}
}

@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
extension ZStackLayout : Sendable {
}
*/
