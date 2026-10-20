package com.fiuu.xdkandroid.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.fiuu.xdkandroid.ui.SvgIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopLabelOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
    enabled: Boolean = true,
    trailingIcon: ImageVector,
) {
    Column(modifier) {
        CustomText(
            label = label,
        )

        Spacer(Modifier.height(4.dp))

        // BasicTextField + the outlined decoration box instead of OutlinedTextField: the latter's
        // fixed 16dp vertical content padding needs ~56dp, so forcing it to 45dp clipped
        // descenders ("example", "Payment"). Smaller padding keeps the compact field, and
        // heightIn lets it grow with the system font scale instead of cropping the value.
        val interactionSource = remember { MutableInteractionSource() }
        val shape = RoundedCornerShape(corner = CornerSize(12.dp))
        val colors = OutlinedTextFieldDefaults.colors()
        val textColor = when {
            !enabled -> colors.disabledTextColor
            isError -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 45.dp),
            enabled = enabled,
            singleLine = true,
            textStyle = LocalTextStyle.current.merge(color = textColor),
            cursorBrush = SolidColor(if (isError) colors.errorCursorColor else colors.cursorColor),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = innerTextField,
                    enabled = enabled,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    isError = isError,
                    trailingIcon = {
                        SvgIcon(
                            imageVector = trailingIcon,
                            contentDescription = label,
                        )
                    },
                    colors = colors,
                    contentPadding = OutlinedTextFieldDefaults.contentPadding(top = 10.dp, bottom = 10.dp),
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = enabled,
                            isError = isError,
                            interactionSource = interactionSource,
                            colors = colors,
                            shape = shape,
                        )
                    },
                )
            },
        )
        if (isError && errorText != null) {
            CustomText(
                errorText = errorText,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}