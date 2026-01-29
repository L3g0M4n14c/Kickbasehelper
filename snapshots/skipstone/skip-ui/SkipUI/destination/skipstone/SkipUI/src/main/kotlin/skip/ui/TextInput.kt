package skip.ui

import skip.lib.*
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.ui.text.input.KeyboardCapitalization

enum class TextInputAutocapitalization(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
    never(0), // For bridging
    words(1), // For bridging
    sentences(2), // For bridging
    characters(3); // For bridging

    internal fun asKeyboardCapitalization(): KeyboardCapitalization {
        when (this) {
            TextInputAutocapitalization.never -> return KeyboardCapitalization.None.sref()
            TextInputAutocapitalization.words -> return KeyboardCapitalization.Words.sref()
            TextInputAutocapitalization.sentences -> return KeyboardCapitalization.Sentences.sref()
            TextInputAutocapitalization.characters -> return KeyboardCapitalization.Characters.sref()
        }
    }

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: Int): TextInputAutocapitalization? {
            return when (rawValue) {
                0 -> TextInputAutocapitalization.never
                1 -> TextInputAutocapitalization.words
                2 -> TextInputAutocapitalization.sentences
                3 -> TextInputAutocapitalization.characters
                else -> null
            }
        }
    }
}

fun TextInputAutocapitalization(rawValue: Int): TextInputAutocapitalization? = TextInputAutocapitalization.init(rawValue = rawValue)

class TextInputFormattingControlPlacement {
    class Set: OptionSet<TextInputFormattingControlPlacement.Set, Int>, MutableStruct {
        override var rawValue: Int

        constructor(rawValue: Int) {
            this.rawValue = rawValue
        }

        override val rawvaluelong: ULong
            get() = ULong(rawValue)
        override fun makeoptionset(rawvaluelong: ULong): TextInputFormattingControlPlacement.Set = Set(rawValue = Int(rawvaluelong))
        override fun assignoptionset(target: TextInputFormattingControlPlacement.Set) {
            willmutate()
            try {
                assignfrom(target)
            } finally {
                didmutate()
            }
        }

        private constructor(copy: MutableStruct) {
            @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as TextInputFormattingControlPlacement.Set
            this.rawValue = copy.rawValue
        }

        override var supdate: ((Any) -> Unit)? = null
        override var smutatingcount = 0
        override fun scopy(): MutableStruct = TextInputFormattingControlPlacement.Set(this as MutableStruct)

        private fun assignfrom(target: TextInputFormattingControlPlacement.Set) {
            this.rawValue = target.rawValue
        }

        @androidx.annotation.Keep
        companion object {

            val contextMenu = TextInputFormattingControlPlacement.Set(rawValue = 1 shl 0)
            val inputAssistant = TextInputFormattingControlPlacement.Set(rawValue = 1 shl 1)
            val all = TextInputFormattingControlPlacement.Set(rawValue = 1 shl 2)
            val default = TextInputFormattingControlPlacement.Set(rawValue = 1 shl 3)

            fun of(vararg options: TextInputFormattingControlPlacement.Set): TextInputFormattingControlPlacement.Set {
                val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
                return Set(rawValue = value)
            }
        }
    }

    @androidx.annotation.Keep
    companion object {
    }
}

/*
@available(iOS 17.0, *)
@available(macOS, unavailable)
@available(watchOS, unavailable)
@available(tvOS, unavailable)
public struct TextInputDictationActivation : Equatable, Sendable {

/// A configuration that activates dictation when someone selects the
/// microphone.
@available(iOS 17.0, *)
@available(macOS, unavailable)
@available(watchOS, unavailable)
@available(tvOS, unavailable)
public static let onSelect: TextInputDictationActivation = { fatalError() }()

/// A configuration that activates dictation when someone selects the
/// microphone or looks at the entry field.
@available(iOS 17.0, *)
@available(macOS, unavailable)
@available(watchOS, unavailable)
@available(tvOS, unavailable)
public static let onLook: TextInputDictationActivation = { fatalError() }()


}

@available(iOS 17.0, *)
@available(macOS, unavailable)
@available(watchOS, unavailable)
@available(tvOS, unavailable)
public struct TextInputDictationBehavior : Equatable, Sendable {

/// A platform-appropriate default text input dictation behavior.
///
/// The automatic behavior uses a ``TextInputDictationActivation`` value of
/// ``TextInputDictationActivation/onLook`` for visionOS apps and
/// ``TextInputDictationActivation/onSelect`` for iOS apps.
@available(iOS 17.0, *)
@available(macOS, unavailable)
@available(watchOS, unavailable)
@available(tvOS, unavailable)
public static let automatic: TextInputDictationBehavior = { fatalError() }()

/// Adds a dictation microphone in the search bar.
@available(iOS 17.0, *)
@available(macOS, unavailable)
@available(watchOS, unavailable)
@available(tvOS, unavailable)
public static func inline(activation: TextInputDictationActivation) -> TextInputDictationBehavior { fatalError() }


}
*/
