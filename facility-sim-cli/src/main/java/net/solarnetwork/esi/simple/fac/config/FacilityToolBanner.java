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
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ResourceBanner;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiOutput.Enabled;
import org.springframework.boot.ansi.AnsiStyle;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import net.solarnetwork.esi.simple.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.simple.fac.service.FacilityService;

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

  @Autowired
  private FacilityService facilityService;

  private final MessageSource messageSource;

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
    ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
    ms.setBasename(getClass().getName());
    this.messageSource = ms;
  }

  @Override
  public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
    super.printBanner(environment, sourceClass, out);

    if (facilityService == null) {
      return;
    }

    ExchangeEntity exchange = facilityService.getExchange();
    if (exchange == null) {
      AnsiOutput.setEnabled(Enabled.ALWAYS);
      out.println(AnsiOutput.toString(AnsiColor.CYAN,
          messageSource.getMessage("exchange.missing.intro", null, Locale.getDefault())));
      out.println(AnsiOutput.toString(messageSource.getMessage("exchange.register.howto",
          new Object[] { AnsiOutput.toString(AnsiStyle.BOLD, "exchange-choose", AnsiStyle.NORMAL) },
          Locale.getDefault())));
    }
  }

  /**
   * Set the facility service.
   * 
   * @param facilityService
   *        the service to set
   */
  public void setFacilityService(FacilityService facilityService) {
    this.facilityService = facilityService;
  }

}
