package skip.ui

import kotlin.reflect.KClass
import skip.lib.*
import skip.lib.Array
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat


/// Common corner radius for our overlay presentations.
internal val overlayPresentationCornerRadius = 16.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SheetPresentation(isPresented: Binding<Boolean>, isFullScreen: Boolean, context: ComposeContext, content: () -> View, onDismiss: (() -> Unit)?) {
    val interactiveDismissDisabledPreference = rememberSaveable(stateSaver = context.stateSaver as Saver<Preference<Boolean>, Any>) { -> mutableStateOf(Preference<Boolean>(key = InteractiveDismissDisabledPreferenceKey::class)) }
    val interactiveDismissDisabledCollector = PreferenceCollector<Boolean>(key = InteractiveDismissDisabledPreferenceKey::class, state = interactiveDismissDisabledPreference)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isPresentedValue = isPresented.get()
    if (isPresentedValue || sheetState.isVisible) {
        // Don't fully evaluate content until we set up the presented environment. For now we just want
        // to get at the modifiers to look for `BackDismissDisabled`
        val contentRenderables = ComposeBuilder.from(content).Evaluate(context = context, options = EvaluateOptions(isKeepNonModified = true).value)
        val topInset = remember { -> mutableStateOf(0.dp) }
        val topInsetPx = with(LocalDensity.current) { -> topInset.value.toPx() }
        val handleHeight = (if (isFullScreen) 0.dp else 8.dp).sref()
        val handleHeightPx = with(LocalDensity.current) { -> handleHeight.toPx() }
        val handlePadding = (if (isFullScreen) 0.dp else 10.dp).sref()
        val handlePaddingPx = with(LocalDensity.current) { -> handlePadding.toPx() }
        val sheetMaxWidth = (if (isFullScreen) Dp.Unspecified else BottomSheetDefaults.SheetMaxWidth).sref()
        val shape = GenericShape { size, _ ->
            val y = (topInsetPx - handleHeightPx - handlePaddingPx).sref()
            addRect(Rect(offset = Offset(x = 0.0f, y = y), size = Size(width = size.width, height = size.height - y)))
        }
        val interactiveDismissDisabled = isFullScreen || interactiveDismissDisabledPreference.value.reduced
        // Implementing backDismissDisabled as a preference doesn't work because preferences require an extra composition
        // and only the first composition of `ModalBottomSheetProperties` is taken into account. So we require the
        // modifier directly on the content view
        val backDismissDisabled = isBackDismissDisabled(on = contentRenderables)
        val onDismissRequest = { -> isPresented.set(false) }
        val properties = ModalBottomSheetProperties(shouldDismissOnBackPress = !backDismissDisabled)
        ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState, sheetMaxWidth = sheetMaxWidth, sheetGesturesEnabled = !interactiveDismissDisabled, containerColor = androidx.compose.ui.graphics.Color.Unspecified, shape = shape, dragHandle = null, contentWindowInsets = { -> WindowInsets(0.dp, 0.dp, 0.dp, 0.dp) }, properties = properties) { ->

            SyncSystemBarsWithTheme()

            val verticalSizeClass = EnvironmentValues.shared.verticalSizeClass
            val isEdgeToEdge = EnvironmentValues.shared._isEdgeToEdge == true
            val sheetDepth = EnvironmentValues.shared._sheetDepth
            var systemBarEdges: Edge.Set = (if (isFullScreen) Edge.Set.all else Edge.Set.of(Edge.Set.top, Edge.Set.bottom)).sref()

            val detentPreferences = rememberSaveable(stateSaver = context.stateSaver as Saver<Preference<PresentationDetentPreferences>, Any>) { -> mutableStateOf(Preference<PresentationDetentPreferences>(key = PresentationDetentPreferenceKey::class)) }
            val detentPreferencesCollector = PreferenceCollector<PresentationDetentPreferences>(key = PresentationDetentPreferences::class, state = detentPreferences)
            val reducedDetentPreferences = detentPreferences.value.reduced.sref()

            if (!isFullScreen && verticalSizeClass != UserInterfaceSizeClass.compact) {
                systemBarEdges.remove(Edge.Set.top)
                if (!isEdgeToEdge) {
                    systemBarEdges.remove(Edge.Set.bottom)
                }

                // TODO: add custom cases
                // Add inset depending on the presentation detent
                val inset: Dp
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp.sref()
                val detent: PresentationDetent = reducedDetentPreferences.detent
                when (detent) {
                    is PresentationDetent.MediumCase -> inset = screenHeight / 2
                    is PresentationDetent.HeightCase -> {
                        val h = detent.associated0
                        inset = (screenHeight - h.dp).sref()
                    }
                    is PresentationDetent.FractionCase -> {
                        val f = detent.associated0
                        inset = screenHeight * Float(1 - f)
                    }
                    else -> {
                        // We have to delay access to WindowInsets until inside the ModalBottomSheet composable to get accurate values
                        val topBarHeight = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
                        // Add 44 for draggable area in case content is not draggable
                        inset = (topBarHeight + (24 * sheetDepth).dp + 44.dp).sref()
                    }
                }

                topInset.value = inset
                // Draw the drag handle and the presentation root content area below it
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(inset - handleHeight - handlePadding))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { -> Capsule().fill(Color.primary.opacity(0.4)).frame(width = 60.0, height = Double(handleHeight.value)).Compose(context = context) }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(handlePadding))
            } else if (!isEdgeToEdge) {
                systemBarEdges.remove(Edge.Set.top)
                systemBarEdges.remove(Edge.Set.bottom)
                val inset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
                topInset.value = inset
                // Push the presentation root content area below the top bar
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(inset))
            } else {
                topInset.value = 0.dp
            }

            val clipShape = RoundedCornerShape(topStart = if (isFullScreen) 0.dp else overlayPresentationCornerRadius.dp, topEnd = if (isFullScreen) 0.dp else overlayPresentationCornerRadius.dp)
            Box(modifier = Modifier.weight(1.0f).clip(clipShape).nestedScroll(DisableScrollToDismissConnection())) { ->
                // Place outside of PresentationRoot recomposes
                val stateSaver = remember { -> ComposeStateSaver() }
                val presentationContext = context.content(stateSaver = stateSaver)
                // Place inside of ModalBottomSheet, which renders content async
                PresentationRoot(context = presentationContext, absoluteSystemBarEdges = systemBarEdges) { context ->
                    EnvironmentValues.shared.setValues(l@{ it ->
                        if (!isFullScreen) {
                            it.set_sheetDepth(sheetDepth + 1)
                        }
                        it.setdismiss(DismissAction(action = { -> isPresented.set(false) }))
                        return@l ComposeResult.ok
                    }, in_ = { ->
                        PreferenceValues.shared.collectPreferences(arrayOf(interactiveDismissDisabledCollector, detentPreferencesCollector)) { ->
                            for (renderable in contentRenderables.sref()) {
                                renderable.Render(context = context)
                            }
                        }
                    })
                }
            }
            if (!isEdgeToEdge) {
                // Move the presentation root content area above the bottom bar
                val inset = max(0.dp, WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() - WindowInsets.ime.asPaddingValues().calculateBottomPadding())
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(inset))
            }
        }
    }

    // When our isPresented binding flips from true to false, hide the sheet if needed and invoke onDismiss
    val wasPresented = remember { -> mutableStateOf(isPresentedValue) }
    val onDismissState = rememberUpdatedState(onDismiss)
    if (isPresentedValue) {
        wasPresented.value = true
    } else {
        LaunchedEffect(true) { ->
            if (sheetState.targetValue != SheetValue.Hidden) {
                sheetState.hide()
            }
            if (wasPresented.value) {
                wasPresented.value = false
                onDismissState.value?.invoke()
            }
        }
    }
}

@Composable
private fun SyncSystemBarsWithTheme() {
    val view = LocalView.current.sref()
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5
    DisposableEffect(dark) { ->
        (view.parent as? DialogWindowProvider)?.window.sref()?.let { window ->
            WindowCompat.getInsetsController(window, view).apply { ->
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
        onDispose { ->  }
    }
}

internal fun isBackDismissDisabled(on: kotlin.collections.List<Renderable>): Boolean {
    val renderables = on
    for (renderable in renderables.sref()) {
        renderable.forEachModifier(perform = { it ->
            (it as? BackDismissDisabledModifier)?.disabled
        })?.let { disabled ->
            return disabled
        }
    }
    return false
}

internal class DisableScrollToDismissConnection: NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available.copy(x = 0.0f)

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = Async.run l@{
        return@l available.copy(x = 0.0f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfirmationDialogPresentation(title: Text?, isPresented: Binding<Boolean>, context: ComposeContext, actions: View, message: View? = null) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (isPresented.get() || sheetState.isVisible) {
        // Collect buttons and message text
        val actionRenderables = actions.Evaluate(context = context, options = 0)
        val composableActions: kotlin.collections.List<Renderable> = actionRenderables.mapNotNull l@{ it ->
            val stripped = it.strip()
            return@l stripped as? Button ?: stripped as? Link ?: stripped as? NavigationLink
        }
        val messageRenderables: kotlin.collections.List<Renderable> = (message?.Evaluate(context = context, options = 0) ?: listOf()).sref()
        val messageText = messageRenderables.mapNotNull { it -> it.strip() as? Text }.firstOrNull()

        ModalBottomSheet(onDismissRequest = { -> isPresented.set(false) }, sheetState = sheetState, containerColor = androidx.compose.ui.graphics.Color.Transparent, dragHandle = null, contentWindowInsets = { -> WindowInsets(0.dp, 0.dp, 0.dp, 0.dp) }) { ->
            // Add padding to always keep the sheet away from the top of the screen. It should tap to dismiss like the background
            val interactionSource = remember { -> MutableInteractionSource() }
            Box(modifier = Modifier.fillMaxWidth().height(128.dp).clickable(interactionSource = interactionSource, indication = null, onClick = { -> isPresented.set(false) }))

            val stateSaver = remember { -> ComposeStateSaver() }
            val scrollState = rememberScrollState()
            val isEdgeToEdge = EnvironmentValues.shared._isEdgeToEdge == true
            val bottomSystemBarPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
            val modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = if (isEdgeToEdge) 0.dp else bottomSystemBarPadding)
                .clip(shape = RoundedCornerShape(topStart = overlayPresentationCornerRadius.dp, topEnd = overlayPresentationCornerRadius.dp))
                .background(Color.overlayBackground.colorImpl())
                .padding(bottom = if (isEdgeToEdge) bottomSystemBarPadding else 0.dp)
                .verticalScroll(scrollState)
            val contentContext = context.content(stateSaver = stateSaver)
            Column(modifier = modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) { -> RenderConfirmationDialog(title = title, context = contentContext, isPresented = isPresented, actionRenderables = composableActions, message = messageText) }
        }
    }
    if (!isPresented.get()) {
        LaunchedEffect(true) { ->
            if (sheetState.targetValue != SheetValue.Hidden) {
                sheetState.hide()
            }
        }
    }
}

@Composable
internal fun RenderConfirmationDialog(title: Text?, context: ComposeContext, isPresented: Binding<Boolean>, actionRenderables: kotlin.collections.List<Renderable>, message: Text?) {
    val padding = 16.dp.sref()
    if (title != null) {
        androidx.compose.material3.Text(modifier = Modifier.padding(horizontal = padding, vertical = 8.dp), color = Color.secondary.colorImpl(), text = title.localizedTextString(), style = Font.callout.bold().fontImpl())
    }
    if (message != null) {
        androidx.compose.material3.Text(modifier = Modifier.padding(start = padding, top = 8.dp, end = padding, bottom = padding), color = Color.secondary.colorImpl(), text = message.localizedTextString(), style = Font.callout.fontImpl())
    }
    if (title != null || message != null) {
        androidx.compose.material3.Divider()
    }

    val buttonModifier = Modifier.padding(horizontal = padding, vertical = padding)
    val buttonFont = Font.title3.sref()
    val tint = (EnvironmentValues.shared._tint ?: Color.accentColor).colorImpl()
    if (actionRenderables.size <= 0) {
        ConfirmationDialogButton(action = { -> isPresented.set(false) }) { -> androidx.compose.material3.Text(modifier = buttonModifier, color = tint, text = stringResource(android.R.string.ok), style = buttonFont.fontImpl()) }
        return
    }

    var cancelButton: Button? = null
    for (actionRenderable in actionRenderables.sref()) {
        var button = actionRenderable.strip() as? Button
        (actionRenderable.strip() as? Link)?.let { link ->
            link.ComposeAction()
            button = link.content
        }
        if (button != null) {
            if (button.role == ButtonRole.cancel) {
                cancelButton = button
                continue
            }
            ConfirmationDialogButton(action = { ->
                isPresented.set(false)
                button.action()
            }) { ->
                val text = button.label.Evaluate(context = context, options = 0).mapNotNull { it -> it.strip() as? Text }.firstOrNull()
                val color = if (button.role == ButtonRole.destructive) Color.red.colorImpl() else tint
                androidx.compose.material3.Text(modifier = buttonModifier, color = color, text = text?.localizedTextString() ?: "", maxLines = 1, style = buttonFont.fontImpl())
            }
        } else {
            (actionRenderable.strip() as? NavigationLink).sref()?.let { navigationLink ->
                val navigationAction = navigationLink.navigationAction()
                ConfirmationDialogButton(action = { ->
                    isPresented.set(false)
                    navigationAction()
                }) { ->
                    val text = navigationLink.label.Evaluate(context = context, options = 0).mapNotNull { it -> it.strip() as? Text }.firstOrNull()
                    androidx.compose.material3.Text(modifier = buttonModifier, color = tint, text = text?.localizedTextString() ?: "", maxLines = 1, style = buttonFont.fontImpl())
                }
            }
        }
        androidx.compose.material3.Divider()
    }
    if (cancelButton != null) {
        ConfirmationDialogButton(action = { ->
            isPresented.set(false)
            cancelButton.action()
        }) { ->
            val text = cancelButton.label.Evaluate(context = context, options = 0).mapNotNull { it -> it.strip() as? Text }.firstOrNull()
            androidx.compose.material3.Text(modifier = buttonModifier, color = tint, text = text?.localizedTextString() ?: "", maxLines = 1, style = buttonFont.bold().fontImpl())
        }
    } else {
        ConfirmationDialogButton(action = { -> isPresented.set(false) }) { -> androidx.compose.material3.Text(modifier = buttonModifier, color = tint, text = stringResource(android.R.string.cancel), style = buttonFont.bold().fontImpl()) }
    }
}

@Composable
internal fun ConfirmationDialogButton(action: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().requiredHeightIn(min = 60.dp).clickable(onClick = action), contentAlignment = androidx.compose.ui.Alignment.Center) { -> content() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlertPresentation(title: Text? = null, titleResource: Int? = null, isPresented: Binding<Boolean>, context: ComposeContext, actions: View, message: View? = null) {
    if (!isPresented.get()) {
        return
    }
    // Collect buttons and message text
    val actionRenderables = actions.Evaluate(context = context, options = 0)
    val textFields: kotlin.collections.List<TextField> = actionRenderables.mapNotNull l@{ it ->
        val stripped = it.strip()
        return@l stripped as? TextField ?: (stripped as? SecureField)?.textField
    }
    val optionRenderables: kotlin.collections.List<Renderable> = actionRenderables.mapNotNull l@{ it ->
        val stripped = it.strip()
        return@l stripped as? Button ?: stripped as? NavigationLink ?: stripped as? Link
    }
    val messageRenderables: kotlin.collections.List<Renderable> = (message?.Evaluate(context = context, options = 0) ?: listOf()).sref()
    val messageText = messageRenderables.mapNotNull { it -> it.strip() as? Text }.firstOrNull()

    BasicAlertDialog(onDismissRequest = { -> isPresented.set(false) }) { ->
        val modifier = Modifier.wrapContentWidth().wrapContentHeight().then(context.modifier)
        Surface(modifier = modifier, shape = MaterialTheme.shapes.large, tonalElevation = AlertDialogDefaults.TonalElevation) { ->
            val contentContext = context.content()
            Column(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) { -> RenderAlert(title = title, titleResource = titleResource, context = contentContext, isPresented = isPresented, textFields = textFields, actionRenderables = optionRenderables, message = messageText) }
        }
    }
}

@Composable
internal fun RenderAlert(title: Text?, titleResource: Int? = null, context: ComposeContext, isPresented: Binding<Boolean>, textFields: kotlin.collections.List<TextField>, actionRenderables: kotlin.collections.List<Renderable>, message: Text?) {
    val padding = 16.dp.sref()
    if (title != null) {
        androidx.compose.material3.Text(modifier = Modifier.padding(horizontal = padding, vertical = 8.dp), color = Color.primary.colorImpl(), text = title.localizedTextString(), style = Font.title3.bold().fontImpl(), textAlign = TextAlign.Center)
    } else if (titleResource != null) {
        androidx.compose.material3.Text(modifier = Modifier.padding(horizontal = padding, vertical = 8.dp), color = Color.primary.colorImpl(), text = stringResource(titleResource), style = Font.title3.bold().fontImpl(), textAlign = TextAlign.Center)
    }
    if (message != null) {
        androidx.compose.material3.Text(modifier = Modifier.padding(start = padding, end = padding), color = Color.primary.colorImpl(), text = message.localizedTextString(), style = Font.callout.fontImpl(), textAlign = TextAlign.Center)
    }

    for (textField in textFields.sref()) {
        val topPadding = (if (textField == textFields.firstOrNull()) 16.dp else 8.dp).sref()
        val textFieldContext = context.content(modifier = Modifier.padding(top = topPadding, start = padding, end = padding))
        textField.Compose(context = textFieldContext)
    }

    androidx.compose.material3.Divider(modifier = Modifier.padding(top = 16.dp))

    val buttonModifier = Modifier.padding(horizontal = padding, vertical = 12.dp)
    val buttonFont = Font.title3.sref()
    val tint = (EnvironmentValues.shared._tint ?: Color.accentColor).colorImpl()
    if (actionRenderables.size <= 0) {
        AlertButton(modifier = Modifier.fillMaxWidth(), renderable = null, isPresented = isPresented) { -> androidx.compose.material3.Text(modifier = buttonModifier, color = tint, text = stringResource(android.R.string.ok), style = buttonFont.fontImpl()) }
        return
    }

    val buttonContent: @Composable (Renderable, Boolean) -> Unit = { renderable, isCancel ->
        val stripped = renderable.strip()
        val button = stripped as? Button ?: (stripped as? Link)?.content
        val label = button?.label ?: (stripped as? NavigationLink)?.label
        val text = label?.Evaluate(context = context, options = 0)?.mapNotNull({ it -> it.strip() as? Text })?.firstOrNull()
        val color = if (button?.role == ButtonRole.destructive) Color.red.colorImpl() else tint
        val style = if (isCancel) buttonFont.bold().fontImpl() else buttonFont.fontImpl()
        androidx.compose.material3.Text(modifier = buttonModifier, color = color, text = text?.localizedTextString() ?: "", maxLines = 1, style = style)
    }

    val optionRenderables = actionRenderables.filter l@{ it ->
        val stripped = it.strip()
        (stripped as? Button)?.let { button ->
            return@l button.role != ButtonRole.cancel
        }
        return@l stripped is Link || stripped is NavigationLink
    }
    val cancelButton = actionRenderables.firstOrNull l@{ it ->
        val button_0 = it.strip() as? Button
        if (button_0 == null) {
            return@l false
        }
        return@l button_0.role == ButtonRole.cancel
    }
    val cancelCount = if (cancelButton == null) 0 else 1
    if (optionRenderables.size + cancelCount == 2) {
        // Horizontal layout for two buttons //TODO: Should revert to vertical when text is too long
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) { ->
            val modifier = Modifier.weight(1.0f)
            (cancelButton ?: optionRenderables.firstOrNull()).sref()?.let { renderable ->
                AlertButton(modifier = modifier, renderable = renderable, isPresented = isPresented) { -> buttonContent(renderable, renderable === cancelButton) }
                androidx.compose.material3.VerticalDivider()
            }
            optionRenderables.lastOrNull()?.let { button ->
                AlertButton(modifier = modifier, renderable = button, isPresented = isPresented) { -> buttonContent(button, false) }
            }
        }
    } else {
        // Vertical layout
        val modifier = Modifier.fillMaxWidth()
        for (actionRenderable in optionRenderables.sref()) {
            AlertButton(modifier = modifier, renderable = actionRenderable, isPresented = isPresented) { -> buttonContent(actionRenderable, false) }
            if (actionRenderable !== optionRenderables.lastOrNull() || cancelButton != null) {
                androidx.compose.material3.Divider()
            }
        }
        if (cancelButton != null) {
            AlertButton(modifier = modifier, renderable = cancelButton, isPresented = isPresented) { -> buttonContent(cancelButton, true) }
        }
    }
}

@Composable
internal fun AlertButton(modifier: Modifier, renderable: Renderable?, isPresented: Binding<Boolean>, content: @Composable () -> Unit) {
    var action: (() -> Unit)? = null
    val stripped = renderable?.strip()
    val matchtarget_0 = stripped as? Button
    if (matchtarget_0 != null) {
        val button = matchtarget_0
        action = button.action
    } else {
        val matchtarget_1 = stripped as? Link
        if (matchtarget_1 != null) {
            val link = matchtarget_1
            link.ComposeAction()
            action = link.content.action
        } else {
            (stripped as? NavigationLink).sref()?.let { navigationLink ->
                action = navigationLink.navigationAction()
            }
        }
    }
    Box(modifier = modifier.clickable(onClick = { ->
        isPresented.set(false)
        action?.invoke()
    }), contentAlignment = androidx.compose.ui.Alignment.Center) { -> content() }
}

enum class PresentationAdaptation {
    automatic,
    none,
    popover,
    sheet,
    fullScreenCover;

    @androidx.annotation.Keep
    companion object {
    }
}

class PresentationBackgroundInteraction {
    internal val enabled: Boolean?
    internal val upThrough: PresentationDetent?

    internal constructor(enabled: Boolean? = null, upThrough: PresentationDetent? = null) {
        this.enabled = enabled
        this.upThrough = upThrough
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = PresentationBackgroundInteraction(enabled = null, upThrough = null)

        val enabled = PresentationBackgroundInteraction(enabled = true, upThrough = null)

        fun enabled(upThrough: PresentationDetent): PresentationBackgroundInteraction = PresentationBackgroundInteraction(enabled = true, upThrough = upThrough)

        val disabled = PresentationBackgroundInteraction(enabled = false, upThrough = null)
    }
}

enum class PresentationContentInteraction {
    automatic,
    resizes,
    scrolls;

    @androidx.annotation.Keep
    companion object {
    }
}

sealed class PresentationDetent {
    class MediumCase: PresentationDetent() {
    }
    class LargeCase: PresentationDetent() {
    }
    class FractionCase(val associated0: Double): PresentationDetent() {
    }
    class HeightCase(val associated0: Double): PresentationDetent() {
    }
    class CustomCase(val associated0: KClass<*>): PresentationDetent() {
    }

    class Context {
        val maxDetentValue: Double

        constructor(maxDetentValue: Double) {
            this.maxDetentValue = maxDetentValue
        }

        //        public subscript<T>(dynamicMember keyPath: KeyPath<EnvironmentValues, T>) -> T { get { fatalError() } }

        @androidx.annotation.Keep
        companion object {
        }
    }

    override fun hashCode(): Int {
        var hasher = Hasher()
        hash(into = InOut<Hasher>({ hasher }, { hasher = it }))
        return hasher.finalize()
    }
    fun hash(into: InOut<Hasher>) {
        val hasher = into
        when (this) {
            is PresentationDetent.MediumCase -> hasher.value.combine(1)
            is PresentationDetent.LargeCase -> hasher.value.combine(2)
            is PresentationDetent.FractionCase -> {
                val fraction = this.associated0
                hasher.value.combine(3)
                hasher.value.combine(fraction)
            }
            is PresentationDetent.HeightCase -> {
                val height = this.associated0
                hasher.value.combine(4)
                hasher.value.combine(height)
            }
            is PresentationDetent.CustomCase -> {
                val type = this.associated0
                hasher.value.combine(String(describing = type))
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PresentationDetent) {
            return false
        }
        val lhs = this
        val rhs = other
        when (lhs) {
            is PresentationDetent.MediumCase -> {
                if (rhs is PresentationDetent.MediumCase) {
                    return true
                } else {
                    return false
                }
            }
            is PresentationDetent.LargeCase -> {
                if (rhs is PresentationDetent.LargeCase) {
                    return true
                } else {
                    return false
                }
            }
            is PresentationDetent.FractionCase -> {
                val fraction1 = lhs.associated0
                if (rhs is PresentationDetent.FractionCase) {
                    val fraction2 = rhs.associated0
                    return fraction1 == fraction2
                } else {
                    return false
                }
            }
            is PresentationDetent.HeightCase -> {
                val height1 = lhs.associated0
                if (rhs is PresentationDetent.HeightCase) {
                    val height2 = rhs.associated0
                    return height1 == height2
                } else {
                    return false
                }
            }
            is PresentationDetent.CustomCase -> {
                val type1 = lhs.associated0
                if (rhs is PresentationDetent.CustomCase) {
                    val type2 = rhs.associated0
                    return type1 == type2
                } else {
                    return false
                }
            }
        }
    }

    @androidx.annotation.Keep
    companion object {
        val medium: PresentationDetent = MediumCase()
        val large: PresentationDetent = LargeCase()
        fun fraction(associated0: Double): PresentationDetent = FractionCase(associated0)
        fun height(associated0: Double): PresentationDetent = HeightCase(associated0)
        fun custom(associated0: KClass<*>): PresentationDetent = CustomCase(associated0)


        internal fun forBridged(identifier: Int, value: Double): PresentationDetent? {
            when (identifier) {
                0 -> return PresentationDetent.medium
                1 -> return PresentationDetent.large
                2 -> return PresentationDetent.fraction(value)
                3 -> return PresentationDetent.height(value)
                else -> return null
            }
        }
    }
}

interface CustomPresentationDetent {
}

//public struct PresentedWindowContent<Data, Content> : View where Data : Decodable, Data : Encodable, Data : Hashable, Content : View {
//
//    public typealias Body = NeverView
//    public var body: Body { fatalError() }
//}

@androidx.annotation.Keep
internal class PresentationDetentPreferenceKey: PreferenceKey<PresentationDetentPreferences> {

    @androidx.annotation.Keep
    companion object: PreferenceKeyCompanion<PresentationDetentPreferences> {
        override val defaultValue = PresentationDetentPreferences()

        override fun reduce(value: InOut<PresentationDetentPreferences>, nextValue: () -> PresentationDetentPreferences) {
            value.value = value.value.reduce(nextValue())
        }
    }
}

internal class PresentationDetentPreferences {
    internal val detent: PresentationDetent

    internal constructor(detent: PresentationDetent? = null) {
        this.detent = detent ?: PresentationDetent.large
    }

    internal fun reduce(next: PresentationDetentPreferences): PresentationDetentPreferences = next

    override fun equals(other: Any?): Boolean {
        if (other !is PresentationDetentPreferences) {
            return false
        }
        val lhs = this
        val rhs = other
        return lhs.detent == rhs.detent
    }
}


internal class PresentationModifier: SideEffectModifier {
    private val presentation: @Composable (ComposeContext) -> Unit
    private val providesNavigation: Boolean

    internal constructor(providesNavigation: Boolean, presentation: @Composable (ComposeContext) -> Unit): super() {
        this.providesNavigation = providesNavigation
        this.presentation = presentation
        this.action = l@{ context ->
            // Clear environment state that should not transfer to presentations
            val providedNavigator = LocalNavigator provides if (providesNavigation) LocalNavigator.current else null
            CompositionLocalProvider(providedNavigator) { ->
                EnvironmentValues.shared.setValues(l@{ it ->
                    it.set_animation(null)
                    it.set_searchableState(null)
                    return@l ComposeResult.ok
                }, in_ = { -> presentation(context.content()) })
            }
            return@l ComposeResult.ok
        }
    }
}

/// Used to disable the back button from dismissing a presentation.
internal class BackDismissDisabledModifier: RenderModifier {
    internal val disabled: Boolean

    internal constructor(disabled: Boolean): super() {
        this.disabled = disabled
    }
}

@androidx.annotation.Keep
internal class InteractiveDismissDisabledPreferenceKey: PreferenceKey<Boolean> {

    @androidx.annotation.Keep
    companion object: PreferenceKeyCompanion<Boolean> {
        override val defaultValue = false

        override fun reduce(value: InOut<Boolean>, nextValue: () -> Boolean) {
            value.value = nextValue()
        }
    }
}
