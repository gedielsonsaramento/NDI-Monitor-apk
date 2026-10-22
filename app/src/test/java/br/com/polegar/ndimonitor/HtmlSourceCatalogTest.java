package br.com.polegar.ndimonitor;

import org.junit.Test;
import static org.junit.Assert.*;

public class HtmlSourceCatalogTest {
    private HtmlSource source(String host) { return new HtmlSource("http://" + host + ":5050/output/saida-html-1", "Stage"); }
    @Test public void repeatedScansKeepOrderAndDoNotDuplicateSources() {
        HtmlSourceCatalog catalog = new HtmlSourceCatalog();
        catalog.found(source("192.168.1.5"), 0);
        catalog.found(source("192.168.1.6"), 100);
        catalog.found(source("192.168.1.5"), 5000);
        catalog.finishScan(10_000);
        assertEquals(2, catalog.snapshot().size());
        assertEquals(source("192.168.1.5").url, catalog.snapshot().get(0).url);
        catalog.finishScan(95_000);
        assertTrue(catalog.snapshot().isEmpty());
    }
    @Test public void savedSourceSurvivesTemporaryOutageAndLabelsDoNotLeakParameters() {
        HtmlSourceCatalog catalog = new HtmlSourceCatalog();
        String url = "http://192.168.1.5:5050/output/custom?token=secret#stage";
        catalog.remember(url, 0);
        catalog.finishScan(1_000_000);
        assertEquals(url, catalog.snapshot().get(0).url);
        assertFalse(catalog.snapshot().get(0).displayName().contains("secret"));
        catalog.found(new HtmlSource(url, "Retorno do palco"), 1_000_001);
        assertEquals(1, catalog.snapshot().size());
        assertTrue(catalog.snapshot().get(0).displayName().startsWith("Retorno do palco"));
    }
}
