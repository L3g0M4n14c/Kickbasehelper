package skip.ui

import skip.lib.*
import skip.lib.Array
import skip.lib.MutableCollection
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path.Companion.combine
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

/// Corner radius for list sections.
internal val listSectionCornerRadius = 8.0

@Stable // Otherwise Compose recomposes all internal @Composable funcs because 'this' is unstable
@androidx.annotation.Keep
class List: View, Renderable, skip.lib.SwiftProjecting {
    internal val fixedContent: ComposeBuilder?
    internal val forEach: ForEach?
    internal val itemTransformer: ((Renderable) -> Renderable)?

    internal constructor(fixedContent: View? = null, identifier: ((Any) -> AnyHashable?)? = null, itemTransformer: ((Renderable) -> Renderable)? = null, indexRange: IntRange? = null, indexedContent: ((Int) -> View)? = null, objects: RandomAccessCollection<Any>? = null, objectContent: ((Any) -> View)? = null, objectsBinding: Binding<RandomAccessCollection<Any>>? = null, objectsBindingContent: ((Binding<RandomAccessCollection<Any>>, Int) -> View)? = null, editActions: EditActions = EditActions.of()) {
        if (fixedContent != null) {
            this.fixedContent = fixedContent as? ComposeBuilder ?: ComposeBuilder(view = fixedContent)
        } else {
            this.fixedContent = null
        }
        if (indexRange != null) {
            this.forEach = ForEach(identifier = identifier, indexRange = { -> indexRange }, indexedContent = indexedContent)
        } else if (objects != null) {
            this.forEach = ForEach(identifier = identifier, objects = objects, objectContent = objectContent)
        } else if (objectsBinding != null) {
            this.forEach = ForEach(identifier = identifier, objectsBinding = objectsBinding, objectsBindingContent = objectsBindingContent, editActions = editActions)
        } else {
            this.forEach = null
        }
        this.itemTransformer = itemTransformer
    }

    constructor(content: () -> View): this(fixedContent = content()) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(selection: Binding<Any>, content: () -> View): this(fixedContent = content()) {
    }

    constructor(bridgedContent: View) {
        val matchtarget_0 = bridgedContent as? ForEach
        if (matchtarget_0 != null) {
            val forEach = matchtarget_0
            this.fixedContent = null
            this.forEach = forEach
        } else {
            this.fixedContent = ComposeBuilder.from { -> bridgedContent }
            this.forEach = null
        }
        this.itemTransformer = null
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun Render(context: ComposeContext) {
        val style = EnvironmentValues.shared._listStyle ?: ListStyle.automatic
        val backgroundVisibility = EnvironmentValues.shared._scrollContentBackground ?: Visibility.visible
        val styling = ListStyling(style = style, backgroundVisibility = backgroundVisibility)
        val itemContext = context.content()

        // When we layout, extend into safe areas that are due to system bars, not into any app chrome
        val safeArea = EnvironmentValues.shared._safeArea
        var ignoresSafeAreaEdges: Edge.Set = Edge.Set.of(Edge.Set.top, Edge.Set.bottom)
        ignoresSafeAreaEdges.formIntersection(safeArea?.absoluteSystemBarEdges ?: Edge.Set.of())
        ComposeContainer(scrollAxes = Axis.Set.vertical, modifier = context.modifier, fillWidth = true, fillHeight = true, then = Modifier.background(BackgroundColor(styling = styling, isItem = false))) { modifier ->
            IgnoresSafeAreaLayout(expandInto = ignoresSafeAreaEdges, checkEdges = Edge.Set.of(Edge.Set.bottom), modifier = modifier) { safeAreaExpansion, safeAreaEdges ->
                var containerModifier: Modifier
                val refreshing = remember { -> mutableStateOf(false) }
                val refreshAction = EnvironmentValues.shared.refresh
                val refreshState: PullRefreshState?
                if (refreshAction != null) {
                    val refreshScope = rememberCoroutineScope()
                    val updatedAction = rememberUpdatedState(refreshAction)
                    refreshState = rememberPullRefreshState(refreshing.value, { ->
                        refreshScope.launch { ->
                            refreshing.value = true
                            updatedAction.value()
                            refreshing.value = false
                        }
                    })
                    containerModifier = modifier.pullRefresh(refreshState!!)
                } else {
                    refreshState = null
                    containerModifier = modifier
                }
                containerModifier = containerModifier.scrollDismissesKeyboardMode(EnvironmentValues.shared.scrollDismissesKeyboardMode)

                Box(modifier = containerModifier) { ->
                    val density = LocalDensity.current.sref()
                    val headerSafeAreaHeight = with(density) { -> safeAreaExpansion.top.toDp() }
                    val footerSafeAreaHeight = with(density) { -> safeAreaExpansion.bottom.toDp() }
                    RenderList(context = itemContext, styling = styling, arguments = ListArguments(headerSafeAreaHeight = headerSafeAreaHeight, footerSafeAreaHeight = footerSafeAreaHeight, safeAreaEdges = safeAreaEdges))
                    if (refreshState != null) {
                        PullRefreshIndicator(refreshing.value, refreshState, Modifier.align(androidx.compose.ui.Alignment.TopCenter))
                    }
                }
            }
        }
    }

    @Composable
    private fun RenderList(context: ComposeContext, styling: ListStyling, arguments: ListArguments) {
        val renderables: kotlin.collections.List<Renderable>
        if (forEach != null) {
            renderables = forEach.EvaluateLazyItems(level = 0, context = context)
        } else if (fixedContent != null) {
            renderables = fixedContent.EvaluateLazyItems(level = 0, context = context)
        } else {
            renderables = listOf()
        }

        var modifier = context.modifier
        if (styling.style != ListStyle.plain) {
            modifier = modifier.padding(start = Companion.horizontalInset.dp, end = Companion.horizontalInset.dp)
        }
        modifier = modifier.fillMaxSize()

        val searchableState = EnvironmentValues.shared._searchableState
        val isSearchable = searchableState?.isOnNavigationStack == false

        val hasHeader = styling.style != ListStyle.plain || (!isSearchable && arguments.headerSafeAreaHeight.value > 0)
        val hasFooter = styling.style != ListStyle.plain || arguments.footerSafeAreaHeight.value > 0

        // Remember the factory because we use it in the remembered reorderable state
        val itemCollector = remember { -> mutableStateOf(LazyItemCollector()) }
        val moveTrigger = remember { -> mutableStateOf(0) }
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = if (isSearchable && arguments.headerSafeAreaHeight.value <= 0) 1 else 0)
        val reorderableState = rememberReorderableLazyListState(listState = listState, onMove = { from, to ->
            // Trigger recompose on move, but don't read the trigger state until we're inside the list content to limit its scope
            itemCollector.value.move(from = from.index, to = to.index, trigger = { it -> moveTrigger.value = it })
        }, onDragEnd = { _, _ -> itemCollector.value.commitMove() }, canDragOver = { candidate, dragging -> itemCollector.value.canMove(from = dragging.index, to = candidate.index) })
        modifier = modifier.reorderable(reorderableState)

        // Integrate with our scroll-to-top and ScrollViewReader
        val coroutineScope = rememberCoroutineScope()
        PreferenceValues.shared.contribute(context = context, key = ScrollToTopPreferenceKey::class, value = ScrollToTopAction(key = reorderableState.listState) { ->
            coroutineScope.launch { -> reorderableState.listState.animateScrollToItem(0) }
        })
        val scrollToID = ScrollToIDAction(key = reorderableState.listState) { id ->
            itemCollector.value.index(for_ = id)?.let { itemIndex ->
                coroutineScope.launch { ->
                    if (Animation.isInWithAnimation) {
                        reorderableState.listState.animateScrollToItem(itemIndex)
                    } else {
                        reorderableState.listState.scrollToItem(itemIndex)
                    }
                }
            }
        }
        PreferenceValues.shared.contribute(context = context, key = ScrollToIDPreferenceKey::class, value = scrollToID)
        val isSystemBackground = styling.style != ListStyle.plain && styling.backgroundVisibility != Visibility.hidden
        // We contribute top bar preferences even without knowing we're safe area-adjacent for multiple reasons:
        // - When there is a search bar we may not be adjacent to the top safe area, but we should act like it
        // - An expanding nav bar can causes issues detecting safe area adjacency
        // - It is unlikely that anyone will use a grouped-style list that is not top-bar adjacent, so the top
        //   bar should always have the grouped-style system color
        PreferenceValues.shared.contribute(context = context, key = ToolbarPreferenceKey::class, value = ToolbarPreferences(isSystemBackground = isSystemBackground, scrollableState = listState, for_ = arrayOf(ToolbarPlacement.navigationBar)))
        if (arguments.safeAreaEdges.contains(Edge.Set.bottom)) {
            PreferenceValues.shared.contribute(context = context, key = ToolbarPreferenceKey::class, value = ToolbarPreferences(isSystemBackground = isSystemBackground, scrollableState = listState, for_ = arrayOf(ToolbarPlacement.bottomBar)))
            PreferenceValues.shared.contribute(context = context, key = TabBarPreferenceKey::class, value = ToolbarBarPreferences(isSystemBackground = isSystemBackground, scrollableState = listState))
        }

        // List item animations in Compose work by setting the `animateItemPlacement` modifier on the items. Critically,
        // this must be done when the items are composed *prior* to any animated change. So by default we compose all items
        // with `animateItemPlacement`. If the entire List is recomposed without an animation in progress (e.g. an unanimated
        // data change), we recompose without animation, then after some time to complete the recompose we flip back to the
        // animated state in anticipation of the next, potentially animated, update
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

        val itemContext = context.content()
        LazyColumn(state = reorderableState.listState, modifier = modifier) { ->
            // Read move trigger here so that a move will recompose list content
            moveTrigger.value.sref()
            val shouldAnimateItems: @Composable () -> Boolean = l@{ ->
                // We disable animation to prevent filtered items from animating when they return
                val animate = !forceUnanimatedItems.value && EnvironmentValues.shared._searchableState?.isSearching?.value != true
                return@l animate
            }

            // Initialize the factory context with closures that use the LazyListScope to generate items
            var startItemIndex = if (hasHeader) 1 else 0 // Header inset
            if (isSearchable) {
                startItemIndex += 1 // Search field
            }
            itemCollector.value.initialize(startItemIndex = startItemIndex, item = { renderable, level ->
                item { ->
                    val itemModifier: Modifier = if (shouldAnimateItems()) Modifier.animateItem() else Modifier
                    RenderItem(content = renderable, level = level, context = itemContext, modifier = itemModifier, styling = styling)
                }
            }, indexedItems = { range, identifier, offset, onDelete, onMove, level, factory ->
                val count = (range.endExclusive - range.start).sref()
                val key: ((Int) -> String)? = if (identifier == null) null else { it -> composeBundleString(for_ = identifier!!(range.start + itemCollector.value.remapIndex(it, from = offset))) }
                items(count = count, key = key) { index ->
                    val keyValue = key?.invoke(index) // Key closure already remaps index
                    val index = itemCollector.value.remapIndex(index, from = offset)
                    val itemModifier: Modifier = if (shouldAnimateItems()) Modifier.animateItem() else Modifier
                    val renderable = factory(index + range.start, itemContext)
                    RenderEditableItem(content = renderable, level = level, context = itemContext, modifier = itemModifier, styling = styling, key = keyValue, index = index, onDelete = onDelete, onMove = onMove, reorderableState = reorderableState)
                }
            }, objectItems = { objects, identifier, offset, onDelete, onMove, level, factory ->
                val key: (Int) -> String = { it -> composeBundleString(for_ = identifier(objects[itemCollector.value.remapIndex(it, from = offset)])) }
                items(count = objects.count, key = key) { index ->
                    val keyValue = key(index) // Key closure already remaps index
                    val index = itemCollector.value.remapIndex(index, from = offset)
                    val itemModifier: Modifier = if (shouldAnimateItems()) Modifier.animateItem() else Modifier
                    val renderable = factory(objects[index], itemContext)
                    RenderEditableItem(content = renderable, level = level, context = itemContext, modifier = itemModifier, styling = styling, key = keyValue, index = index, onDelete = onDelete, onMove = onMove, reorderableState = reorderableState)
                }
            }, objectBindingItems = { objectsBinding, identifier, offset, editActions, onDelete, onMove, level, factory ->
                val key: (Int) -> String = { it -> composeBundleString(for_ = identifier(objectsBinding.wrappedValue[itemCollector.value.remapIndex(it, from = offset)])) }
                items(count = objectsBinding.wrappedValue.count, key = key) { index ->
                    val keyValue = key(index) // Key closure already remaps index
                    val index = itemCollector.value.remapIndex(index, from = offset)
                    val itemModifier: Modifier = if (shouldAnimateItems()) Modifier.animateItem() else Modifier
                    val renderable = factory(objectsBinding, index, itemContext)
                    RenderEditableItem(content = renderable, level = level, context = itemContext, modifier = itemModifier, styling = styling, objectsBinding = objectsBinding, key = keyValue, index = index, editActions = editActions, onDelete = onDelete, onMove = onMove, reorderableState = reorderableState)
                }
            }, sectionHeader = { renderable ->
                val isTop = renderable === renderables.firstOrNull()
                if (styling.style == ListStyle.plain) {
                    stickyHeader { _ -> RenderSectionHeader(content = renderable, context = itemContext, styling = styling, isTop = isTop) }
                } else {
                    item { -> RenderSectionHeader(content = renderable, context = itemContext, styling = styling, isTop = isTop) }
                }
            }, sectionFooter = { renderable ->
                item { -> RenderSectionFooter(content = renderable, context = itemContext, styling = styling) }
            })

            if (isSearchable) {
                item { -> RenderSearchField(state = searchableState!!, context = context, styling = styling, safeAreaHeight = arguments.headerSafeAreaHeight) }
            }
            if (hasHeader) {
                val hasTopSection = renderables.firstOrNull() is LazySectionHeader
                item { -> RenderHeader(styling = styling, safeAreaHeight = if (isSearchable) 0.dp else arguments.headerSafeAreaHeight, hasTopSection = hasTopSection) }
            }
            for (renderable in renderables.sref()) {
                val matchtarget_1 = renderable as? LazyItemFactory
                if (matchtarget_1 != null) {
                    val factory = matchtarget_1
                    if (factory.shouldProduceLazyItems()) {
                        factory.produceLazyItems(collector = itemCollector.value, modifiers = listOf(), level = 0)
                    } else {
                        itemCollector.value.item(renderable, 0)
                    }
                } else {
                    itemCollector.value.item(renderable, 0)
                }
            }
            if (hasFooter) {
                val hasBottomSection = renderables.lastOrNull() is LazySectionFooter
                item { -> RenderFooter(styling = styling, safeAreaHeight = arguments.footerSafeAreaHeight, hasBottomSection = hasBottomSection) }
            }
        }
    }

    @Composable
    private fun RenderItem(content: Renderable, level: Int, context: ComposeContext, modifier: Modifier = Modifier, styling: ListStyling, isItem: Boolean = true) {
        if (content.isSwiftUIEmptyView) {
            return
        }

        val itemRenderable = (itemTransformer?.invoke(content) ?: content).sref()
        val listItemModifier = ListItemModifier.combined(for_ = itemRenderable)
        var itemModifier: Modifier = Modifier
        if (listItemModifier?.background == null) {
            itemModifier = itemModifier.background(BackgroundColor(styling = styling.withStyle(ListStyle.plain), isItem = isItem))
        }

        // The given modifiers include elevation shadow for dragging, etc that need to go before the others
        val containerContext = context.content(modifier = modifier.then(itemModifier).then(context.modifier))
        val contentContext = context.content()
        val contentModifier = Companion.contentModifier(level = level)
        val renderContainer: @Composable (ComposeContext) -> Unit = { context ->
            Column(modifier = context.modifier) { ->
                val placement = EnvironmentValues.shared._placement.sref()
                EnvironmentValues.shared.setValues(l@{ it ->
                    it.set_placement(placement.union(ViewPlacement.listItem))
                    return@l ComposeResult.ok
                }, in_ = { -> Companion.RenderItemContent(item = itemRenderable, context = contentContext, modifier = contentModifier) })
                if (listItemModifier?.separator != Visibility.hidden) {
                    Companion.RenderSeparator(level = level)
                }
            }
        }

        val matchtarget_2 = listItemModifier?.background
        if (matchtarget_2 != null) {
            val background = matchtarget_2
            TargetViewLayout(context = containerContext, isOverlay = false, alignment = Alignment.center, target = renderContainer, dependent = { it -> background.Compose(context = it) })
        } else {
            renderContainer(containerContext)
        }
    }

    @Composable
    private fun RenderEditableItem(content: Renderable, level: Int, context: ComposeContext, modifier: Modifier, styling: ListStyling, objectsBinding: Binding<RandomAccessCollection<Any>>? = null, key: String?, index: Int, editActions: EditActions = EditActions.of(), onDelete: ((IntSet) -> Unit)?, onMove: ((IntSet, Int) -> Unit)?, reorderableState: ReorderableLazyListState) {
        if (content.isSwiftUIEmptyView) {
            return
        }
        if (key == null) {
            RenderItem(content = content, level = level, context = context, modifier = modifier, styling = styling)
            return
        }
        val editActionsModifier = EditActionsModifier.combined(for_ = content)
        val isDeleteEnabled = (editActions.contains(EditActions.delete) || onDelete != null) && editActionsModifier.isDeleteDisabled != true
        val isMoveEnabled = (editActions.contains(EditActions.move) || onMove != null) && editActionsModifier.isMoveDisabled != true
        if (!isDeleteEnabled && !isMoveEnabled) {
            RenderItem(content = content, level = level, context = context, modifier = modifier, styling = styling)
            return
        }

        if (isDeleteEnabled) {
            val rememberedOnDelete = rememberUpdatedState({ ->
                if (onDelete != null) {
                    withAnimation { -> onDelete(IntSet(integer = index)) }
                } else if ((objectsBinding != null) && (objectsBinding.wrappedValue.count > index)) {
                    withAnimation { ->
                        (objectsBinding.wrappedValue as? RangeReplaceableCollection<Any>)?.remove(at = index)
                    }
                }
            })
            val coroutineScope = rememberCoroutineScope()
            val positionalThreshold = with(LocalDensity.current) { -> 164.dp.toPx() }
            val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = l@{ it ->
                if (it == SwipeToDismissBoxValue.EndToStart) {
                    coroutineScope.launch { -> rememberedOnDelete.value() }
                }
                return@l false
            }, positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold.sref())

            val itemContent: @Composable (Modifier) -> Unit = { it ->
                SwipeToDismissBox(state = dismissState, enableDismissFromEndToStart = true, enableDismissFromStartToEnd = false, modifier = it, backgroundContent = { ->
                    val trashVector = Image.composeImageVector(named = "trash")!!
                    Box(modifier = Modifier.background(androidx.compose.ui.graphics.Color.Red).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.CenterEnd) { -> Icon(imageVector = trashVector, contentDescription = "Delete", modifier = Modifier.padding(end = 24.dp), tint = androidx.compose.ui.graphics.Color.White) }
                }, content = { -> RenderItem(content = content, level = level, context = context, styling = styling) })
            }
            if (isMoveEnabled) {
                RenderReorderableItem(reorderableState = reorderableState, key = key, modifier = modifier, content = itemContent)
            } else {
                itemContent(modifier)
            }
        } else {
            RenderReorderableItem(reorderableState = reorderableState, key = key, modifier = modifier) { it -> RenderItem(content = content, level = level, context = context, modifier = it, styling = styling) }
        }
    }

    @Composable
    private fun RenderReorderableItem(reorderableState: ReorderableLazyListState, key: String, modifier: Modifier, content: @Composable (Modifier) -> Unit) {
        ReorderableItem(state = reorderableState, key = key, defaultDraggingModifier = modifier) { dragging ->
            var itemModifier = Modifier.detectReorderAfterLongPress(reorderableState)
            if (dragging) {
                val elevation = animateDpAsState(8.dp)
                itemModifier = itemModifier.shadow(elevation.value)
            }
            content(itemModifier)
        }
    }

    @Composable
    private fun RenderSectionHeader(content: Renderable, context: ComposeContext, styling: ListStyling, isTop: Boolean) {
        if (!isTop && styling.style != ListStyle.plain) {
            // Vertical padding
            RenderFooter(styling = styling, safeAreaHeight = 0.dp, hasBottomSection = true)
        }
        val backgroundColor = BackgroundColor(styling = styling, isItem = false)
        val modifier = Modifier
            .zIndex(0.5f)
            .background(backgroundColor)
            .then(context.modifier)
        var contentModifier = Modifier.fillMaxWidth()
        if (isTop && styling.style != ListStyle.plain) {
            contentModifier = contentModifier.padding(start = Companion.horizontalItemInset.dp, top = 0.dp, end = Companion.horizontalItemInset.dp, bottom = Companion.verticalItemInset.dp)
        } else {
            contentModifier = contentModifier.padding(horizontal = Companion.horizontalItemInset.dp, vertical = Companion.verticalItemInset.dp)
        }
        Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.BottomCenter) { ->
            Column(modifier = Modifier.fillMaxWidth()) { ->
                EnvironmentValues.shared.setValues(l@{ it ->
                    it.set_listSectionHeaderStyle(styling.style)
                    return@l ComposeResult.ok
                }, in_ = { -> content.Render(context = context.content(modifier = contentModifier)) })
            }
            if (styling.style != ListStyle.plain) {
                RenderRoundedCorners(isTop = true, fill = backgroundColor)
            }
        }
    }

    @Composable
    private fun RenderSectionFooter(content: Renderable, context: ComposeContext, styling: ListStyling) {
        if (styling.style == ListStyle.plain) {
            RenderItem(content = content, level = 0, context = context, styling = styling, isItem = false)
        } else {
            val backgroundColor = BackgroundColor(styling = styling, isItem = false)
            val modifier = Modifier.offset(y = -1.dp)
                .zIndex(0.5f)
                .background(backgroundColor)
                .then(context.modifier)
            val contentModifier = Modifier.fillMaxWidth().padding(horizontal = Companion.horizontalItemInset.dp, vertical = Companion.verticalItemInset.dp)
            Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.TopCenter) { ->
                Column(modifier = Modifier.fillMaxWidth().heightIn(min = 1.dp)) { ->
                    EnvironmentValues.shared.setValues(l@{ it ->
                        it.set_listSectionFooterStyle(styling.style)
                        return@l ComposeResult.ok
                    }, in_ = { -> content.Render(context = context.content(modifier = contentModifier)) })
                }
                RenderRoundedCorners(isTop = false, fill = backgroundColor)
            }
        }
    }

    /// - Warning: Only call for non-.plain styles or with a positive safe area height. This is distinct from having this function detect
    /// .plain and zero-height and return without rendering. That causes .plain style lists to have a weird rubber banding effect on overscroll.
    @Composable
    private fun RenderHeader(styling: ListStyling, safeAreaHeight: Dp, hasTopSection: Boolean) {
        var height = safeAreaHeight.sref()
        if (styling.style != ListStyle.plain) {
            height += Companion.verticalInset.dp.sref()
        }
        val backgroundColor = BackgroundColor(styling = styling, isItem = false)
        val modifier = Modifier.fillMaxWidth()
            .height(height)
            .zIndex(0.5f)
            .background(backgroundColor)
        Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.BottomCenter) { ->
            if (!hasTopSection && styling.style != ListStyle.plain) {
                RenderRoundedCorners(isTop = true, fill = backgroundColor)
            }
        }
    }

    /// - Warning: Only call for non-.plain styles or with a positive safe area height. This is distinct from having this function detect
    /// .plain and zero-height and return without rendering. That causes .plain style lists to have a weird rubber banding effect on overscroll.
    @Composable
    private fun RenderFooter(styling: ListStyling, safeAreaHeight: Dp, hasBottomSection: Boolean) {
        var height = safeAreaHeight.sref()
        var offset = 0.dp.sref()
        if (styling.style != ListStyle.plain) {
            height += Companion.verticalInset.dp.sref()
            offset = (-1.dp).sref() // Cover last row's divider
        }
        val backgroundColor = BackgroundColor(styling = styling, isItem = false)
        val modifier = Modifier.fillMaxWidth()
            .height(height)
            .offset(y = offset)
            .zIndex(0.5f)
            .background(backgroundColor)
        Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.TopCenter) { ->
            if (!hasBottomSection && styling.style != ListStyle.plain) {
                RenderRoundedCorners(isTop = false, fill = backgroundColor)
            }
        }
    }

    @Composable
    private fun RenderRoundedCorners(isTop: Boolean, fill: androidx.compose.ui.graphics.Color) {
        val shape = GenericShape { size, _ ->
            val rect = Rect(left = 0.0f, top = 0.0f, right = size.width, bottom = size.height)
            val rectPath = androidx.compose.ui.graphics.Path()
            rectPath.addRect(rect)
            val roundRect: RoundRect
            if (isTop) {
                roundRect = RoundRect(rect, topLeft = CornerRadius(size.height), topRight = CornerRadius(size.height))
            } else {
                roundRect = RoundRect(rect, bottomLeft = CornerRadius(size.height), bottomRight = CornerRadius(size.height))
            }
            val roundedRectPath = androidx.compose.ui.graphics.Path()
            roundedRectPath.addRoundRect(roundRect)
            addPath(combine(PathOperation.Difference, rectPath, roundedRectPath))
        }
        val offset = (if (isTop) listSectionCornerRadius.dp else -listSectionCornerRadius.dp).sref()
        val modifier = Modifier
            .fillMaxWidth()
            .height(listSectionCornerRadius.dp)
            .offset(y = offset)
            .clip(shape)
            .background(fill)
        Box(modifier = modifier)
    }

    @Composable
    private fun RenderSearchField(state: SearchableState, context: ComposeContext, styling: ListStyling, safeAreaHeight: Dp) {
        var modifier = Modifier.background(BackgroundColor(styling = styling, isItem = false))
        if (styling.style == ListStyle.plain) {
            modifier = modifier.padding(top = Companion.verticalInset.dp + safeAreaHeight, start = Companion.horizontalInset.dp, end = Companion.horizontalInset.dp, bottom = Companion.verticalInset.dp)
        } else {
            modifier = modifier.padding(top = Companion.verticalInset.dp + safeAreaHeight)
        }
        modifier = modifier.fillMaxWidth()
        SearchField(state = state, context = context.content(modifier = modifier))
    }

    @Composable
    private fun BackgroundColor(styling: ListStyling, isItem: Boolean): androidx.compose.ui.graphics.Color {
        if (!isItem && styling.backgroundVisibility == Visibility.hidden) {
            return Color.clear.colorImpl()
        } else if (styling.style == ListStyle.plain) {
            return Color.background.colorImpl()
        } else {
            return Color.systemBackground.colorImpl()
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {

        private val horizontalInset = 16.0
        private val verticalInset = 16.0
        private val minimumItemHeight = 32.0
        private val horizontalItemInset = 16.0
        private val verticalItemInset = 8.0
        private val levelInset = 24.0

        internal fun contentModifier(level: Int): Modifier = Modifier.padding(start = (horizontalItemInset + level * levelInset).dp, end = horizontalItemInset.dp, top = verticalItemInset.dp, bottom = verticalItemInset.dp).fillMaxWidth().requiredHeightIn(min = minimumItemHeight.dp)

        @Composable
        internal fun RenderSeparator(level: Int): Unit = androidx.compose.material3.Divider(modifier = Modifier.padding(start = (horizontalItemInset + level * levelInset).dp).fillMaxWidth(), color = Color.separator.colorImpl())

        @Composable
        internal fun RenderItemContent(item: Renderable, context: ComposeContext, modifier: Modifier) {
            val (isListItem, listItemAction) = item.shouldRenderListItem(context = context)
            if (isListItem) {
                val actionModifier: Modifier
                if (listItemAction != null) {
                    val isDisabled = !EnvironmentValues.shared.isEnabled || item.forEachModifier { it ->
                        (it as? DisabledModifier)?.disabled
                    } == true
                    actionModifier = Modifier.clickable(onClick = listItemAction, enabled = !isDisabled)
                } else {
                    actionModifier = Modifier
                }
                Box(modifier = actionModifier.then(modifier), contentAlignment = androidx.compose.ui.Alignment.CenterStart) { -> item.RenderListItem(context = context, modifiers = listOf()) }
            } else {
                Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.CenterStart) { -> item.Render(context = context) }
            }
        }
    }
}

// Kotlin does not support generic constructor parameters, so we have to model many List constructors as functions

//extension List {
//    public init<Data, RowContent>(_ data: Data, @ViewBuilder rowContent: @escaping (Data.Element) -> RowContent) where Content == ForEach<Data, Data.Element.ID, RowContent>, Data : RandomAccessCollection, RowContent : View, Data.Element : Identifiable
//}
fun <ObjectType> List(data: RandomAccessCollection<ObjectType>, rowContent: (ObjectType) -> View): List {
    return List(identifier = { it -> (it as Identifiable<Hashable>).id }, objects = data as RandomAccessCollection<Any>, objectContent = { it -> rowContent(it as ObjectType) })
}

//extension List {
//    public init<Data, ID, RowContent>(_ data: Data, id: KeyPath<Data.Element, ID>, @ViewBuilder rowContent: @escaping (Data.Element) -> RowContent) where Content == ForEach<Data, ID, RowContent>, Data : RandomAccessCollection, ID : Hashable, RowContent : View
//}
fun <ObjectType> List(data: RandomAccessCollection<ObjectType>, id: (ObjectType) -> AnyHashable?, rowContent: (ObjectType) -> View): List where ObjectType: Any {
    return List(identifier = { it -> id(it as ObjectType) }, objects = data as RandomAccessCollection<Any>, objectContent = { it -> rowContent(it as ObjectType) })
}
fun List(data: IntRange, id: ((Int) -> AnyHashable?)? = null, rowContent: (Int) -> View): List {
    return List(identifier = if (id == null) null else { it -> id!!(it as Int) }, indexRange = data, indexedContent = rowContent)
}

//extension List {
//  public init<Data, RowContent>(_ data: Binding<Data>, editActions: EditActions /* <Data> */, @ViewBuilder rowContent: @escaping (Binding<Data.Element>) -> RowContent) where Content == ForEach<IndexedIdentifierCollection<Data, Data.Element.ID>, Data.Element.ID, EditableCollectionContent<RowContent, Data>>, Data : MutableCollection, Data : RandomAccessCollection, RowContent : View, Data.Element : Identifiable, Data.Index : Hashable
//}
fun <Data, ObjectType> List(data: Binding<Data>, editActions: EditActions = EditActions.of(), rowContent: (Binding<ObjectType>) -> View): List where Data: RandomAccessCollection<ObjectType> {
    return List(identifier = { it -> (it as Identifiable<Hashable>).id }, objectsBinding = data as Binding<RandomAccessCollection<Any>>, objectsBindingContent = l@{ data, index ->
        val binding = Binding<ObjectType>(get = { -> data.wrappedValue[index] as ObjectType }, set = { it -> (data.wrappedValue as skip.lib.MutableCollection<ObjectType>)[index] = it.sref() })
        return@l rowContent(binding)
    }, editActions = editActions)
}

//extension List {
//  public init<Data, ID, RowContent>(_ data: Binding<Data>, id: KeyPath<Data.Element, ID>, editActions: EditActions /* <Data> */, @ViewBuilder rowContent: @escaping (Binding<Data.Element>) -> RowContent) where Content == ForEach<IndexedIdentifierCollection<Data, ID>, ID, EditableCollectionContent<RowContent, Data>>, Data : MutableCollection, Data : RandomAccessCollection, ID : Hashable, RowContent : View, Data.Index : Hashable
//}
fun <Data, ObjectType> List(data: Binding<Data>, id: (ObjectType) -> AnyHashable?, editActions: EditActions = EditActions.of(), rowContent: (Binding<ObjectType>) -> View): List where Data: RandomAccessCollection<ObjectType> {
    return List(identifier = { it -> id(it as ObjectType) }, objectsBinding = data as Binding<RandomAccessCollection<Any>>, objectsBindingContent = l@{ data, index ->
        val binding = Binding<ObjectType>(get = { -> data.wrappedValue[index] as ObjectType }, set = { it -> (data.wrappedValue as skip.lib.MutableCollection<ObjectType>)[index] = it.sref() })
        return@l rowContent(binding)
    }, editActions = editActions)
}

internal class ListStyling {
    internal val style: ListStyle
    internal val backgroundVisibility: Visibility

    internal fun withStyle(style: ListStyle): ListStyling = ListStyling(style = style, backgroundVisibility = backgroundVisibility)

    constructor(style: ListStyle, backgroundVisibility: Visibility) {
        this.style = style
        this.backgroundVisibility = backgroundVisibility
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ListStyling) return false
        return style == other.style && backgroundVisibility == other.backgroundVisibility
    }
}

@Stable
internal class ListArguments {
    internal val headerSafeAreaHeight: Dp
    internal val footerSafeAreaHeight: Dp
    internal val safeAreaEdges: Edge.Set

    constructor(headerSafeAreaHeight: Dp, footerSafeAreaHeight: Dp, safeAreaEdges: Edge.Set) {
        this.headerSafeAreaHeight = headerSafeAreaHeight.sref()
        this.footerSafeAreaHeight = footerSafeAreaHeight.sref()
        this.safeAreaEdges = safeAreaEdges.sref()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ListArguments) return false
        return headerSafeAreaHeight == other.headerSafeAreaHeight && footerSafeAreaHeight == other.footerSafeAreaHeight && safeAreaEdges == other.safeAreaEdges
    }
}

class ListStyle: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ListStyle) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = ListStyle(rawValue = 0) // For bridging

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val sidebar = ListStyle(rawValue = 1) // For bridging

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val insetGrouped = ListStyle(rawValue = 2) // For bridging

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val grouped = ListStyle(rawValue = 3) // For bridging

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val inset = ListStyle(rawValue = 4) // For bridging

        val plain = ListStyle(rawValue = 5) // For bridging
    }
}

sealed class ListItemTint {
    class FixedCase(val associated0: Color): ListItemTint() {
    }
    class PreferredCase(val associated0: Color): ListItemTint() {
    }
    class MonochromeCase: ListItemTint() {
    }

    @androidx.annotation.Keep
    companion object {
        fun fixed(associated0: Color): ListItemTint = FixedCase(associated0)
        fun preferred(associated0: Color): ListItemTint = PreferredCase(associated0)
        val monochrome: ListItemTint = MonochromeCase()
    }
}

sealed class ListSectionSpacing {
    class DefaultCase: ListSectionSpacing() {
    }
    class CompactCase: ListSectionSpacing() {
    }
    class CustomCase(val associated0: Double): ListSectionSpacing() {
    }

    @androidx.annotation.Keep
    companion object {
        val default: ListSectionSpacing = DefaultCase()
        val compact: ListSectionSpacing = CompactCase()
        fun custom(associated0: Double): ListSectionSpacing = CustomCase(associated0)
    }
}

internal class ListItemModifier: RenderModifier {
    internal val background: View?
    internal val separator: Visibility?

    internal constructor(background: View? = null, separator: Visibility? = null): super() {
        this.background = background.sref()
        this.separator = separator
    }

    @androidx.annotation.Keep
    companion object {

        internal fun combined(for_: Renderable): ListItemModifier {
            val renderable = for_
            var background: View? = null
            var separator: Visibility? = null
            renderable.forEachModifier l@{ it ->
                (it as? ListItemModifier)?.let { listItemModifier ->
                    background = (background ?: listItemModifier.background).sref()
                    separator = separator ?: listItemModifier.separator
                }
                return@l null
            }
            return ListItemModifier(background = background, separator = separator)
        }
    }
}

