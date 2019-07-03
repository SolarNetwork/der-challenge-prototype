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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Currency;

import org.junit.Test;

import com.google.protobuf.ByteString;

import net.solarnetwork.esi.domain.jpa.PriceComponentsEmbed;
import net.solarnetwork.esi.domain.support.SignableMessage;

/**
 * Test cases for the {@link PriceComponentsEmbed} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PriceComponentsEmbedTests {

  private static final Currency USD = Currency.getInstance("USD");

  @Test
  public void equalityEqualsAll() {
    // given
    PriceComponentsEmbed p1 = new PriceComponentsEmbed(USD, new BigDecimal("2.34"));
    PriceComponentsEmbed p2 = new PriceComponentsEmbed(USD, new BigDecimal("2.34"));

    // then
    assertThat("Equal", p1, equalTo(p2));
  }

  @Test
  public void equalityEqualsNoApparentPrice() {
    // given
    PriceComponentsEmbed p1 = new PriceComponentsEmbed(USD, null);
    PriceComponentsEmbed p2 = new PriceComponentsEmbed(USD, null);

    // then
    assertThat("Equal", p1, equalTo(p2));
  }

  @Test
  public void equalityEqualsNoCurrencyCode() {
    // given
    PriceComponentsEmbed p1 = new PriceComponentsEmbed(null, new BigDecimal("2.34"));
    PriceComponentsEmbed p2 = new PriceComponentsEmbed(null, new BigDecimal("2.34"));

    // then
    assertThat("Equal", p1, equalTo(p2));
  }

  @Test
  public void scaledExactly() {
    // given
    PriceComponentsEmbed p = new PriceComponentsEmbed(USD, new BigDecimal("2.34"));

    // when
    PriceComponentsEmbed result = p.scaledExactly(2);

    // then
    assertThat("Results equal", result, equalTo(p));
  }

  @Test(expected = ArithmeticException.class)
  public void scaledExactlyException() {
    // given
    PriceComponentsEmbed p = new PriceComponentsEmbed(USD, new BigDecimal("2.34"));

    // when
    p.scaledExactly(1);
  }

  @Test
  public void scaledWithRounding() {
    // given
    PriceComponentsEmbed p = new PriceComponentsEmbed(USD, new BigDecimal("3.45"));

    // when
    PriceComponentsEmbed result = p.scaled(1);

    // then
    assertThat("Results equal", result,
        equalTo(new PriceComponentsEmbed(USD, new BigDecimal("3.5"))));
  }

  @Test
  public void signatureMessageBytesSize() {
    // given
    PriceComponentsEmbed p = new PriceComponentsEmbed(USD, new BigDecimal("3.45"));

    // when
    int size = p.signatureMessageBytesSize();

    // then
    assertThat("Size", size, equalTo(3 * 2 + Long.BYTES + Integer.BYTES));
  }

  @Test
  public void signatureMessageBytesSizeNoCurrency() {
    // given
    PriceComponentsEmbed p = new PriceComponentsEmbed(null, new BigDecimal("3.45"));

    // when
    int size = p.signatureMessageBytesSize();

    // then
    assertThat("Size", size, equalTo(Long.BYTES + Integer.BYTES));
  }

  @Test
  public void toSignatureMessage() {
    // given
    PriceComponentsEmbed p = new PriceComponentsEmbed(USD, new BigDecimal("3.45"));

    // when
    byte[] data = p.toSignatureMessageBytes();

    // then
    // @formatter:off
    ByteBuffer bb = ByteBuffer.allocate(p.signatureMessageBytesSize())
        .put(p.getCurrency().getCurrencyCode().getBytes(SignableMessage.UTF8))
        .putLong(3L)
        .putInt(45);
    // @formatter:on
    assertThat("Result", ByteString.copyFrom(data),
        equalTo(ByteString.copyFrom((ByteBuffer) bb.flip())));
  }

  @Test
  public void toSignatureMessageNegativePrice() {
    // given
    PriceComponentsEmbed p = new PriceComponentsEmbed(USD, new BigDecimal("-3.45"));

    // when
    byte[] data = p.toSignatureMessageBytes();

    // then
    // @formatter:off
    ByteBuffer bb = ByteBuffer.allocate(p.signatureMessageBytesSize())
        .put(p.getCurrency().getCurrencyCode().getBytes(SignableMessage.UTF8))
        .putLong(-3L)
        .putInt(-45);
    // @formatter:on
    assertThat("Result", ByteString.copyFrom(data),
        equalTo(ByteString.copyFrom((ByteBuffer) bb.flip())));
  }

  @Test
  public void toSignatureMessageTruncatedPrice() {
    // given
    PriceComponentsEmbed p = new PriceComponentsEmbed(USD, new BigDecimal("3.99999999999999"));

    // when
    byte[] data = p.toSignatureMessageBytes();

    // then
    // @formatter:off
    ByteBuffer bb = ByteBuffer.allocate(p.signatureMessageBytesSize())
        .put(p.getCurrency().getCurrencyCode().getBytes(SignableMessage.UTF8))
        .putLong(3L)
        .putInt(999999999);
    // @formatter:on
    assertThat("Result", ByteString.copyFrom(data),
        equalTo(ByteString.copyFrom((ByteBuffer) bb.flip())));
  }

  @Test
  public void toSignatureMessageTruncatedNegativePrice() {
    // given
    PriceComponentsEmbed p = new PriceComponentsEmbed(USD, new BigDecimal("-3.99999999999999"));

    // when
    byte[] data = p.toSignatureMessageBytes();

    // then
    // @formatter:off
    ByteBuffer bb = ByteBuffer.allocate(p.signatureMessageBytesSize())
        .put(p.getCurrency().getCurrencyCode().getBytes(SignableMessage.UTF8))
        .putLong(-3L)
        .putInt(-999999999);
    // @formatter:on
    assertThat("Result", ByteString.copyFrom(data),
        equalTo(ByteString.copyFrom((ByteBuffer) bb.flip())));
  }
}
