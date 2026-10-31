package br.com.polegar.ndimonitor;

/** Explicit extra /24 for a routed or otherwise separately addressed local network. */
final class HtmlSearchSettings {
    static final String EXTRA_PREFIX = "html_search_extra_prefix";
    private HtmlSearchSettings() { }

    static String normalize(String value) {
        String input = value == null ? "" : value.trim();
        if (input.isEmpty()) return "";
        String[] parts = input.split("\\.", -1);
        if (parts.length != 3) throw new IllegalArgumentException("Use a local IPv4 prefix");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.matches("[0-9]{1,3}")) throw new IllegalArgumentException("Invalid prefix");
            int octet = Integer.parseInt(part);
            if (octet > 255) throw new IllegalArgumentException("Invalid prefix");
            if (result.length() > 0) result.append('.');
            result.append(octet);
        }
        new LanIpv4Subnet(result + ".1", 24);
        return result.toString();
    }
}
