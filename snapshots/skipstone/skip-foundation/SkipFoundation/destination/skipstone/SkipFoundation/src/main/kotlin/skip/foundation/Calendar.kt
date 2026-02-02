package skip.foundation

import skip.lib.*
import skip.lib.Array
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

// Needed to expose `clone`:
fun java.util.Calendar.clone(): java.util.Calendar { return this.clone() as java.util.Calendar }

@androidx.annotation.Keep
@Suppress("MUST_BE_INITIALIZED")
class Calendar: Codable, KotlinConverting<java.util.Calendar>, MutableStruct {
    internal var platformValue: java.util.Calendar
        get() = field.sref({ this.platformValue = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    constructor(platformValue: java.util.Calendar) {
        this.platformValue = platformValue
        this.locale = Locale.current
    }

    constructor(identifier: Calendar.Identifier) {
        this.platformValue = Companion.platformValue(for_ = identifier)
        this.locale = Locale.current
    }

    constructor(from: Decoder) {
        val decoder = from
        val container = decoder.singleValueContainer()
        val identifier = container.decode(Calendar.Identifier::class)
        this.platformValue = Companion.platformValue(for_ = identifier)
        this.locale = Locale.current
    }

    override fun encode(to: Encoder) {
        val encoder = to
        var container = encoder.singleValueContainer()
        container.encode(identifier)
    }

    var locale: Locale
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    var timeZone: TimeZone
        get() = TimeZone(platformValue.getTimeZone()).sref({ this.timeZone = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            platformValue.setTimeZone(newValue.platformValue)
        }

    val description: String
        get() = platformValue.description

    val identifier: Calendar.Identifier
        get() {
            // TODO: non-gregorian calendar
            if (gregorianCalendar != null) {
                return Calendar.Identifier.gregorian
            } else {
                return Calendar.Identifier.iso8601
            }
        }

    internal fun toDate(): Date = Date(platformValue = platformValue.getTime())

    private val dateFormatSymbols: java.text.DateFormatSymbols
        get() = java.text.DateFormatSymbols.getInstance(locale.platformValue)

    private val gregorianCalendar: java.util.GregorianCalendar?
        get() = platformValue as? java.util.GregorianCalendar

    var firstWeekday: Int
        get() = platformValue.getFirstDayOfWeek()
        set(newValue) {
            platformValue.setFirstDayOfWeek(newValue)
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val minimumDaysInFirstWeek: Int
        get() {
            fatalError()
        }

    val eraSymbols: Array<String>
        get() = Array(dateFormatSymbols.getEras().toList())

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val longEraSymbols: Array<String>
        get() {
            fatalError()
        }

    val monthSymbols: Array<String>
        get() {
            // The java.text.DateFormatSymbols.getInstance().getMonths() method in Java returns an array of 13 symbols because it includes both the 12 months of the year and an additional symbol
            // some documentation says the blank symbol is at index 0, but other tests show it at the end, so just pare it out
            return Array(dateFormatSymbols.getMonths().toList()).filter({ it ->
                it?.isEmpty == false
            })
        }

    val shortMonthSymbols: Array<String>
        get() {
            return Array(dateFormatSymbols.getShortMonths().toList()).filter({ it ->
                it?.isEmpty == false
            })
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val veryShortMonthSymbols: Array<String>
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val standaloneMonthSymbols: Array<String>
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val shortStandaloneMonthSymbols: Array<String>
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val veryShortStandaloneMonthSymbols: Array<String>
        get() {
            fatalError()
        }

    val weekdaySymbols: Array<String>
        get() {
            return Array(dateFormatSymbols.getWeekdays().toList()).filter({ it ->
                it?.isEmpty == false
            })
        }

    val shortWeekdaySymbols: Array<String>
        get() {
            return Array(dateFormatSymbols.getShortWeekdays().toList()).filter({ it ->
                it?.isEmpty == false
            })
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val veryShortWeekdaySymbols: Array<String>
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val standaloneWeekdaySymbols: Array<String>
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val shortStandaloneWeekdaySymbols: Array<String>
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val veryShortStandaloneWeekdaySymbols: Array<String>
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val quarterSymbols: Array<String>
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val shortQuarterSymbols: Array<String>
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val standaloneQuarterSymbols: Array<String>
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val shortStandaloneQuarterSymbols: Array<String>
        get() {
            fatalError()
        }

    val amSymbol: String
        get() = dateFormatSymbols.getAmPmStrings()[0]

    val pmSymbol: String
        get() = dateFormatSymbols.getAmPmStrings()[1]

    fun minimumRange(of: Calendar.Component): IntRange? {
        val component = of
        val platformCal = (platformValue.clone() as java.util.Calendar).sref()

        when (component) {
            Calendar.Component.year -> {
                // Year typically starts at 1 and has no defined maximum.
                return 1 until platformCal.getMaximum(java.util.Calendar.YEAR)
            }
            Calendar.Component.month -> {
                // Java's month is 0-based (0-11), but Swift expects 1-based (1-12).
                return 1 until (platformCal.getMaximum(java.util.Calendar.MONTH) + 2)
            }
            Calendar.Component.day -> {
                // getMaximum() gives the largest value that field could theoretically have.
                // getActualMaximum() gives the largest value that field actually has for the specific calendar state.

                // calendar.getActualMaximum(java.util.Calendar.DATE)
                // will return 28 because February 2023 has 28 days (it’s not a leap year).
                platformCal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                clearTime(in_ = platformCal)
                platformCal.set(java.util.Calendar.MONTH, java.util.Calendar.FEBRUARY)
                platformCal.set(java.util.Calendar.YEAR, 2023)
                // Minimum days in a month is 1, maximum can vary (28 for February).
                return platformCal.getMinimum(java.util.Calendar.DATE) until platformCal.getActualMaximum(java.util.Calendar.DATE) + 1
            }
            Calendar.Component.hour -> {
                // Hours are in the range 0-23.
                return platformCal.getMinimum(java.util.Calendar.HOUR_OF_DAY) until (platformCal.getMaximum(java.util.Calendar.HOUR_OF_DAY) + 1)
            }
            Calendar.Component.minute -> {
                // Minutes are in the range 0-59.
                return platformCal.getMinimum(java.util.Calendar.MINUTE) until (platformCal.getMaximum(java.util.Calendar.MINUTE) + 1)
            }
            Calendar.Component.second -> {
                // Seconds are in the range 0-59.
                return platformCal.getMinimum(java.util.Calendar.SECOND) until (platformCal.getMaximum(java.util.Calendar.SECOND) + 1)
            }
            Calendar.Component.weekday -> {
                // Weekday ranges from 1 (Sunday) to 7 (Saturday).
                return platformCal.getMinimum(java.util.Calendar.DAY_OF_WEEK) until (platformCal.getMaximum(java.util.Calendar.DAY_OF_WEEK) + 1)
            }
            Calendar.Component.weekOfMonth, Calendar.Component.weekOfYear -> {
                // Not supported yet...
                fatalError()
            }
            Calendar.Component.quarter -> {
                // There are always 4 quarters in a year.
                return 1 until 5
            }
            else -> return null
        }
    }

    fun maximumRange(of: Calendar.Component): IntRange? {
        val component = of
        val platformCal = (platformValue.clone() as java.util.Calendar).sref()
        when (component) {
            Calendar.Component.day -> {
                // Maximum number of days in a month can vary (e.g., 28, 29, 30, or 31 days)
                return platformCal.getMinimum(java.util.Calendar.DATE) until (platformCal.getMaximum(java.util.Calendar.DATE) + 1)
            }
            Calendar.Component.weekOfMonth, Calendar.Component.weekOfYear -> {
                // Not supported yet...
                fatalError()
            }
            else -> {
                // Maximum range is usually the same logic as minimum but could differ in some cases.
                return minimumRange(of = component)
            }
        }
    }


    fun range(of: Calendar.Component, in_: Calendar.Component, for_: Date): IntRange? {
        val smaller = of
        val larger = in_
        val date = for_
        val platformCal = (platformValue.clone() as java.util.Calendar).sref()
        platformCal.time = date.platformValue

        when (larger) {
            Calendar.Component.month -> {
                if (smaller == Calendar.Component.day) {
                    // Range of days in the current month
                    val numDays = platformCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                    return 1 until (numDays + 1)
                } else if (smaller == Calendar.Component.weekOfMonth) {
                    // Range of weeks in the current month
                    val numWeeks = platformCal.getActualMaximum(java.util.Calendar.WEEK_OF_MONTH)
                    return 1 until (numWeeks + 1)
                }
            }
            Calendar.Component.year -> {
                if (smaller == Calendar.Component.weekOfYear) {
                    // Range of weeks in the current year
                    // Seems like Swift always returns Maximum not for an actual date
                    val numWeeks = platformCal.getMaximum(java.util.Calendar.WEEK_OF_YEAR)
                    return 1 until (numWeeks + 1)
                } else if (smaller == Calendar.Component.day) {
                    // Range of days in the current year
                    val numDays = platformCal.getActualMaximum(java.util.Calendar.DAY_OF_YEAR)
                    return 1 until (numDays + 1)
                } else if (smaller == Calendar.Component.month) {
                    // Range of months in the current year (1 to 12)
                    return 1 until 13
                }
            }
            else -> return null
        }

        return null
    }

    private fun clearTime(in_: java.util.Calendar) {
        val calendar = in_
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0) // “The HOUR_OF_DAY, HOUR and AM_PM fields are handled independently and the the resolution rule for the time of day is applied. Clearing one of the fields doesn't reset the hour of day value of this Calendar. Use set(Calendar.HOUR_OF_DAY, 0) to reset the hour value.”
        calendar.clear(java.util.Calendar.HOUR_OF_DAY)
        calendar.clear(java.util.Calendar.MINUTE)
        calendar.clear(java.util.Calendar.SECOND)
        calendar.clear(java.util.Calendar.MILLISECOND)
    }

    fun dateInterval(of: Calendar.Component, start: InOut<Date>, interval: InOut<Double>, for_: Date): Boolean {
        val component = of
        val date = for_
        val platformCal = (platformValue.clone() as java.util.Calendar).sref()
        platformCal.time = date.platformValue

        when (component) {
            Calendar.Component.day -> {
                clearTime(in_ = platformCal)
                start.value = Date(platformValue = platformCal.time)
                interval.value = TimeInterval(24 * 60 * 60)
                return true
            }
            Calendar.Component.month -> {
                platformCal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                clearTime(in_ = platformCal)
                start.value = Date(platformValue = platformCal.time)
                val numberOfDays = platformCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                interval.value = TimeInterval(numberOfDays) * TimeInterval(24 * 60 * 60)
                return true
            }
            Calendar.Component.weekOfMonth, Calendar.Component.weekOfYear -> {
                platformCal.set(java.util.Calendar.DAY_OF_WEEK, platformCal.firstDayOfWeek)
                clearTime(in_ = platformCal)
                start.value = Date(platformValue = platformCal.time)
                interval.value = TimeInterval(7 * 24 * 60 * 60)
                return true
            }
            Calendar.Component.quarter -> {
                val currentMonth = platformCal.get(java.util.Calendar.MONTH)
                val quarterStartMonth = (currentMonth / 3) * 3 // Find the first month of the current quarter
                platformCal.set(java.util.Calendar.MONTH, quarterStartMonth)
                platformCal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                clearTime(in_ = platformCal)
                start.value = Date(platformValue = platformCal.time)
                interval.value = TimeInterval(platformCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)) * TimeInterval(24 * 60 * 60 * 3)
                return true
            }
            else -> return false
        }
    }

    fun dateInterval(of: Calendar.Component, for_: Date): DateInterval? {
        val component = of
        val date = for_
        var start = Date()
        var interval: Double = 0.0
        if (dateInterval(of = component, start = InOut({ start }, { start = it }), interval = InOut({ interval }, { interval = it }), for_ = date)) {
            return DateInterval(start = start, duration = interval)
        }
        return null
    }

    fun ordinality(of: Calendar.Component, in_: Calendar.Component, for_: Date): Int? {
        val smaller = of
        val larger = in_
        val date = for_
        val platformCal = (platformValue.clone() as java.util.Calendar).sref()
        platformCal.time = date.platformValue

        when (larger) {
            Calendar.Component.year -> {
                if (smaller == Calendar.Component.day) {
                    return platformCal.get(java.util.Calendar.DAY_OF_YEAR)
                } else if (smaller == Calendar.Component.weekOfYear) {
                    return platformCal.get(java.util.Calendar.WEEK_OF_YEAR)
                }
            }
            Calendar.Component.month -> {
                if (smaller == Calendar.Component.day) {
                    return platformCal.get(java.util.Calendar.DAY_OF_MONTH)
                } else if (smaller == Calendar.Component.weekOfMonth) {
                    return platformCal.get(java.util.Calendar.WEEK_OF_MONTH)
                }
            }
            else -> return null
        }
        return null
    }

    fun date(from: DateComponents): Date? {
        val components = from
        var localComponents = components.sref()
        localComponents.calendar = this
        return Date(platformValue = localComponents.createCalendarComponents(timeZone = this.timeZone).getTime())
    }

    fun dateComponents(in_: TimeZone? = null, from: Date): DateComponents {
        val zone = in_
        val date = from
        return DateComponents(fromCalendar = this, in_ = zone ?: this.timeZone, from = date)
    }

    fun dateComponents(components: Set<Calendar.Component>, from: Date, to: Date): DateComponents {
        val start = from
        val end = to
        return DateComponents(fromCalendar = this, in_ = this.timeZone, from = start, to = end)
    }

    fun dateComponents(components: Set<Calendar.Component>, from: Date): DateComponents {
        val date = from
        return DateComponents(fromCalendar = this, in_ = this.timeZone, from = date, with = components)
    }

    fun date(byAdding: DateComponents, to: Date, wrappingComponents: Boolean = false): Date? {
        val components = byAdding
        val date = to
        var comps = DateComponents(fromCalendar = this, in_ = this.timeZone, from = date)
        if (!wrappingComponents) {
            comps.add(components)
        } else {
            comps.roll(components)
        }
        return date(from = comps)
    }

    fun date(byAdding: Calendar.Component, value: Int, to: Date, wrappingComponents: Boolean = false): Date? {
        val component = byAdding
        val date = to
        var comps = DateComponents(fromCalendar = this, in_ = this.timeZone, from = date)
        if (!wrappingComponents) {
            comps.addValue(value, for_ = component)
        } else {
            comps.rollValue(value, for_ = component)
        }
        return date(from = comps)
    }

    fun component(component: Calendar.Component, from: Date): Int {
        val date = from
        return dateComponents(setOf(component), from = date).value(for_ = component) ?: 0
    }

    fun startOfDay(for_: Date): Date {
        val date = for_
        // Clone the calendar to avoid mutating the original
        val platformCal = (platformValue.clone() as java.util.Calendar).sref()
        platformCal.time = date.platformValue

        // Set the time components to the start of the day
        clearTime(in_ = platformCal)

        // Return the new Date representing the start of the day
        return Date(platformValue = platformCal.time)
    }

    fun compare(date1: Date, to: Date, toGranularity: Calendar.Component): ComparisonResult {
        val date2 = to
        val component = toGranularity
        val platformCal1 = (platformValue.clone() as java.util.Calendar).sref()
        val platformCal2 = (platformValue.clone() as java.util.Calendar).sref()

        platformCal1.time = date1.platformValue
        platformCal2.time = date2.platformValue

        when (component) {
            Calendar.Component.year -> {
                val year1 = platformCal1.get(java.util.Calendar.YEAR)
                val year2 = platformCal2.get(java.util.Calendar.YEAR)
                return if (year1 < year2) ComparisonResult.orderedAscending else if (year1 > year2) ComparisonResult.orderedDescending else ComparisonResult.orderedSame
            }
            Calendar.Component.month -> {
                val year1 = platformCal1.get(java.util.Calendar.YEAR)
                val year2 = platformCal2.get(java.util.Calendar.YEAR)
                val month1 = platformCal1.get(java.util.Calendar.MONTH)
                val month2 = platformCal2.get(java.util.Calendar.MONTH)
                if (year1 != year2) {
                    return if (year1 < year2) ComparisonResult.orderedAscending else ComparisonResult.orderedDescending
                }
                return if (month1 < month2) ComparisonResult.orderedAscending else if (month1 > month2) ComparisonResult.orderedDescending else ComparisonResult.orderedSame
            }
            Calendar.Component.day -> {
                val year1 = platformCal1.get(java.util.Calendar.YEAR)
                val year2 = platformCal2.get(java.util.Calendar.YEAR)
                val day1 = platformCal1.get(java.util.Calendar.DAY_OF_YEAR)
                val day2 = platformCal2.get(java.util.Calendar.DAY_OF_YEAR)
                if (year1 != year2) {
                    return if (year1 < year2) ComparisonResult.orderedAscending else ComparisonResult.orderedDescending
                }
                return if (day1 < day2) ComparisonResult.orderedAscending else if (day1 > day2) ComparisonResult.orderedDescending else ComparisonResult.orderedSame
            }
            else -> return ComparisonResult.orderedSame
        }
    }

    fun isDate(date1: Date, equalTo: Date, toGranularity: Calendar.Component): Boolean {
        val date2 = equalTo
        val component = toGranularity
        return compare(date1, to = date2, toGranularity = component) == ComparisonResult.orderedSame
    }

    fun isDate(date1: Date, inSameDayAs: Date): Boolean {
        val date2 = inSameDayAs
        return isDate(date1, equalTo = date2, toGranularity = Calendar.Component.day)
    }

    fun isDateInToday(date: Date): Boolean {
        val platformCal = (platformValue.clone() as java.util.Calendar).sref()
        platformCal.time = Date().platformValue

        val targetCal = (platformValue.clone() as java.util.Calendar).sref()
        targetCal.time = date.platformValue

        return platformCal.get(java.util.Calendar.YEAR) == targetCal.get(java.util.Calendar.YEAR) && platformCal.get(java.util.Calendar.DAY_OF_YEAR) == targetCal.get(java.util.Calendar.DAY_OF_YEAR)
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun isDateInYesterday(date: Date): Boolean {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun isDateInTomorrow(date: Date): Boolean {
        fatalError()
    }

    fun isDateInWeekend(date: Date): Boolean {
        val components = dateComponents(from = date)
        return components.weekday == java.util.Calendar.SATURDAY || components.weekday == java.util.Calendar.SUNDAY
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dateIntervalOfWeekend(containing: Date, start: InOut<Date>, interval: InOut<Double>): Boolean {
        val date = containing
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dateIntervalOfWeekend(containing: Date): DateInterval? {
        val date = containing
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun nextWeekend(startingAfter: Date, start: InOut<Date>, interval: InOut<Double>, direction: Calendar.SearchDirection = Calendar.SearchDirection.forward): Boolean {
        val date = startingAfter
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun nextWeekend(startingAfter: Date, direction: Calendar.SearchDirection = Calendar.SearchDirection.forward): DateInterval? {
        val date = startingAfter
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun enumerateDates(startingAfter: Date, matching: DateComponents, matchingPolicy: Calendar.MatchingPolicy, repeatedTimePolicy: Calendar.RepeatedTimePolicy = Calendar.RepeatedTimePolicy.first, direction: Calendar.SearchDirection = Calendar.SearchDirection.forward, using: (Date?, Boolean, InOut<Boolean>) -> Unit) {
        val start = startingAfter
        val components = matching
        val block = using
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun nextDate(after: Date, matching: DateComponents, matchingPolicy: Calendar.MatchingPolicy, repeatedTimePolicy: Calendar.RepeatedTimePolicy = Calendar.RepeatedTimePolicy.first, direction: Calendar.SearchDirection = Calendar.SearchDirection.forward): Date? {
        val date = after
        val components = matching
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun date(bySetting: Calendar.Component, value: Int, of: Date): Date? {
        val component = bySetting
        val date = of
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun date(bySettingHour: Int, minute: Int, second: Int, of: Date, matchingPolicy: Calendar.MatchingPolicy = Calendar.MatchingPolicy.nextTime, repeatedTimePolicy: Calendar.RepeatedTimePolicy = Calendar.RepeatedTimePolicy.first, direction: Calendar.SearchDirection = Calendar.SearchDirection.forward): Date? {
        val hour = bySettingHour
        val date = of
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun date(date: Date, matchesComponents: DateComponents): Boolean {
        val components = matchesComponents
        fatalError()
    }

    enum class Component {
        era,
        year,
        month,
        day,
        hour,
        minute,
        second,
        weekday,
        weekdayOrdinal,
        quarter,
        weekOfMonth,
        weekOfYear,
        yearForWeekOfYear,
        nanosecond,
        calendar,
        timeZone;

        @androidx.annotation.Keep
        companion object {
        }
    }

    /// Calendar supports many different kinds of calendars. Each is identified by an identifier here.
    @androidx.annotation.Keep
    enum class Identifier(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): Codable, RawRepresentable<Int> {
        /// The common calendar in Europe, the Western Hemisphere, and elsewhere.
        gregorian(0),
        buddhist(1),
        chinese(2),
        coptic(3),
        ethiopicAmeteMihret(4),
        ethiopicAmeteAlem(5),
        hebrew(6),
        iso8601(7),
        indian(8),
        islamic(9),
        islamicCivil(10),
        japanese(11),
        persian(12),
        republicOfChina(13),
        islamicTabular(14),
        islamicUmmAlQura(15);

        override fun encode(to: Encoder) {
            val container = to.singleValueContainer()
            container.encode(rawValue)
        }

        @androidx.annotation.Keep
        companion object: DecodableCompanion<Calendar.Identifier> {
            override fun init(from: Decoder): Calendar.Identifier = Identifier(from = from)

            fun init(rawValue: Int): Calendar.Identifier? {
                return when (rawValue) {
                    0 -> Identifier.gregorian
                    1 -> Identifier.buddhist
                    2 -> Identifier.chinese
                    3 -> Identifier.coptic
                    4 -> Identifier.ethiopicAmeteMihret
                    5 -> Identifier.ethiopicAmeteAlem
                    6 -> Identifier.hebrew
                    7 -> Identifier.iso8601
                    8 -> Identifier.indian
                    9 -> Identifier.islamic
                    10 -> Identifier.islamicCivil
                    11 -> Identifier.japanese
                    12 -> Identifier.persian
                    13 -> Identifier.republicOfChina
                    14 -> Identifier.islamicTabular
                    15 -> Identifier.islamicUmmAlQura
                    else -> null
                }
            }
        }
    }

    enum class SearchDirection {
        forward,
        backward;

        @androidx.annotation.Keep
        companion object {
        }
    }

    enum class RepeatedTimePolicy {
        first,
        last;

        @androidx.annotation.Keep
        companion object {
        }
    }

    enum class MatchingPolicy {
        nextTime,
        nextTimePreservingSmallerComponents,
        previousTimePreservingSmallerComponents,
        strict;

        @androidx.annotation.Keep
        companion object {
        }
    }

    override fun kotlin(nocopy: Boolean): java.util.Calendar = (if (nocopy) platformValue else platformValue.clone() as java.util.Calendar).sref()

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as Calendar
        this.platformValue = copy.platformValue
        this.locale = copy.locale
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = Calendar(this as MutableStruct)

    override fun toString(): String = description

    override fun equals(other: Any?): Boolean {
        if (other !is Calendar) return false
        return platformValue == other.platformValue && locale == other.locale
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, platformValue)
        result = Hasher.combine(result, locale)
        return result
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<Calendar> {

        val current: Calendar
            get() = Calendar(platformValue = java.util.Calendar.getInstance())

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val autoupdatingCurrent: Calendar
            get() {
                fatalError()
            }

        private fun platformValue(for_: Calendar.Identifier): java.util.Calendar {
            val identifier = for_
            when (identifier) {
                Calendar.Identifier.gregorian -> return java.util.GregorianCalendar()
                Calendar.Identifier.iso8601 -> return java.util.Calendar.getInstance()
                else -> {
                    // TODO: how to support the other calendars?
                    return java.util.Calendar.getInstance()
                }
            }
        }

        override fun init(from: Decoder): Calendar = Calendar(from = from)

        fun Identifier(from: Decoder): Calendar.Identifier {
            val container = from.singleValueContainer()
            val rawValue = container.decode(Int::class)
            return Identifier(rawValue = rawValue) ?: throw ErrorException(cause = NullPointerException())
        }

        fun Identifier(rawValue: Int): Calendar.Identifier? = Identifier.init(rawValue = rawValue)
    }
}

