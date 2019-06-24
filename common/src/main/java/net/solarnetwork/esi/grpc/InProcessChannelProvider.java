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
import io.grpc.inprocess.InProcessChannelBuilder;

/**
 * An in-process channel provider.
 * 
 * @author matt
 * @version 1.0
 */
public class InProcessChannelProvider extends SimpleChannelProvider {

  private boolean direct = true;

  /**
   * Default constructor.
   */
  public InProcessChannelProvider() {
    super();
  }

  /**
   * Construct plain text setting.
   * 
   * @param usePlaintext
   *        {@literal true} to use plain text connections, {@literal false} for SSL
   */
  public InProcessChannelProvider(boolean usePlaintext) {
    super();
    setUsePlaintext(usePlaintext);
  }

  /**
   * Create a channel for the statically-configured URI.
   * 
   * @param uri
   *        this parameter is ignored
   */
  @Override
  public ManagedChannel channelForUri(URI uri) {
    if (uri == null) {
      throw new IllegalArgumentException("URI must not be null.");
    }
    String name = uri.getAuthority();
    if (name == null) {
      throw new IllegalArgumentException("Missing URI authority.");
    }
    InProcessChannelBuilder builder = InProcessChannelBuilder.forName(name);
    if (direct) {
      builder = builder.directExecutor();
    }
    return builder.build();
  }

  /**
   * Get the direct flag.
   * 
   * @return {@literal true} to use the direct executor; default is {@literal true}
   */
  public boolean isDirect() {
    return direct;
  }

  /**
   * Set the direct flag.
   * 
   * @param direct
   *        {@literal true} to use the direct executor
   */
  public void setDirect(boolean direct) {
    this.direct = direct;
  }

}
