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

import org.junit.Before;
import org.junit.Test;
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

  private RestTemplate restTemplate;
  private AuthorizationCredentialsProvider credentialsProvider;
  private FacilityResourceDao dao;

  @Before
  public void setup() {
    restTemplate = new RestTemplate();
    credentialsProvider = new StaticAuthorizationCredentialsProvider(randomUUID().toString(),
        randomUUID().toString());
    dao = new SnFacilityResourceDao(restTemplate, credentialsProvider);
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
  public void getById() {
    FacilityResourceCharacteristics entity = dao.findById("TODO").get();
    // TODO assertPropertiesEqual(entity.characteristics(), last);
  }

}
