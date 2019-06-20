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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cryptographic utilities.
 * 
 * @author matt
 * @version 1.0
 */
public final class CryptoUtils {

  /** The standard key pair algorithm to use. */
  public static final String STANDARD_KEY_PAIR_ALG = "EC";

  /** The standard elliptic curve name to use. */
  public static final String STANDARD_EC_NAME = "secp256r1";

  /** The standard key agreement algorithm to use. */
  public static final String STANDARD_KEY_AGREEMENT_ALG = "ECDH";

  /** The standard signature algorithm to use. */
  public static final String STANDARD_SIGNATURE_ALG = "SHA256withECDSA";

  /** The standard secret key algorithm to use. */
  public static final String STANDARD_SECRET_KEY_ALG = "AES";

  /** The standard cipher algorithm to use. */
  public static final String STANDARD_CIPHER_ALG = "AES/GCM/NoPadding";

  private static final Logger log = LoggerFactory.getLogger(CryptoUtils.class);

  /**
   * Generate a new public/private key pair using the standard settings defined in this class.
   * 
   * @return the new key pair
   * @throws RuntimeException
   *         if any security exception is thrown
   */
  public static KeyPair generateStandardKeyPair() {
    try {
      KeyPairGenerator g = KeyPairGenerator.getInstance(STANDARD_KEY_PAIR_ALG);
      ECGenParameterSpec spec = new ECGenParameterSpec(STANDARD_EC_NAME);
      g.initialize(spec);
      return g.generateKeyPair();
    } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
      throw new RuntimeException("Unable to generate " + STANDARD_EC_NAME + " key pair", e);
    }
  }

  /**
   * Derive a secret key suitable for symmetric encryption using the standard settings in this
   * class.
   * 
   * @param recipientPublicKey
   *        the recipient's public key
   * @param senderKeyPair
   *        the sender's public/private key pair
   * @return the secret key
   * @throws RuntimeException
   *         if any security exception is thrown
   */
  public static SecretKey deriveStandardSecretKey(PublicKey recipientPublicKey,
      KeyPair senderKeyPair) {
    try {
      KeyAgreement ka = KeyAgreement.getInstance(STANDARD_KEY_AGREEMENT_ALG);
      ka.init(senderKeyPair.getPrivate());
      ka.doPhase(recipientPublicKey, true);
      byte[] sec = ka.generateSecret(); // with BC, could use AES here

      // derive encryption key from shared secret + both public keys, as recommended by libsodium
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(sec);

      // Use simple deterministic ordering of key data
      List<ByteBuffer> keys = Arrays.asList(ByteBuffer.wrap(recipientPublicKey.getEncoded()),
          ByteBuffer.wrap(senderKeyPair.getPublic().getEncoded()));
      Collections.sort(keys);
      for (ByteBuffer bb : keys) {
        digest.update(bb);
      }

      byte[] data = digest.digest();
      if (log.isTraceEnabled()) {
        log.trace("SecretKey ({}): {}", data.length * 8, Base64.getEncoder().encodeToString(data));
      }
      return new SecretKeySpec(data, STANDARD_SECRET_KEY_ALG);
    } catch (InvalidKeyException | IllegalStateException | NoSuchAlgorithmException e) {
      throw new RuntimeException("Unable to derive " + STANDARD_KEY_AGREEMENT_ALG
          + " secret key for " + STANDARD_SECRET_KEY_ALG, e);
    }
  }

  /**
   * Sign and encrypt a message using the standard settings of this class.
   * 
   * <p>
   * The final message consists of the following joined together: {@code plainText}, the computed
   * signature, and an integer representing the length of the signature.
   * </p>
   * 
   * @param key
   *        the encryption key to use
   * @param plainText
   *        the message to encrypt
   * @param signKey
   *        the private key to sign {@code plainText} with
   * @param iv
   *        the initialization vector to use; the same vector must be used to decrypt the resulting
   *        message
   * @return the encrypted message, encoded as Base64 text
   * @throws RuntimeException
   *         if any security exception is thrown
   */
  public static String encryptStandardMessage(SecretKey key, String plainText, PrivateKey signKey,
      byte[] iv) {
    try {
      byte[] plainTextUtf8 = plainText.getBytes("UTF-8");

      // calculate message signature with sender private key
      Signature sig = Signature.getInstance(STANDARD_SIGNATURE_ALG);
      sig.initSign(signKey);
      sig.update(plainTextUtf8);
      byte[] sigBytes = sig.sign();
      byte[] sigBytesLen = ByteBuffer.allocate(Integer.BYTES).putInt(sigBytes.length).array();

      // compute message as plainText + sig + sigLen
      byte[] msgBytes = new byte[plainTextUtf8.length + sigBytes.length + sigBytesLen.length];
      System.arraycopy(plainTextUtf8, 0, msgBytes, 0, plainTextUtf8.length);
      System.arraycopy(sigBytes, 0, msgBytes, plainTextUtf8.length, sigBytes.length);
      System.arraycopy(sigBytesLen, 0, msgBytes, plainTextUtf8.length + sigBytes.length,
          sigBytesLen.length);

      Cipher cipher = Cipher.getInstance(STANDARD_CIPHER_ALG);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
      byte[] cipherText = cipher.doFinal(msgBytes);

      return Base64.getEncoder().encodeToString(cipherText);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
        | UnsupportedEncodingException | IllegalBlockSizeException | BadPaddingException
        | InvalidAlgorithmParameterException | SignatureException e) {
      throw new RuntimeException("Unable to encrypt message using " + STANDARD_CIPHER_ALG
          + " or sign using " + STANDARD_SIGNATURE_ALG, e);
    }
  }

  /**
   * Decrypt and verify a message previously encrypted via
   * {@link #encryptStandardMessage(SecretKey, String, PrivateKey, byte[])}.
   * 
   * @param key
   *        the decryption key to use
   * @param cipherTextBase64
   *        the encrypted message to decrypt, as a Base64 string
   * @param verifyKey
   *        the public key of the sender, to verify the signature
   * @param iv
   *        the initialization vector to use; the same vector must be used that was used to encrypt
   *        the message
   * @return the decrypted message text
   * @throws RuntimeException
   *         if any security exception is thrown
   */
  public static String decryptStandardMessage(SecretKey key, String cipherTextBase64,
      PublicKey verifyKey, byte[] iv) {
    try {
      Cipher cipher = Cipher.getInstance(STANDARD_CIPHER_ALG);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
      byte[] cipherTextBytes = Base64.getDecoder().decode(cipherTextBase64);
      byte[] plainTextUtf8 = cipher.doFinal(cipherTextBytes);

      ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES).put(plainTextUtf8,
          plainTextUtf8.length - Integer.BYTES, Integer.BYTES);
      bb.flip();
      int sigLength = bb.getInt();

      Signature sig = Signature.getInstance(STANDARD_SIGNATURE_ALG);
      sig.initVerify(verifyKey);
      sig.update(plainTextUtf8, 0, plainTextUtf8.length - Integer.BYTES - sigLength);
      boolean signatureValid = sig.verify(plainTextUtf8,
          plainTextUtf8.length - Integer.BYTES - sigLength, sigLength);
      if (!signatureValid) {
        throw new SecurityException("Message signature not valid.");
      }

      return new String(plainTextUtf8, 0, plainTextUtf8.length - Integer.BYTES - sigLength,
          "UTF-8");
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
        | UnsupportedEncodingException | IllegalBlockSizeException | BadPaddingException
        | InvalidAlgorithmParameterException | SignatureException e) {
      throw new RuntimeException("Unable to encrypt message using " + STANDARD_CIPHER_ALG
          + " or sign using " + STANDARD_SIGNATURE_ALG, e);
    }
  }
}
