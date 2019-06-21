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

import java.util.concurrent.CountDownLatch;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Main entry point for ESI Simple Facility CLI application.
 * 
 * @author matt
 * @version 1.0
 */
@SpringBootApplication
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
