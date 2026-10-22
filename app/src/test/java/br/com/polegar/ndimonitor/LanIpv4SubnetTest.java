package br.com.polegar.ndimonitor;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class LanIpv4SubnetTest {
    @Test public void scansPrivateSubnetWithoutSelfNetworkOrBroadcast() {
        List<String> hosts = new LanIpv4Subnet("192.168.2.12", 24).candidates();
        assertEquals(253, hosts.size());
        assertTrue(hosts.contains("192.168.2.53"));
        assertFalse(hosts.contains("192.168.2.12"));
        assertFalse(hosts.contains("192.168.2.0"));
        assertFalse(hosts.contains("192.168.2.255"));
    }
    @Test public void largeLanIsBoundedButRemembersAddressesElsewhereOnSameLink() {
        LanIpv4Subnet subnet = new LanIpv4Subnet("10.20.30.40", 16);
        assertEquals(253, subnet.candidates().size());
        assertTrue(subnet.candidates().stream().allMatch(ip -> ip.startsWith("10.20.30.")));
        assertTrue(subnet.contains("10.20.80.10"));
        assertFalse(subnet.contains("10.21.30.10"));
    }
    @Test public void smallSubnetsAndPointToPointAreHandled() {
        assertEquals(1, new LanIpv4Subnet("172.16.2.1", 30).candidates().size());
        assertEquals("172.16.2.2", new LanIpv4Subnet("172.16.2.1", 30).candidates().get(0));
        assertEquals(1, new LanIpv4Subnet("169.254.1.4", 31).candidates().size());
        assertTrue(new LanIpv4Subnet("192.168.1.4", 32).candidates().isEmpty());
    }
    @Test public void rejectsPublicLoopbackMulticastAndHostnames() {
        for (String value : new String[]{"8.8.8.8", "127.0.0.1", "224.0.0.1", "pc.local", "192.168.1.256", "1.2.3.-1"}) {
            try { new LanIpv4Subnet(value, 24); fail(value); }
            catch (IllegalArgumentException expected) { }
        }
        assertFalse(new LanIpv4Subnet("192.168.1.2", 24).contains("pc.local"));
    }
}
