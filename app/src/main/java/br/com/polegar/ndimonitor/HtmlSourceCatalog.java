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
    private final Map<String, java.util.Set<String>> pinnedCatalogs = new LinkedHashMap<>();

    void forgetDirectCatalog(String origin, long now) {
        if (!pinnedCatalogs.containsKey(origin)) replaceFromCatalog(origin, new ArrayList<>(), now);
    }

    void replacePinnedCatalog(String origin, List<HtmlSource> current, long now) {
        replaceFromCatalog(origin, current, now);
        java.util.Set<String> urls = new java.util.HashSet<>();
        for (HtmlSource source : current) urls.add(source.url);
        pinnedCatalogs.put(origin, urls);
    }

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
        else {
            // A generic page title must not overwrite the server's destination name.
            if (!source.catalogOrigin.isEmpty() || entry.source.catalogOrigin.isEmpty()) entry.source = source;
            entry.lastSeen = now;
        }
    }

    void replaceFromCatalog(String origin, List<HtmlSource> current, long now) {
        java.util.Set<String> urls = new java.util.HashSet<>();
        for (HtmlSource source : current) if (origin.equals(source.catalogOrigin)) urls.add(source.url);
        Iterator<Entry> iterator = sources.values().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (origin.equals(entry.source.catalogOrigin) && !urls.contains(entry.source.url)) {
                if (entry.source.url.equals(remembered)) {
                    entry.source = new HtmlSource(remembered, "Último retorno salvo");
                } else iterator.remove();
            }
        }
        for (HtmlSource source : current) if (origin.equals(source.catalogOrigin)) found(source, now);
    }

    void finishScan(long now) {
        Iterator<Entry> iterator = sources.values().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            boolean pinned = false;
            for (java.util.Set<String> urls : pinnedCatalogs.values()) if (urls.contains(entry.source.url)) pinned = true;
            if (!pinned && !entry.source.url.equals(remembered) && now - entry.lastSeen >= ABSENCE_GRACE_MS) {
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
