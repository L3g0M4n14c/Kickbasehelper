package skip.lib

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
class UnsafeMutableBufferPointer<Element> {

    @androidx.annotation.Keep
    companion object {
    }
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
class UnsafeBufferPointer<Element> {

    @androidx.annotation.Keep
    companion object {
    }
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
class UnsafePointer<Pointee> {

    @androidx.annotation.Keep
    companion object {
    }
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
class UnsafeMutablePointer<Pointee> {

    @androidx.annotation.Keep
    companion object {
    }
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
class UnsafeMutableRawBufferPointer {

    @androidx.annotation.Keep
    companion object {
    }
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
class UnsafeRawBufferPointer {

    @androidx.annotation.Keep
    companion object {
    }
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
class UnsafeRawPointer {

    @androidx.annotation.Keep
    companion object {
    }
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
class UnsafeMutableRawPointer {

    @androidx.annotation.Keep
    companion object {
    }
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
fun <T, Result> withUnsafeMutableBytes(of: InOut<T>, body: (Any) -> Result): Result {
    val value = of
    fatalError()
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
fun <T, Result> withUnsafeBytes(of: InOut<T>, body: (Any) -> Result): Result {
    val value = of
    fatalError()
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
fun <T, Result> withUnsafeBytes(of: T, body: (Any) -> Result): Result {
    val value = of
    fatalError()
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
fun <R> withUnsafeTemporaryAllocation(byteCount: Int, alignment: Int, body: (Any) -> R): R {
    fatalError()
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
fun <R> withUnsafeTemporaryAllocation(of: Any, capacity: Int, body: (Any) -> R): R {
    val type = of
    fatalError()
}
