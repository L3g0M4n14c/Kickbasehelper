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

@Stable
open class KickbasePlayerService: ObservableObject {
    override val objectWillChange = ObservableObjectPublisher()
    private val apiService: KickbaseAPIServiceProtocol
    private val dataParser: KickbaseDataParserProtocol

    // Simple in-memory caches (MainActor only)
    private var playerPerformanceCache: Dictionary<String, Tuple2<Date, Array<EnhancedMatchPerformance>>> = dictionaryOf()
        get() = field.sref({ this.playerPerformanceCache = it })
        set(newValue) {
            field = newValue.sref()
        }
    private var teamProfileCache: Dictionary<String, Tuple2<Date, TeamInfo>> = dictionaryOf()
        get() = field.sref({ this.teamProfileCache = it })
        set(newValue) {
            field = newValue.sref()
        }
    private val playerPerformanceCacheTTL: Double = 5 * 60 // 5 minutes
    private val teamProfileCacheTTL: Double = 10 * 60 // 10 minutes

    constructor(apiService: KickbaseAPIServiceProtocol, dataParser: KickbaseDataParserProtocol) {
        this.apiService = apiService
        this.dataParser = dataParser
    }

    // MARK: - Current Matchday

    /// Holt Spieltag-Informationen von einem Spieler
    /// - Returns: Tuple mit (smdc: aktueller Spieltag, ismc: Spiele auf dem Platz, smc: Spiele in Startelf)
    open suspend fun getMatchDayStats(leagueId: String, playerId: String): Tuple3<Int, Int, Int>? = MainActor.run l@{
        try {
            val json = MainActor.run { apiService }.getPlayerDetails(leagueId = leagueId, playerId = playerId)
            val smdc_0 = json["smdc"] as? Int
            if (smdc_0 == null) {
                print("⚠️ smdc field not found in player details")
                return@l null
            }

            val ismc = json["ismc"] as? Int ?: 0 // Spiele auf dem Platz (Startelf + Einwechslung)
            val smc = json["smc"] as? Int ?: 0 // Spiele in Startelf (Starting Match Count)

            print("📊 Stats for player ${playerId}: matchday=${smdc_0}, gamesPlayed=${ismc}, gamesStarted=${smc}")

            return@l Tuple3(smdc_0, ismc, smc)
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error fetching match day stats: ${error.localizedDescription}")
            return@l null
        }
    }

    /// Holt den aktuellen Spieltag (smdc) von einem beliebigen Spieler
    /// Einfache Version die nur den Spieltag zurückgibt
    open suspend fun getCurrentMatchDay(leagueId: String, playerId: String): Int? = MainActor.run l@{
        getMatchDayStats(leagueId = leagueId, playerId = playerId)?.let { stats ->
            return@l stats.smdc
        }
        return@l null
    }

    // MARK: - Team Players Loading

    open suspend fun loadTeamPlayers(for_: League): Array<Player> = Async.run l@{
        val league = for_
        print("👥 Loading team players (squad) for league: ${league.name}")

        try {
            val json = MainActor.run { apiService }.getMySquad(leagueId = league.id)
            return@l parseTeamPlayersFromResponse(json, league = league)
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Failed to load team players: ${error.localizedDescription}")
            throw error as Throwable
        }
    }

    // MARK: - Market Players Loading

    open suspend fun loadMarketPlayers(for_: League): Array<MarketPlayer> = Async.run l@{
        val league = for_
        print("💰 Loading market players for league: ${league.name}")

        try {
            val json = MainActor.run { apiService }.getMarketPlayers(leagueId = league.id)
            val marketPlayers = parseMarketPlayersFromResponse(json, league = league)
            print("✅ Successfully loaded ${marketPlayers.count} market players from API")
            return@l marketPlayers.sref()
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Failed to load market players: ${error.localizedDescription}")
            throw error as Throwable
        }
    }

    // MARK: - Player Detail Loading

    open suspend fun loadPlayerDetails(playerId: String, leagueId: String): PlayerDetailResponse? = MainActor.run l@{
        try {
            val json = MainActor.run { apiService }.getPlayerDetails(leagueId = leagueId, playerId = playerId)
            print("✅ Got player details for ID: ${playerId}")

            return@l PlayerDetailResponse(fn = json["fn"] as? String, ln = json["ln"] as? String, tn = json["tn"] as? String, shn = json["shn"] as? Int, id = json["id"] as? String, position = json["position"] as? Int ?: json["pos"] as? Int, number = json["number"] as? Int, averagePoints = json["averagePoints"] as? Double, totalPoints = json["totalPoints"] as? Int ?: json["tp"] as? Int, marketValue = json["marketValue"] as? Int, marketValueTrend = json["marketValueTrend"] as? Int, profileBigUrl = json["profileBigUrl"] as? String ?: json["pim"] as? String, teamId = json["teamId"] as? String, tfhmvt = json["tfhmvt"] as? Int, prlo = json["prlo"] as? Int, stl = json["stl"] as? Int, status = json["st"] as? Int, userOwnsPlayer = json["userOwnsPlayer"] as? Boolean)
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error loading player details for ${playerId}: ${error.localizedDescription}")
            return@l null
        }
    }

    // MARK: - Player Performance Loading

    open suspend fun loadPlayerPerformance(playerId: String, leagueId: String): PlayerPerformanceResponse? = MainActor.run l@{
        print("📊 Loading player performance for ID: ${playerId}")

        try {
            val performanceResponse = MainActor.run { apiService }.getPlayerPerformance(leagueId = leagueId, playerId = playerId)
            print("✅ Successfully loaded performance data for player ${playerId}")
            return@l performanceResponse
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error loading player performance for ${playerId}: ${error.localizedDescription}")
            throw error as Throwable
        }
    }

    // MARK: - Market Value History

    open suspend fun loadPlayerMarketValueHistory(playerId: String, leagueId: String): MarketValueChange? = MainActor.run l@{
        try {
            val json = MainActor.run { apiService }.getPlayerMarketValue(leagueId = leagueId, playerId = playerId, timeframe = 365)
            print("✅ Got market value history for player ID: ${playerId}")
            return@l dataParser.parseMarketValueHistory(from = json)
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error loading market value history for ${playerId}: ${error.localizedDescription}")
            return@l null
        }
    }

    // MARK: - On-Demand Player Market Value Loading

    open suspend fun loadPlayerMarketValueOnDemand(playerId: String, leagueId: String): Int? = MainActor.run l@{
        try {
            val json = MainActor.run { apiService }.getPlayerMarketValue(leagueId = leagueId, playerId = playerId, timeframe = 365)
            val prloValue = json["prlo"] as? Int ?: 0
            print("💰 Found on-demand PRLO value: €${prloValue / 1000}k for player ID: ${playerId}")
            return@l prloValue
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error loading on-demand market value for ${playerId}: ${error.localizedDescription}")
            return@l null
        }
    }

    // MARK: - Team Profile Loading

    open suspend fun loadTeamProfile(teamId: String, leagueId: String): TeamInfo? = MainActor.run l@{
        val cacheKey = "${teamId}|${leagueId}"
        teamProfileCache[cacheKey].sref()?.let { cached ->
            if (Date().timeIntervalSince(cached.timestamp) < teamProfileCacheTTL) {
                print("📥 Returning cached team profile for ${teamId}")
                return@l cached.teamInfo
            }
        }

        print("🏆 Loading team profile for team ${teamId} in league ${leagueId}")

        try {
            val teamProfileResponse = MainActor.run { apiService }.getTeamProfile(leagueId = leagueId, teamId = teamId)
            val teamInfo = TeamInfo(from = teamProfileResponse)
            print("✅ Successfully loaded team profile: ${teamInfo.name} (Platz ${teamInfo.placement})")

            teamProfileCache[cacheKey] = Tuple2(Date(), teamInfo)
            return@l teamInfo
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error loading team profile for ${teamId}: ${error.localizedDescription}")
            return@l null
        }
    }

    // MARK: - Enhanced Performance Loading with Team Info (Optimized)

    open suspend fun loadPlayerPerformanceWithTeamInfo(playerId: String, leagueId: String): Array<EnhancedMatchPerformance>? = MainActor.run l@{
        val cacheKey = "${playerId}|${leagueId}"
        playerPerformanceCache[cacheKey].sref()?.let { cached ->
            if (Date().timeIntervalSince(cached.timestamp) < playerPerformanceCacheTTL) {
                print("📥 Returning cached enhanced matches for ${playerId}")
                return@l cached.enhancedMatches
            }
        }

        print("📊 Loading optimized player performance with team info for player ${playerId}")
        val performance_0 = loadPlayerPerformance(playerId = playerId, leagueId = leagueId)
        if (performance_0 == null) {
            print("⚠️ No performance data available")
            return@l null
        }
        val currentSeason_0 = performance_0.it.last
        if (currentSeason_0 == null) {
            print("⚠️ No performance data available")
            return@l null
        }

        // Finde den aktuellen Spieltag
        val currentMatchDay = getCurrentMatchDayFromPerformance(currentSeason_0.ph)
        print("🎯 Current match day identified as: ${currentMatchDay}")

        // Filtere nur die relevanten Spiele (letzte 5 + aktuelle + nächste 3)
        val relevantMatches = currentSeason_0.ph.filter l@{ match ->
            val matchDay = match.day
            return@l matchDay >= (currentMatchDay - 4) && matchDay <= (currentMatchDay + 3)
        }

        print("🎯 Filtered to ${relevantMatches.count} relevant matches (days ${currentMatchDay - 4} to ${currentMatchDay + 3})")

        // Sammle nur einzigartige Team-IDs aus den relevanten Matches
        var uniqueTeamIds = Set<String>()
        for (match in relevantMatches.sref()) {
            uniqueTeamIds.insert(match.t1)
            uniqueTeamIds.insert(match.t2)
        }

        print("🎯 Found ${uniqueTeamIds.count} unique teams in relevant matches: ${Array(uniqueTeamIds)}")

        // Lade Team-Informationen nur für diese Teams
        var teamInfoCache: Dictionary<String, TeamInfo> = dictionaryOf()

        for (teamId in uniqueTeamIds.sref()) {
            val matchtarget_0 = loadTeamProfile(teamId = teamId, leagueId = leagueId)
            if (matchtarget_0 != null) {
                val teamInfo = matchtarget_0
                teamInfoCache[teamId] = teamInfo
                print("✅ Cached team info for ${teamId}: ${teamInfo.name}")
            } else {
                print("⚠️ Could not load team info for ${teamId}")
            }
        }

        // Erstelle erweiterte Match-Performance Objekte nur für relevante Matches
        var enhancedMatches: Array<EnhancedMatchPerformance> = arrayOf()

        for (match in relevantMatches.sref()) {
            val team1Info = teamInfoCache[match.t1]
            val team2Info = teamInfoCache[match.t2]
            val playerTeamInfo = teamInfoCache[match.pt ?: ""]
            val opponentTeamInfo = teamInfoCache[match.opponentTeamId]

            val enhancedMatch = EnhancedMatchPerformance(basePerformance = match, team1Info = team1Info, team2Info = team2Info, playerTeamInfo = playerTeamInfo, opponentTeamInfo = opponentTeamInfo)

            enhancedMatches.append(enhancedMatch)
        }

        print("✅ Created ${enhancedMatches.count} enhanced matches with team info (optimized)")

        // Cache the enhanced matches
        playerPerformanceCache[cacheKey] = Tuple2(Date(), enhancedMatches.sref())

        return@l enhancedMatches.sref()
    }

    // MARK: - Helper function to determine current match day

    private fun getCurrentMatchDayFromPerformance(matches: Array<MatchPerformance>, fallbackMatchDay: Int? = null): Int {
        // Strategie 1: Finde den aktuellen Spieltag über "cur" Flag
        matches.first(where = { it -> it.cur == true })?.let { currentMatch ->
            print("🎯 Found current match via 'cur' flag: day ${currentMatch.day}")
            return currentMatch.day
        }

        // Strategie 2: Finde den letzten gespielten Spieltag
        val playedMatches = matches.filter { it -> it.hasPlayed }
        playedMatches.max(by = { it, it_1 -> it.day < it_1.day })?.let { lastPlayedMatch ->
            val currentDay = lastPlayedMatch.day + 1 // Nächster Spieltag nach dem letzten gespielten
            print("🎯 Determined current match day from last played: ${currentDay}")
            return currentDay
        }

        // Strategie 3: Fallback - verwende übergebenen matchDay
        fallbackMatchDay?.let { fallback ->
            print("🎯 Using provided fallback match day: ${fallback}")
            return fallback
        }

        // Fallback: Verwende Spieltag 10 als Standardwert
        print("⚠️ Using fallback current match day: 10")
        return 10
    }

    // MARK: - Parsing Methods

    private suspend fun parseTeamPlayersFromResponse(json: Dictionary<String, Any>, league: League): Array<Player> = MainActor.run l@{
        print("🔍 Parsing team players from response...")
        print("📋 Team JSON keys: ${Array(json.keys)}")

        var playersArray: Array<Dictionary<String, Any>> = arrayOf()

        // Erweiterte Suche nach Spieler-Daten (sichere, helper-basierte Suche)
        for (key in arrayOf("players", "squad", "data")) {
            val arrRaw = arrayOfDicts(from = json[key])
            val arr = arrRaw.compactMap { it -> dict(from = it) }
            if (!arr.isEmpty) {
                playersArray = arr.sref()
                print("✅ Found '${key}' array with ${arr.count} entries")
                break
            }
        }

        if (playersArray.isEmpty) {
            // Umfassende Suche in verschachtelten Strukturen
            playersArray = findPlayersInNestedStructure(json)
        }

        if (playersArray.isEmpty) {
            print("❌ NO PLAYER DATA FOUND IN RESPONSE!")
            return@l arrayOf()
        }

        print("🎯 Processing ${playersArray.count} players...")
        var parsedPlayers: Array<Player> = arrayOf()

        for ((index, playerData) in playersArray.enumerated()) {
            print("🔄 Parsing player ${index + 1}: ${Array(playerData.keys)}")

            val player = parsePlayerWithDetails(from = playerData, league = league)
            parsedPlayers.append(player)

            print("✅ Parsed player: ${player.firstName} ${player.lastName} (${player.teamName})")
        }

        print("✅ Successfully parsed ${parsedPlayers.count} team players")
        return@l parsedPlayers.sref()
    }

    private fun findPlayersInNestedStructure(json: Dictionary<String, Any>): Array<Dictionary<String, Any>> {
        print("🔍 Comprehensive search for player arrays in nested structures...")

        for ((topKey, topValue) in json.sref()) {
            val matchtarget_1 = dict(from = topValue)
            if (matchtarget_1 != null) {
                val nestedDict = matchtarget_1
                for ((nestedKey, nestedValue) in nestedDict.sref()) {
                    val raw = arrayOfDicts(from = nestedValue)
                    val array = raw.compactMap { it -> dict(from = it) }
                    if (!array.isEmpty) {
                        array.first.sref()?.let { firstItem ->
                            val keys = firstItem.keys.sref()
                            val hasPlayerKeys = keys.contains("firstName") || keys.contains("lastName") || keys.contains("name") || keys.contains("position") || keys.contains("fn") || keys.contains("ln") || keys.contains("n") || keys.contains("p")

                            if (hasPlayerKeys) {
                                print("✅ Found player-like array in ${topKey}.${nestedKey} with ${array.count} entries")
                                return array.sref()
                            }
                        }
                    }
                }
            } else {
                val raw = arrayOfDicts(from = topValue)
                val array = raw.compactMap { it -> dict(from = it) }
                if (!array.isEmpty) {
                    array.first.sref()?.let { firstItem ->
                        val keys = firstItem.keys.sref()
                        val hasPlayerKeys = keys.contains("firstName") || keys.contains("lastName") || keys.contains("name") || keys.contains("position") || keys.contains("fn") || keys.contains("ln") || keys.contains("n") || keys.contains("p")

                        if (hasPlayerKeys) {
                            print("✅ Found player-like direct array in ${topKey} with ${array.count} entries")
                            return array.sref()
                        }
                    }
                }
            }
        }

        return arrayOf()
    }

    private suspend fun parsePlayerWithDetails(from: Dictionary<String, Any>, league: League): Player = MainActor.run l@{
        val playerData = from
        val apiId = playerData["id"] as? String ?: playerData["i"] as? String ?: ""

        // Lade Player-Details vom Detail-Endpoint falls ID verfügbar
        var playerDetails: PlayerDetailResponse? = null
        if (!apiId.isEmpty) {
            playerDetails = loadPlayerDetails(playerId = apiId, leagueId = league.id)
        }

        // Namen-Extraktion mit Detail-Endpoint Fallback
        val squadName = playerData["name"] as? String ?: playerData["n"] as? String ?: ""
        val firstName: String
        val lastName: String

        if (playerDetails != null) {
            val details = playerDetails
            firstName = details.fn ?: ""
            lastName = details.ln ?: squadName
            print("   ✅ Using names from detail endpoint - fn: '${firstName}', ln: '${lastName}'")
        } else {
            firstName = ""
            lastName = squadName
            print("   ⚠️ Using squad data only - treating 'name' as lastName: '${lastName}'")
        }

        // Team-Name aus Detail-Endpoint oder Fallback
        val teamName: String
        if (playerDetails != null) {
            val details = playerDetails
            val matchtarget_2 = details.tn
            if (matchtarget_2 != null) {
                val detailTeamName = matchtarget_2
                if (!detailTeamName.isEmpty) {
                    teamName = detailTeamName
                } else {
                    teamName = playerData["tn"] as? String ?: "Unknown Team"
                }
            } else {
                teamName = playerData["tn"] as? String ?: "Unknown Team"
            }
        } else {
            teamName = playerData["tn"] as? String ?: "Unknown Team"
        }

        // Trikotnummer mit Priorität auf Detail-Endpoint
        val shirtNumber: Int
        if (playerDetails != null) {
            val details = playerDetails
            val matchtarget_3 = details.shn
            if (matchtarget_3 != null) {
                val detailShirtNumber = matchtarget_3
                shirtNumber = detailShirtNumber
            } else {
                shirtNumber = playerData["number"] as? Int ?: playerData["n"] as? Int ?: playerData["jerseyNumber"] as? Int ?: 0
            }
        } else {
            shirtNumber = playerData["number"] as? Int ?: playerData["n"] as? Int ?: playerData["jerseyNumber"] as? Int ?: 0
        }

        // Andere Felder extrahieren
        val teamId = playerData["teamId"] as? String ?: playerData["ti"] as? String ?: playerData["tid"] as? String ?: playerData["clubId"] as? String ?: ""
        val position = playerData["position"] as? Int ?: playerData["pos"] as? Int ?: playerData["p"] as? Int ?: 0
        val marketValue = playerData["marketValue"] as? Int ?: playerData["mv"] as? Int ?: playerData["value"] as? Int ?: 0
        val marketValueTrend = playerData["marketValueTrend"] as? Int ?: playerData["mvt"] as? Int ?: playerData["trend"] as? Int ?: 0
        val tfhmvt = playerData["tfhmvt"] as? Int ?: 0

        // Namen-Fallback
        val finalFirstName = if (firstName.isEmpty && lastName.isEmpty) "Unbekannter" else if (firstName.isEmpty) lastName else firstName
        val finalLastName = if (firstName.isEmpty && lastName.isEmpty) "Spieler" else if (firstName.isEmpty) "" else lastName

        val uniqueId = if (apiId.isEmpty) "${finalFirstName}-${finalLastName}-${teamId}-${shirtNumber}-${UUID().uuidString.prefix(8)}" else apiId

        return@l Player(id = uniqueId, firstName = finalFirstName, lastName = finalLastName, profileBigUrl = playerData["profileBigUrl"] as? String ?: playerData["pim"] as? String ?: playerData["imageUrl"] as? String ?: playerData["photo"] as? String ?: "", teamName = teamName, teamId = teamId, position = position, number = shirtNumber, averagePoints = dataParser.extractAveragePoints(from = playerData), totalPoints = dataParser.extractTotalPoints(from = playerData), marketValue = marketValue, marketValueTrend = marketValueTrend, tfhmvt = tfhmvt, prlo = 0, stl = playerData["stl"] as? Int ?: 0, status = playerData["st"] as? Int ?: 0, userOwnsPlayer = playerData["userOwnsPlayer"] as? Boolean ?: playerData["owned"] as? Boolean ?: playerData["mine"] as? Boolean ?: true)
    }

    private suspend fun parseMarketPlayersFromResponse(json: Dictionary<String, Any>, league: League): Array<MarketPlayer> = MainActor.run l@{
        print("🔍 === MARKET PLAYER PARSING DEBUG ===")
        print("📋 Market JSON keys: ${Array(json.keys)}")

        // Detaillierte JSON-Struktur-Analyse
        for ((key, value) in json.sref()) {
            val raw = rawArray(from = value)
            val array = raw.compactMap { it -> dict(from = it) }
            if (!array.isEmpty) {
                print("📊 Key '${key}' contains array with ${array.count} items")
                array.first.sref()?.let { firstItem ->
                    print("   First item keys: ${Array(firstItem.keys)}")
                }
            } else {
                val matchtarget_4 = dict(from = value)
                if (matchtarget_4 != null) {
                    val dict = matchtarget_4
                    print("📊 Key '${key}' contains dictionary with keys: ${Array(dict.keys)}")
                } else {
                    print("📊 Key '${key}' contains: ${type(of = value)}")
                }
            }
        }

        var playersArray: Array<Dictionary<String, Any>> = arrayOf()

        // Erweiterte Suche nach Marktspielerdaten (sichere, helper-basierte Suche)
        for (key in arrayOf("it", "players", "market", "data", "transfers", "items", "list")) {
            val arrRawAny = rawArray(from = json[key])
            val arr = arrRawAny.compactMap { it -> dict(from = it) }
            if (!arr.isEmpty) {
                playersArray = arr.sref()
                print("✅ Found '${key}' array with ${arr.count} entries")
                break
            }
        }

        if (playersArray.isEmpty) {
            // Umfassende Suche in verschachtelten Strukturen
            playersArray = findMarketPlayersInNestedStructure(json).compactMap { it -> dict(from = it) }
        }

        if (playersArray.isEmpty) {
            print("❌ NO MARKET PLAYER DATA FOUND IN RESPONSE!")
            print("📋 Available top-level keys: ${Array(json.keys)}")

            // Zusätzliche Fallback-Suche nach alternativen Feldnamen
            for ((key, value) in json.sref()) {
                val matchtarget_5 = value as? String
                if (matchtarget_5 != null) {
                    val stringValue = matchtarget_5
                    print("   ${key}: ${stringValue}")
                } else {
                    val matchtarget_6 = value as? java.lang.Number
                    if (matchtarget_6 != null) {
                        val numberValue = matchtarget_6
                        print("   ${key}: ${numberValue}")
                    } else {
                        (value as? Array<*>).sref()?.let { arrayValue ->
                            print("   ${key}: Array with ${arrayValue.count} elements")
                            arrayValue.first.sref()?.let { firstElement ->
                                print("     First element type: ${type(of = firstElement)}")
                                dict(from = firstElement)?.let { dictElement ->
                                    print("     First element keys: ${Array(dictElement.keys)}")
                                }
                            }
                        }
                    }
                }
            }

            return@l arrayOf()
        }

        print("🎯 Processing ${playersArray.count} market players...")
        var parsedPlayers: Array<MarketPlayer> = arrayOf()

        for ((index, playerData) in playersArray.enumerated()) {
            print("🔄 Parsing market player ${index + 1}: ${Array(playerData.keys)}")

            val player = parseMarketPlayerWithDetails(from = playerData, league = league)
            parsedPlayers.append(player)

            print("✅ Parsed market player: ${player.firstName} ${player.lastName} (€${player.price / 1000}k from ${player.seller.name})")
        }

        print("✅ Successfully parsed ${parsedPlayers.count} market players")
        print("🔍 === END MARKET PLAYER PARSING DEBUG ===")
        return@l parsedPlayers.sref()
    }

    private fun findMarketPlayersInNestedStructure(json: Dictionary<String, Any>): Array<*> {
        print("🔍 Comprehensive search for market player arrays in nested structures...")

        for ((topKey, topValue) in json.sref()) {
            print("🔎 Checking top-level key: ${topKey}")

            val matchtarget_7 = dict(from = topValue)
            if (matchtarget_7 != null) {
                val nestedDict = matchtarget_7
                for ((nestedKey, nestedValue) in nestedDict.sref()) {
                    val arrRawAny = rawArray(from = nestedValue)
                    if (!arrRawAny.isEmpty) {
                        val firstItem = arrRawAny.compactMap { it -> dict(from = it) }.first.sref()
                        if (firstItem != null) {
                            val keys = firstItem.keys.sref()
                            val hasMarketKeys = keys.contains("price") || keys.contains("seller") || keys.contains("expiry") || keys.contains("offers") || keys.contains("firstName") || keys.contains("lastName") || keys.contains("prc") || keys.contains("u") || keys.contains("n") || keys.contains("fn") || keys.contains("ln")

                            if (hasMarketKeys) {
                                print("✅ Found market players array at: ${topKey}.${nestedKey} with ${arrRawAny.count} items")
                                return arrRawAny.sref()
                            }
                        }
                    }
                }
            } else {
                val arrRawAny = rawArray(from = topValue)
                if (!arrRawAny.isEmpty) {
                    arrRawAny.compactMap({ it -> dict(from = it) }).first.sref()?.let { firstItem ->
                        val keys = firstItem.keys.sref()
                        val hasMarketKeys = keys.contains("price") || keys.contains("seller") || keys.contains("expiry") || keys.contains("offers") || keys.contains("firstName") || keys.contains("lastName") || keys.contains("prc") || keys.contains("u") || keys.contains("n") || keys.contains("fn") || keys.contains("ln")

                        if (hasMarketKeys) {
                            print("✅ Found market players array at top level: ${topKey} with ${arrRawAny.count} items")
                            return arrRawAny.sref()
                        }
                    }
                }
            }
        }

        print("❌ No market player arrays found in nested structure")
        return arrayOf()
    }

    private suspend fun parseMarketPlayerWithDetails(from: Dictionary<String, Any>, league: League): MarketPlayer = MainActor.run l@{
        val playerData = from
        val apiId = playerData["id"] as? String ?: playerData["i"] as? String ?: ""

        // Lade Player-Details vom Detail-Endpoint falls ID verfügbar
        var playerDetails: PlayerDetailResponse? = null
        if (!apiId.isEmpty) {
            playerDetails = loadPlayerDetails(playerId = apiId, leagueId = league.id)
        }

        // Namen-Extraktion mit Detail-Endpoint Fallback
        val marketName = playerData["n"] as? String ?: playerData["name"] as? String ?: ""
        val firstName: String
        val lastName: String

        if (playerDetails != null) {
            val details = playerDetails
            firstName = details.fn ?: ""
            lastName = details.ln ?: marketName
            print("   ✅ Using names from detail endpoint - fn: '${firstName}', ln: '${lastName}'")
        } else {
            // Fallback: Versuche firstName/lastName aus Marktdaten zu extrahieren
            firstName = playerData["firstName"] as? String ?: playerData["fn"] as? String ?: ""
            lastName = playerData["lastName"] as? String ?: playerData["ln"] as? String ?: marketName
            print("   ⚠️ Using market data only - firstName: '${firstName}', lastName: '${lastName}'")
        }

        // Team-Name aus Detail-Endpoint oder Fallback
        val teamName: String
        if (playerDetails != null) {
            val details = playerDetails
            val matchtarget_8 = details.tn
            if (matchtarget_8 != null) {
                val detailTeamName = matchtarget_8
                if (!detailTeamName.isEmpty) {
                    teamName = detailTeamName
                } else {
                    teamName = playerData["teamName"] as? String ?: playerData["tn"] as? String ?: "Unknown Team"
                }
            } else {
                teamName = playerData["teamName"] as? String ?: playerData["tn"] as? String ?: "Unknown Team"
            }
        } else {
            teamName = playerData["teamName"] as? String ?: playerData["tn"] as? String ?: "Unknown Team"
        }

        // Seller-Informationen extrahieren
        val seller = MarketSeller(id = dict(from = playerData["seller"])?.get("id") as? String ?: dict(from = playerData["u"])?.get("i") as? String ?: "", name = dict(from = playerData["seller"])?.get("name") as? String ?: dict(from = playerData["u"])?.get("n") as? String ?: "Unknown")

        // Owner-Informationen (falls vorhanden)
        val owner = extractOwnerFromPlayerData(playerData)

        // Andere Felder mit Priorität auf Detail-Endpoint
        val position = playerDetails?.position ?: playerData["position"] as? Int ?: playerData["pos"] as? Int ?: playerData["p"] as? Int ?: 0

        val number = playerDetails?.number ?: playerData["number"] as? Int ?: playerData["jerseyNumber"] as? Int ?: 0

        val marketValue = playerDetails?.marketValue ?: playerData["marketValue"] as? Int ?: playerData["mv"] as? Int ?: 0

        val marketValueTrend = playerDetails?.marketValueTrend ?: playerData["marketValueTrend"] as? Int ?: playerData["mvt"] as? Int ?: 0

        val averagePoints = playerDetails?.averagePoints ?: playerData["averagePoints"] as? Double ?: playerData["ap"] as? Double ?: 0.0

        val totalPoints = playerDetails?.totalPoints ?: playerData["totalPoints"] as? Int ?: playerData["p"] as? Int ?: 0

        // Namen-Fallback falls beide leer sind
        val finalFirstName = if (firstName.isEmpty && lastName.isEmpty) "Unbekannter" else if (firstName.isEmpty) lastName else firstName
        val finalLastName = if (firstName.isEmpty && lastName.isEmpty) "Spieler" else if (firstName.isEmpty) "" else lastName

        val uniqueId = if (apiId.isEmpty) "${finalFirstName}-${finalLastName}-${seller.id}-${UUID().uuidString.prefix(8)}" else apiId

        return@l MarketPlayer(id = uniqueId, firstName = finalFirstName, lastName = finalLastName, profileBigUrl = playerDetails?.profileBigUrl ?: playerData["profileBigUrl"] as? String ?: playerData["pim"] as? String ?: "", teamName = teamName, teamId = playerDetails?.teamId ?: playerData["teamId"] as? String ?: playerData["tid"] as? String ?: "", position = position, number = number, averagePoints = averagePoints, totalPoints = totalPoints, marketValue = marketValue, marketValueTrend = marketValueTrend, price = playerData["price"] as? Int ?: playerData["prc"] as? Int ?: 0, expiry = playerData["expiry"] as? String ?: playerData["dt"] as? String ?: "", offers = playerData["offers"] as? Int ?: playerData["ofc"] as? Int ?: 0, seller = seller, stl = playerData["stl"] as? Int ?: playerData["st"] as? Int ?: 0, status = playerDetails?.status ?: playerData["status"] as? Int ?: playerData["st"] as? Int ?: 0, prlo = playerDetails?.prlo ?: playerData["prlo"] as? Int, owner = owner, exs = playerData["exs"] as? Int ?: 0)
    }

    // MARK: - Helper Functions

    private fun extractOwnerFromPlayerData(playerData: Dictionary<String, Any>): PlayerOwner? {
        val ownerData_0 = dict(from = playerData["u"])
        if (ownerData_0 == null) {
            return null
        }
        val id_0 = ownerData_0["i"] as? String
        if (id_0 == null) {
            print("   ⚠️ Owner data missing required fields (i or n)")
            return null
        }
        val name_0 = ownerData_0["n"] as? String
        if (name_0 == null) {
            print("   ⚠️ Owner data missing required fields (i or n)")
            return null
        }

        val owner = PlayerOwner(i = id_0, n = name_0, uim = ownerData_0["uim"] as? String, isvf = ownerData_0["isvf"] as? Boolean, st = ownerData_0["st"] as? Int)

        print("   ✅ Found owner: ${owner.name} (ID: ${owner.id})")
        return owner
    }

    // MARK: - Mock Data

    private fun createMockTeamPlayers(): Array<Player> = arrayOf(
        Player(id = "mock-player-1", firstName = "Max", lastName = "Mustermann", profileBigUrl = "", teamName = "FC Demo", teamId = "40", position = 4, number = 9, averagePoints = 5.0, totalPoints = 45, marketValue = 15_000_000, marketValueTrend = 500000, tfhmvt = 250000, prlo = 3_000_000, stl = 0, status = 0, userOwnsPlayer = true),
        Player(id = "mock-player-2", firstName = "Hans", lastName = "Beispiel", profileBigUrl = "", teamName = "Demo United", teamId = "1", position = 3, number = 10, averagePoints = 4.2, totalPoints = 38, marketValue = 12_000_000, marketValueTrend = -200000, tfhmvt = -150000, prlo = -1_500_000, stl = 1, status = 0, userOwnsPlayer = true)
    )

    private fun createMockMarketPlayers(): Array<MarketPlayer> = arrayOf(
        MarketPlayer(id = "mock-market-player-1", firstName = "John", lastName = "Doe", profileBigUrl = "", teamName = "FC Mock", teamId = "50", position = 2, number = 7, averagePoints = 6.0, totalPoints = 60, marketValue = 20_000_000, marketValueTrend = 1_000_000, price = 18_000_000, expiry = "2023-12-31T23:59:59Z", offers = 5, seller = MarketSeller(id = "mock-seller-1", name = "Mock Seller"), stl = 0, status = 1, prlo = 3_500_000, owner = PlayerOwner(i = "mock-owner-1", n = "Mock Owner", uim = null, isvf = true, st = 0), exs = 1_700_000_000),
        MarketPlayer(id = "mock-market-player-2", firstName = "Jane", lastName = "Smith", profileBigUrl = "", teamName = "Mock City", teamId = "2", position = 1, number = 11, averagePoints = 7.5, totalPoints = 75, marketValue = 25_000_000, marketValueTrend = -500000, price = 22_000_000, expiry = "2024-01-15T23:59:59Z", offers = 3, seller = MarketSeller(id = "mock-seller-2", name = "Another Seller"), stl = 1, status = 1, prlo = 4_000_000, owner = null, exs = 1_700_100_000)
    )

    @androidx.annotation.Keep
    companion object: CompanionClass() {
    }
    open class CompanionClass {
    }
}
