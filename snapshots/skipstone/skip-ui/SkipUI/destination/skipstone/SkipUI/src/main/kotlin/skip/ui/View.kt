package skip.ui

import kotlin.reflect.KClass
import skip.lib.*
import skip.lib.Array
import skip.lib.Collection
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.runtime.Composable
import skip.model.StateTracking
import skip.foundation.*
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.async
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.MutableState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import android.graphics.Bitmap
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.sharp.*
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.ScaleFactor
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Stable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path.Companion.combine
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.IconButton
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlin.reflect.full.superclasses
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.State
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.Surface
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import android.content.res.Configuration
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import kotlin.reflect.full.companionObjectInstance
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import skip.model.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.popup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import org.burnoutcrew.reorderable.awaitPointerSlopOrCancellation
import kotlin.math.abs
import android.os.VibrationEffect
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.core.util.Consumer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import skip.foundation.LocalizedStringResource
import skip.foundation.Bundle
import skip.foundation.Locale
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.border
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag

interface View {
    // Note: We default the body to invoke the deprecated `ComposeContent` function for backwards compatibility
    // with custom pre-Renderable views that overrode `ComposeContent`
    fun body(): View = ComposeView({ ComposeContent(it) })

    fun animation(animation: Animation?, value: Any?): View {
        return ModifiedContent(content = this, modifier = RenderModifier { renderable, context ->
            val rememberedValue = rememberSaveable(stateSaver = context.stateSaver as Saver<Any?, Any>) { -> mutableStateOf(value) }
            val hasChangedValue = rememberSaveable(stateSaver = context.stateSaver as Saver<Boolean, Any>) { -> mutableStateOf(false) }
            val isValueChange = rememberedValue.value != value
            if (isValueChange) {
                rememberedValue.value = value
                hasChangedValue.value = true
            }
            EnvironmentValues.shared.setValues(l@{ it ->
                // Pass down an infinite repeating animation every time, because it always overrides any withAnimation spec
                if (isValueChange || (animation?.isInfinite == true && hasChangedValue.value)) {
                    it.set_animation(animation)
                }
                return@l ComposeResult.ok
            }, in_ = { -> renderable.Render(context = context) })
        })
    }

    fun animation(animation: Animation?): View {
        return environment({ it -> EnvironmentValues.shared.set_animation(it) }, animation, affectsEvaluate = false)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun springLoadingBehavior(behavior: SpringLoadingBehavior): View = this.sref()

    fun transition(t: AnyTransition): View = ModifiedContent(content = this, modifier = TransitionModifier(transition = t.transition))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun contentTransition(transition: Any): View {
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun scenePadding(edges: Edge.Set = Edge.Set.all): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun scenePadding(padding: ScenePadding, edges: Edge.Set = Edge.Set.all): View = this.sref()
    fun colorScheme(colorScheme: ColorScheme): View {
        return ModifiedContent(content = this, modifier = RenderModifier { renderable, context ->
            MaterialTheme(colorScheme = colorScheme.asMaterialTheme()) { -> renderable.Render(context = context) }
        })
    }

    fun colorScheme(bridgedColorScheme: Int): View = colorScheme(ColorScheme(rawValue = bridgedColorScheme)!!)

    fun preferredColorScheme(colorScheme: ColorScheme?): View = preference(key = PreferredColorSchemePreferenceKey::class, value = PreferredColorScheme(colorScheme = colorScheme))

    fun preferredColorScheme(bridgedColorScheme: Int?): View = preferredColorScheme(if (bridgedColorScheme == null) null else ColorScheme(rawValue = bridgedColorScheme!!))

    fun materialColorScheme(scheme: (@Composable (androidx.compose.material3.ColorScheme, Boolean) -> androidx.compose.material3.ColorScheme)?): View = material3ColorScheme(scheme)

    fun material3ColorScheme(scheme: (@Composable (androidx.compose.material3.ColorScheme, Boolean) -> androidx.compose.material3.ColorScheme)?): View {
        return environment({ it -> EnvironmentValues.shared.set_material3ColorScheme(it) }, scheme, affectsEvaluate = true)
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dropDestination(for_: KClass<*>? = null, isEnabled: Boolean = true, action: (Array<*>, DropSession) -> Unit): View {
        val type = for_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun onDragSessionUpdated(onUpdate: (DragSession) -> Unit): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dropConfiguration(configuration: (DropSession) -> DropConfiguration): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun onDropSessionUpdated(onUpdate: (DropSession) -> Unit): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dragConfiguration(configuration: DragConfiguration): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun draggable(for_: KClass<*>? = null, id: AnyHashable, payload: (Any) -> Any?): View {
        val type = for_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dragContainer(for_: KClass<*>? = null, in_: Namespace.ID? = null, selection: Array<*>, payload: (Array<*>) -> Collection<*>): View {
        val itemType = for_
        val namespace = in_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dragContainer(for_: KClass<*>? = null, id: (Any) -> Any, in_: Namespace.ID? = null, selection: Array<*>, payload: (Array<*>) -> Collection<*>): View {
        val itemType = for_
        val namespace = in_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dragContainer(for_: KClass<*>? = null, in_: Namespace.ID? = null, payload: (Any) -> Collection<*>): View {
        val itemType = for_
        val namespace = in_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dragContainer(for_: KClass<*>? = null, id: (Any) -> Any, in_: Namespace.ID? = null, payload: (Any) -> Collection<*>): View {
        val itemType = for_
        val namespace = in_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dragContainer(for_: KClass<*>? = null, in_: Namespace.ID? = null, selection: Any?, payload: (Any) -> Any?): View {
        val itemType = for_
        val namespace = in_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dragContainer(for_: KClass<*>? = null, id: (Any) -> Any, in_: Namespace.ID? = null, selection: Any?, payload: (Any) -> Any?): View {
        val itemType = for_
        val namespace = in_
        return this.sref()
    }
    fun deleteDisabled(isDisabled: Boolean): View = ModifiedContent(content = this, modifier = EditActionsModifier(isDeleteDisabled = isDisabled))

    fun moveDisabled(isDisabled: Boolean): View = ModifiedContent(content = this, modifier = EditActionsModifier(isMoveDisabled = isDisabled))
    fun menuStyle(style: MenuStyle): View = this.sref()

    fun menuStyle(bridgedStyle: Int): View = menuStyle(MenuStyle(rawValue = bridgedStyle))

    fun menuActionDismissBehavior(behavior: MenuActionDismissBehavior): View = this.sref()

    fun menuActionDismissBehavior(bridgedBehavior: Int): View = menuActionDismissBehavior(MenuActionDismissBehavior(rawValue = bridgedBehavior))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun menuIndicator(visibility: Visibility): View = this.sref()

    fun menuOrder(order: MenuOrder): View = this.sref()

    fun menuOrder(bridgedOrder: Int): View = menuOrder(MenuOrder(rawValue = bridgedOrder))
    fun searchable(text: Binding<String>, placement: SearchFieldPlacement = SearchFieldPlacement.automatic, prompt: Text? = null): View = ModifiedContent(content = this, modifier = SearchableModifier(text = text, prompt = prompt))

    fun searchable(getText: () -> String, setText: (String) -> Unit, prompt: Text?): View = searchable(text = Binding(get = getText, set = setText), prompt = prompt)

    fun searchable(text: Binding<String>, placement: SearchFieldPlacement = SearchFieldPlacement.automatic, prompt: LocalizedStringKey): View = searchable(text = text, placement = placement, prompt = Text(prompt))

    fun searchable(text: Binding<String>, placement: SearchFieldPlacement = SearchFieldPlacement.automatic, prompt: LocalizedStringResource): View = searchable(text = text, placement = placement, prompt = Text(prompt))

    fun searchable(text: Binding<String>, placement: SearchFieldPlacement = SearchFieldPlacement.automatic, prompt: String): View = searchable(text = text, placement = placement, prompt = Text(verbatim = prompt))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun searchToolbarBehavior(behavior: SearchToolbarBehavior): View = this.sref()
    fun toolbar(content: () -> View): View = toolbar(id = "", content = content)

    fun toolbar(id: String, content: () -> View): View = preference(key = ToolbarContentPreferenceKey::class, value = ToolbarContentPreferences(content = arrayOf(content())))

    fun toolbar(id: String, bridgedContent: View): View = preference(key = ToolbarContentPreferenceKey::class, value = ToolbarContentPreferences(content = arrayOf(bridgedContent)))

    fun toolbar(visibility: Visibility): View = _toolbarVisibility(visibility, for_ = arrayOf())

    fun toolbar(visibility: Visibility, vararg for_: ToolbarPlacement): View {
        val bars = Array(for_.asIterable())
        return _toolbarVisibility(visibility, for_ = bars)
    }

    fun toolbarVisibility(visibility: Visibility): View = _toolbarVisibility(visibility, for_ = arrayOf())

    fun toolbarVisibility(visibility: Visibility, vararg for_: ToolbarPlacement): View {
        val bars = Array(for_.asIterable())
        return _toolbarVisibility(visibility, for_ = bars)
    }

    fun toolbarVisibility(bridgedVisibility: Int, bridgedPlacements: Array<Int>): View {
        return _toolbarVisibility(Visibility(rawValue = bridgedVisibility) ?: Visibility.automatic, for_ = bridgedPlacements.compactMap { it -> ToolbarPlacement(rawValue = it) })
    }

    fun _toolbarVisibility(visibility: Visibility, for_: Array<ToolbarPlacement>): View {
        val placements = for_
        var view = this
        var bars = placements.sref()
        if (bars.isEmpty) {
            bars = arrayOf(ToolbarPlacement.bottomBar, ToolbarPlacement.navigationBar, ToolbarPlacement.tabBar)
        }
        if (bars.contains(ToolbarPlacement.tabBar)) {
            view = view.preference(key = TabBarPreferenceKey::class, value = ToolbarBarPreferences(visibility = visibility))
        }
        if (bars.contains(where = { it -> it != ToolbarPlacement.tabBar })) {
            view = view.preference(key = ToolbarPreferenceKey::class, value = ToolbarPreferences(visibility = visibility, for_ = bars))
        }
        return view.sref()
    }

    fun toolbarBackground(style: ShapeStyle): View = _toolbarBackground(style, for_ = arrayOf())

    fun toolbarBackground(style: ShapeStyle, vararg for_: ToolbarPlacement): View {
        val bars = Array(for_.asIterable())
        return _toolbarBackground(style, for_ = bars)
    }

    fun toolbarBackground(style: ShapeStyle, bridgedPlacements: Array<Int>): View {
        return _toolbarBackground(style, for_ = bridgedPlacements.compactMap { it -> ToolbarPlacement(rawValue = it) })
    }

    fun _toolbarBackground(style: ShapeStyle, for_: Array<ToolbarPlacement>): View {
        val placements = for_
        var view = this
        var bars = placements.sref()
        if (bars.isEmpty) {
            bars = arrayOf(ToolbarPlacement.bottomBar, ToolbarPlacement.navigationBar, ToolbarPlacement.tabBar)
        }
        if (bars.contains(ToolbarPlacement.tabBar)) {
            view = view.preference(key = TabBarPreferenceKey::class, value = ToolbarBarPreferences(background = style))
        }
        if (bars.contains(where = { it -> it != ToolbarPlacement.tabBar })) {
            view = view.preference(key = ToolbarPreferenceKey::class, value = ToolbarPreferences(background = style, for_ = bars))
        }
        return view.sref()
    }

    fun toolbarBackground(visibility: Visibility): View = _toolbarBackgroundVisibility(visibility, for_ = arrayOf())

    fun toolbarBackground(visibility: Visibility, vararg for_: ToolbarPlacement): View {
        val bars = Array(for_.asIterable())
        return _toolbarBackgroundVisibility(visibility, for_ = bars)
    }

    fun toolbarBackgroundVisibility(visibility: Visibility): View = _toolbarBackgroundVisibility(visibility, for_ = arrayOf())

    fun toolbarBackgroundVisibility(visibility: Visibility, vararg for_: ToolbarPlacement): View {
        val bars = Array(for_.asIterable())
        return _toolbarBackgroundVisibility(visibility, for_ = bars)
    }

    fun toolbarBackgroundVisibility(bridgedVisibility: Int, bridgedPlacements: Array<Int>): View {
        return _toolbarBackgroundVisibility(Visibility(rawValue = bridgedVisibility) ?: Visibility.automatic, for_ = bridgedPlacements.compactMap { it -> ToolbarPlacement(rawValue = it) })
    }

    fun _toolbarBackgroundVisibility(visibility: Visibility, for_: Array<ToolbarPlacement>): View {
        val placements = for_
        var view = this
        var bars = placements.sref()
        if (bars.isEmpty) {
            bars = arrayOf(ToolbarPlacement.bottomBar, ToolbarPlacement.navigationBar, ToolbarPlacement.tabBar)
        }
        if (bars.contains(ToolbarPlacement.tabBar)) {
            view = view.preference(key = TabBarPreferenceKey::class, value = ToolbarBarPreferences(backgroundVisibility = visibility))
        }
        if (bars.contains(where = { it -> it != ToolbarPlacement.tabBar })) {
            view = view.preference(key = ToolbarPreferenceKey::class, value = ToolbarPreferences(backgroundVisibility = visibility, for_ = bars))
        }
        return view.sref()
    }

    fun toolbarColorScheme(colorScheme: ColorScheme?): View = _toolbarColorScheme(colorScheme, for_ = arrayOf())

    fun toolbarColorScheme(colorScheme: ColorScheme?, vararg for_: ToolbarPlacement): View {
        val bars = Array(for_.asIterable())
        return _toolbarColorScheme(colorScheme, for_ = bars)
    }

    fun toolbarColorScheme(bridgedColorScheme: Int?, bridgedPlacements: Array<Int>): View {
        val colorScheme: ColorScheme? = if (bridgedColorScheme == null) null else ColorScheme(rawValue = bridgedColorScheme!!)
        return _toolbarColorScheme(colorScheme, for_ = bridgedPlacements.compactMap { it -> ToolbarPlacement(rawValue = it) })
    }

    fun _toolbarColorScheme(colorScheme: ColorScheme?, for_: Array<ToolbarPlacement>): View {
        val placements = for_
        var view = this
        var bars = placements.sref()
        if (bars.isEmpty) {
            bars = arrayOf(ToolbarPlacement.bottomBar, ToolbarPlacement.navigationBar, ToolbarPlacement.tabBar)
        }
        if (bars.contains(ToolbarPlacement.tabBar)) {
            view = view.preference(key = TabBarPreferenceKey::class, value = ToolbarBarPreferences(colorScheme = colorScheme))
        }
        if (bars.contains(where = { it -> it != ToolbarPlacement.tabBar })) {
            view = view.preference(key = ToolbarPreferenceKey::class, value = ToolbarPreferences(colorScheme = colorScheme, for_ = bars))
        }
        return view.sref()
    }

    fun toolbarTitleDisplayMode(mode: ToolbarTitleDisplayMode): View = preference(key = ToolbarPreferenceKey::class, value = ToolbarPreferences(titleDisplayMode = mode))

    fun toolbarTitleDisplayMode(bridgedMode: Int): View = toolbarTitleDisplayMode(ToolbarTitleDisplayMode(rawValue = bridgedMode) ?: ToolbarTitleDisplayMode.automatic)

    fun toolbarTitleMenu(content: () -> View): View = preference(key = ToolbarContentPreferenceKey::class, value = ToolbarContentPreferences(content = arrayOf(ToolbarTitleMenu(content = content))))

    fun toolbarTitleMenu(bridgedContent: View): View = preference(key = ToolbarContentPreferenceKey::class, value = ToolbarContentPreferences(content = arrayOf(ToolbarTitleMenu(bridgedContent = bridgedContent))))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun toolbarRole(role: ToolbarRole): View = this.sref()
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun imageScale(scale: Image.Scale): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun allowedDynamicRange(range: Image.DynamicRange?): View = this.sref()
    fun progressViewStyle(style: ProgressViewStyle): View {
        return environment({ it -> EnvironmentValues.shared.set_progressViewStyle(it) }, style, affectsEvaluate = false)
    }

    fun progressViewStyle(bridgedStyle: Int): View = progressViewStyle(ProgressViewStyle(rawValue = bridgedStyle))
    /// Add the given modifier to the underlying Compose view.
    fun composeModifier(modifier: (Modifier) -> Modifier): View {
        return ModifiedContent(content = this, modifier = RenderModifier l@{ it -> return@l modifier(it.modifier) })
    }

    /// Apply the given `ContentModifier`.
    fun applyContentModifier(bridgedContent: Any): View {
        return ((bridgedContent as? ContentModifier)?.modify(view = this) ?: this).sref()
    }
    fun disclosureGroupStyle(style: DisclosureGroupStyle): View {
        // We only support the single .automatic style
        return this.sref()
    }
    fun formStyle(style: FormStyle): View = this.sref()
    /// Expands views with the given lazy item level set in the environment.
    @Composable
    fun EvaluateLazyItems(level: Int, context: ComposeContext): kotlin.collections.List<Renderable> {
        val renderables = Evaluate(context = context, options = EvaluateOptions(lazyItemLevel = level).value)
        if (level <= 0) {
            return renderables.sref()
        }
        return renderables.map l@{ renderable ->
            if (!(renderable.strip() is LazyLevelRenderable)) {
                return@l LazyLevelRenderable(content = renderable, level = level)
            } else {
                return@l renderable
            }
        }
    }
    fun listRowBackground(view: View?): View = ModifiedContent(content = this, modifier = ListItemModifier(background = view))

    fun listRowSeparator(visibility: Visibility, edges: VerticalEdge.Set = VerticalEdge.Set.all): View = ModifiedContent(content = this, modifier = ListItemModifier(separator = visibility))

    fun listRowSeparator(bridgedVisibility: Int, bridgedEdges: Int): View = listRowSeparator(Visibility(rawValue = bridgedVisibility) ?: Visibility.automatic, edges = VerticalEdge.Set(rawValue = bridgedEdges))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun listRowSeparatorTint(color: Color?, edges: VerticalEdge.Set = VerticalEdge.Set.all): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun listSectionIndexVisibility(visibility: Visibility): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun listSectionSeparator(visibility: Visibility, edges: VerticalEdge.Set = VerticalEdge.Set.all): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun listSectionSeparatorTint(color: Color?, edges: VerticalEdge.Set = VerticalEdge.Set.all): View = this.sref()

    fun listStyle(style: ListStyle): View {
        return environment({ it -> EnvironmentValues.shared.set_listStyle(it) }, style, affectsEvaluate = false)
    }

    fun listStyle(bridgedStyle: Int): View = listStyle(ListStyle(rawValue = bridgedStyle))

    fun listItemTint(tint: Color?): View {
        return environment({ it -> EnvironmentValues.shared.set_listItemTint(it) }, tint, affectsEvaluate = false)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun listItemTint(tint: ListItemTint?): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun listRowInsets(insets: EdgeInsets?): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun listRowSpacing(spacing: Double?): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun listSectionSpacing(spacing: ListSectionSpacing): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun listSectionSpacing(spacing: Double): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun swipeActions(edge: HorizontalEdge = HorizontalEdge.trailing, allowsFullSwipe: Boolean = true, content: () -> View): View = this.sref()
    fun navigationBarBackButtonHidden(hidesBackButton: Boolean = true): View = preference(key = ToolbarPreferenceKey::class, value = ToolbarPreferences(backButtonHidden = hidesBackButton))

    fun navigationBarTitleDisplayMode(displayMode: NavigationBarItem.TitleDisplayMode): View {
        val toolbarTitleDisplayMode: ToolbarTitleDisplayMode
        when (displayMode) {
            NavigationBarItem.TitleDisplayMode.automatic -> toolbarTitleDisplayMode = ToolbarTitleDisplayMode.automatic
            NavigationBarItem.TitleDisplayMode.inline_ -> toolbarTitleDisplayMode = ToolbarTitleDisplayMode.inline_
            NavigationBarItem.TitleDisplayMode.large -> toolbarTitleDisplayMode = ToolbarTitleDisplayMode.large
        }
        return preference(key = ToolbarPreferenceKey::class, value = ToolbarPreferences(titleDisplayMode = toolbarTitleDisplayMode))
    }

    fun navigationBarTitleDisplayMode(bridgedDisplayMode: Int): View = navigationBarTitleDisplayMode(NavigationBarItem.TitleDisplayMode(rawValue = bridgedDisplayMode) ?: NavigationBarItem.TitleDisplayMode.automatic)

    fun <D> navigationDestination(for_: KClass<D>, destination: (D) -> View): View where D: Any {
        val data = for_
        val destinations: Dictionary<AnyHashable, NavigationDestination> = dictionaryOf(Tuple2(data, NavigationDestination(destination = { it -> destination(it as D) })))
        return preference(key = NavigationDestinationsPreferenceKey::class, value = destinations)
    }

    fun navigationDestination(destinationKey: String, bridgedDestination: (Any) -> View): View {
        val destinations: Dictionary<AnyHashable, NavigationDestination> = dictionaryOf(Tuple2(destinationKey, NavigationDestination(destination = { it -> bridgedDestination(it) })))
        return preference(key = NavigationDestinationsPreferenceKey::class, value = destinations)
    }

    fun navigationDestination(isPresented: Binding<Boolean>, destination: () -> View): View {
        val destinationView = ComposeBuilder.from(destination)
        return ModifiedContent(content = this, modifier = SideEffectModifier l@{ context ->
            val id = rememberSaveable(stateSaver = context.stateSaver as Saver<String?, Any>) { -> mutableStateOf<String?>(null) }
            val navigator_0 = LocalNavigator.current.sref()
            if (navigator_0 == null) {
                return@l ComposeResult.ok
            }
            if (isPresented.wrappedValue) {
                if (id.value == null || !navigator_0.isViewPresented(id = id.value!!)) {
                    id.value = navigator_0.navigateToView(destinationView, binding = isPresented)
                }
            } else {
                id.value.sref()?.let { idValue ->
                    if (navigator_0.isViewPresented(id = idValue, asTop = true)) {
                        navigator_0.navigateBack()
                    }
                }
                id.value = null
            }
            return@l ComposeResult.ok
        })
    }

    fun navigationDestination(getIsPresented: () -> Boolean, setIsPresented: (Boolean) -> Unit, bridgedDestination: View): View {
        return navigationDestination(isPresented = Binding(get = getIsPresented, set = setIsPresented), destination = { ->
            ComposeBuilder { composectx: ComposeContext ->
                bridgedDestination.Compose(composectx)
                ComposeResult.ok
            }
        })
    }

    fun <D> navigationDestination(item: Binding<D?>, destination: (D) -> View): View {
        return ModifiedContent(content = this, modifier = SideEffectModifier l@{ context ->
            val id = rememberSaveable(stateSaver = context.stateSaver as Saver<String?, Any>) { -> mutableStateOf<String?>(null) }
            val isBeingDismissedByNavigator = remember { -> mutableStateOf(false) }
            val navigator_1 = LocalNavigator.current.sref()
            if (navigator_1 == null) {
                return@l ComposeResult.ok
            }

            val matchtarget_0 = item.wrappedValue
            if (matchtarget_0 != null) {
                val itemValue = matchtarget_0
                val isPresented = if (id.value != null) navigator_1.isViewPresented(id = id.value!!) else false
                if (id.value == null || !isPresented) {
                    var consumed = false
                    val binding = Binding<Boolean>(get = { -> item.wrappedValue != null }, set = { it ->
                        if (!it) {
                            if (!consumed) {
                                consumed = true
                                isBeingDismissedByNavigator.value = true
                                item.wrappedValue = null
                            }
                        }
                    })
                    val destinationView = NavigationDestinationItemWrapper(item = item, isBeingDismissedByNavigator = isBeingDismissedByNavigator, navigationId = id, destination = destination)
                    val newId = navigator_1.navigateToView(destinationView, binding = binding)
                    id.value = newId
                }
            } else {
                id.value.sref()?.let { idValue ->
                    if (navigator_1.isViewPresented(id = idValue, asTop = true)) {
                        navigator_1.navigateBack()
                    }
                }
                id.value = null
            }
            return@l ComposeResult.ok
        })
    }

    fun navigationDestination(getItem: () -> Any?, setItem: (Any?) -> Unit, bridgedDestination: (Any) -> View): View {
        val binding: Binding<AnyHashable?> = Binding(get = { -> getItem() as? AnyHashable }, set = { it -> setItem(it) })
        return navigationDestination(item = binding, destination = bridgedDestination)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun navigationDocument(url: URL): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun navigationSplitViewColumnWidth(width: Double): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun navigationSplitViewColumnWidth(min: Double? = null, ideal: Double, max: Double? = null): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun navigationSplitViewStyle(style: NavigationSplitViewStyle): View = this.sref()

    fun navigationTitle(title: Text): View = preference(key = NavigationTitlePreferenceKey::class, value = title)

    fun navigationTitle(title: LocalizedStringKey): View = preference(key = NavigationTitlePreferenceKey::class, value = Text(title))

    fun navigationTitle(title: LocalizedStringResource): View = preference(key = NavigationTitlePreferenceKey::class, value = Text(title))

    fun navigationTitle(title: String): View = preference(key = NavigationTitlePreferenceKey::class, value = Text(verbatim = title))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun navigationTitle(title: Binding<String>): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun navigationSubtitle(subtitle: Text): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun navigationSubtitle(subtitle: LocalizedStringKey): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun navigationSubtitle(subtitle: LocalizedStringResource): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun navigationSubtitle(subtitle: String): View = this.sref()

    fun material3TopAppBar(options: @Composable (Material3TopAppBarOptions) -> Material3TopAppBarOptions): View {
        return environment({ it -> EnvironmentValues.shared.set_material3TopAppBar(it) }, options, affectsEvaluate = false)
    }

    fun material3BottomAppBar(options: @Composable (Material3BottomAppBarOptions) -> Material3BottomAppBarOptions): View {
        return environment({ it -> EnvironmentValues.shared.set_material3BottomAppBar(it) }, options, affectsEvaluate = false)
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun contentMargins(edges: Edge.Set = Edge.Set.all, insets: EdgeInsets, for_: ContentMarginPlacement = ContentMarginPlacement.automatic): View {
        val placement = for_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun contentMargins(edges: Edge.Set = Edge.Set.all, length: Double?, for_: ContentMarginPlacement = ContentMarginPlacement.automatic): View {
        val placement = for_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun contentMargins(length: Double, for_: ContentMarginPlacement = ContentMarginPlacement.automatic): View {
        val placement = for_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun scrollBounceBehavior(behavior: ScrollBounceBehavior, axes: Axis.Set = Axis.Set.of(Axis.Set.vertical)): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun scrollClipDisabled(disabled: Boolean = true): View = this.sref()

    fun scrollContentBackground(visibility: Visibility): View {
        return environment({ it -> EnvironmentValues.shared.set_scrollContentBackground(it) }, visibility, affectsEvaluate = false)
    }

    fun scrollContentBackground(bridgedVisibility: Int): View = scrollContentBackground(Visibility(rawValue = bridgedVisibility) ?: Visibility.automatic)

    fun scrollDismissesKeyboard(mode: ScrollDismissesKeyboardMode): View {
        return environment({ it -> EnvironmentValues.shared.setscrollDismissesKeyboardMode(it) }, mode, affectsEvaluate = false)
    }

    fun scrollDismissesKeyboard(bridgedMode: Int): View = scrollDismissesKeyboard(ScrollDismissesKeyboardMode(rawValue = bridgedMode) ?: ScrollDismissesKeyboardMode.automatic)

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun scrollDisabled(disabled: Boolean): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun scrollEdgeEffectStyle(style: ScrollEdgeEffectStyle?, for_: Edge.Set): View {
        val edges = for_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun scrollEdgeEffectDisabled(disabled: Boolean = true, for_: Edge.Set = Edge.Set.all): View {
        val edges = for_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun scrollIndicators(visibility: ScrollIndicatorVisibility, axes: Axis.Set = Axis.Set.of(Axis.Set.vertical, Axis.Set.horizontal)): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun scrollIndicatorsFlash(onAppear: Boolean): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun scrollIndicatorsFlash(trigger: Equatable): View {
        val value = trigger
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun scrollPosition(id: Binding<Hashable?>): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun scrollPosition(initialAnchor: UnitPoint?): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun scrollTarget(isEnabled: Boolean = true): View = this.sref()

    fun scrollTargetBehavior(behavior: ScrollTargetBehavior): View {
        return environment({ it -> EnvironmentValues.shared.set_scrollTargetBehavior(it) }, behavior, affectsEvaluate = false)
    }

    fun scrollTargetLayout(isEnabled: Boolean = true): View {
        // We do not support specifying scroll targets, but we want the natural pattern of using this modifier
        // on the VStack/HStack content of a ScrollView to work without #if SKIP-ing it out
        return this.sref()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun sectionIndexLabel(label: Text?): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun sectionIndexLabel(label: String?): View = this.sref()
    fun tabItem(label: () -> View): View = ModifiedContent(content = this, modifier = TabItemModifier(label = label))

    fun tabItem(bridgedLabel: View): View {
        return tabItem({ ->
            ComposeBuilder { composectx: ComposeContext ->
                bridgedLabel.Compose(composectx)
                ComposeResult.ok
            }
        })
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun tabViewBottomAccessory(content: () -> View): View = this.sref()

    fun tabViewStyle(style: TabViewStyle): View {
        return environment({ it -> EnvironmentValues.shared.set_tabViewStyle(it) }, style, affectsEvaluate = false)
    }

    fun tabViewStyle(bridgedStyle: Int, bridgedDisplayMode: Int? = null): View {
        val style: TabViewStyle
        when (bridgedStyle) {
            TabBarOnlyTabViewStyle.identifier -> style = TabBarOnlyTabViewStyle()
            PageTabViewStyle.identifier -> {
                val indexDisplayMode: PageTabViewStyle.IndexDisplayMode = (if (bridgedDisplayMode == null) null else PageTabViewStyle.IndexDisplayMode(rawValue = bridgedDisplayMode!!)) ?: PageTabViewStyle.IndexDisplayMode.automatic
                style = PageTabViewStyle(indexDisplayMode = indexDisplayMode)
            }
            else -> style = DefaultTabViewStyle()
        }
        return tabViewStyle(style)
    }

    fun tabBarMinimizeBehavior(behavior: TabBarMinimizeBehavior): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun customizationBehavior(behavior: TabCustomizationBehavior, vararg for_: AdaptableTabBarPlacement): View {
        val placements = Array(for_.asIterable())
        return this.sref()
    }

    fun customizationID(id: String): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun defaultAdaptableTabBarPlacement(defaultPlacement: AdaptableTabBarPlacement = AdaptableTabBarPlacement.automatic): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun defaultVisibility(visibility: Visibility, vararg for_: AdaptableTabBarPlacement): View {
        val placements = Array(for_.asIterable())
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun tabPlacement(placement: TabPlacement): View = this.sref()

    fun material3NavigationBar(options: @Composable (Material3NavigationBarOptions) -> Material3NavigationBarOptions): View {
        return environment({ it -> EnvironmentValues.shared.set_material3NavigationBar(it) }, options, affectsEvaluate = false)
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun tableColumnHeaders(visibility: Visibility): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun tableStyle(style: TableStyle): View = this.sref()
    fun buttonStyle(style: ButtonStyle): View = ModifiedContent(content = this, modifier = ButtonStyleModifier(style = style))

    fun buttonStyle(bridgedStyle: Int): View = buttonStyle(ButtonStyle(rawValue = bridgedStyle))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun buttonRepeatBehavior(behavior: ButtonRepeatBehavior): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun buttonBorderShape(shape: Any): View = this.sref()

    fun buttonSizing(sizing: ButtonSizing): View {
        // We only support .automatic
        return this.sref()
    }

    /// Compose button customization.
    fun material3Button(options: @Composable (Material3ButtonOptions) -> Material3ButtonOptions): View {
        return environment({ it -> EnvironmentValues.shared.set_material3Button(it) }, options, affectsEvaluate = false)
    }

    fun material3Ripple(options: @Composable (Material3RippleOptions?) -> Material3RippleOptions?): View {
        return ModifiedContent(content = this, modifier = RenderModifier { renderable, context ->
            val rippleConfiguration = LocalRippleConfiguration.current.sref()
            val rippleOptions: Material3RippleOptions?
            if (rippleConfiguration != null) {
                rippleOptions = options(Material3RippleOptions(rippleConfiguration))
            } else {
                rippleOptions = options(null)
            }
            val provided = LocalRippleConfiguration provides rippleOptions?.asConfiguration()
            CompositionLocalProvider(provided) { -> renderable.Render(context = context) }
        })
    }
    fun controlGroupStyle(style: ControlGroupStyle): View = this.sref()
    fun datePickerStyle(style: DatePickerStyle): View {
        // We only support .automatic / .compact
        return this.sref()
    }

    fun datePickerStyle(bridgedStyle: Int): View = datePickerStyle(DatePickerStyle(rawValue = bridgedStyle))
    fun pickerStyle(style: PickerStyle): View = ModifiedContent(content = this, modifier = PickerStyleModifier(style = style))

    fun pickerStyle(bridgedStyle: Int): View = pickerStyle(PickerStyle(rawValue = bridgedStyle))

    /// Compose segmented button customization for `Picker`.
    fun material3SegmentedButton(options: @Composable (Material3SegmentedButtonOptions) -> Material3SegmentedButtonOptions): View {
        return environment({ it -> EnvironmentValues.shared.set_material3SegmentedButton(it) }, options, affectsEvaluate = false)
    }
    fun toggleStyle(style: ToggleStyle): View {
        // We only support Android's Switch control
        return this.sref()
    }

    fun environmentObject(object_: Any): View = environmentObject(type = type(of = object_), object_ = object_)

    // Must be public to allow access from our inline `environment` function.
    fun environmentObject(type: KClass<*>, object_: Any?): View = ModifiedContent(content = this, modifier = EnvironmentObjectModifier(type = type, value = object_))

    // We rely on the transpiler to turn the `WriteableKeyPath` provided in code into a `setValue` closure
    fun <V> environment(setValue: (V) -> Unit, value: V): View = environment(setValue, value, affectsEvaluate = true)

    fun <V> environment(setValue: (V) -> Unit, value: V, affectsEvaluate: Boolean): View {
        return ModifiedContent(content = this, modifier = EnvironmentModifier(affectsEvaluate = affectsEvaluate) l@{ _ ->
            setValue(value)
            return@l ComposeResult.ok
        })
    }

    fun environment(bridgedKey: String, value: EnvironmentSupport?): View {
        return ModifiedContent(content = this, modifier = EnvironmentModifier l@{ environment ->
            environment.setBridged(key = bridgedKey, value = value)
            return@l ComposeResult.ok
        })
    }
    fun preference(key: Any, value: Any?): View {
        return ModifiedContent(content = this, modifier = SideEffectModifier l@{ context ->
            PreferenceValues.shared.contribute(context = context, key = key, value = value)
            return@l ComposeResult.ok
        })
    }

    public fun <K: PreferenceKey<V>, V> onPreferenceChange(key: KClass<K>, perform: (V) -> Unit): View {
        val action = perform
        // Work around transpiler bug
        val companion = key.companionObjectInstance as PreferenceKeyCompanion<V>
        return onPreferenceChange(key = key, defaultValue = companion.defaultValue, reducer = l@{ value, nextValue ->
            var updatedValue = (value as V).sref()
            companion.reduce(value = InOut({ updatedValue }, { updatedValue = it }), nextValue = { -> nextValue as V })
            return@l updatedValue
        }, action = { value -> action(value as V) })
    }

    fun onPreferenceChange(key: Any, defaultValue: Any?, reducer: (Any?, Any?) -> Any?, action: (Any?) -> Unit): View {
        return ModifiedContent(content = this, modifier = RenderModifier { renderable, context ->
            val preference = rememberSaveable(stateSaver = context.stateSaver as Saver<Preference<Any?>, Any>) { -> mutableStateOf(Preference<Any?>(key = key, initialValue = defaultValue, reducer = reducer)) }
            val preferenceCollector = PreferenceCollector<Any?>(key = key, state = preference)
            val currentAction = rememberUpdatedState(action)
            LaunchedEffect(true) { ->
                snapshotFlow { -> preference.value.reduced }
                    .distinctUntilChanged()
                    .collect { newValue -> currentAction.value(newValue) }
            }

            PreferenceValues.shared.collectPreferences(arrayOf(preferenceCollector)) { -> renderable.Render(context = context) }
        })
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun glassEffect(glass: Glass = Glass.regular, in_: Shape = Capsule.capsule, isEnabled: Boolean = true): View {
        val shape = in_
        return this.sref()
    }

    fun glassEffectTransition(transition: GlassEffectTransition, isEnabled: Boolean = true): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun glassEffectUnion(id: Hashable?, namespace: Namespace.ID): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun glassEffectID(id: Hashable?, in_: Namespace.ID): View {
        val namespace = in_
        return this.sref()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun symbolEffect(effect: Any, options: Any? = null, isActive: Boolean = true): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun symbolEffect(effect: Any, options: Any? = null, value: Any): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun symbolRenderingMode(mode: SymbolRenderingMode?): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun symbolVariant(variant: SymbolVariants): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun symbolVariableValueMode(mode: SymbolVariableValueMode?): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun symbolColorRenderingMode(mode: SymbolColorRenderingMode?): View = this.sref()
    fun alert(titleKey: LocalizedStringKey, isPresented: Binding<Boolean>, actions: () -> View): View = alert(Text(titleKey), isPresented = isPresented, actions = actions)

    fun alert(titleResource: LocalizedStringResource, isPresented: Binding<Boolean>, actions: () -> View): View = alert(Text(titleResource), isPresented = isPresented, actions = actions)

    fun alert(title: String, isPresented: Binding<Boolean>, actions: () -> View): View = alert(Text(verbatim = title), isPresented = isPresented, actions = actions)

    fun alert(title: Text, isPresented: Binding<Boolean>, actions: () -> View): View {
        return ModifiedContent(content = this, modifier = PresentationModifier(providesNavigation = true) { context -> AlertPresentation(title = title, isPresented = isPresented, context = context, actions = actions()) })
    }

    fun alert(title: Text, getIsPresented: () -> Boolean, setIsPresented: (Boolean) -> Unit, bridgedActions: View): View {
        val isPresented = Binding(get = getIsPresented, set = setIsPresented)
        return alert(title, isPresented = isPresented, actions = { ->
            ComposeBuilder { composectx: ComposeContext ->
                bridgedActions.Compose(composectx)
                ComposeResult.ok
            }
        })
    }

    fun alert(titleKey: LocalizedStringKey, isPresented: Binding<Boolean>, actions: () -> View, message: () -> View): View = alert(Text(titleKey), isPresented = isPresented, actions = actions, message = message)

    fun alert(titleResource: LocalizedStringResource, isPresented: Binding<Boolean>, actions: () -> View, message: () -> View): View = alert(Text(titleResource), isPresented = isPresented, actions = actions, message = message)

    fun alert(title: String, isPresented: Binding<Boolean>, actions: () -> View, message: () -> View): View = alert(Text(verbatim = title), isPresented = isPresented, actions = actions, message = message)

    fun alert(title: Text, isPresented: Binding<Boolean>, actions: () -> View, message: () -> View): View {
        return ModifiedContent(content = this, modifier = PresentationModifier(providesNavigation = true) { context -> AlertPresentation(title = title, isPresented = isPresented, context = context, actions = actions(), message = message()) })
    }

    fun alert(title: Text, getIsPresented: () -> Boolean, setIsPresented: (Boolean) -> Unit, bridgedActions: View, bridgedMessage: View): View {
        val isPresented = Binding(get = getIsPresented, set = setIsPresented)
        return alert(title, isPresented = isPresented, actions = { ->
            ComposeBuilder { composectx: ComposeContext ->
                bridgedActions.Compose(composectx)
                ComposeResult.ok
            }
        }, message = { ->
            ComposeBuilder { composectx: ComposeContext ->
                bridgedMessage.Compose(composectx)
                ComposeResult.ok
            }
        })
    }

    fun <T> alert(titleKey: LocalizedStringKey, isPresented: Binding<Boolean>, presenting: T?, actions: (T) -> View): View {
        val data = presenting
        return alert(Text(titleKey), isPresented = isPresented, presenting = data, actions = actions)
    }

    fun <T> alert(titleResource: LocalizedStringResource, isPresented: Binding<Boolean>, presenting: T?, actions: (T) -> View): View {
        val data = presenting
        return alert(Text(titleResource), isPresented = isPresented, presenting = data, actions = actions)
    }

    fun <T> alert(title: String, isPresented: Binding<Boolean>, presenting: T?, actions: (T) -> View): View {
        val data = presenting
        return alert(Text(verbatim = title), isPresented = isPresented, presenting = data, actions = actions)
    }

    fun <T> alert(title: Text, isPresented: Binding<Boolean>, presenting: T?, actions: (T) -> View): View {
        val data = presenting
        val actionsWithData: () -> View
        if (data != null) {
            actionsWithData = { -> actions(data) }
        } else {
            actionsWithData = { -> EmptyView() }
        }
        return alert(title, isPresented = isPresented, actions = actionsWithData)
    }

    fun <T> alert(titleKey: LocalizedStringKey, isPresented: Binding<Boolean>, presenting: T?, actions: (T) -> View, message: (T) -> View): View {
        val data = presenting
        return alert(Text(titleKey), isPresented = isPresented, presenting = data, actions = actions, message = message)
    }

    fun <T> alert(titleResource: LocalizedStringResource, isPresented: Binding<Boolean>, presenting: T?, actions: (T) -> View, message: (T) -> View): View {
        val data = presenting
        return alert(Text(titleResource), isPresented = isPresented, presenting = data, actions = actions, message = message)
    }

    fun <T> alert(title: String, isPresented: Binding<Boolean>, presenting: T?, actions: (T) -> View, message: (T) -> View): View {
        val data = presenting
        return alert(Text(verbatim = title), isPresented = isPresented, presenting = data, actions = actions, message = message)
    }

    fun <T> alert(title: Text, isPresented: Binding<Boolean>, presenting: T?, actions: (T) -> View, message: (T) -> View): View {
        val data = presenting
        val actionsWithData: () -> View
        val messageWithData: () -> View
        if (data != null) {
            actionsWithData = { -> actions(data) }
            messageWithData = { -> message(data) }
        } else {
            actionsWithData = { -> EmptyView() }
            messageWithData = { -> EmptyView() }
        }
        return alert(title, isPresented = isPresented, actions = actionsWithData, message = messageWithData)
    }

    //    public func alert<E>(isPresented: Binding<Bool>, error: E?, @ViewBuilder actions: () -> any View) -> some View where E : LocalizedError {
    //        #if SKIP
    //        let titleText: Text?
    //        let titleResource: Int?
    //        let actions: any View
    //        if let error {
    //            titleText = Text(verbatim: error.errorDescription ?? error.localizedDescription)
    //            titleResource = nil
    //            actions = actions()
    //        } else {
    //            titleText = nil
    //            titleResource = android.R.string.dialog_alert_title
    //            actions = EmptyView()
    //        }
    //        return ModifiedContent(content: self, modifier: PresentationModifier(providesNavigation: true) { context in
    //            AlertPresentation(title: titleText, titleResource: titleResource, isPresented: isPresented, context: context, actions: actions)
    //        })
    //        #else
    //        return self
    //        #endif
    //    }
    //
    //    public func alert<E>(isPresented: Binding<Bool>, error: E?, @ViewBuilder actions: () -> any View, @ViewBuilder message: () -> any View) -> some View where E : LocalizedError {
    //        #if SKIP
    //        let titleText: Text?
    //        let titleResource: Int?
    //        let actions: any View
    //        let message: any View
    //        if let error {
    //            titleText = Text(verbatim: error.localizedDescription)
    //            titleResource = nil
    //            actions = actions()
    //            message = message()
    //        } else {
    //            titleText = nil
    //            titleResource = android.R.string.dialog_alert_title
    //            actions = EmptyView()
    //            message = EmptyView()
    //        }
    //        return ModifiedContent(content: self, modifier: PresentationModifier(providesNavigation: true) { context in
    //            AlertPresentation(title: titleText, titleResource: titleResource, isPresented: isPresented, context: context, actions: actions, message: message)
    //        })
    //        #else
    //        return self
    //        #endif
    //    }

    fun confirmationDialog(titleKey: LocalizedStringKey, isPresented: Binding<Boolean>, titleVisibility: Visibility = Visibility.automatic, actions: () -> View): View = confirmationDialog(Text(titleKey), isPresented = isPresented, titleVisibility = titleVisibility, actions = actions)

    fun confirmationDialog(titleResource: LocalizedStringResource, isPresented: Binding<Boolean>, titleVisibility: Visibility = Visibility.automatic, actions: () -> View): View = confirmationDialog(Text(titleResource), isPresented = isPresented, titleVisibility = titleVisibility, actions = actions)

    fun confirmationDialog(title: String, isPresented: Binding<Boolean>, titleVisibility: Visibility = Visibility.automatic, actions: () -> View): View = confirmationDialog(Text(verbatim = title), isPresented = isPresented, titleVisibility = titleVisibility, actions = actions)

    fun confirmationDialog(title: Text, isPresented: Binding<Boolean>, titleVisibility: Visibility = Visibility.automatic, actions: () -> View): View {
        return ModifiedContent(content = this, modifier = PresentationModifier(providesNavigation = true) { context -> ConfirmationDialogPresentation(title = if (titleVisibility != Visibility.visible) null else title, isPresented = isPresented, context = context, actions = actions()) })
    }

    fun confirmationDialog(title: Text, getIsPresented: () -> Boolean, setIsPresented: (Boolean) -> Unit, bridgedTitleVisibility: Int, bridgedActions: View): View {
        val isPresented = Binding(get = getIsPresented, set = setIsPresented)
        return confirmationDialog(title, isPresented = isPresented, titleVisibility = Visibility(rawValue = bridgedTitleVisibility) ?: Visibility.automatic, actions = { ->
            ComposeBuilder { composectx: ComposeContext ->
                bridgedActions.Compose(composectx)
                ComposeResult.ok
            }
        })
    }

    fun confirmationDialog(titleKey: LocalizedStringKey, isPresented: Binding<Boolean>, titleVisibility: Visibility = Visibility.automatic, actions: () -> View, message: () -> View): View = confirmationDialog(Text(titleKey), isPresented = isPresented, titleVisibility = titleVisibility, actions = actions, message = message)

    fun confirmationDialog(titleResource: LocalizedStringResource, isPresented: Binding<Boolean>, titleVisibility: Visibility = Visibility.automatic, actions: () -> View, message: () -> View): View = confirmationDialog(Text(titleResource), isPresented = isPresented, titleVisibility = titleVisibility, actions = actions, message = message)

    fun confirmationDialog(title: String, isPresented: Binding<Boolean>, titleVisibility: Visibility = Visibility.automatic, actions: () -> View, message: () -> View): View = confirmationDialog(Text(verbatim = title), isPresented = isPresented, titleVisibility = titleVisibility, actions = actions, message = message)

    fun confirmationDialog(title: Text, isPresented: Binding<Boolean>, titleVisibility: Visibility = Visibility.automatic, actions: () -> View, message: () -> View): View {
        return ModifiedContent(content = this, modifier = PresentationModifier(providesNavigation = true) { context -> ConfirmationDialogPresentation(title = if (titleVisibility != Visibility.visible) null else title, isPresented = isPresented, context = context, actions = actions(), message = message()) })
    }

    fun confirmationDialog(title: Text, getIsPresented: () -> Boolean, setIsPresented: (Boolean) -> Unit, bridgedTitleVisibility: Int, bridgedActions: View, bridgedMessage: View): View {
        val isPresented = Binding(get = getIsPresented, set = setIsPresented)
        return confirmationDialog(title, isPresented = isPresented, titleVisibility = Visibility(rawValue = bridgedTitleVisibility) ?: Visibility.automatic, actions = { ->
            ComposeBuilder { composectx: ComposeContext ->
                bridgedActions.Compose(composectx)
                ComposeResult.ok
            }
        }, message = { ->
            ComposeBuilder { composectx: ComposeContext ->
                bridgedMessage.Compose(composectx)
                ComposeResult.ok
            }
        })
    }

    fun <T> confirmationDialog(titleKey: LocalizedStringKey, isPresented: Binding<Boolean>, titleVisibility: Visibility = Visibility.automatic, presenting: T?, actions: (T) -> View): View {
        val data = presenting
        return confirmationDialog(Text(titleKey), isPresented = isPresented, titleVisibility = titleVisibility, presenting = data, actions = actions)
    }

    fun <T> confirmationDialog(titleResource: LocalizedStringResource, isPresented: Binding<Boolean>, titleVisibility: Visibility = Visibility.automatic, presenting: T?, actions: (T) -> View): View {
        val data = presenting
        return confirmationDialog(Text(titleResource), isPresented = isPresented, titleVisibility = titleVisibility, presenting = data, actions = actions)
    }

    fun <T> confirmationDialog(title: String, isPresented: Binding<Boolean>, titleVisibility: Visibility = Visibility.automatic, presenting: T?, actions: (T) -> View): View {
        val data = presenting
        return confirmationDialog(Text(verbatim = title), isPresented = isPresented, titleVisibility = titleVisibility, presenting = data, actions = actions)
    }

    fun <T> confirmationDialog(title: Text, isPresented: Binding<Boolean>, titleVisibility: Visibility = Visibility.automatic, presenting: T?, actions: (T) -> View): View {
        val data = presenting
        val actionsWithData: () -> View
        if (data != null) {
            actionsWithData = { -> actions(data) }
        } else {
            actionsWithData = { -> EmptyView() }
        }
        return confirmationDialog(title, isPresented = isPresented, titleVisibility = titleVisibility, actions = actionsWithData)
    }

    fun <T> confirmationDialog(titleKey: LocalizedStringKey, isPresented: Binding<Boolean>, titleVisibility: Visibility = Visibility.automatic, presenting: T?, actions: (T) -> View, message: (T) -> View): View {
        val data = presenting
        return confirmationDialog(Text(titleKey), isPresented = isPresented, titleVisibility = titleVisibility, presenting = data, actions = actions, message = message)
    }

    fun <T> confirmationDialog(titleResource: LocalizedStringResource, isPresented: Binding<Boolean>, titleVisibility: Visibility = Visibility.automatic, presenting: T?, actions: (T) -> View, message: (T) -> View): View {
        val data = presenting
        return confirmationDialog(Text(titleResource), isPresented = isPresented, titleVisibility = titleVisibility, presenting = data, actions = actions, message = message)
    }

    fun <T> confirmationDialog(title: String, isPresented: Binding<Boolean>, titleVisibility: Visibility = Visibility.automatic, presenting: T?, actions: (T) -> View, message: (T) -> View): View {
        val data = presenting
        return confirmationDialog(Text(verbatim = title), isPresented = isPresented, titleVisibility = titleVisibility, presenting = data, actions = actions, message = message)
    }

    fun <T> confirmationDialog(title: Text, isPresented: Binding<Boolean>, titleVisibility: Visibility = Visibility.automatic, presenting: T?, actions: (T) -> View, message: (T) -> View): View {
        val data = presenting
        val actionsWithData: () -> View
        val messageWithData: () -> View
        if (data != null) {
            actionsWithData = { -> actions(data) }
            messageWithData = { -> message(data) }
        } else {
            actionsWithData = { -> EmptyView() }
            messageWithData = { -> EmptyView() }
        }
        return confirmationDialog(title, isPresented = isPresented, titleVisibility = titleVisibility, actions = actionsWithData, message = messageWithData)
    }

    fun <Item> fullScreenCover(item: Binding<Item?>, onDismiss: (() -> Unit)? = null, content: (Item) -> View): View {
        val isPresented = Binding<Boolean>(get = l@{ -> return@l item.wrappedValue != null }, set = { newValue ->
            if (!newValue) {
                item.wrappedValue = null
            }
        })

        return fullScreenCover(isPresented = isPresented, onDismiss = onDismiss) { ->
            ComposeBuilder { composectx: ComposeContext ->
                item.wrappedValue.sref()?.let { unwrappedItem ->
                    content(unwrappedItem).Compose(composectx)
                }
                ComposeResult.ok
            }
        }
    }

    fun fullScreenCover(isPresented: Binding<Boolean>, onDismiss: (() -> Unit)? = null, content: () -> View): View {
        return ModifiedContent(content = this, modifier = PresentationModifier(providesNavigation = false) { context -> SheetPresentation(isPresented = isPresented, isFullScreen = true, context = context, content = content, onDismiss = onDismiss) })
    }

    fun fullScreenCover(getIsPresented: () -> Boolean, setIsPresented: (Boolean) -> Unit, onDismiss: (() -> Unit)?, bridgedContent: View): View {
        val isPresented = Binding(get = getIsPresented, set = setIsPresented)
        return fullScreenCover(isPresented = isPresented, onDismiss = onDismiss, content = { ->
            ComposeBuilder { composectx: ComposeContext ->
                bridgedContent.Compose(composectx)
                ComposeResult.ok
            }
        })
    }

    fun interactiveDismissDisabled(isDisabled: Boolean = true): View = preference(key = InteractiveDismissDisabledPreferenceKey::class, value = isDisabled)

    fun backDismissDisabled(isDisabled: Boolean = true): View = ModifiedContent(content = this, modifier = BackDismissDisabledModifier(disabled = isDisabled))

    fun presentationDetents(detents: Set<PresentationDetent>): View {
        // TODO: Add support for multiple detents
        if (detents.count == 0) {
            return this.sref()
        }
        val selectedDetent = detents.first
        return preference(key = PresentationDetentPreferences::class, value = PresentationDetentPreferences(detent = selectedDetent))
    }

    fun presentationDetents(bridgedDetents: Array<Int>, values: Array<Double>): View {
        var set: Set<PresentationDetent> = setOf()
        for (i in 0 until bridgedDetents.count) {
            PresentationDetent.forBridged(identifier = bridgedDetents[i], value = values[i])?.let { detent ->
                set.insert(detent)
            }
        }
        return presentationDetents(set)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun presentationDetents(detents: Set<PresentationDetent>, selection: Binding<PresentationDetent>): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun presentationDragIndicator(visibility: Visibility): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun presentationBackgroundInteraction(interaction: PresentationBackgroundInteraction): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun presentationCompactAdaptation(adaptation: PresentationAdaptation): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun presentationCompactAdaptation(horizontal: PresentationAdaptation, vertical: PresentationAdaptation): View {
        val horizontalAdaptation = horizontal
        val verticalAdaptation = vertical
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun presentationCornerRadius(cornerRadius: Double?): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun presentationContentInteraction(behavior: PresentationContentInteraction): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun presentationBackground(style: ShapeStyle): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun presentationBackground(alignment: Alignment = Alignment.center, content: () -> View): View = this.sref()

    fun <Item> sheet(item: Binding<Item?>, onDismiss: (() -> Unit)? = null, content: (Item) -> View): View {
        val isPresented = Binding<Boolean>(get = l@{ -> return@l item.wrappedValue != null }, set = { newValue ->
            if (!newValue) {
                item.wrappedValue = null
            }
        })

        return sheet(isPresented = isPresented, onDismiss = onDismiss) { ->
            ComposeBuilder { composectx: ComposeContext ->
                item.wrappedValue.sref()?.let { unwrappedItem ->
                    content(unwrappedItem).Compose(composectx)
                }
                ComposeResult.ok
            }
        }
    }

    fun sheet(isPresented: Binding<Boolean>, onDismiss: (() -> Unit)? = null, content: () -> View): View {
        return ModifiedContent(content = this, modifier = PresentationModifier(providesNavigation = false) { context -> SheetPresentation(isPresented = isPresented, isFullScreen = false, context = context, content = content, onDismiss = onDismiss) })
    }

    fun sheet(getIsPresented: () -> Boolean, setIsPresented: (Boolean) -> Unit, onDismiss: (() -> Unit)?, bridgedContent: View): View {
        val isPresented = Binding(get = getIsPresented, set = setIsPresented)
        return sheet(isPresented = isPresented, onDismiss = onDismiss, content = { ->
            ComposeBuilder { composectx: ComposeContext ->
                bridgedContent.Compose(composectx)
                ComposeResult.ok
            }
        })
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun safeAreaInset(edge: VerticalEdge, alignment: HorizontalAlignment = HorizontalAlignment.center, spacing: Double? = null, content: () -> View): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun safeAreaInset(edge: HorizontalEdge, alignment: VerticalAlignment = VerticalAlignment.center, spacing: Double? = null, content: () -> View): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun safeAreaPadding(insets: EdgeInsets): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun safeAreaPadding(edges: Edge.Set = Edge.Set.all, length: Double? = null): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun safeAreaPadding(length: Double): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun safeAreaBar(edge: VerticalEdge, alignment: HorizontalAlignment = HorizontalAlignment.center, spacing: Double? = null, content: () -> View): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun safeAreaBar(edge: HorizontalEdge, alignment: VerticalAlignment = VerticalAlignment.center, spacing: Double? = null, content: () -> View): View = this.sref()
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun defaultAppStorage(store: UserDefaults): View = this.sref()
    fun accessibilityIdentifier(identifier: String, isEnabled: Boolean = true): View {
        return ModifiedContent(content = this, modifier = RenderModifier(role = ModifierRole.accessibility) l@{ it ->
            if (isEnabled) {
                return@l it.modifier.semantics { -> testTagsAsResourceId = true }.testTag(identifier)
            } else {
                return@l it.modifier
            }
        })
    }

    fun accessibilityLabel(label: Text, isEnabled: Boolean = true): View {
        return ModifiedContent(content = this, modifier = RenderModifier(role = ModifierRole.accessibility) l@{ it ->
            if (isEnabled) {
                val description = label.localizedTextString()
                return@l it.modifier.semantics { -> contentDescription = description }
            } else {
                return@l it.modifier
            }
        })
    }

    fun accessibilityLabel(label: String, isEnabled: Boolean = true): View {
        return ModifiedContent(content = this, modifier = RenderModifier(role = ModifierRole.accessibility) l@{ it ->
            if (isEnabled) {
                return@l it.modifier.semantics { -> contentDescription = label }
            } else {
                return@l it.modifier
            }
        })
    }

    fun accessibilityLabel(key: LocalizedStringKey, isEnabled: Boolean = true): View = accessibilityLabel(Text(key), isEnabled = isEnabled)

    fun accessibilityLabel(resource: LocalizedStringResource, isEnabled: Boolean = true): View = accessibilityLabel(Text(resource), isEnabled = isEnabled)

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityCustomContent(key: AccessibilityCustomContentKey, value: Text?, importance: Any? = null): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityCustomContent(key: AccessibilityCustomContentKey, valueKey: LocalizedStringKey, importance: Any? = null): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityCustomContent(key: AccessibilityCustomContentKey, valueResource: LocalizedStringResource, importance: Any? = null): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityCustomContent(key: AccessibilityCustomContentKey, value: String, importance: Any? = null): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityCustomContent(label: Text, value: Text, importance: Any? = null): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityCustomContent(labelKey: LocalizedStringKey, value: Text, importance: Any? = null): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityCustomContent(labelResource: LocalizedStringResource, value: Text, importance: Any? = null): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityCustomContent(labelKey: LocalizedStringKey, valueKey: LocalizedStringKey, importance: Any? = null): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityCustomContent(labelResource: LocalizedStringResource, valueResource: LocalizedStringResource, importance: Any? = null): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityCustomContent(labelKey: LocalizedStringKey, value: String, importance: Any? = null): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityCustomContent(labelResource: LocalizedStringResource, value: String, importance: Any? = null): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityInputLabels(inputLabels: Any): View {
        // Accepts [Text], [LocalizedStringKey], or [String]
        return this.sref()
    }

    fun accessibilityHint(hint: Text): View {
        return ModifiedContent(content = this, modifier = RenderModifier(role = ModifierRole.accessibility) l@{ it ->
            val label = hint.localizedTextString()
            return@l it.modifier.semantics { -> onClick(label = label, action = null) }
        })
    }

    fun accessibilityHint(hintKey: LocalizedStringKey): View = accessibilityHint(Text(hintKey))

    fun accessibilityHint(hintResource: LocalizedStringResource): View = accessibilityHint(Text(hintResource))

    fun accessibilityHint(hint: String): View {
        return ModifiedContent(content = this, modifier = RenderModifier(role = ModifierRole.accessibility) l@{ it ->
            return@l it.modifier.semantics { -> onClick(label = hint, action = null) }
        })
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityAction(actionKind: AccessibilityActionKind = AccessibilityActionKind.default, handler: () -> Unit): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityAction(named: Text, handler: () -> Unit): View {
        val name = named
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityAction(action: () -> Unit, label: () -> View): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityActions(content: () -> View): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityAction(named: LocalizedStringKey, handler: () -> Unit): View {
        val nameKey = named
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityAction(named: LocalizedStringResource, handler: () -> Unit): View {
        val nameResource = named
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityAction(named: String, handler: () -> Unit): View {
        val name = named
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityRotor(label: Text, entries: () -> AccessibilityRotorContent): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityRotor(systemRotor: AccessibilitySystemRotor, entries: () -> AccessibilityRotorContent): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <EntryModel> accessibilityRotor(rotorLabel: Text, entries: Array<EntryModel>, entryLabel: Any): View where EntryModel: Identifiable<*> = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <EntryModel, ID> accessibilityRotor(rotorLabel: Text, entries: Array<EntryModel>, entryID: Any, entryLabel: Any): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <EntryModel> accessibilityRotor(systemRotor: AccessibilitySystemRotor, entries: Array<EntryModel>, entryLabel: Any): View where EntryModel: Identifiable<*> = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <EntryModel, ID> accessibilityRotor(systemRotor: AccessibilitySystemRotor, entries: Array<EntryModel>, entryID: Any, entryLabel: Any): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityRotor(label: Text, textRanges: Array<IntRange>): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityRotor(systemRotor: AccessibilitySystemRotor, textRanges: Array<IntRange>): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityRotor(labelKey: LocalizedStringKey, entries: () -> AccessibilityRotorContent): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityRotor(labelResource: LocalizedStringResource, entries: () -> AccessibilityRotorContent): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityRotor(label: String, entries: () -> AccessibilityRotorContent): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <EntryModel> accessibilityRotor(rotorLabelKey: LocalizedStringKey, entries: Array<EntryModel>, entryLabel: Any): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <EntryModel> accessibilityRotor(rotorLabelResource: LocalizedStringResource, entries: Array<EntryModel>, entryLabel: Any): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <EntryModel> accessibilityRotor(rotorLabel: String, entries: Array<EntryModel>, entryLabel: Any): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <EntryModel, ID> accessibilityRotor(rotorLabelKey: LocalizedStringKey, entries: Array<EntryModel>, entryID: Any, entryLabel: Any): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <EntryModel, ID> accessibilityRotor(rotorLabelResource: LocalizedStringResource, entries: Array<EntryModel>, entryID: Any, entryLabel: Any): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <EntryModel, ID> accessibilityRotor(rotorLabel: String, entries: Array<EntryModel>, entryID: Any, entryLabel: Any): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityRotor(labelKey: LocalizedStringKey, textRanges: Array<IntRange>): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityRotor(labelResource: LocalizedStringResource, textRanges: Array<IntRange>): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityRotor(label: String, textRanges: Array<IntRange>): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityIgnoresInvertColors(active: Boolean = true): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityZoomAction(handler: (AccessibilityZoomGestureAction) -> Unit): View = this.sref()

    fun accessibilityHidden(hidden: Boolean, isEnabled: Boolean = true): View {
        return ModifiedContent(content = this, modifier = RenderModifier(role = ModifierRole.accessibility) l@{ it ->
            if (isEnabled) {
                return@l it.modifier.semantics { ->
                    if (hidden) {
                        invisibleToUser()
                    }
                }
            } else {
                return@l it.modifier
            }
        })
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityDirectTouch(isDirectTouchArea: Boolean = true, options: AccessibilityDirectTouchOptions = AccessibilityDirectTouchOptions.of()): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityRotorEntry(id: AnyHashable, in_: Namespace.ID): View {
        val namespace = in_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityChartDescriptor(representable: Any): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <Value> accessibilityFocused(binding: Any, equals: Value): View {
        val value = equals
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityFocused(condition: Any): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityRespondsToUserInteraction(respondsToUserInteraction: Boolean = true): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityActivationPoint(activationPoint: CGPoint): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityActivationPoint(activationPoint: UnitPoint): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilitySortPriority(sortPriority: Double): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <V> accessibilityShowsLargeContentViewer(largeContentView: () -> View): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityShowsLargeContentViewer(): View = this.sref()

    fun accessibilityAddTraits(traits: AccessibilityTraits): View {
        return ModifiedContent(content = this, modifier = RenderModifier(role = ModifierRole.accessibility) l@{ it ->
            var modifier = it.modifier
            if (traits.contains(AccessibilityTraits.isButton)) {
                modifier = modifier.semantics { -> role = androidx.compose.ui.semantics.Role.Button.sref() }
            }
            if (traits.contains(AccessibilityTraits.isHeader)) {
                modifier = modifier.semantics { -> heading() }
            }
            if (traits.contains(AccessibilityTraits.isSelected)) {
                modifier = modifier.semantics { -> selected = true }
            }
            if (traits.contains(AccessibilityTraits.isImage)) {
                modifier = modifier.semantics { -> role = androidx.compose.ui.semantics.Role.Image.sref() }
            }
            if (traits.contains(AccessibilityTraits.isModal)) {
                modifier = modifier.semantics { -> popup() }
            }
            if (traits.contains(AccessibilityTraits.isToggle)) {
                modifier = modifier.semantics { -> role = androidx.compose.ui.semantics.Role.Switch.sref() }
            }
            return@l modifier
        })
    }

    fun accessibilityAddTraits(bridgedTraits: Int): View = accessibilityAddTraits(AccessibilityTraits(rawValue = bridgedTraits))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityRemoveTraits(traits: AccessibilityTraits): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityLinkedGroup(id: AnyHashable, in_: Namespace.ID): View {
        val namespace = in_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityLabeledPair(role: AccessibilityLabeledPairRole, id: AnyHashable, in_: Namespace.ID): View {
        val namespace = in_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityRepresentation(representation: () -> View): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityChildren(children: () -> View): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityTextContentType(value: AccessibilityTextContentType): View = this.sref()

    fun accessibilityHeading(level: AccessibilityHeadingLevel): View {
        return ModifiedContent(content = this, modifier = RenderModifier(role = ModifierRole.accessibility) l@{ it ->
            return@l it.modifier.semantics { -> heading() }
        })
    }

    fun accessibilityHeading(bridgedLevel: Int): View = accessibilityHeading(AccessibilityHeadingLevel(rawValue = bridgedLevel) ?: AccessibilityHeadingLevel.unspecified)

    fun accessibilityValue(value: Text, isEnabled: Boolean = true): View {
        return ModifiedContent(content = this, modifier = RenderModifier(role = ModifierRole.accessibility) l@{ it ->
            if (isEnabled) {
                val description = value.localizedTextString()
                return@l it.modifier.semantics { -> stateDescription = description }
            } else {
                return@l it.modifier
            }
        })
    }

    fun accessibilityValue(value: String): View {
        return ModifiedContent(content = this, modifier = RenderModifier(role = ModifierRole.accessibility) l@{ it ->
            return@l it.modifier.semantics { -> stateDescription = value }
        })
    }

    fun accessibilityValue(key: LocalizedStringKey): View = accessibilityValue(Text(key))

    fun accessibilityValue(resource: LocalizedStringResource): View = accessibilityValue(Text(resource))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityScrollAction(handler: (Edge) -> Unit): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityElement(children: AccessibilityChildBehavior = AccessibilityChildBehavior.ignore): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun accessibilityAdjustableAction(handler: (AccessibilityAdjustmentDirection) -> Unit): View = this.sref()
    fun <Value> focused(binding: Binding<Value>, equals: Value): View {
        val value = equals
        return ModifiedContent(content = this, modifier = RenderModifier { renderable, context ->
            val focusRequester = remember { -> FocusRequester() }
            var context = context.sref()
            context.modifier = context.modifier
                .focusRequester(focusRequester)
                .onFocusChanged { it ->
                    if (it.hasFocus) {
                        binding.wrappedValue = value
                    } else if (binding.wrappedValue == value) {
                        if (value == true || value == false) {
                            binding.wrappedValue = false as Value
                        } else {
                            binding.wrappedValue = null as Value
                        }
                    }
                }
            if (value == binding.wrappedValue) {
                SideEffect { -> focusRequester.requestFocus() }
            }
            renderable.Render(context = context)
        })
    }

    fun focusedHashable(getValue: () -> Any?, setValue: (Any?) -> Unit, equals: Any?): View {
        val value = equals
        return focused(Binding(get = getValue, set = setValue), equals = value)
    }

    fun focusedBool(getValue: () -> Boolean, setValue: (Boolean) -> Unit, equals: Boolean): View {
        val value = equals
        return focused(Binding(get = getValue, set = setValue), equals = value)
    }

    fun focused(condition: Binding<Boolean>): View = focused(condition, equals = true)
    fun <V> gesture(gesture: Gesture<V>, including: GestureMask = GestureMask.all): View {
        val mask = including
        // We only support a mask of `.all` or `.none`
        return this.gesture(gesture, isEnabled = !mask.isEmpty)
    }

    fun <V> gesture(gesture: Gesture<V>, isEnabled: Boolean): View = ModifiedContent(content = this, modifier = GestureModifier(gesture = gesture as Gesture<Any>, isEnabled = isEnabled))

    fun bridgedGesture(gesture: Any, isEnabled: Boolean): View = ModifiedContent(content = this, modifier = GestureModifier(gesture = gesture as Gesture<Any>, isEnabled = isEnabled))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <V> highPriorityGesture(gesture: Gesture<V>, including: GestureMask = GestureMask.all): View {
        val mask = including
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <V> highPriorityGesture(gesture: Gesture<V>, isEnabled: Boolean): View = this.sref()

    fun onLongPressGesture(minimumDuration: Double = 0.5, maximumDistance: Double = Double(10.0), perform: () -> Unit): View {
        val action = perform
        val longPressGesture = LongPressGesture(minimumDuration = minimumDuration, maximumDistance = maximumDistance)
        return gesture(longPressGesture.onEnded({ _ -> action() }))
    }

    fun onLongPressGesture(minimumDuration: Double = 0.5, maximumDistance: Double = Double(10.0), perform: () -> Unit, onPressingChanged: (Boolean) -> Unit): View {
        val action = perform
        val longPressGesture = LongPressGesture(minimumDuration = minimumDuration, maximumDistance = maximumDistance)
        return gesture(longPressGesture.onChanged(onPressingChanged).onEnded({ _ -> action() }))
    }

    fun onTapGesture(count: Int = 1, coordinateSpace: CoordinateSpaceProtocol = LocalCoordinateSpace.local, perform: (CGPoint) -> Unit): View {
        val action = perform
        val tapGesture = TapGesture(count = count, coordinateSpace = coordinateSpace)
        var modified = tapGesture.modified.sref()
        modified.onEndedWithLocation = action
        return gesture(modified)
    }

    fun onTapGesture(count: Int, bridgedCoordinateSpace: Int, name: Any?, perform: (Double, Double) -> Unit): View {
        val action = perform
        val coordinateSpace = CoordinateSpaceProtocolFrom(bridged = bridgedCoordinateSpace, name = name as? AnyHashable)
        return onTapGesture(count = count, coordinateSpace = coordinateSpace, perform = { p -> action(p.x, p.y) })
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <V> simultaneousGesture(gesture: Gesture<V>, including: GestureMask = GestureMask.all): View {
        val mask = including
        return this.sref()
    }
    fun <T> sensoryFeedback(feedback: SensoryFeedback, trigger: T): View {
        return onChange(of = trigger) { -> feedback.activate() }
    }

    fun sensoryFeedback(bridgedFeedback: Int, trigger: Any?): View {
        val feedback = SensoryFeedback(rawValue = bridgedFeedback)
        return this.sensoryFeedback(feedback, trigger = trigger)
    }

    fun <T> sensoryFeedback(feedback: SensoryFeedback, trigger: T, condition: (T, T) -> Boolean): View {
        return onChange(of = trigger) { oldValue, newValue ->
            if (condition(oldValue, newValue)) {
                feedback.activate()
            }
        }
    }

    fun <T> sensoryFeedback(trigger: T, feedback: (T, T) -> SensoryFeedback?): View {
        return onChange(of = trigger) { oldValue, newValue ->
            feedback(oldValue, newValue)?.activate()
        }
    }

    fun sensoryFeedbackClosure(trigger: Any?, bridgedFeedback: (Any?, Any?) -> Int?): View {
        return this.sensoryFeedback(trigger = trigger) l@{ oldValue, newValue ->
            val matchtarget_1 = bridgedFeedback(oldValue, newValue)
            if (matchtarget_1 != null) {
                val feedback = matchtarget_1
                return@l SensoryFeedback(rawValue = feedback)
            } else {
                return@l null
            }
        }
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun userActivity(activityType: String, isActive: Boolean = true, update: (Any) -> Unit): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <P> userActivity(activityType: String, element: P?, update: (P, Any) -> Unit): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun onContinueUserActivity(activityType: String, perform: (Any) -> Unit): View {
        val action = perform
        return this.sref()
    }

    fun onOpenURL(perform: (URL) -> Unit): View {
        val action = perform
        return ModifiedContent(content = this, modifier = SideEffectModifier l@{ context ->
            val activity_0 = (LocalContext.current as? ComponentActivity).sref()
            if (activity_0 == null) {
                return@l ComposeResult.ok
            }
            val newIntent = remember { -> mutableStateOf<Intent?>(null) }
            val listener = remember l@{ ->
                val listener = OnNewIntentListener(newIntent)
                activity_0.addOnNewIntentListener(listener)
                return@l listener
            }
            DisposableEffect(true) { ->
                onDispose { -> activity_0.removeOnNewIntentListener(listener) }
            }
            val intent_0 = (newIntent.value ?: activity_0.intent).sref()
            if (intent_0 == null) {
                return@l ComposeResult.ok
            }
            newIntent.value = null
            if (intent_0.action != Intent.ACTION_VIEW) {
                return@l ComposeResult.ok
            }
            val dataString_0 = intent_0.dataString.sref()
            if (dataString_0 == null) {
                return@l ComposeResult.ok
            }
            val url_0 = (try { URL(string = dataString_0) } catch (_: NullReturnException) { null })
            if (url_0 == null) {
                return@l ComposeResult.ok
            }
            SideEffect { ->
                action(url_0)
                // Clear the intent so that we don't process it on recompose. We also considered remembering the last
                // processed intent in each `onOpenURL`, but then navigation to a new `onOpenURL` modifier would process
                // any existing intent as new because it wouldn't be remembered for that modifier
                activity_0.intent = null
            }
            return@l ComposeResult.ok
        })
    }

    fun onOpenURLString(perform: (String) -> Unit): View {
        val action = perform
        return onOpenURL(perform = { it -> action(it.absoluteString) })
    }

    fun onOpenURL(prefersInApp: Boolean): View = this.sref()
    fun labelStyle(style: LabelStyle): View {
        return environment({ it -> EnvironmentValues.shared.set_labelStyle(it) }, style, affectsEvaluate = false)
    }

    fun labelStyle(bridgedStyle: Int): View = labelStyle(LabelStyle(rawValue = bridgedStyle))

    fun labeledContentStyle(style: LabeledContentStyle): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun labelReservedIconWidth(value: Double): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun labelIconToTitleSpacing(value: Double): View = this.sref()
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun allowsTightening(flag: Boolean): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun baselineOffset(baselineOffset: Double): View = this.sref()

    fun bold(isActive: Boolean = true): View = fontWeight(if (isActive) Font.Weight.bold else null)

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dynamicTypeSize(size: DynamicTypeSize): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dynamicTypeSize(range: IntRange): View = this.sref()

    fun font(font: Font?): View {
        return environment({ it -> EnvironmentValues.shared.setfont(it) }, font)
    }

    fun fontDesign(design: Font.Design?): View {
        return textEnvironment(for_ = this) { it -> it.value.fontDesign = design }
    }

    fun fontDesign(bridgedDesign: Int?): View {
        val design = if (bridgedDesign == null) null else Font.Design(rawValue = bridgedDesign!!)
        return fontDesign(design)
    }

    fun fontWeight(weight: Font.Weight?): View {
        return textEnvironment(for_ = this) { it -> it.value.fontWeight = weight }
    }

    fun fontWeight(bridgedWeight: Int?): View {
        val weight = if (bridgedWeight == null) null else Font.Weight(value = bridgedWeight!!)
        return fontWeight(weight)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun fontWidth(width: Font.Width?): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun invalidatableContent(invalidatable: Boolean = true): View = this.sref()

    fun italic(isActive: Boolean = true): View {
        return textEnvironment(for_ = this) { it -> it.value.isItalic = isActive }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun kerning(kerning: Double): View = this.sref()

    fun lineLimit(number: Int?): View {
        return environment({ it -> EnvironmentValues.shared.setlineLimit(it) }, number)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun lineLimit(limit: IntRange): View = this.sref()

    fun lineLimit(limit: Int, reservesSpace: Boolean): View {
        return environment({ it -> EnvironmentValues.shared.setlineLimit(it) }, limit).environment({ it -> EnvironmentValues.shared.set_lineLimitReservesSpace(it) }, reservesSpace, affectsEvaluate = false)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun lineHeight(lineHeight: Any?): View = this.sref()

    fun lineSpacing(lineSpacing: Double): View {
        return textEnvironment(for_ = this) { it -> it.value.lineSpacing = lineSpacing }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun monospacedDigit(): View = this.sref()

    fun monospaced(isActive: Boolean = true): View = fontDesign(if (isActive) Font.Design.monospaced else null)

    fun minimumScaleFactor(factor: Double): View {
        return textEnvironment(for_ = this) { it -> it.value.minimumScaleFactor = factor }
    }

    fun multilineTextAlignment(alignment: TextAlignment): View {
        return environment({ it -> EnvironmentValues.shared.setmultilineTextAlignment(it) }, alignment)
    }

    fun multilineTextAlignment(strategy: Text.AlignmentStrategy): View = this.sref()

    fun multilineTextAlignment(bridgedAlignment: Int): View = multilineTextAlignment(TextAlignment(rawValue = bridgedAlignment) ?: TextAlignment.center)

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun privacySensitive(sensitive: Boolean = true): View = this.sref()

    fun redacted(reason: RedactionReasons): View {
        return environment({ it -> EnvironmentValues.shared.setredactionReasons(it) }, reason)
    }

    fun redacted(bridgedReason: Int): View = redacted(reason = RedactionReasons(rawValue = bridgedReason))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun speechAlwaysIncludesPunctuation(value: Boolean = true): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun speechSpellsOutCharacters(value: Boolean = true): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun speechAdjustedPitch(value: Double): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun speechAnnouncementsQueued(value: Boolean = true): View = this.sref()

    fun strikethrough(isActive: Boolean = true, pattern: Text.LineStyle.Pattern = Text.LineStyle.Pattern.solid, color: Color? = null): View {
        return textEnvironment(for_ = this) { it -> it.value.isStrikethrough = isActive }
    }

    fun bridgedStrikethrough(isActive: Boolean): View = this.strikethrough(isActive)

    fun textCase(textCase: Text.Case?): View {
        return textEnvironment(for_ = this) { it -> it.value.textCase = textCase }
    }

    fun bridgedTextCase(textCase: Int?): View = this.textCase(if (textCase == null) null else Text.Case(rawValue = textCase!!))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun textScale(scale: Text.Scale, isEnabled: Boolean = true): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun textSelection(selectability: TextSelectability): View = this.sref()

    fun tracking(tracking: Double): View {
        return textEnvironment(for_ = this) { it -> it.value.tracking = tracking }
    }

    fun truncationMode(mode: Text.TruncationMode): View {
        return environment({ it -> EnvironmentValues.shared.settruncationMode(it) }, mode, affectsEvaluate = false)
    }

    fun underline(isActive: Boolean = true, pattern: Text.LineStyle.Pattern = Text.LineStyle.Pattern.solid, color: Color? = null): View {
        return textEnvironment(for_ = this) { it -> it.value.isUnderline = isActive }
    }

    fun bridgedUnderline(isActive: Boolean): View = underline(isActive)

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun unredacted(): View = this.sref()

    fun writingDirection(strategy: Text.WritingDirectionStrategy): View = this.sref()

    /// Compose text field customization.
    fun material3Text(options: @Composable (Material3TextOptions) -> Material3TextOptions): View {
        return environment({ it -> EnvironmentValues.shared.set_material3Text(it) }, options, affectsEvaluate = false)
    }
    fun textEditorStyle(style: TextEditorStyle): View = this.sref()

    fun textEditorStyle(bridgedStyle: Int): View = textEditorStyle(TextEditorStyle(rawValue = bridgedStyle))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun findNavigator(isPresented: Binding<Boolean>): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun findDisabled(isDisabled: Boolean = true): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun replaceDisabled(isDisabled: Boolean = true): View = this.sref()
    fun autocorrectionDisabled(disable: Boolean = true): View {
        return keyboardOptionsModifierView l@{ options -> return@l if (options == null) KeyboardOptions(autoCorrectEnabled = !disable) else options.copy(autoCorrectEnabled = !disable) }
    }

    fun keyboardType(type: UIKeyboardType): View {
        val keyboardType = type.asComposeKeyboardType()
        return keyboardOptionsModifierView l@{ options -> return@l if (options == null) KeyboardOptions(keyboardType = keyboardType) else options.copy(keyboardType = keyboardType) }
    }

    fun keyboardType(bridgedType: Int): View = keyboardType(UIKeyboardType(rawValue = bridgedType) ?: UIKeyboardType.default)

    fun onSubmit(of: SubmitTriggers = SubmitTriggers.text, action: () -> Unit): View {
        val triggers = of
        return ModifiedContent(content = this, modifier = EnvironmentModifier(affectsEvaluate = false) l@{ environment ->
            val state = environment._onSubmitState
            val updatedState = if (state == null) OnSubmitState(triggers = triggers, action = action) else state!!.appending(triggers = triggers, action = action)
            environment.set_onSubmitState(updatedState)
            return@l ComposeResult.ok
        })
    }

    fun onSubmit(bridgedTriggers: Int, action: () -> Unit): View = onSubmit(of = SubmitTriggers(rawValue = bridgedTriggers), action)

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun submitScope(isBlocking: Boolean = true): View = this.sref()

    fun submitLabel(submitLabel: SubmitLabel): View {
        val imeAction = submitLabel.asImeAction()
        return keyboardOptionsModifierView l@{ options -> return@l if (options == null) KeyboardOptions(imeAction = imeAction) else options.copy(imeAction = imeAction) }
    }

    fun submitLabel(bridgedLabel: Int): View = submitLabel(SubmitLabel(rawValue = bridgedLabel) ?: SubmitLabel.done)

    fun textContentType(textContentType: UITextContentType?): View {
        val ctype_0 = textContentType?._contentType.sref()
        if (ctype_0 == null) {
            return this.sref()
        }
        return ModifiedContent(content = this, modifier = RenderModifier l@{ it ->
            return@l it.modifier.semantics { -> contentType = ctype_0.sref() }
        })
    }

    fun textContentType(bridgedContentType: Int): View = textContentType(UITextContentType(rawValue = bridgedContentType))

    fun textFieldStyle(style: TextFieldStyle): View {
        return environment({ it -> EnvironmentValues.shared.set_textFieldStyle(it) }, style, affectsEvaluate = false)
    }

    fun textFieldStyle(bridgedStyle: Int): View = textFieldStyle(TextFieldStyle(rawValue = bridgedStyle))

    fun textInputAutocapitalization(autocapitalization: TextInputAutocapitalization?): View {
        val capitalization = (autocapitalization ?: TextInputAutocapitalization.sentences).asKeyboardCapitalization()
        return keyboardOptionsModifierView l@{ options -> return@l if (options == null) KeyboardOptions(capitalization = capitalization) else options.copy(capitalization = capitalization) }
    }

    fun textInputAutocapitalization(bridgedAutocapitalization: Int?): View {
        val autocap: TextInputAutocapitalization? = if (bridgedAutocapitalization == null) null else TextInputAutocapitalization(rawValue = bridgedAutocapitalization!!)
        return textInputAutocapitalization(autocap)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun textInputFormattingControlVisibility(visibility: Visibility, for_: TextInputFormattingControlPlacement.Set): View {
        val placement = for_
        return this.sref()
    }

    /// Return a modifier view that updates the environment's keyboard options.
    fun keyboardOptionsModifierView(update: (KeyboardOptions?) -> KeyboardOptions): View {
        return ModifiedContent(content = this, modifier = EnvironmentModifier l@{ environment ->
            val options = environment._keyboardOptions.sref()
            val updatedOptions = update(options)
            environment.set_keyboardOptions(updatedOptions)
            return@l ComposeResult.ok
        })
    }

    /// Compose text field customization.
    fun material3TextField(options: @Composable (Material3TextFieldOptions) -> Material3TextFieldOptions): View {
        return environment({ it -> EnvironmentValues.shared.set_material3TextField(it) }, options, affectsEvaluate = false)
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun allowsHitTesting(enabled: Boolean): View = this.sref()

    fun aspectRatio(ratio: Double? = null, contentMode: ContentMode): View = ModifiedContent(content = this, modifier = AspectRatioModifier(ratio = ratio, contentMode = contentMode))

    fun aspectRatio(size: CGSize, contentMode: ContentMode): View = aspectRatio(size.width / size.height, contentMode = contentMode)

    fun aspectRatio(ratio: Double? = null, bridgedContentMode: Int): View = aspectRatio(ratio, contentMode = ContentMode(rawValue = bridgedContentMode) ?: ContentMode.fit)

    fun background(background: View, alignment: Alignment = Alignment.center): View {
        return ModifiedContent(content = this, modifier = RenderModifier { renderable, context -> BackgroundLayout(content = renderable, context = context, background = background, alignment = alignment) })
    }

    fun background(alignment: Alignment = Alignment.center, content: () -> View): View = background(content(), alignment = alignment)

    fun background(horizontalAlignmentKey: String, verticalAlignmentKey: String, bridgedContent: View): View {
        return background(alignment = Alignment(horizontal = HorizontalAlignment(key = horizontalAlignmentKey), vertical = VerticalAlignment(key = verticalAlignmentKey)), content = { ->
            ComposeBuilder { composectx: ComposeContext ->
                bridgedContent.Compose(composectx)
                ComposeResult.ok
            }
        })
    }

    /// - Warning: The second argument here should default to `.all`. Our implementation is not yet sophisticated enough to auto-detect when it is
    ///     against a safe area boundary, so this would cause problems. Therefore we default to `[]` and rely on ther user to specify the edges.
    fun background(ignoresSafeAreaEdges: Edge.Set = Edge.Set.of()): View {
        val edges = ignoresSafeAreaEdges
        return this.background(BackgroundStyle.shared, ignoresSafeAreaEdges = edges)
    }

    /// - Warning: The second argument here should default to `.all`. Our implementation is not yet sophisticated enough to auto-detect when it is
    ///     against a safe area boundary, so this would cause problems. Therefore we default to `[]` and rely on ther user to specify the edges.
    fun background(style: ShapeStyle, ignoresSafeAreaEdges: Edge.Set = Edge.Set.of()): View {
        val edges = ignoresSafeAreaEdges
        if (edges.isEmpty) {
            return ModifiedContent(content = this, modifier = RenderModifier l@{ context ->
                val matchtarget_2 = style.asColor(opacity = 1.0, animationContext = context)
                if (matchtarget_2 != null) {
                    val color = matchtarget_2
                    return@l context.modifier.background(color)
                } else {
                    val matchtarget_3 = style.asBrush(opacity = 1.0, animationContext = context)
                    if (matchtarget_3 != null) {
                        val brush = matchtarget_3
                        return@l context.modifier.background(brush)
                    } else {
                        return@l context.modifier
                    }
                }
            })
        } else {
            return background { ->
                ComposeBuilder { composectx: ComposeContext ->
                    style.ignoresSafeArea(edges = edges).Compose(composectx)
                    ComposeResult.ok
                }
            }
        }
    }

    fun background(style: ShapeStyle, bridgedIgnoresSafeAreaEdges: Int): View = background(style, ignoresSafeAreaEdges = Edge.Set(rawValue = bridgedIgnoresSafeAreaEdges))

    fun background(in_: Shape, fillStyle: FillStyle = FillStyle()): View {
        val shape = in_
        return background(BackgroundStyle.shared, in_ = shape, fillStyle = fillStyle)
    }

    fun background(style: ShapeStyle, in_: Shape, fillStyle: FillStyle = FillStyle()): View {
        val shape = in_
        return background(content = { ->
            ComposeBuilder { composectx: ComposeContext ->
                shape.fill(style).Compose(composectx)
                ComposeResult.ok
            }
        })
    }

    fun background(style: ShapeStyle, in_: Shape, eoFill: Boolean, antialiased: Boolean): View {
        val shape = in_
        return background(style, in_ = shape, fillStyle = FillStyle(eoFill = eoFill, antialiased = antialiased))
    }

    fun backgroundStyle(style: ShapeStyle): View {
        return environment({ it -> EnvironmentValues.shared.setbackgroundStyle(it) }, style)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun badge(count: Int): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun badge(label: Text?): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun badge(key: LocalizedStringKey): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun badge(resource: LocalizedStringResource): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun badge(label: String): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun badgeProminence(prominence: BadgeProminence): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun blendMode(blendMode: BlendMode): View = this.sref()

    fun blur(radius: Double, opaque: Boolean = false): View {
        return ModifiedContent(content = this, modifier = RenderModifier l@{ it -> return@l it.modifier.blur(radiusX = radius.dp, radiusY = radius.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded) })
    }

    fun border(style: ShapeStyle, width: Double = 1.0): View {
        return ModifiedContent(content = this, modifier = RenderModifier l@{ context ->
            val matchtarget_4 = style.asColor(opacity = 1.0, animationContext = context)
            if (matchtarget_4 != null) {
                val color = matchtarget_4
                return@l context.modifier.border(width = width.dp, color = color)
            } else {
                val matchtarget_5 = style.asBrush(opacity = 1.0, animationContext = context)
                if (matchtarget_5 != null) {
                    val brush = matchtarget_5
                    return@l context.modifier.border(BorderStroke(width = width.dp, brush = brush))
                } else {
                    return@l context.modifier
                }
            }
        })
    }

    fun brightness(amount: Double): View {
        return ModifiedContent(content = this, modifier = RenderModifier l@{ it -> return@l it.modifier.then(BrightnessModifier(amount = amount)) })
    }

    fun clipShape(shape: Shape, style: FillStyle = FillStyle()): View {
        return ModifiedContent(content = this, modifier = RenderModifier l@{ it -> return@l it.modifier.clip(shape.asComposeShape(density = LocalDensity.current)) })
    }

    fun clipShape(shape: Shape, eoFill: Boolean, antialiased: Boolean): View = clipShape(shape, style = FillStyle(eoFill = eoFill, antialiased = antialiased))

    fun clipped(antialiased: Boolean = false): View {
        return ModifiedContent(content = this, modifier = RenderModifier l@{ it -> return@l it.modifier.clipToBounds() })
    }

    fun colorInvert(): View {
        return ModifiedContent(content = this, modifier = RenderModifier l@{ it -> return@l it.modifier.then(ColorInvertModifier()) })
    }

    fun colorMultiply(color: Color): View {
        return ModifiedContent(content = this, modifier = RenderModifier l@{ context -> return@l context.modifier.then(ColorMultiplyModifier(color = color.colorImpl())) })
    }

    fun compositingGroup(): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun containerBackground(style: ShapeStyle, for_: ContainerBackgroundPlacement): View {
        val container = for_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun containerBackground(for_: ContainerBackgroundPlacement, alignment: Alignment = Alignment.center, content: () -> View): View {
        val container = for_
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun containerRelativeFrame(axes: Axis.Set, alignment: Alignment = Alignment.center): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun containerRelativeFrame(axes: Axis.Set, count: Int, span: Int = 1, spacing: Double, alignment: Alignment = Alignment.center): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun containerRelativeFrame(axes: Axis.Set, alignment: Alignment = Alignment.center, length: (Double, Axis) -> Double): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <T> containerShape(shape: Shape): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun contentShape(shape: Shape, eoFill: Boolean = false): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun contentShape(kind: ContentShapeKinds, shape: Shape, eoFill: Boolean = false): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun contextMenu(menuItems: () -> View): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun contextMenu(menuItems: () -> View, preview: () -> View): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <I> contextMenu(forSelectionType: KClass<*>? = null, menu: (Set<I>) -> View, primaryAction: ((Set<I>) -> Unit)? = null): View {
        val itemType = forSelectionType
        return this.sref()
    }

    fun contrast(amount: Double): View {
        return ModifiedContent(content = this, modifier = RenderModifier l@{ it -> return@l it.modifier.then(ContrastModifier(amount = amount)) })
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun controlSize(controlSize: ControlSize): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun coordinateSpace(name: NamedCoordinateSpace): View = this.sref()

    // No need to @bridge because we define it in terms of `clipShape`
    fun cornerRadius(radius: Double, antialiased: Boolean = true): View = clipShape(RoundedRectangle(cornerRadius = radius))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun defaultHoverEffect(effect: HoverEffect?): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun defersSystemGestures(on: Edge.Set): View {
        val edges = on
        return this.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dialogSuppressionToggle(titleKey: LocalizedStringKey, isSuppressed: Binding<Boolean>): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dialogSuppressionToggle(titleResource: LocalizedStringResource, isSuppressed: Binding<Boolean>): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dialogSuppression(title: String, isSuppressed: Binding<Boolean>): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dialogSuppressionToggle(label: Text, isSuppressed: Binding<Boolean>): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dialogSuppressionToggle(isSuppressed: Binding<Boolean>): View = this.sref()

    fun disabled(disabled: Boolean): View = ModifiedContent(content = this, modifier = DisabledModifier(disabled))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun drawingGroup(opaque: Boolean = false, colorMode: ColorRenderingMode = ColorRenderingMode.nonLinear): View = this.sref()

    // No need to @bridge
    fun equatable(): View = EquatableView(content = this)

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun fileMover(isPresented: Binding<Boolean>, file: URL?, onCompletion: (Result<URL, Error>) -> Unit): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun fileMover(isPresented: Binding<Boolean>, files: Collection<URL>, onCompletion: (Result<Array<URL>, Error>) -> Unit): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun fileMover(isPresented: Binding<Boolean>, file: URL?, onCompletion: (Result<URL, Error>) -> Unit, onCancellation: () -> Unit): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun fileMover(isPresented: Binding<Boolean>, files: Collection<URL>, onCompletion: (Result<Array<URL>, Error>) -> Unit, onCancellation: () -> Unit): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun fixedSize(horizontal: Boolean, vertical: Boolean): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun fixedSize(): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun flipsForRightToLeftLayoutDirection(enabled: Boolean): View = this.sref()

    fun foregroundColor(color: Color?): View {
        return environment({ it -> EnvironmentValues.shared.set_foregroundStyle(it) }, color, affectsEvaluate = false)
    }

    fun foregroundStyle(style: ShapeStyle): View {
        return environment({ it -> EnvironmentValues.shared.set_foregroundStyle(it) }, style, affectsEvaluate = false)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun foregroundStyle(primary: ShapeStyle, secondary: ShapeStyle): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun foregroundStyle(primary: ShapeStyle, secondary: ShapeStyle, tertiary: ShapeStyle): View = this.sref()

    fun frame(width: Double? = null, height: Double? = null, alignment: Alignment = Alignment.center): View {
        return ModifiedContent(content = this, modifier = RenderModifier { renderable, context ->
            val animatable = Tuple2(Float(width ?: 0.0), Float(height ?: 0.0)).asAnimatable(context = context)
            FrameLayout(content = renderable, context = context, width = if (width == null) null else Double(animatable.value.element0), height = if (height == null) null else Double(animatable.value.element1), alignment = alignment)
        })
    }

    fun frame(width: Double?, height: Double?, horizontalAlignmentKey: String, verticalAlignmentKey: String): View = frame(width = width, height = height, alignment = Alignment(horizontal = HorizontalAlignment(key = horizontalAlignmentKey), vertical = VerticalAlignment(key = verticalAlignmentKey)))

    fun frame(minWidth: Double? = null, idealWidth: Double? = null, maxWidth: Double? = null, minHeight: Double? = null, idealHeight: Double? = null, maxHeight: Double? = null, alignment: Alignment = Alignment.center): View {
        return ModifiedContent(content = this, modifier = RenderModifier { renderable, context -> FrameLayout(content = renderable, context = context, minWidth = minWidth, idealWidth = idealWidth, maxWidth = maxWidth, minHeight = minHeight, idealHeight = idealHeight, maxHeight = maxHeight, alignment = alignment) })
    }

    fun frame(minWidth: Double?, idealWidth: Double?, maxWidth: Double?, minHeight: Double?, idealHeight: Double?, maxHeight: Double?, horizontalAlignmentKey: String, verticalAlignmentKey: String): View = frame(minWidth = minWidth, idealWidth = idealWidth, maxWidth = maxWidth, minHeight = minHeight, idealHeight = idealHeight, maxHeight = maxHeight, alignment = Alignment(horizontal = HorizontalAlignment(key = horizontalAlignmentKey), vertical = VerticalAlignment(key = verticalAlignmentKey)))

    fun grayscale(amount: Double): View {
        return ModifiedContent(content = this, modifier = RenderModifier l@{ it -> return@l it.modifier.then(GrayscaleModifier(amount = amount)) })
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun handlesExternalEvents(preferring: Set<String>, allowing: Set<String>): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun headerProminence(prominence: Prominence): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun help(textKey: LocalizedStringKey): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun help(textResource: LocalizedStringResource): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun help(text: Text): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun help(text: String): View = this.sref()

    fun hidden(): View = opacity(0.0)

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun hoverEffect(effect: HoverEffect = HoverEffect.automatic, isEnabled: Boolean = true): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun hoverEffectDisabled(disabled: Boolean = true): View = this.sref()

    fun hueRotation(angle: Angle): View {
        return ModifiedContent(content = this, modifier = RenderModifier l@{ it -> return@l it.modifier.then(HueRotationModifier(degrees = angle.degrees)) })
    }

    fun hueRotation(bridgedAngle: Double): View = hueRotation(Angle.radians(bridgedAngle))

    fun id(id: Any): View = ModifiedContent(content = this, modifier = TagModifier(value = id, role = ModifierRole.id))

    fun ignoresSafeArea(regions: SafeAreaRegions = SafeAreaRegions.all, edges: Edge.Set = Edge.Set.all): View {
        if (!regions.contains(SafeAreaRegions.container)) {
            return this.sref()
        }
        return ModifiedContent(content = this, modifier = RenderModifier { renderable, context -> IgnoresSafeAreaLayout(content = renderable, context = context, expandInto = edges) })
    }

    fun ignoresSafeArea(bridgedRegions: Int, bridgedEdges: Int): View = ignoresSafeArea(SafeAreaRegions(rawValue = bridgedRegions), edges = Edge.Set(rawValue = bridgedEdges))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun inspector(isPresented: Binding<Boolean>, content: () -> View): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun inspectorColumnWidth(min: Double? = null, ideal: Double, max: Double? = null): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun inspectorColumnWidth(width: Double): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun interactionActivityTrackingTag(tag: String): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun keyboardShortcut(key: KeyEquivalent, modifiers: EventModifiers = EventModifiers.command): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun keyboardShortcut(shortcut: KeyboardShortcut?): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun keyboardShortcut(key: KeyEquivalent, modifiers: EventModifiers = EventModifiers.command, localization: KeyboardShortcut.Localization): View = this.sref()

    fun labelsHidden(): View {
        return environment({ it -> EnvironmentValues.shared.set_labelsHidden(it) }, true, affectsEvaluate = false)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun layoutDirectionBehavior(behavior: LayoutDirectionBehavior): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun layoutPriority(value: Double): View = this.sref()

    /// Allow users to revert to previous layout versions.
    fun layoutImplementationVersion(version: Int): View {
        return environment({ it -> EnvironmentValues.shared.set_layoutImplementationVersion(it) }, version)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun luminanceToAlpha(): View = this.sref()

    fun mask(mask: View, alignment: Alignment = Alignment.center): View {
        return ModifiedContent(content = this, modifier = RenderModifier { renderable, context -> MaskLayout(content = renderable, context = context, mask = mask, alignment = alignment) })
    }

    fun mask(alignment: Alignment = Alignment.center, mask: () -> View): View = this.mask(mask(), alignment = alignment)

    fun mask(horizontalAlignmentKey: String, verticalAlignmentKey: String, bridgedMask: View): View = mask(bridgedMask, alignment = Alignment(horizontal = HorizontalAlignment(key = horizontalAlignmentKey), vertical = VerticalAlignment(key = verticalAlignmentKey)))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <ID> matchedGeometryEffect(id: Hashable, in_: Any, properties: MatchedGeometryProperties = MatchedGeometryProperties.frame, anchor: UnitPoint = UnitPoint.center, isSource: Boolean = true): View {
        val namespace = in_
        return this.sref()
    }

    fun offset(offset: CGSize): View = this.offset(x = offset.width, y = offset.height)

    fun offset(x: Double = 0.0, y: Double = 0.0): View {
        return ModifiedContent(content = this, modifier = RenderModifier l@{ it ->
            val density = LocalDensity.current.sref()
            val animatable = Tuple2(Float(x), Float(y)).asAnimatable(context = it)
            val offsetPx = with(density) { -> IntOffset(animatable.value.element0.dp.roundToPx(), animatable.value.element1.dp.roundToPx()) }
            return@l it.modifier.offset { -> offsetPx }
        })
    }

    fun onAppear(perform: (() -> Unit)? = null): View {
        val action = perform
        // TODO: would it be better to use the (new) onFirstVisible and onVisibilityChanged APIs here?
        return ModifiedContent(content = this, modifier = SideEffectModifier l@{ _ ->
            val hasAppeared = remember { -> mutableStateOf(false) }
            if (!hasAppeared.value) {
                hasAppeared.value = true
                SideEffect { -> action?.invoke() }
            }
            return@l ComposeResult.ok
        })
    }

    fun <V> onChange(of: V, perform: (V) -> Unit): View {
        val value = of
        val action = perform
        return ModifiedContent(content = this, modifier = SideEffectModifier l@{ context ->
            val rememberedValue = rememberSaveable(stateSaver = context.stateSaver as Saver<V, Any>) { -> mutableStateOf(value) }
            if (rememberedValue.value != value) {
                rememberedValue.value = value
                SideEffect { -> action(value) }
            }
            return@l ComposeResult.ok
        })
    }

    // Note: Kotlin's type inference has issues when a no-label closure follows a defaulted argument and the closure is
    // inline rather than trailing at the call site. So for these onChange variants we've separated the 'initial' argument
    // out rather than default it

    fun <V> onChange(of: V, action: (V, V) -> Unit): View {
        val value = of
        return onChange(of = value, initial = false, action)
    }

    fun <V> onChange(of: V, initial: Boolean, action: (V, V) -> Unit): View {
        val value = of
        return ModifiedContent(content = this, modifier = SideEffectModifier l@{ context ->
            val rememberedValue = rememberSaveable(stateSaver = context.stateSaver as Saver<V, Any>) { -> mutableStateOf(value) }
            val rememberedInitial = rememberSaveable(stateSaver = context.stateSaver as Saver<Boolean, Any>) { -> mutableStateOf(true) }

            val isInitial = rememberedInitial.value.sref()
            rememberedInitial.value = false

            val oldValue = rememberedValue.value.sref()
            val isUpdate = oldValue != value
            if (isUpdate) {
                rememberedValue.value = value
            }

            if ((initial && isInitial) || isUpdate) {
                SideEffect { -> action(oldValue, value) }
            }
            return@l ComposeResult.ok
        })
    }

    fun <V> onChange(of: V?, action: () -> Unit): View {
        val value = of
        return onChange(of = value, initial = false, action)
    }

    fun <V> onChange(of: V?, initial: Boolean, action: () -> Unit): View {
        val value = of
        return ModifiedContent(content = this, modifier = SideEffectModifier l@{ context ->
            val rememberedValue = rememberSaveable(stateSaver = context.stateSaver as Saver<V?, Any>) { -> mutableStateOf(value) }
            val rememberedInitial = rememberSaveable(stateSaver = context.stateSaver as Saver<Boolean, Any>) { -> mutableStateOf(true) }

            val isInitial = rememberedInitial.value.sref()
            rememberedInitial.value = false

            val oldValue = rememberedValue.value.sref()
            val isUpdate = oldValue != value
            if (isUpdate) {
                rememberedValue.value = value
            }

            if ((initial && isInitial) || isUpdate) {
                SideEffect { -> action() }
            }
            return@l ComposeResult.ok
        })
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun onContinuousHover(coordinateSpace: CoordinateSpaceProtocol = LocalCoordinateSpace.local, perform: (HoverPhase) -> Unit): View {
        val action = perform
        return this.sref()
    }

    fun onDisappear(perform: (() -> Unit)? = null): View {
        val action = perform
        return ModifiedContent(content = this, modifier = SideEffectModifier l@{ _ ->
            val disposeAction = rememberUpdatedState(action)
            DisposableEffect(true) { ->
                onDispose { -> disposeAction.value?.invoke() }
            }
            return@l ComposeResult.ok
        })
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun onHover(perform: (Boolean) -> Unit): View {
        val action = perform
        return this.sref()
    }

    fun opacity(opacity: Double): View {
        return ModifiedContent(content = this, modifier = RenderModifier l@{ context ->
            val animatable = Float(opacity).asAnimatable(context = context)
            return@l context.modifier.graphicsLayer { -> alpha = animatable.value.sref() }
        })
    }

    fun overlay(alignment: Alignment = Alignment.center, content: () -> View): View {
        val overlay = content()
        return ModifiedContent(content = this, modifier = RenderModifier { renderable, context -> OverlayLayout(content = renderable, context = context, overlay = overlay, alignment = alignment) })
    }

    fun overlay(horizontalAlignmentKey: String, verticalAlignmentKey: String, bridgedContent: View): View {
        return overlay(alignment = Alignment(horizontal = HorizontalAlignment(key = horizontalAlignmentKey), vertical = VerticalAlignment(key = verticalAlignmentKey)), content = { ->
            ComposeBuilder { composectx: ComposeContext ->
                bridgedContent.Compose(composectx)
                ComposeResult.ok
            }
        })
    }

    fun overlay(style: ShapeStyle, ignoresSafeAreaEdges: Edge.Set = Edge.Set.all): View {
        val edges = ignoresSafeAreaEdges
        return overlay(style, in_ = Rectangle())
    }

    fun overlay(style: ShapeStyle, bridgedIgnoresSafeAreaEdges: Int): View = overlay(style, ignoresSafeAreaEdges = Edge.Set(rawValue = bridgedIgnoresSafeAreaEdges))

    fun overlay(style: ShapeStyle, in_: Shape, fillStyle: FillStyle = FillStyle()): View {
        val shape = in_
        return overlay(content = { ->
            ComposeBuilder { composectx: ComposeContext ->
                shape.fill(style).Compose(composectx)
                ComposeResult.ok
            }
        })
    }

    fun overlay(style: ShapeStyle, in_: Shape, eoFill: Boolean, antialiased: Boolean): View {
        val shape = in_
        return overlay(style, in_ = shape, fillStyle = FillStyle(eoFill = eoFill, antialiased = antialiased))
    }

    fun padding(insets: EdgeInsets): View = ModifiedContent(content = this, modifier = PaddingModifier(insets = insets))

    fun padding(edges: Edge.Set, length: Double? = null): View {
        var padding = EdgeInsets()
        if (edges.contains(Edge.Set.top)) {
            padding.top = length ?: 16.0
        }
        if (edges.contains(Edge.Set.bottom)) {
            padding.bottom = length ?: 16.0
        }
        if (edges.contains(Edge.Set.leading)) {
            padding.leading = length ?: 16.0
        }
        if (edges.contains(Edge.Set.trailing)) {
            padding.trailing = length ?: 16.0
        }
        return padding(padding)
    }

    fun padding(length: Double? = null): View = padding(Edge.Set.all, length)

    fun padding(top: Double, leading: Double, bottom: Double, trailing: Double): View = padding(EdgeInsets(top = top, leading = leading, bottom = bottom, trailing = trailing))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun persistentSystemOverlays(visibility: Visibility): View = this.sref()

    fun position(position: CGPoint): View = this.position(x = position.x, y = position.y)

    fun position(x: Double = 0.0, y: Double = 0.0): View {
        return ModifiedContent(content = this, modifier = RenderModifier { renderable, context ->
            val density = LocalDensity.current.sref()
            val animatable = Tuple2(Float(x), Float(y)).asAnimatable(context = context)
            val positionPx = with(density) { -> IntOffset(animatable.value.element0.dp.roundToPx(), animatable.value.element1.dp.roundToPx()) }
            PositionLayout(content = renderable, x = Double(animatable.value.element0), y = Double(animatable.value.element1), context = context)
        })
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun projectionEffect(transform: Any): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <Item> popover(item: Binding<Item?>, attachmentAnchor: Any? = null, arrowEdge: Edge = Edge.top, content: (Item) -> View): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun popover(isPresented: Binding<Boolean>, attachmentAnchor: Any? = null, arrowEdge: Edge = Edge.top, content: () -> View): View = this.sref()

    // No need to @bridge because we define in terms of `EnvironmentValues.refresh`
    fun refreshable(action: suspend () -> Unit): View {
        return environment({ it -> EnvironmentValues.shared.setrefresh(it) }, RefreshAction(action = action))
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun renameAction(isFocused: Any): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun renameAction(action: () -> Unit): View = this.sref()

    fun rotationEffect(angle: Angle): View {
        return ModifiedContent(content = this, modifier = RenderModifier l@{ context ->
            val animatable = Float(angle.degrees).asAnimatable(context = context)
            return@l context.modifier.rotate(animatable.value)
        })
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun rotationEffect(angle: Angle, anchor: UnitPoint): View {
        fatalError()
    }

    fun rotationEffect(bridgedAngle: Double, anchorX: Double, anchorY: Double): View {
        // Note: anchor is currently ignored
        return rotationEffect(Angle.radians(bridgedAngle))
    }

    fun rotation3DEffect(angle: Angle, axis: Tuple3<Double, Double, Double>, anchor: UnitPoint = UnitPoint.center, anchorZ: Double = 0.0, perspective: Double = 1.0): View {
        return ModifiedContent(content = this, modifier = RenderModifier l@{ context ->
            val animatable = Float(angle.degrees).asAnimatable(context = context)
            // Try to approximate SwiftUI's perspective adaptation to view size
            val size = remember { -> mutableStateOf(IntSize.Zero) }
            val dimension = max(size.value.width * axis.y, size.value.height * axis.x)
            val distance = max(0.1f, Float(dimension / 65)) / Float(perspective)
            return@l context.modifier
                .onGloballyPositioned { it -> size.value = it.size }
                .graphicsLayer(transformOrigin = TransformOrigin(pivotFractionX = Float(anchor.x), pivotFractionY = Float(anchor.y)), rotationX = Float(axis.x) * animatable.value, rotationY = Float(axis.y) * animatable.value, rotationZ = Float(axis.z) * animatable.value, cameraDistance = distance)
        })
    }

    fun rotation3DEffect(angle: Angle, axis: Tuple3<Int, Int, Int>, perspective: Double = 1.0): View = rotation3DEffect(angle, axis = Tuple3(Double(axis.x), Double(axis.y), Double(axis.z)), perspective = perspective)

    fun rotation3DEffect(bridgedAngle: Double, axis: Tuple3<Double, Double, Double>, anchorX: Double, anchorY: Double, anchorZ: Double, perspective: Double): View = rotation3DEffect(Angle.radians(bridgedAngle), axis = axis, anchor = UnitPoint(x = anchorX, y = anchorY), anchorZ = anchorZ, perspective = perspective)

    fun saturation(amount: Double): View {
        return ModifiedContent(content = this, modifier = RenderModifier l@{ it -> return@l it.modifier.then(SaturationModifier(amount = amount)) })
    }

    // No need to @bridge because we define in terms of `.aspectRatio`
    fun scaledToFit(): View = aspectRatio(null, contentMode = ContentMode.fit)

    // No need to @bridge because we define in terms of `.aspectRatio`
    fun scaledToFill(): View = aspectRatio(null, contentMode = ContentMode.fill)

    fun scaleEffect(scale: CGSize): View = scaleEffect(x = scale.width, y = scale.height)

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun scaleEffect(scale: CGSize, anchor: UnitPoint): View = scaleEffect(x = scale.width, y = scale.height)

    fun scaleEffect(s: Double): View = scaleEffect(x = s, y = s)

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun scaleEffect(s: Double, anchor: UnitPoint): View = scaleEffect(x = s, y = s)

    fun scaleEffect(x: Double = 1.0, y: Double = 1.0): View {
        return ModifiedContent(content = this, modifier = RenderModifier l@{ context ->
            val animatable = Tuple2(Float(x), Float(y)).asAnimatable(context = context)
            return@l context.modifier.scale(scaleX = animatable.value.element0, scaleY = animatable.value.element1)
        })
    }

    fun scaleEffect(x: Double = 1.0, y: Double = 1.0, anchorX: Double, anchorY: Double): View {
        // Note: anchor is currently ignored
        return scaleEffect(x = x, y = y)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun scaleEffect(x: Double = 1.0, y: Double = 1.0, anchor: UnitPoint): View = scaleEffect(x = x, y = y)

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun sectionActions(content: () -> View): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun selectionDisabled(isDisabled: Boolean = true): View = this.sref()

    fun shadow(color: Color = Color(white = 0.0, opacity = 0.33), radius: Double, x: Double = 0.0, y: Double = 0.0): View {
        return ModifiedContent(content = this, modifier = RenderModifier { renderable, context ->
            // See Shadowed.kt
            Shadowed(context = context, color = color.colorImpl(), offsetX = x.dp, offsetY = y.dp, blurRadius = radius.dp) { context -> renderable.Render(context = context) }
        })
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun statusBarHidden(hidden: Boolean = true): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun symbolEffectsRemoved(isEnabled: Boolean = true): View = this.sref()

    fun tag(tag: Any?): View = ModifiedContent(content = this, modifier = TagModifier(value = tag, role = ModifierRole.tag))

    fun task(priority: TaskPriority = TaskPriority.userInitiated, action: suspend () -> Unit): View = task(id = 0, priority = priority, action)

    fun task(id: Any, priority: TaskPriority = TaskPriority.userInitiated, action: suspend () -> Unit): View {
        val value = id
        return ModifiedContent(content = this, modifier = SideEffectModifier l@{ _ ->
            val handler = rememberUpdatedState(action)
            LaunchedEffect(value) { -> handler.value() }
            return@l ComposeResult.ok
        })
    }

    fun task(id: Any, bridgedAction: (CompletionHandler) -> Unit): View {
        val value = id
        return ModifiedContent(content = this, modifier = SideEffectModifier l@{ _ ->
            val actionState = rememberUpdatedState(bridgedAction)
            LaunchedEffect(value) { ->
                kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                    val completionHandler = CompletionHandler({ ->
                        try {
                            continuation.resume(Unit, null)
                        } catch (error: Throwable) {
                            @Suppress("NAME_SHADOWING") val error = error.aserror()
                        }
                    })
                    continuation.invokeOnCancellation { _ -> completionHandler.onCancel?.invoke() }
                    actionState.value(completionHandler)
                }
            }
            return@l ComposeResult.ok
        })
    }

    fun tint(color: Color?): View {
        return environment({ it -> EnvironmentValues.shared.set_tint(it) }, color, affectsEvaluate = false)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun tint(tint: ShapeStyle?): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun transformEffect(transform: Any): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun <V> transformEnvironment(keyPath: Any, transform: (InOut<V>) -> Unit): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun typeSelectEquivalent(text: Text?): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun typeSelectEquivalent(stringKey: LocalizedStringKey): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun typeSelectEquivalent(stringResource: LocalizedStringResource): View = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun typeSelectEquivalent(string: String): View = this.sref()

    fun zIndex(value: Double): View = ModifiedContent(content = this, modifier = ZIndexModifier(zIndex = value))
    fun <P, Output> onReceive(publisher: P, perform: (Output) -> Unit): View where P: Publisher<Output, *> {
        val action = perform
        return ModifiedContent(content = this, modifier = SideEffectModifier l@{ _ ->
            val latestAction = rememberUpdatedState(action)
            val subscription = remember { ->
                publisher.sink { output -> latestAction.value(output) }
            }
            DisposableEffect(subscription) { ->
                onDispose { -> subscription.cancel() }
            }
            return@l ComposeResult.ok
        })
    }
    /// Compose this view without an existing context - typically called when integrating a SwiftUI view tree into pure Compose.
    ///
    /// - Seealso: `Compose(context:)`
    @Composable
    fun Compose(): ComposeResult = Compose(context = ComposeContext())

    /// Compose this view's content.
    ///
    /// Calls to `Compose` are added by the transpiler.
    @Composable
    fun Compose(context: ComposeContext): ComposeResult {
        val matchtarget_6 = context.composer
        if (matchtarget_6 != null) {
            val composer = matchtarget_6
            val composerContext: (Boolean) -> ComposeContext = l@{ retain ->
                if (retain) {
                    return@l context
                }
                var context = context.sref()
                context.composer = null
                return@l context
            }
            return composer.Compose(this, composerContext)
        } else {
            _ComposeContent(context = context)
            return ComposeResult.ok
        }
    }

    /// DEPRECATED
    @Composable
    fun ComposeContent(context: ComposeContext) = Unit

    /// This function provides a non-escaping compose context to avoid excessive recompositions when the calling code
    /// does not need to access the underlying `Renderables`.
    @Composable
    fun _ComposeContent(context: ComposeContext) {
        for (renderable in Evaluate(context = context, options = 0)) {
            renderable.Render(context = context)
        }
    }

    /// Evaluate renderable content.
    ///
    /// - Warning: Do not give `options` a default value in this function signature. We have seen it cause bugs in which
    ///     the default version of the function is always invoked, ignoring implementor overrides.
    @Composable
    fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val matchtarget_7 = this as? Renderable
        if (matchtarget_7 != null) {
            val renderable = matchtarget_7
            return listOf(this)
        } else {
            StateTracking.pushBody()
            val renderables = body().Evaluate(context = context, options = options)
            StateTracking.popBody()
            return renderables.sref()
        }
    }

    /// Helper for the rare cases that we want to treat a `View` as a `Renderable` without evaluating it.
    ///
    /// - Warning: For special cases only.
    fun asRenderable(): Renderable = (this as? Renderable ?: ViewRenderable(view = this)).sref()
    fun modifier(viewModifier: ViewModifier): View = ViewModifierView(view = this, modifier = viewModifier)
}

// Use inline final func to get reified generic type
inline fun <reified T> View.environment(object_: T?): View = environmentObject(type = T::class, object_ = object_)


/// Helper for the rare cases that we want to treat a `View` as a `Renderable` without evaluating it.
///
/// - Warning: For special cases only.
internal class ViewRenderable: Renderable {
    internal val view: View

    internal constructor(view: View) {
        // Don't copy
        this.view = view
    }

    @Composable
    override fun Render(context: ComposeContext) {
        view.Compose(context = context)
    }
}

