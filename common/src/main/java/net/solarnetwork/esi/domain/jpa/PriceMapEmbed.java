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
import java.time.Duration;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

import net.solarnetwork.esi.domain.support.SignableMessage;

/**
 * Embeddable price map details.
 * 
 * @author matt
 * @version 1.0
 */
@Embeddable
public class PriceMapEmbed implements SignableMessage {

  @Embedded
  private PowerComponentsEmbed powerComponents;

  @Basic
  @Column(name = "DUR", nullable = false, insertable = true, updatable = true)
  private Duration duration;

  // CHECKSTYLE IGNORE LineLength FOR NEXT 4 LINES
  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "min", column = @Column(name = "RESP_TIME_MIN", nullable = false)),
      @AttributeOverride(name = "max", column = @Column(name = "RESP_TIME_MAX", nullable = false)) })
  private DurationRangeEmbed responseTime;

  @Embedded
  private PriceComponentsEmbed priceComponents;

  /**
   * Create a copy of this instance.
   * 
   * <p>
   * All properties are copied onto the new instance.
   * </p>
   * 
   * @return the copy
   */
  public PriceMapEmbed copy() {
    PriceMapEmbed c = new PriceMapEmbed();
    c.setPowerComponents(getPowerComponents());
    c.setDuration(getDuration());
    c.setResponseTime(getResponseTime());
    c.setPriceComponents(getPriceComponents());
    return c;
  }

  @Override
  public int signatureMessageBytesSize() {
    PowerComponentsEmbed power = powerComponents != null ? powerComponents
        : new PowerComponentsEmbed();
    DurationRangeEmbed rt = responseTime != null ? responseTime : new DurationRangeEmbed();
    PriceComponentsEmbed price = priceComponents != null ? priceComponents
        : new PriceComponentsEmbed();
    return power.signatureMessageBytesSize() + SignableMessage.durationSignatureMessageSize()
        + rt.signatureMessageBytesSize() + price.signatureMessageBytesSize();
  }

  @Override
  public void addSignatureMessageBytes(ByteBuffer buf) {
    PowerComponentsEmbed power = powerComponents != null ? powerComponents
        : new PowerComponentsEmbed();
    power.addSignatureMessageBytes(buf);

    SignableMessage.addDurationSignatureMessageBytes(buf, duration);

    DurationRangeEmbed rt = responseTime != null ? responseTime : new DurationRangeEmbed();
    rt.addSignatureMessageBytes(buf);

    PriceComponentsEmbed price = priceComponents != null ? priceComponents
        : new PriceComponentsEmbed();
    price.addSignatureMessageBytes(buf);
  }

  @Override
  public int hashCode() {
    return Objects.hash(duration, powerComponents, priceComponents, responseTime);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof PriceMapEmbed)) {
      return false;
    }
    PriceMapEmbed other = (PriceMapEmbed) obj;
    return Objects.equals(duration, other.duration)
        && Objects.equals(powerComponents, other.powerComponents)
        && Objects.equals(priceComponents, other.priceComponents)
        && Objects.equals(responseTime, other.responseTime);
  }

  /**
   * Get the power components.
   * 
   * @return the power components
   */
  public PowerComponentsEmbed getPowerComponents() {
    return powerComponents;
  }

  /**
   * Set the power components.
   * 
   * @param powerComponents
   *        the power components to set
   */
  public void setPowerComponents(PowerComponentsEmbed powerComponents) {
    this.powerComponents = powerComponents;
  }

  /**
   * Get the power component details, creating a new one if it doesn't already exist.
   * 
   * @return the power component details
   */
  @Nonnull
  public PowerComponentsEmbed powerComponents() {
    PowerComponentsEmbed e = getPowerComponents();
    if (e == null) {
      e = new PowerComponentsEmbed();
      setPowerComponents(e);
    }
    return e;
  }

  /**
   * Get the duration of time for this price map.
   * 
   * @return the duration
   */
  public Duration getDuration() {
    return duration;
  }

  /**
   * Set the duration of time for this price map.
   * 
   * @param duration
   *        the duration to set
   */
  public void setDuration(Duration duration) {
    this.duration = duration;
  }

  /**
   * Get the response time range.
   * 
   * @return the response time range
   */
  public DurationRangeEmbed getResponseTime() {
    return responseTime;
  }

  /**
   * Set the response time range.
   * 
   * @param responseTime
   *        the response time range to set
   */
  public void setResponseTime(DurationRangeEmbed responseTime) {
    this.responseTime = responseTime;
  }

  /**
   * Get the response time details, creating a new one if it doesn't already exist.
   * 
   * @return the response time details
   */
  @Nonnull
  public DurationRangeEmbed responseTime() {
    DurationRangeEmbed e = getResponseTime();
    if (e == null) {
      e = new DurationRangeEmbed();
      setResponseTime(e);
    }
    return e;
  }

  /**
   * Get the price components.
   * 
   * @return the price components
   */
  public PriceComponentsEmbed getPriceComponents() {
    return priceComponents;
  }

  /**
   * Set the price components.
   * 
   * @param priceComponents
   *        the price components to set
   */
  public void setPriceComponents(PriceComponentsEmbed priceComponents) {
    this.priceComponents = priceComponents;
  }

  /**
   * Get the price component details, creating a new one if it doesn't already exist.
   * 
   * @return the price component details
   */
  @Nonnull
  public PriceComponentsEmbed priceComponents() {
    PriceComponentsEmbed e = getPriceComponents();
    if (e == null) {
      e = new PriceComponentsEmbed();
      setPriceComponents(e);
    }
    return e;
  }

}
