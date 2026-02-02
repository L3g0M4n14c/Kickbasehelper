package skip.ui

import skip.lib.*
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@androidx.annotation.Keep
class DatePicker: View, Renderable, skip.lib.SwiftProjecting {

    internal val selection: Binding<Date>
    internal val label: ComposeBuilder
    internal val dateFormatter: DateFormatter?
    internal val timeFormatter: DateFormatter?

    constructor(selection: Binding<Date>, displayedComponents: DatePickerComponents = DatePickerComponents.of(DatePickerComponents.hourAndMinute, DatePickerComponents.date), label: () -> View) {
        this.selection = selection.sref()
        this.label = ComposeBuilder.from(label)
        if (displayedComponents.contains(DatePickerComponents.date)) {
            dateFormatter = DateFormatter()
            dateFormatter?.dateStyle = DateFormatter.Style.medium
            dateFormatter?.timeStyle = DateFormatter.Style.none
        } else {
            dateFormatter = null
        }
        if (displayedComponents.contains(DatePickerComponents.hourAndMinute)) {
            timeFormatter = DateFormatter()
            timeFormatter?.dateStyle = DateFormatter.Style.none
            timeFormatter?.timeStyle = DateFormatter.Style.short
        } else {
            timeFormatter = null
        }
    }

    constructor(getSelection: () -> Date, setSelection: (Date) -> Unit, bridgedDisplayedComponents: Int, bridgedLabel: View): this(selection = Binding(get = getSelection, set = setSelection), displayedComponents = DatePickerComponents(rawValue = bridgedDisplayedComponents), label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            bridgedLabel.Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(selection: Binding<Date>, in_: IntRange, displayedComponents: DatePickerComponents = DatePickerComponents.of(DatePickerComponents.hourAndMinute, DatePickerComponents.date), label: () -> View) {
        val range = in_
        this.selection = selection.sref()
        this.dateFormatter = null
        this.timeFormatter = null
        this.label = ComposeBuilder.from(label)
    }

    constructor(titleKey: LocalizedStringKey, selection: Binding<Date>, displayedComponents: DatePickerComponents = DatePickerComponents.of(DatePickerComponents.hourAndMinute, DatePickerComponents.date)): this(selection = selection, displayedComponents = displayedComponents, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleKey).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleResource: LocalizedStringResource, selection: Binding<Date>, displayedComponents: DatePickerComponents = DatePickerComponents.of(DatePickerComponents.hourAndMinute, DatePickerComponents.date)): this(selection = selection, displayedComponents = displayedComponents, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleResource).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(titleKey: LocalizedStringKey, selection: Binding<Date>, in_: IntRange, displayedComponents: DatePickerComponents = DatePickerComponents.of(DatePickerComponents.hourAndMinute, DatePickerComponents.date)): this(selection = selection, displayedComponents = displayedComponents, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleKey).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(titleResource: LocalizedStringResource, selection: Binding<Date>, in_: IntRange, displayedComponents: DatePickerComponents = DatePickerComponents.of(DatePickerComponents.hourAndMinute, DatePickerComponents.date)): this(selection = selection, displayedComponents = displayedComponents, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleResource).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(title: String, selection: Binding<Date>, displayedComponents: DatePickerComponents = DatePickerComponents.of(DatePickerComponents.hourAndMinute, DatePickerComponents.date)): this(selection = selection, displayedComponents = displayedComponents, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(verbatim = title).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(title: String, selection: Binding<Date>, in_: IntRange, displayedComponents: DatePickerComponents = DatePickerComponents.of(DatePickerComponents.hourAndMinute, DatePickerComponents.date)): this(selection = selection, displayedComponents = displayedComponents, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(verbatim = title).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    @Composable
    override fun Render(context: ComposeContext) {
        val contentContext = context.content()
        val horizontalArrangement = Arrangement.spacedBy(8.dp)
        if (EnvironmentValues.shared._labelsHidden) {
            Row(modifier = context.modifier, horizontalArrangement = horizontalArrangement, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { -> RenderPickerContent(context = contentContext) }
        } else {
            ComposeContainer(modifier = context.modifier, fillWidth = true) { modifier ->
                Row(modifier = modifier, horizontalArrangement = horizontalArrangement, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { ->
                    Box(modifier = Modifier.weight(1.0f)) { -> label.Compose(context = contentContext) }
                    RenderPickerContent(context = contentContext)
                }
            }
        }
    }

    @Composable
    private fun RenderPickerContent(context: ComposeContext) {
        val isDatePickerPresented = remember { -> mutableStateOf(false) }
        val isTimePickerPresented = remember { -> mutableStateOf(false) }
        val isEnabled = EnvironmentValues.shared.isEnabled
        val date = selection.wrappedValue.sref()
        val (hour, minute) = hourAndMinute(from = date)
        val currentLocale = Locale(androidx.compose.ui.platform.LocalConfiguration.current.locales[0])

        dateFormatter?.locale = currentLocale
        dateFormatter?.string(from = date)?.let { dateString ->
            val text = Text(verbatim = dateString)
            if (isEnabled) {
                Button.RenderTextButton(label = text, context = context) { -> isDatePickerPresented.value = true }
            } else {
                text.Compose(context = context)
            }
        }
        timeFormatter?.locale = currentLocale
        timeFormatter?.string(from = date)?.let { timeString ->
            val text = Text(verbatim = timeString)
            if (isEnabled) {
                Button.RenderTextButton(label = text, context = context) { -> isTimePickerPresented.value = true }
            } else {
                text.Compose(context = context)
            }
        }

        val tintColor = (EnvironmentValues.shared._tint ?: Color.accentColor).colorImpl()
        RenderDatePicker(context = context, isPresented = isDatePickerPresented, tintColor = tintColor) { it -> didSelect(date = it, hour = hour, minute = minute) }
        RenderTimePicker(context = context, isPresented = isTimePickerPresented, tintColor = tintColor, hour = hour, minute = minute) { it, it_1 -> didSelect(date = date, hour = it, minute = it_1) }
    }

    @Composable
    private fun RenderDatePicker(context: ComposeContext, isPresented: MutableState<Boolean>, tintColor: androidx.compose.ui.graphics.Color, dateSelected: (Date) -> Unit) {
        if (!isPresented.value) {
            return
        }
        val timeZoneOffset = Double(TimeZone.current.secondsFromGMT())
        val initialSeconds = selection.wrappedValue.timeIntervalSince1970 + timeZoneOffset
        val state = rememberDatePickerState(initialSelectedDateMillis = Long(initialSeconds * 1000.0), initialDisplayMode = if (EnvironmentValues.shared.verticalSizeClass == UserInterfaceSizeClass.compact) DisplayMode.Input else DisplayMode.Picker)
        val colors = DatePickerDefaults.colors(selectedDayContainerColor = tintColor, selectedYearContainerColor = tintColor, todayDateBorderColor = tintColor, currentYearContentColor = tintColor)
        SimpleDatePickerDialog(onDismissRequest = { -> isPresented.value = false }) { -> DatePicker(modifier = context.modifier, state = state, colors = colors) }
        state.selectedDateMillis.sref()?.let { millis ->
            dateSelected(Date(timeIntervalSince1970 = Double(millis / 1000.0) - timeZoneOffset))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun RenderTimePicker(context: ComposeContext, isPresented: MutableState<Boolean>, tintColor: androidx.compose.ui.graphics.Color, hour: Int, minute: Int, timeSelected: (Int, Int) -> Unit) {
        if (!isPresented.value) {
            return
        }
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute)
        val containerColor = tintColor.copy(alpha = 0.25f)
        val colors = TimePickerDefaults.colors(selectorColor = tintColor, periodSelectorSelectedContainerColor = containerColor, timeSelectorSelectedContainerColor = containerColor)
        SimpleDatePickerDialog(onDismissRequest = { -> isPresented.value = false }) { -> TimePicker(modifier = context.modifier.padding(16.dp), state = state, colors = colors) }
        timeSelected(state.hour, state.minute)
    }

    private fun didSelect(date: Date, hour: Int, minute: Int) {
        // Subtract out any existing hour and minute from the given date, then add the selected values
        val (baseHour, baseMinute) = hourAndMinute(from = date)
        val baseSeconds = date.timeIntervalSince1970 - Double(baseHour * 60 * 60) - Double(baseMinute * 60)
        val selectedSeconds = baseSeconds + Double(hour * 60 * 60) + Double(minute * 60)
        if (selectedSeconds != selection.wrappedValue.timeIntervalSince1970) {
            // selection is a 'let' constant so Swift would not allow us to assign to it
            selection.wrappedValue = Date(timeIntervalSince1970 = selectedSeconds)
        }
    }

    private fun hourAndMinute(from: Date): Tuple2<Int, Int> {
        val date = from
        val calendar = Calendar.current.sref()
        val timeComponents = calendar.dateComponents(setOf(Calendar.Component.hour, Calendar.Component.minute), from = date)
        return Tuple2(timeComponents.hour!!, timeComponents.minute!!)
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

/// Simplification of the Material 3 `DatePickerDialog` source code.
///
/// We can't use the actual `DatePickerDialog` because it has a fixed size that cuts off content in landscape.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SimpleDatePickerDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    val horizontalPadding = (if (EnvironmentValues.shared.horizontalSizeClass == UserInterfaceSizeClass.compact) 16.dp else 0.dp).sref()
    BasicAlertDialog(modifier = Modifier.wrapContentHeight(), onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) { ->
        Surface(modifier = Modifier.padding(horizontal = horizontalPadding), shape = DatePickerDefaults.shape) { ->
            Column(verticalArrangement = Arrangement.SpaceBetween) { ->
                Box(Modifier.weight(1.0f, fill = false)) { -> content() }
            }
        }
    }
}

class DatePickerComponents: OptionSet<DatePickerComponents, Int>, MutableStruct {
    override var rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override val rawvaluelong: ULong
        get() = ULong(rawValue)
    override fun makeoptionset(rawvaluelong: ULong): DatePickerComponents = DatePickerComponents(rawValue = Int(rawvaluelong))
    override fun assignoptionset(target: DatePickerComponents) {
        willmutate()
        try {
            assignfrom(target)
        } finally {
            didmutate()
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as DatePickerComponents
        this.rawValue = copy.rawValue
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = DatePickerComponents(this as MutableStruct)

    private fun assignfrom(target: DatePickerComponents) {
        this.rawValue = target.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val hourAndMinute = DatePickerComponents(rawValue = 1 shl 0) // For bridging
        val date = DatePickerComponents(rawValue = 1 shl 1) // For bridging

        fun of(vararg options: DatePickerComponents): DatePickerComponents {
            val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
            return DatePickerComponents(rawValue = value)
        }
    }
}

class DatePickerStyle: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is DatePickerStyle) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = DatePickerStyle(rawValue = 0) // For bridging

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val graphical = DatePickerStyle(rawValue = 1) // For bridging

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val wheel = DatePickerStyle(rawValue = 2) // For bridging

        val compact = DatePickerStyle(rawValue = 3) // For bridging
    }
}

/*
import struct Foundation.Date
import struct Foundation.DateComponents
import struct Foundation.DateInterval

/// The properties of a `DatePicker`.
@available(iOS 16.0, macOS 13.0, watchOS 10.0, *)
@available(tvOS, unavailable)
public struct DatePickerStyleConfiguration {

/// A type-erased label of a `DatePicker`.
public struct Label : View {

/// The type of view representing the body of this view.
///
/// When you create a custom view, Swift infers this type from your
/// implementation of the required ``View/body-swift.property`` property.
public typealias Body = NeverView
public var body: Body { fatalError() }
}

/// A description of the `DatePicker`.
public let label: DatePickerStyleConfiguration.Label = { fatalError() }()

/// The date value being displayed and selected.
//    @Binding public var selection: Date { get { fatalError() } nonmutating set { } }

//    public var $selection: Binding<Date> { get { fatalError() } }

/// The oldest selectable date.
public var minimumDate: Date?

/// The most recent selectable date.
public var maximumDate: Date?

/// The date components that the user is able to view and edit.
public var displayedComponents: DatePickerComponents { get { fatalError() } }
}

/// A control for picking multiple dates.
///
/// Use a `MultiDatePicker` when you want to provide a view that allows the
/// user to select multiple dates.
///
/// The following example creates a basic `MultiDatePicker`, which appears as a
/// calendar view representing the selected dates:
///
///     @State private var dates: Set<DateComponents> = []
///
///     var body: some View {
///         MultiDatePicker("Dates Available", selection: $dates)
///     }
///
/// You can limit the `MultiDatePicker` to specific ranges of dates
/// allowing selections only before or after a certain date or between two
/// dates. The following example shows a multi-date picker that only permits
/// selections within the 6th and (excluding) the 16th of December 2021
/// (in the `UTC` time zone):
///
///     @Environment(\.calendar) var calendar
///     @Environment(\.timeZone) var timeZone
///
///     var bounds: Range<Date> {
///         let start = calendar.date(from: DateComponents(
///             timeZone: timeZone, year: 2022, month: 6, day: 6))!
///         let end = calendar.date(from: DateComponents(
///             timeZone: timeZone, year: 2022, month: 6, day: 16))!
///         return start until end
///     }
///
///     @State private var dates: Set<DateComponents> = []
///
///     var body: some View {
///         MultiDatePicker("Dates Available", selection: $dates, in: bounds)
///     }
///
/// You can also specify an alternative locale, calendar and time zone through
/// environment values. This can be useful when using a ``PreviewProvider`` to
/// see how your multi-date picker behaves in environments that differ from
/// your own.
///
/// The following example shows a multi-date picker with a custom locale,
/// calendar and time zone:
///
///     struct ContentView_Previews: PreviewProvider {
///         static var previews: some View {
///             MultiDatePicker("Dates Available", selection: .constant([]))
///                 .environment(\.locale, Locale.init(identifier: "zh"))
///                 .environment(
///                     \.calendar, Calendar.init(identifier: .chinese))
///                 .environment(\.timeZone, TimeZone(abbreviation: "HKT")!)
///         }
///     }
///
@available(iOS 16.0, *)
@available(macOS, unavailable)
@available(tvOS, unavailable)
@available(watchOS, unavailable)
public struct MultiDatePicker<Label> : View where Label : View {

@MainActor public var body: some View { get { return stubView() } }

//    public typealias Body = some View
}

@available(iOS 16.0, *)
@available(macOS, unavailable)
@available(tvOS, unavailable)
@available(watchOS, unavailable)
extension MultiDatePicker {

/// Creates an instance that selects multiple dates with an unbounded
/// range.
///
/// - Parameters:
///   - selection: The date values being displayed and selected.
///   - label: A view that describes the use of the dates.
public init(selection: Binding<Set<DateComponents>>, @ViewBuilder label: () -> Label) { fatalError() }

/// Creates an instance that selects multiple dates in a range.
///
/// - Parameters:
///   - selection: The date values being displayed and selected.
///   - bounds: The exclusive range of selectable dates.
///   - label: A view that describes the use of the dates.
public init(selection: Binding<Set<DateComponents>>, in bounds: Range<Date>, @ViewBuilder label: () -> Label) { fatalError() }

/// Creates an instance that selects multiple dates on or after some
/// start date.
///
/// - Parameters:
///   - selection: The date values being displayed and selected.
///   - bounds: The open range from some selectable start date.
///   - label: A view that describes the use of the dates.
public init(selection: Binding<Set<DateComponents>>, in bounds: PartialRangeFrom<Date>, @ViewBuilder label: () -> Label) { fatalError() }

/// Creates an instance that selects multiple dates before some end date.
///
/// - Parameters:
///   - selection: The date values being displayed and selected.
///   - bounds: The open range before some end date.
///   - label: A view that describes the use of the dates.
public init(selection: Binding<Set<DateComponents>>, in bounds: PartialRangeUpTo<Date>, @ViewBuilder label: () -> Label) { fatalError() }
}

@available(iOS 16.0, *)
@available(macOS, unavailable)
@available(tvOS, unavailable)
@available(watchOS, unavailable)
extension MultiDatePicker where Label == Text {

/// Creates an instance that selects multiple dates with an unbounded
/// range.
///
/// - Parameters:
///   - titleKey: The key for the localized title of `self`, describing
///     its purpose.
///   - selection: The date values being displayed and selected.
public init(_ titleKey: LocalizedStringKey, selection: Binding<Set<DateComponents>>) { fatalError() }

/// Creates an instance that selects multiple dates in a range.
///
/// - Parameters:
///   - titleKey: The key for the localized title of `self`, describing
///     its purpose.
///   - selection: The date values being displayed and selected.
///   - bounds: The exclusive range of selectable dates.
public init(_ titleKey: LocalizedStringKey, selection: Binding<Set<DateComponents>>, in bounds: Range<Date>) { fatalError() }

/// Creates an instance that selects multiple dates on or after some
/// start date.
///
/// - Parameters:
///   - titleKey: The key for the localized title of `self`, describing
///     its purpose.
///   - selection: The date values being displayed and selected.
///   - bounds: The open range from some selectable start date.
public init(_ titleKey: LocalizedStringKey, selection: Binding<Set<DateComponents>>, in bounds: PartialRangeFrom<Date>) { fatalError() }

/// Creates an instance that selects multiple dates before some end date.
///
/// - Parameters:
///   - titleKey: The key for the localized title of `self`, describing
///     its purpose.
///   - selection: The date values being displayed and selected.
///   - bounds: The open range before some end date.
public init(_ titleKey: LocalizedStringKey, selection: Binding<Set<DateComponents>>, in bounds: PartialRangeUpTo<Date>) { fatalError() }
}

@available(iOS 16.0, *)
@available(macOS, unavailable)
@available(tvOS, unavailable)
@available(watchOS, unavailable)
extension MultiDatePicker where Label == Text {

/// Creates an instance that selects multiple dates with an unbounded
/// range.
///
/// - Parameters:
///   - title: The title of `self`, describing its purpose.
///   - selection: The date values being displayed and selected.
public init<S>(_ title: S, selection: Binding<Set<DateComponents>>) where S : StringProtocol { fatalError() }

/// Creates an instance that selects multiple dates in a range.
///
/// - Parameters:
///   - title: The title of `self`, describing its purpose.
///   - selection: The date values being displayed and selected.
///   - bounds: The exclusive range of selectable dates.
public init<S>(_ title: S, selection: Binding<Set<DateComponents>>, in bounds: Range<Date>) where S : StringProtocol { fatalError() }

/// Creates an instance that selects multiple dates on or after some
/// start date.
///
/// - Parameters:
///   - title: The title of `self`, describing its purpose.
///   - selection: The date values being displayed and selected.
///   - bounds: The open range from some selectable start date.
public init<S>(_ title: S, selection: Binding<Set<DateComponents>>, in bounds: PartialRangeFrom<Date>) where S : StringProtocol { fatalError() }

/// Creates an instance that selects multiple dates before some end date.
///
/// - Parameters:
///   - title: The title of `self`, describing its purpose.
///   - selection: The date values being displayed and selected.
///   - bounds: The open range before some end date.
public init<S>(_ title: S, selection: Binding<Set<DateComponents>>, in bounds: PartialRangeUpTo<Date>) where S : StringProtocol { fatalError() }
}
*/
