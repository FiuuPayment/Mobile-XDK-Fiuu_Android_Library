package com.fiuu.xdkandroid.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.fiuu.xdkandroid.ui.AppIcons
import com.fiuu.xdkandroid.ui.SvgIcon
import com.fiuu.xdkandroid.ui.theme.AppColors
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap

private val remoteImageMemoryCache = mutableMapOf<String, ImageBitmap>()

private val remoteImageHttpClient by lazy {
    HttpClient()
}

@Composable
fun RemoteImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onLoading: @Composable (() -> Unit)? = null,
    onFailure: @Composable (() -> Unit)? = null,
) {
    if (url.isNullOrBlank()) {
        if (onFailure != null) {
            onFailure()
        } else {
            SvgIcon(
                imageVector = AppIcons.BrokenImage,
                contentDescription = contentDescription,
                tint = AppColors.TextSecondary,
            )
        }
        return
    }

    var bitmapState by remember(url) { mutableStateOf(remoteImageMemoryCache[url]) }
    var isLoading by remember(url) { mutableStateOf(bitmapState == null) }
    var isError by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        if (bitmapState != null) {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        isError = false
        try {
            val bytes = withContext(Dispatchers.IO) {
                remoteImageHttpClient.get(url).readBytes()
            }
            val bitmap = bytes.decodeToImageBitmap()
            remoteImageMemoryCache[url] = bitmap
            bitmapState = bitmap
        } catch (_: Throwable) {
            isError = true
        } finally {
            isLoading = false
        }
    }

    when {
        bitmapState != null -> {
            Image(
                bitmap = bitmapState!!,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        isLoading -> {
            if (onLoading != null) {
                onLoading()
            } else {
                Box(modifier = modifier, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.Primary.copy(alpha = 0.5f)
                    )
                }
            }
        }
        else -> {
            if (onFailure != null) {
                onFailure()
            } else {
                SvgIcon(
                    imageVector = AppIcons.BrokenImage,
                    contentDescription = contentDescription,
                    tint = AppColors.TextSecondary,
                )
            }
        }
    }
}
