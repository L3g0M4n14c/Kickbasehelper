package skip.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.model.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

class SubscriptionView<PublisherType, Content>: View where Content: View {
    val content: Content
    val publisher: PublisherType
    val action: (Any) -> Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(content: Content, publisher: PublisherType, action: (Any) -> Unit) {
        this.content = content.sref()
        this.publisher = publisher.sref()
        this.action = action
    }


    @androidx.annotation.Keep
    companion object {
    }
}

