//
//  ScalingUtility.swift
//  Kickbasehelper
//
//  Created for macOS scaling improvements
//

import SwiftUI

extension View {
    /// Appliziert plattformspezifische Skalierung
    public func macOSScaled() -> some View {
        self.modifier(MacOSScalingModifier())
    }
}

public struct MacOSScalingModifier: ViewModifier {
    @Environment(\.horizontalSizeClass) var horizontalSizeClass

    public init() {}

    var scaleFactor: CGFloat {
        #if os(macOS)
            return 1.2  // 20% größer auf macOS
        #elseif os(iOS)
            if UIDevice.current.userInterfaceIdiom == .pad {
                return 1.0  // iPad bleibt normal
            } else {
                return 1.0  // iPhone bleibt normal
            }
        #else
            return 1.0
        #endif
    }

    var fontScaleFactor: CGFloat {
        #if os(macOS)
            return 1.15  // 15% größere Schrift auf macOS
        #else
            return 1.0
        #endif
    }

    public func body(content: Content) -> some View {
        #if canImport(UIKit)
            let fontSize = UIFont.systemFontSize
        #elseif canImport(AppKit)
            let fontSize = NSFont.systemFontSize
        #else
            let fontSize = 14.0
        #endif

        return
            content
            .scaleEffect(scaleFactor)
            .font(.system(size: fontSize * fontScaleFactor))
    }
}

// Plattformspezifische Padding-Werte
extension View {
    public func adaptivePadding(_ edges: Edge.Set = .all, _ length: CGFloat? = nil) -> some View {
        let paddingValue = length ?? 8.0

        #if os(macOS)
            let macOSPadding = paddingValue * 1.2
            return self.padding(edges, macOSPadding)
        #else
            return self.padding(edges, paddingValue)
        #endif
    }

    public func adaptiveFont(_ style: Font.TextStyle) -> some View {
        #if os(macOS)
            return self.font(.system(style).weight(.regular))
        #else
            return self.font(.system(style))
        #endif
    }
}

// Zusätzliche macOS-spezifische Modifikationen
extension View {
    public func macOSOptimized() -> some View {
        self.modifier(MacOSOptimizationModifier())
    }
}

public struct MacOSOptimizationModifier: ViewModifier {
    public init() {}

    public func body(content: Content) -> some View {
        #if os(macOS)
            content
                .buttonStyle(PlainButtonStyle())  // Entfernt iOS-spezifische Button-Styles
        #else
            content
        #endif
    }
}
