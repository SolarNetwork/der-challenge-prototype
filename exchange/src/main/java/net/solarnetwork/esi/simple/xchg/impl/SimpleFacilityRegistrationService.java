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

import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import org.apache.commons.codec.digest.DigestUtils;
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
import io.grpc.ManagedChannelBuilder;
import net.solarnetwork.esi.domain.DerFacilityRegistration;
import net.solarnetwork.esi.domain.DerRoute;
import net.solarnetwork.esi.domain.MessageSignature;
import net.solarnetwork.esi.service.DerFacilityServiceGrpc;
import net.solarnetwork.esi.service.DerFacilityServiceGrpc.DerFacilityServiceFutureStub;
import net.solarnetwork.esi.simple.xchg.dao.FacilityEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.FacilityRegistrationEntityDao;
import net.solarnetwork.esi.simple.xchg.domain.FacilityEntity;
import net.solarnetwork.esi.simple.xchg.domain.FacilityRegistrationEntity;
import net.solarnetwork.esi.simple.xchg.service.FacilityRegistrationService;
import net.solarnetwork.esi.util.CryptoHelper;

/**
 * Simple implementation of {@link FacilityRegistrationService}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleFacilityRegistrationService implements FacilityRegistrationService {

  private static final Charset UTF8 = Charset.forName("UTF-8");

  @Autowired
  private FacilityEntityDao facilityDao;

  @Autowired
  private FacilityRegistrationEntityDao facilityRegistrationDao;

  @Autowired
  private Executor taskExecutor;

  private final String operatorUid;
  private final KeyPair operatorKeyPair;
  private final CryptoHelper cryptoHelper;
  private boolean usePlaintext;

  /**
   * Constructor.
   * 
   * @param operatorUid
   *        the operator UID
   */
  @Autowired
  public SimpleFacilityRegistrationService(@Qualifier("operator-uid") String operatorUid,
      @Qualifier("operator-key-pair") KeyPair operatorKeyPair, CryptoHelper cryptoHelper) {
    super();
    this.operatorUid = operatorUid;
    this.operatorKeyPair = operatorKeyPair;
    this.cryptoHelper = cryptoHelper;
    this.usePlaintext = false;
    this.taskExecutor = ForkJoinPool.commonPool();
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

    // SHA256(operatorNonce + facilityNonce + operatorUid + facilityUid + facilityUri)
    MessageDigest sha256 = DigestUtils.getSha256Digest();
    sha256.update(registration.getOperatorNonce());
    sha256.update(registration.getFacilityNonce());
    sha256.update(operatorUid.getBytes(UTF8));
    sha256.update(entity.getFacilityUid().getBytes(UTF8));
    sha256.update(entity.getFacilityEndpointUri().getBytes(UTF8));
    ByteString token = ByteString.copyFrom(sha256.digest());

    // @formatter:off
    
    // sign message
    MessageSignature msgSig = generateMessageSignature(cryptoHelper, operatorKeyPair,
        decodePublicKey(cryptoHelper, entity.getFacilityPublicKey()),
        asList(
            operatorUid, 
            entity.getFacilityUid(), 
            entity.getFacilityEndpointUri(),
            registration.getFacilityNonce()));

    DerFacilityRegistration reg = DerFacilityRegistration.newBuilder()
        .setRegistrationToken(token)
        .setSuccess(true)
        .setRoute(DerRoute.newBuilder()
            .setOperatorUid(operatorUid)
            .setFacilityUid(registration.getFacilityUid())
            .setSignature(msgSig)
            .build())
        .build();
    
    // @formatter:on

    ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
        .forTarget(registration.getFacilityEndpointUri());
    if (usePlaintext) {
      channelBuilder.usePlaintext();
    }
    ManagedChannel channel = channelBuilder.build();
    DerFacilityServiceFutureStub client = DerFacilityServiceGrpc.newFutureStub(channel);

    final FacilityEntity finalEntity = entity;
    CompletableFuture<FacilityEntity> result = new CompletableFuture<FacilityEntity>();
    ListenableFuture<Empty> future = client.completeDerFacilityRegistration(reg);
    Futures.addCallback(future, new FutureCallback<Empty>() {

      @Override
      public void onSuccess(Empty r) {
        result.complete(finalEntity);
      }

      @Override
      public void onFailure(Throwable t) {
        result.completeExceptionally(t);

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
   * Toggle the use of plain text vs SSL.
   * 
   * @param usePlaintext
   *        {@literal true} to use plain text, {@literal false} to use SSL
   */
  public void setUsePlaintext(boolean usePlaintext) {
    this.usePlaintext = usePlaintext;
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

}
