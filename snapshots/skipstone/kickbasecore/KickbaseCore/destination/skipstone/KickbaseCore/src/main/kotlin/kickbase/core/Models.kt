package kickbase.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import skip.lib.*
import skip.lib.Array

import skip.foundation.*
import skip.ui.*
import skip.model.*

// MARK: - Authentication Models
@androidx.annotation.Keep
class LoginRequest: Codable {
    val em: String // email
    val pass: String // password
    val loy: Boolean // loyalty (keep logged in)
    val rep: Dictionary<String, String> // empty rep object

    constructor(email: String, password: String, loyalty: Boolean = false, rep: Dictionary<String, String> = dictionaryOf()) {
        this.em = email
        this.pass = password
        this.loy = loyalty
        this.rep = rep.sref()
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        em("em"),
        pass("pass"),
        loy("loy"),
        rep("rep");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "em" -> CodingKeys.em
                    "pass" -> CodingKeys.pass
                    "loy" -> CodingKeys.loy
                    "rep" -> CodingKeys.rep
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(em, forKey = CodingKeys.em)
        container.encode(pass, forKey = CodingKeys.pass)
        container.encode(loy, forKey = CodingKeys.loy)
        container.encode(rep, forKey = CodingKeys.rep)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.em = container.decode(String::class, forKey = CodingKeys.em)
        this.pass = container.decode(String::class, forKey = CodingKeys.pass)
        this.loy = container.decode(Boolean::class, forKey = CodingKeys.loy)
        this.rep = container.decode(Dictionary::class, keyType = String::class, valueType = String::class, forKey = CodingKeys.rep)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<LoginRequest> {
        override fun init(from: Decoder): LoginRequest = LoginRequest(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class LoginResponse: Codable {
    val tkn: String // token
    val user: User? // Optional, da es möglicherweise nicht in der Response ist

    // Alternative Felder, die möglicherweise in der Response sind
    val leagues: Array<League>?
    val userId: String?

    constructor(tkn: String, user: User?, leagues: Array<League>?, userId: String?) {
        this.tkn = tkn
        this.user = user
        this.leagues = leagues.sref()
        this.userId = userId
    }

    internal enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        tkn("tkn"),
        user("user"),
        leagues("leagues"),
        userId("userId");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): LoginResponse.CodingKeys? {
                return when (rawValue) {
                    "tkn" -> CodingKeys.tkn
                    "user" -> CodingKeys.user
                    "leagues" -> CodingKeys.leagues
                    "userId" -> CodingKeys.userId
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(tkn, forKey = CodingKeys.tkn)
        container.encodeIfPresent(user, forKey = CodingKeys.user)
        container.encodeIfPresent(leagues, forKey = CodingKeys.leagues)
        container.encodeIfPresent(userId, forKey = CodingKeys.userId)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.tkn = container.decode(String::class, forKey = CodingKeys.tkn)
        this.user = container.decodeIfPresent(User::class, forKey = CodingKeys.user)
        this.leagues = container.decodeIfPresent(Array::class, elementType = League::class, forKey = CodingKeys.leagues)
        this.userId = container.decodeIfPresent(String::class, forKey = CodingKeys.userId)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<LoginResponse> {
        override fun init(from: Decoder): LoginResponse = LoginResponse(from = from)

        internal fun CodingKeys(rawValue: String): LoginResponse.CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class User: Codable, Identifiable<String> {
    override val id: String
    val name: String
    val teamName: String
    val email: String
    val budget: Int
    val teamValue: Int
    val points: Int
    val placement: Int
    val flags: Int

    // Alternative gekürzte Feldnamen aus der API
    internal enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        id("i"),
        name_("n"),
        teamName("tn"),
        email("em"),
        budget("b"),
        teamValue("tv"),
        points("p"),
        placement("pl"),
        flags("f");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): User.CodingKeys? {
                return when (rawValue) {
                    "i" -> CodingKeys.id
                    "n" -> CodingKeys.name_
                    "tn" -> CodingKeys.teamName
                    "em" -> CodingKeys.email
                    "b" -> CodingKeys.budget
                    "tv" -> CodingKeys.teamValue
                    "p" -> CodingKeys.points
                    "pl" -> CodingKeys.placement
                    "f" -> CodingKeys.flags
                    else -> null
                }
            }
        }
    }

    // Fallback-Initialisierung für fehlende Werte
    constructor(from: Decoder) {
        val decoder = from
        val container = decoder.container(keyedBy = CodingKeys::class)

        // Versuche zuerst die verkürzten Namen, dann die langen
        id = container.decodeIfPresent(String::class, forKey = User.CodingKeys.id) ?: ""
        name = container.decodeIfPresent(String::class, forKey = User.CodingKeys.name_) ?: ""
        teamName = container.decodeIfPresent(String::class, forKey = User.CodingKeys.teamName) ?: ""
        email = container.decodeIfPresent(String::class, forKey = User.CodingKeys.email) ?: ""
        budget = container.decodeIfPresent(Int::class, forKey = User.CodingKeys.budget) ?: 0
        teamValue = container.decodeIfPresent(Int::class, forKey = User.CodingKeys.teamValue) ?: 0
        points = container.decodeIfPresent(Int::class, forKey = User.CodingKeys.points) ?: 0
        placement = container.decodeIfPresent(Int::class, forKey = User.CodingKeys.placement) ?: 0
        flags = container.decodeIfPresent(Int::class, forKey = User.CodingKeys.flags) ?: 0
    }

    // Standard-Initialisierung für manuelle Erstellung
    constructor(id: String, name: String, teamName: String, email: String, budget: Int, teamValue: Int, points: Int, placement: Int, flags: Int) {
        this.id = id
        this.name = name
        this.teamName = teamName
        this.email = email
        this.budget = budget
        this.teamValue = teamValue
        this.points = points
        this.placement = placement
        this.flags = flags
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(id, forKey = CodingKeys.id)
        container.encode(name, forKey = CodingKeys.name_)
        container.encode(teamName, forKey = CodingKeys.teamName)
        container.encode(email, forKey = CodingKeys.email)
        container.encode(budget, forKey = CodingKeys.budget)
        container.encode(teamValue, forKey = CodingKeys.teamValue)
        container.encode(points, forKey = CodingKeys.points)
        container.encode(placement, forKey = CodingKeys.placement)
        container.encode(flags, forKey = CodingKeys.flags)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<User> {
        override fun init(from: Decoder): User = User(from = from)

        internal fun CodingKeys(rawValue: String): User.CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

// MARK: - League Models
@androidx.annotation.Keep
class League: Codable, Identifiable<String> {
    override val id: String
    val competitionId: String
    val name: String
    val creatorName: String
    val adminName: String
    val created: String
    val season: String
    val matchDay: Int
    val currentUser: LeagueUser

    internal enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        id("i"),
        competitionId("cpi"),
        name_("n"),
        creatorName("cn"),
        adminName("an"),
        created("c"),
        season("s"),
        matchDay("md"),
        currentUser("cu");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): League.CodingKeys? {
                return when (rawValue) {
                    "i" -> CodingKeys.id
                    "cpi" -> CodingKeys.competitionId
                    "n" -> CodingKeys.name_
                    "cn" -> CodingKeys.creatorName
                    "an" -> CodingKeys.adminName
                    "c" -> CodingKeys.created
                    "s" -> CodingKeys.season
                    "md" -> CodingKeys.matchDay
                    "cu" -> CodingKeys.currentUser
                    else -> null
                }
            }
        }
    }

    constructor(from: Decoder) {
        val decoder = from
        val container = decoder.container(keyedBy = CodingKeys::class)
        id = container.decode(String::class, forKey = League.CodingKeys.id)
        // Default to "1" (Bundesliga) if missing
        competitionId = container.decodeIfPresent(String::class, forKey = League.CodingKeys.competitionId) ?: "1"
        name = container.decode(String::class, forKey = League.CodingKeys.name_)
        creatorName = container.decode(String::class, forKey = League.CodingKeys.creatorName)
        adminName = container.decode(String::class, forKey = League.CodingKeys.adminName)
        created = container.decode(String::class, forKey = League.CodingKeys.created)
        season = container.decode(String::class, forKey = League.CodingKeys.season)
        matchDay = container.decode(Int::class, forKey = League.CodingKeys.matchDay)
        currentUser = container.decode(LeagueUser::class, forKey = League.CodingKeys.currentUser)
    }

    // Hashable conformance
    override fun hashCode(): Int {
        var hasher = Hasher()
        hash(into = InOut<Hasher>({ hasher }, { hasher = it }))
        return hasher.finalize()
    }
    fun hash(into: InOut<Hasher>) {
        val hasher = into
        hasher.value.combine(id)
    }

    // Equatable conformance
    override fun equals(other: Any?): Boolean {
        if (other !is League) {
            return false
        }
        val lhs = this
        val rhs = other
        return lhs.id == rhs.id
    }

    constructor(id: String, competitionId: String = "1", name: String, creatorName: String, adminName: String, created: String, season: String, matchDay: Int, currentUser: LeagueUser) {
        this.id = id
        this.competitionId = competitionId
        this.name = name
        this.creatorName = creatorName
        this.adminName = adminName
        this.created = created
        this.season = season
        this.matchDay = matchDay
        this.currentUser = currentUser
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(id, forKey = CodingKeys.id)
        container.encode(competitionId, forKey = CodingKeys.competitionId)
        container.encode(name, forKey = CodingKeys.name_)
        container.encode(creatorName, forKey = CodingKeys.creatorName)
        container.encode(adminName, forKey = CodingKeys.adminName)
        container.encode(created, forKey = CodingKeys.created)
        container.encode(season, forKey = CodingKeys.season)
        container.encode(matchDay, forKey = CodingKeys.matchDay)
        container.encode(currentUser, forKey = CodingKeys.currentUser)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<League> {
        override fun init(from: Decoder): League = League(from = from)

        internal fun CodingKeys(rawValue: String): League.CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class LeagueUser: Codable {
    val id: String
    val name: String
    val teamName: String
    val budget: Int
    val teamValue: Int
    val points: Int
    val placement: Int
    val won: Int
    val drawn: Int
    val lost: Int
    val se11: Int
    val ttm: Int
    val mpst: Int? // Max Players Same Team - Maximale Anzahl Spieler vom gleichen Team
    val lineupPlayerIds: Array<String> // "lp" - Player IDs of the lineup

    constructor(id: String, name: String, teamName: String, budget: Int, teamValue: Int, points: Int, placement: Int, won: Int, drawn: Int, lost: Int, se11: Int, ttm: Int, mpst: Int?, lineupPlayerIds: Array<String> = arrayOf()) {
        this.id = id
        this.name = name
        this.teamName = teamName
        this.budget = budget
        this.teamValue = teamValue
        this.points = points
        this.placement = placement
        this.won = won
        this.drawn = drawn
        this.lost = lost
        this.se11 = se11
        this.ttm = ttm
        this.mpst = mpst
        this.lineupPlayerIds = lineupPlayerIds.sref()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is LeagueUser) return false
        return id == other.id && name == other.name && teamName == other.teamName && budget == other.budget && teamValue == other.teamValue && points == other.points && placement == other.placement && won == other.won && drawn == other.drawn && lost == other.lost && se11 == other.se11 && ttm == other.ttm && mpst == other.mpst && lineupPlayerIds == other.lineupPlayerIds
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, id)
        result = Hasher.combine(result, name)
        result = Hasher.combine(result, teamName)
        result = Hasher.combine(result, budget)
        result = Hasher.combine(result, teamValue)
        result = Hasher.combine(result, points)
        result = Hasher.combine(result, placement)
        result = Hasher.combine(result, won)
        result = Hasher.combine(result, drawn)
        result = Hasher.combine(result, lost)
        result = Hasher.combine(result, se11)
        result = Hasher.combine(result, ttm)
        result = Hasher.combine(result, mpst)
        result = Hasher.combine(result, lineupPlayerIds)
        return result
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        id("id"),
        name_("name"),
        teamName("teamName"),
        budget("budget"),
        teamValue("teamValue"),
        points("points"),
        placement("placement"),
        won("won"),
        drawn("drawn"),
        lost("lost"),
        se11("se11"),
        ttm("ttm"),
        mpst("mpst"),
        lineupPlayerIds("lineupPlayerIds");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "id" -> CodingKeys.id
                    "name" -> CodingKeys.name_
                    "teamName" -> CodingKeys.teamName
                    "budget" -> CodingKeys.budget
                    "teamValue" -> CodingKeys.teamValue
                    "points" -> CodingKeys.points
                    "placement" -> CodingKeys.placement
                    "won" -> CodingKeys.won
                    "drawn" -> CodingKeys.drawn
                    "lost" -> CodingKeys.lost
                    "se11" -> CodingKeys.se11
                    "ttm" -> CodingKeys.ttm
                    "mpst" -> CodingKeys.mpst
                    "lineupPlayerIds" -> CodingKeys.lineupPlayerIds
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(id, forKey = CodingKeys.id)
        container.encode(name, forKey = CodingKeys.name_)
        container.encode(teamName, forKey = CodingKeys.teamName)
        container.encode(budget, forKey = CodingKeys.budget)
        container.encode(teamValue, forKey = CodingKeys.teamValue)
        container.encode(points, forKey = CodingKeys.points)
        container.encode(placement, forKey = CodingKeys.placement)
        container.encode(won, forKey = CodingKeys.won)
        container.encode(drawn, forKey = CodingKeys.drawn)
        container.encode(lost, forKey = CodingKeys.lost)
        container.encode(se11, forKey = CodingKeys.se11)
        container.encode(ttm, forKey = CodingKeys.ttm)
        container.encodeIfPresent(mpst, forKey = CodingKeys.mpst)
        container.encode(lineupPlayerIds, forKey = CodingKeys.lineupPlayerIds)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.id = container.decode(String::class, forKey = CodingKeys.id)
        this.name = container.decode(String::class, forKey = CodingKeys.name_)
        this.teamName = container.decode(String::class, forKey = CodingKeys.teamName)
        this.budget = container.decode(Int::class, forKey = CodingKeys.budget)
        this.teamValue = container.decode(Int::class, forKey = CodingKeys.teamValue)
        this.points = container.decode(Int::class, forKey = CodingKeys.points)
        this.placement = container.decode(Int::class, forKey = CodingKeys.placement)
        this.won = container.decode(Int::class, forKey = CodingKeys.won)
        this.drawn = container.decode(Int::class, forKey = CodingKeys.drawn)
        this.lost = container.decode(Int::class, forKey = CodingKeys.lost)
        this.se11 = container.decode(Int::class, forKey = CodingKeys.se11)
        this.ttm = container.decode(Int::class, forKey = CodingKeys.ttm)
        this.mpst = container.decodeIfPresent(Int::class, forKey = CodingKeys.mpst)
        this.lineupPlayerIds = container.decode(Array::class, elementType = String::class, forKey = CodingKeys.lineupPlayerIds)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<LeagueUser> {
        override fun init(from: Decoder): LeagueUser = LeagueUser(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

// MARK: - Player Models
@androidx.annotation.Keep
class Player: Codable, Identifiable<String> {
    override val id: String
    val firstName: String
    val lastName: String
    val profileBigUrl: String
    val teamName: String
    val teamId: String
    val position: Int
    val number: Int
    val averagePoints: Double
    val totalPoints: Int
    val marketValue: Int
    val marketValueTrend: Int
    val tfhmvt: Int // Marktwertänderung seit letztem Update
    val prlo: Int // Profit/Loss since purchase - Gewinn/Verlust seit Kauf
    val stl: Int // Neues API-Feld
    val status: Int
    val userOwnsPlayer: Boolean

    val fullName: String
        get() = "${firstName} ${lastName}"

    val imageUrl: URL?
        get() {
            if (profileBigUrl.hasPrefix("http")) {
                return (try { URL(string = profileBigUrl) } catch (_: NullReturnException) { null })
            } else if (!profileBigUrl.isEmpty) {
                // Remove leading slash if present to avoid double slashes
                val path = if (profileBigUrl.hasPrefix("/")) String(profileBigUrl.dropFirst()) else profileBigUrl
                return (try { URL(string = "https://kickbase.b-cdn.net/" + path) } catch (_: NullReturnException) { null })
            }
            return null
        }

    // Neue computed property für den vollständigen Teamnamen basierend auf teamId
    val fullTeamName: String
        get() = TeamMapping.getTeamName(for_ = teamId) ?: teamName

    val positionName: String
        get() {
            when (position) {
                1 -> return "TW"
                2 -> return "ABW"
                3 -> return "MF"
                4 -> return "ST"
                else -> return "?"
            }
        }

    val positionColor: Color
        get() {
            when (position) {
                1 -> return Color.yellow // TW
                2 -> return Color.green // ABW
                3 -> return Color.blue // MF
                4 -> return Color.red // ST
                else -> return Color.gray
            }
        }

    constructor(id: String, firstName: String, lastName: String, profileBigUrl: String, teamName: String, teamId: String, position: Int, number: Int, averagePoints: Double, totalPoints: Int, marketValue: Int, marketValueTrend: Int, tfhmvt: Int, prlo: Int, stl: Int, status: Int, userOwnsPlayer: Boolean) {
        this.id = id
        this.firstName = firstName
        this.lastName = lastName
        this.profileBigUrl = profileBigUrl
        this.teamName = teamName
        this.teamId = teamId
        this.position = position
        this.number = number
        this.averagePoints = averagePoints
        this.totalPoints = totalPoints
        this.marketValue = marketValue
        this.marketValueTrend = marketValueTrend
        this.tfhmvt = tfhmvt
        this.prlo = prlo
        this.stl = stl
        this.status = status
        this.userOwnsPlayer = userOwnsPlayer
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        id("id"),
        firstName("firstName"),
        lastName("lastName"),
        profileBigUrl("profileBigUrl"),
        teamName("teamName"),
        teamId("teamId"),
        position("position"),
        number("number"),
        averagePoints("averagePoints"),
        totalPoints("totalPoints"),
        marketValue("marketValue"),
        marketValueTrend("marketValueTrend"),
        tfhmvt("tfhmvt"),
        prlo("prlo"),
        stl("stl"),
        status("status"),
        userOwnsPlayer("userOwnsPlayer");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "id" -> CodingKeys.id
                    "firstName" -> CodingKeys.firstName
                    "lastName" -> CodingKeys.lastName
                    "profileBigUrl" -> CodingKeys.profileBigUrl
                    "teamName" -> CodingKeys.teamName
                    "teamId" -> CodingKeys.teamId
                    "position" -> CodingKeys.position
                    "number" -> CodingKeys.number
                    "averagePoints" -> CodingKeys.averagePoints
                    "totalPoints" -> CodingKeys.totalPoints
                    "marketValue" -> CodingKeys.marketValue
                    "marketValueTrend" -> CodingKeys.marketValueTrend
                    "tfhmvt" -> CodingKeys.tfhmvt
                    "prlo" -> CodingKeys.prlo
                    "stl" -> CodingKeys.stl
                    "status" -> CodingKeys.status
                    "userOwnsPlayer" -> CodingKeys.userOwnsPlayer
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(id, forKey = CodingKeys.id)
        container.encode(firstName, forKey = CodingKeys.firstName)
        container.encode(lastName, forKey = CodingKeys.lastName)
        container.encode(profileBigUrl, forKey = CodingKeys.profileBigUrl)
        container.encode(teamName, forKey = CodingKeys.teamName)
        container.encode(teamId, forKey = CodingKeys.teamId)
        container.encode(position, forKey = CodingKeys.position)
        container.encode(number, forKey = CodingKeys.number)
        container.encode(averagePoints, forKey = CodingKeys.averagePoints)
        container.encode(totalPoints, forKey = CodingKeys.totalPoints)
        container.encode(marketValue, forKey = CodingKeys.marketValue)
        container.encode(marketValueTrend, forKey = CodingKeys.marketValueTrend)
        container.encode(tfhmvt, forKey = CodingKeys.tfhmvt)
        container.encode(prlo, forKey = CodingKeys.prlo)
        container.encode(stl, forKey = CodingKeys.stl)
        container.encode(status, forKey = CodingKeys.status)
        container.encode(userOwnsPlayer, forKey = CodingKeys.userOwnsPlayer)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.id = container.decode(String::class, forKey = CodingKeys.id)
        this.firstName = container.decode(String::class, forKey = CodingKeys.firstName)
        this.lastName = container.decode(String::class, forKey = CodingKeys.lastName)
        this.profileBigUrl = container.decode(String::class, forKey = CodingKeys.profileBigUrl)
        this.teamName = container.decode(String::class, forKey = CodingKeys.teamName)
        this.teamId = container.decode(String::class, forKey = CodingKeys.teamId)
        this.position = container.decode(Int::class, forKey = CodingKeys.position)
        this.number = container.decode(Int::class, forKey = CodingKeys.number)
        this.averagePoints = container.decode(Double::class, forKey = CodingKeys.averagePoints)
        this.totalPoints = container.decode(Int::class, forKey = CodingKeys.totalPoints)
        this.marketValue = container.decode(Int::class, forKey = CodingKeys.marketValue)
        this.marketValueTrend = container.decode(Int::class, forKey = CodingKeys.marketValueTrend)
        this.tfhmvt = container.decode(Int::class, forKey = CodingKeys.tfhmvt)
        this.prlo = container.decode(Int::class, forKey = CodingKeys.prlo)
        this.stl = container.decode(Int::class, forKey = CodingKeys.stl)
        this.status = container.decode(Int::class, forKey = CodingKeys.status)
        this.userOwnsPlayer = container.decode(Boolean::class, forKey = CodingKeys.userOwnsPlayer)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<Player> {
        override fun init(from: Decoder): Player = Player(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

// Team Player is the same as Player but used specifically for team context
typealias TeamPlayer = Player

// MARK: - Market Models
@androidx.annotation.Keep
class MarketPlayer: Codable, Identifiable<String> {
    override val id: String
    val firstName: String
    val lastName: String
    val profileBigUrl: String
    val teamName: String
    val teamId: String
    val position: Int
    val number: Int
    val averagePoints: Double
    val totalPoints: Int
    val marketValue: Int
    val marketValueTrend: Int
    val price: Int
    val expiry: String
    val offers: Int
    val seller: MarketSeller
    val stl: Int // Verletzungsstatus aus API-Daten
    val status: Int // Status-Feld für Verletzung/Angeschlagen
    val prlo: Int? // Profit/Loss since purchase - Gewinn/Verlust seit Kauf
    val owner: PlayerOwner? // Optional owner field
    val exs: Int // Ablaufdatum als Timestamp für Sortierung

    val fullName: String
        get() = "${firstName} ${lastName}"

    val imageUrl: URL?
        get() {
            if (profileBigUrl.hasPrefix("http")) {
                return (try { URL(string = profileBigUrl) } catch (_: NullReturnException) { null })
            } else if (!profileBigUrl.isEmpty) {
                // Remove leading slash if present to avoid double slashes
                val path = if (profileBigUrl.hasPrefix("/")) String(profileBigUrl.dropFirst()) else profileBigUrl
                return (try { URL(string = "https://kickbase.b-cdn.net/" + path) } catch (_: NullReturnException) { null })
            }
            return null
        }

    // Neue computed property für den vollständigen Teamnamen basierend auf teamId
    val fullTeamName: String
        get() = TeamMapping.getTeamName(for_ = teamId) ?: teamName

    val positionName: String
        get() {
            when (position) {
                1 -> return "TW"
                2 -> return "ABW"
                3 -> return "MF"
                4 -> return "ST"
                else -> return "?"
            }
        }

    val positionColor: Color
        get() {
            when (position) {
                1 -> return Color.blue // Torwart
                2 -> return Color.green // Abwehr
                3 -> return Color.orange // Mittelfeld
                4 -> return Color.red // Sturm
                else -> return Color.gray
            }
        }

    // Equatable conformance
    override fun equals(other: Any?): Boolean {
        if (other !is MarketPlayer) {
            return false
        }
        val lhs = this
        val rhs = other
        return lhs.id == rhs.id
    }

    constructor(id: String, firstName: String, lastName: String, profileBigUrl: String, teamName: String, teamId: String, position: Int, number: Int, averagePoints: Double, totalPoints: Int, marketValue: Int, marketValueTrend: Int, price: Int, expiry: String, offers: Int, seller: MarketSeller, stl: Int, status: Int, prlo: Int?, owner: PlayerOwner?, exs: Int) {
        this.id = id
        this.firstName = firstName
        this.lastName = lastName
        this.profileBigUrl = profileBigUrl
        this.teamName = teamName
        this.teamId = teamId
        this.position = position
        this.number = number
        this.averagePoints = averagePoints
        this.totalPoints = totalPoints
        this.marketValue = marketValue
        this.marketValueTrend = marketValueTrend
        this.price = price
        this.expiry = expiry
        this.offers = offers
        this.seller = seller
        this.stl = stl
        this.status = status
        this.prlo = prlo
        this.owner = owner
        this.exs = exs
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        id("id"),
        firstName("firstName"),
        lastName("lastName"),
        profileBigUrl("profileBigUrl"),
        teamName("teamName"),
        teamId("teamId"),
        position("position"),
        number("number"),
        averagePoints("averagePoints"),
        totalPoints("totalPoints"),
        marketValue("marketValue"),
        marketValueTrend("marketValueTrend"),
        price("price"),
        expiry("expiry"),
        offers("offers"),
        seller("seller"),
        stl("stl"),
        status("status"),
        prlo("prlo"),
        owner("owner"),
        exs("exs");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "id" -> CodingKeys.id
                    "firstName" -> CodingKeys.firstName
                    "lastName" -> CodingKeys.lastName
                    "profileBigUrl" -> CodingKeys.profileBigUrl
                    "teamName" -> CodingKeys.teamName
                    "teamId" -> CodingKeys.teamId
                    "position" -> CodingKeys.position
                    "number" -> CodingKeys.number
                    "averagePoints" -> CodingKeys.averagePoints
                    "totalPoints" -> CodingKeys.totalPoints
                    "marketValue" -> CodingKeys.marketValue
                    "marketValueTrend" -> CodingKeys.marketValueTrend
                    "price" -> CodingKeys.price
                    "expiry" -> CodingKeys.expiry
                    "offers" -> CodingKeys.offers
                    "seller" -> CodingKeys.seller
                    "stl" -> CodingKeys.stl
                    "status" -> CodingKeys.status
                    "prlo" -> CodingKeys.prlo
                    "owner" -> CodingKeys.owner
                    "exs" -> CodingKeys.exs
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(id, forKey = CodingKeys.id)
        container.encode(firstName, forKey = CodingKeys.firstName)
        container.encode(lastName, forKey = CodingKeys.lastName)
        container.encode(profileBigUrl, forKey = CodingKeys.profileBigUrl)
        container.encode(teamName, forKey = CodingKeys.teamName)
        container.encode(teamId, forKey = CodingKeys.teamId)
        container.encode(position, forKey = CodingKeys.position)
        container.encode(number, forKey = CodingKeys.number)
        container.encode(averagePoints, forKey = CodingKeys.averagePoints)
        container.encode(totalPoints, forKey = CodingKeys.totalPoints)
        container.encode(marketValue, forKey = CodingKeys.marketValue)
        container.encode(marketValueTrend, forKey = CodingKeys.marketValueTrend)
        container.encode(price, forKey = CodingKeys.price)
        container.encode(expiry, forKey = CodingKeys.expiry)
        container.encode(offers, forKey = CodingKeys.offers)
        container.encode(seller, forKey = CodingKeys.seller)
        container.encode(stl, forKey = CodingKeys.stl)
        container.encode(status, forKey = CodingKeys.status)
        container.encodeIfPresent(prlo, forKey = CodingKeys.prlo)
        container.encodeIfPresent(owner, forKey = CodingKeys.owner)
        container.encode(exs, forKey = CodingKeys.exs)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.id = container.decode(String::class, forKey = CodingKeys.id)
        this.firstName = container.decode(String::class, forKey = CodingKeys.firstName)
        this.lastName = container.decode(String::class, forKey = CodingKeys.lastName)
        this.profileBigUrl = container.decode(String::class, forKey = CodingKeys.profileBigUrl)
        this.teamName = container.decode(String::class, forKey = CodingKeys.teamName)
        this.teamId = container.decode(String::class, forKey = CodingKeys.teamId)
        this.position = container.decode(Int::class, forKey = CodingKeys.position)
        this.number = container.decode(Int::class, forKey = CodingKeys.number)
        this.averagePoints = container.decode(Double::class, forKey = CodingKeys.averagePoints)
        this.totalPoints = container.decode(Int::class, forKey = CodingKeys.totalPoints)
        this.marketValue = container.decode(Int::class, forKey = CodingKeys.marketValue)
        this.marketValueTrend = container.decode(Int::class, forKey = CodingKeys.marketValueTrend)
        this.price = container.decode(Int::class, forKey = CodingKeys.price)
        this.expiry = container.decode(String::class, forKey = CodingKeys.expiry)
        this.offers = container.decode(Int::class, forKey = CodingKeys.offers)
        this.seller = container.decode(MarketSeller::class, forKey = CodingKeys.seller)
        this.stl = container.decode(Int::class, forKey = CodingKeys.stl)
        this.status = container.decode(Int::class, forKey = CodingKeys.status)
        this.prlo = container.decodeIfPresent(Int::class, forKey = CodingKeys.prlo)
        this.owner = container.decodeIfPresent(PlayerOwner::class, forKey = CodingKeys.owner)
        this.exs = container.decode(Int::class, forKey = CodingKeys.exs)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<MarketPlayer> {
        override fun init(from: Decoder): MarketPlayer = MarketPlayer(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class MarketSeller: Codable {
    val id: String
    val name: String

    constructor(id: String, name: String) {
        this.id = id
        this.name = name
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        id("id"),
        name_("name");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "id" -> CodingKeys.id
                    "name" -> CodingKeys.name_
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(id, forKey = CodingKeys.id)
        container.encode(name, forKey = CodingKeys.name_)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.id = container.decode(String::class, forKey = CodingKeys.id)
        this.name = container.decode(String::class, forKey = CodingKeys.name_)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<MarketSeller> {
        override fun init(from: Decoder): MarketSeller = MarketSeller(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

// MARK: - Player Owner (für das "u" Feld in Marktspielern)
@androidx.annotation.Keep
class PlayerOwner: Codable {
    val i: String // ID des Besitzers
    val n: String // Name des Besitzers
    val uim: String? // Benutzer-Image URL
    val isvf: Boolean? // Is verified flag
    val st: Int? // Status

    constructor(i: String, n: String, uim: String?, isvf: Boolean?, st: Int?) {
        this.i = i
        this.n = n
        this.uim = uim
        this.isvf = isvf
        this.st = st
    }

    val id: String
        get() = i
    val name: String
        get() = n
    val userImageUrl: String?
        get() = uim
    val isVerified: Boolean
        get() = isvf ?: false
    val status: Int
        get() = st ?: 0

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        i("i"),
        n("n"),
        uim("uim"),
        isvf("isvf"),
        st("st");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "i" -> CodingKeys.i
                    "n" -> CodingKeys.n
                    "uim" -> CodingKeys.uim
                    "isvf" -> CodingKeys.isvf
                    "st" -> CodingKeys.st
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(i, forKey = CodingKeys.i)
        container.encode(n, forKey = CodingKeys.n)
        container.encodeIfPresent(uim, forKey = CodingKeys.uim)
        container.encodeIfPresent(isvf, forKey = CodingKeys.isvf)
        container.encodeIfPresent(st, forKey = CodingKeys.st)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.i = container.decode(String::class, forKey = CodingKeys.i)
        this.n = container.decode(String::class, forKey = CodingKeys.n)
        this.uim = container.decodeIfPresent(String::class, forKey = CodingKeys.uim)
        this.isvf = container.decodeIfPresent(Boolean::class, forKey = CodingKeys.isvf)
        this.st = container.decodeIfPresent(Int::class, forKey = CodingKeys.st)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<PlayerOwner> {
        override fun init(from: Decoder): PlayerOwner = PlayerOwner(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

// MARK: - Response Wrappers
@androidx.annotation.Keep
class PlayersResponse: Codable {
    val players: Array<Player>

    constructor(players: Array<Player>) {
        this.players = players.sref()
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        players("players");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "players" -> CodingKeys.players
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(players, forKey = CodingKeys.players)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.players = container.decode(Array::class, elementType = Player::class, forKey = CodingKeys.players)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<PlayersResponse> {
        override fun init(from: Decoder): PlayersResponse = PlayersResponse(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class MarketResponse: Codable {
    val players: Array<MarketPlayer>

    constructor(players: Array<MarketPlayer>) {
        this.players = players.sref()
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        players("players");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "players" -> CodingKeys.players
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(players, forKey = CodingKeys.players)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.players = container.decode(Array::class, elementType = MarketPlayer::class, forKey = CodingKeys.players)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<MarketResponse> {
        override fun init(from: Decoder): MarketResponse = MarketResponse(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class LeaguesResponse: Codable {
    val leagues: Array<League>

    constructor(leagues: Array<League>) {
        this.leagues = leagues.sref()
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        leagues("leagues");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "leagues" -> CodingKeys.leagues
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(leagues, forKey = CodingKeys.leagues)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.leagues = container.decode(Array::class, elementType = League::class, forKey = CodingKeys.leagues)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<LeaguesResponse> {
        override fun init(from: Decoder): LeaguesResponse = LeaguesResponse(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

// MARK: - Performance Models
@androidx.annotation.Keep
class PlayerPerformanceResponse: Codable {
    val it: Array<SeasonPerformance>

    constructor(it: Array<SeasonPerformance>) {
        this.it = it.sref()
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        it("it");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "it" -> CodingKeys.it
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(it, forKey = CodingKeys.it)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.it = container.decode(Array::class, elementType = SeasonPerformance::class, forKey = CodingKeys.it)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<PlayerPerformanceResponse> {
        override fun init(from: Decoder): PlayerPerformanceResponse = PlayerPerformanceResponse(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class SeasonPerformance: Codable, Identifiable<String> {
    val ti: String // Saison (z.B. "2024/2025")
    val n: String // Liga Name (z.B. "Bundesliga")
    val ph: Array<MatchPerformance> // Performance History

    override val id: String
        get() = ti
    val title: String
        get() = ti
    val leagueName: String
        get() = n
    val performances: Array<MatchPerformance>
        get() = ph

    constructor(ti: String, n: String, ph: Array<MatchPerformance>) {
        this.ti = ti
        this.n = n
        this.ph = ph.sref()
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        ti("ti"),
        n("n"),
        ph("ph");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "ti" -> CodingKeys.ti
                    "n" -> CodingKeys.n
                    "ph" -> CodingKeys.ph
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(ti, forKey = CodingKeys.ti)
        container.encode(n, forKey = CodingKeys.n)
        container.encode(ph, forKey = CodingKeys.ph)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.ti = container.decode(String::class, forKey = CodingKeys.ti)
        this.n = container.decode(String::class, forKey = CodingKeys.n)
        this.ph = container.decode(Array::class, elementType = MatchPerformance::class, forKey = CodingKeys.ph)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<SeasonPerformance> {
        override fun init(from: Decoder): SeasonPerformance = SeasonPerformance(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class MatchPerformance: Codable, Identifiable<String> {
    val day: Int // Spieltag
    val p: Int? // Punkte (optional, nicht bei zukünftigen Spielen)
    val mp: String? // Spielminuten (z.B. "96'", optional)
    val md: String // Match Date (ISO String)
    val t1: String // Team 1 ID
    val t2: String // Team 2 ID
    val t1g: Int? // Team 1 Goals (optional)
    val t2g: Int? // Team 2 Goals (optional)
    val pt: String? // Player Team ID
    val k: Array<Int>? // Kicker Bewertungen (optional)
    val st: Int // Status (0=nicht gespielt, 1=?, 3=eingewechselt, 4=nicht im Kader, 5=startelf)
    val cur: Boolean // Current (aktueller Spieltag?)
    val mdst: Int // Match Day Status
    val ap: Int // Average Points (Durchschnittspunkte)
    val tp: Int // Total Points (Gesamtpunkte)
    val asp: Int // Average Season Points
    //let t1im: String          // Team 1 Image
    //let t2im: String          // Team 2 Image

    override val id: String
        get() = "${day}-${md}"
    val matchDay: Int
        get() = day
    val points: Int
        get() = p ?: 0
    val minutesPlayed: String
        get() = mp ?: "0'"
    val matchDate: String
        get() = md
    val team1Id: String
        get() = t1
    val team2Id: String
        get() = t2
    val team1Goals: Int
        get() = t1g ?: 0
    val team2Goals: Int
        get() = t2g ?: 0
    val playerTeamId: String
        get() = pt ?: ""
    val kickerRatings: Array<Int>
        get() = k ?: arrayOf()
    val status: Int
        get() = st
    val isCurrent: Boolean
        get() = cur
    val matchDayStatus: Int
        get() = mdst
    val averagePoints: Int
        get() = ap
    val totalPoints: Int
        get() = tp
    val averageSeasonPoints: Int
        get() = asp
    //var team1Image: String { t1im }
    //var team2Image: String { t2im }

    // Computed properties
    val hasPlayed: Boolean
        get() = p != null && st > 1
    val wasStartingEleven: Boolean
        get() = st == 5
    val wasSubstitute: Boolean
        get() = st == 3
    val wasNotInSquad: Boolean
        get() = st == 4
    val didNotPlay: Boolean
        get() = st <= 1

    val statusText: String
        get() {
            when (st) {
                0 -> return "Nicht gespielt"
                1 -> return "Verletzt/Gesperrt"
                3 -> return "Eingewechselt"
                4 -> return "Nicht im Kader"
                5 -> return "Startelf"
                else -> return "Unbekannt"
            }
        }

    val parsedMatchDate: Date
        get() {
            val formatter = ISO8601DateFormatter()
            return formatter.date(from = md) ?: Date()
        }

    val opponentTeamId: String
        get() {
            if (playerTeamId.isEmpty) {
                // Wenn keine Player Team ID verfügbar ist, nimm einfach team2 als Gegner
                return t2
            }
            return if (playerTeamId == t1) t2 else t1
        }

    val opponentTeamName: String
        get() = TeamMapping.getTeamName(for_ = opponentTeamId) ?: "Unbekannt"

    val isHomeMatch: Boolean
        get() {
            if (playerTeamId.isEmpty) {
                return false // Standardmäßig Auswärtsspiel wenn unbekannt
            }
            return playerTeamId == t1
        }

    val result: String
        get() {
            if ((t1g == null) || (t2g == null)) {
                return "-:-"
            }
            return "${t1g}:${t2g}"
        }

    // Neue Methoden, die den Spieler-Kontext verwenden
    fun getOpponentTeamId(playerTeamId: String): String = if (playerTeamId == t1) t2 else t1

    fun getIsHomeMatch(playerTeamId: String): Boolean = playerTeamId == t1

    constructor(day: Int, p: Int? = null, mp: String? = null, md: String, t1: String, t2: String, t1g: Int? = null, t2g: Int? = null, pt: String? = null, k: Array<Int>? = null, st: Int, cur: Boolean, mdst: Int, ap: Int, tp: Int, asp: Int) {
        this.day = day
        this.p = p
        this.mp = mp
        this.md = md
        this.t1 = t1
        this.t2 = t2
        this.t1g = t1g
        this.t2g = t2g
        this.pt = pt
        this.k = k.sref()
        this.st = st
        this.cur = cur
        this.mdst = mdst
        this.ap = ap
        this.tp = tp
        this.asp = asp
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        day("day"),
        p("p"),
        mp("mp"),
        md("md"),
        t1("t1"),
        t2("t2"),
        t1g("t1g"),
        t2g("t2g"),
        pt("pt"),
        k("k"),
        st("st"),
        cur("cur"),
        mdst("mdst"),
        ap("ap"),
        tp("tp"),
        asp("asp");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "day" -> CodingKeys.day
                    "p" -> CodingKeys.p
                    "mp" -> CodingKeys.mp
                    "md" -> CodingKeys.md
                    "t1" -> CodingKeys.t1
                    "t2" -> CodingKeys.t2
                    "t1g" -> CodingKeys.t1g
                    "t2g" -> CodingKeys.t2g
                    "pt" -> CodingKeys.pt
                    "k" -> CodingKeys.k
                    "st" -> CodingKeys.st
                    "cur" -> CodingKeys.cur
                    "mdst" -> CodingKeys.mdst
                    "ap" -> CodingKeys.ap
                    "tp" -> CodingKeys.tp
                    "asp" -> CodingKeys.asp
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(day, forKey = CodingKeys.day)
        container.encodeIfPresent(p, forKey = CodingKeys.p)
        container.encodeIfPresent(mp, forKey = CodingKeys.mp)
        container.encode(md, forKey = CodingKeys.md)
        container.encode(t1, forKey = CodingKeys.t1)
        container.encode(t2, forKey = CodingKeys.t2)
        container.encodeIfPresent(t1g, forKey = CodingKeys.t1g)
        container.encodeIfPresent(t2g, forKey = CodingKeys.t2g)
        container.encodeIfPresent(pt, forKey = CodingKeys.pt)
        container.encodeIfPresent(k, forKey = CodingKeys.k)
        container.encode(st, forKey = CodingKeys.st)
        container.encode(cur, forKey = CodingKeys.cur)
        container.encode(mdst, forKey = CodingKeys.mdst)
        container.encode(ap, forKey = CodingKeys.ap)
        container.encode(tp, forKey = CodingKeys.tp)
        container.encode(asp, forKey = CodingKeys.asp)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.day = container.decode(Int::class, forKey = CodingKeys.day)
        this.p = container.decodeIfPresent(Int::class, forKey = CodingKeys.p)
        this.mp = container.decodeIfPresent(String::class, forKey = CodingKeys.mp)
        this.md = container.decode(String::class, forKey = CodingKeys.md)
        this.t1 = container.decode(String::class, forKey = CodingKeys.t1)
        this.t2 = container.decode(String::class, forKey = CodingKeys.t2)
        this.t1g = container.decodeIfPresent(Int::class, forKey = CodingKeys.t1g)
        this.t2g = container.decodeIfPresent(Int::class, forKey = CodingKeys.t2g)
        this.pt = container.decodeIfPresent(String::class, forKey = CodingKeys.pt)
        this.k = container.decodeIfPresent(Array::class, elementType = Int::class, forKey = CodingKeys.k)
        this.st = container.decode(Int::class, forKey = CodingKeys.st)
        this.cur = container.decode(Boolean::class, forKey = CodingKeys.cur)
        this.mdst = container.decode(Int::class, forKey = CodingKeys.mdst)
        this.ap = container.decode(Int::class, forKey = CodingKeys.ap)
        this.tp = container.decode(Int::class, forKey = CodingKeys.tp)
        this.asp = container.decode(Int::class, forKey = CodingKeys.asp)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<MatchPerformance> {
        override fun init(from: Decoder): MatchPerformance = MatchPerformance(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

// MARK: - Team Profile Models
@androidx.annotation.Keep
class TeamProfileResponse: Codable {
    val tid: String // Team ID
    val tn: String // Team Name
    val pl: Int // Placement (Platzierung)
    val tv: Int // Team Value
    val tw: Int // Team Wins
    val td: Int // Team Draws
    val tl: Int // Team Losses
    //let it: [TeamPlayer]? // Team Players (optional)
    val npt: Int // Next Point Total
    val avpcl: Boolean // Available Players Close

    constructor(tid: String, tn: String, pl: Int, tv: Int, tw: Int, td: Int, tl: Int, npt: Int, avpcl: Boolean) {
        this.tid = tid
        this.tn = tn
        this.pl = pl
        this.tv = tv
        this.tw = tw
        this.td = td
        this.tl = tl
        this.npt = npt
        this.avpcl = avpcl
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        tid("tid"),
        tn("tn"),
        pl("pl"),
        tv("tv"),
        tw("tw"),
        td("td"),
        tl("tl"),
        npt("npt"),
        avpcl("avpcl");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "tid" -> CodingKeys.tid
                    "tn" -> CodingKeys.tn
                    "pl" -> CodingKeys.pl
                    "tv" -> CodingKeys.tv
                    "tw" -> CodingKeys.tw
                    "td" -> CodingKeys.td
                    "tl" -> CodingKeys.tl
                    "npt" -> CodingKeys.npt
                    "avpcl" -> CodingKeys.avpcl
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(tid, forKey = CodingKeys.tid)
        container.encode(tn, forKey = CodingKeys.tn)
        container.encode(pl, forKey = CodingKeys.pl)
        container.encode(tv, forKey = CodingKeys.tv)
        container.encode(tw, forKey = CodingKeys.tw)
        container.encode(td, forKey = CodingKeys.td)
        container.encode(tl, forKey = CodingKeys.tl)
        container.encode(npt, forKey = CodingKeys.npt)
        container.encode(avpcl, forKey = CodingKeys.avpcl)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.tid = container.decode(String::class, forKey = CodingKeys.tid)
        this.tn = container.decode(String::class, forKey = CodingKeys.tn)
        this.pl = container.decode(Int::class, forKey = CodingKeys.pl)
        this.tv = container.decode(Int::class, forKey = CodingKeys.tv)
        this.tw = container.decode(Int::class, forKey = CodingKeys.tw)
        this.td = container.decode(Int::class, forKey = CodingKeys.td)
        this.tl = container.decode(Int::class, forKey = CodingKeys.tl)
        this.npt = container.decode(Int::class, forKey = CodingKeys.npt)
        this.avpcl = container.decode(Boolean::class, forKey = CodingKeys.avpcl)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<TeamProfileResponse> {
        override fun init(from: Decoder): TeamProfileResponse = TeamProfileResponse(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class TeamInfo: Codable, Identifiable<String> {
    override val id: String // Team ID
    val name: String // Team Name
    val placement: Int // Platzierung

    constructor(from: TeamProfileResponse) {
        val response = from
        this.id = response.tid
        this.name = response.tn
        this.placement = response.pl
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        id("id"),
        name_("name"),
        placement("placement");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "id" -> CodingKeys.id
                    "name" -> CodingKeys.name_
                    "placement" -> CodingKeys.placement
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(id, forKey = CodingKeys.id)
        container.encode(name, forKey = CodingKeys.name_)
        container.encode(placement, forKey = CodingKeys.placement)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.id = container.decode(String::class, forKey = CodingKeys.id)
        this.name = container.decode(String::class, forKey = CodingKeys.name_)
        this.placement = container.decode(Int::class, forKey = CodingKeys.placement)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<TeamInfo> {
        override fun init(from: Decoder): TeamInfo = TeamInfo(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

// MARK: - Enhanced Performance Models with Team Info
class EnhancedMatchPerformance: Identifiable<String> {
    val basePerformance: MatchPerformance
    val team1Info: TeamInfo?
    val team2Info: TeamInfo?
    val playerTeamInfo: TeamInfo?
    val opponentTeamInfo: TeamInfo?

    // Delegiere alle Eigenschaften an basePerformance
    override val id: String
        get() = basePerformance.id
    val matchDay: Int
        get() = basePerformance.matchDay
    val points: Int
        get() = basePerformance.points
    val minutesPlayed: String
        get() = basePerformance.minutesPlayed
    val matchDate: String
        get() = basePerformance.matchDate
    val team1Id: String
        get() = basePerformance.team1Id
    val team2Id: String
        get() = basePerformance.team2Id
    val team1Goals: Int
        get() = basePerformance.team1Goals
    val team2Goals: Int
        get() = basePerformance.team2Goals
    val playerTeamId: String
        get() = basePerformance.playerTeamId
    val kickerRatings: Array<Int>
        get() = basePerformance.kickerRatings
    val status: Int
        get() = basePerformance.status
    val isCurrent: Boolean
        get() = basePerformance.isCurrent
    val matchDayStatus: Int
        get() = basePerformance.matchDayStatus
    val averagePoints: Int
        get() = basePerformance.averagePoints
    val totalPoints: Int
        get() = basePerformance.totalPoints
    val averageSeasonPoints: Int
        get() = basePerformance.averageSeasonPoints
    //var team1Image: String { basePerformance.team1Image }
    //var team2Image: String { basePerformance.team2Image }
    val hasPlayed: Boolean
        get() = basePerformance.hasPlayed
    val wasStartingEleven: Boolean
        get() = basePerformance.wasStartingEleven
    val wasSubstitute: Boolean
        get() = basePerformance.wasSubstitute
    val wasNotInSquad: Boolean
        get() = basePerformance.wasNotInSquad
    val didNotPlay: Boolean
        get() = basePerformance.didNotPlay
    val statusText: String
        get() = basePerformance.statusText
    val parsedMatchDate: Date
        get() = basePerformance.parsedMatchDate
    val opponentTeamId: String
        get() = basePerformance.opponentTeamId
    val isHomeMatch: Boolean
        get() = basePerformance.isHomeMatch
    val result: String
        get() = basePerformance.result

    // Erweiterte computed properties mit Team-Informationen
    val team1Name: String
        get() {
            return team1Info?.name ?: TeamMapping.getTeamName(for_ = team1Id) ?: "Unbekannt"
        }

    val team2Name: String
        get() {
            return team2Info?.name ?: TeamMapping.getTeamName(for_ = team2Id) ?: "Unbekannt"
        }

    val playerTeamName: String
        get() {
            return playerTeamInfo?.name ?: TeamMapping.getTeamName(for_ = playerTeamId) ?: "Unbekannt"
        }

    val opponentTeamName: String
        get() {
            return opponentTeamInfo?.name ?: TeamMapping.getTeamName(for_ = opponentTeamId) ?: "Unbekannt"
        }

    val team1Placement: Int?
        get() {
            return team1Info?.placement
        }

    val team2Placement: Int?
        get() {
            return team2Info?.placement
        }

    val playerTeamPlacement: Int?
        get() {
            return playerTeamInfo?.placement
        }

    val opponentTeamPlacement: Int?
        get() {
            return opponentTeamInfo?.placement
        }

    val matchDescription: String
        get() {
            val homeTeam = team1Name
            val awayTeam = team2Name
            val homeGoals = team1Goals
            val awayGoals = team2Goals

            if (hasPlayed) {
                return "${homeTeam} ${homeGoals}:${awayGoals} ${awayTeam}"
            } else {
                return "${homeTeam} vs ${awayTeam}"
            }
        }

    constructor(basePerformance: MatchPerformance, team1Info: TeamInfo? = null, team2Info: TeamInfo? = null, playerTeamInfo: TeamInfo? = null, opponentTeamInfo: TeamInfo? = null) {
        this.basePerformance = basePerformance
        this.team1Info = team1Info
        this.team2Info = team2Info
        this.playerTeamInfo = playerTeamInfo
        this.opponentTeamInfo = opponentTeamInfo
    }

    @androidx.annotation.Keep
    companion object {
    }
}

// MARK: - Stats Models
@androidx.annotation.Keep
class TeamStats: Codable {
    val teamValue: Int
    val teamValueTrend: Int
    val budget: Int
    val points: Int
    val placement: Int
    val won: Int
    val drawn: Int
    val lost: Int

    constructor(teamValue: Int, teamValueTrend: Int, budget: Int, points: Int, placement: Int, won: Int, drawn: Int, lost: Int) {
        this.teamValue = teamValue
        this.teamValueTrend = teamValueTrend
        this.budget = budget
        this.points = points
        this.placement = placement
        this.won = won
        this.drawn = drawn
        this.lost = lost
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        teamValue("teamValue"),
        teamValueTrend("teamValueTrend"),
        budget("budget"),
        points("points"),
        placement("placement"),
        won("won"),
        drawn("drawn"),
        lost("lost");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "teamValue" -> CodingKeys.teamValue
                    "teamValueTrend" -> CodingKeys.teamValueTrend
                    "budget" -> CodingKeys.budget
                    "points" -> CodingKeys.points
                    "placement" -> CodingKeys.placement
                    "won" -> CodingKeys.won
                    "drawn" -> CodingKeys.drawn
                    "lost" -> CodingKeys.lost
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(teamValue, forKey = CodingKeys.teamValue)
        container.encode(teamValueTrend, forKey = CodingKeys.teamValueTrend)
        container.encode(budget, forKey = CodingKeys.budget)
        container.encode(points, forKey = CodingKeys.points)
        container.encode(placement, forKey = CodingKeys.placement)
        container.encode(won, forKey = CodingKeys.won)
        container.encode(drawn, forKey = CodingKeys.drawn)
        container.encode(lost, forKey = CodingKeys.lost)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.teamValue = container.decode(Int::class, forKey = CodingKeys.teamValue)
        this.teamValueTrend = container.decode(Int::class, forKey = CodingKeys.teamValueTrend)
        this.budget = container.decode(Int::class, forKey = CodingKeys.budget)
        this.points = container.decode(Int::class, forKey = CodingKeys.points)
        this.placement = container.decode(Int::class, forKey = CodingKeys.placement)
        this.won = container.decode(Int::class, forKey = CodingKeys.won)
        this.drawn = container.decode(Int::class, forKey = CodingKeys.drawn)
        this.lost = container.decode(Int::class, forKey = CodingKeys.lost)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<TeamStats> {
        override fun init(from: Decoder): TeamStats = TeamStats(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class UserStats: Codable {
    val teamValue: Int
    val teamValueTrend: Int
    val budget: Int
    val points: Int
    val placement: Int
    val won: Int
    val drawn: Int
    val lost: Int

    constructor(teamValue: Int, teamValueTrend: Int, budget: Int, points: Int, placement: Int, won: Int, drawn: Int, lost: Int) {
        this.teamValue = teamValue
        this.teamValueTrend = teamValueTrend
        this.budget = budget
        this.points = points
        this.placement = placement
        this.won = won
        this.drawn = drawn
        this.lost = lost
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        teamValue("teamValue"),
        teamValueTrend("teamValueTrend"),
        budget("budget"),
        points("points"),
        placement("placement"),
        won("won"),
        drawn("drawn"),
        lost("lost");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "teamValue" -> CodingKeys.teamValue
                    "teamValueTrend" -> CodingKeys.teamValueTrend
                    "budget" -> CodingKeys.budget
                    "points" -> CodingKeys.points
                    "placement" -> CodingKeys.placement
                    "won" -> CodingKeys.won
                    "drawn" -> CodingKeys.drawn
                    "lost" -> CodingKeys.lost
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(teamValue, forKey = CodingKeys.teamValue)
        container.encode(teamValueTrend, forKey = CodingKeys.teamValueTrend)
        container.encode(budget, forKey = CodingKeys.budget)
        container.encode(points, forKey = CodingKeys.points)
        container.encode(placement, forKey = CodingKeys.placement)
        container.encode(won, forKey = CodingKeys.won)
        container.encode(drawn, forKey = CodingKeys.drawn)
        container.encode(lost, forKey = CodingKeys.lost)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.teamValue = container.decode(Int::class, forKey = CodingKeys.teamValue)
        this.teamValueTrend = container.decode(Int::class, forKey = CodingKeys.teamValueTrend)
        this.budget = container.decode(Int::class, forKey = CodingKeys.budget)
        this.points = container.decode(Int::class, forKey = CodingKeys.points)
        this.placement = container.decode(Int::class, forKey = CodingKeys.placement)
        this.won = container.decode(Int::class, forKey = CodingKeys.won)
        this.drawn = container.decode(Int::class, forKey = CodingKeys.drawn)
        this.lost = container.decode(Int::class, forKey = CodingKeys.lost)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<UserStats> {
        override fun init(from: Decoder): UserStats = UserStats(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

// MARK: - Team Mapping
class TeamMapping {

    @androidx.annotation.Keep
    companion object {
        // Wird durch Auto-Discovery gefüllt - siehe discoverAndMapTeams() in KickbaseManager
        internal var teamIdToName: Dictionary<String, String> = dictionaryOf()
            get() = field.sref({ this.teamIdToName = it })
            set(newValue) {
                field = newValue.sref()
            }

        internal var teamNameToId: Dictionary<String, String> = linvoke l@{ ->
            var reversed: Dictionary<String, String> = dictionaryOf()
            for ((id, name) in teamIdToName.sref()) {
                reversed[name] = id
            }
            return@l reversed
        }
            get() = field.sref({ this.teamNameToId = it })
            set(newValue) {
                field = newValue.sref()
            }

        fun getTeamName(for_: String): String? {
            val id = for_
            return teamIdToName[id]
        }

        fun getTeamId(for_: String): String? {
            val name = for_
            return teamNameToId[name]
        }

        fun getAllTeams(): Dictionary<String, String> = teamIdToName.sref()

        // Funktion zum Aktualisieren des Mappings durch Auto-Discovery
        fun updateMapping(with: Dictionary<String, String>) {
            val discoveredTeams = with
            teamIdToName = discoveredTeams
            // Aktualisiere auch das umgekehrte Mapping
            var reversed: Dictionary<String, String> = dictionaryOf()
            for ((id, name) in teamIdToName.sref()) {
                reversed[name] = id
            }
            teamNameToId = reversed
        }
    }
}

// MARK: - Player Detail Models
@androidx.annotation.Keep
class PlayerDetailResponse: Codable {
    val fn: String? // First Name
    val ln: String? // Last Name
    val tn: String? // Team Name
    val shn: Int? // Shirt Number (Trikotnummer)
    val id: String?
    val position: Int?
    val number: Int?
    val averagePoints: Double?
    val totalPoints: Int?
    val marketValue: Int?
    val marketValueTrend: Int?
    val profileBigUrl: String?
    val teamId: String?
    val tfhmvt: Int?
    val prlo: Int? // Profit/Loss since purchase - Gewinn/Verlust seit Kauf
    val stl: Int?
    val status: Int?
    val userOwnsPlayer: Boolean?

    constructor(fn: String? = null, ln: String? = null, tn: String? = null, shn: Int? = null, id: String? = null, position: Int? = null, number: Int? = null, averagePoints: Double? = null, totalPoints: Int? = null, marketValue: Int? = null, marketValueTrend: Int? = null, profileBigUrl: String? = null, teamId: String? = null, tfhmvt: Int? = null, prlo: Int? = null, stl: Int? = null, status: Int? = null, userOwnsPlayer: Boolean? = null) {
        this.fn = fn
        this.ln = ln
        this.tn = tn
        this.shn = shn
        this.id = id
        this.position = position
        this.number = number
        this.averagePoints = averagePoints
        this.totalPoints = totalPoints
        this.marketValue = marketValue
        this.marketValueTrend = marketValueTrend
        this.profileBigUrl = profileBigUrl
        this.teamId = teamId
        this.tfhmvt = tfhmvt
        this.prlo = prlo
        this.stl = stl
        this.status = status
        this.userOwnsPlayer = userOwnsPlayer
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        fn("fn"),
        ln("ln"),
        tn("tn"),
        shn("shn"),
        id("id"),
        position("position"),
        number("number"),
        averagePoints("averagePoints"),
        totalPoints("totalPoints"),
        marketValue("marketValue"),
        marketValueTrend("marketValueTrend"),
        profileBigUrl("profileBigUrl"),
        teamId("teamId"),
        tfhmvt("tfhmvt"),
        prlo("prlo"),
        stl("stl"),
        status("status"),
        userOwnsPlayer("userOwnsPlayer");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "fn" -> CodingKeys.fn
                    "ln" -> CodingKeys.ln
                    "tn" -> CodingKeys.tn
                    "shn" -> CodingKeys.shn
                    "id" -> CodingKeys.id
                    "position" -> CodingKeys.position
                    "number" -> CodingKeys.number
                    "averagePoints" -> CodingKeys.averagePoints
                    "totalPoints" -> CodingKeys.totalPoints
                    "marketValue" -> CodingKeys.marketValue
                    "marketValueTrend" -> CodingKeys.marketValueTrend
                    "profileBigUrl" -> CodingKeys.profileBigUrl
                    "teamId" -> CodingKeys.teamId
                    "tfhmvt" -> CodingKeys.tfhmvt
                    "prlo" -> CodingKeys.prlo
                    "stl" -> CodingKeys.stl
                    "status" -> CodingKeys.status
                    "userOwnsPlayer" -> CodingKeys.userOwnsPlayer
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encodeIfPresent(fn, forKey = CodingKeys.fn)
        container.encodeIfPresent(ln, forKey = CodingKeys.ln)
        container.encodeIfPresent(tn, forKey = CodingKeys.tn)
        container.encodeIfPresent(shn, forKey = CodingKeys.shn)
        container.encodeIfPresent(id, forKey = CodingKeys.id)
        container.encodeIfPresent(position, forKey = CodingKeys.position)
        container.encodeIfPresent(number, forKey = CodingKeys.number)
        container.encodeIfPresent(averagePoints, forKey = CodingKeys.averagePoints)
        container.encodeIfPresent(totalPoints, forKey = CodingKeys.totalPoints)
        container.encodeIfPresent(marketValue, forKey = CodingKeys.marketValue)
        container.encodeIfPresent(marketValueTrend, forKey = CodingKeys.marketValueTrend)
        container.encodeIfPresent(profileBigUrl, forKey = CodingKeys.profileBigUrl)
        container.encodeIfPresent(teamId, forKey = CodingKeys.teamId)
        container.encodeIfPresent(tfhmvt, forKey = CodingKeys.tfhmvt)
        container.encodeIfPresent(prlo, forKey = CodingKeys.prlo)
        container.encodeIfPresent(stl, forKey = CodingKeys.stl)
        container.encodeIfPresent(status, forKey = CodingKeys.status)
        container.encodeIfPresent(userOwnsPlayer, forKey = CodingKeys.userOwnsPlayer)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.fn = container.decodeIfPresent(String::class, forKey = CodingKeys.fn)
        this.ln = container.decodeIfPresent(String::class, forKey = CodingKeys.ln)
        this.tn = container.decodeIfPresent(String::class, forKey = CodingKeys.tn)
        this.shn = container.decodeIfPresent(Int::class, forKey = CodingKeys.shn)
        this.id = container.decodeIfPresent(String::class, forKey = CodingKeys.id)
        this.position = container.decodeIfPresent(Int::class, forKey = CodingKeys.position)
        this.number = container.decodeIfPresent(Int::class, forKey = CodingKeys.number)
        this.averagePoints = container.decodeIfPresent(Double::class, forKey = CodingKeys.averagePoints)
        this.totalPoints = container.decodeIfPresent(Int::class, forKey = CodingKeys.totalPoints)
        this.marketValue = container.decodeIfPresent(Int::class, forKey = CodingKeys.marketValue)
        this.marketValueTrend = container.decodeIfPresent(Int::class, forKey = CodingKeys.marketValueTrend)
        this.profileBigUrl = container.decodeIfPresent(String::class, forKey = CodingKeys.profileBigUrl)
        this.teamId = container.decodeIfPresent(String::class, forKey = CodingKeys.teamId)
        this.tfhmvt = container.decodeIfPresent(Int::class, forKey = CodingKeys.tfhmvt)
        this.prlo = container.decodeIfPresent(Int::class, forKey = CodingKeys.prlo)
        this.stl = container.decodeIfPresent(Int::class, forKey = CodingKeys.stl)
        this.status = container.decodeIfPresent(Int::class, forKey = CodingKeys.status)
        this.userOwnsPlayer = container.decodeIfPresent(Boolean::class, forKey = CodingKeys.userOwnsPlayer)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<PlayerDetailResponse> {
        override fun init(from: Decoder): PlayerDetailResponse = PlayerDetailResponse(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

// MARK: - Market Value History Models
@androidx.annotation.Keep
class MarketValueHistoryResponse: Codable {
    val it: Array<MarketValueEntry> // Liste der Marktwert-Einträge
    val prlo: Int? // Profit/Loss since purchase - auf Root-Ebene, nicht in den Einträgen

    constructor(it: Array<MarketValueEntry>, prlo: Int? = null) {
        this.it = it.sref()
        this.prlo = prlo
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        it("it"),
        prlo("prlo");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "it" -> CodingKeys.it
                    "prlo" -> CodingKeys.prlo
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(it, forKey = CodingKeys.it)
        container.encodeIfPresent(prlo, forKey = CodingKeys.prlo)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.it = container.decode(Array::class, elementType = MarketValueEntry::class, forKey = CodingKeys.it)
        this.prlo = container.decodeIfPresent(Int::class, forKey = CodingKeys.prlo)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<MarketValueHistoryResponse> {
        override fun init(from: Decoder): MarketValueHistoryResponse = MarketValueHistoryResponse(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

@androidx.annotation.Keep
class MarketValueEntry: Codable {
    val dt: Int // Datum als Unix-Timestamp (Tage seit 1.1.1970)
    val mv: Int // Marktwert am entsprechenden Tag
    // prlo ist NICHT hier, sondern auf Root-Ebene in MarketValueHistoryResponse

    constructor(dt: Int, mv: Int) {
        this.dt = dt
        this.mv = mv
    }

    private enum class CodingKeys(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CodingKey, RawRepresentable<String> {
        dt("dt"),
        mv("mv");

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: String): CodingKeys? {
                return when (rawValue) {
                    "dt" -> CodingKeys.dt
                    "mv" -> CodingKeys.mv
                    else -> null
                }
            }
        }
    }

    override fun encode(to: Encoder) {
        val container = to.container(keyedBy = CodingKeys::class)
        container.encode(dt, forKey = CodingKeys.dt)
        container.encode(mv, forKey = CodingKeys.mv)
    }

    constructor(from: Decoder) {
        val container = from.container(keyedBy = CodingKeys::class)
        this.dt = container.decode(Int::class, forKey = CodingKeys.dt)
        this.mv = container.decode(Int::class, forKey = CodingKeys.mv)
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<MarketValueEntry> {
        override fun init(from: Decoder): MarketValueEntry = MarketValueEntry(from = from)

        private fun CodingKeys(rawValue: String): CodingKeys? = CodingKeys.init(rawValue = rawValue)
    }
}

// MARK: - Market Value Change Data
class DailyMarketValueChange {
    val date: String
    val value: Int
    val change: Int
    val percentageChange: Double
    val daysAgo: Int

    val isPositive: Boolean
        get() = change > 0

    val isNegative: Boolean
        get() = change < 0

    constructor(date: String, value: Int, change: Int, percentageChange: Double, daysAgo: Int) {
        this.date = date
        this.value = value
        this.change = change
        this.percentageChange = percentageChange
        this.daysAgo = daysAgo
    }

    @androidx.annotation.Keep
    companion object {
    }
}

class MarketValueChange {
    val daysSinceLastUpdate: Int
    val absoluteChange: Int
    val percentageChange: Double
    val previousValue: Int
    val currentValue: Int
    val dailyChanges: Array<DailyMarketValueChange>

    val isPositive: Boolean
        get() = absoluteChange > 0

    val isNegative: Boolean
        get() = absoluteChange < 0

    constructor(daysSinceLastUpdate: Int, absoluteChange: Int, percentageChange: Double, previousValue: Int, currentValue: Int, dailyChanges: Array<DailyMarketValueChange>) {
        this.daysSinceLastUpdate = daysSinceLastUpdate
        this.absoluteChange = absoluteChange
        this.percentageChange = percentageChange
        this.previousValue = previousValue
        this.currentValue = currentValue
        this.dailyChanges = dailyChanges.sref()
    }

    @androidx.annotation.Keep
    companion object {
    }
}

// MARK: - Demo Data Service

/// Service für realistische Demo-Daten für Apple Review
internal open class DemoDataService {

    @androidx.annotation.Keep
    companion object {

        // MARK: - Demo User

        fun createDemoUser(): User = User(id = "demo-user-001", name = "Demo User", teamName = "Demo Team", email = "demo@kickbasehelper.app", budget = 2_500_000, teamValue = 45_000_000, points = 287, placement = 5, flags = 0)

        // MARK: - Demo Leagues

        fun createDemoLeagues(): Array<League> {
            val currentUser = LeagueUser(id = "demo-user-001", name = "Demo User", teamName = "Demo Team", budget = 2_500_000, teamValue = 45_000_000, points = 287, placement = 5, won = 8, drawn = 2, lost = 5, se11 = 0, ttm = 0, mpst = 3)

            return arrayOf(
                League(id = "demo-league-001", name = "🏆 Bundesliga Classic", creatorName = "Demo Admin", adminName = "Demo Admin", created = "2024-08-01", season = "2024/25", matchDay = 12, currentUser = currentUser),
                League(id = "demo-league-002", name = "⚽ Friends Challenge", creatorName = "Demo Creator", adminName = "Demo Creator", created = "2024-09-15", season = "2024/25", matchDay = 10, currentUser = LeagueUser(id = "demo-user-001", name = "Demo User", teamName = "Expert Squad", budget = 1_800_000, teamValue = 52_000_000, points = 315, placement = 2, won = 9, drawn = 1, lost = 5, se11 = 0, ttm = 0, mpst = 3))
            )
        }

        // MARK: - Demo Team Players

        fun createDemoTeamPlayers(): Array<Player> = arrayOf(
            Player(id = "demo-player-001", firstName = "Manuel", lastName = "Neuer", profileBigUrl = "", teamName = "FC Bayern", teamId = "1", position = 1, number = 1, averagePoints = 7.2, totalPoints = 86, marketValue = 8_000_000, marketValueTrend = 500_000, tfhmvt = 250_000, prlo = 7_500_000, stl = 0, status = 0, userOwnsPlayer = true),
            Player(id = "demo-player-002", firstName = "Antonio", lastName = "Rüdiger", profileBigUrl = "", teamName = "Real Madrid", teamId = "2", position = 2, number = 3, averagePoints = 6.8, totalPoints = 82, marketValue = 22_000_000, marketValueTrend = 1_000_000, tfhmvt = 500_000, prlo = 20_000_000, stl = 0, status = 0, userOwnsPlayer = true),
            Player(id = "demo-player-003", firstName = "Jamal", lastName = "Musiala", profileBigUrl = "", teamName = "FC Bayern", teamId = "1", position = 3, number = 42, averagePoints = 7.5, totalPoints = 90, marketValue = 72_000_000, marketValueTrend = 2_000_000, tfhmvt = 1_000_000, prlo = 68_000_000, stl = 0, status = 0, userOwnsPlayer = true),
            Player(id = "demo-player-004", firstName = "Serge", lastName = "Gnabry", profileBigUrl = "", teamName = "FC Bayern", teamId = "1", position = 4, number = 7, averagePoints = 6.9, totalPoints = 83, marketValue = 48_000_000, marketValueTrend = -500_000, tfhmvt = -250_000, prlo = 45_000_000, stl = 0, status = 0, userOwnsPlayer = true),
            Player(id = "demo-player-005", firstName = "Mathys", lastName = "Tel", profileBigUrl = "", teamName = "FC Bayern", teamId = "1", position = 4, number = 39, averagePoints = 5.2, totalPoints = 52, marketValue = 28_000_000, marketValueTrend = 1_500_000, tfhmvt = 750_000, prlo = 26_000_000, stl = 0, status = 0, userOwnsPlayer = true)
        )

        // MARK: - Demo Market Players

        fun createDemoMarketPlayers(): Array<MarketPlayer> = arrayOf(
            MarketPlayer(id = "demo-market-001", firstName = "Florian", lastName = "Wirtz", profileBigUrl = "", teamName = "Bayer Leverkusen", teamId = "5", position = 3, number = 10, averagePoints = 8.1, totalPoints = 97, marketValue = 95_000_000, marketValueTrend = 5_000_000, price = 85_000_000, expiry = "2025-12-15T23:59:59Z", offers = 2, seller = MarketSeller(id = "seller-001", name = "Aktiver Spieler"), stl = 0, status = 1, prlo = 82_000_000, owner = null, exs = 1_735_689_599),
            MarketPlayer(id = "demo-market-002", firstName = "Florent", lastName = "Inzaghi", profileBigUrl = "", teamName = "Benfica Lissabon", teamId = "6", position = 4, number = 9, averagePoints = 7.8, totalPoints = 94, marketValue = 58_000_000, marketValueTrend = 2_000_000, price = 52_000_000, expiry = "2025-12-10T23:59:59Z", offers = 4, seller = MarketSeller(id = "seller-002", name = "Demo Seller"), stl = 0, status = 1, prlo = 50_000_000, owner = null, exs = 1_735_430_399),
            MarketPlayer(id = "demo-market-003", firstName = "Lamine", lastName = "Yamal", profileBigUrl = "", teamName = "FC Barcelona", teamId = "3", position = 3, number = 27, averagePoints = 7.2, totalPoints = 86, marketValue = 75_000_000, marketValueTrend = 3_500_000, price = 68_000_000, expiry = "2025-12-20T23:59:59Z", offers = 1, seller = MarketSeller(id = "seller-003", name = "Team Lead"), stl = 0, status = 1, prlo = 65_000_000, owner = null, exs = 1_735_862_399),
            MarketPlayer(id = "demo-market-004", firstName = "Vinícius", lastName = "Júnior", profileBigUrl = "", teamName = "Real Madrid", teamId = "2", position = 4, number = 20, averagePoints = 8.4, totalPoints = 100, marketValue = 110_000_000, marketValueTrend = 4_000_000, price = 95_000_000, expiry = "2025-12-25T23:59:59Z", offers = 0, seller = MarketSeller(id = "seller-004", name = "Whale"), stl = 0, status = 1, prlo = 90_000_000, owner = null, exs = 1_735_948_799),
            MarketPlayer(id = "demo-market-005", firstName = "Joshua", lastName = "Kimmich", profileBigUrl = "", teamName = "FC Bayern", teamId = "1", position = 2, number = 32, averagePoints = 6.7, totalPoints = 80, marketValue = 32_000_000, marketValueTrend = -1_000_000, price = 28_000_000, expiry = "2025-12-05T23:59:59Z", offers = 3, seller = MarketSeller(id = "seller-005", name = "Casual Player"), stl = 0, status = 1, prlo = 27_000_000, owner = null, exs = 1_735_084_799)
        )

        // MARK: - Demo User Stats

        fun createDemoUserStats(): UserStats = UserStats(teamValue = 45_000_000, teamValueTrend = 500_000, budget = 2_500_000, points = 287, placement = 5, won = 8, drawn = 2, lost = 5)

        // MARK: - Demo Market Value History

        fun createDemoMarketValueHistory(): MarketValueChange {
            val dailyChanges = arrayOf(
                DailyMarketValueChange(date = "24. Nov", value = 45_500_000, change = 200_000, percentageChange = 0.44, daysAgo = 0),
                DailyMarketValueChange(date = "23. Nov", value = 45_300_000, change = 100_000, percentageChange = 0.22, daysAgo = 1),
                DailyMarketValueChange(date = "22. Nov", value = 45_200_000, change = -300_000, percentageChange = -0.66, daysAgo = 2)
            )

            return MarketValueChange(daysSinceLastUpdate = 1, absoluteChange = 500_000, percentageChange = 1.12, previousValue = 45_000_000, currentValue = 45_500_000, dailyChanges = dailyChanges)
        }

        // MARK: - Demo Login Response

        fun createDemoLoginResponse(): LoginResponse {
            val user = createDemoUser()
            return LoginResponse(tkn = "demo-token-${UUID().uuidString}", user = user, leagues = createDemoLeagues(), userId = user.id)
        }
    }
}
