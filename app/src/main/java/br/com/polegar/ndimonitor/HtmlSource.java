package br.com.polegar.ndimonitor;

import java.net.URI;

/** A reachable HTML endpoint; discovery does not execute or certify its contents. */
final class HtmlSource {
    final String url;
    final String title;
    final String catalogOrigin;

    HtmlSource(String url, String title) {
        this(url, title, "");
    }

    HtmlSource(String url, String title, String catalogOrigin) {
        this.url = HtmlSourceUrl.normalize(url);
        this.title = title == null || title.trim().isEmpty() ? "Retorno HTML" : title.trim();
        this.catalogOrigin = catalogOrigin;
    }

    String displayName() {
        URI uri = URI.create(url);
        String host = uri.getHost() + (uri.getPort() < 0 ? "" : ":" + uri.getPort());
        // Query strings can contain tokens: keep them out of list labels.
        return title + "\n" + host + (uri.getPath() == null ? "" : uri.getPath());
    }
}
