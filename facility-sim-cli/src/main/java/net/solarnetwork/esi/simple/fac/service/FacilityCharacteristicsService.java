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

package net.solarnetwork.esi.simple.fac.service;

import java.util.Set;

import javax.annotation.Nonnull;

import net.solarnetwork.esi.simple.fac.domain.ResourceCharacteristicsEntity;

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
  ResourceCharacteristicsEntity resourceCharacteristics();

  /**
   * Save resource characteristics.
   * 
   * <p>
   * Only non-{@literal null} properties will be persisted.
   * </p>
   * 
   * @param characteristics
   *        the characteristics to save
   */
  void saveResourceCharacteristics(ResourceCharacteristicsEntity characteristics);

  /**
   * Get the set of currently active DER program types.
   * 
   * @return the types
   */
  @Nonnull
  Set<String> activeProgramTypes();

  /**
   * Save the set of active DER program types.
   * 
   * <p>
   * This method completely replaces the set of active program types with the values in the given
   * set.
   * </p>
   * 
   * @param programs
   *        the set of programs to save as active
   */
  void saveActiveProgramTypes(Set<String> programs);
}
