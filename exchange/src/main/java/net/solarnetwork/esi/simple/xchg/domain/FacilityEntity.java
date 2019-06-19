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

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import net.solarnetwork.esi.domain.BaseUuidEntity;

/**
 * Entity for DER facilities.
 * 
 * @author matt
 * @version 1.0
 */
@Entity
@Table(name = "FACILITIES")
@Access(AccessType.FIELD)
public class FacilityEntity extends BaseUuidEntity {

  private static final long serialVersionUID = 813555757856502135L;

  @Basic
  @Column(name = "UICI", nullable = false, insertable = true, updatable = true, length = 20)
  private String uici;

  @Basic
  @Column(name = "CUST_ID", nullable = false, insertable = true, updatable = true, length = 20)
  private String customerId;

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

}
