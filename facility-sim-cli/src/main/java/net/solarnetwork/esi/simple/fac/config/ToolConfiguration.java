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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.event.TransactionPhase;

import net.solarnetwork.esi.dao.support.TransactionalApplicationEventPublisher;

/**
 * General configuration for the tool.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class ToolConfiguration {

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  @Bean
  public ThreadPoolTaskExecutor taskExecutor() {
    return new ThreadPoolTaskExecutor();
  }

  /**
   * Create the application event publisher.
   * 
   * @return the publisher
   */
  @Bean
  public ApplicationEventMulticaster applicationEventMulticaster() {
    SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
    eventMulticaster.setTaskExecutor(taskExecutor());
    return eventMulticaster;
  }

  /**
   * Get an event publisher that publishes after the current transaction commits successfully.
   * 
   * @return the event publisher
   */
  @Qualifier("AFTER_COMMIT")
  @Bean
  public ApplicationEventPublisher afterCommitTransactionEventPublisher() {
    TransactionalApplicationEventPublisher pub = new TransactionalApplicationEventPublisher(
        eventPublisher);
    pub.setPhase(TransactionPhase.AFTER_COMMIT);
    return pub;
  }
}
