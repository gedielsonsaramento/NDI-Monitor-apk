package br.com.polegar.ndimonitor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads the authenticated destination catalog. No control or write request is sent. */
final class SpresenterApiProbe {
    static final int MAX_BYTES = 1_048_576;
    static final int MAX_OUTPUTS = 512;
    static final int DEADLINE_MS = 5_000;

    static final class Failure extends IOException {
        final int message;
        Failure(int message) { super("Spresenter catalog unavailable"); this.message = message; }
    }

    List<HtmlSource> read(SpresenterConnection server, HtmlHttpProbe.ConnectionOpener opener,
                          HtmlDiscoverySession.Cancellation cancellation) throws IOException {
        if (cancellation.isCancelled()) throw new Failure(R.string.spresenter_api_cancelled);
        HttpURLConnection connection = opener.open(new URL(server.endpoint()));
        cancellation.track(connection);
        long deadline = System.nanoTime() + DEADLINE_MS * 1_000_000L;
        try {
            if (cancellation.isCancelled()) throw new Failure(R.string.spresenter_api_cancelled);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2_000);
            connection.setReadTimeout(3_000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + server.token);
            int status = connection.getResponseCode();
            if (status == 401) throw new Failure(R.string.spresenter_api_unauthorized);
            if (status == 403) throw new Failure(R.string.spresenter_api_forbidden);
            if (status != 200) throw new Failure(R.string.spresenter_api_unavailable);
            String type = connection.getContentType();
            if (type == null || !type.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
                throw new Failure(R.string.spresenter_api_invalid);
            }
            if (connection.getContentLength() > MAX_BYTES) throw new Failure(R.string.spresenter_api_invalid);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream input = connection.getInputStream()) {
                byte[] buffer = new byte[4096];
                while (!cancellation.isCancelled()) {
                    long remaining = (deadline - System.nanoTime()) / 1_000_000L;
                    if (remaining <= 0) throw new Failure(R.string.spresenter_api_network);
                    connection.setReadTimeout((int) Math.min(3_000, remaining));
                    int count = input.read(buffer, 0, Math.min(buffer.length, MAX_BYTES + 1 - bytes.size()));
                    if (count == -1) break;
                    bytes.write(buffer, 0, count);
                    if (bytes.size() > MAX_BYTES) throw new Failure(R.string.spresenter_api_invalid);
                }
            }
            if (cancellation.isCancelled()) throw new Failure(R.string.spresenter_api_cancelled);
            return parse(server.origin, new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            cancellation.untrack(connection);
            connection.disconnect();
        }
    }

    static List<HtmlSource> parse(String origin, String json) throws Failure {
        try {
            String root = SpresenterConnection.normalizeOrigin(origin);
            JSONObject response = new JSONObject(json);
            JSONArray outputs = response.getJSONObject("config").getJSONArray("outputs");
            if (outputs.length() > MAX_OUTPUTS) throw new IllegalArgumentException();
            JSONArray canvases = response.optJSONArray("virtualOutputs");
            Map<String, HtmlSource> sources = new LinkedHashMap<>();
            for (int i = 0; i < outputs.length(); i++) {
                JSONObject output = outputs.optJSONObject(i);
                if (output == null || !"network".equals(output.optString("type")) ||
                        Boolean.FALSE.equals(output.opt("enabled"))) continue;
                Object rawSlug = output.opt("slug");
                if (!(rawSlug instanceof String)) continue;
                String slug = (String) rawSlug;
                if (!HtmlSourceUrl.isOutputPath("/output/" + slug)) continue;
                String name = output.opt("name") instanceof String ? cleanName(output.getString("name")) : "";
                if (name.isEmpty()) {
                    String key = String.valueOf(output.opt("canvasKey"));
                    if (key.equals("stage")) name = "Retorno (Stage)";
                    else if (canvases != null && key.matches("[0-9]{1,6}")) {
                        JSONObject canvas = canvases.optJSONObject(Integer.parseInt(key));
                        if (canvas != null && canvas.opt("name") instanceof String) name = cleanName(canvas.getString("name"));
                    }
                }
                if (name.isEmpty()) name = slug;
                String url = root + "/output/" + slug;
                if (!sources.containsKey(url)) sources.put(url, new HtmlSource(url, name, root));
            }
            return new ArrayList<>(sources.values());
        } catch (Exception invalid) { throw new Failure(R.string.spresenter_api_invalid); }
    }

    private static String cleanName(String name) {
        String clean = name.replaceAll("[\\p{Cntrl}\\s]+", " ").trim();
        return clean.length() > 160 ? clean.substring(0, 160) : clean;
    }
}
