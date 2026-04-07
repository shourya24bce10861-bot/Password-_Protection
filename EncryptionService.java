package service;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Handles AES-256-CBC encryption and decryption of note content.
 *
 * How it works:
 *   1. The user's plain-text password is used to derive a 256-bit AES key
 *      via PBKDF2WithHmacSHA256 (with a fixed salt per user — ideally store
 *      a random salt in the DB for production use).
 *   2. Each encrypt call generates a random 16-byte IV.
 *   3. The IV is prepended to the ciphertext and Base64-encoded for storage.
 *   4. On decryption the IV is extracted, and the rest is decrypted.
 *
 * Note: This is the Java Cryptography Architecture (JCA/JCE) at work.
 */
public class EncryptionService {

    private static final String ALGORITHM     = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int    KEY_LENGTH    = 256;   // bits
    private static final int    IV_LENGTH     = 16;    // bytes
    private static final int    ITERATIONS    = 65536;

    // In production: store a unique random salt per user in the DB.
    // Here we use the username as a deterministic salt for simplicity.
    private static byte[] deriveSalt(String username) {
        byte[] salt = new byte[16];
        byte[] uBytes = username.getBytes();
        for (int i = 0; i < 16; i++) {
            salt[i] = uBytes[i % uBytes.length];
        }
        return salt;
    }

    /**
     * Derives a 256-bit AES SecretKey from the user's password.
     */
    public static SecretKey deriveKey(String password, String username)
            throws NoSuchAlgorithmException, java.security.spec.InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        KeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            deriveSalt(username),
            ITERATIONS,
            KEY_LENGTH
        );
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts plain text using AES-256-CBC.
     * Returns Base64(IV + ciphertext) for safe DB storage.
     */
    public static String encrypt(String plainText, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);

        // Generate a fresh random IV for each encryption
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] cipherBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

        // Prepend IV to ciphertext: [IV (16 bytes)][CIPHERTEXT]
        byte[] combined = new byte[IV_LENGTH + cipherBytes.length];
        System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
        System.arraycopy(cipherBytes, 0, combined, IV_LENGTH, cipherBytes.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypts Base64(IV + ciphertext) back to plain text.
     */
    public static String decrypt(String encryptedBase64, SecretKey key) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedBase64);

        // Extract IV from first 16 bytes
        byte[] iv         = new byte[IV_LENGTH];
        byte[] cipherBytes = new byte[combined.length - IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
        System.arraycopy(combined, IV_LENGTH, cipherBytes, 0, cipherBytes.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] plainBytes = cipher.doFinal(cipherBytes);

        return new String(plainBytes, "UTF-8");
    }

    /**
     * Quick utility: encrypt with password + username (no pre-derived key needed).
     */
    public static String encryptWithPassword(String text, String password, String username) {
        try {
            return encrypt(text, deriveKey(password, username));
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Quick utility: decrypt with password + username.
     */
    public static String decryptWithPassword(String cipherText, String password, String username) {
        try {
            return decrypt(cipherText, deriveKey(password, username));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
