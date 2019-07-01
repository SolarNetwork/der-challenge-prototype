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

package net.solarnetwork.esi.simple.xchg.dao;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.solarnetwork.esi.simple.xchg.domain.FacilityResourceCharacteristicsEntity;

/**
 * DAO API for facility resource characteristic entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface FacilityResourceCharacteristicsEntityDao
    extends CrudRepository<FacilityResourceCharacteristicsEntity, UUID> {

  /**
   * Find a resource characteristics by the UID of its facility.
   * 
   * @param facilityUid
   *        the UID of the facility to find
   * @return the facility
   */
  Optional<FacilityResourceCharacteristicsEntity> findByFacility_FacilityUid(String facilityUid);

}
