package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

class ContainerBackgroundPlacement {
    override fun equals(other: Any?): Boolean = other is ContainerBackgroundPlacement

    override fun hashCode(): Int = "ContainerBackgroundPlacement".hashCode()

    @androidx.annotation.Keep
    companion object {
    }
}

