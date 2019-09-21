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

package net.solarnetwork.esi.solarnet.fac.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import net.solarnetwork.esi.web.support.GzipRequestInterceptor;
import net.solarnetwork.web.security.AuthorizationCredentialsProvider;
import net.solarnetwork.web.support.AuthorizationV2RequestInterceptor;

/**
 * Utilities for web routines.
 * 
 * @author matt
 * @version 1.0
 */
public final class WebUtils {

  /**
   * Configure a {@link RestTemplate} for use with the SolarNetwork API.
   * 
   * @param restTemplate
   *        the client to configure
   * @param credentialsProvider
   *        the SolarNetwork credentials provider
   * @return the {@code restTemplate} so this method can be chained
   */
  public static RestTemplate setupSolarNetworkClient(RestTemplate restTemplate,
      AuthorizationCredentialsProvider credentialsProvider) {
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
    return restTemplate;
  }

}
