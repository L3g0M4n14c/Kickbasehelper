package skip.foundation

import skip.lib.*
import skip.lib.Array
import skip.lib.MutableCollection
import skip.lib.Sequence

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception

@androidx.annotation.Keep
class IndexPath: Codable, Comparable<IndexPath>, MutableCollection<Int>, RandomAccessCollection<Int>, KotlinConverting<MutableList<Int>>, MutableStruct {

    private val arrayList: ArrayList<Int> = ArrayList<Int>()

    override val mutableList: MutableList<Int>
        get() = arrayList
    override fun willMutateStorage(): Unit = willmutate()
    override fun didMutateStorage(): Unit = didmutate()

    constructor() {
    }

    constructor(indexes: Sequence<Int>) {
        arrayList.addAll(indexes)
    }

    constructor(indexes: Array<Int>) {
        arrayList.addAll(indexes)
    }

    constructor(index: Int) {
        arrayList.add(index)
    }

    // Override copy constructor
    constructor(from: MutableStruct) {
        arrayList.addAll((from as IndexPath).arrayList)
    }

    constructor(from: Decoder) {
        val decoder = from
        val unkeyedContainer = decoder.unkeyedContainer()
        while ((!unkeyedContainer.isAtEnd)) {
            arrayList.add(unkeyedContainer.decode(Int::class))
        }
    }

    override fun encode(to: Encoder) {
        val encoder = to
        val unkeyedContainer = encoder.unkeyedContainer()
        unkeyedContainer.encode(contentsOf = Array(collection = arrayList))
    }

    val description: String
        get() = arrayList.description

    operator fun plus(other: IndexPath): IndexPath {
        val combined = IndexPath()
        combined.arrayList.addAll(arrayList)
        combined.arrayList.addAll(other.arrayList)
        return combined.sref()
    }

    fun dropLast(): IndexPath {
        val dropped = IndexPath()
        dropped.arrayList.addAll(arrayList)
        if (!dropped.arrayList.isEmpty()) {
            // cannot use removeLast() anymore: https://developer.android.com/about/versions/15/behavior-changes-15#openjdk-api-changes
            //dropped.arrayList.removeLast()
            dropped.arrayList.removeAt(dropped.arrayList.lastIndex)
        }
        return dropped.sref()
    }

    fun append(other: IndexPath) {
        willmutate()
        try {
            arrayList.addAll(other.arrayList)
        } finally {
            didmutate()
        }
    }

    fun append(other: Int) {
        willmutate()
        try {
            arrayList.add(other)
        } finally {
            didmutate()
        }
    }

    fun append(other: Array<Int>) {
        willmutate()
        try {
            arrayList.addAll(other)
        } finally {
            didmutate()
        }
    }

    fun appending(other: IndexPath): IndexPath {
        val copy = IndexPath()
        copy.arrayList.addAll(arrayList)
        copy.arrayList.addAll(other.arrayList)
        return copy.sref()
    }

    fun appending(other: Int): IndexPath {
        val copy = IndexPath()
        copy.arrayList.addAll(arrayList)
        copy.arrayList.add(other)
        return copy.sref()
    }

    fun appending(other: Array<Int>): IndexPath {
        val copy = IndexPath()
        copy.arrayList.addAll(arrayList)
        copy.arrayList.addAll(other)
        return copy.sref()
    }

    override operator fun get(range: IntRange): IndexPath {
        val copy = IndexPath()
        for (i in range.sref()) {
            if (i >= arrayList.size) {
                break
            }
            copy.arrayList.add(arrayList[i])
        }
        return copy.sref()
    }

    override operator fun compareTo(other: IndexPath): Int {
        if (this == other) return 0
        fun islessthan(lhs: IndexPath, rhs: IndexPath): Boolean {
            for (i in 0 until lhs.count) {
                if (rhs.count <= i) {
                    break
                } else if (lhs[i] < rhs[i]) {
                    return true
                } else if (lhs[i] > rhs[i]) {
                    return false
                }
            }
            return lhs.count < rhs.count
        }
        return if (islessthan(this, other)) -1 else 1
    }


    fun compare(with: IndexPath): ComparisonResult {
        if (this == with) {
            return ComparisonResult.orderedSame
        } else if (this < with) {
            return ComparisonResult.orderedAscending
        } else {
            return ComparisonResult.orderedDescending
        }
    }
    override fun kotlin(nocopy: Boolean): MutableList<Int> = (if (nocopy) arrayList else ArrayList(arrayList)).sref()

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = IndexPath(this as MutableStruct)

    override fun toString(): String = description

    override fun equals(other: Any?): Boolean {
        if (other !is IndexPath) return false
        return arrayList == other.arrayList
    }

    override fun hashCode(): Int {
        var result = 1
        result = Hasher.combine(result, arrayList)
        return result
    }

    @androidx.annotation.Keep
    companion object: DecodableCompanion<IndexPath> {
        override fun init(from: Decoder): IndexPath = IndexPath(from = from)
    }
}

