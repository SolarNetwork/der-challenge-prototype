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

package net.solarnetwork.esi.grpc;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Simple implementation of {@link ChannelProvider}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleChannelProvider implements ChannelProvider {

  /** A class-level logger. */
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private boolean usePlaintext;

  /**
   * Default constructor.
   */
  public SimpleChannelProvider() {
    this(true);
  }

  /**
   * Construct with plain text setting.
   * 
   * @param usePlaintext
   *        {@true} if plain text should be used, {@literal false} if SSL should be used
   */
  public SimpleChannelProvider(boolean usePlaintext) {
    super();
    setUsePlaintext(usePlaintext);
  }

  @Override
  public ManagedChannel channelForUri(URI uri) {
    ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forTarget(uri.toString());
    if (usePlaintext) {
      channelBuilder.usePlaintext();
    }
    log.debug("Building ManagedChannel for gRPC @ {}; SSL = {}", uri, !usePlaintext);
    return channelBuilder.build();
  }

  /**
   * Get the plain text flag.
   * 
   * @return {@true} if plain text should be used, {@literal false} if SSL should be used
   */
  public boolean isUsePlaintext() {
    return usePlaintext;
  }

  /**
   * Set the plain text flag.
   * 
   * @param usePlaintext
   *        {@true} if plain text should be used, {@literal false} if SSL should be used
   */
  public void setUsePlaintext(boolean usePlaintext) {
    this.usePlaintext = usePlaintext;
  }

}
