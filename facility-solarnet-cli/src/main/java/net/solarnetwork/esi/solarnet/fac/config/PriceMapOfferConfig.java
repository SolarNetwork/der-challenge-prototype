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

package net.solarnetwork.esi.solarnet.fac.config;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import net.solarnetwork.esi.grpc.ChannelProvider;
import net.solarnetwork.esi.solarnet.fac.dao.PriceMapOfferEventEntityDao;
import net.solarnetwork.esi.solarnet.fac.impl.DaoPriceMapService;
import net.solarnetwork.esi.solarnet.fac.impl.PriceMapOfferExecutionManager;
import net.solarnetwork.esi.solarnet.fac.impl.SnPriceMapOfferExecutionService;
import net.solarnetwork.esi.solarnet.fac.service.FacilityService;
import net.solarnetwork.esi.solarnet.fac.service.PriceMapOfferExecutionService;
import net.solarnetwork.esi.solarnet.fac.service.PriceMapService;

/**
 * Price map offer related configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class PriceMapOfferConfig {

  @Value("${esi.facility.solarnetwork.instructionPollMs:2000}")
  private long instructionPollMs = 2000L;

  @Autowired
  private FacilityService facilityService;

  @Autowired
  private PriceMapOfferEventEntityDao offerEventDao;

  @Autowired
  private ChannelProvider exchangeChannelProvider;

  @Resource(name = "afterCommitTransactionEventPublisher")
  private ApplicationEventPublisher eventPublisher;

  @Qualifier("solarnetwork")
  @Autowired
  private RestTemplate solarNetworkClient;

  @Qualifier("solarnetwork-base-url")
  @Autowired
  private String solarNetworkBaseUrl;

  @Autowired
  private PlatformTransactionManager txManager;

  @Autowired
  private TaskScheduler taskScheduler;

  /**
   * The price map service.
   * 
   * @return the service
   */
  @Bean
  public PriceMapService priceMapService() {
    DaoPriceMapService s = new DaoPriceMapService(facilityService, offerEventDao);
    s.setEventPublisher(eventPublisher);
    return s;
  }

  /**
   * The price map offer execution service.
   * 
   * @return the offer execution service
   */
  @Bean
  public PriceMapOfferExecutionService priceMapOfferExecutionService() {
    SnPriceMapOfferExecutionService service = new SnPriceMapOfferExecutionService(taskScheduler,
        facilityService, offerEventDao, solarNetworkClient);
    service.setApiBaseUrl(solarNetworkBaseUrl);
    service.setEventPublisher(eventPublisher);
    service.setExchangeChannelProvider(exchangeChannelProvider);
    service.setInstructionPollMs(instructionPollMs);
    service.setTransactionTemplate(new TransactionTemplate(txManager));
    return service;
  }

  /**
   * The price map offer execution manager.
   * 
   * @return the offer execution manager
   */
  @Bean
  public PriceMapOfferExecutionManager priceMapOfferExecutionManager() {
    PriceMapOfferExecutionManager mgr = new PriceMapOfferExecutionManager(
        priceMapOfferExecutionService());
    return mgr;
  }

}
