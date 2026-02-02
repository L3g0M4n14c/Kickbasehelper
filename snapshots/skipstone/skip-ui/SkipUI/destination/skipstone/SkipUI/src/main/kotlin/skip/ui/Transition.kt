package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

interface Transition {
    //    associatedtype Body : View
    //    @ViewBuilder func body(content: Self.Content, phase: TransitionPhase) -> Self.Body
    //    static var properties: TransitionProperties { get }
    //    typealias Content = PlaceholderContentView<Self>

    @Composable
    fun asEnterTransition(spec: AnimationSpec<Any>): EnterTransition {
        if (spec is FiniteAnimationSpec) {
            return fadeIn(spec as FiniteAnimationSpec<Float>)
        } else {
            return fadeIn()
        }
    }

    @Composable
    fun asExitTransition(spec: AnimationSpec<Any>): ExitTransition {
        if (spec is FiniteAnimationSpec) {
            return fadeOut(spec as FiniteAnimationSpec<Float>)
        } else {
            return fadeOut()
        }
    }

    fun combined(with: Transition): Transition {
        val other = with
        return CombinedTransition(this, other)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun animation(animation: Animation?): Transition {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun apply(content: View, phase: TransitionPhase): View {
        fatalError()
    }

}
interface TransitionCompanion {

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val properties: TransitionProperties
        get() {
            fatalError()
        }
}

class ContentTransition: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ContentTransition) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val identity = ContentTransition(rawValue = 0)
        val opacity = ContentTransition(rawValue = 1)
        val interpolate = ContentTransition(rawValue = 2)
        fun numericText(countsDown: Boolean = false): ContentTransition = ContentTransition(rawValue = 3)
    }
}

@androidx.annotation.Keep
class AnyTransition: skip.lib.SwiftProjecting {
    internal val transition: Transition

    constructor(transition: Transition) {
        this.transition = transition.sref()
    }

    fun combined(with: AnyTransition): AnyTransition {
        val other = with
        return AnyTransition(transition.combined(with = other.transition))
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun animation(animation: Animation?): AnyTransition {
        fatalError()
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {

        fun offset(offset: CGSize): AnyTransition = AnyTransition(OffsetTransition(offset))

        fun offset(x: Double = 0.0, y: Double = 0.0): AnyTransition = AnyTransition(OffsetTransition(CGSize(width = x, height = y)))

        val scale: AnyTransition
            get() = AnyTransition(ScaleTransition(0.5))

        fun scale(scale: Double): AnyTransition = AnyTransition(ScaleTransition(scale))

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun scale(scale: Double, anchor: UnitPoint = UnitPoint.center): AnyTransition {
            fatalError()
        }

        val opacity: AnyTransition
            get() = AnyTransition(OpacityTransition())

        val slide: AnyTransition
            get() = AnyTransition(SlideTransition())

        val identity: AnyTransition
            get() = AnyTransition(IdentityTransition())

        fun move(edge: Edge): AnyTransition = AnyTransition(MoveTransition(edge = edge))

        fun push(from: Edge): AnyTransition {
            val edge = from
            return AnyTransition(PushTransition(edge = edge))
        }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun modifier(active: ViewModifier, identity: ViewModifier): AnyTransition {
            fatalError()
        }

        fun asymmetric(insertion: AnyTransition, removal: AnyTransition): AnyTransition = AnyTransition(AsymmetricTransition(insertion = insertion.transition, removal = removal.transition))
    }
}

enum class TransitionPhase {
    willAppear,
    identity,
    didDisappear;

    val isIdentity: Boolean
        get() = this == TransitionPhase.identity

    val value: Double
        get() {
            when (this) {
                TransitionPhase.willAppear -> return -1.0
                TransitionPhase.identity -> return 0.0
                TransitionPhase.didDisappear -> return 1.0
            }
        }

    @androidx.annotation.Keep
    companion object {
    }
}

class TransitionProperties {
    val hasMotion: Boolean

    constructor(hasMotion: Boolean = true) {
        this.hasMotion = hasMotion
    }

    @androidx.annotation.Keep
    companion object {
    }
}

@androidx.annotation.Keep
@Suppress("MUST_BE_INITIALIZED")
class AsymmetricTransition<Insertion, Removal>: Transition, MutableStruct, skip.lib.SwiftProjecting {
    var insertion: Insertion
        get() = field.sref({ this.insertion = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var removal: Removal
        get() = field.sref({ this.removal = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(insertion: Insertion, removal: Removal) {
        this.insertion = insertion
        this.removal = removal
    }

    //    public func body(content: AsymmetricTransition<Insertion, Removal>.Content, phase: TransitionPhase) -> some View { return stubView() }
    //    public static var properties: TransitionProperties { get { fatalError() } }

    @Composable
    override fun asEnterTransition(spec: AnimationSpec<Any>): EnterTransition = (insertion as Transition).asEnterTransition(spec = spec)

    @Composable
    override fun asExitTransition(spec: AnimationSpec<Any>): ExitTransition = (removal as Transition).asExitTransition(spec = spec)

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as AsymmetricTransition<Insertion, Removal>
        this.insertion = copy.insertion
        this.removal = copy.removal
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = AsymmetricTransition<Insertion, Removal>(this as MutableStruct)

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: TransitionCompanion {
    }
}

@androidx.annotation.Keep
@Suppress("MUST_BE_INITIALIZED")
class CombinedTransition: Transition, MutableStruct, skip.lib.SwiftProjecting {
    var first: Transition
        get() = field.sref({ this.first = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var second: Transition
        get() = field.sref({ this.second = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(first: Transition, second: Transition) {
        this.first = first
        this.second = second
    }

    @Composable
    override fun asEnterTransition(spec: AnimationSpec<Any>): EnterTransition = (first.asEnterTransition(spec = spec) + second.asEnterTransition(spec = spec)).sref()

    @Composable
    override fun asExitTransition(spec: AnimationSpec<Any>): ExitTransition = (first.asExitTransition(spec = spec) + second.asExitTransition(spec = spec)).sref()

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as CombinedTransition
        this.first = copy.first
        this.second = copy.second
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = CombinedTransition(this as MutableStruct)

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: TransitionCompanion {
    }
}

@androidx.annotation.Keep
@Suppress("MUST_BE_INITIALIZED")
class PushTransition: Transition, MutableStruct, skip.lib.SwiftProjecting {
    var edge: Edge
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(edge: Edge) {
        this.edge = edge
    }

    constructor(bridgedEdge: Int): this(edge = Edge(rawValue = bridgedEdge) ?: Edge.leading) {
    }

    //    public func body(content: PushTransition.Content, phase: TransitionPhase) -> some View { return stubView() }

    @Composable
    override fun asEnterTransition(spec: AnimationSpec<Any>): EnterTransition {
        val isRTL = EnvironmentValues.shared.layoutDirection == LayoutDirection.rightToLeft
        if (spec is FiniteAnimationSpec) {
            return (fadeIn(spec as FiniteAnimationSpec<Float>) + slideIn(animationSpec = spec as FiniteAnimationSpec<IntOffset>, initialOffset = { it -> it.offset(edge = edge, isRTL = isRTL) })).sref()
        } else {
            return (fadeIn() + slideIn(initialOffset = { it -> it.offset(edge = edge, isRTL = isRTL) })).sref()
        }
    }

    @Composable
    override fun asExitTransition(spec: AnimationSpec<Any>): ExitTransition {
        val isRTL = EnvironmentValues.shared.layoutDirection == LayoutDirection.rightToLeft
        if (spec is FiniteAnimationSpec) {
            return (fadeOut(spec as FiniteAnimationSpec<Float>) + slideOut(animationSpec = spec as FiniteAnimationSpec<IntOffset>, targetOffset = { it -> it.offsetOpposite(edge = edge, isRTL = isRTL) })).sref()
        } else {
            return (fadeOut() + slideOut(targetOffset = { it -> it.offsetOpposite(edge = edge, isRTL = isRTL) })).sref()
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as PushTransition
        this.edge = copy.edge
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = PushTransition(this as MutableStruct)

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: TransitionCompanion {
    }
}

@androidx.annotation.Keep
@Suppress("MUST_BE_INITIALIZED")
class ScaleTransition: Transition, MutableStruct, skip.lib.SwiftProjecting {
    var scale: Double
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var anchor: UnitPoint
        get() = field.sref({ this.anchor = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(scale: Double, anchor: UnitPoint = UnitPoint.center) {
        this.scale = scale
        this.anchor = anchor
    }

    constructor(scale: Double, anchorX: Double, anchorY: Double): this(scale, anchor = UnitPoint(x = anchorX, y = anchorY)) {
    }

    //    public func body(content: ScaleTransition.Content, phase: TransitionPhase) -> some View { return stubView() }

    @Composable
    override fun asEnterTransition(spec: AnimationSpec<Any>): EnterTransition {
        if (spec is FiniteAnimationSpec) {
            return scaleIn(animationSpec = spec as FiniteAnimationSpec<Float>, initialScale = Float(scale))
        } else {
            return scaleIn(initialScale = Float(scale))
        }
    }

    @Composable
    override fun asExitTransition(spec: AnimationSpec<Any>): ExitTransition {
        if (spec is FiniteAnimationSpec) {
            return scaleOut(animationSpec = spec as FiniteAnimationSpec<Float>, targetScale = Float(scale))
        } else {
            return scaleOut(targetScale = Float(scale))
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as ScaleTransition
        this.scale = copy.scale
        this.anchor = copy.anchor
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = ScaleTransition(this as MutableStruct)

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: TransitionCompanion {
    }
}

@androidx.annotation.Keep
class SlideTransition: Transition, skip.lib.SwiftProjecting {
    constructor() {
    }

    //    public func body(content: SlideTransition.Content, phase: TransitionPhase) -> some View { return stubView() }

    @Composable
    override fun asEnterTransition(spec: AnimationSpec<Any>): EnterTransition {
        val isRTL = EnvironmentValues.shared.layoutDirection == LayoutDirection.rightToLeft
        if (spec is FiniteAnimationSpec) {
            return slideIn(animationSpec = spec as FiniteAnimationSpec<IntOffset>, initialOffset = { it -> it.offset(edge = Edge.leading, isRTL = isRTL) })
        } else {
            return slideIn(initialOffset = { it -> it.offset(edge = Edge.leading, isRTL = isRTL) })
        }
    }

    @Composable
    override fun asExitTransition(spec: AnimationSpec<Any>): ExitTransition {
        val isRTL = EnvironmentValues.shared.layoutDirection == LayoutDirection.rightToLeft
        if (spec is FiniteAnimationSpec) {
            return slideOut(animationSpec = spec as FiniteAnimationSpec<IntOffset>, targetOffset = { it -> it.offset(edge = Edge.trailing, isRTL = isRTL) })
        } else {
            return slideOut(targetOffset = { it -> it.offset(edge = Edge.trailing, isRTL = isRTL) })
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: TransitionCompanion {
    }
}

@androidx.annotation.Keep
class IdentityTransition: Transition, skip.lib.SwiftProjecting {
    constructor() {
    }

    //    public func body(content: IdentityTransition.Content, phase: TransitionPhase) -> Body { fatalError() }
    //    public static let properties: TransitionProperties = { fatalError() }()

    @Composable
    override fun asEnterTransition(spec: AnimationSpec<Any>): EnterTransition = EnterTransition.None.sref()

    @Composable
    override fun asExitTransition(spec: AnimationSpec<Any>): ExitTransition = ExitTransition.None.sref()

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: TransitionCompanion {
    }
}

@androidx.annotation.Keep
@Suppress("MUST_BE_INITIALIZED")
class MoveTransition: Transition, MutableStruct, skip.lib.SwiftProjecting {
    var edge: Edge
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(edge: Edge) {
        this.edge = edge
    }

    constructor(bridgedEdge: Int): this(edge = Edge(rawValue = bridgedEdge) ?: Edge.leading) {
    }

    //    public func body(content: MoveTransition.Content, phase: TransitionPhase) -> some View { return stubView() }

    @Composable
    override fun asEnterTransition(spec: AnimationSpec<Any>): EnterTransition {
        val isRTL = EnvironmentValues.shared.layoutDirection == LayoutDirection.rightToLeft
        if (spec is FiniteAnimationSpec) {
            return slideIn(animationSpec = spec as FiniteAnimationSpec<IntOffset>, initialOffset = { it -> it.offset(edge = edge, isRTL = isRTL) })
        } else {
            return slideIn(initialOffset = { it -> it.offset(edge = edge, isRTL = isRTL) })
        }
    }

    @Composable
    override fun asExitTransition(spec: AnimationSpec<Any>): ExitTransition {
        val isRTL = EnvironmentValues.shared.layoutDirection == LayoutDirection.rightToLeft
        if (spec is FiniteAnimationSpec) {
            return slideOut(animationSpec = spec as FiniteAnimationSpec<IntOffset>, targetOffset = { it -> it.offset(edge = edge, isRTL = isRTL) })
        } else {
            return slideOut(targetOffset = { it -> it.offset(edge = edge, isRTL = isRTL) })
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as MoveTransition
        this.edge = copy.edge
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = MoveTransition(this as MutableStruct)

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: TransitionCompanion {
    }
}

@androidx.annotation.Keep
@Suppress("MUST_BE_INITIALIZED")
class OffsetTransition: Transition, MutableStruct, skip.lib.SwiftProjecting {
    var offset: CGSize
        get() = field.sref({ this.offset = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(offset: CGSize) {
        this.offset = offset
    }

    constructor(offsetWidth: Double, height: Double): this(CGSize(width = offsetWidth, height = height)) {
    }

    //    public func body(content: OffsetTransition.Content, phase: TransitionPhase) -> some View { return stubView() }

    @Composable
    override fun asEnterTransition(spec: AnimationSpec<Any>): EnterTransition {
        val intOffset = with(LocalDensity.current) { -> IntOffset(offset.width.dp.roundToPx(), offset.height.dp.roundToPx()) }
        if (spec is FiniteAnimationSpec) {
            return slideIn(animationSpec = spec as FiniteAnimationSpec<IntOffset>, initialOffset = { _ -> intOffset })
        } else {
            return slideIn(initialOffset = { _ -> intOffset })
        }
    }

    @Composable
    override fun asExitTransition(spec: AnimationSpec<Any>): ExitTransition {
        val intOffset = with(LocalDensity.current) { -> IntOffset(offset.width.dp.roundToPx(), offset.height.dp.roundToPx()) }
        if (spec is FiniteAnimationSpec) {
            return slideOut(animationSpec = spec as FiniteAnimationSpec<IntOffset>, targetOffset = { _ -> intOffset })
        } else {
            return slideOut(targetOffset = { _ -> intOffset })
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as OffsetTransition
        this.offset = copy.offset
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = OffsetTransition(this as MutableStruct)

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: TransitionCompanion {
    }
}

@androidx.annotation.Keep
class OpacityTransition: Transition, skip.lib.SwiftProjecting {

    constructor() {
    }

    //    public func body(content: OpacityTransition.Content, phase: TransitionPhase) -> some View { return stubView() }
    //    public static let properties: TransitionProperties = { fatalError() }()

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: TransitionCompanion {
        internal val shared = OpacityTransition()
    }
}


internal class TransitionModifier: RenderModifier {
    internal val transition: Transition

    internal constructor(transition: Transition): super() {
        this.transition = transition.sref()
    }

    @androidx.annotation.Keep
    companion object {

        /// Extract the transition from the given view's modifiers.
        internal fun transition(for_: Renderable): Transition? {
            val renderable = for_
            return renderable.forEachModifier l@{ it ->
                return@l (it as? TransitionModifier)?.transition
            }
        }
    }
}

internal fun IntSize.offset(edge: Edge, isRTL: Boolean): IntOffset {
    when (edge) {
        Edge.top -> return IntOffset(0, -this.height)
        Edge.bottom -> return IntOffset(0, this.height)
        Edge.leading -> return IntOffset(if (isRTL) this.width else -this.width, 0)
        Edge.trailing -> return IntOffset(if (isRTL) -this.width else this.width, 0)
    }
}

internal fun IntSize.offsetOpposite(edge: Edge, isRTL: Boolean): IntOffset {
    val offset = offset(edge = edge, isRTL = isRTL)
    return IntOffset(offset.x * -1, offset.y * -1)
}
