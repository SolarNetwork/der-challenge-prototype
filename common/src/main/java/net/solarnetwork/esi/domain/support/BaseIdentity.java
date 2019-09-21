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

package net.solarnetwork.esi.domain.support;

import java.io.Serializable;

/**
 * Base implementation of {@link Identity}.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseIdentity<K extends Serializable & Comparable<K>>
    implements Identity<K>, Serializable {

  private static final long serialVersionUID = 1L;

  private K id;

  /**
   * Default constructor.
   */
  public BaseIdentity() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param id
   *        the primary key
   */
  public BaseIdentity(K id) {
    super();
    this.id = id;
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
    Object otherK = ((Identity<?>) obj).getId();
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

  @Override
  public K getId() {
    return id;
  }

  public void setId(K id) {
    this.id = id;
  }

}
