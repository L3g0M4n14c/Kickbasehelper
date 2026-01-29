package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

@androidx.annotation.Keep
@Suppress("MUST_BE_INITIALIZED")
class Angle: Codable, Comparable<Angle>, MutableStruct {

    var radians: Double
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var degrees: Double
        get() = Companion.radiansToDegrees(radians)
        set(newValue) {
            radians = Companion.degreesToRadians(newValue)
        }

    constructor() {
        this.radians = 0.0
    }

    constructor(radians: Double) {
        this.radians = radians
    }

    constructor(degrees: Double, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) {
        this.radians = Companion.degreesToRadians(degrees)
    }

    override operator fun compareTo(other: Angle): Int {
        if (this == other) return 0
        fun islessthan(lhs: Angle, rhs: Angle): Boolean {
            return lhs.radians < rhs.radians
        }
        return if (islessthan(this, other)) -1 else 1
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as Angle
        this.radians = copy.radians
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = Angle(this as MutableStruct)

    override fun equals(other: Any?): Boolean {
        if (other !is Angle) return false
        return radians == other.radians
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, radians)
        return result
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        radians("radians");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "radians" -> CodingKeys.radians
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(radians, forKey = CodingKeys.radians)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.radians = container.decode(Double::class, forKey = CodingKeys.radians)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<Angle> {
        var zero = Angle()
            get() = field.sref({ this.zero = it })
            set(newValue) {
                field = newValue.sref()
            }

        fun radians(radians: Double): Angle = Angle(radians = radians)

        fun degrees(degrees: Double): Angle = Angle(degrees = degrees)

        private fun radiansToDegrees(radians: Double): Double = radians * 180 / Double.pi

        private fun degreesToRadians(degrees: Double): Double = degrees * Double.pi / 180

        override fun init(from: Decoder): Angle = Angle(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

