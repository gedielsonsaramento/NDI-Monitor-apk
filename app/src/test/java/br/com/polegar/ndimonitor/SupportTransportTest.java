package br.com.polegar.ndimonitor;

import android.app.Application;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, application = Application.class)
public class SupportTransportTest {
    private JSONObject event() throws Exception {
        return SupportPayload.create("heartbeat", UUID.randomUUID().toString(), "1.4.12", null, null, null);
    }
    @Test public void postsBoundedJsonWithGenericUserAgentAndNeverFollowsRedirects() throws Exception {
        SupportTestSupport.Factory factory = new SupportTestSupport.Factory();
        factory.status = 302;
        SupportTransport transport = new SupportTransport(factory);
        assertEquals(302, transport.post(SupportTestSupport.ENDPOINT, event(), () -> true));
        assertEquals(1, factory.connections.size());
        SupportTestSupport.Connection connection = factory.connections.get(0);
        assertEquals("https", connection.getURL().getProtocol());
        assertEquals("POST", connection.getRequestMethod());
        assertFalse(connection.getInstanceFollowRedirects());
        assertEquals("S-Retorno-Support/1", connection.getRequestProperty("User-Agent"));
        assertNull(connection.getRequestProperty("Authorization"));
        assertTrue(connection.getRequestProperty("Content-Type").startsWith("application/json"));
        assertEquals("heartbeat", new JSONObject(connection.body.toString("UTF-8")).getString("kind"));
        assertFalse(SupportTransport.accepted(302));
        assertTrue(connection.disconnected);
    }

    @Test public void refusesHttpOversizedPayloadAndRequestsWithoutAuthorization() throws Exception {
        SupportTestSupport.Factory factory = new SupportTestSupport.Factory();
        SupportTransport transport = new SupportTransport(factory);
        assertEquals(0, transport.post("http://example.test/receive", event(), () -> true));
        assertEquals(0, transport.post(SupportTestSupport.ENDPOINT, event(), () -> false));
        char[] large = new char[9000];
        java.util.Arrays.fill(large, 'x');
        assertEquals(0, transport.post(SupportTestSupport.ENDPOINT, new JSONObject().put("too_large", new String(large)), () -> true));
        assertTrue(factory.connections.isEmpty());
    }

    @Test public void revocationDuringConnectionSetupStopsBeforeAnyPayloadIsWritten() throws Exception {
        AtomicBoolean allowed = new AtomicBoolean(true);
        SupportTestSupport.Factory factory = new SupportTestSupport.Factory();
        factory.onOutput = () -> allowed.set(false);
        assertEquals(0, new SupportTransport(factory).post(SupportTestSupport.ENDPOINT, event(), allowed::get));
        assertEquals(0, factory.connections.get(0).body.size());
    }
}
