package br.com.polegar.ndimonitor;

import android.app.Activity;
import android.app.Application;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ClipboardManager;
import android.os.Looper;
import android.widget.CheckBox;
import android.widget.Spinner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 25, 34}, application = Application.class)
public class SupportUiTest {
    @Before public void reset() {
        RuntimeEnvironment.getApplication().getSharedPreferences(SupportStore.PREFERENCES, Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    @Test public void serverPendingStillAllowsUserToCopyALocalReportWithoutAutomaticCollection() {
        try (ActivityController<Activity> activity = Robolectric.buildActivity(Activity.class).setup()) {
            activity.get().setTheme(R.style.AppTheme);
            SupportStore store = new SupportStore(activity.get(), "");
            SupportTestSupport.Factory factory = new SupportTestSupport.Factory();
            SupportTestSupport.Scheduler clock = new SupportTestSupport.Scheduler();
            SupportRuntime runtime = new SupportRuntime(store, "1.4.12", SupportTestSupport.probe(clock),
                    new SupportTransport(factory), clock, () -> null, false);
            runtime.onForeground();
            SupportDialog ui = new SupportDialog(activity.get(), runtime);
            try {
                ui.show();
                AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
                CheckBox automatic = dialog.findViewById(R.id.support_auto_checkbox);
                assertFalse(automatic.isChecked());
                assertFalse(automatic.isEnabled());
                assertFalse(dialog.findViewById(R.id.support_send_button).isEnabled());
                assertFalse(dialog.findViewById(R.id.support_copy_button).isEnabled());
                clock.runNext(); clock.runNext();
                Shadows.shadowOf(Looper.getMainLooper()).idle();
                assertTrue(dialog.findViewById(R.id.support_copy_button).isEnabled());
                ((Spinner) dialog.findViewById(R.id.support_category)).setSelection(1);
                dialog.findViewById(R.id.support_copy_button).performClick();
                ClipboardManager clipboard = (ClipboardManager) activity.get().getSystemService(Context.CLIPBOARD_SERVICE);
                String report = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
                assertTrue(report.contains("1.4.12"));
                assertTrue(report.contains("Áudio"));
                assertTrue(report.contains("Memória do app"));
                assertFalse(report.contains("http"));
                assertTrue(factory.connections.isEmpty());
                assertTrue(activity.get().getSharedPreferences(SupportStore.PREFERENCES, Context.MODE_PRIVATE).getAll().isEmpty());
            } finally { ui.close(); runtime.closeForTests(); }
        }
    }

    @Test public void configuredDestinationRequiresExplicitOptInAndUserCanTurnItOff() {
        try (ActivityController<Activity> activity = Robolectric.buildActivity(Activity.class).setup()) {
            activity.get().setTheme(R.style.AppTheme);
            SupportStore store = new SupportStore(activity.get(), SupportTestSupport.ENDPOINT);
            SupportTestSupport.Factory factory = new SupportTestSupport.Factory();
            SupportTestSupport.Scheduler clock = new SupportTestSupport.Scheduler();
            SupportRuntime runtime = new SupportRuntime(store, "1.4.12", SupportTestSupport.probe(clock),
                    new SupportTransport(factory), clock, () -> null, false);
            runtime.onForeground();
            SupportDialog ui = new SupportDialog(activity.get(), runtime);
            try {
                ui.show();
                AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
                CheckBox automatic = dialog.findViewById(R.id.support_auto_checkbox);
                assertFalse(automatic.isChecked());
                automatic.setChecked(true);
                assertTrue(store.enabled());
                clock.runNext(); clock.runNext(); clock.runNext();
                Shadows.shadowOf(Looper.getMainLooper()).idle();
                assertTrue(dialog.findViewById(R.id.support_send_button).isEnabled());
                dialog.findViewById(R.id.support_send_button).performClick();
                clock.runNext();
                Shadows.shadowOf(Looper.getMainLooper()).idle();
                assertEquals(1, factory.connections.size());
                automatic.setChecked(false);
                assertFalse(store.enabled());
                assertTrue(store.pending().isEmpty());
                assertFalse(clock.runNext());
            } finally { ui.close(); runtime.closeForTests(); }
        }
    }
}
