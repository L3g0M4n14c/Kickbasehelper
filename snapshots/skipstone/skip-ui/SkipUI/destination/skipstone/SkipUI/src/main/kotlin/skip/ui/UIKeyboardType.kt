package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.ui.text.input.KeyboardType

enum class UIKeyboardType(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
    default(0), // For bridging
    asciiCapable(1), // For bridging
    numbersAndPunctuation(2), // For bridging
    URL(3), // For bridging
    numberPad(4), // For bridging
    phonePad(5), // For bridging
    namePhonePad(6), // For bridging
    emailAddress(7), // For bridging
    decimalPad(8), // For bridging
    twitter(9), // For bridging
    webSearch(10), // For bridging
    asciiCapableNumberPad(11), // For bridging
    alphabet(12); // For bridging

    internal fun asComposeKeyboardType(): KeyboardType {
        when (this) {
            UIKeyboardType.default -> return KeyboardType.Text.sref()
            UIKeyboardType.asciiCapable -> return KeyboardType.Ascii.sref()
            UIKeyboardType.numbersAndPunctuation -> return KeyboardType.Text.sref()
            UIKeyboardType.URL -> return KeyboardType.Uri.sref()
            UIKeyboardType.numberPad -> return KeyboardType.NumberPassword.sref()
            UIKeyboardType.phonePad -> return KeyboardType.Phone.sref()
            UIKeyboardType.namePhonePad -> return KeyboardType.Text.sref()
            UIKeyboardType.emailAddress -> return KeyboardType.Email.sref()
            UIKeyboardType.decimalPad -> return KeyboardType.Decimal.sref()
            UIKeyboardType.twitter -> return KeyboardType.Text.sref()
            UIKeyboardType.webSearch -> return KeyboardType.Text.sref()
            UIKeyboardType.asciiCapableNumberPad -> return KeyboardType.Text.sref()
            UIKeyboardType.alphabet -> return KeyboardType.Text.sref()
        }
    }

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: Int): UIKeyboardType? {
            return when (rawValue) {
                0 -> UIKeyboardType.default
                1 -> UIKeyboardType.asciiCapable
                2 -> UIKeyboardType.numbersAndPunctuation
                3 -> UIKeyboardType.URL
                4 -> UIKeyboardType.numberPad
                5 -> UIKeyboardType.phonePad
                6 -> UIKeyboardType.namePhonePad
                7 -> UIKeyboardType.emailAddress
                8 -> UIKeyboardType.decimalPad
                9 -> UIKeyboardType.twitter
                10 -> UIKeyboardType.webSearch
                11 -> UIKeyboardType.asciiCapableNumberPad
                12 -> UIKeyboardType.alphabet
                else -> null
            }
        }
    }
}

fun UIKeyboardType(rawValue: Int): UIKeyboardType? = UIKeyboardType.init(rawValue = rawValue)

