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
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Currency;

import org.junit.Test;

import com.google.protobuf.ByteString;

import net.solarnetwork.esi.domain.jpa.PriceComponentsEmbed;
import net.solarnetwork.esi.util.NumberUtils;

/**
 * Test cases for the {@link PriceComponentsEmbed} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PriceComponentsEmbedTests {

  @Test
  public void signatureMessageSize() {
    int size = new PriceComponentsEmbed(Currency.getInstance("USD"), new BigDecimal("1234.123456"))
        .signatureMessageBytesSize();
    assertThat("Offer bytes size", size, equalTo(3 + Long.BYTES + Integer.BYTES));
  }

  @Test
  public void signatureMessageBytes() {
    PriceComponentsEmbed pc = new PriceComponentsEmbed(Currency.getInstance("USD"),
        new BigDecimal("1234.123456"));

    // @formatter:off
    ByteBuffer bb = ByteBuffer.allocate(255);
    bb.put("USD".getBytes(Charset.forName("UTF-8")))
        .putLong(NumberUtils.wholePartToInteger(pc.getApparentEnergyPrice()).longValue())
        .putInt(NumberUtils.fractionalPartToInteger(pc.getApparentEnergyPrice(), 9).intValue());
    bb.flip();
    // @formatter:on

    assertThat("Signature bytes", ByteString.copyFrom(pc.toSignatureMessageBytes()),
        equalTo(ByteString.copyFrom(bb)));
  }

}
