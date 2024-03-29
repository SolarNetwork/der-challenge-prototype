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

package net.solarnetwork.esi.solarnet.fac;

import java.util.concurrent.CountDownLatch;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import net.solarnetwork.esi.cli.CliServices;
import net.solarnetwork.esi.solarnet.fac.config.AppConfiguration;
import net.solarnetwork.esi.solarnet.fac.impl.AppServices;

/**
 * Main entry point for ESI Simple Facility CLI application.
 * 
 * @author matt
 * @version 1.0
 */
@SpringBootApplication(scanBasePackageClasses = { AppConfiguration.class, AppServices.class,
    CliServices.class })
@EnableAsync
@EnableScheduling
public class FacilityTool {

  /**
   * Command-line entry point.
   * 
   * @param args
   *        the command-line arguments
   */
  public static void main(String[] args) throws InterruptedException {
    ApplicationContext ctx = new SpringApplicationBuilder().sources(FacilityTool.class)
        .web(WebApplicationType.NONE).logStartupInfo(false).build().run(args);

    // keep the app running as a service
    final CountDownLatch closeLatch = ctx.getBean("closeLatch", CountDownLatch.class);
    Runtime.getRuntime().addShutdownHook(new Thread() {

      @Override
      public void run() {
        closeLatch.countDown();
      }
    });
    closeLatch.await();
  }

  @Bean
  public CountDownLatch closeLatch() {
    return new CountDownLatch(1);
  }

}
