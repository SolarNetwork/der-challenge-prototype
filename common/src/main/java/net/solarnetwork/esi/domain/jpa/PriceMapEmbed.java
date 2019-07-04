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

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Currency;
import java.util.Locale;
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
    powerComponents().addSignatureMessageBytes(buf);
    SignableMessage.addDurationSignatureMessageBytes(buf, duration);
    responseTime().addSignatureMessageBytes(buf);
    priceComponents().addSignatureMessageBytes(buf);
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

  @Override
  public String toString() {
    return "PriceMap{powerComponents=" + powerComponents + ", duration=" + duration
        + ", responseTime=" + responseTime + ", priceComponents=" + priceComponents + "}";
  }

  /**
   * Calculate the theoretical cost represented by this price map as the apparent power multiplied
   * by the duration (in hours) multiplied by the apparent energy price.
   * 
   * @return the apparent energy cost, in the configured currency units per volt-amp-hours (VAh)
   */
  @Nonnull
  public BigDecimal calculatedApparentEnergyCost() {
    BigDecimal vahPrice = priceComponents().apparentEnergyPrice();
    if (vahPrice.equals(BigDecimal.ZERO)) {
      return vahPrice;
    }
    PowerComponentsEmbed p = powerComponents();
    double va = p.derivedApparentPower();
    double vah = va * durationHours();
    return vahPrice.multiply(new BigDecimal(String.valueOf(vah)));
  }

  /**
   * Get the fractional hours represented by the configured duration.
   * 
   * @return the duration, as fractional hours
   */
  public double durationHours() {
    return duration().toMillis() / (1000.0 * 60.0 * 60.0);
  }

  /**
   * Get a brief informational string out of the main aspects of this price map.
   * 
   * @return the string
   */
  @Nonnull
  public String toInfoString(Locale locale) {
    PowerComponentsEmbed p = powerComponents();
    PriceComponentsEmbed pr = priceComponents();
    double hours = durationHours();
    Currency c = pr.currency();
    BigDecimal cost = calculatedApparentEnergyCost();
    return String.format(locale, "%,.3f kVA ~ %,.3fh @ %s%,.2f/kVAh = %3$s%,.2f",
        p.derivedApparentPower() / 1000.0, hours, c.getSymbol(locale),
        pr.apparentEnergyPrice().movePointRight(3), cost);
  }

  /**
   * Get the info string in the default locale.
   * 
   * @return the info string
   * @see #toInfoString(Locale)
   */
  @Nonnull
  public String getInfo() {
    return toInfoString(Locale.getDefault());
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
   * <p>
   * If a new power component is created, its values will be initialized to zero.
   * </p>
   * 
   * @return the power component details
   */
  @Nonnull
  public PowerComponentsEmbed powerComponents() {
    PowerComponentsEmbed e = getPowerComponents();
    if (e == null) {
      e = new PowerComponentsEmbed(0L, 0L);
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
   * Get the duration, never {@literal null}.
   */
  @Nonnull
  public Duration duration() {
    Duration d = getDuration();
    if (d == null) {
      d = Duration.ZERO;
    }
    return d;
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
   * <p>
   * If a new instance is created, it will be initialized with the currency for the default locale
   * and zero-values for all prices.
   * </p>
   * 
   * @return the price component details
   */
  @Nonnull
  public PriceComponentsEmbed priceComponents() {
    PriceComponentsEmbed e = getPriceComponents();
    if (e == null) {
      e = new PriceComponentsEmbed(Currency.getInstance(Locale.getDefault()), BigDecimal.ZERO);
      setPriceComponents(e);
    }
    return e;
  }

}
