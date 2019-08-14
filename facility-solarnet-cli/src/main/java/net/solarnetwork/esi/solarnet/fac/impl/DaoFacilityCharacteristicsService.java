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

package net.solarnetwork.esi.solarnet.fac.impl;

import static net.solarnetwork.esi.util.CryptoUtils.generateMessageSignature;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import net.solarnetwork.esi.domain.DerRoute;
import net.solarnetwork.esi.domain.PriceMapCharacteristics;
import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
import net.solarnetwork.esi.domain.support.ProtobufUtils;
import net.solarnetwork.esi.grpc.ChannelProvider;
import net.solarnetwork.esi.grpc.FutureStreamObserver;
import net.solarnetwork.esi.grpc.QueuingStreamObserver;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc.DerFacilityExchangeStub;
import net.solarnetwork.esi.solarnet.fac.dao.FacilityPriceMapDao;
import net.solarnetwork.esi.solarnet.fac.dao.FacilityResourceDao;
import net.solarnetwork.esi.solarnet.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityPriceMap;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityResourceCharacteristics;
import net.solarnetwork.esi.solarnet.fac.service.FacilityCharacteristicsService;
import net.solarnetwork.esi.solarnet.fac.service.FacilityService;

/**
 * DAO based implementation of {@link FacilityCharacteristicsService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoFacilityCharacteristicsService implements FacilityCharacteristicsService {

  private final FacilityService facilityService;
  private final FacilityPriceMapDao priceMapDao;
  private final FacilityResourceDao resourceCharacteristicsDao;
  private ChannelProvider exchangeChannelProvider;

  private static final Logger log = LoggerFactory
      .getLogger(DaoFacilityCharacteristicsService.class);

  /**
   * Constructor.
   * 
   * @param facilityService
   *        the facility service
   * @param priceMapDao
   *        the price map DAO
   * @param resourceCharacteristicsDao
   *        the resource characteristics DAO
   */
  public DaoFacilityCharacteristicsService(FacilityService facilityService,
      FacilityPriceMapDao priceMapDao, FacilityResourceDao resourceCharacteristicsDao) {
    super();
    this.facilityService = facilityService;
    this.priceMapDao = priceMapDao;
    this.resourceCharacteristicsDao = resourceCharacteristicsDao;
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public Iterable<FacilityResourceCharacteristics> resourceCharacteristics() {
    return resourceCharacteristicsDao.findAll();
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public Set<String> activeProgramTypes() {
    return facilityService.getEnabledProgramTypes();
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public Iterable<FacilityPriceMap> priceMaps() {
    return facilityService.getPriceMaps();
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public FacilityPriceMap priceMap(String priceMapId) {
    return priceMapDao.findById(priceMapId).orElseThrow(
        () -> new IllegalArgumentException("Price map " + priceMapId + " not available."));
  }

  @Override
  public void registerPriceMap(String priceMapId) {
    FacilityPriceMap priceMap = priceMap(priceMapId);
    postPriceMapsToExchange(Collections.singleton(priceMap));
  }

  private void postPriceMapsToExchange(Iterable<FacilityPriceMap> priceMaps) {
    ExchangeEntity exchange = facilityService.getExchange();
    if (exchange != null) {
      List<Object> messageData = new ArrayList<>();
      messageData.add(exchange.getId());
      messageData.add(facilityService.getUid());
      PriceMapCharacteristics.Builder pmc = PriceMapCharacteristics.newBuilder();
      for (FacilityPriceMap pm : priceMaps) {
        PriceMapEmbed pme = pm.priceMap();
        messageData.add(pme);
        pmc.addPriceMap(ProtobufUtils.priceMapForPriceMapEmbed(pme));
      }
      ManagedChannel channel = exchangeChannelProvider
          .channelForUri(URI.create(exchange.getExchangeEndpointUri()));
      try {
        DerFacilityExchangeStub client = DerFacilityExchangeGrpc.newStub(channel);
        FutureStreamObserver<Empty, Iterable<Empty>> in = new QueuingStreamObserver<>(1);
        StreamObserver<PriceMapCharacteristics> out = client.providePriceMaps(in);
        // @formatter:off
        out.onNext(pmc.setRoute(DerRoute.newBuilder()
                .setExchangeUid(exchange.getId())
                .setFacilityUid(facilityService.getUid())
                .setSignature(generateMessageSignature(facilityService.getCryptoHelper(), 
                    facilityService.getKeyPair(), exchange.publicKey(), messageData))
                .build())
            .build());
        // @formatter:on
        out.onCompleted();
        in.nab(1, TimeUnit.MINUTES);
        log.info("Successfully published price map list to exchange {}", exchange.getId());
      } catch (TimeoutException e) {
        throw new RuntimeException(
            "Timeout waiting to publish price map list to exchange " + exchange.getId(), e);
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
