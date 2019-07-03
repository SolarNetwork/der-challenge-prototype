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
import java.util.UUID;

import javax.annotation.Nonnull;

import com.google.protobuf.Duration;
import com.google.protobuf.DurationOrBuilder;
import com.google.type.Money;
import com.google.type.MoneyOrBuilder;

import net.solarnetwork.esi.domain.DurationRange;
import net.solarnetwork.esi.domain.PowerComponents;
import net.solarnetwork.esi.domain.PriceComponents;
import net.solarnetwork.esi.domain.PriceMap;
import net.solarnetwork.esi.domain.PriceMapOrBuilder;
import net.solarnetwork.esi.domain.Uuid;
import net.solarnetwork.esi.domain.UuidOrBuilder;
import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.domain.jpa.PowerComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
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
  public static BigDecimal decimalValue(MoneyOrBuilder money) {
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
  public static java.time.Duration durationValue(DurationOrBuilder duration) {
    if (duration == null) {
      return java.time.Duration.ZERO;
    }
    return java.time.Duration.ofSeconds(duration.getSeconds(), duration.getNanos());
  }

  /**
   * Derive a {@link Uuid} from a {@link UUID}.
   * 
   * @param value
   *        the value, or {@literal null} for {@literal 0}
   * @return the new Uuid instance
   */
  @Nonnull
  public static Uuid uuidForUuid(UUID value) {
    UUID d = (value != null ? value : new UUID(0L, 0L));
    return Uuid.newBuilder().setHi(d.getMostSignificantBits()).setLo(d.getLeastSignificantBits())
        .build();
  }

  /**
   * Derive a {@link UUID} from a {@link Uuid}.
   * 
   * @param uuid
   *        the Uuid to derive from
   * @return the value; if {@code uuid} is {@literal null} then a UUID with both most/least
   *         significant values set to {@literal 0} will be returned
   */
  @Nonnull
  public static UUID uuidValue(UuidOrBuilder uuid) {
    long hi;
    long lo;
    if (uuid == null) {
      hi = 0;
      lo = 0;
    } else {
      hi = uuid.getHi();
      lo = uuid.getLo();
    }
    return new UUID(hi, lo);
  }

  /**
   * Derive a {@link PriceMap} from a {@link PriceMapEmbed}.
   * 
   * @param value
   *        the value
   * @return the new PriceMap instance
   */
  @Nonnull
  public static PriceMap priceMapForPriceMapEmbed(PriceMapEmbed value) {
    PriceMap.Builder builder = PriceMap.newBuilder();
    if (value != null) {
      // @formatter:off
      builder.setPowerComponents(PowerComponents.newBuilder()
              .setRealPower(value.powerComponents().getRealPower())
              .setReactivePower(value.powerComponents().getReactivePower())
              .build())
          .setDuration(durationForDuration(value.getDuration()))
          .setResponseTime(DurationRange.newBuilder()
              .setMin(durationForDuration(value.responseTime().getMin()))
              .setMax(durationForDuration(value.responseTime().getMax()))
              .build())
          .setPrice(PriceComponents.newBuilder()
              .setApparentEnergyPrice(moneyForDecimal(value.priceComponents().getCurrency(),
                  value.priceComponents().getApparentEnergyPrice()))
              .build());
      // @formatter:on
    }
    return builder.build();
  }

  /**
   * Derive a {@link PriceMapEmbed} from a {@link PriceMap}.
   * 
   * @param priceMap
   *        the PriceMap to derive from
   * @return the value
   */
  @Nonnull
  public static PriceMapEmbed priceMapEmbedValue(PriceMapOrBuilder priceMap) {
    if (priceMap == null) {
      priceMap = PriceMap.getDefaultInstance();
    }
    PriceMapEmbed result = new PriceMapEmbed();
    result.setPowerComponents(new PowerComponentsEmbed(priceMap.getPowerComponents().getRealPower(),
        priceMap.getPowerComponents().getReactivePower()));
    result.setDuration(durationValue(priceMap.getDuration()));
    result
        .setResponseTime(new DurationRangeEmbed(durationValue(priceMap.getResponseTime().getMin()),
            durationValue(priceMap.getResponseTime().getMax())));

    Currency currency = null;
    try {
      currency = Currency
          .getInstance(priceMap.getPrice().getApparentEnergyPrice().getCurrencyCode());
    } catch (IllegalArgumentException e) {
      currency = Currency.getInstance(Locale.getDefault());
    }

    result.setPriceComponents(new PriceComponentsEmbed(currency,
        ProtobufUtils.decimalValue(priceMap.getPrice().getApparentEnergyPrice())));

    return result;
  }
}
