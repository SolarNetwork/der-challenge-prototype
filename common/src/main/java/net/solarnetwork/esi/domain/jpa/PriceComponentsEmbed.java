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

package net.solarnetwork.esi.domain.jpa;

import java.math.BigDecimal;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Embeddable components of price.
 * 
 * @author matt
 * @version 1.0
 */
@Embeddable
public class PriceComponentsEmbed {

  // CHECKSTYLE IGNORE LineLength FOR NEXT 12 LINES

  @Basic
  @Column(name = "PRICE_CURRENCY", nullable = false, insertable = true, updatable = true, length = 3)
  private String currencyCode;

  @Basic
  @Column(name = "PRICE_ENERGY_REAL", nullable = true, insertable = true, updatable = true, precision = 18, scale = 9)
  private BigDecimal realEnergyPrice;

  @Basic
  @Column(name = "PRICE_ENERGY_APPARENT", nullable = true, insertable = true, updatable = true, precision = 18, scale = 9)
  private BigDecimal apparentEnergyPrice;

  /**
   * Default constructor.
   */
  public PriceComponentsEmbed() {
    super();
  }

  /**
   * Construct with values.
   * 
   * @param currencyCode
   *        the currency code
   * @param realEnergyPrice
   *        the real energy price, in units per watt hour (Wh)
   * @param apparentEnergyPrice
   *        the apparent energy price, in units per volt-amp hour (VAh)
   */
  public PriceComponentsEmbed(String currencyCode, BigDecimal realEnergyPrice,
      BigDecimal apparentEnergyPrice) {
    super();
    this.currencyCode = currencyCode;
    this.realEnergyPrice = realEnergyPrice;
    this.apparentEnergyPrice = apparentEnergyPrice;
  }

  /**
   * Get the currency code.
   * 
   * @return the currencyCode the currency code
   */
  public String getCurrencyCode() {
    return currencyCode;
  }

  /**
   * Set the currency code.
   * 
   * <p>
   * This should be a 3-letter currency code defined in ISO 4217.
   * </p>
   * 
   * @param currencyCode
   *        the currency code to set
   */
  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  /**
   * Get the real energy price.
   * 
   * @return the real energy price, in units per watt hour (Wh), or {@literal null} if no price
   *         available
   */
  public BigDecimal getRealEnergyPrice() {
    return realEnergyPrice;
  }

  /**
   * Set the real energy price.
   * 
   * @param realEnergyPrice
   *        the price to set, in units per watt hour (Wh)
   */
  public void setRealEnergyPrice(BigDecimal realEnergyPrice) {
    this.realEnergyPrice = realEnergyPrice;
  }

  /**
   * Get the apparent energy price.
   * 
   * @return the apparent energy price, in units per volt-amp hour (VAh), or {@literal null} if no
   *         price available
   */
  public BigDecimal getApparentEnergyPrice() {
    return apparentEnergyPrice;
  }

  /**
   * Get the apparent energy price.
   * 
   * @param apparentEnergyPrice
   *        the price to set, in units per volt-amp hour (VAh)
   */
  public void setApparentEnergyPrice(BigDecimal apparentEnergyPrice) {
    this.apparentEnergyPrice = apparentEnergyPrice;
  }

}
