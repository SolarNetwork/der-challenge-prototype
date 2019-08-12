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

package net.solarnetwork.esi.solarnet.fac.impl;

import java.net.URI;
import java.security.KeyPair;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.solarnetwork.esi.domain.DerProgramType;
import net.solarnetwork.esi.solarnet.fac.dao.ExchangeEntityDao;
import net.solarnetwork.esi.solarnet.fac.dao.FacilityPriceMapDao;
import net.solarnetwork.esi.solarnet.fac.dao.FacilityProgramDao;
import net.solarnetwork.esi.solarnet.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityPriceMap;
import net.solarnetwork.esi.solarnet.fac.service.FacilityService;
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
  private final FacilityProgramDao programDao;
  private final FacilityPriceMapDao priceMapDao;

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
   * @param programDao
   *        the program DAO
   * @param priceMapDao
   *        the settings DAO
   */
  public DaoFacilityService(String uid, URI uri, boolean usePlaintext, KeyPair keyPair,
      CryptoHelper cryptoHelper, ExchangeEntityDao exchangeDao, FacilityProgramDao programDao,
      FacilityPriceMapDao priceMapDao) {
    super();
    this.uid = uid;
    this.uri = uri;
    this.usePlaintext = usePlaintext;
    this.keyPair = keyPair;
    this.cryptoHelper = cryptoHelper;
    this.exchangeDao = exchangeDao;
    this.programDao = programDao;
    this.priceMapDao = priceMapDao;
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

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public Set<String> getEnabledProgramTypes() {
    Iterable<DerProgramType> itr = programDao.findAll();
    return StreamSupport.stream(itr.spliterator(), false).map(DerProgramType::toString)
        .collect(Collectors.toSet());
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public Iterable<FacilityPriceMap> getPriceMaps() {
    return priceMapDao.findAll();
  }

}
