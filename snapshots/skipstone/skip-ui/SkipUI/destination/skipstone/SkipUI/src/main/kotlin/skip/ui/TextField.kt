package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation

@androidx.annotation.Keep
class TextField: View, Renderable, skip.lib.SwiftProjecting {
    internal val text: Binding<String>
    internal val selection: Binding<TextSelection?>?
    internal val label: ComposeBuilder
    internal val prompt: Text?
    internal val isSecure: Boolean

    constructor(text: Binding<String>, selection: Binding<TextSelection?>? = null, prompt: Text? = null, isSecure: Boolean = false, label: () -> View) {
        this.text = text.sref()
        this.selection = selection.sref()
        this.label = ComposeBuilder.from(label)
        this.prompt = prompt
        this.isSecure = isSecure
    }

    constructor(getText: () -> String, setText: (String) -> Unit, getSelection: () -> TextSelection?, setSelection: (TextSelection?) -> Unit, prompt: Text?, isSecure: Boolean, bridgedLabel: View) {
        this.text = Binding(get = getText, set = setText)
        this.selection = Binding(get = getSelection, set = setSelection)
        this.label = ComposeBuilder.from { -> bridgedLabel }
        this.prompt = prompt
        this.isSecure = isSecure
    }

    constructor(title: String, text: Binding<String>, selection: Binding<TextSelection?>? = null, prompt: Text? = null): this(text = text, selection = selection, prompt = prompt, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(verbatim = title).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleKey: LocalizedStringKey, text: Binding<String>, selection: Binding<TextSelection?>? = null, prompt: Text? = null): this(text = text, selection = selection, prompt = prompt, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleKey).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleResource: LocalizedStringResource, text: Binding<String>, selection: Binding<TextSelection?>? = null, prompt: Text? = null): this(text = text, selection = selection, prompt = prompt, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleResource).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(titleKey: LocalizedStringKey, text: Binding<String>, axis: Axis): this(titleKey, text = text) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(titleResource: LocalizedStringResource, text: Binding<String>, axis: Axis): this(titleResource, text = text) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(titleKey: LocalizedStringKey, text: Binding<String>, prompt: Text?, axis: Axis): this(titleKey, text = text, prompt = prompt) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(titleResource: LocalizedStringResource, text: Binding<String>, prompt: Text?, axis: Axis): this(titleResource, text = text, prompt = prompt) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(title: String, text: Binding<String>, axis: Axis): this(title, text = text) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(title: String, text: Binding<String>, prompt: Text?, axis: Axis): this(title, text = text, prompt = prompt) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(text: Binding<String>, prompt: Text? = null, axis: Axis, label: () -> View): this(text = text, prompt = prompt, label = label) {
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val contentContext = context.content()
        val textEnvironment = EnvironmentValues.shared._textEnvironment.sref()
        val redaction = EnvironmentValues.shared.redactionReasons.sref()
        val styleInfo = Text.styleInfo(textEnvironment = textEnvironment, redaction = redaction, context = context)
        val animatable = styleInfo.style.asAnimatable(context = context)
        val colors = Companion.colors(styleInfo = styleInfo)
        val keyboardOptions = (if (isSecure) KeyboardOptions(keyboardType = KeyboardType.Password) else EnvironmentValues.shared._keyboardOptions ?: KeyboardOptions.Default).sref()
        val keyboardActions = KeyboardActions(EnvironmentValues.shared._onSubmitState, LocalFocusManager.current)
        val visualTransformation = (if (isSecure) PasswordVisualTransformation() else VisualTransformation.None).sref()

        val currentText = text.wrappedValue.sref()
        val currentSelection = selection?.wrappedValue?.asComposeTextRange()
        val defaultTextFieldValue = TextFieldValue(text = currentText, selection = TextRange(currentText.count))
        val textFieldValue = remember { -> mutableStateOf(defaultTextFieldValue) }
        var currentTextFieldValue = textFieldValue.value.sref()

        // If the text has been updated externally, use the default value for the current text,
        // which also places the cursor at the end. This mimics SwiftUI behavior for external modifications,
        // such as when applying formatting to the user input.
        if (currentTextFieldValue.text != currentText) {
            currentTextFieldValue = defaultTextFieldValue.sref()
        }

        // If the selection has been updated externally, update just the selection value.
        if ((currentSelection != null) && (currentTextFieldValue.selection != currentSelection)) {
            currentTextFieldValue = currentTextFieldValue.copy(selection = currentSelection)
        }

        val textAlign = EnvironmentValues.shared.multilineTextAlignment.asTextAlign()
        val alignedTextStyle = animatable.value.merge(TextStyle(textAlign = textAlign))

        var options = Material3TextFieldOptions(value = currentTextFieldValue, onValueChange = { value ->
            text.wrappedValue = value.text
            selection?.wrappedValue = TextSelection(range = value.selection.start until value.selection.end)
            textFieldValue.value = value
        }, placeholder = { -> Companion.Placeholder(prompt = prompt ?: label, context = contentContext) }, modifier = context.modifier.fillWidth(), textStyle = alignedTextStyle, enabled = EnvironmentValues.shared.isEnabled, singleLine = true, visualTransformation = visualTransformation, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions, maxLines = 1, shape = OutlinedTextFieldDefaults.shape, colors = colors)
        EnvironmentValues.shared._material3TextField?.let { updateOptions ->
            options = updateOptions(options)
        }
        OutlinedTextField(value = options.value, onValueChange = options.onValueChange, modifier = options.modifier, enabled = options.enabled, readOnly = options.readOnly, textStyle = options.textStyle, label = options.label, placeholder = options.placeholder, leadingIcon = options.leadingIcon, trailingIcon = options.trailingIcon, prefix = options.prefix, suffix = options.suffix, supportingText = options.supportingText, isError = options.isError, visualTransformation = options.visualTransformation, keyboardOptions = options.keyboardOptions, keyboardActions = options.keyboardActions, singleLine = options.singleLine, maxLines = options.maxLines, minLines = options.minLines, interactionSource = options.interactionSource, shape = options.shape, colors = options.colors)
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {

        @Composable
        internal fun textColor(styleInfo: TextStyleInfo, enabled: Boolean): androidx.compose.ui.graphics.Color {
            val color_0 = styleInfo.color
            if (color_0 == null) {
                return androidx.compose.ui.graphics.Color.Unspecified
            }
            return if (enabled) color_0 else color_0.copy(alpha = ContentAlpha.disabled)
        }

        @Composable
        internal fun colors(styleInfo: TextStyleInfo, outline: Color? = null): TextFieldColors {
            val textColor = textColor(styleInfo = styleInfo, enabled = true)
            val disabledTextColor = textColor(styleInfo = styleInfo, enabled = false)
            val isPlainStyle = EnvironmentValues.shared._textFieldStyle == TextFieldStyle.plain
            if (isPlainStyle) {
                val clearColor = androidx.compose.ui.graphics.Color.Transparent.sref()
                val matchtarget_0 = EnvironmentValues.shared._tint
                if (matchtarget_0 != null) {
                    val tint = matchtarget_0
                    val tintColor = tint.colorImpl()
                    return OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, disabledTextColor = disabledTextColor, cursorColor = tintColor, focusedBorderColor = clearColor, unfocusedBorderColor = clearColor, disabledBorderColor = clearColor, errorBorderColor = clearColor)
                } else {
                    return OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, disabledTextColor = disabledTextColor, focusedBorderColor = clearColor, unfocusedBorderColor = clearColor, disabledBorderColor = clearColor, errorBorderColor = clearColor)
                }
            } else {
                val borderColor = (outline ?: Color.primary.opacity(0.3)).colorImpl()
                val matchtarget_1 = EnvironmentValues.shared._tint
                if (matchtarget_1 != null) {
                    val tint = matchtarget_1
                    val tintColor = tint.colorImpl()
                    return OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, disabledTextColor = disabledTextColor, cursorColor = tintColor, focusedBorderColor = tintColor, unfocusedBorderColor = borderColor, disabledBorderColor = Color.separator.colorImpl())
                } else {
                    return OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, disabledTextColor = disabledTextColor, unfocusedBorderColor = borderColor, disabledBorderColor = Color.separator.colorImpl())
                }
            }
        }

        @Composable
        internal fun Placeholder(prompt: View?, context: ComposeContext) {
            if (prompt == null) {
                return
            }

            EnvironmentValues.shared.setValues(l@{ it ->
                it.set_material3Text { options -> options.copy(modifier = options.modifier.fillMaxWidth()) }
                it.set_foregroundStyle(Color(colorImpl = { -> Color.primary.colorImpl().copy(alpha = ContentAlpha.disabled) }))
                return@l ComposeResult.ok
            }, in_ = { -> prompt.Compose(context = context) })
        }
    }
}

class TextFieldStyle: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TextFieldStyle) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = TextFieldStyle(rawValue = 0) // For bridging
        val roundedBorder = TextFieldStyle(rawValue = 1) // For bridging
        val plain = TextFieldStyle(rawValue = 2) // For bridging
    }
}

@Suppress("MUST_BE_INITIALIZED")
class Material3TextFieldOptions: MutableStruct {
    var value: TextFieldValue
        get() = field.sref({ this.value = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var onValueChange: (TextFieldValue) -> Unit
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
    var readOnly: Boolean
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var textStyle: TextStyle
        get() = field.sref({ this.textStyle = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var label: (@Composable () -> Unit)? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var placeholder: (@Composable () -> Unit)? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var leadingIcon: (@Composable () -> Unit)? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var trailingIcon: (@Composable () -> Unit)? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var prefix: (@Composable () -> Unit)? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var suffix: (@Composable () -> Unit)? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var supportingText: (@Composable () -> Unit)? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var isError: Boolean
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var visualTransformation: VisualTransformation
        get() = field.sref({ this.visualTransformation = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var keyboardOptions: KeyboardOptions
        get() = field.sref({ this.keyboardOptions = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var keyboardActions: KeyboardActions
        get() = field.sref({ this.keyboardActions = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var singleLine: Boolean
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var maxLines: Int
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var minLines: Int
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
    var shape: androidx.compose.ui.graphics.Shape
        get() = field.sref({ this.shape = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var colors: TextFieldColors
        get() = field.sref({ this.colors = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    fun copy(value: TextFieldValue = this.value, onValueChange: (TextFieldValue) -> Unit = this.onValueChange, modifier: Modifier = this.modifier, enabled: Boolean = this.enabled, readOnly: Boolean = this.readOnly, textStyle: TextStyle = this.textStyle, label: (@Composable () -> Unit)? = this.label, placeholder: (@Composable () -> Unit)? = this.placeholder, leadingIcon: (@Composable () -> Unit)? = this.leadingIcon, trailingIcon: (@Composable () -> Unit)? = this.trailingIcon, prefix: (@Composable () -> Unit)? = this.prefix, suffix: (@Composable () -> Unit)? = this.suffix, supportingText: (@Composable () -> Unit)? = this.supportingText, isError: Boolean = this.isError, visualTransformation: VisualTransformation = this.visualTransformation, keyboardOptions: KeyboardOptions = this.keyboardOptions, keyboardActions: KeyboardActions = this.keyboardActions, singleLine: Boolean = this.singleLine, maxLines: Int = Int.MAX_VALUE, minLines: Int = this.minLines, interactionSource: MutableInteractionSource? = this.interactionSource, shape: androidx.compose.ui.graphics.Shape = this.shape, colors: TextFieldColors = this.colors): Material3TextFieldOptions = Material3TextFieldOptions(value = value, onValueChange = onValueChange, modifier = modifier, enabled = enabled, readOnly = readOnly, textStyle = textStyle, label = label, placeholder = placeholder, leadingIcon = leadingIcon, trailingIcon = trailingIcon, prefix = prefix, suffix = suffix, supportingText = supportingText, isError = isError, visualTransformation = visualTransformation, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions, singleLine = singleLine, maxLines = maxLines, minLines = minLines, interactionSource = interactionSource, shape = shape, colors = colors)

    constructor(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, readOnly: Boolean = false, textStyle: TextStyle, label: (@Composable () -> Unit)? = null, placeholder: (@Composable () -> Unit)? = null, leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null, prefix: (@Composable () -> Unit)? = null, suffix: (@Composable () -> Unit)? = null, supportingText: (@Composable () -> Unit)? = null, isError: Boolean = false, visualTransformation: VisualTransformation = VisualTransformation.None.sref(), keyboardOptions: KeyboardOptions = KeyboardOptions.Default.sref(), keyboardActions: KeyboardActions = KeyboardActions.Default.sref(), singleLine: Boolean = false, maxLines: Int = Int.max, minLines: Int = 1, interactionSource: MutableInteractionSource? = null, shape: androidx.compose.ui.graphics.Shape, colors: TextFieldColors) {
        this.value = value
        this.onValueChange = onValueChange
        this.modifier = modifier
        this.enabled = enabled
        this.readOnly = readOnly
        this.textStyle = textStyle
        this.label = label
        this.placeholder = placeholder
        this.leadingIcon = leadingIcon
        this.trailingIcon = trailingIcon
        this.prefix = prefix
        this.suffix = suffix
        this.supportingText = supportingText
        this.isError = isError
        this.visualTransformation = visualTransformation
        this.keyboardOptions = keyboardOptions
        this.keyboardActions = keyboardActions
        this.singleLine = singleLine
        this.maxLines = maxLines
        this.minLines = minLines
        this.interactionSource = interactionSource
        this.shape = shape
        this.colors = colors
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = Material3TextFieldOptions(value, onValueChange, modifier, enabled, readOnly, textStyle, label, placeholder, leadingIcon, trailingIcon, prefix, suffix, supportingText, isError, visualTransformation, keyboardOptions, keyboardActions, singleLine, maxLines, minLines, interactionSource, shape, colors)

    @androidx.annotation.Keep
    companion object {
    }
}

/// State for `onSubmit` actions.
internal class OnSubmitState {
    internal val actions: Array<Tuple2<SubmitTriggers, () -> Unit>>

    internal constructor(triggers: SubmitTriggers, action: () -> Unit) {
        actions = arrayOf(Tuple2(triggers.sref(), action))
    }

    private constructor(actions: Array<Tuple2<SubmitTriggers, () -> Unit>>) {
        this.actions = actions.sref()
    }

    internal fun appending(triggers: SubmitTriggers, action: () -> Unit): OnSubmitState = OnSubmitState(actions = actions + arrayOf(Tuple2(triggers.sref(), action)))

    internal fun appending(state: OnSubmitState): OnSubmitState = OnSubmitState(actions = actions + state.actions)

    internal fun onSubmit(trigger: SubmitTriggers) {
        for (action in actions.sref()) {
            if (action.element0.contains(trigger)) {
                action.element1()
            }
        }
    }
}

/// Create keyboard actions that execute the given submit state.
internal fun KeyboardActions(submitState: OnSubmitState?, clearFocusWith: FocusManager? = null): KeyboardActions {
    return KeyboardActions(onDone = { ->
        clearFocusWith?.clearFocus()
        submitState?.onSubmit(trigger = SubmitTriggers.text)
    }, onGo = { ->
        clearFocusWith?.clearFocus()
        submitState?.onSubmit(trigger = SubmitTriggers.text)
    }, onNext = { ->
        clearFocusWith?.clearFocus()
        submitState?.onSubmit(trigger = SubmitTriggers.text)
    }, onPrevious = { ->
        clearFocusWith?.clearFocus()
        submitState?.onSubmit(trigger = SubmitTriggers.text)
    }, onSearch = { ->
        clearFocusWith?.clearFocus()
        submitState?.onSubmit(trigger = SubmitTriggers.search)
    }, onSend = { ->
        clearFocusWith?.clearFocus()
        submitState?.onSubmit(trigger = SubmitTriggers.text)
    })
}

/*
import class Foundation.Formatter
import protocol Foundation.ParseableFormatStyle

//extension TextField where Label == Text {

/// Creates a text field that applies a format style to a bound optional
/// value, with a label generated from a localized title string.
///
/// Use this initializer to create a text field that binds to a bound optional
/// value, using a
/// to convert to and from this type. Changes to the bound value update
/// the string displayed by the text field. Editing the text field
/// updates the bound value, as long as the format style can parse the
/// text. If the format style can't parse the input, the text field
/// sets the bound value to `nil`.
///
/// Use the ``View/onSubmit(of:_:)`` modifier to invoke an action
/// whenever the user submits this text field.
///
/// The following example uses an optional
/// as the bound currency value, and a
/// instance to convert to and from a representation as U.S. dollars. As
/// the user types, a `View.onChange(of:_:)` modifier logs the new value to
/// the console. If the user enters an invalid currency value, like letters
/// or emoji, the console output is `Optional(nil)`.
///
///     @State private var myMoney: Double? = 300.0
///     var body: some View {
///         TextField(
///             "Currency (USD)",
///             value: $myMoney,
///             format: .currency(code: "USD")
///         )
///         .onChange(of: myMoney) { newValue in
///             print ("myMoney: \(newValue)")
///         }
///     }
///
/// - Parameters:
///   - titleKey: The title of the text field, describing its purpose.
///   - value: The underlying value to edit.
///   - format: A format style of type `F` to use when converting between
///     the string the user edits and the underlying value of type
///     `F.FormatInput`. If `format` can't perform the conversion, the text
///     field sets `binding.value` to `nil`.
///   - prompt: A `Text` which provides users with guidance on what to type
///     into the text field.
//    @available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
//    public init<F>(_ titleKey: LocalizedStringKey, value: Binding<F.FormatInput?>, format: F, prompt: Text? = nil) where F : ParseableFormatStyle, F.FormatOutput == String { fatalError() }

/// Creates a text field that applies a format style to a bound optional
/// value, with a label generated from a title string.
///
/// Use this initializer to create a text field that binds to a bound optional
/// value, using a
/// to convert to and from this type. Changes to the bound value update
/// the string displayed by the text field. Editing the text field
/// updates the bound value, as long as the format style can parse the
/// text. If the format style can't parse the input, the text field
/// sets the bound value to `nil`.
///
/// Use the ``View/onSubmit(of:_:)`` modifier to invoke an action
/// whenever the user submits this text field.
///
/// The following example uses an optional
/// as the bound currency value, and a
/// instance to convert to and from a representation as U.S. dollars. As
/// the user types, a `View.onChange(of:_:)` modifier logs the new value to
/// the console. If the user enters an invalid currency value, like letters
/// or emoji, the console output is `Optional(nil)`.
///
///     @State private var label = "Currency (USD)"
///     @State private var myMoney: Double? = 300.0
///     var body: some View {
///         TextField(
///             label,
///             value: $myMoney,
///             format: .currency(code: "USD")
///         )
///         .onChange(of: myMoney) { newValue in
///             print ("myMoney: \(newValue)")
///         }
///     }
///
/// - Parameters:
///   - title: The title of the text field, describing its purpose.
///   - value: The underlying value to edit.
///   - format: A format style of type `F` to use when converting between
///     the string the user edits and the underlying value of type
///     `F.FormatInput`. If `format` can't perform the conversion, the text
///     field sets `binding.value` to `nil`.
///   - prompt: A `Text` which provides users with guidance on what to type
///     into the text field.
//    @available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
//    public init<S, F>(_ title: S, value: Binding<F.FormatInput?>, format: F, prompt: Text? = nil) where S : StringProtocol, F : ParseableFormatStyle, F.FormatOutput == String { fatalError() }

/// Creates a text field that applies a format style to a bound
/// value, with a label generated from a localized title string.
///
/// Use this initializer to create a text field that binds to a bound
/// value, using a
/// to convert to and from this type. Changes to the bound value update
/// the string displayed by the text field. Editing the text field
/// updates the bound value, as long as the format style can parse the
/// text. If the format style can't parse the input, the bound value
/// remains unchanged.
///
/// Use the ``View/onSubmit(of:_:)`` modifier to invoke an action
/// whenever the user submits this text field.
///
///
/// The following example uses a
/// as the bound value, and a
/// instance to convert to and from a string representation. As the user types, the bound
/// value updates, which in turn updates three ``Text`` views that use
/// different format styles. If the user enters text that doesn't represent
/// a valid `Double`, the bound value doesn't update.
///
///     @State private var myDouble: Double = 0.673
///     var body: some View {
///         VStack {
///             TextField(
///                 "Double",
///                 value: $myDouble,
///                 format: .number
///             )
///             Text(myDouble, format: .number)
///             Text(myDouble, format: .number.precision(.significantDigits(5)))
///             Text(myDouble, format: .number.notation(.scientific))
///         }
///     }
///
/// ![A text field with the string 0.673. Below this, three text views
/// showing the number with different styles: 0.673, 0.67300, and 6.73E-1.](TextField-init-format-1)
///
/// - Parameters:
///   - titleKey: The title of the text field, describing its purpose.
///   - value: The underlying value to edit.
///   - format: A format style of type `F` to use when converting between
///     the string the user edits and the underlying value of type
///     `F.FormatInput`. If `format` can't perform the conversion, the text
///     field leaves `binding.value` unchanged. If the user stops editing
///     the text in an invalid state, the text field updates the field's
///     text to the last known valid value.
///   - prompt: A `Text` which provides users with guidance on what to type
///     into the text field.
//    @available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
//    public init<F>(_ titleKey: LocalizedStringKey, value: Binding<F.FormatInput>, format: F, prompt: Text? = nil) where F : ParseableFormatStyle, F.FormatOutput == String { fatalError() }

/// Creates a text field that applies a format style to a bound
/// value, with a label generated from a title string.
///
/// Use this initializer to create a text field that binds to a bound
/// value, using a
/// to convert to and from this type. Changes to the bound value update
/// the string displayed by the text field. Editing the text field
/// updates the bound value, as long as the format style can parse the
/// text. If the format style can't parse the input, the bound value
/// remains unchanged.
///
/// Use the ``View/onSubmit(of:_:)`` modifier to invoke an action
/// whenever the user submits this text field.
///
/// The following example uses a
/// as the bound value, and a
/// instance to convert to and from a string representation. As the user types, the bound
/// value updates, which in turn updates three ``Text`` views that use
/// different format styles. If the user enters text that doesn't represent
/// a valid `Double`, the bound value doesn't update.
///
///     @State private var label = "Double"
///     @State private var myDouble: Double = 0.673
///     var body: some View {
///         VStack {
///             TextField(
///                 label,
///                 value: $myDouble,
///                 format: .number
///             )
///             Text(myDouble, format: .number)
///             Text(myDouble, format: .number.precision(.significantDigits(5)))
///             Text(myDouble, format: .number.notation(.scientific))
///         }
///     }
///
/// ![A text field with the string 0.673. Below this, three text views
/// showing the number with different styles: 0.673, 0.67300, and 6.73E-1.](TextField-init-format-1)
/// - Parameters:
///   - title: The title of the text field, describing its purpose.
///   - value: The underlying value to edit.
///   - format: A format style of type `F` to use when converting between
///     the string the user edits and the underlying value of type
///     `F.FormatInput`. If `format` can't perform the conversion, the text
///     field leaves `binding.value` unchanged. If the user stops editing
///     the text in an invalid state, the text field updates the field's
///     text to the last known valid value.
///   - prompt: A `Text` which provides users with guidance on what to type
///     into the text field.
//    @available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
//    public init<S, F>(_ title: S, value: Binding<F.FormatInput>, format: F, prompt: Text? = nil) where S : StringProtocol, F : ParseableFormatStyle, F.FormatOutput == String { fatalError() }
//}

extension TextField {

/// Creates a text field that applies a format style to a bound optional
/// value, with a label generated from a view builder.
///
/// Use this initializer to create a text field that binds to a bound optional
/// value, using a
/// to convert to and from this type. Changes to the bound value update
/// the string displayed by the text field. Editing the text field
/// updates the bound value, as long as the format style can parse the
/// text. If the format style can't parse the input, the text field
/// sets the bound value to `nil`.
///
/// Use the ``View/onSubmit(of:_:)`` modifier to invoke an action
/// whenever the user submits this text field.
///
/// The following example uses an optional
/// as the bound currency value, and a
/// instance to convert to and from a representation as U.S. dollars. As
/// the user types, a `View.onChange(of:_:)` modifier logs the new value to
/// the console. If the user enters an invalid currency value, like letters
/// or emoji, the console output is `Optional(nil)`.
///
///     @State private var myMoney: Double? = 300.0
///     var body: some View {
///         TextField(
///             value: $myMoney,
///             format: .currency(code: "USD")
///         ) {
///             Text("Currency (USD)")
///         }
///         .onChange(of: myMoney) { newValue in
///             print ("myMoney: \(newValue)")
///         }
///     }
///
/// - Parameters:
///   - value: The underlying value to edit.
///   - format: A format style of type `F` to use when converting between
///     the string the user edits and the underlying value of type
///     `F.FormatInput`. If `format` can't perform the conversion, the text
///     field sets `binding.value` to `nil`.
///   - prompt: A `Text` which provides users with guidance on what to type
///     into the text field.
///   - label: A view builder that produces a label for the text field,
///     describing its purpose.
//    @available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
//    public init<F>(value: Binding<F.FormatInput?>, format: F, prompt: Text? = nil, @ViewBuilder label: () -> Label) where F : ParseableFormatStyle, F.FormatOutput == String { fatalError() }

/// Creates a text field that applies a format style to a bound
/// value, with a label generated from a view builder.
///
/// Use this initializer to create a text field that binds to a bound
/// value, using a
/// to convert to and from this type. Changes to the bound value update
/// the string displayed by the text field. Editing the text field
/// updates the bound value, as long as the format style can parse the
/// text. If the format style can't parse the input, the bound value
/// remains unchanged.
///
/// Use the ``View/onSubmit(of:_:)`` modifier to invoke an action
/// whenever the user submits this text field.
///
/// The following example uses a
/// as the bound value, and a
/// instance to convert to and from a string representation. As the user types, the bound
/// value updates, which in turn updates three ``Text`` views that use
/// different format styles. If the user enters text that doesn't represent
/// a valid `Double`, the bound value doesn't update.
///
///     @State private var myDouble: Double = 0.673
///     var body: some View {
///         VStack {
///             TextField(
///                 value: $myDouble,
///                 format: .number
///             ) {
///                 Text("Double")
///             }
///             Text(myDouble, format: .number)
///             Text(myDouble, format: .number.precision(.significantDigits(5)))
///             Text(myDouble, format: .number.notation(.scientific))
///         }
///     }
///
/// ![A text field with the string 0.673. Below this, three text views
/// showing the number with different styles: 0.673, 0.67300, and 6.73E-1.](TextField-init-format-1)
///
/// - Parameters:
///   - value: The underlying value to edit.
///   - format: A format style of type `F` to use when converting between
///     the string the user edits and the underlying value of type
///     `F.FormatInput`. If `format` can't perform the conversion, the text
///     field leaves the value unchanged. If the user stops editing
///     the text in an invalid state, the text field updates the field's
///     text to the last known valid value.
///   - prompt: A `Text` which provides users with guidance on what to type
///     into the text field.
///   - label: A view builder that produces a label for the text field,
///     describing its purpose.
//    @available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
//    public init<F>(value: Binding<F.FormatInput>, format: F, prompt: Text? = nil, @ViewBuilder label: () -> Label) where F : ParseableFormatStyle, F.FormatOutput == String { fatalError() }
}

//extension TextField where Label == Text {

/// Creates a text field that applies a formatter to a bound
/// value, with a label generated from a localized title string.
///
/// Use this initializer to create a text field that binds to a bound
/// value, using a
/// to convert to and from this type. Changes to the bound value update
/// the string displayed by the text field. Editing the text field
/// updates the bound value, as long as the formatter can parse the
/// text. If the format style can't parse the input, the bound value
/// remains unchanged.
///
/// Use the ``View/onSubmit(of:_:)`` modifier to invoke an action
/// whenever the user submits this text field.
///
/// The following example uses a
/// as the bound value, and a
/// instance to convert to and from a string representation. The formatter
/// uses the
/// style, to allow entering a fractional part. As the user types, the bound
/// value updates, which in turn updates three ``Text`` views that use
/// different format styles. If the user enters text that doesn't represent
/// a valid `Double`, the bound value doesn't update.
///
///     @State private var myDouble: Double = 0.673
///     @State private var numberFormatter: NumberFormatter = {
///         var nf = NumberFormatter()
///         nf.numberStyle = .decimal
///         return nf
///     }()
///
///     var body: some View {
///         VStack {
///             TextField(
///                 "Double",
///                 value: $myDouble,
///                 formatter: numberFormatter
///             )
///             Text(myDouble, format: .number)
///             Text(myDouble, format: .number.precision(.significantDigits(5)))
///             Text(myDouble, format: .number.notation(.scientific))
///         }
///     }
///
/// - Parameters:
///   - titleKey: The key for the localized title of the text field,
///     describing its purpose.
///   - value: The underlying value to edit.
///   - formatter: A formatter to use when converting between the
///     string the user edits and the underlying value of type `V`.
///     If `formatter` can't perform the conversion, the text field doesn't
///     modify `binding.value`.
///   - prompt: A `Text` which provides users with guidance on what to enter
///     into the text field.
//    @available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
//    public init<V>(_ titleKey: LocalizedStringKey, value: Binding<V>, formatter: Formatter, prompt: Text?) { fatalError() }

/// Creates a text field that applies a formatter to a bound
/// value, with a label generated from a title string.
///
/// Use this initializer to create a text field that binds to a bound
/// value, using a
/// to convert to and from this type. Changes to the bound value update
/// the string displayed by the text field. Editing the text field
/// updates the bound value, as long as the formatter can parse the
/// text. If the format style can't parse the input, the bound value
/// remains unchanged.
///
/// Use the ``View/onSubmit(of:_:)`` modifier to invoke an action
/// whenever the user submits this text field.
///
///
/// The following example uses a
/// as the bound value, and a
/// instance to convert to and from a string representation. The formatter
/// uses the
/// style, to allow entering a fractional part. As the user types, the bound
/// value updates, which in turn updates three ``Text`` views that use
/// different format styles. If the user enters text that doesn't represent
/// a valid `Double`, the bound value doesn't update.
///
///     @State private var label = "Double"
///     @State private var myDouble: Double = 0.673
///     @State private var numberFormatter: NumberFormatter = {
///         var nf = NumberFormatter()
///         nf.numberStyle = .decimal
///         return nf
///     }()
///
///     var body: some View {
///         VStack {
///             TextField(
///                 label,
///                 value: $myDouble,
///                 formatter: numberFormatter
///             )
///             Text(myDouble, format: .number)
///             Text(myDouble, format: .number.precision(.significantDigits(5)))
///             Text(myDouble, format: .number.notation(.scientific))
///         }
///     }
///
/// - Parameters:
///   - title: The title of the text field, describing its purpose.
///   - value: The underlying value to edit.
///   - formatter: A formatter to use when converting between the
///     string the user edits and the underlying value of type `V`.
///     If `formatter` can't perform the conversion, the text field doesn't
///     modify `binding.value`.
///   - prompt: A `Text` which provides users with guidance on what to enter
///     into the text field.
//    @available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
//    public init<S, V>(_ title: S, value: Binding<V>, formatter: Formatter, prompt: Text?) where S : StringProtocol { fatalError() }
//}

extension TextField {

/// Creates a text field that applies a formatter to a bound optional
/// value, with a label generated from a view builder.
///
/// Use this initializer to create a text field that binds to a bound optional
/// value, using a
/// to convert to and from this type. Changes to the bound value update
/// the string displayed by the text field. Editing the text field
/// updates the bound value, as long as the formatter can parse the
/// text. If the format style can't parse the input, the bound value
/// remains unchanged.
///
/// Use the ``View/onSubmit(of:_:)`` modifier to invoke an action
/// whenever the user submits this text field.
///
/// The following example uses a
/// as the bound value, and a
/// instance to convert to and from a string representation. The formatter
/// uses the
/// style, to allow entering a fractional part. As the user types, the bound
/// value updates, which in turn updates three ``Text`` views that use
/// different format styles. If the user enters text that doesn't represent
/// a valid `Double`, the bound value doesn't update.
///
///     @State private var myDouble: Double = 0.673
///     @State private var numberFormatter: NumberFormatter = {
///         var nf = NumberFormatter()
///         nf.numberStyle = .decimal
///         return nf
///     }()
///
///     var body: some View {
///         VStack {
///             TextField(
///                 value: $myDouble,
///                 formatter: numberFormatter
///             ) {
///                 Text("Double")
///             }
///             Text(myDouble, format: .number)
///             Text(myDouble, format: .number.precision(.significantDigits(5)))
///             Text(myDouble, format: .number.notation(.scientific))
///         }
///     }
///
/// - Parameters:
///   - value: The underlying value to edit.
///   - formatter: A formatter to use when converting between the
///     string the user edits and the underlying value of type `V`.
///     If `formatter` can't perform the conversion, the text field doesn't
///     modify `binding.value`.
///   - prompt: A `Text` which provides users with guidance on what to enter
///     into the text field.
///   - label: A view that describes the purpose of the text field.
//    @available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
//    public init<V>(value: Binding<V>, formatter: Formatter, prompt: Text? = nil, @ViewBuilder label: () -> Label) { fatalError() }
}

//extension TextField where Label == Text {

/// Create an instance which binds over an arbitrary type, `V`.
///
/// Use this initializer to create a text field that binds to a bound optional
/// value, using a
/// to convert to and from this type. Changes to the bound value update
/// the string displayed by the text field. Editing the text field
/// updates the bound value, as long as the formatter can parse the
/// text. If the format style can't parse the input, the bound value
/// remains unchanged.
///
/// Use the ``View/onSubmit(of:_:)`` modifier to invoke an action
/// whenever the user submits this text field.
///
/// The following example uses a
/// as the bound value, and a
/// instance to convert to and from a string representation. The formatter
/// uses the
/// style, to allow entering a fractional part. As the user types, the bound
/// value updates, which in turn updates three ``Text`` views that use
/// different format styles. If the user enters text that doesn't represent
/// a valid `Double`, the bound value doesn't update.
///
///     @State private var myDouble: Double = 0.673
///     @State private var numberFormatter: NumberFormatter = {
///         var nf = NumberFormatter()
///         nf.numberStyle = .decimal
///         return nf
///     }()
///
///     var body: some View {
///         VStack {
///             TextField(
///                 value: $myDouble,
///                 formatter: numberFormatter
///             ) {
///                 Text("Double")
///             }
///             Text(myDouble, format: .number)
///             Text(myDouble, format: .number.precision(.significantDigits(5)))
///             Text(myDouble, format: .number.notation(.scientific))
///         }
///     }
///
/// - Parameters:
///   - titleKey: The key for the localized title of the text field,
///     describing its purpose.
///   - value: The underlying value to edit.
///   - formatter: A formatter to use when converting between the
///     string the user edits and the underlying value of type `V`.
///     If `formatter` can't perform the conversion, the text field doesn't
///     modify `binding.value`.
//    @available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
//    public init<V>(_ titleKey: LocalizedStringKey, value: Binding<V>, formatter: Formatter) { fatalError() }

/// Create an instance which binds over an arbitrary type, `V`.
///
/// Use this initializer to create a text field that binds to a bound optional
/// value, using a
/// to convert to and from this type. Changes to the bound value update
/// the string displayed by the text field. Editing the text field
/// updates the bound value, as long as the formatter can parse the
/// text. If the format style can't parse the input, the bound value
/// remains unchanged.
///
/// Use the ``View/onSubmit(of:_:)`` modifier to invoke an action
/// whenever the user submits this text field.
///
/// The following example uses a
/// as the bound value, and a
/// instance to convert to and from a string representation. The formatter
/// uses the
/// style, to allow entering a fractional part. As the user types, the bound
/// value updates, which in turn updates three ``Text`` views that use
/// different format styles. If the user enters text that doesn't represent
/// a valid `Double`, the bound value doesn't update.
///
///     @State private var myDouble: Double = 0.673
///     @State private var numberFormatter: NumberFormatter = {
///         var nf = NumberFormatter()
///         nf.numberStyle = .decimal
///         return nf
///     }()
///
///     var body: some View {
///         VStack {
///             TextField(
///                 value: $myDouble,
///                 formatter: numberFormatter
///             ) {
///                 Text("Double")
///             }
///             Text(myDouble, format: .number)
///             Text(myDouble, format: .number.precision(.significantDigits(5)))
///             Text(myDouble, format: .number.notation(.scientific))
///         }
///     }
///
/// - Parameters:
///   - title: The title of the text view, describing its purpose.
///   - value: The underlying value to edit.
///   - formatter: A formatter to use when converting between the
///     string the user edits and the underlying value of type `V`.
///     If `formatter` can't perform the conversion, the text field doesn't
///     modify `binding.value`.
//    @available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
//    public init<S, V>(_ title: S, value: Binding<V>, formatter: Formatter) where S : StringProtocol { fatalError() }
//}
*/
