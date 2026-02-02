package skip.ui

import skip.lib.*
import skip.lib.Array
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

@androidx.annotation.Keep
enum class Axis(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, RawRepresentable<Int> {
    horizontal(1), // For bridging
    vertical(2); // For bridging

    class Set: OptionSet<Axis.Set, Int>, MutableStruct {
        override var rawValue: Int

        constructor(rawValue: Int) {
            this.rawValue = rawValue
        }

        constructor(axis: Axis) {
            this.rawValue = axis.rawValue
        }

        override val rawvaluelong: ULong
            get() = ULong(rawValue)
        override fun makeoptionset(rawvaluelong: ULong): Axis.Set = Set(rawValue = Int(rawvaluelong))
        override fun assignoptionset(target: Axis.Set) {
            willmutate()
            try {
                assignfrom(target)
            } finally {
                didmutate()
            }
        }

        private constructor(copy: MutableStruct) {
            @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as Axis.Set
            this.rawValue = copy.rawValue
        }

        override var supdate: ((Any) -> Unit)? = null
        override var smutatingcount = 0
        override fun scopy(): MutableStruct = Axis.Set(this as MutableStruct)

        private fun assignfrom(target: Axis.Set) {
            this.rawValue = target.rawValue
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Axis.Set) return false
            return rawValue == other.rawValue
        }

        override fun hashCode(): Int {
            var result = 1
            result = Hasher.combine(result, rawValue)
            return result
        }

        @androidx.annotation.Keep
        companion object {

            val horizontal: Axis.Set = Axis.Set(Axis.horizontal)
            val vertical: Axis.Set = Axis.Set(Axis.vertical)

            fun of(vararg options: Axis.Set): Axis.Set {
                val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
                return Set(rawValue = value)
            }
        }
    }

    @androidx.annotation.Keep
    companion object: CaseIterableCompanion<Axis> {
        fun init(rawValue: Int): Axis? {
            return when (rawValue) {
                1 -> Axis.horizontal
                2 -> Axis.vertical
                else -> null
            }
        }

        override val allCases: Array<Axis>
            get() = arrayOf(horizontal, vertical)
    }
}

fun Axis(rawValue: Int): Axis? = Axis.init(rawValue = rawValue)

