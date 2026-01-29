package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*

class ControlGroup: View {
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(content: () -> View) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(content: () -> View, label: () -> View) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(titleKey: LocalizedStringKey, content: () -> View) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(titleResource: LocalizedStringResource, content: () -> View) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(title: String, content: () -> View) {
    }


    @androidx.annotation.Keep
    companion object {
    }
}

class ControlGroupStyle: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ControlGroupStyle) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = ControlGroupStyle(rawValue = 0)

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val navigation = ControlGroupStyle(rawValue = 1)

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val palette = ControlGroupStyle(rawValue = 2)

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val menu = ControlGroupStyle(rawValue = 3)

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val compactMenu = ControlGroupStyle(rawValue = 4)
    }
}

/*
@available(watchOS, unavailable)
extension ControlGroup where Content == ControlGroupStyleConfiguration.Content {

/// Creates a control group based on a style configuration.
///
/// Use this initializer within the
/// ``ControlGroupStyle/makeBody(configuration:)`` method of a
/// ``ControlGroupStyle`` instance to create an instance of the control group
/// being styled. This is useful for custom control group styles that modify
/// the current control group style.
///
/// For example, the following code creates a new, custom style that places a
/// red border around the current control group:
///
///     struct RedBorderControlGroupStyle: ControlGroupStyle {
///         func makeBody(configuration: Configuration) -> some View {
///             ControlGroup(configuration)
///                 .border(Color.red)
///         }
///     }
///
public init(_ configuration: ControlGroupStyleConfiguration) { fatalError() }
}

/// The properties of a control group.
@available(iOS 15.0, macOS 12.0, tvOS 17.0, *)
@available(watchOS, unavailable)
public struct ControlGroupStyleConfiguration {

/// A type-erased content of a `ControlGroup`.
public struct Content : View {

/// The type of view representing the body of this view.
///
/// When you create a custom view, Swift infers this type from your
/// implementation of the required ``View/body-swift.property`` property.
public typealias Body = NeverView
public var body: Body { fatalError() }
}

/// A view that represents the content of the `ControlGroup`.
public let content: ControlGroupStyleConfiguration.Content = { fatalError() }()

/// A type-erased label of a ``ControlGroup``.
@available(iOS 16.0, macOS 13.0, *)
public struct Label : View {

/// The type of view representing the body of this view.
///
/// When you create a custom view, Swift infers this type from your
/// implementation of the required ``View/body-swift.property`` property.
public typealias Body = NeverView
public var body: Body { fatalError() }
}

/// A view that provides the optional label of the ``ControlGroup``.
@available(iOS 16.0, macOS 13.0, *)
public let label: ControlGroupStyleConfiguration.Label = { fatalError() }()
}
*/
