package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*

@Suppress("MUST_BE_INITIALIZED")
class LocalizedStringKey: ExpressibleByStringInterpolation<LocalizedStringKey.StringInterpolation>, MutableStruct {

    internal var stringInterpolation: LocalizedStringKey.StringInterpolation
        get() = field.sref({ this.stringInterpolation = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(string: String): this(stringLiteral = string) {
    }

    constructor(stringLiteral: String, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) {
        val value = stringLiteral
        var interp = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
        interp.appendLiteral(value)
        this.stringInterpolation = interp
    }

    constructor(stringInterpolation: LocalizedStringKey.StringInterpolation) {
        this.stringInterpolation = stringInterpolation
    }

    constructor(resource: LocalizedStringResource) {
        var interp = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
        // For (presumably) historical reasons, SwiftUI.LocalizedStringKey and Foundation.LocalizedStringResource are both StringInterpolationProtocol, but have different implementation, so copy the underlying fields over manually
        interp.pattern = resource.keyAndValue.stringInterpolation.pattern
        interp.values.addAll(resource.keyAndValue.stringInterpolation.values.toList().filterNotNull())
        this.stringInterpolation = interp
    }

    /// Returns the pattern string to use for looking up localized values in the `.xcstrings` file
    val patternFormat: String
        get() = stringInterpolation.pattern


    class StringInterpolation: StringInterpolationProtocol, MutableStruct {

        internal val values: MutableList<Any>
        internal var pattern = ""
            set(newValue) {
                willmutate()
                field = newValue
                didmutate()
            }

        constructor(literalCapacity: Int, interpolationCount: Int) {
            this.values = mutableListOf()
        }

        internal constructor(pattern: String, values: Array<*>?) {
            this.values = mutableListOf()
            this.pattern = pattern
            if (values != null) {
                this.values.addAll(values.toList().filterNotNull())
            }
        }

        override fun appendLiteral(literal: String) {
            willmutate()
            try {
                // need to escape out Java-specific format marker
                pattern += literal.replacingOccurrences(of = "%", with = "%%")
            } finally {
                didmutate()
            }
        }

        fun appendInterpolation(string: String) {
            willmutate()
            try {
                values.add(string)
                pattern += "%@"
            } finally {
                didmutate()
            }
        }

        fun appendInterpolation(int: Int) {
            willmutate()
            try {
                values.add(int)
                pattern += "%lld"
            } finally {
                didmutate()
            }
        }

        fun appendInterpolation(int: Short) {
            willmutate()
            try {
                values.add(int)
                pattern += "%d"
            } finally {
                didmutate()
            }
        }

        fun appendInterpolation(int: Long) {
            willmutate()
            try {
                values.add(int)
                pattern += "%lld"
            } finally {
                didmutate()
            }
        }

        fun appendInterpolation(int: UInt) {
            willmutate()
            try {
                values.add(int)
                pattern += "%llu"
            } finally {
                didmutate()
            }
        }

        fun appendInterpolation(int: UShort) {
            willmutate()
            try {
                values.add(int)
                pattern += "%u"
            } finally {
                didmutate()
            }
        }

        fun appendInterpolation(int: ULong) {
            willmutate()
            try {
                values.add(int)
                pattern += "%llu"
            } finally {
                didmutate()
            }
        }

        fun appendInterpolation(double: Double) {
            willmutate()
            try {
                values.add(double)
                pattern += "%lf"
            } finally {
                didmutate()
            }
        }

        fun appendInterpolation(float: Float) {
            willmutate()
            try {
                values.add(float)
                pattern += "%f"
            } finally {
                didmutate()
            }
        }

        override fun <T> appendInterpolation(value: T) {
            willmutate()
            try {
                values.add(value as Any)
                pattern += "%@"
            } finally {
                didmutate()
            }
        }

        private constructor(copy: MutableStruct) {
            @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as LocalizedStringKey.StringInterpolation
            this.values = copy.values
            this.pattern = copy.pattern
        }

        override var supdate: ((Any) -> Unit)? = null
        override var smutatingcount = 0
        override fun scopy(): MutableStruct = LocalizedStringKey.StringInterpolation(this as MutableStruct)

        override fun equals(other: Any?): Boolean {
            if (other !is LocalizedStringKey.StringInterpolation) return false
            return values == other.values && pattern == other.pattern
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as LocalizedStringKey
        this.stringInterpolation = copy.stringInterpolation
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = LocalizedStringKey(this as MutableStruct)

    override fun equals(other: Any?): Boolean {
        if (other !is LocalizedStringKey) return false
        return stringInterpolation == other.stringInterpolation
    }

    @androidx.annotation.Keep
    companion object {
    }
}

