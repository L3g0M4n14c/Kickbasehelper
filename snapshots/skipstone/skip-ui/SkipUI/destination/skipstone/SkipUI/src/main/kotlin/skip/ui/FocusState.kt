package skip.ui

import skip.lib.*

// Copyright 2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.model.*

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember

// Model State as a class rather than struct to mutate by reference and avoid copy overhead.
@Suppress("MUST_BE_INITIALIZED")
class FocusState<Value>: StateTracker {
    constructor() {
        _wrappedValue = false as Value
        StateTracking.register(this)
    }

    /// Used by the transpiler to handle both `Bool` and `Hashable` types.
    constructor(initialValue: Value) {
        _wrappedValue = initialValue
        StateTracking.register(this)
    }

    var wrappedValue: Value
        get() {
            _wrappedValueState.sref()?.let { _wrappedValueState ->
                return _wrappedValueState.value.sref({ this.wrappedValue = it })
            }
            return _wrappedValue.sref({ this.wrappedValue = it })
        }
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            _wrappedValue = newValue
            _wrappedValueState?.value = _wrappedValue
        }
    private var _wrappedValue: Value
        get() = field.sref({ this._wrappedValue = it })
        set(newValue) {
            field = newValue.sref()
        }
    private var _wrappedValueState: MutableState<Value>? = null
        get() = field.sref({ this._wrappedValueState = it })
        set(newValue) {
            field = newValue.sref()
        }

    val projectedValue: Binding<Value>
        get() {
            return Binding(get = { -> this.wrappedValue }, set = { it -> this.wrappedValue = it })
        }

    override fun trackState() {
        _wrappedValueState = mutableStateOf(_wrappedValue)
    }

    @androidx.annotation.Keep
    companion object {
    }
}
