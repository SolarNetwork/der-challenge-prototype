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
import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;

import net.solarnetwork.esi.domain.support.SignableMessage;

/**
 * Embeddable components of price.
 * 
 * @author matt
 * @version 1.0
 */
@Embeddable
public class PriceComponentsEmbed implements SignableMessage {

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

  @Override
  public int hashCode() {
    return Objects.hash(apparentEnergyPrice, currencyCode, realEnergyPrice);
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
        && Objects.equals(currencyCode, other.currencyCode)
        && Objects.equals(realEnergyPrice, other.realEnergyPrice);
  }

  @Override
  public String toString() {
    return "PriceComponentsEmbed{currencyCode=" + currencyCode + ", realEnergyPrice="
        + realEnergyPrice + ", apparentEnergyPrice=" + apparentEnergyPrice + "}";
  }

  @Override
  public int signatureMessageBytesSize() {
    int ccLength = 0;
    if (currencyCode != null) {
      ccLength += currencyCode.getBytes(UTF8).length;
    }
    return (ccLength * 2 + Long.BYTES * 2 + Integer.BYTES * 2);
  }

  /**
   * Encode this object as a byte array suitable for using as signature message data.
   * 
   * <p>
   * <b>Note</b> that the price values are encoded in a manner compatible with the
   * {@code google.type.money} Protobuf specification; the fractional portion is limited to 9
   * decimal digits.
   * </p>
   * 
   * @return the bytes
   * @see <a href=
   *      "https://github.com/googleapis/googleapis/blob/master/google/type/money.proto">money.proto</a>
   */
  @Override
  public byte[] toSignatureMessageBytes() {
    ByteBuffer buf = ByteBuffer.allocate(signatureMessageBytesSize());
    addSignatureMessageBytes(buf);
    buf.flip();
    byte[] bytes = new byte[buf.limit()];
    buf.get(bytes);
    return bytes;
  }

  @Override
  public void addSignatureMessageBytes(ByteBuffer buf) {
    byte[] cc = currencyCode != null ? currencyCode.getBytes(UTF8) : null;
    addPrice(buf, cc, realEnergyPrice);
    addPrice(buf, cc, apparentEnergyPrice);
  }

  private void addPrice(ByteBuffer buf, byte[] currencyCodeBytes, BigDecimal d) {
    if (currencyCodeBytes != null) {
      buf.put(currencyCodeBytes);
    }
    buf.putLong(d != null ? d.longValue() : 0L);
    int i = SignableMessage.fractionalPartToInteger(d, 9).intValue();
    buf.putInt(i);
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
    return new PriceComponentsEmbed(currencyCode,
        realEnergyPrice != null ? realEnergyPrice.setScale(scale, roundingMode) : null,
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
