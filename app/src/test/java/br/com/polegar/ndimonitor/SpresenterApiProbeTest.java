package br.com.polegar.ndimonitor;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class SpresenterApiProbeTest {
    private static final String ROOT = "http://192.168.2.53:5050";
    private static final String KEY = "spk_example_read_only_token";
    private static final String CATALOG = "{\"config\":{\"outputs\":[" +
            "{\"id\":\"n1\",\"type\":\"network\",\"slug\":\"madureira\",\"name\":\"Público\",\"canvasKey\":0}," +
            "{\"id\":\"n2\",\"type\":\"network\",\"slug\":\"canvas-2\",\"name\":\"\",\"canvasKey\":1}," +
            "{\"id\":\"n3\",\"type\":\"network\",\"slug\":\"retorno\",\"canvasKey\":\"stage\"}," +
            "{\"type\":\"network\",\"slug\":\"desativada\",\"enabled\":false}," +
            "{\"type\":\"ndi\",\"slug\":\"nao-e-html\"}," +
            "{\"type\":\"network\",\"slug\":\"../api/control\"}," +
            "{\"type\":\"network\",\"slug\":\"https://example.invalid/\"}," +
            "{\"type\":\"network\",\"slug\":\"madureira\",\"name\":\"duplicada\"}" +
            "]},\"virtualOutputs\":[{\"name\":\"Público\"},{\"name\":\"Retorno Músicos\"}]}";

    @Test public void discoversArbitraryPublishedNamesAndAllDestinationsOfOnePc() throws Exception {
        List<HtmlSource> list = SpresenterApiProbe.parse(ROOT, CATALOG);
        assertEquals(3, list.size());
        assertEquals(ROOT + "/output/madureira", list.get(0).url);
        assertEquals("Público", list.get(0).title);
        assertEquals(ROOT + "/output/canvas-2", list.get(1).url);
        assertEquals("Retorno Músicos", list.get(1).title);
        assertEquals("Retorno (Stage)", list.get(2).title);
        for (HtmlSource source : list) assertEquals(ROOT, source.catalogOrigin);
    }

    @Test public void emptyAuthoritativeCatalogIsValidAndMissingOrOversizedCatalogIsRejected() throws Exception {
        assertTrue(SpresenterApiProbe.parse(ROOT, "{\"config\":{\"outputs\":[]}}").isEmpty());
        for (String bad : new String[]{"{}", "[]", "<html></html>", "{\"config\":{\"outputs\":null}}",
                "{\"config\":{\"outputs\":[" + "{},".repeat(SpresenterApiProbe.MAX_OUTPUTS) + "{}]}}"}) {
            try { SpresenterApiProbe.parse(ROOT, bad); fail(bad); }
            catch (SpresenterApiProbe.Failure expected) { assertEquals(R.string.spresenter_api_invalid, expected.message); }
        }
    }

    @Test public void missingNamesUseSlugAndNonStringSlugsNeverBecomePhantomSources() throws Exception {
        List<HtmlSource> list=SpresenterApiProbe.parse(ROOT,"{\"config\":{\"outputs\":["+
                "{\"type\":\"network\",\"slug\":null},{\"type\":\"network\",\"slug\":123},"+
                "{\"type\":\"network\",\"slug\":\"palco-novo\",\"name\":null}]}}");
        assertEquals(1,list.size());assertEquals("palco-novo",list.get(0).title);
    }

    @Test public void usesOnlyGetAndKeepsKeyInHeaderOfTheConfiguredOrigin() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(CATALOG));
            SpresenterConnection config = new SpresenterConnection(server.url("/api/v1").toString(), KEY);
            List<HtmlSource> list = new SpresenterApiProbe().read(config,
                    u -> (HttpURLConnection) u.openConnection(Proxy.NO_PROXY), new HtmlDiscoverySession.Cancellation());
            RecordedRequest request = server.takeRequest();
            assertEquals("GET", request.getMethod());
            assertEquals("/api/v1/advanced", request.getPath());
            assertEquals("Bearer " + KEY, request.getHeader("Authorization"));
            assertEquals(0, request.getBodySize());
            assertEquals(3, list.size());
            assertFalse(list.get(0).displayName().contains(KEY));
            assertFalse(list.get(0).url.contains(KEY));
            assertTrue(list.get(0).url.startsWith(config.origin + "/output/"));
        }
    }

    @Test public void refusesRedirectsAndAuthFailuresWithoutFollowingOrDisclosingKey() throws Exception {
        try (MockWebServer server = new MockWebServer(); MockWebServer other = new MockWebServer()) {
            server.start(); other.start();
            SpresenterConnection config = new SpresenterConnection(server.url("/").toString(), KEY);
            int[] codes = {302, 401, 403, 404};
            int[] messages = {R.string.spresenter_api_unavailable, R.string.spresenter_api_unauthorized,
                    R.string.spresenter_api_forbidden, R.string.spresenter_api_unavailable};
            for (int i = 0; i < codes.length; i++) {
                server.enqueue(new MockResponse().setResponseCode(codes[i]).setHeader("Location", other.url("/sink")));
                try {
                    new SpresenterApiProbe().read(config, u -> (HttpURLConnection) u.openConnection(Proxy.NO_PROXY),
                            new HtmlDiscoverySession.Cancellation()); fail();
                } catch (SpresenterApiProbe.Failure expected) {
                    assertEquals(messages[i], expected.message);
                    assertFalse(expected.toString().contains(KEY));
                }
            }
            assertEquals(4, server.getRequestCount());
            assertEquals(0, other.getRequestCount());
        }
    }

    @Test public void rejectsHtmlWrongShapeAndOversizedJson() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setHeader("Content-Type", "text/html").setBody("<html>Login</html>"));
            server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("{}"));
            server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                    .setChunkedBody("x".repeat(SpresenterApiProbe.MAX_BYTES + 1), 8192));
            SpresenterConnection config = new SpresenterConnection(server.url("/").toString(), KEY);
            for (int i = 0; i < 3; i++) {
                try {
                    new SpresenterApiProbe().read(config, u -> (HttpURLConnection) u.openConnection(Proxy.NO_PROXY),
                            new HtmlDiscoverySession.Cancellation()); fail();
                } catch (SpresenterApiProbe.Failure expected) { assertEquals(R.string.spresenter_api_invalid, expected.message); }
            }
        }
    }

    @Test public void cancellationBeforeOpeningDoesNotSendKeyOrOpenSocket() throws Exception {
        AtomicInteger opened = new AtomicInteger();
        HtmlDiscoverySession.Cancellation cancellation = new HtmlDiscoverySession.Cancellation();
        cancellation.cancel();
        try {
            new SpresenterApiProbe().read(new SpresenterConnection(ROOT, KEY), u -> { opened.incrementAndGet(); return null; },
                    cancellation); fail();
        } catch (SpresenterApiProbe.Failure expected) { assertEquals(R.string.spresenter_api_cancelled, expected.message); }
        assertEquals(0, opened.get());
    }

    @Test public void acceptsOnlyAnExplicitApiOriginAndRejectsTokenInjection() {
        assertEquals(ROOT, new SpresenterConnection("192.168.2.53", KEY).origin);
        assertEquals(ROOT, new SpresenterConnection(ROOT + "/api/v1/", KEY).origin);
        SpresenterConnection config = new SpresenterConnection(ROOT, KEY);
        assertTrue(config.owns(ROOT + "/output/retorno"));
        for (String address : new String[]{"http://192.168.2.54:5050", "https://192.168.2.53:5050",
                "http://192.168.2.53", "http://user@192.168.2.53:5050"}) assertFalse(config.owns(address));
        for (String address : new String[]{ROOT+"/output/retorno", ROOT+"/api/v1?token=secret", ROOT+"/#secret",
                "file:///private", "http://user@192.168.2.53:5050", "http://192.168.2.53:0", ""}) {
            try { new SpresenterConnection(address, KEY); fail(address); } catch (IllegalArgumentException expected) { }
        }
        for (String key : new String[]{"", "spk_x", KEY+"\r\nX-Other: leak", KEY+" space"}) {
            try { new SpresenterConnection(ROOT,key); fail(); } catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void apiPrefersTheLanInterfaceThatContainsTheConfiguredPc() throws Exception {
        android.net.LinkProperties wifi=new android.net.LinkProperties();
        addAddress(wifi,"10.0.0.5/24");
        android.net.LinkProperties ethernet=new android.net.LinkProperties();
        addAddress(ethernet,"192.168.2.9/24");
        assertFalse(LanHtmlDiscovery.matchesApiSubnet(wifi,ROOT));
        assertTrue(LanHtmlDiscovery.matchesApiSubnet(ethernet,ROOT));
    }

    private void addAddress(android.net.LinkProperties link,String cidr) throws Exception {
        String[] parts=cidr.split("/",2);
        android.net.LinkAddress address=org.robolectric.util.ReflectionHelpers.callConstructor(android.net.LinkAddress.class,
                org.robolectric.util.ReflectionHelpers.ClassParameter.from(java.net.InetAddress.class,
                        java.net.InetAddress.getByName(parts[0])),
                org.robolectric.util.ReflectionHelpers.ClassParameter.from(int.class,Integer.parseInt(parts[1])));
        org.robolectric.util.ReflectionHelpers.callInstanceMethod(link,"addLinkAddress",
                org.robolectric.util.ReflectionHelpers.ClassParameter.from(android.net.LinkAddress.class,address));
    }
}
