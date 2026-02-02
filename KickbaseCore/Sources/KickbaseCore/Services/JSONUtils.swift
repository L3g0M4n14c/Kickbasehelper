import Foundation

/// Kleine Hilfsfunktionen, um JSON-Parsing ohne generische Runtime-Casts zu machen
/// Ziel: Vermeide direkte casts wie `as? [[String: Any]]` die beim Transpilieren Warnungen erzeugen.

// SKIP REPLACE:
// internal fun arrayOfDicts(from: Any?): Array<Dictionary<String, Any>> {
//     val value = from
//     val nsArr = (value as? NSArray).sref()
//     if (nsArr == null) return arrayOf()
//     var out: Array<Dictionary<String, Any>> = arrayOf()
//     for (el in nsArr.sref()) {
//         (el as? NSDictionary).sref()?.let { nd ->
//             var d: Dictionary<String, Any> = dictionaryOf()
//             for (entry in nd.sref()) {
//                 val k = entry.key as? String
//                 if (k != null) d[k] = entry.value.sref()
//             }
//             out.append(d)
//         }
//     }
//     return out.sref()
// }
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

// SKIP REPLACE:
// internal fun dict(from: Any?): Dictionary<String, Any>? {
//     val value = from
//     val nsDict = (value as? NSDictionary).sref()
//     if (nsDict == null) return null
//     var dict: Dictionary<String, Any> = dictionaryOf()
//     for (entry in nsDict.sref()) {
//         val key = entry.key as? String
//         if (key != null) dict[key] = entry.value.sref()
//     }
//     return dict.sref()
// }
@inline(__always)
internal func dict(from value: Any?) -> [String: Any]? {
    guard let nsDict = value as? NSDictionary else { return nil }
    var dict: [String: Any] = [:]
    for (key, val) in nsDict {
        if let ks = key as? String { dict[ks] = val }
    }
    return dict
}

// SKIP REPLACE:
// internal fun arrayOfStrings(from: Any?): Array<String> {
//     val value = from
//     val nsArr = (value as? NSArray).sref()
//     if (nsArr == null) return arrayOf()
//     var out: Array<String> = arrayOf()
//     for (el in nsArr.sref()) {
//         (el as? String)?.let { s -> out.append(s) }
//         (el as? kotlin.String)?.let { s -> out.append(s as String) }
//     }
//     return out.sref()
// }
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

// SKIP REPLACE:
// internal fun rawArray(from: Any?): Array<*> {
//     val value = from
//     val nsArr = (value as? NSArray).sref()
//     if (nsArr == null) return arrayOf()
//     var out: Array<Any?> = arrayOf()
//     for (el in nsArr.sref()) {
//         out.append(el)
//     }
//     return out.sref()
// }
@inline(__always)
internal func rawArray(from value: Any?) -> [Any] {
    guard let nsArr = value as? NSArray else { return [] }
    var out: [Any] = []
    for el in nsArr {
        out.append(el)
    }
    return out
}

// SKIP REPLACE:
// internal fun jsonDict(from: Data): Dictionary<String, Any> {
//     val data = from
//     try {
//         val obj = JSONSerialization.jsonObject(with = data)
//         (obj as? NSDictionary).sref()?.let { nsDict ->
//             var dict: Dictionary<String, Any> = dictionaryOf()
//             for (entry in nsDict.sref()) {
//                 val key = entry.key as? String
//                 if (key != null) dict[key] = entry.value.sref()
//             }
//             return dict.sref()
//         }
//         (obj as? NSArray).sref()?.let { nsArr ->
//             var arr: Array<Dictionary<String, Any>> = arrayOf()
//             for (el in nsArr.sref()) {
//                 (el as? NSDictionary).sref()?.let { nd ->
//                     var d: Dictionary<String, Any> = dictionaryOf()
//                     for (entry in nd.sref()) {
//                         val k = entry.key as? String
//                         if (k != null) d[k] = entry.value.sref()
//                     }
//                     arr.append(d)
//                 }
//             }
//             if (!arr.isEmpty) return dictionaryOf(Tuple2("data", arr))
//         }
//         return dictionaryOf()
//     } catch (error: Throwable) {
//         return dictionaryOf()
//     }
// }
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
