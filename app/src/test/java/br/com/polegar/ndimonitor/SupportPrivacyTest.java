package br.com.polegar.ndimonitor;

import android.app.Application;
import android.content.Context;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25, application = Application.class)
public class SupportPrivacyTest {
    private Context context;
    private static final String ENDPOINT = "https://support.example.test/api/v1/telemetry";
    @Before public void clear() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences(SupportStore.PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test public void payloadContainsNoRawMessagesFilesThreadNamesOrSourceConfiguration() throws Exception {
        IllegalStateException error = new IllegalStateException("http://192.168.2.53/output/secret?token=spk_PRIVATE");
        error.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("br.com.polegar.ndimonitor.MainActivity", "onCreate", "/private/customer-name.java", 129),
                new StackTraceElement("outside.private.Name", "secret", "secret-name", 10)});
        JSONObject event = SupportPayload.create("java_crash", UUID.randomUUID().toString(), "1.4.12",
                new SupportMetrics.Sample(2000L, 1000L, 10d, 120L, 20L), SupportPayload.javaFailure(error), null);
        assertEquals(7, event.length());
        String body = event.toString();
        for (String forbidden : new String[]{"192.168", "spk_", "customer", "secret-name", "outside.private", "MODEL", "MANUFACTURER", "android_id", "SSID"})
            assertFalse(forbidden, body.contains(forbidden));
        assertEquals("java.lang.IllegalStateException", event.getJSONObject("failure").getString("exception_class"));
        assertEquals(1, event.getJSONObject("failure").getJSONArray("app_frames").length());
        assertEquals(64, event.getJSONObject("failure").getString("fingerprint").length());
    }

    @Test public void optOutClearsQueueIdentifierAndAuthorization() throws Exception {
        SupportStore store = new SupportStore(context, ENDPOINT);
        assertFalse(store.enabled());
        store.setEnabled(true, 123);
        String first = store.installationId();
        store.markForeground();
        store.enqueue(SupportPayload.create("java_crash", first, "1.4.12", null, null, null), true);
        assertEquals(1, store.pending().size());
        store.setEnabled(false, 124);
        assertFalse(store.enabled());
        assertTrue(store.pending().isEmpty());
        assertTrue(context.getSharedPreferences(SupportStore.PREFERENCES, Context.MODE_PRIVATE).getAll().isEmpty());
        store.setEnabled(true, 125);
        assertNotEquals(first, store.installationId());
        assertEquals(125, store.consentSinceMs());
    }

    @Test public void factoryDropsUnknownFailureFieldsAndRecomputesTheFingerprint() throws Exception {
        JSONObject unsafe = new JSONObject().put("message", "token=spk_PRIVATE")
                .put("url", "http://192.168.2.53/output/private")
                .put("source", "customer-private-name").put("fingerprint", "private-free-text")
                .put("exception_class", "java.lang.IllegalStateException")
                .put("app_frames", new JSONArray().put("outside.private.Person#secret:1")
                        .put("br.com.polegar.ndimonitor.MainActivity#onCreate:129"));
        JSONObject safe = SupportPayload.create("java_crash", UUID.randomUUID().toString(),
                "1.4.12", null, unsafe, null).getJSONObject("failure");
        assertEquals(3, safe.length());
        assertEquals(1, safe.getJSONArray("app_frames").length());
        assertEquals(64, safe.getString("fingerprint").length());
        for (String forbidden : new String[]{"PRIVATE", "192.168", "private", "message", "url", "source"})
            assertFalse(forbidden, safe.toString().contains(forbidden));
    }

    @Test public void destinationChangesRequireFreshAuthorizationAndDiscardOldData() throws Exception {
        SupportStore first = new SupportStore(context, ENDPOINT);
        first.setEnabled(true, 1);
        first.enqueue(SupportPayload.create("java_crash", first.installationId(), "1.4.12", null, null, null), true);
        SupportStore changed = new SupportStore(context, "https://other.example.test/receive");
        assertFalse(changed.enabled());
        assertTrue(changed.pending().isEmpty());
    }

    @Test public void offlineFailureQueueIsBoundedAndAcknowledgesOnlyTheReceivedEvent() throws Exception {
        SupportStore store = new SupportStore(context, ENDPOINT);
        store.setEnabled(true, 1);
        String last = "";
        for (int i = 0; i < 30; i++) {
            JSONObject event = SupportPayload.create("java_crash", store.installationId(), "1.4.12", null, null, null);
            last = event.getString("event_id");
            store.enqueue(event, true);
        }
        assertEquals(SupportStore.MAX_EVENTS, store.pending().size());
        store.acknowledge(last);
        assertEquals(SupportStore.MAX_EVENTS - 1, store.pending().size());
        assertTrue(context.getSharedPreferences(SupportStore.PREFERENCES, Context.MODE_PRIVATE)
                .getString("pending_events", "").length() < SupportStore.MAX_QUEUE_BYTES);
    }

    @Test public void emptyHttpCredentialAndRedirectStyleEndpointsCannotEnableAutomaticSharing() {
        for (String endpoint : new String[]{"", "http://example.test/receive", "https://user:password@example.test/receive",
                "https://example.test/receive?token=secret", "https://example.test/receive#fragment"}) {
            SupportStore store = new SupportStore(context, endpoint);
            store.setEnabled(true, 1);
            assertFalse(endpoint, store.enabled());
            assertFalse(endpoint, store.configured());
        }
        assertEquals("https://example.test/a%2Fb", SupportEndpoint.normalize("https://EXAMPLE.test/a%2Fb"));
    }
}
