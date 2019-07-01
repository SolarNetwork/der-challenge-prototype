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

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;

import net.solarnetwork.esi.domain.jpa.BaseLongEntity;
import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.domain.jpa.PowerComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceComponentsEmbed;
import net.solarnetwork.esi.domain.support.SignableMessage;

/**
 * A price map entity.
 * 
 * @author matt
 * @version 1.0
 */
@Entity
@Table(name = "PRICE_MAPS")
public class PriceMapEntity extends BaseLongEntity implements SignableMessage {

  private static final long serialVersionUID = 6873938081379625344L;

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
   * Default constructor.
   */
  public PriceMapEntity() {
    super();
  }

  /**
   * Construct with creation date.
   * 
   * @param created
   *        the creation date
   */
  public PriceMapEntity(Instant created) {
    super(created);
  }

  /**
   * Construct with values.
   * 
   * @param created
   *        the creation date
   * @param id
   *        the primary key
   */
  public PriceMapEntity(Instant created, Long id) {
    super(created, id);
  }

  /**
   * Create a copy of this instance.
   * 
   * <p>
   * All properties are copied onto the new instance.
   * </p>
   * 
   * @return the copy
   */
  public PriceMapEntity copy() {
    PriceMapEntity c = new PriceMapEntity(getCreated());
    c.setDuration(getDuration());
    c.setModified(getModified());
    c.setPowerComponents(getPowerComponents());
    c.setPriceComponents(getPriceComponents());
    c.setResponseTime(getResponseTime());
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

}
