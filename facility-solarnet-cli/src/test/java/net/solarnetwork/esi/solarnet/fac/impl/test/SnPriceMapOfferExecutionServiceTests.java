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

package net.solarnetwork.esi.solarnet.fac.impl.test;

import static java.util.Collections.singleton;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
import net.solarnetwork.esi.solarnet.fac.dao.PriceMapOfferEventEntityDao;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityPriceMap;
import net.solarnetwork.esi.solarnet.fac.domain.PriceMapOfferEventEntity;
import net.solarnetwork.esi.solarnet.fac.domain.PriceMapOfferExecutionState;
import net.solarnetwork.esi.solarnet.fac.domain.PriceMapOfferNotification.PriceMapOfferExecutionStateChanged;
import net.solarnetwork.esi.solarnet.fac.impl.SnPriceMapOfferExecutionService;
import net.solarnetwork.esi.solarnet.fac.impl.WebUtils;
import net.solarnetwork.esi.solarnet.fac.service.FacilityService;
import net.solarnetwork.web.security.AuthorizationCredentialsProvider;
import net.solarnetwork.web.support.StaticAuthorizationCredentialsProvider;

/**
 * Test cases for the {@link SnPriceMapOfferExecutionService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SnPriceMapOfferExecutionServiceTests {

  private static final String TEST_BASE_URL = "http://localhost";

  private RestTemplate restTemplate;
  private AuthorizationCredentialsProvider credProvider;
  private TaskScheduler taskScheduler;
  private FacilityService facilityService;
  private PriceMapOfferEventEntityDao priceMapOfferDao;
  private ApplicationEventPublisher eventPublisher;
  private SnPriceMapOfferExecutionService service;

  private MockRestServiceServer server;

  @Before
  public void setup() {
    credProvider = new StaticAuthorizationCredentialsProvider(randomUUID().toString(),
        randomUUID().toString());
    restTemplate = WebUtils.setupSolarNetworkClient(new RestTemplate(), credProvider);
    taskScheduler = Mockito.mock(TaskScheduler.class);
    facilityService = Mockito.mock(FacilityService.class);
    priceMapOfferDao = Mockito.mock(PriceMapOfferEventEntityDao.class);
    eventPublisher = mock(ApplicationEventPublisher.class);

    service = new SnPriceMapOfferExecutionService(taskScheduler, facilityService, priceMapOfferDao,
        restTemplate);
    service.setApiBaseUrl(TEST_BASE_URL);
    service.setEventPublisher(eventPublisher);

    server = MockRestServiceServer.bindTo(restTemplate).build();
  }

  @Test
  public void executeOffer() throws Exception {
    // GIVEN
    final UUID offerId = UUID.randomUUID();
    final PriceMapEmbed offerPriceMap = new PriceMapEmbed();
    offerPriceMap.powerComponents().setRealPower(-1000L); // shed 1kW

    final FacilityPriceMap facPriceMap = new FacilityPriceMap(UUID.randomUUID().toString(),
        offerPriceMap);
    facPriceMap.setControlId("/load/1");
    facPriceMap.setNodeId(123L);
    given(facilityService.getPriceMaps()).willReturn(singleton(facPriceMap));

    final PriceMapOfferEventEntity offerEvent = new PriceMapOfferEventEntity(Instant.now(), offerId,
        Instant.now(), offerPriceMap);
    offerEvent.setFacilityPriceMapId(facPriceMap.getId());
    offerEvent.setExecutionState(PriceMapOfferExecutionState.WAITING);
    given(priceMapOfferDao.findById(offerId)).willReturn(Optional.of(offerEvent));

    // invoke /instr/add API on SolarNetwork, which returns Completed state
    Resource addResp = new ClassPathResource("instr-add-resp-01.json", getClass());
    HttpHeaders addRespHeaders = new HttpHeaders();
    addRespHeaders.setContentLength(addResp.contentLength());
    MultiValueMap<String, String> addRespExpectedContent = new LinkedMultiValueMap<>(8);
    addRespExpectedContent.add("topic", "ShedLoad");
    addRespExpectedContent.add("nodeIds", facPriceMap.getNodeId().toString());
    addRespExpectedContent.add("parameters[0].name", "/load/1");
    addRespExpectedContent.add("parameters[0].value", "1000");
    // @formatter:off
    server.expect(requestTo(startsWith(TEST_BASE_URL + "/solaruser/api/v1/sec/instr/add")))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.HOST, "localhost"))
        .andExpect(header(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate"))
        .andExpect(header(HttpHeaders.AUTHORIZATION, 
            startsWith("SNWS2 Credential=" + credProvider.getAuthorizationId() 
                + ",SignedHeaders=content-type;date;host,Signature=")))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(content().formData(addRespExpectedContent))
        .andRespond(withSuccess(addResp, APPLICATION_JSON_UTF8).headers(addRespHeaders));
    // @formatter:on

    given(priceMapOfferDao.save(offerEvent)).willReturn(offerEvent);

    // WHEN
    CompletableFuture<?> future = service.executePriceMapOfferEvent(offerId);

    // THEN
    assertThat("Future returned", future, notNullValue());

    Object result = future.get();
    assertThat("Future result is offer entity", result, sameInstance(offerEvent));
    assertThat("Offet state completed", offerEvent.getExecutionState(),
        equalTo(PriceMapOfferExecutionState.EXECUTING));

    // verify state notifications WAITING -> EXECUTING -> COMPLETED
    ArgumentCaptor<PriceMapOfferExecutionStateChanged> eventCaptor = ArgumentCaptor
        .forClass(PriceMapOfferExecutionStateChanged.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

    List<PriceMapOfferExecutionStateChanged> evts = eventCaptor.getAllValues();
    PriceMapOfferExecutionStateChanged evt1 = evts.get(0);
    assertThat("Event 1 source same as persisted", evt1.getSource(), sameInstance(offerEvent));
    assertThat("Event 1 entity same as persisted", evt1.getOfferEvent(), sameInstance(offerEvent));
    assertThat("Event 1 old state", evt1.getOldState(),
        equalTo(PriceMapOfferExecutionState.WAITING));
    assertThat("Event 1 new state", evt1.getNewState(),
        equalTo(PriceMapOfferExecutionState.EXECUTING));

    // verify no task needed
    verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
  }

  @Test
  public void executeOfferViaQueuedStateDelay() throws Exception {
    // GIVEN
    final UUID offerId = UUID.randomUUID();
    final PriceMapEmbed offerPriceMap = new PriceMapEmbed();
    offerPriceMap.powerComponents().setRealPower(-1000L); // shed 1kW

    final FacilityPriceMap facPriceMap = new FacilityPriceMap(UUID.randomUUID().toString(),
        offerPriceMap);
    facPriceMap.setControlId("/load/1");
    facPriceMap.setNodeId(123L);
    given(facilityService.getPriceMaps()).willReturn(singleton(facPriceMap));

    final PriceMapOfferEventEntity offerEvent = new PriceMapOfferEventEntity(Instant.now(), offerId,
        Instant.now(), offerPriceMap);
    offerEvent.setFacilityPriceMapId(facPriceMap.getId());
    offerEvent.setExecutionState(PriceMapOfferExecutionState.WAITING);
    given(priceMapOfferDao.findById(offerId)).willReturn(Optional.of(offerEvent));

    // invoke /instr/add API on SolarNetwork, which returns Queued state
    Resource addResp = new ClassPathResource("instr-add-resp-02.json", getClass());
    HttpHeaders addRespHeaders = new HttpHeaders();
    addRespHeaders.setContentLength(addResp.contentLength());
    MultiValueMap<String, String> addRespExpectedContent = new LinkedMultiValueMap<>(8);
    addRespExpectedContent.add("topic", "ShedLoad");
    addRespExpectedContent.add("nodeIds", facPriceMap.getNodeId().toString());
    addRespExpectedContent.add("parameters[0].name", "/load/1");
    addRespExpectedContent.add("parameters[0].value", "1000");
    // @formatter:off
    server.expect(requestTo(startsWith(TEST_BASE_URL + "/solaruser/api/v1/sec/instr/add")))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.HOST, "localhost"))
        .andExpect(header(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate"))
        .andExpect(header(HttpHeaders.AUTHORIZATION, 
            startsWith("SNWS2 Credential=" + credProvider.getAuthorizationId() 
                + ",SignedHeaders=content-type;date;host,Signature=")))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(content().formData(addRespExpectedContent))
        .andRespond(withSuccess(addResp, APPLICATION_JSON_UTF8).headers(addRespHeaders));
    // @formatter:on

    given(priceMapOfferDao.save(offerEvent)).willReturn(offerEvent);

    ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
    given(taskScheduler.schedule(taskCaptor.capture(), any(Instant.class))).willReturn(null);

    // invoke /instr/view API on SolarNetwork, which returns Completed state
    Resource viewResp = new ClassPathResource("instr-view-resp-01.json", getClass());
    HttpHeaders viewRespHeaders = new HttpHeaders();
    viewRespHeaders.setContentLength(viewResp.contentLength());
    MultiValueMap<String, String> viewRespExpectedContent = new LinkedMultiValueMap<>(8);
    viewRespExpectedContent.add("ids", "123456799");
    // @formatter:off
    server.expect(requestTo(startsWith(TEST_BASE_URL + "/solaruser/api/v1/sec/instr/view")))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.HOST, "localhost"))
        .andExpect(header(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate"))
        .andExpect(header(HttpHeaders.AUTHORIZATION, 
            startsWith("SNWS2 Credential=" + credProvider.getAuthorizationId() 
                + ",SignedHeaders=date;host,Signature=")))
        .andRespond(withSuccess(viewResp, APPLICATION_JSON_UTF8).headers(viewRespHeaders));
    // @formatter:on

    // WHEN
    CompletableFuture<?> future = service.executePriceMapOfferEvent(offerId);

    // THEN
    assertThat("Future returned", future, notNullValue());

    assertThat("Instruction state poll task scheduled", taskCaptor.getValue(), notNullValue());

    // run the task now, so we can wait for future result
    taskCaptor.getValue().run();

    Object result = future.get();
    assertThat("Future result is offer entity", result, sameInstance(offerEvent));
    assertThat("Offet state completed", offerEvent.getExecutionState(),
        equalTo(PriceMapOfferExecutionState.EXECUTING));

    // verify state notifications WAITING -> EXECUTING
    ArgumentCaptor<PriceMapOfferExecutionStateChanged> eventCaptor = ArgumentCaptor
        .forClass(PriceMapOfferExecutionStateChanged.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

    List<PriceMapOfferExecutionStateChanged> evts = eventCaptor.getAllValues();
    PriceMapOfferExecutionStateChanged evt1 = evts.get(0);
    assertThat("Event 1 source same as persisted", evt1.getSource(), sameInstance(offerEvent));
    assertThat("Event 1 entity same as persisted", evt1.getOfferEvent(), sameInstance(offerEvent));
    assertThat("Event 1 old state", evt1.getOldState(),
        equalTo(PriceMapOfferExecutionState.WAITING));
    assertThat("Event 1 new state", evt1.getNewState(),
        equalTo(PriceMapOfferExecutionState.EXECUTING));
  }

  @Test
  public void executeOfferViaQueuedStateDelayDeclined() throws Exception {
    // GIVEN
    final UUID offerId = UUID.randomUUID();
    final PriceMapEmbed offerPriceMap = new PriceMapEmbed();
    offerPriceMap.powerComponents().setRealPower(-1000L); // shed 1kW

    final FacilityPriceMap facPriceMap = new FacilityPriceMap(UUID.randomUUID().toString(),
        offerPriceMap);
    facPriceMap.setControlId("/load/1");
    facPriceMap.setNodeId(123L);
    given(facilityService.getPriceMaps()).willReturn(singleton(facPriceMap));

    final PriceMapOfferEventEntity offerEvent = new PriceMapOfferEventEntity(Instant.now(), offerId,
        Instant.now(), offerPriceMap);
    offerEvent.setFacilityPriceMapId(facPriceMap.getId());
    offerEvent.setExecutionState(PriceMapOfferExecutionState.WAITING);
    given(priceMapOfferDao.findById(offerId)).willReturn(Optional.of(offerEvent));

    // invoke /instr/add API on SolarNetwork, which returns Queued state
    Resource addResp = new ClassPathResource("instr-add-resp-02.json", getClass());
    HttpHeaders addRespHeaders = new HttpHeaders();
    addRespHeaders.setContentLength(addResp.contentLength());
    MultiValueMap<String, String> addRespExpectedContent = new LinkedMultiValueMap<>(8);
    addRespExpectedContent.add("topic", "ShedLoad");
    addRespExpectedContent.add("nodeIds", facPriceMap.getNodeId().toString());
    addRespExpectedContent.add("parameters[0].name", "/load/1");
    addRespExpectedContent.add("parameters[0].value", "1000");
    // @formatter:off
    server.expect(requestTo(startsWith(TEST_BASE_URL + "/solaruser/api/v1/sec/instr/add")))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.HOST, "localhost"))
        .andExpect(header(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate"))
        .andExpect(header(HttpHeaders.AUTHORIZATION, 
            startsWith("SNWS2 Credential=" + credProvider.getAuthorizationId() 
                + ",SignedHeaders=content-type;date;host,Signature=")))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(content().formData(addRespExpectedContent))
        .andRespond(withSuccess(addResp, APPLICATION_JSON_UTF8).headers(addRespHeaders));
    // @formatter:on

    given(priceMapOfferDao.save(offerEvent)).willReturn(offerEvent);

    ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
    given(taskScheduler.schedule(taskCaptor.capture(), any(Instant.class))).willReturn(null);

    // invoke /instr/view API on SolarNetwork, which returns Completed state
    Resource viewResp = new ClassPathResource("instr-view-resp-02.json", getClass());
    HttpHeaders viewRespHeaders = new HttpHeaders();
    viewRespHeaders.setContentLength(viewResp.contentLength());
    MultiValueMap<String, String> viewRespExpectedContent = new LinkedMultiValueMap<>(8);
    viewRespExpectedContent.add("ids", "123456799");
    // @formatter:off
    server.expect(requestTo(startsWith(TEST_BASE_URL + "/solaruser/api/v1/sec/instr/view")))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.HOST, "localhost"))
        .andExpect(header(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate"))
        .andExpect(header(HttpHeaders.AUTHORIZATION, 
            startsWith("SNWS2 Credential=" + credProvider.getAuthorizationId() 
                + ",SignedHeaders=date;host,Signature=")))
        .andRespond(withSuccess(viewResp, APPLICATION_JSON_UTF8).headers(viewRespHeaders));
    // @formatter:on

    // WHEN
    CompletableFuture<?> future = service.executePriceMapOfferEvent(offerId);

    // THEN
    assertThat("Future returned", future, notNullValue());

    assertThat("Instruction state poll task scheduled", taskCaptor.getValue(), notNullValue());

    // run the task now, so we can wait for future result
    taskCaptor.getValue().run();

    Object result = future.get();
    assertThat("Future result is offer entity", result, sameInstance(offerEvent));
    assertThat("Offet state completed", offerEvent.getExecutionState(),
        equalTo(PriceMapOfferExecutionState.ABORTED));

    // verify state notifications WAITING -> EXECUTING -> ABORTED
    ArgumentCaptor<PriceMapOfferExecutionStateChanged> eventCaptor = ArgumentCaptor
        .forClass(PriceMapOfferExecutionStateChanged.class);
    verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

    List<PriceMapOfferExecutionStateChanged> evts = eventCaptor.getAllValues();
    PriceMapOfferExecutionStateChanged evt1 = evts.get(0);
    assertThat("Event 1 source same as persisted", evt1.getSource(), sameInstance(offerEvent));
    assertThat("Event 1 entity same as persisted", evt1.getOfferEvent(), sameInstance(offerEvent));
    assertThat("Event 1 old state", evt1.getOldState(),
        equalTo(PriceMapOfferExecutionState.WAITING));
    assertThat("Event 1 new state", evt1.getNewState(),
        equalTo(PriceMapOfferExecutionState.EXECUTING));

    PriceMapOfferExecutionStateChanged evt2 = evts.get(1);
    assertThat("Event 2 source same as persisted", evt2.getSource(), sameInstance(offerEvent));
    assertThat("Event 2 entity same as persisted", evt2.getOfferEvent(), sameInstance(offerEvent));
    assertThat("Event 2 old state", evt2.getOldState(),
        equalTo(PriceMapOfferExecutionState.EXECUTING));
    assertThat("Event 2 new state", evt2.getNewState(),
        equalTo(PriceMapOfferExecutionState.ABORTED));
  }

  @Test
  public void endOffer() throws Exception {
    // GIVEN
    final UUID offerId = UUID.randomUUID();
    final PriceMapEmbed offerPriceMap = new PriceMapEmbed();
    offerPriceMap.powerComponents().setRealPower(-1000L); // shed 1kW

    final FacilityPriceMap facPriceMap = new FacilityPriceMap(UUID.randomUUID().toString(),
        offerPriceMap);
    facPriceMap.setControlId("/load/1");
    facPriceMap.setNodeId(123L);
    given(facilityService.getPriceMaps()).willReturn(singleton(facPriceMap));

    final PriceMapOfferEventEntity offerEvent = new PriceMapOfferEventEntity(Instant.now(), offerId,
        Instant.now(), offerPriceMap);
    offerEvent.setFacilityPriceMapId(facPriceMap.getId());
    offerEvent.setExecutionState(PriceMapOfferExecutionState.EXECUTING);
    given(priceMapOfferDao.findById(offerId)).willReturn(Optional.of(offerEvent));

    // invoke /instr/add API on SolarNetwork, which returns Completed state
    Resource addResp = new ClassPathResource("instr-add-resp-03.json", getClass());
    HttpHeaders addRespHeaders = new HttpHeaders();
    addRespHeaders.setContentLength(addResp.contentLength());
    MultiValueMap<String, String> addRespExpectedContent = new LinkedMultiValueMap<>(8);
    addRespExpectedContent.add("topic", "ShedLoad");
    addRespExpectedContent.add("nodeIds", facPriceMap.getNodeId().toString());
    addRespExpectedContent.add("parameters[0].name", "/load/1");
    addRespExpectedContent.add("parameters[0].value", "0");
    // @formatter:off
    server.expect(requestTo(startsWith(TEST_BASE_URL + "/solaruser/api/v1/sec/instr/add")))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.HOST, "localhost"))
        .andExpect(header(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate"))
        .andExpect(header(HttpHeaders.AUTHORIZATION, 
            startsWith("SNWS2 Credential=" + credProvider.getAuthorizationId() 
                + ",SignedHeaders=content-type;date;host,Signature=")))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(content().formData(addRespExpectedContent))
        .andRespond(withSuccess(addResp, APPLICATION_JSON_UTF8).headers(addRespHeaders));
    // @formatter:on

    given(priceMapOfferDao.save(offerEvent)).willReturn(offerEvent);

    // WHEN
    CompletableFuture<?> future = service.endPriceMapOfferEvent(offerId,
        PriceMapOfferExecutionState.COMPLETED);

    // THEN
    assertThat("Future returned", future, notNullValue());

    Object result = future.get();
    assertThat("Future result is offer entity", result, sameInstance(offerEvent));
    assertThat("Offet state completed", offerEvent.getExecutionState(),
        equalTo(PriceMapOfferExecutionState.COMPLETED));

    // verify state notifications WAITING -> EXECUTING -> COMPLETED
    ArgumentCaptor<PriceMapOfferExecutionStateChanged> eventCaptor = ArgumentCaptor
        .forClass(PriceMapOfferExecutionStateChanged.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

    List<PriceMapOfferExecutionStateChanged> evts = eventCaptor.getAllValues();
    PriceMapOfferExecutionStateChanged evt1 = evts.get(0);
    assertThat("Event 1 source same as persisted", evt1.getSource(), sameInstance(offerEvent));
    assertThat("Event 1 entity same as persisted", evt1.getOfferEvent(), sameInstance(offerEvent));
    assertThat("Event 1 old state", evt1.getOldState(),
        equalTo(PriceMapOfferExecutionState.EXECUTING));
    assertThat("Event 1 new state", evt1.getNewState(),
        equalTo(PriceMapOfferExecutionState.COMPLETED));

    // verify no task needed
    verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
  }

  @Test
  public void endOfferViaQueuedStateDelay() throws Exception {
    // GIVEN
    final UUID offerId = UUID.randomUUID();
    final PriceMapEmbed offerPriceMap = new PriceMapEmbed();
    offerPriceMap.powerComponents().setRealPower(-1000L); // shed 1kW

    final FacilityPriceMap facPriceMap = new FacilityPriceMap(UUID.randomUUID().toString(),
        offerPriceMap);
    facPriceMap.setControlId("/load/1");
    facPriceMap.setNodeId(123L);
    given(facilityService.getPriceMaps()).willReturn(singleton(facPriceMap));

    final PriceMapOfferEventEntity offerEvent = new PriceMapOfferEventEntity(Instant.now(), offerId,
        Instant.now(), offerPriceMap);
    offerEvent.setFacilityPriceMapId(facPriceMap.getId());
    offerEvent.setExecutionState(PriceMapOfferExecutionState.EXECUTING);
    given(priceMapOfferDao.findById(offerId)).willReturn(Optional.of(offerEvent));

    // invoke /instr/add API on SolarNetwork, which returns Queued state
    Resource addResp = new ClassPathResource("instr-add-resp-04.json", getClass());
    HttpHeaders addRespHeaders = new HttpHeaders();
    addRespHeaders.setContentLength(addResp.contentLength());
    MultiValueMap<String, String> addRespExpectedContent = new LinkedMultiValueMap<>(8);
    addRespExpectedContent.add("topic", "ShedLoad");
    addRespExpectedContent.add("nodeIds", facPriceMap.getNodeId().toString());
    addRespExpectedContent.add("parameters[0].name", "/load/1");
    addRespExpectedContent.add("parameters[0].value", "0");
    // @formatter:off
    server.expect(requestTo(startsWith(TEST_BASE_URL + "/solaruser/api/v1/sec/instr/add")))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.HOST, "localhost"))
        .andExpect(header(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate"))
        .andExpect(header(HttpHeaders.AUTHORIZATION, 
            startsWith("SNWS2 Credential=" + credProvider.getAuthorizationId() 
                + ",SignedHeaders=content-type;date;host,Signature=")))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(content().formData(addRespExpectedContent))
        .andRespond(withSuccess(addResp, APPLICATION_JSON_UTF8).headers(addRespHeaders));
    // @formatter:on

    given(priceMapOfferDao.save(offerEvent)).willReturn(offerEvent);

    ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
    given(taskScheduler.schedule(taskCaptor.capture(), any(Instant.class))).willReturn(null);

    // invoke /instr/view API on SolarNetwork, which returns Completed state
    Resource viewResp = new ClassPathResource("instr-view-resp-03.json", getClass());
    HttpHeaders viewRespHeaders = new HttpHeaders();
    viewRespHeaders.setContentLength(viewResp.contentLength());
    MultiValueMap<String, String> viewRespExpectedContent = new LinkedMultiValueMap<>(8);
    viewRespExpectedContent.add("ids", "123456889");
    // @formatter:off
    server.expect(requestTo(startsWith(TEST_BASE_URL + "/solaruser/api/v1/sec/instr/view")))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.HOST, "localhost"))
        .andExpect(header(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate"))
        .andExpect(header(HttpHeaders.AUTHORIZATION, 
            startsWith("SNWS2 Credential=" + credProvider.getAuthorizationId() 
                + ",SignedHeaders=date;host,Signature=")))
        .andRespond(withSuccess(viewResp, APPLICATION_JSON_UTF8).headers(viewRespHeaders));
    // @formatter:on

    // WHEN
    CompletableFuture<?> future = service.endPriceMapOfferEvent(offerId,
        PriceMapOfferExecutionState.COMPLETED);

    // THEN
    assertThat("Future returned", future, notNullValue());

    assertThat("Instruction state poll task scheduled", taskCaptor.getValue(), notNullValue());

    // run the task now, so we can wait for future result
    taskCaptor.getValue().run();

    Object result = future.get();
    assertThat("Future result is offer entity", result, sameInstance(offerEvent));
    assertThat("Offet state completed", offerEvent.getExecutionState(),
        equalTo(PriceMapOfferExecutionState.COMPLETED));

    // verify state notifications WAITING -> EXECUTING
    ArgumentCaptor<PriceMapOfferExecutionStateChanged> eventCaptor = ArgumentCaptor
        .forClass(PriceMapOfferExecutionStateChanged.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

    List<PriceMapOfferExecutionStateChanged> evts = eventCaptor.getAllValues();
    PriceMapOfferExecutionStateChanged evt1 = evts.get(0);
    assertThat("Event 1 source same as persisted", evt1.getSource(), sameInstance(offerEvent));
    assertThat("Event 1 entity same as persisted", evt1.getOfferEvent(), sameInstance(offerEvent));
    assertThat("Event 1 old state", evt1.getOldState(),
        equalTo(PriceMapOfferExecutionState.EXECUTING));
    assertThat("Event 1 new state", evt1.getNewState(),
        equalTo(PriceMapOfferExecutionState.COMPLETED));
  }

}
