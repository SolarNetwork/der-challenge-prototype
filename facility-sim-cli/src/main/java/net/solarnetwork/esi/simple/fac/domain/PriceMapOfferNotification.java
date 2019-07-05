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

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.context.ApplicationEvent;

/**
 * Base class for price map offer related events.
 * 
 * @author matt
 * @version 1.0
 */
@ParametersAreNonnullByDefault
public abstract class PriceMapOfferNotification extends ApplicationEvent {

  private static final long serialVersionUID = -9174587607142526937L;

  private PriceMapOfferNotification(Object source) {
    super(source);
  }

  /**
   * An event related to a {@link PriceMapOfferEventEntity} instance.
   * 
   * @author matt
   * @version 1.0
   */
  public abstract static class PriceMapOfferEventEntityNotification
      extends PriceMapOfferNotification {

    private static final long serialVersionUID = -636410790824000953L;

    private PriceMapOfferEventEntityNotification(PriceMapOfferEventEntity source) {
      super(source);
    }

    /**
     * Get the price map offer event associated with this event.
     * 
     * @return the entity
     */
    public PriceMapOfferEventEntity getOfferEvent() {
      return (PriceMapOfferEventEntity) getSource();
    }

  }

  /**
   * Event published when a price map offer has been accepted.
   * 
   * @author matt
   * @version 1.0
   */
  public static final class PriceMapOfferAccepted extends PriceMapOfferEventEntityNotification {

    private static final long serialVersionUID = 7666498080860953568L;

    public PriceMapOfferAccepted(PriceMapOfferEventEntity entity) {
      super(entity);
    }

  }

  /**
   * Event published when a price map offer has been countered with a different price map.
   * 
   * @author matt
   * @version 1.0
   */
  public static final class PriceMapOfferCountered extends PriceMapOfferEventEntityNotification {

    private static final long serialVersionUID = -4926032113197841644L;

    public PriceMapOfferCountered(PriceMapOfferEventEntity entity) {
      super(entity);
    }

  }

}
