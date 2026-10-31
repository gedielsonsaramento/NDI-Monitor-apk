package br.com.polegar.ndimonitor;

import android.app.Application;
import android.content.Context;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Looper;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 25}, application = Application.class,
        shadows = {SpresenterSetupNetworkTest.Connections.class, LanMultiPcDiscoveryTest.Requests.class})
public class LanMultiPcDiscoveryTest {
    private static final String A = "http://192.168.2.53:5050", B = "http://10.20.80.10:5050";
    private static final String KEY_A = "spk_example_lan_pc_a", KEY_B = "spk_example_lan_pc_b";
    private Context context;
    private SpresenterApiSettings settings;

    @Implements(Network.class)
    public static class Requests {
        static final Map<String, MockWebServer> servers = new ConcurrentHashMap<>();
        static final Set<String> publicHosts = ConcurrentHashMap.newKeySet();
        static final AtomicBoolean publicCredential = new AtomicBoolean();
        @Implementation protected URLConnection openConnection(URL url, Proxy proxy) throws java.io.IOException {
            MockWebServer server = servers.get(url.getHost());
            if (url.getPath().equals("/api/v1/advanced") && server != null) {
                return server.url(url.getFile()).url().openConnection();
            }
            return new HttpURLConnection(url) {
                @Override public int getResponseCode() {
                    publicHosts.add(url.getHost());
                    if (getRequestProperty("Authorization") != null) publicCredential.set(true);
                    return 404;
                }
                @Override public void connect() { }
                @Override public void disconnect() { }
                @Override public boolean usingProxy() { return false; }
            };
        }
    }

    @Before public void prepare() throws Exception {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit();
        SpresenterSetupNetworkTest.Connections.active = null;
        SpresenterSetupNetworkTest.Connections.capabilities.clear();
        SpresenterSetupNetworkTest.Connections.links.clear();
        Requests.servers.clear(); Requests.publicHosts.clear(); Requests.publicCredential.set(false);
        Network wifi = ReflectionHelpers.callConstructor(Network.class, ClassParameter.from(int.class, 2));
        NetworkCapabilities caps = new NetworkCapabilities();
        ReflectionHelpers.callInstanceMethod(caps, "addTransportType",
                ClassParameter.from(int.class, NetworkCapabilities.TRANSPORT_WIFI));
        LinkProperties link = new LinkProperties();
        LinkAddress address = ReflectionHelpers.callConstructor(LinkAddress.class,
                ClassParameter.from(InetAddress.class, InetAddress.getByName("192.168.2.20")), ClassParameter.from(int.class, 24));
        ReflectionHelpers.callInstanceMethod(link, "addLinkAddress", ClassParameter.from(LinkAddress.class, address));
        SpresenterSetupNetworkTest.Connections.capabilities.put(wifi, caps);
        SpresenterSetupNetworkTest.Connections.links.put(wifi, link);
        SpresenterSetupNetworkTest.Connections.active = wifi;
        KeyGenerator generator = KeyGenerator.getInstance("AES"); generator.init(128);
        SecretKey secret = generator.generateKey();
        settings = new SpresenterApiSettings(context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE),
                context.getPackageName(), new SpresenterTokenCipher(() -> secret));
    }

    @Test public void bothRegisteredPcsAreQueriedEvenOutsideTheDeviceSubnet() throws Exception {
        try (MockWebServer first = new MockWebServer(); MockWebServer second = new MockWebServer()) {
            first.start(); second.start();
            first.enqueue(catalog()); second.enqueue(catalog());
            Requests.servers.put("192.168.2.53", first); Requests.servers.put("10.20.80.10", second);
            settings.save(new SpresenterConnection(A, KEY_A)); settings.save(new SpresenterConnection(B, KEY_B));
            Scan scan = scan("");
            assertEquals(2, scan.catalogs.size());
            assertEquals(A + "/output/musicos", scan.catalogs.get(A).get(0).url);
            assertEquals(B + "/output/musicos", scan.catalogs.get(B).get(0).url);
            assertEquals("Bearer " + KEY_A, first.takeRequest(2, TimeUnit.SECONDS).getHeader("Authorization"));
            assertEquals("Bearer " + KEY_B, second.takeRequest(2, TimeUnit.SECONDS).getHeader("Authorization"));
            assertFalse(Requests.publicCredential.get());
        }
    }

    @Test public void explicitExtraRangeIsScannedWithoutSendingTheConfiguredPcKey() throws Exception {
        try (MockWebServer first = new MockWebServer()) {
            first.start(); first.enqueue(catalog()); Requests.servers.put("192.168.2.53", first);
            settings.save(new SpresenterConnection(A, KEY_A));
            context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE).edit()
                    .putString(HtmlSearchSettings.EXTRA_PREFIX, "10.20.80").commit();
            Scan scan = scan("http://10.20.90.5:5050/output/musicos?screen=1");
            assertEquals(1, scan.catalogs.size());
            assertTrue(Requests.publicHosts.contains("10.20.80.77"));
            assertTrue(Requests.publicHosts.contains("10.20.90.5"));
            assertFalse(Requests.publicCredential.get());
            assertTrue(scan.targets <= LanHtmlDiscovery.MAX_TARGETS);
        }
    }

    private Scan scan(String saved) throws Exception {
        return scan(saved, false);
    }

    @Test public void automaticSearchIgnoresOldCatalogCredentialsAndProbesTheirPublicAddress() throws Exception {
        settings.save(new SpresenterConnection(A, KEY_A));
        settings.save(new SpresenterConnection(B, KEY_B));
        Scan scan = scan(A + "/output/musicos", true);
        assertTrue(scan.catalogs.isEmpty());
        assertTrue(Requests.publicHosts.contains("192.168.2.53"));
        assertTrue(Requests.publicHosts.contains("192.168.2.77"));
        assertFalse(Requests.publicCredential.get());
    }

    private Scan scan(String saved, boolean publicOnly) throws Exception {
        LanHtmlDiscovery discovery = new LanHtmlDiscovery(context);
        Scan scan = new Scan();
        try {
            if (publicOnly) discovery.startPublic(saved, scan);
            else discovery.start(saved, scan);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
            while (!scan.finished && System.nanoTime() < deadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10);
            }
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertTrue("Discovery did not finish", scan.finished);
            assertFalse("Local network was not available", scan.unavailable);
            return scan;
        } finally { discovery.stop(); }
    }

    private static final class Scan implements LanHtmlDiscovery.Listener {
        final Map<String, List<HtmlSource>> catalogs = new HashMap<>();
        boolean finished, unavailable;
        int targets;
        @Override public void onStarted(int count, boolean limited) { targets = count; }
        @Override public void onSource(HtmlSource source) { }
        @Override public void onProgress(int completed, int total) { }
        @Override public void onFinished() { finished = true; }
        @Override public void onUnavailable() { unavailable = true; finished = true; }
        @Override public void onCatalog(String origin, List<HtmlSource> sources) { catalogs.put(origin, sources); }
    }

    private static MockResponse catalog() {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(
                "{\"config\":{\"outputs\":[{\"type\":\"network\",\"slug\":\"musicos\",\"name\":\"Músicos\"}]}}");
    }
}
