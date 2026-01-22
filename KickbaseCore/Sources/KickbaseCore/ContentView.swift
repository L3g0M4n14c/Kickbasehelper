//
//  ContentView.swift
//  Kickbasehelper
//
//  Created by Marco Corro on 27.08.25.
//

import SwiftData
import SwiftUI

public struct ContentView: View {
    @StateObject private var authManager = AuthenticationManager()
    @StateObject private var kickbaseManager = KickbaseManager()

    public init() {}

    public var body: some View {
        Group {
            if authManager.isAuthenticated {
                MainDashboardView()
                    .environmentObject(authManager)
                    .environmentObject(kickbaseManager)
                    .onAppear {
                        if let token = authManager.accessToken {
                            kickbaseManager.setAuthToken(token)
                        }
                        Task {
                            await kickbaseManager.loadUserData()
                        }
                    }
            } else {
                LoginView()
                    .environmentObject(authManager)
            }
        }
        #if !SKIP
            .macOSScaled()
            .macOSOptimized()
        #endif
    }
}

#Preview {
    ContentView()
}
