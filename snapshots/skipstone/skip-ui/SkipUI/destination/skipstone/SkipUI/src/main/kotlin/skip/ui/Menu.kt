package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Use a class to avoid copying so that we can update our toggleMenu action on the current instance
@androidx.annotation.Keep
class Menu: View, Renderable, skip.lib.SwiftProjecting {
    internal val content: ComposeBuilder
    internal val label: ComposeBuilder
    internal val primaryAction: (() -> Unit)?
    internal var toggleMenu: () -> Unit = { ->  }

    constructor(content: () -> View, label: () -> View) {
        this.content = ComposeBuilder.from(content)
        this.label = ComposeBuilder(view = Button(action = { -> this.toggleMenu() }, label = label))
        this.primaryAction = null
    }

    constructor(titleKey: LocalizedStringKey, content: () -> View): this(content = content, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleKey).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleResource: LocalizedStringResource, content: () -> View): this(content = content, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleResource).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(title: String, content: () -> View): this(content = content, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(verbatim = title).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(content: () -> View, label: () -> View, primaryAction: () -> Unit) {
        this.content = ComposeBuilder.from(content)
        // We don't use a Button because we can't attach a long press detector to it
        // So currently, any Menu with a primaryAction ignores .buttonStyle
        this.label = ComposeBuilder.from(label)
        this.primaryAction = primaryAction
    }

    constructor(titleKey: LocalizedStringKey, content: () -> View, primaryAction: () -> Unit): this(content = content, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleKey).Compose(composectx)
            ComposeResult.ok
        }
    }, primaryAction = primaryAction) {
    }

    constructor(titleResource: LocalizedStringResource, content: () -> View, primaryAction: () -> Unit): this(content = content, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleResource).Compose(composectx)
            ComposeResult.ok
        }
    }, primaryAction = primaryAction) {
    }

    constructor(title: String, content: () -> View, primaryAction: () -> Unit): this(content = content, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(verbatim = title).Compose(composectx)
            ComposeResult.ok
        }
    }, primaryAction = primaryAction) {
    }

    constructor(bridgedContent: View, bridgedLabel: View, primaryAction: (() -> Unit)?) {
        this.content = ComposeBuilder.from { -> bridgedContent }
        if (primaryAction != null) {
            this.label = ComposeBuilder.from { -> bridgedLabel }
            this.primaryAction = primaryAction
        } else {
            this.label = ComposeBuilder(view = Button(action = { -> this.toggleMenu() }, label = { ->
                ComposeBuilder { composectx: ComposeContext ->
                    bridgedLabel.Compose(composectx)
                    ComposeResult.ok
                }
            }))
            this.primaryAction = null
        }
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val contentContext = context.content()
        val isEnabled = EnvironmentValues.shared.isEnabled
        ComposeContainer(eraseAxis = true, modifier = context.modifier) { modifier ->
            Box(modifier = modifier) { ->
                val matchtarget_0 = primaryAction
                if (matchtarget_0 != null) {
                    val primaryAction = matchtarget_0
                    val primaryActionModifier = Modifier.combinedClickable(enabled = isEnabled, onLongClick = { -> toggleMenu() }, onClick = primaryAction)
                    Button.RenderTextButton(label = label, context = context.content(modifier = primaryActionModifier))
                } else {
                    label.Compose(context = contentContext)
                }
                if (isEnabled) {
                    toggleMenu = Companion.RenderDropdownMenu(content = content, context = contentContext)
                } else {
                    toggleMenu = { ->  }
                }
            }
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {

        @Composable
        internal fun RenderDropdownMenu(content: ComposeBuilder, context: ComposeContext): () -> Unit {
            // We default to displaying our own content, but if the user selects a nested menu we can present
            // that instead. The nested menu selection is cleared on dismiss
            val isMenuExpanded = remember { -> mutableStateOf(false) }
            val nestedMenu = remember { -> mutableStateOf<Menu?>(null) }
            val coroutineScope = rememberCoroutineScope()
            val toggleMenu = { ->
                nestedMenu.value = null
                isMenuExpanded.value = !isMenuExpanded.value
            }
            val replaceMenu: (Menu?) -> Unit = { menu ->
                coroutineScope.launch { ->
                    delay(200) // Allow menu item selection animation to be visible
                    isMenuExpanded.value = false
                    delay(100) // Otherwise we see a flash of the primary menu on nested menu dismiss
                    nestedMenu.value = null
                    if (menu != null) {
                        nestedMenu.value = menu
                        isMenuExpanded.value = true
                    }
                }
            }
            DropdownMenu(expanded = isMenuExpanded.value, onDismissRequest = { ->
                isMenuExpanded.value = false
                coroutineScope.launch { ->
                    delay(100) // Otherwise we see a flash of the primary menu on nested menu dismiss
                    nestedMenu.value = null
                }
            }) { ->
                var placement = EnvironmentValues.shared._placement.sref()
                EnvironmentValues.shared.setValues(l@{ it ->
                    placement.remove(ViewPlacement.toolbar) // Menus popovers are displayed outside the toolbar context
                    it.set_placement(placement)
                    return@l ComposeResult.ok
                }, in_ = { ->
                    val renderables = (nestedMenu.value?.content ?: content).Evaluate(context = context, options = 0)
                    Companion.RenderDropdownMenuItems(for_ = renderables, context = context, replaceMenu = replaceMenu)
                })
            }
            return toggleMenu
        }

        @Composable
        internal fun RenderDropdownMenuItems(for_: kotlin.collections.List<Renderable>, selection: Hashable? = null, context: ComposeContext, replaceMenu: (Menu?) -> Unit) {
            val renderables = for_
            for (renderable in renderables.sref()) {
                var stripped = renderable.strip()
                val matchtarget_1 = stripped as? ShareLink
                if (matchtarget_1 != null) {
                    val shareLink = matchtarget_1
                    shareLink.ComposeAction()
                    stripped = shareLink.content
                } else {
                    (stripped as? Link)?.let { link ->
                        link.ComposeAction()
                        stripped = link.content
                    }
                }
                val matchtarget_2 = stripped as? Button
                if (matchtarget_2 != null) {
                    val button = matchtarget_2
                    val isSelected: Boolean?
                    val matchtarget_3 = TagModifier.on(content = renderable, role = ModifierRole.tag)
                    if (matchtarget_3 != null) {
                        val tagModifier = matchtarget_3
                        isSelected = tagModifier.value == selection
                    } else {
                        isSelected = null
                    }
                    RenderDropdownMenuItem(for_ = button.label, context = context, isSelected = isSelected) { ->
                        button.action()
                        replaceMenu(null)
                    }
                } else {
                    val matchtarget_4 = stripped as? Text
                    if (matchtarget_4 != null) {
                        val text = matchtarget_4
                        DropdownMenuItem(text = { -> text.Render(context = context) }, onClick = { ->  }, enabled = false)
                    } else {
                        val matchtarget_5 = stripped as? Section
                        if (matchtarget_5 != null) {
                            val section = matchtarget_5
                            section.header?.let { header ->
                                DropdownMenuItem(text = { -> header.Compose(context = context) }, onClick = { ->  }, enabled = false)
                            }
                            val sectionRenderables = section.content.Evaluate(context = context, options = 0)
                            RenderDropdownMenuItems(for_ = sectionRenderables, context = context, replaceMenu = replaceMenu)
                            Divider().Compose(context = context)
                        } else {
                            val matchtarget_6 = stripped as? Menu
                            if (matchtarget_6 != null) {
                                val menu = matchtarget_6
                                (menu.label.Evaluate(context = context, options = 0).firstOrNull()?.strip() as? Button)?.let { button ->
                                    RenderDropdownMenuItem(for_ = button.label, context = context) { -> replaceMenu(menu) }
                                }
                            } else {
                                // Dividers are also supported... maybe other view types?
                                renderable.Render(context = context)
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun RenderDropdownMenuItem(for_: ComposeBuilder, context: ComposeContext, isSelected: Boolean? = null, action: () -> Unit) {
            val view = for_
            val renderables = view.Evaluate(context = context, options = 0)
            val label = renderables.firstOrNull()?.strip() as? Label
            if (isSelected != null) {
                val selectedIcon: @Composable () -> Unit
                if (isSelected) {
                    selectedIcon = { -> Icon(imageVector = Icons.Outlined.Check, contentDescription = "selected") }
                } else {
                    selectedIcon = { ->  }
                }
                if (label != null) {
                    DropdownMenuItem(text = { -> label.RenderTitle(context = context) }, leadingIcon = selectedIcon, trailingIcon = { -> label.RenderImage(context = context) }, onClick = action)
                } else {
                    DropdownMenuItem(text = { ->
                        for (renderable in renderables.sref()) {
                            renderable.Render(context = context)
                        }
                    }, leadingIcon = selectedIcon, onClick = action)
                }
            } else if (label != null) {
                DropdownMenuItem(text = { -> label.RenderTitle(context = context) }, trailingIcon = { -> label.RenderImage(context = context) }, onClick = action)
            } else {
                DropdownMenuItem(text = { ->
                    for (renderable in renderables.sref()) {
                        renderable.Render(context = context)
                    }
                }, onClick = action)
            }
        }
    }
}

class MenuStyle: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is MenuStyle) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = MenuStyle(rawValue = 0) // For bridging
        val button = MenuStyle(rawValue = 1) // For bridging
    }
}

class MenuActionDismissBehavior: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is MenuActionDismissBehavior) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = MenuActionDismissBehavior(rawValue = 0) // For bridging
        val enabled = MenuActionDismissBehavior(rawValue = 0) // For bridging
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val disabled = MenuActionDismissBehavior(rawValue = 1) // For bridging
    }
}

class MenuOrder: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is MenuOrder) return false
        return rawValue == other.rawValue
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, rawValue)
        return result
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = MenuOrder(rawValue = 0) // For bridging
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val priority = MenuOrder(rawValue = 1) // For bridging
        val fixed = MenuOrder(rawValue = 2) // For bridging
    }
}

/*
//@available(iOS 14.0, macOS 11.0, tvOS 17.0, *)
//@available(watchOS, unavailable)
//extension Menu where Label == MenuStyleConfiguration.Label, Content == MenuStyleConfiguration.Content {
//
//    /// Creates a menu based on a style configuration.
//    ///
//    /// Use this initializer within the ``MenuStyle/makeBody(configuration:)``
//    /// method of a ``MenuStyle`` instance to create an instance of the menu
//    /// being styled. This is useful for custom menu styles that modify the
//    /// current menu style.
//    ///
//    /// For example, the following code creates a new, custom style that adds a
//    /// red border around the current menu style:
//    ///
//    ///     struct RedBorderMenuStyle: MenuStyle {
//    ///         func makeBody(configuration: Configuration) -> some View {
//    ///             Menu(configuration)
//    ///                 .border(Color.red)
//    ///         }
//    ///     }
//    ///
//    public init(_ configuration: MenuStyleConfiguration) { fatalError() }
//}
//
///// A type that applies standard interaction behavior and a custom appearance
///// to all menus within a view hierarchy.
/////
///// To configure the current menu style for a view hierarchy, use the
///// ``View/menuStyle(_:)`` modifier.
//@available(iOS 14.0, macOS 11.0, tvOS 17.0, *)
//@available(watchOS, unavailable)
//public protocol MenuStyle {
//
//    /// A view that represents the body of a menu.
//    associatedtype Body : View
//
//    /// Creates a view that represents the body of a menu.
//    ///
//    /// - Parameter configuration: The properties of the menu.
//    ///
//    /// The system calls this method for each ``Menu`` instance in a view
//    /// hierarchy where this style is the current menu style.
//    @ViewBuilder func makeBody(configuration: Self.Configuration) -> Self.Body
//
//    /// The properties of a menu.
//    typealias Configuration = MenuStyleConfiguration
//}

/// A configuration of a menu.
///
/// Use the ``Menu/init(_:)`` initializer of ``Menu`` to create an
/// instance using the current menu style, which you can modify to create a
/// custom style.
///
/// For example, the following code creates a new, custom style that adds a red
/// border to the current menu style:
///
///     struct RedBorderMenuStyle: MenuStyle {
///         func makeBody(configuration: Configuration) -> some View {
///             Menu(configuration)
///                 .border(Color.red)
///         }
///     }
@available(iOS 14.0, macOS 11.0, tvOS 17.0, *)
@available(watchOS, unavailable)
public struct MenuStyleConfiguration {

/// A type-erased label of a menu.
public struct Label : View {

/// The type of view representing the body of this view.
///
/// When you create a custom view, Swift infers this type from your
/// implementation of the required ``View/body-swift.property`` property.
public typealias Body = NeverView
public var body: Body { fatalError() }
}

/// A type-erased content of a menu.
public struct Content : View {

/// The type of view representing the body of this view.
///
/// When you create a custom view, Swift infers this type from your
/// implementation of the required ``View/body-swift.property`` property.
public typealias Body = NeverView
public var body: Body { fatalError() }
}
}
*/
