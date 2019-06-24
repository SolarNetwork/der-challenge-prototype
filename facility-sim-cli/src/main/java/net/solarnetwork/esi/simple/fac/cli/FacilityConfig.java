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

package net.solarnetwork.esi.simple.fac.cli;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Base64;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import net.solarnetwork.esi.simple.fac.impl.DaoFacilityService;
import net.solarnetwork.esi.util.CryptoHelper;
import net.solarnetwork.esi.util.CryptoUtils;

/**
 * Facility configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class FacilityConfig {

  @Value("${esi.facility.uid:test-facility}")
  private String facilityUid = "test-facility";

  @Value("${esi.facility.conn.uri:dns:///localhost:9092}")
  private String facilityUri = "dns:///localhost:9092";

  @Value("${esi.facility.conn.usePlaintext:false}")
  private boolean usePlaintext = false;

  @Value("${esi.facility.generateKeyPair:true}")
  private boolean generateKeyPair = true;

  @Value("${esi.facility.keyStorePath:file:facility-key-pair.dat}")
  private Resource keyStoreResource = new FileSystemResource("facility-key-pair.dat");

  @Value("${esi.facility.keyStorePassword:not.a.password}")
  private String keyStorePassword = "not.a.password";

  @Value("${esi.facility.keyStoreSalt:not.a.salt}")
  private String keyStoreSalt = "not.a.salt";

  @Value("${esi.facility.keyStoreIv:not.an.initialization.vector}")
  private String keyStoreIv = "not.an.initialization.vector";

  @Bean
  public DaoFacilityService facilityService() {
    return new DaoFacilityService(facilityUid, URI.create(facilityUri), usePlaintext,
        facilityKeyPair(), cryptoHelper());
  }

  /**
   * Create the {@link CryptoHelper}.
   * 
   * @return the helper
   */
  @Bean
  public CryptoHelper cryptoHelper() {
    return CryptoUtils.STANDARD_HELPER;
  }

  private static byte[] decodeConfigBytes(String s, int len) {
    byte[] data = null;
    try {
      data = Base64.getDecoder().decode(s);
    } catch (IllegalArgumentException e) {
      try {
        data = Hex.decodeHex(s);
      } catch (DecoderException e1) {
        data = s.getBytes(CryptoUtils.STANDARD_CHARSET);
      }
    }
    if (data.length == len) {
      return data;
    }
    byte[] result = new byte[len];
    if (data.length < len) {
      System.arraycopy(data, 0, result, 0, data.length);
      Arrays.fill(result, data.length, result.length, (byte) 0x88);
    } else {
      System.arraycopy(data, 0, result, 0, result.length);
    }
    return result;
  }

  private KeyPair facilityKeyPair() {
    KeyPair result = null;
    byte[] salt = decodeConfigBytes(keyStoreSalt, 8);
    byte[] iv = decodeConfigBytes(keyStoreIv, 12);
    try {
      if (keyStoreResource.exists()) {
        result = CryptoUtils.loadKeyPair(keyStoreResource.getInputStream(), keyStorePassword, salt,
            iv);
      } else if (!generateKeyPair) {
        throw new RuntimeException("The operator key store " + keyStoreResource
            + " does not exist and generateKeyPair is false.");
      } else {
        result = cryptoHelper().generateKeyPair();
        try (OutputStream out = new FileOutputStream(keyStoreResource.getFile())) {
          CryptoUtils.saveKeyPair(out, result, keyStorePassword, salt, iv);
        }
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException("Error loading or generating operator key pair.", e);
    }
  }
}
