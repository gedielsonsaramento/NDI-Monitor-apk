package br.com.polegar.ndimonitor;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.WindowInsets;

/** Keeps API 28/30 framework types out of the Activity verified by old ART. */
final class ControlInsets {
    private ControlInsets() { }

    static int[] read(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= 30) return Api30.read(insets);
        int[] safe = {insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()};
        if (Build.VERSION.SDK_INT >= 28) Api28.addCutout(insets, safe);
        return safe;
    }

    @TargetApi(28)
    @android.annotation.SuppressLint("UseRequiresApi") // Constructed only through the SDK-checked branch above.
    private static final class Api28 {
        static void addCutout(WindowInsets insets, int[] safe) {
            android.view.DisplayCutout cutout = insets.getDisplayCutout();
            if (cutout == null) return;
            safe[0] = Math.max(safe[0], cutout.getSafeInsetLeft());
            safe[1] = Math.max(safe[1], cutout.getSafeInsetTop());
            safe[2] = Math.max(safe[2], cutout.getSafeInsetRight());
            safe[3] = Math.max(safe[3], cutout.getSafeInsetBottom());
        }
    }

    @TargetApi(30)
    @android.annotation.SuppressLint("UseRequiresApi") // Constructed only through the SDK-checked branch above.
    private static final class Api30 {
        static int[] read(WindowInsets insets) {
            android.graphics.Insets safe = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            return new int[]{safe.left, safe.top, safe.right, safe.bottom};
        }
    }
}
