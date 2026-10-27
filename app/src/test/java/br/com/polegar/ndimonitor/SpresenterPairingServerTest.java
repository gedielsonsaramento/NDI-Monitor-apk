package br.com.polegar.ndimonitor;

import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class SpresenterPairingServerTest {
    private static final String KEY = "spk_synthetic_pairing_token_123456";
    private static final String LINK = "http://192.168.2.53:5050/warp/?token=" + KEY;
    static final class Events implements SpresenterPairingServer.Listener {
        final CountDownLatch received = new CountDownLatch(1), ended = new CountDownLatch(1);
        final AtomicInteger count = new AtomicInteger();
        final AtomicReference<SpresenterConnection> connection = new AtomicReference<>();
        @Override public void onConnection(SpresenterConnection value) {
            connection.set(value); count.incrementAndGet(); received.countDown();
        }
        @Override public void onEnded() { ended.countDown(); }
    }

    @Test public void browserFormDoesNotExposePairingCodeAndStoresNothingInUrlOrPage() throws Exception {
        Events events = new Events();
        try (SpresenterPairingServer server = new SpresenterPairingServer("127.0.0.1", events)) {
            server.start();
            String response = get(server, "/");
            assertTrue(response.startsWith("HTTP/1.1 200 "));
            assertTrue(response.contains("method=post"));
            assertTrue(response.contains("name=link"));
            assertTrue(response.contains("Cache-Control: no-store"));
            assertTrue(response.contains("Referrer-Policy: same-origin"));
            assertTrue(response.contains("form-action 'self'"));
            assertFalse(response.contains(server.code()));
            assertFalse(server.address().contains(server.code()));
            assertFalse(response.contains(KEY));
            assertEquals(0, events.count.get());
        }
    }

    @Test public void validSubmissionDeliversOneConnectionAndCannotBeReused() throws Exception {
        Events events = new Events();
        try (SpresenterPairingServer server = new SpresenterPairingServer("127.0.0.1", events)) {
            server.start();
            String result = post(server, form(server.code(), LINK), "");
            assertTrue(result.startsWith("HTTP/1.1 200 "));
            assertFalse(result.contains(KEY));
            assertTrue(events.received.await(3, TimeUnit.SECONDS));
            assertEquals("http://192.168.2.53:5050", events.connection.get().origin);
            assertEquals(KEY, events.connection.get().token);
            assertEquals(1, events.count.get());
            server.close();
            assertThrows(IOException.class, () -> get(server, "/"));
        }
    }

    @Test public void fiveWrongCodesEndTheSessionWithoutAcceptingAKey() throws Exception {
        Events events = new Events();
        try (SpresenterPairingServer server = new SpresenterPairingServer("127.0.0.1", events)) {
            server.start();
            String wrong = server.code().equals("000000") ? "999999" : "000000";
            for (int i = 0; i < 5; i++) assertTrue(post(server, form(wrong, LINK), "").startsWith("HTTP/1.1 403 "));
            assertTrue(events.ended.await(3, TimeUnit.SECONDS));
            assertEquals(0, events.count.get());
            assertThrows(IOException.class, () -> get(server, "/"));
        }
    }

    @Test public void invalidLinkCanBeCorrectedWithoutDisplayingTheSuppliedLink() throws Exception {
        Events events = new Events();
        try (SpresenterPairingServer server = new SpresenterPairingServer("127.0.0.1", events)) {
            server.start();
            String response = post(server, form(server.code(), LINK.replace("/warp/", "/other/")), "");
            assertTrue(response.startsWith("HTTP/1.1 400 ")); assertFalse(response.contains(KEY));
            assertEquals(0, events.count.get());
            assertTrue(post(server, form(server.code(), LINK), "").startsWith("HTTP/1.1 200 "));
            assertTrue(events.received.await(3, TimeUnit.SECONDS));
        }
    }

    @Test public void codeBelongsToOneBoxSession() throws Exception {
        Events events = new Events();
        try (SpresenterPairingServer first = new SpresenterPairingServer("127.0.0.1", new Events());
             SpresenterPairingServer second = new SpresenterPairingServer("127.0.0.1", events)) {
            second.start();
            String unrelated = first.code();
            if (unrelated.equals(second.code())) unrelated = second.code().equals("000000") ? "999999" : "000000";
            assertTrue(post(second, form(unrelated, LINK), "").startsWith("HTTP/1.1 403 "));
            assertEquals(0, events.count.get());
            assertTrue(post(second, form(second.code(), LINK), "").startsWith("HTTP/1.1 200 "));
            assertTrue(events.received.await(3, TimeUnit.SECONDS));
        }
    }

    @Test public void rejectsCrossOriginPostsAndUnrelatedHostHeaders() throws Exception {
        Events events = new Events();
        try (SpresenterPairingServer server = new SpresenterPairingServer("127.0.0.1", events)) {
            server.start();
            String body = form(server.code(), LINK);
            assertTrue(post(server, body, "Origin: https://other.example\r\n").startsWith("HTTP/1.1 403 "));
            assertTrue(post(server, body, "Origin: null\r\n").startsWith("HTTP/1.1 403 "));
            assertTrue(raw(server, "GET / HTTP/1.1\r\nHost: other.example\r\n\r\n").startsWith("HTTP/1.1 403 "));
            assertTrue(post(server, body, "Origin: " + server.address() + "\r\n").startsWith("HTTP/1.1 200 "));
            assertTrue(events.received.await(3, TimeUnit.SECONDS));
        }
    }

    @Test public void rejectsGetCredentialsDuplicateFormFieldsAndChunkedRequests() throws Exception {
        Events events = new Events();
        try (SpresenterPairingServer server = new SpresenterPairingServer("127.0.0.1", events)) {
            server.start();
            assertTrue(get(server, "/configurar?" + form(server.code(), LINK)).startsWith("HTTP/1.1 404 "));
            assertTrue(post(server, form(server.code(), LINK) + "&code=" + server.code(), "")
                    .startsWith("HTTP/1.1 400 "));
            assertTrue(post(server, form(server.code(), LINK), "Transfer-Encoding: chunked\r\n")
                    .startsWith("HTTP/1.1 403 "));
            assertTrue(post(server, form(server.code(), LINK), "Content-Length: 1\r\n")
                    .startsWith("HTTP/1.1 400 "));
            assertEquals(0, events.count.get());
        }
    }

    @Test public void rejectsOversizedContentBeforeReadingBody() throws Exception {
        Events events = new Events();
        try (SpresenterPairingServer server = new SpresenterPairingServer("127.0.0.1", events)) {
            server.start();
            String request = "POST /configurar HTTP/1.1\r\nHost: " + authority(server)
                    + "\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: 999999\r\n\r\n";
            assertTrue(raw(server, request).startsWith("HTTP/1.1 413 "));
            assertEquals(0, events.count.get());
        }
    }

    @Test public void expiresEvenWithoutTraffic() throws Exception {
        Events events = new Events();
        try (SpresenterPairingServer server = new SpresenterPairingServer("127.0.0.1", events, 100)) {
            server.start();
            assertTrue(events.ended.await(3, TimeUnit.SECONDS));
            assertEquals(0, events.count.get());
            assertThrows(IOException.class, () -> get(server, "/"));
        }
    }

    @Test public void cancellationInterruptsAnIncompleteClientWithoutDeliveringCredentials() throws Exception {
        Events events = new Events();
        try (SpresenterPairingServer server = new SpresenterPairingServer("127.0.0.1", events)) {
            server.start(); URI url = URI.create(server.address());
            try (Socket socket = new Socket(url.getHost(), url.getPort())) {
                socket.setSoTimeout(2000);
                socket.getOutputStream().write("POST /configurar HTTP/1.1\r\n".getBytes(StandardCharsets.US_ASCII));
                server.close();
                try { assertEquals(-1, socket.getInputStream().read()); } catch (IOException closed) { }
            }
            assertEquals(0, events.count.get());
        }
    }

    private static String form(String code, String link) throws Exception {
        return "code=" + URLEncoder.encode(code, "UTF-8") + "&link=" + URLEncoder.encode(link, "UTF-8");
    }
    private static String authority(SpresenterPairingServer server) { return URI.create(server.address()).getAuthority(); }
    private static String get(SpresenterPairingServer server, String path) throws Exception {
        return raw(server, "GET " + path + " HTTP/1.1\r\nHost: " + authority(server) + "\r\n\r\n");
    }
    private static String post(SpresenterPairingServer server, String body, String extra) throws Exception {
        return raw(server, "POST /configurar HTTP/1.1\r\nHost: " + authority(server)
                + "\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: "
                + body.getBytes(StandardCharsets.UTF_8).length + "\r\n" + extra + "\r\n" + body);
    }
    private static String raw(SpresenterPairingServer server, String request) throws Exception {
        URI url = URI.create(server.address());
        try (Socket socket = new Socket(url.getHost(), url.getPort())) {
            socket.setSoTimeout(3000);
            socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048]; int size;
            while ((size = socket.getInputStream().read(buffer)) >= 0) output.write(buffer, 0, size);
            return output.toString("UTF-8");
        }
    }
}
