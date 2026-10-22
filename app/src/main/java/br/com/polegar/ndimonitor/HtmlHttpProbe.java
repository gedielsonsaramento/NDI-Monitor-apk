package br.com.polegar.ndimonitor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
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
    interface ConnectionOpener { HttpURLConnection open(URL url) throws IOException; }
    static final class Target {
        final String url;
        final ConnectionOpener opener;
        Target(String url, ConnectionOpener opener) { this.url = url; this.opener = opener; }
    }

    HtmlSource probe(Target target, HtmlDiscoverySession.Cancellation cancellation) throws IOException {
        URL address = new URL(target.url);
        long deadline = System.nanoTime() + DEADLINE_MS * 1_000_000L;
        for (int redirect = 0; redirect <= 1 && !cancellation.isCancelled(); ++redirect) {
            HttpURLConnection connection = target.opener.open(address);
            cancellation.track(connection);
            try {
                if (cancellation.isCancelled()) return null;
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setInstanceFollowRedirects(false);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "text/html, application/xhtml+xml");
                connection.setRequestProperty("User-Agent", "MonitorDeRetorno/1.4 (LAN discovery)");
                int status = connection.getResponseCode();
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    String location = connection.getHeaderField("Location");
                    if (redirect != 0 || location == null) return null;
                    URL next = new URL(address, location);
                    if (!sameEndpoint(address, next)) return null;
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
                        long remaining = (deadline - System.nanoTime()) / 1_000_000L;
                        if (remaining <= 0) return null;
                        connection.setReadTimeout((int) Math.min(READ_TIMEOUT_MS, remaining));
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
                return new HtmlSource(target.url, name);
            } finally {
                cancellation.untrack(connection);
                connection.disconnect();
            }
        }
        return null;
    }

    private static boolean sameEndpoint(URL old, URL next) {
        try {
            URI before = old.toURI(), after = next.toURI();
            return before.getScheme().equalsIgnoreCase(after.getScheme()) &&
                    before.getHost().equalsIgnoreCase(after.getHost()) && before.getPort() == after.getPort() &&
                    after.getUserInfo() == null &&
                    before.getPath().replaceAll("/+$", "").equals(after.getPath().replaceAll("/+$", "")) &&
                    java.util.Objects.equals(before.getRawQuery(), after.getRawQuery());
        } catch (Exception invalid) { return false; }
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
