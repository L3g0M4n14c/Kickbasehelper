package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

internal open class ColorInvertModifier: DrawModifier {
    override fun ContentDrawScope.draw() {
        val invertMatrix = ColorMatrix().apply { -> setToInvert() }
        val invertFilter = ColorFilter.colorMatrix(invertMatrix)
        val paint = Paint().apply { -> colorFilter = invertFilter.sref() }
        drawIntoCanvas { it ->
            it.saveLayer(Rect(0.0f, 0.0f, size.width, size.height), paint)
            drawContent()
            it.restore()
        }
    }
}

internal fun ColorMatrix.setToInvert(): Unit = set(ColorMatrix(floatArrayOf(Float(-1), 0f, 0f, 0f, 255f, 0f, Float(-1), 0f, 0f, 255f, 0f, 0f, Float(-1), 0f, 255f, 0f, 0f, 0f, 1f, 0f)))

internal open class GrayscaleModifier: DrawModifier {
    internal val amount: Double

    internal constructor(amount: Double) {
        this.amount = amount
    }

    override fun ContentDrawScope.draw() {
        val saturationMatrix = ColorMatrix().apply { -> setToSaturation(Float(max(0.0, 1.0 - amount))) }
        val saturationFilter = ColorFilter.colorMatrix(saturationMatrix)
        val paint = Paint().apply { -> colorFilter = saturationFilter.sref() }
        drawIntoCanvas { it ->
            it.saveLayer(Rect(0.0f, 0.0f, size.width, size.height), paint)
            drawContent()
            it.restore()
        }
    }
}

internal open class BrightnessModifier: DrawModifier {
    internal val amount: Double

    internal constructor(amount: Double) {
        this.amount = amount
    }

    override fun ContentDrawScope.draw() {
        // Brightness adjustment: shift RGB values by amount * 255
        // amount: -1.0 (black) to 1.0 (white), 0.0 = no change
        val shift = Float(amount * 255.0)
        val brightnessMatrix = ColorMatrix(floatArrayOf(1f, 0f, 0f, 0f, shift, 0f, 1f, 0f, 0f, shift, 0f, 0f, 1f, 0f, shift, 0f, 0f, 0f, 1f, 0f))
        val brightnessFilter = ColorFilter.colorMatrix(brightnessMatrix)
        val paint = Paint().apply { -> colorFilter = brightnessFilter.sref() }
        drawIntoCanvas { it ->
            it.saveLayer(Rect(0.0f, 0.0f, size.width, size.height), paint)
            drawContent()
            it.restore()
        }
    }
}

internal open class ContrastModifier: DrawModifier {
    internal val amount: Double

    internal constructor(amount: Double) {
        this.amount = amount
    }

    override fun ContentDrawScope.draw() {
        // Contrast adjustment: scale RGB around 0.5 (128)
        // amount: 0.0 = gray, 1.0 = no change, >1.0 = increased contrast
        val scale = Float(amount)
        val translate = Float((1.0 - amount) * 127.5)
        val contrastMatrix = ColorMatrix(floatArrayOf(scale, 0f, 0f, 0f, translate, 0f, scale, 0f, 0f, translate, 0f, 0f, scale, 0f, translate, 0f, 0f, 0f, 1f, 0f))
        val contrastFilter = ColorFilter.colorMatrix(contrastMatrix)
        val paint = Paint().apply { -> colorFilter = contrastFilter.sref() }
        drawIntoCanvas { it ->
            it.saveLayer(Rect(0.0f, 0.0f, size.width, size.height), paint)
            drawContent()
            it.restore()
        }
    }
}

internal open class SaturationModifier: DrawModifier {
    internal val amount: Double

    internal constructor(amount: Double) {
        this.amount = amount
    }

    override fun ContentDrawScope.draw() {
        // Saturation: 0.0 = grayscale, 1.0 = no change, >1.0 = oversaturated
        val saturationMatrix = ColorMatrix().apply { -> setToSaturation(Float(amount)) }
        val saturationFilter = ColorFilter.colorMatrix(saturationMatrix)
        val paint = Paint().apply { -> colorFilter = saturationFilter.sref() }
        drawIntoCanvas { it ->
            it.saveLayer(Rect(0.0f, 0.0f, size.width, size.height), paint)
            drawContent()
            it.restore()
        }
    }
}

internal open class HueRotationModifier: DrawModifier {
    internal val degrees: Double

    internal constructor(degrees: Double) {
        this.degrees = degrees
    }

    override fun ContentDrawScope.draw() {
        // Hue rotation using ColorMatrix
        val radians = Float(degrees * Double.pi / 180.0)
        val cos = kotlin.math.cos(radians)
        val sin = kotlin.math.sin(radians)

        // Hue rotation matrix derived from rotation around the (1,1,1) axis in RGB space
        val lumR = 0.213f
        val lumG = 0.715f
        val lumB = 0.072f

        val hueMatrix = ColorMatrix(floatArrayOf(lumR + cos * (1f - lumR) + sin * (-lumR), lumG + cos * (-lumG) + sin * (-lumG), lumB + cos * (-lumB) + sin * (1f - lumB), 0f, 0f, lumR + cos * (-lumR) + sin * 0.143f, lumG + cos * (1f - lumG) + sin * 0.140f, lumB + cos * (-lumB) + sin * Float(-0.283), 0f, 0f, lumR + cos * (-lumR) + sin * (-(1f - lumR)), lumG + cos * (-lumG) + sin * (lumG), lumB + cos * (1f - lumB) + sin * (lumB), 0f, 0f, 0f, 0f, 0f, 1f, 0f))
        val hueFilter = ColorFilter.colorMatrix(hueMatrix)
        val paint = Paint().apply { -> colorFilter = hueFilter.sref() }
        drawIntoCanvas { it ->
            it.saveLayer(Rect(0.0f, 0.0f, Float(size.width), Float(size.height)), paint)
            drawContent()
            it.restore()
        }
    }
}

internal open class ColorMultiplyModifier: DrawModifier {
    internal val color: androidx.compose.ui.graphics.Color

    internal constructor(color: androidx.compose.ui.graphics.Color) {
        this.color = color
    }

    override fun ContentDrawScope.draw() {
        // Color multiply: multiply each channel by the corresponding color channel
        val r = color.red.sref()
        val g = color.green.sref()
        val b = color.blue.sref()
        val a = color.alpha.sref()
        val multiplyMatrix = ColorMatrix(floatArrayOf(r, 0f, 0f, 0f, 0f, 0f, g, 0f, 0f, 0f, 0f, 0f, b, 0f, 0f, 0f, 0f, 0f, a, 0f))
        val multiplyFilter = ColorFilter.colorMatrix(multiplyMatrix)
        val paint = Paint().apply { -> colorFilter = multiplyFilter.sref() }
        drawIntoCanvas { it ->
            it.saveLayer(Rect(0.0f, 0.0f, size.width, size.height), paint)
            drawContent()
            it.restore()
        }
    }
}
