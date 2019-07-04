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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.solarnetwork.esi.grpc.ChannelProvider;
import net.solarnetwork.esi.simple.fac.dao.PriceMapEntityDao;
import net.solarnetwork.esi.simple.fac.dao.PriceMapOfferEventEntityDao;
import net.solarnetwork.esi.simple.fac.dao.ResourceCharacteristicsEntityDao;
import net.solarnetwork.esi.simple.fac.impl.DaoFacilityCharacteristicsService;
import net.solarnetwork.esi.simple.fac.impl.DaoPriceMapService;
import net.solarnetwork.esi.simple.fac.service.FacilityCharacteristicsService;
import net.solarnetwork.esi.simple.fac.service.FacilityService;
import net.solarnetwork.esi.simple.fac.service.PriceMapService;

/**
 * Configuration related to resources and characteristics.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class CharacteristicsConfig {

  @Autowired
  private FacilityService facilityService;

  @Autowired
  private PriceMapEntityDao priceMapDao;

  @Autowired
  private PriceMapOfferEventEntityDao offerEventDao;

  @Autowired
  private ResourceCharacteristicsEntityDao resourceCharacteristicsDao;

  @Autowired
  private ChannelProvider exchangeChannelProvider;

  /**
   * Create the {@link FacilityCharacteristicsService}.
   * 
   * @return the service
   */
  @Bean
  public FacilityCharacteristicsService characteristicsService() {
    DaoFacilityCharacteristicsService s = new DaoFacilityCharacteristicsService(facilityService,
        priceMapDao, resourceCharacteristicsDao);
    s.setExchangeChannelProvider(exchangeChannelProvider);
    return s;
  }

  @Bean
  public PriceMapService priceMapService() {
    DaoPriceMapService s = new DaoPriceMapService(facilityService, offerEventDao);
    return s;
  }

}
