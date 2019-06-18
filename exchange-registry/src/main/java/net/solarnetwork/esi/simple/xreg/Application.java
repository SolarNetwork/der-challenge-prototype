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

package net.solarnetwork.esi.simple.xreg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import net.solarnetwork.esi.simple.xreg.config.AppConfiguration;
import net.solarnetwork.esi.simple.xreg.impl.AppServices;

/**
 * Main entry point for ESI Simple Operator Registry App.
 * 
 * @author matt
 */
@SpringBootApplication(scanBasePackageClasses = { AppConfiguration.class, AppServices.class })
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

}
