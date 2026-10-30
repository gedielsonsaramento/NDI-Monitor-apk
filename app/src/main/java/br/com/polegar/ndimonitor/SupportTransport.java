package br.com.polegar.ndimonitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

/** One-way HTTPS ingestion. Default certificate validation and no redirects/cookies/device UA. */
final class SupportTransport {
    interface Gate { boolean allowed(); }
    interface ConnectionFactory { HttpsURLConnection open(URL url) throws IOException; }
    private final ConnectionFactory factory;
    private final Object lock = new Object();
    private HttpsURLConnection active;
    private static final java.util.concurrent.ExecutorService CANCELLER =
            java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "SRetornoSupportCancel");
                thread.setDaemon(true);
                return thread;
            });

    SupportTransport() { this(url -> (HttpsURLConnection) url.openConnection()); }
    SupportTransport(ConnectionFactory factory) { this.factory = factory; }

    int post(String endpoint, JSONObject event, Gate gate) {
        String address = SupportEndpoint.normalize(endpoint);
        byte[] body = event.toString().getBytes(StandardCharsets.UTF_8);
        if (address.isEmpty() || body.length > SupportPayload.MAX_BYTES || !gate.allowed()) return 0;
        HttpsURLConnection connection = null;
        try {
            connection = factory.open(new URL(address));
            synchronized (lock) {
                if (!gate.allowed()) return 0;
                active = connection;
            }
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Cache-Control", "no-store");
            // Android's default Dalvik user agent includes device/OS details.
            connection.setRequestProperty("User-Agent", "S-Retorno-Support/1");
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(body.length);
            if (!gate.allowed()) return 0;
            try (OutputStream output = connection.getOutputStream()) {
                if (!gate.allowed()) return 0;
                output.write(body);
            }
            if (!gate.allowed()) return 0;
            int status = connection.getResponseCode();
            // No response body, error message or remote instructions are retained.
            InputStream response = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (response != null) response.close();
            return status;
        } catch (IOException | RuntimeException unavailable) {
            return 0;
        } finally {
            synchronized (lock) { if (active == connection) active = null; }
            if (connection != null) connection.disconnect();
        }
    }

    void cancel() {
        HttpsURLConnection connection;
        synchronized (lock) {
            connection = active;
            active = null;
        }
        // A platform disconnect may wait on I/O. Never make the UI wait for it.
        if (connection != null) CANCELLER.execute(connection::disconnect);
    }

    static boolean accepted(int status) { return status >= 200 && status < 300; }
}
