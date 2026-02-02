package skip.foundation

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

open class PropertyListSerialization {

    enum class PropertyListFormat(override val rawValue: UInt, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<UInt> {
        openStep(UInt(1)),
        xml(UInt(100)),
        binary(UInt(200));

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: UInt): PropertyListSerialization.PropertyListFormat? {
                return when (rawValue) {
                    UInt(1) -> PropertyListFormat.openStep
                    UInt(100) -> PropertyListFormat.xml
                    UInt(200) -> PropertyListFormat.binary
                    else -> null
                }
            }
        }
    }

    class ReadOptions: RawRepresentable<UInt>, OptionSet<PropertyListSerialization.ReadOptions, UInt>, MutableStruct {
        override var rawValue: UInt

        constructor(rawValue: UInt) {
            this.rawValue = rawValue
        }

        override val rawvaluelong: ULong
            get() = ULong(rawValue)
        override fun makeoptionset(rawvaluelong: ULong): PropertyListSerialization.ReadOptions = ReadOptions(rawValue = UInt(rawvaluelong))
        override fun assignoptionset(target: PropertyListSerialization.ReadOptions) {
            willmutate()
            try {
                assignfrom(target)
            } finally {
                didmutate()
            }
        }

        private constructor(copy: MutableStruct) {
            @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as PropertyListSerialization.ReadOptions
            this.rawValue = copy.rawValue
        }

        override var supdate: ((Any) -> Unit)? = null
        override var smutatingcount = 0
        override fun scopy(): MutableStruct = PropertyListSerialization.ReadOptions(this as MutableStruct)

        private fun assignfrom(target: PropertyListSerialization.ReadOptions) {
            this.rawValue = target.rawValue
        }

        @androidx.annotation.Keep
        companion object {

            val mutableContainers = ReadOptions(rawValue = 1U)
            val mutableContainersAndLeaves = ReadOptions(rawValue = 2U)

            fun of(vararg options: PropertyListSerialization.ReadOptions): PropertyListSerialization.ReadOptions {
                val value = options.fold(UInt(0)) { result, option -> result or option.rawValue }
                return ReadOptions(rawValue = value)
            }
        }
    }

    @androidx.annotation.Keep
    companion object: CompanionClass() {
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun propertyList(propertyList: Any, isValidFor: PropertyListSerialization.PropertyListFormat): Boolean {
            fatalError()
        }


        override fun propertyList(from: Data, options: PropertyListSerialization.ReadOptions, format: Any?): Dictionary<String, String>? {
            // TODO: auto-detect format from data content if the format argument is unset
            return openStepPropertyList(from = from, options = options)
        }

        override fun openStepPropertyList(from: Data, options: PropertyListSerialization.ReadOptions): Dictionary<String, String>? {
            var dict: Dictionary<String, String> = dictionaryOf()

            val text = from.utf8String
            if (text == null) {
                // should this throw an error?
                return null
            }

            val lines = text.components(separatedBy = "\n")

            for (line in lines.sref()) {
                if (!line.hasPrefix("\"")) {
                    continue // maybe a comment? (note: we do no support multi-line /* */ comments
                }
                var key: String? = null
                var value: String? = null
                var isParsingKey = true
                var currentToken = ""
                var isEscaped = false
                var isInsideString = false

                for (char in line) {
                    if (isEscaped) {
                        if (char == 'n') {
                            currentToken += "\n"
                        } else if (char == 'r') {
                            currentToken += "\r"
                        } else if (char == 't') {
                            currentToken += "\t"
                            //} else if char == "u" { // TODO: handle unicode escapes like \uXXXX
                        } else {
                            // otherwise, just add the literal characters (like " or \)
                            currentToken += char
                        }
                        isEscaped = false
                        continue
                    }

                    when (char) {
                        '\\' -> isEscaped = true
                        '\"' -> {
                            isInsideString = !isInsideString
                            if (!isInsideString) {
                                if (isParsingKey) {
                                    key = currentToken
                                    isParsingKey = false
                                } else {
                                    value = currentToken
                                }
                                currentToken = ""
                            }
                        }
                        '=' -> {
                            if (isInsideString) {
                                currentToken += char
                            } else {
                                isParsingKey = false
                            }
                        }
                        ';' -> {
                            if (isInsideString) {
                                currentToken += char
                            } else {
                                key?.let { k ->
                                    value?.let { v ->
                                        dict[k] = v
                                    }
                                }
                            }
                        }
                        else -> {
                            if (isInsideString) {
                                currentToken += char
                            }
                        }
                    }
                }
            }

            return dict.sref()
        }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun data(fromPropertyList: Any, format: PropertyListSerialization.PropertyListFormat, options: Int): Data {
            fatalError()
        }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun writePropertyList(propertyList: Any, to: Any, format: PropertyListSerialization.PropertyListFormat, options: Int, error: Any): Int {
            fatalError()
        }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun propertyList(with: Any, options: PropertyListSerialization.ReadOptions = PropertyListSerialization.ReadOptions.of(), format: Any?): Any {
            fatalError()
        }

        override fun PropertyListFormat(rawValue: UInt): PropertyListSerialization.PropertyListFormat? = PropertyListFormat.init(rawValue = rawValue)
    }
    open class CompanionClass {
        open fun propertyList(from: Data, options: PropertyListSerialization.ReadOptions = PropertyListSerialization.ReadOptions.of(), format: Any?): Dictionary<String, String>? = PropertyListSerialization.propertyList(from = from, options = options, format = format)
        internal open fun openStepPropertyList(from: Data, options: PropertyListSerialization.ReadOptions = PropertyListSerialization.ReadOptions.of()): Dictionary<String, String>? = PropertyListSerialization.openStepPropertyList(from = from, options = options)
        open fun PropertyListFormat(rawValue: UInt): PropertyListSerialization.PropertyListFormat? = PropertyListSerialization.PropertyListFormat(rawValue = rawValue)
    }
}

