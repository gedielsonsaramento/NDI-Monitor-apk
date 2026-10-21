package br.com.polegar.ndimonitor;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class SourceCatalogTest {
    @Test public void laterSourcesAppearWithoutClearingEarlierOnes() {
        SourceCatalog catalog = new SourceCatalog();
        assertEquals(Collections.singletonList("Palco"), catalog.update(new String[]{"Palco"}, 0));
        assertEquals(Arrays.asList("Palco", "Camera"),
                catalog.update(new String[]{"Camera", "Palco"}, 500));
    }

    @Test public void emptyRefreshPreservesSourcesDuringTransientDiscoveryGap() {
        SourceCatalog catalog = new SourceCatalog();
        catalog.update(new String[]{"Palco"}, 100);
        assertEquals(Collections.singletonList("Palco"), catalog.update(new String[0], 500));
        assertEquals(Arrays.asList("Palco", "Camera"), catalog.update(new String[]{"Camera"}, 1000));
    }

    @Test public void permanentlyMissingSourceExpiresButLiveSourceStays() {
        SourceCatalog catalog = new SourceCatalog();
        catalog.update(new String[]{"Palco", "Camera"}, 0);
        assertEquals(Arrays.asList("Palco", "Camera"), catalog.update(new String[]{"Camera"}, 9999));
        assertEquals(Collections.singletonList("Camera"), catalog.update(new String[]{"Camera"}, 10000));
    }

    @Test public void repeatedSnapshotsDoNotReorderSourcesOrDuplicateThem() {
        SourceCatalog catalog = new SourceCatalog();
        catalog.update(new String[]{"B", "C"}, 0);
        assertEquals(Arrays.asList("B", "C", "A"),
                catalog.update(new String[]{"A", "B", "B", "C", "", " ", null}, 500));
        assertEquals(Arrays.asList("B", "C", "A"), catalog.update(new String[]{"C", "A", "B"}, 1000));
    }

    @Test public void reappearingSourceGetsFreshGracePeriod() {
        SourceCatalog catalog = new SourceCatalog();
        catalog.update(new String[]{"Palco"}, 0);
        catalog.update(new String[]{"Palco"}, 9000);
        assertEquals(Collections.singletonList("Palco"), catalog.update(null, 18000));
        assertEquals(Collections.emptyList(), catalog.update(null, 19000));
    }

    @Test public void returnedSnapshotsCannotModifyDiscoveryState() {
        SourceCatalog catalog = new SourceCatalog();
        catalog.update(new String[]{"Palco"}, 0).clear();
        assertEquals(Collections.singletonList("Palco"), catalog.update(null, 1));
    }

    @Test public void refreshAfterPauseDoesNotHideKnownSourcesWhileFinderRestarts() {
        SourceCatalog catalog = new SourceCatalog();
        catalog.update(new String[]{"Palco", "Camera"}, 0);
        catalog.beginRefresh(60000);
        assertEquals(Arrays.asList("Palco", "Camera"), catalog.update(new String[0], 60001));
        assertEquals(Arrays.asList("Palco", "Camera"), catalog.update(new String[]{"Camera"}, 65000));
        assertEquals(Collections.singletonList("Camera"), catalog.update(new String[]{"Camera"}, 70000));
    }
}
