package kickbase.core

import skip.lib.*
import skip.lib.Array

import skip.foundation.*

// MARK: - Live Event Types
@androidx.annotation.Keep
class LiveEventTypesResponse: Codable {
    val types: Array<LiveEventType>
    val formulas: Dictionary<String, String>?

    internal enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        types("it"),
        formulas("dds");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): LiveEventTypesResponse.CodingKeys? {
                return when (rawValue) {
                    "it" -> CodingKeys.types
                    "dds" -> CodingKeys.formulas
                    else -> null
                }
            }
        }
    }

    constructor(types: Array<LiveEventType>, formulas: Dictionary<String, String>? = null) {
        this.types = types.sref()
        this.formulas = formulas.sref()
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(types, forKey = CodingKeys.types)
        container.encodeIfPresent(formulas, forKey = CodingKeys.formulas)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.types = container.decode(Array::class, elementType = LiveEventType::class, forKey = CodingKeys.types)
        this.formulas = container.decodeIfPresent(Dictionary::class, keyType = String::class, valueType = String::class, forKey = CodingKeys.formulas)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<LiveEventTypesResponse> {
        override fun init(from: Decoder): LiveEventTypesResponse = LiveEventTypesResponse(from = from)

        internal fun CodingKeys(rawValue: String): LiveEventTypesResponse.CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class LiveEventType: Codable, Identifiable<Int> {
    override val id: Int
    val name: String

    internal enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        id("i"),
        name_("ti");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): LiveEventType.CodingKeys? {
                return when (rawValue) {
                    "i" -> CodingKeys.id
                    "ti" -> CodingKeys.name_
                    else -> null
                }
            }
        }
    }

    constructor(id: Int, name: String) {
        this.id = id
        this.name = name
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(id, forKey = CodingKeys.id)
        container.encode(name, forKey = CodingKeys.name_)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.id = container.decode(Int::class, forKey = CodingKeys.id)
        this.name = container.decode(String::class, forKey = CodingKeys.name_)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<LiveEventType> {
        override fun init(from: Decoder): LiveEventType = LiveEventType(from = from)

        internal fun CodingKeys(rawValue: String): LiveEventType.CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

// MARK: - Live/Match Experience Models
@androidx.annotation.Keep
class LiveMatchDayResponse: Codable {
    val players: Array<LivePlayer>

    internal enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        players("lp");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): LiveMatchDayResponse.CodingKeys? {
                return when (rawValue) {
                    "lp" -> CodingKeys.players
                    else -> null
                }
            }
        }
    }

    constructor(players: Array<LivePlayer>) {
        this.players = players.sref()
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(players, forKey = CodingKeys.players)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.players = container.decode(Array::class, elementType = LivePlayer::class, forKey = CodingKeys.players)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<LiveMatchDayResponse> {
        override fun init(from: Decoder): LiveMatchDayResponse = LiveMatchDayResponse(from = from)

        internal fun CodingKeys(rawValue: String): LiveMatchDayResponse.CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class LivePlayer: Codable, Identifiable<String> {
    override val id: String
    val name: String // "n" in API
    val position: Int
    val teamId: String
    val p: Int // Live points
    val profileBigUrl: String?
    val k: Array<Int>

    val imageUrl: URL?
        get() {
            val urlString_0 = profileBigUrl
            if (urlString_0 == null) {
                return null
            }
            if (urlString_0.hasPrefix("http")) {
                return (try { URL(string = urlString_0) } catch (_: NullReturnException) { null })
            } else if (!urlString_0.isEmpty) {
                val path = if (urlString_0.hasPrefix("/")) String(urlString_0.dropFirst()) else urlString_0
                return (try { URL(string = "https://kickbase.com/${path}") } catch (_: NullReturnException) { null })
            }
            return null
        }

    val eventIcons: String
        get() {
            return k.compactMap(fun(eventId: *): String? {
                when (eventId) {
                    1 -> return "⚽️"
                    3 -> return "👟"
                    5 -> return "🟨🟥"
                    6 -> return "🟥"
                    7 -> return "🧤"
                    else -> return null
                }
            }).joined()
        }

    // Berechnet firstName und lastName aus dem vollständigen Namen
    val firstName: String
        get() {
            val components = name.split(separator = ' ', maxSplits = 1, omittingEmptySubsequences = true)
            return if (components.count > 1) String(components[0]) else ""
        }

    val lastName: String
        get() {
            val components = name.split(separator = ' ', maxSplits = 1, omittingEmptySubsequences = true)
            return if (components.count > 1) String(components[1]) else name
        }

    internal enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        id("i"),
        name_("n"),
        position("pos"),
        teamId("tid"),
        p("p"),
        profileBigUrl("pb"),
        k("k");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): LivePlayer.CodingKeys? {
                return when (rawValue) {
                    "i" -> CodingKeys.id
                    "n" -> CodingKeys.name_
                    "pos" -> CodingKeys.position
                    "tid" -> CodingKeys.teamId
                    "p" -> CodingKeys.p
                    "pb" -> CodingKeys.profileBigUrl
                    "k" -> CodingKeys.k
                    else -> null
                }
            }
        }
    }

    constructor(from: Decoder) {
        val decoder = from
        val container = decoder.container(keyedBy = CodingKeys::class)
        id = container.decode(String::class, forKey = LivePlayer.CodingKeys.id)
        name = container.decode(String::class, forKey = LivePlayer.CodingKeys.name_)
        position = container.decode(Int::class, forKey = LivePlayer.CodingKeys.position)
        teamId = container.decode(String::class, forKey = LivePlayer.CodingKeys.teamId)
        profileBigUrl = container.decodeIfPresent(String::class, forKey = LivePlayer.CodingKeys.profileBigUrl)
        p = container.decodeIfPresent(Int::class, forKey = LivePlayer.CodingKeys.p) ?: 0
        k = (container.decodeIfPresent(Array::class, elementType = Int::class, forKey = LivePlayer.CodingKeys.k) ?: arrayOf()).sref()
    }

    constructor(id: String, name: String, position: Int, teamId: String, p: Int, profileBigUrl: String? = null, k: Array<Int>) {
        this.id = id
        this.name = name
        this.position = position
        this.teamId = teamId
        this.p = p
        this.profileBigUrl = profileBigUrl
        this.k = k.sref()
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(id, forKey = CodingKeys.id)
        container.encode(name, forKey = CodingKeys.name_)
        container.encode(position, forKey = CodingKeys.position)
        container.encode(teamId, forKey = CodingKeys.teamId)
        container.encode(p, forKey = CodingKeys.p)
        container.encodeIfPresent(profileBigUrl, forKey = CodingKeys.profileBigUrl)
        container.encode(k, forKey = CodingKeys.k)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<LivePlayer> {
        override fun init(from: Decoder): LivePlayer = LivePlayer(from = from)

        internal fun CodingKeys(rawValue: String): LivePlayer.CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

// MARK: - Player Match Detail
@androidx.annotation.Keep
class PlayerMatchDetailResponse: Codable {
    val events: Array<PlayerMatchEvent>

    internal enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        events("events");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): PlayerMatchDetailResponse.CodingKeys? {
                return when (rawValue) {
                    "events" -> CodingKeys.events
                    else -> null
                }
            }
        }
    }

    constructor(events: Array<PlayerMatchEvent>) {
        this.events = events.sref()
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(events, forKey = CodingKeys.events)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.events = container.decode(Array::class, elementType = PlayerMatchEvent::class, forKey = CodingKeys.events)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<PlayerMatchDetailResponse> {
        override fun init(from: Decoder): PlayerMatchDetailResponse = PlayerMatchDetailResponse(from = from)

        internal fun CodingKeys(rawValue: String): PlayerMatchDetailResponse.CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class PlayerMatchEvent: Codable, Identifiable<String> {
    override val id: String
        get() = eventId ?: UUID().uuidString

    val eventId: String?
    val type: Int?
    val minute: Int?
    val points: Int?
    val name: String?

    val icon: String
        get() {
            when (type) {
                1 -> return "⚽️"
                2 -> return "🧤"
                3 -> return "👟"
                4 -> return "🟨"
                5 -> return "🟨🟥"
                6 -> return "🟥"
                7 -> return "🧤" // Saved Penalty
                8 -> return "💀" // Own Goal
                12 -> return "📺" // VAR
                else -> return "🔹" // Generic
            }
        }

    internal enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        eventId("eid"),
        type("t"),
        minute("m"),
        points("p"),
        name_("n");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): PlayerMatchEvent.CodingKeys? {
                return when (rawValue) {
                    "eid" -> CodingKeys.eventId
                    "t" -> CodingKeys.type
                    "m" -> CodingKeys.minute
                    "p" -> CodingKeys.points
                    "n" -> CodingKeys.name_
                    else -> null
                }
            }
        }
    }

    internal enum class V4CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        eventId("ei"),
        type("eti"),
        minute("mt"),
        points("p"),
        attributes("att");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): PlayerMatchEvent.V4CodingKeys? {
                return when (rawValue) {
                    "ei" -> V4CodingKeys.eventId
                    "eti" -> V4CodingKeys.type
                    "mt" -> V4CodingKeys.minute
                    "p" -> V4CodingKeys.points
                    "att" -> V4CodingKeys.attributes
                    else -> null
                }
            }
        }
    }

    constructor(from: Decoder) {
        val decoder = from
        val v4Container = decoder.container(keyedBy = V4CodingKeys::class)
        val legacyContainer = decoder.container(keyedBy = CodingKeys::class)

        // Try to decode V4 first
        val v4Type = v4Container.decodeIfPresent(Int::class, forKey = PlayerMatchEvent.V4CodingKeys.type)
        val v4Minute = v4Container.decodeIfPresent(Int::class, forKey = PlayerMatchEvent.V4CodingKeys.minute)

        if (v4Type != null || v4Minute != null) {
            this.type = v4Type
            this.minute = v4Minute
            this.eventId = v4Container.decodeIfPresent(String::class, forKey = PlayerMatchEvent.V4CodingKeys.eventId)
            this.points = v4Container.decodeIfPresent(Int::class, forKey = PlayerMatchEvent.V4CodingKeys.points)
            this.name = null
        } else {
            // Legacy decode
            this.type = legacyContainer.decodeIfPresent(Int::class, forKey = PlayerMatchEvent.CodingKeys.type)
            this.minute = legacyContainer.decodeIfPresent(Int::class, forKey = PlayerMatchEvent.CodingKeys.minute)
            this.eventId = legacyContainer.decodeIfPresent(String::class, forKey = PlayerMatchEvent.CodingKeys.eventId)
            this.points = legacyContainer.decodeIfPresent(Int::class, forKey = PlayerMatchEvent.CodingKeys.points)
            this.name = legacyContainer.decodeIfPresent(String::class, forKey = PlayerMatchEvent.CodingKeys.name_)
        }
    }

    override fun encode(to: Encoder) {
        val encoder = to
        var container = encoder.container(keyedBy = CodingKeys::class)
        container.encodeIfPresent(eventId, forKey = PlayerMatchEvent.CodingKeys.eventId)
        container.encodeIfPresent(type, forKey = PlayerMatchEvent.CodingKeys.type)
        container.encodeIfPresent(minute, forKey = PlayerMatchEvent.CodingKeys.minute)
        container.encodeIfPresent(points, forKey = PlayerMatchEvent.CodingKeys.points)
        container.encodeIfPresent(name, forKey = PlayerMatchEvent.CodingKeys.name_)
    }

    constructor(eventId: String? = null, type: Int? = null, minute: Int? = null, points: Int? = null, name: String? = null) {
        this.eventId = eventId
        this.type = type
        this.minute = minute
        this.points = points
        this.name = name
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<PlayerMatchEvent> {
        override fun init(from: Decoder): PlayerMatchEvent = PlayerMatchEvent(from = from)

        internal fun CodingKeys(rawValue: String): PlayerMatchEvent.CodingKeys? = CodingKeys.init(rawValue = rawValue)

        internal fun V4CodingKeys(rawValue: String): PlayerMatchEvent.V4CodingKeys? = V4CodingKeys.init(rawValue = rawValue)
    }
}
