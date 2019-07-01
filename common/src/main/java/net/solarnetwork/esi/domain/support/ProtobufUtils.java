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

package net.solarnetwork.esi.domain.support;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;

import javax.annotation.Nonnull;

import com.google.protobuf.Duration;
import com.google.type.Money;

import net.solarnetwork.esi.util.NumberUtils;

/**
 * Utility methods for common functions with some Protobuf domain objects.
 * 
 * @author matt
 * @version 1.0
 */
public final class ProtobufUtils {

  /**
   * Derive a {@link Money} instance from a currency and value.
   * 
   * @param currency
   *        the currency, or {@literal null} for the currency for the default locale
   * @param value
   *        the value, or {@literal null} for {@literal 0}
   * @return the new money instance
   * @see <a href=
   *      "https://github.com/googleapis/googleapis/blob/master/google/type/money.proto">money.proto</a>
   */
  @Nonnull
  public static Money moneyForDecimal(Currency currency, BigDecimal value) {
    Currency c = (currency != null ? currency : Currency.getInstance(Locale.getDefault()));
    BigDecimal d = (value != null ? value : BigDecimal.ZERO);
    // @formatter:off
    return Money.newBuilder()
        .setCurrencyCode(c.getCurrencyCode())
        .setUnits(NumberUtils.wholePartToInteger(d).longValue())
        .setNanos(NumberUtils.fractionalPartToInteger(d, 9).intValue())
        .build();
    // @formatter:on
  }

  /**
   * Derive a {@link BigDecimal} from a {@link Money}.
   * 
   * @param money
   *        the money instance to derive the value from
   * @return the value; if {@code money} is {@literal null} then {@literal 0} will be returned
   */
  @Nonnull
  public static BigDecimal decimalValue(Money money) {
    if (money == null) {
      return BigDecimal.ZERO;
    }
    return new BigDecimal(String.valueOf(money.getUnits()) + "." + Math.abs(money.getNanos()));
  }

  /**
   * Derive a {@link Duration} from a {@link java.time.Duration}.
   * 
   * @param value
   *        the value, or {@literal null} for {@literal 0}
   * @return the new duration instance
   */
  @Nonnull
  public static Duration durationForDuration(java.time.Duration value) {
    java.time.Duration d = (value != null ? value : java.time.Duration.ZERO);
    return Duration.newBuilder().setSeconds(d.getSeconds()).setNanos(d.getNano()).build();
  }

  /**
   * Derive a {@link java.time.Duration} from a {@link Duration}.
   * 
   * @param duration
   *        the duration to derive from
   * @return the value; if {@code duration} is {@literal null} then {@literal 0} will be returned
   */
  @Nonnull
  public static java.time.Duration durationValue(Duration duration) {
    if (duration == null) {
      return java.time.Duration.ZERO;
    }
    return java.time.Duration.ofSeconds(duration.getSeconds(), duration.getNanos());
  }

}
