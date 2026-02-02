package skip.ui

import skip.lib.*

// Copyright 2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

/// Generic completion handler to take the place of passing a completion closure to a bridged closure, as we
/// do not yet supporting bridging closure arguments to closures.
@androidx.annotation.Keep
class CompletionHandler: skip.lib.SwiftProjecting {
    private val handler: () -> Unit

    constructor(handler: () -> Unit) {
        this.handler = handler
    }

    fun run(): Unit = handler()

    var onCancel: (() -> Unit)? = null

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

/// Generic completion handler to take the place of passing a completion closure to a bridged closure, as we
/// do not yet supporting bridging closure arguments to closures.
@androidx.annotation.Keep
class ValueCompletionHandler: skip.lib.SwiftProjecting {
    private val handler: (Any?) -> Unit

    constructor(handler: (Any?) -> Unit) {
        this.handler = handler
    }

    fun run(value: Any?): Unit = handler(value)

    var onCancel: (() -> Unit)? = null

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

