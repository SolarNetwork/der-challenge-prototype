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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.solarnetwork.esi.util.CryptoHelper;
import net.solarnetwork.esi.util.CryptoUtils;

/**
 * Test cases for the {@link CryptoUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CryptoUtilsTests {

  private static final Logger log = LoggerFactory.getLogger(CryptoUtilsTests.class);

  // Code to print out the available crypto algorithms
  // 
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

  @Test
  public void roundTripEncryption() throws Exception {
    CryptoHelper helper = CryptoUtils.STANDARD_HELPER;
    final byte[] iv = new SecureRandom().generateSeed(helper.getInitializationVectorMinimumSize());

    KeyPair senderKeyPair = helper.generateKeyPair();

    log.debug("Public format: {}", senderKeyPair.getPublic().getFormat()); // X.509
    log.debug("Private format: {}", senderKeyPair.getPrivate().getFormat()); // PKCS#8

    // create the destination key pair
    // normally only the dest public key would be available for encryption
    KeyPair recipientKeyPair = helper.generateKeyPair();

    SecretKey encryptKey = helper.deriveSecretKey(recipientKeyPair.getPublic(), senderKeyPair);

    final String msg = "Hello, world.";
    final byte[] msgBytes = msg.getBytes(CryptoUtils.STANDARD_CHARSET);
    final byte[] msgDigest = helper.computeDigest(msgBytes);
    log.debug("Original message digest: {}", Base64.getEncoder().encodeToString(msgDigest));

    // SENDER: encrypt/sign message for recipient using recipient public key
    byte[] cipherBytes = helper.encryptMessageDigest(encryptKey, msgBytes,
        senderKeyPair.getPrivate(), iv);
    log.debug("Cipher text: {}", Base64.getEncoder().encodeToString(cipherBytes));

    // RECIPIENT: decrypt/verify message from sender using sender public key
    // Note that decryptKey will be the same as encryptKey; this shows how the same key is derived 
    // from the different public/private keys available to the sender vs. receiver
    SecretKey decryptKey = helper.deriveSecretKey(senderKeyPair.getPublic(), recipientKeyPair);
    byte[] decryptedMessageDigest = helper.decryptMessageDigest(decryptKey, cipherBytes,
        senderKeyPair.getPublic(), iv);
    log.debug("Decrypted message digest: {}",
        Base64.getEncoder().encodeToString(decryptedMessageDigest));

    assertThat("Decrypted message digest same as original message digest",
        Base64.getEncoder().encodeToString(decryptedMessageDigest),
        equalTo(Base64.getEncoder().encodeToString(msgDigest)));

    // now perform actual verification call
    byte[] validatedDigest = helper.validateMessageDigest(recipientKeyPair, cipherBytes,
        senderKeyPair.getPublic(), msgBytes, iv);
    assertThat("Expected and validated digests match",
        Base64.getEncoder().encodeToString(validatedDigest),
        equalTo(Base64.getEncoder().encodeToString(msgDigest)));
  }

  @Test
  public void roundTripKeyPairSave() throws Exception {
    CryptoHelper helper = CryptoUtils.STANDARD_HELPER;
    KeyPair keyPair = helper.generateKeyPair();
    byte[] salt = new SecureRandom().generateSeed(8);
    byte[] iv = new SecureRandom().generateSeed(12);

    ByteArrayOutputStream byos = new ByteArrayOutputStream();
    CryptoUtils.saveKeyPair(byos, keyPair, "foobar", salt, iv);
    byte[] encryptedKeyStore = byos.toByteArray();
    assertThat("Key pair saved", encryptedKeyStore.length, greaterThan(0));

    KeyPair loadedKeyPair = CryptoUtils.loadKeyPair(new ByteArrayInputStream(encryptedKeyStore),
        "foobar", salt, iv);
    assertThat("Key pair loaded", loadedKeyPair, notNullValue());

    assertThat("Public key same", keyPair.getPublic(), equalTo(loadedKeyPair.getPublic()));
    assertThat("Private key same", keyPair.getPrivate(), equalTo(loadedKeyPair.getPrivate()));
  }

}
