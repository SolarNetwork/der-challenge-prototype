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

package net.solarnetwork.esi.simple.fac.domain;

import java.time.Instant;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import net.solarnetwork.esi.domain.jpa.BaseStringEntity;

/**
 * A facility exchange registration entity.
 * 
 * @author matt
 * @version 1.0
 */
@Entity
@Table(name = "EXCHANGE_REGS")
public class ExchangeRegistrationEntity extends BaseStringEntity {

  private static final long serialVersionUID = 7346580327458815972L;

  @Basic
  @Column(name = "EXCH_URI", nullable = false, insertable = true, updatable = false, length = 255)
  private String exchangeEndpointUri;

  @Basic
  @Column(name = "EXCH_KEY", nullable = false, insertable = true, updatable = false, length = 255)
  private byte[] exchangePublicKey;

  @Basic
  @Column(name = "EXCH_NONCE", nullable = false, insertable = true, updatable = false, length = 24)
  private byte[] exchangeNonce;

  @Basic
  @Column(name = "FAC_NONCE", nullable = false, insertable = true, updatable = false, length = 24)
  private byte[] facilityNonce;

  /**
   * Default constructor.
   */
  public ExchangeRegistrationEntity() {
    super();
  }

  /**
   * Construct with creation date.
   * 
   * @param created
   *        the creation date
   */
  public ExchangeRegistrationEntity(Instant created) {
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
  public ExchangeRegistrationEntity(Instant created, String id) {
    super(created, id);
  }

  /**
   * Get the exchange gRPC URI.
   * 
   * @return the exchangeEndpointUri the URI
   */
  public String getExchangeEndpointUri() {
    return exchangeEndpointUri;
  }

  /**
   * Set the exchange gRPC URI.
   * 
   * @param exchangeEndpointUri
   *        the exchangeEndpointUri to set
   */
  public void setExchangeEndpointUri(String exchangeEndpointUri) {
    this.exchangeEndpointUri = exchangeEndpointUri;
  }

  /**
   * Get the exchange public key.
   * 
   * @return the exchangePublicKey the public key data
   */
  public byte[] getExchangePublicKey() {
    return exchangePublicKey;
  }

  /**
   * Set the exchange public key.
   * 
   * @param exchangePublicKey
   *        the exchangePublicKey to set
   */
  public void setExchangePublicKey(byte[] exchangePublicKey) {
    this.exchangePublicKey = exchangePublicKey;
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
   * Set the exchange nonce value.
   * 
   * @return the nonce
   */
  public byte[] getExchangeNonce() {
    return exchangeNonce;
  }

  /**
   * Get the exchange nonce value.
   * 
   * @param exchangeNonce
   *        the nonce
   */
  public void setExchangeNonce(byte[] exchangeNonce) {
    this.exchangeNonce = exchangeNonce;
  }
}
