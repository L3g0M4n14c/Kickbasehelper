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
import skip.lib.Set

import skip.ui.*
import skip.foundation.*
import skip.model.*

internal class StandardNavigationModifier: skip.ui.ViewModifier {
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    internal var authManager: AuthenticationManager
        get() = _authManager.wrappedValue
        set(newValue) {
            _authManager.wrappedValue = newValue
        }
    internal var _authManager = skip.ui.Environment<AuthenticationManager>()

    override fun body(content: View): View {
        return ComposeBuilder { composectx: ComposeContext ->
            content
                .navigationTitle(kickbaseManager.selectedLeague?.name ?: "Kickbase Helper")
                .toolbar { ->
                    ComposeBuilder { composectx: ComposeContext ->
                        ToolbarItem(placement = ToolbarItemPlacement.navigationBarTrailingCompat) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                Button(LocalizedStringKey(stringLiteral = "Logout")) { -> authManager.logout() }.Compose(composectx)
                                ComposeResult.ok
                            }
                        }.Compose(composectx)

                        ToolbarItem(placement = ToolbarItemPlacement.navigationBarLeadingCompat) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                if (kickbaseManager.isLoading) {
                                    ProgressView()
                                        .scaleEffect(0.8).Compose(composectx)
                                }
                                ComposeResult.ok
                            }
                        }.Compose(composectx)
                        ComposeResult.ok
                    }
                }.Compose(composectx)
        }
    }

    @Composable
    override fun Evaluate(content: View, context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!
        _authManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = AuthenticationManager::class)!!

        return super.Evaluate(content, context, options)
    }
}

internal class TeamPlayerCounts {
    internal val total: Int
    internal val goalkeepers: Int
    internal val defenders: Int
    internal val midfielders: Int
    internal val forwards: Int

    constructor(total: Int, goalkeepers: Int, defenders: Int, midfielders: Int, forwards: Int) {
        this.total = total
        this.goalkeepers = goalkeepers
        this.defenders = defenders
        this.midfielders = midfielders
        this.forwards = forwards
    }
}

internal class MainDashboardView: View {
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    internal var authManager: AuthenticationManager
        get() = _authManager.wrappedValue
        set(newValue) {
            _authManager.wrappedValue = newValue
        }
    internal var _authManager = skip.ui.Environment<AuthenticationManager>()
    internal var ligainsiderService: LigainsiderService
        get() = _ligainsiderService.wrappedValue
        set(newValue) {
            _ligainsiderService.wrappedValue = newValue
        }
    internal var _ligainsiderService = skip.ui.Environment<LigainsiderService>()
    private var selectedTab: Int
        get() = _selectedTab.wrappedValue
        set(newValue) {
            _selectedTab.wrappedValue = newValue
        }
    private var _selectedTab: skip.ui.State<Int>
    internal var horizontalSizeClass: UserInterfaceSizeClass? = null

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            ZStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    if (!ligainsiderService.isLigainsiderReady) {
                        // Loading State - warte bis Ligainsider Cache fertig ist
                        VStack(spacing = 20.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                ProgressView()
                                    .scaleEffect(1.5).Compose(composectx)
                                Text(LocalizedStringKey(stringLiteral = "Ligainsider-Daten werden geladen..."))
                                    .foregroundColor(Color.secondary)
                                    .font(Font.headline).Compose(composectx)
                                ComposeResult.ok
                            }
                        }
                        .frame(maxWidth = Double.infinity, maxHeight = Double.infinity).background(Color(UIColor.windowBackgroundColor)).Compose(composectx)
                    } else {
                        // Normale Layouts wenn Ligainsider fertig ist
                        Group { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                if (horizontalSizeClass == UserInterfaceSizeClass.regular) {
                                    // iPad Layout - Sidebar Navigation
                                    iPadLayout.Compose(composectx)
                                } else {
                                    // iPhone Layout - Tab Navigation
                                    iPhoneLayout.Compose(composectx)
                                }
                                ComposeResult.ok
                            }
                        }
                        .environmentObject(kickbaseManager)
                        .macOSOptimized().Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }
            .onAppear { ->
                // Automatisches Laden aller Daten beim ersten Start
                Task(isMainActor = true) { ->
                    // 1. Lade ZUERST Ligainsider Daten (mit await für vollständigen Cache)
                    print("🔄 MainDashboard: Starting Ligainsider data load...")
                    ligainsiderService.fetchLineupsAsync()
                    print("✅ MainDashboard: Lineups loaded with cache size: ${ligainsiderService.playerCacheCount}")
                    // NOTE: fetchAllSquadsAsync() is disabled because:
                    // - Squad data is already loaded from fetchLineupsAsync() (match.homeSquad + match.awaySquad)
                    // - fetchAllSquadsAsync() was finding 0 players anyway
                    // await ligainsiderService.fetchAllSquadsAsync()
                    print("✅ MainDashboard: Ligainsider cache is complete with ${ligainsiderService.playerCacheCount} players")

                    // 2. Lade danach die Kickbase-Daten (Team + Market Players)
                    print("🔄 MainDashboard: Now loading Kickbase data...")
                    kickbaseManager.loadUserData()

                    // 3. Zusätzlich: Lade Team-Daten wenn Liga verfügbar
                    kickbaseManager.selectedLeague?.let { league ->
                        kickbaseManager.loadTeamPlayers(for_ = league)
                        kickbaseManager.loadMarketPlayers(for_ = league)
                    }
                    print("✅ MainDashboard: All data loaded completely")
                }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedselectedTab by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Int>, Any>) { mutableStateOf(_selectedTab) }
        _selectedTab = rememberedselectedTab

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!
        _authManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = AuthenticationManager::class)!!
        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!
        this.horizontalSizeClass = EnvironmentValues.shared.horizontalSizeClass

        return super.Evaluate(context, options)
    }

    // iPad-spezifisches Layout mit Sidebar
    private val iPadLayout: View
        get() = Text(LocalizedStringKey(stringLiteral = "iPad Layout not supported on Android"))

    // iPhone-spezifisches Layout mit Tabs
    private val iPhoneLayout: View
        get() {
            return TabView(selection = Binding({ _selectedTab.wrappedValue }, { it -> _selectedTab.wrappedValue = it })) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Team Tab mit Punktzahlen
                    NavigationStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            TeamView()
                                .modifier(StandardNavigationModifier())
                                .accessibilityIdentifier("tab_team").Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .tabItem { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Image(systemName = "person.3.fill").Compose(composectx)
                            Text(LocalizedStringKey(stringLiteral = "Team")).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .tag(0).Compose(composectx)

                    // Market Tab
                    NavigationStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            MarketView()
                                .modifier(StandardNavigationModifier()).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .tabItem { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Image(systemName = "cart.fill").Compose(composectx)
                            Text(LocalizedStringKey(stringLiteral = "Markt")).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .tag(1).Compose(composectx)

                    // Sales Recommendation Tab (ersetzt Stats Tab)
                    NavigationStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            SalesRecommendationView()
                                .modifier(StandardNavigationModifier()).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .tabItem { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Image(systemName = "dollarsign.circle.fill").Compose(composectx)
                            Text(LocalizedStringKey(stringLiteral = "Verkaufen")).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .tag(2).Compose(composectx)

                    // Lineup Optimizer Tab
                    NavigationStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            LineupOptimizerView()
                                .modifier(StandardNavigationModifier()).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .tabItem { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Image(systemName = "person.crop.square.fill.and.at.rectangle").Compose(composectx)
                            Text(LocalizedStringKey(stringLiteral = "Aufstellung")).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .tag(3).Compose(composectx)

                    // Transfer Recommendations Tab
                    NavigationStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            TransferRecommendationsView(kickbaseManager = kickbaseManager)
                                .modifier(StandardNavigationModifier()).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .tabItem { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Image(systemName = "person.crop.circle.badge.plus").Compose(composectx)
                            Text(LocalizedStringKey(stringLiteral = "Transfer-Tipps")).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .tag(4).Compose(composectx)

                    // Ligainsider Tab
                    NavigationStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            LigainsiderView()
                                .modifier(StandardNavigationModifier()).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .tabItem { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Image(systemName = "list.bullet.clipboard").Compose(composectx)
                            Text(LocalizedStringKey(stringLiteral = "Ligainsider")).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .tag(5).Compose(composectx)

                    // League Table Tab
                    NavigationStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            LeagueTableView()
                                .modifier(StandardNavigationModifier()).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .tabItem { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Image(systemName = "list.number").Compose(composectx)
                            Text(LocalizedStringKey(stringLiteral = "Tabelle")).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .tag(6).Compose(composectx)

                    // Live View Tab
                    NavigationStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            LiveView(kickbaseManager = kickbaseManager)
                                .environmentObject(ligainsiderService)
                                .modifier(StandardNavigationModifier()).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .tabItem { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Image(systemName = "sportscourt.fill").Compose(composectx)
                            Text(LocalizedStringKey(stringLiteral = "Live")).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .tag(7).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .onAppear { ->  }
        }

    private fun getNavigationTitle(): String {
        when (selectedTab) {
            0 -> return "Team"
            1 -> return "Markt"
            2 -> return "Verkaufen"
            3 -> return "Aufstellung"
            4 -> return "Transfer-Tipps"
            5 -> return "Ligainsider"
            6 -> return "Tabelle"
            7 -> return "Live"
            else -> return "Team"
        }
    }

    private constructor(selectedTab: Int = 0, privatep: Nothing? = null) {
        this._selectedTab = skip.ui.State(selectedTab)
    }

    constructor(): this(privatep = null) {
    }
}

// MARK: - Team View mit prominenten Punktzahlen
internal class TeamView: View {
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    internal var ligainsiderService: LigainsiderService
        get() = _ligainsiderService.wrappedValue
        set(newValue) {
            _ligainsiderService.wrappedValue = newValue
        }
    internal var _ligainsiderService = skip.ui.Environment<LigainsiderService>()
    private var sortBy: TeamView.SortOption
        get() = _sortBy.wrappedValue
        set(newValue) {
            _sortBy.wrappedValue = newValue
        }
    private var _sortBy: skip.ui.State<TeamView.SortOption>
    private var playersForSale: Set<String>
        get() = _playersForSale.wrappedValue.sref({ this.playersForSale = it })
        set(newValue) {
            _playersForSale.wrappedValue = newValue.sref()
        }
    private var _playersForSale: skip.ui.State<Set<String>>
    private var showRecommendations: Boolean
        get() = _showRecommendations.wrappedValue
        set(newValue) {
            _showRecommendations.wrappedValue = newValue
        }
    private var _showRecommendations: skip.ui.State<Boolean>

    @androidx.annotation.Keep
    internal enum class SortOption(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, RawRepresentable<String> {
        name_("Name"),
        marketValue("Marktwert"),
        points("Punkte"),
        trend("Trend"),
        position("Position");

        @androidx.annotation.Keep
        companion object: CaseIterableCompanion<TeamView.SortOption> {
            fun init(rawValue: String): TeamView.SortOption? {
                return when (rawValue) {
                    "Name" -> SortOption.name_
                    "Marktwert" -> SortOption.marketValue
                    "Punkte" -> SortOption.points
                    "Trend" -> SortOption.trend
                    "Position" -> SortOption.position
                    else -> null
                }
            }

            override val allCases: Array<TeamView.SortOption>
                get() = arrayOf(name_, marketValue, points, trend, position)
        }
    }

    // Berechnung des Gesamtwerts der zum Verkauf ausgewählten Spieler
    private val totalSaleValue: Int
        get() {
            return kickbaseManager.teamPlayers
                .filter { it -> playersForSale.contains(it.id) }
                .reduce(initialResult = 0) { it, it_1 -> it + it_1.marketValue }
        }

    // Berechnung der Spieleranzahl nach Positionen (ohne zum Verkauf markierte)
    private val playerCounts: TeamPlayerCounts
        get() {
            val availablePlayers = kickbaseManager.teamPlayers.filter { it -> !playersForSale.contains(it.id) }

            val goalkeepers = availablePlayers.filter { it -> it.position == 1 }.count
            val defenders = availablePlayers.filter { it -> it.position == 2 }.count
            val midfielders = availablePlayers.filter { it -> it.position == 3 }.count
            val forwards = availablePlayers.filter { it -> it.position == 4 }.count
            val total = availablePlayers.count

            return TeamPlayerCounts(total = total, goalkeepers = goalkeepers, defenders = defenders, midfielders = midfielders, forwards = forwards)
        }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 0.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Tab Toggle
                    Picker(LocalizedStringKey(stringLiteral = "View"), selection = Binding({ _showRecommendations.wrappedValue }, { it -> _showRecommendations.wrappedValue = it })) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(LocalizedStringKey(stringLiteral = "Mein Team")).tag(false).Compose(composectx)
                            Text(LocalizedStringKey(stringLiteral = "Verkaufs-Tipps")).tag(true).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .pickerStyle(PickerStyle.segmented)
                    .padding().Compose(composectx)

                    if (showRecommendations) {
                        // Verkaufs-Empfehlungen View
                        SaleRecommendationsView(kickbaseManager = kickbaseManager).Compose(composectx)
                    } else {
                        // Original Team View
                        VStack(spacing = 0.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                // Neue Budget-Anzeige mit Verkaufslogik - mit explizitem padding-bottom
                                kickbaseManager.userStats?.let { stats ->
                                    TeamBudgetHeaderMain(currentBudget = stats.budget, saleValue = totalSaleValue)
                                        .padding(Edge.Set.bottom, 16.0).Compose(composectx)
                                }

                                // Player Count Overview and Sort Controls - mit separatem padding
                                VStack(spacing = 15.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        // Spieleranzahl-Übersicht
                                        PlayerCountOverview(playerCounts = playerCounts).Compose(composectx)

                                        // Sort Controls
                                        HStack { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                Text(LocalizedStringKey(stringLiteral = "Sortieren:"))
                                                    .font(Font.caption)
                                                    .foregroundColor(Color.secondary).Compose(composectx)

                                                Picker(LocalizedStringKey(stringLiteral = "Sortierung"), selection = Binding({ _sortBy.wrappedValue }, { it -> _sortBy.wrappedValue = it })) { ->
                                                    ComposeBuilder { composectx: ComposeContext ->
                                                        ForEach(SortOption.allCases, id = { it }) { option ->
                                                            ComposeBuilder { composectx: ComposeContext ->
                                                                Text(option.rawValue).tag(option).Compose(composectx)
                                                                ComposeResult.ok
                                                            }
                                                        }.Compose(composectx)
                                                        ComposeResult.ok
                                                    }
                                                }
                                                .pickerStyle(PickerStyle.segmented).Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }
                                .padding(Edge.Set.horizontal)
                                .padding(Edge.Set.bottom, 8.0).Compose(composectx)

                                // Spielerliste mit Verkaufs-Toggles
                                List { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        ForEach(filteredAndSortedPlayers) { player ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                PlayerRowViewWithSale(player = player, isSelectedForSale = playersForSale.contains(player.id), onToggleSale = { isSelected ->
                                                    if (isSelected) {
                                                        playersForSale.insert(player.id)
                                                    } else {
                                                        playersForSale.remove(player.id)
                                                    }
                                                }).Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }
                                .environmentObject(kickbaseManager)
                                .environmentObject(ligainsiderService)
                                .refreshable { -> MainActor.run {
                                    kickbaseManager.selectedLeague?.let { league ->
                                        kickbaseManager.loadTeamPlayers(for_ = league)
                                    }
                                } }.Compose(composectx)
                                ComposeResult.ok
                            }
                        }.Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }
            .onAppear { ->
                kickbaseManager.selectedLeague?.let { league ->
                    Task(isMainActor = true) { -> kickbaseManager.loadTeamPlayers(for_ = league) }
                }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedsortBy by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<TeamView.SortOption>, Any>) { mutableStateOf(_sortBy) }
        _sortBy = rememberedsortBy

        val rememberedplayersForSale by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Set<String>>, Any>) { mutableStateOf(_playersForSale) }
        _playersForSale = rememberedplayersForSale

        val rememberedshowRecommendations by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_showRecommendations) }
        _showRecommendations = rememberedshowRecommendations

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!
        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    private val filteredAndSortedPlayers: Array<Player>
        get() {
            // Entfernung der Suchfunktionalität - zeige alle Spieler
            return kickbaseManager.teamPlayers.sorted(by = l@{ player1, player2 ->
                when (sortBy) {
                    TeamView.SortOption.name_ -> return@l player1.lastName < player2.lastName
                    TeamView.SortOption.marketValue -> return@l player1.marketValue > player2.marketValue
                    TeamView.SortOption.points -> return@l player1.totalPoints > player2.totalPoints
                    TeamView.SortOption.trend -> return@l player1.tfhmvt > player2.tfhmvt
                    TeamView.SortOption.position -> return@l player1.position < player2.position
                }
            })
        }

    private constructor(sortBy: TeamView.SortOption = TeamView.SortOption.marketValue, playersForSale: Set<String> = setOf(), showRecommendations: Boolean = false, privatep: Nothing? = null) {
        this._sortBy = skip.ui.State(sortBy)
        this._playersForSale = skip.ui.State(playersForSale.sref())
        this._showRecommendations = skip.ui.State(showRecommendations)
    }

    constructor(): this(privatep = null) {
    }

    @androidx.annotation.Keep
    companion object {

        internal fun SortOption(rawValue: String): TeamView.SortOption? = SortOption.init(rawValue = rawValue)
    }
}

// MARK: - Player Row mit prominenten Punktzahlen
internal class PlayerRowView: View {
    internal val player: Player
    private var showingPlayerDetail: Boolean
        get() = _showingPlayerDetail.wrappedValue
        set(newValue) {
            _showingPlayerDetail.wrappedValue = newValue
        }
    private var _showingPlayerDetail: skip.ui.State<Boolean>

    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    internal var ligainsiderService: LigainsiderService
        get() = _ligainsiderService.wrappedValue
        set(newValue) {
            _ligainsiderService.wrappedValue = newValue
        }
    internal var _ligainsiderService = skip.ui.Environment<LigainsiderService>()

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            Button(action = { ->
                print("🔄 PlayerRowView: Tapped on player ${player.fullName}")
                showingPlayerDetail = true
            }) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    HStack(spacing = 12.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Position Badge
                            PositionBadge(position = player.position).Compose(composectx)

                            VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    // Name mit Status-Icons
                                    HStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(player.fullName)
                                                .font(Font.subheadline)
                                                .fontWeight(Font.Weight.medium).Compose(composectx)

                                            // Ligainsider Status Icon
                                            val status = ligainsiderService.getPlayerStatus(firstName = player.firstName, lastName = player.lastName)
                                            if (status != LigainsiderStatus.out) {
                                                Image(systemName = ligainsiderService.getIcon(for_ = status))
                                                    .foregroundColor(ligainsiderService.getColor(for_ = status))
                                                    .font(Font.caption).Compose(composectx)
                                            }

                                            // Status-Icons basierend auf st-Feld aus API-Daten anzeigen
                                            if (player.status == 1) {
                                                // Verletzt - rotes Kreuz
                                                Image(systemName = "cross.circle.fill")
                                                    .foregroundColor(Color.red)
                                                    .font(Font.caption).Compose(composectx)
                                            } else if (player.status == 2) {
                                                // Angeschlagen - Tabletten-Icon
                                                Image(systemName = "pills.fill")
                                                    .foregroundColor(Color.orange)
                                                    .font(Font.caption).Compose(composectx)
                                            } else if (player.status == 4) {
                                                // Aufbautraining - Hantel-Icon
                                                Image(systemName = "dumbbell.fill")
                                                    .foregroundColor(Color.blue)
                                                    .font(Font.caption).Compose(composectx)
                                            } else if (player.status == 8) {
                                                // Sperre - rote Karte
                                                Image(systemName = "rectangle.fill")
                                                    .foregroundColor(Color.red)
                                                    .font(Font.caption).Compose(composectx)
                                            }
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    // Team
                                    Text(player.fullTeamName)
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            Spacer().Compose(composectx)

                            // PUNKTZAHLEN - Feste Breite um Umbrüche zu vermeiden
                            VStack(alignment = HorizontalAlignment.trailing, spacing = 6.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    // Durchschnittspunkte - groß und prominent mit fester Breite
                                    HStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Image(systemName = "star.fill")
                                                .font(Font.subheadline)
                                                .foregroundColor(Color.orange).Compose(composectx)
                                            Text(String(format = "%.0f", player.averagePoints))
                                                .font(Font.headline)
                                                .fontWeight(Font.Weight.bold)
                                                .foregroundColor(Color.primary)
                                                .minimumScaleFactor(0.8)
                                                .lineLimit(1)
                                                .onAppear { -> print("📊 Displaying average points: ${player.averagePoints}") }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }
                                    .frame(minWidth = 60.0, alignment = Alignment.trailing).Compose(composectx)

                                    // Gesamtpunkte - jetzt kleinere Anzeige mit fester Breite
                                    HStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Image(systemName = "sum")
                                                .font(Font.caption2)
                                                .foregroundColor(Color.secondary).Compose(composectx)
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendInterpolation(player.totalPoints)
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.caption)
                                                .foregroundColor(Color.secondary)
                                                .minimumScaleFactor(0.8)
                                                .lineLimit(1)
                                                .onAppear { -> print("📊 Displaying total points: ${player.totalPoints}") }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }
                                    .frame(minWidth = 60.0, alignment = Alignment.trailing).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    // Marktwert mit fester Breite
                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendLiteral("€")
                                        str.appendInterpolation(formatValue(player.marketValue))
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.subheadline)
                                        .fontWeight(Font.Weight.semibold)
                                        .minimumScaleFactor(0.8)
                                        .lineLimit(1)
                                        .frame(minWidth = 50.0, alignment = Alignment.trailing).Compose(composectx)

                                    // Trend - verwende tfhmvt (Marktwertänderung seit letztem Update)
                                    if (player.tfhmvt != 0) {
                                        HStack(spacing = 2.0) { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                Image(systemName = if (player.tfhmvt >= 0) "arrow.up" else "arrow.down")
                                                    .font(Font.caption2)
                                                    .foregroundColor(if (player.tfhmvt >= 0) Color.green else Color.red).Compose(composectx)
                                                Text({
                                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                    str.appendLiteral("€")
                                                    str.appendInterpolation(formatValue(abs(player.tfhmvt)))
                                                    LocalizedStringKey(stringInterpolation = str)
                                                }())
                                                    .font(Font.caption)
                                                    .foregroundColor(if (player.tfhmvt >= 0) Color.green else Color.red)
                                                    .minimumScaleFactor(0.8)
                                                    .lineLimit(1).Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }
                                        .frame(minWidth = 50.0, alignment = Alignment.trailing).Compose(composectx)
                                    }
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding(Edge.Set.vertical, 8.0).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .accessibilityIdentifier("player_row_${player.id}")
            .sheet(isPresented = Binding({ _showingPlayerDetail.wrappedValue }, { it -> _showingPlayerDetail.wrappedValue = it })) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    PlayerDetailView(player = player)
                        .environmentObject(kickbaseManager)
                        .environmentObject(ligainsiderService).Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedshowingPlayerDetail by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_showingPlayerDetail) }
        _showingPlayerDetail = rememberedshowingPlayerDetail

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!
        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    private constructor(player: Player, showingPlayerDetail: Boolean = false, privatep: Nothing? = null) {
        this.player = player
        this._showingPlayerDetail = skip.ui.State(showingPlayerDetail)
    }

    constructor(player: Player): this(player = player, privatep = null) {
    }
}

// MARK: - Player Row mit Verkaufs-Toggle
internal class PlayerRowViewWithSale: View {
    internal val player: Player
    internal val isSelectedForSale: Boolean
    internal val onToggleSale: (Boolean) -> Unit

    private var showingPlayerDetail: Boolean
        get() = _showingPlayerDetail.wrappedValue
        set(newValue) {
            _showingPlayerDetail.wrappedValue = newValue
        }
    private var _showingPlayerDetail: skip.ui.State<Boolean>
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    internal var ligainsiderService: LigainsiderService
        get() = _ligainsiderService.wrappedValue
        set(newValue) {
            _ligainsiderService.wrappedValue = newValue
        }
    internal var _ligainsiderService = skip.ui.Environment<LigainsiderService>()

    private val photoUrl: URL?
        get() {
            ligainsiderService.getLigainsiderPlayer(firstName = player.firstName, lastName = player.lastName)?.let { ligaPlayer ->
                ligaPlayer.imageUrl?.let { imgString ->
                    (try { URL(string = imgString) } catch (_: NullReturnException) { null })?.let { url ->
                        return url
                    }
                }
            }
            return player.imageUrl
        }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            HStack(spacing = 12.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Spieler-Foto oder Position Badge
                    val matchtarget_0 = photoUrl
                    if (matchtarget_0 != null) {
                        val url = matchtarget_0
                        AsyncImage(url = url) { phase ->
                            ComposeBuilder { composectx: ComposeContext ->
                                when (phase) {
                                    is AsyncImagePhase.EmptyCase -> PositionBadge(position = player.position).Compose(composectx)
                                    is AsyncImagePhase.SuccessCase -> {
                                        val image = phase.associated0
                                        image
                                            .resizable()
                                            .aspectRatio(contentMode = ContentMode.fill)
                                            .frame(width = 32.0, height = 32.0)
                                            .clipShape(Circle()).Compose(composectx)
                                    }
                                    is AsyncImagePhase.FailureCase -> PositionBadge(position = player.position).Compose(composectx)
                                    else -> PositionBadge(position = player.position).Compose(composectx)
                                }
                                ComposeResult.ok
                            }
                        }
                        .frame(width = 32.0, height = 32.0).Compose(composectx)
                    } else {
                        PositionBadge(position = player.position).Compose(composectx)
                    }

                    // Spieler-Info Bereich (klickbar für Details)
                    Button(action = { -> showingPlayerDetail = true }) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            HStack(spacing = 12.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            // Name mit Status-Icons
                                            HStack(spacing = 4.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(player.fullName)
                                                        .font(Font.subheadline)
                                                        .fontWeight(Font.Weight.medium).Compose(composectx)

                                                    // Ligainsider Status Icon
                                                    val status = ligainsiderService.getPlayerStatus(firstName = player.firstName, lastName = player.lastName)
                                                    if (status != LigainsiderStatus.out) {
                                                        Image(systemName = ligainsiderService.getIcon(for_ = status))
                                                            .foregroundColor(ligainsiderService.getColor(for_ = status))
                                                            .font(Font.caption).Compose(composectx)
                                                    }

                                                    // Status-Icons basierend auf st-Feld aus API-Daten anzeigen
                                                    if (player.status == 1) {
                                                        // Verletzt - rotes Kreuz
                                                        Image(systemName = "cross.circle.fill")
                                                            .foregroundColor(Color.red)
                                                            .font(Font.caption).Compose(composectx)
                                                    } else if (player.status == 2) {
                                                        // Angeschlagen - Tabletten-Icon
                                                        Image(systemName = "pills.fill")
                                                            .foregroundColor(Color.orange)
                                                            .font(Font.caption).Compose(composectx)
                                                    } else if (player.status == 4) {
                                                        // Aufbautraining - Hantel-Icon
                                                        Image(systemName = "dumbbell.fill")
                                                            .foregroundColor(Color.blue)
                                                            .font(Font.caption).Compose(composectx)
                                                    } else if (player.status == 8) {
                                                        // Sperre - rote Karte
                                                        Image(systemName = "rectangle.fill")
                                                            .foregroundColor(Color.red)
                                                            .font(Font.caption).Compose(composectx)
                                                    }
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            // Nur Vereinsname anzeigen
                                            HStack(spacing = 4.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(player.fullTeamName)
                                                        .font(Font.caption)
                                                        .foregroundColor(Color.secondary)
                                                        .fontWeight(Font.Weight.medium).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    Spacer().Compose(composectx)

                                    // PUNKTZAHLEN - Feste Breite um Umbrüche zu vermeiden
                                    VStack(alignment = HorizontalAlignment.trailing, spacing = 6.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            // Durchschnittspunkte - groß und prominent mit fester Breite
                                            HStack(spacing = 4.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Image(systemName = "star.fill")
                                                        .font(Font.subheadline)
                                                        .foregroundColor(Color.orange).Compose(composectx)
                                                    Text(String(format = "%.0f", player.averagePoints))
                                                        .font(Font.subheadline)
                                                        .fontWeight(Font.Weight.bold)
                                                        .foregroundColor(Color.primary)
                                                        .minimumScaleFactor(0.8)
                                                        .lineLimit(1).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }
                                            .frame(minWidth = 60.0, alignment = Alignment.trailing).Compose(composectx)

                                            // Gesamtpunkte - jetzt kleinere Anzeige mit fester Breite
                                            HStack(spacing = 4.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Image(systemName = "sum")
                                                        .font(Font.caption2)
                                                        .foregroundColor(Color.secondary).Compose(composectx)
                                                    Text({
                                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                        str.appendInterpolation(player.totalPoints)
                                                        LocalizedStringKey(stringInterpolation = str)
                                                    }())
                                                        .font(Font.caption)
                                                        .foregroundColor(Color.secondary)
                                                        .minimumScaleFactor(0.8)
                                                        .lineLimit(1).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }
                                            .frame(minWidth = 60.0, alignment = Alignment.trailing).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            // Marktwert mit fester Breite
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendLiteral("€")
                                                str.appendInterpolation(formatValue(player.marketValue))
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.subheadline)
                                                .fontWeight(Font.Weight.semibold)
                                                .minimumScaleFactor(0.8)
                                                .lineLimit(1)
                                                .frame(minWidth = 50.0, alignment = Alignment.trailing).Compose(composectx)

                                            // Trend - verwende tfhmvt (Marktwertänderung seit letztem Update)
                                            if (player.tfhmvt != 0) {
                                                HStack(spacing = 2.0) { ->
                                                    ComposeBuilder { composectx: ComposeContext ->
                                                        Image(systemName = if (player.tfhmvt >= 0) "arrow.up" else "arrow.down")
                                                            .font(Font.caption2)
                                                            .foregroundColor(if (player.tfhmvt >= 0) Color.green else Color.red).Compose(composectx)
                                                        Text({
                                                            val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                            str.appendLiteral("€")
                                                            str.appendInterpolation(formatValue(abs(player.tfhmvt)))
                                                            LocalizedStringKey(stringInterpolation = str)
                                                        }())
                                                            .font(Font.caption)
                                                            .foregroundColor(if (player.tfhmvt >= 0) Color.green else Color.red)
                                                            .minimumScaleFactor(0.8)
                                                            .lineLimit(1).Compose(composectx)
                                                        ComposeResult.ok
                                                    }
                                                }
                                                .frame(minWidth = 50.0, alignment = Alignment.trailing).Compose(composectx)
                                            }
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .sheet(isPresented = Binding({ _showingPlayerDetail.wrappedValue }, { it -> _showingPlayerDetail.wrappedValue = it })) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            PlayerDetailView(player = player)
                                .environmentObject(kickbaseManager)
                                .environmentObject(ligainsiderService).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    // Toggle für Verkauf (separater Bereich)
                    Toggle(isOn = Binding<Boolean>(get = { -> isSelectedForSale }, set = { newValue -> onToggleSale(newValue) })) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(LocalizedStringKey(stringLiteral = "")).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .frame(width = 50.0, height = 30.0).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding(Edge.Set.vertical, 8.0)
            .sheet(isPresented = Binding({ _showingPlayerDetail.wrappedValue }, { it -> _showingPlayerDetail.wrappedValue = it })) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    PlayerDetailView(player = player).Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedshowingPlayerDetail by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_showingPlayerDetail) }
        _showingPlayerDetail = rememberedshowingPlayerDetail

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!
        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    private constructor(player: Player, isSelectedForSale: Boolean, onToggleSale: (Boolean) -> Unit, showingPlayerDetail: Boolean = false, privatep: Nothing? = null) {
        this.player = player
        this.isSelectedForSale = isSelectedForSale
        this.onToggleSale = onToggleSale
        this._showingPlayerDetail = skip.ui.State(showingPlayerDetail)
    }

    constructor(player: Player, isSelectedForSale: Boolean, onToggleSale: (Boolean) -> Unit): this(player = player, isSelectedForSale = isSelectedForSale, onToggleSale = onToggleSale, privatep = null) {
    }
}

// MARK: - Position Badge
internal class PositionBadge: View {
    internal val position: Int

    private val positionInfo: Tuple2<String, Color>
        get() {
            when (position) {
                1 -> return Tuple2("TW", Color.yellow)
                2 -> return Tuple2("ABW", Color.green)
                3 -> return Tuple2("MF", Color.blue)
                4 -> return Tuple2("ST", Color.red)
                else -> return Tuple2("?", Color.gray)
            }
        }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            Text(positionInfo.element0)
                .font(Font.caption)
                .fontWeight(Font.Weight.bold)
                .foregroundColor(Color.white)
                .frame(width = 32.0, height = 32.0)
                .background(positionInfo.element1)
                .clipShape(Circle()).Compose(composectx)
        }
    }

    constructor(position: Int) {
        this.position = position
    }
}

// MARK: - Team Stats Header mit Gesamtpunkten
internal class TeamStatsHeader: View {
    internal val stats: UserStats

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            HStack(spacing = 20.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(LocalizedStringKey(stringLiteral = "Budget"))
                                .font(Font.caption)
                                .foregroundColor(Color.secondary).Compose(composectx)
                            Text({
                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                str.appendLiteral("€")
                                str.appendInterpolation(formatValue(stats.budget))
                                LocalizedStringKey(stringInterpolation = str)
                            }())
                                .font(Font.headline)
                                .fontWeight(Font.Weight.bold).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    Spacer().Compose(composectx)

                    VStack(alignment = HorizontalAlignment.center, spacing = 4.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(LocalizedStringKey(stringLiteral = "Meine Punkte"))
                                .font(Font.caption)
                                .foregroundColor(Color.secondary).Compose(composectx)
                            HStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Image(systemName = "star.fill")
                                        .foregroundColor(Color.orange).Compose(composectx)
                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendInterpolation(stats.points)
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.title)
                                        .fontWeight(Font.Weight.bold).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    Spacer().Compose(composectx)

                    VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(LocalizedStringKey(stringLiteral = "Teamwert"))
                                .font(Font.caption)
                                .foregroundColor(Color.secondary).Compose(composectx)
                            HStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendLiteral("€")
                                        str.appendInterpolation(formatValue(stats.teamValue))
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.headline)
                                        .fontWeight(Font.Weight.bold).Compose(composectx)
                                    if (stats.teamValueTrend != 0) {
                                        Image(systemName = if (stats.teamValueTrend >= 0) "arrow.up" else "arrow.down")
                                            .foregroundColor(if (stats.teamValueTrend >= 0) Color.green else Color.red).Compose(composectx)
                                    }
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding()
            .background(Color.systemGray6Compat)
            .cornerRadius(12.0)
            .padding(Edge.Set.horizontal).Compose(composectx)
        }
    }

    constructor(stats: UserStats) {
        this.stats = stats
    }
}

// MARK: - Market View mit Punktzahlen und Sortierung
internal class MarketView: View {
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    private var sortBy: MarketView.MarketSortOption
        get() = _sortBy.wrappedValue
        set(newValue) {
            _sortBy.wrappedValue = newValue
        }
    private var _sortBy: skip.ui.State<MarketView.MarketSortOption>
    private var searchText: String
        get() = _searchText.wrappedValue
        set(newValue) {
            _searchText.wrappedValue = newValue
        }
    private var _searchText: skip.ui.State<String>

    @androidx.annotation.Keep
    internal enum class MarketSortOption(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, RawRepresentable<String> {
        price("Preis"),
        marketValue("Marktwert"),
        points("Punkte"),
        position("Position"),
        expiry("Ablaufdatum"),
        offers("Gebote");

        @androidx.annotation.Keep
        companion object: CaseIterableCompanion<MarketView.MarketSortOption> {
            fun init(rawValue: String): MarketView.MarketSortOption? {
                return when (rawValue) {
                    "Preis" -> MarketSortOption.price
                    "Marktwert" -> MarketSortOption.marketValue
                    "Punkte" -> MarketSortOption.points
                    "Position" -> MarketSortOption.position
                    "Ablaufdatum" -> MarketSortOption.expiry
                    "Gebote" -> MarketSortOption.offers
                    else -> null
                }
            }

            override val allCases: Array<MarketView.MarketSortOption>
                get() = arrayOf(price, marketValue, points, position, expiry, offers)
        }
    }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 0.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Search and Sort Controls
                    VStack(spacing = 10.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            HStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Image(systemName = "magnifyingglass")
                                        .foregroundColor(Color.gray).Compose(composectx)
                                    TextField(LocalizedStringKey(stringLiteral = "Spieler suchen..."), text = Binding({ _searchText.wrappedValue }, { it -> _searchText.wrappedValue = it }))
                                        .textFieldStyle(TextFieldStyle.roundedBorder).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            HStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Sortieren:"))
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary).Compose(composectx)

                                    Picker(LocalizedStringKey(stringLiteral = "Sortierung"), selection = Binding({ _sortBy.wrappedValue }, { it -> _sortBy.wrappedValue = it })) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            ForEach(MarketSortOption.allCases, id = { it }) { option ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(option.rawValue).tag(option).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }
                                    .pickerStyle(PickerStyle.segmented).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding(Edge.Set.horizontal)
                    .padding(Edge.Set.vertical, 8.0).Compose(composectx)

                    List { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            ForEach(filteredAndSortedMarketPlayers) { player ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    MarketPlayerRowView(player = player).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }
            .refreshable { -> MainActor.run {
                kickbaseManager.selectedLeague?.let { league ->
                    kickbaseManager.loadMarketPlayers(for_ = league)
                }
            } }
            .onAppear { ->
                kickbaseManager.selectedLeague?.let { league ->
                    Task(isMainActor = true) { -> kickbaseManager.loadMarketPlayers(for_ = league) }
                }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedsortBy by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<MarketView.MarketSortOption>, Any>) { mutableStateOf(_sortBy) }
        _sortBy = rememberedsortBy

        val rememberedsearchText by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<String>, Any>) { mutableStateOf(_searchText) }
        _searchText = rememberedsearchText

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!

        return super.Evaluate(context, options)
    }

    private val filteredAndSortedMarketPlayers: Array<MarketPlayer>
        get() {
            val filtered = (if (searchText.isEmpty) kickbaseManager.marketPlayers else kickbaseManager.marketPlayers.filter { player ->
                player.firstName.lowercased().contains(searchText.lowercased()) || player.lastName.lowercased().contains(searchText.lowercased()) || player.fullTeamName.lowercased().contains(searchText.lowercased()) || (player.owner?.name?.lowercased()?.contains(searchText.lowercased()) ?: false)
            }).sref()

            return filtered.sorted(by = l@{ player1, player2 ->
                when (sortBy) {
                    MarketView.MarketSortOption.price -> {
                        // Sortierung für Preis basiert nur auf Marktwert
                        return@l player1.marketValue > player2.marketValue
                    }
                    MarketView.MarketSortOption.marketValue -> {
                        // Sortierung für Marktwert basiert nur auf Marktwert
                        return@l player1.marketValue > player2.marketValue
                    }
                    MarketView.MarketSortOption.points -> return@l player1.averagePoints > player2.averagePoints
                    MarketView.MarketSortOption.position -> return@l player1.position < player2.position
                    MarketView.MarketSortOption.expiry -> {
                        // Sortierung nach Ablaufdatum orientiert sich am exs-Feld (niedrigster Wert oben)
                        return@l player1.exs < player2.exs
                    }
                    MarketView.MarketSortOption.offers -> return@l player1.offers > player2.offers
                }
            })
        }

    private fun parseExpiryDate(dateString: String): Date {
        val formatter = ISO8601DateFormatter()
        return (formatter.date(from = dateString) ?: Date.distantFuture).sref()
    }

    private constructor(sortBy: MarketView.MarketSortOption = MarketView.MarketSortOption.price, searchText: String = "", privatep: Nothing? = null) {
        this._sortBy = skip.ui.State(sortBy)
        this._searchText = skip.ui.State(searchText)
    }

    constructor(): this(privatep = null) {
    }

    @androidx.annotation.Keep
    companion object {

        internal fun MarketSortOption(rawValue: String): MarketView.MarketSortOption? = MarketSortOption.init(rawValue = rawValue)
    }
}

// MARK: - Market Player Row mit Punktzahlen
internal class MarketPlayerRowView: View {
    internal val player: MarketPlayer
    private var showingPlayerDetail: Boolean
        get() = _showingPlayerDetail.wrappedValue
        set(newValue) {
            _showingPlayerDetail.wrappedValue = newValue
        }
    private var _showingPlayerDetail: skip.ui.State<Boolean>
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    internal var ligainsiderService: LigainsiderService
        get() = _ligainsiderService.wrappedValue
        set(newValue) {
            _ligainsiderService.wrappedValue = newValue
        }
    internal var _ligainsiderService = skip.ui.Environment<LigainsiderService>()

    private val photoUrl: URL?
        get() {
            ligainsiderService.getLigainsiderPlayer(firstName = player.firstName, lastName = player.lastName)?.let { ligaPlayer ->
                ligaPlayer.imageUrl?.let { imgString ->
                    (try { URL(string = imgString) } catch (_: NullReturnException) { null })?.let { url ->
                        return url
                    }
                }
            }
            return player.imageUrl
        }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            Button(action = { ->
                print("🔄 MarketPlayerRowView: Tapped on player ${player.fullName}")
                showingPlayerDetail = true
            }) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    HStack(spacing = 12.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Spieler-Foto oder Position Badge
                            val matchtarget_1 = photoUrl
                            if (matchtarget_1 != null) {
                                val url = matchtarget_1
                                AsyncImage(url = url) { phase ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        when (phase) {
                                            is AsyncImagePhase.EmptyCase -> PositionBadge(position = player.position).Compose(composectx)
                                            is AsyncImagePhase.SuccessCase -> {
                                                val image = phase.associated0
                                                image
                                                    .resizable()
                                                    .aspectRatio(contentMode = ContentMode.fill)
                                                    .frame(width = 32.0, height = 32.0)
                                                    .clipShape(Circle()).Compose(composectx)
                                            }
                                            is AsyncImagePhase.FailureCase -> PositionBadge(position = player.position).Compose(composectx)
                                            else -> PositionBadge(position = player.position).Compose(composectx)
                                        }
                                        ComposeResult.ok
                                    }
                                }
                                .frame(width = 32.0, height = 32.0).Compose(composectx)
                            } else {
                                PositionBadge(position = player.position).Compose(composectx)
                            }

                            VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    // Name mit Status-Icons
                                    HStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(player.fullName)
                                                .font(Font.headline)
                                                .fontWeight(Font.Weight.medium).Compose(composectx)

                                            // Ligainsider Status Icon
                                            val status = ligainsiderService.getPlayerStatus(firstName = player.firstName, lastName = player.lastName)
                                            if (status != LigainsiderStatus.out) {
                                                Image(systemName = ligainsiderService.getIcon(for_ = status))
                                                    .foregroundColor(ligainsiderService.getColor(for_ = status))
                                                    .font(Font.caption).Compose(composectx)
                                            }

                                            // Status-Icons basierend auf status-Feld aus API-Daten anzeigen
                                            if (player.status == 1) {
                                                // Verletzt - rotes Kreuz
                                                Image(systemName = "cross.circle.fill")
                                                    .foregroundColor(Color.red)
                                                    .font(Font.caption).Compose(composectx)
                                            } else if (player.status == 2) {
                                                // Angeschlagen - Tabletten-Icon
                                                Image(systemName = "pills.fill")
                                                    .foregroundColor(Color.orange)
                                                    .font(Font.caption).Compose(composectx)
                                            } else if (player.status == 4) {
                                                // Aufbautraining - Hantel-Icon
                                                Image(systemName = "dumbbell.fill")
                                                    .foregroundColor(Color.blue)
                                                    .font(Font.caption).Compose(composectx)
                                            } else if (player.status == 8) {
                                                // Sperre - rote Karte
                                                Image(systemName = "rectangle.fill")
                                                    .foregroundColor(Color.red)
                                                    .font(Font.caption).Compose(composectx)
                                            }
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    // Team und Owner
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendInterpolation(player.fullTeamName)
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.caption)
                                                .foregroundColor(Color.secondary)
                                                .onAppear { -> print("📝 Displaying team '${player.fullTeamName}'") }.Compose(composectx)

                                            // Owner-Information anzeigen, falls vorhanden
                                            player.owner?.let { owner ->
                                                HStack(spacing = 4.0) { ->
                                                    ComposeBuilder { composectx: ComposeContext ->
                                                        Image(systemName = "person.fill")
                                                            .font(Font.caption2)
                                                            .foregroundColor(Color.blue).Compose(composectx)
                                                        Text({
                                                            val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                            str.appendLiteral("Besitzer: ")
                                                            str.appendInterpolation(owner.name)
                                                            LocalizedStringKey(stringInterpolation = str)
                                                        }())
                                                            .font(Font.caption2)
                                                            .foregroundColor(Color.blue)
                                                            .fontWeight(Font.Weight.medium).Compose(composectx)

                                                        // Verified badge falls der User verifiziert ist
                                                        if (owner.isVerified) {
                                                            Image(systemName = "checkmark.seal.fill")
                                                                .font(Font.caption2)
                                                                .foregroundColor(Color.green).Compose(composectx)
                                                        }
                                                        ComposeResult.ok
                                                    }
                                                }
                                                .onAppear { -> print("👤 Displaying owner: '${owner.name}' (ID: ${owner.id}, verified: ${owner.isVerified})") }.Compose(composectx)
                                            }
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            Spacer().Compose(composectx)

                            // Punktzahlen für Marktplayer - mit fester Breite
                            VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    // Durchschnittspunkte - jetzt groß und prominent mit fester Breite
                                    HStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Image(systemName = "star.fill")
                                                .font(Font.subheadline)
                                                .foregroundColor(Color.orange).Compose(composectx)
                                            Text(String(format = "%.0f", player.averagePoints))
                                                .font(Font.title2)
                                                .fontWeight(Font.Weight.bold)
                                                .foregroundColor(Color.primary)
                                                .minimumScaleFactor(0.8)
                                                .lineLimit(1).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }
                                    .frame(minWidth = 60.0, alignment = Alignment.trailing).Compose(composectx)

                                    // Gesamtpunkte - jetzt kleinere Anzeige mit fester Breite
                                    HStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Image(systemName = "sum")
                                                .font(Font.caption2)
                                                .foregroundColor(Color.secondary).Compose(composectx)
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendInterpolation(player.totalPoints)
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.caption)
                                                .foregroundColor(Color.secondary)
                                                .minimumScaleFactor(0.8)
                                                .lineLimit(1).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }
                                    .frame(minWidth = 60.0, alignment = Alignment.trailing).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    // Verkaufspreis mit fester Breite
                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendLiteral("€")
                                        str.appendInterpolation(formatValue(player.price))
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.subheadline)
                                        .fontWeight(Font.Weight.bold)
                                        .foregroundColor(Color.green)
                                        .minimumScaleFactor(0.8)
                                        .lineLimit(1)
                                        .frame(minWidth = 50.0, alignment = Alignment.trailing).Compose(composectx)

                                    // Marktwert mit fester Breite
                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendLiteral("MW: €")
                                        str.appendInterpolation(formatValue(player.marketValue))
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary)
                                        .minimumScaleFactor(0.8)
                                        .lineLimit(1)
                                        .frame(minWidth = 50.0, alignment = Alignment.trailing).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding(Edge.Set.vertical, 8.0).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .sheet(isPresented = Binding({ _showingPlayerDetail.wrappedValue }, { it -> _showingPlayerDetail.wrappedValue = it })) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    PlayerDetailView(player = convertMarketPlayerToTeamPlayer(player))
                        .environmentObject(kickbaseManager)
                        .environmentObject(ligainsiderService).Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedshowingPlayerDetail by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_showingPlayerDetail) }
        _showingPlayerDetail = rememberedshowingPlayerDetail

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!
        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    private constructor(player: MarketPlayer, showingPlayerDetail: Boolean = false, privatep: Nothing? = null) {
        this.player = player
        this._showingPlayerDetail = skip.ui.State(showingPlayerDetail)
    }

    constructor(player: MarketPlayer): this(player = player, privatep = null) {
    }
}

// MARK: - Sales Recommendation View
// TODO: Consider moving sale recommendation logic into `PlayerRecommendationService` to reuse caching/batching
internal class SalesRecommendationView: View {
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    private var selectedGoal: SalesRecommendationView.OptimizationGoal
        get() = _selectedGoal.wrappedValue
        set(newValue) {
            _selectedGoal.wrappedValue = newValue
        }
    private var _selectedGoal: skip.ui.State<SalesRecommendationView.OptimizationGoal>
    private var recommendedSales: Array<SalesRecommendation>
        get() = _recommendedSales.wrappedValue.sref({ this.recommendedSales = it })
        set(newValue) {
            _recommendedSales.wrappedValue = newValue.sref()
        }
    private var _recommendedSales: skip.ui.State<Array<SalesRecommendation>>
    private var selectedSales: Set<String>
        get() = _selectedSales.wrappedValue.sref({ this.selectedSales = it })
        set(newValue) {
            _selectedSales.wrappedValue = newValue.sref()
        }
    private var _selectedSales: skip.ui.State<Set<String>>

    // Task management for recommendation generation (cancel/debounce)
    private var recommendationTask: Task<Unit>?
        get() = _recommendationTask.wrappedValue
        set(newValue) {
            _recommendationTask.wrappedValue = newValue
        }
    private var _recommendationTask: skip.ui.State<Task<Unit>?> = skip.ui.State(null)
    private var debounceTask: Task<Unit>?
        get() = _debounceTask.wrappedValue
        set(newValue) {
            _debounceTask.wrappedValue = newValue
        }
    private var _debounceTask: skip.ui.State<Task<Unit>?> = skip.ui.State(null)

    @androidx.annotation.Keep
    internal enum class OptimizationGoal(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, RawRepresentable<String> {
        balancePositive("Budget ins Plus"),
        maximizeProfit("Maximaler Profit"),
        keepBestPlayers("Beste Spieler behalten");

        @androidx.annotation.Keep
        companion object: CaseIterableCompanion<SalesRecommendationView.OptimizationGoal> {
            fun init(rawValue: String): SalesRecommendationView.OptimizationGoal? {
                return when (rawValue) {
                    "Budget ins Plus" -> OptimizationGoal.balancePositive
                    "Maximaler Profit" -> OptimizationGoal.maximizeProfit
                    "Beste Spieler behalten" -> OptimizationGoal.keepBestPlayers
                    else -> null
                }
            }

            override val allCases: Array<SalesRecommendationView.OptimizationGoal>
                get() = arrayOf(balancePositive, maximizeProfit, keepBestPlayers)
        }
    }

    // Berechnung der Spieleranzahl nach Positionen (ohne ausgewählte Verkäufe)
    private val playerCountsAfterSales: TeamPlayerCounts
        get() {
            val remainingPlayers = kickbaseManager.teamPlayers.filter { it -> !selectedSales.contains(it.id) }

            val goalkeepers = remainingPlayers.filter { it -> it.position == 1 }.count
            val defenders = remainingPlayers.filter { it -> it.position == 2 }.count
            val midfielders = remainingPlayers.filter { it -> it.position == 3 }.count
            val forwards = remainingPlayers.filter { it -> it.position == 4 }.count
            val total = remainingPlayers.count

            return TeamPlayerCounts(total = total, goalkeepers = goalkeepers, defenders = defenders, midfielders = midfielders, forwards = forwards)
        }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            ScrollView { ->
                ComposeBuilder { composectx: ComposeContext ->
                    VStack(spacing = 20.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Header
                            SalesRecommendationHeader(currentBudget = kickbaseManager.userStats?.budget ?: 0, recommendedSaleValue = recommendedSales.map { it -> it.expectedValue }.reduce(initialResult = 0, { it, it_1 -> it + it_1 }), selectedSaleValue = selectedSales.compactMap { id ->
                                recommendedSales.first(where = { it -> it.player.id == id })?.expectedValue
                            }.reduce(initialResult = 0, { it, it_1 -> it + it_1 })).Compose(composectx)

                            // Spieleranzahl-Übersicht
                            VStack(spacing = 16.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Verbleibende Spieler nach Verkauf"))
                                        .font(Font.title2)
                                        .fontWeight(Font.Weight.bold).Compose(composectx)

                                    PlayerCountOverview(playerCounts = playerCountsAfterSales).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .padding()
                            .background(Color.systemGray6Compat)
                            .cornerRadius(12.0).Compose(composectx)

                            // Optimierungsziel Auswahl
                            VStack(spacing = 16.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Optimierungsziel"))
                                        .font(Font.title2)
                                        .fontWeight(Font.Weight.bold).Compose(composectx)

                                    Picker(LocalizedStringKey(stringLiteral = "Ziel wählen"), selection = Binding({ _selectedGoal.wrappedValue }, { it -> _selectedGoal.wrappedValue = it })) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            ForEach(OptimizationGoal.allCases, id = { it }) { goal ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(goal.rawValue).tag(goal).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }
                                    .pickerStyle(PickerStyle.segmented).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .padding()
                            .background(Color.systemGray6Compat)
                            .cornerRadius(12.0).Compose(composectx)

                            // Verkaufs-Empfehlungen
                            SalesRecommendationSummary(recommendations = recommendedSales, optimizationGoal = selectedGoal).Compose(composectx)

                            // Detaillierte Empfehlungen
                            VStack(alignment = HorizontalAlignment.leading, spacing = 16.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Detaillierte Empfehlungen"))
                                        .font(Font.title2)
                                        .fontWeight(Font.Weight.bold).Compose(composectx)

                                    ForEach(recommendedSales) { recommendation ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            SalesRecommendationRow(recommendation = recommendation, isSelected = selectedSales.contains(recommendation.player.id), onToggle = { isSelected ->
                                                if (isSelected) {
                                                    selectedSales.insert(recommendation.player.id)
                                                } else {
                                                    selectedSales.remove(recommendation.player.id)
                                                }
                                            }).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .padding()
                            .background(Color.systemGray6Compat)
                            .cornerRadius(12.0).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding().Compose(composectx)
                    ComposeResult.ok
                }
            }
            .onAppear { -> scheduleGenerateImmediate() }
            .onChange(of = selectedGoal) { _ -> scheduleGenerateDebounced() }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedselectedGoal by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<SalesRecommendationView.OptimizationGoal>, Any>) { mutableStateOf(_selectedGoal) }
        _selectedGoal = rememberedselectedGoal

        val rememberedrecommendedSales by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Array<SalesRecommendation>>, Any>) { mutableStateOf(_recommendedSales) }
        _recommendedSales = rememberedrecommendedSales

        val rememberedselectedSales by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Set<String>>, Any>) { mutableStateOf(_selectedSales) }
        _selectedSales = rememberedselectedSales

        val rememberedrecommendationTask by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Task<Unit>?>, Any>) { mutableStateOf(_recommendationTask) }
        _recommendationTask = rememberedrecommendationTask

        val remembereddebounceTask by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Task<Unit>?>, Any>) { mutableStateOf(_debounceTask) }
        _debounceTask = remembereddebounceTask

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!

        return super.Evaluate(context, options)
    }

    private fun generateIntelligentRecommendations() {
        // Cancel any running tasks and start a new immediate task
        recommendationTask?.cancel()
        recommendationTask = Task(isMainActor = true) { -> this.generateIntelligentRecommendationsAsync() }
    }

    private fun scheduleGenerateImmediate() {
        debounceTask?.cancel()
        recommendationTask?.cancel()
        recommendationTask = Task(isMainActor = true) { -> this.generateIntelligentRecommendationsAsync() }
    }

    private fun scheduleGenerateDebounced() {
        // Debounce input changes (e.g., segment switch)
        debounceTask?.cancel()
        debounceTask = Task l@{ ->
            try { Task.sleep(nanoseconds = 300_000_000) } catch (_: Throwable) { null } // 300 ms
            if (Task.isCancelled) {
                return@l
            }
            recommendationTask?.cancel()
            recommendationTask = Task(isMainActor = true) { -> this.generateIntelligentRecommendationsAsync() }
        }
    }

    private suspend fun generateIntelligentRecommendationsAsync(): Unit = Async.run l@{
        val startTs = Date()
        print("🔧 Recommendation generation started at ${startTs}")

        val allPlayers = kickbaseManager.teamPlayers.sref()
        val currentBudget = kickbaseManager.userStats?.budget ?: 0

        var newRecommendations: Array<SalesRecommendation> = arrayOf()

        // Bounded concurrency: verarbeite Spieler in Chunks, z.B. 6 parallel
        val concurrency = 6
        val players = allPlayers.sref()

        for (start in stride(from = 0, to = players.count, by = concurrency)) {
            if (Task.isCancelled) {
                return@l
            }
            val end = skip.lib.min(players.count, start + concurrency)
            val slice = Array(players[start until end])

            withTaskGroup(of = SalesRecommendation?::class) l@{ group ->
                for (player in slice.sref()) {
                    group.addTask l@{ ->
                        if (Task.isCancelled) {
                            return@l null
                        }
                        return@l this.analyzePlayerForSale(player = player, allPlayers = players, currentBudget = currentBudget, optimizationGoal = selectedGoal)
                    }
                }

                for (result in group.sref()) {
                    if (Task.isCancelled) {
                        return@l
                    }
                    result.sref()?.let { rec ->
                        newRecommendations.append(rec)
                    }
                }
            }

            // Sortiere nach Priorität und Impact - beste Verkaufskandidaten zuerst
            newRecommendations.sort l@{ recommendation1, recommendation2 ->
                val priority1Value = getPriorityValue(recommendation1.priority)
                val priority2Value = getPriorityValue(recommendation2.priority)
                val impact1Value = getImpactValue(recommendation1.impact)
                val impact2Value = getImpactValue(recommendation2.impact)

                // Erstelle einen kombinierten Score: Hohe Priorität (niedrige Zahl) + Niedriger Impact (niedrige Zahl) = besserer Kandidat
                val score1 = priority1Value + impact1Value
                val score2 = priority2Value + impact2Value

                if (score1 != score2) {
                    return@l score1 < score2 // Niedrigerer Score = besserer Verkaufskandidat
                }

                // Bei gleichem Score: Sekundäre Sortierung nach Optimierungsziel
                when (selectedGoal) {
                    SalesRecommendationView.OptimizationGoal.balancePositive -> return@l recommendation1.player.marketValue > recommendation2.player.marketValue
                    SalesRecommendationView.OptimizationGoal.maximizeProfit -> return@l recommendation1.expectedValue > recommendation2.expectedValue
                    SalesRecommendationView.OptimizationGoal.keepBestPlayers -> return@l recommendation1.player.averagePoints < recommendation2.player.averagePoints
                }
            }

            // Inkrementelles UI-Update
            MainActor.run { -> this.recommendedSales = newRecommendations }
            print("🔔 Published ${newRecommendations.count} intermediate recommendations after ${Date().timeIntervalSince(startTs)}s")
        }

        val duration = Date().timeIntervalSince(startTs)
        print("🔧 Recommendation generation finished in ${duration} seconds")
    }

    private suspend fun analyzePlayerForSale(player: Player, allPlayers: Array<Player>, currentBudget: Int, optimizationGoal: SalesRecommendationView.OptimizationGoal): SalesRecommendation? = Async.run l@{

        var reasons: Array<String> = arrayOf()
        var shouldSell = false
        var priority: SalesRecommendation.Priority = SalesRecommendation.Priority.low

        // 1. KRITISCHE KRITERIEN - Immer verkaufen

        // Verletzte Spieler (Status 1)
        if (player.status == 1) {
            reasons.append("Verletzt")
            shouldSell = true
            priority = SalesRecommendation.Priority.high
        }

        // Gesperrte Spieler (Status 8)
        if (player.status == 8) {
            reasons.append("Gesperrt")
            shouldSell = true
            priority = SalesRecommendation.Priority.high
        }

        // Spieler im Aufbautraining (Status 4)
        if (player.status == 4) {
            reasons.append("Aufbautraining")
            shouldSell = true
            priority = SalesRecommendation.Priority.high
        }

        // 2. PERFORMANCE-ANALYSE & FIXTURE-ANALYSE parallel ausführen
        withTaskGroup(of = Unit::class) { group ->
            // Performance-Analyse parallel starten
            group.addTask { -> this.analyzePlayerPerformance(player = player, reasons = InOut({ reasons }, { reasons = it }), shouldSell = InOut({ shouldSell }, { shouldSell = it }), priority = InOut({ priority }, { priority = it })) }

            // Fixture-Analyse parallel starten
            group.addTask { -> this.analyzeUpcomingFixtures(player = player, reasons = InOut({ reasons }, { reasons = it }), shouldSell = InOut({ shouldSell }, { shouldSell = it }), priority = InOut({ priority }, { priority = it })) }

            // Auf beide Tasks warten
            group.waitForAll()
        }

        // 4. POSITIONSLIMITS PRÜFEN
        val positionAnalysis = analyzePositionRedundancy(player = player, allPlayers = allPlayers)
        if (positionAnalysis.isRedundant) {
            shouldSell = true
            if (priority == SalesRecommendation.Priority.low) {
                priority = if (positionAnalysis.isWeakestInPosition) SalesRecommendation.Priority.medium else SalesRecommendation.Priority.low
            }
        }

        // 5. BUDGET-BASIERTE KRITERIEN (nur wenn Budget negativ)
        if (currentBudget < 0) {
            val budgetPressure = abs(currentBudget)

            // Bei hohem Budgetdruck verkaufe teure Spieler
            if (player.marketValue >= budgetPressure / 2) {
                reasons.append("Budget im Minus")
                shouldSell = true
                if (priority == SalesRecommendation.Priority.low) {
                    priority = SalesRecommendation.Priority.medium
                }
            }
        }

        // 6. OPTIMIERUNGSZIEL-SPEZIFISCHE KRITERIEN
        when (optimizationGoal) {
            SalesRecommendationView.OptimizationGoal.balancePositive -> {
                // Verkaufe Spieler mit schlechtem Preis-Leistungs-Verhältnis
                if (isPlayerOverpriced(player = player, allPlayers = allPlayers)) {
                    reasons.append("Schlechtes Preis-Leistungs-Verhältnis")
                    shouldSell = true
                }
            }
            SalesRecommendationView.OptimizationGoal.maximizeProfit -> {
                // Verkaufe Spieler mit positiver Marktwertentwicklung
                if (player.tfhmvt > 0 && player.tfhmvt > player.marketValue / 10) {
                    reasons.append("Hoher Marktwertgewinn")
                    shouldSell = true
                }
            }
            SalesRecommendationView.OptimizationGoal.keepBestPlayers -> {
                // Verkaufe schwächste Spieler auf der Position
                if (isPlayerWeakestInPosition(player = player, allPlayers = allPlayers) && positionAnalysis.isRedundant) {
                    reasons.append("Schwächster Spieler auf Position")
                    shouldSell = true
                }
            }
        }

        // 7. WEITERE PERFORMANCE-KRITERIEN (nur bei geringer Priorität)
        if (priority == SalesRecommendation.Priority.low) {
            val teamAveragePoints = allPlayers.map({ it.averagePoints }).reduce(initialResult = 0.0, { it, it_1 -> it + it_1 }) / Double(allPlayers.count)

            if (player.averagePoints < teamAveragePoints * 0.6) {
                reasons.append("Schwache Performance")
                shouldSell = true
            }

            // Fallender Marktwert
            if (player.tfhmvt < 0 && abs(player.tfhmvt) > player.marketValue / 8) {
                reasons.append("Fallender Marktwert")
                shouldSell = true
            }
        }
        if (!shouldSell) {
            return@l null
        }

        // Aufstellungsimpact berechnen
        val lineupImpact = calculateLineupImpact(player = player, allPlayers = allPlayers)

        return@l SalesRecommendation(player = player, reason = reasons.joined(separator = " • "), priority = priority, expectedValue = player.marketValue, impact = lineupImpact)
    }

    // MARK: - Performance Analysis Helper
    private suspend fun analyzePlayerPerformance(player: Player, reasons: InOut<Array<String>>, shouldSell: InOut<Boolean>, priority: InOut<SalesRecommendation.Priority>): Unit = Async.run l@{
        val selectedLeague_0 = kickbaseManager.selectedLeague
        if (selectedLeague_0 == null) {
            return@l
        }
        if (Task.isCancelled) {
            return@l
        }

        // Lade die letzten 5 Spiele des Spielers
        kickbaseManager.loadPlayerRecentPerformanceWithTeamInfo(playerId = player.id, leagueId = selectedLeague_0.id)?.let { recentPerformances ->

            // Analysiere die letzten gespielten Spiele
            val playedGames = recentPerformances.filter { it -> it.hasPlayed }

            if (playedGames.count >= 3) {
                val recentPoints = playedGames.map { it -> it.points }
                val recentAverage = Double(recentPoints.reduce(initialResult = 0, { it, it_1 -> it + it_1 })) / Double(recentPoints.count)

                // Vergleiche mit der Saison-Durchschnittsleistung
                if (recentAverage < player.averagePoints * 0.6) {
                    reasons.value.append("Schwache Form (letzte ${playedGames.count} Spiele: ${String(format = "%.1f", recentAverage)} Pkt.)")
                    shouldSell.value = true
                    if (priority.value == SalesRecommendation.Priority.low) {
                        priority.value = SalesRecommendation.Priority.medium
                    }
                }

                // Prüfe auf konstant schlechte Leistung
                val goodGames = recentPoints.filter { it -> Double(it) >= player.averagePoints * 0.8 }
                    .count
                if (goodGames == 0 && playedGames.count >= 3) {
                    reasons.value.append("Keine guten Spiele in letzten ${playedGames.count} Partien")
                    shouldSell.value = true
                    if (priority.value == SalesRecommendation.Priority.low) {
                        priority.value = SalesRecommendation.Priority.medium
                    }
                }
            }

            // Analysiere Einsatzzeiten und Status
            val startingElevenGames = playedGames.filter { it -> it.wasStartingEleven }.count
            val substituteGames = playedGames.filter { it -> it.wasSubstitute }.count
            val notInSquadGames = recentPerformances.filter { it -> it.wasNotInSquad }.count

            // Spieler verliert Stammplatz
            if (playedGames.count >= 3 && startingElevenGames == 0 && substituteGames > 0) {
                reasons.value.append("Nur noch Einwechselspieler (keine Startelf)")
                shouldSell.value = true
            }

            // Spieler fällt aus dem Kader
            if (notInSquadGames >= 2) {
                reasons.value.append("Häufig nicht im Kader (${notInSquadGames} Spiele)")
                shouldSell.value = true
                if (priority.value == SalesRecommendation.Priority.low) {
                    priority.value = SalesRecommendation.Priority.medium
                }
            }
        }
    }

    // MARK: - Upcoming Fixtures Analysis Helper
    private suspend fun analyzeUpcomingFixtures(player: Player, reasons: InOut<Array<String>>, shouldSell: InOut<Boolean>, priority: InOut<SalesRecommendation.Priority>): Unit = Async.run l@{
        val selectedLeague_1 = kickbaseManager.selectedLeague
        if (selectedLeague_1 == null) {
            print("⚠️ Fixture-Analyse: Keine Liga ausgewählt für ${player.fullName}")
            return@l
        }
        if (Task.isCancelled) {
            return@l
        }

        print("🔍 Fixture-Analyse startet für ${player.fullName} (Team: ${player.teamId})")

        try {
            // Lade alle Performance-Daten um zukünftige Spiele zu analysieren
            val matchtarget_2 = kickbaseManager.loadPlayerPerformanceWithTeamInfo(playerId = player.id, leagueId = selectedLeague_1.id)
            if (matchtarget_2 != null) {
                val allPerformances = matchtarget_2
                print("📊 ${allPerformances.count} Performance-Einträge geladen für ${player.fullName}")

                val currentMatchDay = getCurrentMatchDay(allPerformances = allPerformances)
                print("🗓️ Aktueller Spieltag: ${currentMatchDay}")

                // Finde zukünftige Spiele (noch nicht gespielt)
                val upcomingMatches = allPerformances.filter { it -> !it.hasPlayed && it.matchDay >= currentMatchDay }
                    .sorted { it, it_1 -> it.matchDay < it_1.matchDay }
                    .prefix(3) // Analysiere die nächsten 3 Spiele

                print("🎯 ${upcomingMatches.count} zukünftige Spiele gefunden für ${player.fullName}")

                if (upcomingMatches.count >= 1) {
                    // Debug: Zeige kommende Spiele - korrigierte Logik mit player.teamId
                    for ((index, match) in upcomingMatches.enumerated()) {
                        // Verwende die neuen Methoden mit der korrekten playerTeamId aus dem Player-Objekt
                        val opponentTeamId = match.basePerformance.getOpponentTeamId(playerTeamId = player.teamId)
                        val isAwayGame = !match.basePerformance.getIsHomeMatch(playerTeamId = player.teamId)
                        print("   Spiel ${index + 1}: Spieltag ${match.matchDay}, Gegner: ${opponentTeamId}, Auswärts: ${isAwayGame}")
                    }

                    val fixtureAnalysis = analyzeFixtureDifficulty(matches = Array(upcomingMatches), playerTeam = player.teamId)
                    print("📈 Fixture-Analyse Ergebnis für ${player.fullName}:")
                    print("   - Durchschnittliche Schwierigkeit: ${String(format = "%.2f", fixtureAnalysis.averageDifficulty)}")
                    print("   - Top-Teams als Gegner: ${fixtureAnalysis.topTeamOpponents}")
                    print("   - Schwere Auswärtsspiele: ${fixtureAnalysis.difficultAwayGames}")

                    // Schwere Fixture-Liste
                    if (fixtureAnalysis.averageDifficulty >= 0.7) {
                        val difficultyPercentage = Int(fixtureAnalysis.averageDifficulty * 100)
                        val reason = "Schwere Gegner kommend (${difficultyPercentage}% Schwierigkeit, ${upcomingMatches.count} Spiele)"
                        reasons.value.append(reason)
                        shouldSell.value = true
                        print("✅ Verkaufsgrund hinzugefügt: ${reason}")

                        // Besonders schwer -> höhere Priorität
                        if (fixtureAnalysis.averageDifficulty >= 0.8 && priority.value != SalesRecommendation.Priority.high) {
                            priority.value = SalesRecommendation.Priority.medium
                            print("⬆️ Priorität auf MEDIUM erhöht wegen sehr schwerer Fixtures")
                        }
                    }

                    // Viele Top-6-Teams als Gegner
                    if (fixtureAnalysis.topTeamOpponents >= 2) {
                        val reason = "Viele Top-Teams als Gegner (${fixtureAnalysis.topTeamOpponents} von ${upcomingMatches.count})"
                        reasons.value.append(reason)
                        shouldSell.value = true
                        print("✅ Verkaufsgrund hinzugefügt: ${reason}")
                        if (priority.value == SalesRecommendation.Priority.low) {
                            priority.value = SalesRecommendation.Priority.medium
                            print("⬆️ Priorität auf MEDIUM erhöht wegen vieler Top-Teams")
                        }
                    }

                    // Auswärtsspiele-Schwere
                    if (fixtureAnalysis.difficultAwayGames >= 2) {
                        val reason = "Schwere Auswärtsspiele (${fixtureAnalysis.difficultAwayGames} Spiele)"
                        reasons.value.append(reason)
                        shouldSell.value = true
                        print("✅ Verkaufsgrund hinzugefügt: ${reason}")
                    }

                    // Positive Indikatoren (gegen Verkauf)
                    if (fixtureAnalysis.averageDifficulty <= 0.3) {
                        print("🟢 Sehr einfache kommende Spiele - Verkauf weniger empfehlenswert")
                        // Sehr einfache kommende Spiele - weniger verkaufsbereit
                        if (priority.value == SalesRecommendation.Priority.low && reasons.value.count <= 2) {
                            print("⬇️ Verkaufsempfehlung reduziert wegen einfacher Fixtures")
                            // Entferne schwächere Verkaufsgründe wenn einfache Spiele kommen
                            return@l
                        }
                    }
                } else {
                    print("⚠️ Keine zukünftigen Spiele gefunden für ${player.fullName}")
                }
            } else {
                print("❌ Keine Performance-Daten geladen für ${player.fullName}")
            }
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Fehler beim Laden der Fixture-Daten für ${player.fullName}: ${error}")
        }
    }

    // MARK: - Fixture Difficulty Analysis
    private fun analyzeFixtureDifficulty(matches: Array<EnhancedMatchPerformance>, playerTeam: String): FixtureAnalysis {
        var totalDifficulty: Double = 0.0
        var topTeamOpponents = 0
        var difficultAwayGames = 0

        for (match in matches.sref()) {
            // Verwende die neuen Methoden mit der korrekten playerTeamId
            val opponentTeamId = match.basePerformance.getOpponentTeamId(playerTeamId = playerTeam)
            val isAwayGame = !match.basePerformance.getIsHomeMatch(playerTeamId = playerTeam)

            // Berechne Gegnerstärke basierend auf echter Platzierung aus den Team-Infos
            val opponentStrength = getTeamStrengthFromMatch(match = match, opponentTeamId = opponentTeamId)
            val difficultyScore = calculateMatchDifficulty(opponentStrength = opponentStrength, isAwayGame = isAwayGame)

            totalDifficulty += difficultyScore

            // Top-6-Teams zählen (Stärke >= 0.7)
            if (opponentStrength >= 0.7) {
                topTeamOpponents += 1
            }

            // Schwere Auswärtsspiele
            if (isAwayGame && opponentStrength >= 0.6) {
                difficultAwayGames += 1
            }
        }

        val averageDifficulty = if (matches.isEmpty) 0.0 else totalDifficulty / Double(matches.count)

        return FixtureAnalysis(averageDifficulty = averageDifficulty, topTeamOpponents = topTeamOpponents, difficultAwayGames = difficultAwayGames, totalMatches = matches.count)
    }

    // MARK: - Team Strength from Match Data
    private fun getTeamStrengthFromMatch(match: EnhancedMatchPerformance, opponentTeamId: String): Double {
        // Versuche Team-Info aus den geladenen Daten zu finden
        val opponentTeamInfo: TeamInfo?

        if (match.team1Id == opponentTeamId) {
            opponentTeamInfo = match.team1Info
        } else if (match.team2Id == opponentTeamId) {
            opponentTeamInfo = match.team2Info
        } else {
            opponentTeamInfo = null
        }

        // Falls Team-Info verfügbar ist, berechne Stärke basierend auf Platzierung
        if (opponentTeamInfo != null) {
            val teamInfo = opponentTeamInfo
            val strength = calculateTeamStrengthFromPlacement(teamInfo.placement)
            print("🎯 Team ${teamInfo.name} (Platz ${teamInfo.placement}) hat Stärke ${String(format = "%.2f", strength)}")
            return strength
        } else {
            // Fallback: mittlere Stärke wenn keine Team-Info verfügbar
            print("⚠️ Keine Team-Info für Team ${opponentTeamId} verfügbar, verwende Fallback-Stärke 0.5")
            return 0.5
        }
    }

    private constructor(selectedGoal: SalesRecommendationView.OptimizationGoal = SalesRecommendationView.OptimizationGoal.balancePositive, recommendedSales: Array<SalesRecommendation> = arrayOf(), selectedSales: Set<String> = setOf(), recommendationTask: Task<Unit>? = null, debounceTask: Task<Unit>? = null, privatep: Nothing? = null) {
        this._selectedGoal = skip.ui.State(selectedGoal)
        this._recommendedSales = skip.ui.State(recommendedSales.sref())
        this._selectedSales = skip.ui.State(selectedSales.sref())
        this._recommendationTask = skip.ui.State(recommendationTask)
        this._debounceTask = skip.ui.State(debounceTask)
    }

    constructor(): this(privatep = null) {
    }

    @androidx.annotation.Keep
    companion object {

        internal fun OptimizationGoal(rawValue: String): SalesRecommendationView.OptimizationGoal? = OptimizationGoal.init(rawValue = rawValue)
    }
}

// MARK: - Sales Recommendation Data Models
internal class SalesRecommendation: Identifiable<UUID> {
    override val id = UUID()
    internal val player: Player
    internal val reason: String
    internal val priority: SalesRecommendation.Priority
    internal val expectedValue: Int
    internal val impact: SalesRecommendation.LineupImpact

    @androidx.annotation.Keep
    internal enum class Priority(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, RawRepresentable<String> {
        high("Hoch"),
        medium("Mittel"),
        low("Niedrig");

        internal val color: Color
            get() {
                when (this) {
                    SalesRecommendation.Priority.high -> return Color.red
                    SalesRecommendation.Priority.medium -> return Color.orange
                    SalesRecommendation.Priority.low -> return Color.green
                }
            }

        @androidx.annotation.Keep
        companion object: CaseIterableCompanion<SalesRecommendation.Priority> {
            fun init(rawValue: String): SalesRecommendation.Priority? {
                return when (rawValue) {
                    "Hoch" -> Priority.high
                    "Mittel" -> Priority.medium
                    "Niedrig" -> Priority.low
                    else -> null
                }
            }

            override val allCases: Array<SalesRecommendation.Priority>
                get() = arrayOf(high, medium, low)
        }
    }

    @androidx.annotation.Keep
    internal enum class LineupImpact(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, RawRepresentable<String> {
        minimal("Minimal"),
        moderate("Moderat"),
        significant("Erheblich");

        internal val color: Color
            get() {
                when (this) {
                    SalesRecommendation.LineupImpact.minimal -> return Color.green
                    SalesRecommendation.LineupImpact.moderate -> return Color.orange
                    SalesRecommendation.LineupImpact.significant -> return Color.red
                }
            }

        @androidx.annotation.Keep
        companion object: CaseIterableCompanion<SalesRecommendation.LineupImpact> {
            fun init(rawValue: String): SalesRecommendation.LineupImpact? {
                return when (rawValue) {
                    "Minimal" -> LineupImpact.minimal
                    "Moderat" -> LineupImpact.moderate
                    "Erheblich" -> LineupImpact.significant
                    else -> null
                }
            }

            override val allCases: Array<SalesRecommendation.LineupImpact>
                get() = arrayOf(minimal, moderate, significant)
        }
    }

    constructor(player: Player, reason: String, priority: SalesRecommendation.Priority, expectedValue: Int, impact: SalesRecommendation.LineupImpact) {
        this.player = player
        this.reason = reason
        this.priority = priority
        this.expectedValue = expectedValue
        this.impact = impact
    }

    @androidx.annotation.Keep
    companion object {

        internal fun Priority(rawValue: String): SalesRecommendation.Priority? = Priority.init(rawValue = rawValue)

        internal fun LineupImpact(rawValue: String): SalesRecommendation.LineupImpact? = LineupImpact.init(rawValue = rawValue)
    }
}

// MARK: - Sales Recommendation Header
internal class SalesRecommendationHeader: View {
    internal val currentBudget: Int
    internal val recommendedSaleValue: Int
    internal val selectedSaleValue: Int

    private val budgetAfterRecommended: Int
        get() = currentBudget + recommendedSaleValue

    private val budgetAfterSelected: Int
        get() = currentBudget + selectedSaleValue

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 16.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Aktuelle Budget-Situation
                    HStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Aktuelles Budget"))
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary).Compose(composectx)
                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendLiteral("€")
                                        str.appendInterpolation(formatValueWithSeparators(currentBudget))
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.headline)
                                        .fontWeight(Font.Weight.bold)
                                        .foregroundColor(if (currentBudget < 0) Color.red else Color.green).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            Spacer().Compose(composectx)

                            if (currentBudget < 0) {
                                VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Text(LocalizedStringKey(stringLiteral = "Benötigt"))
                                            .font(Font.caption)
                                            .foregroundColor(Color.secondary).Compose(composectx)
                                        Text({
                                            val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                            str.appendLiteral("€")
                                            str.appendInterpolation(formatValueWithSeparators(abs(currentBudget)))
                                            LocalizedStringKey(stringInterpolation = str)
                                        }())
                                            .font(Font.headline)
                                            .fontWeight(Font.Weight.bold)
                                            .foregroundColor(Color.red).Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)
                            }
                            ComposeResult.ok
                        }
                    }
                    .padding()
                    .background(if (currentBudget < 0) Color.red.opacity(0.1) else Color.green.opacity(0.1))
                    .cornerRadius(12.0).Compose(composectx)

                    // Empfohlene vs. Ausgewählte Verkäufe
                    if (recommendedSaleValue > 0 || selectedSaleValue > 0) {
                        LazyVGrid(columns = Array(repeating = GridItem(GridItem.Size.flexible()), count = 2), spacing = 12.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                // Empfohlene Verkäufe
                                VStack(spacing = 8.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Text(LocalizedStringKey(stringLiteral = "Empfohlen"))
                                            .font(Font.caption)
                                            .foregroundColor(Color.secondary).Compose(composectx)

                                        VStack(spacing = 4.0) { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                Text({
                                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                    str.appendLiteral("€")
                                                    str.appendInterpolation(formatValueWithSeparators(recommendedSaleValue))
                                                    LocalizedStringKey(stringInterpolation = str)
                                                }())
                                                    .font(Font.headline)
                                                    .fontWeight(Font.Weight.bold)
                                                    .foregroundColor(Color.blue).Compose(composectx)

                                                Text({
                                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                    str.appendLiteral("Budget: €")
                                                    str.appendInterpolation(formatValueWithSeparators(budgetAfterRecommended))
                                                    LocalizedStringKey(stringInterpolation = str)
                                                }())
                                                    .font(Font.caption)
                                                    .foregroundColor(if (budgetAfterRecommended >= 0) Color.green else Color.red).Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }
                                .padding()
                                .background(Color.systemGray6Compat)
                                .cornerRadius(8.0).Compose(composectx)

                                // Ausgewählte Verkäufe
                                VStack(spacing = 8.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Text(LocalizedStringKey(stringLiteral = "Ausgewählt"))
                                            .font(Font.caption)
                                            .foregroundColor(Color.secondary).Compose(composectx)

                                        VStack(spacing = 4.0) { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                Text({
                                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                    str.appendLiteral("€")
                                                    str.appendInterpolation(formatValueWithSeparators(selectedSaleValue))
                                                    LocalizedStringKey(stringInterpolation = str)
                                                }())
                                                    .font(Font.headline)
                                                    .fontWeight(Font.Weight.bold)
                                                    .foregroundColor(Color.orange).Compose(composectx)

                                                if (selectedSaleValue > 0) {
                                                    Text({
                                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                        str.appendLiteral("Budget: €")
                                                        str.appendInterpolation(formatValueWithSeparators(budgetAfterSelected))
                                                        LocalizedStringKey(stringInterpolation = str)
                                                    }())
                                                        .font(Font.caption)
                                                        .foregroundColor(if (budgetAfterSelected >= 0) Color.green else Color.red).Compose(composectx)
                                                } else {
                                                    Text(LocalizedStringKey(stringLiteral = "Keine Auswahl"))
                                                        .font(Font.caption)
                                                        .foregroundColor(Color.secondary).Compose(composectx)
                                                }
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }
                                .padding()
                                .background(Color.systemGray6Compat)
                                .cornerRadius(8.0).Compose(composectx)
                                ComposeResult.ok
                            }
                        }.Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    private fun formatValueWithSeparators(value: Int): String {
        val formatter = NumberFormatter()
        formatter.numberStyle = NumberFormatter.Style.decimal
        formatter.groupingSeparator = "."
        formatter.groupingSize = 3

        val matchtarget_3 = formatter.string(from = NSNumber(value = value))
        if (matchtarget_3 != null) {
            val formattedString = matchtarget_3
            return formattedString
        } else {
            return "${value}"
        }
    }

    constructor(currentBudget: Int, recommendedSaleValue: Int, selectedSaleValue: Int) {
        this.currentBudget = currentBudget
        this.recommendedSaleValue = recommendedSaleValue
        this.selectedSaleValue = selectedSaleValue
    }
}

// MARK: - Sales Recommendation Summary
internal class SalesRecommendationSummary: View {
    internal val recommendations: Array<SalesRecommendation>
    internal val optimizationGoal: SalesRecommendationView.OptimizationGoal

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(alignment = HorizontalAlignment.leading, spacing = 16.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(LocalizedStringKey(stringLiteral = "Empfehlungsübersicht"))
                        .font(Font.title2)
                        .fontWeight(Font.Weight.bold).Compose(composectx)

                    // Strategieerklärung
                    VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text({
                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                str.appendLiteral("Strategie: ")
                                str.appendInterpolation(optimizationGoal.rawValue)
                                LocalizedStringKey(stringInterpolation = str)
                            }())
                                .font(Font.headline)
                                .foregroundColor(Color.blue).Compose(composectx)

                            Text(getStrategyDescription(for_ = optimizationGoal))
                                .font(Font.subheadline)
                                .foregroundColor(Color.secondary).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding()
                    .background(Color.systemBackgroundCompat)
                    .cornerRadius(8.0).Compose(composectx)

                    // Prioritäten-Übersicht
                    val priorityCounts = getPriorityCounts()
                    LazyVGrid(columns = Array(repeating = GridItem(GridItem.Size.flexible()), count = 3), spacing = 12.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            ForEach(SalesRecommendation.Priority.allCases, id = { it }) { priority ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    VStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Image(systemName = getPriorityIcon(priority))
                                                .font(Font.title2)
                                                .foregroundColor(priority.color).Compose(composectx)

                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendInterpolation(priorityCounts[priority] ?: 0)
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.title)
                                                .fontWeight(Font.Weight.bold).Compose(composectx)

                                            Text(priority.rawValue)
                                                .font(Font.caption)
                                                .foregroundColor(Color.secondary).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }
                                    .frame(maxWidth = Double.infinity)
                                    .padding()
                                    .background(Color.systemGray6Compat)
                                    .cornerRadius(8.0).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding()
            .background(Color.systemGray6Compat)
            .cornerRadius(12.0).Compose(composectx)
        }
    }

    private fun getStrategyDescription(for_: SalesRecommendationView.OptimizationGoal): String {
        val goal = for_
        when (goal) {
            SalesRecommendationView.OptimizationGoal.balancePositive -> return "Verkaufe Spieler um das Budget ins Plus zu bringen, dabei werden Spielerleistung und Positionsbesetzung berücksichtigt."
            SalesRecommendationView.OptimizationGoal.maximizeProfit -> return "Verkaufe Spieler mit dem höchsten Gewinn seit dem Kauf, um maximalen Profit zu erzielen."
            SalesRecommendationView.OptimizationGoal.keepBestPlayers -> return "Verkaufe schwächere Spieler zuerst, um die besten Leistungsträger im Team zu behalten."
        }
    }

    private fun getPriorityCounts(): Dictionary<SalesRecommendation.Priority, Int> {
        var counts: Dictionary<SalesRecommendation.Priority, Int> = dictionaryOf()

        for (priority in SalesRecommendation.Priority.allCases.sref()) {
            counts[priority] = recommendations.filter { it -> it.priority == priority }.count
        }

        return counts.sref()
    }

    private fun getPriorityIcon(priority: SalesRecommendation.Priority): String {
        when (priority) {
            SalesRecommendation.Priority.high -> return "exclamationmark.triangle.fill"
            SalesRecommendation.Priority.medium -> return "minus.circle.fill"
            SalesRecommendation.Priority.low -> return "checkmark.circle.fill"
        }
    }

    constructor(recommendations: Array<SalesRecommendation>, optimizationGoal: SalesRecommendationView.OptimizationGoal) {
        this.recommendations = recommendations.sref()
        this.optimizationGoal = optimizationGoal
    }
}

// MARK: - Sales Recommendation Row
internal class SalesRecommendationRow: View, MutableStruct {
    internal val recommendation: SalesRecommendation
    internal val isSelected: Boolean
    internal val onToggle: (Boolean) -> Unit

    // Optional override binding for tests to control sheet presentation
    internal var showingPlayerDetailBinding: Binding<Boolean>? = null
        get() = field.sref({ this.showingPlayerDetailBinding = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    private var internalShowingPlayerDetail: Boolean
        get() = _internalShowingPlayerDetail.wrappedValue
        set(newValue) {
            _internalShowingPlayerDetail.wrappedValue = newValue
        }
    private var _internalShowingPlayerDetail: skip.ui.State<Boolean>
    private val showingPlayerDetail: Binding<Boolean>
        get() = showingPlayerDetailBinding ?: Binding({ _internalShowingPlayerDetail.wrappedValue }, { it -> _internalShowingPlayerDetail.wrappedValue = it })

    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    internal var ligainsiderService: LigainsiderService
        get() = _ligainsiderService.wrappedValue
        set(newValue) {
            _ligainsiderService.wrappedValue = newValue
        }
    internal var _ligainsiderService = skip.ui.Environment<LigainsiderService>()

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            HStack(spacing = 12.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Position Badge
                    PositionBadge(position = recommendation.player.position).Compose(composectx)

                    // Spieler-Info Bereich (klickbar für Details)
                    Button(action = { -> showingPlayerDetail.wrappedValue = true }) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            HStack(spacing = 12.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 6.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            // Spieler Name mit Status
                                            HStack(spacing = 4.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(recommendation.player.fullName)
                                                        .font(Font.headline)
                                                        .fontWeight(Font.Weight.medium).Compose(composectx)

                                                    // Ligainsider Status Icon
                                                    val status = ligainsiderService.getPlayerStatus(firstName = recommendation.player.firstName, lastName = recommendation.player.lastName)
                                                    if (status != LigainsiderStatus.out) {
                                                        Image(systemName = ligainsiderService.getIcon(for_ = status))
                                                            .foregroundColor(ligainsiderService.getColor(for_ = status))
                                                            .font(Font.caption).Compose(composectx)
                                                    }

                                                    // Status-Icons
                                                    if (recommendation.player.status == 2) {
                                                        Image(systemName = "pills.fill")
                                                            .foregroundColor(Color.orange)
                                                            .font(Font.caption).Compose(composectx)
                                                    }
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            // Team
                                            Text(recommendation.player.fullTeamName)
                                                .font(Font.caption)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            // Verkaufsgrund
                                            Text(recommendation.reason)
                                                .font(Font.caption)
                                                .foregroundColor(Color.blue)
                                                .italic().Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    Spacer().Compose(composectx)

                                    // Stats
                                    VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            // Durchschnittspunkte
                                            HStack(spacing = 4.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Image(systemName = "star.fill")
                                                        .font(Font.caption)
                                                        .foregroundColor(Color.orange).Compose(composectx)
                                                    Text(String(format = "%.0f", recommendation.player.averagePoints))
                                                        .font(Font.subheadline)
                                                        .fontWeight(Font.Weight.semibold).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            // Marktwert
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendLiteral("€")
                                                str.appendInterpolation(formatValue(recommendation.player.marketValue))
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.headline)
                                                .fontWeight(Font.Weight.bold)
                                                .foregroundColor(Color.green).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    VStack(spacing = 8.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            // Priorität
                                            HStack(spacing = 4.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Circle()
                                                        .fill(recommendation.priority.color)
                                                        .frame(width = 8.0, height = 8.0).Compose(composectx)
                                                    Text(recommendation.priority.rawValue)
                                                        .font(Font.caption)
                                                        .foregroundColor(recommendation.priority.color).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            // Aufstellungsimpact
                                            HStack(spacing = 4.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Image(systemName = getImpactIcon(recommendation.impact))
                                                        .font(Font.caption2)
                                                        .foregroundColor(recommendation.impact.color).Compose(composectx)
                                                    Text(recommendation.impact.rawValue)
                                                        .font(Font.caption2)
                                                        .foregroundColor(recommendation.impact.color).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    // Toggle (separater Bereich)
                    Toggle(isOn = Binding<Boolean>(get = { -> isSelected }, set = { newValue -> onToggle(newValue) })) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(LocalizedStringKey(stringLiteral = "")).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .frame(width = 50.0, height = 30.0).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding(Edge.Set.vertical, 8.0)
            .sheet2(isPresented = showingPlayerDetail) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    PlayerDetailView(player = recommendation.player)
                        .environmentObject(kickbaseManager)
                        .environmentObject(ligainsiderService).Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedinternalShowingPlayerDetail by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_internalShowingPlayerDetail) }
        _internalShowingPlayerDetail = rememberedinternalShowingPlayerDetail

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!
        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    private fun getImpactIcon(impact: SalesRecommendation.LineupImpact): String {
        when (impact) {
            SalesRecommendation.LineupImpact.minimal -> return "checkmark.circle.fill"
            SalesRecommendation.LineupImpact.moderate -> return "minus.circle.fill"
            SalesRecommendation.LineupImpact.significant -> return "exclamationmark.triangle.fill"
        }
    }

    private constructor(recommendation: SalesRecommendation, isSelected: Boolean, onToggle: (Boolean) -> Unit, showingPlayerDetailBinding: Binding<Boolean>? = null, internalShowingPlayerDetail: Boolean = false, privatep: Nothing? = null) {
        this.recommendation = recommendation
        this.isSelected = isSelected
        this.onToggle = onToggle
        this.showingPlayerDetailBinding = showingPlayerDetailBinding
        this._internalShowingPlayerDetail = skip.ui.State(internalShowingPlayerDetail)
    }

    constructor(recommendation: SalesRecommendation, isSelected: Boolean, onToggle: (Boolean) -> Unit, showingPlayerDetailBinding: Binding<Boolean>? = null): this(recommendation = recommendation, isSelected = isSelected, onToggle = onToggle, showingPlayerDetailBinding = showingPlayerDetailBinding, privatep = null) {
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = SalesRecommendationRow(recommendation, isSelected, onToggle, showingPlayerDetailBinding, internalShowingPlayerDetail)
}

// MARK: - Lineup Optimizer View
internal class LineupOptimizerView: View {
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    internal var ligainsiderService: LigainsiderService
        get() = _ligainsiderService.wrappedValue
        set(newValue) {
            _ligainsiderService.wrappedValue = newValue
        }
    internal var _ligainsiderService = skip.ui.Environment<LigainsiderService>()
    private var selectedOptimization: LineupOptimizerView.OptimizationType
        get() = _selectedOptimization.wrappedValue
        set(newValue) {
            _selectedOptimization.wrappedValue = newValue
        }
    private var _selectedOptimization: skip.ui.State<LineupOptimizerView.OptimizationType>
    private var lineupComparison: LineupComparison?
        get() = _lineupComparison.wrappedValue
        set(newValue) {
            _lineupComparison.wrappedValue = newValue
        }
    private var _lineupComparison: skip.ui.State<LineupComparison?> = skip.ui.State(null)
    private var showOptimalComparison: Boolean
        get() = _showOptimalComparison.wrappedValue
        set(newValue) {
            _showOptimalComparison.wrappedValue = newValue
        }
    private var _showOptimalComparison: skip.ui.State<Boolean>
    private var isGeneratingComparison: Boolean
        get() = _isGeneratingComparison.wrappedValue
        set(newValue) {
            _isGeneratingComparison.wrappedValue = newValue
        }
    private var _isGeneratingComparison: skip.ui.State<Boolean>

    @androidx.annotation.Keep
    internal enum class OptimizationType(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, RawRepresentable<String> {
        averagePoints("Durchschnittspunkte"),
        totalPoints("Gesamtpunkte");

        @androidx.annotation.Keep
        companion object: CaseIterableCompanion<LineupOptimizerView.OptimizationType> {
            fun init(rawValue: String): LineupOptimizerView.OptimizationType? {
                return when (rawValue) {
                    "Durchschnittspunkte" -> OptimizationType.averagePoints
                    "Gesamtpunkte" -> OptimizationType.totalPoints
                    else -> null
                }
            }

            override val allCases: Array<LineupOptimizerView.OptimizationType>
                get() = arrayOf(averagePoints, totalPoints)
        }
    }

    // Verfügbare Formationen
    @androidx.annotation.Keep
    internal enum class Formation(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, RawRepresentable<String> {
        formation442("4-4-2"),
        formation424("4-2-4"),
        formation343("3-4-3"),
        formation433("4-3-3"),
        formation532("5-3-2"),
        formation352("3-5-2"),
        formation541("5-4-1"),
        formation451("4-5-1"),
        formation361("3-6-1"),
        formation523("5-2-3");

        internal val positions: Tuple3<Int, Int, Int>
            get() {
                when (this) {
                    LineupOptimizerView.Formation.formation442 -> return Tuple3(4, 4, 2)
                    LineupOptimizerView.Formation.formation424 -> return Tuple3(4, 2, 4)
                    LineupOptimizerView.Formation.formation343 -> return Tuple3(3, 4, 3)
                    LineupOptimizerView.Formation.formation433 -> return Tuple3(4, 3, 3)
                    LineupOptimizerView.Formation.formation532 -> return Tuple3(5, 3, 2)
                    LineupOptimizerView.Formation.formation352 -> return Tuple3(3, 5, 2)
                    LineupOptimizerView.Formation.formation541 -> return Tuple3(5, 4, 1)
                    LineupOptimizerView.Formation.formation451 -> return Tuple3(4, 5, 1)
                    LineupOptimizerView.Formation.formation361 -> return Tuple3(3, 6, 1)
                    LineupOptimizerView.Formation.formation523 -> return Tuple3(5, 2, 3)
                }
            }

        @androidx.annotation.Keep
        companion object: CaseIterableCompanion<LineupOptimizerView.Formation> {
            fun init(rawValue: String): LineupOptimizerView.Formation? {
                return when (rawValue) {
                    "4-4-2" -> Formation.formation442
                    "4-2-4" -> Formation.formation424
                    "3-4-3" -> Formation.formation343
                    "4-3-3" -> Formation.formation433
                    "5-3-2" -> Formation.formation532
                    "3-5-2" -> Formation.formation352
                    "5-4-1" -> Formation.formation541
                    "4-5-1" -> Formation.formation451
                    "3-6-1" -> Formation.formation361
                    "5-2-3" -> Formation.formation523
                    else -> null
                }
            }

            override val allCases: Array<LineupOptimizerView.Formation>
                get() = arrayOf(formation442, formation424, formation343, formation433, formation532, formation352, formation541, formation451, formation361, formation523)
        }
    }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 0.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Header with optimization type selector
                    VStack(spacing = 16.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(LocalizedStringKey(stringLiteral = "Beste Aufstellung"))
                                .font(Font.title2)
                                .fontWeight(Font.Weight.bold).Compose(composectx)

                            Picker(LocalizedStringKey(stringLiteral = "Optimierung"), selection = Binding({ _selectedOptimization.wrappedValue }, { it -> _selectedOptimization.wrappedValue = it })) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    ForEach(OptimizationType.allCases, id = { it }) { type ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(type.rawValue).tag(type).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .pickerStyle(PickerStyle.segmented).Compose(composectx)

                            // Button für optimale Aufstellung mit Marktspieler
                            Button(action = ::generateOptimalLineupComparison) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    HStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Image(systemName = "sparkles").Compose(composectx)
                                            Text(LocalizedStringKey(stringLiteral = "Mit Marktspieler")).Compose(composectx)
                                            Spacer().Compose(composectx)
                                            if (isGeneratingComparison) {
                                                ProgressView()
                                                    .scaleEffect(0.8).Compose(composectx)
                                            }
                                            ComposeResult.ok
                                        }
                                    }
                                    .font(Font.subheadline)
                                    .fontWeight(Font.Weight.semibold)
                                    .foregroundColor(Color.white)
                                    .padding(Edge.Set.vertical, 10.0)
                                    .padding(Edge.Set.horizontal)
                                    .background(Color.blue)
                                    .cornerRadius(8.0).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .disabled(isGeneratingComparison || kickbaseManager.teamPlayers.isEmpty).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding()
                    .background(Color.systemGray6Compat).Compose(composectx)

                    if (kickbaseManager.teamPlayers.isEmpty) {
                        // Empty state
                        VStack(spacing = 20.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                Spacer().Compose(composectx)

                                Image(systemName = "person.crop.square.fill.and.at.rectangle")
                                    .font(Font.system(size = 60.0))
                                    .foregroundColor(Color.gray).Compose(composectx)

                                Text(LocalizedStringKey(stringLiteral = "Keine Spieler geladen"))
                                    .font(Font.headline)
                                    .foregroundColor(Color.primary).Compose(composectx)

                                Text(LocalizedStringKey(stringLiteral = "Lade dein Team, um die beste Aufstellung zu sehen"))
                                    .font(Font.subheadline)
                                    .foregroundColor(Color.secondary)
                                    .multilineTextAlignment(TextAlignment.center)
                                    .padding(Edge.Set.horizontal).Compose(composectx)

                                Button(LocalizedStringKey(stringLiteral = "Team laden")) { ->
                                    Task(isMainActor = true) { ->
                                        kickbaseManager.selectedLeague?.let { league ->
                                            kickbaseManager.loadTeamPlayers(for_ = league)
                                        }
                                    }
                                }
                                .buttonStyle(ButtonStyle.borderedProminent).Compose(composectx)

                                Spacer().Compose(composectx)
                                ComposeResult.ok
                            }
                        }.Compose(composectx)
                    } else {
                        // Lineup display
                        ScrollView { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                VStack(spacing = 20.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        // Stats header
                                        val optimalResult = getBestPossibleLineup()
                                        OptimalLineupStatsView(lineup = optimalResult.lineup, formation = optimalResult.formation, optimizationType = selectedOptimization).Compose(composectx)

                                        // Formation display
                                        OptimalLineupFormationView(lineup = optimalResult.lineup, formation = optimalResult.formation).Compose(composectx)

                                        // Reserve players section
                                        ReservePlayersView(allPlayers = kickbaseManager.teamPlayers.filter { it -> it.status != 1 && it.status != 4 && it.status != 8 }, startingLineup = optimalResult.lineup, optimizationType = selectedOptimization).Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }
                                .padding().Compose(composectx)
                                ComposeResult.ok
                            }
                        }.Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }
            .refreshable { -> MainActor.run {
                kickbaseManager.selectedLeague?.let { league ->
                    kickbaseManager.loadTeamPlayers(for_ = league)
                }
            } }
            .sheet(item = Binding({ _lineupComparison.wrappedValue }, { it -> _lineupComparison.wrappedValue = it })) { comparison ->
                ComposeBuilder { composectx: ComposeContext ->
                    LineupComparisonView(comparison = comparison)
                        .environmentObject(kickbaseManager)
                        .environmentObject(ligainsiderService).Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedselectedOptimization by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<LineupOptimizerView.OptimizationType>, Any>) { mutableStateOf(_selectedOptimization) }
        _selectedOptimization = rememberedselectedOptimization

        val rememberedlineupComparison by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<LineupComparison?>, Any>) { mutableStateOf(_lineupComparison) }
        _lineupComparison = rememberedlineupComparison

        val rememberedshowOptimalComparison by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_showOptimalComparison) }
        _showOptimalComparison = rememberedshowOptimalComparison

        val rememberedisGeneratingComparison by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_isGeneratingComparison) }
        _isGeneratingComparison = rememberedisGeneratingComparison

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!
        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    private fun generateOptimalLineupComparison() {
        Task l@{ ->
            var deferaction_0: (() -> Unit)? = null
            try {
                isGeneratingComparison = true
                deferaction_0 = {
                    isGeneratingComparison = false
                }

                run {
                    val league_0 = kickbaseManager.selectedLeague
                    if (league_0 == null) {
                        return@l
                    }

                    // Reuse shared service to keep caches and avoid repeated instantiation
                    val recommendationService = kickbaseManager.playerRecommendationService

                    // Finde die beste Formation basierend auf verfügbaren Spielern
                    val bestFormation = findBestFormation()

                    val comparison = recommendationService.generateOptimalLineupComparison(for_ = league_0, teamPlayers = kickbaseManager.mainactor { it.teamPlayers }, marketPlayers = kickbaseManager.mainactor { it.marketPlayers }, formation = bestFormation)

                    MainActor.run { -> this.lineupComparison = comparison }
                }
            } finally {
                deferaction_0?.invoke()
            }
        }
    }

    private fun findBestFormation(): Array<Int> {
        val availablePlayers = kickbaseManager.teamPlayers.filter { it -> it.status != 1 && it.status != 4 && it.status != 8 }

        val goalkeepers = availablePlayers.filter { it -> it.position == 1 }
        val defenders = availablePlayers.filter { it -> it.position == 2 }
        val midfielders = availablePlayers.filter { it -> it.position == 3 }
        val forwards = availablePlayers.filter { it -> it.position == 4 }
        if (goalkeepers.isEmpty) {
            return arrayOf(1, 4, 4, 2) // Fallback
        }

        // Finde die beste mögliche Formation basierend auf verfügbaren Spielern
        var bestFormation: Array<Int>? = null
        var bestScore: Double = 0.0

        for (formation in LineupOptimizerView.Formation.allCases.sref()) {
            val positions = formation.positions

            // Prüfe ob genügend Spieler für diese Formation vorhanden sind
            if (defenders.count >= positions.defenders && midfielders.count >= positions.midfielders && forwards.count >= positions.forwards) {
                // Berechne Score für diese Formation
                val score = Double(defenders.count + midfielders.count + forwards.count)

                if (score > bestScore) {
                    bestScore = score
                    bestFormation = formationToArray(formation)
                }
            }
        }

        return (bestFormation ?: arrayOf(1, 4, 4, 2)).sref() // Fallback
    }

    private fun formationToArray(formation: LineupOptimizerView.Formation): Array<Int> {
        val positions = formation.positions
        return arrayOf(1, positions.defenders, positions.midfielders, positions.forwards)
    }

    private fun getBestPossibleLineup(): Tuple2<OptimalLineup, LineupOptimizerView.Formation> {
        val availablePlayers = kickbaseManager.teamPlayers.filter { it ->
            it.status != 1 && it.status != 4 && it.status != 8 // Ausschluss verletzter Spieler und Spieler im Aufbautraining
        } // Ausschluss verletzter Spieler und Spieler im Aufbautraining

        // Gruppiere verfügbare Spieler nach Position
        val goalkeepers = availablePlayers.filter { it -> it.position == 1 }
        val defenders = availablePlayers.filter { it -> it.position == 2 }
        val midfielders = availablePlayers.filter { it -> it.position == 3 }
        val forwards = availablePlayers.filter { it -> it.position == 4 }
        if (goalkeepers.isEmpty) {
            // Fallback: Verwende alle verfügbaren Spieler
            return createFallbackLineup(from = availablePlayers)
        }

        // Finde die beste mögliche Formation basierend auf verfügbaren Spielern
        var bestResult: Tuple3<OptimalLineup, LineupOptimizerView.Formation, Double>? = null

        for (formation in Formation.allCases.sref()) {
            val positions = formation.positions

            // Prüfe ob genügend Spieler für diese Formation vorhanden sind
            if (defenders.count >= positions.defenders && midfielders.count >= positions.midfielders && forwards.count >= positions.forwards) {

                val lineup = calculateOptimalLineupForFormation(formation = formation, goalkeepers = goalkeepers, defenders = defenders, midfielders = midfielders, forwards = forwards)

                val score = calculateLineupScore(lineup)

                if (bestResult == null || score > bestResult!!.score) {
                    bestResult = Tuple3(lineup, formation, score)
                }
            }
        }

        // Falls keine komplette Formation möglich ist, verwende bestmögliche Aufstellung
        if (bestResult != null) {
            val best = bestResult
            return Tuple2(best.lineup, best.formation)
        } else {
            return createBestPossibleLineup(goalkeepers = goalkeepers, defenders = defenders, midfielders = midfielders, forwards = forwards)
        }
    }

    private fun calculateOptimalLineupForFormation(formation: LineupOptimizerView.Formation, goalkeepers: Array<Player>, defenders: Array<Player>, midfielders: Array<Player>, forwards: Array<Player>): OptimalLineup {
        val positions = formation.positions

        // Sortiere Spieler basierend auf gewähltem Kriterium
        val sortedGK: Array<Player>
        val sortedDF: Array<Player>
        val sortedMF: Array<Player>
        val sortedFW: Array<Player>

        when (selectedOptimization) {
            LineupOptimizerView.OptimizationType.averagePoints -> {
                sortedGK = goalkeepers.sorted { it, it_1 -> it.averagePoints > it_1.averagePoints }
                sortedDF = defenders.sorted { it, it_1 -> it.averagePoints > it_1.averagePoints }
                sortedMF = midfielders.sorted { it, it_1 -> it.averagePoints > it_1.averagePoints }
                sortedFW = forwards.sorted { it, it_1 -> it.averagePoints > it_1.averagePoints }
            }
            LineupOptimizerView.OptimizationType.totalPoints -> {
                sortedGK = goalkeepers.sorted { it, it_1 -> it.totalPoints > it_1.totalPoints }
                sortedDF = defenders.sorted { it, it_1 -> it.totalPoints > it_1.totalPoints }
                sortedMF = midfielders.sorted { it, it_1 -> it.totalPoints > it_1.totalPoints }
                sortedFW = forwards.sorted { it, it_1 -> it.totalPoints > it_1.totalPoints }
            }
        }

        return OptimalLineup(goalkeeper = sortedGK.first, defenders = Array(sortedDF.prefix(positions.defenders)), midfielders = Array(sortedMF.prefix(positions.midfielders)), forwards = Array(sortedFW.prefix(positions.forwards)))
    }

    private fun calculateLineupScore(lineup: OptimalLineup): Double {
        when (selectedOptimization) {
            LineupOptimizerView.OptimizationType.averagePoints -> return lineup.averagePoints
            LineupOptimizerView.OptimizationType.totalPoints -> return Double(lineup.totalPoints)
        }
    }

    private fun createBestPossibleLineup(goalkeepers: Array<Player>, defenders: Array<Player>, midfielders: Array<Player>, forwards: Array<Player>): Tuple2<OptimalLineup, LineupOptimizerView.Formation> {
        // Verwende so viele Spieler wie möglich, beginnend mit den besten
        val maxDefenders = min(defenders.count, 5) // Max 5 Verteidiger
        val maxMidfielders = min(midfielders.count, 6) // Max 6 Mittelfeldspieler
        val maxForwards = min(forwards.count, 4) // Max 4 Stürmer

        // Finde eine passende Formation
        val customFormation: LineupOptimizerView.Formation
        if (maxDefenders >= 4 && maxMidfielders >= 4 && maxForwards >= 2) {
            customFormation = LineupOptimizerView.Formation.formation442
        } else if (maxDefenders >= 4 && maxMidfielders >= 3 && maxForwards >= 3) {
            customFormation = LineupOptimizerView.Formation.formation433
        } else if (maxDefenders >= 3 && maxMidfielders >= 4 && maxForwards >= 3) {
            customFormation = LineupOptimizerView.Formation.formation343
        } else if (maxDefenders >= 5 && maxMidfielders >= 3 && maxForwards >= 2) {
            customFormation = LineupOptimizerView.Formation.formation532
        } else if (maxDefenders >= 4 && maxMidfielders >= 5 && maxForwards >= 1) {
            customFormation = LineupOptimizerView.Formation.formation451
        } else {
            // Fallback auf 4-3-3 mit verfügbaren Spielern
            customFormation = LineupOptimizerView.Formation.formation433
        }

        val lineup = calculateOptimalLineupForFormation(formation = customFormation, goalkeepers = goalkeepers, defenders = defenders, midfielders = midfielders, forwards = forwards)

        return Tuple2(lineup, customFormation)
    }

    private fun createFallbackLineup(from: Array<Player>): Tuple2<OptimalLineup, LineupOptimizerView.Formation> {
        val players = from
        // Notfall: Wenn kein Torwart verfügbar ist, verwende den besten verfügbaren Spieler
        val sortedPlayers: Array<Player>

        when (selectedOptimization) {
            LineupOptimizerView.OptimizationType.averagePoints -> {
                sortedPlayers = players.sorted { it, it_1 -> it.averagePoints > it_1.averagePoints }
            }
            LineupOptimizerView.OptimizationType.totalPoints -> {
                sortedPlayers = players.sorted { it, it_1 -> it.totalPoints > it_1.totalPoints }
            }
        }

        val lineup = OptimalLineup(goalkeeper = sortedPlayers.first, defenders = Array(sortedPlayers.dropFirst().prefix(4)), midfielders = Array(sortedPlayers.dropFirst(8).prefix(3)), forwards = Array(sortedPlayers.dropFirst(11).prefix(3)))

        return Tuple2(lineup, LineupOptimizerView.Formation.formation433)
    }

    private constructor(selectedOptimization: LineupOptimizerView.OptimizationType = LineupOptimizerView.OptimizationType.averagePoints, lineupComparison: LineupComparison? = null, showOptimalComparison: Boolean = false, isGeneratingComparison: Boolean = false, privatep: Nothing? = null) {
        this._selectedOptimization = skip.ui.State(selectedOptimization)
        this._lineupComparison = skip.ui.State(lineupComparison)
        this._showOptimalComparison = skip.ui.State(showOptimalComparison)
        this._isGeneratingComparison = skip.ui.State(isGeneratingComparison)
    }

    constructor(): this(privatep = null) {
    }

    @androidx.annotation.Keep
    companion object {

        internal fun OptimizationType(rawValue: String): LineupOptimizerView.OptimizationType? = OptimizationType.init(rawValue = rawValue)

        internal fun Formation(rawValue: String): LineupOptimizerView.Formation? = Formation.init(rawValue = rawValue)
    }
}

// MARK: - Optimal Lineup Data Structure
internal class OptimalLineup {
    internal val goalkeeper: Player?
    internal val defenders: Array<Player>
    internal val midfielders: Array<Player>
    internal val forwards: Array<Player>

    internal val allPlayers: Array<Player>
        get() {
            var players: Array<Player> = arrayOf()
            goalkeeper?.let { gk ->
                players.append(gk)
            }
            players.append(contentsOf = defenders)
            players.append(contentsOf = midfielders)
            players.append(contentsOf = forwards)
            return players
        }

    internal val totalPoints: Int
        get() {
            return allPlayers.reduce(initialResult = 0) { it, it_1 -> it + it_1.totalPoints }
        }

    internal val averagePoints: Double
        get() {
            val total = allPlayers.reduce(initialResult = 0.0) { it, it_1 -> it + it_1.averagePoints }
            return if (allPlayers.isEmpty) 0.0 else total / Double(allPlayers.count)
        }

    internal val totalMarketValue: Int
        get() {
            return allPlayers.reduce(initialResult = 0) { it, it_1 -> it + it_1.marketValue }
        }

    constructor(goalkeeper: Player? = null, defenders: Array<Player>, midfielders: Array<Player>, forwards: Array<Player>) {
        this.goalkeeper = goalkeeper
        this.defenders = defenders.sref()
        this.midfielders = midfielders.sref()
        this.forwards = forwards.sref()
    }
}

// MARK: - Optimal Lineup Stats View
internal class OptimalLineupStatsView: View {
    internal val lineup: OptimalLineup
    internal val formation: LineupOptimizerView.Formation
    internal val optimizationType: LineupOptimizerView.OptimizationType

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 16.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(LocalizedStringKey(stringLiteral = "Aufstellungs-Statistiken"))
                        .font(Font.headline)
                        .fontWeight(Font.Weight.bold).Compose(composectx)

                    // Formation info
                    HStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Image(systemName = "rectangle.3.group")
                                .foregroundColor(Color.blue).Compose(composectx)
                            Text({
                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                str.appendLiteral("Formation: ")
                                str.appendInterpolation(formation.rawValue)
                                LocalizedStringKey(stringInterpolation = str)
                            }())
                                .font(Font.subheadline)
                                .fontWeight(Font.Weight.medium).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding(Edge.Set.bottom, 8.0).Compose(composectx)

                    LazyVGrid(columns = Array(repeating = GridItem(GridItem.Size.flexible()), count = 2), spacing = 12.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            StatCard(title = "Gesamtpunkte", value = "${lineup.totalPoints}", icon = "star.fill", color = Color.orange).Compose(composectx)
                            StatCard(title = "Ø Punkte", value = String(format = "%.0f", lineup.averagePoints), icon = "chart.line.uptrend.xyaxis", color = Color.blue).Compose(composectx)
                            StatCard(title = "Teamwert", value = "€${formatValue(lineup.totalMarketValue)}", icon = "eurosign.circle.fill", color = Color.green).Compose(composectx)
                            StatCard(title = "Spieler", value = "${lineup.allPlayers.count}/11", icon = "person.crop.square.fill.and.at.rectangle", color = Color.purple).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    // Warnung wenn nicht genügend Spieler
                    if (lineup.allPlayers.count < 11) {
                        HStack { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                Image(systemName = "exclamationmark.triangle.fill")
                                    .foregroundColor(Color.orange).Compose(composectx)
                                Text(LocalizedStringKey(stringLiteral = "Nicht genügend Spieler für eine komplette Aufstellung"))
                                    .font(Font.caption)
                                    .foregroundColor(Color.secondary).Compose(composectx)
                                ComposeResult.ok
                            }
                        }
                        .padding(Edge.Set.top, 8.0).Compose(composectx)
                    }

                    // Kein Torwart Warnung
                    if (lineup.goalkeeper == null) {
                        HStack { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                Image(systemName = "exclamationmark.circle.fill")
                                    .foregroundColor(Color.red).Compose(composectx)
                                Text(LocalizedStringKey(stringLiteral = "Kein Torwart verfügbar - bester Spieler als Ersatz eingesetzt"))
                                    .font(Font.caption)
                                    .foregroundColor(Color.red).Compose(composectx)
                                ComposeResult.ok
                            }
                        }
                        .padding(Edge.Set.top, 4.0).Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }
            .padding()
            .background(Color.systemGray6Compat)
            .cornerRadius(12.0).Compose(composectx)
        }
    }

    constructor(lineup: OptimalLineup, formation: LineupOptimizerView.Formation, optimizationType: LineupOptimizerView.OptimizationType) {
        this.lineup = lineup
        this.formation = formation
        this.optimizationType = optimizationType
    }
}

// MARK: - Optimal Lineup Formation View
internal class OptimalLineupFormationView: View {
    internal val lineup: OptimalLineup
    internal val formation: LineupOptimizerView.Formation

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 20.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text({
                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                        str.appendLiteral("Formation (")
                        str.appendInterpolation(formation.rawValue)
                        str.appendLiteral(")")
                        LocalizedStringKey(stringInterpolation = str)
                    }())
                        .font(Font.headline)
                        .fontWeight(Font.Weight.bold).Compose(composectx)

                    VStack(spacing = 25.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Forwards
                            if (!lineup.forwards.isEmpty) {
                                HStack(spacing = 8.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        ForEach(lineup.forwards, id = { it.id }) { player ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                LineupPlayerCard(player = player).Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)
                            }

                            // Midfielders
                            if (!lineup.midfielders.isEmpty) {
                                HStack(spacing = 6.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        ForEach(lineup.midfielders, id = { it.id }) { player ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                LineupPlayerCard(player = player).Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)
                            }

                            // Defenders
                            if (!lineup.defenders.isEmpty) {
                                HStack(spacing = 8.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        ForEach(lineup.defenders, id = { it.id }) { player ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                LineupPlayerCard(player = player).Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)
                            }

                            // Goalkeeper
                            lineup.goalkeeper?.let { goalkeeper ->
                                LineupPlayerCard(player = goalkeeper).Compose(composectx)
                            }
                            ComposeResult.ok
                        }
                    }
                    .padding()
                    .background(LinearGradient(gradient = Gradient(colors = arrayOf(Color.green.opacity(0.3), Color.green.opacity(0.1))), startPoint = UnitPoint.top, endPoint = UnitPoint.bottom))
                    .cornerRadius(12.0).Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    constructor(lineup: OptimalLineup, formation: LineupOptimizerView.Formation) {
        this.lineup = lineup
        this.formation = formation
    }
}

// MARK: - Lineup Player Card
internal class LineupPlayerCard: View {
    internal val player: Player
    private var showingPlayerDetail: Boolean
        get() = _showingPlayerDetail.wrappedValue
        set(newValue) {
            _showingPlayerDetail.wrappedValue = newValue
        }
    private var _showingPlayerDetail: skip.ui.State<Boolean>
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    internal var ligainsiderService: LigainsiderService
        get() = _ligainsiderService.wrappedValue
        set(newValue) {
            _ligainsiderService.wrappedValue = newValue
        }
    internal var _ligainsiderService = skip.ui.Environment<LigainsiderService>()

    // Plattformspezifische Größen
    private val cardSize: Tuple2<Double, Double>
        get() = Tuple2(60.0, 80.0)

    private class CardFontSizes {
        internal val firstName: Int
        internal val lastName: Int
        internal val avgPoints: Int
        internal val totalPoints: Int
        internal val status: Int

        constructor(firstName: Int, lastName: Int, avgPoints: Int, totalPoints: Int, status: Int) {
            this.firstName = firstName
            this.lastName = lastName
            this.avgPoints = avgPoints
            this.totalPoints = totalPoints
            this.status = status
        }
    }

    private val fontSizes: LineupPlayerCard.CardFontSizes
        get() = CardFontSizes(firstName = 11, lastName = 13, avgPoints = 12, totalPoints = 9, status = 10)

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            Button(action = { -> showingPlayerDetail = true }) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    VStack(spacing = 5.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            VStack(spacing = 2.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(player.firstName)
                                        .font(Font.system(size = Double(fontSizes.firstName), weight = Font.Weight.medium))
                                        .lineLimit(1)
                                        .minimumScaleFactor(0.7).Compose(composectx)
                                    Text(player.lastName)
                                        .font(Font.system(size = Double(fontSizes.lastName), weight = Font.Weight.bold))
                                        .lineLimit(1)
                                        .minimumScaleFactor(0.7).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .foregroundColor(Color.primary).Compose(composectx)

                            // Points - größer und prominenter
                            VStack(spacing = 1.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(String(format = "%.0f", player.averagePoints))
                                        .font(Font.system(size = Double(fontSizes.avgPoints), weight = Font.Weight.bold))
                                        .foregroundColor(Color.orange).Compose(composectx)
                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendInterpolation(player.totalPoints)
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.system(size = Double(fontSizes.totalPoints)))
                                        .foregroundColor(Color.secondary).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            // Status indicator
                            HStack(spacing = 2.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    if (player.status == 2) {
                                        Image(systemName = "pills.fill")
                                            .foregroundColor(Color.orange)
                                            .font(Font.system(size = Double(fontSizes.status))).Compose(composectx)
                                    }

                                    // Ligainsider Icon (wenn verfügbar)
                                    val status = ligainsiderService.getPlayerStatus(firstName = player.firstName, lastName = player.lastName)
                                    if (status != LigainsiderStatus.out) {
                                        Image(systemName = ligainsiderService.getIcon(for_ = status))
                                            .foregroundColor(ligainsiderService.getColor(for_ = status))
                                            .font(Font.system(size = Double(fontSizes.status))).Compose(composectx)
                                    }
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .frame(width = cardSize.width, height = cardSize.height)
                    .background(Color.systemBackgroundCompat)
                    .cornerRadius(10.0)
                    .shadow(radius = 1.0).Compose(composectx) // Stärkerer Schatten
                    ComposeResult.ok
                }
            }
            .sheet(isPresented = Binding({ _showingPlayerDetail.wrappedValue }, { it -> _showingPlayerDetail.wrappedValue = it })) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    PlayerDetailView(player = player).Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedshowingPlayerDetail by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_showingPlayerDetail) }
        _showingPlayerDetail = rememberedshowingPlayerDetail

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!
        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    private constructor(player: Player, showingPlayerDetail: Boolean = false, privatep: Nothing? = null) {
        this.player = player
        this._showingPlayerDetail = skip.ui.State(showingPlayerDetail)
    }

    constructor(player: Player): this(player = player, privatep = null) {
    }
}

// MARK: - Reserve Players View
internal class ReservePlayersView: View {
    internal val allPlayers: Array<Player>
    internal val startingLineup: OptimalLineup
    internal val optimizationType: LineupOptimizerView.OptimizationType

    private val reservePlayers: Array<Player>
        get() {
            val sortedReserve = allPlayers.filter { player ->
                !startingLineup.allPlayers.contains(where = { it -> it.id == player.id })
            }

            // Sortiere nach gewähltem Kriterium
            when (optimizationType) {
                LineupOptimizerView.OptimizationType.averagePoints -> {
                    return sortedReserve.sorted { it, it_1 -> it.averagePoints > it_1.averagePoints }
                }
                LineupOptimizerView.OptimizationType.totalPoints -> {
                    return sortedReserve.sorted { it, it_1 -> it.totalPoints > it_1.totalPoints }
                }
            }
        }

    // Gruppiere Reservespieler nach Position
    private val reservePlayersByPosition: Array<Tuple2<String, Array<Player>>>
        get() {
            var grouped: Dictionary<Int, Array<Player>> = dictionaryOf()
            for (player in reservePlayers.sref()) {
                var list = (grouped[player.position] ?: arrayOf()).sref()
                list.append(player)
                grouped[player.position] = list.sref()
            }

            return arrayOf(
                Tuple2("Torhüter", (grouped[1] ?: arrayOf()).sref()),
                Tuple2("Abwehr", (grouped[2] ?: arrayOf()).sref()),
                Tuple2("Mittelfeld", (grouped[3] ?: arrayOf()).sref()),
                Tuple2("Sturm", (grouped[4] ?: arrayOf()).sref())
            ).filter { ( it, it_1) -> !it_1.isEmpty }
        }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(alignment = HorizontalAlignment.leading, spacing = 16.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Header mit Statistiken
                    HStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Reservebank"))
                                        .font(Font.title2)
                                        .fontWeight(Font.Weight.bold).Compose(composectx)

                                    Text(LocalizedStringKey(stringLiteral = "Beste verfügbare Alternativen"))
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            Spacer().Compose(composectx)

                            VStack(alignment = HorizontalAlignment.trailing, spacing = 2.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendInterpolation(reservePlayers.count)
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.title2)
                                        .fontWeight(Font.Weight.bold)
                                        .foregroundColor(Color.blue).Compose(composectx)
                                    Text(LocalizedStringKey(stringLiteral = "Spieler"))
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    if (reservePlayers.isEmpty) {
                        // Empty state
                        VStack(spacing = 12.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                HStack { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Image(systemName = "checkmark.circle.fill")
                                            .foregroundColor(Color.green)
                                            .font(Font.title2).Compose(composectx)

                                        VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                Text(LocalizedStringKey(stringLiteral = "Optimale Nutzung!"))
                                                    .font(Font.headline)
                                                    .foregroundColor(Color.green).Compose(composectx)
                                                Text(LocalizedStringKey(stringLiteral = "Alle verfügbaren Spieler sind in der Startaufstellung."))
                                                    .font(Font.subheadline)
                                                    .foregroundColor(Color.secondary).Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)

                                // Verletzt Spieler Info falls vorhanden
                                val injuredPlayers = allPlayers.filter { it -> it.status == 1 }
                                if (!injuredPlayers.isEmpty) {
                                    HStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Image(systemName = "cross.circle.fill")
                                                .foregroundColor(Color.red).Compose(composectx)
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendInterpolation(injuredPlayers.count)
                                                str.appendLiteral(" verletzte Spieler nicht berücksichtigt")
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.caption)
                                                .foregroundColor(Color.secondary).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                }
                                ComposeResult.ok
                            }
                        }
                        .padding(Edge.Set.vertical, 8.0).Compose(composectx)
                    } else {
                        // Reserve Statistiken
                        ReserveBenchStatsView(reservePlayers = reservePlayers, optimizationType = optimizationType).Compose(composectx)

                        // Positionsweise Gruppierung
                        ForEach(reservePlayersByPosition, id = { it.element0 }) { (positionName, players) ->
                            ComposeBuilder { composectx: ComposeContext ->
                                VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        HStack { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                Text(positionName)
                                                    .font(Font.headline)
                                                    .foregroundColor(Color.primary).Compose(composectx)

                                                Spacer().Compose(composectx)

                                                Text({
                                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                    str.appendLiteral("(")
                                                    str.appendInterpolation(players.count)
                                                    str.appendLiteral(")")
                                                    LocalizedStringKey(stringInterpolation = str)
                                                }())
                                                    .font(Font.caption)
                                                    .foregroundColor(Color.secondary).Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)

                                        LazyVGrid(columns = Array(repeating = GridItem(GridItem.Size.flexible()), count = 2), spacing = 8.0) { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                ForEach(players, id = { it.id }) { player ->
                                                    ComposeBuilder { composectx: ComposeContext ->
                                                        ReservePlayerRow(player = player, showPosition = false).Compose(composectx)
                                                        ComposeResult.ok
                                                    }
                                                }.Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }
                                .padding(Edge.Set.vertical, 4.0).Compose(composectx)
                                ComposeResult.ok
                            }
                        }.Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }
            .padding()
            .background(Color.systemGray6Compat)
            .cornerRadius(12.0).Compose(composectx)
        }
    }

    constructor(allPlayers: Array<Player>, startingLineup: OptimalLineup, optimizationType: LineupOptimizerView.OptimizationType) {
        this.allPlayers = allPlayers.sref()
        this.startingLineup = startingLineup
        this.optimizationType = optimizationType
    }
}

// MARK: - Reserve Bench Stats
internal class ReserveBenchStatsView: View {
    internal val reservePlayers: Array<Player>
    internal val optimizationType: LineupOptimizerView.OptimizationType

    private val benchStats: Tuple4<Int, Double, Player?, Int>
        get() {
            val totalPoints = reservePlayers.reduce(initialResult = 0) { it, it_1 -> it + it_1.totalPoints }
            val averagePoints = if (reservePlayers.isEmpty) 0.0 else Double(totalPoints) / Double(reservePlayers.count)

            val bestPlayer: Player?
            when (optimizationType) {
                LineupOptimizerView.OptimizationType.averagePoints -> {
                    bestPlayer = reservePlayers.max(by = { it, it_1 -> it.averagePoints < it_1.averagePoints })
                }
                LineupOptimizerView.OptimizationType.totalPoints -> {
                    bestPlayer = reservePlayers.max(by = { it, it_1 -> it.totalPoints < it_1.totalPoints })
                }
            }

            val totalValue = reservePlayers.reduce(initialResult = 0) { it, it_1 -> it + it_1.marketValue }

            return Tuple4(totalPoints, averagePoints, bestPlayer, totalValue)
        }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            val stats = benchStats

            VStack(spacing = 12.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(LocalizedStringKey(stringLiteral = "Reservebank-Statistiken"))
                        .font(Font.subheadline)
                        .fontWeight(Font.Weight.semibold).Compose(composectx)

                    LazyVGrid(columns = Array(repeating = GridItem(GridItem.Size.flexible()), count = 2), spacing = 8.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            BenchStatCard(title = "Gesamt\npunkte", value = "${stats.totalPoints}", icon = "star.fill", color = Color.orange).Compose(composectx)
                            BenchStatCard(title = "Ø Punkte", value = String(format = "%.0f", stats.averagePoints), icon = "chart.line.uptrend.xyaxis", color = Color.blue).Compose(composectx)
                            BenchStatCard(title = "Bank\nwert", value = "€${formatValue(stats.totalValue)}", icon = "eurosign.circle.fill", color = Color.green).Compose(composectx)
                            stats.bestPlayer?.let { bestPlayer ->
                                BenchStatCard(title = "Top\nSpieler", value = bestPlayer.lastName, icon = "crown.fill", color = Color.yellow).Compose(composectx)
                            }
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding()
            .background(Color.systemBackgroundCompat)
            .cornerRadius(8.0).Compose(composectx)
            ComposeResult.ok
        }
    }

    constructor(reservePlayers: Array<Player>, optimizationType: LineupOptimizerView.OptimizationType) {
        this.reservePlayers = reservePlayers.sref()
        this.optimizationType = optimizationType
    }
}

// MARK: - Bench Stat Card
internal class BenchStatCard: View {
    internal val title: String
    internal val value: String
    internal val icon: String
    internal val color: Color

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 8.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Image(systemName = icon)
                        .font(Font.title2)
                        .foregroundColor(color).Compose(composectx)

                    Text(value)
                        .font(Font.title)
                        .fontWeight(Font.Weight.bold).Compose(composectx)

                    Text(title)
                        .font(Font.caption)
                        .foregroundColor(Color.secondary)
                        .multilineTextAlignment(TextAlignment.center)
                        .lineLimit(2).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .frame(height = 100.0)
            .frame(maxWidth = Double.infinity)
            .background(Color.systemBackgroundCompat)
            .cornerRadius(8.0).Compose(composectx)
        }
    }

    constructor(title: String, value: String, icon: String, color: Color) {
        this.title = title
        this.value = value
        this.icon = icon
        this.color = color
    }
}

// MARK: - Reserve Player Row
internal class ReservePlayerRow: View {
    internal val player: Player
    internal val showPosition: Boolean
    private var showingPlayerDetail: Boolean
        get() = _showingPlayerDetail.wrappedValue
        set(newValue) {
            _showingPlayerDetail.wrappedValue = newValue
        }
    private var _showingPlayerDetail: skip.ui.State<Boolean> = skip.ui.State(false)

    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    internal var ligainsiderService: LigainsiderService
        get() = _ligainsiderService.wrappedValue
        set(newValue) {
            _ligainsiderService.wrappedValue = newValue
        }
    internal var _ligainsiderService = skip.ui.Environment<LigainsiderService>()

    internal constructor(player: Player, showPosition: Boolean = true) {
        this.player = player
        this.showPosition = showPosition
    }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            Button(action = { -> showingPlayerDetail = true }) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    HStack(spacing = 8.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Position Badge - nur anzeigen wenn showPosition true ist
                            if (showPosition) {
                                Text(positionAbbreviation(player.position))
                                    .font(Font.caption2)
                                    .fontWeight(Font.Weight.bold)
                                    .foregroundColor(Color.white)
                                    .frame(width = 20.0, height = 20.0)
                                    .background(positionColor(player.position))
                                    .clipShape(Circle()).Compose(composectx)
                            }

                            // Player Info
                            VStack(alignment = HorizontalAlignment.leading, spacing = 1.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    HStack(spacing = 3.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(player.fullName)
                                                .font(Font.caption)
                                                .fontWeight(Font.Weight.medium)
                                                .lineLimit(1).Compose(composectx)

                                            // Ligainsider Status Icon
                                            val status = ligainsiderService.getPlayerStatus(firstName = player.firstName, lastName = player.lastName)
                                            if (status != LigainsiderStatus.out) {
                                                Image(systemName = ligainsiderService.getIcon(for_ = status))
                                                    .foregroundColor(ligainsiderService.getColor(for_ = status))
                                                    .font(Font.system(size = 8.0)).Compose(composectx)
                                            }

                                            // Status indicator
                                            if (player.status == 2) {
                                                Image(systemName = "pills.fill")
                                                    .foregroundColor(Color.orange)
                                                    .font(Font.system(size = 6.0)).Compose(composectx)
                                            }
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    Text(player.fullTeamName)
                                        .font(Font.caption2)
                                        .foregroundColor(Color.secondary)
                                        .lineLimit(1).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            Spacer().Compose(composectx)

                            // Points und Wert
                            VStack(alignment = HorizontalAlignment.trailing, spacing = 1.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(String(format = "%.0f", player.averagePoints))
                                        .font(Font.caption)
                                        .fontWeight(Font.Weight.bold)
                                        .foregroundColor(Color.orange).Compose(composectx)

                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendInterpolation(player.totalPoints)
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.caption2)
                                        .foregroundColor(Color.secondary).Compose(composectx)

                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendLiteral("€")
                                        str.appendInterpolation(formatValue(player.marketValue))
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.caption2)
                                        .foregroundColor(Color.green).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding(Edge.Set.horizontal, 10.0)
                    .padding(Edge.Set.vertical, 6.0)
                    .background(Color.systemBackgroundCompat)
                    .cornerRadius(6.0)
                    .shadow(radius = 0.5).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .sheet(isPresented = Binding({ _showingPlayerDetail.wrappedValue }, { it -> _showingPlayerDetail.wrappedValue = it })) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    PlayerDetailView(player = player)
                        .environmentObject(kickbaseManager)
                        .environmentObject(ligainsiderService).Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedshowingPlayerDetail by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_showingPlayerDetail) }
        _showingPlayerDetail = rememberedshowingPlayerDetail

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!
        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    private fun positionAbbreviation(position: Int): String {
        when (position) {
            1 -> return "TW"
            2 -> return "ABW"
            3 -> return "MF"
            4 -> return "ST"
            else -> return "?"
        }
    }

    private fun positionColor(position: Int): Color {
        when (position) {
            1 -> return Color.yellow
            2 -> return Color.green
            3 -> return Color.blue
            4 -> return Color.red
            else -> return Color.gray
        }
    }
}

// MARK: - Player Count Overview
internal class PlayerCountOverview: View {
    internal val playerCounts: TeamPlayerCounts

    private fun getPositionColor(position: String, count: Int): Color {
        val minRequired: Int
        when (position) {
            "TW" -> minRequired = 1 // Mindestens 1 Torwart
            "ABW" -> minRequired = 3 // Mindestens 3 Verteidiger
            "MF" -> minRequired = 2 // Mindestens 2 Mittelfeldspieler
            "ST" -> minRequired = 1 // Mindestens 1 Stürmer
            else -> minRequired = 1
        }

        return if (count >= minRequired) Color.green else Color.red
    }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            HStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Gesamtanzahl
                    PlayerPositionCountView(position = "Gesamt", count = playerCounts.total, color = if (playerCounts.total >= 11) Color.green else Color.red).Compose(composectx)

                    Spacer().Compose(composectx)

                    // Torhüter
                    PlayerPositionCountView(position = "TW", count = playerCounts.goalkeepers, color = getPositionColor(position = "TW", count = playerCounts.goalkeepers)).Compose(composectx)

                    Spacer().Compose(composectx)

                    // Verteidiger
                    PlayerPositionCountView(position = "ABW", count = playerCounts.defenders, color = getPositionColor(position = "ABW", count = playerCounts.defenders)).Compose(composectx)

                    Spacer().Compose(composectx)

                    // Mittelfeldspieler
                    PlayerPositionCountView(position = "MF", count = playerCounts.midfielders, color = getPositionColor(position = "MF", count = playerCounts.midfielders)).Compose(composectx)

                    Spacer().Compose(composectx)

                    // Stürmer
                    PlayerPositionCountView(position = "ST", count = playerCounts.forwards, color = getPositionColor(position = "ST", count = playerCounts.forwards)).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .font(Font.headline)
            .padding(Edge.Set.horizontal)
            .padding(Edge.Set.vertical, 10.0)
            .background(Color.systemGray6Compat)
            .cornerRadius(12.0).Compose(composectx)
        }
    }

    constructor(playerCounts: TeamPlayerCounts) {
        this.playerCounts = playerCounts
    }
}

// MARK: - Spieleranzahl nach Position
internal class PlayerPositionCountView: View {
    internal val position: String
    internal val count: Int
    internal val color: Color

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 4.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(position)
                        .font(Font.caption)
                        .foregroundColor(Color.secondary).Compose(composectx)

                    Text({
                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                        str.appendInterpolation(count)
                        LocalizedStringKey(stringInterpolation = str)
                    }())
                        .font(Font.title2)
                        .fontWeight(Font.Weight.bold)
                        .foregroundColor(color).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .frame(maxWidth = Double.infinity).Compose(composectx)
        }
    }

    constructor(position: String, count: Int, color: Color) {
        this.position = position
        this.count = count
        this.color = color
    }
}

// MARK: - Team Budget Header für MainDashboardView
internal class TeamBudgetHeaderMain: View {
    internal val currentBudget: Int
    internal val saleValue: Int

    private val totalBudget: Int
        get() = currentBudget + saleValue

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            HStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(LocalizedStringKey(stringLiteral = "Aktuelles Budget"))
                                .font(Font.caption)
                                .foregroundColor(Color.secondary).Compose(composectx)
                            Text({
                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                str.appendLiteral("€")
                                str.appendInterpolation(formatValueWithSeparators(currentBudget))
                                LocalizedStringKey(stringInterpolation = str)
                            }())
                                .font(Font.headline)
                                .fontWeight(Font.Weight.bold)
                                .foregroundColor(if (currentBudget < 0) Color.red else Color.green).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    Spacer().Compose(composectx)

                    VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(LocalizedStringKey(stringLiteral = "Budget + Verkäufe"))
                                .font(Font.caption)
                                .foregroundColor(Color.secondary).Compose(composectx)
                            Text({
                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                str.appendLiteral("€")
                                str.appendInterpolation(formatValueWithSeparators(totalBudget))
                                LocalizedStringKey(stringInterpolation = str)
                            }())
                                .font(Font.headline)
                                .fontWeight(Font.Weight.bold)
                                .foregroundColor(if (totalBudget < 0) Color.red else (if (saleValue > 0) Color.green else Color.primary)).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding()
            .background(Color.systemGray6Compat)
            .cornerRadius(12.0)
            .padding(Edge.Set.horizontal).Compose(composectx)
        }
    }

    private fun formatValueWithSeparators(value: Int): String {
        val formatter = NumberFormatter()
        formatter.numberStyle = NumberFormatter.Style.decimal
        formatter.groupingSeparator = "."
        formatter.groupingSize = 3

        val matchtarget_4 = formatter.string(from = NSNumber(value = value))
        if (matchtarget_4 != null) {
            val formattedString = matchtarget_4
            return formattedString
        } else {
            return "${value}"
        }
    }

    constructor(currentBudget: Int, saleValue: Int) {
        this.currentBudget = currentBudget
        this.saleValue = saleValue
    }
}

// MARK: - Stats View mit detaillierten Punktzahl-Statistiken
internal class StatsView: View {
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            ScrollView { ->
                ComposeBuilder { composectx: ComposeContext ->
                    VStack(spacing = 20.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            kickbaseManager.userStats?.let { stats ->
                                DetailedStatsView(stats = stats).Compose(composectx)
                            }

                            // Team-Punktzahl Übersicht
                            TeamPointsOverview().Compose(composectx)

                            kickbaseManager.selectedLeague?.let { league ->
                                LeagueInfoView(league = league).Compose(composectx)
                            }
                            ComposeResult.ok
                        }
                    }
                    .padding().Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!

        return super.Evaluate(context, options)
    }
}

// MARK: - Team Punktzahl Übersicht
internal class TeamPointsOverview: View {
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(alignment = HorizontalAlignment.leading, spacing = 16.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(LocalizedStringKey(stringLiteral = "Team Punktzahl-Übersicht"))
                        .font(Font.title2)
                        .fontWeight(Font.Weight.bold).Compose(composectx)

                    val totalTeamPoints = kickbaseManager.teamPlayers.reduce(initialResult = 0) { it, it_1 -> it + it_1.totalPoints }
                    val averageTeamPoints = if (kickbaseManager.teamPlayers.isEmpty) 0.0 else Double(totalTeamPoints) / Double(kickbaseManager.teamPlayers.count)

                    LazyVGrid(columns = Array(repeating = GridItem(GridItem.Size.flexible()), count = 2), spacing = 16.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            StatCard(title = "Teampunkte Gesamt", value = "${totalTeamPoints}", icon = "star.fill", color = Color.orange).Compose(composectx)
                            StatCard(title = "Ø pro Spieler", value = String(format = "%.0f", averageTeamPoints), icon = "chart.line.uptrend.xyaxis", color = Color.blue).Compose(composectx)
                            StatCard(title = "Beste Punktzahl", value = "${kickbaseManager.teamPlayers.map({ it.totalPoints }).max() ?: 0}", icon = "crown.fill", color = Color.yellow).Compose(composectx)
                            StatCard(title = "Spieleranzahl", value = "${kickbaseManager.teamPlayers.count}", icon = "person.3.fill", color = Color.green).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding()
            .background(Color.systemGray6Compat)
            .cornerRadius(12.0).Compose(composectx)
        }
    }

    @Composable
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!

        return super.Evaluate(context, options)
    }
}

internal class DetailedStatsView: View {
    internal val stats: UserStats

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(alignment = HorizontalAlignment.leading, spacing = 16.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(LocalizedStringKey(stringLiteral = "Meine Liga-Statistiken"))
                        .font(Font.title2)
                        .fontWeight(Font.Weight.bold).Compose(composectx)

                    LazyVGrid(columns = Array(repeating = GridItem(GridItem.Size.flexible()), count = 2), spacing = 16.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            StatCard(title = "Gesamtpunkte", value = "${stats.points}", icon = "star.fill", color = Color.orange).Compose(composectx)
                            StatCard(title = "Platzierung", value = "#${stats.placement}", icon = "trophy.fill", color = Color.yellow).Compose(composectx)
                            StatCard(title = "Siege", value = "${stats.won}", icon = "checkmark.circle.fill", color = Color.green).Compose(composectx)
                            StatCard(title = "Niederlagen", value = "${stats.lost}", icon = "xmark.circle.fill", color = Color.red).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding()
            .background(Color.systemGray6Compat)
            .cornerRadius(12.0).Compose(composectx)
        }
    }

    constructor(stats: UserStats) {
        this.stats = stats
    }
}

internal class StatCard: View {
    internal val title: String
    internal val value: String
    internal val icon: String
    internal val color: Color

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 8.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Image(systemName = icon)
                        .font(Font.title2)
                        .foregroundColor(color).Compose(composectx)

                    Text(value)
                        .font(Font.title)
                        .fontWeight(Font.Weight.bold).Compose(composectx)

                    Text(title)
                        .font(Font.caption)
                        .foregroundColor(Color.secondary)
                        .multilineTextAlignment(TextAlignment.center)
                        .lineLimit(2).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .frame(height = 100.0)
            .frame(maxWidth = Double.infinity)
            .background(Color.systemBackgroundCompat)
            .cornerRadius(8.0).Compose(composectx)
        }
    }

    constructor(title: String, value: String, icon: String, color: Color) {
        this.title = title
        this.value = value
        this.icon = icon
        this.color = color
    }
}

internal class LeagueInfoView: View {
    internal val league: League

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(alignment = HorizontalAlignment.leading, spacing = 12.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(LocalizedStringKey(stringLiteral = "Liga-Informationen"))
                        .font(Font.title2)
                        .fontWeight(Font.Weight.bold).Compose(composectx)

                    InfoRow(label = "Liga", value = league.name).Compose(composectx)
                    InfoRow(label = "Saison", value = league.season).Compose(composectx)
                    InfoRow(label = "Spieltag", value = "${league.matchDay}").Compose(composectx)
                    InfoRow(label = "Admin", value = league.adminName).Compose(composectx)
                    InfoRow(label = "Ersteller", value = league.creatorName).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding()
            .background(Color.systemGray6Compat)
            .cornerRadius(12.0).Compose(composectx)
        }
    }

    constructor(league: League) {
        this.league = league
    }
}

// MARK: - Helper Views
internal class InfoRow: View {
    internal val label: String
    internal val value: String

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            HStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(label + ":")
                        .foregroundColor(Color.secondary).Compose(composectx)
                    Spacer().Compose(composectx)
                    Text(value)
                        .fontWeight(Font.Weight.medium).Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    constructor(label: String, value: String) {
        this.label = label
        this.value = value
    }
}

// Konvertiert MarketPlayer zu TeamPlayer für die PlayerDetailView
private fun convertMarketPlayerToTeamPlayer(marketPlayer: MarketPlayer): Player = Player(id = marketPlayer.id, firstName = marketPlayer.firstName, lastName = marketPlayer.lastName, profileBigUrl = marketPlayer.profileBigUrl, teamName = marketPlayer.teamName, teamId = marketPlayer.teamId, position = marketPlayer.position, number = marketPlayer.number, averagePoints = marketPlayer.averagePoints, totalPoints = marketPlayer.totalPoints, marketValue = marketPlayer.marketValue, marketValueTrend = marketPlayer.marketValueTrend, tfhmvt = 0, prlo = marketPlayer.prlo ?: 0, stl = marketPlayer.stl, status = marketPlayer.status, userOwnsPlayer = false)

// MARK: - All Players Row für Sales View
internal class AllPlayersRow: View {
    internal val player: Player
    internal val isSelected: Boolean
    internal val isRecommended: Boolean
    internal val onToggle: (Boolean) -> Unit
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    internal var ligainsiderService: LigainsiderService
        get() = _ligainsiderService.wrappedValue
        set(newValue) {
            _ligainsiderService.wrappedValue = newValue
        }
    internal var _ligainsiderService = skip.ui.Environment<LigainsiderService>()
    private var showingPlayerDetail: Boolean
        get() = _showingPlayerDetail.wrappedValue
        set(newValue) {
            _showingPlayerDetail.wrappedValue = newValue
        }
    private var _showingPlayerDetail: skip.ui.State<Boolean>

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            HStack(spacing = 12.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Position Badge
                    PositionBadge(position = player.position).Compose(composectx)

                    // Spieler-Info Bereich (klickbar für Details)
                    Button(action = { -> showingPlayerDetail = true }) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            HStack(spacing = 12.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            // Name mit Status-Icons und Empfehlungsindikator
                                            HStack(spacing = 4.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(player.fullName)
                                                        .font(Font.headline)
                                                        .fontWeight(Font.Weight.medium).Compose(composectx)

                                                    // Ligainsider Status Icon
                                                    val status = ligainsiderService.getPlayerStatus(firstName = player.firstName, lastName = player.lastName)
                                                    if (status != LigainsiderStatus.out) {
                                                        Image(systemName = ligainsiderService.getIcon(for_ = status))
                                                            .foregroundColor(ligainsiderService.getColor(for_ = status))
                                                            .font(Font.caption).Compose(composectx)
                                                    }

                                                    // Empfehlungsindikator
                                                    if (isRecommended) {
                                                        Image(systemName = "star.fill")
                                                            .foregroundColor(Color.orange)
                                                            .font(Font.caption).Compose(composectx)
                                                    }

                                                    // Status-Icons
                                                    if (player.status == 1) {
                                                        Image(systemName = "cross.circle.fill")
                                                            .foregroundColor(Color.red)
                                                            .font(Font.caption).Compose(composectx)
                                                    } else if (player.status == 2) {
                                                        Image(systemName = "pills.fill")
                                                            .foregroundColor(Color.orange)
                                                            .font(Font.caption).Compose(composectx)
                                                    } else if (player.status == 4) {
                                                        Image(systemName = "dumbbell.fill")
                                                            .foregroundColor(Color.blue)
                                                            .font(Font.caption).Compose(composectx)
                                                    } else if (player.status == 8) {
                                                        Image(systemName = "rectangle.fill")
                                                            .foregroundColor(Color.red)
                                                            .font(Font.caption).Compose(composectx)
                                                    }
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            // Team
                                            Text(player.fullTeamName)
                                                .font(Font.caption)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            // Verkaufsgrund falls empfohlen
                                            if (isRecommended) {
                                                Text(LocalizedStringKey(stringLiteral = "Verkaufsempfehlung"))
                                                    .font(Font.caption)
                                                    .foregroundColor(Color.blue)
                                                    .italic().Compose(composectx)
                                            }
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    Spacer().Compose(composectx)

                                    // Stats
                                    VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            // Durchschnittspunkte
                                            HStack(spacing = 4.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Image(systemName = "star.fill")
                                                        .font(Font.caption)
                                                        .foregroundColor(Color.orange).Compose(composectx)
                                                    Text(String(format = "%.0f", player.averagePoints))
                                                        .font(Font.subheadline)
                                                        .fontWeight(Font.Weight.semibold).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            // Marktwert
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendLiteral("€")
                                                str.appendInterpolation(formatValue(player.marketValue))
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.headline)
                                                .fontWeight(Font.Weight.bold)
                                                .foregroundColor(Color.green).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    // Toggle für Verkauf (separater Bereich)
                    Toggle(isOn = Binding<Boolean>(get = { -> isSelected }, set = { newValue -> onToggle(newValue) })) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(LocalizedStringKey(stringLiteral = "")).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .frame(width = 50.0, height = 30.0).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding(Edge.Set.vertical, 8.0)
            .background(if (isRecommended) Color.orange.opacity(0.1) else Color.clear)
            .cornerRadius(8.0)
            .sheet(isPresented = Binding({ _showingPlayerDetail.wrappedValue }, { it -> _showingPlayerDetail.wrappedValue = it })) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    PlayerDetailView(player = player)
                        .environmentObject(kickbaseManager)
                        .environmentObject(ligainsiderService).Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedshowingPlayerDetail by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_showingPlayerDetail) }
        _showingPlayerDetail = rememberedshowingPlayerDetail

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!
        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    private constructor(player: Player, isSelected: Boolean, isRecommended: Boolean, onToggle: (Boolean) -> Unit, showingPlayerDetail: Boolean = false, privatep: Nothing? = null) {
        this.player = player
        this.isSelected = isSelected
        this.isRecommended = isRecommended
        this.onToggle = onToggle
        this._showingPlayerDetail = skip.ui.State(showingPlayerDetail)
    }

    constructor(player: Player, isSelected: Boolean, isRecommended: Boolean, onToggle: (Boolean) -> Unit): this(player = player, isSelected = isSelected, isRecommended = isRecommended, onToggle = onToggle, privatep = null) {
    }
}

// MARK: - Helper Functions
private fun formatValue(value: Int): String {
    if (value >= 1_000_000) {
        return String(format = "%.1fM", Double(value) / 1_000_000)
    } else if (value >= 1000) {
        val kValue = Double(value) / 1000
        // Zeige Dezimalstelle wenn unter 10k, sonst ganz Zahlen
        if (kValue < 10) {
            return String(format = "%.1fk", kValue)
        } else {
            return String(format = "%.0fk", kValue)
        }
    } else {
        return "${value}"
    }
}

private fun getPriorityValue(priority: SalesRecommendation.Priority): Int {
    when (priority) {
        SalesRecommendation.Priority.high -> return 1
        SalesRecommendation.Priority.medium -> return 2
        SalesRecommendation.Priority.low -> return 3
    }
}

private fun getImpactValue(impact: SalesRecommendation.LineupImpact): Int {
    when (impact) {
        SalesRecommendation.LineupImpact.minimal -> return 1
        SalesRecommendation.LineupImpact.moderate -> return 2
        SalesRecommendation.LineupImpact.significant -> return 3
    }
}

private fun analyzePositionRedundancy(player: Player, allPlayers: Array<Player>): Tuple2<Boolean, Boolean> {
    val playersInSamePosition = allPlayers.filter { it -> it.position == player.position }

    // Mindestanzahl pro Position
    val minRequired: Int
    when (player.position) {
        1 -> minRequired = 1 // Torwart
        2 -> minRequired = 3 // Verteidiger
        3 -> minRequired = 2 // Mittelfeld
        4 -> minRequired = 1 // Sturm
        else -> minRequired = 1
    }

    val isRedundant = playersInSamePosition.count > minRequired

    // Prüfe ob der Spieler der schwächste auf seiner Position ist
    val sortedByPerformance = playersInSamePosition.sorted { it, it_1 -> it.averagePoints > it_1.averagePoints }
    val isWeakestInPosition = sortedByPerformance.last?.id == player.id

    return Tuple2(isRedundant, isWeakestInPosition)
}

private fun isPlayerOverpriced(player: Player, allPlayers: Array<Player>): Boolean {
    val teamAveragePoints = allPlayers.map({ it.averagePoints }).reduce(initialResult = 0.0, { it, it_1 -> it + it_1 }) / Double(allPlayers.count)
    val teamAverageValue = Double(allPlayers.map({ it.marketValue }).reduce(initialResult = 0, { it, it_1 -> it + it_1 })) / Double(allPlayers.count)

    // Spieler ist überbewertet wenn sein Wert/Punkte-Verhältnis deutlich schlechter ist als der Teamdurchschnitt
    val playerValuePerPoint = Double(player.marketValue) / max(player.averagePoints, 1.0)
    val teamValuePerPoint = teamAverageValue / max(teamAveragePoints, 1.0)

    return playerValuePerPoint > teamValuePerPoint * 1.3 // 30% schlechter als Durchschnitt
}

private fun isPlayerWeakestInPosition(player: Player, allPlayers: Array<Player>): Boolean {
    val playersInSamePosition = allPlayers.filter { it -> it.position == player.position }
    val sortedByPerformance = playersInSamePosition.sorted { it, it_1 -> it.averagePoints > it_1.averagePoints }

    return sortedByPerformance.last?.id == player.id
}

private fun calculateLineupImpact(player: Player, allPlayers: Array<Player>): SalesRecommendation.LineupImpact {
    val positionAnalysis = analyzePositionRedundancy(player = player, allPlayers = allPlayers)

    // Hoher Impact wenn Position nicht redundant ist
    if (!positionAnalysis.isRedundant) {
        return SalesRecommendation.LineupImpact.significant
    }

    // Mittlerer Impact wenn Spieler überdurchschnittlich ist
    val teamAveragePoints = allPlayers.map({ it.averagePoints }).reduce(initialResult = 0.0, { it, it_1 -> it + it_1 }) / Double(allPlayers.count)
    if (player.averagePoints > teamAveragePoints * 1.1) {
        return SalesRecommendation.LineupImpact.moderate
    }

    // Minimaler Impact bei schwachen, redundanten Spielern
    return SalesRecommendation.LineupImpact.minimal
}

// MARK: - Team Strength Calculation (Dynamic based on actual placements)
private fun getTeamStrength(teamId: String): Double {
    // Fallback: Verwende mittlere Stärke, da wir hier keinen Zugriff auf den Cache haben
    return 0.5
}

// Verbesserte Version: Team-Stärke basierend auf Platzierung berechnen
private fun calculateTeamStrengthFromPlacement(placement: Int, totalTeams: Int = 18): Double {
    // Konvertiere Platzierung in Stärke-Wert (1.0 = bestes Team, 0.0 = schlechtestes Team)
    // Platz 1 = Stärke 1.0, Platz 18 = Stärke ~0.06
    val normalizedPlacement = Double(placement - 1) / Double(totalTeams - 1)
    val strength = 1.0 - normalizedPlacement

    // Minimum-Stärke von 0.1 für das schlechteste Team
    return max(0.1, strength)
}

// MARK: - Match Difficulty Calculation
private fun calculateMatchDifficulty(opponentStrength: Double, isAwayGame: Boolean): Double {
    var difficulty = opponentStrength

    // Auswärtsspiele sind schwieriger
    if (isAwayGame) {
        difficulty *= 1.2 // 20% schwieriger
    }

    // Begrenze auf maximale Schwierigkeit von 1.0
    return min(1.0, difficulty)
}

// Anhand von isCurrent den aktuellen Spieltag laden.
private fun getCurrentMatchDay(allPerformances: Array<EnhancedMatchPerformance>): Int {

    allPerformances.first(where = { it -> it.isCurrent })?.let { currentMatch ->
        return currentMatch.matchDay
    }

    return 1 // Fallback-Wert
}
