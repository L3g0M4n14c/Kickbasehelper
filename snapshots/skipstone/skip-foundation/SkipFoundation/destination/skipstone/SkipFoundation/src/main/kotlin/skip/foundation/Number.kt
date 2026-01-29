package skip.foundation

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

typealias Decimal = java.math.BigDecimal
typealias NSDecimalNumber = java.math.BigDecimal
typealias NSNumber = java.lang.Number

val java.lang.Number.doubleValue: Double
    get() = doubleValue()
val java.lang.Number.intValue: Int
    get() = intValue()
val java.lang.Number.longValue: Long
    get() = longValue()
val java.lang.Number.int64Value: Long
    get() = longValue()
val java.lang.Number.int32Value: Int
    get() = intValue()
val java.lang.Number.int16Value: Short
    get() = shortValue()
val java.lang.Number.int8Value: Byte
    get() = byteValue()

// Initializing an NSNumber with a numeric value just returns the instance itself
fun NSNumber(value: Byte): java.lang.Number = value as java.lang.Number
fun NSNumber(value: Short): java.lang.Number = value as java.lang.Number
fun NSNumber(value: Int): java.lang.Number = value as java.lang.Number
fun NSNumber(value: Long): java.lang.Number = value as java.lang.Number
fun NSNumber(value: UByte): java.lang.Number = value as java.lang.Number
fun NSNumber(value: UShort): java.lang.Number = value as java.lang.Number
fun NSNumber(value: UInt): java.lang.Number = value as java.lang.Number
fun NSNumber(value: ULong): java.lang.Number = value as java.lang.Number
fun NSNumber(value: Float): java.lang.Number = value as java.lang.Number
fun NSNumber(value: Double): java.lang.Number = value as java.lang.Number

// NSNumber also accepts unlabeled values. Add an additional unused argument to satisfy the Kotlin compiler that they are different functions.
fun NSNumber(v: Byte, unusedp: Unit? = null): java.lang.Number = v as java.lang.Number
fun NSNumber(v: Short, unusedp: Unit? = null): java.lang.Number = v as java.lang.Number
fun NSNumber(v: Int, unusedp: Unit? = null): java.lang.Number = v as java.lang.Number
fun NSNumber(v: Long, unusedp: Unit? = null): java.lang.Number = v as java.lang.Number
fun NSNumber(v: UByte, unusedp: Unit? = null): java.lang.Number = v as java.lang.Number
fun NSNumber(v: UShort, unusedp: Unit? = null): java.lang.Number = v as java.lang.Number
fun NSNumber(v: UInt, unusedp: Unit? = null): java.lang.Number = v as java.lang.Number
fun NSNumber(v: ULong, unusedp: Unit? = null): java.lang.Number = v as java.lang.Number
fun NSNumber(v: Float, unusedp: Unit? = null): java.lang.Number = v as java.lang.Number
fun NSNumber(v: Double, unusedp: Unit? = null): java.lang.Number = v as java.lang.Number

fun Decimal(string: String, locale: Locale? = null): java.math.BigDecimal? {
    try {
        return java.math.BigDecimal(string)
    } catch (error: Throwable) {
        @Suppress("NAME_SHADOWING") val error = error.aserror()
        return null
    }
}

//    static let zero = java.math.BigDecimal(0)
//    static let pi = java.math.BigDecimal("3.14159265358979323846264338327950288419")
//
//    @available(*, unavailable)
//    static let nan = java.math.BigDecimal(0) // BigDecimal class provides no representation of nan

