package skip.ui

import skip.lib.*
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.registerForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.lang.ref.WeakReference

internal val logger: SkipLogger = SkipLogger(subsystem = "skip.ui", category = "SkipUI") // adb logcat '*:S' 'skip.ui.SkipUI:V'

/* @MainActor */
@androidx.annotation.Keep
open class UIApplication: skip.lib.SwiftProjecting {
    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null
        get() = field.sref({ this.requestPermissionLauncher = it })
        set(newValue) {
            field = newValue.sref()
        }
    private val waitingContinuations: MutableList<Continuation<Boolean>> = mutableListOf<Continuation<Boolean>>()

    private constructor() {
        suppresssideeffects = true
        try {
            val lifecycle = ProcessLifecycleOwner.get().lifecycle.sref()
            lifecycle.addObserver(UIApplicationLifecycleEventObserver(application = this))
        } finally {
            suppresssideeffects = false
        }
    }

    /// The Android main activity.
    ///
    /// This API mirrors `ProcessInfo.androidContext` for the application context.
    ///
    open var androidActivity: androidx.activity.ComponentActivity?
        get() {
            val activity = androidActivityReference?.get()
            return (if (activity?.isDestroyed == false) activity else null).sref({ this.androidActivity = it })
        }
        internal set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            if (newValue != null) {
                androidActivityReference = WeakReference(newValue)
            } else {
                androidActivityReference = null
            }
            if (isIdleTimerDisabled) {
                setWindowFlagsForIsIdleTimerDisabled()
            }
        }
    private var androidActivityReference: WeakReference<androidx.activity.ComponentActivity>? = null
        get() = field.sref({ this.androidActivityReference = it })
        set(newValue) {
            field = newValue.sref()
        }

    internal open fun onActivityDestroy() {
        // The permission launcher appears to hold a strong reference to the activity, so we must nil it to avoid memory leaks
        this.requestPermissionLauncher = null
    }

    /// Requests the given permission.
    /// - Parameters:
    ///   - permission: the name of the permission, such as `android.permission.POST_NOTIFICATIONS`
    ///   - showRationale: an optional async callback to invoke when the system determies that a rationale should be displayed for the permission check
    /// - Returns: true if the permission was granted, false if denied or there was an error making the request
    open suspend fun requestPermission(permission: String, showRationale: (suspend () -> Boolean)?): Boolean = Async.run l@{
        logger.info("requestPermission: ${permission}")
        val activity_0 = this.androidActivity.sref()
        if (activity_0 == null) {
            return@l false
        }
        if (ContextCompat.checkSelfPermission(activity_0, permission) == PackageManager.PERMISSION_GRANTED) {
            return@l true // already granted
        }
        val requestPermissionLauncher_0 = requestPermissionLauncher.sref()
        if (requestPermissionLauncher_0 == null) {
            logger.warning("requestPermission: ${permission} requestPermissionLauncher is nil")
            return@l false
        }
        // check if we are expected to show a rationalle for the permission request, and if so,
        // and if we have a `showRationale` callback, then wait for the result
        if ((showRationale != null) && (ActivityCompat.shouldShowRequestPermissionRationale(activity_0, permission) == true)) {
            if (showRationale() == false) {
                return@l false
            }
        }
        suspendCoroutine { continuation ->
            var count = 0
            synchronized(waitingContinuations) { ->
                waitingContinuations.add(continuation)
                count = waitingContinuations.count()
            }
            if (count == 1) {
                logger.info("launch requestPermission: ${permission}")
                requestPermissionLauncher_0?.launch(permission)
            }
        }
    }

    /// Requests the given permission.
    /// - Parameters:
    ///   - permission: the name of the permission, such as `android.permission.POST_NOTIFICATIONS`
    /// - Returns: true if the permission was granted, false if denied or there was an error making the request
    open suspend fun requestPermission(permission: String): Boolean = Async.run l@{
        // We can't bridge the `showRationale` parameter async closure
        return@l requestPermission(permission, showRationale = null)
    }
    fun callback_requestPermission(permission: String, f_return_callback: (Boolean) -> Unit) {
        Task {
            f_return_callback(requestPermission(permission = permission))
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open var delegate: Any?
        get() {
            fatalError()
        }
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
        }

    open var isIdleTimerDisabled = false
        set(newValue) {
            field = newValue
            if (!suppresssideeffects) {
                setWindowFlagsForIsIdleTimerDisabled()
            }
        }

    private fun setWindowFlagsForIsIdleTimerDisabled() {
        val flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.sref()
        if (isIdleTimerDisabled) {
            androidActivity?.window?.addFlags(flags)
        } else {
            androidActivity?.window?.clearFlags(flags)
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun canOpenURL(url: URL): Boolean {
        fatalError()
    }


    open suspend fun open(url: URL, options: Dictionary<UIApplication.OpenExternalURLOptionsKey, Any> = dictionaryOf()): Boolean = Async.run l@{
        val context = ProcessInfo.processInfo.androidContext.sref()
        try {
            val intent: Intent
            val uri = android.net.Uri.parse(url.absoluteString)
            // adding the Android-specific URL key "intent" will use the custom intent name
            val matchtarget_0 = options[OpenExternalURLOptionsKey.intent] as? String
            if (matchtarget_0 != null) {
                val intentName = matchtarget_0
                intent = Intent(intentName, uri)
            } else if (url.scheme == "intent") {
                val action = url.host()
                // ACTION_APP_NOTIFICATION_SETTINGS requires the package name as an extra, not in the data URI
                if (action == Settings.ACTION_APP_NOTIFICATION_SETTINGS) {
                    intent = Intent(action)
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
                } else {
                    intent = Intent(action, android.net.Uri.parse("package:" + context.getPackageName()))
                }
            } else if (url.scheme == "tel") {
                intent = Intent(Intent.ACTION_DIAL, uri)
            } else if (url.scheme == "sms" || url.scheme == "mailto") {
                intent = Intent(Intent.ACTION_SENDTO, uri)
            } else {
                intent = Intent(Intent.ACTION_VIEW, uri)
            }
            for ((key, value) in options.sref()) {
                if (key.rawValue == OpenExternalURLOptionsKey.intent.rawValue) {
                    continue
                }
                (value as? String)?.let { valueString ->
                    intent.putExtra(key.rawValue, valueString)
                }
            }
            val matchtarget_1 = androidActivity
            if (matchtarget_1 != null) {
                val androidActivity = matchtarget_1
                androidActivity.startActivity(intent)
            } else {
                // needed or else: android.util.AndroidRuntimeException: Calling startActivity() from outside of an Activity context requires the FLAG_ACTIVITY_NEW_TASK flag. Is this really what you want?
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            return@l true
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            logger.warning("UIApplication.launch error: ${error}")
            return@l false
        }
    }

    open suspend fun bridgedOpen(url: URL, options: Dictionary<String, Any>): Boolean = Async.run l@{
        val keyedOptions = options.reduce(into = Dictionary<OpenExternalURLOptionsKey, Any>()) { result, entry -> result.value[OpenExternalURLOptionsKey(rawValue = entry.key)] = entry.value }
        return@l open(url, options = keyedOptions)
    }
    fun callback_bridgedOpen(url: URL, options: Dictionary<String, Any>, f_return_callback: (Boolean) -> Unit) {
        Task {
            f_return_callback(bridgedOpen(url = url, options = options))
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun sendEvent(event: Any) = Unit
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun sendAction(action: Any, to: Any?, from: Any?, for_: Any?): Boolean {
        val target = to
        val sender = from
        val event = for_
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun supportedInterfaceOrientations(for_: Any?): Any {
        val window = for_
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open var applicationSupportsShakeToEdit: Boolean
        get() {
            fatalError()
        }
        set(newValue) {
        }

    open var applicationState: UIApplication.State
        get() = _applicationState.value
        internal set(newValue) {
            _applicationState.value = newValue
        }
    private val _applicationState: MutableState<UIApplication.State> = mutableStateOf(UIApplication.State.active)

    open val bridgedApplicationState: Int
        get() = applicationState.rawValue

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val backgroundTimeRemaining: Double
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun beginBackgroundTask(expirationHandler: (() -> Unit)? = null): Any {
        val handler = expirationHandler
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun beginBackgroundTask(withName: String?, expirationHandler: (() -> Unit)? = null): Any {
        val taskName = withName
        val handler = expirationHandler
        fatalError()
    }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun endBackgroundTask(identifier: Any) = Unit
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val backgroundRefreshStatus: Any
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val isProtectedDataAvailable: Boolean
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val userInterfaceLayoutDirection: Any
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val preferredContentSizeCategory: Any
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val connectedScenes: Set<AnyHashable>
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val openSessions: Set<AnyHashable>
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val supportsMultipleScenes: Boolean
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun requestSceneSessionDestruction(sceneSession: Any, options: Any?, errorHandler: ((Error) -> Unit)? = null) = Unit
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun requestSceneSessionRefresh(sceneSession: Any) = Unit
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun activateSceneSession(for_: Any, errorHandler: ((Error) -> Unit)? = null) = Unit
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun registerForRemoteNotifications() = Unit
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun unregisterForRemoteNotifications() = Unit
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val isRegisteredForRemoteNotifications: Boolean
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun beginReceivingRemoteControlEvents() = Unit
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun endReceivingRemoteControlEvents() = Unit
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val shortcutItems: Any?
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val supportsAlternateIcons: Boolean
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun setAlternateIconName(alternateIconName: String?, completionHandler: ((Error?) -> Unit)? = null) = Unit
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open suspend fun setAlternateIconName(alternateIconName: String?): Unit = Unit
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val alternateIconName: String?
        get() {
            fatalError()
        }
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun extendStateRestoration() = Unit
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun completeStateRestoration() = Unit
    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open fun ignoreSnapshotOnNextApplicationLaunch() = Unit

    // NOTE: Keep in sync with SkipSwiftUI.UIApplication.State
    enum class State(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
        active(0),
        inactive(1),
        background(2);

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: Int): UIApplication.State? {
                return when (rawValue) {
                    0 -> State.active
                    1 -> State.inactive
                    2 -> State.background
                    else -> null
                }
            }
        }
    }

    class OpenExternalURLOptionsKey: RawRepresentable<String> {
        override val rawValue: String
        constructor(rawValue: String) {
            this.rawValue = rawValue
        }

        override fun equals(other: Any?): Boolean {
            if (other !is UIApplication.OpenExternalURLOptionsKey) return false
            return rawValue == other.rawValue
        }

        override fun hashCode(): Int {
            var result = 1
            result = Hasher.combine(result, rawValue)
            return result
        }

        @androidx.annotation.Keep
        companion object {

            val universalLinksOnly = OpenExternalURLOptionsKey(rawValue = "universalLinksOnly")
            val eventAttribution = OpenExternalURLOptionsKey(rawValue = "eventAttribution")

            // Android-specific keys
            val intent = OpenExternalURLOptionsKey(rawValue = "intent")
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    private var suppresssideeffects = false

    @androidx.annotation.Keep
    companion object: CompanionClass() {
        override val shared = UIApplication()

        /// Setup the Android main activity.
        ///
        /// This API mirrors `ProcessInfo.launch` for the application context.
        override fun launch(activity: androidx.activity.ComponentActivity) {
            if (activity !== shared.androidActivity) {
                shared.androidActivity = activity

                // Must registerForActivityResult on or before Activity.onCreate
                try {
                    val contract = ActivityResultContracts.RequestPermission()
                    shared.requestPermissionLauncher = activity.registerForActivityResult(contract) { isGranted ->
                        var continuations: ArrayList<Continuation<Boolean>>? = null
                        synchronized(shared.waitingContinuations) { ->
                            continuations = ArrayList(shared.waitingContinuations)
                            shared.waitingContinuations.clear()
                        }
                        continuations?.forEach { it -> it.resume(isGranted) }
                    }
                    logger.info("requestPermissionLauncher: ${shared.requestPermissionLauncher}")
                } catch (error: Throwable) {
                    @Suppress("NAME_SHADOWING") val error = error.aserror()
                    android.util.Log.w("SkipUI", "error initializing permission launcher", error as? Throwable)
                }
            }
        }
        override val openSettingsURLString = "intent://" + Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        override val openDefaultApplicationsSettingsURLString = "intent://android.settings.APP_OPEN_BY_DEFAULT_SETTINGS" // ACTION_APP_OPEN_BY_DEFAULT_SETTINGS added in API 31
        override val openNotificationSettingsURLString = "intent://" + Settings.ACTION_APP_NOTIFICATION_SETTINGS
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        fun registerObject(forStateRestoration: Any, restorationIdentifier: String) = Unit
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val didEnterBackgroundNotification: Notification.Name
            get() {
                fatalError()
            }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val willEnterForegroundNotification: Notification.Name
            get() {
                fatalError()
            }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val didFinishLaunchingNotification: Notification.Name
            get() {
                fatalError()
            }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val didBecomeActiveNotification: Notification.Name
            get() {
                fatalError()
            }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val willResignActiveNotification: Notification.Name
            get() {
                fatalError()
            }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val didReceiveMemoryWarningNotification: Notification.Name
            get() {
                fatalError()
            }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val willTerminateNotification: Notification.Name
            get() {
                fatalError()
            }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val significantTimeChangeNotification: Notification.Name
            get() {
                fatalError()
            }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val backgroundRefreshStatusDidChangeNotification: Notification.Name
            get() {
                fatalError()
            }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val protectedDataWillBecomeUnavailableNotification: Notification.Name
            get() {
                fatalError()
            }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val protectedDataDidBecomeAvailableNotification: Notification.Name
            get() {
                fatalError()
            }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val userDidTakeScreenshotNotification: Notification.Name
            get() {
                fatalError()
            }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val invalidInterfaceOrientationException: Any
            get() {
                fatalError()
            }

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val backgroundFetchIntervalMinimum: Double
            get() {
                fatalError()
            }
        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val backgroundFetchIntervalNever: Double
            get() {
                fatalError()
            }

        override fun State(rawValue: Int): UIApplication.State? = State.init(rawValue = rawValue)
    }
    open class CompanionClass {
        open val shared
            get() = UIApplication.shared
        open fun launch(activity: androidx.activity.ComponentActivity) = UIApplication.launch(activity)
        open val openSettingsURLString
            get() = UIApplication.openSettingsURLString
        open val openDefaultApplicationsSettingsURLString
            get() = UIApplication.openDefaultApplicationsSettingsURLString
        open val openNotificationSettingsURLString
            get() = UIApplication.openNotificationSettingsURLString
        open fun State(rawValue: Int): UIApplication.State? = UIApplication.State(rawValue = rawValue)
    }
}

internal class UIApplicationLifecycleEventObserver: LifecycleEventObserver, DefaultLifecycleObserver {
    internal val application: UIApplication

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        for (unusedi in 0..0) {
            when (event) {
                Lifecycle.Event.ON_CREATE -> break
                Lifecycle.Event.ON_START -> break
                Lifecycle.Event.ON_RESUME -> application.applicationState = UIApplication.State.active
                Lifecycle.Event.ON_PAUSE -> application.applicationState = UIApplication.State.inactive
                Lifecycle.Event.ON_STOP -> application.applicationState = UIApplication.State.background
                Lifecycle.Event.ON_DESTROY -> application.onActivityDestroy()
                Lifecycle.Event.ON_ANY -> break
            }
        }
    }

    constructor(application: UIApplication) {
        this.application = application
    }
}
