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

import javax.annotation.Nonnull;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

import net.solarnetwork.esi.domain.support.Cloning;
import net.solarnetwork.esi.domain.support.SignableMessage;

/**
 * Embeddable DER characteristic details.
 * 
 * @author matt
 * @version 1.0
 */
@Embeddable
public class ResourceCharacteristicsEmbed
    implements SignableMessage, Cloning<ResourceCharacteristicsEmbed> {

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
  public ResourceCharacteristicsEmbed() {
    super();
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

  @Override
  public ResourceCharacteristicsEmbed copy() {
    ResourceCharacteristicsEmbed c = new ResourceCharacteristicsEmbed();
    c.setLoadPowerMax(getLoadPowerMax());
    c.setLoadPowerFactor(getLoadPowerFactor());
    c.setSupplyPowerMax(getSupplyPowerMax());
    c.setSupplyPowerFactor(getSupplyPowerFactor());
    c.setStorageEnergyCapacity(getStorageEnergyCapacity());

    DurationRangeEmbed d = getResponseTime();
    if (d != null) {
      c.setResponseTime(d.copy());
    }
    return c;
  }

  @Override
  public int hashCode() {
    return Objects.hash(loadPowerFactor, loadPowerMax, responseTime, storageEnergyCapacity,
        supplyPowerFactor, supplyPowerMax);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof ResourceCharacteristicsEmbed)) {
      return false;
    }
    ResourceCharacteristicsEmbed other = (ResourceCharacteristicsEmbed) obj;
    return Objects.equals(loadPowerFactor, other.loadPowerFactor)
        && Objects.equals(loadPowerMax, other.loadPowerMax)
        && Objects.equals(responseTime, other.responseTime)
        && Objects.equals(storageEnergyCapacity, other.storageEnergyCapacity)
        && Objects.equals(supplyPowerFactor, other.supplyPowerFactor)
        && Objects.equals(supplyPowerMax, other.supplyPowerMax);
  }

  @Override
  public String toString() {
    return "ResourceCharacteristics{loadPowerMax=" + loadPowerMax + ", loadPowerFactor="
        + loadPowerFactor + ", supplyPowerMax=" + supplyPowerMax + ", supplyPowerFactor="
        + supplyPowerFactor + ", storageEnergyCapacity=" + storageEnergyCapacity + ", responseTime="
        + responseTime + "}";
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
   * Get the maximum load this resource can demand, in W.
   * 
   * @return the power maximum, or {@literal 0} if not set
   */
  @Nonnull
  public Long loadPowerMax() {
    Long v = getLoadPowerMax();
    return (v != null ? v : 0L);
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
   * Get the expected power factor of load, between -1..1.
   * 
   * @return the power factor, or {@literal 0} if not set
   */
  @Nonnull
  public Float loadPowerFactor() {
    Float v = getLoadPowerFactor();
    return (v != null ? v : 0f);
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
   * Get the maximum supply resource can offer, in W.
   * 
   * @return the power maximum, or {@literal 0} if not set
   */
  @Nonnull
  public Long supplyPowerMax() {
    Long v = getSupplyPowerMax();
    return (v != null ? v : 0L);
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
   * Get the expected power factor of supply, between -1..1.
   * 
   * @return the power factor, or {@literal 0} if not set
   */
  @Nonnull
  public Float supplyPowerFactor() {
    Float v = getSupplyPowerFactor();
    return (v != null ? v : 0f);
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
   * Get the theoretical storage capacity of this resource, in Wh.
   * 
   * @return the capacity, or {@literal 0} if not set
   */
  @Nonnull
  public Long storageEnergyCapacity() {
    Long v = getStorageEnergyCapacity();
    return (v != null ? v : 0L);
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
