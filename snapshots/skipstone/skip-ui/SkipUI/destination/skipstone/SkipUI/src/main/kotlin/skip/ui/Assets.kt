package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*

/// A simple local cache that returns the cached value if it exists, or else instantiates it using the block and stores the result.
internal fun <T, U> rememberCachedAsset(cache: Dictionary<T, U>, key: T, block: (T) -> U): U {
    return synchronized(cache) l@{ ->
        cache[key].sref()?.let { value ->
            return@l value
        }
        val value = block(key)
        cache[key] = value.sref()
        return@l value
    }
}

/// Find all the `.xcassets` resource for the given bundle.
internal fun assetContentsURLs(name: String, bundle: Bundle): Array<URL> {
    val name = name.replace(" ", "%20")

    val resourceNames = bundle.resourcesIndex.sref()

    var resourceURLs: Array<URL> = arrayOf()
    for (resourceName in resourceNames.sref()) {
        val components = resourceName.split(separator = '/').map({ it -> String(it) })
        // return every *.xcassets/NAME/Contents.json
        if (components.first?.hasSuffix(".xcassets") == true && components.dropFirst().first == name && components.last == "Contents.json") {
            bundle.url(forResource = resourceName, withExtension = null)?.let { contentsURL ->
                resourceURLs.append(contentsURL)
            }
        }
    }
    return resourceURLs.sref()
}

/// A cache key for remembering the `Content.json` URL location in the bundled assets for the given
/// name, bundle, and `ColorScheme` combination.
internal class AssetKey {
    internal val name: String
    internal val bundle: Bundle?
    internal val colorScheme: ColorScheme?

    internal constructor(name: String, bundle: Bundle? = null, colorScheme: ColorScheme? = null) {
        this.name = name
        this.bundle = bundle
        this.colorScheme = colorScheme
    }

    override fun equals(other: Any?): Boolean {
        if (other !is AssetKey) return false
        return name == other.name && bundle == other.bundle && colorScheme == other.colorScheme
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, name)
        result = Hasher.combine(result, bundle)
        result = Hasher.combine(result, colorScheme)
        return result
    }
}

/// Protocol used to sort candidate assets.
internal interface AssetSortable {
    val idiom: String?
    val appearances: Array<AssetAppearance>?
}

/// Sort assets by relevance for the given color scheme - the most relevant will be **last**.
internal fun <Element> Array<Element>.sortedByAssetFit(colorScheme: ColorScheme): Array<Element> {
    return sorted l@{ it, it_1 ->
        val asset0 = (it as? AssetSortable).sref()
        val asset1 = (it_1 as? AssetSortable).sref()

        // We use universal assets for Android
        val isCandidate0 = asset0?.idiom == null || asset0?.idiom == "universal"
        val isCandidate1 = asset1?.idiom == null || asset1?.idiom == "universal"
        if (isCandidate0 && !isCandidate1) {
            return@l false
        } else if (isCandidate1 && !isCandidate0) {
            return@l true
        }

        // If one of the assets is explicitly of the target color scheme, use it
        val isColorScheme0 = asset0?.appearances?.contains { it -> it.isColorScheme(colorScheme) } == true
        val isColorScheme1 = asset1?.appearances?.contains { it -> it.isColorScheme(colorScheme) } == true
        if (isColorScheme0 && !isColorScheme1) {
            return@l false
        } else if (isColorScheme1 && !isColorScheme0) {
            return@l true
        }

        // If one of the assets is of the wrong color scheme, use the other
        val otherColorScheme: ColorScheme = if (colorScheme == ColorScheme.light) ColorScheme.dark else ColorScheme.light
        val isWrongColorScheme0 = asset0?.appearances?.contains { it -> it.isColorScheme(otherColorScheme) } == true
        val isWrongColorScheme1 = asset1?.appearances?.contains { it -> it.isColorScheme(otherColorScheme) } == true
        if (isWrongColorScheme0 && !isWrongColorScheme1) {
            return@l true
        } else if (isWrongColorScheme1 && !isWrongColorScheme0) {
            return@l false
        }

        // Equal
        return@l false
    }
}

/// Appearance as encoded in the asset catalog.
@androidx.annotation.Keep
internal class AssetAppearance: Decodable {
    internal val appearance: String? // e.g., "luminosity"
    internal val value: String? // e.g., "light", "dark"

    internal fun isColorScheme(colorScheme: ColorScheme): Boolean {
        when (colorScheme) {
            ColorScheme.light -> return appearance == "luminosity" && value == "light"
            ColorScheme.dark -> return appearance == "luminosity" && value == "dark"
            else -> return false
        }
    }

    constructor(appearance: String? = null, value: String? = null) {
        this.appearance = appearance
        this.value = value
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        appearance("appearance"),
        value_("value");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "appearance" -> CodingKeys.appearance
                    "value" -> CodingKeys.value_
                    else -> null
                }
            }
        }
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.appearance = container.decodeIfPresent(String::class, forKey = CodingKeys.appearance)
        this.value = container.decodeIfPresent(String::class, forKey = CodingKeys.value_)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<AssetAppearance> {
        override fun init(from: Decoder): AssetAppearance = AssetAppearance(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

/// Additional asset content information as encoded in the asset catalog.
@androidx.annotation.Keep
internal class AssetContentsInfo: Decodable {
    internal val author: String? // e.g. "xcode"
    internal val version: Int? // e.g. 1

    constructor(author: String? = null, version: Int? = null) {
        this.author = author
        this.version = version
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        author("author"),
        version("version");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "author" -> CodingKeys.author
                    "version" -> CodingKeys.version
                    else -> null
                }
            }
        }
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.author = container.decodeIfPresent(String::class, forKey = CodingKeys.author)
        this.version = container.decodeIfPresent(Int::class, forKey = CodingKeys.version)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<AssetContentsInfo> {
        override fun init(from: Decoder): AssetContentsInfo = AssetContentsInfo(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

