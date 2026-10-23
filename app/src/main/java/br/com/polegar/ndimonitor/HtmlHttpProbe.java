package br.com.polegar.ndimonitor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads a small HTML response only. Never loads scripts, images, cookies or API controls. */
final class HtmlHttpProbe {
    static final int MAX_BYTES = 65_536;
    static final int CONNECT_TIMEOUT_MS = 650;
    static final int READ_TIMEOUT_MS = 800;
    static final int DEADLINE_MS = 2_500;
    static final int MAX_REDIRECTS = 2;
    interface ConnectionOpener { HttpURLConnection open(URL url) throws IOException; }
    static final class Target {
        final String url;
        final ConnectionOpener opener;
        final String[] alternatives;
        Target(String url, ConnectionOpener opener, String... alternatives) {
            this.url = url;
            this.opener = opener;
            this.alternatives = alternatives.clone();
        }
    }
    private static final class MissingOutputRoute extends IOException { }

    HtmlSource probe(Target target, HtmlDiscoverySession.Cancellation cancellation) throws IOException {
        long deadline = System.nanoTime() + DEADLINE_MS * 1_000_000L;
        URL first = new URL(target.url);
        for (int candidate = 0; candidate <= target.alternatives.length && !cancellation.isCancelled(); ++candidate) {
            URL original = candidate == 0 ? first : new URL(target.alternatives[candidate - 1]);
            if (!sameOrigin(first, original) || original.getUserInfo() != null) return null;
            try {
                return probeAddress(original, target.opener, cancellation, deadline);
            } catch (MissingOutputRoute absent) {
                // A missing output name is different from an unreachable host. Try another
                // known display name only after an actual 404/410 from this server.
            }
        }
        return null;
    }

    private HtmlSource probeAddress(URL original, ConnectionOpener opener,
                                    HtmlDiscoverySession.Cancellation cancellation, long deadline) throws IOException {
        URL address = original;
        for (int redirect = 0; redirect <= MAX_REDIRECTS && !cancellation.isCancelled(); ++redirect) {
            long remaining = (deadline - System.nanoTime()) / 1_000_000L;
            if (remaining <= 0) return null;
            HttpURLConnection connection = opener.open(address);
            cancellation.track(connection);
            try {
                if (cancellation.isCancelled()) return null;
                connection.setConnectTimeout((int) Math.min(CONNECT_TIMEOUT_MS, remaining));
                connection.setReadTimeout((int) Math.min(READ_TIMEOUT_MS, remaining));
                connection.setInstanceFollowRedirects(false);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "text/html, application/xhtml+xml");
                connection.setRequestProperty("User-Agent", "MonitorDeRetorno/1.4.3 (LAN discovery)");
                int status = connection.getResponseCode();
                if (System.nanoTime() >= deadline || cancellation.isCancelled()) return null;
                if (status == 404 || status == 410) throw new MissingOutputRoute();
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    String location = connection.getHeaderField("Location");
                    if (redirect == MAX_REDIRECTS || location == null) return null;
                    URL next = new URL(address, location);
                    if (!mayFollow(original, address, next)) return null;
                    address = next;
                    continue;
                }
                if (status != 200) return null;
                String type = connection.getContentType();
                if (type == null) return null;
                String mime = type.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
                if (!mime.equals("text/html") && !mime.equals("application/xhtml+xml")) return null;
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (InputStream input = connection.getInputStream()) {
                    byte[] buffer = new byte[2048];
                    while (bytes.size() < MAX_BYTES && !cancellation.isCancelled()) {
                        long readRemaining = (deadline - System.nanoTime()) / 1_000_000L;
                        if (readRemaining <= 0) return null;
                        connection.setReadTimeout((int) Math.min(READ_TIMEOUT_MS, readRemaining));
                        int count = input.read(buffer, 0, Math.min(buffer.length, MAX_BYTES - bytes.size()));
                        if (count == -1) break;
                        bytes.write(buffer, 0, count);
                    }
                }
                if (cancellation.isCancelled()) return null;
                String html = new String(bytes.toByteArray(), charset(type));
                String lower = html.toLowerCase(Locale.ROOT);
                if (!lower.contains("<html") && !lower.contains("<!doctype html")) return null;
                Matcher title = Pattern.compile("<title[^>]*>(.*?)</title>",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
                String name = title.find() ? cleanTitle(title.group(1)) : "Retorno HTML";
                return new HtmlSource(original.toExternalForm(), name);
            } finally {
                cancellation.untrack(connection);
                connection.disconnect();
            }
        }
        return null;
    }

    private static boolean mayFollow(URL original, URL old, URL next) {
        try {
            URI before = old.toURI(), after = next.toURI();
            if (!sameOrigin(old, next) || after.getUserInfo() != null) return false;
            String beforePath = before.getPath().replaceAll("/+$", "");
            String afterPath = after.getPath().replaceAll("/+$", "");
            if (beforePath.equals(afterPath) &&
                    java.util.Objects.equals(before.getRawQuery(), after.getRawQuery())) return true;
            // S-Present's stable output URL opens a generated, display-only stage route.
            // Never follow login/control routes or move a custom saved query to another path.
            URI start = original.toURI();
            return HtmlSourceUrl.isOutputPath(start.getPath()) &&
                    start.getRawQuery() == null && before.getRawQuery() == null &&
                    HtmlSourceUrl.isOutputPath(beforePath) && afterPath.equals("/slide") &&
                    hasSingleStageSelector(after.getRawQuery());
        } catch (Exception invalid) { return false; }
    }

    private static boolean sameOrigin(URL before, URL after) {
        int beforePort = before.getPort() == -1 ? before.getDefaultPort() : before.getPort();
        int afterPort = after.getPort() == -1 ? after.getDefaultPort() : after.getPort();
        return before.getProtocol().equalsIgnoreCase(after.getProtocol()) &&
                before.getHost().equalsIgnoreCase(after.getHost()) && beforePort == afterPort;
    }

    private static boolean hasSingleStageSelector(String query) throws Exception {
        if (query == null) return false;
        int selectors = 0;
        for (String item : query.split("&", -1)) {
            String[] pair = item.split("=", 2);
            String name = URLDecoder.decode(pair[0], "UTF-8");
            if (!name.equalsIgnoreCase("output")) continue;
            if (!name.equals("output") || pair.length != 2 ||
                    !URLDecoder.decode(pair[1], "UTF-8").equals("stage")) return false;
            if (++selectors != 1) return false;
        }
        return selectors == 1;
    }

    private static Charset charset(String type) {
        Matcher matcher = Pattern.compile("charset\\s*=\\s*[\"']?([^;\\s\"']+)", Pattern.CASE_INSENSITIVE).matcher(type);
        try { if (matcher.find()) return Charset.forName(matcher.group(1)); }
        catch (IllegalArgumentException unsupported) { /* Use UTF-8 for unknown declarations. */ }
        return StandardCharsets.UTF_8;
    }

    private static String cleanTitle(String value) {
        String clean = value.replaceAll("<[^>]*>", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&nbsp;", " ").replaceAll("[\\p{Cntrl}\\s]+", " ").trim();
        return clean.length() > 100 ? clean.substring(0, 100) : clean;
    }
}
