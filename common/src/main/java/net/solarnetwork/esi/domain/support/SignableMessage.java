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
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Duration;

import javax.annotation.Nonnull;

/**
 * API for something that knows how to encode itself into a byte array message suitable for signing.
 * 
 * @author matt
 * @version 1.0
 */
public interface SignableMessage {

  /** The UTF-8 Charset. */
  Charset UTF8 = Charset.forName("UTF-8");

  /**
   * Get the size, in bytes, required to encode this object as a signature message.
   * 
   * @return the encoding size
   */
  int signatureMessageBytesSize();

  /**
   * Encode this object as a byte array suitable for using as signature message data.
   * 
   * @return the bytes
   */
  @Nonnull
  byte[] toSignatureMessageBytes();

  /**
   * Add the signature message data of this object to an existing byte buffer.
   * 
   * @param buf
   *        the buffer to add to
   */
  void addSignatureMessageBytes(ByteBuffer buf);

  /**
   * Get the fractional part of a {@link BigDecimal} as a {@link BigInteger}.
   * 
   * @param decimal
   *        the decimal part
   * @return the fractional part as an integer, or zero if {@code decimal} is {@literal null}
   */
  @Nonnull
  static BigInteger fractionalPartToInteger(BigDecimal decimal) {
    if (decimal == null) {
      return BigInteger.ZERO;
    }
    return fractionalPartToInteger(decimal, decimal.scale());
  }

  /**
   * Get the fractional part of a {@link BigDecimal} as a {@link BigInteger} with a maximum scale.
   * 
   * <p>
   * If the fractional part must be rounded, the {@link RoundingMode#HALF_DOWN} method will be used
   * to truncate the value to keep it within the desired scale.
   * </p>
   * 
   * @param decimal
   *        the decimal part
   * @param maxScale
   *        the maximum power-of-10 scale
   * @return the fractional part as an integer, or zero if {@code decimal} is {@literal null}
   */
  @Nonnull
  static BigInteger fractionalPartToInteger(BigDecimal decimal, int maxScale) {
    if (decimal == null) {
      return BigInteger.ZERO;
    }
    return decimal.remainder(BigDecimal.ONE).movePointRight(Math.min(decimal.scale(), maxScale))
        .setScale(maxScale, RoundingMode.HALF_DOWN).toBigInteger();
  }

  /**
   * Get the size, in bytes, needed to encode a {@link Duration} in the
   * {@link #addDurationSignatureMessageBytes(ByteBuffer, Duration)} method.
   * 
   * @return the size in bytes
   */
  static int durationSignatureMessageSize() {
    return Long.BYTES + Integer.BYTES;
  }

  /**
   * Add a {@link Duration} to a signature message.
   * 
   * @param buf
   *        the signature message to add the duration to
   * @param duration
   *        the duration to add
   */
  static void addDurationSignatureMessageBytes(ByteBuffer buf, Duration duration) {
    Duration d = duration != null ? duration : Duration.ZERO;
    buf.putLong(d.getSeconds()).putInt(d.getNano());
  }

}
