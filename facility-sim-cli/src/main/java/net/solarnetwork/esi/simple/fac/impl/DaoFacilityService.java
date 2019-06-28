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
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.solarnetwork.esi.simple.fac.dao.ExchangeEntityDao;
import net.solarnetwork.esi.simple.fac.dao.FacilitySettingsEntityDao;
import net.solarnetwork.esi.simple.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.simple.fac.domain.FacilitySettingsEntity;
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
  private final ExchangeEntityDao exchangeDao;
  private final FacilitySettingsEntityDao settingsDao;

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
   * @param exchangeDao
   *        the exchange DAO
   * @param settingsDao
   *        the settings DAO
   */
  public DaoFacilityService(String uid, URI uri, boolean usePlaintext, KeyPair keyPair,
      CryptoHelper cryptoHelper, ExchangeEntityDao exchangeDao,
      FacilitySettingsEntityDao settingsDao) {
    super();
    this.uid = uid;
    this.uri = uri;
    this.usePlaintext = usePlaintext;
    this.keyPair = keyPair;
    this.cryptoHelper = cryptoHelper;
    this.exchangeDao = exchangeDao;
    this.settingsDao = settingsDao;
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

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public ExchangeEntity getExchange() {
    // get the first available exchange, sorted by creation date asending, so newest
    Iterator<ExchangeEntity> itr = exchangeDao
        .findAll(PageRequest.of(0, 1, Direction.DESC, "created")).iterator();
    return (itr.hasNext() ? itr.next() : null);
  }

  private FacilitySettingsEntity settings() {
    // get the first available settings, sorted by creation date asending, so newest
    Iterator<FacilitySettingsEntity> itr = settingsDao
        .findAll(PageRequest.of(0, 1, Direction.DESC, "created")).iterator();
    return (itr.hasNext() ? itr.next() : null);
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public Set<String> getEnabledProgramTypes() {
    FacilitySettingsEntity settings = settings();
    return (settings != null && settings.getProgramTypes() != null
        ? Collections.unmodifiableSet(settings.getProgramTypes())
        : Collections.emptySet());
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public void setEnabledProgramTypes(Set<String> types) {
    FacilitySettingsEntity settings = settings();
    if (settings == null) {
      settings = new FacilitySettingsEntity(Instant.now());
    }
    if (settings.getProgramTypes() == null) {
      settings.setProgramTypes(types);
    } else {
      for (Iterator<String> itr = settings.getProgramTypes().iterator(); itr.hasNext();) {
        if (!types.contains(itr.next())) {
          itr.remove();
        }
      }
      settings.getProgramTypes().addAll(types);
    }
    settingsDao.save(settings);
  }

}
