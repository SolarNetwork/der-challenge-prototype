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

import javax.annotation.Nonnull;

import net.solarnetwork.esi.domain.PriceMapOffer;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferEventEntity;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferNotification.PriceMapOfferAccepted;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferNotification.PriceMapOfferCountered;

/**
 * API for managing price map events.
 * 
 * @author matt
 * @version 1.0
 */
public interface PriceMapService {

  /**
   * Receive a price map offer.
   * 
   * <p>
   * Implementations must publish a {@link PriceMapOfferAccepted} event after accepting the offer. A
   * {@link PriceMapOfferCountered} event must be published after proposing a counter offer.
   * </p>
   * 
   * @param offer
   *        the offer
   * @return the offer entity, which informs the caller if the offer has been accepted, rejected, or
   *         contains a counter-offer
   */
  @Nonnull
  PriceMapOfferEventEntity receivePriceMapOffer(PriceMapOffer offer);

}
