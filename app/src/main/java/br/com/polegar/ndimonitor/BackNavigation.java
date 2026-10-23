package br.com.polegar.ndimonitor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

/** Isolates API 33 classes from the Activity loaded on old TV firmware. */
final class BackNavigation {
    private BackNavigation() { }

    static Object register(Activity activity, Runnable action) {
        return Build.VERSION.SDK_INT >= 33 ? Api33.register(activity, action) : null;
    }

    static void unregister(Activity activity, Object callback) {
        if (Build.VERSION.SDK_INT >= 33 && callback != null) Api33.unregister(activity, callback);
    }

    @TargetApi(33)
    @android.annotation.SuppressLint("UseRequiresApi") // Private helper; both callers explicitly check SDK_INT.
    private static final class Api33 {
        static Object register(Activity activity, Runnable action) {
            OnBackInvokedCallback callback = action::run;
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
            return callback;
        }
        static void unregister(Activity activity, Object callback) {
            activity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(
                    (OnBackInvokedCallback) callback);
        }
    }
}
