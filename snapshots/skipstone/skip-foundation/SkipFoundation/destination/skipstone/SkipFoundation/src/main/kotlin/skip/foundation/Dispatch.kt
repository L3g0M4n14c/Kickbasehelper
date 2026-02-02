package skip.foundation

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Stubs that allow SkipModel to implement Publisher.receive(on:) for the main queue

interface Scheduler {
}

class RunLoop: Scheduler {

    enum class Mode(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
        default(0),
        common(1),
        eventTracking(2),
        modalPanel(3),
        tracking(4);

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: Int): RunLoop.Mode? {
                return when (rawValue) {
                    0 -> Mode.default
                    1 -> Mode.common
                    2 -> Mode.eventTracking
                    3 -> Mode.modalPanel
                    4 -> Mode.tracking
                    else -> null
                }
            }
        }
    }

    class SchedulerOptions {

        @androidx.annotation.Keep
        companion object {
        }
    }

    private constructor() {
    }

    fun add(timer: Timer, forMode: RunLoop.Mode) {
        val mode = forMode
        timer.start() // We don't yet support non-main run loops and timer always uses main
    }

    @androidx.annotation.Keep
    companion object {
        val main = RunLoop()

        fun Mode(rawValue: Int): RunLoop.Mode? = Mode.init(rawValue = rawValue)
    }
}

typealias DispatchWallTime = Double
typealias DispatchTime = Double

// Mirror Double's cast functions, which typealiasing doesn't cover
fun DispatchWallTime(number: Number): Double = Double(number = number)
fun DispatchWallTime(number: UByte): Double = Double(number = number)
fun DispatchWallTime(number: UShort): Double = Double(number = number)
fun DispatchWallTime(number: UInt): Double = Double(number = number)
fun DispatchWallTime(number: ULong): Double = Double(number = number)
fun DispatchWallTime(string: String): Double? = Double(string = string)
fun DispatchTime(number: Number): Double = Double(number = number)
fun DispatchTime(number: UByte): Double = Double(number = number)
fun DispatchTime(number: UShort): Double = Double(number = number)
fun DispatchTime(number: UInt): Double = Double(number = number)
fun DispatchTime(number: ULong): Double = Double(number = number)
fun DispatchTime(string: String): Double? = Double(string = string)

fun Double.Companion.now(): Double = Double(System.currentTimeMillis()) / 1000.0

class DispatchQueue: Scheduler {

    private constructor() {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(label: String, qos: Any, attributes: Any, autoreleaseFrequency: Any, target: DispatchQueue?) {
    }

    fun async(execute: () -> Unit) {
        GlobalScope.launch(Dispatchers.Main) { -> execute() }
    }

    fun asyncAfter(deadline: Double, unusedp: Nothing? = null, execute: () -> Unit) {
        GlobalScope.launch(Dispatchers.Main) { ->
            delay(Long(deadline * 1000.0) - System.currentTimeMillis())
            execute()
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun asyncAfter(deadline: Double, qos: Any, flags: Any, execute: () -> Unit, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) = Unit

    fun asyncAfter(wallDeadline: Double, execute: () -> Unit) {
        GlobalScope.launch(Dispatchers.Main) { ->
            delay(Long(wallDeadline * 1000.0) - System.currentTimeMillis())
            execute()
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun asyncAfter(wallDeadline: Double, qos: Any, flags: Any, execute: () -> Unit) = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun sync(execute: () -> Unit) = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun sync(execute: () -> Any): Any {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun sync(flags: Any, execute: () -> Any): Any {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun asyncAndWait(execute: () -> Unit) = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun async(group: Any, execute: () -> Unit) = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun async(group: Any?, qos: Any, flags: Any, execute: () -> Unit) = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    var label: String
        get() {
            fatalError()
        }
        set(newValue) {
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    var qos: Any?
        get() {
            fatalError()
        }
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun setTarget(queue: Any?) = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun setSpecific(key: Any, value: Any?) = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun getSpecific(key: Any): Any? {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dispatchMain(): Any {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun schedule(options: Any?, operation: () -> Unit) = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun schedule(after: Any, tolerance: Any, options: Any?, operation: () -> Unit) = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun schedule(after: Any, interval: Any, tolerance: Any, options: Any?, operation: () -> Unit): Any {
        fatalError()
    }

    @androidx.annotation.Keep
    companion object {
        val main = DispatchQueue()

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun global(qos: Any): DispatchQueue {
            fatalError()
        }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun concurrentPerform(iterations: Int, execute: (Int) -> Unit) = Unit

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun getSpecific(key: Any): Any? {
            fatalError()
        }
    }
}

