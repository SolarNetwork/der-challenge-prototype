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
import java.util.Locale;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;

import net.solarnetwork.esi.domain.jpa.BaseLongEntity;
import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.domain.jpa.PowerComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
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

  private static final long serialVersionUID = -305242540130865864L;

  @Embedded
  private PriceMapEmbed priceMap;

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
    PriceMapEntity c = new PriceMapEntity(getCreated(), getId());
    c.setModified(getModified());
    PriceMapEmbed pm = getPriceMap();
    if (pm != null) {
      c.setPriceMap(pm.copy());
    }
    return c;
  }

  /**
   * Get a brief informational string out of the main aspects of this price map.
   * 
   * @param locale
   *        the desired locale
   * @return the string
   */
  @Nonnull
  public String toInfoString(Locale locale) {
    return priceMap().toInfoString(locale);
  }

  /**
   * Get the info string in the default locale.
   * 
   * @return the info string
   * @see #toInfoString(Locale)
   */
  @Nonnull
  public String getInfo() {
    return priceMap().getInfo();
  }

  @Override
  public int signatureMessageBytesSize() {
    PriceMapEmbed e = (priceMap != null ? priceMap : new PriceMapEmbed());
    return e.signatureMessageBytesSize();
  }

  @Override
  public void addSignatureMessageBytes(ByteBuffer buf) {
    PriceMapEmbed e = (priceMap != null ? priceMap : new PriceMapEmbed());
    e.addSignatureMessageBytes(buf);
  }

  /**
   * Get the price map details.
   * 
   * @return the price map details
   */
  public PriceMapEmbed getPriceMap() {
    return priceMap;
  }

  /**
   * Set the price map details.
   * 
   * @param priceMap
   *        the price map details to set
   */
  public void setPriceMap(PriceMapEmbed priceMap) {
    this.priceMap = priceMap;
  }

  /**
   * Get an optional of the price map details.
   * 
   * @return the optional price map details
   */
  @Nonnull
  public Optional<PriceMapEmbed> priceMapOpt() {
    return Optional.ofNullable(getPriceMap());
  }

  /**
   * Get the price map details, creating a new one if it doesn't already exist.
   * 
   * @return the price map details
   */
  @Nonnull
  public PriceMapEmbed priceMap() {
    PriceMapEmbed pm = getPriceMap();
    if (pm == null) {
      pm = new PriceMapEmbed();
      setPriceMap(pm);
    }
    return pm;
  }

  /**
   * Get the power components.
   * 
   * @return the power components
   */
  public PowerComponentsEmbed getPowerComponents() {
    return priceMapOpt().map(PriceMapEmbed::getPowerComponents).orElse(null);
  }

  /**
   * Set the power components.
   * 
   * @param powerComponents
   *        the power components to set
   */
  public void setPowerComponents(PowerComponentsEmbed powerComponents) {
    priceMap().setPowerComponents(powerComponents);
  }

  /**
   * Get the duration of time for this price map.
   * 
   * @return the duration
   */
  public Duration getDuration() {
    return priceMapOpt().map(PriceMapEmbed::getDuration).orElse(null);
  }

  /**
   * Set the duration of time for this price map.
   * 
   * @param duration
   *        the duration to set
   */
  public void setDuration(Duration duration) {
    priceMap().setDuration(duration);
  }

  /**
   * Get the response time range.
   * 
   * @return the response time range
   */
  public DurationRangeEmbed getResponseTime() {
    return priceMapOpt().map(PriceMapEmbed::getResponseTime).orElse(null);
  }

  /**
   * Set the response time range.
   * 
   * @param responseTime
   *        the response time range to set
   */
  public void setResponseTime(DurationRangeEmbed responseTime) {
    priceMap().setResponseTime(responseTime);
  }

  /**
   * Get the price components.
   * 
   * @return the price components
   */
  public PriceComponentsEmbed getPriceComponents() {
    return priceMapOpt().map(PriceMapEmbed::getPriceComponents).orElse(null);
  }

  /**
   * Set the price components.
   * 
   * @param priceComponents
   *        the price components to set
   */
  public void setPriceComponents(PriceComponentsEmbed priceComponents) {
    priceMap().setPriceComponents(priceComponents);
  }

}
