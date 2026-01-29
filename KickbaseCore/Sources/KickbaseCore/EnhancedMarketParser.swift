import Foundation

// Erweiterte Parser-Methode für Marktspielerdaten
// Diese Datei enthält die verbesserte parseMarketPlayersFromResponse Methode

/*
Diese Methode sollte die bestehende parseMarketPlayersFromResponse Methode
in KickbasePlayerService.swift ersetzen:
*/

private func parseMarketPlayersFromResponse(_ json: [String: Any]) -> [MarketPlayer] {
    print("🔍 === MARKET PLAYER PARSING DEBUG ===")
    print("📋 Market JSON keys: \(Array(json.keys))")

    // Verwende die neue Debug-Analyse
    DebugMarketParser.analyzeMarketResponse(json)

    // Intelligente Suche nach Marktspielerdaten
    let playersRaw = DebugMarketParser.findMarketPlayersInResponse(json)
    let playersArray = playersRaw.compactMap { dict(from: $0) }

    if playersArray.isEmpty {
        print("❌ NO MARKET PLAYER DATA FOUND IN RESPONSE!")
        print("📋 Available top-level keys: \(Array(json.keys))")

        // Zusätzliche manuelle Fallback-Suche
        for (key, value) in json {
            if let stringValue = value as? String {
                print("   \(key): \(stringValue)")
            } else if let numberValue = value as? NSNumber {
                print("   \(key): \(numberValue)")
            } else {
                let arrayValue = rawArray(from: value)
                if !arrayValue.isEmpty {
                    print("   \(key): Array with \(arrayValue.count) elements")
                    // Schaue in das Array hinein
                    if let firstElement = arrayValue.first {
                        print("     First element type: \(type(of: firstElement))")
                        if let dictElement = dict(from: firstElement) {
                            print("     First element keys: \(Array(dictElement.keys))")
                        }
                    }
                } else if let dictValue = dict(from: value) {
                    print("   \(key): Dictionary with keys: \(Array(dictValue.keys))")
                    // Schaue nach verschachtelten Arrays
                    for (nestedKey, nestedValue) in dictValue {
                        let nestedRawAny = rawArray(from: nestedValue)
                        let nestedArray = nestedRawAny.compactMap { dict(from: $0) }
                        if !nestedArray.isEmpty {
                            print(
                                "     \(key).\(nestedKey): Array with \(nestedArray.count) elements"
                            )
                        } else if !nestedRawAny.isEmpty {
                            print(
                                "     \(key).\(nestedKey): Array with \(nestedRawAny.count) elements"
                            )
                        }
                    }
                }
            }
        }

        return []
    }

    print("🎯 Processing \(playersArray.count) market players...")
    var parsedPlayers: [MarketPlayer] = []

    for (index, playerData) in playersArray.enumerated() {
        print("🔄 Parsing market player \(index + 1):")

        // Detaillierte Feldanalyse für jeden Spieler
        for (fieldKey, fieldValue) in playerData {
            if let stringValue = fieldValue as? String {
                print("   \(fieldKey): '\(stringValue)'")
            } else if let numberValue = fieldValue as? NSNumber {
                print("   \(fieldKey): \(numberValue)")
            } else if let nestedDict = dict(from: fieldValue) {
                print("   \(fieldKey): {nested dict with keys: \(Array(nestedDict.keys))}")
            } else {
                print("   \(fieldKey): \(type(of: fieldValue))")
            }
        }

        // Flexibles Parsing mit verschiedenen Feldname-Varianten
        let seller = MarketSeller(
            id: extractSellerId(from: playerData),
            name: extractSellerName(from: playerData)
        )

        // Parse owner information from "u" field
        let owner = extractOwner(from: playerData)

        let player = MarketPlayer(
            id: playerData["id"] as? String ?? playerData["playerId"] as? String ?? playerData["i"]
                as? String ?? playerData["pId"] as? String ?? "",
            firstName: playerData["firstName"] as? String ?? playerData["fn"] as? String
                ?? playerData["name"] as? String ?? "",
            lastName: playerData["lastName"] as? String ?? playerData["ln"] as? String
                ?? playerData["n"] as? String ?? "",
            // Choose profile image using shared heuristics (keeps behavior consistent with team parsing)
            profileBigUrl: chooseProfileBigUrl(nil, playerData),
            teamName: playerData["teamName"] as? String ?? playerData["tn"] as? String
                ?? playerData["club"] as? String ?? "",
            teamId: playerData["teamId"] as? String ?? playerData["tid"] as? String ?? "",
            position: playerData["position"] as? Int ?? playerData["pos"] as? Int ?? 0,
            number: playerData["number"] as? Int ?? playerData["jerseyNumber"] as? Int ?? 0,
            averagePoints: playerData["averagePoints"] as? Double ?? playerData["ap"] as? Double
                ?? playerData["avgPoints"] as? Double ?? 0.0,
            totalPoints: playerData["totalPoints"] as? Int ?? playerData["p"] as? Int ?? playerData[
                "points"] as? Int ?? 0,
            marketValue: playerData["marketValue"] as? Int ?? playerData["mv"] as? Int ?? 0,
            marketValueTrend: playerData["marketValueTrend"] as? Int ?? playerData["mvt"] as? Int
                ?? 0,
            price: playerData["price"] as? Int ?? playerData["prc"] as? Int ?? playerData["bid"]
                as? Int ?? playerData["amount"] as? Int ?? 0,
            expiry: playerData["expiry"] as? String ?? playerData["dt"] as? String ?? playerData[
                "expires"] as? String ?? playerData["until"] as? String ?? "",
            offers: playerData["offers"] as? Int ?? playerData["ofc"] as? Int ?? playerData["bids"]
                as? Int ?? 0,
            seller: seller,
            stl: playerData["stl"] as? Int ?? 0,
            status: playerData["st"] as? Int ?? playerData["status"] as? Int ?? 0,
            prlo: playerData["prlo"] as? Int,
            owner: owner,
            exs: playerData["exs"] as? Int ?? 0
        )
        parsedPlayers.append(player)

        let displayName =
            player.firstName.isEmpty ? player.lastName : "\(player.firstName) \(player.lastName)"
        print(
            "✅ Parsed market player: \(displayName) (€\(player.price/1000)k) from \(player.seller.name)"
        )
    }

    print("✅ Successfully parsed \(parsedPlayers.count) market players")
    print("🔍 === END MARKET PLAYER PARSING DEBUG ===")
    return parsedPlayers
}

func extractSellerId(from playerData: [String: Any]) -> String {
    if let seller = dict(from: playerData["seller"]) {
        return seller["id"] as? String ?? seller["userId"] as? String ?? ""
    }
    return playerData["sellerId"] as? String ?? ""
}

func extractSellerName(from playerData: [String: Any]) -> String {
    if let seller = dict(from: playerData["seller"]) {
        return seller["name"] as? String ?? seller["username"] as? String ?? "Unknown"
    }
    return playerData["sellerName"] as? String ?? "Unknown"
}

func extractOwner(from playerData: [String: Any]) -> PlayerOwner? {
    guard let ownerData = dict(from: playerData["u"]) else {
        return nil
    }

    guard let id = ownerData["i"] as? String,
        let name = ownerData["n"] as? String
    else {
        print("   ⚠️ Owner data missing required fields (i or n)")
        return nil
    }

    let owner = PlayerOwner(
        i: id,
        n: name,
        uim: ownerData["uim"] as? String,
        isvf: ownerData["isvf"] as? Bool,
        st: ownerData["st"] as? Int
    )

    print("   ✅ Found owner: \(owner.name) (ID: \(owner.id))")
    return owner
}
