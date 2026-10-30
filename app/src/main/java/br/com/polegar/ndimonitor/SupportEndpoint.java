package br.com.polegar.ndimonitor;

import java.net.URI;

/** A fixed HTTPS ingestion URL, never a playback URL or an editor token. */
final class SupportEndpoint {
    static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        try {
            URI uri = new URI(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getHost().isEmpty() || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || uri.getPort() < -1 || uri.getPort() == 0 || uri.getPort() > 65535)
                return "";
            String path = uri.getRawPath();
            if (path == null || path.isEmpty()) path = "/";
            String origin = new URI("https", null, uri.getHost().toLowerCase(java.util.Locale.ROOT),
                    uri.getPort(), null, null, null).toASCIIString();
            return new URI(origin + path).normalize().toASCIIString();
        } catch (java.net.URISyntaxException invalid) {
            return "";
        }
    }

    private SupportEndpoint() { }
}
