package skip.foundation

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

typealias NSDate = Date
typealias TimeInterval = Double
typealias CFTimeInterval = Double

// Mirror Double's cast functions, which typealiasing doesn't cover
fun TimeInterval(number: Number): Double = Double(number = number)
fun TimeInterval(number: UByte): Double = Double(number = number)
fun TimeInterval(number: UShort): Double = Double(number = number)
fun TimeInterval(number: UInt): Double = Double(number = number)
fun TimeInterval(number: ULong): Double = Double(number = number)
fun TimeInterval(string: String): Double? = Double(string = string)

// Mimic the constructor for `TimeInterval()` with an Int.
fun TimeInterval(seconds: Int): Double = seconds.toDouble()

typealias CFAbsoluteTime = Double

fun CFAbsoluteTimeGetCurrent(): Double = Date.timeIntervalSinceReferenceDate

@androidx.annotation.Keep
@Suppress("MUST_BE_INITIALIZED")
class Date: Comparable<Date>, Codable, KotlinConverting<java.util.Date>, SwiftCustomBridged, MutableStruct {
    internal var platformValue: java.util.Date
        get() = field.sref({ this.platformValue = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    constructor() {
        this.platformValue = java.util.Date()
    }

    constructor(platformValue: java.util.Date) {
        this.platformValue = platformValue
    }

    constructor(from: Decoder) {
        val decoder = from
        val container = decoder.singleValueContainer()
        val timeIntervalSinceReferenceDate = container.decode(Double::class)
        this.platformValue = java.util.Date(((timeIntervalSinceReferenceDate + Date.timeIntervalBetween1970AndReferenceDate) * 1000.0).toLong())
    }

    override fun encode(to: Encoder) {
        val encoder = to
        var container = encoder.singleValueContainer()
        container.encode(this.timeIntervalSinceReferenceDate)
    }

    constructor(timeIntervalSince1970: Double, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) {
        this.platformValue = java.util.Date((timeIntervalSince1970 * 1000.0).toLong())
    }

    constructor(timeIntervalSince1970: Int, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null): this(timeIntervalSince1970 = TimeInterval(timeIntervalSince1970)) {
    }

    constructor(timeIntervalSinceReferenceDate: Double) {
        this.platformValue = java.util.Date(((timeIntervalSinceReferenceDate + Date.timeIntervalBetween1970AndReferenceDate) * 1000.0).toLong())
    }

    constructor(timeIntervalSinceReferenceDate: Int): this(timeIntervalSinceReferenceDate = TimeInterval(timeIntervalSinceReferenceDate)) {
    }

    constructor(timeInterval: Double, since: Date): this(timeIntervalSince1970 = timeInterval + since.timeIntervalSince1970) {
    }

    constructor(timeInterval: Int, since: Date): this(timeInterval = TimeInterval(timeInterval), since = since) {
    }

    constructor(timeIntervalSinceNow: Double, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null, @Suppress("UNUSED_PARAMETER") unusedp_1: Nothing? = null): this(timeInterval = timeIntervalSinceNow, since = Date()) {
    }

    constructor(timeIntervalSinceNow: Int, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null, @Suppress("UNUSED_PARAMETER") unusedp_1: Nothing? = null): this(timeIntervalSinceNow = TimeInterval(timeIntervalSinceNow)) {
    }

    /// Useful for converting to Java's `long` time representation
    val currentTimeMillis: Long
        get() = platformValue.getTime()

    val description: String
        get() = description(with = null)

    fun description(with: Locale?): String {
        val locale = with
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", (locale ?: Locale.current).platformValue)
        fmt.setTimeZone(TimeZone.gmt.platformValue)
        return fmt.format(platformValue)
    }

    override operator fun compareTo(other: Date): Int {
        if (this == other) return 0
        fun islessthan(lhs: Date, rhs: Date): Boolean {
            return lhs.platformValue < rhs.platformValue
        }
        return if (islessthan(this, other)) -1 else 1
    }

    fun compare(with: Date): ComparisonResult {
        if (this == with) {
            return ComparisonResult.orderedSame
        } else if (this < with) {
            return ComparisonResult.orderedAscending
        } else {
            return ComparisonResult.orderedDescending
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Date) {
            return false
        }
        val lhs = this
        val rhs = other
        return lhs.platformValue == rhs.platformValue
    }

    override fun hashCode(): Int {
        var hasher = Hasher()
        hash(into = InOut<Hasher>({ hasher }, { hasher = it }))
        return hasher.finalize()
    }
    fun hash(into: InOut<Hasher>) {
        val hasher = into
        hasher.value.combine(platformValue)
    }

    val timeIntervalSince1970: Double
        get() = currentTimeMillis.toDouble() / 1000.0

    val timeIntervalSinceReferenceDate: Double
        get() = timeIntervalSince1970 - Date.timeIntervalBetween1970AndReferenceDate

    fun timeIntervalSince(date: Date): Double = timeIntervalSince1970 - date.timeIntervalSince1970

    val timeIntervalSinceNow: Double
        get() = timeIntervalSince1970 - Date().timeIntervalSince1970

    fun addingTimeInterval(timeInterval: Double): Date = Date(timeInterval = timeInterval, since = this)

    fun addingTimeInterval(timeInterval: Int): Date = Date(timeInterval = timeInterval, since = this)

    fun addTimeInterval(timeInterval: Double) {
        willmutate()
        try {
            assignfrom(addingTimeInterval(timeInterval))
        } finally {
            didmutate()
        }
    }

    fun addTimeInterval(timeInterval: Int) {
        willmutate()
        try {
            assignfrom(addingTimeInterval(timeInterval))
        } finally {
            didmutate()
        }
    }

    fun advanced(by: Double): Date {
        val n = by
        return addingTimeInterval(n)
    }

    fun distance(to: Date): Double {
        val other = to
        return other.timeIntervalSince1970 - timeIntervalSince1970
    }

    fun ISO8601Format(style: Date.ISO8601FormatStyle = Date.ISO8601FormatStyle.iso8601): String {
        // TODO: use the style parameters
        // local time zone specific
        // return java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.getDefault()).format(platformValue)
        var dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
        dateFormat.timeZone = java.util.TimeZone.getTimeZone("GMT")
        return dateFormat.format(platformValue)
    }

    @Suppress("MUST_BE_INITIALIZED")
    class ISO8601FormatStyle: MutableStruct {

        enum class TimeZoneSeparator(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<String> {
            colon("colon"),
            omitted("omitted");

            @androidx.annotation.Keep
            companion object {
                fun init(rawValue: String): Date.ISO8601FormatStyle.TimeZoneSeparator? {
                    return when (rawValue) {
                        "colon" -> TimeZoneSeparator.colon
                        "omitted" -> TimeZoneSeparator.omitted
                        else -> null
                    }
                }
            }
        }

        enum class DateSeparator(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<String> {
            dash("dash"),
            omitted("omitted");

            @androidx.annotation.Keep
            companion object {
                fun init(rawValue: String): Date.ISO8601FormatStyle.DateSeparator? {
                    return when (rawValue) {
                        "dash" -> DateSeparator.dash
                        "omitted" -> DateSeparator.omitted
                        else -> null
                    }
                }
            }
        }

        enum class TimeSeparator(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<String> {
            colon("colon"),
            omitted("omitted");

            @androidx.annotation.Keep
            companion object {
                fun init(rawValue: String): Date.ISO8601FormatStyle.TimeSeparator? {
                    return when (rawValue) {
                        "colon" -> TimeSeparator.colon
                        "omitted" -> TimeSeparator.omitted
                        else -> null
                    }
                }
            }
        }

        enum class DateTimeSeparator(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<String> {
            space("space"),
            standard("standard");

            @androidx.annotation.Keep
            companion object {
                fun init(rawValue: String): Date.ISO8601FormatStyle.DateTimeSeparator? {
                    return when (rawValue) {
                        "space" -> DateTimeSeparator.space
                        "standard" -> DateTimeSeparator.standard
                        else -> null
                    }
                }
            }
        }

        var timeSeparator: Date.ISO8601FormatStyle.TimeSeparator
            set(newValue) {
                willmutate()
                field = newValue
                didmutate()
            }
        var includingFractionalSeconds: Boolean
            set(newValue) {
                willmutate()
                field = newValue
                didmutate()
            }
        var timeZoneSeparator: Date.ISO8601FormatStyle.TimeZoneSeparator
            set(newValue) {
                willmutate()
                field = newValue
                didmutate()
            }
        var dateSeparator: Date.ISO8601FormatStyle.DateSeparator
            set(newValue) {
                willmutate()
                field = newValue
                didmutate()
            }
        var dateTimeSeparator: Date.ISO8601FormatStyle.DateTimeSeparator
            set(newValue) {
                willmutate()
                field = newValue
                didmutate()
            }
        var timeZone: TimeZone
            get() = field.sref({ this.timeZone = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }

        constructor(dateSeparator: Date.ISO8601FormatStyle.DateSeparator = Date.ISO8601FormatStyle.DateSeparator.dash, dateTimeSeparator: Date.ISO8601FormatStyle.DateTimeSeparator = Date.ISO8601FormatStyle.DateTimeSeparator.standard, timeSeparator: Date.ISO8601FormatStyle.TimeSeparator = Date.ISO8601FormatStyle.TimeSeparator.colon, timeZoneSeparator: Date.ISO8601FormatStyle.TimeZoneSeparator = Date.ISO8601FormatStyle.TimeZoneSeparator.omitted, includingFractionalSeconds: Boolean = false, timeZone: TimeZone = TimeZone(secondsFromGMT = 0)) {
            this.dateSeparator = dateSeparator
            this.dateTimeSeparator = dateTimeSeparator
            this.timeSeparator = timeSeparator
            this.timeZoneSeparator = timeZoneSeparator
            this.includingFractionalSeconds = includingFractionalSeconds
            this.timeZone = timeZone
        }

        private constructor(copy: MutableStruct) {
            @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as Date.ISO8601FormatStyle
            this.timeSeparator = copy.timeSeparator
            this.includingFractionalSeconds = copy.includingFractionalSeconds
            this.timeZoneSeparator = copy.timeZoneSeparator
            this.dateSeparator = copy.dateSeparator
            this.dateTimeSeparator = copy.dateTimeSeparator
            this.timeZone = copy.timeZone
        }

        override var supdate: ((Any) -> Unit)? = null
        override var smutatingcount = 0
        override fun scopy(): MutableStruct = Date.ISO8601FormatStyle(this as MutableStruct)

        @androidx.annotation.Keep
        companion object {
            val iso8601 = ISO8601FormatStyle()

            fun TimeZoneSeparator(rawValue: String): Date.ISO8601FormatStyle.TimeZoneSeparator? = TimeZoneSeparator.init(rawValue = rawValue)

            fun DateSeparator(rawValue: String): Date.ISO8601FormatStyle.DateSeparator? = DateSeparator.init(rawValue = rawValue)

            fun TimeSeparator(rawValue: String): Date.ISO8601FormatStyle.TimeSeparator? = TimeSeparator.init(rawValue = rawValue)

            fun DateTimeSeparator(rawValue: String): Date.ISO8601FormatStyle.DateTimeSeparator? = DateTimeSeparator.init(rawValue = rawValue)
        }
    }

    override fun kotlin(nocopy: Boolean): java.util.Date = (if (nocopy) platformValue else platformValue.clone() as java.util.Date).sref()

    fun formatted(date: Date.FormatStyle.DateStyle, time: Date.FormatStyle.TimeStyle): String {
        val df = DateFormatter()

        when (date) {
            Date.FormatStyle.DateStyle.omitted -> df.dateStyle = DateFormatter.Style.none
            Date.FormatStyle.DateStyle.numeric -> df.dateStyle = DateFormatter.Style.short
            Date.FormatStyle.DateStyle.abbreviated -> df.dateStyle = DateFormatter.Style.medium
            Date.FormatStyle.DateStyle.long -> df.dateStyle = DateFormatter.Style.long
            Date.FormatStyle.DateStyle.complete -> df.dateStyle = DateFormatter.Style.full
            else -> df.dateStyle = DateFormatter.Style.short
        }

        when (time) {
            Date.FormatStyle.TimeStyle.omitted -> df.timeStyle = DateFormatter.Style.none
            Date.FormatStyle.TimeStyle.shortened -> df.timeStyle = DateFormatter.Style.short
            Date.FormatStyle.TimeStyle.standard -> df.timeStyle = DateFormatter.Style.medium
            Date.FormatStyle.TimeStyle.complete -> df.timeStyle = DateFormatter.Style.full
            else -> df.timeStyle = DateFormatter.Style.short
        }

        return df.string(from = this)
    }

    fun formatted(): String = formatted(date = Date.FormatStyle.DateStyle.numeric, time = Date.FormatStyle.TimeStyle.shortened)

    /// Strategies for formatting a `Date`.
    @Suppress("MUST_BE_INITIALIZED")
    class FormatStyle: MutableStruct {

        /// The locale to use when formatting date and time values.
        var locale: Locale
            set(newValue) {
                willmutate()
                field = newValue
                didmutate()
            }

        /// The time zone with which to specify date and time values.
        var timeZone: TimeZone
            get() = field.sref({ this.timeZone = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }

        /// The calendar to use for date values.
        var calendar: Calendar
            get() = field.sref({ this.calendar = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }

        /// The capitalization formatting context used when formatting date and time values.
        //public var capitalizationContext: FormatStyleCapitalizationContext

        /// Returns a type erased attributed variant of this style.
        //public var attributed: Date.AttributedStyle { get }

        //public init(date: Date.FormatStyle.DateStyle? = nil, time: Date.FormatStyle.TimeStyle? = nil, locale: Locale = .autoupdatingCurrent, calendar: Calendar = .autoupdatingCurrent, timeZone: TimeZone = .autoupdatingCurrent, capitalizationContext: FormatStyleCapitalizationContext = .unknown)

        /// Predefined date styles varied in lengths or the components included. The exact format depends on the locale.
        class DateStyle {
            internal val rawValue: Int

            internal constructor(rawValue: Int) {
                this.rawValue = rawValue
            }

            override fun equals(other: Any?): Boolean {
                if (other !is Date.FormatStyle.DateStyle) return false
                return rawValue == other.rawValue
            }

            override fun hashCode(): Int {
                var result = 1
                result = Hasher.combine(result, rawValue)
                return result
            }

            @androidx.annotation.Keep
            companion object {

                /// Excludes the date part.
                val omitted: Date.FormatStyle.DateStyle = DateStyle(rawValue = 0)

                /// Shows date components in their numeric form. For example, "10/21/2015".
                val numeric: Date.FormatStyle.DateStyle = DateStyle(rawValue = 1)

                /// Shows date components in their abbreviated form if possible. For example, "Oct 21, 2015".
                val abbreviated: Date.FormatStyle.DateStyle = DateStyle(rawValue = 2)

                /// Shows date components in their long form if possible. For example, "October 21, 2015".
                val long: Date.FormatStyle.DateStyle = DateStyle(rawValue = 3)

                /// Shows the complete day. For example, "Wednesday, October 21, 2015".
                val complete: Date.FormatStyle.DateStyle = DateStyle(rawValue = 4)
            }
        }

        /// Predefined time styles varied in lengths or the components included. The exact format depends on the locale.
        class TimeStyle {
            internal val rawValue: Int

            internal constructor(rawValue: Int) {
                this.rawValue = rawValue
            }

            override fun equals(other: Any?): Boolean {
                if (other !is Date.FormatStyle.TimeStyle) return false
                return rawValue == other.rawValue
            }

            override fun hashCode(): Int {
                var result = 1
                result = Hasher.combine(result, rawValue)
                return result
            }

            @androidx.annotation.Keep
            companion object {

                /// Excludes the time part.
                val omitted: Date.FormatStyle.TimeStyle = TimeStyle(rawValue = 0)

                /// For example, `04:29 PM`, `16:29`.
                val shortened: Date.FormatStyle.TimeStyle = TimeStyle(rawValue = 1)

                /// For example, `4:29:24 PM`, `16:29:24`.
                val standard: Date.FormatStyle.TimeStyle = TimeStyle(rawValue = 2)

                /// For example, `4:29:24 PM PDT`, `16:29:24 GMT`.
                val complete: Date.FormatStyle.TimeStyle = TimeStyle(rawValue = 3)
            }
        }

        constructor(locale: Locale, timeZone: TimeZone, calendar: Calendar) {
            this.locale = locale
            this.timeZone = timeZone
            this.calendar = calendar
        }

        override var supdate: ((Any) -> Unit)? = null
        override var smutatingcount = 0
        override fun scopy(): MutableStruct = Date.FormatStyle(locale, timeZone, calendar)

        @androidx.annotation.Keep
        companion object {
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as Date
        this.platformValue = copy.platformValue
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = Date(this as MutableStruct)

    private fun assignfrom(target: Date) {
        this.platformValue = target.platformValue
    }

    override fun toString(): String = description

    @androidx.annotation.Keep
    companion object: DecodableCompanion<Date> {

        val timeIntervalBetween1970AndReferenceDate: Double = 978307200.0

        val timeIntervalSinceReferenceDate: Double
            get() = (System.currentTimeMillis().toDouble() / 1000.0) - timeIntervalBetween1970AndReferenceDate

        val distantPast = Date(timeIntervalSince1970 = -62135769600.0)
        val distantFuture = Date(timeIntervalSince1970 = 64092211200.0)

        val now: Date
            get() = Date()

        override fun init(from: Decoder): Date = Date(from = from)
    }
}

