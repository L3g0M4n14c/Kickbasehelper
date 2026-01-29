package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@androidx.annotation.Keep
class Toggle: View, Renderable, skip.lib.SwiftProjecting {
    internal val isOn: Binding<Boolean>
    internal val label: View

    constructor(isOn: Binding<Boolean>, label: () -> View) {
        this.isOn = isOn.sref()
        this.label = label()
    }

    constructor(getIsOn: () -> Boolean, setIsOn: (Boolean) -> Unit, bridgedLabel: View) {
        this.isOn = Binding(get = getIsOn, set = setIsOn)
        this.label = ComposeBuilder.from { -> bridgedLabel }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(sources: Any, isOn: (Any) -> Binding<Boolean>, label: () -> View): this(isOn = isOn(0), label = label) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(titleKey: LocalizedStringKey, sources: Any, isOn: (Any) -> Binding<Boolean>): this(isOn = isOn(0), label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleKey).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(titleResource: LocalizedStringResource, sources: Any, isOn: (Any) -> Binding<Boolean>): this(isOn = isOn(0), label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleResource).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(title: String, sources: Any, isOn: (Any) -> Binding<Boolean>): this(isOn = isOn(0), label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(verbatim = title).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleKey: LocalizedStringKey, isOn: Binding<Boolean>): this(isOn = isOn, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleKey).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleResource: LocalizedStringResource, isOn: Binding<Boolean>): this(isOn = isOn, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleResource).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(title: String, isOn: Binding<Boolean>): this(isOn = isOn, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(verbatim = title).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val colors: SwitchColors
        val matchtarget_0 = EnvironmentValues.shared._tint
        if (matchtarget_0 != null) {
            val tint = matchtarget_0
            val tintColor = tint.colorImpl()
            colors = SwitchDefaults.colors(checkedTrackColor = tintColor, disabledCheckedTrackColor = tintColor.copy(alpha = ContentAlpha.disabled))
        } else {
            colors = SwitchDefaults.colors()
        }
        if (EnvironmentValues.shared._labelsHidden) {
            PaddingLayout(padding = EdgeInsets(top = -6.0, leading = 0.0, bottom = -6.0, trailing = 0.0), context = context) { context ->
                Switch(modifier = context.modifier, checked = isOn.wrappedValue, onCheckedChange = { it -> isOn.wrappedValue = it }, enabled = EnvironmentValues.shared.isEnabled, colors = colors)
            }
        } else {
            val contentContext = context.content()
            ComposeContainer(modifier = context.modifier, fillWidth = true) { modifier ->
                Row(modifier = modifier, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { ->
                    Box(modifier = Modifier.weight(1.0f)) { -> label.Compose(context = contentContext) }
                    PaddingLayout(padding = EdgeInsets(top = -6.0, leading = 0.0, bottom = -6.0, trailing = 0.0), context = context) { context ->
                        Switch(checked = isOn.wrappedValue, onCheckedChange = { it -> isOn.wrappedValue = it }, enabled = EnvironmentValues.shared.isEnabled, colors = colors)
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

class ToggleStyle: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ToggleStyle) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = ButtonStyle(rawValue = 0)

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val button = ButtonStyle(rawValue = 1)

        val switch = ButtonStyle(rawValue = 2)
    }
}

/*
//@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
//extension Toggle where Label == ToggleStyleConfiguration.Label {

/// Creates a toggle based on a toggle style configuration.
///
/// You can use this initializer within the
/// ``ToggleStyle/makeBody(configuration:)`` method of a ``ToggleStyle`` to
/// create an instance of the styled toggle. This is useful for custom
/// toggle styles that only modify the current toggle style, as opposed to
/// implementing a brand new style.
///
/// For example, the following style adds a red border around the toggle,
/// but otherwise preserves the toggle's current style:
///
///     struct RedBorderToggleStyle: ToggleStyle {
///         func makeBody(configuration: Configuration) -> some View {
///             Toggle(configuration)
///                 .padding()
///                 .border(.red)
///         }
///     }
///
/// - Parameter configuration: The properties of the toggle, including a
///   label and a binding to the toggle's state.
//    @available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
//    public init(_ configuration: ToggleStyleConfiguration) { fatalError() }
//}

/// The properties of a toggle instance.
///
/// When you define a custom toggle style by creating a type that conforms to
/// the ``ToggleStyle`` protocol, you implement the
/// ``ToggleStyle/makeBody(configuration:)`` method. That method takes a
/// `ToggleStyleConfiguration` input that has the information you need
/// to define the behavior and appearance of a ``Toggle``.
///
/// The configuration structure's ``label-swift.property`` reflects the
/// toggle's content, which might be the value that you supply to the
/// `label` parameter of the ``Toggle/init(isOn:label:)`` initializer.
/// Alternatively, it could be another view that SkipUI builds from an
/// initializer that takes a string input, like ``Toggle/init(_:isOn:)-8qx3l``.
/// In either case, incorporate the label into the toggle's view to help
/// the user understand what the toggle does. For example, the built-in
/// ``ToggleStyle/switch`` style horizontally stacks the label with the
/// control element.
///
/// The structure's ``isOn`` property provides a ``Binding`` to the state
/// of the toggle. Adjust the appearance of the toggle based on this value.
/// For example, the built-in ``ToggleStyle/button`` style fills the button's
/// background when the property is `true`, but leaves the background empty
/// when the property is `false`. Change the value when the user performs
/// an action that's meant to change the toggle, like the button does when
/// tapped or clicked by the user.
@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
public struct ToggleStyleConfiguration {

/// A type-erased label of a toggle.
///
/// SkipUI provides a value of this type --- which is a ``View`` type ---
/// as the ``label-swift.property`` to your custom toggle style
/// implementation. Use the label to help define the appearance of the
/// toggle.
public struct Label : View {

/// The type of view representing the body of this view.
///
/// When you create a custom view, Swift infers this type from your
/// implementation of the required ``View/body-swift.property`` property.
public typealias Body = NeverView
public var body: Body { fatalError() }
}

/// A view that describes the effect of switching the toggle between states.
///
/// Use this value in your implementation of the
/// ``ToggleStyle/makeBody(configuration:)`` method when defining a custom
/// ``ToggleStyle``. Access it through the that method's `configuration`
/// parameter.
///
/// Because the label is a ``View``, you can incorporate it into the
/// view hierarchy that you return from your style definition. For example,
/// you can combine the label with a circle image in an ``HStack``:
///
///     HStack {
///         Image(systemName: configuration.isOn
///             ? "checkmark.circle.fill"
///             : "circle")
///         configuration.label
///     }
///
public let label: ToggleStyleConfiguration.Label = { fatalError() }()

/// A binding to a state property that indicates whether the toggle is on.
///
/// Because this value is a ``Binding``, you can both read and write it
/// in your implementation of the ``ToggleStyle/makeBody(configuration:)``
/// method when defining a custom ``ToggleStyle``. Access it through
/// that method's `configuration` parameter.
///
/// Read this value to set the appearance of the toggle. For example, you
/// can choose between empty and filled circles based on the `isOn` value:
///
///     Image(systemName: configuration.isOn
///         ? "checkmark.circle.fill"
///         : "circle")
///
/// Write this value when the user takes an action that's meant to change
/// the state of the toggle. For example, you can toggle it inside the
/// `action` closure of a ``Button`` instance:
///
///     Button {
///         configuration.isOn.toggle()
///     } label: {
///         // Draw the toggle.
///     }
///
//    @Binding public var isOn: Bool { get { fatalError() } nonmutating set { } }

//    public var $isOn: Binding<Bool> { get { fatalError() } }

/// Whether the ``Toggle`` is currently in a mixed state.
///
/// Use this property to determine whether the toggle style should render
/// a mixed state presentation. A mixed state corresponds to an underlying
/// collection with a mix of true and false Bindings.
/// To toggle the state, use the ``Bool.toggle()`` method on the ``isOn``
/// binding.
///
/// In the following example, a custom style uses the `isMixed` property
/// to render the correct toggle state using symbols:
///
///     struct SymbolToggleStyle: ToggleStyle {
///         func makeBody(configuration: Configuration) -> some View {
///             Button {
///                 configuration.isOn.toggle()
///             } label: {
///                 Image(
///                     systemName: configuration.isMixed
///                     ? "minus.circle.fill" : configuration.isOn
///                     ? "checkmark.circle.fill" : "circle.fill")
///                 configuration.label
///             }
///         }
///     }
@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
public var isMixed: Bool { get { fatalError() } }
}
*/
