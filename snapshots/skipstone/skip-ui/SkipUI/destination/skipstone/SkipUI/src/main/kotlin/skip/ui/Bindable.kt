package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

// Model Bindable as a class rather than struct to avoid copy overhead on mutation
@Suppress("MUST_BE_INITIALIZED")
class Bindable<Value> {
    constructor(wrappedValue: Value) {
        this.wrappedValue = wrappedValue
    }

    constructor(wrappedValue: Value, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) {
        this.wrappedValue = wrappedValue
    }

    var wrappedValue: Value
        get() = field.sref({ this.wrappedValue = it })
        set(newValue) {
            field = newValue.sref()
        }

    val projectedValue: Bindable<Value>
        get() = Bindable(wrappedValue = wrappedValue)

    @androidx.annotation.Keep
    companion object {
    }
}

