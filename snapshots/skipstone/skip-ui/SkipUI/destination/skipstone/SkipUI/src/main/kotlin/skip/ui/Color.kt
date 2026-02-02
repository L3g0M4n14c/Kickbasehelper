package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@androidx.annotation.Keep
class Color: ShapeStyle, Renderable, skip.lib.SwiftProjecting {
    val colorImpl: @Composable () -> androidx.compose.ui.graphics.Color

    constructor(colorImpl: @Composable () -> androidx.compose.ui.graphics.Color) {
        this.colorImpl = colorImpl
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val animatable = colorImpl().asAnimatable(context = context)
        val modifier = context.modifier.background(animatable.value).fillSize()
        Box(modifier = modifier)
    }

    /// Return the equivalent Compose color.
    @Composable
    fun asComposeColor(): androidx.compose.ui.graphics.Color = colorImpl()

    // MARK: - ShapeStyle

    @Composable
    override fun asColor(opacity: Double, animationContext: ComposeContext?): androidx.compose.ui.graphics.Color? {
        val color = this.opacity(opacity).colorImpl()
        if (animationContext != null) {
            return color.asAnimatable(context = animationContext).value
        } else {
            return color
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(cgColor: Any) {
        colorImpl = { -> androidx.compose.ui.graphics.Color.White }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val cgColor: Any?
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun resolve(in_: Any): Color.Resolved {
        val environment = in_
        fatalError()
    }

    // MARK: -

    enum class RGBColorSpace {
        sRGB,
        sRGBLinear,
        displayP3;

        @androidx.annotation.Keep
        companion object {
        }
    }

    constructor(red: Double, green: Double, blue: Double, opacity: Double = 1.0) {
        colorImpl = { -> androidx.compose.ui.graphics.Color(red = Companion.clamp(red), green = Companion.clamp(green), blue = Companion.clamp(blue), alpha = Companion.clamp(opacity)) }
    }

    constructor(colorSpace: Color.RGBColorSpace, red: Double, green: Double, blue: Double, opacity: Double = 1.0): this(red = red, green = green, blue = blue, opacity = opacity) {
    }

    constructor(white: Double, opacity: Double = 1.0): this(red = white, green = white, blue = white, opacity = opacity) {
    }

    constructor(colorSpace: Color.RGBColorSpace, white: Double, opacity: Double = 1.0): this(white = white, opacity = opacity) {
    }

    constructor(hue: Double, saturation: Double, brightness: Double, opacity: Double = 1.0, unusedp: Any? = null) {
        colorImpl = { -> androidx.compose.ui.graphics.Color.hsv(hue = Companion.clamp(hue) * 360, saturation = Companion.clamp(saturation), value = Companion.clamp(brightness), alpha = Companion.clamp(opacity)) }
    }

    constructor(color: UIColor): this(red = color.red, green = color.green, blue = color.blue, opacity = color.alpha) {
    }

    constructor(uiColor: UIColor, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null): this(red = uiColor.red, green = uiColor.green, blue = uiColor.blue, opacity = uiColor.alpha) {
    }

    constructor(name: String, bundle: Bundle? = null) {
        val assetColorInfo: AssetColorInfo? = rememberCachedAsset(namedColorCache, AssetKey(name = name, bundle = bundle)) { _ -> assetColorInfo(name = name, bundle = bundle ?: Bundle.main) }
        colorImpl = { ->
            assetColorInfo?.colorImpl() ?: Color.gray.colorImpl()
        }
    }

    constructor(name: String, bridgedBundle: Any?, unusedp: Any? = null): this(name, bundle = bridgedBundle as? Bundle) {
    }

    fun headroom(headroom: Double?): Color = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun exposureAdjust(stops: Double): Color {
        fatalError()
    }

    // MARK: -

    @Suppress("MUST_BE_INITIALIZED")
    class Resolved: MutableStruct {
        var red: Float
            set(newValue) {
                willmutate()
                field = newValue
                didmutate()
            }
        var green: Float
            set(newValue) {
                willmutate()
                field = newValue
                didmutate()
            }
        var blue: Float
            set(newValue) {
                willmutate()
                field = newValue
                didmutate()
            }
        var opacity: Float
            set(newValue) {
                willmutate()
                field = newValue
                didmutate()
            }

        constructor(red: Float, green: Float, blue: Float, opacity: Float) {
            this.red = red
            this.green = green
            this.blue = blue
            this.opacity = opacity
        }

        constructor(colorSpace: Color.RGBColorSpace, red: Float, green: Float, blue: Float, opacity: Float): this(red = red, green = green, blue = blue, opacity = opacity) {
        }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val cgColor: Any
            get() {
                fatalError()
            }

        private constructor(copy: MutableStruct) {
            @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as Color.Resolved
            this.red = copy.red
            this.green = copy.green
            this.blue = copy.blue
            this.opacity = copy.opacity
        }

        override var supdate: ((Any) -> Unit)? = null
        override var smutatingcount = 0
        override fun scopy(): MutableStruct = Color.Resolved(this as MutableStruct)

        override fun equals(other: Any?): Boolean {
            if (other !is Color.Resolved) return false
            return red == other.red && green == other.green && blue == other.blue && opacity == other.opacity
        }

        override fun hashCode(): Int {
            var result = 1
            result = Hasher.combine(result, red)
            result = Hasher.combine(result, green)
            result = Hasher.combine(result, blue)
            result = Hasher.combine(result, opacity)
            return result
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    constructor(resolved: Color.Resolved): this(red = Double(resolved.red), green = Double(resolved.green), blue = Double(resolved.blue), opacity = Double(resolved.opacity)) {
    }


    // MARK: -

    override fun opacity(opacity: Double): Color {
        if (opacity == 1.0) {
            return this.sref()
        }
        return Color(colorImpl = l@{ ->
            val color = colorImpl()
            return@l color.copy(alpha = color.alpha * Float(opacity))
        })
    }

    fun saturate(by: Double): Color {
        val multiplier = by
        val colorImpl: @Composable () -> androidx.compose.ui.graphics.Color = { ->
            val color = colorImpl()
            val hsv = FloatArray(3)
            android.graphics.Color.RGBToHSV(Int(color.red * 255), Int(color.green * 255), Int(color.blue * 255), hsv)
            androidx.compose.ui.graphics.Color.hsv(hsv[0], min(1.0f, hsv[1] * Float(multiplier)), hsv[2], alpha = color.alpha)
        }
        return Color(colorImpl = colorImpl)
    }

    val gradient: AnyGradient
        get() {
            // Create a SwiftUI-like gradient by varying the saturation of this color
            val startColor = saturate(by = 0.75)
            val endColor = saturate(by = 1.33)
            return AnyGradient(gradient = Gradient(colors = arrayOf(startColor, endColor)))
        }

    override fun equals(other: Any?): Boolean {
        if (other !is Color) return false
        return colorImpl == other.colorImpl
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, colorImpl)
        return result
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: ShapeStyleCompanion {

        private fun clamp(value: Double): Float = max(0.0f, min(1.0f, Float(value)))

        // MARK: -

        val accentColor: Color
            get() {
                return Color(colorImpl = { -> MaterialTheme.colorScheme.primary })
            }
        @Composable
        internal fun assetAccentColor(colorScheme: ColorScheme): androidx.compose.ui.graphics.Color? {
            val name = "AccentColor"
            val colorInfo = rememberCachedAsset(namedColorCache, AssetKey(name = name, bundle = Bundle.main)) { _ -> assetColorInfo(name = name, bundle = Bundle.main) }
            return colorInfo?.colorImpl(colorScheme = colorScheme)
        }

        internal val background = Color(colorImpl = { -> MaterialTheme.colorScheme.surface })

        /// Matches Android's default bottom bar color.
        internal val systemBarBackground: Color = Color(colorImpl = { -> MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp) })

        /// Use for e.g. grouped table backgrounds, etc.
        internal val systemBackground: Color = systemBarBackground.sref()

        /// Use for overlays like alerts and action sheets.
        internal val overlayBackground: Color = Color(colorImpl = { -> MaterialTheme.colorScheme.surface.copy(alpha = 0.9f) })

        /// Use for separators, etc.
        internal var separator: Color = Color(colorImpl = { -> MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp) })
            get() = field.sref({ this.separator = it })
            set(newValue) {
                field = newValue.sref()
            }

        /// Use for placeholder content.
        internal val placeholderOpacity = 0.2

        /// Use for placeholder content.
        internal val placeholder: Color
            get() = _primary.opacity(placeholderOpacity)

        val _primary = Color(colorImpl = { -> MaterialTheme.colorScheme.onBackground })
        val _secondary = Color(colorImpl = { -> MaterialTheme.colorScheme.onBackground.copy(alpha = ContentAlpha.medium) })
        val _clear = Color(colorImpl = { -> androidx.compose.ui.graphics.Color.Transparent })
        val _white = Color(colorImpl = { -> androidx.compose.ui.graphics.Color.White })
        val _black = Color(colorImpl = { -> androidx.compose.ui.graphics.Color.Black })
        val _gray = Color(colorImpl = { -> ComposeColor(light = 0xFF8E8E93, dark = 0xFF8E8E93) })
        val _red = Color(colorImpl = { -> ComposeColor(light = 0xFFFF3B30, dark = 0xFFFF453A) })
        val _orange = Color(colorImpl = { -> ComposeColor(light = 0xFFFF9500, dark = 0xFFFF9F0A) })
        val _yellow = Color(colorImpl = { -> ComposeColor(light = 0xFFFFCC00, dark = 0xFFFFD60A) })
        val _green = Color(colorImpl = { -> ComposeColor(light = 0xFF34C759, dark = 0xFF30D158) })
        val _mint = Color(colorImpl = { -> ComposeColor(light = 0xFF00C7BE, dark = 0xFF63E6E2) })
        val _teal = Color(colorImpl = { -> ComposeColor(light = 0xFF30B0C7, dark = 0xFF40C8E0) })
        val _cyan = Color(colorImpl = { -> ComposeColor(light = 0xFF32ADE6, dark = 0xFF64D2FF) })
        val _blue = Color(colorImpl = { -> ComposeColor(light = 0xFF007AFF, dark = 0xFF0A84FF) })
        val _indigo = Color(colorImpl = { -> ComposeColor(light = 0xFF5856D6, dark = 0xFF5E5CE6) })
        val _purple = Color(colorImpl = { -> ComposeColor(light = 0xFFAF52DE, dark = 0xFFBF5AF2) })
        val _pink = Color(colorImpl = { -> ComposeColor(light = 0xFFFF2D55, dark = 0xFFFF375F) })
        val _brown = Color(colorImpl = { -> ComposeColor(light = 0xFFA2845E, dark = 0xFFAC8E68) })

        val primary: Color
            get() = Color._primary
        val secondary: Color
            get() = Color._secondary
        val clear: Color
            get() = Color._clear
        val white: Color
            get() = Color._white
        val black: Color
            get() = Color._black
        val gray: Color
            get() = Color._gray
        val red: Color
            get() = Color._red
        val orange: Color
            get() = Color._orange
        val yellow: Color
            get() = Color._yellow
        val green: Color
            get() = Color._green
        val mint: Color
            get() = Color._mint
        val teal: Color
            get() = Color._teal
        val cyan: Color
            get() = Color._cyan
        val blue: Color
            get() = Color._blue
        val indigo: Color
            get() = Color._indigo
        val purple: Color
            get() = Color._purple
        val pink: Color
            get() = Color._pink
        val brown: Color
            get() = Color._brown
    }
}

/// Returns the given color value based on whether the view is in dark mode or light mode.
@Composable
private fun ComposeColor(light: Long, dark: Long): androidx.compose.ui.graphics.Color {
    // TODO: EnvironmentValues.shared.colorMode == .dark ? dark : light
    return androidx.compose.ui.graphics.Color(if (isSystemInDarkTheme()) dark else light)
}


private val namedColorCache: Dictionary<AssetKey, AssetColorInfo?> = dictionaryOf()

private class AssetColorInfo {
    internal val light: ColorSet.ColorSpec?
    internal val dark: ColorSet.ColorSpec?

    /// The `ColorSet` that was loaded for the given info.
    internal val colorSet: ColorSet

    @Composable
    internal fun colorImpl(colorScheme: ColorScheme? = null): androidx.compose.ui.graphics.Color? {
        val colorScheme = colorScheme ?: EnvironmentValues.shared.colorScheme
        var color: androidx.compose.ui.graphics.Color? = null
        if ((colorScheme == ColorScheme.dark) && (dark != null)) {
            return dark.colorImpl()
        } else {
            return light?.colorImpl()
        }
    }

    constructor(light: ColorSet.ColorSpec? = null, dark: ColorSet.ColorSpec? = null, colorSet: ColorSet) {
        this.light = light
        this.dark = dark
        this.colorSet = colorSet
    }
}

private fun assetColorInfo(name: String, bundle: Bundle): AssetColorInfo? {
    for (dataURL in assetContentsURLs(name = "${name}.colorset", bundle = bundle)) {
        try {
            val data = Data(contentsOf = dataURL)
            logger.debug("loading colorset asset contents from: ${dataURL}")
            val colorSet = JSONDecoder().decode(ColorSet::class, from = data)
            val lightColor = colorSet.colors.sortedByAssetFit(colorScheme = ColorScheme.light).compactMap { it -> it.color }.last.sref()
            val darkColor = colorSet.colors.sortedByAssetFit(colorScheme = ColorScheme.dark).compactMap { it -> it.color }.last.sref()
            return AssetColorInfo(light = lightColor, dark = darkColor, colorSet = colorSet)
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            logger.warning("error loading color data from ${name}: ${error}")
        }
    }
    return null
}

/* The `Contents.json` in a `*.colorset` folder for a symbol
https://developer.apple.com/library/archive/documentation/Xcode/Reference/xcode_ref-Asset_Catalog_Format/Named_Color.html
{
"colors" : [
{
"color" : {
"platform" : "universal",
"reference" : "systemBlueColor"
},
"idiom" : "universal"
}
],
"info" : {
"author" : "xcode",
"version" : 1
}
}
*/
@androidx.annotation.Keep
private class ColorSet: Decodable {
    internal val colors: Array<ColorSet.ColorInfo>
    internal val info: AssetContentsInfo

    @androidx.annotation.Keep
    internal class ColorInfo: Decodable, AssetSortable {
        internal val color: ColorSet.ColorSpec?
        override val idiom: String? // e.g. "universal"
        override val appearances: Array<AssetAppearance>?

        constructor(color: ColorSet.ColorSpec? = null, idiom: String? = null, appearances: Array<AssetAppearance>? = null) {
            this.color = color
            this.idiom = idiom
            this.appearances = appearances.sref()
        }

        private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
            color("color"),
            idiom("idiom"),
            appearances("appearances");

            @androidx.annotation.Keep
            companion object {
                fun init(rawValue: String): CodingKeys? {
                    return when (rawValue) {
                        "color" -> CodingKeys.color
                        "idiom" -> CodingKeys.idiom
                        "appearances" -> CodingKeys.appearances
                        else -> null
                    }
                }
            }
        }

        constructor(from: Decoder) {
            val container = from.container(keyedBy = CodingKeys::class)
            this.color = container.decodeIfPresent(ColorSet.ColorSpec::class, forKey = CodingKeys.color)
            this.idiom = container.decodeIfPresent(String::class, forKey = CodingKeys.idiom)
            this.appearances = container.decodeIfPresent(Array::class, elementType = AssetAppearance::class, forKey = CodingKeys.appearances)
        }

        @androidx.annotation.Keep
        companion object: DecodableCompanion<ColorSet.ColorInfo> {
            override fun init(from: Decoder): ColorSet.ColorInfo = ColorInfo(from = from)

            private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
        }
    }

    @androidx.annotation.Keep
    internal class ColorSpec: Decodable {
        internal val platform: String? // e.g. "ios"
        internal val reference: String? // e.g. "systemBlueColor"
        internal val components: ColorSet.ColorComponents?

        @Composable
        internal fun colorImpl(): androidx.compose.ui.graphics.Color? {
            if (components != null) {
                return components.color.colorImpl()
            }
            when (reference) {
                "labelColor" -> return Color.primary.colorImpl()
                "secondaryLabelColor" -> return Color.secondary.colorImpl()
                "systemBlueColor" -> return Color.blue.colorImpl()
                "systemBrownColor" -> return Color.brown.colorImpl()
                "systemCyanColor" -> return Color.cyan.colorImpl()
                "systemGrayColor" -> return Color.gray.colorImpl()
                "systemGreenColor" -> return Color.green.colorImpl()
                "systemIndigoColor" -> return Color.indigo.colorImpl()
                "systemMintColor" -> return Color.mint.colorImpl()
                "systemOrangeColor" -> return Color.orange.colorImpl()
                "systemPinkColor" -> return Color.pink.colorImpl()
                "systemPurpleColor" -> return Color.purple.colorImpl()
                "systemRedColor" -> return Color.red.colorImpl()
                "systemTealColor" -> return Color.teal.colorImpl()
                "systemYellowColor" -> return Color.yellow.colorImpl()
                else -> return null
            }
        }

        constructor(platform: String? = null, reference: String? = null, components: ColorSet.ColorComponents? = null) {
            this.platform = platform
            this.reference = reference
            this.components = components
        }

        private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
            platform("platform"),
            reference("reference"),
            components("components");

            @androidx.annotation.Keep
            companion object {
                fun init(rawValue: String): CodingKeys? {
                    return when (rawValue) {
                        "platform" -> CodingKeys.platform
                        "reference" -> CodingKeys.reference
                        "components" -> CodingKeys.components
                        else -> null
                    }
                }
            }
        }

        constructor(from: Decoder) {
            val container = from.container(keyedBy = CodingKeys::class)
            this.platform = container.decodeIfPresent(String::class, forKey = CodingKeys.platform)
            this.reference = container.decodeIfPresent(String::class, forKey = CodingKeys.reference)
            this.components = container.decodeIfPresent(ColorSet.ColorComponents::class, forKey = CodingKeys.components)
        }

        @androidx.annotation.Keep
        companion object: DecodableCompanion<ColorSet.ColorSpec> {
            override fun init(from: Decoder): ColorSet.ColorSpec = ColorSpec(from = from)

            private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
        }
    }

    @androidx.annotation.Keep
    internal class ColorComponents: Decodable {
        internal val red: String?
        internal val green: String?
        internal val blue: String?
        internal val alpha: String?

        internal val color: Color
            get() {
                val redValue = Double(red ?: "") ?: 0.0
                val greenValue = Double(green ?: "") ?: 0.0
                val blueValue = Double(blue ?: "") ?: 0.0
                val alphaValue = Double(alpha ?: "") ?: 1.0
                return Color(red = redValue, green = greenValue, blue = blueValue, opacity = alphaValue)
            }

        constructor(red: String? = null, green: String? = null, blue: String? = null, alpha: String? = null) {
            this.red = red
            this.green = green
            this.blue = blue
            this.alpha = alpha
        }

        private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
            red("red"),
            green("green"),
            blue("blue"),
            alpha("alpha");

            @androidx.annotation.Keep
            companion object {
                fun init(rawValue: String): CodingKeys? {
                    return when (rawValue) {
                        "red" -> CodingKeys.red
                        "green" -> CodingKeys.green
                        "blue" -> CodingKeys.blue
                        "alpha" -> CodingKeys.alpha
                        else -> null
                    }
                }
            }
        }

        constructor(from: Decoder) {
            val container = from.container(keyedBy = CodingKeys::class)
            this.red = container.decodeIfPresent(String::class, forKey = CodingKeys.red)
            this.green = container.decodeIfPresent(String::class, forKey = CodingKeys.green)
            this.blue = container.decodeIfPresent(String::class, forKey = CodingKeys.blue)
            this.alpha = container.decodeIfPresent(String::class, forKey = CodingKeys.alpha)
        }

        @androidx.annotation.Keep
        companion object: DecodableCompanion<ColorSet.ColorComponents> {
            override fun init(from: Decoder): ColorSet.ColorComponents = ColorComponents(from = from)

            private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
        }
    }

    constructor(colors: Array<ColorSet.ColorInfo>, info: AssetContentsInfo) {
        this.colors = colors.sref()
        this.info = info
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        colors("colors"),
        info("info");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "colors" -> CodingKeys.colors
                    "info" -> CodingKeys.info
                    else -> null
                }
            }
        }
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.colors = container.decode(Array::class, elementType = ColorSet.ColorInfo::class, forKey = CodingKeys.colors)
        this.info = container.decode(AssetContentsInfo::class, forKey = CodingKeys.info)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<ColorSet> {
        override fun init(from: Decoder): ColorSet = ColorSet(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}


/*
#if canImport(CoreTransferable)
import protocol CoreTransferable.Transferable
import protocol CoreTransferable.TransferRepresentation

@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
extension Color : Transferable {

/// One group of colors–constant colors–created with explicitly specified
/// component values are transferred as is.
///
/// Another group of colors–standard colors, like `Color.mint`,
/// and semantic colors, like `Color.accentColor`–are rendered on screen
/// differently depending on the current ``SkipUI/Environment``. For transferring,
/// they are resolved against the default environment and might produce
/// a slightly different result at the destination if the source of drag
/// or copy uses a non-default environment.
@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
public static var transferRepresentation: some TransferRepresentation { get { return stubTransferRepresentation() } }

/// The type of the representation used to import and export the item.
///
/// Swift infers this type from the return value of the
/// ``transferRepresentation`` property.
//public typealias Representation = Never // some TransferRepresentation
}
#endif

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
extension Color.Resolved : Animatable {

/// The type defining the data to animate.
public typealias AnimatableData = AnimatablePair<Float, AnimatablePair<Float, AnimatablePair<Float, Float>>>

/// The data to animate.
public var animatableData: AnimatableData { get { fatalError() } set { } }
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
extension Color.Resolved : Codable {

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
