package br.com.polegar.ndimonitor;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** A finite, cancellable scan. A fixed number of workers pull targets without queuing sockets. */
final class HtmlDiscoverySession {
    static final int MAX_WORKERS = 12;
    interface Prober { HtmlSource probe(HtmlHttpProbe.Target target, Cancellation cancellation) throws Exception; }
    interface Listener {
        void onSource(HtmlSource source);
        void onProgress(int completed, int total);
        void onFinished();
    }
    static final class Cancellation {
        private volatile boolean cancelled;
        private final Set<HttpURLConnection> active = ConcurrentHashMap.newKeySet();
        boolean isCancelled() { return cancelled || Thread.currentThread().isInterrupted(); }
        void track(HttpURLConnection connection) {
            active.add(connection);
            if (cancelled) connection.disconnect();
        }
        void untrack(HttpURLConnection connection) { active.remove(connection); }
        void cancel() {
            cancelled = true;
            for (HttpURLConnection connection : active) connection.disconnect();
            active.clear();
        }
    }

    private final Cancellation cancellation = new Cancellation();
    private final List<HtmlHttpProbe.Target> targets;
    private final Prober prober;
    private final Listener listener;
    private ExecutorService workers;

    HtmlDiscoverySession(List<HtmlHttpProbe.Target> targets, Prober prober, Listener listener) {
        this.targets = new ArrayList<>(targets);
        this.prober = prober;
        this.listener = listener;
    }

    void start() {
        if (workers != null || cancellation.isCancelled()) return;
        if (targets.isEmpty()) { listener.onFinished(); return; }
        int count = Math.min(MAX_WORKERS, targets.size());
        workers = Executors.newFixedThreadPool(count);
        AtomicInteger cursor = new AtomicInteger(), completed = new AtomicInteger();
        AtomicInteger running = new AtomicInteger(count);
        for (int worker = 0; worker < count; ++worker) {
            workers.execute(() -> {
                try {
                    while (!cancellation.isCancelled()) {
                        int index = cursor.getAndIncrement();
                        if (index >= targets.size()) break;
                        try {
                            HtmlSource source = prober.probe(targets.get(index), cancellation);
                            if (source != null && !cancellation.isCancelled()) listener.onSource(source);
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception unavailable) { /* Try the next host. */ }
                        int done = completed.incrementAndGet();
                        if (!cancellation.isCancelled() && (done % MAX_WORKERS == 0 || done == targets.size())) {
                            listener.onProgress(done, targets.size());
                        }
                    }
                } finally {
                    if (running.decrementAndGet() == 0) {
                        workers.shutdown();
                        if (!cancellation.isCancelled()) listener.onFinished();
                    }
                }
            });
        }
    }

    void cancel() {
        cancellation.cancel();
        if (workers != null) workers.shutdownNow();
    }
}
