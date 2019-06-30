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

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.protobuf.Empty;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.solarnetwork.esi.domain.DerCharacteristics;
import net.solarnetwork.esi.domain.DerProgramSet;
import net.solarnetwork.esi.domain.DerProgramType;
import net.solarnetwork.esi.domain.DerRoute;
import net.solarnetwork.esi.domain.DurationRange;
import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.grpc.ChannelProvider;
import net.solarnetwork.esi.grpc.FutureStreamObserver;
import net.solarnetwork.esi.grpc.QueuingStreamObserver;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc.DerFacilityExchangeStub;
import net.solarnetwork.esi.simple.fac.dao.ResourceCharacteristicsEntityDao;
import net.solarnetwork.esi.simple.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.simple.fac.domain.PriceMapEntity;
import net.solarnetwork.esi.simple.fac.domain.ResourceCharacteristicsEntity;
import net.solarnetwork.esi.simple.fac.service.FacilityCharacteristicsService;
import net.solarnetwork.esi.simple.fac.service.FacilityService;

/**
 * DAO based implementation of {@link FacilityCharacteristicsService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoFacilityCharacteristicsService implements FacilityCharacteristicsService {

  private final FacilityService facilityService;
  private final ResourceCharacteristicsEntityDao resourceCharacteristicsDao;
  private ChannelProvider exchangeChannelProvider;

  private static final Logger log = LoggerFactory
      .getLogger(DaoFacilityCharacteristicsService.class);

  /**
   * Constructor.
   * 
   * @param resourceCharacteristicsDao
   *        the resource characteristics DAO
   */
  public DaoFacilityCharacteristicsService(FacilityService facilityService,
      ResourceCharacteristicsEntityDao resourceCharacteristicsDao) {
    super();
    this.facilityService = facilityService;
    this.resourceCharacteristicsDao = resourceCharacteristicsDao;
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public ResourceCharacteristicsEntity resourceCharacteristics() {
    Iterator<ResourceCharacteristicsEntity> itr = resourceCharacteristicsDao.findAll().iterator();
    ResourceCharacteristicsEntity entity = (itr.hasNext() ? itr.next() : null);
    if (entity == null) {
      entity = new ResourceCharacteristicsEntity(Instant.now());
      entity.setLoadPowerMax(0L);
      entity.setLoadPowerFactor(0f);
      entity.setSupplyPowerMax(0L);
      entity.setSupplyPowerFactor(0f);
      entity.setStorageEnergyCapacity(0L);
      entity.setResponseTime(new DurationRangeEmbed(Duration.ofMillis(0), Duration.ofMillis(0)));
    }
    return entity;
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public void saveResourceCharacteristics(ResourceCharacteristicsEntity characteristics) {
    ResourceCharacteristicsEntity entity = resourceCharacteristics();
    if (characteristics.getLoadPowerMax() != null) {
      entity.setLoadPowerMax(characteristics.getLoadPowerMax());
    }
    if (characteristics.getLoadPowerFactor() != null) {
      entity.setLoadPowerFactor(characteristics.getLoadPowerFactor());
    }
    if (characteristics.getSupplyPowerMax() != null) {
      entity.setSupplyPowerMax(characteristics.getSupplyPowerMax());
    }
    if (characteristics.getSupplyPowerFactor() != null) {
      entity.setSupplyPowerFactor(characteristics.getSupplyPowerFactor());
    }
    if (characteristics.getStorageEnergyCapacity() != null) {
      entity.setStorageEnergyCapacity(characteristics.getStorageEnergyCapacity());
    }
    if (characteristics.getResponseTime() != null) {
      if (entity.getResponseTime() == null) {
        entity.setResponseTime(characteristics.getResponseTime());
      } else {
        if (characteristics.getResponseTime().getMin() != null) {
          entity.getResponseTime().setMin(characteristics.getResponseTime().getMin());
        }
        if (characteristics.getResponseTime().getMax() != null) {
          entity.getResponseTime().setMax(characteristics.getResponseTime().getMax());
        }
      }
    }
    ExchangeEntity exchange = facilityService.getExchange();
    if (exchange == null) {
      resourceCharacteristicsDao.save(entity);
    } else {
      ManagedChannel channel = exchangeChannelProvider
          .channelForUri(URI.create(exchange.getExchangeEndpointUri()));
      try {
        DerFacilityExchangeStub client = DerFacilityExchangeGrpc.newStub(channel);
        FutureStreamObserver<Empty, Iterable<Empty>> out = new QueuingStreamObserver<>(1);
        StreamObserver<DerCharacteristics> in = client.provideDerCharacteristics(out);
        // @formatter:off
        in.onNext(DerCharacteristics.newBuilder()
            .setLoadPowerMax(entity.getLoadPowerMax())
            .setLoadPowerFactor(entity.getLoadPowerFactor())
            .setSupplyPowerMax(entity.getSupplyPowerMax())
            .setSupplyPowerFactor(entity.getSupplyPowerFactor())
            .setStorageEnergyCapacity(entity.getStorageEnergyCapacity())
            .setResponseTime(DurationRange.newBuilder()
                .setMin(com.google.protobuf.Duration.newBuilder()
                    .setSeconds(entity.getResponseTime().getMin().getSeconds())
                    .setNanos(entity.getResponseTime().getMin().getNano())
                    .build())
                .setMax(com.google.protobuf.Duration.newBuilder()
                    .setSeconds(entity.getResponseTime().getMax().getSeconds())
                    .setNanos(entity.getResponseTime().getMax().getNano())
                    .build())
                .build())
            .setRoute(DerRoute.newBuilder()
                .setExchangeUid(exchange.getId())
                .setFacilityUid(facilityService.getUid())
                .setSignature(generateMessageSignature(facilityService.getCryptoHelper(), 
                    facilityService.getKeyPair(), exchange.publicKey(), asList(
                        exchange.getId(),
                        facilityService.getUid(),
                        characteristics.toSignatureBytes()))
                    )
                .build())
            .build());
        // @formatter:on
        in.onCompleted();
        out.nab(1, TimeUnit.MINUTES);
        log.info("Successfully published resource characteristics to exchange {}",
            exchange.getId());
        resourceCharacteristicsDao.save(entity);
      } catch (TimeoutException e) {
        throw new RuntimeException(
            "Timeout waiting to publish resource characteristics to exchange " + exchange.getId(),
            e);
      } catch (StatusRuntimeException e) {
        if (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) {
          throw new IllegalArgumentException(e.getStatus().getDescription());
        } else {
          throw e;
        }
      } catch (InterruptedException e) {
        throw new RuntimeException("Interrupted waiting for result.");
      } finally {
        channel.shutdown();
        try {
          channel.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
          log.debug("Timeout waiting for channel to shut down.");
        }
      }
    }
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public Set<String> activeProgramTypes() {
    return facilityService.getEnabledProgramTypes();
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public void saveActiveProgramTypes(Set<String> programs) {
    ExchangeEntity exchange = facilityService.getExchange();
    if (exchange == null) {
      facilityService.setEnabledProgramTypes(programs);
    } else {
      ManagedChannel channel = exchangeChannelProvider
          .channelForUri(URI.create(exchange.getExchangeEndpointUri()));
      try {
        DerFacilityExchangeStub client = DerFacilityExchangeGrpc.newStub(channel);
        FutureStreamObserver<Empty, Iterable<Empty>> out = new QueuingStreamObserver<>(1);
        StreamObserver<DerProgramSet> in = client.provideSupportedDerPrograms(out);
        DerProgramSet.Builder derProgramSetBuilder = DerProgramSet.newBuilder();
        ByteBuffer signatureData = ByteBuffer.allocate(Integer.BYTES * programs.size());
        for (String program : programs) {
          if (program == null) {
            continue;
          }
          try {
            DerProgramType type = DerProgramType.valueOf(program);
            signatureData.putInt(type.getNumber());
            derProgramSetBuilder.addType(type);
          } catch (IllegalArgumentException e) {
            log.info("Program type {} is not supported by exchange API; ignoring.", program);
          }
        }
        // @formatter:off
        DerProgramSet derProgram = derProgramSetBuilder
            .setRoute(DerRoute.newBuilder()
                .setExchangeUid(exchange.getId())
                .setFacilityUid(facilityService.getUid())
                .setSignature(generateMessageSignature(facilityService.getCryptoHelper(), 
                    facilityService.getKeyPair(), exchange.publicKey(), asList(
                        exchange.getId(),
                        facilityService.getUid(),
                        signatureData))
                    )
                .build())
            .build();
        // @formatter:on
        in.onNext(derProgram);
        in.onCompleted();
        out.nab(1, TimeUnit.MINUTES);
        log.info("Successfully published active programs {} to exchange {}", programs,
            exchange.getId());
        facilityService.setEnabledProgramTypes(programs);
      } catch (TimeoutException e) {
        throw new RuntimeException(
            "Timeout waiting to publish resource characteristics to exchange " + exchange.getId(),
            e);
      } catch (StatusRuntimeException e) {
        if (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) {
          throw new IllegalArgumentException(e.getStatus().getDescription());
        } else {
          throw e;
        }
      } catch (InterruptedException e) {
        throw new RuntimeException("Interrupted waiting for result.");
      } finally {
        channel.shutdown();
        try {
          channel.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
          log.debug("Timeout waiting for channel to shut down.");
        }
      }
    }
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public PriceMapEntity priceMap() {
    return facilityService.getPriceMap();
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public void savePriceMap(PriceMapEntity priceMap) {
    facilityService.savePriceMap(priceMap);
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
