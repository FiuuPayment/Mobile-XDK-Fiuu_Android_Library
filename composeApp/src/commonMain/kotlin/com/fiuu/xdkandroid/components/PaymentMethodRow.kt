package com.fiuu.xdkandroid.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fiuu.xdkandroid.ui.AppIcons
import com.fiuu.xdkandroid.ui.SvgIcon
import com.fiuu.xdkandroid.ui.model.PayWithItem
import com.fiuu.xdkandroid.ui.theme.AppColors

@Composable
fun PaymentMethodRow(
    item: PayWithItem,
    onClick: () -> Unit
) {
    val categoryTag = "category_" + item.title.lowercase().replace('/', '_').replace(' ', '_')
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = categoryTag
                    testTag = categoryTag
                }
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.White
            ),
            border = BorderStroke(
                0.5.dp,
                Color(0xFFE0E0E0)
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomText(item.title, fontWeight = FontWeight.Medium)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.staticIcon != null) {
                        SvgIcon(
                            resDrawablePath = item.staticIcon,
                            contentDescription = item.staticCaption ?: item.title,
                            tint = AppColors.TextPrimary
                        )
                        item.staticCaption?.let {
                            CustomText(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextSecondary
                            )
                        }
                    } else {
                        item.channels
                            .take(3)
                            .forEach { channel ->
                                key(channel.id) {
                                    channel.logoUrl3?.let { url ->
                                        RemoteImage(
                                            url = url,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.size(24.dp),
                                            contentDescription = channel.title,
                                            onLoading = {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                            },
                                            onFailure = {
                                                // Empty fallback for inline channel preview
                                            },
                                        )
                                    }
                                }
                            }
                    }
                    SvgIcon(
                        imageVector = AppIcons.ExpandRight,
                        contentDescription = "Expand",
                    )
                }
            }
        }
    }
}
