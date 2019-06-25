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

package net.solarnetwork.esi.simple.fac.impl;

import java.net.URI;
import java.security.KeyPair;

import net.solarnetwork.esi.simple.fac.service.FacilityService;
import net.solarnetwork.esi.util.CryptoHelper;

/**
 * DAO implementation of facility service.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoFacilityService implements FacilityService {

  private final String uid;
  private final URI uri;
  private final boolean usePlaintext;
  private final KeyPair keyPair;
  private final CryptoHelper cryptoHelper;

  /**
   * Constructor.
   * 
   * @param uid
   *        the uid
   * @param uri
   *        the URI
   * @param usePlaintext
   *        the plain text flag
   * @param keyPair
   *        the key pair
   */
  public DaoFacilityService(String uid, URI uri, boolean usePlaintext, KeyPair keyPair,
      CryptoHelper cryptoHelper) {
    super();
    this.uid = uid;
    this.uri = uri;
    this.usePlaintext = usePlaintext;
    this.keyPair = keyPair;
    this.cryptoHelper = cryptoHelper;
  }

  @Override
  public String getUid() {
    return uid;
  }

  @Override
  public URI getUri() {
    return uri;
  }

  @Override
  public boolean isUsePlaintext() {
    return usePlaintext;
  }

  @Override
  public KeyPair getKeyPair() {
    return keyPair;
  }

  @Override
  public CryptoHelper getCryptoHelper() {
    return cryptoHelper;
  }

}