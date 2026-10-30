package br.com.polegar.ndimonitor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;

final class SupportTestSupport {
    static final String ENDPOINT = "https://support.example.test/api/v1/telemetry";
    static final class Factory implements SupportTransport.ConnectionFactory {
        final List<Connection> connections = new ArrayList<>();
        int status = 202;
        Runnable onOutput;
        @Override public HttpsURLConnection open(URL url) {
            Connection connection = new Connection(url, status, onOutput);
            connections.add(connection);
            return connection;
        }
    }
    static final class Connection extends HttpsURLConnection {
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private final int status;
        private final Runnable onOutput;
        volatile boolean disconnected;
        Connection(URL url, int status, Runnable onOutput) { super(url); this.status = status; this.onOutput = onOutput; }
        @Override public String getCipherSuite() { return "test"; }
        @Override public Certificate[] getLocalCertificates() { return null; }
        @Override public Certificate[] getServerCertificates() { return new Certificate[0]; }
        @Override public void disconnect() { disconnected = true; }
        @Override public boolean usingProxy() { return false; }
        @Override public void connect() { }
        @Override public OutputStream getOutputStream() {
            if (onOutput != null) onOutput.run();
            return body;
        }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(new byte[0]); }
        @Override public InputStream getErrorStream() { return new ByteArrayInputStream(new byte[0]); }
        @Override public int getResponseCode() { return status; }
    }

    /** Virtual time: tests never sleep or make network requests. */
    static final class Scheduler extends AbstractExecutorService implements ScheduledExecutorService {
        long nowMs;
        private long sequence;
        private boolean shutdown;
        private final PriorityQueue<Task> queue = new PriorityQueue<>();
        final class Task implements ScheduledFuture<Object> {
            final Runnable runnable;
            final long due, order = sequence++;
            boolean cancelled, done;
            Task(Runnable runnable, long delayMs) { this.runnable = runnable; due = nowMs + delayMs; }
            @Override public long getDelay(TimeUnit unit) { return unit.convert(due - nowMs, TimeUnit.MILLISECONDS); }
            @Override public int compareTo(Delayed other) {
                Task task = (Task) other;
                int time = Long.compare(due, task.due);
                return time != 0 ? time : Long.compare(order, task.order);
            }
            @Override public boolean cancel(boolean interrupt) { cancelled = true; return !done; }
            @Override public boolean isCancelled() { return cancelled; }
            @Override public boolean isDone() { return done || cancelled; }
            @Override public Object get() { return null; }
            @Override public Object get(long timeout, TimeUnit unit) { return null; }
        }
        boolean runNext() {
            Task next;
            do { next = queue.poll(); } while (next != null && next.cancelled);
            if (next == null) return false;
            nowMs = Math.max(nowMs, next.due);
            try { next.runnable.run(); } finally { next.done = true; }
            return true;
        }
        long nextDelayMs() {
            while (!queue.isEmpty() && queue.peek().cancelled) queue.poll();
            return queue.isEmpty() ? -1 : queue.peek().due - nowMs;
        }
        @Override public ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit) {
            if (shutdown) throw new java.util.concurrent.RejectedExecutionException();
            Task task = new Task(runnable, unit.toMillis(delay));
            queue.add(task);
            return task;
        }
        @Override public void execute(Runnable runnable) { schedule(runnable, 0, TimeUnit.MILLISECONDS); }
        @Override public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) { throw new UnsupportedOperationException(); }
        @Override public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initial, long period, TimeUnit unit) { throw new UnsupportedOperationException(); }
        @Override public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initial, long delay, TimeUnit unit) { throw new UnsupportedOperationException(); }
        @Override public void shutdown() { shutdown = true; }
        @Override public List<Runnable> shutdownNow() { shutdown = true; queue.clear(); return Collections.emptyList(); }
        @Override public boolean isShutdown() { return shutdown; }
        @Override public boolean isTerminated() { return shutdown && queue.isEmpty(); }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return isTerminated(); }
    }

    static SupportMetrics.Probe probe(Scheduler clock) {
        return () -> new SupportMetrics.Reading(clock.nowMs, clock.nowMs / 10, 20480,
                clock.nowMs * 1000, clock.nowMs * 100, 4);
    }
    private SupportTestSupport() { }
}
