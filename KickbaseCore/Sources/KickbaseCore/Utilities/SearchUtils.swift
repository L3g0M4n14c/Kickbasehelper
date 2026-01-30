import Foundation

extension String {
    // Helper to extract content safely without exposing Swift.String.Index to Kotlin/Skip

    public func substringAfter(_ marker: String) -> String? {
        // Safer implementation effectively using split/components which transpiles well
        let parts = self.components(separatedBy: marker)
        guard parts.count > 1 else { return nil }
        // Join the rest in case the marker appears multiple times
        return parts.dropFirst().joined(separator: marker)
    }

    public func substringBefore(_ marker: String) -> String? {
        let parts = self.components(separatedBy: marker)
        guard parts.count > 1 else { return nil }
        return parts.first
    }

    public func substringBetween(_ startMarker: String, _ endMarker: String) -> String? {
        guard let after = self.substringAfter(startMarker) else { return nil }
        return after.substringBefore(endMarker)
    }

    public func splitBy(_ separator: String) -> [String] {
        return self.components(separatedBy: separator)
    }

    /// Split a string by any of the provided characters (e.g., " _-") in a stable, explicit way
    public func splitByCharacters(_ separators: String) -> [String] {
        var out: [String] = []
        var current = ""
        for ch in self {
            if separators.contains(ch) {
                if !current.isEmpty {
                    out.append(current)
                    current = ""
                }
            } else {
                current.append(ch)
            }
        }
        if !current.isEmpty { out.append(current) }
        return out
    }
}

extension Substring {
    public func substringAfter(_ marker: String) -> String? {
        return String(self).substringAfter(marker)
    }

    public func substringBefore(_ marker: String) -> String? {
        return String(self).substringBefore(marker)
    }

    public func substringBetween(_ startMarker: String, _ endMarker: String) -> String? {
        return String(self).substringBetween(startMarker, endMarker)
    }

    public func splitBy(_ separator: String) -> [String] {
        return String(self).components(separatedBy: separator)
    }

    public func splitByCharacters(_ separators: String) -> [String] {
        return String(self).splitByCharacters(separators)
    }
}
