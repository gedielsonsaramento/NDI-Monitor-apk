package br.com.polegar.ndimonitor;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import java.net.Inet4Address;

/** Selects an address actually assigned to Wi-Fi/Ethernet; never listens on every interface. */
final class SpresenterSetupNetwork {
    static String address(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return null;
        try {
            Network preferred = manager.getActiveNetwork();
            String first = address(manager, preferred);
            if (first != null) return first;
            for (Network network : manager.getAllNetworks()) {
                if (network.equals(preferred)) continue;
                first = address(manager, network);
                if (first != null) return first;
            }
        } catch (SecurityException unavailable) { return null; }
        return null;
    }

    private static String address(ConnectivityManager manager, Network network) {
        if (network == null) return null;
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        if (capabilities == null || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                 !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) return null;
        LinkProperties link = manager.getLinkProperties(network);
        if (link != null) for (LinkAddress address : link.getLinkAddresses()) {
            if (!(address.getAddress() instanceof Inet4Address)) continue;
            String host = address.getAddress().getHostAddress();
            try { new LanIpv4Subnet(host, address.getPrefixLength()); return host; }
            catch (IllegalArgumentException ignored) { }
        }
        return null;
    }
}
