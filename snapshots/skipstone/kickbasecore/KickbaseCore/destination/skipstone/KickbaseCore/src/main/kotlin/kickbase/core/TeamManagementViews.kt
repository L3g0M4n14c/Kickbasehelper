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

internal class TeamTab: View {
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
    private var sortBy: TeamTab.SortOption
        get() = _sortBy.wrappedValue
        set(newValue) {
            _sortBy.wrappedValue = newValue
        }
    private var _sortBy: skip.ui.State<TeamTab.SortOption>
    private var searchText: String
        get() = _searchText.wrappedValue
        set(newValue) {
            _searchText.wrappedValue = newValue
        }
    private var _searchText: skip.ui.State<String>
    private var playersForSale: Set<String>
        get() = _playersForSale.wrappedValue.sref({ this.playersForSale = it })
        set(newValue) {
            _playersForSale.wrappedValue = newValue.sref()
        }
    private var _playersForSale: skip.ui.State<Set<String>>
    private var selectedSaleValue: Int
        get() = _selectedSaleValue.wrappedValue
        set(newValue) {
            _selectedSaleValue.wrappedValue = newValue
        }
    private var _selectedSaleValue: skip.ui.State<Int>
    private var showRecommendations: Boolean
        get() = _showRecommendations.wrappedValue
        set(newValue) {
            _showRecommendations.wrappedValue = newValue
        }
    private var _showRecommendations: skip.ui.State<Boolean>
    private var showLineupOptimization: Boolean
        get() = _showLineupOptimization.wrappedValue
        set(newValue) {
            _showLineupOptimization.wrappedValue = newValue
        }
    private var _showLineupOptimization: skip.ui.State<Boolean>
    private var lineupComparison: LineupComparison?
        get() = _lineupComparison.wrappedValue
        set(newValue) {
            _lineupComparison.wrappedValue = newValue
        }
    private var _lineupComparison: skip.ui.State<LineupComparison?> = skip.ui.State(null)
    private var isGeneratingLineup: Boolean
        get() = _isGeneratingLineup.wrappedValue
        set(newValue) {
            _isGeneratingLineup.wrappedValue = newValue
        }
    private var _isGeneratingLineup: skip.ui.State<Boolean>
    private var lineupGenerationError: String?
        get() = _lineupGenerationError.wrappedValue
        set(newValue) {
            _lineupGenerationError.wrappedValue = newValue
        }
    private var _lineupGenerationError: skip.ui.State<String?> = skip.ui.State(null)
    @androidx.annotation.Keep
    internal enum class SortOption(override val rawValue: String, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, RawRepresentable<String> {
        name_("Name"),
        marketValue("Marktwert"),
        points("Punkte"),
        trend("Trend"),
        position("Position");

        @androidx.annotation.Keep
        companion object: CaseIterableCompanion<TeamTab.SortOption> {
            fun init(rawValue: String): TeamTab.SortOption? {
                return when (rawValue) {
                    "Name" -> SortOption.name_
                    "Marktwert" -> SortOption.marketValue
                    "Punkte" -> SortOption.points
                    "Trend" -> SortOption.trend
                    "Position" -> SortOption.position
                    else -> null
                }
            }

            override val allCases: Array<TeamTab.SortOption>
                get() = arrayOf(name_, marketValue, points, trend, position)
        }
    }

    // Funktion zur Berechnung des Gesamtwerts der zum Verkauf ausgewählten Spieler
    private fun calculateTotalSaleValue() {
        val selectedPlayers = kickbaseManager.teamPlayers
            .filter { it -> playersForSale.contains(it.id) }

        print("🔍 TeamTab: Calculating totalSaleValue")
        print("   - Selected players count: ${selectedPlayers.count}")
        print("   - PlayersForSale set: ${playersForSale}")

        val total = selectedPlayers.reduce(initialResult = 0) l@{ sum, player ->
            print("   - Adding player: ${player.fullName} with marketValue: ${player.marketValue}")
            return@l sum + player.marketValue
        }

        print("   - Total sale value: ${total}")
        selectedSaleValue = total
    }

    private fun generateOptimalLineup() {
        Task l@{ ->
            isGeneratingLineup = true
            lineupGenerationError = null

            try {
                val league_0 = kickbaseManager.selectedLeague
                if (league_0 == null) {
                    lineupGenerationError = "Keine Liga ausgewählt"
                    isGeneratingLineup = false
                    return@l
                }

                print("🎯 TeamTab: Starting lineup generation for league: ${league_0.name}")

                // Verwende den shared PlayerRecommendationService vom KickbaseManager
                val recommendationService = kickbaseManager.playerRecommendationService

                // Standard-Formation: 4-2-3-1 [1 TW, 4 ABW, 2 MF, 3 MF, 1 ST]
                // Die API gibt uns die möglichen Formationen, aber für jetzt verwenden wir eine Standard-Formation
                val formation = arrayOf(1, 4, 4, 2) // 4-4-2 als Alternative

                val comparison = recommendationService.generateOptimalLineupComparison(for_ = league_0, teamPlayers = kickbaseManager.mainactor { it.teamPlayers }, marketPlayers = kickbaseManager.mainactor { it.marketPlayers }, formation = formation)

                MainActor.run { ->
                    this.lineupComparison = comparison
                    this.showLineupOptimization = true
                    isGeneratingLineup = false
                    print("✅ Lineup generation completed")
                }
            } catch (error: Throwable) {
                @Suppress("NAME_SHADOWING") val error = error.aserror()
                MainActor.run { ->
                    lineupGenerationError = "Fehler bei der Aufstellungs-Generierung: ${error.localizedDescription}"
                    isGeneratingLineup = false
                }
            }
        }
    }

    override fun body(): View {
        return ComposeBuilder l@{ composectx: ComposeContext ->
            print("[🟠 TEAMTAB] TeamTab.body rendering")
            return@l NavigationStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    ZStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            VStack(spacing = 0.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    // Header mit Buttons
                                    VStack(spacing = 8.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Picker(LocalizedStringKey(stringLiteral = "View"), selection = Binding({ _showRecommendations.wrappedValue }, { it -> _showRecommendations.wrappedValue = it })) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(LocalizedStringKey(stringLiteral = "Mein Team")).tag(false).Compose(composectx)
                                                    Text(LocalizedStringKey(stringLiteral = "Verkaufs-Tipps")).tag(true).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }
                                            .pickerStyle(PickerStyle.segmented)
                                            .padding().Compose(composectx)

                                            lineupGenerationError?.let { error ->
                                                HStack { ->
                                                    ComposeBuilder { composectx: ComposeContext ->
                                                        Image(systemName = "exclamationmark.circle.fill")
                                                            .foregroundColor(Color.red).Compose(composectx)
                                                        Text(error)
                                                            .font(Font.caption)
                                                            .foregroundColor(Color.red).Compose(composectx)
                                                        ComposeResult.ok
                                                    }
                                                }
                                                .padding(8.0)
                                                .background(Color.red.opacity(0.1))
                                                .cornerRadius(6.0)
                                                .padding(Edge.Set.horizontal).Compose(composectx)
                                            }
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    if (showRecommendations) {
                                        // Verkaufs-Empfehlungen View
                                        SaleRecommendationsView(kickbaseManager = kickbaseManager).Compose(composectx)
                                    } else {
                                        // Original Team View
                                        VStack { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                // Budget-Anzeige mit verkaufbaren Spielern
                                                kickbaseManager.userStats?.let { stats ->
                                                    TeamBudgetHeader(currentBudget = stats.budget, saleValue = selectedSaleValue)
                                                        .padding(Edge.Set.horizontal).Compose(composectx)
                                                }

                                                // Search and Sort Controls
                                                VStack(spacing = 10.0) { ->
                                                    ComposeBuilder { composectx: ComposeContext ->
                                                        HStack { ->
                                                            ComposeBuilder { composectx: ComposeContext ->
                                                                Image(systemName = "magnifyingglass")
                                                                    .foregroundColor(Color.gray).Compose(composectx)
                                                                TextField(LocalizedStringKey(stringLiteral = "Spieler suchen..."), text = Binding({ _searchText.wrappedValue }, { it -> _searchText.wrappedValue = it })).Compose(composectx)
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
                                                                        ForEach(SortOption.allCases, id = { it }) { option ->
                                                                            ComposeBuilder { composectx: ComposeContext ->
                                                                                Text(option.rawValue).tag(option).Compose(composectx)
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
                                                }
                                                .padding(Edge.Set.horizontal).Compose(composectx)

                                                // Players List or Empty State
                                                if (kickbaseManager.teamPlayers.isEmpty) {
                                                    VStack(spacing = 20.0) { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            Spacer().Compose(composectx)

                                                            Image(systemName = "person.3.fill")
                                                                .font(Font.system(size = 60.0))
                                                                .foregroundColor(Color.gray).Compose(composectx)

                                                            Text(LocalizedStringKey(stringLiteral = "Keine Spieler geladen"))
                                                                .font(Font.headline)
                                                                .foregroundColor(Color.primary).Compose(composectx)

                                                            Text(LocalizedStringKey(stringLiteral = "Ziehe nach unten um zu aktualisieren oder wähle eine Liga aus"))
                                                                .font(Font.subheadline)
                                                                .foregroundColor(Color.secondary)
                                                                .multilineTextAlignment(TextAlignment.center)
                                                                .padding(Edge.Set.horizontal).Compose(composectx)

                                                            Button(LocalizedStringKey(stringLiteral = "Team neu laden")) { ->
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
                                                    // Players List
                                                    List { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            ForEach(Array(filteredAndSortedPlayers.enumerated()), id = { it.offset }) { (index, player) ->
                                                                ComposeBuilder { composectx: ComposeContext ->
                                                                    print("[LIST] Rendering player ${index + 1}: ${player.fullName}")
                                                                    TeamPlayerRowWithSale(teamPlayer = player, isSelectedForSale = playersForSale.contains(player.id), onToggleSale = { isSelected ->
                                                                        print("🔄 TeamTab: Toggle for player ${player.fullName} (ID: ${player.id}) - isSelected: ${isSelected}")
                                                                        print("   - Player market value: ${player.marketValue}")
                                                                        if (isSelected) {
                                                                            playersForSale.insert(player.id)
                                                                            print("   - Added to playersForSale. New set: ${playersForSale}")
                                                                        } else {
                                                                            playersForSale.remove(player.id)
                                                                            print("   - Removed from playersForSale. New set: ${playersForSale}")
                                                                        }
                                                                        // Explizit die Berechnung triggern
                                                                        calculateTotalSaleValue()
                                                                    })
                                                                    .id("${player.id}-${index}").Compose(composectx)
                                                                    ComposeResult.ok
                                                                }
                                                            }.Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }
                                                    .environmentObject(ligainsiderService)
                                                    .refreshable { -> MainActor.run {
                                                        kickbaseManager.selectedLeague?.let { league ->
                                                            kickbaseManager.loadTeamPlayers(for_ = league)
                                                        }
                                                    } }.Compose(composectx)
                                                }
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)
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
            .navigationTitle({
                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                str.appendLiteral("Mein Team (")
                str.appendInterpolation(kickbaseManager.teamPlayers.count)
                str.appendLiteral(")")
                LocalizedStringKey(stringInterpolation = str)
            }())
            .onAppear { ->
                print("🎯 TeamTab appeared - Players count: ${kickbaseManager.teamPlayers.count}")
                calculateTotalSaleValue() // Initial berechnen
                if (kickbaseManager.teamPlayers.isEmpty) {
                    print("🔄 TeamTab: No players found, triggering reload...")
                    Task(isMainActor = true) { ->
                        kickbaseManager.selectedLeague?.let { league ->
                            kickbaseManager.loadTeamPlayers(for_ = league)
                        }
                    }
                }
            }
            .sheet(item = Binding({ _lineupComparison.wrappedValue }, { it -> _lineupComparison.wrappedValue = it })) { comparison ->
                ComposeBuilder { composectx: ComposeContext ->
                    LineupComparisonView(comparison = comparison)
                        .environmentObject(kickbaseManager)
                        .environmentObject(ligainsiderService).Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
            ComposeResult.ok
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedsortBy by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<TeamTab.SortOption>, Any>) { mutableStateOf(_sortBy) }
        _sortBy = rememberedsortBy

        val rememberedsearchText by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<String>, Any>) { mutableStateOf(_searchText) }
        _searchText = rememberedsearchText

        val rememberedplayersForSale by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Set<String>>, Any>) { mutableStateOf(_playersForSale) }
        _playersForSale = rememberedplayersForSale

        val rememberedselectedSaleValue by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Int>, Any>) { mutableStateOf(_selectedSaleValue) }
        _selectedSaleValue = rememberedselectedSaleValue

        val rememberedshowRecommendations by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_showRecommendations) }
        _showRecommendations = rememberedshowRecommendations

        val rememberedshowLineupOptimization by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_showLineupOptimization) }
        _showLineupOptimization = rememberedshowLineupOptimization

        val rememberedlineupComparison by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<LineupComparison?>, Any>) { mutableStateOf(_lineupComparison) }
        _lineupComparison = rememberedlineupComparison

        val rememberedisGeneratingLineup by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_isGeneratingLineup) }
        _isGeneratingLineup = rememberedisGeneratingLineup

        val rememberedlineupGenerationError by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<String?>, Any>) { mutableStateOf(_lineupGenerationError) }
        _lineupGenerationError = rememberedlineupGenerationError

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!
        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    private val filteredAndSortedPlayers: Array<Player>
        get() {
            val filtered = (if (searchText.isEmpty) kickbaseManager.teamPlayers else kickbaseManager.teamPlayers.filter { player -> player.firstName.lowercased().contains(searchText.lowercased()) || player.lastName.lowercased().contains(searchText.lowercased()) || player.fullTeamName.lowercased().contains(searchText.lowercased()) }).sref()

            print("[⚡ FILTER] filteredAndSortedPlayers computed: ${filtered.count} players after filtering")

            val sorted = filtered.sorted(by = l@{ player1, player2 ->
                when (sortBy) {
                    TeamTab.SortOption.name_ -> return@l player1.lastName < player2.lastName
                    TeamTab.SortOption.marketValue -> return@l player1.marketValue > player2.marketValue
                    TeamTab.SortOption.points -> return@l player1.totalPoints > player2.totalPoints
                    TeamTab.SortOption.trend -> return@l player1.marketValueTrend > player2.marketValueTrend
                    TeamTab.SortOption.position -> return@l player1.position < player2.position
                }
            })
            print("[⚡ FILTER] Returning ${sorted.count} sorted players")
            return sorted
        }

    private constructor(sortBy: TeamTab.SortOption = TeamTab.SortOption.marketValue, searchText: String = "", playersForSale: Set<String> = setOf(), selectedSaleValue: Int = 0, showRecommendations: Boolean = false, showLineupOptimization: Boolean = false, lineupComparison: LineupComparison? = null, isGeneratingLineup: Boolean = false, lineupGenerationError: String? = null, privatep: Nothing? = null) {
        this._sortBy = skip.ui.State(sortBy)
        this._searchText = skip.ui.State(searchText)
        this._playersForSale = skip.ui.State(playersForSale.sref())
        this._selectedSaleValue = skip.ui.State(selectedSaleValue)
        this._showRecommendations = skip.ui.State(showRecommendations)
        this._showLineupOptimization = skip.ui.State(showLineupOptimization)
        this._lineupComparison = skip.ui.State(lineupComparison)
        this._isGeneratingLineup = skip.ui.State(isGeneratingLineup)
        this._lineupGenerationError = skip.ui.State(lineupGenerationError)
    }

    constructor(): this(privatep = null) {
    }

    @androidx.annotation.Keep
    companion object {

        internal fun SortOption(rawValue: String): TeamTab.SortOption? = SortOption.init(rawValue = rawValue)
    }
}

internal class TeamPlayerRow: View {
    internal val teamPlayer: Player
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
            print("[DEBUG] TeamPlayerRow.body rendering: ${teamPlayer.fullName}")
            Button(action = { ->
                print("🔄 TeamPlayerRow: Tapped on player ${teamPlayer.fullName}")
                showingPlayerDetail = true
            }) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    HStack(spacing = 12.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Ligainsider Status Icon
                            val status = ligainsiderService.getPlayerStatus(firstName = teamPlayer.firstName, lastName = teamPlayer.lastName)
                            if (status != LigainsiderStatus.out) {
                                Image(systemName = ligainsiderService.getIcon(for_ = status))
                                    .foregroundColor(ligainsiderService.getColor(for_ = status))
                                    .font(Font.caption).Compose(composectx)
                            }

                            // Player photo or position indicator
                            // Call getLigainsiderPlayer INSIDE the closure for fresh reference
                            val photoUrl: URL? = linvoke l@{ ->
                                val ligaPlayer = ligainsiderService.getLigainsiderPlayer(firstName = teamPlayer.firstName, lastName = teamPlayer.lastName)

                                // Try ligainsider imageUrl first
                                ligaPlayer?.imageUrl?.let { imgString ->
                                    (try { URL(string = imgString) } catch (_: NullReturnException) { null })?.let { url ->
                                        print("[TeamPlayerRow] USING LIGAINSIDER: ${teamPlayer.firstName} ${teamPlayer.lastName} -> ${imgString}")
                                        return@l url
                                    }
                                }
                                // Fall back to teamPlayer imageUrl
                                teamPlayer.imageUrl.sref()?.let { url ->
                                    print("[TeamPlayerRow] USING KICKBASE: ${teamPlayer.firstName} ${teamPlayer.lastName} -> ${url}")
                                    return@l url
                                }
                                print("[TeamPlayerRow] NO PHOTO: ${teamPlayer.firstName} ${teamPlayer.lastName}")
                                return@l null
                            }

                            print("[🔴 CRITICAL DEBUG] photoUrl result for ${teamPlayer.firstName} ${teamPlayer.lastName}: ${photoUrl?.absoluteString ?: "NIL"}")

                            if (photoUrl != null) {
                                val url = photoUrl.sref()
                                // Show player photo
                                print("[🟢 AsyncImage] Will show photo: ${url.absoluteString}")
                                // Fallback for non-Apple platforms
                                VStack { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Text(positionAbbreviation(teamPlayer.position))
                                            .font(Font.caption2)
                                            .fontWeight(Font.Weight.bold)
                                            .foregroundColor(Color.white)
                                            .padding(Edge.Set.horizontal, 6.0)
                                            .padding(Edge.Set.vertical, 2.0)
                                            .background(positionColor(teamPlayer.position))
                                            .cornerRadius(4.0).Compose(composectx)

                                        Text({
                                            val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                            str.appendInterpolation(teamPlayer.number)
                                            LocalizedStringKey(stringInterpolation = str)
                                        }())
                                            .font(Font.caption2)
                                            .foregroundColor(Color.secondary).Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }
                                .frame(minWidth = 40.0).Compose(composectx)
                            } else {
                                // Fallback: No photo available, show position circle
                                VStack { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Text(positionAbbreviation(teamPlayer.position))
                                            .font(Font.caption2)
                                            .fontWeight(Font.Weight.bold)
                                            .foregroundColor(Color.white)
                                            .padding(Edge.Set.horizontal, 6.0)
                                            .padding(Edge.Set.vertical, 2.0)
                                            .background(positionColor(teamPlayer.position))
                                            .cornerRadius(4.0).Compose(composectx)

                                        Text({
                                            val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                            str.appendInterpolation(teamPlayer.number)
                                            LocalizedStringKey(stringInterpolation = str)
                                        }())
                                            .font(Font.caption2)
                                            .foregroundColor(Color.secondary).Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }
                                .frame(minWidth = 40.0).Compose(composectx)
                            }

                            // Player Info - erweiterte Breite für Namen
                            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    HStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(teamPlayer.fullName)
                                                .font(Font.headline)
                                                .lineLimit(2).Compose(composectx) // Erlaubt 2 Zeilen für längere Namen

                                            // Status-Icons basierend auf st-Feld aus API-Daten anzeigen
                                            if (teamPlayer.status == 1) {
                                                // Verletzt - rotes Kreuz
                                                Image(systemName = "cross.circle.fill")
                                                    .foregroundColor(Color.red)
                                                    .font(Font.caption).Compose(composectx)
                                            } else if (teamPlayer.status == 2) {
                                                // Angeschlagen - Tabletten-Icon
                                                Image(systemName = "pills.fill")
                                                    .foregroundColor(Color.orange)
                                                    .font(Font.caption).Compose(composectx)
                                            } else if (teamPlayer.status == 4) {
                                                // Aufbautraining - Hantel-Icon
                                                Image(systemName = "dumbbell.fill")
                                                    .foregroundColor(Color.blue)
                                                    .font(Font.caption).Compose(composectx)
                                            }
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    Text(teamPlayer.fullTeamName)
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary)
                                        .lineLimit(1).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .frame(minWidth = 160.0, alignment = Alignment.leading).Compose(composectx) // Mindestbreite für Namensbereich

                            Spacer(minLength = 8.0).Compose(composectx) // Reduzierter Mindestabstand

                            // Stats - Durchschnittspunktzahl als große Zahl, Gesamtpunktzahl als kleine Zahl
                            VStack(alignment = HorizontalAlignment.trailing, spacing = 2.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendInterpolation(teamPlayer.averagePoints)
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.title2)
                                        .fontWeight(Font.Weight.bold)
                                        .foregroundColor(Color.primary).Compose(composectx)

                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendInterpolation(teamPlayer.totalPoints)
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary).Compose(composectx)

                                    HStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendLiteral("€")
                                                str.appendInterpolation(teamPlayer.marketValue / 1_000_000)
                                                str.appendLiteral("M")
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.caption2)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            if (teamPlayer.tfhmvt > 0) {
                                                Image(systemName = "arrow.up")
                                                    .foregroundColor(Color.green)
                                                    .font(Font.caption2).Compose(composectx)
                                            } else if (teamPlayer.tfhmvt < 0) {
                                                Image(systemName = "arrow.down")
                                                    .foregroundColor(Color.red)
                                                    .font(Font.caption2).Compose(composectx)
                                            } else {
                                                Image(systemName = "minus")
                                                    .foregroundColor(Color.gray)
                                                    .font(Font.caption2).Compose(composectx)
                                            }
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .frame(minWidth = 80.0, alignment = Alignment.trailing).Compose(composectx) // Feste Breite für Stats
                            ComposeResult.ok
                        }
                    }
                    .padding(Edge.Set.vertical, 4.0).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .sheet(isPresented = Binding({ _showingPlayerDetail.wrappedValue }, { it -> _showingPlayerDetail.wrappedValue = it })) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    kickbaseManager.selectedLeague?.let { league ->
                        PlayerDetailView(player = teamPlayer)
                            .environmentObject(kickbaseManager)
                            .environmentObject(ligainsiderService).Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }.Compose(composectx)
            ComposeResult.ok
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

    private constructor(teamPlayer: Player, showingPlayerDetail: Boolean = false, privatep: Nothing? = null) {
        this.teamPlayer = teamPlayer
        this._showingPlayerDetail = skip.ui.State(showingPlayerDetail)
    }

    constructor(teamPlayer: Player): this(teamPlayer = teamPlayer, privatep = null) {
    }
}

internal class TeamPlayerRowWithSale: View {
    internal val teamPlayer: Player
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

    override fun body(): View {
        return ComposeBuilder l@{ composectx: ComposeContext ->
            print("[DEBUG] TeamPlayerRowWithSale.body rendering: ${teamPlayer.fullName}")

            // Calculate photoUrl HERE inside body with getLigainsiderPlayer() called INSIDE the closure
            val photoUrl: URL? = linvoke l@{ ->
                print("[🔵 PHOTOURL] Computing photoUrl for ${teamPlayer.firstName} ${teamPlayer.lastName}")

                // Call getLigainsiderPlayer INSIDE the closure for fresh reference
                val ligaPlayer = ligainsiderService.getLigainsiderPlayer(firstName = teamPlayer.firstName, lastName = teamPlayer.lastName)

                print("[PHOTO_URL] ${teamPlayer.firstName} ${teamPlayer.lastName}:")
                if (ligaPlayer != null) {
                    print("  ✅ FOUND in Ligainsider cache")
                    ligaPlayer?.let { lp ->
                        print("     imageUrl = ${lp.imageUrl ?: "NIL"}")
                    }
                } else {
                    print("  ❌ NOT FOUND in Ligainsider cache")
                }
                print("  TeamPlayer.imageUrl = ${teamPlayer.imageUrl?.absoluteString ?: "NIL"}")

                // Try ligainsider imageUrl first
                ligaPlayer?.imageUrl?.let { imgString ->
                    (try { URL(string = imgString) } catch (_: NullReturnException) { null })?.let { url ->
                        print("  -> RESULT: USING LIGAINSIDER")
                        return@l url
                    }
                }
                // Fall back to teamPlayer imageUrl
                teamPlayer.imageUrl.sref()?.let { url ->
                    print("  -> RESULT: USING KICKBASE FALLBACK")
                    return@l url
                }
                print("  -> RESULT: NO PHOTO")
                return@l null
            }

            print("[🔴 CRITICAL DEBUG] photoUrl result for ${teamPlayer.firstName} ${teamPlayer.lastName}: ${photoUrl?.absoluteString ?: "NIL"}")

            return@l Button(action = { ->
                print("🔄 TeamPlayerRow: Tapped on player ${teamPlayer.fullName}")
                showingPlayerDetail = true
            }) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    HStack(spacing = 12.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Player photo or position indicator

                            if (photoUrl != null) {
                                val url = photoUrl.sref()
                                // Show player photo
                                print("[🟢 AsyncImage] Will show photo: ${url.absoluteString}")
                                // Fallback for non-Apple platforms
                                VStack { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Text(positionAbbreviation(teamPlayer.position))
                                            .font(Font.caption2)
                                            .fontWeight(Font.Weight.bold)
                                            .foregroundColor(Color.white)
                                            .padding(Edge.Set.horizontal, 6.0)
                                            .padding(Edge.Set.vertical, 2.0)
                                            .background(positionColor(teamPlayer.position))
                                            .cornerRadius(4.0).Compose(composectx)

                                        Text({
                                            val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                            str.appendInterpolation(teamPlayer.number)
                                            LocalizedStringKey(stringInterpolation = str)
                                        }())
                                            .font(Font.caption2)
                                            .foregroundColor(Color.secondary).Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }
                                .frame(minWidth = 40.0).Compose(composectx)
                            } else {
                                // Fallback: No photo available, show position circle
                                VStack { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Text(positionAbbreviation(teamPlayer.position))
                                            .font(Font.caption2)
                                            .fontWeight(Font.Weight.bold)
                                            .foregroundColor(Color.white)
                                            .padding(Edge.Set.horizontal, 6.0)
                                            .padding(Edge.Set.vertical, 2.0)
                                            .background(positionColor(teamPlayer.position))
                                            .cornerRadius(4.0).Compose(composectx)

                                        Text({
                                            val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                            str.appendInterpolation(teamPlayer.number)
                                            LocalizedStringKey(stringInterpolation = str)
                                        }())
                                            .font(Font.caption2)
                                            .foregroundColor(Color.secondary).Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }
                                .frame(minWidth = 40.0).Compose(composectx)
                            }

                            // Player Info - erweiterte Breite für Namen
                            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    HStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(teamPlayer.fullName)
                                                .font(Font.headline)
                                                .lineLimit(2).Compose(composectx) // Erlaubt 2 Zeilen für längere Namen

                                            // Ligainsider Status Icon
                                            val status = ligainsiderService.getPlayerStatus(firstName = teamPlayer.firstName, lastName = teamPlayer.lastName)
                                            if (status != LigainsiderStatus.out) {
                                                Image(systemName = ligainsiderService.getIcon(for_ = status))
                                                    .foregroundColor(ligainsiderService.getColor(for_ = status))
                                                    .font(Font.caption).Compose(composectx)
                                            }

                                            // Status-Icons basierend auf st-Feld aus API-Daten anzeigen
                                            if (teamPlayer.status == 1) {
                                                // Verletzt - rotes Kreuz
                                                Image(systemName = "cross.circle.fill")
                                                    .foregroundColor(Color.red)
                                                    .font(Font.caption).Compose(composectx)
                                            } else if (teamPlayer.status == 2) {
                                                // Angeschlagen - Tabletten-Icon
                                                Image(systemName = "pills.fill")
                                                    .foregroundColor(Color.orange)
                                                    .font(Font.caption).Compose(composectx)
                                            } else if (teamPlayer.status == 4) {
                                                // Aufbautraining - Hantel-Icon
                                                Image(systemName = "dumbbell.fill")
                                                    .foregroundColor(Color.blue)
                                                    .font(Font.caption).Compose(composectx)
                                            }
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    Text(teamPlayer.fullTeamName)
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary)
                                        .lineLimit(1).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .frame(minWidth = 160.0, alignment = Alignment.leading).Compose(composectx) // Mindestbreite für Namensbereich

                            Spacer(minLength = 8.0).Compose(composectx) // Reduzierter Mindestabstand

                            // Stats - Durchschnittspunktzahl als große Zahl, Gesamtpunktzahl als kleine Zahl
                            VStack(alignment = HorizontalAlignment.trailing, spacing = 2.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendInterpolation(teamPlayer.averagePoints)
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.title2)
                                        .fontWeight(Font.Weight.bold)
                                        .foregroundColor(Color.primary).Compose(composectx)

                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendInterpolation(teamPlayer.totalPoints)
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary).Compose(composectx)

                                    HStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendLiteral("€")
                                                str.appendInterpolation(teamPlayer.marketValue / 1_000_000)
                                                str.appendLiteral("M")
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.caption2)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            if (teamPlayer.tfhmvt > 0) {
                                                Image(systemName = "arrow.up")
                                                    .foregroundColor(Color.green)
                                                    .font(Font.caption2).Compose(composectx)
                                            } else if (teamPlayer.tfhmvt < 0) {
                                                Image(systemName = "arrow.down")
                                                    .foregroundColor(Color.red)
                                                    .font(Font.caption2).Compose(composectx)
                                            } else {
                                                Image(systemName = "minus")
                                                    .foregroundColor(Color.gray)
                                                    .font(Font.caption2).Compose(composectx)
                                            }
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .frame(minWidth = 80.0, alignment = Alignment.trailing).Compose(composectx) // Feste Breite für Stats

                            // Sale Toggle
                            Toggle(isOn = Binding(get = { -> isSelectedForSale }, set = { it -> onToggleSale(it) })) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    // Empty label
                                    ComposeResult.ok
                                }
                            }
                            .scaleEffect(0.8)
                            .frame(width = 50.0).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding(Edge.Set.vertical, 4.0).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .sheet(isPresented = Binding({ _showingPlayerDetail.wrappedValue }, { it -> _showingPlayerDetail.wrappedValue = it })) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    kickbaseManager.selectedLeague?.let { league ->
                        PlayerDetailView(player = teamPlayer)
                            .environmentObject(kickbaseManager)
                            .environmentObject(ligainsiderService).Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }.Compose(composectx)
            ComposeResult.ok
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

    private constructor(teamPlayer: Player, isSelectedForSale: Boolean, onToggleSale: (Boolean) -> Unit, showingPlayerDetail: Boolean = false, privatep: Nothing? = null) {
        this.teamPlayer = teamPlayer
        this.isSelectedForSale = isSelectedForSale
        this.onToggleSale = onToggleSale
        this._showingPlayerDetail = skip.ui.State(showingPlayerDetail)
    }

    constructor(teamPlayer: Player, isSelectedForSale: Boolean, onToggleSale: (Boolean) -> Unit): this(teamPlayer = teamPlayer, isSelectedForSale = isSelectedForSale, onToggleSale = onToggleSale, privatep = null) {
    }
}

internal class MarketTab: View {
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    private var searchText: String
        get() = _searchText.wrappedValue
        set(newValue) {
            _searchText.wrappedValue = newValue
        }
    private var _searchText: skip.ui.State<String>
    private var selectedPosition: Int
        get() = _selectedPosition.wrappedValue
        set(newValue) {
            _selectedPosition.wrappedValue = newValue
        }
    private var _selectedPosition: skip.ui.State<Int> // 0 = All, 1 = TW, 2 = ABW, 3 = MF, 4 = ST
    private var isManuallyLoading: Boolean
        get() = _isManuallyLoading.wrappedValue
        set(newValue) {
            _isManuallyLoading.wrappedValue = newValue
        }
    private var _isManuallyLoading: skip.ui.State<Boolean>
    private var forceRefreshId: UUID
        get() = _forceRefreshId.wrappedValue
        set(newValue) {
            _forceRefreshId.wrappedValue = newValue
        }
    private var _forceRefreshId: skip.ui.State<UUID>

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            NavigationStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    VStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Debug Info (nur in Debug-Builds)

                            // Filter Controls
                            VStack(spacing = 10.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    HStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Image(systemName = "magnifyingglass")
                                                .foregroundColor(Color.gray).Compose(composectx)
                                            TextField(LocalizedStringKey(stringLiteral = "Spieler oder Verein suchen..."), text = Binding({ _searchText.wrappedValue }, { it -> _searchText.wrappedValue = it })).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    ScrollView(Axis.Set.horizontal, showsIndicators = false) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            HStack(spacing = 10.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    FilterButton(title = "Alle", isSelected = selectedPosition == 0) { -> selectedPosition = 0 }.Compose(composectx)
                                                    FilterButton(title = "TW", isSelected = selectedPosition == 1) { -> selectedPosition = 1 }.Compose(composectx)
                                                    FilterButton(title = "ABW", isSelected = selectedPosition == 2) { -> selectedPosition = 2 }.Compose(composectx)
                                                    FilterButton(title = "MF", isSelected = selectedPosition == 3) { -> selectedPosition = 3 }.Compose(composectx)
                                                    FilterButton(title = "ST", isSelected = selectedPosition == 4) { -> selectedPosition = 4 }.Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }
                                            .padding(Edge.Set.horizontal).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .padding(Edge.Set.horizontal).Compose(composectx)

                            // Market Players List or Empty State
                            if (kickbaseManager.isLoading || isManuallyLoading) {
                                VStack(spacing = 20.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Spacer().Compose(composectx)
                                        ProgressView(LocalizedStringKey(stringLiteral = "Lade Transfermarkt...")).Compose(composectx)
                                        Text(LocalizedStringKey(stringLiteral = "Ladevorgang läuft..."))
                                            .font(Font.caption)
                                            .foregroundColor(Color.secondary).Compose(composectx)
                                        Spacer().Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)
                            } else {
                                val matchtarget_0 = kickbaseManager.errorMessage
                                if (matchtarget_0 != null) {
                                    val error = matchtarget_0
                                    VStack(spacing = 20.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Spacer().Compose(composectx)

                                            Image(systemName = "exclamationmark.triangle.fill")
                                                .font(Font.system(size = 60.0))
                                                .foregroundColor(Color.red).Compose(composectx)

                                            Text(LocalizedStringKey(stringLiteral = "Fehler beim Laden"))
                                                .font(Font.headline)
                                                .foregroundColor(Color.primary).Compose(composectx)

                                            Text(error)
                                                .font(Font.subheadline)
                                                .foregroundColor(Color.secondary)
                                                .multilineTextAlignment(TextAlignment.center)
                                                .padding(Edge.Set.horizontal).Compose(composectx)

                                            Button(LocalizedStringKey(stringLiteral = "Erneut versuchen")) { -> manualReload() }
                                                .buttonStyle(ButtonStyle.borderedProminent)
                                                .disabled(kickbaseManager.selectedLeague == null).Compose(composectx)

                                            Spacer().Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                } else if (kickbaseManager.marketPlayers.isEmpty) {
                                    VStack(spacing = 20.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Spacer().Compose(composectx)

                                            Image(systemName = "cart.fill")
                                                .font(Font.system(size = 60.0))
                                                .foregroundColor(Color.gray).Compose(composectx)

                                            Text(LocalizedStringKey(stringLiteral = "Keine Transfermarkt-Spieler verfügbar"))
                                                .font(Font.headline)
                                                .foregroundColor(Color.primary).Compose(composectx)

                                            Text(LocalizedStringKey(stringLiteral = "Derzeit sind keine Spieler auf dem Transfermarkt"))
                                                .font(Font.subheadline)
                                                .foregroundColor(Color.secondary)
                                                .multilineTextAlignment(TextAlignment.center)
                                                .padding(Edge.Set.horizontal).Compose(composectx)

                                            Button(LocalizedStringKey(stringLiteral = "Aktualisieren")) { -> manualReload() }
                                                .buttonStyle(ButtonStyle.borderedProminent)
                                                .disabled(kickbaseManager.selectedLeague == null).Compose(composectx)

                                            Spacer().Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                } else {
                                    // Market Players List
                                    List(filteredMarketPlayers, id = { it.id }) { player ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            MarketPlayerRow(marketPlayer = player)
                                                .id("${player.id}-${forceRefreshId}").Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }
                                    .id(forceRefreshId)
                                    .refreshable { -> MainActor.run { performRefresh() } }.Compose(composectx)
                                }
                            }
                            ComposeResult.ok
                        }
                    }
                    .navigationTitle({
                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                        str.appendLiteral("Transfermarkt (")
                        str.appendInterpolation(kickbaseManager.marketPlayers.count)
                        str.appendLiteral(")")
                        LocalizedStringKey(stringInterpolation = str)
                    }())
                    .onAppear { ->
                        print("🎯 MarketTab appeared")
                        print("   - Market players count: ${kickbaseManager.marketPlayers.count}")
                        print("   - Selected league: ${kickbaseManager.selectedLeague?.name ?: "None"}")
                        print("   - Is loading: ${kickbaseManager.isLoading}")

                        // Force initial load if needed
                        if (kickbaseManager.marketPlayers.isEmpty && !kickbaseManager.isLoading) {
                            print("🔄 MarketTab: No market players found, triggering reload...")
                            Task(isMainActor = true) { -> performInitialLoad() }
                        }
                    }
                    .onChange(of = kickbaseManager.selectedLeague) { oldLeague, newLeague ->
                        print("🔄 MarketTab: League changed from ${oldLeague?.name ?: "None"} to ${newLeague?.name ?: "None"}")
                        if (newLeague != null) {
                            Task(isMainActor = true) { -> performInitialLoad() }
                        }
                    }
                    .onChange(of = kickbaseManager.marketPlayers) { oldPlayers, newPlayers ->
                        print("🔄 MarketTab: Market players changed from ${oldPlayers.count} to ${newPlayers.count}")
                        forceRefreshId = UUID()
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedsearchText by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<String>, Any>) { mutableStateOf(_searchText) }
        _searchText = rememberedsearchText

        val rememberedselectedPosition by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Int>, Any>) { mutableStateOf(_selectedPosition) }
        _selectedPosition = rememberedselectedPosition

        val rememberedisManuallyLoading by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_isManuallyLoading) }
        _isManuallyLoading = rememberedisManuallyLoading

        val rememberedforceRefreshId by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<UUID>, Any>) { mutableStateOf(_forceRefreshId) }
        _forceRefreshId = rememberedforceRefreshId

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!

        return super.Evaluate(context, options)
    }

    private suspend fun performInitialLoad(): Unit = Async.run l@{
        val league_1 = kickbaseManager.selectedLeague
        if (league_1 == null) {
            print("❌ MarketTab: No league selected for initial load")
            return@l
        }

        print("🔄 MarketTab: Starting initial load for league ${league_1.name}")
        kickbaseManager.loadMarketPlayers(for_ = league_1)
        print("✅ MarketTab: Initial load completed. Market players count: ${kickbaseManager.marketPlayers.count}")
    }

    private suspend fun performRefresh(): Unit = Async.run l@{
        val league_2 = kickbaseManager.selectedLeague
        if (league_2 == null) {
            print("❌ MarketTab: No league selected for refresh")
            return@l
        }

        print("🔄 MarketTab: Starting refresh for league ${league_2.name}")
        kickbaseManager.loadMarketPlayers(for_ = league_2)
        print("✅ MarketTab: Refresh completed. Market players count: ${kickbaseManager.marketPlayers.count}")
    }

    private fun manualReload() {
        val league_3 = kickbaseManager.selectedLeague
        if (league_3 == null) {
            print("❌ MarketTab: No league selected for manual reload")
            return
        }

        isManuallyLoading = true
        print("🔄 MarketTab: Starting manual reload for league ${league_3.name}")

        Task { ->
            kickbaseManager.loadMarketPlayers(for_ = league_3)
            MainActor.run { ->
                isManuallyLoading = false
                print("✅ MarketTab: Manual reload completed. Market players count: ${kickbaseManager.marketPlayers.count}")
            }
        }
    }

    private val filteredMarketPlayers: Array<MarketPlayer>
        get() {
            return kickbaseManager.marketPlayers.filter l@{ player ->
                val matchesSearch = searchText.isEmpty || player.firstName.lowercased().contains(searchText.lowercased()) || player.lastName.lowercased().contains(searchText.lowercased()) || player.fullTeamName.lowercased().contains(searchText.lowercased())

                val matchesPosition = selectedPosition == 0 || player.position == selectedPosition

                return@l matchesSearch && matchesPosition
            }
            .sorted { it, it_1 -> it.price < it_1.price }
        }

    private constructor(searchText: String = "", selectedPosition: Int = 0, isManuallyLoading: Boolean = false, forceRefreshId: UUID = UUID(), privatep: Nothing? = null) {
        this._searchText = skip.ui.State(searchText)
        this._selectedPosition = skip.ui.State(selectedPosition)
        this._isManuallyLoading = skip.ui.State(isManuallyLoading)
        this._forceRefreshId = skip.ui.State(forceRefreshId)
    }

    constructor(): this(privatep = null) {
    }
}

internal class MarketPlayerRow: View {
    internal val marketPlayer: MarketPlayer
    private var showingPlayerDetail: Boolean
        get() = _showingPlayerDetail.wrappedValue
        set(newValue) {
            _showingPlayerDetail.wrappedValue = newValue
        }
    private var _showingPlayerDetail: skip.ui.State<Boolean>
    internal var ligainsiderService: LigainsiderService
        get() = _ligainsiderService.wrappedValue
        set(newValue) {
            _ligainsiderService.wrappedValue = newValue
        }
    internal var _ligainsiderService = skip.ui.Environment<LigainsiderService>()

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            Button(action = { ->
                print("🔄 MarketPlayerRow: Tapped on player ${marketPlayer.fullName}")
                showingPlayerDetail = true
            }) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    HStack(spacing = 12.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Position indicator
                            VStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(marketPlayer.positionName)
                                        .font(Font.caption2)
                                        .fontWeight(Font.Weight.bold)
                                        .foregroundColor(Color.white)
                                        .padding(Edge.Set.horizontal, 6.0)
                                        .padding(Edge.Set.vertical, 2.0)
                                        .background(positionColor(marketPlayer.position))
                                        .cornerRadius(4.0).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .frame(minWidth = 40.0).Compose(composectx)

                            // Player Info - erweiterte Breite für Namen
                            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    HStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(marketPlayer.fullName)
                                                .font(Font.headline)
                                                .lineLimit(2).Compose(composectx) // Erlaubt 2 Zeilen für längere Namen

                                            // Ligainsider Status Icon
                                            if (!ligainsiderService.matches.isEmpty) {
                                                val status = ligainsiderService.getPlayerStatus(firstName = marketPlayer.firstName, lastName = marketPlayer.lastName)
                                                if (status != LigainsiderStatus.out) {
                                                    Image(systemName = ligainsiderService.getIcon(for_ = status))
                                                        .foregroundColor(ligainsiderService.getColor(for_ = status))
                                                        .font(Font.caption).Compose(composectx)
                                                }
                                            }

                                            // Status-Icons basierend auf status-Feld aus API-Daten anzeigen
                                            if (marketPlayer.status == 1) {
                                                // Verletzt - rotes Kreuz
                                                Image(systemName = "cross.circle.fill")
                                                    .foregroundColor(Color.red)
                                                    .font(Font.caption).Compose(composectx)
                                            } else if (marketPlayer.status == 2) {
                                                // Angeschlagen - Tabletten-Icon
                                                Image(systemName = "pills.fill")
                                                    .foregroundColor(Color.orange)
                                                    .font(Font.caption).Compose(composectx)
                                            } else if (marketPlayer.status == 4) {
                                                // Aufbautraining - Hantel-Icon
                                                Image(systemName = "dumbbell.fill")
                                                    .foregroundColor(Color.blue)
                                                    .font(Font.caption).Compose(composectx)
                                            }
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    HStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(marketPlayer.fullTeamName)
                                                .font(Font.caption)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            Spacer().Compose(composectx)

                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendLiteral("Von: ")
                                                str.appendInterpolation(marketPlayer.seller.name)
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.caption2)
                                                .foregroundColor(Color.secondary).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .frame(minWidth = 180.0, alignment = Alignment.leading).Compose(composectx) // Mindestbreite für Namensbereich (etwas größer wegen Verkäufer-Info)

                            Spacer(minLength = 8.0).Compose(composectx) // Reduzierter Mindestabstand

                            // Price and Market Value
                            VStack(alignment = HorizontalAlignment.trailing, spacing = 2.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendLiteral("€")
                                        str.appendInterpolation(marketPlayer.price / 1_000_000)
                                        str.appendLiteral("M")
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.subheadline)
                                        .fontWeight(Font.Weight.semibold)
                                        .foregroundColor(Color.green).Compose(composectx)

                                    HStack(spacing = 2.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendLiteral("MW: €")
                                                str.appendInterpolation(marketPlayer.marketValue / 1_000_000)
                                                str.appendLiteral("M")
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.caption2)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            if (marketPlayer.marketValueTrend > 0) {
                                                Image(systemName = "arrow.up")
                                                    .foregroundColor(Color.green)
                                                    .font(Font.caption2).Compose(composectx)
                                            } else if (marketPlayer.marketValueTrend < 0) {
                                                Image(systemName = "arrow.down")
                                                    .foregroundColor(Color.red)
                                                    .font(Font.caption2).Compose(composectx)
                                            }
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .frame(minWidth = 80.0, alignment = Alignment.trailing).Compose(composectx) // Feste Breite für Preis/Marktwert
                            ComposeResult.ok
                        }
                    }
                    .padding(Edge.Set.vertical, 4.0).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .sheet(isPresented = Binding({ _showingPlayerDetail.wrappedValue }, { it -> _showingPlayerDetail.wrappedValue = it })) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    MarketPlayerDetailView(player = marketPlayer)
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

        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    private constructor(marketPlayer: MarketPlayer, showingPlayerDetail: Boolean = false, privatep: Nothing? = null) {
        this.marketPlayer = marketPlayer
        this._showingPlayerDetail = skip.ui.State(showingPlayerDetail)
    }

    constructor(marketPlayer: MarketPlayer): this(marketPlayer = marketPlayer, privatep = null) {
    }
}

internal class FilterButton: View {
    internal val title: String
    internal val isSelected: Boolean
    internal val action: () -> Unit

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            Button(action = action) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(title)
                        .font(Font.caption)
                        .fontWeight(Font.Weight.medium)
                        .padding(Edge.Set.horizontal, 12.0)
                        .padding(Edge.Set.vertical, 6.0)
                        .background(if (isSelected) Color.blue else Color.systemGray5Compat)
                        .foregroundColor(if (isSelected) Color.white else Color.primary)
                        .cornerRadius(8.0).Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    constructor(title: String, isSelected: Boolean, action: () -> Unit) {
        this.title = title
        this.isSelected = isSelected
        this.action = action
    }
}

internal class MarketPlayerDetailView: View {
    internal val player: MarketPlayer
    private lateinit var dismiss: DismissAction

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            NavigationStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    ScrollView { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            VStack(alignment = HorizontalAlignment.leading, spacing = 20.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    // Player Header
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 10.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            HStack { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(player.fullName)
                                                        .font(Font.largeTitle)
                                                        .fontWeight(Font.Weight.bold).Compose(composectx)

                                                    Spacer().Compose(composectx)

                                                    Text(positionAbbreviation(player.position))
                                                        .font(Font.headline)
                                                        .fontWeight(Font.Weight.bold)
                                                        .foregroundColor(Color.white)
                                                        .padding(Edge.Set.horizontal, 12.0)
                                                        .padding(Edge.Set.vertical, 6.0)
                                                        .background(positionColor(player.position))
                                                        .cornerRadius(8.0).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            Text(player.fullTeamName)
                                                .font(Font.title3)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            if (player.status == 1) {
                                                HStack { ->
                                                    ComposeBuilder { composectx: ComposeContext ->
                                                        Image(systemName = "cross.circle.fill")
                                                            .foregroundColor(Color.red).Compose(composectx)
                                                        Text(LocalizedStringKey(stringLiteral = "Verletzt"))
                                                            .foregroundColor(Color.red).Compose(composectx)
                                                        ComposeResult.ok
                                                    }
                                                }
                                                .font(Font.caption).Compose(composectx)
                                            } else if (player.status == 2) {
                                                HStack { ->
                                                    ComposeBuilder { composectx: ComposeContext ->
                                                        Image(systemName = "pills.fill")
                                                            .foregroundColor(Color.orange).Compose(composectx)
                                                        Text(LocalizedStringKey(stringLiteral = "Angeschlagen"))
                                                            .foregroundColor(Color.orange).Compose(composectx)
                                                        ComposeResult.ok
                                                    }
                                                }
                                                .font(Font.caption).Compose(composectx)
                                            } else if (player.status == 4) {
                                                HStack { ->
                                                    ComposeBuilder { composectx: ComposeContext ->
                                                        Image(systemName = "dumbbell.fill")
                                                            .foregroundColor(Color.blue).Compose(composectx)
                                                        Text(LocalizedStringKey(stringLiteral = "Aufbautraining"))
                                                            .foregroundColor(Color.blue).Compose(composectx)
                                                        ComposeResult.ok
                                                    }
                                                }
                                                .font(Font.caption).Compose(composectx)
                                            }
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    Divider().Compose(composectx)

                                    // Market Info
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 15.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Transferinformationen"))
                                                .font(Font.headline).Compose(composectx)

                                            HStack { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    VStack(alignment = HorizontalAlignment.leading) { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            Text(LocalizedStringKey(stringLiteral = "Preis"))
                                                                .font(Font.caption)
                                                                .foregroundColor(Color.secondary).Compose(composectx)
                                                            Text({
                                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                                str.appendLiteral("€")
                                                                str.appendInterpolation(player.price / 1_000_000)
                                                                str.appendLiteral("M")
                                                                LocalizedStringKey(stringInterpolation = str)
                                                            }())
                                                                .font(Font.title2)
                                                                .fontWeight(Font.Weight.semibold)
                                                                .foregroundColor(Color.green).Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }.Compose(composectx)

                                                    Spacer().Compose(composectx)

                                                    VStack(alignment = HorizontalAlignment.trailing) { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            Text(LocalizedStringKey(stringLiteral = "Marktwert"))
                                                                .font(Font.caption)
                                                                .foregroundColor(Color.secondary).Compose(composectx)
                                                            HStack { ->
                                                                ComposeBuilder { composectx: ComposeContext ->
                                                                    Text({
                                                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                                        str.appendLiteral("€")
                                                                        str.appendInterpolation(player.marketValue / 1_000_000)
                                                                        str.appendLiteral("M")
                                                                        LocalizedStringKey(stringInterpolation = str)
                                                                    }())
                                                                        .font(Font.title2)
                                                                        .fontWeight(Font.Weight.semibold).Compose(composectx)

                                                                    if (player.marketValueTrend > 0) {
                                                                        Image(systemName = "arrow.up")
                                                                            .foregroundColor(Color.green).Compose(composectx)
                                                                    } else if (player.marketValueTrend < 0) {
                                                                        Image(systemName = "arrow.down")
                                                                            .foregroundColor(Color.red).Compose(composectx)
                                                                    }
                                                                    ComposeResult.ok
                                                                }
                                                            }.Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }.Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            VStack(alignment = HorizontalAlignment.leading) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(LocalizedStringKey(stringLiteral = "Verkäufer"))
                                                        .font(Font.caption)
                                                        .foregroundColor(Color.secondary).Compose(composectx)
                                                    Text(player.seller.name)
                                                        .font(Font.body).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    Spacer().Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .padding().Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .navigationTitle(LocalizedStringKey(stringLiteral = "Spielerdetails"))
                    .toolbar { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            ToolbarItem(placement = ToolbarItemPlacement.navigationBarTrailingCompat) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Button(LocalizedStringKey(stringLiteral = "Schließen")) { -> dismiss() }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        this.dismiss = EnvironmentValues.shared.dismiss

        return super.Evaluate(context, options)
    }

    constructor(player: MarketPlayer) {
        this.player = player
    }
}

internal class TeamBudgetHeader: View {
    internal val currentBudget: Int
    internal val saleValue: Int

    private val totalBudget: Int
        get() = currentBudget + saleValue

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 12.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
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
                                        str.appendInterpolation(currentBudget / 1_000_000)
                                        str.appendLiteral("M")
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.headline)
                                        .fontWeight(Font.Weight.bold).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            Spacer().Compose(composectx)

                            VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Ausgewählt zum Verkauf"))
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary).Compose(composectx)
                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendLiteral("€")
                                        str.appendInterpolation(saleValue / 1_000_000)
                                        str.appendLiteral("M")
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.headline)
                                        .fontWeight(Font.Weight.bold)
                                        .foregroundColor(if (saleValue > 0) Color.blue else Color.secondary).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    // Total Budget Row
                    HStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Gesamtbudget (mit Verkäufen)"))
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary).Compose(composectx)
                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendLiteral("€")
                                        str.appendInterpolation(totalBudget / 1_000_000)
                                        str.appendLiteral("M")
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.title2)
                                        .fontWeight(Font.Weight.bold)
                                        .foregroundColor(if (saleValue > 0) Color.green else Color.primary).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            Spacer().Compose(composectx)

                            if (saleValue > 0) {
                                VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Text(LocalizedStringKey(stringLiteral = "Zusätzliches Budget"))
                                            .font(Font.caption)
                                            .foregroundColor(Color.secondary).Compose(composectx)
                                        Text({
                                            val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                            str.appendLiteral("+€")
                                            str.appendInterpolation(saleValue / 1_000_000)
                                            str.appendLiteral("M")
                                            LocalizedStringKey(stringInterpolation = str)
                                        }())
                                            .font(Font.title2)
                                            .fontWeight(Font.Weight.bold)
                                            .foregroundColor(Color.green).Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)
                            }
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

    constructor(currentBudget: Int, saleValue: Int) {
        this.currentBudget = currentBudget
        this.saleValue = saleValue
    }
}
