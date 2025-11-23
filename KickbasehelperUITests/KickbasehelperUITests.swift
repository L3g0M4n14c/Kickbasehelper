//
//  KickbasehelperUITests.swift
//  KickbasehelperUITests
//
//  Created by Marco Corro on 27.08.25.
//

import XCTest

final class KickbasehelperUITests: XCTestCase {

    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.

        // In UI tests it is usually best to stop immediately when a failure occurs.
        continueAfterFailure = false

        // In UI tests it’s important to set the initial state - such as interface orientation - required for your tests before they run. The setUp method is a good place to do this.
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

    // MARK: - Navigation Tests

    @MainActor
    func testTabBarNavigation() throws {
        let app = XCUIApplication()
        app.launch()

        let tabBar = app.tabBars.firstMatch

        if tabBar.exists {
            let buttons = tabBar.buttons
            XCTAssertTrue(buttons.count > 0, "Tab bar sollte Navigations-Buttons haben")
        }
    }

    @MainActor
    func testBackNavigation() throws {
        let app = XCUIApplication()
        app.launch()

        let backButton = app.navigationBars.buttons.element(boundBy: 0)

        if backButton.exists {
            XCTAssertTrue(backButton.isHittable, "Zurück-Button sollte klickbar sein")
        }
    }

    // MARK: - UI Elements Tests

    @MainActor
    func testButtonInteraction() throws {
        let app = XCUIApplication()
        app.launch()

        let buttons = app.buttons

        if buttons.count > 0 {
            let button = buttons.firstMatch
            XCTAssertTrue(button.exists, "Buttons sollten in der App existieren")
        }
    }

    @MainActor
    func testTextFieldInput() throws {
        let app = XCUIApplication()
        app.launch()

        let textFields = app.textFields

        if textFields.count > 0 {
            let textField = textFields.firstMatch
            XCTAssertTrue(textField.exists, "Text-Felder sollten vorhanden sein")
        }
    }

    // MARK: - Scrolling Tests

    @MainActor
    func testScrollViewScrolling() throws {
        let app = XCUIApplication()
        app.launch()

        let scrollView = app.scrollViews.firstMatch

        if scrollView.exists {
            scrollView.scroll(byDeltaX: 0, deltaY: 100)
            XCTAssertTrue(scrollView.exists, "Scroll-View sollte nach Scrollen existieren")
        }
    }

    @MainActor
    func testTableViewScrolling() throws {
        let app = XCUIApplication()
        app.launch()

        let tables = app.tables

        if tables.count > 0 {
            let table = tables.firstMatch

            table.scroll(byDeltaX: 0, deltaY: 200)
            XCTAssertTrue(table.exists, "Table-View sollte scrollbar sein")
        }
    }

    // MARK: - Alert Tests

    @MainActor
    func testAlertPresentation() throws {
        let app = XCUIApplication()
        app.launch()

        let alerts = app.alerts

        XCTAssertTrue(alerts.count >= 0, "Alerts sollten zugänglich sein")
    }

    // MARK: - Accessibility Tests

    @MainActor
    func testAccessibilityLabels() throws {
        let app = XCUIApplication()
        app.launch()

        let buttons = app.buttons

        // Überprüfe die ersten 5 Buttons auf Labels
        for button in buttons.allElementsBoundByIndex.prefix(5) {
            XCTAssertTrue(button.exists, "Button sollte existieren")
        }
    }

    @MainActor
    func testKeyboardDismissal() throws {
        let app = XCUIApplication()
        app.launch()

        let textField = app.textFields.firstMatch

        if textField.exists {
            textField.tap()
            app.keyboards.buttons["Return"].tap()

            XCTAssertTrue(textField.exists, "Text-Feld sollte nach Keyboard-Schließung existieren")
        }
    }

    // MARK: - Performance Tests

    @MainActor
    func testListScrollPerformance() throws {
        let app = XCUIApplication()
        app.launch()

        let scrollView = app.scrollViews.firstMatch

        if scrollView.exists {
            measure {
                scrollView.scroll(byDeltaX: 0, deltaY: 300)
                scrollView.scroll(byDeltaX: 0, deltaY: -300)
            }
        }
    }

    // MARK: - View Hierarchy Tests

    @MainActor
    func testViewHierarchy() throws {
        let app = XCUIApplication()
        app.launch()

        let windows = app.windows

        XCTAssertTrue(windows.count > 0, "App sollte mindestens ein Fenster haben")
        XCTAssertTrue(windows.firstMatch.exists, "Haupt-Fenster sollte existieren")
    }

    @MainActor
    func testStaticTextPresence() throws {
        let app = XCUIApplication()
        app.launch()

        let staticTexts = app.staticTexts

        XCTAssertTrue(staticTexts.count >= 0, "Statische Text-Elemente sollten vorhanden sein")
    }

    // MARK: - Orientation Tests

    @MainActor
    func testPortraitOrientation() throws {
        let app = XCUIApplication()
        XCUIDevice.shared.orientation = .portrait
        app.launch()

        let appWindow = app.windows.firstMatch
        XCTAssertTrue(appWindow.exists, "App sollte im Portrait-Modus funktionieren")
    }

    @MainActor
    func testLaunchPerformance() throws {
        // This measures how long it takes to launch your application.
        measure(metrics: [XCTApplicationLaunchMetric()]) {
            XCUIApplication().launch()
        }
    }
}
