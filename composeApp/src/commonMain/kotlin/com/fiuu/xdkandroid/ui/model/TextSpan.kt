package com.fiuu.xdkandroid.ui.model

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

data class TextSpan(
    val text: String,
    val style: SpanStyle,
    val url: String? = null
)

fun buildDynamicAnnotatedString(
    spans: List<TextSpan>
): AnnotatedString {
    return buildAnnotatedString {
        spans.forEach { span ->
            val url = span.url
            if (url != null) {
                withLink(LinkAnnotation.Url(url, TextLinkStyles(style = span.style))) {
                    append(span.text)
                }
            } else {
                withStyle(span.style) {
                    append(span.text)
                }
            }
        }
    }
}