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

internal class LineupComparisonView: View {
    internal val comparison: LineupComparison
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    private var showTeamOnly: Boolean
        get() = _showTeamOnly.wrappedValue
        set(newValue) {
            _showTeamOnly.wrappedValue = newValue
        }
    private var _showTeamOnly: skip.ui.State<Boolean>

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            ScrollView { ->
                ComposeBuilder { composectx: ComposeContext ->
                    VStack(spacing = 20.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Tab-Selector
                            Picker(LocalizedStringKey(stringLiteral = "Ansicht"), selection = Binding({ _showTeamOnly.wrappedValue }, { it -> _showTeamOnly.wrappedValue = it })) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Nur eigene Spieler")).tag(true).Compose(composectx)
                                    if (comparison.shouldShowHybrid) {
                                        Text(LocalizedStringKey(stringLiteral = "Mit Marktspieler")).tag(false).Compose(composectx)
                                    }
                                    ComposeResult.ok
                                }
                            }
                            .pickerStyle(PickerStyle.segmented)
                            .padding().Compose(composectx)

                            if (showTeamOnly) {
                                LineupDetailView(lineup = comparison.teamOnlyLineup, teamPlayers = kickbaseManager.teamPlayers, marketPlayers = arrayOf()).Compose(composectx)
                            } else {
                                if (comparison.shouldShowHybrid) {
                                    comparison.hybridLineup?.let { hybridLineup ->
                                        LineupDetailView(lineup = hybridLineup, teamPlayers = kickbaseManager.teamPlayers, marketPlayers = kickbaseManager.marketPlayers).Compose(composectx)

                                        // Zusammenfassung der Hybrid-Empfehlungen
                                        HybridLineupSummary(comparison = comparison, teamPlayers = kickbaseManager.teamPlayers, marketPlayers = kickbaseManager.marketPlayers).Compose(composectx)
                                    }
                                }
                            }

                            Spacer().Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding(Edge.Set.horizontal).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .navigationTitle(LocalizedStringKey(stringLiteral = "Aufstellung optimieren")).Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedshowTeamOnly by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_showTeamOnly) }
        _showTeamOnly = rememberedshowTeamOnly

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!

        return super.Evaluate(context, options)
    }

    private constructor(comparison: LineupComparison, showTeamOnly: Boolean = true, privatep: Nothing? = null) {
        this.comparison = comparison
        this._showTeamOnly = skip.ui.State(showTeamOnly)
    }

    constructor(comparison: LineupComparison): this(comparison = comparison, privatep = null) {
    }
}

internal class LineupDetailView: View {
    internal val lineup: OptimalLineupResult
    internal val teamPlayers: Array<Player>
    internal val marketPlayers: Array<MarketPlayer>

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 16.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Header mit Formation und Scores
                    VStack(spacing = 8.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(lineup.formationName)
                                .font(Font.title2)
                                .fontWeight(Font.Weight.bold).Compose(composectx)

                            HStack(spacing = 20.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    VStack(alignment = HorizontalAlignment.leading) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Gesamtbewertung"))
                                                .font(Font.caption)
                                                .foregroundColor(Color.secondary).Compose(composectx)
                                            Text(String(format = "%.1f", lineup.totalLineupScore))
                                                .font(Font.title3)
                                                .fontWeight(Font.Weight.bold).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    VStack(alignment = HorizontalAlignment.leading) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Ø pro Spieler"))
                                                .font(Font.caption)
                                                .foregroundColor(Color.secondary).Compose(composectx)
                                            Text(String(format = "%.1f", lineup.averagePlayerScore))
                                                .font(Font.title3)
                                                .fontWeight(Font.Weight.bold).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    if (lineup.isHybridWithMarketPlayers) {
                                        VStack(alignment = HorizontalAlignment.leading) { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                Text(LocalizedStringKey(stringLiteral = "Investment"))
                                                    .font(Font.caption)
                                                    .foregroundColor(Color.secondary).Compose(composectx)
                                                Text({
                                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                    str.appendLiteral("€")
                                                    str.appendInterpolation(lineup.totalMarketCost / 1_000_000)
                                                    str.appendLiteral("M")
                                                    LocalizedStringKey(stringInterpolation = str)
                                                }())
                                                    .font(Font.title3)
                                                    .fontWeight(Font.Weight.bold)
                                                    .foregroundColor(Color.blue).Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)
                                    }

                                    Spacer().Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .padding()
                            .background(Color.systemGray6Compat)
                            .cornerRadius(8.0).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    // Aufstellung visualisiert
                    VStack(spacing = 12.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(LocalizedStringKey(stringLiteral = "Aufstellung"))
                                .font(Font.headline)
                                .frame(maxWidth = Double.infinity, alignment = Alignment.leading).Compose(composectx)

                            // Gruppiere Slots nach Position
                            var slotsByPosition: Dictionary<Int, Array<LineupSlot>> = dictionaryOf()
                            for (slot in lineup.slots.sref()) {
                                var list = (slotsByPosition[slot.positionType] ?: arrayOf()).sref()
                                list.append(slot)
                                slotsByPosition[slot.positionType] = list.sref()
                            }
                            val positions = arrayOf(1, 2, 3, 4).filter { it -> slotsByPosition[it] != null }

                            ForEach(positions, id = { it }) { position ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    val positionName = positionName(position)
                                    val slots = (slotsByPosition[position] ?: arrayOf()).sref()

                                    VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(positionName)
                                                .font(Font.caption)
                                                .fontWeight(Font.Weight.semibold)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            VStack(spacing = 6.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    ForEach(slots, id = { it.id }) { slot ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            LineupSlotRowView(slot = slot, teamPlayers = teamPlayers, marketPlayers = marketPlayers).Compose(composectx)
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

    private fun positionName(position: Int): String {
        when (position) {
            1 -> return "Torwart (TW)"
            2 -> return "Abwehr (ABW)"
            3 -> return "Mittelfeld (MF)"
            4 -> return "Stürmer (ST)"
            else -> return "Unbekannt"
        }
    }

    constructor(lineup: OptimalLineupResult, teamPlayers: Array<Player>, marketPlayers: Array<MarketPlayer>) {
        this.lineup = lineup
        this.teamPlayers = teamPlayers.sref()
        this.marketPlayers = marketPlayers.sref()
    }
}

internal class LineupSlotRowView: View {
    internal val slot: LineupSlot
    internal val teamPlayers: Array<Player>
    internal val marketPlayers: Array<MarketPlayer>
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
                    // Score-Indikator
                    VStack(alignment = HorizontalAlignment.center, spacing = 2.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(String(format = "%.0f", slot.slotScore))
                                .font(Font.headline)
                                .fontWeight(Font.Weight.bold)
                                .foregroundColor(Color.white)
                                .frame(width = 40.0, height = 40.0)
                                .background(scoreColor(slot.slotScore))
                                .cornerRadius(8.0).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    // Spieler-Info
                    val matchtarget_0 = slot.recommendedMarketPlayerId
                    if (matchtarget_0 != null) {
                        val marketId = matchtarget_0
                        val matchtarget_1 = marketPlayers.first(where = { it -> it.id == marketId })
                        if (matchtarget_1 != null) {
                            val marketPlayer = matchtarget_1
                            // Markt-Spieler
                            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    HStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    HStack(spacing = 4.0) { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            Text(marketPlayer.fullName)
                                                                .font(Font.subheadline)
                                                                .fontWeight(Font.Weight.semibold).Compose(composectx)

                                                            // Ligainsider Status Icon
                                                            if (!ligainsiderService.matches.isEmpty) {
                                                                val status = ligainsiderService.getPlayerStatus(firstName = marketPlayer.firstName, lastName = marketPlayer.lastName)
                                                                if (status != LigainsiderStatus.out) {
                                                                    Image(systemName = ligainsiderService.getIcon(for_ = status))
                                                                        .foregroundColor(ligainsiderService.getColor(for_ = status))
                                                                        .font(Font.caption2).Compose(composectx)
                                                                }
                                                            }
                                                            ComposeResult.ok
                                                        }
                                                    }.Compose(composectx)

                                                    Text(marketPlayer.fullTeamName)
                                                        .font(Font.caption)
                                                        .foregroundColor(Color.secondary).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            Spacer().Compose(composectx)

                                            // Badge: Markt
                                            Text(LocalizedStringKey(stringLiteral = "Markt"))
                                                .font(Font.caption2)
                                                .fontWeight(Font.Weight.bold)
                                                .foregroundColor(Color.white)
                                                .padding(Edge.Set.horizontal, 8.0)
                                                .padding(Edge.Set.vertical, 4.0)
                                                .background(Color.blue)
                                                .cornerRadius(4.0).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)

                                    HStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendLiteral("€")
                                                str.appendInterpolation(marketPlayer.price / 1_000_000)
                                                str.appendLiteral("M")
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.caption)
                                                .foregroundColor(Color.green).Compose(composectx)

                                            Spacer().Compose(composectx)

                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendInterpolation(marketPlayer.averagePoints)
                                                str.appendLiteral(" Ø")
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .font(Font.caption)
                                                .foregroundColor(Color.secondary).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                        } else {
                            val matchtarget_2 = slot.ownedPlayerId
                            if (matchtarget_2 != null) {
                                val ownId = matchtarget_2
                                val matchtarget_3 = teamPlayers.first(where = { it -> it.id == ownId })
                                if (matchtarget_3 != null) {
                                    val ownPlayer = matchtarget_3
                                    // Eigener Spieler
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            HStack { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            HStack(spacing = 4.0) { ->
                                                                ComposeBuilder { composectx: ComposeContext ->
                                                                    Text(ownPlayer.fullName)
                                                                        .font(Font.subheadline)
                                                                        .fontWeight(Font.Weight.semibold).Compose(composectx)

                                                                    // Ligainsider Status Icon
                                                                    if (!ligainsiderService.matches.isEmpty) {
                                                                        val status = ligainsiderService.getPlayerStatus(firstName = ownPlayer.firstName, lastName = ownPlayer.lastName)
                                                                        if (status != LigainsiderStatus.out) {
                                                                            Image(systemName = ligainsiderService.getIcon(for_ = status))
                                                                                .foregroundColor(ligainsiderService.getColor(for_ = status))
                                                                                .font(Font.caption2).Compose(composectx)
                                                                        }
                                                                    }
                                                                    ComposeResult.ok
                                                                }
                                                            }.Compose(composectx)

                                                            Text(ownPlayer.fullTeamName)
                                                                .font(Font.caption)
                                                                .foregroundColor(Color.secondary).Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }.Compose(composectx)

                                                    Spacer().Compose(composectx)

                                                    // Badge: Team
                                                    Text(LocalizedStringKey(stringLiteral = "Team"))
                                                        .font(Font.caption2)
                                                        .fontWeight(Font.Weight.bold)
                                                        .foregroundColor(Color.white)
                                                        .padding(Edge.Set.horizontal, 8.0)
                                                        .padding(Edge.Set.vertical, 4.0)
                                                        .background(Color.green)
                                                        .cornerRadius(4.0).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)

                                            HStack { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    Text({
                                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                        str.appendLiteral("€")
                                                        str.appendInterpolation(ownPlayer.marketValue / 1_000_000)
                                                        str.appendLiteral("M")
                                                        LocalizedStringKey(stringInterpolation = str)
                                                    }())
                                                        .font(Font.caption)
                                                        .foregroundColor(Color.secondary).Compose(composectx)

                                                    Spacer().Compose(composectx)

                                                    Text({
                                                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                        str.appendInterpolation(ownPlayer.averagePoints)
                                                        str.appendLiteral(" Ø")
                                                        LocalizedStringKey(stringInterpolation = str)
                                                    }())
                                                        .font(Font.caption)
                                                        .foregroundColor(Color.secondary).Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                } else {
                                    VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Keine Empfehlung"))
                                                .font(Font.subheadline)
                                                .fontWeight(Font.Weight.semibold)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            Text(LocalizedStringKey(stringLiteral = "Nicht genug Spieler für diese Position"))
                                                .font(Font.caption)
                                                .foregroundColor(Color.secondary).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                }
                            } else {
                                VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Text(LocalizedStringKey(stringLiteral = "Keine Empfehlung"))
                                            .font(Font.subheadline)
                                            .fontWeight(Font.Weight.semibold)
                                            .foregroundColor(Color.secondary).Compose(composectx)

                                        Text(LocalizedStringKey(stringLiteral = "Nicht genug Spieler für diese Position"))
                                            .font(Font.caption)
                                            .foregroundColor(Color.secondary).Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)
                            }
                        }
                    } else {
                        val matchtarget_2 = slot.ownedPlayerId
                        if (matchtarget_2 != null) {
                            val ownId = matchtarget_2
                            val matchtarget_3 = teamPlayers.first(where = { it -> it.id == ownId })
                            if (matchtarget_3 != null) {
                                val ownPlayer = matchtarget_3
                                // Eigener Spieler
                                VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        HStack { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                                    ComposeBuilder { composectx: ComposeContext ->
                                                        HStack(spacing = 4.0) { ->
                                                            ComposeBuilder { composectx: ComposeContext ->
                                                                Text(ownPlayer.fullName)
                                                                    .font(Font.subheadline)
                                                                    .fontWeight(Font.Weight.semibold).Compose(composectx)

                                                                // Ligainsider Status Icon
                                                                if (!ligainsiderService.matches.isEmpty) {
                                                                    val status = ligainsiderService.getPlayerStatus(firstName = ownPlayer.firstName, lastName = ownPlayer.lastName)
                                                                    if (status != LigainsiderStatus.out) {
                                                                        Image(systemName = ligainsiderService.getIcon(for_ = status))
                                                                            .foregroundColor(ligainsiderService.getColor(for_ = status))
                                                                            .font(Font.caption2).Compose(composectx)
                                                                    }
                                                                }
                                                                ComposeResult.ok
                                                            }
                                                        }.Compose(composectx)

                                                        Text(ownPlayer.fullTeamName)
                                                            .font(Font.caption)
                                                            .foregroundColor(Color.secondary).Compose(composectx)
                                                        ComposeResult.ok
                                                    }
                                                }.Compose(composectx)

                                                Spacer().Compose(composectx)

                                                // Badge: Team
                                                Text(LocalizedStringKey(stringLiteral = "Team"))
                                                    .font(Font.caption2)
                                                    .fontWeight(Font.Weight.bold)
                                                    .foregroundColor(Color.white)
                                                    .padding(Edge.Set.horizontal, 8.0)
                                                    .padding(Edge.Set.vertical, 4.0)
                                                    .background(Color.green)
                                                    .cornerRadius(4.0).Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)

                                        HStack { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                Text({
                                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                    str.appendLiteral("€")
                                                    str.appendInterpolation(ownPlayer.marketValue / 1_000_000)
                                                    str.appendLiteral("M")
                                                    LocalizedStringKey(stringInterpolation = str)
                                                }())
                                                    .font(Font.caption)
                                                    .foregroundColor(Color.secondary).Compose(composectx)

                                                Spacer().Compose(composectx)

                                                Text({
                                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                    str.appendInterpolation(ownPlayer.averagePoints)
                                                    str.appendLiteral(" Ø")
                                                    LocalizedStringKey(stringInterpolation = str)
                                                }())
                                                    .font(Font.caption)
                                                    .foregroundColor(Color.secondary).Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)
                            } else {
                                VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Text(LocalizedStringKey(stringLiteral = "Keine Empfehlung"))
                                            .font(Font.subheadline)
                                            .fontWeight(Font.Weight.semibold)
                                            .foregroundColor(Color.secondary).Compose(composectx)

                                        Text(LocalizedStringKey(stringLiteral = "Nicht genug Spieler für diese Position"))
                                            .font(Font.caption)
                                            .foregroundColor(Color.secondary).Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)
                            }
                        } else {
                            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(LocalizedStringKey(stringLiteral = "Keine Empfehlung"))
                                        .font(Font.subheadline)
                                        .fontWeight(Font.Weight.semibold)
                                        .foregroundColor(Color.secondary).Compose(composectx)

                                    Text(LocalizedStringKey(stringLiteral = "Nicht genug Spieler für diese Position"))
                                        .font(Font.caption)
                                        .foregroundColor(Color.secondary).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                        }
                    }

                    Spacer().Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding(Edge.Set.vertical, 8.0)
            .padding(Edge.Set.horizontal, 10.0)
            .background(Color.systemBackgroundCompat)
            .cornerRadius(8.0)
            .overlay { ->
                ComposeBuilder { composectx: ComposeContext ->
                    ZStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            RoundedRectangle(cornerRadius = 8.0)
                                .strokeBorder(if (slot.hasBetterMarketOption) Color.blue.opacity(0.3) else Color.systemGray5Compat, lineWidth = 1.0).Compose(composectx)
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

    private fun scoreColor(score: Double): Color {
        if (score >= 16) {
            return Color.green
        } else if (score >= 12) {
            return Color.blue
        } else if (score >= 8) {
            return Color.orange
        } else {
            return Color.red
        }
    }

    constructor(slot: LineupSlot, teamPlayers: Array<Player>, marketPlayers: Array<MarketPlayer>) {
        this.slot = slot
        this.teamPlayers = teamPlayers.sref()
        this.marketPlayers = marketPlayers.sref()
    }
}

internal class HybridLineupSummary: View {
    internal val comparison: LineupComparison
    internal val teamPlayers: Array<Player>
    internal val marketPlayers: Array<MarketPlayer>

    override fun body(): View {
        return ComposeBuilder l@{ composectx: ComposeContext ->
            val hybridLineup_0 = comparison.hybridLineup
            if (hybridLineup_0 == null) {
                return@l AnyView(EmptyView()).Compose(composectx)
            }

            return@l AnyView(VStack(spacing = 16.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(LocalizedStringKey(stringLiteral = "Hybrid-Aufstellung Übersicht"))
                        .font(Font.headline)
                        .frame(maxWidth = Double.infinity, alignment = Alignment.leading).Compose(composectx)

                    // Verbesserungen
                    VStack(spacing = 12.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            LineupInfoRow(label = "Leistungsverbesserung", value = String(format = "+%.1f Punkte/Spiel", comparison.performanceGainWithHybrid), valueColor = Color.green).Compose(composectx)

                            LineupInfoRow(label = "Benötigte Investition", value = "€${comparison.totalInvestmentNeeded / 1_000_000}M", valueColor = Color.blue).Compose(composectx)

                            LineupInfoRow(label = "Markt-Spieler zum kaufen", value = "${hybridLineup_0.marketPlayerCount} Spieler", valueColor = Color.orange).Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .padding()
                    .background(Color.systemGray6Compat)
                    .cornerRadius(8.0).Compose(composectx)

                    // Liste der empfohlenen Markt-Spieler
                    if (!hybridLineup_0.marketPlayersNeeded.isEmpty) {
                        VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                Text(LocalizedStringKey(stringLiteral = "Zu kaufende Spieler"))
                                    .font(Font.subheadline)
                                    .fontWeight(Font.Weight.semibold).Compose(composectx)

                                VStack(spacing = 6.0) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        ForEach(hybridLineup_0.marketPlayersNeeded, id = { it }) { playerId ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                marketPlayers.first(where = { it -> it.id == playerId })?.let { player ->
                                                    HStack { ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) { ->
                                                                ComposeBuilder { composectx: ComposeContext ->
                                                                    Text(player.fullName)
                                                                        .font(Font.subheadline)
                                                                        .fontWeight(Font.Weight.semibold).Compose(composectx)

                                                                    Text(player.fullTeamName)
                                                                        .font(Font.caption)
                                                                        .foregroundColor(Color.secondary).Compose(composectx)
                                                                    ComposeResult.ok
                                                                }
                                                            }.Compose(composectx)

                                                            Spacer().Compose(composectx)

                                                            Text({
                                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                                str.appendLiteral("€")
                                                                str.appendInterpolation(player.price / 1_000_000)
                                                                str.appendLiteral("M")
                                                                LocalizedStringKey(stringInterpolation = str)
                                                            }())
                                                                .font(Font.subheadline)
                                                                .fontWeight(Font.Weight.semibold)
                                                                .foregroundColor(Color.green).Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }
                                                    .padding(8.0)
                                                    .background(Color.systemBackgroundCompat)
                                                    .cornerRadius(6.0).Compose(composectx)
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
                    }

                    // Empfehlung-Button
                    VStack(spacing = 8.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(LocalizedStringKey(stringLiteral = "Diese Aufstellung bietet eine bessere Gesamtleistung durch strategische Marktzukäufe."))
                                .font(Font.caption)
                                .foregroundColor(Color.secondary)
                                .lineLimit(3).Compose(composectx)

                            HStack(spacing = 12.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Image(systemName = "lightbulb.fill")
                                        .foregroundColor(Color.yellow).Compose(composectx)

                                    Text(LocalizedStringKey(stringLiteral = "Gesamtbudget nach Verkäufen prüfen"))
                                        .font(Font.caption)
                                        .fontWeight(Font.Weight.semibold).Compose(composectx)

                                    Spacer().Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .padding(10.0)
                            .background(Color.yellow.opacity(0.1))
                            .cornerRadius(6.0).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding()
            .background(Color.systemGray6Compat)
            .cornerRadius(12.0)).Compose(composectx)
            ComposeResult.ok
        }
    }

    constructor(comparison: LineupComparison, teamPlayers: Array<Player>, marketPlayers: Array<MarketPlayer>) {
        this.comparison = comparison
        this.teamPlayers = teamPlayers.sref()
        this.marketPlayers = marketPlayers.sref()
    }
}

internal class LineupInfoRow: View {
    internal val label: String
    internal val value: String
    internal val valueColor: Color

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            HStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Text(label)
                        .font(Font.subheadline)
                        .foregroundColor(Color.secondary).Compose(composectx)

                    Spacer().Compose(composectx)

                    Text(value)
                        .font(Font.subheadline)
                        .fontWeight(Font.Weight.semibold)
                        .foregroundColor(valueColor).Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    constructor(label: String, value: String, valueColor: Color) {
        this.label = label
        this.value = value
        this.valueColor = valueColor
    }
}

// #Preview omitted
