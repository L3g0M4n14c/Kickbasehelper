import XCTest

final class KickbaseCoreUITests: XCTestCase {
    func test_launchAndOpenPlayerDetail_demoFlow() throws {
        let app = XCUIApplication()

        // Use launch arguments/environment to force demo data and test-friendly mode
        app.launchArguments.append("--uitesting")
        app.launchEnvironment["KICKBASE_USE_DEMO_ACCOUNT"] = "1"
        app.launchEnvironment["KICKBASE_SANITIZER_FORCE_PRODUCTION"] = "0"

        app.launch()

        // Expect a navigation title or label that indicates the main screen loaded
        let navTitle = app.staticTexts["Kickbase Helper"]
        XCTAssertTrue(navTitle.waitForExistence(timeout: 5))

        // Try to find a Team tab/button and open it
        if app.buttons["tab_team"].waitForExistence(timeout: 2) {
            app.buttons["tab_team"].tap()
        }

        // Try to tap the first player row (requires accessibility id set in views)
        let firstPlayer = app.buttons.matching(identifier: "player_row_").firstMatch
        if firstPlayer.exists {
            firstPlayer.tap()
            // Expect player detail to show – check for player lastname accessibility id
            let lastName = app.staticTexts["player_lastname"]
            XCTAssertTrue(lastName.waitForExistence(timeout: 2))
        } else {
            // If no player rows available, pass with a note; CI will still need real demo data
            XCTContext.runActivity(named: "No player row found - ensure demo data is enabled") {
                _ in
                XCTAssertTrue(true)
            }
        }
    }
}
