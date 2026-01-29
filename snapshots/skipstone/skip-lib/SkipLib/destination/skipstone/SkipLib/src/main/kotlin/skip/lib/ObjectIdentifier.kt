package skip.lib

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

class ObjectIdentifier {
    internal val object_: Any

    override fun equals(other: Any?): Boolean {
        if (other !is ObjectIdentifier) {
            return false
        }
        val lhs = this
        val rhs = other
        return lhs.object_ === rhs.object_
    }

    internal constructor(object_: Any) {
        this.object_ = object_.sref()
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, object_)
        return result
    }

    @androidx.annotation.Keep
    companion object {
    }
}

