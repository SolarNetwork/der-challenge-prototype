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

package net.solarnetwork.esi.simple.xchg.impl;

import static java.util.Arrays.asList;
import static net.solarnetwork.esi.util.CryptoUtils.validateMessageSignature;

import java.security.KeyPair;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.solarnetwork.esi.domain.DerCharacteristicsOrBuilder;
import net.solarnetwork.esi.domain.DerRouteOrBuilder;
import net.solarnetwork.esi.simple.xchg.dao.FacilityEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.FacilityResourceCharacteristicsEntityDao;
import net.solarnetwork.esi.simple.xchg.domain.FacilityEntity;
import net.solarnetwork.esi.simple.xchg.domain.FacilityResourceCharacteristicsEntity;
import net.solarnetwork.esi.simple.xchg.service.FacilityCharacteristicsService;
import net.solarnetwork.esi.util.CryptoHelper;

/**
 * DAO based implementation of {@link FacilityCharacteristicsService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoFacilityCharacteristicsService implements FacilityCharacteristicsService {

  private final String exchangeUid;
  private final KeyPair exchangeKeyPair;
  private final CryptoHelper cryptoHelper;
  private FacilityEntityDao facilityDao;
  private FacilityResourceCharacteristicsEntityDao resourceCharacteristicsDao;

  private final Logger log = LoggerFactory.getLogger(DaoFacilityCharacteristicsService.class);

  /**
   * Constructor.
   * 
   * @param exchangeUid
   *        the exchange UID
   * @param exchangeKeyPair
   *        the exchange key pair
   * @param cryptoHelper
   *        the crypto helper
   */
  public DaoFacilityCharacteristicsService(@Qualifier("exchange-uid") String exchangeUid,
      @Qualifier("exchange-key-pair") KeyPair exchangeKeyPair, CryptoHelper cryptoHelper) {
    super();
    this.exchangeUid = exchangeUid;
    this.exchangeKeyPair = exchangeKeyPair;
    this.cryptoHelper = cryptoHelper;
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public FacilityResourceCharacteristicsEntity resourceCharacteristics(String facilityUid) {
    return resourceCharacteristicsDao.findByFacility_FacilityUid(facilityUid).orElse(null);
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public FacilityResourceCharacteristicsEntity saveResourceCharacteristics(
      DerCharacteristicsOrBuilder characteristics) {
    DerRouteOrBuilder route = characteristics.getRouteOrBuilder();
    if (route == null) {
      throw new IllegalArgumentException("Route missing");
    }

    if (!exchangeUid.equals(route.getExchangeUid())) {
      throw new IllegalArgumentException("Exchange UID not valid.");
    }

    String facilityUid = route.getFacilityUid();
    if (facilityUid == null || facilityUid.trim().isEmpty()) {
      throw new IllegalArgumentException("Facility UID missing.");
    }

    // verify the facility already exists
    FacilityResourceCharacteristicsEntity entity = resourceCharacteristicsDao
        .findByFacility_FacilityUid(facilityUid).orElse(null);
    if (entity == null) {
      FacilityEntity facility = facilityDao.findByFacilityUid(facilityUid)
          .orElseThrow(() -> new IllegalArgumentException("Facility not registered."));
      entity = new FacilityResourceCharacteristicsEntity(Instant.now(), facility);
    }

    FacilityResourceCharacteristicsEntity posted = FacilityResourceCharacteristicsEntity
        .entityForMessage(characteristics);

    // verify signature
    // @formatter:off
    validateMessageSignature(cryptoHelper, route.getSignature(), exchangeKeyPair,
        entity.getFacility().publicKey(),
        asList(exchangeUid, 
            facilityUid,
            posted.toSignatureBytes()));
    // @formatter:on

    log.info("Saving facility {} resource characteristcs: {}", facilityUid, characteristics);

    entity.populateFromMessage(characteristics);

    entity = resourceCharacteristicsDao.save(entity);

    // TODO: post app event about update

    return entity;
  }

  /**
   * Set the DAO to use for facility data.
   * 
   * @param facilityDao
   *        the facility DAO to use
   */
  public void setFacilityDao(FacilityEntityDao facilityDao) {
    this.facilityDao = facilityDao;
  }

  /**
   * Set the resource characteristics DAO to use.
   * 
   * @param resourceCharacteristicsDao
   *        the DAO to set
   */
  public void setResourceCharacteristicsDao(
      FacilityResourceCharacteristicsEntityDao resourceCharacteristicsDao) {
    this.resourceCharacteristicsDao = resourceCharacteristicsDao;
  }

}
