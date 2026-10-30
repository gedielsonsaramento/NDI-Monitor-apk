package br.com.polegar.ndimonitor;

import android.net.TrafficStats;
import android.os.Debug;
import android.os.Process;
import android.os.SystemClock;
import org.json.JSONException;
import org.json.JSONObject;

/** Measurements of this app only. No interfaces, addresses or packet contents. */
final class SupportMetrics {
    interface Probe { Reading read(); }

    static final class Reading {
        final long elapsedMs, cpuMs, pssKiB, rxBytes, txBytes;
        final int processors;
        Reading(long elapsedMs, long cpuMs, long pssKiB, long rxBytes, long txBytes, int processors) {
            this.elapsedMs = elapsedMs;
            this.cpuMs = cpuMs;
            this.pssKiB = pssKiB;
            this.rxBytes = rxBytes;
            this.txBytes = txBytes;
            this.processors = Math.max(1, processors);
        }
    }

    static final class Sample {
        final Long pssKiB, windowMs, rxBytes, txBytes;
        final Double cpuPercent;
        Sample(Long pssKiB, Long windowMs, Double cpuPercent, Long rxBytes, Long txBytes) {
            this.pssKiB = pssKiB;
            this.windowMs = windowMs;
            this.cpuPercent = cpuPercent;
            this.rxBytes = rxBytes;
            this.txBytes = txBytes;
        }
        JSONObject json() throws JSONException {
            return new JSONObject().put("memory_pss_kib", nullable(pssKiB))
                    .put("sample_window_ms", nullable(windowMs))
                    .put("cpu_percent", nullable(cpuPercent))
                    .put("rx_bytes", nullable(rxBytes)).put("tx_bytes", nullable(txBytes));
        }
        private static Object nullable(Object value) { return value == null ? JSONObject.NULL : value; }
    }

    static final class Sampler {
        private final Probe probe;
        private Reading previous;
        Sampler(Probe probe) { this.probe = probe; }
        synchronized Sample sample() {
            Reading now = probe.read();
            Reading before = previous;
            previous = now;
            Long window = before != null && now.elapsedMs > before.elapsedMs
                    ? now.elapsedMs - before.elapsedMs : null;
            Double cpu = null;
            if (window != null && before.cpuMs >= 0 && now.cpuMs >= before.cpuMs) {
                cpu = Math.min(100d, 100d * (now.cpuMs - before.cpuMs) / window / now.processors);
                cpu = Math.round(cpu * 100d) / 100d;
            }
            return new Sample(now.pssKiB > 0 ? now.pssKiB : null, window, cpu,
                    window == null ? null : delta(before.rxBytes, now.rxBytes),
                    window == null ? null : delta(before.txBytes, now.txBytes));
        }
        private static Long delta(long before, long now) {
            return before >= 0 && now >= before ? now - before : null;
        }
    }

    static final class AndroidProbe implements Probe {
        @Override public Reading read() {
            long cpu = -1, pss = -1, rx = -1, tx = -1;
            int processors = 1;
            try { cpu = Process.getElapsedCpuTime(); } catch (RuntimeException unavailable) { }
            try { pss = Debug.getPss(); } catch (RuntimeException unavailable) { }
            try { rx = TrafficStats.getUidRxBytes(Process.myUid()); } catch (RuntimeException unavailable) { }
            try { tx = TrafficStats.getUidTxBytes(Process.myUid()); } catch (RuntimeException unavailable) { }
            try { processors = Runtime.getRuntime().availableProcessors(); } catch (RuntimeException unavailable) { }
            return new Reading(SystemClock.elapsedRealtime(), cpu, pss, rx, tx, processors);
        }
    }

    private SupportMetrics() { }
}
