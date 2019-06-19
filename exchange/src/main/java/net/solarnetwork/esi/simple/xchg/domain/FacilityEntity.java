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
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import net.solarnetwork.esi.domain.BaseUuidEntity;

/**
 * Entity for DER facilities that have been registered.
 * 
 * @author matt
 * @version 1.0
 */
@Entity
@Table(name = "FACILITIES")
public class FacilityEntity extends BaseUuidEntity {

  private static final long serialVersionUID = 1583057782830681381L;

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
  private String facilityEndpoint;

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
  public String getFacilityEndpoint() {
    return facilityEndpoint;
  }

  /**
   * Get the facility gRPC endpoint URI.
   * 
   * @param facilityEndpoint
   *        the endpoint URL to use
   */
  public void setFacilityEndpoint(String facilityEndpoint) {
    this.facilityEndpoint = facilityEndpoint;
  }

}
