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

package net.solarnetwork.esi.solarnet.fac.dao;

import java.util.Optional;

/**
 * API for a repository with read-only accessors.
 * 
 * <p>
 * This API has been adapted from the Spring Data
 * {@link org.springframework.data.repository.CrudRepository} API.
 * </p>
 * 
 * @param <T>
 *        the entity object type
 * @param <ID>
 *        the primary key type
 * @author matt
 * @version 1.0
 */
public interface ReadableRepository<T, ID> {

  /**
   * Retrieves an entity by its id.
   *
   * @param id
   *        must not be {@literal null}.
   * @return the entity with the given id or {@literal Optional#empty()} if none found
   * @throws IllegalArgumentException
   *         if {@code id} is {@literal null}.
   */
  Optional<T> findById(ID id);

  /**
   * Returns whether an entity with the given id exists.
   *
   * @param id
   *        must not be {@literal null}.
   * @return {@literal true} if an entity with the given id exists, {@literal false} otherwise.
   * @throws IllegalArgumentException
   *         if {@code id} is {@literal null}.
   */
  boolean existsById(ID id);

  /**
   * Returns all instances of the type.
   *
   * @return all entities
   */
  Iterable<T> findAll();

  /**
   * Returns all instances of the type with the given IDs.
   *
   * @param ids
   *        the IDs to find
   * @return matching entities
   */
  Iterable<T> findAllById(Iterable<ID> ids);

  /**
   * Returns the number of entities available.
   *
   * @return the number of entities
   */
  long count();

}
