package br.com.polegar.ndimonitor;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import java.io.PrintWriter;
import java.io.StringWriter;

/** Small local startup checkpoint. Does not load NDI, WebView or send reports. */
@android.annotation.SuppressLint("ApplySharedPref") // Checkpoints must reach disk before native code or process termination.
final class StartupJournal {
    static final String PREFERENCES = "monitor_startup_journal";
    private static StartupJournal instance;
    private final Context context;
    private final SharedPreferences preferences;
    private boolean recoveryRequired;
    private String previousReport;
    private long operation;

    static synchronized StartupJournal get(Context context) {
        if (instance == null) instance = new StartupJournal(context);
        return instance;
    }

    StartupJournal(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        recoveryRequired = preferences.getBoolean("startup_pending", false)
                || preferences.getBoolean("operation_pending", false)
                || preferences.getBoolean("crash_pending", false);
        previousReport = preferences.getString("last_report", "");
        if (recoveryRequired && !(preferences.getBoolean("crash_pending", false) && !previousReport.isEmpty())) {
            String phase = preferences.getString(preferences.getBoolean("operation_pending", false)
                    ? "operation_phase" : "startup_phase", "não identificada");
            String crash = preferences.getString("crash", "");
            previousReport = "A abertura anterior foi interrompida.\nÚltima etapa: " + phase
                    + "\n" + describeDevice() + (crash.isEmpty()
                    ? "\nSem exceção Java registrada. Uma interrupção não confirma, por si só, uma falha nativa."
                    : "\n\n" + crash);
            preferences.edit().putString("last_report", previousReport).commit();
        }
    }

    synchronized boolean needsRecovery() { return recoveryRequired; }
    synchronized String previousReport() { return previousReport; }

    synchronized void beginStartup() {
        preferences.edit().putBoolean("startup_pending", true)
                .putString("startup_phase", "Abrindo a interface do monitor")
                .putString("current_phase", "Abrindo a interface do monitor").commit();
    }

    synchronized void finishStartup() {
        if (preferences.getBoolean("startup_pending", false))
            preferences.edit().putBoolean("startup_pending", false).commit();
    }

    synchronized long beginOperation(String phase) {
        long token = ++operation;
        preferences.edit().putBoolean("operation_pending", true)
                .putString("operation_phase", phase).putString("current_phase", phase).commit();
        return token;
    }

    synchronized void finishOperation(long token) {
        if (token == operation) preferences.edit().putBoolean("operation_pending", false).commit();
    }

    synchronized void acknowledge() {
        recoveryRequired = false;
        preferences.edit().putBoolean("startup_pending", false)
                .putBoolean("operation_pending", false).putBoolean("crash_pending", false)
                .remove("crash").commit();
    }

    synchronized void recordCrash(Thread thread, Throwable error) {
        StringWriter trace = new StringWriter();
        error.printStackTrace(new PrintWriter(trace));
        String details = "Thread: " + thread.getName() + "\n" + trace;
        if (details.length() > 24000) details = details.substring(0, 24000);
        String phase = preferences.getString("current_phase", "não identificada");
        previousReport = "Exceção Java registrada.\nÚltima etapa: " + phase
                + "\n" + describeDevice() + "\n\n" + details;
        preferences.edit().putBoolean("crash_pending", true)
                .putString("crash", details).putString("last_report", previousReport).commit();
    }

    String describeDevice() {
        String version = "?";
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = info.versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException ignored) { }
        return "App: " + version + "\nPacote: " + context.getPackageName()
                + "\nAparelho: " + Build.MANUFACTURER + " " + Build.MODEL
                + "\nAndroid: " + Build.VERSION.RELEASE + " / API " + Build.VERSION.SDK_INT
                + "\nArquiteturas: " + android.text.TextUtils.join(", ", Build.SUPPORTED_ABIS)
                + "\nCompilação: " + Build.DISPLAY;
    }
}
