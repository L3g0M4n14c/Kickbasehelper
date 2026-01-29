package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.ui.text.input.ImeAction

enum class SubmitLabel(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
    done(0), // For bridging
    go(1), // For bridging
    send(2), // For bridging
    join(3), // For bridging
    route(4), // For bridging
    search(5), // For bridging
    return_(6), // For bridging
    next(7), // For bridging
    continue_(8); // For bridging

    internal fun asImeAction(): ImeAction {
        when (this) {
            SubmitLabel.done -> return ImeAction.Done.sref()
            SubmitLabel.go -> return ImeAction.Go.sref()
            SubmitLabel.send -> return ImeAction.Send.sref()
            SubmitLabel.join -> return ImeAction.Go.sref()
            SubmitLabel.route -> return ImeAction.Go.sref()
            SubmitLabel.search -> return ImeAction.Search.sref()
            SubmitLabel.return_ -> return ImeAction.Default.sref()
            SubmitLabel.next -> return ImeAction.Next.sref()
            SubmitLabel.continue_ -> return ImeAction.Next.sref()
        }
    }

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: Int): SubmitLabel? {
            return when (rawValue) {
                0 -> SubmitLabel.done
                1 -> SubmitLabel.go
                2 -> SubmitLabel.send
                3 -> SubmitLabel.join
                4 -> SubmitLabel.route
                5 -> SubmitLabel.search
                6 -> SubmitLabel.return_
                7 -> SubmitLabel.next
                8 -> SubmitLabel.continue_
                else -> null
            }
        }
    }
}

fun SubmitLabel(rawValue: Int): SubmitLabel? = SubmitLabel.init(rawValue = rawValue)

