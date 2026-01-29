package kickbase.core

import skip.lib.*
import skip.lib.Array

import skip.foundation.*

// Helper to extract content safely without exposing Swift.String.Index to Kotlin/Skip

fun String.substringAfter(marker: String): String? {
    // Safer implementation effectively using split/components which transpiles well
    val parts = this.components(separatedBy = marker)
    if (parts.count <= 1) {
        return null
    }
    // Join the rest in case the marker appears multiple times
    return parts.dropFirst().joined(separator = marker)
}

fun String.substringBefore(marker: String): String? {
    val parts = this.components(separatedBy = marker)
    if (parts.count <= 1) {
        return null
    }
    return parts.first
}

fun String.substringBetween(startMarker: String, endMarker: String): String? {
    val after_0 = this.substringAfter(startMarker)
    if (after_0 == null) {
        return null
    }
    return after_0.substringBefore(endMarker)
}

fun String.splitBy(separator: String): Array<String> = this.components(separatedBy = separator)

fun Substring.substringAfter(marker: String): String? = String(this).substringAfter(marker)

fun Substring.substringBefore(marker: String): String? = String(this).substringBefore(marker)

fun Substring.substringBetween(startMarker: String, endMarker: String): String? = String(this).substringBetween(startMarker, endMarker)

fun Substring.splitBy(separator: String): Array<String> = String(this).components(separatedBy = separator)
