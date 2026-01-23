import SwiftUI

#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

extension Color {
    static var systemBackgroundCompat: Color {
        #if canImport(UIKit)
        return Color(uiColor: .systemBackground)
        #elseif canImport(AppKit)
        return Color(nsColor: .windowBackgroundColor)
        #else
        return Color.white
        #endif
    }
    
    static var systemGray6Compat: Color {
        #if canImport(UIKit)
        return Color(uiColor: .systemGray6)
        #elseif canImport(AppKit)
        return Color(nsColor: .controlBackgroundColor)
        #else
        return Color.gray
        #endif
    }
    
    static var systemGray5Compat: Color {
        #if canImport(UIKit)
        return Color(uiColor: .systemGray5)
        #elseif canImport(AppKit)
        return Color(nsColor: .controlBackgroundColor)
        #else
        return Color.gray
        #endif
    }
    
    static var systemGroupedBackgroundCompat: Color {
        #if canImport(UIKit)
        return Color(uiColor: .systemGroupedBackground)
        #elseif canImport(AppKit)
        return Color(nsColor: .windowBackgroundColor)
        #else
        return Color.white
        #endif
    }
}

extension ToolbarItemPlacement {
    static var navigationBarTrailingCompat: ToolbarItemPlacement {
        #if os(iOS)
        return .navigationBarTrailing
        #else
        return .primaryAction
        #endif
    }
    
    static var navigationBarLeadingCompat: ToolbarItemPlacement {
        #if os(iOS)
        return .navigationBarLeading
        #else
        return .cancellationAction
        #endif
    }
}

func isLargeScreen() -> Bool {
    #if os(iOS)
    return UIDevice.current.userInterfaceIdiom == .pad || UIDevice.current.userInterfaceIdiom == .mac
    #elseif os(macOS)
    return true
    #else
    return false
    #endif
}
