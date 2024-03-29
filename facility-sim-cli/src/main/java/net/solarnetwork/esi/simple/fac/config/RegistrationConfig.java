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

import java.net.URI;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.solarnetwork.esi.grpc.ChannelProvider;
import net.solarnetwork.esi.grpc.StaticChannelProvider;
import net.solarnetwork.esi.simple.fac.dao.ExchangeEntityDao;
import net.solarnetwork.esi.simple.fac.dao.ExchangeRegistrationEntityDao;
import net.solarnetwork.esi.simple.fac.impl.DaoExchangeRegistrationService;
import net.solarnetwork.esi.simple.fac.service.ExchangeRegistrationService;
import net.solarnetwork.esi.simple.fac.service.FacilityService;

/**
 * Configuration for the ESI Facility Registration client.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class RegistrationConfig {

  @Value("${esi.registry.conn.usePlaintext:false}")
  private boolean usePlaintext = false;

  @Value("${esi.registry.conn.uri:dns:///localhost:9090}")
  private String uri = "dns:///localhost:9090";

  @Autowired
  private ExchangeEntityDao exchangeDao;

  @Autowired
  private ExchangeRegistrationEntityDao exchangeRegistrationDao;

  @Autowired
  private FacilityService facilityService;

  @Resource(name = "afterCommitTransactionEventPublisher")
  private ApplicationEventPublisher eventPublisher;

  @Autowired
  private ChannelProvider exchangeChannelProvider;

  private ChannelProvider exchangeRegistryChannelProvider() {
    return new StaticChannelProvider(URI.create(uri), usePlaintext);
  }

  /**
   * Create the {@link ExchangeRegistrationService}.
   * 
   * @return the service
   */
  @Bean
  public DaoExchangeRegistrationService exchangeRegistrationService() {
    DaoExchangeRegistrationService s = new DaoExchangeRegistrationService(facilityService,
        exchangeDao, exchangeRegistrationDao);
    s.setExchangeRegistryChannelProvider(exchangeRegistryChannelProvider());
    s.setExchangeChannelProvider(exchangeChannelProvider);
    s.setEventPublisher(eventPublisher);
    return s;
  }

}
