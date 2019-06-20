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

import static net.solarnetwork.esi.util.CryptoUtils.STANDARD_HELPER;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.solarnetwork.esi.util.CryptoHelper;
import net.solarnetwork.esi.util.CryptoUtils;
import net.solarnetwork.esi.util.EcCryptoHelper;

/**
 * Test cases for the {@link EcCryptoHelper} class.
 * 
 * @author matt
 * @version 1.0
 */
public class EcCryptoHelperTests {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Test
  public void encodeDecodeKeys() throws Exception {
    KeyPair senderKeyPair = new EcCryptoHelper().generateKeyPair();

    log.debug("Public format: {}", senderKeyPair.getPublic().getFormat()); // X.509
    log.debug("Private format: {}", senderKeyPair.getPrivate().getFormat()); // PKCS#8

    byte[] pubKeyBytes = senderKeyPair.getPublic().getEncoded();
    byte[] privKeyBytes = senderKeyPair.getPrivate().getEncoded();

    EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pubKeyBytes);
    EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privKeyBytes);

    KeyFactory kf = KeyFactory.getInstance(EcCryptoHelper.DEFAULT_KEY_PAIR_ALG);
    KeyPair newKp = new KeyPair(kf.generatePublic(pubKeySpec), kf.generatePrivate(privKeySpec));

    assertThat("Loaded public key same as original", newKp.getPublic(),
        equalTo(senderKeyPair.getPublic()));
    assertThat("Loaded private key same as original", newKp.getPrivate(),
        equalTo(senderKeyPair.getPrivate()));
  }

  @Test
  public void roundTripEncryption() throws Exception {
    final CryptoHelper helper = new EcCryptoHelper();
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
    final byte[] msgDigest = STANDARD_HELPER.computeDigest(msgBytes);
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

}
