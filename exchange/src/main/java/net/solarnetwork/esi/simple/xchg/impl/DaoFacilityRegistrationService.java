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

package net.solarnetwork.esi.simple.xchg.impl;

import static java.util.Arrays.asList;
import static net.solarnetwork.esi.util.CryptoUtils.decodePublicKey;
import static net.solarnetwork.esi.util.CryptoUtils.generateMessageSignature;
import static net.solarnetwork.esi.util.CryptoUtils.validateMessageSignature;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;

import io.grpc.ManagedChannel;
import net.solarnetwork.esi.domain.CryptoKey;
import net.solarnetwork.esi.domain.DerFacilityRegistration;
import net.solarnetwork.esi.domain.DerFacilityRegistrationFormData;
import net.solarnetwork.esi.domain.DerRoute;
import net.solarnetwork.esi.domain.DerRouteOrBuilder;
import net.solarnetwork.esi.domain.Form;
import net.solarnetwork.esi.domain.FormData;
import net.solarnetwork.esi.domain.MessageSignature;
import net.solarnetwork.esi.grpc.ChannelProvider;
import net.solarnetwork.esi.service.DerFacilityServiceGrpc;
import net.solarnetwork.esi.service.DerFacilityServiceGrpc.DerFacilityServiceFutureStub;
import net.solarnetwork.esi.simple.xchg.dao.FacilityEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.FacilityRegistrationEntityDao;
import net.solarnetwork.esi.simple.xchg.domain.FacilityEntity;
import net.solarnetwork.esi.simple.xchg.domain.FacilityRegistrationEntity;
import net.solarnetwork.esi.simple.xchg.service.FacilityRegistrationService;
import net.solarnetwork.esi.util.CryptoHelper;
import net.solarnetwork.esi.util.CryptoUtils;

/**
 * DAO based implementation of {@link FacilityRegistrationService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoFacilityRegistrationService implements FacilityRegistrationService {

  @Autowired
  private FacilityEntityDao facilityDao;

  @Autowired
  private FacilityRegistrationEntityDao facilityRegistrationDao;

  @Autowired
  private Executor taskExecutor;

  private static final Logger log = LoggerFactory.getLogger(DaoFacilityRegistrationService.class);

  private final String exchangeUid;
  private final KeyPair exchangeKeyPair;
  private final List<Form> registrationForms;
  private final CryptoHelper cryptoHelper;
  private ChannelProvider facilityChannelProvider;

  /**
   * Constructor.
   * 
   * @param exchangeUid
   *        the exchange UID
   * @param exchangeKeyPair
   *        the key pair to use for asymmetric encryption with facilities
   * @param registrationForms
   *        the registration form, as a list to support multiple languages
   * @param cryptoHelper
   *        the {@link CryptoHelper} to use
   * @throws IllegalArgumentException
   *         if any parameter is {@literal null} or empty
   */
  @Autowired
  public DaoFacilityRegistrationService(@Qualifier("exchange-uid") String exchangeUid,
      @Qualifier("exchange-key-pair") KeyPair exchangeKeyPair,
      @Qualifier("regform-list") List<Form> registrationForms, CryptoHelper cryptoHelper) {
    super();
    if (exchangeUid == null || exchangeUid.isEmpty()) {
      throw new IllegalArgumentException("The exchange UID must not be empty.");
    }
    this.exchangeUid = exchangeUid;
    if (exchangeKeyPair == null) {
      throw new IllegalArgumentException("The exchange key pair must be provided.");
    }
    this.exchangeKeyPair = exchangeKeyPair;
    if (registrationForms == null || registrationForms.isEmpty()) {
      throw new IllegalArgumentException("The registration forms list must not be empty.");
    }
    this.registrationForms = Collections.unmodifiableList(new ArrayList<>(registrationForms));
    if (cryptoHelper == null) {
      throw new IllegalArgumentException("The crypto helper must be provided.");
    }
    this.cryptoHelper = cryptoHelper;
    this.taskExecutor = ForkJoinPool.commonPool();
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public FacilityRegistrationEntity submitDerFacilityRegistrationForm(
      DerFacilityRegistrationFormData request) {
    DerRouteOrBuilder route = request.getRouteOrBuilder();
    if (route == null) {
      throw new IllegalArgumentException("Route missing");
    }

    if (!exchangeUid.equals(route.getExchangeUid())) {
      throw new IllegalArgumentException("Exchange UID not valid.");
    }

    String facilityUid = route.getFacilityUid();
    if (facilityUid == null || facilityUid.trim().isEmpty()) {
      throw new IllegalArgumentException("Facility UID missing.");
    }

    // check if facility already exists, or registration for it exists
    if (facilityDao.findByFacilityUid(facilityUid).isPresent()) {
      throw new IllegalArgumentException("Facility already registered for UID.");
    }
    if (facilityRegistrationDao.findByFacilityUid(facilityUid).isPresent()) {
      throw new IllegalArgumentException("Facility registration already submitted for UID.");
    }

    String facilityEndpointUri = request.getFacilityEndpointUri();
    if (facilityEndpointUri == null || facilityEndpointUri.trim().isEmpty()) {
      throw new IllegalArgumentException("Facility endpoint URI missing.");
    }
    try {
      new URI(facilityEndpointUri);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Facility endpoint URI syntax not valid.", e);
    }

    CryptoKey facilityKey = request.getFacilityPublicKey();
    if (facilityKey == null) {
      throw new IllegalArgumentException("Facility public key missing");
    } else if (facilityKey.getKey() == null || facilityKey.getKey().isEmpty()) {
      throw new IllegalArgumentException("Facility public key value missing.");
    } else if (!"EC".equals(facilityKey.getAlgorithm())) {
      throw new IllegalArgumentException(
          "Facility public key algorithm not supported (must be 'EC').");
    } else if (!"X.509".equals(facilityKey.getEncoding())) {
      throw new IllegalArgumentException(
          "Facility public key encoding not supported (must be 'X.509').");
    }

    ByteString facilityNonce = request.getFacilityNonce();
    if (facilityNonce == null || facilityNonce.isEmpty()) {
      throw new IllegalArgumentException("Facility nonce missing");
    } else if (facilityNonce.size() < 8) {
      throw new IllegalArgumentException("Facility nonce must be at least 8 bytes long.");
    } else if (facilityNonce.size() > 24) {
      throw new IllegalArgumentException("Facility nonce must be at most 24 bytes long.");
    }

    // verify signature
    validateMessageSignature(cryptoHelper, route.getSignature(), exchangeKeyPair,
        cryptoHelper.decodePublicKey(facilityKey),
        asList(exchangeUid, facilityUid, facilityEndpointUri, facilityNonce));

    FormData formData = request.getData();
    if (formData == null) {
      throw new IllegalArgumentException("Form data missing.");
    }
    String formKey = formData.getKey();
    if (formKey == null || formKey.trim().isEmpty()) {
      throw new IllegalArgumentException("Form key missing");
    }
    Form form = registrationForms.stream().filter(f -> formKey.equals(f.getKey())).findFirst()
        .orElse(null);
    if (form == null) {
      throw new IllegalArgumentException("Form key invalid");
    }

    String uici = formData.getDataOrDefault(FORM_KEY_UICI, null);
    if (uici == null || uici.trim().isEmpty()) {
      throw new IllegalArgumentException("UICI value missing");
    } else if (!uici.matches("[1-9]{3}-[1-9]{4}-[1-9]{4}")) {
      throw new IllegalArgumentException("UICI invliad syntax; must be in form 123-1234-1234");
    }

    String custId = formData.getDataOrDefault(FORM_KEY_CUSTOMER_ID, null);
    if (custId == null || custId.trim().isEmpty()) {
      throw new IllegalArgumentException("Customer number value missing");
    } else if (!custId.matches("[A-Z]{3}[0-9]{9}")) {
      throw new IllegalArgumentException(
          "Customer number invalid syntax; must be in form ABC123456789");
    }

    String custSurname = formData.getDataOrDefault(FORM_KEY_CUSTOMER_SURNAME, null);
    if (custSurname == null || custSurname.trim().isEmpty()) {
      throw new IllegalArgumentException("Customer surname value missing");
    }

    // wow, it passed validation checks; generate our nonce and persist registration entity
    byte[] opNonce = CryptoUtils.generateRandomBytes(24);

    FacilityRegistrationEntity entity = new FacilityRegistrationEntity(Instant.now());
    entity.setCustomerId(custId);
    entity.setUici(uici);
    entity.setFacilityUid(facilityUid);
    entity.setFacilityEndpointUri(facilityEndpointUri);
    entity.setFacilityPublicKey(facilityKey.getKey().toByteArray());
    entity.setFacilityNonce(facilityNonce.toByteArray());
    entity.setExchangeNonce(opNonce);
    return facilityRegistrationDao.save(entity);
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public CompletableFuture<FacilityEntity> processFacilityRegistration(
      FacilityRegistrationEntity registration) {
    // automatically approve
    FacilityEntity entity = new FacilityEntity(Instant.now());
    entity.setCustomerId(registration.getCustomerId());
    entity.setFacilityEndpointUri(registration.getFacilityEndpointUri());
    entity.setFacilityPublicKey(registration.getFacilityPublicKey());
    entity.setFacilityUid(registration.getFacilityUid());
    entity.setUici(registration.getUici());
    entity = facilityDao.save(entity);

    // delete the registration entity
    facilityRegistrationDao.deleteById(registration.getId());

    // SHA256(exchangeNonce + facilityNonce + exchangeUid + facilityUid + facilityUri)
    ByteString token = ByteString.copyFrom(CryptoUtils
        .sha256(Arrays.asList(registration.getExchangeNonce(), registration.getFacilityNonce(),
            exchangeUid, entity.getFacilityUid(), entity.getFacilityEndpointUri())));

    // @formatter:off
    
    // sign message
    MessageSignature msgSig = generateMessageSignature(cryptoHelper, exchangeKeyPair,
        decodePublicKey(cryptoHelper, entity.getFacilityPublicKey()),
        asList(
            exchangeUid, 
            entity.getFacilityUid(), 
            entity.getFacilityEndpointUri(),
            registration.getFacilityNonce()));

    DerFacilityRegistration reg = DerFacilityRegistration.newBuilder()
        .setRegistrationToken(token)
        .setSuccess(true)
        .setRoute(DerRoute.newBuilder()
            .setExchangeUid(exchangeUid)
            .setFacilityUid(registration.getFacilityUid())
            .setSignature(msgSig)
            .build())
        .build();
    
    // @formatter:on

    ManagedChannel channel = facilityChannelProvider
        .channelForUri(URI.create(registration.getFacilityEndpointUri()));
    DerFacilityServiceFutureStub client = DerFacilityServiceGrpc.newFutureStub(channel);

    final FacilityEntity finalEntity = entity;
    CompletableFuture<FacilityEntity> result = new CompletableFuture<FacilityEntity>();
    ListenableFuture<Empty> future = client.completeDerFacilityRegistration(reg);
    Futures.addCallback(future, new FutureCallback<Empty>() {

      @Override
      public void onSuccess(Empty r) {
        log.info("Successfully completed registration for facility [" + finalEntity.getFacilityUid()
            + "]");
        result.complete(finalEntity);
        channel.shutdown();
      }

      @Override
      public void onFailure(Throwable t) {
        log.error("Error completing registration for facility [" + finalEntity.getFacilityUid()
            + "]: " + t.getMessage());
        result.completeExceptionally(t);
        channel.shutdown();
      }
    }, taskExecutor);

    return result;
  }

  /**
   * Set the DAO to use for facility data.
   * 
   * @param facilityDao
   *        the facility DAO to use
   */
  public void setFacilityDao(FacilityEntityDao facilityDao) {
    this.facilityDao = facilityDao;
  }

  /**
   * Set the DAO to use for facility registration data.
   * 
   * @param facilityRegistrationDao
   *        the facility registration DAO to use
   */
  public void setFacilityRegistrationDao(FacilityRegistrationEntityDao facilityRegistrationDao) {
    this.facilityRegistrationDao = facilityRegistrationDao;
  }

  /**
   * Set the executor to use for tasks.
   * 
   * @param taskExecutor
   *        the executor
   */
  public void setTaskExecutor(Executor taskExecutor) {
    this.taskExecutor = taskExecutor;
  }

  /**
   * Set the channel provider to use for facilities.
   * 
   * @param facilityChannelProvider
   *        the provider to set
   */
  public void setFacilityChannelProvider(ChannelProvider facilityChannelProvider) {
    this.facilityChannelProvider = facilityChannelProvider;
  }

}
