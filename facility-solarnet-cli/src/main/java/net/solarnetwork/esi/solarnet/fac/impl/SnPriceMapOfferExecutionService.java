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

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static net.solarnetwork.esi.util.CryptoUtils.generateMessageSignature;
import static net.solarnetwork.esi.util.CryptoUtils.validateMessageSignature;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
import net.solarnetwork.esi.solarnet.fac.dao.PriceMapOfferEventEntityDao;
import net.solarnetwork.esi.solarnet.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityPriceMap;
import net.solarnetwork.esi.solarnet.fac.domain.PriceMapOfferEventEntity;
import net.solarnetwork.esi.solarnet.fac.domain.PriceMapOfferExecutionState;
import net.solarnetwork.esi.solarnet.fac.domain.PriceMapOfferNotification.PriceMapOfferExecutionStateChanged;
import net.solarnetwork.esi.solarnet.fac.service.FacilityService;
import net.solarnetwork.esi.solarnet.fac.service.PriceMapOfferExecutionService;
import net.solarnetwork.util.StringUtils;
import net.solarnetwork.web.security.AuthorizationCredentialsProvider;

/**
 * SolarNetwork based {@link PriceMapOfferExecutionService}.
 * 
 * @author matt
 * @version 1.0
 */
public class SnPriceMapOfferExecutionService extends BaseSolarNetworkClientService
    implements PriceMapOfferExecutionService {

  /** The default instruction poll interval, in milliseconds. */
  public static final long DEFAULT_INSTRUCTION_POLL_MS = 2000L;

  private static final Logger log = LoggerFactory.getLogger(SnPriceMapOfferExecutionService.class);

  private final TaskScheduler taskScheduler;
  private TransactionTemplate txTemplate;
  private final FacilityService facilityService;
  private final PriceMapOfferEventEntityDao offerEventDao;
  private ChannelProvider exchangeChannelProvider;
  private ApplicationEventPublisher eventPublisher;
  private long instructionPollMs = DEFAULT_INSTRUCTION_POLL_MS;

  /**
   * Constructor.
   * 
   * @param taskScheduler
   *        the task scheduler
   * @param facilityService
   *        the facility service
   * @param offerEventDao
   *        the offer event DAO
   * @param credentialsProvider
   *        the credentials provider to use
   */
  public SnPriceMapOfferExecutionService(TaskScheduler taskScheduler,
      FacilityService facilityService, PriceMapOfferEventEntityDao offerEventDao,
      AuthorizationCredentialsProvider credentialsProvider) {
    this(taskScheduler, facilityService, offerEventDao,
        WebUtils.setupSolarNetworkClient(new RestTemplate(), credentialsProvider));
  }

  /**
   * Constructor.
   * 
   * @param taskScheduler
   *        the task scheduler
   * @param facilityService
   *        the facility service
   * @param offerEventDao
   *        the offer event DAO
   * @param restTemplate
   *        the RestTemplate to use; this must already be configured to support any necessary
   *        authentication for working with the SolarNetwork API
   */
  public SnPriceMapOfferExecutionService(TaskScheduler taskScheduler,
      FacilityService facilityService, PriceMapOfferEventEntityDao offerEventDao,
      RestTemplate restTemplate) {
    super(restTemplate);
    this.taskScheduler = taskScheduler;
    this.facilityService = facilityService;
    this.offerEventDao = offerEventDao;
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public CompletableFuture<?> executePriceMapOfferEvent(UUID offerId) {
    log.info("Starting execution of price map offer {}", offerId);
    PriceMapOfferEventEntity offerEvent = offerEventDao.findById(offerId).orElseThrow(
        () -> new IllegalArgumentException("Offer event " + offerId + " not available."));
    final String facPriceMapId = offerEvent.getFacilityPriceMapId();
    if (facPriceMapId == null) {
      throw new IllegalArgumentException("The facility price map ID must be provided.");
    }

    final CompletableFuture<PriceMapOfferEventEntity> result = new CompletableFuture<>();
    PriceMapOfferExecutionState oldState = offerEvent.executionState();
    if (oldState == PriceMapOfferExecutionState.WAITING) {
      FacilityPriceMap facPriceMap = StreamSupport
          .stream(facilityService.getPriceMaps().spliterator(), false)
          .filter(p -> facPriceMapId.equals(p.getId())).findFirst().get();

      // update state to Executing
      offerEvent.setExecutionState(PriceMapOfferExecutionState.EXECUTING);
      offerEvent = offerEventDao.save(offerEvent);
      PriceMapOfferExecutionState newState = offerEvent.getExecutionState();
      publishEvent(new PriceMapOfferExecutionStateChanged(offerEvent, oldState, newState));
      oldState = PriceMapOfferExecutionState.EXECUTING;

      // now execute by issuing the appropriate instruction to SolarNetwork
      if (offerEvent.priceMap().getPowerComponents().isRealPowerNegative()) {
        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>(2);
        String controlId = facPriceMap.getControlId();
        if (controlId == null || controlId.trim().isEmpty()) {
          throw new IllegalArgumentException(
              "The facility price map has no control ID configured.");
        }
        Long nodeId = facPriceMap.getNodeId();
        if (nodeId == null) {
          throw new IllegalArgumentException("The facility price map has no node ID configured.");
        }
        parameters.add(controlId, -offerEvent.priceMap().getPowerComponents().getRealPower());
        try {
          JsonNode statuses = enqueueInstruction("ShedLoad", singleton(facPriceMap.getNodeId()),
              parameters);
          PriceMapOfferEventEntity resultEntity = checkStatus(offerId, statuses);
          if (resultEntity == null) {
            Set<Long> instructionIds = StreamSupport.stream(statuses.spliterator(), false)
                .map(j -> j.path("id").longValue()).filter(l -> l > 0).collect(toSet());
            taskScheduler.schedule(new PriceMapInstructionPollTask(offerId, result, instructionIds),
                Instant.now().plusMillis(instructionPollMs));
          }
        } catch (JsonProcessingException | URISyntaxException | IllegalArgumentException e) {
          log.error("Error enqueuing ShedLoad node instruction for node {} parameters {}: {}",
              facPriceMap.getNodeId(), parameters, e.toString());
          newState = PriceMapOfferExecutionState.ABORTED;
          offerEvent.setMessage("Error enqueuing node instruction: " + e.toString());
        }
      } else {
        // TODO: we don't support supply load yet
        newState = PriceMapOfferExecutionState.ABORTED;
        offerEvent.setMessage("Positive power (supply) not supported yet.");
      }
      if (newState != oldState) {
        offerEvent.setExecutionState(newState);
        offerEvent = offerEventDao.save(offerEvent);
        publishEvent(new PriceMapOfferExecutionStateChanged(offerEvent, oldState, newState));
      }
    }
    result.complete(offerEvent);
    return result;
  }

  private class PriceMapInstructionPollTask implements Runnable {

    private final UUID offerId;
    private final CompletableFuture<PriceMapOfferEventEntity> result;
    private final Set<Long> instructionIds;

    private PriceMapInstructionPollTask(UUID offerId,
        CompletableFuture<PriceMapOfferEventEntity> result, Set<Long> instructionIds) {
      super();
      this.offerId = offerId;
      this.result = result;
      this.instructionIds = instructionIds;
    }

    @Override
    public void run() {
      try {
        JsonNode statuses = instructionStatus(instructionIds);
        TransactionTemplate tt = txTemplate();
        PriceMapOfferEventEntity entity = null;
        if (tt != null) {
          entity = tt.execute(new TransactionCallback<PriceMapOfferEventEntity>() {

            @Override
            public PriceMapOfferEventEntity doInTransaction(TransactionStatus status) {
              return checkStatus(offerId, statuses);
            }
          });
        } else {
          entity = checkStatus(offerId, statuses);
        }
        if (entity == null) {
          // enqueue task again
          taskScheduler.schedule(this, Instant.now().plusMillis(instructionPollMs));
        } else {
          result.complete(entity);
        }
      } catch (JsonProcessingException | URISyntaxException | IllegalArgumentException e) {
        log.error("Error viewing node instructions {} status: {}", instructionIds, e.toString());
        updateOfferState(offerId, PriceMapOfferExecutionState.ABORTED,
            "Error checking instruction status: " + e.toString());
        result.completeExceptionally(e);
      }
    }
  }

  private PriceMapOfferEventEntity checkStatus(UUID offerId, JsonNode statuses) {
    // if ANY instruction goes to Declined, then entire status is ABORTED;
    // if ALL instructions go to Completed, then entire status is COMPLETED
    PriceMapOfferExecutionState newState = PriceMapOfferExecutionState.EXECUTING;
    String message = null;
    for (JsonNode instr : statuses) {
      Long instructionId = (instr != null ? instr.path("id").longValue() : null);
      if (instructionId == null) {
        newState = PriceMapOfferExecutionState.ABORTED;
        message = "Error viewing node instruction status: no instruction ID available.";
        break;
      }
      String state = instr.path("state").asText();
      if (state.equals("Declined")) {
        newState = PriceMapOfferExecutionState.ABORTED;
        message = "Node declined instruction.";
        break;
      } else if (state.equals("Completed")) {
        newState = PriceMapOfferExecutionState.COMPLETED;
      } else {
        newState = PriceMapOfferExecutionState.EXECUTING;
      }
    }
    if (newState == PriceMapOfferExecutionState.EXECUTING) {
      return null;
    }
    return updateOfferState(offerId, newState, message);
  }

  private PriceMapOfferEventEntity updateOfferState(UUID offerId,
      PriceMapOfferExecutionState newState, String message) {
    PriceMapOfferEventEntity offerEvent = offerEventDao.findById(offerId).orElseThrow(
        () -> new IllegalArgumentException("Offer event " + offerId + " not available."));
    offerEvent.setExecutionState(newState);
    if (message != null) {
      offerEvent.setMessage(message);
    }
    offerEvent = offerEventDao.save(offerEvent);
    publishEvent(new PriceMapOfferExecutionStateChanged(offerEvent,
        PriceMapOfferExecutionState.EXECUTING, newState));
    return offerEvent;
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
      publishEvent(new PriceMapOfferExecutionStateChanged(offerEvent, oldState, newState));
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

  private JsonNode enqueueInstruction(String topic, Set<Long> nodeIds,
      MultiValueMap<String, Object> parameters) throws URISyntaxException, JsonProcessingException {
    if (topic == null || topic.trim().isEmpty()) {
      throw new IllegalArgumentException("An instruction topic is required.");
    }
    if (nodeIds == null || nodeIds.isEmpty()) {
      throw new IllegalArgumentException(
          "At least one node ID is required for enqueuing an instruction.");
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>(4);
    params.add("topic", topic);
    params.add("nodeIds", StringUtils.commaDelimitedStringFromCollection(nodeIds));
    int idx = 0;
    for (Map.Entry<String, List<Object>> me : parameters.entrySet()) {
      for (Object v : me.getValue()) {
        params.add("parameters[" + idx + "].name", me.getKey());
        params.add("parameters[" + idx + "].value", v.toString());
        idx++;
      }
    }

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

    UriComponentsBuilder uriBuilder = UriComponentsBuilder
        .fromHttpUrl(apiUrl("/solaruser/api/v1/sec/instr/add"));
    String url = uriBuilder.toUriString();
    log.info("Enqueuing SolarNetwork instruction {} for nodes {} with parameters: {}", topic,
        nodeIds, params);
    try {
      ObjectNode json = getRestOperations().postForObject(new URI(url), request, ObjectNode.class);
      if (json != null && json.findPath("success").booleanValue()) {
        JsonNode data = json.path("data");
        if (data.isArray() && data.size() > 0) {
          return data;
        }
      }
      return MissingNode.getInstance();
    } catch (HttpClientErrorException.Unauthorized e) {
      throw new IllegalArgumentException("Access denied by SolarNetwork on POST request to "
          + uriBuilder.toUriString() + "; check the configured SolarNetwork credentials");
    }
  }

  private JsonNode instructionStatus(Set<Long> instructionIds)
      throws URISyntaxException, JsonProcessingException {
    UriComponentsBuilder uriBuilder = UriComponentsBuilder
        .fromHttpUrl(apiUrl("/solaruser/api/v1/sec/instr/view"))
        .queryParam("ids", StringUtils.commaDelimitedStringFromCollection(instructionIds));
    log.info("Checking SolarNetwork instruction statuses: {}", instructionIds);
    try {
      ObjectNode json = getRestOperations().getForObject(new URI(uriBuilder.toUriString()),
          ObjectNode.class);
      if (json != null && json.findPath("success").booleanValue()) {
        JsonNode data = json.path("data");
        if (data.isArray() && data.size() > 0) {
          return data;
        }
      }
      return MissingNode.getInstance();
    } catch (HttpClientErrorException.Unauthorized e) {
      throw new IllegalArgumentException("Access denied by SolarNetwork on GET request to "
          + uriBuilder.toUriString() + "; check the configured SolarNetwork credentials");
    }
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

  private TransactionTemplate txTemplate() {
    return txTemplate;
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

  
  /**
   * Set the instruction poll interval, in milliseconds.
   * 
   * @param ms the poll interval to set; defaults to {@link #DEFAULT_INSTRUCTION_POLL_MS}
   */
  public void setInstructionPollMs(long ms) {
    this.instructionPollMs = ms;
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
  
}
