package skip.foundation

import skip.lib.*
import skip.lib.Array
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
internal fun SkipFoundationInternalModuleName(): String = "SkipFoundation"

fun SkipFoundationPublicModuleName(): String = "SkipFoundation"

/// A shim that pretends to return any `T`, but just crashes with a fatal error.
internal fun <T> SkipCrash(reason: String): T {
    return fatalError("skipme: ${reason}")
}


fun NSLog(message: String): Unit = print(message)

/// Returns an array containing only the unique elements of the array, preserving the order of the first occurrence of each element.
internal fun <Element> Array<Element>.distinctValues(): Array<Element> {
    var seen = Set<Element>()
    return filter l@{ element ->
        if (seen.contains(element)) {
            return@l false
        } else {
            seen.insert(element)
            return@l true
        }
    }
}

typealias NSObject = java.lang.Object

interface NSObjectProtocol {
}

open class NSNull {
    constructor() {
    }

    @androidx.annotation.Keep
    companion object: CompanionClass() {
        override val null_ = NSNull()
    }
    open class CompanionClass {
        open val null_
            get() = NSNull.null_
    }
}

/// The Objective-C BOOL type.
@Suppress("MUST_BE_INITIALIZED")
class ObjCBool: MutableStruct {
    var boolValue: Boolean
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    constructor(value: Boolean) {
        this.boolValue = value
    }
    constructor(booleanLiteral: Boolean, @Suppress("UNUSED_PARAMETER") unusedp_0: Nothing? = null) {
        val value = booleanLiteral
        this.boolValue = value
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as ObjCBool
        this.boolValue = copy.boolValue
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = ObjCBool(this as MutableStruct)

    @androidx.annotation.Keep
    companion object {
    }
}

// MARK: Foundation Stubs

internal class EnergyFormatter {
}

internal class LengthFormatter {
}

internal class MassFormatter {
}

internal interface SocketPort {
}

internal interface PersonNameComponents {
}

open class NSCoder: java.lang.Object() {

    @androidx.annotation.Keep
    companion object: CompanionClass() {
    }
    open class CompanionClass {
    }
}

internal interface NSRange {
}

internal open class NSArray: java.lang.Object() {
}

internal open class NSMutableArray: NSArray() {
}

internal open class NSPredicate: java.lang.Object() {
}

internal open class NSTextCheckingResult: java.lang.Object() {
}

internal interface NSBinarySearchingOptions {
}

