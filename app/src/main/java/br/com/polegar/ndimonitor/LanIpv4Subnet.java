package br.com.polegar.ndimonitor;

import java.util.ArrayList;
import java.util.List;

/** Bounded IPv4 candidate list. Large LANs scan only the device's /24. */
final class LanIpv4Subnet {
    final long address;
    final int prefix;

    LanIpv4Subnet(String address, int prefix) {
        this.address = parse(address);
        if (prefix < 0 || prefix > 32 || !isLocal(this.address)) {
            throw new IllegalArgumentException("Not a private LAN IPv4 address");
        }
        this.prefix = prefix;
    }

    boolean contains(String candidate) {
        try {
            long value = parse(candidate);
            return isLocal(value) && (value & mask(prefix)) == (address & mask(prefix));
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    List<String> candidates() {
        int scanPrefix = Math.max(24, prefix);
        long first = address & mask(scanPrefix);
        long last = first | (0xffffffffL ^ mask(scanPrefix));
        if (scanPrefix <= 30) { ++first; --last; }
        List<String> result = new ArrayList<>();
        for (long host = first; host <= last; ++host) {
            if (host != address && isLocal(host)) result.add(format(host));
        }
        return result;
    }

    static long parse(String value) {
        if (value == null) throw new IllegalArgumentException();
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) throw new IllegalArgumentException();
        long address = 0;
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) throw new IllegalArgumentException();
            for (int i = 0; i < part.length(); ++i) {
                if (part.charAt(i) < '0' || part.charAt(i) > '9') throw new IllegalArgumentException();
            }
            int octet = Integer.parseInt(part);
            if (octet > 255) throw new IllegalArgumentException();
            address = (address << 8) | octet;
        }
        return address;
    }

    private static long mask(int prefix) {
        return prefix == 0 ? 0 : (0xffffffffL << (32 - prefix)) & 0xffffffffL;
    }

    private static boolean isLocal(long value) {
        return (value & 0xff000000L) == 0x0a000000L ||
                (value & 0xfff00000L) == 0xac100000L ||
                (value & 0xffff0000L) == 0xc0a80000L ||
                (value & 0xffff0000L) == 0xa9fe0000L ||
                (value & 0xffc00000L) == 0x64400000L;
    }

    private static String format(long address) {
        return ((address >>> 24) & 255) + "." + ((address >>> 16) & 255) + "." +
                ((address >>> 8) & 255) + "." + (address & 255);
    }
}
