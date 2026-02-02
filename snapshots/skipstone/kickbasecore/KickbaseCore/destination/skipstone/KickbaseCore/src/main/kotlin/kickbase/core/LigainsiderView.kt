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

internal class LigainsiderView: View {
    // Verwende jetzt den globalen Service
    internal var service: LigainsiderService
        get() = _service.wrappedValue
        set(newValue) {
            _service.wrappedValue = newValue
        }
    internal var _service = skip.ui.Environment<LigainsiderService>()

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            List { ->
                ComposeBuilder { composectx: ComposeContext ->
                    if (service.isLoading) {
                        ProgressView(LocalizedStringKey(stringLiteral = "Lade Aufstellungen...")).Compose(composectx)
                    } else {
                        val matchtarget_0 = service.errorMessage
                        if (matchtarget_0 != null) {
                            val error = matchtarget_0
                            Text(error)
                                .foregroundColor(Color.red).Compose(composectx)
                            Button(LocalizedStringKey(stringLiteral = "Erneut versuchen")) { -> service.fetchLineups() }.Compose(composectx)
                        } else if (service.matches.isEmpty) {
                            Text(LocalizedStringKey(stringLiteral = "Keine Aufstellungen gefunden. Überprüfe die Internetverbindung oder ziehe zum Aktualisieren."))
                                .multilineTextAlignment(TextAlignment.center)
                                .padding().Compose(composectx)
                            Button(LocalizedStringKey(stringLiteral = "Laden")) { -> service.fetchLineups() }.Compose(composectx)
                        } else {
                            ForEach(service.matches) { match ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    LigainsiderMatchRow(match = match).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                        }
                    }
                    ComposeResult.ok
                }
            }
            .navigationTitle(LocalizedStringKey(stringLiteral = "Voraussichtliche Aufstellungen"))
            .onAppear { ->
                if (service.matches.isEmpty) {
                    service.fetchLineups()
                }
            }
            .toolbar { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Button(action = { -> service.fetchLineups() }) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Image(systemName = "arrow.clockwise").Compose(composectx)
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
        _service.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }
}

// MARK: - Row View (List Item)

internal class LigainsiderMatchRow: View {
    internal val match: LigainsiderMatch
    private var isExpanded: Boolean
        get() = _isExpanded.wrappedValue
        set(newValue) {
            _isExpanded.wrappedValue = newValue
        }
    private var _isExpanded: skip.ui.State<Boolean>

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 0.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Header: Team vs Team
                    Button(action = { -> isExpanded = !isExpanded }) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            HStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    // Home Team
                                    HStack(spacing = 8.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            match.homeLogo?.let { logo ->
                                                (try { URL(string = logo) } catch (_: NullReturnException) { null })?.let { url ->
                                                    Image(systemName = "shield")
                                                        .resizable()
                                                        .scaledToFit()
                                                        .frame(width = 30.0, height = 30.0)
                                                        .foregroundColor(Color.gray).Compose(composectx)
                                                }
                                            }
                                            Text(match.homeTeam)
                                                .font(Font.headline).Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }
                                    .frame(maxWidth = Double.infinity, alignment = Alignment.leading).Compose(composectx)

                                    Text(LocalizedStringKey(stringLiteral = "vs"))
                                        .foregroundColor(Color.gray)
                                        .font(Font.caption).Compose(composectx)

                                    // Away Team
                                    HStack(spacing = 8.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(match.awayTeam)
                                                .font(Font.headline)
                                                .multilineTextAlignment(TextAlignment.trailing).Compose(composectx)

                                            match.awayLogo?.let { logo ->
                                                (try { URL(string = logo) } catch (_: NullReturnException) { null })?.let { url ->
                                                    Image(systemName = "shield")
                                                        .resizable()
                                                        .scaledToFit()
                                                        .frame(width = 30.0, height = 30.0)
                                                        .foregroundColor(Color.gray).Compose(composectx)
                                                }
                                            }
                                            ComposeResult.ok
                                        }
                                    }
                                    .frame(maxWidth = Double.infinity, alignment = Alignment.trailing).Compose(composectx)

                                    Image(systemName = if (isExpanded) "chevron.up" else "chevron.down")
                                        .foregroundColor(Color.gray).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .padding(Edge.Set.vertical, 8.0).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    if (isExpanded) {
                        Divider().Compose(composectx)
                        // Pitch Views für beide Teams
                        VStack(spacing = 20.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                // Heim
                                VStack(alignment = HorizontalAlignment.leading) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Text({
                                            val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                            str.appendLiteral("Heim: ")
                                            str.appendInterpolation(match.homeTeam)
                                            LocalizedStringKey(stringInterpolation = str)
                                        }())
                                            .font(Font.subheadline).bold()
                                            .padding(Edge.Set.top, 8.0).Compose(composectx)
                                        PitchView(rows = match.homeLineup).Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)

                                Divider().Compose(composectx)

                                // Gast
                                VStack(alignment = HorizontalAlignment.leading) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Text({
                                            val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                            str.appendLiteral("Gast: ")
                                            str.appendInterpolation(match.awayTeam)
                                            LocalizedStringKey(stringInterpolation = str)
                                        }())
                                            .font(Font.subheadline).bold().Compose(composectx)
                                        PitchView(rows = match.awayLineup).Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)
                                ComposeResult.ok
                            }
                        }
                        .padding(Edge.Set.vertical).Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedisExpanded by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_isExpanded) }
        _isExpanded = rememberedisExpanded

        return super.Evaluate(context, options)
    }

    private constructor(match: LigainsiderMatch, isExpanded: Boolean = false, privatep: Nothing? = null) {
        this.match = match
        this._isExpanded = skip.ui.State(isExpanded)
    }

    constructor(match: LigainsiderMatch): this(match = match, privatep = null) {
    }
}

// MARK: - Pitch View (Spielfeld Darstellung)

internal class PitchView: View {
    internal val rows: Array<LineupRow>

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            ZStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Background (Spielfeldrasen Optik)
                    ZStack { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            LinearGradient(gradient = Gradient(colors = arrayOf(
                                Color.green.opacity(0.8),
                                Color.green.opacity(0.6)
                            )), startPoint = UnitPoint.top, endPoint = UnitPoint.bottom).Compose(composectx)

                            // Spielfeld Linien Andeutung
                            VStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Divider().background(Color.white.opacity(0.5)).Compose(composectx)
                                    Spacer().Compose(composectx)
                                    Circle().stroke(Color.white.opacity(0.3), lineWidth = 2.0)
                                        .frame(width = 80.0, height = 80.0).Compose(composectx)
                                    Spacer().Compose(composectx)
                                    Divider().background(Color.white.opacity(0.5)).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }
                            .padding().Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .cornerRadius(12.0).Compose(composectx)

                    // Spieler Positionen
                    VStack(spacing = 12.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Wir gehen durch die Reihen (GK bis ST)
                            // Ligainsider gibt oft GK zuerst. Row1=GK.
                            ForEach(rows) { row ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    HStack(spacing = 10.0) { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            ForEach(row.players) { player ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    PlayerPillView(player = player).Compose(composectx)
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
                    .padding(Edge.Set.vertical, 20.0).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .frame(minHeight = 250.0).Compose(composectx)
        }
    }

    constructor(rows: Array<LineupRow>) {
        this.rows = rows.sref()
    }
}

// MARK: - Player Pill (Einzelner Spieler Marker)

internal class PlayerPillView: View {
    internal val player: LigainsiderPlayer
    internal var service: LigainsiderService
        get() = _service.wrappedValue
        set(newValue) {
            _service.wrappedValue = newValue
        }
    internal var _service = skip.ui.Environment<LigainsiderService>()

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            VStack(spacing = 4.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Spieler Foto oder Icon / Kreis
                    val matchtarget_1 = player.imageUrl
                    if (matchtarget_1 != null) {
                        val imgString = matchtarget_1
                        val matchtarget_2 = (try { URL(string = imgString) } catch (_: NullReturnException) { null })
                        if (matchtarget_2 != null) {
                            val url = matchtarget_2
                            // Show player photo
                            // Fallback for non-Apple platforms
                            ZStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Circle()
                                        .fill(Color.white)
                                        .frame(width = 30.0, height = 30.0).Compose(composectx)

                                    Text(String(player.name.prefix(1)))
                                        .font(Font.caption).bold()
                                        .foregroundColor(Color.black).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                        } else {
                            // No photo available - show initial letter
                            ZStack { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Circle()
                                        .fill(Color.white)
                                        .frame(width = 30.0, height = 30.0).Compose(composectx)

                                    Text(String(player.name.prefix(1)))
                                        .font(Font.caption).bold()
                                        .foregroundColor(Color.black).Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                        }
                    } else {
                        // No photo available - show initial letter
                        ZStack { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                Circle()
                                    .fill(Color.white)
                                    .frame(width = 30.0, height = 30.0).Compose(composectx)

                                Text(String(player.name.prefix(1)))
                                    .font(Font.caption).bold()
                                    .foregroundColor(Color.black).Compose(composectx)
                                ComposeResult.ok
                            }
                        }.Compose(composectx)
                    }

                    // Name
                    Text(player.name)
                        .font(Font.system(size = 10.0, weight = Font.Weight.semibold))
                        .foregroundColor(Color.white)
                        .lineLimit(1)
                        .truncationMode(Text.TruncationMode.tail)
                        .padding(Edge.Set.horizontal, 4.0)
                        .background(Color.black.opacity(0.4))
                        .cornerRadius(4.0).Compose(composectx)

                    // Alternative anzeigen via Icon (1. Option / 2. Option) falls möglich
                    // Da wir hier LigainsiderPlayer haben, kennen wir den Status via 'alternative' property
                    // Wenn player.alternative != nil -> Er ist S11, aber hat Alternative (1. Option)
                    // Wenn er eine Alternative IST -> Das wissen wir hier im Pill View isoliert nicht sicher,
                    // (außer wir checken ob er als Alternative im Match gelistet war - aber PillView kriegt nur Player).
                    // ABER: LigainsiderStatus Logik in Service nutzt Cache.
                    // Checken wir den Status via Service? PitchView hat keinen Service access direkt, aber LigainsiderView hat environment.
                    // Wir fügen EnvironmentObject zu PitchView/PillView hinzu?

                    player.alternative?.let { alt ->
                        HStack(spacing = 2.0) { ->
                            ComposeBuilder { composectx: ComposeContext ->
                                Image(systemName = "1.circle.fill")
                                    .font(Font.system(size = 8.0))
                                    .foregroundColor(Color.orange).Compose(composectx)
                                Text(alt)
                                    .font(Font.system(size = 8.0))
                                    .lineLimit(1).Compose(composectx)
                                ComposeResult.ok
                            }
                        }
                        .foregroundColor(Color.white)
                        .padding(2.0)
                        .background(Color.black.opacity(0.5))
                        .cornerRadius(4.0).Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }
            .frame(minWidth = 60.0).Compose(composectx)
        }
    }

    @Composable
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        _service.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    constructor(player: LigainsiderPlayer) {
        this.player = player
    }
}

// #Preview omitted
