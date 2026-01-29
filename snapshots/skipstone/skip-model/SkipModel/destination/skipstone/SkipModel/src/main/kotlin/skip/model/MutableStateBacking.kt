package skip.model

import skip.lib.*

// Copyright 2024–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class MutableStateBacking: StateTracker {
    private var state: MutableList<MutableState<Int>> = mutableListOf()
        get() = field.sref({ this.state = it })
        set(newValue) {
            field = newValue.sref()
        }
    private var isTracking = false

    constructor() {
        StateTracking.register(this)
    }

    fun access(stateAt: Int) {
        val index = stateAt
        synchronized(this) { ->
            initialize(stateAt = index)
            state[index].value.sref()
        }
    }

    fun update(stateAt: Int) {
        val index = stateAt
        synchronized(this) { ->
            initialize(stateAt = index)
            // Only update state when tracking. We do, however, read state even when tracking has not begun.
            // Otherwise post-tracking updates may not cause recomposition
            if (isTracking) {
                state[index].value += 1
            }
        }
    }

    private fun initialize(stateAt: Int) {
        val index = stateAt
        while (state.size <= index) {
            state.add(mutableStateOf(0))
        }
    }

    override fun trackState() {
        synchronized(this) { -> isTracking = true }
    }

    @androidx.annotation.Keep
    companion object {
    }
}
