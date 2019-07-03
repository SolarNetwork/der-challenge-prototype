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

package net.solarnetwork.esi.domain.jpa;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;

import net.solarnetwork.esi.domain.support.SignableMessage;

/**
 * An embeddable duration range.
 * 
 * <p>
 * <b>Note</b> that the duration values are encoded in a manner compatible with the
 * {@code google.type.duration} Protobuf specification in the {@link SignableMessage} methods.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
@Embeddable
public class DurationRangeEmbed implements SignableMessage {

  @Basic
  @Column(name = "DUR_MIN", nullable = false, insertable = true, updatable = true)
  private Duration min;

  @Basic
  @Column(name = "DUR_MAX", nullable = false, insertable = true, updatable = true)
  private Duration max;

  /**
   * Default constructor.
   */
  public DurationRangeEmbed() {
    super();
  }

  /**
   * Construct with values.
   * 
   * @param min
   *        the minimum duration
   * @param max
   *        the maximum duration
   */
  public DurationRangeEmbed(Duration min, Duration max) {
    super();
    this.min = min;
    this.max = max;
  }

  @Override
  public int hashCode() {
    return Objects.hash(max, min);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof DurationRangeEmbed)) {
      return false;
    }
    DurationRangeEmbed other = (DurationRangeEmbed) obj;
    return Objects.equals(max, other.max) && Objects.equals(min, other.min);
  }

  @Override
  public String toString() {
    return "DurationRange{min=" + min + ", max=" + max + "}";
  }

  @Override
  public int signatureMessageBytesSize() {
    return SignableMessage.durationSignatureMessageSize() * 2;
  }

  @Override
  public void addSignatureMessageBytes(ByteBuffer buf) {
    SignableMessage.addDurationSignatureMessageBytes(buf, min);
    SignableMessage.addDurationSignatureMessageBytes(buf, max);
  }

  /**
   * Get the minimum duration.
   * 
   * @return the minimum duration
   */
  public Duration getMin() {
    return min;
  }

  /**
   * Set the minimum duration.
   * 
   * @param min
   *        the duration to set
   */
  public void setMin(Duration min) {
    this.min = min;
  }

  /**
   * Get the minimum duration, never {@literal null}.
   */
  @Nonnull
  public Duration min() {
    Duration d = getMin();
    if (d == null) {
      d = Duration.ZERO;
    }
    return d;
  }

  /**
   * Get the maximum duration.
   * 
   * @return the maximum duration
   */
  public Duration getMax() {
    return max;
  }

  /**
   * Set the maximum duration.
   * 
   * @param max
   *        the duration to set
   */
  public void setMax(Duration max) {
    this.max = max;
  }

  /**
   * Get the maximum duration, never {@literal null}.
   */
  @Nonnull
  public Duration max() {
    Duration d = getMax();
    if (d == null) {
      d = Duration.ZERO;
    }
    return d;
  }

}
