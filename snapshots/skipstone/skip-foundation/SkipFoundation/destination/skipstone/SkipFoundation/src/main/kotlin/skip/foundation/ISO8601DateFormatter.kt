package skip.foundation

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAccessor

@Suppress("MUST_BE_INITIALIZED")
open class ISO8601DateFormatter: DateFormatter {
    private var dateParser: DateTimeFormatter
        get() = field.sref({ this.dateParser = it })
        set(newValue) {
            field = newValue.sref()
        }
    private var dateFormatter: DateTimeFormatter
        get() = field.sref({ this.dateFormatter = it })
        set(newValue) {
            field = newValue.sref()
        }

    constructor(): super() {
        suppresssideeffects = true
        try {
            this.formatOptions = ISO8601DateFormatter.Options.withInternetDateTime
            this.timeZone = (try { TimeZone(identifier = "UTC") } catch (_: NullReturnException) { null })
            this.dateParser = buildDateFormatter(parse = true)
            this.dateFormatter = buildDateFormatter(parse = false)
        } finally {
            suppresssideeffects = false
        }
    }

    open var formatOptions: ISO8601DateFormatter.Options = Options(rawValue = 0U)
        get() = field.sref({ this.formatOptions = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            field = newValue
            if (!suppresssideeffects) {
                // re-build the parser and formatter whenever the formatting options change
                this.dateParser = buildDateFormatter(parse = true)
                this.dateFormatter = buildDateFormatter(parse = false)
            }
        }

    private fun buildDateFormatter(parse: Boolean): DateTimeFormatter {
        var builder = DateTimeFormatterBuilder()

        val withInternetDateTime = formatOptions.contains(ISO8601DateFormatter.Options.withInternetDateTime)
        val withDay = formatOptions.contains(ISO8601DateFormatter.Options.withDay)
        val withMonth = formatOptions.contains(ISO8601DateFormatter.Options.withMonth)
        val withYear = formatOptions.contains(ISO8601DateFormatter.Options.withYear)
        val withWeekOfYear = formatOptions.contains(ISO8601DateFormatter.Options.withWeekOfYear)
        val withDate = withInternetDateTime || formatOptions.contains(ISO8601DateFormatter.Options.withFullDate)
        val hasDate = withDate || withDay || withMonth || withYear || withWeekOfYear
        if (hasDate) {
            var hadDateValue = false
            fun appendSeparator() {
                val withDash = formatOptions.contains(ISO8601DateFormatter.Options.withDashSeparatorInDate)
                if (hadDateValue && withDash) {
                    builder.appendLiteral("-")
                }
                hadDateValue = true
            }
            if (withDate || withYear) {
                builder.appendValue(ChronoField.YEAR, 4)
                hadDateValue = true
            }
            if (withDate || withMonth) {
                appendSeparator()
                builder.appendValue(ChronoField.MONTH_OF_YEAR, 2)
            }
            if (withWeekOfYear) {
                appendSeparator()
                builder.appendLiteral("W")
                builder.appendValue(ChronoField.ALIGNED_WEEK_OF_YEAR)
            }
            // The format for day is inferred based on provided options:
            // If withMonth is specified, dd is used.
            // If withWeekOfYear is specified, ee is used.
            if (withDate || withDay) {
                appendSeparator()
                if (withWeekOfYear) {
                    builder.appendValue(ChronoField.DAY_OF_WEEK, 2)
                } else if (withDate || withMonth) {
                    builder.appendValue(ChronoField.DAY_OF_MONTH, 2)
                } else {
                    builder.appendValue(ChronoField.DAY_OF_YEAR)
                }
            }
        }

        val withTime = formatOptions.contains(ISO8601DateFormatter.Options.withTime)
        val hasTime = withTime || withInternetDateTime || formatOptions.contains(ISO8601DateFormatter.Options.withFullTime)
        if (hasTime) {
            if (hasDate) {
                builder.appendLiteral(if (formatOptions.contains(ISO8601DateFormatter.Options.withSpaceBetweenDateAndTime)) " " else "T")
            }

            val withColon = formatOptions.contains(ISO8601DateFormatter.Options.withColonSeparatorInTime)
            builder.appendValue(ChronoField.HOUR_OF_DAY, 2)
            if (withColon) {
                builder.appendLiteral(":")
            }
            builder.appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            if (withColon) {
                builder.appendLiteral(":")
            }
            builder.appendValue(ChronoField.SECOND_OF_MINUTE, 2)

            if (formatOptions.contains(ISO8601DateFormatter.Options.withFractionalSeconds)) {
                // fractions are formatted to 3 digits, but are parsed at any length
                builder.appendFraction(ChronoField.NANO_OF_SECOND, 0, if (parse) 9 else 3, true)
            }
        }

        if (withInternetDateTime || formatOptions.contains(ISO8601DateFormatter.Options.withTimeZone)) {
            val withColon = formatOptions.contains(ISO8601DateFormatter.Options.withColonSeparatorInTimeZone)
            if (parse) {
                builder.appendPattern("[XXX][XX][X]")
            } else {
                //builder.appendPattern(withColon ? "XXX" : "XX") // same as below
                builder.appendOffset(if (withColon) "+HH:MM" else "+HHMM", "Z")
            }
        }

        return builder.toFormatter() //.withResolverStyle(ResolverStyle.LENIENT)
    }

    override fun date(from: String): Date? {
        val string = from
        // return nil for exceptions, like: java.time.format.DateTimeParseException: Text '2016-10-08T00:00:00+0600' could not be parsed, unparsed text found at index 22
        try {
            val matchtarget_0 = dateParser.parse(string)
            if (matchtarget_0 != null) {
                val accessor = matchtarget_0
                val matchtarget_1 = java.util.Date.from(Instant.from(accessor))
                if (matchtarget_1 != null) {
                    val date = matchtarget_1
                    return Date(platformValue = date)
                } else {
                    return null
                }
            } else {
                return null
            }
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            // Foundation expects failed date parses to return nil
            //print("failed to parse date: \(string): \(error)")
            return null
        }
    }

    override fun string(from: Date): String {
        val date = from
        return dateFormatter.format(date.platformValue.toInstant().atZone(timeZone?.platformValue?.toZoneId()))
    }

    override fun string(for_: Any?): String? {
        val obj = for_
        val date_0 = (obj as? Date).sref()
        if (date_0 == null) {
            return null
        }
        return string(from = date_0)
    }

    class Options: OptionSet<ISO8601DateFormatter.Options, UInt>, MutableStruct {
        override var rawValue: UInt
        constructor(rawValue: UInt) {
            this.rawValue = rawValue
        }

        override val rawvaluelong: ULong
            get() = ULong(rawValue)
        override fun makeoptionset(rawvaluelong: ULong): ISO8601DateFormatter.Options = Options(rawValue = UInt(rawvaluelong))
        override fun assignoptionset(target: ISO8601DateFormatter.Options) {
            willmutate()
            try {
                assignfrom(target)
            } finally {
                didmutate()
            }
        }

        private constructor(copy: MutableStruct) {
            @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as ISO8601DateFormatter.Options
            this.rawValue = copy.rawValue
        }

        override var supdate: ((Any) -> Unit)? = null
        override var smutatingcount = 0
        override fun scopy(): MutableStruct = ISO8601DateFormatter.Options(this as MutableStruct)

        private fun assignfrom(target: ISO8601DateFormatter.Options) {
            this.rawValue = target.rawValue
        }

        @androidx.annotation.Keep
        companion object {
            val withYear = ISO8601DateFormatter.Options(rawValue = 1U shl 0)
            val withMonth = ISO8601DateFormatter.Options(rawValue = 1U shl 1)
            val withWeekOfYear = ISO8601DateFormatter.Options(rawValue = 1U shl 2)
            val withDay = ISO8601DateFormatter.Options(rawValue = 1U shl 4)
            val withTime = ISO8601DateFormatter.Options(rawValue = 1U shl 5)
            val withTimeZone = ISO8601DateFormatter.Options(rawValue = 1U shl 6)
            val withSpaceBetweenDateAndTime = ISO8601DateFormatter.Options(rawValue = 1U shl 7)
            val withDashSeparatorInDate = ISO8601DateFormatter.Options(rawValue = 1U shl 8)
            val withColonSeparatorInTime = ISO8601DateFormatter.Options(rawValue = 1U shl 9)
            val withColonSeparatorInTimeZone = ISO8601DateFormatter.Options(rawValue = 1U shl 10)
            val withFractionalSeconds = ISO8601DateFormatter.Options(rawValue = 1U shl 11)
            val withFullDate = ISO8601DateFormatter.Options(rawValue = withYear.rawValue + withMonth.rawValue + withDay.rawValue + withDashSeparatorInDate.rawValue)
            val withFullTime = ISO8601DateFormatter.Options(rawValue = withTime.rawValue + withTimeZone.rawValue + withColonSeparatorInTime.rawValue + withColonSeparatorInTimeZone.rawValue)
            val withInternetDateTime = ISO8601DateFormatter.Options(rawValue = withFullDate.rawValue + withFullTime.rawValue)

            fun of(vararg options: ISO8601DateFormatter.Options): ISO8601DateFormatter.Options {
                val value = options.fold(UInt(0)) { result, option -> result or option.rawValue }
                return Options(rawValue = value)
            }
        }
    }

    private var suppresssideeffects = false

    @androidx.annotation.Keep
    companion object: CompanionClass() {

        override fun string(from: Date, timeZone: TimeZone, formatOptions: ISO8601DateFormatter.Options): String {
            val date = from
            val formatter = ISO8601DateFormatter()
            formatter.timeZone = timeZone
            formatter.formatOptions = formatOptions
            return formatter.string(from = date)
        }
    }
    open class CompanionClass: DateFormatter.CompanionClass() {
        open fun string(from: Date, timeZone: TimeZone, formatOptions: ISO8601DateFormatter.Options = ISO8601DateFormatter.Options.withInternetDateTime): String = ISO8601DateFormatter.string(from = from, timeZone = timeZone, formatOptions = formatOptions)
    }
}

