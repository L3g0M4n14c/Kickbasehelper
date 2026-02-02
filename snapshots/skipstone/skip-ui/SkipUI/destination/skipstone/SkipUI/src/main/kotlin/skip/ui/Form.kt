package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.runtime.Composable

@androidx.annotation.Keep
class Form: View, skip.lib.SwiftProjecting {
    // It appears that on iOS, List and Form render the same
    internal val list: List

    constructor(content: () -> View) {
        this.list = List(content = content)
    }

    constructor(bridgedContent: View) {
        this.list = List(bridgedContent = bridgedContent)
    }

    @Composable
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> = list.Evaluate(context = context, options = options)

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

class FormStyle: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FormStyle) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = FormStyle(rawValue = 0)

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val columns = FormStyle(rawValue = 1)

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val grouped = FormStyle(rawValue = 2)
    }
}

/*
@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
extension Form where Content == FormStyleConfiguration.Content {

/// Creates a form based on a form style configuration.
///
/// - Parameter configuration: The properties of the form.
public init(_ configuration: FormStyleConfiguration) { fatalError() }
}

/// The properties of a form instance.
@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
public struct FormStyleConfiguration {

/// A type-erased content of a form.
public struct Content : View {

/// The type of view representing the body of this view.
///
/// When you create a custom view, Swift infers this type from your
/// implementation of the required ``View/body-swift.property`` property.
public typealias Body = NeverView
public var body: Body { fatalError() }
}

/// A view that is the content of the form.
public let content: FormStyleConfiguration.Content = { fatalError() }()
}
*/
