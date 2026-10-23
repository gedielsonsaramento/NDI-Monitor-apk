package br.com.polegar.ndimonitor;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Binds discovery requests to Wi-Fi/Ethernet, even when mobile data is the default network. */
final class LanHtmlDiscovery {
    static final int MAX_TARGETS = 512;
    interface Listener {
        void onStarted(int targets, boolean limited);
        void onSource(HtmlSource source);
        void onProgress(int completed, int total);
        void onFinished();
        void onUnavailable();
    }
    private final ConnectivityManager connectivity;
    private final Handler main = new Handler(Looper.getMainLooper());
    private HtmlDiscoverySession session;
    private int generation;

    LanHtmlDiscovery(Context context) {
        connectivity = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    void start(String savedAddress, Listener listener) {
        stop();
        int current = generation;
        Map<String, HtmlHttpProbe.Target> targets = new LinkedHashMap<>();
        java.util.List<String> paths = HtmlSourceUrl.discoveryPaths(savedAddress);
        boolean limited = false;
        int networks = 0;
        try {
            if (connectivity != null) for (Network network : connectivity.getAllNetworks()) {
                NetworkCapabilities capabilities = connectivity.getNetworkCapabilities(network);
                if (capabilities == null || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                        (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                         !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) continue;
                LinkProperties link = connectivity.getLinkProperties(network);
                if (link == null) continue;
                if (++networks > 2) { limited = true; break; }
                for (LinkAddress address : link.getLinkAddresses()) {
                    if (!(address.getAddress() instanceof Inet4Address)) continue;
                    LanIpv4Subnet subnet;
                    try { subnet = new LanIpv4Subnet(address.getAddress().getHostAddress(), address.getPrefixLength()); }
                    catch (IllegalArgumentException notLocal) { continue; }
                    limited |= subnet.prefix < 24;
                    // Only the saved host receives its full custom URL, including parameters.
                    if (HtmlSourceUrl.isWebAddress(savedAddress)) {
                        URI saved = URI.create(savedAddress);
                        if (subnet.contains(saved.getHost())) add(targets, network, savedAddress);
                    }
                    for (String host : subnet.candidates()) {
                        String root = "http://" + host + ":" + HtmlSourceUrl.DEFAULT_PORT;
                        String[] alternatives = new String[paths.size() - 1];
                        for (int i = 1; i < paths.size(); ++i) alternatives[i - 1] = root + paths.get(i);
                        if (!add(targets, network, root + paths.get(0), alternatives)) { limited = true; break; }
                    }
                }
            }
        } catch (SecurityException unavailable) { targets.clear(); }
        if (targets.isEmpty()) { dispatch(current, listener::onUnavailable); return; }
        int total = targets.size();
        boolean scanLimited = limited;
        dispatch(current, () -> listener.onStarted(total, scanLimited));
        HtmlHttpProbe probe = new HtmlHttpProbe();
        session = new HtmlDiscoverySession(new ArrayList<>(targets.values()), probe::probe,
                new HtmlDiscoverySession.Listener() {
                    @Override public void onSource(HtmlSource source) {
                        dispatch(current, () -> listener.onSource(source));
                    }
                    @Override public void onProgress(int done, int count) {
                        dispatch(current, () -> listener.onProgress(done, count));
                    }
                    @Override public void onFinished() { dispatch(current, listener::onFinished); }
                });
        session.start();
    }

    private static boolean add(Map<String, HtmlHttpProbe.Target> targets, Network network, String url,
                               String... alternatives) {
        if (targets.size() >= MAX_TARGETS) return false;
        String key = network.toString() + "|" + url;
        // Targets are assembled on the calling thread. Map.putIfAbsent needs API 24.
        if (!targets.containsKey(key)) targets.put(key, new HtmlHttpProbe.Target(url,
                address -> (HttpURLConnection) network.openConnection(address, Proxy.NO_PROXY), alternatives));
        return true;
    }

    private void dispatch(int expected, Runnable callback) {
        main.post(() -> { if (generation == expected) callback.run(); });
    }

    void stop() {
        ++generation;
        if (session != null) { session.cancel(); session = null; }
        main.removeCallbacksAndMessages(null);
    }
}
