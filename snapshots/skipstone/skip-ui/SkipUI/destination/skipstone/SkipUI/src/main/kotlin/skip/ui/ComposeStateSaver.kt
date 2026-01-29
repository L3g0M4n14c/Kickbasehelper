package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope

/// Use to make a Bundle-saveable string from a SwiftUI value.
///
/// We typically use a `ComposeStateSaver` to save state, but when working with Compose internal state like `LazyList` state, use this function
/// to turn user-supplied values into strings that Compose can save natively.
///
/// - Seealso: `SkipSwiftUI.Java_composeBundleString(for:)`
internal fun composeBundleString(for_: Any?): String {
    val value = for_
    val matchtarget_0 = value as? Identifiable<*>
    if (matchtarget_0 != null) {
        val identifiable = matchtarget_0
        return String(describing = identifiable.id)
    } else {
        val matchtarget_1 = value as? RawRepresentable<*>
        if (matchtarget_1 != null) {
            val rawRepresentable = matchtarget_1
            return String(describing = rawRepresentable.rawValue)
        } else {
            return String(describing = value)
        }
    }
}

/// Used in conjunction with `rememberSaveable` to save and restore state with SwiftUI-like behavior.
internal class ComposeStateSaver: Saver<Any?, Any> {
    private val state: MutableMap<ComposeStateSaver.Key, Any> = mutableMapOf()

    override fun restore(value: Any): Any? {
        if (value == Companion.nilMarker) {
            return null
        } else {
            val matchtarget_2 = value as? ComposeStateSaver.Key
            if (matchtarget_2 != null) {
                val key = matchtarget_2
                return state[key].sref()
            } else {
                return value.sref()
            }
        }
    }

    override fun SaverScope.save(value: Any?): Any {
        if (value == null) {
            return Companion.nilMarker
        } else if (value is Boolean || value is Number || value is String || value is Char) {
            return value.sref()
        } else {
            val key = Key.next()
            state[key] = value.sref()
            return key.sref()
        }
    }

    /// Key under which to save values that cannot be stored directly in the Bundle.
    internal class Key: Parcelable {
        private val value: Int

        internal constructor(value: Int) {
            this.value = value
        }

        internal constructor(parcel: Parcel): this(parcel.readInt()) {
        }

        override fun writeToParcel(parcel: Parcel, flags: Int): Unit = parcel.writeInt(value)

        override fun describeContents(): Int = 0

        // We must use a companion CREATOR to meet the Java Parcelable contract. Note that if this code breaks, it may have no
        // immediate noticable effect. However, we've had dev reports that it can cause crashes on a high percentage of user
        // devices, even though we don't know how to exercise it. We can manually test for contract compatibility with:
        /*
        val key = ComposeStateSaver.Key(99999)
        val bundle = android.os.Bundle()
        bundle.putParcelable(key::class.java.name, key)
        val parcel = Parcel.obtain()
        parcel.writeBundle(bundle)
        val bytes = parcel.marshall()
        val rparcel = Parcel.obtain()
        rparcel.unmarshall(bytes, 0, bytes.size)
        rparcel.setDataPosition(0)
        val rbundle = rparcel.readBundle()
        rbundle?.classLoader = key::class.java.classLoader
        val rkey: ComposeStateSaver.Key? = rbundle?.getParcelable(key::class.java.name)
        android.util.Log.e("", "Roundtripped: $rkey")
        */

        companion object CREATOR: Parcelable.Creator<Key> {
            private var keyValue = 0

            internal fun next(): ComposeStateSaver.Key {
                keyValue += 1
                return Key(value = keyValue)
            }

            override fun createFromParcel(parcel: Parcel): ComposeStateSaver.Key = Key(parcel)

            override fun newArray(size: Int): kotlin.Array<ComposeStateSaver.Key?> = arrayOfNulls(size)
        }
    }

    @androidx.annotation.Keep
    companion object {
        private val nilMarker = "__SkipUI.ComposeStateSaver.nilMarker"
    }
}
