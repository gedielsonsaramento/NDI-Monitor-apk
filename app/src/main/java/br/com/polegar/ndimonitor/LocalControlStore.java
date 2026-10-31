package br.com.polegar.ndimonitor;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/** Local pairing and a small, redacted event history; never shared with cloud support. */
@android.annotation.SuppressLint("ApplySharedPref")
final class LocalControlStore {
    static final String PREFS = "monitor_local_control_v1";
    private final SharedPreferences prefs;
    private final SpresenterTokenCipher cipher;
    private String token = "";
    private boolean durable;
    LocalControlStore(Context context) { this(context, new SpresenterTokenCipher()); }
    LocalControlStore(Context context, SpresenterTokenCipher cipher) {
        this.cipher = cipher;
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            String encrypted = prefs.getString("credential", "");
            if (!encrypted.isEmpty()) {
                token = cipher.decrypt("s-retorno-lan-v1", encrypted);
                durable = true;
            }
        } catch (Exception unavailable) { prefs.edit().remove("credential").commit(); }
    }
    boolean enabled() { return prefs.getBoolean("enabled", false); }
    synchronized String token() { return token; }
    synchronized boolean durable() { return durable; }
    synchronized void enable() {
        if (!enabled()) prefs.edit().putBoolean("enabled", true).putLong("since", System.currentTimeMillis()).commit();
    }
    synchronized void revoke() {
        token = ""; durable = false;
        prefs.edit().clear().commit();
    }
    synchronized JSONObject pair(String credential, String name) throws Exception {
        SharedPreferences.Editor edit = prefs.edit().putBoolean("enabled", true).putBoolean("session_active", true).putString("name", cleanName(name));
        String id = prefs.getString("id", "");
        if (id.isEmpty()) edit.putString("id", UUID.randomUUID().toString());
        boolean saved = false;
        try {
            edit.putString("credential", cipher.encrypt("s-retorno-lan-v1", credential));
            saved = true;
        } catch (Exception unavailable) { edit.remove("credential"); }
        if (!edit.commit()) throw new java.io.IOException("Local storage unavailable");
        token = credential; durable = saved;
        event("paired");
        return new JSONObject().put("id", id()).put("name", name()).put("durable", durable);
    }
    synchronized String id() { return prefs.getString("id", ""); }
    String name() { return prefs.getString("name", "Retorno"); }
    void rename(String name) { prefs.edit().putString("name", cleanName(name)).apply(); }
    static String cleanName(String value) {
        String name = value == null ? "" : value.replaceAll("[\\p{Cntrl}\\s]+", " ").trim();
        return name.isEmpty() ? "Retorno" : name.substring(0, Math.min(48, name.length()));
    }
    synchronized void foreground() {
        if (!enabled()) return;
        // A persisted foreground marker can mean daily power-off; never infer a crash from it.
        prefs.edit().putBoolean("session_active", true).commit();
    }
    synchronized void reviewPreviousExit(SupportExitHistory.Exit exit) {
        if (!enabled() || exit == null || exit.timestampMs < prefs.getLong("since", Long.MAX_VALUE)
                || exit.timestampMs <= prefs.getLong("last_exit", 0)) return;
        prefs.edit().putLong("last_exit", exit.timestampMs).apply();
        if (exit.normal) { background(); return; }
        if (prefs.getBoolean("session_active", false) && !exit.kind.isEmpty()) {
            event(exit.kind);
            background();
        }
    }
    synchronized void background() { prefs.edit().remove("session_active").apply(); }
    synchronized void event(String kind) {
        if (!enabled() || kind == null || !kind.matches("[a-z_]{2,40}") || availabilityOnly(kind)) return;
        try {
            JSONArray old = events(), next = new JSONArray();
            // Coalesce repeated polling/retry failures without a growing event log.
            JSONObject last = old.optJSONObject(old.length() - 1);
            if (last != null && kind.equals(last.optString("kind"))
                    && System.currentTimeMillis() - last.optLong("at") < 30000) return;
            for (int i = Math.max(0, old.length() - 23); i < old.length(); i++) next.put(old.getJSONObject(i));
            next.put(new JSONObject().put("kind", kind).put("at", System.currentTimeMillis()));
            prefs.edit().putString("events", next.toString()).commit();
        } catch (Exception ignored) { }
    }
    private static boolean availabilityOnly(String kind) {
        return "session_interrupted".equals(kind) || "html_error".equals(kind) || "ndi_signal_error".equals(kind);
    }
    synchronized JSONArray events() {
        String value = prefs.getString("events", "[]");
        if (value.length() > 8192) return new JSONArray();
        try {
            JSONArray old = new JSONArray(value), filtered = new JSONArray();
            for (int i = 0; i < old.length(); i++) {
                JSONObject event = old.optJSONObject(i);
                if (event != null && !availabilityOnly(event.optString("kind"))) filtered.put(event);
            }
            return filtered;
        } catch (Exception invalid) { return new JSONArray(); }
    }
}
