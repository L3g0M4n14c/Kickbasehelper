package skip.foundation

import skip.lib.*
import skip.lib.Set

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.parser.Parser

/// A node in a markdown AST.
class MarkdownNode {

    enum class NodeType {
        bold,
        code,
        italic,
        link,
        paragraph,
        root,
        strikethrough,
        text,
        unknown;

        @androidx.annotation.Keep
        companion object {

            internal val markdownCharacters: Set<Char> = setOf('*', '_', '`', '[', '~')

            internal fun hasMarkdown(node: Node?): Boolean {
                if (node == null) {
                    return false
                }
                if (node is StrongEmphasis || node is Code || node is Emphasis || node is Link || node is Strikethrough) {
                    return true
                }
                var child = node.firstChild.sref()
                while (child != null) {
                    if (hasMarkdown(child)) {
                        return true
                    }
                    child = child?.next.sref()
                }
                return false
            }
        }
    }

    val type: MarkdownNode.NodeType
    /// The string is converted to use Kotlin/Java-style format specifiers.
    val string: String?
    val interpolationIndexes: List<Int>?
    val children: List<MarkdownNode>?

    fun format(interpolations: List<AnyHashable>?): MarkdownNode {
        if ((interpolations == null) || interpolations.isEmpty()) {
            return this
        }
        val string = formattedString(interpolations)
        return MarkdownNode(type = type, string = string, interpolationIndexes = null, children = children?.map { it -> it.format(interpolations) })
    }

    fun formattedString(interpolations: List<AnyHashable>?): String? {
        if ((string == null) || (interpolationIndexes == null) || (interpolations == null)) {
            return this.string
        }
        val stringInterpolations = mutableListOf<AnyHashable>()
        for (index in interpolationIndexes.sref()) {
            if (interpolations.count() >= index) {
                stringInterpolations.add(interpolations[index - 1])
            }
        }
        return string.format(*stringInterpolations.toTypedArray())
    }

    private constructor(type: MarkdownNode.NodeType, string: String?, interpolationIndexes: List<Int>?, children: List<MarkdownNode>?) {
        this.type = type
        this.string = string
        this.interpolationIndexes = interpolationIndexes.sref()
        this.children = children.sref()
    }

    private constructor(node: Node, interpolationInfo: MarkdownNode.InterpolationInfo = InterpolationInfo()) {
        val matchtarget_0 = node as? Text
        if (matchtarget_0 != null) {
            val text = matchtarget_0
            this.type = NodeType.text
            val (string, indexes) = text.literal.kotlinFormatInfo(interpolationIndex = interpolationInfo.index, removePositions = true)
            interpolationInfo.update(for_ = indexes)
            this.string = string
            this.interpolationIndexes = indexes.sref()
            this.children = null
        } else {
            val matchtarget_1 = node as? Code
            if (matchtarget_1 != null) {
                val code = matchtarget_1
                this.type = NodeType.code
                val (string, indexes) = code.literal.kotlinFormatInfo(interpolationIndex = interpolationInfo.index, removePositions = true)
                interpolationInfo.update(for_ = indexes)
                this.string = string
                this.interpolationIndexes = indexes.sref()
                this.children = null
            } else if (node is Emphasis) {
                this.type = NodeType.italic
                this.string = null
                this.interpolationIndexes = null
                this.children = Companion.processChildren(node, interpolationInfo = interpolationInfo)
            } else if (node is StrongEmphasis) {
                this.type = NodeType.bold
                this.string = null
                this.interpolationIndexes = null
                this.children = Companion.processChildren(node, interpolationInfo = interpolationInfo)
            } else if (node is Strikethrough) {
                this.type = NodeType.strikethrough
                this.string = null
                this.interpolationIndexes = null
                this.children = Companion.processChildren(node, interpolationInfo = interpolationInfo)
            } else {
                val matchtarget_2 = node as? Link
                if (matchtarget_2 != null) {
                    val link = matchtarget_2
                    this.type = NodeType.link
                    // Process children before destination because they appear first in the link markdown
                    this.children = Companion.processChildren(node, interpolationInfo = interpolationInfo)
                    val (string, indexes) = link.destination.kotlinFormatInfo(interpolationIndex = interpolationInfo.index, removePositions = true)
                    interpolationInfo.update(for_ = indexes)
                    this.string = string
                    this.interpolationIndexes = indexes.sref()
                } else if (node is Document) {
                    this.type = NodeType.root
                    this.string = null
                    this.interpolationIndexes = null
                    this.children = Companion.processChildren(node, interpolationInfo = interpolationInfo)
                } else if (node is Paragraph) {
                    this.type = NodeType.paragraph
                    this.string = null
                    this.interpolationIndexes = null
                    this.children = Companion.processChildren(node, interpolationInfo = interpolationInfo)
                } else if (node is HardLineBreak || node is SoftLineBreak) {
                    this.type = NodeType.text
                    this.string = "\n"
                    this.interpolationIndexes = null
                    this.children = null
                } else {
                    this.type = NodeType.unknown
                    this.string = null
                    this.interpolationIndexes = null
                    this.children = Companion.processChildren(node, interpolationInfo = interpolationInfo)
                }
            }
        }
    }

    private class InterpolationInfo {
        internal var index = 0

        internal fun update(for_: List<Int>?) {
            val indexes = for_
            if (indexes != null) {
                index = max(index, indexes.maxOrNull() ?: 0)
            }
        }
    }

    @androidx.annotation.Keep
    companion object {
        internal val parser: Parser = Parser.builder()
            .enabledBlockTypes(emptySet())
            .extensions(listOf(StrikethroughExtension.create()))
            .build()

        /// Parse the string as markdown.
        ///
        /// - Returns: `nil` for empty or invalid string
        fun from(string: String?): MarkdownNode? {
            if ((string == null) || string.isEmpty()) {
                return null
            }
            if (!string.contains(where = { it -> NodeType.markdownCharacters.contains(it) })) {
                return null
            }
            try {
                val document_0 = (Companion.parser.parse(string) as? Document).sref()
                if ((document_0 == null) || !NodeType.hasMarkdown(document_0)) {
                    return null
                }
                return MarkdownNode(document_0)
            } catch (error: Throwable) {
                @Suppress("NAME_SHADOWING") val error = error.aserror()
                return null
            }
        }

        private fun processChildren(node: Node, interpolationInfo: MarkdownNode.InterpolationInfo): List<MarkdownNode>? {
            var current_0 = node.firstChild.sref()
            if (current_0 == null) {
                return null
            }

            var children: MutableList<MarkdownNode> = mutableListOf()
            while (current_0 != null) {
                children.add(MarkdownNode(current_0, interpolationInfo = interpolationInfo))
                current_0 = current_0.next.sref()
            }
            return children.sref()
        }
    }
}

