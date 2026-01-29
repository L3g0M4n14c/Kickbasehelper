package skip.ui

import skip.lib.*
import skip.lib.Array
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.random.Random

@androidx.annotation.Keep
class UNUserNotificationCenter: skip.lib.SwiftProjecting {

    private constructor() {
    }

    suspend fun notificationSettings(): UNNotificationSettings = Async.run l@{
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return@l UNNotificationSettings(authorizationStatus = UNAuthorizationStatus.authorized)
        }
        val status: UNAuthorizationStatus
        val matchtarget_0 = UIApplication.shared.androidActivity
        if (matchtarget_0 != null) {
            val activity = matchtarget_0
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                status = UNAuthorizationStatus.authorized
            } else if (UserDefaults.standard.bool(forKey = "UNNotificationPermissionDenied")) {
                status = UNAuthorizationStatus.denied
            } else {
                status = UNAuthorizationStatus.notDetermined
            }
        } else if (UserDefaults.standard.bool(forKey = "UNNotificationPermissionDenied")) {
            status = UNAuthorizationStatus.denied
        } else {
            status = UNAuthorizationStatus.notDetermined
        }
        return@l UNNotificationSettings(authorizationStatus = status)
    }
    fun callback_notificationSettings(f_return_callback: (skip.ui.UNNotificationSettings) -> Unit) {
        Task {
            f_return_callback(notificationSettings())
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    suspend fun setBadgeCount(count: Int): Unit = Unit

    suspend fun requestAuthorization(options: UNAuthorizationOptions): Boolean = Async.run l@{
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return@l true
        }
        val granted = UIApplication.shared.requestPermission(Manifest.permission.POST_NOTIFICATIONS)
        val defaults = UserDefaults.standard
        if (granted) {
            defaults.removeObject(forKey = "UNNotificationPermissionDenied")
        } else {
            defaults.set(true, forKey = "UNNotificationPermissionDenied")
        }
        return@l granted
    }

    suspend fun requestAuthorization(bridgedOptions: Int): Boolean = Async.run l@{
        return@l requestAuthorization(options = UNAuthorizationOptions(rawValue = bridgedOptions))
    }
    fun callback_requestAuthorization(bridgedOptions: Int, f_return_callback: (Boolean?, Throwable?) -> Unit) {
        Task {
            try {
                f_return_callback(requestAuthorization(bridgedOptions = bridgedOptions), null)
            } catch(t: Throwable) {
                f_return_callback(null, t)
            }
        }
    }

    var delegate: UNUserNotificationCenterDelegate? = null
        get() = field.sref({ this.delegate = it })
        set(newValue) {
            field = newValue.sref()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val supportsContentExtensions: Boolean
        get() {
            fatalError()
        }

    suspend fun add(request: UNNotificationRequest): Unit = Async.run l@{
        val delegate_0 = delegate.sref()
        if (delegate_0 == null) {
            return@l
        }
        val notification = UNNotification(request = request, date = Date.now)
        val options = delegate_0.userNotificationCenter(this, willPresent = notification)
        if (!options.contains(UNNotificationPresentationOptions.banner) && !options.contains(UNNotificationPresentationOptions.alert)) {
            return@l
        }
        val activity_0 = UIApplication.shared.androidActivity.sref()
        if (activity_0 == null) {
            return@l
        }
        val intent = Intent(activity_0, type(of = activity_0).java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val extras = android.os.Bundle()
        for ((key, value) in request.content.userInfo.sref()) {
            val matchtarget_1 = value as? String
            if (matchtarget_1 != null) {
                val s = matchtarget_1
                extras.putString(key.toString(), s)
            } else {
                val matchtarget_2 = value as? Boolean
                if (matchtarget_2 != null) {
                    val b = matchtarget_2
                    extras.putBoolean(key.toString(), b)
                } else {
                    val matchtarget_3 = value as? Int
                    if (matchtarget_3 != null) {
                        val i = matchtarget_3
                        extras.putInt(key.toString(), i)
                    } else {
                        val matchtarget_4 = value as? Double
                        if (matchtarget_4 != null) {
                            val d = matchtarget_4
                            extras.putDouble(key.toString(), d)
                        } else {
                            extras.putString(key.toString(), value.toString())
                        }
                    }
                }
            }
        }
        intent.putExtras(extras)

        val pendingFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(activity_0, 0, intent, pendingFlags)

        val channelID = "tools.skip.firebase.messaging" // Match AndroidManifest.xml
        val notificationBuilder = NotificationCompat.Builder(activity_0, channelID)
            .setContentTitle(request.content.title)
            .setContentText(request.content.body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        val application = activity_0.application.sref()
        val matchtarget_5 = request.content.attachments.first(where = { it -> it.type == "public.image" })
        if (matchtarget_5 != null) {
            val imageAttachment = matchtarget_5
            notificationBuilder.setSmallIcon(IconCompat.createWithContentUri(imageAttachment.url.absoluteString))
        } else {
            val packageName = application.getPackageName()

            // Notification icon: must be a resource with transparent background and white logo
            // eg: to be used as a default icon must be added in the AndroidManifest.xml with the following code:
            // <meta-data
            // android:name="com.google.firebase.messaging.default_notification_icon"
            // android:resource="@drawable/ic_notification" />

            val iconNotificationIdentifier = "ic_notification"
            val resourceFolder = "drawable"

            var resId = application.resources.getIdentifier(iconNotificationIdentifier, resourceFolder, packageName)

            // Check if the resource is found, otherwise fallback to use the default app icon (eg. ic_launcher)
            if (resId == 0) {
                resId = application.resources.getIdentifier("ic_launcher", "mipmap", packageName)
            }

            notificationBuilder.setSmallIcon(IconCompat.createWithResource(application, resId))
        }

        val manager = (activity_0.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).sref()
        val appName = application.packageManager.getApplicationLabel(application.applicationInfo)
        val channel = NotificationChannel(channelID, appName, NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(channel)
        manager.notify(Random.nextInt(), notificationBuilder.build())
    }
    fun callback_add(request: UNNotificationRequest, f_return_callback: (Throwable?) -> Unit) {
        Task {
            try {
                add(request = request)
                f_return_callback(null)
            } catch(t: Throwable) {
                f_return_callback(t)
            }
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    suspend fun getPendingNotificationRequests(): Array<*> = Async.run {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun removePendingNotificationRequests(withIdentifiers: Array<String>) = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun removeAllPendingNotificationRequests() = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    suspend fun getDeliveredNotifications(): Array<*> = Async.run {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun removeDeliveredNotifications(withIdentifiers: Array<String>) = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun removeAllDeliveredNotifications() = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun setNotificationCategories(categories: Set<AnyHashable>) = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    suspend fun getNotificationCategories(): Set<AnyHashable> = Async.run {
        fatalError()
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
        private val shared = UNUserNotificationCenter()

        fun current(): UNUserNotificationCenter = shared
    }
}

interface UNUserNotificationCenterDelegate {
    suspend fun userNotificationCenter(center: UNUserNotificationCenter, didReceive: UNNotificationResponse): Unit = Unit

    suspend fun userNotificationCenter(center: UNUserNotificationCenter, willPresent: UNNotification): UNNotificationPresentationOptions = Async.run l@{
        val notification = willPresent
        return@l UNNotificationPresentationOptions.of()
    }

    fun userNotificationCenter(center: UNUserNotificationCenter, openSettingsFor: UNNotification?) = Unit
}

class UNAuthorizationOptions: OptionSet<UNAuthorizationOptions, Int>, MutableStruct {
    override var rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override val rawvaluelong: ULong
        get() = ULong(rawValue)
    override fun makeoptionset(rawvaluelong: ULong): UNAuthorizationOptions = UNAuthorizationOptions(rawValue = Int(rawvaluelong))
    override fun assignoptionset(target: UNAuthorizationOptions) {
        willmutate()
        try {
            assignfrom(target)
        } finally {
            didmutate()
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as UNAuthorizationOptions
        this.rawValue = copy.rawValue
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = UNAuthorizationOptions(this as MutableStruct)

    private fun assignfrom(target: UNAuthorizationOptions) {
        this.rawValue = target.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val badge = UNAuthorizationOptions(rawValue = 1 shl 0) // For bridging
        val sound = UNAuthorizationOptions(rawValue = 1 shl 1) // For bridging
        val alert = UNAuthorizationOptions(rawValue = 1 shl 2) // For bridging
        val carPlay = UNAuthorizationOptions(rawValue = 1 shl 3) // For bridging
        val criticalAlert = UNAuthorizationOptions(rawValue = 1 shl 4) // For bridging
        val providesAppNotificationSettings = UNAuthorizationOptions(rawValue = 1 shl 5) // For bridging
        var provisional = UNAuthorizationOptions(rawValue = 1 shl 6)
            get() = field.sref({ this.provisional = it })
            set(newValue) {
                field = newValue.sref()
            } // For bridging

        fun of(vararg options: UNAuthorizationOptions): UNAuthorizationOptions {
            val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
            return UNAuthorizationOptions(rawValue = value)
        }
    }
}

@androidx.annotation.Keep
class UNNotification: skip.lib.SwiftProjecting {
    val request: UNNotificationRequest
    val date: Date

    constructor(request: UNNotificationRequest, date: Date) {
        this.request = request
        this.date = date.sref()
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

@androidx.annotation.Keep
class UNNotificationRequest: skip.lib.SwiftProjecting {
    val identifier: String
    val content: UNNotificationContent
    val trigger: UNNotificationTrigger?

    constructor(identifier: String, content: UNNotificationContent, trigger: UNNotificationTrigger?) {
        this.identifier = identifier
        this.content = content
        this.trigger = trigger
    }

    constructor(identifier: String, content: UNNotificationContent): this(identifier = identifier, content = content, trigger = UNPushNotificationTrigger(repeats = false)) {
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

val UNNotificationDefaultActionIdentifier = "UNNotificationDefaultActionIdentifier" // For bridging
val UNNotificationDismissActionIdentifier = "UNNotificationDismissActionIdentifier" // For bridging

@androidx.annotation.Keep
class UNNotificationResponse: skip.lib.SwiftProjecting {
    val actionIdentifier: String
    val notification: UNNotification

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    val targetScene: Any?
        get() {
            fatalError()
        }

    constructor(actionIdentifier: String = "UNNotificationDefaultActionIdentifier", notification: UNNotification) {
        this.actionIdentifier = actionIdentifier
        this.notification = notification
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

@androidx.annotation.Keep
class UNNotificationPresentationOptions: OptionSet<UNNotificationPresentationOptions, Int>, MutableStruct, skip.lib.SwiftProjecting {
    override var rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override val rawvaluelong: ULong
        get() = ULong(rawValue)
    override fun makeoptionset(rawvaluelong: ULong): UNNotificationPresentationOptions = UNNotificationPresentationOptions(rawValue = Int(rawvaluelong))
    override fun assignoptionset(target: UNNotificationPresentationOptions) {
        willmutate()
        try {
            assignfrom(target)
        } finally {
            didmutate()
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as UNNotificationPresentationOptions
        this.rawValue = copy.rawValue
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = UNNotificationPresentationOptions(this as MutableStruct)

    private fun assignfrom(target: UNNotificationPresentationOptions) {
        this.rawValue = target.rawValue
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {

        val badge = UNNotificationPresentationOptions(rawValue = 1 shl 0) // For bridging
        val banner = UNNotificationPresentationOptions(rawValue = 1 shl 1) // For bridging
        val list = UNNotificationPresentationOptions(rawValue = 1 shl 2) // For bridging
        val sound = UNNotificationPresentationOptions(rawValue = 1 shl 3) // For bridging
        val alert = UNNotificationPresentationOptions(rawValue = 1 shl 4) // For bridging

        fun of(vararg options: UNNotificationPresentationOptions): UNNotificationPresentationOptions {
            val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
            return UNNotificationPresentationOptions(rawValue = value)
        }
    }
}

@androidx.annotation.Keep
@Suppress("MUST_BE_INITIALIZED", "MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT")
open class UNNotificationContent: skip.lib.SwiftProjecting {
    open var title: String
        internal set
    open var subtitle: String
        internal set
    open var body: String
        internal set
    open var badge: java.lang.Number? = null
        internal set
    open val bridgedBadge: Int?
        get() {
            return badge?.intValue
        }
    open var sound: UNNotificationSound? = null
        internal set
    open var launchImageName: String
        internal set
    open var userInfo: Dictionary<AnyHashable, Any>
        get() = field.sref({ this.userInfo = it })
        internal set(newValue) {
            field = newValue.sref()
        }
    open val bridgedUserInfo: Dictionary<AnyHashable, Any>
        get() {
            return userInfo.filter l@{ entry ->
                val value = entry.value
                return@l value is Boolean || value is Double || value is Float || value is Int || value is Long || value is String || value is Array<*> || value is Dictionary<*, *> || value is Set<*>
            }
        }
    open var attachments: Array<UNNotificationAttachment>
        get() = field.sref({ this.attachments = it })
        internal set(newValue) {
            field = newValue.sref()
        }
    open var categoryIdentifier: String
        internal set
    open var threadIdentifier: String
        internal set
    open var targetContentIdentifier: String? = null
        internal set
    open var summaryArgument: String
        internal set
    open var summaryArgumentCount: Int
        internal set
    open var filterCriteria: String? = null
        internal set

    constructor(title: String = "", subtitle: String = "", body: String = "", badge: java.lang.Number? = null, sound: UNNotificationSound? = UNNotificationSound.default, launchImageName: String = "", userInfo: Dictionary<AnyHashable, Any> = dictionaryOf(), attachments: Array<UNNotificationAttachment> = arrayOf(), categoryIdentifier: String = "", threadIdentifier: String = "", targetContentIdentifier: String? = null, summaryArgument: String = "", summaryArgumentCount: Int = 0, filterCriteria: String? = null) {
        this.title = title
        this.subtitle = subtitle
        this.body = body
        this.badge = badge
        this.sound = sound
        this.launchImageName = launchImageName
        this.userInfo = userInfo
        this.attachments = attachments
        this.categoryIdentifier = categoryIdentifier
        this.threadIdentifier = threadIdentifier
        this.targetContentIdentifier = targetContentIdentifier
        this.summaryArgument = summaryArgument
        this.summaryArgumentCount = summaryArgumentCount
        this.filterCriteria = filterCriteria
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: CompanionClass() {

        override fun bridgedContent(title: String, subtitle: String, body: String, badge: Int?, sound: UNNotificationSound?, launchImageName: String, userInfo: Dictionary<AnyHashable, Any>, attachments: Array<UNNotificationAttachment>, categoryIdentifier: String, threadIdentifier: String, targetContentIdentifier: String?, summaryArgument: String, summaryArgumentCount: Int, filterCriteria: String?): UNNotificationContent = UNNotificationContent(title = title, subtitle = subtitle, body = body, badge = if (badge == null) null else NSNumber(value = Int(badge!!)), sound = sound, launchImageName = launchImageName, userInfo = userInfo, attachments = attachments, categoryIdentifier = categoryIdentifier, threadIdentifier = threadIdentifier, targetContentIdentifier = targetContentIdentifier, summaryArgument = summaryArgument, summaryArgumentCount = summaryArgumentCount, filterCriteria = filterCriteria)
    }
    open class CompanionClass {
        open fun bridgedContent(title: String, subtitle: String, body: String, badge: Int?, sound: UNNotificationSound?, launchImageName: String, userInfo: Dictionary<AnyHashable, Any>, attachments: Array<UNNotificationAttachment>, categoryIdentifier: String, threadIdentifier: String, targetContentIdentifier: String?, summaryArgument: String, summaryArgumentCount: Int, filterCriteria: String?): UNNotificationContent = UNNotificationContent.bridgedContent(title = title, subtitle = subtitle, body = body, badge = badge, sound = sound, launchImageName = launchImageName, userInfo = userInfo, attachments = attachments, categoryIdentifier = categoryIdentifier, threadIdentifier = threadIdentifier, targetContentIdentifier = targetContentIdentifier, summaryArgument = summaryArgument, summaryArgumentCount = summaryArgumentCount, filterCriteria = filterCriteria)
    }
}

class UNMutableNotificationContent: UNNotificationContent {
    override var title: String
        get() = super.title
        set(newValue) {
            super.title = newValue
        }
    override var subtitle: String
        get() = super.subtitle
        set(newValue) {
            super.subtitle = newValue
        }
    override var body: String
        get() = super.body
        set(newValue) {
            super.body = newValue
        }
    override var badge: java.lang.Number?
        get() = super.badge
        set(newValue) {
            super.badge = newValue
        }
    override var sound: UNNotificationSound?
        get() = super.sound
        set(newValue) {
            super.sound = newValue
        }
    override var launchImageName: String
        get() = super.launchImageName
        set(newValue) {
            super.launchImageName = newValue
        }
    override var userInfo: Dictionary<AnyHashable, Any>
        get() = super.userInfo.sref({ this.userInfo = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            super.userInfo = newValue
        }
    override var attachments: Array<UNNotificationAttachment>
        get() = super.attachments.sref({ this.attachments = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            super.attachments = newValue
        }
    override var categoryIdentifier: String
        get() = super.categoryIdentifier
        set(newValue) {
            super.categoryIdentifier = newValue
        }
    override var threadIdentifier: String
        get() = super.threadIdentifier
        set(newValue) {
            super.threadIdentifier = newValue
        }
    override var targetContentIdentifier: String?
        get() = super.targetContentIdentifier
        set(newValue) {
            super.targetContentIdentifier = newValue
        }
    override var summaryArgument: String
        get() = super.summaryArgument
        set(newValue) {
            super.summaryArgument = newValue
        }
    override var summaryArgumentCount: Int
        get() = super.summaryArgumentCount
        set(newValue) {
            super.summaryArgumentCount = newValue
        }
    override var filterCriteria: String?
        get() = super.filterCriteria
        set(newValue) {
            super.filterCriteria = newValue
        }

    constructor(title: String = "", subtitle: String = "", body: String = "", badge: java.lang.Number? = null, sound: UNNotificationSound? = UNNotificationSound.default, launchImageName: String = "", userInfo: Dictionary<AnyHashable, Any> = dictionaryOf(), attachments: Array<UNNotificationAttachment> = arrayOf(), categoryIdentifier: String = "", threadIdentifier: String = "", targetContentIdentifier: String? = null, summaryArgument: String = "", summaryArgumentCount: Int = 0, filterCriteria: String? = null): super(title, subtitle, body, badge, sound, launchImageName, userInfo, attachments, categoryIdentifier, threadIdentifier, targetContentIdentifier, summaryArgument, summaryArgumentCount, filterCriteria) {
    }

    @androidx.annotation.Keep
    companion object: UNNotificationContent.CompanionClass() {
    }
}

@androidx.annotation.Keep
class UNNotificationSound: skip.lib.SwiftProjecting {
    val name: UNNotificationSoundName
    val bridgedName: String
        get() = name.rawValue
    val volume: Float

    constructor(named: UNNotificationSoundName, volume: Float = 0.0f) {
        val name = named
        this.name = name
        this.volume = volume
    }

    constructor(named: String, volume: Float): this(named = UNNotificationSoundName(rawValue = named), volume = volume) {
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {

        val default: UNNotificationSound
            get() = UNNotificationSound(named = UNNotificationSoundName(rawValue = "default"))

        val defaultCriticalSound: UNNotificationSound
            get() = UNNotificationSound(named = UNNotificationSoundName(rawValue = "default_critical"))

        fun defaultCriticalSound(withAudioVolume: Float): UNNotificationSound {
            val volume = withAudioVolume
            return UNNotificationSound(named = UNNotificationSoundName(rawValue = "default_critical"), volume = volume)
        }

        fun soundNamed(name: UNNotificationSoundName): UNNotificationSound = UNNotificationSound(named = name)
    }
}

class UNNotificationSoundName: RawRepresentable<String> {
    override val rawValue: String

    constructor(rawValue: String) {
        this.rawValue = rawValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is UNNotificationSoundName) return false
        return rawValue == other.rawValue
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, rawValue)
        return result
    }

    @androidx.annotation.Keep
    companion object {
    }
}

val UNNotificationAttachmentOptionsTypeHintKey = "UNNotificationAttachmentOptionsTypeHintKey"
val UNNotificationAttachmentOptionsThumbnailHiddenKey = "UNNotificationAttachmentOptionsThumbnailHiddenKey"
val UNNotificationAttachmentOptionsThumbnailClippingRectKey = "UNNotificationAttachmentOptionsThumbnailClippingRectKey"
val UNNotificationAttachmentOptionsThumbnailTimeKey = "UNNotificationAttachmentOptionsThumbnailTimeKey"

@androidx.annotation.Keep
open class UNNotificationAttachment: skip.lib.SwiftProjecting {
    val identifier: String
    val url: URL
    val type: String
    val timeShift: Double

    constructor(identifier: String, url: URL, type: String = "public.data", timeShift: Double = 0.0) {
        this.identifier = identifier
        this.url = url.sref()
        this.type = type
        this.timeShift = timeShift
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: CompanionClass() {

        override fun attachment(withIdentifier: String, url: URL, options: Dictionary<AnyHashable, Any>?): UNNotificationAttachment {
            val identifier = withIdentifier
            return UNNotificationAttachment(identifier = identifier, url = url, type = "public.data")
        }
    }
    open class CompanionClass {
        open fun attachment(withIdentifier: String, url: URL, options: Dictionary<AnyHashable, Any>? = null): UNNotificationAttachment = UNNotificationAttachment.attachment(withIdentifier = withIdentifier, url = url, options = options)
    }
}

open class UNNotificationTrigger {
    val repeats: Boolean

    constructor(repeats: Boolean) {
        this.repeats = repeats
    }

    @androidx.annotation.Keep
    companion object: CompanionClass() {
    }
    open class CompanionClass {
    }
}

class UNTimeIntervalNotificationTrigger: UNNotificationTrigger {
    val timeInterval: Double

    constructor(timeInterval: Double, repeats: Boolean): super(repeats = repeats) {
        this.timeInterval = timeInterval
    }

    @androidx.annotation.Keep
    companion object: UNNotificationTrigger.CompanionClass() {
    }
}

class UNCalendarNotificationTrigger: UNNotificationTrigger {
    val dateComponents: DateComponents

    constructor(dateComponents: DateComponents, repeats: Boolean): super(repeats = repeats) {
        this.dateComponents = dateComponents.sref()
    }

    @androidx.annotation.Keep
    companion object: UNNotificationTrigger.CompanionClass() {
    }
}

class UNLocationNotificationTrigger: UNNotificationTrigger {
    val region: Any /* CLRegion */

    constructor(region: Any, repeats: Boolean): super(repeats = repeats) {
        this.region = region.sref()
    }

    @androidx.annotation.Keep
    companion object: UNNotificationTrigger.CompanionClass() {
    }
}

class UNPushNotificationTrigger: UNNotificationTrigger {
    constructor(repeats: Boolean): super(repeats = repeats) {
    }

    @androidx.annotation.Keep
    companion object: UNNotificationTrigger.CompanionClass() {
    }
}

@androidx.annotation.Keep
enum class UNAuthorizationStatus(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int>, skip.lib.SwiftProjecting {
    notDetermined(0),
    denied(1),
    authorized(2),
    provisional(3),
    ephemeral(4);

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: Int): UNAuthorizationStatus? {
            return when (rawValue) {
                0 -> UNAuthorizationStatus.notDetermined
                1 -> UNAuthorizationStatus.denied
                2 -> UNAuthorizationStatus.authorized
                3 -> UNAuthorizationStatus.provisional
                4 -> UNAuthorizationStatus.ephemeral
                else -> null
            }
        }
    }
}

fun UNAuthorizationStatus(rawValue: Int): UNAuthorizationStatus? = UNAuthorizationStatus.init(rawValue = rawValue)

enum class UNShowPreviewsSetting(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
    always(0),
    whenAuthenticated(1),
    never(2);

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: Int): UNShowPreviewsSetting? {
            return when (rawValue) {
                0 -> UNShowPreviewsSetting.always
                1 -> UNShowPreviewsSetting.whenAuthenticated
                2 -> UNShowPreviewsSetting.never
                else -> null
            }
        }
    }
}

fun UNShowPreviewsSetting(rawValue: Int): UNShowPreviewsSetting? = UNShowPreviewsSetting.init(rawValue = rawValue)

enum class UNNotificationSetting(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
    notSupported(0),
    disabled(1),
    enabled(2);

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: Int): UNNotificationSetting? {
            return when (rawValue) {
                0 -> UNNotificationSetting.notSupported
                1 -> UNNotificationSetting.disabled
                2 -> UNNotificationSetting.enabled
                else -> null
            }
        }
    }
}

fun UNNotificationSetting(rawValue: Int): UNNotificationSetting? = UNNotificationSetting.init(rawValue = rawValue)

enum class UNAlertStyle(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
    none(0),
    banner(1),
    alert(2);

    @androidx.annotation.Keep
    companion object {
        fun init(rawValue: Int): UNAlertStyle? {
            return when (rawValue) {
                0 -> UNAlertStyle.none
                1 -> UNAlertStyle.banner
                2 -> UNAlertStyle.alert
                else -> null
            }
        }
    }
}

fun UNAlertStyle(rawValue: Int): UNAlertStyle? = UNAlertStyle.init(rawValue = rawValue)

@androidx.annotation.Keep
open class UNNotificationSettings: java.lang.Object, skip.lib.SwiftProjecting {
    private val _authorizationStatus: UNAuthorizationStatus

    open val authorizationStatus: UNAuthorizationStatus
        get() = _authorizationStatus

    constructor(authorizationStatus: UNAuthorizationStatus): super() {
        this._authorizationStatus = authorizationStatus
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val soundSetting: UNNotificationSetting
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val badgeSetting: UNNotificationSetting
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val alertSetting: UNNotificationSetting
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val notificationCenterSetting: UNNotificationSetting
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val lockScreenSetting: UNNotificationSetting
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val carPlaySetting: UNNotificationSetting
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val alertStyle: UNAlertStyle
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val showPreviewsSetting: UNShowPreviewsSetting
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val criticalAlertSetting: UNNotificationSetting
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val providesAppNotificationSettings: Boolean
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val announcementSetting: UNNotificationSetting
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val timeSensitiveSetting: UNNotificationSetting
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val scheduledDeliverySetting: UNNotificationSetting
        get() {
            fatalError()
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    open val directMessagesSetting: UNNotificationSetting
        get() {
            fatalError()
        }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object: CompanionClass() {
    }
    open class CompanionClass {
    }
}

