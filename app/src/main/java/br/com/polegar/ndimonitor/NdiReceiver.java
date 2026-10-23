package br.com.polegar.ndimonitor;

import android.os.Build;
import android.view.Surface;

/** Keeps the optional native receiver from preventing use of the HTML monitor. */
final class NdiReceiver {
    private static volatile LinkageError loadFailure;

    private interface NativeCall<T> { T run(); }

    private NdiReceiver() { }

    private static <T> T call(NativeCall<T> action, T unavailable) {
        // Do not initialize NativeBridge at all on API 23: its binaries require 24.
        if (Build.VERSION.SDK_INT < 24 || loadFailure != null) return unavailable;
        try {
            return action.run();
        } catch (LinkageError failure) {
            loadFailure = failure;
            return unavailable;
        }
    }

    static boolean isNdiAvailable() { return call(NativeBridge::isNdiAvailable, false); }
    static String[] findSources(int timeout) {
        return call(() -> NativeBridge.findSources(timeout), new String[0]);
    }
    static void releaseDiscovery() { call(() -> { NativeBridge.releaseDiscovery(); return null; }, null); }
    static void connect(String source) { call(() -> { NativeBridge.connect(source); return null; }, null); }
    static void disconnect() { call(() -> { NativeBridge.disconnect(); return null; }, null); }
    static void setSurface(Surface surface) { call(() -> { NativeBridge.setSurface(surface); return null; }, null); }
    static void setCallbackTarget(Object target) {
        call(() -> { NativeBridge.setCallbackTarget(target); return null; }, null);
    }
    static String failureDescription() {
        if (Build.VERSION.SDK_INT < 24) return "Android 6: modo HTML; NDI requer API 24 ou superior";
        LinkageError failure = loadFailure;
        return failure == null ? "Biblioteca carregada ou ainda não solicitada"
                : failure.getClass().getSimpleName() + ": " + failure.getMessage();
    }
}
