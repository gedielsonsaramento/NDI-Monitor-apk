package br.com.polegar.ndimonitor;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;

/** Foreground-only, optional support telemetry, independent of NDI and playback state. */
final class SupportRuntime {
    static final long INTERVAL_MS = 60000;
    static final long MAX_RETRY_MS = 900000;
    interface SampleCallback { void ready(SupportMetrics.Sample sample); }
    interface SendCallback { void finished(boolean accepted); }
    private final SupportStore store;
    private final String version;
    private final SupportMetrics.Probe probe;
    private final SupportMetrics.Sampler sampler;
    private final SupportTransport transport;
    private final ScheduledExecutorService worker;
    private final SupportExitHistory.Reader exitReader;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean foreground;
    private volatile long generation;
    private volatile SupportMetrics.Sample lastSample;
    private ScheduledFuture<?> next;
    private int failures;
    private boolean reviewPreviousExit;
    private final boolean previousInterrupted;
    private final boolean previousJavaRecorded;

    SupportRuntime(Context context, String endpoint, String version, boolean interrupted) {
        this(new SupportStore(context, endpoint), version, new SupportMetrics.AndroidProbe(),
                new SupportTransport(), Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "SRetornoSupport");
                    thread.setDaemon(true);
                    return thread;
                }), SupportExitHistory.reader(context), interrupted);
    }

    SupportRuntime(SupportStore store, String version, SupportMetrics.Probe probe,
                   SupportTransport transport, ScheduledExecutorService worker,
                   SupportExitHistory.Reader exitReader, boolean interrupted) {
        this.store = store;
        this.version = version;
        this.probe = probe;
        sampler = new SupportMetrics.Sampler(probe);
        this.transport = transport;
        this.worker = worker;
        this.exitReader = exitReader;
        boolean[] previous = store.takePreviousSession();
        previousInterrupted = previous[0] || interrupted;
        previousJavaRecorded = previous[1];
        reviewPreviousExit = store.enabled();
    }

    boolean configured() { return store.configured(); }
    boolean automaticEnabled() { return store.enabled(); }
    String version() { return version; }

    synchronized void onForeground() {
        if (foreground) return;
        foreground = true;
        if (store.enabled()) startAutomatic();
    }

    synchronized void onBackground() {
        foreground = false;
        cancel();
        store.markBackground();
    }

    synchronized void setAutomatic(boolean enabled) {
        cancel();
        reviewPreviousExit = false;
        store.setEnabled(enabled, System.currentTimeMillis());
        if (foreground && store.enabled()) startAutomatic();
    }

    private void startAutomatic() {
        store.markForeground();
        failures = 0;
        final long token = ++generation;
        worker.execute(() -> {
            if (!allowed(token)) return;
            try { lastSample = sampler.sample(); }
            catch (RuntimeException unavailable) { lastSample = null; }
        });
        next = worker.schedule(() -> tick(token), 5, TimeUnit.SECONDS);
    }

    private synchronized void cancel() {
        ++generation;
        if (next != null) next.cancel(true);
        next = null;
        transport.cancel();
    }

    private boolean allowed(long token) { return foreground && generation == token && store.enabled(); }

    private void tick(long token) {
        if (!allowed(token)) return;
        boolean success = false;
        try {
            if (reviewPreviousExit) {
                reviewPreviousExit = false;
                reviewPreviousExit();
            }
            lastSample = sampler.sample();
            List<JSONObject> pending = store.pending();
            success = true;
            if (!pending.isEmpty()) {
                JSONObject event = pending.get(0);
                success = SupportTransport.accepted(transport.post(store.endpoint, event, () -> allowed(token)));
                if (success) store.acknowledge(event.optString("event_id"));
            }
            if (success && allowed(token)) {
                JSONObject heartbeat = SupportPayload.create("heartbeat", store.automaticId(), version,
                        lastSample, null, null);
                success = SupportTransport.accepted(transport.post(store.endpoint, heartbeat, () -> allowed(token)));
            }
        } catch (RuntimeException | JSONException unavailable) {
            success = false;
        }
        synchronized (this) {
            if (!allowed(token)) return;
            failures = success ? 0 : Math.min(4, failures + 1);
            long delay = Math.min(MAX_RETRY_MS, INTERVAL_MS << failures);
            next = worker.schedule(() -> tick(token), delay, TimeUnit.MILLISECONDS);
        }
    }

    private void reviewPreviousExit() throws JSONException {
        SupportExitHistory.Exit exit = exitReader.latest();
        boolean fresh = exit != null && exit.timestampMs >= store.consentSinceMs()
                && exit.timestampMs > store.lastExitMs();
        boolean known = false;
        if (fresh) {
            store.markExitSeen(exit.timestampMs);
            known = exit.normal || !exit.kind.isEmpty();
            if (!exit.kind.isEmpty() && !(previousJavaRecorded && "java_crash".equals(exit.kind))) {
                JSONObject event = SupportPayload.create(exit.kind, store.automaticId(), version, null,
                        new JSONObject().put("source", "android_exit_reason"), null);
                store.enqueue(event, false);
            }
        }
        if (previousInterrupted && !previousJavaRecorded && !known) {
            store.enqueue(SupportPayload.create("session_interrupted", store.automaticId(), version, null,
                    new JSONObject().put("source", "incomplete_session"), null), false);
        }
    }

    void recordJavaCrash(Throwable error) {
        if (!store.enabled()) return;
        try {
            store.enqueue(SupportPayload.create("java_crash", store.automaticId(), version, lastSample,
                    SupportPayload.javaFailure(error), null), true);
        } catch (RuntimeException | JSONException unavailable) {
            // Android's original uncaught exception handler must still run.
        }
    }

    void snapshot(SampleCallback callback) {
        SupportMetrics.Sampler manual = new SupportMetrics.Sampler(probe);
        worker.execute(() -> {
            try { manual.sample(); } catch (RuntimeException unavailable) { }
            worker.schedule(() -> {
                SupportMetrics.Sample result;
                try { result = manual.sample(); }
                catch (RuntimeException unavailable) { result = new SupportMetrics.Sample(null, null, null, null, null); }
                SupportMetrics.Sample measured = result;
                main.post(() -> callback.ready(measured));
            }, 1, TimeUnit.SECONDS);
        });
    }

    JSONObject manualReport(SupportMetrics.Sample sample, String category) throws JSONException {
        return SupportPayload.create("manual_report", store.installationId(), version, sample, null, category);
    }

    void sendManual(JSONObject event, SendCallback callback) {
        final long token = generation;
        worker.execute(() -> {
            boolean success = SupportTransport.accepted(transport.post(store.endpoint, event,
                    () -> foreground && generation == token && store.configured()));
            main.post(() -> callback.finished(success));
        });
    }

    void closeForTests() {
        onBackground();
        worker.shutdownNow();
    }
}
