package kickbase.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import skip.lib.*

import skip.ui.*
import skip.foundation.*
import skip.model.*

internal class PlayerMatchDetailView: View {
    internal val player: LivePlayer // From LiveModels.swift
    internal val league: League // From Models.swift
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
    internal lateinit var dismiss: DismissAction

    private var details: PlayerMatchDetailResponse?
        get() = _details.wrappedValue
        set(newValue) {
            _details.wrappedValue = newValue
        }
    private var _details: skip.ui.State<PlayerMatchDetailResponse?> = skip.ui.State(null)
    private var isLoading: Boolean
        get() = _isLoading.wrappedValue
        set(newValue) {
            _isLoading.wrappedValue = newValue
        }
    private var _isLoading: skip.ui.State<Boolean>
    private var error: String?
        get() = _error.wrappedValue
        set(newValue) {
            _error.wrappedValue = newValue
        }
    private var _error: skip.ui.State<String?> = skip.ui.State(null)

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
            NavigationStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    Group { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            if (isLoading) {
                                ProgressView(LocalizedStringKey(stringLiteral = "Lade Details...")).Compose(composectx)
                            } else {
                                val matchtarget_0 = error
                                if (matchtarget_0 != null) {
                                    val error = matchtarget_0
                                    VStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Image(systemName = "exclamationmark.triangle")
                                                .font(Font.largeTitle)
                                                .foregroundColor(Color.orange).Compose(composectx)
                                            Text(error)
                                                .multilineTextAlignment(TextAlignment.center)
                                                .padding().Compose(composectx)
                                            Button(LocalizedStringKey(stringLiteral = "Erneut versuchen")) { -> loadDetails() }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }.Compose(composectx)
                                } else {
                                    val matchtarget_1 = details
                                    if (matchtarget_1 != null) {
                                        val details = matchtarget_1
                                        List { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                // Header
                                                Section { ->
                                                    ComposeBuilder { composectx: ComposeContext ->
                                                        HStack { ->
                                                            ComposeBuilder { composectx: ComposeContext ->
                                                                AsyncImage(url = photoUrl, content = { image ->
                                                                    ComposeBuilder { composectx: ComposeContext ->
                                                                        image.resizable().scaledToFit().Compose(composectx)
                                                                        ComposeResult.ok
                                                                    }
                                                                }, placeholder = { ->
                                                                    ComposeBuilder { composectx: ComposeContext ->
                                                                        Image(systemName = "person.fill")
                                                                            .resizable().scaledToFit()
                                                                            .foregroundColor(Color.gray).Compose(composectx)
                                                                        ComposeResult.ok
                                                                    }
                                                                })
                                                                .frame(width = 80.0, height = 80.0)
                                                                .clipShape(Circle()).Compose(composectx)

                                                                VStack(alignment = HorizontalAlignment.leading, spacing = 5.0) { ->
                                                                    ComposeBuilder { composectx: ComposeContext ->
                                                                        Text(player.name)
                                                                            .font(Font.title2)
                                                                            .bold().Compose(composectx)
                                                                        Text({
                                                                            val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                                            str.appendLiteral("Punkte: ")
                                                                            str.appendInterpolation(player.p)
                                                                            LocalizedStringKey(stringInterpolation = str)
                                                                        }())
                                                                            .font(Font.headline)
                                                                            .foregroundColor(if (player.p >= 0) Color.green else Color.red).Compose(composectx)
                                                                        ComposeResult.ok
                                                                    }
                                                                }.Compose(composectx)
                                                                ComposeResult.ok
                                                            }
                                                        }
                                                        .padding(Edge.Set.vertical).Compose(composectx)
                                                        ComposeResult.ok
                                                    }
                                                }.Compose(composectx)

                                                // Events
                                                Section(LocalizedStringKey(stringLiteral = "Ereignisse")) { ->
                                                    ComposeBuilder { composectx: ComposeContext ->
                                                        if (details.events.isEmpty) {
                                                            Text(LocalizedStringKey(stringLiteral = "Keine Ereignisse vorhanden"))
                                                                .foregroundColor(Color.secondary).Compose(composectx)
                                                        } else {
                                                            ForEach(details.events.sorted { it, it_1 -> (it.minute ?: 0) > (it_1.minute ?: 0) }) { event ->
                                                                ComposeBuilder { composectx: ComposeContext ->
                                                                    resolveEventName(event)?.let { eventName ->
                                                                        HStack { ->
                                                                            ComposeBuilder { composectx: ComposeContext ->
                                                                                Text({
                                                                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                                                    str.appendInterpolation(event.minute ?: 0)
                                                                                    str.appendLiteral("'")
                                                                                    LocalizedStringKey(stringInterpolation = str)
                                                                                }())
                                                                                    .frame(width = 35.0, alignment = Alignment.trailing)
                                                                                    .foregroundColor(Color.secondary).Compose(composectx)

                                                                                Text(event.icon).Compose(composectx)

                                                                                Text(eventName).Compose(composectx)

                                                                                Spacer().Compose(composectx)

                                                                                Text({
                                                                                    val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                                                    str.appendInterpolation(event.points ?: 0)
                                                                                    LocalizedStringKey(stringInterpolation = str)
                                                                                }())
                                                                                    .bold()
                                                                                    .foregroundColor(if ((event.points ?: 0) >= 0) Color.green else Color.red).Compose(composectx)
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
                                        }.Compose(composectx)
                                    } else {
                                        Text(LocalizedStringKey(stringLiteral = "Keine Daten geladen")).Compose(composectx)
                                    }
                                }
                            }
                            ComposeResult.ok
                        }
                    }
                    .navigationTitle(LocalizedStringKey(stringLiteral = "Match Details"))
                    .toolbar { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            ToolbarItem(placement = ToolbarItemPlacement.confirmationAction) { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    Button(LocalizedStringKey(stringLiteral = "Fertig")) { -> dismiss() }.Compose(composectx)
                                    ComposeResult.ok
                                }
                            }.Compose(composectx)
                            ComposeResult.ok
                        }
                    }
                    .task { -> MainActor.run {
                        if (kickbaseManager.eventTypeNames.isEmpty) {
                            kickbaseManager.loadEventDefinitions()
                        }
                        loadDetails()
                    } }.Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val remembereddetails by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<PlayerMatchDetailResponse?>, Any>) { mutableStateOf(_details) }
        _details = remembereddetails

        val rememberedisLoading by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<Boolean>, Any>) { mutableStateOf(_isLoading) }
        _isLoading = rememberedisLoading

        val rememberederror by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<String?>, Any>) { mutableStateOf(_error) }
        _error = rememberederror

        _ligainsiderService.wrappedValue = EnvironmentValues.shared.environmentObject(type = LigainsiderService::class)!!
        this.dismiss = EnvironmentValues.shared.dismiss

        return super.Evaluate(context, options)
    }

    private fun loadDetails() {
        isLoading = true
        error = null
        Task { ->
            try {
                details = kickbaseManager.loadPlayerMatchDetails(leagueId = league.id, competitionId = league.competitionId, playerId = player.id, dayNumber = league.matchDay)
            } catch (error: Throwable) {
                @Suppress("NAME_SHADOWING") val error = error.aserror()
                print("Error loading details: ${error}")
                this.error = "Details konnten nicht geladen werden.\n${error.localizedDescription}"
            }
            isLoading = false
        }
    }

    private fun resolveEventName(event: PlayerMatchEvent): String? {
        event.name?.let { name ->
            return name
        }
        val type_0 = event.type
        if (type_0 == null) {
            return null
        }
        return kickbaseManager.eventTypeNames[type_0]
    }

    private constructor(player: LivePlayer, league: League, kickbaseManager: KickbaseManager, details: PlayerMatchDetailResponse? = null, isLoading: Boolean = false, error: String? = null, privatep: Nothing? = null) {
        this.player = player
        this.league = league
        this._kickbaseManager = skip.ui.Bindable(kickbaseManager)
        this._details = skip.ui.State(details)
        this._isLoading = skip.ui.State(isLoading)
        this._error = skip.ui.State(error)
    }

    constructor(player: LivePlayer, league: League, kickbaseManager: KickbaseManager): this(player = player, league = league, kickbaseManager = kickbaseManager, privatep = null) {
    }
}
