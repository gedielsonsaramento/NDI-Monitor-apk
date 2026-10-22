package br.com.polegar.ndimonitor;

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class HtmlHttpProbeTest {
    private MockWebServer server;
    @Before public void start() throws Exception { server = new MockWebServer(); server.start(); }
    @After public void stop() throws Exception { server.shutdown(); }
    private void response(int status, String type, String body) {
        server.enqueue(new MockResponse().setResponseCode(status).setHeader("Content-Type", type).setBody(body));
    }
    private HtmlSource probe(String path) throws Exception {
        return new HtmlHttpProbe().probe(new HtmlHttpProbe.Target(server.url(path).toString(),
                url -> (HttpURLConnection) url.openConnection(Proxy.NO_PROXY)), new HtmlDiscoverySession.Cancellation());
    }
    @Test public void findsReachableHtmlAndReadsTitleWithoutExecutingScripts() throws Exception {
        response(200, "text/html; charset=utf-8",
                "<!doctype html><html><head><title>Retorno &amp; Louvor</title></head><script>bad()</script></html>");
        HtmlSource source = probe("/output/saida-html-1");
        assertNotNull(source);
        assertEquals("Retorno & Louvor", source.title);
        assertEquals(server.url("/output/saida-html-1").toString(), source.url);
        assertEquals("/output/saida-html-1", server.takeRequest().getPath());
        assertEquals(1, server.getRequestCount());
    }
    @Test public void rejectsMissingRoutesUnauthorizedJsonAndHtmlMimeWithoutHtml() throws Exception {
        response(404, "text/html", "<html>not found</html>");
        response(401, "text/html", "<html>login</html>");
        response(200, "application/json", "{\"ok\":true}");
        response(200, "text/html", "ok");
        for (String path : new String[]{"/missing", "/private", "/api", "/plain"}) assertNull(path, probe(path));
    }
    @Test public void neverFollowsRedirectToAnotherPathOrHost() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", server.url("/login")));
        server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "http://example.invalid/page"));
        assertNull(probe("/output/private"));
        assertNull(probe("/outside"));
        assertEquals(2, server.getRequestCount());
    }
    @Test public void permitsSingleTrailingSlashRedirectAtSameEndpoint() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(301).setHeader("Location", "/output/slash/"));
        response(200, "text/html", "<html><title>Stage</title></html>");
        assertNotNull(probe("/output/slash"));
        assertEquals(2, server.getRequestCount());
    }
    @Test public void cancelledProbeNeverOpensConnection() throws Exception {
        HtmlDiscoverySession.Cancellation cancellation = new HtmlDiscoverySession.Cancellation();
        cancellation.cancel();
        AtomicInteger opened = new AtomicInteger();
        assertNull(new HtmlHttpProbe().probe(new HtmlHttpProbe.Target(server.url("/anything").toString(), url -> {
            opened.incrementAndGet(); return (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        }), cancellation));
        assertEquals(0, opened.get());
    }
    @Test public void capsResponseReadEvenForOversizedHtml() throws Exception {
        response(200, "text/html", "<html><title>Stage</title>" + "x".repeat(HtmlHttpProbe.MAX_BYTES * 2) + "</html>");
        assertEquals("Stage", probe("/large").title);
    }
    @Test public void slowResponseDoesNotHoldDiscoveryIndefinitely() throws Exception {
        server.enqueue(new MockResponse().setHeader("Content-Type", "text/html")
                .setBody("<html><title>Slow</title></html>").setBodyDelay(2, TimeUnit.SECONDS));
        long start = System.nanoTime();
        try { probe("/slow"); fail("Expected read timeout"); }
        catch (java.io.IOException expected) { }
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 1900);
    }
}
