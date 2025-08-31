package me.ash.reader.ui.component.reader

import android.content.Context
import android.util.Log
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

/**
 * Utilities for rendering markdown in different formats.
 */
object MarkdownUtils {

    /**
     * Convert simple markdown to an AnnotatedString with basic formatting.
     * This is a simplified renderer for summaries.
     */
    fun markdownToAnnotatedString(markdown: String): AnnotatedString {
        return buildAnnotatedString {
            // Split by double newlines to preserve paragraph breaks
            val paragraphs = markdown.split("\n\n")

            val unorderedListRegex = Regex("^([*\\-])\\s+(.*)")
            val orderedListRegex = Regex("^(\\d+\\.)\\s+(.*)")

            paragraphs.forEachIndexed { index, paragraph ->
                if (index > 0) {
                    append("\n\n") // Add paragraph breaks
                }

                val lines = paragraph.lines()
                lines.forEachIndexed { lineIndex, line ->
                    if (lineIndex > 0) {
                        append("\n")
                    }

                    // Handle lists and then apply inline formatting to the content
                    val trimmedLine = line.trimStart()
                    val indentation = " ".repeat(line.length - trimmedLine.length)

                    val unorderedListMatch = unorderedListRegex.find(trimmedLine)
                    val orderedListMatch = orderedListRegex.find(trimmedLine)

                    val textToParse: String
                    if (unorderedListMatch != null) {
                        val (_, content) = unorderedListMatch.destructured
                        append(indentation)
                        append("• ")
                        textToParse = content
                    } else if (orderedListMatch != null) {
                        val (prefix, content) = orderedListMatch.destructured
                        append(indentation)
                        append(prefix)
                        append(" ")
                        textToParse = content
                    } else {
                        textToParse = line
                    }

                    // Process each line with basic formatting
                    var text = textToParse
                    var pos = 0
                    while (pos < text.length) {
                        when {
                            text.startsWith("**", pos) -> {
                                val end = text.indexOf("**", pos + 2)
                                if (end != -1) {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(text.substring(pos + 2, end))
                                    }
                                    pos = end + 2
                                } else {
                                    append(text.substring(pos))
                                    pos = text.length
                                }
                            }
                            text.startsWith("*", pos) -> {
                                val end = text.indexOf("*", pos + 1)
                                if (end != -1) {
                                    append(text.substring(pos + 1, end))
                                    pos = end + 1
                                } else {
                                    append(text.substring(pos))
                                    pos = text.length
                                }
                            }
                            else -> {
                                append(text[pos])
                                pos++
                            }
                        }
                    }
                }
            }
        }
    }



    /**
     * Convert markdown into an HTML string for WebView rendering.
     */
    fun markdownToHtml(context: Context, markdown: String): String {
        Log.d("MarkdownUtils", "Input markdown: $markdown")

        // Write input markdown to file
        try {
            val logFile = File(context.filesDir, "markdown_log.txt")
            val fos = FileOutputStream(logFile, true)
            val writer = OutputStreamWriter(fos)
            writer.append("\n\n=== Input Markdown ===\n")
            writer.append(markdown)
            writer.append("\n=== End Input ===\n")
            writer.close()
        } catch (e: Exception) {
            Log.e("MarkdownUtils", "Failed to write markdown log", e)
        }

        // Normalize line endings and ensure paragraph breaks
        // Normalize line endings and ensure proper paragraph breaks
        // The previous logic of replacing single newlines with spaces breaks list formatting.
        // Flexmark can handle newlines correctly, so we just normalize them.
        val formattedMarkdown = markdown.replace("\r\n", "\n").trim()

        // Configure Flexmark for better Markdown processing
        val options = MutableDataSet()
        options.set(Parser.EXTENSIONS, listOf(
            com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension.create(),
            com.vladsch.flexmark.ext.tables.TablesExtension.create(),
            com.vladsch.flexmark.ext.autolink.AutolinkExtension.create()
        ))
        options.set(HtmlRenderer.SOFT_BREAK, "<br />\n")
        options.set(HtmlRenderer.HARD_BREAK, "<br />\n")

        val parser = Parser.builder(options).build()
        val document = parser.parse(formattedMarkdown)
        val renderer = HtmlRenderer.builder(options).build()
        var htmlBody = renderer.render(document)

        // Add explicit line breaks in HTML and improve spacing
        htmlBody = htmlBody
            .replace("</p><p>", "</p>\n\n<p>") // Add line breaks between paragraphs
            .replace("</li><li>", "</li>\n<li>") // Add line breaks between list items
            .replace("</ul><p>", "</ul>\n\n<p>") // Add line breaks between lists and paragraphs
            .replace("</ol><p>", "</ol>\n\n<p>") // Add line breaks between lists and paragraphs
            .replace("</blockquote><p>", "</blockquote>\n\n<p>") // Add line breaks between blockquotes and paragraphs
            .replace("<br>", "<br>\n") // Add line breaks after BR tags
            .replace("<br/>", "<br/>\n") // Add line breaks after BR tags
            .replace("<br />", "<br />\n") // Add line breaks after BR tags
            .replace("</h1><p>", "</h1>\n\n<p>") // Add line breaks after headings
            .replace("</h2><p>", "</h2>\n\n<p>")
            .replace("</h3><p>", "</h3>\n\n<p>")
            .replace("</h4><p>", "</h4>\n\n<p>")
            .replace("</h5><p>", "</h5>\n\n<p>")
            .replace("</h6><p>", "</h6>\n\n<p>")

        // Write generated HTML to file
        try {
            val logFile = File(context.filesDir, "markdown_log.txt")
            val fos = FileOutputStream(logFile, true)
            val writer = OutputStreamWriter(fos)
            writer.append("\n\n=== Generated HTML ===\n")
            writer.append(htmlBody)
            writer.append("\n=== End HTML ===\n")
            writer.close()
        } catch (e: Exception) {
            Log.e("MarkdownUtils", "Failed to write HTML log", e)
        }

        // Add explicit line breaks between block elements
        htmlBody = htmlBody
            .replace("</p><p>", "</p>\n\n<p>")
            .replace("</li><li>", "</li>\n<li>")
            .replace("</ul><p>", "</ul>\n\n<p>")
            .replace("</ol><p>", "</ol>\n\n<p>")
            .replace("</blockquote><p>", "</blockquote>\n\n<p>")

        Log.d("MarkdownUtils", "Generated HTML: $htmlBody")

        // Wrap inside minimal HTML/CSS for readability in RYWebView
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8"/>
                <style>
                    body {
                        font-family: sans-serif;
                        padding: 12px;
                        line-height: 1.6em;
                        color: #222;
                        background-color: transparent;
                        white-space: normal;
                        word-wrap: break-word;
                        overflow-wrap: break-word;
                    }
                    h1, h2, h3, h4, h5, h6 {
                        margin-top: 1em;
                        margin-bottom: 0.5em;
                        font-weight: bold;
                    }
                    ul, ol {
                        margin-left: 1.2em;
                        padding-left: 1em;
                        margin-bottom: 1em;
                        white-space: normal;
                    }
                    li {
                        margin-bottom: 0.5em;
                        white-space: pre-wrap;
                    }
                    blockquote {
                        border-left: 3px solid #888;
                        margin: 0.8em 0;
                        padding-left: 0.8em;
                        color: #555;
                    }
                    code {
                        background-color: #f5f5f5;
                        padding: 2px 4px;
                        border-radius: 4px;
                        font-family: monospace;
                    }
                    pre {
                        background-color: #f5f5f5;
                        padding: 8px;
                        border-radius: 6px;
                        overflow-x: auto;
                        margin-bottom: 1em;
                        white-space: pre-wrap;
                    }
                    p {
                        margin-top: 0;
                        margin-bottom: 1.2em;
                        text-align: left;
                        white-space: normal;
                    }
                    /* Ensure proper spacing between different block elements */
                    p + p, p + ul, p + ol, p + blockquote,
                    ul + p, ol + p, blockquote + p {
                        margin-top: 1.2em;
                    }
                    /* Prevent text from being too close to edges */
                    body > p:first-child, body > ul:first-child, body > ol:first-child {
                        margin-top: 0.5em;
                    }
                </style>
            </head>
            <body>
                $htmlBody
            </body>
            </html>
        """.trimIndent()
    }
}
