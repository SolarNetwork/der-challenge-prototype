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
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;

import net.solarnetwork.esi.domain.support.SignableMessage;
import net.solarnetwork.esi.util.NumberUtils;

/**
 * Embeddable components of price.
 * 
 * <p>
 * <b>Note</b> that the price values are encoded in a manner compatible with the
 * {@code google.type.money} Protobuf specification in the {@link SignableMessage} methods; the
 * fractional portion is limited to 9 decimal digits.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
@Embeddable
public class PriceComponentsEmbed implements SignableMessage {

  // CHECKSTYLE IGNORE LineLength FOR NEXT 12 LINES

  @Basic
  @Column(name = "PRICE_CURRENCY", nullable = false, insertable = true, updatable = true, length = 3)
  private Currency currency;

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
   * @param currency
   *        the currency
   * @param apparentEnergyPrice
   *        the apparent energy price, in units per volt-amp hour (VAh)
   */
  public PriceComponentsEmbed(Currency currency, BigDecimal apparentEnergyPrice) {
    super();
    this.currency = currency;
    this.apparentEnergyPrice = apparentEnergyPrice;
  }

  @Override
  public int hashCode() {
    return Objects.hash(apparentEnergyPrice, currency);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof PriceComponentsEmbed)) {
      return false;
    }
    PriceComponentsEmbed other = (PriceComponentsEmbed) obj;
    return Objects.equals(apparentEnergyPrice, other.apparentEnergyPrice)
        && Objects.equals(currency, other.currency);
  }

  @Override
  public String toString() {
    return "PriceComponents{currency=" + currency + ", apparentEnergyPrice=" + apparentEnergyPrice
        + "}";
  }

  @Override
  public int signatureMessageBytesSize() {
    int ccLength = 0;
    if (currency != null) {
      ccLength += currency.getCurrencyCode().getBytes(UTF8).length;
    }
    return (ccLength + Long.BYTES + Integer.BYTES);
  }

  @Override
  public void addSignatureMessageBytes(ByteBuffer buf) {
    byte[] cc = currency != null ? currency.getCurrencyCode().getBytes(UTF8) : null;
    addPrice(buf, cc, apparentEnergyPrice);
  }

  private void addPrice(ByteBuffer buf, byte[] currencyCodeBytes, BigDecimal d) {
    if (currencyCodeBytes != null) {
      buf.put(currencyCodeBytes);
    }
    buf.putLong(NumberUtils.wholePartToInteger(d).longValue());
    buf.putInt(NumberUtils.fractionalPartToInteger(d, 9).intValue());
  }

  /**
   * Create a copy of the price components with a specific decimal scale.
   * 
   * @param scale
   *        the desired scale
   * @param roundingMode
   *        the rounding mode to use
   * @return the new price components
   */
  public PriceComponentsEmbed scaled(int scale, RoundingMode roundingMode) {
    return new PriceComponentsEmbed(currency,
        apparentEnergyPrice != null ? apparentEnergyPrice.setScale(scale, roundingMode) : null);
  }

  /**
   * Create a copy of the price components with a specific decimal scale, with
   * {@link RoundingMode#HALF_UP} rounding.
   * 
   * @param scale
   *        the desired scale
   * @return the new price components
   */
  public PriceComponentsEmbed scaled(int scale) {
    return scaled(scale, RoundingMode.HALF_UP);
  }

  /**
   * Create a copy of the price components with a specific decimal scale, without rounding.
   * 
   * @param scale
   *        the desired scale
   * @return the new price components
   * @throws ArithmeticException
   *         if any price cannot be set to the given scale without rounding
   */
  public PriceComponentsEmbed scaledExactly(int scale) {
    return scaled(scale, RoundingMode.UNNECESSARY);
  }

  /**
   * Get the currency.
   * 
   * @return the currency
   */
  public Currency getCurrency() {
    return currency;
  }

  /**
   * Set the currency.
   * 
   * @param currency
   *        the currency to set
   */
  public void setCurrency(Currency currency) {
    this.currency = currency;
  }

  /**
   * Get a non-null currency value.
   * 
   * <p>
   * If the currency is not set, this will return the default currency for the default locale.
   * </p>
   * 
   * @return the currency, or a default
   */
  @Nonnull
  public Currency currency() {
    Currency c = getCurrency();
    return (c != null ? c : Currency.getInstance(Locale.getDefault()));
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

  /**
   * Get a non-null apparent energy price.
   * 
   * <p>
   * If the apparent energy price is not set, {@literal 0} will be returned.
   * </p>
   * 
   * @return the apparent energy price, or {@literal 0}
   */
  @Nonnull
  public BigDecimal apparentEnergyPrice() {
    BigDecimal d = getApparentEnergyPrice();
    return (d != null ? d : BigDecimal.ZERO);
  }

}
