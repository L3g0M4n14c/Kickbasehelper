//
//  ContentView.swift
//  Kickbasehelper
//
//  Created by Marco Corro on 27.08.25.
//

package kickbase.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import skip.lib.*

// import SwiftData
import skip.ui.*
import skip.foundation.*
import skip.model.*

class ContentView: View {
    private var authManager: AuthenticationManager
        get() = _authManager.wrappedValue
        set(newValue) {
            _authManager.wrappedValue = newValue
        }
    private var _authManager: skip.ui.State<AuthenticationManager> = skip.ui.State(AuthenticationManager())
    private var kickbaseManager: KickbaseManager
        get() = _kickbaseManager.wrappedValue
        set(newValue) {
            _kickbaseManager.wrappedValue = newValue
        }
    private var _kickbaseManager: skip.ui.State<KickbaseManager> = skip.ui.State(KickbaseManager())

    constructor() {
    }

    override fun body(): View {
        return ComposeBuilder { composectx: ComposeContext ->
            Group { ->
                ComposeBuilder { composectx: ComposeContext ->
                    if (authManager.isAuthenticated) {
                        MainDashboardView()
                            .environmentObject(authManager)
                            .environmentObject(kickbaseManager)
                            .onAppear { ->
                                authManager.accessToken?.let { token ->
                                    kickbaseManager.setAuthToken(token)
                                }
                                Task(isMainActor = true) { -> kickbaseManager.loadUserData() }
                            }.Compose(composectx)
                    } else {
                        LoginView()
                            .environmentObject(authManager).Compose(composectx)
                    }
                    ComposeResult.ok
                }
            }.Compose(composectx)
        }
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        val rememberedauthManager by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<AuthenticationManager>, Any>) { mutableStateOf(_authManager) }
        _authManager = rememberedauthManager

        val rememberedkickbaseManager by rememberSaveable(stateSaver = context.stateSaver as Saver<skip.ui.State<KickbaseManager>, Any>) { mutableStateOf(_kickbaseManager) }
        _kickbaseManager = rememberedkickbaseManager

        return super.Evaluate(context, options)
    }

    @androidx.annotation.Keep
    companion object {
    }
}

// #Preview omitted
