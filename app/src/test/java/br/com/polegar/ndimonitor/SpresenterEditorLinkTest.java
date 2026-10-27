package br.com.polegar.ndimonitor;

import org.junit.Test;
import static org.junit.Assert.*;

public class SpresenterEditorLinkTest {
    private static final String KEY = "spk_synthetic_setup_key_123456";

    @Test public void extractsAddressAndTokenWithoutOpeningEditor() {
        SpresenterConnection result = SpresenterConnection.fromEditorLink(
                "  http://192.168.2.53:5050/warp/?token=" + KEY + "\n");
        assertEquals("http://192.168.2.53:5050", result.origin);
        assertEquals(KEY, result.token);
        assertEquals("http://192.168.2.53:5050/api/v1/advanced", result.endpoint());
        assertEquals("https://pc.example", SpresenterConnection.fromEditorLink(
                "https://PC.EXAMPLE/warp?token=" + KEY).origin);
        assertEquals(KEY, SpresenterConnection.fromEditorLink(
                "http://192.168.2.53:5050/warp/?token=spk_%73ynthetic_setup_key_123456").token);
    }

    @Test public void rejectsOtherUrlsAmbiguousCredentialsAndInvalidKeysWithoutEchoingSecrets() {
        String[] invalid = {null, "", "/warp/?token=" + KEY,
                "ftp://pc.example/warp/?token=" + KEY, "javascript:alert(1)",
                "http://user@pc.example/warp/?token=" + KEY,
                "http://pc.example:0/warp/?token=" + KEY,
                "http://pc.example/output/retorno?token=" + KEY,
                "http://pc.example/warp/?token=" + KEY + "&token=spk_other_key_123456",
                "http://pc.example/warp/?token=" + KEY + "#fragment",
                "http://pc.example/warp/?token=bad", "http://pc.example/warp/?token=%zz",
                "http://pc.example/warp/?other=" + KEY,
                "http://pc.example/warp/?token=spk_key%0A12345678"};
        for (String link : invalid) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> SpresenterConnection.fromEditorLink(link));
            assertEquals("editor_link", error.getMessage());
            assertNull(error.getCause());
        }
    }
}
