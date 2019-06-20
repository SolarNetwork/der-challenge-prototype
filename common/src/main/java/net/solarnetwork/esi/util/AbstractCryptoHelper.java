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

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
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
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base implementation of {@link CryptoHelper} using the standard Java encryption
 * framework.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class AbstractCryptoHelper implements CryptoHelper {

  protected final String keyPairAlg;
  protected final String keyAgreementAlg;
  protected final String digestAlg;
  protected final int digestByteLength;
  protected final String signatureAlg;
  protected final String secretKeyAlg;
  protected final String cipherAlg;

  protected final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Constructor.
   * 
   * @param keyPairAlg
   *        the key pair algorithm to use
   * @param keyAgreementAlg
   *        the key agreement algorithm to use
   * @param digestAlg
   *        the message digest algorithm to use
   * @param signatureAlg
   *        the signature algorithm to use
   * @param secretKeyAlg
   *        the secret key algorithm to use
   * @param cipherAlg
   *        the cipher algorithm to use
   */
  public AbstractCryptoHelper(String keyPairAlg, String keyAgreementAlg, String digestAlg,
      String signatureAlg, String secretKeyAlg, String cipherAlg) {
    super();
    this.keyPairAlg = keyPairAlg;
    this.keyAgreementAlg = keyAgreementAlg;
    this.digestAlg = digestAlg;
    this.signatureAlg = signatureAlg;
    this.secretKeyAlg = secretKeyAlg;
    this.cipherAlg = cipherAlg;

    try {
      MessageDigest digest = MessageDigest.getInstance(digestAlg);
      this.digestByteLength = digest.getDigestLength();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Unable to compute digest using " + digestAlg, e);
    }

  }

  /**
   * Create and initialize a {@link KeyPairGenerator}.
   * 
   * @return the fully initialized key pair generator
   * @throws NoSuchAlgorithmException
   *         if the key pair algorithm cannot be resolved
   * @throws InvalidAlgorithmParameterException
   *         if an initialization parameter exception occurs
   */
  protected abstract KeyPairGenerator createKeyPairGenerator()
      throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

  @Override
  public KeyPair generateKeyPair() {
    try {
      KeyPairGenerator keyPairGenerator = createKeyPairGenerator();
      return keyPairGenerator.generateKeyPair();
    } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
      throw new RuntimeException("Unable to generate a new key pair: " + e.getMessage(), e);
    }
  }

  /**
   * Create and initialize a new {@link Cipher} instance.
   * 
   * @param mode
   *        the cipher mode; use constants like {@link Cipher#ENCRYPT_MODE}
   * @param key
   *        the key to use
   * @param iv
   *        the initialization vector to use
   * @return the initialize cipher
   * @throws NoSuchAlgorithmException
   *         if the cipher algorithm cannot be resolved
   * @throws NoSuchPaddingException
   *         if the padding algorithm cannot be resolved
   * @throws InvalidKeyException
   *         if there is problem with the given key
   * @throws InvalidAlgorithmParameterException
   *         if there is a problem initializing the cipher
   */
  protected abstract Cipher createCipher(int mode, Key key, byte[] iv)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
      InvalidAlgorithmParameterException;

  @Override
  public byte[] encryptMessageDigest(SecretKey key, byte[] message, PrivateKey signKey, byte[] iv) {
    try {
      // calculate message digest to use as message body
      byte[] messageDigest = computeDigest(message);

      // calculate message signature with sender private key
      byte[] sigBytes = computeSignature(messageDigest, signKey);

      // compute message as digest + sig
      byte[] msgBytes = new byte[messageDigest.length + sigBytes.length];
      System.arraycopy(messageDigest, 0, msgBytes, 0, messageDigest.length);
      System.arraycopy(sigBytes, 0, msgBytes, messageDigest.length, sigBytes.length);

      Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, key, iv);
      return cipher.doFinal(msgBytes);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
        | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
      throw new RuntimeException("Unable to encrypt message using " + cipherAlg, e);
    }
  }

  @Override
  public SecretKey deriveSecretKey(PublicKey recipientPublicKey, KeyPair senderKeyPair) {
    try {
      KeyAgreement ka = KeyAgreement.getInstance(keyAgreementAlg);
      ka.init(senderKeyPair.getPrivate());
      ka.doPhase(recipientPublicKey, true);
      byte[] sec = ka.generateSecret(); // with BC, could use AES here

      // derive encryption key from shared secret + both public keys, as recommended by libsodium
      MessageDigest digest = MessageDigest.getInstance(digestAlg);
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
      return new SecretKeySpec(data, secretKeyAlg);
    } catch (InvalidKeyException | IllegalStateException | NoSuchAlgorithmException e) {
      throw new RuntimeException(
          "Unable to derive " + keyAgreementAlg + " secret key for " + secretKeyAlg, e);
    }
  }

  @Override
  public int getDigestByteLength() {
    return digestByteLength;
  }

  @Override
  public byte[] computeDigest(byte[] message) {
    try {
      MessageDigest digest = MessageDigest.getInstance(digestAlg);
      return digest.digest(message);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Unable to compute digest using " + digestAlg, e);
    }
  }

  @Override
  public byte[] computeSignature(byte[] message, PrivateKey signKey) {
    try {
      Signature sig = Signature.getInstance(signatureAlg);
      sig.initSign(signKey);
      sig.update(message);
      return sig.sign();
    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
      throw new RuntimeException("Unable to sign message using " + signatureAlg, e);
    }
  }

  @Override
  public byte[] decryptMessageDigest(SecretKey key, byte[] cipherText, PublicKey verifyKey,
      byte[] iv) {
    try {
      Cipher cipher = createCipher(Cipher.DECRYPT_MODE, key, iv);
      byte[] message = cipher.doFinal(cipherText);

      Signature sig = Signature.getInstance(signatureAlg);
      sig.initVerify(verifyKey);
      sig.update(message, 0, digestByteLength);
      boolean signatureValid = sig.verify(message, digestByteLength,
          message.length - digestByteLength);
      if (!signatureValid) {
        throw new SecurityException("Message signature not valid.");
      }

      byte[] messageDigest = new byte[digestByteLength];
      System.arraycopy(message, 0, messageDigest, 0, digestByteLength);
      return messageDigest;
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
        | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException
        | SignatureException e) {
      throw new RuntimeException(
          "Unable to decrypt message using " + cipherAlg + " or verify using " + signatureAlg, e);
    }
  }

}
