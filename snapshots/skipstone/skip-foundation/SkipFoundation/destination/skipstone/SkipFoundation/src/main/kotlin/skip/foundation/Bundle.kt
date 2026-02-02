package skip.foundation

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

open class Bundle: SwiftCustomBridged {

    private val location: LocalizedStringResource.BundleDescription
    val bundleURL: URL
    val bundleIdentifier: String?


    internal open val isLocalizedBundle: Boolean
        get() = bundleURL.absoluteString.hasSuffix(Companion.lprojExtension + "/")

    open val description: String
        get() = location.description

    constructor(location: LocalizedStringResource.BundleDescription) {
        this.location = location
        AssetURLProtocol.register() // ensure that we can handle the "asset:/" URL protocol
        when (location) {
            is LocalizedStringResource.BundleDescription.AtURLCase -> {
                val url = location.associated0
                this.bundleURL = url.sref()
                this.bundleIdentifier = null
            }
            is LocalizedStringResource.BundleDescription.MainCase -> {
                val identifer = Companion.packageName(forClassName = applicationInfo.className)
                this.bundleIdentifier = identifer
                this.bundleURL = Companion.createBundleURL(forPackage = identifer)
            }
            is LocalizedStringResource.BundleDescription.ForClassCase -> {
                val cls = location.associated0
                val identifer = Companion.packageName(forClassName = cls.java.name)
                this.bundleIdentifier = identifer
                this.bundleURL = Companion.createBundleURL(forPackage = identifer)
            }
        }
    }

    open val resourceURL: URL?
        get() = bundleURL // note: this isn't how traditional bundles work

    constructor(path: String): this(location = LocalizedStringResource.BundleDescription.atURL(URL(fileURLWithPath = path))) {
    }

    constructor(url: URL): this(location = LocalizedStringResource.BundleDescription.atURL(url)) {
    }

    constructor(for_: AnyClass): this(location = LocalizedStringResource.BundleDescription.forClass(for_)) {
    }

    constructor(): this(location = LocalizedStringResource.BundleDescription.forClass(Bundle::class)) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(identifier: String, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null): this(location = LocalizedStringResource.BundleDescription.main) {
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Bundle) {
            return false
        }
        val lhs = this
        val rhs = other
        return lhs.location == rhs.location
    }

    override fun hashCode(): Int {
        var hasher = Hasher()
        hash(into = InOut<Hasher>({ hasher }, { hasher = it }))
        return hasher.finalize()
    }
    open fun hash(into: InOut<Hasher>) {
        val hasher = into
        hasher.value.combine(location.hashCode())
    }

    /// Creates a relative path to the given bundle URL
    private fun relativeBundleURL(path: String, validate: Boolean): URL? {
        val relativeURL = resourceURL?.appendingPathComponent(path.addingPercentEncoding(withAllowedCharacters = CharacterSet.urlQueryAllowed) ?: path)
        if (validate && (relativeURL != null)) {
            if (relativeURL.scheme == AssetURLProtocol.scheme) {
                // check the AssetManager to see if the URL exists
                val assets = ProcessInfo.processInfo.androidContext.resources.assets.sref()
                val assetDir_0 = relativeURL.deletingLastPathComponent().path.removingPercentEncoding?.trim("/"[0])
                if (assetDir_0 == null) {
                    return null
                }
                val assetBasename_0 = relativeURL.lastPathComponent.removingPercentEncoding
                if (assetBasename_0 == null) {
                    return null
                }
                val elements_0 = assets.list(assetDir_0)
                if ((elements_0 == null) || (elements_0.count() <= 0)) {
                    return null
                }
                if (!elements_0.contains(assetBasename_0)) {
                    // if the parent folder does not contain the basename, then the asset is not present
                    return null
                }
            } else if (relativeURL.isFileURL) {
                if (!FileManager.default.fileExists(atPath = relativeURL.path)) {
                    return null // the file or directory does not exist
                }
            } else {
                // unknown protocol … what to do?
            }
        }
        return relativeURL.sref()
    }

    open fun url(forResource: String? = null, withExtension: String? = null, subdirectory: String? = null, localization: String? = null): URL? {
        // similar behavior to: https://github.com/apple/swift-corelibs-foundation/blob/69ab3975ea636d1322ad19bbcea38ce78b65b26a/CoreFoundation/PlugIn.subproj/CFBundle_Resources.c#L1114
        var res = forResource ?: ""
        if ((withExtension != null) && !withExtension.isEmpty) {
            // TODO: If `forResource` is nil, we are expected to find the first file in the bundle whose extension matches
            if (!withExtension.hasPrefix(".")) {
                res += "."
            }
            res += withExtension
        } else if (res.isEmpty) {
            return null
        }
        if (localization != null) {
            res = localization + Companion.lprojExtension + "/" + res
        }
        if (subdirectory != null) {
            res = subdirectory + "/" + res
        }

        return relativeBundleURL(path = res, validate = true)
    }

    open fun urls(forResourcesWithExtension: String?, subdirectory: String? = null, localization: String? = null): Array<URL>? {
        val ext = forResourcesWithExtension
        val subpath = subdirectory
        val localizationName = localization
        var filteredResources = resourcesIndex.sref()
        localizationName?.let { localization ->
            filteredResources = filteredResources.filter { it -> it.hasPrefix("${localization}${Companion.lprojExtension}/") }
        }
        if (subpath != null) {
            filteredResources = filteredResources.filter { it -> it.hasPrefix("${subpath}/") }
        }
        if (ext != null) {
            val extWithDot = if (ext.hasPrefix(".")) ext else ".${ext}"
            filteredResources = filteredResources.filter { it -> it.hasSuffix(extWithDot) }
        }
        val resourceURLs = filteredResources.compactMap { it -> relativeBundleURL(path = it, validate = true) }
        return (if (resourceURLs.isEmpty) null else resourceURLs).sref()
    }

    open fun path(forResource: String? = null, ofType: String? = null, inDirectory: String? = null, forLocalization: String? = null): String? {
        return url(forResource = forResource, withExtension = ofType, subdirectory = inDirectory, localization = forLocalization)?.path
    }

    open fun paths(forResourcesOfType: String?, inDirectory: String? = null, forLocalization: String? = null): Array<String> {
        val ext = forResourcesOfType
        val subpath = inDirectory
        val localizationName = forLocalization
        return (urls(forResourcesWithExtension = ext, subdirectory = subpath, localization = localizationName)?.compactMap { it -> it.path } ?: arrayOf()).sref()
    }

    /// An index of all the assets in the AssetManager relative to the resourceURL for the AssetManager
    open var resourcesIndex: Array<String>
        get() {
            if (!::resourcesIndexstorage.isInitialized) {
                resourcesIndexstorage = linvoke l@{ ->
                    val basePath = (resourceURL?.path ?: "").trim("/"[0]) // AssetManager paths are relative, not absolute
                    val resourcePaths = Companion.listAssets(in_ = basePath, recursive = true)
                    val resourceIndexPaths = resourcePaths.map { it -> it.dropFirst(basePath.count + 1) }
                    return@l resourceIndexPaths
                }
            }
            return resourcesIndexstorage.sref({ this.resourcesIndex = it })
        }
        set(newValue) {
            resourcesIndexstorage = newValue.sref()
        }
    private lateinit var resourcesIndexstorage: Array<String>

    /// We default to en as the development localization
    open val developmentLocalization: String
        get() = "en"

    /// Identify the Bundle's localizations by the presence of a `LOCNAME.lproj/` folder in index of the root of the resources folder
    open var localizations: Array<String>
        get() {
            if (!::localizationsstorage.isInitialized) {
                localizationsstorage = linvoke l@{ ->
                    return@l resourcesIndex
                        .compactMap({ it -> it.components(separatedBy = "/").first })
                        .filter({ it -> it.hasSuffix(Companion.lprojExtension) })
                        .filter({ it -> it != "base.lproj" })
                        .map({ it -> it.dropLast(Companion.lprojExtension.count) })
                        .distinctValues()
                }
            }
            return localizationsstorage.sref({ this.localizations = it })
        }
        set(newValue) {
            localizationsstorage = newValue.sref()
        }
    private lateinit var localizationsstorage: Array<String>

    /// The localized strings tables for this bundle
    private var localizedTables: MutableMap<String, MutableMap<String, Triple<String, String, MarkdownNode?>>> = mutableMapOf()
        get() = field.sref({ this.localizedTables = it })
        set(newValue) {
            field = newValue.sref()
        }

    open fun localizedString(forKey: String, value: String?, table: String?, locale: Locale? = null): String {
        val key = forKey
        val tableName = table
        return localizedInfo(forKey = key, value = value, table = tableName, locale = locale)?.first ?: value ?: key
    }

    /// Check for the localized key for the given Locale's localized bundle, falling back to the "base.lproj" bundle and then just checking the top-level bundle.
    /// The result will be cached for future lookup.
    open fun localizedInfo(forKey: String, value: String?, table: String?, locale: Locale?): Triple<String, String, MarkdownNode?> {
        val key = forKey
        val tableName = table
        if (this.isLocalizedBundle) {
            // when the bundle is itself already a localized Bundle (e.g., from a top-level bundle the use gets "fr.lproj", then we ignore the local parameter and instead look it up directly in the current bundle
            this.lookupLocalizableString(forKey = key, value = value, table = tableName, fallback = true)?.let { info ->
                return info.sref()
            }
        }

        // attempt to get the given locale's bundle, fall back to the "base.lproj" locale, and then fall back to self
        localizedBundle(locale = locale ?: Locale.current)?.lookupLocalizableString(forKey = key, value = value, table = tableName)?.let { info ->
            return info.sref()
        }

        // attempt to look up the string in the baseLocale ("base.lproj"), and fall back to the current bundle if it is not present
        (localizedBundle(locale = Locale.baseLocale) ?: this).lookupLocalizableString(forKey = key, value = value, table = tableName)?.let { info ->
            return info.sref()
        }

        // create a fallback key if it could not be found in any of the localized bundles
        val info = this.lookupLocalizableString(forKey = key, value = value, table = tableName, fallback = true)!!
        return info.sref()
    }

    /// Localize the given string, returning a string suitable for Kotlin/Java formatting rather than Swift formatting.
    private fun lookupLocalizableString(forKey: String, value: String?, table: String?, fallback: Boolean = false): Triple<String, String, MarkdownNode?>? {
        val key = forKey
        val tableName = table
        return synchronized(this) l@{ ->
            val table = tableName ?: "Localizable"
            var locTable = localizedTables[table].sref()
            if (locTable == null) {
                val resURL = url(forResource = table, withExtension = "strings")
                val resTable = if (resURL == null) null else try { PropertyListSerialization.propertyList(from = Data(contentsOf = resURL!!), format = null) } catch (_: Throwable) { null }
                locTable = Companion.stringFormatsTable(from = resTable)
                localizedTables[table] = locTable!!.sref()
            }
            locTable?.get(key).sref()?.let { formats ->
                return@l formats
            }

            // If we have specified a fallback bundle (e.g., for a default localization), then call into that
            if (value != null) {
                // We can't cache this in case different values are passed on different calls
                return@l Triple(value, value.kotlinFormatString, MarkdownNode.from(string = value))
            } else if (fallback) {
                // only cache the miss if we specify fallback; this is so we can cache this only for the top-level bundle
                val formats = Triple(key, key.kotlinFormatString, MarkdownNode.from(string = key))
                locTable!![key] = formats.sref()
                return@l formats
            } else {
                return@l null
            }
        }
    }

    /// The individual loaded bundles by locale
    private var localizedBundles: MutableMap<Locale, Bundle> = mutableMapOf()
        get() = field.sref({ this.localizedBundles = it })
        set(newValue) {
            field = newValue.sref()
        }

    /// Looks up the Bundle for the given locale and returns it, caching the result in the process.
    open fun localizedBundle(locale: Locale): Bundle {
        return synchronized(this) l@{ ->
            this.localizedBundles[locale].sref()?.let { cached ->
                return@l cached
            }

            var locBundle: Bundle? = null
            // for each identifier, attempt to load the Localizable.strings to see if it exists
            for (localeid in locale.localeSearchTags.sref()) {
                if (locBundle == null) {
                    this.url(forResource = "Localizable", withExtension = "strings", subdirectory = null, localization = localeid)?.let { locstrURL ->
                        (try { (try { Bundle(url = locstrURL.deletingLastPathComponent()) } catch (_: NullReturnException) { null }) } catch (_: Throwable) { null })?.let { locBundleLocal ->
                            locBundle = locBundleLocal
                            //break // The feature "break continue in inline lambdas" is experimental and should be enabled explicitly
                        }
                    }
                }
            }

            // cache the result of the lookup
            val resBundle = locBundle ?: this
            this.localizedBundles[locale] = resBundle
            return@l resBundle
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val preferredLocalizations: Array<String>
        get() {
            fatalError()
        }

    open val infoDictionary: Dictionary<String, Any>?
        get() {
            // infoDictionary only supported for main bundle currently
            if (location == LocalizedStringResource.BundleDescription.main) {
                return Companion.mainInfoDictionary
            } else {
                return null
            }
        }

    open val localizedInfoDictionary: Dictionary<String, Any>?
        get() {
            // currently no support for localized info on Android
            return infoDictionary
        }

    open fun object_(forInfoDictionaryKey: String): Any? {
        val key = forInfoDictionaryKey
        return infoDictionary?.get(key).sref()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val executableURL: URL?
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun url(forAuxiliaryExecutable: String): URL? {
        val executableName = forAuxiliaryExecutable
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val privateFrameworksURL: URL?
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val sharedFrameworksURL: URL?
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val sharedSupportURL: URL?
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val builtInPlugInsURL: URL?
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val appStoreReceiptURL: URL?
        get() {
            fatalError()
        }

    open val bundlePath: String
        get() = bundleURL.path

    open val resourcePath: String?
        get() {
            return resourceURL?.path
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val executablePath: String?
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun path(forAuxiliaryExecutable: String): String? {
        val executableName = forAuxiliaryExecutable
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val privateFrameworksPath: String?
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val sharedFrameworksPath: String?
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val sharedSupportPath: String?
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val builtInPlugInsPath: String?
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun load(): Boolean {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val isLoaded: Boolean
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun unload(): Boolean {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun preflight() = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun loadAndReturnError() = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun classNamed(className: String): AnyClass? {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val principalClass: AnyClass?
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val executableArchitectures: Array<java.lang.Number>?
        get() {
            fatalError()
        }

    @androidx.annotation.Keep
    companion object: CompanionClass() {
        override val main = Bundle(location = LocalizedStringResource.BundleDescription.main)

        /// Each package will generate its own `Bundle.module` extension to access the local bundle.
        override val module: Bundle
            get() = _bundleModule
        private val _bundleModule = Bundle(for_ = Bundle::class)
        private val lprojExtension = ".lproj" // _CFBundleLprojExtensionWithDot

        /// Convert `showcase.module.AndroidAppMain` into `showcase.module`
        private fun packageName(forClassName: String?): String {
            // applicationInfo.className is nil when testing on the Android emulator
            val className = forClassName ?: "skip.foundation.Bundle"
            return className.split(separator = '.').dropLast().joined(separator = ".")
        }

        /// Convert `showcase.module` into `asset:/showcase/module/Resources`
        private fun createBundleURL(forPackage: String): URL {
            val packageName = forPackage
            val parts = packageName.replace(".", "/")
            val url = URL(string = AssetURLProtocol.scheme + ":/" + parts + "/Resources")
            return url.sref()
        }

        override fun url(forResource: String?, withExtension: String?, subdirectory: String?, in_: URL): URL? {
            val name = forResource
            val ext = withExtension
            val subpath = subdirectory
            val bundleURL = in_
            return (try { Bundle(url = bundleURL) } catch (_: NullReturnException) { null })?.url(forResource = name, withExtension = ext, subdirectory = subpath)
        }

        override fun urls(forResourcesWithExtension: String?, subdirectory: String?, in_: URL): Array<URL>? {
            val ext = forResourcesWithExtension
            val subpath = subdirectory
            val bundleURL = in_
            return (try { Bundle(url = bundleURL) } catch (_: NullReturnException) { null })?.urls(forResourcesWithExtension = ext, subdirectory = subpath)
        }

        override fun path(forResource: String?, ofType: String?, inDirectory: String): String? {
            val name = forResource
            val ext = ofType
            val bundlePath = inDirectory
            return (try { Bundle(path = bundlePath) } catch (_: NullReturnException) { null })?.path(forResource = name, ofType = ext)
        }

        override fun paths(forResourcesOfType: String?, inDirectory: String): Array<String> {
            val ext = forResourcesOfType
            val bundlePath = inDirectory
            return ((try { Bundle(path = bundlePath) } catch (_: NullReturnException) { null })?.paths(forResourcesOfType = ext) ?: arrayOf()).sref()
        }

        override fun listAssets(in_: String, recursive: Boolean): Array<String> {
            val folderName = in_
            val am = ProcessInfo.processInfo.androidContext.resources.assets.sref()
            val contents_0 = am.list(folderName)
            if (contents_0 == null) {
                return arrayOf()
            }
            val contentArray = Array(contents_0.toList()).map({ it -> folderName + "/" + it })
            if (!recursive) {
                return contentArray.sref()
            }
            return (contentArray + contentArray.flatMap({ it -> listAssets(in_ = it, recursive = recursive) })).sref()
        }

        private fun stringFormatsTable(from: Dictionary<String, String>?): MutableMap<String, Triple<String, String, MarkdownNode?>> {
            val table = from
            if (table == null) {
                return mutableMapOf()
            }
            // We cache both the format string and its Kotlin-ized and parsed markdown version so that `localizedKotlinFormatInfo`
            // doesn't have to do the conversion each time and is fast for use in `SwiftUI.Text` implicit localization
            val formatsTable = mutableMapOf<String, Triple<String, String, MarkdownNode?>>()
            for ((key, value) in table.sref()) {
                formatsTable[key] = Triple(value, value.kotlinFormatString, MarkdownNode.from(string = value))
            }
            return formatsTable.sref()
        }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun preferredLocalizations(from: Array<String>, forPreferences: Array<String>? = null): Array<String> {
            val localizationsArray = from
            val preferencesArray = forPreferences
            fatalError()
        }

        /// The global Android context
        private val androidContext: android.content.Context
            get() = ProcessInfo.processInfo.androidContext

        private val packageManager: android.content.pm.PackageManager
            get() = androidContext.getPackageManager()

        private val packageInfo: android.content.pm.PackageInfo
            get() = packageManager.getPackageInfo(androidContext.getPackageName(), android.content.pm.PackageManager.GET_META_DATA)

        private val applicationInfo: android.content.pm.ApplicationInfo
            get() = androidContext.getApplicationInfo()

        /// The `Bundle.main.infoDictionary` with keys synthesized from various Android metadata accessors
        private val mainInfoDictionary: Dictionary<String, Any>
            get() {
                val packageManager = this.packageManager.sref()
                val packageInfo = this.packageInfo.sref()
                val applicationInfo = this.applicationInfo.sref()

                var info = Dictionary<String, Any>()
                info["CFBundleIdentifier"] = Companion.androidContext.getPackageName()
                info["CFBundleName"] = packageManager.getApplicationLabel(applicationInfo).toString()
                info["CFBundleDisplayName"] = packageManager.getApplicationLabel(applicationInfo).toString()
                info["CFBundleShortVersionString"] = (packageInfo.versionName ?: "").sref()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    info["CFBundleVersion"] = packageInfo.longVersionCode.toString()
                } else {
                    info["CFBundleVersion"] = packageInfo.versionCode.toString()
                }
                info["CFBundleExecutable"] = androidContext.getPackageName()
                info["DTPlatformName"] = "android"
                info["DTPlatformVersion"] = android.os.Build.VERSION.SDK_INT.toString()
                info["DTSDKName"] = "android" + android.os.Build.VERSION.SDK_INT.toString()
                info["BuildMachineOSBuild"] = android.os.Build.FINGERPRINT.sref()
                info["MinimumOSVersion"] = applicationInfo.minSdkVersion?.toString()
                info["CFBundleLocalizations"] = Bundle.main.localizations.sref()

                return info
            }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val allBundles: Array<Bundle>
            get() {
                fatalError()
            }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val allFrameworks: Array<Bundle>
            get() {
                fatalError()
            }
    }
    open class CompanionClass {
        open val main
            get() = Bundle.main
        internal open val module: Bundle
            get() = Bundle.module
        open fun url(forResource: String?, withExtension: String? = null, subdirectory: String? = null, in_: URL): URL? = Bundle.url(forResource = forResource, withExtension = withExtension, subdirectory = subdirectory, in_ = in_)
        open fun urls(forResourcesWithExtension: String?, subdirectory: String?, in_: URL): Array<URL>? = Bundle.urls(forResourcesWithExtension = forResourcesWithExtension, subdirectory = subdirectory, in_ = in_)
        open fun path(forResource: String?, ofType: String?, inDirectory: String): String? = Bundle.path(forResource = forResource, ofType = ofType, inDirectory = inDirectory)
        open fun paths(forResourcesOfType: String?, inDirectory: String): Array<String> = Bundle.paths(forResourcesOfType = forResourcesOfType, inDirectory = inDirectory)
        internal open fun listAssets(in_: String, recursive: Boolean): Array<String> = Bundle.listAssets(in_ = in_, recursive = recursive)
    }
}

fun NSLocalizedString(key: String, tableName: String? = null, bundle: Bundle? = Bundle.main, value: String? = null, comment: String): String = (bundle ?: Bundle.main).localizedString(forKey = key, value = value, table = tableName, locale = Locale.current)

/// A localized string bundle with the key, the kotlin format, and optionally a markdown node
class LocalizedStringInfo {
    val string: String
    val kotlinFormat: String
    val markdownNode: MarkdownNode?

    constructor(string: String, kotlinFormat: String, markdownNode: MarkdownNode? = null) {
        this.string = string
        this.kotlinFormat = kotlinFormat
        this.markdownNode = markdownNode
    }

    @androidx.annotation.Keep
    companion object {
    }
}


typealias URLProtocol = java.net.URLStreamHandlerFactory

open class AssetURLProtocol: java.net.URLStreamHandlerFactory {

    private constructor(): super() {
    }

    override fun createURLStreamHandler(protocol: String): java.net.URLStreamHandler? {
        if (protocol == AssetURLProtocol.scheme) {
            return AssetStreamHandler()
        } else {
            return null
        }
    }

    internal open class AssetStreamHandler: java.net.URLStreamHandler {
        internal constructor() {
        }

        override fun openConnection(url: java.net.URL): java.net.URLConnection = AssetURLConnection(url = url)

        internal open class AssetURLConnection: java.net.URLConnection {
            internal constructor(url: java.net.URL): super(url) {
            }

            override fun connect() {
                // No-op
            }

            override fun getInputStream(): java.io.InputStream {
                // e.g.: asset:/skip/path/file.ext
                var urlPath = this.url.path.sref()
                while (urlPath.startsWith("/")) {
                    urlPath = urlPath.substring(1) // trim initial "/"
                }
                urlPath = (urlPath.removingPercentEncoding ?: urlPath).sref()
                // removingPercentEncoding does not always convert "+" to "%2B" to a space, which the Android AssetManager needs to be able to find the file
                urlPath = urlPath.replacingOccurrences(of = "+", with = "%2B")
                urlPath = urlPath.replacingOccurrences(of = "%2B", with = " ")
                val assetManager = ProcessInfo.processInfo.androidContext.resources.assets.sref()
                // android.content.res.AssetManager$AssetInputStream
                val stream = assetManager.open(urlPath)
                return stream.sref()
            }
        }
    }

    @androidx.annotation.Keep
    companion object: CompanionClass() {
        /// The URL scheme that this protocol handles
        override var scheme = "asset"

        private var registered = false

        override fun register() {
            if (registered) {
                return
            }
            registered = true

            java.net.URL.setURLStreamHandlerFactory(AssetURLProtocol())
            // cannot ever call this twice in the same JVM, or else:
            //java.net.URL.setURLStreamHandlerFactory(AssetStreamHandlerFactory()) // java.lang.Error: factory already defined
        }
    }
    open class CompanionClass {
        open var scheme
            get() = AssetURLProtocol.scheme
            set(newValue) {
                AssetURLProtocol.scheme = newValue
            }
        open fun register() = AssetURLProtocol.register()
    }
}

