package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.popup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTagsAsResourceId

class AccessibilityActionKind {

    constructor(named: Text) {
    }

    private constructor() {
    }

    override fun equals(other: Any?): Boolean = other is AccessibilityActionKind

    @androidx.annotation.Keep
    companion object {
        val default = AccessibilityActionKind()
        val escape = AccessibilityActionKind()
        val magicTap = AccessibilityActionKind()
    }
}

enum class AccessibilityAdjustmentDirection {
    increment,
    decrement;

    @androidx.annotation.Keep
    companion object {
    }
}

enum class AccessibilityChildBehavior {
    ignore,
    contain,
    combine;

    @androidx.annotation.Keep
    companion object {
    }
}

class AccessibilityCustomContentKey {
    constructor(label: Text, id: String) {
    }

    constructor(labelKey: LocalizedStringKey, id: String) {
    }

    constructor(labelKey: LocalizedStringKey) {
    }

    constructor(label: LocalizedStringResource, id: String) {
    }

    constructor(label: LocalizedStringResource) {
    }

    override fun equals(other: Any?): Boolean = other is AccessibilityCustomContentKey

    @androidx.annotation.Keep
    companion object {
    }
}

class AccessibilityDirectTouchOptions: OptionSet<AccessibilityDirectTouchOptions, Int>, MutableStruct {
    override var rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override val rawvaluelong: ULong
        get() = ULong(rawValue)
    override fun makeoptionset(rawvaluelong: ULong): AccessibilityDirectTouchOptions = AccessibilityDirectTouchOptions(rawValue = Int(rawvaluelong))
    override fun assignoptionset(target: AccessibilityDirectTouchOptions) {
        willmutate()
        try {
            assignfrom(target)
        } finally {
            didmutate()
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as AccessibilityDirectTouchOptions
        this.rawValue = copy.rawValue
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = AccessibilityDirectTouchOptions(this as MutableStruct)

    private fun assignfrom(target: AccessibilityDirectTouchOptions) {
        this.rawValue = target.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val silentOnTouch = AccessibilityDirectTouchOptions(rawValue = 1)
        val requiresActivation = AccessibilityDirectTouchOptions(rawValue = 2)

        fun of(vararg options: AccessibilityDirectTouchOptions): AccessibilityDirectTouchOptions {
            val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
            return AccessibilityDirectTouchOptions(rawValue = value)
        }
    }
}

enum class AccessibilityHeadingLevel(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
    unspecified(0), // For bridging
    h1(1), // For bridging
    h2(2), // For bridging
    h3(3), // For bridging
    h4(4), // For bridging
    h5(5), // For bridging
    h6(6); // For bridging

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: Int): AccessibilityHeadingLevel? {
            return when (rawValue) {
                0 -> AccessibilityHeadingLevel.unspecified
                1 -> AccessibilityHeadingLevel.h1
                2 -> AccessibilityHeadingLevel.h2
                3 -> AccessibilityHeadingLevel.h3
                4 -> AccessibilityHeadingLevel.h4
                5 -> AccessibilityHeadingLevel.h5
                6 -> AccessibilityHeadingLevel.h6
                else -> null
            }
        }
    }
}

fun AccessibilityHeadingLevel(rawValue: Int): AccessibilityHeadingLevel? = AccessibilityHeadingLevel.init(rawValue = rawValue)

enum class AccessibilityLabeledPairRole {
    label,
    content;

    @androidx.annotation.Keep
    companion object {
    }
}

interface AccessibilityRotorContent {
}

class AccessibilityRotorEntry<ID>: AccessibilityRotorContent {
    constructor(label: Text, id: ID, textRange: IntRange? = null, prepare: () -> Unit = { ->  }) {
    }

    constructor(label: Text, id: ID, in_: Namespace.ID, textRange: IntRange? = null, prepare: () -> Unit = { ->  }) {
    }

    constructor(label: Text? = null, textRange: IntRange, prepare: () -> Unit = { ->  }) {
    }

    constructor(labelKey: LocalizedStringKey, id: ID, textRange: IntRange? = null, prepare: () -> Unit = { ->  }) {
    }

    constructor(labelResource: LocalizedStringResource, id: ID, textRange: IntRange? = null, prepare: () -> Unit = { ->  }) {
    }

    constructor(label: String, id: ID, textRange: IntRange? = null, prepare: () -> Unit = { ->  }) {
    }

    constructor(labelKey: LocalizedStringKey, id: ID, in_: Namespace.ID, textRange: IntRange? = null, prepare: () -> Unit = { ->  }) {
    }

    constructor(labelResource: LocalizedStringResource, id: ID, in_: Namespace.ID, textRange: IntRange? = null, prepare: () -> Unit = { ->  }) {
    }

    constructor(labelKey: LocalizedStringKey, textRange: IntRange, prepare: () -> Unit = { ->  }) {
    }

    constructor(labelResource: LocalizedStringResource, textRange: IntRange, prepare: () -> Unit = { ->  }) {
    }

    constructor(label: String, textRange: IntRange, prepare: () -> Unit = { ->  }) {
    }

    @androidx.annotation.Keep
    companion object {
    }
}

class AccessibilitySystemRotor {

    @androidx.annotation.Keep
    companion object {
        fun links(visited: Boolean): AccessibilitySystemRotor = AccessibilitySystemRotor()

        val links = AccessibilitySystemRotor()

        fun headings(level: AccessibilityHeadingLevel): AccessibilitySystemRotor = AccessibilitySystemRotor()

        val headings = AccessibilitySystemRotor()
        val boldText = AccessibilitySystemRotor()
        val italicText = AccessibilitySystemRotor()
        val underlineText = AccessibilitySystemRotor()
        val misspelledWords = AccessibilitySystemRotor()
        val images = AccessibilitySystemRotor()
        val textFields = AccessibilitySystemRotor()
        val tables = AccessibilitySystemRotor()
        val lists = AccessibilitySystemRotor()
        val landmarks = AccessibilitySystemRotor()
    }
}

class AccessibilityTechnologies: OptionSet<AccessibilityTechnologies, Int>, MutableStruct {
    override var rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override val rawvaluelong: ULong
        get() = ULong(rawValue)
    override fun makeoptionset(rawvaluelong: ULong): AccessibilityTechnologies = AccessibilityTechnologies(rawValue = Int(rawvaluelong))
    override fun assignoptionset(target: AccessibilityTechnologies) {
        willmutate()
        try {
            assignfrom(target)
        } finally {
            didmutate()
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as AccessibilityTechnologies
        this.rawValue = copy.rawValue
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = AccessibilityTechnologies(this as MutableStruct)

    private fun assignfrom(target: AccessibilityTechnologies) {
        this.rawValue = target.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val voiceOver = AccessibilityTechnologies(rawValue = 1)
        val switchControl = AccessibilityTechnologies(rawValue = 2)

        fun of(vararg options: AccessibilityTechnologies): AccessibilityTechnologies {
            val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
            return AccessibilityTechnologies(rawValue = value)
        }
    }
}

enum class AccessibilityTextContentType {
    plain,
    console,
    fileSystem,
    messaging,
    narrative,
    sourceCode,
    spreadsheet,
    wordProcessing;

    @androidx.annotation.Keep
    companion object {
    }
}

class AccessibilityTraits: OptionSet<AccessibilityTraits, Int>, MutableStruct {
    override var rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override val rawvaluelong: ULong
        get() = ULong(rawValue)
    override fun makeoptionset(rawvaluelong: ULong): AccessibilityTraits = AccessibilityTraits(rawValue = Int(rawvaluelong))
    override fun assignoptionset(target: AccessibilityTraits) {
        willmutate()
        try {
            assignfrom(target)
        } finally {
            didmutate()
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as AccessibilityTraits
        this.rawValue = copy.rawValue
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = AccessibilityTraits(this as MutableStruct)

    private fun assignfrom(target: AccessibilityTraits) {
        this.rawValue = target.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val isButton = AccessibilityTraits(rawValue = 1 shl 0) // For bridging
        val isHeader = AccessibilityTraits(rawValue = 1 shl 1) // For bridging
        val isSelected = AccessibilityTraits(rawValue = 1 shl 2) // For bridging
        val isLink = AccessibilityTraits(rawValue = 1 shl 3) // For bridging
        val isSearchField = AccessibilityTraits(rawValue = 1 shl 4) // For bridging
        val isImage = AccessibilityTraits(rawValue = 1 shl 5) // For bridging
        val playsSound = AccessibilityTraits(rawValue = 1 shl 6) // For bridging
        val isKeyboardKey = AccessibilityTraits(rawValue = 1 shl 7) // For bridging
        val isStaticText = AccessibilityTraits(rawValue = 1 shl 8) // For bridging
        val isSummaryElement = AccessibilityTraits(rawValue = 1 shl 9) // For bridging
        val updatesFrequently = AccessibilityTraits(rawValue = 1 shl 10) // For bridging
        val startsMediaSession = AccessibilityTraits(rawValue = 1 shl 11) // For bridging
        val allowsDirectInteraction = AccessibilityTraits(rawValue = 1 shl 12) // For bridging
        val causesPageTurn = AccessibilityTraits(rawValue = 1 shl 13) // For bridging
        val isModal = AccessibilityTraits(rawValue = 1 shl 14) // For bridging
        val isToggle = AccessibilityTraits(rawValue = 1 shl 15) // For bridging
        val isTabBar = AccessibilityTraits(rawValue = 1 shl 16) // For bridging

        fun of(vararg options: AccessibilityTraits): AccessibilityTraits {
            val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
            return AccessibilityTraits(rawValue = value)
        }
    }
}

class AccessibilityZoomGestureAction {
    enum class Direction {
        zoomIn,
        zoomOut;

        @androidx.annotation.Keep
        companion object {
        }
    }

    val direction: AccessibilityZoomGestureAction.Direction
    val location: UnitPoint
    val point: CGPoint

    constructor(direction: AccessibilityZoomGestureAction.Direction, location: UnitPoint, point: CGPoint) {
        this.direction = direction
        this.location = location.sref()
        this.point = point.sref()
    }

    @androidx.annotation.Keep
    companion object {
    }
}

/*
@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
extension Never : AccessibilityRotorContent {
}

public struct AccessibilityAttachmentModifier : ViewModifier {
func body(content: Content) -> some View {
content
}
}

public struct AccessibilityFocusState<Value> : DynamicProperty where Value : Hashable {

@propertyWrapper @frozen public struct Binding {

/// The underlying value referenced by the bound property.
public var wrappedValue: Value { get { fatalError() } nonmutating set { } }

/// The currently focused element.
public var projectedValue: AccessibilityFocusState<Value>.Binding { get { fatalError() } }

public init(wrappedValue: Value) { fatalError() }
}

/// The current state value, taking into account whatever bindings might be
/// in effect due to the current location of focus.
///
/// When focus is not in any view that is bound to this state, the wrapped
/// value will be `nil` (for optional-typed state) or `false` (for `Bool`-
/// typed state).
public var wrappedValue: Value { get { fatalError() } nonmutating set { } }

/// A projection of the state value that can be used to establish bindings between view content
/// and accessibility focus placement.
///
/// Use `projectedValue` in conjunction with
/// ``SkipUI/View/accessibilityFocused(_:equals:)`` to establish
/// bindings between view content and accessibility focus placement.
public var projectedValue: AccessibilityFocusState<Value>.Binding { get { fatalError() } }

/// Creates a new accessibility focus state for a Boolean value.
public init() where Value == Bool { fatalError() }

/// Creates a new accessibility focus state for a Boolean value, using the accessibility
/// technologies you specify.
///
/// - Parameters:
///   - technologies: One of the available ``AccessibilityTechnologies``.
public init(for technologies: AccessibilityTechnologies) where Value == Bool { fatalError() }

/// Creates a new accessibility focus state of the type you provide.
public init<T>() where Value == T?, T : Hashable { fatalError() }

/// Creates a new accessibility focus state of the type and
/// using the accessibility technologies you specify.
///
/// - Parameter technologies: One or more of the available
///  ``AccessibilityTechnologies``.
public init<T>(for technologies: AccessibilityTechnologies) where Value == T?, T : Hashable { fatalError() }
}

/// A gauge style that displays a closed ring that's partially filled in to
/// indicate the gauge's current value.
///
/// Use ``GaugeStyle/accessoryCircularCapacity`` to construct this style.
@available(iOS 16.0, macOS 13.0, watchOS 9.0, *)
@available(tvOS, unavailable)
public struct AccessoryCircularCapacityGaugeStyle : GaugeStyle {

/// Creates an accessory circular capacity gauge style.
public init() { fatalError() }

/// Creates a view representing the body of a gauge.
///
/// The system calls this modifier on each instance of gauge within a view
/// hierarchy where this style is the current gauge style.
///
/// - Parameter configuration: The properties to apply to the gauge instance.
public func makeBody(configuration: AccessoryCircularCapacityGaugeStyle.Configuration) -> some View { return stubView() }


/// A view representing the body of a gauge.
//    public typealias Body = some View
}

/// A gauge style that displays an open ring with a marker that appears at a
/// point along the ring to indicate the gauge's current value.
///
/// Use ``GaugeStyle/accessoryCircular`` to construct this style.
@available(iOS 16.0, macOS 13.0, watchOS 9.0, *)
@available(tvOS, unavailable)
public struct AccessoryCircularGaugeStyle : GaugeStyle {

/// Creates an accessory circular gauge style.
public init() { fatalError() }

/// Creates a view representing the body of a gauge.
///
/// The system calls this modifier on each instance of gauge within a view
/// hierarchy where this style is the current gauge style.
///
/// - Parameter configuration: The properties to apply to the gauge instance.
public func makeBody(configuration: AccessoryCircularGaugeStyle.Configuration) -> some View { return stubView() }


/// A view representing the body of a gauge.
//    public typealias Body = some View
}

/// A gauge style that displays bar that fills from leading to trailing
/// edges as the gauge's current value increases.
///
/// Use ``GaugeStyle/accessoryLinearCapacity`` to construct this style.
@available(iOS 16.0, macOS 13.0, watchOS 9.0, *)
@available(tvOS, unavailable)
public struct AccessoryLinearCapacityGaugeStyle : GaugeStyle {

/// Creates an accessory linear capacity gauge style.
public init() { fatalError() }

/// Creates a view representing the body of a gauge.
///
/// The system calls this modifier on each instance of gauge within a view
/// hierarchy where this style is the current gauge style.
///
/// - Parameter configuration: The properties to apply to the gauge instance.
public func makeBody(configuration: AccessoryLinearCapacityGaugeStyle.Configuration) -> some View { return stubView() }


/// A view representing the body of a gauge.
//    public typealias Body = some View
}

/// A gauge style that displays bar with a marker that appears at a
/// point along the bar to indicate the gauge's current value.
///
/// Use ``GaugeStyle/accessoryLinear`` to construct this style.
@available(iOS 16.0, macOS 13.0, watchOS 9.0, *)
@available(tvOS, unavailable)
public struct AccessoryLinearGaugeStyle : GaugeStyle {

/// Creates an accessory linear gauge style.
public init() { fatalError() }

/// Creates a view representing the body of a gauge.
///
/// The system calls this modifier on each instance of gauge within a view
/// hierarchy where this style is the current gauge style.
///
/// - Parameter configuration: The properties to apply to the gauge instance.
public func makeBody(configuration: AccessoryLinearGaugeStyle.Configuration) -> some View { return stubView() }


/// A view representing the body of a gauge.
//    public typealias Body = some View
}

/// A type to generate an `AXChartDescriptor` object that you use to provide
/// information about a chart and its data for an accessible experience
/// in VoiceOver or other assistive technologies.
///
/// Note that you may use the `@Environment` property wrapper inside the
/// implementation of your `AXChartDescriptorRepresentable`, in which case you
/// should implement `updateChartDescriptor`, which will be called when the
/// `Environment` changes.
///
/// For example, to provide accessibility for a view that represents a chart,
/// you would first declare your chart descriptor representable type:
///
///     struct MyChartDescriptorRepresentable: AXChartDescriptorRepresentable {
///         func makeChartDescriptor() -> AXChartDescriptor {
///             // Build and return your `AXChartDescriptor` here.
///         }
///
///         func updateChartDescriptor(_ descriptor: AXChartDescriptor) {
///             // Update your chart descriptor with any new values.
///         }
///     }
///
/// Then, provide an instance of your `AXChartDescriptorRepresentable` type to
/// your view using the `accessibilityChartDescriptor` modifier:
///
///     var body: some View {
///         MyChartView()
///             .accessibilityChartDescriptor(MyChartDescriptorRepresentable())
///     }
@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
public protocol AXChartDescriptorRepresentable {

/// Create the `AXChartDescriptor` for this view, and return it.
///
/// This will be called once per identity of your `View`. It will not be run
/// again unless the identity of your `View` changes. If you need to
/// update the `AXChartDescriptor` based on changes in your `View`, or in
/// the `Environment`, implement `updateChartDescriptor`.
/// This method will only be called if / when accessibility needs the
/// `AXChartDescriptor` of your view, for VoiceOver.
func makeChartDescriptor() -> AXChartDescriptor

/// Update the existing `AXChartDescriptor` for your view, based on changes
/// in your view or in the `Environment`.
///
/// This will be called as needed, when accessibility needs your
/// `AXChartDescriptor` for VoiceOver. It will only be called if the inputs
/// to your views, or a relevant part of the `Environment`, have changed.
func updateChartDescriptor(_ descriptor: AXChartDescriptor)
}

@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
extension AXChartDescriptorRepresentable {

/// Update the existing `AXChartDescriptor` for your view, based on changes
/// in your view or in the `Environment`.
///
/// This will be called as needed, when accessibility needs your
/// `AXChartDescriptor` for VoiceOver. It will only be called if the inputs
/// to your views, or a relevant part of the `Environment`, have changed.
public func updateChartDescriptor(_ descriptor: AXChartDescriptor) { fatalError() }
}

//@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
//extension Group : AccessibilityRotorContent where Content : AccessibilityRotorContent {
//
//    /// The internal content of this `AccessibilityRotorContent`.
//    public var body: Never { get { fatalError() } }
//
//    /// Creates an instance that generates Rotor content by combining, in order,
//    /// all the Rotor content specified in the passed-in result builder.
//    ///
//    /// - Parameter content: The result builder that generates Rotor content for
//    ///   the group.
//    public init(@AccessibilityRotorContentBuilder content: () -> Content) { fatalError() }
//}



@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
extension ModifiedContent where Modifier == AccessibilityAttachmentModifier {

/// Add additional accessibility information to the view.
///
/// Use this method to add information you want accessibility users to be
/// able to access about this element, beyond the basics of label, value,
/// and hint. For example, `accessibilityCustomContent` can be used to add
/// information about the orientation of a photograph, or the number of
/// people found in the picture.
///
/// - Parameter key: Key used to specify the identifier and label of the
///   of the additional accessibility information entry.
/// - Parameter value: Text value for the additional accessibility
///   information. For example: "landscape." A value of `nil` will remove
///   any entry of additional information added earlier for any `key` with
///   the same identifier.
/// - Note: Repeated calls of `accessibilityCustomContent` with `key`s
///   having different identifiers will create new entries of
///   additional information.
///   Calling `accessibilityAdditionalContent` repeatedly with `key`s
///   having matching identifiers will replace the previous entry.
public func accessibilityCustomContent(_ key: AccessibilityCustomContentKey, _ value: Text?, importance: AXCustomContent.Importance = .default) -> ModifiedContent<Content, Modifier> { fatalError() }

/// Add additional accessibility information to the view.
///
/// Use this method to add information you want accessibility users to be
/// able to access about this element, beyond the basics of label, value,
/// and hint. For example, `accessibilityCustomContent` can be used to add
/// information about the orientation of a photograph, or the number of
/// people found in the picture.
///
/// - Parameter key: Key used to specify the identifier and label of the
///   of the additional accessibility information entry.
/// - Parameter valueKey: Text value for the additional accessibility
///   information. For example: "landscape." A value of `nil` will remove
///   any entry of additional information added earlier for any `key` with
///   the same identifier.
/// - Parameter importance: Importance of the accessibility information.
///   High-importance information gets read out immediately, while
///   default-importance information must be explicitly asked for by the
///   user.
/// - Note: Repeated calls of `accessibilityCustomContent` with `key`s
///   having different identifiers will create new entries of
///   additional information.
///   Calling `accessibilityAdditionalContent` repeatedly with `key`s
///   having matching identifiers will replace the previous entry.
public func accessibilityCustomContent(_ key: AccessibilityCustomContentKey, _ valueKey: LocalizedStringKey, importance: AXCustomContent.Importance = .default) -> ModifiedContent<Content, Modifier> { fatalError() }

/// Add additional accessibility information to the view.
///
/// Use this method to add information you want accessibility users to be
/// able to access about this element, beyond the basics of label, value,
/// and hint. For example, `accessibilityCustomContent` can be used to add
/// information about the orientation of a photograph, or the number of
/// people found in the picture.
///
/// - Parameter key: Key used to specify the identifier and label of the
///   of the additional accessibility information entry.
/// - Parameter value: Text value for the additional accessibility
///   information. For example: "landscape." A value of `nil` will remove
///   any entry of additional information added earlier for any `key` with
///   the same identifier.
/// - Parameter importance: Importance of the accessibility information.
///   High-importance information gets read out immediately, while
///   default-importance information must be explicitly asked for by the
///   user.
/// - Note: Repeated calls of `accessibilityCustomContent` with `key`s
///   having different identifiers will create new entries of
///   additional information.
///   Calling `accessibilityAdditionalContent` repeatedly with `key`s
///   having matching identifiers will replace the previous entry.
public func accessibilityCustomContent<V>(_ key: AccessibilityCustomContentKey, _ value: V, importance: AXCustomContent.Importance = .default) -> ModifiedContent<Content, Modifier> where V : StringProtocol { fatalError() }

/// Add additional accessibility information to the view.
///
/// Use this method to add information you want accessibility users to be
/// able to access about this element, beyond the basics of label, value,
/// and hint. For example: `accessibilityCustomContent` can be used to add
/// information about the orientation of a photograph, or the number of
/// people found in the picture.
///
/// - Parameter label: Localized text describing to the user what
///   is contained in this additional information entry. For example:
///   "orientation".
/// - Parameter value: Text value for the additional accessibility
///   information. For example: "landscape."
/// - Parameter importance: Importance of the accessibility information.
///   High-importance information gets read out immediately, while
///   default-importance information must be explicitly asked for by the
///   user.
/// - Note: Repeated calls of `accessibilityCustomContent` with different
///   labels will create new entries of additional information. Calling
///   `accessibilityAdditionalContent` repeatedly with the same label will
///   instead replace the previous value and importance.
public func accessibilityCustomContent(_ label: Text, _ value: Text, importance: AXCustomContent.Importance = .default) -> ModifiedContent<Content, Modifier> { fatalError() }

/// Add additional accessibility information to the view.
///
/// Use this method to add information you want accessibility users to be
/// able to access about this element, beyond the basics of label, value,
/// and hint. For example: `accessibilityCustomContent` can be used to add
/// information about the orientation of a photograph, or the number of
/// people found in the picture.
///
/// - Parameter label: Localized text describing to the user what
///   is contained in this additional information entry. For example:
///   "orientation".
/// - Parameter value: Text value for the additional accessibility
///   information. For example: "landscape."
/// - Parameter importance: Importance of the accessibility information.
///   High-importance information gets read out immediately, while
///   default-importance information must be explicitly asked for by the
///   user.
/// - Note: Repeated calls of `accessibilityCustomContent` with different
///   labels will create new entries of additional information. Calling
///   `accessibilityAdditionalContent` repeatedly with the same label will
///   instead replace the previous value and importance.
public func accessibilityCustomContent(_ labelKey: LocalizedStringKey, _ value: Text, importance: AXCustomContent.Importance = .default) -> ModifiedContent<Content, Modifier> { fatalError() }

/// Add additional accessibility information to the view.
///
/// Use this method to add information you want accessibility users to be
/// able to access about this element, beyond the basics of label, value,
/// and hint. For example, `accessibilityCustomContent` can be used to add
/// information about the orientation of a photograph, or the number of
/// people found in the picture.
///
/// - Parameter labelKey: Localized text describing to the user what
///   is contained in this additional information entry. For example:
///   "orientation".
/// - Parameter value: Text value for the additional accessibility
///   information. For example: "landscape."
/// - Parameter importance: Importance of the accessibility information.
///   High-importance information gets read out immediately, while
///   default-importance information must be explicitly asked for by the
///   user.
/// - Note: Repeated calls of `accessibilityCustomContent` with different
///   labels will create new entries of additional information. Calling
///   `accessibilityAdditionalContent` repeatedly with the same label will
///   instead replace the previous value and importance.
public func accessibilityCustomContent(_ labelKey: LocalizedStringKey, _ valueKey: LocalizedStringKey, importance: AXCustomContent.Importance = .default) -> ModifiedContent<Content, Modifier> { fatalError() }

/// Add additional accessibility information to the view.
///
/// Use this method to add information you want accessibility users to be
/// able to access about this element, beyond the basics of label, value,
/// and hint. For example, `accessibilityCustomContent` can be used to add
/// information about the orientation of a photograph, or the number of
/// people found in the picture.
///
/// - Parameter labelKey: Localized text describing to the user what
///   is contained in this additional information entry. For example:
///   "orientation".
/// - Parameter value: Text value for the additional accessibility
///   information. For example: "landscape."
/// - Parameter importance: Importance of the accessibility information.
///   High-importance information gets read out immediately, while
///   default-importance information must be explicitly asked for by the
///   user.
/// - Note: Repeated calls of `accessibilityCustomContent` with different
///   labels will create new entries of additional information. Calling
///   `accessibilityAdditionalContent` repeatedly with the same label will
///   instead replace the previous value and importance.
public func accessibilityCustomContent<V>(_ labelKey: LocalizedStringKey, _ value: V, importance: AXCustomContent.Importance = .default) -> ModifiedContent<Content, Modifier> where V : StringProtocol { fatalError() }
}
*/
