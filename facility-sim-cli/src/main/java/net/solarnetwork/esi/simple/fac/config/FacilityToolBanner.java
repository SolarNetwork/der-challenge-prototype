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

package net.solarnetwork.esi.simple.fac.config;

import java.io.PrintStream;

import org.springframework.boot.ResourceBanner;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * A specialized banner for printing out relevant startup information for the facility CLI.
 * 
 * @author matt
 * @version 1.0
 */
@Primary
@Component
public class FacilityToolBanner extends ResourceBanner {

  // TODO: DAO for telling if a facility is configured

  /**
   * Default constructor.
   */
  public FacilityToolBanner() {
    this(new ClassPathResource("banner.txt"));
  }

  /**
   * Constructor.
   * 
   * @param resource
   *        the banner resource to display
   */
  public FacilityToolBanner(Resource resource) {
    super(resource);
  }

  @Override
  public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
    super.printBanner(environment, sourceClass, out);

    out.println("Hi, ya!");
  }

}
