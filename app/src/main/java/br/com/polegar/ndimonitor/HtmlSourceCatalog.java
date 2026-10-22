package br.com.polegar.ndimonitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Retains selection order during rescans, with a grace period for missing endpoints. */
final class HtmlSourceCatalog {
    static final long ABSENCE_GRACE_MS = 90_000;
    private static final class Entry {
        HtmlSource source;
        long lastSeen;
        Entry(HtmlSource source, long now) { this.source = source; lastSeen = now; }
    }
    private final Map<String, Entry> sources = new LinkedHashMap<>();
    private String remembered = "";

    void remember(String address, long now) {
        if (!HtmlSourceUrl.isWebAddress(address)) return;
        remembered = address;
        if (!sources.containsKey(address)) {
            sources.put(address, new Entry(new HtmlSource(address, "Último retorno salvo"), now));
        }
    }

    void found(HtmlSource source, long now) {
        Entry entry = sources.get(source.url);
        if (entry == null) sources.put(source.url, new Entry(source, now));
        else { entry.source = source; entry.lastSeen = now; }
    }

    void finishScan(long now) {
        Iterator<Entry> iterator = sources.values().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (!entry.source.url.equals(remembered) && now - entry.lastSeen >= ABSENCE_GRACE_MS) {
                iterator.remove();
            }
        }
    }

    List<HtmlSource> snapshot() {
        List<HtmlSource> result = new ArrayList<>();
        for (Entry entry : sources.values()) result.add(entry.source);
        return result;
    }
}
