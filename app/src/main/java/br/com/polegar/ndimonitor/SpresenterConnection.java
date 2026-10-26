package br.com.polegar.ndimonitor;

import java.net.URI;
import java.util.Locale;

/** Credentials belong to one explicit origin, never to the LAN address range. */
final class SpresenterConnection {
    final String origin;
    final String token;

    SpresenterConnection(String address, String token) {
        origin = normalizeOrigin(address);
        if (token == null || !token.matches("spk_[A-Za-z0-9_-]{8,512}")) {
            throw new IllegalArgumentException("token");
        }
        this.token = token;
    }

    static String normalizeOrigin(String address) {
        try {
            String input = address == null ? "" : address.trim();
            if (input.isEmpty() || input.length() > 8192) throw new IllegalArgumentException("address");
            boolean complete = input.contains("://");
            URI uri = new URI(complete ? input : "http://" + input);
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            String path = uri.getRawPath();
            if ((!scheme.equals("http") && !scheme.equals("https")) || uri.getHost() == null ||
                    uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null ||
                    uri.getPort() == 0 || uri.getPort() > 65535 ||
                    (path != null && !path.isEmpty() && !path.equals("/") &&
                            !path.equals("/api/v1") && !path.equals("/api/v1/"))) {
                throw new IllegalArgumentException("address");
            }
            int port = uri.getPort();
            if (!complete && port == -1) port = HtmlSourceUrl.DEFAULT_PORT;
            return new URI(scheme, null, uri.getHost().toLowerCase(Locale.ROOT), port,
                    "", null, null).toASCIIString();
        } catch (Exception invalid) {
            throw new IllegalArgumentException("address");
        }
    }

    String endpoint() { return origin + "/api/v1/advanced"; }

    boolean owns(String address) {
        try {
            URI uri = URI.create(address);
            URI root = URI.create(origin);
            int port = uri.getPort() < 0 ? ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80) : uri.getPort();
            int rootPort = root.getPort() < 0 ? ("https".equals(root.getScheme()) ? 443 : 80) : root.getPort();
            return root.getScheme().equalsIgnoreCase(uri.getScheme()) && root.getHost().equalsIgnoreCase(uri.getHost()) &&
                    port == rootPort && uri.getUserInfo() == null;
        } catch (Exception invalid) { return false; }
    }
}
