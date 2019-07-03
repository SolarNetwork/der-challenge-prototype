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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
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
import net.solarnetwork.esi.domain.jpa.ResourceCharacteristicsEmbed;
import net.solarnetwork.esi.domain.support.ProtobufUtils;
import net.solarnetwork.esi.domain.support.SignableMessage;

/**
 * Facility-specific resource characteristics entity.
 * 
 * @author matt
 * @version 1.0
 */
@Entity
@Table(name = "FACILITY_RESOURCE_CHARS")
public class FacilityResourceCharacteristicsEntity extends BaseEntity<UUID>
    implements SignableMessage {

  // NOTE that this class does NOT extend BaseUuidEntity so that it can override the @Id using
  // the @MapsId on the one-to-one facility relationship

  private static final long serialVersionUID = 643497212720367137L;

  @Id
  private UUID id;

  // CHECKSTYLE IGNORE LineLength FOR NEXT 4 LINES
  @OneToOne(optional = false)
  @MapsId
  @JoinColumn(name = "FACILITY_ID", nullable = false, foreignKey = @ForeignKey(name = "FACILITY_RESOURCE_CHARS_FACILITY_FK"))
  private FacilityEntity facility;

  @Embedded
  private ResourceCharacteristicsEmbed characteristics;

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
    setResponseTime(
        new DurationRangeEmbed(ProtobufUtils.durationValue(message.getResponseTime().getMin()),
            ProtobufUtils.durationValue(message.getResponseTime().getMax())));
  }

  @Override
  public int signatureMessageBytesSize() {
    return characteristics().signatureMessageBytesSize();
  }

  @Override
  public void addSignatureMessageBytes(ByteBuffer buf) {
    characteristics().addSignatureMessageBytes(buf);
  }

  @Override
  public UUID getId() {
    return id;
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

  /**
   * Get the resource characteristics.
   * 
   * @return the characteristics
   */
  public ResourceCharacteristicsEmbed getCharacteristics() {
    return characteristics;
  }

  /**
   * Set the resource characteristics.
   * 
   * @param characteristics
   *        the characteristics to set
   */
  public void setCharacteristics(ResourceCharacteristicsEmbed characteristics) {
    this.characteristics = characteristics;
  }

  /**
   * Get an optional of the characteristic details.
   * 
   * @return the optional characteristic details
   */
  @Nonnull
  public Optional<ResourceCharacteristicsEmbed> characteristicsOpt() {
    return Optional.ofNullable(getCharacteristics());
  }

  /**
   * Get the resource characteristics, creating a new one if it doesn't already exist.
   * 
   * @return the characteristic details
   */
  @Nonnull
  public ResourceCharacteristicsEmbed characteristics() {
    ResourceCharacteristicsEmbed pm = getCharacteristics();
    if (pm == null) {
      pm = new ResourceCharacteristicsEmbed();
      setCharacteristics(pm);
    }
    return pm;
  }

  /**
   * Get the maximum load this resource can demand, in W.
   * 
   * @return the power maximum
   */
  public Long getLoadPowerMax() {
    return characteristicsOpt().map(ResourceCharacteristicsEmbed::getLoadPowerMax).orElse(null);
  }

  /**
   * Set the maximum load this resource can demand, in W.
   * 
   * @param loadPowerMax
   *        the power maximum to set
   */
  public void setLoadPowerMax(Long loadPowerMax) {
    characteristics().setLoadPowerMax(loadPowerMax);
  }

  /**
   * Get the expected power factor of load, between -1..1.
   * 
   * @return the power factor
   */
  public Float getLoadPowerFactor() {
    return characteristicsOpt().map(ResourceCharacteristicsEmbed::getLoadPowerFactor).orElse(null);
  }

  /**
   * Set the maximum supply resource can offer, in W.
   * 
   * @param loadPowerFactor
   *        the loadPowerFactor to set
   */
  public void setLoadPowerFactor(Float loadPowerFactor) {
    characteristics().setLoadPowerFactor(loadPowerFactor);
  }

  /**
   * Get the maximum supply resource can offer, in W.
   * 
   * @return the power maximum
   */
  public Long getSupplyPowerMax() {
    return characteristicsOpt().map(ResourceCharacteristicsEmbed::getSupplyPowerMax).orElse(null);
  }

  /**
   * Set the maximum supply resource can offer, in W.
   * 
   * @param supplyPowerMax
   *        the power maximum to set
   */
  public void setSupplyPowerMax(Long supplyPowerMax) {
    characteristics().setSupplyPowerMax(supplyPowerMax);
  }

  /**
   * Get the expected power factor of supply, between -1..1.
   * 
   * @return the power factor
   */
  public Float getSupplyPowerFactor() {
    return characteristicsOpt().map(ResourceCharacteristicsEmbed::getSupplyPowerFactor)
        .orElse(null);
  }

  /**
   * Set the expected power factor of supply, between -1..1.
   * 
   * @param supplyPowerFactor
   *        the power factor to set
   */
  public void setSupplyPowerFactor(Float supplyPowerFactor) {
    characteristics().setSupplyPowerFactor(supplyPowerFactor);
  }

  /**
   * Get the theoretical storage capacity of this resource, in Wh.
   * 
   * @return the capacity
   */
  public Long getStorageEnergyCapacity() {
    return characteristicsOpt().map(ResourceCharacteristicsEmbed::getStorageEnergyCapacity)
        .orElse(null);
  }

  /**
   * Set the theoretical storage capacity of this resource, in Wh.
   * 
   * @param storageEnergyCapacity
   *        the capacity to set
   */
  public void setStorageEnergyCapacity(Long storageEnergyCapacity) {
    characteristics().setStorageEnergyCapacity(storageEnergyCapacity);
  }

  /**
   * Get the expected minimum/maximum response time to start/finish executing load or supply
   * changes.
   * 
   * @return the response time
   */
  public DurationRangeEmbed getResponseTime() {
    return characteristicsOpt().map(ResourceCharacteristicsEmbed::getResponseTime).orElse(null);
  }

  /**
   * Set the expected minimum/maximum response time to start/finish executing load or supply
   * changes.
   * 
   * @param responseTime
   *        the value to set
   */
  public void setResponseTime(DurationRangeEmbed responseTime) {
    characteristics().setResponseTime(responseTime);
  }

}
