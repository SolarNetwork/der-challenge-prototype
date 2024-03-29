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

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

import net.solarnetwork.esi.simple.xchg.domain.FacilityEntity;
import net.solarnetwork.esi.simple.xchg.domain.FacilityInfo;

/**
 * DAO API for {@link FacilityEntity} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface FacilityEntityDao extends PagingAndSortingRepository<FacilityEntity, UUID> {

  /**
   * Find all available facility infos.
   * 
   * @param sort
   *        the sort
   * @return sorted collection of all matching infos
   */
  Iterable<FacilityInfo> findAllInfoBy(Sort sort);

  /**
   * Find a set of facilities by their UIDs.
   *
   * @param facilityUids
   *        the UIDs of the facilities to find
   * @return the matching results
   */
  Iterable<FacilityEntity> findAllByFacilityUidIn(Iterable<String> facilityUids);

  /**
   * Find a facility by its UID.
   * 
   * @param facilityUid
   *        the UID of the facility to find
   * @return the facility
   */
  Optional<FacilityEntity> findByFacilityUid(String facilityUid);

}
