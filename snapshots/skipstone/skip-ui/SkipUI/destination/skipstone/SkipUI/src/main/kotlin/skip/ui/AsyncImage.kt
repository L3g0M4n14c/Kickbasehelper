package skip.ui

import skip.lib.*
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.webkit.MimeTypeMap
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import coil3.fetch.Fetcher
import coil3.fetch.FetchResult
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.PlatformContext
import coil3.asImage
import kotlin.math.roundToInt
import okio.buffer
import okio.source

@androidx.annotation.Keep
class AsyncImage: View, Renderable, skip.lib.SwiftProjecting {
    internal val url: URL?
    internal val scale: Double
    internal val content: (AsyncImagePhase) -> View

    constructor(url: URL?, scale: Double = 1.0) {
        this.url = url.sref()
        this.scale = scale
        this.content = l@{ phase ->
            when (phase) {
                is AsyncImagePhase.EmptyCase -> return@l Companion.defaultPlaceholder()
                is AsyncImagePhase.FailureCase -> return@l Companion.defaultPlaceholder()
                is AsyncImagePhase.SuccessCase -> {
                    val image = phase.associated0
                    return@l image
                }
            }
        }
    }

    constructor(url: URL?, scale: Double = 1.0, content: (Image) -> View, placeholder: () -> View) {
        this.url = url.sref()
        this.scale = scale
        this.content = l@{ phase ->
            when (phase) {
                is AsyncImagePhase.EmptyCase -> return@l placeholder()
                is AsyncImagePhase.FailureCase -> return@l placeholder()
                is AsyncImagePhase.SuccessCase -> {
                    val image = phase.associated0
                    return@l content(image)
                }
            }
        }
    }

    constructor(url: URL?, scale: Double = 1.0, transaction: Any? = null, content: (AsyncImagePhase) -> View) {
        this.url = url.sref()
        this.scale = scale
        this.content = content
    }

    // Note that we reverse the `url` and `scale` parameter order just to create a unique JVM signature
    constructor(scale: Double, url: URL?, bridgedContent: ((Image?, Error?) -> View)?) {
        this.url = url.sref()
        this.scale = scale
        this.content = l@{ phase ->
            when (phase) {
                is AsyncImagePhase.EmptyCase -> {
                    if (bridgedContent != null) {
                        return@l bridgedContent(null, null)
                    } else {
                        return@l Companion.defaultPlaceholder()
                    }
                }
                is AsyncImagePhase.FailureCase -> {
                    val error = phase.associated0
                    if (bridgedContent != null) {
                        return@l bridgedContent(null, error)
                    } else {
                        return@l Companion.defaultPlaceholder()
                    }
                }
                is AsyncImagePhase.SuccessCase -> {
                    val image = phase.associated0
                    if (bridgedContent != null) {
                        return@l bridgedContent(image, null)
                    } else {
                        return@l image
                    }
                }
            }
        }
    }

    @Composable
    override fun Render(context: ComposeContext) {
        if (url == null) {
            this.content(AsyncImagePhase.empty).Compose(context)
            return
        }

        val urlString = url.absoluteString
        // Coil does not automatically handle embedded jar URLs like
        // jar:file:/data/app/…/base.apk!/showcase/module/Resources/swift-logo.png or
        // asset:/showcase/module/Resources/swift-logo.png, so
        // we add a custom fetchers that will handle loading the URL.
        // Otherwise use Coil's default URL string handling
        val requestSource: Any = (if (AssetURLFetcher.handlesURL(url)) url else urlString).sref()
        val model = ImageRequest.Builder(LocalContext.current)
            .fetcherFactory(AssetURLFetcher.Factory())
            .decoderFactory(coil3.svg.SvgDecoder.Factory())
            .decoderFactory(PdfDecoder.Factory())
            .data(requestSource)
            .size(Size.ORIGINAL)
            .memoryCacheKey(urlString)
            .diskCacheKey(urlString)
            .build()

        SubcomposeAsyncImage(model = model, contentDescription = null, loading = { _ -> content(AsyncImagePhase.empty).Compose(context = context) }, success = { state ->
            val image = Image(painter = this.painter, scale = scale)
            val content = content(AsyncImagePhase.success(image))
            content.Compose(context = context)
        }, error = { state -> content(AsyncImagePhase.failure(ErrorException(cause = state.result.throwable))).Compose(context = context) })
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {

        private fun defaultPlaceholder(): View {
            return ComposeBuilder { composectx: ComposeContext -> Color.placeholder.Compose(composectx) }
        }
    }
}

sealed class AsyncImagePhase {
    class EmptyCase: AsyncImagePhase() {
    }
    class SuccessCase(val associated0: Image): AsyncImagePhase() {
    }
    class FailureCase(val associated0: Error): AsyncImagePhase() {
    }

    val image: Image?
        get() {
            when (this) {
                is AsyncImagePhase.SuccessCase -> {
                    val image = this.associated0
                    return image
                }
                else -> return null
            }
        }

    val error: Error?
        get() {
            when (this) {
                is AsyncImagePhase.FailureCase -> {
                    val error = this.associated0
                    return error
                }
                else -> return null
            }
        }

    @androidx.annotation.Keep
    companion object {
        val empty: AsyncImagePhase = EmptyCase()
        fun success(associated0: Image): AsyncImagePhase = SuccessCase(associated0)
        fun failure(associated0: Error): AsyncImagePhase = FailureCase(associated0)
    }
}

/// A Coil fetcher that handles `skip.foundation.URL` instances for known custom URL schemes.
internal class AssetURLFetcher: Fetcher {
    private val url: URL
    private val options: coil3.request.Options

    internal constructor(url: URL, options: coil3.request.Options) {
        this.url = url.sref()
        this.options = options.sref()
    }

    override suspend fun fetch(): FetchResult = Async.run l@{
        val ctx = options.context.sref()
        val stream = url.kotlin().toURL().openConnection().getInputStream().source().buffer()
        val source = coil3.decode.ImageSource(source = stream, fileSystem = okio.FileSystem.SYSTEM)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url.absoluteString))
        val dataSource: coil3.decode.DataSource = coil3.decode.DataSource.DISK.sref()
        return@l coil3.fetch.SourceFetchResult(source = source, mimeType = mimeType, dataSource = dataSource)
    }

    internal class Factory: Fetcher.Factory<URL> {
        override fun create(data: URL, options: coil3.request.Options, imageLoader: ImageLoader): Fetcher? {
            if ((!AssetURLFetcher.handlesURL(data))) {
                return null
            }
            return AssetURLFetcher(url = data, options = options)
        }
    }

    @androidx.annotation.Keep
    companion object {
        internal val handledURLSchemes: Set<String> = setOf("asset", "jar", "jarfile", "jar:file")

        internal fun handlesURL(url: URL): Boolean {
            val scheme_0 = url.scheme
            if (scheme_0 == null) {
                return false
            }
            return handledURLSchemes.contains(scheme_0)
        }
    }
}

internal open class PdfDecoder: coil3.decode.Decoder {
    internal val sourceResult: coil3.fetch.SourceFetchResult
    internal val options: coil3.request.Options

    internal class Factory: coil3.decode.Decoder.Factory {
        override fun create(result: coil3.fetch.SourceFetchResult, options: coil3.request.Options, imageLoader: coil3.ImageLoader): coil3.decode.Decoder? {
            //logger.debug("PdfDecoder.Factory.create result=\(result) options=\(options) imageLoader=\(imageLoader)")
            return PdfDecoder(sourceResult = result, options = options)
        }
    }

    internal constructor(sourceResult: coil3.fetch.SourceFetchResult, options: coil3.request.Options) {
        this.sourceResult = sourceResult.sref()
        this.options = options.sref()
    }

    override suspend fun decode(): coil3.decode.DecodeResult? = Async.run l@{
        val deferactions_0: MutableList<() -> Unit> = mutableListOf()
        try {
            val src: coil3.decode.ImageSource = sourceResult.source.sref()
            val source: okio.BufferedSource = src.source()

            // make sure it is a PDF image by scanning for "%PDF-" (25 50 44 46 2D)
            val peek = source.peek()
            // logger.debug("PdfDecoder.decode peek \(peek.readByte()) \(peek.readByte()) \(peek.readByte()) \(peek.readByte()) \(peek.readByte())")

            if (peek.readByte() != Byte(0x25)) {
                return@l null // %
            } // %
            if (peek.readByte() != Byte(0x50)) {
                return@l null // P
            } // P
            if (peek.readByte() != Byte(0x44)) {
                return@l null // D
            } // D
            if (peek.readByte() != Byte(0x46)) {
                return@l null // F
            } // F
            if (peek.readByte() != Byte(0x2D)) {
                return@l null // -
            } // -

            // Unfortunately, PdfRenderer requires a ParcelFileDescriptor, which can only be created from an actual file, and not the JarInputStream from which we load assets from the .apk; so we need to write the PDF out to a temporary file in order to be able to render the PDF to a Bitmap that Coil can use
            // Fortunately, even through we are loading from a buffer, Coil's ImageSource.file() function will: “Return a Path that resolves to a file containing this ImageSource's data. If this image source is backed by a BufferedSource, a temporary file containing this ImageSource's data will be created.”
            val imageFile = src.file().toFile()

            val parcelFileDescriptor = android.os.ParcelFileDescriptor.open(imageFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            deferactions_0.add {
                parcelFileDescriptor.close()
            }

            val pdfRenderer = android.graphics.pdf.PdfRenderer(parcelFileDescriptor)
            deferactions_0.add {
                pdfRenderer.close()
            }

            val page = pdfRenderer.openPage(0)
            deferactions_0.add {
                page.close()
            }

            val density = options.context.resources.displayMetrics.density.sref()

            val srcWidth = Double(page.width * density)
            val srcHeight = Double(page.height * density)

            val optionsWidth = (options.size.width as? coil3.size.Dimension.Pixels)?.px?.toDouble()
            val optionsHeight = (options.size.height as? coil3.size.Dimension.Pixels)?.px?.toDouble()

            val dstWidth: Double = optionsWidth ?: srcWidth
            val dstHeight: Double = optionsHeight ?: srcHeight

            val scale = coil3.decode.DecodeUtils.computeSizeMultiplier(srcWidth = srcWidth, srcHeight = srcHeight, dstWidth = dstWidth, dstHeight = dstHeight, scale = options.scale)

            val width = (scale * srcWidth).roundToInt()
            val height = (scale * srcHeight).roundToInt()

            logger.debug("PdfDecoder.decode result=${sourceResult} options=${options} imageFile=${imageFile} width=${width} height=${height}")
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val drawable = android.graphics.drawable.BitmapDrawable(options.context.resources, bitmap)
            return@l coil3.decode.DecodeResult(image = drawable.asImage(), isSampled = true)
        } finally {
            deferactions_0.asReversed().forEach { it.invoke() }
        }
    }
}
