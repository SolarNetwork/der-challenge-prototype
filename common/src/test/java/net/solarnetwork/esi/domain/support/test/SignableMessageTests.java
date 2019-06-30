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

import java.nio.ByteBuffer;
import java.time.Duration;

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
  public void durationSignatureMessageSize() {
    int size = SignableMessage.durationSignatureMessageSize();
    assertThat("Duratino bytes size", size, equalTo(Long.BYTES + Integer.BYTES));
  }

  @Test
  public void addDurationSignatureMessageBytes() {
    ByteBuffer bb = ByteBuffer.allocate(SignableMessage.durationSignatureMessageSize());
    SignableMessage.addDurationSignatureMessageBytes(bb, Duration.ofMillis(123456789));
    bb.flip();
    assertThat("Seconds", bb.getLong(), equalTo(123456L));
  }

}
