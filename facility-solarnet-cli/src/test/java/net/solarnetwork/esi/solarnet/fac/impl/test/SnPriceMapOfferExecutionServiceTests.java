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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
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
    service = new SnPriceMapOfferExecutionService(taskScheduler, facilityService, priceMapOfferDao,
        restTemplate);
    service.setApiBaseUrl(TEST_BASE_URL);

    server = MockRestServiceServer.bindTo(restTemplate).build();
  }

  @Test
  public void executeOffer() throws IOException {
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

    Resource respResource = new ClassPathResource("instr-add-resp-01.json", getClass());
    HttpHeaders respHeaders = new HttpHeaders();
    respHeaders.setContentLength(respResource.contentLength());
    MultiValueMap<String, String> expectedContent = new LinkedMultiValueMap<>(8);
    expectedContent.add("topic", "ShedLoad");
    expectedContent.add("nodeIds", facPriceMap.getNodeId().toString());
    expectedContent.add("parameters[0].name", "/load/1");
    expectedContent.add("parameters[0].value", "1000");
    // @formatter:off
    server.expect(requestTo(startsWith(TEST_BASE_URL + "/solaruser/api/v1/sec/instr/add")))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.HOST, "localhost"))
        .andExpect(header(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate"))
        .andExpect(header(HttpHeaders.AUTHORIZATION, 
            startsWith("SNWS2 Credential=" + credProvider.getAuthorizationId() 
                + ",SignedHeaders=content-type;date;host,Signature=")))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(content().formData(expectedContent))
        .andRespond(withSuccess(respResource, APPLICATION_JSON_UTF8).headers(respHeaders));
    // @formatter:on

    given(priceMapOfferDao.save(offerEvent)).willReturn(offerEvent);

    // WHEN
    CompletableFuture<?> future = service.executePriceMapOfferEvent(offerId);

    // THEN
    assertThat("Future returned", future, notNullValue());
    Assert.fail("TODO");
  }

}
