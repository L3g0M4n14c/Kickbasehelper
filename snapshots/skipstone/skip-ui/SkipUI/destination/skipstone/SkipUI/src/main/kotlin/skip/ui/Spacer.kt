package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// We use a class rather than struct to be able to mutate the `positionalMinLength` for layout.
@androidx.annotation.Keep
class Spacer: View, Renderable, skip.lib.SwiftProjecting {
    internal val minLength: Double?

    constructor(minLength: Double? = null) {
        this.minLength = minLength
    }

    /// When we layout an `HStack` or `VStack` we apply a positional min length to spacers between elements.
    internal var positionalMinLength: Double? = null

    @Composable
    override fun Render(context: ComposeContext) {
        val layoutImplementationVersion = EnvironmentValues.shared._layoutImplementationVersion
        val axis = EnvironmentValues.shared._layoutAxis
        val effectiveMinLength = minLength ?: positionalMinLength
        val minLengthFloat: Float? = if (effectiveMinLength != null && effectiveMinLength!! > 0.0) Float(effectiveMinLength!!) else null
        if (layoutImplementationVersion == 0) {
            // Maintain previous layout behavior for users who opt in
            if (minLengthFloat != null) {
                val minModifier: Modifier
                when (axis) {
                    Axis.horizontal -> minModifier = Modifier.width(minLengthFloat.dp)
                    Axis.vertical -> minModifier = Modifier.height(minLengthFloat.dp)
                    null -> minModifier = Modifier
                }
                androidx.compose.foundation.layout.Spacer(modifier = minModifier.then(context.modifier))
            }

            val fillModifier: Modifier
            when (axis) {
                Axis.horizontal -> fillModifier = EnvironmentValues.shared._flexibleWidth?.invoke(null, null, Float.flexibleSpace) ?: Modifier
                Axis.vertical -> fillModifier = EnvironmentValues.shared._flexibleHeight?.invoke(null, null, Float.flexibleSpace) ?: Modifier
                null -> fillModifier = Modifier
            }
            androidx.compose.foundation.layout.Spacer(modifier = fillModifier.then(context.modifier))
        } else {
            val modifier: Modifier
            when (axis) {
                Axis.horizontal -> {
                    val matchtarget_0 = EnvironmentValues.shared._flexibleWidth
                    if (matchtarget_0 != null) {
                        val flexibleWidth = matchtarget_0
                        modifier = flexibleWidth(null, minLengthFloat, Float.flexibleSpace)
                    } else {
                        modifier = Modifier
                    }
                }
                Axis.vertical -> {
                    val matchtarget_1 = EnvironmentValues.shared._flexibleHeight
                    if (matchtarget_1 != null) {
                        val flexibleHeight = matchtarget_1
                        modifier = flexibleHeight(null, minLengthFloat, Float.flexibleSpace)
                    } else {
                        modifier = Modifier
                    }
                }
                null -> modifier = Modifier.fillMaxSize()
            }
            androidx.compose.foundation.layout.Spacer(modifier = modifier.then(context.modifier))
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

enum class SpacerSizing(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
    flexible(1), // For bridging
    fixed(2); // For bridging

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: Int): SpacerSizing? {
            return when (rawValue) {
                1 -> SpacerSizing.flexible
                2 -> SpacerSizing.fixed
                else -> null
            }
        }
    }
}

fun SpacerSizing(rawValue: Int): SpacerSizing? = SpacerSizing.init(rawValue = rawValue)

