/* ========================================================================
 * Copyright 2019 SolarNetwork Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */

package net.solarnetwork.esi.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.util.FileCopyUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.esi.domain.KeyPairStore;

/**
 * Cryptographic utilities.
 * 
 * @author matt
 * @version 1.0
 */
public final class CryptoUtils {

  /** A standard UTF-8 charset. */
  public static final Charset STANDARD_CHARSET = Charset.forName("UTF-8");

  /** A standard crypto helper. */
  public static final CryptoHelper STANDARD_HELPER = new EcCryptoHelper();

  /** The standard password-based-encryption secret key algorithm to use. */
  public static final String STANDARD_PBE_SECRET_KEY_ALG = "PBKDF2WithHmacSHA1";

  /** The standard password-based-encryption cipher algorithm to use. */
  public static final String STANDARD_PBE_CIPHER_ALG = "AES/GCM/NoPadding";

  /**
   * Save a key pair from to an output stream.
   * 
   * <p>
   * The {@link #STANDARD_PBE_SECRET_KEY_ALG} key derivation algorithm and
   * {@link #STANDARD_PBE_CIPHER_ALG} cipher algorithm are used.
   * </p>
   * 
   * @param out
   *        the output stream to save the key pair to
   * @param keyPair
   *        the key pair to save
   * @param password
   *        the password used to encrypt the keys
   * @param salt
   *        the key salt to use
   * @param iv
   *        the GCM mode initialization vector to use
   * @throws RuntimeException
   *         if any error occurs
   */
  public static final void saveKeyPair(OutputStream out, KeyPair keyPair, String password,
      byte[] salt, byte[] iv) {
    try (OutputStream cos = new CipherOutputStream(out,
        createPasswordBasedCipher(password.toCharArray(), salt, iv, STANDARD_PBE_SECRET_KEY_ALG,
            STANDARD_PBE_CIPHER_ALG, false))) {
      byte[] data = new ObjectMapper().writeValueAsBytes(new KeyPairStore(keyPair));
      cos.write(data);
    } catch (IOException e) {
      throw new RuntimeException("Error loading key pair: " + e.getMessage(), e);
    }
  }

  /**
   * Load a key pair from an input stream.
   * 
   * <p>
   * The {@link #STANDARD_PBE_SECRET_KEY_ALG} key derivation algorithm and
   * {@link #STANDARD_PBE_CIPHER_ALG} cipher algorithm are used.
   * </p>
   * 
   * @param in
   *        the input stream to load the key pair from
   * @param password
   *        the password used to decrypt the keys
   * @param salt
   *        the key salt to use
   * @param iv
   *        the GCM mode initialization vector to use
   * @return the key pair
   * @throws RuntimeException
   *         if any error occurs
   */
  public static final KeyPair loadKeyPair(InputStream in, String password, byte[] salt, byte[] iv) {
    try (InputStream cis = new CipherInputStream(in,
        createPasswordBasedCipher(password.toCharArray(), salt, iv, STANDARD_PBE_SECRET_KEY_ALG,
            STANDARD_PBE_CIPHER_ALG, true))) {
      byte[] data = FileCopyUtils.copyToByteArray(cis);
      KeyPairStore keyStore = new ObjectMapper().readValue(data, KeyPairStore.class);
      return keyStore.asKeyPair();
    } catch (IOException e) {
      throw new RuntimeException("Error loading key pair: " + e.getMessage(), e);
    }
  }

  /**
   * Create a cipher suitable for password-based encryption.
   * 
   * @param password
   *        the password
   * @param salt
   *        the key salt to use
   * @param iv
   *        the GCM mode initialization vector to use
   * @param secretKeyAlg
   *        the algorithm to use for deriving the encryption key with; must support password-based
   *        encryption
   * @param cipherAlg
   *        the algorithm to use for encryption; must support GCM mode and password-based encryption
   * @param decrypt
   *        {@literal true} to decrypt, {@literal false} to encrypt
   * @return the cipher
   * @throws RuntimeException
   *         if any error occurs
   */
  public static Cipher createPasswordBasedCipher(char[] password, byte[] salt, byte[] iv,
      String secretKeyAlg, String cipherAlg, boolean decrypt) {
    try {
      PBEKeySpec keySpec = new PBEKeySpec(password, salt, 65536, 256);
      SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(secretKeyAlg);
      SecretKey key = keyFactory.generateSecret(keySpec);

      AlgorithmParameterSpec cipherSpec = new GCMParameterSpec(128, iv);
      Key k = new SecretKeySpec(key.getEncoded(), "AES");
      Cipher cipher = Cipher.getInstance(cipherAlg);
      if (decrypt) {
        cipher.init(Cipher.DECRYPT_MODE, k, cipherSpec);
      } else {
        cipher.init(Cipher.ENCRYPT_MODE, k, cipherSpec);
      }
      return cipher;
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeySpecException
        | InvalidKeyException | InvalidAlgorithmParameterException e) {
      throw new RuntimeException("Error creating password-based cipher: " + e.getMessage(), e);
    }
  }

}
