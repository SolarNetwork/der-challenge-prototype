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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import net.solarnetwork.esi.domain.jpa.BaseUuidEntity;

/**
 * A price map offer entity for a specific facility.
 * 
 * @author matt
 * @version 1.0
 */
@Entity
@Table(name = "FACILITY_PRICE_MAP_OFFERS")
public class FacilityPriceMapOfferEntity extends BaseUuidEntity {

  private static final long serialVersionUID = 5090046078033013977L;

  // @formatter:off
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "FACILITY_ID", nullable = false, insertable = true, updatable = false,
      foreignKey = @ForeignKey(name = "FACILITY_PRICE_MAP_OFFERS_FACILITY_FK"))
  private FacilityEntity facility;
  // @formatter:on

  // @formatter:off
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFERING_ID", nullable = false, insertable = true, updatable = false,
      foreignKey = @ForeignKey(name = "FACILITY_PRICE_MAP_OFFERS_OFFERING_FK"))
  private PriceMapOfferingEntity offering;
  // @on

  @Basic
  @Column(name = "IS_ACCEPTED", nullable = false, insertable = true, updatable = true)
  private boolean accepted;

  @Basic
  @Column(name = "IS_CONFIRMED", nullable = false, insertable = true, updatable = true)
  private boolean confirmed;

  // @formatter:off
  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "PRICE_MAP_ID", nullable = true, 
      foreignKey = @ForeignKey(name = "FACILITY_PRICE_MAP_OFFERS_PRICE_MAP_FK"))
  private PriceMapEntity priceMap;
  // @formatter:on

  /**
   * Default constructor.
   */
  public FacilityPriceMapOfferEntity() {
    super();
  }

  /**
   * Construct with creation date.
   * 
   * @param created
   *        the creation date
   */
  public FacilityPriceMapOfferEntity(Instant created) {
    super(created);
  }

  /**
   * Construct with creation date and ID.
   * 
   * @param created
   *        the creation date
   * @param id
   *        the ID
   */
  public FacilityPriceMapOfferEntity(Instant created, UUID id) {
    super(created, id);
  }

  /**
   * Construct with values.
   * 
   * @param created
   *        the creation date
   * @param facility
   *        the facility
   */
  public FacilityPriceMapOfferEntity(Instant created, FacilityEntity facility) {
    super(created);
    setFacility(facility);
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
   * Get the price map offering.
   * 
   * @return the offering
   */
  public PriceMapOfferingEntity getOffering() {
    return offering;
  }

  /**
   * Set the price map offering.
   * 
   * @param offering
   *        the offering to set
   */
  public void setOffering(PriceMapOfferingEntity offering) {
    this.offering = offering;
  }

  /**
   * Get the price map details.
   * 
   * @return the price map details
   */
  public PriceMapEntity getPriceMap() {
    return priceMap;
  }

  /**
   * Set the price map details.
   * 
   * @param priceMap
   *        the price map details to set
   */
  public void setPriceMap(PriceMapEntity priceMap) {
    this.priceMap = priceMap;
  }

  /**
   * Get an optional of the price map details.
   * 
   * @return the optional price map details
   */
  public Optional<PriceMapEntity> priceMapOpt() {
    return Optional.ofNullable(getPriceMap());
  }

  /**
   * Get the price map details, creating a new one if it doesn't already exist.
   * 
   * @return the price map details
   */
  @Nonnull
  public PriceMapEntity priceMap() {
    PriceMapEntity pm = getPriceMap();
    if (pm == null) {
      pm = new PriceMapEntity();
      setPriceMap(pm);
    }
    return pm;
  }

  /**
   * Get the accepted flag.
   * 
   * @return {@literal true} if the facility has accepted the offer
   */
  public boolean isAccepted() {
    return accepted;
  }

  /**
   * Set the accepted flag.
   * 
   * @param accepted
   *        the accepted to set
   */
  public void setAccepted(boolean accepted) {
    this.accepted = accepted;
  }

  /**
   * Get the confirmed flag.
   * 
   * @return {@literal true} if the facility has confirmed the offer
   */
  public boolean isConfirmed() {
    return confirmed;
  }

  /**
   * Set the confirmed flag.
   * 
   * @param confirmed
   *        the confirmed to set
   */
  public void setConfirmed(boolean confirmed) {
    this.confirmed = confirmed;
  }

}
