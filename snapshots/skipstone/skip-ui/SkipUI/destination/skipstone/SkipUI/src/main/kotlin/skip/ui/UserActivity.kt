package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer

internal class OnNewIntentListener: Consumer<Intent> {
    internal val newIntent: MutableState<Intent?>

    override fun accept(value: Intent) {
        newIntent.value = value
    }

    constructor(newIntent: MutableState<Intent?>) {
        this.newIntent = newIntent.sref()
    }
}

/*
#if canImport(Foundation)
import class Foundation.NSUserActivity

@available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
extension NSUserActivity {

//    /// Error types when getting/setting typed payload
//    @available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
//    public enum TypedPayloadError : Error {
//
//        /// UserInfo is empty or invalid
//        case invalidContent
//
//        /// Content failed to encode into a valid Dictionary
//        case encodingError
//    //    }

/// Given a Codable Swift type, return an instance decoded from the
/// NSUserActivity's userInfo dictionary
///
/// - Parameter type: the instance type to be decoded from userInfo
/// - Returns: the type safe instance or raises if it can't be decoded
public func typedPayload<T>(_ type: T.Type) throws -> T where T : Decodable, T : Encodable { fatalError() }

/// Given an instance of a Codable Swift type, encode it into the
/// NSUserActivity's userInfo dictionary
///
/// - Parameter payload: the instance to be converted to userInfo
public func setTypedPayload<T>(_ payload: T) throws where T : Decodable, T : Encodable { fatalError() }
}
#endif
*/
