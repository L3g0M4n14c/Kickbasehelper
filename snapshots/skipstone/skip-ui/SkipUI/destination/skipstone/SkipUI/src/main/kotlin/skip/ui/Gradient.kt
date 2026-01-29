package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Suppress("MUST_BE_INITIALIZED")
class Gradient: ShapeStyle, MutableStruct {
    class Stop {
        val color: Color
        val location: Double

        constructor(color: Color, location: Double) {
            this.color = color.sref()
            this.location = location
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Gradient.Stop) return false
            return color == other.color && location == other.location
        }

        override fun hashCode(): Int {
            var result = 1
            result = Hasher.combine(result, color)
            result = Hasher.combine(result, location)
            return result
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    var stops: Array<Gradient.Stop>
        get() = field.sref({ this.stops = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(stops: Array<Gradient.Stop>) {
        this.stops = stops
    }

    constructor(colors: Array<Color>, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) {
        if (colors.isEmpty) {
            this.stops = arrayOf()
        } else {
            val step = if (colors.count == 1) 0.0 else 1.0 / Double(colors.count - 1)
            this.stops = colors.enumerated().map { it -> Gradient.Stop(color = it.element1, location = step * Double(it.element0)) }
        }
    }

    @Composable
    override fun asBrush(opacity: Double, animationContext: ComposeContext?): Brush? = AnyGradient(gradient = this).asBrush(opacity = opacity, animationContext = animationContext)

    @Composable
    internal fun colorStops(opacity: Double = 1.0): kotlin.collections.List<Pair<Float, androidx.compose.ui.graphics.Color>> {
        val list = mutableListOf<Pair<Float, androidx.compose.ui.graphics.Color>>()
        for (stop in stops.sref()) {
            list.add(Pair(Float(stop.location), stop.color.opacity(opacity).colorImpl()))
        }
        return list.sref()
    }

    class ColorSpace {

        override fun equals(other: Any?): Boolean = other is Gradient.ColorSpace

        override fun hashCode(): Int = "Gradient.ColorSpace".hashCode()

        @androidx.annotation.Keep
        companion object {
            val device = Gradient.ColorSpace()
            val perceptual = Gradient.ColorSpace()
        }
    }

    fun colorSpace(space: Gradient.ColorSpace): AnyGradient = AnyGradient(gradient = this)

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as Gradient
        this.stops = copy.stops
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = Gradient(this as MutableStruct)

    override fun equals(other: Any?): Boolean {
        if (other !is Gradient) return false
        return stops == other.stops
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, stops)
        return result
    }

    @androidx.annotation.Keep
    companion object: ShapeStyleCompanion {
    }
}

class AnyGradient: ShapeStyle, Renderable {
    internal val gradient: LinearGradient

    constructor(gradient: Gradient) {
        this.gradient = LinearGradient(gradient = gradient, startPoint = UnitPoint(x = 0.5, y = 0.0), endPoint = UnitPoint(x = 0.5, y = 1.0))
    }

    @Composable
    override fun Render(context: ComposeContext) {
        gradient.Compose(context = context)
    }

    // MARK: - ShapeStyle

    @Composable
    override fun asBrush(opacity: Double, animationContext: ComposeContext?): Brush? = gradient.asBrush(opacity = opacity, animationContext = animationContext)

    @androidx.annotation.Keep
    companion object: ShapeStyleCompanion {
    }
}

@androidx.annotation.Keep
class LinearGradient: ShapeStyle, Renderable, skip.lib.SwiftProjecting {
    internal val gradient: Gradient
    internal val startPoint: UnitPoint
    internal val endPoint: UnitPoint

    constructor(gradient: Gradient, startPoint: UnitPoint, endPoint: UnitPoint) {
        this.gradient = gradient.sref()
        this.startPoint = startPoint.sref()
        this.endPoint = endPoint.sref()
    }

    constructor(stops: Array<Gradient.Stop>, startPoint: UnitPoint, endPoint: UnitPoint): this(gradient = Gradient(stops = stops), startPoint = startPoint, endPoint = endPoint) {
    }

    constructor(colors: Array<Color>, startPoint: UnitPoint, endPoint: UnitPoint, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null): this(gradient = Gradient(colors = colors), startPoint = startPoint, endPoint = endPoint) {
    }

    constructor(colors: Array<Color>, locations: Array<Double>, startX: Double, startY: Double, endX: Double, endY: Double) {
        val stops = zip(colors, locations).map { it -> Gradient.Stop(color = it.element0, location = it.element1) }
        this.gradient = Gradient(stops = stops)
        this.startPoint = UnitPoint(x = startX, y = startY)
        this.endPoint = UnitPoint(x = endX, y = endY)
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val modifier = context.modifier.background(asBrush(opacity = 1.0, animationContext = null)!!).fillSize()
        Box(modifier = modifier)
    }

    // MARK: - ShapeStyle

    @Composable
    override fun asBrush(opacity: Double, animationContext: ComposeContext?): Brush? {
        val stops = gradient.colorStops(opacity = opacity)
        val rememberedOpacity = remember { -> mutableStateOf(opacity) }
        val rememberedBrush = remember { -> mutableStateOf(LinearGradientShaderBrush(colorStops = stops, startPoint = startPoint, endPoint = endPoint)) }
        if (opacity != rememberedOpacity.value) {
            rememberedOpacity.value = opacity
            rememberedBrush.value = LinearGradientShaderBrush(colorStops = stops, startPoint = startPoint, endPoint = endPoint)
        }
        return rememberedBrush.value.sref()
    }

    private class LinearGradientShaderBrush: ShaderBrush {
        internal val colorStops: kotlin.collections.List<Pair<Float, androidx.compose.ui.graphics.Color>>
        internal val startPoint: UnitPoint
        internal val endPoint: UnitPoint

        override fun createShader(size: androidx.compose.ui.geometry.Size): Shader {
            val from = Offset(x = size.width * Float(startPoint.x), y = size.height * Float(startPoint.y))
            val to = Offset(x = size.width * Float(endPoint.x), y = size.height * Float(endPoint.y))
            return LinearGradientShader(from, to, colors = colorStops.map { it -> it.second }, colorStops = colorStops.map { it -> it.first }, tileMode = TileMode.Clamp)
        }

        constructor(colorStops: kotlin.collections.List<Pair<Float, androidx.compose.ui.graphics.Color>>, startPoint: UnitPoint, endPoint: UnitPoint) {
            this.colorStops = colorStops.sref()
            this.startPoint = startPoint.sref()
            this.endPoint = endPoint.sref()
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: ShapeStyleCompanion {

        fun linearGradient(gradient: Gradient, startPoint: UnitPoint, endPoint: UnitPoint): LinearGradient = LinearGradient(gradient = gradient, startPoint = startPoint, endPoint = endPoint)

        fun linearGradient(colors: Array<Color>, startPoint: UnitPoint, endPoint: UnitPoint, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null): LinearGradient = LinearGradient(colors = colors, startPoint = startPoint, endPoint = endPoint)

        fun linearGradient(stops: Array<Gradient.Stop>, startPoint: UnitPoint, endPoint: UnitPoint): LinearGradient = LinearGradient(stops = stops, startPoint = startPoint, endPoint = endPoint)
    }
}

@androidx.annotation.Keep
class EllipticalGradient: ShapeStyle, Renderable, skip.lib.SwiftProjecting {
    internal val gradient: Gradient
    internal val center: UnitPoint
    internal val startFraction: Double
    internal val endFraction: Double

    constructor(gradient: Gradient, center: UnitPoint = UnitPoint.center, startRadiusFraction: Double = 0.0, endRadiusFraction: Double = 0.5) {
        this.gradient = gradient.sref()
        this.center = center.sref()
        this.startFraction = startRadiusFraction
        this.endFraction = endRadiusFraction
    }

    constructor(colors: Array<Color>, center: UnitPoint = UnitPoint.center, startRadiusFraction: Double = 0.0, endRadiusFraction: Double = 0.5, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null): this(gradient = Gradient(colors = colors), center = center, startRadiusFraction = startRadiusFraction, endRadiusFraction = endRadiusFraction) {
    }

    constructor(stops: Array<Gradient.Stop>, center: UnitPoint = UnitPoint.center, startRadiusFraction: Double = 0.0, endRadiusFraction: Double = 0.5): this(gradient = Gradient(stops = stops), center = center, startRadiusFraction = startRadiusFraction, endRadiusFraction = endRadiusFraction) {
    }

    constructor(colors: Array<Color>, locations: Array<Double>, centerX: Double, centerY: Double, startFraction: Double, endFraction: Double) {
        val stops = zip(colors, locations).map { it -> Gradient.Stop(color = it.element0, location = it.element1) }
        this.gradient = Gradient(stops = stops)
        this.center = UnitPoint(x = centerX, y = centerY)
        this.startFraction = startFraction
        this.endFraction = endFraction
    }

    @Composable
    override fun Render(context: ComposeContext) {
        // Trick to scale our (circular) radial brush into an ellipse when this gradient is used as a view
        BoxWithConstraints(modifier = context.modifier.fillSize().clipToBounds()) { ->
            val aspectRatio = (maxWidth / maxHeight).sref()
            val modifier = Modifier.fillMaxSize().scale(max(aspectRatio, 1.0f), max(1.0f / aspectRatio, 1.0f)).background(asBrush(opacity = 1.0, animationContext = null)!!!!)
            Box(modifier = modifier)
        }
    }

    // MARK: - ShapeStyle

    @Composable
    override fun asBrush(opacity: Double, animationContext: ComposeContext?): Brush? {
        val stops = gradient.colorStops(opacity = opacity)
        val brush = remember { -> RadialGradientShaderBrush(colorStops = stops, center = center, startFraction = startFraction, endFraction = endFraction) }
        return brush.sref()
    }

    private class RadialGradientShaderBrush: ShaderBrush {
        internal val colorStops: kotlin.collections.List<Pair<Float, androidx.compose.ui.graphics.Color>>
        internal val center: UnitPoint
        internal val startFraction: Double
        internal val endFraction: Double

        override fun createShader(size: androidx.compose.ui.geometry.Size): Shader {
            // TODO: We are not creating an ellipitcal gradient (which appears to be impossible in Android).
            // Rather, this is just a normal RadialGradient that fills the smallest dimension
            val center = Offset(x = size.width * Float(center.x), y = size.height * Float(center.y))
            val radius = Float(min(size.width, size.height) * endFraction)
            return RadialGradientShader(center = center, radius = radius, colors = colorStops.map { it -> it.second }, colorStops = colorStops.map { it -> Float(startFraction) + it.first * Float(1.0 - startFraction) }, tileMode = TileMode.Clamp)
        }

        constructor(colorStops: kotlin.collections.List<Pair<Float, androidx.compose.ui.graphics.Color>>, center: UnitPoint, startFraction: Double, endFraction: Double) {
            this.colorStops = colorStops.sref()
            this.center = center.sref()
            this.startFraction = startFraction
            this.endFraction = endFraction
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: ShapeStyleCompanion {

        fun ellipticalGradient(gradient: Gradient, center: UnitPoint = UnitPoint.center, startRadiusFraction: Double = 0.0, endRadiusFraction: Double = 0.5): EllipticalGradient = EllipticalGradient(gradient = gradient, center = center, startRadiusFraction = startRadiusFraction, endRadiusFraction = endRadiusFraction)

        fun ellipticalGradient(colors: Array<Color>, center: UnitPoint = UnitPoint.center, startRadiusFraction: Double = 0.0, endRadiusFraction: Double = 0.5, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null): EllipticalGradient = EllipticalGradient(colors = colors, center = center, startRadiusFraction = startRadiusFraction, endRadiusFraction = endRadiusFraction)

        fun ellipticalGradient(stops: Array<Gradient.Stop>, center: UnitPoint = UnitPoint.center, startRadiusFraction: Double = 0.0, endRadiusFraction: Double = 0.5): EllipticalGradient = EllipticalGradient(stops = stops, center = center, startRadiusFraction = startRadiusFraction, endRadiusFraction = endRadiusFraction)
    }
}

@androidx.annotation.Keep
class RadialGradient: ShapeStyle, Renderable, skip.lib.SwiftProjecting {
    internal val gradient: Gradient
    internal val center: UnitPoint
    internal val startRadius: Double
    internal val endRadius: Double

    constructor(gradient: Gradient, center: UnitPoint, startRadius: Double, endRadius: Double) {
        this.gradient = gradient.sref()
        this.center = center.sref()
        this.startRadius = startRadius
        this.endRadius = endRadius
    }

    constructor(colors: Array<Color>, center: UnitPoint, startRadius: Double, endRadius: Double, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null): this(gradient = Gradient(colors = colors), center = center, startRadius = startRadius, endRadius = endRadius) {
    }

    constructor(stops: Array<Gradient.Stop>, center: UnitPoint, startRadius: Double, endRadius: Double): this(gradient = Gradient(stops = stops), center = center, startRadius = startRadius, endRadius = endRadius) {
    }

    constructor(colors: Array<Color>, locations: Array<Double>, centerX: Double, centerY: Double, startRadius: Double, endRadius: Double) {
        val stops = zip(colors, locations).map { it -> Gradient.Stop(color = it.element0, location = it.element1) }
        this.gradient = Gradient(stops = stops)
        this.center = UnitPoint(x = centerX, y = centerY)
        this.startRadius = startRadius
        this.endRadius = endRadius
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val modifier = context.modifier.background(asBrush(opacity = 1.0, animationContext = null)!!).fillSize()
        Box(modifier = modifier)
    }

    // MARK: - ShapeStyle

    @Composable
    override fun asBrush(opacity: Double, animationContext: ComposeContext?): Brush? {
        val density = LocalDensity.current.sref()
        val start = with(density) { -> startRadius.dp.toPx() }
        val end = with(density) { -> endRadius.dp.toPx() }
        val stops = gradient.colorStops(opacity = opacity)
        val brush = remember { -> RadialGradientShaderBrush(colorStops = stops, center = center, startRadius = start, endRadius = end) }
        return brush.sref()
    }

    private class RadialGradientShaderBrush: ShaderBrush {
        internal val colorStops: kotlin.collections.List<Pair<Float, androidx.compose.ui.graphics.Color>>
        internal val center: UnitPoint
        internal val startRadius: Float
        internal val endRadius: Float

        override fun createShader(size: androidx.compose.ui.geometry.Size): Shader {
            val center = Offset(x = size.width * Float(center.x), y = size.height * Float(center.y))
            val startFraction = if (endRadius == 0.0f) 0.0f else startRadius / endRadius
            return RadialGradientShader(center = center, radius = endRadius, colors = colorStops.map { it -> it.second }, colorStops = colorStops.map { it -> startFraction + it.first * (1.0f - startFraction) }, tileMode = TileMode.Clamp)
        }

        constructor(colorStops: kotlin.collections.List<Pair<Float, androidx.compose.ui.graphics.Color>>, center: UnitPoint, startRadius: Float, endRadius: Float) {
            this.colorStops = colorStops.sref()
            this.center = center.sref()
            this.startRadius = startRadius
            this.endRadius = endRadius
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: ShapeStyleCompanion {

        fun radialGradient(gradient: Gradient, center: UnitPoint, startRadius: Double, endRadius: Double): RadialGradient = RadialGradient(gradient = gradient, center = center, startRadius = startRadius, endRadius = endRadius)

        fun radialGradient(colors: Array<Color>, center: UnitPoint, startRadius: Double, endRadius: Double, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null): RadialGradient = RadialGradient(colors = colors, center = center, startRadius = startRadius, endRadius = endRadius)

        fun radialGradient(stops: Array<Gradient.Stop>, center: UnitPoint, startRadius: Double, endRadius: Double): RadialGradient = RadialGradient(stops = stops, center = center, startRadius = startRadius, endRadius = endRadius)
    }
}

class AngularGradient: ShapeStyle {
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(gradient: Gradient, center: UnitPoint, startAngle: Angle = Angle.zero, endAngle: Angle = Angle.zero) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(colors: Array<Color>, center: UnitPoint, startAngle: Angle, endAngle: Angle, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(stops: Array<Gradient.Stop>, center: UnitPoint, startAngle: Angle, endAngle: Angle) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(gradient: Gradient, center: UnitPoint, angle: Angle = Angle.zero) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(colors: Array<Color>, center: UnitPoint, angle: Angle = Angle.zero, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(stops: Array<Gradient.Stop>, center: UnitPoint, angle: Angle = Angle.zero) {
    }


    @androidx.annotation.Keep
    companion object: ShapeStyleCompanion {

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun angularGradient(gradient: Gradient, center: UnitPoint, startAngle: Angle, endAngle: Angle): AngularGradient {
            fatalError()
        }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun angularGradient(colors: Array<Color>, center: UnitPoint, startAngle: Angle, endAngle: Angle, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null): AngularGradient {
            fatalError()
        }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun angularGradient(stops: Array<Gradient.Stop>, center: UnitPoint, startAngle: Angle, endAngle: Angle): AngularGradient {
            fatalError()
        }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun conicGradient(gradient: Gradient, center: UnitPoint, angle: Angle = Angle.zero): AngularGradient {
            fatalError()
        }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun conicGradient(colors: Array<Color>, center: UnitPoint, angle: Angle = Angle.zero, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null): AngularGradient {
            fatalError()
        }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun conicGradient(stops: Array<Gradient.Stop>, center: UnitPoint, angle: Angle = Angle.zero): AngularGradient {
            fatalError()
        }
    }
}

