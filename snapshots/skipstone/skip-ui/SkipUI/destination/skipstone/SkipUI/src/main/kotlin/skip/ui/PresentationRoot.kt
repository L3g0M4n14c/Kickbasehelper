package skip.ui

import skip.lib.*
import skip.lib.Array
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection

/// The root of a presentation, such as the root presentation or a sheet.
@Composable
fun PresentationRoot(defaultColorScheme: ColorScheme? = null, absoluteSystemBarEdges: Edge.Set = Edge.Set.all, context: ComposeContext, content: @Composable (ComposeContext) -> Unit) {
    val systemBarEdges = absoluteSystemBarEdges
    launchUIApplicationActivity()

    val preferredColorScheme = rememberSaveable(stateSaver = context.stateSaver as Saver<Preference<PreferredColorScheme>, Any>) { -> mutableStateOf(Preference<PreferredColorScheme>(key = PreferredColorSchemePreferenceKey::class)) }
    val preferredColorSchemeCollector = PreferenceCollector<PreferredColorScheme>(key = PreferredColorSchemePreferenceKey::class, state = preferredColorScheme)
    PreferenceValues.shared.collectPreferences(arrayOf(preferredColorSchemeCollector)) { ->
        val materialColorScheme = (preferredColorScheme.value.reduced.colorScheme?.asMaterialTheme() ?: defaultColorScheme?.asMaterialTheme() ?: MaterialTheme.colorScheme).sref()
        MaterialTheme(colorScheme = materialColorScheme) { ->
            val presentationBounds = remember { -> mutableStateOf(Rect.Zero) }
            val density = LocalDensity.current.sref()
            val layoutDirection = LocalLayoutDirection.current.sref()
            var rootModifier = Modifier
                .background(androidx.compose.ui.graphics.Color.Black)
                .fillMaxSize()
            if (systemBarEdges.contains(Edge.Set.leading)) {
                rootModifier = rootModifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
            }
            if (systemBarEdges.contains(Edge.Set.trailing)) {
                rootModifier = rootModifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End))
            }
            if (systemBarEdges.contains(Edge.Set.bottom)) {
                rootModifier = rootModifier.imePadding()
            }
            rootModifier = rootModifier.background(Color.background.colorImpl())
                .onGloballyPositionedInWindow { it -> presentationBounds.value = it }
            Box(modifier = rootModifier) l@{ ->
                if (presentationBounds.value == Rect.Zero) {
                    return@l
                }
                // Cannot get accurate WindowInsets until we're in the content box. We only check top and bottom
                // because we've padded the content to within horizontal safe insets already, mirroring standard
                // Android app behavior like e.g. Settings
                var (safeLeft, safeTop, safeRight, safeBottom) = presentationBounds.value.sref()
                if (systemBarEdges.contains(Edge.Set.top)) {
                    safeTop += WindowInsets.safeDrawing.getTop(density)
                }
                if (systemBarEdges.contains(Edge.Set.bottom)) {
                    safeBottom -= max(0, WindowInsets.safeDrawing.getBottom(density) - WindowInsets.ime.getBottom(density))
                }
                val safeBounds = Rect(left = safeLeft, top = safeTop, right = safeRight, bottom = safeBottom)
                val safeArea = SafeArea(presentation = presentationBounds.value, safe = safeBounds, absoluteSystemBars = systemBarEdges)
                EnvironmentValues.shared.setValues(l@{ it ->
                    // Detect whether the app is edge to edge mode based on whether we're padding horizontally (landscape)
                    // or we have a top/bttom safe area (portrait)
                    if (it._isEdgeToEdge == null) {
                        it.set_isEdgeToEdge(safeBounds != presentationBounds.value)
                    }
                    it.set_safeArea(safeArea)
                    return@l ComposeResult.ok
                }, in_ = { ->
                    Box(modifier = Modifier.fillMaxSize().padding(safeArea), contentAlignment = androidx.compose.ui.Alignment.Center.sref()) { -> content(context) }
                })
            }
        }
    }
}

@Composable
internal fun launchUIApplicationActivity() {
    // Modern Skip projects will set the launch activity in Main.kt. This function exists for older projects
    var context: Context? = LocalContext.current.sref()
    var activity: ComponentActivity? = null
    while (context != null) {
        val matchtarget_0 = context as? ComponentActivity
        if (matchtarget_0 != null) {
            val a = matchtarget_0
            activity = a.sref()
            break
        } else {
            val matchtarget_1 = context as? ContextWrapper
            if (matchtarget_1 != null) {
                val w = matchtarget_1
                context = w.baseContext.sref()
            } else {
                break
            }
        }
    }
    if (activity != null) {
        UIApplication.launch(activity)
    }
}

