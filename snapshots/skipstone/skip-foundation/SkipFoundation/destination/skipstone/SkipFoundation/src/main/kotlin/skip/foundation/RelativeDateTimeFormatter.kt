package skip.foundation

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

import skip.foundation.*

import android.icu.text.RelativeDateTimeFormatter.AbsoluteUnit
import android.icu.text.RelativeDateTimeFormatter.Direction
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit

open class RelativeDateTimeFormatter: Formatter {
    internal open var platformValue: android.icu.text.RelativeDateTimeFormatter
        get() = platformValuestorage.sref({ this.platformValue = it })
        set(newValue) {
            platformValuestorage = newValue.sref()
        }
    private lateinit var platformValuestorage: android.icu.text.RelativeDateTimeFormatter

    open var dateTimeStyle: RelativeDateTimeFormatter.DateTimeStyle = RelativeDateTimeFormatter.DateTimeStyle.numeric

    open var unitsStyle: RelativeDateTimeFormatter.UnitsStyle = RelativeDateTimeFormatter.UnitsStyle.full
        set(newValue) {
            field = newValue
            if (!suppresssideeffects) {
                updatePlatformValue()
            }
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open var calendar: Calendar
        get() = calendarstorage.sref()
        set(newValue) {
            calendarstorage = newValue.sref()
        }
    private lateinit var calendarstorage: Calendar

    open var locale: Locale
        get() = localestorage
        set(newValue) {
            localestorage = newValue
            if (!suppresssideeffects) {
                updatePlatformValue()
            }
        }
    private lateinit var localestorage: Locale

    override var formattingContext: Formatter.Context
        get() = super.formattingContext
        set(newValue) {
            super.formattingContext = newValue
            updatePlatformValue()
        }

    constructor(): super() {
        suppresssideeffects = true
        try {
            locale = Locale.current
            updatePlatformValue() // didSet is not called from init(), so do it manually to set the platformValue
        } finally {
            suppresssideeffects = false
        }
    }

    private fun updatePlatformValue() {
        val ulocale = if (locale != null) android.icu.util.ULocale.forLocale(locale!!.platformValue) else android.icu.util.ULocale.getDefault()
        platformValue = android.icu.text.RelativeDateTimeFormatter.getInstance(ulocale, null, relativeDateTimeFormatterStyle, formattingContext.capitalization)
    }

    open fun localizedString(from: DateComponents): String {
        val dateComponents = from
        var relativeUnit: RelativeUnit? = null
        var absoluteUnit: AbsoluteUnit? = null
        // Find the set date component, prioritizing non-zero
        var value = 0
        dateComponents.year?.let { year ->
            value = year
            if (abs(value) == 1 && dateTimeStyle == RelativeDateTimeFormatter.DateTimeStyle.named) {
                absoluteUnit = AbsoluteUnit.YEAR.sref()
            } else {
                relativeUnit = RelativeUnit.YEARS.sref()
            }
        }
        if (value == 0) {
            dateComponents.month?.let { month ->
                value = month
                if (abs(value) == 1 && dateTimeStyle == RelativeDateTimeFormatter.DateTimeStyle.named) {
                    absoluteUnit = AbsoluteUnit.MONTH.sref()
                } else {
                    relativeUnit = RelativeUnit.MONTHS.sref()
                }
            }
        }
        if (value == 0) {
            dateComponents.weekOfMonth?.let { week ->
                value = week
                if (abs(value) == 1 && dateTimeStyle == RelativeDateTimeFormatter.DateTimeStyle.named) {
                    absoluteUnit = AbsoluteUnit.WEEK.sref()
                } else {
                    relativeUnit = RelativeUnit.WEEKS.sref()
                }
            }
        }
        if (value == 0) {
            dateComponents.day?.let { day ->
                value = day
                if ((value == 0 || abs(value) == 1) && dateTimeStyle == RelativeDateTimeFormatter.DateTimeStyle.named) {
                    absoluteUnit = AbsoluteUnit.DAY.sref()
                } else {
                    relativeUnit = RelativeUnit.DAYS.sref()
                }
            }
        }
        if (value == 0) {
            dateComponents.hour?.let { hour ->
                value = hour
                relativeUnit = RelativeUnit.HOURS.sref()
            }
        }
        if (value == 0) {
            dateComponents.minute?.let { minute ->
                value = minute
                relativeUnit = RelativeUnit.MINUTES.sref()
            }
        }
        if (value == 0) {
            dateComponents.second?.let { second ->
                value = second
                if (value == 0 && dateTimeStyle == RelativeDateTimeFormatter.DateTimeStyle.named) {
                    return platformValue.format(Direction.PLAIN, AbsoluteUnit.NOW)
                }
                relativeUnit = RelativeUnit.SECONDS.sref()
            }
        }
        val direction = (if (value == -0 || value <= 0) Direction.LAST else Direction.NEXT).sref()
        if (absoluteUnit != null) {
            return platformValue.format(if (value == 0) Direction.THIS else direction, absoluteUnit)
        }
        if (value == 0 && relativeUnit == null) {
            return ""
        }
        val timeValue = Double(abs(value))
        return platformValue.format(timeValue, direction, relativeUnit!!)
    }

    open fun localizedString(fromTimeInterval: Double): String {
        val timeInterval = fromTimeInterval
        val isNegative = timeInterval < 0.0
        val direction = (if (isNegative) Direction.LAST else Direction.NEXT).sref()
        var relativeUnit: RelativeUnit? = null
        var absoluteUnit: AbsoluteUnit? = null
        var timeValue = abs(timeInterval)
        if (timeValue < 60.0) {
            relativeUnit = RelativeUnit.SECONDS.sref()
            if (timeValue < 1.0 && dateTimeStyle == RelativeDateTimeFormatter.DateTimeStyle.named) {
                return platformValue.format(Direction.PLAIN, AbsoluteUnit.NOW)
            }
        } else if (timeValue < 60.0 * 60.0) {
            timeValue /= 60.0
            relativeUnit = RelativeUnit.MINUTES.sref()
        } else if (timeValue < 60.0 * 60.0 * 24.0) {
            timeValue /= 60.0 * 60.0
            relativeUnit = RelativeUnit.HOURS.sref()
        } else if (timeValue < 60.0 * 60.0 * 24.0 * 7.0) {
            timeValue /= 60.0 * 60.0 * 24.0
            if (timeValue < 2.0 && dateTimeStyle == RelativeDateTimeFormatter.DateTimeStyle.named) {
                absoluteUnit = AbsoluteUnit.DAY.sref()
            } else {
                relativeUnit = RelativeUnit.DAYS.sref()
            }
        } else if (timeValue < 60.0 * 60.0 * 24.0 * 31.0) {
            timeValue /= 60.0 * 60.0 * 24.0 * 7
            if (timeValue < 2.0 && dateTimeStyle == RelativeDateTimeFormatter.DateTimeStyle.named) {
                absoluteUnit = AbsoluteUnit.WEEK.sref()
            } else {
                relativeUnit = RelativeUnit.WEEKS.sref()
            }
        } else if (timeValue < 60.0 * 60.0 * 24.0 * (if (isNegative) 366.0 else 365.0)) {
            timeValue /= 60.0 * 60.0 * 24.0 * 30.4375
            if (timeValue < 2.0 && dateTimeStyle == RelativeDateTimeFormatter.DateTimeStyle.named) {
                absoluteUnit = AbsoluteUnit.MONTH.sref()
            } else {
                relativeUnit = RelativeUnit.MONTHS.sref()
            }
        } else {
            timeValue /= 60.0 * 60.0 * 24.0 * 365.25
            if (timeValue < 2.0 && dateTimeStyle == RelativeDateTimeFormatter.DateTimeStyle.named) {
                absoluteUnit = AbsoluteUnit.YEAR.sref()
            } else {
                relativeUnit = RelativeUnit.YEARS.sref()
            }
        }
        if (absoluteUnit != null) {
            return platformValue.format(direction, absoluteUnit)
        }
        timeValue = if (relativeUnit!! != RelativeUnit.SECONDS && timeValue < 1.0) 1.0 else timeValue.rounded(FloatingPointRoundingRule.down)
        return platformValue.format(timeValue, direction, relativeUnit!!)
    }

    open fun localizedString(for_: Date, relativeTo: Date): String {
        val date = for_
        val referenceDate = relativeTo
        val timeInterval = date.timeIntervalSince(referenceDate)
        return localizedString(fromTimeInterval = timeInterval)
    }

    override fun string(for_: Any?): String? {
        val obj = for_
        val date_0 = (obj as? Date).sref()
        if (date_0 == null) {
            return null
        }
        return localizedString(for_ = date_0, relativeTo = Date.now)
    }

    private val relativeDateTimeFormatterStyle: android.icu.text.RelativeDateTimeFormatter.Style
        get() {
            // NARROW and SHORT both behave like `UnitsStyle.Abbreviated` as of Android 15
            // https://developer.android.com/reference/android/icu/text/RelativeDateTimeFormatter.Style#NARROW
            return when (unitsStyle) {
                RelativeDateTimeFormatter.UnitsStyle.short -> android.icu.text.RelativeDateTimeFormatter.Style.SHORT
                else -> android.icu.text.RelativeDateTimeFormatter.Style.LONG
            }
        }

    enum class DateTimeStyle(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
        numeric(0),
        named(1);

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: Int): RelativeDateTimeFormatter.DateTimeStyle? {
                return when (rawValue) {
                    0 -> DateTimeStyle.numeric
                    1 -> DateTimeStyle.named
                    else -> null
                }
            }
        }
    }

    enum class UnitsStyle(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
        full(0),
        spellOut(1),
        short(2),
        abbreviated(3);

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: Int): RelativeDateTimeFormatter.UnitsStyle? {
                return when (rawValue) {
                    0 -> UnitsStyle.full
                    1 -> UnitsStyle.spellOut
                    2 -> UnitsStyle.short
                    3 -> UnitsStyle.abbreviated
                    else -> null
                }
            }
        }
    }

    private var suppresssideeffects = false

    @androidx.annotation.Keep
    companion object: CompanionClass() {

        override fun DateTimeStyle(rawValue: Int): RelativeDateTimeFormatter.DateTimeStyle? = DateTimeStyle.init(rawValue = rawValue)

        override fun UnitsStyle(rawValue: Int): RelativeDateTimeFormatter.UnitsStyle? = UnitsStyle.init(rawValue = rawValue)
    }
    open class CompanionClass: Formatter.CompanionClass() {
        open fun DateTimeStyle(rawValue: Int): RelativeDateTimeFormatter.DateTimeStyle? = RelativeDateTimeFormatter.DateTimeStyle(rawValue = rawValue)
        open fun UnitsStyle(rawValue: Int): RelativeDateTimeFormatter.UnitsStyle? = RelativeDateTimeFormatter.UnitsStyle(rawValue = rawValue)
    }
}

