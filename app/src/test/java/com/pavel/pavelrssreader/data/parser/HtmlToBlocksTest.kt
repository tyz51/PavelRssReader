package com.pavel.pavelrssreader.data.parser

import com.pavel.pavelrssreader.domain.model.ContentBlock
import com.pavel.pavelrssreader.domain.model.TextSpan
import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlToBlocksTest {

    @Test
    fun `heading h1 parsed`() {
        assertEquals(listOf(ContentBlock.Heading(1, "Title")), HtmlToBlocks.parse("<h1>Title</h1>"))
    }

    @Test
    fun `heading h2 parsed`() {
        assertEquals(listOf(ContentBlock.Heading(2, "Hello")), HtmlToBlocks.parse("<h2>Hello</h2>"))
    }

    @Test
    fun `heading level from tag`() {
        assertEquals(listOf(ContentBlock.Heading(3, "Sub")), HtmlToBlocks.parse("<h3>Sub</h3>"))
        assertEquals(listOf(ContentBlock.Heading(5, "Small")), HtmlToBlocks.parse("<h5>Small</h5>"))
    }

    @Test
    fun `plain paragraph`() {
        assertEquals(
            listOf(ContentBlock.Paragraph(listOf(TextSpan.Plain("Hello world")))),
            HtmlToBlocks.parse("<p>Hello world</p>")
        )
    }

    @Test
    fun `paragraph with bold`() {
        assertEquals(
            listOf(ContentBlock.Paragraph(listOf(TextSpan.Plain("Hello "), TextSpan.Bold("world")))),
            HtmlToBlocks.parse("<p>Hello <strong>world</strong></p>")
        )
    }

    @Test
    fun `paragraph with italic`() {
        assertEquals(
            listOf(ContentBlock.Paragraph(listOf(TextSpan.Plain("Hello "), TextSpan.Italic("world")))),
            HtmlToBlocks.parse("<p>Hello <em>world</em></p>")
        )
    }

    @Test
    fun `paragraph with link`() {
        assertEquals(
            listOf(ContentBlock.Paragraph(listOf(TextSpan.Link("click", "https://example.com")))),
            HtmlToBlocks.parse("""<p><a href="https://example.com">click</a></p>""")
        )
    }

    @Test
    fun `img parsed`() {
        assertEquals(
            listOf(ContentBlock.Image("https://example.com/img.jpg", null)),
            HtmlToBlocks.parse("""<img src="https://example.com/img.jpg">""")
        )
    }

    @Test
    fun `figure with caption`() {
        assertEquals(
            listOf(ContentBlock.Image("https://x.com/a.jpg", "Cap")),
            HtmlToBlocks.parse("""<figure><img src="https://x.com/a.jpg"><figcaption>Cap</figcaption></figure>""")
        )
    }

    @Test
    fun `blockquote parsed`() {
        assertEquals(
            listOf(ContentBlock.Quote("A quote")),
            HtmlToBlocks.parse("<blockquote>A quote</blockquote>")
        )
    }

    @Test
    fun `ul list items become paragraphs with bullet`() {
        assertEquals(
            listOf(
                ContentBlock.Paragraph(listOf(TextSpan.Plain("• Item 1"))),
                ContentBlock.Paragraph(listOf(TextSpan.Plain("• Item 2")))
            ),
            HtmlToBlocks.parse("<ul><li>Item 1</li><li>Item 2</li></ul>")
        )
    }

    @Test
    fun `ol list items become paragraphs with numbers`() {
        assertEquals(
            listOf(
                ContentBlock.Paragraph(listOf(TextSpan.Plain("1. First"))),
                ContentBlock.Paragraph(listOf(TextSpan.Plain("2. Second")))
            ),
            HtmlToBlocks.parse("<ol><li>First</li><li>Second</li></ol>")
        )
    }

    @Test
    fun `iframe is dropped`() {
        assertEquals(
            emptyList<ContentBlock>(),
            HtmlToBlocks.parse("""<iframe src="https://youtube.com/embed/abc"></iframe>""")
        )
    }

    @Test
    fun `form is dropped`() {
        assertEquals(
            emptyList<ContentBlock>(),
            HtmlToBlocks.parse("""<form><input type="email"><button>Subscribe</button></form>""")
        )
    }

    @Test
    fun `blank paragraph filtered out`() {
        assertEquals(emptyList<ContentBlock>(), HtmlToBlocks.parse("<p>   </p>"))
    }

    @Test
    fun `img without src filtered out`() {
        assertEquals(emptyList<ContentBlock>(), HtmlToBlocks.parse("<img>"))
    }
}
