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

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.solarnetwork.esi.solarnet.fac.dao.ReadableRepository;
import net.solarnetwork.esi.solarnet.fac.domain.SolarNodeMetadataEntity;
import net.solarnetwork.esi.solarnet.fac.impl.BaseSolarNetworkClientService;
import net.solarnetwork.esi.solarnet.fac.impl.WebUtils;
import net.solarnetwork.util.StringUtils;
import net.solarnetwork.web.security.AuthorizationCredentialsProvider;

/**
 * Base class for SolarNetwork-based DAO implementations.
 * 
 * @param <T>
 *        the entity object type
 * @param <String>
 *        the primary key type
 * @author matt
 * @version 1.0
 */
public abstract class BaseSolarNodeMetadataDao<T extends SolarNodeMetadataEntity>
    extends BaseSolarNetworkClientService implements ReadableRepository<T, String> {

  private final Class<T> entityClass;
  private final String metadataRootKey;

  /**
   * Default constructor.
   * 
   * <p>
   * This will create a new, default {@link RestTemplate}.
   * </p>
   * 
   * @param entityClass
   *        the entity class instance
   * @param metadataRootKey
   *        the metadata root key for all entities
   * @param credentialsProvider
   *        the credentials provider to use
   */
  public BaseSolarNodeMetadataDao(Class<T> entityClass, String metadataRootKey,
      AuthorizationCredentialsProvider credentialsProvider) {
    this(entityClass, metadataRootKey,
        WebUtils.setupSolarNetworkClient(new RestTemplate(), credentialsProvider));
  }

  /**
   * Constructor.
   * 
   * @param entityClass
   *        the entity class instance
   * @param metadataRootKey
   *        the metadata root key for all entities
   * @param restTemplate
   *        the RestTemplate to use; this must already be configured to support any necessary
   *        authentication for working with the SolarNetwork API
   */
  public BaseSolarNodeMetadataDao(Class<T> entityClass, String metadataRootKey,
      RestTemplate restTemplate) {
    super(restTemplate);
    this.entityClass = entityClass;
    this.metadataRootKey = metadataRootKey;
  }

  /**
   * Get all available data, possibly from cache.
   * 
   * <p>
   * This method will call the {@link #loadAll()} method if the data is not already cached.
   * </p>
   * 
   * @return all available data
   */
  protected Map<Long, Map<String, T>> getAll() {
    // TODO cache support
    Map<Long, Map<String, T>> result = loadAll();
    if (result != null) {
      return result;
    }
    return Collections.emptyMap();
  }

  /**
   * Load all available data into a map with node String keys.
   * 
   * @return all available data
   */
  protected Map<Long, Map<String, T>> loadAll() {
    final ObjectMapper mapper = new ObjectMapper();
    try {
      // TODO: cache support
      Map<Long, Map<String, T>> result = new HashMap<>(8);
      String metadataFilter = "(/pm/" + metadataRootKey + "/*~=.*)";
      JsonNode data = queryForMetadata(null, metadataFilter);
      for (JsonNode datumNode : data) {
        JsonNode nodeIdNode = datumNode.get("nodeId");
        JsonNode resourceNode = datumNode.findPath("pm").findPath(metadataRootKey);
        if (nodeIdNode != null && nodeIdNode.isNumber() && resourceNode != null
            && resourceNode.isObject()) {
          final Long nodeId = nodeIdNode.asLong();
          for (Iterator<Map.Entry<String, JsonNode>> itr = resourceNode.fields(); itr.hasNext();) {
            Map.Entry<String, JsonNode> e = itr.next();
            if (e.getValue().isObject()) {
              T entity = mapper.treeToValue(e.getValue(), entityClass);
              entity.setId(e.getKey());
              entity.setNodeId(nodeId.longValue());
              result.computeIfAbsent(nodeId, k -> new HashMap<>(8)).put(e.getKey(), entity);
            }
          }
        }
      }
      return result;
    } catch (URISyntaxException e) {
      throw new RuntimeException("Invalid query URL [" + e.getInput() + ']');
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Error processing datum metadata response", e);
    }
  }

  @Override
  public Iterable<T> findAll() {
    Map<Long, Map<String, T>> result = getAll();
    return result.values().stream().flatMap(m -> m.values().stream()).collect(toList());
  }

  @Override
  public Iterable<T> findAllById(Iterable<String> ids) {
    Set<String> idSet = StreamSupport.stream(ids.spliterator(), false).collect(toSet());
    Map<Long, Map<String, T>> all = getAll();
    return all.values().stream().flatMap(m -> m.entrySet().stream())
        .filter(e -> idSet.contains(e.getKey())).map(e -> e.getValue()).collect(toList());
  }

  @Override
  public long count() {
    List<T> all = (List<T>) findAll();
    return all.size();
  }

  @Override
  public Optional<T> findById(String id) {
    // CHECKSTYLE IGNORE GenericWhitespace FOR NEXT 3 LINES
    Map<Long, Map<String, T>> all = getAll();
    return all.values().stream().flatMap(m -> m.containsKey(id) ? singleton(m.get(id)).stream()
        : Collections.<T> emptySet().stream()).findAny();
  }

  @Override
  public boolean existsById(String id) {
    return findById(id).isPresent();
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
      uriBuilder.queryParam("metadataFilter", metadataFilter);
    }
    String url = uriBuilder.toUriString();
    log.info("Querying SolarNetwork for node metadata: {}", url);
    ObjectNode json = getRestOperations().getForObject(new URI(url), ObjectNode.class);
    if (json != null && json.findPath("success").booleanValue()) {
      return json.path("data").path("results");
    }
    return MissingNode.getInstance();
  }

}
