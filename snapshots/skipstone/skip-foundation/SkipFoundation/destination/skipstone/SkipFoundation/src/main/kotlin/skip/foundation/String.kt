// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
// This code is adapted from https://github.com/swiftlang/swift-corelibs-foundation/blob/main/Sources/Foundation/URL.swift which has the following license:

package skip.foundation

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*


import skip.lib.*
import skip.lib.Array

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


typealias NSString = kotlin.String
fun NSString(string: String): kotlin.String = string

fun strlen(string: String): Int = string.count

fun strncmp(str1: String, str2: String): Int = if (str1.lowercase() == str2.lowercase()) 0 else 1

fun String.appending(other: String): String = this + other

val String.capitalized: String
    get() {
        return split(separator = ' ', omittingEmptySubsequences = false)
            .joinToString(separator = " ") { it ->
                it.replaceFirstChar { it -> it.titlecase() }
            }
    }

val String.deletingLastPathComponent: String
    get() {
        val lastSlash_0 = lastIndex(of = '/')
        if (lastSlash_0 == null) {
            // No slash, entire string is deleted
            return ""
        }
        val lastNonSlash_0 = String(this[Int.min until lastSlash_0]).lastIndex(where = { it -> it != '/' })
        if (lastNonSlash_0 == null) {
            // String consists entirely of slashes, return a single slash
            return "/"
        }

        val hasTrailingSlash = (lastSlash_0 == index(before = endIndex))
        if (!hasTrailingSlash) {
            // No trailing slash, return up to (including) the last non-slash character
            return String(this[Int.min..lastNonSlash_0])
        }
        val previousSlash_0 = String(this[Int.min until lastNonSlash_0]).lastIndex(of = '/')
        if (previousSlash_0 == null) {
            // No prior slash, deleting the last component removes the entire string (e.g. "path/")
            return ""
        }
        val previousNonSlash_0 = String(this[Int.min until previousSlash_0]).lastIndex(where = { it -> it != '/' })
        if (previousNonSlash_0 == null) {
            // String is an absolute path with a single component (e.g. "/path/" or "//path/")
            return "/"
        }

        return String(this[Int.min..previousNonSlash_0])
    }

fun String.replacingOccurrences(of: String, with: String): String {
    val search = of
    val replacement = with
    return replace(search, replacement)
}

fun String.components(separatedBy: String): Array<String> {
    val separator = separatedBy
    return Array(split(separator, ignoreCase = false))
}

private fun String.compare(aString: String, strength: Int, locale: Locale = Locale.current): ComparisonResult {
    val collator = java.text.Collator.getInstance(locale.platformValue)
    collator.setStrength(strength)
    val result = collator.compare(this, aString)
    if (result < 0) {
        return ComparisonResult.orderedAscending
    } else if (result > 0) {
        return ComparisonResult.orderedDescending
    } else {
        return ComparisonResult.orderedSame
    }
}

fun String.localizedCompare(string: String): ComparisonResult {
    // only SECONDARY and above differences are considered significant during comparison. The assignment of strengths to language features is locale dependant. A common example is for different accented forms of the same base letter ("a" vs "ä") to be considered a SECONDARY difference.
    return compare(string, strength = java.text.Collator.SECONDARY)
}

fun String.localizedCaseInsensitiveCompare(string: String): ComparisonResult {
    // only TERTIARY and above differences are considered significant during comparison. The assignment of strengths to language features is locale dependant. A common example is for case differences ("a" vs "A") to be considered a TERTIARY difference.
    return compare(string, strength = java.text.Collator.TERTIARY)
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
fun String.localizedStandardCompare(string: String): ComparisonResult {
    return fatalError("unsupported")
}

fun String.trimmingCharacters(in_: CharacterSet): String {
    val set = in_
    return trim { it -> set.platformValue.contains(UInt(it.code)) }
}

fun String.addingPercentEncoding(withAllowedCharacters: CharacterSet): String? {
    val allowedCharacters = withAllowedCharacters
    return UrlEncoderUtil.encode(this, allowedCharacters.platformValue, spaceToPlus = true)
}

val String.removingPercentEncoding: String?
    get() = UrlEncoderUtil.decode(this, plusToSpace = true)

fun String.range(of: String): IntRange? {
    val searchString = of
    val startIndex = indexOf(searchString)
    return if (startIndex != -1) startIndex until (startIndex + searchString.count) else null
}

val String.utf8Data: Data
    get() = data(using = StringEncoding.utf8) ?: Data()

fun String.data(using: StringEncoding, allowLossyConversion: Boolean = true): Data? {
    if (using == StringEncoding.utf16) {
        return Data(this.utf16) // Darwin is little-endian while Java is big-endian
    } else if (using == StringEncoding.utf32) {
        return Data(this.utf32) // Darwin is little-endian while Java is big-endian
    } else {
        val bytes = toByteArray(using.rawValue)
        return Data(platformValue = bytes)
    }
}

val String.utf8: Array<UByte>
    get() {
        // TODO: there should be a faster way to convert a string to a UInt8 array
        return Array(toByteArray(StringEncoding.utf8.rawValue).toUByteArray())
    }

val String.utf16: Array<UByte>
    get() {
        // Darwin is little-endian while Java is big-endian
        // encoding difference with UTF16: https://github.com/google/j2objc/issues/403
        // so we manually use utf16LittleEndian (no BOM) then add back in the byte-order mark (the first two bytes)
        return arrayOf(UByte(0xFF), UByte(0xFE)) + Array(toByteArray(StringEncoding.utf16LittleEndian.rawValue).toUByteArray())
    }

val String.utf32: Array<UByte>
    get() {
        // manually use utf32LittleEndian (no BOM) then add back in the byte-order mark (the first two bytes)
        return arrayOf(UByte(0xFF), UByte(0xFE), UByte(0x00), UByte(0x00)) + Array(toByteArray(StringEncoding.utf32LittleEndian.rawValue).toUByteArray())
    }

val String.unicodeScalars: Array<UByte>
    get() = Array(toByteArray(StringEncoding.utf8.rawValue).toUByteArray())

fun String.write(to: URL, atomically: Boolean, encoding: StringEncoding) {
    val url = to
    val useAuxiliaryFile = atomically
    val enc = encoding
    var opts: Array<java.nio.file.StandardOpenOption> = arrayOf()
    opts.append(java.nio.file.StandardOpenOption.CREATE)
    opts.append(java.nio.file.StandardOpenOption.WRITE)
    if (useAuxiliaryFile) {
        opts.append(java.nio.file.StandardOpenOption.DSYNC)
        opts.append(java.nio.file.StandardOpenOption.SYNC)
    }
    java.nio.file.Files.write(platformFilePath(for_ = url), this.data(using = enc)?.platformValue, *(opts.toList().toTypedArray()))
}

fun String.write(toFile: String, atomically: Boolean, encoding: StringEncoding) {
    val path = toFile
    val useAuxiliaryFile = atomically
    val enc = encoding
    var opts: Array<java.nio.file.StandardOpenOption> = arrayOf()
    opts.append(java.nio.file.StandardOpenOption.CREATE)
    opts.append(java.nio.file.StandardOpenOption.WRITE)
    if (useAuxiliaryFile) {
        opts.append(java.nio.file.StandardOpenOption.DSYNC)
        opts.append(java.nio.file.StandardOpenOption.SYNC)
    }
    java.nio.file.Files.write(platformFilePath(for_ = path), this.data(using = enc)?.platformValue, *(opts.toList().toTypedArray()))
}

fun String(data: Data, encoding: StringEncoding): String? = (java.lang.String(data.platformValue, encoding.rawValue) as kotlin.String?).sref()

fun String(bytes: Array<UByte>, encoding: StringEncoding): String? {
    val byteArray = ByteArray(size = bytes.count) l@{ it -> return@l bytes[it].toByte() }
    return byteArray.toString(encoding.rawValue)
}

fun String(contentsOf: URL): String = contentsOf.absoluteURL.platformValue.toURL().readText()

fun String(contentsOf: URL, encoding: StringEncoding): String = (java.lang.String(Data(contentsOf = contentsOf).platformValue, encoding.rawValue) as kotlin.String).sref()

private fun localizationValue(keyAndValue: StringLocalizationValue, bundle: Bundle, defaultValue: String?, tableName: String?, locale: Locale?): String {
    val key = keyAndValue.patternFormat // interpolated string: "Hello \(name)" keyed as: "Hello %@"
    val (_, locfmt, _) = bundle.localizedInfo(forKey = key, value = defaultValue, table = tableName, locale = locale)
    // re-interpret the placeholder strings in the resulting localized string with the string interpolation's values
    val replaced = locfmt.format(*keyAndValue.stringInterpolation.values.toTypedArray())
    return replaced
}

fun String(localized: LocalizedStringResource): String {
    val resource = localized
    return localizationValue(keyAndValue = resource.keyAndValue, bundle = resource.bundle?.bundle ?: Bundle.main, defaultValue = resource.defaultValue?.patternFormat?.kotlinFormatString, tableName = resource.table, locale = resource.locale)
}

/// e.g.: `String(localized: "Done", table: nil, bundle: Bundle.module, locale: Locale(identifier: "en"), comment: nil)`
fun String(localized: StringLocalizationValue, table: String? = null, bundle: Bundle? = null, locale: Locale = Locale.current, comment: String? = null): String {
    val keyAndValue = localized
    return localizationValue(keyAndValue = keyAndValue, bundle = bundle ?: Bundle.main, defaultValue = null, tableName = table, locale = locale)
}

fun String(localized: String, defaultValue: String? = null, table: String? = null, bundle: Bundle? = null, locale: Locale? = null, comment: String? = null): String {
    val key = localized
    return (bundle ?: Bundle.main).localizedString(forKey = key, value = defaultValue, table = table, locale = locale) ?: defaultValue ?: key
}

