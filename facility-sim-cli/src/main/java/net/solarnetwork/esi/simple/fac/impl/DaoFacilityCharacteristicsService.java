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

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
import net.solarnetwork.esi.domain.DurationRange;
import net.solarnetwork.esi.domain.DurationRangeEmbed;
import net.solarnetwork.esi.grpc.ChannelProvider;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc.DerFacilityExchangeStub;
import net.solarnetwork.esi.simple.fac.dao.ResourceCharacteristicsEntityDao;
import net.solarnetwork.esi.simple.fac.domain.ExchangeEntity;
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
    if (exchange != null) {
      ManagedChannel channel = exchangeChannelProvider
          .channelForUri(URI.create(exchange.getExchangeEndpointUri()));
      try {
        DerFacilityExchangeStub client = DerFacilityExchangeGrpc.newStub(channel);
        final CountDownLatch latch = new CountDownLatch(1);
        StreamObserver<Empty> out = new StreamObserver<Empty>() {

          @Override
          public void onNext(Empty value) {
            // nothing to do with result
          }

          @Override
          public void onError(Throwable t) {
            latch.countDown();
          }

          @Override
          public void onCompleted() {
            latch.countDown();
          }
        };
        StreamObserver<DerCharacteristics> in = client.provideDerCharacteristics(out);
        // @formatter:off
        in.onNext(DerCharacteristics.newBuilder()
            .setLoadPowerMax(entity.getLoadPowerMax())
            .setLoadPowerFactor(entity.getLoadPowerFactor())
            .setSupplyPowerMax(entity.getSupplyPowerMax())
            .setSupplyPowerFactor(entity.getSupplyPowerFactor())
            .setStorageEnergyCapacity(entity.getStorageEnergyCapacity())
            .setReponseTime(DurationRange.newBuilder()
                .setMin(com.google.protobuf.Duration.newBuilder()
                    .setSeconds(entity.getResponseTime().getMin().getSeconds())
                    .setNanos(entity.getResponseTime().getMin().getNano())
                    .build())
                .setMax(com.google.protobuf.Duration.newBuilder()
                    .setSeconds(entity.getResponseTime().getMax().getSeconds())
                    .setNanos(entity.getResponseTime().getMax().getNano())
                    .build())
                .build())
            .build());
        // @formatter:on
        in.onCompleted();
        latch.await(1, TimeUnit.MINUTES);
        resourceCharacteristicsDao.save(entity);
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
