package skip.ui

import skip.lib.*

// Copyright 2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.model.*
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/// Support for bridged`SwiftUI.@State`.
///
/// The Compose side manages object lifecycles, so we hold references to the native value and a `MutableState` recomposition trigger.
/// State values are not necessarily bridged or bridgable, so we use an opaque pointer. This support object is `remembered` and synced to
/// and from the native view.
@androidx.annotation.Keep
class StateSupport: StateTracker, skip.lib.SwiftProjecting {
    private var state: MutableState<Int>? = null
        get() = field.sref({ this.state = it })
        set(newValue) {
            field = newValue.sref()
        }

    /// Supply a Swift pointer to an object that holds the `@State` value and a block to release the object on finalize.
    constructor(valueHolder: Long) {
        this.valueHolder = valueHolder
        StateTracking.register(this)
    }

    fun finalize() {
        valueHolder = Swift_release(valueHolder)
    }

    /// - Seealso `SkipSwiftUI.BridgedStateBox`
    private external fun Swift_release(Swift_valueHolder: Long): Long

    var valueHolder: Long
        private set

    fun access() {
        state?.value.sref()
    }

    fun update() {
        state?.value += 1
    }

    override fun trackState() {
        state = mutableStateOf(0)
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

