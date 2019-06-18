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

package net.solarnetwork.esi.domain;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * A helpful base entity object.
 * 
 * @author matt
 * @version 1.0
 */
@MappedSuperclass
public abstract class BaseEntity<K extends Serializable & Comparable<K>>
    implements Entity<K>, Serializable {

  private static final long serialVersionUID = 1159210622003952913L;

  /**
   * Default constructor.
   */
  public BaseEntity() {
    super();
  }

  /**
   * Construct with a creation date.
   * 
   * @param created
   *        the creation date
   */
  public BaseEntity(Instant created) {
    super();
    this.created = created;
  }

  @Basic
  @CreatedDate
  @Column(name = "CREATED_AT", nullable = false, insertable = true, updatable = false)
  private Instant created;

  @Version
  @Column(name = "MODIFIED_AT", nullable = false, insertable = true, updatable = true)
  private Timestamp modified;

  @Override
  public Instant getCreated() {
    return created;
  }

  /**
   * Set the creation date.
   * 
   * @param created
   *        the creation date to set
   */
  protected void setCreated(Instant created) {
    this.created = created;
  }

  @Override
  @LastModifiedDate
  public Instant getModified() {
    Timestamp ts = this.modified;
    return (ts == null ? null : Instant.ofEpochMilli(ts.getTime()));
  }

  /**
   * Set the modification date.
   * 
   * @param time
   *        the modification date to set, or {@literal null} to clear
   */
  public void setModified(Instant time) {
    if (time != null) {
      this.modified = new Timestamp(time.toEpochMilli());
    } else if (this.modified != null) {
      this.modified = null;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    K id = getId();
    Object otherK = ((Entity<?>) obj).getId();
    if (id == null) {
      if (otherK != null) {
        return false;
      }
    } else if (!id.equals(otherK)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return (getClass().getSimpleName() + "{id=" + getId() + "}");
  }

  /**
   * Compare based on the primary key, with {@literal null} values ordered before
   * non-{@literal null} values.
   */
  @Override
  public int compareTo(Identity<K> o) {
    final K id = getId();
    if (id == null && o == null) {
      return 0;
    }
    if (id == null) {
      return -1;
    }
    if (o == null) {
      return 1;
    }
    return id.compareTo(o.getId());
  }

}
