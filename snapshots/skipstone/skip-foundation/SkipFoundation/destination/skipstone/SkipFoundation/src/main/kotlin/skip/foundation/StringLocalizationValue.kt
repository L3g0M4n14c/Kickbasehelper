package skip.foundation

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

class StringLocalizationValue: ExpressibleByStringInterpolation<StringLocalizationValue.ValueStringInterpolation> {

    val stringInterpolation: StringLocalizationValue.ValueStringInterpolation

    constructor(value: String) {
        var interp = StringLocalizationValue.ValueStringInterpolation(literalCapacity = 0, interpolationCount = 0)
        interp.appendLiteral(value)
        this.stringInterpolation = interp.sref()
    }

    constructor(stringLiteral: String, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) {
        val value = stringLiteral
        var interp = StringLocalizationValue.ValueStringInterpolation(literalCapacity = 0, interpolationCount = 0)
        interp.appendLiteral(value)
        this.stringInterpolation = interp.sref()
    }

    constructor(stringInterpolation: StringLocalizationValue.ValueStringInterpolation) {
        this.stringInterpolation = stringInterpolation.sref()
    }

    /// Returns the pattern string to use for looking up localized values in the `.xcstrings` file
    val patternFormat: String
        get() = stringInterpolation.pattern

    class ValueStringInterpolation: StringInterpolationProtocol, MutableStruct {

        // public so it can be accessed from SkipUI
        val values: MutableList<AnyHashable>
        var pattern = ""
            set(newValue) {
                willmutate()
                field = newValue
                didmutate()
            }

        constructor(literalCapacity: Int, interpolationCount: Int) {
            this.values = mutableListOf()
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
            @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as StringLocalizationValue.ValueStringInterpolation
            this.values = copy.values
            this.pattern = copy.pattern
        }

        override var supdate: ((Any) -> Unit)? = null
        override var smutatingcount = 0
        override fun scopy(): MutableStruct = StringLocalizationValue.ValueStringInterpolation(this as MutableStruct)

        override fun equals(other: Any?): Boolean {
            if (other !is StringLocalizationValue.ValueStringInterpolation) return false
            return values == other.values && pattern == other.pattern
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    @androidx.annotation.Keep
    companion object {
    }
}
