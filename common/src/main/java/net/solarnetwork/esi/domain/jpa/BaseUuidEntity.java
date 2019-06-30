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

package net.solarnetwork.esi.domain.jpa;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * A helpful base class for auto-generated UUID primary key based entities.
 * 
 * @author matt
 * @version 1.0
 */
@MappedSuperclass
public abstract class BaseUuidEntity extends BaseEntity<UUID> {

  private static final long serialVersionUID = 4582556470770011903L;

  @Id
  @GeneratedValue
  @Column(name = "ID", nullable = false, insertable = true, updatable = false, length = 16)
  private UUID id;

  /**
   * Default constructor.
   */
  public BaseUuidEntity() {
    super();
  }

  /**
   * Construct with creation date.
   * 
   * @param created
   *        the creation date
   */
  public BaseUuidEntity(Instant created) {
    super(created);
  }

  /**
   * Construct with creation date and ID.
   * 
   * @param created
   *        the creation date
   * @param id
   *        the ID
   */
  public BaseUuidEntity(Instant created, UUID id) {
    super(created);
    this.id = id;
  }

  @Override
  public UUID getId() {
    return id;
  }

}
