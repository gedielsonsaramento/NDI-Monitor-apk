package br.com.polegar.ndimonitor;

import android.content.Context;
import android.content.SharedPreferences;
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
    static final int MAX_TARGETS = 768;
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
    private final SharedPreferences preferences;
    private final Handler main = new Handler(Looper.getMainLooper());
    private HtmlDiscoverySession session;
    private int generation;

    LanHtmlDiscovery(Context context) {
        apiSettings = new SpresenterApiSettings(context);
        preferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        connectivity = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    void start(String savedAddress, Listener listener) {
        start(savedAddress, listener, false);
    }

    void startPublic(String savedAddress, Listener listener) {
        start(savedAddress, listener, true);
    }

    private void start(String savedAddress, Listener listener, boolean publicOnly) {
        stop();
        int current = generation;
        Map<String, HtmlHttpProbe.Target> targets = new LinkedHashMap<>();
        java.util.List<String> paths = HtmlSourceUrl.discoveryPaths(savedAddress);
        java.util.List<SpresenterConnection> apis = publicOnly ? java.util.Collections.emptyList() : apiSettings.loadAll();
        Map<String, Network> apiNetworks = new LinkedHashMap<>();
        Network fallbackApiNetwork = null;
        Network savedNetwork = null;
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
                for (SpresenterConnection api : apis) {
                    if (!apiNetworks.containsKey(api.origin) && matchesApiSubnet(link, api.origin)) {
                        apiNetworks.put(api.origin, network);
                    }
                }
                if (savedNetwork == null && HtmlSourceUrl.isWebAddress(savedAddress)
                        && matchesApiSubnet(link, savedAddress)) savedNetwork = network;
                for (LinkAddress address : link.getLinkAddresses()) {
                    if (!(address.getAddress() instanceof Inet4Address)) continue;
                    LanIpv4Subnet subnet;
                    try { subnet = new LanIpv4Subnet(address.getAddress().getHostAddress(), address.getPrefixLength()); }
                    catch (IllegalArgumentException notLocal) { continue; }
                    limited |= subnet.prefix < 24;
                    for (String host : subnet.candidates()) {
                        String root = "http://" + host + ":" + HtmlSourceUrl.DEFAULT_PORT;
                        if (ownedBy(apis, root)) continue;
                        String[] alternatives = new String[paths.size() - 1];
                        for (int i = 1; i < paths.size(); ++i) alternatives[i - 1] = root + paths.get(i);
                        if (!add(targets, network, root + paths.get(0), alternatives)) { limited = true; break; }
                    }
                }
            }
        } catch (SecurityException unavailable) { targets.clear(); fallbackApiNetwork = null; apiNetworks.clear(); }
        // Explicitly chosen extra ranges are public probes, never authenticated API guesses.
        if (fallbackApiNetwork != null) try {
            String prefix = HtmlSearchSettings.normalize(preferences.getString(HtmlSearchSettings.EXTRA_PREFIX, ""));
            if (!prefix.isEmpty()) for (String host : new LanIpv4Subnet(prefix + ".0", 24).candidates()) {
                String root = "http://" + host + ":" + HtmlSourceUrl.DEFAULT_PORT;
                if (ownedBy(apis, root)) continue;
                String[] alternatives = new String[paths.size() - 1];
                for (int i = 1; i < paths.size(); ++i) alternatives[i - 1] = root + paths.get(i);
                if (!add(targets, fallbackApiNetwork, root + paths.get(0), alternatives)) { limited = true; break; }
            }
        } catch (IllegalArgumentException invalid) { }
        java.util.List<HtmlHttpProbe.Target> work = new ArrayList<>();
        Map<String, SpresenterConnection> apiTargets = new LinkedHashMap<>();
        for (SpresenterConnection api : apis) {
            Network selected = apiNetworks.containsKey(api.origin) ? apiNetworks.get(api.origin) : fallbackApiNetwork;
            if (selected == null) continue;
            work.add(new HtmlHttpProbe.Target(api.endpoint(),
                    address -> (HttpURLConnection) selected.openConnection(address, Proxy.NO_PROXY)));
            apiTargets.put(api.endpoint(), api);
        }
        if (fallbackApiNetwork != null && HtmlSourceUrl.isWebAddress(savedAddress) && !ownedBy(apis, savedAddress)) {
            // The selected URL is also retried through a routed LAN, outside the local /24.
            Network selected = savedNetwork != null ? savedNetwork : fallbackApiNetwork;
            work.add(new HtmlHttpProbe.Target(savedAddress,
                    address -> (HttpURLConnection) selected.openConnection(address, Proxy.NO_PROXY)));
        }
        for (HtmlHttpProbe.Target target : targets.values()) {
            if (work.size() >= MAX_TARGETS) { limited = true; break; }
            work.add(target);
        }
        if (work.isEmpty()) { dispatch(current, listener::onUnavailable); return; }
        int total = work.size();
        boolean scanLimited = limited;
        dispatch(current, () -> listener.onStarted(total, scanLimited));
        if (!publicOnly && apiSettings.origins().size() > apis.size()) {
            dispatch(current, () -> listener.onCatalogError(R.string.spresenter_api_key_required));
        }
        HtmlHttpProbe probe = new HtmlHttpProbe();
        session = new HtmlDiscoverySession(work, (target, cancellation) -> {
            SpresenterConnection api = apiTargets.get(target.url);
            if (api != null) {
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

    private static boolean ownedBy(java.util.List<SpresenterConnection> connections, String address) {
        for (SpresenterConnection connection : connections) if (connection.owns(address)) return true;
        return false;
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
