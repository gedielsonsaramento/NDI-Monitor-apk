package br.com.polegar.ndimonitor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Deliberate allowlist. Never serialize StartupJournal, Throwable messages or device diagnostics. */
final class SupportPayload {
    static final int MAX_BYTES = 8192;
    static final String[] CATEGORIES = {"video", "audio", "connection", "startup", "other"};

    static JSONObject create(String kind, String installationId, String appVersion,
                             SupportMetrics.Sample metrics, JSONObject failure, String category)
            throws JSONException {
        if (!kind.matches("heartbeat|manual_report|java_crash|native_crash|anr|memory_pressure|initialization_failure|resource_limit|session_interrupted"))
            throw new IllegalArgumentException("Unsupported support event");
        if (installationId == null || installationId.length() != 36)
            throw new IllegalArgumentException("A random installation UUID is required");
        UUID.fromString(installationId);
        JSONObject result = new JSONObject().put("schema", 1)
                .put("event_id", UUID.randomUUID().toString())
                .put("installation_id", installationId).put("app_version", appVersion)
                .put("kind", kind).put("metrics", metrics == null ? JSONObject.NULL : metrics.json());
        JSONObject safeFailure = sanitizeFailure(failure);
        if (safeFailure != null) result.put("failure", safeFailure);
        if ("manual_report".equals(kind)) {
            boolean known = false;
            for (String item : CATEGORIES) known |= item.equals(category);
            result.put("category", known ? category : "other");
        }
        return result;
    }

    private static JSONObject sanitizeFailure(JSONObject failure) throws JSONException {
        if (failure == null) return null;
        JSONObject safe = new JSONObject();
        String source = failure.optString("source");
        if ("android_exit_reason".equals(source) || "incomplete_session".equals(source))
            safe.put("source", source);
        String type = failure.optString("exception_class");
        if (!type.isEmpty() && type.length() <= 180 && type.matches("[A-Za-z0-9_.$]+")) {
            JSONArray frames = new JSONArray();
            JSONArray supplied = failure.optJSONArray("app_frames");
            if (supplied != null) for (int i = 0; i < supplied.length() && frames.length() < 8; i++) {
                String location = supplied.optString(i);
                if (location.startsWith("br.com.polegar.ndimonitor.") && location.length() <= 240
                        && location.matches("[A-Za-z0-9_.$#:<>()-]+")) frames.put(location);
            }
            safe.put("exception_class", type).put("app_frames", frames)
                    .put("fingerprint", fingerprint(type + frames));
        }
        return safe.length() == 0 ? null : safe;
    }

    static JSONObject javaFailure(Throwable error) throws JSONException {
        // Only a software exception class and locations in our published app code.
        String type = error.getClass().getName();
        if (type.length() > 180 || !type.matches("[A-Za-z0-9_.$]+")) type = "java.lang.Throwable";
        JSONArray frames = new JSONArray();
        for (StackTraceElement frame : error.getStackTrace()) {
            if (frame.getClassName().startsWith("br.com.polegar.ndimonitor.") && frames.length() < 8) {
                String location = frame.getClassName() + "#" + frame.getMethodName()
                        + ":" + Math.max(0, frame.getLineNumber());
                if (location.length() <= 240 && location.matches("[A-Za-z0-9_.$#:<>()-]+")) frames.put(location);
            }
        }
        return new JSONObject().put("exception_class", type).put("app_frames", frames)
                .put("fingerprint", fingerprint(type + frames));
    }

    private static String fingerprint(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) hex.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException(unavailable);
        }
    }

    private SupportPayload() { }
}
