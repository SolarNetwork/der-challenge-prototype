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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

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
   * <p>
   * This default implementation allocates a buffer of size {@link #signatureMessageBytesSize()} and
   * passes that to {@link #addSignatureMessageBytes(ByteBuffer)}. Then the buffer is flipped and
   * the contents are returned as a byte array.
   * </p>
   * 
   * @return the bytes
   */
  @Nonnull
  default byte[] toSignatureMessageBytes() {
    ByteBuffer buf = ByteBuffer.allocate(signatureMessageBytesSize());
    addSignatureMessageBytes(buf);
    buf.flip();
    byte[] bytes = new byte[buf.limit()];
    buf.get(bytes);
    return bytes;
  }

  /**
   * Add the signature message data of this object to an existing byte buffer.
   * 
   * @param buf
   *        the buffer to add to
   */
  void addSignatureMessageBytes(@Nonnull ByteBuffer buf);

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
   * <p>
   * This puts a long (seconds) and an int (nanos) into the buffer.
   * </p>
   * 
   * @param buf
   *        the signature message to add the duration to
   * @param duration
   *        the duration to add; if {@literal null} then zero values will be put into the buffer
   */
  static void addDurationSignatureMessageBytes(@Nonnull ByteBuffer buf, Duration duration) {
    Duration d = duration != null ? duration : Duration.ZERO;
    buf.putLong(d.getSeconds()).putInt(d.getNano());
  }

  /**
   * Get the size, in bytes, needed to encode an {@link Instant} in the
   * {@link #addInstantSignatureMessageBytes(ByteBuffer, Instant)} method.
   * 
   * @return the size in bytes
   */
  static int instantSignatureMessageSize() {
    return Long.BYTES + Integer.BYTES;
  }

  /**
   * Add an {@link Instant} to a signature message.
   * 
   * <p>
   * This puts a long (seconds) and an int (nanos) into the buffer.
   * </p>
   * 
   * @param buf
   *        the signature message to add the duration to
   * @param instant
   *        the instant to add; if {@literal null} then zero values will be put into the buffer
   */
  static void addInstantSignatureMessageBytes(@Nonnull ByteBuffer buf, Instant instant) {
    Instant d = instant != null ? instant : Instant.EPOCH;
    buf.putLong(d.getEpochSecond()).putInt(d.getNano());
  }

  /**
   * Get the size, in bytes, needed to encode a {@link UUID} in the
   * {@link #addUuidSignatureMessageBytes(ByteBuffer, UUID)} method.
   * 
   * @return the size in bytes
   */
  static int uuidSignatureMessageSize() {
    return Long.BYTES * 2;
  }

  /**
   * Add a {@link UUID} to a signature message.
   * 
   * <p>
   * This puts two longs into the buffer, for the most and least significant bits of the UUID.
   * </p>
   * 
   * @param buf
   *        the signature message to add the UUID to
   * @param uuid
   *        the UUID to add; if {@literal null} then zero values will be put into the buffer
   */
  static void addUuidSignatureMessageBytes(@Nonnull ByteBuffer buf, UUID uuid) {
    if (uuid != null) {
      buf.putLong(uuid.getMostSignificantBits());
      buf.putLong(uuid.getLeastSignificantBits());
    } else {
      buf.putLong(0L);
      buf.putLong(0L);
    }
  }

}
