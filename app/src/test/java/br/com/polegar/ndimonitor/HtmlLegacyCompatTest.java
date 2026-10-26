package br.com.polegar.ndimonitor;

import android.webkit.WebResourceResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class HtmlLegacyCompatTest {
    private static final String PAGE = "http://192.168.2.52:5050/output/retorno";
    private static final String SCRIPT = "http://192.168.2.52:5050/static/js/main.test.js";
    private static final String AGENT = "Mozilla/5.0 Chrome/52.0.2743.100 Safari/537.36";
    private static final byte[] ORIGINAL = bytes("captured original");
    private static final byte[] ADAPTED = bytes("compatible live program");

    private static byte[] bytes(String s) { return s.getBytes(StandardCharsets.UTF_8); }
    private static Map<String, byte[]> assets() throws Exception {
        JSONObject r = new JSONObject().put("name", "main.test.js").put("mime", "application/javascript")
                .put("sourceSha256", HtmlLegacyCompat.sha256(ORIGINAL))
                .put("asset", "html-compat/test.js").put("assetSha256", HtmlLegacyCompat.sha256(ADAPTED));
        JSONObject manifest = new JSONObject().put("schema", 1).put("minChrome", 52).put("maxChrome", 86)
                .put("resources", new JSONArray().put(r));
        Map<String, byte[]> data = new HashMap<>();
        data.put("html-compat/manifest.json", bytes(manifest.toString()));
        data.put("html-compat/test.js", ADAPTED);
        return data;
    }
    private static HtmlLegacyCompat.Assets source(Map<String, byte[]> data) {
        return p -> { if (!data.containsKey(p)) throw new IOException("absent"); return data.get(p); };
    }
    private static HtmlLegacyCompat compat(Map<String, byte[]> data, HtmlLegacyCompat.Fetcher fetcher) {
        return new HtmlLegacyCompat(source(data), fetcher, AGENT, PAGE);
    }
    private static WebResourceResponse get(HtmlLegacyCompat c, String address) {
        return c.intercept("GET", address, Collections.emptyMap());
    }

    @Test public void appliesAdaptationOnlyAfterVerifyingBothHashes() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HtmlLegacyCompat c = compat(assets(), (uri, h) -> { requests.incrementAndGet(); return ORIGINAL; });
        WebResourceResponse r = get(c, SCRIPT);
        assertNotNull(r);
        assertArrayEquals(ADAPTED, HtmlLegacyCompat.readBounded(r.getData()));
        assertEquals("application/javascript", r.getMimeType());
        assertEquals("no-store", r.getResponseHeaders().get("Cache-Control"));
        assertEquals(1, requests.get());
        assertTrue(c.describe().contains("1 de 1"));
    }

    @Test public void modernOrUnknownEnginesNeverReadAssetsOrFetch() {
        for (String agent : new String[]{"Chrome/87.0", "Chrome/156.0", "Chrome/51.0", "unknown", null}) {
            HtmlLegacyCompat c = new HtmlLegacyCompat(p -> { fail("asset read"); return null; },
                    (u, h) -> { fail("network request"); return null; }, agent, PAGE);
            assertNull(get(c, SCRIPT));
        }
    }

    @Test public void neverInterceptsOtherHostsPortsMethodsOrUnrecognizedFiles() throws Exception {
        HtmlLegacyCompat c = compat(assets(), (u, h) -> { fail("unexpected network"); return null; });
        for (String url : new String[]{"http://other:5050/static/js/main.test.js",
                "http://192.168.2.52:5051/main.test.js", "https://192.168.2.52:5050/main.test.js",
                "http://u:p@192.168.2.52:5050/main.test.js", PAGE, "file:///main.test.js",
                "http://192.168.2.52:5050/assets/video.mp4", "http://192.168.2.52:5050/main.other.js"}) {
            assertNull(get(c, url));
        }
        assertNull(c.intercept("POST", SCRIPT, Collections.emptyMap()));
    }

    @Test public void changedProgramIsNeverReplacedWithAnOlderCapture() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HtmlLegacyCompat c = compat(assets(), (u, h) -> { requests.incrementAndGet(); return bytes("different program"); });
        assertNull(get(c, SCRIPT));
        assertNull(get(c, SCRIPT));
        assertEquals(1, requests.get());
        assertTrue(c.describe().contains("diferente da captura"));
    }

    @Test public void cacheIsScopedToTheCompleteVerifiedResourceUrl() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HtmlLegacyCompat c = compat(assets(), (u, h) -> { requests.incrementAndGet(); return ORIGINAL; });
        assertNotNull(get(c, SCRIPT));
        assertNotNull(get(c, SCRIPT));
        assertNotNull(get(c, SCRIPT + "?different=1"));
        assertEquals(2, requests.get());
    }

    @Test public void capsNativeVerificationAtFourRequestsPerRenderer() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HtmlLegacyCompat c = compat(assets(), (u, h) -> { requests.incrementAndGet(); return ORIGINAL; });
        for (int i = 0; i < 8; ++i) {
            WebResourceResponse result = get(c, SCRIPT + "?v=" + i);
            if (i < 4) assertNotNull(result); else assertNull(result);
        }
        assertEquals(4, requests.get());
    }

    @Test public void corruptedLocalProgramIsRejectedBeforeContactingTheServer() throws Exception {
        Map<String, byte[]> files = assets(); files.put("html-compat/test.js", bytes("damaged"));
        HtmlLegacyCompat c = compat(files, (u, h) -> { fail("network"); return null; });
        assertNull(get(c, SCRIPT));
    }

    @Test public void missingOrInvalidManifestKeepsTheOriginalPage() {
        for (Map<String, byte[]> files : java.util.Arrays.asList(Collections.<String,byte[]>emptyMap(),
                Collections.singletonMap("html-compat/manifest.json", bytes("invalid json")))) {
            HtmlLegacyCompat c = compat(files, (u, h) -> { fail("network"); return null; });
            assertNull(get(c, SCRIPT));
        }
    }

    @Test public void manifestCannotReferenceFilesOutsideCompatibilityAssets() throws Exception {
        Map<String, byte[]> files = assets();
        JSONObject manifest = new JSONObject(new String(files.get("html-compat/manifest.json"), StandardCharsets.UTF_8));
        manifest.getJSONArray("resources").getJSONObject(0).put("asset", "html-compat/../private.js");
        files.put("html-compat/manifest.json", bytes(manifest.toString()));
        assertNull(get(compat(files, (u, h) -> { fail("network"); return null; }), SCRIPT));
    }

    @Test public void stoppedRendererCannotBeginANewRequest() throws Exception {
        HtmlLegacyCompat c = compat(assets(), (u, h) -> { fail("network"); return null; });
        c.close(); assertNull(get(c, SCRIPT));
    }

    @Test public void stoppingNeverWaitsForAnInFlightNetworkRead() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicReference<WebResourceResponse> result = new AtomicReference<>();
        HtmlLegacyCompat c = compat(assets(), (u, h) -> {
            entered.countDown();
            try { if (!release.await(3, TimeUnit.SECONDS)) throw new IOException("timeout"); }
            catch (InterruptedException e) { throw new IOException(e); }
            return ORIGINAL;
        });
        Thread worker = new Thread(() -> result.set(get(c, SCRIPT))); worker.start();
        try {
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            long start = System.nanoTime(); c.close();
            assertTrue("close blocked the UI", System.nanoTime() - start < TimeUnit.MILLISECONDS.toNanos(200));
        } finally { release.countDown(); worker.join(3000); }
        assertFalse(worker.isAlive()); assertNull(result.get());
    }

    @Test public void responseReadersRejectOversizedResources() throws Exception {
        try {
            HtmlLegacyCompat.readBounded(new ByteArrayInputStream(new byte[HtmlLegacyCompat.MAX_BYTES + 1]));
            fail("oversized resource accepted");
        } catch (IOException expected) { assertTrue(expected.getMessage().contains("grande")); }
    }

    @Test public void nativeVerifierDoesNotFollowRedirects() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(302).addHeader("Location", "http://other.invalid/main.test.js"));
            server.start();
            try { HtmlLegacyCompat.download(URI.create(server.url("/main.test.js").toString()), Collections.emptyMap());
                fail("redirect accepted"); } catch (IOException expected) { assertEquals(1, server.getRequestCount()); }
        }
    }

    @Test public void nativeVerifierDoesNotForwardCredentialsOrCompressionFraming() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("captured original")); server.start();
            Map<String, String> headers = new HashMap<>(); headers.put("Cookie", "secret=1");
            headers.put("Authorization", "private"); headers.put("Accept-Encoding", "gzip"); headers.put("User-Agent", AGENT);
            assertArrayEquals(ORIGINAL, HtmlLegacyCompat.download(URI.create(server.url("/main.test.js").toString()), headers));
            RecordedRequest r = server.takeRequest(2, TimeUnit.SECONDS); assertNotNull(r);
            assertNull(r.getHeader("Cookie")); assertNull(r.getHeader("Authorization"));
            assertEquals("identity", r.getHeader("Accept-Encoding")); assertEquals(AGENT, r.getHeader("User-Agent"));
        }
    }
}
