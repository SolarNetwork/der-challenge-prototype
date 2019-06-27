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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.solarnetwork.esi.domain.DerCharacteristicsOrBuilder;
import net.solarnetwork.esi.simple.xchg.domain.FacilityResourceCharacteristicsEntity;
import net.solarnetwork.esi.simple.xchg.service.FacilityCharacteristicsService;

/**
 * DAO based implementation of {@link FacilityCharacteristicsService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoFacilityCharacteristicsService implements FacilityCharacteristicsService {

  private static final Logger log = LoggerFactory
      .getLogger(DaoFacilityCharacteristicsService.class);

  @Override
  public FacilityResourceCharacteristicsEntity resourceCharacteristics(String facilityUid) {
    log.info("TOOD: get characteristics for facility not implemented yet.");
    return null;
  }

  @Override
  public void saveResourceCharacteristics(DerCharacteristicsOrBuilder characteristics) {
    log.info("TOOD: save characteristics not implemented yet.");
  }

}
