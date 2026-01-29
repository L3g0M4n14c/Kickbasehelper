package kickbase.core

import skip.lib.*
import skip.lib.Array
import skip.lib.Set

import skip.foundation.*

// Temporäre Debug-Klasse zum Analysieren der Marktspielerdaten
internal open class DebugMarketParser {

    @androidx.annotation.Keep
    companion object {

        internal fun analyzeMarketResponse(json: Dictionary<String, Any>) {
            print("🔍 === MARKET RESPONSE ANALYSIS ===")
            print("📋 Top-level keys: ${Array(json.keys).sorted()}")

            for ((key, value) in json.sref()) {
                analyzeValue(key = key, value = value, depth = 0)
            }

            print("🔍 === END ANALYSIS ===")
        }

        private fun analyzeValue(key: String, value: Any, depth: Int) {
            val indent = String(repeating = "  ", count = depth)

            val raw = rawArray(from = value)
            val array = raw.compactMap { it -> dict(from = it) }
            if (!array.isEmpty) {
                print("${indent}📊 ${key}: Array[${array.count}]")
                array.first.sref()?.let { firstItem ->
                    print("${indent}  First item keys: ${Array(firstItem.keys).sorted()}")

                    // Prüfe ob es Marktspielerdaten sein könnten
                    val marketKeys = arrayOf("price", "seller", "expiry", "offers")
                    val playerKeys = arrayOf("firstName", "lastName", "name", "id")

                    val hasMarketKeys = marketKeys.contains { it -> firstItem.keys.contains(it) }
                    val hasPlayerKeys = playerKeys.contains { it -> firstItem.keys.contains(it) }

                    if (hasMarketKeys || hasPlayerKeys) {
                        print("${indent}  🎯 POTENTIAL MARKET PLAYERS ARRAY!")
                        print("${indent}  Sample item:")
                        for ((itemKey, itemValue) in firstItem.sref()) {
                            if (itemValue is String || itemValue is java.lang.Number) {
                                print("${indent}    ${itemKey}: ${itemValue}")
                            } else {
                                dict(from = itemValue)?.let { nestedDict ->
                                    print("${indent}    ${itemKey}: {nested dict with ${nestedDict.count} keys}")
                                }
                            }
                        }
                    }
                }
            } else {
                val matchtarget_0 = dict(from = value)
                if (matchtarget_0 != null) {
                    val dictVal = matchtarget_0
                    print("${indent}📊 ${key}: Dict[${dictVal.count}]")
                    if (depth < 2) {
                        for ((nestedKey, nestedValue) in dictVal.sref()) {
                            analyzeValue(key = "${key}.${nestedKey}", value = nestedValue, depth = depth + 1)
                        }
                    }
                } else {
                    val matchtarget_1 = value as? String
                    if (matchtarget_1 != null) {
                        val stringValue = matchtarget_1
                        print("${indent}📊 ${key}: String(${stringValue.prefix(50)})")
                    } else {
                        val matchtarget_2 = value as? java.lang.Number
                        if (matchtarget_2 != null) {
                            val numberValue = matchtarget_2
                            print("${indent}📊 ${key}: Number(${numberValue})")
                        } else {
                            print("${indent}📊 ${key}: ${type(of = value)}")
                        }
                    }
                }
            }
        }

        internal fun findMarketPlayersInResponse(json: Dictionary<String, Any>): Array<*> {
            print("🔍 Smart search for market players...")

            // Direkte Arrays prüfen
            val directArrayKeys = arrayOf(
                "players",
                "market",
                "data",
                "transfers",
                "items",
                "list",
                "offers",
                "bids"
            )
            for (key in directArrayKeys.sref()) {
                val array = rawArray(from = json[key])
                if (!array.isEmpty) {
                    if (looksLikeMarketPlayers(array)) {
                        print("✅ Found market players in direct key: ${key}")
                        return array.sref()
                    }
                }
            }

            // Verschachtelte Strukturen durchsuchen
            for ((topKey, topValue) in json.sref()) {
                dict(from = topValue)?.let { nestedDict ->
                    for ((nestedKey, nestedValue) in nestedDict.sref()) {
                        val array = rawArray(from = nestedValue)
                        if (!array.isEmpty) {
                            if (looksLikeMarketPlayers(array)) {
                                print("✅ Found market players in nested structure: ${topKey}.${nestedKey}")
                                return array.sref()
                            }
                        }
                    }
                }
            }

            print("❌ No market players found in response")
            return (arrayOf() as Array<*>).sref()
        }

        private fun looksLikeMarketPlayers(array: Array<*>): Boolean {
            val firstItem = array.compactMap { it -> dict(from = it) }.first.sref()
            if (firstItem == null) {
                return false
            }

            val keys = Set(firstItem.keys)

            // Marktspezifische Felder
            val marketKeys: Set<String> = setOf("price", "seller", "expiry", "offers", "bid")
            val playerKeys: Set<String> = setOf("firstName", "lastName", "name", "id", "teamName")

            val hasMarketKeys = (!keys.intersection(marketKeys).isEmpty).sref()
            val hasPlayerKeys = (!keys.intersection(playerKeys).isEmpty).sref()

            return hasMarketKeys || hasPlayerKeys
        }
    }
}
