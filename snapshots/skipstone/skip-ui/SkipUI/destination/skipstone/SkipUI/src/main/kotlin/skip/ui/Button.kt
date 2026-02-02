package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@androidx.annotation.Keep
class Button: View, Renderable, skip.lib.SwiftProjecting {
    internal val action: () -> Unit
    internal val label: ComposeBuilder
    internal val role: ButtonRole?

    constructor(action: () -> Unit, label: () -> View): this(role = null, action = action, label = label) {
    }

    constructor(title: String, action: () -> Unit): this(action = action, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(verbatim = title).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleKey: LocalizedStringKey, action: () -> Unit): this(action = action, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleKey).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleResource: LocalizedStringResource, action: () -> Unit): this(action = action, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleResource).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(title: String, systemImage: String, role: ButtonRole? = null, action: () -> Unit): this(role = role, action = action, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Label(title, systemImage = systemImage).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleKey: LocalizedStringKey, systemImage: String, role: ButtonRole? = null, action: () -> Unit): this(role = role, action = action, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Label(titleKey, systemImage = systemImage).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleResource: LocalizedStringResource, systemImage: String, role: ButtonRole? = null, action: () -> Unit): this(role = role, action = action, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Label(titleResource, systemImage = systemImage).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(role: ButtonRole?, action: () -> Unit, label: () -> View) {
        this.role = role
        this.action = action
        this.label = ComposeBuilder.from(label)
    }

    constructor(title: String, role: ButtonRole?, action: () -> Unit): this(role = role, action = action, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(verbatim = title).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleKey: LocalizedStringKey, role: ButtonRole?, action: () -> Unit): this(role = role, action = action, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleKey).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleResource: LocalizedStringResource, role: ButtonRole?, action: () -> Unit): this(role = role, action = action, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleResource).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(role: ButtonRole, action: () -> Unit): this(role = role, action = action, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Companion.defaultLabel(for_ = role).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(bridgedRole: Int?, action: () -> Unit, bridgedLabel: View?) {
        this.role = if (bridgedRole == null) null else ButtonRole(rawValue = bridgedRole!!)
        this.action = action
        if (bridgedLabel != null) {
            this.label = ComposeBuilder.from { -> bridgedLabel }
        } else if (role != null) {
            this.label = ComposeBuilder.from { -> Companion.defaultLabel(for_ = role) }
        } else {
            this.label = ComposeBuilder.from { -> EmptyView() }
        }
    }

    @Composable
    override fun Render(context: ComposeContext): Unit = Companion.RenderButton(label = label, context = context, role = role, action = action)

    @Composable
    override fun shouldRenderListItem(context: ComposeContext): Tuple2<Boolean, (() -> Unit)?> {
        val buttonStyle = EnvironmentValues.shared._buttonStyle
        if (buttonStyle != null && buttonStyle != ButtonStyle.automatic && buttonStyle != ButtonStyle.plain) {
            return Tuple2(false, null)
        }
        return Tuple2(true, action)
    }

    @Composable
    override fun RenderListItem(context: ComposeContext, modifiers: kotlin.collections.List<ModifierProtocol>) {
        ModifiedContent.RenderWithModifiers(modifiers, context = context) { context ->
            val style = EnvironmentValues.shared._buttonStyle
            Companion.RenderTextButton(label = label, context = context, isPlain = style == ButtonStyle.plain, role = role)
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {

        private fun defaultLabel(for_: ButtonRole): View {
            val role = for_
            when (role) {
                ButtonRole.cancel -> return Text(LocalizedStringKey(stringLiteral = "Cancel"))
                ButtonRole.destructive -> return Text(LocalizedStringKey(stringLiteral = "Delete"))
                ButtonRole.confirm -> return skip.ui.Label(LocalizedStringKey(stringLiteral = "OK"), systemImage = "checkmark")
                ButtonRole.close -> return skip.ui.Label(LocalizedStringKey(stringLiteral = "Close"), systemImage = "xmark")
            }
        }

        /// Render a button in the current style.
        @Composable
        internal fun RenderButton(label: View, context: ComposeContext, role: ButtonRole? = null, isEnabled: Boolean = EnvironmentValues.shared.isEnabled, action: () -> Unit) {
            val buttonStyle = EnvironmentValues.shared._buttonStyle
            ComposeContainer(modifier = context.modifier) { modifier ->
                when (buttonStyle) {
                    ButtonStyle.bordered -> {
                        val tint = (if (role == ButtonRole.destructive) Color.red else EnvironmentValues.shared._tint).sref()
                        val colors: ButtonColors
                        if (tint != null) {
                            val tintColor = tint.colorImpl()
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = tintColor.copy(alpha = 0.15f), contentColor = tintColor, disabledContainerColor = tintColor.copy(alpha = 0.15f), disabledContentColor = tintColor.copy(alpha = ContentAlpha.medium))
                        } else {
                            colors = ButtonDefaults.filledTonalButtonColors()
                        }
                        var options = Material3ButtonOptions(onClick = action, modifier = modifier, enabled = isEnabled, shape = ButtonDefaults.filledTonalShape, colors = colors, elevation = ButtonDefaults.filledTonalButtonElevation())
                        EnvironmentValues.shared._material3Button?.let { updateOptions ->
                            options = updateOptions(options)
                        }
                        val placement = EnvironmentValues.shared._placement.sref()
                        val contentContext = context.content()
                        EnvironmentValues.shared.setValues(l@{ it ->
                            if (tint != null) {
                                val foregroundStyle = (if (isEnabled) tint else tint.opacity(Double(ContentAlpha.disabled))).sref()
                                it.set_foregroundStyle(foregroundStyle)
                            } else {
                                it.set_placement(placement.union(ViewPlacement.systemTextColor))
                            }
                            return@l ComposeResult.ok
                        }, in_ = { ->
                            FilledTonalButton(onClick = options.onClick, modifier = options.modifier, enabled = options.enabled, shape = options.shape, colors = options.colors, elevation = options.elevation, border = options.border, contentPadding = options.contentPadding, interactionSource = options.interactionSource) { -> label.Compose(context = contentContext) }
                        })
                    }
                    ButtonStyle.borderedProminent -> {
                        val tint = (if (role == ButtonRole.destructive) Color.red else EnvironmentValues.shared._tint).sref()
                        val colors: ButtonColors
                        if (tint != null) {
                            val tintColor = tint.colorImpl()
                            colors = ButtonDefaults.buttonColors(containerColor = tintColor, disabledContainerColor = tintColor.copy(alpha = ContentAlpha.disabled))
                        } else {
                            colors = ButtonDefaults.buttonColors()
                        }
                        var options = Material3ButtonOptions(onClick = action, modifier = modifier, enabled = isEnabled, shape = ButtonDefaults.shape, colors = colors, elevation = ButtonDefaults.buttonElevation())
                        EnvironmentValues.shared._material3Button?.let { updateOptions ->
                            options = updateOptions(options)
                        }
                        val placement = EnvironmentValues.shared._placement.sref()
                        val contentContext = context.content()
                        EnvironmentValues.shared.setValues(l@{ it ->
                            it.set_placement(placement.union(ViewPlacement.systemTextColor).union(ViewPlacement.onPrimaryColor))
                            return@l ComposeResult.ok
                        }, in_ = { ->
                            androidx.compose.material3.Button(onClick = options.onClick, modifier = options.modifier, enabled = options.enabled, shape = options.shape, colors = options.colors, elevation = options.elevation, border = options.border, contentPadding = options.contentPadding, interactionSource = options.interactionSource) { -> label.Compose(context = contentContext) }
                        })
                    }
                    ButtonStyle.plain -> RenderTextButton(label = label, context = context.content(modifier = modifier), role = role, isPlain = true, isEnabled = isEnabled, action = action)
                    else -> RenderTextButton(label = label, context = context.content(modifier = modifier), role = role, isEnabled = isEnabled, action = action)
                }
            }
        }

        /// Render a plain-style button.
        ///
        /// - Parameters:
        ///   - action: Pass nil if the given modifier already includes `clickable`
        @Composable
        internal fun RenderTextButton(label: View, context: ComposeContext, role: ButtonRole? = null, isPlain: Boolean = false, isEnabled: Boolean = EnvironmentValues.shared.isEnabled, action: (() -> Unit)? = null) {
            var foregroundStyle: ShapeStyle
            if (role == ButtonRole.destructive) {
                foregroundStyle = Color.red.sref()
            } else {
                foregroundStyle = (EnvironmentValues.shared._foregroundStyle ?: (if (isPlain) Color.primary else (EnvironmentValues.shared._tint ?: Color.accentColor))).sref()
            }
            if (!isEnabled) {
                val disabledAlpha = Double(ContentAlpha.disabled)
                foregroundStyle = AnyShapeStyle(foregroundStyle, opacity = disabledAlpha)
            }

            var modifier = context.modifier
            if (action != null) {
                modifier = modifier.clickable(onClick = action, enabled = isEnabled)
            }
            val contentContext = context.content(modifier = modifier)

            EnvironmentValues.shared.setValues(l@{ it ->
                it.set_foregroundStyle(foregroundStyle)
                return@l ComposeResult.ok
            }, in_ = { -> label.Compose(context = contentContext) })
        }
    }
}

class ButtonStyle: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ButtonStyle) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = ButtonStyle(rawValue = 0) // For bridging
        val plain = ButtonStyle(rawValue = 1) // For bridging
        val borderless = ButtonStyle(rawValue = 2) // For bridging
        val bordered = ButtonStyle(rawValue = 3) // For bridging
        val borderedProminent = ButtonStyle(rawValue = 4) // For bridging
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val glass = ButtonStyle(rawValue = 5) // For bridging
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val glassProminent = ButtonStyle(rawValue = 6) // For bridging
    }
}

enum class ButtonRepeatBehavior {
    automatic,
    enabled,
    disabled;

    @androidx.annotation.Keep
    companion object {
    }
}

enum class ButtonRole(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
    destructive(1), // For bridging
    cancel(2), // For bridging
    confirm(3), // For bridging
    close(4); // For bridging

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: Int): ButtonRole? {
            return when (rawValue) {
                1 -> ButtonRole.destructive
                2 -> ButtonRole.cancel
                3 -> ButtonRole.confirm
                4 -> ButtonRole.close
                else -> null
            }
        }
    }
}

fun ButtonRole(rawValue: Int): ButtonRole? = ButtonRole.init(rawValue = rawValue)

class ButtonSizing: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ButtonSizing) return false
        return rawValue == other.rawValue
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, rawValue)
        return result
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = ButtonSizing(rawValue = 0) // For bridging
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val flexible = ButtonSizing(rawValue = 1) // For bridging
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val fitted = ButtonSizing(rawValue = 2) // For bridging
    }
}

internal class ButtonStyleModifier: EnvironmentModifier {
    internal val style: ButtonStyle

    internal constructor(style: ButtonStyle): super() {
        this.style = style
        this.action = l@{ environment ->
            environment.set_buttonStyle(style)
            return@l ComposeResult.ok
        }
    }

    @Composable
    override fun shouldRenderListItem(content: Renderable, context: ComposeContext): Tuple2<Boolean, (() -> Unit)?> {
        // The button style matters when deciding whether to render buttons and navigation links as list items
        return EnvironmentValues.shared.setValuesWithReturn(action!!, in_ = l@{ -> return@l content.shouldRenderListItem(context = context) })
    }
}

@Suppress("MUST_BE_INITIALIZED")
class Material3ButtonOptions: MutableStruct {
    var onClick: () -> Unit
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var modifier: Modifier
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var enabled: Boolean
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var shape: androidx.compose.ui.graphics.Shape
        get() = field.sref({ this.shape = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var colors: ButtonColors
        get() = field.sref({ this.colors = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var elevation: ButtonElevation? = null
        get() = field.sref({ this.elevation = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var border: BorderStroke? = null
        get() = field.sref({ this.border = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var contentPadding: PaddingValues
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var interactionSource: MutableInteractionSource? = null
        get() = field.sref({ this.interactionSource = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    fun copy(onClick: () -> Unit = this.onClick, modifier: Modifier = this.modifier, enabled: Boolean = this.enabled, shape: androidx.compose.ui.graphics.Shape = this.shape, colors: ButtonColors = this.colors, elevation: ButtonElevation? = this.elevation, border: BorderStroke? = this.border, contentPadding: PaddingValues = this.contentPadding, interactionSource: MutableInteractionSource? = this.interactionSource): Material3ButtonOptions = Material3ButtonOptions(onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, colors = colors, elevation = elevation, border = border, contentPadding = contentPadding, interactionSource = interactionSource)

    constructor(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: androidx.compose.ui.graphics.Shape, colors: ButtonColors, elevation: ButtonElevation? = null, border: BorderStroke? = null, contentPadding: PaddingValues = ButtonDefaults.ContentPadding, interactionSource: MutableInteractionSource? = null) {
        this.onClick = onClick
        this.modifier = modifier
        this.enabled = enabled
        this.shape = shape
        this.colors = colors
        this.elevation = elevation
        this.border = border
        this.contentPadding = contentPadding
        this.interactionSource = interactionSource
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = Material3ButtonOptions(onClick, modifier, enabled, shape, colors, elevation, border, contentPadding, interactionSource)

    @androidx.annotation.Keep
    companion object {
    }
}

@Suppress("MUST_BE_INITIALIZED")
class Material3RippleOptions: MutableStruct {
    var color: androidx.compose.ui.graphics.Color
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var rippleAlpha: RippleAlpha? = null
        get() = field.sref({ this.rippleAlpha = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified, rippleAlpha: RippleAlpha? = null) {
        this.color = color
        this.rippleAlpha = rippleAlpha
    }

    internal constructor(configuration: RippleConfiguration) {
        this.color = configuration.color
        this.rippleAlpha = configuration.rippleAlpha
    }

    fun copy(color: androidx.compose.ui.graphics.Color = this.color, rippleAlpha: RippleAlpha? = this.rippleAlpha): Material3RippleOptions = Material3RippleOptions(color = color, rippleAlpha = rippleAlpha)

    internal fun asConfiguration(): RippleConfiguration = RippleConfiguration(color = color, rippleAlpha = rippleAlpha)

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as Material3RippleOptions
        this.color = copy.color
        this.rippleAlpha = copy.rippleAlpha
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = Material3RippleOptions(this as MutableStruct)

    @androidx.annotation.Keep
    companion object {
    }
}

/*
//@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
//extension Button where Label == PrimitiveButtonStyleConfiguration.Label {

/// Creates a button based on a configuration for a style with a custom
/// appearance and custom interaction behavior.
///
/// Use this initializer within the
/// ``PrimitiveButtonStyle/makeBody(configuration:)`` method of a
/// ``PrimitiveButtonStyle`` to create an instance of the button that you
/// want to style. This is useful for custom button styles that modify the
/// current button style, rather than implementing a brand new style.
///
/// For example, the following style adds a red border around the button,
/// but otherwise preserves the button's current style:
///
///     struct RedBorderedButtonStyle: PrimitiveButtonStyle {
///         func makeBody(configuration: Configuration) -> some View {
///             Button(configuration)
///                 .border(Color.red)
///         }
///     }
///
/// - Parameter configuration: A configuration for a style with a custom
///   appearance and custom interaction behavior.
//    @available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
//    public init(_ configuration: PrimitiveButtonStyleConfiguration) { fatalError() }
//}

/// A shape that is used to draw a button's border.
@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
public struct ButtonBorderShape : Equatable, Sendable {

/// A shape that defers to the system to determine an appropriate shape
/// for the given context and platform.
public static let automatic: ButtonBorderShape = { fatalError() }()

/// A capsule shape.
///
/// - Note: This has no effect on non-widget system buttons on macOS.
@available(macOS 14.0, tvOS 17.0, *)
public static let capsule: ButtonBorderShape = { fatalError() }()

/// A rounded rectangle shape.
public static let roundedRectangle: ButtonBorderShape = { fatalError() }()

/// A rounded rectangle shape.
///
/// - Parameter radius: the corner radius of the rectangle.
/// - Note: This has no effect on non-widget system buttons on macOS.
@available(macOS 14.0, tvOS 17.0, *)
public static func roundedRectangle(radius: CGFloat) -> ButtonBorderShape { fatalError() }

@available(iOS 17.0, macOS 14.0, tvOS 16.4, watchOS 10.0, *)
public static let circle: ButtonBorderShape = { fatalError() }()
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
extension ButtonBorderShape : InsettableShape {

/// Returns `self` inset by `amount`.
public func inset(by amount: CGFloat) -> InsetShape { fatalError() }


/// The type of the inset shape.
public typealias InsetShape = Never
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
extension ButtonBorderShape : Shape {

/// Describes this shape as a path within a rectangular frame of reference.
///
/// - Parameter rect: The frame of reference for describing this shape.
///
/// - Returns: A path that describes this shape.
public func path(in rect: CGRect) -> Path { fatalError() }

/// The type defining the data to animate.
public typealias AnimatableData = EmptyAnimatableData
public var animatableData: AnimatableData { get { fatalError() } set { } }

public typealias Body = NeverView
public var body: Body { fatalError() }
}

/// The properties of a button.
@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
public struct ButtonStyleConfiguration {

/// A type-erased label of a button.
public struct Label : View {

/// The type of view representing the body of this view.
///
/// When you create a custom view, Swift infers this type from your
/// implementation of the required ``View/body-swift.property`` property.
public typealias Body = NeverView
public var body: Body { fatalError() }
}

/// An optional semantic role that describes the button's purpose.
///
/// A value of `nil` means that the Button doesn't have an assigned role. If
/// the button does have a role, use it to make adjustments to the button's
/// appearance. The following example shows a custom style that uses
/// bold text when the role is ``ButtonRole/cancel``,
/// ``ShapeStyle/red`` text when the role is ``ButtonRole/destructive``,
/// and adds no special styling otherwise:
///
///     struct MyButtonStyle: ButtonStyle {
///         func makeBody(configuration: Configuration) -> some View {
///             configuration.label
///                 .font(
///                     configuration.role == .cancel ? .title2.bold() : .title2)
///                 .foregroundColor(
///                     configuration.role == .destructive ? Color.red : nil)
///         }
///     }
///
/// You can create one of each button using this style to see the effect:
///
///     VStack(spacing: 20) {
///         Button("Cancel", role: .cancel) {}
///         Button("Delete", role: .destructive) {}
///         Button("Continue") {}
///     }
///     .buttonStyle(MyButtonStyle())
///
/// ![A screenshot of three buttons stacked vertically. The first says
/// Cancel in black, bold letters. The second says Delete in red, regular
/// weight letters. The third says Continue in black, regular weight
/// letters.](ButtonStyleConfiguration-role-1)
@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
public let role: ButtonRole?

/// A view that describes the effect of pressing the button.
public let label: ButtonStyleConfiguration.Label = { fatalError() }()

/// A Boolean that indicates whether the user is currently pressing the
/// button.
public let isPressed: Bool = { fatalError() }()
}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
public struct PrimitiveButtonStyleConfiguration {

/// A type-erased label of a button.
public struct Label : View {

/// The type of view representing the body of this view.
///
/// When you create a custom view, Swift infers this type from your
/// implementation of the required ``View/body-swift.property`` property.
public typealias Body = NeverView
public var body: Body { fatalError() }
}

/// An optional semantic role describing the button's purpose.
///
/// A value of `nil` means that the Button has no assigned role. If the
/// button does have a role, use it to make adjustments to the button's
/// appearance. The following example shows a custom style that uses
/// bold text when the role is ``ButtonRole/cancel``,
/// ``ShapeStyle/red`` text when the role is ``ButtonRole/destructive``,
/// and adds no special styling otherwise:
///
///     struct MyButtonStyle: PrimitiveButtonStyle {
///         func makeBody(configuration: Configuration) -> some View {
///             configuration.label
///                 .onTapGesture {
///                     configuration.trigger()
///                 }
///                 .font(
///                     configuration.role == .cancel ? .title2.bold() : .title2)
///                 .foregroundColor(
///                     configuration.role == .destructive ? Color.red : nil)
///         }
///     }
///
/// You can create one of each button using this style to see the effect:
///
///     VStack(spacing: 20) {
///         Button("Cancel", role: .cancel) {}
///         Button("Delete", role: .destructive) {}
///         Button("Continue") {}
///     }
///     .buttonStyle(MyButtonStyle())
///
/// ![A screenshot of three buttons stacked vertically. The first says
/// Cancel in black, bold letters. The second says Delete in red, regular
/// weight letters. The third says Continue in black, regular weight
/// letters.](PrimitiveButtonStyleConfiguration-role-1)
@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
public let role: ButtonRole?

/// A view that describes the effect of calling the button's action.
public let label: PrimitiveButtonStyleConfiguration.Label = { fatalError() }()

/// Performs the button's action.
public func trigger() { fatalError() }
}
*/
