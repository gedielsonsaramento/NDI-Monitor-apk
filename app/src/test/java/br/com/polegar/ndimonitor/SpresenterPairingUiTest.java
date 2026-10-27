package br.com.polegar.ndimonitor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={23,25,28,34,35}, shadows={MainActivityModeTest.NoNativeReceiver.class,
        MainActivityModeTest.FakeHtmlDiscovery.class, SpresenterPairingUiTest.SetupNetwork.class})
public class SpresenterPairingUiTest {
    private static final String KEY = "spk_synthetic_ui_key_123456";
    private static final String ROOT = "http://192.168.2.53:5050";
    private static final String SAVED = ROOT + "/output/retorno";
    private SharedPreferences prefs;

    @Implements(SpresenterSetupNetwork.class)
    public static class SetupNetwork {
        static String value;
        @Implementation protected static String address(Context context) { return value; }
    }

    @Before public void prepare() {
        SetupNetwork.value = "127.0.0.1";
        MainActivityModeTest.NoNativeReceiver.available = false;
        MainActivityModeTest.FakeHtmlDiscovery.listener = null;
        MainActivityModeTest.FakeHtmlDiscovery.starts = 0;
        prefs = RuntimeEnvironment.getApplication().getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        prefs.edit().clear().putString(MainActivity.KEY_DISPLAY_MODE, "html")
                .putString(MainActivity.KEY_HTML_URL, SAVED).commit();
    }

    @Test public void wholeEditorLinkImportsWithoutTypingIndividualKeyAndKeepsPlaybackSelection() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            AlertDialog form = open(controller.get());
            assertEquals(View.GONE, form.findViewById(R.id.spresenter_manual_form).getVisibility());
            assertTrue(form.findViewById(R.id.spresenter_pairing_button).isFocused());
            EditText link = form.findViewById(R.id.spresenter_editor_link);
            assertFalse(link.isSaveEnabled());
            link.setText(ROOT + "/warp/?token=" + KEY);
            form.findViewById(R.id.spresenter_link_save).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertFalse(form.isShowing()); assertEquals("", link.getText().toString());
            assertEquals(ROOT, prefs.getString(SpresenterApiSettings.ORIGIN, ""));
            assertFalse(prefs.getAll().toString().contains(KEY));
            assertEquals(SAVED, prefs.getString(MainActivity.KEY_HTML_URL, ""));
            assertEquals(1, MainActivityModeTest.FakeHtmlDiscovery.starts);
        }
    }

    @Test public void invalidFullLinkShowsLocalErrorWithoutSaving() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            AlertDialog form = open(controller.get());
            EditText link = form.findViewById(R.id.spresenter_editor_link);
            link.setText(ROOT + "/output/retorno?token=" + KEY);
            form.findViewById(R.id.spresenter_link_save).performClick();
            assertNotNull(link.getError()); assertFalse(link.getError().toString().contains(KEY));
            assertTrue(form.isShowing()); assertFalse(prefs.contains(SpresenterApiSettings.ORIGIN));
            assertEquals(SAVED, prefs.getString(MainActivity.KEY_HTML_URL, ""));
        }
    }

    @Test public void receiveOnActivePairingScreenSavesAndStartsSourceSearch() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            open(activity).findViewById(R.id.spresenter_pairing_button).performClick();
            AlertDialog pair = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(((TextView)pair.findViewById(R.id.spresenter_pairing_address)).getText().toString()
                    .startsWith("http://127.0.0.1:"));
            assertTrue(((TextView)pair.findViewById(R.id.spresenter_pairing_code)).getText().toString().matches("[0-9]{6}"));
            SpresenterPairingServer server = ReflectionHelpers.getField(activity, "spresenterPairingServer");
            SpresenterPairingServer.Listener listener = ReflectionHelpers.getField(server, "listener");
            listener.onConnection(new SpresenterConnection(ROOT, KEY));
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertFalse(pair.isShowing());
            assertEquals(ROOT, prefs.getString(SpresenterApiSettings.ORIGIN, ""));
            assertFalse(prefs.getAll().toString().contains(KEY));
            assertEquals(SAVED, prefs.getString(MainActivity.KEY_HTML_URL, ""));
            assertEquals(1, MainActivityModeTest.FakeHtmlDiscovery.starts);
            assertNull(ReflectionHelpers.getField(activity, "spresenterPairingServer"));
        }
    }

    @Test public void dismissAndReopenIgnorePreviousSessionCallbacks() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get(); AlertDialog form = open(activity);
            form.findViewById(R.id.spresenter_pairing_button).performClick();
            AlertDialog pair = ShadowAlertDialog.getLatestAlertDialog();
            SpresenterPairingServer server = ReflectionHelpers.getField(activity, "spresenterPairingServer");
            SpresenterPairingServer.Listener stale = ReflectionHelpers.getField(server, "listener");
            pair.dismiss();
            form.findViewById(R.id.spresenter_pairing_button).performClick();
            stale.onConnection(new SpresenterConnection(ROOT, KEY)); stale.onEnded();
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertFalse(prefs.contains(SpresenterApiSettings.ORIGIN));
            assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());
        }
    }

    @Test public void backgroundingClosesPairingAndRejectsQueuedSubmissions() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            open(activity).findViewById(R.id.spresenter_pairing_button).performClick();
            AlertDialog pair = ShadowAlertDialog.getLatestAlertDialog();
            SpresenterPairingServer server = ReflectionHelpers.getField(activity, "spresenterPairingServer");
            SpresenterPairingServer.Listener listener = ReflectionHelpers.getField(server, "listener");
            listener.onConnection(new SpresenterConnection(ROOT, KEY));
            controller.pause().stop(); Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertFalse(pair.isShowing()); assertFalse(prefs.contains(SpresenterApiSettings.ORIGIN));
            assertNull(ReflectionHelpers.getField(activity, "spresenterPairingServer"));
        }
    }

    @Test public void noLanExplainsConnectionAndKeepsExistingSettingsUntouched() {
        SetupNetwork.value = null;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get(); AlertDialog form = open(activity);
            form.findViewById(R.id.spresenter_pairing_button).performClick();
            assertSame(form, ShadowAlertDialog.getLatestAlertDialog());
            assertEquals(activity.getString(R.string.spresenter_pairing_no_network), ShadowToast.getTextOfLatestToast());
            assertNull(ReflectionHelpers.getField(activity, "spresenterPairingServer"));
            assertEquals(SAVED, prefs.getString(MainActivity.KEY_HTML_URL, ""));
        }
    }

    private static AlertDialog open(MainActivity activity) {
        activity.findViewById(R.id.monitor_settings_button).performClick();
        ShadowAlertDialog.getLatestAlertDialog().findViewById(R.id.spresenter_api_button).performClick();
        return ShadowAlertDialog.getLatestAlertDialog();
    }
}
