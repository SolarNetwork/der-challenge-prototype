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

package net.solarnetwork.esi.simple.xchg.config;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.protobuf.util.JsonFormat;

import net.solarnetwork.esi.domain.Form;
import net.solarnetwork.esi.grpc.ChannelProvider;
import net.solarnetwork.esi.grpc.SimpleChannelProvider;
import net.solarnetwork.esi.simple.xchg.dao.FacilityEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.FacilityPriceMapOfferEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.FacilityRegistrationEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.FacilityResourceCharacteristicsEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.PriceMapOfferingEntityDao;
import net.solarnetwork.esi.simple.xchg.impl.DaoFacilityCharacteristicsService;
import net.solarnetwork.esi.simple.xchg.impl.DaoFacilityRegistrationService;
import net.solarnetwork.esi.simple.xchg.impl.DaoPriceMapOfferingService;
import net.solarnetwork.esi.simple.xchg.impl.SimpleDerFacilityExchange;
import net.solarnetwork.esi.simple.xchg.service.FacilityCharacteristicsService;
import net.solarnetwork.esi.simple.xchg.service.FacilityRegistrationService;
import net.solarnetwork.esi.simple.xchg.service.PriceMapOfferingService;
import net.solarnetwork.esi.util.CryptoHelper;
import net.solarnetwork.esi.util.CryptoUtils;

/**
 * Configuration for the DER facility exchange.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class DerFacilityExchangeConfig {

  @Value("${xchg.uid:#{T(java.util.UUID).randomUUID().toString()}}")
  private String uid;

  @Value("${xchg.generateKeyPair:true}")
  private boolean generateKeyPair = true;

  @Value("${xchg.keyStorePath:file:xchg-key-pair.dat}")
  private Resource keyStoreResource = new FileSystemResource("xchg-key-pair.dat");

  @Value("${xchg.keyStorePassword:not.a.password}")
  private String keyStorePassword = "not.a.password";

  @Value("${xchg.keyStoreSalt:not.a.salt}")
  private String keyStoreSalt = "not.a.salt";

  @Value("${xchg.keyStoreIv:not.an.initialization.vector}")
  private String keyStoreIv = "not.an.initialization.vector";

  // CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINE
  @Value("${xchg.registrationFormPath:classpath:/net/solarnetwork/esi/simple/xchg/impl/default-registration-form.json}")
  private Resource registrationFormResource = new ClassPathResource(
      "default-registration-form.json", SimpleDerFacilityExchange.class);

  @Value("${xchg.facility.conn.usePlaintext:false}")
  private boolean usePlaintext = false;

  @Autowired
  public FacilityRegistrationEntityDao facilityRegistrationDao;

  @Autowired
  public FacilityEntityDao facilityDao;

  @Autowired
  public FacilityPriceMapOfferEntityDao priceMapOfferDao;

  @Autowired
  public FacilityResourceCharacteristicsEntityDao resourceCharacteristicsDao;

  @Autowired
  public PriceMapOfferingEntityDao offeringDao;

  @javax.annotation.Resource(name = "afterCommitTransactionEventPublisher")
  private ApplicationEventPublisher eventPublisher;

  @Autowired
  private PlatformTransactionManager txManager;

  @Qualifier("exchange-uid")
  @Bean
  public String exchangeUid() {
    return uid;
  }

  /**
   * The facility registration form list.
   * 
   * @return the list
   */
  @Qualifier("regform-list")
  @Bean
  public List<Form> registrationForms() {
    try {
      return Collections.singletonList(loadForm(registrationFormResource));
    } catch (IOException e) {
      throw new RuntimeException("Error loading registration forms.", e);
    }
  }

  private Form loadForm(Resource resource) throws IOException {
    try (Reader r = new InputStreamReader(resource.getInputStream(), Charset.forName("UTF-8"))) {
      Form.Builder builder = Form.newBuilder();
      JsonFormat.parser().merge(r, builder);
      return builder.build();
    }
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

  /**
   * Get the exchange key pair.
   * 
   * @return the key pair
   */
  @Qualifier("exchange-key-pair")
  @Bean
  public KeyPair exchangeKeyPair() {
    KeyPair result = null;
    byte[] salt = decodeConfigBytes(keyStoreSalt, 8);
    byte[] iv = decodeConfigBytes(keyStoreIv, 12);
    try {
      if (keyStoreResource.exists()) {
        result = CryptoUtils.loadKeyPair(keyStoreResource.getInputStream(), keyStorePassword, salt,
            iv);
      } else if (!generateKeyPair) {
        throw new RuntimeException("The exchange key store " + keyStoreResource
            + " does not exist and generateKeyPair is false.");
      } else {
        result = cryptoHelper().generateKeyPair();
        try (OutputStream out = new FileOutputStream(keyStoreResource.getFile())) {
          CryptoUtils.saveKeyPair(out, result, keyStorePassword, salt, iv);
        }
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException("Error loading or generating exchange key pair.", e);
    }
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

  @Bean
  public ChannelProvider facilityChannelProvider() {
    return new SimpleChannelProvider(usePlaintext);
  }

  /**
   * Create the {@link FacilityRegistrationService}.
   * 
   * @return the service
   */
  @Bean
  public FacilityRegistrationService facilityRegistrationService() {
    DaoFacilityRegistrationService s = new DaoFacilityRegistrationService(exchangeUid(),
        exchangeKeyPair(), registrationForms(), cryptoHelper());
    s.setFacilityDao(facilityDao);
    s.setFacilityRegistrationDao(facilityRegistrationDao);
    s.setFacilityChannelProvider(facilityChannelProvider());
    return s;
  }

  /**
   * Create the {@link FacilityCharacteristicsService}.
   * 
   * @return the service
   */
  @Bean
  public FacilityCharacteristicsService facilityCharacteristicsService() {
    DaoFacilityCharacteristicsService s = new DaoFacilityCharacteristicsService(exchangeUid(),
        exchangeKeyPair(), cryptoHelper());
    s.setFacilityDao(facilityDao);
    s.setResourceCharacteristicsDao(resourceCharacteristicsDao);
    return s;
  }

  /**
   * Create the {@link PriceMapOfferingService}.
   * 
   * @return the service
   */
  @Bean
  public PriceMapOfferingService priceMapOfferingService() {
    DaoPriceMapOfferingService s = new DaoPriceMapOfferingService(exchangeUid(), exchangeKeyPair(),
        cryptoHelper());
    s.setFacilityDao(facilityDao);
    s.setOfferingDao(offeringDao);
    s.setPriceMapOfferDao(priceMapOfferDao);
    s.setEventPublisher(eventPublisher);
    s.setFacilityChannelProvider(facilityChannelProvider());
    s.setTransactionTemplate(new TransactionTemplate(txManager));
    return s;
  }

}
