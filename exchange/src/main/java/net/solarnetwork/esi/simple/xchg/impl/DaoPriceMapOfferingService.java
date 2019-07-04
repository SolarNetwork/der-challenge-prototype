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
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import net.solarnetwork.esi.domain.DerRoute;
import net.solarnetwork.esi.domain.PriceMapOffer;
import net.solarnetwork.esi.domain.PriceMapOfferResponse;
import net.solarnetwork.esi.domain.PriceMapOfferResponseOrBuilder;
import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
import net.solarnetwork.esi.domain.support.ProtobufUtils;
import net.solarnetwork.esi.grpc.ChannelProvider;
import net.solarnetwork.esi.grpc.CompletableStreamObserver;
import net.solarnetwork.esi.grpc.FutureStreamObserver;
import net.solarnetwork.esi.service.DerFacilityServiceGrpc;
import net.solarnetwork.esi.service.DerFacilityServiceGrpc.DerFacilityServiceStub;
import net.solarnetwork.esi.simple.xchg.dao.FacilityEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.FacilityPriceMapOfferEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.PriceMapOfferingEntityDao;
import net.solarnetwork.esi.simple.xchg.domain.FacilityEntity;
import net.solarnetwork.esi.simple.xchg.domain.FacilityPriceMapOfferEntity;
import net.solarnetwork.esi.simple.xchg.domain.PriceMapEntity;
import net.solarnetwork.esi.simple.xchg.domain.PriceMapOfferingEntity;
import net.solarnetwork.esi.simple.xchg.domain.PriceMapOfferingEvent.FacilityPriceMapOfferCompleted;
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
  private ApplicationEventPublisher eventPublisher;

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
      PriceMapOffer pmo = createOffer(offering, facility, offering.priceMap().priceMap());
      offers.add(new QueuedPriceMapOffer(facility.facilityUri(), pmo));
    });

    // save the offering now
    offeringDao.save(offering);

    // register a post-commit hook to start sending the offers to the facilities
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      registerSynchronization(new TransactionSynchronizationAdapter() {

        @Override
        public void afterCommit() {
          try {
            for (QueuedPriceMapOffer qpmo : offers) {
              proposeOfferToFacility(offeringId, qpmo);
            }
          } catch (Exception e) {
            log.error("Error proposing offer {} to facility: {}", offeringId, e.getMessage(), e);
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

    private final PriceMapOffer initialOffer;
    private final URI facilityUri;
    private final CompletableFuture<FacilityPriceMapOfferEntity> future;
    private final Queue<UUID> offerIds;

    // our outbound offer stream, to deal with counter-offers
    private StreamObserver<PriceMapOffer> out;

    private QueuedPriceMapOffer(URI facilityUri, PriceMapOffer pmo) {
      super();
      this.facilityUri = facilityUri;
      this.initialOffer = pmo;
      this.future = new CompletableFuture<FacilityPriceMapOfferEntity>();
      this.offerIds = new ArrayBlockingQueue<>(64); // maximum number of counter offers essentially
      this.offerIds.add(ProtobufUtils.uuidValue(pmo.getOfferIdOrBuilder()));
    }

    public String facilityUid() {
      return initialOffer.getRoute().getFacilityUid();
    }

  }

  private PriceMapOffer createOffer(PriceMapOfferingEntity offering, FacilityEntity facility,
      PriceMapEmbed priceMap) {
    UUID offerId = UUID.randomUUID();
    FacilityPriceMapOfferEntity offer = new FacilityPriceMapOfferEntity(Instant.now(), offerId);
    offer.setFacility(facility);
    offering.addOffer(offer);
    offer = priceMapOfferDao.save(offer);
    return buildPriceMapOffer(offer);
  }

  private PriceMapOffer buildPriceMapOffer(FacilityPriceMapOfferEntity offer) {
    // if the offer has a price map itself, use that; otherwise use the offering's price map
    PriceMapEmbed priceMap = (offer.getPriceMap() != null ? offer.priceMap().priceMap()
        : offer.getOffering().priceMap().priceMap());
    // @formatter:off
    return PriceMapOffer.newBuilder()
        .setOfferId(ProtobufUtils.uuidForUuid(offer.getId()))
        .setWhen(ProtobufUtils.timestampForInstant(offer.getOffering().getStartDate()))
        .setPriceMap(ProtobufUtils.priceMapForPriceMapEmbed(priceMap))
        .setRoute(DerRoute.newBuilder()
            .setExchangeUid(exchangeUid)
            .setFacilityUid(offer.getFacility().getFacilityUid())
            .setSignature(generateMessageSignature(cryptoHelper, exchangeKeyPair,
                decodePublicKey(cryptoHelper, offer.getFacility().getFacilityPublicKey()),
                asList(
                    exchangeUid, 
                    offer.getFacility().getFacilityUid(), 
                    offer)))
            .build())
        .build();
    // @formatter:on
  }

  private Future<FacilityPriceMapOfferEntity> proposeOfferToFacility(UUID offeringId,
      QueuedPriceMapOffer qpmo) {
    final String facilityUid = qpmo.facilityUid();

    ManagedChannel channel = facilityChannelProvider.channelForUri(qpmo.facilityUri);
    DerFacilityServiceStub client = DerFacilityServiceGrpc.newStub(channel);

    // CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINE
    FutureStreamObserver<PriceMapOfferResponse, FacilityPriceMapOfferEntity> in = new CompletableStreamObserver<PriceMapOfferResponse, FacilityPriceMapOfferEntity>(
        qpmo.future) {

      @Override
      public void onNext(PriceMapOfferResponse r) {
        UUID offerId = qpmo.offerIds.poll();
        log.info("Received price map offer [{}] response [{}] from facility [{}]", offerId, r,
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
          if (entity.isConfirmed()) {
            getFuture().complete(entity);
            if (eventPublisher != null) {
              eventPublisher.publishEvent(new FacilityPriceMapOfferCompleted(entity));
            }
          } else {
            // a new counter-counter offer must be passed to facility
            qpmo.offerIds.add(entity.getId());
            qpmo.out.onNext(buildPriceMapOffer(entity));
          }
        } catch (RuntimeException e) {
          getFuture().completeExceptionally(e);
        }
      }

      @Override
      public void onError(Throwable t) {
        log.error("Error making offer to facility");
        super.onError(t);
      }

    };
    StreamObserver<PriceMapOffer> out = client.proposePriceMapOffer(in);
    qpmo.out = out;

    // send the initial offer to the facility
    out.onNext(qpmo.initialOffer);

    return qpmo.future.whenCompleteAsync((e, t) -> {
      if (t != null) {
        out.onError(t);
      } else {
        out.onCompleted();
      }
      channel.shutdown();
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
      // NOTE: we are blindly accepting any counter-offer without validating 

      UUID counterCounterOfferId = UUID.randomUUID();
      FacilityPriceMapOfferEntity counterCounterOffer = new FacilityPriceMapOfferEntity(
          Instant.now(), counterCounterOfferId);
      counterCounterOffer.setFacility(offer.getFacility());
      counterCounterOffer.setProposed(true);
      counterCounterOffer.setPriceMap(
          PriceMapEntity.entityForMessage(response.getCounterOffer(), UUID.randomUUID()));

      log.info("Offer [{}] to facility [{}] counter offered [{}]", offeringId,
          offer.getFacility().getFacilityUid(), response.getCounterOffer());
      counterCounterOffer = priceMapOfferDao.save(counterCounterOffer);

      PriceMapOfferingEntity offering = offeringDao.findById(offeringId).orElseThrow(
          () -> new IllegalArgumentException("Offering [" + offeringId + "] not available."));
      offering.addOffer(counterCounterOffer);
      offeringDao.save(offering);

      return counterCounterOffer;
    } else {
      offer.setProposed(true);
      offer.setAccepted(response.getAccept());
      offer.setConfirmed(true);
      log.info("Offer [{}] to facility [{}] {}", offerId, offer.getFacility().getFacilityUid(),
          response.getAccept() ? "accepted" : "declined");
      return priceMapOfferDao.save(offer);
    }
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

  /**
   * Set an event publisher to use.
   * 
   * @param eventPublisher
   *        the event publisher to set
   */
  public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

}
