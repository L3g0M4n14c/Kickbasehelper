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

/// Chooses a profile image URL from available sources, preferring Ligainsider/player images and skipping Kickbase-provided images.
/// We intentionally *ignore* Kickbase-supplied images (profileBigUrl/pim/etc.) to prefer Ligainsider or explicit player images.
func chooseProfileBigUrl(_ details: PlayerDetailResponse?, _ playerData: [String: Any]) -> String {
    // Candidate keys to try in order (these often come from Kickbase API, but we'll only accept Ligainsider/player ones)
    let keys = ["profileBigUrl", "imageUrl", "image", "photo", "pim"]
    let candidates = keys.compactMap { playerData[$0] as? String }

    // 1) Prefer explicit Ligainsider-hosted images (strong signal)
    if let liga = candidates.first(where: {
        $0.contains("ligainsider.de") && !isLikelyFlagImage($0)
    }) {
        return liga
    }

    // 2) Prefer explicit player images (e.g., '/player/team/') if present and not a flag
    if let playerImg = candidates.first(where: { isLikelyPlayerImage($0) && !isLikelyFlagImage($0) }
    ) {
        return playerImg
    }

    // 3) Otherwise, do not use Kickbase-supplied images -> return empty
    return ""
}
