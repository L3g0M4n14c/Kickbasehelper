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

internal class SaleRecommendationsView: View {
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
    private var selectedGoal: SaleRecommendationGoal
        get() = _selectedGoal.wrappedValue
        set(newValue) {
            _selectedGoal.wrappedValue = newValue
        }
    private var _selectedGoal: skip.ui.State<SaleRecommendationGoal> = skip.ui.State(SaleRecommendationGoal.balanceBudget)
    private var recommendations: Array<SaleRecommendation>
        get() = _recommendations.wrappedValue.sref({ this.recommendations = it })
        set(newValue) {
            _recommendations.wrappedValue = newValue.sref()
        }
    private var _recommendations: skip.ui.State<Array<SaleRecommendation>> = skip.ui.State(arrayOf())
    private var isLoading: Boolean
        get() = _isLoading.wrappedValue
        set(newValue) {
            _isLoading.wrappedValue = newValue
        }
    private var _isLoading: skip.ui.State<Boolean> = skip.ui.State(false)
    private var errorMessage: String?
        get() = _errorMessage.wrappedValue
        set(newValue) {
            _errorMessage.wrappedValue = newValue
        }
    private var _errorMessage: skip.ui.State<String?> = skip.ui.State(null)

    internal constructor(kickbaseManager: KickbaseManager) {
        this._kickbaseManager = skip.ui.Bindable(kickbaseManager)
        this._recommendationService = State(wrappedValue = PlayerRecommendationService(kickbaseManager = kickbaseManager))
    }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 0.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Budget Info Header
                    kickbaseManager.userStats?.let { stats ->
                        HStack { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Text(LocalizedStringKey(stringLiteral = "Aktuelles Budget"))
                                            .font(Font.caption)
                                            .foregroundColor(Color.secondary).Compose(composectx)
                                        Text(formatCurrencyForSales(stats.budget))
                                            .font(Font.headline)
                                            .fontWeight(Font.Weight.bold)
                                            .foregroundColor(if (stats.budget >= 0) Color.green else Color.red).Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)

                                Spacer().Compose(composectx)

                                VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Text(LocalizedStringKey(stringLiteral = "Status"))
                                            .font(Font.caption)
                                            .foregroundColor(Color.secondary).Compose(composectx)
                                        HStack(spacing = 4.0) { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                Image(systemName = if (stats.budget >= 0) "checkmark.circle.fill" else "exclamationmark.circle.fill")
                                                    .foregroundColor(if (stats.budget >= 0) Color.green else Color.red).Compose(composectx)
                                                Text(if (stats.budget >= 0) "Im Plus" else "Im Minus")
                                                    .font(Font.subheadline)
                                                    .fontWeight(Font.Weight.semibold).Compose(composectx)
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
                        .background(Color.systemGray6Compat).Compose(composectx)
                    }

                    // Goal Selection
                    VStack(spacing = 12.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(LocalizedStringKey(stringLiteral = "Verkaufs-Empfehlungen"))
                                .font(Font.headline)
                                .padding(Edge.Set.bottom, 4.0).Compose(composectx)

                            // Ziel-Buttons
                            ScrollView(Axis.Set.horizontal, showsIndicators = false) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    HStack(spacing = 10.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            ForEach(SaleRecommendationGoal.allCases, id = { it }) { goal ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    GoalSelectionButton(goal = goal, isSelected = selectedGoal == goal) { ->
                                                        selectedGoal = goal
                                                        Task(isMainActor = true) { -> loadRecommendations() }
                                                    }.Compose(composectx)
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
                    }
                    .padding()
                    .background(Color.systemGray6Compat).Compose(composectx)

                    // Content
                    if (isLoading) {
                        VStack(spacing = 20.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                Spacer().Compose(composectx)
                                ProgressView()
                                    .scaleEffect(1.5).Compose(composectx)
                                Text(LocalizedStringKey(stringLiteral = "Generiere Empfehlungen..."))
                                    .font(Font.subheadline)
                                    .foregroundColor(Color.secondary).Compose(composectx)
                                Spacer().Compose(composectx)
                                ComposeResult.ok
                            }
                        }.Compose(composectx)
                    } else {
                        val matchtarget_0 = errorMessage
                        if (matchtarget_0 != null) {
                            val errorMessage = matchtarget_0
                            VStack(spacing = 15.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Spacer().Compose(composectx)
                                    Image(systemName = "exclamationmark.triangle.fill")
                                        .font(Font.system(size = 50.0))
                                        .foregroundColor(Color.orange).Compose(composectx)
                                    Text(LocalizedStringKey(stringLiteral = "Fehler"))
                                        .font(Font.headline).Compose(composectx)
                                    Text(errorMessage)
                                        .font(Font.subheadline)
                                        .foregroundColor(Color.secondary)
                                        .multilineTextAlignment(TextAlignment.center).Compose(composectx)
                                    Spacer().Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .padding().Compose(composectx)
                        } else if (recommendations.isEmpty) {
                            VStack(spacing = 15.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Spacer().Compose(composectx)
                                    Image(systemName = "checkmark.circle.fill")
                                        .font(Font.system(size = 50.0))
                                        .foregroundColor(Color.green).Compose(composectx)
                                    Text(LocalizedStringKey(stringLiteral = "Keine Empfehlungen"))
                                        .font(Font.headline).Compose(composectx)
                                    Text(LocalizedStringKey(stringLiteral = "Für dieses Ziel gibt es derzeit keine Empfehlungen."))
                                        .font(Font.subheadline)
                                        .foregroundColor(Color.secondary).Compose(composectx)
                                    Spacer().Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .padding().Compose(composectx)
                        } else {
                            ScrollView { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    VStack(spacing = 12.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            ForEach(recommendations) { recommendation ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    SaleRecommendationCard(recommendation = recommendation, currentBudget = kickbaseManager.userStats?.budget ?: 0).Compose(composectx)
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
                        }
                    }
                    ComposeResult.ok
                }
            }
            .task { -> MainActor.run { loadRecommendations() } }
            .onChange(of = kickbaseManager.selectedLeague) { _, _ ->
                Task(isMainActor = true) { -> loadRecommendations() }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedrecommendationService by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<PlayerRecommendationService>, Any>) { mutableStateOf(_recommendationService) }
        _recommendationService = rememberedrecommendationService

        val rememberedselectedGoal by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<SaleRecommendationGoal>, Any>) { mutableStateOf(_selectedGoal) }
        _selectedGoal = rememberedselectedGoal

        val rememberedrecommendations by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Array<SaleRecommendation>>, Any>) { mutableStateOf(_recommendations) }
        _recommendations = rememberedrecommendations

        val rememberedisLoading by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_isLoading) }
        _isLoading = rememberedisLoading

        val rememberederrorMessage by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<String?>, Any>) { mutableStateOf(_errorMessage) }
        _errorMessage = rememberederrorMessage

        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    private suspend fun loadRecommendations(): Unit = Async.run l@{
        val league_0 = kickbaseManager.selectedLeague
        if (league_0 == null) {
            errorMessage = "Keine Liga ausgewählt"
            return@l
        }

        isLoading = true
        errorMessage = null
        recommendations = arrayOf()

        try {
            val teamPlayers = recommendationService.getTeamPlayersSync(for_ = league_0)
            val marketPlayers = recommendationService.getMarketPlayersSync(for_ = league_0)
            // Wichtig: Aktuelles Budget aus userStats verwenden, nicht league.currentUser.budget (das ist das Startbudget)
            val budget = kickbaseManager.userStats?.budget ?: league_0.currentUser.budget

            val recs = recommendationService.generateSaleRecommendations(for_ = league_0, goal = selectedGoal, teamPlayers = teamPlayers, marketPlayers = marketPlayers, currentBudget = budget)

            MainActor.run { ->
                recommendations = recs
                isLoading = false
            }
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            MainActor.run { ->
                errorMessage = error.localizedDescription
                isLoading = false
            }
        }
    }
}

// MARK: - Goal Selection Button

internal class GoalSelectionButton: View {
    internal val goal: SaleRecommendationGoal
    internal val isSelected: Boolean
    internal val action: () -> Unit

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            Button(action = action) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    VStack(spacing = 4.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Image(systemName = goal.icon)
                                .font(Font.system(size = 18.0)).Compose(composectx)
                            Text(goal.rawValue)
                                .font(Font.caption2)
                                .lineLimit(2)
                                .multilineTextAlignment(TextAlignment.center).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .frame(minWidth = 70.0, minHeight = 60.0)
                    .padding(Edge.Set.horizontal, 8.0)
                    .padding(Edge.Set.vertical, 6.0)
                    .background(if (isSelected) Color.blue else Color.systemGray5Compat)
                    .foregroundColor(if (isSelected) Color.white else Color.primary)
                    .cornerRadius(10.0).Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    constructor(goal: SaleRecommendationGoal, isSelected: Boolean, action: () -> Unit) {
        this.goal = goal
        this.isSelected = isSelected
        this.action = action
    }
}

// MARK: - Sale Recommendation Card

internal class SaleRecommendationCard: View {
    internal val recommendation: SaleRecommendation
    private var expandedReplacements: Boolean
        get() = _expandedReplacements.wrappedValue
        set(newValue) {
            _expandedReplacements.wrappedValue = newValue
        }
    private var _expandedReplacements: skip.ui.State<Boolean>
    internal val currentBudget: Int
    internal var ligainsiderService: LigainsiderService
        get() = _ligainsiderService.wrappedValue
        set(newValue) {
            _ligainsiderService.wrappedValue = newValue
        }
    internal var _ligainsiderService = skip.ui.Environment<LigainsiderService>()

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(alignment = HorizontalAlignment.leading, spacing = 12.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Budget Info
                    recommendation.bestReplacement?.let { bestReplacement ->
                        if (bestReplacement.budgetSavings > 0) {
                            VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    HStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(LocalizedStringKey(stringLiteral = "Aktuelles Budget"))
                                                        .font(Font.caption)
                                                        .foregroundColor(Color.secondary).Compose(composectx)
                                                    Text(formatCurrencyForSales(currentBudget))
                                                        .font(Font.headline)
                                                        .fontWeight(Font.Weight.bold)
                                                        .foregroundColor(if (currentBudget >= 0) Color.green else Color.red).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            Spacer().Compose(composectx)

                                            VStack(alignment = HorizontalAlignment.center, spacing = 2.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Image(systemName = "arrow.right")
                                                        .font(Font.caption)
                                                        .foregroundColor(Color.secondary).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }
                                            .frame(maxWidth = 30.0).Compose(composectx)

                                            Spacer().Compose(composectx)

                                            VStack(alignment = HorizontalAlignment.trailing, spacing = 2.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(LocalizedStringKey(stringLiteral = "Nach Empfehlung"))
                                                        .font(Font.caption)
                                                        .foregroundColor(Color.secondary).Compose(composectx)
                                                    val budgetAfter = currentBudget + bestReplacement.budgetSavings
                                                    Text(formatCurrencyForSales(budgetAfter))
                                                        .font(Font.headline)
                                                        .fontWeight(Font.Weight.bold)
                                                        .foregroundColor(if (budgetAfter >= 0) Color.green else Color.red).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    HStack(spacing = 8.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Image(systemName = "plus.circle.fill")
                                                .font(Font.caption)
                                                .foregroundColor(Color.green).Compose(composectx)
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendLiteral("+")
                                                str.appendInterpolation(formatCurrencyForSales(bestReplacement.budgetSavings))
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.subheadline)
                                                .fontWeight(Font.Weight.semibold)
                                                .foregroundColor(Color.green).Compose(composectx)
                                            Spacer().Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .padding()
                            .background(Color.green.opacity(0.05))
                            .cornerRadius(10.0).Compose(composectx)
                        }
                    }

                    // Header: Spieler zum Verkaufen
                    VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            HStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Label(LocalizedStringKey(stringLiteral = "Zum Verkaufen"), systemImage = "arrow.up.right.square")
                                                .font(Font.caption)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            HStack(spacing = 8.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(recommendation.playerToSell.fullName)
                                                        .font(Font.headline)
                                                        .fontWeight(Font.Weight.semibold).Compose(composectx)

                                                    // Ligainsider Status Icon
                                                    val status = ligainsiderService.getPlayerStatus(firstName = recommendation.playerToSell.firstName, lastName = recommendation.playerToSell.lastName)
                                                    if (status != LigainsiderStatus.out) {
                                                        Image(systemName = ligainsiderService.getIcon(for_ = status))
                                                            .foregroundColor(ligainsiderService.getColor(for_ = status))
                                                            .font(Font.caption).Compose(composectx)
                                                    }

                                                    Text(positionAbbreviation(recommendation.playerToSell.position))
                                                        .font(Font.caption2)
                                                        .fontWeight(Font.Weight.bold)
                                                        .foregroundColor(Color.white)
                                                        .padding(Edge.Set.horizontal, 6.0)
                                                        .padding(Edge.Set.vertical, 2.0)
                                                        .background(positionColor(recommendation.playerToSell.position))
                                                        .cornerRadius(4.0).Compose(composectx)

                                                    Spacer().Compose(composectx)

                                                    SalesPriorityBadge(priority = recommendation.priority).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    Spacer().Compose(composectx)

                                    VStack(alignment = HorizontalAlignment.trailing, spacing = 2.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Verkaufswert"))
                                                .font(Font.caption2)
                                                .foregroundColor(Color.secondary).Compose(composectx)
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendLiteral("€")
                                                str.appendInterpolation(recommendation.playerToSell.marketValue / 1_000_000)
                                                str.appendLiteral("M")
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.headline)
                                                .fontWeight(Font.Weight.bold).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            // Spieler Stats
                            HStack(spacing = 12.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Punkte"))
                                                .font(Font.caption2)
                                                .foregroundColor(Color.secondary).Compose(composectx)
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendInterpolation(recommendation.playerToSell.totalPoints)
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.subheadline)
                                                .fontWeight(Font.Weight.semibold).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Ø/Spiel"))
                                                .font(Font.caption2)
                                                .foregroundColor(Color.secondary).Compose(composectx)
                                            Text(String(format = "%.1f", Double(recommendation.playerToSell.averagePoints)))
                                                .font(Font.subheadline)
                                                .fontWeight(Font.Weight.semibold).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    Spacer().Compose(composectx)

                                    if (recommendation.playerToSell.status == 1) {
                                        HStack(spacing = 4.0) { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                Image(systemName = "cross.circle.fill")
                                                    .foregroundColor(Color.red).Compose(composectx)
                                                Text(LocalizedStringKey(stringLiteral = "Verletzt"))
                                                    .font(Font.caption2)
                                                    .foregroundColor(Color.red).Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)
                                    } else if (recommendation.playerToSell.status == 2) {
                                        HStack(spacing = 4.0) { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                Image(systemName = "pills.fill")
                                                    .foregroundColor(Color.orange).Compose(composectx)
                                                Text(LocalizedStringKey(stringLiteral = "Angeschlagen"))
                                                    .font(Font.caption2)
                                                    .foregroundColor(Color.orange).Compose(composectx)
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
                    .cornerRadius(10.0).Compose(composectx)

                    // Erklärung
                    VStack(alignment = HorizontalAlignment.leading, spacing = 6.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Label(LocalizedStringKey(stringLiteral = "Warum?"), systemImage = "lightbulb")
                                .font(Font.caption)
                                .fontWeight(Font.Weight.semibold)
                                .foregroundColor(Color.blue).Compose(composectx)

                            Text(recommendation.explanation)
                                .font(Font.caption)
                                .foregroundColor(Color.secondary)
                                .lineLimit(3).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding()
                    .background(Color.blue.opacity(0.05))
                    .cornerRadius(10.0).Compose(composectx)

                    // Ersatz-Spieler
                    recommendation.bestReplacement?.let { bestReplacement ->
                        VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                HStack { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Label(LocalizedStringKey(stringLiteral = "Empfohlener Ersatz"), systemImage = "star.fill")
                                            .font(Font.caption)
                                            .foregroundColor(Color.secondary).Compose(composectx)

                                        Spacer().Compose(composectx)

                                        if (expandedReplacements) {
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendLiteral("Alle anzeigen (")
                                                str.appendInterpolation(recommendation.replacements.count)
                                                str.appendLiteral(")")
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.caption2)
                                                .foregroundColor(Color.blue).Compose(composectx)
                                        }
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)

                                ReplacementPlayerCard(replacement = bestReplacement, originalPlayer = recommendation.playerToSell).Compose(composectx)

                                // Weitere Optionen (wenn mehr als 1 Ersatz)
                                if (recommendation.replacements.count > 1 && expandedReplacements) {
                                    Divider()
                                        .padding(Edge.Set.vertical, 4.0).Compose(composectx)

                                    ForEach(recommendation.replacements.dropFirst()) { replacement ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            ReplacementPlayerCard(replacement = replacement, originalPlayer = recommendation.playerToSell).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                }

                                // Toggle für mehr Optionen
                                if (recommendation.replacements.count > 1) {
                                    Button(action = { -> expandedReplacements = !expandedReplacements }) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            HStack { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Image(systemName = if (expandedReplacements) "chevron.up" else "chevron.down").Compose(composectx)
                                                    Text(if (expandedReplacements) "Weniger anzeigen" else "${recommendation.replacements.count - 1} weitere Option(en)").Compose(composectx)
                                                    Spacer().Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }
                                            .font(Font.caption)
                                            .foregroundColor(Color.blue).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                }
                                ComposeResult.ok
                            }
                        }
                        .padding()
                        .background(Color.systemGray6Compat)
                        .cornerRadius(10.0).Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }
            .padding()
            .background(Color.systemBackgroundCompat)
            .cornerRadius(12.0)
            .shadow(color = Color.black.opacity(0.05), radius = 2.0, x = 0.0, y = 1.0).Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedexpandedReplacements by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_expandedReplacements) }
        _expandedReplacements = rememberedexpandedReplacements

        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    private constructor(recommendation: SaleRecommendation, expandedReplacements: Boolean = false, currentBudget: Int, privatep: Nothing? = null) {
        this.recommendation = recommendation
        this._expandedReplacements = skip.ui.State(expandedReplacements)
        this.currentBudget = currentBudget
    }

    constructor(recommendation: SaleRecommendation, currentBudget: Int): this(recommendation = recommendation, currentBudget = currentBudget, privatep = null) {
    }
}

// MARK: - Replacement Player Card

internal class ReplacementPlayerCard: View {
    internal val replacement: ReplacementSuggestion
    internal val originalPlayer: Player
    internal var ligainsiderService: LigainsiderService
        get() = _ligainsiderService.wrappedValue
        set(newValue) {
            _ligainsiderService.wrappedValue = newValue
        }
    internal var _ligainsiderService = skip.ui.Environment<LigainsiderService>()

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    HStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    HStack(spacing = 8.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(replacement.player.firstName + " " + replacement.player.lastName)
                                                .font(Font.subheadline)
                                                .fontWeight(Font.Weight.semibold).Compose(composectx)

                                            // Ligainsider Status Icon
                                            val status = ligainsiderService.getPlayerStatus(firstName = replacement.player.firstName, lastName = replacement.player.lastName)
                                            if (status != LigainsiderStatus.out) {
                                                Image(systemName = ligainsiderService.getIcon(for_ = status))
                                                    .foregroundColor(ligainsiderService.getColor(for_ = status))
                                                    .font(Font.caption).Compose(composectx)
                                            }

                                            Text(positionAbbreviation(replacement.player.position))
                                                .font(Font.caption2)
                                                .fontWeight(Font.Weight.bold)
                                                .foregroundColor(Color.white)
                                                .padding(Edge.Set.horizontal, 6.0)
                                                .padding(Edge.Set.vertical, 2.0)
                                                .background(positionColor(replacement.player.position))
                                                .cornerRadius(4.0).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    Text(replacement.player.fullTeamName)
                                        .font(Font.caption2)
                                        .foregroundColor(Color.secondary).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            Spacer().Compose(composectx)

                            VStack(alignment = HorizontalAlignment.trailing, spacing = 2.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(formatCurrencyForSales(replacement.player.price))
                                        .font(Font.subheadline)
                                        .fontWeight(Font.Weight.semibold)
                                        .foregroundColor(Color.green).Compose(composectx)

                                    if (replacement.budgetSavings > 0) {
                                        Text({
                                            val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                            str.appendLiteral("-")
                                            str.appendInterpolation(formatCurrencyForSales(replacement.budgetSavings))
                                            LocalizedStringKey(stringInterpolation = str)
                                        }())
                                            .font(Font.caption2)
                                            .foregroundColor(Color.green).Compose(composectx)
                                    }
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    // Vergleich
                    HStack(spacing = 12.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            ComparisonItem(title = "Punkte/Spiel", original = String(format = "%.1f", Double(originalPlayer.averagePoints)), replacement = String(format = "%.1f", replacement.player.averagePoints), gain = replacement.performanceGain).Compose(composectx)

                            ComparisonItem(title = "Gesamtpunkte", original = "${originalPlayer.totalPoints}", replacement = "${replacement.player.totalPoints}", gain = Double(replacement.player.totalPoints - originalPlayer.totalPoints)).Compose(composectx)

                            Spacer().Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .font(Font.caption).Compose(composectx)
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

    constructor(replacement: ReplacementSuggestion, originalPlayer: Player) {
        this.replacement = replacement
        this.originalPlayer = originalPlayer
    }
}

internal class ComparisonItem: View {
    internal val title: String
    internal val original: String
    internal val replacement: String
    internal val gain: Double

    internal val gainColor: Color
        get() {
            if (gain > 0) {
                return Color.green
            } else if (gain < 0) {
                return Color.red
            } else {
                return Color.gray
            }
        }

    internal val gainIcon: String
        get() {
            if (gain > 0) {
                return "arrow.up"
            } else if (gain < 0) {
                return "arrow.down"
            } else {
                return "minus"
            }
        }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(title)
                        .foregroundColor(Color.secondary).Compose(composectx)

                    HStack(spacing = 4.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(original)
                                .foregroundColor(Color.secondary).Compose(composectx)

                            Image(systemName = gainIcon)
                                .foregroundColor(gainColor)
                                .font(Font.caption2).Compose(composectx)

                            Text(replacement)
                                .fontWeight(Font.Weight.semibold).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding(6.0)
            .background(Color.systemGray6Compat)
            .cornerRadius(6.0).Compose(composectx)
        }
    }

    constructor(title: String, original: String, replacement: String, gain: Double) {
        this.title = title
        this.original = original
        this.replacement = replacement
        this.gain = gain
    }
}

// MARK: - Helper Functions

internal fun positionAbbreviation(position: Int): String {
    when (position) {
        1 -> return "TW"
        2 -> return "ABW"
        3 -> return "MF"
        4 -> return "ST"
        else -> return "?"
    }
}

internal fun positionColor(position: Int): Color {
    when (position) {
        1 -> return Color.yellow
        2 -> return Color.green
        3 -> return Color.blue
        4 -> return Color.red
        else -> return Color.gray
    }
}

internal fun formatCurrencyForSales(value: Int): String {
    val absValue = abs(value)
    if (absValue >= 1_000_000) {
        return String(format = "€%.1fM", Double(value) / 1_000_000)
    } else if (absValue >= 1_000) {
        val kValue = Double(value) / 1_000
        // Wenn es mehr als 1000k ist, in Millionen konvertieren
        if (kValue >= 1_000) {
            return String(format = "€%.1fM", kValue / 1_000)
        }
        return String(format = "€%.1fk", kValue)
    } else {
        return "€${value}"
    }
}

// MARK: - Priority Badge

internal class SalesPriorityBadge: View {
    internal val priority: TransferRecommendation.Priority

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            HStack(spacing = 3.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Image(systemName = priorityIcon)
                        .font(Font.caption2).Compose(composectx)
                    Text(priorityLabel)
                        .font(Font.caption2)
                        .fontWeight(Font.Weight.semibold).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .foregroundColor(priorityTextColor)
            .padding(Edge.Set.horizontal, 8.0)
            .padding(Edge.Set.vertical, 4.0)
            .background(priorityBgColor)
            .cornerRadius(6.0).Compose(composectx)
        }
    }

    internal val priorityLabel: String
        get() {
            when (priority) {
                TransferRecommendation.Priority.essential -> return "Essentiell"
                TransferRecommendation.Priority.recommended -> return "Empfohlen"
                TransferRecommendation.Priority.optional -> return "Optional"
                else -> return "Unbekannt"
            }
        }

    internal val priorityIcon: String
        get() {
            when (priority) {
                TransferRecommendation.Priority.essential -> return "exclamationmark.2"
                TransferRecommendation.Priority.recommended -> return "exclamationmark"
                TransferRecommendation.Priority.optional -> return "checkmark"
                else -> return "questionmark"
            }
        }

    internal val priorityTextColor: Color
        get() {
            when (priority) {
                TransferRecommendation.Priority.essential -> return Color.red
                TransferRecommendation.Priority.recommended -> return Color.orange
                TransferRecommendation.Priority.optional -> return Color.blue
                else -> return Color.gray
            }
        }

    internal val priorityBgColor: Color
        get() {
            when (priority) {
                TransferRecommendation.Priority.essential -> return Color.red.opacity(0.1)
                TransferRecommendation.Priority.recommended -> return Color.orange.opacity(0.1)
                TransferRecommendation.Priority.optional -> return Color.blue.opacity(0.1)
                else -> return Color.gray.opacity(0.1)
            }
        }

    constructor(priority: TransferRecommendation.Priority) {
        this.priority = priority
    }
}

// #Preview omitted
