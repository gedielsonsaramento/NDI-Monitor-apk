package br.com.polegar.ndimonitor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Accepts web addresses only. A bare PC address expands to the Stage HTML route. */
final class HtmlSourceUrl {
    static final int DEFAULT_PORT = 5050;
    static final String DEFAULT_PATH = "/output/saida-html-1";

    private HtmlSourceUrl() {}

    static String normalize(String input) {
        String text = input == null ? "" : input.trim();
        if (text.isEmpty() || text.length() > 8192) throw new IllegalArgumentException();
        for (int i = 0; i < text.length(); ++i) {
            if (Character.isWhitespace(text.charAt(i)) || Character.isISOControl(text.charAt(i))) {
                throw new IllegalArgumentException();
            }
        }
        boolean complete = text.contains("://");
        try {
            URI uri = new URI(complete ? text : "http://" + text);
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            if ((!scheme.equals("http") && !scheme.equals("https")) ||
                    uri.getHost() == null || uri.getUserInfo() != null ||
                    uri.getPort() == 0 || uri.getPort() > 65535) {
                throw new IllegalArgumentException();
            }
            // Preserve a supplied complete URL, including its path, query and fragment.
            if (complete) return uri.toASCIIString();
            String authority = uri.getRawAuthority();
            if (uri.getPort() == -1) authority += ":" + DEFAULT_PORT;
            String path = uri.getRawPath();
            if (path == null || path.isEmpty() || path.equals("/")) path = DEFAULT_PATH;
            return new URI("http://" + authority + path +
                    (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery()) +
                    (uri.getRawFragment() == null ? "" : "#" + uri.getRawFragment())).toASCIIString();
        } catch (URISyntaxException invalid) {
            throw new IllegalArgumentException(invalid);
        }
    }

    static boolean isWebAddress(String address) {
        if (address == null || !address.contains("://")) return false;
        try {
            normalize(address);
            return true;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }
}
