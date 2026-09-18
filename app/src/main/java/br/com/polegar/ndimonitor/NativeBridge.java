package br.com.polegar.ndimonitor;

import android.view.Surface;

public final class NativeBridge {
    static {
        System.loadLibrary("ndimonitor");
    }

    private NativeBridge() {}

    public static native boolean isNdiAvailable();
    public static native String[] findSources(int timeoutMs);
    public static native void connect(String sourceName);
    public static native void disconnect();
    public static native void setSurface(Surface surface);
    public static native void setCallbackTarget(Object target);
}
