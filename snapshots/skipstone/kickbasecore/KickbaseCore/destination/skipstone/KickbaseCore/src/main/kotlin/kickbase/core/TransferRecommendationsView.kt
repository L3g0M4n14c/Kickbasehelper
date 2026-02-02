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

internal class TransferRecommendationsView: View {
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager: skip.ui.Bindable<KickbaseManager>
    internal var ligainsiderService: LigainsiderService
        get() = _ligainsiderService.wrappedValue
        set(newValue) {
            _ligainsiderService.wrappedValue = newValue
        }
    internal var _ligainsiderService = skip.ui.Environment<LigainsiderService>()
    private var recommendationService: PlayerRecommendationService
        get() = _recommendationService.wrappedValue
        set(newValue) {
            _recommendationService.wrappedValue = newValue
        }
    private var _recommendationService: skip.ui.State<PlayerRecommendationService>
    private var recommendations: Array<TransferRecommendation>
        get() = _recommendations.wrappedValue.sref({ this.recommendations = it })
        set(newValue) {
            _recommendations.wrappedValue = newValue.sref()
        }
    private var _recommendations: skip.ui.State<Array<TransferRecommendation>> = skip.ui.State(arrayOf())
    private var teamAnalysis: TeamAnalysis?
        get() = _teamAnalysis.wrappedValue
        set(newValue) {
            _teamAnalysis.wrappedValue = newValue
        }
    private var _teamAnalysis: skip.ui.State<TeamAnalysis?> = skip.ui.State(null)
    private var isLoading: Boolean
        get() = _isLoading.wrappedValue
        set(newValue) {
            _isLoading.wrappedValue = newValue
        }
    private var _isLoading: skip.ui.State<Boolean> = skip.ui.State(false)
    private var loadingMessage: String
        get() = _loadingMessage.wrappedValue
        set(newValue) {
            _loadingMessage.wrappedValue = newValue
        }
    private var _loadingMessage: skip.ui.State<String> = skip.ui.State("Analysiere Team und lade Empfehlungen...")
    private var errorMessage: String?
        get() = _errorMessage.wrappedValue
        set(newValue) {
            _errorMessage.wrappedValue = newValue
        }
    private var _errorMessage: skip.ui.State<String?> = skip.ui.State(null)
    private var filters: RecommendationFilters
        get() = _filters.wrappedValue.sref({ this.filters = it })
        set(newValue) {
            _filters.wrappedValue = newValue.sref()
        }
    private var _filters: skip.ui.State<RecommendationFilters> = skip.ui.State(RecommendationFilters())
    private var sortOption: RecommendationSortOption
        get() = _sortOption.wrappedValue
        set(newValue) {
            _sortOption.wrappedValue = newValue
        }
    private var _sortOption: skip.ui.State<RecommendationSortOption> = skip.ui.State(RecommendationSortOption.recommendationScore)
    private var showFilterSheet: Boolean
        get() = _showFilterSheet.wrappedValue
        set(newValue) {
            _showFilterSheet.wrappedValue = newValue
        }
    private var _showFilterSheet: skip.ui.State<Boolean> = skip.ui.State(false)
    private var selectedRecommendation: TransferRecommendation?
        get() = _selectedRecommendation.wrappedValue
        set(newValue) {
            _selectedRecommendation.wrappedValue = newValue
        }
    private var _selectedRecommendation: skip.ui.State<TransferRecommendation?> = skip.ui.State(null)

    internal constructor(kickbaseManager: KickbaseManager) {
        this._kickbaseManager = skip.ui.Bindable(kickbaseManager)
        this._recommendationService = State(wrappedValue = PlayerRecommendationService(kickbaseManager = kickbaseManager))
    }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            mainContent
                .sheet(isPresented = Binding({ _showFilterSheet.wrappedValue }, { it -> _showFilterSheet.wrappedValue = it })) { ->
                    ComposeBuilder { composectx: ComposeContext ->
                        FilterSheet(filters = Binding({ _filters.wrappedValue }, { it -> _filters.wrappedValue = it })).Compose(composectx)
                        ComposeResult.ok
                    }
                }
                .sheet(item = Binding({ _selectedRecommendation.wrappedValue }, { it -> _selectedRecommendation.wrappedValue = it })) { recommendation ->
                    ComposeBuilder { composectx: ComposeContext ->
                        PlayerDetailSheet(recommendation = recommendation).Compose(composectx)
                        ComposeResult.ok
                    }
                }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedrecommendationService by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<PlayerRecommendationService>, Any>) { mutableStateOf(_recommendationService) }
        _recommendationService = rememberedrecommendationService

        val rememberedrecommendations by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Array<TransferRecommendation>>, Any>) { mutableStateOf(_recommendations) }
        _recommendations = rememberedrecommendations

        val rememberedteamAnalysis by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<TeamAnalysis?>, Any>) { mutableStateOf(_teamAnalysis) }
        _teamAnalysis = rememberedteamAnalysis

        val rememberedisLoading by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_isLoading) }
        _isLoading = rememberedisLoading

        val rememberedloadingMessage by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<String>, Any>) { mutableStateOf(_loadingMessage) }
        _loadingMessage = rememberedloadingMessage

        val rememberederrorMessage by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<String?>, Any>) { mutableStateOf(_errorMessage) }
        _errorMessage = rememberederrorMessage

        val rememberedfilters by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<RecommendationFilters>, Any>) { mutableStateOf(_filters) }
        _filters = rememberedfilters

        val rememberedsortOption by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<RecommendationSortOption>, Any>) { mutableStateOf(_sortOption) }
        _sortOption = rememberedsortOption

        val rememberedshowFilterSheet by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_showFilterSheet) }
        _showFilterSheet = rememberedshowFilterSheet

        val rememberedselectedRecommendation by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<TransferRecommendation?>, Any>) { mutableStateOf(_selectedRecommendation) }
        _selectedRecommendation = rememberedselectedRecommendation

        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    private val sidebarContent: View
        get() {
            return VStack(spacing = 0.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    if (isLoading) {
                        VStack(spacing = 20.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                ProgressView()
                                    .scaleEffect(1.5).Compose(composectx)
                                Text(loadingMessage)
                                    .font(Font.subheadline)
                                    .foregroundColor(Color.secondary).Compose(composectx)
                                ComposeResult.ok
                            }
                        }
                        .frame(maxWidth = Double.infinity, maxHeight = Double.infinity).Compose(composectx)
                    } else if (recommendations.isEmpty) {
                        emptyStateView.Compose(composectx)
                    } else {
                        recommendationsContent.Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }
            .navigationTitle(LocalizedStringKey(stringLiteral = "Transfer-Empfehlungen"))
            .toolbar { ->
                ComposeBuilder { composectx: ComposeContext ->
                    ToolbarItem(placement = ToolbarItemPlacement.navigationBarTrailingCompat) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            HStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Menu(content = { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Button(action = { ->
                                                Task { -> loadRecommendations() }
                                            }) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Label(LocalizedStringKey(stringLiteral = "Aktualisieren"), systemImage = "arrow.clockwise").Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }
                                            .disabled(isLoading).Compose(composectx)

                                            Button(action = { ->
                                                kickbaseManager.selectedLeague?.id?.let { leagueId ->
                                                    recommendationService.clearCacheForLeague(leagueId)
                                                }
                                                Task { -> loadRecommendations() }
                                            }) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Label(LocalizedStringKey(stringLiteral = "Cache leeren & neu laden"), systemImage = "trash").Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }
                                            .disabled(isLoading).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }, label = { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Image(systemName = "ellipsis.circle").Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }).Compose(composectx)

                                    Button(action = { -> showFilterSheet = true }) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Image(systemName = "slider.horizontal.3").Compose(composectx)
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
            .task { -> Async.run { loadRecommendations() } }
            .sheet(isPresented = Binding({ _showFilterSheet.wrappedValue }, { it -> _showFilterSheet.wrappedValue = it })) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    FilterSheet(filters = Binding({ _filters.wrappedValue }, { it -> _filters.wrappedValue = it })).Compose(composectx)
                    ComposeResult.ok
                }
            }
        }

    private val mainContent: View
        get() {
            return VStack(spacing = 0.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    if (isLoading) {
                        VStack(spacing = 20.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                ProgressView()
                                    .scaleEffect(1.5).Compose(composectx)
                                Text(loadingMessage)
                                    .font(Font.subheadline)
                                    .foregroundColor(Color.secondary).Compose(composectx)
                                ComposeResult.ok
                            }
                        }
                        .frame(maxWidth = Double.infinity, maxHeight = Double.infinity).Compose(composectx)
                    } else if (recommendations.isEmpty) {
                        emptyStateView.Compose(composectx)
                    } else {
                        recommendationsContent.Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }
            .navigationTitle(LocalizedStringKey(stringLiteral = "Transfer-Empfehlungen"))
            .toolbar { ->
                ComposeBuilder { composectx: ComposeContext ->
                    ToolbarItem(placement = ToolbarItemPlacement.navigationBarTrailingCompat) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            HStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Button(action = { -> showFilterSheet = true }) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Image(systemName = "slider.horizontal.3").Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    Button(LocalizedStringKey(stringLiteral = "Aktualisieren")) { ->
                                        Task { -> loadRecommendations() }
                                    }
                                    .disabled(isLoading).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }
            .task { -> Async.run { loadRecommendations() } }
        }

    private val defaultDetailView: View
        get() {
            return VStack(spacing = 20.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Image(systemName = "person.crop.circle.badge.plus")
                        .font(Font.system(size = 80.0))
                        .foregroundColor(Color.gray).Compose(composectx)

                    Text(LocalizedStringKey(stringLiteral = "Wählen Sie eine Empfehlung"))
                        .font(Font.title2)
                        .fontWeight(Font.Weight.medium)
                        .foregroundColor(Color.secondary).Compose(composectx)

                    Text(LocalizedStringKey(stringLiteral = "Tippen Sie auf eine Transfer-Empfehlung in der Liste, um Details zu sehen."))
                        .font(Font.body)
                        .foregroundColor(Color.secondary)
                        .multilineTextAlignment(TextAlignment.center)
                        .padding(Edge.Set.horizontal).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .frame(maxWidth = Double.infinity, maxHeight = Double.infinity)
            .background(Color.systemGroupedBackgroundCompat)
        }

    private val emptyStateView: View
        get() {
            return VStack(spacing = 20.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Image(systemName = "person.crop.circle.badge.plus")
                        .font(Font.system(size = 60.0))
                        .foregroundColor(Color.gray).Compose(composectx)

                    Text(LocalizedStringKey(stringLiteral = "Keine Empfehlungen verfügbar"))
                        .font(Font.title2)
                        .fontWeight(Font.Weight.medium).Compose(composectx)

                    Text(LocalizedStringKey(stringLiteral = "Wählen Sie eine Liga aus und stellen Sie sicher, dass Transfermarkt-Daten verfügbar sind."))
                        .font(Font.body)
                        .foregroundColor(Color.secondary)
                        .multilineTextAlignment(TextAlignment.center)
                        .padding(Edge.Set.horizontal).Compose(composectx)

                    errorMessage?.let { errorMessage ->
                        Text({
                            val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                            str.appendLiteral("Fehler: ")
                            str.appendInterpolation(errorMessage)
                            LocalizedStringKey(stringInterpolation = str)
                        }())
                            .font(Font.caption)
                            .foregroundColor(Color.red)
                            .padding()
                            .background(Color.red.opacity(0.1))
                            .cornerRadius(8.0).Compose(composectx)
                    }

                    Button(LocalizedStringKey(stringLiteral = "Erneut versuchen")) { ->
                        Task { -> loadRecommendations() }
                    }
                    .buttonStyle(ButtonStyle.bordered).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding()
        }

    private val recommendationsContent: View
        get() {
            return VStack(spacing = 0.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Enhanced Team Analysis Header
                    enhancedTeamAnalysisHeader.Compose(composectx)

                    // Quick Filters
                    quickFiltersSection.Compose(composectx)

                    // Sort Options
                    sortingSection.Compose(composectx)

                    // Recommendations List
                    ScrollView { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            LazyVStack(spacing = 12.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    ForEach(filteredAndSortedRecommendations) { recommendation ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            EnhancedRecommendationCard(recommendation = recommendation) { -> selectedRecommendation = recommendation }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .padding().Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }
        }

    private val enhancedTeamAnalysisHeader: View
        get() {
            return VStack(alignment = HorizontalAlignment.leading, spacing = 12.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    HStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(LocalizedStringKey(stringLiteral = "Team-Analyse"))
                                .font(Font.headline).Compose(composectx)
                            Spacer().Compose(composectx)
                            Text({
                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                str.appendLiteral("Budget: ")
                                str.appendInterpolation(formatCurrency(kickbaseManager.selectedLeague?.currentUser?.budget ?: 0))
                                LocalizedStringKey(stringInterpolation = str)
                            }())
                                .font(Font.subheadline)
                                .foregroundColor(Color.secondary).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    val matchtarget_0 = teamAnalysis
                    if (matchtarget_0 != null) {
                        val analysis = matchtarget_0
                        // Budget Analysis
                        HStack { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Text(LocalizedStringKey(stringLiteral = "Empfohlene Ausgaben"))
                                            .font(Font.caption)
                                            .foregroundColor(Color.secondary).Compose(composectx)
                                        Text(formatCurrency(analysis.budgetConstraints.recommendedSpending))
                                            .font(Font.subheadline)
                                            .fontWeight(Font.Weight.semibold).Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)

                                Spacer().Compose(composectx)

                                VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Text(LocalizedStringKey(stringLiteral = "Reserve"))
                                            .font(Font.caption)
                                            .foregroundColor(Color.secondary).Compose(composectx)
                                        Text(formatCurrency(analysis.budgetConstraints.emergencyReserve))
                                            .font(Font.subheadline)
                                            .fontWeight(Font.Weight.semibold)
                                            .foregroundColor(Color.green).Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)
                                ComposeResult.ok
                            }
                        }.Compose(composectx)

                        // Weak Positions
                        if (!analysis.weakPositions.isEmpty) {
                            VStack(alignment = HorizontalAlignment.leading, spacing = 6.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Prioritäre Positionen"))
                                        .font(Font.caption)
                                        .fontWeight(Font.Weight.medium)
                                        .foregroundColor(Color.secondary).Compose(composectx)

                                    ScrollView(Axis.Set.horizontal, showsIndicators = false) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            HStack(spacing = 8.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    ForEach(analysis.weakPositions, id = { it }) { position ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            Text(position.rawValue)
                                                                .font(Font.caption)
                                                                .padding(Edge.Set.horizontal, 8.0)
                                                                .padding(Edge.Set.vertical, 4.0)
                                                                .background(Color.orange.opacity(0.2))
                                                                .foregroundColor(Color.orange)
                                                                .cornerRadius(8.0).Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }.Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }
                                            .padding(Edge.Set.horizontal).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                        }

                        // Stats Summary
                        HStack { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                Label({
                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                    str.appendInterpolation(recommendations.count)
                                    str.appendLiteral(" Empfehlungen")
                                    LocalizedStringKey(stringInterpolation = str)
                                }(), systemImage = "person.crop.circle.badge.plus")
                                    .font(Font.caption)
                                    .foregroundColor(Color.blue).Compose(composectx)

                                Spacer().Compose(composectx)

                                val highPriorityCount = recommendations.filter { it -> it.priority == TransferRecommendation.Priority.essential }
                                    .count
                                if (highPriorityCount > 0) {
                                    Label({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendInterpolation(highPriorityCount)
                                        str.appendLiteral(" dringend")
                                        LocalizedStringKey(stringInterpolation = str)
                                    }(), systemImage = "exclamationmark.triangle.fill")
                                        .font(Font.caption)
                                        .foregroundColor(Color.red).Compose(composectx)
                                }
                                ComposeResult.ok
                            }
                        }.Compose(composectx)
                    } else {
                        // Fallback when no team analysis available
                        HStack { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                Label({
                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                    str.appendInterpolation(recommendations.count)
                                    str.appendLiteral(" Empfehlungen")
                                    LocalizedStringKey(stringInterpolation = str)
                                }(), systemImage = "person.crop.circle.badge.plus")
                                    .font(Font.caption)
                                    .foregroundColor(Color.blue).Compose(composectx)

                                Spacer().Compose(composectx)

                                val highPriorityCount = recommendations.filter { it -> it.priority == TransferRecommendation.Priority.essential }
                                    .count
                                if (highPriorityCount > 0) {
                                    Label({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendInterpolation(highPriorityCount)
                                        str.appendLiteral(" dringend")
                                        LocalizedStringKey(stringInterpolation = str)
                                    }(), systemImage = "exclamationmark.triangle.fill")
                                        .font(Font.caption)
                                        .foregroundColor(Color.red).Compose(composectx)
                                }
                                ComposeResult.ok
                            }
                        }.Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }
            .padding()
            .background(Color.systemGray6Compat)
            .cornerRadius(12.0)
            .padding(Edge.Set.horizontal)
        }

    private val quickFiltersSection: View
        get() {
            return Text(LocalizedStringKey(stringLiteral = "Filters Disabled"))
                .padding()
        }

    private val sortingSection: View
        get() {
            /*
            Picker("Sortierung", selection: $sortOption) {
            Text("Empfehlungswert").tag(RecommendationSortOption.recommendationScore)
            Text("Preis").tag(RecommendationSortOption.price)
            Text("Punkte").tag(RecommendationSortOption.points)
            Text("Preis-Leistung").tag(RecommendationSortOption.valueForMoney)
            Text("Form-Trend").tag(RecommendationSortOption.formTrend)
            Text("Risiko").tag(RecommendationSortOption.risk)
            }
            #if os(iOS)
            .pickerStyle(SegmentedPickerStyle())
            #endif
            .padding(.horizontal)
            .padding(.bottom, 8)
            */
            return EmptyView()
        }

    private val filteredAndSortedRecommendations: Array<TransferRecommendation>
        get() {
            print("🔧 [DEBUG] Filtering ${recommendations.count} recommendations")
            print("🔧 [DEBUG] Current filters:")
            print("   - Positions: ${filters.positions}")
            print("   - Max Risk: ${filters.maxRisk}")
            print("   - Min Priority: ${filters.minPriority}")
            print("   - Form Trend: ${filters.formTrend?.rawValue ?: "nil"}")
            print("   - Max Price: ${filters.maxPrice ?: 0}")
            print("   - Min Points: ${filters.minPoints ?: 0}")
            print("   - Min Confidence: ${filters.minConfidence ?: 0.0}")

            val filtered = recommendations.filter l@{ recommendation ->
                print("🔧 [DEBUG] Checking recommendation: ${recommendation.player.firstName} ${recommendation.player.lastName}")
                print("   - Position: ${recommendation.player.position}")
                print("   - Risk Level: ${recommendation.riskLevel.rawValue}")
                print("   - Priority: ${recommendation.priority.rawValue}")
                print("   - Form Trend: ${recommendation.analysis.formTrend.rawValue}")

                // Position filter
                if (!filters.positions.isEmpty) {
                    val playerPosition = mapIntToPosition(recommendation.player.position)
                    if (playerPosition != null) {
                        if (!filters.positions.contains(playerPosition)) {
                            print("   ❌ Failed position filter")
                            return@l false
                        }
                    }
                }

                // Risk filter - Korrigiert: Direkte Enum-Vergleiche
                if (!isRiskLevelAcceptable(recommendation.riskLevel, maxRisk = filters.maxRisk)) {
                    print("   ❌ Failed risk filter (${recommendation.riskLevel.rawValue} > ${filters.maxRisk.rawValue})")
                    return@l false
                }

                // Priority filter - Korrigiert: Direkte Enum-Vergleiche
                if (!isPriorityAcceptable(recommendation.priority, minPriority = filters.minPriority)) {
                    print("   ❌ Failed priority filter (${recommendation.priority.rawValue} < ${filters.minPriority.rawValue})")
                    return@l false
                }

                // Form trend filter
                filters.formTrend?.let { formTrend ->
                    if (recommendation.analysis.formTrend != formTrend) {
                        print("   ❌ Failed form trend filter")
                        return@l false
                    }
                }

                // Price filter
                filters.maxPrice?.let { maxPrice ->
                    if (recommendation.player.price > maxPrice) {
                        print("   ❌ Failed price filter")
                        return@l false
                    }
                }

                // Points filter
                filters.minPoints?.let { minPoints ->
                    if (recommendation.player.totalPoints < minPoints) {
                        print("   ❌ Failed points filter")
                        return@l false
                    }
                }

                // Confidence filter
                filters.minConfidence?.let { minConfidence ->
                    if (recommendation.analysis.seasonProjection.confidence < minConfidence) {
                        print("   ❌ Failed confidence filter")
                        return@l false
                    }
                }

                print("   ✅ Passed all filters")
                return@l true
            }

            print("🔧 [DEBUG] Filtered to ${filtered.count} recommendations")

            return filtered.sorted l@{ first, second ->
                when (sortOption) {
                    RecommendationSortOption.recommendationScore -> return@l first.recommendationScore > second.recommendationScore
                    RecommendationSortOption.price -> return@l first.player.price < second.player.price
                    RecommendationSortOption.points -> return@l first.player.totalPoints > second.player.totalPoints
                    RecommendationSortOption.valueForMoney -> return@l first.analysis.valueForMoney > second.analysis.valueForMoney
                    RecommendationSortOption.formTrend -> return@l first.analysis.formTrend.rawValue < second.analysis.formTrend.rawValue
                    RecommendationSortOption.risk -> return@l getRiskLevelOrder(first.riskLevel) < getRiskLevelOrder(second.riskLevel)
                }
            }
        }

    // MARK: - Helper Functions für Enum-Vergleiche

    private fun isRiskLevelAcceptable(riskLevel: TransferRecommendation.RiskLevel, maxRisk: TransferRecommendation.RiskLevel): Boolean = getRiskLevelOrder(riskLevel) <= getRiskLevelOrder(maxRisk)

    private fun isPriorityAcceptable(priority: TransferRecommendation.Priority, minPriority: TransferRecommendation.Priority): Boolean = getPriorityOrder(priority) >= getPriorityOrder(minPriority)

    private fun getRiskLevelOrder(risk: TransferRecommendation.RiskLevel): Int {
        when (risk) {
            TransferRecommendation.RiskLevel.low -> return 1
            TransferRecommendation.RiskLevel.medium -> return 2
            TransferRecommendation.RiskLevel.high -> return 3
        }
    }

    private fun getPriorityOrder(priority: TransferRecommendation.Priority): Int {
        when (priority) {
            TransferRecommendation.Priority.optional -> return 1
            TransferRecommendation.Priority.recommended -> return 2
            TransferRecommendation.Priority.essential -> return 3
        }
    }

    private fun mapIntToPosition(position: Int): TeamAnalysis.Position? {
        when (position) {
            1 -> return TeamAnalysis.Position.goalkeeper
            2 -> return TeamAnalysis.Position.defender
            3 -> return TeamAnalysis.Position.midfielder
            4 -> return TeamAnalysis.Position.striker
            else -> return null
        }
    }

    private suspend fun loadRecommendations(): Unit = Async.run l@{
        val selectedLeague_0 = kickbaseManager.selectedLeague
        if (selectedLeague_0 == null) {
            errorMessage = "Keine Liga ausgewählt"
            return@l
        }

        isLoading = true
        errorMessage = null

        try {
            loadingMessage = "Lade Spieldaten..."
            val budget = selectedLeague_0.currentUser.budget
            print("🎯 Loading recommendations with budget: ${budget}")

            loadingMessage = "Analysiere Spieler..."
            val results = recommendationService.generateRecommendations(for_ = selectedLeague_0, budget = budget)

            loadingMessage = "Bereite Empfehlungen vor..."

            MainActor.run { ->
                this.recommendations = results
                this.isLoading = false
                print("✅ Loaded ${results.count} recommendations")
            }
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            MainActor.run { ->
                this.errorMessage = error.localizedDescription
                this.isLoading = false
                print("❌ Error loading recommendations: ${error}")
            }
        }
    }

    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormatter()
        formatter.numberStyle = NumberFormatter.Style.currency
        formatter.locale = Locale(identifier = "de_DE")
        formatter.currencyCode = "EUR"
        return formatter.string(from = NSNumber(value = Double(amount))) ?: "€0"
    }
}

// MARK: - Enhanced Recommendation Card

internal class EnhancedRecommendationCard: View {
    internal val recommendation: TransferRecommendation
    internal val onTap: () -> Unit
    internal var ligainsiderService: LigainsiderService
        get() = _ligainsiderService.wrappedValue
        set(newValue) {
            _ligainsiderService.wrappedValue = newValue
        }
    internal var _ligainsiderService = skip.ui.Environment<LigainsiderService>()

    private val playerImageUrl: URL?
        get() {
            val ligaPlayer_0 = ligainsiderService.getLigainsiderPlayer(firstName = recommendation.player.firstName, lastName = recommendation.player.lastName)
            if (ligaPlayer_0 == null) {
                return null
            }
            val imgString_0 = ligaPlayer_0.imageUrl
            if (imgString_0 == null) {
                return null
            }
            val url_0 = (try { URL(string = imgString_0) } catch (_: NullReturnException) { null })
            if (url_0 == null) {
                return null
            }
            return url_0
        }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(alignment = HorizontalAlignment.leading, spacing = 14.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Player Header with Enhanced Info
                    HStack(alignment = VerticalAlignment.top, spacing = 12.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Player Photo from Ligainsider
                            playerImageUrl.sref()?.let { url ->
                                Circle()
                                    .fill(positionColor(for_ = recommendation.player.position).opacity(0.3))
                                    .frame(width = 50.0, height = 50.0)
                                    .overlay { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Image(systemName = "person.fill")
                                                .foregroundColor(positionColor(for_ = recommendation.player.position)).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                            }

                            VStack(alignment = HorizontalAlignment.leading, spacing = 6.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    // Name with Form Trend
                                    HStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(recommendation.player.firstName + " " + recommendation.player.lastName)
                                                .font(Font.headline)
                                                .fontWeight(Font.Weight.semibold)
                                                .foregroundColor(Color.primary).Compose(composectx)

                                            // Ligainsider Status Icon
                                            val status = ligainsiderService.getPlayerStatus(firstName = recommendation.player.firstName, lastName = recommendation.player.lastName)
                                            if (status != LigainsiderStatus.out) {
                                                Image(systemName = ligainsiderService.getIcon(for_ = status))
                                                    .foregroundColor(ligainsiderService.getColor(for_ = status))
                                                    .font(Font.caption).Compose(composectx)
                                            }

                                            // Form Trend Indicator
                                            FormTrendBadge(trend = recommendation.analysis.formTrend).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    // Team Name
                                    Text(recommendation.player.teamName)
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary).Compose(composectx)

                                    // All Badges in one row (Position, Risk, Priority)
                                    HStack(spacing = 6.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            // Position Badge
                                            Text(positionName(for_ = recommendation.player.position))
                                                .font(Font.caption)
                                                .fontWeight(Font.Weight.medium)
                                                .padding(Edge.Set.horizontal, 8.0)
                                                .padding(Edge.Set.vertical, 3.0)
                                                .background(positionColor(for_ = recommendation.player.position))
                                                .foregroundColor(Color.white)
                                                .cornerRadius(6.0).Compose(composectx)

                                            RiskBadge(risk = recommendation.riskLevel).Compose(composectx)
                                            PriorityBadge(priority = recommendation.priority).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            Spacer().Compose(composectx)

                            VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(formatPrice(recommendation.player.price))
                                        .font(Font.headline)
                                        .fontWeight(Font.Weight.bold)
                                        .foregroundColor(Color.primary).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .onTapGesture { it -> onTap() }.Compose(composectx)

                    // Enhanced Stats Section
                    VStack(spacing = 8.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            HStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    EnhancedStatItem(title = "Punkte", value = "${recommendation.player.totalPoints}", icon = "target").Compose(composectx)
                                    EnhancedStatItem(title = "Ø Punkte", value = String(format = "%.1f", recommendation.analysis.pointsPerGame), icon = "chart.bar").Compose(composectx)
                                    EnhancedStatItem(title = "Wert/€M", value = String(format = "%.1f", recommendation.analysis.valueForMoney), icon = "eurosign.circle").Compose(composectx)
                                    EnhancedStatItem(title = "Score", value = String(format = "%.1f", recommendation.recommendationScore), icon = "star.fill").Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            // Season Projection
                            HStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Saisonprognose"))
                                                .font(Font.caption2)
                                                .fontWeight(Font.Weight.medium)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendInterpolation(recommendation.analysis.seasonProjection.projectedTotalPoints)
                                                str.appendLiteral(" Pkt.")
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.caption)
                                                .fontWeight(Font.Weight.medium).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    Spacer().Compose(composectx)

                                    // Confidence Indicator
                                    HStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Vertrauen:"))
                                                .font(Font.caption2)
                                                .foregroundColor(Color.secondary).Compose(composectx)
                                            ConfidenceBadge(confidence = recommendation.analysis.seasonProjection.confidence).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding(Edge.Set.top, 4.0).Compose(composectx)

                    // Enhanced Reasons Section
                    if (!recommendation.reasons.isEmpty) {
                        VStack(alignment = HorizontalAlignment.leading, spacing = 6.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                Text(LocalizedStringKey(stringLiteral = "Gründe für die Empfehlung:"))
                                    .font(Font.caption)
                                    .fontWeight(Font.Weight.semibold)
                                    .foregroundColor(Color.secondary).Compose(composectx)

                                ForEach(recommendation.reasons.prefix(3)) { reason ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        HStack(spacing = 8.0) { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                Image(systemName = iconForReasonType(reason.type))
                                                    .font(Font.caption)
                                                    .foregroundColor(colorForImpact(reason.impact)).Compose(composectx)

                                                Text(reason.description)
                                                    .font(Font.caption)
                                                    .foregroundColor(Color.secondary)
                                                    .lineLimit(2).Compose(composectx)

                                                Spacer().Compose(composectx)

                                                // Impact Score
                                                Text(String(format = "%.1f", reason.impact))
                                                    .font(Font.caption2)
                                                    .fontWeight(Font.Weight.medium)
                                                    .padding(Edge.Set.horizontal, 6.0)
                                                    .padding(Edge.Set.vertical, 2.0)
                                                    .background(colorForImpact(reason.impact).opacity(0.2))
                                                    .foregroundColor(colorForImpact(reason.impact))
                                                    .cornerRadius(4.0).Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)

                                if (recommendation.reasons.count > 3) {
                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendLiteral("... und ")
                                        str.appendInterpolation(recommendation.reasons.count - 3)
                                        str.appendLiteral(" weitere Gründe")
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.caption2)
                                        .foregroundColor(Color.secondary)
                                        .italic().Compose(composectx)
                                }
                                ComposeResult.ok
                            }
                        }
                        .padding(Edge.Set.top, 4.0).Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }
            .padding(16.0)
            .background(Color.systemBackgroundCompat)
            .cornerRadius(16.0)
            .shadow(color = Color.black.opacity(0.08), radius = 4.0, x = 0.0, y = 2.0)
            .overlay { ->
                ComposeBuilder { composectx: ComposeContext ->
                    ZStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            RoundedRectangle(cornerRadius = 16.0)
                                .strokeBorder(priorityBorderColor(recommendation.priority), lineWidth = 2.0)
                                .opacity(0.3).Compose(composectx)
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
        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    private fun positionName(for_: Int): String {
        val position = for_
        when (position) {
            1 -> return "TW"
            2 -> return "ABW"
            3 -> return "MF"
            4 -> return "ST"
            else -> return "?"
        }
    }

    private fun positionColor(for_: Int): Color {
        val position = for_
        when (position) {
            1 -> return Color.blue // Torwart
            2 -> return Color.green // Abwehr
            3 -> return Color.orange // Mittelfeld
            4 -> return Color.red // Sturm
            else -> return Color.gray
        }
    }

    private fun formatPrice(price: Int): String = "€${String(format = "%.1f", Double(price) / 1_000_000.0)}M"

    private fun priorityBorderColor(priority: TransferRecommendation.Priority): Color {
        when (priority) {
            TransferRecommendation.Priority.essential -> return Color.red
            TransferRecommendation.Priority.recommended -> return Color.orange
            TransferRecommendation.Priority.optional -> return Color.blue
        }
    }

    private fun iconForReasonType(type: RecommendationReason.ReasonType): String {
        when (type) {
            RecommendationReason.ReasonType.performance -> return "chart.line.uptrend.xyaxis"
            RecommendationReason.ReasonType.value_ -> return "eurosign.circle"
            RecommendationReason.ReasonType.potential -> return "star"
            RecommendationReason.ReasonType.teamNeed -> return "person.crop.circle.badge.plus"
            RecommendationReason.ReasonType.injury -> return "cross.case"
            RecommendationReason.ReasonType.form -> return "waveform.path.ecg"
            RecommendationReason.ReasonType.opponent -> return "sportscourt"
        }
    }

    private fun colorForImpact(impact: Double): Color {
        if (impact >= 7) {
            return Color.green
        } else if (impact >= 4) {
            return Color.blue
        } else if (impact >= 0) {
            return Color.orange
        } else {
            return Color.red
        }
    }

    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormatter()
        formatter.numberStyle = NumberFormatter.Style.currency
        formatter.locale = Locale(identifier = "de_DE")
        formatter.currencyCode = "EUR"
        return formatter.string(from = NSNumber(value = Double(amount))) ?: "€0"
    }

    constructor(recommendation: TransferRecommendation, onTap: () -> Unit) {
        this.recommendation = recommendation
        this.onTap = onTap
    }
}

// MARK: - Recommendation Player Detail View (für iPad/macOS NavigationSplitView)

internal class RecommendationPlayerDetailView: View {
    internal val recommendation: TransferRecommendation
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    private var marketValueHistory: MarketValueChange?
        get() = _marketValueHistory.wrappedValue
        set(newValue) {
            _marketValueHistory.wrappedValue = newValue
        }
    private var _marketValueHistory: skip.ui.State<MarketValueChange?> = skip.ui.State(null)
    private var isLoadingHistory: Boolean
        get() = _isLoadingHistory.wrappedValue
        set(newValue) {
            _isLoadingHistory.wrappedValue = newValue
        }
    private var _isLoadingHistory: skip.ui.State<Boolean>

    internal val latestDailyChange: Int
        get() {
            val history_0 = marketValueHistory
            if (history_0 == null) {
                return recommendation.analysis.seasonProjection.projectedValueIncrease
            }
            val today_0 = history_0.dailyChanges.first(where = { it -> it.daysAgo == 0 })
            if (today_0 == null) {
                return recommendation.analysis.seasonProjection.projectedValueIncrease
            }
            return today_0.change
        }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            ScrollView { ->
                ComposeBuilder { composectx: ComposeContext ->
                    VStack(alignment = HorizontalAlignment.leading, spacing = 20.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Player Header
                            VStack(alignment = HorizontalAlignment.leading, spacing = 12.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    HStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            VStack(alignment = HorizontalAlignment.leading) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(recommendation.player.firstName + " " + recommendation.player.lastName)
                                                        .font(Font.title2)
                                                        .fontWeight(Font.Weight.bold).Compose(composectx)

                                                    Text(recommendation.player.teamName)
                                                        .font(Font.subheadline)
                                                        .foregroundColor(Color.secondary).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            Spacer().Compose(composectx)

                                            VStack(alignment = HorizontalAlignment.trailing) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(formatPrice(recommendation.player.price))
                                                        .font(Font.title2)
                                                        .fontWeight(Font.Weight.bold).Compose(composectx)

                                                    HStack { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            RiskBadge(risk = recommendation.riskLevel).Compose(composectx)
                                                            PriorityBadge(priority = recommendation.priority).Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }.Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    // Score and Rating
                                    HStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            VStack(alignment = HorizontalAlignment.leading) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(LocalizedStringKey(stringLiteral = "Empfehlungswert"))
                                                        .font(Font.caption)
                                                        .foregroundColor(Color.secondary).Compose(composectx)
                                                    Text(String(format = "%.1f/24", recommendation.recommendationScore))
                                                        .font(Font.headline)
                                                        .fontWeight(Font.Weight.semibold).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            Spacer().Compose(composectx)

                                            // Progress Bar for Score
                                            ProgressView(value = recommendation.recommendationScore, total = 24.0)
                                                .progressViewStyle(ProgressViewStyle.linear)
                                                .tint(scoreColor(recommendation.recommendationScore)).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .padding()
                            .background(Color.systemGray6Compat)
                            .cornerRadius(12.0).Compose(composectx)

                            // Detailed Analysis
                            VStack(alignment = HorizontalAlignment.leading, spacing = 16.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Detaillierte Analyse"))
                                        .font(Font.headline).Compose(composectx)

                                    // Performance Stats
                                    HStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            StatDetailItem(title = "Gesamtpunkte", value = "${recommendation.player.totalPoints}").Compose(composectx)
                                            StatDetailItem(title = "Ø pro Spiel", value = String(format = "%.1f", recommendation.analysis.pointsPerGame)).Compose(composectx)
                                            StatDetailItem(title = "Preis-Leistung", value = String(format = "%.1f", recommendation.analysis.valueForMoney)).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    // Form
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Form"))
                                                .font(Font.subheadline)
                                                .fontWeight(Font.Weight.medium).Compose(composectx)

                                            HStack { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    VStack(alignment = HorizontalAlignment.leading) { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            Text(LocalizedStringKey(stringLiteral = "Form-Trend"))
                                                                .font(Font.caption)
                                                                .foregroundColor(Color.secondary).Compose(composectx)
                                                            FormTrendBadge(trend = recommendation.analysis.formTrend).Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }.Compose(composectx)

                                                    Spacer().Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    // Season Projection
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Saisonprognose"))
                                                .font(Font.subheadline)
                                                .fontWeight(Font.Weight.medium).Compose(composectx)

                                            HStack { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    VStack(alignment = HorizontalAlignment.leading) { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            Text(LocalizedStringKey(stringLiteral = "Erwartete Punkte"))
                                                                .font(Font.caption)
                                                                .foregroundColor(Color.secondary).Compose(composectx)
                                                            Text({
                                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                                str.appendInterpolation(recommendation.analysis.seasonProjection.projectedTotalPoints)
                                                                LocalizedStringKey(stringInterpolation = str)
                                                            }())
                                                                .font(Font.subheadline)
                                                                .fontWeight(Font.Weight.semibold).Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }.Compose(composectx)

                                                    Spacer().Compose(composectx)

                                                    VStack(alignment = HorizontalAlignment.trailing) { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            Text(LocalizedStringKey(stringLiteral = "Wertsteigerung (täglich)"))
                                                                .font(Font.caption)
                                                                .foregroundColor(Color.secondary).Compose(composectx)
                                                            Text(formatCurrency(latestDailyChange))
                                                                .font(Font.subheadline)
                                                                .fontWeight(Font.Weight.semibold)
                                                                .foregroundColor(if (latestDailyChange > 0) Color.green else Color.red).Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }.Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            HStack { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(LocalizedStringKey(stringLiteral = "Vertrauen:"))
                                                        .font(Font.caption)
                                                        .foregroundColor(Color.secondary).Compose(composectx)
                                                    ConfidenceBadge(confidence = recommendation.analysis.seasonProjection.confidence).Compose(composectx)
                                                    Spacer().Compose(composectx)
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

                            // All Reasons
                            VStack(alignment = HorizontalAlignment.leading, spacing = 12.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Alle Empfehlungsgründe"))
                                        .font(Font.headline).Compose(composectx)

                                    ForEach(recommendation.reasons) { reason ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            VStack(alignment = HorizontalAlignment.leading, spacing = 6.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    HStack { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            Image(systemName = iconForReasonType(reason.type))
                                                                .foregroundColor(colorForImpact(reason.impact)).Compose(composectx)
                                                            Text(reason.type.rawValue)
                                                                .font(Font.subheadline)
                                                                .fontWeight(Font.Weight.medium).Compose(composectx)
                                                            Spacer().Compose(composectx)
                                                            Text(String(format = "%.1f", reason.impact))
                                                                .font(Font.caption)
                                                                .fontWeight(Font.Weight.medium)
                                                                .padding(Edge.Set.horizontal, 6.0)
                                                                .padding(Edge.Set.vertical, 2.0)
                                                                .background(colorForImpact(reason.impact).opacity(0.2))
                                                                .foregroundColor(colorForImpact(reason.impact))
                                                                .cornerRadius(4.0).Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }.Compose(composectx)

                                                    Text(reason.description)
                                                        .font(Font.caption)
                                                        .foregroundColor(Color.secondary).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }
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
                    .padding().Compose(composectx)
                    ComposeResult.ok
                }
            }
            .navigationTitle(LocalizedStringKey(stringLiteral = "Spieler Details"))
            .task { -> MainActor.run { loadMarketValueHistory() } }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedmarketValueHistory by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<MarketValueChange?>, Any>) { mutableStateOf(_marketValueHistory) }
        _marketValueHistory = rememberedmarketValueHistory

        val rememberedisLoadingHistory by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_isLoadingHistory) }
        _isLoadingHistory = rememberedisLoadingHistory

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!

        return super.Evaluate(context, options)
    }

    private suspend fun loadMarketValueHistory(): Unit = MainActor.run l@{
        val selectedLeague_1 = kickbaseManager.selectedLeague
        if (selectedLeague_1 == null) {
            return@l
        }
        isLoadingHistory = true
        val history = kickbaseManager.loadPlayerMarketValueHistory(playerId = recommendation.player.id, leagueId = selectedLeague_1.id)
        marketValueHistory = history
        isLoadingHistory = false
    }

    private fun formatPrice(price: Int): String = "€${String(format = "%.1f", Double(price) / 1_000_000.0)}M"

    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormatter()
        formatter.numberStyle = NumberFormatter.Style.currency
        formatter.locale = Locale(identifier = "de_DE")
        formatter.currencyCode = "EUR"
        return formatter.string(from = NSNumber(value = Double(amount))) ?: "€0"
    }

    private fun scoreColor(score: Double): Color {
        if (score >= 7) {
            return Color.green
        } else if (score >= 5) {
            return Color.blue
        } else if (score >= 3) {
            return Color.orange
        } else {
            return Color.red
        }
    }

    private fun colorForInjuryRisk(risk: PlayerAnalysis.InjuryRisk): Color {
        when (risk) {
            PlayerAnalysis.InjuryRisk.low -> return Color.green
            PlayerAnalysis.InjuryRisk.medium -> return Color.orange
            PlayerAnalysis.InjuryRisk.high -> return Color.red
        }
    }

    private fun iconForReasonType(type: RecommendationReason.ReasonType): String {
        when (type) {
            RecommendationReason.ReasonType.performance -> return "chart.line.uptrend.xyaxis"
            RecommendationReason.ReasonType.value_ -> return "eurosign.circle"
            RecommendationReason.ReasonType.potential -> return "star"
            RecommendationReason.ReasonType.teamNeed -> return "person.crop.circle.badge.plus"
            RecommendationReason.ReasonType.injury -> return "cross.case"
            RecommendationReason.ReasonType.form -> return "waveform.path.ecg"
            RecommendationReason.ReasonType.opponent -> return "sportscourt"
        }
    }

    private fun colorForImpact(impact: Double): Color {
        if (impact >= 7) {
            return Color.green
        } else if (impact >= 4) {
            return Color.blue
        } else if (impact >= 0) {
            return Color.orange
        } else {
            return Color.red
        }
    }

    private constructor(recommendation: TransferRecommendation, marketValueHistory: MarketValueChange? = null, isLoadingHistory: Boolean = false, privatep: Nothing? = null) {
        this.recommendation = recommendation
        this._marketValueHistory = skip.ui.State(marketValueHistory)
        this._isLoadingHistory = skip.ui.State(isLoadingHistory)
    }

    constructor(recommendation: TransferRecommendation): this(recommendation = recommendation, privatep = null) {
    }
}

internal class StatDetailItem: View {
    internal val title: String
    internal val value: String

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(title)
                        .font(Font.caption)
                        .foregroundColor(Color.secondary).Compose(composectx)
                    Text(value)
                        .font(Font.subheadline)
                        .fontWeight(Font.Weight.semibold).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .frame(maxWidth = Double.infinity).Compose(composectx)
        }
    }

    constructor(title: String, value: String) {
        this.title = title
        this.value = value
    }
}

// MARK: - Enhanced Supporting Views

internal class EnhancedStatItem: View {
    internal val title: String
    internal val value: String
    internal val icon: String

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 2.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    HStack(spacing = 2.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Image(systemName = icon)
                                .font(Font.caption2)
                                .foregroundColor(Color.secondary).Compose(composectx)
                            Text(title)
                                .font(Font.caption2)
                                .foregroundColor(Color.secondary).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    Text(value)
                        .font(Font.caption)
                        .fontWeight(Font.Weight.semibold).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .frame(maxWidth = Double.infinity).Compose(composectx)
        }
    }

    constructor(title: String, value: String, icon: String) {
        this.title = title
        this.value = value
        this.icon = icon
    }
}

internal class FormTrendBadge: View {
    internal val trend: PlayerAnalysis.FormTrend

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            HStack(spacing = 2.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Image(systemName = iconForTrend(trend))
                        .font(Font.caption2).Compose(composectx)
                    Text(trend.rawValue)
                        .font(Font.caption2).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding(Edge.Set.horizontal, 6.0)
            .padding(Edge.Set.vertical, 2.0)
            .background(colorForTrend(trend).opacity(0.2))
            .foregroundColor(colorForTrend(trend))
            .cornerRadius(4.0).Compose(composectx)
        }
    }

    private fun iconForTrend(trend: PlayerAnalysis.FormTrend): String {
        when (trend) {
            PlayerAnalysis.FormTrend.improving -> return "arrow.up"
            PlayerAnalysis.FormTrend.stable -> return "arrow.right"
            PlayerAnalysis.FormTrend.declining -> return "arrow.down"
        }
    }

    private fun colorForTrend(trend: PlayerAnalysis.FormTrend): Color {
        when (trend) {
            PlayerAnalysis.FormTrend.improving -> return Color.green
            PlayerAnalysis.FormTrend.stable -> return Color.blue
            PlayerAnalysis.FormTrend.declining -> return Color.red
        }
    }

    constructor(trend: PlayerAnalysis.FormTrend) {
        this.trend = trend
    }
}

internal class ConfidenceBadge: View {
    internal val confidence: Double

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            Text({
                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                str.appendInterpolation(Int(confidence * 100))
                str.appendLiteral("%")
                LocalizedStringKey(stringInterpolation = str)
            }())
                .font(Font.caption2)
                .fontWeight(Font.Weight.medium)
                .padding(Edge.Set.horizontal, 6.0)
                .padding(Edge.Set.vertical, 2.0)
                .background(colorForConfidence(confidence).opacity(0.2))
                .foregroundColor(colorForConfidence(confidence))
                .cornerRadius(4.0).Compose(composectx)
        }
    }

    private fun colorForConfidence(confidence: Double): Color {
        if (confidence >= 0.8) {
            return Color.green
        } else if (confidence >= 0.6) {
            return Color.blue
        } else if (confidence >= 0.4) {
            return Color.orange
        } else {
            return Color.red
        }
    }

    constructor(confidence: Double) {
        this.confidence = confidence
    }
}

internal class RiskBadge: View {
    internal val risk: TransferRecommendation.RiskLevel

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            Text(risk.rawValue)
                .font(Font.caption2)
                .fontWeight(Font.Weight.medium)
                .padding(Edge.Set.horizontal, 6.0)
                .padding(Edge.Set.vertical, 2.0)
                .background(backgroundColor)
                .foregroundColor(Color.white)
                .cornerRadius(4.0).Compose(composectx)
        }
    }

    private val backgroundColor: Color
        get() {
            when (risk) {
                TransferRecommendation.RiskLevel.low -> return Color.green
                TransferRecommendation.RiskLevel.medium -> return Color.orange
                TransferRecommendation.RiskLevel.high -> return Color.red
            }
        }

    constructor(risk: TransferRecommendation.RiskLevel) {
        this.risk = risk
    }
}

internal class PriorityBadge: View {
    internal val priority: TransferRecommendation.Priority

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            Text(priority.rawValue)
                .font(Font.caption2)
                .fontWeight(Font.Weight.medium)
                .padding(Edge.Set.horizontal, 6.0)
                .padding(Edge.Set.vertical, 2.0)
                .background(backgroundColor)
                .foregroundColor(Color.white)
                .cornerRadius(4.0).Compose(composectx)
        }
    }

    private val backgroundColor: Color
        get() {
            when (priority) {
                TransferRecommendation.Priority.essential -> return Color.red
                TransferRecommendation.Priority.recommended -> return Color.orange
                TransferRecommendation.Priority.optional -> return Color.blue
            }
        }

    constructor(priority: TransferRecommendation.Priority) {
        this.priority = priority
    }
}

internal class FilterChip: View {
    internal val title: String
    internal val isSelected: Boolean
    internal val action: () -> Unit

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            Text(title)
                .font(Font.caption)
                .fontWeight(Font.Weight.medium)
                .padding(Edge.Set.horizontal, 12.0)
                .padding(Edge.Set.vertical, 6.0)
                .background(if (isSelected) Color.blue else Color.systemGray5Compat)
                .foregroundColor(if (isSelected) Color.white else Color.primary)
                .cornerRadius(16.0)
                .onTapGesture { it -> action() }.Compose(composectx)
        }
    }

    constructor(title: String, isSelected: Boolean, action: () -> Unit) {
        this.title = title
        this.isSelected = isSelected
        this.action = action
    }
}

// MARK: - Filter Sheet

internal class FilterSheet: View {
    // MARK: - Computed Bindings for Kotlin Compat
    internal val maxPriceBindingCompat: Binding<String>
        get() {
            return Binding<String>(get = l@{ ->
                filters.maxPrice?.let { extractedValue ->
                    return@l String(extractedValue)
                }
                return@l ""
            }, set = { newValue ->
                val matchtarget_1 = Int(newValue)
                if (matchtarget_1 != null) {
                    val v = matchtarget_1
                    filters.maxPrice = v
                } else {
                    filters.maxPrice = null
                }
            })
        }

    internal val minPointsBindingCompat: Binding<String>
        get() {
            return Binding<String>(get = l@{ ->
                filters.minPoints?.let { extractedValue ->
                    return@l String(extractedValue)
                }
                return@l ""
            }, set = { newValue ->
                val matchtarget_2 = Int(newValue)
                if (matchtarget_2 != null) {
                    val v = matchtarget_2
                    filters.minPoints = v
                } else {
                    filters.minPoints = null
                }
            })
        }

    internal val minConfidenceBindingCompat: Binding<String>
        get() {
            return Binding<String>(get = l@{ ->
                filters.minConfidence?.let { extractedValue ->
                    return@l String(extractedValue)
                }
                return@l ""
            }, set = { newValue ->
                val s = newValue.replacingOccurrences(of = ",", with = ".")
                val matchtarget_3 = Double(s)
                if (matchtarget_3 != null) {
                    val v = matchtarget_3
                    filters.minConfidence = v
                } else {
                    filters.minConfidence = null
                }
            })
        }

    internal var filters: RecommendationFilters
        get() = _filters.wrappedValue.sref({ this.filters = it })
        set(newValue) {
            _filters.wrappedValue = newValue.sref()
        }
    internal var _filters: Binding<RecommendationFilters>
    private lateinit var dismiss: DismissAction

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            NavigationStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Form { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Section(LocalizedStringKey(stringLiteral = "Positionen")) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    /*
                                    // Companion object access causes Kotlin errors
                                    ForEach(TeamAnalysis.Position.allCases, id: \.self) { position in
                                    Button(action: {
                                    if filters.positions.contains(position) {
                                    filters.positions.remove(position)
                                    } else {
                                    filters.positions.insert(position)
                                    }
                                    }) {
                                    HStack {
                                    Text(position.rawValue)
                                    Spacer()
                                    if filters.positions.contains(position) {
                                    Image(systemName: "checkmark")
                                    .foregroundColor(.blue)
                                    }
                                    }
                                    }
                                    }
                                    */
                                    Text(LocalizedStringKey(stringLiteral = "Filter deaktiviert (Wartung)")).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            Section(LocalizedStringKey(stringLiteral = "Risiko & Priorität")) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Filters Disabled for Android Build")).Compose(composectx)
                                    /*
                                    // Manual iteration to avoid Companion object issues in Kotlin transpile
                                    Picker("Max. Risiko", selection: $filters.maxRisk) {
                                    Text("Low").tag(TransferRecommendation.RiskLevel.low)
                                    Text("Medium").tag(TransferRecommendation.RiskLevel.medium)
                                    Text("High").tag(TransferRecommendation.RiskLevel.high)
                                    }

                                    Picker("Min. Priorität", selection: $filters.minPriority) {
                                    Text("Essential").tag(TransferRecommendation.Priority.essential)
                                    Text("Recommended").tag(TransferRecommendation.Priority.recommended)
                                    Text("Optional").tag(TransferRecommendation.Priority.optional)
                                    }
                                    */
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            Section(LocalizedStringKey(stringLiteral = "Erweiterte Filter")) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    /*
                                    Picker("Form-Trend", selection: $filters.formTrend) {
                                    Text("Alle").tag(PlayerAnalysis.FormTrend?.none)
                                    ForEach(
                                    [PlayerAnalysis.FormTrend.improving, .stable, .declining], id: \.self
                                    ) { trend in
                                    Text(trend.rawValue).tag(PlayerAnalysis.FormTrend?.some(trend))
                                    }
                                    }
                                    */
                                    Text(LocalizedStringKey(stringLiteral = "Deaktiviert")).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            Section(LocalizedStringKey(stringLiteral = "Werte-Filter")) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    HStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Max. Preis")).Compose(composectx)
                                            Spacer().Compose(composectx)
                                            TextField(LocalizedStringKey(stringLiteral = "€ Millionen"), text = this.maxPriceBindingCompat).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    HStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Min. Punkte")).Compose(composectx)
                                            Spacer().Compose(composectx)
                                            TextField(LocalizedStringKey(stringLiteral = "Punkte"), text = this.minPointsBindingCompat).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    HStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Min. Vertrauen")).Compose(composectx)
                                            Spacer().Compose(composectx)
                                            TextField(LocalizedStringKey(stringLiteral = "0.0 - 1.0"), text = this.minConfidenceBindingCompat).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .navigationTitle(LocalizedStringKey(stringLiteral = "Filter"))
                    .toolbar { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            ToolbarItem(placement = ToolbarItemPlacement.navigationBarLeadingCompat) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Button(LocalizedStringKey(stringLiteral = "Zurücksetzen")) { -> filters = RecommendationFilters() }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ToolbarItem(placement = ToolbarItemPlacement.navigationBarTrailingCompat) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Button(LocalizedStringKey(stringLiteral = "Fertig")) { -> dismiss() }.Compose(composectx)
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

    constructor(filters: Binding<RecommendationFilters>) {
        this._filters = filters
    }
}

// MARK: - Player Detail Sheet

internal class PlayerDetailSheet: View {
    internal val recommendation: TransferRecommendation
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    private lateinit var dismiss: DismissAction
    private var marketValueHistory: MarketValueChange?
        get() = _marketValueHistory.wrappedValue
        set(newValue) {
            _marketValueHistory.wrappedValue = newValue
        }
    private var _marketValueHistory: skip.ui.State<MarketValueChange?> = skip.ui.State(null)
    private var isLoadingHistory: Boolean
        get() = _isLoadingHistory.wrappedValue
        set(newValue) {
            _isLoadingHistory.wrappedValue = newValue
        }
    private var _isLoadingHistory: skip.ui.State<Boolean>

    internal val latestDailyChange: Int
        get() {
            val history_1 = marketValueHistory
            if (history_1 == null) {
                return recommendation.analysis.seasonProjection.projectedValueIncrease
            }
            val today_1 = history_1.dailyChanges.first(where = { it -> it.daysAgo == 0 })
            if (today_1 == null) {
                return recommendation.analysis.seasonProjection.projectedValueIncrease
            }
            return today_1.change
        }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            NavigationStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    ScrollView { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            VStack(alignment = HorizontalAlignment.leading, spacing = 20.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    // Player Header
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 12.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            HStack { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    VStack(alignment = HorizontalAlignment.leading) { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            Text(recommendation.player.firstName + " " + recommendation.player.lastName)
                                                                .font(Font.title2)
                                                                .fontWeight(Font.Weight.bold).Compose(composectx)

                                                            Text(recommendation.player.teamName)
                                                                .font(Font.subheadline)
                                                                .foregroundColor(Color.secondary).Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }.Compose(composectx)

                                                    Spacer().Compose(composectx)

                                                    VStack(alignment = HorizontalAlignment.trailing) { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            Text(formatPrice(recommendation.player.price))
                                                                .font(Font.title2)
                                                                .fontWeight(Font.Weight.bold).Compose(composectx)

                                                            HStack { ->
                                                                ComposeBuilder { composectx: ComposeContext ->
                                                                    RiskBadge(risk = recommendation.riskLevel).Compose(composectx)
                                                                    PriorityBadge(priority = recommendation.priority).Compose(composectx)
                                                                    ComposeResult.ok
                                                                }
                                                            }.Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }.Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            // Score and Rating
                                            HStack { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    VStack(alignment = HorizontalAlignment.leading) { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            Text(LocalizedStringKey(stringLiteral = "Empfehlungswert"))
                                                                .font(Font.caption)
                                                                .foregroundColor(Color.secondary).Compose(composectx)
                                                            Text(String(format = "%.1f/24", recommendation.recommendationScore))
                                                                .font(Font.headline)
                                                                .fontWeight(Font.Weight.semibold).Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }.Compose(composectx)

                                                    Spacer().Compose(composectx)

                                                    // Progress Bar for Score
                                                    ProgressView(value = recommendation.recommendationScore, total = 24.0)
                                                        .progressViewStyle(ProgressViewStyle.linear)
                                                        .tint(scoreColor(recommendation.recommendationScore)).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }
                                    .padding()
                                    .background(Color.systemGray6Compat)
                                    .cornerRadius(12.0).Compose(composectx)

                                    // Detailed Analysis
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 16.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Detaillierte Analyse"))
                                                .font(Font.headline).Compose(composectx)

                                            // Performance Stats
                                            HStack { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    StatDetailItem(title = "Gesamtpunkte", value = "${recommendation.player.totalPoints}").Compose(composectx)
                                                    StatDetailItem(title = "Ø pro Spiel", value = String(format = "%.1f", recommendation.analysis.pointsPerGame)).Compose(composectx)
                                                    StatDetailItem(title = "Preis-Leistung", value = String(format = "%.1f", recommendation.analysis.valueForMoney)).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            // Form
                                            VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(LocalizedStringKey(stringLiteral = "Form"))
                                                        .font(Font.subheadline)
                                                        .fontWeight(Font.Weight.medium).Compose(composectx)

                                                    HStack { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            VStack(alignment = HorizontalAlignment.leading) { ->
                                                                ComposeBuilder { composectx: ComposeContext ->
                                                                    Text(LocalizedStringKey(stringLiteral = "Form-Trend"))
                                                                        .font(Font.caption)
                                                                        .foregroundColor(Color.secondary).Compose(composectx)
                                                                    FormTrendBadge(trend = recommendation.analysis.formTrend).Compose(composectx)
                                                                    ComposeResult.ok
                                                                }
                                                            }.Compose(composectx)

                                                            Spacer().Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }.Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            // Season Projection
                                            VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(LocalizedStringKey(stringLiteral = "Saisonprognose"))
                                                        .font(Font.subheadline)
                                                        .fontWeight(Font.Weight.medium).Compose(composectx)

                                                    HStack { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            VStack(alignment = HorizontalAlignment.leading) { ->
                                                                ComposeBuilder { composectx: ComposeContext ->
                                                                    Text(LocalizedStringKey(stringLiteral = "Erwartete Punkte"))
                                                                        .font(Font.caption)
                                                                        .foregroundColor(Color.secondary).Compose(composectx)
                                                                    Text({
                                                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                                        str.appendInterpolation(recommendation.analysis.seasonProjection.projectedTotalPoints)
                                                                        LocalizedStringKey(stringInterpolation = str)
                                                                    }())
                                                                        .font(Font.subheadline)
                                                                        .fontWeight(Font.Weight.semibold).Compose(composectx)
                                                                    ComposeResult.ok
                                                                }
                                                            }.Compose(composectx)

                                                            Spacer().Compose(composectx)

                                                            VStack(alignment = HorizontalAlignment.trailing) { ->
                                                                ComposeBuilder { composectx: ComposeContext ->
                                                                    Text(LocalizedStringKey(stringLiteral = "Wertsteigerung (täglich)"))
                                                                        .font(Font.caption)
                                                                        .foregroundColor(Color.secondary).Compose(composectx)
                                                                    Text(formatCurrency(latestDailyChange))
                                                                        .font(Font.subheadline)
                                                                        .fontWeight(Font.Weight.semibold)
                                                                        .foregroundColor(if (latestDailyChange > 0) Color.green else Color.red).Compose(composectx)
                                                                    ComposeResult.ok
                                                                }
                                                            }.Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }.Compose(composectx)

                                                    HStack { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            Text(LocalizedStringKey(stringLiteral = "Vertrauen:"))
                                                                .font(Font.caption)
                                                                .foregroundColor(Color.secondary).Compose(composectx)
                                                            ConfidenceBadge(confidence = recommendation.analysis.seasonProjection.confidence).Compose(composectx)
                                                            Spacer().Compose(composectx)
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

                                    // All Reasons
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 12.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Alle Empfehlungsgründe"))
                                                .font(Font.headline).Compose(composectx)

                                            ForEach(recommendation.reasons) { reason ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    VStack(alignment = HorizontalAlignment.leading, spacing = 6.0) { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            HStack { ->
                                                                ComposeBuilder { composectx: ComposeContext ->
                                                                    Image(systemName = iconForReasonType(reason.type))
                                                                        .foregroundColor(colorForImpact(reason.impact)).Compose(composectx)
                                                                    Text(reason.type.rawValue)
                                                                        .font(Font.subheadline)
                                                                        .fontWeight(Font.Weight.medium).Compose(composectx)
                                                                    Spacer().Compose(composectx)
                                                                    Text(String(format = "%.1f", reason.impact))
                                                                        .font(Font.caption)
                                                                        .fontWeight(Font.Weight.medium)
                                                                        .padding(Edge.Set.horizontal, 6.0)
                                                                        .padding(Edge.Set.vertical, 2.0)
                                                                        .background(colorForImpact(reason.impact).opacity(0.2))
                                                                        .foregroundColor(colorForImpact(reason.impact))
                                                                        .cornerRadius(4.0).Compose(composectx)
                                                                    ComposeResult.ok
                                                                }
                                                            }.Compose(composectx)

                                                            Text(reason.description)
                                                                .font(Font.caption)
                                                                .foregroundColor(Color.secondary).Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }
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
                            .padding().Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .navigationTitle(LocalizedStringKey(stringLiteral = "Spieler Details"))
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
            }
            .task { -> MainActor.run { loadMarketValueHistory() } }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedmarketValueHistory by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<MarketValueChange?>, Any>) { mutableStateOf(_marketValueHistory) }
        _marketValueHistory = rememberedmarketValueHistory

        val rememberedisLoadingHistory by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_isLoadingHistory) }
        _isLoadingHistory = rememberedisLoadingHistory

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!
        this.dismiss = EnvironmentValues.shared.dismiss

        return super.Evaluate(context, options)
    }

    private suspend fun loadMarketValueHistory(): Unit = MainActor.run l@{
        val selectedLeague_2 = kickbaseManager.selectedLeague
        if (selectedLeague_2 == null) {
            return@l
        }
        isLoadingHistory = true
        val history = kickbaseManager.loadPlayerMarketValueHistory(playerId = recommendation.player.id, leagueId = selectedLeague_2.id)
        marketValueHistory = history
        isLoadingHistory = false
    }

    private fun formatPrice(price: Int): String = "€${String(format = "%.1f", Double(price) / 1_000_000.0)}M"

    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormatter()
        formatter.numberStyle = NumberFormatter.Style.currency
        formatter.locale = Locale(identifier = "de_DE")
        formatter.currencyCode = "EUR"
        return formatter.string(from = NSNumber(value = Double(amount))) ?: "€0"
    }

    private fun scoreColor(score: Double): Color {
        if (score >= 7) {
            return Color.green
        } else if (score >= 5) {
            return Color.blue
        } else if (score >= 3) {
            return Color.orange
        } else {
            return Color.red
        }
    }

    private fun colorForInjuryRisk(risk: PlayerAnalysis.InjuryRisk): Color {
        when (risk) {
            PlayerAnalysis.InjuryRisk.low -> return Color.green
            PlayerAnalysis.InjuryRisk.medium -> return Color.orange
            PlayerAnalysis.InjuryRisk.high -> return Color.red
        }
    }

    private fun iconForReasonType(type: RecommendationReason.ReasonType): String {
        when (type) {
            RecommendationReason.ReasonType.performance -> return "chart.line.uptrend.xyaxis"
            RecommendationReason.ReasonType.value_ -> return "eurosign.circle"
            RecommendationReason.ReasonType.potential -> return "star"
            RecommendationReason.ReasonType.teamNeed -> return "person.crop.circle.badge.plus"
            RecommendationReason.ReasonType.injury -> return "cross.case"
            RecommendationReason.ReasonType.form -> return "waveform.path.ecg"
            RecommendationReason.ReasonType.opponent -> return "sportscourt"
        }
    }

    private fun colorForImpact(impact: Double): Color {
        if (impact >= 7) {
            return Color.green
        } else if (impact >= 4) {
            return Color.blue
        } else if (impact >= 0) {
            return Color.orange
        } else {
            return Color.red
        }
    }

    private constructor(recommendation: TransferRecommendation, marketValueHistory: MarketValueChange? = null, isLoadingHistory: Boolean = false, privatep: Nothing? = null) {
        this.recommendation = recommendation
        this._marketValueHistory = skip.ui.State(marketValueHistory)
        this._isLoadingHistory = skip.ui.State(isLoadingHistory)
    }

    constructor(recommendation: TransferRecommendation): this(recommendation = recommendation, privatep = null) {
    }
}

// MARK: - Supporting Data Models

@Suppress("MUST_BE_INITIALIZED")
internal class RecommendationFilters: MutableStruct {
    internal var positions: Set<TeamAnalysis.Position>
        get() = field.sref({ this.positions = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    internal var maxPrice: Int? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    internal var minPoints: Int? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    internal var maxRisk: TransferRecommendation.RiskLevel
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    internal var minPriority: TransferRecommendation.Priority
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    internal var formTrend: PlayerAnalysis.FormTrend? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    internal var minConfidence: Double? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }


    constructor(positions: Set<TeamAnalysis.Position> = setOf(), maxPrice: Int? = null, minPoints: Int? = null, maxRisk: TransferRecommendation.RiskLevel = TransferRecommendation.RiskLevel.high, minPriority: TransferRecommendation.Priority = TransferRecommendation.Priority.optional, formTrend: PlayerAnalysis.FormTrend? = null, minConfidence: Double? = null) {
        this.positions = positions
        this.maxPrice = maxPrice
        this.minPoints = minPoints
        this.maxRisk = maxRisk
        this.minPriority = minPriority
        this.formTrend = formTrend
        this.minConfidence = minConfidence
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = RecommendationFilters(positions, maxPrice, minPoints, maxRisk, minPriority, formTrend, minConfidence)
}

/*
#if !SKIP
#Preview {
TransferRecommendationsView(kickbaseManager: KickbaseManager())
}
#endif
*/
