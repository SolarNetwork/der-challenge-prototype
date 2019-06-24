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

import net.solarnetwork.esi.domain.BaseStringEntity;

/**
 * A facility exchange entity.
 * 
 * @author matt
 * @version 1.0
 */
@Entity
@Table(name = "EXCHANGES")
public class ExchangeEntity extends BaseStringEntity {

  private static final long serialVersionUID = 5971284405373027212L;

  @Basic
  @Column(name = "EXCH_URI", nullable = false, insertable = true, updatable = false, length = 255)
  private String exchangeEndpointUri;

  @Basic
  @Column(name = "EXCH_KEY", nullable = false, insertable = true, updatable = false, length = 255)
  private byte[] exchangePublicKey;

  /**
   * Default constructor.
   */
  public ExchangeEntity() {
    super();
  }

  /**
   * Construct with creation date.
   * 
   * @param created
   *        the creation date
   */
  public ExchangeEntity(Instant created) {
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
  public ExchangeEntity(Instant created, String id) {
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

}
