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

    func testFetchAllSquadsUsesNonPlayerLigainsiderImage() async throws {
        let session = makeSession()
        let service = LigainsiderService(session: session)

        let html = """
                <div>
                    <img src=\"https://www.ligainsider.de/images/player.jpg\" />
                </div>
                <a href=\"/john-doe_12345\">John Doe</a>
            """.data(using: .utf8)!

        URLProtocolMock.requestHandler = { request in
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (resp, html)
        }

        await service.fetchAllSquadsAsync()
        let p = service.getLigainsiderPlayer(firstName: "John", lastName: "Doe")
        XCTAssertNotNil(p)
        XCTAssertEqual(p?.imageUrl, "https://www.ligainsider.de/images/player.jpg")
    }

    func testFetchAllSquadsPrefersPlayerImgOverNation() async throws {
        let session = makeSession()
        let service = LigainsiderService(session: session)

        let html = """
                <div>
                    <img class="player_img" src=\"https://www.ligainsider.de/player/team/player123.jpg\" />
                    <img class="small_inner_icon" src=\"https://www.ligainsider.de/images/nations/de.png\" />
                </div>
                <a href=\"/john-doe_12345\">John Doe</a>
            """.data(using: .utf8)!

        URLProtocolMock.requestHandler = { request in
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (resp, html)
        }

        await service.fetchAllSquadsAsync()
        let p = service.getLigainsiderPlayer(firstName: "John", lastName: "Doe")
        XCTAssertNotNil(p)
        XCTAssertEqual(p?.imageUrl, "https://www.ligainsider.de/player/team/player123.jpg")
    }

    func testGetLigainsiderPlayerMatchesWhenKickbaseHasOnlyFirstName() async throws {
        let session = makeSession()
        let service = LigainsiderService(session: session)

        let html = """
                <div>
                    <img src=\"https://www.ligainsider.de/player/team/bernardo.jpg\" />
                </div>
                <a href=\"/bernardo_12345\">Bernardo</a>
            """.data(using: .utf8)!

        URLProtocolMock.requestHandler = { request in
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (resp, html)
        }

        await service.fetchAllSquadsAsync()
        // Kickbase has only first name 'Bernardo'
        let p1 = service.getLigainsiderPlayer(firstName: "Bernardo", lastName: "")
        XCTAssertNotNil(p1)
        XCTAssertEqual(p1?.name, "Bernardo")

        // Also check if searching with empty firstName and lastName 'Bernardo' succeeds
        let p2 = service.getLigainsiderPlayer(firstName: "", lastName: "Bernardo")
        XCTAssertNotNil(p2)
        XCTAssertEqual(p2?.name, "Bernardo")
    }

    func testGetLigainsiderPlayerMatchesWhenNamesSwapped() async throws {
        let session = makeSession()
        let service = LigainsiderService(session: session)

        let html = """
                <div>
                    <img src=\"https://www.ligainsider.de/player/team/silva-bernardo.jpg\" />
                </div>
                <a href=\"/silva-bernardo_99999\">Bernardo Silva</a>
            """.data(using: .utf8)!

        URLProtocolMock.requestHandler = { request in
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (resp, html)
        }

        await service.fetchAllSquadsAsync()
        let p = service.getLigainsiderPlayer(firstName: "Bernardo", lastName: "Silva")
        XCTAssertNotNil(p)
        XCTAssertTrue(p?.ligainsiderId?.contains("bernardo") ?? false)
    }

    func testGetPlayerStatusHandlesFirstNameFallbackAndSwappedNames() async throws {
        let session = makeSession()
        let service = LigainsiderService(session: session)

        // Case 1: Kickbase only has first name, Ligainsider has bernardo in squad (not start)
        var html = """
                <div>
                    <img src=\"https://www.ligainsider.de/player/team/bernardo.jpg\" />
                </div>
                <a href=\"/bernardo_12345\">Bernardo</a>
            """.data(using: .utf8)!

        URLProtocolMock.requestHandler = { request in
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (resp, html)
        }

        await service.fetchAllSquadsAsync()

        let status1 = service.getPlayerStatus(firstName: "Bernardo", lastName: "")
        XCTAssertEqual(status1, .bench)

        // Case 2: Ligainsider lists 'Bernardo Silva' in starting lineup -> should be likelyStart
        html = """
                <html>
                    <body>
                        <h2 itemprop=\"name\">TeamName</h2>
                        VORAUSSICHTLICHE AUFSTELLUNG
                        <a href=\"/silva-bernardo_99999\">Bernardo Silva</a>
                    </body>
                </html>
            """.data(using: .utf8)!

        URLProtocolMock.requestHandler = { request in
            let urlStr = request.url!.absoluteString
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            if urlStr.contains("spieltage") {
                // Overview page: return a page that includes team links
                let overviewHTML = """
                        <html>
                            <body>
                                <a href=\"/bundesliga/team/team1/saison-2025\">Team 1</a>
                            </body>
                        </html>
                    """.data(using: .utf8)!
                return (resp, overviewHTML)
            }
            return (resp, html)
        }

        // Re-fetch lineups which will pick up the starting lineup entry
        await service.fetchLineupsAsync()
        let status2 = service.getPlayerStatus(firstName: "Bernardo", lastName: "Silva")
        XCTAssertEqual(status2, .likelyStart)
    }

    func testFetchAllSquadsHandlesDataSrcAndSrcset() async throws {
        let session = makeSession()
        let service = LigainsiderService(session: session)

        let html = """
                <div>
                    <img class="player_img lazy" data-src=\"https://www.ligainsider.de/player/team/lazy-player.jpg\" />
                    <img class="small_inner_icon" src=\"https://www.ligainsider.de/images/nations/de.png\" />
                </div>
                <a href=\"/john-doe_12345\">John Doe</a>
            """.data(using: .utf8)!

        URLProtocolMock.requestHandler = { request in
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (resp, html)
        }

        await service.fetchAllSquadsAsync()
        let p = service.getLigainsiderPlayer(firstName: "John", lastName: "Doe")
        XCTAssertNotNil(p)
        XCTAssertEqual(p?.imageUrl, "https://www.ligainsider.de/player/team/lazy-player.jpg")
    }

    func testFetchAllSquadsDoesNotConstructImageWhenNoImageFound() async throws {
        let session = makeSession()
        let service = LigainsiderService(session: session)

        let html = """
                <div></div>
                <a href=\"/john-doe_12345\">John Doe</a>
            """.data(using: .utf8)!

        URLProtocolMock.requestHandler = { request in
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (resp, html)
        }

        XCTAssertEqual(service.playerCacheCount, 0)
        await service.fetchAllSquadsAsync()
        let p = service.getLigainsiderPlayer(firstName: "John", lastName: "Doe")
        XCTAssertNotNil(p)
        // No image should be constructed from slug; if the squad page has no image, imageUrl should remain nil
        XCTAssertNil(p?.imageUrl)
    }

    func testFetchAllSquadsCountsOnlyFlagsWhenNoPlayerImage() async throws {
        let session = makeSession()
        let service = LigainsiderService(session: session)

        let html = """
                <div>
                    <img src=\"https://www.ligainsider.de/images/teams/wappen-bayern.png\" />
                    <img class=\"small_inner_icon\" src=\"https://www.ligainsider.de/images/nations/de.png\" />
                </div>
                <a href=\"/jane-doe_54321\">Jane Doe</a>
            """.data(using: .utf8)!

        URLProtocolMock.requestHandler = { request in
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            return (resp, html)
        }

        XCTAssertEqual(service.playerCacheCount, 0)
        await service.fetchAllSquadsAsync()
        let p = service.getLigainsiderPlayer(firstName: "Jane", lastName: "Doe")
        XCTAssertNotNil(p)
        XCTAssertNil(p?.imageUrl)
        XCTAssertTrue(
            service.onlyFlagsCount > 0,
            "Expected onlyFlagsCount to be incremented when only flags/wappen found")
    }

    func testSquadImageOverridesLineupImage() async throws {
        let session = makeSession()
        let service = LigainsiderService(session: session)

        // Team lineup page contains a player image A
        let lineupHtml = """
                <html>
                    <body>
                        VORAUSSICHTLICHE AUFSTELLUNG
                        <div>
                            <img class=\"player_img\" src=\"https://www.ligainsider.de/player/team/lineup-player.jpg\" />
                        </div>
                        <a href=\"/john-doe_12345\">John Doe</a>
                    </body>
                </html>
            """.data(using: .utf8)!

        // Squad page contains a different image B which should take precedence
        let squadHtml = """
                <div>
                    <img src=\"https://www.ligainsider.de/player/team/squad-player.jpg\" />
                </div>
                <a href=\"/john-doe_12345\">John Doe</a>
            """.data(using: .utf8)!

        URLProtocolMock.requestHandler = { request in
            let urlStr = request.url!.absoluteString
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            if urlStr.contains("spieltage") {
                // Overview returns a team link so that fetchLineupsAsync will call fetchTeamData
                let overviewHTML = """
                        <html>
                            <body>
                                <a href=\"/bundesliga/team/team1/saison-2025\">Team 1</a>
                            </body>
                        </html>
                    """.data(using: .utf8)!
                return (resp, overviewHTML)
            }
            if urlStr.contains("team1") {
                // Return lineup html for team page
                return (resp, lineupHtml)
            }
            // For squad fetches (called by fetchTeamData later) return squad html
            return (resp, squadHtml)
        }

        await service.fetchLineupsAsync()
        // The player should exist in cache and the image should be taken from the squad (squad-player.jpg)
        let p = service.getLigainsiderPlayer(firstName: "John", lastName: "Doe")
        XCTAssertNotNil(p)
        XCTAssertEqual(p?.imageUrl, "https://www.ligainsider.de/player/team/squad-player.jpg")
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

    func testMultipleBenchPlayersGetDistinctImages() async throws {
        let session = makeSession()
        let service = LigainsiderService(session: session)

        // Team page with two bench players, each with their own image
        let teamHtml = """
                <html>
                    <body>
                        <div class=\"bench\">
                            <img src=\"https://www.ligainsider.de/player/team/bench-player-a.jpg\" />
                            <a href=\"/bench-player-a_111\">Bench A</a>
                        </div>
                        <div class=\"bench\">
                            <img src=\"https://www.ligainsider.de/player/team/bench-player-b.jpg\" />
                            <a href=\"/bench-player-b_112\">Bench B</a>
                        </div>
                    </body>
                </html>
            """.data(using: .utf8)!

        URLProtocolMock.requestHandler = { request in
            let urlStr = request.url!.absoluteString
            let resp = HTTPURLResponse(
                url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            if urlStr.contains("spieltage") {
                let overviewHTML = """
                        <html>
                            <body>
                                <a href=\"/bundesliga/team/team1/saison-2025\">Team 1</a>
                            </body>
                        </html>
                    """.data(using: .utf8)!
                return (resp, overviewHTML)
            }
            return (resp, teamHtml)
        }

        // Ensure cache empty and trigger background refresh
        XCTAssertEqual(service.playerCacheCount, 0)
        await service.fetchLineupsAsync()

        let a = service.getLigainsiderPlayer(firstName: "Bench", lastName: "A")
        let b = service.getLigainsiderPlayer(firstName: "Bench", lastName: "B")

        XCTAssertNotNil(a)
        XCTAssertNotNil(b)
        XCTAssertEqual(a?.imageUrl, "https://www.ligainsider.de/player/team/bench-player-a.jpg")
        XCTAssertEqual(b?.imageUrl, "https://www.ligainsider.de/player/team/bench-player-b.jpg")
    }
}
