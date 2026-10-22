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
    @Before public void resetDiscovery() { FakeHtmlDiscovery.listener = null; FakeHtmlDiscovery.starts = 0; }
    @Implements(value = NativeBridge.class, isInAndroidSdk = false)
    public static class NoNativeReceiver {
        @Implementation protected static void __staticInitializer__() { }
        @Implementation protected static void setCallbackTarget(Object target) { }
        @Implementation protected static boolean isNdiAvailable() { return false; }
        @Implementation protected static void setSurface(Surface surface) { }
        @Implementation protected static void disconnect() { }
        @Implementation protected static void releaseDiscovery() { }
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
            assertEquals("ndi", prefs.getString(MainActivity.KEY_DISPLAY_MODE, ""));
            assertEquals(View.GONE, activity.findViewById(R.id.html_container).getVisibility());
            assertEquals(View.VISIBLE, activity.findViewById(R.id.source_list).getVisibility());
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
}
