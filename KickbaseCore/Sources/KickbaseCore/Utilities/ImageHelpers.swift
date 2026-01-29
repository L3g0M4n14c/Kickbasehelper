import Foundation

/// Heuristics to detect if a URL likely points to a country flag image (not a player photo)
func isLikelyFlagImage(_ urlString: String) -> Bool {
    let s = urlString.lowercased()

    // Explicit flag paths used by Ligainsider
    if s.contains("images/nations") || s.contains("/nations/") {
        return true
    }

    // Common path segments or filenames that indicate flags
    if s.contains("/flag/") || s.contains("/flags/") || s.contains("/country/") {
        return true
    }

    if s.contains("-flag") || s.contains("/flag-") || s.contains("flag.png")
        || s.contains("flag.jpg")
    {
        return true
    }

    // Team wappen or team logos - treat as non-player images (skip)
    if s.contains("images/teams") || s.contains("wappen") || s.contains("/teams/") {
        return true
    }

    // Heuristic: two-letter country codes as filenames like 'de.png' or 'us.jpg'
    if let last = URL(string: s)?.lastPathComponent {
        let name = last.components(separatedBy: ".").first ?? ""
        if name.count == 2
            && (last.hasSuffix(".png") || last.hasSuffix(".jpg") || last.hasSuffix(".jpeg"))
        {
            return true
        }
    }

    return false
}

/// Returns true if the URL looks like a player's image on Ligainsider (contains 'player/team')
func isLikelyPlayerImage(_ urlString: String) -> Bool {
    let s = urlString.lowercased()

    // Explicit player directories
    if s.contains("/player/team/") || s.contains("/images/player/") {
        return true
    }

    // Generic player hints and class patterns
    if s.contains("/player/") || s.contains("player_img") || s.contains("player-image")
        || s.contains("playerphoto") || s.contains("player-photo")
    {
        return true
    }

    return false
}

/// Chooses a profile image URL from available sources, preferring explicit player images and skipping likely flag images.
func chooseProfileBigUrl(_ details: PlayerDetailResponse?, _ playerData: [String: Any]) -> String {
    // Prefer detail-provided image if present and not a flag
    if let d = details, let pb = d.profileBigUrl, !pb.isEmpty, !isLikelyFlagImage(pb) {
        return pb
    }

    // Candidate keys to try in order
    let keys = ["profileBigUrl", "imageUrl", "image", "photo", "pim"]
    var candidates = keys.compactMap { playerData[$0] as? String }

    // Prefer explicit player images (e.g., contain '/player/team/') if present
    if let playerCandidate = candidates.first(where: {
        isLikelyPlayerImage($0) && !isLikelyFlagImage($0)
    }) {
        return playerCandidate
    }

    // Otherwise pick first non-flag candidate
    if let nonFlag = candidates.first(where: { !isLikelyFlagImage($0) }) {
        return nonFlag
    }

    // No suitable image found
    return ""
}
