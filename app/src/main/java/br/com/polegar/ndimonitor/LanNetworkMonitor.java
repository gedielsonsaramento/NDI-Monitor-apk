package br.com.polegar.ndimonitor;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Watches LAN address changes without requiring location, SSID or mobile network access. */
final class LanNetworkMonitor {
    private final ConnectivityManager connectivity;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable changed;
    private ConnectivityManager.NetworkCallback callback;
    private boolean running;
    private String last = "";
    private Runnable delivery;
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (!running) return;
            check();
            main.postDelayed(this, 10000); // Some TV firmware misses link-property callbacks.
        }
    };

    LanNetworkMonitor(Context context, Runnable changed) {
        connectivity = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        this.changed = changed;
    }

    void start() {
        if (running) return;
        running = true;
        ConnectivityManager.NetworkCallback current = new ConnectivityManager.NetworkCallback() {
            private void update() { main.post(() -> { if (running && callback == this) check(); }); }
            @Override public void onAvailable(Network network) { update(); }
            @Override public void onLost(Network network) { update(); }
            @Override public void onLinkPropertiesChanged(Network network, LinkProperties properties) { update(); }
            @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) { update(); }
        };
        callback = current;
        if (connectivity != null) try {
            connectivity.registerNetworkCallback(new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET).build(), current);
        } catch (RuntimeException unavailable) { /* Periodic checks also cover restricted TV firmware. */ }
        poll.run();
    }

    private void check() {
        String next = snapshot();
        if (next.equals(last)) return;
        last = next;
        if (delivery != null) main.removeCallbacks(delivery);
        delivery = null;
        if (next.isEmpty()) return;
        delivery = () -> { delivery = null; if (running && next.equals(last)) changed.run(); };
        main.postDelayed(delivery, 800); // Coalesce availability, address and route notifications.
    }

    private String snapshot() {
        List<String> links = new ArrayList<>();
        if (connectivity != null) try {
            for (Network network : connectivity.getAllNetworks()) {
                NetworkCapabilities caps = connectivity.getNetworkCapabilities(network);
                if (caps == null || caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                        (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                         !caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) continue;
                LinkProperties link = connectivity.getLinkProperties(network);
                if (link == null) continue;
                for (LinkAddress address : link.getLinkAddresses()) {
                    if (!(address.getAddress() instanceof Inet4Address)) continue;
                    try {
                        new LanIpv4Subnet(address.getAddress().getHostAddress(), address.getPrefixLength());
                        links.add(network + "|" + address.toString());
                    } catch (IllegalArgumentException ignored) { }
                }
            }
        } catch (RuntimeException unavailable) { return ""; }
        Collections.sort(links);
        return links.isEmpty() ? "" : links.toString();
    }

    void stop() {
        running = false;
        main.removeCallbacksAndMessages(null);
        delivery = null;
        ConnectivityManager.NetworkCallback previous = callback;
        callback = null;
        if (connectivity != null && previous != null) try { connectivity.unregisterNetworkCallback(previous); }
        catch (RuntimeException ignored) { }
    }
}
