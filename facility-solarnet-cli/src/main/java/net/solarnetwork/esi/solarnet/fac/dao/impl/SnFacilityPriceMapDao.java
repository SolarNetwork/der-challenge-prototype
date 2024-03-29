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

package net.solarnetwork.esi.solarnet.fac.dao.impl;

import org.springframework.web.client.RestTemplate;

import net.solarnetwork.esi.solarnet.fac.dao.FacilityPriceMapDao;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityPriceMap;
import net.solarnetwork.web.security.AuthorizationCredentialsProvider;

/**
 * SolarNetwork implementation of {@link FacilityPriceMapDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class SnFacilityPriceMapDao extends BaseSolarNodeMetadataDao<FacilityPriceMap>
    implements FacilityPriceMapDao {

  /** The root key for price map metadata information. */
  public static final String PRICEMAP_METADATA_ROOT_KEY = "esi-pricemap";

  /**
   * Default constructor.
   * 
   * <p>
   * This will create a new, default {@link RestTemplate}.
   * </p>
   * 
   * @param credentialsProvider
   *        the credentials provider
   */
  public SnFacilityPriceMapDao(AuthorizationCredentialsProvider credentialsProvider) {
    super(FacilityPriceMap.class, PRICEMAP_METADATA_ROOT_KEY, credentialsProvider);
  }

  /**
   * Constructor.
   * 
   * @param restTemplate
   *        the RestTemplate to use; this must already be configured to support any necessary
   *        authentication for working with the SolarNetwork API
   */
  public SnFacilityPriceMapDao(RestTemplate restTemplate) {
    super(FacilityPriceMap.class, PRICEMAP_METADATA_ROOT_KEY, restTemplate);
  }

}
