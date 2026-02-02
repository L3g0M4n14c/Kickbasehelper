package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.runtime.Composable

// Use a class to be able to update our openURL action on compose by reference.
@androidx.annotation.Keep
class Link: View, Renderable, skip.lib.SwiftProjecting {
    internal val content: Button
    internal var openURL = OpenURLAction.default

    constructor(destination: URL, label: () -> View) {
        content = Button(action = { -> this.openURL(destination) }, label = label)
    }

    constructor(destination: URL, bridgedLabel: View) {
        content = Button(bridgedRole = null, action = { -> this.openURL(destination) }, bridgedLabel = bridgedLabel)
    }

    constructor(titleKey: LocalizedStringKey, destination: URL): this(destination = destination, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleKey).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleResource: LocalizedStringResource, destination: URL): this(destination = destination, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleResource).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(title: String, destination: URL): this(destination = destination, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(verbatim = title).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    @Composable
    override fun Render(context: ComposeContext) {
        ComposeAction()
        content.Compose(context = context)
    }

    @Composable
    internal fun ComposeAction() {
        openURL = EnvironmentValues.shared.openURL
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

