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

import skip.foundation.*
import skip.ui.*
import skip.model.*

@Stable
open class KickbaseManager: ObservableObject {
    override val objectWillChange = ObservableObjectPublisher()
    // Published Properties für UI State
    open var leagues: Array<League>
        get() = _leagues.wrappedValue.sref({ this.leagues = it })
        set(newValue) {
            objectWillChange.send()
            _leagues.wrappedValue = newValue.sref()
        }
    var _leagues: skip.model.Published<Array<League>> = skip.model.Published(arrayOf())
    open var selectedLeague: League?
        get() = _selectedLeague.wrappedValue
        set(newValue) {
            objectWillChange.send()
            _selectedLeague.wrappedValue = newValue
        }
    var _selectedLeague: skip.model.Published<League?> = skip.model.Published(null)
    open var teamPlayers: Array<Player>
        get() = _teamPlayers.wrappedValue.sref({ this.teamPlayers = it })
        set(newValue) {
            objectWillChange.send()
            _teamPlayers.wrappedValue = newValue.sref()
        }
    var _teamPlayers: skip.model.Published<Array<Player>> = skip.model.Published(arrayOf())
    open var livePlayers: Array<LivePlayer>
        get() = _livePlayers.wrappedValue.sref({ this.livePlayers = it })
        set(newValue) {
            objectWillChange.send()
            _livePlayers.wrappedValue = newValue.sref()
        }
    var _livePlayers: skip.model.Published<Array<LivePlayer>> = skip.model.Published(arrayOf()) // New live players property
    open var eventTypeNames: Dictionary<Int, String>
        get() = _eventTypeNames.wrappedValue.sref({ this.eventTypeNames = it })
        set(newValue) {
            objectWillChange.send()
            _eventTypeNames.wrappedValue = newValue.sref()
        }
    var _eventTypeNames: skip.model.Published<Dictionary<Int, String>> = skip.model.Published(dictionaryOf())
    open var marketPlayers: Array<MarketPlayer>
        get() = _marketPlayers.wrappedValue.sref({ this.marketPlayers = it })
        set(newValue) {
            objectWillChange.send()
            _marketPlayers.wrappedValue = newValue.sref()
        }
    var _marketPlayers: skip.model.Published<Array<MarketPlayer>> = skip.model.Published(arrayOf())
    open var userStats: UserStats?
        get() = _userStats.wrappedValue
        set(newValue) {
            objectWillChange.send()
            _userStats.wrappedValue = newValue
        }
    var _userStats: skip.model.Published<UserStats?> = skip.model.Published(null)
    open var leagueUsers: Array<LeagueUser>
        get() = _leagueUsers.wrappedValue.sref({ this.leagueUsers = it })
        set(newValue) {
            objectWillChange.send()
            _leagueUsers.wrappedValue = newValue.sref()
        }
    var _leagueUsers: skip.model.Published<Array<LeagueUser>> = skip.model.Published(arrayOf())
    open var matchDayUsers: Array<LeagueUser>
        get() = _matchDayUsers.wrappedValue.sref({ this.matchDayUsers = it })
        set(newValue) {
            objectWillChange.send()
            _matchDayUsers.wrappedValue = newValue.sref()
        }
    var _matchDayUsers: skip.model.Published<Array<LeagueUser>> = skip.model.Published(arrayOf())
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

    // Track current matchday to prevent race conditions
    private var currentRequestedMatchDay: Int? = null

    // Services
    private val apiService: KickbaseAPIService
    private val dataParser: KickbaseDataParser
    private val leagueService: KickbaseLeagueService
    private val playerService: KickbasePlayerService
    private val userStatsService: KickbaseUserStatsService

    // MARK: - Public Service Access

    open val authenticatedPlayerService: KickbasePlayerService
        get() = playerService

    // Shared PlayerRecommendationService singleton for reuse (avoids re-instantiation/cache loss)
    private var sharedPlayerRecommendationService: PlayerRecommendationService
        get() {
            if (!::sharedPlayerRecommendationServicestorage.isInitialized) {
                sharedPlayerRecommendationServicestorage = PlayerRecommendationService(kickbaseManager = this)
            }
            return sharedPlayerRecommendationServicestorage
        }
        set(newValue) {
            sharedPlayerRecommendationServicestorage = newValue
        }
    private lateinit var sharedPlayerRecommendationServicestorage: PlayerRecommendationService

    open val playerRecommendationService: PlayerRecommendationService
        get() = sharedPlayerRecommendationService

    // Designated initializer with dependency injection for easier testing
    constructor(apiService: KickbaseAPIService? = null, dataParser: KickbaseDataParser? = null, leagueService: KickbaseLeagueService? = null, playerService: KickbasePlayerService? = null, userStatsService: KickbaseUserStatsService? = null) {
        val api = apiService ?: KickbaseAPIService()
        val parser = dataParser ?: KickbaseDataParser()

        this.apiService = api
        this.dataParser = parser
        this.leagueService = leagueService ?: KickbaseLeagueService(apiService = api, dataParser = parser)
        this.playerService = playerService ?: KickbasePlayerService(apiService = api, dataParser = parser)
        this.userStatsService = userStatsService ?: KickbaseUserStatsService(apiService = api, dataParser = parser)
    }

    // MARK: - Authentication

    open fun setAuthToken(token: String) {
        apiService.setAuthToken(token)
        print("🔑 Auth token set for KickbaseManager")
    }

    // MARK: - Data Loading Coordination

    open suspend fun loadUserData(): Unit = MainActor.run {
        print("📊 Loading user data...")
        loadLeagues()

        // Forciere das Laden der UserStats nach Liga-Auswahl
        selectedLeague?.let { selectedLeague ->
            print("🔄 Force reloading user stats after league selection...")
            loadUserStats(for_ = selectedLeague)
        }
    }

    open suspend fun loadLeagues(): Unit = MainActor.run {
        isLoading = true
        errorMessage = null

        try {
            val loadedLeagues = MainActor.run { leagueService }.loadLeagues()
            this.leagues = loadedLeagues

            // Wähle automatisch die erste Liga aus, wenn noch keine ausgewählt ist
            if (selectedLeague == null && !leagues.isEmpty) {
                selectedLeague = leagues.first
            }

            print("✅ Loaded ${leagues.count} leagues")
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error loading leagues: ${error}")
            errorMessage = "Fehler beim Laden der Ligen: ${error.localizedDescription}"
        }

        isLoading = false
    }

    open suspend fun loadTeamPlayers(for_: League): Unit = Async.run {
        val league = for_
        isLoading = true
        errorMessage = null

        try {
            val players = MainActor.run { playerService }.loadTeamPlayers(for_ = league)
            this.teamPlayers = players
            print("✅ Loaded ${players.count} team players")
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error loading team players: ${error}")
            errorMessage = "Fehler beim Laden der Team-Spieler: ${error.localizedDescription}"
        }

        isLoading = false
    }

    open suspend fun loadMarketPlayers(for_: League): Unit = Async.run {
        val league = for_
        isLoading = true
        errorMessage = null

        try {
            val players = MainActor.run { playerService }.loadMarketPlayers(for_ = league)
            this.marketPlayers = players
            print("✅ Loaded ${players.count} market players")
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error loading market players: ${error}")
            errorMessage = "Fehler beim Laden der Markt-Spieler: ${error.localizedDescription}"
        }

        isLoading = false
    }

    open suspend fun loadLeagueRanking(for_: League): Unit = Async.run {
        val league = for_
        isLoading = true
        errorMessage = null

        try {
            val users = MainActor.run { leagueService }.loadLeagueRanking(for_ = league)
            this.leagueUsers = users
            print("✅ Loaded ${users.count} league users")
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error loading league ranking: ${error}")
            errorMessage = "Fehler beim Laden der Liga-Tabelle: ${error.localizedDescription}"
        }

        isLoading = false
    }

    open suspend fun loadMatchDayRanking(for_: League, matchDay: Int): Unit = Async.run {
        val league = for_
        print("📊 KickbaseManager: Loading ranking for matchday ${matchDay}")

        // Clear old data immediately when new matchday is requested
        this.matchDayUsers = arrayOf()
        this.currentRequestedMatchDay = matchDay
        isLoading = true
        errorMessage = null

        try {
            val users = MainActor.run { leagueService }.loadMatchDayRanking(for_ = league, matchDay = matchDay)

            // Only update if this is still the requested matchday (prevents race conditions)
            if (this.currentRequestedMatchDay == matchDay) {
                this.matchDayUsers = users
                print("✅ Loaded ${users.count} matchday users for matchday ${matchDay}")
                print("🔄 matchDayUsers updated in KickbaseManager - triggering UI update")
            } else {
                print("⚠️ Ignoring response for matchday ${matchDay} - newer request pending for ${this.currentRequestedMatchDay ?: -1}")
            }
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error loading matchday ranking: ${error}")
            errorMessage = "Fehler beim Laden der Spieltag-Tabelle: ${error.localizedDescription}"
        }

        // Always clear loading state when request completes (even if not the current one)
        isLoading = false
        print("📊 KickbaseManager: isLoading set to false")
    }

    open suspend fun loadLivePoints(): Unit = MainActor.run l@{
        val league_0 = selectedLeague
        if (league_0 == null) {
            errorMessage = "Keine Liga ausgewählt"
            return@l
        }

        isLoading = true
        errorMessage = null

        try {
            print("🔴 Loading live points. Manager ID: ${ObjectIdentifier(this)}")
            val response = MainActor.run { apiService }.getMyEleven(leagueId = league_0.id)

            // Force UI update on main thread explicitly
            MainActor.run { ->
                this.livePlayers = response.players
                this.isLoading = false
            }
            print("✅ Loaded ${livePlayers.count} live players (on MainActor)")
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error loading live points: ${error}")
            MainActor.run { ->
                this.errorMessage = "Fehler beim Laden der Live-Punkte: ${error.localizedDescription}"
                this.isLoading = false
            }
        }
        // Removed outer isLoading = false to avoid race conditions with MainActor.run block above
    }

    // Wrapper for detail view
    open suspend fun loadPlayerMatchDetails(leagueId: String, competitionId: String, playerId: String, dayNumber: Int): PlayerMatchDetailResponse = MainActor.run l@{
        if (eventTypeNames.isEmpty) {
            loadEventDefinitions()
        }
        return@l apiService.getPlayerEventHistory(competitionId = competitionId, playerId = playerId, dayNumber = dayNumber)
    }

    open suspend fun loadEventDefinitions(): Unit = MainActor.run {
        try {
            val response = MainActor.run { apiService }.getLiveEventTypes()
            var map: Dictionary<Int, String> = dictionaryOf()

            // 1. Process standard types (it / types)
            for (type in response.types.sref()) {
                map[type.id] = type.name
            }

            // 2. Process formulas (dds) for core events
            response.formulas.sref()?.let { formulas ->
                for ((key, value) in formulas.sref()) {
                    Int(key)?.let { id ->
                        // Clean template string for display (e.g. "Goal by {0}" -> "Goal by ...")
                        // For now we use the raw template as the name, leaving semantic resolution for later
                        map[id] = value
                    }
                }
            }

            this.eventTypeNames = map
            print("✅ Loaded ${map.count} event type definitions")
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("⚠️ Failed to load event types: ${error}")
        }
    }

    open suspend fun loadUserStats(for_: League): Unit = Async.run {
        val league = for_
        isLoading = true
        errorMessage = null

        try {
            val stats = MainActor.run { userStatsService }.loadUserStats(for_ = league)
            this.userStats = stats
            print("✅ Loaded user stats")
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error loading user stats: ${error}")
            errorMessage = "Fehler beim Laden der Benutzerstatistiken: ${error.localizedDescription}"
        }

        isLoading = false
    }

    // MARK: - League Selection

    open fun selectLeague(league: League) {
        selectedLeague = league

        // Lade Daten für die neue Liga
        Task { ->
            loadTeamPlayers(for_ = league)
            loadMarketPlayers(for_ = league)
            loadUserStats(for_ = league)
        }
    }

    // MARK: - Player Market Value History

    open suspend fun loadPlayerMarketValueHistory(playerId: String, leagueId: String): MarketValueChange? = MainActor.run l@{
        print("📈 Loading market value history for player ${playerId} in league ${leagueId}")

        run {
            val history = MainActor.run { playerService }.loadPlayerMarketValueHistory(playerId = playerId, leagueId = leagueId)
            if (history != null) {
                print("✅ Successfully loaded market value history with ${history.dailyChanges.count} daily changes")
            } else {
                print("⚠️ No market value history returned from player service")
            }
            return@l history
        }
    }

    open suspend fun loadPlayerMarketValueOnDemand(playerId: String, leagueId: String): Int? = MainActor.run l@{
        print("💰 Loading on-demand market value for player ${playerId} in league ${leagueId}")

        run {
            val profit = MainActor.run { playerService }.loadPlayerMarketValueOnDemand(playerId = playerId, leagueId = leagueId)
            if (profit != null) {
                print("✅ Successfully loaded on-demand profit: €${profit}")
            } else {
                print("⚠️ No profit value returned from player service")
            }
            return@l profit
        }
    }

    // MARK: - Player Performance with Team Info

    open suspend fun loadPlayerPerformanceWithTeamInfo(playerId: String, leagueId: String): Array<EnhancedMatchPerformance>? = MainActor.run l@{
        print("📊 Loading enhanced player performance with team info for player ${playerId}")

        try {
            val enhancedMatches = MainActor.run { playerService }.loadPlayerPerformanceWithTeamInfo(playerId = playerId, leagueId = leagueId)

            if (enhancedMatches != null) {
                print("✅ Successfully loaded ${enhancedMatches.count} enhanced matches with team info")
            } else {
                print("⚠️ No enhanced performance data returned")
            }

            return@l enhancedMatches.sref()
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error loading enhanced player performance: ${error}")
            throw error as Throwable
        }
    }

    open suspend fun loadPlayerRecentPerformanceWithTeamInfo(playerId: String, leagueId: String): Array<EnhancedMatchPerformance>? = MainActor.run l@{
        print("📊 Loading recent enhanced player performance (last 5 match days) for player ${playerId}")

        try {
            val allEnhancedMatches_0 = loadPlayerPerformanceWithTeamInfo(playerId = playerId, leagueId = leagueId)
            if (allEnhancedMatches_0 == null) {
                print("⚠️ No enhanced performance data available for filtering recent matches")
                return@l null
            }
            val currentMatch_0 = allEnhancedMatches_0.first(where = { it -> it.isCurrent })
            if (currentMatch_0 == null) {
                print("⚠️ No current match day found (cur = true)")
                return@l allEnhancedMatches_0.sref()
            }

            val currentMatchDay = currentMatch_0.matchDay
            print("🎯 Found current match day: ${currentMatchDay}")

            // Filtere die letzten 5 Spieltage (inklusive aktueller)
            val recentMatches = allEnhancedMatches_0.filter { match -> match.matchDay <= currentMatchDay && match.matchDay > (currentMatchDay - 5) }.sorted { it, it_1 -> it.matchDay < it_1.matchDay }

            print("✅ Filtered to ${recentMatches.count} recent enhanced matches (days ${recentMatches.first?.matchDay ?: 0} - ${currentMatchDay})")
            return@l recentMatches.sref()
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error loading recent performance with team info: ${error}")
            return@l null
        }
    }

    open suspend fun loadPlayerUpcomingPerformanceWithTeamInfo(playerId: String, leagueId: String): Array<EnhancedMatchPerformance>? = MainActor.run l@{
        print("📊 Loading upcoming enhanced player performance (next 3 match days) for player ${playerId}")

        try {
            val allEnhancedMatches_1 = loadPlayerPerformanceWithTeamInfo(playerId = playerId, leagueId = leagueId)
            if (allEnhancedMatches_1 == null) {
                print("⚠️ No enhanced performance data available for filtering upcoming matches")
                return@l null
            }
            val currentMatch_1 = allEnhancedMatches_1.first(where = { it -> it.isCurrent })
            if (currentMatch_1 == null) {
                print("⚠️ No current match day found (cur = true)")
                return@l allEnhancedMatches_1.sref()
            }

            val currentMatchDay = currentMatch_1.matchDay
            print("🎯 Found current match day: ${currentMatchDay}")

            // Filtere die nächsten 3 Spieltage (nach dem aktuellen)
            val upcomingMatches = allEnhancedMatches_1.filter { match -> match.matchDay > currentMatchDay && match.matchDay <= (currentMatchDay + 3) }.sorted { it, it_1 -> it.matchDay < it_1.matchDay }

            print("✅ Filtered to ${upcomingMatches.count} upcoming enhanced matches (days ${currentMatchDay + 1} - ${currentMatchDay + 3})")
            return@l upcomingMatches.sref()
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error loading upcoming performance with team info: ${error}")
            return@l null
        }
    }

    // MARK: - Team Discovery and Mapping

    open suspend fun discoverAndMapTeams(): Unit = MainActor.run l@{
        print("🔍 Starting team discovery and mapping...")
        val selectedLeague_0 = selectedLeague
        if (selectedLeague_0 == null) {
            print("⚠️ No league selected for team discovery")
            return@l
        }

        var discoveredTeams: Dictionary<String, String> = dictionaryOf()

        // Sammle Team-IDs aus den Team-Spielern
        for (player in teamPlayers.sref()) {
            if (!player.teamId.isEmpty && !player.teamName.isEmpty) {
                discoveredTeams[player.teamId] = player.teamName
            }
        }

        // Sammle Team-IDs aus den Markt-Spielern
        for (player in marketPlayers.sref()) {
            if (!player.teamId.isEmpty && !player.teamName.isEmpty) {
                discoveredTeams[player.teamId] = player.teamName
            }
        }

        // Aktualisiere das Team-Mapping
        TeamMapping.updateMapping(with = discoveredTeams)

        print("✅ Discovered and mapped ${discoveredTeams.count} teams:")
        for ((id, name) in discoveredTeams.sref()) {
            print("   ${id}: ${name}")
        }
    }

    // MARK: - User Squad Loading

    /// Loads the squad (list of players) for a specific user in a league
    /// API Response Format: The getManagerSquad endpoint returns a JSON object with:
    /// - "it": [[String: Any]] - array of player objects (Squad-API specific field name)
    /// - "u": String - userId
    /// - "unm": String - userName
    /// Each player object in the array contains abbreviated field names:
    /// - "pi": String (Profile ID / Player ID)
    /// - "pn": String (Player Name - full name that needs to be split)
    /// - "pos": Int (Position: 1=TW, 2=ABW, 3=MF, 4=ST)
    /// - "tid": String (Team ID)
    /// - "p": Int (Points)
    /// - "ap": Int (Average Points)
    /// - "mv": Int (Market Value)
    /// - "mvt": Int (Market Value Trend)
    /// - "st": Int (Status)
    /// - "tfhmvt": Int (Marktwertänderung)
    /// - "prc": Int (Purchase Price)
    /// - "lo": Int (Loan-out?)
    /// - "iotm": Boolean (In Of The Moment?)
    open suspend fun loadUserSquad(leagueId: String, userId: String): Array<Player>? = MainActor.run l@{
        print("👤 Loading squad for user ${userId} in league ${leagueId}")

        try {
            val json = MainActor.run { apiService }.getManagerSquad(leagueId = leagueId, userId = userId)
            print("📋 Squad API Response keys: ${json.keys.sorted()}")

            // Squad endpoint returns players in "it" field
            val playersRaw = arrayOfDicts(from = json["it"])
            val playersArray = playersRaw.compactMap { it -> dict(from = it) }
            if (playersArray.isEmpty) {
                print("⚠️ No 'it' array found or empty in squad response. Available keys: ${json.keys.sorted()}")
                return@l null
            }

            print("🎯 Processing ${playersArray.count} players for user...")
            var parsedPlayers: Array<Player> = arrayOf()

            for (playerData in playersArray.sref()) {
                parseSquadPlayer(from = playerData)?.let { player ->
                    parsedPlayers.append(player)
                }
            }

            print("✅ Successfully loaded ${parsedPlayers.count} players for user ${userId}")
            return@l parsedPlayers.sref()
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error loading user squad: ${error}")
            return@l null
        }
    }

    /// Loads player details for the given lineup player IDs
    /// Uses available player data (teamPlayers, marketPlayers) and falls back to Player Details endpoint
    /// This is used to get player details from historical squad lineups
    open suspend fun loadPlayersForLineup(lineupPlayerIds: Array<String>, leagueId: String, userId: String): Array<Player>? = MainActor.run l@{
        if (lineupPlayerIds.isEmpty) {
            print("⚠️ loadPlayersForLineup: No lineup player IDs provided")
            return@l null
        }

        print("🎯 Loading details for ${lineupPlayerIds.count} players from lineup IDs")

        var players: Array<Player> = arrayOf()

        for (playerId in lineupPlayerIds.sref()) {
            // Try to find player in available data sources
            var player: Player? = null

            // Try teamPlayers first (TeamPlayer is a typealias for Player)
            teamPlayers.first(where = { it -> it.id == playerId })?.let { teamPlayer ->
                player = teamPlayer
                print("📍 Found player ${playerId} in teamPlayers")
            }

            // Try marketPlayers if not found
            if (player == null) {
                marketPlayers.first(where = { it -> it.id == playerId })?.let { marketPlayer ->
                    // Convert MarketPlayer to Player
                    player = Player(id = marketPlayer.id, firstName = marketPlayer.firstName, lastName = marketPlayer.lastName, profileBigUrl = marketPlayer.profileBigUrl, teamName = marketPlayer.teamName, teamId = marketPlayer.teamId, position = marketPlayer.position, number = marketPlayer.number, averagePoints = marketPlayer.averagePoints, totalPoints = marketPlayer.totalPoints, marketValue = marketPlayer.marketValue, marketValueTrend = marketPlayer.marketValueTrend, tfhmvt = 0, prlo = marketPlayer.prlo ?: 0, stl = marketPlayer.stl, status = marketPlayer.status, userOwnsPlayer = false)
                    print("📍 Found player ${playerId} in marketPlayers")
                }
            }

            // If not found in available data, try loading from Player Details endpoint
            if (player == null) {
                print("🌐 Loading player ${playerId} from Player Details endpoint")
                val matchtarget_0 = playerService.loadPlayerDetails(playerId = playerId, leagueId = leagueId)
                if (matchtarget_0 != null) {
                    val playerDetails = matchtarget_0
                    // Log what we got
                    print("   - Name: ${playerDetails.fn ?: "?"} ${playerDetails.ln ?: "?"}")
                    print("   - Position: ${playerDetails.position ?: 0}, Team: ${playerDetails.tn ?: "?"}")
                    print("   - Points: Avg=${playerDetails.averagePoints ?: 0}, Total=${playerDetails.totalPoints ?: 0}")

                    // Check if we have the player in market players to get position
                    // The Player Details endpoint sometimes doesn't return position data
                    var position = playerDetails.position ?: 0
                    var totalPoints = playerDetails.totalPoints ?: 0

                    if (position == 0) {
                        marketPlayers.first(where = { it -> it.id == playerId })?.let { marketPlayer ->
                            position = marketPlayer.position
                            print("   ⚠️ Position missing from Details endpoint, using marketPlayers: ${position}")
                        }
                    }

                    if (totalPoints == 0) {
                        marketPlayers.first(where = { it -> it.id == playerId })?.let { marketPlayer ->
                            totalPoints = marketPlayer.totalPoints
                            print("   ⚠️ Total Points missing from Details endpoint, using marketPlayers: ${totalPoints}")
                        }
                    }

                    // Convert PlayerDetailResponse to Player
                    player = Player(id = playerDetails.id ?: playerId, firstName = playerDetails.fn ?: "?", lastName = playerDetails.ln ?: "?", profileBigUrl = playerDetails.profileBigUrl ?: "", teamName = playerDetails.tn ?: "?", teamId = playerDetails.teamId ?: "", position = position, number = playerDetails.number ?: 0, averagePoints = playerDetails.averagePoints ?: 0, totalPoints = totalPoints, marketValue = playerDetails.marketValue ?: 0, marketValueTrend = playerDetails.marketValueTrend ?: 0, tfhmvt = playerDetails.tfhmvt ?: 0, prlo = playerDetails.prlo ?: 0, stl = playerDetails.stl ?: 0, status = playerDetails.status ?: 0, userOwnsPlayer = playerDetails.userOwnsPlayer ?: false)
                    print("✅ Loaded player ${playerId} from Player Details endpoint: ${player?.fullName ?: "unknown"}")
                } else {
                    print("❌ Could not load player ${playerId} from Player Details endpoint - returned nil")
                }
            }

            // If we have a player, add to list
            if (player != null) {
                players.append(player)
            } else {
                print("⚠️ Could not find player details for ID ${playerId}")
            }
        }

        print("✅ Loaded ${players.count} out of ${lineupPlayerIds.count} players from lineup IDs")
        if (players.count != lineupPlayerIds.count) {
            print("⚠️ Warning: Only found ${players.count} out of ${lineupPlayerIds.count} players")
        }
        return@l (if (players.isEmpty) null else players).sref()
    }

    /// Parses a player from squad API data, handling Squad-specific abbreviated field names
    /// The Squad API uses different field names than the regular player endpoints
    private fun parseSquadPlayer(from: Dictionary<String, Any>): Player? {
        val playerData = from
        // Player ID - required field
        val playerId = playerData["pi"] as? String ?: playerData["id"] as? String ?: playerData["i"] as? String ?: ""
        if (playerId.isEmpty) {
            print("⚠️ Skipping player with no ID. Available keys: ${playerData.keys.sorted()}")
            return null
        }

        // Parse player name from "pn" field - comes as full name "Vorname Nachname"
        val fullName = playerData["pn"] as? String ?: ""
        val (firstName, lastName) = parseFullName(fullName)

        // Profile image URL - use "pi" as fallback? No, that's ID. Check for profile image fields
        val profileBigUrl = playerData["pim"] as? String ?: playerData["prf"] as? String ?: ""

        // Team information
        val teamName = playerData["tn"] as? String ?: ""
        val teamId = playerData["tid"] as? String ?: ""

        // Position and number
        val position = playerData["pos"] as? Int ?: playerData["position"] as? Int ?: 0
        val number = playerData["n"] as? Int ?: playerData["number"] as? Int ?: 0

        // Points and values
        val averagePoints = Double(playerData["ap"] as? Int ?: 0)
        val totalPoints = playerData["p"] as? Int ?: 0
        val marketValue = playerData["mv"] as? Int ?: 0
        val marketValueTrend = playerData["mvt"] as? Int ?: 0
        val tfhmvt = playerData["tfhmvt"] as? Int ?: 0
        val prlo = playerData["prc"] as? Int ?: 0 // Purchase price as proxy for profit/loss
        val stl = playerData["lo"] as? Int ?: 0 // Loan out indicator
        val status = playerData["st"] as? Int ?: 0
        val userOwnsPlayer = playerData["iotm"] as? Boolean ?: true

        return Player(id = playerId, firstName = firstName, lastName = lastName, profileBigUrl = profileBigUrl, teamName = teamName, teamId = teamId, position = position, number = number, averagePoints = averagePoints, totalPoints = totalPoints, marketValue = marketValue, marketValueTrend = marketValueTrend, tfhmvt = tfhmvt, prlo = prlo, stl = stl, status = status, userOwnsPlayer = userOwnsPlayer)
    }

    /// Splits a full name string into firstName and lastName
    /// Assumes format: "Vorname Nachname" or "Vorname Middlename Nachname"
    private fun parseFullName(fullName: String): Tuple2<String, String> {
        val components = fullName.split(separator = ' ').map(String)

        if (components.isEmpty) {
            return Tuple2("", "")
        } else if (components.count == 1) {
            return Tuple2(components[0], "")
        } else {
            // Last component is lastName, rest is firstName
            val lastName = components.last ?: ""
            val firstName = components.dropLast().joined(separator = " ")
            return Tuple2(firstName, lastName)
        }
    }

    @androidx.annotation.Keep
    companion object: CompanionClass() {
    }
    open class CompanionClass {
    }
}
