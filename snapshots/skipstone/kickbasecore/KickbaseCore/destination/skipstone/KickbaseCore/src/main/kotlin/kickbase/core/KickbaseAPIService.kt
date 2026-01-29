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

import skip.model.*
import skip.foundation.*
import skip.ui.*

/// Hauptservice für alle Kickbase API v4 Endpoints basierend auf der Swagger-Dokumentation
/// Quelle: https://github.com/kevinskyba/kickbase-api-doc
@Stable
open class KickbaseAPIService: ObservableObject, KickbaseAPIServiceProtocol {
    override val objectWillChange = ObservableObjectPublisher()
    private val baseURL = "https://api.kickbase.com"
    private var authToken: String? = null
    // Exposed as internal for tests
    internal val session: URLSession

    constructor(session: URLSession = URLSession.shared) {
        this.session = SessionSanitizer.sanitized(session)
    }

    // MARK: - Authentication

    open fun setAuthToken(token: String) {
        authToken = token
        print("🔑 Auth token set for KickbaseAPIService")
    }

    open fun hasAuthToken(): Boolean = authToken != null

    // MARK: - Generic Request Methods

    private suspend fun makeRequest(endpoint: String, method: String = "GET", body: Data? = null, requiresAuth: Boolean = true): Tuple2<Data, HTTPURLResponse> = MainActor.run l@{
        if (requiresAuth) {
            if (authToken == null) {
                throw APIError.noAuthToken
            }
        }
        val url_0 = (try { URL(string = "${baseURL}${endpoint}") } catch (_: NullReturnException) { null })
        if (url_0 == null) {
            throw APIError.invalidURL
        }

        var request = URLRequest(url = url_0)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField = "Accept")
        request.setValue("application/json", forHTTPHeaderField = "Content-Type")
        request.cachePolicy = URLRequest.CachePolicy.reloadIgnoringLocalCacheData

        if (requiresAuth) {
            authToken?.let { token ->
                request.setValue("Bearer ${token}", forHTTPHeaderField = "Authorization")
            }
        }

        if (body != null) {
            request.httpBody = body
        }

        print("📤 ${method} ${endpoint}")

        val (data, response) = MainActor.run { this.session }.data(for_ = request)
        val httpResponse_0 = response as? HTTPURLResponse
        if (httpResponse_0 == null) {
            throw APIError.noHTTPResponse
        }

        print("📊 Response: ${httpResponse_0.statusCode}")
        if (!(200..299).contains(httpResponse_0.statusCode)) {
            if (httpResponse_0.statusCode == 401) {
                throw APIError.authenticationFailed
            }
            throw APIError.networkError("HTTP ${httpResponse_0.statusCode}")
        }

        return@l Tuple2(data.sref(), httpResponse_0)
    }

    // MARK: - User Endpoints

    /// POST /v4/user/login - User Login
    open suspend fun login(email: String, password: String): LoginResponse = MainActor.run l@{
        val loginRequest = LoginRequest(email = email, password = password)
        val encoder = JSONEncoder()
        val body = encoder.encode(loginRequest)
        val url_1 = (try { URL(string = "${baseURL}/v4/user/login") } catch (_: NullReturnException) { null })
        if (url_1 == null) {
            throw NSError(domain = "InvalidURL", code = -1)
        }

        var request = URLRequest(url = url_1)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField = "Accept")
        request.setValue("application/json", forHTTPHeaderField = "Content-Type")
        request.cachePolicy = URLRequest.CachePolicy.reloadIgnoringLocalCacheData
        request.httpBody = body

        print("📤 POST /v4/user/login")

        val (data, response) = MainActor.run { this.session }.data(for_ = request)
        val httpResponse_1 = response as? HTTPURLResponse
        if (httpResponse_1 == null) {
            throw NSError(domain = "NoHTTPResponse", code = -1)
        }

        print("📊 Response: ${httpResponse_1.statusCode}")

        // Debug: Log die rohe Response
        String(data = data, encoding = StringEncoding.utf8)?.let { jsonString ->
            print("📥 Login Response: ${jsonString}")
        }

        // Spezifische Fehlerbehandlung für Login
        if (httpResponse_1.statusCode == 401) {
            print("❌ HTTP 401: Invalid credentials")
            throw NSError(domain = "InvalidCredentials", code = 401, userInfo = dictionaryOf(
                Tuple2(NSLocalizedDescriptionKey, "Ungültige E-Mail oder Passwort. Bitte überprüfen Sie Ihre Anmeldedaten.")
            ))
        }
        if (!(200..299).contains(httpResponse_1.statusCode)) {
            print("❌ HTTP ${httpResponse_1.statusCode}")
            throw NSError(domain = "HTTPError", code = httpResponse_1.statusCode, userInfo = dictionaryOf(
                Tuple2(NSLocalizedDescriptionKey, "Anmeldung fehlgeschlagen. Server antwortet mit HTTP ${httpResponse_1.statusCode}. Bitte versuchen Sie es später erneut.")
            ))
        }

        val decoder = JSONDecoder()
        try {
            return@l decoder.decode(LoginResponse::class, from = data)
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Decode error: ${error}")
            throw NSError(domain = "DecodingError", code = -1, userInfo = dictionaryOf(
                Tuple2(NSLocalizedDescriptionKey, "Unerwartete Serverantwort. Bitte versuchen Sie es erneut.")
            ))
        }
    }

    /// GET /v4/user/settings - Account Settings
    open suspend fun getUserSettings(): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/user/settings")
        return@l jsonDict(from = data)
    }

    // MARK: - Base Endpoints

    /// GET /v4/base/overview - Base Overview
    open suspend fun getBaseOverview(): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/base/overview")
        return@l jsonDict(from = data)
    }

    // MARK: - Bonus Endpoints

    /// GET /v4/bonus/collect - Bonus Collection
    open suspend fun collectBonus(): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/bonus/collect")
        return@l jsonDict(from = data)
    }

    // MARK: - League Endpoints

    /// GET /v4/leagues/selection - List all Leagues
    override suspend fun getLeagueSelection(): Dictionary<String, Any> = MainActor.run l@{
        // Public endpoint - does not require auth
        val (data, _) = makeRequest(endpoint = "/v4/leagues/selection", requiresAuth = false)
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/overview - League Overview
    open suspend fun getLeagueOverview(leagueId: String, matchDay: Int? = null): Dictionary<String, Any> = MainActor.run l@{
        var endpoint = "/v4/leagues/${leagueId}/overview"
        if (matchDay != null) {
            endpoint += "?matchDay=${matchDay}"
        }
        val (data, _) = makeRequest(endpoint = endpoint)
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/me - My Stats in League
    override suspend fun getLeagueMe(leagueId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/me")
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/me/budget - My Budget
    override suspend fun getMyBudget(leagueId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/me/budget")
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/squad - My Squad/Team Players
    override suspend fun getMySquad(leagueId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/squad")
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/lineup - My Lineup
    open suspend fun getMyLineup(leagueId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/lineup")
        return@l jsonDict(from = data)
    }

    /// POST /v4/leagues/{leagueId}/lineup - Update My Lineup
    open suspend fun updateMyLineup(leagueId: String, lineup: Array<Int>): Dictionary<String, Any> = MainActor.run l@{
        val body = JSONSerialization.data(withJSONObject = lineup)
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/lineup", method = "POST", body = body)
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/teamcenter/myeleven - My Eleven
    open suspend fun getMyEleven(leagueId: String): LiveMatchDayResponse = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/teamcenter/myeleven")
        val decoder = JSONDecoder()
        return@l decoder.decode(LiveMatchDayResponse::class, from = data)
    }

    /// GET /v4/competitions/{competitionId}/playercenter/{playerId} - Player Match Detail for specific day
    open suspend fun getPlayerEventHistory(competitionId: String, playerId: String, dayNumber: Int): PlayerMatchDetailResponse = MainActor.run l@{
        val endpoint = "/v4/competitions/${competitionId}/playercenter/${playerId}?day=${dayNumber}"
        val (data, _) = makeRequest(endpoint = endpoint)
        val decoder = JSONDecoder()
        return@l decoder.decode(PlayerMatchDetailResponse::class, from = data)
    }

    /// GET /v4/leagues/{leagueId}/ranking - League Ranking
    override suspend fun getLeagueRanking(leagueId: String, matchDay: Int?): Dictionary<String, Any> = MainActor.run l@{
        var endpoint = "/v4/leagues/${leagueId}/ranking"
        if (matchDay != null) {
            endpoint += "?dayNumber=${matchDay}"
            print("🌐 API: Calling ranking endpoint with dayNumber parameter: ${endpoint}")
        } else {
            print("🌐 API: Calling ranking endpoint without dayNumber: ${endpoint}")
        }
        val (data, _) = makeRequest(endpoint = endpoint)
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/settings - League Settings (Admin Only)
    open suspend fun getLeagueSettings(leagueId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/settings")
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/settings/managers - League Members (Admin Only)
    open suspend fun getLeagueManagers(leagueId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/settings/managers")
        return@l jsonDict(from = data)
    }

    // MARK: - Manager Endpoints

    /// GET /v4/leagues/{leagueId}/managers/{userId}/dashboard - Manager Profile
    open suspend fun getManagerDashboard(leagueId: String, userId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/managers/${userId}/dashboard")
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/managers/{userId}/performance - Manager Performance
    open suspend fun getManagerPerformance(leagueId: String, userId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/managers/${userId}/performance")
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/managers/{userId}/squad - Manager Squad Details
    open suspend fun getManagerSquad(leagueId: String, userId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/managers/${userId}/squad")
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/managers/{userId}/transfer - Manager Transfers
    open suspend fun getManagerTransfers(leagueId: String, userId: String, matchDay: Int? = null): Dictionary<String, Any> = MainActor.run l@{
        var endpoint = "/v4/leagues/${leagueId}/managers/${userId}/transfer"
        if (matchDay != null) {
            endpoint += "?matchDay=${matchDay}"
        }
        val (data, _) = makeRequest(endpoint = endpoint)
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/users/{userId}/teamcenter - Team Center
    open suspend fun getTeamCenter(leagueId: String, userId: String, matchDay: Int? = null): Dictionary<String, Any> = MainActor.run l@{
        var endpoint = "/v4/leagues/${leagueId}/users/${userId}/teamcenter"
        if (matchDay != null) {
            endpoint += "?matchDay=${matchDay}"
        }
        val (data, _) = makeRequest(endpoint = endpoint)
        return@l jsonDict(from = data)
    }

    // MARK: - Player Endpoints

    /// GET /v4/leagues/{leagueId}/players/{playerId} - Player Details
    override suspend fun getPlayerDetails(leagueId: String, playerId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/players/${playerId}")
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/players/{playerId}/performance - Player Performance
    override suspend fun getPlayerPerformance(leagueId: String, playerId: String): PlayerPerformanceResponse = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/players/${playerId}/performance")
        val decoder = JSONDecoder()
        return@l decoder.decode(PlayerPerformanceResponse::class, from = data)
    }

    /// GET /v4/leagues/{leagueId}/players/{playerId}/marketvalue/{timeframe} - Player Market Value History
    override suspend fun getPlayerMarketValue(leagueId: String, playerId: String, timeframe: Int): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/players/${playerId}/marketvalue/${timeframe}")
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/players/{playerId}/transferHistory - Player Transfer History
    open suspend fun getPlayerTransferHistory(leagueId: String, playerId: String, matchDay: Int? = null): Dictionary<String, Any> = MainActor.run l@{
        var endpoint = "/v4/leagues/${leagueId}/players/${playerId}/transferHistory"
        if (matchDay != null) {
            endpoint += "?matchDay=${matchDay}"
        }
        val (data, _) = makeRequest(endpoint = endpoint)
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/players/{playerId}/transfers - Player Transfers
    open suspend fun getPlayerTransfers(leagueId: String, playerId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/players/${playerId}/transfers")
        return@l jsonDict(from = data)
    }

    // MARK: - Market Endpoints

    /// GET /v4/leagues/{leagueId}/market - Get Players On Transfer Market
    override suspend fun getMarketPlayers(leagueId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/market")
        return@l jsonDict(from = data)
    }

    /// POST /v4/leagues/{leagueId}/market - Set Player Transfer Price
    open suspend fun setPlayerTransferPrice(leagueId: String, playerId: String, price: Int): Dictionary<String, Any> = MainActor.run l@{
        val body = JSONSerialization.data(withJSONObject = dictionaryOf(
            Tuple2("playerId", playerId),
            Tuple2("price", price)
        ))
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/market", method = "POST", body = body)
        return@l jsonDict(from = data)
    }

    /// DELETE /v4/leagues/{leagueId}/market/{playerId} - Remove Player From Market
    open suspend fun removePlayerFromMarket(leagueId: String, playerId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/market/${playerId}", method = "DELETE")
        return@l jsonDict(from = data)
    }

    /// POST /v4/leagues/{leagueId}/market/{playerId}/offers - Place An Offer
    open suspend fun placeOffer(leagueId: String, playerId: String, price: Int): Dictionary<String, Any> = MainActor.run l@{
        val body = JSONSerialization.data(withJSONObject = dictionaryOf(Tuple2("price", price)))
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/market/${playerId}/offers", method = "POST", body = body)
        return@l jsonDict(from = data)
    }

    /// DELETE /v4/leagues/{leagueId}/market/{playerId}/offers/{offerId} - Withdraw Offer
    open suspend fun withdrawOffer(leagueId: String, playerId: String, offerId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/market/${playerId}/offers/${offerId}", method = "DELETE")
        return@l jsonDict(from = data)
    }

    /// DELETE /v4/leagues/{leagueId}/market/{playerId}/offers/{offerId}/accept - Accept Manager Offer
    open suspend fun acceptOffer(leagueId: String, playerId: String, offerId: String): Unit = MainActor.run {
        makeRequest(endpoint = "/v4/leagues/${leagueId}/market/${playerId}/offers/${offerId}/accept", method = "DELETE")
    }

    /// DELETE /v4/leagues/{leagueId}/market/{playerId}/offers/{offerId}/decline - Decline Manager Offer
    open suspend fun declineOffer(leagueId: String, playerId: String, offerId: String): Unit = MainActor.run {
        makeRequest(endpoint = "/v4/leagues/${leagueId}/market/${playerId}/offers/${offerId}/decline", method = "DELETE")
    }

    /// DELETE /v4/leagues/{leagueId}/market/{playerId}/sell - Accept Kickbase Offer
    open suspend fun acceptKickbaseOffer(leagueId: String, playerId: String): Unit = MainActor.run {
        makeRequest(endpoint = "/v4/leagues/${leagueId}/market/${playerId}/sell", method = "DELETE")
    }

    // MARK: - Scouted Players Endpoints

    /// GET /v4/leagues/{leagueId}/scoutedplayers - Get Scouted Players List
    open suspend fun getScoutedPlayers(leagueId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/scoutedplayers")
        return@l jsonDict(from = data)
    }

    /// POST /v4/leagues/{leagueId}/scoutedplayers/{playerId} - Add Player To Scouted List
    open suspend fun addScoutedPlayer(leagueId: String, playerId: String): Unit = MainActor.run {
        makeRequest(endpoint = "/v4/leagues/${leagueId}/scoutedplayers/${playerId}", method = "POST")
    }

    /// DELETE /v4/leagues/{leagueId}/scoutedplayers/{playerId} - Remove Player From Scouted List
    open suspend fun removeScoutedPlayer(leagueId: String, playerId: String): Unit = MainActor.run {
        makeRequest(endpoint = "/v4/leagues/${leagueId}/scoutedplayers/${playerId}", method = "DELETE")
    }

    /// DELETE /v4/leagues/{leagueId}/scoutedplayers - Clear Scouted Players List
    open suspend fun clearScoutedPlayers(leagueId: String): Unit = MainActor.run {
        makeRequest(endpoint = "/v4/leagues/${leagueId}/scoutedplayers", method = "DELETE")
    }

    // MARK: - Activities Feed Endpoints

    /// GET /v4/leagues/{leagueId}/activitiesFeed - Get Activity Feed
    open suspend fun getActivitiesFeed(leagueId: String, start: Int? = null, max: Int? = null): Dictionary<String, Any> = MainActor.run l@{
        var endpoint = "/v4/leagues/${leagueId}/activitiesFeed"
        var params: Array<String> = arrayOf()
        if (start != null) {
            params.append("start=${start}")
        }
        if (max != null) {
            params.append("max=${max}")
        }
        if (!params.isEmpty) {
            endpoint += "?" + params.joined(separator = "&")
        }
        val (data, _) = makeRequest(endpoint = endpoint)
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/activitiesFeed/{activityId} - Get Feed Item
    open suspend fun getFeedItem(leagueId: String, activityId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/activitiesFeed/${activityId}")
        return@l jsonDict(from = data)
    }

    /// POST /v4/leagues/{leagueId}/activitiesFeed/{activityId} - Send Feed Item Comment
    open suspend fun sendFeedItemComment(leagueId: String, activityId: String, comment: String): Unit = MainActor.run {
        val body = JSONSerialization.data(withJSONObject = dictionaryOf(Tuple2("comment", comment)))
        makeRequest(endpoint = "/v4/leagues/${leagueId}/activitiesFeed/${activityId}", method = "POST", body = body)
    }

    /// GET /v4/leagues/{leagueId}/activitiesFeed/{activityId}/comments - Get Feed Item Comments
    open suspend fun getFeedItemComments(leagueId: String, activityId: String, start: Int? = null, max: Int? = null): Dictionary<String, Any> = MainActor.run l@{
        var endpoint = "/v4/leagues/${leagueId}/activitiesFeed/${activityId}/comments"
        var params: Array<String> = arrayOf()
        if (start != null) {
            params.append("start=${start}")
        }
        if (max != null) {
            params.append("max=${max}")
        }
        if (!params.isEmpty) {
            endpoint += "?" + params.joined(separator = "&")
        }
        val (data, _) = makeRequest(endpoint = endpoint)
        return@l jsonDict(from = data)
    }

    // MARK: - Achievements Endpoints

    /// GET /v4/leagues/{leagueId}/user/achievements - Get All Achievements
    open suspend fun getUserAchievements(leagueId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/user/achievements")
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/user/achievements/{type} - Get Achievements By Type
    open suspend fun getUserAchievementsByType(leagueId: String, type: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/user/achievements/${type}")
        return@l jsonDict(from = data)
    }

    /// GET /v4/leagues/{leagueId}/battles/{type}/users - Battle By Type
    open suspend fun getBattleByType(leagueId: String, type: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/battles/${type}/users")
        return@l jsonDict(from = data)
    }

    // MARK: - Team Endpoints

    /// GET /v4/leagues/{leagueId}/teams/{teamId}/teamprofile - Team Profile (All Players)
    override suspend fun getTeamProfile(leagueId: String, teamId: String): TeamProfileResponse = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/leagues/${leagueId}/teams/${teamId}/teamprofile")
        val decoder = JSONDecoder()
        return@l decoder.decode(TeamProfileResponse::class, from = data)
    }

    // MARK: - Competition Endpoints

    /// GET /v4/competitions/{competitionId}/overview - Competition Details
    open suspend fun getCompetitionOverview(competitionId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/competitions/${competitionId}/overview")
        return@l jsonDict(from = data)
    }

    /// GET /v4/competitions/{competitionId}/players - Competition Players
    open suspend fun getCompetitionPlayers(competitionId: String, matchDay: Int? = null): Dictionary<String, Any> = MainActor.run l@{
        var endpoint = "/v4/competitions/${competitionId}/players"
        if (matchDay != null) {
            endpoint += "?matchDay=${matchDay}"
        }
        val (data, _) = makeRequest(endpoint = endpoint)
        return@l jsonDict(from = data)
    }

    /// GET /v4/competitions/{competitionId}/players/search - Search Competition Players
    open suspend fun searchCompetitionPlayers(competitionId: String, query: String, sorting: String? = null, start: Int? = null, max: Int? = null): Dictionary<String, Any> = MainActor.run l@{
        var endpoint = "/v4/competitions/${competitionId}/players/search?query=${query}"
        if (sorting != null) {
            endpoint += "&sorting=${sorting}"
        }
        if (start != null) {
            endpoint += "&start=${start}"
        }
        if (max != null) {
            endpoint += "&max=${max}"
        }
        val (data, _) = makeRequest(endpoint = endpoint)
        return@l jsonDict(from = data)
    }

    /// GET /v4/competitions/{competitionId}/players/{playerId}/performance - Competition Player Performance
    open suspend fun getCompetitionPlayerPerformance(competitionId: String, playerId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/competitions/${competitionId}/players/${playerId}/performance")
        return@l jsonDict(from = data)
    }

    /// GET /v4/competitions/{competitionId}/players/{playerId}/marketvalue/{timeframe} - Competition Player Market Value
    open suspend fun getCompetitionPlayerMarketValue(competitionId: String, playerId: String, timeframe: Int = 365): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/competitions/${competitionId}/players/${playerId}/marketvalue/${timeframe}")
        return@l jsonDict(from = data)
    }

    /// GET /v4/competitions/{competitionId}/playercenter/{playerId} - Player Event History
    open suspend fun getPlayerEventHistory(competitionId: String, playerId: String, matchDay: Int? = null): Dictionary<String, Any> = MainActor.run l@{
        var endpoint = "/v4/competitions/${competitionId}/playercenter/${playerId}"
        if (matchDay != null) {
            endpoint += "?matchDay=${matchDay}"
        }
        val (data, _) = makeRequest(endpoint = endpoint)
        return@l jsonDict(from = data)
    }

    /// GET /v4/competitions/{competitionId}/ranking - Team Ranking
    open suspend fun getCompetitionRanking(competitionId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/competitions/${competitionId}/ranking")
        return@l jsonDict(from = data)
    }

    /// GET /v4/competitions/{competitionId}/table - Competition Table
    open suspend fun getCompetitionTable(competitionId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/competitions/${competitionId}/table")
        return@l jsonDict(from = data)
    }

    /// GET /v4/competitions/{competitionId}/matchdays - Fixtures
    open suspend fun getCompetitionMatchdays(competitionId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/competitions/${competitionId}/matchdays")
        return@l jsonDict(from = data)
    }

    /// GET /v4/competitions/{competitionId}/teams/{teamId}/teamcenter - Matchday Players
    open suspend fun getCompetitionTeamCenter(competitionId: String, teamId: String, matchDay: Int? = null): Dictionary<String, Any> = MainActor.run l@{
        var endpoint = "/v4/competitions/${competitionId}/teams/${teamId}/teamcenter"
        if (matchDay != null) {
            endpoint += "?matchDay=${matchDay}"
        }
        val (data, _) = makeRequest(endpoint = endpoint)
        return@l jsonDict(from = data)
    }

    /// GET /v4/competitions/{competitionId}/teams/{teamId}/teamprofile - Competition Team Profile
    open suspend fun getCompetitionTeamProfile(competitionId: String, teamId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/competitions/${competitionId}/teams/${teamId}/teamprofile")
        return@l jsonDict(from = data)
    }

    // MARK: - Match Endpoints

    /// GET /v4/matches/{matchId}/details - Match Details
    open suspend fun getMatchDetails(matchId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/matches/${matchId}/details")
        return@l jsonDict(from = data)
    }

    /// GET /v4/matches/{matchId}/betlink - Match Betlink
    open suspend fun getMatchBetlink(matchId: String): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/matches/${matchId}/betlink")
        return@l jsonDict(from = data)
    }

    // MARK: - Live Endpoints

    /// GET /v4/live/eventtypes - Event Types
    open suspend fun getLiveEventTypes(): LiveEventTypesResponse = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/live/eventtypes")
        val decoder = JSONDecoder()
        return@l decoder.decode(LiveEventTypesResponse::class, from = data)
    }

    // MARK: - Config Endpoints

    /// GET /v4/config - Get Config
    open suspend fun getConfig(): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/config")
        return@l jsonDict(from = data)
    }

    // MARK: - Chat Endpoints

    /// GET /v4/chat/leagueselection - Chat League Selection
    open suspend fun getChatLeagueSelection(): String = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/chat/leagueselection")
        return@l String(data = data, encoding = StringEncoding.utf8) ?: ""
    }

    /// GET /v4/chat/refreshtoken - Chat Refresh Token
    open suspend fun getChatRefreshToken(): Dictionary<String, Any> = MainActor.run l@{
        val (data, _) = makeRequest(endpoint = "/v4/chat/refreshtoken")
        return@l jsonDict(from = data)
    }

    @androidx.annotation.Keep
    companion object: CompanionClass() {
    }
    open class CompanionClass {
    }
}
