package br.com.polegar.ndimonitor;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class HtmlPageDiagnosticsTest {
    @Test public void formatsQuotedJavascriptResultAndMarksMissingCssSupport() {
        String data = "{\"width\":1920,\"height\":1080,\"dpr\":1,\"grid\":false,\"variables\":true,\"clamp\":false,\"state\":\"complete\"}";
        String result = HtmlPageDiagnostics.format(JSONObject.quote(data));
        assertTrue(result.contains("1920 × 1080"));
        assertTrue(result.contains("CSS Grid: não; variáveis: sim; clamp: não"));
    }

    @Test public void invalidOrNullJavascriptResultRemainsReadable() {
        assertEquals("Página: dados indisponíveis", HtmlPageDiagnostics.format("null"));
        assertEquals("Página: dados indisponíveis", HtmlPageDiagnostics.format("invalid"));
    }
}
