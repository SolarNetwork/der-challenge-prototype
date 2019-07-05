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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;

import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferExecutionState;

/**
 * API for executing price map offers.
 * 
 * @author matt
 * @version 1.0
 */
public interface PriceMapOfferExecutionService {

  /**
   * Execute a price offer event.
   * 
   * @param offerId
   *        the ID of the event to execute
   * @return TODO
   */
  @Async
  CompletableFuture<?> executePriceMapOfferEvent(UUID offerId);

  /**
   * Finish a price offer event.
   * 
   * @param offerId
   *        the ID of the event to finish
   * @param newState
   *        the desired final state; should be either {@link PriceMapOfferExecutionState#COMPLETED}
   *        or {@link PriceMapOfferExecutionState#ABORTED}
   * @return TODO
   */
  @Async
  CompletableFuture<?> endPriceMapOfferEvent(UUID offerId, PriceMapOfferExecutionState newState);

}
