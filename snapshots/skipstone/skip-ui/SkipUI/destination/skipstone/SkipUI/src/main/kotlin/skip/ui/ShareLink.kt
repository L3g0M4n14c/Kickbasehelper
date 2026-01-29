package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.startActivity

// Use a class to be able to update our openURL action on compose by reference.
@androidx.annotation.Keep
class ShareLink: View, Renderable, skip.lib.SwiftProjecting {

    internal val text: String
    internal val subject: Text?
    internal val message: Text?
    internal val content: Button
    internal var action: () -> Unit

    internal constructor(text: String, subject: Text? = null, message: Text? = null, label: () -> View, unusedp: Any? = null) {
        this.text = text
        this.subject = subject
        this.message = message
        this.action = { ->  }
        this.content = Button(action = { -> this.action() }, label = label)
    }

    constructor(text: String, subject: Text?, message: Text?, bridgedLabel: View?) {
        this.text = text
        this.subject = subject
        this.message = message
        this.action = { ->  }
        val label: () -> View = if (bridgedLabel == null) { -> Image(systemName = Companion.defaultSystemImageName) } else { -> bridgedLabel!! }
        this.content = Button(action = { -> this.action() }, label = label)
    }

    constructor(item: URL, subject: Text? = null, message: Text? = null, label: () -> View): this(text = item.absoluteString, subject = subject, message = message, label = label) {
    }

    constructor(item: String, subject: Text? = null, message: Text? = null, label: () -> View): this(text = item, subject = subject, message = message, label = label) {
    }

    constructor(item: URL, subject: Text? = null, message: Text? = null): this(text = item.absoluteString, subject = subject, message = message, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Image(systemName = Companion.defaultSystemImageName).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(item: String, subject: Text? = null, message: Text? = null): this(text = item, subject = subject, message = message, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Image(systemName = Companion.defaultSystemImageName).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleKey: LocalizedStringKey, item: URL, subject: Text? = null, message: Text? = null): this(text = item.absoluteString, subject = subject, message = message, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Label(titleKey, systemImage = Companion.defaultSystemImageName).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleResource: LocalizedStringResource, item: URL, subject: Text? = null, message: Text? = null): this(text = item.absoluteString, subject = subject, message = message, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Label(titleResource, systemImage = Companion.defaultSystemImageName).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleKey: LocalizedStringKey, item: String, subject: Text? = null, message: Text? = null): this(text = item, subject = subject, message = message, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Label(titleKey, systemImage = Companion.defaultSystemImageName).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleResource: LocalizedStringResource, item: String, subject: Text? = null, message: Text? = null): this(text = item, subject = subject, message = message, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Label(titleResource, systemImage = Companion.defaultSystemImageName).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(title: String, item: URL, subject: Text? = null, message: Text? = null): this(text = item.absoluteString, subject = subject, message = message, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Label(title, systemImage = Companion.defaultSystemImageName).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(title: String, item: String, subject: Text? = null, message: Text? = null): this(text = item, subject = subject, message = message, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Label(title, systemImage = Companion.defaultSystemImageName).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(title: Text, item: URL, subject: Text? = null, message: Text? = null): this(text = item.absoluteString, subject = subject, message = message, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Label(title = { ->
                ComposeBuilder { composectx: ComposeContext ->
                    title.Compose(composectx)
                    ComposeResult.ok
                }
            }, icon = { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Image(systemName = Companion.defaultSystemImageName).Compose(composectx)
                    ComposeResult.ok
                }
            }).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(title: Text, item: String, subject: Text? = null, message: Text? = null): this(text = item, subject = subject, message = message, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Label(title = { ->
                ComposeBuilder { composectx: ComposeContext ->
                    title.Compose(composectx)
                    ComposeResult.ok
                }
            }, icon = { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Image(systemName = Companion.defaultSystemImageName).Compose(composectx)
                    ComposeResult.ok
                }
            }).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    @Composable
    override fun Render(context: ComposeContext) {
        ComposeAction()
        content.Compose(context = context)
    }

    @Composable
    internal fun ComposeAction() {
        val localContext = LocalContext.current.sref()

        val intent = Intent().apply { ->
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            if (subject != null) {
                putExtra(Intent.EXTRA_SUBJECT, subject.localizedTextString())
            }
            type = "text/plain"
        }

        action = { ->
            val shareIntent = Intent.createChooser(intent, null)
            localContext.startActivity(shareIntent)
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
        private val defaultSystemImageName = "square.and.arrow.up"
    }
}

/*
import protocol CoreTransferable.Transferable
import struct UniformTypeIdentifiers.UTType

@available(iOS 16.0, macOS 13.0, watchOS 9.0, *)
@available(tvOS, unavailable)
extension ShareLink {

/// Creates an instance that presents the share interface.
///
/// - Parameters:
///     - item: The item to share.
///     - subject: A title for the item to show when sharing to activities
///     that support a subject field.
///     - message: A description of the item to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
///     - preview: A representation of the item to render in a preview.
///     - label: A view builder that produces a label that describes the
///     share action.
public init<I>(item: I, subject: Text? = nil, message: Text? = nil, preview: SharePreview<PreviewImage, PreviewIcon>, @ViewBuilder label: () -> Label) where Data == CollectionOfOne<I>, I : Transferable { fatalError() }
}


@available(iOS 16.0, macOS 13.0, watchOS 9.0, *)
@available(tvOS, unavailable)
extension ShareLink where PreviewImage == Never, PreviewIcon == Never, Data.Element == URL {

/// Creates an instance that presents the share interface.
///
/// - Parameters:
///     - items: The items to share.
///     - subject: A title for the items to show when sharing to activities
///     that support a subject field.
///     - message: A description of the items to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
///     - label: A view builder that produces a label that describes the
///     share action.
public init(items: Data, subject: Text? = nil, message: Text? = nil, @ViewBuilder label: () -> Label) { fatalError() }
}

@available(iOS 16.0, macOS 13.0, watchOS 9.0, *)
@available(tvOS, unavailable)
extension ShareLink where PreviewImage == Never, PreviewIcon == Never, Data.Element == String {

/// Creates an instance that presents the share interface.
///
/// - Parameters:
///     - items: The items to share.
///     - subject: A title for the items to show when sharing to activities
///     that support a subject field.
///     - message: A description of the items to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
///     - label: A view builder that produces a label that describes the
///     share action.
public init(items: Data, subject: Text? = nil, message: Text? = nil, @ViewBuilder label: () -> Label) { fatalError() }
}

@available(iOS 16.0, macOS 13.0, watchOS 9.0, *)
@available(tvOS, unavailable)
extension ShareLink where Label == DefaultShareLinkLabel {

/// Creates an instance that presents the share interface.
///
/// Use this initializer when you want the system-standard appearance for
/// `ShareLink`.
///
/// - Parameters:
///     - items: The items to share.
///     - subject: A title for the items to show when sharing to activities
///     that support a subject field.
///     - message: A description of the items to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
///     - preview: A closure that returns a representation of each item to
///     render in a preview.
public init(items: Data, subject: Text? = nil, message: Text? = nil, preview: @escaping (Data.Element) -> SharePreview<PreviewImage, PreviewIcon>) { fatalError() }

/// Creates an instance, with a custom label, that presents the share
/// interface.
///
/// - Parameters:
///     - titleKey: A key identifying the title of the share action.
///     - items: The items to share.
///     - subject: A title for the items to show when sharing to activities
///     that support a subject field.
///     - message: A description of the items to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
///     - preview: A closure that returns a representation of each item to
///     render in a preview.
public init(_ titleKey: LocalizedStringKey, items: Data, subject: Text? = nil, message: Text? = nil, preview: @escaping (Data.Element) -> SharePreview<PreviewImage, PreviewIcon>) { fatalError() }

/// Creates an instance, with a custom label, that presents the share
/// interface.
///
/// - Parameters:
///     - title: The title of the share action.
///     - items: The item to share.
///     - subject: A title for the items to show when sharing to activities
///     that support a subject field.
///     - message: A description of the items to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
///     - preview: A closure that returns a representation of each item to
///     render in a preview.
public init<S>(_ title: S, items: Data, subject: Text? = nil, message: Text? = nil, preview: @escaping (Data.Element) -> SharePreview<PreviewImage, PreviewIcon>) where S : StringProtocol { fatalError() }

/// Creates an instance, with a custom label, that presents the share
/// interface.
///
/// - Parameters:
///     - title: The title of the share action.
///     - items: The items to share.
///     - subject: A title for the items to show when sharing to activities
///     that support a subject field.
///     - message: A description of the items to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
///     - preview: A closure that returns a representation of each item to
///     render in a preview.
public init(_ title: Text, items: Data, subject: Text? = nil, message: Text? = nil, preview: @escaping (Data.Element) -> SharePreview<PreviewImage, PreviewIcon>) { fatalError() }
}

@available(iOS 16.0, macOS 13.0, watchOS 9.0, *)
@available(tvOS, unavailable)
extension ShareLink where Label == DefaultShareLinkLabel {

/// Creates an instance that presents the share interface.
///
/// Use this initializer when you want the system-standard appearance for
/// `ShareLink`.
///
/// - Parameters:
///     - item: The item to share.
///     - subject: A title for the item to show when sharing to activities
///     that support a subject field.
///     - message: A description of the item to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
///     - preview: A representation of the item to render in a preview.
public init<I>(item: I, subject: Text? = nil, message: Text? = nil, preview: SharePreview<PreviewImage, PreviewIcon>) where Data == CollectionOfOne<I>, I : Transferable { fatalError() }

/// Creates an instance, with a custom label, that presents the share
/// interface.
///
/// - Parameters:
///     - titleKey: A key identifying the title of the share action.
///     - item: The item to share.
///     - subject: A title for the item to show when sharing to activities
///     that support a subject field.
///     - message: A description of the item to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
///     - preview: A representation of the item to render in a preview.
public init<I>(_ titleKey: LocalizedStringKey, item: I, subject: Text? = nil, message: Text? = nil, preview: SharePreview<PreviewImage, PreviewIcon>) where Data == CollectionOfOne<I>, I : Transferable { fatalError() }

/// Creates an instance, with a custom label, that presents the share
/// interface.
///
/// - Parameters:
///     - title: The title of the share action.
///     - item: The item to share.
///     - subject: A title for the item to show when sharing to activities
///     that support a subject field.
///     - message: A description of the item to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
///     - preview: A representation of the item to render in a preview.
public init<S, I>(_ title: S, item: I, subject: Text? = nil, message: Text? = nil, preview: SharePreview<PreviewImage, PreviewIcon>) where Data == CollectionOfOne<I>, S : StringProtocol, I : Transferable { fatalError() }

/// Creates an instance, with a custom label, that presents the share
/// interface.
///
/// - Parameters:
///     - title: The title of the share action.
///     - item: The item to share.
///     - subject: A title for the item to show when sharing to activities
///     that support a subject field.
///     - message: A description of the item to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
///     - preview: A representation of the item to render in a preview.
public init<I>(_ title: Text, item: I, subject: Text? = nil, message: Text? = nil, preview: SharePreview<PreviewImage, PreviewIcon>) where Data == CollectionOfOne<I>, I : Transferable { fatalError() }
}

@available(iOS 16.0, macOS 13.0, watchOS 9.0, *)
@available(tvOS, unavailable)
extension ShareLink where PreviewImage == Never, PreviewIcon == Never, Label == DefaultShareLinkLabel, Data.Element == URL {

/// Creates an instance that presents the share interface.
///
/// Use this initializer when you want the system-standard appearance for
/// `ShareLink`.
///
/// - Parameters:
///     - items: The items to share.
///     - subject: A title for the items to show when sharing to activities
///     that support a subject field.
///     - message: A description of the items to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
public init(items: Data, subject: Text? = nil, message: Text? = nil) { fatalError() }

/// Creates an instance, with a custom label, that presents the share
/// interface.
///
/// - Parameters:
///     - titleKey: A key identifying the title of the share action.
///     - items: The items to share.
///     - subject: A title for the items to show when sharing to activities
///     that support a subject field.
///     - message: A description of the items to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
public init(_ titleKey: LocalizedStringKey, items: Data, subject: Text? = nil, message: Text? = nil) { fatalError() }

/// Creates an instance, with a custom label, that presents the share
/// interface.
///
/// - Parameters:
///     - title: The title of the share action.
///     - items: The item to share.
///     - subject: A title for the items to show when sharing to activities
///     that support a subject field.
///     - message: A description of the items to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
public init<S>(_ title: S, items: Data, subject: Text? = nil, message: Text? = nil) where S : StringProtocol { fatalError() }

/// Creates an instance, with a custom label, that presents the share
/// interface.
///
/// - Parameters:
///     - title: The title of the share action.
///     - items: The items to share.
///     - subject: A title for the items to show when sharing to activities
///     that support a subject field.
///     - message: A description of the items to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
public init(_ title: Text, items: Data, subject: Text? = nil, message: Text? = nil) { fatalError() }
}

@available(iOS 16.0, macOS 13.0, watchOS 9.0, *)
@available(tvOS, unavailable)
extension ShareLink where PreviewImage == Never, PreviewIcon == Never, Label == DefaultShareLinkLabel, Data.Element == String {

/// Creates an instance that presents the share interface.
///
/// Use this initializer when you want the system-standard appearance for
/// `ShareLink`.
///
/// - Parameters:
///     - items: The items to share.
///     - subject: A title for the items to show when sharing to activities
///     that support a subject field.
///     - message: A description of the items to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
public init(items: Data, subject: Text? = nil, message: Text? = nil) { fatalError() }

/// Creates an instance, with a custom label, that presents the share
/// interface.
///
/// - Parameters:
///     - titleKey: A key identifying the title of the share action.
///     - items: The items to share.
///     - subject: A title for the items to show when sharing to activities
///     that support a subject field.
///     - message: A description of the items to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
public init(_ titleKey: LocalizedStringKey, items: Data, subject: Text? = nil, message: Text? = nil) { fatalError() }

/// Creates an instance, with a custom label, that presents the share
/// interface.
///
/// - Parameters:
///     - title: The title of the share action.
///     - items: The item to share.
///     - subject: A title for the items to show when sharing to activities
///     that support a subject field.
///     - message: A description of the items to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
public init<S>(_ title: S, items: Data, subject: Text? = nil, message: Text? = nil) where S : StringProtocol { fatalError() }

/// Creates an instance, with a custom label, that presents the share
/// interface.
///
/// - Parameters:
///     - title: The title of the share action.
///     - items: The items to share.
///     - subject: A title for the items to show when sharing to activities
///     that support a subject field.
///     - message: A description of the items to show when sharing to
///     activities that support a message field. Activities may
///     support attributed text or HTML strings.
public init(_ title: Text, items: Data, subject: Text? = nil, message: Text? = nil) { fatalError() }
}


/// The default label used for a share link.
///
/// You don't use this type directly. Instead, ``ShareLink`` uses it
/// automatically depending on how you create a share link.
@available(iOS 16.0, macOS 13.0, watchOS 9.0, *)
@available(tvOS, unavailable)
public struct DefaultShareLinkLabel : View {

@MainActor public var body: some View { get { return stubView() } }

//    public typealias Body = some View
}
*/
