package skip.ui

import kotlin.reflect.KClass
import skip.lib.*
import skip.lib.Array
import skip.lib.Sequence
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlin.reflect.full.superclasses
import kotlinx.coroutines.delay

@androidx.annotation.Keep
class NavigationStack: View, Renderable, skip.lib.SwiftProjecting {
    internal val root: ComposeBuilder
    internal val path: Binding<Array<*>?>?
    internal val navigationPath: Binding<NavigationPath>?
    internal val destinationKeyTransformer: ((Any) -> String)?

    constructor(root: () -> View) {
        this.root = ComposeBuilder.from(root)
        this.path = null
        this.navigationPath = null
        this.destinationKeyTransformer = null
    }

    constructor(path: Binding<NavigationPath>, root: () -> View) {
        this.root = ComposeBuilder.from(root)
        this.path = null
        this.navigationPath = path.sref()
        this.destinationKeyTransformer = null
    }

    constructor(path: Any, root: () -> View) {
        this.root = ComposeBuilder.from(root)
        this.path = (path as Binding<Array<*>?>?)?.sref()
        this.navigationPath = null
        this.destinationKeyTransformer = null
    }

    constructor(getData: (() -> Array<*>?)?, setData: ((Array<*>?) -> Unit)?, bridgedRoot: View, destinationKeyTransformer: (Any) -> String) {
        this.root = ComposeBuilder.from { -> bridgedRoot }
        this.navigationPath = null
        if ((getData != null) && (setData != null)) {
            this.path = Binding<Array<*>?>(get = getData, set = setData)
        } else {
            this.path = null
        }
        this.destinationKeyTransformer = destinationKeyTransformer
    }

    @Composable
    override fun Render(context: ComposeContext) {
        // Have to use rememberSaveable for e.g. a nav stack in each tab
        val destinations = rememberSaveable(stateSaver = context.stateSaver as Saver<Preference<Dictionary<AnyHashable, NavigationDestination>>, Any>) { -> mutableStateOf(Preference<Dictionary<AnyHashable, NavigationDestination>>(key = NavigationDestinationsPreferenceKey::class)) }
        // Make this collector non-erasable so that destinations defined at e.g. the root nav stack layer don't disappear when you push
        val destinationsCollector = PreferenceCollector<Dictionary<AnyHashable, NavigationDestination>>(key = NavigationDestinationsPreferenceKey::class, state = destinations, isErasable = false)
        val reducedDestinations = destinations.value.reduced.sref()
        val navController = rememberNavController()
        val navigator = rememberSaveable(stateSaver = context.stateSaver as Saver<Navigator, Any>) { -> mutableStateOf(Navigator(navController = navController, destinations = reducedDestinations, destinationKeyTransformer = destinationKeyTransformer)) }
        navigator.value.didCompose(navController = navController, destinations = reducedDestinations, path = path, navigationPath = navigationPath, keyboardController = LocalSoftwareKeyboardController.current)

        val providedNavigator = LocalNavigator provides navigator.value
        CompositionLocalProvider(providedNavigator) { ->
            val safeArea = EnvironmentValues.shared._safeArea
            // We have to ignore the safe area around the entire NavHost to prevent push/pop animation issues with the system bars.
            // When we layout, only extend into safe areas that are due to system bars, not into any app chrome
            var ignoresSafeAreaEdges: Edge.Set = Edge.Set.of(Edge.Set.top, Edge.Set.bottom)
            ignoresSafeAreaEdges.formIntersection(safeArea?.absoluteSystemBarEdges ?: Edge.Set.of())
            IgnoresSafeAreaLayout(expandInto = ignoresSafeAreaEdges) { _, _ ->
                ComposeContainer(modifier = context.modifier, fillWidth = true, fillHeight = true) { modifier ->
                    val isRTL = EnvironmentValues.shared.layoutDirection == LayoutDirection.rightToLeft
                    NavHost(navController = navController, startDestination = Navigator.rootRoute, modifier = modifier) { ->
                        composable(route = Navigator.rootRoute, exitTransition = { ->
                            fadeOut(animationSpec = tween(durationMillis = 200)) + slideOutHorizontally(targetOffsetX = { it -> it * (if (isRTL) 1 else -1) / 3 })
                        }, popEnterTransition = { ->
                            fadeIn() + slideInHorizontally(initialOffsetX = { it -> it * (if (isRTL) 1 else -1) / 3 })
                        }) l@{ entry ->
                            val state_0 = navigator.value.state(for_ = entry)
                            if (state_0 == null) {
                                return@l
                            }
                            // These preferences are per-entry, but if we put them in RenderEntry then their initial values don't show
                            // during the navigation animation. We have to collect them here
                            val title = rememberSaveable(stateSaver = state_0.stateSaver as Saver<Preference<Text>, Any>) { -> mutableStateOf(Preference<Text>(key = NavigationTitlePreferenceKey::class)) }
                            val titleCollector = PreferenceCollector<Text>(key = NavigationTitlePreferenceKey::class, state = title)
                            val toolbarPreferences = rememberSaveable(stateSaver = state_0.stateSaver as Saver<Preference<ToolbarPreferences>, Any>) { -> mutableStateOf(Preference<ToolbarPreferences>(key = ToolbarPreferenceKey::class)) }
                            val toolbarPreferencesCollector = PreferenceCollector<ToolbarPreferences>(key = ToolbarPreferenceKey::class, state = toolbarPreferences)
                            val toolbarContentPreferences = rememberSaveable(stateSaver = state_0.stateSaver as Saver<Preference<ToolbarContentPreferences>, Any>) { -> mutableStateOf(Preference<ToolbarContentPreferences>(key = ToolbarContentPreferenceKey::class)) }
                            val toolbarContentPreferencesCollector = PreferenceCollector<ToolbarContentPreferences>(key = ToolbarContentPreferenceKey::class, state = toolbarContentPreferences)
                            val arguments = NavigationEntryArguments(isRoot = true, state = state_0, safeArea = safeArea, ignoresSafeAreaEdges = ignoresSafeAreaEdges, title = title.value.reduced, toolbarPreferences = toolbarPreferences.value.reduced)
                            PreferenceValues.shared.collectPreferences(arrayOf(titleCollector, toolbarPreferencesCollector, toolbarContentPreferencesCollector, destinationsCollector)) { ->
                                RenderEntry(navigator = navigator, toolbarContent = toolbarContentPreferences, arguments = arguments, context = context) { context -> root.Compose(context = context) }
                            }
                        }
                        for (destinationIndex in 0 until Navigator.destinationCount) {
                            composable(route = Navigator.route(for_ = destinationIndex, valueString = "{identifier}"), arguments = listOf(navArgument("identifier") { -> type = NavType.StringType }), enterTransition = { ->
                                fadeIn() + slideInHorizontally(initialOffsetX = { it -> it * (if (isRTL) -1 else 1) / 3 })
                            }, exitTransition = { ->
                                fadeOut(animationSpec = tween(durationMillis = 200)) + slideOutHorizontally(targetOffsetX = { it -> it * (if (isRTL) 1 else -1) / 3 })
                            }, popEnterTransition = { ->
                                fadeIn() + slideInHorizontally(initialOffsetX = { it -> it * (if (isRTL) 1 else -1) / 3 })
                            }, popExitTransition = { ->
                                fadeOut(animationSpec = tween(durationMillis = 200)) + slideOutHorizontally(targetOffsetX = { it -> it * (if (isRTL) -1 else 1) / 3 })
                            }) l@{ entry ->
                                val state_1 = navigator.value.state(for_ = entry)
                                if (state_1 == null) {
                                    return@l
                                }
                                val targetValue_0 = state_1.targetValue.sref()
                                if (targetValue_0 == null) {
                                    return@l
                                }
                                // These preferences are per-entry, but if we put them in RenderEntry then their initial values don't show
                                // during the navigation animation. We have to collect them here
                                val title = rememberSaveable(stateSaver = state_1.stateSaver as Saver<Preference<Text>, Any>) { -> mutableStateOf(Preference<Text>(key = NavigationTitlePreferenceKey::class)) }
                                val titleCollector = PreferenceCollector<Text>(key = NavigationTitlePreferenceKey::class, state = title)
                                val toolbarPreferences = rememberSaveable(stateSaver = state_1.stateSaver as Saver<Preference<ToolbarPreferences>, Any>) { -> mutableStateOf(Preference<ToolbarPreferences>(key = ToolbarPreferenceKey::class)) }
                                val toolbarPreferencesCollector = PreferenceCollector<ToolbarPreferences>(key = ToolbarPreferenceKey::class, state = toolbarPreferences)
                                val toolbarContentPreferences = rememberSaveable(stateSaver = state_1.stateSaver as Saver<Preference<ToolbarContentPreferences>, Any>) { -> mutableStateOf(Preference<ToolbarContentPreferences>(key = ToolbarContentPreferenceKey::class)) }
                                val toolbarContentPreferencesCollector = PreferenceCollector<ToolbarContentPreferences>(key = ToolbarContentPreferenceKey::class, state = toolbarContentPreferences)
                                EnvironmentValues.shared.setValues(l@{ it ->
                                    it.setdismiss(DismissAction(action = { -> navigator.value.navigateBack() }))
                                    return@l ComposeResult.ok
                                }, in_ = { ->
                                    val arguments = NavigationEntryArguments(isRoot = false, state = state_1, safeArea = safeArea, ignoresSafeAreaEdges = ignoresSafeAreaEdges, title = title.value.reduced, toolbarPreferences = toolbarPreferences.value.reduced)
                                    PreferenceValues.shared.collectPreferences(arrayOf(titleCollector, toolbarPreferencesCollector, toolbarContentPreferencesCollector, destinationsCollector)) { ->
                                        RenderEntry(navigator = navigator, toolbarContent = toolbarContentPreferences, arguments = arguments, context = context) { context ->
                                            val destinationArguments = NavigationDestinationArguments(targetValue = targetValue_0)
                                            RenderDestination(state_1.destination, arguments = destinationArguments, context = context)
                                        }
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    private fun RenderEntry(navigator: MutableState<Navigator>, toolbarContent: MutableState<Preference<ToolbarContentPreferences>>, arguments: NavigationEntryArguments, context: ComposeContext, content: @Composable (ComposeContext) -> Unit) {
        val state = arguments.state
        val context = context.content(stateSaver = state.stateSaver)

        val topBarPreferences = arguments.toolbarPreferences.navigationBar
        val topBarHidden = remember { -> mutableStateOf(false) }
        val bottomBarPreferences = arguments.toolbarPreferences.bottomBar
        val hasTitle = arguments.title != NavigationTitlePreferenceKey.defaultValue
        val effectiveTitleDisplayMode = navigator.value.titleDisplayMode(for_ = state, hasTitle = hasTitle, preference = arguments.toolbarPreferences.titleDisplayMode)
        val isInlineTitleDisplayMode = useInlineTitleDisplayMode(for_ = effectiveTitleDisplayMode, safeArea = arguments.safeArea)

        // We would like to only process toolbar content in our topBar/bottomBar Composables, but composing
        // custom ToolbarContent multiple times (in order to process the placement of the items in its body
        // content for each bar) prevents it from updating properly on recompose
        val toolbarContentReduced = toolbarContent.value.reduced.sref()
        val toolbarItems = ToolbarItems(content = toolbarContentReduced.content ?: arrayOf())
        val (titleMenu, topLeadingItems, topTrailingItems, bottomItems) = toolbarItems.Evaluate(context = context)

        val searchFieldPadding = 16.dp.sref()
        val density = LocalDensity.current.sref()
        val searchFieldHeightPx = with(density) { -> searchFieldHeight.dp.toPx() + searchFieldPadding.toPx() }
        val searchFieldOffsetPx = rememberSaveable(stateSaver = context.stateSaver as Saver<Float, Any>) { -> mutableStateOf(0.0f) }
        val searchFieldScrollConnection = remember { -> SearchFieldScrollConnection(heightPx = searchFieldHeightPx, offsetPx = searchFieldOffsetPx) }

        val searchableStatePreference = rememberSaveable(stateSaver = context.stateSaver as Saver<Preference<SearchableState?>, Any>) { -> mutableStateOf(Preference<SearchableState?>(key = SearchableStatePreferenceKey::class)) }
        val searchableStateCollector = PreferenceCollector<SearchableState?>(key = SearchableStatePreferenceKey::class, state = searchableStatePreference)

        val scrollToTop = rememberSaveable(stateSaver = context.stateSaver as Saver<Preference<ScrollToTopAction>, Any>) { -> mutableStateOf(Preference<ScrollToTopAction>(key = ScrollToTopPreferenceKey::class)) }
        val scrollToTopCollector = PreferenceCollector<ScrollToTopAction>(key = ScrollToTopPreferenceKey::class, state = scrollToTop)

        val initialScrollBehavior = if (isInlineTitleDisplayMode) TopAppBarDefaults.pinnedScrollBehavior() else TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        // Determine the final scrollBehavior early by checking if the environment value would modify it
        // We need to do this before we create the nestedScroll modifier so we attach the correct nestedScrollConnection
        val scrollBehavior: TopAppBarScrollBehavior
        val matchtarget_0 = EnvironmentValues.shared._material3TopAppBar
        if (matchtarget_0 != null) {
            val updateOptions = matchtarget_0
            val tempOptions = Material3TopAppBarOptions(title = { ->  }, modifier = Modifier, navigationIcon = { ->  }, colors = TopAppBarDefaults.topAppBarColors(), scrollBehavior = initialScrollBehavior)
            val updatedOptions = updateOptions(tempOptions)
            scrollBehavior = (updatedOptions.scrollBehavior ?: initialScrollBehavior).sref()
        } else {
            scrollBehavior = initialScrollBehavior.sref()
        }
        var modifier = Modifier.nestedScroll(searchFieldScrollConnection)
        if (!topBarHidden.value) {
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        }
        modifier = modifier.then(context.modifier)

        // Intercept system back button to keep our state in sync
        BackHandler(enabled = !arguments.isRoot) { ->
            if (arguments.toolbarPreferences.backButtonHidden != true) {
                navigator.value.navigateBack()
            }
        }

        val topBarBottomPx = remember { ->
            // Default our initial value to the expected value, which helps avoid visual artifacts as we measure actual values and
            // recompose with adjusted layouts
            val safeAreaTopPx = arguments.safeArea?.safeBoundsPx?.top ?: 0.0f
            mutableStateOf(with(density) { -> safeAreaTopPx + 112.dp.toPx() })
        }

        val isSystemBackground = topBarPreferences?.isSystemBackground == true
        val topBar: @Composable () -> Unit = l@{ ->
            if (topBarPreferences?.visibility == Visibility.hidden) {
                SideEffect { ->
                    topBarHidden.value = true
                    topBarBottomPx.value = 0.0f
                }
                return@l
            }
            if (arguments.isRoot && !hasTitle && topLeadingItems.size <= 0 && topTrailingItems.size <= 0 && topBarPreferences?.visibility != Visibility.visible) {
                SideEffect { ->
                    topBarHidden.value = true
                    topBarBottomPx.value = 0.0f
                }
                return@l
            }
            topBarHidden.value = false

            val isOverlapped = scrollBehavior.state.overlappedFraction > 0
            val materialColorScheme: androidx.compose.material3.ColorScheme
            if (isOverlapped) {
                val matchtarget_1 = topBarPreferences?.colorScheme?.asMaterialTheme()
                if (matchtarget_1 != null) {
                    val customColorScheme = matchtarget_1
                    materialColorScheme = customColorScheme.sref()
                } else {
                    materialColorScheme = MaterialTheme.colorScheme.sref()
                }
            } else {
                materialColorScheme = MaterialTheme.colorScheme.sref()
            }
            MaterialTheme(colorScheme = materialColorScheme) { ->
                val topBarBackgroundColor: androidx.compose.ui.graphics.Color
                val unscrolledTopBarBackgroundColor: androidx.compose.ui.graphics.Color
                val topBarBackgroundForBrush: ShapeStyle?
                // If there is a custom color scheme, we also always show any custom background even when unscrolled, because we can't
                // properly interpolate between the title text colors
                val topBarHasColorScheme = topBarPreferences?.colorScheme != null
                val isSystemBackground = topBarPreferences?.isSystemBackground == true
                if (topBarPreferences?.backgroundVisibility == Visibility.hidden) {
                    topBarBackgroundColor = androidx.compose.ui.graphics.Color.Transparent
                    unscrolledTopBarBackgroundColor = androidx.compose.ui.graphics.Color.Transparent
                    topBarBackgroundForBrush = null
                } else {
                    val matchtarget_2 = topBarPreferences?.background
                    if (matchtarget_2 != null) {
                        val background = matchtarget_2
                        val matchtarget_3 = background.asColor(opacity = 1.0, animationContext = null)
                        if (matchtarget_3 != null) {
                            val color = matchtarget_3
                            topBarBackgroundColor = color
                            unscrolledTopBarBackgroundColor = if (isSystemBackground) Color.systemBarBackground.colorImpl() else color.copy(alpha = 0.0f)
                            topBarBackgroundForBrush = null
                        } else {
                            unscrolledTopBarBackgroundColor = if (isSystemBackground) Color.systemBarBackground.colorImpl() else androidx.compose.ui.graphics.Color.Transparent
                            topBarBackgroundColor = if (!topBarHasColorScheme || isOverlapped) unscrolledTopBarBackgroundColor.copy(alpha = 0.0f) else unscrolledTopBarBackgroundColor
                            topBarBackgroundForBrush = background.sref()
                        }
                    } else {
                        topBarBackgroundColor = Color.systemBarBackground.colorImpl()
                        unscrolledTopBarBackgroundColor = if (isSystemBackground) topBarBackgroundColor else topBarBackgroundColor.copy(alpha = 0.0f)
                        topBarBackgroundForBrush = null
                    }
                }

                val tint = (EnvironmentValues.shared._tint ?: Color(colorImpl = { -> MaterialTheme.colorScheme.onSurface })).sref()
                val placement = EnvironmentValues.shared._placement.sref()
                EnvironmentValues.shared.setValues(l@{ it ->
                    it.set_placement(placement.union(ViewPlacement.toolbar))
                    it.set_tint(tint)
                    return@l ComposeResult.ok
                }, in_ = { ->
                    val interactionSource = remember { -> MutableInteractionSource() }
                    var topBarModifier = Modifier.zIndex(1.1f)
                        .clickable(interactionSource = interactionSource, indication = null, onClick = { -> scrollToTop.value.reduced.action() })
                        .onGloballyPositionedInWindow { it -> topBarBottomPx.value = it.bottom }
                    if ((!topBarHasColorScheme || isOverlapped) && (topBarBackgroundForBrush != null)) {
                        val opacity = if (topBarHasColorScheme) 1.0 else if (isInlineTitleDisplayMode) min(1.0, Double(scrollBehavior.state.overlappedFraction * 5)) else Double(scrollBehavior.state.collapsedFraction)
                        topBarBackgroundForBrush.asBrush(opacity = opacity, animationContext = null)?.let { topBarBackgroundBrush ->
                            topBarModifier = topBarModifier.background(topBarBackgroundBrush)
                        }
                    }
                    val alwaysShowScrolledBackground = topBarPreferences?.backgroundVisibility == Visibility.visible
                    val topBarColors = TopAppBarDefaults.topAppBarColors(containerColor = if (alwaysShowScrolledBackground) topBarBackgroundColor else unscrolledTopBarBackgroundColor, scrolledContainerColor = topBarBackgroundColor, titleContentColor = MaterialTheme.colorScheme.onSurface)
                    val topBarTitle: @Composable () -> Unit = { ->
                        if (titleMenu != null) {
                            val menuModifier = Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = { -> titleMenu.toggleMenu() })
                            val arrangement = Arrangement.spacedBy(2.dp, alignment = androidx.compose.ui.Alignment.CenterHorizontally)
                            Row(modifier = menuModifier, horizontalArrangement = arrangement, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { ->
                                androidx.compose.material3.Text(arguments.title.localizedTextString(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Image(systemName = "chevron.down").accessibilityHidden(true).Compose(context = context)
                            }
                            titleMenu.Render(context = context)
                        } else {
                            androidx.compose.material3.Text(arguments.title.localizedTextString(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    val topBarNavigationIcon: @Composable () -> Unit = { ->
                        val hasBackButton = !arguments.isRoot && arguments.toolbarPreferences.backButtonHidden != true
                        if (hasBackButton || topLeadingItems.size > 0) {
                            val toolbarItemContext = context.content(modifier = Modifier.padding(start = 12.dp, end = 12.dp))
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { ->
                                if (hasBackButton) {
                                    IconButton(onClick = { -> navigator.value.navigateBack() }) { ->
                                        val isRTL = EnvironmentValues.shared.layoutDirection == LayoutDirection.rightToLeft
                                        Icon(imageVector = (if (isRTL) Icons.Filled.ArrowForward else Icons.Filled.ArrowBack), contentDescription = "Back", tint = tint.colorImpl())
                                    }
                                }
                                for (renderable in topLeadingItems.sref()) {
                                    renderable.Render(context = toolbarItemContext)
                                }
                            }
                        }
                    }
                    val topBarActions: @Composable () -> Unit = { ->
                        val toolbarItemContext = context.content(modifier = Modifier.padding(start = 12.dp, end = 12.dp))
                        for (renderable in topTrailingItems.sref()) {
                            renderable.Render(context = toolbarItemContext)
                        }
                    }
                    var options = Material3TopAppBarOptions(title = topBarTitle, modifier = topBarModifier, navigationIcon = topBarNavigationIcon, colors = topBarColors, scrollBehavior = scrollBehavior)
                    EnvironmentValues.shared._material3TopAppBar?.let { updateOptions ->
                        options = updateOptions(options)
                    }
                    // Use scrollBehavior (from the early call) for the TopAppBar to ensure it matches the nestedScrollConnection
                    options = options.copy(scrollBehavior = scrollBehavior)
                    if (isInlineTitleDisplayMode) {
                        if (options.preferCenterAlignedStyle) {
                            CenterAlignedTopAppBar(title = options.title, modifier = options.modifier, navigationIcon = options.navigationIcon, actions = { -> topBarActions() }, colors = options.colors, scrollBehavior = options.scrollBehavior)
                        } else {
                            TopAppBar(title = options.title, modifier = options.modifier, navigationIcon = options.navigationIcon, actions = { -> topBarActions() }, colors = options.colors, scrollBehavior = options.scrollBehavior)
                        }
                    } else {
                        // Force a larger, bold title style in the uncollapsed state by replacing the headlineSmall style the bar uses
                        val typography = MaterialTheme.typography.sref()
                        val appBarTitleStyle = typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                        val appBarTypography = typography.copy(headlineSmall = appBarTitleStyle)
                        MaterialTheme(colorScheme = MaterialTheme.colorScheme, typography = appBarTypography, shapes = MaterialTheme.shapes) { ->
                            if (options.preferLargeStyle) {
                                LargeTopAppBar(title = options.title, modifier = options.modifier, navigationIcon = options.navigationIcon, actions = { -> topBarActions() }, colors = options.colors, scrollBehavior = options.scrollBehavior)
                            } else {
                                MediumTopAppBar(title = options.title, modifier = options.modifier, navigationIcon = options.navigationIcon, actions = { -> topBarActions() }, colors = options.colors, scrollBehavior = options.scrollBehavior)
                            }
                        }
                    }
                })
            }
        }

        val bottomBarTopPx = remember { -> mutableStateOf(0.0f) }
        val bottomBarHeightPx = remember { -> mutableStateOf(0.0f) }
        val bottomBar: @Composable () -> Unit = l@{ ->
            if (bottomBarPreferences?.visibility == Visibility.hidden) {
                SideEffect { ->
                    bottomBarTopPx.value = 0.0f
                    bottomBarHeightPx.value = 0.0f
                }
                return@l
            }
            if (bottomItems.size <= 0 && bottomBarPreferences?.visibility != Visibility.visible) {
                SideEffect { ->
                    bottomBarTopPx.value = 0.0f
                    bottomBarHeightPx.value = 0.0f
                }
                return@l
            }

            val showScrolledBackground = bottomBarPreferences?.backgroundVisibility == Visibility.visible || bottomBarPreferences?.scrollableState?.canScrollForward == true
            val materialColorScheme: androidx.compose.material3.ColorScheme
            if (showScrolledBackground) {
                val matchtarget_4 = bottomBarPreferences?.colorScheme?.asMaterialTheme()
                if (matchtarget_4 != null) {
                    val customColorScheme = matchtarget_4
                    materialColorScheme = customColorScheme.sref()
                } else {
                    materialColorScheme = MaterialTheme.colorScheme.sref()
                }
            } else {
                materialColorScheme = MaterialTheme.colorScheme.sref()
            }
            MaterialTheme(colorScheme = materialColorScheme) { ->
                val bottomBarBackgroundColor: androidx.compose.ui.graphics.Color
                val unscrolledBottomBarBackgroundColor: androidx.compose.ui.graphics.Color
                val bottomBarBackgroundForBrush: ShapeStyle?
                val bottomBarHasColorScheme = bottomBarPreferences?.colorScheme != null
                if (bottomBarPreferences?.backgroundVisibility == Visibility.hidden) {
                    bottomBarBackgroundColor = androidx.compose.ui.graphics.Color.Transparent
                    unscrolledBottomBarBackgroundColor = androidx.compose.ui.graphics.Color.Transparent
                    bottomBarBackgroundForBrush = null
                } else {
                    val matchtarget_5 = bottomBarPreferences?.background
                    if (matchtarget_5 != null) {
                        val background = matchtarget_5
                        val matchtarget_6 = background.asColor(opacity = 1.0, animationContext = null)
                        if (matchtarget_6 != null) {
                            val color = matchtarget_6
                            bottomBarBackgroundColor = color
                            unscrolledBottomBarBackgroundColor = if (isSystemBackground) Color.systemBarBackground.colorImpl() else color.copy(alpha = 0.0f)
                            bottomBarBackgroundForBrush = null
                        } else {
                            unscrolledBottomBarBackgroundColor = if (isSystemBackground) Color.systemBarBackground.colorImpl() else androidx.compose.ui.graphics.Color.Transparent
                            bottomBarBackgroundColor = unscrolledBottomBarBackgroundColor.copy(alpha = 0.0f)
                            bottomBarBackgroundForBrush = background.sref()
                        }
                    } else {
                        bottomBarBackgroundColor = Color.systemBarBackground.colorImpl()
                        unscrolledBottomBarBackgroundColor = if (isSystemBackground) bottomBarBackgroundColor else bottomBarBackgroundColor.copy(alpha = 0.0f)
                        bottomBarBackgroundForBrush = null
                    }
                }

                val tint = (EnvironmentValues.shared._tint ?: Color(colorImpl = { -> MaterialTheme.colorScheme.onSurface })).sref()
                val placement = EnvironmentValues.shared._placement.sref()
                EnvironmentValues.shared.setValues(l@{ it ->
                    it.set_tint(tint)
                    it.set_placement(placement.union(ViewPlacement.toolbar))
                    return@l ComposeResult.ok
                }, in_ = { ->
                    var bottomBarModifier = Modifier.zIndex(1.1f)
                        .onGloballyPositionedInWindow { bounds ->
                            bottomBarTopPx.value = bounds.top
                            bottomBarHeightPx.value = bounds.bottom - bounds.top
                        }
                    if (showScrolledBackground && (bottomBarBackgroundForBrush != null)) {
                        bottomBarBackgroundForBrush.asBrush(opacity = 1.0, animationContext = null)?.let { bottomBarBackgroundBrush ->
                            bottomBarModifier = bottomBarModifier.background(bottomBarBackgroundBrush)
                        }
                    }
                    // Pull the bottom bar below the keyboard
                    val bottomPadding = with(density) { -> min(bottomBarHeightPx.value, Float(WindowInsets.ime.getBottom(density))).toDp() }
                    PaddingLayout(padding = EdgeInsets(top = 0.0, leading = 0.0, bottom = Double(-bottomPadding.value), trailing = 0.0), context = context.content()) { context ->
                        val containerColor = if (showScrolledBackground) bottomBarBackgroundColor else unscrolledBottomBarBackgroundColor
                        val windowInsets = (if (EnvironmentValues.shared._isEdgeToEdge == true) BottomAppBarDefaults.windowInsets else WindowInsets(bottom = 0.dp)).sref()
                        var options = Material3BottomAppBarOptions(modifier = context.modifier.then(bottomBarModifier), containerColor = containerColor, contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor), contentPadding = PaddingValues.Absolute(left = 16.dp, right = 16.dp))
                        EnvironmentValues.shared._material3BottomAppBar?.let { updateOptions ->
                            options = updateOptions(options)
                        }
                        BottomAppBar(modifier = options.modifier, containerColor = options.containerColor, contentColor = options.contentColor, tonalElevation = options.tonalElevation, contentPadding = options.contentPadding, windowInsets = windowInsets) { ->
                            // Use an HStack so that it sets up the environment for bottom toolbar Spacers
                            HStack(spacing = 24.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    ComposeView { context ->
                                        for (renderable in bottomItems.sref()) {
                                            renderable.Render(context = context)
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(context)
                        }
                    }
                })
            }
        }

        // We place nav bars within each entry rather than at the navigation controller level. There isn't a fluid animation
        // between navigation bar states on Android, and it is simpler to only hoist navigation bar preferences to this level
        Column(modifier = modifier.background(Color.background.colorImpl())) { ->
            // Calculate safe area for content
            val contentSafeArea = arguments.safeArea?.insetting(Edge.top, to = topBarBottomPx.value)?.insetting(Edge.bottom, to = bottomBarTopPx.value)
            // Inset manually for any edge where our container ignored the safe area, but we aren't showing a bar
            val topPadding = (if (topBarBottomPx.value <= 0.0f && arguments.ignoresSafeAreaEdges.contains(Edge.Set.top)) WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() else 0.dp).sref()
            var bottomPadding = 0.dp.sref()
            if (bottomBarTopPx.value <= 0.0f && arguments.ignoresSafeAreaEdges.contains(Edge.Set.bottom)) {
                bottomPadding = max(0.dp, WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding() - WindowInsets.ime.asPaddingValues().calculateBottomPadding())
            }
            val contentModifier = Modifier.fillMaxWidth().weight(1.0f).padding(top = topPadding, bottom = bottomPadding)

            topBar()
            Box(modifier = contentModifier, contentAlignment = androidx.compose.ui.Alignment.Center) { ->
                var topPadding = 0.dp.sref()
                val searchableState: SearchableState? = if (arguments.isRoot) (EnvironmentValues.shared._searchableState ?: searchableStatePreference.value.reduced) else null
                if (searchableState != null) {
                    val searchFieldBackground = if (isSystemBackground) Color.systemBarBackground.colorImpl() else androidx.compose.ui.graphics.Color.Transparent
                    val searchFieldFadeOffset = searchFieldHeightPx / 3
                    val searchFieldModifier = Modifier.height(searchFieldHeight.dp + searchFieldPadding)
                        .align(androidx.compose.ui.Alignment.TopCenter)
                        .offset({ -> IntOffset(0, Int(searchFieldOffsetPx.value)) })
                        .background(searchFieldBackground)
                        .padding(start = searchFieldPadding, bottom = searchFieldPadding, end = searchFieldPadding)
                        .graphicsLayer { -> alpha = max(0.0f, (searchFieldFadeOffset + searchFieldOffsetPx.value) / searchFieldFadeOffset) }
                        .fillMaxWidth()
                    SearchField(state = searchableState, context = context.content(modifier = searchFieldModifier))
                    val searchFieldPlaceholderPadding = (searchFieldHeight.dp + searchFieldPadding + (with(LocalDensity.current) { -> searchFieldOffsetPx.value.toDp() })).sref()
                    topPadding = searchFieldPlaceholderPadding.sref()
                }
                EnvironmentValues.shared.setValues(l@{ it ->
                    if (contentSafeArea != null) {
                        it.set_safeArea(contentSafeArea)
                    }
                    it.set_searchableState(searchableState)
                    it.set_isNavigationRoot(arguments.isRoot)
                    return@l ComposeResult.ok
                }, in_ = { ->
                    // Elevate the top padding modifier so that content always has the same context, allowing it to avoid recomposition
                    Box(modifier = Modifier.padding(top = topPadding)) { ->
                        PreferenceValues.shared.collectPreferences(arrayOf(searchableStateCollector, scrollToTopCollector)) { -> content(context.content()) }
                    }
                })
            }
            bottomBar()
        }
    }

    @Composable
    private fun RenderDestination(destination: ((Any) -> View)?, arguments: NavigationDestinationArguments, context: ComposeContext) {
        // Break out this function to give it stable arguments and avoid recomosition on push/pop
        destination?.invoke(arguments.targetValue)?.Compose(context = context)
    }

    @Composable
    private fun useInlineTitleDisplayMode(for_: ToolbarTitleDisplayMode, safeArea: SafeArea?): Boolean {
        val titleDisplayMode = for_
        if (titleDisplayMode != ToolbarTitleDisplayMode.automatic) {
            return titleDisplayMode == ToolbarTitleDisplayMode.inline_
        }
        // Default to inline if in landscape or a sheet
        if ((safeArea != null) && (safeArea.presentationBoundsPx.width > safeArea.presentationBoundsPx.height)) {
            return true
        }
        return EnvironmentValues.shared._sheetDepth > 0
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

@Stable
internal class NavigationEntryArguments {
    internal val isRoot: Boolean
    internal val state: Navigator.BackStackState
    internal val safeArea: SafeArea?
    internal val ignoresSafeAreaEdges: Edge.Set
    internal val title: Text
    internal val toolbarPreferences: ToolbarPreferences

    constructor(isRoot: Boolean, state: Navigator.BackStackState, safeArea: SafeArea? = null, ignoresSafeAreaEdges: Edge.Set, title: Text, toolbarPreferences: ToolbarPreferences) {
        this.isRoot = isRoot
        this.state = state
        this.safeArea = safeArea
        this.ignoresSafeAreaEdges = ignoresSafeAreaEdges.sref()
        this.title = title
        this.toolbarPreferences = toolbarPreferences
    }

    override fun equals(other: Any?): Boolean {
        if (other !is NavigationEntryArguments) return false
        return isRoot == other.isRoot && state == other.state && safeArea == other.safeArea && ignoresSafeAreaEdges == other.ignoresSafeAreaEdges && title == other.title && toolbarPreferences == other.toolbarPreferences
    }
}

@Stable
internal class NavigationDestinationArguments {
    internal val targetValue: Any

    constructor(targetValue: Any) {
        this.targetValue = targetValue.sref()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is NavigationDestinationArguments) return false
        return targetValue == other.targetValue
    }
}

internal typealias NavigationDestinations = Dictionary<AnyHashable, NavigationDestination>
internal class NavigationDestination {
    internal val destination: (Any) -> View
    // No way to compare closures. Assume equal so we don't think our destinations are constantly updating
    override fun equals(other: Any?): Boolean = true

    constructor(destination: (Any) -> View) {
        this.destination = destination
    }
}

@Stable
@Suppress("MUST_BE_INITIALIZED")
internal class Navigator {

    private var navController: NavHostController
        get() = field.sref({ this.navController = it })
        set(newValue) {
            field = newValue.sref()
        }
    private var keyboardController: SoftwareKeyboardController? = null
        get() = field.sref({ this.keyboardController = it })
        set(newValue) {
            field = newValue.sref()
        }
    private var destinations: Dictionary<AnyHashable, NavigationDestination>
        get() = field.sref({ this.destinations = it })
        set(newValue) {
            field = newValue.sref()
        }
    private var destinationIndexes: Dictionary<AnyHashable, Int> = dictionaryOf()
        get() = field.sref({ this.destinationIndexes = it })
        set(newValue) {
            field = newValue.sref()
        }
    private var destinationKeyTransformer: ((Any) -> String)? = null

    // We reserve the last destination index for static destinations. Every time we navigate to a static destination view, we increment the
    // destination value to give it a unique navigation path of e.g. 99/0, 99/1, 99/2, etc
    private val viewDestinationIndex = Companion.destinationCount - 1
    private var viewDestinationValue = 0

    private var boundPath: Binding<Array<*>?>? = null
        get() = field.sref({ this.boundPath = it })
        set(newValue) {
            field = newValue.sref()
        }
    private var boundNavigationPath: Binding<NavigationPath>? = null
        get() = field.sref({ this.boundNavigationPath = it })
        set(newValue) {
            field = newValue.sref()
        }

    private var backStackState: Dictionary<String, Navigator.BackStackState> = dictionaryOf()
        get() = field.sref({ this.backStackState = it })
        set(newValue) {
            field = newValue.sref()
        }
    internal class BackStackState {
        internal val id: String
        internal val route: String
        internal val destination: ((Any) -> View)?
        internal val targetValue: Any?
        internal val stateSaver: ComposeStateSaver
        internal var titleDisplayMode: ToolbarTitleDisplayMode? = null
        internal var binding: Binding<Boolean>? = null
            get() = field.sref({ this.binding = it })
            set(newValue) {
                field = newValue.sref()
            }

        internal constructor(id: String, route: String, destination: ((Any) -> View)? = null, targetValue: Any? = null, stateSaver: ComposeStateSaver = ComposeStateSaver()) {
            this.id = id
            this.route = route
            this.destination = destination
            this.targetValue = targetValue.sref()
            this.stateSaver = stateSaver
        }
    }

    internal constructor(navController: NavHostController, destinations: Dictionary<AnyHashable, NavigationDestination>, destinationKeyTransformer: ((Any) -> String)?) {
        this.navController = navController
        this.destinations = destinations
        this.destinationKeyTransformer = destinationKeyTransformer
        updateDestinationIndexes()
    }

    /// Call with updated state on recompose.
    @Composable
    internal fun didCompose(navController: NavHostController, destinations: Dictionary<AnyHashable, NavigationDestination>, path: Binding<Array<*>?>?, navigationPath: Binding<NavigationPath>?, keyboardController: SoftwareKeyboardController?) {
        this.navController = navController
        this.destinations = destinations
        this.boundPath = path
        this.boundNavigationPath = navigationPath
        this.keyboardController = keyboardController
        updateDestinationIndexes()
        syncState()
        navigateToPath()
    }

    /// Whether we're at the root of the navigation stack.
    internal val isRoot: Boolean
        get() = navController.currentBackStack.value.size <= 2 // graph entry, root entry

    /// Navigate to a target value specified in a `NavigationLink`.
    internal fun navigate(to: Any, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) {
        val targetValue = to
        val matchtarget_7 = boundPath
        if (matchtarget_7 != null) {
            val path = matchtarget_7
            (path.wrappedValue as? Array<Any?>)?.append(targetValue)
        } else {
            val matchtarget_8 = boundNavigationPath
            if (matchtarget_8 != null) {
                val navigationPath = matchtarget_8
                (navigationPath.wrappedValue as? NavigationPath)?.append(targetValue)
            } else {
                navigate(toKeyed = targetValue)
            }
        }
    }

    private fun navigate(toKeyed: Any) {
        val targetValue = toKeyed
        val key: AnyHashable
        val matchtarget_9 = destinationKeyTransformer
        if (matchtarget_9 != null) {
            val destinationKeyTransformer = matchtarget_9
            key = destinationKeyTransformer(targetValue)
        } else {
            key = type(of = targetValue)
        }
        navigate(toKeyed = targetValue, key = key)
    }

    /// Navigate to a destination view.
    ///
    /// - Parameter binding: Optional binding to toggle to `false` when the view is popped.
    /// - Returns: The navigation stack entry ID of the pushed view.
    internal fun navigateToView(view: View, binding: Binding<Boolean>? = null): String? {
        val targetValue = viewDestinationValue
        viewDestinationValue += 1

        val route = Companion.route(for_ = viewDestinationIndex, valueString = String(describing = targetValue))
        return navigate(route = route, destination = { _ -> view }, targetValue = targetValue, binding = binding)
    }

    /// Pop the back stack.
    internal fun navigateBack() {
        // Check for a view destination before we pop our path bindings, because the user could push arbitrary views
        // that are not represented in the bound path
        val viewDestinationPrefix = Companion.route(for_ = viewDestinationIndex, valueString = "")
        if (navController.currentBackStackEntry?.destination?.route?.hasPrefix(viewDestinationPrefix) == true) {
            navController.popBackStack()
        } else {
            val matchtarget_10 = boundPath
            if (matchtarget_10 != null) {
                val path = matchtarget_10
                path.wrappedValue?.popLast()
            } else {
                val matchtarget_11 = boundNavigationPath
                if (matchtarget_11 != null) {
                    val navigationPath = matchtarget_11
                    (navigationPath.wrappedValue as? NavigationPath)?.removeLast()
                } else if (!isRoot) {
                    navController.popBackStack()
                }
            }
        }
    }

    /// Whether the given view entry ID is presented.
    internal fun isViewPresented(id: String, asTop: Boolean = false): Boolean {
        val stack = navController.currentBackStack.value.sref()
        if (stack.isEmpty()) {
            return false
        }
        if (asTop) {
            return stack.last().id == id
        }
        return stack.any { it -> it.id == id }
    }

    /// The entry being navigated to.
    internal fun state(for_: NavBackStackEntry): Navigator.BackStackState? {
        val entry = for_
        backStackState[entry.id]?.let { state ->
            return state
        }
        if (navController.currentBackStack.value.count() <= 1 || entry.id != navController.currentBackStack.value[1].id) {
            return null
        }
        val rootState = BackStackState(id = entry.id, route = Companion.rootRoute)
        backStackState[entry.id] = rootState
        return rootState
    }

    /// The effective title display mode for the given preference value.
    internal fun titleDisplayMode(for_: Navigator.BackStackState, hasTitle: Boolean, preference: ToolbarTitleDisplayMode?): ToolbarTitleDisplayMode {
        val state = for_
        if (preference != null) {
            state.titleDisplayMode = preference
            return preference
        }
        if (!hasTitle) {
            return ToolbarTitleDisplayMode.inline_
        }

        // Base the display mode on the back stack
        var titleDisplayMode: ToolbarTitleDisplayMode? = null
        for (entry in navController.currentBackStack.value.sref()) {
            if (entry.id == state.id) {
                break
            } else {
                backStackState[entry.id]?.titleDisplayMode?.let { entryTitleDisplayMode ->
                    titleDisplayMode = entryTitleDisplayMode
                }
            }
        }
        return titleDisplayMode ?: ToolbarTitleDisplayMode.automatic
    }

    /// Sync our back stack state with the nav controller.
    @Composable
    private fun syncState() {
        // Collect as state to ensure we get re-called on change
        val entryList = navController.currentBackStack.collectAsState()

        // Toggle any presented bindings for popped states back to false. Do this immediately so that we don't
        // re-present views that were removed from the stack
        val entryIDs = Set(entryList.value.map { it -> it.id })
        for ((id, state) in backStackState.sref()) {
            if (!entryIDs.contains(id)) {
                state.binding?.wrappedValue = false
            }
        }

        // Sync the back stack with remaining states. We delay this to allow views that receive compose calls while
        // animating away to find their state
        LaunchedEffect(entryList.value) { ->
            delay(1000) // 1 second
            var syncedBackStackState: Dictionary<String, Navigator.BackStackState> = dictionaryOf()
            for (entry in entryList.value.sref()) {
                backStackState[entry.id]?.let { state ->
                    syncedBackStackState[entry.id] = state
                }
            }
            backStackState = syncedBackStackState
        }
    }

    private fun navigateToPath() {
        val path_0 = (this.boundPath?.wrappedValue ?: boundNavigationPath?.wrappedValue?.path).sref()
        if (path_0 == null) {
            return
        }
        val backStack = navController.currentBackStack.value.sref()
        if (backStack.isEmpty()) {
            return
        }

        // Figure out where the path and back stack first differ
        var pathIndex = 0
        var backStackIndex = 2 // graph, root
        while (pathIndex < path_0.count) {
            if (backStackIndex >= backStack.count()) {
                break
            }
            val state = backStackState[backStack[backStackIndex].id]
            if (state?.targetValue != (path_0[pathIndex] as Any?)) {
                break
            }
            pathIndex += 1
            backStackIndex += 1
        }

        // If we exhausted the path and the back stack contains only post-path views, keep them in place. This allows
        // users to have a path binding but then append arbitrary views as leaves
        var hasOnlyTrailingViews = false
        if (pathIndex == path_0.count) {
            hasOnlyTrailingViews = true
            val viewDestinationPrefix = Companion.route(for_ = viewDestinationIndex, valueString = "")
            for (i in 0 until (backStack.count() - backStackIndex)) {
                if (backStack[backStackIndex + i].destination.route?.hasPrefix(viewDestinationPrefix) != true) {
                    hasOnlyTrailingViews = false
                    break
                }
            }
        }
        if (hasOnlyTrailingViews) {
            return
        }

        // Pop back to last common value
        for (unusedbinding in 0 until (backStack.count() - backStackIndex)) {
            navController.popBackStack()
        }
        // Navigate to any new path values
        for (i in pathIndex until path_0.count) {
            navigate(toKeyed = path_0[i] as Any)
        }
    }

    private fun navigate(toKeyed: Any, key: AnyHashable?): Boolean {
        val targetValue = toKeyed
        if (key == null) {
            return false
        }
        val destination_0 = destinations[key]
        if (destination_0 == null) {
            (key as? KClass<*>)?.let { type ->
                for (supertype in type.superclasses.sref()) {
                    if (navigate(toKeyed = targetValue, key = supertype)) {
                        return true
                    }
                }
            }
            return false
        }

        val route = route(for_ = key, value = targetValue)
        navigate(route = route, destination = destination_0.destination, targetValue = targetValue)
        return true
    }

    private fun navigate(route: String, destination: ((Any) -> View)?, targetValue: Any, binding: Binding<Boolean>? = null): String? {
        // We see a top app bar glitch when the keyboard animates away after push, so manually dismiss it first
        keyboardController?.hide()
        navController.navigate(route)
        val entry_0 = navController.currentBackStackEntry.sref()
        if (entry_0 == null) {
            return null
        }
        var state = backStackState[entry_0.id]
        if (state == null) {
            state = BackStackState(id = entry_0.id, route = route, destination = destination, targetValue = targetValue)
            backStackState[entry_0.id] = state
        }
        if (binding != null) {
            state?.binding = binding
        }
        return entry_0.id
    }

    private fun route(for_: AnyHashable, value: Any): String {
        val key = for_
        val index_0 = destinationIndexes[key]
        if (index_0 == null) {
            return String(describing = key) + "?"
        }
        // Escape '/' because it is meaningful in navigation routes
        val valueString = composeBundleString(for_ = value).replacingOccurrences(of = "/", with = "%2F")
        return route(for_ = index_0, valueString = valueString)
    }

    private fun updateDestinationIndexes() {
        for (key in destinations.keys.sref()) {
            if (destinationIndexes[key] == null) {
                destinationIndexes[key] = destinationIndexes.count
            }
        }
    }

    @androidx.annotation.Keep
    companion object {
        /// Route for the root of the navigation stack.
        internal val rootRoute = "navigationroot"

        /// Number of possible destiation routes.
        ///
        /// We route to destinations by static index rather than a dynamic system based on the provided destination
        /// keys because changing the destinations of a `NavHost` wipes out its back stack. By using a fixed set of
        /// indexes, we can maintain the back stack even as we add destination mappings.
        internal val destinationCount = 100

        /// Route for the given destination index and value string.
        internal fun route(for_: Int, valueString: String): String {
            val destinationIndex = for_
            return String(describing = destinationIndex) + "/" + valueString
        }
    }
}

internal class NavigationDestinationItemWrapper<D>: View {
    internal lateinit var dismiss: DismissAction
    internal val item: Binding<D?>
    internal val isBeingDismissedByNavigator: MutableState<Boolean>
    internal val navigationId: MutableState<String?>
    internal val destination: (D) -> View

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            Group { ->
                ComposeBuilder { composectx: ComposeContext ->
                    linvokeComposable l@{
                        val matchtarget_12 = item.wrappedValue
                        if (matchtarget_12 != null) {
                            val itemValue = matchtarget_12
                            return@l destination(itemValue).Compose(composectx)
                        } else {
                            return@l EmptyView().Compose(composectx)
                        }
                    }
                    ComposeResult.ok
                }
            }
            .onChange(of = item.wrappedValue, initial = true) { oldValue, newValue ->
                if (newValue == null) {
                    if (!isBeingDismissedByNavigator.value) {
                        dismiss()
                    }
                    navigationId.value = null
                }
                isBeingDismissedByNavigator.value = false
            }.Compose(composectx)
        }
    }

    @Composable
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        this.dismiss = EnvironmentValues.shared.dismiss

        return super.Evaluate(context, options)
    }

    constructor(item: Binding<D?>, isBeingDismissedByNavigator: MutableState<Boolean>, navigationId: MutableState<String?>, destination: (D) -> View) {
        this.item = item.sref()
        this.isBeingDismissedByNavigator = isBeingDismissedByNavigator.sref()
        this.navigationId = navigationId.sref()
        this.destination = destination
    }
}

internal val LocalNavigator: ProvidableCompositionLocal<Navigator?> = compositionLocalOf { -> null as Navigator? }

class NavigationSplitViewStyle: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is NavigationSplitViewStyle) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        var automatic = NavigationSplitViewStyle(rawValue = 0)
            get() = field.sref({ this.automatic = it })
            set(newValue) {
                field = newValue.sref()
            }
        var balanced = NavigationSplitViewStyle(rawValue = 1)
            get() = field.sref({ this.balanced = it })
            set(newValue) {
                field = newValue.sref()
            }
        var prominentDetail = NavigationSplitViewStyle(rawValue = 2)
            get() = field.sref({ this.prominentDetail = it })
            set(newValue) {
                field = newValue.sref()
            }
    }
}

class NavigationBarItem {
    enum class TitleDisplayMode(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
        automatic(0), // For bridging
        inline_(1), // For bridging
        large(2); // For bridging

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: Int): NavigationBarItem.TitleDisplayMode? {
                return when (rawValue) {
                    0 -> TitleDisplayMode.automatic
                    1 -> TitleDisplayMode.inline_
                    2 -> TitleDisplayMode.large
                    else -> null
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean = other is NavigationBarItem

    override fun hashCode(): Int = "NavigationBarItem".hashCode()

    @androidx.annotation.Keep
    companion object {

        fun TitleDisplayMode(rawValue: Int): NavigationBarItem.TitleDisplayMode? = TitleDisplayMode.init(rawValue = rawValue)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("MUST_BE_INITIALIZED")
class Material3TopAppBarOptions: MutableStruct {
    var title: @Composable () -> Unit
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
    var navigationIcon: @Composable () -> Unit
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var colors: TopAppBarColors
        get() = field.sref({ this.colors = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var scrollBehavior: TopAppBarScrollBehavior? = null
        get() = field.sref({ this.scrollBehavior = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var preferCenterAlignedStyle: Boolean
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var preferLargeStyle: Boolean
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    fun copy(title: @Composable () -> Unit = this.title, modifier: Modifier = this.modifier, navigationIcon: @Composable () -> Unit = this.navigationIcon, colors: TopAppBarColors = this.colors, scrollBehavior: TopAppBarScrollBehavior? = this.scrollBehavior, preferCenterAlignedStyle: Boolean = this.preferCenterAlignedStyle, preferLargeStyle: Boolean = this.preferLargeStyle): Material3TopAppBarOptions = Material3TopAppBarOptions(title = title, modifier = modifier, navigationIcon = navigationIcon, colors = colors, scrollBehavior = scrollBehavior, preferCenterAlignedStyle = preferCenterAlignedStyle, preferLargeStyle = preferLargeStyle)

    constructor(title: @Composable () -> Unit, modifier: Modifier = Modifier, navigationIcon: @Composable () -> Unit = { ->  }, colors: TopAppBarColors, scrollBehavior: TopAppBarScrollBehavior? = null, preferCenterAlignedStyle: Boolean = false, preferLargeStyle: Boolean = false) {
        this.title = title
        this.modifier = modifier
        this.navigationIcon = navigationIcon
        this.colors = colors
        this.scrollBehavior = scrollBehavior
        this.preferCenterAlignedStyle = preferCenterAlignedStyle
        this.preferLargeStyle = preferLargeStyle
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = Material3TopAppBarOptions(title, modifier, navigationIcon, colors, scrollBehavior, preferCenterAlignedStyle, preferLargeStyle)

    @androidx.annotation.Keep
    companion object {
    }
}

@Suppress("MUST_BE_INITIALIZED")
class Material3BottomAppBarOptions: MutableStruct {
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
    var contentPadding: PaddingValues
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    fun copy(modifier: Modifier = this.modifier, containerColor: androidx.compose.ui.graphics.Color = this.containerColor, contentColor: androidx.compose.ui.graphics.Color = this.contentColor, tonalElevation: Dp = this.tonalElevation, contentPadding: PaddingValues = this.contentPadding): Material3BottomAppBarOptions = Material3BottomAppBarOptions(modifier = modifier, containerColor = containerColor, contentColor = contentColor, tonalElevation = tonalElevation, contentPadding = contentPadding)

    constructor(modifier: Modifier = Modifier, containerColor: androidx.compose.ui.graphics.Color, contentColor: androidx.compose.ui.graphics.Color, tonalElevation: Dp = BottomAppBarDefaults.ContainerElevation.sref(), contentPadding: PaddingValues = BottomAppBarDefaults.ContentPadding) {
        this.modifier = modifier
        this.containerColor = containerColor
        this.contentColor = contentColor
        this.tonalElevation = tonalElevation
        this.contentPadding = contentPadding
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = Material3BottomAppBarOptions(modifier, containerColor, contentColor, tonalElevation, contentPadding)

    @androidx.annotation.Keep
    companion object {
    }
}

@androidx.annotation.Keep
internal class NavigationDestinationsPreferenceKey: PreferenceKey<Dictionary<AnyHashable, NavigationDestination>> {

    @androidx.annotation.Keep
    companion object: PreferenceKeyCompanion<Dictionary<AnyHashable, NavigationDestination>> {
        override val defaultValue: Dictionary<AnyHashable, NavigationDestination> = dictionaryOf()

        override fun reduce(value: InOut<Dictionary<AnyHashable, NavigationDestination>>, nextValue: () -> Dictionary<AnyHashable, NavigationDestination>) {
            for ((type, destination) in nextValue()) {
                value.value[type] = destination
            }
        }
    }
}

@androidx.annotation.Keep
internal class NavigationTitlePreferenceKey: PreferenceKey<Text> {

    @androidx.annotation.Keep
    companion object: PreferenceKeyCompanion<Text> {
        override val defaultValue = Text(LocalizedStringKey(stringLiteral = ""))

        override fun reduce(value: InOut<Text>, nextValue: () -> Text) {
            value.value = nextValue()
        }
    }
}

@androidx.annotation.Keep
class NavigationLink: View, Renderable, skip.lib.SwiftProjecting {
    internal val value: Any?
    internal val destination: ComposeBuilder?
    internal val label: ComposeBuilder

    constructor(value: Any?, label: () -> View) {
        this.value = value.sref()
        this.destination = null
        this.label = ComposeBuilder.from(label)
    }

    constructor(title: String, value: Any?): this(value = value, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(verbatim = title).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleKey: LocalizedStringKey, value: Any?): this(value = value, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleKey).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleResource: LocalizedStringResource, value: Any?): this(value = value, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleResource).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(destination: () -> View, label: () -> View) {
        this.value = null
        this.destination = ComposeBuilder.from(destination)
        this.label = ComposeBuilder.from(label)
    }

    constructor(destination: View, label: () -> View): this(destination = { ->
        ComposeBuilder { composectx: ComposeContext ->
            destination.Compose(composectx)
            ComposeResult.ok
        }
    }, label = label) {
    }

    constructor(titleKey: LocalizedStringKey, destination: () -> View): this(destination = destination, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleKey).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleResource: LocalizedStringResource, destination: () -> View): this(destination = destination, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleResource).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(bridgedDestination: View?, value: Any?, bridgedLabel: View) {
        this.destination = if (bridgedDestination == null) null else ComposeBuilder.from { -> bridgedDestination!! }
        this.value = value.sref()
        this.label = ComposeBuilder.from { -> bridgedLabel }
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val isEnabled = (value != null || destination != null) && EnvironmentValues.shared.isEnabled
        Button.RenderButton(label = label, context = context, isEnabled = isEnabled, action = navigationAction())
    }

    @Composable
    override fun shouldRenderListItem(context: ComposeContext): Tuple2<Boolean, (() -> Unit)?> {
        val buttonStyle = EnvironmentValues.shared._buttonStyle
        if (buttonStyle != null && buttonStyle != ButtonStyle.automatic && buttonStyle != ButtonStyle.plain) {
            return Tuple2(false, null)
        }
        val action: (() -> Unit)? = if (value != null || destination != null) navigationAction() else null
        return Tuple2(true, action)
    }

    @Composable
    override fun RenderListItem(context: ComposeContext, modifiers: kotlin.collections.List<ModifierProtocol>) {
        ModifiedContent.RenderWithModifiers(modifiers, context = context) { context ->
            val renderables = label.Evaluate(context = context, options = 0)
            Row(modifier = context.modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { ->
                Box(modifier = Modifier.weight(1.0f)) { ->
                    val labelContext = context.content()
                    // Continue to specialize for list rendering within the content (e.g. Label)
                    if ((renderables.size == 1) && renderables[0].shouldRenderListItem(context = context).element0) {
                        renderables[0].RenderListItem(context = labelContext, modifiers = listOf())
                    } else {
                        for (renderable in renderables.sref()) {
                            renderable.Render(context = labelContext)
                        }
                    }
                }
                Companion.RenderChevron()
            }
        }
    }

    @Composable
    internal fun navigationAction(): () -> Unit {
        val navigator = LocalNavigator.current.sref()
        return l@{ ->
            // Hack to prevent multiple quick taps from pushing duplicate entries
            val now = CFAbsoluteTimeGetCurrent()
            if (NavigationLink.lastNavigationTime + NavigationLink.minimumNavigationInterval > now) {
                return@l
            }
            NavigationLink.lastNavigationTime = now

            if (value != null) {
                navigator?.navigate(to = value)
            } else if (destination != null) {
                navigator?.navigateToView(destination)
            }
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {

        private val minimumNavigationInterval = 0.35
        private var lastNavigationTime = 0.0

        @Composable
        internal fun RenderChevron() {
            val isRTL = EnvironmentValues.shared.layoutDirection == LayoutDirection.rightToLeft
            Icon(imageVector = if (isRTL) Icons.Outlined.KeyboardArrowLeft else Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

class NavigationPath: MutableStruct {
    internal var path: Array<Any?> = Array()
        get() = field.sref({ v: Array<Any?> -> this.path = v })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    constructor() {
    }



    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(codable: NavigationPath.CodableRepresentation) {
    }

    val count: Int
        get() = path.count

    val isEmpty: Boolean
        get() = path.isEmpty

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val codable: NavigationPath.CodableRepresentation?
        get() {
            fatalError()
        }

    fun append(value: Any) {
        willmutate()
        try {
            (path as Array<Any?>).append(value)
        } finally {
            didmutate()
        }
    }

    fun removeLast(k: Int = 1) {
        willmutate()
        try {
            path.removeLast(k)
        } finally {
            didmutate()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is NavigationPath) {
            return false
        }
        val lhs = this
        val rhs = other
        return lhs.path == rhs.path
    }

    @androidx.annotation.Keep
    class CodableRepresentation: Codable {
        constructor(from: Decoder) {
        }

        override fun encode(to: Encoder) = Unit

        @androidx.annotation.Keep
        companion object: DecodableCompanion<NavigationPath.CodableRepresentation> {
            override fun init(from: Decoder): NavigationPath.CodableRepresentation = CodableRepresentation(from = from)
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as NavigationPath
        this.path = copy.path
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = NavigationPath(this as MutableStruct)

    @androidx.annotation.Keep
    companion object {
    }
}

/*
import struct CoreGraphics.CGFloat
import struct Foundation.URL

@available(iOS 13.0, *)
@available(macOS, unavailable)
@available(tvOS, unavailable)
@available(watchOS, unavailable)
extension NavigationLink {

/// Sets the navigation link to present its destination as the detail
/// component of the containing navigation view.
///
/// This method sets the behavior when the navigation link is used in a
/// ``NavigationSplitView``, or a
/// multi-column navigation view, such as one using
/// ``ColumnNavigationViewStyle``.
///
/// For example, in a two-column navigation split view, if `isDetailLink` is
/// `true`, triggering the link in the sidebar column sets the contents of
/// the detail column to be the link's destination view. If `isDetailLink`
/// is `false`, the link navigates to the destination view within the
/// primary column.
///
/// If you do not set the detail link behavior with this method, the
/// behavior defaults to `true`.
///
/// The `isDetailLink` modifier only affects view-destination links. Links
/// that present data values always search for a matching navigation
/// destination beginning in the column that contains the link.
///
/// - Parameter isDetailLink: A Boolean value that specifies whether this
/// link presents its destination as the detail component when used in a
/// multi-column navigation view.
/// - Returns: A view that applies the specified detail link behavior.
@available(macOS, unavailable)
@available(tvOS, unavailable)
@available(watchOS, unavailable)
public func isDetailLink(_ isDetailLink: Bool) -> some View { return stubView() }

}

/// A view that presents views in two or three columns, where selections in
/// leading columns control presentations in subsequent columns.
///
/// You create a navigation split view with two or three columns, and typically
/// use it as the root view in a ``Scene``. People choose one or more
/// items in a leading column to display details about those items in
/// subsequent columns.
///
/// To create a two-column navigation split view, use the
/// ``init(sidebar:detail:)`` initializer:
///
///     @State private var employeeIds: Set<Employee.ID> = []
///
///     var body: some View {
///         NavigationSplitView {
///             List(model.employees, selection: $employeeIds) { employee in
///                 Text(employee.name)
///             }
///         } detail: {
///             EmployeeDetails(for: employeeIds)
///         }
///     }
///
/// In the above example, the navigation split view coordinates with the
/// ``List`` in its first column, so that when people make a selection, the
/// detail view updates accordingly. Programmatic changes that you make to the
/// selection property also affect both the list appearance and the presented
/// detail view.
///
/// To create a three-column view, use the ``init(sidebar:content:detail:)``
/// initializer. The selection in the first column affects the second, and the
/// selection in the second column affects the third. For example, you can show
/// a list of departments, the list of employees in the selected department,
/// and the details about all of the selected employees:
///
///     @State private var departmentId: Department.ID? // Single selection.
///     @State private var employeeIds: Set<Employee.ID> = [] // Multiple selection.
///
///     var body: some View {
///         NavigationSplitView {
///             List(model.departments, selection: $departmentId) { department in
///                 Text(department.name)
///             }
///         } content: {
///             if let department = model.department(id: departmentId) {
///                 List(department.employees, selection: $employeeIds) { employee in
///                     Text(employee.name)
///                 }
///             } else {
///                 Text("Select a department")
///             }
///         } detail: {
///             EmployeeDetails(for: employeeIds)
///         }
///     }
///
/// You can also embed a ``NavigationStack`` in a column. Tapping or clicking a
/// ``NavigationLink`` that appears in an earlier column sets the view that the
/// stack displays over its root view. Activating a link in the same column
/// adds a view to the stack. Either way, the link must present a data type
/// for which the stack has a corresponding
/// ``View/navigationDestination(for:destination:)`` modifier.
///
/// On watchOS and tvOS, and with narrow sizes like on iPhone or on iPad in
/// Slide Over, the navigation split view collapses all of its columns
/// into a stack, and shows the last column that displays useful information.
/// For example, the three-column example above shows the list of departments to
/// start, the employees in the department after someone selects a department,
/// and the employee details when someone selects an employee. For rows in a
/// list that have ``NavigationLink`` instances, the list draws disclosure
/// chevrons while in the collapsed state.
///
/// ### Control column visibility
///
/// You can programmatically control the visibility of navigation split view
/// columns by creating a ``State`` value of type
/// ``NavigationSplitViewVisibility``. Then pass a ``Binding`` to that state to
/// the appropriate initializer --- such as
/// ``init(columnVisibility:sidebar:detail:)`` for two columns, or
/// the ``init(columnVisibility:sidebar:content:detail:)`` for three columns.
///
/// The following code updates the first example above to always hide the
/// first column when the view appears:
///
///     @State private var employeeIds: Set<Employee.ID> = []
///     @State private var columnVisibility =
///         NavigationSplitViewVisibility.detailOnly
///
///     var body: some View {
///         NavigationSplitView(columnVisibility: $columnVisibility) {
///             List(model.employees, selection: $employeeIds) { employee in
///                 Text(employee.name)
///             }
///         } detail: {
///             EmployeeDetails(for: employeeIds)
///         }
///     }
///
/// The split view ignores the visibility control when it collapses its columns
/// into a stack.
///
/// ### Collapsed split views
///
/// At narrow size classes, such as on iPhone or Apple Watch, a navigation split
/// view collapses into a single stack. Typically SkipUI automatically chooses
/// the view to show on top of this single stack, based on the content of the
/// split view's columns.
///
/// For custom navigation experiences, you can provide more information to help
/// SkipUI choose the right column. Create a `State` value of type
/// ``NavigationSplitViewColumn``. Then pass a `Binding` to that state to the
/// appropriate initializer, such as
/// ``init(preferredCompactColumn:sidebar:detail:)`` or
/// ``init(preferredCompactColumn:sidebar:content:detail:)``.
///
/// The following code shows the blue detail view when run on iPhone. When the
/// person using the app taps the back button, they'll see the yellow view. The
/// value of `preferredPreferredCompactColumn` will change from `.detail` to
/// `.sidebar`:
///
///     @State private var preferredColumn =
///         NavigationSplitViewColumn.detail
///
///     var body: some View {
///         NavigationSplitView(preferredCompactColumn: $preferredColumn) {
///             Color.yellow
///         } detail: {
///             Color.blue
///         }
///     }
///
/// ### Customize a split view
///
/// To specify a preferred column width in a navigation split view, use the
/// ``View/navigationSplitViewColumnWidth(_:)`` modifier. To set minimum,
/// maximum, and ideal sizes for a column, use
/// ``View/navigationSplitViewColumnWidth(min:ideal:max:)``. You can specify a
/// different modifier in each column. The navigation split view does its
/// best to accommodate the preferences that you specify, but might make
/// adjustments based on other constraints.
///
/// To specify how columns in a navigation split view interact, use the
/// ``View/navigationSplitViewStyle(_:)`` modifier with a
/// ``NavigationSplitViewStyle`` value. For example, you can specify
/// whether to emphasize the detail column or to give all of the columns equal
/// prominence.
@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
public struct NavigationSplitView<Sidebar, Content, Detail> : View where Sidebar : View, Content : View, Detail : View {

/// Creates a three-column navigation split view.
///
/// - Parameters:
///   - sidebar: The view to show in the leading column.
///   - content: The view to show in the middle column.
///   - detail: The view to show in the detail area.
public init(@ViewBuilder sidebar: () -> Sidebar, @ViewBuilder content: () -> Content, @ViewBuilder detail: () -> Detail) { fatalError() }

/// Creates a three-column navigation split view that enables programmatic
/// control of leading columns' visibility.
///
/// - Parameters:
///   - columnVisibility: A ``Binding`` to state that controls the
///     visibility of the leading columns.
///   - sidebar: The view to show in the leading column.
///   - content: The view to show in the middle column.
///   - detail: The view to show in the detail area.
public init(columnVisibility: Binding<NavigationSplitViewVisibility>, @ViewBuilder sidebar: () -> Sidebar, @ViewBuilder content: () -> Content, @ViewBuilder detail: () -> Detail) { fatalError() }

/// Creates a two-column navigation split view.
///
/// - Parameters:
///   - sidebar: The view to show in the leading column.
///   - detail: The view to show in the detail area.
public init(@ViewBuilder sidebar: () -> Sidebar, @ViewBuilder detail: () -> Detail) where Content == EmptyView { fatalError() }

/// Creates a two-column navigation split view that enables programmatic
/// control of the sidebar's visibility.
///
/// - Parameters:
///   - columnVisibility: A ``Binding`` to state that controls the
///     visibility of the leading column.
///   - sidebar: The view to show in the leading column.
///   - detail: The view to show in the detail area.
public init(columnVisibility: Binding<NavigationSplitViewVisibility>, @ViewBuilder sidebar: () -> Sidebar, @ViewBuilder detail: () -> Detail) where Content == EmptyView { fatalError() }

@MainActor public var body: some View { get { return stubView() } }

//    public typealias Body = some View
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
extension NavigationSplitView {

/// Creates a three-column navigation split view that enables programmatic
/// control over which column appears on top when the view collapses into a
/// single column in narrow sizes.
///
/// - Parameters:
///   - preferredCompactColumn: A ``Binding`` to state that controls which
///     column appears on top when the view collapses.
///   - sidebar: The view to show in the leading column.
///   - content: The view to show in the middle column.
///   - detail: The view to show in the detail area.
public init(preferredCompactColumn: Binding<NavigationSplitViewColumn>, @ViewBuilder sidebar: () -> Sidebar, @ViewBuilder content: () -> Content, @ViewBuilder detail: () -> Detail) { fatalError() }

/// Creates a three-column navigation split view that enables programmatic
/// control of leading columns' visibility in regular sizes and which column
/// appears on top when the view collapses into a single column in narrow
/// sizes.
///
/// - Parameters:
///   - columnVisibility: A ``Binding`` to state that controls the
///     visibility of the leading columns.
///   - preferredCompactColumn: A ``Binding`` to state that controls which
///     column appears on top when the view collapses.
///   - sidebar: The view to show in the leading column.
///   - content: The view to show in the middle column.
///   - detail: The view to show in the detail area.
public init(columnVisibility: Binding<NavigationSplitViewVisibility>, preferredCompactColumn: Binding<NavigationSplitViewColumn>, @ViewBuilder sidebar: () -> Sidebar, @ViewBuilder content: () -> Content, @ViewBuilder detail: () -> Detail) { fatalError() }

/// Creates a two-column navigation split view that enables programmatic
/// control over which column appears on top when the view collapses into a
/// single column in narrow sizes.
///
/// - Parameters:
///   - preferredCompactColumn: A ``Binding`` to state that controls which
///     column appears on top when the view collapses.
///   - sidebar: The view to show in the leading column.
///   - detail: The view to show in the detail area.
public init(preferredCompactColumn: Binding<NavigationSplitViewColumn>, @ViewBuilder sidebar: () -> Sidebar, @ViewBuilder detail: () -> Detail) where Content == EmptyView { fatalError() }

/// Creates a two-column navigation split view that enables programmatic
/// control of the sidebar's visibility in regular sizes and which column
/// appears on top when the view collapses into a single column in narrow
/// sizes.
///
/// - Parameters:
///   - columnVisibility: A ``Binding`` to state that controls the
///     visibility of the leading column.
///   - preferredCompactColumn: A ``Binding`` to state that controls which
///     column appears on top when the view collapses.
///   - sidebar: The view to show in the leading column.
///   - detail: The view to show in the detail area.
public init(columnVisibility: Binding<NavigationSplitViewVisibility>, preferredCompactColumn: Binding<NavigationSplitViewColumn>, @ViewBuilder sidebar: () -> Sidebar, @ViewBuilder detail: () -> Detail) where Content == EmptyView { fatalError() }
}

/// A view that represents a column in a navigation split view.
///
/// A ``NavigationSplitView`` collapses into a single stack in some contexts,
/// like on iPhone or Apple Watch. Use this type with the
/// `preferredCompactColumn` parameter to control which column of the navigation
/// split view appears on top of the collapsed stack.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
public struct NavigationSplitViewColumn : Hashable, Sendable {

public static var sidebar: NavigationSplitViewColumn { get { fatalError() } }

public static var content: NavigationSplitViewColumn { get { fatalError() } }

public static var detail: NavigationSplitViewColumn { get { fatalError() } }
}

/// The properties of a navigation split view instance.
@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
public struct NavigationSplitViewStyleConfiguration {
}

/// The visibility of the leading columns in a navigation split view.
///
/// Use a value of this type to control the visibility of the columns of a
/// ``NavigationSplitView``. Create a ``State`` property with a
/// value of this type, and pass a ``Binding`` to that state to the
/// ``NavigationSplitView/init(columnVisibility:sidebar:detail:)`` or
/// ``NavigationSplitView/init(columnVisibility:sidebar:content:detail:)``
/// initializer when you create the navigation split view. You can then
/// modify the value elsewhere in your code to:
///
/// * Hide all but the trailing column with ``detailOnly``.
/// * Hide the leading column of a three-column navigation split view
///   with ``doubleColumn``.
/// * Show all the columns with ``all``.
/// * Rely on the automatic behavior for the current context with ``automatic``.
///
/// >Note: Some platforms don't respect every option. For example, macOS always
/// displays the content column.
@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
public struct NavigationSplitViewVisibility : Equatable, Codable, Sendable {

/// Hide the leading two columns of a three-column navigation split view, so
/// that just the detail area shows.
public static var detailOnly: NavigationSplitViewVisibility { get { fatalError() } }

/// Show the content column and detail area of a three-column navigation
/// split view, or the sidebar column and detail area of a two-column
/// navigation split view.
///
/// For a two-column navigation split view, `doubleColumn` is equivalent
/// to `all`.
public static var doubleColumn: NavigationSplitViewVisibility { get { fatalError() } }

/// Show all the columns of a three-column navigation split view.
public static var all: NavigationSplitViewVisibility { get { fatalError() } }

/// Use the default leading column visibility for the current device.
///
/// This computed property returns one of the three concrete cases:
/// ``detailOnly``, ``doubleColumn``, or ``all``.
public static var automatic: NavigationSplitViewVisibility { get { fatalError() } }

/// Encodes this value into the given encoder.
///
/// If the value fails to encode anything, `encoder` will encode an empty
/// keyed container in its place.
///
/// This function throws an error if any values are invalid for the given
/// encoder's format.
///
/// - Parameter encoder: The encoder to write data to.
public func encode(to encoder: Encoder) throws { fatalError() }

/// Creates a new instance by decoding from the given decoder.
///
/// This initializer throws an error if reading from the decoder fails, or
/// if the data read is corrupted or otherwise invalid.
///
/// - Parameter decoder: The decoder to read data from.
public init(from decoder: Decoder) throws { fatalError() }
}
*/
