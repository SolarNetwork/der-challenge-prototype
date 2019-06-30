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

package net.solarnetwork.esi.domain.jpa;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Embeddable components of power.
 * 
 * @author matt
 * @version 1.0
 */
@Embeddable
public class PowerComponentsEmbed {

  @Basic
  @Column(name = "POWER_REAL", nullable = true, insertable = true, updatable = true)
  private Long realPower;

  @Basic
  @Column(name = "POWER_REACTIVE", nullable = true, insertable = true, updatable = true)
  private Long reactivePower;

  /**
   * Default constructor.
   */
  public PowerComponentsEmbed() {
    super();
  }

  /**
   * Construct with values.
   * 
   * @param realPower
   *        the real power, in watts (W)
   * @param reactivePower
   *        the reactive power, in in volt-amps-reactive (VAR)
   */
  public PowerComponentsEmbed(Long realPower, Long reactivePower) {
    super();
    this.realPower = realPower;
    this.reactivePower = reactivePower;
  }

  /**
   * Get the real power.
   * 
   * @return the real power, in watts (W), or {@literal null} if not available
   */
  public Long getRealPower() {
    return realPower;
  }

  /**
   * Set the real power.
   * 
   * @param realPower
   *        the power to set, in watts (W)
   */
  public void setRealPower(Long realPower) {
    this.realPower = realPower;
  }

  /**
   * Get the reactive power.
   * 
   * @return the reactive power, in volt-amps-reactive (VAR), or {@literal null} if not available
   */
  public Long getReactivePower() {
    return reactivePower;
  }

  /**
   * Set the reactive power.
   * 
   * @param reactivePower
   *        the power to set, in volt-amps-reactive (VAR)
   */
  public void setReactivePower(Long reactivePower) {
    this.reactivePower = reactivePower;
  }

}
