package br.com.polegar.ndimonitor;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** API 23+ authenticated encryption. The server origin is bound as associated data. */
final class SpresenterTokenCipher {
    interface Keys { SecretKey get() throws GeneralSecurityException, java.io.IOException; }
    private static final String ALIAS = "spresenter-catalog-read-v1";
    private final Keys keys;

    SpresenterTokenCipher() { this(SpresenterTokenCipher::androidKey); }
    SpresenterTokenCipher(Keys keys) { this.keys = keys; }

    private static SecretKey androidKey() throws GeneralSecurityException, java.io.IOException {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }

    String encrypt(String origin, String token) throws GeneralSecurityException, java.io.IOException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keys.get());
        cipher.updateAAD(origin.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":" +
                Base64.encodeToString(cipher.doFinal(token.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
    }

    String decrypt(String origin, String saved) throws GeneralSecurityException, java.io.IOException {
        try {
            String[] parts = saved.split(":", -1);
            if (parts.length != 2) throw new GeneralSecurityException("Invalid encrypted credential");
            byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
            if (iv.length != 12) throw new GeneralSecurityException("Invalid IV");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keys.get(), new GCMParameterSpec(128, iv));
            cipher.updateAAD(origin.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException invalid) { throw new GeneralSecurityException("Invalid encrypted credential"); }
    }
}
