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

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
import net.solarnetwork.esi.simple.xchg.domain.PriceMapOfferingEntity;

/**
 * API for functions related to price map-based events.
 * 
 * @author matt
 * @version 1.0
 */
public interface PriceMapOfferingService {

  /**
   * Create a new price map offering.
   * 
   * @param priceMap
   *        the price map details
   * @param startDate
   *        the start date of the offer
   * @return the new offering entity
   */
  PriceMapOfferingEntity createPriceMapOffering(PriceMapEmbed priceMap, Instant startDate);

  /**
   * Create new price map offers to a set of facilities.
   * 
   * <p>
   * Call this method after creating an offering via
   * {@link #createPriceMapOffering(PriceMapEmbed, Instant)}, to make an offer of the associated
   * price map to a set of facilities.
   * </p>
   * 
   * @param offeringId
   *        the ID of the {@link PriceMapOfferingEntity} to create offers to facilities from
   * @param facilityUids
   *        the UIDs of the facilities to make the offers for
   * @return the updated offering entity
   */
  PriceMapOfferingEntity makeOfferToFacilities(UUID offeringId, Set<String> facilityUids);

}
