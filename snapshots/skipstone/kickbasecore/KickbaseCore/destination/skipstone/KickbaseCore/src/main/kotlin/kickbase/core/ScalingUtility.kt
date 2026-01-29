//
//  ScalingUtility.swift
//  Kickbasehelper
//
//  Created for macOS scaling improvements
//

package kickbase.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import skip.lib.*
import skip.lib.Set

import skip.ui.*
import skip.foundation.*
import skip.model.*

/// Appliziert plattformspezifische Skalierung
fun View.macOSScaled(): View = this.modifier(MacOSScalingModifier())

class MacOSScalingModifier: ViewModifier {
    internal var horizontalSizeClass: UserInterfaceSizeClass? = null

    constructor() {
    }

    internal val scaleFactor: Double
        get() = 1.0

    internal val fontScaleFactor: Double
        get() = 1.0

    override fun body(content: View): View {
        return ComposeBuilder l@{ composectx: ComposeContext ->
            val fontSize = 14.0

            return@l content
                .scaleEffect(scaleFactor)
                .font(Font.system(size = fontSize * fontScaleFactor)).Compose(composectx)
            ComposeResult.ok
        }
    }

    @Composable
    override fun Evaluate(content: View, context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        this.horizontalSizeClass = EnvironmentValues.shared.horizontalSizeClass

        return super.Evaluate(content, context, options)
    }

    @androidx.annotation.Keep
    companion object {
    }
}

fun View.adaptivePadding(edges: Edge.Set = Edge.Set.all, length: Double? = null): View {
    val paddingValue = length ?: 8.0

    return this.padding(edges, paddingValue)
}

fun View.adaptiveFont(style: Font.TextStyle): View = this.font(Font.system(style))

fun View.macOSOptimized(): View = this.modifier(MacOSOptimizationModifier())

class MacOSOptimizationModifier: ViewModifier {
    constructor() {
    }

    override fun body(content: View): View {
        return ComposeBuilder { composectx: ComposeContext -> content.Compose(composectx) }
    }

    @androidx.annotation.Keep
    companion object {
    }
}
