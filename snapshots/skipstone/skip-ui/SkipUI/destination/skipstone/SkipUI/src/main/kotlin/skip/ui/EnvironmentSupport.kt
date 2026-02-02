package skip.ui

import skip.lib.*

// Copyright 2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

/// Support for bridged`SwiftUI.@Environment`.
///
/// The Compose side manages object lifecycles, so we hold references to the native value.
/// Environment values are not necessarily bridged or bridgable, so we use an opaque pointer.
/// This support object is placed in the Compose environment.
@androidx.annotation.Keep
class EnvironmentSupport: skip.lib.SwiftProjecting {
    /// Supply a Swift pointer to an object that holds the environment value and a block to release the object on finalize.
    constructor(valueHolder: Long) {
        this.valueHolder = valueHolder
        this.builtinValue = null
    }

    constructor(builtinValue: Any?) {
        this.builtinValue = builtinValue.sref()
        this.valueHolder = 0L
    }

    fun finalize() {
        if (valueHolder != 0L) {
            valueHolder = Swift_release(valueHolder)
        }
    }

    /// - Seealso `SkipSwiftUI.Environment`
    private external fun Swift_release(Swift_valueHolder: Long): Long


    var valueHolder: Long
        private set

    val builtinValue: Any?

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

