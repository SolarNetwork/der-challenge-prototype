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

package net.solarnetwork.esi.simple.fac.impl;

import static java.util.Arrays.asList;
import static net.solarnetwork.esi.util.CryptoUtils.generateMessageSignature;
import static net.solarnetwork.esi.util.CryptoUtils.validateMessageSignature;

import java.net.URI;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;

import io.grpc.ManagedChannel;
import net.solarnetwork.esi.domain.CryptoKey;
import net.solarnetwork.esi.domain.DerFacilityExchangeInfo;
import net.solarnetwork.esi.domain.DerFacilityExchangeRequest;
import net.solarnetwork.esi.domain.DerFacilityRegistration;
import net.solarnetwork.esi.domain.DerFacilityRegistrationForm;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormData;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormDataReceipt;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormRequest;
import net.solarnetwork.esi.domain.DerRoute;
import net.solarnetwork.esi.domain.DerRouteOrBuilder;
import net.solarnetwork.esi.domain.FormData;
import net.solarnetwork.esi.domain.MessageSignature;
import net.solarnetwork.esi.grpc.ChannelProvider;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc.DerFacilityExchangeBlockingStub;
import net.solarnetwork.esi.service.DerFacilityExchangeRegistryGrpc;
import net.solarnetwork.esi.service.DerFacilityExchangeRegistryGrpc.DerFacilityExchangeRegistryBlockingStub;
import net.solarnetwork.esi.simple.fac.dao.ExchangeEntityDao;
import net.solarnetwork.esi.simple.fac.dao.ExchangeRegistrationEntityDao;
import net.solarnetwork.esi.simple.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.simple.fac.domain.ExchangeRegistrationEntity;
import net.solarnetwork.esi.simple.fac.service.ExchangeRegistrationService;
import net.solarnetwork.esi.simple.fac.service.FacilityService;
import net.solarnetwork.esi.util.CryptoUtils;

/**
 * DAO based implementation of {@link ExchangeRegistrationService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoExchangeRegistrationService implements ExchangeRegistrationService {

  private final FacilityService facilityService;
  private final ExchangeEntityDao exchangeDao;
  private final ExchangeRegistrationEntityDao exchangeRegistrationDao;
  private ChannelProvider exchangeRegistryChannelProvider;
  private ChannelProvider exchangeChannelProvider;

  /**
   * Constructor.
   * 
   * @param facilityService
   *        the facility service
   * @param exchangeDao
   *        the exchange DAO
   * @param exchangeRegistrationDao
   *        the exchange registration DAO
   */
  public DaoExchangeRegistrationService(FacilityService facilityService,
      ExchangeEntityDao exchangeDao, ExchangeRegistrationEntityDao exchangeRegistrationDao) {
    super();
    this.facilityService = facilityService;
    this.exchangeDao = exchangeDao;
    this.exchangeRegistrationDao = exchangeRegistrationDao;
  }

  @Override
  public Iterator<DerFacilityExchangeInfo> listExchanges(DerFacilityExchangeRequest criteria) {
    ManagedChannel channel = exchangeRegistryChannelProvider.channelForUri(null);
    try {
      DerFacilityExchangeRegistryBlockingStub client = DerFacilityExchangeRegistryGrpc
          .newBlockingStub(channel);
      return client.listDerFacilityExchanges(
          criteria != null ? criteria : DerFacilityExchangeRequest.getDefaultInstance());
    } finally {
      channel.shutdown();
    }
  }

  @Override
  public DerFacilityRegistrationForm getExchangeRegistrationForm(DerFacilityExchangeInfo exchange,
      Locale locale) {
    ManagedChannel channel = exchangeChannelProvider
        .channelForUri(URI.create(exchange.getEndpointUri()));
    try {
      DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);
      return client.getDerFacilityRegistrationForm(DerFacilityRegistrationFormRequest.newBuilder()
          .setLanguageCode(locale.getLanguage()).setExchangeUid(exchange.getUid()).build());
    } finally {
      channel.shutdown();
    }
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public ExchangeRegistrationEntity registerWithExchange(DerFacilityExchangeInfo exchange,
      FormData formData) {
    ManagedChannel channel = exchangeChannelProvider
        .channelForUri(URI.create(exchange.getEndpointUri()));
    try {
      DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);

      // TODO: check if we already have a key for this exchange!
      CryptoKey exchangePublicKey = client.getPublicCryptoKey(Empty.getDefaultInstance());
      ByteString nonce = ByteString.copyFrom(CryptoUtils.generateRandomBytes(12));
      KeyPair facilityKeyPair = facilityService.getKeyPair();

      // @formatter:off
      
      MessageSignature msgSig = generateMessageSignature(
          facilityService.getCryptoHelper(), 
          facilityKeyPair, 
          facilityService.getCryptoHelper().decodePublicKey(exchangePublicKey), 
          asList(
              exchange.getUid(), 
              facilityService.getUid(),
              facilityService.getUri(),
              nonce));
      
      DerFacilityRegistrationFormData regFormData = DerFacilityRegistrationFormData.newBuilder()
          .setData(formData)
          .setFacilityEndpointUri(facilityService.getUri().toString())
          .setFacilityPublicKey(CryptoKey.newBuilder()
              .setAlgorithm(facilityKeyPair.getPublic().getAlgorithm())
              .setEncoding(facilityKeyPair.getPublic().getFormat())
              .setKey(ByteString.copyFrom(facilityKeyPair.getPublic().getEncoded()))
              .build())
          .setFacilityNonce(nonce)
          .setRoute(DerRoute.newBuilder()
              .setExchangeUid(exchange.getUid())
              .setFacilityUid(facilityService.getUid())
              .setSignature(msgSig)
              .build())
          .build();
      // @formatter:on

      DerFacilityRegistrationFormDataReceipt receipt = client
          .submitDerFacilityRegistrationForm(regFormData);

      ExchangeRegistrationEntity reg = new ExchangeRegistrationEntity(Instant.now(),
          exchange.getUid());
      reg.setExchangeEndpointUri(exchange.getEndpointUri());
      reg.setExchangePublicKey(exchangePublicKey.getKey().toByteArray());
      reg.setFacilityNonce(nonce.toByteArray());
      reg.setExchangeNonce(receipt.getExchangeNonce().toByteArray());
      reg = exchangeRegistrationDao.save(reg);

      return reg;
    } finally {
      channel.shutdown();
    }
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public ExchangeEntity completeExchangeRegistration(DerFacilityRegistration request) {
    DerRouteOrBuilder route = request.getRouteOrBuilder();
    if (route == null) {
      throw new IllegalArgumentException("Route missing");
    }

    if (!facilityService.getUid().equals(route.getFacilityUid())) {
      throw new IllegalArgumentException("Facility UID not valid.");
    }

    String exchangeUid = route.getExchangeUid();
    if (exchangeUid == null || exchangeUid.trim().isEmpty()) {
      throw new IllegalArgumentException("Exchange UID missing.");
    }

    ExchangeRegistrationEntity reg = exchangeRegistrationDao.findById(exchangeUid)
        .orElseThrow(() -> new IllegalArgumentException("Exchange registration not found."));

    // verify signature
    validateMessageSignature(facilityService.getCryptoHelper(), route.getSignature(),
        facilityService.getKeyPair(),
        facilityService.getCryptoHelper().decodePublicKey(
            CryptoKey.newBuilder().setKey(ByteString.copyFrom(reg.getExchangePublicKey())).build()),
        asList(exchangeUid, facilityService.getUid()));

    // @formatter:off
    ByteString expectedToken = ByteString.copyFrom(CryptoUtils.sha256(Arrays.asList(
        reg.getExchangeNonce(),
        reg.getFacilityNonce(),
        exchangeUid,
        facilityService.getUid(),
        facilityService.getUri())));
    // @formatter:on
    ByteString reqToken = request.getRegistrationToken();
    if (!expectedToken.equals(reqToken)) {
      throw new IllegalArgumentException("The registration token is not valid.");
    }

    ExchangeEntity entity = new ExchangeEntity(Instant.now(), reg.getId());
    entity.setExchangeEndpointUri(reg.getExchangeEndpointUri());
    entity.setExchangePublicKey(reg.getExchangePublicKey());
    return exchangeDao.save(entity);
  }

  /**
   * Set the channel provider to use for connecting to the exchange registry.
   * 
   * @param exchangeRegistryChannelProvider
   *        the channel provider to use
   */
  public void setExchangeRegistryChannelProvider(ChannelProvider exchangeRegistryChannelProvider) {
    this.exchangeRegistryChannelProvider = exchangeRegistryChannelProvider;
  }

  /**
   * Set the channel provider to use for connecting to exchanges.
   * 
   * @param exchangeChannelProvider
   *        the exchangeChannelProvider to set
   */
  public void setExchangeChannelProvider(ChannelProvider exchangeChannelProvider) {
    this.exchangeChannelProvider = exchangeChannelProvider;
  }

}
