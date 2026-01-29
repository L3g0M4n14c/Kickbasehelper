package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

sealed class CoordinateSpace {
    class GlobalCase: CoordinateSpace() {
    }
    class LocalCase: CoordinateSpace() {
    }
    class NamedCase(val associated0: AnyHashable): CoordinateSpace() {
        override fun equals(other: Any?): Boolean {
            if (other !is NamedCase) return false
            return associated0 == other.associated0
        }
        override fun hashCode(): Int {
            var result = 1
            result = Hasher.combine(result, associated0)
            return result
        }
    }

    val isGlobal: Boolean
        get() = this == CoordinateSpace.global

    val isLocal: Boolean
        get() = this == CoordinateSpace.local

    @androidx.annotation.Keep
    companion object {
        val global: CoordinateSpace = GlobalCase()
        val local: CoordinateSpace = LocalCase()
        fun named(associated0: AnyHashable): CoordinateSpace = NamedCase(associated0)
    }
}

interface CoordinateSpaceProtocol {
    val coordinateSpace: CoordinateSpace
}
interface CoordinateSpaceProtocolCompanion {

    fun scrollView(axis: Axis): NamedCoordinateSpace = named("_scrollView_axis_${axis.rawValue}_")

    val scrollView: NamedCoordinateSpace
        get() = named("_scrollView_")

    fun named(name: Hashable): NamedCoordinateSpace = NamedCoordinateSpace(coordinateSpace = CoordinateSpace.named(name))
}

internal fun CoordinateSpaceProtocolFrom(bridged: Int, name: AnyHashable?): CoordinateSpaceProtocol {
    when (bridged) {
        0 -> return GlobalCoordinateSpace.global
        1 -> return LocalCoordinateSpace.local
        else -> return NamedCoordinateSpace.named(name ?: "" as AnyHashable)
    }
}

open class NamedCoordinateSpace: CoordinateSpaceProtocol {
    private val _coordinateSpace: CoordinateSpace

    internal constructor(coordinateSpace: CoordinateSpace) {
        _coordinateSpace = coordinateSpace
    }

    override val coordinateSpace: CoordinateSpace
        get() = _coordinateSpace

    override fun equals(other: Any?): Boolean {
        if (other !is NamedCoordinateSpace) {
            return false
        }
        val lhs = this
        val rhs = other
        return lhs.coordinateSpace == rhs.coordinateSpace
    }

    @androidx.annotation.Keep
    companion object: CompanionClass() {
    }
    open class CompanionClass: CoordinateSpaceProtocolCompanion {
    }
}

open class LocalCoordinateSpace: CoordinateSpaceProtocol {
    override val coordinateSpace: CoordinateSpace
        get() = CoordinateSpace.local

    @androidx.annotation.Keep
    companion object: CompanionClass() {

        override val local: LocalCoordinateSpace
            get() = LocalCoordinateSpace()
    }
    open class CompanionClass: CoordinateSpaceProtocolCompanion {
        open val local: LocalCoordinateSpace
            get() = LocalCoordinateSpace.local
    }
}

open class GlobalCoordinateSpace: CoordinateSpaceProtocol {
    override val coordinateSpace: CoordinateSpace
        get() = CoordinateSpace.global

    @androidx.annotation.Keep
    companion object: CompanionClass() {

        override val global: GlobalCoordinateSpace
            get() = GlobalCoordinateSpace()
    }
    open class CompanionClass: CoordinateSpaceProtocolCompanion {
        open val global: GlobalCoordinateSpace
            get() = GlobalCoordinateSpace.global
    }
}

