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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import skip.model.StateTracking

@androidx.annotation.Keep
class Picker<SelectionValue>: View, Renderable, skip.lib.SwiftProjecting {
    internal val selection: Binding<SelectionValue>
    internal val label: ComposeBuilder
    internal val content: ComposeBuilder
    private var isMenuExpanded: MutableState<Boolean>? = null
        get() = field.sref({ this.isMenuExpanded = it })
        set(newValue) {
            field = newValue.sref()
        }

    constructor(selection: Binding<SelectionValue>, content: () -> View, label: () -> View) {
        this.selection = selection.sref()
        this.content = ComposeBuilder.from(content)
        this.label = ComposeBuilder.from(label)
    }

    constructor(titleKey: LocalizedStringKey, selection: Binding<SelectionValue>, content: () -> View): this(selection = selection, content = content, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleKey).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleResource: LocalizedStringResource, selection: Binding<SelectionValue>, content: () -> View): this(selection = selection, content = content, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleResource).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(title: String, selection: Binding<SelectionValue>, content: () -> View): this(selection = selection, content = content, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(verbatim = title).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(getSelection: () -> SelectionValue, setSelection: (SelectionValue) -> Unit, bridgedContent: View, bridgedLabel: View) {
        this.selection = Binding(get = getSelection, set = setSelection)
        this.content = ComposeBuilder.from { -> bridgedContent }
        this.label = ComposeBuilder.from { -> bridgedLabel }
    }

    @Composable
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        isMenuExpanded = remember { -> mutableStateOf(false) }
        return listOf(this)
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val style = EnvironmentValues.shared._pickerStyle ?: PickerStyle.automatic
        if (style == PickerStyle.segmented) {
            RenderSegmentedValue(context = context)
        } else if (EnvironmentValues.shared._labelsHidden || style != PickerStyle.navigationLink) {
            // Most picker styles do not display their label outside of a Form (see RenderListItem)
            val (selected, tagged) = processPickerContent(content = content, selection = selection, context = context)
            RenderSelectedValue(selectedRenderable = selected, taggedRenderables = tagged, context = context, style = style)
        } else {
            // Navigation link style outside of a List. This style does display its label
            RenderLabeledValue(context = context, style = style)
        }
    }

    @Composable
    private fun RenderLabeledValue(context: ComposeContext, style: PickerStyle) {
        val (selected, tagged) = processPickerContent(content = content, selection = selection, context = context)
        val contentContext = context.content()
        val navigator = LocalNavigator.current.sref()
        val label = ((this.label.Evaluate(context = context, options = 0).firstOrNull() ?: EmptyView()) as Renderable).sref() // Let transpiler understand type
        val title = titleFromLabel(label, context = context)
        val modifier = context.modifier.clickable(onClick = { ->
            navigator?.navigateToView(PickerSelectionView(title = title, content = content, selection = selection))
        }, enabled = EnvironmentValues.shared.isEnabled)
        ComposeContainer(modifier = modifier, fillWidth = true) { modifier ->
            Row(modifier = modifier, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { ->
                Box(modifier = Modifier.padding(end = 8.dp).weight(1.0f)) { -> Button.RenderTextButton(label = label.asView(), context = contentContext) }
                RenderSelectedValue(selectedRenderable = selected, taggedRenderables = tagged, context = contentContext, style = style, performsAction = false)
            }
        }
    }

    @Composable
    private fun RenderSelectedValue(selectedRenderable: Renderable, taggedRenderables: kotlin.collections.List<Renderable>?, context: ComposeContext, style: PickerStyle, performsAction: Boolean = true) {
        val selectedValueRenderable = (selectedRenderable ?: processPickerContent(content = content, selection = selection, context = context).element0).sref()
        val selectedValueLabel: Renderable
        val isMenu: Boolean
        if (style == PickerStyle.automatic || style == PickerStyle.menu) {
            selectedValueLabel = HStack(spacing = 2.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    selectedValueRenderable.asView().Compose(composectx)
                    Image(systemName = "chevron.down").accessibilityHidden(true).Compose(composectx)
                    ComposeResult.ok
                }
            }
            isMenu = true
        } else {
            selectedValueLabel = selectedValueRenderable.sref()
            isMenu = false
        }
        if (performsAction) {
            Box { ->
                Button.RenderTextButton(label = selectedValueLabel.asView(), context = context) { -> toggleIsMenuExpanded() }
                if (isMenu) {
                    RenderPickerSelectionMenu(taggedRenderables = taggedRenderables, context = context.content())
                }
            }
        } else {
            var foregroundStyle = (EnvironmentValues.shared._tint ?: Color(colorImpl = { -> MaterialTheme.colorScheme.outline })).sref()
            if (!EnvironmentValues.shared.isEnabled) {
                foregroundStyle = foregroundStyle.opacity(Double(ContentAlpha.disabled))
            }
            EnvironmentValues.shared.setValues(l@{ it ->
                it.set_foregroundStyle(foregroundStyle)
                return@l ComposeResult.ok
            }, in_ = { -> selectedValueLabel.Render(context = context) })
        }
    }

    @Composable
    private fun RenderSegmentedValue(context: ComposeContext) {
        val (_, tagged) = processPickerContent(content = content, selection = selection, context = context, requireTaggedRenderables = true)
        val selectedIndex = tagged?.indexOfFirst { it ->
            TagModifier.on(content = it, role = ModifierRole.tag)?.value == selection.wrappedValue
        } ?: -1
        val isEnabled = EnvironmentValues.shared.isEnabled
        val colors: SegmentedButtonColors
        val disabledBorderColor = Color.primary.colorImpl().copy(alpha = ContentAlpha.disabled)
        val matchtarget_0 = EnvironmentValues.shared._tint
        if (matchtarget_0 != null) {
            val tint = matchtarget_0
            colors = SegmentedButtonDefaults.colors(activeContainerColor = tint.colorImpl().copy(alpha = 0.15f), disabledActiveBorderColor = disabledBorderColor, disabledInactiveBorderColor = disabledBorderColor)
        } else {
            colors = SegmentedButtonDefaults.colors(disabledActiveBorderColor = disabledBorderColor, disabledInactiveBorderColor = disabledBorderColor)
        }

        val contentContext = context.content()
        val updateOptions = EnvironmentValues.shared._material3SegmentedButton
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillWidth().then(context.modifier)) { ->
            if (tagged != null) {
                for ((index, taggedRenderable) in tagged.withIndex()) {
                    val isSelected = index == selectedIndex
                    val onClick: () -> Unit = { ->
                        selection.wrappedValue = TagModifier.on(content = taggedRenderable, role = ModifierRole.tag)?.value as SelectionValue
                    }
                    val shape = SegmentedButtonDefaults.itemShape(index = index, count = tagged.size)
                    val borderColor = (if (isSelected) (if (isEnabled) colors.activeBorderColor else colors.disabledActiveBorderColor) else (if (isEnabled) colors.inactiveBorderColor else colors.disabledInactiveBorderColor)).sref()
                    val border = SegmentedButtonDefaults.borderStroke(borderColor)
                    val icon: @Composable () -> Unit = { -> SegmentedButtonDefaults.Icon(isSelected) }
                    var options = Material3SegmentedButtonOptions(index = index, count = tagged.size, selected = isSelected, onClick = onClick, modifier = Modifier, enabled = isEnabled, shape = shape, colors = colors, border = border, icon = icon)
                    if (updateOptions != null) {
                        options = updateOptions(options)
                    }
                    SegmentedButton(selected = options.selected, onClick = options.onClick, modifier = options.modifier, enabled = options.enabled, shape = options.shape, colors = options.colors, border = options.border, icon = options.icon) { ->
                        val matchtarget_1 = taggedRenderable.strip() as? Label
                        if (matchtarget_1 != null) {
                            val label = matchtarget_1
                            label.RenderTitle(context = contentContext)
                        } else {
                            taggedRenderable.Render(context = contentContext)
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun shouldRenderListItem(context: ComposeContext): Tuple2<Boolean, (() -> Unit)?> {
        val style = EnvironmentValues.shared._pickerStyle
        if (style == PickerStyle.segmented) {
            return Tuple2(false, null)
        }
        val action: () -> Unit
        if (style == PickerStyle.navigationLink) {
            val navigator = LocalNavigator.current.sref()
            val label = this.label.Evaluate(context = context, options = 0).firstOrNull() ?: EmptyView()
            val title = titleFromLabel(label, context = context)
            action = { ->
                navigator?.navigateToView(PickerSelectionView(title = title, content = content, selection = selection))
            }
        } else {
            action = { -> toggleIsMenuExpanded() }
        }
        return Tuple2(true, action)
    }

    @Composable
    override fun RenderListItem(context: ComposeContext, modifiers: kotlin.collections.List<ModifierProtocol>) {
        ModifiedContent.RenderWithModifiers(modifiers, context = context) { context ->
            val (selected, tagged) = processPickerContent(content = content, selection = selection, context = context)
            val style = EnvironmentValues.shared._pickerStyle ?: PickerStyle.automatic
            Row(modifier = context.modifier, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { ->
                if (!EnvironmentValues.shared._labelsHidden) {
                    Box(modifier = Modifier.padding(end = 8.dp).weight(1.0f)) { -> label.Compose(context = context) }
                }
                Box { ->
                    RenderSelectedValue(selectedRenderable = selected, taggedRenderables = tagged, context = context, style = style, performsAction = false)
                    if (style != PickerStyle.segmented && style != PickerStyle.navigationLink) {
                        RenderPickerSelectionMenu(taggedRenderables = tagged, context = context)
                    }
                }
                if (style == PickerStyle.navigationLink) {
                    NavigationLink.RenderChevron()
                }
            }
        }
    }

    @Composable
    private fun RenderPickerSelectionMenu(taggedRenderables: kotlin.collections.List<Renderable>?, context: ComposeContext) {
        // Create selectable views from the *content* of each tag view, preserving the enclosing tag
        val renderables = (taggedRenderables ?: processPickerContent(content = content, selection = selection, context = context, requireTaggedRenderables = true).element1 ?: listOf()).sref()
        val menuItems = renderables.map l@{ it ->
            val renderable = (it as Renderable).sref() // Let transpiler understand type
            val tagValue = TagModifier.on(content = renderable, role = ModifierRole.tag)?.value.sref()
            val button = Button(action = { -> selection.wrappedValue = tagValue as SelectionValue }, label = { ->
                ComposeBuilder { composectx: ComposeContext ->
                    renderable.asView().Compose(composectx)
                    ComposeResult.ok
                }
            })
            return@l ModifiedContent(content = button as Renderable, modifier = TagModifier(value = tagValue, role = ModifierRole.tag))
        }
        DropdownMenu(expanded = isMenuExpanded?.value == true, onDismissRequest = { ->
            isMenuExpanded?.value = false
        }) { ->
            val coroutineScope = rememberCoroutineScope()
            Menu.RenderDropdownMenuItems(for_ = menuItems, selection = selection.wrappedValue, context = context, replaceMenu = { _ ->
                coroutineScope.launch { ->
                    delay(200) // Allow menu item selection animation to be visible
                    isMenuExpanded?.value = false
                }
            })
        }
    }

    @Composable
    private fun titleFromLabel(label: Renderable, context: ComposeContext): Text {
        val stripped = label.strip()
        val matchtarget_2 = stripped as? Text
        if (matchtarget_2 != null) {
            val text = matchtarget_2
            return text
        } else {
            val matchtarget_3 = stripped as? Label
            if (matchtarget_3 != null) {
                val label = matchtarget_3
                val matchtarget_4 = label.title.Evaluate(context = context, options = 0).firstOrNull()?.strip() as? Text
                if (matchtarget_4 != null) {
                    val text = matchtarget_4
                    return text
                } else {
                    return Text(verbatim = String(describing = selection.wrappedValue))
                }
            } else {
                return Text(verbatim = String(describing = selection.wrappedValue))
            }
        }
    }

    private fun toggleIsMenuExpanded() {
        isMenuExpanded.sref()?.let { isMenuExpanded ->
            isMenuExpanded.value = !isMenuExpanded.value
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

class PickerStyle: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PickerStyle) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = PickerStyle(rawValue = 1) // For bridging
        val navigationLink = PickerStyle(rawValue = 2) // For bridging
        val segmented = PickerStyle(rawValue = 3) // For bridging

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val inline = PickerStyle(rawValue = 4) // For bridging

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val wheel = PickerStyle(rawValue = 5) // For bridging

        val menu = PickerStyle(rawValue = 6) // For bridging

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val palette = PickerStyle(rawValue = 7) // For bridging
    }
}

@Composable
internal fun <SelectionValue> processPickerContent(content: ComposeBuilder, selection: Binding<SelectionValue>, context: ComposeContext, requireTaggedRenderables: Boolean = false): Tuple2<Renderable, kotlin.collections.List<Renderable>?> {
    val selectedTag = selection.wrappedValue.sref()
    val renderables = content.Evaluate(context = context, options = EvaluateOptions(isKeepForEach = true).value)
    if (!requireTaggedRenderables) {
        // Attempt to shortcut finding the selected view without expanding everything
        for (forEach in renderables.mapNotNull({ it -> it as? ForEach })) {
            forEach.untaggedRenderable(forTag = selectedTag, context = context)?.let { selected ->
                return Tuple2(selected.sref(), null)
            }
        }
    }

    var selected: Renderable? = null
    var tagged: kotlin.collections.MutableList<Renderable> = mutableListOf()
    for (renderable in renderables.sref()) {
        val current: kotlin.collections.List<Renderable>
        val matchtarget_5 = renderable as? View
        if (matchtarget_5 != null) {
            val view = matchtarget_5
            if (renderable.strip() is ForEach) {
                current = view.Evaluate(context = context, options = 0)
            } else {
                current = listOf(renderable)
            }
        } else {
            current = listOf(renderable)
        }
        for (renderable in current.sref()) {
            TagModifier.on(content = renderable, role = ModifierRole.tag)?.let { tagModifier ->
                tagged.add(renderable)
                if ((selected == null) && (tagModifier.value == selectedTag)) {
                    selected = renderable.sref()
                }
            }
        }
    }
    return Tuple2((selected ?: EmptyView()).sref(), tagged.sref())
}

internal class PickerSelectionView<SelectionValue>: View {
    internal val title: Text
    internal val content: View
    internal val selection: Binding<SelectionValue>

    private var selectionValue: SelectionValue
        get() = _selectionValue.wrappedValue.sref({ this.selectionValue = it })
        set(newValue) {
            _selectionValue.wrappedValue = newValue.sref()
        }
    private var _selectionValue: skip.ui.State<SelectionValue>
    private lateinit var dismiss: DismissAction

    internal constructor(title: Text, content: View, selection: Binding<SelectionValue>) {
        this.title = title
        this.content = content.sref()
        this.selection = selection.sref()
        this._selectionValue = State(initialValue = selection.wrappedValue)
    }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            List(fixedContent = content, itemTransformer = { it -> pickerRow(label = it) })
                .navigationTitle(title).Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedselectionValue by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<SelectionValue>, Any>) { mutableStateOf(_selectionValue) }
        _selectionValue = rememberedselectionValue

        this.dismiss = EnvironmentValues.shared.dismiss

        return super.Evaluate(context, options)
    }

    private fun pickerRow(label: Renderable): Renderable {
        val labelValue = (TagModifier.on(content = label, role = ModifierRole.tag)?.value as? SelectionValue).sref()
        return Button(action = { ->
            if (labelValue != null) {
                selection.wrappedValue = labelValue
                this.selectionValue = labelValue // Update the checkmark in the UI while we dismiss
            }
            dismiss()
        }, label = { ->
            ComposeBuilder { composectx: ComposeContext ->
                HStack { ->
                    ComposeBuilder { composectx: ComposeContext ->
                        // The embedded ZStack allows us to fill the width without a Spacer, which in Compose will share equal space with
                        // the label if it also wants to expand to fill space
                        ZStack { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                label.asView().Compose(composectx)
                                ComposeResult.ok
                            }
                        }
                        .frame(maxWidth = Double.infinity, alignment = Alignment.leading).Compose(composectx)
                        Image(systemName = "checkmark")
                            .foregroundStyle(EnvironmentValues.shared._tint ?: Color.accentColor)
                            .opacity(if (labelValue == selectionValue) 1.0 else 0.0).Compose(composectx)
                        ComposeResult.ok
                    }
                }.Compose(composectx)
                ComposeResult.ok
            }
        })
        .buttonStyle(ButtonStyle.plain)
        .asRenderable()
    }
}

internal class PickerStyleModifier: EnvironmentModifier {
    internal val style: PickerStyle

    internal constructor(style: PickerStyle): super() {
        this.style = style
        this.action = l@{ environment ->
            environment.set_pickerStyle(style)
            return@l ComposeResult.ok
        }
    }

    @Composable
    override fun shouldRenderListItem(content: Renderable, context: ComposeContext): Tuple2<Boolean, (() -> Unit)?> {
        // The picker style matters when deciding whether to render pickers as list items
        return EnvironmentValues.shared.setValuesWithReturn(action!!, in_ = l@{ -> return@l content.shouldRenderListItem(context = context) })
    }
}

@Suppress("MUST_BE_INITIALIZED")
class Material3SegmentedButtonOptions: MutableStruct {
    val index: Int
    val count: Int
    var selected: Boolean
        get() = field.sref({ this.selected = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
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
        get() = field.sref({ this.enabled = it })
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
    var colors: SegmentedButtonColors
        get() = field.sref({ this.colors = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var border: BorderStroke
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
    var icon: @Composable () -> Unit
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    fun copy(selected: Boolean = this.selected, onClick: () -> Unit = this.onClick, modifier: Modifier = this.modifier, enabled: Boolean = this.enabled, shape: androidx.compose.ui.graphics.Shape = this.shape, colors: SegmentedButtonColors = this.colors, border: BorderStroke = this.border, contentPadding: PaddingValues = this.contentPadding, interactionSource: MutableInteractionSource? = this.interactionSource, icon: @Composable () -> Unit = this.icon): Material3SegmentedButtonOptions = Material3SegmentedButtonOptions(index = index, count = count, selected = selected, onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, colors = colors, border = border, contentPadding = contentPadding, interactionSource = interactionSource, icon = icon)

    constructor(index: Int, count: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, shape: androidx.compose.ui.graphics.Shape, colors: SegmentedButtonColors, border: BorderStroke, contentPadding: PaddingValues = SegmentedButtonDefaults.ContentPadding, interactionSource: MutableInteractionSource? = null, icon: @Composable () -> Unit) {
        this.index = index
        this.count = count
        this.selected = selected
        this.onClick = onClick
        this.modifier = modifier
        this.enabled = enabled
        this.shape = shape
        this.colors = colors
        this.border = border
        this.contentPadding = contentPadding
        this.interactionSource = interactionSource
        this.icon = icon
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = Material3SegmentedButtonOptions(index, count, selected, onClick, modifier, enabled, shape, colors, border, contentPadding, interactionSource, icon)

    @androidx.annotation.Keep
    companion object {
    }
}

/*
@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension Picker {
/// Creates a picker that displays a custom label.
///
/// If the wrapped values of the collection passed to `sources` are not all
/// the same, some styles render the selection in a mixed state. The
/// specific presentation depends on the style.  For example, a Picker
/// with a menu style uses dashes instead of checkmarks to indicate the
/// selected values.
///
/// In the following example, a picker in a document inspector controls the
/// thickness of borders for the currently-selected shapes, which can be of
/// any number.
///
///     enum Thickness: String, CaseIterable, Identifiable {
///         case thin
///         case regular
///         case thick
///
///         var id: String { rawValue }
///     }
///
///     struct Border {
///         var color: Color
///         var thickness: Thickness
///     }
///
///     @State private var selectedObjectBorders = [
///         Border(color: .black, thickness: .thin),
///         Border(color: .red, thickness: .thick)
///     ]
///
///     Picker(
///         sources: $selectedObjectBorders,
///         selection: \.thickness
///     ) {
///         ForEach(Thickness.allCases) { thickness in
///             Text(thickness.rawValue)
///         }
///     } label: {
///         Text("Border Thickness")
///     }
///
/// - Parameters:
///     - sources: A collection of values used as the source for displaying
///       the Picker's selection.
///     - selection: The key path of the values that determines the
///       currently-selected options. When a user selects an option from the
///       picker, the values at the key path of all items in the `sources`
///       collection are updated with the selected option.
///     - content: A view that contains the set of options.
///     - label: A view that describes the purpose of selecting an option.
@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
public init<C>(sources: C, selection: KeyPath<C.Element, Binding<SelectionValue>>, @ViewBuilder content: () -> Content, @ViewBuilder label: () -> Label) where C : RandomAccessCollection { fatalError() }
}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension Picker /* where Label == Text */ {

/// Creates a picker that generates its label from a localized string key.
///
/// - Parameters:
///     - titleKey: A localized string key that describes the purpose of
///       selecting an option.
///     - selection: A binding to a property that determines the
///       currently-selected option.
///     - content: A view that contains the set of options.
///
/// This initializer creates a ``Text`` view on your behalf, and treats the
/// localized key similar to ``Text/init(_:tableName:bundle:comment:)``. See
/// ``Text`` for more information about localizing strings.
///
/// To initialize a picker with a string variable, use
/// ``init(_:selection:content:)-5njtq`` instead.


/// Creates a picker that generates its label from a localized string key.
///
/// If the wrapped values of the collection passed to `sources` are not all
/// the same, some styles render the selection in a mixed state. The
/// specific presentation depends on the style.  For example, a Picker
/// with a menu style uses dashes instead of checkmarks to indicate the
/// selected values.
///
/// In the following example, a picker in a document inspector controls the
/// thickness of borders for the currently-selected shapes, which can be of
/// any number.
///
///     enum Thickness: String, CaseIterable, Identifiable {
///         case thin
///         case regular
///         case thick
///
///         var id: String { rawValue }
///     }
///
///     struct Border {
///         var color: Color
///         var thickness: Thickness
///     }
///
///     @State private var selectedObjectBorders = [
///         Border(color: .black, thickness: .thin),
///         Border(color: .red, thickness: .thick)
///     ]
///
///     Picker(
///         "Border Thickness",
///         sources: $selectedObjectBorders,
///         selection: \.thickness
///     ) {
///         ForEach(Thickness.allCases) { thickness in
///             Text(thickness.rawValue)
///         }
///     }
///
/// - Parameters:
///     - titleKey: A localized string key that describes the purpose of
///       selecting an option.
///     - sources: A collection of values used as the source for displaying
///       the Picker's selection.
///     - selection: The key path of the values that determines the
///       currently-selected options. When a user selects an option from the
///       picker, the values at the key path of all items in the `sources`
///       collection are updated with the selected option.
///     - content: A view that contains the set of options.
///
/// This initializer creates a ``Text`` view on your behalf, and treats the
/// localized key similar to ``Text/init(_:tableName:bundle:comment:)``. See
/// ``Text`` for more information about localizing strings.
@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
public init<C>(_ titleKey: LocalizedStringKey, sources: C, selection: KeyPath<C.Element, Binding<SelectionValue>>, @ViewBuilder content: () -> Content) where C : RandomAccessCollection { fatalError() }

/// Creates a picker bound to a collection of bindings that generates its
/// label from a string.
///
/// If the wrapped values of the collection passed to `sources` are not all
/// the same, some styles render the selection in a mixed state. The
/// specific presentation depends on the style.  For example, a Picker
/// with a menu style uses dashes instead of checkmarks to indicate the
/// selected values.
///
/// In the following example, a picker in a document inspector controls the
/// thickness of borders for the currently-selected shapes, which can be of
/// any number.
///
///     enum Thickness: String, CaseIterable, Identifiable {
///         case thin
///         case regular
///         case thick
///
///         var id: String { rawValue }
///     }
///
///     struct Border {
///         var color: Color
///         var thickness: Thickness
///     }
///
///     @State private var selectedObjectBorders = [
///         Border(color: .black, thickness: .thin),
///         Border(color: .red, thickness: .thick)
///     ]
///
///     Picker(
///         "Border Thickness",
///         sources: $selectedObjectBorders,
///         selection: \.thickness
///     ) {
///         ForEach(Thickness.allCases) { thickness in
///             Text(thickness.rawValue)
///         }
///     }
///
/// - Parameters:
///     - title: A string that describes the purpose of selecting an option.
///     - sources: A collection of values used as the source for displaying
///       the Picker's selection.
///     - selection: The key path of the values that determines the
///       currently-selected options. When a user selects an option from the
///       picker, the values at the key path of all items in the `sources`
///       collection are updated with the selected option.
///     - content: A view that contains the set of options.
///
/// This initializer creates a ``Text`` view on your behalf, and treats the
/// title similar to ``Text/init(_:)-9d1g4``. See ``Text`` for more
/// information about localizing strings.
///
/// To initialize a picker with a localized string key, use
/// ``init(_:sources:selection:content:)-6e1x`` instead.
@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
public init<C, S>(_ title: S, sources: C, selection: KeyPath<C.Element, Binding<SelectionValue>>, @ViewBuilder content: () -> Content) where C : RandomAccessCollection, S : StringProtocol { fatalError() }
}
*/
