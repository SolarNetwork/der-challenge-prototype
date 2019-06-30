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

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import net.solarnetwork.esi.domain.DerCharacteristicsOrBuilder;
import net.solarnetwork.esi.domain.jpa.BaseEntity;
import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;

/**
 * Facility-specific resource characteristics entity.
 * 
 * @author matt
 * @version 1.0
 */
@Entity
@Table(name = "FACILITY_RESOURCE_CHARS")
public class FacilityResourceCharacteristicsEntity extends BaseEntity<UUID> {

  // NOTE that this class does NOT extend BaseUuidEntity so that it can override the @Id using
  // the @MapsId on the one-to-one facility relationship

  private static final long serialVersionUID = 4828967993081419913L;

  @Id
  private UUID id;

  // CHECKSTYLE IGNORE LineLength FOR NEXT 4 LINES
  @OneToOne(optional = false)
  @MapsId
  @JoinColumn(name = "FACILITY_ID", nullable = false, foreignKey = @ForeignKey(name = "FACILITY_RESOURCE_CHARS_FACILITY_FK"))
  private FacilityEntity facility;

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

  // CHECKSTYLE IGNORE LineLength FOR NEXT 4 LINES
  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "min", column = @Column(name = "RESP_TIME_MIN", nullable = false)),
      @AttributeOverride(name = "max", column = @Column(name = "RESP_TIME_MAX", nullable = false)) })
  private DurationRangeEmbed responseTime;

  /**
   * Default constructor.
   */
  public FacilityResourceCharacteristicsEntity() {
    super();
  }

  /**
   * Construct with creation date.
   * 
   * @param created
   *        the creation date
   */
  public FacilityResourceCharacteristicsEntity(Instant created) {
    super(created);
  }

  /**
   * Construct with values.
   * 
   * @param created
   *        the creation date
   * @param facility
   *        the facility
   */
  public FacilityResourceCharacteristicsEntity(Instant created, FacilityEntity facility) {
    super(created);
    setFacility(facility);
  }

  /**
   * Create a resource characteristics entity out of a source message.
   * 
   * @param message
   *        the message to copy the properties from
   * @return the new entity
   */
  public static FacilityResourceCharacteristicsEntity entityForMessage(
      DerCharacteristicsOrBuilder message) {
    FacilityResourceCharacteristicsEntity entity = new FacilityResourceCharacteristicsEntity(
        Instant.now());
    entity.populateFromMessage(message);
    return entity;
  }

  /**
   * Update the properties of this object from equivalent properties in a source message.
   * 
   * @param message
   *        the message to copy the properties from
   */
  public void populateFromMessage(DerCharacteristicsOrBuilder message) {
    setLoadPowerMax(message.getLoadPowerMax());
    setLoadPowerFactor(message.getLoadPowerFactor());
    setSupplyPowerMax(message.getSupplyPowerMax());
    setSupplyPowerFactor(message.getSupplyPowerFactor());
    setStorageEnergyCapacity(message.getStorageEnergyCapacity());
    setResponseTime(new DurationRangeEmbed(
        Duration.ofSeconds(message.getResponseTime().getMin().getSeconds(),
            message.getResponseTime().getMin().getNanos()),
        Duration.ofSeconds(message.getResponseTime().getMax().getSeconds(),
            message.getResponseTime().getMax().getNanos())));
  }

  /**
   * Encode this entity as a byte array suitable for using as message signature data.
   * 
   * @return the bytes
   */
  @Nonnull
  public byte[] toSignatureBytes() {
    // @formatter:off
    ByteBuffer bb = ByteBuffer.allocate(64)
        .putLong(getLoadPowerMax())
        .putFloat(getLoadPowerFactor())
        .putLong(getSupplyPowerMax())
        .putFloat(getSupplyPowerFactor())
        .putLong(getStorageEnergyCapacity())
        .putLong(getResponseTime().getMin().getSeconds())
        .putLong(getResponseTime().getMin().getNano())
        .putLong(getResponseTime().getMax().getSeconds())
        .putLong(getResponseTime().getMax().getNano());
    // @formatter:on
    bb.flip();
    byte[] bytes = new byte[bb.limit()];
    bb.get(bytes);
    return bytes;
  }

  @Override
  public UUID getId() {
    return id;
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
   * Get the associated facility.
   * 
   * @return the facility the facility
   */
  public FacilityEntity getFacility() {
    return facility;
  }

  /**
   * Set the associated facility.
   * 
   * @param facility
   *        the facility to set
   */
  public void setFacility(FacilityEntity facility) {
    this.facility = facility;
  }

}
