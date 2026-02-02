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

internal class LeagueTableView: View {
    internal var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    internal var _kickbaseManager = skip.ui.Environment<KickbaseManager>()
    internal var viewModel: LeagueTableViewModel
        get() = _viewModel.wrappedValue
        set(newValue) {
            _viewModel.wrappedValue = newValue
        }
    internal var _viewModel: skip.ui.State<LeagueTableViewModel>

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            NavigationStack { ->
                ComposeBuilder { composectx: ComposeContext ->
                    VStack(spacing = 0.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            // Segmented picker to switch between table types
                            if (viewModel.selectedLeague != null) {
                                Picker(LocalizedStringKey(stringLiteral = ""), selection = Binding({ _viewModel.wrappedValue.tableType }, { it -> _viewModel.wrappedValue.tableType = it })) { ->
                                    ComposeBuilder { composectx: ComposeContext ->
                                        Text(LocalizedStringKey(stringLiteral = "Gesamttabelle")).tag(LeagueTableViewModel.TableType.overall).Compose(composectx)
                                        Text(LocalizedStringKey(stringLiteral = "Spieltag")).tag(LeagueTableViewModel.TableType.matchday).Compose(composectx)
                                        ComposeResult.ok
                                    }
                                }
                                .pickerStyle(PickerStyle.segmented)
                                .padding(Edge.Set.horizontal)
                                .padding(Edge.Set.top, 8.0)
                                .onChange(of = viewModel.tableType) { oldValue, newValue ->
                                    Task(isMainActor = true) { -> viewModel.switchTableType(to = newValue) }
                                }.Compose(composectx)

                                // Matchday selector (shown only when matchday mode is selected)
                                if (viewModel.tableType == LeagueTableViewModel.TableType.matchday) {
                                    HStack { ->
                                        ComposeBuilder { composectx: ComposeContext ->
                                            Text(LocalizedStringKey(stringLiteral = "Spieltag auswählen:"))
                                                .font(Font.subheadline)
                                                .foregroundColor(Color.secondary).Compose(composectx)

                                            Picker(LocalizedStringKey(stringLiteral = "Spieltag"), selection = Binding({ _viewModel.wrappedValue.selectedMatchDay }, { it -> _viewModel.wrappedValue.selectedMatchDay = it })) { ->
                                                ComposeBuilder { composectx: ComposeContext ->
                                                    // Show all 34 matchdays for Bundesliga season
                                                    ForEach(1..34, id = { it }) { day ->
                                                        ComposeBuilder { composectx: ComposeContext ->
                                                            Text({
                                                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                                                str.appendLiteral("Spieltag ")
                                                                str.appendInterpolation(day)
                                                                LocalizedStringKey(stringInterpolation = str)
                                                            }()).tag(day).Compose(composectx)
                                                            ComposeResult.ok
                                                        }
                                                    }.Compose(composectx)
                                                    ComposeResult.ok
                                                }
                                            }
                                            .pickerStyle(PickerStyle.menu)
                                            .onChange(of = viewModel.selectedMatchDay) { oldValue, newValue ->
                                                Task(isMainActor = true) { -> viewModel.selectMatchDay(newValue) }
                                            }.Compose(composectx)
                                            ComposeResult.ok
                                        }
                                    }
                                    .padding(Edge.Set.horizontal)
                                    .padding(Edge.Set.bottom, 8.0).Compose(composectx)
                                } else {
                                    Spacer()
                                        .frame(height = 8.0).Compose(composectx)
                                }
                            }

                            Group { ->
                                ComposeBuilder { composectx: ComposeContext ->
                                    if (viewModel.isLoading) {
                                        ProgressView(LocalizedStringKey(stringLiteral = "Lade Tabelle...")).Compose(composectx)
                                    } else if (viewModel.displayedUsers.isEmpty) {
                                        VStack { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                Image(systemName = "list.number")
                                                    .font(Font.system(size = 60.0))
                                                    .foregroundColor(Color.gray).Compose(composectx)
                                                Text(LocalizedStringKey(stringLiteral = "Keine Tabellendaten verfügbar"))
                                                    .font(Font.headline)
                                                    .padding(Edge.Set.top).Compose(composectx)
                                                Text(LocalizedStringKey(stringLiteral = "Bitte wähle eine Liga aus oder aktualisiere die Daten."))
                                                    .font(Font.subheadline)
                                                    .foregroundColor(Color.gray)
                                                    .multilineTextAlignment(TextAlignment.center)
                                                    .padding().Compose(composectx)
                                                Button(LocalizedStringKey(stringLiteral = "Aktualisieren")) { ->
                                                    Task(isMainActor = true) { -> viewModel.refresh() }
                                                }.Compose(composectx)
                                                ComposeResult.ok
                                            }
                                        }.Compose(composectx)
                                    } else {
                                        List { ->
                                            ComposeBuilder { composectx: ComposeContext ->
                                                ForEach(Array(viewModel.displayedUsers.enumerated()), id = { it.element.id }) { (index, user) ->
                                                    ComposeBuilder { composectx: ComposeContext ->
                                                        NavigationLink(destination = UserDetailView(user = user, selectedMatchDay = if (viewModel.tableType == LeagueTableViewModel.TableType.matchday) viewModel.selectedMatchDay else null)) { ->
                                                            ComposeBuilder { composectx: ComposeContext ->
                                                                LeagueUserRow(user = user, position = index + 1).Compose(composectx)
                                                                ComposeResult.ok
                                                            }
                                                        }.Compose(composectx)
                                                        ComposeResult.ok
                                                    }
                                                }
                                                .refreshable { -> MainActor.run { viewModel.refresh() } }.Compose(composectx)
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
                    .navigationTitle(if (viewModel.tableType == LeagueTableViewModel.TableType.overall) "Tabelle" else "Spieltag ${viewModel.selectedMatchDay}")
                    .onAppear { ->
                        viewModel.setKickbaseManager(kickbaseManager)
                        Task(isMainActor = true) { -> viewModel.loadOverallRanking() }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedviewModel by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<LeagueTableViewModel>, Any>) { mutableStateOf(_viewModel) }
        _viewModel = rememberedviewModel

        _kickbaseManager.wrappedValue = EnvironmentValues.shared.environmentObject(type = KickbaseManager::class)!!

        return super.Evaluate(context, options)
    }

    constructor(viewModel: LeagueTableViewModel = LeagueTableViewModel()) {
        this._viewModel = skip.ui.State(viewModel)
    }
}

internal class LeagueUserRow: View {
    internal val user: LeagueUser
    internal val position: Int

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            HStack(spacing = 12.0) { ->
                ComposeBuilder { composectx: ComposeContext ->
                    // Position badge
                    Text({
                        val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                        str.appendInterpolation(position)
                        LocalizedStringKey(stringInterpolation = str)
                    }())
                        .font(Font.system(size = 16.0, weight = Font.Weight.bold))
                        .foregroundColor(Color.white)
                        .frame(width = 32.0, height = 32.0)
                        .background(positionColor)
                        .clipShape(Circle()).Compose(composectx)

                    // User info
                    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text(user.name)
                                .font(Font.headline).Compose(composectx)
                            Text(user.teamName)
                                .font(Font.subheadline)
                                .foregroundColor(Color.secondary).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)

                    Spacer().Compose(composectx)

                    // Points
                    VStack(alignment = HorizontalAlignment.trailing, spacing = 4.0) { ->
                        ComposeBuilder { composectx: ComposeContext ->
                            Text({
                                val str = LocalizedStringKey.StringInterpolation(literalCapacity = 0, interpolationCount = 0)
                                str.appendInterpolation(user.points)
                                LocalizedStringKey(stringInterpolation = str)
                            }())
                                .font(Font.system(size = 20.0, weight = Font.Weight.bold))
                                .foregroundColor(Color.primary).Compose(composectx)
                            Text(LocalizedStringKey(stringLiteral = "Punkte"))
                                .font(Font.caption)
                                .foregroundColor(Color.secondary).Compose(composectx)
                            ComposeResult.ok
                        }
                    }.Compose(composectx)
                    ComposeResult.ok
                }
            }
            .padding(Edge.Set.vertical, 4.0).Compose(composectx)
        }
    }

    private val positionColor: Color
        get() {
            when (position) {
                1 -> return Color.yellow
                2 -> return Color(red = 0.75, green = 0.75, blue = 0.75) // Silver
                3 -> return Color(red = 0.8, green = 0.5, blue = 0.2) // Bronze
                else -> return Color.blue
            }
        }

    constructor(user: LeagueUser, position: Int) {
        this.user = user
        this.position = position
    }
}
