package skip.foundation

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

typealias NSThread = Thread

@Suppress("MUST_BE_INITIALIZED")
class Thread: KotlinConverting<java.lang.Thread>, MutableStruct {
    internal var platformValue: java.lang.Thread
        get() = field.sref({ this.platformValue = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    val isMainThread: Boolean
        get() = this == Thread.main

    val isExecuting: Boolean
        get() = this == Thread.current

    override fun kotlin(nocopy: Boolean): java.lang.Thread = platformValue.sref()

    fun isEqual(other: Any?): Boolean {
        val other_0 = (other as? Thread).sref()
        if (other_0 == null) {
            return false
        }
        return this.platformValue == other_0.platformValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Thread) {
            return false
        }
        val lhs = this
        val rhs = other
        return lhs.platformValue == rhs.platformValue
    }

    override fun hashCode(): Int {
        var hasher = Hasher()
        hash(into = InOut<Hasher>({ hasher }, { hasher = it }))
        return hasher.finalize()
    }
    fun hash(into: InOut<Hasher>) {
        val hasher = into
        hasher.value.combine(platformValue)
    }

    internal constructor(platformValue: java.lang.Thread) {
        this.platformValue = platformValue
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = Thread(platformValue)

    @androidx.annotation.Keep
    companion object {

        /// Re-initialized from `ProcessInfo.launch(context:)`
        var main: Thread = Thread.current.sref()
            get() = field.sref({ this.main = it })
            internal set(newValue) {
                field = newValue.sref()
            }

        val isMainThread: Boolean
            get() = Thread.current == Thread.main

        val current: Thread
            get() = Thread(platformValue = java.lang.Thread.currentThread())

        val callStackSymbols: Array<String>
            get() {
                return Array(Thread.current.platformValue.getStackTrace().map({ it -> it.toString() }))
            }

        fun sleep(forTimeInterval: Double): Unit = java.lang.Thread.sleep(Long(forTimeInterval * 1000.0))

        fun sleep(until: Date) {
            val date = until
            sleep(forTimeInterval = date.timeIntervalSince(Date.now))
        }
    }
}

