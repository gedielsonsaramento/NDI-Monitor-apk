package br.com.polegar.ndimonitor;

import android.content.Context;
import android.content.SharedPreferences;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 25})
public class MultiPcCatalogTest {
    private static final String A = "http://192.168.2.53:5050", B = "http://10.20.80.10:5050";
    private static final String KEY_A = "spk_example_multi_pc_a", KEY_B = "spk_example_multi_pc_b";
    private SharedPreferences prefs;
    private SpresenterTokenCipher cipher;

    @Before public void prepare() throws Exception {
        prefs = RuntimeEnvironment.getApplication().getSharedPreferences("multi-pc", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(128);
        SecretKey key = generator.generateKey();
        cipher = new SpresenterTokenCipher(() -> key);
    }

    @Test public void legacyPcMigratesAndTwoIndependentKeysSurviveProcessRestart() throws Exception {
        prefs.edit().putString(SpresenterApiSettings.ORIGIN, A)
                .putString(SpresenterApiSettings.SECRET, cipher.encrypt(A, KEY_A)).commit();
        SpresenterApiSettings settings = new SpresenterApiSettings(prefs, "multi.first", cipher);
        assertEquals(KEY_A, settings.load().token);
        assertTrue(settings.save(new SpresenterConnection(B, KEY_B)));
        SpresenterApiSettings restarted = new SpresenterApiSettings(prefs, "multi.next-process", cipher);
        assertEquals(2, restarted.loadAll().size());
        assertEquals(KEY_A, restarted.load(A).token);
        assertEquals(KEY_B, restarted.load(B).token);
        assertFalse(prefs.getAll().toString().contains(KEY_A));
        assertFalse(prefs.getAll().toString().contains(KEY_B));
    }

    @Test public void updatingAndRemovingOnePcPreservesTheOtherAndSelectedPlayback() {
        SpresenterApiSettings settings = new SpresenterApiSettings(prefs, "multi.update", cipher);
        prefs.edit().putString(MainActivity.KEY_HTML_URL, A + "/output/musicos").commit();
        settings.save(new SpresenterConnection(A, KEY_A));
        settings.save(new SpresenterConnection(B, KEY_B));
        settings.save(new SpresenterConnection(A, "spk_example_new_key_for_pc_a"));
        assertEquals(2, settings.loadAll().size());
        assertEquals(KEY_B, settings.load(B).token);
        settings.remove(A);
        assertNull(settings.load(A));
        assertEquals(B, settings.origin());
        assertEquals(KEY_B, settings.load().token);
        assertEquals(A + "/output/musicos", prefs.getString(MainActivity.KEY_HTML_URL, ""));
        SpresenterApiSettings restarted = new SpresenterApiSettings(prefs, "multi.after-remove", cipher);
        assertEquals(1, restarted.loadAll().size());
        assertEquals(KEY_B, restarted.load(B).token);
    }

    @Test public void unsupportedKeystoreRetainsBothKeysOnlyForTheCurrentSession() {
        SpresenterTokenCipher unavailable = new SpresenterTokenCipher(() -> {
            throw new java.security.GeneralSecurityException("Unavailable in fixture");
        });
        SpresenterApiSettings settings = new SpresenterApiSettings(prefs, "multi.session", unavailable);
        assertFalse(settings.save(new SpresenterConnection(A, KEY_A)));
        assertFalse(settings.save(new SpresenterConnection(B, KEY_B)));
        assertEquals(2, settings.loadAll().size());
        assertFalse(prefs.getAll().toString().contains(KEY_A));
        assertFalse(prefs.getAll().toString().contains(KEY_B));
        SpresenterApiSettings restarted = new SpresenterApiSettings(prefs, "multi.new-session", unavailable);
        assertTrue(restarted.loadAll().isEmpty());
        assertEquals(2, restarted.origins().size());
    }

    @Test public void twoPcRequestsKeepTheirOwnCredentialsAndOneFailureDoesNotEraseOtherCatalogs() throws Exception {
        SpresenterApiSettings settings = new SpresenterApiSettings(prefs, "multi.http", cipher);
        settings.save(new SpresenterConnection(A, KEY_A));
        settings.save(new SpresenterConnection(B, KEY_B));
        HtmlSourceCatalog catalog = new HtmlSourceCatalog();
        try (MockWebServer first = new MockWebServer(); MockWebServer second = new MockWebServer()) {
            first.start(); second.start();
            first.enqueue(json("Músicos")); second.enqueue(json("Músicos"));
            HtmlHttpProbe.ConnectionOpener opener = url -> (HttpURLConnection)
                    (url.getHost().equals("192.168.2.53") ? first : second).url(url.getFile()).url().openConnection();
            for (SpresenterConnection server : settings.loadAll()) {
                catalog.replaceFromCatalog(server.origin,
                        new SpresenterApiProbe().read(server, opener, new HtmlDiscoverySession.Cancellation()), 1);
            }
            assertEquals(2, catalog.snapshot().size());
            assertNotEquals(catalog.snapshot().get(0).url, catalog.snapshot().get(1).url);
            assertEquals("Bearer " + KEY_A, first.takeRequest(2, TimeUnit.SECONDS).getHeader("Authorization"));
            assertEquals("Bearer " + KEY_B, second.takeRequest(2, TimeUnit.SECONDS).getHeader("Authorization"));
            first.enqueue(new MockResponse().setResponseCode(401)); second.enqueue(json("Banda"));
            int failures = 0;
            for (SpresenterConnection server : settings.loadAll()) try {
                catalog.replaceFromCatalog(server.origin,
                        new SpresenterApiProbe().read(server, opener, new HtmlDiscoverySession.Cancellation()), 2);
            } catch (SpresenterApiProbe.Failure failure) {
                assertEquals(R.string.spresenter_api_unauthorized, failure.message); failures++;
            }
            assertEquals(1, failures);
            assertEquals(2, catalog.snapshot().size());
            assertEquals("Músicos", catalog.snapshot().get(0).title);
            assertEquals("Banda", catalog.snapshot().get(1).title);
        }
    }

    @Test public void extraRangeAcceptsOnlyAnExplicitLocalIpv4Prefix() {
        assertEquals("192.168.2", HtmlSearchSettings.normalize(" 192.168.002 "));
        assertEquals("", HtmlSearchSettings.normalize(""));
        assertEquals(254, new LanIpv4Subnet(HtmlSearchSettings.normalize("10.20.80") + ".0", 24).candidates().size());
        for (String invalid : new String[]{"8.8.8", "127.0.0", "192.168", "192.168.1.2", "192.168.1/24", "http://192.168.1", "192.168.256", "192.168.-1"}) {
            try { HtmlSearchSettings.normalize(invalid); fail(invalid); }
            catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void removingADirectConnectionDoesNotRemoveCentralPinnedSources() {
        HtmlSourceCatalog catalog = new HtmlSourceCatalog();
        catalog.replacePinnedCatalog(A, List.of(new HtmlSource(A + "/output/musicos", "Central", A)), 1);
        catalog.replaceFromCatalog(B, List.of(new HtmlSource(B + "/output/musicos", "PC adicional", B)), 1);
        catalog.forgetDirectCatalog(A, 2);
        catalog.forgetDirectCatalog(B, 2);
        assertEquals(1, catalog.snapshot().size());
        assertEquals("Central", catalog.snapshot().get(0).title);
    }

    @Test public void connectionLimitRejectsOnlyAdditionalPcsAndAllowsUpdatingAnExistingPc() {
        SpresenterApiSettings settings = new SpresenterApiSettings(prefs, "multi.limit", cipher);
        for (int i = 0; i < SpresenterApiSettings.MAX_CONNECTIONS; i++) {
            settings.save(new SpresenterConnection("http://192.168.2." + (20 + i) + ":5050", KEY_A));
        }
        try { settings.save(new SpresenterConnection(B, KEY_B)); fail(); }
        catch (IllegalArgumentException expected) { }
        assertEquals(SpresenterApiSettings.MAX_CONNECTIONS, settings.loadAll().size());
        settings.save(new SpresenterConnection("http://192.168.2.20:5050", KEY_B));
        assertEquals(KEY_B, settings.load("http://192.168.2.20:5050").token);
    }

    private static MockResponse json(String name) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(
                "{\"config\":{\"outputs\":[{\"type\":\"network\",\"slug\":\"musicos\",\"name\":\"" + name + "\"}]}}");
    }
}
