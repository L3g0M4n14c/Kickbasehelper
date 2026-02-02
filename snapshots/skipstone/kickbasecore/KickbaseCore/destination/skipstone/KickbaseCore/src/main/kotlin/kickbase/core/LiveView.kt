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

internal class LiveView: View {
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager: skip.ui.Bindable<KickbaseManager>
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
    private var selectedPlayer: LivePlayer?
        get() = _selectedPlayer.wrappedValue
        set(newValue) {
            _selectedPlayer.wrappedValue = newValue
        }
    private var _selectedPlayer: skip.ui.State<LivePlayer?> = skip.ui.State(null)

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            NavigationStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Group { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            if (kickbaseManager.isLoading) {
                                ProgressView(LocalizedStringKey(stringLiteral = "Lade Live-Daten...")).Compose(composectx)
                            } else if (kickbaseManager.livePlayers.isEmpty) {
                                VStack { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Image(systemName = "sportscourt")
                                            .font(Font.system(size = 60.0))
                                            .foregroundColor(Color.gray).Compose(composectx)
                                        Text(LocalizedStringKey(stringLiteral = "Keine Live-Daten verfügbar"))
                                            .font(Font.headline)
                                            .padding(Edge.Set.top).Compose(composectx)
                                        kickbaseManager.errorMessage?.let { error ->
                                            Text({
                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                str.appendLiteral("Fehler: ")
                                                str.appendInterpolation(error)
                                                LocalizedStringKey(stringInterpolation = str)
                                            }())
                                                .foregroundColor(Color.red)
                                                .multilineTextAlignment(TextAlignment.center)
                                                .padding().Compose(composectx)
                                        }

                                        Text(LocalizedStringKey(stringLiteral = "Möglicherweise läuft gerade kein Spieltag oder die Aufstellung ist leer."))
                                            .font(Font.subheadline)
                                            .foregroundColor(Color.gray)
                                            .multilineTextAlignment(TextAlignment.center)
                                            .padding().Compose(composectx)
                                        Button(LocalizedStringKey(stringLiteral = "Aktualisieren")) { ->
                                            Task(isMainActor = true) { -> kickbaseManager.loadLivePoints() }
                                        }.Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }.Compose(composectx)
                            } else {
                                List { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        // Summary Section
                                        Section { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                HStack { ->
                                                    ComposeBuilder { composectx: ComposeContext ->
                                                        VStack(alignment = HorizontalAlignment.leading) { ->
                                                            ComposeBuilder { composectx: ComposeContext ->
                                                                Text(LocalizedStringKey(stringLiteral = "Gesamtpunkte"))
                                                                    .font(Font.caption)
                                                                    .foregroundColor(Color.secondary).Compose(composectx)
                                                                Text({
                                                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                                    str.appendInterpolation(calculateTotalPoints())
                                                                    LocalizedStringKey(stringInterpolation = str)
                                                                }())
                                                                    .font(Font.system(size = 34.0, weight = Font.Weight.bold))
                                                                    .foregroundColor(if (calculateTotalPoints() >= 0) Color.green else Color.red).Compose(composectx)
                                                                ComposeResult.ok
                                                            }
                                                        }.Compose(composectx)
                                                        Spacer().Compose(composectx)
                                                        VStack(alignment = HorizontalAlignment.trailing) { ->
                                                            ComposeBuilder { composectx: ComposeContext ->
                                                                Text(LocalizedStringKey(stringLiteral = "Spieler im Einsatz"))
                                                                    .font(Font.caption)
                                                                    .foregroundColor(Color.secondary).Compose(composectx)
                                                                Text({
                                                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                                    str.appendInterpolation(kickbaseManager.livePlayers.count)
                                                                    LocalizedStringKey(stringInterpolation = str)
                                                                }())
                                                                    .font(Font.title2)
                                                                    .fontWeight(Font.Weight.semibold).Compose(composectx)
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

                                        // Players List
                                        Section(header = Text(LocalizedStringKey(stringLiteral = "Meine Aufstellung"))) { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                ForEach(sortedPlayers) { player ->
                                                    ComposeBuilder { composectx: ComposeContext ->
                                                        Button(action = { -> selectedPlayer = player }, label = { ->
                                                            ComposeBuilder { composectx: ComposeContext ->
                                                                LivePlayerRow(player = player).Compose(composectx)
                                                                ComposeResult.ok
                                                            }
                                                        })
                                                        .buttonStyle(ButtonStyle.plain).Compose(composectx)
                                                        ComposeResult.ok
                                                    }
                                                }.Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }
                                .refreshable { -> MainActor.run { kickbaseManager.loadLivePoints() } }.Compose(composectx)
                            }
                            ComposeResult.ok
                        }
                    }
                    .sheet(item = Binding({ _selectedPlayer.wrappedValue }, { it -> _selectedPlayer.wrappedValue = it })) { player ->
                        ComposeBuilder { composectx: ComposeContext ->
                            linvokeComposable l@{
                                val matchtarget_0 = kickbaseManager.selectedLeague
                                if (matchtarget_0 != null) {
                                    val league = matchtarget_0
                                    return@l PlayerMatchDetailView(player = player, league = league, kickbaseManager = kickbaseManager).Compose(composectx)
                                } else {
                                    return@l Text(LocalizedStringKey(stringLiteral = "Keine Liga ausgewählt")).Compose(composectx)
                                }
                            }
                            ComposeResult.ok
                        }
                    }
                    .task(id = kickbaseManager.selectedLeague?.id) { ->
                        if (kickbaseManager.selectedLeague != null) {
                            kickbaseManager.loadLivePoints()
                        }
                    }
                    .onAppear { ->
                        if (kickbaseManager.selectedLeague != null) {
                            Task(isMainActor = true) { -> kickbaseManager.loadLivePoints() }
                        }
                    }
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedselectedPlayer by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<LivePlayer?>, Any>) { mutableStateOf(_selectedPlayer) }
        _selectedPlayer = rememberedselectedPlayer

        _authManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = AuthenticationManager::class)!!
        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!

        return super.Evaluate(context, options)
    }

    // Sort players by position (GK -> DEF -> MID -> FWD) then points
    private val sortedPlayers: Array<LivePlayer>
        get() {
            return kickbaseManager.livePlayers.sorted l@{ it, it_1 ->
                if (it.position == it_1.position) {
                    return@l it.p > it_1.p
                }
                return@l it.position < it_1.position
            }
        }

    private fun calculateTotalPoints(): Int {
        return kickbaseManager.livePlayers.reduce(initialResult = 0) { it, it_1 -> it + it_1.p }
    }

    private constructor(kickbaseManager: KickbaseManager, selectedPlayer: LivePlayer? = null, privatep: Nothing? = null) {
        this._kickbaseManager = skip.ui.Bindable(kickbaseManager)
        this._selectedPlayer = skip.ui.State(selectedPlayer)
    }

    constructor(kickbaseManager: KickbaseManager): this(kickbaseManager = kickbaseManager, privatep = null) {
    }
}

internal class LivePlayerRow: View {
    internal val player: LivePlayer
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
            HStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Player Image or Placeholder
                    AsyncImage(url = photoUrl, content = { image ->
                        ComposeBuilder { composectx: ComposeContext ->
                            image.resizable().Compose(composectx)
                            ComposeResult.ok
                        }
                    }, placeholder = { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Image(systemName = "person.fill")
                                .resizable()
                                .padding(8.0)
                                .foregroundColor(Color.gray).background(Color.gray.opacity(0.2)).Compose(composectx)
                            ComposeResult.ok
                        }
                    })
                    .frame(width = 40.0, height = 40.0)
                    .clipShape(Circle()).Compose(composectx)

                    VStack(alignment = HorizontalAlignment.leading) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            HStack(spacing = 4.0) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Text(player.name)
                                        .font(Font.headline).Compose(composectx)

                                    if (!player.eventIcons.isEmpty) {
                                        Text(player.eventIcons)
                                            .font(Font.subheadline).Compose(composectx)
                                    }
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)

                            Text(positionName(for_ = player.position))
                                .font(Font.caption)
                                .foregroundColor(Color.secondary).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    Spacer().Compose(composectx)

                    // Points Badge
                    Text({
                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                        str.appendInterpolation(player.p)
                        LocalizedStringKey(stringInterpolation = str)
                    }())
                        .font(Font.title3)
                        .fontWeight(Font.Weight.bold)
                        .foregroundColor(if (player.p >= 0) Color.green else Color.red)
                        .padding(Edge.Set.horizontal, 12.0)
                        .padding(Edge.Set.vertical, 6.0).background(Color.gray.opacity(0.2))
                        .cornerRadius(8.0).Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding(Edge.Set.vertical, 4.0).Compose(composectx)
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
            1 -> return "Torwart"
            2 -> return "Abwehr"
            3 -> return "Mittelfeld"
            4 -> return "Angriff"
            else -> return "Spieler"
        }
    }

    constructor(player: LivePlayer) {
        this.player = player
    }
}
