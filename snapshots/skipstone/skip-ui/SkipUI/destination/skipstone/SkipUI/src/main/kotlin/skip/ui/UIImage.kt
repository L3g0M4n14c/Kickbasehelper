package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import java.nio.ByteBuffer
import android.graphics.Bitmap

@androidx.annotation.Keep
open class UIImage: skip.lib.SwiftProjecting {
    internal val bitmap: Bitmap?

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(named: String, in_: Bundle? = null, compatibleWith: Any? = null) {
        val bundle = in_
        val traitCollection = compatibleWith
        this.bitmap = null
        this.scale = 1.0
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(named: String, in_: Bundle?, with: UIImage.Configuration?, unusedp: Unit? = null) {
        val bundle = in_
        val configuration = with
        this.bitmap = null
        this.scale = 1.0
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(named: String, in_: Bundle? = null, variableValue: Double, configuration: UIImage.Configuration? = null) {
        val bundle = in_
        this.bitmap = null
        this.scale = 1.0
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(imageLiteralResourceName: String, unusedp_0: Unit? = null, unusedp_1: Unit? = null) {
        this.bitmap = null
        this.scale = 1.0
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(systemName: String, withConfiguration: UIImage.Configuration? = null, unusedp_0: Unit? = null, unusedp_1: Unit? = null, unusedp_2: Unit? = null) {
        val configuration = withConfiguration
        this.bitmap = null
        this.scale = 1.0
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(systemName: String, variableValue: Double, configuration: UIImage.Configuration? = null, unusedp: Unit? = null) {
        this.bitmap = null
        this.scale = 1.0
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(systemName: String, compatibleWith: Any?, unusedp_0: Unit? = null, unusedp_1: Unit? = null, unusedp_2: Unit? = null, unusedp_3: Unit? = null) {
        val traitCollection = compatibleWith
        this.bitmap = null
        this.scale = 1.0
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(resource: Any, unusedp_0: Unit? = null, unusedp_1: Unit? = null, unusedp_2: Unit? = null, unusedp_3: Unit? = null) {
        this.bitmap = null
        this.scale = 1.0
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun preparingForDisplay(): UIImage? {
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun prepareForDisplay(completionHandler: (UIImage?) -> Unit) {
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun preparingThumbnail(of: CGSize): UIImage? {
        val size = of
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun prepareThumbnail(of: CGSize, completionHandler: (UIImage?) -> Unit) {
        val size = of
        fatalError()
    }

    constructor(contentsOfFile: String) {
        val path = contentsOfFile
        val contentResolver = ProcessInfo.processInfo.androidContext.getContentResolver()
        val uri = Uri.parse(path)
        val source = ImageDecoder.createSource(contentResolver, uri)
        val bitmap_0 = ImageDecoder.decodeBitmap(source)
        if (bitmap_0 == null) {
            throw NullReturnException()
        }

        this.bitmap = bitmap_0.sref()
        this.scale = 1.0
    }

    constructor(data: Data, scale: Double = 1.0) {
        try {
            val bytes = data.kotlin(nocopy = true)
            val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
            val bitmap_1 = ImageDecoder.decodeBitmap(source)
            if (bitmap_1 == null) {
                throw NullReturnException()
            }

            this.bitmap = bitmap_1.sref()

        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            throw NullReturnException()
        }
        this.scale = scale
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    class UIImageReader {

        @androidx.annotation.Keep
        companion object {
        }
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun withConfiguration(configuration: UIImage.Configuration): UIImage {
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun applyingSymbolConfiguration(configuration: Any): UIImage? {
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun imageFlippedForRightToLeftLayoutDirection(): UIImage {
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun withHorizontallyFlippedOrientation(): UIImage {
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun withRenderingMode(renderingMode: Any): UIImage {
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun withAlignmentRectInsets(insets: Any): UIImage {
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun resizableImage(withCapInsets: Any, resizingMode: Any? = null): UIImage {
        val insets = withCapInsets
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun imageWithoutBaseline(): UIImage {
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun withBaselineOffset(fromBottom: Double): UIImage {
        val offset = fromBottom
        fatalError()
    }

    val scale: Double

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val size: CGSize
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val imageOrientation: Any
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val flipsForRightToLeftLayoutDirection: Boolean
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val resizingMode: Any
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val duration: Double
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val capInsets: Any
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val alignmentRectInsets: Any
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val isSymbolImage: Boolean
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val configuration: Any?
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val symbolConfiguration: Any?
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val traitCollection: Any
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val isHighDynamicRange: Boolean
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun imageRestrictedToStandardDynamicRange(): UIImage {
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun heicData(): Data? {
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val baselineOffsetFromBottom: Double?
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val renderingMode: Any
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun withTintColor(color: Any, renderingMode: Any? = null): UIImage {
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun draw(at: CGPoint, blendMode: Any? = null, alpha: Double? = null) = Unit
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    internal open fun draw(in_: CGRect, blendMode: Any? = null, alpha: Double? = null) = Unit
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun drawAsPattern(in_: CGRect) = Unit

    open fun jpegData(compressionQuality: Double): Data? {
        if (bitmap == null) {
            return null
        }
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, Int(compressionQuality * 100.0), outputStream)
        val bytes = outputStream.toByteArray()
        return Data(platformValue = bytes)
    }

    open fun pngData(): Data? {
        if (bitmap == null) {
            return null
        }
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val bytes = outputStream.toByteArray()
        return Data(platformValue = bytes)
    }

    class Configuration {

        @androidx.annotation.Keep
        companion object {
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: CompanionClass() {

        override fun bridgedInit(contentsOfFile: String): UIImage? {
            val path = contentsOfFile
            return (try { UIImage(contentsOfFile = path) } catch (_: NullReturnException) { null })
        }

        override fun bridgedInit(data: Data, scale: Double): UIImage? = (try { UIImage(data = data, scale = scale) } catch (_: NullReturnException) { null })
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun animatedImageNamed(name: String, duration: Double): UIImage? {
            fatalError()
        }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun animatedImage(with: Array<UIImage>, duration: Double): UIImage? {
            val images = with
            fatalError()
        }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun animatedResizableImageNamed(name: String, capInsets: Any, resizingMode: Any? = null, duration: Double): UIImage? {
            fatalError()
        }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val add: UIImage
            get() {
                fatalError()
            }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val remove: UIImage
            get() {
                fatalError()
            }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val actions: UIImage
            get() {
                fatalError()
            }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val checkmark: UIImage
            get() {
                fatalError()
            }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val strokedCheckmark: UIImage
            get() {
                fatalError()
            }
    }
    open class CompanionClass {
        open fun bridgedInit(contentsOfFile: String): UIImage? = UIImage.bridgedInit(contentsOfFile = contentsOfFile)
        open fun bridgedInit(data: Data, scale: Double): UIImage? = UIImage.bridgedInit(data = data, scale = scale)
    }
}

