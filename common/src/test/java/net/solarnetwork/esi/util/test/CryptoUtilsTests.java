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

package net.solarnetwork.esi.util.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.SecretKey;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.solarnetwork.esi.util.CryptoUtils;

/**
 * Test cases for the {@link CryptoUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CryptoUtilsTests {

  private static final Logger log = LoggerFactory.getLogger(CryptoUtilsTests.class);

  private static byte[] IV = new SecureRandom().generateSeed(12);

  private void encodeDecodeKeyPair(KeyPair senderKeyPair)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] pubKeyBytes = senderKeyPair.getPublic().getEncoded();
    byte[] privKeyBytes = senderKeyPair.getPrivate().getEncoded();

    EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pubKeyBytes);
    EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privKeyBytes);

    KeyFactory kf = KeyFactory.getInstance(CryptoUtils.STANDARD_KEY_PAIR_ALG);
    KeyPair newKp = new KeyPair(kf.generatePublic(pubKeySpec), kf.generatePrivate(privKeySpec));

    assertThat("Loaded public key same as original", newKp.getPublic(),
        equalTo(senderKeyPair.getPublic()));
    assertThat("Loaded private key same as original", newKp.getPrivate(),
        equalTo(senderKeyPair.getPrivate()));
  }

  @Test
  public void haveAGo() throws Exception {
    //    for (Provider provider : Security.getProviders()) {
    //      System.out.println(provider.getName());
    //      for (String key : provider.stringPropertyNames())
    //        System.out.println("\t" + key + "\t" + provider.getProperty(key));
    //    }
    //    for (Provider provider : Security.getProviders()) {
    //      System.out.println("Provider: " + provider.getName());
    //      for (Provider.Service service : provider.getServices()) {
    //        System.out.println("  Algorithm: " + service.getAlgorithm());
    //      }
    //    }
    //    String[] curves = Security.getProvider("SunEC")
    //        .getProperty("AlgorithmParameters.EC SupportedCurves").split("\\|");
    //    for (String curve : curves) {
    //      System.out.println(curve.substring(1, curve.indexOf(",")));
    //    }
    KeyPair senderKeyPair = CryptoUtils.generateStandardKeyPair();

    log.debug("Public format: {}", senderKeyPair.getPublic().getFormat()); // X.509
    log.debug("Private format: {}", senderKeyPair.getPrivate().getFormat()); // PKCS#8

    // **** show encoding to bytes/ loading from bytes
    encodeDecodeKeyPair(senderKeyPair);

    // create the destination key pair
    // normally only the dest public key would be available for encryption
    KeyPair recipientKeyPair = CryptoUtils.generateStandardKeyPair();

    SecretKey encKey = CryptoUtils.deriveStandardSecretKey(recipientKeyPair.getPublic(),
        senderKeyPair);

    final String msg = "Hello, world.";

    // SENDER: encrypt/sign message for recipient using recipient public key
    String cipherText = CryptoUtils.encryptStandardMessage(encKey, msg, senderKeyPair.getPrivate(),
        IV);
    log.debug("Cipher text: {}", cipherText);

    // RECIPIENT: decrypt/verify message from sender using sender public key
    SecretKey destKey = CryptoUtils.deriveStandardSecretKey(senderKeyPair.getPublic(),
        recipientKeyPair);
    String decryptedCipherText = CryptoUtils.decryptStandardMessage(destKey, cipherText,
        senderKeyPair.getPublic(), IV);
    log.debug("Decrypted cipher text: {}", decryptedCipherText);

    assertThat("Decrypted cipher text same as original message", decryptedCipherText, equalTo(msg));
  }

}
