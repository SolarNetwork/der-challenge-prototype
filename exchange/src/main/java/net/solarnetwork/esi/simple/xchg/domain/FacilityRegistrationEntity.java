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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import net.solarnetwork.esi.domain.BaseLongEntity;

/**
 * Entity for DER facilities undergoing registration.
 * 
 * <p>
 * This is a short-lived entity, in that once registration is complete this entity is discarded.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
@Entity
@Table(name = "FACILITY_REGS")
public class FacilityRegistrationEntity extends BaseLongEntity {

  private static final long serialVersionUID = -2134611054441693836L;

  @Basic
  @Column(name = "UICI", nullable = false, insertable = true, updatable = false, length = 20)
  private String uici;

  @Basic
  @Column(name = "CUST_ID", nullable = false, insertable = true, updatable = false, length = 20)
  private String customerId;

  @Basic
  @Column(name = "FAC_URI", nullable = false, insertable = true, updatable = false, length = 255)
  private String facilityEndpoint;

  @Basic
  @Column(name = "FAC_NONCE", nullable = false, insertable = true, updatable = false, length = 24)
  private byte[] facilityNonce;

  @Basic
  @Column(name = "OP_NONCE", nullable = false, insertable = true, updatable = false, length = 24)
  private byte[] operatorNonce;

  /**
   * Default constructor.
   */
  public FacilityRegistrationEntity() {
    super();
  }

  /**
   * Construct with creation date.
   * 
   * @param created
   *        the creation date
   */
  public FacilityRegistrationEntity(Instant created) {
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
  public FacilityRegistrationEntity(Instant created, Long id) {
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

  /**
   * Set the facility nonce value.
   * 
   * @return the nonce
   */
  public byte[] getFacilityNonce() {
    return facilityNonce;
  }

  /**
   * Get the facility nonce value.
   * 
   * @param facilityNonce
   *        the nonce
   */
  public void setFacilityNonce(byte[] facilityNonce) {
    this.facilityNonce = facilityNonce;
  }

  /**
   * Set the operator nonce value.
   * 
   * @return the nonce
   */
  public byte[] getOperatorNonce() {
    return operatorNonce;
  }

  /**
   * Get the operator nonce value.
   * 
   * @param operatorNonce
   *        the nonce
   */
  public void setOperatorNonce(byte[] operatorNonce) {
    this.operatorNonce = operatorNonce;
  }

}
