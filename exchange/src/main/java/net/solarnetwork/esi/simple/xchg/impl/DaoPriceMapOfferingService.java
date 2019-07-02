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
import static java.util.stream.Collectors.toList;
import static net.solarnetwork.esi.util.CryptoUtils.decodePublicKey;
import static net.solarnetwork.esi.util.CryptoUtils.generateMessageSignature;
import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

import java.net.URI;
import java.security.KeyPair;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.grpc.ManagedChannel;
import net.solarnetwork.esi.domain.DerRoute;
import net.solarnetwork.esi.domain.PriceMapOffer;
import net.solarnetwork.esi.domain.PriceMapOfferResponse;
import net.solarnetwork.esi.domain.PriceMapOfferResponseOrBuilder;
import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
import net.solarnetwork.esi.domain.support.ProtobufUtils;
import net.solarnetwork.esi.grpc.ChannelProvider;
import net.solarnetwork.esi.service.DerFacilityServiceGrpc;
import net.solarnetwork.esi.service.DerFacilityServiceGrpc.DerFacilityServiceFutureStub;
import net.solarnetwork.esi.simple.xchg.dao.FacilityEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.FacilityPriceMapOfferEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.PriceMapOfferingEntityDao;
import net.solarnetwork.esi.simple.xchg.domain.FacilityPriceMapOfferEntity;
import net.solarnetwork.esi.simple.xchg.domain.PriceMapEntity;
import net.solarnetwork.esi.simple.xchg.domain.PriceMapOfferingEntity;
import net.solarnetwork.esi.simple.xchg.service.PriceMapOfferingService;
import net.solarnetwork.esi.util.CryptoHelper;

/**
 * DAO based implementation of {@link PriceMapOfferingService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoPriceMapOfferingService implements PriceMapOfferingService {

  private final String exchangeUid;
  private final KeyPair exchangeKeyPair;
  private final CryptoHelper cryptoHelper;
  private ChannelProvider facilityChannelProvider;
  private FacilityEntityDao facilityDao;
  private FacilityPriceMapOfferEntityDao priceMapOfferDao;
  private PriceMapOfferingEntityDao offeringDao;
  private TransactionTemplate txTemplate;
  private Executor taskExecutor;

  private static final Logger log = LoggerFactory.getLogger(DaoPriceMapOfferingService.class);

  /**
   * Constructor.
   * 
   * @param exchangeUid
   *        the exchange UID
   * @param exchangeKeyPair
   *        the exchange key pair
   * @param cryptoHelper
   *        the crypto helper
   */
  public DaoPriceMapOfferingService(@Qualifier("exchange-uid") String exchangeUid,
      @Qualifier("exchange-key-pair") KeyPair exchangeKeyPair, CryptoHelper cryptoHelper) {
    super();
    if (exchangeUid == null || exchangeUid.isEmpty()) {
      throw new IllegalArgumentException("The exchange UID must not be empty.");
    }
    this.exchangeUid = exchangeUid;
    if (exchangeKeyPair == null) {
      throw new IllegalArgumentException("The exchange key pair must be provided.");
    }
    this.exchangeKeyPair = exchangeKeyPair;
    if (cryptoHelper == null) {
      throw new IllegalArgumentException("The crypto helper must be provided.");
    }
    this.cryptoHelper = cryptoHelper;
    this.taskExecutor = ForkJoinPool.commonPool();
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public PriceMapOfferingEntity createPriceMapOffering(PriceMapEmbed priceMap, Instant startDate) {
    PriceMapOfferingEntity offering = new PriceMapOfferingEntity(Instant.now());
    offering.setStartDate(startDate);
    offering.setPriceMap(new PriceMapEntity(Instant.now(), priceMap));
    offering = offeringDao.save(offering);
    return offering;
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public Future<Iterable<FacilityPriceMapOfferEntity>> makeOfferToFacilities(UUID offeringId,
      Set<String> facilityUids) {
    PriceMapOfferingEntity offering = offeringDao.findById(offeringId)
        .orElseThrow(() -> new IllegalArgumentException("Offering not available."));

    // generate a list of offers to send to the facilities in this offering for async processing
    List<QueuedPriceMapOffer> offers = new ArrayList<>(facilityUids.size());
    facilityDao.findAllByFacilityUidIn(facilityUids).forEach(facility -> {
      UUID offerId = UUID.randomUUID();
      FacilityPriceMapOfferEntity offer = new FacilityPriceMapOfferEntity(Instant.now(), offerId);
      offer.setFacility(facility);
      offering.addOffer(offer);
      offer = priceMapOfferDao.save(offer);

      // @formatter:off
      PriceMapOffer pmo = PriceMapOffer.newBuilder()
          .setOfferId(ProtobufUtils.uuidForUuid(offerId))
          .setPriceMap(ProtobufUtils.priceMapForPriceMapEmbed(offering.priceMap().priceMap()))
          .setRoute(DerRoute.newBuilder()
              .setExchangeUid(exchangeUid)
              .setFacilityUid(facility.getFacilityUid())
              .setSignature(generateMessageSignature(cryptoHelper, exchangeKeyPair,
                  decodePublicKey(cryptoHelper, facility.getFacilityPublicKey()),
                  asList(exchangeUid, facility.getFacilityUid(), offering)))
              .build())
          .build();
      // @formatter:on

      offers.add(new QueuedPriceMapOffer(facility.facilityUri(), pmo));
    });

    // save the offering now
    offeringDao.save(offering);

    // register a post-commit hook to start sending the offers to the facilities
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      registerSynchronization(new TransactionSynchronizationAdapter() {

        @Override
        public void afterCommit() {
          for (QueuedPriceMapOffer qpmo : offers) {
            proposeOfferToFacility(offeringId, qpmo);
          }
        }

      });
    } else {
      for (QueuedPriceMapOffer qpmo : offers) {
        proposeOfferToFacility(offeringId, qpmo);
      }
    }

    // return a single Future that completes when all facilities have been contacted
    @SuppressWarnings("rawtypes")
    CompletableFuture[] cfs = offers.stream().map(e -> e.future).collect(toList())
        .toArray(new CompletableFuture[offers.size()]);
    CompletableFuture<Iterable<FacilityPriceMapOfferEntity>> cf = CompletableFuture.allOf(cfs)
        .thenApply(
            l -> offers.stream().map(e -> e.future).map(CompletableFuture::join).collect(toList()));
    return cf;
  }

  /**
   * Internal structure used to manage queue of price map offers.
   */
  private static final class QueuedPriceMapOffer {

    private final URI facilityUri;
    private final PriceMapOffer pmo;
    private final CompletableFuture<FacilityPriceMapOfferEntity> future;

    private QueuedPriceMapOffer(URI facilityUri, PriceMapOffer pmo) {
      super();
      this.facilityUri = facilityUri;
      this.pmo = pmo;
      this.future = new CompletableFuture<FacilityPriceMapOfferEntity>();
    }

  }

  private void proposeOfferToFacility(UUID offeringId, QueuedPriceMapOffer qpmo) {
    final PriceMapOffer pmo = qpmo.pmo;
    final String facilityUid = pmo.getRoute().getFacilityUid();
    final UUID offerId = ProtobufUtils.uuidValue(pmo.getOfferId());

    ManagedChannel channel = facilityChannelProvider.channelForUri(qpmo.facilityUri);
    DerFacilityServiceFutureStub client = DerFacilityServiceGrpc.newFutureStub(channel);

    // CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINE
    ListenableFuture<PriceMapOfferResponse> future = client.proposePriceMapOffer(pmo);
    Futures.addCallback(future, new FutureCallback<PriceMapOfferResponse>() {

      @Override
      public void onSuccess(PriceMapOfferResponse r) {
        log.info("Successfully proposed price map offer [{}] to facility [{}]", offerId,
            facilityUid);
        try {
          FacilityPriceMapOfferEntity entity = null;
          TransactionTemplate tt = txTemplate();
          if (tt != null) {
            entity = tt.execute(new TransactionCallback<FacilityPriceMapOfferEntity>() {

              @Override
              public FacilityPriceMapOfferEntity doInTransaction(TransactionStatus status) {
                return handlePriceMapOfferResponse(offeringId, offerId, r);
              }
            });
          } else {
            entity = handlePriceMapOfferResponse(offeringId, offerId, r);
          }
          qpmo.future.complete(entity);
        } catch (RuntimeException e) {
          qpmo.future.completeExceptionally(e);
        }
        channel.shutdown();
      }

      @Override
      public void onFailure(Throwable t) {
        log.info("Error proposing price map offering [{}] to facility [{}]: {}", offerId,
            facilityUid, t.getMessage());
        qpmo.future.completeExceptionally(t);
        channel.shutdown();
      }
    }, taskExecutor);
  }

  private FacilityPriceMapOfferEntity handlePriceMapOfferResponse(UUID offeringId, UUID offerId,
      PriceMapOfferResponseOrBuilder response) {
    UUID responseUuid = ProtobufUtils.uuidValue(response.getOfferId());
    if (!offerId.equals(responseUuid)) {
      throw new IllegalArgumentException("The offer ID does not match the expected value.");
    }
    FacilityPriceMapOfferEntity offer = priceMapOfferDao.findById(offerId).orElseThrow(
        () -> new IllegalArgumentException("Price map offer [" + offerId + "] not found."));
    if (response.hasCounterOffer()) {
      PriceMapEntity tmp = PriceMapEntity.entityForMessage(response.getCounterOffer());
      UUID counterOfferId = UUID.randomUUID();
      PriceMapEntity counterOfferPriceMap = new PriceMapEntity(Instant.now(), counterOfferId);
      counterOfferPriceMap.setPriceMap(tmp.getPriceMap());
      offer.setPriceMap(counterOfferPriceMap);
      offer.setProposed(true);
      offer.setAccepted(true); // NOTE: we are blindly accepting any counter-offer
      log.info("Offer [{}] to facility [{}] counter offered [{}]", offeringId, offerId,
          counterOfferId);
    } else {
      offer.setProposed(true);
      offer.setAccepted(response.getAccept());
      log.info("Offer [{}] to facility [{}] {}", offerId, offer.getFacility().getFacilityUid(),
          response.getAccept() ? "accepted" : "declined");
    }
    return priceMapOfferDao.save(offer);
  }

  private TransactionTemplate txTemplate() {
    return txTemplate;
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

  /**
   * Set a {@link TransactionTemplate} to use for fine-grained transaction support.
   * 
   * @param transactionTemplate
   *        the transaction template to use
   */
  public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
    this.txTemplate = transactionTemplate;
  }

  /**
   * Set the DAO to use for facility data.
   * 
   * @param facilityDao
   *        the DAO to use
   */
  public void setFacilityDao(FacilityEntityDao facilityDao) {
    this.facilityDao = facilityDao;
  }

  /**
   * Set the DAO to use for offering data.
   * 
   * @param offeringDao
   *        the DAO to use
   */
  public void setOfferingDao(PriceMapOfferingEntityDao offeringDao) {
    this.offeringDao = offeringDao;
  }

  /**
   * Set the DAO to use for price map offers.
   * 
   * @param priceMapOfferDao
   *        the DAO to use
   */
  public void setPriceMapOfferDao(FacilityPriceMapOfferEntityDao priceMapOfferDao) {
    this.priceMapOfferDao = priceMapOfferDao;
  }

}
