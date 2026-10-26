package br.com.polegar.ndimonitor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={23,25,28,34,35},shadows={MainActivityModeTest.NoNativeReceiver.class,MainActivityModeTest.FakeHtmlDiscovery.class})
public class SpresenterCatalogUiTest {
    private static final String ROOT="http://192.168.2.53:5050";
    private SharedPreferences prefs;
    @Before public void prepare() {
        MainActivityModeTest.NoNativeReceiver.available=false;
        MainActivityModeTest.FakeHtmlDiscovery.listener=null;
        MainActivityModeTest.FakeHtmlDiscovery.starts=0;
        prefs=RuntimeEnvironment.getApplication().getSharedPreferences(MainActivity.PREFERENCES,Context.MODE_PRIVATE);
        prefs.edit().clear().putString(MainActivity.KEY_DISPLAY_MODE,"html").commit();
    }

    @Test public void catalogNamesUpdateWithoutAutoSelectingAndLateCallbacksAreIgnored() {
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity=controller.get();ListView list=activity.findViewById(R.id.source_list);
            LanHtmlDiscovery.Listener first=MainActivityModeTest.FakeHtmlDiscovery.listener;
            first.onCatalog(ROOT,List.of(new HtmlSource(ROOT+"/output/madureira","Público",ROOT),
                    new HtmlSource(ROOT+"/output/canvas-2","Músicos",ROOT)));
            first.onFinished();assertEquals(2,list.getAdapter().getCount());
            assertTrue(list.getAdapter().getItem(1).toString().startsWith("Músicos\n"));
            assertFalse(prefs.contains(MainActivity.KEY_HTML_URL));
            activity.findViewById(R.id.scan_button).performClick();
            LanHtmlDiscovery.Listener second=MainActivityModeTest.FakeHtmlDiscovery.listener;
            first.onCatalog(ROOT,List.of());assertEquals(2,list.getAdapter().getCount());
            second.onCatalog(ROOT,List.of(new HtmlSource(ROOT+"/output/canvas-2","Banda",ROOT)));
            second.onFinished();assertEquals(1,list.getAdapter().getCount());
            assertTrue(list.getAdapter().getItem(0).toString().startsWith("Banda\n"));
            second.onCatalogError(R.string.spresenter_api_unauthorized);second.onFinished();
            assertEquals(activity.getString(R.string.spresenter_api_unauthorized),
                    ((TextView)activity.findViewById(R.id.source_status_text)).getText().toString());
            list.performItemClick(null,0,0);
            assertEquals(ROOT+"/output/canvas-2",prefs.getString(MainActivity.KEY_HTML_URL,""));
            assertEquals(View.GONE,activity.findViewById(R.id.source_panel).getVisibility());
            second.onCatalog(ROOT,List.of());assertEquals(1,list.getAdapter().getCount());
        }
    }

    @Test public void settingsFormOpensOnLegacyAndroidAndRequiresKeyWithoutChangingCurrentSource() {
        String saved=ROOT+"/output/retorno";
        prefs.edit().putString(MainActivity.KEY_HTML_URL,saved).commit();
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity=controller.get();activity.findViewById(R.id.monitor_settings_button).performClick();
            AlertDialog settings=ShadowAlertDialog.getLatestAlertDialog();
            settings.findViewById(R.id.spresenter_api_button).performClick();
            AlertDialog api=ShadowAlertDialog.getLatestAlertDialog();
            assertEquals(ROOT,((EditText)api.findViewById(R.id.spresenter_api_address)).getText().toString());
            api.findViewById(R.id.spresenter_api_save).performClick();
            assertNotNull(((EditText)api.findViewById(R.id.spresenter_api_token)).getError());
            assertTrue(api.isShowing());assertEquals(saved,prefs.getString(MainActivity.KEY_HTML_URL,""));
            assertFalse(prefs.contains(SpresenterApiSettings.SECRET));
        }
    }

    @Test public void savingApiConnectionStartsRequestedSearchAndRemovalKeepsPlaybackSelection() {
        String saved=ROOT+"/output/retorno";
        prefs.edit().putString(MainActivity.KEY_HTML_URL,saved).commit();
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity=controller.get();activity.findViewById(R.id.monitor_settings_button).performClick();
            ShadowAlertDialog.getLatestAlertDialog().findViewById(R.id.spresenter_api_button).performClick();
            AlertDialog form=ShadowAlertDialog.getLatestAlertDialog();
            ((EditText)form.findViewById(R.id.spresenter_api_token)).setText("spk_example_read_only_token");
            form.findViewById(R.id.spresenter_api_save).performClick();
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertFalse(form.isShowing());assertEquals(ROOT,prefs.getString(SpresenterApiSettings.ORIGIN,""));
            assertFalse(prefs.getAll().toString().contains("spk_example_read_only_token"));
            assertEquals(saved,prefs.getString(MainActivity.KEY_HTML_URL,""));
            assertEquals(1,MainActivityModeTest.FakeHtmlDiscovery.starts);
            assertEquals(View.VISIBLE,activity.findViewById(R.id.source_panel).getVisibility());
            activity.findViewById(R.id.monitor_settings_button).performClick();
            ShadowAlertDialog.getLatestAlertDialog().findViewById(R.id.spresenter_api_button).performClick();
            form=ShadowAlertDialog.getLatestAlertDialog();
            form.findViewById(R.id.spresenter_api_remove).performClick();
            assertFalse(prefs.contains(SpresenterApiSettings.ORIGIN));
            assertEquals(saved,prefs.getString(MainActivity.KEY_HTML_URL,""));
        }
    }
}
