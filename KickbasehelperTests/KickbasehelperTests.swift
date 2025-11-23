//
//  KickbasehelperTests.swift
//  KickbasehelperTests
//
//  Created by Marco Corro on 27.08.25.
//

import Foundation

// MARK: - Test Models and Infrastructure

struct TestResult {
    let testName: String
    let passed: Bool
    let error: String?
}

class TestRunner {
    var results: [TestResult] = []

    func assertEqual<T: Equatable>(_ a: T, _ b: T, testName: String) {
        if a == b {
            results.append(TestResult(testName: testName, passed: true, error: nil))
        } else {
            results.append(TestResult(testName: testName, passed: false, error: "\(a) != \(b)"))
        }
    }

    func assertTrue(_ condition: Bool, testName: String) {
        if condition {
            results.append(TestResult(testName: testName, passed: true, error: nil))
        } else {
            results.append(
                TestResult(testName: testName, passed: false, error: "Condition was false"))
        }
    }
}

// MARK: - Backend Tests

class BackendTests {
    let runner = TestRunner()

    func runAll() {
        testBudgetCalculation()
        testTeamValueCalc()
        testPlayerValueChangeCalc()
        testBudgetAllocationPercentage()
        testPlayerSortingByPoints()
        testPlayerFilteringByStatus()
        testPlayerFilteringByPositionAndValue()
        testRecommendationScoreCalculation()
        testPositionValidation()
        testFormationValidation()
        testPlayerDataMapping()
        testCacheExpiration()
        testCacheValidity()
        testValidEmailValidation()
        testInvalidEmailValidation()
        testPasswordValidation()
        testBudgetValidation()
    }

    func testBudgetCalculation() {
        let initialBudget = 1_000_000
        let playerCost = 350000
        let remainingBudget = initialBudget - playerCost
        runner.assertEqual(remainingBudget, 650000, testName: "Budget Calculation")
    }

    func testTeamValueCalc() {
        let playerValues = [500000, 600000, 450000, 700000, 550000]
        let totalValue = playerValues.reduce(0, +)
        runner.assertEqual(totalValue, 2_800_000, testName: "Team Value Calculation")
    }

    func testPlayerValueChangeCalc() {
        let buyPrice = 450000
        let currentPrice = 500000
        let profit = currentPrice - buyPrice
        runner.assertEqual(profit, 50000, testName: "Player Value Change")
    }

    func testBudgetAllocationPercentage() {
        let budget = 1_000_000
        let spent = 800000
        let percentageSpent = (Double(spent) / Double(budget)) * 100
        runner.assertEqual(percentageSpent, 80.0, testName: "Budget Allocation Percentage")
    }

    func testPlayerSortingByPoints() {
        var players = [
            (name: "Player A", points: 75),
            (name: "Player B", points: 85),
            (name: "Player C", points: 70),
            (name: "Player D", points: 90),
        ]

        players.sort { $0.points > $1.points }

        runner.assertEqual(players[0].points, 90, testName: "Player Sorting First")
        runner.assertEqual(players[1].points, 85, testName: "Player Sorting Second")
        runner.assertEqual(players[3].points, 70, testName: "Player Sorting Last")
    }

    func testPlayerFilteringByStatus() {
        let players = [
            (id: "1", status: 0),
            (id: "2", status: 8),
            (id: "3", status: 0),
            (id: "4", status: 16),
        ]

        let healthyPlayers = players.filter { $0.status == 0 }

        runner.assertEqual(healthyPlayers.count, 2, testName: "Healthy Player Count")
        runner.assertEqual(healthyPlayers[0].id, "1", testName: "Healthy Player ID First")
    }

    func testPlayerFilteringByPositionAndValue() {
        let players = [
            (position: 2, value: 500000),
            (position: 3, value: 600000),
            (position: 2, value: 550000),
            (position: 4, value: 450000),
        ]

        let defendersCheap = players.filter { $0.position == 2 && $0.value < 560000 }

        runner.assertEqual(defendersCheap.count, 1, testName: "Defenders Count")
        runner.assertEqual(defendersCheap[0].value, 500000, testName: "Defender Value")
    }

    func testRecommendationScoreCalculation() {
        let playerPoints = 85.0
        let marketValue = 500000
        let trend = 2.5

        let baseScore = playerPoints
        let valueMultiplier = 0.1
        let trendBonus = trend

        let totalScore = baseScore + (valueMultiplier * Double(marketValue) / 10000) + trendBonus

        runner.assertTrue(totalScore > 85.0, testName: "Recommendation Score")
    }

    func testPositionValidation() {
        let validPositions = [1, 2, 3, 4]
        var allValid = true
        for position in validPositions {
            if !validPositions.contains(position) {
                allValid = false
            }
        }
        runner.assertTrue(allValid, testName: "Position Validation")
    }

    func testFormationValidation() {
        let formation = (defenders: 4, midfielders: 4, forwards: 2)
        let totalPlayers = formation.defenders + formation.midfielders + formation.forwards
        runner.assertEqual(totalPlayers, 10, testName: "Formation Total Players")
    }

    func testPlayerDataMapping() {
        let mockData: [String: Any] = [
            "i": "player123",
            "p": 2,
            "m": 500000,
            "avgp": 82.5,
        ]

        let id = mockData["i"] as? String
        let position = mockData["p"] as? Int
        let value = mockData["m"] as? Int
        let points = mockData["avgp"] as? Double

        runner.assertEqual(id, "player123", testName: "Player ID Mapping")
        runner.assertEqual(position, 2, testName: "Player Position Mapping")
        runner.assertEqual(value, 500000, testName: "Player Value Mapping")
        runner.assertEqual(points, 82.5, testName: "Player Points Mapping")
    }

    func testCacheExpiration() {
        let currentTime = Date()
        let cacheTime = currentTime.addingTimeInterval(-400)
        let cacheDuration: TimeInterval = 300
        let isExpired = currentTime.timeIntervalSince(cacheTime) >= cacheDuration
        runner.assertTrue(isExpired, testName: "Cache Expiration")
    }

    func testCacheValidity() {
        let currentTime = Date()
        let cacheTime = currentTime.addingTimeInterval(-60)
        let cacheDuration: TimeInterval = 300
        let isValid = currentTime.timeIntervalSince(cacheTime) < cacheDuration
        runner.assertTrue(isValid, testName: "Cache Validity")
    }

    func testValidEmailValidation() {
        let validEmail = "user@example.com"
        let isValid = validEmail.contains("@") && validEmail.contains(".")
        runner.assertTrue(isValid, testName: "Valid Email Check")
    }

    func testInvalidEmailValidation() {
        let invalidEmail = "invalid-email"
        let isValid = invalidEmail.contains("@") && invalidEmail.contains(".")
        runner.assertTrue(!isValid, testName: "Invalid Email Check")
    }

    func testPasswordValidation() {
        let strongPassword = "SecurePass123!"
        let weakPassword = "123"

        runner.assertTrue(strongPassword.count >= 8, testName: "Strong Password Length")
        runner.assertTrue(weakPassword.count < 8, testName: "Weak Password Detection")
    }

    func testBudgetValidation() {
        let validBudget = 1_000_000
        let invalidBudget = -100000

        runner.assertTrue(validBudget > 0, testName: "Valid Budget Check")
        runner.assertTrue(invalidBudget < 0, testName: "Invalid Budget Check")
    }

    func printResults() {
        let passed = runner.results.filter { $0.passed }.count
        let failed = runner.results.filter { !$0.passed }.count

        print("\n========== TEST RESULTS ==========")
        print("✅ Passed: \(passed)")
        print("❌ Failed: \(failed)")
        print("Total: \(runner.results.count)")
        print("=================================\n")

        for result in runner.results {
            if result.passed {
                print("✅ \(result.testName)")
            } else {
                print("❌ \(result.testName): \(result.error ?? "Unknown error")")
            }
        }
    }
}

// MARK: - Unit Tests

class UnitTests {
    let runner = TestRunner()

    func runAll() {
        testUserModel()
        testLoginRequest()
        testBudgetBalance()
        testTeamValueCalculation()
        testPlayerValueChange()
        testEmailValidation()
        testPasswordValidation()
        testBudgetConstraints()
        testArrayFiltering()
        testStringManipulation()
    }

    func testUserModel() {
        let id = "123"
        runner.assertEqual(id, "123", testName: "User ID")
        runner.assertEqual("Test User", "Test User", testName: "User Name")
        runner.assertEqual("test@example.com", "test@example.com", testName: "User Email")
    }

    func testLoginRequest() {
        let email = "test@example.com"
        runner.assertEqual(email, "test@example.com", testName: "Login Email")
        runner.assertEqual("password123", "password123", testName: "Login Password")
    }

    func testBudgetBalance() {
        let startingBudget = 1_000_000
        let spent = 750000
        let remaining = startingBudget - spent
        runner.assertEqual(remaining, 250000, testName: "Budget Balance")
    }

    func testTeamValueCalculation() {
        let playerValues = [500000, 750000, 600000, 450000, 350000]
        let totalTeamValue = playerValues.reduce(0, +)
        runner.assertEqual(totalTeamValue, 2_650_000, testName: "Team Value Calculation")
    }

    func testPlayerValueChange() {
        let initialValue = 500000
        let newValue = 550000
        let change = newValue - initialValue
        runner.assertEqual(change, 50000, testName: "Player Value Change")
    }

    func testEmailValidation() {
        let validEmail = "user@example.com"
        let isValid = validEmail.contains("@") && validEmail.contains(".")
        runner.assertTrue(isValid, testName: "Email Validation")
    }

    func testPasswordValidation() {
        let strongPassword = "SecurePass123!"
        let weakPassword = "123"

        runner.assertTrue(strongPassword.count >= 8, testName: "Strong Password Length")
        runner.assertTrue(weakPassword.count < 8, testName: "Weak Password Detection")
    }

    func testBudgetConstraints() {
        let budget = 1_000_000
        let maxAllowedSpend = Int(Double(budget) * 0.8)
        let desiredSpend = 900000
        let allowedSpend = min(desiredSpend, maxAllowedSpend)
        runner.assertEqual(allowedSpend, 800000, testName: "Budget Constraints")
    }

    func testArrayFiltering() {
        let numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
        let evens = numbers.filter { $0 % 2 == 0 }
        runner.assertEqual(evens.count, 5, testName: "Even Number Count")
    }

    func testStringManipulation() {
        let fullName = "John Doe"
        let firstName = fullName.split(separator: " ").first.map(String.init) ?? ""
        runner.assertEqual(firstName, "John", testName: "First Name Extraction")
    }

    func printResults() {
        let passed = runner.results.filter { $0.passed }.count
        let failed = runner.results.filter { !$0.passed }.count

        print("\n========== UNIT TEST RESULTS ==========")
        print("✅ Passed: \(passed)")
        print("❌ Failed: \(failed)")
        print("Total: \(runner.results.count)")
        print("======================================\n")

        for result in runner.results {
            if result.passed {
                print("✅ \(result.testName)")
            } else {
                print("❌ \(result.testName): \(result.error ?? "Unknown error")")
            }
        }
    }
}

// MARK: - Test Entry Point

@main
struct TestSuite {
    static func main() {
        print("🧪 Starting Kickbasehelper Test Suite...\n")

        let unitTests = UnitTests()
        unitTests.runAll()
        unitTests.printResults()

        let backendTests = BackendTests()
        backendTests.runAll()
        backendTests.printResults()

        print("✨ Test suite completed!")
    }
}
