package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

interface ToolbarContent: View {
}

interface CustomizableToolbarContent: ToolbarContent {

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun defaultCustomization(defaultVisibility: Visibility = Visibility.automatic, options: ToolbarCustomizationOptions = ToolbarCustomizationOptions.of()): CustomizableToolbarContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun customizationBehavior(behavior: ToolbarCustomizationBehavior): CustomizableToolbarContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun sharedBackgroundVisibility(visibility: Visibility): CustomizableToolbarContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun matchedTransitionSource(id: Hashable, in_: Namespace.ID): CustomizableToolbarContent {
        val namespace = in_
        return this.sref()
    }
}

// We base our toolbar content on `View` rather than a custom protocol so that we can reuse the
// `@ViewBuilder` logic built into the transpiler. The Swift compiler will guarantee that the
// only allowed toolbar content are types that conform to `ToolbarContent`

@androidx.annotation.Keep
@Suppress("MUST_BE_INITIALIZED")
class ToolbarItem: View, Renderable, CustomizableToolbarContent, MutableStruct, skip.lib.SwiftProjecting {
    internal var placement: ToolbarItemPlacement
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    internal val content: ComposeBuilder

    constructor(placement: ToolbarItemPlacement = ToolbarItemPlacement.automatic, content: () -> View) {
        this.placement = placement
        this.content = ComposeBuilder.from(content)
    }

    constructor(id: String, placement: ToolbarItemPlacement = ToolbarItemPlacement.automatic, content: () -> View) {
        this.placement = placement
        this.content = ComposeBuilder.from(content)
    }

    constructor(id: String, bridgedPlacement: Int, bridgedContent: View) {
        this.placement = ToolbarItemPlacement(rawValue = bridgedPlacement)
        this.content = ComposeBuilder.from { -> bridgedContent }
    }

    @Composable
    override fun Render(context: ComposeContext) {
        EnvironmentValues.shared.setValues(l@{ it ->
            if (placement == ToolbarItemPlacement.confirmationAction) {
                var textEnvironment = it._textEnvironment.sref()
                textEnvironment.fontWeight = Font.Weight.bold
                it.set_textEnvironment(textEnvironment)
            }
            return@l ComposeResult.ok
        }, in_ = { -> content.Compose(context = context) })
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as ToolbarItem
        this.placement = copy.placement
        this.content = copy.content
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = ToolbarItem(this as MutableStruct)

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

class DefaultToolbarItem: View, ToolbarContent {
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(kind: ToolbarDefaultItemKind, placement: ToolbarItemPlacement = ToolbarItemPlacement.automatic) {
    }


    @androidx.annotation.Keep
    companion object {
    }
}

@androidx.annotation.Keep
class ToolbarItemGroup: CustomizableToolbarContent, View, skip.lib.SwiftProjecting {
    internal val placement: ToolbarItemPlacement
    internal val content: ComposeBuilder

    constructor(placement: ToolbarItemPlacement = ToolbarItemPlacement.automatic, content: () -> View) {
        this.placement = placement
        this.content = ComposeBuilder.from(content)
    }

    constructor(placement: ToolbarItemPlacement = ToolbarItemPlacement.automatic, content: () -> View, label: () -> View) {
        this.placement = placement
        this.content = ComposeBuilder.from(content)
    }

    constructor(bridgedPlacement: Int, bridgedContent: View) {
        this.placement = ToolbarItemPlacement(rawValue = bridgedPlacement)
        this.content = ComposeBuilder.from { -> bridgedContent }
    }

    @Composable
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val renderables = EnvironmentValues.shared.setValuesWithReturn(l@{ it ->
            if (placement == ToolbarItemPlacement.confirmationAction) {
                var textEnvironment = it._textEnvironment.sref()
                textEnvironment.fontWeight = Font.Weight.bold
                it.set_textEnvironment(textEnvironment)
            }
            return@l ComposeResult.ok
        }, in_ = l@{ -> return@l content.Evaluate(context = context, options = options) })
        return renderables.map l@{ it ->
            val renderable = (it as Renderable).sref() // Tell transpiler the type
            val matchtarget_0 = renderable as? ToolbarItem
            if (matchtarget_0 != null) {
                var toolbarItem = matchtarget_0
                if (toolbarItem.placement == ToolbarItemPlacement.automatic) {
                    toolbarItem.placement = placement
                }
                return@l toolbarItem
            } else if (renderable is Spacer) {
                return@l ToolbarSpacer(placement = placement)
            } else {
                return@l ToolbarItem(placement = placement, content = { ->
                    ComposeBuilder { composectx: ComposeContext ->
                        renderable.asView().Compose(composectx)
                        ComposeResult.ok
                    }
                })
            }
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

@androidx.annotation.Keep
class ToolbarTitleMenu: CustomizableToolbarContent, View, Renderable, skip.lib.SwiftProjecting {
    internal val content: ComposeBuilder
    internal var toggleMenu: () -> Unit = { ->  }

    constructor() {
        this.content = ComposeBuilder(view = EmptyView())
    }

    constructor(content: () -> View) {
        this.content = ComposeBuilder.from(content)
    }

    constructor(bridgedContent: View) {
        this.content = ComposeBuilder.from({ -> bridgedContent })
    }

    @Composable
    override fun Render(context: ComposeContext) {
        this.toggleMenu = Menu.RenderDropdownMenu(content = content, context = context)
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

@androidx.annotation.Keep
class ToolbarSpacer: ToolbarContent, CustomizableToolbarContent, View, Renderable, skip.lib.SwiftProjecting {
    internal val sizing: SpacerSizing
    internal val placement: ToolbarItemPlacement

    constructor(sizing: SpacerSizing = SpacerSizing.flexible, placement: ToolbarItemPlacement = ToolbarItemPlacement.automatic) {
        this.sizing = sizing
        this.placement = placement
    }

    constructor(bridgedSizing: Int, bridgedPlacement: Int) {
        this.sizing = SpacerSizing(rawValue = bridgedSizing) ?: SpacerSizing.flexible
        this.placement = ToolbarItemPlacement(rawValue = bridgedPlacement)
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val modifier: Modifier
        if (sizing == SpacerSizing.fixed) {
            modifier = Modifier.width(8.dp)
        } else {
            modifier = EnvironmentValues.shared._flexibleWidth?.invoke(null, null, Float.flexibleSpace) ?: Modifier
        }
        androidx.compose.foundation.layout.Spacer(modifier = modifier)
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

enum class ToolbarCustomizationBehavior {
    default,
    reorderable,
    disabled;

    @androidx.annotation.Keep
    companion object {
    }
}

class ToolbarItemPlacement: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ToolbarItemPlacement) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = ToolbarItemPlacement(rawValue = 0) // For bridging
        val principal = ToolbarItemPlacement(rawValue = 1) // For bridging
        val navigation = ToolbarItemPlacement(rawValue = 2) // For bridging
        val primaryAction = ToolbarItemPlacement(rawValue = 3) // For bridging
        val secondaryAction = ToolbarItemPlacement(rawValue = 4) // For bridging
        val status = ToolbarItemPlacement(rawValue = 5) // For bridging
        val confirmationAction = ToolbarItemPlacement(rawValue = 6) // For bridging
        val cancellationAction = ToolbarItemPlacement(rawValue = 7) // For bridging
        val destructiveAction = ToolbarItemPlacement(rawValue = 8) // For bridging
        val keyboard = ToolbarItemPlacement(rawValue = 9) // For bridging
        val topBarLeading = ToolbarItemPlacement(rawValue = 10) // For bridging
        val topBarTrailing = ToolbarItemPlacement(rawValue = 11) // For bridging
        val bottomBar = ToolbarItemPlacement(rawValue = 12) // For bridging
        val navigationBarLeading = ToolbarItemPlacement(rawValue = 13) // For bridging
        val navigationBarTrailing = ToolbarItemPlacement(rawValue = 14) // For bridging
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val title = ToolbarItemPlacement(rawValue = 15) // For bridging
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val largeTitle = ToolbarItemPlacement(rawValue = 16) // For bridging
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val subtitle = ToolbarItemPlacement(rawValue = 17) // For bridging
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val largeSubtitle = ToolbarItemPlacement(rawValue = 18) // For bridging
    }
}

enum class ToolbarPlacement(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
    automatic(0), // For bridging
    bottomBar(1), // For bridging
    navigationBar(2), // For bridging
    tabBar(3); // For bridging

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: Int): ToolbarPlacement? {
            return when (rawValue) {
                0 -> ToolbarPlacement.automatic
                1 -> ToolbarPlacement.bottomBar
                2 -> ToolbarPlacement.navigationBar
                3 -> ToolbarPlacement.tabBar
                else -> null
            }
        }
    }
}

fun ToolbarPlacement(rawValue: Int): ToolbarPlacement? = ToolbarPlacement.init(rawValue = rawValue)

enum class ToolbarRole {
    automatic,
    navigationStack,
    browser,
    editor;

    @androidx.annotation.Keep
    companion object {
    }
}

enum class ToolbarTitleDisplayMode(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
    automatic(0), // For bridging
    large(1), // For bridging
    inlineLarge(2), // For bridging
    inline_(3); // For bridging

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: Int): ToolbarTitleDisplayMode? {
            return when (rawValue) {
                0 -> ToolbarTitleDisplayMode.automatic
                1 -> ToolbarTitleDisplayMode.large
                2 -> ToolbarTitleDisplayMode.inlineLarge
                3 -> ToolbarTitleDisplayMode.inline_
                else -> null
            }
        }
    }
}

fun ToolbarTitleDisplayMode(rawValue: Int): ToolbarTitleDisplayMode? = ToolbarTitleDisplayMode.init(rawValue = rawValue)

class ToolbarCustomizationOptions: OptionSet<ToolbarCustomizationOptions, Int>, MutableStruct {
    override var rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override val rawvaluelong: ULong
        get() = ULong(rawValue)
    override fun makeoptionset(rawvaluelong: ULong): ToolbarCustomizationOptions = ToolbarCustomizationOptions(rawValue = Int(rawvaluelong))
    override fun assignoptionset(target: ToolbarCustomizationOptions) {
        willmutate()
        try {
            assignfrom(target)
        } finally {
            didmutate()
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as ToolbarCustomizationOptions
        this.rawValue = copy.rawValue
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = ToolbarCustomizationOptions(this as MutableStruct)

    private fun assignfrom(target: ToolbarCustomizationOptions) {
        this.rawValue = target.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        var alwaysAvailable = ToolbarCustomizationOptions(rawValue = 1 shl 0)
            get() = field.sref({ this.alwaysAvailable = it })
            set(newValue) {
                field = newValue.sref()
            }

        fun of(vararg options: ToolbarCustomizationOptions): ToolbarCustomizationOptions {
            val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
            return ToolbarCustomizationOptions(rawValue = value)
        }
    }
}

class ToolbarDefaultItemKind: RawRepresentable<Int> {
    override val rawValue: Int // For bridging

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val sidebarToggle = ToolbarDefaultItemKind(rawValue = 1) // For bridging
        val title = ToolbarDefaultItemKind(rawValue = 2) // For bridging
        val search = ToolbarDefaultItemKind(rawValue = 3) // For bridging
    }
}

@androidx.annotation.Keep
internal class ToolbarPreferenceKey: PreferenceKey<ToolbarPreferences> {

    @androidx.annotation.Keep
    companion object: PreferenceKeyCompanion<ToolbarPreferences> {
        override val defaultValue = ToolbarPreferences()

        override fun reduce(value: InOut<ToolbarPreferences>, nextValue: () -> ToolbarPreferences) {
            value.value = value.value.reduce(nextValue())
        }
    }
}

internal class ToolbarPreferences {
    internal val titleDisplayMode: ToolbarTitleDisplayMode?
    internal val backButtonHidden: Boolean?
    internal val navigationBar: ToolbarBarPreferences?
    internal val bottomBar: ToolbarBarPreferences?

    internal constructor(titleDisplayMode: ToolbarTitleDisplayMode? = null, backButtonHidden: Boolean? = null, navigationBar: ToolbarBarPreferences? = null, bottomBar: ToolbarBarPreferences? = null) {
        this.titleDisplayMode = titleDisplayMode
        this.backButtonHidden = backButtonHidden
        this.navigationBar = navigationBar
        this.bottomBar = bottomBar
    }

    internal constructor(visibility: Visibility? = null, background: ShapeStyle? = null, backgroundVisibility: Visibility? = null, colorScheme: ColorScheme? = null, isSystemBackground: Boolean? = null, scrollableState: ScrollableState? = null, for_: Array<ToolbarPlacement>) {
        val bars = for_
        val barPreferences = ToolbarBarPreferences(visibility = visibility, background = background, backgroundVisibility = backgroundVisibility, colorScheme = colorScheme, isSystemBackground = isSystemBackground, scrollableState = scrollableState)
        var navigationBar: ToolbarBarPreferences? = null
        var bottomBar: ToolbarBarPreferences? = null
        for (bar in bars.sref()) {
            for (unusedi in 0..0) {
                when (bar) {
                    ToolbarPlacement.automatic, ToolbarPlacement.navigationBar -> navigationBar = barPreferences
                    ToolbarPlacement.bottomBar -> bottomBar = barPreferences
                    ToolbarPlacement.tabBar -> break
                }
            }
        }
        this.navigationBar = navigationBar
        this.bottomBar = bottomBar
        this.titleDisplayMode = null
        this.backButtonHidden = null
    }

    internal fun reduce(next: ToolbarPreferences): ToolbarPreferences = ToolbarPreferences(titleDisplayMode = next.titleDisplayMode ?: titleDisplayMode, backButtonHidden = next.backButtonHidden ?: backButtonHidden, navigationBar = reduceBar(navigationBar, next.navigationBar), bottomBar = reduceBar(bottomBar, next.bottomBar))

    private fun reduceBar(bar: ToolbarBarPreferences?, next: ToolbarBarPreferences?): ToolbarBarPreferences? {
        if ((bar != null) && (next != null)) {
            return bar.reduce(next)
        } else {
            return next ?: bar
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ToolbarPreferences) {
            return false
        }
        val lhs = this
        val rhs = other
        return lhs.titleDisplayMode == rhs.titleDisplayMode && lhs.backButtonHidden == rhs.backButtonHidden && lhs.navigationBar == rhs.navigationBar && lhs.bottomBar == rhs.bottomBar
    }
}

internal class ToolbarBarPreferences {
    internal val visibility: Visibility?
    internal val background: ShapeStyle?
    internal val backgroundVisibility: Visibility?
    internal val colorScheme: ColorScheme?
    internal val isSystemBackground: Boolean?
    internal val scrollableState: ScrollableState?

    internal fun reduce(next: ToolbarBarPreferences): ToolbarBarPreferences = ToolbarBarPreferences(visibility = next.visibility ?: visibility, background = next.background ?: background, backgroundVisibility = next.backgroundVisibility ?: backgroundVisibility, colorScheme = next.colorScheme ?: colorScheme, isSystemBackground = next.isSystemBackground ?: isSystemBackground, scrollableState = next.scrollableState ?: scrollableState)

    override fun equals(other: Any?): Boolean {
        if (other !is ToolbarBarPreferences) {
            return false
        }
        val lhs = this
        val rhs = other
        // Don't compare on background because it will never compare equal
        return lhs.visibility == rhs.visibility && lhs.backgroundVisibility == rhs.backgroundVisibility && (lhs.background != null) == (rhs.background != null) && lhs.colorScheme == rhs.colorScheme && lhs.isSystemBackground == rhs.isSystemBackground && lhs.scrollableState == rhs.scrollableState
    }

    constructor(visibility: Visibility? = null, background: ShapeStyle? = null, backgroundVisibility: Visibility? = null, colorScheme: ColorScheme? = null, isSystemBackground: Boolean? = null, scrollableState: ScrollableState? = null) {
        this.visibility = visibility
        this.background = background.sref()
        this.backgroundVisibility = backgroundVisibility
        this.colorScheme = colorScheme
        this.isSystemBackground = isSystemBackground
        this.scrollableState = scrollableState.sref()
    }
}

@androidx.annotation.Keep
internal class ToolbarContentPreferenceKey: PreferenceKey<ToolbarContentPreferences> {

    @androidx.annotation.Keep
    companion object: PreferenceKeyCompanion<ToolbarContentPreferences> {
        override val defaultValue = ToolbarContentPreferences()

        override fun reduce(value: InOut<ToolbarContentPreferences>, nextValue: () -> ToolbarContentPreferences) {
            value.value = value.value.reduce(nextValue())
        }
    }
}

internal class ToolbarContentPreferences {
    internal val content: Array<View>?

    internal constructor(content: Array<View>? = null) {
        this.content = content.sref()
    }

    internal fun reduce(next: ToolbarContentPreferences): ToolbarContentPreferences {
        val rcontent: Array<View>?
        val matchtarget_1 = next.content
        if (matchtarget_1 != null) {
            val ncontent = matchtarget_1
            if (content != null) {
                rcontent = (content + ncontent).sref()
            } else {
                rcontent = (next.content ?: content).sref()
            }
        } else {
            rcontent = (next.content ?: content).sref()
        }
        return ToolbarContentPreferences(content = rcontent)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ToolbarContentPreferences) {
            return false
        }
        val lhs = this
        val rhs = other
        // Views are not going to compare equal most of the time, even if they are logically the same.
        // That's why we isolate content from other preferences, so we can only access it in the bars themselves
        return lhs.content == rhs.content
    }
}

internal class ToolbarItems {
    internal val content: Array<View>

    /// Proces our content items, dividing them into the locations at which they should render.
    @Composable
    internal fun Evaluate(context: ComposeContext): Tuple4<ToolbarTitleMenu?, kotlin.collections.List<Renderable>, kotlin.collections.List<Renderable>, kotlin.collections.List<Renderable>> {
        var titleMenu: ToolbarTitleMenu? = null
        val leading: kotlin.collections.MutableList<Renderable> = mutableListOf()
        val trailing: kotlin.collections.MutableList<Renderable> = mutableListOf()
        var principal: Renderable? = null
        val bottom: kotlin.collections.MutableList<Renderable> = mutableListOf()
        for (view in content.sref()) {
            val renderables = view.Evaluate(context = context, options = 0)
            for (renderable in renderables.sref()) {
                val matchtarget_2 = renderable as? ToolbarTitleMenu
                if (matchtarget_2 != null) {
                    val menu = matchtarget_2
                    titleMenu = menu
                } else {
                    val placement = (renderable as? ToolbarItem)?.placement ?: (renderable as? ToolbarSpacer)?.placement ?: ToolbarItemPlacement.automatic
                    when (placement) {
                        ToolbarItemPlacement.principal -> principal = renderable.sref()
                        ToolbarItemPlacement.topBarLeading, ToolbarItemPlacement.navigationBarLeading, ToolbarItemPlacement.cancellationAction -> leading.add(renderable)
                        ToolbarItemPlacement.bottomBar -> bottom.add(renderable)
                        else -> trailing.add(renderable)
                    }
                }
            }
        }
        if (principal != null) {
            leading.add(principal)
        }
        // SwiftUI inserts a spacer before the last bottom item
        if (bottom.size > 1 && !bottom.any(l@{ it ->
            val stripped = it.strip()
            return@l stripped is Spacer || stripped is ToolbarSpacer
        })) {
            bottom.add(1, Spacer())
        }
        return Tuple4(titleMenu, leading.sref(), trailing.sref(), bottom.sref())
    }

    constructor(content: Array<View>) {
        this.content = content.sref()
    }
}

/*
/// A built-in set of commands for manipulating window toolbars.
///
/// These commands are optional and can be explicitly requested by passing a
/// value of this type to the ``Scene/commands(content:)`` modifier.
@available(iOS 14.0, macOS 11.0, *)
@available(tvOS, unavailable)
@available(watchOS, unavailable)
public struct ToolbarCommands : Commands {

/// A new value describing the built-in toolbar-related commands.
public init() { fatalError() }

/// The contents of the command hierarchy.
///
/// For any commands that you create, provide a computed `body` property
/// that defines the scene as a composition of other scenes. You can
/// assemble a command hierarchy from built-in commands that SkipUI
/// provides, as well as other commands that you've defined.
public var body: some Commands { get { return stubCommands() } }

/// The type of commands that represents the body of this command hierarchy.
///
/// When you create custom commands, Swift infers this type from your
/// implementation of the required ``SkipUI/Commands/body-swift.property``
/// property.
//public typealias Body = NeverView
}

//@available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
//extension Group : ToolbarContent where Content : ToolbarContent {
//    /// Creates a group of toolbar content instances.
//    ///
//    /// - Parameter content: A ``SkipUI/ToolbarContentBuilder`` that produces
//    /// the toolbar content instances to group.
//    public init(@ToolbarContentBuilder content: () -> Content) { fatalError() }
//
//    //public typealias Body = NeverView
//    public var body: some ToolbarContent { return stubToolbarContent() }
//}

//@available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
//extension Group : CustomizableToolbarContent where Content : CustomizableToolbarContent {
//
//    /// Creates a group of customizable toolbar content instances.
//    ///
//    /// - Parameter content: A ``SkipUI/ToolbarContentBuilder`` that produces
//    /// the customizable toolbar content instances to group.
//    public init(@ToolbarContentBuilder content: () -> Content) { fatalError() }
//
//    //public typealias Body = NeverView
//    public var body: some CustomizableToolbarContent { stubToolbar() }
//
//}
*/
