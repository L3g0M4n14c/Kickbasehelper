import Foundation

/// Kleine Hilfsfunktionen, um JSON-Parsing ohne generische Runtime-Casts zu machen
/// Ziel: Vermeide direkte casts wie `as? [[String: Any]]` die beim Transpilieren Warnungen erzeugen.

@inline(__always)
internal func arrayOfDicts(from value: Any?) -> [[String: Any]] {
    guard let nsArr = value as? NSArray else { return [] }
    var out: [[String: Any]] = []
    for el in nsArr {
        if let nsDict = el as? NSDictionary {
            var dict: [String: Any] = [:]
            for (key, val) in nsDict {
                if let ks = key as? String { dict[ks] = val }
            }
            out.append(dict)
        }
    }
    return out
}

@inline(__always)
internal func dict(from value: Any?) -> [String: Any]? {
    guard let nsDict = value as? NSDictionary else { return nil }
    var dict: [String: Any] = [:]
    for (key, val) in nsDict {
        if let ks = key as? String { dict[ks] = val }
    }
    return dict
}

@inline(__always)
internal func arrayOfStrings(from value: Any?) -> [String] {
    guard let nsArr = value as? NSArray else { return [] }
    var out: [String] = []
    for el in nsArr {
        if let s = el as? String {
            out.append(s)
        } else if let s = el as? NSString {
            out.append(s as String)
        }
    }
    return out
}

@inline(__always)
internal func rawArray(from value: Any?) -> [Any] {
    guard let nsArr = value as? NSArray else { return [] }
    var out: [Any] = []
    for el in nsArr {
        out.append(el)
    }
    return out
}

@inline(__always)
internal func jsonDict(from data: Data) -> [String: Any] {
    do {
        let obj = try JSONSerialization.jsonObject(with: data)
        if let nsDict = obj as? NSDictionary {
            var dict: [String: Any] = [:]
            for (key, val) in nsDict {
                if let ks = key as? String { dict[ks] = val }
            }
            return dict
        }

        if let nsArr = obj as? NSArray {
            let arr = nsArr.compactMap { el -> [String: Any]? in
                if let nd = el as? NSDictionary {
                    var d: [String: Any] = [:]
                    for (k, v) in nd { if let ks = k as? String { d[ks] = v } }
                    return d
                }
                return nil
            }
            if !arr.isEmpty { return ["data": arr] }
        }

        return [:]
    } catch {
        return [:]
    }
}
