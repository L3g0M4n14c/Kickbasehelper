package skip.ui

import skip.lib.*

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density

@androidx.annotation.Keep
class GeometryProxy: skip.lib.SwiftProjecting {
    internal val globalFramePx: Rect
    internal val density: Density
    internal val safeArea: SafeArea?

    val size: CGSize
        get() {
            return with(density) { -> CGSize(width = Double(globalFramePx.width.toDp().value), height = Double(globalFramePx.height.toDp().value)) }
        }

    val bridgedSize: Tuple2<Double, Double>
        get() {
            val size = this.size.sref()
            return Tuple2(size.width, size.height)
        }

    val bridgedSafeAreaInsets: Tuple4<Double, Double, Double, Double>
        get() {
            val insets = this.safeAreaInsets.sref()
            return Tuple4(insets.top, insets.leading, insets.bottom, insets.trailing)
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    operator fun <T> get(anchor: Any): T {
        fatalError()
    }

    val safeAreaInsets: EdgeInsets
        get() {
            if (safeArea == null) {
                return EdgeInsets()
            }
            return with(density) l@{ ->
                val presentation = safeArea.presentationBoundsPx.sref()
                val safe = safeArea.safeBoundsPx.sref()
                return@l EdgeInsets(top = Double((safe.top - presentation.top).toDp().value), leading = Double((safe.left - presentation.left).toDp().value), bottom = Double((presentation.bottom - safe.bottom).toDp().value), trailing = Double((presentation.right - safe.right).toDp().value))
            }
        }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun bounds(of: NamedCoordinateSpace): CGRect? {
        val coordinateSpace = of
        fatalError()
    }

    fun frame(in_: CoordinateSpaceProtocol): CGRect {
        val coordinateSpace = in_
        if (coordinateSpace.coordinateSpace.isGlobal) {
            return with(density) { -> CGRect(x = Double(globalFramePx.left.toDp().value), y = Double(globalFramePx.top.toDp().value), width = Double(globalFramePx.width.toDp().value), height = Double(globalFramePx.height.toDp().value)) }
        } else {
            return CGRect(origin = CGPoint.zero, size = size)
        }
    }

    fun frame(bridgedCoordinateSpace: Int, name: Any?): Tuple4<Double, Double, Double, Double> {
        val coordinateSpace = CoordinateSpaceProtocolFrom(bridged = bridgedCoordinateSpace, name = name as? AnyHashable)
        val frame = this.frame(in_ = coordinateSpace)
        return Tuple4(frame.origin.x, frame.origin.y, frame.width, frame.height)
    }

    internal constructor(globalFramePx: Rect, density: Density, safeArea: SafeArea? = null) {
        this.globalFramePx = globalFramePx.sref()
        this.density = density.sref()
        this.safeArea = safeArea
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

