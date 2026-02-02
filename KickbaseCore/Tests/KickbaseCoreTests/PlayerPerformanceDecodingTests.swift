import XCTest

@testable import KickbaseCore

@MainActor
final class PlayerPerformanceDecodingTests: XCTestCase {
    func testMatchPerformanceDecodesWhenMissingAP() throws {
        // JSON without 'ap' field
        let json = """
            {
                "it": [
                    { "ti": "2025", "n": "TestLeague", "ph": [ { "day": 1, "p": 5, "mp": "90'", "md": "2025-01-01", "t1": "t1", "t2": "t2", "st": 5, "cur": false, "mdst": 0, "tp": 5 } ] }
                ]
            }
            """.data(using: .utf8)!

        let decoder = JSONDecoder()
        let resp = try decoder.decode(PlayerPerformanceResponse.self, from: json)
        XCTAssertEqual(resp.it.count, 1)
        let match = resp.it.first!.ph.first!
        XCTAssertEqual(match.averagePoints, 0)  // ap missing -> default 0
        XCTAssertEqual(match.totalPoints, 5)
    }

    func testPlayerPerformanceDecodingResilientToMissingFields() throws {
        // JSON missing multiple optional fields
        let json = """
            {
                "it": [
                    { "ti": "2025", "n": "TestLeague", "ph": [ { "day": 2, "md": "2025-01-02", "t1": "t1", "t2": "t2", "st": 0, "cur": false, "mdst": 0 } ] }
                ]
            }
            """.data(using: .utf8)!

        let decoder = JSONDecoder()
        let resp = try decoder.decode(PlayerPerformanceResponse.self, from: json)
        XCTAssertEqual(resp.it.count, 1)
        let match = resp.it.first!.ph.first!
        XCTAssertEqual(match.averagePoints, 0)
        XCTAssertEqual(match.totalPoints, 0)
    }
}
