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

import skip.model.*
import skip.foundation.*
import skip.ui.*

@Stable
open class KickbaseUserStatsService: ObservableObject {
    override val objectWillChange = ObservableObjectPublisher()
    private val apiService: KickbaseAPIServiceProtocol
    private val dataParser: KickbaseDataParserProtocol

    constructor(apiService: KickbaseAPIServiceProtocol, dataParser: KickbaseDataParserProtocol) {
        this.apiService = apiService
        this.dataParser = dataParser
    }

    // MARK: - User Stats Loading

    open suspend fun loadUserStats(for_: League): UserStats = Async.run l@{
        val league = for_
        print("📊 Loading user stats for league: ${league.name}")

        try {
            // Versuche zuerst den Budget-Endpoint
            val json = MainActor.run { apiService }.getMyBudget(leagueId = league.id)
            return@l dataParser.parseUserStatsFromResponse(json, fallbackUser = league.currentUser)
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            // Fallback auf Me-Endpoint
            try {
                val json = MainActor.run { apiService }.getLeagueMe(leagueId = league.id)
                return@l dataParser.parseUserStatsFromResponse(json, fallbackUser = league.currentUser)
            } catch (error: Throwable) {
                @Suppress("NAME_SHADOWING") val error = error.aserror()
                print("⚠️ All user stats endpoints failed, using fallback data")
                return@l createFallbackUserStats(from = league.currentUser)
            }
        }
    }

    // MARK: - Fallback Data

    private fun createFallbackUserStats(from: LeagueUser): UserStats {
        val leagueUser = from
        print("📊 Using league user data as fallback for user stats")

        val userStats = UserStats(teamValue = leagueUser.teamValue, teamValueTrend = 0, budget = leagueUser.budget, points = leagueUser.points, placement = leagueUser.placement, won = leagueUser.won, drawn = leagueUser.drawn, lost = leagueUser.lost)

        print("📊 Fallback stats applied - Budget: €${leagueUser.budget / 1000}k, Teamwert: €${leagueUser.teamValue / 1000}k")
        return userStats
    }

    @androidx.annotation.Keep
    companion object: CompanionClass() {
    }
    open class CompanionClass {
    }
}
