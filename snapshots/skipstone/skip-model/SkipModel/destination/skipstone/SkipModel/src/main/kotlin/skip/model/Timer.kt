package skip.model

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*

fun Timer.Companion.publish(every: Double, tolerance: Double? = null, on: RunLoop, in_: RunLoop.Mode, options: RunLoop.SchedulerOptions? = null): ConnectablePublisher<Date, Never> {
    val runLoop = on
    val mode = in_
    return TimerPublisher(every = every, tolerance = tolerance, on = runLoop, in_ = mode, options = options)
}

private class TimerPublisher: ConnectablePublisher<Date, Never> {

    private val helper: SubjectHelper<Date, Never> = SubjectHelper<Date, Never>()
    private val timeInterval: Double
    private val runLoop: RunLoop
    private val mode: RunLoop.Mode
    private var timer: Timer? = null
    private var connections = 0

    internal constructor(every: Double, tolerance: Double?, on: RunLoop, in_: RunLoop.Mode, options: RunLoop.SchedulerOptions?) {
        val runLoop = on
        val mode = in_
        this.timeInterval = every
        this.runLoop = runLoop
        this.mode = mode
    }

    fun finalize() {
        timer?.invalidate()
    }

    override fun connect(): Cancellable {
        val lock = this
        synchronized(lock) { ->
            connections += 1
            if (connections == 1) {
                timer = Timer(timeInterval = timeInterval, repeats = true) { it -> helper.send(Date()) }
                runLoop.add(timer!!, forMode = mode)
            }
        }
        return AnyCancellable { ->
            synchronized(lock) { ->
                connections -= 1
                if (connections <= 0) {
                    timer?.invalidate()
                    timer = null
                }
            }
        }
    }

    override fun sink(receiveValue: (Date) -> Unit): AnyCancellable = helper.sink(receiveValue)
}
