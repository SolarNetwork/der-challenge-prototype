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

package net.solarnetwork.esi.simple.fac.cli;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Configuration for the ESI Facility Registration client.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class RegistsryConfig {

  @Value("${esi.registry.conn.usePlaintext:false}")
  private boolean usePlaintext = false;

  @Value("${esi.registry.conn.uri:dns:///localhost:9090}")
  private String uri = "dns:///localhost:9090";

  /**
   * An object factory for creating a gRPC channel to the ESI Facility Registry.
   * 
   * @return the factory
   */
  @Bean(name = "facility-registration")
  public ObjectFactory<ManagedChannel> registryChannel() {
    return new ObjectFactory<ManagedChannel>() {

      @Override
      public ManagedChannel getObject() throws BeansException {
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forTarget(uri);
        if (usePlaintext) {
          channelBuilder.usePlaintext();
        }
        return channelBuilder.build();
      }
    };
  }

}
