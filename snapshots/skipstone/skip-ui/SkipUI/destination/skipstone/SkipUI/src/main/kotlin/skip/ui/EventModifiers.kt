package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

class EventModifiers: OptionSet<EventModifiers, Int>, MutableStruct {
    override var rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override val rawvaluelong: ULong
        get() = ULong(rawValue)
    override fun makeoptionset(rawvaluelong: ULong): EventModifiers = EventModifiers(rawValue = Int(rawvaluelong))
    override fun assignoptionset(target: EventModifiers) {
        willmutate()
        try {
            assignfrom(target)
        } finally {
            didmutate()
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as EventModifiers
        this.rawValue = copy.rawValue
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = EventModifiers(this as MutableStruct)

    private fun assignfrom(target: EventModifiers) {
        this.rawValue = target.rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is EventModifiers) return false
        return rawValue == other.rawValue
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, rawValue)
        return result
    }

    @androidx.annotation.Keep
    companion object {

        val capsLock = EventModifiers(rawValue = 1)
        val shift = EventModifiers(rawValue = 2)
        val control = EventModifiers(rawValue = 4)
        val option = EventModifiers(rawValue = 8)
        val command = EventModifiers(rawValue = 16)
        val numericPad = EventModifiers(rawValue = 32)
        val all = EventModifiers(rawValue = 63)

        fun of(vararg options: EventModifiers): EventModifiers {
            val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
            return EventModifiers(rawValue = value)
        }
    }
}

