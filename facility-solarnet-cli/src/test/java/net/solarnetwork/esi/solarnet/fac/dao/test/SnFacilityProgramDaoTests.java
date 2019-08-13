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

import net.solarnetwork.esi.domain.DerProgramType;
import net.solarnetwork.esi.solarnet.fac.dao.FacilityProgramDao;
import net.solarnetwork.esi.solarnet.fac.dao.impl.SnFacilityProgramDao;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityProgram;
import net.solarnetwork.esi.solarnet.fac.impl.WebUtils;
import net.solarnetwork.web.security.AuthorizationCredentialsProvider;
import net.solarnetwork.web.support.StaticAuthorizationCredentialsProvider;

/**
 * Test cases for the {@link SnFacilityProgramDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SnFacilityProgramDaoTests {

  private static final String TEST_BASE_URL = "http://localhost";

  private RestTemplate restTemplate;
  private AuthorizationCredentialsProvider credProvider;
  private FacilityProgramDao dao;

  private MockRestServiceServer server;

  @Before
  public void setup() {
    credProvider = new StaticAuthorizationCredentialsProvider(randomUUID().toString(),
        randomUUID().toString());
    restTemplate = WebUtils.setupSolarNetworkClient(new RestTemplate(), credProvider);
    SnFacilityProgramDao snDao = new SnFacilityProgramDao(restTemplate);
    snDao.setApiBaseUrl(TEST_BASE_URL);
    dao = snDao;

    server = MockRestServiceServer.bindTo(restTemplate).build();
  }

  private void assertEquals(FacilityProgram entity, FacilityProgram expected) {
    assertThat("ID", entity.getId(), equalTo(expected.getId()));
    assertThat("Program type", entity.getProgramType(), equalTo(expected.getProgramType()));
    assertThat("PriceMap ID", entity.getPriceMapId(), equalTo(expected.getPriceMapId()));
    assertThat("PriceMap group UID", entity.getPriceMapGroupUid(),
        equalTo(expected.getPriceMapGroupUid()));
    assertThat("Resource ID", entity.getResourceId(), equalTo(expected.getResourceId()));
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
        .andExpect(queryParam("metadataFilter", "(/pm/esi-program/*~%3D.*)"))
        .andExpect(header(HttpHeaders.HOST, "localhost"))
        .andExpect(header(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate"))
        .andExpect(header(HttpHeaders.AUTHORIZATION, 
            startsWith("SNWS2 Credential=" + credProvider.getAuthorizationId() 
                + ",SignedHeaders=date;host,Signature=")))
        .andRespond(withSuccess(respResource, APPLICATION_JSON_UTF8).headers(respHeaders));
    // @formatter:on

    // when
    final FacilityProgram entity = dao.findById("prgrm1").get();

    // then
    FacilityProgram expected = new FacilityProgram("prgrm1", DerProgramType.MARKET_PRICE_RESPONSE);
    expected.setPriceMapId("");
    expected.setPriceMapGroupUid("Demand Response");
    expected.setResourceId("rsrc1");
    assertEquals(entity, expected);
  }

}
