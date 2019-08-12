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

package net.solarnetwork.esi.solarnet.fac.service;

import java.util.Set;

import javax.annotation.Nonnull;

import net.solarnetwork.esi.solarnet.fac.domain.FacilityPriceMap;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityResourceCharacteristics;

/**
 * API for managing the characteristics of facility resources.
 * 
 * @author matt
 * @version 1.0
 */
public interface FacilityCharacteristicsService {

  /**
   * Get the current resource characteristics.
   * 
   * @return the current resource characteristics, never {@literal null}
   */
  @Nonnull
  Iterable<FacilityResourceCharacteristics> resourceCharacteristics();

  /**
   * Get the set of currently active DER program types.
   * 
   * @return the types
   */
  @Nonnull
  Set<String> activeProgramTypes();

  /**
   * Get the current facility price map settings.
   * 
   * @return the price maps
   */
  @Nonnull
  Iterable<FacilityPriceMap> priceMaps();

  /**
   * Get a price map by ID.
   * 
   * @param priceMapId
   *        the ID of the price map to get
   * @return the price map
   * @throws IllegalArgumentException
   *         if the price map is not found
   */
  @Nonnull
  FacilityPriceMap priceMap(String priceMapId);

}
