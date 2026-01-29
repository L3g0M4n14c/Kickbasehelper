package skip.ui

import skip.lib.*
import skip.lib.Array

// Copyright 2023–2025 Skip
// SPDX-License-Identifier: LGPL-3.0-only WITH LGPL-3.0-linking-exception
import skip.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import skip.foundation.LocalizedStringResource
import skip.foundation.Bundle
import skip.foundation.Locale

@androidx.annotation.Keep
class Text: View, Renderable, skip.lib.SwiftProjecting {
    private val textView: _Text
    private val modifiedView: View

    constructor(verbatim: String) {
        textView = _Text(verbatim = verbatim)
        modifiedView = textView
    }

    constructor(attributedContent: AttributedString) {
        textView = _Text(attributedString = attributedContent)
        modifiedView = textView
    }

    constructor(key: LocalizedStringKey, tableName: String? = null, bundle: Bundle? = Bundle.main, comment: String? = null) {
        textView = _Text(key = key, tableName = tableName, bundle = bundle)
        modifiedView = textView
    }

    constructor(resource: LocalizedStringResource) {
        textView = _Text(key = LocalizedStringKey(resource), tableName = resource.table, locale = resource.locale, bundle = resource.bundle?.bundle ?: Bundle.main)
        modifiedView = textView
    }

    constructor(key: String, tableName: String? = null, bundle: Bundle? = Bundle.main, comment: String? = null) {
        textView = _Text(key = LocalizedStringKey(stringLiteral = key), tableName = tableName, bundle = bundle)
        modifiedView = textView
    }

    constructor(date: Date, style: Text.DateStyle): this(verbatim = style.format(date)) {
    }

    constructor(keyPattern: String, keyValues: Array<*>?, tableName: String?, localeIdentifier: String?, bridgedBundle: Any?) {
        val interpolation = LocalizedStringKey.StringInterpolation(pattern = keyPattern, values = keyValues)
        val locale = if (localeIdentifier == null) null else Locale(identifier = localeIdentifier!!)
        textView = _Text(key = LocalizedStringKey(stringInterpolation = interpolation), tableName = tableName, locale = locale, bundle = bridgedBundle as? Bundle)
        modifiedView = textView
    }

    internal constructor(textView: _Text, modifiedView: View) {
        this.textView = textView
        // Don't copy view
        this.modifiedView = modifiedView
    }

    /// Interpret the key against the given bundle and the environment's current locale.
    @Composable
    fun localizedTextString(): String = textView.localizedTextString()

    @Composable
    override fun Render(context: ComposeContext) {
        modifiedView.Compose(context = context)
    }

    override fun strip(): Renderable = if (modifiedView === textView) this else Text(textView = textView, modifiedView = textView)

    override fun equals(other: Any?): Boolean {
        if (other !is Text) {
            return false
        }
        val lhs = this
        val rhs = other
        return lhs.textView == rhs.textView && lhs.modifiedView == rhs.modifiedView
    }

    // Text-specific implementations of View modifiers

    fun accessibilityLabel(label: Text): Text = Text(textView = textView, modifiedView = modifiedView.accessibilityLabel(label))

    fun accessibilityLabel(label: String): Text = Text(textView = textView, modifiedView = modifiedView.accessibilityLabel(label))

    override fun foregroundColor(color: Color?): Text = Text(textView = textView, modifiedView = modifiedView.foregroundColor(color))

    override fun foregroundStyle(style: ShapeStyle): Text = Text(textView = textView, modifiedView = modifiedView.foregroundStyle(style))

    override fun font(font: Font?): Text = Text(textView = textView, modifiedView = modifiedView.font(font))

    override fun fontWeight(weight: Font.Weight?): Text = Text(textView = textView, modifiedView = modifiedView.fontWeight(weight))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun fontWidth(width: Font.Width?): Text = this

    override fun bold(isActive: Boolean): Text = Text(textView = textView, modifiedView = modifiedView.bold(isActive))

    override fun italic(isActive: Boolean): Text = Text(textView = textView, modifiedView = modifiedView.italic(isActive))

    override fun monospaced(isActive: Boolean): Text = Text(textView = textView, modifiedView = modifiedView.monospaced(isActive))

    override fun fontDesign(design: Font.Design?): Text = Text(textView = textView, modifiedView = modifiedView.fontDesign(design))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun monospacedDigit(): Text = this

    override fun strikethrough(isActive: Boolean, pattern: Text.LineStyle.Pattern, color: Color?): Text = Text(textView = textView, modifiedView = modifiedView.strikethrough(isActive, pattern = pattern, color = color))

    override fun underline(isActive: Boolean, pattern: Text.LineStyle.Pattern, color: Color?): Text = Text(textView = textView, modifiedView = modifiedView.underline(isActive, pattern = pattern, color = color))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun kerning(kerning: Double): Text = this

    override fun tracking(tracking: Double): Text = Text(textView = textView, modifiedView = modifiedView.tracking(tracking))

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    override fun baselineOffset(baselineOffset: Double): Text = this

    enum class Case(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
        uppercase(0), // For bridging
        lowercase(1); // For bridging

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: Int): Text.Case? {
                return when (rawValue) {
                    0 -> Case.uppercase
                    1 -> Case.lowercase
                    else -> null
                }
            }
        }
    }

    class LineStyle {
        val pattern: Text.LineStyle.Pattern
        val color: Color?

        constructor(pattern: Text.LineStyle.Pattern = Text.LineStyle.Pattern.solid, color: Color? = null) {
            this.pattern = pattern
            this.color = color.sref()
        }

        enum class Pattern {
            solid,
            dot,
            dash,
            dashot,
            dashDotDot;

            @androidx.annotation.Keep
            companion object {
            }
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Text.LineStyle) return false
            return pattern == other.pattern && color == other.color
        }

        override fun hashCode(): Int {
            var result = 1
            result = Hasher.combine(result, pattern)
            result = Hasher.combine(result, color)
            return result
        }

        @androidx.annotation.Keep
        companion object {

            val single = Text.LineStyle()
        }
    }

    enum class Scale {
        default,
        secondary;

        @androidx.annotation.Keep
        companion object {
        }
    }

    enum class TruncationMode(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): RawRepresentable<Int> {
        head(1), // For bridging
        tail(2), // For bridging
        middle(3); // For bridging

        @androidx.annotation.Keep
        companion object {
            fun init(rawValue: Int): Text.TruncationMode? {
                return when (rawValue) {
                    1 -> TruncationMode.head
                    2 -> TruncationMode.tail
                    3 -> TruncationMode.middle
                    else -> null
                }
            }
        }
    }

    class DateStyle {

        internal val format: (Date) -> String

        private constructor(format: (Date) -> String) {
            this.format = format
        }

        @androidx.annotation.Keep
        companion object {
            val time = DateStyle(format = l@{ date ->
                val formatter = DateFormatter()
                formatter.timeStyle = DateFormatter.Style.medium
                return@l formatter.string(from = date)
            })

            val date = DateStyle(format = l@{ date ->
                val formatter = DateFormatter()
                formatter.dateStyle = DateFormatter.Style.medium
                return@l formatter.string(from = date)
            })

            @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
            val relative = DateStyle(format = { _ ->
                fatalError()
            })

            @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
            val offset = DateStyle(format = { _ ->
                fatalError()
            })

            @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
            val timer = DateStyle(format = { _ ->
                fatalError()
            })
        }
    }

    class WritingDirectionStrategy {

        override fun equals(other: Any?): Boolean = other is Text.WritingDirectionStrategy

        override fun hashCode(): Int = "Text.WritingDirectionStrategy".hashCode()

        @androidx.annotation.Keep
        companion object {
            @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
            val layoutBased = WritingDirectionStrategy()
            val contentBased = WritingDirectionStrategy()
            val default = WritingDirectionStrategy()
        }
    }

    class AlignmentStrategy {

        override fun equals(other: Any?): Boolean = other is Text.AlignmentStrategy

        override fun hashCode(): Int = "Text.AlignmentStrategy".hashCode()

        @androidx.annotation.Keep
        companion object {
            @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
            val layoutBased = AlignmentStrategy()
            @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
            val writingDirectionBased = AlignmentStrategy()
            val default = Text.AlignmentStrategy()
        }
    }

    override fun Swift_projection(options: Int): () -> Any = Swift_projectionImpl(options)
    private external fun Swift_projectionImpl(options: Int): () -> Any

    @androidx.annotation.Keep
    companion object {

        /// Gather text style information for the current environment.
        @Composable
        internal fun styleInfo(textEnvironment: TextEnvironment, redaction: RedactionReasons, context: ComposeContext): TextStyleInfo {
            var isUppercased = textEnvironment.textCase == Text.Case.uppercase
            val isLowercased = textEnvironment.textCase == Text.Case.lowercase
            var font: Font
            val matchtarget_0 = EnvironmentValues.shared.font
            if (matchtarget_0 != null) {
                val environmentFont = matchtarget_0
                font = environmentFont.sref()
            } else {
                val matchtarget_1 = EnvironmentValues.shared._listSectionHeaderStyle
                if (matchtarget_1 != null) {
                    val sectionHeaderStyle = matchtarget_1
                    font = Font.callout.sref()
                    if (sectionHeaderStyle == ListStyle.plain) {
                        font = font.bold()
                    } else {
                        isUppercased = true
                    }
                } else {
                    val matchtarget_2 = EnvironmentValues.shared._listSectionFooterStyle
                    if (matchtarget_2 != null) {
                        val sectionFooterStyle = matchtarget_2
                        if (sectionFooterStyle != ListStyle.plain) {
                            font = Font.footnote.sref()
                        } else {
                            font = Font(fontImpl = { -> LocalTextStyle.current })
                        }
                    } else {
                        font = Font(fontImpl = { -> LocalTextStyle.current })
                    }
                }
            }
            textEnvironment.fontWeight?.let { weight ->
                font = font.weight(weight)
            }
            textEnvironment.fontDesign?.let { design ->
                font = font.design(design)
            }
            if (textEnvironment.isItalic == true) {
                font = font.italic()
            }

            var textColor: androidx.compose.ui.graphics.Color? = null
            var textBrush: Brush? = null
            val matchtarget_3 = EnvironmentValues.shared._foregroundStyle
            if (matchtarget_3 != null) {
                val foregroundStyle = matchtarget_3
                val matchtarget_4 = foregroundStyle.asColor(opacity = 1.0, animationContext = context)
                if (matchtarget_4 != null) {
                    val color = matchtarget_4
                    textColor = color
                } else if (!redaction.isEmpty) {
                    textColor = Color.primary.colorImpl()
                } else {
                    textBrush = foregroundStyle.asBrush(opacity = 1.0, animationContext = context)
                }
            } else if (EnvironmentValues.shared._listSectionHeaderStyle != null) {
                textColor = Color.secondary.colorImpl()
            } else {
                val matchtarget_5 = EnvironmentValues.shared._listSectionFooterStyle
                if (matchtarget_5 != null) {
                    val sectionFooterStyle = matchtarget_5
                    if (sectionFooterStyle != ListStyle.plain) {
                        textColor = Color.secondary.colorImpl()
                    } else {
                        textColor = if (EnvironmentValues.shared._placement.contains(ViewPlacement.systemTextColor)) androidx.compose.ui.graphics.Color.Unspecified else Color.primary.colorImpl()
                    }
                } else {
                    textColor = if (EnvironmentValues.shared._placement.contains(ViewPlacement.systemTextColor)) androidx.compose.ui.graphics.Color.Unspecified else Color.primary.colorImpl()
                }
            }

            var style = font.fontImpl()
            // Trim the line height padding to mirror SwiftUI.Text layout. For now we only do this here on the Text component
            // rather than in Font to de-risk this aberration from Compose default text style behavior
            style = style.copy(lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both))
            if (textBrush != null) {
                style = style.copy(brush = textBrush)
            }
            if (redaction.contains(RedactionReasons.placeholder)) {
                if (textColor != null) {
                    style = style.copy(background = textColor.copy(alpha = textColor.alpha * Float(Color.placeholderOpacity)))
                }
                textColor = androidx.compose.ui.graphics.Color.Transparent
            }
            return TextStyleInfo(style = style, color = textColor, isUppercased = isUppercased, isLowercased = isLowercased)
        }

        fun Case(rawValue: Int): Text.Case? = Case.init(rawValue = rawValue)

        fun TruncationMode(rawValue: Int): Text.TruncationMode? = TruncationMode.init(rawValue = rawValue)
    }
}

internal class _Text: View, Renderable {
    internal val verbatim: String?
    internal val attributedString: AttributedString?
    internal val key: LocalizedStringKey?
    internal val tableName: String?
    internal val locale: Locale?
    internal val bundle: Bundle?

    internal constructor(verbatim: String? = null, attributedString: AttributedString? = null, key: LocalizedStringKey? = null, tableName: String? = null, locale: Locale? = null, bundle: Bundle? = null) {
        this.verbatim = verbatim
        this.attributedString = attributedString
        this.key = key.sref()
        this.tableName = tableName
        this.locale = locale
        this.bundle = bundle
    }

    @Composable
    internal fun localizedTextString(): String {
        val (locfmt, _, interpolations) = localizedTextInfo()
        if ((interpolations != null) && !interpolations.isEmpty()) {
            return locfmt.format(*interpolations.toTypedArray())
        } else {
            return locfmt
        }
    }

    @Composable
    private fun localizedTextInfo(): Tuple3<String, MarkdownNode?, kotlin.collections.List<AnyHashable>?> {
        if (verbatim != null) {
            return Tuple3(verbatim, null, null)
        }
        if (attributedString != null) {
            return Tuple3(attributedString.string, attributedString.markdownNode, null)
        }
        if (key == null) {
            return Tuple3("", null, null)
        }

        // localize and Kotlin-ize the format string. the string is cached by the bundle, and we
        // cache the Kotlin-ized version too so that we don't have to convert it on every compose
        val locale = this.locale ?: EnvironmentValues.shared.locale
        val matchtarget_6 = (this.bundle ?: Bundle.main).localizedInfo(forKey = key.patternFormat, value = null, table = this.tableName, locale = locale)
        if (matchtarget_6 != null) {
            val (_, locfmt, locnode) = matchtarget_6
            return Tuple3(locfmt.sref(), locnode.sref(), key.stringInterpolation.values.sref())
        } else {
            return Tuple3(key.patternFormat.kotlinFormatString, MarkdownNode.from(string = key.patternFormat), key.stringInterpolation.values.sref())
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Composable
    override fun Render(context: ComposeContext) {
        val (locfmt, locnode, interpolations) = localizedTextInfo()
        val textEnvironment = EnvironmentValues.shared._textEnvironment.sref()
        val textDecoration = textEnvironment.textDecoration.sref()
        val textAlign = EnvironmentValues.shared.multilineTextAlignment.asTextAlign()
        val maxLines = max(1, EnvironmentValues.shared.lineLimit ?: Int.MAX_VALUE)
        val truncationMode = EnvironmentValues.shared.truncationMode
        val hasLineLimit = (EnvironmentValues.shared.lineLimit != null)
        val reservesSpace = EnvironmentValues.shared._lineLimitReservesSpace ?: false
        val minLines = if (reservesSpace) maxLines else 1
        val redaction = EnvironmentValues.shared.redactionReasons.sref()
        val styleInfo = Text.styleInfo(textEnvironment = textEnvironment, redaction = redaction, context = context)
        val animatable = styleInfo.style.asAnimatable(context = context)
        var modifier = Modifier.flexibleWidth(max = Float.flexibleUnknownNonExpanding).then(context.modifier)
        var options: Material3TextOptions
        if (locnode != null) {
            val layoutResult = remember { -> mutableStateOf<TextLayoutResult?>(null) }
            val isPlaceholder = redaction.contains(RedactionReasons.placeholder)
            var linkColor = EnvironmentValues.shared._tint?.colorImpl?.invoke() ?: Color.accentColor.colorImpl()
            if (isPlaceholder) {
                linkColor = linkColor.copy(alpha = linkColor.alpha * Float(Color.placeholderOpacity))
            }
            val annotatedText = annotatedString(markdown = locnode, interpolations = interpolations, linkColor = linkColor, isUppercased = styleInfo.isUppercased, isLowercased = styleInfo.isLowercased, isRedacted = isPlaceholder)
            val links = annotatedText.getUrlAnnotations(start = 0, end = annotatedText.length)
            if (!links.isEmpty()) {
                val currentText = rememberUpdatedState(annotatedText)
                val currentHandler = rememberUpdatedState(EnvironmentValues.shared.openURL)
                val currentIsEnabled = rememberUpdatedState(EnvironmentValues.shared.isEnabled)
                modifier = modifier.pointerInput(true) { ->
                    detectTapGestures { pos ->
                        if (currentIsEnabled.value) {
                            layoutResult.value?.getOffsetForPosition(pos)?.let { offset ->
                                currentText.value.getUrlAnnotations(offset, offset).firstOrNull()?.item?.url.sref()?.let { urlString ->
                                    (try { URL(string = urlString) } catch (_: NullReturnException) { null })?.let { url ->
                                        currentHandler.value.invoke(url)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            options = Material3TextOptions(annotatedText = annotatedText, modifier = modifier, color = styleInfo.color ?: androidx.compose.ui.graphics.Color.Unspecified, maxLines = maxLines, minLines = minLines, style = animatable.value, textDecoration = textDecoration, textAlign = textAlign, onTextLayout = { it -> layoutResult.value = it })
        } else {
            var text: String
            if (interpolations != null) {
                text = locfmt.format(*interpolations.toTypedArray())
            } else {
                text = locfmt
            }
            if (styleInfo.isUppercased) {
                text = text.uppercased()
            } else if (styleInfo.isLowercased) {
                text = text.lowercased()
            }
            options = Material3TextOptions(text = text, modifier = modifier, color = styleInfo.color ?: androidx.compose.ui.graphics.Color.Unspecified, maxLines = maxLines, minLines = minLines, style = animatable.value, textDecoration = textDecoration, textAlign = textAlign)
        }
        textEnvironment.tracking?.let { tracking ->
            options = options.copy(letterSpacing = tracking.sp)
        }
        textEnvironment.lineSpacing?.let { lineSpacing ->
            // SwiftUI lineSpacing adds extra space between lines (in points)
            // Compose lineHeight is total height - we approximate by converting lineSpacing to em
            // and adding to a base of 1.0em (normal line height)
            val lineHeightEm = 1.0 + (lineSpacing / 16.0) // Approximate: 16pt = 1em
            options = options.copy(lineHeight = lineHeightEm.em)
        }
        textEnvironment.minimumScaleFactor?.let { factor ->
            // Calculate minFontSize based on current style's fontSize
            // TextAutoSize.StepBased finds the LARGEST font that fits, so we need to set both
            // minFontSize (based on factor) and maxFontSize (current size) to constrain the range
            val currentFontSize = options.style.fontSize.sref()
            if (currentFontSize != TextUnit.Unspecified) {
                val minFontSize = currentFontSize * Float(factor)
                options = options.copy(autoSize = TextAutoSize.StepBased(minFontSize = minFontSize, maxFontSize = currentFontSize))
            } else {
                // Use default 14sp as base if no fontSize specified (Compose default body text size)
                val defaultSize = 14.0.sp.sref()
                options = options.copy(autoSize = TextAutoSize.StepBased(minFontSize = (14.0 * factor).sp, maxFontSize = defaultSize))
            }
        }

        if (hasLineLimit) {
            options = options.copy(overflow = linvoke l@{ ->
                val singleLine = (maxLines == 1) || options.softWrap == false
                when (truncationMode) {
                    Text.TruncationMode.tail -> return@l TextOverflow.Ellipsis
                    Text.TruncationMode.head -> return@l if (singleLine) TextOverflow.StartEllipsis else TextOverflow.Ellipsis
                    Text.TruncationMode.middle -> return@l if (singleLine) TextOverflow.MiddleEllipsis else TextOverflow.Ellipsis
                    else -> return@l TextOverflow.Clip
                }
            })
        }

        EnvironmentValues.shared._material3Text?.let { updateOptions ->
            options = updateOptions(options)
        }
        val matchtarget_7 = options.annotatedText
        if (matchtarget_7 != null) {
            val annotatedText = matchtarget_7
            val matchtarget_8 = options.onTextLayout
            if (matchtarget_8 != null) {
                val onTextLayout = matchtarget_8
                androidx.compose.material3.Text(text = annotatedText, modifier = options.modifier, color = options.color, autoSize = options.autoSize, fontSize = options.fontSize, fontStyle = options.fontStyle, fontWeight = options.fontWeight, fontFamily = options.fontFamily, letterSpacing = options.letterSpacing, textDecoration = options.textDecoration, textAlign = options.textAlign, lineHeight = options.lineHeight, overflow = options.overflow, softWrap = options.softWrap, maxLines = options.maxLines, minLines = options.minLines, onTextLayout = onTextLayout, style = options.style)
            } else {
                androidx.compose.material3.Text(text = options.text ?: "", modifier = options.modifier, color = options.color, autoSize = options.autoSize, fontSize = options.fontSize, fontStyle = options.fontStyle, fontWeight = options.fontWeight, fontFamily = options.fontFamily, letterSpacing = options.letterSpacing, textDecoration = options.textDecoration, textAlign = options.textAlign, lineHeight = options.lineHeight, overflow = options.overflow, softWrap = options.softWrap, maxLines = options.maxLines, minLines = options.minLines, onTextLayout = options.onTextLayout, style = options.style)
            }
        } else {
            androidx.compose.material3.Text(text = options.text ?: "", modifier = options.modifier, color = options.color, autoSize = options.autoSize, fontSize = options.fontSize, fontStyle = options.fontStyle, fontWeight = options.fontWeight, fontFamily = options.fontFamily, letterSpacing = options.letterSpacing, textDecoration = options.textDecoration, textAlign = options.textAlign, lineHeight = options.lineHeight, overflow = options.overflow, softWrap = options.softWrap, maxLines = options.maxLines, minLines = options.minLines, onTextLayout = options.onTextLayout, style = options.style)
        }
    }

    private fun annotatedString(markdown: MarkdownNode, interpolations: kotlin.collections.List<AnyHashable>?, linkColor: androidx.compose.ui.graphics.Color, isUppercased: Boolean, isLowercased: Boolean, isRedacted: Boolean): AnnotatedString {
        return buildAnnotatedString { -> append(markdown = markdown, to = this, interpolations = interpolations, linkColor = linkColor, isUppercased = isUppercased, isLowercased = isLowercased, isRedacted = isRedacted) }
    }

    @OptIn(ExperimentalTextApi::class)
    private fun append(markdown: MarkdownNode, to: AnnotatedString.Builder, interpolations: kotlin.collections.List<AnyHashable>?, isFirstChild: Boolean = true, linkColor: androidx.compose.ui.graphics.Color, isUppercased: Boolean, isLowercased: Boolean, isRedacted: Boolean) {
        val builder = to
        fun appendChildren() {
            markdown.children?.forEachIndexed { it, it_1 -> append(markdown = it_1, to = builder, interpolations = interpolations, isFirstChild = it == 0, linkColor = linkColor, isUppercased = isUppercased, isLowercased = isLowercased, isRedacted = isRedacted) }
        }

        when (markdown.type) {
            MarkdownNode.NodeType.bold -> {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendChildren()
                builder.pop()
            }
            MarkdownNode.NodeType.code -> {
                markdown.formattedString(interpolations)?.let { text ->
                    builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                    if (isUppercased) {
                        builder.append(text.uppercased())
                    } else if (isLowercased) {
                        builder.append(text.lowercased())
                    } else {
                        builder.append(text)
                    }
                    builder.pop()
                }
            }
            MarkdownNode.NodeType.italic -> {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendChildren()
                builder.pop()
            }
            MarkdownNode.NodeType.link -> {
                if (isRedacted) {
                    builder.pushStyle(SpanStyle(background = linkColor))
                } else {
                    builder.pushStyle(SpanStyle(color = linkColor))
                }
                builder.pushUrlAnnotation(UrlAnnotation(markdown.formattedString(interpolations) ?: ""))
                appendChildren()
                builder.pop()
                builder.pop()
            }
            MarkdownNode.NodeType.paragraph -> {
                if (!isFirstChild) {
                    builder.append("\n\n")
                }
                appendChildren()
            }
            MarkdownNode.NodeType.root -> appendChildren()
            MarkdownNode.NodeType.strikethrough -> {
                builder.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                appendChildren()
                builder.pop()
            }
            MarkdownNode.NodeType.text -> {
                markdown.formattedString(interpolations)?.let { text ->
                    if (isUppercased) {
                        builder.append(text.uppercased())
                    } else if (isLowercased) {
                        builder.append(text.lowercased())
                    } else {
                        builder.append(text)
                    }
                }
            }
            MarkdownNode.NodeType.unknown -> appendChildren()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is _Text) return false
        return verbatim == other.verbatim && attributedString == other.attributedString && key == other.key && tableName == other.tableName && locale == other.locale && bundle == other.bundle
    }
}

@androidx.annotation.Keep
enum class TextAlignment(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): CaseIterable, RawRepresentable<Int> {
    leading(0), // For bridging
    center(1), // For bridging
    trailing(2); // For bridging

    /// Convert this enum to a Compose `TextAlign` value.
    fun asTextAlign(): TextAlign {
        return when (this) {
            TextAlignment.leading -> TextAlign.Start
            TextAlignment.center -> TextAlign.Center
            TextAlignment.trailing -> TextAlign.End
        }
    }

    @androidx.annotation.Keep
    companion object: CaseIterableCompanion<TextAlignment> {
        fun init(rawValue: Int): TextAlignment? {
            return when (rawValue) {
                0 -> TextAlignment.leading
                1 -> TextAlignment.center
                2 -> TextAlignment.trailing
                else -> null
            }
        }

        override val allCases: Array<TextAlignment>
            get() = arrayOf(leading, center, trailing)
    }
}

fun TextAlignment(rawValue: Int): TextAlignment? = TextAlignment.init(rawValue = rawValue)

internal class TextEnvironment: MutableStruct {
    internal var fontWeight: Font.Weight? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    internal var fontDesign: Font.Design? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    internal var isItalic: Boolean? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    internal var isUnderline: Boolean? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    internal var isStrikethrough: Boolean? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    internal var textCase: Text.Case? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    internal var tracking: Double? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    internal var lineSpacing: Double? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    internal var minimumScaleFactor: Double? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }

    internal val textDecoration: TextDecoration?
        get() {
            if ((isUnderline == true) && (isStrikethrough == true)) {
                return TextDecoration.Underline + TextDecoration.LineThrough
            } else if (isUnderline == true) {
                return TextDecoration.Underline
            } else if (isStrikethrough == true) {
                return TextDecoration.LineThrough
            } else {
                return null
            }
        }

    constructor(fontWeight: Font.Weight? = null, fontDesign: Font.Design? = null, isItalic: Boolean? = null, isUnderline: Boolean? = null, isStrikethrough: Boolean? = null, textCase: Text.Case? = null, tracking: Double? = null, lineSpacing: Double? = null, minimumScaleFactor: Double? = null) {
        this.fontWeight = fontWeight
        this.fontDesign = fontDesign
        this.isItalic = isItalic
        this.isUnderline = isUnderline
        this.isStrikethrough = isStrikethrough
        this.textCase = textCase
        this.tracking = tracking
        this.lineSpacing = lineSpacing
        this.minimumScaleFactor = minimumScaleFactor
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = TextEnvironment(fontWeight, fontDesign, isItalic, isUnderline, isStrikethrough, textCase, tracking, lineSpacing, minimumScaleFactor)

    override fun equals(other: Any?): Boolean {
        if (other !is TextEnvironment) return false
        return fontWeight == other.fontWeight && fontDesign == other.fontDesign && isItalic == other.isItalic && isUnderline == other.isUnderline && isStrikethrough == other.isStrikethrough && textCase == other.textCase && tracking == other.tracking && lineSpacing == other.lineSpacing && minimumScaleFactor == other.minimumScaleFactor
    }
}

internal fun textEnvironment(for_: View, update: (InOut<TextEnvironment>) -> Unit): View {
    val view = for_
    return ModifiedContent(content = view, modifier = EnvironmentModifier(affectsEvaluate = false) l@{ environment ->
        var textEnvironment = environment._textEnvironment.sref()
        update(InOut({ textEnvironment }, { textEnvironment = it }))
        environment.set_textEnvironment(textEnvironment)
        return@l ComposeResult.ok
    })
}

internal class TextStyleInfo {
    internal val style: TextStyle
    internal val color: androidx.compose.ui.graphics.Color?
    internal val isUppercased: Boolean
    internal val isLowercased: Boolean

    constructor(style: TextStyle, color: androidx.compose.ui.graphics.Color? = null, isUppercased: Boolean, isLowercased: Boolean) {
        this.style = style.sref()
        this.color = color
        this.isUppercased = isUppercased
        this.isLowercased = isLowercased
    }
}

@Suppress("MUST_BE_INITIALIZED")
class Material3TextOptions: MutableStruct {
    var text: String? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var annotatedText: AnnotatedString? = null
        get() = field.sref({ this.annotatedText = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var modifier: Modifier
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var color: androidx.compose.ui.graphics.Color
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var fontSize: TextUnit
        get() = field.sref({ this.fontSize = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var fontStyle: FontStyle? = null
        get() = field.sref({ this.fontStyle = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var fontWeight: FontWeight? = null
        get() = field.sref({ this.fontWeight = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var fontFamily: FontFamily? = null
        get() = field.sref({ this.fontFamily = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var letterSpacing: TextUnit
        get() = field.sref({ this.letterSpacing = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var textDecoration: TextDecoration? = null
        get() = field.sref({ this.textDecoration = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var textAlign: TextAlign? = null
        get() = field.sref({ this.textAlign = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var lineHeight: TextUnit
        get() = field.sref({ this.lineHeight = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var overflow: TextOverflow
        get() = field.sref({ this.overflow = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var softWrap: Boolean
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var maxLines: Int
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var minLines: Int
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var onTextLayout: ((TextLayoutResult) -> Unit)? = null
        set(newValue) {
            willmutate()
            field = newValue
            didmutate()
        }
    var style: TextStyle
        get() = field.sref({ this.style = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }
    var autoSize: TextAutoSize? = null
        get() = field.sref({ this.autoSize = it })
        set(newValue) {
            @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
            willmutate()
            field = newValue
            didmutate()
        }

    fun copy(text: String? = this.text, annotatedText: AnnotatedString? = this.annotatedText, modifier: Modifier = this.modifier, color: androidx.compose.ui.graphics.Color = this.color, fontSize: TextUnit = this.fontSize, fontStyle: FontStyle? = this.fontStyle, fontWeight: FontWeight? = this.fontWeight, fontFamily: FontFamily? = this.fontFamily, letterSpacing: TextUnit = this.letterSpacing, textDecoration: TextDecoration? = this.textDecoration, textAlign: TextAlign? = this.textAlign, lineHeight: TextUnit = this.lineHeight, overflow: TextOverflow = this.overflow, softWrap: Boolean = this.softWrap, maxLines: Int = this.maxLines, minLines: Int = this.minLines, onTextLayout: ((TextLayoutResult) -> Unit)? = this.onTextLayout, style: TextStyle = this.style, autoSize: TextAutoSize? = this.autoSize): Material3TextOptions = Material3TextOptions(text = text, annotatedText = annotatedText, modifier = modifier, color = color, fontSize = fontSize, fontStyle = fontStyle, fontWeight = fontWeight, fontFamily = fontFamily, letterSpacing = letterSpacing, textDecoration = textDecoration, textAlign = textAlign, lineHeight = lineHeight, overflow = overflow, softWrap = softWrap, maxLines = maxLines, minLines = minLines, onTextLayout = onTextLayout, style = style, autoSize = autoSize)

    constructor(text: String? = null, annotatedText: AnnotatedString? = null, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified, fontSize: TextUnit = TextUnit.Unspecified.sref(), fontStyle: FontStyle? = null, fontWeight: FontWeight? = null, fontFamily: FontFamily? = null, letterSpacing: TextUnit = TextUnit.Unspecified.sref(), textDecoration: TextDecoration? = null, textAlign: TextAlign? = null, lineHeight: TextUnit = TextUnit.Unspecified.sref(), overflow: TextOverflow = TextOverflow.Clip.sref(), softWrap: Boolean = true, maxLines: Int = Int.max, minLines: Int = 1, onTextLayout: ((TextLayoutResult) -> Unit)? = null, style: TextStyle, autoSize: TextAutoSize? = null) {
        this.text = text
        this.annotatedText = annotatedText
        this.modifier = modifier
        this.color = color
        this.fontSize = fontSize
        this.fontStyle = fontStyle
        this.fontWeight = fontWeight
        this.fontFamily = fontFamily
        this.letterSpacing = letterSpacing
        this.textDecoration = textDecoration
        this.textAlign = textAlign
        this.lineHeight = lineHeight
        this.overflow = overflow
        this.softWrap = softWrap
        this.maxLines = maxLines
        this.minLines = minLines
        this.onTextLayout = onTextLayout
        this.style = style
        this.autoSize = autoSize
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = Material3TextOptions(text, annotatedText, modifier, color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines, minLines, onTextLayout, style, autoSize)

    @androidx.annotation.Keep
    companion object {
    }
}

class RedactionReasons: OptionSet<RedactionReasons, Int>, MutableStruct {
    override var rawValue: Int

    constructor(rawValue: Int) {
        this.rawValue = rawValue
    }

    override val rawvaluelong: ULong
        get() = ULong(rawValue)
    override fun makeoptionset(rawvaluelong: ULong): RedactionReasons = RedactionReasons(rawValue = Int(rawvaluelong))
    override fun assignoptionset(target: RedactionReasons) {
        willmutate()
        try {
            assignfrom(target)
        } finally {
            didmutate()
        }
    }

    private constructor(copy: MutableStruct) {
        @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as RedactionReasons
        this.rawValue = copy.rawValue
    }

    override var supdate: ((Any) -> Unit)? = null
    override var smutatingcount = 0
    override fun scopy(): MutableStruct = RedactionReasons(this as MutableStruct)

    private fun assignfrom(target: RedactionReasons) {
        this.rawValue = target.rawValue
    }

    @androidx.annotation.Keep
    companion object {

        val placeholder = RedactionReasons(rawValue = 1 shl 0)

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val privacy = RedactionReasons(rawValue = 1 shl 1)

        @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
        val invalidated = RedactionReasons(rawValue = 1 shl 2)

        fun of(vararg options: RedactionReasons): RedactionReasons {
            val value = options.fold(Int(0)) { result, option -> result or option.rawValue }
            return RedactionReasons(rawValue = value)
        }
    }
}

/*
import struct Foundation.AttributedString
import struct Foundation.Date
import struct Foundation.DateInterval
import struct Foundation.Locale
import struct Foundation.LocalizedStringResource

import protocol Foundation.AttributeScope
import struct Foundation.AttributeScopeCodableConfiguration
import enum Foundation.AttributeScopes
import enum Foundation.AttributeDynamicLookup
import protocol Foundation.AttributedStringKey

import class Foundation.Bundle
import class Foundation.NSObject
import class Foundation.Formatter

import protocol Foundation.ParseableFormatStyle
import protocol Foundation.FormatStyle
import protocol Foundation.ReferenceConvertible

extension Text {

/// Creates an instance that wraps an `Image`, suitable for concatenating
/// with other `Text`
@available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
public init(_ image: Image) { fatalError() }
}

extension Text {

/// Specifies the language for typesetting.
///
/// In some cases `Text` may contain text of a particular language which
/// doesn't match the device UI language. In that case it's useful to
/// specify a language so line height, line breaking and spacing will
/// respect the script used for that language. For example:
///
///     Text(verbatim: "แอปเปิล")
///         .typesettingLanguage(.init(languageCode: .thai))
///
/// Note: this language does not affect text localization.
///
/// - Parameters:
///   - language: The explicit language to use for typesetting.
///   - isEnabled: A Boolean value that indicates whether text langauge is
///     added
/// - Returns: Text with the typesetting language set to the value you
///   supply.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
public func typesettingLanguage(_ language: Locale.Language, isEnabled: Bool = true) -> Text { fatalError() }

/// Specifies the language for typesetting.
///
/// In some cases `Text` may contain text of a particular language which
/// doesn't match the device UI language. In that case it's useful to
/// specify a language so line height, line breaking and spacing will
/// respect the script used for that language. For example:
///
///     Text(verbatim: "แอปเปิล").typesettingLanguage(
///         .explicit(.init(languageCode: .thai)))
///
/// Note: this language does not affect text localized localization.
///
/// - Parameters:
///   - language: The language to use for typesetting.
///   - isEnabled: A Boolean value that indicates whether text language is
///     added
/// - Returns: Text with the typesetting language set to the value you
///   supply.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
public func typesettingLanguage(_ language: TypesettingLanguage, isEnabled: Bool = true) -> Text { fatalError() }
}

@available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
extension Text {

/// Creates a text view that displays the formatted representation
/// of a reference-convertible value.
///
/// Use this initializer to create a text view that formats `subject`
/// using `formatter`.
/// - Parameters:
///   - subject: A
///
///   instance compatible with `formatter`.
///   - formatter: A
///
///   capable of converting `subject` into a string representation.
public init<Subject>(_ subject: Subject, formatter: Formatter) where Subject : ReferenceConvertible { fatalError() }

/// Creates a text view that displays the formatted representation
/// of a Foundation object.
///
/// Use this initializer to create a text view that formats `subject`
/// using `formatter`.
/// - Parameters:
///   - subject: An
///
///   instance compatible with `formatter`.
///   - formatter: A
///
///   capable of converting `subject` into a string representation.
public init<Subject>(_ subject: Subject, formatter: Formatter) where Subject : NSObject { fatalError() }
}

@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
extension Text {

/// Creates a text view that displays the formatted representation
/// of a nonstring type supported by a corresponding format style.
///
/// Use this initializer to create a text view backed by a nonstring
/// value, using a
/// to convert the type to a string representation. Any changes to the value
/// update the string displayed by the text view.
///
/// In the following example, three ``Text`` views present a date with
/// different combinations of date and time fields, by using different
/// options.
///
///     @State private var myDate = Date()
///     var body: some View {
///         VStack {
///             Text(myDate, format: Date.FormatStyle(date: .numeric, time: .omitted))
///             Text(myDate, format: Date.FormatStyle(date: .complete, time: .complete))
///             Text(myDate, format: Date.FormatStyle().hour(.defaultDigitsNoAMPM).minute())
///         }
///     }
///
/// ![Three vertically stacked text views showing the date with different
/// levels of detail: 4/1/1976; April 1, 1976; Thursday, April 1,
/// 1976.](Text-init-format-1)
///
/// - Parameters:
///   - input: The underlying value to display.
///   - format: A format style of type `F` to convert the underlying value
///     of type `F.FormatInput` to a string representation.
public init<F>(_ input: F.FormatInput, format: F) where F : FormatStyle, F.FormatInput : Equatable, F.FormatOutput == String { fatalError() }
}

@available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
extension Text {

/// A predefined style used to display a `Date`.
public struct DateStyle : Sendable {

/// A style displaying only the time component for a date.
///
///     Text(event.startDate, style: .time)
///
/// Example output:
///     11:23PM
public static let time: Text.DateStyle = { fatalError() }()

/// A style displaying a date.
///
///     Text(event.startDate, style: .date)
///
/// Example output:
///     June 3, 2019
public static let date: Text.DateStyle = { fatalError() }()

/// A style displaying a date as relative to now.
///
///     Text(event.startDate, style: .relative)
///
/// Example output:
///     2 hours, 23 minutes
///     1 year, 1 month
public static let relative: Text.DateStyle = { fatalError() }()

/// A style displaying a date as offset from now.
///
///     Text(event.startDate, style: .offset)
///
/// Example output:
///     +2 hours
///     -3 months
public static let offset: Text.DateStyle = { fatalError() }()

/// A style displaying a date as timer counting from now.
///
///     Text(event.startDate, style: .timer)
///
/// Example output:
///    2:32
///    36:59:01
public static let timer: Text.DateStyle = { fatalError() }()
}

/// Creates an instance that displays localized dates and times using a specific style.
///
/// - Parameters:
///     - date: The target date to display.
///     - style: The style used when displaying a date.
public init(_ date: Date, style: Text.DateStyle) { fatalError() }

/// Creates an instance that displays a localized range between two dates.
///
/// - Parameters:
///     - dates: The range of dates to display
public init(_ dates: ClosedRange<Date>) { fatalError() }

/// Creates an instance that displays a localized time interval.
///
///     Text(DateInterval(start: event.startDate, duration: event.duration))
///
/// Example output:
///     9:30AM - 3:30PM
///
/// - Parameters:
///     - interval: The date interval to display
public init(_ interval: DateInterval) { fatalError() }
}

@available(iOS 16.0, macOS 13.0, tvOS 16.0, watchOS 9.0, *)
extension Text {

/// Creates an instance that displays a timer counting within the provided
/// interval.
///
///     Text(
///         timerInterval: Date.now...Date(timeInterval: 12 * 60, since: .now))
///         pauseTime: Date.now + (10 * 60))
///
/// The example above shows a text that displays a timer counting down
/// from "12:00" and will pause when reaching "10:00".
///
/// - Parameters:
///     - timerInterval: The interval between where to run the timer.
///     - pauseTime: If present, the date at which to pause the timer.
///         The default is `nil` which indicates to never pause.
///     - countsDown: Whether to count up or down. The default is `true`.
///     - showsHours: Whether to include an hours component if there are
///         more than 60 minutes left on the timer. The default is `true`.
public init(timerInterval: ClosedRange<Date>, pauseTime: Date? = nil, countsDown: Bool = true, showsHours: Bool = true) { fatalError() }
}

extension Text {

/// Applies a text scale to the text.
///
/// - Parameters:
///   - scale: The text scale to apply.
///   - isEnabled: If true the text scale is applied; otherwise text scale
///     is unchanged.
/// - Returns: Text with the specified scale applied.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
public func textScale(_ scale: Text.Scale, isEnabled: Bool = true) -> Text { fatalError() }
}

@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
extension Text {

/// Sets whether VoiceOver should always speak all punctuation in the text view.
///
/// Use this modifier to control whether the system speaks punctuation characters
/// in the text. You might use this for code or other text where the punctuation is relevant, or where
/// you want VoiceOver to speak a verbatim transcription of the text you provide. For example,
/// given the text:
///
///     Text("All the world's a stage, " +
///          "And all the men and women merely players;")
///          .speechAlwaysIncludesPunctuation()
///
/// VoiceOver would speak "All the world apostrophe s a stage comma and all the men
/// and women merely players semicolon".
///
/// By default, VoiceOver voices punctuation based on surrounding context.
///
/// - Parameter value: A Boolean value that you set to `true` if
///   VoiceOver should speak all punctuation in the text. Defaults to `true`.
public func speechAlwaysIncludesPunctuation(_ value: Bool = true) -> Text { fatalError() }

/// Sets whether VoiceOver should speak the contents of the text view character by character.
///
/// Use this modifier when you want VoiceOver to speak text as individual letters,
/// character by character. This is important for text that is not meant to be spoken together, like:
/// - An acronym that isn't a word, like APPL, spoken as "A-P-P-L".
/// - A number representing a series of digits, like 25, spoken as "two-five" rather than "twenty-five".
///
/// - Parameter value: A Boolean value that when `true` indicates
///    VoiceOver should speak text as individual characters. Defaults
///    to `true`.
public func speechSpellsOutCharacters(_ value: Bool = true) -> Text { fatalError() }

/// Raises or lowers the pitch of spoken text.
///
/// Use this modifier when you want to change the pitch of spoken text.
/// The value indicates how much higher or lower to change the pitch.
///
/// - Parameter value: The amount to raise or lower the pitch.
///   Values between `-1` and `0` result in a lower pitch while
///   values between `0` and `1` result in a higher pitch.
///   The method clamps values to the range `-1` to `1`.
public func speechAdjustedPitch(_ value: Double) -> Text { fatalError() }

/// Controls whether to queue pending announcements behind existing speech rather than
/// interrupting speech in progress.
///
/// Use this modifier when you want affect the order in which the
/// accessibility system delivers spoken text. Announcements can
/// occur automatically when the label or value of an accessibility
/// element changes.
///
/// - Parameter value: A Boolean value that determines if VoiceOver speaks
///   changes to text immediately or enqueues them behind existing speech.
///   Defaults to `true`.
public func speechAnnouncementsQueued(_ value: Bool = true) -> Text { fatalError() }
}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension Text {

/// Concatenates the text in two text views in a new text view.
///
/// - Parameters:
///   - lhs: The first text view with text to combine.
///   - rhs: The second text view with text to combine.
///
/// - Returns: A new text view containing the combined contents of the two
///   input text views.
public static func + (lhs: Text, rhs: Text) -> Text { fatalError() }
}

extension Text {

/// Creates a text view that displays a localized string resource.
///
/// Use this initializer to display a localized string that is
/// represented by a
///
///     var object = LocalizedStringResource("pencil")
///     Text(object) // Localizes the resource if possible, or displays "pencil" if not.
///
//@available(iOS 16.0, macOS 13, tvOS 16.0, watchOS 9.0, *)
//public init(_ resource: LocalizedStringResource) { fatalError() }
}

@available(iOS 15.0, macOS 12.0, tvOS 15.0, watchOS 8.0, *)
extension Text {

/// Sets an accessibility text content type.
///
/// Use this modifier to set the content type of this accessibility
/// element. Assistive technologies can use this property to choose
/// an appropriate way to output the text. For example, when
/// encountering a source coding context, VoiceOver could
/// choose to speak all punctuation.
///
/// If you don't set a value with this method, the default content type
/// is ``AccessibilityTextContentType/plain``.
///
/// - Parameter value: The accessibility content type from the available
/// ``AccessibilityTextContentType`` options.
public func accessibilityTextContentType(_ value: AccessibilityTextContentType) -> Text { fatalError() }

/// Sets the accessibility level of this heading.
///
/// Use this modifier to set the level of this heading in relation to other headings. The system speaks
/// the level number of levels ``AccessibilityHeadingLevel/h1`` through
/// ``AccessibilityHeadingLevel/h6`` alongside the text.
///
/// The default heading level if you don't use this modifier
/// is ``AccessibilityHeadingLevel/unspecified``.
///
/// - Parameter level: The heading level to associate with this element
///   from the available ``AccessibilityHeadingLevel`` levels.
public func accessibilityHeading(_ level: AccessibilityHeadingLevel) -> Text { fatalError() }

/// Use this method to provide an alternative accessibility label to the text that is displayed.
/// For example, you can give an alternate label to a navigation title:
///
///     var body: some View {
///         NavigationView {
///             ContentView()
///                 .navigationTitle(Text("􀈤").accessibilityLabel("Inbox"))
///         }
///     }
///
/// - Parameter labelKey: The string key for the alternative
///   accessibility label.
public func accessibilityLabel(_ labelKey: LocalizedStringKey) -> Text { fatalError() }
}

//@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
//extension Text.Storage : @unchecked Sendable {
//}
//
//@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
//extension Text.Modifier : @unchecked Sendable {
//}

@available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
extension Text.DateStyle : Equatable {


}

@available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
extension Text.DateStyle : Codable {

/// Encodes this value into the given encoder.
///
/// If the value fails to encode anything, `encoder` will encode an empty
/// keyed container in its place.
///
/// This function throws an error if any values are invalid for the given
/// encoder's format.
///
/// - Parameter encoder: The encoder to write data to.
public func encode(to encoder: Encoder) throws { fatalError() }

/// Creates a new instance by decoding from the given decoder.
///
/// This initializer throws an error if reading from the decoder fails, or
/// if the data read is corrupted or otherwise invalid.
///
/// - Parameter decoder: The decoder to read data from.
public init(from decoder: Decoder) throws { fatalError() }
}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension Text.TruncationMode : Equatable {
}

@available(iOS 13.0, macOS 10.15, tvOS 13.0, watchOS 6.0, *)
extension Text.TruncationMode : Hashable {
}

@available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
extension Text.Case : Equatable {
}

@available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 7.0, *)
extension Text.Case : Hashable {
}

/// A built-in group of commands for searching, editing, and transforming
/// selections of text.
///
/// These commands are optional and can be explicitly requested by passing a
/// value of this type to the `Scene.commands(_:)` modifier.
@available(iOS 14.0, macOS 11.0, *)
@available(tvOS, unavailable)
@available(watchOS, unavailable)
public struct TextEditingCommands : Commands {

/// A new value describing the built-in text-editing commands.
public init() { fatalError() }

/// The contents of the command hierarchy.
///
/// For any commands that you create, provide a computed `body` property
/// that defines the scene as a composition of other scenes. You can
/// assemble a command hierarchy from built-in commands that SkipUI
/// provides, as well as other commands that you've defined.
public var body: Body { fatalError() }

/// The type of commands that represents the body of this command hierarchy.
///
/// When you create custom commands, Swift infers this type from your
/// implementation of the required ``SkipUI/Commands/body-swift.property``
/// property.
public typealias Body = NeverView
}

extension AttributeScopes {

/// A property for accessing the attribute scopes defined by SkipUI.
@available(macOS 12.0, iOS 15.0, tvOS 15.0, watchOS 8.0, *)
public var skipUI: AttributeScopes.SkipUIAttributes.Type { get { fatalError() } }

/// Attribute scopes defined by SkipUI.
@available(macOS 12.0, iOS 15.0, tvOS 15.0, watchOS 8.0, *)
public struct SkipUIAttributes : AttributeScope {

//        /// A property for accessing a font attribute.
//        public let font: AttributeScopes.SwiftUI.FontAttribute = { fatalError() }()
//
//        /// A property for accessing a foreground color attribute.
//        public let foregroundColor: AttributeScopes.SkipUIAttributes.ForegroundColorAttribute = { fatalError() }()
//
//        /// A property for accessing a background color attribute.
//        public let backgroundColor: AttributeScopes.SkipUIAttributes.BackgroundColorAttribute = { fatalError() }()
//
//        /// A property for accessing a strikethrough style attribute.
//        public let strikethroughStyle: AttributeScopes.SkipUIAttributes.StrikethroughStyleAttribute = { fatalError() }()
//
//        /// A property for accessing an underline style attribute.
//        public let underlineStyle: AttributeScopes.SkipUIAttributes.UnderlineStyleAttribute = { fatalError() }()
//
//        /// A property for accessing a kerning attribute.
//        public let kern: AttributeScopes.SkipUIAttributes.KerningAttribute = { fatalError() }()
//
//        /// A property for accessing a tracking attribute.
//        public let tracking: AttributeScopes.SkipUIAttributes.TrackingAttribute = { fatalError() }()
//
//        /// A property for accessing a baseline offset attribute.
//        public let baselineOffset: AttributeScopes.SkipUIAttributes.BaselineOffsetAttribute = { fatalError() }()
//
//        /// A property for accessing attributes defined by the Accessibility framework.
//        public let accessibility: AttributeScopes.AccessibilityAttributes = { fatalError() }()

/// A property for accessing attributes defined by the Foundation framework.
public let foundation: AttributeScopes.FoundationAttributes = { fatalError() }()

public typealias DecodingConfiguration = AttributeScopeCodableConfiguration

public typealias EncodingConfiguration = AttributeScopeCodableConfiguration
}
}

@available(macOS 12.0, iOS 15.0, tvOS 15.0, watchOS 8.0, *)
extension AttributeDynamicLookup {

public subscript<T>(dynamicMember keyPath: KeyPath<AttributeScopes.SkipUIAttributes, T>) -> T where T : AttributedStringKey { get { fatalError() } }
}
*/
