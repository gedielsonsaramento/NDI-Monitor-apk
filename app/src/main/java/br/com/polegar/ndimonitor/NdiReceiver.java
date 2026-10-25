package br.com.polegar.ndimonitor;

import android.os.Build;
import android.view.Surface;

/** Keeps the optional native receiver from preventing use of the HTML monitor. */
final class NdiReceiver {
    private static volatile LinkageError loadFailure;
    private static volatile boolean initializationAttempted;
    private static volatile boolean available;
    private static volatile Object callbackTarget;
    private static volatile Surface pendingSurface;

    private interface NativeCall<T> { T run(); }

    private NdiReceiver() { }

    private static <T> T call(NativeCall<T> action, T unavailable) {
        // Do not initialize NativeBridge at all on API 23: its binaries require 24.
        if (Build.VERSION.SDK_INT < 24 || !available || loadFailure != null) return unavailable;
        try {
            return action.run();
        } catch (LinkageError failure) {
            loadFailure = failure;
            return unavailable;
        }
    }

    // Only an explicit choice of NDI may initialize the native runtime. Surface
    // callbacks and HTML cleanup must remain safe before that choice.
    static synchronized boolean isNdiAvailable() {
        if (Build.VERSION.SDK_INT < 24 || loadFailure != null) return false;
        if (!initializationAttempted) {
            initializationAttempted = true;
            try {
                available = NativeBridge.isNdiAvailable();
                if (available) {
                    NativeBridge.setCallbackTarget(callbackTarget);
                    Surface surface = pendingSurface;
                    if (surface != null && surface.isValid()) NativeBridge.setSurface(surface);
                }
            } catch (LinkageError failure) {
                available = false;
                loadFailure = failure;
            }
        }
        return available && loadFailure == null;
    }
    static String[] findSources(int timeout) {
        return call(() -> NativeBridge.findSources(timeout), new String[0]);
    }
    static void releaseDiscovery() { call(() -> { NativeBridge.releaseDiscovery(); return null; }, null); }
    static void connect(String source) { call(() -> { NativeBridge.connect(source); return null; }, null); }
    static void disconnect() { call(() -> { NativeBridge.disconnect(); return null; }, null); }
    static void setSurface(Surface surface) {
        pendingSurface = surface;
        call(() -> { NativeBridge.setSurface(surface); return null; }, null);
    }
    static void setCallbackTarget(Object target) {
        callbackTarget = target;
        call(() -> { NativeBridge.setCallbackTarget(target); return null; }, null);
    }
    static String failureDescription() {
        if (Build.VERSION.SDK_INT < 24) return "Android 6: modo HTML; NDI requer API 24 ou superior";
        LinkageError failure = loadFailure;
        if (failure != null) return failure.getClass().getSimpleName() + ": " + failure.getMessage();
        if (!initializationAttempted) return "NDI ainda não inicializado; HTML disponível";
        return available ? "NDI inicializado" : "NDI indisponível neste aparelho";
    }
}
