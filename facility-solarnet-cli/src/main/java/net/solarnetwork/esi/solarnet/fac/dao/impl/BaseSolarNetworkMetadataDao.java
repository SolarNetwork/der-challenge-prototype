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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.solarnetwork.esi.web.support.GzipRequestInterceptor;
import net.solarnetwork.util.StringUtils;
import net.solarnetwork.web.security.AuthorizationCredentialsProvider;
import net.solarnetwork.web.support.AuthorizationV2RequestInterceptor;

/**
 * Base class for SolarNetwork-based DAO implementations.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseSolarNetworkMetadataDao {

  private final RestTemplate client;
  private final AuthorizationCredentialsProvider credentialsProvider;
  private String apiBaseUrl = "https://data.solarnetwork.net";

  /** A class-level logger. */
  protected final Logger log = LoggerFactory.getLogger(getClass());

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

  /**
   * Query for node metadata.
   * 
   * @param nodeIds
   *        the optional node IDs to limit the results to
   * @param metadataFilter
   *        an optional metadata filter to use
   * @return the results
   * @throws RuntimeException
   *         if the URL syntax is not valid or there is a JSON processing error
   */
  protected JsonNode queryForMetadata(Set<Long> nodeIds, String metadataFilter)
      throws URISyntaxException, JsonProcessingException {
    UriComponentsBuilder uriBuilder = UriComponentsBuilder
        .fromHttpUrl(apiUrl("/solarquery/api/v1/sec/nodes/meta"));
    if (nodeIds != null && !nodeIds.isEmpty()) {
      uriBuilder.queryParam("nodeIds", StringUtils.commaDelimitedStringFromCollection(nodeIds));
    }
    if (metadataFilter != null && !metadataFilter.isEmpty()) {
      uriBuilder.queryParam("metadataFilter", "(/pm/esi-resource/*~=.*)");
    }
    String url = uriBuilder.toUriString();
    log.info("Querying SolarNetwork for node metadata: {}", url);
    ObjectNode json = getRestOperations().getForObject(new URI(url), ObjectNode.class);
    if (json != null && json.findPath("success").booleanValue()) {
      return json.path("data").path("results");
    }
    return MissingNode.getInstance();
  }

  /**
   * Get a full API URL from a path segment.
   * 
   * @param path
   *        the path
   * @return the URL
   */
  protected String apiUrl(String path) {
    return getApiBaseUrl() + path;
  }

  /**
   * Get the configured RestOperations.
   * 
   * @return the RestOperations
   */
  public RestOperations getRestOperations() {
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
