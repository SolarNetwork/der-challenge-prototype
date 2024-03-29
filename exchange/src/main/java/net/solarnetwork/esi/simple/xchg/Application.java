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

package net.solarnetwork.esi.simple.xchg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import net.solarnetwork.esi.cli.CliServices;
import net.solarnetwork.esi.simple.xchg.config.AppConfiguration;
import net.solarnetwork.esi.simple.xchg.impl.AppServices;
import net.solarnetwork.esi.simple.xchg.web.config.WebConfiguration;

/**
 * Main Operator Service application entry point for executable JAR based deployment.
 * 
 * @author matt
 * @version 1.0
 */
@SpringBootApplication(scanBasePackageClasses = { Application.class, AppConfiguration.class,
    AppServices.class, CliServices.class, WebConfiguration.class })
@EnableAsync
public class Application {

  /**
   * Executable JAR deployment main entry point.
   * 
   * @param args
   *        the command line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

}
