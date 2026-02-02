package kickbase.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import skip.lib.*
import skip.lib.Array
import skip.lib.Set

import skip.model.*
import skip.foundation.*
import skip.ui.*

// MARK: - Models

@androidx.annotation.Keep
class LigainsiderPlayer: Codable, Identifiable<String> {
    override val id: String
        get() = ligainsiderId ?: name
    val name: String
    val alternative: String? // Name der Alternative
    val ligainsiderId: String? // z.B. "nikola-vasilj_13866"
    val imageUrl: String? // URL zum Profilbild

    constructor(name: String, alternative: String? = null, ligainsiderId: String? = null, imageUrl: String? = null) {
        this.name = name
        this.alternative = alternative
        this.ligainsiderId = ligainsiderId
        this.imageUrl = imageUrl
    }

    override fun equals(other: Any?): Boolean {
        if (other !is LigainsiderPlayer) return false
        return name == other.name && alternative == other.alternative && ligainsiderId == other.ligainsiderId && imageUrl == other.imageUrl
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, name)
        result = Hasher.combine(result, alternative)
        result = Hasher.combine(result, ligainsiderId)
        result = Hasher.combine(result, imageUrl)
        return result
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        name_("name"),
        alternative("alternative"),
        ligainsiderId("ligainsiderId"),
        imageUrl("imageUrl");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "name" -> CodingKeys.name_
                    "alternative" -> CodingKeys.alternative
                    "ligainsiderId" -> CodingKeys.ligainsiderId
                    "imageUrl" -> CodingKeys.imageUrl
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(name, forKey = CodingKeys.name_)
        container.encodeIfPresent(alternative, forKey = CodingKeys.alternative)
        container.encodeIfPresent(ligainsiderId, forKey = CodingKeys.ligainsiderId)
        container.encodeIfPresent(imageUrl, forKey = CodingKeys.imageUrl)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.name = container.decode(String::class, forKey = CodingKeys.name_)
        this.alternative = container.decodeIfPresent(String::class, forKey = CodingKeys.alternative)
        this.ligainsiderId = container.decodeIfPresent(String::class, forKey = CodingKeys.ligainsiderId)
        this.imageUrl = container.decodeIfPresent(String::class, forKey = CodingKeys.imageUrl)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<LigainsiderPlayer> {
        override fun init(from: Decoder): LigainsiderPlayer = LigainsiderPlayer(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class LineupRow: Codable, Identifiable<UUID>, MutableStruct {
    override var id = UUID()
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    val players: Array<LigainsiderPlayer>

    constructor(players: Array<LigainsiderPlayer>) {
        this.players = players.sref()
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as LineupRow
        this.id = copy.id
        this.players = copy.players
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = LineupRow(this as MutableStruct)

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        id("id"),
        players("players");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "id" -> CodingKeys.id
                    "players" -> CodingKeys.players
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(id, forKey = CodingKeys.id)
        container.encode(players, forKey = CodingKeys.players)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.id = container.decode(UUID::class, forKey = CodingKeys.id)
        this.players = container.decode(Array::class, elementType = LigainsiderPlayer::class, forKey = CodingKeys.players)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<LineupRow> {
        override fun init(from: Decoder): LineupRow = LineupRow(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class LigainsiderMatch: Codable, Identifiable<String> {
    override val id: String
        get() = homeTeam + (url ?: UUID().uuidString)
    val homeTeam: String
    val awayTeam: String
    val homeLogo: String?
    val awayLogo: String?
    // Aufstellung ist jetzt ein Array von Reihen (z.B. [Torwart, Abwehr, Mittelfeld, Sturm])
    // Jede Reihe ist ein Array von Spielern
    val homeLineup: Array<LineupRow>
    val awayLineup: Array<LineupRow>
    val homeSquad: Array<LigainsiderPlayer>
    val awaySquad: Array<LigainsiderPlayer>
    val url: String?

    constructor(homeTeam: String, awayTeam: String, homeLogo: String? = null, awayLogo: String? = null, homeLineup: Array<LineupRow>, awayLineup: Array<LineupRow>, homeSquad: Array<LigainsiderPlayer>, awaySquad: Array<LigainsiderPlayer>, url: String? = null) {
        this.homeTeam = homeTeam
        this.awayTeam = awayTeam
        this.homeLogo = homeLogo
        this.awayLogo = awayLogo
        this.homeLineup = homeLineup.sref()
        this.awayLineup = awayLineup.sref()
        this.homeSquad = homeSquad.sref()
        this.awaySquad = awaySquad.sref()
        this.url = url
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        homeTeam("homeTeam"),
        awayTeam("awayTeam"),
        homeLogo("homeLogo"),
        awayLogo("awayLogo"),
        homeLineup("homeLineup"),
        awayLineup("awayLineup"),
        homeSquad("homeSquad"),
        awaySquad("awaySquad"),
        url("url");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "homeTeam" -> CodingKeys.homeTeam
                    "awayTeam" -> CodingKeys.awayTeam
                    "homeLogo" -> CodingKeys.homeLogo
                    "awayLogo" -> CodingKeys.awayLogo
                    "homeLineup" -> CodingKeys.homeLineup
                    "awayLineup" -> CodingKeys.awayLineup
                    "homeSquad" -> CodingKeys.homeSquad
                    "awaySquad" -> CodingKeys.awaySquad
                    "url" -> CodingKeys.url
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(homeTeam, forKey = CodingKeys.homeTeam)
        container.encode(awayTeam, forKey = CodingKeys.awayTeam)
        container.encodeIfPresent(homeLogo, forKey = CodingKeys.homeLogo)
        container.encodeIfPresent(awayLogo, forKey = CodingKeys.awayLogo)
        container.encode(homeLineup, forKey = CodingKeys.homeLineup)
        container.encode(awayLineup, forKey = CodingKeys.awayLineup)
        container.encode(homeSquad, forKey = CodingKeys.homeSquad)
        container.encode(awaySquad, forKey = CodingKeys.awaySquad)
        container.encodeIfPresent(url, forKey = CodingKeys.url)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.homeTeam = container.decode(String::class, forKey = CodingKeys.homeTeam)
        this.awayTeam = container.decode(String::class, forKey = CodingKeys.awayTeam)
        this.homeLogo = container.decodeIfPresent(String::class, forKey = CodingKeys.homeLogo)
        this.awayLogo = container.decodeIfPresent(String::class, forKey = CodingKeys.awayLogo)
        this.homeLineup = container.decode(Array::class, elementType = LineupRow::class, forKey = CodingKeys.homeLineup)
        this.awayLineup = container.decode(Array::class, elementType = LineupRow::class, forKey = CodingKeys.awayLineup)
        this.homeSquad = container.decode(Array::class, elementType = LigainsiderPlayer::class, forKey = CodingKeys.homeSquad)
        this.awaySquad = container.decode(Array::class, elementType = LigainsiderPlayer::class, forKey = CodingKeys.awaySquad)
        this.url = container.decodeIfPresent(String::class, forKey = CodingKeys.url)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<LigainsiderMatch> {
        override fun init(from: Decoder): LigainsiderMatch = LigainsiderMatch(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

enum class LigainsiderStatus {
    likelyStart, // S11 ohne Alternative
    startWithAlternative, // S11 mit Alternative (1. Option)
    isAlternative, // Ist die Alternative (2. Option)
    bench, // Auf der Bank / im Kader aber nicht in S11
    out; // Nicht im Kader / nicht gefunden

    @androidx.annotation.Keep
    companion object {
    }
}

@Stable
open class LigainsiderService: ObservableObject {
    override val objectWillChange = ObservableObjectPublisher()
    open var matches: Array<LigainsiderMatch>
        get() = _matches.wrappedValue.sref({ this.matches = it })
        set(newValue) {
            objectWillChange.send()
            _matches.wrappedValue = newValue.sref()
        }
    var _matches: skip.model.Published<Array<LigainsiderMatch>> = skip.model.Published(arrayOf())
    open var isLoading: Boolean
        get() = _isLoading.wrappedValue
        set(newValue) {
            objectWillChange.send()
            _isLoading.wrappedValue = newValue
        }
    var _isLoading: skip.model.Published<Boolean> = skip.model.Published(false)
    open var errorMessage: String?
        get() = _errorMessage.wrappedValue
        set(newValue) {
            objectWillChange.send()
            _errorMessage.wrappedValue = newValue
        }
    var _errorMessage: skip.model.Published<String?> = skip.model.Published(null)
    open var cacheUpdateTrigger: UUID
        get() = _cacheUpdateTrigger.wrappedValue
        set(newValue) {
            objectWillChange.send()
            _cacheUpdateTrigger.wrappedValue = newValue
        }
    var _cacheUpdateTrigger: skip.model.Published<UUID> = skip.model.Published(UUID()) // Triggert Re-Render wenn Cache sich ändert
    open var isLigainsiderReady: Boolean
        get() = _isLigainsiderReady.wrappedValue
        set(newValue) {
            objectWillChange.send()
            _isLigainsiderReady.wrappedValue = newValue
        }
    var _isLigainsiderReady: skip.model.Published<Boolean> = skip.model.Published(false) // true wenn Cache vollständig geladen ist

    // Basis URL
    private val overviewURL = "https://www.ligainsider.de/bundesliga/spieltage/"

    // Cache für schnellen Zugriff: LigainsiderId -> LigainsiderPlayer
    // Wir speichern alle Spieler die in S11 oder als Alternative gelistet sind
    private var playerCache: Dictionary<String, LigainsiderPlayer> = dictionaryOf()
        get() = field.sref({ this.playerCache = it })
        set(newValue) {
            field = newValue.sref()
        }

    // Public readonly access for debugging
    open val playerCacheCount: Int
        get() = playerCache.count

    // Cache für Alternativen (Namen)
    private var alternativeNames: Set<String> = setOf()
        get() = field.sref({ this.alternativeNames = it })
        set(newValue) {
            field = newValue.sref()
        }
    // Cache für Spieler in der Startelf (IDs)
    private var startingLineupIds: Set<String> = setOf()
        get() = field.sref({ this.startingLineupIds = it })
        set(newValue) {
            field = newValue.sref()
        }

    // Exposed as internal for tests
    internal val session: URLSession

    constructor(session: URLSession = URLSession.shared) {
        this.session = SessionSanitizer.sanitized(session)
    }

    // MARK: - Async variant for initialization (waits for completion)
    open suspend fun fetchLineupsAsync(): Unit = Async.run {
        isLoading = true
        errorMessage = null

        try {
            val fetchedMatches = scrapeLigaInsider()

            // Cache aufbauen
            var newCache: Dictionary<String, LigainsiderPlayer> = dictionaryOf()
            var newAlts: Set<String> = setOf()
            var newLineupIds: Set<String> = setOf()

            for (match in fetchedMatches.sref()) {
                // Zuerst Kader zum Cache hinzufügen (für Bilder von Bankspielern/Alternativen)
                val allSquad = (match.homeSquad + match.awaySquad).sref()
                for (player in allSquad.sref()) {
                    player.ligainsiderId?.let { id ->
                        newCache[id] = player
                    }
                }

                val allRows = (match.homeLineup + match.awayLineup).sref()
                for (row in allRows.sref()) {
                    for (player in row.players.sref()) {
                        // Speichere Hauptspieler (überschreibt Kader-Eintrag -> wichtig wegen 'alternative' Property)
                        player.ligainsiderId?.let { id ->
                            newCache[id] = player
                            newLineupIds.insert(id) // Markiere als Startelfspieler
                        }
                        // Speichere Alternative falls vorhanden
                        player.alternative?.let { altName ->
                            newAlts.insert(altName.lowercased())
                        }
                    }
                }
            }

            MainActor.run { ->
                // Merge new cache into existing to preserve squad data
                for ((id, player) in newCache.sref()) {
                    // Preserve existing imageUrl if new player doesn't have one
                    val matchtarget_0 = this.playerCache[id]
                    if (matchtarget_0 != null) {
                        val existingPlayer = matchtarget_0
                        val matchtarget_1 = existingPlayer.imageUrl
                        if (matchtarget_1 != null) {
                            val existingImageUrl = matchtarget_1
                            if (player.imageUrl == null) {
                                // Keep existing image
                                this.playerCache[id] = LigainsiderPlayer(name = player.name, alternative = player.alternative, ligainsiderId = player.ligainsiderId, imageUrl = existingImageUrl)
                            } else {
                                this.playerCache[id] = player
                            }
                        } else {
                            this.playerCache[id] = player
                        }
                    } else {
                        this.playerCache[id] = player
                    }
                }
                // Merge alternatives
                for (alt in newAlts.sref()) {
                    this.alternativeNames.insert(alt)
                }
                // Merge lineup IDs
                for (id in newLineupIds.sref()) {
                    this.startingLineupIds.insert(id)
                }

                print("[Ligainsider] fetchLineupsAsync complete: newCache had ${newCache.count} players, total playerCache now: ${this.playerCache.count}")
                print("[Ligainsider] Alternative names found: ${this.alternativeNames.count}")
                print("[Ligainsider] Starting lineup IDs: ${this.startingLineupIds.count}")

                this.matches = fetchedMatches
                this.isLoading = false
                // Trigger UI-Updates durch Cache-Signal auf Main Thread
                print("[DEBUG] 🔔 Setting cacheUpdateTrigger on Main Thread")
                this.cacheUpdateTrigger = UUID()
                print("[DEBUG] ✅ cacheUpdateTrigger set: ${this.cacheUpdateTrigger}")
                // Mark as ready - Cache ist vollständig geladen
                this.isLigainsiderReady = true
                print("[DEBUG] ✅ Ligainsider is ready: ${this.playerCache.count} players loaded")

                // Backup speichern (kann im Background sein)
                this.saveToLocal(matches = fetchedMatches)
            }
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            MainActor.run { ->
                print("Fehler beim Scrapen: ${error}")
                this.errorMessage = "Fehler beim Laden: ${error.localizedDescription}"
                this.isLoading = false
                // Versuch lokale Daten zu laden
                this.loadFromLocal()
            }
        }
    }

    open fun fetchLineups() {
        isLoading = true
        errorMessage = null

        Task { -> fetchLineupsAsync() }
    }

    // Helper für Normalisierung (entfernt Akzente und Sonderzeichen)
    private fun normalize(text: String): String {
        // Manuelle Transliteration für deutsche Umlaute (da IDs oft ae/oe/ue nutzen)
        val manualReplacement = text.lowercased()
            .replacingOccurrences(of = "ä", with = "ae")
            .replacingOccurrences(of = "ö", with = "oe")
            .replacingOccurrences(of = "ü", with = "ue")
            .replacingOccurrences(of = "ß", with = "ss")
            .replacingOccurrences(of = "ć", with = "c")
            .replacingOccurrences(of = "č", with = "c")
            .replacingOccurrences(of = "š", with = "s")
            .replacingOccurrences(of = "ž", with = "z")
            .replacingOccurrences(of = "đ", with = "d")

        // Android: Einfache Normalisierung ohne Diacritic Removal (da folding nicht verfügbar)
        return manualReplacement
            .replacingOccurrences(of = "-", with = " ")
            .trimmingCharacters(in_ = CharacterSet.whitespacesAndNewlines)
    }

    // MARK: - Matching Logic

    open fun getLigainsiderPlayer(firstName: String, lastName: String): LigainsiderPlayer? {
        // Check if cache has any data (not just matches)
        if (playerCache.isEmpty) {
            print("[MATCHING] ❌ Cache EMPTY for ${firstName} ${lastName}")
            return null
        }

        val normalizedLastName = normalize(lastName)
        val normalizedFirstName = normalize(firstName)
        print("[MATCHING] 🔍 Searching: '${firstName}' '${lastName}' (normalized: '${normalizedFirstName}' '${normalizedLastName}') in cache of ${playerCache.count} players")

        // First try: exact lastName match (as separate word)
        val candidates = playerCache.filter l@{ (key, _) ->
            val normalizedKey = normalize(key)
            // Split by space and underscore to get name parts
            val keyParts = normalizedKey.components(separatedBy = CharacterSet(charactersIn = " _-"))
            return@l keyParts.contains(normalizedLastName)
        }

        print("   → Step 1: Found ${candidates.count} candidates by last name '${normalizedLastName}'")
        if (candidates.count > 0 && candidates.count <= 3) {
            candidates.forEach { it -> print("[Ligainsider]   - ${it.key}") }
        }

        if (candidates.count == 1) {
            print("[Ligainsider] FOUND (exact): ${firstName} ${lastName} -> ${candidates.first?.key ?: ""}")
            return candidates.first?.element1
        } else if (candidates.count > 1) {
            // Multiple matches: use firstName to disambiguate
            val bestMatch = candidates.first(where = l@{ (key, _) ->
                val normalizedKey = normalize(key)
                val keyParts = normalizedKey.components(separatedBy = CharacterSet(charactersIn = " _-"))
                return@l keyParts.contains(normalizedFirstName)
            })
            bestMatch?.let { match ->
                print("[Ligainsider] FOUND (firstName disamb): ${firstName} ${lastName} -> ${match.key}")
                return match.element1
            }
            // If no firstName match, try to find one where lastName is first in the key
            val firstLastNameMatch = candidates.first(where = l@{ (key, _) ->
                val normalizedKey = normalize(key)
                return@l normalizedKey.hasPrefix(normalizedLastName) || normalizedKey.hasPrefix(normalizedFirstName)
            })
            firstLastNameMatch?.let { match ->
                print("[Ligainsider] FOUND (prefix match): ${firstName} ${lastName} -> ${match.key}")
                return match.element1
            }
            candidates.first?.let { match ->
                print("[Ligainsider] FOUND (first of many): ${firstName} ${lastName} -> ${match.key}")
                return match.element1
            }
        }

        // Fallback: loose contains matching (for partial names)
        print("[Ligainsider] Step2: Trying loose contains matching for '${normalizedLastName}'")
        val looseCandidates = playerCache.filter l@{ (key, _) ->
            val normalizedKey = normalize(key)
            return@l normalizedKey.contains(normalizedLastName)
        }

        print("[Ligainsider] Found ${looseCandidates.count} loose candidates")
        if (looseCandidates.count == 1) {
            print("[Ligainsider] FOUND (loose exact): ${firstName} ${lastName} -> ${looseCandidates.first?.key ?: ""}")
            return looseCandidates.first?.element1
        } else if (looseCandidates.count > 1) {
            val bestMatch = looseCandidates.first(where = l@{ (key, _) ->
                val normalizedKey = normalize(key)
                return@l normalizedKey.contains(normalizedFirstName)
            })
            bestMatch?.let { match ->
                print("[Ligainsider] FOUND (loose firstName): ${firstName} ${lastName} -> ${match.key}")
                return match.element1
            }
            print("[Ligainsider] NOT FOUND: Multiple loose matches and no firstName match")
        }

        print("[Ligainsider] NOT FOUND: ${firstName} ${lastName}")
        return null
    }

    open fun getPlayerStatus(firstName: String, lastName: String): LigainsiderStatus {
        if (matches.isEmpty) {
            return LigainsiderStatus.out // Noch keine Daten
        } // Noch keine Daten

        val normalizedLastName = normalize(lastName)
        val normalizedFirstName = normalize(firstName)

        // 1. Zuerst prüfen ob Spieler in alternativeNames ist (wichtig für korrekte Statusanzeige)
        val isAlternative = alternativeNames.contains l@{ altName ->
            val normalizedAlt = normalize(altName)
            return@l normalizedLastName == normalizedAlt || normalizedAlt.contains(normalizedLastName)
        }

        if (isAlternative) {
            return LigainsiderStatus.isAlternative
        }

        // 2. Suche im Cache via ID (bester Match)
        // Strategie: Wir filtern Cache Keys die den normalisierten Nachnamen enthalten

        var foundPlayer: LigainsiderPlayer? = null
        var foundId: String? = null

        val candidates = playerCache.filter l@{ (key, _) ->
            val normalizedKey = normalize(key) // key ist z.B. "adam-dzwigala_25807"
            return@l normalizedKey.contains(normalizedLastName)
        }

        if (candidates.count == 1) {
            foundPlayer = candidates.first?.element1
            foundId = candidates.first?.element0
        } else if (candidates.count > 1) {
            val bestMatch = candidates.first(where = l@{ (key, _) ->
                val normalizedKey = normalize(key)
                return@l normalizedKey.contains(normalizedFirstName)
            })
            // Fallback: Lockereres Matching bei Statusabfrage
            (bestMatch ?: candidates.first)?.let { match ->
                foundPlayer = match.element1
                foundId = match.element0
            }
        }

        // Wenn Spieler gefunden: Check Status
        foundPlayer?.let { player ->
            foundId?.let { id ->
                // Prüfe ob Spieler in der Startelf ist
                if (startingLineupIds.contains(id)) {
                    // Spieler ist in Startelf
                    if (player.alternative != null) {
                        return LigainsiderStatus.startWithAlternative
                    }
                    return LigainsiderStatus.likelyStart
                } else {
                    // Spieler ist im Kader aber nicht in Startelf -> Bank
                    return LigainsiderStatus.bench
                }
            }
        }

        return LigainsiderStatus.out
    }

    // Helper für Views
    open fun getIcon(for_: LigainsiderStatus): String {
        val status = for_
        when (status) {
            LigainsiderStatus.likelyStart -> return "checkmark.circle.fill"
            LigainsiderStatus.startWithAlternative -> return "1.circle.fill"
            LigainsiderStatus.isAlternative -> return "2.circle.fill"
            LigainsiderStatus.bench -> return "person.fill.badge.minus"
            LigainsiderStatus.out -> return "xmark.circle.fill"
        }
    }

    open fun getColor(for_: LigainsiderStatus): Color {
        val status = for_
        when (status) {
            LigainsiderStatus.likelyStart -> return Color.green
            LigainsiderStatus.startWithAlternative -> return Color.orange
            LigainsiderStatus.isAlternative -> return Color.orange
            LigainsiderStatus.bench -> return Color.gray
            LigainsiderStatus.out -> return Color.red
        }
    }

    // MARK: - Squad Scraping

    private val teamSquadPaths: Dictionary<String, String> = dictionaryOf(
        Tuple2("FC Bayern München", "/fc-bayern-muenchen/1/kader/"),
        Tuple2("Borussia Dortmund", "/borussia-dortmund/14/kader/"),
        Tuple2("RB Leipzig", "/rb-leipzig/43/kader/"),
        Tuple2("Bayer 04 Leverkusen", "/bayer-04-leverkusen/4/kader/"),
        Tuple2("VfB Stuttgart", "/vfb-stuttgart/11/kader/"),
        Tuple2("Eintracht Frankfurt", "/eintracht-frankfurt/5/kader/"),
        Tuple2("VfL Wolfsburg", "/vfl-wolfsburg/24/kader/"),
        Tuple2("SC Freiburg", "/sc-freiburg/8/kader/"),
        Tuple2("1. FC Heidenheim", "/1-fc-heidenheim-1846/1376/kader/"),
        Tuple2("Werder Bremen", "/werder-bremen/6/kader/"),
        Tuple2("FC Augsburg", "/fc-augsburg/80/kader/"),
        Tuple2("TSG Hoffenheim", "/tsg-hoffenheim/30/kader/"),
        Tuple2("1. FSV Mainz 05", "/1-fsv-mainz-05/16/kader/"),
        Tuple2("Borussia M'gladbach", "/borussia-moenchengladbach/13/kader/"),
        Tuple2("1. FC Union Berlin", "/1-fc-union-berlin/62/kader/"),
        Tuple2("VfL Bochum", "/vfl-bochum/29/kader/"),
        Tuple2("FC St. Pauli", "/fc-st-pauli/20/kader/"),
        Tuple2("Holstein Kiel", "/holstein-kiel/321/kader/")
    )

    // MARK: - Async variant for initialization (waits for completion)
    open suspend fun fetchAllSquadsAsync(): Unit = Async.run {
        print("Starte Kader-Abruf für alle Teams...")
        withTaskGroup(of = Array::class) { group ->
            for ((teamName, path) in teamSquadPaths.sref()) {
                group.addTask l@{ -> return@l this.fetchSquad(path = path, teamName = teamName) }
            }

            var accumulatedPlayers: Array<LigainsiderPlayer> = arrayOf()
            for (squad in group.sref()) {
                // Start manually iterating to avoid Sequence type issues
                for (player in squad.sref()) {
                    val p = player as LigainsiderPlayer
                    accumulatedPlayers.append(p)
                }
            }

            MainActor.run { ->
                print("Kader-Abruf beendet. Gefundene Spieler: ${accumulatedPlayers.count}")
                for (player in accumulatedPlayers.sref()) {
                    player.ligainsiderId?.let { id ->
                        // Update playerCache safely
                        val matchtarget_2 = this.playerCache[id]
                        if (matchtarget_2 != null) {
                            val existing = matchtarget_2
                            // Update image if we found one and existing didn't have one
                            if (existing.imageUrl == null && player.imageUrl != null) {
                                this.playerCache[id] = LigainsiderPlayer(name = existing.name, alternative = existing.alternative, ligainsiderId = existing.ligainsiderId, imageUrl = player.imageUrl)
                            }
                        } else {
                            this.playerCache[id] = player
                        }
                    }
                }
            }
        }
    }

    open fun fetchAllSquads() {
        Task { -> fetchAllSquadsAsync() }
    }

    private suspend fun fetchSquad(path: String, teamName: String): Array<LigainsiderPlayer> = Async.run l@{
        val fullUrl = "https://www.ligainsider.de" + path
        val url_0 = (try { URL(string = fullUrl) } catch (_: NullReturnException) { null })
        if (url_0 == null) {
            return@l arrayOf()
        }

        try {
            val (data, _) = this.session.data(from = url_0)
            val html_0 = String(data = data, encoding = StringEncoding.utf8)
            if (html_0 == null) {
                return@l arrayOf()
            }

            var players: Array<LigainsiderPlayer> = arrayOf()
            // Robustere Suche: Wir suchen nach allen Links auf Spielerprofile
            // Pattern: href="/(name)_(id)/"

            val components = html_0.components(separatedBy = "href=\"/")

            // Wir iterieren mit Index, um auf vorherige Komponenten zugreifen zu können (für Bilder davor)
            for (i in 1 until components.count) {
                val component = components[i]
                val previousComponent = components[i - 1]

                // 1. Slug extrahieren
                val rawSlug = component.substringBefore("\"") ?: ""
                // Remove query string and any trailing slashes
                var slug = rawSlug
                slug.firstIndex(of = '?')?.let { qIndex ->
                    slug = String(slug[Int.min until qIndex])
                }
                slug = slug.trimmingCharacters(in_ = CharacterSet(charactersIn = "/"))

                // Validierung
                if (!slug.contains("_")) {
                    continue
                }
                if (slug.contains("/")) {
                    continue
                }
                val lastChar = slug.last ?: ' '
                if (!"0123456789".contains(lastChar)) {
                    continue
                }
                val rawName_0 = component.substringBetween(">", "</a>")
                if (rawName_0 == null) {
                    continue
                }
                val name = removeHtmlTags(rawName_0).trimmingCharacters(in_ = CharacterSet.whitespacesAndNewlines)

                if (name.isEmpty || name.count > 50) {
                    continue
                }

                // 3. Bild suchen
                var imageUrl: String? = null

                // A) Im Link selbst (contentInLink ist rawName content)
                val contentInLink = rawName_0
                if (contentInLink.contains("<img")) {
                    contentInLink.substringBetween("src=\"", "\"")?.let { extracted ->
                        if (extracted.contains("ligainsider.de")) {
                            imageUrl = extracted
                        }
                    }
                }

                // B) Kurz vor dem Link (im previousComponent)
                if (imageUrl == null) {
                    // Simple Suche von hinten im Previous Component
                    // Wir suchen das letzte Vorkommen von src="..." das ligainsider.de enthält
                    // Da findLastRange komplex ist, splitten wir einfach nach src=" und nehmen das letzte was passt
                    val parts = previousComponent.components(separatedBy = "src=\"")
                    if (parts.count > 1) {
                        for (part in parts.reversed()) {
                            part.substringBefore("\"")?.let { candidate ->
                                if (candidate.contains("ligainsider.de")) {
                                    imageUrl = candidate
                                    break
                                }
                            }
                        }
                    }
                }

                if (!players.contains(where = { it -> it.ligainsiderId == slug })) {
                    players.append(LigainsiderPlayer(name = name, alternative = null, ligainsiderId = slug, imageUrl = imageUrl))
                }
            }

            return@l players.sref()
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("Fehler Kader ${teamName}: ${error}")
            return@l arrayOf()
        }
    }

    // MARK: - Native Swift Scraping

    private suspend fun scrapeLigaInsider(): Array<LigainsiderMatch> = Async.run l@{
        // 1. Übersicht laden
        print("Lade Übersicht von: ${overviewURL}")
        val url_1 = (try { URL(string = overviewURL) } catch (_: NullReturnException) { null })
        if (url_1 == null) {
            throw URLError(URLError.Code.badURL)
        }
        val (data, _) = this.session.data(from = url_1)
        val htmlString_0 = String(data = data, encoding = StringEncoding.utf8)
        if (htmlString_0 == null) {
            throw URLError(URLError.Code.cannotDecodeContentData)
        }

        // 2. Spiel-Links extrahieren (String-Parsing statt Regex für Skip-Support)
        var matchLinks: Array<String> = arrayOf()

        // Wir suchen nach href="/bundesliga/team/.../saison-..."
        val components = htmlString_0.splitBy("href=\"")
        for (component in components.dropFirst()) {
            // component beginnt mit dem URL-Pfad
            component.firstIndex(of = '\"')?.let { quoteIndex ->
                val path = String(component[Int.min until quoteIndex])

                if (path.contains("/bundesliga/team/") && path.contains("/saison-")) {
                    val fullUrl = "https://www.ligainsider.de" + path
                    if (!matchLinks.contains(fullUrl)) {
                        matchLinks.append(fullUrl)
                    }
                }
            }
        }

        // Match Pairs bilden
        var matchPairs: Array<Array<String>> = arrayOf()
        var currentPair: Array<String> = arrayOf()

        for (link in matchLinks.sref()) {
            currentPair.append(link)
            if (currentPair.count == 2) {
                matchPairs.append(currentPair)
                currentPair = arrayOf()
            }
        }

        // 3. Details laden (parallel für bessere Performance)
        print("Starte paralleles Laden der Match-Details...")
        return@l withTaskGroup(of = LigainsiderMatch?::class) l@{ group ->
            for (pair in matchPairs.sref()) {
                group.addTask l@{ ->
                    val homeUrl = pair[0]
                    val awayUrl = pair[1]

                    try {
                        val home = this.fetchTeamData(url = homeUrl)
                        val away = this.fetchTeamData(url = awayUrl)

                        return@l LigainsiderMatch(homeTeam = home.name, awayTeam = away.name, homeLogo = home.logo, awayLogo = away.logo, homeLineup = home.lineup.map { it -> LineupRow(players = it) }, awayLineup = away.lineup.map { it -> LineupRow(players = it) }, homeSquad = home.squad, awaySquad = away.squad, url = homeUrl)
                    } catch (error: Throwable) {
                        @Suppress("NAME_SHADOWING") val error = error.aserror()
                        print("Fehler bei Match Paar: ${error}")
                        return@l null
                    }
                }
            }

            var finalMatches: Array<LigainsiderMatch> = arrayOf()
            for (match in group.sref()) {
                if (match != null) {
                    finalMatches.append(match)
                }
            }
            return@l finalMatches
        }
    }

    private class TeamDataResult {
        internal val name: String
        internal val logo: String?
        internal val lineup: Array<Array<LigainsiderPlayer>>
        internal val squad: Array<LigainsiderPlayer>

        constructor(name: String, logo: String? = null, lineup: Array<Array<LigainsiderPlayer>>, squad: Array<LigainsiderPlayer>) {
            this.name = name
            this.logo = logo
            this.lineup = lineup.sref()
            this.squad = squad.sref()
        }
    }

    private suspend fun fetchTeamData(url: String): LigainsiderService.TeamDataResult = Async.run l@{
        print("Lade Team Details: ${url}")
        val urlObj_0 = (try { URL(string = url) } catch (_: NullReturnException) { null })
        if (urlObj_0 == null) {
            return@l TeamDataResult(name = "Unbekannt", logo = null, lineup = arrayOf(), squad = arrayOf())
        }
        val (data, _) = this.session.data(from = urlObj_0)
        val html_1 = String(data = data, encoding = StringEncoding.utf8)
        if (html_1 == null) {
            return@l TeamDataResult(name = "Unbekannt", logo = null, lineup = arrayOf(), squad = arrayOf())
        }

        // Teamnamen extrahieren
        var teamName = "Team"
        var teamLogo: String? = null

        // Suche nach: <h2 ... itemprop="name">Team Name</h2>
        val nameComponents = html_1.splitBy("itemprop=\"name\"")
        if (nameComponents.count > 1) {
            // Logo suchen im Teil davor (letzte src="..." vor dem Namen)
            // Struktur: <div ...><a ...><img src="..."></a></div> ... <h2 ... itemprop="name">
            val partBeforeName = nameComponents[0]
            // Wir nutzen last components logic um das letzte bild zu finden?
            // "src=\"" explizit suchen
            val srcParts = partBeforeName.components(separatedBy = "src=\"")
            if (srcParts.count > 1) {
                srcParts.last?.let { lastPart ->
                    lastPart.substringBefore("\"")?.let { logoUrl ->
                        if (logoUrl.contains("ligainsider.de") && (logoUrl.contains("wappen") || logoUrl.contains("images/teams"))) {
                            teamLogo = logoUrl
                        }
                    }
                }
            }

            val partAfterName = nameComponents[1] // > Team name </h2> ...
            partAfterName.substringBetween(">", "</h2>")?.let { name ->
                teamName = name.trimmingCharacters(in_ = CharacterSet.whitespacesAndNewlines)
            }
        } else {
            html_1.substringBetween("<title>", "</title>")?.let { title ->
                teamName = title.components(separatedBy = "|").first?.trimmingCharacters(in_ = CharacterSet.whitespacesAndNewlines) ?: "Team"
            }
        }

        var formationRows: Array<Array<LigainsiderPlayer>> = arrayOf()
        var allParsedPlayers: Array<LigainsiderPlayer> = arrayOf() // Sammelt ALLE gefundenen Spieler (inkl. Alternativen)

        // Parsing Logik für Aufstellung
        // Support for both Pre-Match (VORAUSSICHTLICHE AUFSTELLUNG) and Live/Post-Match (Voraussichtliche Aufstellung und Ergebnisse) headers
        val headerMarkers = arrayOf("VORAUSSICHTLICHE AUFSTELLUNG", "Voraussichtliche Aufstellung")
        var contentStart: String? = null

        for (marker in headerMarkers.sref()) {
            html_1.substringAfter(marker)?.let { found ->
                contentStart = found
                break
            }
        }

        if (contentStart != null) {
            val limit = min(100000, contentStart.count)
            val searchArea = String(contentStart.prefix(limit))

            // Falls das Ende der Aufstellung durch die Legende markiert ist, schneiden wir dort ab
            // Das verhindert, dass Links aus dem Footer (News, Kommentare) fälschlicherweise als Spieler erkannt werden
            var cleanSearchArea = searchArea
            cleanSearchArea.substringBefore("Spieler stand in der Startelf")?.let { beforeLegend ->
                cleanSearchArea = beforeLegend
            }

            // Aufteilen in Rows
            val rowComponents = cleanSearchArea.splitBy("player_position_row")

            for (i in 1 until rowComponents.count) {
                val rowHtml = rowComponents[i]
                if (!rowHtml.contains("player_position_column")) {
                    continue
                }

                var currentRowPlayers: Array<LigainsiderPlayer> = arrayOf()
                val colComponents = rowHtml.splitBy("player_position_column")

                for (j in 1 until colComponents.count) {
                    val colHtml = colComponents[j]

                    // Extrahiere ALLE Bild URLs in dieser Spalte
                    // Struktur: main player photo + sub_pic photos für Alternativen
                    var allImageUrls: Array<String> = arrayOf()
                    val srcComponents = colHtml.splitBy("src=\"")
                    for (srcPart in srcComponents.dropFirst()) {
                        srcPart.substringBefore("\"")?.let { urlCandidate ->
                            // Akzeptiere Bilder die "ligainsider.de" enthalten und player/team Pfad haben
                            if (urlCandidate.contains("ligainsider.de") && urlCandidate.contains("/player/team/")) {
                                allImageUrls.append(urlCandidate)
                            }
                        }
                    }

                    // Suche: <a ... href="/id/" ... >Name</a>
                    val linkComponents = colHtml.splitBy("<a ")

                    var namesInColumn: Array<Tuple3<String, String, String?>> = arrayOf()
                    var seen = Set<String>()
                    var usedImageIndices = Set<Int>() // Track which images we've already matched

                    for (linkPart in linkComponents.dropFirst()) {
                        val slug_0 = linkPart.substringBetween("href=\"/", "/\"")
                        if (slug_0 == null) {
                            continue
                        }

                        // Slug muss ID enthalten (z.B. name_12345)
                        if (!slug_0.contains("_")) {
                            continue
                        }
                        // Darf keine Slashes enthalten (wären Sub-Pfade wie News)
                        if (slug_0.contains("/")) {
                            continue
                        }

                        // Check if last char is digit (Skip workaround for .isNumber)
                        slug_0.last?.let { last ->
                            if (!"0123456789".contains(last)) {
                                continue
                            }
                        }
                        val rawName_1 = linkPart.substringBetween(">", "</a>")
                        if (rawName_1 == null) {
                            continue
                        }

                        // HTML Tags entfernen bevor getrimmt wird
                        val clearName = removeHtmlTags(rawName_1)
                        var name = clearName.trimmingCharacters(in_ = CharacterSet.whitespacesAndNewlines)

                        // Fallback: Wenn Name leer ist (z.B. bei Live-Ansicht wo nur ein IMG Tag drin ist), versuche title/alt attribute zu lesen
                        if (name.isEmpty) {
                            val matchtarget_3 = rawName_1.substringBetween("title=\"", "\"")
                            if (matchtarget_3 != null) {
                                val title = matchtarget_3
                                name = title
                            } else {
                                rawName_1.substringBetween("alt=\"", "\"")?.let { alt ->
                                    name = alt
                                }
                            }
                        }

                        if (name.isEmpty || name.count > 50) {
                            continue
                        }

                        if (name.count > 1 && !seen.contains(name)) {
                            seen.insert(name)

                            // Versuche das passende Bild für diesen Spieler zu finden
                            var matchedImageUrl: String? = null
                            var matchedIndex: Int? = null

                            // Suche in den verfügbaren Bildern nach einem Match basierend auf dem slug
                            // Kombiniert exaktes Matching und Fallback in einer Iteration für bessere Performance
                            var firstAvailableIndex: Int? = null
                            for ((index, imageUrl) in allImageUrls.enumerated()) {
                                // Skip if we've already used this image
                                if (usedImageIndices.contains(index)) {
                                    continue
                                }

                                // Speichere den ersten verfügbaren Index als Fallback
                                if (firstAvailableIndex == null) {
                                    firstAvailableIndex = index
                                }

                                val normalizedImageUrl = normalize(imageUrl)
                                val normalizedSlug = normalize(slug_0)

                                // Extrahiere nur den Namen-Teil des Slugs (vor dem Underscore)
                                val slugNamePart = normalizedSlug.components(separatedBy = "_").first ?: normalizedSlug

                                // Prüfe ob der Spielername im Bild-URL vorkommt (z.B. "lars-ritzka" in "lars-ritzka-pauli-25-26.jpg")
                                if (normalizedImageUrl.contains(slugNamePart)) {
                                    matchedImageUrl = imageUrl
                                    matchedIndex = index
                                    break
                                }
                            }

                            // Fallback: Verwende das erste verfügbare ungenutzte Bild wenn kein Name-Match gefunden wurde
                            if (matchedImageUrl == null) {
                                firstAvailableIndex?.let { fallbackIndex ->
                                    matchedImageUrl = allImageUrls[fallbackIndex]
                                    matchedIndex = fallbackIndex
                                }
                            }

                            // Mark the image as used if we found one
                            matchedIndex?.let { index ->
                                usedImageIndices.insert(index)
                            }

                            namesInColumn.append(Tuple3(name, slug_0, matchedImageUrl))
                        }
                    }

                    namesInColumn.first?.let { firstEntry ->
                        val mainName = firstEntry.name
                        val mainSlug = firstEntry.slug
                        val mainImageUrl = firstEntry.imageUrl
                        val alternativeName = if (namesInColumn.count > 1) namesInColumn[1].name else null
                        currentRowPlayers.append(LigainsiderPlayer(name = mainName, alternative = alternativeName, ligainsiderId = mainSlug, imageUrl = mainImageUrl))
                    }

                    // Füge ALLE Spieler (Haupt + Alternativen) zur allParsedPlayers Liste hinzu
                    // WICHTIG: Für den Hauptspieler muss das 'alternative' Feld erhalten bleiben für korrekte Statusanzeige
                    // Alternativen werden als separate Spieler ohne alternative-Link gespeichert
                    for ((index, playerData) in namesInColumn.enumerated()) {
                        val isMainPlayer = (index == 0)
                        val alternativeField = if (isMainPlayer && namesInColumn.count > 1) namesInColumn[1].name else null

                        allParsedPlayers.append(LigainsiderPlayer(name = playerData.name, alternative = alternativeField, ligainsiderId = playerData.slug, imageUrl = playerData.imageUrl))
                    }
                }

                if (!currentRowPlayers.isEmpty) {
                    formationRows.append(currentRowPlayers)
                }
            }
        }

        // Kader laden: Kombiniere geparste Spieler mit fetchSquad (für zusätzliche Spieler die nicht in Aufstellung sind)
        val path = (try { URL(string = url) } catch (_: NullReturnException) { null })?.path ?: ""
        var fetchedSquad = fetchSquad(path = path, teamName = teamName)

        // Merge allParsedPlayers mit fetchedSquad
        // Strategie: Priorisiere Spieler mit Bildern aus allParsedPlayers (Aufstellungsseite),
        // behalte aber fetchedSquad-Einträge für Spieler die nicht in der Aufstellung sind
        var squadMap: Dictionary<String, LigainsiderPlayer> = dictionaryOf()

        // Zuerst fetchedSquad einfügen (Basis-Daten)
        for (player in fetchedSquad.sref()) {
            player.ligainsiderId?.let { id ->
                squadMap[id] = player
            }
        }

        // Dann allParsedPlayers einfügen (überschreibt nur wenn wir bessere Daten haben)
        for (player in allParsedPlayers.sref()) {
            player.ligainsiderId?.let { id ->
                val matchtarget_4 = squadMap[id]
                if (matchtarget_4 != null) {
                    val existingPlayer = matchtarget_4
                    // Überschreibe nur wenn der neue Spieler ein Bild hat und der alte nicht
                    if (player.imageUrl != null && existingPlayer.imageUrl == null) {
                        squadMap[id] = player
                    } else if (player.imageUrl != null) {
                        squadMap[id] = player
                    }
                } else {
                    // Neuer Spieler, füge hinzu
                    squadMap[id] = player
                }
            }
        }

        val finalSquad = Array(squadMap.values)

        return@l TeamDataResult(name = teamName, logo = teamLogo, lineup = formationRows, squad = finalSquad)
    }

    // Helper zum Entfernen von HTML Tags
    private fun removeHtmlTags(text: String): String {
        var result = ""
        var insideTag = false

        for (char in text) {
            if (char == '<') {
                insideTag = true
            } else if (char == '>') {
                insideTag = false
            } else if (!insideTag) {
                result += String(char)
            }
        }
        return result
    }

    // Backup (Lokal Speichern)
    private fun saveToLocal(matches: Array<LigainsiderMatch>) = Unit

    private fun loadFromLocal() = Unit

    @androidx.annotation.Keep
    companion object: CompanionClass() {
    }
    open class CompanionClass {
    }
}
