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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;

import javax.annotation.Nonnull;
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
import com.google.protobuf.ByteString;

import net.solarnetwork.esi.domain.CryptoKey;
import net.solarnetwork.esi.domain.MessageSignature;
import net.solarnetwork.esi.domain.support.KeyPairStore;
import net.solarnetwork.esi.domain.support.SignableMessage;

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
   * Generate an array of random bytes of a given length.
   * 
   * @param length
   *        the desired length
   * @return the random bytes
   */
  public static final byte[] generateRandomBytes(int length) {
    byte[] b = new byte[length];
    new SecureRandom().nextBytes(b);
    return b;
  }

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

  /**
   * Get a {@link PublicKey} from encoded key data.
   * 
   * @param helper
   *        the helper to use
   * @param encodedKey
   *        the key data to decode
   * @return the key
   * @throws RuntimeException
   *         if any error occurs
   */
  public static PublicKey decodePublicKey(CryptoHelper helper, byte[] encodedKey) {
    return helper
        .decodePublicKey(CryptoKey.newBuilder().setKey(ByteString.copyFrom(encodedKey)).build());
  }

  /**
   * Generate a {@link MessageSignature} from a set of message data.
   * 
   * <p>
   * This method will generate a new random encryption initialization vector. It will then generate
   * the message to sign by iterating over {@code messageData}, converting each element to bytes via
   * {@link #bytesForObject(Object)} and concatenating everything into one final byte array message.
   * The final message signature is calculated via
   * {@link CryptoHelper#encryptMessageDigest(SecretKey, byte[], java.security.PrivateKey, byte[])}.
   * </p>
   * 
   * @param helper
   *        the helper to use
   * @param senderKeyPair
   *        the sender's key pair to sign the message data with
   * @param recipientKey
   *        the recipient's public key to encrypt the message data signature with
   * @param messageData
   *        the message data to convert to a byte array message to sign; all elements are converted
   *        to bytes via {@link #bytesForObject(Object)}
   * @return the new message signature instance
   * @throws RuntimeException
   *         if any error occurs
   * @see #bytesForObject(Object)
   */
  public static MessageSignature generateMessageSignature(CryptoHelper helper,
      KeyPair senderKeyPair, PublicKey recipientKey, Iterable<?> messageData) {
    try {
      final byte[] iv = generateRandomBytes(helper.getInitializationVectorMinimumSize());
      final SecretKey encryptKey = helper.deriveSecretKey(recipientKey, senderKeyPair);
      final ByteArrayOutputStream byos = new ByteArrayOutputStream();
      for (Object o : messageData) {
        byte[] bytes = bytesForObject(o);
        byos.write(bytes);
      }
      final byte[] msgSigData = helper.encryptMessageDigest(encryptKey, byos.toByteArray(),
          senderKeyPair.getPrivate(), iv);
      return MessageSignature.newBuilder().setIv(ByteString.copyFrom(iv))
          .setSignature(ByteString.copyFrom(msgSigData)).build();
    } catch (IOException e) {
      throw new RuntimeException("Error generating message signature: " + e.getMessage(), e);
    }
  }

  /**
   * Validate a {@link MessageSignature} from a set of message data.
   * 
   * <p>
   * This method calls the
   * {@link CryptoHelper#validateMessageDigest(KeyPair, byte[], PublicKey, byte[], byte[])}, passing
   * in a message computed by concatenating all values in {@code messageData} into a byte array.
   * </p>
   * 
   * @param cryptoHelper
   *        the helper to use
   * @param msgSig
   *        the signature to validate
   * @param recipientKeyPair
   *        the recipient's key pair to decrypt the signature data with
   * @param senderPublicKey
   *        the sender's public key to validate the signature with
   * @param messageData
   *        the message data to compute the expected message digest from to compare to the digest
   *        decrypted from the signature; the same rules outlined in
   *        {@link #generateMessageSignature(CryptoHelper, KeyPair, PublicKey, Iterable)} for
   *        converting the objects to bytes are used
   * @return the computed and validated message digest
   * @throws RuntimeException
   *         if any error occurs
   * @see #bytesForObject(Object)
   */
  public static byte[] validateMessageSignature(CryptoHelper cryptoHelper, MessageSignature msgSig,
      KeyPair recipientKeyPair, PublicKey senderPublicKey, Iterable<?> messageData) {
    if (msgSig == null) {
      throw new IllegalArgumentException("Route message signature missing.");
    } else if (msgSig.getIv() == null || msgSig.getIv().isEmpty()) {
      throw new IllegalArgumentException("Route message signature initialization vector missing.");
    } else if (msgSig.getSignature() == null || msgSig.getSignature().isEmpty()) {
      throw new IllegalArgumentException("Route message signature value missing.");
    }
    try {
      final ByteArrayOutputStream byos = new ByteArrayOutputStream();
      for (Object o : messageData) {
        byte[] bytes = bytesForObject(o);
        byos.write(bytes);
      }
      return cryptoHelper.validateMessageDigest(recipientKeyPair,
          msgSig.getSignature().toByteArray(), senderPublicKey, byos.toByteArray(),
          msgSig.getIv().toByteArray());
    } catch (IOException e) {
      throw new RuntimeException("Error validating message signature: " + e.getMessage(), e);
    }
  }

  /**
   * Convert an object to bytes.
   * 
   * <p>
   * The following rules will be used to convert the object into bytes:
   * </p>
   * 
   * <ol>
   * <li>If it is a {@code byte[]} it will be returned directly.</li>
   * <li>If it is a {@link ByteString} then it's bytes will be returned.</li>
   * <li>If it is a {@link ByteBuffer} then {@link ByteBuffer#flip()} will be called and then the
   * remaining bytes will be returned.</li>
   * <li>If it is a {@link ByteArrayOutputStream} then {@link ByteArrayOutputStream#toByteArray()}
   * will be returned.</li>
   * <li>If it is a {@link SignableMessage} then {@link SignableMessage#toSignatureMessageBytes()}
   * will be returned.</li>
   * <li>If it is {@literal null} an array of length {@literal 0} will be returned.</li>
   * <li>For all other objects, {@link Object#toString()} will be used to turn it into a string, and
   * then the UTF-8 bytes of that will be returned.</li>
   * </ol>
   * 
   * @param o
   *        the object to convert to bytes
   * @return the bytes, never {@literal null}
   */
  @Nonnull
  public static byte[] bytesForObject(Object o) {
    byte[] bytes;
    if (o instanceof byte[]) {
      bytes = (byte[]) o;
    } else if (o instanceof ByteString) {
      bytes = ((ByteString) o).toByteArray();
    } else if (o instanceof ByteBuffer) {
      ByteBuffer bb = ((ByteBuffer) o);
      bb.flip();
      bytes = new byte[bb.limit()];
      bb.get(bytes);
    } else if (o instanceof ByteArrayOutputStream) {
      bytes = ((ByteArrayOutputStream) o).toByteArray();
    } else if (o instanceof SignableMessage) {
      bytes = ((SignableMessage) o).toSignatureMessageBytes();
    } else if (o != null) {
      bytes = o.toString().getBytes(STANDARD_CHARSET);
    } else {
      bytes = new byte[0];
    }
    return bytes;
  }

  /**
   * Compute a SHA-256 digest of a set of data.
   * 
   * @param messageData
   *        the message data to compute the expected message digest from to compare to the digest
   *        decrypted from the signature; all elements are converted to bytes via
   *        {@link #bytesForObject(Object)} and concatenated as the message to digest
   * @return the digest value
   * @throws RuntimeException
   *         if any error occurs
   */
  public static byte[] sha256(Iterable<?> messageData) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      if (messageData != null) {
        for (Object o : messageData) {
          byte[] bytes = bytesForObject(o);
          digest.update(bytes);
        }
      }
      return digest.digest();
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException("Error computing SHA-256 digest: " + e.getMessage(), e);
    }
  }

}
