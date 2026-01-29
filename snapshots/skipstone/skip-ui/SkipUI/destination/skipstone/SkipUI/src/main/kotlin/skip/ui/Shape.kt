package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

interface Shape: View, Renderable {
    fun path(in_: CGRect): Path {
        val rect = in_
        return Path()
    }

    val layoutDirectionBehavior: LayoutDirectionBehavior
        get() = LayoutDirectionBehavior.mirrors

    fun sizeThatFits(proposal: ProposedViewSize): CGSize = proposal.replacingUnspecifiedDimensions()

    val modified: ModifiedShape
        get() = ModifiedShape(shape = this)

    val canOutsetForStroke: Boolean
        get() = false

    fun path(inX: Double, y: Double, width: Double, height: Double): Path = path(in_ = CGRect(x = inX, y = y, width = width, height = height))

    @Composable
    override fun Render(context: ComposeContext) {
        fill().Compose(context = context)
    }

    fun asComposePath(size: Size, density: Density): androidx.compose.ui.graphics.Path {
        val px = with(density) { -> 1.dp.toPx() }
        val path = path(in_ = CGRect(x = 0.0, y = 0.0, width = max(0.0, Double(size.width / px)), height = max(0.0, Double(size.height / px))))
        return path.asComposePath(density = density)
    }

    fun asComposeShape(density: Density): androidx.compose.ui.graphics.Shape {
        return GenericShape { size, _ -> this.addPath(asComposePath(size = size, density = density)) }
    }
    fun fill(content: ShapeStyle, style: FillStyle = FillStyle()): Shape {
        var modifiedShape = this.modified.sref()
        modifiedShape.fill = content
        return modifiedShape.sref()
    }

    fun fill(style: FillStyle = FillStyle()): Shape = fill(ForegroundStyle(), style = style)

    fun fill(content: ShapeStyle, eoFill: Boolean, antialiased: Boolean): Shape = fill(content, style = FillStyle(eoFill = eoFill, antialiased = antialiased))

    fun inset(by: Double): Shape {
        val amount = by
        var modifiedShape = this.modified.sref()
        modifiedShape.modifications.append(ShapeModification.inset(amount))
        return modifiedShape.sref()
    }

    override fun offset(offset: CGSize): Shape = this.offset(CGPoint(x = offset.width, y = offset.height))

    fun offset(offset: CGPoint): Shape {
        var modifiedShape = this.modified.sref()
        modifiedShape.modifications.append(ShapeModification.offset(offset))
        return modifiedShape.sref()
    }

    override fun offset(x: Double, y: Double): Shape = this.offset(CGPoint(x = x, y = y))

    fun rotation(angle: Angle, anchor: UnitPoint = UnitPoint.center): Shape {
        var modifiedShape = this.modified.sref()
        modifiedShape.modifications.append(ShapeModification.rotation(angle, anchor))
        return modifiedShape.sref()
    }

    fun rotation(bridgedAngle: Double, anchorX: Double, anchorY: Double): Shape = rotation(Angle(radians = bridgedAngle), anchor = UnitPoint(x = anchorX, y = anchorY))

    fun scale(x: Double = 1.0, y: Double = 1.0, anchor: UnitPoint = UnitPoint.center): Shape {
        var modifiedShape = this.modified.sref()
        modifiedShape.modifications.append(ShapeModification.scale(CGPoint(x = x, y = y), anchor))
        return modifiedShape.sref()
    }

    fun scale(scale: Double, anchor: UnitPoint = UnitPoint.center): Shape = this.scale(x = scale, y = scale, anchor = anchor)

    fun scale(width: Double, height: Double, anchorX: Double, anchorY: Double): Shape = scale(x = width, y = height, anchor = UnitPoint(x = anchorX, y = anchorY))

    fun stroke(content: ShapeStyle, style: StrokeStyle, antialiased: Boolean = true): Shape {
        var modifiedShape = this.modified.sref()
        modifiedShape.strokes.append(ShapeStroke(content, style, false))
        return modifiedShape.sref()
    }

    fun stroke(content: ShapeStyle, lineWidth: Double, bridgedLineCap: Int, bridgedLineJoin: Int, miterLmit: Double, dash: Array<Double>, dashPhase: Double, antialiased: Boolean): Shape = stroke(content, style = StrokeStyle(lineWidth = lineWidth, lineCap = CGLineCap(rawValue = bridgedLineCap) ?: CGLineCap.butt, lineJoin = CGLineJoin(rawValue = bridgedLineJoin) ?: CGLineJoin.miter, miterLimit = miterLmit, dash = dash, dashPhase = dashPhase), antialiased = antialiased)

    fun stroke(content: ShapeStyle, lineWidth: Double = 1.0, antialiased: Boolean = true): Shape = stroke(content, style = StrokeStyle(lineWidth = lineWidth), antialiased = antialiased)

    fun stroke(style: StrokeStyle): Shape = stroke(ForegroundStyle(), style = style)

    fun stroke(lineWidth: Double = 1.0): Shape = stroke(ForegroundStyle(), style = StrokeStyle(lineWidth = lineWidth))

    fun strokeBorder(content: ShapeStyle = ForegroundStyle.foreground, style: StrokeStyle, antialiased: Boolean = true): View {
        var modifiedShape = this.modified.sref()
        modifiedShape.strokes.append(ShapeStroke(content, style, true))
        return modifiedShape.sref()
    }

    fun strokeBorder(style: StrokeStyle, antialiased: Boolean = true): View = strokeBorder(ForegroundStyle(), style = style, antialiased = antialiased)

    fun strokeBorder(content: ShapeStyle = ForegroundStyle.foreground, lineWidth: Double = 1.0, antialiased: Boolean = true): View = strokeBorder(content, style = StrokeStyle(lineWidth = lineWidth), antialiased = antialiased)

    fun strokeBorder(lineWidth: Double = 1.0, antialiased: Boolean = true): View = strokeBorder(ForegroundStyle(), style = StrokeStyle(lineWidth = lineWidth), antialiased = antialiased)

    fun strokeBorder(content: ShapeStyle, lineWidth: Double, bridgedLineCap: Int, bridgedLineJoin: Int, miterLmit: Double, dash: Array<Double>, dashPhase: Double, antialiased: Boolean): View = strokeBorder(content, style = StrokeStyle(lineWidth = lineWidth, lineCap = CGLineCap(rawValue = bridgedLineCap) ?: CGLineCap.butt, lineJoin = CGLineJoin(rawValue = bridgedLineJoin) ?: CGLineJoin.miter, miterLimit = miterLmit, dash = dash, dashPhase = dashPhase), antialiased = antialiased)
}
interface ShapeCompanion {
}

/// Modifications to a shape.
internal sealed class ShapeModification {
    class OffsetCase(val associated0: CGPoint): ShapeModification() {
    }
    class InsetCase(val associated0: Double): ShapeModification() {
    }
    class ScaleCase(val associated0: CGPoint, val associated1: UnitPoint): ShapeModification() {
    }
    class RotationCase(val associated0: Angle, val associated1: UnitPoint): ShapeModification() {
    }

    @androidx.annotation.Keep
    companion object {
        fun offset(associated0: CGPoint): ShapeModification = OffsetCase(associated0)
        fun inset(associated0: Double): ShapeModification = InsetCase(associated0)
        fun scale(associated0: CGPoint, associated1: UnitPoint): ShapeModification = ScaleCase(associated0, associated1)
        fun rotation(associated0: Angle, associated1: UnitPoint): ShapeModification = RotationCase(associated0, associated1)
    }
}

/// Strokes on a shape.
internal class ShapeStroke {
    internal val stroke: ShapeStyle
    internal val style: StrokeStyle?
    internal val isInset: Boolean

    constructor(stroke: ShapeStyle, style: StrokeStyle? = null, isInset: Boolean) {
        this.stroke = stroke.sref()
        this.style = style.sref()
        this.isInset = isInset
    }
}

/// A shape that has been modified.
class ModifiedShape: Shape, MutableStruct {
    internal val shape: Shape
    internal var modifications: Array<ShapeModification> = arrayOf()
        get() = field.sref({ this.modifications = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    internal var fill: ShapeStyle? = null
        get() = field.sref({ this.fill = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    internal var strokes: Array<ShapeStroke> = arrayOf()
        get() = field.sref({ this.strokes = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    internal constructor(shape: Shape) {
        this.shape = shape.sref()
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val modifier = context.modifier.fillSize()
        val density = LocalDensity.current.sref()

        val fillBrush: Brush?
        val matchtarget_0 = fill
        if (matchtarget_0 != null) {
            val fill = matchtarget_0
            fillBrush = (fill.asBrush(opacity = 1.0, animationContext = context) ?: Color.primary.asBrush(opacity = 1.0, animationContext = null)).sref()
        } else {
            fillBrush = null
        }
        var strokeBrushes: Array<Tuple3<Brush, DrawStyle, Float>> = arrayOf()
        for (stroke in strokes.sref()) {
            val brush = (stroke.stroke.asBrush(opacity = 1.0, animationContext = context) ?: Color.primary.asBrush(opacity = 1.0, animationContext = null)!!).sref()
            val drawStyle = (stroke.style?.asDrawStyle() ?: Stroke()).sref()
            var inset = 0.0f
            if (stroke.isInset) {
                stroke.style.sref()?.let { style ->
                    inset = with(density) { -> (style.lineWidth / 2.0).dp.toPx() }
                }
            }
            strokeBrushes.append(Tuple3(brush.sref(), drawStyle.sref(), inset))
        }

        Canvas(modifier = modifier) { ->
            val scope = this.sref()
            val path = asComposePath(size = scope.size, density = density)
            if (fillBrush != null) {
                scope.drawPath(path, fillBrush)
            }
            for (strokeBrush in strokeBrushes.sref()) {
                val strokeInset = strokeBrush.element2
                if (strokeInset == 0.0f) {
                    scope.drawPath(path, brush = strokeBrush.element0, style = strokeBrush.element1)
                } else {
                    // Insetting to a negative size causes a crash
                    scope.inset(min(scope.size.width / 2, min(scope.size.height / 2, strokeInset))) { ->
                        val strokePath = asComposePath(size = scope.size, density = density)
                        scope.drawPath(strokePath, brush = strokeBrush.element0, style = strokeBrush.element1)
                    }
                }
            }
        }
    }

    override val modified: ModifiedShape
        get() = this

    override fun asComposePath(size: Size, density: Density): androidx.compose.ui.graphics.Path = asComposePath(size = size, density = density, strokeOutset = 0.0)

    /// If this shape can be expressed as a touchable area, return it.
    ///
    /// This only works for shapes that aren't stroked or that can be outset for their stroke.
    /// - Seealso: `canOutsetForStroke`
    internal fun asComposeTouchShape(density: Density): androidx.compose.ui.graphics.Shape? {
        var strokeOutset = 0.0
        for (stroke in strokes.sref()) {
            if (!stroke.isInset) {
                stroke.style.sref()?.let { style ->
                    strokeOutset = max(strokeOutset, style.lineWidth / 2.0)
                }
            }
        }
        if (strokeOutset <= 0.0) {
            return asComposeShape(density = density)
        }
        if (!shape.canOutsetForStroke) {
            return null
        }
        return GenericShape { size, _ -> this.addPath(asComposePath(size = size, density = density, strokeOutset = strokeOutset)) }
    }

    private fun asComposePath(size: Size, density: Density, strokeOutset: Double): androidx.compose.ui.graphics.Path {
        val path = shape.asComposePath(size = size, density = density)
        var scaledSize = size.sref()
        var totalOffset = Offset(0.0f, 0.0f)
        var modifications = this.modifications.sref()
        if (strokeOutset > 0.0) {
            modifications.append(ShapeModification.inset(-strokeOutset))
        }
        // TODO: Support scale and rotation anchors
        for (mod in modifications.sref()) {
            when (mod) {
                is ShapeModification.OffsetCase -> {
                    val offset = mod.associated0
                    val offsetX = with(density) { -> offset.x.dp.toPx() }
                    val offsetY = with(density) { -> offset.y.dp.toPx() }
                    path.translate(Offset(offsetX, offsetY))
                    totalOffset = Offset(totalOffset.x + offsetX, totalOffset.y + offsetY)
                }
                is ShapeModification.InsetCase -> {
                    val inset = mod.associated0
                    val px = with(density) { -> inset.dp.toPx() }
                    val scaleX = 1.0f - (px * 2 / scaledSize.width)
                    val scaleY = 1.0f - (px * 2 / scaledSize.height)
                    val matrix = Matrix()
                    matrix.scale(scaleX, scaleY, 1.0f)
                    path.transform(matrix)
                    // Android scales from the origin, so the transform will move our translation too. Put it back
                    val scaledOffsetX = totalOffset.x * Float(scaleX)
                    val scaledOffsetY = totalOffset.y * Float(scaleY)
                    path.translate(Offset(px - (scaledOffsetX - totalOffset.x), px - (scaledOffsetY - totalOffset.y)))
                    scaledSize = Size(scaledSize.width - px * 2, scaledSize.height - px * 2)
                    totalOffset = Offset(totalOffset.x + px, totalOffset.y + px)
                }
                is ShapeModification.ScaleCase -> {
                    val scale = mod.associated0
                    val matrix = Matrix()
                    matrix.scale(Float(scale.x), Float(scale.y), 1.0f)
                    path.transform(matrix)
                    // Android scales from the origin, so the transform will move our translation too. Put it back
                    val scaledWidth = scaledSize.width * Float(scale.x)
                    val scaledHeight = scaledSize.height * Float(scale.y)
                    val scaledOffsetX = totalOffset.x * Float(scale.x)
                    val scaledOffsetY = totalOffset.y * Float(scale.y)
                    val additionalOffsetX = (scaledSize.width - scaledWidth) / 2
                    val additionalOffsetY = (scaledSize.height - scaledHeight) / 2
                    path.translate(Offset(additionalOffsetX - (scaledOffsetX - totalOffset.x), additionalOffsetY - (scaledOffsetY - totalOffset.y)))
                    scaledSize = Size(scaledWidth, scaledHeight)
                    totalOffset = Offset(totalOffset.x + additionalOffsetX, totalOffset.y + additionalOffsetY)
                }
                is ShapeModification.RotationCase -> {
                    val angle = mod.associated0
                    val matrix = Matrix()
                    matrix.rotateZ(Float(angle.degrees))
                    path.transform(matrix)
                    // Android rotates around the origin rather than the center. Calculate the offset that this rotation
                    // causes to the center point and apply its inverse to get a rotation around the center. Note that we
                    // negate the y axis because mathmatical coordinate systems have the origin in the bottom left, not top
                    val radians = angle.radians
                    val centerX = scaledSize.width / 2 + totalOffset.x
                    val centerY = -scaledSize.height / 2 - totalOffset.y
                    val rotatedCenterX = centerX * cos(-radians) - centerY * sin(-radians)
                    val rotatedCenterY = centerX * sin(-radians) + centerY * cos(-radians)
                    val additionalOffsetX = Float(centerX - rotatedCenterX)
                    val additionalOffsetY = Float(-(centerY - rotatedCenterY))
                    path.translate(Offset(additionalOffsetX, additionalOffsetY))
                }
            }
        }
        return path.sref()
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as ModifiedShape
        this.shape = copy.shape
        this.modifications = copy.modifications
        this.fill = copy.fill
        this.strokes = copy.strokes
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = ModifiedShape(this as MutableStruct)

    @androidx.annotation.Keep
    companion object: ShapeCompanion {
    }
}

@androidx.annotation.Keep
class Circle: Shape, skip.lib.SwiftProjecting {
    constructor() {
    }

    override fun path(in_: CGRect): Path {
        val rect = in_
        val dim = min(rect.width, rect.height)
        val x = rect.minX + (rect.width - dim) / 2.0
        val y = rect.minY + (rect.height - dim) / 2.0
        return Path(ellipseIn = CGRect(x = x, y = y, width = dim, height = dim))
    }

    override val canOutsetForStroke: Boolean
        get() = true

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: ShapeCompanion {

        val circle: Circle
            get() = Circle()
    }
}

@androidx.annotation.Keep
class Rectangle: Shape, skip.lib.SwiftProjecting {
    constructor() {
    }

    override fun path(in_: CGRect): Path {
        val rect = in_
        return Path(rect)
    }

    override val canOutsetForStroke: Boolean
        get() = true

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: ShapeCompanion {

        val rect: Rectangle
            get() = Rectangle()
    }
}

@androidx.annotation.Keep
class RoundedRectangle: Shape, MutableStruct, skip.lib.SwiftProjecting {
    val cornerSize: CGSize
    val style: RoundedCornerStyle
    internal var fillStyle: ShapeStyle? = null
        get() = field.sref({ this.fillStyle = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(cornerSize: CGSize, style: RoundedCornerStyle = RoundedCornerStyle.continuous) {
        this.cornerSize = cornerSize.sref()
        this.style = style
    }

    constructor(cornerRadius: Double, style: RoundedCornerStyle = RoundedCornerStyle.continuous) {
        this.cornerSize = CGSize(width = cornerRadius, height = cornerRadius)
        this.style = style
    }

    constructor(cornerWidth: Double, cornerHeight: Double, bridgedStyle: Int) {
        this.cornerSize = CGSize(width = cornerWidth, height = cornerHeight)
        this.style = RoundedCornerStyle(rawValue = bridgedStyle) ?: RoundedCornerStyle.continuous
    }

    override fun path(in_: CGRect): Path {
        val rect = in_
        return Path(roundedRect = rect, cornerSize = cornerSize, style = style)
    }

    override val canOutsetForStroke: Boolean
        get() = true

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as RoundedRectangle
        this.cornerSize = copy.cornerSize
        this.style = copy.style
        this.fillStyle = copy.fillStyle
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = RoundedRectangle(this as MutableStruct)

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: ShapeCompanion {

        fun rect(cornerSize: CGSize, style: RoundedCornerStyle = RoundedCornerStyle.continuous): RoundedRectangle = RoundedRectangle(cornerSize = cornerSize, style = style)

        fun rect(cornerRadius: Double, style: RoundedCornerStyle = RoundedCornerStyle.continuous): RoundedRectangle = RoundedRectangle(cornerRadius = cornerRadius, style = style)
    }
}

@androidx.annotation.Keep
class UnevenRoundedRectangle: Shape, skip.lib.SwiftProjecting {
    val cornerRadii: RectangleCornerRadii
    val style: RoundedCornerStyle

    constructor(cornerRadii: RectangleCornerRadii, style: RoundedCornerStyle = RoundedCornerStyle.continuous) {
        this.cornerRadii = cornerRadii
        this.style = style
    }

    constructor(topLeadingRadius: Double = 0.0, bottomLeadingRadius: Double = 0.0, bottomTrailingRadius: Double = 0.0, topTrailingRadius: Double = 0.0, style: RoundedCornerStyle = RoundedCornerStyle.continuous) {
        this.cornerRadii = RectangleCornerRadii(topLeading = topLeadingRadius, bottomLeading = bottomLeadingRadius, bottomTrailing = bottomTrailingRadius, topTrailing = topTrailingRadius)
        this.style = style
    }

    constructor(topLeadingRadius: Double, bottomLeadingRadius: Double, bottomTrailingRadius: Double, topTrailingRadius: Double, bridgedStyle: Int) {
        this.cornerRadii = RectangleCornerRadii(topLeading = topLeadingRadius, bottomLeading = bottomLeadingRadius, bottomTrailing = bottomTrailingRadius, topTrailing = topTrailingRadius)
        this.style = RoundedCornerStyle(rawValue = bridgedStyle) ?: RoundedCornerStyle.continuous
    }

    override fun path(in_: CGRect): Path {
        val rect = in_
        return Path(roundedRect = rect, cornerRadii = cornerRadii, style = style)
    }

    override val canOutsetForStroke: Boolean
        get() = true

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: ShapeCompanion {

        fun rect(cornerRadii: RectangleCornerRadii, style: RoundedCornerStyle = RoundedCornerStyle.continuous): UnevenRoundedRectangle = UnevenRoundedRectangle(cornerRadii = cornerRadii, style = style)

        fun rect(topLeadingRadius: Double = 0.0, bottomLeadingRadius: Double = 0.0, bottomTrailingRadius: Double = 0.0, topTrailingRadius: Double = 0.0, style: RoundedCornerStyle = RoundedCornerStyle.continuous): UnevenRoundedRectangle = UnevenRoundedRectangle(topLeadingRadius = topLeadingRadius, bottomLeadingRadius = bottomLeadingRadius, bottomTrailingRadius = bottomTrailingRadius, topTrailingRadius = topTrailingRadius, style = style)
    }
}

@androidx.annotation.Keep
class Capsule: Shape, skip.lib.SwiftProjecting {
    val style: RoundedCornerStyle

    constructor(style: RoundedCornerStyle = RoundedCornerStyle.continuous) {
        this.style = style
    }

    constructor(bridgedStyle: Int) {
        this.style = RoundedCornerStyle(rawValue = bridgedStyle) ?: RoundedCornerStyle.continuous
    }

    override fun path(in_: CGRect): Path {
        val rect = in_
        var path = Path()
        if (rect.width >= rect.height) {
            path.move(to = CGPoint(x = rect.minX + rect.height / 2.0, y = rect.minY))
            path.addLine(to = CGPoint(x = rect.maxX - rect.height / 2.0, y = rect.minY))
            path.addRelativeArc(center = CGPoint(x = rect.maxX - rect.height / 2.0, y = rect.midY), radius = rect.height / 2.0, startAngle = Angle(degrees = -90.0), delta = Angle(degrees = 180.0))
            path.addLine(to = CGPoint(x = rect.minX + rect.height / 2.0, y = rect.maxY))
            path.addRelativeArc(center = CGPoint(x = rect.minX + rect.height / 2.0, y = rect.midY), radius = rect.height / 2.0, startAngle = Angle(degrees = 90.0), delta = Angle(degrees = 180.0))
        } else {
            path.move(to = CGPoint(x = rect.minX, y = rect.minY + rect.width / 2.0))
            path.addRelativeArc(center = CGPoint(x = rect.midX, y = rect.minY + rect.width / 2.0), radius = rect.width / 2.0, startAngle = Angle(degrees = -180.0), delta = Angle(degrees = 180.0))
            path.addLine(to = CGPoint(x = rect.maxX, y = rect.maxY - rect.width / 2.0))
            path.addRelativeArc(center = CGPoint(x = rect.midX, y = rect.maxY - rect.width / 2.0), radius = rect.width / 2.0, startAngle = Angle(degrees = 0.0), delta = Angle(degrees = 180.0))
        }
        return path.sref()
    }

    override val canOutsetForStroke: Boolean
        get() = true

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: ShapeCompanion {

        val capsule: Capsule
            get() = Capsule()

        fun capsule(style: RoundedCornerStyle): Capsule = Capsule(style = style)
    }
}

@androidx.annotation.Keep
class Ellipse: Shape, skip.lib.SwiftProjecting {
    constructor() {
    }

    override fun path(in_: CGRect): Path {
        val rect = in_
        return Path(ellipseIn = rect)
    }

    override val canOutsetForStroke: Boolean
        get() = true

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: ShapeCompanion {

        val ellipse: Ellipse
            get() = Ellipse()
    }
}

@androidx.annotation.Keep
class BridgedCustomShape: Shape, skip.lib.SwiftProjecting {
    val pathBlock: (Double, Double, Double, Double) -> Path

    constructor(path: (Double, Double, Double, Double) -> Path) {
        this.pathBlock = path
    }

    override fun path(in_: CGRect): Path {
        val rect = in_
        return pathBlock(rect.origin.x, rect.origin.y, rect.width, rect.height)
    }


    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: ShapeCompanion {
    }
}

class AnyShape: Shape {
    private val shape: Shape

    constructor(shape: Shape) {
        this.shape = shape.sref()
    }

    override fun path(in_: CGRect): Path {
        val rect = in_
        return shape.path(in_ = rect)
    }

    @Composable
    override fun Render(context: ComposeContext) {
        shape.Compose(context = context)
    }

    override val modified: ModifiedShape
        get() = shape.modified

    override val canOutsetForStroke: Boolean
        get() = shape.canOutsetForStroke

    @androidx.annotation.Keep
    companion object: ShapeCompanion {
    }
}

class RectangleCornerRadii {
    val topLeading: Double
    val bottomLeading: Double
    val bottomTrailing: Double
    val topTrailing: Double

    constructor(topLeading: Double = 0.0, bottomLeading: Double = 0.0, bottomTrailing: Double = 0.0, topTrailing: Double = 0.0) {
        this.topLeading = topLeading
        this.bottomLeading = bottomLeading
        this.bottomTrailing = bottomTrailing
        this.topTrailing = topTrailing
    }


    override fun equals(other: Any?): Boolean {
        if (other !is RectangleCornerRadii) return false
        return topLeading == other.topLeading && bottomLeading == other.bottomLeading && bottomTrailing == other.bottomTrailing && topTrailing == other.topTrailing
    }

    @androidx.annotation.Keep
    companion object {
    }
}

/*
/// No-op
func stubShape() -> some Shape {
//return never() // raises warning: “A call to a never-returning function”
struct NeverShape : Shape {
typealias AnimatableData = Never
typealias Body = Never
var body: Body { fatalError() }
func path(in rect: CGRect) -> Path { fatalError() }
}
return NeverShape()
}

extension Shape {
/// An indication of how to style a shape.
///
/// SkipUI looks at a shape's role when deciding how to apply a
/// ``ShapeStyle`` at render time. The ``Shape`` protocol provides a
/// default implementation with a value of ``ShapeRole/fill``. If you
/// create a composite shape, you can provide an override of this property
/// to return another value, if appropriate.
@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
static var role: ShapeRole { get { fatalError() } }
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
extension Shape {

/// Returns a new shape with filled regions common to both shapes.
///
/// - Parameters:
///   - other: The shape to intersect.
///   - eoFill: Whether to use the even-odd rule for determining
///       which areas to treat as the interior of the shapes (if true),
///       or the non-zero rule (if false).
/// - Returns: A new shape.
///
/// The filled region of the resulting shape is the overlapping area
/// of the filled region of both shapes.  This can be used to clip
/// the fill of a shape to a mask.
///
/// Any unclosed subpaths in either shape are assumed to be closed.
/// The result of filling this shape using either even-odd or
/// non-zero fill rules is identical.
public func intersection<T>(_ other: T, eoFill: Bool = false) -> some Shape where T : Shape { stubShape() }


/// Returns a new shape with filled regions in either this shape or
/// the given shape.
///
/// - Parameters:
///   - other: The shape to union.
///   - eoFill: Whether to use the even-odd rule for determining
///       which areas to treat as the interior of the shapes (if true),
///       or the non-zero rule (if false).
/// - Returns: A new shape.
///
/// The filled region of resulting shape is the combination of the
/// filled region of both shapes added together.
///
/// Any unclosed subpaths in either shape are assumed to be closed.
/// The result of filling this shape using either even-odd or
/// non-zero fill rules is identical.
public func union<T>(_ other: T, eoFill: Bool = false) -> some Shape where T : Shape { stubShape() }


/// Returns a new shape with filled regions from this shape that are
/// not in the given shape.
///
/// - Parameters:
///   - other: The shape to subtract.
///   - eoFill: Whether to use the even-odd rule for determining
///       which areas to treat as the interior of the shapes (if true),
///       or the non-zero rule (if false).
/// - Returns: A new shape.
///
/// The filled region of the resulting shape is the filled region of
/// this shape with the filled  region `other` removed from it.
///
/// Any unclosed subpaths in either shape are assumed to be closed.
/// The result of filling this shape using either even-odd or
/// non-zero fill rules is identical.
public func subtracting<T>(_ other: T, eoFill: Bool = false) -> some Shape where T : Shape { stubShape() }


/// Returns a new shape with filled regions either from this shape or
/// the given shape, but not in both.
///
/// - Parameters:
///   - other: The shape to difference.
///   - eoFill: Whether to use the even-odd rule for determining
///       which areas to treat as the interior of the shapes (if true),
///       or the non-zero rule (if false).
/// - Returns: A new shape.
///
/// The filled region of the resulting shape is the filled region
/// contained in either this shape or `other`, but not both.
///
/// Any unclosed subpaths in either shape are assumed to be closed.
/// The result of filling this shape using either even-odd or
/// non-zero fill rules is identical.
public func symmetricDifference<T>(_ other: T, eoFill: Bool = false) -> some Shape where T : Shape { stubShape() }


/// Returns a new shape with a line from this shape that overlaps the
/// filled regions of the given shape.
///
/// - Parameters:
///   - other: The shape to intersect.
///   - eoFill: Whether to use the even-odd rule for determining
///       which areas to treat as the interior of the shapes (if true),
///       or the non-zero rule (if false).
/// - Returns: A new shape.
///
/// The line of the resulting shape is the line of this shape that
/// overlaps the filled region of `other`.
///
/// Intersected subpaths that are clipped create open subpaths.
/// Closed subpaths that do not intersect `other` remain closed.
public func lineIntersection<T>(_ other: T, eoFill: Bool = false) -> some Shape where T : Shape { stubShape() }


/// Returns a new shape with a line from this shape that does not
/// overlap the filled region of the given shape.
///
/// - Parameters:
///   - other: The shape to subtract.
///   - eoFill: Whether to use the even-odd rule for determining
///       which areas to treat as the interior of the shapes (if true),
///       or the non-zero rule (if false).
/// - Returns: A new shape.
///
/// The line of the resulting shape is the line of this shape that
/// does not overlap the filled region of `other`.
///
/// Intersected subpaths that are clipped create open subpaths.
/// Closed subpaths that do not intersect `other` remain closed.
public func lineSubtraction<T>(_ other: T, eoFill: Bool = false) -> some Shape where T : Shape { stubShape() }

}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension Shape {

/// Trims this shape by a fractional amount based on its representation as a
/// path.
///
/// To create a `Shape` instance, you define the shape's path using lines and
/// curves. Use the `trim(from:to:)` method to draw a portion of a shape by
/// ignoring portions of the beginning and ending of the shape's path.
///
/// For example, if you're drawing a figure eight or infinity symbol (∞)
/// starting from its center, setting the `startFraction` and `endFraction`
/// to different values determines the parts of the overall shape.
///
/// The following example shows a simplified infinity symbol that draws
/// only three quarters of the full shape. That is, of the two lobes of the
/// symbol, one lobe is complete and the other is half complete.
///
///     Path { path in
///         path.addLines([
///             .init(x: 2, y: 1),
///             .init(x: 1, y: 0),
///             .init(x: 0, y: 1),
///             .init(x: 1, y: 2),
///             .init(x: 3, y: 0),
///             .init(x: 4, y: 1),
///             .init(x: 3, y: 2),
///             .init(x: 2, y: 1)
///         ])
///     }
///     .trim(from: 0.25, to: 1.0)
///     .scale(50, anchor: .topLeading)
///     .stroke(Color.black, lineWidth: 3)
///
/// Changing the parameters of `trim(from:to:)` to
/// `.trim(from: 0, to: 1)` draws the full infinity symbol, while
/// `.trim(from: 0, to: 0.5)` draws only the left lobe of the symbol.
///
/// - Parameters:
///   - startFraction: The fraction of the way through drawing this shape
///     where drawing starts.
///   - endFraction: The fraction of the way through drawing this shape
///     where drawing ends.
/// - Returns: A shape built by capturing a portion of this shape's path.
public func trim(from startFraction: CGFloat = 0, to endFraction: CGFloat = 1) -> some Shape { stubShape() }
// NOTE: animatable property

}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension Shape {
/// Applies an affine transform to this shape.
///
/// Affine transforms present a mathematical approach to applying
/// combinations of rotation, scaling, translation, and skew to shapes.
///
/// - Parameter transform: The affine transformation matrix to apply to this
///   shape.
///
/// - Returns: A transformed shape, based on its matrix values.
public func transform(_ transform: CGAffineTransform) -> TransformedShape<Self> { fatalError() }
}

@available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
extension Shape where Self == ContainerRelativeShape {

/// A shape that is replaced by an inset version of the current
/// container shape. If no container shape was defined, is replaced by
/// a rectangle.
public static var containerRelative: ContainerRelativeShape { get { fatalError() } }
}

/// Ways of styling a shape.
@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
public enum ShapeRole : Sendable {

/// Indicates to the shape's style that SkipUI fills the shape.
case fill

/// Indicates to the shape's style that SkipUI applies a stroke to
/// the shape's path.
case stroke

/// Indicates to the shape's style that SkipUI uses the shape as a
/// separator.
case separator
}

@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
extension ShapeRole : Equatable {
}

@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
extension ShapeRole : Hashable {
}

/// A shape with an affine transform applied to it.
@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
@frozen public struct TransformedShape<Content> : Shape where Content : Shape {

public var shape: Content { get { fatalError() } }

public var transform: CGAffineTransform { get { fatalError() } }

@inlinable public init(shape: Content, transform: CGAffineTransform) { fatalError() }

/// Describes this shape as a path within a rectangular frame of reference.
///
/// - Parameter rect: The frame of reference for describing this shape.
///
/// - Returns: A path that describes this shape.
public func path(in rect: CGRect) -> Path { fatalError() }

/// An indication of how to style a shape.
///
/// SkipUI looks at a shape's role when deciding how to apply a
/// ``ShapeStyle`` at render time. The ``Shape`` protocol provides a
/// default implementation with a value of ``ShapeRole/fill``. If you
/// create a composite shape, you can provide an override of this property
/// to return another value, if appropriate.
@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
public static var role: ShapeRole { get { fatalError() } }

/// Returns the behavior this shape should use for different layout
/// directions.
///
/// If the layoutDirectionBehavior for a Shape is one that mirrors, the
/// shape's path will be mirrored horizontally when in the specified layout
/// direction. When mirrored, the individual points of the path will be
/// transformed.
///
/// Defaults to `.mirrors` when deploying on iOS 17.0, macOS 14.0,
/// tvOS 17.0, watchOS 10.0 and later, and to `.fixed` if not.
/// To mirror a path when deploying to earlier releases, either use
/// `View.flipsForRightToLeftLayoutDirection` for a filled or stroked
/// shape or conditionally mirror the points in the path of the shape.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
public var layoutDirectionBehavior: LayoutDirectionBehavior { get { fatalError() } }

/// The type defining the data to animate.
//public typealias AnimatableData = Content.AnimatableData

/// The data to animate.
//public var animatableData: AnimatableData { get { fatalError() } set { } }

public typealias Body = NeverView
public var body: Body { fatalError() }
}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension Rectangle : InsettableShape {

/// Returns `self` inset by `amount`.
public func inset(by amount: CGFloat) -> InsetShape { fatalError() }


/// The type of the inset shape.
public typealias InsetShape = Never
}

/// A shape with a rotation transform applied to it.
@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
@frozen public struct RotatedShape<Content> : Shape where Content : Shape {

public var shape: Content { get { fatalError() } }

public var angle: Angle { get { fatalError() } }

public var anchor: UnitPoint { get { fatalError() } }

@inlinable public init(shape: Content, angle: Angle, anchor: UnitPoint = .center) { fatalError() }

/// Describes this shape as a path within a rectangular frame of reference.
///
/// - Parameter rect: The frame of reference for describing this shape.
///
/// - Returns: A path that describes this shape.
public func path(in rect: CGRect) -> Path { fatalError() }

/// An indication of how to style a shape.
///
/// SkipUI looks at a shape's role when deciding how to apply a
/// ``ShapeStyle`` at render time. The ``Shape`` protocol provides a
/// default implementation with a value of ``ShapeRole/fill``. If you
/// create a composite shape, you can provide an override of this property
/// to return another value, if appropriate.
@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
public static var role: ShapeRole { get { fatalError() } }

/// Returns the behavior this shape should use for different layout
/// directions.
///
/// If the layoutDirectionBehavior for a Shape is one that mirrors, the
/// shape's path will be mirrored horizontally when in the specified layout
/// direction. When mirrored, the individual points of the path will be
/// transformed.
///
/// Defaults to `.mirrors` when deploying on iOS 17.0, macOS 14.0,
/// tvOS 17.0, watchOS 10.0 and later, and to `.fixed` if not.
/// To mirror a path when deploying to earlier releases, either use
/// `View.flipsForRightToLeftLayoutDirection` for a filled or stroked
/// shape or conditionally mirror the points in the path of the shape.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
public var layoutDirectionBehavior: LayoutDirectionBehavior { get { fatalError() } }

/// The type defining the data to animate.
//public typealias AnimatableData = AnimatablePair<Content.AnimatableData, AnimatablePair<Angle.AnimatableData, UnitPoint.AnimatableData>>

/// The data to animate.
//public var animatableData: AnimatableData { get { fatalError() } set { } }

public typealias Body = NeverView
public var body: Body { fatalError() }
}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension RotatedShape : InsettableShape where Content : InsettableShape {

/// Returns `self` inset by `amount`.
public func inset(by amount: CGFloat) -> RotatedShape<Content.InsetShape> { fatalError() }

/// The type of the inset shape.
public typealias InsetShape = RotatedShape<Content.InsetShape>
}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension RoundedRectangle : InsettableShape {

/// Returns `self` inset by `amount`.
public func inset(by amount: CGFloat) -> InsetShape { fatalError() }


/// The type of the inset shape.
public typealias InsetShape = Never
}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension Capsule : InsettableShape {
/// Returns `self` inset by `amount`.
public func inset(by amount: CGFloat) -> InsetShape { stub() as Never }


/// The type of the inset shape.
public typealias InsetShape = Never
}

/// A shape that is replaced by an inset version of the current
/// container shape. If no container shape was defined, is replaced by
/// a rectangle.
@available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
@frozen public struct ContainerRelativeShape : Shape {

/// Describes this shape as a path within a rectangular frame of reference.
///
/// - Parameter rect: The frame of reference for describing this shape.
///
/// - Returns: A path that describes this shape.
public func path(in rect: CGRect) -> Path { fatalError() }

@inlinable public init() { fatalError() }

/// The type defining the data to animate.
public typealias AnimatableData = EmptyAnimatableData
public var animatableData: AnimatableData { get { fatalError() } set { } }

public typealias Body = NeverView
public var body: Body { fatalError() }
}

@available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
extension ContainerRelativeShape : InsettableShape {

/// Returns `self` inset by `amount`.
public func inset(by amount: CGFloat) -> InsetShape { fatalError() }


/// The type of the inset shape.
public typealias InsetShape = Never
}

@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
extension Circle {

/// Returns the size of the view that will render the shape, given
/// a proposed size.
///
/// Implement this method to tell the container of the shape how
/// much space the shape needs to render itself, given a size
/// proposal.
///
/// See ``Layout/sizeThatFits(proposal:subviews:cache:)``
/// for more details about how the layout system chooses the size of
/// views.
///
/// - Parameters:
///   - proposal: A size proposal for the container.
///
/// - Returns: A size that indicates how much space the shape needs.
public func sizeThatFits(_ proposal: ProposedViewSize) -> CGSize { fatalError() }
}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension Circle : InsettableShape {

/// Returns `self` inset by `amount`.
public func inset(by amount: CGFloat) -> InsetShape { fatalError() }


/// The type of the inset shape.
public typealias InsetShape = Never
}

/// A shape with a scale transform applied to it.
@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
@frozen public struct ScaledShape<Content> : Shape where Content : Shape {

public var shape: Content { get { fatalError() } }

public var scale: CGSize { get { fatalError() } }

public var anchor: UnitPoint { get { fatalError() } }

@inlinable public init(shape: Content, scale: CGSize, anchor: UnitPoint = .center) { fatalError() }

/// Describes this shape as a path within a rectangular frame of reference.
///
/// - Parameter rect: The frame of reference for describing this shape.
///
/// - Returns: A path that describes this shape.
public func path(in rect: CGRect) -> Path { fatalError() }

/// An indication of how to style a shape.
///
/// SkipUI looks at a shape's role when deciding how to apply a
/// ``ShapeStyle`` at render time. The ``Shape`` protocol provides a
/// default implementation with a value of ``ShapeRole/fill``. If you
/// create a composite shape, you can provide an override of this property
/// to return another value, if appropriate.
@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
public static var role: ShapeRole { get { fatalError() } }

/// Returns the behavior this shape should use for different layout
/// directions.
///
/// If the layoutDirectionBehavior for a Shape is one that mirrors, the
/// shape's path will be mirrored horizontally when in the specified layout
/// direction. When mirrored, the individual points of the path will be
/// transformed.
///
/// Defaults to `.mirrors` when deploying on iOS 17.0, macOS 14.0,
/// tvOS 17.0, watchOS 10.0 and later, and to `.fixed` if not.
/// To mirror a path when deploying to earlier releases, either use
/// `View.flipsForRightToLeftLayoutDirection` for a filled or stroked
/// shape or conditionally mirror the points in the path of the shape.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
public var layoutDirectionBehavior: LayoutDirectionBehavior { get { fatalError() } }

/// The type defining the data to animate.
public typealias AnimatableData = Never // AnimatablePair<Content.AnimatableData, AnimatablePair<CGSize.AnimatableData, UnitPoint.AnimatableData>>

/// The data to animate.
public var animatableData: AnimatableData { get { fatalError() } set { } }

public typealias Body = NeverView
public var body: Body { fatalError() }
}

@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
extension UnevenRoundedRectangle : InsettableShape {

/// Returns `self` inset by `amount`.
public func inset(by amount: CGFloat) -> InsetShape { fatalError() }


/// The type of the inset shape.
public typealias InsetShape = Never
}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension Ellipse : InsettableShape {

/// Returns `self` inset by `amount`.
public func inset(by amount: CGFloat) -> InsetShape { fatalError() }


/// The type of the inset shape.
public typealias InsetShape = Never
}

/// A shape type that is able to inset itself to produce another shape.
@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
public protocol InsettableShape : Shape {

/// The type of the inset shape.
associatedtype InsetShape : InsettableShape

/// Returns `self` inset by `amount`.
func inset(by amount: CGFloat) -> Self.InsetShape
}

/// A shape with a translation offset transform applied to it.
@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
@frozen public struct OffsetShape<Content> : Shape where Content : Shape {

public var shape: Content { get { fatalError() } }

public var offset: CGSize { get { fatalError() } }

@inlinable public init(shape: Content, offset: CGSize) { fatalError() }

/// Describes this shape as a path within a rectangular frame of reference.
///
/// - Parameter rect: The frame of reference for describing this shape.
///
/// - Returns: A path that describes this shape.
public func path(in rect: CGRect) -> Path { fatalError() }

/// An indication of how to style a shape.
///
/// SkipUI looks at a shape's role when deciding how to apply a
/// ``ShapeStyle`` at render time. The ``Shape`` protocol provides a
/// default implementation with a value of ``ShapeRole/fill``. If you
/// create a composite shape, you can provide an override of this property
/// to return another value, if appropriate.
@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
public static var role: ShapeRole { get { fatalError() } }

/// Returns the behavior this shape should use for different layout
/// directions.
///
/// If the layoutDirectionBehavior for a Shape is one that mirrors, the
/// shape's path will be mirrored horizontally when in the specified layout
/// direction. When mirrored, the individual points of the path will be
/// transformed.
///
/// Defaults to `.mirrors` when deploying on iOS 17.0, macOS 14.0,
/// tvOS 17.0, watchOS 10.0 and later, and to `.fixed` if not.
/// To mirror a path when deploying to earlier releases, either use
/// `View.flipsForRightToLeftLayoutDirection` for a filled or stroked
/// shape or conditionally mirror the points in the path of the shape.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
public var layoutDirectionBehavior: LayoutDirectionBehavior { get { fatalError() } }

/// The type defining the data to animate.
public typealias AnimatableData = Never // AnimatablePair<Content.AnimatableData, CGSize.AnimatableData>

/// The data to animate.
public var animatableData: AnimatableData { get { fatalError() } set { } }

public typealias Body = NeverView
public var body: Body { fatalError() }
}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension OffsetShape : InsettableShape where Content : InsettableShape {

/// Returns `self` inset by `amount`.
public func inset(by amount: CGFloat) -> OffsetShape<Content.InsetShape> { fatalError() }

/// The type of the inset shape.
public typealias InsetShape = OffsetShape<Content.InsetShape>
}

extension Never : Shape {
public typealias AnimatableData = Never
public var animatableData: AnimatableData { get { fatalError() } set { } }

public func path(in rect: CGRect) -> Path {
fatalError()
}
}

extension Never : InsettableShape {
public func inset(by amount: CGFloat) -> Never {
fatalError()
}
}
*/
