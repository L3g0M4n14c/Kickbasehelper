package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.VisualTransformation

@androidx.annotation.Keep
class TextEditor: View, Renderable, skip.lib.SwiftProjecting {
    internal val text: Binding<String>

    constructor(text: Binding<String>) {
        this.text = text.sref()
    }

    constructor(getText: () -> String, setText: (String) -> Unit) {
        this.text = Binding(get = getText, set = setText)
    }

    constructor(titleResource: LocalizedStringResource, text: Binding<String>) {
        this.text = text.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(text: Binding<String>, selection: Any?) {
        this.text = Binding(get = { -> "" }, set = { _ ->  })
    }

    //    @available(*, unavailable)
    //    public init(text: Binding<AttributedString>, selection: Any? /* Binding<AttributedTextSelection>? */ = nil) {
    //        self.text = Binding(get: { "" }, set: { _ in })
    //    }

    @Composable
    override fun Render(context: ComposeContext) {
        val contentContext = context.content()
        val textEnvironment = EnvironmentValues.shared._textEnvironment.sref()
        val redaction = EnvironmentValues.shared.redactionReasons.sref()
        val styleInfo = Text.styleInfo(textEnvironment = textEnvironment, redaction = redaction, context = context)
        val animatable = styleInfo.style.asAnimatable(context = context)
        val keyboardOptions = (EnvironmentValues.shared._keyboardOptions ?: KeyboardOptions.Default).sref()
        val keyboardActions = KeyboardActions(EnvironmentValues.shared._onSubmitState, LocalFocusManager.current)
        val colors = TextField.colors(styleInfo = styleInfo, outline = Color.clear)
        val visualTransformation = VisualTransformation.None.sref()
        OutlinedTextField(value = text.wrappedValue, onValueChange = { it -> text.wrappedValue = it }, modifier = context.modifier.fillSize(), textStyle = animatable.value, enabled = EnvironmentValues.shared.isEnabled, singleLine = false, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions, colors = colors, visualTransformation = visualTransformation)
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

class TextEditorStyle: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TextEditorStyle) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = TextEditorStyle(rawValue = 0) // For bridging
        val plain = TextEditorStyle(rawValue = 1) // For bridging
    }
}

/*
/// The properties of a text editor.
@available(iOS 17.0, macOS 14.0, *)
@available(tvOS, unavailable)
@available(watchOS, unavailable)
public struct TextEditorStyleConfiguration {
}
*/
