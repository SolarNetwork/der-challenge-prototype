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
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.solarnetwork.esi.domain.DerRoute;
import net.solarnetwork.esi.domain.PriceMapOfferStatus;
import net.solarnetwork.esi.domain.PriceMapOfferStatusResponse;
import net.solarnetwork.esi.domain.support.ProtobufUtils;
import net.solarnetwork.esi.domain.support.SignableMessage;
import net.solarnetwork.esi.grpc.ChannelProvider;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc.DerFacilityExchangeBlockingStub;
import net.solarnetwork.esi.simple.fac.dao.PriceMapOfferEventEntityDao;
import net.solarnetwork.esi.simple.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferEventEntity;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferExecutionState;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferNotification;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferNotification.PriceMapOfferExecutionStateChanged;
import net.solarnetwork.esi.simple.fac.service.FacilityService;
import net.solarnetwork.esi.simple.fac.service.PriceMapOfferExecutionService;

/**
 * DAO based {@link PriceMapOfferExecutionService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoPriceMapOfferExecutionService implements PriceMapOfferExecutionService {

  private static final Logger log = LoggerFactory.getLogger(DaoPriceMapOfferExecutionService.class);

  private final FacilityService facilityService;
  private final PriceMapOfferEventEntityDao offerEventDao;
  private ChannelProvider exchangeChannelProvider;
  private ApplicationEventPublisher eventPublisher;

  /**
   * Constructor.
   * 
   * @param facilityService
   *        the facility service
   * @param offerEventDao
   *        the offer event DAO
   */
  public DaoPriceMapOfferExecutionService(FacilityService facilityService,
      PriceMapOfferEventEntityDao offerEventDao) {
    super();
    this.facilityService = facilityService;
    this.offerEventDao = offerEventDao;
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public CompletableFuture<?> executePriceMapOfferEvent(UUID offerId) {
    log.info("Starting execution of price map offer {}", offerId);
    PriceMapOfferEventEntity offerEvent = offerEventDao.findById(offerId).orElseThrow(
        () -> new IllegalArgumentException("Offer event " + offerId + " not available."));
    // we don't actually do anything to execute, other than mark the entity as executing
    PriceMapOfferExecutionState oldState = offerEvent.executionState();
    if (oldState == PriceMapOfferExecutionState.WAITING) {
      PriceMapOfferExecutionState newState = PriceMapOfferExecutionState.EXECUTING;
      offerEvent.setExecutionState(newState);
      offerEvent = offerEventDao.save(offerEvent);
      publishEvent(new PriceMapOfferNotification.PriceMapOfferExecutionStateChanged(offerEvent,
          oldState, newState));
    }
    CompletableFuture<PriceMapOfferEventEntity> result = new CompletableFuture<>();
    result.complete(offerEvent);
    return result;
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public CompletableFuture<?> endPriceMapOfferEvent(UUID offerId,
      PriceMapOfferExecutionState newState) {
    log.info("Finishing execution of price map offer {}", offerId);
    PriceMapOfferEventEntity offerEvent = offerEventDao.findById(offerId).orElseThrow(
        () -> new IllegalArgumentException("Offer event " + offerId + " not available."));
    // we don't actually do anything to execute, other than mark the entity as executing
    PriceMapOfferExecutionState oldState = offerEvent.executionState();
    if (oldState == PriceMapOfferExecutionState.EXECUTING) {
      offerEvent.setExecutionState(newState);
      offerEvent.setCompletedSuccessfully(newState == PriceMapOfferExecutionState.COMPLETED);
      offerEvent = offerEventDao.save(offerEvent);
      publishEvent(new PriceMapOfferNotification.PriceMapOfferExecutionStateChanged(offerEvent,
          oldState, newState));
    }
    CompletableFuture<PriceMapOfferEventEntity> result = new CompletableFuture<>();
    result.complete(offerEvent);
    return result;
  }

  /**
   * Handle a price map offer accepted event.
   * 
   * @param event
   *        the event
   */
  @Async
  @EventListener
  @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
  public void handlePriceMapOfferExecutionStateChanged(PriceMapOfferExecutionStateChanged event) {
    PriceMapOfferEventEntity entity = event.getOfferEvent();
    PriceMapOfferExecutionState executionState = event.getNewState();
    publishPriceMapOfferStatus(entity, executionState);
  }

  private void publishPriceMapOfferStatus(PriceMapOfferEventEntity entity,
      PriceMapOfferExecutionState executionState) {
    ExchangeEntity exchange = facilityService.getExchange();
    if (exchange == null) {
      return;
    }
    PriceMapOfferStatus.Status status = PriceMapOfferStatus.Status.UNKNOWN;
    switch (executionState) {
      case ABORTED:
      case COUNTERED:
      case DECLINED:
        status = PriceMapOfferStatus.Status.REJECTED;
        break;

      case WAITING:
        status = PriceMapOfferStatus.Status.ACCEPTED;
        break;

      case EXECUTING:
        status = PriceMapOfferStatus.Status.EXECUTING;
        break;

      case COMPLETED:
        status = PriceMapOfferStatus.Status.COMPLETED;
        break;

      default:
        status = PriceMapOfferStatus.Status.UNKNOWN;
        break;
    }
    log.info("Provding price map offer {} status {} to exchange {}", entity.getId(), status,
        exchange.getId());

    ByteBuffer signatureData = ByteBuffer
        .allocate(SignableMessage.uuidSignatureMessageSize() + Integer.BYTES);
    SignableMessage.addUuidSignatureMessageBytes(signatureData, entity.getId());
    signatureData.putInt(status.getNumber());

    // @formatter:off
    PriceMapOfferStatus pmoStatus = PriceMapOfferStatus.newBuilder()
        .setOfferId(ProtobufUtils.uuidForUuid(entity.getId()))
        .setStatus(status)
        .setRoute(DerRoute.newBuilder()
            .setExchangeUid(exchange.getId())
            .setFacilityUid(facilityService.getUid())
            .setSignature(generateMessageSignature(facilityService.getCryptoHelper(), 
                facilityService.getKeyPair(), exchange.publicKey(), asList(
                    exchange.getId(),
                    facilityService.getUid(),
                    signatureData)))
            .build())
        .build();
    // @formatter:on
    ManagedChannel channel = exchangeChannelProvider
        .channelForUri(URI.create(exchange.getExchangeEndpointUri()));
    try {
      DerFacilityExchangeBlockingStub client = DerFacilityExchangeGrpc.newBlockingStub(channel);
      PriceMapOfferStatusResponse response = client.providePriceMapOfferStatus(pmoStatus);

      ByteBuffer responseSignatureData = ByteBuffer
          .allocate(SignableMessage.uuidSignatureMessageSize()
              + SignableMessage.booleanSignatureMessageSize());
      SignableMessage.addUuidSignatureMessageBytes(responseSignatureData, entity.getId());
      SignableMessage.addBooleanSignatureMessageBytes(responseSignatureData,
          response.getAccepted());

      // @formatter:off
      validateMessageSignature(facilityService.getCryptoHelper(),
          response.getRoute().getSignature(), facilityService.getKeyPair(), exchange.publicKey(),
          asList(exchange.getId(),
              facilityService.getUid(),
              responseSignatureData
              ));
      // @formatter:off
      
      log.info("Successfully published price map offer {} status {} to exchange {}, update was {}",
          entity.getId(), executionState, exchange.getId(),
          response.getAccepted() ? "accepted" : "rejected");
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) {
        throw new IllegalArgumentException(e.getStatus().getDescription());
      } else {
        throw e;
      }
    } finally {
      channel.shutdown();
      try {
        channel.awaitTermination(1, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        log.debug("Timeout waiting for channel to shut down.");
      }
    }
  }

  private void publishEvent(ApplicationEvent event) {
    if (eventPublisher != null) {
      eventPublisher.publishEvent(event);
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

  /**
   * Set an event publisher to use.
   * 
   * <p>
   * <b>Note</b> consider using a transaction-aware publisher so that events are published after the
   * transactions that emit them are committed.
   * </p>
   * 
   * @param eventPublisher
   *        the event publisher to set
   */
  public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

}
