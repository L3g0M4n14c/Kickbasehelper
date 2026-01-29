import SwiftUI
import ViewInspector
import XCTest

@testable import KickbaseCore

// Make tested SwiftUI views inspectable
extension MainDashboardView: Inspectable {}
extension SalesRecommendationView: Inspectable {}
extension SalesRecommendationSummary: Inspectable {}
extension SalesRecommendationRow: Inspectable {}
extension PlayerDetailView: Inspectable {}
extension PlayerHeroHeader: Inspectable {}
extension LineupComparisonView: Inspectable {}
extension LineupDetailView: Inspectable {}
extension LineupSlotRowView: Inspectable {}

#if canImport(ViewInspector)
    // Make our InspectableSheet act as a PopupPresenter so ViewInspector can inspect the sheet content
    // Provide a minimal compatibility shim for the newer ViewInspector PopupPresenter requirements
    extension InspectableSheet: PopupPresenter {
        public typealias Popup = AnyView
        public var isPresented: Binding<Bool> { Binding.constant(false) }
        public var popupBuilder: () -> AnyView { { AnyView(EmptyView()) } }
        public var onDismiss: (() -> Void)? { nil }
    }
#endif

@MainActor
final class ViewInspectorTests: XCTestCase {
    func test_mainDashboard_showsLoadingWhenLigainsiderNotReady() throws {
        let ligainsider = LigainsiderService()
        // Ensure not ready
        ligainsider.isLigainsiderReady = false

        let manager = KickbaseManager()
        let auth = AuthenticationManager()

        let view = MainDashboardView()
            .environmentObject(manager)
            .environmentObject(auth)
            .environmentObject(ligainsider)

        // Should contain the loading text
        XCTAssertNoThrow(try view.inspect().find(text: "Ligainsider-Daten werden geladen..."))
    }

    func test_salesRecommendationSummary_showsStrategyAndCounts() throws {
        let player = TeamPlayer(
            id: "p1", firstName: "Max", lastName: "Muster", profileBigUrl: "", teamName: "FC",
            teamId: "1", position: 3, number: 8, averagePoints: 5.0, totalPoints: 100,
            marketValue: 1_000_000, marketValueTrend: 0, tfhmvt: 0, prlo: 0, stl: 0, status: 0,
            userOwnsPlayer: true)
        let recs: [SalesRecommendation] = [
            SalesRecommendation(
                player: player, reason: "Zu teuer", priority: .high, expectedValue: 100_000,
                impact: .moderate)
        ]
        let goal: SalesRecommendationView.OptimizationGoal = .balancePositive

        let summary = SalesRecommendationSummary(recommendations: recs, optimizationGoal: goal)

        let v = try summary.inspect()
        XCTAssertEqual(try v.vStack().text(0).string(), "Empfehlungsübersicht")
        XCTAssertNoThrow(try v.find(text: "Strategie: \(goal.rawValue)"))
    }

    func test_playerDetailView_showsPlayerNames() throws {
        let player = TeamPlayer(
            id: "p1",
            firstName: "Max",
            lastName: "Mustermann",
            profileBigUrl: "",
            teamName: "FC Test",
            teamId: "1",
            position: 3,
            number: 10,
            averagePoints: 6.3,
            totalPoints: 200,
            marketValue: 2_000_000,
            marketValueTrend: 0,
            tfhmvt: 0,
            prlo: 0,
            stl: 0,
            status: 0,
            userOwnsPlayer: true
        )

        let ligainsider = LigainsiderService()
        let manager = KickbaseManager()

        let view = PlayerDetailView(player: player)
            .environmentObject(manager)
            .environmentObject(ligainsider)

        // Verify hero header shows first and last name
        let hero = try view.inspect().navigationStack().scrollView().vStack().view(
            PlayerHeroHeader.self, 0)
        // First inner VStack (index 1) contains another VStack (index 0) with first and last name
        XCTAssertEqual(try hero.vStack().vStack(1).vStack(0).text(0).string(), player.firstName)
        XCTAssertEqual(try hero.vStack().vStack(1).vStack(0).text(1).string(), player.lastName)
    }

    func test_lineupComparisonView_showsFormationAndScores() throws {
        // create a sample slot and lineup
        let slot = LineupSlot(
            slotIndex: 0, positionType: 3, ownedPlayerId: "p1", recommendedMarketPlayerId: nil,
            slotScore: 12.5)
        let lineup = OptimalLineupResult(
            slots: [slot], formationName: "4-4-2", totalLineupScore: 55.0,
            isHybridWithMarketPlayers: false, averagePlayerScore: 11.0)
        let comparison = LineupComparison(teamOnlyLineup: lineup)

        let manager = KickbaseManager()
        // give manager a team player to match slot ownedPlayerId
        let player = TeamPlayer(
            id: "p1", firstName: "Max", lastName: "Muster", profileBigUrl: "", teamName: "FC",
            teamId: "1", position: 3, number: 8, averagePoints: 5.0, totalPoints: 100,
            marketValue: 1_000_000, marketValueTrend: 0, tfhmvt: 0, prlo: 0, stl: 0, status: 0,
            userOwnsPlayer: true)
        manager.teamPlayers = [player]

        let view = LineupComparisonView(comparison: comparison)
            .environmentObject(manager)

        // Find the embedded LineupDetailView and inspect its header
        let detail = try view.inspect().find(LineupDetailView.self)
        XCTAssertEqual(try detail.vStack().vStack(0).text(0).string(), lineup.formationName)
        XCTAssertNoThrow(try detail.find(text: String(format: "%.1f", lineup.totalLineupScore)))

    }

    func test_lineupComparison_picker_showsOptions() throws {
        // Setup team-only and hybrid lineups
        let slot = LineupSlot(
            slotIndex: 0, positionType: 3, ownedPlayerId: "p1", recommendedMarketPlayerId: "m1",
            slotScore: 12.5)
        let teamLineup = OptimalLineupResult(
            slots: [slot], formationName: "4-4-2", totalLineupScore: 55.0,
            isHybridWithMarketPlayers: false, averagePlayerScore: 11.0)
        let hybridLineup = OptimalLineupResult(
            slots: [slot], formationName: "3-5-2", totalLineupScore: 65.0,
            isHybridWithMarketPlayers: true, marketPlayersNeeded: ["m1"],
            totalMarketCost: 1_500_000, averagePlayerScore: 13.0)
        let comparison = LineupComparison(teamOnlyLineup: teamLineup, hybridLineup: hybridLineup)

        let manager = KickbaseManager()
        manager.teamPlayers = [
            TeamPlayer(
                id: "p1", firstName: "Max", lastName: "Muster", profileBigUrl: "", teamName: "FC",
                teamId: "1", position: 3, number: 8, averagePoints: 5.0, totalPoints: 100,
                marketValue: 1_000_000, marketValueTrend: 0, tfhmvt: 0, prlo: 0, stl: 0, status: 0,
                userOwnsPlayer: true)
        ]
        manager.marketPlayers = [
            MarketPlayer(
                id: "m1", firstName: "Mark", lastName: "Smith", profileBigUrl: "", teamName: "Club",
                teamId: "1", position: 3, number: 8, averagePoints: 6.0, totalPoints: 100,
                marketValue: 1_200_000, marketValueTrend: 0, price: 1_500_000, expiry: "",
                offers: 0, seller: MarketSeller(id: "s", name: "S"), stl: 0, status: 0, prlo: 0,
                owner: nil, exs: 0)
        ]

        let view = LineupComparisonView(comparison: comparison)
            .environmentObject(manager)
            .environmentObject(LigainsiderService())

        // Picker exists and has correct labels
        let picker = try view.inspect().scrollView().vStack().picker(0)
        XCTAssertEqual(try picker.text(0).string(), "Nur eigene Spieler")
        XCTAssertEqual(try picker.text(1).string(), "Mit Marktspieler")
        // Initially should be showing team-only
        XCTAssertTrue(try picker.selectedValue(Bool.self))
    }

    func test_hybridLineupSummary_showsWhenHybridAvailable() throws {
        let slot = LineupSlot(
            slotIndex: 0, positionType: 3, ownedPlayerId: "p1", recommendedMarketPlayerId: "m1",
            slotScore: 12.5)
        let hybridLineup = OptimalLineupResult(
            slots: [slot], formationName: "3-5-2", totalLineupScore: 65.0,
            isHybridWithMarketPlayers: true, marketPlayersNeeded: ["m1"],
            totalMarketCost: 1_500_000, averagePlayerScore: 13.0)
        let comparison = LineupComparison(teamOnlyLineup: hybridLineup, hybridLineup: hybridLineup)

        let summary = HybridLineupSummary(
            comparison: comparison,
            teamPlayers: [
                TeamPlayer(
                    id: "p1", firstName: "Max", lastName: "Muster", profileBigUrl: "",
                    teamName: "FC", teamId: "1", position: 3, number: 8, averagePoints: 5.0,
                    totalPoints: 100, marketValue: 1_000_000, marketValueTrend: 0, tfhmvt: 0,
                    prlo: 0, stl: 0, status: 0, userOwnsPlayer: true)
            ],
            marketPlayers: [
                MarketPlayer(
                    id: "m1", firstName: "Mark", lastName: "Smith", profileBigUrl: "",
                    teamName: "Club", teamId: "1", position: 3, number: 8, averagePoints: 6.0,
                    totalPoints: 100, marketValue: 1_200_000, marketValueTrend: 0, price: 1_500_000,
                    expiry: "", offers: 0, seller: MarketSeller(id: "s", name: "S"), stl: 0,
                    status: 0, prlo: 0, owner: nil, exs: 0)
            ])

        let v = try summary.inspect().view(HybridLineupSummary.self).anyView()
        XCTAssertEqual(try v.vStack().text(0).string(), "Hybrid-Aufstellung Übersicht")
    }

    func test_salesRecommendation_toggle_callsOnToggle() throws {
        let player = TeamPlayer(
            id: "p1", firstName: "Max", lastName: "Muster", profileBigUrl: "", teamName: "FC Test",
            teamId: "1", position: 3, number: 10, averagePoints: 6.3, totalPoints: 200,
            marketValue: 2_000_000, marketValueTrend: 0, tfhmvt: 0, prlo: 0, stl: 0, status: 0,
            userOwnsPlayer: true)
        let recommendation = SalesRecommendation(
            player: player, reason: "Grund", priority: .medium, expectedValue: 50_000,
            impact: .minimal)

        var toggled = false
        let row = SalesRecommendationRow(recommendation: recommendation, isSelected: false) {
            newValue in
            toggled = newValue
        }
        .environmentObject(KickbaseManager())
        .environmentObject(LigainsiderService())

        // Toggle the switch
        try row.inspect().hStack().toggle(2).tap()
        XCTAssertTrue(toggled)
    }
}
