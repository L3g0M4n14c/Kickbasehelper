package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@androidx.annotation.Keep
class Slider: View, Renderable, skip.lib.SwiftProjecting {
    internal val value: Binding<Double>
    internal val bounds: ClosedRange<Double>
    internal val step: Double?

    constructor(value: Binding<Double>, in_: Any? = null, step: Double? = null) {
        val bounds = in_
        this.value = value.sref()
        this.bounds = Companion.bounds(for_ = bounds)
        this.step = step
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(value: Binding<Double>, in_: Any? = null, step: Double? = null, onEditingChanged: (Boolean) -> Unit) {
        val bounds = in_
        this.value = value.sref()
        this.bounds = Companion.bounds(for_ = bounds)
        this.step = step
    }

    constructor(value: Binding<Double>, in_: Any? = null, step: Double? = null, label: () -> View): this(value = value, in_ = in_, step = step) {
    }

    constructor(getValue: () -> Double, setValue: (Double) -> Unit, min: Double, max: Double, step: Double?, bridgedLabel: View?) {
        this.value = Binding(get = getValue, set = setValue)
        this.bounds = min..max
        this.step = step
        // Note: label is ignored
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(value: Binding<Double>, in_: Any? = null, step: Double? = null, label: () -> View, onEditingChanged: (Boolean) -> Unit) {
        val bounds = in_
        this.value = value.sref()
        this.bounds = Companion.bounds(for_ = bounds)
        this.step = step
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(value: Binding<Double>, in_: Any? = null, step: Double? = null, label: () -> View, minimumValueLabel: () -> View, maximumValueLabel: () -> View, onEditingChanged: (Boolean) -> Unit = { _ ->  }) {
        val bounds = in_
        this.value = value.sref()
        this.bounds = Companion.bounds(for_ = bounds)
        this.step = step
    }

    @Composable
    override fun Render(context: ComposeContext) {
        var steps = 0
        if ((step != null) && (step > 0.0)) {
            steps = max(0, Int(ceil(bounds.endInclusive - bounds.start) / step) - 1)
        }
        val colors: SliderColors
        val matchtarget_0 = EnvironmentValues.shared._tint
        if (matchtarget_0 != null) {
            val tint = matchtarget_0
            val activeColor = tint.colorImpl()
            val disabledColor = activeColor.copy(alpha = ContentAlpha.disabled)
            colors = SliderDefaults.colors(thumbColor = activeColor, activeTrackColor = activeColor, disabledThumbColor = disabledColor, disabledActiveTrackColor = disabledColor)
        } else {
            colors = SliderDefaults.colors()
        }
        val modifier = Modifier.fillWidth().then(context.modifier)
        androidx.compose.material3.Slider(modifier = modifier, value = Float(value.get()), onValueChange = { it -> value.set(Double(it)) }, enabled = EnvironmentValues.shared.isEnabled, valueRange = Float(bounds.start)..Float(bounds.endInclusive), steps = steps, colors = colors)
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {

        private fun bounds(for_: Any?): ClosedRange<Double> {
            val bounds = for_
            val range_0 = bounds as? ClosedRange<*>
            if (range_0 == null) {
                return 0.0..1.0
            }
            return Double(range_0.start as kotlin.Number)..Double(range_0.endInclusive as kotlin.Number)
        }
    }
}

