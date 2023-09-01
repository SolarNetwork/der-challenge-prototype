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
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.ByteBuffer;
import java.time.Duration;

import org.junit.Test;

import com.google.protobuf.ByteString;

import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;

/**
 * Test cases for the {@link DurationRangeEmbed} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DurationRangeEmbedTests {

  @Test
  public void signatureMessageBytesSize() {
    // given
    DurationRangeEmbed d = new DurationRangeEmbed(Duration.ofMillis(123456789),
        Duration.ofMillis(234567891));

    // when
    int size = d.signatureMessageBytesSize();

    // then
    assertThat("Size", size, equalTo(Long.BYTES * 2 + Integer.BYTES * 2));
  }

  @Test
  public void toSignatureMessage() {
    // given
    DurationRangeEmbed d = new DurationRangeEmbed(Duration.ofMillis(123456789),
        Duration.ofMillis(234567891));

    // when
    byte[] data = d.toSignatureMessageBytes();

    // then
    // @formatter:off
    ByteBuffer bb = ByteBuffer.allocate(d.signatureMessageBytesSize())
        .putLong(d.getMin().getSeconds())
        .putInt(d.getMin().getNano())
        .putLong(d.getMax().getSeconds())
        .putInt(d.getMax().getNano());
    // @formatter:on
    assertThat("Result", ByteString.copyFrom(data),
        equalTo(ByteString.copyFrom((ByteBuffer) bb.flip())));
  }

}
