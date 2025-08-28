//
//  ContentView.swift
//  Kickbasehelper
//
//  Created by Marco Corro on 27.08.25.
//

import SwiftUI
import SwiftData

struct ContentView: View {
    @StateObject private var authManager = AuthenticationManager()
    @StateObject private var kickbaseManager = KickbaseManager()

    var body: some View {
        NavigationStack {
            if authManager.isAuthenticated {
                MainDashboardView()
                    .environmentObject(kickbaseManager)
                    .environmentObject(authManager)
            } else {
                LoginView()
                    .environmentObject(authManager)
            }
        }
        .onAppear {
            if authManager.isAuthenticated && authManager.accessToken != nil {
                kickbaseManager.setAuthToken(authManager.accessToken!)
                Task {
                    await kickbaseManager.loadUserData()
                }
            }
        }
    }
}

#Preview {
    ContentView()
}
