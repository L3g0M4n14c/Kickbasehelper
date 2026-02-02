package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@androidx.annotation.Keep
class ProgressView: View, Renderable, skip.lib.SwiftProjecting {
    internal val value: Double?
    internal val total: Double?
    internal val label: ComposeBuilder?

    constructor() {
        this.value = null
        this.total = null
        this.label = null
    }

    constructor(label: () -> View): this(value = null, label = label) {
    }

    constructor(titleKey: LocalizedStringKey): this(titleKey, value = null) {
    }

    constructor(titleResource: LocalizedStringResource): this(titleResource, value = null) {
    }

    constructor(title: String): this(title, value = null) {
    }

    constructor(value: Double?, total: Double = 1.0) {
        this.value = value
        this.total = total
        this.label = null
    }

    constructor(value: Double?, total: Double = 1.0, label: () -> View) {
        this.value = value
        this.total = total
        this.label = ComposeBuilder.from(label)
    }

    constructor(titleKey: LocalizedStringKey, value: Double?, total: Double = 1.0): this(value = value, total = total, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleKey).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(titleResource: LocalizedStringResource, value: Double?, total: Double = 1.0): this(value = value, total = total, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(titleResource).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(title: String, value: Double?, total: Double = 1.0): this(value = value, total = total, label = { ->
        ComposeBuilder { composectx: ComposeContext ->
            Text(verbatim = title).Compose(composectx)
            ComposeResult.ok
        }
    }) {
    }

    constructor(value: Double?, total: Double?, bridgedLabel: View?) {
        this.value = value
        this.total = total
        this.label = if (bridgedLabel == null) null else ComposeBuilder.from { -> bridgedLabel!! }
    }

    @Composable
    override fun Render(context: ComposeContext) {
        var style = EnvironmentValues.shared._progressViewStyle ?: ProgressViewStyle.automatic
        if (style == ProgressViewStyle.automatic) {
            style = if (value == null) ProgressViewStyle.circular else ProgressViewStyle.linear
        }
        if (style == ProgressViewStyle.linear) {
            if ((label != null) && !EnvironmentValues.shared._labelsHidden) {
                val contentContext = context.content()
                ComposeContainer(modifier = context.modifier, fillWidth = true) { modifier ->
                    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = androidx.compose.ui.Alignment.Start) { ->
                        label.Compose(context = contentContext)
                        RenderLinearProgress(context = contentContext)
                    }
                }
            } else {
                RenderLinearProgress(context = context)
            }
        } else if ((label != null) && !EnvironmentValues.shared._labelsHidden) {
            val contentContext = context.content()
            Column(modifier = context.modifier, verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) { ->
                RenderCircularProgress(context = contentContext)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { -> label.Compose(context = contentContext) }
            }
        } else {
            RenderCircularProgress(context = context)
        }
    }

    @Composable
    private fun RenderLinearProgress(context: ComposeContext) {
        val modifier = Modifier.fillWidth().then(context.modifier)
        val color = EnvironmentValues.shared._tint?.colorImpl?.invoke() ?: ProgressIndicatorDefaults.linearColor
        if (value == null || total == null) {
            LinearProgressIndicator(modifier = modifier, color = color)
        } else {
            LinearProgressIndicator(progress = Float(value!! / total!!), modifier = modifier, color = color)
        }
    }

    @Composable
    private fun RenderCircularProgress(context: ComposeContext) {
        val color = EnvironmentValues.shared._tint?.colorImpl?.invoke() ?: ProgressIndicatorDefaults.circularColor
        // Reduce size to better match SwiftUI
        val indicatorModifier = Modifier.size(20.dp)
        Box(modifier = context.modifier, contentAlignment = androidx.compose.ui.Alignment.Center) { ->
            if (value == null || total == null) {
                CircularProgressIndicator(modifier = indicatorModifier, color = color)
            } else {
                CircularProgressIndicator(progress = Float(value!! / total!!), modifier = indicatorModifier, color = color)
            }
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

class ProgressViewStyle: RawRepresentable<Int> {
    override val rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ProgressViewStyle) return false
        return rawValue == other.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val automatic = ProgressViewStyle(rawValue = 0) // For bridging
        val linear = ProgressViewStyle(rawValue = 1) // For bridging
        val circular = ProgressViewStyle(rawValue = 2) // For bridging
    }
}

/*
import struct Foundation.Date
import class Foundation.Progress

@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
extension ProgressView {

/// Creates a progress view for showing continuous progress as time passes,
/// with descriptive and current progress labels.
///
/// Use this initializer to create a view that shows continuous progress
/// within a date range. The following example initializes a progress view
/// with a range of `start...end`, where `start` is 30 seconds in the past
/// and `end` is 90 seconds in the future. As a result, the progress view
/// begins at 25 percent complete. This example also provides custom views
/// for a descriptive label (Progress) and a current value label that shows
/// the date range.
///
///     struct ContentView: View {
///         let start = Date().addingTimeInterval(-30)
///         let end = Date().addingTimeInterval(90)
///
///         var body: some View {
///             ProgressView(interval: start...end,
///                          countsDown: false) {
///                 Text("Progress")
///             } currentValueLabel: {
///                 Text(start...end)
///              }
///          }
///     }
///
/// ![A horizontal bar that represents progress, partially filled in from
/// the leading edge. The title, Progress, appears above the bar, and the
/// date range, 1:43 to 1:45 PM, appears below the bar. These values represent
/// the time progress began and when it ends, given a current time of
/// 1:44.](ProgressView-6-macOS)
///
/// By default, the progress view empties as time passes from the start of
/// the date range to the end, but you can use the `countsDown` parameter to
/// create a progress view that fills as time passes, as the above example
/// demonstrates.
///
/// > Note: Date-relative progress views, such as those created with this
///   initializer, don't support custom styles.
///
/// - Parameters:
///     - timerInterval: The date range over which the view should progress.
///     - countsDown: A Boolean value that determines whether the view
///       empties or fills as time passes. If `true` (the default), the
///       view empties.
///     - label: An optional view that describes the purpose of the progress
///       view.
///     - currentValueLabel: A view that displays the current value of the
///       timer.
//    public init(timerInterval: ClosedRange<Date>, countsDown: Bool = true, @ViewBuilder label: () -> Label, @ViewBuilder currentValueLabel: () -> CurrentValueLabel) { fatalError() }
}

//@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
//extension ProgressView where CurrentValueLabel == DefaultDateProgressLabel {

/// Creates a progress view for showing continuous progress as time passes,
/// with a descriptive label.
///
/// Use this initializer to create a view that shows continuous progress
/// within a date range. The following example initializes a progress view
/// with a range of `start...end`, where `start` is 30 seconds in the past
/// and `end` is 90 seconds in the future. As a result, the progress view
/// begins at 25 percent complete. This example also provides a custom
/// descriptive label.
///
///     struct ContentView: View {
///         let start = Date().addingTimeInterval(-30)
///         let end = Date().addingTimeInterval(90)
///
///         var body: some View {
///             ProgressView(interval: start...end,
///                          countsDown: false) {
///                 Text("Progress")
///              }
///         }
///     }
///
/// ![A horizontal bar that represents progress, partially filled in from
/// the leading edge. The title, Progress, appears above the bar, and the
/// elapsed time, 0:34, appears below the bar.](ProgressView-7-macOS)
///
/// By default, the progress view empties as time passes from the start of
/// the date range to the end, but you can use the `countsDown` parameter to
/// create a progress view that fills as time passes, as the above example
/// demonstrates.
///
/// The progress view provided by this initializer uses a text label that
/// automatically updates to describe the current time remaining. To provide
/// a custom label to show the current value, use
/// ``init(value:total:label:currentValueLabel:)`` instead.
///
/// > Note: Date-relative progress views, such as those created with this
///   initializer, don't support custom styles.
///
/// - Parameters:
///     - timerInterval: The date range over which the view progresses.
///     - countsDown: A Boolean value that determines whether the view
///       empties or fills as time passes. If `true` (the default), the
///       view empties.
///     - label: An optional view that describes the purpose of the progress
///       view.
//    public init(timerInterval: ClosedRange<Date>, countsDown: Bool = true, @ViewBuilder label: () -> Label) { fatalError() }
//}

//@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
//extension ProgressView where Label == EmptyView, CurrentValueLabel == DefaultDateProgressLabel {

/// Creates a progress view for showing continuous progress as time passes.
///
/// Use this initializer to create a view that shows continuous progress
/// within a date range. The following example initializes a progress view
/// with a range of `start...end`, where `start` is 30 seconds in the past
/// and `end` is 90 seconds in the future. As a result, the progress view
/// begins at 25 percent complete.
///
///     struct ContentView: View {
///         let start = Date().addingTimeInterval(-30)
///         let end = Date().addingTimeInterval(90)
///
///         var body: some View {
///             ProgressView(interval: start...end
///                          countsDown: false)
///         }
///     }
///
/// ![A horizontal bar that represents progress, partially filled in from
/// the leading edge. The elapsed time, 0:34, appears below the
/// bar.](ProgressView-8-macOS)
///
/// By default, the progress view empties as time passes from the start of
/// the date range to the end, but you can use the `countsDown` parameter to
/// create a progress view that fills as time passes, as the above example
/// demonstrates.
///
/// The progress view provided by this initializer omits a descriptive
/// label and provides a text label that automatically updates to describe
/// the current time remaining. To provide custom views for these labels,
/// use ``init(value:total:label:currentValueLabel:)`` instead.
///
/// > Note: Date-relative progress views, such as those created with this
///   initializer, don't support custom styles.
///
/// - Parameters:
///     - timerInterval: The date range over which the view progresses.
///     - countsDown: If `true` (the default), the view empties as time passes.
//    public init(timerInterval: ClosedRange<Date>, countsDown: Bool = true) { fatalError() }
//}

@available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
extension ProgressView {

/// Creates a progress view for visualizing the given progress instance.
///
/// The progress view synthesizes a default label using the
/// `localizedDescription` of the given progress instance.
//    public init(_ progress: Progress) where Label == EmptyView, CurrentValueLabel == EmptyView { fatalError() }
}

@available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
extension ProgressView {

/// Creates a progress view based on a style configuration.
///
/// You can use this initializer within the
/// ``ProgressViewStyle/makeBody(configuration:)`` method of a
/// ``ProgressViewStyle`` to create an instance of the styled progress view.
/// This is useful for custom progress view styles that only modify the
/// current progress view style, as opposed to implementing a brand new
/// style. Because this modifier style can't know how the current style
/// represents progress, avoid making assumptions about the view's contents,
/// such as whether it uses bars or other shapes.
///
/// The following example shows a style that adds a rounded pink border to a
/// progress view, but otherwise preserves the progress view's current
/// style:
///
///     struct PinkBorderedProgressViewStyle: ProgressViewStyle {
///         func makeBody(configuration: Configuration) -> some View {
///             ProgressView(configuration)
///                 .padding(4)
///                 .border(.pink, width: 3)
///                 .cornerRadius(4)
///         }
///     }
///
/// ![Two horizontal progress views, one at 25 percent complete and the
/// other at 75 percent, each rendered with a rounded pink
/// border.](ProgressView-4-macOS)
///
/// - Note: Progress views in widgets don't apply custom styles.
//    public init(_ configuration: ProgressViewStyleConfiguration) where Label == ProgressViewStyleConfiguration.Label, CurrentValueLabel == ProgressViewStyleConfiguration.CurrentValueLabel { fatalError() }
}

/// The properties of a progress view instance.
@available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
public struct ProgressViewStyleConfiguration {

/// A type-erased label describing the task represented by the progress
/// view.
public struct Label : View {

/// The type of view representing the body of this view.
///
/// When you create a custom view, Swift infers this type from your
/// implementation of the required ``View/body-swift.property`` property.
public typealias Body = NeverView
public var body: Body { fatalError() }
}

/// A type-erased label that describes the current value of a progress view.
public struct CurrentValueLabel : View {

/// The type of view representing the body of this view.
///
/// When you create a custom view, Swift infers this type from your
/// implementation of the required ``View/body-swift.property`` property.
public typealias Body = NeverView
public var body: Body { fatalError() }
}

/// The completed fraction of the task represented by the progress view,
/// from `0.0` (not yet started) to `1.0` (fully complete), or `nil` if the
/// progress is indeterminate or relative to a date interval.
public let fractionCompleted: Double?

/// A view that describes the task represented by the progress view.
///
/// If `nil`, then the task is self-evident from the surrounding context,
/// and the style does not need to provide any additional description.
///
/// If the progress view is defined using a `Progress` instance, then this
/// label is equivalent to its `localizedDescription`.
public var label: ProgressViewStyleConfiguration.Label?

/// A view that describes the current value of a progress view.
///
/// If `nil`, then the value of the progress view is either self-evident
/// from the surrounding context or unknown, and the style does not need to
/// provide any additional description.
///
/// If the progress view is defined using a `Progress` instance, then this
/// label is equivalent to its `localizedAdditionalDescription`.
public var currentValueLabel: ProgressViewStyleConfiguration.CurrentValueLabel?
}

/// The default type of the current value label when used by a date-relative
/// progress view.
@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
public struct DefaultDateProgressLabel : View {

@MainActor public var body: some View { get { return stubView() } }

//    public typealias Body = some View
}
*/
