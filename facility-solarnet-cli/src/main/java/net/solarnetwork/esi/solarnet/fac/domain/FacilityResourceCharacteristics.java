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

package net.solarnetwork.esi.solarnet.fac.domain;

import java.nio.ByteBuffer;
import java.util.Optional;

import javax.annotation.Nonnull;

import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.domain.jpa.ResourceCharacteristicsEmbed;
import net.solarnetwork.esi.domain.support.BaseIdentity;
import net.solarnetwork.esi.domain.support.SignableMessage;

/**
 * Facility resource characteristics.
 * 
 * @author matt
 * @version 1.0
 */
public class FacilityResourceCharacteristics extends BaseIdentity<String>
    implements SignableMessage, SolarNodeMetadataEntity {

  private static final long serialVersionUID = 8191295041648828467L;

  private Long nodeId;
  private ResourceCharacteristicsEmbed characteristics;

  /**
   * Default constructor.
   */
  public FacilityResourceCharacteristics() {
    super();
  }

  /**
   * Construct with values.
   * 
   * @param id
   *        the primary key
   */
  public FacilityResourceCharacteristics(String id) {
    super(id);
  }

  /**
   * Construct with creation date.
   * 
   * @param id
   *        the primary key
   * @param characteristics
   *        the characteristics
   */
  public FacilityResourceCharacteristics(String id, ResourceCharacteristicsEmbed characteristics) {
    super(id);
    this.characteristics = characteristics;
  }

  @Override
  public int signatureMessageBytesSize() {
    return characteristics().signatureMessageBytesSize();
  }

  @Override
  public void addSignatureMessageBytes(ByteBuffer buf) {
    characteristics().addSignatureMessageBytes(buf);
  }

  /**
   * Get the node ID.
   * 
   * @return the nodeId
   */
  public Long getNodeId() {
    return nodeId;
  }

  /**
   * Set the node ID.
   * 
   * @param nodeId
   *        the nodeId to set
   */
  @Override
  public void setNodeId(Long nodeId) {
    this.nodeId = nodeId;
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
