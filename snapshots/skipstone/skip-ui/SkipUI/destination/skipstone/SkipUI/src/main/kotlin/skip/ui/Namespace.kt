package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

class Namespace {
    constructor() {
    }

    val wrappedValue: Namespace.ID
        get() {
            fatalError()
        }

    class ID {
        override fun equals(other: Any?): Boolean = other is Namespace.ID

        override fun hashCode(): Int = "Namespace.ID".hashCode()

        @androidx.annotation.Keep
        companion object {
        }
    }

    @androidx.annotation.Keep
    companion object {
    }
}

