package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

enum class BlendMode {
    normal,
    multiply,
    screen,
    overlay,
    darken,
    lighten,
    colorDodge,
    colorBurn,
    softLight,
    hardLight,
    difference,
    exclusion,
    hue,
    saturation,
    color,
    luminosity,
    sourceAtop,
    destinationOver,
    destinationOut,
    plusDarker,
    plusLighter;

    @androidx.annotation.Keep
    companion object {
    }
}

