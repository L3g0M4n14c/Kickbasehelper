package skip.ui

import skip.lib.*
import skip.lib.Array
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@androidx.annotation.Keep
class ScrollView: View, Renderable, skip.lib.SwiftProjecting {
    internal val content: ComposeBuilder
    internal val axes: Axis.Set

    constructor(axes: Axis.Set = Axis.Set.vertical, showsIndicators: Boolean = true, content: () -> View) {
        // Note: showsIndicator is ignored
        this.axes = axes.sref()
        this.content = ComposeBuilder.from(content)
    }

    constructor(bridgedAxes: Int, showsIndicators: Boolean, bridgedContent: View) {
        // Note: showsIndicator is ignored
        this.axes = Axis.Set(rawValue = bridgedAxes)
        this.content = ComposeBuilder.from { -> bridgedContent }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun Render(context: ComposeContext) {
        // Some components in Compose have their own scrolling built in
        val builtinScrollAxisSet = rememberSaveable(stateSaver = context.stateSaver as Saver<Preference<Axis.Set>, Any>) { -> mutableStateOf(Preference<Axis.Set>(key = BuiltinScrollAxisSetPreferenceKey::class)) }
        val builtinScrollAxisSetCollector = PreferenceCollector<Axis.Set>(key = BuiltinScrollAxisSetPreferenceKey::class, state = builtinScrollAxisSet)

        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        val isVerticalScroll = axes.contains(Axis.Set.vertical) && !builtinScrollAxisSet.value.reduced.contains(Axis.Set.vertical)
        val isHorizontalScroll = axes.contains(Axis.Set.horizontal) && !builtinScrollAxisSet.value.reduced.contains(Axis.Set.horizontal)
        var scrollModifier: Modifier = Modifier
        var effectiveScrollAxes: Axis.Set = Axis.Set.of()
        if (isVerticalScroll) {
            scrollModifier = scrollModifier.verticalScroll(scrollState)
            effectiveScrollAxes.insert(Axis.Set.vertical)
            if (!axes.contains(Axis.Set.horizontal)) {
                // Integrate with our scroll-to-top navigation bar taps
                PreferenceValues.shared.contribute(context = context, key = ScrollToTopPreferenceKey::class, value = ScrollToTopAction(key = scrollState) { ->
                    coroutineScope.launch { -> scrollState.animateScrollTo(0) }
                })
            }
        }
        if (isHorizontalScroll) {
            scrollModifier = scrollModifier.horizontalScroll(scrollState)
            effectiveScrollAxes.insert(Axis.Set.horizontal)
        }

        val contentContext = context.content()
        ComposeContainer(scrollAxes = effectiveScrollAxes, modifier = context.modifier, fillWidth = axes.contains(Axis.Set.horizontal), fillHeight = axes.contains(Axis.Set.vertical)) { modifier ->
            IgnoresSafeAreaLayout(expandInto = Edge.Set.of(), checkEdges = Edge.Set.of(Edge.Set.bottom), modifier = modifier) { _, safeAreaEdges ->
                var containerModifier: Modifier = Modifier
                if (isVerticalScroll) {
                    containerModifier = containerModifier.fillMaxHeight()
                    if (safeAreaEdges.contains(Edge.Set.bottom)) {
                        PreferenceValues.shared.contribute(context = context, key = ToolbarPreferenceKey::class, value = ToolbarPreferences(scrollableState = scrollState, for_ = arrayOf(ToolbarPlacement.bottomBar)))
                        PreferenceValues.shared.contribute(context = context, key = TabBarPreferenceKey::class, value = ToolbarBarPreferences(scrollableState = scrollState))
                    }
                }
                if (isHorizontalScroll) {
                    containerModifier = containerModifier.fillMaxWidth()
                }

                val refreshing = remember { -> mutableStateOf(false) }
                val refreshAction = EnvironmentValues.shared.refresh
                val refreshState: PullRefreshState?
                if (refreshAction != null) {
                    val updatedAction = rememberUpdatedState(refreshAction)
                    refreshState = rememberPullRefreshState(refreshing.value, { ->
                        coroutineScope.launch { ->
                            refreshing.value = true
                            updatedAction.value()
                            refreshing.value = false
                        }
                    })
                    containerModifier = containerModifier.pullRefresh(refreshState!!)
                } else {
                    refreshState = null
                }
                containerModifier = containerModifier.scrollDismissesKeyboardMode(EnvironmentValues.shared.scrollDismissesKeyboardMode)

                Box(modifier = containerModifier) { ->
                    Column(modifier = scrollModifier) { ->
                        if (isVerticalScroll) {
                            val searchableState = EnvironmentValues.shared._searchableState
                            val isSearchable = searchableState?.isOnNavigationStack == false
                            if (isSearchable) {
                                SearchField(state = searchableState, context = context.content(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)))
                            }
                        }
                        EnvironmentValues.shared.setValues(l@{ it ->
                            it.set_scrollViewAxes(axes)
                            return@l ComposeResult.ok
                        }, in_ = { ->
                            PreferenceValues.shared.collectPreferences(arrayOf(builtinScrollAxisSetCollector)) { -> content.Compose(context = contentContext) }
                        })
                    }
                    if (refreshState != null) {
                        PullRefreshIndicator(refreshing.value, refreshState, Modifier.align(androidx.compose.ui.Alignment.TopCenter))
                    }
                }
            }
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

@androidx.annotation.Keep
class ScrollViewProxy: skip.lib.SwiftProjecting {
    internal val scrollToID: (Any) -> Unit

    fun scrollTo(id: Any, anchor: UnitPoint? = null) {
        // Warning: anchor is currently ignored
        scrollToID(id)
    }

    fun scrollTo(bridgedID: Any, anchorX: Double?, anchorY: Double?) {
        val anchor: UnitPoint?
        if ((anchorX != null) && (anchorY != null)) {
            anchor = UnitPoint(x = anchorX, y = anchorY)
        } else {
            anchor = null
        }
        scrollTo(bridgedID, anchor = anchor)
    }

    internal constructor(scrollToID: (Any) -> Unit) {
        this.scrollToID = scrollToID
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

@androidx.annotation.Keep
class ScrollViewReader: View, Renderable, skip.lib.SwiftProjecting {
    val content: (ScrollViewProxy) -> View

    constructor(content: (ScrollViewProxy) -> View) {
        this.content = content
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val scrollToID = rememberSaveable(stateSaver = context.stateSaver as Saver<Preference<ScrollToIDAction>, Any>) { -> mutableStateOf(Preference<ScrollToIDAction>(key = ScrollToIDPreferenceKey::class)) }
        val scrollToIDCollector = PreferenceCollector<ScrollToIDAction>(key = ScrollToIDPreferenceKey::class, state = scrollToID)
        val scrollProxy = ScrollViewProxy(scrollToID = scrollToID.value.reduced.action)
        PreferenceValues.shared.collectPreferences(arrayOf(scrollToIDCollector)) { -> content(scrollProxy).Compose(context) }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

@androidx.annotation.Keep
internal class ScrollToTopPreferenceKey: PreferenceKey<ScrollToTopAction> {

    @androidx.annotation.Keep
    companion object: PreferenceKeyCompanion<ScrollToTopAction> {
        override val defaultValue = ScrollToTopAction(key = null, action = { ->  })

        override fun reduce(value: InOut<ScrollToTopAction>, nextValue: () -> ScrollToTopAction) {
            value.value = nextValue()
        }
    }
}

internal class ScrollToTopAction {
    // Key the action on the listState/gridState/etc that performs the scrolling, so that on
    // recompose when the remembered state might change, the preference action is updated
    internal val key: Any?
    internal val action: () -> Unit

    override fun equals(other: Any?): Boolean {
        if (other !is ScrollToTopAction) {
            return false
        }
        val lhs = this
        val rhs = other
        return lhs.key == rhs.key
    }

    constructor(key: Any? = null, action: () -> Unit) {
        this.key = key.sref()
        this.action = action
    }
}

@androidx.annotation.Keep
internal class ScrollToIDPreferenceKey: PreferenceKey<ScrollToIDAction> {

    @androidx.annotation.Keep
    companion object: PreferenceKeyCompanion<ScrollToIDAction> {
        override val defaultValue = ScrollToIDAction(key = null, action = { _ ->  })

        override fun reduce(value: InOut<ScrollToIDAction>, nextValue: () -> ScrollToIDAction) {
            value.value = nextValue()
        }
    }
}

internal class ScrollToIDAction {
    // Key the action on the listState/gridState/etc that performs the scrolling, so that on
    // recompose when the remembered state might change, the preference action is updated
    internal val key: Any?
    internal val action: (Any) -> Unit

    override fun equals(other: Any?): Boolean {
        if (other !is ScrollToIDAction) {
            return false
        }
        val lhs = this
        val rhs = other
        return lhs.key == rhs.key
    }

    constructor(key: Any? = null, action: (Any) -> Unit) {
        this.key = key.sref()
        this.action = action
    }
}

@androidx.annotation.Keep
internal class BuiltinScrollAxisSetPreferenceKey: PreferenceKey<Axis.Set> {

    @androidx.annotation.Keep
    companion object: PreferenceKeyCompanion<Axis.Set> {
        override val defaultValue: Axis.Set = Axis.Set.of()

        override fun reduce(value: InOut<Axis.Set>, nextValue: () -> Axis.Set): Unit = value.value.formUnion(nextValue())
    }
}

enum class ScrollBounceBehavior {
    automatic,
    always,
    basedOnSize;

    @androidx.annotation.Keep
    companion object {
    }
}

enum class ScrollDismissesKeyboardMode(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
    automatic(1), // For bridging
    immediately(2), // For bridging
    interactively(3), // For bridging
    never(4); // For bridging

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: Int): ScrollDismissesKeyboardMode? {
            return when (rawValue) {
                1 -> ScrollDismissesKeyboardMode.automatic
                2 -> ScrollDismissesKeyboardMode.immediately
                3 -> ScrollDismissesKeyboardMode.interactively
                4 -> ScrollDismissesKeyboardMode.never
                else -> null
            }
        }
    }
}

fun ScrollDismissesKeyboardMode(rawValue: Int): ScrollDismissesKeyboardMode? = ScrollDismissesKeyboardMode.init(rawValue = rawValue)

enum class ScrollEdgeEffectStyle {
    automatic,
    hard,
    soft;

    @androidx.annotation.Keep
    companion object {
    }
}

enum class ScrollIndicatorVisibility {
    automatic,
    visible,
    hidden,
    never;

    @androidx.annotation.Keep
    companion object {
    }
}

@Suppress("MUST_BE_INITIALIZED")
class ScrollTarget: MutableStruct {
    var rect: CGRect
        get() = field.sref({ this.rect = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var anchor: UnitPoint? = null
        get() = field.sref({ this.anchor = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(rect: CGRect, anchor: UnitPoint? = null) {
        this.rect = rect
        this.anchor = anchor
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as ScrollTarget
        this.rect = copy.rect
        this.anchor = copy.anchor
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = ScrollTarget(this as MutableStruct)

    @androidx.annotation.Keep
    companion object {
    }
}

class PinnedScrollableViews: OptionSet<PinnedScrollableViews, Int>, MutableStruct {
    override var rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override val rawvaluelong: ULong
        get() = ULong(rawValue)
    override fun makeoptionset(rawvaluelong: ULong): PinnedScrollableViews = PinnedScrollableViews(rawValue = Int(rawvaluelong))
    override fun assignoptionset(target: PinnedScrollableViews) {
        willmutate()
        try {
            assignfrom(target)
        } finally {
            didmutate()
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as PinnedScrollableViews
        this.rawValue = copy.rawValue
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = PinnedScrollableViews(this as MutableStruct)

    private fun assignfrom(target: PinnedScrollableViews) {
        this.rawValue = target.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val sectionHeaders = PinnedScrollableViews(rawValue = 1 shl 0) // For bridging
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val sectionFooters = PinnedScrollableViews(rawValue = 1 shl 1) // For bridging

        fun of(vararg options: PinnedScrollableViews): PinnedScrollableViews {
            val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
            return PinnedScrollableViews(rawValue = value)
        }
    }
}

// MARK: ScrollTargetBehavior

interface ScrollTargetBehavior {
}
interface ScrollTargetBehaviorCompanion {
}

class PagingScrollTargetBehavior: ScrollTargetBehavior {
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor() {
    }

    @androidx.annotation.Keep
    companion object: ScrollTargetBehaviorCompanion {

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val paging: PagingScrollTargetBehavior
            get() {
                fatalError()
            }
    }
}

@androidx.annotation.Keep
class ViewAlignedScrollTargetBehavior: ScrollTargetBehavior, skip.lib.SwiftProjecting {
    constructor(limitBehavior: ViewAlignedScrollTargetBehavior.LimitBehavior = ViewAlignedScrollTargetBehavior.LimitBehavior.automatic) {
        // Note: we currently ignore the limit behavior
    }

    constructor(bridgedLimitBehavior: Int) {
        // Note: we currently ignore the limit behavior
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(anchor: UnitPoint?) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(limitBehavior: ViewAlignedScrollTargetBehavior.LimitBehavior, anchor: UnitPoint?) {
    }

    enum class LimitBehavior(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
        automatic(0), // For bridging
        always(1), // For bridging
        never(2), // For bridging
        alwaysByFew(3), // For bridging
        alwaysByOne(4); // For bridging

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: Int): ViewAlignedScrollTargetBehavior.LimitBehavior? {
                return when (rawValue) {
                    0 -> LimitBehavior.automatic
                    1 -> LimitBehavior.always
                    2 -> LimitBehavior.never
                    3 -> LimitBehavior.alwaysByFew
                    4 -> LimitBehavior.alwaysByOne
                    else -> null
                }
            }
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: ScrollTargetBehaviorCompanion {

        val viewAligned: ViewAlignedScrollTargetBehavior
            get() = ViewAlignedScrollTargetBehavior()

        fun viewAligned(limitBehavior: ViewAlignedScrollTargetBehavior.LimitBehavior): ViewAlignedScrollTargetBehavior = ViewAlignedScrollTargetBehavior(limitBehavior = limitBehavior)

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun viewAligned(anchor: UnitPoint?): ViewAlignedScrollTargetBehavior {
            fatalError()
        }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun viewAligned(limitBehavior: ViewAlignedScrollTargetBehavior.LimitBehavior, anchor: UnitPoint?): ViewAlignedScrollTargetBehavior {
            fatalError()
        }

        fun LimitBehavior(rawValue: Int): ViewAlignedScrollTargetBehavior.LimitBehavior? = LimitBehavior.init(rawValue = rawValue)
    }
}

/*
import struct CoreGraphics.CGSize
import struct CoreGraphics.CGVector

// TODO: Process for use in SkipUI

/// The context in which a scroll target behavior updates its scroll target.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
@dynamicMemberLookup public struct ScrollTargetBehaviorContext {

/// The original target when the scroll gesture began.
public var originalTarget: ScrollTarget { get { fatalError() } }

/// The current velocity of the scrollable view's scroll gesture.
public var velocity: CGVector { get { fatalError() } }

/// The size of the content of the scrollable view.
public var contentSize: CGSize { get { fatalError() } }

/// The size of the container of the scrollable view.
///
/// This is the size of the bounds of the scroll view subtracting any
/// insets applied to the scroll view (like the safe area).
public var containerSize: CGSize { get { fatalError() } }

/// The axes in which the scrollable view is scrollable.
public var axes: Axis.Set { get { fatalError() } }

public subscript<T>(dynamicMember keyPath: KeyPath<EnvironmentValues, T>) -> T { get { fatalError() } }
}

/// The configuration of a scroll transition that controls how a transition
/// is applied as a view is scrolled through the visible region of a containing
/// scroll view or other container.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
public struct ScrollTransitionConfiguration {

/// Creates a new configuration that discretely animates the transition
/// when the view becomes visible.
///
/// Unlike the interactive configuration, the transition isn't
/// interpolated as the scroll view is scrolled. Instead, the transition
/// phase only changes once the threshold has been reached, at which
/// time the given animation is used to animate to the new phase.
///
/// - Parameters:
///   - animation: The animation to use when transitioning between states.
///
/// - Returns: A configuration that discretely animates between
///   transition phases.
public static func animated(_ animation: Animation = .default) -> ScrollTransitionConfiguration { fatalError() }

/// Creates a new configuration that discretely animates the transition
/// when the view becomes visible.
public static let animated: ScrollTransitionConfiguration = { fatalError() }()

/// Creates a new configuration that interactively interpolates the
/// transition's effect as the view is scrolled into the visible region
/// of the container.
///
/// - Parameters:
///   - timingCurve: The curve that adjusts the pace at which the effect
///     is interpolated between phases of the transition. For example, an
///     `.easeIn` curve causes interpolation to begin slowly as the view
///     reaches the edge of the scroll view, then speed up as it reaches
///     the visible threshold. The curve is applied 'forward' while the
///     view is appearing, meaning that time zero corresponds to the
///     view being just hidden, and time 1.0 corresponds to the pont at
///     which the view reaches the configuration threshold. This also means
///     that the timing curve is applied in reversed while the view
///     is moving away from the center of the scroll view.
///
/// - Returns: A configuration that interactively interpolates between
///   transition phases based on the current scroll position.
public static func interactive(timingCurve: UnitCurve = .easeInOut) -> ScrollTransitionConfiguration { fatalError() }

/// Creates a new configuration that interactively interpolates the
/// transition's effect as the view is scrolled into the visible region
/// of the container.
public static let interactive: ScrollTransitionConfiguration = { fatalError() }()

/// Creates a new configuration that does not change the appearance of the view.
public static let identity: ScrollTransitionConfiguration = { fatalError() }()

/// Sets the animation with which the transition will be applied.
///
/// If the transition is interactive, the given animation will be used
/// to animate the effect toward the current interpolated value, causing
/// the effect to lag behind the current scroll position.
///
/// - Parameter animation: An animation that will be used to apply the
///   transition to the view.
///
/// - Returns: A copy of this configuration with the animation set to the
///   given value.
public func animation(_ animation: Animation) -> ScrollTransitionConfiguration { fatalError() }

/// Sets the threshold at which the view will be considered fully visible.
///
/// - Parameters:
///   - threshold: The threshold specifying how much of the view must
///     intersect with the container before it is treated as visible.
///
/// - Returns: A copy of this configuration with the threshold set to the
///   given value.
public func threshold(_ threshold: ScrollTransitionConfiguration.Threshold) -> ScrollTransitionConfiguration { fatalError() }
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
extension ScrollTransitionConfiguration {

/// Describes a specific point in the progression of a target view within a container
/// from hidden (fully outside the container) to visible.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
public struct Threshold {

public static let visible: ScrollTransitionConfiguration.Threshold = { fatalError() }()

public static let hidden: ScrollTransitionConfiguration.Threshold = { fatalError() }()

/// The target view is centered within the container
public static var centered: ScrollTransitionConfiguration.Threshold { get { fatalError() } }

/// The target view is visible by the given amount, where zero is fully
/// hidden, and one is fully visible.
///
/// Values less than zero or greater than one are clamped.
public static func visible(_ amount: Double) -> ScrollTransitionConfiguration.Threshold { fatalError() }

/// Creates a new threshold that combines this threshold value with
/// another threshold, interpolated by the given amount.
///
/// - Parameters:
///   - other: The second threshold value.
///   - amount: The ratio with which this threshold is combined with
///     the given threshold, where zero is equal to this threshold,
///     1.0 is equal to `other`, and values in between combine the two
///     thresholds.
public func interpolated(towards other: ScrollTransitionConfiguration.Threshold, amount: Double) -> ScrollTransitionConfiguration.Threshold { fatalError() }

/// Returns a threshold that is met when the target view is closer to the
/// center of the container by `distance`. Use negative values to move
/// the threshold away from the center.
public func inset(by distance: Double) -> ScrollTransitionConfiguration.Threshold { fatalError() }
}
}

/// The phases that a view transitions between when it scrolls among other views.
///
/// When a view with a scroll transition modifier applied is approaching
/// the visible region of the containing scroll view or other container,
/// the effect  will first be applied with the `topLeading` or `bottomTrailing`
/// phase (depending on which edge the view is approaching), then will be
/// moved to the `identity` phase as the view moves into the visible area. The
/// timing and behavior that determines when a view is visible within the
/// container is controlled by the configuration that is provided to the
/// `scrollTransition` modifier.
///
/// In the `identity` phase, scroll transitions should generally not make any
/// visual change to the view they are applied to, since the transition's view
/// modifications in the `identity` phase will be applied to the view as long
/// as it is visible. In the `topLeading` and `bottomTrailing` phases,
/// transitions should apply a change that will be animated to create the
/// transition.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
@frozen public enum ScrollTransitionPhase {

/// The scroll transition is being applied to a view that is about to
/// move into the visible area at the top edge of a vertical scroll view,
/// or the leading edge of a horizont scroll view.
case topLeading

/// The scroll transition is being applied to a view that is in the
/// visible area.
///
/// In this phase, a transition should show its steady state appearance,
/// which will generally not make any visual change to the view.
case identity

/// The scroll transition is being applied to a view that is about to
/// move into the visible area at the bottom edge of a vertical scroll
/// view, or the trailing edge of a horizontal scroll view.
case bottomTrailing

public var isIdentity: Bool { get { fatalError() } }

/// A phase-derived value that can be used to scale or otherwise modify
/// effects.
///
/// Returns -1.0 when in the topLeading phase, zero when in the identity
/// phase, and 1.0 when in the bottomTrailing phase.
public var value: Double { get { fatalError() } }
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
extension ScrollTransitionPhase : Equatable {
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
extension ScrollTransitionPhase : Hashable {
}

/// The scroll behavior that aligns scroll targets to container-based geometry.
///
/// In the following example, every view in the lazy stack is flexible
/// in both directions and the scroll view settles to container-aligned
/// boundaries.
///
///     ScrollView {
///         LazyVStack(spacing: 0.0) {
///             ForEach(items) { item in
///                 FullScreenItem(item)
///             }
///         }
///     }
///     .scrollTargetBehavior(.paging)
///
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
public struct PagingScrollTargetBehavior : ScrollTargetBehavior {

/// Creates a paging scroll behavior.
public init() { fatalError() }

/// Updates the proposed target that a scrollable view should scroll to.
///
/// The system calls this method in two main cases:
/// - When a scroll gesture ends, it calculates where it would naturally
///   scroll to using its deceleration rate. The system
///   provides this calculated value as the target of this method.
/// - When a scrollable view's size changes, it calculates where it should
///   be scrolled given the new size and provides this calculates value
///   as the target of this method.
///
/// You can implement this method to override the calculated target
/// which will have the scrollable view scroll to a different position
/// than it would otherwise.
public func updateTarget(_ target: inout ScrollTarget, context: PagingScrollTargetBehavior.TargetContext) { fatalError() }
}
*/
