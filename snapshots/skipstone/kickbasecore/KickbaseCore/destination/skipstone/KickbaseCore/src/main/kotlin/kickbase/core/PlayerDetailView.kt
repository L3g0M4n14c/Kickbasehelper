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

internal class AlternativeCandidate {
    internal val player: MarketPlayer
    internal val score: Double

    constructor(player: MarketPlayer, score: Double) {
        this.player = player
        this.score = score
    }
}

internal class PlayerDetailView: View {
    internal val player: Player
    private lateinit var dismiss: DismissAction
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
            NavigationStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    ScrollView { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            VStack(spacing = 20.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    // Hero Header mit Spielerfoto und allen Grundinformationen
                                    PlayerHeroHeader(player = player)
                                        .environmentObject(ligainsiderService).Compose(composectx)

                                    // Punktzahl-Performance
                                    PlayerPointsSection(player = player).Compose(composectx)

                                    // Marktwert und Finanzen
                                    PlayerMarketValueSection(player = player).Compose(composectx)

                                    // Spiele und Gegner (neu)
                                    PlayerMatchesSection(player = player).Compose(composectx)

                                    // Marktwertentwicklung der letzten 3 Tage
                                    PlayerMarketTrendSection(player = player).Compose(composectx)

                                    // Transfer-Vergleich und Alternativen
                                    PlayerAlternativesSection(player = player).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .padding().Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .background(LinearGradient(gradient = Gradient(colors = arrayOf(
                        positionColor(player.position).opacity(0.1),
                        Color.systemBackgroundCompat
                    )), startPoint = UnitPoint.top, endPoint = UnitPoint.bottom))
                    .navigationTitle(LocalizedStringKey(stringLiteral = ""))
                    .toolbar { ->
                        ComposeBuilder { composectx: ComposeContext -> ComposeResult.ok }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        this.dismiss = EnvironmentValues.shared.dismiss
        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!
        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    constructor(player: Player) {
        this.player = player
    }
}

// MARK: - Hero Header (erweitert mit allen Grunddaten)
internal class PlayerHeroHeader: View {
    internal val player: Player
    internal var ligainsiderService: LigainsiderService
        get() = _ligainsiderService.wrappedValue
        set(newValue) {
            _ligainsiderService.wrappedValue = newValue
        }
    internal var _ligainsiderService = skip.ui.Environment<LigainsiderService>()

    private val heroImageUrl: URL?
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
            VStack(spacing = 20.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Großes Profilbild mit Position Badge
                    ZStack(alignment = Alignment.bottomTrailing) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            AsyncImage(url = heroImageUrl, content = { image ->
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
                                                .fill(positionColor(player.position).opacity(0.3)).Compose(composectx)

                                            Image(systemName = "person.fill")
                                                .font(Font.system(size = 50.0))
                                                .foregroundColor(positionColor(player.position)).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            })
                            .frame(width = 120.0, height = 120.0)
                            .clipShape(Circle())
                            .overlay { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    ZStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Circle()
                                                .strokeBorder(positionColor(player.position), lineWidth = 3.0).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            // Position Badge
                            Text(positionAbbreviation(player.position))
                                .font(Font.caption)
                                .fontWeight(Font.Weight.bold)
                                .foregroundColor(Color.white)
                                .frame(width = 30.0, height = 30.0)
                                .background(positionColor(player.position))
                                .clipShape(Circle())
                                .overlay { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        ZStack { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                Circle()
                                                    .strokeBorder(Color.white, lineWidth = 2.0).Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }
                                .offset(x = 5.0, y = 5.0).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    // Name und grundlegende Informationen
                    VStack(spacing = 12.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Vor- und Nachname
                            VStack(spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(player.firstName)
                                        .font(Font.subheadline)
                                        .foregroundColor(Color.secondary)
                                        .accessibilityIdentifier("player_firstname").Compose(composectx)
                                    Text(player.lastName)
                                        .font(Font.title)
                                        .fontWeight(Font.Weight.bold)
                                        .accessibilityIdentifier("player_lastname")
                                        .font(Font.subheadline)
                                        .foregroundColor(Color.blue)
                                        .fontWeight(Font.Weight.medium).Compose(composectx)

                                    Text(LocalizedStringKey(stringLiteral = "•"))
                                        .foregroundColor(Color.secondary).Compose(composectx)

                                    Text(player.positionName)
                                        .font(Font.subheadline)
                                        .foregroundColor(positionColor(player.position))
                                        .fontWeight(Font.Weight.semibold).Compose(composectx)

                                    Text(LocalizedStringKey(stringLiteral = "•"))
                                        .foregroundColor(Color.secondary).Compose(composectx)

                                    Text({
                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                        str.appendLiteral("#")
                                        str.appendInterpolation(player.number)
                                        LocalizedStringKey(stringInterpolation = str)
                                    }())
                                        .font(Font.subheadline)
                                        .foregroundColor(Color.secondary)
                                        .fontWeight(Font.Weight.medium).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            // Statusbereich (Verletzung + Ligainsider)
                            HStack(spacing = 15.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    // Fit-Status Badge
                                    HStack(spacing = 6.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Image(systemName = getStatusIcon(player.status))
                                                .foregroundColor(getStatusColor(player.status)).Compose(composectx)
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendInterpolation(getPlayerStatusText(player.status))
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.caption)
                                                .fontWeight(Font.Weight.medium)
                                                .foregroundColor(getStatusColor(player.status)).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }
                                    .padding(Edge.Set.horizontal, 12.0)
                                    .padding(Edge.Set.vertical, 6.0)
                                    .background(getStatusColor(player.status).opacity(0.15))
                                    .cornerRadius(16.0).Compose(composectx)

                                    // Ligainsider Status Badge
                                    if (!ligainsiderService.matches.isEmpty) {
                                        val liStatus = ligainsiderService.getPlayerStatus(firstName = player.firstName, lastName = player.lastName)
                                        val color = ligainsiderService.getColor(for_ = liStatus)

                                        HStack(spacing = 6.0) { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                Image(systemName = ligainsiderService.getIcon(for_ = liStatus))
                                                    .foregroundColor(color).Compose(composectx)
                                                Text(if (liStatus == LigainsiderStatus.likelyStart) "S11" else (if (liStatus == LigainsiderStatus.startWithAlternative) "1. Option" else (if (liStatus == LigainsiderStatus.isAlternative) "2. Option" else "Bank/Out")))
                                                    .font(Font.caption)
                                                    .fontWeight(Font.Weight.medium)
                                                    .foregroundColor(color).Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }
                                        .padding(Edge.Set.horizontal, 12.0)
                                        .padding(Edge.Set.vertical, 6.0)
                                        .background(color.opacity(0.15))
                                        .cornerRadius(16.0).Compose(composectx)
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
            .background(Color.systemBackgroundCompat)
            .cornerRadius(16.0)
            .shadow(color = Color.black.opacity(0.1), radius = 8.0, x = 0.0, y = 4.0).Compose(composectx)
        }
    }

    @Composable
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    // Helper-Funktionen für Status
    private fun getStatusIcon(status: Int): String {
        when (status) {
            0 -> return "checkmark.circle.fill"
            1 -> return "cross.circle.fill"
            2 -> return "pills.fill"
            3 -> return "exclamationmark.triangle.fill"
            4 -> return "dumbbell.fill"
            8 -> return "rectangle.fill" // Rote Karte Symbol
            else -> return "questionmark.circle.fill"
        }
    }

    private fun getStatusColor(status: Int): Color {
        when (status) {
            0 -> return Color.green
            1 -> return Color.red
            2 -> return Color.orange
            3 -> return Color.red
            4 -> return Color.blue
            8 -> return Color.red // Rote Farbe für Sperre
            else -> return Color.gray
        }
    }

    constructor(player: Player) {
        this.player = player
    }
}

// MARK: - Punktzahl-Sektion
internal class PlayerPointsSection: View {
    internal val player: Player

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(alignment = HorizontalAlignment.leading, spacing = 16.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(LocalizedStringKey(stringLiteral = "Punktzahl-Performance"))
                        .font(Font.headline)
                        .fontWeight(Font.Weight.bold).Compose(composectx)

                    HStack(spacing = 16.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Durchschnittspunkte
                            VStack(spacing = 12.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Image(systemName = "star.fill")
                                        .font(Font.title)
                                        .foregroundColor(Color.orange).Compose(composectx)

                                    VStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendInterpolation(player.averagePoints)
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.title)
                                                .fontWeight(Font.Weight.bold)
                                                .foregroundColor(Color.orange).Compose(composectx)

                                            Text(LocalizedStringKey(stringLiteral = "Ø Punkte"))
                                                .font(Font.caption)
                                                .foregroundColor(Color.secondary)
                                                .textCase(Text.Case.uppercase).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .frame(maxWidth = Double.infinity)
                            .padding()
                            .background(Color.orange.opacity(0.1))
                            .cornerRadius(12.0).Compose(composectx)

                            // Gesamtpunkte
                            VStack(spacing = 12.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Image(systemName = "sum")
                                        .font(Font.title)
                                        .foregroundColor(Color.blue).Compose(composectx)

                                    VStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendInterpolation(player.totalPoints)
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.title)
                                                .fontWeight(Font.Weight.bold)
                                                .foregroundColor(Color.blue).Compose(composectx)

                                            Text(LocalizedStringKey(stringLiteral = "Gesamtpunkte"))
                                                .font(Font.caption)
                                                .foregroundColor(Color.secondary)
                                                .textCase(Text.Case.uppercase).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .frame(maxWidth = Double.infinity)
                            .padding()
                            .background(Color.blue.opacity(0.1))
                            .cornerRadius(12.0).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding()
            .background(Color.systemBackgroundCompat)
            .cornerRadius(16.0)
            .shadow(color = Color.black.opacity(0.05), radius = 5.0, x = 0.0, y = 2.0).Compose(composectx)
        }
    }

    constructor(player: Player) {
        this.player = player
    }
}

// MARK: - Marktwert-Sektion (ohne 24h Trend)
internal class PlayerMarketValueSection: View {
    internal val player: Player
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    private var actualProfit: Int?
        get() = _actualProfit.wrappedValue
        set(newValue) {
            _actualProfit.wrappedValue = newValue
        }
    private var _actualProfit: skip.ui.State<Int?> = skip.ui.State(null)
    private var isLoadingProfit: Boolean
        get() = _isLoadingProfit.wrappedValue
        set(newValue) {
            _isLoadingProfit.wrappedValue = newValue
        }
    private var _isLoadingProfit: skip.ui.State<Boolean>

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(alignment = HorizontalAlignment.leading, spacing = 16.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(LocalizedStringKey(stringLiteral = "Marktwert & Finanzen"))
                        .font(Font.headline)
                        .fontWeight(Font.Weight.bold).Compose(composectx)

                    VStack(spacing = 12.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Aktueller Marktwert
                            HStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Aktueller Marktwert"))
                                                .font(Font.subheadline)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            Text(formatCurrency(player.marketValue))
                                                .font(Font.title2)
                                                .fontWeight(Font.Weight.bold)
                                                .foregroundColor(Color.green).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    Spacer().Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            // Gewinn/Verlust und letzte Änderung (nur wenn Spieler im Besitz)
                            if (player.userOwnsPlayer) {
                                Divider().Compose(composectx)

                                HStack { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                HStack { ->
                                                    ComposeBuilder { composectx: ComposeContext ->
                                                        Text(LocalizedStringKey(stringLiteral = "Gewinn/Verlust"))
                                                            .font(Font.subheadline)
                                                            .foregroundColor(Color.secondary).Compose(composectx)

                                                        if (isLoadingProfit) {
                                                            ProgressView()
                                                                .scaleEffect(0.6).Compose(composectx)
                                                        }
                                                        ComposeResult.ok
                                                    }
                                                }.Compose(composectx)

                                                val profitValue = actualProfit ?: player.prlo
                                                Text(formatCurrency(profitValue))
                                                    .font(Font.headline)
                                                    .fontWeight(Font.Weight.bold)
                                                    .foregroundColor(if (profitValue >= 0) Color.green else Color.red).Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)

                                        Spacer().Compose(composectx)

                                        if (player.tfhmvt != 0) {
                                            VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(LocalizedStringKey(stringLiteral = "Letzte Änderung"))
                                                        .font(Font.caption)
                                                        .foregroundColor(Color.secondary).Compose(composectx)

                                                    HStack(spacing = 4.0) { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            Image(systemName = if (player.tfhmvt >= 0) "arrow.up" else "arrow.down")
                                                                .foregroundColor(if (player.tfhmvt >= 0) Color.green else Color.red).Compose(composectx)

                                                            Text(formatCurrency(abs(player.tfhmvt)))
                                                                .font(Font.subheadline)
                                                                .fontWeight(Font.Weight.medium)
                                                                .foregroundColor(if (player.tfhmvt >= 0) Color.green else Color.red).Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }.Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)
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
            }
            .padding()
            .background(Color.systemBackgroundCompat)
            .cornerRadius(16.0)
            .shadow(color = Color.black.opacity(0.05), radius = 5.0, x = 0.0, y = 2.0)
            .task { -> MainActor.run {
                if (player.userOwnsPlayer && actualProfit == null) {
                    loadActualProfit()
                }
            } }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedactualProfit by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Int?>, Any>) { mutableStateOf(_actualProfit) }
        _actualProfit = rememberedactualProfit

        val rememberedisLoadingProfit by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_isLoadingProfit) }
        _isLoadingProfit = rememberedisLoadingProfit

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!

        return super.Evaluate(context, options)
    }

    private suspend fun loadActualProfit(): Unit = MainActor.run l@{
        val selectedLeague_0 = kickbaseManager.selectedLeague
        if (selectedLeague_0 == null) {
            return@l
        }

        isLoadingProfit = true
        print("💰 Loading actual profit for player: ${player.fullName} (ID: ${player.id})")

        // Zugriff auf den playerService über KickbaseManager
        val matchtarget_0 = kickbaseManager.loadPlayerMarketValueOnDemand(playerId = player.id, leagueId = selectedLeague_0.id)
        if (matchtarget_0 != null) {
            val profit = matchtarget_0
            actualProfit = profit
            print("✅ Successfully loaded actual profit: €${profit}")
        } else {
            print("⚠️ Could not load actual profit, using fallback value")
        }

        isLoadingProfit = false
    }

    private constructor(player: Player, actualProfit: Int? = null, isLoadingProfit: Boolean = false, privatep: Nothing? = null) {
        this.player = player
        this._actualProfit = skip.ui.State(actualProfit)
        this._isLoadingProfit = skip.ui.State(isLoadingProfit)
    }

    constructor(player: Player): this(player = player, privatep = null) {
    }
}

// MARK: - Spiele und Gegner Sektion (neu)
internal class PlayerMatchesSection: View {
    internal val player: Player
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    private var recentMatches: Array<EnhancedMatchPerformance>
        get() = _recentMatches.wrappedValue.sref({ this.recentMatches = it })
        set(newValue) {
            _recentMatches.wrappedValue = newValue.sref()
        }
    private var _recentMatches: skip.ui.State<Array<EnhancedMatchPerformance>>
    private var upcomingMatches: Array<EnhancedMatchPerformance>
        get() = _upcomingMatches.wrappedValue.sref({ this.upcomingMatches = it })
        set(newValue) {
            _upcomingMatches.wrappedValue = newValue.sref()
        }
    private var _upcomingMatches: skip.ui.State<Array<EnhancedMatchPerformance>>
    private var isLoading: Boolean
        get() = _isLoading.wrappedValue
        set(newValue) {
            _isLoading.wrappedValue = newValue
        }
    private var _isLoading: skip.ui.State<Boolean>

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(alignment = HorizontalAlignment.leading, spacing = 16.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(LocalizedStringKey(stringLiteral = "Spiele & Gegner"))
                        .font(Font.headline)
                        .fontWeight(Font.Weight.bold).Compose(composectx)

                    if (isLoading) {
                        ProgressView(LocalizedStringKey(stringLiteral = "Lade Spiele..."))
                            .frame(maxWidth = Double.infinity, alignment = Alignment.center)
                            .padding(Edge.Set.vertical, 20.0).Compose(composectx)
                    } else {
                        VStack(spacing = 16.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                // Vergangene Spiele
                                if (!recentMatches.isEmpty) {
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 12.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Vergangene Spiele"))
                                                .font(Font.subheadline)
                                                .fontWeight(Font.Weight.semibold)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            ForEach(recentMatches.suffix(5)) { match ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    MatchRow(match = match, isUpcoming = false).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    if (!upcomingMatches.isEmpty) {
                                        Divider().Compose(composectx)
                                    }
                                }

                                // Zukünftige Spiele
                                if (!upcomingMatches.isEmpty) {
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 12.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Kommende Spiele"))
                                                .font(Font.subheadline)
                                                .fontWeight(Font.Weight.semibold)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            ForEach(upcomingMatches.prefix(3)) { match ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    MatchRow(match = match, isUpcoming = true).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                }

                                // Hinweis, falls keine Spiele gefunden wurden
                                if (recentMatches.isEmpty && upcomingMatches.isEmpty) {
                                    Text(LocalizedStringKey(stringLiteral = "Keine Spiele verfügbar."))
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary)
                                        .frame(maxWidth = Double.infinity, alignment = Alignment.center)
                                        .padding(Edge.Set.vertical, 20.0).Compose(composectx)
                                }
                                ComposeResult.ok
                            }
                        }.Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }
            .padding()
            .background(Color.systemBackgroundCompat)
            .cornerRadius(16.0)
            .shadow(color = Color.black.opacity(0.05), radius = 5.0, x = 0.0, y = 2.0)
            .task { -> MainActor.run { loadMatches() } }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedrecentMatches by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Array<EnhancedMatchPerformance>>, Any>) { mutableStateOf(_recentMatches) }
        _recentMatches = rememberedrecentMatches

        val rememberedupcomingMatches by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Array<EnhancedMatchPerformance>>, Any>) { mutableStateOf(_upcomingMatches) }
        _upcomingMatches = rememberedupcomingMatches

        val rememberedisLoading by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_isLoading) }
        _isLoading = rememberedisLoading

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!

        return super.Evaluate(context, options)
    }

    private suspend fun loadMatches(): Unit = MainActor.run l@{
        val selectedLeague_1 = kickbaseManager.selectedLeague
        if (selectedLeague_1 == null) {
            return@l
        }

        isLoading = true
        print("⚽️ Loading enhanced matches for player: ${player.fullName} (ID: ${player.id})")

        run {
            // Lade vergangene Spiele mit Team-Info
            val recent = kickbaseManager.loadPlayerRecentPerformanceWithTeamInfo(playerId = player.id, leagueId = selectedLeague_1.id)
            recentMatches = recent ?: arrayOf()

            // Lade zukünftige Spiele mit Team-Info
            val upcoming = kickbaseManager.loadPlayerUpcomingPerformanceWithTeamInfo(playerId = player.id, leagueId = selectedLeague_1.id)
            upcomingMatches = upcoming ?: arrayOf()

            print("✅ Successfully loaded ${recentMatches.count} recent matches and ${upcomingMatches.count} upcoming matches")
        }

        isLoading = false
    }

    private constructor(player: Player, recentMatches: Array<EnhancedMatchPerformance> = arrayOf(), upcomingMatches: Array<EnhancedMatchPerformance> = arrayOf(), isLoading: Boolean = false, privatep: Nothing? = null) {
        this.player = player
        this._recentMatches = skip.ui.State(recentMatches.sref())
        this._upcomingMatches = skip.ui.State(upcomingMatches.sref())
        this._isLoading = skip.ui.State(isLoading)
    }

    constructor(player: Player): this(player = player, privatep = null) {
    }
}

// MARK: - Match Row Component
internal class MatchRow: View {
    internal val match: EnhancedMatchPerformance
    internal val isUpcoming: Boolean

    // Hilfsfunktion zur Bestimmung der Ergebnisfarbe
    private fun getResultColor(): Color {
        if (isUpcoming) {
            return Color.secondary
        }

        // Parse das Ergebnis (Format: "2:1" oder ähnlich)
        val components = match.result.split(separator = ':')
        if (components.count != 2) {
            return Color.primary // Fallback wenn Ergebnis nicht parsbar ist
        }
        val team1Goals_0 = Int(components[0].trimmingCharacters(in_ = CharacterSet.whitespaces))
        if (team1Goals_0 == null) {
            return Color.primary // Fallback wenn Ergebnis nicht parsbar ist
        }
        val team2Goals_0 = Int(components[1].trimmingCharacters(in_ = CharacterSet.whitespaces))
        if (team2Goals_0 == null) {
            return Color.primary // Fallback wenn Ergebnis nicht parsbar ist
        }

        // Bestimme ob der Spieler in Team 1 oder Team 2 ist
        val isPlayerInTeam1 = match.playerTeamId == match.team1Id

        // Bestimme das Ergebnis aus Sicht des Spielerteams
        val playerTeamGoals = (if (isPlayerInTeam1) team1Goals_0 else team2Goals_0).sref()
        val opponentGoals = (if (isPlayerInTeam1) team2Goals_0 else team1Goals_0).sref()

        if (playerTeamGoals > opponentGoals) {
            return Color.green // Sieg
        } else if (playerTeamGoals < opponentGoals) {
            return Color.red // Niederlage
        } else {
            return Color.primary // Unentschieden (weiß/default)
        }
    }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 8.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    HStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Spieltag
                            Text({
                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                str.appendLiteral("ST ")
                                str.appendInterpolation(match.matchDay)
                                LocalizedStringKey(stringInterpolation = str)
                            }())
                                .font(Font.caption)
                                .fontWeight(Font.Weight.semibold)
                                .foregroundColor(Color.white)
                                .padding(Edge.Set.horizontal, 8.0)
                                .padding(Edge.Set.vertical, 4.0)
                                .background(if (match.isCurrent) Color.orange else Color.blue)
                                .cornerRadius(6.0).Compose(composectx)

                            Spacer().Compose(composectx)

                            // Datum
                            Text(formatMatchDate(match.parsedMatchDate))
                                .font(Font.caption)
                                .foregroundColor(Color.secondary).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    HStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Home vs Away mit Tabellenplatz
                            VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    HStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(match.team1Name)
                                                .font(Font.subheadline)
                                                .fontWeight(Font.Weight.semibold)
                                                .foregroundColor(if (match.playerTeamId == match.team1Id) Color.blue else Color.primary).Compose(composectx)

                                            match.team1Placement?.let { placement ->
                                                Text({
                                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                    str.appendLiteral("(")
                                                    str.appendInterpolation(placement)
                                                    str.appendLiteral(".)")
                                                    LocalizedStringKey(stringInterpolation = str)
                                                }())
                                                    .font(Font.caption2)
                                                    .foregroundColor(Color.secondary).Compose(composectx)
                                            }
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    Text(LocalizedStringKey(stringLiteral = "vs"))
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary).Compose(composectx)

                                    HStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(match.team2Name)
                                                .font(Font.subheadline)
                                                .fontWeight(Font.Weight.semibold)
                                                .foregroundColor(if (match.playerTeamId == match.team2Id) Color.blue else Color.primary).Compose(composectx)

                                            match.team2Placement?.let { placement ->
                                                Text({
                                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                    str.appendLiteral("(")
                                                    str.appendInterpolation(placement)
                                                    str.appendLiteral(".)")
                                                    LocalizedStringKey(stringInterpolation = str)
                                                }())
                                                    .font(Font.caption2)
                                                    .foregroundColor(Color.secondary).Compose(composectx)
                                            }
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            Spacer().Compose(composectx)

                            // Ergebnis oder Status
                            VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    if (isUpcoming) {
                                        Text(LocalizedStringKey(stringLiteral = "-:-"))
                                            .font(Font.headline)
                                            .fontWeight(Font.Weight.bold)
                                            .foregroundColor(Color.secondary).Compose(composectx)
                                    } else {
                                        Text(match.result)
                                            .font(Font.headline)
                                            .fontWeight(Font.Weight.bold)
                                            .foregroundColor(getResultColor()).Compose(composectx)
                                    }

                                    // Spieler Status/Punkte
                                    if (isUpcoming) {
                                        Text(LocalizedStringKey(stringLiteral = "Geplant"))
                                            .font(Font.caption2)
                                            .foregroundColor(Color.orange).Compose(composectx)
                                    } else {
                                        HStack(spacing = 4.0) { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                Text({
                                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                    str.appendInterpolation(match.points)
                                                    str.appendLiteral(" Pkt")
                                                    LocalizedStringKey(stringInterpolation = str)
                                                }())
                                                    .font(Font.caption2)
                                                    .fontWeight(Font.Weight.medium)
                                                    .foregroundColor(if (match.points > 0) Color.green else Color.secondary).Compose(composectx)

                                                if (match.wasStartingEleven) {
                                                    Image(systemName = "star.fill")
                                                        .font(Font.caption2)
                                                        .foregroundColor(Color.orange).Compose(composectx)
                                                } else if (match.wasSubstitute) {
                                                    Image(systemName = "arrow.up.circle.fill")
                                                        .font(Font.caption2)
                                                        .foregroundColor(Color.blue).Compose(composectx)
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
            .padding(Edge.Set.vertical, 8.0)
            .padding(Edge.Set.horizontal, 12.0)
            .background(if (match.isCurrent) Color.orange.opacity(0.1) else Color.clear)
            .cornerRadius(8.0).Compose(composectx)
        }
    }

    constructor(match: EnhancedMatchPerformance, isUpcoming: Boolean) {
        this.match = match
        this.isUpcoming = isUpcoming
    }
}

// Helper Funktion für Datum-Formatierung
private fun formatMatchDate(date: Date): String {
    val formatter = DateFormatter()
    formatter.dateStyle = DateFormatter.Style.short
    formatter.timeStyle = DateFormatter.Style.none
    formatter.locale = Locale(identifier = "de_DE")
    return formatter.string(from = date)
}

private val dateFormatter: DateFormatter = linvoke l@{ ->
    val formatter = DateFormatter()
    formatter.dateStyle = DateFormatter.Style.medium
    formatter.timeStyle = DateFormatter.Style.short
    formatter.locale = Locale(identifier = "de_DE")
    return@l formatter
}

// MARK: - Marktwert-Trend der letzten 3 Tage mit echten DailyMarketValueChange Daten
internal class PlayerMarketTrendSection: View {
    internal val player: Player
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
    private var hasLoaded: Boolean
        get() = _hasLoaded.wrappedValue
        set(newValue) {
            _hasLoaded.wrappedValue = newValue
        }
    private var _hasLoaded: skip.ui.State<Boolean>

    internal val marketTrendData: Array<Tuple4<String, Int, Int, Double>>
        get() {
            val history_0 = marketValueHistory
            if (history_0 == null) {
                // Zeige nur aktuellen Marktwert ohne historische Daten
                return arrayOf(
                    Tuple4("Heute", player.marketValue, player.tfhmvt, 0.0),
                    Tuple4("Gestern", 0, 0, 0.0),
                    Tuple4("Vorgestern", 0, 0, 0.0)
                )
            }

            // Verwende die letzten 3 Tage aus der echten Historie
            val sortedDailyChanges = history_0.dailyChanges.sorted { it, it_1 -> it.daysAgo < it_1.daysAgo }
            val last3Days = Array(sortedDailyChanges.prefix(3))

            return last3Days.enumerated().map l@{ (index, dailyChange) ->
                val dayName: String
                when (dailyChange.daysAgo) {
                    0 -> dayName = "Heute"
                    1 -> dayName = "Gestern"
                    2 -> dayName = "Vorgestern"
                    else -> dayName = "${dailyChange.daysAgo} Tage"
                }

                return@l Tuple4(dayName, dailyChange.value, dailyChange.change, dailyChange.percentageChange)
            }
        }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(alignment = HorizontalAlignment.leading, spacing = 16.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    HStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(LocalizedStringKey(stringLiteral = "Marktwertentwicklung"))
                                .font(Font.headline)
                                .fontWeight(Font.Weight.bold).Compose(composectx)

                            Spacer().Compose(composectx)

                            if (isLoadingHistory) {
                                ProgressView()
                                    .scaleEffect(0.8).Compose(composectx)
                            } else {
                                Text(LocalizedStringKey(stringLiteral = "(3 Tage)"))
                                    .font(Font.caption)
                                    .foregroundColor(Color.secondary).Compose(composectx)
                            }
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    VStack(spacing = 8.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Header-Zeile
                            HStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Tag"))
                                        .font(Font.caption)
                                        .fontWeight(Font.Weight.semibold)
                                        .foregroundColor(Color.secondary)
                                        .frame(width = 70.0, alignment = Alignment.leading).Compose(composectx)

                                    Spacer().Compose(composectx)

                                    Text(LocalizedStringKey(stringLiteral = "Marktwert"))
                                        .font(Font.caption)
                                        .fontWeight(Font.Weight.semibold)
                                        .foregroundColor(Color.secondary)
                                        .frame(width = 80.0, alignment = Alignment.center).Compose(composectx)

                                    Spacer().Compose(composectx)

                                    Text(LocalizedStringKey(stringLiteral = "Änderung"))
                                        .font(Font.caption)
                                        .fontWeight(Font.Weight.semibold)
                                        .foregroundColor(Color.secondary)
                                        .frame(width = 70.0, alignment = Alignment.center).Compose(composectx)

                                    Spacer().Compose(composectx)

                                    Text(LocalizedStringKey(stringLiteral = "%"))
                                        .font(Font.caption)
                                        .fontWeight(Font.Weight.semibold)
                                        .foregroundColor(Color.secondary)
                                        .frame(width = 50.0, alignment = Alignment.trailing).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .padding(Edge.Set.horizontal, 12.0)
                            .padding(Edge.Set.vertical, 8.0)
                            .background(Color.systemGray6Compat)
                            .cornerRadius(8.0).Compose(composectx)

                            // Daten-Zeilen
                            ForEach(Array(marketTrendData.enumerated()), id = { it.offset }) { (index, data) ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    HStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            // Tag
                                            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(data.day)
                                                        .font(Font.subheadline)
                                                        .fontWeight(Font.Weight.medium)
                                                        .foregroundColor(if (index == 0) Color.primary else Color.secondary).Compose(composectx)

                                                    if (index == 0) {
                                                        Text(LocalizedStringKey(stringLiteral = "aktuell"))
                                                            .font(Font.caption2)
                                                            .foregroundColor(Color.blue).Compose(composectx)
                                                    }
                                                    ComposeResult.ok
                                                }
                                            }
                                            .frame(width = 70.0, alignment = Alignment.leading).Compose(composectx)

                                            Spacer().Compose(composectx)

                                            // Marktwert
                                            Text(if (data.value > 0) formatCurrencyShort(data.value) else "-")
                                                .font(Font.subheadline)
                                                .fontWeight(Font.Weight.semibold)
                                                .frame(width = 80.0, alignment = Alignment.center)
                                                .foregroundColor(if (data.value > 0) Color.primary else Color.secondary).Compose(composectx)

                                            Spacer().Compose(composectx)

                                            // Veränderung absolut
                                            HStack(spacing = 2.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    if (data.change != 0) {
                                                        Image(systemName = if (data.change >= 0) "arrow.up" else "arrow.down")
                                                            .font(Font.caption2)
                                                            .foregroundColor(if (data.change >= 0) Color.green else Color.red).Compose(composectx)
                                                    }

                                                    Text(if (data.change == 0) "±0" else formatCurrencyShort(abs(data.change)))
                                                        .font(Font.caption)
                                                        .fontWeight(Font.Weight.medium)
                                                        .foregroundColor(if (data.change >= 0) Color.green else (if (data.change < 0) Color.red else Color.secondary)).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }
                                            .frame(width = 70.0, alignment = Alignment.center).Compose(composectx)

                                            Spacer().Compose(composectx)

                                            // Veränderung prozentual
                                            Text(if (abs(data.changePercent) < 0.1) "±0%" else String(format = if (data.changePercent >= 0) "+%.1f%%" else "%.1f%%", data.changePercent))
                                                .font(Font.caption)
                                                .fontWeight(Font.Weight.medium)
                                                .foregroundColor(if (abs(data.changePercent) < 0.1) Color.secondary else (if (data.changePercent >= 0) Color.green else Color.red))
                                                .frame(width = 50.0, alignment = Alignment.trailing).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }
                                    .padding(Edge.Set.vertical, 6.0)
                                    .padding(Edge.Set.horizontal, 12.0)
                                    .background(if (index == 0) Color.blue.opacity(0.1) else Color.clear)
                                    .cornerRadius(6.0)
                                    .opacity(if (data.value > 0) 1.0 else 0.5).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    if (marketValueHistory == null && !isLoadingHistory && hasLoaded) {
                        Text(LocalizedStringKey(stringLiteral = "Keine Marktwerthistorie verfügbar"))
                            .font(Font.caption)
                            .foregroundColor(Color.secondary)
                            .padding(Edge.Set.top, 8.0).Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }
            .padding()
            .background(Color.systemBackgroundCompat)
            .cornerRadius(16.0)
            .shadow(color = Color.black.opacity(0.05), radius = 5.0, x = 0.0, y = 2.0)
            .task { -> MainActor.run l@{
                if (hasLoaded) {
                    return@l
                }
                loadMarketValueHistory()
            } }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedmarketValueHistory by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<MarketValueChange?>, Any>) { mutableStateOf(_marketValueHistory) }
        _marketValueHistory = rememberedmarketValueHistory

        val rememberedisLoadingHistory by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_isLoadingHistory) }
        _isLoadingHistory = rememberedisLoadingHistory

        val rememberedhasLoaded by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_hasLoaded) }
        _hasLoaded = rememberedhasLoaded

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!

        return super.Evaluate(context, options)
    }

    private suspend fun loadMarketValueHistory(): Unit = MainActor.run l@{
        if (player.id.isEmpty) {
            print("⚠️ Cannot load market value history: missing player ID or league")
            hasLoaded = true
            return@l
        }
        val selectedLeague_2 = kickbaseManager.selectedLeague
        if (selectedLeague_2 == null) {
            print("⚠️ Cannot load market value history: missing player ID or league")
            hasLoaded = true
            return@l
        }

        isLoadingHistory = true
        print("📈 Loading market value history for player: ${player.fullName} (ID: ${player.id})")

        run {
            // Zugriff auf den playerService über KickbaseManager
            val history = kickbaseManager.loadPlayerMarketValueHistory(playerId = player.id, leagueId = selectedLeague_2.id)

            if (history != null) {
                print("✅ Successfully loaded market value history with ${history.dailyChanges.count} daily changes")
                marketValueHistory = history
            } else {
                print("⚠️ No market value history returned from API")
            }
        }

        isLoadingHistory = false
        hasLoaded = true
    }

    private constructor(player: Player, marketValueHistory: MarketValueChange? = null, isLoadingHistory: Boolean = false, hasLoaded: Boolean = false, privatep: Nothing? = null) {
        this.player = player
        this._marketValueHistory = skip.ui.State(marketValueHistory)
        this._isLoadingHistory = skip.ui.State(isLoadingHistory)
        this._hasLoaded = skip.ui.State(hasLoaded)
    }

    constructor(player: Player): this(player = player, privatep = null) {
    }
}

// MARK: - Helper Views
internal class PlayerInfoCard: View {
    internal val title: String
    internal val value: String
    internal val icon: String
    internal val color: Color

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 8.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Image(systemName = icon)
                        .font(Font.title3)
                        .foregroundColor(color).Compose(composectx)

                    Text(value)
                        .font(Font.subheadline)
                        .fontWeight(Font.Weight.semibold)
                        .multilineTextAlignment(TextAlignment.center)
                        .lineLimit(2)
                        .minimumScaleFactor(0.8).Compose(composectx)

                    Text(title)
                        .font(Font.caption)
                        .foregroundColor(Color.secondary)
                        .multilineTextAlignment(TextAlignment.center).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .frame(height = 70.0)
            .frame(maxWidth = Double.infinity)
            .padding(Edge.Set.vertical, 8.0)
            .background(color.opacity(0.1))
            .cornerRadius(10.0).Compose(composectx)
        }
    }

    constructor(title: String, value: String, icon: String, color: Color) {
        this.title = title
        this.value = value
        this.icon = icon
        this.color = color
    }
}

// MARK: - Helper Functions
private fun formatCurrency(value: Int): String {
    val formatter = NumberFormatter()
    formatter.numberStyle = NumberFormatter.Style.currency
    formatter.currencyCode = "EUR"
    formatter.maximumFractionDigits = 0
    return formatter.string(from = NSNumber(value = value)) ?: "${value} €"
}

private fun formatCurrencyShort(value: Int): String {
    if (value >= 1_000_000) {
        val millions = Double(value) / 1_000_000
        return "${millions}M €"
    } else if (value >= 1000) {
        val thousands = Double(value) / 1000
        return "${thousands}k €"
    } else {
        return "${value} €"
    }
}

private fun getPlayerStatusText(status: Int): String {
    when (status) {
        0 -> return "Verfügbar"
        1 -> return "Verletzt"
        2 -> return "Angeschlagen"
        3 -> return "Gesperrt"
        4 -> return "Aufbautraining"
        8 -> return "Sperre" // Neuer Status für Sperre
        else -> return "Unbekannt"
    }
}

// MARK: - Transfer-Vergleich & Alternativen Sektion
internal class PlayerAlternativesSection: View {
    internal val player: Player
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    private var alternatives: Array<MarketPlayer>
        get() = _alternatives.wrappedValue.sref({ this.alternatives = it })
        set(newValue) {
            _alternatives.wrappedValue = newValue.sref()
        }
    private var _alternatives: skip.ui.State<Array<MarketPlayer>>
    private var isLoading: Boolean
        get() = _isLoading.wrappedValue
        set(newValue) {
            _isLoading.wrappedValue = newValue
        }
    private var _isLoading: skip.ui.State<Boolean>
    private var comparisonMetrics: Array<ComparisonMetric>
        get() = _comparisonMetrics.wrappedValue.sref({ this.comparisonMetrics = it })
        set(newValue) {
            _comparisonMetrics.wrappedValue = newValue.sref()
        }
    private var _comparisonMetrics: skip.ui.State<Array<ComparisonMetric>>

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(alignment = HorizontalAlignment.leading, spacing = 16.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(LocalizedStringKey(stringLiteral = "Transfer-Alternativen"))
                        .font(Font.headline)
                        .fontWeight(Font.Weight.bold).Compose(composectx)

                    if (isLoading) {
                        ProgressView(LocalizedStringKey(stringLiteral = "Lade Alternativen..."))
                            .frame(maxWidth = Double.infinity, alignment = Alignment.center)
                            .padding(Edge.Set.vertical, 20.0).Compose(composectx)
                    } else if (alternatives.isEmpty) {
                        Text(LocalizedStringKey(stringLiteral = "Keine besseren Alternativen gefunden"))
                            .font(Font.caption)
                            .foregroundColor(Color.secondary)
                            .frame(maxWidth = Double.infinity, alignment = Alignment.center)
                            .padding(Edge.Set.vertical, 20.0).Compose(composectx)
                    } else {
                        // Vergleichs-Kennzahlen
                        VStack(spacing = 12.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                Text(LocalizedStringKey(stringLiteral = "Vergleich mit Alternativen"))
                                    .font(Font.subheadline)
                                    .fontWeight(Font.Weight.semibold)
                                    .foregroundColor(Color.secondary).Compose(composectx)

                                ComparisonMetricsView(currentPlayer = player, metrics = comparisonMetrics).Compose(composectx)
                                ComposeResult.ok
                            }
                        }
                        .padding()
                        .background(Color.systemGray6Compat)
                        .cornerRadius(12.0).Compose(composectx)

                        Divider().Compose(composectx)

                        // Alternativen-Liste
                        VStack(alignment = HorizontalAlignment.leading, spacing = 12.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                Text(LocalizedStringKey(stringLiteral = "Empfohlene Spieler"))
                                    .font(Font.subheadline)
                                    .fontWeight(Font.Weight.semibold)
                                    .foregroundColor(Color.secondary).Compose(composectx)

                                ForEach(alternatives.prefix(5), id = { it.id }) { alternative ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        AlternativePlayerCard(currentPlayer = player, alternativePlayer = alternative).Compose(composectx)
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
            .background(Color.systemBackgroundCompat)
            .cornerRadius(16.0)
            .shadow(color = Color.black.opacity(0.05), radius = 5.0, x = 0.0, y = 2.0)
            .task { -> MainActor.run { loadAlternatives() } }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedalternatives by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Array<MarketPlayer>>, Any>) { mutableStateOf(_alternatives) }
        _alternatives = rememberedalternatives

        val rememberedisLoading by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_isLoading) }
        _isLoading = rememberedisLoading

        val rememberedcomparisonMetrics by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Array<ComparisonMetric>>, Any>) { mutableStateOf(_comparisonMetrics) }
        _comparisonMetrics = rememberedcomparisonMetrics

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!

        return super.Evaluate(context, options)
    }

    private suspend fun loadAlternatives(): Unit = MainActor.run l@{
        val selectedLeague_3 = kickbaseManager.selectedLeague
        if (selectedLeague_3 == null) {
            return@l
        }

        isLoading = true
        print("🔄 Loading alternatives for player: ${player.fullName} (Position: ${player.position})")

        try {
            val loadedAlternatives = findBetterAlternatives(for_ = player, in_ = selectedLeague_3)

            alternatives = loadedAlternatives

            // Berechne Vergleichs-Metriken
            if (!loadedAlternatives.isEmpty) {
                comparisonMetrics = calculateComparisonMetrics(currentPlayer = player, alternatives = loadedAlternatives)
            }

            print("✅ Found ${loadedAlternatives.count} alternatives")
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Error loading alternatives: ${error}")
        }

        isLoading = false
    }

    private suspend fun findBetterAlternatives(for_: Player, in_: League): Array<MarketPlayer> = Async.run l@{
        val player = for_
        val league = in_
        val marketPlayers = kickbaseManager.mainactor { it.authenticatedPlayerService }.loadMarketPlayers(for_ = league)

        // Filter: Gleiche Position
        val samePosition = marketPlayers.filter { it -> it.position == player.position }

        // Filter: Nicht der aktuelle Spieler selbst
        val filtered = samePosition.filter { it -> it.id != player.id }

        // Berechne Scores für jeden Kandidaten
        var candidates: Array<AlternativeCandidate> = arrayOf()

        for (candidate in filtered.sref()) {
            val score = calculateAlternativeScore(current = player, alternative = candidate)
            // Nur Kandidaten mit positivem Score berücksichtigen
            if (score > 0) {
                candidates.append(AlternativeCandidate(player = candidate, score = score))
            }
        }

        // Sortiere nach Score absteigend und nimm Top 5
        return@l candidates
            .prefix(5)
            .map { it -> it.player }
    }

    private fun calculateAlternativeScore(current: Player, alternative: MarketPlayer): Double = 0.0 // Stubbed for transpilation safety

    private fun calculateComparisonMetrics(currentPlayer: Player, alternatives: Array<MarketPlayer>): Array<ComparisonMetric> = arrayOf() // Stubbed for transpilation safety

    private constructor(player: Player, alternatives: Array<MarketPlayer> = arrayOf(), isLoading: Boolean = false, comparisonMetrics: Array<ComparisonMetric> = arrayOf(), privatep: Nothing? = null) {
        this.player = player
        this._alternatives = skip.ui.State(alternatives.sref())
        this._isLoading = skip.ui.State(isLoading)
        this._comparisonMetrics = skip.ui.State(comparisonMetrics.sref())
    }

    constructor(player: Player): this(player = player, privatep = null) {
    }
}

// MARK: - Comparison Metric Model
internal class ComparisonMetric {
    internal val name: String
    internal val currentValue: Double
    internal val alternativeValue: Double
    internal val change: Double
    internal val isPositive: Boolean

    constructor(name: String, currentValue: Double, alternativeValue: Double, change: Double, isPositive: Boolean) {
        this.name = name
        this.currentValue = currentValue
        this.alternativeValue = alternativeValue
        this.change = change
        this.isPositive = isPositive
    }
}

// MARK: - Vergleichs-Metriken View
internal class ComparisonMetricsView: View {
    internal val currentPlayer: Player
    internal val metrics: Array<ComparisonMetric>

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 12.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    ForEach(metrics, id = { it.name }) { metric ->
                        ComposeBuilder { composectx: ComposeContext ->
                            HStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(metric.name)
                                                .font(Font.caption)
                                                .fontWeight(Font.Weight.semibold)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            HStack(spacing = 8.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text(formatMetricValue(metric.currentValue))
                                                        .font(Font.subheadline)
                                                        .fontWeight(Font.Weight.semibold)
                                                        .foregroundColor(Color.primary).Compose(composectx)

                                                    Image(systemName = "arrow.right")
                                                        .font(Font.caption2)
                                                        .foregroundColor(Color.secondary).Compose(composectx)

                                                    Text(formatMetricValue(metric.alternativeValue))
                                                        .font(Font.subheadline)
                                                        .fontWeight(Font.Weight.semibold)
                                                        .foregroundColor(if (metric.isPositive) Color.green else Color.red).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    Spacer().Compose(composectx)

                                    VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            HStack(spacing = 2.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Image(systemName = if (metric.isPositive) "arrow.up" else "arrow.down")
                                                        .font(Font.caption2)
                                                        .foregroundColor(if (metric.isPositive) Color.green else Color.red).Compose(composectx)

                                                    Text(LocalizedStringKey(stringLiteral = "0"))
                                                        .font(Font.caption2)
                                                        .fontWeight(Font.Weight.semibold)
                                                        .foregroundColor(if (metric.isPositive) Color.green else Color.red).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            Text(LocalizedStringKey(stringLiteral = "0%"))
                                                .font(Font.caption2)
                                                .foregroundColor(if (metric.isPositive) Color.green else Color.red).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .padding(Edge.Set.vertical, 8.0).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    private fun formatMetricValue(value: Double): String = "${Int(value)}" // Stubbed for transpilation safety

    constructor(currentPlayer: Player, metrics: Array<ComparisonMetric>) {
        this.currentPlayer = currentPlayer
        this.metrics = metrics.sref()
    }
}

// MARK: - Alternative Player Card
internal class AlternativePlayerCard: View {
    internal val currentPlayer: Player
    internal val alternativePlayer: MarketPlayer

    internal val improvementPercentage: Double
        get() = 0.0 // Stubbed for transpilation safety

    internal val valueImprovement: Double
        get() = 0.0 // Stubbed for transpilation safety

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 12.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Header mit Foto und Namen
                    HStack(spacing = 12.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            AsyncImage(url = alternativePlayer.imageUrl, content = { image ->
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
                                                .fill(positionColor(alternativePlayer.position).opacity(0.3)).Compose(composectx)

                                            Image(systemName = "person.fill")
                                                .font(Font.system(size = 20.0))
                                                .foregroundColor(positionColor(alternativePlayer.position)).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            })
                            .frame(width = 50.0, height = 50.0)
                            .clipShape(Circle())
                            .overlay { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Circle()
                                        .strokeBorder(positionColor(alternativePlayer.position), lineWidth = 2.0).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(alternativePlayer.fullName)
                                        .font(Font.subheadline)
                                        .fontWeight(Font.Weight.bold)
                                        .lineLimit(1).Compose(composectx)

                                    HStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(alternativePlayer.fullTeamName)
                                                .font(Font.caption2)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            Text(LocalizedStringKey(stringLiteral = "•"))
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            Text(alternativePlayer.positionName)
                                                .font(Font.caption2)
                                                .fontWeight(Font.Weight.semibold)
                                                .foregroundColor(positionColor(alternativePlayer.position)).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            Spacer().Compose(composectx)

                            // Score Badge
                            VStack(alignment = HorizontalAlignment.center, spacing = 2.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "LEISTUNG"))
                                        .font(Font.caption2)
                                        .fontWeight(Font.Weight.bold)
                                        .foregroundColor(Color.white).Compose(composectx)

                                    ZStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Circle()
                                                .fill(Color.green).Compose(composectx) // Stubbed color logic
                                            /* .fill(improvementPercentage >= 0 ? Color.green : Color.orange) */

                                            Text(String(format = "%+.0f%%", improvementPercentage))
                                                .font(Font.caption2)
                                                .fontWeight(Font.Weight.bold)
                                                .foregroundColor(Color.white)
                                                .lineLimit(1).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }
                                    .frame(width = 40.0, height = 40.0).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    Divider().Compose(composectx)

                    // Vergleichs-Details
                    HStack(spacing = 16.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Leistung
                            VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Leistung"))
                                        .font(Font.caption2)
                                        .fontWeight(Font.Weight.semibold)
                                        .foregroundColor(Color.secondary).Compose(composectx)

                                    HStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(String(format = "%.1f", currentPlayer.averagePoints))
                                                .font(Font.caption)
                                                .foregroundColor(Color.primary).Compose(composectx)

                                            Text(LocalizedStringKey(stringLiteral = "→"))
                                                .font(Font.caption2)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            Text(String(format = "%.1f", alternativePlayer.averagePoints))
                                                .font(Font.caption)
                                                .fontWeight(Font.Weight.bold)
                                                .foregroundColor(Color.green).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            Divider().Compose(composectx)

                            // Marktwert
                            VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Marktwert"))
                                        .font(Font.caption2)
                                        .fontWeight(Font.Weight.semibold)
                                        .foregroundColor(Color.secondary).Compose(composectx)

                                    HStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(formatCurrencyShort(currentPlayer.marketValue))
                                                .font(Font.caption)
                                                .foregroundColor(Color.primary).Compose(composectx)

                                            Text(LocalizedStringKey(stringLiteral = "→"))
                                                .font(Font.caption2)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            Text(formatCurrencyShort(alternativePlayer.price))
                                                .font(Font.caption)
                                                .fontWeight(Font.Weight.bold)
                                                .foregroundColor(Color.green).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            Divider().Compose(composectx)

                            // Status
                            VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Status"))
                                        .font(Font.caption2)
                                        .fontWeight(Font.Weight.semibold)
                                        .foregroundColor(Color.secondary).Compose(composectx)

                                    HStack(spacing = 4.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Image(systemName = getStatusIcon(alternativePlayer.status))
                                                .font(Font.caption)
                                                .foregroundColor(getStatusColor(alternativePlayer.status)).Compose(composectx)

                                            Text(getPlayerStatusText(alternativePlayer.status))
                                                .font(Font.caption2)
                                                .fontWeight(Font.Weight.semibold)
                                                .foregroundColor(getStatusColor(alternativePlayer.status)).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding(8.0)
                    .background(Color.systemGray6Compat)
                    .cornerRadius(8.0).Compose(composectx)

                    // Value-for-Money Bonus
                    /*
                    if valueImprovement > 10 {
                    HStack(spacing: 8) {
                    Image(systemName: "star.fill")
                    .font(.caption)
                    .foregroundColor(.orange)

                    VStack(alignment: .leading, spacing: 2) {
                    Text("Besseres Preis-Leistungs-Verhältnis")
                    .font(.caption2)
                    .fontWeight(.semibold)

                    Text(String(format: "%.0f%% besserer Value", valueImprovement))
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    }

                    Spacer()
                    }
                    .padding(8)
                    .background(Color.orange.opacity(0.1))
                    .cornerRadius(8)
                    }
                    */
                    ComposeResult.ok
                }
            }
            .padding()
            .background(Color.systemGray6Compat)
            .cornerRadius(12.0).Compose(composectx)
        }
    }

    private fun getStatusIcon(status: Int): String {
        when (status) {
            0 -> return "checkmark.circle.fill"
            1 -> return "cross.circle.fill"
            2 -> return "pills.fill"
            3 -> return "exclamationmark.triangle.fill"
            4 -> return "dumbbell.fill"
            8 -> return "rectangle.fill"
            else -> return "questionmark.circle.fill"
        }
    }

    private fun getStatusColor(status: Int): Color {
        when (status) {
            0 -> return Color.green
            1 -> return Color.red
            2 -> return Color.orange
            3 -> return Color.red
            4 -> return Color.blue
            8 -> return Color.red
            else -> return Color.gray
        }
    }

    constructor(currentPlayer: Player, alternativePlayer: MarketPlayer) {
        this.currentPlayer = currentPlayer
        this.alternativePlayer = alternativePlayer
    }
}

// MARK: - Preview
