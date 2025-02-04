package com.hanhome.youtube_comments.utils;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class AESUtil {
    private final SecretKeySpec secretKey;

    public AESUtil(SecretKeySpec secretKey) {
        this.secretKey = secretKey;
    }

    public byte[] generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    public String encrypt(String data) throws Exception {
        byte[] ivBytes = generateIv();
        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);

        byte[] encryptedData = cipher.doFinal(data.getBytes());

        byte[] combinedBytes = new byte[ivBytes.length + encryptedData.length];
        System.arraycopy(ivBytes,0, combinedBytes, 0, ivBytes.length);
        System.arraycopy(encryptedData, 0, combinedBytes, ivBytes.length, encryptedData.length);
        return Base64.getEncoder().encodeToString(combinedBytes);
    }

    public String decrypt(String encryptedData) throws Exception {
        byte[] combinedBytes = Base64.getDecoder().decode(encryptedData);

        byte[] ivBytes = new byte[16];
        byte[] encryptedBytes = new byte[combinedBytes.length - ivBytes.length];
        System.arraycopy(combinedBytes, 0, ivBytes, 0, ivBytes.length);
        System.arraycopy(combinedBytes, ivBytes.length, encryptedBytes, 0, encryptedBytes.length);

        IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

        byte[] decryptedData = cipher.doFinal(encryptedBytes);
        return new String(decryptedData);
    }
}
