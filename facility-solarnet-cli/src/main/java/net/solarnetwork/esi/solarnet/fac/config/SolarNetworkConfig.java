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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import net.solarnetwork.esi.solarnet.fac.dao.FacilityPriceMapDao;
import net.solarnetwork.esi.solarnet.fac.dao.FacilityProgramDao;
import net.solarnetwork.esi.solarnet.fac.dao.FacilityResourceDao;
import net.solarnetwork.esi.solarnet.fac.dao.impl.SnFacilityPriceMapDao;
import net.solarnetwork.esi.solarnet.fac.dao.impl.SnFacilityProgramDao;
import net.solarnetwork.esi.solarnet.fac.dao.impl.SnFacilityResourceDao;
import net.solarnetwork.esi.solarnet.fac.impl.WebUtils;
import net.solarnetwork.web.security.AuthorizationCredentialsProvider;
import net.solarnetwork.web.support.StaticAuthorizationCredentialsProvider;

/**
 * SolarNetwork integration configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class SolarNetworkConfig {

  @Value("${esi.facility.solarnetwork.url:https://data.solarnetwork.net}")
  private String solarNetworkBaseUrl = "https://data.solarnetwork.net";

  @Value("${esi.facility.solarnetwork.tokenId}")
  private String solarNetworkTokenId = null;

  @Value("${esi.facility.solarnetwork.tokenSecret}")
  private String solarNetworkTokenSecret = null;

  @Bean
  public AuthorizationCredentialsProvider solarNetworkCredentialsProvider() {
    return new StaticAuthorizationCredentialsProvider(solarNetworkTokenId, solarNetworkTokenSecret);
  }

  @Bean
  public RestTemplate solarNetworkRestTemplate() {
    return WebUtils.setupSolarNetworkClient(new RestTemplate(), solarNetworkCredentialsProvider());
  }

  /**
   * Get the facility resource DAO.
   * 
   * @return the DAO
   */
  @Bean
  public FacilityResourceDao facilityResourceDao() {
    SnFacilityResourceDao dao = new SnFacilityResourceDao(solarNetworkRestTemplate());
    dao.setApiBaseUrl(solarNetworkBaseUrl);
    return dao;
  }

  /**
   * Get the facility price map DAO.
   * 
   * @return the DAO
   */
  @Bean
  public FacilityPriceMapDao facilityPriceMapDao() {
    SnFacilityPriceMapDao dao = new SnFacilityPriceMapDao(solarNetworkRestTemplate());
    dao.setApiBaseUrl(solarNetworkBaseUrl);
    return dao;
  }

  /**
   * Get the facility program DAO.
   * 
   * @return the DAO
   */
  @Bean
  public FacilityProgramDao facilityProgramDao() {
    SnFacilityProgramDao dao = new SnFacilityProgramDao(solarNetworkRestTemplate());
    dao.setApiBaseUrl(solarNetworkBaseUrl);
    return dao;
  }

}
