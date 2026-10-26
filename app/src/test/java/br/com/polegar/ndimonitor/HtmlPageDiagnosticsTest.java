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

    @Test public void manualProbeAllowsFiveSecondsForSlowLegacyRenderer() {
        try (org.robolectric.android.controller.ActivityController<android.app.Activity> controller =
                     org.robolectric.Robolectric.buildActivity(android.app.Activity.class).setup()) {
            android.app.Activity activity = controller.get();
            HtmlMonitor monitor = new HtmlMonitor(activity, new android.widget.FrameLayout(activity),
                    new HtmlMonitor.Listener() {
                        public void onStatus(int message) { }
                        public void onInteraction() { }
                    });
            monitor.setDisplayLayout(true, true);
            monitor.resume();
            monitor.show("http://192.168.2.52:5050/output/retorno");
            java.util.concurrent.atomic.AtomicReference<String> result = new java.util.concurrent.atomic.AtomicReference<>();
            monitor.describePage(result::set);
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
                    .idleFor(1600, java.util.concurrent.TimeUnit.MILLISECONDS);
            assertNull(result.get());
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
                    .idleFor(3400, java.util.concurrent.TimeUnit.MILLISECONDS);
            assertNotNull(result.get());
            assertTrue(result.get().contains("sem resposta ao diagnóstico"));
            monitor.destroy();
        }
    }
}
