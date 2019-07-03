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
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.google.protobuf.ByteString;

import net.solarnetwork.esi.domain.jpa.PowerComponentsEmbed;

/**
 * Test cases for the {@link PowerComponentsEmbed} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PowerComponentsEmbedTests {

  @Test
  public void signatureMessageBytesSize() {
    // given
    PowerComponentsEmbed p = new PowerComponentsEmbed(1L, 2L);

    // when
    int size = p.signatureMessageBytesSize();

    // then
    assertThat("Size", size, equalTo(Long.BYTES * 2));
  }

  @Test
  public void toSignatureMessage() {
    // given
    PowerComponentsEmbed p = new PowerComponentsEmbed(1L, 2L);

    // when
    byte[] data = p.toSignatureMessageBytes();

    // then
    // @formatter:off
    ByteBuffer bb = ByteBuffer.allocate(p.signatureMessageBytesSize())
        .putLong(1L)
        .putLong(2L);
    // @formatter:on
    assertThat("Result", ByteString.copyFrom(data),
        equalTo(ByteString.copyFrom((ByteBuffer) bb.flip())));
  }

  @Test
  public void apparentPowerNoPowerValues() {
    PowerComponentsEmbed p = new PowerComponentsEmbed();
    assertThat("Apparent power computes to zero", p.derivedApparentPower(), equalTo(0.0));
  }

  @Test
  public void apparentPower() {
    PowerComponentsEmbed p = new PowerComponentsEmbed(12345L, 23456L);
    double expected = 26506.281538533465; // Math.sqrt(12345L * 12345L + 23456L * 23456L);
    assertThat("Apparent power computes to zero", p.derivedApparentPower(),
        closeTo(expected, 0.00001));
  }
}
