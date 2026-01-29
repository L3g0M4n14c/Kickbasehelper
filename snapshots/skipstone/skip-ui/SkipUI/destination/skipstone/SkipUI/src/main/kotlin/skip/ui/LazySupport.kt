package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.runtime.Composable

/// Adopted by `Renderables` that produce lazy items.
internal interface LazyItemFactory {
    fun shouldProduceLazyItems(): Boolean = true

    /// Use the given collector to add individual lazy items and ranges of items.
    fun produceLazyItems(collector: LazyItemCollector, modifiers: kotlin.collections.List<ModifierProtocol>, level: Int)
}


/// Use to render lazy items at the appropriate level.
internal class LazyLevelRenderable: Renderable, LazyItemFactory {
    internal val content: Renderable
    internal val level: Int

    internal constructor(content: Renderable, level: Int) {
        // Do not copy view
        this.content = content
        this.level = level
    }

    @Composable
    override fun Render(context: ComposeContext): Unit = content.Render(context = context)

    override fun strip(): Renderable = content.strip()

    override fun <R> forEachModifier(perform: (ModifierProtocol) -> R?): R? {
        val action = perform
        return content.forEachModifier(perform = action)
    }

    override fun produceLazyItems(collector: LazyItemCollector, modifiers: kotlin.collections.List<ModifierProtocol>, level: Int) {
        val matchtarget_0 = content as? LazyItemFactory
        if (matchtarget_0 != null) {
            val lazyItemFactory = matchtarget_0
            if (lazyItemFactory.shouldProduceLazyItems()) {
                lazyItemFactory.produceLazyItems(collector = collector, modifiers = modifiers, level = this.level)
            } else {
                collector.item(ModifiedContent.apply(modifiers = modifiers, to = content), this.level)
            }
        } else {
            collector.item(ModifiedContent.apply(modifiers = modifiers, to = content), this.level)
        }
    }
}

/// Add to lazy items to render a section header.
internal class LazySectionHeader: Renderable, LazyItemFactory {
    internal val content: Renderable

    internal constructor(content: Renderable) {
        // Do not copy view
        this.content = content
    }

    @Composable
    override fun Render(context: ComposeContext): Unit = content.Render(context = context)

    override fun produceLazyItems(collector: LazyItemCollector, modifiers: kotlin.collections.List<ModifierProtocol>, level: Int): Unit = collector.sectionHeader(content)
}

/// Add to lazy items to render a section footer.
internal class LazySectionFooter: Renderable, LazyItemFactory {
    internal val content: Renderable

    internal constructor(content: Renderable) {
        // Do not copy view
        this.content = content
    }

    @Composable
    override fun Render(context: ComposeContext): Unit = content.Render(context = context)

    override fun produceLazyItems(collector: LazyItemCollector, modifiers: kotlin.collections.List<ModifierProtocol>, level: Int): Unit = collector.sectionFooter(content)
}

/// Collect lazy content added by `LazyItemFactory` instances.
class LazyItemCollector {
    internal var item: (Renderable, Int) -> Unit = { _, _ ->  }
        private set
    internal var indexedItems: (IntRange, ((Any) -> AnyHashable?)?, ((IntSet) -> Unit)?, ((IntSet, Int) -> Unit)?, Int, @Composable (Int, ComposeContext) -> Renderable) -> Unit = { _, _, _, _, _, _ ->  }
        private set
    internal var objectItems: (RandomAccessCollection<Any>, (Any) -> AnyHashable?, ((IntSet) -> Unit)?, ((IntSet, Int) -> Unit)?, Int, @Composable (Any, ComposeContext) -> Renderable) -> Unit = { _, _, _, _, _, _ ->  }
        private set
    internal var objectBindingItems: (Binding<RandomAccessCollection<Any>>, (Any) -> AnyHashable?, EditActions, ((IntSet) -> Unit)?, ((IntSet, Int) -> Unit)?, Int, @Composable (Binding<RandomAccessCollection<Any>>, Int, ComposeContext) -> Renderable) -> Unit = { _, _, _, _, _, _, _ ->  }
        private set
    internal var sectionHeader: (Renderable) -> Unit = { _ ->  }
        private set
    internal var sectionFooter: (Renderable) -> Unit = { _ ->  }
        private set
    private var startItemIndex = 0

    /// Initialize the content factories.
    internal fun initialize(startItemIndex: Int, item: (Renderable, Int) -> Unit, indexedItems: (IntRange, ((Any) -> AnyHashable?)?, Int, ((IntSet) -> Unit)?, ((IntSet, Int) -> Unit)?, Int, @Composable (Int, ComposeContext) -> Renderable) -> Unit, objectItems: (RandomAccessCollection<Any>, (Any) -> AnyHashable?, Int, ((IntSet) -> Unit)?, ((IntSet, Int) -> Unit)?, Int, @Composable (Any, ComposeContext) -> Renderable) -> Unit, objectBindingItems: (Binding<RandomAccessCollection<Any>>, (Any) -> AnyHashable?, Int, EditActions, ((IntSet) -> Unit)?, ((IntSet, Int) -> Unit)?, Int, @Composable (Binding<RandomAccessCollection<Any>>, Int, ComposeContext) -> Renderable) -> Unit, sectionHeader: (Renderable) -> Unit, sectionFooter: (Renderable) -> Unit) {
        this.startItemIndex = startItemIndex

        content.removeAll()
        this.item = { renderable, level ->
            // If this is an item after a section, add a header before it
            if (content.last is LazyItemCollector.Content.SectionFooterCase) {
                this.sectionHeader(EmptyView())
            }
            item(renderable, level)
            val id = TagModifier.on(content = renderable, role = ModifierRole.id)?.value.sref()
            content.append(LazyItemCollector.Content.items(0, 1, { _ -> id }, null))
        }
        this.indexedItems = { range, identifier, onDelete, onMove, level, factory ->
            if (content.last is LazyItemCollector.Content.SectionFooterCase) {
                this.sectionHeader(EmptyView())
            }
            indexedItems(range, identifier, count, onDelete, onMove, level, factory)
            content.append(LazyItemCollector.Content.items(range.start, range.endExclusive - range.start, identifier, onMove))
        }
        this.objectItems = { objects, identifier, onDelete, onMove, level, factory ->
            if (content.last is LazyItemCollector.Content.SectionFooterCase) {
                this.sectionHeader(EmptyView())
            }
            objectItems(objects, identifier, count, onDelete, onMove, level, factory)
            content.append(LazyItemCollector.Content.objectItems(objects, identifier, onMove))
        }
        this.objectBindingItems = { binding, identifier, editActions, onDelete, onMove, level, factory ->
            if (content.last is LazyItemCollector.Content.SectionFooterCase) {
                this.sectionHeader(EmptyView())
            }
            objectBindingItems(binding, identifier, count, editActions, onDelete, onMove, level, factory)
            content.append(LazyItemCollector.Content.objectBindingItems(binding, identifier, onMove))
        }
        this.sectionHeader = { renderable ->
            // If this is a header after an item, add a section footer before it
            for (unusedi in 0..0) {
                when (content.last) {
                    is LazyItemCollector.Content.SectionFooterCase, null -> break
                    else -> this.sectionFooter(EmptyView())
                }
            }
            sectionHeader(renderable)
            content.append(LazyItemCollector.Content.sectionHeader)
        }
        this.sectionFooter = { renderable ->
            sectionFooter(renderable)
            content.append(LazyItemCollector.Content.sectionFooter)
        }
    }

    /// The current number of content items.
    internal val count: Int
        get() {
            var itemCount = 0
            for (content in this.content.sref()) {
                when (content) {
                    is LazyItemCollector.Content.ItemsCase -> {
                        val count = content.associated1
                        itemCount += count
                    }
                    is LazyItemCollector.Content.ObjectItemsCase -> {
                        val objects = content.associated0
                        itemCount += objects.count
                    }
                    is LazyItemCollector.Content.ObjectBindingItemsCase -> {
                        val binding = content.associated0
                        itemCount += binding.wrappedValue.count
                    }
                    is LazyItemCollector.Content.SectionHeaderCase, is LazyItemCollector.Content.SectionFooterCase -> itemCount += 1
                }
            }
            return itemCount
        }

    /// Return the list index for the given item ID, or nil.
    internal fun index(for_: Any): Int? {
        val id = for_
        var index = startItemIndex
        for (content in this.content.sref()) {
            when (content) {
                is LazyItemCollector.Content.ItemsCase -> {
                    val start = content.associated0
                    val count = content.associated1
                    val idMap = content.associated2
                    for (i in start until (start + count)) {
                        val itemID: Any?
                        if (idMap != null) {
                            itemID = idMap(i)
                        } else {
                            itemID = i
                        }
                        if (itemID == id) {
                            return index
                        }
                        index += 1
                    }
                }
                is LazyItemCollector.Content.ObjectItemsCase -> {
                    val objects = content.associated0
                    val idMap = content.associated1
                    for (object_ in objects.sref()) {
                        val itemID = idMap(object_)
                        if (itemID == id) {
                            return index
                        }
                        index += 1
                    }
                }
                is LazyItemCollector.Content.ObjectBindingItemsCase -> {
                    val binding = content.associated0
                    val idMap = content.associated1
                    for (object_ in binding.wrappedValue.sref()) {
                        val itemID = idMap(object_)
                        if (itemID == id) {
                            return index
                        }
                        index += 1
                    }
                }
                is LazyItemCollector.Content.SectionHeaderCase, is LazyItemCollector.Content.SectionFooterCase -> index += 1
            }
        }
        return null
    }

    private var moving: Tuple2<Int, Int>? = null
    private var moveTrigger = 0

    /// Re-map indexes for any in-progress operations.
    internal fun remapIndex(index: Int, from: Int): Int {
        val offset = from
        val moving_0 = moving
        if (moving_0 == null) {
            return index
        }
        // While a move is in progress we have to make the list appear reordered even though we don't change
        // the underlying data until the user ends the drag
        val offsetIndex = index + offset + startItemIndex
        if (offsetIndex == moving_0.toIndex) {
            return moving_0.fromIndex - offset - startItemIndex
        }
        if (moving_0.fromIndex < moving_0.toIndex && offsetIndex >= moving_0.fromIndex && offsetIndex < moving_0.toIndex) {
            return index + 1
        } else if (moving_0.fromIndex > moving_0.toIndex && offsetIndex > moving_0.toIndex && offsetIndex <= moving_0.fromIndex) {
            return index - 1
        } else {
            return index
        }
    }

    /// Commit the current active move operation, if any.
    internal fun commitMove() {
        val moving_1 = moving
        if (moving_1 == null) {
            return
        }
        val fromIndex = moving_1.fromIndex
        val toIndex = moving_1.toIndex
        this.moving = null
        performMove(fromIndex = fromIndex, toIndex = toIndex)
    }

    /// Call this function during an active move operation with the current move progress.
    internal fun move(from: Int, to: Int, trigger: (Int) -> Unit) {
        val fromIndex = from
        val toIndex = to
        if (moving == null) {
            if (fromIndex != toIndex) {
                moving = Tuple2(fromIndex, toIndex)
                trigger(++moveTrigger) // Trigger recompose to see change
            }
        } else {
            // Keep the original fromIndex, not the in-progress one. The framework assumes we move one position at a time
            if (moving!!.fromIndex == toIndex) {
                moving = null
            } else {
                moving = Tuple2(moving!!.fromIndex, toIndex)
            }
            trigger(++moveTrigger) // Trigger recompose to see change
        }
    }

    private fun performMove(fromIndex: Int, toIndex: Int) {
        var itemIndex = startItemIndex
        for (content in this.content.sref()) {
            when (content) {
                is LazyItemCollector.Content.ItemsCase -> {
                    val count = content.associated1
                    val onMove = content.associated3
                    if (performMove(fromIndex = fromIndex, toIndex = toIndex, itemIndex = InOut({ itemIndex }, { itemIndex = it }), count = count, onMove = onMove)) {
                        return
                    }
                }
                is LazyItemCollector.Content.ObjectItemsCase -> {
                    val objects = content.associated0
                    val onMove = content.associated2
                    if (performMove(fromIndex = fromIndex, toIndex = toIndex, itemIndex = InOut({ itemIndex }, { itemIndex = it }), count = objects.count, onMove = onMove)) {
                        return
                    }
                }
                is LazyItemCollector.Content.ObjectBindingItemsCase -> {
                    val binding = content.associated0
                    val onMove = content.associated2
                    if (performMove(fromIndex = fromIndex, toIndex = toIndex, itemIndex = InOut({ itemIndex }, { itemIndex = it }), count = binding.wrappedValue.count, onMove = onMove, customMove = { ->
                        (binding.wrappedValue as? RangeReplaceableCollection<Any>)?.remove(at = fromIndex - itemIndex)?.let { element ->
                            (binding.wrappedValue as? RangeReplaceableCollection<Any>)?.insert(element, at = toIndex - itemIndex)
                        }
                    })) {
                        return
                    }
                }
                is LazyItemCollector.Content.SectionHeaderCase, is LazyItemCollector.Content.SectionFooterCase -> {
                    if (performMove(fromIndex = fromIndex, toIndex = toIndex, itemIndex = InOut({ itemIndex }, { itemIndex = it }), count = 1, onMove = null)) {
                        return
                    }
                }
            }
        }
    }

    private fun performMove(fromIndex: Int, toIndex: Int, itemIndex: InOut<Int>, count: Int, onMove: ((IntSet, Int) -> Unit)?, customMove: (() -> Unit)? = null): Boolean {
        if (min(fromIndex, toIndex) < itemIndex.value || max(fromIndex, toIndex) >= itemIndex.value + count) {
            itemIndex.value += count
            return false
        }
        if (onMove != null) {
            val indexSet = IntSet(integer = fromIndex - itemIndex.value)
            onMove(indexSet, if (fromIndex < toIndex) toIndex - itemIndex.value + 1 else toIndex - itemIndex.value)
        } else if (customMove != null) {
            customMove()
        }
        return true
    }

    /// Whether a given move would be permitted.
    internal fun canMove(from: Int, to: Int): Boolean {
        val fromIndex = from
        val toIndex = to
        if (fromIndex == toIndex) {
            return true
        }
        var itemIndex = startItemIndex
        for (content in this.content.sref()) {
            when (content) {
                is LazyItemCollector.Content.ItemsCase -> {
                    val count = content.associated1
                    canMove(fromIndex = fromIndex, toIndex = toIndex, itemIndex = InOut({ itemIndex }, { itemIndex = it }), count = count)?.let { ret ->
                        return ret
                    }
                }
                is LazyItemCollector.Content.ObjectItemsCase -> {
                    val objects = content.associated0
                    canMove(fromIndex = fromIndex, toIndex = toIndex, itemIndex = InOut({ itemIndex }, { itemIndex = it }), count = objects.count)?.let { ret ->
                        return ret
                    }
                }
                is LazyItemCollector.Content.ObjectBindingItemsCase -> {
                    val binding = content.associated0
                    canMove(fromIndex = fromIndex, toIndex = toIndex, itemIndex = InOut({ itemIndex }, { itemIndex = it }), count = binding.wrappedValue.count)?.let { ret ->
                        return ret
                    }
                }
                is LazyItemCollector.Content.SectionHeaderCase, is LazyItemCollector.Content.SectionFooterCase -> {
                    canMove(fromIndex = fromIndex, toIndex = toIndex, itemIndex = InOut({ itemIndex }, { itemIndex = it }), count = 1)?.let { ret ->
                        return ret
                    }
                }
            }
        }
        return false
    }

    private fun canMove(fromIndex: Int, toIndex: Int, itemIndex: InOut<Int>, count: Int): Boolean? {
        if (fromIndex >= itemIndex.value && fromIndex < itemIndex.value + count) {
            return toIndex >= itemIndex.value && toIndex < itemIndex.value + count
        } else {
            itemIndex.value += count
            return null
        }
    }

    private sealed class Content {
        class ItemsCase(val associated0: Int, val associated1: Int, val associated2: ((Int) -> AnyHashable?)?, val associated3: ((IntSet, Int) -> Unit)?): Content() {
        }
        class ObjectItemsCase(val associated0: RandomAccessCollection<Any>, val associated1: (Any) -> AnyHashable?, val associated2: ((IntSet, Int) -> Unit)?): Content() {
        }
        class ObjectBindingItemsCase(val associated0: Binding<RandomAccessCollection<Any>>, val associated1: (Any) -> AnyHashable?, val associated2: ((IntSet, Int) -> Unit)?): Content() {
        }
        class SectionHeaderCase: Content() {
        }
        class SectionFooterCase: Content() {
        }

        @androidx.annotation.Keep
        companion object {
            fun items(associated0: Int, associated1: Int, associated2: ((Int) -> AnyHashable?)?, associated3: ((IntSet, Int) -> Unit)?): Content = ItemsCase(associated0, associated1, associated2, associated3)
            fun objectItems(associated0: RandomAccessCollection<Any>, associated1: (Any) -> AnyHashable?, associated2: ((IntSet, Int) -> Unit)?): Content = ObjectItemsCase(associated0, associated1, associated2)
            fun objectBindingItems(associated0: Binding<RandomAccessCollection<Any>>, associated1: (Any) -> AnyHashable?, associated2: ((IntSet, Int) -> Unit)?): Content = ObjectBindingItemsCase(associated0, associated1, associated2)
            val sectionHeader: Content = SectionHeaderCase()
            val sectionFooter: Content = SectionFooterCase()
        }
    }
    private var content: Array<LazyItemCollector.Content> = arrayOf()
        get() = field.sref({ this.content = it })
        set(newValue) {
            field = newValue.sref()
        }

    @androidx.annotation.Keep
    companion object {
    }
}

