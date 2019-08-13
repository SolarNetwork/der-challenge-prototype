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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.esi.domain.jpa.ResourceCharacteristicsEmbed;
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

  private Map<Long, Map<String, FacilityResourceCharacteristics>> nodeResources = new HashMap<>(8);

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
    // CHECKSTYLE IGNORE GenericWhitespace FOR NEXT 3 LINES
    Map<Long, Map<String, FacilityResourceCharacteristics>> all = getAll();
    return all.values().stream().flatMap(m -> m.containsKey(id) ? singleton(m.get(id)).stream()
        : Collections.<FacilityResourceCharacteristics> emptySet().stream()).findAny();
  }

  @Override
  public boolean existsById(String id) {
    return findById(id).isPresent();
  }

  @Override
  public Iterable<FacilityResourceCharacteristics> findAll() {
    Map<Long, Map<String, FacilityResourceCharacteristics>> result = getAll();
    synchronized (nodeResources) {
      nodeResources.putAll(result);
    }
    return result.values().stream().flatMap(m -> m.values().stream()).collect(toList());
  }

  @Override
  public Iterable<FacilityResourceCharacteristics> findAllById(Iterable<String> ids) {
    Set<String> idSet = StreamSupport.stream(ids.spliterator(), false).collect(toSet());
    Map<Long, Map<String, FacilityResourceCharacteristics>> all = getAll();
    return all.values().stream().flatMap(m -> m.values().stream())
        .filter(r -> idSet.contains(r.getId())).collect(toList());
  }

  @Override
  public long count() {
    List<FacilityResourceCharacteristics> all = (List<FacilityResourceCharacteristics>) findAll();
    return all.size();
  }

  private Map<Long, Map<String, FacilityResourceCharacteristics>> getAll() {
    // TODO cache support
    return loadAll();
  }

  private Map<Long, Map<String, FacilityResourceCharacteristics>> loadAll() {
    final ObjectMapper mapper = new ObjectMapper();
    try {
      // TODO: cache support
      Map<Long, Map<String, FacilityResourceCharacteristics>> result = new HashMap<>(8);
      JsonNode data = queryForMetadata(null, "(/pm/esi-resource/*~=.*)");
      for (JsonNode datumNode : data) {
        JsonNode nodeIdNode = datumNode.get("nodeId");
        JsonNode resourceNode = datumNode.findPath("pm").findPath("esi-resource");
        if (nodeIdNode != null && nodeIdNode.isNumber() && resourceNode != null
            && resourceNode.isObject()) {
          final Long nodeId = nodeIdNode.asLong();
          for (Iterator<Map.Entry<String, JsonNode>> itr = resourceNode.fields(); itr.hasNext();) {
            Map.Entry<String, JsonNode> e = itr.next();
            JsonNode characteristicsNode = e.getValue().path("characteristics");
            if (characteristicsNode.isObject()) {
              ResourceCharacteristicsEmbed r = mapper.treeToValue(characteristicsNode,
                  ResourceCharacteristicsEmbed.class);
              result.computeIfAbsent(nodeId, k -> new HashMap<>(8)).put(e.getKey(),
                  new FacilityResourceCharacteristics(e.getKey(), r));
            }
          }
        }
      }
      synchronized (nodeResources) {
        nodeResources.putAll(result);
      }
      return result;
    } catch (URISyntaxException e) {
      throw new RuntimeException("Invalid query URL [" + e.getInput() + ']');
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Error processing datum metadata response", e);
    }
  }
}
