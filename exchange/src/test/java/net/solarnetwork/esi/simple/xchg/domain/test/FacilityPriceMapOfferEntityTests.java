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

package net.solarnetwork.esi.simple.xchg.domain.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import org.junit.Test;

import com.google.protobuf.ByteString;

import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.domain.jpa.PowerComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
import net.solarnetwork.esi.simple.xchg.domain.FacilityPriceMapOfferEntity;
import net.solarnetwork.esi.simple.xchg.domain.PriceMapEntity;
import net.solarnetwork.esi.simple.xchg.domain.PriceMapOfferingEntity;

/**
 * Test cases for the {@link FacilityPriceMapOfferEntity} class.
 * 
 * @author matt
 * @version 1.0
 */
public class FacilityPriceMapOfferEntityTests {

  @Test
  public void signatureMessageSize() {
    int size = new FacilityPriceMapOfferEntity().signatureMessageBytesSize();
    assertThat("Offer bytes size", size, equalTo(Long.BYTES * 2 + Long.BYTES + Integer.BYTES
        + new PriceMapEmbed().signatureMessageBytesSize()));
  }

  @Test
  public void signatureMessageBytes() {
    PriceMapOfferingEntity offering = new PriceMapOfferingEntity(Instant.now(), UUID.randomUUID());
    offering.setStartDate(Instant.now().plusSeconds(66));

    PriceMapEntity pm = new PriceMapEntity(Instant.now(), UUID.randomUUID());
    pm.priceMap().setPowerComponents(new PowerComponentsEmbed(1L, 2L));
    pm.priceMap().setDuration(Duration.ofMillis(1234L));
    pm.priceMap().setResponseTime(
        new DurationRangeEmbed(Duration.ofMillis(2345L), Duration.ofMillis(3456L)));
    pm.priceMap().setPriceComponents(
        new PriceComponentsEmbed(Currency.getInstance("USD"), new BigDecimal("1.23456")));

    FacilityPriceMapOfferEntity e = new FacilityPriceMapOfferEntity(Instant.now(),
        UUID.randomUUID());
    e.setOffering(offering);
    e.setPriceMap(pm);

    // @formatter:off
    ByteBuffer bb = ByteBuffer.allocate(255);
    bb.putLong(e.getId().getMostSignificantBits())
        .putLong(e.getId().getLeastSignificantBits())
        .putLong(offering.getStartDate().getEpochSecond())
        .putInt(offering.getStartDate().getNano());
    pm.addSignatureMessageBytes(bb);
    bb.flip();
    // @formatter:on

    assertThat("Signature bytes", ByteString.copyFrom(e.toSignatureMessageBytes()),
        equalTo(ByteString.copyFrom(bb)));
  }

}
