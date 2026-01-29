package skip.foundation

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

typealias NSTimer = Timer

class Timer: KotlinConverting<java.util.Timer?> {
    private var timer: java.util.Timer? = null
        get() = field.sref({ this.timer = it })
        set(newValue) {
            field = newValue.sref()
        }
    private var repeats = false
    private var block: ((Timer) -> Unit)? = null
    private var invalidated = false

    constructor(platformValue: java.util.Timer) {
        this.timer = platformValue
    }

    override fun kotlin(nocopy: Boolean): java.util.Timer? = timer.sref()

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(timeInterval: Double, invocation: Any, repeats: Boolean) {
        val ti = timeInterval
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(timeInterval: Double, target: Any, selector: Any, userInfo: Any?, repeats: Boolean) {
        val ti = timeInterval
        val aTarget = target
        val aSelector = selector
        fatalError()
    }

    constructor(timeInterval: Double, repeats: Boolean, block: (Timer) -> Unit) {
        this.timeInterval = timeInterval
        this.repeats = repeats
        this.block = block
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(fire: Date, interval: Double, repeats: Boolean, block: (Timer) -> Unit) {
        val date = fire
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(fireAt: Date, interval: Double, target: Any, selector: Any, userInfo: Any?, repeats: Boolean) {
        val date = fireAt
        val ti = interval
        val t = target
        val s = selector
        val ui = userInfo
        val rep = repeats
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun fire() {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    var fireDate: Date
        get() {
            fatalError()
        }
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            fatalError()
        }

    var timeInterval: Double = 0.0
        private set

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    var tolerance: Double
        get() {
            fatalError()
        }
        set(newValue) {
            fatalError()
        }

    fun invalidate() {
        synchronized(this) { ->
            timer?.cancel()
            timer = null
            block = null
            invalidated = true
        }
    }

    val isValid: Boolean
        get() {
            return synchronized(this) l@{ -> return@l !invalidated }
        }

    var userInfo: Any? = null
        get() = field.sref({ this.userInfo = it })
        set(newValue) {
            field = newValue.sref()
        }

    /// Used by the run loop to start the timer.
    fun start() {
        synchronized(this) l@{ ->
            if (invalidated || block == null) {
                return@l
            }
            val block = this.block
            val timerTask = Task { -> block?.invoke(this) }
            timer = java.util.Timer(true)
            val delayms = Long(timeInterval * 1000.0)
            if (repeats) {
                timer?.schedule(timerTask, delayms, delayms)
            } else {
                timer?.schedule(timerTask, delayms)
            }
        }
    }

    private class Task: java.util.TimerTask {
        internal val task: () -> Unit

        internal constructor(task: () -> Unit) {
            this.task = task
        }

        override fun run() {
            GlobalScope.launch(Dispatchers.Main) { -> task() }
        }
    }

    @androidx.annotation.Keep
    companion object {

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun scheduledTimer(timeInterval: Double, invocation: Any, repeats: Boolean): Timer {
            val ti = timeInterval
            fatalError()
        }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun scheduledTimer(timeInterval: Double, target: Any, selector: Any, userInfo: Any?, repeats: Boolean): Timer {
            val ti = timeInterval
            val aTarget = target
            val aSelector = selector
            fatalError()
        }

        fun scheduledTimer(withTimeInterval: Double, repeats: Boolean, block: (Timer) -> Unit): Timer {
            val interval = withTimeInterval
            val timer = Timer(timeInterval = interval, repeats = repeats, block = block)
            timer.start()
            return timer
        }
    }
}

