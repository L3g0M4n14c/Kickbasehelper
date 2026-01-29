package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

class SubmitTriggers: OptionSet<SubmitTriggers, Int>, MutableStruct {
    override var rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override val rawvaluelong: ULong
        get() = ULong(rawValue)
    override fun makeoptionset(rawvaluelong: ULong): SubmitTriggers = SubmitTriggers(rawValue = Int(rawvaluelong))
    override fun assignoptionset(target: SubmitTriggers) {
        willmutate()
        try {
            assignfrom(target)
        } finally {
            didmutate()
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as SubmitTriggers
        this.rawValue = copy.rawValue
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = SubmitTriggers(this as MutableStruct)

    private fun assignfrom(target: SubmitTriggers) {
        this.rawValue = target.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val text = SubmitTriggers(rawValue = 1 shl 0) // For bridging
        val search = SubmitTriggers(rawValue = 1 shl 1) // For bridging

        fun of(vararg options: SubmitTriggers): SubmitTriggers {
            val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
            return SubmitTriggers(rawValue = value)
        }
    }
}

