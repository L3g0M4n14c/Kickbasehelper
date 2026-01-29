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

// MARK: - Protocols for testability
interface KickbaseAPIServiceProtocol {
    suspend fun getLeagueSelection(): Dictionary<String, Any>
    suspend fun getLeagueRanking(leagueId: String, matchDay: Int?): Dictionary<String, Any>

    // Player-related
    suspend fun getPlayerDetails(leagueId: String, playerId: String): Dictionary<String, Any>
    suspend fun getMySquad(leagueId: String): Dictionary<String, Any>
    suspend fun getMarketPlayers(leagueId: String): Dictionary<String, Any>
    suspend fun getPlayerPerformance(leagueId: String, playerId: String): PlayerPerformanceResponse
    suspend fun getPlayerMarketValue(leagueId: String, playerId: String, timeframe: Int): Dictionary<String, Any>
    suspend fun getTeamProfile(leagueId: String, teamId: String): TeamProfileResponse

    // User stats
    suspend fun getMyBudget(leagueId: String): Dictionary<String, Any>
    suspend fun getLeagueMe(leagueId: String): Dictionary<String, Any>
}

interface KickbaseDataParserProtocol {
    fun parseLeaguesFromResponse(json: Dictionary<String, Any>): Array<League>
    fun parseLeagueRanking(from: Dictionary<String, Any>, isMatchDayQuery: Boolean): Array<LeagueUser>

    // Market / Stats helpers
    fun parseMarketValueHistory(from: Dictionary<String, Any>): MarketValueChange?
    fun parseUserStatsFromResponse(json: Dictionary<String, Any>, fallbackUser: LeagueUser): UserStats

    // Small extract helpers used by PlayerService
    fun extractAveragePoints(from: Dictionary<String, Any>): Double
    fun extractTotalPoints(from: Dictionary<String, Any>): Int
}

@Stable
open class KickbaseLeagueService: ObservableObject {
    override val objectWillChange = ObservableObjectPublisher()
    private val apiService: KickbaseAPIServiceProtocol
    private val dataParser: KickbaseDataParserProtocol

    constructor(apiService: KickbaseAPIServiceProtocol, dataParser: KickbaseDataParserProtocol) {
        this.apiService = apiService
        this.dataParser = dataParser
    }

    // MARK: - League Loading

    open suspend fun loadLeagues(): Array<League> = MainActor.run l@{
        print("🏆 Loading leagues...")

        try {
            val json = MainActor.run { apiService }.getLeagueSelection()
            val leagues = dataParser.parseLeaguesFromResponse(json)

            if (leagues.isEmpty) {
                print("⚠️ No leagues found in response")
            }
            return@l leagues.sref()
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Failed to load leagues: ${error.localizedDescription}")
            throw error as Throwable
        }
    }

    // MARK: - League Ranking

    open suspend fun loadLeagueRanking(for_: League): Array<LeagueUser> = Async.run l@{
        val league = for_
        print("🏆 Loading league ranking for: ${league.name}")

        try {
            val json = MainActor.run { apiService }.getLeagueRanking(leagueId = league.id, matchDay = null)
            val users = dataParser.parseLeagueRanking(from = json, isMatchDayQuery = false)

            // Sort by points descending
            val sortedUsers = users.sorted { it, it_1 -> it.points > it_1.points }

            if (sortedUsers.isEmpty) {
                print("⚠️ No users found in ranking")
            } else {
                print("✅ Loaded ${sortedUsers.count} users in ranking")
            }
            return@l sortedUsers.sref()
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Failed to load league ranking: ${error.localizedDescription}")
            throw error as Throwable
        }
    }

    open suspend fun loadMatchDayRanking(for_: League, matchDay: Int): Array<LeagueUser> = Async.run l@{
        val league = for_
        print("🏆 Loading matchday ${matchDay} ranking for: ${league.name}")

        try {
            val json = MainActor.run { apiService }.getLeagueRanking(leagueId = league.id, matchDay = matchDay)
            val users = dataParser.parseLeagueRanking(from = json, isMatchDayQuery = true)

            // Sort by points descending
            val sortedUsers = users.sorted { it, it_1 -> it.points > it_1.points }

            if (sortedUsers.isEmpty) {
                print("⚠️ No users found in matchday ranking")
            } else {
                print("✅ Loaded ${sortedUsers.count} users in matchday ranking")
            }
            return@l sortedUsers.sref()
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Failed to load matchday ranking: ${error.localizedDescription}")
            throw error as Throwable
        }
    }


    @androidx.annotation.Keep
    companion object: CompanionClass() {
    }
    open class CompanionClass {
    }
}
