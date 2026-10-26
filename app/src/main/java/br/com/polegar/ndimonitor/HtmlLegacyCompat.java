package br.com.polegar.ndimonitor;

import android.content.res.AssetManager;
import android.webkit.WebResourceResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Serves build-time adaptations only after verifying the current server's exact resource hash. */
final class HtmlLegacyCompat {
    static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final String MANIFEST = "html-compat/manifest.json";
    private static final Pattern CHROME = Pattern.compile("(?:Chrome|Chromium)/(\\d+)\\.");
    interface Assets { byte[] read(String path) throws IOException; }
    interface Fetcher { byte[] fetch(URI uri, Map<String, String> headers) throws IOException; }

    private final Assets assets;
    private final Fetcher fetcher;
    private final URI origin;
    private final int chrome;
    private final Map<String, byte[]> cache = new HashMap<>();
    private final Set<String> rejected = new HashSet<>();
    private final Set<String> applied = new HashSet<>();
    private List<Resource> resources;
    private volatile boolean closed;
    private int attempts;
    private volatile String status;

    HtmlLegacyCompat(AssetManager manager, String userAgent, String pageAddress) {
        this(path -> {
            try (InputStream stream = manager.open(path)) { return readBounded(stream); }
        }, HtmlLegacyCompat::download, userAgent, pageAddress);
    }

    HtmlLegacyCompat(Assets assets, Fetcher fetcher, String userAgent, String pageAddress) {
        this.assets = assets;
        this.fetcher = fetcher;
        this.chrome = chromeMajor(userAgent);
        URI parsed;
        try { parsed = URI.create(pageAddress); } catch (IllegalArgumentException invalid) { parsed = null; }
        origin = parsed;
        status = eligible() ? "aguardando arquivos da captura compatível"
                : chrome >= 87 ? "motor atual; página original"
                : "motor sem perfil validado";
    }

    String describe() { return "Compatibilidade HTML: " + status + " (Chrome " + chrome + ")"; }

    synchronized WebResourceResponse intercept(String method, String address, Map<String, String> headers) {
        if (closed || !eligible() || !"GET".equals(method)) return null;
        final URI requested;
        try { requested = URI.create(address); } catch (IllegalArgumentException invalid) { return null; }
        if (!sameOrigin(origin, requested)) return null;
        String path = requested.getPath();
        if (path == null) return null;
        String name = path.substring(path.lastIndexOf('/') + 1);
        // No native requests or reads for media, fonts, HTML documents, discovery or unknown files.
        if (!name.matches("main\\.[A-Za-z0-9]+\\.(js|css)")) return null;
        String cacheKey = requested.toASCIIString();
        if (!loadRegistry() || rejected.contains(cacheKey)) return null;
        Resource resource = null;
        for (Resource candidate : resources) if (candidate.name.equals(name)) { resource = candidate; break; }
        if (resource == null) return null;
        byte[] bytes = cache.get(cacheKey);
        if (bytes == null) {
            if (attempts >= 4) { status = "limite desta abertura atingido; página original"; return null; }
            attempts++;
            try {
                byte[] adapted = assets.read(resource.asset);
                if (adapted.length > MAX_BYTES || !sha256(adapted).equals(resource.assetHash)) {
                    throw new IOException("pacote local inválido");
                }
                byte[] original = fetcher.fetch(requested, headers == null ? Collections.emptyMap() : headers);
                if (original.length > MAX_BYTES || !sha256(original).equals(resource.sourceHash)) {
                    status = "arquivo diferente da captura; página original preservada";
                    rejected.add(cacheKey);
                    return null;
                }
                if (closed) return null;
                bytes = adapted;
                cache.put(cacheKey, bytes);
                applied.add(name);
                status = "adaptação aplicada a " + applied.size() + " de " + resources.size() + " arquivos";
            } catch (IOException unavailable) {
                status = "adaptação indisponível; página original preservada";
                rejected.add(cacheKey);
                return null;
            }
        }
        return new WebResourceResponse(resource.mime, "UTF-8", 200, "OK",
                Collections.singletonMap("Cache-Control", "no-store"), new ByteArrayInputStream(bytes));
    }

    // Called on the UI thread: never wait for an in-flight network read.
    void close() { closed = true; }

    private boolean eligible() { return chrome >= 44 && chrome <= 86 && sameOrigin(origin, origin); }

    private boolean loadRegistry() {
        if (resources != null) return !resources.isEmpty();
        resources = new ArrayList<>();
        try {
            byte[] bytes = assets.read(MANIFEST);
            if (bytes.length > 32768) throw new IOException("manifesto grande");
            JSONObject manifest = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            if (manifest.getInt("schema") != 1 || manifest.getInt("minChrome") != 44 ||
                    manifest.getInt("maxChrome") != 86) throw new IOException("perfil desconhecido");
            JSONArray items = manifest.getJSONArray("resources");
            if (items.length() < 1 || items.length() > 4) throw new IOException("recursos inválidos");
            Set<String> names = new HashSet<>();
            for (int i = 0; i < items.length(); ++i) {
                Resource r = new Resource(items.getJSONObject(i));
                if (!names.add(r.name)) throw new IOException("recurso repetido");
                resources.add(r);
            }
            return true;
        } catch (IOException | JSONException invalid) {
            resources.clear();
            status = "pacote de compatibilidade ausente ou inválido";
            return false;
        }
    }

    private static final class Resource {
        final String name, mime, asset, sourceHash, assetHash;
        Resource(JSONObject data) throws JSONException, IOException {
            name = data.getString("name"); mime = data.getString("mime");
            asset = data.getString("asset"); sourceHash = data.getString("sourceSha256");
            assetHash = data.getString("assetSha256");
            if (!name.matches("main\\.[A-Za-z0-9]+\\.(js|css)") ||
                    !asset.matches("html-compat/[A-Za-z0-9._-]+") || asset.contains("..") ||
                    !sourceHash.matches("[a-f0-9]{64}") || !assetHash.matches("[a-f0-9]{64}") ||
                    !(mime.equals("application/javascript") || mime.equals("text/css"))) {
                throw new IOException("recurso inválido");
            }
        }
    }

    static int chromeMajor(String agent) {
        Matcher match = CHROME.matcher(agent == null ? "" : agent);
        if (!match.find()) return 0;
        try { return Integer.parseInt(match.group(1)); } catch (NumberFormatException invalid) { return 0; }
    }

    static boolean sameOrigin(URI first, URI second) {
        if (first == null || second == null || first.getHost() == null || second.getHost() == null ||
                first.getUserInfo() != null || second.getUserInfo() != null) return false;
        String scheme = first.getScheme();
        return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) &&
                scheme.equalsIgnoreCase(second.getScheme()) &&
                first.getHost().equalsIgnoreCase(second.getHost()) && port(first) == port(second);
    }

    private static int port(URI uri) {
        return uri.getPort() >= 0 ? uri.getPort() : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    static String sha256(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(64);
            final String hex = "0123456789abcdef";
            for (byte b : hash) { result.append(hex.charAt((b & 255) >>> 4)); result.append(hex.charAt(b & 15)); }
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    static byte[] readBounded(InputStream stream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = stream.read(buffer)) != -1) {
            if (result.size() + count > MAX_BYTES) throw new IOException("recurso grande");
            result.write(buffer, 0, count);
        }
        return result.toByteArray();
    }

    static byte[] download(URI uri, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setUseCaches(false);
            // Do not forward cookies, authorization, ranges or compression framing.
            for (Map.Entry<String, String> header : headers.entrySet()) {
                String key = header.getKey().toLowerCase(Locale.ROOT);
                if (key.equals("user-agent") || key.equals("accept") || key.equals("accept-language")) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            connection.setRequestProperty("Accept-Encoding", "identity");
            if (connection.getResponseCode() != 200) throw new IOException("resposta não integral");
            if (connection.getContentLength() > MAX_BYTES) throw new IOException("recurso grande");
            try (InputStream stream = connection.getInputStream()) { return readBounded(stream); }
        } finally { connection.disconnect(); }
    }
}
