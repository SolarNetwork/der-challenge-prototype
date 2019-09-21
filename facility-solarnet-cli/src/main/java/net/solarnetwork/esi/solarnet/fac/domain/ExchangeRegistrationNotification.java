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

package net.solarnetwork.esi.solarnet.fac.domain;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.context.ApplicationEvent;

/**
 * Base class for exchange registration events.
 * 
 * @author matt
 * @version 1.0
 */
@ParametersAreNonnullByDefault
public abstract class ExchangeRegistrationNotification extends ApplicationEvent {

  private static final long serialVersionUID = -7161917000677959436L;

  private ExchangeRegistrationNotification(Object source) {
    super(source);
  }

  /**
   * Event published when an exchange completes the registration process.
   * 
   * @author matt
   * @version 1.0
   */
  public static final class ExchangeRegistrationCompleted extends ExchangeRegistrationNotification {

    private static final long serialVersionUID = -4467969205173775266L;

    private final boolean success;

    public ExchangeRegistrationCompleted(ExchangeRegistrationEntity entity, boolean success) {
      super(entity);
      this.success = success;
    }

    /**
     * Get the success flag.
     * 
     * @return the success
     */
    public boolean isSuccess() {
      return success;
    }

    /**
     * Get the exchange registration associated with this event.
     * 
     * @return
     */
    public ExchangeRegistrationEntity getExchangeRegistration() {
      return (ExchangeRegistrationEntity) getSource();
    }

  }

}
