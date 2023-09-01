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

package net.solarnetwork.esi.domain.jpa.test;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Currency;
import java.util.Locale;

import org.junit.Test;

import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.domain.jpa.PowerComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;

/**
 * Test cases for the {@link PriceMapEmbed} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PriceMapEmbedTests {

  @Test
  public void durationHoursNoDuration() {
    PriceMapEmbed pm = new PriceMapEmbed();
    assertThat("Duration hours", pm.durationHours(), equalTo(0.0));
  }

  @Test
  public void durationHours() {
    PriceMapEmbed pm = new PriceMapEmbed();
    pm.setDuration(Duration.ofMillis(12345678L));
    assertThat("Duration hours", pm.durationHours(), closeTo(3.429355, 0.000001));
  }

  @Test
  public void costNoPrice() {
    PriceMapEmbed pm = new PriceMapEmbed();
    assertThat("Cost computes to zero", pm.calculatedApparentEnergyCost(),
        equalTo(BigDecimal.ZERO));
  }

  @Test
  public void costNoPowerValues() {
    PriceMapEmbed pm = new PriceMapEmbed();
    pm.setDuration(Duration.ofMillis(12345678L));
    pm.priceComponents().setCurrency(Currency.getInstance("USD"));
    pm.priceComponents().setApparentEnergyPrice(new BigDecimal("5.6789"));
    assertThat("Cost computes to zero", pm.calculatedApparentEnergyCost(),
        closeTo(BigDecimal.ZERO, BigDecimal.ZERO));
  }

  @Test
  public void cost() {
    PriceMapEmbed pm = new PriceMapEmbed();
    pm.setPowerComponents(new PowerComponentsEmbed(12345L, 23456L));
    pm.setDuration(Duration.ofMillis(12345678L));
    pm.setPriceComponents(
        new PriceComponentsEmbed(Currency.getInstance("USD"), new BigDecimal("5.6789")));
    assertThat("Cost", pm.calculatedApparentEnergyCost(),
        closeTo(new BigDecimal("516208.881639242"), new BigDecimal("0.000001")));
  }

  @Test
  public void infoString() {
    // given
    PriceMapEmbed pm = new PriceMapEmbed();
    pm.setPowerComponents(new PowerComponentsEmbed(1234L, 2345L));
    pm.setDuration(Duration.ofMillis(12345678L));
    pm.setResponseTime(new DurationRangeEmbed(Duration.ofMillis(3456), Duration.ofMillis(4567)));
    pm.setPriceComponents(
        new PriceComponentsEmbed(Currency.getInstance("USD"), new BigDecimal("0.5678")));

    // when
    String info = pm.toInfoString(Locale.US);

    // then
    assertThat("Info string", info, equalTo("2.650 kVA ~ 3.429h @ $567.80/kVAh = $5,159.78"));
  }

}
