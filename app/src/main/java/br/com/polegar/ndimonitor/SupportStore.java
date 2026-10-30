package br.com.polegar.ndimonitor;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Private, bounded failure queue. Opt-out clears the identifier and unsent data. */
@android.annotation.SuppressLint("ApplySharedPref")
final class SupportStore {
    static final String PREFERENCES = "monitor_support";
    static final int MAX_EVENTS = 8;
    static final int MAX_QUEUE_BYTES = 32768;
    final String endpoint;
    private final SharedPreferences preferences;

    SupportStore(Context context, String endpoint) {
        this.endpoint = SupportEndpoint.normalize(endpoint);
        preferences = context.getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        // Authorization is for one configured destination, not every future server.
        if (preferences.getBoolean("enabled", false)
                && !this.endpoint.equals(preferences.getString("destination", "")))
            preferences.edit().clear().commit();
    }

    synchronized boolean configured() { return !endpoint.isEmpty(); }
    synchronized boolean enabled() {
        return configured() && preferences.getBoolean("enabled", false)
                && endpoint.equals(preferences.getString("destination", ""));
    }

    synchronized void setEnabled(boolean enabled, long nowMs) {
        if (!enabled || !configured()) {
            preferences.edit().clear().commit();
            return;
        }
        if (enabled()) return;
        preferences.edit().putBoolean("enabled", true).putString("destination", endpoint)
                .putLong("consent_since_ms", nowMs).commit();
        installationId();
    }

    synchronized String installationId() {
        String id = preferences.getString("installation_id", "");
        try { if (id.length() == 36) return UUID.fromString(id).toString(); }
        catch (IllegalArgumentException ignored) { }
        id = UUID.randomUUID().toString();
        preferences.edit().putString("installation_id", id).commit();
        return id;
    }

    synchronized String automaticId() { return enabled() ? installationId() : null; }

    synchronized boolean[] takePreviousSession() {
        boolean[] result = {enabled() && preferences.getBoolean("session_active", false),
                enabled() && preferences.getBoolean("java_recorded", false)};
        if (preferences.contains("session_active") || preferences.contains("java_recorded"))
            preferences.edit().remove("session_active").remove("java_recorded").commit();
        return result;
    }

    synchronized void markForeground() {
        if (enabled()) preferences.edit().putBoolean("session_active", true).commit();
    }

    synchronized void markBackground() {
        if (preferences.contains("session_active")) preferences.edit().remove("session_active").apply();
    }
    synchronized long consentSinceMs() { return preferences.getLong("consent_since_ms", Long.MAX_VALUE); }
    synchronized long lastExitMs() { return preferences.getLong("last_exit_ms", 0); }
    synchronized void markExitSeen(long timestamp) {
        if (enabled()) preferences.edit().putLong("last_exit_ms", timestamp).apply();
    }

    synchronized void enqueue(JSONObject event, boolean javaCrash) {
        if (!enabled() || event.toString().getBytes(StandardCharsets.UTF_8).length > SupportPayload.MAX_BYTES) return;
        JSONArray old = queue();
        List<JSONObject> items = new ArrayList<>();
        for (int i = 0; i < old.length(); i++) {
            JSONObject item = old.optJSONObject(i);
            if (item != null) items.add(item);
        }
        items.add(event);
        while (items.size() > MAX_EVENTS || encode(items).getBytes(StandardCharsets.UTF_8).length > MAX_QUEUE_BYTES)
            items.remove(0);
        SharedPreferences.Editor edit = preferences.edit().putString("pending_events", encode(items));
        if (javaCrash) edit.putBoolean("java_recorded", true);
        edit.commit();
    }

    synchronized List<JSONObject> pending() {
        List<JSONObject> result = new ArrayList<>();
        JSONArray array = queue();
        for (int i = 0; i < Math.min(array.length(), MAX_EVENTS); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item != null) result.add(item);
        }
        return result;
    }

    synchronized void acknowledge(String eventId) {
        if (!enabled()) return;
        JSONArray old = queue();
        List<JSONObject> retained = new ArrayList<>();
        for (int i = 0; i < old.length(); i++) {
            JSONObject item = old.optJSONObject(i);
            if (item != null && !eventId.equals(item.optString("event_id"))) retained.add(item);
        }
        preferences.edit().putString("pending_events", encode(retained)).apply();
    }

    private JSONArray queue() {
        String stored = preferences.getString("pending_events", "[]");
        if (stored.length() > MAX_QUEUE_BYTES) return new JSONArray();
        try { return new JSONArray(stored); }
        catch (JSONException invalid) { return new JSONArray(); }
    }

    private static String encode(List<JSONObject> items) {
        JSONArray result = new JSONArray();
        for (JSONObject item : items) result.put(item);
        return result.toString();
    }
}
