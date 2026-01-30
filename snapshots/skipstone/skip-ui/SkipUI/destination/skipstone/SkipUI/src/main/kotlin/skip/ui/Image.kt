package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.sharp.*
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest

@androidx.annotation.Keep
class Image: View, Renderable, MutableStruct, skip.lib.SwiftProjecting {
    internal val image: Image.ImageType
    internal var resizingMode: Image.ResizingMode? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var templateRenderingMode: Image.TemplateRenderingMode? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    internal val scale: Double

    internal sealed class ImageType {
        class NamedCase(val associated0: String, val associated1: Bundle?, val associated2: Text?): ImageType() {
            val name = associated0
            val bundle = associated1
            val label = associated2

            override fun equals(other: Any?): Boolean {
                if (other !is NamedCase) return false
                return associated0 == other.associated0 && associated1 == other.associated1 && associated2 == other.associated2
            }
        }
        class DecorativeCase(val associated0: String, val associated1: Bundle?): ImageType() {
            val name = associated0
            val bundle = associated1

            override fun equals(other: Any?): Boolean {
                if (other !is DecorativeCase) return false
                return associated0 == other.associated0 && associated1 == other.associated1
            }
        }
        class SystemCase(val associated0: String): ImageType() {
            val systemName = associated0

            override fun equals(other: Any?): Boolean {
                if (other !is SystemCase) return false
                return associated0 == other.associated0
            }
        }
        class BitmapCase(val associated0: Bitmap, val associated1: Double): ImageType() {
            val bitmap = associated0
            val scale = associated1

            override fun equals(other: Any?): Boolean {
                if (other !is BitmapCase) return false
                return associated0 == other.associated0 && associated1 == other.associated1
            }
        }
        class PainterCase(val associated0: Painter, val associated1: Double): ImageType() {
            val painter = associated0
            val scale = associated1

            override fun equals(other: Any?): Boolean {
                if (other !is PainterCase) return false
                return associated0 == other.associated0 && associated1 == other.associated1
            }
        }

        @androidx.annotation.Keep
        companion object {
            fun named(name: String, bundle: Bundle?, label: Text?): ImageType = NamedCase(name, bundle, label)
            fun decorative(name: String, bundle: Bundle?): ImageType = DecorativeCase(name, bundle)
            fun system(systemName: String): ImageType = SystemCase(systemName)
            fun bitmap(bitmap: Bitmap, scale: Double): ImageType = BitmapCase(bitmap, scale)
            fun painter(painter: Painter, scale: Double): ImageType = PainterCase(painter, scale)
        }
    }

    constructor(name: String, bundle: Bundle? = null, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) {
        this.scale = 1.0
        this.image = Image.ImageType.named(name = name, bundle = bundle, label = null)
    }

    constructor(name: String, bundle: Bundle? = null, label: Text) {
        this.scale = 1.0
        this.image = Image.ImageType.named(name = name, bundle = bundle, label = label)
    }

    constructor(decorative: String, bundle: Bundle? = null) {
        val name = decorative
        this.scale = 1.0
        this.image = Image.ImageType.decorative(name = name, bundle = bundle)
    }

    constructor(systemName: String, unusedp0: Any? = null, unusedp1: Any? = null) {
        this.scale = 1.0
        this.image = Image.ImageType.system(systemName = systemName)
    }

    constructor(name: String, isSystem: Boolean, isDecorative: Boolean, bridgedBundle: Any?, label: Text?) {
        this.scale = 1.0
        if (isSystem) {
            this.image = Image.ImageType.system(systemName = name)
        } else if (isDecorative) {
            this.image = Image.ImageType.decorative(name = name, bundle = bridgedBundle as? Bundle)
        } else {
            this.image = Image.ImageType.named(name = name, bundle = bridgedBundle as? Bundle, label = label)
        }
    }

    constructor(uiImage: UIImage) {
        this.scale = 1.0
        this.image = Image.ImageType.bitmap(bitmap = uiImage.bitmap!!, scale = uiImage.scale)
    }

    constructor(painter: Painter, scale: Double) {
        this.scale = 1.0
        this.image = Image.ImageType.painter(painter = painter, scale = scale)
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val aspect = EnvironmentValues.shared._aspectRatio
        val colorScheme = EnvironmentValues.shared.colorScheme

        // Put given modifiers on the containing Box so that the image can scale itself without affecting them
        Box(modifier = context.modifier, contentAlignment = androidx.compose.ui.Alignment.Center) { ->
            val matchtarget_0 = image
            when (matchtarget_0) {
                is Image.ImageType.BitmapCase -> {
                    val bitmap = matchtarget_0.associated0
                    val scale = matchtarget_0.associated1
                    RenderBitmap(bitmap = bitmap, scale = scale, aspectRatio = aspect?.element0, contentMode = aspect?.element1, context = context)
                }
                is Image.ImageType.PainterCase -> {
                    val painter = matchtarget_0.associated0
                    val scale = matchtarget_0.associated1
                    RenderPainter(painter = painter, scale = scale, aspectRatio = aspect?.element0, contentMode = aspect?.element1, context = context)
                }
                is Image.ImageType.SystemCase -> {
                    val systemName = matchtarget_0.associated0
                    RenderSystem(systemName = systemName, aspectRatio = aspect?.element0, contentMode = aspect?.element1, context = context)
                }
                is Image.ImageType.NamedCase -> {
                    val name = matchtarget_0.associated0
                    val bundle = matchtarget_0.associated1
                    val label = matchtarget_0.associated2
                    RenderNamedImage(name = name, bundle = bundle, label = label, aspectRatio = aspect?.element0, contentMode = aspect?.element1, colorScheme = colorScheme, context = context)
                }
                is Image.ImageType.DecorativeCase -> {
                    val name = matchtarget_0.associated0
                    val bundle = matchtarget_0.associated1
                    RenderNamedImage(name = name, bundle = bundle, label = null, aspectRatio = aspect?.element0, contentMode = aspect?.element1, colorScheme = colorScheme, context = context)
                }
            }
        }
    }

    @Composable
    private fun RenderNamedImage(name: String, bundle: Bundle?, label: Text?, aspectRatio: Double?, contentMode: ContentMode?, colorScheme: ColorScheme, context: ComposeContext) {
        val matchtarget_1 = rememberCachedAsset(assetImageCache, AssetKey(name = name, bundle = bundle, colorScheme = colorScheme)) { _ -> assetImageInfo(name = name, colorScheme = colorScheme, bundle = bundle ?: Bundle.main) }
        if (matchtarget_1 != null) {
            val assetImageInfo = matchtarget_1
            RenderAssetImage(asset = assetImageInfo, label = label, aspectRatio = aspectRatio, contentMode = contentMode, context = context)
        } else {
            rememberCachedAsset(contentsCache, AssetKey(name = name, bundle = bundle)) { _ -> symbolResourceURL(name = name, bundle = bundle ?: Bundle.main) }?.let { symbolResourceURL ->
                RenderSymbolImage(name = name, url = symbolResourceURL, label = label, aspectRatio = aspectRatio, contentMode = contentMode, context = context)
            }
        }
    }

    @Composable
    private fun RenderAssetImage(asset: AssetImageInfo, label: Text?, aspectRatio: Double?, contentMode: ContentMode?, context: ComposeContext) {
        val url = asset.url.sref()
        val model = ImageRequest.Builder(LocalContext.current)
            .fetcherFactory(AssetURLFetcher.Factory())
            .decoderFactory(coil3.svg.SvgDecoder.Factory())
            .decoderFactory(PdfDecoder.Factory())
            .data(url)
            .size(coil3.size.Size.ORIGINAL)
            .memoryCacheKey(url.description)
            .diskCacheKey(url.description)
            .build()

        val shouldTint = (templateRenderingMode == Image.TemplateRenderingMode.template) || (templateRenderingMode == null && asset.isTemplateImage)
        val tintColor = if (shouldTint) EnvironmentValues.shared._foregroundStyle?.asColor(opacity = 1.0, animationContext = context) ?: Color.primary.colorImpl() else null

        SubcomposeAsyncImage(model = model, contentDescription = null, loading = { _ ->
            // Display a neutral placeholder while loading
            RenderPainter(painter = ColorPainter(androidx.compose.ui.graphics.Color(0xFFECEFF1)), tintColor = null, scale = 1.0, aspectRatio = aspectRatio, contentMode = contentMode, context = context)
        }, success = { state -> RenderPainter(painter = this.painter, tintColor = tintColor, scale = scale, aspectRatio = aspectRatio, contentMode = contentMode, context = context) }, error = { state ->
            // Display a distinct error placeholder on failure
            RenderPainter(painter = ColorPainter(androidx.compose.ui.graphics.Color(0xFFB0BEC5)), tintColor = null, scale = 1.0, aspectRatio = aspectRatio, contentMode = contentMode, context = context)
        })
    }

    @Composable
    private fun RenderSymbolImage(name: String, url: URL, label: Text?, aspectRatio: Double?, contentMode: ContentMode?, context: ComposeContext) {

        fun symbolToImageVector(symbol: SymbolInfo, tintColor: androidx.compose.ui.graphics.Color): ImageVector {
            // this is the default size for material icons (24f), defined in the internal MaterialIconDimension variable with the comment "All Material icons (currently) are 24dp by 24dp, with a viewport size of 24 by 24" at:
            // https://github.com/androidx/androidx/blob/androidx-main/compose/material/material-icons-core/src/commonMain/kotlin/androidx/compose/material/icons/Icons.kt#L257
            //let size = androidx.compose.ui.geometry.Size(Float(24), Float(24))

            // manually create the bounding rect for all the symbols so we know how to size the viewport and offset the group
            // note that this does not take into account symbols that are designed to be smaller than their bounds, and ignores any baseline accommodation
            var symbolBounds = (symbol.paths.first?.pathParser?.toPath()?.getBounds() ?: Rect.Zero).sref()
            for (symbolPath in symbol.paths.dropFirst()) {
                val bounds = symbolPath.pathParser.toPath().getBounds()
                symbolBounds = Rect(minOf(symbolBounds.left, bounds.left), minOf(symbolBounds.top, bounds.top), maxOf(symbolBounds.right, bounds.right), maxOf(symbolBounds.bottom, bounds.bottom))
            }

            val symbolWidth = (symbolBounds.right - symbolBounds.left).sref()
            val symbolHeight = (symbolBounds.bottom - symbolBounds.top).sref()
            val symbolSpan = maxOf(symbolWidth, symbolHeight)

            // the offsets are adjusted to center the symbol in the viewport
            val symbolOffsetX = -symbolBounds.left + (if (symbolHeight > symbolWidth) ((symbolHeight - symbolWidth) / 2.0f) else 0.0f)
            val symbolOffsetY = -symbolBounds.top + (if (symbolWidth > symbolHeight) ((symbolWidth - symbolHeight) / 2.0f) else 0.0f)

            //logger.debug("created union path symbolSpan=\(symbolSpan) bounds=\(symbolBounds)")

            val imageVector = ImageVector.Builder(name = name, defaultWidth = symbolSpan.dp, defaultHeight = symbolSpan.dp, viewportWidth = symbolSpan, viewportHeight = symbolSpan, autoMirror = true).apply { ->
                group(translationX = symbolOffsetX, translationY = symbolOffsetY) { ->
                    path(fill = SolidColor(tintColor), fillAlpha = 1.0f, stroke = SolidColor(tintColor), strokeAlpha = 1.0f, strokeLineWidth = 1.0f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Bevel, strokeLineMiter = 1.0f, pathFillType = PathFillType.NonZero, pathBuilder = { ->
                        for (symbolPath in symbol.paths.sref()) {
                            val pathParser = symbolPath.pathParser.sref()
                            val bounds = pathParser.toPath().getBounds()
                            val pathData = pathParser.toNodes()
                            //logger.debug("parsed path bounds=\(bounds) nodes=\(pathData)")
                            addPath(pathData, fill = SolidColor(tintColor), stroke = SolidColor(tintColor))
                        }
                    })
                }
            }.build()

            return imageVector.sref()
        }

        // parse the Symbol Export XML and extract the SVG path representation that most closely matches the current font weight (e.g., "Black-S", "Regular-S", "Ultralight-S")
        fun parseSymbolXML(url: URL): Dictionary<SymbolSize, SymbolInfo> {
            logger.debug("parsing symbol SVG at: ${url}")
            val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(url.kotlin().toURL().openStream())

            // filter a NodeList into an array of Elements
            fun elements(list: org.w3c.dom.NodeList): Array<org.w3c.dom.Element> {
                return Array(0 until list.length).compactMap({ i -> list.item(i) as? org.w3c.dom.Element })
            }

            var symbolInfos: Dictionary<SymbolSize, SymbolInfo> = dictionaryOf()

            val gnodes = document.getElementsByTagName("g")
            for (symbolG in elements(gnodes)) {
                if (symbolG.getAttribute("id") != "Symbols") {
                    continue // there are also "Notes" and "Guides"
                }

                for (subG in elements(symbolG.childNodes)) {
                    val subGID = subG.getAttribute("id") // e.g., "Black-S", "Regular-S", "Ultralight-S"
                    val symbolSize_0 = SymbolSize(rawValue = subGID)
                    if (symbolSize_0 == null) {
                        logger.warning("could not parse symbol size: ${subGID}")
                        continue
                    }
                    var paths: Array<SymbolPath> = arrayOf()

                    for (pathNode in elements(subG.childNodes).filter { it -> it.nodeName == "path" }) {
                        // TODO: use the layers line multicolor-0:tintColor hierarchical-0:secondary to translate into compose equivalents
                        val pathClass = pathNode.getAttribute("class") ?: "" // e.g., monochrome-0 multicolor-0:tintColor hierarchical-0:secondary SFSymbolsPreviewWireframe
                        val pathD = pathNode.getAttribute("d")
                        if (!pathD.isEmpty) {
                            val pathParser = PathParser().parsePathString(pathD)
                            paths.append(SymbolPath(pathParser = pathParser, attrs = Array(pathClass.split(" "))))
                        }
                    }

                    symbolInfos[symbolSize_0] = SymbolInfo(size = symbolSize_0, paths = paths)
                }
            }

            return symbolInfos.sref()
        }

        val symbolInfos = rememberCachedAsset(symbolXMLCache, url) { url -> parseSymbolXML(url) }

        // match the best symbol for the current font weight
        val fontWeight = EnvironmentValues.shared._textEnvironment.fontWeight ?: Font.Weight.regular

        // Exporting as "Static" will contain all 27 variants (9 weights * 3 sizes),
        // but "Variable" will only have 3: Ultralight-S, Regular-S, and Black-S
        // in theory, we should interpolate the paths for in-between weights (like "light"),
        // but in absence of that logic, we just try to pick the closest variant for the current font weight

        val ultraLight: Array<SymbolSize> = arrayOf(SymbolSize.UltralightM, SymbolSize.UltralightS, SymbolSize.UltralightL)
        val thin: Array<SymbolSize> = arrayOf(SymbolSize.ThinM, SymbolSize.ThinS, SymbolSize.ThinL)
        val light: Array<SymbolSize> = arrayOf(SymbolSize.LightM, SymbolSize.LightS, SymbolSize.LightL)
        val regular: Array<SymbolSize> = arrayOf(SymbolSize.RegularM, SymbolSize.RegularS, SymbolSize.RegularL)
        val medium: Array<SymbolSize> = arrayOf(SymbolSize.MediumM, SymbolSize.MediumS, SymbolSize.MediumL)
        val semibold: Array<SymbolSize> = arrayOf(SymbolSize.SemiboldM, SymbolSize.SemiboldS, SymbolSize.SemiboldL)
        val bold: Array<SymbolSize> = arrayOf(SymbolSize.BoldM, SymbolSize.BoldS, SymbolSize.BoldL)
        val heavy: Array<SymbolSize> = arrayOf(SymbolSize.HeavyM, SymbolSize.HeavyS, SymbolSize.HeavyL)
        val black: Array<SymbolSize> = arrayOf(SymbolSize.BlackM, SymbolSize.BlackS, SymbolSize.BlackL)

        var weightPriority: Array<SymbolSize> = arrayOf()

        when (fontWeight) {
            Font.Weight.ultraLight -> weightPriority = (ultraLight + thin + light + regular + medium + semibold + bold + heavy + black).sref()
            Font.Weight.thin -> weightPriority = (thin + ultraLight + light + regular + medium + semibold + bold + heavy + black).sref()
            Font.Weight.light -> weightPriority = (light + thin + ultraLight + regular + medium + semibold + bold + heavy + black).sref()
            Font.Weight.regular -> weightPriority = (regular + medium + light + thin + semibold + bold + ultraLight + heavy + black).sref()
            Font.Weight.medium -> weightPriority = (medium + regular + semibold + light + bold + thin + heavy + black + ultraLight).sref()
            Font.Weight.semibold -> weightPriority = (semibold + medium + regular + bold + light + thin + heavy + ultraLight + black).sref()
            Font.Weight.bold -> weightPriority = (bold + heavy + black + semibold + medium + regular + light + thin + ultraLight).sref()
            Font.Weight.heavy -> weightPriority = (heavy + black + bold + semibold + medium + regular + light + thin + ultraLight).sref()
            Font.Weight.black -> weightPriority = (black + heavy + bold + semibold + medium + regular + light + thin + ultraLight).sref()
        }

        val tintColor = EnvironmentValues.shared._foregroundStyle?.asColor(opacity = 1.0, animationContext = context) ?: Color.primary.colorImpl()

        weightPriority.compactMap({ it -> symbolInfos[it] }).first?.let { symbolInfo ->
            val imageVector = symbolToImageVector(symbolInfo, tintColor = tintColor)
            RenderScaledImageVector(image = imageVector, name = name, aspectRatio = aspectRatio, contentMode = contentMode, context = context)
        }
    }

    @Composable
    private fun RenderBitmap(bitmap: Bitmap, scale: Double, aspectRatio: Double?, contentMode: ContentMode?, context: ComposeContext) {
        val imageBitmap = bitmap.asImageBitmap()
        val painter = BitmapPainter(imageBitmap)
        RenderPainter(painter = painter, scale = scale, aspectRatio = aspectRatio, contentMode = contentMode, context = context)
    }

    @Composable
    private fun RenderPainter(painter: Painter, scale: Double = 1.0, tintColor: androidx.compose.ui.graphics.Color? = null, aspectRatio: Double?, contentMode: ContentMode?, context: ComposeContext) {
        val isPlaceholder = EnvironmentValues.shared.redactionReasons.contains(RedactionReasons.placeholder)
        val colorFilter: ColorFilter?

        var templateColor: androidx.compose.ui.graphics.Color? = null
        if (this.templateRenderingMode == Image.TemplateRenderingMode.template) {
            templateColor = EnvironmentValues.shared._foregroundStyle?.asColor(opacity = 1.0, animationContext = context)
        }

        val matchtarget_2 = tintColor ?: templateColor
        if (matchtarget_2 != null) {
            val tintColor = matchtarget_2
            colorFilter = if (isPlaceholder) placeholderColorFilter(color = tintColor.copy(alpha = Float(Color.placeholderOpacity))) else ColorFilter.tint(tintColor)
        } else if (isPlaceholder) {
            colorFilter = placeholderColorFilter(color = Color.placeholder.colorImpl())
        } else {
            colorFilter = null
        }
        when (resizingMode) {
            Image.ResizingMode.stretch -> {
                if (painter.intrinsicSize.isUnspecified || painter.intrinsicSize.width.isNaN() || painter.intrinsicSize.width <= 0 || painter.intrinsicSize.height.isNaN() || painter.intrinsicSize.height <= 0) {
                    var modifier = Modifier.fillSize()
                    if (aspectRatio != null) {
                        modifier = modifier.aspectRatio(Float(aspectRatio))
                    }
                    androidx.compose.foundation.Image(painter = painter, modifier = modifier, contentDescription = null, contentScale = ContentScale.FillBounds, colorFilter = colorFilter)
                } else {
                    ImageLayout(intrinsicWidth = painter.intrinsicSize.width, intrinsicHeight = painter.intrinsicSize.height, aspectRatio = aspectRatio, contentMode = contentMode) { -> androidx.compose.foundation.Image(painter = painter, contentDescription = null, contentScale = ContentScale.FillBounds, colorFilter = colorFilter) }
                }
            }
            else -> {
                val modifier = Modifier.wrapContentSize(unbounded = true).size((painter.intrinsicSize.width / scale).dp, (painter.intrinsicSize.height / scale).dp)
                androidx.compose.foundation.Image(painter = painter, contentDescription = null, modifier = modifier, colorFilter = colorFilter)
            }
        }
    }

    @Composable
    private fun RenderSystem(systemName: String, aspectRatio: Double?, contentMode: ContentMode?, context: ComposeContext) {
        // we first check to see if there is a bundled symbol with the name in any of the asset catalogs, in which case we will use that symbol
        // note that we can only use the `main` (i.e., top-level) bundle to look up image resources, since Image(systemName:) does not accept a bundle
        rememberCachedAsset(contentsCache, AssetKey(name = systemName)) { _ -> symbolResourceURL(name = systemName, bundle = Bundle.main) }?.let { symbolResourceURL ->
            RenderSymbolImage(name = systemName, url = symbolResourceURL, label = null, aspectRatio = aspectRatio, contentMode = contentMode, context = context)
            return
        }
        val image_0 = Companion.composeImageVector(named = systemName)
        if (image_0 == null) {
            logger.warning("Unable to find system image named: ${systemName}")
            Icon(imageVector = Icons.Default.Warning, contentDescription = "missing icon")
            return
        }

        RenderScaledImageVector(image = image_0, name = systemName, aspectRatio = aspectRatio, contentMode = contentMode, context = context)
    }

    @Composable
    private fun RenderScaledImageVector(image: ImageVector, name: String, aspectRatio: Double?, contentMode: ContentMode?, context: ComposeContext) {

        val tintColor = EnvironmentValues.shared._foregroundStyle?.asColor(opacity = 1.0, animationContext = context) ?: Color.primary.colorImpl()
        when (resizingMode) {
            Image.ResizingMode.stretch -> {
                val painter = rememberVectorPainter(image)
                RenderPainter(painter = painter, tintColor = tintColor, aspectRatio = aspectRatio, contentMode = contentMode, context = context)
            }
            else -> {
                val textStyle = EnvironmentValues.shared.font?.fontImpl?.invoke() ?: LocalTextStyle.current
                var modifier: Modifier
                if (textStyle.fontSize.isSp) {
                    val textSizeDp = with(LocalDensity.current) { -> textStyle.fontSize.toDp() }
                    // Apply a multiplier to more closely match SwiftUI's relative text and system image sizes
                    modifier = Modifier.size(textSizeDp * 1.5f)
                } else {
                    modifier = Modifier
                }
                val isPlaceholder = EnvironmentValues.shared.redactionReasons.contains(RedactionReasons.placeholder)
                if (isPlaceholder) {
                    val placeholderColor = tintColor ?: Color.primary.colorImpl()
                    modifier = modifier.paint(ColorPainter(placeholderColor.copy(alpha = Float(Color.placeholderOpacity))))
                }
                val iconTint = if (isPlaceholder) {
                    androidx.compose.ui.graphics.Color.Transparent
                } else {
                    tintColor ?: Color.primary.colorImpl()
                }
                Icon(imageVector = image, contentDescription = name, modifier = modifier, tint = iconTint)
            }
        }
    }

    private fun placeholderColorFilter(color: androidx.compose.ui.graphics.Color): ColorFilter {
        val matrix = ColorMatrix().apply { ->
            set(0, 0, 0f) // Do not preserve original R
            set(1, 1, 0f) // Do not preserve original G
            set(2, 2, 0f) // Do not preserve original B
            set(3, 3, 0f) // Do not preserve original A

            set(0, 4, color.red * 255) // Use given color's R
            set(1, 4, color.green * 255) // Use given color's G
            set(2, 4, color.blue * 255) // Use given color's B
            set(3, 4, color.alpha * 255) // Use given color's A
        }
        return ColorFilter.colorMatrix(matrix)
    }

    enum class ResizingMode(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
        tile(0), // For bridging
        stretch(1); // For bridging

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: Int): Image.ResizingMode? {
                return when (rawValue) {
                    0 -> ResizingMode.tile
                    1 -> ResizingMode.stretch
                    else -> null
                }
            }
        }
    }

    fun resizable(): Image {
        var image = this.sref()
        image.resizingMode = Image.ResizingMode.stretch
        return image.sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun resizable(capInsets: EdgeInsets): Image {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun resizable(capInsets: EdgeInsets = EdgeInsets(), resizingMode: Image.ResizingMode): Image {
        fatalError()
    }

    enum class Interpolation {
        none,
        low,
        medium,
        high;

        @androidx.annotation.Keep
        companion object {
        }
    }

    fun interpolation(interpolation: Image.Interpolation): Image = this.sref()

    fun antialiased(isAntialiased: Boolean): Image = this.sref()

    enum class DynamicRange {
        standard,
        constrainedHigh,
        high;

        @androidx.annotation.Keep
        companion object {
        }
    }

    @androidx.annotation.Keep
    enum class TemplateRenderingMode: skip.lib.SwiftProjecting {
        template,
        original;

        override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
        private external fun Swift_projectionImpl(options: Int): () -> Any

        @androidx.annotation.Keep
        companion object {
        }
    }

    fun renderingMode(renderingMode: Image.TemplateRenderingMode?): Image {
        var image = this.sref()
        image.templateRenderingMode = renderingMode
        return image.sref()
    }

    @androidx.annotation.Keep
    enum class Orientation(override val rawValue: UByte, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, RawRepresentable<UByte> {
        up(UByte(0)),
        upMirrored(UByte(1)),
        down(UByte(2)),
        downMirrored(UByte(3)),
        left(UByte(4)),
        leftMirrored(UByte(5)),
        right(UByte(6)),
        rightMirrored(UByte(7));

        @androidx.annotation.Keep
        companion object: CaseIterableCompanion<Image.Orientation> {
            fun init(rawValue: UByte): Image.Orientation? {
                return when (rawValue) {
                    UByte(0) -> Orientation.up
                    UByte(1) -> Orientation.upMirrored
                    UByte(2) -> Orientation.down
                    UByte(3) -> Orientation.downMirrored
                    UByte(4) -> Orientation.left
                    UByte(5) -> Orientation.leftMirrored
                    UByte(6) -> Orientation.right
                    UByte(7) -> Orientation.rightMirrored
                    else -> null
                }
            }

            override val allCases: Array<Image.Orientation>
                get() = arrayOf(up, upMirrored, down, downMirrored, left, leftMirrored, right, rightMirrored)
        }
    }

    enum class Scale {
        small,
        medium,
        large;

        @androidx.annotation.Keep
        companion object {
        }
    }

    private fun assetImageInfo(name: String, colorScheme: ColorScheme, bundle: Bundle): AssetImageInfo? {
        for (dataURL in assetContentsURLs(name = "${name}.imageset", bundle = bundle)) {
            try {
                val data = Data(contentsOf = dataURL)
                logger.debug("loading imageset asset contents from: ${dataURL}")
                val imageSet = JSONDecoder().decode(ImageSet::class, from = data)
                val images = imageSet.images.sortedByAssetFit(colorScheme = colorScheme)
                // fall-back to load the highest-resolution image that is set (e.g., 3x before 2x before 1x)
                images.compactMap({ it.filename }).last.sref()?.let { fileName ->
                    // get the image filename and append it to the end
                    val resURL = dataURL.deletingLastPathComponent().appendingPathComponent(fileName)
                    logger.debug("loading imageset data from: ${resURL}")
                    return AssetImageInfo(url = resURL, imageSet = imageSet)
                }
            } catch (error: Throwable) {
                @Suppress("NAME_SHADOWING") val error = error.aserror()
                logger.warning("error loading image data from ${name}: ${error}")
            }
        }

        return null
    }

    private fun symbolResourceURL(name: String, bundle: Bundle): URL? {
        for (dataURL in assetContentsURLs(name = "${name}.symbolset", bundle = bundle)) {
            try {
                val data = Data(contentsOf = dataURL)
                logger.debug("loading symbolset asset contents from ${dataURL}")
                val symbolSet = JSONDecoder().decode(SymbolSet::class, from = data)
                symbolSet.symbols.compactMap({ it.filename }).last.sref()?.let { fileName ->
                    // get the symbol filename and append it to the end
                    val resURL = dataURL.deletingLastPathComponent().appendingPathComponent(fileName)
                    return resURL.sref()
                }
            } catch (error: Throwable) {
                @Suppress("NAME_SHADOWING") val error = error.aserror()
                logger.warning("error loading symbol data from ${name}: ${error}")
            }
        }

        return null
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun symbolRenderingMode(mode: SymbolRenderingMode?): Image = this.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun symbolVariableValueMode(mode: SymbolVariableValueMode?): Image {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun symbolColorRenderingMode(mode: SymbolColorRenderingMode?): Image {
        fatalError()
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as Image
        this.image = copy.image
        this.resizingMode = copy.resizingMode
        this.templateRenderingMode = copy.templateRenderingMode
        this.scale = copy.scale
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = Image(this as MutableStruct)

    override fun equals(other: Any?): Boolean {
        if (other !is Image) return false
        return image == other.image && resizingMode == other.resizingMode && templateRenderingMode == other.templateRenderingMode && scale == other.scale
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {

        private fun composeSymbolName(for_: String): String? {
            val symbolName = for_
            when (symbolName) {
                "person.crop.square" -> return "Icons.Outlined.AccountBox" //􀉹
                "person.crop.circle" -> return "Icons.Outlined.AccountCircle" //􀉭
                "plus.circle.fill" -> return "Icons.Outlined.AddCircle" //􀁍
                "plus" -> return "Icons.Outlined.Add" //􀅼
                "arrow.left" -> return "Icons.Outlined.ArrowBack" //􀄪
                "arrowtriangle.down.fill" -> return "Icons.Outlined.ArrowDropDown" //􀄥
                "arrow.forward" -> return "Icons.Outlined.ArrowForward" //􀰑
                "wrench" -> return "Icons.Outlined.Build" //􀎕
                "phone" -> return "Icons.Outlined.Call" //􀌾
                "checkmark.circle" -> return "Icons.Outlined.CheckCircle" //􀁢
                "checkmark" -> return "Icons.Outlined.Check" //􀆅
                "xmark" -> return "Icons.Outlined.Clear" //􀆄
                "pencil" -> return "Icons.Outlined.Create" //􀈊
                "calendar" -> return "Icons.Outlined.DateRange" //􀉉
                "trash" -> return "Icons.Outlined.Delete" //􀈑
                "envelope" -> return "Icons.Outlined.Email" //􀍕
                "arrow.forward.square" -> return "Icons.Outlined.ExitToApp" //􀰔
                "face.smiling" -> return "Icons.Outlined.Face" //􀎸
                "heart" -> return "Icons.Outlined.FavoriteBorder" //􀊴
                "heart.fill" -> return "Icons.Outlined.Favorite" //􀊵
                "house" -> return "Icons.Outlined.Home" //􀎞
                "info.circle" -> return "Icons.Outlined.Info" //􀅴
                "chevron.down" -> return "Icons.Outlined.KeyboardArrowDown" //􀆈
                "chevron.left" -> return "Icons.Outlined.KeyboardArrowLeft" //􀆉
                "chevron.right" -> return "Icons.Outlined.KeyboardArrowRight" //􀆊
                "chevron.up" -> return "Icons.Outlined.KeyboardArrowUp" //􀆇
                "list.bullet" -> return "Icons.Outlined.List" //􀋲
                "location" -> return "Icons.Outlined.LocationOn" //􀋑
                "lock" -> return "Icons.Outlined.Lock" //􀎠
                "line.3.horizontal" -> return "Icons.Outlined.Menu" //􀌇
                "ellipsis" -> return "Icons.Outlined.MoreVert" //􀍠
                "bell" -> return "Icons.Outlined.Notifications" //􀋙
                "person" -> return "Icons.Outlined.Person" //􀉩
                "mappin.circle" -> return "Icons.Outlined.Place" //􀎪
                "play" -> return "Icons.Outlined.PlayArrow" //􀊃
                "arrow.clockwise.circle" -> return "Icons.Outlined.Refresh" //􀚁
                "magnifyingglass" -> return "Icons.Outlined.Search" //􀊫
                "paperplane" -> return "Icons.Outlined.Send" //􀈟
                "gearshape" -> return "Icons.Outlined.Settings" //􀣋
                "square.and.arrow.up" -> return "Icons.Outlined.Share" //􀈂
                "cart" -> return "Icons.Outlined.ShoppingCart" //􀍩
                "hand.thumbsup" -> return "Icons.Outlined.ThumbUp" //􀉿
                "exclamationmark.triangle" -> return "Icons.Outlined.Warning" //􀇿
                "person.crop.square.fill" -> return "Icons.Filled.AccountBox" //􀉺
                "person.crop.circle.fill" -> return "Icons.Filled.AccountCircle" //􀉮
                "wrench.fill" -> return "Icons.Filled.Build" //􀎖
                "phone.fill" -> return "Icons.Filled.Call" //􀌿
                "checkmark.circle.fill" -> return "Icons.Filled.CheckCircle" //􀁣
                "trash.fill" -> return "Icons.Filled.Delete" //􀈒
                "envelope.fill" -> return "Icons.Filled.Email" //􀍖
                "house.fill" -> return "Icons.Filled.Home" //􀎟
                "info.circle.fill" -> return "Icons.Filled.Info" //􀅵
                "location.fill" -> return "Icons.Filled.LocationOn" //􀋒
                "lock.fill" -> return "Icons.Filled.Lock" //􀎡
                "bell.fill" -> return "Icons.Filled.Notifications" //􀋚
                "person.fill" -> return "Icons.Filled.Person" //􀉪
                "mappin.circle.fill" -> return "Icons.Filled.Place" //􀜈
                "play.fill" -> return "Icons.Filled.PlayArrow" //􀊄
                "paperplane.fill" -> return "Icons.Filled.Send" //􀈠
                "gearshape.fill" -> return "Icons.Filled.Settings" //􀣌
                "square.and.arrow.up.fill" -> return "Icons.Filled.Share" //􀈃
                "cart.fill" -> return "Icons.Filled.ShoppingCart" //􀍪
                "star.fill" -> return "Icons.Filled.Star" //􀋃
                "hand.thumbsup.fill" -> return "Icons.Filled.ThumbUp" //􀊀
                "exclamationmark.triangle.fill" -> return "Icons.Filled.Warning" //􀇿
                // Additional mappings for Kickbase-specific SF symbols
                "1.circle.fill" -> return "Icons.Filled.LooksOne" // numeric badge 1
                "2.circle.fill" -> return "Icons.Filled.LooksTwo" // numeric badge 2
                "person.fill.badge.minus" -> return "Icons.Filled.PersonRemoveAlt1" // person with minus
                "xmark.circle.fill" -> return "Icons.Filled.Clear" // xmark inside circle
                else -> return null
            }
        }

        /// Returns the `androidx.compose.ui.graphics.vector.ImageVector` for the given constant name.
        ///
        /// See: https://developer.android.com/reference/kotlin/androidx/compose/material/icons/Icons.Outlined
        internal fun composeImageVector(named: String): ImageVector? {
            val name = named
            when (composeSymbolName(for_ = name) ?: name) {
                "Icons.Outlined.AccountBox" -> return Icons.Outlined.AccountBox.sref()
                "Icons.Outlined.AccountCircle" -> return Icons.Outlined.AccountCircle.sref()
                "Icons.Outlined.AddCircle" -> return Icons.Outlined.AddCircle.sref()
                "Icons.Outlined.Add" -> return Icons.Outlined.Add.sref()
                "Icons.Outlined.ArrowBack" -> return Icons.Outlined.ArrowBack.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Outlined.ArrowBack
                "Icons.Outlined.ArrowDropDown" -> return Icons.Outlined.ArrowDropDown.sref()
                "Icons.Outlined.ArrowForward" -> return Icons.Outlined.ArrowForward.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Outlined.ArrowForward
                "Icons.Outlined.Build" -> return Icons.Outlined.Build.sref()
                "Icons.Outlined.Call" -> return Icons.Outlined.Call.sref()
                "Icons.Outlined.CheckCircle" -> return Icons.Outlined.CheckCircle.sref()
                "Icons.Outlined.Check" -> return Icons.Outlined.Check.sref()
                "Icons.Outlined.Clear" -> return Icons.Outlined.Clear.sref()
                "Icons.Outlined.Close" -> return Icons.Outlined.Close.sref()
                "Icons.Outlined.Create" -> return Icons.Outlined.Create.sref()
                "Icons.Outlined.DateRange" -> return Icons.Outlined.DateRange.sref()
                "Icons.Outlined.Delete" -> return Icons.Outlined.Delete.sref()
                "Icons.Outlined.Done" -> return Icons.Outlined.Done.sref()
                "Icons.Outlined.Edit" -> return Icons.Outlined.Edit.sref()
                "Icons.Outlined.Email" -> return Icons.Outlined.Email.sref()
                "Icons.Outlined.ExitToApp" -> return Icons.Outlined.ExitToApp.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Outlined.ExitToApp
                "Icons.Outlined.Face" -> return Icons.Outlined.Face.sref()
                "Icons.Outlined.FavoriteBorder" -> return Icons.Outlined.FavoriteBorder.sref()
                "Icons.Outlined.Favorite" -> return Icons.Outlined.Favorite.sref()
                "Icons.Outlined.Home" -> return Icons.Outlined.Home.sref()
                "Icons.Outlined.Info" -> return Icons.Outlined.Info.sref()
                "Icons.Outlined.KeyboardArrowDown" -> return Icons.Outlined.KeyboardArrowDown.sref()
                "Icons.Outlined.KeyboardArrowLeft" -> return Icons.Outlined.KeyboardArrowLeft.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Outlined.KeyboardArrowLeft
                "Icons.Outlined.KeyboardArrowRight" -> return Icons.Outlined.KeyboardArrowRight.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Outlined.KeyboardArrowRight
                "Icons.Outlined.KeyboardArrowUp" -> return Icons.Outlined.KeyboardArrowUp.sref()
                "Icons.Outlined.List" -> return Icons.Outlined.List.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Outlined.List
                "Icons.Outlined.LocationOn" -> return Icons.Outlined.LocationOn.sref()
                "Icons.Outlined.Lock" -> return Icons.Outlined.Lock.sref()
                "Icons.Outlined.MailOutline" -> return Icons.Outlined.MailOutline.sref()
                "Icons.Outlined.Menu" -> return Icons.Outlined.Menu.sref()
                "Icons.Outlined.MoreVert" -> return Icons.Outlined.MoreVert.sref()
                "Icons.Outlined.Notifications" -> return Icons.Outlined.Notifications.sref()
                "Icons.Outlined.Person" -> return Icons.Outlined.Person.sref()
                "Icons.Outlined.Phone" -> return Icons.Outlined.Phone.sref()
                "Icons.Outlined.Place" -> return Icons.Outlined.Place.sref()
                "Icons.Outlined.PlayArrow" -> return Icons.Outlined.PlayArrow.sref()
                "Icons.Outlined.Refresh" -> return Icons.Outlined.Refresh.sref()
                "Icons.Outlined.Search" -> return Icons.Outlined.Search.sref()
                "Icons.Outlined.Send" -> return Icons.Outlined.Send.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Outlined.Send
                "Icons.Outlined.Settings" -> return Icons.Outlined.Settings.sref()
                "Icons.Outlined.Share" -> return Icons.Outlined.Share.sref()
                "Icons.Outlined.ShoppingCart" -> return Icons.Outlined.ShoppingCart.sref()
                "Icons.Outlined.Star" -> return Icons.Outlined.Star.sref()
                "Icons.Outlined.ThumbUp" -> return Icons.Outlined.ThumbUp.sref()
                "Icons.Outlined.Warning" -> return Icons.Outlined.Warning.sref()
                "Icons.Filled.AccountBox" -> return Icons.Filled.AccountBox.sref()
                "Icons.Filled.AccountCircle" -> return Icons.Filled.AccountCircle.sref()
                "Icons.Filled.AddCircle" -> return Icons.Filled.AddCircle.sref()
                "Icons.Filled.Add" -> return Icons.Filled.Add.sref()
                "Icons.Filled.ArrowBack" -> return Icons.Filled.ArrowBack.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Filled.ArrowBack
                "Icons.Filled.ArrowDropDown" -> return Icons.Filled.ArrowDropDown.sref()
                "Icons.Filled.ArrowForward" -> return Icons.Filled.ArrowForward.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Filled.ArrowForward
                "Icons.Filled.Build" -> return Icons.Filled.Build.sref()
                "Icons.Filled.Call" -> return Icons.Filled.Call.sref()
                "Icons.Filled.CheckCircle" -> return Icons.Filled.CheckCircle.sref()
                "Icons.Filled.Check" -> return Icons.Filled.Check.sref()
                "Icons.Filled.Clear" -> return Icons.Filled.Clear.sref()
                "Icons.Filled.Close" -> return Icons.Filled.Close.sref()
                "Icons.Filled.Create" -> return Icons.Filled.Create.sref()
                "Icons.Filled.DateRange" -> return Icons.Filled.DateRange.sref()
                "Icons.Filled.Delete" -> return Icons.Filled.Delete.sref()
                "Icons.Filled.Done" -> return Icons.Filled.Done.sref()
                "Icons.Filled.Edit" -> return Icons.Filled.Edit.sref()
                "Icons.Filled.Email" -> return Icons.Filled.Email.sref()
                "Icons.Filled.ExitToApp" -> return Icons.Filled.ExitToApp.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Filled.ExitToApp
                "Icons.Filled.Face" -> return Icons.Filled.Face.sref()
                "Icons.Filled.FavoriteBorder" -> return Icons.Filled.FavoriteBorder.sref()
                "Icons.Filled.Favorite" -> return Icons.Filled.Favorite.sref()
                "Icons.Filled.Home" -> return Icons.Filled.Home.sref()
                "Icons.Filled.Info" -> return Icons.Filled.Info.sref()
                "Icons.Filled.KeyboardArrowDown" -> return Icons.Filled.KeyboardArrowDown.sref()
                "Icons.Filled.KeyboardArrowLeft" -> return Icons.Filled.KeyboardArrowLeft.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Filled.KeyboardArrowLeft
                "Icons.Filled.KeyboardArrowRight" -> return Icons.Filled.KeyboardArrowRight.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Filled.KeyboardArrowRight
                "Icons.Filled.KeyboardArrowUp" -> return Icons.Filled.KeyboardArrowUp.sref()
                "Icons.Filled.List" -> return Icons.Filled.List.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Filled.List
                "Icons.Filled.LocationOn" -> return Icons.Filled.LocationOn.sref()
                "Icons.Filled.Lock" -> return Icons.Filled.Lock.sref()
                "Icons.Filled.MailOutline" -> return Icons.Filled.MailOutline.sref()
                "Icons.Filled.Menu" -> return Icons.Filled.Menu.sref()
                "Icons.Filled.MoreVert" -> return Icons.Filled.MoreVert.sref()
                "Icons.Filled.Notifications" -> return Icons.Filled.Notifications.sref()
                "Icons.Filled.Person" -> return Icons.Filled.Person.sref()
                "Icons.Filled.Phone" -> return Icons.Filled.Phone.sref()
                "Icons.Filled.Place" -> return Icons.Filled.Place.sref()
                "Icons.Filled.PlayArrow" -> return Icons.Filled.PlayArrow.sref()
                "Icons.Filled.Refresh" -> return Icons.Filled.Refresh.sref()
                "Icons.Filled.Search" -> return Icons.Filled.Search.sref()
                "Icons.Filled.Send" -> return Icons.Filled.Send.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Filled.Send
                "Icons.Filled.Settings" -> return Icons.Filled.Settings.sref()
                "Icons.Filled.Share" -> return Icons.Filled.Share.sref()
                "Icons.Filled.ShoppingCart" -> return Icons.Filled.ShoppingCart.sref()
                "Icons.Filled.Star" -> return Icons.Filled.Star.sref()
                "Icons.Filled.ThumbUp" -> return Icons.Filled.ThumbUp.sref()
                "Icons.Filled.Warning" -> return Icons.Filled.Warning.sref()
                "Icons.Rounded.AccountBox" -> return Icons.Rounded.AccountBox.sref()
                "Icons.Rounded.AccountCircle" -> return Icons.Rounded.AccountCircle.sref()
                "Icons.Rounded.AddCircle" -> return Icons.Rounded.AddCircle.sref()
                "Icons.Rounded.Add" -> return Icons.Rounded.Add.sref()
                "Icons.Rounded.ArrowBack" -> return Icons.Rounded.ArrowBack.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Rounded.ArrowBack
                "Icons.Rounded.ArrowDropDown" -> return Icons.Rounded.ArrowDropDown.sref()
                "Icons.Rounded.ArrowForward" -> return Icons.Rounded.ArrowForward.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Rounded.ArrowForward
                "Icons.Rounded.Build" -> return Icons.Rounded.Build.sref()
                "Icons.Rounded.Call" -> return Icons.Rounded.Call.sref()
                "Icons.Rounded.CheckCircle" -> return Icons.Rounded.CheckCircle.sref()
                "Icons.Rounded.Check" -> return Icons.Rounded.Check.sref()
                "Icons.Rounded.Clear" -> return Icons.Rounded.Clear.sref()
                "Icons.Rounded.Close" -> return Icons.Rounded.Close.sref()
                "Icons.Rounded.Create" -> return Icons.Rounded.Create.sref()
                "Icons.Rounded.DateRange" -> return Icons.Rounded.DateRange.sref()
                "Icons.Rounded.Delete" -> return Icons.Rounded.Delete.sref()
                "Icons.Rounded.Done" -> return Icons.Rounded.Done.sref()
                "Icons.Rounded.Edit" -> return Icons.Rounded.Edit.sref()
                "Icons.Rounded.Email" -> return Icons.Rounded.Email.sref()
                "Icons.Rounded.ExitToApp" -> return Icons.Rounded.ExitToApp.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Rounded.ExitToApp
                "Icons.Rounded.Face" -> return Icons.Rounded.Face.sref()
                "Icons.Rounded.FavoriteBorder" -> return Icons.Rounded.FavoriteBorder.sref()
                "Icons.Rounded.Favorite" -> return Icons.Rounded.Favorite.sref()
                "Icons.Rounded.Home" -> return Icons.Rounded.Home.sref()
                "Icons.Rounded.Info" -> return Icons.Rounded.Info.sref()
                "Icons.Rounded.KeyboardArrowDown" -> return Icons.Rounded.KeyboardArrowDown.sref()
                "Icons.Rounded.KeyboardArrowLeft" -> return Icons.Rounded.KeyboardArrowLeft.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Rounded.KeyboardArrowLeft
                "Icons.Rounded.KeyboardArrowRight" -> return Icons.Rounded.KeyboardArrowRight.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Rounded.KeyboardArrowRight
                "Icons.Rounded.KeyboardArrowUp" -> return Icons.Rounded.KeyboardArrowUp.sref()
                "Icons.Rounded.List" -> return Icons.Rounded.List.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Rounded.List
                "Icons.Rounded.LocationOn" -> return Icons.Rounded.LocationOn.sref()
                "Icons.Rounded.Lock" -> return Icons.Rounded.Lock.sref()
                "Icons.Rounded.MailOutline" -> return Icons.Rounded.MailOutline.sref()
                "Icons.Rounded.Menu" -> return Icons.Rounded.Menu.sref()
                "Icons.Rounded.MoreVert" -> return Icons.Rounded.MoreVert.sref()
                "Icons.Rounded.Notifications" -> return Icons.Rounded.Notifications.sref()
                "Icons.Rounded.Person" -> return Icons.Rounded.Person.sref()
                "Icons.Rounded.Phone" -> return Icons.Rounded.Phone.sref()
                "Icons.Rounded.Place" -> return Icons.Rounded.Place.sref()
                "Icons.Rounded.PlayArrow" -> return Icons.Rounded.PlayArrow.sref()
                "Icons.Rounded.Refresh" -> return Icons.Rounded.Refresh.sref()
                "Icons.Rounded.Search" -> return Icons.Rounded.Search.sref()
                "Icons.Rounded.Send" -> return Icons.Rounded.Send.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Rounded.Send
                "Icons.Rounded.Settings" -> return Icons.Rounded.Settings.sref()
                "Icons.Rounded.Share" -> return Icons.Rounded.Share.sref()
                "Icons.Rounded.ShoppingCart" -> return Icons.Rounded.ShoppingCart.sref()
                "Icons.Rounded.Star" -> return Icons.Rounded.Star.sref()
                "Icons.Rounded.ThumbUp" -> return Icons.Rounded.ThumbUp.sref()
                "Icons.Rounded.Warning" -> return Icons.Rounded.Warning.sref()
                "Icons.Sharp.AccountBox" -> return Icons.Sharp.AccountBox.sref()
                "Icons.Sharp.AccountCircle" -> return Icons.Sharp.AccountCircle.sref()
                "Icons.Sharp.AddCircle" -> return Icons.Sharp.AddCircle.sref()
                "Icons.Sharp.Add" -> return Icons.Sharp.Add.sref()
                "Icons.Sharp.ArrowBack" -> return Icons.Sharp.ArrowBack.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Sharp.ArrowBack
                "Icons.Sharp.ArrowDropDown" -> return Icons.Sharp.ArrowDropDown.sref()
                "Icons.Sharp.ArrowForward" -> return Icons.Sharp.ArrowForward.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Sharp.ArrowForward
                "Icons.Sharp.Build" -> return Icons.Sharp.Build.sref()
                "Icons.Sharp.Call" -> return Icons.Sharp.Call.sref()
                "Icons.Sharp.CheckCircle" -> return Icons.Sharp.CheckCircle.sref()
                "Icons.Sharp.Check" -> return Icons.Sharp.Check.sref()
                "Icons.Sharp.Clear" -> return Icons.Sharp.Clear.sref()
                "Icons.Sharp.Close" -> return Icons.Sharp.Close.sref()
                "Icons.Sharp.Create" -> return Icons.Sharp.Create.sref()
                "Icons.Sharp.DateRange" -> return Icons.Sharp.DateRange.sref()
                "Icons.Sharp.Delete" -> return Icons.Sharp.Delete.sref()
                "Icons.Sharp.Done" -> return Icons.Sharp.Done.sref()
                "Icons.Sharp.Edit" -> return Icons.Sharp.Edit.sref()
                "Icons.Sharp.Email" -> return Icons.Sharp.Email.sref()
                "Icons.Sharp.ExitToApp" -> return Icons.Sharp.ExitToApp.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Sharp.ExitToApp
                "Icons.Sharp.Face" -> return Icons.Sharp.Face.sref()
                "Icons.Sharp.FavoriteBorder" -> return Icons.Sharp.FavoriteBorder.sref()
                "Icons.Sharp.Favorite" -> return Icons.Sharp.Favorite.sref()
                "Icons.Sharp.Home" -> return Icons.Sharp.Home.sref()
                "Icons.Sharp.Info" -> return Icons.Sharp.Info.sref()
                "Icons.Sharp.KeyboardArrowDown" -> return Icons.Sharp.KeyboardArrowDown.sref()
                "Icons.Sharp.KeyboardArrowLeft" -> return Icons.Sharp.KeyboardArrowLeft.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Sharp.KeyboardArrowLeft
                "Icons.Sharp.KeyboardArrowRight" -> return Icons.Sharp.KeyboardArrowRight.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Sharp.KeyboardArrowRight
                "Icons.Sharp.KeyboardArrowUp" -> return Icons.Sharp.KeyboardArrowUp.sref()
                "Icons.Sharp.List" -> return Icons.Sharp.List.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Sharp.List
                "Icons.Sharp.LocationOn" -> return Icons.Sharp.LocationOn.sref()
                "Icons.Sharp.Lock" -> return Icons.Sharp.Lock.sref()
                "Icons.Sharp.MailOutline" -> return Icons.Sharp.MailOutline.sref()
                "Icons.Sharp.Menu" -> return Icons.Sharp.Menu.sref()
                "Icons.Sharp.MoreVert" -> return Icons.Sharp.MoreVert.sref()
                "Icons.Sharp.Notifications" -> return Icons.Sharp.Notifications.sref()
                "Icons.Sharp.Person" -> return Icons.Sharp.Person.sref()
                "Icons.Sharp.Phone" -> return Icons.Sharp.Phone.sref()
                "Icons.Sharp.Place" -> return Icons.Sharp.Place.sref()
                "Icons.Sharp.PlayArrow" -> return Icons.Sharp.PlayArrow.sref()
                "Icons.Sharp.Refresh" -> return Icons.Sharp.Refresh.sref()
                "Icons.Sharp.Search" -> return Icons.Sharp.Search.sref()
                "Icons.Sharp.Send" -> return Icons.Sharp.Send.sref() // Compose 1.6 TODO: Icons.AutoMirrored.Sharp.Send
                "Icons.Sharp.Settings" -> return Icons.Sharp.Settings.sref()
                "Icons.Sharp.Share" -> return Icons.Sharp.Share.sref()
                "Icons.Sharp.ShoppingCart" -> return Icons.Sharp.ShoppingCart.sref()
                "Icons.Sharp.Star" -> return Icons.Sharp.Star.sref()
                "Icons.Sharp.ThumbUp" -> return Icons.Sharp.ThumbUp.sref()
                "Icons.Sharp.Warning" -> return Icons.Sharp.Warning.sref()
                "Icons.TwoTone.AccountBox" -> return Icons.TwoTone.AccountBox.sref()
                "Icons.TwoTone.AccountCircle" -> return Icons.TwoTone.AccountCircle.sref()
                "Icons.TwoTone.AddCircle" -> return Icons.TwoTone.AddCircle.sref()
                "Icons.TwoTone.Add" -> return Icons.TwoTone.Add.sref()
                "Icons.TwoTone.ArrowBack" -> return Icons.TwoTone.ArrowBack.sref() // Compose 1.6 TODO: Icons.AutoMirrored.TwoTone.ArrowBack
                "Icons.TwoTone.ArrowDropDown" -> return Icons.TwoTone.ArrowDropDown.sref()
                "Icons.TwoTone.ArrowForward" -> return Icons.TwoTone.ArrowForward.sref() // Compose 1.6 TODO: Icons.AutoMirrored.TwoTone.ArrowForward
                "Icons.TwoTone.Build" -> return Icons.TwoTone.Build.sref()
                "Icons.TwoTone.Call" -> return Icons.TwoTone.Call.sref()
                "Icons.TwoTone.CheckCircle" -> return Icons.TwoTone.CheckCircle.sref()
                "Icons.TwoTone.Check" -> return Icons.TwoTone.Check.sref()
                "Icons.TwoTone.Clear" -> return Icons.TwoTone.Clear.sref()
                "Icons.TwoTone.Close" -> return Icons.TwoTone.Close.sref()
                "Icons.TwoTone.Create" -> return Icons.TwoTone.Create.sref()
                "Icons.TwoTone.DateRange" -> return Icons.TwoTone.DateRange.sref()
                "Icons.TwoTone.Delete" -> return Icons.TwoTone.Delete.sref()
                "Icons.TwoTone.Done" -> return Icons.TwoTone.Done.sref()
                "Icons.TwoTone.Edit" -> return Icons.TwoTone.Edit.sref()
                "Icons.TwoTone.Email" -> return Icons.TwoTone.Email.sref()
                "Icons.TwoTone.ExitToApp" -> return Icons.TwoTone.ExitToApp.sref() // Compose 1.6 TODO: Icons.AutoMirrored.TwoTone.ExitToApp
                "Icons.TwoTone.Face" -> return Icons.TwoTone.Face.sref()
                "Icons.TwoTone.FavoriteBorder" -> return Icons.TwoTone.FavoriteBorder.sref()
                "Icons.TwoTone.Favorite" -> return Icons.TwoTone.Favorite.sref()
                "Icons.TwoTone.Home" -> return Icons.TwoTone.Home.sref()
                "Icons.TwoTone.Info" -> return Icons.TwoTone.Info.sref()
                "Icons.TwoTone.KeyboardArrowDown" -> return Icons.TwoTone.KeyboardArrowDown.sref()
                "Icons.TwoTone.KeyboardArrowLeft" -> return Icons.TwoTone.KeyboardArrowLeft.sref() // Compose 1.6 TODO: Icons.AutoMirrored.TwoTone.KeyboardArrowLeft
                "Icons.TwoTone.KeyboardArrowRight" -> return Icons.TwoTone.KeyboardArrowRight.sref() // Compose 1.6 TODO: Icons.AutoMirrored.TwoTone.KeyboardArrowRight
                "Icons.TwoTone.KeyboardArrowUp" -> return Icons.TwoTone.KeyboardArrowUp.sref()
                "Icons.TwoTone.List" -> return Icons.TwoTone.List.sref() // Compose 1.6 TODO: Icons.AutoMirrored.TwoTone.List
                "Icons.TwoTone.LocationOn" -> return Icons.TwoTone.LocationOn.sref()
                "Icons.TwoTone.Lock" -> return Icons.TwoTone.Lock.sref()
                "Icons.TwoTone.MailOutline" -> return Icons.TwoTone.MailOutline.sref()
                "Icons.TwoTone.Menu" -> return Icons.TwoTone.Menu.sref()
                "Icons.TwoTone.MoreVert" -> return Icons.TwoTone.MoreVert.sref()
                "Icons.TwoTone.Notifications" -> return Icons.TwoTone.Notifications.sref()
                "Icons.TwoTone.Person" -> return Icons.TwoTone.Person.sref()
                "Icons.TwoTone.Phone" -> return Icons.TwoTone.Phone.sref()
                "Icons.TwoTone.Place" -> return Icons.TwoTone.Place.sref()
                "Icons.TwoTone.PlayArrow" -> return Icons.TwoTone.PlayArrow.sref()
                "Icons.TwoTone.Refresh" -> return Icons.TwoTone.Refresh.sref()
                "Icons.TwoTone.Search" -> return Icons.TwoTone.Search.sref()
                "Icons.TwoTone.Send" -> return Icons.TwoTone.Send.sref() // Compose 1.6 TODO: Icons.AutoMirrored.TwoTone.Send
                "Icons.TwoTone.Settings" -> return Icons.TwoTone.Settings.sref()
                "Icons.TwoTone.Share" -> return Icons.TwoTone.Share.sref()
                "Icons.TwoTone.ShoppingCart" -> return Icons.TwoTone.ShoppingCart.sref()
                "Icons.TwoTone.Star" -> return Icons.TwoTone.Star.sref()
                "Icons.TwoTone.ThumbUp" -> return Icons.TwoTone.ThumbUp.sref()
                "Icons.TwoTone.Warning" -> return Icons.TwoTone.Warning.sref()
                else -> return null
            }
        }

        fun ResizingMode(rawValue: Int): Image.ResizingMode? = ResizingMode.init(rawValue = rawValue)

        fun Orientation(rawValue: UByte): Image.Orientation? = Orientation.init(rawValue = rawValue)
    }
}


private class SymbolInfo {
    internal val size: SymbolSize
    internal val paths: Array<SymbolPath>

    constructor(size: SymbolSize, paths: Array<SymbolPath>) {
        this.size = size
        this.paths = paths.sref()
    }
}

private class SymbolPath {
    internal val pathParser: PathParser
    internal val attrs: Array<String>

    constructor(pathParser: PathParser, attrs: Array<String>) {
        this.pathParser = pathParser.sref()
        this.attrs = attrs.sref()
    }
}

private val symbolXMLCache: Dictionary<URL, Dictionary<SymbolSize, SymbolInfo>> = dictionaryOf()
private val assetImageCache: Dictionary<AssetKey, AssetImageInfo?> = dictionaryOf()
private val contentsCache: Dictionary<AssetKey, URL?> = dictionaryOf()

@Composable
private fun ImageLayout(intrinsicWidth: Float, intrinsicHeight: Float, aspectRatio: Double?, contentMode: ContentMode?, image: @Composable () -> Unit) {
    Layout(content = { -> image() }) l@{ measurables, constraints ->
        if (measurables.isEmpty()) {
            return@l layout(width = 0, height = 0) { ->  }
        }

        val ratio = aspectRatio ?: Double(intrinsicWidth / intrinsicHeight)
        val placeable: Placeable
        val width: Int
        val height: Int
        if (constraints.hasBoundedWidth && constraints.maxWidth > 0 && constraints.hasBoundedHeight && constraints.maxHeight > 0) {
            if (contentMode == null) {
                height = constraints.maxHeight
                width = constraints.maxWidth
            } else {
                val constraintsRatio = (Double(constraints.maxWidth) / Double(constraints.maxHeight)).sref()
                val fitToWidth = if (contentMode == ContentMode.fill) ratio < constraintsRatio else ratio > constraintsRatio
                if (fitToWidth) {
                    width = constraints.maxWidth
                    height = Int(width / ratio)
                } else {
                    height = constraints.maxHeight
                    width = Int(height * ratio)
                }
            }
            placeable = measurables[0].measure(constraints.copy(minWidth = width, maxWidth = width, minHeight = height, maxHeight = height))
        } else if (constraints.hasBoundedWidth && constraints.maxWidth > 0) {
            width = constraints.maxWidth
            height = Int(width / ratio)
            placeable = measurables[0].measure(constraints.copy(minWidth = width, maxWidth = width, minHeight = height, maxHeight = height))
        } else if ((constraints.hasBoundedHeight && constraints.maxHeight > 0)) {
            height = constraints.maxHeight
            width = Int(height * ratio)
            placeable = measurables[0].measure(constraints.copy(minWidth = width, maxWidth = width, minHeight = height, maxHeight = height))
        } else {
            placeable = measurables[0].measure(constraints)
            width = placeable.width
            height = placeable.height
        }
        val layoutWidth = min(constraints.maxWidth, width)
        val layoutHeight = min(constraints.maxHeight, height)
        layout(width = layoutWidth, height = layoutHeight) { -> placeable.placeRelative(x = (layoutWidth - placeable.width) / 2, y = (layoutHeight - placeable.height) / 2) }
    }
}

/// Custom scale to handle fitting or filling a user-specified aspect ratio.
private class AspectRatioContentScale: ContentScale {
    internal val aspectRatio: Double
    internal val contentMode: ContentMode

    override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
        val dstAspectRatio = (dstSize.width / dstSize.height).sref()
        when (contentMode) {
            ContentMode.fit -> return if (aspectRatio > dstAspectRatio) fitToWidth(srcSize, dstSize) else fitToHeight(srcSize, dstSize)
            ContentMode.fill -> return if (aspectRatio < dstAspectRatio) fitToWidth(srcSize, dstSize) else fitToHeight(srcSize, dstSize)
        }
    }

    private fun fitToWidth(srcSize: Size, dstSize: Size): ScaleFactor = ScaleFactor(scaleX = dstSize.width / srcSize.width, scaleY = dstSize.width / Float(aspectRatio) / srcSize.height)

    private fun fitToHeight(srcSize: Size, dstSize: Size): ScaleFactor = ScaleFactor(scaleX = dstSize.height * Float(aspectRatio) / srcSize.width, scaleY = dstSize.height / srcSize.height)

    constructor(aspectRatio: Double, contentMode: ContentMode) {
        this.aspectRatio = aspectRatio
        this.contentMode = contentMode
    }
}

/// The Symbols layer contains up to 27 sublayers, each representing a symbol image variant. Identifiers of symbol variants have the form <weight>-<{S, M, L}>, where weight corresponds to a weight of the system font and S, M, or L matches the small, medium, or large symbol scale.
private enum class SymbolSize(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<String> {
    UltralightS("Ultralight-S"),
    ThinS("Thin-S"),
    LightS("Light-S"),
    RegularS("Regular-S"),
    MediumS("Medium-S"),
    SemiboldS("Semibold-S"),
    BoldS("Bold-S"),
    HeavyS("Heavy-S"),
    BlackS("Black-S"),

    UltralightM("Ultralight-M"),
    ThinM("Thin-M"),
    LightM("Light-M"),
    RegularM("Regular-M"),
    MediumM("Medium-M"),
    SemiboldM("Semibold-M"),
    BoldM("Bold-M"),
    HeavyM("Heavy-M"),
    BlackM("Black-M"),

    UltralightL("Ultralight-L"),
    ThinL("Thin-L"),
    LightL("Light-L"),
    RegularL("Regular-L"),
    MediumL("Medium-L"),
    SemiboldL("Semibold-L"),
    BoldL("Bold-L"),
    HeavyL("Heavy-L"),
    BlackL("Black-L");

    internal val fontWeight: Font.Weight
        get() {
            when (this) {
                SymbolSize.UltralightS, SymbolSize.UltralightM, SymbolSize.UltralightL -> return Font.Weight.ultraLight
                SymbolSize.ThinS, SymbolSize.ThinM, SymbolSize.ThinL -> return Font.Weight.thin
                SymbolSize.LightS, SymbolSize.LightM, SymbolSize.LightL -> return Font.Weight.light
                SymbolSize.RegularS, SymbolSize.RegularM, SymbolSize.RegularL -> return Font.Weight.regular
                SymbolSize.MediumS, SymbolSize.MediumM, SymbolSize.MediumL -> return Font.Weight.medium
                SymbolSize.SemiboldS, SymbolSize.SemiboldM, SymbolSize.SemiboldL -> return Font.Weight.semibold
                SymbolSize.BoldS, SymbolSize.BoldM, SymbolSize.BoldL -> return Font.Weight.bold
                SymbolSize.HeavyS, SymbolSize.HeavyM, SymbolSize.HeavyL -> return Font.Weight.heavy
                SymbolSize.BlackS, SymbolSize.BlackM, SymbolSize.BlackL -> return Font.Weight.black
            }
        }

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: String): SymbolSize? {
            return when (rawValue) {
                "Ultralight-S" -> SymbolSize.UltralightS
                "Thin-S" -> SymbolSize.ThinS
                "Light-S" -> SymbolSize.LightS
                "Regular-S" -> SymbolSize.RegularS
                "Medium-S" -> SymbolSize.MediumS
                "Semibold-S" -> SymbolSize.SemiboldS
                "Bold-S" -> SymbolSize.BoldS
                "Heavy-S" -> SymbolSize.HeavyS
                "Black-S" -> SymbolSize.BlackS
                "Ultralight-M" -> SymbolSize.UltralightM
                "Thin-M" -> SymbolSize.ThinM
                "Light-M" -> SymbolSize.LightM
                "Regular-M" -> SymbolSize.RegularM
                "Medium-M" -> SymbolSize.MediumM
                "Semibold-M" -> SymbolSize.SemiboldM
                "Bold-M" -> SymbolSize.BoldM
                "Heavy-M" -> SymbolSize.HeavyM
                "Black-M" -> SymbolSize.BlackM
                "Ultralight-L" -> SymbolSize.UltralightL
                "Thin-L" -> SymbolSize.ThinL
                "Light-L" -> SymbolSize.LightL
                "Regular-L" -> SymbolSize.RegularL
                "Medium-L" -> SymbolSize.MediumL
                "Semibold-L" -> SymbolSize.SemiboldL
                "Bold-L" -> SymbolSize.BoldL
                "Heavy-L" -> SymbolSize.HeavyL
                "Black-L" -> SymbolSize.BlackL
                else -> null
            }
        }
    }
}

private fun SymbolSize(rawValue: String): SymbolSize? = SymbolSize.init(rawValue = rawValue)

private class AssetImageInfo {
    /// The URL to the asset image
    internal val url: URL
    /// The ImageSet that was loaded for the given info
    internal val imageSet: ImageSet

    internal val isTemplateImage: Boolean
        get() {
            return imageSet.properties?.templateRenderingIntent == "template"
        }

    constructor(url: URL, imageSet: ImageSet) {
        this.url = url.sref()
        this.imageSet = imageSet
    }
}

/* The `Contents.json` in a `*.imageset` folder for an image
https://developer.apple.com/library/archive/documentation/Xcode/Reference/xcode_ref-Asset_Catalog_Format/ImageSetType.html
{
"images" : [
{
"filename" : "Cat.jpg",
"idiom" : "universal",
"scale" : "1x"
},
{
"idiom" : "universal",
"scale" : "2x"
},
{
"idiom" : "universal",
"scale" : "3x"
}
],
"info" : {
"author" : "xcode",
"version" : 1
}
}
*/
@androidx.annotation.Keep
private class ImageSet: Decodable {
    internal val images: Array<ImageSet.ImageInfo>
    internal val info: AssetContentsInfo
    internal val properties: ImageSet.ImageAssetProperties?

    @androidx.annotation.Keep
    internal class ImageInfo: Decodable, AssetSortable {
        internal val filename: String?
        override val idiom: String? // e.g. "universal"
        internal val scale: String? // e.g. "3x"
        override val appearances: Array<AssetAppearance>?

        constructor(filename: String? = null, idiom: String? = null, scale: String? = null, appearances: Array<AssetAppearance>? = null) {
            this.filename = filename
            this.idiom = idiom
            this.scale = scale
            this.appearances = appearances.sref()
        }

        private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
            filename("filename"),
            idiom("idiom"),
            scale("scale"),
            appearances("appearances");

            @androidx.annotation.Keep
            companion object {
                fun init(rawValue: String): CodingKeys? {
                    return when (rawValue) {
                        "filename" -> CodingKeys.filename
                        "idiom" -> CodingKeys.idiom
                        "scale" -> CodingKeys.scale
                        "appearances" -> CodingKeys.appearances
                        else -> null
                    }
                }
            }
        }

        constructor(from: Decoder) {
            val container = from.container(keyedBy = CodingKeys::class)
            this.filename = container.decodeIfPresent(String::class, forKey = CodingKeys.filename)
            this.idiom = container.decodeIfPresent(String::class, forKey = CodingKeys.idiom)
            this.scale = container.decodeIfPresent(String::class, forKey = CodingKeys.scale)
            this.appearances = container.decodeIfPresent(Array::class, elementType = AssetAppearance::class, forKey = CodingKeys.appearances)
        }

        @androidx.annotation.Keep
        companion object: DecodableCompanion<ImageSet.ImageInfo> {
            override fun init(from: Decoder): ImageSet.ImageInfo = ImageInfo(from = from)

            private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
        }
    }

    @androidx.annotation.Keep
    internal class ImageAssetProperties: Decodable {
        internal val preservesVectorRepresentation: Boolean?
        internal val templateRenderingIntent: String?

        internal enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
            preservesVectorRepresentation("preserves-vector-representation"),
            templateRenderingIntent("template-rendering-intent");

            @androidx.annotation.Keep
            companion object {
                fun init(rawValue: String): ImageSet.ImageAssetProperties.CodingKeys? {
                    return when (rawValue) {
                        "preserves-vector-representation" -> CodingKeys.preservesVectorRepresentation
                        "template-rendering-intent" -> CodingKeys.templateRenderingIntent
                        else -> null
                    }
                }
            }
        }

        constructor(preservesVectorRepresentation: Boolean? = null, templateRenderingIntent: String? = null) {
            this.preservesVectorRepresentation = preservesVectorRepresentation
            this.templateRenderingIntent = templateRenderingIntent
        }

        constructor(from: Decoder) {
            val container = from.container(keyedBy = CodingKeys::class)
            this.preservesVectorRepresentation = container.decodeIfPresent(Boolean::class, forKey = CodingKeys.preservesVectorRepresentation)
            this.templateRenderingIntent = container.decodeIfPresent(String::class, forKey = CodingKeys.templateRenderingIntent)
        }

        @androidx.annotation.Keep
        companion object: DecodableCompanion<ImageSet.ImageAssetProperties> {
            override fun init(from: Decoder): ImageSet.ImageAssetProperties = ImageAssetProperties(from = from)

            internal fun CodingKeys(rawValue: String): ImageSet.ImageAssetProperties.CodingKeys? = CodingKeys.init(rawValue = rawValue)
        }
    }

    constructor(images: Array<ImageSet.ImageInfo>, info: AssetContentsInfo, properties: ImageSet.ImageAssetProperties? = null) {
        this.images = images.sref()
        this.info = info
        this.properties = properties
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        images("images"),
        info("info"),
        properties("properties");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "images" -> CodingKeys.images
                    "info" -> CodingKeys.info
                    "properties" -> CodingKeys.properties
                    else -> null
                }
            }
        }
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.images = container.decode(Array::class, elementType = ImageSet.ImageInfo::class, forKey = CodingKeys.images)
        this.info = container.decode(AssetContentsInfo::class, forKey = CodingKeys.info)
        this.properties = container.decodeIfPresent(ImageSet.ImageAssetProperties::class, forKey = CodingKeys.properties)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<ImageSet> {
        override fun init(from: Decoder): ImageSet = ImageSet(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

/* The `Contents.json` in a `*.symbolset` folder for a symbol, which looks like:
{
"info" : {
"author" : "xcode",
"version" : 1
},
"symbols" : [
{
"filename" : "face.dashed.fill.svg",
"idiom" : "universal"
}
]
}
*/
@androidx.annotation.Keep
private class SymbolSet: Decodable {
    internal val symbols: Array<SymbolSet.Symbol>
    internal val info: AssetContentsInfo

    @androidx.annotation.Keep
    internal class Symbol: Decodable {
        internal val filename: String?
        internal val idiom: String? // e.g. "universal"

        constructor(filename: String? = null, idiom: String? = null) {
            this.filename = filename
            this.idiom = idiom
        }

        private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
            filename("filename"),
            idiom("idiom");

            @androidx.annotation.Keep
            companion object {
                fun init(rawValue: String): CodingKeys? {
                    return when (rawValue) {
                        "filename" -> CodingKeys.filename
                        "idiom" -> CodingKeys.idiom
                        else -> null
                    }
                }
            }
        }

        constructor(from: Decoder) {
            val container = from.container(keyedBy = CodingKeys::class)
            this.filename = container.decodeIfPresent(String::class, forKey = CodingKeys.filename)
            this.idiom = container.decodeIfPresent(String::class, forKey = CodingKeys.idiom)
        }

        @androidx.annotation.Keep
        companion object: DecodableCompanion<SymbolSet.Symbol> {
            override fun init(from: Decoder): SymbolSet.Symbol = Symbol(from = from)

            private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
        }
    }

    constructor(symbols: Array<SymbolSet.Symbol>, info: AssetContentsInfo) {
        this.symbols = symbols.sref()
        this.info = info
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        symbols("symbols"),
        info("info");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "symbols" -> CodingKeys.symbols
                    "info" -> CodingKeys.info
                    else -> null
                }
            }
        }
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.symbols = container.decode(Array::class, elementType = SymbolSet.Symbol::class, forKey = CodingKeys.symbols)
        this.info = container.decode(AssetContentsInfo::class, forKey = CodingKeys.info)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<SymbolSet> {
        override fun init(from: Decoder): SymbolSet = SymbolSet(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

