package br.com.polegar.ndimonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

/** Foreground-only local management; independent of opt-in remote support telemetry. */
final class LocalControl implements AutoCloseable {
    interface Host {
        JSONObject status() throws Exception;
        JSONObject sources() throws Exception;
        JSONObject command(JSONObject command) throws Exception;
    }
    interface Work { JSONObject run() throws Exception; }
    private final Activity owner;
    private final Host host;
    final LocalControlStore store;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private final SupportMetrics.Sampler sampler = new SupportMetrics.Sampler(new SupportMetrics.AndroidProbe());
    private volatile SupportMetrics.Sample metrics;
    private volatile LanControlServer server;
    private volatile boolean foreground, closed;
    private String bound = "";
    private String networkState = "Aguardando rede local";
    private ScheduledFuture<?> next;
    private AlertDialog dialog;
    private TextView info;
    private volatile boolean pairingRequested;
    private final long started = SystemClock.elapsedRealtime();
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (dialog == null) return;
            LanControlServer current = server;
            String code = current == null ? "" : current.pairingCode();
            info.setText(!store.enabled() ? "Ative o pareamento para gerenciar este retorno pelo Spresenter."
                    : current == null ? networkState
                    : "Endereço: " + current.address() + "\n\n" + (code.isEmpty()
                    ? store.token().isEmpty() ? "Gere um código para vincular o plugin."
                    : "Pareado: " + store.name() + (store.durable() ? "" : "\nVínculo somente nesta sessão; será necessário parear após fechar o processo.")
                    : "Código: " + code + "\nVálido por até 5 minutos com esta janela aberta.")
                    + "\n\nNo PC, abra Central de Retornos e informe este endereço e código."
                    + "\nO acesso fica disponível enquanto o app está aberto. Use uma rede local confiável.");
            main.postDelayed(this, 1000);
        }
    };
    LocalControl(Activity owner, Host host) {
        this.owner = owner; this.host = host;
        store = MonitorApplication.local(owner);
    }
    synchronized void foreground() {
        if (foreground || closed) return;
        foreground = true; store.foreground(); schedule(0);
    }
    synchronized void background() {
        foreground = false; pairingRequested = false;
        if (next != null) next.cancel(false);
        stopServer(); store.background(); dismiss();
    }
    private synchronized void schedule(long delay) {
        if (!foreground || closed || !store.enabled()) return;
        next = worker.schedule(this::tick, delay, TimeUnit.MILLISECONDS);
    }
    private void tick() {
        try {
            if (!foreground || closed || !store.enabled()) { stopServer(); return; }
            metrics = sampler.sample();
            String address = SpresenterSetupNetwork.address(owner);
            synchronized (this) {
                if (!foreground || closed || !store.enabled()) return;
                if (address == null || !address.equals(bound)) stopServer();
                if (address != null && server == null) {
                    try {
                        server = new LanControlServer(address, LanControlServer.PORT, new LanControlServer.Controller() {
                            @Override public String token() { return store.token(); }
                            @Override public JSONObject pair(String token, String name) throws Exception {
                                if (!foreground || !store.enabled()) throw new LanControlServer.Failure(503);
                                return store.pair(token, name);
                            }
                            @Override public JSONObject status() throws Exception {
                                JSONObject result = onMain(host::status);
                                SupportMetrics.Sample sample = metrics;
                                return result.put("schema", 1).put("id", store.id()).put("name", store.name())
                                        .put("version", BuildConfig.VERSION_NAME).put("uptime_ms", SystemClock.elapsedRealtime() - started)
                                        .put("durable", store.durable()).put("events", store.events())
                                        .put("metrics", sample == null ? JSONObject.NULL : sample.json());
                            }
                            @Override public JSONObject sources() throws Exception { return onMain(host::sources); }
                            @Override public JSONObject command(JSONObject command) throws Exception {
                                return onMain(() -> {
                                    if ("rename".equals(command.optString("action"))) {
                                        if (!(command.opt("name") instanceof String)) throw new LanControlServer.Failure(400);
                                        store.rename(command.getString("name")); return new JSONObject().put("applied", true);
                                    }
                                    return host.command(command);
                                });
                            }
                        });
                        bound = address; server.start(); networkState = "";
                    } catch (java.io.IOException unavailable) { networkState = "Não foi possível abrir o acesso local. Tente novamente."; }
                }
                if (pairingRequested && server != null) main.post(() -> {
                    LanControlServer current = server;
                    if (foreground && dialog != null && dialog.isShowing() && pairingRequested && current != null) {
                        current.openPairing(); pairingRequested = false;
                    }
                });
            }
        } catch (Exception unavailable) { networkState = "Aguardando uma conexão Wi-Fi ou Ethernet"; }
        finally { schedule(5000); }
    }
    private JSONObject onMain(Work work) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicBoolean pending = new AtomicBoolean(true);
        JSONObject[] result = new JSONObject[1]; Exception[] failure = new Exception[1];
        main.post(() -> {
            if (!pending.compareAndSet(true, false)) return;
            try {
                if (!foreground || closed || !store.enabled()) throw new LanControlServer.Failure(503);
                result[0] = work.run();
            } catch (Exception e) { failure[0] = e; }
            finally { done.countDown(); }
        });
        if (!done.await(2000, TimeUnit.MILLISECONDS)) {
            pending.set(false); throw new LanControlServer.Failure(503);
        }
        if (failure[0] != null) throw failure[0];
        return result[0];
    }
    void show() {
        if (dialog != null) return;
        LinearLayout content = new LinearLayout(owner); content.setOrientation(LinearLayout.VERTICAL);
        int p = Math.round(18 * owner.getResources().getDisplayMetrics().density); content.setPadding(p,p,p,p);
        info = new TextView(owner); info.setTextSize(17); info.setTextIsSelectable(true);
        content.addView(info, new LinearLayout.LayoutParams(-1,-2));
        Button pair = new Button(owner); pair.setText("Gerar código de pareamento"); pair.setAllCaps(false);
        content.addView(pair, new LinearLayout.LayoutParams(-1,-2));
        Button revoke = new Button(owner); revoke.setText("Desativar e desvincular"); revoke.setAllCaps(false);
        content.addView(revoke, new LinearLayout.LayoutParams(-1,-2));
        TextView scope = new TextView(owner);
        scope.setText("O plugin poderá escolher fontes, consultar métricas e solicitar manutenção.\nPara manter o mesmo IP, configure uma reserva DHCP no roteador.\nEste gerenciamento local não ativa o envio de dados ao servidor remoto.");
        content.addView(scope, new LinearLayout.LayoutParams(-1,-2));
        pair.setOnClickListener(view -> {
            store.enable(); pairingRequested = true;
            synchronized (this) { if (next != null) next.cancel(false); schedule(0); }
        });
        revoke.setOnClickListener(view -> {
            store.revoke(); pairingRequested = false; stopServer();
        });
        ScrollView scroll = new ScrollView(owner); scroll.addView(content);
        dialog = new AlertDialog.Builder(owner).setTitle("Central de Retornos").setView(scroll)
                .setPositiveButton("Fechar", null).create();
        dialog.setOnDismissListener(ignored -> {
            pairingRequested = false;
            LanControlServer current = server; if (current != null) current.closePairing();
            main.removeCallbacks(refresh); dialog = null;
        });
        dialog.show(); refresh.run();
    }
    private synchronized void stopServer() {
        LanControlServer old = server; server = null; bound = "";
        if (old != null) old.close();
    }
    void dismiss() { if (dialog != null) dialog.dismiss(); }
    @Override public void close() { closed = true; background(); worker.shutdownNow(); }
}
