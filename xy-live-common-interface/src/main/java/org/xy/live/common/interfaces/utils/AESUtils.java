package org.xy.live.common.interfaces.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class AESUtils {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    // 完全保留你原来的方法签名
    public static String encrypt(String content, String salt) throws Exception {
        byte[] key = deriveKey(salt);
        byte[] iv = deriveIv(salt);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decrypt(String encryptedContent, String salt) throws Exception {
        byte[] key = deriveKey(salt);
        byte[] iv = deriveIv(salt);
        byte[] encrypted = Base64.getDecoder().decode(encryptedContent);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    // 相同salt = 相同key
    private static byte[] deriveKey(String salt) {
        byte[] s = salt.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[16];
        for (int i = 0; i < 16; i++) {
            key[i] = i < s.length ? s[i] : (byte) (i * 7 + 13);
        }
        return key;
    }

    // 相同salt = 相同iv（核心！保证确定性加密）
    private static byte[] deriveIv(String salt) {
        byte[] s = salt.getBytes(StandardCharsets.UTF_8);
        byte[] iv = new byte[16];
        for (int i = 0; i < 16; i++) {
            iv[i] = s[i % s.length];
        }
        return iv;
    }

    public static void main(String[] args) throws Exception {
        String encrypt1 = encrypt("15656565656", "test");
        String encrypt2 = encrypt("15656565456", "test");
        System.out.println(encrypt1);
        System.out.println(encrypt2);
    }
}