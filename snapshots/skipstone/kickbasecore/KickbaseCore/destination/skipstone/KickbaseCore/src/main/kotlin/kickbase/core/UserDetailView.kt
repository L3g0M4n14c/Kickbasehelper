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

import skip.ui.*
import skip.foundation.*
import skip.model.*

internal class UserDetailView: View {
    internal val user: LeagueUser
    internal val selectedMatchDay: Int?
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    private var userPlayers: Array<Player>
        get() = _userPlayers.wrappedValue.sref({ this.userPlayers = it })
        set(newValue) {
            _userPlayers.wrappedValue = newValue.sref()
        }
    private var _userPlayers: skip.ui.State<Array<Player>>
    private var isLoading: Boolean
        get() = _isLoading.wrappedValue
        set(newValue) {
            _isLoading.wrappedValue = newValue
        }
    private var _isLoading: skip.ui.State<Boolean>

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            ScrollView { ->
                ComposeBuilder { composectx: ComposeContext ->
                    VStack(spacing = 20.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // User Header Section
                            UserHeaderSection(user = user).Compose(composectx)

                            // User Stats Section
                            UserStatsSection(user = user).Compose(composectx)

                            // User Squad Section
                            UserSquadSection(players = userPlayers, isLoading = isLoading).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding().Compose(composectx)
                    ComposeResult.ok
                }
            }
            .navigationTitle(user.name)
            .task { -> MainActor.run l@{
                var deferaction_0: (() -> Unit)? = null
                try {
                    val league_0 = kickbaseManager.selectedLeague
                    if (league_0 == null) {
                        print("⚠️ UserDetailView: No league selected")
                        return@l
                    }

                    print("🔍 UserDetailView: Loading squad for user ${user.id} in league ${league_0.id}")
                    isLoading = true
                    deferaction_0 = {
                        isLoading = false
                    }

                    // If a specific matchday is selected, reload the ranking for that matchday to get the lineup IDs
                    var currentUser = user
                    selectedMatchDay?.let { matchDay ->
                        print("🎯 UserDetailView: Reloading matchday ranking for day ${matchDay} to get lineup data")
                        kickbaseManager.loadMatchDayRanking(for_ = league_0, matchDay = matchDay)

                        // Get the updated user with lineup IDs from matchday ranking
                        val matchtarget_0 = kickbaseManager.matchDayUsers.first(where = { it -> it.id == user.id })
                        if (matchtarget_0 != null) {
                            val updatedUser = matchtarget_0
                            currentUser = updatedUser
                            print("✅ UserDetailView: Updated user with lineup IDs from matchday ${matchDay}: ${currentUser.lineupPlayerIds.count} players")
                        } else {
                            print("⚠️ UserDetailView: Could not find updated user in matchday ranking")
                        }
                    }

                    // Now load the players based on whether we have a specific matchday selected
                    // Important: Only use lineup IDs if we're in matchday mode
                    if (selectedMatchDay != null && !currentUser.lineupPlayerIds.isEmpty) {
                        print("🎯 UserDetailView: Matchday mode - Using lineup player IDs (${currentUser.lineupPlayerIds.count} players)")
                        val matchtarget_1 = kickbaseManager.loadPlayersForLineup(lineupPlayerIds = currentUser.lineupPlayerIds, leagueId = league_0.id, userId = currentUser.id)
                        if (matchtarget_1 != null) {
                            val players = matchtarget_1
                            print("✅ UserDetailView: Received ${players.count} players for matchday")
                            userPlayers = players
                        } else {
                            print("❌ UserDetailView: Failed to load players from matchday lineup IDs")
                        }
                    } else {
                        // Overall ranking or no lineup data: Load current full squad
                        if (selectedMatchDay != null) {
                            print("⚠️ UserDetailView: Matchday ${selectedMatchDay ?: -1} has no lineup data, loading current squad")
                        } else {
                            print("ℹ️ UserDetailView: Overall ranking - loading current full squad")
                        }
                        val matchtarget_2 = kickbaseManager.loadUserSquad(leagueId = league_0.id, userId = currentUser.id)
                        if (matchtarget_2 != null) {
                            val players = matchtarget_2
                            print("✅ UserDetailView: Received ${players.count} players from current squad")
                            userPlayers = players
                        } else {
                            print("❌ UserDetailView: No players received from loadUserSquad")
                        }
                    }
                } finally {
                    deferaction_0?.invoke()
                }
            } }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val remembereduserPlayers by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Array<Player>>, Any>) { mutableStateOf(_userPlayers) }
        _userPlayers = remembereduserPlayers

        val rememberedisLoading by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_isLoading) }
        _isLoading = rememberedisLoading

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!

        return super.Evaluate(context, options)
    }

    private constructor(user: LeagueUser, selectedMatchDay: Int? = null, userPlayers: Array<Player> = arrayOf(), isLoading: Boolean = false, privatep: Nothing? = null) {
        this.user = user
        this.selectedMatchDay = selectedMatchDay
        this._userPlayers = skip.ui.State(userPlayers.sref())
        this._isLoading = skip.ui.State(isLoading)
    }

    constructor(user: LeagueUser, selectedMatchDay: Int? = null): this(user = user, selectedMatchDay = selectedMatchDay, privatep = null) {
    }
}

// MARK: - User Header Section
internal class UserHeaderSection: View {
    internal val user: LeagueUser

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 12.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Team Name
                    Text(user.teamName)
                        .font(Font.title)
                        .fontWeight(Font.Weight.bold)
                        .multilineTextAlignment(TextAlignment.center).Compose(composectx)

                    // User Name
                    Text(user.name)
                        .font(Font.title3)
                        .foregroundColor(Color.secondary).Compose(composectx)

                    // Points Badge
                    VStack(spacing = 4.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text({
                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                str.appendInterpolation(user.points)
                                LocalizedStringKey(stringInterpolation = str)
                            }())
                                .font(Font.system(size = 48.0, weight = Font.Weight.bold))
                                .foregroundColor(Color.primary).Compose(composectx)
                            Text(LocalizedStringKey(stringLiteral = "Punkte"))
                                .font(Font.caption)
                                .foregroundColor(Color.secondary).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding()
                    .background(RoundedRectangle(cornerRadius = 12.0)
                        .fill(Color.blue.opacity(0.1))).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding()
            .frame(maxWidth = Double.infinity)
            .background(RoundedRectangle(cornerRadius = 16.0)
                .fill(Color.systemBackgroundCompat)
                .shadow(radius = 2.0)).Compose(composectx)
        }
    }

    constructor(user: LeagueUser) {
        this.user = user
    }
}

// MARK: - User Stats Section
internal class UserStatsSection: View {
    internal val user: LeagueUser

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 16.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(LocalizedStringKey(stringLiteral = "Statistiken"))
                        .font(Font.headline)
                        .frame(maxWidth = Double.infinity, alignment = Alignment.leading).Compose(composectx)

                    LazyVGrid(columns = arrayOf(
                        GridItem(GridItem.Size.flexible()),
                        GridItem(GridItem.Size.flexible())
                    ), spacing = 16.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            UserStatCard(title = "Platzierung", value = "${user.placement}.").Compose(composectx)
                            UserStatCard(title = "Teamwert", value = formatCurrency(user.teamValue)).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding()
            .background(RoundedRectangle(cornerRadius = 16.0)
                .fill(Color.systemBackgroundCompat)
                .shadow(radius = 2.0)).Compose(composectx)
        }
    }

    private fun formatCurrency(value: Int): String {
        val millions = Double(value) / 1_000_000.0
        return String(format = "%.1fM €", millions)
    }

    constructor(user: LeagueUser) {
        this.user = user
    }
}

// MARK: - User Stat Card
internal class UserStatCard: View {
    internal val title: String
    internal val value: String

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 8.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(value)
                        .font(Font.title2)
                        .fontWeight(Font.Weight.bold).Compose(composectx)
                    Text(title)
                        .font(Font.caption)
                        .foregroundColor(Color.secondary).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .frame(maxWidth = Double.infinity)
            .padding()
            .background(RoundedRectangle(cornerRadius = 12.0)
                .fill(Color.blue.opacity(0.1))).Compose(composectx)
        }
    }

    constructor(title: String, value: String) {
        this.title = title
        this.value = value
    }
}

// MARK: - User Squad Section
internal class UserSquadSection: View {
    internal val players: Array<Player>
    internal val isLoading: Boolean

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 16.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    HStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(LocalizedStringKey(stringLiteral = "Kader"))
                                .font(Font.headline).Compose(composectx)
                            Spacer().Compose(composectx)
                            if (!players.isEmpty) {
                                Text({
                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                    str.appendInterpolation(players.count)
                                    str.appendLiteral(" Spieler")
                                    LocalizedStringKey(stringInterpolation = str)
                                }())
                                    .font(Font.caption)
                                    .foregroundColor(Color.secondary).Compose(composectx)
                            }
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    if (isLoading) {
                        ProgressView(LocalizedStringKey(stringLiteral = "Lade Kader..."))
                            .frame(maxWidth = Double.infinity)
                            .padding().Compose(composectx)
                    } else if (players.isEmpty) {
                        VStack(spacing = 12.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                Image(systemName = "person.3.fill")
                                    .font(Font.system(size = 40.0))
                                    .foregroundColor(Color.gray).Compose(composectx)
                                Text(LocalizedStringKey(stringLiteral = "Keine Spieler verfügbar"))
                                    .font(Font.subheadline)
                                    .foregroundColor(Color.gray).Compose(composectx)
                                ComposeResult.ok
                            }
                        }
                        .frame(maxWidth = Double.infinity)
                        .padding().Compose(composectx)
                    } else {
                        LazyVStack(spacing = 12.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                ForEach(groupedPlayers.sorted(by = { it, it_1 -> it.key < it_1.key }), id = { it.key }) { (position, positionPlayers) ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        PositionGroupView(positionName = positionName(for_ = position), players = positionPlayers).Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)
                                ComposeResult.ok
                            }
                        }.Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }
            .padding()
            .background(RoundedRectangle(cornerRadius = 16.0)
                .fill(Color.systemBackgroundCompat)
                .shadow(radius = 2.0)).Compose(composectx)
        }
    }

    private val groupedPlayers: Dictionary<Int, Array<Player>>
        get() {
            var grouped: Dictionary<Int, Array<Player>> = dictionaryOf()
            for (player in players.sref()) {
                var list = (grouped[player.position] ?: arrayOf()).sref()
                list.append(player)
                grouped[player.position] = list.sref()
            }
            return grouped
        }

    private fun positionName(for_: Int): String {
        val position = for_
        when (position) {
            1 -> return "Torwart"
            2 -> return "Abwehr"
            3 -> return "Mittelfeld"
            4 -> return "Sturm"
            else -> return "Unbekannt"
        }
    }

    constructor(players: Array<Player>, isLoading: Boolean) {
        this.players = players.sref()
        this.isLoading = isLoading
    }
}

// MARK: - Position Group View
internal class PositionGroupView: View {
    internal val positionName: String
    internal val players: Array<Player>

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(positionName)
                        .font(Font.subheadline)
                        .fontWeight(Font.Weight.semibold)
                        .foregroundColor(Color.secondary).Compose(composectx)

                    ForEach(players) { player ->
                        ComposeBuilder { composectx: ComposeContext ->
                            UserSquadPlayerRow(player = player).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    constructor(positionName: String, players: Array<Player>) {
        this.positionName = positionName
        this.players = players.sref()
    }
}

// MARK: - User Squad Player Row
internal class UserSquadPlayerRow: View {
    internal val player: Player

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            HStack(spacing = 12.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Player Image
                    AsyncImage(url = player.imageUrl, content = { image ->
                        ComposeBuilder { composectx: ComposeContext ->
                            image
                                .resizable()
                                .aspectRatio(contentMode = ContentMode.fill).Compose(composectx)
                            ComposeResult.ok
                        }
                    }, placeholder = { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            ZStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Circle()
                                        .fill(userDetailPositionColor(player.position).opacity(0.3)).Compose(composectx)
                                    Image(systemName = "person.fill")
                                        .foregroundColor(userDetailPositionColor(player.position)).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    })
                    .frame(width = 50.0, height = 50.0)
                    .clipShape(Circle()).Compose(composectx)

                    // Player Info
                    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(player.fullName)
                                .font(Font.subheadline)
                                .fontWeight(Font.Weight.medium).Compose(composectx)

                            HStack(spacing = 8.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(player.teamName)
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary).Compose(composectx)

                                    Text(LocalizedStringKey(stringLiteral = "•"))
                                        .foregroundColor(Color.secondary).Compose(composectx)

                                    Text(player.positionName)
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    Spacer().Compose(composectx)

                    // Player Stats
                    VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text({
                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                str.appendInterpolation(player.totalPoints)
                                LocalizedStringKey(stringInterpolation = str)
                            }())
                                .font(Font.subheadline)
                                .fontWeight(Font.Weight.bold).Compose(composectx)
                            Text(LocalizedStringKey(stringLiteral = "Pkt."))
                                .font(Font.caption)
                                .foregroundColor(Color.secondary).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding(12.0)
            .background(RoundedRectangle(cornerRadius = 8.0)
                .fill(Color.secondarySystemBackgroundCompat)).Compose(composectx)
        }
    }

    constructor(player: Player) {
        this.player = player
    }
}

// MARK: - Helper Functions
internal fun userDetailPositionColor(position: Int): Color {
    when (position) {
        1 -> return Color.yellow
        2 -> return Color.green
        3 -> return Color.blue
        4 -> return Color.red
        else -> return Color.gray
    }
}
