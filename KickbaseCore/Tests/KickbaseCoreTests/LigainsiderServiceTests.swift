import XCTest

@testable import KickbaseCore

@MainActor
final class LigainsiderServiceTests: XCTestCase {

    override func tearDown() {
        URLProtocolMock.requestHandler = nil
        super.tearDown()
    }

    func makeSession() -> URLSession {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [URLProtocolMock.self]
        return URLSession(configuration: config)
    }

    func testFetchAllSquadsPopulatesCache() async throws {
        let session = makeSession()
        let service = LigainsiderService(session: session)

        // Minimal HTML with one player link and an image in previous component
        let html = """
                <div>
                    <img src=\"https://www.ligainsider.de/images/player.jpg\" />
                </div>
                <a href=\"/john-doe_12345\">John Doe</a>
            """.data(using: .utf8)!

        URLProtocolMock.requestHandler = { request in
            // Return the same simple HTML for every team path
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (resp, html)
        }

        XCTAssertEqual(service.playerCacheCount, 0)
        await service.fetchAllSquadsAsync()
        XCTAssertTrue(service.playerCacheCount > 0, "Expected player cache to be populated")
    }

    func testFetchLineupsParsesMatchesAndPopulatesCache() async throws {
        let session = makeSession()
        let service = LigainsiderService(session: session)

        // Overview: contains two team links to form one match pair
        let overviewHTML = """
                <html>
                    <body>
                        <a href=\"/bundesliga/team/team1/saison-2025\">Team 1</a>
                        <a href=\"/bundesliga/team/team2/saison-2025\">Team 2</a>
                    </body>
                </html>
            """

        // Team detail with a lineup marker and a player link inside
        let teamHTML = """
                <html>
                    <body>
                        <h2 itemprop=\"name\">TeamName</h2>
                        VORAUSSICHTLICHE AUFSTELLUNG
                        <a href=\"/player-one_99999\">Player One</a>
                    </body>
                </html>
            """

        URLProtocolMock.requestHandler = { request in
            let urlStr = request.url!.absoluteString
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            if urlStr.contains("spieltage") {
                return (resp, overviewHTML.data(using: .utf8)!)
            }
            return (resp, teamHTML.data(using: .utf8)!)
        }

        XCTAssertEqual(service.matches.count, 0)
        XCTAssertEqual(service.playerCacheCount, 0)

        await service.fetchLineupsAsync()

        XCTAssertTrue(service.matches.count >= 1, "Expected at least one match parsed")
        XCTAssertTrue(
            service.playerCacheCount > 0,
            "Expected player cache to contain players after fetching lineups")
        XCTAssertTrue(service.isLigainsiderReady, "Service should mark ready after fetch")
    }

    func testFetchSquadAcceptsTrailingSlashInHref() async throws {
        let session = makeSession()
        let service = LigainsiderService(session: session)

        let html = """
                <div>
                    <img src=\"https://www.ligainsider.de/images/player2.jpg\" />
                </div>
                <a href=\"/jane-doe_54321/\">Jane Doe</a>
            """.data(using: .utf8)!

        URLProtocolMock.requestHandler = { request in
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (resp, html)
        }

        XCTAssertEqual(service.playerCacheCount, 0)
        await service.fetchAllSquadsAsync()
        XCTAssertTrue(service.playerCacheCount > 0, "Expected trailing-slash slugs to be parsed")
    }

    func testFetchSquadHandles404Gracefully() async throws {
        let session = makeSession()
        let service = LigainsiderService(session: session)

        URLProtocolMock.requestHandler = { request in
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 404, httpVersion: nil, headerFields: nil)!
            return (resp, Data())
        }

        XCTAssertEqual(service.playerCacheCount, 0)
        await service.fetchAllSquadsAsync()
        XCTAssertEqual(service.playerCacheCount, 0, "404 should result in empty squad, not crash")
    }

    func testFetchLineupsHandlesMalformedOverview() async throws {
        let session = makeSession()
        let service = LigainsiderService(session: session)

        // Overview without team links
        let overviewHTML = """
                <html>
                    <body>
                        <p>No teams here</p>
                    </body>
                </html>
            """

        URLProtocolMock.requestHandler = { request in
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (resp, overviewHTML.data(using: .utf8)!)
        }

        XCTAssertEqual(service.matches.count, 0)
        await service.fetchLineupsAsync()
        XCTAssertEqual(service.matches.count, 0, "Malformed overview should parse zero matches")
        XCTAssertTrue(
            service.isLigainsiderReady, "Service should mark ready even if no matches found")
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

        let service = LigainsiderService(session: session)
        XCTAssertTrue(
            service.session.configuration.protocolClasses == nil
                || service.session.configuration.protocolClasses!.isEmpty)
    }
}
