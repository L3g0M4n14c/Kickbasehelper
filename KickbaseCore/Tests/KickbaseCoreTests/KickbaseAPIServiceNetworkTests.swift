import XCTest

@testable import KickbaseCore

@MainActor
final class KickbaseAPIServiceNetworkTests: XCTestCase {

    override func tearDown() {
        URLProtocolMock.requestHandler = nil
        super.tearDown()
    }

    func makeSession() -> URLSession {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        return URLSession(configuration: config)
    }

    func testGetLeagueSelectionReturnsParsedJson() async throws {
        let session = makeSession()
        let api = KickbaseAPIService(session: session)

        let payload = "{ \"leagues\": [{ \"id\": \"L1\", \"name\": \"Test\" }] }".data(
            using: .utf8)!

        URLProtocolMock.requestHandler = { request in
            XCTAssertEqual(request.url?.path, "/v4/leagues/selection")
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (resp, payload)
        }

        let json = try await api.getLeagueSelection()
        let leagues = json["leagues"] as? [[String: Any]]
        XCTAssertEqual(leagues?.first?["id"] as? String, "L1")
    }

    func testGetMyBudgetThrowsWithoutAuthToken() async throws {
        let session = makeSession()
        let api = KickbaseAPIService(session: session)

        // Should throw before network call due to missing auth
        do {
            _ = try await api.getMyBudget(leagueId: "l")
            XCTFail("Expected APIError.noAuthToken")
        } catch let err as APIError {
            if case APIError.noAuthToken = err {
                // success
            } else {
                XCTFail("Expected APIError.noAuthToken, got \(err)")
            }
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }

    func testGetMyBudgetReturnsJsonWhenAuthorized() async throws {
        let session = makeSession()
        let api = KickbaseAPIService(session: session)
        api.setAuthToken("token-123")

        let payload = "{ \"budget\": 5000 }".data(using: .utf8)!

        URLProtocolMock.requestHandler = { request in
            XCTAssertEqual(request.url?.path, "/v4/leagues/l/me/budget")
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer token-123")
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (resp, payload)
        }

        let json = try await api.getMyBudget(leagueId: "l")
        XCTAssertEqual(json["budget"] as? Int, 5000)
    }

    func testSanitizesInjectedSessionInProduction() async throws {
        let originalFlag = ProcessInfo.processInfo.environment[
            "KICKBASE_SANITIZER_FORCE_PRODUCTION"]
        setenv("KICKBASE_SANITIZER_FORCE_PRODUCTION", "1", 1)
        defer {
            if let originalFlag = originalFlag {
                setenv("KICKBASE_SANITIZER_FORCE_PRODUCTION", originalFlag, 1)
            } else {
                unsetenv("KICKBASE_SANITIZER_FORCE_PRODUCTION")
            }
        }

        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        let session = URLSession(configuration: config)

        let api = KickbaseAPIService(session: session)
        XCTAssertTrue(
            api.session.configuration.protocolClasses == nil
                || api.session.configuration.protocolClasses!.isEmpty)
    }
}
