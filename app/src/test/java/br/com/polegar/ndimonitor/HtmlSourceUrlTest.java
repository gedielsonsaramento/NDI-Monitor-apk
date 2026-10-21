package br.com.polegar.ndimonitor;

import org.junit.Test;
import static org.junit.Assert.*;

public class HtmlSourceUrlTest {
    @Test public void expandsPcAddressWithStagePortAndPath() {
        assertEquals("http://192.168.2.53:5050/output/saida-html-1",
                HtmlSourceUrl.normalize(" 192.168.2.53 "));
        assertEquals("http://pc.local:6060/output/saida-html-1",
                HtmlSourceUrl.normalize("pc.local:6060"));
    }

    @Test public void keepsCompleteCustomUrlIncludingQueryAndEscapes() {
        String url = "https://pc.local:6060/output/retorno%20palco?output=2&tema=a%2Fb#tela";
        assertEquals(url, HtmlSourceUrl.normalize(url));
        assertEquals("http://pc.local/", HtmlSourceUrl.normalize("http://pc.local/"));
        assertEquals("http://pc.local:5050/slide?output=0",
                HtmlSourceUrl.normalize("pc.local/slide?output=0"));
    }

    @Test public void acceptsIpv6OnlyWithExplicitBrackets() {
        assertEquals("http://[2001:db8::1]:5050/output/saida-html-1",
                HtmlSourceUrl.normalize("[2001:db8::1]"));
    }

    @Test public void rejectsNonWebSchemesCredentialsAndInvalidAddresses() {
        for (String input : new String[]{null, "", " ", "javascript:alert(1)",
                "file:///sdcard/a.html", "content://media/a", "data:text/html,a",
                "ftp://pc.local/a", "intent://pc.local/a", "http://user:pass@pc.local/",
                "http://pc.local:65536/", "http://pc.local:0/", "http://bad address/",
                "http://pc.local/\nprivate", "http://", "http://pc.local/%zz"}) {
            try {
                HtmlSourceUrl.normalize(input);
                fail("Should reject: " + input);
            } catch (IllegalArgumentException expected) { }
        }
    }
}
