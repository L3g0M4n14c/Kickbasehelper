package skip.ui

import skip.lib.*
import skip.lib.Array
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import org.burnoutcrew.reorderable.awaitPointerSlopOrCancellation
import kotlin.math.abs

interface Gesture<V>: BridgedGesture {
    val modified: ModifiedGesture<V>
        get() = ModifiedGesture(gesture = this)

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun exclusively(before: Gesture<*>): Gesture<V> {
        val other = before
        return this.sref()
    }

    // Skip can't distinguish between this and the other onEnded variant
    //    @available(*, unavailable)
    //    public func onEnded(_ action: @escaping () -> Void) -> any Gesture<V> {
    //        return self
    //    }

    fun onEnded(action: (V) -> Unit): Gesture<V> {
        var gesture = this.modified.sref()
        gesture.onEnded.append(action)
        return gesture.sref()
    }

    // Skip can't distinguish between this and the other onChanged variant
    //    @available(*, unavailable)
    //    public func onChanged(_ action: @escaping () -> Void) -> any Gesture<V> {
    //        return self
    //    }

    fun onChanged(action: (V) -> Unit): Gesture<V> {
        var gesture = this.modified.sref()
        gesture.onChanged.append(action)
        return gesture.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun map(body: () -> Any): Gesture<V> = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun map(body: (Any) -> Any): Gesture<V> = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun sequenced(before: Gesture<*>): Gesture<V> {
        val other = before
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun simultaneously(with: Gesture<*>): Gesture<V> {
        val other = with
        return this.sref()
    }

}

interface BridgedGesture {

    fun onChangedBool(bridgedAction: (Boolean) -> Unit): BridgedGesture = (this as Gesture<Boolean>).onChanged(bridgedAction)

    fun onEndedBool(bridgedAction: (Boolean) -> Unit): BridgedGesture = (this as Gesture<Boolean>).onEnded(bridgedAction)

    fun onChangedCGFloat(bridgedAction: (Double) -> Unit): BridgedGesture = (this as Gesture<Double>).onChanged(bridgedAction)

    fun onEndedCGFloat(bridgedAction: (Double) -> Unit): BridgedGesture = (this as Gesture<Double>).onEnded(bridgedAction)

    fun onChangedDragGestureValue(bridgedAction: (DragGestureValue) -> Unit): BridgedGesture {
        val action: (DragGesture.Value) -> Unit = { it -> bridgedAction(DragGestureValue(it)) }
        return (this as Gesture<DragGesture.Value>).onChanged(action)
    }

    fun onEndedDragGestureValue(bridgedAction: (DragGestureValue) -> Unit): BridgedGesture {
        val action: (DragGesture.Value) -> Unit = { it -> bridgedAction(DragGestureValue(it)) }
        return (this as Gesture<DragGesture.Value>).onEnded(action)
    }

    fun onChangedMagnifyGestureValue(bridgedAction: (MagnifyGestureValue) -> Unit): BridgedGesture {
        val action: (MagnifyGesture.Value) -> Unit = { it -> bridgedAction(MagnifyGestureValue(it)) }
        return (this as Gesture<MagnifyGesture.Value>).onChanged(action)
    }

    fun onEndedMagnifyGestureValue(bridgedAction: (MagnifyGestureValue) -> Unit): BridgedGesture {
        val action: (MagnifyGesture.Value) -> Unit = { it -> bridgedAction(MagnifyGestureValue(it)) }
        return (this as Gesture<MagnifyGesture.Value>).onEnded(action)
    }

    fun onChangedRotateGestureValue(bridgedAction: (RotateGestureValue) -> Unit): BridgedGesture {
        val action: (RotateGesture.Value) -> Unit = { it -> bridgedAction(RotateGestureValue(it)) }
        return (this as Gesture<RotateGesture.Value>).onChanged(action)
    }

    fun onEndedRotateGestureValue(bridgedAction: (RotateGestureValue) -> Unit): BridgedGesture {
        val action: (RotateGesture.Value) -> Unit = { it -> bridgedAction(RotateGestureValue(it)) }
        return (this as Gesture<RotateGesture.Value>).onEnded(action)
    }

    fun onChangedVoid(bridgedAction: () -> Unit): BridgedGesture {
        val action: (Unit) -> Unit = { _ -> bridgedAction() }
        return (this as Gesture<Unit>).onChanged(action)
    }

    fun onEndedVoid(bridgedAction: () -> Unit): BridgedGesture {
        val action: (Unit) -> Unit = { _ -> bridgedAction() }
        return (this as Gesture<Unit>).onEnded(action)
    }
}

@androidx.annotation.Keep
@Suppress("MUST_BE_INITIALIZED")
class DragGesture: Gesture<DragGesture.Value>, BridgedGesture, MutableStruct, skip.lib.SwiftProjecting {

    @Suppress("MUST_BE_INITIALIZED")
    class Value: MutableStruct {
        var time: Date
            get() = field.sref({ this.time = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }
        var location: CGPoint
            get() = field.sref({ this.location = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }
        var startLocation: CGPoint
            get() = field.sref({ this.startLocation = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }
        var translation: CGSize
            get() = field.sref({ this.translation = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }
        var velocity: CGSize
            get() = field.sref({ this.velocity = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }
        var predictedEndLocation: CGPoint
            get() = field.sref({ this.predictedEndLocation = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }
        var predictedEndTranslation: CGSize
            get() = field.sref({ this.predictedEndTranslation = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }

        constructor(time: Date, location: CGPoint, startLocation: CGPoint, translation: CGSize, velocity: CGSize, predictedEndLocation: CGPoint, predictedEndTranslation: CGSize) {
            this.time = time
            this.location = location
            this.startLocation = startLocation
            this.translation = translation
            this.velocity = velocity
            this.predictedEndLocation = predictedEndLocation
            this.predictedEndTranslation = predictedEndTranslation
        }

        override var supdate: ((Any) -> Unit)? = null
        override var smutatingcount = 0
        override fun scopy(): MutableStruct = DragGesture.Value(time, location, startLocation, translation, velocity, predictedEndLocation, predictedEndTranslation)

        override fun equals(other: Any?): Boolean {
            if (other !is DragGesture.Value) return false
            return time == other.time && location == other.location && startLocation == other.startLocation && translation == other.translation && velocity == other.velocity && predictedEndLocation == other.predictedEndLocation && predictedEndTranslation == other.predictedEndTranslation
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    var minimumDistance: Double
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var coordinateSpace: CoordinateSpaceProtocol
        get() = field.sref({ this.coordinateSpace = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(minimumDistance: Double = 10.0, coordinateSpace: CoordinateSpaceProtocol = LocalCoordinateSpace.local) {
        this.minimumDistance = minimumDistance
        this.coordinateSpace = coordinateSpace
    }

    constructor(minimumDistance: Double, bridgedCoordinateSpace: Int, name: Any?) {
        this.minimumDistance = minimumDistance
        this.coordinateSpace = CoordinateSpaceProtocolFrom(bridged = bridgedCoordinateSpace, name = name as? AnyHashable)
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as DragGesture
        this.minimumDistance = copy.minimumDistance
        this.coordinateSpace = copy.coordinateSpace
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = DragGesture(this as MutableStruct)

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

@androidx.annotation.Keep
class DragGestureValue: skip.lib.SwiftProjecting {
    val time: Double
    val locationX: Double
    val locationY: Double
    val startLocationX: Double
    val startLocationY: Double
    val velocityWidth: Double
    val velocityHeight: Double
    val predictedEndLocationX: Double
    val predictedEndLocationY: Double

    internal constructor(value: DragGesture.Value) {
        this.time = value.time.timeIntervalSinceReferenceDate
        this.locationX = value.location.x
        this.locationY = value.location.y
        this.startLocationX = value.startLocation.x
        this.startLocationY = value.startLocation.y
        this.velocityWidth = value.velocity.width
        this.velocityHeight = value.velocity.height
        this.predictedEndLocationX = value.predictedEndLocation.x
        this.predictedEndLocationY = value.predictedEndLocation.y
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

@androidx.annotation.Keep
@Suppress("MUST_BE_INITIALIZED")
class TapGesture: Gesture<Unit>, BridgedGesture, MutableStruct, skip.lib.SwiftProjecting {

    var count: Int
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var coordinateSpace: CoordinateSpaceProtocol
        get() = field.sref({ this.coordinateSpace = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(count: Int = 1, coordinateSpace: CoordinateSpaceProtocol = LocalCoordinateSpace.local) {
        this.count = count
        this.coordinateSpace = coordinateSpace
    }

    constructor(count: Int, bridgedCoordinateSpace: Int, name: Any?) {
        this.count = count
        this.coordinateSpace = CoordinateSpaceProtocolFrom(bridged = bridgedCoordinateSpace, name = name as? AnyHashable)
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as TapGesture
        this.count = copy.count
        this.coordinateSpace = copy.coordinateSpace
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = TapGesture(this as MutableStruct)

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

@androidx.annotation.Keep
@Suppress("MUST_BE_INITIALIZED")
class LongPressGesture: Gesture<Boolean>, BridgedGesture, MutableStruct, skip.lib.SwiftProjecting {

    var minimumDuration: Double
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var maximumDistance: Double
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(minimumDuration: Double = 0.5, maximumDistance: Double = 10.0) {
        this.minimumDuration = minimumDuration
        this.maximumDistance = maximumDistance
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as LongPressGesture
        this.minimumDuration = copy.minimumDuration
        this.maximumDistance = copy.maximumDistance
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = LongPressGesture(this as MutableStruct)

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

@androidx.annotation.Keep
@Suppress("MUST_BE_INITIALIZED")
class MagnifyGesture: Gesture<MagnifyGesture.Value>, BridgedGesture, MutableStruct, skip.lib.SwiftProjecting {

    @Suppress("MUST_BE_INITIALIZED")
    class Value: MutableStruct {
        var time: Date
            get() = field.sref({ this.time = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }
        var magnification: Double
            set(newValue) {
                willmutate()
                field = newValue
                didmutate()
            }
        var velocity: Double
            set(newValue) {
                willmutate()
                field = newValue
                didmutate()
            }
        var startAnchor: UnitPoint
            get() = field.sref({ this.startAnchor = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }
        var startLocation: CGPoint
            get() = field.sref({ this.startLocation = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }

        constructor(time: Date, magnification: Double, velocity: Double, startAnchor: UnitPoint, startLocation: CGPoint) {
            this.time = time
            this.magnification = magnification
            this.velocity = velocity
            this.startAnchor = startAnchor
            this.startLocation = startLocation
        }

        override var supdate: ((Any) -> Unit)? = null
        override var smutatingcount = 0
        override fun scopy(): MutableStruct = MagnifyGesture.Value(time, magnification, velocity, startAnchor, startLocation)

        override fun equals(other: Any?): Boolean {
            if (other !is MagnifyGesture.Value) return false
            return time == other.time && magnification == other.magnification && velocity == other.velocity && startAnchor == other.startAnchor && startLocation == other.startLocation
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    var minimumScaleDelta: Double
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(minimumScaleDelta: Double = 0.01) {
        this.minimumScaleDelta = minimumScaleDelta
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as MagnifyGesture
        this.minimumScaleDelta = copy.minimumScaleDelta
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = MagnifyGesture(this as MutableStruct)

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

@androidx.annotation.Keep
class MagnifyGestureValue: skip.lib.SwiftProjecting {
    val time: Double
    val magnification: Double
    val velocity: Double
    val startAnchorX: Double
    val startAnchorY: Double
    val startLocationX: Double
    val startLocationY: Double

    internal constructor(value: MagnifyGesture.Value) {
        this.time = value.time.timeIntervalSinceReferenceDate
        this.magnification = value.magnification
        this.velocity = value.velocity
        this.startAnchorX = value.startAnchor.x
        this.startAnchorY = value.startAnchor.y
        this.startLocationX = value.startLocation.x
        this.startLocationY = value.startLocation.y
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

@androidx.annotation.Keep
@Suppress("MUST_BE_INITIALIZED")
class RotateGesture: Gesture<RotateGesture.Value>, BridgedGesture, MutableStruct, skip.lib.SwiftProjecting {

    @Suppress("MUST_BE_INITIALIZED")
    class Value: MutableStruct {
        var time: Date
            get() = field.sref({ this.time = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }
        var rotation: Angle
            get() = field.sref({ this.rotation = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }
        var velocity: Angle
            get() = field.sref({ this.velocity = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }
        var startAnchor: UnitPoint
            get() = field.sref({ this.startAnchor = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }
        var startLocation: CGPoint
            get() = field.sref({ this.startLocation = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }

        constructor(time: Date, rotation: Angle, velocity: Angle, startAnchor: UnitPoint, startLocation: CGPoint) {
            this.time = time
            this.rotation = rotation
            this.velocity = velocity
            this.startAnchor = startAnchor
            this.startLocation = startLocation
        }

        override var supdate: ((Any) -> Unit)? = null
        override var smutatingcount = 0
        override fun scopy(): MutableStruct = RotateGesture.Value(time, rotation, velocity, startAnchor, startLocation)

        override fun equals(other: Any?): Boolean {
            if (other !is RotateGesture.Value) return false
            return time == other.time && rotation == other.rotation && velocity == other.velocity && startAnchor == other.startAnchor && startLocation == other.startLocation
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    var minimumAngleDelta: Angle
        get() = field.sref({ this.minimumAngleDelta = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(minimumAngleDelta: Angle = Angle.degrees(1.0)) {
        this.minimumAngleDelta = minimumAngleDelta
    }

    constructor(minimumAngleDegreesDelta: Double) {
        this.minimumAngleDelta = Angle.degrees(minimumAngleDegreesDelta)
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as RotateGesture
        this.minimumAngleDelta = copy.minimumAngleDelta
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = RotateGesture(this as MutableStruct)

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

@androidx.annotation.Keep
class RotateGestureValue: skip.lib.SwiftProjecting {
    val time: Double
    val rotationDegrees: Double
    val velocityDegrees: Double
    val startAnchorX: Double
    val startAnchorY: Double
    val startLocationX: Double
    val startLocationY: Double

    internal constructor(value: RotateGesture.Value) {
        this.time = value.time.timeIntervalSinceReferenceDate
        this.rotationDegrees = value.rotation.degrees
        this.velocityDegrees = value.velocity.degrees
        this.startAnchorX = value.startAnchor.x
        this.startAnchorY = value.startAnchor.y
        this.startLocationX = value.startLocation.x
        this.startLocationY = value.startLocation.y
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

class SpatialEventGesture: Gesture<Unit> {

    val coordinateSpace: CoordinateSpaceProtocol
    val action: (Any) -> Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(coordinateSpace: CoordinateSpaceProtocol = LocalCoordinateSpace.local, action: (Any) -> Unit) {
        this.coordinateSpace = coordinateSpace.sref()
        this.action = action
    }

    @androidx.annotation.Keep
    companion object {
    }
}

@Suppress("MUST_BE_INITIALIZED")
class SpatialTapGesture: Gesture<SpatialTapGesture.Value>, MutableStruct {

    @Suppress("MUST_BE_INITIALIZED")
    class Value: MutableStruct {
        var location: CGPoint
            get() = field.sref({ this.location = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }

        constructor(location: CGPoint) {
            this.location = location
        }

        override var supdate: ((Any) -> Unit)? = null
        override var smutatingcount = 0
        override fun scopy(): MutableStruct = SpatialTapGesture.Value(location)

        override fun equals(other: Any?): Boolean {
            if (other !is SpatialTapGesture.Value) return false
            return location == other.location
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    var count: Int
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var coordinateSpace: CoordinateSpaceProtocol
        get() = field.sref({ this.coordinateSpace = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(count: Int = 1, coordinateSpace: CoordinateSpaceProtocol = LocalCoordinateSpace.local) {
        this.count = count
        this.coordinateSpace = coordinateSpace
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as SpatialTapGesture
        this.count = copy.count
        this.coordinateSpace = copy.coordinateSpace
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = SpatialTapGesture(this as MutableStruct)

    @androidx.annotation.Keep
    companion object {
    }
}

class GestureMask: OptionSet<GestureMask, Int>, MutableStruct {
    override var rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override val rawvaluelong: ULong
        get() = ULong(rawValue)
    override fun makeoptionset(rawvaluelong: ULong): GestureMask = GestureMask(rawValue = Int(rawvaluelong))
    override fun assignoptionset(target: GestureMask) {
        willmutate()
        try {
            assignfrom(target)
        } finally {
            didmutate()
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as GestureMask
        this.rawValue = copy.rawValue
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = GestureMask(this as MutableStruct)

    private fun assignfrom(target: GestureMask) {
        this.rawValue = target.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val none: GestureMask = GestureMask.of()
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val gesture = GestureMask(rawValue = 1) // For bridging
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val subviews = GestureMask(rawValue = 2) // For bridging
        val all = GestureMask(rawValue = 3) // For bridging

        fun of(vararg options: GestureMask): GestureMask {
            val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
            return GestureMask(rawValue = value)
        }
    }
}

/// A gesture that has been modified with callbacks, etc.
class ModifiedGesture<V>: Gesture<V>, MutableStruct {
    internal val gesture: Gesture<V>
    internal val coordinateSpace: CoordinateSpace
    internal var onChanged: Array<(V) -> Unit> = arrayOf()
        get() = field.sref({ this.onChanged = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    internal var onEnded: Array<(V) -> Unit> = arrayOf()
        get() = field.sref({ this.onEnded = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    internal var onEndedWithLocation: ((CGPoint) -> Unit)? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    internal constructor(gesture: Gesture<V>) {
        this.gesture = gesture.sref()
        this.coordinateSpace = (gesture as? TapGesture)?.coordinateSpace?.coordinateSpace ?: (gesture as? DragGesture)?.coordinateSpace?.coordinateSpace ?: CoordinateSpace.local
    }

    internal val isTapGesture: Boolean
        get() {
            return (gesture as? TapGesture)?.count == 1
        }

    internal fun onTap(at: CGPoint) {
        val point = at
        onEndedWithLocation?.invoke(point)
        onEnded.forEach { it -> it(Unit as V) }
    }

    internal val isDoubleTapGesture: Boolean
        get() {
            return (gesture as? TapGesture)?.count == 2
        }

    internal fun onDoubleTap(at: CGPoint) {
        val point = at
        onEndedWithLocation?.invoke(point)
        onEnded.forEach { it -> it(Unit as V) }
    }

    internal val isLongPressGesture: Boolean
        get() = gesture is LongPressGesture

    internal fun onLongPressChange() {
        onChanged.forEach { it -> it(true as V) }
    }

    internal fun onLongPressEnd() {
        onEnded.forEach { it -> it(true as V) }
    }

    internal val isDragGesture: Boolean
        get() = gesture is DragGesture

    internal val minimumDistance: Double
        get() {
            return (gesture as? DragGesture)?.minimumDistance ?: 0.0
        }

    internal fun onDragChange(location: CGPoint, translation: CGSize) {
        val value = dragValue(location = location, translation = translation)
        onChanged.forEach { it -> it(value as V) }
    }

    internal fun onDragEnd(location: CGPoint, translation: CGSize) {
        val value = dragValue(location = location, translation = translation)
        onEnded.forEach { it -> it(value as V) }
    }

    private fun dragValue(location: CGPoint, translation: CGSize): DragGesture.Value = DragGesture.Value(time = Date(), location = location, startLocation = CGPoint(x = location.x - translation.width, y = location.y - translation.height), translation = translation, velocity = CGSize.zero, predictedEndLocation = location, predictedEndTranslation = translation)

    internal val isMagnifyGesture: Boolean
        get() = gesture is MagnifyGesture

    internal fun onMagnifyChange(magnification: Double, location: CGPoint, translation: CGSize) {
        val value = magnifyValue(magnification = magnification, location = location, translation = translation)
        onChanged.forEach { it -> it(value as V) }
    }

    internal fun onMagnifyEnd(magnification: Double, location: CGPoint, translation: CGSize) {
        val value = magnifyValue(magnification = magnification, location = location, translation = translation)
        onEnded.forEach { it -> it(value as V) }
    }

    private fun magnifyValue(magnification: Double, location: CGPoint, translation: CGSize): MagnifyGesture.Value = MagnifyGesture.Value(time = Date(), magnification = magnification, velocity = 0.0, startAnchor = UnitPoint(x = 0.0, y = 0.0), startLocation = CGPoint(x = location.x - translation.width, y = location.y - translation.height))

    internal val isRotateGesture: Boolean
        get() = gesture is RotateGesture

    internal fun onRotateChange(rotation: Double, location: CGPoint, translation: CGSize) {
        val value = rotateValue(rotation = rotation, location = location, translation = translation)
        onChanged.forEach { it -> it(value as V) }
    }

    internal fun onRotateEnd(rotation: Double, location: CGPoint, translation: CGSize) {
        val value = rotateValue(rotation = rotation, location = location, translation = translation)
        onEnded.forEach { it -> it(value as V) }
    }

    private fun rotateValue(rotation: Double, location: CGPoint, translation: CGSize): RotateGesture.Value = RotateGesture.Value(time = Date(), rotation = Angle.degrees(rotation), velocity = Angle.degrees(0.0), startAnchor = UnitPoint(x = 0.0, y = 0.0), startLocation = CGPoint(x = location.x - translation.width, y = location.y - translation.height))

    override val modified: ModifiedGesture<V>
        get() = this

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as ModifiedGesture<V>
        this.gesture = copy.gesture
        this.coordinateSpace = copy.coordinateSpace
        this.onChanged = copy.onChanged
        this.onEnded = copy.onEnded
        this.onEndedWithLocation = copy.onEndedWithLocation
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = ModifiedGesture<V>(this as MutableStruct)

    @androidx.annotation.Keep
    companion object {
    }
}

/// Modifier view that collects and executes gestures.
internal class GestureModifier: RenderModifier {
    internal val gesture: ModifiedGesture<Any>?
    internal var isConsumed = false

    internal constructor(gesture: Gesture<Any>, isEnabled: Boolean): super() {
        this.gesture = (if (isEnabled) gesture.modified else null).sref()
        this.action = { renderable, context ->
            var context = context.sref()
            context.modifier = addGestures(for_ = renderable, to = context.modifier)
            renderable.Render(context = context)
        }
    }

    @Composable
    private fun addGestures(for_: Renderable, to: Modifier): Modifier {
        val renderable = for_
        val modifier = to
        if (isConsumed) {
            return modifier
        }
        if (!EnvironmentValues.shared.isEnabled) {
            return modifier
        }

        // Compose wants you to collect all e.g. tap gestures into a single pointerInput modifier, so we collect all our gestures
        val gestures: kotlin.collections.MutableList<ModifiedGesture<Any>> = mutableListOf()
        if (gesture != null) {
            gestures.add(gesture)
        }
        renderable.forEachModifier l@{ it ->
            val gestureModifier_0 = it as? GestureModifier
            if (gestureModifier_0 == null) {
                return@l null
            }
            gestureModifier_0.gesture.sref()?.let { gesture ->
                gestures.add(gesture)
            }
            gestureModifier_0.isConsumed = true
            return@l null
        }

        val density = LocalDensity.current.sref()
        var ret = modifier

        // If the gesture is placed directly on a shape, we attempt to constrain hits to the shape
        (renderable.strip() as? ModifiedShape).sref()?.let { shape ->
            if (renderable.forEachModifier(perform = { it -> if (it.role != ModifierRole.accessibility && !(it is GestureModifier)) true else null }) == null) {
                shape.asComposeTouchShape(density = density)?.let { touchShape ->
                    ret = ret.clip(touchShape)
                }
            }
        }

        val layoutCoordinates = remember { -> mutableStateOf<LayoutCoordinates?>(null) }
        ret = ret.onGloballyPositioned { it -> layoutCoordinates.value = it }

        val tapGestures = rememberUpdatedState(gestures.filter { it -> it.isTapGesture })
        val doubleTapGestures = rememberUpdatedState(gestures.filter { it -> it.isDoubleTapGesture })
        val longPressGestures = rememberUpdatedState(gestures.filter { it -> it.isLongPressGesture })
        if (tapGestures.value.size > 0 || doubleTapGestures.value.size > 0 || longPressGestures.value.size > 0) {
            ret = ret.pointerInput(true) { ->
                val onDoubleTap: ((Offset) -> Unit)?
                if (doubleTapGestures.value.size > 0) {
                    onDoubleTap = { offsetPx ->
                        val x = with(density) { -> offsetPx.x.toDp() }
                        val y = with(density) { -> offsetPx.y.toDp() }
                        val point = CGPoint(x = Double(x.value), y = Double(y.value))
                        doubleTapGestures.value.forEach { it -> it.onDoubleTap(at = point) }
                    }
                } else {
                    onDoubleTap = null
                }
                val onLongPress: ((Offset) -> Unit)?
                if (longPressGestures.value.size > 0) {
                    onLongPress = { _ ->
                        longPressGestures.value.forEach { it -> it.onLongPressEnd() }
                    }
                } else {
                    onLongPress = null
                }
                detectTapGestures(onDoubleTap = onDoubleTap, onLongPress = onLongPress, onPress = { _ ->
                    longPressGestures.value.forEach { it -> it.onLongPressChange() }
                }, onTap = { localOffsetPx ->
                    for (tapGesture in tapGestures.value.sref()) {
                        var offsetPx = localOffsetPx.sref()
                        if (tapGesture.coordinateSpace.isGlobal) {
                            layoutCoordinates.value.sref()?.let { layoutCoordinates ->
                                offsetPx = layoutCoordinates.localToRoot(offsetPx)
                            }
                        }
                        val x = with(density) { -> offsetPx.x.toDp() }
                        val y = with(density) { -> offsetPx.y.toDp() }
                        val point = CGPoint(x = Double(x.value), y = Double(y.value))
                        tapGesture.onTap(at = point)
                    }
                })
            }
        }

        val dragGestures = rememberUpdatedState(gestures.filter { it -> it.isDragGesture })
        if (dragGestures.value.size > 0) {
            val dragOffsetX = remember { -> mutableStateOf(0.0f) }
            val dragOffsetY = remember { -> mutableStateOf(0.0f) }
            val dragPositionPx = remember { -> mutableStateOf(Offset(x = 0.0f, y = 0.0f)) }
            val noMinimumDistance = dragGestures.value.any { it -> it.minimumDistance <= 0.0 }
            val scrollAxes = EnvironmentValues.shared._scrollAxes.sref()
            ret = ret.pointerInput(scrollAxes) { ->
                val onDrag: (PointerInputChange, Offset) -> Unit = { change, offsetPx ->
                    val offsetX = with(density) { -> offsetPx.x.toDp() }
                    val offsetY = with(density) { -> offsetPx.y.toDp() }
                    dragOffsetX.value += offsetX.value
                    dragOffsetY.value += offsetY.value
                    val translation = CGSize(width = Double(dragOffsetX.value), height = Double(dragOffsetY.value))

                    dragPositionPx.value = change.position
                    for (dragGesture in dragGestures.value.sref()) {
                        var positionPx = change.position.sref()
                        if (dragGesture.coordinateSpace.isGlobal) {
                            layoutCoordinates.value.sref()?.let { layoutCoordinates ->
                                positionPx = layoutCoordinates.localToRoot(positionPx)
                            }
                        }
                        val positionX = (with(density) { -> positionPx.x.toDp() }).value.sref()
                        val positionY = (with(density) { -> positionPx.y.toDp() }).value.sref()
                        val location = CGPoint(x = Double(positionX), y = Double(positionY))
                        dragGesture.onDragChange(location = location, translation = translation)
                    }
                }
                val onDragEnd: () -> Unit = { ->
                    val translation = CGSize(width = Double(dragOffsetX.value), height = Double(dragOffsetY.value))
                    dragOffsetX.value = 0.0f
                    dragOffsetY.value = 0.0f
                    for (dragGesture in dragGestures.value.sref()) {
                        var positionPx = dragPositionPx.value.sref()
                        if (dragGesture.coordinateSpace.isGlobal) {
                            layoutCoordinates.value.sref()?.let { layoutCoordinates ->
                                positionPx = layoutCoordinates.localToRoot(positionPx)
                            }
                        }
                        val positionX = (with(density) { -> positionPx.x.toDp() }).value.sref()
                        val positionY = (with(density) { -> positionPx.y.toDp() }).value.sref()
                        val location = CGPoint(x = Double(positionX), y = Double(positionY))
                        dragGesture.onDragEnd(location = location, translation = translation)
                    }
                }
                detectDragGesturesWithScrollAxes(onDrag = onDrag, onDragEnd = onDragEnd, onDragCancel = onDragEnd, shouldAwaitTouchSlop = { -> !noMinimumDistance }, scrollAxes = scrollAxes)
            }
        }

        val magnifyGestures = rememberUpdatedState(gestures.filter { it -> it.isMagnifyGesture })
        val rotateGestures = rememberUpdatedState(gestures.filter { it -> it.isRotateGesture })
        if (magnifyGestures.value.size > 0 || rotateGestures.value.size > 0) {
            val magnification = remember { -> mutableStateOf(1.0f) }
            val rotation = remember { -> mutableStateOf(0.0f) }
            val panOffsetX = remember { -> mutableStateOf(0.0f) }
            val panOffsetY = remember { -> mutableStateOf(0.0f) }
            val locationX = remember { -> mutableStateOf(0.0f) }
            val locationY = remember { -> mutableStateOf(0.0f) }
            val hasMagnified = remember { -> mutableStateOf(false) }
            val hasRotated = remember { -> mutableStateOf(false) }
            ret = ret.pointerInput(true) { ->
                detectTransformGesturesWithGestureEnd(panZoomLock = false, onGesture = l@{ centroidPx, panPx, zoom, rotate ->
                    if (zoom != 1.0f) {
                        hasMagnified.value = true
                    }
                    if (rotate != 0.0f) {
                        hasRotated.value = true
                    }
                    if (!hasMagnified.value && !hasRotated.value) {
                        return@l
                    }

                    panOffsetX.value = (with(density) { -> panPx.x.toDp() }).value
                    panOffsetY.value = (with(density) { -> panPx.y.toDp() }).value
                    val translation = CGSize(width = Double(panOffsetX.value), height = Double(panOffsetY.value))

                    locationX.value = (with(density) { -> centroidPx.x.toDp() }).value
                    locationY.value = (with(density) { -> centroidPx.y.toDp() }).value
                    val location = CGPoint(x = Double(locationX.value), y = Double(locationY.value))

                    if (hasMagnified.value) {
                        magnification.value *= zoom
                        for (magnifyGesture in magnifyGestures.value.sref()) {
                            magnifyGesture.onMagnifyChange(magnification = Double(magnification.value), location = location, translation = translation)
                        }
                    }
                    if (hasRotated.value) {
                        rotation.value += rotate
                        for (rotateGesture in rotateGestures.value.sref()) {
                            rotateGesture.onRotateChange(rotation = Double(rotation.value), location = location, translation = translation)
                        }
                    }
                }, onGestureEnd = l@{ ->
                    if (!hasMagnified.value && !hasRotated.value) {
                        return@l
                    }
                    val translation = CGSize(width = Double(panOffsetX.value), height = Double(panOffsetY.value))
                    val location = CGPoint(x = Double(locationX.value), y = Double(locationY.value))
                    if (hasMagnified.value) {
                        for (magnifyGesture in magnifyGestures.value.sref()) {
                            magnifyGesture.onMagnifyEnd(magnification = Double(magnification.value), location = location, translation = translation)
                        }
                    }
                    if (hasRotated.value) {
                        for (rotateGesture in rotateGestures.value.sref()) {
                            rotateGesture.onRotateEnd(rotation = Double(rotation.value), location = location, translation = translation)
                        }
                    }

                    magnification.value = 1.0f
                    rotation.value = 0.0f
                    panOffsetX.value = 0.0f
                    panOffsetY.value = 0.0f
                    locationX.value = 0.0f
                    locationY.value = 0.0f
                    hasMagnified.value = false
                    hasRotated.value = false
                })
            }
        }
        return ret
    }
}

// This is an adaptation of the internal Compose function here: https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/gestures/DragGestureDetector.kt
suspend fun PointerInputScope.detectDragGesturesWithScrollAxes(onDrag: (PointerInputChange, Offset) -> Unit, onDragEnd: () -> Unit, onDragCancel: () -> Unit, shouldAwaitTouchSlop: () -> Boolean, scrollAxes: Axis.Set) {
    var overSlop: Offset
    awaitEachGesture { ->
        val initialDown = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val awaitTouchSlop = shouldAwaitTouchSlop()
        if ((!awaitTouchSlop)) {
            initialDown.consume()
        }
        val down = awaitFirstDown(requireUnconsumed = false)
        var drag: PointerInputChange? = null
        overSlop = Offset.Zero.sref()
        if ((awaitTouchSlop)) {
            do {
                drag = awaitPointerSlopOrCancellation(down.id, down.type) { change, over ->
                    if (scrollAxes == Axis.Set.vertical) {
                        if (abs(over.x) > abs(over.y)) {
                            change.consume()
                        }
                    } else if (scrollAxes == Axis.Set.horizontal) {
                        if (abs(over.y) > abs(over.x)) {
                            change.consume()
                        }
                    } else {
                        change.consume()
                    }
                    overSlop = over.sref()
                }
            } while (drag != null && drag?.isConsumed != true)
        } else {
            drag = initialDown.sref()
        }

        if (drag != null) {
            onDrag(drag, overSlop)
            val didCompleteDrag = drag(pointerId = drag.id, onDrag = { it ->
                onDrag(it, it.positionChange())
                it.consume()
            })
            if (didCompleteDrag) {
                onDragEnd()
            } else {
                onDragCancel()
            }
        }
    }
}

/*
import struct CoreGraphics.CGFloat
import struct CoreGraphics.CGPoint
import struct CoreGraphics.CGSize
import struct Foundation.Date

/// A type-erased gesture.
@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
@frozen public struct AnyGesture<V> : Gesture {

/// Creates an instance from another gesture.
///
/// - Parameter gesture: A gesture that you use to create a new gesture.
public init<T>(_ gesture: T) where V == T.V, T : Gesture { fatalError() }
}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension Gesture {

/// Updates the provided gesture state property as the gesture's value
/// changes.
///
/// Use this callback to update transient UI state as described in
/// <doc:Adding-Interactivity-with-Gestures>.
///
/// - Parameters:
///   - state: A binding to a view's ``GestureState`` property.
///   - body: The callback that SkipUI invokes as the gesture's value
///     changes. Its `currentState` parameter is the updated state of the
///     gesture. The `gestureState` parameter is the previous state of the
///     gesture, and the `transaction` is the context of the gesture.
///
/// - Returns: A version of the gesture that updates the provided `state` as
///   the originating gesture's value changes and that resets the `state`
///   to its initial value when the user or the system ends or cancels the
///   gesture.
public func updating<State>(_ state: GestureState<State>, body: @escaping (Self.V, inout State, inout Transaction) -> Void) -> GestureStateGesture<Self, State> { fatalError() }
}

/// A property wrapper type that updates a property while the user performs a
/// gesture and resets the property back to its initial state when the gesture
/// ends.
///
/// Declare a property as `@GestureState`, pass as a binding to it as a
/// parameter to a gesture's ``Gesture/updating(_:body:)`` callback, and receive
/// updates to it. A property that's declared as `@GestureState` implicitly
/// resets when the gesture becomes inactive, making it suitable for tracking
/// transient state.
///
/// Add a long-press gesture to a ``Circle``, and update the interface during
/// the gesture by declaring a property as `@GestureState`:
///
///     struct SimpleLongPressGestureView: View {
///         @GestureState private var isDetectingLongPress = false
///
///         var longPress: some Gesture {
///             LongPressGesture(minimumDuration: 3)
///                 .updating($isDetectingLongPress) { currentState, gestureState, transaction in
///                     gestureState = currentState
///                 }
///         }
///
///         var body: some View {
///             Circle()
///                 .fill(self.isDetectingLongPress ? Color.red : Color.green)
///                 .frame(width: 100, height: 100, alignment: .center)
///                 .gesture(longPress)
///         }
///     }
@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
@propertyWrapper @frozen public struct GestureState<Value> : DynamicProperty {

/// Creates a view state that's derived from a gesture.
///
/// - Parameter wrappedValue: A wrapped value for the gesture state
///   property.
public init(wrappedValue: Value) { fatalError() }

/// Creates a view state that's derived from a gesture with an initial
/// value.
///
/// - Parameter initialValue: An initial value for the gesture state
///   property.
public init(initialValue: Value) { fatalError() }

/// Creates a view state that's derived from a gesture with a wrapped state
/// value and a transaction to reset it.
///
/// - Parameters:
///   - wrappedValue: A wrapped value for the gesture state property.
///   - resetTransaction: A transaction that provides metadata for view
///     updates.
public init(wrappedValue: Value, resetTransaction: Transaction) { fatalError() }

/// Creates a view state that's derived from a gesture with an initial state
/// value and a transaction to reset it.
///
/// - Parameters:
///   - initialValue: An initial state value.
///   - resetTransaction: A transaction that provides metadata for view
///     updates.
public init(initialValue: Value, resetTransaction: Transaction) { fatalError() }

/// Creates a view state that's derived from a gesture with a wrapped state
/// value and a closure that provides a transaction to reset it.
///
/// - Parameters:
///   - wrappedValue: A wrapped value for the gesture state property.
///   - reset: A closure that provides a ``Transaction``.
public init(wrappedValue: Value, reset: @escaping (Value, inout Transaction) -> Void) { fatalError() }

/// Creates a view state that's derived from a gesture with an initial state
/// value and a closure that provides a transaction to reset it.
///
/// - Parameters:
///   - initialValue: An initial state value.
///   - reset: A closure that provides a ``Transaction``.
public init(initialValue: Value, reset: @escaping (Value, inout Transaction) -> Void) { fatalError() }

/// The wrapped value referenced by the gesture state property.
public var wrappedValue: Value { get { fatalError() } }

/// A binding to the gesture state property.
public var projectedValue: GestureState<Value> { get { fatalError() } }
}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension GestureState where Value : ExpressibleByNilLiteral {

/// Creates a view state that's derived from a gesture with a transaction to
/// reset it.
///
/// - Parameter resetTransaction: A transaction that provides metadata for
///   view updates.
public init(resetTransaction: Transaction = Transaction()) { fatalError() }

/// Creates a view state that's derived from a gesture with a closure that
/// provides a transaction to reset it.
///
/// - Parameter reset: A closure that provides a ``Transaction``.
public init(reset: @escaping (Value, inout Transaction) -> Void) { fatalError() }
}

/// A gesture that updates the state provided by a gesture's updating callback.
///
/// A gesture's ``Gesture/updating(_:body:)`` callback returns a
/// `GestureStateGesture` instance for updating a transient state property
/// that's annotated with the ``GestureState`` property wrapper.
@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
@frozen public struct GestureStateGesture<Base, State> : Gesture where Base : Gesture {

/// The type representing the gesture's value.
public typealias V = Base.V

/// The originating gesture.
public var base: Base { get { fatalError() } }

/// A value that changes as the user performs the gesture.
public var state: GestureState<State>

/// The updating gesture containing the originating gesture's value, the
/// updated state of the gesture, and a transaction.
//    public var body: (GestureStateGesture<Base, State>.Value, inout State, inout Transaction) -> Void { get { fatalError() } }

/// Creates a new gesture that's the result of an ongoing gesture.
///
/// - Parameters:
///   - base: The originating gesture.
///   - state: The wrapped value of a ``GestureState`` property.
///   - body: The callback that SkipUI invokes as the gesture's value
///     changes.
@inlinable public init(base: Base, state: GestureState<State>, body: @escaping (GestureStateGesture<Base, State>.V, inout State, inout Transaction) -> Void) { fatalError() }
}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension Never : Gesture {

/// The type representing the gesture's value.
public typealias V = Never
}

/// Extends `T?` to conform to `Gesture` type if `T` also conforms to
/// `Gesture`. A nil value is mapped to an empty (i.e. failing)
/// gesture.
@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension Optional : Gesture where Wrapped : Gesture {

/// The type representing the gesture's value.
public typealias V = Wrapped.V
}
*/
