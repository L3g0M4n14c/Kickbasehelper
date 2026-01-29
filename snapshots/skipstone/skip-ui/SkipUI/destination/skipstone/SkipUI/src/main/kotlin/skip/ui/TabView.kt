package skip.ui

import kotlin.reflect.KClass
import skip.lib.*
import skip.lib.Array
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@androidx.annotation.Keep
class TabView: View, Renderable, skip.lib.SwiftProjecting {
    internal val selection: Binding<Any>?
    internal val content: ComposeBuilder

    constructor(content: () -> View) {
        this.selection = null
        this.content = ComposeBuilder.from(content)
    }

    constructor(selection: Any?, content: () -> View) {
        this.selection = (selection as Binding<Any>?).sref()
        this.content = ComposeBuilder.from(content)
    }

    constructor(selectionGet: (() -> Any)?, selectionSet: ((Any) -> Unit)?, bridgedContent: View) {
        if ((selectionGet != null) && (selectionSet != null)) {
            this.selection = Binding(get = selectionGet, set = selectionSet)
        } else {
            this.selection = null
        }
        this.content = ComposeBuilder.from { -> bridgedContent }
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val matchtarget_0 = EnvironmentValues.shared._tabViewStyle as? PageTabViewStyle
        if (matchtarget_0 != null) {
            val pageTabViewStyle = matchtarget_0
            RenderPageViewContent(indexDisplayMode = pageTabViewStyle.indexDisplayMode, context = context)
        } else {
            RenderTabViewContent(context = context)
        }
    }

    @Composable
    private fun RenderPageViewContent(indexDisplayMode: PageTabViewStyle.IndexDisplayMode, context: ComposeContext) {
        // WARNING: This function is a potential recomposition hotspot
        val contentContext = context.content()
        val tabRenderables = EvaluateContent(context = contentContext)
        val tags = tabRenderables.map { it -> tabTagValue(for_ = it) }
        val coroutineScope = rememberCoroutineScope()
        val isSyncingToSelection = remember { -> mutableStateOf(false) }
        val pagerState = rememberPagerState(pageCount = { -> tabRenderables.size })
        ComposeContainer(modifier = context.modifier, fillWidth = true) { modifier ->
            Box(modifier = modifier) { ->
                syncPagerStateToSelection(pagerState, tags = tags, isSyncingToSelection = isSyncingToSelection, coroutineScope = coroutineScope)
                RenderPageViewPager(pagerState = pagerState, tabRenderables = tabRenderables, tags = tags, isSyncingToSelection = isSyncingToSelection, context = contentContext)
                if (indexDisplayMode == PageTabViewStyle.IndexDisplayMode.always || (indexDisplayMode == PageTabViewStyle.IndexDisplayMode.automatic && tabRenderables.size > 1)) {
                    val modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                    RenderPageViewIndicator(pagerState = pagerState, modifier = modifier, coroutineScope = coroutineScope, context = contentContext)
                }
            }
        }
    }

    @Composable
    private fun RenderPageViewPager(pagerState: PagerState, tabRenderables: kotlin.collections.List<Renderable>, tags: kotlin.collections.List<Any?>, isSyncingToSelection: MutableState<Boolean>, context: ComposeContext) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page >= 0 && page < tabRenderables.size) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { -> tabRenderables[page].Render(context = context) }
                // We don't get a callback when the user scrolls the pager, so use the rendering callback to sync any
                // user-initiated navigation to the selection binding
                syncSelectionToPagerState(pagerState, tags = tags, isSyncingToSelection = isSyncingToSelection)
            }
        }
    }

    @Composable
    private fun syncPagerStateToSelection(pagerState: PagerState, tags: kotlin.collections.List<Any?>, isSyncingToSelection: MutableState<Boolean>, coroutineScope: CoroutineScope) {
        val selectedTag_0 = selection?.wrappedValue.sref()
        if (selectedTag_0 == null) {
            return
        }
        val selectedPageState = rememberUpdatedState(tags.indexOfFirst { it -> it == selectedTag_0 })
        val selectedPage = selectedPageState.value.sref()
        if (selectedPage == -1 || selectedPage == pagerState.targetPage) {
            return
        }
        if (pagerState.isScrollInProgress && !isSyncingToSelection.value) {
            return
        }
        val isWithAnimationState = rememberUpdatedState(Animation.isInWithAnimation)
        // Track that we're syncing to the selection so that we don't try to sync the other way while waiting for
        // the coroutine to launch, or confuse the resulting scrolling with user scrolling
        isSyncingToSelection.value = true
        coroutineScope.launch { ->
            val selectedPage = selectedPageState.value.sref()
            if (selectedPage != -1) {
                if (pagerState.isScrollInProgress || isWithAnimationState.value) {
                    pagerState.animateScrollToPage(selectedPage)
                } else {
                    pagerState.scrollToPage(selectedPage)
                }
                isSyncingToSelection.value = false
            }
        }
    }

    @Composable
    private fun syncSelectionToPagerState(pagerState: PagerState, tags: kotlin.collections.List<Any?>, isSyncingToSelection: MutableState<Boolean>) {
        if (isSyncingToSelection.value) {
            return
        }
        if (pagerState.targetPage < 0 || pagerState.targetPage >= tags.size) {
            return
        }
        val targetTag_0 = tags[pagerState.targetPage].sref()
        if (targetTag_0 == null) {
            return
        }
        val selectedTag_1 = selection?.wrappedValue.sref()
        if ((selectedTag_1 == null) || (selectedTag_1 == targetTag_0)) {
            return
        }
        selection?.wrappedValue = targetTag_0
    }

    // https://developer.android.com/develop/ui/compose/layouts/pager#add-page
    @Composable
    private fun RenderPageViewIndicator(pagerState: PagerState, modifier: Modifier, coroutineScope: CoroutineScope, context: ComposeContext) {
        Row(modifier = modifier, horizontalArrangement = Arrangement.Center) { ->
            for (indicatorPage in 0 until pagerState.pageCount) {
                val isCurrentPage = pagerState.targetPage == indicatorPage
                val buttonModifier = context.modifier.clickable(onClick = { ->
                    coroutineScope.launch { -> pagerState.animateScrollToPage(indicatorPage) }
                }, enabled = !isCurrentPage)
                Box(modifier = buttonModifier) { ->
                    val indicatorColor = (if (isCurrentPage) Color.white else Color.white.opacity(0.5)).sref()
                    val indicatorDynamicSize = (8.dp * LocalDensity.current.fontScale).sref()
                    val indicatorModifier = Modifier
                        .padding(horizontal = indicatorDynamicSize)
                        .clip(CircleShape)
                        .background(indicatorColor.colorImpl())
                        .size(indicatorDynamicSize)
                    Box(modifier = indicatorModifier)
                }
            }
        }
    }

    @Composable
    private fun RenderTabViewContent(context: ComposeContext) {
        // WARNING: This function is a potential recomposition hotspot. It should not need to be called on every tab
        // change. Test after any modification

        val tabContext = context.content()
        val tabRenderables = EvaluateContent(context = tabContext)
        val tabs: kotlin.collections.List<Tab?> = tabRenderables.map l@{ it ->
            val renderable = (it as Renderable).sref() // Let transpiler understand type
            val matchtarget_1 = renderable.strip() as? Tab
            if (matchtarget_1 != null) {
                val tab = matchtarget_1
                return@l tab
            } else {
                val matchtarget_2 = renderable.forEachModifier(perform = { it -> it as? TabItemModifier }) as? TabItemModifier
                if (matchtarget_2 != null) {
                    val tabItemModifier = matchtarget_2
                    return@l Tab(content = { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            renderable.asView().Compose(composectx)
                            ComposeResult.ok
                        }
                    }, label = { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            tabItemModifier.label.Compose(composectx)
                            ComposeResult.ok
                        }
                    })
                } else {
                    return@l null
                }
            }
        }

        val navController = rememberNavController()
        // Isolate access to current route within child Composable so route nav does not force us to recompose
        navigateToCurrentRoute(controller = navController, tabRenderables = tabRenderables)

        val tabBarPreferences = rememberSaveable(stateSaver = context.stateSaver as Saver<Preference<ToolbarBarPreferences>, Any>) { -> mutableStateOf(Preference<ToolbarBarPreferences>(key = TabBarPreferenceKey::class)) }
        val tabBarPreferencesCollector = PreferenceCollector<ToolbarBarPreferences>(key = TabBarPreferenceKey::class, state = tabBarPreferences)

        val safeArea = EnvironmentValues.shared._safeArea
        val density = LocalDensity.current.sref()
        val defaultBottomBarHeight = 80.dp.sref()
        val bottomBarTopPx = remember { ->
            // Default our initial value to the expected value, which helps avoid visual artifacts as we measure actual values and
            // recompose with adjusted layouts
            if (safeArea != null) {
                mutableStateOf(with(density) { -> safeArea.presentationBoundsPx.bottom - defaultBottomBarHeight.toPx() })
            } else {
                mutableStateOf(0.0f)
            }
        }
        val bottomBarHeightPx = remember { ->
            mutableStateOf(with(density) { -> defaultBottomBarHeight.toPx() })
        }

        // Reduce the tab bar preferences outside the bar composable. Otherwise the reduced value may change
        // when the bottom bar recomposes
        val reducedTabBarPreferences = tabBarPreferences.value.reduced.sref()
        val bottomBar: @Composable () -> Unit = l@{ ->
            if (!tabs.any({ it -> it != null }) || reducedTabBarPreferences.visibility == Visibility.hidden) {
                SideEffect { ->
                    bottomBarTopPx.value = 0.0f
                    bottomBarHeightPx.value = 0.0f
                }
                return@l
            }
            var tabBarModifier = Modifier.fillMaxWidth()
                .onGloballyPositionedInWindow { bounds ->
                    bottomBarTopPx.value = bounds.top
                    bottomBarHeightPx.value = bounds.bottom - bounds.top
                }
            val tint = EnvironmentValues.shared._tint.sref()
            val hasColorScheme = reducedTabBarPreferences.colorScheme != null
            val isSystemBackground = reducedTabBarPreferences.isSystemBackground == true
            val showScrolledBackground = reducedTabBarPreferences.backgroundVisibility == Visibility.visible || reducedTabBarPreferences.scrollableState?.canScrollForward == true
            val materialColorScheme: androidx.compose.material3.ColorScheme
            if (showScrolledBackground) {
                val matchtarget_3 = reducedTabBarPreferences.colorScheme?.asMaterialTheme()
                if (matchtarget_3 != null) {
                    val customColorScheme = matchtarget_3
                    materialColorScheme = customColorScheme.sref()
                } else {
                    materialColorScheme = MaterialTheme.colorScheme.sref()
                }
            } else {
                materialColorScheme = MaterialTheme.colorScheme.sref()
            }
            MaterialTheme(colorScheme = materialColorScheme) { ->
                val indicatorColor: androidx.compose.ui.graphics.Color
                if (tint != null) {
                    indicatorColor = tint.asComposeColor().copy(alpha = 0.35f)
                } else {
                    indicatorColor = if (ColorScheme.fromMaterialTheme(colorScheme = materialColorScheme) == ColorScheme.dark) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f)
                }
                val tabBarBackgroundColor: androidx.compose.ui.graphics.Color
                val unscrolledTabBarBackgroundColor: androidx.compose.ui.graphics.Color
                val tabBarBackgroundForBrush: ShapeStyle?
                val tabBarItemColors: NavigationBarItemColors
                if (reducedTabBarPreferences.backgroundVisibility == Visibility.hidden) {
                    tabBarBackgroundColor = androidx.compose.ui.graphics.Color.Transparent
                    unscrolledTabBarBackgroundColor = androidx.compose.ui.graphics.Color.Transparent
                    tabBarBackgroundForBrush = null
                    tabBarItemColors = NavigationBarItemDefaults.colors(indicatorColor = indicatorColor)
                } else {
                    val matchtarget_4 = reducedTabBarPreferences.background
                    if (matchtarget_4 != null) {
                        val background = matchtarget_4
                        val matchtarget_5 = background.asColor(opacity = 1.0, animationContext = null)
                        if (matchtarget_5 != null) {
                            val color = matchtarget_5
                            tabBarBackgroundColor = color
                            unscrolledTabBarBackgroundColor = if (isSystemBackground) Color.systemBarBackground.colorImpl() else color.copy(alpha = 0.0f)
                            tabBarBackgroundForBrush = null
                        } else {
                            unscrolledTabBarBackgroundColor = if (isSystemBackground) Color.systemBarBackground.colorImpl() else androidx.compose.ui.graphics.Color.Transparent
                            tabBarBackgroundColor = unscrolledTabBarBackgroundColor.copy(alpha = 0.0f)
                            tabBarBackgroundForBrush = background.sref()
                        }
                        tabBarItemColors = NavigationBarItemDefaults.colors(indicatorColor = indicatorColor)
                    } else {
                        tabBarBackgroundColor = Color.systemBarBackground.colorImpl()
                        unscrolledTabBarBackgroundColor = if (isSystemBackground) tabBarBackgroundColor else tabBarBackgroundColor.copy(alpha = 0.0f)
                        tabBarBackgroundForBrush = null
                        if (tint == null) {
                            tabBarItemColors = NavigationBarItemDefaults.colors()
                        } else {
                            tabBarItemColors = NavigationBarItemDefaults.colors(indicatorColor = indicatorColor)
                        }
                    }
                }
                if (showScrolledBackground && (tabBarBackgroundForBrush != null)) {
                    tabBarBackgroundForBrush.asBrush(opacity = 1.0, animationContext = null)?.let { tabBarBackgroundBrush ->
                        tabBarModifier = tabBarModifier.background(tabBarBackgroundBrush)
                    }
                }

                val currentRoute = currentRoute(for_ = navController) // Note: forces recompose of this context on tab navigation
                // Pull the tab bar below the keyboard
                val bottomPadding = with(density) { -> min(bottomBarHeightPx.value, Float(WindowInsets.ime.getBottom(density))).toDp() }
                PaddingLayout(padding = EdgeInsets(top = 0.0, leading = 0.0, bottom = Double(-bottomPadding.value), trailing = 0.0), context = context.content()) { context ->
                    val tabsState = rememberUpdatedState(tabs)
                    val containerColor = if (showScrolledBackground) tabBarBackgroundColor else unscrolledTabBarBackgroundColor
                    val onItemClick: (Int) -> Unit = { tabIndex ->
                        val route = String(describing = tabIndex)
                        if (selection != null) {
                            val matchtarget_6 = tagValue(route = route, in_ = tabRenderables)
                            if (matchtarget_6 != null) {
                                val tagValue = matchtarget_6
                                selection.wrappedValue = tagValue
                            } else {
                                navigate(controller = navController, route = route)
                            }
                        } else {
                            navigate(controller = navController, route = route)
                        }
                    }
                    val itemIcon: @Composable (Int) -> Unit = { tabIndex ->
                        val tab = tabsState.value[tabIndex].sref()
                        tab?.RenderImage(context = tabContext)
                    }
                    val itemLabel: @Composable (Int) -> Unit = { tabIndex ->
                        val tab = tabsState.value[tabIndex].sref()
                        tab?.RenderTitle(context = tabContext)
                    }
                    var options = Material3NavigationBarOptions(modifier = context.modifier.then(tabBarModifier), containerColor = containerColor, contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor), onItemClick = onItemClick, itemIcon = itemIcon, itemLabel = itemLabel, itemColors = tabBarItemColors)
                    EnvironmentValues.shared._material3NavigationBar?.let { updateOptions ->
                        options = updateOptions(options)
                    }
                    NavigationBar(modifier = options.modifier, containerColor = options.containerColor, contentColor = options.contentColor, tonalElevation = options.tonalElevation) { ->
                        for (tabIndex in 0 until tabRenderables.size) {
                            if (tabs[tabIndex]?.isHidden == true) {
                                continue
                            }
                            val route = String(describing = tabIndex)
                            val label: (@Composable () -> Unit)?
                            val matchtarget_7 = options.itemLabel
                            if (matchtarget_7 != null) {
                                val itemLabel = matchtarget_7
                                label = { -> itemLabel(tabIndex) }
                            } else {
                                label = null
                            }
                            NavigationBarItem(selected = route == currentRoute, onClick = { -> options.onItemClick(tabIndex) }, icon = { -> options.itemIcon(tabIndex) }, modifier = options.itemModifier(tabIndex), enabled = options.itemEnabled(tabIndex) && tabs[tabIndex]?.isDisabled != true, label = label, alwaysShowLabel = options.alwaysShowItemLabels, colors = options.itemColors, interactionSource = options.itemInteractionSource)
                        }
                    }
                }
            }
        }

        // When we layout, extend into the safe area if it is due to system bars, not into any app chrome. We extend
        // into the top bar too so that tab content can also extend into the top area without getting cut off during
        // tab switches
        var ignoresSafeAreaEdges: Edge.Set = Edge.Set.of(Edge.Set.bottom, Edge.Set.top)
        ignoresSafeAreaEdges.formIntersection(safeArea?.absoluteSystemBarEdges ?: Edge.Set.of())
        IgnoresSafeAreaLayout(expandInto = ignoresSafeAreaEdges) { _, _ ->
            ComposeContainer(modifier = context.modifier, fillWidth = true, fillHeight = true) { modifier ->
                // Don't use a Scaffold: it clips content beyond its bounds and prevents .ignoresSafeArea modifiers from working
                Column(modifier = modifier.background(Color.background.colorImpl())) { ->
                    NavHost(navController, modifier = Modifier.fillMaxWidth().weight(1.0f), startDestination = "0", enterTransition = { -> fadeIn() }, exitTransition = { -> fadeOut() }) { ->
                        // Use a constant number of routes. Changing routes causes a NavHost to reset its state
                        val entryContext = context.content()
                        for (tabIndex in 0 until 100) {
                            composable(String(describing = tabIndex)) { _ ->
                                // Inset manually where our container ignored the safe area, but we aren't showing a bar
                                val topPadding = (if (ignoresSafeAreaEdges.contains(Edge.Set.top)) WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() else 0.dp).sref()
                                var bottomPadding = 0.dp.sref()
                                if (bottomBarTopPx.value <= 0.0f && ignoresSafeAreaEdges.contains(Edge.Set.bottom)) {
                                    bottomPadding = max(0.dp, WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding() - WindowInsets.ime.asPaddingValues().calculateBottomPadding())
                                }
                                val contentModifier = Modifier.fillMaxSize().padding(top = topPadding, bottom = bottomPadding)
                                val contentSafeArea = safeArea?.insetting(Edge.bottom, to = bottomBarTopPx.value)

                                // Special-case the first composition to avoid seeing the layout adjust. This is a common
                                // issue with nav stacks in particular, and they're common enough that we need to cater to them.
                                // Use an extra container to avoid causing the content itself to recompose
                                val hasComposed = remember { -> mutableStateOf(false) }
                                SideEffect { -> hasComposed.value = true }
                                val alpha = if (hasComposed.value) 1.0f else 0.0f
                                Box(modifier = Modifier.alpha(alpha), contentAlignment = androidx.compose.ui.Alignment.Center) { ->
                                    // This block is called multiple times on tab switch. Use stable arguments that will prevent our entry from
                                    // recomposing when called with the same values
                                    val arguments = TabEntryArguments(tabIndex = tabIndex, modifier = contentModifier, safeArea = contentSafeArea)
                                    PreferenceValues.shared.collectPreferences(arrayOf(tabBarPreferencesCollector)) { -> RenderEntry(with = arguments, context = entryContext) }
                                }
                            }
                        }
                    }
                    bottomBar()
                }
            }
        }
    }

    @Composable
    private fun RenderEntry(with: TabEntryArguments, context: ComposeContext) {
        val arguments = with
        // WARNING: This function is a potential recomposition hotspot. It should not need to be called
        // multiple times for the same tab on tab change. Test after modifications
        Box(modifier = arguments.modifier, contentAlignment = androidx.compose.ui.Alignment.Center) { ->
            EnvironmentValues.shared.setValues(l@{ it ->
                arguments.safeArea?.let { safeArea ->
                    it.set_safeArea(safeArea)
                }
                return@l ComposeResult.ok
            }, in_ = { ->
                val renderables = EvaluateContent(context = context)
                if (renderables.size > arguments.tabIndex) {
                    renderables[arguments.tabIndex].Render(context = context)
                }
            })
        }
    }

    @Composable
    private fun EvaluateContent(context: ComposeContext): kotlin.collections.List<Renderable> {
        // Evaluate our content without recursively evaluating every custom tab view. We only want to fully
        // evaluate views when we render them
        val options = EvaluateOptions(isKeepNonModified = true).value
        val renderables = content.Evaluate(context = context, options = options)
        var tabContent: kotlin.collections.MutableList<Renderable> = mutableListOf()
        for (renderable in renderables.sref()) {
            // Expand through sections
            val matchtarget_8 = renderable as? TabSection
            if (matchtarget_8 != null) {
                val tabSection = matchtarget_8
                tabContent.addAll(tabSection.Evaluate(context = context, options = options))
            } else {
                tabContent.add(renderable)
            }
        }
        return tabContent.sref()
    }

    private fun tagValue(route: String, in_: kotlin.collections.List<Renderable>): Any? {
        val tabRenderables = in_
        val tabIndex_0 = Int(string = route)
        if ((tabIndex_0 == null) || (tabIndex_0 < 0) || (tabIndex_0 >= tabRenderables.size)) {
            return null
        }
        return tabTagValue(for_ = tabRenderables[tabIndex_0])
    }

    private fun route(tagValue: Any, in_: kotlin.collections.List<Renderable>): String? {
        val tabRenderables = in_
        for (tabIndex in 0 until tabRenderables.size) {
            val tabTagValue = tabTagValue(for_ = tabRenderables[tabIndex])
            if (tagValue == tabTagValue) {
                return String(describing = tabIndex)
            }
        }
        return null
    }

    private fun tabTagValue(for_: Renderable): Any? {
        val renderable = for_
        val matchtarget_9 = renderable.strip() as? Tab
        if (matchtarget_9 != null) {
            val tab = matchtarget_9
            val matchtarget_10 = tab.value
            if (matchtarget_10 != null) {
                val value = matchtarget_10
                return value.sref()
            } else {
                return TagModifier.on(content = renderable, role = ModifierRole.tag)?.value.sref()
            }
        } else {
            return TagModifier.on(content = renderable, role = ModifierRole.tag)?.value.sref()
        }
    }

    private fun navigate(controller: NavHostController, route: String) {
        val navController = controller
        navController.navigate(route) { ->
            // Clear back stack so that tabs don't participate in Android system back button
            val destinationID = (navController.currentBackStackEntry?.destination?.id ?: navController.graph.startDestinationId).sref()
            popUpTo(destinationID) { ->
                inclusive = true
                saveState = true
            }
            // Avoid multiple copies of the same destination when reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }
    }

    @Composable
    private fun navigateToCurrentRoute(controller: NavHostController, tabRenderables: kotlin.collections.List<Renderable>) {
        val navController = controller
        val currentRoute = currentRoute(for_ = navController)
        if ((selection != null) && (currentRoute != null) && (selection.wrappedValue != tagValue(route = currentRoute, in_ = tabRenderables))) {
            route(tagValue = selection.wrappedValue, in_ = tabRenderables)?.let { route ->
                navigate(controller = navController, route = route)
            }
        }
    }

    @Composable
    private fun currentRoute(for_: NavHostController): String? {
        val navController = for_
        // In your BottomNavigation composable, get the current NavBackStackEntry using the currentBackStackEntryAsState() function. This entry gives you access to the current NavDestination. The selected state of each BottomNavigationItem can then be determined by comparing the item's route with the route of the current destination and its parent destinations (to handle cases when you are using nested navigation) via the NavDestination hierarchy.
        return navController.currentBackStackEntryAsState().value?.destination?.route
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

@Stable
internal class TabEntryArguments {
    internal val tabIndex: Int
    internal val modifier: Modifier
    internal val safeArea: SafeArea?

    constructor(tabIndex: Int, modifier: Modifier, safeArea: SafeArea? = null) {
        this.tabIndex = tabIndex
        this.modifier = modifier
        this.safeArea = safeArea
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TabEntryArguments) return false
        return tabIndex == other.tabIndex && modifier == other.modifier && safeArea == other.safeArea
    }
}

@androidx.annotation.Keep
internal class TabBarPreferenceKey: PreferenceKey<ToolbarBarPreferences> {

    @androidx.annotation.Keep
    companion object: PreferenceKeyCompanion<ToolbarBarPreferences> {
        override val defaultValue = ToolbarBarPreferences()

        override fun reduce(value: InOut<ToolbarBarPreferences>, nextValue: () -> ToolbarBarPreferences) {
            value.value = value.value.reduce(nextValue())
        }
    }
}

internal class TabItemModifier: RenderModifier {
    internal val label: ComposeBuilder

    internal constructor(label: () -> View): super() {
        this.label = ComposeBuilder.from(label)
    }
}

// MARK: TabViewStyle

interface TabViewStyle {
}
interface TabViewStyleCompanion {
}

class DefaultTabViewStyle: TabViewStyle {

    constructor() {
    }

    override fun equals(other: Any?): Boolean = other is DefaultTabViewStyle

    @androidx.annotation.Keep
    companion object: TabViewStyleCompanion {
        internal val identifier = 0 // For bridging

        val automatic: DefaultTabViewStyle
            get() = DefaultTabViewStyle()
    }
}

class TabBarOnlyTabViewStyle: TabViewStyle {

    constructor() {
    }

    override fun equals(other: Any?): Boolean = other is TabBarOnlyTabViewStyle

    @androidx.annotation.Keep
    companion object: TabViewStyleCompanion {
        internal val identifier = 1 // For bridging

        val tabBarOnly: TabBarOnlyTabViewStyle
            get() = TabBarOnlyTabViewStyle()
    }
}

class PageTabViewStyle: TabViewStyle {

    val indexDisplayMode: PageTabViewStyle.IndexDisplayMode

    class IndexDisplayMode: RawRepresentable<Int> {
        override val rawValue: Int

        constructor(rawValue: Int) {
            this.rawValue = rawValue
        }

        override fun equals(other: Any?): Boolean {
            if (other !is PageTabViewStyle.IndexDisplayMode) return false
            return rawValue == other.rawValue
        }

        @androidx.annotation.Keep
        companion object {

            val automatic = IndexDisplayMode(rawValue = 0) // For bridging
            val always = IndexDisplayMode(rawValue = 1) // For bridging
            val never = IndexDisplayMode(rawValue = 2) // For bridging
        }
    }

    constructor(indexDisplayMode: PageTabViewStyle.IndexDisplayMode = PageTabViewStyle.IndexDisplayMode.automatic) {
        this.indexDisplayMode = indexDisplayMode
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PageTabViewStyle) return false
        return indexDisplayMode == other.indexDisplayMode
    }

    @androidx.annotation.Keep
    companion object: TabViewStyleCompanion {
        internal val identifier = 2 // For bridging

        val page: PageTabViewStyle
            get() = PageTabViewStyle()

        fun page(indexDisplayMode: PageTabViewStyle.IndexDisplayMode): PageTabViewStyle = PageTabViewStyle(indexDisplayMode = indexDisplayMode)
    }
}

// MARK: Tab

class TabBarMinimizeBehavior: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TabBarMinimizeBehavior) return false
        return rawValue == other.rawValue
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, rawValue)
        return result
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = TabBarMinimizeBehavior(rawValue = 1)
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val onScrollDown = TabBarMinimizeBehavior(rawValue = 2)
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val onScrollUp = TabBarMinimizeBehavior(rawValue = 3)
        val never = TabBarMinimizeBehavior(rawValue = 4)
    }
}

enum class AdaptableTabBarPlacement {
    automatic,
    tabBar,
    sidebar;

    @androidx.annotation.Keep
    companion object {
    }
}

interface TabContent: View {
    var isHidden: Boolean
    var isDisabled: Boolean

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun accessibilityValue(valueDescription: Text, isEnabled: Boolean): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityValue(valueKey: LocalizedStringKey, isEnabled: Boolean = true): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityValue(valueResource: LocalizedStringResource, isEnabled: Boolean = true): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityValue(value: String, isEnabled: Boolean = true): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun accessibilityLabel(label: Text, isEnabled: Boolean): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun accessibilityLabel(labelKey: LocalizedStringKey, isEnabled: Boolean): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun accessibilityLabel(label: LocalizedStringResource, isEnabled: Boolean): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun accessibilityLabel(label: String, isEnabled: Boolean): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityHint(hint: Text, isEnabled: Boolean = true): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityHint(hintKey: LocalizedStringKey, isEnabled: Boolean = true): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityHint(hint: LocalizedStringResource, isEnabled: Boolean = true): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityHint(hint: String, isEnabled: Boolean = true): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun customizationBehavior(behavior: TabCustomizationBehavior, vararg for_: AdaptableTabBarPlacement): TabContent {
        val placements = Array(for_.asIterable())
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun customizationID(id: String): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <Content> sectionActions(content: () -> Content): TabContent where Content: View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dropDestination(for_: KClass<*>? = null, action: (Array<*>) -> Unit): TabContent {
        val payloadType = for_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun springLoadingBehavior(behavior: SpringLoadingBehavior): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun draggable(payload: Any): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun badge(count: Int): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun badge(label: Text?): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun badge(key: LocalizedStringKey): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun badge(resource: LocalizedStringResource): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun badge(label: String): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun popover(isPresented: Binding<Boolean>, attachmentAnchor: Any? = null, arrowEdge: Edge? = null, content: () -> View): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun popover(item: Binding<Any?>, attachmentAnchor: Any? = null, arrowEdge: Edge? = null, content: (Any) -> View): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityInputLabels(inputLabels: Array<*>, isEnabled: Boolean = true): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun swipeActions(edge: HorizontalEdge, allowsFullSwipe: Boolean, content: () -> View): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun contextMenu(menuItems: () -> View): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun accessibilityIdentifier(identifier: String, isEnabled: Boolean): TabContent = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun defaultVisibility(visibility: Visibility, vararg for_: AdaptableTabBarPlacement): TabContent {
        val placements = Array(for_.asIterable())
        return this.sref()
    }

    fun hidden(hidden: Boolean = true): TabContent {
        var tabContent = this.sref()
        tabContent.isHidden = hidden
        return tabContent.sref()
    }

    override fun disabled(disabled: Boolean): TabContent {
        var tabContent = this.sref()
        tabContent.isDisabled = disabled
        return tabContent.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun tabPlacement(placement: TabPlacement): TabContent = this.sref()
}

@androidx.annotation.Keep
class Tab: TabContent, Renderable, MutableStruct, skip.lib.SwiftProjecting {
    internal val label: ComposeBuilder
    internal val content: ComposeBuilder
    internal val value: Any?

    constructor(title: String, image: String, value: Any?, role: TabRole? = null, content: () -> View, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) {
        this.label = ComposeBuilder(view = Label(title, image = image))
        this.content = ComposeBuilder.from { -> content() }
        this.value = value.sref()
    }

    constructor(titleKey: LocalizedStringKey, image: String, value: Any?, role: TabRole? = null, content: () -> View, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) {
        this.label = ComposeBuilder(view = Label(titleKey, image = image))
        this.content = ComposeBuilder.from { -> content() }
        this.value = value.sref()
    }

    constructor(titleResource: LocalizedStringResource, image: String, value: Any?, role: TabRole? = null, content: () -> View, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) {
        this.label = ComposeBuilder(view = Label(titleResource, image = image))
        this.content = ComposeBuilder.from { -> content() }
        this.value = value.sref()
    }

    constructor(title: String, systemImage: String, value: Any?, role: TabRole? = null, content: () -> View) {
        this.label = ComposeBuilder(view = Label(title, systemImage = systemImage))
        this.content = ComposeBuilder.from { -> content() }
        this.value = value.sref()
    }

    constructor(titleKey: LocalizedStringKey, systemImage: String, value: Any?, role: TabRole? = null, content: () -> View) {
        this.label = ComposeBuilder(view = Label(titleKey, systemImage = systemImage))
        this.content = ComposeBuilder.from { -> content() }
        this.value = value.sref()
    }

    constructor(titleResource: LocalizedStringResource, systemImage: String, value: Any?, role: TabRole? = null, content: () -> View) {
        this.label = ComposeBuilder(view = Label(titleResource, systemImage = systemImage))
        this.content = ComposeBuilder.from { -> content() }
        this.value = value.sref()
    }

    constructor(value: Any?, role: TabRole? = null, content: () -> View) {
        this.content = ComposeBuilder.from { -> content() }
        this.value = value.sref()
        if (role == TabRole.search) {
            this.label = ComposeBuilder(view = Label(LocalizedStringKey(stringLiteral = "Search"), systemImage = "magnifyingglass"))
        } else {
            this.label = ComposeBuilder(view = EmptyView())
        }
    }

    constructor(value: Any?, role: TabRole? = null, content: () -> View, label: () -> View) {
        this.label = ComposeBuilder.from { -> label() }
        this.content = ComposeBuilder.from { -> content() }
        this.value = value.sref()
    }

    constructor(value: Any?, bridgedRole: Int?, bridgedContent: View, bridgedLabel: View?) {
        if (bridgedLabel != null) {
            this.label = ComposeBuilder.from { -> bridgedLabel }
        } else if (bridgedRole == TabRole.search.rawValue) {
            this.label = ComposeBuilder(view = Label(LocalizedStringKey(stringLiteral = "Search"), systemImage = "magnifyingglass"))
        } else {
            this.label = ComposeBuilder(view = EmptyView())
        }
        this.content = ComposeBuilder.from { -> bridgedContent }
        this.value = value.sref()
    }

    constructor(title: String, image: String, role: TabRole? = null, content: () -> View, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null): this(title, image = image, value = null, role = role, content = content) {
    }

    constructor(titleKey: LocalizedStringKey, image: String, role: TabRole? = null, content: () -> View, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null): this(titleKey, image = image, value = null, role = role, content = content) {
    }

    constructor(titleResource: LocalizedStringResource, image: String, role: TabRole? = null, content: () -> View, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null): this(titleResource, image = image, value = null, role = role, content = content) {
    }

    constructor(title: String, systemImage: String, role: TabRole? = null, content: () -> View): this(title, systemImage = systemImage, value = null, role = role, content = content) {
    }

    constructor(titleKey: LocalizedStringKey, systemImage: String, role: TabRole? = null, content: () -> View): this(titleKey, systemImage = systemImage, value = null, role = role, content = content) {
    }

    constructor(titleResource: LocalizedStringResource, systemImage: String, role: TabRole? = null, content: () -> View): this(titleResource, systemImage = systemImage, value = null, role = role, content = content) {
    }

    constructor(role: TabRole? = null, content: () -> View): this(value = null, role = role, content = content) {
    }

    constructor(role: TabRole? = null, content: () -> View, label: () -> View): this(value = null, role = role, content = content, label = label) {
    }

    override var isHidden = false
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    override var isDisabled = false
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    @Composable
    override fun Render(context: ComposeContext) {
        content.Compose(context = context)
    }

    @Composable
    internal fun RenderTitle(context: ComposeContext) {
        val renderable = label.Evaluate(context = context, options = 0).firstOrNull() ?: EmptyView()
        val stripped = renderable.strip()
        val matchtarget_11 = stripped as? Label
        if (matchtarget_11 != null) {
            val label = matchtarget_11
            label.RenderTitle(context = context)
        } else if (stripped is Text) {
            renderable.Render(context = context)
        }
    }

    @Composable
    internal fun RenderImage(context: ComposeContext) {
        val renderable = label.Evaluate(context = context, options = 0).firstOrNull() ?: EmptyView()
        val stripped = renderable.strip()
        val matchtarget_12 = stripped as? Label
        if (matchtarget_12 != null) {
            val label = matchtarget_12
            label.RenderImage(context = context)
        } else if (stripped is Image) {
            renderable.Render(context = context)
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as Tab
        this.label = copy.label
        this.content = copy.content
        this.value = copy.value
        this.isHidden = copy.isHidden
        this.isDisabled = copy.isDisabled
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = Tab(this as MutableStruct)

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

class TabCustomizationBehavior {

    override fun equals(other: Any?): Boolean = other is TabCustomizationBehavior

    @androidx.annotation.Keep
    companion object {
        val automatic = TabCustomizationBehavior()
        val reorderable = TabCustomizationBehavior()
        val disabled = TabCustomizationBehavior()
    }
}

class TabPlacement {

    override fun equals(other: Any?): Boolean = other is TabPlacement

    override fun hashCode(): Int = "TabPlacement".hashCode()

    @androidx.annotation.Keep
    companion object {
        val automatic = TabPlacement()
        val pinned = TabPlacement()
        val sidebarOnly = TabPlacement()
    }
}

enum class TabRole(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
    search(1); // For bridging

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: Int): TabRole? {
            return when (rawValue) {
                1 -> TabRole.search
                else -> null
            }
        }
    }
}

fun TabRole(rawValue: Int): TabRole? = TabRole.init(rawValue = rawValue)

@androidx.annotation.Keep
class TabSection: TabContent, Renderable, MutableStruct, skip.lib.SwiftProjecting {
    private val content: ComposeBuilder

    constructor(content: () -> View, header: () -> View) {
        this.content = ComposeBuilder.from { -> content() }
    }

    constructor(content: () -> View) {
        this.content = ComposeBuilder.from { -> content() }
    }

    constructor(title: String, content: () -> View) {
        this.content = ComposeBuilder.from { -> content() }
    }

    constructor(titleKey: LocalizedStringKey, content: () -> View) {
        this.content = ComposeBuilder.from { -> content() }
    }

    constructor(titleResource: LocalizedStringResource, content: () -> View) {
        this.content = ComposeBuilder.from { -> content() }
    }

    constructor(bridgedContent: View) {
        this.content = ComposeBuilder.from { -> bridgedContent }
    }

    override var isHidden = false
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    override var isDisabled = false
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    @Composable
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val renderables = content.Evaluate(context = context, options = options)
        for (renderable in renderables.sref()) {
            if (isHidden) {
                (renderable as? TabContent)?.isHidden = true
            }
            if (isDisabled) {
                (renderable as? TabContent)?.isDisabled = true
            }
        }
        return renderables.sref()
    }

    @Composable
    override fun Render(context: ComposeContext) {
        // We should never render directly, but we are collected as a Renderable in tab view content
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as TabSection
        this.content = copy.content
        this.isHidden = copy.isHidden
        this.isDisabled = copy.isDisabled
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = TabSection(this as MutableStruct)

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

@Suppress("MUST_BE_INITIALIZED")
class Material3NavigationBarOptions: MutableStruct {
    var modifier: Modifier
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var containerColor: androidx.compose.ui.graphics.Color
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var contentColor: androidx.compose.ui.graphics.Color
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var tonalElevation: Dp
        get() = field.sref({ this.tonalElevation = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var onItemClick: (Int) -> Unit
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var itemIcon: @Composable (Int) -> Unit
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var itemModifier: @Composable (Int) -> Modifier
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var itemEnabled: (Int) -> Boolean
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var itemLabel: (@Composable (Int) -> Unit)? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var alwaysShowItemLabels: Boolean
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var itemColors: NavigationBarItemColors
        get() = field.sref({ this.itemColors = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var itemInteractionSource: MutableInteractionSource? = null
        get() = field.sref({ this.itemInteractionSource = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    fun copy(modifier: Modifier = this.modifier, containerColor: androidx.compose.ui.graphics.Color = this.containerColor, contentColor: androidx.compose.ui.graphics.Color = this.contentColor, tonalElevation: Dp = this.tonalElevation, onItemClick: (Int) -> Unit = this.onItemClick, itemIcon: @Composable (Int) -> Unit = this.itemIcon, itemModifier: @Composable (Int) -> Modifier = this.itemModifier, itemEnabled: (Int) -> Boolean = this.itemEnabled, itemLabel: (@Composable (Int) -> Unit)? = this.itemLabel, alwaysShowItemLabels: Boolean = this.alwaysShowItemLabels, itemColors: NavigationBarItemColors = this.itemColors, itemInteractionSource: MutableInteractionSource? = this.itemInteractionSource): Material3NavigationBarOptions = Material3NavigationBarOptions(modifier = modifier, containerColor = containerColor, contentColor = contentColor, tonalElevation = tonalElevation, onItemClick = onItemClick, itemIcon = itemIcon, itemModifier = itemModifier, itemEnabled = itemEnabled, itemLabel = itemLabel, alwaysShowItemLabels = alwaysShowItemLabels, itemColors = itemColors, itemInteractionSource = itemInteractionSource)

    constructor(modifier: Modifier = Modifier, containerColor: androidx.compose.ui.graphics.Color, contentColor: androidx.compose.ui.graphics.Color, tonalElevation: Dp = NavigationBarDefaults.Elevation.sref(), onItemClick: (Int) -> Unit, itemIcon: @Composable (Int) -> Unit, itemModifier: @Composable (Int) -> Modifier = { _ -> Modifier }, itemEnabled: (Int) -> Boolean = { _ -> true }, itemLabel: (@Composable (Int) -> Unit)? = null, alwaysShowItemLabels: Boolean = true, itemColors: NavigationBarItemColors, itemInteractionSource: MutableInteractionSource? = null) {
        this.modifier = modifier
        this.containerColor = containerColor
        this.contentColor = contentColor
        this.tonalElevation = tonalElevation
        this.onItemClick = onItemClick
        this.itemIcon = itemIcon
        this.itemModifier = itemModifier
        this.itemEnabled = itemEnabled
        this.itemLabel = itemLabel
        this.alwaysShowItemLabels = alwaysShowItemLabels
        this.itemColors = itemColors
        this.itemInteractionSource = itemInteractionSource
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = Material3NavigationBarOptions(modifier, containerColor, contentColor, tonalElevation, onItemClick, itemIcon, itemModifier, itemEnabled, itemLabel, alwaysShowItemLabels, itemColors, itemInteractionSource)

    @androidx.annotation.Keep
    companion object {
    }
}
