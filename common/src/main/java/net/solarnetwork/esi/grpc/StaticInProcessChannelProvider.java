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

import io.grpc.ManagedChannel;

/**
 * A statically-configured in-process channel provider.
 * 
 * @author matt
 * @version 1.0
 */
public class StaticInProcessChannelProvider extends InProcessChannelProvider {

  private final String name;

  /**
   * Construct with name.
   * 
   * @param name
   *        the channel name
   */
  public StaticInProcessChannelProvider(String name) {
    this(name, false);
  }

  /**
   * Construct with name and plain text setting.
   * 
   * @param name
   *        the channel name
   * @param usePlaintext
   *        {@literal true} to use plain text connections, {@literal false} for SSL
   */
  public StaticInProcessChannelProvider(String name, boolean usePlaintext) {
    super(usePlaintext);
    this.name = name;
  }

  /**
   * Create a channel for the statically-configured in-process channel name.
   * 
   * @param uri
   *        this parameter is ignored
   */
  @Override
  public ManagedChannel channelForUri(URI uri) {
    return super.channelForUri(URI.create("//" + name));
  }

  /**
   * Get the in-process channel name.
   * 
   * @return the name
   */
  public String getName() {
    return name;
  }

}
