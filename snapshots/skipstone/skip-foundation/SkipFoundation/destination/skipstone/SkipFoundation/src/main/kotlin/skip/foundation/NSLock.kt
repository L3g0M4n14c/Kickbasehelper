package skip.foundation

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
interface NSLocking {
    fun lock()
    fun unlock()

    fun <R> withLock(body: () -> R): R {
        var deferaction_0: (() -> Unit)? = null
        try {
            lock()
            deferaction_0 = {
                unlock()
            }
            return body()
        } finally {
            deferaction_0?.invoke()
        }
    }
}

class NSLock: NSLocking, KotlinConverting<java.util.concurrent.Semaphore> {
    val platformValue: java.util.concurrent.Semaphore

    constructor() {
        platformValue = java.util.concurrent.Semaphore(1)
    }

    constructor(platformValue: java.util.concurrent.Semaphore) {
        this.platformValue = platformValue.sref()
    }

    var name: String? = null

    override fun lock(): Unit = platformValue.acquireUninterruptibly()

    override fun unlock(): Unit = platformValue.release()

    fun try_(): Boolean = platformValue.tryAcquire()

    fun lock(before: Date): Boolean {
        val millis = before.currentTimeMillis - Date.now.currentTimeMillis
        return platformValue.tryAcquire(millis, java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    override fun kotlin(nocopy: Boolean): java.util.concurrent.Semaphore = platformValue.sref()

    @androidx.annotation.Keep
    companion object {
    }
}

class NSRecursiveLock: NSLocking, KotlinConverting<java.util.concurrent.locks.Lock> {
    val platformValue: java.util.concurrent.locks.Lock

    constructor() {
        platformValue = java.util.concurrent.locks.ReentrantLock()
    }

    constructor(platformValue: java.util.concurrent.locks.Lock) {
        this.platformValue = platformValue.sref()
    }

    var name: String? = null

    override fun lock(): Unit = platformValue.lock()

    override fun unlock(): Unit = platformValue.unlock()

    fun try_(): Boolean = platformValue.tryLock()

    fun lock(before: Date): Boolean {
        val millis = before.currentTimeMillis - Date.now.currentTimeMillis
        return platformValue.tryLock(millis, java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    override fun kotlin(nocopy: Boolean): java.util.concurrent.locks.Lock = platformValue.sref()

    @androidx.annotation.Keep
    companion object {
    }
}
