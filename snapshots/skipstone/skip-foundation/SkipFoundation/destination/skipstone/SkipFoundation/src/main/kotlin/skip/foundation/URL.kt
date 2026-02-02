// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
// This code is adapted from https://github.com/swiftlang/swift-corelibs-foundation/blob/main/Sources/Foundation/URL.swift which has the following license:

package skip.foundation

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*


import skip.lib.*
import skip.lib.Array
import skip.lib.Set

//===----------------------------------------------------------------------===//
//
// This source file is part of the Swift.org open source project
//
// Copyright (c) 2014 - 2021 Apple Inc. and the Swift project authors
// Licensed under Apache License v2.0 with Runtime Library Exception
//
// See https://swift.org/LICENSE.txt for license information
// See https://swift.org/CONTRIBUTORS.txt for the list of Swift project authors
//
//===----------------------------------------------------------------------===//

typealias NSURL = URL

@androidx.annotation.Keep
class URL: Codable, KotlinConverting<java.net.URI>, SwiftCustomBridged, MutableStruct {
    internal var platformValue: java.net.URI
    private var isDirectoryFlag: Boolean?

    var baseURL: URL?

    constructor(platformValue: java.net.URI, isDirectory: Boolean? = null, baseURL: URL? = null) {
        this.platformValue = platformValue.sref()
        this.isDirectoryFlag = isDirectory
        this.baseURL = baseURL.sref()
    }

    constructor(url: URL) {
        this.platformValue = url.platformValue.sref()
        this.isDirectoryFlag = url.isDirectoryFlag
        this.baseURL = url.baseURL.sref()
    }

    constructor(string: String, relativeTo: URL? = null) {
        val baseURL = relativeTo
        val url_0 = (try { URL(string = string, encodingInvalidCharacters = true) } catch (_: NullReturnException) { null })
        if (url_0 == null) {
            throw NullReturnException()
        }
        this.platformValue = url_0.platformValue.sref()
        this.baseURL = baseURL.sref()
        // Use the same logic as the constructor so that `URL(fileURLWithPath: "/tmp/") == URL(string: "file:///tmp/")`
        val scheme = (baseURL?.platformValue?.scheme ?: this.platformValue.scheme).sref()
        this.isDirectoryFlag = scheme == "file" && string.hasSuffix("/")
    }

    constructor(string: String, encodingInvalidCharacters: Boolean) {
        try {
            this.platformValue = java.net.URI(string) // throws on malformed
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            if (!encodingInvalidCharacters) {
                throw NullReturnException()
            }
            val queryIndex_0 = string.firstIndex(of = '?')
            if (queryIndex_0 == null) {
                throw NullReturnException()
            }
            // As of iOS 17, URLs are automatically encoded if needed. We're only doing the query
            val base = string.prefix(upTo = queryIndex_0)
            val query = string.suffix(from = queryIndex_0 + 1)
            val queryItems = URLQueryItem.from(query)?.map { it ->
                URLQueryItem(name = it.name.addingPercentEncoding(withAllowedCharacters = CharacterSet.urlQueryAllowed) ?: "", value = it.value?.addingPercentEncoding(withAllowedCharacters = CharacterSet.urlQueryAllowed))
            }
            val encodedQuery = URLQueryItem.queryString(from = queryItems)
            try {
                this.platformValue = java.net.URI(base + "?" + (encodedQuery ?: ""))
            } catch (error: Throwable) {
                @Suppress("NAME_SHADOWING") val error = error.aserror()
                throw NullReturnException()
            }
        }
        this.baseURL = null
        this.isDirectoryFlag = this.platformValue.scheme == "file" && string.hasSuffix("/")
    }

    constructor(fileURLWithPath: String, isDirectory: Boolean? = null, relativeTo: URL? = null) {
        val path = fileURLWithPath
        val base = relativeTo
        this.platformValue = java.net.URI("file://" + path) // TODO: escaping
        this.baseURL = base.sref()
        this.isDirectoryFlag = isDirectory ?: path.hasSuffix("/") // TODO: should we hit the file system like NSURL does?
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    internal constructor(fileURLWithFileSystemRepresentation: Any, isDirectory: Boolean, relativeTo: URL? = null, unusedp: Nothing? = null) {
        this.platformValue = java.net.URI("")
        this.baseURL = null
        this.isDirectoryFlag = false
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(fileReferenceLiteralResourceName: String) {
        this.platformValue = java.net.URI("")
        this.baseURL = null
        this.isDirectoryFlag = false
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    internal constructor(resolvingBookmarkData: Data, options: Any? = null, relativeTo: URL? = null, bookmarkDataIsStale: InOut<Boolean>) {
        this.platformValue = java.net.URI("")
        this.baseURL = null
        this.isDirectoryFlag = false
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    internal constructor(resolvingAliasFileAt: URL, options: Any? = null) {
        this.platformValue = java.net.URI("")
        this.baseURL = null
        this.isDirectoryFlag = false
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    internal constructor(resource: URLResource) {
        this.platformValue = java.net.URI("")
        this.baseURL = null
        this.isDirectoryFlag = false
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    internal constructor(parseInput: Any, strategy: Any, unusedp: Nothing? = null) {
        this.platformValue = java.net.URI("")
        this.baseURL = null
        this.isDirectoryFlag = false
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(dataRepresentation: Data, relativeTo: URL?, isAbsolute: Boolean) {
        this.platformValue = java.net.URI("")
        this.baseURL = null
        this.isDirectoryFlag = false
    }

    constructor(from: Decoder) {
        val decoder = from
        val container = decoder.singleValueContainer()
        val assignfrom = URL(string = container.decode(String::class))
        this.platformValue = assignfrom.platformValue
        this.isDirectoryFlag = assignfrom.isDirectoryFlag
        this.baseURL = assignfrom.baseURL
    }

    override fun encode(to: Encoder) {
        val encoder = to
        val container = encoder.singleValueContainer()
        container.encode(absoluteString)
    }

    val description: String
        get() = platformValue.toString()

    /// Converts this URL to a `java.nio.file.Path`.
    fun toPath(): java.nio.file.Path = java.nio.file.Paths.get(absoluteURL.platformValue)

    val host: String?
        get() = absoluteURL.platformValue.host

    fun host(percentEncoded: Boolean = true): String? = absoluteURL.platformValue.host

    val hasDirectoryPath: Boolean
        get() = this.isDirectoryFlag == true

    val path: String
        get() = absoluteURL.platformValue.path ?: ""

    fun path(percentEncoded: Boolean = true): String = (if (percentEncoded) absoluteURL.platformValue.rawPath else absoluteURL.platformValue.path) ?: ""

    val port: Int?
        get() {
            val port = absoluteURL.platformValue.port.sref()
            return if (port == -1) null else port
        }

    val scheme: String?
        get() = absoluteURL.platformValue.scheme

    val query: String?
        get() = absoluteURL.platformValue.query

    fun query(percentEncoded: Boolean = true): String? = if (percentEncoded) absoluteURL.platformValue.rawQuery else absoluteURL.platformValue.query

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val user: String?
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun user(percentEncoded: Boolean = true): String? {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val password: String?
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun password(percentEncoded: Boolean = true): String? {
        fatalError()
    }

    val fragment: String?
        get() = absoluteURL.platformValue.fragment

    fun fragment(percentEncoded: Boolean = true): String? = if (percentEncoded) absoluteURL.platformValue.rawFragment else absoluteURL.platformValue.fragment

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val dataRepresentation: Data
        get() {
            fatalError()
        }

    val standardized: URL
        get() = URL(platformValue = toPath().normalize().toUri())

    val absoluteString: String
        get() = absoluteURL.platformValue.toString()

    val lastPathComponent: String
        get() = pathComponents.last ?: ""

    val pathExtension: String
        get() {
            val lastPathComponent_0 = pathComponents.last
            if (lastPathComponent_0 == null) {
                return ""
            }
            val parts = lastPathComponent_0.split(separator = '.')
            if (parts.count < 2) {
                return ""
            }
            return parts.last!!
        }

    val isFileURL: Boolean
        get() = scheme == "file"

    val pathComponents: Array<String>
        get() {
            val path = path
            if (path.isEmpty) {
                return arrayOf()
            }
            var result = Array<String>()
            var start = path.startIndex
            if (path.first == '/') {
                result.append("/")
                start = path.firstIndex { it -> it != '/' } ?: path.endIndex
            }
            var end = start
            while (end != path.endIndex) {
                end = path[end..Int.max].firstIndex(of = '/') ?: path.endIndex
                if (start != end) {
                    val subpath = String(path[start until end])
                    result.append(subpath)
                }
                start = path[end..Int.max].firstIndex { it -> it != '/' } ?: path.endIndex
                end = start
            }
            // Mimic swift-foundation representing any number of multiple trailing slashes with "/"
            if (path.count > 2 && path.hasSuffix("//")) {
                result.append("/")
            }
            return result
        }

    val relativePath: String
        get() = platformValue.path

    val relativeString: String
        get() = platformValue.toString()

    val standardizedFileURL: URL
        get() = if (isFileURL) standardized else this

    fun standardize() {
        willmutate()
        try {
            assignfrom(standardized)
        } finally {
            didmutate()
        }
    }

    val absoluteURL: URL
        get() {
            val matchtarget_0 = this.baseURL
            if (matchtarget_0 != null) {
                val baseURL = matchtarget_0
                return URL(platformValue = baseURL.platformValue.resolve(platformValue))
            } else {
                return this
            }
        }

    private fun _appendingPathComponent(pathComponent: String): URL {
        var components_0 = (try { URLComponents(url = this, resolvingAgainstBaseURL = true) } catch (_: NullReturnException) { null })
        if (components_0 == null) {
            return this.sref()
        }
        var newPath = components_0.percentEncodedPath
        if (!newPath.hasSuffix("/")) {
            newPath += "/"
        }
        newPath += pathComponent
        components_0.percentEncodedPath = newPath
        return components_0.url(relativeTo = baseURL)!!
    }

    fun appendingPathComponent(pathComponent: String): URL = _appendingPathComponent(pathComponent)

    fun appendingPathComponent(pathComponent: String, isDirectory: Boolean): URL {
        val string = _appendingPathComponent(pathComponent).absoluteString
        return URL(platformValue = java.net.URI(string), isDirectory = isDirectory)
    }

    fun appendPathComponent(pathComponent: String) {
        willmutate()
        try {
            assignfrom(appendingPathComponent(pathComponent))
        } finally {
            didmutate()
        }
    }

    fun appendPathComponent(pathComponent: String, isDirectory: Boolean) {
        willmutate()
        try {
            assignfrom(appendingPathComponent(pathComponent, isDirectory = isDirectory))
        } finally {
            didmutate()
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun appendingPathComponent(pathComponent: String, conformingTo: Any): URL {
        val type = conformingTo
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun appendPathComponent(pathComponent: String, conformingTo: Any) {
        val type = conformingTo
        willmutate()
        try {
            fatalError()
        } finally {
            didmutate()
        }
    }

    fun appendingPathExtension(pathExtension: String): URL {
        if (pathExtension.isEmpty) {
            return this.sref()
        }
        var components_1 = (try { URLComponents(url = this, resolvingAgainstBaseURL = true) } catch (_: NullReturnException) { null })
        if (components_1 == null) {
            return this.sref()
        }
        var newPath = components_1.percentEncodedPath
        if (newPath.isEmpty) {
            return this.sref()
        }
        var endsWithSlash = newPath.hasSuffix("/")
        while (newPath.hasSuffix("/")) {
            newPath = newPath.dropLast(1)
        }
        if (newPath.isEmpty) {
            newPath = components_1.percentEncodedPath
            endsWithSlash = false
        }
        newPath += ".${pathExtension}"
        if (endsWithSlash) {
            newPath += "/"
        }
        components_1.percentEncodedPath = newPath
        return components_1.url(relativeTo = baseURL)!!
    }

    fun appendPathExtension(pathExtension: String) {
        willmutate()
        try {
            assignfrom(appendingPathExtension(pathExtension))
        } finally {
            didmutate()
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun appendingPathExtension(for_: Any, unusedp: Nothing? = null): URL {
        val type = for_
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun appendPathExtension(for_: Any, unusedp: Nothing? = null) {
        val type = for_
        willmutate()
        try {
            fatalError()
        } finally {
            didmutate()
        }
    }

    fun deletingLastPathComponent(): URL {
        var components_2 = (try { URLComponents(url = this, resolvingAgainstBaseURL = true) } catch (_: NullReturnException) { null })
        if (components_2 == null) {
            return this.sref()
        }
        var newPath = components_2.percentEncodedPath.deletingLastPathComponent
        if (!newPath.isEmpty && newPath.last != '/') {
            newPath += "/"
        }
        components_2.percentEncodedPath = newPath
        return components_2.url(relativeTo = baseURL)!!
    }

    fun deleteLastPathComponent() {
        willmutate()
        try {
            assignfrom(deletingLastPathComponent())
        } finally {
            didmutate()
        }
    }

    fun deletingPathExtension(): URL {
        var components_3 = (try { URLComponents(url = this, resolvingAgainstBaseURL = true) } catch (_: NullReturnException) { null })
        if (components_3 == null) {
            return this.sref()
        }
        var newPath = components_3.percentEncodedPath
        if (newPath.count <= 2) {
            return this.sref()
        }
        val hasTrailingSlash = newPath.hasSuffix("/")
        val lastSlash = newPath.lastIndex(of = '/')
        val previousSlash = if (hasTrailingSlash) (if (lastSlash != null) String(newPath[Int.min until lastSlash!!]).lastIndex(of = '/') else null) else lastSlash
        if (newPath.hasSuffix(".")) {
            return this.sref()
        }
        val lastDot_0 = newPath.lastIndex(of = '.')
        if ((lastDot_0 == null) || (previousSlash != null && lastDot_0 <= previousSlash!!)) {
            return this.sref()
        }
        newPath = String(newPath[Int.min until lastDot_0])
        if (newPath == "/") {
            return this.sref()
        }
        if (hasTrailingSlash) {
            newPath += "/"
        }
        components_3.percentEncodedPath = newPath
        return components_3.url(relativeTo = baseURL)!!
    }

    fun deletePathExtension() {
        willmutate()
        try {
            assignfrom(deletingPathExtension())
        } finally {
            didmutate()
        }
    }

    fun resolvingSymlinksInPath(): URL {
        if (!isFileURL) {
            return this.sref()
        }
        val originalPath = toPath()
        //if !java.nio.file.Files.isSymbolicLink(originalPath) {
        //    return self // not a link
        //} else {
        //    let normalized = java.nio.file.Files.readSymbolicLink(originalPath).normalize()
        //    return URL(platformValue: normalized.toUri().toURL())
        //}
        try {
            return URL(platformValue = originalPath.toRealPath().toUri())
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            // this will fail if the file does not exist, but Foundation expects it to return the path itself
            return this.sref()
        }
    }

    fun resolveSymlinksInPath() {
        willmutate()
        try {
            assignfrom(resolvingSymlinksInPath())
        } finally {
            didmutate()
        }
    }

    fun checkResourceIsReachable(): Boolean {
        if (!isFileURL) {
            // “This method is currently applicable only to URLs for file system resources. For other URL types, `false` is returned.”
            return false
        }
        // check whether the resource can be reached by opening and closing a connection
        platformValue.toURL().openConnection().getInputStream().close()
        return true
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun resourceValues(forKeys: Set<URLResourceKey>): URLResourceValues {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun setResourceValues(values: URLResourceValues) {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun removeCachedResourceValue(forKey: URLResourceKey) {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun setTemporaryResourceValue(value: Any, forKey: URLResourceKey) {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun removeAllCachedResourceValues() {
        willmutate()
        try {
            fatalError()
        } finally {
            didmutate()
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun bookmarkData(options: Any, includingResourceValuesForKeys: Set<URLResourceKey>?, relativeTo: URL?): Data {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val resourceBytes: Any
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val lines: Any
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun checkPromisedItemIsReachable(): Boolean {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun promisedItemResourceValues(forKeys: Set<URLResourceKey>): URLResourceValues {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun startAccessingSecurityScopedResource(): Boolean {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun stopAccessingSecurityScopedResource() {
        fatalError()
    }

    override fun kotlin(nocopy: Boolean): java.net.URI = platformValue.sref()

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as URL
        this.platformValue = copy.platformValue
        this.isDirectoryFlag = copy.isDirectoryFlag
        this.baseURL = copy.baseURL
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = URL(this as MutableStruct)

    private fun assignfrom(target: URL) {
        this.platformValue = target.platformValue
        this.isDirectoryFlag = target.isDirectoryFlag
        this.baseURL = target.baseURL
    }

    override fun toString(): String = description

    override fun equals(other: Any?): Boolean {
        if (other !is URL) return false
        return platformValue == other.platformValue && isDirectoryFlag == other.isDirectoryFlag && baseURL == other.baseURL
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, platformValue)
        result = Hasher.combine(result, isDirectoryFlag)
        result = Hasher.combine(result, baseURL)
        return result
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<URL> {

        fun currentDirectory(): URL = URL(fileURLWithPath = System.getProperty("user.dir"), isDirectory = true)

        val homeDirectory: URL
            get() = URL(fileURLWithPath = System.getProperty("user.home"), isDirectory = true)

        val temporaryDirectory: URL
            get() = URL(fileURLWithPath = NSTemporaryDirectory(), isDirectory = true)

        val cachesDirectory: URL
            get() = URL(platformValue = ProcessInfo.processInfo.androidContext.getCacheDir().toURI(), isDirectory = true)

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val applicationDirectory: URL
            get() {
                return fatalError("applicationDirectory unimplemented in Skip")
            }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val libraryDirectory: URL
            get() {
                return fatalError("libraryDirectory unimplemented in Skip")
            }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val userDirectory: URL
            get() {
                return fatalError("desktopDirectory unimplemented in Skip")
            }

        val documentsDirectory: URL
            get() = URL(platformValue = ProcessInfo.processInfo.androidContext.getFilesDir().toURI(), isDirectory = true)

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val desktopDirectory: URL
            get() {
                return fatalError("desktopDirectory unimplemented in Skip")
            }

        val applicationSupportDirectory: URL
            get() = URL(platformValue = ProcessInfo.processInfo.androidContext.getFilesDir().toURI(), isDirectory = true)

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val downloadsDirectory: URL
            get() {
                return fatalError("downloadsDirectory unimplemented in Skip")
            }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val moviesDirectory: URL
            get() {
                return fatalError("moviesDirectory unimplemented in Skip")
            }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val musicDirectory: URL
            get() {
                return fatalError("musicDirectory unimplemented in Skip")
            }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val picturesDirectory: URL
            get() {
                return fatalError("picturesDirectory unimplemented in Skip")
            }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val sharedPublicDirectory: URL
            get() {
                return fatalError("sharedPublicDirectory unimplemented in Skip")
            }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val trashDirectory: URL
            get() {
                return fatalError("trashDirectory unimplemented in Skip")
            }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun bookmarkData(withContentsOf: URL): Data {
            fatalError()
        }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun writeBookmarkData(data: Data, to: URL) {
            fatalError()
        }

        override fun init(from: Decoder): URL = URL(from = from)
    }
}

class URLResource {
    val bundle: Bundle
    val name: String
    val subdirectory: String?
    val locale: Locale

    constructor(name: String, subdirectory: String? = null, locale: Locale = Locale.current, bundle: Bundle = Bundle.main) {
        this.bundle = bundle
        this.name = name
        this.subdirectory = subdirectory
        this.locale = locale
    }

    override fun equals(other: Any?): Boolean {
        if (other !is URLResource) return false
        return bundle == other.bundle && name == other.name && subdirectory == other.subdirectory && locale == other.locale
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, bundle)
        result = Hasher.combine(result, name)
        result = Hasher.combine(result, subdirectory)
        result = Hasher.combine(result, locale)
        return result
    }

    @androidx.annotation.Keep
    companion object {
    }
}

class URLResourceKey: RawRepresentable<String> {
    override val rawValue: String

    constructor(rawValue: String) {
        this.rawValue = rawValue
    }

    constructor(rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is URLResourceKey) return false
        return rawValue == other.rawValue
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, rawValue)
        return result
    }

    @androidx.annotation.Keep
    companion object {
    }
}

@Suppress("MUST_BE_INITIALIZED")
class URLResourceValues: MutableStruct {
    var allValues: Dictionary<URLResourceKey, Any>
        get() = field.sref({ this.allValues = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(allValues: Dictionary<URLResourceKey, Any>) {
        this.allValues = allValues
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = URLResourceValues(allValues)

    @androidx.annotation.Keep
    companion object {
    }
}

