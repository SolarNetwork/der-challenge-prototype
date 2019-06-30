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

package net.solarnetwork.esi.domain.support.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.Test;

import com.google.type.Money;

import net.solarnetwork.esi.domain.support.ProtobufUtils;

/**
 * Test cases for the {@link ProtobufUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ProtobufUtilsTests {

  @Test
  public void moneyForDecimal() {
    Currency currency = Currency.getInstance("USD");
    BigDecimal value = new BigDecimal("9.99");
    Money m = ProtobufUtils.moneyForDecimal(currency, value);
    assertThat("Money returned", m, notNullValue());
    assertThat("Money currency", m.getCurrencyCode(), equalTo(currency.getCurrencyCode()));
    assertThat("Money units", m.getUnits(), equalTo(9L));
    assertThat("Money nanos", m.getNanos(), equalTo(99));
  }

  @Test
  public void moneyForDecimalNegative() {
    Currency currency = Currency.getInstance("USD");
    BigDecimal value = new BigDecimal("-9.99");
    Money m = ProtobufUtils.moneyForDecimal(currency, value);
    assertThat("Money returned", m, notNullValue());
    assertThat("Money currency", m.getCurrencyCode(), equalTo(currency.getCurrencyCode()));
    assertThat("Money units", m.getUnits(), equalTo(-9L));
    assertThat("Money nanos", m.getNanos(), equalTo(-99));
  }

  @Test
  public void moneyForLargeDecimal() {
    Currency currency = Currency.getInstance("USD");
    BigDecimal value = new BigDecimal("9129879182731.999999999");
    Money m = ProtobufUtils.moneyForDecimal(currency, value);
    assertThat("Money returned", m, notNullValue());
    assertThat("Money currency", m.getCurrencyCode(), equalTo(currency.getCurrencyCode()));
    assertThat("Money units", m.getUnits(), equalTo(9129879182731L));
    assertThat("Money nanos", m.getNanos(), equalTo(999999999));
  }

  @Test
  public void moneyForLargeDecimalTruncated() {
    Currency currency = Currency.getInstance("USD");
    BigDecimal value = new BigDecimal("9129879182731.1987123498174558");
    Money m = ProtobufUtils.moneyForDecimal(currency, value);
    assertThat("Money returned", m, notNullValue());
    assertThat("Money currency", m.getCurrencyCode(), equalTo(currency.getCurrencyCode()));
    assertThat("Money units", m.getUnits(), equalTo(9129879182731L));
    assertThat("Money nanos", m.getNanos(), equalTo(198712349));
  }

  @Test
  public void decimalForMoney() {
    Money m = Money.newBuilder().setCurrencyCode("USD").setUnits(9).setNanos(99).build();
    BigDecimal value = ProtobufUtils.decimalValue(m);
    assertThat("Result", value, equalTo(new BigDecimal("9.99")));
  }

  @Test
  public void decimalForMoneyNegative() {
    Money m = Money.newBuilder().setCurrencyCode("USD").setUnits(-9).setNanos(-99).build();
    BigDecimal value = ProtobufUtils.decimalValue(m);
    assertThat("Result", value, equalTo(new BigDecimal("-9.99")));
  }

  @Test
  public void decimalForBigMoney() {
    Money m = Money.newBuilder().setCurrencyCode("USD").setUnits(9129879182731L).setNanos(999999999)
        .build();
    BigDecimal value = ProtobufUtils.decimalValue(m);
    assertThat("Result", value, equalTo(new BigDecimal("9129879182731.999999999")));
  }

  @Test
  public void decimalForBigMoneyNegative() {
    Money m = Money.newBuilder().setCurrencyCode("USD").setUnits(-9129879182731L)
        .setNanos(-999999999).build();
    BigDecimal value = ProtobufUtils.decimalValue(m);
    assertThat("Result", value, equalTo(new BigDecimal("-9129879182731.999999999")));
  }
}
