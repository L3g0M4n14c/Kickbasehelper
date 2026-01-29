package skip.foundation

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

open class Formatter {
    open fun string(for_: Any?): String? {
        val obj = for_
        return null
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun attributedString(for_: Any, withDefaultAttributes: Dictionary<AnyHashable, Any>? = null): Any? {
        val obj = for_
        val attrs = withDefaultAttributes
        return null
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun editingString(for_: Any): String? {
        val obj = for_
        return null
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun getObjectValue(obj: Any?, for_: String, errorDescription: Any?): Boolean {
        val string = for_
        val error = errorDescription
        return false
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun isPartialStringValid(partialString: String, newEditingString: Any?, errorDescription: Any?): Boolean {
        val newString = newEditingString
        val error = errorDescription
        return false
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun isPartialStringValid(partialStringPtr: Any, proposedSelectedRange: Any?, originalString: String, originalSelectedRange: Any, errorDescription: Any?): Boolean {
        val proposedSelRangePtr = proposedSelectedRange
        val origString = originalString
        val origSelRange = originalSelectedRange
        val error = errorDescription
        return false
    }

    open var formattingContext: Formatter.Context = Formatter.Context.unknown

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun getObjectValue(obj: Any?, for_: String, range: Any?, unusedp: Nothing? = null) = Unit

    enum class Context(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
        unknown(0),
        dynamic(1),
        standalone(2),
        listItem(3),
        beginningOfSentence(4),
        middleOfSentence(5);

        internal val capitalization: android.icu.text.DisplayContext
            get() {
                return when (this) {
                    Formatter.Context.beginningOfSentence -> android.icu.text.DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE
                    Formatter.Context.listItem -> android.icu.text.DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU
                    Formatter.Context.middleOfSentence -> android.icu.text.DisplayContext.CAPITALIZATION_FOR_MIDDLE_OF_SENTENCE
                    Formatter.Context.standalone -> android.icu.text.DisplayContext.CAPITALIZATION_FOR_STANDALONE
                    Formatter.Context.unknown -> android.icu.text.DisplayContext.CAPITALIZATION_NONE
                    else -> android.icu.text.DisplayContext.CAPITALIZATION_NONE
                }
            }

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: Int): Formatter.Context? {
                return when (rawValue) {
                    0 -> Context.unknown
                    1 -> Context.dynamic
                    2 -> Context.standalone
                    3 -> Context.listItem
                    4 -> Context.beginningOfSentence
                    5 -> Context.middleOfSentence
                    else -> null
                }
            }
        }
    }

    @androidx.annotation.Keep
    companion object: CompanionClass() {

        override fun Context(rawValue: Int): Formatter.Context? = Context.init(rawValue = rawValue)
    }
    open class CompanionClass {
        open fun Context(rawValue: Int): Formatter.Context? = Formatter.Context(rawValue = rawValue)
    }
}

