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
    @Test public void findsSpresenterStageRedirectAndKeepsStableOutputAddress() throws Exception {
        String stage = "/slide/?output=stage&outputId=1788226692937-vr912n&" +
                "stageThemeGuid=20827a02-e21a-489f-839e-66b3fec90794&id=Sa%C3%ADda%20HTML%201";
        for (String location : new String[]{stage, server.url(stage).toString()}) {
            server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", location));
            response(200, "text/html; charset=utf-8", "<html><title>Slide do apresentador</title></html>");
            HtmlSource source = probe("/output/saida-html-1");
            assertNotNull(source);
            assertEquals(server.url("/output/saida-html-1").toString(), source.url);
            assertEquals("Slide do apresentador", source.title);
            assertEquals("/output/saida-html-1", server.takeRequest().getPath());
            assertEquals(stage, server.takeRequest().getPath());
        }
    }
    @Test public void followsStageRedirectAfterOutputSlashNormalization() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(301).setHeader("Location", "/output/saida-html-1/"));
        server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "/slide/?output=stage&id=Saida"));
        response(200, "text/html", "<html><title>Retorno</title></html>");
        assertNotNull(probe("/output/saida-html-1"));
        assertEquals(3, server.getRequestCount());
    }
    @Test public void refusesStageRedirectToOtherOriginsOrWithoutUniqueStageSelector() throws Exception {
        String[] locations = {
            "/slide/?output=live", "/slide/?id=retorno", "/slide/?output=stage&output=live",
            "/slide/?output=stage&Output=live", "/api/control?output=stage", "/login?output=stage",
            "http://example.invalid/slide/?output=stage", "http://localhost:1/slide/?output=stage",
            "https://localhost:" + server.getPort() + "/slide/?output=stage",
            "http://user@localhost:" + server.getPort() + "/slide/?output=stage"
        };
        for (String location : locations) {
            server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", location));
            assertNull(location, probe("/output/saida-html-1"));
        }
        assertEquals(locations.length, server.getRequestCount());
    }
    @Test public void neverMovesSavedCustomParametersToTheStageRoute() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "/slide/?output=stage"));
        assertNull(probe("/output/saida-html-1?token=private"));
        server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "/slide/?output=stage"));
        assertNull(probe("/custom-output"));
        assertEquals(2, server.getRequestCount());
    }
    @Test public void capsRedirectLoopsWithoutExecutingTheDestinationPage() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(301).setHeader("Location", "/output/saida-html-1/"));
        server.enqueue(new MockResponse().setResponseCode(301).setHeader("Location", "/output/saida-html-1"));
        server.enqueue(new MockResponse().setResponseCode(301).setHeader("Location", "/output/saida-html-1/"));
        assertNull(probe("/output/saida-html-1"));
        assertEquals(3, server.getRequestCount());
    }
    @Test public void rejectsNonHtmlReturnedByTheStageRedirect() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "/slide/?output=stage"));
        response(200, "application/json", "{\"ok\":true}");
        assertNull(probe("/output/saida-html-1"));
        assertEquals(2, server.getRequestCount());
    }
    @Test public void findsNamedRetonoAfterMissingDefaultRoutes() throws Exception {
        response(404, "text/html", "<html>Missing</html>");
        response(404, "text/html", "<html>Missing</html>");
        server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "/slide/?output=stage&id=Retono"));
        response(200, "text/html", "<html><title>Retono</title></html>");
        HtmlSource source = new HtmlHttpProbe().probe(new HtmlHttpProbe.Target(
                server.url("/output/saida-html-1").toString(),
                url -> (HttpURLConnection) url.openConnection(Proxy.NO_PROXY),
                server.url("/output/retorno").toString(), server.url("/output/retono").toString()),
                new HtmlDiscoverySession.Cancellation());
        assertNotNull(source);
        assertEquals(server.url("/output/retono").toString(), source.url);
        for (String expected : new String[]{"/output/saida-html-1", "/output/retorno", "/output/retono",
                "/slide/?output=stage&id=Retono"}) assertEquals(expected, server.takeRequest().getPath());
    }
    @Test public void noExtraNamesAreRequestedFromAnUnreachableServer() throws Exception {
        AtomicInteger opened = new AtomicInteger();
        try {
            new HtmlHttpProbe().probe(new HtmlHttpProbe.Target(server.url("/output/saida-html-1").toString(),
                    url -> { opened.incrementAndGet(); throw new java.net.ConnectException("Unreachable"); },
                    server.url("/output/retono").toString()), new HtmlDiscoverySession.Cancellation());
            fail("Expected connection failure");
        } catch (java.net.ConnectException expected) { }
        assertEquals(1, opened.get());
    }
    @Test public void alternativeOutputNeverChangesServer() throws Exception {
        response(404, "text/html", "<html>Missing</html>");
        assertNull(new HtmlHttpProbe().probe(new HtmlHttpProbe.Target(server.url("/output/saida-html-1").toString(),
                url -> (HttpURLConnection) url.openConnection(Proxy.NO_PROXY),
                "http://example.invalid/output/retono"), new HtmlDiscoverySession.Cancellation()));
        assertEquals(1, server.getRequestCount());
    }
    @Test public void stopsAtFirstValidOutputWithoutTryingOtherNames() throws Exception {
        response(200, "text/html", "<html><title>Stage</title></html>");
        HtmlSource source = new HtmlHttpProbe().probe(new HtmlHttpProbe.Target(
                server.url("/output/saida-html-1").toString(),
                url -> (HttpURLConnection) url.openConnection(Proxy.NO_PROXY),
                server.url("/output/retono").toString()), new HtmlDiscoverySession.Cancellation());
        assertNotNull(source);
        assertEquals(server.url("/output/saida-html-1").toString(), source.url);
        assertEquals(1, server.getRequestCount());
    }
    @Test public void namedOutputCanOpenItsGeneratedStageRoute() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "/slide/?output=stage&id=Retono"));
        response(200, "text/html", "<html><title>Retono</title></html>");
        assertEquals(server.url("/output/retono").toString(), probe("/output/retono").url);
        assertEquals(2, server.getRequestCount());
    }
    @Test public void unauthorizedRouteDoesNotStartTryingOtherNames() throws Exception {
        response(401, "text/html", "<html>Login</html>");
        assertNull(new HtmlHttpProbe().probe(new HtmlHttpProbe.Target(server.url("/output/saida-html-1").toString(),
                url -> (HttpURLConnection) url.openConnection(Proxy.NO_PROXY),
                server.url("/output/retono").toString()), new HtmlDiscoverySession.Cancellation()));
        assertEquals(1, server.getRequestCount());
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
