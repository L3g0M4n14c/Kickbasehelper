package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import skip.model.*
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

@Suppress("MUST_BE_INITIALIZED")
class AppStorage<Value>: StateTracker {
    val key: String
    val store: UserDefaults?
    private val serializer: ((Value) -> Any)?
    private val deserializer: ((Any) -> Value?)?
    /// The property change listener from the UserDefaults
    private var listener: Any? = null

    constructor(wrappedValue: Value, key: String, store: UserDefaults? = null, serializer: ((Value) -> Any)? = null, deserializer: ((Any) -> Value?)? = null) {
        this.key = key
        this.store = store
        this.serializer = serializer
        this.deserializer = deserializer
        _wrappedValue = wrappedValue
        StateTracking.register(this)
    }

    var wrappedValue: Value
        get() {
            _wrappedValueState.sref()?.let { _wrappedValueState ->
                return _wrappedValueState.value.sref({ this.wrappedValue = it })
            }
            return _wrappedValue.sref({ this.wrappedValue = it })
        }
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            // Changing the store value should trigger our listener to update our own state
            val matchtarget_0 = serializer
            if (matchtarget_0 != null) {
                val serializer = matchtarget_0
                currentStore.set(serializer(newValue), forKey = key)
            } else {
                currentStore.set(newValue, forKey = key)
            }
            // Unless we haven't started tracking
            if (listener == null) {
                _wrappedValue = newValue
            }
        }
    private var _wrappedValue: Value
        get() = field.sref({ this._wrappedValue = it })
        set(newValue) {
            field = newValue.sref()
        }
    private var _wrappedValueState: MutableState<Value>? = null
        get() = field.sref({ this._wrappedValueState = it })
        set(newValue) {
            field = newValue.sref()
        }

    val projectedValue: Binding<Value>
        get() {
            return Binding(get = { -> this.wrappedValue }, set = { it -> this.wrappedValue = it })
        }

    override fun trackState() {
        // Create our Compose-trackable backing state and keep it in sync with the store. Note that we have to seed the store with a value
        // for the key in order for our listener to work
        val store = this.currentStore
        val object_ = store.object_(forKey = key)
        val value: Value?
        if (object_ != null) {
            val matchtarget_1 = deserializer
            if (matchtarget_1 != null) {
                val deserializer = matchtarget_1
                value = deserializer(object_)
            } else {
                value = (object_ as? Value).sref()
            }
        } else {
            value = (object_ as? Value).sref()
        }
        if (value != null) {
            _wrappedValue = value
        } else {
            val matchtarget_2 = serializer
            if (matchtarget_2 != null) {
                val serializer = matchtarget_2
                store.set(serializer(_wrappedValue), forKey = key)
            } else {
                store.set(_wrappedValue, forKey = key)
            }
        }
        _wrappedValueState = mutableStateOf(_wrappedValue)

        // Caution: The preference manager does not currently store a strong reference to the listener. You must store a strong reference to the listener, or it will be susceptible to garbage collection. We recommend you keep a reference to the listener in the instance data of an object that will exist as long as you need the listener.
        // https://developer.android.com/reference/android/content/SharedPreferences.html#registerOnSharedPreferenceChangeListener(android.content.SharedPreferences.OnSharedPreferenceChangeListener)
        this.listener = store.registerOnSharedPreferenceChangeListener(key = key) { ->
            val object_ = store.object_(forKey = key)
            val value: Value?
            if (object_ != null) {
                val matchtarget_3 = deserializer
                if (matchtarget_3 != null) {
                    val deserializer = matchtarget_3
                    value = deserializer(object_)
                } else {
                    value = (object_ as? Value).sref()
                }
            } else {
                value = (object_ as? Value).sref()
            }
            if (value != null) {
                _wrappedValue = value
                _wrappedValueState?.value = value
            }
        }
    }

    /// The current active store
    private val currentStore: UserDefaults
        get() {
            // TODO: handle Scene.defaultAppStorage() and View.defaultAppStorage() by storing it in the environment
            return store ?: UserDefaults.standard
        }

    @androidx.annotation.Keep
    companion object {
    }
}

/*
@available(iOS 17.0, macOS 14.0, *)
@available(tvOS, unavailable)
@available(watchOS, unavailable)
extension AppStorage {

/// Creates a property that can save and restore table column state.
///
/// Table column state is typically not bound from a table directly to
/// `AppStorage`, but instead indirecting through `State` or `SceneStorage`,
/// and using the app storage value as its initial value kept up to date
/// on changes to the direcr backing.
///
/// - Parameters:
///   - wrappedValue: The default value if table column state is not
///   available for the given key.
///   - key: The key to read and write the value to in the user defaults
///     store.
///   - store: The user defaults store to read and write to. A value
///     of `nil` will use the user default store from the environment.
public convenience init<RowValue>(wrappedValue: Value = TableColumnCustomization<RowValue>(), _ key: String, store: UserDefaults? = nil) where Value == TableColumnCustomization<RowValue>, RowValue : Identifiable { fatalError() }
}

@available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
extension AppStorage where Value : ExpressibleByNilLiteral {

/// Creates a property that can read and write an Optional boolean user
/// default.
///
/// Defaults to nil if there is no restored value.
///
/// - Parameters:
///   - key: The key to read and write the value to in the user defaults
///     store.
///   - store: The user defaults store to read and write to. A value
///     of `nil` will use the user default store from the environment.
public convenience init(_ key: String, store: UserDefaults? = nil) where Value == Bool? { fatalError() }

/// Creates a property that can read and write an Optional integer user
/// default.
///
/// Defaults to nil if there is no restored value.
///
/// - Parameters:
///   - key: The key to read and write the value to in the user defaults
///     store.
///   - store: The user defaults store to read and write to. A value
///     of `nil` will use the user default store from the environment.
public convenience init(_ key: String, store: UserDefaults? = nil) where Value == Int? { fatalError() }

/// Creates a property that can read and write an Optional double user
/// default.
///
/// Defaults to nil if there is no restored value.
///
/// - Parameters:
///   - key: The key to read and write the value to in the user defaults
///     store.
///   - store: The user defaults store to read and write to. A value
///     of `nil` will use the user default store from the environment.
public convenience init(_ key: String, store: UserDefaults? = nil) where Value == Double? { fatalError() }

/// Creates a property that can read and write an Optional string user
/// default.
///
/// Defaults to nil if there is no restored value.
///
/// - Parameters:
///   - key: The key to read and write the value to in the user defaults
///     store.
///   - store: The user defaults store to read and write to. A value
///     of `nil` will use the user default store from the environment.
public convenience init(_ key: String, store: UserDefaults? = nil) where Value == String? { fatalError() }

/// Creates a property that can read and write an Optional URL user
/// default.
///
/// Defaults to nil if there is no restored value.
///
/// - Parameters:
///   - key: The key to read and write the value to in the user defaults
///     store.
///   - store: The user defaults store to read and write to. A value
///     of `nil` will use the user default store from the environment.
public convenience init(_ key: String, store: UserDefaults? = nil) where Value == URL? { fatalError() }

/// Creates a property that can read and write an Optional data user
/// default.
///
/// Defaults to nil if there is no restored value.
///
/// - Parameters:
///   - key: The key to read and write the value to in the user defaults
///     store.
///   - store: The user defaults store to read and write to. A value
///     of `nil` will use the user default store from the environment.
public convenience init(_ key: String, store: UserDefaults? = nil) where Value == Data? { fatalError() }
}

@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
extension AppStorage {

/// Creates a property that can save and restore an Optional string,
/// transforming it to an Optional `RawRepresentable` data type.
///
/// Defaults to nil if there is no restored value
///
/// A common usage is with enumerations:
///
///     enum MyEnum: String {
///         case a
///         case b
///         case c
///     }
///     struct MyView: View {
///         @AppStorage("MyEnumValue") private var value: MyEnum?
///         var body: some View { ... }
///     }
///
/// - Parameters:
///   - key: The key to read and write the value to in the user defaults
///     store.
///   - store: The user defaults store to read and write to. A value
///     of `nil` will use the user default store from the environment.
public convenience init<R>(_ key: String, store: UserDefaults? = nil) where Value == R?, R : RawRepresentable, R.RawValue == String { fatalError() }

/// Creates a property that can save and restore an Optional integer,
/// transforming it to an Optional `RawRepresentable` data type.
///
/// Defaults to nil if there is no restored value
///
/// A common usage is with enumerations:
///
///     enum MyEnum: Int {
///         case a
///         case b
///         case c
///     }
///     struct MyView: View {
///         @AppStorage("MyEnumValue") private var value: MyEnum?
///         var body: some View { ... }
///     }
///
/// - Parameters:
///   - key: The key to read and write the value to in the user defaults
///     store.
///   - store: The user defaults store to read and write to. A value
///     of `nil` will use the user default store from the environment.
public convenience init<R>(_ key: String, store: UserDefaults? = nil) where Value == R?, R : RawRepresentable, R.RawValue == Int { fatalError() }
}
*/
