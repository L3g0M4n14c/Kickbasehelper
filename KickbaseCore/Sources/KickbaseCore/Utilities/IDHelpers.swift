import Foundation

/// Returns a short stable id derived from a UUID (default length 8). Removes hyphens to be safe.
public func shortUUID(_ length: Int = 8) -> String {
    let raw = UUID().uuidString.replacingOccurrences(of: "-", with: "")
    return String(raw.prefix(max(0, min(length, raw.count))))
}
