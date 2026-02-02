package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

/// Manage options used in `View.Evaluate(context:options:)`.
@Suppress("MUST_BE_INITIALIZED")
internal class EvaluateOptions: MutableStruct {

    internal constructor(value: Int) {
        this.value = value
    }

    internal constructor(isKeepForEach: Boolean = false, isKeepNonModified: Boolean = false, lazyItemLevel: Int? = null) {
        var options = EvaluateOptions(0)
        options.isKeepForEach = isKeepForEach
        options.isKeepNonModified = isKeepNonModified
        options.lazyItemLevel = lazyItemLevel
        this.value = options.value
    }

    internal var value: Int
        private set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    /// Option to keep `ForEach` instances rather than evaluating them.
    internal var isKeepForEach: Boolean
        get() = (value % Companion.lazyItemLevels) and Companion.keepForEach == Companion.keepForEach
        set(newValue) {
            if (newValue) {
                value = value or Companion.keepForEach
            } else {
                value = value and Companion.keepForEach.inv()
            }
        }

    /// Option to keep any view that is not a `ModifiedContent` rather than evaluating it.
    ///
    /// If the view is not a `Renderable`, it is wrapped in one.
    ///
    /// - Warning: Only works reliably when evaluating a `ComposeBuilder`.
    internal var isKeepNonModified: Boolean
        get() = (value % Companion.lazyItemLevels) and Companion.keepNonModified == Companion.keepNonModified
        set(newValue) {
            if (newValue) {
                value = value or Companion.keepNonModified
            } else {
                value = value and Companion.keepNonModified.inv()
            }
        }

    /// Manage lazy item evaluation.
    ///
    /// - Seealso: `LazyItemFactory`
    internal var lazyItemLevel: Int?
        get() {
            if (value < Companion.lazyItemLevels) {
                return null
            }
            var level = 0
            while (true) {
                value -= Companion.lazyItemLevels
                if (value < Companion.lazyItemLevels) {
                    break
                }
                level += 1
            }
            return level
        }
        set(newValue) {
            var value = this.value % Companion.lazyItemLevels
            if (newValue != null) {
                value += Companion.lazyItemLevels * (newValue + 1)
            }
            this.value = value
        }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as EvaluateOptions
        this.value = copy.value
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = EvaluateOptions(this as MutableStruct)

    @androidx.annotation.Keep
    companion object {
        private val keepForEach = 1 shl 0
        private val keepNonModified = 1 shl 1
        // We use values < 1000 for bitwise options and add 1000 per lazy item level (+ 1)
        private val lazyItemLevels = 1000
    }
}

