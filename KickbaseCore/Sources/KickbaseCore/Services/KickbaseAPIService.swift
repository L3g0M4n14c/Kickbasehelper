import Combine
import SwiftUI
import Foundation

/// Hauptservice für alle Kickbase API v4 Endpoints basierend auf der Swagger-Dokumentation
/// Quelle: https://github.com/kevinskyba/kickbase-api-doc
@MainActor
public class KickbaseAPIService: ObservableObject {
    private let baseURL = "https://api.kickbase.com"
    private var authToken: String?

    public init() {}

    // MARK: - Authentication

    public func setAuthToken(_ token: String) {
        authToken = token
        print("🔑 Auth token set for KickbaseAPIService")
    }

    public func hasAuthToken() -> Bool {
        return authToken != nil
    }

    // MARK: - Generic Request Methods

    private func makeRequest(
        endpoint: String,
        method: String = "GET",
        body: Data? = nil,
        requiresAuth: Bool = true
    ) async throws -> (Data, HTTPURLResponse) {
        if requiresAuth {
            guard let token = authToken else {
                throw APIError.noAuthToken
            }
        }

        guard let url = URL(string: "\(baseURL)\(endpoint)") else {
            throw APIError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.cachePolicy = .reloadIgnoringLocalCacheData

        if requiresAuth, let token = authToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let body = body {
            request.httpBody = body
        }

        print("📤 \(method) \(endpoint)")

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.noHTTPResponse
        }

        print("📊 Response: \(httpResponse.statusCode)")

        guard (200...299).contains(httpResponse.statusCode) else {
            if httpResponse.statusCode == 401 {
                throw APIError.authenticationFailed
            }
            throw APIError.networkError("HTTP \(httpResponse.statusCode)")
        }

        return (data, httpResponse)
    }

    // MARK: - User Endpoints

    /// POST /v4/user/login - User Login
    public func login(email: String, password: String) async throws -> LoginResponse {
        let loginRequest = LoginRequest(email: email, password: password)
        let encoder = JSONEncoder()
        let body = try encoder.encode(loginRequest)

        guard let url = URL(string: "\(baseURL)/v4/user/login") else {
            throw NSError(domain: "InvalidURL", code: -1)
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.cachePolicy = .reloadIgnoringLocalCacheData
        request.httpBody = body

        print("📤 POST /v4/user/login")

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NSError(domain: "NoHTTPResponse", code: -1)
        }

        print("📊 Response: \(httpResponse.statusCode)")

        // Debug: Log die rohe Response
        if let jsonString = String(data: data, encoding: .utf8) {
            print("📥 Login Response: \(jsonString)")
        }

        // Spezifische Fehlerbehandlung für Login
        if httpResponse.statusCode == 401 {
            print("❌ HTTP 401: Invalid credentials")
            throw NSError(
                domain: "InvalidCredentials", code: 401,
                userInfo: [
                    NSLocalizedDescriptionKey:
                        "Ungültige E-Mail oder Passwort. Bitte überprüfen Sie Ihre Anmeldedaten."
                ])
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            print("❌ HTTP \(httpResponse.statusCode)")
            throw NSError(
                domain: "HTTPError", code: httpResponse.statusCode,
                userInfo: [
                    NSLocalizedDescriptionKey:
                        "Anmeldung fehlgeschlagen. Server antwortet mit HTTP \(httpResponse.statusCode). Bitte versuchen Sie es später erneut."
                ])
        }

        let decoder = JSONDecoder()
        do {
            return try decoder.decode(LoginResponse.self, from: data)
        } catch {
            print("❌ Decode error: \(error)")
            throw NSError(
                domain: "DecodingError", code: -1,
                userInfo: [
                    NSLocalizedDescriptionKey:
                        "Unerwartete Serverantwort. Bitte versuchen Sie es erneut."
                ])
        }
    }

    /// GET /v4/user/settings - Account Settings
    public func getUserSettings() async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/user/settings")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    // MARK: - Base Endpoints

    /// GET /v4/base/overview - Base Overview
    public func getBaseOverview() async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/base/overview")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    // MARK: - Bonus Endpoints

    /// GET /v4/bonus/collect - Bonus Collection
    public func collectBonus() async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/bonus/collect")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    // MARK: - League Endpoints

    /// GET /v4/leagues/selection - List all Leagues
    public func getLeagueSelection() async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/leagues/selection")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/overview - League Overview
    public func getLeagueOverview(leagueId: String, matchDay: Int? = nil) async throws -> [String: Any] {
        var endpoint = "/v4/leagues/\(leagueId)/overview"
        if let matchDay = matchDay {
            endpoint += "?matchDay=\(matchDay)"
        }
        let (data, _) = try await makeRequest(endpoint: endpoint)
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/me - My Stats in League
    public func getLeagueMe(leagueId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/leagues/\(leagueId)/me")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/me/budget - My Budget
    public func getMyBudget(leagueId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/leagues/\(leagueId)/me/budget")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/squad - My Squad/Team Players
    public func getMySquad(leagueId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/leagues/\(leagueId)/squad")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/lineup - My Lineup
    public func getMyLineup(leagueId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/leagues/\(leagueId)/lineup")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// POST /v4/leagues/{leagueId}/lineup - Update My Lineup
    public func updateMyLineup(leagueId: String, lineup: [Int]) async throws -> [String: Any] {
        let body = try JSONSerialization.data(withJSONObject: lineup)
        let (data, _) = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/lineup",
            method: "POST",
            body: body
        )
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/teamcenter/myeleven - My Eleven
    public func getMyEleven(leagueId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/teamcenter/myeleven")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/ranking - League Ranking
    public func getLeagueRanking(leagueId: String, matchDay: Int? = nil) async throws -> [String: Any] {
        var endpoint = "/v4/leagues/\(leagueId)/ranking"
        if let matchDay = matchDay {
            endpoint += "?matchDay=\(matchDay)"
        }
        let (data, _) = try await makeRequest(endpoint: endpoint)
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/settings - League Settings (Admin Only)
    public func getLeagueSettings(leagueId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/leagues/\(leagueId)/settings")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/settings/managers - League Members (Admin Only)
    public func getLeagueManagers(leagueId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/leagues/\(leagueId)/settings/managers")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    // MARK: - Manager Endpoints

    /// GET /v4/leagues/{leagueId}/managers/{userId}/dashboard - Manager Profile
    public func getManagerDashboard(leagueId: String, userId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/managers/\(userId)/dashboard")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/managers/{userId}/performance - Manager Performance
    public func getManagerPerformance(leagueId: String, userId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/managers/\(userId)/performance")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/managers/{userId}/squad - Manager Squad Details
    public func getManagerSquad(leagueId: String, userId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/managers/\(userId)/squad")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/managers/{userId}/transfer - Manager Transfers
    public func getManagerTransfers(leagueId: String, userId: String, matchDay: Int? = nil) async throws
        -> [String: Any]
    {
        var endpoint = "/v4/leagues/\(leagueId)/managers/\(userId)/transfer"
        if let matchDay = matchDay {
            endpoint += "?matchDay=\(matchDay)"
        }
        let (data, _) = try await makeRequest(endpoint: endpoint)
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/users/{userId}/teamcenter - Team Center
    public func getTeamCenter(leagueId: String, userId: String, matchDay: Int? = nil) async throws
        -> [String: Any]
    {
        var endpoint = "/v4/leagues/\(leagueId)/users/\(userId)/teamcenter"
        if let matchDay = matchDay {
            endpoint += "?matchDay=\(matchDay)"
        }
        let (data, _) = try await makeRequest(endpoint: endpoint)
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    // MARK: - Player Endpoints

    /// GET /v4/leagues/{leagueId}/players/{playerId} - Player Details
    public func getPlayerDetails(leagueId: String, playerId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/players/\(playerId)")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/players/{playerId}/performance - Player Performance
    public func getPlayerPerformance(leagueId: String, playerId: String) async throws
        -> PlayerPerformanceResponse
    {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/players/\(playerId)/performance")
        let decoder = JSONDecoder()
        return try decoder.decode(PlayerPerformanceResponse.self, from: data)
    }

    /// GET /v4/leagues/{leagueId}/players/{playerId}/marketvalue/{timeframe} - Player Market Value History
    public func getPlayerMarketValue(leagueId: String, playerId: String, timeframe: Int = 365) async throws
        -> [String: Any]
    {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/players/\(playerId)/marketvalue/\(timeframe)")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/players/{playerId}/transferHistory - Player Transfer History
    public func getPlayerTransferHistory(leagueId: String, playerId: String, matchDay: Int? = nil)
        async throws -> [String: Any]
    {
        var endpoint = "/v4/leagues/\(leagueId)/players/\(playerId)/transferHistory"
        if let matchDay = matchDay {
            endpoint += "?matchDay=\(matchDay)"
        }
        let (data, _) = try await makeRequest(endpoint: endpoint)
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/players/{playerId}/transfers - Player Transfers
    public func getPlayerTransfers(leagueId: String, playerId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/players/\(playerId)/transfers")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    // MARK: - Market Endpoints

    /// GET /v4/leagues/{leagueId}/market - Get Players On Transfer Market
    public func getMarketPlayers(leagueId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/leagues/\(leagueId)/market")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// POST /v4/leagues/{leagueId}/market - Set Player Transfer Price
    public func setPlayerTransferPrice(leagueId: String, playerId: String, price: Int) async throws
        -> [String: Any]
    {
        let body = try JSONSerialization.data(withJSONObject: [
            "playerId": playerId, "price": price,
        ])
        let (data, _) = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/market",
            method: "POST",
            body: body
        )
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// DELETE /v4/leagues/{leagueId}/market/{playerId} - Remove Player From Market
    public func removePlayerFromMarket(leagueId: String, playerId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/market/\(playerId)",
            method: "DELETE"
        )
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// POST /v4/leagues/{leagueId}/market/{playerId}/offers - Place An Offer
    public func placeOffer(leagueId: String, playerId: String, price: Int) async throws -> [String: Any] {
        let body = try JSONSerialization.data(withJSONObject: ["price": price])
        let (data, _) = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/market/\(playerId)/offers",
            method: "POST",
            body: body
        )
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// DELETE /v4/leagues/{leagueId}/market/{playerId}/offers/{offerId} - Withdraw Offer
    public func withdrawOffer(leagueId: String, playerId: String, offerId: String) async throws -> [String:
        Any]
    {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/market/\(playerId)/offers/\(offerId)",
            method: "DELETE"
        )
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// DELETE /v4/leagues/{leagueId}/market/{playerId}/offers/{offerId}/accept - Accept Manager Offer
    public func acceptOffer(leagueId: String, playerId: String, offerId: String) async throws {
        _ = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/market/\(playerId)/offers/\(offerId)/accept",
            method: "DELETE"
        )
    }

    /// DELETE /v4/leagues/{leagueId}/market/{playerId}/offers/{offerId}/decline - Decline Manager Offer
    public func declineOffer(leagueId: String, playerId: String, offerId: String) async throws {
        _ = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/market/\(playerId)/offers/\(offerId)/decline",
            method: "DELETE"
        )
    }

    /// DELETE /v4/leagues/{leagueId}/market/{playerId}/sell - Accept Kickbase Offer
    public func acceptKickbaseOffer(leagueId: String, playerId: String) async throws {
        _ = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/market/\(playerId)/sell",
            method: "DELETE"
        )
    }

    // MARK: - Scouted Players Endpoints

    /// GET /v4/leagues/{leagueId}/scoutedplayers - Get Scouted Players List
    public func getScoutedPlayers(leagueId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/leagues/\(leagueId)/scoutedplayers")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// POST /v4/leagues/{leagueId}/scoutedplayers/{playerId} - Add Player To Scouted List
    public func addScoutedPlayer(leagueId: String, playerId: String) async throws {
        _ = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/scoutedplayers/\(playerId)",
            method: "POST"
        )
    }

    /// DELETE /v4/leagues/{leagueId}/scoutedplayers/{playerId} - Remove Player From Scouted List
    public func removeScoutedPlayer(leagueId: String, playerId: String) async throws {
        _ = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/scoutedplayers/\(playerId)",
            method: "DELETE"
        )
    }

    /// DELETE /v4/leagues/{leagueId}/scoutedplayers - Clear Scouted Players List
    public func clearScoutedPlayers(leagueId: String) async throws {
        _ = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/scoutedplayers",
            method: "DELETE"
        )
    }

    // MARK: - Activities Feed Endpoints

    /// GET /v4/leagues/{leagueId}/activitiesFeed - Get Activity Feed
    public func getActivitiesFeed(leagueId: String, start: Int? = nil, max: Int? = nil) async throws
        -> [String: Any]
    {
        var endpoint = "/v4/leagues/\(leagueId)/activitiesFeed"
        var params: [String] = []
        if let start = start {
            params.append("start=\(start)")
        }
        if let max = max {
            params.append("max=\(max)")
        }
        if !params.isEmpty {
            endpoint += "?" + params.joined(separator: "&")
        }
        let (data, _) = try await makeRequest(endpoint: endpoint)
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/activitiesFeed/{activityId} - Get Feed Item
    public func getFeedItem(leagueId: String, activityId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/activitiesFeed/\(activityId)")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// POST /v4/leagues/{leagueId}/activitiesFeed/{activityId} - Send Feed Item Comment
    public func sendFeedItemComment(leagueId: String, activityId: String, comment: String) async throws {
        let body = try JSONSerialization.data(withJSONObject: ["comment": comment])
        _ = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/activitiesFeed/\(activityId)",
            method: "POST",
            body: body
        )
    }

    /// GET /v4/leagues/{leagueId}/activitiesFeed/{activityId}/comments - Get Feed Item Comments
    public func getFeedItemComments(
        leagueId: String, activityId: String, start: Int? = nil, max: Int? = nil
    ) async throws -> [String: Any] {
        var endpoint = "/v4/leagues/\(leagueId)/activitiesFeed/\(activityId)/comments"
        var params: [String] = []
        if let start = start {
            params.append("start=\(start)")
        }
        if let max = max {
            params.append("max=\(max)")
        }
        if !params.isEmpty {
            endpoint += "?" + params.joined(separator: "&")
        }
        let (data, _) = try await makeRequest(endpoint: endpoint)
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    // MARK: - Achievements Endpoints

    /// GET /v4/leagues/{leagueId}/user/achievements - Get All Achievements
    public func getUserAchievements(leagueId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/leagues/\(leagueId)/user/achievements")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/user/achievements/{type} - Get Achievements By Type
    public func getUserAchievementsByType(leagueId: String, type: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/user/achievements/\(type)")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/leagues/{leagueId}/battles/{type}/users - Battle By Type
    public func getBattleByType(leagueId: String, type: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/battles/\(type)/users")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    // MARK: - Team Endpoints

    /// GET /v4/leagues/{leagueId}/teams/{teamId}/teamprofile - Team Profile (All Players)
    public func getTeamProfile(leagueId: String, teamId: String) async throws -> TeamProfileResponse {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/leagues/\(leagueId)/teams/\(teamId)/teamprofile")
        let decoder = JSONDecoder()
        return try decoder.decode(TeamProfileResponse.self, from: data)
    }

    // MARK: - Competition Endpoints

    /// GET /v4/competitions/{competitionId}/overview - Competition Details
    public func getCompetitionOverview(competitionId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/competitions/\(competitionId)/overview")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/competitions/{competitionId}/players - Competition Players
    public func getCompetitionPlayers(competitionId: String, matchDay: Int? = nil) async throws -> [String:
        Any]
    {
        var endpoint = "/v4/competitions/\(competitionId)/players"
        if let matchDay = matchDay {
            endpoint += "?matchDay=\(matchDay)"
        }
        let (data, _) = try await makeRequest(endpoint: endpoint)
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/competitions/{competitionId}/players/search - Search Competition Players
    public func searchCompetitionPlayers(
        competitionId: String, query: String, sorting: String? = nil, start: Int? = nil,
        max: Int? = nil
    ) async throws -> [String: Any] {
        var endpoint = "/v4/competitions/\(competitionId)/players/search?query=\(query)"
        if let sorting = sorting {
            endpoint += "&sorting=\(sorting)"
        }
        if let start = start {
            endpoint += "&start=\(start)"
        }
        if let max = max {
            endpoint += "&max=\(max)"
        }
        let (data, _) = try await makeRequest(endpoint: endpoint)
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/competitions/{competitionId}/players/{playerId}/performance - Competition Player Performance
    public func getCompetitionPlayerPerformance(competitionId: String, playerId: String) async throws
        -> [String: Any]
    {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/competitions/\(competitionId)/players/\(playerId)/performance")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/competitions/{competitionId}/players/{playerId}/marketvalue/{timeframe} - Competition Player Market Value
    public func getCompetitionPlayerMarketValue(
        competitionId: String, playerId: String, timeframe: Int = 365
    ) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(
            endpoint:
                "/v4/competitions/\(competitionId)/players/\(playerId)/marketvalue/\(timeframe)")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/competitions/{competitionId}/playercenter/{playerId} - Player Event History
    public func getPlayerEventHistory(competitionId: String, playerId: String, matchDay: Int? = nil)
        async throws -> [String: Any]
    {
        var endpoint = "/v4/competitions/\(competitionId)/playercenter/\(playerId)"
        if let matchDay = matchDay {
            endpoint += "?matchDay=\(matchDay)"
        }
        let (data, _) = try await makeRequest(endpoint: endpoint)
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/competitions/{competitionId}/ranking - Team Ranking
    public func getCompetitionRanking(competitionId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/competitions/\(competitionId)/ranking")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/competitions/{competitionId}/table - Competition Table
    public func getCompetitionTable(competitionId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/competitions/\(competitionId)/table")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/competitions/{competitionId}/matchdays - Fixtures
    public func getCompetitionMatchdays(competitionId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/competitions/\(competitionId)/matchdays")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/competitions/{competitionId}/teams/{teamId}/teamcenter - Matchday Players
    public func getCompetitionTeamCenter(competitionId: String, teamId: String, matchDay: Int? = nil)
        async throws -> [String: Any]
    {
        var endpoint = "/v4/competitions/\(competitionId)/teams/\(teamId)/teamcenter"
        if let matchDay = matchDay {
            endpoint += "?matchDay=\(matchDay)"
        }
        let (data, _) = try await makeRequest(endpoint: endpoint)
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/competitions/{competitionId}/teams/{teamId}/teamprofile - Competition Team Profile
    public func getCompetitionTeamProfile(competitionId: String, teamId: String) async throws -> [String:
        Any]
    {
        let (data, _) = try await makeRequest(
            endpoint: "/v4/competitions/\(competitionId)/teams/\(teamId)/teamprofile")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    // MARK: - Match Endpoints

    /// GET /v4/matches/{matchId}/details - Match Details
    public func getMatchDetails(matchId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/matches/\(matchId)/details")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    /// GET /v4/matches/{matchId}/betlink - Match Betlink
    public func getMatchBetlink(matchId: String) async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/matches/\(matchId)/betlink")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    // MARK: - Live Endpoints

    /// GET /v4/live/eventtypes - Event Types
    public func getLiveEventTypes() async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/live/eventtypes")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    // MARK: - Config Endpoints

    /// GET /v4/config - Get Config
    public func getConfig() async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/config")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }

    // MARK: - Chat Endpoints

    /// GET /v4/chat/leagueselection - Chat League Selection
    public func getChatLeagueSelection() async throws -> String {
        let (data, _) = try await makeRequest(endpoint: "/v4/chat/leagueselection")
        return String(data: data, encoding: .utf8) ?? ""
    }

    /// GET /v4/chat/refreshtoken - Chat Refresh Token
    public func getChatRefreshToken() async throws -> [String: Any] {
        let (data, _) = try await makeRequest(endpoint: "/v4/chat/refreshtoken")
        return try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
    }
}
