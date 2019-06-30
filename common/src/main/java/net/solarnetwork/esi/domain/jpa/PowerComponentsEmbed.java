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

import java.nio.ByteBuffer;
import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;

import net.solarnetwork.esi.domain.support.SignableMessage;

/**
 * Embeddable components of power.
 * 
 * @author matt
 * @version 1.0
 */
@Embeddable
public class PowerComponentsEmbed implements SignableMessage {

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

  @Override
  public int hashCode() {
    return Objects.hash(reactivePower, realPower);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof PowerComponentsEmbed)) {
      return false;
    }
    PowerComponentsEmbed other = (PowerComponentsEmbed) obj;
    return Objects.equals(reactivePower, other.reactivePower)
        && Objects.equals(realPower, other.realPower);
  }

  @Override
  public int signatureMessageBytesSize() {
    return Long.BYTES * 2;
  }

  @Override
  public void addSignatureMessageBytes(ByteBuffer buf) {
    buf.putLong(realPower != null ? realPower.longValue() : 0L);
    buf.putLong(reactivePower != null ? reactivePower.longValue() : 0L);
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
