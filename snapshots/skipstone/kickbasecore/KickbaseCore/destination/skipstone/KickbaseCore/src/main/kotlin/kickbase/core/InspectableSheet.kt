package kickbase.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import skip.lib.*

import skip.ui.*
import skip.foundation.*
import skip.model.*

fun <Sheet> View.sheet2(isPresented: Binding<Boolean>, onDismiss: (() -> Unit)? = null, content: () -> Sheet): View where Sheet: View = this.modifier(InspectableSheet(presented = isPresented, onDismiss = onDismiss, builder = content))

internal class InspectableSheet<Sheet>: skip.ui.ViewModifier where Sheet: View {
    // Use different stored names so we can explicitly expose the protocol properties
    internal val presented: Binding<Boolean>
    internal val onDismiss: (() -> Unit)?
    internal val builder: () -> Sheet

    override fun body(content: View): View {
        return ComposeBuilder { composectx: ComposeContext -> content.sheet(isPresented = presented, onDismiss = onDismiss, content = builder).Compose(composectx) }
    }

    constructor(presented: Binding<Boolean>, onDismiss: (() -> Unit)? = null, builder: () -> Sheet) {
        this.presented = presented.sref()
        this.onDismiss = onDismiss
        this.builder = builder
    }
}

