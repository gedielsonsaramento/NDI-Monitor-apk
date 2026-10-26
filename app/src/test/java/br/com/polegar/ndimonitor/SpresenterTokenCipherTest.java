package br.com.polegar.ndimonitor;

import android.content.Context;
import android.content.SharedPreferences;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class SpresenterTokenCipherTest {
    private static final String ROOT="http://192.168.2.53:5050", KEY="spk_example_read_only_token";
    private SecretKey key() throws Exception { KeyGenerator g=KeyGenerator.getInstance("AES");g.init(128);return g.generateKey(); }

    @Test public void encryptsWithDifferentIvAndBindsCiphertextToExactOrigin() throws Exception {
        SecretKey secret=key(); SpresenterTokenCipher cipher=new SpresenterTokenCipher(()->secret);
        String saved=cipher.encrypt(ROOT,KEY), other=cipher.encrypt(ROOT,KEY);
        assertNotEquals(saved,other);assertFalse(saved.contains(KEY));
        assertEquals(KEY,cipher.decrypt(ROOT,saved));
        try { cipher.decrypt("http://192.168.2.54:5050",saved); fail(); }
        catch(java.security.GeneralSecurityException expected) { }
        char last=saved.charAt(saved.length()-4);
        String changed=saved.substring(0,saved.length()-4)+(last=='a'?'b':'a')+saved.substring(saved.length()-3);
        try { cipher.decrypt(ROOT,changed); fail(); } catch(java.security.GeneralSecurityException expected) { }
    }

    @Test public void persistsEncryptedTokenAndNeverStoresItInPlaintext() throws Exception {
        SharedPreferences prefs=RuntimeEnvironment.getApplication().getSharedPreferences("api-crypto",Context.MODE_PRIVATE);
        prefs.edit().clear().commit();SecretKey secret=key();
        SpresenterApiSettings settings=new SpresenterApiSettings(prefs,"test.persist",new SpresenterTokenCipher(()->secret));
        assertTrue(settings.save(new SpresenterConnection(ROOT,KEY)));
        assertFalse(prefs.getAll().toString().contains(KEY));
        assertEquals(KEY,settings.load().token);
        // A separate process cache identity forces decryption from the saved bytes.
        SpresenterApiSettings restored=new SpresenterApiSettings(prefs,"test.restored",new SpresenterTokenCipher(()->secret));
        assertEquals(KEY,restored.load().token);
        restored.clear();assertNull(restored.load());assertFalse(prefs.contains(SpresenterApiSettings.SECRET));
    }

    @Test public void unsupportedKeystoreKeepsOnlySessionCredentialAndClearRemovesIt() {
        SharedPreferences prefs=RuntimeEnvironment.getApplication().getSharedPreferences("api-session",Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        SpresenterApiSettings settings=new SpresenterApiSettings(prefs,"test.session",new SpresenterTokenCipher(()->{
            throw new java.security.GeneralSecurityException("unavailable");}));
        assertFalse(settings.save(new SpresenterConnection(ROOT,KEY)));
        assertEquals(KEY,settings.load().token);assertFalse(prefs.contains(SpresenterApiSettings.SECRET));
        SpresenterApiSettings nextProcess=new SpresenterApiSettings(prefs,"test.new.process",new SpresenterTokenCipher());
        assertNull(nextProcess.load());
        settings.clear();assertNull(settings.load());
    }
}
