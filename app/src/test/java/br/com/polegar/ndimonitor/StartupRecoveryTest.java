package br.com.polegar.ndimonitor;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 25}, application = Application.class,
        shadows = {TvBoxCompatibilityTest.UnloadableReceiver.class,
                MainActivityModeTest.FakeHtmlDiscovery.class})
public class StartupRecoveryTest {
    private Context context;

    @Before public void reset() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences(StartupJournal.PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit();
        ReflectionHelpers.setStaticField(StartupJournal.class, "instance", null);
        ReflectionHelpers.setStaticField(NdiReceiver.class, "loadFailure", null);
        ReflectionHelpers.setStaticField(NdiReceiver.class, "initializationAttempted", false);
        ReflectionHelpers.setStaticField(NdiReceiver.class, "available", false);
        ReflectionHelpers.setStaticField(NdiReceiver.class, "callbackTarget", null);
        ReflectionHelpers.setStaticField(NdiReceiver.class, "pendingSurface", null);
        TvBoxCompatibilityTest.UnloadableReceiver.calls = 0;
        MainActivityModeTest.FakeHtmlDiscovery.starts = 0;
    }

    @Test public void firstLegacyStartShowsHtmlWithoutLoadingNdiIncludingSurfaceAndCleanup() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            assertEquals(View.VISIBLE, activity.findViewById(R.id.html_actions).getVisibility());
            activity.surfaceCreated(((android.view.SurfaceView) activity.findViewById(R.id.video_surface)).getHolder());
            activity.surfaceDestroyed(((android.view.SurfaceView) activity.findViewById(R.id.video_surface)).getHolder());
            assertTrue(NdiReceiver.failureDescription().contains(android.os.Build.VERSION.SDK_INT == 23
                    ? "Android 6" : "ainda não inicializado"));
        }
        assertEquals(0, TvBoxCompatibilityTest.UnloadableReceiver.calls);
    }

    @Test public void interruptedNativeInitializationShowsReportBeforeMainAndCanChooseHtml() {
        StartupJournal original = new StartupJournal(context);
        original.beginStartup();
        original.beginOperation("Inicializando o receptor NDI");
        StartupJournal nextProcess = new StartupJournal(context);
        ReflectionHelpers.setStaticField(StartupJournal.class, "instance", nextProcess);
        assertTrue(nextProcess.needsRecovery());
        assertTrue(nextProcess.previousReport().contains("Inicializando o receptor NDI"));
        try (ActivityController<StartupActivity> controller = Robolectric.buildActivity(StartupActivity.class).setup()) {
            StartupActivity activity = controller.get();
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
            assertTrue(textIn(activity.getWindow().getDecorView(), "Última etapa"));
            buttonIn(activity.getWindow().getDecorView(), activity.getString(R.string.recovery_html)).performClick();
            Intent launch = Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals(MainActivity.class.getName(), launch.getComponent().getClassName());
            assertTrue(launch.getBooleanExtra(StartupActivity.EXTRA_HTML_ONLY, false));
        }
        assertEquals(0, TvBoxCompatibilityTest.UnloadableReceiver.calls);
    }

    @Test public void forcedHtmlPreservesSavedNdiSourceAndAvoidsNativeInitialization() {
        SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        preferences.edit().putString(MainActivity.KEY_DISPLAY_MODE, "ndi")
                .putString(MainActivity.KEY_LAST_SOURCE, "PC (Palco salvo)").commit();
        Intent intent = new Intent(context, MainActivity.class).putExtra(StartupActivity.EXTRA_HTML_ONLY, true);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class, intent).setup()) {
            assertEquals(View.VISIBLE, controller.get().findViewById(R.id.html_actions).getVisibility());
            assertEquals("ndi", preferences.getString(MainActivity.KEY_DISPLAY_MODE, ""));
            assertEquals("PC (Palco salvo)", preferences.getString(MainActivity.KEY_LAST_SOURCE, ""));
        }
        assertEquals(0, TvBoxCompatibilityTest.UnloadableReceiver.calls);
    }

    @Test public void completedStartupDoesNotShowRecoveryOnNextProcess() {
        StartupJournal journal = new StartupJournal(context);
        journal.beginStartup();
        long operation = journal.beginOperation("Inicializando o receptor NDI");
        journal.finishOperation(operation);
        journal.finishStartup();
        StartupJournal nextProcess = new StartupJournal(context);
        assertFalse(nextProcess.needsRecovery());
        ReflectionHelpers.setStaticField(StartupJournal.class, "instance", nextProcess);
        try (ActivityController<StartupActivity> controller = Robolectric.buildActivity(StartupActivity.class).setup()) {
            Intent launch = Shadows.shadowOf(controller.get()).getNextStartedActivity();
            assertNotNull(launch);
            assertFalse(launch.getBooleanExtra(StartupActivity.EXTRA_HTML_ONLY, false));
        }
    }

    @Test public void launcherReturnToExistingTaskDoesNotLeavePendingStartup() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            controller.pause();
            StartupJournal.get(context).beginStartup();
            controller.resume();
            assertFalse(new StartupJournal(context).needsRecovery());
        }
    }

    @Test public void olderCompletionCannotEraseNewerInterruptedOperation() {
        StartupJournal journal = new StartupJournal(context);
        long first = journal.beginOperation("Primeira operação");
        journal.beginOperation("Segunda operação");
        journal.finishOperation(first);
        StartupJournal nextProcess = new StartupJournal(context);
        assertTrue(nextProcess.needsRecovery());
        assertTrue(nextProcess.previousReport().contains("Segunda operação"));
    }

    @Test public void javaCrashKeepsRealCauseAndReportAfterAcknowledgment() {
        StartupJournal journal = new StartupJournal(context);
        journal.beginStartup();
        long operation = journal.beginOperation("Inicializando o receptor NDI");
        journal.finishOperation(operation);
        journal.recordCrash(Thread.currentThread(), new IllegalStateException("Falha sintética de teste"));
        StartupJournal nextProcess = new StartupJournal(context);
        assertTrue(nextProcess.needsRecovery());
        assertTrue(nextProcess.previousReport().contains("IllegalStateException: Falha sintética de teste"));
        assertTrue(nextProcess.previousReport().contains("Inicializando o receptor NDI"));
        nextProcess.acknowledge();
        StartupJournal afterAcknowledgment = new StartupJournal(context);
        assertFalse(afterAcknowledgment.needsRecovery());
        assertTrue(afterAcknowledgment.previousReport().contains("Falha sintética de teste"));
    }

    @Test public void explicitNdiChoiceTriesLibraryOnceAndThenHtmlRemainsUsable() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            activity.findViewById(R.id.source_mode_button).performClick();
            org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
                    .getListView().performItemClick(null, 0, 0);
            assertEquals(android.os.Build.VERSION.SDK_INT == 23 ? 0 : 1,
                    TvBoxCompatibilityTest.UnloadableReceiver.calls);
            activity.findViewById(R.id.source_mode_button).performClick();
            org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
                    .getListView().performItemClick(null, 1, 1);
            assertEquals(View.VISIBLE, activity.findViewById(R.id.html_actions).getVisibility());
        }
    }

    private static boolean textIn(View view, String needle) {
        if (view instanceof TextView && ((TextView) view).getText().toString().contains(needle)) return true;
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++) if (textIn(parent.getChildAt(i), needle)) return true;
        }
        return false;
    }

    private static Button buttonIn(View view, String text) {
        if (view instanceof Button && text.equals(((Button) view).getText().toString())) return (Button) view;
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++) {
                Button found = buttonIn(parent.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
