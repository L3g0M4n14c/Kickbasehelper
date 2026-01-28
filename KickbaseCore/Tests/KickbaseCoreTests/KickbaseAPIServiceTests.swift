import XCTest

@testable import KickbaseCore

@MainActor
final class KickbaseAPIServiceTests: XCTestCase {

    func testAuthTokenSetAndHasAuthToken() {
        let api = KickbaseAPIService()
        XCTAssertFalse(api.hasAuthToken())
        api.setAuthToken("abc123")
        XCTAssertTrue(api.hasAuthToken())
    }
}
