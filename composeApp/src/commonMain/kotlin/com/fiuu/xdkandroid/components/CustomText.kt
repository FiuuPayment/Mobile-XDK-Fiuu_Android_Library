package com.fiuu.xdkandroid.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiuu.xdkandroid.platform.roboto
import com.fiuu.xdkandroid.ui.model.TextSpan
import com.fiuu.xdkandroid.ui.model.buildDynamicAnnotatedString

@Composable
fun CustomText(
    label: String? = null,
    spans: List<TextSpan>? = null,
    errorText: String? = null,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Start,
    style: TextStyle? = null,
    color: Color? = null,
) {
    Column(modifier = modifier) {

        label?.let{
            Text(
                text = label,
                style = (style ?: MaterialTheme.typography.bodyMedium)
                    .merge(
                        color?.let { color ->
                            TextStyle(
                                fontWeight = fontWeight,
                                color = color
                            )
                        }
                    ),
                fontWeight = fontWeight,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        when {
            spans != null -> Text(
                text = remember(spans){buildDynamicAnnotatedString(spans)},
                textAlign = textAlign
            )
        }

        errorText?.let {
            Text(
                fontFamily = roboto,
                text = it,
                fontSize = 12.sp,
                fontWeight = fontWeight,
                color = MaterialTheme.colorScheme.error,
                modifier = modifier
            )
        }
    }
}