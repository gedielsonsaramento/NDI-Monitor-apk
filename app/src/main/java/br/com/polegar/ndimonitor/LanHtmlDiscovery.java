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
        default void onCatalog(String origin, java.util.List<HtmlSource> sources) { }
        default void onCatalogError(int message) { }
    }
    private final ConnectivityManager connectivity;
    private final SpresenterApiSettings apiSettings;
    private final Handler main = new Handler(Looper.getMainLooper());
    private HtmlDiscoverySession session;
    private int generation;

    LanHtmlDiscovery(Context context) {
        apiSettings = new SpresenterApiSettings(context);
        connectivity = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    void start(String savedAddress, Listener listener) {
        stop();
        int current = generation;
        Map<String, HtmlHttpProbe.Target> targets = new LinkedHashMap<>();
        java.util.List<String> paths = HtmlSourceUrl.discoveryPaths(savedAddress);
        SpresenterConnection api = apiSettings.load();
        Network apiNetwork = null;
        Network fallbackApiNetwork = null;
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
                if (fallbackApiNetwork == null) fallbackApiNetwork = network;
                if (api != null && apiNetwork == null && matchesApiSubnet(link, api.origin)) apiNetwork = network;
                for (LinkAddress address : link.getLinkAddresses()) {
                    if (!(address.getAddress() instanceof Inet4Address)) continue;
                    LanIpv4Subnet subnet;
                    try { subnet = new LanIpv4Subnet(address.getAddress().getHostAddress(), address.getPrefixLength()); }
                    catch (IllegalArgumentException notLocal) { continue; }
                    limited |= subnet.prefix < 24;
                    // Only the saved host receives its full custom URL, including parameters.
                    if (HtmlSourceUrl.isWebAddress(savedAddress)) {
                        URI saved = URI.create(savedAddress);
                        if (subnet.contains(saved.getHost()) && (api == null || !api.owns(savedAddress))) {
                            add(targets, network, savedAddress);
                        }
                    }
                    for (String host : subnet.candidates()) {
                        String root = "http://" + host + ":" + HtmlSourceUrl.DEFAULT_PORT;
                        if (api != null && api.owns(root)) continue;
                        String[] alternatives = new String[paths.size() - 1];
                        for (int i = 1; i < paths.size(); ++i) alternatives[i - 1] = root + paths.get(i);
                        if (!add(targets, network, root + paths.get(0), alternatives)) { limited = true; break; }
                    }
                }
            }
        } catch (SecurityException unavailable) { targets.clear(); }
        java.util.List<HtmlHttpProbe.Target> work = new ArrayList<>();
        Network selectedApiNetwork = apiNetwork != null ? apiNetwork : fallbackApiNetwork;
        if (api != null && selectedApiNetwork != null) {
            work.add(new HtmlHttpProbe.Target(api.endpoint(),
                    address -> (HttpURLConnection) selectedApiNetwork.openConnection(address, Proxy.NO_PROXY)));
        }
        for (HtmlHttpProbe.Target target : targets.values()) {
            if (work.size() >= MAX_TARGETS) { limited = true; break; }
            work.add(target);
        }
        if (work.isEmpty()) { dispatch(current, listener::onUnavailable); return; }
        int total = work.size();
        boolean scanLimited = limited;
        dispatch(current, () -> listener.onStarted(total, scanLimited));
        if (api == null && !apiSettings.origin().isEmpty()) {
            dispatch(current, () -> listener.onCatalogError(R.string.spresenter_api_key_required));
        }
        HtmlHttpProbe probe = new HtmlHttpProbe();
        session = new HtmlDiscoverySession(work, (target, cancellation) -> {
            if (api != null && target.url.equals(api.endpoint())) {
                try {
                    java.util.List<HtmlSource> sources = new SpresenterApiProbe().read(api, target.opener, cancellation);
                    dispatch(current, () -> listener.onCatalog(api.origin, sources));
                } catch (SpresenterApiProbe.Failure failure) {
                    if (!cancellation.isCancelled()) dispatch(current, () -> listener.onCatalogError(failure.message));
                } catch (java.io.IOException unavailable) {
                    if (!cancellation.isCancelled()) dispatch(current,
                            () -> listener.onCatalogError(R.string.spresenter_api_network));
                }
                return null;
            }
            return probe.probe(target, cancellation);
        },
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

    static boolean matchesApiSubnet(LinkProperties link, String origin) {
        String host = URI.create(origin).getHost();
        for (LinkAddress address : link.getLinkAddresses()) {
            if (!(address.getAddress() instanceof Inet4Address)) continue;
            try {
                if (new LanIpv4Subnet(address.getAddress().getHostAddress(), address.getPrefixLength()).contains(host)) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) { }
        }
        return false;
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
