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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@androidx.annotation.Keep
class HStack: View, Renderable, skip.lib.SwiftProjecting {
    internal val alignment: VerticalAlignment
    internal val spacing: Double?
    internal val content: ComposeBuilder
    internal val isBridged: Boolean

    constructor(alignment: VerticalAlignment = VerticalAlignment.center, spacing: Double? = null, content: () -> View) {
        this.alignment = alignment
        this.spacing = spacing
        this.content = ComposeBuilder.from(content)
        this.isBridged = false
    }

    constructor(alignmentKey: String, spacing: Double?, bridgedContent: View) {
        this.alignment = VerticalAlignment(key = alignmentKey)
        this.spacing = if (spacing == null) null else spacing!!
        this.content = ComposeBuilder.from { -> bridgedContent }
        this.isBridged = true
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val renderables = content.Evaluate(context = context, options = 0).filter { it -> !it.isSwiftUIEmptyView }
        val layoutImplementationVersion = EnvironmentValues.shared._layoutImplementationVersion

        var hasSpacers = false
        if (layoutImplementationVersion > 0) {
            // Assign positional default spacing to any Spacer between non-Spacers
            val firstNonSpacerIndex = renderables.indexOfFirst { it -> !(it.strip() is Spacer) }
            val lastNonSpacerIndex = renderables.indexOfLast { it -> !(it.strip() is Spacer) }
            for (i in (firstNonSpacerIndex + 1) until lastNonSpacerIndex) {
                (renderables[i].strip() as? Spacer)?.let { spacer ->
                    hasSpacers = true
                    spacer.positionalMinLength = Companion.defaultSpacing
                }
            }
            hasSpacers = hasSpacers || firstNonSpacerIndex > 0 || (lastNonSpacerIndex > 0 && lastNonSpacerIndex < renderables.size - 1)
        }

        val rowAlignment = alignment.asComposeAlignment()
        val rowArrangement: Arrangement.Horizontal
        // Compose's internal arrangement code puts space between all elements, but we do not want to add space
        // around `Spacers`. So we arrange with no spacing and add our own spacing elements
        val adaptiveSpacing = spacing != 0.0 && hasSpacers
        if (adaptiveSpacing) {
            rowArrangement = Arrangement.spacedBy(0.dp, alignment = androidx.compose.ui.Alignment.CenterHorizontally)
        } else {
            rowArrangement = Arrangement.spacedBy((spacing ?: Companion.defaultSpacing).dp, alignment = androidx.compose.ui.Alignment.CenterHorizontally)
        }

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
            ComposeContainer(axis = Axis.horizontal, modifier = context.modifier) { modifier ->
                if (layoutImplementationVersion == 0) {
                    // Maintain previous layout behavior for users who opt in
                    Row(modifier = modifier, horizontalArrangement = rowArrangement, verticalAlignment = rowAlignment) { ->
                        val flexibleWidthModifier: (Float?, Float?, Float?) -> Modifier = l@{ ideal, min, max ->
                            var modifier: Modifier = Modifier
                            if (max?.isFlexibleExpanding == true) {
                                modifier = modifier.weight(1f) // Only available in Row context
                            }
                            return@l modifier.applyNonExpandingFlexibleWidth(ideal = ideal, min = min, max = max)
                        }
                        EnvironmentValues.shared.setValues(l@{ it ->
                            it.set_flexibleWidthModifier(flexibleWidthModifier)
                            return@l ComposeResult.ok
                        }, in_ = { ->
                            var lastWasSpacer: Boolean? = null
                            for (renderable in renderables.sref()) {
                                lastWasSpacer = RenderSpaced(renderable = renderable, adaptiveSpacing = adaptiveSpacing, lastWasSpacer = lastWasSpacer, layoutImplementationVersion = layoutImplementationVersion, context = contentContext)
                            }
                        })
                    }
                } else {
                    HStackRow(modifier = modifier, horizontalArrangement = rowArrangement, verticalAlignment = rowAlignment) { ->
                        val flexibleWidthModifier: (Float?, Float?, Float?) -> Modifier = l@{ it, it_1, it_2 ->
                            return@l Modifier.flexible(it, it_1, it_2) // Only available in HStackRow context
                        }
                        EnvironmentValues.shared.setValues(l@{ it ->
                            it.set_flexibleWidthModifier(flexibleWidthModifier)
                            return@l ComposeResult.ok
                        }, in_ = { ->
                            var lastWasSpacer: Boolean? = null
                            for (renderable in renderables.sref()) {
                                lastWasSpacer = RenderSpaced(renderable = renderable, adaptiveSpacing = adaptiveSpacing, lastWasSpacer = lastWasSpacer, layoutImplementationVersion = layoutImplementationVersion, context = contentContext)
                            }
                        })
                    }
                }
            }
        } else {
            ComposeContainer(axis = Axis.horizontal, modifier = context.modifier) { modifier ->
                val arguments = AnimatedContentArguments(renderables = renderables, idMap = idMap, ids = ids, rememberedIds = rememberedIds, newIds = newIds, rememberedNewIds = rememberedNewIds, isBridged = isBridged)
                RenderAnimatedContent(context = context, modifier = modifier, arguments = arguments, rowAlignment = rowAlignment, rowArrangement = rowArrangement, adaptiveSpacing = adaptiveSpacing, layoutImplementationVersion = layoutImplementationVersion)
            }
        }
    }

    @Composable
    private fun RenderAnimatedContent(context: ComposeContext, modifier: Modifier, arguments: AnimatedContentArguments, rowAlignment: androidx.compose.ui.Alignment.Vertical, rowArrangement: Arrangement.Horizontal, adaptiveSpacing: Boolean, layoutImplementationVersion: Int) {
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
            if (layoutImplementationVersion == 0) {
                // Maintain previous layout behavior for users who opt in
                Row(horizontalArrangement = rowArrangement, verticalAlignment = rowAlignment) { ->
                    val flexibleWidthModifier: (Float?, Float?, Float?) -> Modifier = l@{ ideal, min, max ->
                        var modifier: Modifier = Modifier
                        if (max?.isFlexibleExpanding == true) {
                            modifier = modifier.weight(1f) // Only available in Row context
                        }
                        return@l modifier.applyNonExpandingFlexibleWidth(ideal = ideal, min = min, max = max)
                    }
                    EnvironmentValues.shared.setValues(l@{ it ->
                        it.set_flexibleWidthModifier(flexibleWidthModifier)
                        return@l ComposeResult.ok
                    }, in_ = { ->
                        var lastWasSpacer: Boolean? = null
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
                            val contentContext = context.content(modifier = modifier)
                            lastWasSpacer = RenderSpaced(renderable = renderable, adaptiveSpacing = adaptiveSpacing, lastWasSpacer = lastWasSpacer, layoutImplementationVersion = layoutImplementationVersion, context = contentContext)
                        }
                    })
                }
            } else {
                HStackRow(horizontalArrangement = rowArrangement, verticalAlignment = rowAlignment) { ->
                    val flexibleWidthModifier: (Float?, Float?, Float?) -> Modifier = l@{ it, it_1, it_2 ->
                        return@l Modifier.flexible(it, it_1, it_2) // Only available in HStackRow context
                    }
                    EnvironmentValues.shared.setValues(l@{ it ->
                        it.set_flexibleWidthModifier(flexibleWidthModifier)
                        return@l ComposeResult.ok
                    }, in_ = { ->
                        var lastWasSpacer: Boolean? = null
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
                            val contentContext = context.content(modifier = modifier)
                            lastWasSpacer = RenderSpaced(renderable = renderable, adaptiveSpacing = adaptiveSpacing, lastWasSpacer = lastWasSpacer, layoutImplementationVersion = layoutImplementationVersion, context = contentContext)
                        }
                    })
                }
            }
        }, label = "HStack")
    }

    @Composable
    private fun RenderSpaced(renderable: Renderable, adaptiveSpacing: Boolean, lastWasSpacer: Boolean?, layoutImplementationVersion: Int, context: ComposeContext): Boolean? {
        if (!adaptiveSpacing) {
            renderable.Render(context = context)
            return null
        }

        // Add spacing before any non-Spacer
        val isSpacer = renderable.strip() is Spacer
        if ((lastWasSpacer != null) && (!lastWasSpacer && !isSpacer)) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width((spacing ?: Companion.defaultSpacing).dp))
        }
        renderable.Render(context = context)
        return isSpacer
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {

        private val defaultSpacing = 8.0
    }
}

/*
/// A horizontal container that you can use in conditional layouts.
///
/// This layout container behaves like an ``HStack``, but conforms to the
/// ``Layout`` protocol so you can use it in the conditional layouts that you
/// construct with ``AnyLayout``. If you don't need a conditional layout, use
/// ``HStack`` instead.
@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
@frozen public struct HStackLayout : Layout {
/// The vertical alignment of subviews.
public var alignment: VerticalAlignment { get { fatalError() } }

/// The distance between adjacent subviews.
///
/// Set this value to `nil` to use default distances between subviews.
public var spacing: CGFloat?

/// Creates a horizontal stack with the specified spacing and vertical
/// alignment.
///
/// - Parameters:
///     - alignment: The guide for aligning the subviews in this stack. It
///       has the same vertical screen coordinate for all subviews.
///     - spacing: The distance between adjacent subviews. Set this value
///       to `nil` to use default distances between subviews.
@inlinable public init(alignment: VerticalAlignment = .center, spacing: CGFloat? = nil) { fatalError() }

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
public typealias Cache = Any

public func makeCache(subviews: Subviews) -> Cache {
fatalError()
}

public func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout Cache) -> CGSize {
fatalError()
}

public func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout Cache) {
}
}

@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
extension HStackLayout : Sendable {
}
*/
