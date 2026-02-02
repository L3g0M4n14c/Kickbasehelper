package skip.lib

import kotlin.reflect.KClass
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

interface CodingKey: CustomDebugStringConvertible {
    val rawValue: String
    val stringValue: String
        get() = rawValue

    val intValue: Int?
        get() = Int(rawValue)

    val description: String
        get() = rawValue

    override val debugDescription: String
        get() = rawValue
}

interface CodingKeyRepresentable {
    val codingKey: CodingKey
}

class CodingUserInfoKey: RawRepresentable<String> {
    override val rawValue: String
    constructor(rawValue: String) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is CodingUserInfoKey) return false
        return rawValue == other.rawValue
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, rawValue)
        return result
    }

    @androidx.annotation.Keep
    companion object {
    }
}

sealed class EncodingError: Exception(), Error {

    class InvalidValueCase(val associated0: Any, val associated1: EncodingError.Context): EncodingError() {
    }
    class Context {
        val codingPath: Array<CodingKey>
        val debugDescription: String
        val underlyingError: Error?

        constructor(codingPath: Array<CodingKey>, debugDescription: String, underlyingError: Error? = null) {
            this.codingPath = codingPath.sref()
            this.debugDescription = debugDescription
            this.underlyingError = underlyingError.sref()
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    @androidx.annotation.Keep
    companion object {
        fun invalidValue(associated0: Any, associated1: EncodingError.Context): EncodingError = InvalidValueCase(associated0, associated1)
    }
}

sealed class DecodingError: Exception(), Error {

    class TypeMismatchCase(val associated0: KClass<*>, val associated1: DecodingError.Context): DecodingError() {
    }
    class ValueNotFoundCase(val associated0: KClass<*>, val associated1: DecodingError.Context): DecodingError() {
    }
    class KeyNotFoundCase(val associated0: CodingKey, val associated1: DecodingError.Context): DecodingError() {
    }
    class DataCorruptedCase(val associated0: DecodingError.Context): DecodingError() {
    }
    class Context {
        val codingPath: Array<CodingKey>
        val debugDescription: String
        val underlyingError: Error?

        constructor(codingPath: Array<CodingKey>, debugDescription: String, underlyingError: Error? = null) {
            this.codingPath = codingPath.sref()
            this.debugDescription = debugDescription
            this.underlyingError = underlyingError.sref()
        }

        @androidx.annotation.Keep
        companion object {
        }
    }

    @androidx.annotation.Keep
    companion object {
        fun typeMismatch(associated0: KClass<*>, associated1: DecodingError.Context): DecodingError = TypeMismatchCase(associated0, associated1)
        fun valueNotFound(associated0: KClass<*>, associated1: DecodingError.Context): DecodingError = ValueNotFoundCase(associated0, associated1)
        fun keyNotFound(associated0: CodingKey, associated1: DecodingError.Context): DecodingError = KeyNotFoundCase(associated0, associated1)
        fun dataCorrupted(associated0: DecodingError.Context): DecodingError = DataCorruptedCase(associated0)


        fun <C> dataCorruptedError(forKey: CodingKey, in_: C, debugDescription: String): DecodingError where C: KeyedDecodingContainerProtocol<*> {
            val key = forKey
            val container = in_
            return DecodingError.dataCorrupted(DecodingError.Context(codingPath = container.codingPath + arrayOf(key), debugDescription = debugDescription))
        }

        fun dataCorruptedError(in_: UnkeyedDecodingContainer, debugDescription: String): DecodingError {
            val container = in_
            return DecodingError.dataCorrupted(DecodingError.Context(codingPath = container.codingPath, debugDescription = debugDescription))
        }

        fun dataCorruptedError(in_: SingleValueDecodingContainer, debugDescription: String): DecodingError {
            val container = in_
            return DecodingError.dataCorrupted(DecodingError.Context(codingPath = container.codingPath, debugDescription = debugDescription))
        }
    }
}

