package kickbase.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import skip.lib.*

import skip.ui.*
import skip.foundation.*
import skip.model.*


internal val Color.Companion.systemBackgroundCompat: Color
    get() = Color.white

internal val Color.Companion.secondarySystemBackgroundCompat: Color
    get() = Color(white = 0.95)

internal val Color.Companion.systemGray6Compat: Color
    get() = Color.gray

internal val Color.Companion.systemGray5Compat: Color
    get() = Color.gray

internal val Color.Companion.systemGroupedBackgroundCompat: Color
    get() = Color.white

internal val ToolbarItemPlacement.Companion.navigationBarTrailingCompat: ToolbarItemPlacement
    get() = ToolbarItemPlacement.primaryAction

internal val ToolbarItemPlacement.Companion.navigationBarLeadingCompat: ToolbarItemPlacement
    get() = ToolbarItemPlacement.cancellationAction

internal fun isLargeScreen(): Boolean = false
