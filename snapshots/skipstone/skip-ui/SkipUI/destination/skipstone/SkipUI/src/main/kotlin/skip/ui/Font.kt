package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@androidx.annotation.Keep
class Font: skip.lib.SwiftProjecting {
    val fontImpl: @Composable () -> androidx.compose.ui.text.TextStyle

    constructor(fontImpl: @Composable () -> androidx.compose.ui.text.TextStyle) {
        this.fontImpl = fontImpl
    }

    /// Return the equivalent Compose text style.
    @Composable
    fun asComposeTextStyle(): androidx.compose.ui.text.TextStyle = fontImpl()

    @androidx.annotation.Keep
    enum class TextStyle(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, Codable, RawRepresentable<Int> {
        largeTitle(0), // For bridging
        title(1), // For bridging
        title2(2), // For bridging
        title3(3), // For bridging
        headline(4), // For bridging
        subheadline(5), // For bridging
        body(6), // For bridging
        callout(7), // For bridging
        footnote(8), // For bridging
        caption(9), // For bridging
        caption2(10); // For bridging

        override fun encode(to: Encoder) {
            val container = to.singleValueContainer()
            container.encode(rawValue)
        }

        @androidx.annotation.Keep
        companion object: CaseIterableCompanion<Font.TextStyle>, DecodableCompanion<Font.TextStyle> {
            override fun init(from: Decoder): Font.TextStyle = TextStyle(from = from)

            fun init(rawValue: Int): Font.TextStyle? {
                return when (rawValue) {
                    0 -> TextStyle.largeTitle
                    1 -> TextStyle.title
                    2 -> TextStyle.title2
                    3 -> TextStyle.title3
                    4 -> TextStyle.headline
                    5 -> TextStyle.subheadline
                    6 -> TextStyle.body
                    7 -> TextStyle.callout
                    8 -> TextStyle.footnote
                    9 -> TextStyle.caption
                    10 -> TextStyle.caption2
                    else -> null
                }
            }

            override val allCases: Array<Font.TextStyle>
                get() = arrayOf(largeTitle, title, title2, title3, headline, subheadline, body, callout, footnote, caption, caption2)
        }
    }


    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(font: Any) {
        fontImpl = { -> MaterialTheme.typography.bodyMedium }
    }

    fun italic(isActive: Boolean = true): Font {
        return Font(fontImpl = { -> fontImpl().copy(fontStyle = if (isActive) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal) })
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun smallCaps(isActive: Boolean = true): Font {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun lowercaseSmallCaps(isActive: Boolean = true): Font {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun uppercaseSmallCaps(isActive: Boolean = true): Font {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun monospacedDigit(isActive: Boolean = true): Font {
        fatalError()
    }

    fun weight(weight: Font.Weight): Font {
        return Font(fontImpl = { -> fontImpl().copy(fontWeight = Companion.fontWeight(for_ = weight)) })
    }

    fun weight(bridgedWeight: Int): Font = weight(Font.Weight(value = bridgedWeight))


    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun width(width: Font.Width): Font {
        fatalError()
    }

    fun bold(isActive: Boolean = true): Font = weight(if (isActive) Weight.bold else Weight.regular)

    fun monospaced(isActive: Boolean = true): Font = design(if (isActive) Design.monospaced else Design.default)

    fun design(design: Font.Design?): Font {
        return Font(fontImpl = { -> fontImpl().copy(fontFamily = Companion.fontFamily(for_ = design)) })
    }

    fun design(bridgedValue: Int): Font = design(Design(rawValue = bridgedValue))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun leading(leading: Font.Leading): Font {
        fatalError()
    }

    fun pointSize(size: Double): Font {
        return Font(fontImpl = { -> fontImpl().copy(fontSize = size.sp) })
    }

    fun scaledBy(factor: Double): Font {
        return Font(fontImpl = l@{ ->
            val textStyle = fontImpl()
            val pointSize = textStyle.fontSize.value * factor
            return@l textStyle.copy(fontSize = pointSize.sp)
        })
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun resolve(in_: Font.Context): Font.Resolved {
        val context = in_
        fatalError()
    }

    class Context: CustomDebugStringConvertible {
        override val debugDescription: String
            get() = "Font.Context"

        override fun equals(other: Any?): Boolean = other is Font.Context

        override fun hashCode(): Int = "Font.Context".hashCode()

        @androidx.annotation.Keep
        companion object {
        }
    }

    class Resolved {
        val ctFont: Int /* CTFont */
        val isBold: Boolean
        val isItalic: Boolean
        val pointSize: Double
        val weight: Font.Weight
        val width: Font.Width
        val leading: Font.Leading
        val isMonospaced: Boolean
        val isLowercaseSmallCaps: Boolean
        val isUppercaseSmallCaps: Boolean
        val isSmallCaps: Boolean

        constructor(ctFont: Int, isBold: Boolean, isItalic: Boolean, pointSize: Double, weight: Font.Weight, width: Font.Width, leading: Font.Leading, isMonospaced: Boolean, isLowercaseSmallCaps: Boolean, isUppercaseSmallCaps: Boolean, isSmallCaps: Boolean) {
            this.ctFont = ctFont
            this.isBold = isBold
            this.isItalic = isItalic
            this.pointSize = pointSize
            this.weight = weight
            this.width = width.sref()
            this.leading = leading
            this.isMonospaced = isMonospaced
            this.isLowercaseSmallCaps = isLowercaseSmallCaps
            this.isUppercaseSmallCaps = isUppercaseSmallCaps
            this.isSmallCaps = isSmallCaps
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Font.Resolved) return false
            return ctFont == other.ctFont && isBold == other.isBold && isItalic == other.isItalic && pointSize == other.pointSize && weight == other.weight && width == other.width && leading == other.leading && isMonospaced == other.isMonospaced && isLowercaseSmallCaps == other.isLowercaseSmallCaps && isUppercaseSmallCaps == other.isUppercaseSmallCaps && isSmallCaps == other.isSmallCaps
        }

        override fun hashCode(): Int {
            var result = 1
            result = Hasher.combine(result, ctFont)
            result = Hasher.combine(result, isBold)
            result = Hasher.combine(result, isItalic)
            result = Hasher.combine(result, pointSize)
            result = Hasher.combine(result, weight)
            result = Hasher.combine(result, width)
            result = Hasher.combine(result, leading)
            result = Hasher.combine(result, isMonospaced)
            result = Hasher.combine(result, isLowercaseSmallCaps)
            result = Hasher.combine(result, isUppercaseSmallCaps)
            result = Hasher.combine(result, isSmallCaps)
            return result
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    class Weight {
        internal val value: Int

        internal constructor(value: Int) {
            this.value = value
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Font.Weight) return false
            return value == other.value
        }

        override fun hashCode(): Int {
            var result = 1
            result = Hasher.combine(result, value)
            return result
        }

        @androidx.annotation.Keep
        companion object {
            val ultraLight = Weight(value = -3) // For bridging (-0.8)
            val thin = Weight(value = -2) // For bridging (-0.6)
            val light = Weight(value = -1) // For bridging (-0.4)
            val regular = Weight(value = 0) // For bridging (0.0)
            val medium = Weight(value = 1) // For bridging (0.23)
            val semibold = Weight(value = 2) // For bridging (0.3)
            val bold = Weight(value = 3) // For bridging (0.4)
            val heavy = Weight(value = 4) // For bridging (0.56)
            val black = Weight(value = 5) // For bridging (0.62)
        }
    }

    @Suppress("MUST_BE_INITIALIZED")
    class Width: MutableStruct {
        var value: Double
            set(newValue) {
                willmutate()
                field = newValue
                didmutate()
            }

        constructor(value: Double) {
            this.value = value
        }

        private constructor(copy: MutableStruct) {
            @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as Font.Width
            this.value = copy.value
        }

        override var supdate: ((Any) -> Unit)? = null
        override var smutatingcount = 0
        override fun scopy(): MutableStruct = Font.Width(this as MutableStruct)

        override fun equals(other: Any?): Boolean {
            if (other !is Font.Width) return false
            return value == other.value
        }

        override fun hashCode(): Int {
            var result = 1
            result = Hasher.combine(result, value)
            return result
        }

        @androidx.annotation.Keep
        companion object {

            val compressed = Width(0.8)
            val condensed = Width(0.9)
            val standard = Width(1.0)
            val expanded = Width(1.2)
        }
    }

    enum class Leading {
        standard,
        tight,
        loose;

        @androidx.annotation.Keep
        companion object {
        }
    }

    enum class Design(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
        default(0), // For bridging
        serif(1), // For bridging
        rounded(2), // For bridging
        monospaced(3); // For bridging

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: Int): Font.Design? {
                return when (rawValue) {
                    0 -> Design.default
                    1 -> Design.serif
                    2 -> Design.rounded
                    3 -> Design.monospaced
                    else -> null
                }
            }
        }
    }


    override fun equals(other: Any?): Boolean {
        if (other !is Font) return false
        return fontImpl == other.fontImpl
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, fontImpl)
        return result
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {

        // M3: Default Font Size/Line Height
        // displayLarge: Roboto 57/64
        // displayMedium: Roboto 45/52
        // displaySmall: Roboto 36/44
        // headlineLarge: Roboto 32/40
        // headlineMedium: Roboto 28/36
        // headlineSmall: Roboto 24/32
        // titleLarge: New-Roboto Medium 22/28
        // titleMedium: Roboto Medium 16/24
        // titleSmall: Roboto Medium 14/20
        // bodyLarge: Roboto 16/24
        // bodyMedium: Roboto 14/20
        // bodySmall: Roboto 12/16
        // labelLarge: Roboto Medium 14/20
        // labelMedium: Roboto Medium 12/16
        // labelSmall: New Roboto Medium 11/16

        // manual offsets are applied to the default font sizes to get them to line up with SwiftUI default sizes; see TextTests.swift

        val largeTitle = Font(fontImpl = { -> adjust(MaterialTheme.typography.titleLarge, by = Float(+9.0 + 1.0)) })

        val title = Font(fontImpl = { -> adjust(MaterialTheme.typography.headlineMedium, by = Float(-2.0)) })

        val title2 = Font(fontImpl = { -> adjust(MaterialTheme.typography.headlineSmall, by = Float(-5.0 + 1.0)) })

        val title3 = Font(fontImpl = { -> adjust(MaterialTheme.typography.headlineSmall, by = Float(-6.0)) })

        val headline = Font(fontImpl = { -> adjust(MaterialTheme.typography.titleMedium, by = 0.0f) })

        val subheadline = Font(fontImpl = { -> adjust(MaterialTheme.typography.titleSmall, by = 0.0f) })

        val body = Font(fontImpl = { -> adjust(MaterialTheme.typography.bodyLarge, by = 0.0f) })

        val callout = Font(fontImpl = { -> adjust(MaterialTheme.typography.bodyMedium, by = Float(+1.0)) })

        val footnote = Font(fontImpl = { -> adjust(MaterialTheme.typography.bodySmall, by = Float(+0.0)) })

        val caption = Font(fontImpl = { -> adjust(MaterialTheme.typography.bodySmall, by = Float(-0.75)) })

        val caption2 = Font(fontImpl = { -> adjust(MaterialTheme.typography.bodySmall, by = Float(-1.0)) })

        private fun adjust(style: androidx.compose.ui.text.TextStyle, by: Float): androidx.compose.ui.text.TextStyle {
            val amount = by
            if (amount == 0.0f) {
                return style
            }
            val fontSize = (style.fontSize.value + amount).sp.sref()
            val lineHeight = (if (style.lineHeight == TextUnit.Unspecified) style.lineHeight else (style.lineHeight.value + amount).sp).sref()
            return style.copy(fontSize = fontSize, lineHeight = lineHeight)
        }

        fun system(style: Font.TextStyle, design: Font.Design? = null, weight: Font.Weight? = null): Font {
            val font: Font
            when (style) {
                Font.TextStyle.largeTitle -> font = Font.largeTitle.sref()
                Font.TextStyle.title -> font = Font.title.sref()
                Font.TextStyle.title2 -> font = Font.title2.sref()
                Font.TextStyle.title3 -> font = Font.title3.sref()
                Font.TextStyle.headline -> font = Font.headline.sref()
                Font.TextStyle.subheadline -> font = Font.subheadline.sref()
                Font.TextStyle.body -> font = Font.body.sref()
                Font.TextStyle.callout -> font = Font.callout.sref()
                Font.TextStyle.footnote -> font = Font.footnote.sref()
                Font.TextStyle.caption -> font = Font.caption.sref()
                Font.TextStyle.caption2 -> font = Font.caption2.sref()
            }
            if (weight == null && design == null) {
                return font.sref()
            }
            return Font(fontImpl = { -> font.fontImpl().copy(fontWeight = fontWeight(for_ = weight), fontFamily = fontFamily(for_ = design)) })
        }

        fun system(bridgedStyle: Int, bridgedDesign: Int?, bridgedWeight: Int?): Font {
            val style = Font.TextStyle(rawValue = bridgedStyle) ?: Font.TextStyle.body
            val design = if (bridgedDesign == null) null else Font.Design(rawValue = bridgedDesign!!)
            val weight = if (bridgedWeight == null) null else Font.Weight(value = bridgedWeight!!)
            return system(style, design = design, weight = weight)
        }

        fun system(size: Double, weight: Font.Weight? = null, design: Font.Design? = null): Font {
            return Font(fontImpl = { -> androidx.compose.ui.text.TextStyle(fontSize = size.sp, fontWeight = fontWeight(for_ = weight), fontFamily = fontFamily(for_ = design)) })
        }

        fun system(size: Double, bridgedDesign: Int?, bridgedWeight: Int?): Font {
            val design = if (bridgedDesign == null) null else Font.Design(rawValue = bridgedDesign!!)
            val weight = if (bridgedWeight == null) null else Font.Weight(value = bridgedWeight!!)
            return system(size = size, weight = weight, design = design)
        }
        // Cache is used not only to avoid expense of recreating font families, but also because recreated families for
        // the same name do not compare equal, causing recompositions under some configs:
        // https://github.com/skiptools/skip/issues/399
        private var fontFamilyCache: Dictionary<String, FontFamily> = dictionaryOf()
            get() = field.sref({ this.fontFamilyCache = it })
            set(newValue) {
                field = newValue.sref()
            }

        private fun findNamedFont(fontName: String, ctx: android.content.Context): FontFamily? {
            var fontFamily: FontFamily? = null
            synchronized(fontFamilyCache) { -> fontFamily = fontFamilyCache[fontName].sref() }
            if (fontFamily != null) {
                return fontFamily.sref()
            }

            // Android font names are lowercased and separated by "_" characters, since Android resource names can take only alphanumeric characters.
            // Font lookups on Android reference the font's filename, whereas SwiftUI references the font's Postscript name
            // So the best way to have the same font lookup code work on both platforms is to name the
            // font with PS name "Some Poscript Font-Bold" as "some_postscript_font_bold.ttf", and then both iOS and Android
            // can reference it by the postscript name
            val name = fontName.lowercased().replace(" ", "_").replace("-", "_")

            //android.util.Log.i("SkipUI", "finding font: \(name)")

            // look up the font in the resource bundle for custom embedded fonts
            val fid = ctx.resources.getIdentifier(name, "font", ctx.packageName)
            if (fid == 0) {
                // try to fall back on system installed fonts like "courier"
                val matchtarget_0 = Typeface.create(name, Typeface.NORMAL)
                if (matchtarget_0 != null) {
                    val typeface = matchtarget_0
                    //android.util.Log.i("SkipUI", "found font: \(typeface)")
                    fontFamily = FontFamily(typeface)
                } else {
                    android.util.Log.w("SkipUI", "unable to find font named: ${fontName} (${name})")
                }
            } else {
                val matchtarget_1 = ctx.resources.getFont(fid)
                if (matchtarget_1 != null) {
                    val customTypeface = matchtarget_1
                    fontFamily = FontFamily(customTypeface)
                } else {
                    android.util.Log.w("SkipUI", "unable to find font named: ${name}")
                }
            }
            if (fontFamily != null) {
                synchronized(fontFamilyCache) { -> fontFamilyCache[fontName] = fontFamily.sref() }
            }
            return fontFamily.sref()
        }

        fun custom(name: String, size: Double): Font {
            return Font(fontImpl = { -> androidx.compose.ui.text.TextStyle(fontFamily = Companion.findNamedFont(name, ctx = LocalContext.current), fontSize = size.sp) })
        }

        fun custom(name: String, size: Double, relativeTo: Font.TextStyle): Font {
            val textStyle = relativeTo
            val systemFont = system(textStyle)
            return Font(fontImpl = { ->
                val absoluteSize = systemFont.fontImpl().fontSize.value + size
                androidx.compose.ui.text.TextStyle(fontFamily = Companion.findNamedFont(name, ctx = LocalContext.current), fontSize = absoluteSize.sp)
            })
        }

        fun custom(name: String, size: Double, bridgedRelativeTo: Int): Font {
            val textStyle = bridgedRelativeTo
            return custom(name, size = size, relativeTo = Font.TextStyle(rawValue = textStyle) ?: Font.TextStyle.body)
        }

        fun custom(name: String, fixedSize: Double, unusedp: Any? = null): Font = Font.custom(name, size = fixedSize)

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val default: Font
            get() {
                fatalError()
            }
        private fun fontWeight(for_: Font.Weight?): FontWeight? {
            val weight = for_
            when (weight) {
                null -> return null
                Font.Weight.ultraLight -> return FontWeight.Thin.sref()
                Font.Weight.thin -> return FontWeight.ExtraLight.sref()
                Font.Weight.light -> return FontWeight.Light.sref()
                Font.Weight.regular -> return FontWeight.Normal.sref()
                Font.Weight.medium -> return FontWeight.Medium.sref()
                Font.Weight.semibold -> return FontWeight.SemiBold.sref()
                Font.Weight.bold -> return FontWeight.Bold.sref()
                Font.Weight.heavy -> return FontWeight.ExtraBold.sref()
                Font.Weight.black -> return FontWeight.Black.sref()
                else -> return FontWeight.Normal.sref()
            }
        }
        private fun fontFamily(for_: Font.Design?): FontFamily? {
            val design = for_
            when (design) {
                null -> return null
                Font.Design.default -> return FontFamily.Default.sref()
                Font.Design.serif -> return FontFamily.Serif.sref()
                Font.Design.rounded -> return FontFamily.Cursive.sref()
                Font.Design.monospaced -> return FontFamily.Monospace.sref()
            }
        }

        fun TextStyle(from: Decoder): Font.TextStyle {
            val container = from.singleValueContainer()
            val rawValue = container.decode(Int::class)
            return TextStyle(rawValue = rawValue) ?: throw ErrorException(cause = NullPointerException())
        }

        fun TextStyle(rawValue: Int): Font.TextStyle? = TextStyle.init(rawValue = rawValue)

        fun Design(rawValue: Int): Font.Design? = Design.init(rawValue = rawValue)
    }
}

enum class LegibilityWeight {
    regular,
    bold;

    @androidx.annotation.Keep
    companion object {
    }
}

