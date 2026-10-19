package com.fiuu.xdkandroid.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fiuu.xdkandroid.data.mapper.PaymentChannelImage
import com.fiuu.xdkandroid.domain.entity.PaymentChannel
import com.fiuu.xdkandroid.ui.AppIcons
import com.fiuu.xdkandroid.ui.SvgIcon
import com.fiuu.xdkandroid.ui.theme.AppColors
import com.fiuu.xdkandroid.ui.theme.AppColors.TextSecondary

// PAYMENT CHANNEL SELECTION
@Composable
fun ChannelGrid(
    channels: List<PaymentChannel>,
    selected: PaymentChannel?,
    onSelect: (PaymentChannel) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(channels) { channel ->
            ChannelGridItem(
                channel = channel,
                selected = channel == selected,
                onClick = { onSelect(channel) }
            )
        }
    }
}

@Composable
fun ChannelGridItem(
    channel: PaymentChannel,
    selected: Boolean,
    onClick: () -> Unit
) {
    val channelTag = "channel_" + (channel.name ?: channel.title ?: "unknown")
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .semantics {
                contentDescription = channelTag
                testTag = channelTag
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) AppColors.Secondary
            else Color(0xFFE0E0E0)
        ),
        colors = CardDefaults.cardColors(containerColor = if (selected) AppColors.Secondary.copy(alpha = 0.1f) else AppColors.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (channel.logoUrl3 == null && channel.maskName?.equals("Apple Pay", ignoreCase = true) == true) {
                // No hosted logo_url3 for Apple Pay's PLUGIN channel — use the same bundled
                // local asset the QuickPay row's Apple Pay button renders.
                PaymentChannelImage(
                    title = channel.name,
                    contentDescription = channel.title,
                    modifier = Modifier.size(60.dp)
                )
            } else if (channel.logoUrl3 != null) {
                RemoteImage(
                    url = channel.logoUrl3,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(60.dp),
                    contentDescription = channel.title,
                    onLoading = {
                        Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = AppColors.Primary.copy(alpha = 0.5f)
                            )
                        }
                    },
                    onFailure = {
                        SvgIcon(
                            imageVector = AppIcons.BrokenImage,
                            contentDescription = "Image Null",
                            tint = TextSecondary,
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.size(4.dp))
            // Long titles ("Bank Simpanan Nasional (FPX)") used to be clipped mid-line by the
            // card's fixed square height. autoSize shrinks the font until the title fits the
            // space left under the logo; line height is in em so it scales with the font.
            val titleStyle = MaterialTheme.typography.labelMedium
            BasicText(
                text = channel.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                style = titleStyle.merge(
                    color = LocalContentColor.current,
                    lineHeight = 1.25.em,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 2,
                overflow = TextOverflow.Clip,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 8.sp,
                    maxFontSize = titleStyle.fontSize,
                    stepSize = 0.5.sp,
                ),
            )
        }
    }
}