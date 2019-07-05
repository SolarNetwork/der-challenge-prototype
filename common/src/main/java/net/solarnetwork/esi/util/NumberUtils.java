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

package net.solarnetwork.esi.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import javax.annotation.Nonnull;

/**
 * Utilities for dealing with numbers.
 * 
 * @author matt
 * @version 1.0
 */
public final class NumberUtils {

  /**
   * Get the whole part of a {@link BigDecimal} as a {@link BigInteger}.
   * 
   * <p>
   * If whole portion of the decimal is returned without any rounding from the fractional part of
   * the decimal.
   * </p>
   * 
   * @param decimal
   *        the decimal
   * @return the whole part as an integer, or zero if {@code decimal} is {@literal null}
   */
  @Nonnull
  public static BigInteger wholePartToInteger(BigDecimal decimal) {
    if (decimal == null) {
      return BigInteger.ZERO;
    }
    return decimal.setScale(0, decimal.signum() < 0 ? RoundingMode.CEILING : RoundingMode.FLOOR)
        .toBigInteger();
  }

  /**
   * Get the fractional part of a {@link BigDecimal} as a {@link BigInteger}.
   * 
   * @param decimal
   *        the decimal
   * @return the fractional part as an integer, or zero if {@code decimal} is {@literal null}
   */
  @Nonnull
  public static BigInteger fractionalPartToInteger(BigDecimal decimal) {
    if (decimal == null) {
      return BigInteger.ZERO;
    }
    return fractionalPartToInteger(decimal, decimal.scale());
  }

  /**
   * Get the fractional part of a {@link BigDecimal} as a {@link BigInteger} with a maximum scale.
   * 
   * <p>
   * If the fractional part must be rounded, the {@link RoundingMode#FLOOR} method (when positive)
   * or {@link RoundingMode#CEILING} (when negative) will be used to truncate the value to keep it
   * within the desired scale.
   * </p>
   * 
   * @param decimal
   *        the decimal
   * @param maxScale
   *        the maximum power-of-10 scale
   * @return the fractional part as an integer, or zero if {@code decimal} is {@literal null}
   */
  @Nonnull
  public static BigInteger fractionalPartToInteger(BigDecimal decimal, int maxScale) {
    if (decimal == null) {
      return BigInteger.ZERO;
    }
    return decimal.remainder(BigDecimal.ONE).movePointRight(Math.min(decimal.scale(), maxScale))
        .setScale(maxScale, decimal.signum() < 0 ? RoundingMode.CEILING : RoundingMode.FLOOR)
        .toBigInteger();
  }

  /**
   * Get the fractional part of a {@link BigDecimal} as a {@link BigInteger}, scaled by some power
   * of ten.
   * 
   * <p>
   * For example, to convert the fractional part to "nano" scale, pass in {@literal 9} for the
   * scale.
   * </p>
   * 
   * @param decimal
   *        the decimal to get the scaled fractional part from
   * @return the fractional part as an integer, or zero if {@code decimal} is {@literal null}
   */
  @Nonnull
  public static BigInteger fractionalPartScaledToInteger(BigDecimal decimal, int scale) {
    if (decimal == null) {
      return BigInteger.ZERO;
    }
    return decimal.subtract(new BigDecimal(wholePartToInteger(decimal))).movePointRight(scale)
        .setScale(0, decimal.signum() < 0 ? RoundingMode.CEILING : RoundingMode.FLOOR)
        .toBigInteger();
  }

  /**
   * Scale a number by a power of 10.
   * 
   * @param num
   *        the number to scale
   * @param scale
   *        the power of 10 to scale by; a negative value shifts the decimal point left this many
   *        places; a positive value shifts the decimcal point right this many places
   * @return the scaled value
   */
  public static BigDecimal scaled(Number num, int scale) {
    if (num == null) {
      return null;
    }
    BigDecimal n = (num instanceof BigDecimal ? (BigDecimal) num : new BigDecimal(num.toString()));
    if (scale == 0) {
      return n;
    } else if (scale < 0) {
      return n.movePointLeft(-scale);
    } else {
      return n.movePointRight(scale);
    }
  }

}
