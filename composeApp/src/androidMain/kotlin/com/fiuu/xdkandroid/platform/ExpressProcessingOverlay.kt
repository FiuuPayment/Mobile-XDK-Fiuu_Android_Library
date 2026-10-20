package com.fiuu.xdkandroid.platform

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

/**
 * Full-screen "Processing payment..." overlay on the host's own activity, for the express wallet
 * buttons the host renders itself (presentGooglePayExpressButton / presentSamsungPayExpressButton).
 * Once the wallet sheet returns a token, /express — and, after a 3DS page closes, the /inquire
 * confirmation — run with only the host's screen showing, so its buttons stayed tappable and the
 * payment looked stalled until the 3DS page appeared.
 *
 * Plain Android Views on android.R.id.content (same as AndroidWebViewBridge's WebView), since the
 * host may not be Compose. It's added before any 3DS WebView, so that WebView — added later and
 * kept INVISIBLE until its page renders — sits on top of it, and this keeps showing underneath
 * while the page loads. Same look as FiuuPaymentSDK's FullScreenLoadingOverlay.
 */
internal class ExpressProcessingOverlay(private val activity: Activity) {
    private var overlay: View? = null
    private var backPressedCallback: OnBackPressedCallback? = null

    fun show(message: String = "Processing payment...") = activity.runOnUiThread {
        removeOverlay()
        if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return@runOnUiThread
        val density = activity.resources.displayMetrics.density

        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(ProgressBar(activity).apply {
                isIndeterminate = true
                indeterminateTintList = ColorStateList.valueOf(Color.WHITE)
            })
            addView(TextView(activity).apply {
                text = message
                setTextColor(Color.WHITE)
                textSize = 16f
                setPadding(0, (16 * density).toInt(), 0, 0)
            })
        }
        val root = FrameLayout(activity).apply {
            // Color.Black at 0.6 alpha, as in FullScreenLoadingOverlay.
            setBackgroundColor(0x99000000.toInt())
            // Swallows every tap so nothing on the host screen underneath can be pressed.
            isClickable = true
            isFocusable = true
            addView(column, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ))
        }
        content.addView(root, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        overlay = root

        // Back would otherwise leave the host screen mid-payment. A 3DS WebView registers its own
        // callback later, which takes precedence while that page is showing.
        (activity as? ComponentActivity)?.let { componentActivity ->
            val callback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            }
            componentActivity.onBackPressedDispatcher.addCallback(componentActivity, callback)
            backPressedCallback = callback
        }
    }

    fun hide() = activity.runOnUiThread { removeOverlay() }

    private fun removeOverlay() {
        backPressedCallback?.remove()
        backPressedCallback = null
        overlay?.let { (it.parent as? ViewGroup)?.removeView(it) }
        overlay = null
    }
}
