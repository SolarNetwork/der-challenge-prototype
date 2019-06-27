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

package net.solarnetwork.esi.simple.xchg.service;

import net.solarnetwork.esi.domain.DerCharacteristicsOrBuilder;
import net.solarnetwork.esi.simple.xchg.domain.FacilityResourceCharacteristicsEntity;

/**
 * API for supporting facility characteristics.
 * 
 * @author matt
 * @version 1.0
 */
public interface FacilityCharacteristicsService {

  /**
   * Get the current resource characteristics for a specific facility.
   * 
   * @param facilityUid
   *        the UID of the facility to get the characteristics for
   * @return the current resource characteristics, or {@literal null} if not available
   */
  FacilityResourceCharacteristicsEntity resourceCharacteristics(String facilityUid);

  /**
   * Save resource characteristics.
   * 
   * @param characteristics
   *        the characteristics to save
   */
  void saveResourceCharacteristics(DerCharacteristicsOrBuilder characteristics);

}
