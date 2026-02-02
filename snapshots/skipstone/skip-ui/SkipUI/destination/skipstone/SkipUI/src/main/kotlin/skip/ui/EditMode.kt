package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

enum class EditMode {
    inactive,
    transient,
    active;

    val isEditing: Boolean
        get() = this != EditMode.inactive

    @androidx.annotation.Keep
    companion object {
    }
}

