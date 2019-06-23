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

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import javax.crypto.SecretKey;

import net.solarnetwork.esi.domain.CryptoKey;

/**
 * API for an object that can help perform common cryptographic computations.
 * 
 * @author matt
 * @version 1.0
 */
public interface CryptoHelper {

  /**
   * Generate a new public/private key pair.
   * 
   * @return the new key pair
   * @throws RuntimeException
   *         if any security exception is thrown
   */
  KeyPair generateKeyPair();

  /**
   * Get the initialization vector minimum size, in bytes.
   * 
   * @return the minimum size, in bytes
   */
  int getInitializationVectorMinimumSize();

  /**
   * Get the initialization vector maximum size, in bytes.
   * 
   * @return the maximum size, in bytes
   */
  int getInitializationVectorMaximumSize();

  /**
   * Decode a {@link PublicKey} from a {@link CryptoKey}.
   * 
   * <p>
   * The {@code algorithm} and {@code encoding} properties can be omitted, in which case the default
   * values used by the helper implementation is assumed.
   * </p>
   * 
   * @param cryptoKey
   *        the key data to decode
   * @return the decoded key
   * @throws RuntimeException
   *         if any security exception is thrown
   */
  PublicKey decodePublicKey(CryptoKey cryptoKey);

  /**
   * Decrypt and verify a message previously encrypted via
   * {@link #encryptMessage(SecretKey, byte[], PrivateKey, byte[])}, and then compute the digest of
   * a message and validate the computed digest matches the decrypted/signed one.
   * 
   * <p>
   * This is a one-shot method to validate a received encrypted message has a verified signature and
   * a message digest that matches an expected value.
   * </p>
   * 
   * @param keyPair
   *        the public and private key to derive the encryption key from and sign {@code message}
   *        with
   * @param encryptedMessage
   *        the encrypted message and signature to decrypt
   * @param verifyKey
   *        the public key of the sender, to verify the signature
   * @param message
   *        the source message to compute a digest from to compare to the signed digest decrypted
   *        from {@code encryptedMessage}
   * @param iv
   *        the initialization vector to use; the same vector must be used to decrypt the resulting
   *        message
   * @return the validated message digest
   * @throws RuntimeException
   *         if any security exception is thrown
   */
  default byte[] validateMessageDigest(KeyPair keyPair, byte[] encryptedMessage,
      PublicKey verifyKey, byte[] message, byte[] iv) {
    try {
      SecretKey key = deriveSecretKey(verifyKey, keyPair);
      byte[] signedDigest = decryptMessageDigest(key, encryptedMessage, verifyKey, iv);
      byte[] computedDigest = computeDigest(message);
      if (!Arrays.equals(signedDigest, computedDigest)) {
        throw new SecurityException("Computed message digest does not match signed digest.");
      }
      return computedDigest;
    } catch (SecurityException e) {
      throw new RuntimeException("Unable to validate encrypted message", e);
    }
  }

  /**
   * Sign and encrypt a message.
   * 
   * <p>
   * The final message consists of SHA256({@code message}) followed by the computed signature.
   * </p>
   * 
   * @param key
   *        the encryption key to use
   * @param message
   *        the message to encrypt
   * @param signKey
   *        the private key to sign {@code plainText} with
   * @param iv
   *        the initialization vector to use; the same vector must be used to decrypt the resulting
   *        message
   * @return the encrypted message
   * @throws RuntimeException
   *         if any security exception is thrown
   * @see #validateMessage(KeyPair, byte[], PublicKey, byte[], byte[])
   * @see #decryptMessage(SecretKey, byte[], PublicKey, byte[])
   */
  byte[] encryptMessageDigest(SecretKey key, byte[] message, PrivateKey signKey, byte[] iv);

  /**
   * Derive a secret key suitable for symmetric encryption using the standard settings in this
   * class.
   * 
   * <p>
   * The key generated here will be a {@link #STANDARD_DIGEST_ALG} digest computed from a
   * {@link #STANDARD_KEY_AGREEMENT_ALG} secret from the <i>sender's</i> private key and
   * <i>recipient's</i> public key, followed by both public keys ordered lexicographically.
   * </p>
   * 
   * @param recipientPublicKey
   *        the recipient's public key
   * @param senderKeyPair
   *        the sender's public/private key pair
   * @return the secret key
   * @throws RuntimeException
   *         if any security exception is thrown
   */
  SecretKey deriveSecretKey(PublicKey recipientPublicKey, KeyPair senderKeyPair);

  /**
   * Get the length of computed digest, in bytes.
   * 
   * @return the digest length, in bytes
   */
  int getDigestByteLength();

  /**
   * Compute a standard message digest.
   * 
   * @param message
   *        the message to digest
   * @return the digest value
   */
  byte[] computeDigest(byte[] message);

  /**
   * Sign and encrypt a message signature using the standard settings of this class.
   * 
   * <p>
   * This method computes a standard signature of {@code plainText} using {@code signKey}, and then
   * encrypts the result using {@code key}.
   * </p>
   * 
   * @param message
   *        the message to sign
   * @param signKey
   *        the private key to sign {@code plainText} with
   * @return the encrypted message
   * @throws RuntimeException
   *         if any security exception is thrown
   * @see #decryptSignature(SecretKey, byte[], PublicKey, byte[])
   */
  byte[] computeSignature(byte[] message, PrivateKey signKey);

  /**
   * Decrypt and verify a message previously encrypted via
   * {@link #encryptMessage(SecretKey, byte[], PrivateKey, byte[])}.
   * 
   * @param key
   *        the decryption key to use
   * @param cipherText
   *        the encrypted message to decrypt
   * @param verifyKey
   *        the public key of the sender, to verify the signature
   * @param iv
   *        the initialization vector to use; the same vector must be used that was used to encrypt
   *        the message
   * @return the decrypted message, which is a SHA256 digest of the signed message
   * @throws RuntimeException
   *         if any security exception is thrown
   */
  byte[] decryptMessageDigest(SecretKey key, byte[] cipherText, PublicKey verifyKey, byte[] iv);

}
