package br.com.polegar.ndimonitor;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={23,25}, shadows=SpresenterSetupNetworkTest.Connections.class)
public class SpresenterSetupNetworkTest {
    @Implements(ConnectivityManager.class)
    public static class Connections {
        static Network active;
        static final Map<Network, NetworkCapabilities> capabilities = new LinkedHashMap<>();
        static final Map<Network, LinkProperties> links = new LinkedHashMap<>();
        @Implementation protected Network getActiveNetwork() { return active; }
        @Implementation protected Network[] getAllNetworks() { return capabilities.keySet().toArray(new Network[0]); }
        @Implementation protected NetworkCapabilities getNetworkCapabilities(Network network) { return capabilities.get(network); }
        @Implementation protected LinkProperties getLinkProperties(Network network) { return links.get(network); }
    }

    @Before public void reset() { Connections.active = null; Connections.capabilities.clear(); Connections.links.clear(); }

    @Test public void selectsWifiEvenWithDefaultMobileData() throws Exception {
        Network mobile = network(1, NetworkCapabilities.TRANSPORT_CELLULAR, "10.1.1.2", 24);
        network(2, NetworkCapabilities.TRANSPORT_WIFI, "192.168.2.91", 24);
        Connections.active = mobile;
        assertEquals("192.168.2.91", SpresenterSetupNetwork.address(RuntimeEnvironment.getApplication()));
    }

    @Test public void prefersActiveEthernetAndSkipsVpn() throws Exception {
        network(1, NetworkCapabilities.TRANSPORT_WIFI, "192.168.3.5", 24);
        Network ethernet = network(2, NetworkCapabilities.TRANSPORT_ETHERNET, "192.168.2.5", 24);
        Connections.active = ethernet;
        assertEquals("192.168.2.5", SpresenterSetupNetwork.address(RuntimeEnvironment.getApplication()));
        transport(Connections.capabilities.get(ethernet), NetworkCapabilities.TRANSPORT_VPN);
        assertEquals("192.168.3.5", SpresenterSetupNetwork.address(RuntimeEnvironment.getApplication()));
    }

    @Test public void doesNotBindLoopbackPublicIpv4OrIpv6AsLanSetupAddress() throws Exception {
        network(1, NetworkCapabilities.TRANSPORT_WIFI, "127.0.0.1", 8);
        network(2, NetworkCapabilities.TRANSPORT_ETHERNET, "203.0.113.5", 24);
        network(3, NetworkCapabilities.TRANSPORT_WIFI, "fe80::1", 64);
        assertNull(SpresenterSetupNetwork.address(RuntimeEnvironment.getApplication()));
    }

    private static Network network(int id, int transport, String host, int prefix) throws Exception {
        Network network = ReflectionHelpers.callConstructor(Network.class, ClassParameter.from(int.class, id));
        NetworkCapabilities capabilities = new NetworkCapabilities();
        transport(capabilities, transport);
        Connections.capabilities.put(network, capabilities);
        LinkProperties properties = new LinkProperties();
        LinkAddress address = ReflectionHelpers.callConstructor(LinkAddress.class,
                ClassParameter.from(InetAddress.class, InetAddress.getByName(host)), ClassParameter.from(int.class, prefix));
        ReflectionHelpers.callInstanceMethod(properties, "addLinkAddress", ClassParameter.from(LinkAddress.class, address));
        Connections.links.put(network, properties);
        return network;
    }

    private static void transport(NetworkCapabilities capabilities, int value) {
        ReflectionHelpers.callInstanceMethod(capabilities, "addTransportType", ClassParameter.from(int.class, value));
    }
}
