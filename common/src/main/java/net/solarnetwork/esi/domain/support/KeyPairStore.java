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

package net.solarnetwork.esi.domain.support;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Storage for a key pair.
 * 
 * @author matt
 * @version 1.0
 */
public class KeyPairStore {

  private final byte[] publicKey;

  private final byte[] privateKey;

  private final String keyAlgorithm;

  /**
   * Constructor.
   * 
   * @param publicKey
   *        the public key data
   * @param privateKey
   *        the private key data
   * @param keyAlgorithm
   *        the key algorithm to use
   */
  @JsonCreator
  public KeyPairStore(@JsonProperty("publicKey") byte[] publicKey,
      @JsonProperty("privateKey") byte[] privateKey,
      @JsonProperty("keyAlgorithm") String keyAlgorithm) {
    super();
    this.publicKey = publicKey;
    this.privateKey = privateKey;
    this.keyAlgorithm = keyAlgorithm;
  }

  /**
   * Construct from an existing key pair.
   * 
   * @param keyPair
   *        the key pair to construct from
   */
  public KeyPairStore(KeyPair keyPair) {
    super();
    this.keyAlgorithm = keyPair.getPrivate().getAlgorithm();
    this.publicKey = keyPair.getPublic().getEncoded();
    this.privateKey = keyPair.getPrivate().getEncoded();
  }

  /**
   * Get a {@link KeyPair} instance from this store.
   * 
   * @return the key pair
   * @throws RuntimeException
   *         if any error occurs
   */
  public KeyPair asKeyPair() {
    try {
      EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(publicKey);
      EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privateKey);
      KeyFactory kf = KeyFactory.getInstance(keyAlgorithm);
      return new KeyPair(kf.generatePublic(pubKeySpec), kf.generatePrivate(privKeySpec));
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new RuntimeException("Error loading key pair data: " + e.getMessage(), e);
    }
  }

  @JsonProperty("keyAlgorithm")
  public String getKeyAlgorithm() {
    return keyAlgorithm;
  }

  @JsonProperty("publicKey")
  public byte[] getPublicKey() {
    return publicKey;
  }

  @JsonProperty("privateKey")
  public byte[] getPrivateKey() {
    return privateKey;
  }

}
