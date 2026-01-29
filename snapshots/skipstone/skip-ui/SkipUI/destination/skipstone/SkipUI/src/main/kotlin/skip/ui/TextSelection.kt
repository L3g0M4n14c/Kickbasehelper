package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.ui.text.TextRange

@androidx.annotation.Keep
@Suppress("MUST_BE_INITIALIZED")
class TextSelection: MutableStruct, skip.lib.SwiftProjecting {
    sealed class Indices {
        class SelectionCase(val associated0: IntRange): Indices() {
            override fun equals(other: Any?): Boolean {
                if (other !is SelectionCase) return false
                return associated0 == other.associated0
            }
            override fun hashCode(): Int {
                var result = 1
                result = Hasher.combine(result, associated0)
                return result
            }
        }

        @androidx.annotation.Keep
        companion object {
            fun selection(associated0: IntRange): Indices = SelectionCase(associated0)
        }
    }

    var indices: TextSelection.Indices
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    val affinity: TextSelectionAffinity
        get() = TextSelectionAffinity.automatic

    val isInsertion: Boolean

    constructor(range: IntRange) {
        this.indices = TextSelection.Indices.selection(range)
        this.isInsertion = false
    }

    constructor(insertionPoint: Int) {
        this.indices = TextSelection.Indices.selection(insertionPoint until insertionPoint)
        this.isInsertion = true
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TextSelection) {
            return false
        }
        val lhs = this
        val rhs = other
        return false
    }


    /// Return the equivalent Compose text range.
    fun asComposeTextRange(): TextRange {
        val matchtarget_0 = this.indices
        when (matchtarget_0) {
            is TextSelection.Indices.SelectionCase -> {
                val range = matchtarget_0.associated0
                return TextRange(range.lowerBound, range.upperBound)
            }
        }
    }


    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as TextSelection
        this.indices = copy.indices
        this.isInsertion = copy.isInsertion
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = TextSelection(this as MutableStruct)

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, indices)
        result = Hasher.combine(result, isInsertion)
        return result
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

enum class TextSelectionAffinity {
    automatic,
    upstream,
    downstream;

    @androidx.annotation.Keep
    companion object {
    }
}

