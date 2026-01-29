package kickbase.core

import skip.lib.*
import skip.lib.Array

import skip.foundation.*

// Erweiterte Parser-Methode für Marktspielerdaten
// Diese Datei enthält die verbesserte parseMarketPlayersFromResponse Methode

/*
Diese Methode sollte die bestehende parseMarketPlayersFromResponse Methode
in KickbasePlayerService.swift ersetzen:
*/

private fun parseMarketPlayersFromResponse(json: Dictionary<String, Any>): Array<MarketPlayer> {
    print("🔍 === MARKET PLAYER PARSING DEBUG ===")
    print("📋 Market JSON keys: ${Array(json.keys)}")

    // Verwende die neue Debug-Analyse
    DebugMarketParser.analyzeMarketResponse(json)

    // Intelligente Suche nach Marktspielerdaten
    val playersRaw = DebugMarketParser.findMarketPlayersInResponse(json)
    val playersArray = playersRaw.compactMap { it -> dict(from = it) }

    if (playersArray.isEmpty) {
        print("❌ NO MARKET PLAYER DATA FOUND IN RESPONSE!")
        print("📋 Available top-level keys: ${Array(json.keys)}")

        // Zusätzliche manuelle Fallback-Suche
        for ((key, value) in json.sref()) {
            val matchtarget_0 = value as? String
            if (matchtarget_0 != null) {
                val stringValue = matchtarget_0
                print("   ${key}: ${stringValue}")
            } else {
                val matchtarget_1 = value as? java.lang.Number
                if (matchtarget_1 != null) {
                    val numberValue = matchtarget_1
                    print("   ${key}: ${numberValue}")
                } else {
                    val arrayValue = rawArray(from = value)
                    if (!arrayValue.isEmpty) {
                        print("   ${key}: Array with ${arrayValue.count} elements")
                        // Schaue in das Array hinein
                        arrayValue.first.sref()?.let { firstElement ->
                            print("     First element type: ${type(of = firstElement)}")
                            dict(from = firstElement)?.let { dictElement ->
                                print("     First element keys: ${Array(dictElement.keys)}")
                            }
                        }
                    } else {
                        dict(from = value)?.let { dictValue ->
                            print("   ${key}: Dictionary with keys: ${Array(dictValue.keys)}")
                            // Schaue nach verschachtelten Arrays
                            for ((nestedKey, nestedValue) in dictValue.sref()) {
                                val nestedRawAny = rawArray(from = nestedValue)
                                val nestedArray = nestedRawAny.compactMap { it -> dict(from = it) }
                                if (!nestedArray.isEmpty) {
                                    print("     ${key}.${nestedKey}: Array with ${nestedArray.count} elements")
                                } else if (!nestedRawAny.isEmpty) {
                                    print("     ${key}.${nestedKey}: Array with ${nestedRawAny.count} elements")
                                }
                            }
                        }
                    }
                }
            }
        }

        return arrayOf()
    }

    print("🎯 Processing ${playersArray.count} market players...")
    var parsedPlayers: Array<MarketPlayer> = arrayOf()

    for ((index, playerData) in playersArray.enumerated()) {
        print("🔄 Parsing market player ${index + 1}:")

        // Detaillierte Feldanalyse für jeden Spieler
        for ((fieldKey, fieldValue) in playerData.sref()) {
            val matchtarget_2 = fieldValue as? String
            if (matchtarget_2 != null) {
                val stringValue = matchtarget_2
                print("   ${fieldKey}: '${stringValue}'")
            } else {
                val matchtarget_3 = fieldValue as? java.lang.Number
                if (matchtarget_3 != null) {
                    val numberValue = matchtarget_3
                    print("   ${fieldKey}: ${numberValue}")
                } else {
                    val matchtarget_4 = dict(from = fieldValue)
                    if (matchtarget_4 != null) {
                        val nestedDict = matchtarget_4
                        print("   ${fieldKey}: {nested dict with keys: ${Array(nestedDict.keys)}}")
                    } else {
                        print("   ${fieldKey}: ${type(of = fieldValue)}")
                    }
                }
            }
        }

        // Flexibles Parsing mit verschiedenen Feldname-Varianten
        val seller = MarketSeller(id = extractSellerId(from = playerData), name = extractSellerName(from = playerData))

        // Parse owner information from "u" field
        val owner = extractOwner(from = playerData)

        val player = MarketPlayer(id = playerData["id"] as? String ?: playerData["playerId"] as? String ?: playerData["i"] as? String ?: playerData["pId"] as? String ?: "", firstName = playerData["firstName"] as? String ?: playerData["fn"] as? String ?: playerData["name"] as? String ?: "", lastName = playerData["lastName"] as? String ?: playerData["ln"] as? String ?: playerData["n"] as? String ?: "", profileBigUrl = playerData["profileBigUrl"] as? String ?: playerData["imageUrl"] as? String ?: playerData["pim"] as? String ?: playerData["image"] as? String ?: "", teamName = playerData["teamName"] as? String ?: playerData["tn"] as? String ?: playerData["club"] as? String ?: "", teamId = playerData["teamId"] as? String ?: playerData["tid"] as? String ?: "", position = playerData["position"] as? Int ?: playerData["pos"] as? Int ?: 0, number = playerData["number"] as? Int ?: playerData["jerseyNumber"] as? Int ?: 0, averagePoints = playerData["averagePoints"] as? Double ?: playerData["ap"] as? Double ?: playerData["avgPoints"] as? Double ?: 0.0, totalPoints = playerData["totalPoints"] as? Int ?: playerData["p"] as? Int ?: playerData["points"] as? Int ?: 0, marketValue = playerData["marketValue"] as? Int ?: playerData["mv"] as? Int ?: 0, marketValueTrend = playerData["marketValueTrend"] as? Int ?: playerData["mvt"] as? Int ?: 0, price = playerData["price"] as? Int ?: playerData["prc"] as? Int ?: playerData["bid"] as? Int ?: playerData["amount"] as? Int ?: 0, expiry = playerData["expiry"] as? String ?: playerData["dt"] as? String ?: playerData["expires"] as? String ?: playerData["until"] as? String ?: "", offers = playerData["offers"] as? Int ?: playerData["ofc"] as? Int ?: playerData["bids"] as? Int ?: 0, seller = seller, stl = playerData["stl"] as? Int ?: 0, status = playerData["st"] as? Int ?: playerData["status"] as? Int ?: 0, prlo = playerData["prlo"] as? Int, owner = owner, exs = playerData["exs"] as? Int ?: 0)
        parsedPlayers.append(player)

        val displayName = if (player.firstName.isEmpty) player.lastName else "${player.firstName} ${player.lastName}"
        print("✅ Parsed market player: ${displayName} (€${player.price / 1000}k) from ${player.seller.name}")
    }

    print("✅ Successfully parsed ${parsedPlayers.count} market players")
    print("🔍 === END MARKET PLAYER PARSING DEBUG ===")
    return parsedPlayers.sref()
}

internal fun extractSellerId(from: Dictionary<String, Any>): String {
    val playerData = from
    dict(from = playerData["seller"])?.let { seller ->
        return seller["id"] as? String ?: seller["userId"] as? String ?: ""
    }
    return playerData["sellerId"] as? String ?: ""
}

internal fun extractSellerName(from: Dictionary<String, Any>): String {
    val playerData = from
    dict(from = playerData["seller"])?.let { seller ->
        return seller["name"] as? String ?: seller["username"] as? String ?: "Unknown"
    }
    return playerData["sellerName"] as? String ?: "Unknown"
}

internal fun extractOwner(from: Dictionary<String, Any>): PlayerOwner? {
    val playerData = from
    val ownerData_0 = dict(from = playerData["u"])
    if (ownerData_0 == null) {
        return null
    }
    val id_0 = ownerData_0["i"] as? String
    if (id_0 == null) {
        print("   ⚠️ Owner data missing required fields (i or n)")
        return null
    }
    val name_0 = ownerData_0["n"] as? String
    if (name_0 == null) {
        print("   ⚠️ Owner data missing required fields (i or n)")
        return null
    }

    val owner = PlayerOwner(i = id_0, n = name_0, uim = ownerData_0["uim"] as? String, isvf = ownerData_0["isvf"] as? Boolean, st = ownerData_0["st"] as? Int)

    print("   ✅ Found owner: ${owner.name} (ID: ${owner.id})")
    return owner
}
