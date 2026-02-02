package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

@androidx.annotation.Keep
class UIColor: skip.lib.SwiftProjecting {
    internal val red: Double
    internal val green: Double
    internal val blue: Double
    internal val alpha: Double

    constructor(red: Double, green: Double, blue: Double, alpha: Double) {
        this.red = red
        this.green = green
        this.blue = blue
        this.alpha = alpha
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

