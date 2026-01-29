package skip.foundation

import skip.lib.*
import skip.lib.Array
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

internal typealias NSLocale = Locale

class Locale: SwiftCustomBridged, KotlinConverting<java.util.Locale> {
    internal val platformValue: java.util.Locale

    constructor(platformValue: java.util.Locale) {
        this.platformValue = platformValue.sref()
    }

    constructor(identifier: String) {
        // Returns a locale for the specified IETF BCP 47 language tag string.
        this.platformValue = java.util.Locale.forLanguageTag(identifier.replace("_", "-"))
    }

    /// Construct an identifier that conforms to the expected Foundation identifiers
    val identifier: String
        get() {
            // Returns a string representation of this Locale object, consisting of language, country, variant, script, and extensions as below: language + "_" + country + "_" + (variant + "_#" | "#") + script + "-" + extensions Language is always lower case, country is always upper case, script is always title case, and extensions are always lower case.
            //return platformValue.toString()

            // To represent a Locale as a String for interchange purposes, use toLanguageTag().
            return platformValue.toLanguageTag().replace("-", "_")
        }

    override fun equals(other: Any?): Boolean {
        if (other !is Locale) {
            return false
        }
        val lhs = this
        val rhs = other
        return lhs.platformValue == rhs.platformValue
    }

    override fun hashCode(): Int {
        var hasher = Hasher()
        hash(into = InOut<Hasher>({ hasher }, { hasher = it }))
        return hasher.finalize()
    }
    fun hash(into: InOut<Hasher>) {
        val hasher = into
        hasher.value.combine(platformValue.hashCode())
    }

    /// Returns an array of tags to search for a locale identifier, from most specific to least specific
    internal val localeSearchTags: Array<String>
        get() {
            // the base locale is the thing we search in
            if (this.identifier == Companion.baseLocale.identifier) {
                return arrayOf(this.identifier)
            }

            // for an identifier like "fr_FR", seek "fr-FR.lproj" and "fr.lproj"
            // for an identifier like "zh_Hant", seek "zh-Hant.lproj" and "zh.lproj"
            // for an identifier like "fr_CA_QC", seek "fr-QC-CA.lproj" and "fr-CA.lproj" and "fr.lproj"
            var identifiers = arrayOf(this.canonicalIdentifier)
            val languageCode = this.languageCode ?: ""
            val matchtarget_0 = this.regionCode
            if (matchtarget_0 != null) {
                val regionCode = matchtarget_0
                if (!regionCode.isEmpty) {
                    val matchtarget_1 = this.variantCode
                    if (matchtarget_1 != null) {
                        val variantCode = matchtarget_1
                        if (!variantCode.isEmpty) {
                            identifiers.append(languageCode + "-" + variantCode + "-" + regionCode)
                            identifiers.append(languageCode + "-" + variantCode)
                            identifiers.append(languageCode + "-" + regionCode)
                        } else {
                            identifiers.append(languageCode + "-" + regionCode)
                        }
                    } else {
                        identifiers.append(languageCode + "-" + regionCode)
                    }
                } else {
                    this.variantCode?.let { variantCode ->
                        if (!variantCode.isEmpty) {
                            identifiers.append(languageCode + "-" + variantCode)
                        }
                    }
                }
            } else {
                this.variantCode?.let { variantCode ->
                    if (!variantCode.isEmpty) {
                        identifiers.append(languageCode + "-" + variantCode)
                    }
                }
            }
            // special case: un-specified "zh" should fall back to zh-Hans
            if (languageCode == "zh") {
                identifiers.append("zh-Hans")
            }
            identifiers.append(languageCode)

            return identifiers
        }

    /// The identifier that matches the default ID used for xcstrings dictionary keys
    val canonicalIdentifier: String
        get() {
            return platformValue.toLanguageTag()

            val languageCode = languageCode ?: "en"
            val matchtarget_2 = regionCode
            if (matchtarget_2 != null) {
                val regionCode = matchtarget_2
                if (!regionCode.isEmpty) {
                    val matchtarget_3 = variantCode
                    if (matchtarget_3 != null) {
                        val variantCode = matchtarget_3
                        if (!variantCode.isEmpty) {
                            return languageCode + "-" + variantCode + "-" + regionCode
                        } else {
                            return languageCode + "-" + regionCode
                        }
                    } else {
                        return languageCode + "-" + regionCode
                    }
                } else {
                    val matchtarget_4 = variantCode
                    if (matchtarget_4 != null) {
                        val variantCode = matchtarget_4
                        if (!variantCode.isEmpty) {
                            return languageCode + "-" + variantCode
                        } else {
                            return languageCode
                        }
                    } else {
                        return languageCode
                    }
                }
            } else {
                val matchtarget_4 = variantCode
                if (matchtarget_4 != null) {
                    val variantCode = matchtarget_4
                    if (!variantCode.isEmpty) {
                        return languageCode + "-" + variantCode
                    } else {
                        return languageCode
                    }
                } else {
                    return languageCode
                }
            }
        }

    val currency: Locale.Currency?
        get() {
            val currency_0 = java.text.NumberFormat.getCurrencyInstance(platformValue)?.currency.sref()
            if (currency_0 == null) {
                return null
            }
            return Locale.Currency(platformValue = currency_0)
        }

    val currencySymbol: String?
        get() {
            return currency?.symbol
        }

    val language: Locale.Language
        get() = Locale.Language(platformValue = platformValue)

    val languageCode: String?
        get() {
            return language?.languageCode?.identifier
        }

    val scriptCode: String?
        get() {
            return language?.script?.identifier
        }

    val region: Locale.Region?
        get() = language.region

    val regionCode: String?
        get() {
            return region?.identifier
        }

    val variant: Locale.Variant?
        get() = Locale.Variant(platformValue = platformValue)

    val variantCode: String?
        get() {
            return variant?.identifier
        }

    val decimalSeparator: String?
        get() {
            val symbols = java.text.DecimalFormatSymbols.getInstance(platformValue)
            return symbols.getDecimalSeparator().toString()
        }

    val groupingSeparator: String?
        get() {
            val symbols = java.text.DecimalFormatSymbols.getInstance(platformValue)
            return symbols.getGroupingSeparator().toString()
        }

    fun localizedString(forCurrencyCode: String, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null): String? {
        val currencyCode = forCurrencyCode
        // Swift is case-insensitive with currency codes but Java expects uppercased currency codes.
        // Swift returns nil when provided an invalid ISO 4217 currency code
        // but Java throws an exception.
        return try { java.util.Currency.getInstance(currencyCode.uppercased()).getDisplayName(platformValue) } catch (_: Throwable) { null }
    }

    fun localizedString(forIdentifier: String, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null, @Suppress("UNUSED_PARAMETER") unusedp_1: Nothing? = null): String? {
        val targetIdentifier = forIdentifier
        return Locale(identifier = targetIdentifier).platformValue.getDisplayName(platformValue)
    }

    fun localizedString(forLanguageCode: String, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null, @Suppress("UNUSED_PARAMETER") unusedp_1: Nothing? = null, @Suppress("UNUSED_PARAMETER") unusedp_2: Nothing? = null): String? {
        // malformed languages like "en-AU" throw an exception in Java, but no in Cocoa; so we ignore exceptions and fallback to attempting to create the Locale directly
        val locale = try { java.util.Locale.Builder().setLanguage(forLanguageCode).build() } catch (_: Throwable) { null }
        return (locale ?: Locale(identifier = forLanguageCode).platformValue).getDisplayLanguage(platformValue)
    }

    fun localizedString(forRegionCode: String, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null, @Suppress("UNUSED_PARAMETER") unusedp_1: Nothing? = null, @Suppress("UNUSED_PARAMETER") unusedp_2: Nothing? = null, @Suppress("UNUSED_PARAMETER") unusedp_3: Nothing? = null): String? {
        val locale = try { java.util.Locale.Builder().setRegion(forRegionCode).build() } catch (_: Throwable) { null }
        return (locale ?: Locale(identifier = forRegionCode).platformValue).getDisplayCountry(platformValue)
    }

    fun localizedString(forScriptCode: String): String? {
        val locale = try { java.util.Locale.Builder().setScript(forScriptCode).build() } catch (_: Throwable) { null }
        return (locale ?: Locale(identifier = forScriptCode).platformValue).getDisplayScript(platformValue)
    }

    //    public func localizedString(forVariantCode: String) -> String? {
    //        let locale = try? java.util.Locale.Builder().setScript(forScriptCode).build()
    //        return (locale ?? Locale(identifier: forScriptCode).platformValue).getDisplayScript(platformValue)
    //    }

    fun localize(key: String, value: String?, bundle: Bundle?, tableName: String?): String? {
        return bundle?.localizedBundle(locale = this)?.localizedString(forKey = key, value = value, table = tableName)
        baseLocale
    }

    class Currency: ExpressibleByStringLiteral {
        internal val platformValue: java.util.Currency

        constructor(platformValue: java.util.Currency) {
            this.platformValue = platformValue.sref()
        }

        constructor(identifier: String) {
            this.platformValue = java.util.Currency.getInstance(identifier)
        }

        constructor(stringLiteral: String, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) {
            val value = stringLiteral
            this.platformValue = java.util.Currency.getInstance(value)
        }

        val identifier: String
            get() = platformValue.getCurrencyCode()

        val symbol: String
            get() = platformValue.getSymbol()

        val isISOCurrency: Boolean
            get() {
                // Currency is always an ISO 4217 currency.
                // https://developer.android.com/reference/java/util/Currency#getInstance(java.lang.String)
                return true
            }

        override fun equals(other: Any?): Boolean {
            if (other !is Locale.Currency) return false
            return platformValue == other.platformValue
        }

        override fun hashCode(): Int {
            var result = 1
            result = Hasher.combine(result, platformValue)
            return result
        }

        @androidx.annotation.Keep
        companion object {

            val isoCurrencies: Array<Locale.Currency>
                get() {
                    val currencies = Array(java.util.Currency.getAvailableCurrencies().map { it -> Currency(platformValue = it) })
                    return currencies.sorted { it, it_1 -> it.identifier < it_1.identifier }
                }
        }
    }

    class Language {
        internal val platformValue: java.util.Locale

        constructor(platformValue: java.util.Locale) {
            this.platformValue = platformValue.sref()
        }

        val languageCode: Locale.LanguageCode
            get() = LanguageCode(platformValue = platformValue)

        val script: Locale.Script
            get() = Script(platformValue = platformValue)

        val region: Locale.Region?
            get() = Region(platformValue = platformValue)

        override fun equals(other: Any?): Boolean {
            if (other !is Locale.Language) return false
            return platformValue == other.platformValue
        }

        override fun hashCode(): Int {
            var result = 1
            result = Hasher.combine(result, platformValue)
            return result
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    class LanguageCode {
        internal val platformValue: java.util.Locale

        constructor(platformValue: java.util.Locale) {
            this.platformValue = platformValue.sref()
        }

        val identifier: String
            get() = platformValue.getLanguage()

        override fun equals(other: Any?): Boolean {
            if (other !is Locale.LanguageCode) return false
            return platformValue == other.platformValue
        }

        override fun hashCode(): Int {
            var result = 1
            result = Hasher.combine(result, platformValue)
            return result
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    class Variant {
        internal val platformValue: java.util.Locale

        constructor(platformValue: java.util.Locale) {
            this.platformValue = platformValue.sref()
        }

        val identifier: String
            get() = platformValue.getVariant()

        override fun equals(other: Any?): Boolean {
            if (other !is Locale.Variant) return false
            return platformValue == other.platformValue
        }

        override fun hashCode(): Int {
            var result = 1
            result = Hasher.combine(result, platformValue)
            return result
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    class Region {
        internal val platformValue: java.util.Locale

        constructor(platformValue: java.util.Locale) {
            this.platformValue = platformValue.sref()
        }

        val identifier: String
            get() = platformValue.getCountry()

        override fun equals(other: Any?): Boolean {
            if (other !is Locale.Region) return false
            return platformValue == other.platformValue
        }

        override fun hashCode(): Int {
            var result = 1
            result = Hasher.combine(result, platformValue)
            return result
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    class Script {
        internal val platformValue: java.util.Locale

        constructor(platformValue: java.util.Locale) {
            this.platformValue = platformValue.sref()
        }

        val identifier: String
            get() = platformValue.getScript()

        override fun equals(other: Any?): Boolean {
            if (other !is Locale.Script) return false
            return platformValue == other.platformValue
        }

        override fun hashCode(): Int {
            var result = 1
            result = Hasher.combine(result, platformValue)
            return result
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    override fun kotlin(nocopy: Boolean): java.util.Locale = (if (nocopy) platformValue else platformValue.clone() as java.util.Locale).sref()

    @androidx.annotation.Keep
    companion object {
        /// The fallback locale that is used for string lookup
        internal val baseLocale = Locale(identifier = "base")

        val availableIdentifiers: Array<String>
            get() {
                return Array(java.util.Locale.getAvailableLocales().map({ it -> it.toString() }))
            }

        val current: Locale
            get() = Locale(platformValue = java.util.Locale.getDefault())

        val system: Locale
            get() = Locale(platformValue = java.util.Locale.getDefault()) // FIXME: not the same as .system: “Use the system locale when you don’t want any localizations”

        val commonISOCurrencyCodes: Array<String>
            get() {
                val availableCurrencies = Set(java.util.Currency.getAvailableCurrencies().map { it -> it.getCurrencyCode() })

                // There is no equivalent in Android so this list is hardcoded to be equivalent to Swift.
                return arrayOf("AED", "AFN", "ALL", "AMD", "ANG", "AOA", "ARS", "AUD", "AWG", "AZN", "BAM", "BBD", "BDT", "BGN", "BHD", "BIF", "BMD", "BND", "BOB", "BRL", "BSD", "BTN", "BWP", "BYN", "BZD", "CAD", "CDF", "CHF", "CLP", "CNY", "COP", "CRC", "CUC", "CUP", "CVE", "CZK", "DJF", "DKK", "DOP", "DZD", "EGP", "ERN", "ETB", "EUR", "FJD", "FKP", "GBP", "GEL", "GHS", "GIP", "GMD", "GNF", "GTQ", "GYD", "HKD", "HNL", "HRK", "HTG", "HUF", "IDR", "ILS", "INR", "IQD", "IRR", "ISK", "JMD", "JOD", "JPY", "KES", "KGS", "KHR", "KMF", "KPW", "KRW", "KWD", "KYD", "KZT", "LAK", "LBP", "LKR", "LRD", "LSL", "LYD", "MAD", "MDL", "MGA", "MKD", "MMK", "MNT", "MOP", "MRU", "MUR", "MVR", "MWK", "MXN", "MYR", "MZN", "NAD", "NGN", "NIO", "NOK", "NPR", "NZD", "OMR", "PAB", "PEN", "PGK", "PHP", "PKR", "PLN", "PYG", "QAR", "RON", "RSD", "RUB", "RWF", "SAR", "SBD", "SCR", "SDG", "SEK", "SGD", "SHP", "SLE", "SLL", "SOS", "SRD", "SSP", "STN", "SYP", "SZL", "THB", "TJS", "TMT", "TND", "TOP", "TRY", "TTD", "TWD", "TZS", "UAH", "UGX", "USD", "UYU", "UZS", "VEF", "VES", "VND", "VUV", "WST", "XAF", "XCD", "XOF", "XPF", "YER", "ZAR", "ZMW").filter { it -> availableCurrencies.contains(it) }
            }
    }
}

