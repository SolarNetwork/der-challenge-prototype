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

/**
 * API for something that can make copies of itself.
 * 
 * @param <T>
 *        the cloned object type
 * @author matt
 * @version 1.0
 */
public interface Cloning<T> {

  /**
   * Make a copy of this object.
   * 
   * <p>
   * Generally this method is expected to return a deep copy of the object, at least for any mutable
   * portions of the object hierarchy.
   * </p>
   * 
   * @return the copy
   */
  T copy();

}
