package com.fiuu.xdkandroid.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {
    val Close: ImageVector = Icons.Default.Close
    val Warning: ImageVector = Icons.Default.Warning
    val Person: ImageVector = Icons.Default.Person
    val Email: ImageVector = Icons.Default.Email
    val Phone: ImageVector = Icons.Default.Phone
    val ArrowBack: ImageVector = Icons.AutoMirrored.Filled.ArrowBack

    val Visibility: ImageVector by lazy {
        ImageVector.Builder(
            name = "Visibility",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 4.5f)
                curveTo(7.0f, 4.5f, 2.73f, 7.61f, 1.0f, 12.0f)
                curveToRelative(1.73f, 4.39f, 6.0f, 7.5f, 11.0f, 7.5f)
                reflectiveCurveToRelative(9.27f, -3.11f, 11.0f, -7.5f)
                curveToRelative(-1.73f, -4.39f, -6.0f, -7.5f, -11.0f, -7.5f)
                close()
                moveTo(12.0f, 17.0f)
                curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
                reflectiveCurveToRelative(2.24f, -5.0f, 5.0f, -5.0f)
                reflectiveCurveToRelative(5.0f, 2.24f, 5.0f, 5.0f)
                reflectiveCurveToRelative(-2.24f, 5.0f, -5.0f, 5.0f)
                close()
                moveTo(12.0f, 9.0f)
                curveToRelative(-1.66f, 0.0f, -3.0f, 1.34f, -3.0f, 3.0f)
                reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
                reflectiveCurveToRelative(3.0f, -1.34f, 3.0f, -3.0f)
                reflectiveCurveToRelative(-1.34f, -3.0f, -3.0f, -3.0f)
                close()
            }
        }.build()
    }

    val VisibilityOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "VisibilityOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 7.0f)
                curveToRelative(2.76f, 0.0f, 5.0f, 2.24f, 5.0f, 5.0f)
                curveToRelative(0.0f, 0.65f, -0.13f, 1.26f, -0.36f, 1.83f)
                lineToRelative(2.92f, 2.92f)
                curveToRelative(1.51f, -1.26f, 2.7f, -2.89f, 3.44f, -4.75f)
                curveToRelative(-1.73f, -4.39f, -6.0f, -7.5f, -11.0f, -7.5f)
                curveToRelative(-1.4f, 0.0f, -2.74f, 0.25f, -3.98f, 0.7f)
                lineToRelative(2.16f, 2.16f)
                curveTo(10.74f, 7.13f, 11.35f, 7.0f, 12.0f, 7.0f)
                close()
                moveTo(2.0f, 4.27f)
                lineToRelative(2.28f, 2.28f)
                lineToRelative(0.46f, 0.46f)
                curveTo(3.08f, 8.3f, 1.78f, 10.02f, 1.0f, 12.0f)
                curveToRelative(1.73f, 4.39f, 6.0f, 7.5f, 11.0f, 7.5f)
                curveToRelative(1.55f, 0.0f, 3.03f, -0.3f, 4.38f, -0.84f)
                lineToRelative(0.42f, 0.42f)
                lineTo(19.73f, 22.0f)
                lineTo(21.0f, 20.73f)
                lineTo(3.27f, 3.0f)
                lineTo(2.0f, 4.27f)
                close()
                moveTo(7.53f, 9.8f)
                lineToRelative(1.55f, 1.55f)
                curveToRelative(-0.05f, 0.21f, -0.08f, 0.43f, -0.08f, 0.65f)
                curveToRelative(0.0f, 1.66f, 1.34f, 3.0f, 3.0f, 3.0f)
                curveToRelative(0.22f, 0.0f, 0.44f, -0.03f, 0.65f, -0.08f)
                lineToRelative(1.55f, 1.55f)
                curveToRelative(-0.67f, 0.33f, -1.41f, 0.53f, -2.2f, 0.53f)
                curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
                curveToRelative(0.0f, -0.79f, 0.2f, -1.53f, 0.53f, -2.2f)
                close()
                moveTo(11.84f, 9.02f)
                lineToRelative(3.15f, 3.15f)
                lineToRelative(0.02f, -0.16f)
                curveToRelative(0.0f, -1.66f, -1.34f, -3.0f, -3.0f, -3.0f)
                lineToRelative(-0.17f, 0.01f)
                close()
            }
        }.build()
    }

    val BrokenImage: ImageVector by lazy {
        ImageVector.Builder(
            name = "BrokenImage",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(21.0f, 5.0f)
                verticalLineToRelative(6.59f)
                lineToRelative(-3.0f, -3.01f)
                lineToRelative(-4.0f, 4.01f)
                lineToRelative(-4.0f, -4.0f)
                lineToRelative(-4.0f, 4.0f)
                verticalLineTo(5.0f)
                curveToRelative(0.0f, -1.1f, 0.9f, -2.0f, 2.0f, -2.0f)
                horizontalLineToRelative(14.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, 0.9f, 2.0f, 2.0f)
                close()
                moveTo(18.0f, 11.42f)
                lineToRelative(3.0f, 3.01f)
                verticalLineTo(19.0f)
                curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f)
                horizontalLineTo(5.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
                verticalLineToRelative(-6.58f)
                lineToRelative(4.0f, -4.0f)
                lineToRelative(4.0f, 4.0f)
                lineToRelative(4.0f, -4.01f)
                lineToRelative(3.0f, 3.01f)
                close()
            }
        }.build()
    }

    val ExpandMore: ImageVector by lazy {
        ImageVector.Builder(
            name = "ExpandMore",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16.59f, 8.59f)
                lineTo(12.0f, 13.17f)
                lineTo(7.41f, 8.59f)
                lineTo(6.0f, 10.0f)
                lineToRelative(6.0f, 6.0f)
                lineToRelative(6.0f, -6.0f)
                close()
            }
        }.build()
    }

    val ExpandRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "ExpandRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8.59f, 16.59f)
                lineTo(13.17f, 12.0f)
                lineTo(8.59f, 7.41f)
                lineTo(10.0f, 6.0f)
                lineToRelative(6.0f, 6.0f)
                lineToRelative(-6.0f, 6.0f)
                close()
            }
        }.build()
    }

    val Description: ImageVector by lazy {
        ImageVector.Builder(
            name = "Description",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(14.0f, 2.0f)
                horizontalLineTo(6.0f)
                curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
                lineTo(4.0f, 20.0f)
                curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 1.99f, 2.0f)
                horizontalLineTo(18.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(8.0f)
                lineToRelative(-6.0f, -6.0f)
                close()
                moveTo(16.0f, 18.0f)
                horizontalLineTo(8.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(16.0f, 14.0f)
                horizontalLineTo(8.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(13.0f, 9.0f)
                verticalLineTo(3.5f)
                lineTo(18.5f, 9.0f)
                horizontalLineTo(13.0f)
                close()
            }
        }.build()
    }

    val ApplePay: ImageVector by lazy {
        ImageVector.Builder(
            name = "ApplePay",
            defaultWidth = 40.dp,
            defaultHeight = 40.dp,
            viewportWidth = 170f,
            viewportHeight = 170f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(132.8f, 117.8f)
                curveToRelative(-3.4f, 7.8f, -7.5f, 14.9f, -12.4f, 21.2f)
                curveToRelative(-6.6f, 8.6f, -13.5f, 17.2f, -24.4f, 17.4f)
                curveToRelative(-10.7f, 0.2f, -14.2f, -6.4f, -26.5f, -6.4f)
                curveToRelative(-12.4f, 0.0f, -16.3f, 6.2f, -26.5f, 6.6f)
                curveToRelative(-10.5f, 0.4f, -18.5f, -9.6f, -25.2f, -19.2f)
                curveToRelative(-13.6f, -19.6f, -24.1f, -55.5f, -10.0f, -79.9f)
                curveToRelative(7.0f, -12.1f, 19.5f, -19.8f, 33.0f, -20.0f)
                curveToRelative(10.2f, -0.2f, 20.0f, 6.9f, 26.3f, 6.9f)
                curveToRelative(6.2f, 0.0f, 18.0f, -8.5f, 30.5f, -7.3f)
                curveToRelative(5.2f, 0.2f, 19.8f, 2.1f, 29.2f, 15.8f)
                curveToRelative(-0.8f, 0.5f, -17.4f, 10.2f, -17.2f, 30.5f)
                curveToRelative(0.2f, 24.2f, 21.2f, 32.2f, 21.5f, 32.4f)
                curveToRelative(-0.2f, 0.6f, -3.4f, 11.5f, -11.0f, 22.6f)
                close()
                moveTo(110.8f, 38.2f)
                curveToRelative(5.1f, -6.2f, 8.6f, -14.9f, 7.7f, -23.6f)
                curveToRelative(-7.5f, 0.3f, -17.2f, 5.4f, -22.5f, 11.5f)
                curveToRelative(-4.6f, 5.3f, -8.8f, 14.2f, -7.7f, 22.6f)
                curveToRelative(8.4f, 0.7f, 17.4f, -4.4f, 22.5f, -10.5f)
                close()
            }
        }.build()
    }

    val GooglePay: ImageVector by lazy {
        ImageVector.Builder(
            name = "GooglePay",
            defaultWidth = 40.dp,
            defaultHeight = 40.dp,
            viewportWidth = 48f,
            viewportHeight = 48f
        ).apply {
            // Google G multi-color mark centered
            path(fill = SolidColor(Color(0xFF4285F4))) {
                moveTo(43.61f, 24.59f)
                curveToRelative(0.0f, -1.56f, -0.14f, -3.06f, -0.4f, -4.5f)
                horizontalLineTo(24.0f)
                verticalLineToRelative(8.51f)
                horizontalLineToRelative(11.01f)
                curveToRelative(-0.47f, 2.56f, -1.91f, 4.73f, -4.07f, 6.18f)
                verticalLineToRelative(5.14f)
                horizontalLineToRelative(6.59f)
                curveToRelative(3.86f, -3.55f, 6.08f, -8.79f, 6.08f, -15.33f)
                close()
            }
            path(fill = SolidColor(Color(0xFF34A853))) {
                moveTo(24.0f, 44.5f)
                curveToRelative(5.53f, 0.0f, 10.18f, -1.84f, 13.57f, -4.98f)
                lineToRelative(-6.59f, -5.14f)
                curveToRelative(-1.84f, 1.23f, -4.19f, 1.96f, -6.98f, 1.96f)
                curveToRelative(-5.37f, 0.0f, -9.92f, -3.63f, -11.55f, -8.51f)
                horizontalLineTo(5.62f)
                verticalLineToRelative(5.31f)
                curveTo(9.0f, 39.84f, 15.93f, 44.5f, 24.0f, 44.5f)
                close()
            }
            path(fill = SolidColor(Color(0xFFFBBC05))) {
                moveTo(12.45f, 27.83f)
                curveToRelative(-0.42f, -1.25f, -0.66f, -2.59f, -0.66f, -3.83f)
                reflectiveCurveToRelative(0.24f, -2.58f, 0.66f, -3.83f)
                verticalLineToRelative(-5.31f)
                horizontalLineTo(5.62f)
                curveTo(4.25f, 17.6f, 3.5f, 20.71f, 3.5f, 24.0f)
                reflectiveCurveToRelative(0.75f, 6.4f, 2.12f, 8.97f)
                lineToRelative(6.83f, -5.14f)
                close()
            }
            path(fill = SolidColor(Color(0xFFEA4335))) {
                moveTo(24.0f, 11.66f)
                curveToRelative(3.01f, 0.0f, 5.71f, 1.04f, 7.84f, 3.07f)
                lineToRelative(5.88f, -5.88f)
                curveTo(34.17f, 5.56f, 29.53f, 3.5f, 24.0f, 3.5f)
                curveTo(15.93f, 3.5f, 9.0f, 8.16f, 5.62f, 14.86f)
                lineToRelative(6.83f, 5.31f)
                curveToRelative(1.63f, -4.88f, 6.18f, -8.51f, 11.55f, -8.51f)
                close()
            }
        }.build()
    }

    val SamsungPay: ImageVector by lazy {
        ImageVector.Builder(
            name = "SamsungPay",
            defaultWidth = 40.dp,
            defaultHeight = 40.dp,
            viewportWidth = 40f,
            viewportHeight = 40f
        ).apply {
            // Samsung blue background rounded rect
            path(fill = SolidColor(Color(0xFF1428A0))) {
                moveTo(4.0f, 0.0f)
                horizontalLineToRelative(32.0f)
                curveToRelative(2.21f, 0.0f, 4.0f, 1.79f, 4.0f, 4.0f)
                verticalLineToRelative(32.0f)
                curveToRelative(0.0f, 2.21f, -1.79f, 4.0f, -4.0f, 4.0f)
                horizontalLineTo(4.0f)
                curveToRelative(-2.21f, 0.0f, -4.0f, -1.79f, -4.0f, -4.0f)
                verticalLineTo(4.0f)
                curveToRelative(0.0f, -2.21f, 1.79f, -4.0f, 4.0f, -4.0f)
                close()
            }
            // Samsung S curve mark in white (fill)
            path(fill = SolidColor(Color.White)) {
                moveTo(26.5f, 13.5f)
                curveTo(25.0f, 11.8f, 22.8f, 11.0f, 20.0f, 11.0f)
                curveToRelative(-3.8f, 0.0f, -6.5f, 2.2f, -6.5f, 5.5f)
                curveToRelative(0.0f, 3.3f, 2.3f, 4.7f, 6.5f, 6.0f)
                curveToRelative(4.2f, 1.3f, 6.5f, 2.7f, 6.5f, 6.0f)
                curveToRelative(0.0f, 3.3f, -2.7f, 5.5f, -6.5f, 5.5f)
                curveToRelative(-2.8f, 0.0f, -5.0f, -0.8f, -6.5f, -2.5f)
                lineToRelative(1.8f, -1.8f)
                curveToRelative(1.1f, 1.2f, 2.7f, 1.8f, 4.7f, 1.8f)
                curveToRelative(2.4f, 0.0f, 4.0f, -1.3f, 4.0f, -3.0f)
                curveToRelative(0.0f, -1.8f, -1.5f, -2.8f, -4.8f, -3.8f)
                curveToRelative(-4.2f, -1.3f, -5.7f, -2.9f, -5.7f, -5.7f)
                curveToRelative(0.0f, -2.8f, 2.3f, -4.5f, 5.5f, -4.5f)
                curveToRelative(2.3f, 0.0f, 4.2f, 0.7f, 5.4f, 2.0f)
                close()
            }
        }.build()
    }

    val CreditCard: ImageVector by lazy {
        ImageVector.Builder(
            name = "CreditCard",
            defaultWidth = 40.dp,
            defaultHeight = 40.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF1F1F1F))) {
                moveTo(20.0f, 4.0f)
                horizontalLineTo(4.0f)
                curveToRelative(-1.11f, 0.0f, -1.99f, 0.89f, -1.99f, 2.0f)
                lineTo(2.0f, 18.0f)
                curveToRelative(0.0f, 1.11f, 0.89f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(16.0f)
                curveToRelative(1.11f, 0.0f, 2.0f, -0.89f, 2.0f, -2.0f)
                verticalLineTo(6.0f)
                curveToRelative(0.0f, -1.11f, -0.89f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(20.0f, 18.0f)
                horizontalLineTo(4.0f)
                verticalLineToRelative(-6.0f)
                horizontalLineToRelative(16.0f)
                verticalLineToRelative(6.0f)
                close()
                moveTo(20.0f, 8.0f)
                horizontalLineTo(4.0f)
                verticalLineTo(6.0f)
                horizontalLineToRelative(16.0f)
                verticalLineToRelative(2.0f)
                close()
            }
        }.build()
    }

    // Small arrow/paper-plane mark used next to the "Fiuu" wordmark in the "Powered by" footer.
    val FiuuMark: ImageVector by lazy {
        ImageVector.Builder(
            name = "FiuuMark",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(2.01f, 21.0f)
                lineTo(23.0f, 12.0f)
                lineTo(2.01f, 3.0f)
                lineTo(2.0f, 10.0f)
                lineToRelative(15.0f, 2.0f)
                lineToRelative(-15.0f, 2.0f)
                close()
            }
        }.build()
    }

    val DefaultChannel: ImageVector by lazy {
        ImageVector.Builder(
            name = "DefaultChannel",
            defaultWidth = 40.dp,
            defaultHeight = 40.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF555555))) {
                moveTo(21.0f, 18.0f)
                verticalLineToRelative(1.0f)
                curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f)
                horizontalLineTo(5.0f)
                curveToRelative(-1.11f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
                verticalLineTo(5.0f)
                curveToRelative(0.0f, -1.1f, 0.89f, -2.0f, 2.0f, -2.0f)
                horizontalLineToRelative(14.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, 0.9f, 2.0f, 2.0f)
                verticalLineToRelative(1.0f)
                horizontalLineToRelative(-9.0f)
                curveToRelative(-1.11f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(8.0f)
                curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(9.0f)
                close()
                moveTo(12.0f, 16.0f)
                horizontalLineToRelative(10.0f)
                verticalLineTo(8.0f)
                horizontalLineTo(12.0f)
                verticalLineToRelative(8.0f)
                close()
                moveTo(16.0f, 13.5f)
                curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
                reflectiveCurveToRelative(0.67f, -1.5f, 1.5f, -1.5f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                reflectiveCurveToRelative(-0.67f, 1.5f, -1.5f, 1.5f)
                close()
            }
        }.build()
    }
}