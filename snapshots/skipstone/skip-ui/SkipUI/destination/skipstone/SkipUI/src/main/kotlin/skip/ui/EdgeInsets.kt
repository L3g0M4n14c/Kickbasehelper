package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Suppress("MUST_BE_INITIALIZED")
class EdgeInsets: MutableStruct {
    var top: Double
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var leading: Double
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var bottom: Double
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var trailing: Double
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(top: Double = 0.0, leading: Double = 0.0, bottom: Double = 0.0, trailing: Double = 0.0) {
        this.top = top
        this.leading = leading
        this.bottom = bottom
        this.trailing = trailing
    }

    @Composable
    internal fun asPaddingValues(): PaddingValues = PaddingValues(start = leading.dp, top = top.dp, end = trailing.dp, bottom = bottom.dp)

    val description: String
        get() = "EdgeInsets(top: ${top}, leading: ${leading}, bottom: ${bottom}, trailing: ${trailing})"

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as EdgeInsets
        this.top = copy.top
        this.leading = copy.leading
        this.bottom = copy.bottom
        this.trailing = copy.trailing
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = EdgeInsets(this as MutableStruct)

    override fun toString(): String = description

    override fun equals(other: Any?): Boolean {
        if (other !is EdgeInsets) return false
        return top == other.top && leading == other.leading && bottom == other.bottom && trailing == other.trailing
    }

    @androidx.annotation.Keep
    companion object {
    }
}

