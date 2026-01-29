package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*

class EditActions: OptionSet<EditActions, Int>, MutableStruct {
    override var rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override val rawvaluelong: ULong
        get() = ULong(rawValue)
    override fun makeoptionset(rawvaluelong: ULong): EditActions = EditActions(rawValue = Int(rawvaluelong))
    override fun assignoptionset(target: EditActions) {
        willmutate()
        try {
            assignfrom(target)
        } finally {
            didmutate()
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as EditActions
        this.rawValue = copy.rawValue
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = EditActions(this as MutableStruct)

    private fun assignfrom(target: EditActions) {
        this.rawValue = target.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val move = EditActions(rawValue = 1) // For bridging
        val delete = EditActions(rawValue = 2) // For bridging
        val all = EditActions(rawValue = 3) // For bridging

        fun of(vararg options: EditActions): EditActions {
            val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
            return EditActions(rawValue = value)
        }
    }
}

internal class EditActionsModifier: RenderModifier {
    internal val isDeleteDisabled: Boolean?
    internal val isMoveDisabled: Boolean?

    internal constructor(isDeleteDisabled: Boolean? = null, isMoveDisabled: Boolean? = null): super() {
        this.isDeleteDisabled = isDeleteDisabled
        this.isMoveDisabled = isMoveDisabled
    }

    @androidx.annotation.Keep
    companion object {

        /// Return the edit actions modifier information for the given view.
        internal fun combined(for_: Renderable): EditActionsModifier {
            val renderable = for_
            var isDeleteDisabled: Boolean? = null
            var isMoveDisabled: Boolean? = null
            renderable.forEachModifier l@{ it ->
                (it as? EditActionsModifier)?.let { editActionsModifier ->
                    isDeleteDisabled = isDeleteDisabled ?: editActionsModifier.isDeleteDisabled
                    isMoveDisabled = isMoveDisabled ?: editActionsModifier.isMoveDisabled
                }
                return@l null
            }
            return EditActionsModifier(isDeleteDisabled = isDeleteDisabled, isMoveDisabled = isMoveDisabled)
        }
    }
}

fun <Element> Array<Element>.remove(atOffsets: IntSet) {
    val offsets = atOffsets
    for (offset in offsets.reversed()) {
        remove(at = offset)
    }
}

fun <Element> Array<Element>.move(fromOffsets: IntSet, toOffset: Int) {
    val source = fromOffsets
    val destination = toOffset
    if (source.count <= 1 && (destination == source[0] || destination == source[0] + 1)) {
        return
    }

    var moved: Array<Element> = arrayOf()
    var belowDestinationCount = 0
    for (offset in source.reversed()) {
        moved.append(remove(at = offset))
        if (offset < destination) {
            belowDestinationCount += 1
        }
    }
    for (m in moved.sref()) {
        insert(m, at = destination - belowDestinationCount)
    }
}
