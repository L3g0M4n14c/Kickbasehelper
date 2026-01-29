package skip.foundation

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

// Warning: Updating this class to make it properly `ExpressibleByStringInterpolation` will likely cause
// ambiguous call conflicts when calling various SkipUI functions that also have `LocalizedStringKey`
// variants. We will likely need to use @_disfavoredOverload to disambiguate, including adding
// @_disfavoredOverload support to the transpiler and its function call match scoring

class LocalizedStringResource {
    val keyAndValue: StringLocalizationValue
    var defaultValue: StringLocalizationValue? = null
    var table: String? = null
    var locale: Locale? = null
    var bundle: LocalizedStringResource.BundleDescription? = null
    var comment: String? = null

    /// The raw string used to create the keyAndValue `String.LocalizationValue`
    val key: String
        get() = keyAndValue.patternFormat

    constructor(stringLiteral: String) {
        this.keyAndValue = StringLocalizationValue(stringLiteral)
        this.bundle = LocalizedStringResource.BundleDescription.main
    }

    constructor(keyAndValue: StringLocalizationValue, defaultValue: StringLocalizationValue? = null, table: String? = null, locale: Locale? = null, bundle: LocalizedStringResource.BundleDescription? = null, comment: String? = null) {
        this.keyAndValue = keyAndValue
        this.defaultValue = defaultValue
        this.table = table
        this.locale = locale
        this.bundle = bundle
        this.comment = comment
    }

    override fun equals(other: Any?): Boolean {
        if (other !is LocalizedStringResource) {
            return false
        }
        val lhs = this
        val rhs = other
        return lhs.keyAndValue == rhs.keyAndValue && lhs.defaultValue == rhs.defaultValue && lhs.table == rhs.table && lhs.locale == rhs.locale && lhs.bundle == rhs.bundle && lhs.comment == rhs.comment
    }

    override fun hashCode(): Int {
        var hasher = Hasher()
        hash(into = InOut<Hasher>({ hasher }, { hasher = it }))
        return hasher.finalize()
    }
    fun hash(into: InOut<Hasher>) {
        val hasher = into
        hasher.value.combine(keyAndValue.hashCode())
        defaultValue?.let { defaultValue ->
            hasher.value.combine(defaultValue.hashCode())
        }
        table?.let { table ->
            hasher.value.combine(table.hashCode())
        }
        locale?.let { locale ->
            hasher.value.combine(locale.hashCode())
        }
        bundle?.let { bundle ->
            hasher.value.combine(bundle.hashCode())
        }
        comment?.let { comment ->
            hasher.value.combine(comment.hashCode())
        }
    }

    sealed class BundleDescription {
        class MainCase: BundleDescription() {
        }
        class ForClassCase(val associated0: AnyClass): BundleDescription() {
            override fun equals(other: Any?): Boolean {
                if (other !is ForClassCase) return false
                return associated0 == other.associated0
            }
            override fun hashCode(): Int {
                var result = 1
                result = Hasher.combine(result, associated0)
                return result
            }
        }
        class AtURLCase(val associated0: URL): BundleDescription() {
            override fun equals(other: Any?): Boolean {
                if (other !is AtURLCase) return false
                return associated0 == other.associated0
            }
            override fun hashCode(): Int {
                var result = 1
                result = Hasher.combine(result, associated0)
                return result
            }
        }

        val description: String
            get() {
                when (this) {
                    is LocalizedStringResource.BundleDescription.MainCase -> return "bundle: main"
                    is LocalizedStringResource.BundleDescription.ForClassCase -> {
                        val c = this.associated0
                        return "bundle: ${c}"
                    }
                    is LocalizedStringResource.BundleDescription.AtURLCase -> {
                        val url = this.associated0
                        return "bundle: ${url}"
                    }
                }
            }

        val bundle: Bundle
            get() = Bundle(location = this)

        override fun toString(): String = description

        @androidx.annotation.Keep
        companion object {
            val main: BundleDescription = MainCase()
            fun forClass(associated0: AnyClass): BundleDescription = ForClassCase(associated0)
            fun atURL(associated0: URL): BundleDescription = AtURLCase(associated0)
        }
    }

    @androidx.annotation.Keep
    companion object {
    }
}

