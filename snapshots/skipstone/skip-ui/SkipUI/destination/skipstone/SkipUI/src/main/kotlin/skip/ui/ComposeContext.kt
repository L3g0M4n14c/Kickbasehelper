package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier

/// Context to provide modifiers, etc to composables.
///
/// This type is often used as an argument to internal `@Composable` functions and is not mutated by reference, so mark `@Stable`
/// to avoid excessive recomposition.
@Stable
@Suppress("MUST_BE_INITIALIZED")
class ComposeContext: MutableStruct {
    /// Modifiers to apply.
    var modifier: Modifier
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    /// Mechanism for a parent view to change how a child view is composed.
    var composer: Composer? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    /// Use in conjunction with `rememberSaveable` to store view state.
    var stateSaver: Saver<Any?, Any>
        get() = field.sref({ this.stateSaver = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    /// The context to pass to child content of a container view.
    ///
    /// By default, modifiers and the `composer` are reset for child content.
    fun content(modifier: Modifier = Modifier, composer: Composer? = null, stateSaver: Saver<Any?, Any>? = null): ComposeContext {
        var context = this.sref()
        context.modifier = modifier
        context.composer = composer
        context.stateSaver = stateSaver ?: this.stateSaver
        return context.sref()
    }

    constructor(modifier: Modifier = Modifier, composer: Composer? = null, stateSaver: Saver<Any?, Any> = ComposeStateSaver()) {
        this.modifier = modifier
        this.composer = composer
        this.stateSaver = stateSaver
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = ComposeContext(modifier, composer, stateSaver)

    override fun equals(other: Any?): Boolean {
        if (other !is ComposeContext) return false
        return modifier == other.modifier && composer == other.composer && stateSaver == other.stateSaver
    }

    @androidx.annotation.Keep
    companion object {
    }
}

