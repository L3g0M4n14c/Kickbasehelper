import XCTest

@testable import KickbaseCore

@MainActor
final class KickbaseAPIClientTests: XCTestCase {

    override func tearDown() {
        URLProtocolMock.requestHandler = nil
        super.tearDown()
    }

    func makeSession() -> URLSession {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        return URLSession(configuration: config)
    }

    func testMakeRequestAddsAuthorizationHeaderAndReturnsData() async throws {
        let session = makeSession()
        let client = KickbaseAPIClient(session: session)
        client.setAuthToken("token-xyz")

        let payload = "{ \"ok\": true }".data(using: .utf8)!

        URLProtocolMock.requestHandler = { request in
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer token-xyz")
            XCTAssertEqual(request.httpMethod, "GET")
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (resp, payload)
        }

        let (data, response) = try await client.makeRequest(endpoint: "/v4/test")
        XCTAssertEqual((response as HTTPURLResponse).statusCode, 200)
        let json = jsonDict(from: data)
        XCTAssertEqual(json["ok"] as? Bool, true)
    }

    func testMakeRequestThrowsWithoutAuthToken() async throws {
        let session = makeSession()
        let client = KickbaseAPIClient(session: session)

        do {
            _ = try await client.makeRequest(endpoint: "/v4/test")
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

    func testTestNetworkConnectivityReturnsTrueOn200() async throws {
        let session = makeSession()
        let client = KickbaseAPIClient(session: session)

        URLProtocolMock.requestHandler = { request in
            // Expect HEAD
            XCTAssertEqual(request.httpMethod, "HEAD")
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (resp, Data())
        }

        let ok = await client.testNetworkConnectivity()
        XCTAssertTrue(ok)
    }

    func testTestNetworkConnectivityReturnsFalseOnError() async throws {
        let session = makeSession()
        let client = KickbaseAPIClient(session: session)

        URLProtocolMock.requestHandler = { request in
            throw NSError(domain: "Network", code: -1, userInfo: nil)
        }

        let ok = await client.testNetworkConnectivity()
        XCTAssertFalse(ok)
    }

    func testTryMultipleEndpointsSkips500AndReturns200() async throws {
        let session = makeSession()
        let client = KickbaseAPIClient(session: session)
        client.setAuthToken("t")

        let payload = "{ \"ok\": true }".data(using: .utf8)!
        var callCount = 0

        URLProtocolMock.requestHandler = { request in
            callCount += 1
            let resp: HTTPURLResponse
            if callCount == 1 {
                resp = HTTPURLResponse(
                    url: request.url!, statusCode: 500, httpVersion: nil, headerFields: nil)!
                return (resp, Data())
            }
            resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (resp, payload)
        }

        let (data, json) = try await client.tryMultipleEndpoints(endpoints: ["/e1", "/e2"])
        let parsed = jsonDict(from: data)
        XCTAssertEqual(parsed["ok"] as? Bool, true)
        XCTAssertGreaterThanOrEqual(callCount, 2)
        XCTAssertEqual(json["ok"] as? Bool, true)
    }

    func testTryMultipleEndpointsThrowsWhenAllFail() async throws {
        let session = makeSession()
        let client = KickbaseAPIClient(session: session)
        client.setAuthToken("t")

        URLProtocolMock.requestHandler = { request in
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 500, httpVersion: nil, headerFields: nil)!
            return (resp, Data())
        }

        do {
            _ = try await client.tryMultipleEndpoints(endpoints: ["/e1", "/e2"])
            XCTFail("Expected error when all endpoints fail")
        } catch {
            // any error is acceptable (network error or APIError)
        }
    }

    func testSanitizesInjectedSessionInProduction() async throws {
        // Simulate non-test production by forcing production sanitizer behavior
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

        let client = KickbaseAPIClient(session: session)
        // In forced-production the sanitizer should remove custom protocol classes
        XCTAssertTrue(
            client.session.configuration.protocolClasses == nil
                || client.session.configuration.protocolClasses!.isEmpty)
    }
}
