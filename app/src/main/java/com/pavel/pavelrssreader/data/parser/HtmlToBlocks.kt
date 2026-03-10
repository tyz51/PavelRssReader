package com.pavel.pavelrssreader.data.parser

import com.pavel.pavelrssreader.domain.model.ContentBlock
import com.pavel.pavelrssreader.domain.model.TextSpan
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

object HtmlToBlocks {

    fun parse(html: String): List<ContentBlock> =
        Jsoup.parseBodyFragment(html).body().children()
            .flatMap { parseElement(it) }

    private fun parseElement(element: Element): List<ContentBlock> =
        when (element.tagName().lowercase()) {
            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                val text = element.text()
                if (text.isBlank()) emptyList() else listOf(ContentBlock.Heading(element.tagName()[1].digitToInt(), text))
            }
            "p" -> {
                val spans = parseSpans(element)
                if (spans.isEmpty()) emptyList() else listOf(ContentBlock.Paragraph(spans))
            }
            "blockquote" -> {
                val text = element.text()
                if (text.isBlank()) emptyList() else listOf(ContentBlock.Quote(text))
            }
            "figure" -> {
                val img = element.selectFirst("img") ?: return emptyList()
                val src = img.attr("src").takeIf { it.isNotBlank() } ?: return emptyList()
                val caption = element.selectFirst("figcaption")?.text()?.takeIf { it.isNotBlank() }
                listOf(ContentBlock.Image(src, caption))
            }
            "img" -> {
                val src = element.attr("src").takeIf { it.isNotBlank() } ?: return emptyList()
                listOf(ContentBlock.Image(src, null))
            }
            "ul" -> element.select("> li").map { li ->
                ContentBlock.Paragraph(listOf(TextSpan.Plain("• ${li.text()}")))
            }
            "ol" -> element.select("> li").mapIndexed { i, li ->
                ContentBlock.Paragraph(listOf(TextSpan.Plain("${i + 1}. ${li.text()}")))
            }
            "div", "section", "article" -> element.children().flatMap { parseElement(it) }
            else -> emptyList()
        }

    private fun parseSpans(element: Element): List<TextSpan> =
        element.childNodes().flatMap { parseNode(it) }

    private fun parseNode(node: Node): List<TextSpan> = when {
        node is TextNode -> {
            val text = node.text()
            if (text.isBlank()) emptyList() else listOf(TextSpan.Plain(text))
        }
        node is Element -> when (node.tagName().lowercase()) {
            "strong", "b" -> {
                val text = node.text()
                if (text.isBlank()) emptyList() else listOf(TextSpan.Bold(text))
            }
            "em", "i" -> {
                val text = node.text()
                if (text.isBlank()) emptyList() else listOf(TextSpan.Italic(text))
            }
            "a" -> {
                val href = node.attr("href").takeIf { it.isNotBlank() }
                val text = node.text()
                when {
                    href != null && text.isNotBlank() -> listOf(TextSpan.Link(text, href))
                    text.isNotBlank() -> listOf(TextSpan.Plain(text))
                    else -> emptyList()
                }
            }
            "br" -> listOf(TextSpan.Plain("\n"))
            else -> node.childNodes().flatMap { parseNode(it) }
        }
        else -> emptyList()
    }
}
