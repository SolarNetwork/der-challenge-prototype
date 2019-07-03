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

import java.net.URI;
import java.security.PublicKey;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import net.solarnetwork.esi.domain.jpa.BaseUuidEntity;
import net.solarnetwork.esi.util.CryptoUtils;

/**
 * Entity for DER facilities that have been registered.
 * 
 * <p>
 * This entity is modeled with UUID primary keys, which are not to be confused with
 * {@link #getFacilityUid()} values, which are provided by facilities themselves and not required to
 * be UUID values.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
@Entity
@Table(name = "FACILITIES", indexes = { @Index(name = "FAC_UID_IDX", columnList = "FAC_UID") })
public class FacilityEntity extends BaseUuidEntity implements FacilityInfo {

  private static final long serialVersionUID = -4777273455189387417L;

  @Basic
  @Column(name = "UICI", nullable = false, insertable = true, updatable = true, length = 20)
  private String uici;

  @Basic
  @Column(name = "CUST_ID", nullable = false, insertable = true, updatable = true, length = 20)
  private String customerId;

  @Basic
  @Column(name = "FAC_UID", nullable = false, insertable = true, updatable = false, length = 255)
  private String facilityUid;

  @Basic
  @Column(name = "FAC_URI", nullable = false, insertable = true, updatable = true, length = 255)
  private String facilityEndpointUri;

  @Basic
  @Column(name = "FAC_KEY", nullable = false, insertable = true, updatable = true, length = 255)
  private byte[] facilityPublicKey;

  // @formatter:off
  @ElementCollection(fetch = FetchType.EAGER)
  @Column(name = "PROGRAM", nullable = false, length = 64)
  @CollectionTable(name = "FACILITY_PROGRAM_TYPES", 
      joinColumns = @JoinColumn(name = "FACILITY_ID", nullable = false), 
      foreignKey = @ForeignKey(name = "FACILITY_PROGRAM_TYPES_FACILITY_FK"),
      uniqueConstraints = @UniqueConstraint(name = "FACILITY_PROGRAM_TYPES_PK",
          columnNames = { "FACILITY_ID", "PROGRAM" }))
  private Set<String> programTypes;
  // @formatter:on

  // @formatter:off
  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinTable(name = "FACILITY_PRICE_MAPS",
      joinColumns = @JoinColumn(name = "FACILITY_ID", referencedColumnName = "ID",
          foreignKey = @ForeignKey(name = "FACILITY_PRICE_MAPS_FACILITY_FK")),
      inverseJoinColumns = @JoinColumn(name = "PRICE_MAP_ID", referencedColumnName = "ID",
          foreignKey = @ForeignKey(name = "FACILITY_PRICE_MAPS_PRICE_MAP_FK")),
      uniqueConstraints = @UniqueConstraint(name = "FACILITY_PRICE_MAPS_PRICE_MAP_UNQ",
          columnNames = "PRICE_MAP_ID"))
  private Set<PriceMapEntity> priceMaps;
  // @formatter:on

  /**
   * Default constructor.
   */
  public FacilityEntity() {
    super();
  }

  /**
   * Construct with creation date.
   * 
   * @param created
   *        the creation date
   */
  public FacilityEntity(Instant created) {
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
  public FacilityEntity(Instant created, UUID id) {
    super(created, id);
  }

  /**
   * Get the facility {@link PublicKey}.
   * 
   * <p>
   * This derives the instance from the {@link #getFacilityPublicKey()} data.
   * </p>
   * 
   * @return the public key
   */
  public PublicKey publicKey() {
    byte[] pk = getFacilityPublicKey();
    if (pk == null) {
      return null;
    }
    return CryptoUtils.decodePublicKey(CryptoUtils.STANDARD_HELPER, pk);
  }

  /**
   * Get the Utility Interconnection Customer Identifier.
   * 
   * @return the UICI
   */
  @Override
  public String getUici() {
    return uici;
  }

  /**
   * Set the Utility Interconnection Customer Identifier.
   * 
   * @param uici
   *        the UICI to use
   */
  public void setUici(String uici) {
    this.uici = uici;
  }

  /**
   * Get the customer ID.
   * 
   * @return the customer ID
   */
  @Override
  public String getCustomerId() {
    return customerId;
  }

  /**
   * Set the customer ID.
   * 
   * @param customerId
   *        the customer ID to use
   */
  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  /**
   * Get the facility UID.
   * 
   * @return the facility UID
   */
  @Override
  public String getFacilityUid() {
    return facilityUid;
  }

  /**
   * Set the facility UID.
   * 
   * @param facilityUid
   *        the facility UID to use
   */
  public void setFacilityUid(String facilityUid) {
    this.facilityUid = facilityUid;
  }

  /**
   * Set the facility gRPC endpoint URI.
   * 
   * @return the facility endpoint URI
   */
  public String getFacilityEndpointUri() {
    return facilityEndpointUri;
  }

  /**
   * Get the facility gRPC endpoint URI.
   * 
   * @param facilityEndpointUri
   *        the endpoint URL to use
   */
  public void setFacilityEndpointUri(String facilityEndpointUri) {
    this.facilityEndpointUri = facilityEndpointUri;
  }

  /**
   * Get a URI from the facility endpoint URI value.
   * 
   * @return the URI
   */
  public URI facilityUri() {
    return URI.create(getFacilityEndpointUri());
  }

  /**
   * Get the facility public key.
   * 
   * @return the facility public key
   */
  public byte[] getFacilityPublicKey() {
    return facilityPublicKey;
  }

  /**
   * Set the facility public key.
   * 
   * @param facilityPublicKey
   *        the facility public key
   */
  public void setFacilityPublicKey(byte[] facilityPublicKey) {
    this.facilityPublicKey = facilityPublicKey;
  }

  /**
   * Get the program types.
   * 
   * @return the program types
   */
  public Set<String> getProgramTypes() {
    return programTypes;
  }

  /**
   * Set the program types.
   * 
   * @param programTypes
   *        the program types to set
   */
  public void setProgramTypes(Set<String> programTypes) {
    this.programTypes = programTypes;
  }

  /**
   * Add a program type.
   * 
   * @param type
   *        the type to add
   */
  public void addProgramType(String type) {
    Set<String> set = getProgramTypes();
    if (set == null) {
      set = new HashSet<>(4);
      setProgramTypes(set);
    }
    set.add(type);
  }

  /**
   * Remove a program type.
   * 
   * @param type
   *        the type to remove
   */
  public void removeProgramType(String type) {
    Set<String> set = getProgramTypes();
    if (set != null) {
      set.remove(type);
    }
  }

  /**
   * Get the price map.
   * 
   * @return the price maps, or {@literal null} if not available
   */
  public Set<PriceMapEntity> getPriceMaps() {
    return priceMaps;
  }

  /**
   * SEt the price map.
   * 
   * @param priceMaps
   *        the price maps to set
   */
  public void setPriceMaps(Set<PriceMapEntity> priceMaps) {
    this.priceMaps = priceMaps;
  }

  /**
   * Add a price map.
   * 
   * @param priceMap
   *        the price map to add
   */
  public void addPriceMap(PriceMapEntity priceMap) {
    Set<PriceMapEntity> set = getPriceMaps();
    if (set == null) {
      set = new HashSet<>(4);
      setPriceMaps(set);
    }
    set.add(priceMap);
  }

  /**
   * Remove a price map.
   * 
   * @param priceMap
   *        the price map to remove
   */
  public void removePriceMap(PriceMapEntity priceMap) {
    Set<PriceMapEntity> set = getPriceMaps();
    if (set != null) {
      set.remove(priceMap);
    }
  }

  /**
   * Remove all price maps from this facility.
   */
  public void clearPriceMaps() {
    Set<PriceMapEntity> set = getPriceMaps();
    if (set == null || set.isEmpty()) {
      return;
    }
    set.clear();
  }

}
