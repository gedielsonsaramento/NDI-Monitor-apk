package br.com.polegar.ndimonitor;

import android.content.Context;
import android.view.Surface;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ListView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, shadows = {MainActivityModeTest.NoNativeReceiver.class, MainActivityModeTest.FakeHtmlDiscovery.class})
public class MainActivityModeTest {
    @Implements(value = LanHtmlDiscovery.class, isInAndroidSdk = false)
    public static class FakeHtmlDiscovery {
        static LanHtmlDiscovery.Listener listener;
        static int starts;
        @Implementation protected void __constructor__(Context context) { }
        @Implementation protected void start(String address, LanHtmlDiscovery.Listener callback) {
            listener = callback;
            ++starts;
            callback.onStarted(253, false);
        }
        @Implementation protected void stop() { }
    }
    private void drainWorkers() throws Exception {
        for (String name : new String[]{"CAPTURE_WORKER", "DISCOVERY_WORKER"}) {
            java.util.concurrent.ExecutorService worker = org.robolectric.util.ReflectionHelpers
                    .getStaticField(MainActivity.class, name);
            worker.submit(() -> { }).get(5, java.util.concurrent.TimeUnit.SECONDS);
        }
    }
    @Before public void resetDiscovery() throws Exception {
        drainWorkers();
        FakeHtmlDiscovery.listener = null;
        FakeHtmlDiscovery.starts = 0;
        NoNativeReceiver.available = false;
        NoNativeReceiver.finds.set(0);
        NoNativeReceiver.releases.set(0);
        NoNativeReceiver.disconnects.set(0);
        NoNativeReceiver.connected.clear();
    }
    @Implements(value = NativeBridge.class, isInAndroidSdk = false)
    public static class NoNativeReceiver {
        static volatile boolean available;
        static final java.util.concurrent.atomic.AtomicInteger finds = new java.util.concurrent.atomic.AtomicInteger();
        static final java.util.concurrent.atomic.AtomicInteger releases = new java.util.concurrent.atomic.AtomicInteger();
        static final java.util.concurrent.atomic.AtomicInteger disconnects = new java.util.concurrent.atomic.AtomicInteger();
        static final java.util.List<String> connected = new java.util.concurrent.CopyOnWriteArrayList<>();
        @Implementation protected static void __staticInitializer__() { }
        @Implementation protected static void setCallbackTarget(Object target) { }
        @Implementation protected static boolean isNdiAvailable() { return available; }
        @Implementation protected static void setSurface(Surface surface) { }
        @Implementation protected static void connect(String source) { connected.add(source); }
        @Implementation protected static void disconnect() { disconnects.incrementAndGet(); }
        @Implementation protected static void releaseDiscovery() { releases.incrementAndGet(); }
        @Implementation protected static String[] findSources(int timeout) {
            finds.incrementAndGet();
            return new String[]{"PC (Palco)"};
        }
    }

    @Test public void htmlWorksWhenNdiSdkIsUnavailableAndIgnoresLateNativeStatus() {
        android.content.SharedPreferences prefs = RuntimeEnvironment.getApplication()
                .getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        prefs.edit().clear().putString(MainActivity.KEY_DISPLAY_MODE, "html").commit();
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            assertEquals(View.VISIBLE, activity.findViewById(R.id.html_actions).getVisibility());
            assertEquals(View.VISIBLE, activity.findViewById(R.id.source_list).getVisibility());
            assertTrue(activity.findViewById(R.id.scan_button).isEnabled());
            activity.findViewById(R.id.manual_html_button).performClick();
            android.app.AlertDialog addressDialog = ShadowAlertDialog.getLatestAlertDialog();
            ((EditText) addressDialog.findViewById(R.id.html_url)).setText("192.168.2.53");
            addressDialog.findViewById(R.id.open_html_button).performClick();
            assertEquals("http://192.168.2.53:5050/output/saida-html-1",
                    prefs.getString(MainActivity.KEY_HTML_URL, ""));
            assertEquals(View.GONE, activity.findViewById(R.id.source_panel).getVisibility());
            TextView status = activity.findViewById(R.id.status_text);
            activity.onNativeStatus("old NDI status");
            assertNotEquals("old NDI status", status.getText().toString());
            activity.onBackPressed();
            activity.findViewById(R.id.source_mode_button).performClick();
            android.app.AlertDialog chooser = ShadowAlertDialog.getLatestAlertDialog();
            chooser.getListView().performItemClick(null, 0, 0);
            // Browsing an unavailable mode must not replace the selected HTML source.
            assertEquals("html", prefs.getString(MainActivity.KEY_DISPLAY_MODE, ""));
            assertEquals(View.GONE, activity.findViewById(R.id.html_container).getVisibility());
            assertEquals(View.VISIBLE, activity.findViewById(R.id.source_list).getVisibility());
        }
    }

    @Test public void htmlDisplayChoiceIsSavedWithoutChangingSourceOrRestartingDiscovery() {
        android.content.SharedPreferences prefs = RuntimeEnvironment.getApplication()
                .getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        String url = "http://192.168.2.60:5050/output/saida-html-1";
        prefs.edit().clear().putString(MainActivity.KEY_DISPLAY_MODE, "html")
                .putString(MainActivity.KEY_HTML_URL, url).commit();
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            android.widget.FrameLayout frame = activity.findViewById(R.id.html_container);
            assertTrue(((HtmlStageViewport) frame.getChildAt(0)).usesStageLayout());
            activity.onBackPressed();
            activity.findViewById(R.id.monitor_settings_button).performClick();
            android.app.AlertDialog settings = ShadowAlertDialog.getLatestAlertDialog();
            settings.findViewById(R.id.html_layout_button).performClick();
            android.app.AlertDialog chooser = ShadowAlertDialog.getLatestAlertDialog();
            chooser.getListView().performItemClick(null, 1, 1);
            assertFalse(prefs.getBoolean(MainActivity.KEY_HTML_STAGE_LAYOUT, true));
            assertFalse(((HtmlStageViewport) frame.getChildAt(0)).usesStageLayout());
            settings.dismiss();
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            controller.recreate();
            frame = controller.get().findViewById(R.id.html_container);
            assertFalse(((HtmlStageViewport) frame.getChildAt(0)).usesStageLayout());
            assertEquals(url, prefs.getString(MainActivity.KEY_HTML_URL, ""));
            assertEquals("html", prefs.getString(MainActivity.KEY_DISPLAY_MODE, ""));
            assertEquals(0, FakeHtmlDiscovery.starts);
        }
    }

    @Test public void invalidAddressDoesNotReplaceSavedAddressOrCloseForm() {
        android.content.SharedPreferences prefs = RuntimeEnvironment.getApplication()
                .getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        prefs.edit().clear().putString(MainActivity.KEY_DISPLAY_MODE, "html").commit();
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            activity.findViewById(R.id.manual_html_button).performClick();
            android.app.AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
            EditText editor = dialog.findViewById(R.id.html_url);
            editor.setText("file:///private/file");
            dialog.findViewById(R.id.open_html_button).performClick();
            assertNotNull(editor.getError());
            assertFalse(prefs.contains(MainActivity.KEY_HTML_URL));
            assertTrue(dialog.isShowing());
            assertEquals(View.VISIBLE, activity.findViewById(R.id.source_panel).getVisibility());
        }
    }

    @Test public void discoveredHtmlCanBeSelectedAndSavedWithoutTyping() {
        android.content.SharedPreferences prefs = RuntimeEnvironment.getApplication()
                .getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        prefs.edit().clear().putString(MainActivity.KEY_DISPLAY_MODE, "html").commit();
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            HtmlSource source = new HtmlSource("http://192.168.2.53:5050/output/saida-html-1", "Palco");
            assertNotNull(FakeHtmlDiscovery.listener);
            FakeHtmlDiscovery.listener.onSource(source);
            ListView list = activity.findViewById(R.id.source_list);
            assertEquals(1, list.getAdapter().getCount());
            list.performItemClick(null, 0, 0);
            assertEquals(source.url, prefs.getString(MainActivity.KEY_HTML_URL, ""));
            assertEquals(View.GONE, activity.findViewById(R.id.source_panel).getVisibility());
        }
    }

    @Test public void rescanKeepsSourcesAndIgnoresCallbacksAfterCancellationOrStop() {
        android.content.SharedPreferences prefs = RuntimeEnvironment.getApplication()
                .getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        prefs.edit().clear().putString(MainActivity.KEY_DISPLAY_MODE, "html").commit();
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            ListView list = activity.findViewById(R.id.source_list);
            LanHtmlDiscovery.Listener old = FakeHtmlDiscovery.listener;
            old.onSource(new HtmlSource("http://192.168.2.53:5050/output/saida-html-1", "Primeiro"));
            activity.findViewById(R.id.scan_button).performClick();
            assertEquals(1, list.getAdapter().getCount());
            old.onSource(new HtmlSource("http://192.168.2.54:5050/output/saida-html-1", "Obsoleto"));
            assertEquals(1, list.getAdapter().getCount());
            LanHtmlDiscovery.Listener current = FakeHtmlDiscovery.listener;
            current.onSource(new HtmlSource("http://192.168.2.55:5050/output/saida-html-1", "Segundo"));
            assertEquals(2, list.getAdapter().getCount());
            controller.pause().stop();
            current.onSource(new HtmlSource("http://192.168.2.56:5050/output/saida-html-1", "Tardio"));
            assertEquals(2, list.getAdapter().getCount());
        }
    }

    @Test public void repeatsSearchWhilePanelIsOpenAndCancelsRepeatWhenClosed() {
        RuntimeEnvironment.getApplication().getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE)
                .edit().clear().putString(MainActivity.KEY_DISPLAY_MODE, "html").commit();
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            int initial = FakeHtmlDiscovery.starts;
            FakeHtmlDiscovery.listener.onFinished();
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(30));
            assertEquals(initial + 1, FakeHtmlDiscovery.starts);
            FakeHtmlDiscovery.listener.onFinished();
            activity.findViewById(R.id.close_panel_button).performClick();
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(30));
            assertEquals(initial + 1, FakeHtmlDiscovery.starts);
        }
    }

    @Test public void restoresHtmlAddressAndRemoteCanOpenPanelOverWebContent() {
        android.content.SharedPreferences prefs = RuntimeEnvironment.getApplication()
                .getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        prefs.edit().clear().putString(MainActivity.KEY_DISPLAY_MODE, "html")
                .putString(MainActivity.KEY_HTML_URL, "http://pc.local:5050/output/palco").commit();
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            assertEquals(View.GONE, activity.findViewById(R.id.source_panel).getVisibility());
            assertEquals(View.VISIBLE, activity.findViewById(R.id.html_container).getVisibility());
            activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
            assertEquals(View.VISIBLE, activity.findViewById(R.id.menu_button).getVisibility());
            activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
            assertEquals(View.VISIBLE, activity.findViewById(R.id.source_panel).getVisibility());
        }
    }

    @Test public void coldStartAndRecreationRestoreNdiWithoutEnumeratingOtherSources() throws Exception {
        android.content.SharedPreferences prefs = RuntimeEnvironment.getApplication()
                .getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        prefs.edit().clear().putString(MainActivity.KEY_DISPLAY_MODE, "ndi")
                .putString(MainActivity.KEY_LAST_SOURCE, "PC (Retorno salvo)").commit();
        NoNativeReceiver.available = true;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            drainWorkers();
            assertEquals(java.util.Collections.singletonList("PC (Retorno salvo)"), NoNativeReceiver.connected);
            assertEquals(View.GONE, controller.get().findViewById(R.id.source_panel).getVisibility());
            assertEquals(0, NoNativeReceiver.finds.get());
            controller.recreate();
            drainWorkers();
            assertEquals(2, NoNativeReceiver.connected.size());
            assertEquals("PC (Retorno salvo)", NoNativeReceiver.connected.get(1));
            assertEquals(View.GONE, controller.get().findViewById(R.id.source_panel).getVisibility());
            controller.get().onBackPressed();
            assertEquals(View.VISIBLE, controller.get().findViewById(R.id.source_panel).getVisibility());
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMinutes(1));
            drainWorkers();
            assertEquals(0, NoNativeReceiver.finds.get());
            assertEquals(0, FakeHtmlDiscovery.starts);
            assertEquals("PC (Retorno salvo)", prefs.getString(MainActivity.KEY_LAST_SOURCE, ""));
        }
    }

    @Test public void backgroundStopsNdiAndForegroundReconnectsOnlyToSavedSource() throws Exception {
        RuntimeEnvironment.getApplication().getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE)
                .edit().clear().putString(MainActivity.KEY_DISPLAY_MODE, "ndi")
                .putString(MainActivity.KEY_LAST_SOURCE, "PC (Palco)").commit();
        NoNativeReceiver.available = true;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            drainWorkers();
            assertEquals(1, NoNativeReceiver.connected.size());
            controller.pause().stop();
            drainWorkers();
            assertEquals(1, NoNativeReceiver.disconnects.get());
            controller.start().resume().visible();
            drainWorkers();
            assertEquals(2, NoNativeReceiver.connected.size());
            assertEquals("PC (Palco)", NoNativeReceiver.connected.get(1));
            assertEquals(0, NoNativeReceiver.finds.get());
        }
    }

    @Test public void selectingNdiReleasesFinderAndMenuDoesNotRestartIt() throws Exception {
        RuntimeEnvironment.getApplication().getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE)
                .edit().clear().commit();
        NoNativeReceiver.available = true;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            drainWorkers();
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertTrue(NoNativeReceiver.finds.get() > 0);
            ListView list = controller.get().findViewById(R.id.source_list);
            assertEquals(1, list.getAdapter().getCount());
            list.performItemClick(null, 0, 0);
            drainWorkers();
            int searches = NoNativeReceiver.finds.get();
            assertTrue(NoNativeReceiver.releases.get() > 0);
            controller.get().onBackPressed();
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMinutes(1));
            drainWorkers();
            assertEquals(searches, NoNativeReceiver.finds.get());
            assertEquals(controller.get().getString(R.string.discovery_idle),
                    ((TextView) controller.get().findViewById(R.id.source_status_text)).getText().toString());
            controller.get().findViewById(R.id.scan_button).performClick();
            drainWorkers();
            assertTrue(NoNativeReceiver.finds.get() > searches);
        }
    }

    @Test public void savedHtmlRestoresAndRetriesSameUrlWithoutSearchingEvenWithMenuOpen() throws Exception {
        String url = "http://pc.local:5050/output/palco?view=stage";
        android.content.SharedPreferences prefs = RuntimeEnvironment.getApplication()
                .getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        prefs.edit().clear().putString(MainActivity.KEY_DISPLAY_MODE, "html")
                .putString(MainActivity.KEY_HTML_URL, url).commit();
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            android.widget.FrameLayout frame = controller.get().findViewById(R.id.html_container);
            android.webkit.WebView web = (android.webkit.WebView) ((HtmlStageViewport) frame.getChildAt(0)).getChildAt(0);
            assertEquals(url, org.robolectric.Shadows.shadowOf(web).getLastLoadedUrl());
            controller.get().onBackPressed();
            assertEquals(1, ((ListView) controller.get().findViewById(R.id.source_list)).getAdapter().getCount());
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(75));
            drainWorkers();
            assertEquals(url, org.robolectric.Shadows.shadowOf(web).getLastLoadedUrl());
            assertEquals(0, FakeHtmlDiscovery.starts);
            assertEquals(0, NoNativeReceiver.finds.get());
            assertEquals(url, prefs.getString(MainActivity.KEY_HTML_URL, ""));
            controller.recreate();
            assertEquals(View.GONE, controller.get().findViewById(R.id.source_panel).getVisibility());
            assertEquals(0, FakeHtmlDiscovery.starts);
            controller.get().onBackPressed();
            controller.get().findViewById(R.id.scan_button).performClick();
            assertEquals(1, FakeHtmlDiscovery.starts);
        }
    }

    @Test public void browsingUnconfiguredModeDoesNotReplaceLastSelectedConfiguration() throws Exception {
        android.content.SharedPreferences prefs = RuntimeEnvironment.getApplication()
                .getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        prefs.edit().clear().putString(MainActivity.KEY_DISPLAY_MODE, "ndi")
                .putString(MainActivity.KEY_LAST_SOURCE, "PC (Retorno salvo)").commit();
        NoNativeReceiver.available = true;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            controller.get().onBackPressed();
            controller.get().findViewById(R.id.source_mode_button).performClick();
            ShadowAlertDialog.getLatestAlertDialog().getListView().performItemClick(null, 1, 1);
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertEquals("ndi", prefs.getString(MainActivity.KEY_DISPLAY_MODE, ""));
            assertEquals(1, FakeHtmlDiscovery.starts);
            controller.recreate();
            drainWorkers();
            assertEquals(View.GONE, controller.get().findViewById(R.id.html_container).getVisibility());
            assertEquals(View.GONE, controller.get().findViewById(R.id.source_panel).getVisibility());
            assertEquals("PC (Retorno salvo)", prefs.getString(MainActivity.KEY_LAST_SOURCE, ""));
            assertEquals(0, NoNativeReceiver.finds.get());
        }
    }

    @Test public void newHtmlSelectionPersistsModeAndExactUrlForNextStart() throws Exception {
        android.content.SharedPreferences prefs = RuntimeEnvironment.getApplication()
                .getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        prefs.edit().clear().putString(MainActivity.KEY_DISPLAY_MODE, "ndi")
                .putString(MainActivity.KEY_LAST_SOURCE, "PC (Palco)").commit();
        NoNativeReceiver.available = true;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            controller.get().onBackPressed();
            controller.get().findViewById(R.id.source_mode_button).performClick();
            ShadowAlertDialog.getLatestAlertDialog().getListView().performItemClick(null, 1, 1);
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            String url = "http://192.168.2.53:5050/output/palco?view=stage#return";
            FakeHtmlDiscovery.listener.onSource(new HtmlSource(url, "Palco"));
            ((ListView) controller.get().findViewById(R.id.source_list)).performItemClick(null, 0, 0);
            assertEquals("html", prefs.getString(MainActivity.KEY_DISPLAY_MODE, ""));
            assertEquals(url, prefs.getString(MainActivity.KEY_HTML_URL, ""));
            int scans = FakeHtmlDiscovery.starts;
            controller.recreate();
            assertEquals(View.GONE, controller.get().findViewById(R.id.source_panel).getVisibility());
            android.widget.FrameLayout frame = controller.get().findViewById(R.id.html_container);
            assertEquals(url, org.robolectric.Shadows.shadowOf((android.webkit.WebView) ((HtmlStageViewport) frame.getChildAt(0)).getChildAt(0)).getLastLoadedUrl());
            assertEquals(scans, FakeHtmlDiscovery.starts);
        }
    }

    @Test public void legacyIncompleteModeFallsBackToExistingSavedSource() {
        android.content.SharedPreferences prefs = RuntimeEnvironment.getApplication()
                .getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        NoNativeReceiver.available = true;
        prefs.edit().clear().putString(MainActivity.KEY_DISPLAY_MODE, "html")
                .putString(MainActivity.KEY_LAST_SOURCE, "PC (Palco)").commit();
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            assertEquals(View.GONE, controller.get().findViewById(R.id.html_container).getVisibility());
            assertEquals(View.GONE, controller.get().findViewById(R.id.source_panel).getVisibility());
        }
        prefs.edit().clear().putString(MainActivity.KEY_DISPLAY_MODE, "ndi")
                .putString(MainActivity.KEY_HTML_URL, "http://192.168.2.53:5050/output/palco").commit();
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            assertEquals(View.VISIBLE, controller.get().findViewById(R.id.html_container).getVisibility());
            assertEquals(View.GONE, controller.get().findViewById(R.id.source_panel).getVisibility());
        }
    }
}
