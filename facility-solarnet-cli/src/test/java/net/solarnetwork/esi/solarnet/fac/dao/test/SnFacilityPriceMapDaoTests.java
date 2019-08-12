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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Before;
import org.junit.Test;

import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
import net.solarnetwork.esi.solarnet.fac.dao.FacilityPriceMapDao;
import net.solarnetwork.esi.solarnet.fac.dao.impl.SnFacilityPriceMapDao;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityPriceMap;

/**
 * Test cases for the {@link SnFacilityPriceMapDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SnFacilityPriceMapDaoTests {

  private FacilityPriceMapDao dao;

  @Before
  public void setup() {
    dao = new SnFacilityPriceMapDao();
  }

  private void assertPropertiesEqual(PriceMapEmbed entity, PriceMapEmbed expected) {
    assertThat("Duration", entity.getDuration(), equalTo(expected.getDuration()));
    assertThat("Power components", entity.getPowerComponents(),
        equalTo(expected.getPowerComponents()));
    assertThat("Price components", entity.getPriceComponents().scaledExactly(2),
        equalTo(expected.getPriceComponents().scaledExactly(2)));
    assertThat("Response time", entity.getResponseTime(), equalTo(expected.getResponseTime()));
  }

  private void assertEquals(FacilityPriceMap entity, FacilityPriceMap expected) {
    assertThat("ID", entity.getId(), equalTo(expected.getId()));
    assertPropertiesEqual(entity.getPriceMap(), expected.getPriceMap());
  }

  @Test
  public void getById() {
    FacilityPriceMap entity = dao.findById("TODO").get();
    // TODO assertEqual(entity, last);
  }

}
