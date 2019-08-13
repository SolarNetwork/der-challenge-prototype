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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import net.solarnetwork.esi.web.support.GzipRequestInterceptor;
import net.solarnetwork.web.security.AuthorizationCredentialsProvider;
import net.solarnetwork.web.support.AuthorizationV2RequestInterceptor;

/**
 * Base class for SolarNetwork-based DAO implementations.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseSolarNetworkMetadataDao {

  protected final RestTemplate client;
  private final AuthorizationCredentialsProvider credentialsProvider;

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
  public BaseSolarNetworkMetadataDao(AuthorizationCredentialsProvider credentialsProvider) {
    this(new RestTemplate(), credentialsProvider);
  }

  /**
   * Constructor.
   * 
   * @param restTemplate
   *        the RestTemplate to use
   * @param credentialsProvider
   *        the credentials provider
   */
  public BaseSolarNetworkMetadataDao(RestTemplate restTemplate,
      AuthorizationCredentialsProvider credentialsProvider) {
    super();
    this.client = restTemplate;
    this.credentialsProvider = credentialsProvider;
    setupRestTemplateInterceptors();
  }

  private void setupRestTemplateInterceptors() {
    RestTemplate restTemplate = client;
    List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
    if (restTemplate.getInterceptors() != null) {
      interceptors.addAll(restTemplate.getInterceptors().stream()
          .filter(o -> !(o instanceof AuthorizationV2RequestInterceptor))
          .collect(Collectors.toList()));
    }
    if (credentialsProvider != null) {
      interceptors.add(0, new AuthorizationV2RequestInterceptor(credentialsProvider));
    }
    if (!interceptors.stream().filter(o -> o instanceof GzipRequestInterceptor).findAny()
        .isPresent()) {
      interceptors.add(0, new GzipRequestInterceptor());
    }
    restTemplate.setInterceptors(interceptors);
  }

}
