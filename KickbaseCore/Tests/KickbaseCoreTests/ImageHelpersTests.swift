import XCTest

@testable import KickbaseCore

final class ImageHelpersTests: XCTestCase {
    func testIsLikelyFlagImageDetectsImagesNations() {
        XCTAssertTrue(isLikelyFlagImage("https://example.com/images/nations/de.png"))
        XCTAssertTrue(isLikelyFlagImage("https://example.com/flags/de.png"))
        XCTAssertTrue(isLikelyFlagImage("https://example.com/flag-de.png"))
    }

    func testIsLikelyPlayerImageDetectsPlayerTeam() {
        XCTAssertTrue(isLikelyPlayerImage("https://ligainsider.de/player/team/12345.png"))
        XCTAssertTrue(isLikelyPlayerImage("https://example.com/player/photo/1.png"))
    }

    func testChooseProfileBigUrlPrefersPlayerImageOverFlag() {
        let playerData: [String: Any] = [
            "pim": "https://example.com/images/nations/de.png",
            "imageUrl": "https://ligainsider.de/player/team/678.png",
        ]

        let chosen = chooseProfileBigUrl(nil, playerData)
        XCTAssertEqual(chosen, "https://ligainsider.de/player/team/678.png")
    }

    func testChooseProfileBigUrlReturnsEmptyWhenOnlyFlagPresent() {
        let playerData: [String: Any] = [
            "pim": "https://example.com/images/nations/de.png"
        ]

        let chosen = chooseProfileBigUrl(nil, playerData)
        XCTAssertEqual(chosen, "")
    }
}
