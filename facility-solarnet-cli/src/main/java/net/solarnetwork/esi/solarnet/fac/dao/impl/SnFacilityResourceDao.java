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

import java.util.Optional;

import org.springframework.web.client.RestTemplate;

import net.solarnetwork.esi.solarnet.fac.dao.FacilityResourceDao;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityResourceCharacteristics;
import net.solarnetwork.web.security.AuthorizationCredentialsProvider;

/**
 * SolarNetwork implementation of {@link FacilityResourceDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class SnFacilityResourceDao extends BaseSolarNetworkMetadataDao
    implements FacilityResourceDao {

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
  public SnFacilityResourceDao(AuthorizationCredentialsProvider credentialsProvider) {
    super(credentialsProvider);
  }

  /**
   * Constructor.
   * 
   * @param restTemplate
   *        the RestTemplate to use
   * @param credentialsProvider
   *        the credentials provider
   */
  public SnFacilityResourceDao(RestTemplate restTemplate,
      AuthorizationCredentialsProvider credentialsProvider) {
    super(restTemplate, credentialsProvider);
  }

  @Override
  public Optional<FacilityResourceCharacteristics> findById(String id) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean existsById(String id) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Iterable<FacilityResourceCharacteristics> findAll() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Iterable<FacilityResourceCharacteristics> findAllById(Iterable<String> ids) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long count() {
    // TODO Auto-generated method stub
    return 0;
  }

}
