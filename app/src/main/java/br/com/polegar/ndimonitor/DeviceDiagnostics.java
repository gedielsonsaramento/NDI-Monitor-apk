package br.com.polegar.ndimonitor;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

/** Device-local information, only read when the user opens the diagnostics dialog. */
final class DeviceDiagnostics {
    private DeviceDiagnostics() { }

    static String describe(Context context, boolean ndiAvailable) {
        String version;
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = info.versionName + " (" + info.versionCode + ")";
        } catch (PackageManager.NameNotFoundException missing) {
            version = "indisponível";
        }
        return "App: " + version + "\nPacote: " + context.getPackageName()
                + "\nAparelho: " + Build.MANUFACTURER + " " + Build.MODEL
                + "\nAndroid: " + Build.VERSION.RELEASE + " — API " + Build.VERSION.SDK_INT
                + "\nArquiteturas: " + android.text.TextUtils.join(", ", Build.SUPPORTED_ABIS)
                + "\nDensidade do sistema: " + context.getResources().getDisplayMetrics().densityDpi + " dpi"
                + "\nNDI: " + (ndiAvailable ? "disponível" : "indisponível")
                + "\n" + NdiReceiver.failureDescription();
    }
}
