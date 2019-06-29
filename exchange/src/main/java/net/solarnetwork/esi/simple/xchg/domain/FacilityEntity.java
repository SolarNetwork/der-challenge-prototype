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

import java.security.PublicKey;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import net.solarnetwork.esi.domain.BaseUuidEntity;
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
public class FacilityEntity extends BaseUuidEntity {

  private static final long serialVersionUID = -4450594216630145370L;

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
    Set<String> tags = getProgramTypes();
    if (tags == null) {
      tags = new HashSet<>(4);
      setProgramTypes(tags);
    }
    tags.add(type);
  }

  /**
   * Remove a program type.
   * 
   * @param type
   *        the type to remove
   */
  public void removeProgramType(String type) {
    Set<String> tags = getProgramTypes();
    if (tags != null) {
      tags.remove(type);
    }
  }

}
