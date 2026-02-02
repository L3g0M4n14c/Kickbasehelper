package skip.ui

import skip.lib.*
import skip.lib.Array
import skip.lib.MutableCollection

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.runtime.Composable

@androidx.annotation.Keep
class ForEach: View, Renderable, LazyItemFactory, skip.lib.SwiftProjecting {
    internal val identifier: ((Any) -> AnyHashable?)?
    internal val indexRange: (() -> IntRange)?
    internal val indexedContent: ((Int) -> View)?
    internal val objects: RandomAccessCollection<Any>?
    internal val objectContent: ((Any) -> View)?
    internal val objectsBinding: Binding<RandomAccessCollection<Any>>?
    internal val objectsBindingContent: ((Binding<RandomAccessCollection<Any>>, Int) -> View)?
    internal val editActions: EditActions
    internal var onDeleteAction: ((IntSet) -> Unit)? = null
    internal var onMoveAction: ((IntSet, Int) -> Unit)? = null

    internal constructor(identifier: ((Any) -> AnyHashable?)? = null, indexRange: (() -> IntRange)? = null, indexedContent: ((Int) -> View)? = null, objects: RandomAccessCollection<Any>? = null, objectContent: ((Any) -> View)? = null, objectsBinding: Binding<RandomAccessCollection<Any>>? = null, objectsBindingContent: ((Binding<RandomAccessCollection<Any>>, Int) -> View)? = null, editActions: EditActions = EditActions.of()) {
        this.identifier = identifier
        this.indexRange = indexRange
        this.indexedContent = indexedContent
        this.objects = objects.sref()
        this.objectContent = objectContent
        this.objectsBinding = objectsBinding.sref()
        this.objectsBindingContent = objectsBindingContent
        this.editActions = editActions.sref()
    }

    constructor(startIndex: () -> Int, endIndex: () -> Int, identifier: ((Int) -> AnyHashable)?, bridgedContent: (Int) -> View) {
        this.identifier = if (identifier == null) null else { it -> identifier!!(it as Int) }
        // We use start and end index closures so that the values are up to date if the data is mutated. When
        // bridging we perform the mutations by assigning delete/move actions rather than passing `EditActions`
        this.indexRange = { -> startIndex() until endIndex() }
        this.indexedContent = bridgedContent
        this.objects = null
        this.objectContent = null
        this.objectsBinding = null
        this.objectsBindingContent = null
        this.editActions = EditActions.of()
    }

    fun onDelete(perform: ((IntSet) -> Unit)?): ForEach {
        val action = perform
        onDeleteAction = action
        return this
    }

    fun onDeleteArray(bridgedAction: ((Array<Int>) -> Unit)?): ForEach {
        val action: ((IntSet) -> Unit)?
        if (bridgedAction != null) {
            action = { it -> bridgedAction(Array(it)) }
        } else {
            action = null
        }
        return onDelete(perform = action)
    }

    fun onMove(perform: ((IntSet, Int) -> Unit)?): ForEach {
        val action = perform
        onMoveAction = action
        return this
    }

    fun onMoveArray(bridgedAction: ((Array<Int>, Int) -> Unit)?): ForEach {
        val action: ((IntSet, Int) -> Unit)?
        if (bridgedAction != null) {
            action = { it, it_1 -> bridgedAction(Array(it), it_1) }
        } else {
            action = null
        }
        return onMove(perform = action)
    }

    @Composable
    override fun Evaluate(context: ComposeContext, options: Int): kotlin.collections.List<Renderable> {
        if (EvaluateOptions(options).isKeepForEach) {
            return listOf(this)
        }
        val isLazy = EvaluateOptions(options).lazyItemLevel != null

        // ForEach views might contain nested lazy item factories such as Sections or other ForEach instances. They also
        // might contain more than one view per iteration, which isn't supported by Compose lazy processing. We execute
        // our content closure for the first item in the ForEach and examine its content to see if it should be unrolled
        // If it should, we perform the full ForEach to append all items. If not, we append ourselves instead so that we
        // can take advantage of Compose's ability to specify ranges of items
        var isFirst = true
        var collected: kotlin.collections.MutableList<Renderable> = mutableListOf()
        val matchtarget_0 = indexRange
        if (matchtarget_0 != null) {
            val indexRange = matchtarget_0
            for (index in indexRange()) {
                var renderables = indexedContent!!(index).Evaluate(context = context, options = options)
                if (isLazy && !isUnrollRequired(renderables = renderables, isFirst = isFirst, context = context)) {
                    collected.add(this)
                    break
                } else {
                    isFirst = false
                }
                val defaultTag: Any?
                val matchtarget_1 = identifier
                if (matchtarget_1 != null) {
                    val identifier = matchtarget_1
                    defaultTag = identifier(index)
                } else {
                    defaultTag = index
                }
                renderables = renderables.map { it -> taggedRenderable(for_ = it, defaultTag = defaultTag) }
                collected.addAll(renderables)
            }
        } else if (objects != null) {
            for (object_ in objects.sref()) {
                var renderables = objectContent!!(object_).Evaluate(context = context, options = options)
                if (isLazy && !isUnrollRequired(renderables = renderables, isFirst = isFirst, context = context)) {
                    collected.add(this)
                    break
                } else {
                    isFirst = false
                }
                identifier?.let { identifier ->
                    renderables = renderables.map { it -> taggedRenderable(for_ = it, defaultTag = identifier(object_)) }
                }
                collected.addAll(renderables)
            }
        } else if (objectsBinding != null) {
            val objects = objectsBinding.wrappedValue.sref()
            for (i in 0 until objects.count) {
                var renderables = objectsBindingContent!!(objectsBinding, i).Evaluate(context = context, options = options)
                if (isLazy && !isUnrollRequired(renderables = renderables, isFirst = isFirst, context = context)) {
                    collected.add(this)
                    break
                } else {
                    isFirst = false
                }
                identifier?.let { identifier ->
                    renderables = renderables.map { it -> taggedRenderable(for_ = it, defaultTag = identifier(objects[i])) }
                }
                collected.addAll(renderables)
            }
        }
        return collected.sref()
    }

    @Composable
    override fun Render(context: ComposeContext) {
        fatalError()
    }

    /// If there aren't explicit `.tag` modifiers on `ForEach` content, we can potentially find the matching
    /// renderable for a tag without having to unroll.
    ///
    /// - Seealso: `Picker`
    @Composable
    internal fun untaggedRenderable(forTag: Any?, context: ComposeContext): Renderable? {
        val tag = forTag
        // Evaluate the view generated by the first item to see if our body produces tagged views
        var firstView: View? = null
        val matchtarget_2 = indexRange
        if (matchtarget_2 != null) {
            val indexRange = matchtarget_2
            val matchtarget_3 = indexRange().first
            if (matchtarget_3 != null) {
                val first = matchtarget_3
                firstView = indexedContent!!(first)
            } else {
                if (objects != null) {
                    val matchtarget_4 = objects.first
                    if (matchtarget_4 != null) {
                        val first = matchtarget_4
                        firstView = objectContent!!(first)
                    } else if (objectsBinding != null) {
                        val objects = objectsBinding.wrappedValue.sref()
                        if (!objects.isEmpty) {
                            firstView = objectsBindingContent!!(objectsBinding, 0)
                        }
                    }
                } else if (objectsBinding != null) {
                    val objects = objectsBinding.wrappedValue.sref()
                    if (!objects.isEmpty) {
                        firstView = objectsBindingContent!!(objectsBinding, 0)
                    }
                }
            }
        } else {
            if (objects != null) {
                val matchtarget_4 = objects.first
                if (matchtarget_4 != null) {
                    val first = matchtarget_4
                    firstView = objectContent!!(first)
                } else if (objectsBinding != null) {
                    val objects = objectsBinding.wrappedValue.sref()
                    if (!objects.isEmpty) {
                        firstView = objectsBindingContent!!(objectsBinding, 0)
                    }
                }
            } else if (objectsBinding != null) {
                val objects = objectsBinding.wrappedValue.sref()
                if (!objects.isEmpty) {
                    firstView = objectsBindingContent!!(objectsBinding, 0)
                }
            }
        }
        if (firstView == null) {
            return null
        }
        val renderables = firstView.Evaluate(context = context, options = 0)
        if (renderables.any({ it -> TagModifier.on(content = it, role = ModifierRole.tag) != null })) {
            return null
        }

        // If we do not produce tagged views, then we can match the supplied tag against our id function
        val matchtarget_5 = indexRange
        if (matchtarget_5 != null) {
            val indexRange = matchtarget_5
            val matchtarget_6 = tag as? Int
            if (matchtarget_6 != null) {
                val index = matchtarget_6
                if (indexRange().contains(index)) {
                    return indexedContent!!(index).Evaluate(context = context, options = 0).firstOrNull()
                } else {
                    if (objects != null) {
                        val matchtarget_7 = identifier
                        if (matchtarget_7 != null) {
                            val identifier = matchtarget_7
                            for (object_ in objects.sref()) {
                                val id = identifier(object_)
                                if (id == tag) {
                                    return objectContent!!(object_).Evaluate(context = context, options = 0).firstOrNull()
                                }
                            }
                        } else {
                            if (objectsBinding != null) {
                                identifier?.let { identifier ->
                                    val objects = objectsBinding.wrappedValue.sref()
                                    for (i in 0 until objects.count) {
                                        val id = identifier(objects[i])
                                        if (id == tag) {
                                            return objectsBindingContent!!(objectsBinding, i).Evaluate(context = context, options = 0).firstOrNull()
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (objectsBinding != null) {
                            identifier?.let { identifier ->
                                val objects = objectsBinding.wrappedValue.sref()
                                for (i in 0 until objects.count) {
                                    val id = identifier(objects[i])
                                    if (id == tag) {
                                        return objectsBindingContent!!(objectsBinding, i).Evaluate(context = context, options = 0).firstOrNull()
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (objects != null) {
                    val matchtarget_7 = identifier
                    if (matchtarget_7 != null) {
                        val identifier = matchtarget_7
                        for (object_ in objects.sref()) {
                            val id = identifier(object_)
                            if (id == tag) {
                                return objectContent!!(object_).Evaluate(context = context, options = 0).firstOrNull()
                            }
                        }
                    } else {
                        if (objectsBinding != null) {
                            identifier?.let { identifier ->
                                val objects = objectsBinding.wrappedValue.sref()
                                for (i in 0 until objects.count) {
                                    val id = identifier(objects[i])
                                    if (id == tag) {
                                        return objectsBindingContent!!(objectsBinding, i).Evaluate(context = context, options = 0).firstOrNull()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (objectsBinding != null) {
                        identifier?.let { identifier ->
                            val objects = objectsBinding.wrappedValue.sref()
                            for (i in 0 until objects.count) {
                                val id = identifier(objects[i])
                                if (id == tag) {
                                    return objectsBindingContent!!(objectsBinding, i).Evaluate(context = context, options = 0).firstOrNull()
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (objects != null) {
                val matchtarget_7 = identifier
                if (matchtarget_7 != null) {
                    val identifier = matchtarget_7
                    for (object_ in objects.sref()) {
                        val id = identifier(object_)
                        if (id == tag) {
                            return objectContent!!(object_).Evaluate(context = context, options = 0).firstOrNull()
                        }
                    }
                } else {
                    if (objectsBinding != null) {
                        identifier?.let { identifier ->
                            val objects = objectsBinding.wrappedValue.sref()
                            for (i in 0 until objects.count) {
                                val id = identifier(objects[i])
                                if (id == tag) {
                                    return objectsBindingContent!!(objectsBinding, i).Evaluate(context = context, options = 0).firstOrNull()
                                }
                            }
                        }
                    }
                }
            } else {
                if (objectsBinding != null) {
                    identifier?.let { identifier ->
                        val objects = objectsBinding.wrappedValue.sref()
                        for (i in 0 until objects.count) {
                            val id = identifier(objects[i])
                            if (id == tag) {
                                return objectsBindingContent!!(objectsBinding, i).Evaluate(context = context, options = 0).firstOrNull()
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    @Composable
    private fun isUnrollRequired(renderables: kotlin.collections.List<Renderable>, isFirst: Boolean, context: ComposeContext): Boolean {
        if (!isFirst) {
            return true
        }
        // We have to unroll if the ForEach body contains multiple views. We also unroll if this is
        // e.g. a ForEach of Sections which each append lazy items
        return renderables.size > 1 || (renderables.firstOrNull() as? LazyItemFactory)?.shouldProduceLazyItems() == true
    }

    override fun produceLazyItems(collector: LazyItemCollector, modifiers: kotlin.collections.List<ModifierProtocol>, level: Int) {
        val matchtarget_8 = indexRange
        if (matchtarget_8 != null) {
            val indexRange = matchtarget_8
            val factory: @Composable (Int, ComposeContext) -> Renderable = l@{ index, context ->
                val renderables = ModifiedContent.apply(modifiers = modifiers, to = indexedContent!!(index)).Evaluate(context = context, options = 0)
                val renderable = renderables.firstOrNull() ?: EmptyView()
                val tag: Any?
                val matchtarget_9 = identifier
                if (matchtarget_9 != null) {
                    val identifier = matchtarget_9
                    tag = identifier!!(index)
                } else {
                    tag = index
                }
                return@l taggedRenderable(for_ = renderable, defaultTag = tag)
            }
            collector.indexedItems(indexRange(), identifier, onDeleteAction, onMoveAction, level, factory)
        } else if (objects != null) {
            val factory: @Composable (Any, ComposeContext) -> Renderable = l@{ object_, context ->
                val renderables = ModifiedContent.apply(modifiers = modifiers, to = objectContent!!(object_)).Evaluate(context = context, options = 0)
                val renderable = renderables.firstOrNull() ?: EmptyView()
                val tag_0 = identifier!!(object_)
                if (tag_0 == null) {
                    return@l renderable
                }
                return@l taggedRenderable(for_ = renderable, defaultTag = tag_0)
            }
            collector.objectItems(objects, identifier!!, onDeleteAction, onMoveAction, level, factory)
        } else if (objectsBinding != null) {
            val factory: @Composable (Binding<RandomAccessCollection<Any>>, Int, ComposeContext) -> Renderable = l@{ objects, index, context ->
                val renderables = ModifiedContent.apply(modifiers = modifiers, to = objectsBindingContent!!(objects, index)).Evaluate(context = context, options = 0)
                val renderable = renderables.firstOrNull() ?: EmptyView()
                val tag_1 = identifier!!(objects.wrappedValue[index])
                if (tag_1 == null) {
                    return@l renderable
                }
                return@l taggedRenderable(for_ = renderable, defaultTag = tag_1)
            }
            collector.objectBindingItems(objectsBinding, identifier!!, editActions, onDeleteAction, onMoveAction, level, factory)
        }
    }

    private fun taggedRenderable(for_: Renderable, defaultTag: Any?): Renderable {
        val renderable = for_
        if ((defaultTag != null) && (TagModifier.on(content = renderable, role = ModifierRole.tag) == null)) {
            return ModifiedContent(content = renderable, modifier = TagModifier(value = defaultTag, role = ModifierRole.tag))
        } else {
            return renderable.sref()
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {
    }
}

// Kotlin does not support generic constructor parameters, so we have to model many ForEach constructors as functions

//extension ForEach where ID == Data.Element.ID, Content : AccessibilityRotorContent, Data.Element : Identifiable {
//    public init(_ data: Data, @AccessibilityRotorContentBuilder content: @escaping (Data.Element) -> Content) { fatalError() }
//}
fun <D> ForEach(data: RandomAccessCollection<D>, content: (D) -> View): ForEach {
    return ForEach(identifier = { it -> (it as Identifiable<Hashable>).id }, objects = data as RandomAccessCollection<Any>, objectContent = { it -> content(it as D) })
}

//extension ForEach where Content : AccessibilityRotorContent {
//    public init(_ data: Data, id: KeyPath<Data.Element, ID>, @AccessibilityRotorContentBuilder content: @escaping (Data.Element) -> Content) { fatalError() }
//}
fun <D> ForEach(data: RandomAccessCollection<D>, id: (D) -> AnyHashable?, content: (D) -> View): ForEach {
    return ForEach(identifier = { it -> id(it as D) }, objects = data as RandomAccessCollection<Any>, objectContent = { it -> content(it as D) })
}
fun ForEach(data: IntRange, id: ((Int) -> AnyHashable?)? = null, content: (Int) -> View): ForEach {
    return ForEach(identifier = if (id == null) null else { it -> id!!(it as Int) }, indexRange = { -> data }, indexedContent = content)
}

//extension ForEach {
//  public init<C, R>(_ data: Binding<C>, editActions: EditActions /* <C> */, @ViewBuilder content: @escaping (Binding<C.Element>) -> R) where Data == IndexedIdentifierCollection<C, ID>, ID == C.Element.ID, Content == EditableCollectionContent<R, C>, C : MutableCollection, C : RandomAccessCollection, R : View, C.Element : Identifiable, C.Index : Hashable
//}
fun <C, E> ForEach(data: Binding<C>, editActions: EditActions = EditActions.of(), content: (Binding<E>) -> View): ForEach where C: RandomAccessCollection<E> {
    return ForEach(identifier = { it -> (it as Identifiable<Hashable>).id }, objectsBinding = data as Binding<RandomAccessCollection<Any>>, objectsBindingContent = l@{ data, index ->
        val binding = Binding<E>(get = { -> data.wrappedValue[index] as E }, set = { it -> (data.wrappedValue as skip.lib.MutableCollection<E>)[index] = it.sref() })
        return@l content(binding)
    }, editActions = editActions)
}

//extension ForEach {
//    public init<C, R>(_ data: Binding<C>, id: KeyPath<C.Element, ID>, editActions: EditActions /* <C> */, @ViewBuilder content: @escaping (Binding<C.Element>) -> R) where Data == IndexedIdentifierCollection<C, ID>, Content == EditableCollectionContent<R, C>, C : MutableCollection, C : RandomAccessCollection, R : View, C.Index : Hashable { fatalError() }
//}
fun <C, E> ForEach(data: Binding<C>, id: (E) -> AnyHashable?, editActions: EditActions = EditActions.of(), content: (Binding<E>) -> View): ForEach where C: RandomAccessCollection<E> {
    return ForEach(identifier = { it -> id(it as E) }, objectsBinding = data as Binding<RandomAccessCollection<Any>>, objectsBindingContent = l@{ data, index ->
        val binding = Binding<E>(get = { -> data.wrappedValue[index] as E }, set = { it -> (data.wrappedValue as skip.lib.MutableCollection<E>)[index] = it.sref() })
        return@l content(binding)
    }, editActions = editActions)
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
fun ForEach(sections: View, content: (Any) -> View): ForEach {
    val view = sections
    fatalError()
}

@Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
fun ForEach(subviews: View, unusedp: Nothing? = null, content: (Any) -> View): ForEach {
    val view = subviews
    fatalError()
}

