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

package net.solarnetwork.esi.simple.fac.domain;

/**
 * An enumeration of offer execution states.
 * 
 * @author matt
 * @version 1.0
 */
public enum PriceMapOfferExecutionState {

  /** An unknown state. */
  UNKNOWN,

  /** Waiting to execute the event, i.e. before the event's start date. */
  WAITING,

  /** Executing the event. */
  EXECUTING,

  /** Completed executing the event. */
  COMPLETED,

  /** Aborted from executing the event. */
  ABORTED,

}
