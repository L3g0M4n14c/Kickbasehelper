package skip.ui

import kotlin.reflect.KClass
import skip.lib.*
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Stable // Otherwise Compose recomposes all internal @Composable funcs because 'this' is unstable
class Table<ObjectType, ID>: View, Renderable where ObjectType: Identifiable<ID> {
    internal val data: RandomAccessCollection<ObjectType>
    internal var selection: Binding<Any?>? = null
        get() = field.sref({ this.selection = it })
        set(newValue) {
            field = newValue.sref()
        }
    internal val columnSpecs: ComposeBuilder

    // Note: The SwiftUI.Table content block does *not* accept any arguments. We add the argument to the
    // Kotlin transpilation so that we can also add it to each nested TableColumn call, which in turn allows
    // the Kotlin compiler to infer the expected ObjectType for the columns. Otherwise TableColumn calls
    // don't have enough information for the Kotlin compiler to infer their generic type
    constructor(data: RandomAccessCollection<ObjectType>, selection: Any? = null, content: (RandomAccessCollection<ObjectType>) -> View) {
        this.data = data.sref()
        this.selection = selection as? Binding<Any?>
        val view = content(data)
        this.columnSpecs = view as? ComposeBuilder ?: ComposeBuilder(view = view)
    }

    @Composable
    override fun Render(context: ComposeContext) {
        // When we layout, extend into safe areas that are due to system bars, not into any app chrome. We'll add
        // blank head
        val safeArea = EnvironmentValues.shared._safeArea
        var ignoresSafeAreaEdges: Edge.Set = Edge.Set.of(Edge.Set.top, Edge.Set.bottom)
        ignoresSafeAreaEdges.formIntersection(safeArea?.absoluteSystemBarEdges ?: Edge.Set.of())
        val itemContext = context.content()
        ComposeContainer(scrollAxes = Axis.Set.vertical, modifier = context.modifier, fillWidth = true, fillHeight = true) { modifier ->
            IgnoresSafeAreaLayout(expandInto = ignoresSafeAreaEdges, modifier = modifier) { safeAreaExpansion, _ ->
                val density = LocalDensity.current.sref()
                val headerSafeAreaHeight = with(density) { -> safeAreaExpansion.top.toDp() }
                val footerSafeAreaHeight = with(density) { -> safeAreaExpansion.bottom.toDp() }
                RenderTable(context = itemContext, headerSafeAreaHeight = headerSafeAreaHeight, footerSafeAreaHeight = footerSafeAreaHeight)
            }
        }
    }

    @Composable
    private fun RenderTable(context: ComposeContext, headerSafeAreaHeight: Dp, footerSafeAreaHeight: Dp) {
        // Collect all top-level views to compose. The LazyColumn itself is not a composable context, so we have to gather
        // our content before entering the LazyColumn body, then use LazyColumn's LazyListScope functions to compose
        // individual items
        val columnSpecs = this.columnSpecs.Evaluate(context = context, options = 0)
        val modifier = context.modifier.fillMaxWidth()

        val listState = rememberLazyListState()
        // Integrate with our scroll-to-top navigation bar taps
        val coroutineScope = rememberCoroutineScope()
        PreferenceValues.shared.contribute(context = context, key = ScrollToTopPreferenceKey::class, value = ScrollToTopAction(key = listState) { ->
            coroutineScope.launch { -> listState.animateScrollToItem(0) }
        })

        // See explanation in List.swift
        val forceUnanimatedItems = remember { -> mutableStateOf(false) }
        if (Animation.current(isAnimating = false) == null) {
            forceUnanimatedItems.value = true
            LaunchedEffect(System.currentTimeMillis()) { ->
                delay(300)
                forceUnanimatedItems.value = false
            }
        } else {
            forceUnanimatedItems.value = false
        }

        val shouldAnimateItems: @Composable () -> Boolean = { ->
            // We disable animation to prevent filtered items from animating when they return
            !forceUnanimatedItems.value && EnvironmentValues.shared._searchableState?.isSearching?.value != true
        }

        val key: (Int) -> String = { it -> composeBundleString(for_ = data[it].id) }
        val isCompact = EnvironmentValues.shared.horizontalSizeClass == UserInterfaceSizeClass.compact
        LazyColumn(state = listState, modifier = modifier) { ->
            if (headerSafeAreaHeight.value > 0) {
                item { -> RenderHeaderFooter(safeAreaHeight = headerSafeAreaHeight) }
            }
            if (!isCompact) {
                item { ->
                    val animationModifier = (if (shouldAnimateItems()) Modifier.animateItem() else Modifier).sref()
                    RenderHeadersRow(columnSpecs = columnSpecs, context = context, animationModifier = animationModifier)
                }
            }
            items(count = data.count, key = key) { index ->
                val animationModifier = (if (shouldAnimateItems()) Modifier.animateItem() else Modifier).sref()
                RenderRow(columnSpecs = columnSpecs, index = index, context = context, isCompact = isCompact, animationModifier = animationModifier)
            }
            if (footerSafeAreaHeight.value > 0.0) {
                item { -> RenderHeaderFooter(safeAreaHeight = footerSafeAreaHeight) }
            }
        }
    }

    @Composable
    private fun RenderHeadersRow(columnSpecs: kotlin.collections.List<Renderable>, context: ComposeContext, animationModifier: Modifier) {
        val modifier = Modifier.fillMaxWidth().then(animationModifier)
        val foregroundStyle: ShapeStyle = (EnvironmentValues.shared._foregroundStyle ?: Color.accentColor).sref()
        EnvironmentValues.shared.setValues(l@{ it ->
            it.set_foregroundStyle(foregroundStyle)
            return@l ComposeResult.ok
        }, in_ = { ->
            Column(modifier = modifier) { ->
                Row(modifier = List.contentModifier(level = 0), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { ->
                    for (columnSpec in columnSpecs.sref()) {
                        val tableColumn_0 = columnSpec as? TableColumn
                        if (tableColumn_0 == null) {
                            continue
                        }
                        val itemContentModifier = modifier(for_ = tableColumn_0.columnWidth, defaultWeight = Modifier.weight(1.0f))
                        val itemContext = context.content(modifier = itemContentModifier)
                        tableColumn_0.columnHeader.Compose(context = itemContext)
                    }
                }
                List.RenderSeparator(level = 0)
            }
        })
    }

    @Composable
    private fun RenderRow(columnSpecs: kotlin.collections.List<Renderable>, index: Int, context: ComposeContext, isCompact: Boolean, animationModifier: Modifier) {
        var modifier = Modifier.fillMaxWidth()
        val itemID = rememberUpdatedState(data[index].id)
        val isSelected = isSelected(id = itemID.value)
        if (isSelected) {
            val selectionColor = if (isCompact) Color.separator.colorImpl() else (EnvironmentValues.shared._tint?.colorImpl?.invoke() ?: Color.accentColor.colorImpl())
            modifier = modifier.background(selectionColor)
        }
        if (selection != null) {
            val interactionSource = remember { -> MutableInteractionSource() }
            modifier = modifier.clickable(interactionSource = interactionSource, indication = null) { -> select(id = itemID.value) }
        }
        modifier = modifier.then(animationModifier)
        val foregroundStyle = EnvironmentValues.shared._foregroundStyle.sref()
        Column(modifier = modifier) { ->
            Row(modifier = List.contentModifier(level = 0), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { ->
                val count = if (isCompact) 1 else columnSpecs.size
                for (i in 0 until min(count, columnSpecs.size)) {
                    val tableColumn_1 = columnSpecs[i] as? TableColumn
                    if (tableColumn_1 == null) {
                        continue
                    }
                    var itemForegroundStyle: ShapeStyle? = foregroundStyle.sref()
                    if (itemForegroundStyle == null) {
                        if (isSelected && !isCompact) {
                            itemForegroundStyle = (if (i == 0) Color.white else Color.white.opacity(0.8)).sref()
                        } else {
                            itemForegroundStyle = (if (i == 0) null else Color.secondary).sref()
                        }
                    }
                    val placement = EnvironmentValues.shared._placement.sref()
                    EnvironmentValues.shared.setValues(l@{ it ->
                        it.set_foregroundStyle(itemForegroundStyle)
                        it.set_placement(placement.union(ViewPlacement.listItem))
                        return@l ComposeResult.ok
                    }, in_ = { ->
                        val itemContentModifier = if (isCompact) Modifier.fillMaxWidth() else modifier(for_ = tableColumn_1.columnWidth, defaultWeight = Modifier.weight(1.0f))
                        val itemContext = context.content()
                        val itemRenderable = tableColumn_1.cellContent(data[index]).Evaluate(context = itemContext, options = 0).firstOrNull() ?: EmptyView()
                        List.RenderItemContent(item = itemRenderable, context = itemContext, modifier = itemContentModifier)
                    })
                }
            }
            List.RenderSeparator(level = 0)
        }
    }

    /// - Warning: Only call with a positive safe area height. This is distinct from having this function detect
    /// and return without rendering. That causes a weird rubber banding effect on overscroll.
    @Composable
    private fun RenderHeaderFooter(safeAreaHeight: Dp) {
        val modifier = Modifier.fillMaxWidth()
            .height(safeAreaHeight)
            .zIndex(0.5f)
            .background(Color.background.colorImpl())
        Box(modifier = modifier) { ->  }
    }

    @Composable
    private fun modifier(for_: TableColumn.WidthSpec, defaultWeight: Modifier): Modifier {
        val width = for_
        when (width) {
            is TableColumn.WidthSpec.FixedCase -> {
                val value = width.associated0
                return Modifier.width(value.dp)
            }
            is TableColumn.WidthSpec.RangeCase -> {
                val min = width.associated0
                val ideal = width.associated1
                val max = width.associated2
                // Mirror the logic we use in FrameLayout
                if (max == Double.infinity) {
                    var modifier = defaultWeight
                    if ((min != null) && (min > 0.0)) {
                        modifier = modifier.requiredWidthIn(min = min.dp)
                    }
                    return modifier
                } else if (min != null || max != null) {
                    return Modifier.requiredWidthIn(min = if (min != null) min!!.dp else Dp.Unspecified, max = if (max != null) max!!.dp else Dp.Unspecified)
                } else {
                    return defaultWeight
                }
            }
            is TableColumn.WidthSpec.DefaultCase -> return defaultWeight
        }
    }


    private fun isSelected(id: ID): Boolean {
        val wrappedValue = selection?.wrappedValue.sref()
        val matchtarget_0 = wrappedValue as? Set<ID>
        if (matchtarget_0 != null) {
            val set = matchtarget_0
            return set.contains(id)
        } else {
            return id == (wrappedValue as? ID)
        }
    }

    private fun select(id: ID) {
        if (selection?.wrappedValue is Set<*>) {
            val selectedSet: Set<ID> = setOf(id)
            selection?.wrappedValue = selectedSet
        } else {
            selection?.wrappedValue = id
        }
    }

    @androidx.annotation.Keep
    companion object {
    }
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
fun <T, ID> Table(of: KClass<T>, selection: Any? = null, columnCustomization: Any? = null, columns: () -> View, rows: () -> View): Table<T, ID> where T: Identifiable<ID> {
    val valueType = of
    fatalError()
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
fun Table(selection: Any? = null, sortOrder: Any, columns: () -> View, rows: () -> View): View {
    fatalError()
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
fun <T, ID> Table(of: KClass<T>, selection: Any? = null, sortOrder: Any, columnCustomization: Any? = null, columns: () -> View, rows: () -> View): Table<T, ID> where T: Identifiable<ID> {
    val valueType = of
    fatalError()
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
fun <ObjectType, ID> Table(data: RandomAccessCollection<ObjectType>, selection: Any? = null, columnCustomization: Any, content: (RandomAccessCollection<ObjectType>) -> View): Table<ObjectType, ID> where ObjectType: Identifiable<ID> {
    fatalError()
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
fun <ObjectType, ID> Table(data: RandomAccessCollection<ObjectType>, selection: Any? = null, sortOrder: Any, columnCustomization: Any? = null, content: (RandomAccessCollection<ObjectType>) -> View): Table<ObjectType, ID> where ObjectType: Identifiable<ID> {
    fatalError()
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
fun <ObjectType, ID> Table(data: RandomAccessCollection<ObjectType>, children: Any, selection: Any? = null, sortOrder: Any? = null, columnCustomization: Any? = null, content: (RandomAccessCollection<ObjectType>) -> View): Table<ObjectType, ID> where ObjectType: Identifiable<ID> {
    fatalError()
}

class TableColumn: View, Renderable {
    internal val columnHeader: Text
    internal val columnWidth: TableColumn.WidthSpec
    internal val cellContent: (Any) -> View

    internal constructor(columnHeader: Text, columnWidth: TableColumn.WidthSpec, cellContent: (Any) -> View) {
        this.columnHeader = columnHeader
        this.columnWidth = columnWidth
        this.cellContent = cellContent
    }

    @Composable
    override fun Render(context: ComposeContext) = Unit

    fun width(width: Double? = null): TableColumn {
        if (width == null) {
            return this
        }
        return TableColumn(columnHeader = columnHeader, columnWidth = TableColumn.WidthSpec.fixed(width), cellContent = cellContent)
    }

    fun width(min: Double? = null, ideal: Double? = null, max: Double? = null): TableColumn {
        if (min == null && ideal == null && max == null) {
            return this
        }
        return TableColumn(columnHeader = columnHeader, columnWidth = TableColumn.WidthSpec.range(min = min, ideal = ideal, max = max), cellContent = cellContent)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun defaultVisibility(visibility: Visibility): TableColumn {
        fatalError()
    }

    override fun customizationID(id: String): TableColumn = this

    fun disabledCustomizationBehavior(behavior: Any): TableColumn = this

    internal sealed class WidthSpec {
        class DefaultCase: WidthSpec() {
        }
        class FixedCase(val associated0: Double): WidthSpec() {
        }
        class RangeCase(val associated0: Double?, val associated1: Double?, val associated2: Double?): WidthSpec() {
            val min = associated0
            val ideal = associated1
            val max = associated2
        }

        @androidx.annotation.Keep
        companion object {
            val default: WidthSpec = DefaultCase()
            fun fixed(associated0: Double): WidthSpec = FixedCase(associated0)
            fun range(min: Double?, ideal: Double?, max: Double?): WidthSpec = RangeCase(min, ideal, max)
        }
    }

    @androidx.annotation.Keep
    companion object {
    }
}

fun <ObjectType> TableColumn(data: RandomAccessCollection<ObjectType>, title: String, value: (ObjectType) -> String, comparator: Any? = null): TableColumn {
    return TableColumn(columnHeader = Text(verbatim = title).bold(), columnWidth = TableColumn.WidthSpec.default, cellContent = { it -> Text(verbatim = value(it as ObjectType)) })
}

fun <ObjectType> TableColumn(data: RandomAccessCollection<ObjectType>, title: String, value: Any? = null, comparator: Any? = null, content: (ObjectType) -> View): TableColumn {
    return TableColumn(columnHeader = Text(verbatim = title).bold(), columnWidth = TableColumn.WidthSpec.default, cellContent = { it -> content(it as ObjectType) })
}

fun <ObjectType> TableColumn(data: RandomAccessCollection<ObjectType>, titleKey: LocalizedStringKey, value: (ObjectType) -> String, comparator: Any? = null): TableColumn {
    return TableColumn(columnHeader = Text(titleKey).bold(), columnWidth = TableColumn.WidthSpec.default, cellContent = { it -> Text(verbatim = value(it as ObjectType)) })
}

fun <ObjectType> TableColumn(data: RandomAccessCollection<ObjectType>, titleResource: LocalizedStringResource, value: (ObjectType) -> String, comparator: Any? = null): TableColumn {
    return TableColumn(columnHeader = Text(titleResource).bold(), columnWidth = TableColumn.WidthSpec.default, cellContent = { it -> Text(verbatim = value(it as ObjectType)) })
}

fun <ObjectType> TableColumn(data: RandomAccessCollection<ObjectType>, titleKey: LocalizedStringKey, value: Any? = null, comparator: Any? = null, content: (ObjectType) -> View): TableColumn {
    return TableColumn(columnHeader = Text(titleKey).bold(), columnWidth = TableColumn.WidthSpec.default, cellContent = { it -> content(it as ObjectType) })
}

fun <ObjectType> TableColumn(data: RandomAccessCollection<ObjectType>, titleResource: LocalizedStringResource, value: Any? = null, comparator: Any? = null, content: (ObjectType) -> View): TableColumn {
    return TableColumn(columnHeader = Text(titleResource).bold(), columnWidth = TableColumn.WidthSpec.default, cellContent = { it -> content(it as ObjectType) })
}

fun <ObjectType> TableColumn(data: RandomAccessCollection<ObjectType>, title: Text, value: (ObjectType) -> String, comparator: Any? = null): TableColumn {
    return TableColumn(columnHeader = title, columnWidth = TableColumn.WidthSpec.default, cellContent = { it -> Text(verbatim = value(it as ObjectType)) })
}

fun <ObjectType> TableColumn(data: RandomAccessCollection<ObjectType>, title: Text, value: Any? = null, comparator: Any? = null, content: (ObjectType) -> View): TableColumn {
    return TableColumn(columnHeader = title, columnWidth = TableColumn.WidthSpec.default, cellContent = { it -> content(it as ObjectType) })
}

fun <ObjectType> TableColumn(data: RandomAccessCollection<ObjectType>, title: String, sortUsing: Any, content: (ObjectType) -> View): TableColumn {
    val comparator = sortUsing
    fatalError()
}

fun <ObjectType> TableColumn(data: RandomAccessCollection<ObjectType>, titleKey: LocalizedStringKey, sortUsing: Any, content: (ObjectType) -> View): TableColumn {
    val comparator = sortUsing
    fatalError()
}

fun <ObjectType> TableColumn(data: RandomAccessCollection<ObjectType>, titleResource: LocalizedStringResource, sortUsing: Any, content: (ObjectType) -> View): TableColumn {
    val comparator = sortUsing
    fatalError()
}

fun <ObjectType> TableColumn(data: RandomAccessCollection<ObjectType>, text: Text, sortUsing: Any, content: (ObjectType) -> View): TableColumn {
    val comparator = sortUsing
    fatalError()
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
class TableRow: View {
    constructor(value: Any) {
    }


    //    @available(*, unavailable)
    //    public func contextMenu(@ViewBuilder menuItems: () -> any View) -> TableRow {
    //        fatalError()
    //    }
    //
    //    @available(*, unavailable)
    //    public func contextMenu(@ViewBuilder menuItems: () -> any View, @ViewBuilder preview: () -> any View) -> TableRow {
    //        fatalError()
    //    }
    //
    //    @available(*, unavailable)
    //    public func draggable(_ payload: () -> Any) -> TableRow {
    //        fatalError()
    //    }
    //
    //    @available(*, unavailable)
    //    public func dropDestination(for payloadType: Any.Type, action: ([Any]) -> Void) -> TableRow {
    //        fatalError()
    //    }
    //
    //    @available(*, unavailable)
    //    public func itemProvider(_ action: (() -> Any?)?) -> TableRow {
    //        fatalError()
    //    }
    //
    //    @available(*, unavailable)
    //    public func dropDestination(for payloadType: Any.Type, action: (Int, [Any]) -> Void) -> TableRow {
    //        fatalError()
    //    }
    //
    //    @available(*, unavailable)
    //    public func onInsert(of supportedContentTypes: [Any], perform action: (Int, [Any]) -> Void) -> TableRow {
    //        fatalError()
    //    }

    @androidx.annotation.Keep
    companion object {
    }
}

class TableStyle {

    @androidx.annotation.Keep
    companion object {
        val automatic = TableStyle()
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val inset = TableStyle()
    }
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
class DisclosureTableRow {
    constructor(value: Any, isExpanded: Binding<Boolean>? = null, content: () -> View) {
    }

    @androidx.annotation.Keep
    companion object {
    }
}

