package br.com.polegar.ndimonitor;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Explicit, short-lived LAN setup. One credential submission; no playback/control endpoints. */
final class SpresenterPairingServer implements AutoCloseable {
    static final long LIFETIME_MS = 5 * 60 * 1000;
    private static final int MAX_HEADERS = 8192;
    private static final int MAX_BODY = 8192;
    private static final long REQUEST_MS = 3000;
    interface Listener {
        void onConnection(SpresenterConnection connection);
        void onEnded();
    }
    private final ServerSocket server;
    private final Listener listener;
    private final String authority;
    private final String code;
    private final long deadline;
    private volatile boolean closed;
    private Socket active;
    private int attempts;
    private boolean started;

    SpresenterPairingServer(String address, Listener listener) throws IOException {
        this(address, listener, LIFETIME_MS);
    }

    SpresenterPairingServer(String address, Listener listener, long lifetimeMs) throws IOException {
        if (lifetimeMs <= 0 || lifetimeMs > LIFETIME_MS) throw new IllegalArgumentException("lifetime");
        InetAddress bind = InetAddress.getByName(address);
        if (bind.isAnyLocalAddress() || bind.isMulticastAddress() || bind.getAddress().length != 4) {
            throw new IllegalArgumentException("address");
        }
        server = new ServerSocket(0, 4, bind);
        authority = bind.getHostAddress() + ":" + server.getLocalPort();
        code = String.format(Locale.ROOT, "%06d", new SecureRandom().nextInt(1000000));
        deadline = System.nanoTime() + lifetimeMs * 1000000;
        this.listener = listener;
    }

    String address() { return "http://" + authority; }
    String code() { return code; }

    synchronized void start() {
        if (closed || started) return;
        started = true;
        Thread worker = new Thread(this::serve, "MonitorSetup");
        worker.setDaemon(true);
        worker.start();
    }

    private void serve() {
        boolean notifyEnd = true;
        try {
            while (!closed && System.nanoTime() < deadline && attempts < 5) {
                server.setSoTimeout(remaining(deadline, 1000));
                Socket socket;
                try { socket = server.accept(); }
                catch (SocketTimeoutException retry) { continue; }
                synchronized (this) {
                    if (closed) { socket.close(); break; }
                    active = socket;
                }
                SpresenterConnection connection = null;
                try (Socket client = socket) { connection = handle(client); }
                catch (IOException ignored) { /* A closed/incomplete client never changes configuration. */ }
                finally { synchronized (this) { if (active == socket) active = null; } }
                if (connection != null) {
                    synchronized (this) {
                        if (!closed && System.nanoTime() < deadline) {
                            // MainActivity checks that this exact setup dialog is still visible before saving.
                            listener.onConnection(connection);
                            notifyEnd = false;
                        }
                    }
                    break;
                }
            }
        } catch (IOException ignored) { /* Local message only, never log request contents. */ }
        finally {
            boolean wasOpen = !closed;
            close();
            if (notifyEnd && wasOpen) listener.onEnded();
        }
    }

    private SpresenterConnection handle(Socket client) throws IOException {
        long requestDeadline = Math.min(deadline, System.nanoTime() + REQUEST_MS * 1000000);
        InputStream input = new BufferedInputStream(client.getInputStream());
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        int tail = 0;
        while (header.size() < MAX_HEADERS && tail != 0x0d0a0d0a) {
            int next = read(client, input, requestDeadline);
            if (next < 0) return null;
            header.write(next);
            tail = (tail << 8) | next;
        }
        if (tail != 0x0d0a0d0a) { reply(client, 431, "Cabeçalho muito grande."); return null; }
        String[] lines = new String(header.toByteArray(), StandardCharsets.US_ASCII).split("\r\n");
        String[] request = lines[0].split(" ", -1);
        if (request.length != 3 || !"HTTP/1.1".equals(request[2])) {
            reply(client, 400, "Pedido inválido."); return null;
        }
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon <= 0) { reply(client, 400, "Pedido inválido."); return null; }
            String name = lines[i].substring(0, colon).toLowerCase(Locale.ROOT);
            if (!name.matches("[a-z0-9-]+") || headers.containsKey(name)) {
                reply(client, 400, "Pedido inválido."); return null;
            }
            headers.put(name, lines[i].substring(colon + 1).trim());
        }
        String origin = headers.get("origin");
        if (!authority.equals(headers.get("host")) ||
                (origin != null && !address().equals(origin)) || headers.containsKey("transfer-encoding")) {
            reply(client, 403, "Abra o endereço mostrado na TV."); return null;
        }
        if ("GET".equals(request[0]) && "/".equals(request[1])) {
            reply(client, 200, page()); return null;
        }
        if (!"POST".equals(request[0]) || !"/configurar".equals(request[1])) {
            reply(client, 404, "Página não encontrada."); return null;
        }
        String type = headers.get("content-type");
        if (type == null || !type.split(";", 2)[0].trim().equalsIgnoreCase("application/x-www-form-urlencoded")) {
            reply(client, 415, "Use o formulário desta página."); return null;
        }
        int length;
        try { length = Integer.parseInt(headers.get("content-length")); }
        catch (RuntimeException invalid) { reply(client, 411, "Pedido incompleto."); return null; }
        if (length < 1 || length > MAX_BODY) { reply(client, 413, "Link muito grande."); return null; }
        byte[] body = new byte[length];
        for (int i = 0; i < length; i++) {
            int next = read(client, input, requestDeadline);
            if (next < 0) return null;
            body[i] = (byte) next;
        }
        Map<String, String> form = new HashMap<>();
        try {
            for (String pair : new String(body, StandardCharsets.UTF_8).split("&", -1)) {
                int equal = pair.indexOf('=');
                if (equal <= 0) throw new IllegalArgumentException();
                String name = URLDecoder.decode(pair.substring(0, equal), "UTF-8");
                if ((!name.equals("code") && !name.equals("link")) || form.containsKey(name)) {
                    throw new IllegalArgumentException();
                }
                form.put(name, URLDecoder.decode(pair.substring(equal + 1), "UTF-8"));
            }
        } catch (IllegalArgumentException invalid) { reply(client, 400, "Pedido inválido."); return null; }
        String supplied = form.get("code");
        if (supplied == null || !MessageDigest.isEqual(code.getBytes(StandardCharsets.US_ASCII),
                supplied.getBytes(StandardCharsets.UTF_8))) {
            attempts++;
            reply(client, 403, "Código incorreto. Volte e confira o código na TV."); return null;
        }
        SpresenterConnection connection;
        try { connection = SpresenterConnection.fromEditorLink(form.get("link")); }
        catch (IllegalArgumentException invalid) {
            reply(client, 400, "Link inválido. Volte e cole o link completo de Copiar link com token."); return null;
        }
        if (closed || System.nanoTime() >= deadline) return null;
        reply(client, 200, "<h1>Link recebido</h1><p>Confira a lista de saídas na TV e selecione a desejada.</p>"
                + "<p>Se a tela de configuração foi fechada na TV, abra novamente para repetir o envio.</p>");
        return connection;
    }

    private static int read(Socket client, InputStream input, long deadline) throws IOException {
        client.setSoTimeout(remaining(deadline, (int) REQUEST_MS));
        return input.read();
    }

    private static int remaining(long deadline, int maximum) throws SocketTimeoutException {
        long nanos = deadline - System.nanoTime();
        if (nanos <= 0) throw new SocketTimeoutException();
        return (int) Math.max(1, Math.min(maximum, nanos / 1000000));
    }

    private static void reply(Socket client, int status, String content) throws IOException {
        String html = "<!doctype html><html lang=pt-BR><meta charset=utf-8>"
                + "<meta name=viewport content=\"width=device-width,initial-scale=1\">"
                + "<title>Configurar S-Retorno</title><style>"
                + "body{font:18px sans-serif;background:#11181f;color:#f3f5f7;margin:0;padding:24px}"
                + "main{max-width:520px;margin:auto}h1{font-size:26px}label{display:block;margin:24px 0 8px}"
                + "input,textarea,button{box-sizing:border-box;width:100%;font:inherit;padding:14px;border-radius:8px}"
                + "input,textarea{background:#fff;color:#111;border:1px solid #abb3bc}"
                + "button{background:#22b7df;color:#071116;border:0;margin:24px 0;cursor:pointer}"
                + "p{line-height:1.5}a{color:#63d6f5}</style><main>" + content + "</main></html>";
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + status + " Response\r\nContent-Type: text/html; charset=utf-8\r\n"
                + "Content-Length: " + body.length + "\r\nConnection: close\r\nCache-Control: no-store\r\n"
                // no-referrer makes a normal browser form POST send Origin: null, which must stay rejected.
                + "Referrer-Policy: same-origin\r\nX-Content-Type-Options: nosniff\r\nX-Frame-Options: DENY\r\n"
                + "Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; "
                + "base-uri 'none'; frame-ancestors 'none'\r\n\r\n";
        client.getOutputStream().write(headers.getBytes(StandardCharsets.US_ASCII));
        client.getOutputStream().write(body);
        client.getOutputStream().flush();
    }

    private static String page() {
        return "<h1>Configurar esta TV</h1><p>No S-Present, use <b>Copiar link com token</b> no Editor remoto (rede). "
                + "Cole o link completo abaixo e informe o código mostrado na TV.</p>"
                + "<form action=/configurar method=post autocomplete=off>"
                + "<label for=code>Código da TV</label><input id=code name=code inputmode=numeric pattern=\"[0-9]{6}\" "
                + "maxlength=6 minlength=6 autocomplete=off required>"
                + "<label for=link>Link completo do S-Present</label><textarea id=link name=link rows=4 maxlength=2048 "
                + "autocomplete=off autocapitalize=off spellcheck=false required></textarea>"
                + "<button type=submit>Enviar para esta TV</button></form>"
                + "<p>A página funciona por até 5 minutos, enquanto a configuração estiver aberta na TV.</p>";
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        try { server.close(); } catch (IOException ignored) { }
        if (active != null) try { active.close(); } catch (IOException ignored) { }
        active = null;
    }
}
