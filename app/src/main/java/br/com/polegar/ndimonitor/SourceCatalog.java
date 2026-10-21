package br.com.polegar.ndimonitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stable source order with a short grace period for intermittent mDNS updates. */
final class SourceCatalog {
    static final long MISSING_SOURCE_GRACE_MS = 10_000;
    private final Map<String, Long> lastSeen = new LinkedHashMap<>();
    private long keepKnownUntil;

    void beginRefresh(long nowMs) {
        keepKnownUntil = nowMs + MISSING_SOURCE_GRACE_MS;
    }

    List<String> update(String[] currentSources, long nowMs) {
        if (currentSources != null) {
            for (String name : currentSources) {
                if (name != null && !name.trim().isEmpty()) lastSeen.put(name, nowMs);
            }
        }
        Iterator<Map.Entry<String, Long>> entries = lastSeen.entrySet().iterator();
        while (entries.hasNext()) {
            if (nowMs - entries.next().getValue() >= MISSING_SOURCE_GRACE_MS
                    && nowMs >= keepKnownUntil) {
                entries.remove();
            }
        }
        return new ArrayList<>(lastSeen.keySet());
    }
}
