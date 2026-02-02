package skip.lib

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

/// Kotlin representation of `Swift.Result`.
///
/// - Note: We do not map to `KotlinConverting<kotlin.Result...>` because `KotlinResult` is a value type
///   that is near impossible to use with our bridging reflection.
sealed class Result<out Success, out Failure>: KotlinConverting<Pair<*, *>>, SwiftCustomBridged where Failure: Error {
    class SuccessCase<Success>(val associated0: Success): Result<Success, Nothing>() {
    }
    class FailureCase<Failure>(val associated0: Failure): Result<Nothing, Failure>() where Failure: Error {
    }

    fun get(): Success {
        when (this) {
            is Result.SuccessCase -> {
                val success = this.associated0
                return success.sref()
            }
            is Result.FailureCase -> {
                val failure = this.associated0
                throw failure as Throwable
            }
        }
    }

    override fun kotlin(nocopy: Boolean): Pair<Success?, Failure?> {
        when (this) {
            is Result.SuccessCase -> {
                val success = this.associated0
                return Pair(success, null)
            }
            is Result.FailureCase -> {
                val failure = this.associated0
                return Pair(null, failure)
            }
        }
    }

    @androidx.annotation.Keep
    companion object {
        fun <Success> success(associated0: Success): Result<Success, Nothing> = SuccessCase(associated0)
        fun <Failure> failure(associated0: Failure): Result<Nothing, Failure> where Failure: Error = FailureCase(associated0)


        fun <Success, Failure> init(platformValue: Pair<Success?, Failure?>): Result<Success, Failure> where Failure: Error {
            val matchtarget_0 = platformValue.second
            if (matchtarget_0 != null) {
                val failure = matchtarget_0
                return Result.failure(failure)
            } else {
                return Result.success(platformValue.first!!)
            }
        }

        fun <Success, Failure> init(catching: () -> Success): Result<Success, Failure> where Failure: Error {
            val body = catching
            try {
                return Result.success(body())
            } catch (error: Throwable) {
                @Suppress("NAME_SHADOWING") val error = error.aserror()
                return Result.failure(error as Failure)
            }
        }
    }
}
fun <Success, Failure> Result(catching: () -> Success): Result<Success, Failure> where Failure: Error {
    val body = catching
    return Result.init(catching = body)
}
fun <Success, Failure> Result(platformValue: Pair<Success?, Failure?>): Result<Success, Failure> where Failure: Error = Result.init(platformValue = platformValue)

