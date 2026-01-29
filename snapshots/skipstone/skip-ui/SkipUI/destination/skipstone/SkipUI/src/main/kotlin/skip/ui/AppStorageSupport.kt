package skip.ui

import skip.lib.*

// Copyright 2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import skip.model.*
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/// Support for bridged`SwiftUI.@AppStorage`.
///
/// The Compose side manages object lifecycles, so we hold references to the value as a `MutableState` recomposition trigger.
/// We also monitor the underlying defautls key using a preferences listener.
@androidx.annotation.Keep
class AppStorageSupport: StateTracker, skip.lib.SwiftProjecting {
    private val key: String
    private val store: UserDefaults?
    private var value: Any? = null
        get() = field.sref({ this.value = it })
        set(newValue) {
            field = newValue.sref()
        }
    private val get: (UserDefaults, Any?) -> Any?
    private val set: (UserDefaults, Any?) -> Unit
    private var state: MutableState<Any?>? = null
        get() = field.sref({ this.state = it })
        set(newValue) {
            field = newValue.sref()
        }
    /// The property change listener from the UserDefaults
    private var listener: Any? = null
        get() = field.sref({ this.listener = it })
        set(newValue) {
            field = newValue.sref()
        }

    constructor(value: Boolean, key: String, store: Any?) {
        this.key = key
        this.store = store as? UserDefaults
        this.value = value
        this.get = { it, it_1 -> it_1 as? Boolean ?: it.bool(forKey = key) }
        this.set = { it, it_1 -> it.set(it_1 as? Boolean, forKey = key) }
        StateTracking.register(this)
    }

    constructor(value: Double, key: String, store: Any?) {
        this.key = key
        this.store = store as? UserDefaults
        this.value = value
        this.get = { it, it_1 -> it_1 as? Double ?: it.double(forKey = key) }
        this.set = { it, it_1 -> it.set(it_1 as? Double, forKey = key) }
        StateTracking.register(this)
    }

    constructor(value: Int, key: String, store: Any?) {
        this.key = key
        this.store = store as? UserDefaults
        this.value = value
        this.get = { it, it_1 -> it_1 as? Int ?: it.integer(forKey = key) }
        this.set = { it, it_1 -> it.set(it_1 as? Int, forKey = key) }
        StateTracking.register(this)
    }

    constructor(value: String, key: String, store: Any?) {
        this.key = key
        this.store = store as? UserDefaults
        this.value = value
        this.get = { it, it_1 -> it_1 as? String ?: it.string(forKey = key) }
        this.set = { it, it_1 -> it.set(it_1 as? String, forKey = key) }
        StateTracking.register(this)
    }

    constructor(value: URL, key: String, store: Any?) {
        this.key = key
        this.store = store as? UserDefaults
        this.value = value
        this.get = { it, it_1 -> it_1 as? URL ?: it.url(forKey = key) }
        this.set = { it, it_1 -> it.set(it_1 as? URL, forKey = key) }
        StateTracking.register(this)
    }

    constructor(value: Data, key: String, store: Any?) {
        this.key = key
        this.store = store as? UserDefaults
        this.value = value
        this.get = { it, it_1 -> it_1 as? Data ?: it.data(forKey = key) }
        this.set = { it, it_1 -> it.set(it_1 as? Data, forKey = key) }
        StateTracking.register(this)
    }

    var boolValue: Boolean?
        get() {
            state.sref()?.let { state ->
                return state.value as? Boolean
            }
            return value as? Boolean
        }
        set(newValue) {
            set(currentStore, newValue)
            if (listener == null) {
                value = newValue
            }
        }

    var doubleValue: Double?
        get() {
            state.sref()?.let { state ->
                return state.value as? Double
            }
            return value as? Double
        }
        set(newValue) {
            set(currentStore, newValue)
            if (listener == null) {
                value = newValue
            }
        }

    var intValue: Int?
        get() {
            state.sref()?.let { state ->
                return state.value as? Int
            }
            return value as? Int
        }
        set(newValue) {
            set(currentStore, newValue)
            if (listener == null) {
                value = newValue
            }
        }

    var stringValue: String?
        get() {
            state.sref()?.let { state ->
                return state.value as? String
            }
            return value as? String
        }
        set(newValue) {
            set(currentStore, newValue)
            if (listener == null) {
                value = newValue
            }
        }

    var urlValue: URL?
        get() {
            state.sref()?.let { state ->
                return (state.value as? URL).sref({ this.urlValue = it })
            }
            return (value as? URL).sref({ this.urlValue = it })
        }
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            set(currentStore, newValue)
            if (listener == null) {
                value = newValue
            }
        }

    var dataValue: Data?
        get() {
            state.sref()?.let { state ->
                return (state.value as? Data).sref({ this.dataValue = it })
            }
            return (value as? Data).sref({ this.dataValue = it })
        }
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            set(currentStore, newValue)
            if (listener == null) {
                value = newValue
            }
        }

    /// The current active store
    private val currentStore: UserDefaults
        get() {
            // TODO: handle Scene.defaultAppStorage() and View.defaultAppStorage() by storing it in the environment
            return store ?: UserDefaults.standard
        }

    override fun trackState() {
        // Create our Compose-trackable backing state and keep it in sync with the store. Note that we have to seed the store with a value
        // for the key in order for our listener to work
        val store = this.currentStore
        val object_ = store.object_(forKey = key)
        if (object_ != null) {
            value = get(store, object_)
        } else {
            value.sref()?.let { value ->
                set(store, value)
            }
        }
        state = mutableStateOf(value)

        // Caution: The preference manager does not currently store a strong reference to the listener. You must store a strong reference to the listener, or it will be susceptible to garbage collection. We recommend you keep a reference to the listener in the instance data of an object that will exist as long as you need the listener.
        // https://developer.android.com/reference/android/content/SharedPreferences.html#registerOnSharedPreferenceChangeListener(android.content.SharedPreferences.OnSharedPreferenceChangeListener)
        this.listener = store.registerOnSharedPreferenceChangeListener(key = key) { ->
            store.object_(forKey = key)?.let { obj ->
                value = get(store, obj)
                state?.value = value
            }
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

