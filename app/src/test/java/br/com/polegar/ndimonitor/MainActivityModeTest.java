package br.com.polegar.ndimonitor;

import android.content.Context;
import android.view.Surface;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
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
@Config(sdk = 35, shadows = MainActivityModeTest.NoNativeReceiver.class)
public class MainActivityModeTest {
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
            assertEquals(View.VISIBLE, activity.findViewById(R.id.html_form).getVisibility());
            assertEquals(View.GONE, activity.findViewById(R.id.source_list).getVisibility());
            assertTrue(activity.findViewById(R.id.scan_button).isEnabled());
            ((EditText) activity.findViewById(R.id.html_url)).setText("192.168.2.53");
            activity.findViewById(R.id.open_html_button).performClick();
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
            EditText editor = activity.findViewById(R.id.html_url);
            editor.setText("file:///private/file");
            activity.findViewById(R.id.open_html_button).performClick();
            assertNotNull(editor.getError());
            assertFalse(prefs.contains(MainActivity.KEY_HTML_URL));
            assertEquals(View.VISIBLE, activity.findViewById(R.id.source_panel).getVisibility());
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
            assertEquals("http://pc.local:5050/output/palco",
                    ((EditText) activity.findViewById(R.id.html_url)).getText().toString());
            activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
            assertEquals(View.VISIBLE, activity.findViewById(R.id.menu_button).getVisibility());
            activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER));
            assertEquals(View.VISIBLE, activity.findViewById(R.id.source_panel).getVisibility());
        }
    }
}
