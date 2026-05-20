package backend.service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class TokenEncryptionService {

  private static final Logger log = LoggerFactory.getLogger(TokenEncryptionService.class);
  private static final int GCM_TAG_BITS = 128;
  private static final int GCM_IV_BYTES = 12;

  private final String configuredKey;
  private final Environment environment;
  private final SecureRandom secureRandom = new SecureRandom();

  private SecretKeySpec keySpec;

  public TokenEncryptionService(
      @Value("${app.delivery.token-encryption-key:}") String configuredKey,
      Environment environment
  ) {
    this.configuredKey = configuredKey;
    this.environment = environment;
  }

  @PostConstruct
  void init() {
    if (configuredKey == null || configuredKey.isBlank()) {
      if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
        throw new IllegalStateException("APP_TOKEN_ENCRYPTION_KEY must be set in prod profile");
      }
      log.warn("APP_TOKEN_ENCRYPTION_KEY is not configured; mail OAuth token storage is disabled");
      return;
    }
    keySpec = new SecretKeySpec(resolveKeyBytes(configuredKey.trim()), "AES");
  }

  public String encrypt(String plaintext) {
    if (plaintext == null) {
      return null;
    }
    requireKey();
    try {
      byte[] iv = new byte[GCM_IV_BYTES];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      byte[] combined = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
      return Base64.getEncoder().encodeToString(combined);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to encrypt OAuth token", ex);
    }
  }

  public String decrypt(String encryptedValue) {
    if (encryptedValue == null) {
      return null;
    }
    requireKey();
    try {
      byte[] combined = Base64.getDecoder().decode(encryptedValue);
      if (combined.length <= GCM_IV_BYTES) {
        throw new IllegalArgumentException("Invalid encrypted token payload");
      }

      byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_BYTES);
      byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_BYTES, combined.length);

      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to decrypt OAuth token", ex);
    }
  }

  private void requireKey() {
    if (keySpec == null) {
      throw new IllegalStateException("APP_TOKEN_ENCRYPTION_KEY is required before storing mail OAuth tokens");
    }
  }

  private byte[] resolveKeyBytes(String raw) {
    byte[] decoded = tryBase64(raw);
    if (isAesKeyLength(decoded)) {
      return decoded;
    }

    byte[] utf8 = raw.getBytes(StandardCharsets.UTF_8);
    if (isAesKeyLength(utf8)) {
      return utf8;
    }

    try {
      return MessageDigest.getInstance("SHA-256").digest(utf8);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to derive AES key", ex);
    }
  }

  private byte[] tryBase64(String raw) {
    try {
      return Base64.getDecoder().decode(raw);
    } catch (IllegalArgumentException ignored) {
      return new byte[0];
    }
  }

  private boolean isAesKeyLength(byte[] bytes) {
    return bytes != null && (bytes.length == 16 || bytes.length == 24 || bytes.length == 32);
  }
}
