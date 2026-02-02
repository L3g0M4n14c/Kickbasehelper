package skip.lib

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

/// Kotlin representation of `Swift.RawRepresentable`.
interface RawRepresentable<RawType> {
    val rawValue: RawType
}

