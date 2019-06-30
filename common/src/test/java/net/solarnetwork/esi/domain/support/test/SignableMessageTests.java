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
import java.math.BigInteger;

import org.junit.Test;

import net.solarnetwork.esi.domain.support.SignableMessage;

/**
 * Test cases for the {@link SignableMessage} API.
 * 
 * @author matt
 * @version 1.0
 */
public class SignableMessageTests {

  @Test
  public void fractionalPartFromNull() {
    BigInteger bi = SignableMessage.fractionalPartToInteger(null);
    assertThat("Result", bi, equalTo(BigInteger.ZERO));
  }

  @Test
  public void fractionalPartFromSmallDecimal() {
    BigInteger bi = SignableMessage.fractionalPartToInteger(new BigDecimal("123.12345"));
    assertThat("Result", bi, equalTo(new BigInteger("12345")));
  }

  @Test
  public void fractionalPartFromSmallDecimalNegative() {
    BigInteger bi = SignableMessage.fractionalPartToInteger(new BigDecimal("-123.12345"));
    assertThat("Result", bi, equalTo(new BigInteger("-12345")));
  }

  @Test
  public void fractionalPartFromScaledDecimal() {
    BigInteger bi = SignableMessage.fractionalPartToInteger(new BigDecimal("123.12345"), 9);
    assertThat("Result", bi, equalTo(new BigInteger("12345")));
  }

  @Test
  public void fractionalPartFromScaledDecimalTruncated() {
    BigInteger bi = SignableMessage
        .fractionalPartToInteger(new BigDecimal("123.123456789123456789"), 9);
    assertThat("Result", bi, equalTo(new BigInteger("123456789")));
  }

  @Test
  public void fractionalPartFromScaledDecimalTruncatedNegative() {
    BigInteger bi = SignableMessage
        .fractionalPartToInteger(new BigDecimal("-123.123456789123456789"), 9);
    assertThat("Result", bi, equalTo(new BigInteger("-123456789")));
  }

  @Test
  public void fractionalPartScaledRounded() {
    BigInteger bi = SignableMessage.fractionalPartToInteger(new BigDecimal("3.99999999999999"), 9);
    assertThat("Result", bi, equalTo(new BigInteger(String.valueOf("999999999"))));
  }

  @Test
  public void fractionalPartScaledRoundedNegative() {
    BigInteger bi = SignableMessage.fractionalPartToInteger(new BigDecimal("-3.99999999999999"), 9);
    assertThat("Result", bi, equalTo(new BigInteger(String.valueOf("-999999999"))));
  }
}
