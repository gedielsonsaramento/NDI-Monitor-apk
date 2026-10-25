package br.com.polegar.ndimonitor;

import android.content.Context;
import android.view.Surface;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 25}, shadows = {TvBoxCompatibilityTest.UnloadableReceiver.class,
        MainActivityModeTest.FakeHtmlDiscovery.class})
public class TvBoxCompatibilityTest {
    private static final String URL = "http://192.168.2.60:5050/output/retono";

    @Implements(value = NativeBridge.class, isInAndroidSdk = false)
    public static class UnloadableReceiver {
        static int calls;
        @Implementation protected static void __staticInitializer__() { }
        @Implementation protected static void setCallbackTarget(Object target) {
            calls++;
            throw new UnsatisfiedLinkError("biblioteca indisponível no teste");
        }
        @Implementation protected static boolean isNdiAvailable() {
            calls++;
            throw new UnsatisfiedLinkError("biblioteca indisponível no teste");
        }
        @Implementation protected static void setSurface(Surface surface) { calls++; }
        @Implementation protected static void disconnect() { calls++; }
        @Implementation protected static void releaseDiscovery() { calls++; }
        @Implementation protected static String[] findSources(int timeout) { calls++; return new String[0]; }
        @Implementation protected static void connect(String source) { calls++; }
    }

    @Before public void reset() {
        ReflectionHelpers.setStaticField(NdiReceiver.class, "loadFailure", null);
        ReflectionHelpers.setStaticField(NdiReceiver.class, "initializationAttempted", false);
        ReflectionHelpers.setStaticField(NdiReceiver.class, "available", false);
        ReflectionHelpers.setStaticField(NdiReceiver.class, "callbackTarget", null);
        ReflectionHelpers.setStaticField(NdiReceiver.class, "pendingSurface", null);
        UnloadableReceiver.calls = 0;
        MainActivityModeTest.FakeHtmlDiscovery.starts = 0;
        RuntimeEnvironment.getApplication().getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE)
                .edit().clear().putString(MainActivity.KEY_DISPLAY_MODE, "html")
                .putString(MainActivity.KEY_HTML_URL, URL).commit();
    }

    @Test public void nativeLoadFailureDoesNotPreventHtmlRestoreOrDeviceDiagnostics() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            assertEquals(View.VISIBLE, activity.findViewById(R.id.html_container).getVisibility());
            assertEquals(View.GONE, activity.findViewById(R.id.source_panel).getVisibility());
            assertEquals(0, MainActivityModeTest.FakeHtmlDiscovery.starts);
            assertEquals(0, UnloadableReceiver.calls);
            activity.onBackPressed();
            activity.findViewById(R.id.monitor_settings_button).performClick();
            android.app.AlertDialog settings = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(settings.findViewById(R.id.device_diagnostics_button).performClick());
            android.app.AlertDialog diagnostics = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(diagnostics.isShowing());
            assertTrue(DeviceDiagnostics.describe(activity, false).contains("API " + android.os.Build.VERSION.SDK_INT));
            diagnostics.dismiss();
            settings.dismiss();
            assertEquals(URL, activity.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE)
                    .getString(MainActivity.KEY_HTML_URL, ""));
            assertEquals(0, MainActivityModeTest.FakeHtmlDiscovery.starts);
            assertEquals(0, UnloadableReceiver.calls);
        }
    }

    @Test @Config(sdk = 23)
    public void api23NeverLoadsNativeForSurfaceConnectOrCleanup() {
        NdiReceiver.setCallbackTarget(new Object());
        NdiReceiver.setSurface(null);
        NdiReceiver.connect("PC (Palco)");
        NdiReceiver.findSources(20);
        NdiReceiver.disconnect();
        NdiReceiver.releaseDiscovery();
        assertEquals(0, UnloadableReceiver.calls);
    }

    @Test @Config(qualifiers = "land-xhdpi")
    public void compatibilityUsesSystemDensityAndLegacyNavigationStaysInsideWeb() {
        try (ActivityController<android.app.Activity> controller =
                     Robolectric.buildActivity(android.app.Activity.class).setup()) {
            android.app.Activity activity = controller.get();
            FrameLayout frame = new FrameLayout(activity);
            HtmlMonitor monitor = new HtmlMonitor(activity, frame, new HtmlMonitor.Listener() {
                public void onStatus(int message) { }
                public void onInteraction() { }
            });
            monitor.setDisplayLayout(true, true);
            monitor.resume();
            monitor.show(URL);
            HtmlStageViewport viewport = (HtmlStageViewport) frame.getChildAt(0);
            WebView web = (WebView) viewport.getChildAt(0);
            assertEquals(activity.getResources().getDisplayMetrics().density,
                    web.getResources().getDisplayMetrics().density, 0);
            assertTrue(viewport.usesStageLayout());
            assertTrue(Shadows.shadowOf(web).getWebViewClient()
                    .shouldOverrideUrlLoading(web, "intent://outside/app"));
            assertFalse(Shadows.shadowOf(web).getWebViewClient().shouldOverrideUrlLoading(web, URL));
            assertEquals(URL, Shadows.shadowOf(web).getLastLoadedUrl());
            monitor.destroy();
        }
    }
}
