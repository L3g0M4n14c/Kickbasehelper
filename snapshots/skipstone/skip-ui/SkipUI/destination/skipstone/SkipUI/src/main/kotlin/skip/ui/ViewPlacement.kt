package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

/// Allow views to specialize based on their placement.
internal class ViewPlacement: RawRepresentable<Int>, OptionSet<ViewPlacement, Int>, MutableStruct {
    override var rawValue: Int

    override val rawvaluelong: ULong
        get() = ULong(rawValue)
    override fun makeoptionset(rawvaluelong: ULong): ViewPlacement = ViewPlacement(rawValue = Int(rawvaluelong))
    override fun assignoptionset(target: ViewPlacement) {
        willmutate()
        try {
            assignfrom(target)
        } finally {
            didmutate()
        }
    }

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = ViewPlacement(rawValue)

    private fun assignfrom(target: ViewPlacement) {
        this.rawValue = target.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        internal val listItem = ViewPlacement(rawValue = 1)
        internal val systemTextColor = ViewPlacement(rawValue = 2)
        internal val onPrimaryColor = ViewPlacement(rawValue = 4)
        internal val toolbar = ViewPlacement(rawValue = 8)

        fun of(vararg options: ViewPlacement): ViewPlacement {
            val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
            return ViewPlacement(rawValue = value)
        }
    }
}

