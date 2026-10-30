package br.com.polegar.ndimonitor;

import android.app.Application;
import android.content.Context;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25, application = Application.class)
public class SupportRuntimeTest {
    private Context context;
    private final List<SupportRuntime> runtimes = new ArrayList<>();
    @Before public void clear() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences(SupportStore.PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit();
    }
    @After public void close() { for (SupportRuntime runtime : runtimes) runtime.closeForTests(); }
    private SupportRuntime runtime(SupportStore store, SupportTestSupport.Factory factory,
                                   SupportTestSupport.Scheduler clock, SupportExitHistory.Reader reader) {
        SupportRuntime runtime = new SupportRuntime(store, "1.4.12", SupportTestSupport.probe(clock),
                new SupportTransport(factory), clock, reader, false);
        runtimes.add(runtime);
        return runtime;
    }
    private void firstTick(SupportTestSupport.Scheduler clock) { assertTrue(clock.runNext()); assertTrue(clock.runNext()); }

    @Test public void noBackgroundWorkOrCrashUploadWithoutBothEndpointAndOptIn() {
        for (String endpoint : new String[]{"", SupportTestSupport.ENDPOINT}) {
            SupportStore store = new SupportStore(context, endpoint);
            SupportTestSupport.Factory factory = new SupportTestSupport.Factory();
            SupportTestSupport.Scheduler clock = new SupportTestSupport.Scheduler();
            SupportRuntime runtime = runtime(store, factory, clock, () -> null);
            runtime.onForeground();
            runtime.recordJavaCrash(new IllegalStateException("private"));
            assertFalse(clock.runNext());
            assertTrue(factory.connections.isEmpty());
            assertTrue(store.pending().isEmpty());
        }
    }

    @Test public void heartbeatRunsOnceAMinuteOnlyWhileAppIsForeground() throws Exception {
        SupportStore store = new SupportStore(context, SupportTestSupport.ENDPOINT);
        store.setEnabled(true, 0);
        SupportTestSupport.Factory factory = new SupportTestSupport.Factory();
        SupportTestSupport.Scheduler clock = new SupportTestSupport.Scheduler();
        SupportRuntime runtime = runtime(store, factory, clock, () -> null);
        runtime.onForeground();
        firstTick(clock);
        assertEquals(1, factory.connections.size());
        JSONObject body = new JSONObject(factory.connections.get(0).body.toString("UTF-8"));
        assertEquals("heartbeat", body.getString("kind"));
        assertEquals(2.5d, body.getJSONObject("metrics").getDouble("cpu_percent"), .001);
        assertEquals(SupportRuntime.INTERVAL_MS, clock.nextDelayMs());
        runtime.onBackground();
        assertFalse(clock.runNext());
        assertEquals(1, factory.connections.size());
    }

    @Test public void failedServerUsesBoundedBackoffInsteadOfContinuousRequests() {
        SupportStore store = new SupportStore(context, SupportTestSupport.ENDPOINT);
        store.setEnabled(true, 0);
        SupportTestSupport.Factory factory = new SupportTestSupport.Factory(); factory.status = 503;
        SupportTestSupport.Scheduler clock = new SupportTestSupport.Scheduler();
        runtime(store, factory, clock, () -> null).onForeground();
        firstTick(clock);
        assertEquals(120000, clock.nextDelayMs());
        clock.runNext(); assertEquals(240000, clock.nextDelayMs());
        clock.runNext(); assertEquals(480000, clock.nextDelayMs());
        clock.runNext(); assertEquals(SupportRuntime.MAX_RETRY_MS, clock.nextDelayMs());
        assertEquals(4, factory.connections.size());
    }

    @Test public void confirmedNativeFailureIsReportedOnceAcrossRestart() throws Exception {
        SupportStore store = new SupportStore(context, SupportTestSupport.ENDPOINT);
        store.setEnabled(true, 0); store.markForeground();
        SupportTestSupport.Factory factory = new SupportTestSupport.Factory();
        SupportTestSupport.Scheduler clock = new SupportTestSupport.Scheduler();
        SupportRuntime runtime = runtime(store, factory, clock,
                () -> new SupportExitHistory.Exit(10000, "native_crash", false));
        runtime.onForeground(); firstTick(clock);
        assertEquals("native_crash", new JSONObject(factory.connections.get(0).body.toString("UTF-8")).getString("kind"));
        assertEquals(2, factory.connections.size());
        assertTrue(store.pending().isEmpty());
        runtime.onBackground();
        SupportTestSupport.Scheduler nextClock = new SupportTestSupport.Scheduler();
        SupportRuntime next = runtime(store, factory, nextClock,
                () -> new SupportExitHistory.Exit(10000, "native_crash", false));
        next.onForeground(); firstTick(nextClock);
        assertEquals(3, factory.connections.size());
        assertEquals("heartbeat", new JSONObject(factory.connections.get(2).body.toString("UTF-8")).getString("kind"));
    }

    @Test public void oldAndroidInterruptionIsExplicitlyUnknownRatherThanClaimedNativeCrash() throws Exception {
        SupportStore store = new SupportStore(context, SupportTestSupport.ENDPOINT);
        store.setEnabled(true, 0); store.markForeground();
        SupportTestSupport.Factory factory = new SupportTestSupport.Factory();
        SupportTestSupport.Scheduler clock = new SupportTestSupport.Scheduler();
        runtime(store, factory, clock, () -> null).onForeground(); firstTick(clock);
        JSONObject event = new JSONObject(factory.connections.get(0).body.toString("UTF-8"));
        assertEquals("session_interrupted", event.getString("kind"));
        assertEquals("incomplete_session", event.getJSONObject("failure").getString("source"));
    }

    @Test public void consentDoesNotReportHistoricalFailuresFromBeforeItWasEnabled() throws Exception {
        SupportStore store = new SupportStore(context, SupportTestSupport.ENDPOINT);
        store.setEnabled(true, 20000);
        SupportTestSupport.Factory factory = new SupportTestSupport.Factory();
        SupportTestSupport.Scheduler clock = new SupportTestSupport.Scheduler();
        runtime(store, factory, clock, () -> new SupportExitHistory.Exit(10000, "native_crash", false)).onForeground();
        firstTick(clock);
        assertEquals(1, factory.connections.size());
        assertEquals("heartbeat", new JSONObject(factory.connections.get(0).body.toString("UTF-8")).getString("kind"));
    }

    @Test public void explicitManualSendWorksWithoutEnablingAutomaticCollection() throws Exception {
        SupportStore store = new SupportStore(context, SupportTestSupport.ENDPOINT);
        SupportTestSupport.Factory factory = new SupportTestSupport.Factory();
        SupportTestSupport.Scheduler clock = new SupportTestSupport.Scheduler();
        SupportRuntime runtime = runtime(store, factory, clock, () -> null);
        runtime.onForeground();
        boolean[] accepted = {false};
        runtime.sendManual(runtime.manualReport(new SupportMetrics.Sample(1000L, 1000L, 1d, 1000L, 10L), "audio"),
                success -> accepted[0] = success);
        assertTrue(clock.runNext());
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertTrue(accepted[0]);
        assertFalse(store.enabled());
        JSONObject event = new JSONObject(factory.connections.get(0).body.toString("UTF-8"));
        assertEquals("manual_report", event.getString("kind"));
        assertEquals("audio", event.getString("category"));
        assertFalse(clock.runNext());
    }

    @Test public void anExplicitUserStopIsNotClassifiedAsAFailure() throws Exception {
        SupportStore store = new SupportStore(context, SupportTestSupport.ENDPOINT);
        store.setEnabled(true, 0); store.markForeground();
        SupportTestSupport.Factory factory = new SupportTestSupport.Factory();
        SupportTestSupport.Scheduler clock = new SupportTestSupport.Scheduler();
        runtime(store, factory, clock, () -> new SupportExitHistory.Exit(10000, "", true)).onForeground();
        firstTick(clock);
        assertEquals(1, factory.connections.size());
        assertEquals("heartbeat", new JSONObject(factory.connections.get(0).body.toString("UTF-8")).getString("kind"));
    }

    @Test public void revocationDuringSendLeavesNoQueuedDataOrNewIdentifier() {
        SupportStore store = new SupportStore(context, SupportTestSupport.ENDPOINT);
        store.setEnabled(true, 0);
        SupportTestSupport.Factory factory = new SupportTestSupport.Factory();
        SupportTestSupport.Scheduler clock = new SupportTestSupport.Scheduler();
        SupportRuntime runtime = runtime(store, factory, clock, () -> null);
        factory.onOutput = () -> runtime.setAutomatic(false);
        runtime.onForeground(); firstTick(clock);
        assertEquals(0, factory.connections.get(0).body.size());
        assertFalse(clock.runNext());
        assertTrue(context.getSharedPreferences(SupportStore.PREFERENCES, Context.MODE_PRIVATE).getAll().isEmpty());
    }

    @Test public void javaHandlerReportIsRetriedAfterRestartWithoutDuplicateSystemReport() throws Exception {
        SupportStore store = new SupportStore(context, SupportTestSupport.ENDPOINT);
        store.setEnabled(true, 0);
        SupportTestSupport.Factory factory = new SupportTestSupport.Factory();
        SupportTestSupport.Scheduler oldClock = new SupportTestSupport.Scheduler();
        SupportRuntime previous = runtime(store, factory, oldClock, () -> null);
        previous.onForeground();
        previous.recordJavaCrash(new IllegalStateException("spk_SECRET http://private.example"));
        assertEquals(1, store.pending().size());
        SupportTestSupport.Scheduler clock = new SupportTestSupport.Scheduler();
        SupportRuntime next = runtime(store, factory, clock,
                () -> new SupportExitHistory.Exit(10000, "java_crash", false));
        previous.closeForTests();
        next.onForeground(); firstTick(clock);
        assertEquals(2, factory.connections.size());
        String body = factory.connections.get(0).body.toString("UTF-8");
        assertEquals("java_crash", new JSONObject(body).getString("kind"));
        assertFalse(body.contains("spk_SECRET"));
        assertTrue(store.pending().isEmpty());
    }
}
