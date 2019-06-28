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

package net.solarnetwork.esi.simple.fac.service;

import java.net.URI;
import java.security.KeyPair;
import java.util.Set;

import javax.annotation.Nonnull;

import net.solarnetwork.esi.simple.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.util.CryptoHelper;

/**
 * API for facility management.
 * 
 * @author matt
 * @version 1.0
 */
public interface FacilityService {

  /**
   * Get the facility UID.
   * 
   * @return the UID
   */
  String getUid();

  /**
   * Get the facility gRPC URI.
   * 
   * @return the URI
   */
  URI getUri();

  /**
   * Flag if a plain text transport connection should be used for this facility.
   * 
   * @return {@literal true} if a plain text connection should be used, {@literal false} for SSL
   */
  boolean isUsePlaintext();

  /**
   * Get the facility key pair.
   * 
   * @return the key pair
   */
  KeyPair getKeyPair();

  /**
   * Get the crypto helper.
   * 
   * @return the helper
   */
  CryptoHelper getCryptoHelper();

  /**
   * Get the registered exchange.
   * 
   * @return the exchange, or {@literal null} if not available
   */
  ExchangeEntity getExchange();

  /**
   * Get the enabled program types.
   * 
   * @return the enabled types, never {@literal null}
   */
  @Nonnull
  Set<String> getEnabledProgramTypes();

  /**
   * Set the enabled program types.
   * 
   * @param types
   *        the enabled types
   */
  void setEnabledProgramTypes(Set<String> types);

}
