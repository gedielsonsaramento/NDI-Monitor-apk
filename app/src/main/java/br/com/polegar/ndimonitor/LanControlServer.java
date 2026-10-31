package br.com.polegar.ndimonitor;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import org.json.JSONObject;

/** Bounded LAN API. Only a visible pairing session may grant a controller credential. */
final class LanControlServer implements AutoCloseable {
    static final int PORT = 48762;
    interface Controller {
        String token();
        JSONObject pair(String token, String name) throws Exception;
        JSONObject status() throws Exception;
        JSONObject sources() throws Exception;
        JSONObject command(JSONObject command) throws Exception;
    }
    static final class Failure extends Exception {
        final int status;
        Failure(int status) { this.status = status; }
    }
    private final ServerSocket server;
    private final Controller controller;
    private final String authority;
    private final Map<String, String[]> receipts = new LinkedHashMap<>();
    private volatile boolean closed;
    private Socket active;
    private String code = "";
    private long pairingUntil;
    private int attempts;
    LanControlServer(String address, int port, Controller controller) throws IOException {
        InetAddress bind = InetAddress.getByName(address);
        if (!(bind instanceof Inet4Address) || !(bind.isSiteLocalAddress() || bind.isLoopbackAddress()
                || bind.isLinkLocalAddress())) throw new IllegalArgumentException("LAN address required");
        server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress(bind, port), 4);
        authority = bind.getHostAddress() + ":" + server.getLocalPort();
        this.controller = controller;
    }
    String address() { return "http://" + authority; }
    synchronized String openPairing() {
        return openPairing(300_000);
    }
    synchronized String openPairing(long lifetimeMs) {
        if (lifetimeMs <= 0 || lifetimeMs > 300_000) throw new IllegalArgumentException("lifetime");
        code = String.format(Locale.ROOT, "%06d", new SecureRandom().nextInt(1000000));
        pairingUntil = System.nanoTime() + lifetimeMs * 1_000_000L; attempts = 0;
        return code;
    }
    synchronized String pairingCode() {
        return !closed && System.nanoTime() < pairingUntil && attempts < 5 ? code : "";
    }
    synchronized void closePairing() { code = ""; pairingUntil = 0; }
    void start() {
        Thread thread = new Thread(this::serve, "SRetornoLocalControl");
        thread.setDaemon(true); thread.start();
    }
    private void serve() {
        while (!closed) {
            try {
                Socket socket = server.accept();
                synchronized (this) {
                    if (closed) { socket.close(); break; }
                    active = socket;
                }
                try (Socket client = socket) { handle(client); }
                catch (Exception ignored) { /* Never log credentials, URLs or client payloads. */ }
                finally { synchronized (this) { if (active == socket) active = null; } }
            } catch (IOException ignored) { if (closed) break; }
        }
    }
    private void handle(Socket client) throws Exception {
        long deadline = System.nanoTime() + 3_000_000_000L;
        InputStream in = new BufferedInputStream(client.getInputStream());
        ByteArrayOutputStream header = new ByteArrayOutputStream(); int tail = 0;
        while (header.size() < 8192 && tail != 0x0d0a0d0a) {
            int b = read(client, in, deadline); if (b < 0) return;
            header.write(b); tail = (tail << 8) | b;
        }
        if (tail != 0x0d0a0d0a) { reply(client, 431, error("invalid_headers")); return; }
        String[] lines = header.toString("US-ASCII").split("\r\n");
        String[] request = lines[0].split(" ", -1);
        if (request.length != 3 || !("HTTP/1.1".equals(request[2]) || "HTTP/1.0".equals(request[2]))) {
            reply(client, 400, error("invalid_request")); return;
        }
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon <= 0) { reply(client, 400, error("invalid_headers")); return; }
            String name = lines[i].substring(0, colon).toLowerCase(Locale.ROOT);
            if (!name.matches("[a-z0-9-]+") || headers.put(name, lines[i].substring(colon + 1).trim()) != null) {
                reply(client, 400, error("invalid_headers")); return;
            }
        }
        if (!authority.equals(headers.get("host")) || headers.containsKey("origin")
                || headers.containsKey("transfer-encoding")) {
            reply(client, 403, error("local_client_required")); return;
        }
        String method = request[0], path = request[1];
        if (method.equals("GET") && path.equals("/v1/hello")) {
            reply(client, 200, new JSONObject().put("protocol", "s-retorno-control").put("schema", 1)); return;
        }
        boolean pair = method.equals("POST") && path.equals("/v1/pair");
        if (!pair && !authorized(headers.get("authorization"))) {
            reply(client, 401, error("pairing_required")); return;
        }
        JSONObject body = new JSONObject();
        if (method.equals("POST")) {
            String type = headers.get("content-type");
            if (type == null || !type.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
                reply(client, 415, error("json_required")); return;
            }
            int length;
            try { length = Integer.parseInt(headers.get("content-length")); }
            catch (Exception invalid) { reply(client, 411, error("length_required")); return; }
            if (length < 2 || length > 8192) { reply(client, 413, error("invalid_size")); return; }
            byte[] bytes = new byte[length];
            for (int i = 0; i < length; i++) { int b = read(client, in, deadline); if (b < 0) return; bytes[i] = (byte)b; }
            try { body = new JSONObject(new String(bytes, StandardCharsets.UTF_8)); }
            catch (Exception invalid) { reply(client, 400, error("invalid_json")); return; }
        }
        try {
            if (pair) {
                synchronized (this) {
                    String expected = pairingCode();
                    if (expected.isEmpty()) throw new Failure(403);
                    if (!(body.opt("code") instanceof String) || !equal(expected, body.optString("code"))) {
                        attempts++; throw new Failure(403);
                    }
                    if (!(body.opt("name") instanceof String) || body.optString("name").length() > 96) throw new Failure(400);
                    String token = randomToken();
                    JSONObject result = controller.pair(token, body.getString("name"));
                    closePairing(); receipts.clear();
                    reply(client, 200, result.put("ok", true).put("token", token));
                }
            } else if (method.equals("GET") && path.equals("/v1/status")) {
                reply(client, 200, controller.status().put("ok", true));
            } else if (method.equals("GET") && path.equals("/v1/sources")) {
                reply(client, 200, controller.sources().put("ok", true));
            } else if (method.equals("POST") && path.equals("/v1/commands")) {
                String id = body.optString("id");
                if (!id.matches("[A-Za-z0-9-]{12,80}")) throw new Failure(400);
                String digest = digest(body.toString());
                String[] receipt = receipts.get(id);
                if (receipt != null && !receipt[0].equals(digest)) throw new Failure(409);
                JSONObject result;
                if (receipt != null) result = new JSONObject(receipt[1]);
                else {
                    result = controller.command(body).put("ok", true).put("command_id", id);
                    receipts.put(id, new String[]{digest, result.toString()});
                    if (receipts.size() > 32) receipts.remove(receipts.keySet().iterator().next());
                }
                reply(client, 200, result);
            } else reply(client, 404, error("not_found"));
        } catch (Failure failure) { reply(client, failure.status, error("request_rejected")); }
        catch (Exception unavailable) { reply(client, 503, error("app_unavailable")); }
    }
    private boolean authorized(String header) {
        String token = controller.token();
        return token != null && token.matches("[a-f0-9]{64}") && equal("Bearer " + token, header);
    }
    private static boolean equal(String a, String b) {
        return b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
    private static String digest(String value) throws Exception { return hex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
    private static String randomToken() { byte[] b = new byte[32]; new SecureRandom().nextBytes(b); return hex(b); }
    private static String hex(byte[] bytes) { StringBuilder s = new StringBuilder(); for (byte b : bytes) s.append(String.format(Locale.ROOT, "%02x", b & 255)); return s.toString(); }
    private static JSONObject error(String code) throws Exception { return new JSONObject().put("ok", false).put("error", code); }
    private static int read(Socket socket, InputStream in, long deadline) throws IOException {
        long left = (deadline - System.nanoTime()) / 1_000_000;
        if (left <= 0) throw new SocketTimeoutException();
        socket.setSoTimeout((int)Math.max(1, Math.min(3000, left))); return in.read();
    }
    private static void reply(Socket client, int status, JSONObject value) throws IOException {
        byte[] body = value.toString().getBytes(StandardCharsets.UTF_8);
        if (body.length > 65536) throw new IOException("Response limit");
        String head = "HTTP/1.1 " + status + " Response\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: "
                + body.length + "\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nConnection: close\r\n\r\n";
        client.getOutputStream().write(head.getBytes(StandardCharsets.US_ASCII));
        client.getOutputStream().write(body); client.getOutputStream().flush();
    }
    @Override public synchronized void close() {
        if (closed) return; closed = true; closePairing();
        try { server.close(); } catch (IOException ignored) { }
        if (active != null) try { active.close(); } catch (IOException ignored) { }
    }
}
