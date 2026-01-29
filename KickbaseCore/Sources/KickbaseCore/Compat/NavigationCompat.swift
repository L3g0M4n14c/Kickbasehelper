// NavigationCompat.swift
// Purpose: Provide a small place to add SKIP comments / platform guards to influence
// generated Kotlin for Navigation-related Swift constructs.

// IMPORTANT: This file is intended as a template. Add SKIP DECLARE / REPLACE
// comments here to override generator output for Android when necessary, and
// document any temporary changes with TODO + Issue reference.

import Foundation
import SwiftUI

// Example pattern: provide an Android fallback replacement for a complex Navigation type.
// Place precise SKIP directive(s) above a Swift declaration so the Skip transpiler will
// apply the directive when generating Kotlin.

#if os(Android)
// SKIP REPLACE:
// // Kotlin replacement for NavigationStack to keep Android output simple/stable
// package skip.ui
//
// public class NavigationStack {
//     public fun didCompose(navController: Any, destinations: Any, path: Any?, navigationPath: Any?, keyboardController: Any?) { }
//     public fun navigate(to: Any) { }
// }

// Android stub implementation used only for transpiler guidance.
struct NavigationStackAndroidStub: View {
    let content: () -> AnyView
    init(@ViewBuilder content: @escaping () -> AnyView) {
        self.content = content
    }
    var body: some View { EmptyView() }
}

#else
// On non-Android platforms, do nothing and use real NavigationStack.
#endif

// TODO: Replace the SKIP REPLACE snippet above with a minimal, exact Kotlin signature that
// matches the usage sites in our Swift codebase. Reference: docs/SKIP_SKILL.md and
// open an upstream issue to make generator behave consistently.
