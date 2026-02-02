package skip.lib

import kotlin.reflect.KClass

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow

class AsyncStream<Element>: AsyncSequence<Element>, SwiftCustomBridged, KotlinConverting<kotlinx.coroutines.flow.Flow<Element>> where Element: Any {
    class Continuation<Element> {
        enum class Termination {
            finished,
            cancelled;

            @androidx.annotation.Keep
            companion object {
            }
        }

        sealed class YieldResult<out Element> {
            class EnqueuedCase(val associated0: Int): YieldResult<Nothing>() {
                val remaining = associated0
            }
            class DroppedCase<Element>(val associated0: Element): YieldResult<Element>() {
            }
            class TerminatedCase: YieldResult<Nothing>() {
            }

            @androidx.annotation.Keep
            companion object {
                fun enqueued(remaining: Int): YieldResult<Nothing> = EnqueuedCase(remaining)
                fun <Element> dropped(associated0: Element): YieldResult<Element> = DroppedCase(associated0)
                val terminated: YieldResult<Nothing> = TerminatedCase()
            }
        }

        sealed class BufferingPolicy {
            class UnboundedCase: BufferingPolicy() {
            }
            class BufferingOldestCase(val associated0: Int): BufferingPolicy() {
            }
            class BufferingNewestCase(val associated0: Int): BufferingPolicy() {
            }

            @androidx.annotation.Keep
            companion object {
                val unbounded: BufferingPolicy = UnboundedCase()
                fun bufferingOldest(associated0: Int): BufferingPolicy = BufferingOldestCase(associated0)
                fun bufferingNewest(associated0: Int): BufferingPolicy = BufferingNewestCase(associated0)
            }
        }

        internal val channel: Channel<Element>

        constructor(channel: Channel<Element>) {
            this.channel = channel.sref()
        }

        fun yield(value: Element): AsyncStream.Continuation.YieldResult<Element> {
            val result = channel.trySend(value)
            if (result.isClosed) {
                return YieldResult.terminated
            } else if (result.isFailure) {
                return YieldResult.dropped(value)
            } else {
                return YieldResult.enqueued(remaining = 0)
            }
        }

        fun yield(with: Result<Element, Never>): AsyncStream.Continuation.YieldResult<Element> {
            val result = with
            if (result is Result.SuccessCase) {
                val success = result.associated0
                return yield(success)
            } else {
                finish()
                return YieldResult.terminated
            }
        }

        fun yield(): AsyncStream.Continuation.YieldResult<Element> = yield(Unit as Element)

        fun finish() {
            channel.close()
        }

        var onTermination: ((AsyncStream.Continuation.Termination) -> Unit)? = null

        @androidx.annotation.Keep
        companion object {
        }
    }

    internal var continuation: AsyncStream.Continuation<Element>? = null // Internal for makeStream()

    var swiftDataSource: AsyncStreamSwiftDataSource<Element>? = null
        private set // Bridging support
    private var producer: (suspend () -> Element?)? = null
    private var onCancel: (() -> Unit)? = null

    constructor(elementType: KClass<Element>? = null, bufferingPolicy: AsyncStream.Continuation.BufferingPolicy = Continuation.BufferingPolicy.unbounded, build: (AsyncStream.Continuation<Element>) -> Unit) {
        val limit = bufferingPolicy
        val channel: Channel<Element>
        when (limit) {
            is AsyncStream.Continuation.BufferingPolicy.BufferingNewestCase -> {
                val capacity = limit.associated0
                channel = Channel<Element>(capacity, onBufferOverflow = BufferOverflow.DROP_OLDEST)
            }
            is AsyncStream.Continuation.BufferingPolicy.BufferingOldestCase -> {
                val capacity = limit.associated0
                channel = Channel<Element>(capacity, onBufferOverflow = BufferOverflow.DROP_LATEST)
            }
            is AsyncStream.Continuation.BufferingPolicy.UnboundedCase -> channel = Channel<Element>(Channel.UNLIMITED)
        }
        this.continuation = Continuation<Element>(channel = channel)
        build(continuation!!)
    }

    constructor(unfolding: suspend () -> Element?, onCancel: (() -> Unit)? = null) {
        val producer = unfolding
        this.producer = producer
        this.onCancel = onCancel
    }

    constructor(flow: kotlinx.coroutines.flow.Flow<Element>, bufferingPolicy: AsyncStream.Continuation.BufferingPolicy = Continuation.BufferingPolicy.unbounded): this(null, bufferingPolicy, { continuation ->
        Task { ->
            flow.collect { value -> continuation.yield(value) }
            continuation.finish()
        }
    }) {
    }

    constructor(swiftDataSource: AsyncStreamSwiftDataSource<Element>) {
        this.swiftDataSource = swiftDataSource
        this.producer = { -> Async.run { swiftDataSource.next() } }
    }

    override fun makeAsyncIterator(): AsyncStream.Iterator<Element> = Iterator<Element>(stream = this)

    class Iterator<Element>: AsyncIteratorProtocol<Element> where Element: Any {
        private val stream: AsyncStream<Element>

        internal constructor(stream: AsyncStream<Element>) {
            this.stream = stream
        }

        override suspend fun next(): Element? = Async.run l@{
            if (Task.isCancelled) {
                onCancel()
                return@l null
            }
            withTaskCancellationHandler(operation = { -> Async.run l@{
                val matchtarget_0 = stream.continuation?.channel
                if (matchtarget_0 != null) {
                    val channel = matchtarget_0
                    val result = channel.receiveCatching()
                    if (result.isClosed) {
                        stream.continuation?.onTermination?.let { onTermination ->
                            stream.continuation?.onTermination = null
                            onTermination(Continuation.Termination.finished)
                        }
                        return@l null
                    } else {
                        return@l result.getOrNull()
                    }
                } else {
                    val matchtarget_1 = stream.producer
                    if (matchtarget_1 != null) {
                        val producer = matchtarget_1
                        return@l producer()
                    } else {
                        return@l null
                    }
                }
            } }, onCancel = { -> onCancel() })
        }

        private fun onCancel() {
            stream.continuation?.channel?.close()
            val matchtarget_2 = stream.continuation?.onTermination
            if (matchtarget_2 != null) {
                val onTermination = matchtarget_2
                stream.continuation?.onTermination = null
                onTermination(AsyncStream.Continuation.Termination.cancelled)
            } else {
                stream.onCancel?.let { onCancel ->
                    stream.onCancel = null
                    onCancel()
                }
            }
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    override fun kotlin(nocopy: Boolean): kotlinx.coroutines.flow.Flow<Element> {
        val matchtarget_3 = continuation?.channel
        if (matchtarget_3 != null) {
            val channel = matchtarget_3
            return channel.consumeAsFlow()
        } else {
            val matchtarget_4 = producer
            if (matchtarget_4 != null) {
                val producer = matchtarget_4
                return flow { ->
                    while (true) {
                        val matchtarget_5 = producer()
                        if (matchtarget_5 != null) {
                            val value = matchtarget_5
                            emit(value)
                        } else {
                            break
                        }
                    }
                }
            } else {
                return flow { ->  }
            }
        }
    }

    @androidx.annotation.Keep
    companion object {
    }
}

fun <Element> AsyncStream.Companion.makeStream(of: KClass<Element>? = null, bufferingPolicy: AsyncStream.Continuation.BufferingPolicy = AsyncStream.Continuation.BufferingPolicy.unbounded): Tuple2<AsyncStream<Element>, AsyncStream.Continuation<Element>> where Element: Any {
    val elementType = of
    val limit = bufferingPolicy
    val stream = AsyncStream<Element>(bufferingPolicy = limit) { _ ->  }
    return Tuple2(stream, stream.continuation!!)
}

// Unfortunately because of minor API differences between `AsyncStream` and `AsyncThrowingStream`, we can't
// really share any code between them

class AsyncThrowingStream<Element, Failure>: AsyncSequence<Element>, SwiftCustomBridged, KotlinConverting<kotlinx.coroutines.flow.Flow<Element>> where Element: Any, Failure: Error {
    class Continuation<Element, Failure> where Element: Any, Failure: Error {
        sealed class Termination<out Failure> {
            class FinishedCase<Failure>(val associated0: Failure?): Termination<Failure>() {
                override fun equals(other: Any?): Boolean {
                    if (other !is FinishedCase<*>) return false
                    return associated0 == other.associated0
                }
                override fun hashCode(): Int {
                    var result = 1
                    result = Hasher.combine(result, associated0)
                    return result
                }
            }
            class CancelledCase: Termination<Nothing>() {
            }

            @androidx.annotation.Keep
            companion object {
                fun <Failure> finished(associated0: Failure?): Termination<Failure> = FinishedCase(associated0)
                val cancelled: Termination<Nothing> = CancelledCase()
            }
        }

        sealed class YieldResult<out Element> {
            class EnqueuedCase(val associated0: Int): YieldResult<Nothing>() {
                val remaining = associated0
            }
            class DroppedCase<Element>(val associated0: Element): YieldResult<Element>() {
            }
            class TerminatedCase: YieldResult<Nothing>() {
            }

            @androidx.annotation.Keep
            companion object {
                fun enqueued(remaining: Int): YieldResult<Nothing> = EnqueuedCase(remaining)
                fun <Element> dropped(associated0: Element): YieldResult<Element> = DroppedCase(associated0)
                val terminated: YieldResult<Nothing> = TerminatedCase()
            }
        }

        sealed class BufferingPolicy {
            class UnboundedCase: BufferingPolicy() {
            }
            class BufferingOldestCase(val associated0: Int): BufferingPolicy() {
            }
            class BufferingNewestCase(val associated0: Int): BufferingPolicy() {
            }

            @androidx.annotation.Keep
            companion object {
                val unbounded: BufferingPolicy = UnboundedCase()
                fun bufferingOldest(associated0: Int): BufferingPolicy = BufferingOldestCase(associated0)
                fun bufferingNewest(associated0: Int): BufferingPolicy = BufferingNewestCase(associated0)
            }
        }

        internal val channel: Channel<Element>
        internal var terminationError: Failure? = null
            get() = field.sref({ this.terminationError = it })
            set(newValue) {
                field = newValue.sref()
            }

        constructor(channel: Channel<Element>) {
            this.channel = channel.sref()
        }

        fun yield(value: Element): AsyncThrowingStream.Continuation.YieldResult<Element> {
            val result = channel.trySend(value)
            if (result.isClosed) {
                return YieldResult.terminated
            } else if (result.isFailure) {
                return YieldResult.dropped(value)
            } else {
                return YieldResult.enqueued(remaining = 0)
            }
        }

        fun yield(with: Result<Element, Failure>): AsyncThrowingStream.Continuation.YieldResult<Element> {
            val result = with
            when (result) {
                is Result.SuccessCase -> {
                    val success = result.associated0
                    return yield(success)
                }
                is Result.FailureCase -> {
                    val error = result.associated0
                    finish(throwing = error)
                    return YieldResult.terminated
                }
            }
        }

        fun yield(): AsyncThrowingStream.Continuation.YieldResult<Element> = yield(Unit as Element)

        fun finish(throwing: Failure? = null) {
            val error = throwing
            channel.close()
            terminationError = error
        }

        var onTermination: ((AsyncThrowingStream.Continuation.Termination<*>) -> Unit)? = null

        @androidx.annotation.Keep
        companion object {
        }
    }

    internal var continuation: AsyncThrowingStream.Continuation<Element, Failure>? = null // Internal for makeStream()

    var swiftDataSource: AsyncThrowingStreamSwiftDataSource<Element>? = null
        private set // Bridging support
    private var producer: (suspend () -> Element?)? = null
    private var onCancel: (() -> Unit)? = null

    constructor(elementType: KClass<Element>? = null, bufferingPolicy: AsyncThrowingStream.Continuation.BufferingPolicy = Continuation.BufferingPolicy.unbounded, build: (AsyncThrowingStream.Continuation<Element, Failure>) -> Unit) {
        val limit = bufferingPolicy
        val channel: Channel<Element>
        when (limit) {
            is AsyncThrowingStream.Continuation.BufferingPolicy.BufferingNewestCase -> {
                val capacity = limit.associated0
                channel = Channel<Element>(capacity, onBufferOverflow = BufferOverflow.DROP_OLDEST)
            }
            is AsyncThrowingStream.Continuation.BufferingPolicy.BufferingOldestCase -> {
                val capacity = limit.associated0
                channel = Channel<Element>(capacity, onBufferOverflow = BufferOverflow.DROP_LATEST)
            }
            is AsyncThrowingStream.Continuation.BufferingPolicy.UnboundedCase -> channel = Channel<Element>(Channel.UNLIMITED)
        }
        this.continuation = Continuation<Element, Failure>(channel = channel)
        build(continuation!!)
    }

    constructor(unfolding: suspend () -> Element?, onCancel: (() -> Unit)? = null) {
        val producer = unfolding
        this.producer = producer
        this.onCancel = onCancel
    }

    constructor(flow: kotlinx.coroutines.flow.Flow<Element>, bufferingPolicy: AsyncThrowingStream.Continuation.BufferingPolicy = Continuation.BufferingPolicy.unbounded): this(null, bufferingPolicy, { continuation ->
        Task { ->
            try {
                flow.collect { value -> continuation.yield(value) }
                continuation.finish()
            } catch (error: Throwable) {
                @Suppress("NAME_SHADOWING") val error = error.aserror()
                continuation.finish(throwing = error as? Failure)
            }
        }
    }) {
    }

    constructor(swiftDataSource: AsyncThrowingStreamSwiftDataSource<Element>) {
        this.swiftDataSource = swiftDataSource
        this.producer = { -> Async.run {
            swiftDataSource.next()
        } }
    }

    override fun makeAsyncIterator(): AsyncThrowingStream.Iterator<Element, Failure> = Iterator<Element, Failure>(stream = this)

    class Iterator<Element, Failure>: AsyncIteratorProtocol<Element> where Element: Any, Failure: Error {
        private val stream: AsyncThrowingStream<Element, Failure>

        internal constructor(stream: AsyncThrowingStream<Element, Failure>) {
            this.stream = stream
        }

        override suspend fun next(): Element? = Async.run l@{
            if (Task.isCancelled) {
                onCancel()
                return@l null
            }
            withTaskCancellationHandler(operation = { -> Async.run l@{
                val matchtarget_6 = stream.continuation?.channel
                if (matchtarget_6 != null) {
                    val channel = matchtarget_6
                    val result = channel.receiveCatching()
                    if (result.isClosed) {
                        val terminationError = stream.continuation?.terminationError.sref()
                        stream.continuation?.terminationError = null
                        stream.continuation?.onTermination?.let { onTermination ->
                            stream.continuation?.onTermination = null
                            onTermination(Continuation.Termination.finished(terminationError))
                        }
                        if (terminationError != null) {
                            throw (terminationError as Throwable) as Throwable
                        } else {
                            return@l null
                        }
                    } else {
                        return@l result.getOrThrow()
                    }
                } else {
                    val matchtarget_7 = stream.producer
                    if (matchtarget_7 != null) {
                        val producer = matchtarget_7
                        return@l producer()
                    } else {
                        return@l null
                    }
                }
            } }, onCancel = { -> onCancel() })
        }

        private fun onCancel() {
            stream.continuation?.channel?.close()
            val matchtarget_8 = stream.continuation?.onTermination
            if (matchtarget_8 != null) {
                val onTermination = matchtarget_8
                stream.continuation?.onTermination = null
                onTermination(AsyncThrowingStream.Continuation.Termination.cancelled)
            } else {
                stream.onCancel?.let { onCancel ->
                    stream.onCancel = null
                    onCancel()
                }
            }
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    override fun kotlin(nocopy: Boolean): kotlinx.coroutines.flow.Flow<Element> {
        val matchtarget_9 = continuation?.channel
        if (matchtarget_9 != null) {
            val channel = matchtarget_9
            return channel.consumeAsFlow()
        } else {
            val matchtarget_10 = producer
            if (matchtarget_10 != null) {
                val producer = matchtarget_10
                return flow { ->
                    while (true) {
                        val matchtarget_11 = producer()
                        if (matchtarget_11 != null) {
                            val value = matchtarget_11
                            emit(value)
                        } else {
                            break
                        }
                    }
                }
            } else {
                return flow { ->  }
            }
        }
    }

    @androidx.annotation.Keep
    companion object {
    }
}

fun <Element> AsyncThrowingStream.Companion.makeStream(of: KClass<Element>? = null, throwing: KClass<*>? = null, bufferingPolicy: AsyncThrowingStream.Continuation.BufferingPolicy = AsyncThrowingStream.Continuation.BufferingPolicy.unbounded): Tuple2<AsyncThrowingStream<Element, Error>, AsyncThrowingStream.Continuation<Element, Error>> where Element: Any {
    val elementType = of
    val failureType = throwing
    val limit = bufferingPolicy
    val stream = AsyncThrowingStream<Element, Error>(bufferingPolicy = limit) { _ ->  }
    return Tuple2(stream, stream.continuation!!)
}

// MARK: Bridge Support

class AsyncStreamBridgingDataSource {
    private var stream: AsyncStream<Any>? = null
    private var throwingStream: AsyncThrowingStream<Any, Error>? = null
    private var flow: kotlinx.coroutines.flow.Flow<Any>? = null
        get() = field.sref({ this.flow = it })
        set(newValue) {
            field = newValue.sref()
        }

    constructor(stream: AsyncStream<Any>) {
        this.stream = stream
    }

    constructor(stream: AsyncThrowingStream<Any, Error>) {
        this.throwingStream = stream
    }

    constructor(flow: kotlinx.coroutines.flow.Flow<Any>) {
        this.flow = flow
    }

    fun collect(onNext: (Any) -> Unit, onFinish: (Throwable?) -> Unit) {
        Task { ->
            val matchtarget_12 = stream
            if (matchtarget_12 != null) {
                val stream = matchtarget_12
                val itr = stream.makeAsyncIterator()
                while (true) {
                    val next_0 = itr.next()
                    if (next_0 == null) {
                        break
                    }
                    onNext(next_0)
                }
                onFinish(null)
            } else {
                val matchtarget_13 = throwingStream
                if (matchtarget_13 != null) {
                    val throwingStream = matchtarget_13
                    val itr = throwingStream.makeAsyncIterator()
                    try {
                        while (true) {
                            val next_1 = itr.next()
                            if (next_1 == null) {
                                break
                            }
                            onNext(next_1)
                        }
                        onFinish(null)
                    } catch (error: Throwable) {
                        @Suppress("NAME_SHADOWING") val error = error.aserror()
                        onFinish(error as? Throwable)
                    }
                } else {
                    flow.sref()?.let { flow ->
                        try {
                            flow.collect { it -> onNext(it) }
                            onFinish(null)
                        } catch (error: Throwable) {
                            @Suppress("NAME_SHADOWING") val error = error.aserror()
                            onFinish(error as? Throwable)
                        }
                    }
                }
            }
        }
    }

    @androidx.annotation.Keep
    companion object {
    }
}

class AsyncStreamSwiftDataSource<Element> {
    var Swift_producer: Long
        private set

    constructor(Swift_producer: Long) {
        this.Swift_producer = Swift_producer
    }

    fun finalize() {
        Swift_producer = Swift_release(Swift_producer)
    }

    suspend fun next(): Element? = Async.run l@{
        return@l suspendCoroutine { continuation ->
            Swift_next(Swift_producer) { element -> continuation.resume(element as? Element) }
        }
    }

    fun asFlow(): kotlinx.coroutines.flow.Flow<Element> {
        return flow { ->
            while (true) {
                val matchtarget_14 = next()
                if (matchtarget_14 != null) {
                    val value = matchtarget_14
                    emit(value)
                } else {
                    break
                }
            }
        }
    }

    // @JvmName is needed for test cases, as the name is otherwise mangled to append "$SkipLib_debug"
    @JvmName("Swift_next")
    internal external fun Swift_next(Swift_producer: Long, callback: (Any?) -> Unit)
    internal external fun Swift_release(Swift_producer: Long): Long

    @androidx.annotation.Keep
    companion object {
    }
}

class AsyncThrowingStreamSwiftDataSource<Element> {
    var Swift_producer: Long
        private set

    constructor(Swift_producer: Long) {
        this.Swift_producer = Swift_producer
    }

    fun finalize() {
        Swift_producer = Swift_release(Swift_producer = Swift_producer)
    }

    suspend fun next(): Element? = Async.run l@{
        return@l suspendCoroutine { continuation ->
            Swift_next(Swift_producer) { element, error ->
                if (error != null) {
                    continuation.resumeWithException(error)
                } else {
                    continuation.resume(element as? Element)
                }
            }
        }
    }

    fun asFlow(): kotlinx.coroutines.flow.Flow<Element> {
        return flow { ->
            while (true) {
                val matchtarget_15 = next()
                if (matchtarget_15 != null) {
                    val value = matchtarget_15
                    emit(value)
                } else {
                    break
                }
            }
        }
    }

    // @JvmName is needed for test cases, as the name is otherwise mangled to append "$SkipLib_debug"
    @JvmName("Swift_next")
    internal external fun Swift_next(Swift_producer: Long, callback: (Any?, Throwable?) -> Unit)
    internal external fun Swift_release(Swift_producer: Long): Long

    @androidx.annotation.Keep
    companion object {
    }
}

