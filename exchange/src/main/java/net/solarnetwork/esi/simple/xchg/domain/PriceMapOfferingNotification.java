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

package net.solarnetwork.esi.simple.xchg.domain;

import org.springframework.context.ApplicationEvent;

/**
 * Base class for price map offering events.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class PriceMapOfferingNotification extends ApplicationEvent {

  private static final long serialVersionUID = -5787634174136594745L;

  private PriceMapOfferingNotification(Object source) {
    super(source);
  }

  /**
   * Event related to a {@link FacilityPriceMapOfferEntity}.
   * 
   * @author matt
   * @version 1.0
   */
  public abstract static class FacilityPriceMapOfferEntityNotification
      extends PriceMapOfferingNotification {

    private static final long serialVersionUID = 5292694791612891264L;

    /**
     * Constructor.
     * 
     * @param entity
     *        the entity
     */
    public FacilityPriceMapOfferEntityNotification(FacilityPriceMapOfferEntity entity) {
      super(entity);
    }

    /**
     * Get the price map offer associated with this event.
     * 
     * @return the offer
     */
    public FacilityPriceMapOfferEntity getOffer() {
      return (FacilityPriceMapOfferEntity) getSource();
    }

  }

  /**
   * Event published when an exchange completes the registration process.
   * 
   * @author matt
   * @version 1.0
   */
  public static final class FacilityPriceMapOfferCompleted
      extends FacilityPriceMapOfferEntityNotification {

    private static final long serialVersionUID = 6016764496596236293L;

    private final boolean success;

    /**
     * Constructor.
     * 
     * @param entity
     *        the entity
     */
    public FacilityPriceMapOfferCompleted(FacilityPriceMapOfferEntity entity) {
      super(entity);
      this.success = entity.isProposed() && entity.isAccepted();
    }

    /**
     * Get the success flag.
     * 
     * <p>
     * This returns {@literal true} if the price map offer was both proposed and accepted.
     * </p>
     * 
     * @return the success
     */
    public boolean isSuccess() {
      return success;
    }

  }

}
