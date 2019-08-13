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

package net.solarnetwork.esi.solarnet.fac.dao.test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Currency;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
import net.solarnetwork.esi.solarnet.fac.dao.FacilityPriceMapDao;
import net.solarnetwork.esi.solarnet.fac.dao.impl.SnFacilityPriceMapDao;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityPriceMap;
import net.solarnetwork.esi.solarnet.fac.impl.WebUtils;
import net.solarnetwork.web.security.AuthorizationCredentialsProvider;
import net.solarnetwork.web.support.StaticAuthorizationCredentialsProvider;

/**
 * Test cases for the {@link SnFacilityPriceMapDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SnFacilityPriceMapDaoTests {

  private static final String TEST_BASE_URL = "http://localhost";

  private RestTemplate restTemplate;
  private AuthorizationCredentialsProvider credProvider;
  private FacilityPriceMapDao dao;

  private MockRestServiceServer server;

  @Before
  public void setup() {
    credProvider = new StaticAuthorizationCredentialsProvider(randomUUID().toString(),
        randomUUID().toString());
    restTemplate = WebUtils.setupSolarNetworkClient(new RestTemplate(), credProvider);
    SnFacilityPriceMapDao snDao = new SnFacilityPriceMapDao(restTemplate);
    snDao.setApiBaseUrl(TEST_BASE_URL);
    dao = snDao;

    server = MockRestServiceServer.bindTo(restTemplate).build();
  }

  private void assertPropertiesEqual(PriceMapEmbed entity, PriceMapEmbed expected) {
    assertThat("Duration", entity.getDuration(), equalTo(expected.getDuration()));
    assertThat("Power components", entity.getPowerComponents(),
        equalTo(expected.getPowerComponents()));
    assertThat("Price components", entity.getPriceComponents(),
        equalTo(expected.getPriceComponents()));
    assertThat("Response time", entity.getResponseTime(), equalTo(expected.getResponseTime()));
  }

  private void assertEquals(FacilityPriceMap entity, FacilityPriceMap expected) {
    assertThat("ID", entity.getId(), equalTo(expected.getId()));
    assertThat("Group UID", entity.getGroupUid(), equalTo(expected.getGroupUid()));
    assertPropertiesEqual(entity.getPriceMap(), expected.getPriceMap());
  }

  @Test
  public void getById() throws IOException {
    // given
    Resource respResource = new ClassPathResource("node-metadata-01.json", getClass());
    HttpHeaders respHeaders = new HttpHeaders();
    respHeaders.setContentLength(respResource.contentLength());
    // @formatter:off
    server.expect(requestTo(startsWith(TEST_BASE_URL + "/solarquery/api/v1/sec/nodes/meta")))
        .andExpect(method(HttpMethod.GET))
        .andExpect(queryParam("metadataFilter", "(/pm/esi-pricemap/*~%3D.*)"))
        .andExpect(header(HttpHeaders.HOST, "localhost"))
        .andExpect(header(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate"))
        .andExpect(header(HttpHeaders.AUTHORIZATION, 
            startsWith("SNWS2 Credential=" + credProvider.getAuthorizationId() 
                + ",SignedHeaders=date;host,Signature=")))
        .andRespond(withSuccess(respResource, APPLICATION_JSON_UTF8).headers(respHeaders));
    // @formatter:on

    // when
    final FacilityPriceMap entity = dao.findById("pm1").get();

    // then
    PriceMapEmbed pme = new PriceMapEmbed();
    pme.powerComponents().setRealPower(-2000L);
    pme.powerComponents().setReactivePower(-1000L);
    pme.setDuration(Duration.ofMillis(600000L));
    pme.responseTime().setMinMillis(10000L);
    pme.responseTime().setMaxMillis(60000L);
    pme.priceComponents().setCurrency(Currency.getInstance("USD"));
    pme.priceComponents().setApparentEnergyPrice(new BigDecimal("0.123456789"));

    FacilityPriceMap expected = new FacilityPriceMap("pm1", pme);
    expected.setGroupUid("Demand Response");
    assertEquals(entity, expected);
  }

}
