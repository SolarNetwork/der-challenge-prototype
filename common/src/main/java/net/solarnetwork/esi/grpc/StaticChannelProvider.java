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
 * A statically-configured channel provider.
 * 
 * @author matt
 * @version 1.0
 */
public class StaticChannelProvider extends SimpleChannelProvider {

  private final URI uri;

  /**
   * Construct with URI.
   * 
   * @param uri
   *        the URI
   */
  public StaticChannelProvider(URI uri) {
    this(uri, true);
  }

  /**
   * Construct with URI and plain text setting.
   * 
   * @param uri
   *        the URI
   * @param usePlaintext
   *        {@literal true} to use plain text connections, {@literal false} for SSL
   */
  public StaticChannelProvider(URI uri, boolean usePlaintext) {
    super(usePlaintext);
    this.uri = uri;
  }

  /**
   * Create a channel for the statically-configured URI.
   * 
   * @param uri
   *        this parameter is ignored
   */
  @Override
  public ManagedChannel channelForUri(URI uri) {
    return super.channelForUri(this.uri);
  }

  /**
   * Get the static channel URI.
   * 
   * @return the URI
   */
  public URI getUri() {
    return uri;
  }

}
