package br.com.polegar.ndimonitor;

import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, application = Application.class)
public class SupportMetricsTest {
    @Test public void measuresOnlyTheIntervalAndNormalizesCpuToAvailableProcessors() throws Exception {
        SupportMetrics.Reading[] reads = {
                new SupportMetrics.Reading(1000, 100, 10240, 500000, 1000, 4),
                new SupportMetrics.Reading(3000, 2100, 20480, 2500000, 3000, 4)};
        int[] index = {0};
        SupportMetrics.Sampler sampler = new SupportMetrics.Sampler(() -> reads[index[0]++]);
        SupportMetrics.Sample first = sampler.sample();
        assertNull(first.cpuPercent);
        assertNull(first.rxBytes);
        SupportMetrics.Sample next = sampler.sample();
        assertEquals(Long.valueOf(2000), next.windowMs);
        assertEquals(25d, next.cpuPercent, .0001);
        assertEquals(Long.valueOf(2000000), next.rxBytes);
        assertEquals(Long.valueOf(2000), next.txBytes);
        assertEquals(Long.valueOf(20480), next.pssKiB);
        assertEquals(5, next.json().length());
    }

    @Test public void unavailableOrResetCountersAreUnknownRatherThanInventedZeroUsage() throws Exception {
        SupportMetrics.Reading[] reads = {
                new SupportMetrics.Reading(1000, 100, -1, -1, 100, 1),
                new SupportMetrics.Reading(2000, 50, 0, -1, 50, 1)};
        int[] index = {0};
        SupportMetrics.Sampler sampler = new SupportMetrics.Sampler(() -> reads[index[0]++]);
        sampler.sample();
        SupportMetrics.Sample sample = sampler.sample();
        assertTrue(sample.json().isNull("cpu_percent"));
        assertTrue(sample.json().isNull("memory_pss_kib"));
        assertTrue(sample.json().isNull("rx_bytes"));
        assertTrue(sample.json().isNull("tx_bytes"));
    }

    @Test public void oldAndroidCanReadOwnMetricsAndExitHistoryWithoutNewApiLinkage() {
        SupportMetrics.Reading sample = new SupportMetrics.AndroidProbe().read();
        assertTrue(sample.elapsedMs >= 0);
        assertNull(SupportExitHistory.reader(org.robolectric.RuntimeEnvironment.getApplication()).latest());
    }
}
