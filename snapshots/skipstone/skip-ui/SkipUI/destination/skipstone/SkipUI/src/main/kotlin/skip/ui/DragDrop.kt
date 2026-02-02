package skip.ui

import kotlin.reflect.KClass
import skip.lib.*
import skip.lib.Array
import skip.lib.Set

// Copyright 2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

@Suppress("MUST_BE_INITIALIZED")
class DragConfiguration: MutableStruct {
    class OperationsWithinApp {
        constructor(allowMove: Boolean = false) {
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    class OperationsOutsideApp {
        constructor(allowCopy: Boolean = true) {
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    var operationsWithinApp: DragConfiguration.OperationsWithinApp
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var operationsOutsideApp: DragConfiguration.OperationsOutsideApp
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(operationsWithinApp: DragConfiguration.OperationsWithinApp = OperationsWithinApp(), operationsOutsideApp: DragConfiguration.OperationsOutsideApp = OperationsOutsideApp()) {
        this.operationsWithinApp = operationsWithinApp
        this.operationsOutsideApp = operationsOutsideApp
    }

    constructor(allowMove: Boolean) {
        this.operationsWithinApp = OperationsWithinApp(allowMove = allowMove)
        this.operationsOutsideApp = OperationsOutsideApp()
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as DragConfiguration
        this.operationsWithinApp = copy.operationsWithinApp
        this.operationsOutsideApp = copy.operationsOutsideApp
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = DragConfiguration(this as MutableStruct)

    @androidx.annotation.Keep
    companion object {
    }
}

class DragSession: Identifiable<DragSession.ID> {
    sealed class Phase {
        class InitialCase: Phase() {
        }
        class ActiveCase: Phase() {
        }
        class EndingCase(val associated0: DropOperation): Phase() {
            override fun equals(other: Any?): Boolean {
                if (other !is EndingCase) return false
                return associated0 == other.associated0
            }
            override fun hashCode(): Int {
                var result = 1
                result = Hasher.combine(result, associated0)
                return result
            }
        }
        class EndedCase(val associated0: DropOperation): Phase() {
            override fun equals(other: Any?): Boolean {
                if (other !is EndedCase) return false
                return associated0 == other.associated0
            }
            override fun hashCode(): Int {
                var result = 1
                result = Hasher.combine(result, associated0)
                return result
            }
        }
        class DataTransferCompletedCase: Phase() {
        }

        @androidx.annotation.Keep
        companion object {
            val initial: Phase = InitialCase()
            val active: Phase = ActiveCase()
            fun ending(associated0: DropOperation): Phase = EndingCase(associated0)
            fun ended(associated0: DropOperation): Phase = EndedCase(associated0)
            val dataTransferCompleted: Phase = DataTransferCompletedCase()
        }
    }

    class ID {
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun matches(dragSession: Any): Boolean {
            fatalError()
        }

        override fun equals(other: Any?): Boolean = other is DragSession.ID

        override fun hashCode(): Int = "DragSession.ID".hashCode()

        @androidx.annotation.Keep
        companion object {
        }
    }

    override val id: DragSession.ID
    val phase: DragSession.Phase
    val draggedItemIndex: Int

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun draggedItemIDs(for_: KClass<*>): Array<*> {
        val type = for_
        fatalError()
    }

    constructor(id: DragSession.ID, phase: DragSession.Phase, draggedItemIndex: Int) {
        this.id = id
        this.phase = phase
        this.draggedItemIndex = draggedItemIndex
    }

    @androidx.annotation.Keep
    companion object {
    }
}

class DropConfiguration {
    val operation: DropOperation

    constructor(operation: DropOperation) {
        this.operation = operation
    }

    @androidx.annotation.Keep
    companion object {
    }
}

interface DropDelegate {
    fun validateDrop(info: DropInfo): Boolean = true
    fun performDrop(info: DropInfo): Boolean

    fun dropEntered(info: DropInfo) = Unit

    fun dropUpdated(info: DropInfo): DropProposal? = null

    fun dropExited(info: DropInfo) = Unit
}

class DropInfo {
    val location: CGPoint

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun hasItemsConforming(to: Array<*>): Boolean {
        val contentTypes = to
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun itemProviders(for_: Array<*>): Array<*> {
        val contentTypes = for_
        fatalError()
    }

    constructor(location: CGPoint) {
        this.location = location.sref()
    }

    @androidx.annotation.Keep
    companion object {
    }
}

//extension DropInfo {
//    @available(*, unavailable)
//    public func hasItemsConforming(to types: [String]) -> Bool {
//        fatalError()
//    }
//
//    @available(*, unavailable)
//    public func itemProviders(for types: [String]) -> [Any /* NSItemProvider */] {
//        fatalError()
//    }
//}

enum class DropOperation(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
    cancel(1),
    forbidden(2),
    copy(4),
    move(8),
    delete(16);

    class Set: OptionSet<DropOperation.Set, Int>, MutableStruct {
        override var rawValue: Int

        constructor(rawValue: Int) {
            this.rawValue = rawValue
        }

        override val rawvaluelong: ULong
            get() = ULong(rawValue)
        override fun makeoptionset(rawvaluelong: ULong): DropOperation.Set = Set(rawValue = Int(rawvaluelong))
        override fun assignoptionset(target: DropOperation.Set) {
            willmutate()
            try {
                assignfrom(target)
            } finally {
                didmutate()
            }
        }

        private constructor(copy: MutableStruct) {
            @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as DropOperation.Set
            this.rawValue = copy.rawValue
        }

        override var supdate: ((Any) -> Unit)? = null
        override var smutatingcount = 0
        override fun scopy(): MutableStruct = DropOperation.Set(this as MutableStruct)

        private fun assignfrom(target: DropOperation.Set) {
            this.rawValue = target.rawValue
        }

        override fun equals(other: Any?): Boolean {
            if (other !is DropOperation.Set) return false
            return rawValue == other.rawValue
        }

        override fun hashCode(): Int {
            var result = 1
            result = Hasher.combine(result, rawValue)
            return result
        }

        @androidx.annotation.Keep
        companion object {

            val cancel = DropOperation.Set(rawValue = DropOperation.cancel.rawValue)
            val copy = DropOperation.Set(rawValue = DropOperation.copy.rawValue)
            val move = DropOperation.Set(rawValue = DropOperation.move.rawValue)
            val forbidden = DropOperation.Set(rawValue = DropOperation.forbidden.rawValue)
            val delete = DropOperation.Set(rawValue = DropOperation.delete.rawValue)

            fun of(vararg options: DropOperation.Set): DropOperation.Set {
                val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
                return Set(rawValue = value)
            }
        }
    }

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: Int): DropOperation? {
            return when (rawValue) {
                1 -> DropOperation.cancel
                2 -> DropOperation.forbidden
                4 -> DropOperation.copy
                8 -> DropOperation.move
                16 -> DropOperation.delete
                else -> null
            }
        }
    }
}

fun DropOperation(rawValue: Int): DropOperation? = DropOperation.init(rawValue = rawValue)

class DropProposal {
    val operation: DropOperation
    val operationOutsideApplication: DropOperation?

    constructor(operation: DropOperation) {
        this.operation = operation
        this.operationOutsideApplication = null
    }

    constructor(withinApplication: DropOperation, outsideApplication: DropOperation) {
        this.operation = withinApplication
        this.operationOutsideApplication = outsideApplication
    }

    @androidx.annotation.Keep
    companion object {
    }
}

@Suppress("MUST_BE_INITIALIZED")
class DropSession: Identifiable<DropSession.ID>, MutableStruct {
    class LocalSession {
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun draggedItemIDs(for_: KClass<*>): Array<*> {
            val type = for_
            fatalError()
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    class ID {
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun matches(dropSession: Any): Boolean {
            fatalError()
        }

        override fun equals(other: Any?): Boolean = other is DropSession.ID

        override fun hashCode(): Int = "DropSession.ID".hashCode()

        @androidx.annotation.Keep
        companion object {
        }
    }

    sealed class Phase {
        class EnteringCase: Phase() {
        }
        class ActiveCase: Phase() {
        }
        class ExitingCase: Phase() {
        }
        class EndedCase(val associated0: DropOperation): Phase() {
            override fun equals(other: Any?): Boolean {
                if (other !is EndedCase) return false
                return associated0 == other.associated0
            }
            override fun hashCode(): Int {
                var result = 1
                result = Hasher.combine(result, associated0)
                return result
            }
        }
        class DataTransferCompletedCase: Phase() {
        }

        @androidx.annotation.Keep
        companion object {
            val entering: Phase = EnteringCase()
            val active: Phase = ActiveCase()
            val exiting: Phase = ExitingCase()
            fun ended(associated0: DropOperation): Phase = EndedCase(associated0)
            val dataTransferCompleted: Phase = DataTransferCompletedCase()
        }
    }

    override val id: DropSession.ID
    val phase: DropSession.Phase
    val localSession: DropSession.LocalSession?

    var itemsCount: Int
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var suggestedOperations: DropOperation.Set
        get() = field.sref({ this.suggestedOperations = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var size: CGSize
        get() = field.sref({ this.size = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var location: CGPoint
        get() = field.sref({ this.location = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(id: DropSession.ID, phase: DropSession.Phase, localSession: DropSession.LocalSession? = null, itemsCount: Int, suggestedOperations: DropOperation.Set, size: CGSize, location: CGPoint) {
        this.id = id
        this.phase = phase
        this.localSession = localSession
        this.itemsCount = itemsCount
        this.suggestedOperations = suggestedOperations
        this.size = size
        this.location = location
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = DropSession(id, phase, localSession, itemsCount, suggestedOperations, size, location)

    @androidx.annotation.Keep
    companion object {
    }
}
