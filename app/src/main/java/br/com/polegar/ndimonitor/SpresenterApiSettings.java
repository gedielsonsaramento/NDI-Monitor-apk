package br.com.polegar.ndimonitor;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Bounded PC catalogs, with each credential encrypted for its own origin. */
final class SpresenterApiSettings {
    static final String ORIGIN = "spresenter_api_origin";
    static final String SECRET = "spresenter_api_encrypted_token";
    static final String SERVERS = "spresenter_api_servers_v2";
    static final int MAX_CONNECTIONS = 8;
    private static final Map<String, SpresenterConnection> sessions = new LinkedHashMap<>();
    private static String owner = "";
    private final SharedPreferences preferences;
    private final String packageName;
    private final SpresenterTokenCipher cipher;

    SpresenterApiSettings(Context context) {
        this(context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE),
                context.getPackageName(), new SpresenterTokenCipher());
    }
    SpresenterApiSettings(SharedPreferences preferences, String packageName, SpresenterTokenCipher cipher) {
        this.preferences = preferences; this.packageName = packageName; this.cipher = cipher;
    }

    synchronized SpresenterConnection load() { return load(origin()); }

    synchronized SpresenterConnection load(String origin) {
        Map<String, String> saved = entries();
        if (!saved.containsKey(origin)) return null;
        synchronized (SpresenterApiSettings.class) {
            selectOwner();
            SpresenterConnection cached = sessions.get(origin);
            if (cached != null) return cached;
            try {
                String encrypted = saved.get(origin);
                if (encrypted.isEmpty()) return null;
                SpresenterConnection connection = new SpresenterConnection(origin, cipher.decrypt(origin, encrypted));
                sessions.put(origin, connection);
                return connection;
            } catch (Exception unavailable) { return null; }
        }
    }

    synchronized List<SpresenterConnection> loadAll() {
        List<SpresenterConnection> result = new ArrayList<>();
        for (String origin : origins()) {
            SpresenterConnection connection = load(origin);
            if (connection != null) result.add(connection);
        }
        return result;
    }

    synchronized List<String> origins() { return new ArrayList<>(entries().keySet()); }

    String origin() { return preferences.getString(ORIGIN, ""); }

    synchronized boolean save(SpresenterConnection connection) {
        Map<String, String> saved = entries();
        if (!saved.containsKey(connection.origin) && saved.size() >= MAX_CONNECTIONS) {
            throw new IllegalArgumentException("Catalog PC limit reached");
        }
        boolean durable = false;
        String encrypted = "";
        try { encrypted = cipher.encrypt(connection.origin, connection.token); durable = true; }
        catch (Exception unavailable) { /* Older firmware may not provide AndroidKeyStore. */ }
        saved.put(connection.origin, encrypted);
        write(saved, connection.origin);
        synchronized (SpresenterApiSettings.class) { selectOwner(); sessions.put(connection.origin, connection); }
        return durable;
    }

    synchronized void remove(String origin) {
        Map<String, String> saved = entries();
        if (!saved.containsKey(origin)) return;
        saved.remove(origin);
        String active = origin();
        if (!saved.containsKey(active)) active = saved.isEmpty() ? "" : saved.keySet().iterator().next();
        write(saved, active);
        synchronized (SpresenterApiSettings.class) { if (owner.equals(packageName)) sessions.remove(origin); }
    }

    synchronized void clear() {
        preferences.edit().remove(ORIGIN).remove(SECRET).remove(SERVERS).apply();
        synchronized (SpresenterApiSettings.class) { if (owner.equals(packageName)) { sessions.clear(); owner = ""; } }
    }

    private void selectOwner() {
        if (!owner.equals(packageName)) { sessions.clear(); owner = packageName; }
    }

    private Map<String, String> entries() {
        Map<String, String> result = new LinkedHashMap<>();
        String json = preferences.getString(SERVERS, "");
        if (!json.isEmpty() && json.length() <= 32768) try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < Math.min(array.length(), MAX_CONNECTIONS); ++i) {
                JSONObject entry = array.optJSONObject(i);
                if (entry == null) continue;
                String origin = entry.optString("origin");
                try {
                    if (origin.equals(SpresenterConnection.normalizeOrigin(origin))) {
                        result.put(origin, entry.optString("secret", ""));
                    }
                } catch (IllegalArgumentException invalid) { }
            }
        } catch (Exception invalid) { }
        // Read the previous single-PC format without changing or losing its key.
        String legacy = origin();
        if (!legacy.isEmpty() && !result.containsKey(legacy) && result.size() < MAX_CONNECTIONS) try {
            if (legacy.equals(SpresenterConnection.normalizeOrigin(legacy))) {
                result.put(legacy, preferences.getString(SECRET, ""));
            }
        } catch (IllegalArgumentException invalid) { }
        return result;
    }

    private void write(Map<String, String> values, String active) {
        SharedPreferences.Editor edit = preferences.edit().remove(ORIGIN).remove(SECRET).remove(SERVERS);
        if (!values.isEmpty()) try {
            JSONArray array = new JSONArray();
            for (Map.Entry<String, String> entry : values.entrySet()) {
                array.put(new JSONObject().put("origin", entry.getKey()).put("secret", entry.getValue()));
            }
            edit.putString(SERVERS, array.toString()).putString(ORIGIN, active);
            String encrypted = values.get(active);
            if (encrypted != null && !encrypted.isEmpty()) edit.putString(SECRET, encrypted);
        } catch (org.json.JSONException invalid) { throw new IllegalStateException("Catalog storage unavailable", invalid); }
        edit.apply();
    }
}
