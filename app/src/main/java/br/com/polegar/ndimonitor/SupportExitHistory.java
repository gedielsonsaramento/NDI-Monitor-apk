package br.com.polegar.ndimonitor;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;
import java.util.List;

/** Only the reason for our own process exit; never reads tombstones, logs or traces. */
final class SupportExitHistory {
    interface Reader { Exit latest(); }
    static final class Exit {
        final long timestampMs;
        final String kind;
        final boolean normal;
        Exit(long timestampMs, String kind, boolean normal) {
            this.timestampMs = timestampMs;
            this.kind = kind;
            this.normal = normal;
        }
    }

    static Reader reader(Context context) {
        Context app = context.getApplicationContext();
        return () -> {
            if (Build.VERSION.SDK_INT < 30) return null;
            try { return Api30.latest(app); }
            catch (RuntimeException | LinkageError unavailable) { return null; }
        };
    }

    @android.annotation.TargetApi(30)
    // The project deliberately has no AndroidX runtime dependency; the caller gates API 30.
    @android.annotation.SuppressLint("UseRequiresApi")
    private static final class Api30 {
        static Exit latest(Context context) {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager == null) return null;
            List<ApplicationExitInfo> exits = manager.getHistoricalProcessExitReasons(context.getPackageName(), 0, 1);
            if (exits == null || exits.isEmpty()) return null;
            ApplicationExitInfo exit = exits.get(0);
            String kind = "";
            boolean normal = false;
            switch (exit.getReason()) {
                case ApplicationExitInfo.REASON_CRASH: kind = "java_crash"; break;
                case ApplicationExitInfo.REASON_CRASH_NATIVE: kind = "native_crash"; break;
                case ApplicationExitInfo.REASON_ANR: kind = "anr"; break;
                case ApplicationExitInfo.REASON_LOW_MEMORY: kind = "memory_pressure"; break;
                case ApplicationExitInfo.REASON_INITIALIZATION_FAILURE: kind = "initialization_failure"; break;
                case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE: kind = "resource_limit"; break;
                case ApplicationExitInfo.REASON_EXIT_SELF:
                case ApplicationExitInfo.REASON_USER_REQUESTED:
                case ApplicationExitInfo.REASON_USER_STOPPED: normal = true; break;
                default: break;
            }
            return new Exit(exit.getTimestamp(), kind, normal);
        }
    }

    private SupportExitHistory() { }
}
