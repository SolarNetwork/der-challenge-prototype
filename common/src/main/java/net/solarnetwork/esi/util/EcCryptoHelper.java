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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Implementation of {@link CryptoHelper} using elliptic curves.
 * 
 * @author matt
 * @version 1.0
 */
public class EcCryptoHelper extends AbstractCryptoHelper {

  /** The standard key pair algorithm to use. */
  public static final String DEFAULT_KEY_PAIR_ALG = "EC";

  /** The standard elliptic curve name to use. */
  public static final String DEFAULT_EC_NAME = "secp256r1";

  /** The standard key agreement algorithm to use. */
  public static final String DEFAULT_KEY_AGREEMENT_ALG = "ECDH";

  /** The standard message digest algorithm to use. */
  public static final String DEFAULT_DIGEST_ALG = "SHA-256";

  /** The standard signature algorithm to use. */
  public static final String DEFAULT_SIGNATURE_ALG = "SHA256withECDSA";

  /** The standard secret key algorithm to use. */
  public static final String DEFAULT_SECRET_KEY_ALG = "AES";

  /** The standard cipher algorithm to use. */
  public static final String DEFAULT_CIPHER_ALG = "AES/GCM/NoPadding";

  /** The standard GCM parameter size. */
  private static final int DEFAULT_GCM_PARAMETER_SIZE = 128;

  /** The value returned for both the min/max initialization vector sizes. */
  public static final int STANDARD_IV_SIZE = 12;

  private final String ecName;
  private final int gcmParameterSize;

  /**
   * Default constructor.
   * 
   * <p>
   * All standard properties are used.
   * </p>
   */
  public EcCryptoHelper() {
    this(DEFAULT_KEY_PAIR_ALG, DEFAULT_KEY_AGREEMENT_ALG, DEFAULT_DIGEST_ALG, DEFAULT_SIGNATURE_ALG,
        DEFAULT_SECRET_KEY_ALG, DEFAULT_CIPHER_ALG, DEFAULT_EC_NAME, DEFAULT_GCM_PARAMETER_SIZE);
  }

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
   * @param ecName
   *        the elliptic curve name to use
   * @param gcmParameterSize
   *        the GCM parameter size
   */
  public EcCryptoHelper(String keyPairAlg, String keyAgreementAlg, String digestAlg,
      String signatureAlg, String secretKeyAlg, String cipherAlg, String ecName,
      int gcmParameterSize) {
    super(keyPairAlg, keyAgreementAlg, digestAlg, signatureAlg, secretKeyAlg, cipherAlg);
    this.ecName = ecName;
    this.gcmParameterSize = gcmParameterSize;
  }

  @Override
  public int getInitializationVectorMinimumSize() {
    return STANDARD_IV_SIZE;
  }

  @Override
  public int getInitializationVectorMaximumSize() {
    return STANDARD_IV_SIZE;
  }

  @Override
  protected KeyPairGenerator createKeyPairGenerator()
      throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyPairAlg);
    AlgorithmParameterSpec spec = new ECGenParameterSpec(ecName);
    keyPairGenerator.initialize(spec);
    return keyPairGenerator;
  }

  @Override
  protected Cipher createCipher(int mode, Key key, byte[] iv) throws NoSuchAlgorithmException,
      NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
    Cipher cipher = Cipher.getInstance(cipherAlg);
    cipher.init(mode, key, new GCMParameterSpec(gcmParameterSize, iv));
    return cipher;
  }

}
