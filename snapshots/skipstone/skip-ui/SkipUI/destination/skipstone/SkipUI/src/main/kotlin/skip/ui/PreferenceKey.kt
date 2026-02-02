package skip.ui

import kotlin.reflect.KClass
import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import kotlin.reflect.full.companionObjectInstance
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

interface PreferenceKey<Value> {
}

/// Added to `PreferenceKey` companion objects.
interface PreferenceKeyCompanion<Value> {
    val defaultValue: Value
    fun reduce(value: InOut<Value>, nextValue: () -> Value)
}

/// Internal analog to `EnvironmentValues` for preferences.
///
/// Uses environment `CompositionLocals` internally.
///
/// - Seealso: `EnvironmentValues`
internal class PreferenceValues {

    /// Return a preference collector for the given `PreferenceKey` type.
    @Composable
    internal fun collector(key: Any): PreferenceCollector<Any?>? {
        return EnvironmentValues.shared.compositionLocals[key]?.current as? PreferenceCollector<Any?>
    }

    /// Collect the values of the given preferences while composing the given content.
    @Composable internal fun collectPreferences(collectors: Array<PreferenceCollector<*>>, in_: @Composable () -> Unit) {
        val content = in_
        val provided = collectors.map { collector ->
            var compositionLocal = EnvironmentValues.shared.compositionLocals[collector.key].sref()
            if (compositionLocal == null) {
                compositionLocal = compositionLocalOf { -> Unit }
                EnvironmentValues.shared.compositionLocals[collector.key] = compositionLocal.sref()
            }
            val element = compositionLocal!! provides collector
            element
        }
        val kprovided = (provided.kotlin(nocopy = true) as MutableList<ProvidedValue<*>>).toTypedArray()
        CompositionLocalProvider(*kprovided) { -> content() }
    }

    /// Update the value of the given preference, as if by calling .preference(key:value:).
    @Composable
    internal fun contribute(context: ComposeContext, key: Any, value: Any?) {
        // Use a saveable value because the preferences themselves and their node IDs are saved
        val id = rememberSaveable(stateSaver = context.stateSaver as Saver<Int?, Any>) { -> mutableStateOf<Int?>(null) }
        val collector = rememberUpdatedState(PreferenceValues.shared.collector(key = key))
        collector.value.sref()?.let { collector ->
            // A side effect is required to ensure that a state change during composition causes a recomposition
            SideEffect { -> id.value = collector.contribute(value, id = id.value) }
        }
        DisposableEffect(true) { ->
            onDispose { ->
                collector.value?.erase(id = id.value)
            }
        }
    }

    @androidx.annotation.Keep
    companion object {
        internal val shared = PreferenceValues()
    }
}

/// Used internally by our preferences system to collect preferences and recompose on change.
internal class PreferenceCollector<Value> {
    internal val key: Any
    internal val state: MutableState<Preference<Value>>
    internal val isErasable: Boolean

    internal constructor(key: Any, state: MutableState<Preference<Value>>, isErasable: Boolean = true) {
        this.key = key.sref()
        this.state = state.sref()
        this.isErasable = isErasable
    }

    /// Contribute a value to the collected preference.
    ///
    /// - Parameter id: The id of this value in the value chain, or nil if no id has been assigned.
    /// - Returns: The id to use for future contributions.
    internal fun contribute(value: Value, id: Int?): Int {
        var preference = state.value.sref()
        if (id == null) {
            val maxID = preference.nodes.reduce(initialResult = -1) l@{ result, node -> return@l max(result, node.id) }
            val nextID = maxID + 1
            preference.nodes.append(PreferenceNode(id = nextID, value = value))
            state.value = preference
            return nextID
        }
        val index_0 = preference.nodes.firstIndex(where = { it -> it.id == id })
        if (index_0 == null) {
            val maxID = preference.nodes.reduce(initialResult = -1) l@{ result, node -> return@l max(result, node.id) }
            val nextID = maxID + 1
            preference.nodes.append(PreferenceNode(id = nextID, value = value))
            state.value = preference
            return nextID
        }
        preference.nodes[index_0] = PreferenceNode(id = id, value = value)
        state.value = preference
        return id
    }

    /// Remove the contribution by the given id.
    internal fun erase(id: Int?) {
        if (!isErasable) {
            return
        }
        var preference = state.value.sref()
        if (id != null) {
            preference.nodes.firstIndex(where = { it -> it.id == id })?.let { index ->
                preference.nodes.remove(at = index)
                state.value = preference
            }
        }
    }
}

/// The collected preference values that are reduced to achieve the final value.
@Stable
internal class Preference<Value>: MutableStruct {
    internal var nodes: Array<PreferenceNode<Value>> = arrayOf()
        get() = field.sref({ this.nodes = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    internal constructor(key: KClass<*>, initialValue: Value? = null) {
        this.key = key
        val companion = (key.companionObjectInstance as PreferenceKeyCompanion<Value>).sref()
        this.initialValue = (initialValue ?: companion.defaultValue).sref()
        this.reducer = l@{ value, nextValue ->
            var updatedValue = value.sref()
            companion.reduce(value = InOut({ updatedValue }, { updatedValue = it }), nextValue = { -> nextValue })
            return@l updatedValue
        }
    }

    internal constructor(key: Any, initialValue: Value, reducer: (Value, Value) -> Value) {
        this.key = key.sref()
        this.initialValue = initialValue.sref()
        this.reducer = reducer
    }

    internal val key: Any
    internal val initialValue: Value
    internal val reducer: (Value, Value) -> Value

    /// The reduced preference value.
    internal val reduced: Value
        get() {
            var value = initialValue.sref()
            for (node in nodes.sref()) {
                value = reducer(value, node.value as Value)
            }
            return value
        }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as Preference<Value>
        this.nodes = copy.nodes
        this.key = copy.key
        this.initialValue = copy.initialValue
        this.reducer = copy.reducer
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = Preference<Value>(this as MutableStruct)

    override fun equals(other: Any?): Boolean {
        if (other !is Preference<*>) return false
        return nodes == other.nodes && key == other.key && initialValue == other.initialValue && reducer == other.reducer
    }
}

internal class PreferenceNode<Value> {
    internal val id: Int
    internal val value: Value

    override fun equals(other: Any?): Boolean {
        if (other !is PreferenceNode<*>) {
            return false
        }
        val lhs = this
        val rhs = other
        if (lhs.id != rhs.id) {
            return false
        }
        if ((lhs.value is Function<*>) && (rhs.value is Function<*>)) {
            return true
        }
        return lhs.value == rhs.value
    }

    constructor(id: Int, value: Value) {
        this.id = id
        this.value = value.sref()
    }
}

/*
@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension PreferenceKey where Self.Value : ExpressibleByNilLiteral {

/// Let nil-expressible values default-initialize to nil.
public static var defaultValue: Self.Value { get { fatalError() } }
}

/// A key for specifying the preferred color scheme.
///
/// Don't use this key directly. Instead, set a preferred color scheme for a
/// view using the ``View/preferredColorScheme(_:)`` view modifier. Get the
/// current color scheme for a view by accessing the
/// ``EnvironmentValues/colorScheme`` value.
@available(iOS 13.0, macOS 11.0, tvOS 13.0, watchOS 6.0, *)
public struct PreferredColorSchemeKey : PreferenceKey {

/// The type of value produced by this preference.
public typealias Value = ColorScheme?

/// Combines a sequence of values by modifying the previously-accumulated
/// value with the result of a closure that provides the next value.
///
/// This method receives its values in view-tree order. Conceptually, this
/// combines the preference value from one tree with that of its next
/// sibling.
///
/// - Parameters:
///   - value: The value accumulated through previous calls to this method.
///     The implementation should modify this value.
///   - nextValue: A closure that returns the next value in the sequence.
public static func reduce(value: inout PreferredColorSchemeKey.Value, nextValue: () -> PreferredColorSchemeKey.Value) { fatalError() }
}

@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
extension View {

/// Reads the specified preference value from the view, using it to
/// produce a second view that is applied as an overlay to the
/// original view.
///
/// The values of the preference key from both views
/// are combined and made visible to the parent view.
///
/// - Parameters:
///   - key: The preference key type whose value is to be read.
///   - alignment: An optional alignment to use when positioning the
///     overlay view relative to the original view.
///   - transform: A function that produces the overlay view from
///     the preference value read from the original view.
///
/// - Returns: A view that layers a second view in front of the view.
public func overlayPreferenceValue<K, V>(_ key: K.Type, alignment: Alignment = .center, @ViewBuilder _ transform: @escaping (K.Value) -> V) -> some View where K : PreferenceKey, V : View { return stubView() }


/// Reads the specified preference value from the view, using it to
/// produce a second view that is applied as the background of the
/// original view.
///
/// The values of the preference key from both views
/// are combined and made visible to the parent view.
///
/// - Parameters:
///   - key: The preference key type whose value is to be read.
///   - alignment: An optional alignment to use when positioning the
///     background view relative to the original view.
///   - transform: A function that produces the background view from
///     the preference value read from the original view.
///
/// - Returns: A view that layers a second view behind the view.
public func backgroundPreferenceValue<K, V>(_ key: K.Type, alignment: Alignment = .center, @ViewBuilder _ transform: @escaping (K.Value) -> V) -> some View where K : PreferenceKey, V : View { return stubView() }

}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension View {

/// Reads the specified preference value from the view, using it to
/// produce a second view that is applied as an overlay to the
/// original view.
///
/// - Parameters:
///   - key: The preference key type whose value is to be read.
///   - transform: A function that produces the overlay view from
///     the preference value read from the original view.
///
/// - Returns: A view that layers a second view in front of the view.
public func overlayPreferenceValue<Key, T>(_ key: Key.Type = Key.self, @ViewBuilder _ transform: @escaping (Key.Value) -> T) -> some View where Key : PreferenceKey, T : View { return stubView() }


/// Reads the specified preference value from the view, using it to
/// produce a second view that is applied as the background of the
/// original view.
///
/// - Parameters:
///   - key: The preference key type whose value is to be read.
///   - transform: A function that produces the background view from
///     the preference value read from the original view.
///
/// - Returns: A view that layers a second view behind the view.
public func backgroundPreferenceValue<Key, T>(_ key: Key.Type = Key.self, @ViewBuilder _ transform: @escaping (Key.Value) -> T) -> some View where Key : PreferenceKey, T : View { return stubView() }

}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension View {

/// Sets a value for the specified preference key, the value is a
/// function of the key's current value and a geometry value tied
/// to the current coordinate space, allowing readers of the value
/// to convert the geometry to their local coordinates.
///
/// - Parameters:
///   - key: the preference key type.
///   - value: the geometry value in the current coordinate space.
///   - transform: the function to produce the preference value.
///
/// - Returns: a new version of the view that writes the preference.
public func transformAnchorPreference<A, K>(key _: K.Type = K.self, value: Anchor<A>.Source, transform: @escaping (inout K.Value, Anchor<A>) -> Void) -> some View where K : PreferenceKey { return stubView() }

}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension View {

/// Sets a value for the specified preference key, the value is a
/// function of a geometry value tied to the current coordinate
/// space, allowing readers of the value to convert the geometry to
/// their local coordinates.
///
/// - Parameters:
///   - key: the preference key type.
///   - value: the geometry value in the current coordinate space.
///   - transform: the function to produce the preference value.
///
/// - Returns: a new version of the view that writes the preference.
public func anchorPreference<A, K>(key _: K.Type = K.self, value: Anchor<A>.Source, transform: @escaping (Anchor<A>) -> K.Value) -> some View where K : PreferenceKey { return stubView() }

}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension View {

/// Applies a transformation to a preference value.
public func transformPreference<K>(_ key: K.Type = K.self, _ callback: @escaping (inout K.Value) -> Void) -> some View where K : PreferenceKey { return stubView() }

}
*/
