package com.pavel.pavelrssreader.domain.model

sealed class ContentBlock {
    data class Heading(val level: Int, val text: String) : ContentBlock()
    data class Paragraph(val spans: List<TextSpan>) : ContentBlock()
    data class Image(val url: String, val caption: String?) : ContentBlock()
    data class Quote(val text: String) : ContentBlock()
}

sealed class TextSpan {
    data class Plain(val text: String) : TextSpan()
    data class Bold(val text: String) : TextSpan()
    data class Italic(val text: String) : TextSpan()
    data class Link(val text: String, val url: String) : TextSpan()
}
