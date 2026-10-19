package com.fiuu.xdk.utils;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Edge-to-edge insets for the legacy Webcore screens, matching the native PaymentScaffold: the
 * status bar area shows the blue root background, the navigation bar area shows the white content.
 * fitsSystemWindows can't do this — it pads the root on every side, so the nav bar turned blue too.
 *
 * Expects the layout root to be a vertical container whose last child is the content area.
 */
public final class SystemBarInsets {

    private SystemBarInsets() {}

    public static void apply(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;
        View root = content.getChildAt(0);
        if (!(root instanceof ViewGroup) || ((ViewGroup) root).getChildCount() == 0) return;
        ViewGroup rootGroup = (ViewGroup) root;
        View body = rootGroup.getChildAt(rootGroup.getChildCount() - 1);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            // Keyboard included so card forms in the WebView stay above it, as fitsSystemWindows did.
            body.setPadding(0, 0, 0, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
