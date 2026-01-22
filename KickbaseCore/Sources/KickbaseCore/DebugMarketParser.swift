import Foundation

// Temporäre Debug-Klasse zum Analysieren der Marktspielerdaten
class DebugMarketParser {
    
    static func analyzeMarketResponse(_ json: [String: Any]) {
        print("🔍 === MARKET RESPONSE ANALYSIS ===")
        print("📋 Top-level keys: \(Array(json.keys).sorted())")
        
        for (key, value) in json {
            analyzeValue(key: key, value: value, depth: 0)
        }
        
        print("🔍 === END ANALYSIS ===")
    }
    
    private static func analyzeValue(key: String, value: Any, depth: Int) {
        let indent = String(repeating: "  ", count: depth)
        
        if let array = value as? [[String: Any]] {
            print("\(indent)📊 \(key): Array[\(array.count)]")
            if let firstItem = array.first {
                print("\(indent)  First item keys: \(Array(firstItem.keys).sorted())")
                
                // Prüfe ob es Marktspielerdaten sein könnten
                let marketKeys = ["price", "seller", "expiry", "offers"]
                let playerKeys = ["firstName", "lastName", "name", "id"]
                
                let hasMarketKeys = marketKeys.contains { firstItem.keys.contains($0) }
                let hasPlayerKeys = playerKeys.contains { firstItem.keys.contains($0) }
                
                if hasMarketKeys || hasPlayerKeys {
                    print("\(indent)  🎯 POTENTIAL MARKET PLAYERS ARRAY!")
                    print("\(indent)  Sample item:")
                    for (itemKey, itemValue) in firstItem {
                        if itemValue is String || itemValue is NSNumber {
                            print("\(indent)    \(itemKey): \(itemValue)")
                        } else if let nestedDict = itemValue as? [String: Any] {
                            print("\(indent)    \(itemKey): {nested dict with \(nestedDict.count) keys}")
                        }
                    }
                }
            }
        } else if let dict = value as? [String: Any] {
            print("\(indent)📊 \(key): Dict[\(dict.count)]")
            if depth < 2 { // Verhindere zu tiefe Verschachtelung
                for (nestedKey, nestedValue) in dict {
                    analyzeValue(key: "\(key).\(nestedKey)", value: nestedValue, depth: depth + 1)
                }
            }
        } else if let stringValue = value as? String {
            print("\(indent)📊 \(key): String(\(stringValue.prefix(50)))")
        } else if let numberValue = value as? NSNumber {
            print("\(indent)📊 \(key): Number(\(numberValue))")
        } else {
            print("\(indent)📊 \(key): \(type(of: value))")
        }
    }
    
    static func findMarketPlayersInResponse(_ json: [String: Any]) -> [[String: Any]] {
        print("🔍 Smart search for market players...")
        
        // Direkte Arrays prüfen
        let directArrayKeys = ["players", "market", "data", "transfers", "items", "list", "offers", "bids"]
        for key in directArrayKeys {
            if let array = json[key] as? [[String: Any]], !array.isEmpty {
                if looksLikeMarketPlayers(array) {
                    print("✅ Found market players in direct key: \(key)")
                    return array
                }
            }
        }
        
        // Verschachtelte Strukturen durchsuchen
        for (topKey, topValue) in json {
            if let nestedDict = topValue as? [String: Any] {
                for (nestedKey, nestedValue) in nestedDict {
                    if let array = nestedValue as? [[String: Any]], !array.isEmpty {
                        if looksLikeMarketPlayers(array) {
                            print("✅ Found market players in nested structure: \(topKey).\(nestedKey)")
                            return array
                        }
                    }
                }
            }
        }
        
        print("❌ No market players found in response")
        return []
    }
    
    private static func looksLikeMarketPlayers(_ array: [[String: Any]]) -> Bool {
        guard let firstItem = array.first else { return false }
        
        let keys = Set(firstItem.keys)
        
        // Marktspezifische Felder
        let marketKeys: Set<String> = ["price", "seller", "expiry", "offers", "bid"]
        let playerKeys: Set<String> = ["firstName", "lastName", "name", "id", "teamName"]
        
        let hasMarketKeys = !keys.intersection(marketKeys).isEmpty
        let hasPlayerKeys = !keys.intersection(playerKeys).isEmpty
        
        return hasMarketKeys || hasPlayerKeys
    }
}