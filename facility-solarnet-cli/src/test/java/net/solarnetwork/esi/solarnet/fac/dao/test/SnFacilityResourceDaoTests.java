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

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import net.solarnetwork.esi.domain.jpa.ResourceCharacteristicsEmbed;
import net.solarnetwork.esi.solarnet.fac.dao.FacilityResourceDao;
import net.solarnetwork.esi.solarnet.fac.dao.impl.SnFacilityResourceDao;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityResourceCharacteristics;
import net.solarnetwork.web.security.AuthorizationCredentialsProvider;
import net.solarnetwork.web.support.StaticAuthorizationCredentialsProvider;

/**
 * Test cases for the JPA {@link FacilityResourceDao} implementation.
 * 
 * @author matt
 * @version 1.0
 */
public class SnFacilityResourceDaoTests {

  private static final String TEST_BASE_URL = "http://localhost";

  private RestTemplate restTemplate;
  private AuthorizationCredentialsProvider credProvider;
  private FacilityResourceDao dao;

  private MockRestServiceServer server;

  @Before
  public void setup() {
    restTemplate = new RestTemplate();
    credProvider = new StaticAuthorizationCredentialsProvider(randomUUID().toString(),
        randomUUID().toString());
    SnFacilityResourceDao snDao = new SnFacilityResourceDao(restTemplate, credProvider);
    snDao.setApiBaseUrl(TEST_BASE_URL);
    dao = snDao;

    server = MockRestServiceServer.bindTo(restTemplate).build();
  }

  private void assertPropertiesEqual(ResourceCharacteristicsEmbed entity,
      ResourceCharacteristicsEmbed expected) {
    assertThat("Load power factor", entity.getLoadPowerFactor(),
        equalTo(expected.getLoadPowerFactor()));
    assertThat("Load power max", entity.getLoadPowerMax(), equalTo(expected.getLoadPowerMax()));
    assertThat("Response time", entity.getResponseTime(), equalTo(expected.getResponseTime()));
    assertThat("Storage energy capacity", entity.getStorageEnergyCapacity(),
        equalTo(expected.getStorageEnergyCapacity()));
    assertThat("Suppply power factor", entity.getSupplyPowerFactor(),
        equalTo(expected.getSupplyPowerFactor()));
    assertThat("Supply power max", entity.getSupplyPowerMax(),
        equalTo(expected.getSupplyPowerMax()));
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
        .andExpect(queryParam("metadataFilter", "(/pm/esi-resource/*~%3D.*)"))
        .andExpect(header(HttpHeaders.HOST, "localhost"))
        .andExpect(header(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate"))
        .andExpect(header(HttpHeaders.AUTHORIZATION, 
            startsWith("SNWS2 Credential=" + credProvider.getAuthorizationId() 
                + ",SignedHeaders=date;host,Signature=")))
        .andRespond(withSuccess(respResource, APPLICATION_JSON_UTF8).headers(respHeaders));
    // @formatter:on

    // when
    final FacilityResourceCharacteristics entity = dao.findById("rsrc1").get();

    // then
    ResourceCharacteristicsEmbed expected = new ResourceCharacteristicsEmbed();
    expected.setLoadPowerFactor(0.8f);
    expected.setLoadPowerMax(1000L);
    expected.setSupplyPowerFactor(0f);
    expected.setSupplyPowerMax(0L);
    expected.setStorageEnergyCapacity(0L);
    expected.responseTime().setMinMillis(5000L);
    expected.responseTime().setMaxMillis(60000L);
    assertPropertiesEqual(entity.characteristics(), expected);
  }

}
