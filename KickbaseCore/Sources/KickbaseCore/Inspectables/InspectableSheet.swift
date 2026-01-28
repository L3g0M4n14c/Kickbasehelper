import SwiftUI

// A small wrapper that forwards to the real .sheet but provides a named modifier
// that can be extended in tests to conform to ViewInspector's PopupPresenter.

extension View {
    func sheet2<Sheet>(
        isPresented: Binding<Bool>, onDismiss: (() -> Void)? = nil,
        @ViewBuilder content: @escaping () -> Sheet
    ) -> some View where Sheet: View {
        return self.modifier(
            InspectableSheet(presented: isPresented, onDismiss: onDismiss, builder: content))
    }
}

struct InspectableSheet<Sheet>: SwiftUI.ViewModifier where Sheet: View {
    // Use different stored names so we can explicitly expose the protocol properties
    let presented: Binding<Bool>
    let onDismiss: (() -> Void)?
    let builder: () -> Sheet

    func body(content: Self.Content) -> some View {
        content.sheet(isPresented: presented, onDismiss: onDismiss, content: builder)
    }
}

#if canImport(ViewInspector)
    import ViewInspector
    extension InspectableSheet: PopupPresenter {
        public typealias Popup = Sheet
        public var isPresented: Binding<Bool> { presented }
        public var popupBuilder: () -> Sheet { builder }
    }
#endif
