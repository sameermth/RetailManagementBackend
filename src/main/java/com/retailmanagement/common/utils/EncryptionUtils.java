package com.retailmanagement.common.utils;

import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

@Component
public class EncryptionUtils {

    private static final String SECRET_KEY = "mySuperSecretKeyForEncryption";
    private static final String SALT = "mySaltValue";
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    public String encrypt(String value) throws Exception {
        SecretKeySpec secretKey = generateKey();
        IvParameterSpec iv = generateIv();

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

        byte[] cipherText = cipher.doFinal(value.getBytes());
        byte[] ivAndCipherText = new byte[iv.getIV().length + cipherText.length];
        System.arraycopy(iv.getIV(), 0, ivAndCipherText, 0, iv.getIV().length);
        System.arraycopy(cipherText, 0, ivAndCipherText, iv.getIV().length, cipherText.length);

        return Base64.getEncoder().encodeToString(ivAndCipherText);
    }

    public String decrypt(String encrypted) throws Exception {
        byte[] ivAndCipherText = Base64.getDecoder().decode(encrypted);
        byte[] iv = new byte[16];
        byte[] cipherText = new byte[ivAndCipherText.length - iv.length];

        System.arraycopy(ivAndCipherText, 0, iv, 0, iv.length);
        System.arraycopy(ivAndCipherText, iv.length, cipherText, 0, cipherText.length);

        SecretKeySpec secretKey = generateKey();
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

        byte[] plainText = cipher.doFinal(cipherText);
        return new String(plainText);
    }

    private SecretKeySpec generateKey() throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SALT.getBytes(), 65536, 256);
        SecretKey secretKey = factory.generateSecret(spec);
        return new SecretKeySpec(secretKey.getEncoded(), "AES");
    }

    private IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    public String hashPassword(String password) {
        // Use BCrypt or similar for password hashing
        return org.springframework.security.crypto.bcrypt.BCrypt.hashpw(password,
                org.springframework.security.crypto.bcrypt.BCrypt.gensalt());
    }

    public boolean verifyPassword(String password, String hashedPassword) {
        return org.springframework.security.crypto.bcrypt.BCrypt.checkpw(password, hashedPassword);
    }
}