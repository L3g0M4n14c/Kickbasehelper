package skip.model

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

/// Kotlin representation of `Observation.Observable`.
interface Observable {
}

/// Kotlin representation of `Combine.ObservableObject`.
interface ObservableObject {
    val objectWillChange: ObservableObjectPublisher
}

