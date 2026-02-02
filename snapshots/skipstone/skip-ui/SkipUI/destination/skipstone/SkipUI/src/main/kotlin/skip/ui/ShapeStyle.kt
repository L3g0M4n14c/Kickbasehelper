package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor

// Note: ShapeStyle does not extend View in SwiftUI, but most concrete ShapeStyles do and it helps us disambiguate calls
// to functions that are overloaded on both View and ShapeStyle, like some .background(...) variants
interface ShapeStyle: View {
    @Composable
    fun asColor(opacity: Double, animationContext: ComposeContext?): androidx.compose.ui.graphics.Color? = null

    @Composable
    fun asBrush(opacity: Double, animationContext: ComposeContext?): Brush? {
        val color_0 = asColor(opacity = opacity, animationContext = animationContext)
        if (color_0 == null) {
            return null
        }
        return SolidColor(color_0)
    }
}
interface ShapeStyleCompanion {
}


class AnyShapeStyle: ShapeStyle {
    internal val style: ShapeStyle
    internal val opacity: Double

    constructor(style: ShapeStyle, opacity: Double = 1.0) {
        this.style = style.sref()
        this.opacity = opacity
    }

    @Composable
    override fun asColor(opacity: Double, animationContext: ComposeContext?): androidx.compose.ui.graphics.Color? = style.asColor(opacity = opacity * this.opacity, animationContext = animationContext)

    @Composable
    override fun asBrush(opacity: Double, animationContext: ComposeContext?): Brush? = style.asBrush(opacity = opacity * this.opacity, animationContext = animationContext)

    @androidx.annotation.Keep
    companion object: ShapeStyleCompanion {
    }
}

@androidx.annotation.Keep
class ForegroundStyle: ShapeStyle, skip.lib.SwiftProjecting {

    constructor() {
    }

    @Composable
    override fun asColor(opacity: Double, animationContext: ComposeContext?): androidx.compose.ui.graphics.Color? {
        return EnvironmentValues.shared._foregroundStyle?.asColor(opacity = opacity, animationContext = animationContext)
    }

    @Composable
    override fun asBrush(opacity: Double, animationContext: ComposeContext?): Brush? {
        return EnvironmentValues.shared._foregroundStyle?.asBrush(opacity = opacity, animationContext = animationContext)
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: ShapeStyleCompanion {
        internal val shared = ForegroundStyle()

        val foreground: ForegroundStyle
            get() = ForegroundStyle.shared
    }
}

@androidx.annotation.Keep
class BackgroundStyle: ShapeStyle, skip.lib.SwiftProjecting {

    constructor() {
    }

    @Composable
    override fun asColor(opacity: Double, animationContext: ComposeContext?): androidx.compose.ui.graphics.Color? {
        val matchtarget_0 = EnvironmentValues.shared.backgroundStyle
        if (matchtarget_0 != null) {
            val style = matchtarget_0
            return style.asColor(opacity = opacity, animationContext = animationContext)
        } else {
            return Color.background.asColor(opacity = opacity, animationContext = null)
        }
    }

    @Composable
    override fun asBrush(opacity: Double, animationContext: ComposeContext?): Brush? {
        val matchtarget_1 = EnvironmentValues.shared.backgroundStyle
        if (matchtarget_1 != null) {
            val style = matchtarget_1
            return style.asBrush(opacity = opacity, animationContext = animationContext)
        } else {
            return Color.background.asBrush(opacity = opacity, animationContext = null)
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: ShapeStyleCompanion {
        internal val shared = BackgroundStyle()

        val background: BackgroundStyle
            get() = BackgroundStyle.shared
    }
}

@androidx.annotation.Keep
class TintShapeStyle: ShapeStyle, skip.lib.SwiftProjecting {

    constructor() {
    }

    @Composable
    override fun asColor(opacity: Double, animationContext: ComposeContext?): androidx.compose.ui.graphics.Color? {
        val matchtarget_2 = EnvironmentValues.shared._tint
        if (matchtarget_2 != null) {
            val style = matchtarget_2
            return style.asColor(opacity = opacity, animationContext = animationContext)
        } else {
            return Color.accentColor.asColor(opacity = opacity, animationContext = null)
        }
    }

    @Composable
    override fun asBrush(opacity: Double, animationContext: ComposeContext?): Brush? {
        val matchtarget_3 = EnvironmentValues.shared._tint
        if (matchtarget_3 != null) {
            val style = matchtarget_3
            return style.asBrush(opacity = opacity, animationContext = animationContext)
        } else {
            return Color.accentColor.asBrush(opacity = opacity, animationContext = null)
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: ShapeStyleCompanion {
        internal val shared = TintShapeStyle()

        val tint: TintShapeStyle
            get() = TintShapeStyle.shared
    }
}

@Suppress("MUST_BE_INITIALIZED")
class FillStyle: MutableStruct {
    var isEOFilled: Boolean
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var isAntialiased: Boolean
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(eoFill: Boolean = false, antialiased: Boolean = true) {
        this.isEOFilled = eoFill
        this.isAntialiased = antialiased
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as FillStyle
        this.isEOFilled = copy.isEOFilled
        this.isAntialiased = copy.isAntialiased
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = FillStyle(this as MutableStruct)

    override fun equals(other: Any?): Boolean {
        if (other !is FillStyle) return false
        return isEOFilled == other.isEOFilled && isAntialiased == other.isAntialiased
    }

    @androidx.annotation.Keep
    companion object {
    }
}

enum class RoundedCornerStyle(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
    circular(0), // For bridging
    continuous(1); // For bridging

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: Int): RoundedCornerStyle? {
            return when (rawValue) {
                0 -> RoundedCornerStyle.circular
                1 -> RoundedCornerStyle.continuous
                else -> null
            }
        }
    }
}

fun RoundedCornerStyle(rawValue: Int): RoundedCornerStyle? = RoundedCornerStyle.init(rawValue = rawValue)

/*
import struct CoreGraphics.CGFloat
import struct CoreGraphics.CGRect

/// No-op
func stubShapeStyle() -> some ShapeStyle {
//return never() // raises warning: “A call to a never-returning function”
struct NeverShapeStyle : ShapeStyle {
typealias Body = Never
var body: Body { fatalError() }
}
return NeverShapeStyle()
}

//@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
//extension AnyShapeStyle.Storage : @unchecked Sendable {
//}


@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension ShapeStyle where Self == ImagePaint {

/// A shape style that fills a shape by repeating a region of an image.
///
/// For information about how to use shape styles, see ``ShapeStyle``.
///
/// - Parameters:
///   - image: The image to be drawn.
///   - sourceRect: A unit-space rectangle defining how much of the source
///     image to draw. The results are undefined if `sourceRect` selects
///     areas outside the `[0, 1]` range in either axis.
///   - scale: A scale factor applied to the image during rendering.
public static func image(_ image: Image, sourceRect: CGRect = CGRect(x: 0, y: 0, width: 1, height: 1), scale: CGFloat = 1) -> ImagePaint { fatalError() }
}

@available(iOS 17.0, macOS 10.15, tvOS 17.0, watchOS 10.0, *)
extension ShapeStyle where Self == SeparatorShapeStyle {

/// A style appropriate for foreground separator or border lines.
///
/// For information about how to use shape styles, see ``ShapeStyle``.
public static var separator: SeparatorShapeStyle { get { fatalError() } }
}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension ShapeStyle {

/// Maps a shape style's unit-space coordinates to the absolute coordinates
/// of a given rectangle.
///
/// Some shape styles have colors or patterns that vary
/// with position based on ``UnitPoint`` coordinates. For example, you
/// can create a ``LinearGradient`` using ``UnitPoint/top`` and
/// ``UnitPoint/bottom`` as the start and end points:
///
///     let gradient = LinearGradient(
///         colors: [.red, .yellow],
///         startPoint: .top,
///         endPoint: .bottom)
///
/// When rendering such styles, SkipUI maps the unit space coordinates to
/// the absolute coordinates of the filled shape. However, you can tell
/// SkipUI to use a different set of coordinates by supplying a rectangle
/// to the `in(_:)` method. Consider two resizable rectangles using the
/// gradient defined above:
///
///     HStack {
///         Rectangle()
///             .fill(gradient)
///         Rectangle()
///             .fill(gradient.in(CGRect(x: 0, y: 0, width: 0, height: 300)))
///     }
///     .onTapGesture { isBig.toggle() }
///     .frame(height: isBig ? 300 : 50)
///     .animation(.easeInOut)
///
/// When `isBig` is true — defined elsewhere as a private ``State``
/// variable — the rectangles look the same, because their heights
/// match that of the modified gradient:
///
/// ![Two identical, tall rectangles, with a gradient that starts red at
/// the top and transitions to yellow at the bottom.](ShapeStyle-in-1)
///
/// When the user toggles `isBig` by tapping the ``HStack``, the
/// rectangles shrink, but the gradients each react in a different way:
///
/// ![Two short rectangles with different coloration. The first has a
/// gradient that transitions top to bottom from full red to full yellow.
/// The second starts as red at the top and then begins to transition
/// to yellow toward the bottom.](ShapeStyle-in-2)
///
/// SkipUI remaps the gradient of the first rectangle to the new frame
/// height, so that you continue to see the full range of colors in a
/// smaller area. For the second rectangle, the modified gradient retains
/// a mapping to the full height, so you instead see only a small part of
/// the overall gradient. Animation helps to visualize the difference.
///
/// - Parameter rect: A rectangle that gives the absolute coordinates over
///   which to map the shape style.
/// - Returns: A new shape style mapped to the coordinates given by `rect`.
public func `in`(_ rect: CGRect) -> some ShapeStyle { stubShapeStyle() }

}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
extension ShapeStyle where Self == LinkShapeStyle {

/// A style appropriate for links.
///
/// For information about how to use shape styles, see ``ShapeStyle``.
public static var link: LinkShapeStyle { get { fatalError() } }
}

/// A style appropriate for links.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
@frozen public struct LinkShapeStyle : ShapeStyle {

/// Creates a new link shape style instance.
public init() { fatalError() }

/// The type of shape style this will resolve to.
///
/// When you create a custom shape style, Swift infers this type
/// from your implementation of the required `resolve` function.
public typealias Resolved = Never
}

@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
extension ShapeStyle {

/// Returns a new style based on `self` that multiplies by the
/// specified opaciopacity when drawing.
public func opacity(_ opacity: Double) -> some ShapeStyle { stubShapeStyle() }

}

@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
extension ShapeStyle where Self == AnyShapeStyle {

/// Returns a new style based on the current style that multiplies
/// by `opacity` when drawing.
///
/// In most contexts the current style is the foreground but e.g.
/// when setting the value of the background style, that becomes
/// the current implicit style.
///
/// For example, a circle filled with the current foreground
/// style at fifty-percent opacity:
///
///     Circle().fill(.opacity(0.5))
///
public static func opacity(_ opacity: Double) -> some ShapeStyle { stubShapeStyle() }

}

@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
extension ShapeStyle where Self == HierarchicalShapeStyle {

/// A shape style that maps to the first level of the current content style.
///
/// This hierarchical style maps to the first level of the current
/// foreground style, or to the first level of the default foreground style
/// if you haven't set a foreground style in the view's environment. You
/// typically set a foreground style by supplying a non-hierarchical style
/// to the ``View/foregroundStyle(_:)`` modifier.
///
/// For information about how to use shape styles, see ``ShapeStyle``.
public static var primary: HierarchicalShapeStyle { get { fatalError() } }

/// A shape style that maps to the second level of the current content style.
///
/// This hierarchical style maps to the second level of the current
/// foreground style, or to the second level of the default foreground style
/// if you haven't set a foreground style in the view's environment. You
/// typically set a foreground style by supplying a non-hierarchical style
/// to the ``View/foregroundStyle(_:)`` modifier.
///
/// For information about how to use shape styles, see ``ShapeStyle``.
public static var secondary: HierarchicalShapeStyle { get { fatalError() } }

/// A shape style that maps to the third level of the current content
/// style.
///
/// This hierarchical style maps to the third level of the current
/// foreground style, or to the third level of the default foreground style
/// if you haven't set a foreground style in the view's environment. You
/// typically set a foreground style by supplying a non-hierarchical style
/// to the ``View/foregroundStyle(_:)`` modifier.
///
/// For information about how to use shape styles, see ``ShapeStyle``.
public static var tertiary: HierarchicalShapeStyle { get { fatalError() } }

/// A shape style that maps to the fourth level of the current content
/// style.
///
/// This hierarchical style maps to the fourth level of the current
/// foreground style, or to the fourth level of the default foreground style
/// if you haven't set a foreground style in the view's environment. You
/// typically set a foreground style by supplying a non-hierarchical style
/// to the ``View/foregroundStyle(_:)`` modifier.
///
/// For information about how to use shape styles, see ``ShapeStyle``.
public static var quaternary: HierarchicalShapeStyle { get { fatalError() } }
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
extension ShapeStyle {

/// Returns the second level of this shape style.
public var secondary: some ShapeStyle { get { stubShapeStyle() } }

/// Returns the third level of this shape style.
public var tertiary: some ShapeStyle { get { stubShapeStyle() } }

/// Returns the fourth level of this shape style.
public var quaternary: some ShapeStyle { get { stubShapeStyle() } }
}

@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
extension ShapeStyle {

/// Returns a new style based on `self` that applies the specified
/// blend mode when drawing.
public func blendMode(_ mode: BlendMode) -> some ShapeStyle { stubShapeStyle() }

}

@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
extension ShapeStyle where Self == AnyShapeStyle {

/// Returns a new style based on the current style that uses
/// `mode` as its blend mode when drawing.
///
/// In most contexts the current style is the foreground but e.g.
/// when setting the value of the background style, that becomes
/// the current implicit style.
///
/// For example, a circle filled with the current foreground
/// style and the overlay blend mode:
///
///     Circle().fill(.blendMode(.overlay))
///
public static func blendMode(_ mode: BlendMode) -> some ShapeStyle { stubShapeStyle() }

}

@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
extension ShapeStyle where Self == AnyShapeStyle {

/// Returns a shape style that applies the specified shadow style to the
/// current style.
///
/// In most contexts the current style is the foreground, but not always.
/// For example, when setting the value of the background style, that
/// becomes the current implicit style.
///
/// The following example creates a circle filled with the current
/// foreground style that uses an inner shadow:
///
///     Circle().fill(.shadow(.inner(radius: 1, y: 1)))
///
/// - Parameter style: The shadow style to apply.
///
/// - Returns: A new shape style based on the current style that uses the
///   specified shadow style.
public static func shadow(_ style: ShadowStyle) -> some ShapeStyle { stubShapeStyle() }

}

@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
extension ShapeStyle {

/// Applies the specified shadow effect to the shape style.
///
/// For example, you can create a rectangle that adds a drop shadow to
/// the ``ShapeStyle/red`` shape style.
///
///     Rectangle().fill(.red.shadow(.drop(radius: 2, y: 3)))
///
/// - Parameter style: The shadow style to apply.
///
/// - Returns: A new shape style that uses the specified shadow style.
public func shadow(_ style: ShadowStyle) -> some ShapeStyle { stubShapeStyle() }

}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
extension ShapeStyle where Self == PlaceholderTextShapeStyle {

/// A style appropriate for placeholder text.
///
/// For information about how to use shape styles, see ``ShapeStyle``.
public static var placeholder: PlaceholderTextShapeStyle { get { fatalError() } }
}

//@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
//extension ShapeStyle where Self.Resolved == Never {
//
//    /// Evaluate to a resolved shape style given the current `environment`.
//    public func resolve(in environment: EnvironmentValues) -> Never { fatalError() }
//}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
extension ShapeStyle where Self == FillShapeStyle {

/// An overlay fill style for filling shapes.
///
/// This shape style is appropriate for items situated on top of an existing
/// background color. It incorporates transparency to allow the background
/// color to show through.
///
/// Use the primary version of this style to fill thin or small shapes, such
/// as the track of a slider on iOS.
/// Use the secondary version of this style to fill medium-size shapes, such
/// as the background of a switch on iOS.
/// Use the tertiary version of this style to fill large shapes, such as
/// input fields, search bars, or buttons on iOS.
/// Use the quaternary version of this style to fill large areas that
/// contain complex content, such as an expanded table cell on iOS.
public static var fill: FillShapeStyle { get { fatalError() } }
}

@available(iOS 15.0, macOS 12.0, *)
@available(tvOS, unavailable)
@available(watchOS, unavailable)
extension ShapeStyle where Self == SelectionShapeStyle {

/// A style used to visually indicate selection following platform conventional
/// colors and behaviors.
///
/// For example:
///
///     ForEach(items) {
///        ItemView(value: item, isSelected: item.id == selectedID)
///     }
///
///     struct ItemView {
///         var value: item
///         var isSelected: Bool
///
///         var body: some View {
///             // construct the actual cell content
///                 .background(selectionBackground)
///         }
///         @ViewBuilder
///         private var selectionBackground: some View {
///             if isSelected {
///                 RoundedRectangle(cornerRadius: 8)
///                     .fill(.selection)
///             }
///         }
///     }
///
/// On macOS and iPadOS this automatically reflects window key state and focus
/// state, where the emphasized appearance will be used only when the window is
/// key and the nearest focusable element is actually focused. On iPhone, this
/// will always fill with the environment's accent color.
///
/// For information about how to use shape styles, see ``ShapeStyle``.
public static var selection: SelectionShapeStyle { get { fatalError() } }
}

//@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
//extension ShapeStyle where Self : View, Self.Body == _ShapeView<Rectangle, Self> {
//
//    /// A rectangular view that's filled with the shape style.
//    ///
//    /// For a ``ShapeStyle`` that also conforms to the ``View`` protocol, like
//    /// ``Color`` or ``LinearGradient``, this default implementation of the
//    /// ``View/body-swift.property`` property provides a visual representation
//    /// for the shape style. As a result, you can use the shape style in a view
//    /// hierarchy like any other view:
//    ///
//    ///     ZStack {
//    ///         Color.cyan
//    ///         Text("Hello!")
//    ///     }
//    ///     .frame(width: 200, height: 50)
//    ///
//    /// ![A screenshot of a cyan rectangle with the text hello appearing
//    /// in the middle of the rectangle.](ShapeStyle-body-1)
//    public var body: _ShapeView<Rectangle, Self> { get { fatalError() } }
//}

@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
extension ShapeStyle where Self == LinearGradient {

/// A linear gradient.
///
/// The gradient applies the color function along an axis, as
/// defined by its start and end points. The gradient maps the unit
/// space points into the bounding rectangle of each shape filled
/// with the gradient.
///
/// For example, a linear gradient used as a background:
///
///     ContentView()
///         .background(.linearGradient(.red.gradient,
///             startPoint: .top, endPoint: .bottom))
///
/// For information about how to use shape styles, see ``ShapeStyle``.
public static func linearGradient(_ gradient: AnyGradient, startPoint: UnitPoint, endPoint: UnitPoint) -> some ShapeStyle { stubShapeStyle() }

}

@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
extension ShapeStyle where Self == RadialGradient {

/// A radial gradient.
///
/// The gradient applies the color function as the distance from a
/// center point, scaled to fit within the defined start and end
/// radii. The gradient maps the unit space center point into the
/// bounding rectangle of each shape filled with the gradient.
///
/// For example, a radial gradient used as a background:
///
///     ContentView()
///         .background(.radialGradient(.red.gradient, endRadius: 100))
///
/// For information about how to use shape styles, see ``ShapeStyle``.
public static func radialGradient(_ gradient: AnyGradient, center: UnitPoint = .center, startRadius: CGFloat = 0, endRadius: CGFloat) -> some ShapeStyle { stubShapeStyle() }

}

@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
extension ShapeStyle where Self == EllipticalGradient {

/// A radial gradient that draws an ellipse.
///
/// The gradient maps its coordinate space to the unit space square
/// in which its center and radii are defined, then stretches that
/// square to fill its bounding rect, possibly also stretching the
/// circular gradient to have elliptical contours.
///
/// For example, an elliptical gradient used as a background:
///
///     ContentView()
///         .background(.ellipticalGradient(.red.gradient))
///
/// For information about how to use shape styles, see ``ShapeStyle``.
public static func ellipticalGradient(_ gradient: AnyGradient, center: UnitPoint = .center, startRadiusFraction: CGFloat = 0, endRadiusFraction: CGFloat = 0.5) -> some ShapeStyle { stubShapeStyle() }

}

@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
extension ShapeStyle where Self == AngularGradient {

/// An angular gradient, which applies the color function as the
/// angle changes between the start and end angles, and anchored to
/// a relative center point within the filled shape.
///
/// An angular gradient is also known as a "conic" gradient. If
/// `endAngle - startAngle > 2π`, the gradient only draws the last complete
/// turn. If `endAngle - startAngle < 2π`, the gradient fills the missing
/// area with the colors defined by gradient stop locations at `0` and `1`,
/// transitioning between the two halfway across the missing area.
///
/// For example, an angular gradient used as a background:
///
///     ContentView()
///         .background(.angularGradient(.red.gradient))
///
/// For information about how to use shape styles, see ``ShapeStyle``.
///
/// - Parameters:
///   - gradient: The gradient to use for filling the shape, providing the
///     colors and their relative stop locations.
///   - center: The relative center of the gradient, mapped from the unit
///     space into the bounding rectangle of the filled shape.
///   - startAngle: The angle that marks the beginning of the gradient.
///   - endAngle: The angle that marks the end of the gradient.
public static func angularGradient(_ gradient: AnyGradient, center: UnitPoint = .center, startAngle: Angle, endAngle: Angle) -> some ShapeStyle { stubShapeStyle() }


/// A conic gradient that completes a full turn, optionally starting from
/// a given angle and anchored to a relative center point within the filled
/// shape.
///
/// For example, a conic gradient used as a background:
///
///     let gradient = Gradient(colors: [.red, .yellow])
///
///     ContentView()
///         .background(.conicGradient(gradient))
///
/// For information about how to use shape styles, see ``ShapeStyle``.
///
/// - Parameters:
///   - gradient: The gradient to use for filling the shape, providing the
///     colors and their relative stop locations.
///   - center: The relative center of the gradient, mapped from the unit
///     space into the bounding rectangle of the filled shape.
///   - angle: The angle to offset the beginning of the gradient's full
///     turn.
public static func conicGradient(_ gradient: AnyGradient, center: UnitPoint = .center, angle: Angle = .zero) -> some ShapeStyle { stubShapeStyle() }

}

@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 10.0, *)
extension ShapeStyle where Self == Material {

/// A material that's somewhat translucent.
public static var regularMaterial: Material { get { fatalError() } }

/// A material that's more opaque than translucent.
public static var thickMaterial: Material { get { fatalError() } }

/// A material that's more translucent than opaque.
public static var thinMaterial: Material { get { fatalError() } }

/// A mostly translucent material.
public static var ultraThinMaterial: Material { get { fatalError() } }

/// A mostly opaque material.
public static var ultraThickMaterial: Material { get { fatalError() } }
}

@available(iOS 15.0, macOS 12.0, *)
@available(tvOS, unavailable)
@available(watchOS, unavailable)
extension ShapeStyle where Self == Material {

/// A material matching the style of system toolbars.
public static var bar: Material { get { fatalError() } }
}

/// A style used to visually indicate selection following platform conventional
/// colors and behaviors.
///
/// You can also use ``ShapeStyle/selection`` to construct this style.
@available(iOS 15.0, macOS 10.15, *)
@available(tvOS, unavailable)
@available(watchOS, unavailable)
public struct SelectionShapeStyle : ShapeStyle {

/// Creates a selection shape style.
@available(macOS 12.0, *)
public init() { fatalError() }

/// The type of shape style this will resolve to.
///
/// When you create a custom shape style, Swift infers this type
/// from your implementation of the required `resolve` function.
public typealias Resolved = Never
}

/// A style appropriate for foreground separator or border lines.
///
/// You can also use ``ShapeStyle/separator`` to construct this style.
@available(iOS 17.0, macOS 10.15, tvOS 17.0, watchOS 10.0, *)
public struct SeparatorShapeStyle : ShapeStyle {

/// Creates a new separator shape style instance.
public init() { fatalError() }

/// The type of shape style this will resolve to.
///
/// When you create a custom shape style, Swift infers this type
/// from your implementation of the required `resolve` function.
public typealias Resolved = Never
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, *)
@available(watchOS, unavailable)
extension Shader : ShapeStyle {
}

/// A shape style that displays one of the overlay fills.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
@frozen public struct FillShapeStyle : ShapeStyle {

/// An overlay fill style for filling shapes.
///
/// This shape style is appropriate for items situated on top of an existing
/// background color. It incorporates transparency to allow the background
/// color to show through.
///
/// Use the primary version of this style to fill thin or small shapes, such
/// as the track of a slider.
/// Use the secondary version of this style to fill medium-size shapes, such
/// as the background of a switch.
/// Use the tertiary version of this style to fill large shapes, such as
/// input fields, search bars, or buttons.
/// Use the quaternary version of this style to fill large areas that
/// contain complex content, such as an expanded table cell.
public init() { fatalError() }

/// The type of shape style this will resolve to.
///
/// When you create a custom shape style, Swift infers this type
/// from your implementation of the required `resolve` function.
public typealias Resolved = Never
}

/// A shape provider that fills its shape.
///
/// You do not create this type directly, it is the return type of `Shape.fill`.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
@frozen public struct FillShapeView<Content, Style, Background> : ShapeView where Content : Shape, Style : ShapeStyle, Background : View {

/// The shape that this type draws and provides for other drawing
/// operations.
public var shape: Content { get { fatalError() } }

/// The style that fills this view's shape.
public var style: Style { get { fatalError() } }

/// The fill style used when filling this view's shape.
public var fillStyle: FillStyle { get { fatalError() } }

/// The background shown beneath this view.
public var background: Background { get { fatalError() } }

/// Create a FillShapeView.
public init(shape: Content, style: Style, fillStyle: FillStyle, background: Background) { fatalError() }

public typealias Body = NeverView
public var body: Body { fatalError() }
}


/// A shape style that maps to one of the numbered content styles.
@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
@frozen public struct HierarchicalShapeStyle : ShapeStyle {

/// A shape style that maps to the first level of the current
/// content style.
public static let primary: HierarchicalShapeStyle = { fatalError() }()

/// A shape style that maps to the second level of the current
/// content style.
public static let secondary: HierarchicalShapeStyle = { fatalError() }()

/// A shape style that maps to the third level of the current
/// content style.
public static let tertiary: HierarchicalShapeStyle = { fatalError() }()

/// A shape style that maps to the fourth level of the current
/// content style.
public static let quaternary: HierarchicalShapeStyle = { fatalError() }()

/// The type of shape style this will resolve to.
///
/// When you create a custom shape style, Swift infers this type
/// from your implementation of the required `resolve` function.
public typealias Resolved = Never
}

/// Styles that you can apply to hierarchical shapes.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
@frozen public struct HierarchicalShapeStyleModifier<Base> : ShapeStyle where Base : ShapeStyle {

/// The type of shape style this will resolve to.
///
/// When you create a custom shape style, Swift infers this type
/// from your implementation of the required `resolve` function.
public typealias Resolved = Never
}

@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
extension Material : ShapeStyle {

/// The type of shape style this will resolve to.
///
/// When you create a custom shape style, Swift infers this type
/// from your implementation of the required `resolve` function.
public typealias Resolved = Never
}

/// A style appropriate for placeholder text.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
@frozen public struct PlaceholderTextShapeStyle : ShapeStyle {

/// Creates a new placeholder text shape style.
public init() { fatalError() }

/// The type of shape style this will resolve to.
///
/// When you create a custom shape style, Swift infers this type
/// from your implementation of the required `resolve` function.
public typealias Resolved = Never
}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension Never : ShapeStyle {

/// The type of shape style this will resolve to.
///
/// When you create a custom shape style, Swift infers this type
/// from your implementation of the required `resolve` function.
public typealias Resolved = Never
}
*/
