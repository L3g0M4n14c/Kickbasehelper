package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

class KeyboardShortcut {
    enum class Localization {
        automatic,
        withoutMirroring,
        custom;

        @androidx.annotation.Keep
        companion object {
        }
    }

    val key: KeyEquivalent
    val modifiers: EventModifiers
    val localization: KeyboardShortcut.Localization

    constructor(key: KeyEquivalent, modifiers: EventModifiers = EventModifiers.command, localization: KeyboardShortcut.Localization = KeyboardShortcut.Localization.automatic) {
        this.key = key
        this.modifiers = modifiers.sref()
        this.localization = localization
    }

    override fun equals(other: Any?): Boolean {
        if (other !is KeyboardShortcut) return false
        return key == other.key && modifiers == other.modifiers && localization == other.localization
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, key)
        result = Hasher.combine(result, modifiers)
        result = Hasher.combine(result, localization)
        return result
    }

    @androidx.annotation.Keep
    companion object {

        val defaultAction = KeyboardShortcut(KeyEquivalent.return_, modifiers = EventModifiers.of())
        val cancelAction = KeyboardShortcut(KeyEquivalent.escape, modifiers = EventModifiers.of())
    }
}

