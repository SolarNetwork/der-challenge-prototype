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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import net.solarnetwork.esi.domain.jpa.BaseUuidEntity;
import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
import net.solarnetwork.esi.domain.support.SignableMessage;

/**
 * An offering of a price map, across one or more facilities.
 * 
 * @author matt
 * @version 1.0
 */
@Entity
@Table(name = "PRICE_MAP_OFFERINGS")
public class PriceMapOfferingEntity extends BaseUuidEntity implements SignableMessage {

  private static final long serialVersionUID = 4008724231000362116L;

  // @formatter:off
  @OneToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.ALL,
      orphanRemoval = true)
  @JoinColumn(name = "PRICE_MAP_ID", nullable = false, 
      foreignKey = @ForeignKey(name = "FACILITY_PRICE_MAP_OFFERS_PRICE_MAP_FK"))
  private PriceMapEntity priceMap;
  // @formatter:on

  @Basic
  @Column(name = "START_AT")
  private Instant startDate;

  // CHECKSTYLE IGNORE LineLength FOR NEXT 2 LINES
  @OneToMany(mappedBy = "offering", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<FacilityPriceMapOfferEntity> offers;

  /**
   * Default constructor.
   */
  public PriceMapOfferingEntity() {
    super();
  }

  /**
   * Construct with creation date.
   * 
   * @param created
   *        the creation date
   */
  public PriceMapOfferingEntity(Instant created) {
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
  public PriceMapOfferingEntity(Instant created, UUID id) {
    super(created, id);
  }

  @Override
  public int signatureMessageBytesSize() {
    PriceMapEmbed pm = (priceMap != null ? priceMap.getPriceMap() : null);
    if (pm == null) {
      pm = new PriceMapEmbed();
    }
    return Long.BYTES * 2 + pm.signatureMessageBytesSize();
  }

  @Override
  public void addSignatureMessageBytes(ByteBuffer buf) {
    UUID offerId = (priceMap != null ? priceMap.getId() : null);
    SignableMessage.addUuidSignatureMessageBytes(buf, offerId);
    PriceMapEmbed pm = (priceMap != null ? priceMap.getPriceMap() : new PriceMapEmbed());
    pm.addSignatureMessageBytes(buf);
  }

  /**
   * Get the price map details.
   * 
   * <p>
   * This price map represents the original price map offered to all facilities. Each facility is
   * free to propose a counter-offer, so the actual price map actually "active" for a facility is
   * defined by {@link FacilityPriceMapOfferEntity#getPriceMap()}.
   * </p>
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
      pm = new PriceMapEntity(Instant.now());
      setPriceMap(pm);
    }
    return pm;
  }

  /**
   * Get the offer start date.
   * 
   * @return the start date
   */
  public Instant getStartDate() {
    return startDate;
  }

  /**
   * Set the offer start date.
   * 
   * @param startDate
   *        the date to set
   */
  public void setStartDate(Instant startDate) {
    this.startDate = startDate;
  }

  /**
   * Get the facility offers.
   * 
   * @return the offers
   */
  public Set<FacilityPriceMapOfferEntity> getOffers() {
    return offers;
  }

  /**
   * Set the facility offers.
   * 
   * @param offers
   *        the offers to set
   */
  public void setOffers(Set<FacilityPriceMapOfferEntity> offers) {
    this.offers = offers;
  }

  /**
   * Add an offer.
   * 
   * @param offer
   *        the offer to add
   */
  public void addOffer(FacilityPriceMapOfferEntity offer) {
    Set<FacilityPriceMapOfferEntity> set = getOffers();
    if (set == null) {
      set = new HashSet<>(4);
      setOffers(set);
    }
    if (offer.getOffering() != this) {
      offer.setOffering(this);
    }
    set.add(offer);
  }

  /**
   * Remove an offer from this offering.
   * 
   * @param offer
   *        the offer to remove
   * @return {@literal true} if the offer was present and removed
   */
  public boolean removeOffer(FacilityPriceMapOfferEntity offer) {
    Set<FacilityPriceMapOfferEntity> set = getOffers();
    boolean result = false;
    if (set != null) {
      result = set.remove(offer);
      if (result) {
        offer.setOffering(null);
      }
    }
    return result;
  }
}
