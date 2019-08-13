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

import net.solarnetwork.esi.solarnet.fac.dao.FacilityProgramDao;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityProgram;
import net.solarnetwork.web.security.AuthorizationCredentialsProvider;

/**
 * SolarNetwork implementation of {@link FacilityProgramDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class SnFacilityProgramDao extends BaseSolarNodeMetadataDao<FacilityProgram>
    implements FacilityProgramDao {

  /** The root key for price map metadata information. */
  public static final String PROGRAM_METADATA_ROOT_KEY = "esi-program";

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
  public SnFacilityProgramDao(AuthorizationCredentialsProvider credentialsProvider) {
    super(FacilityProgram.class, PROGRAM_METADATA_ROOT_KEY, credentialsProvider);
  }

  /**
   * Constructor.
   * 
   * @param restTemplate
   *        the RestTemplate to use; this must already be configured to support any necessary
   *        authentication for working with the SolarNetwork API
   */
  public SnFacilityProgramDao(RestTemplate restTemplate) {
    super(FacilityProgram.class, PROGRAM_METADATA_ROOT_KEY, restTemplate);
  }

}
