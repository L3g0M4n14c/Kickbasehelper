package kickbase.core

import skip.lib.*
import skip.lib.Array

import skip.foundation.*

/// Kleine Hilfsfunktionen, um JSON-Parsing ohne generische Runtime-Casts zu machen
/// Ziel: Vermeide direkte casts wie `as? [[String: Any]]` die beim Transpilieren Warnungen erzeugen.

internal inline fun arrayOfDicts(from: Any?): Array<Dictionary<String, Any>> {
    val value = from
    val nsArr_0 = (value as? NSArray).sref()
    if (nsArr_0 == null) {
        return arrayOf()
    }
    var out: Array<Dictionary<String, Any>> = arrayOf()
    for (el in nsArr_0.sref()) {
        (el as? NSDictionary).sref()?.let { nsDict ->
            var dict: Dictionary<String, Any> = dictionaryOf()
            for ((key, val_) in nsDict.sref()) {
                (key as? String)?.let { ks ->
                    dict[ks] = val_.sref()
                }
            }
            out.append(dict)
        }
    }
    return out.sref()
}

internal inline fun dict(from: Any?): Dictionary<String, Any>? {
    val value = from
    val nsDict_0 = (value as? NSDictionary).sref()
    if (nsDict_0 == null) {
        return null
    }
    var dict: Dictionary<String, Any> = dictionaryOf()
    for ((key, val_) in nsDict_0.sref()) {
        (key as? String)?.let { ks ->
            dict[ks] = val_.sref()
        }
    }
    return dict.sref()
}

internal inline fun arrayOfStrings(from: Any?): Array<String> {
    val value = from
    val nsArr_1 = (value as? NSArray).sref()
    if (nsArr_1 == null) {
        return arrayOf()
    }
    var out: Array<String> = arrayOf()
    for (el in nsArr_1.sref()) {
        val matchtarget_0 = el as? String
        if (matchtarget_0 != null) {
            val s = matchtarget_0
            out.append(s)
        } else {
            (el as? kotlin.String)?.let { s ->
                out.append(s as String)
            }
        }
    }
    return out.sref()
}

internal inline fun rawArray(from: Any?): Array<*> {
    val value = from
    val nsArr_2 = (value as? NSArray).sref()
    if (nsArr_2 == null) {
        return arrayOf()
    }
    var out: Array<*> = arrayOf()
    for (el in nsArr_2.sref()) {
        out.append(el)
    }
    return out.sref()
}

internal inline fun jsonDict(from: Data): Dictionary<String, Any> {
    val data = from
    try {
        val obj = JSONSerialization.jsonObject(with = data)
        (obj as? NSDictionary).sref()?.let { nsDict ->
            var dict: Dictionary<String, Any> = dictionaryOf()
            for ((key, val_) in nsDict.sref()) {
                (key as? String)?.let { ks ->
                    dict[ks] = val_.sref()
                }
            }
            return dict.sref()
        }

        (obj as? NSArray).sref()?.let { nsArr ->
            val arr = nsArr.compactMap(fun(el: *): Dictionary<String, Any>? {
                (el as? NSDictionary).sref()?.let { nd ->
                    var d: Dictionary<String, Any> = dictionaryOf()
                    for ((k, v) in nd.sref()) {
                        (k as? String)?.let { ks ->
                            d[ks] = v.sref()
                        }
                    }
                    return d
                }
                return null
            })
            if (!arr.isEmpty) {
                return dictionaryOf(Tuple2("data", arr))
            }
        }

        return dictionaryOf()
    } catch (error: Throwable) {
        @Suppress("NAME_SHADOWING") val error = error.aserror()
        return dictionaryOf()
    }
}
