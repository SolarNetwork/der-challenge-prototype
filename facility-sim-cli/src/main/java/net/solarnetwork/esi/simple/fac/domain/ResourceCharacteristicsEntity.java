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
import java.time.Instant;

import javax.annotation.Nonnull;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;

import net.solarnetwork.esi.domain.jpa.BaseLongEntity;
import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.domain.support.SignableMessage;

/**
 * Resource characterisitcs entity.
 * 
 * @author matt
 * @version 1.0
 */
@Entity
@Table(name = "RESOURCE_CHARS")
public class ResourceCharacteristicsEntity extends BaseLongEntity implements SignableMessage {

  private static final long serialVersionUID = 3836713957221189845L;

  @Basic
  @Column(name = "LOAD_POWER_MAX", nullable = false, insertable = true, updatable = true)
  private Long loadPowerMax;

  @Basic
  @Column(name = "LOAD_POWER_FACTOR", nullable = false, insertable = true, updatable = true)
  private Float loadPowerFactor;

  @Basic
  @Column(name = "SUPPLY_POWER_MAX", nullable = false, insertable = true, updatable = true)
  private Long supplyPowerMax;

  @Basic
  @Column(name = "SUPPLY_POWER_FACTOR", nullable = false, insertable = true, updatable = true)
  private Float supplyPowerFactor;

  @Basic
  @Column(name = "STORAGE_ENERGY_CAP", nullable = false, insertable = true, updatable = true)
  private Long storageEnergyCapacity;

  @Embedded
  @AttributeOverrides({ @AttributeOverride(name = "min", column = @Column(name = "RESP_TIME_MIN")),
      @AttributeOverride(name = "max", column = @Column(name = "RESP_TIME_MAX")) })
  private DurationRangeEmbed responseTime;

  /**
   * Default constructor.
   */
  public ResourceCharacteristicsEntity() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * Construct with creation date.
   * 
   * @param created
   *        the creation date
   */
  public ResourceCharacteristicsEntity(Instant created) {
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
  public ResourceCharacteristicsEntity(Instant created, Long id) {
    super(created, id);
  }

  @Override
  public int signatureMessageBytesSize() {
    return Long.BYTES * 3 + Float.BYTES * 2 + responseTime().signatureMessageBytesSize();
  }

  @Override
  public void addSignatureMessageBytes(ByteBuffer buf) {
    // @formatter:off
    buf.putLong(getLoadPowerMax())
        .putFloat(getLoadPowerFactor())
        .putLong(getSupplyPowerMax())
        .putFloat(getSupplyPowerFactor())
        .putLong(getStorageEnergyCapacity());
    // @formatter:on
    responseTime().addSignatureMessageBytes(buf);
  }

  /**
   * Get the maximum load this resource can demand, in W.
   * 
   * @return the power maximum
   */
  public Long getLoadPowerMax() {
    return loadPowerMax;
  }

  /**
   * Set the maximum load this resource can demand, in W.
   * 
   * @param loadPowerMax
   *        the power maximum to set
   */
  public void setLoadPowerMax(Long loadPowerMax) {
    this.loadPowerMax = loadPowerMax;
  }

  /**
   * Get the expected power factor of load, between -1..1.
   * 
   * @return the power factor
   */
  public Float getLoadPowerFactor() {
    return loadPowerFactor;
  }

  /**
   * Set the maximum supply resource can offer, in W.
   * 
   * @param loadPowerFactor
   *        the loadPowerFactor to set
   */
  public void setLoadPowerFactor(Float loadPowerFactor) {
    this.loadPowerFactor = loadPowerFactor;
  }

  /**
   * Get the maximum supply resource can offer, in W.
   * 
   * @return the power maximum
   */
  public Long getSupplyPowerMax() {
    return supplyPowerMax;
  }

  /**
   * Set the maximum supply resource can offer, in W.
   * 
   * @param supplyPowerMax
   *        the power maximum to set
   */
  public void setSupplyPowerMax(Long supplyPowerMax) {
    this.supplyPowerMax = supplyPowerMax;
  }

  /**
   * Get the expected power factor of supply, between -1..1.
   * 
   * @return the power factor
   */
  public Float getSupplyPowerFactor() {
    return supplyPowerFactor;
  }

  /**
   * Set the expected power factor of supply, between -1..1.
   * 
   * @param supplyPowerFactor
   *        the power factor to set
   */
  public void setSupplyPowerFactor(Float supplyPowerFactor) {
    this.supplyPowerFactor = supplyPowerFactor;
  }

  /**
   * Get the theoretical storage capacity of this resource, in Wh.
   * 
   * @return the capacity
   */
  public Long getStorageEnergyCapacity() {
    return storageEnergyCapacity;
  }

  /**
   * Set the theoretical storage capacity of this resource, in Wh.
   * 
   * @param storageEnergyCapacity
   *        the capacity to set
   */
  public void setStorageEnergyCapacity(Long storageEnergyCapacity) {
    this.storageEnergyCapacity = storageEnergyCapacity;
  }

  /**
   * Get the expected minimum/maximum response time to start/finish executing load or supply
   * changes.
   * 
   * @return the response time
   */
  public DurationRangeEmbed getResponseTime() {
    return responseTime;
  }

  /**
   * Set the expected minimum/maximum response time to start/finish executing load or supply
   * changes.
   * 
   * @param responseTime
   *        the value to set
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

}
