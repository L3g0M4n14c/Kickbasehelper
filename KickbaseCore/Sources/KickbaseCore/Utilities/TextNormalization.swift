import Foundation

/// Normalize a string for searching: lowercase, simple transliteration for common characters,
/// remove combining diacritics and collapse separators into single spaces.
/// This avoids using `String.folding(options:locale:)` which does not transpile reliably.
public func normalizeForSearch(_ text: String) -> String {
    var s = text.lowercased()

    // Common manual transliterations to keep IDs stable across platforms
    let replacements: [(String, String)] = [
        ("ä", "ae"), ("ö", "oe"), ("ü", "ue"), ("ß", "ss"),
        ("ć", "c"), ("č", "c"), ("š", "s"), ("ž", "z"), ("đ", "d"),
    ]
    for (from, to) in replacements {
        s = s.replacingOccurrences(of: from, with: to)
    }

    // Remove diacritic marks by decomposing and stripping common combining mark ranges
    let decomposed = s.decomposedStringWithCanonicalMapping
    let filteredScalars = decomposed.unicodeScalars.filter { scalar in
        let v = scalar.value
        // Basic Combining Diacritical Marks range (U+0300 - U+036F)
        return !(v >= 0x0300 && v <= 0x036F)
    }
    s = String(String.UnicodeScalarView(filteredScalars))

    // Replace common separators with space and collapse whitespace
    s = s.replacingOccurrences(of: "-", with: " ")
    let parts = s.split { $0.isWhitespace }.map(String.init)
    return parts.joined(separator: " ").map { trimWhitespaceNewlines($0) }.joined(separator: " ")
}
