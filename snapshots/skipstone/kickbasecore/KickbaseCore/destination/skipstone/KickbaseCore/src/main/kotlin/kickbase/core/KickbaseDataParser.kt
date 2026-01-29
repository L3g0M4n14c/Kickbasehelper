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

@Stable
open class KickbaseDataParser: ObservableObject, KickbaseDataParserProtocol {
    override val objectWillChange = ObservableObjectPublisher()

    // MARK: - Int/String Extraction Helpers

    open fun extractInt(from: Dictionary<String, Any>, keys: Array<String>): Int? {
        val data = from
        for (key in keys.sref()) {
            val matchtarget_0 = data[key] as? Int
            if (matchtarget_0 != null) {
                val value = matchtarget_0
                return value
            } else {
                val matchtarget_1 = data[key] as? Double
                if (matchtarget_1 != null) {
                    val value = matchtarget_1
                    return Int(value)
                } else {
                    (data[key] as? String)?.let { value ->
                        Int(value)?.let { intValue ->
                            return intValue
                        }
                    }
                }
            }
        }
        return null
    }

    open fun extractString(from: Dictionary<String, Any>, keys: Array<String>): String? {
        val data = from
        for (key in keys.sref()) {
            (data[key] as? String)?.let { value ->
                return value
            }
        }
        return null
    }

    open fun extractDouble(from: Dictionary<String, Any>, keys: Array<String>): Double? {
        val data = from
        for (key in keys.sref()) {
            val matchtarget_2 = data[key] as? Double
            if (matchtarget_2 != null) {
                val value = matchtarget_2
                return value
            } else {
                val matchtarget_3 = data[key] as? Int
                if (matchtarget_3 != null) {
                    val value = matchtarget_3
                    return Double(value)
                } else {
                    (data[key] as? String)?.let { value ->
                        Double(value)?.let { doubleValue ->
                            return doubleValue
                        }
                    }
                }
            }
        }
        return null
    }

    // MARK: - Punktzahl-Extraktions-Helper-Funktionen

    override fun extractTotalPoints(from: Dictionary<String, Any>): Int {
        val playerData = from
        val possibleKeys = arrayOf(
            "p",
            "totalPoints",
            "tp",
            "points",
            "pts",
            "totalPts",
            "gesamtpunkte",
            "total",
            "score",
            "seasonPoints",
            "sp"
        )

        for (key in possibleKeys.sref()) {
            val matchtarget_4 = playerData[key] as? Int
            if (matchtarget_4 != null) {
                val value = matchtarget_4
                print("   ✅ Found totalPoints in field '${key}': ${value}")
                return value
            } else {
                val matchtarget_5 = playerData[key] as? Double
                if (matchtarget_5 != null) {
                    val value = matchtarget_5
                    print("   ✅ Found totalPoints in field '${key}': ${Int(value)}")
                    return Int(value)
                } else {
                    (playerData[key] as? String)?.let { value ->
                        Int(value)?.let { intValue ->
                            print("   ✅ Found totalPoints in field '${key}': ${intValue}")
                            return intValue
                        }
                    }
                }
            }
        }

        print("   ⚠️ No totalPoints found in any field")
        return 0 // Fallback wenn keine Punktzahl gefunden wird
    }

    override fun extractAveragePoints(from: Dictionary<String, Any>): Double {
        val playerData = from
        val possibleKeys = arrayOf(
            "averagePoints",
            "ap",
            "avgPoints",
            "durchschnitt",
            "avg",
            "averageScore",
            "avgp",
            "avp"
        )

        for (key in possibleKeys.sref()) {
            val matchtarget_6 = playerData[key] as? Double
            if (matchtarget_6 != null) {
                val value = matchtarget_6
                print("   ✅ Found averagePoints in field '${key}': ${value}")
                return value
            } else {
                val matchtarget_7 = playerData[key] as? Int
                if (matchtarget_7 != null) {
                    val value = matchtarget_7
                    print("   ✅ Found averagePoints in field '${key}': ${Double(value)}")
                    return Double(value)
                } else {
                    (playerData[key] as? String)?.let { value ->
                        Double(value)?.let { doubleValue ->
                            print("   ✅ Found averagePoints in field '${key}': ${doubleValue}")
                            return doubleValue
                        }
                    }
                }
            }
        }

        print("   ⚠️ No averagePoints found in any field")
        return 0.0 // Fallback wenn keine Durchschnittspunktzahl gefunden wird
    }

    // MARK: - League Parsing

    override fun parseLeaguesFromResponse(json: Dictionary<String, Any>): Array<League> {
        print("🔍 Parsing leagues response...")
        print("📋 Raw JSON keys: ${Array(json.keys)}")

        var leaguesArray: Array<Dictionary<String, Any>> = arrayOf()

        // Versuche verschiedene mögliche Response-Formate (sichere, nicht-generic Runtime casts)
        for (key in arrayOf("leagues", "data", "l", "it", "anol")) {
            val arrRawAny = rawArray(from = json[key])
            val arr = arrRawAny.compactMap { it -> dict(from = it) }
            if (!arr.isEmpty) {
                leaguesArray = arr.sref()
                print("✅ Found ${key} array with ${arr.count} entries")
                break
            }
        }

        if (leaguesArray.isEmpty && json.keys.contains("id")) {
            // Single league response
            leaguesArray = arrayOf(json)
            print("✅ Found single league response")
        } else {
            // Erweiterte Behandlung für "it" und "anol" Keys
            leaguesArray = findLeaguesInComplexStructure(json).compactMap { it -> dict(from = it) }
        }

        var parsedLeagues: Array<League> = arrayOf()

        for ((index, leagueData) in leaguesArray.enumerated()) {
            print("🔄 Parsing league ${index + 1}: ${Array(leagueData.keys)}")

            val currentUser = parseLeagueUser(from = leagueData)

            val league = League(id = leagueData["id"] as? String ?: leagueData["i"] as? String ?: UUID().uuidString, name = leagueData["name"] as? String ?: leagueData["n"] as? String ?: "Liga ${index + 1}", creatorName = leagueData["creatorName"] as? String ?: leagueData["cn"] as? String ?: "", adminName = leagueData["adminName"] as? String ?: leagueData["an"] as? String ?: "", created = leagueData["created"] as? String ?: leagueData["c"] as? String ?: "", season = leagueData["season"] as? String ?: leagueData["s"] as? String ?: "2024/25", matchDay = leagueData["matchDay"] as? Int ?: leagueData["md"] as? Int ?: 1, currentUser = currentUser)

            parsedLeagues.append(league)
            print("✅ Parsed league: ${league.name}")
        }

        print("🏆 Successfully parsed ${parsedLeagues.count} leagues")
        return parsedLeagues.sref()
    }

    private fun findLeaguesInComplexStructure(json: Dictionary<String, Any>): Array<*> {
        print("🔍 Checking alternative formats for it/anol keys...")

        // Prüfe "it" key
        json["it"].sref()?.let { it ->
            print("🔍 Found 'it' key with type: ${type(of = it)}")

            val matchtarget_8 = dict(from = it)
            if (matchtarget_8 != null) {
                val itDict = matchtarget_8
                print("✅ 'it' is a dictionary with keys: ${Array(itDict.keys)}")
                for ((key, value) in itDict.sref()) {
                    val arrRawAny = rawArray(from = value)
                    if (!arrRawAny.isEmpty) {
                        print("✅ Found leagues in it[${key}] with ${arrRawAny.count} entries")
                        return arrRawAny.sref()
                    }
                }
                return (arrayOf(itDict) as Array<*>).sref()
            } else {
                val arrRawAny = rawArray(from = it)
                if (!arrRawAny.isEmpty) {
                    print("✅ Found 'it' as direct array with ${arrRawAny.count} entries")
                    return arrRawAny.sref()
                }
            }
        }

        // Prüfe "anol" key
        json["anol"].sref()?.let { anol ->
            print("🔍 Found 'anol' key with type: ${type(of = anol)}")

            val matchtarget_9 = dict(from = anol)
            if (matchtarget_9 != null) {
                val anolDict = matchtarget_9
                print("✅ 'anol' is a dictionary with keys: ${Array(anolDict.keys)}")
                for ((key, value) in anolDict.sref()) {
                    val arrRawAny = rawArray(from = value)
                    if (!arrRawAny.isEmpty) {
                        print("✅ Found leagues in anol[${key}] with ${arrRawAny.count} entries")
                        return arrRawAny.sref()
                    }
                }
                return (arrayOf(anolDict) as Array<*>).sref()
            } else {
                val arrRawAny = rawArray(from = anol)
                if (!arrRawAny.isEmpty) {
                    print("✅ Found 'anol' as direct array with ${arrRawAny.count} entries")
                    return arrRawAny.sref()
                }
            }
        }

        // Suche alle Keys nach Arrays ab
        print("🔍 Searching all keys for array data...")
        for ((key, value) in json.sref()) {
            val arrRawAny = rawArray(from = value)
            if (!arrRawAny.isEmpty) {
                print("✅ Found leagues in [${key}] with ${arrRawAny.count} entries")
                return arrRawAny.sref()
            } else {
                dict(from = value)?.let { dict ->
                    if (!dict.isEmpty) {
                        if (key != "it" && key != "anol") {
                            return (arrayOf(dict) as Array<*>).sref()
                        }
                    }
                }
            }
        }

        // Falls Liga-ähnliche Daten direkt im JSON
        if (json.keys.contains("id") || json.keys.contains("name") || json.keys.contains("i") || json.keys.contains("n")) {
            print("✅ Using entire response as single league")
            return (arrayOf(json) as Array<*>).sref()
        }

        print("❌ Unknown response format. Keys: ${Array(json.keys)}")
        return (arrayOf() as Array<*>).sref()
    }

    open fun parseLeagueUser(from: Dictionary<String, Any>): LeagueUser {
        val leagueData = from
        var currentUser = LeagueUser(id = "unknown", name = "Unknown", teamName = "Unknown Team", budget = 5_000_000, teamValue = 50_000_000, points = 0, placement = 1, won = 0, drawn = 0, lost = 0, se11 = 0, ttm = 0, mpst = 3)

        val _userData = (dict(from = leagueData["currentUser"]) ?: dict(from = leagueData["cu"]) ?: dict(from = leagueData["user"]) ?: dict(from = leagueData["it"]) ?: dict(from = leagueData["anol"])).sref()
        if (_userData != null) {
            val userData = _userData.sref()

            print("👤 Available user keys: ${userData.keys.sorted()}")

            // Prüfe verschiedene mögliche Feldnamen für teamName
            val possibleTeamNames = arrayOf(
                userData["teamName"] as? String,
                userData["tn"] as? String,
                userData["team_name"] as? String,
                userData["tname"] as? String,
                userData["club"] as? String,
                userData["clubName"] as? String,
                userData["teamname"] as? String
            ).compactMap { it -> it }

            val teamName = possibleTeamNames.first ?: "Team"
            print("🏆 Found team name: '${teamName}' from keys: ${possibleTeamNames}")

            currentUser = LeagueUser(id = userData["id"] as? String ?: userData["i"] as? String ?: "unknown", name = userData["name"] as? String ?: userData["n"] as? String ?: "User", teamName = teamName, budget = userData["budget"] as? Int ?: userData["b"] as? Int ?: 5_000_000, teamValue = userData["teamValue"] as? Int ?: userData["tv"] as? Int ?: 50_000_000, points = userData["points"] as? Int ?: userData["p"] as? Int ?: 0, placement = userData["placement"] as? Int ?: userData["pl"] as? Int ?: 1, won = userData["won"] as? Int ?: userData["w"] as? Int ?: 0, drawn = userData["drawn"] as? Int ?: userData["d"] as? Int ?: 0, lost = userData["lost"] as? Int ?: userData["l"] as? Int ?: 0, se11 = userData["se11"] as? Int ?: userData["s"] as? Int ?: 0, ttm = userData["ttm"] as? Int ?: userData["t"] as? Int ?: 0, mpst = userData["mpst"] as? Int ?: userData["maxPlayersPerTeam"] as? Int ?: 3)
            print("✅ Parsed user: ${currentUser.name} - ${currentUser.teamName}")
        } else {
            print("❌ No user data found in league data")
        }

        return currentUser
    }

    // MARK: - League Ranking Parsing

    override fun parseLeagueRanking(from: Dictionary<String, Any>, isMatchDayQuery: Boolean): Array<LeagueUser> {
        val json = from
        print("🏆 Parsing league ranking... (isMatchDayQuery: ${isMatchDayQuery})")

        // The ranking uses "us" array according to API documentation
        val usersArray = arrayOfDicts(from = json["us"])
        if (usersArray.isEmpty) {
            print("⚠️ No users array found in ranking response")
            print("📋 Available keys: ${json.keys.sorted()}")
            return arrayOf()
        }

        val users = usersArray.compactMap(fun(userData: *): LeagueUser? {
            print("👤 User data keys: ${userData.keys.sorted()}")
            print("📋 Full user data: ${userData}")
            val id = extractString(from = userData, keys = arrayOf("i", "id")) ?: "unknown"
            val name = extractString(from = userData, keys = arrayOf("n", "name")) ?: "User"
            // Note: ranking API doesn't include teamName, so we'll use an empty default
            val teamName = extractString(from = userData, keys = arrayOf("tn", "teamName")) ?: ""

            val budget = extractInt(from = userData, keys = arrayOf("b", "budget")) ?: 0
            val teamValue = extractInt(from = userData, keys = arrayOf("tv", "teamValue")) ?: 0

            // Choose the correct fields based on query type
            val points: Int
            val placement: Int

            if (isMatchDayQuery) {
                // For matchday queries, prioritize 'mdp' (matchday points) and 'mdpl' (matchday placement)
                points = extractInt(from = userData, keys = arrayOf("mdp", "p", "points")) ?: 0
                placement = extractInt(from = userData, keys = arrayOf("mdpl", "pl", "placement")) ?: 0
            } else {
                // For overall queries, use 'sp' (season points) and 'spl' (season placement)
                points = extractInt(from = userData, keys = arrayOf("sp", "p", "points")) ?: 0
                placement = extractInt(from = userData, keys = arrayOf("spl", "pl", "placement")) ?: 0
            }

            // These fields don't exist in ranking API, set to 0 as defaults
            val won = 0
            val drawn = 0
            val lost = 0

            // se11, ttm may not be in ranking response either
            val se11 = extractInt(from = userData, keys = arrayOf("se11", "s")) ?: 0
            val ttm = extractInt(from = userData, keys = arrayOf("ttm", "t")) ?: 0
            val mpst = extractInt(from = userData, keys = arrayOf("mpst", "maxPlayersPerTeam"))

            // Extract lineup player IDs ("lp" field)
            // The API might return integers or strings, so handle both
            var lineupPlayerIds: Array<String> = arrayOf()

            val matchtarget_10 = userData["lp"] as? Array<*>
            if (matchtarget_10 != null) {
                val lpAnyArray = matchtarget_10
                for (el in lpAnyArray.sref()) {
                    val matchtarget_11 = el as? String
                    if (matchtarget_11 != null) {
                        val s = matchtarget_11
                        lineupPlayerIds.append(s)
                    } else {
                        val matchtarget_12 = el as? java.lang.Number
                        if (matchtarget_12 != null) {
                            val n = matchtarget_12
                            lineupPlayerIds.append(n.stringValue)
                        } else {
                            (el as? Int)?.let { i ->
                                lineupPlayerIds.append(String(i))
                            }
                        }
                    }
                }
                if (!lineupPlayerIds.isEmpty) {
                    print("✅ Parsed lp array with ${lineupPlayerIds.count} entries: ${lineupPlayerIds}")
                } else {
                    print("⚠️ 'lp' field present but contained no parsable elements. Raw value: ${userData["lp"] ?: "nil"}")
                }
            } else {
                print("⚠️ No 'lp' field found or wrong format. Raw value: ${userData["lp"] ?: "nil"}")
            }

            print("👤 User ${name} has ${lineupPlayerIds.count} players in lineup: ${lineupPlayerIds}")

            return LeagueUser(id = id, name = name, teamName = teamName, budget = budget, teamValue = teamValue, points = points, placement = placement, won = won, drawn = drawn, lost = lost, se11 = se11, ttm = ttm, mpst = mpst, lineupPlayerIds = lineupPlayerIds)
        })

        print("✅ Parsed ${users.count} users from ranking")
        return users.sref()
    }

    // MARK: - User Stats Parsing

    override fun parseUserStatsFromResponse(json: Dictionary<String, Any>, fallbackUser: LeagueUser): UserStats {
        print("🔍 Parsing user stats from response...")
        print("📋 Stats JSON keys: ${Array(json.keys)}")

        var statsData: Dictionary<String, Any> = json.sref()

        // Prüfe auf verschachtelte Strukturen
        val matchtarget_13 = dict(from = json["user"])
        if (matchtarget_13 != null) {
            val user = matchtarget_13
            print("✅ Found 'user' object")
            statsData = user.sref()
        } else {
            val matchtarget_14 = dict(from = json["me"])
            if (matchtarget_14 != null) {
                val me = matchtarget_14
                print("✅ Found 'me' object")
                statsData = me.sref()
            } else {
                val matchtarget_15 = dict(from = json["data"])
                if (matchtarget_15 != null) {
                    val data = matchtarget_15
                    print("✅ Found 'data' object")
                    statsData = data.sref()
                } else {
                    val matchtarget_16 = dict(from = json["team"])
                    if (matchtarget_16 != null) {
                        val team = matchtarget_16
                        print("✅ Found 'team' object")
                        statsData = team.sref()
                    } else {
                        dict(from = json["league"])?.let { league ->
                            print("✅ Found 'league' object")
                            statsData = league.sref()
                        }
                    }
                }
            }
        }

        val teamValue = extractInt(from = statsData, keys = arrayOf("teamValue", "tv", "marketValue", "mv", "value")) ?: fallbackUser.teamValue
        val teamValueTrend = extractInt(from = statsData, keys = arrayOf("teamValueTrend", "tvt", "marketValueTrend", "mvt", "trend", "t")) ?: 0
        val budget = extractInt(from = statsData, keys = arrayOf("b", "budget", "money", "cash", "funds")) ?: fallbackUser.budget
        val points = extractInt(from = statsData, keys = arrayOf("points", "p", "totalPoints", "tp")) ?: fallbackUser.points
        val placement = extractInt(from = statsData, keys = arrayOf("placement", "pl", "rank", "position", "pos")) ?: fallbackUser.placement
        val won = extractInt(from = statsData, keys = arrayOf("won", "w", "wins", "victories")) ?: fallbackUser.won
        val drawn = extractInt(from = statsData, keys = arrayOf("drawn", "d", "draws", "ties")) ?: fallbackUser.drawn
        val lost = extractInt(from = statsData, keys = arrayOf("lost", "l", "losses", "defeats")) ?: fallbackUser.lost

        // Debug: Zeige Budget-relevante Felder
        print("🔍 Budget-related fields found:")
        statsData["b"].sref()?.let { b ->
            print("   b (Budget): ${b}")
        }
        statsData["pbas"].sref()?.let { pbas ->
            print("   pbas (Previous Budget At Start): ${pbas}")
        }
        statsData["bs"].sref()?.let { bs ->
            print("   bs (Budget Start/Spent): ${bs}")
        }

        val userStats = UserStats(teamValue = teamValue, teamValueTrend = teamValueTrend, budget = budget, points = points, placement = placement, won = won, drawn = drawn, lost = lost)

        print("✅ User stats parsed successfully:")
        print("   💰 Budget: €${budget / 1000}k")
        print("   📈 Teamwert: €${teamValue / 1000}k")
        print("   🔄 Trend: €${teamValueTrend / 1000}k")
        print("   🏆 Punkte: ${points} (Platz ${placement})")

        return userStats
    }

    // MARK: - Market Value History Parsing

    override fun parseMarketValueHistory(from: Dictionary<String, Any>): MarketValueChange? {
        val json = from
        print("🔍 Parsing market value history from response...")
        print("📋 History JSON keys: ${Array(json.keys)}")

        // Extrahiere das prlo-Feld auf der gleichen Ebene wie "it"
        val prloValue = json["prlo"] as? Int
        print("📊 Found PRLO value at root level: ${prloValue ?: 0}")

        // Extrahiere die "it" Liste mit den Marktwert-Einträgen
        val itArray = arrayOfDicts(from = json["it"])
        if (itArray.isEmpty) {
            print("❌ No 'it' array found in market value history response")
            return null
        }

        print("📊 Found ${itArray.count} market value entries")

        // Konvertiere zu MarketValueEntry Objekten
        var entries: Array<MarketValueEntry> = arrayOf()
        for (entryData in itArray.sref()) {
            (entryData["dt"] as? Int)?.let { dt ->
                (entryData["mv"] as? Int)?.let { mv ->
                    entries.append(MarketValueEntry(dt = dt, mv = mv))
                    print("   📈 Entry dt:${dt} mv:€${mv / 1000}k")
                }
            }
        }

        // Sortiere nach dt (Datum) absteigend
        entries.sort { it, it_1 -> it.dt > it_1.dt }

        // Berechne die Änderung seit dem letzten Tag
        val currentEntry = entries.first
        val previousEntry = entries.dropFirst().first

        val absoluteChange = (currentEntry?.mv ?: 0) - (previousEntry?.mv ?: 0)
        val percentageChange = if (previousEntry?.mv != 0) (Double(absoluteChange) / Double(previousEntry!!.mv)) * 100.0 else 0.0

        val daysDifference = (currentEntry?.dt ?: 0) - (previousEntry?.dt ?: 0)

        // Berechne tägliche Änderungen für die letzten drei Tage
        var dailyChanges: Array<DailyMarketValueChange> = arrayOf()
        val dateFormatter = DateFormatter()
        dateFormatter.dateStyle = DateFormatter.Style.medium
        dateFormatter.timeStyle = DateFormatter.Style.none
        dateFormatter.locale = Locale(identifier = "de_DE")

        val maxDays = min(3, entries.count - 1)
        for (i in 0 until maxDays) {
            val currentDayEntry = entries[i]
            val previousDayEntry = entries[i + 1]

            val dailyChange = currentDayEntry.mv - previousDayEntry.mv
            val dailyPercentageChange = if (previousDayEntry.mv != 0) (Double(dailyChange) / Double(previousDayEntry.mv)) * 100.0 else 0.0

            val date = Date(timeIntervalSince1970 = TimeInterval(currentDayEntry.dt * 24 * 60 * 60))
            val dateString = dateFormatter.string(from = date)

            val dailyMarketValueChange = DailyMarketValueChange(date = dateString, value = currentDayEntry.mv, change = dailyChange, percentageChange = dailyPercentageChange, daysAgo = i)

            dailyChanges.append(dailyMarketValueChange)
        }

        val marketValueChange = MarketValueChange(daysSinceLastUpdate = daysDifference, absoluteChange = absoluteChange, percentageChange = percentageChange, previousValue = previousEntry?.mv ?: 0, currentValue = currentEntry?.mv ?: 0, dailyChanges = dailyChanges)

        print("✅ Calculated market value change:")
        print("   📈 Absolute change: €${absoluteChange / 1000}k")
        print("   📊 Percentage change: ${String(format = "%.1f", percentageChange)}%")

        return marketValueChange
    }

    @androidx.annotation.Keep
    companion object: CompanionClass() {
    }
    open class CompanionClass {
    }
}
