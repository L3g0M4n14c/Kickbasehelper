package skip.ui

import skip.lib.*
import skip.lib.Array
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

@androidx.annotation.Keep
enum class Edge(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, RawRepresentable<Int> {
    top(1), // For bridging
    leading(2), // For bridging
    bottom(4), // For bridging
    trailing(8); // For bridging

    class Set: OptionSet<Edge.Set, Int>, MutableStruct {
        override var rawValue: Int

        constructor(rawValue: Int) {
            this.rawValue = rawValue
        }

        constructor(e: Edge) {
            this.rawValue = e.rawValue
        }

        val description: String
            get() {
                var edges: Array<String> = arrayOf()
                if (this.contains(Edge.Set.top)) {
                    edges.append("top")
                }
                if (this.contains(Edge.Set.leading)) {
                    edges.append("leading")
                }
                if (this.contains(Edge.Set.bottom)) {
                    edges.append("bottom")
                }
                if (this.contains(Edge.Set.trailing)) {
                    edges.append("trailing")
                }
                if (edges.isEmpty) {
                    return "[]"
                }
                return "[${edges.joined(separator = ",")}]"
            }

        override val rawvaluelong: ULong
            get() = ULong(rawValue)
        override fun makeoptionset(rawvaluelong: ULong): Edge.Set = Set(rawValue = Int(rawvaluelong))
        override fun assignoptionset(target: Edge.Set) {
            willmutate()
            try {
                assignfrom(target)
            } finally {
                didmutate()
            }
        }

        private constructor(copy: MutableStruct) {
            @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as Edge.Set
            this.rawValue = copy.rawValue
        }

        override var supdate: ((Any) -> Unit)? = null
        override var smutatingcount = 0
        override fun scopy(): MutableStruct = Edge.Set(this as MutableStruct)

        private fun assignfrom(target: Edge.Set) {
            this.rawValue = target.rawValue
        }

        override fun toString(): String = description

        override fun equals(other: Any?): Boolean {
            if (other !is Edge.Set) return false
            return rawValue == other.rawValue
        }

        @androidx.annotation.Keep
        companion object {

            val top: Edge.Set = Edge.Set(Edge.top)
            val leading: Edge.Set = Edge.Set(Edge.leading)
            val bottom: Edge.Set = Edge.Set(Edge.bottom)
            val trailing: Edge.Set = Edge.Set(Edge.trailing)

            val all: Edge.Set = Edge.Set(rawValue = 15)
            val horizontal: Edge.Set = Edge.Set(rawValue = 10)
            val vertical: Edge.Set = Edge.Set(rawValue = 5)

            fun of(vararg options: Edge.Set): Edge.Set {
                val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
                return Set(rawValue = value)
            }
        }
    }

    @androidx.annotation.Keep
    companion object: CaseIterableCompanion<Edge> {
        fun init(rawValue: Int): Edge? {
            return when (rawValue) {
                1 -> Edge.top
                2 -> Edge.leading
                4 -> Edge.bottom
                8 -> Edge.trailing
                else -> null
            }
        }

        override val allCases: Array<Edge>
            get() = arrayOf(top, leading, bottom, trailing)
    }
}

fun Edge(rawValue: Int): Edge? = Edge.init(rawValue = rawValue)

