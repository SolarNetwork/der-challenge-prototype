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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * Base class to support SolarNetwork API based services.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseSolarNetworkClientService {

  private final RestTemplate client;
  private String apiBaseUrl = "https://data.solarnetwork.net";

  /** A class-level logger. */
  protected final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Constructor.
   * 
   * @param restTemplate
   *        the RestTemplate to use; this must already be configured to support any necessary
   *        authentication for working with the SolarNetwork API
   */
  public BaseSolarNetworkClientService(RestTemplate restTemplate) {
    super();
    this.client = restTemplate;
  }

  /**
   * Get a full API URL from a path segment.
   * 
   * @param path
   *        the path
   * @return the URL
   */
  protected final String apiUrl(String path) {
    return getApiBaseUrl() + path;
  }

  /**
   * Get the configured RestOperations.
   * 
   * @return the RestOperations
   */
  public final RestOperations getRestOperations() {
    return client;
  }

  /**
   * Set the SolarNetwork API base URL.
   * 
   * @param apiBaseUrl
   *        the base URL to use
   */
  public void setApiBaseUrl(String apiBaseUrl) {
    this.apiBaseUrl = apiBaseUrl;
  }

  /**
   * Get the SolarNetwork API base URL.
   * 
   * @return the base URL to use; defaults to {@literal https://data.solarnetwork.net}
   */
  public String getApiBaseUrl() {
    return apiBaseUrl;
  }

}
