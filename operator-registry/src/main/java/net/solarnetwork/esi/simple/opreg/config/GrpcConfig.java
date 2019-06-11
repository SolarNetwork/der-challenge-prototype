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

package net.solarnetwork.esi.simple.opreg.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import net.solarnetwork.esi.domain.DerOperatorInfo;
import net.solarnetwork.esi.simple.opreg.impl.CsvDerOperatorInfoParser;
import net.solarnetwork.esi.simple.opreg.impl.SimpleDerOperatorRegistryService;

/**
 * gRPC configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class GrpcConfig {

  // CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINE
  @Value("${opreg.registry.csv:classpath:net/solarnetwork/esi/simple/opreg/impl/default-registry.csv}")
  private Resource registryCsvResource = new ClassPathResource("default-registry.csv",
      SimpleDerOperatorRegistryService.class);

  private static final Logger log = LoggerFactory.getLogger(GrpcConfig.class);

  private List<DerOperatorInfo> operatorInfos() throws IOException {
    List<DerOperatorInfo> infos = new ArrayList<>(8);
    try (CsvDerOperatorInfoParser parser = new CsvDerOperatorInfoParser(
        new InputStreamReader(registryCsvResource.getInputStream(), "UTF-8"))) {
      for (DerOperatorInfo info : parser) {
        infos.add(info);
      }
    }
    if (log.isInfoEnabled()) {
      StringBuilder buf = new StringBuilder();
      String fmt = "%-20s | %-20s | %-40s\n";
      buf.append(String.format(fmt, "Name", "UID", "URI"));
      buf.append(String.format(fmt, "--------------------", "--------------------",
          "----------------------------------------"));
      for (DerOperatorInfo info : infos) {
        buf.append(String.format(fmt, info.getName(), info.getUid(), info.getEndpointUri()));
      }
      log.info("Loaded {} DerOperatorInfo registry entries:\n\n{}", infos.size(), buf);
    }
    return infos;
  }

  /**
   * Configure the {@link SimpleDerOperatorRegistryService}.
   * 
   * <p>
   * The registry is populated from the data loaded from the CSV resource configured by the
   * {@literal opreg.registry.csv} setting.
   * </p>
   * 
   * @return the configured service
   */
  @Bean
  public SimpleDerOperatorRegistryService operatorRegistryService() {
    try {
      return new SimpleDerOperatorRegistryService(operatorInfos());
    } catch (IOException e) {
      throw new RuntimeException("Error loading registry data from CSV "
          + registryCsvResource.getDescription() + ": " + e.getMessage());
    }
  }

}
