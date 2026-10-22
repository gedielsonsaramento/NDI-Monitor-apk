package br.com.polegar.ndimonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class HtmlDiscoverySessionTest {
    private List<HtmlHttpProbe.Target> targets(int count) {
        List<HtmlHttpProbe.Target> targets = new ArrayList<>();
        for (int i = 0; i < count; ++i) targets.add(new HtmlHttpProbe.Target("http://192.168.1." + (i + 1) + ":5050/output/saida-html-1", null));
        return targets;
    }
    @Test public void boundsConcurrencyAndDeliversAllIncrementalResults() throws Exception {
        AtomicInteger active = new AtomicInteger(), peak = new AtomicInteger(), found = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(HtmlDiscoverySession.MAX_WORKERS);
        CountDownLatch release = new CountDownLatch(1), finished = new CountDownLatch(1);
        HtmlDiscoverySession scan = new HtmlDiscoverySession(targets(30), (target, cancellation) -> {
            peak.accumulateAndGet(active.incrementAndGet(), Math::max);
            entered.countDown();
            try { assertTrue(release.await(5, TimeUnit.SECONDS)); return new HtmlSource(target.url, "Stage"); }
            finally { active.decrementAndGet(); }
        }, new HtmlDiscoverySession.Listener() {
            @Override public void onSource(HtmlSource source) { found.incrementAndGet(); }
            @Override public void onProgress(int completed, int total) { assertEquals(30, total); }
            @Override public void onFinished() { finished.countDown(); }
        });
        try {
            scan.start();
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertEquals(HtmlDiscoverySession.MAX_WORKERS, peak.get());
            release.countDown();
            assertTrue(finished.await(5, TimeUnit.SECONDS));
            assertEquals(30, found.get());
            assertTrue(peak.get() <= HtmlDiscoverySession.MAX_WORKERS);
        } finally { release.countDown(); scan.cancel(); }
    }
    @Test public void cancellingIgnoresLateResults() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), returned = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger(), callbacks = new AtomicInteger();
        HtmlDiscoverySession scan = new HtmlDiscoverySession(targets(1), (target, cancellation) -> {
            calls.incrementAndGet(); entered.countDown();
            try { release.await(5, TimeUnit.SECONDS); }
            catch (InterruptedException ignored) { /* Simulate a legacy operation returning after cancellation. */ }
            returned.countDown();
            return new HtmlSource(target.url, "Late");
        }, new HtmlDiscoverySession.Listener() {
            @Override public void onSource(HtmlSource source) { callbacks.incrementAndGet(); }
            @Override public void onProgress(int completed, int total) { callbacks.incrementAndGet(); }
            @Override public void onFinished() { callbacks.incrementAndGet(); }
        });
        scan.start(); assertTrue(entered.await(5, TimeUnit.SECONDS));
        scan.cancel(); release.countDown();
        assertTrue(returned.await(5, TimeUnit.SECONDS));
        assertEquals(1, calls.get());
        assertEquals(0, callbacks.get());
    }
}
