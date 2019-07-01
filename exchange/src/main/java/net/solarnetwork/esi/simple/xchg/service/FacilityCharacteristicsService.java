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

import java.util.Set;

import javax.annotation.Nonnull;

import net.solarnetwork.esi.domain.DerCharacteristicsOrBuilder;
import net.solarnetwork.esi.domain.DerProgramSetOrBuilder;
import net.solarnetwork.esi.domain.DerProgramType;
import net.solarnetwork.esi.domain.PriceMapCharacteristicsOrBuilder;
import net.solarnetwork.esi.simple.xchg.domain.FacilityInfo;
import net.solarnetwork.esi.simple.xchg.domain.FacilityResourceCharacteristicsEntity;
import net.solarnetwork.esi.simple.xchg.domain.PriceMapEntity;

/**
 * API for supporting facility characteristics.
 * 
 * @author matt
 * @version 1.0
 */
public interface FacilityCharacteristicsService {

  /**
   * Get a listing of all available facilities.
   * 
   * <p>
   * The facilities will be ordered by {@code customerId}, ascending.
   * </p>
   * 
   * @return the facilities
   */
  Iterable<FacilityInfo> listFacilities();

  /**
   * Get a info on a single facility.
   * 
   * @return the facility info
   * @throws IllegalArgumentException
   *         if no facility exists for {@code facilityUid}
   */
  @Nonnull
  FacilityInfo facilityInfo(String facilityUid);

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
   * @return the persisted characteristics
   */
  FacilityResourceCharacteristicsEntity saveResourceCharacteristics(
      DerCharacteristicsOrBuilder characteristics);

  /**
   * Get the set of currently active DER program types.
   * 
   * @param facilityUid
   *        the UID of the facility to get the characteristics for
   * @return the types
   */
  @Nonnull
  Set<DerProgramType> activeProgramTypes(String facilityUid);

  /**
   * Save the set of active DER program types.
   * 
   * <p>
   * This method completely replaces the set of active program types with the values in the given
   * set.
   * </p>
   * 
   * @param programSet
   *        the set of programs to save as active
   */
  void saveActiveProgramTypes(DerProgramSetOrBuilder programSet);

  /**
   * Get the current price map for a specific facility.
   * 
   * @param facilityUid
   *        the UID of the facility to get the price map for
   * @return the types
   * @throws IllegalArgumentException
   *         if no facility exists for {@code facilityUid}
   */
  @Nonnull
  PriceMapEntity priceMap(String facilityUid);

  /**
   * Save the price map characteristics for a facility.
   * 
   * <p>
   * This method completely replaces price map characteristics for the specified facility with the
   * given value.
   * </p>
   * 
   * @param priceMapCharacteristcis
   *        the price map characteristics to save
   */
  void savePriceMap(PriceMapCharacteristicsOrBuilder priceMapCharacteristcis);

}
