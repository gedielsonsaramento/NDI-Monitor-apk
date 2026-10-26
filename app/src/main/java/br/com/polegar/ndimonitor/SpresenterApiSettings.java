package br.com.polegar.ndimonitor;

import android.content.Context;
import android.content.SharedPreferences;

/** One catalog PC per installation. Unsupported keystores retain the token in memory only. */
final class SpresenterApiSettings {
    static final String ORIGIN = "spresenter_api_origin";
    static final String SECRET = "spresenter_api_encrypted_token";
    private static SpresenterConnection session;
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

    synchronized SpresenterConnection load() {
        String origin = origin();
        if (origin.isEmpty()) return null;
        synchronized (SpresenterApiSettings.class) {
            if (owner.equals(packageName) && session != null && session.origin.equals(origin)) return session;
            try {
                String saved = preferences.getString(SECRET, "");
                if (saved.isEmpty()) return null;
                SpresenterConnection connection = new SpresenterConnection(origin, cipher.decrypt(origin, saved));
                session = connection; owner = packageName;
                return connection;
            } catch (Exception unavailable) { return null; }
        }
    }

    String origin() { return preferences.getString(ORIGIN, ""); }

    boolean save(SpresenterConnection connection) {
        boolean durable = false;
        SharedPreferences.Editor edit = preferences.edit().putString(ORIGIN, connection.origin).remove(SECRET);
        try { edit.putString(SECRET, cipher.encrypt(connection.origin, connection.token)); durable = true; }
        catch (Exception unavailable) { /* Older firmware may not provide AndroidKeyStore. */ }
        edit.apply();
        synchronized (SpresenterApiSettings.class) { session = connection; owner = packageName; }
        return durable;
    }

    void clear() {
        preferences.edit().remove(ORIGIN).remove(SECRET).apply();
        synchronized (SpresenterApiSettings.class) { if (owner.equals(packageName)) { session = null; owner = ""; } }
    }
}
