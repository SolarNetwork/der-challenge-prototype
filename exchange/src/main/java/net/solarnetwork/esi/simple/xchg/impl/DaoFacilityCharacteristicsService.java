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

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.solarnetwork.esi.domain.DerCharacteristicsOrBuilder;
import net.solarnetwork.esi.domain.DerProgramSetOrBuilder;
import net.solarnetwork.esi.domain.DerProgramType;
import net.solarnetwork.esi.domain.DerRouteOrBuilder;
import net.solarnetwork.esi.domain.PriceMapCharacteristicsOrBuilder;
import net.solarnetwork.esi.simple.xchg.dao.FacilityEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.FacilityResourceCharacteristicsEntityDao;
import net.solarnetwork.esi.simple.xchg.domain.FacilityEntity;
import net.solarnetwork.esi.simple.xchg.domain.FacilityPriceMapEntity;
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
            posted));
    // @formatter:on

    log.info("Saving facility {} resource characteristcs: {}", facilityUid, characteristics);

    entity.populateFromMessage(characteristics);

    entity = resourceCharacteristicsDao.save(entity);

    return entity;
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public Set<DerProgramType> activeProgramTypes(String facilityUid) {
    FacilityEntity facility = facilityDao.findByFacilityUid(facilityUid)
        .orElseThrow(() -> new IllegalArgumentException("Facility not registered."));
    Set<String> programs = facility.getProgramTypes();
    if (programs == null) {
      return Collections.emptySet();
    }
    Set<DerProgramType> result = new HashSet<>(programs.size());
    for (String program : programs) {
      try {
        DerProgramType type = DerProgramType.valueOf(program);
        if (type != DerProgramType.UNRECOGNIZED) {
          result.add(type);
        }
      } catch (IllegalArgumentException e) {
        // ignore
      }
    }
    return result;
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public void saveActiveProgramTypes(DerProgramSetOrBuilder programSet) {
    DerRouteOrBuilder route = programSet.getRouteOrBuilder();
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

    FacilityEntity facility = facilityDao.findByFacilityUid(facilityUid)
        .orElseThrow(() -> new IllegalArgumentException("Facility not registered."));

    Set<String> activePrograms = new HashSet<>();
    ByteBuffer signatureData = ByteBuffer.allocate(Integer.BYTES * programSet.getTypeCount());
    for (DerProgramType type : programSet.getTypeList()) {
      activePrograms.add(type.name());
      signatureData.putInt(type.getNumber());
    }

    // verify signature
    // @formatter:off
    validateMessageSignature(cryptoHelper, route.getSignature(), exchangeKeyPair,
        facility.publicKey(),
        asList(exchangeUid, 
            facilityUid,
            signatureData));
    // @formatter:on

    if (facility.getProgramTypes() == null) {
      facility.setProgramTypes(activePrograms);
    } else {
      for (Iterator<String> itr = facility.getProgramTypes().iterator(); itr.hasNext();) {
        String program = itr.next();
        if (!activePrograms.contains(program)) {
          itr.remove();
        }
      }
      facility.getProgramTypes().addAll(activePrograms);
    }

    log.info("Saving facility {} active programs: {}", facilityUid, activePrograms);
    facilityDao.save(facility);
  }

  @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
  @Override
  public FacilityPriceMapEntity priceMap(String facilityUid) {
    FacilityEntity facility = facilityDao.findByFacilityUid(facilityUid)
        .orElseThrow(() -> new IllegalArgumentException("Facility not registered."));
    FacilityPriceMapEntity result = facility.getPriceMap();
    return (result != null ? result.copy() : null);
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public void savePriceMap(PriceMapCharacteristicsOrBuilder priceMapCharacteristics) {
    DerRouteOrBuilder route = priceMapCharacteristics.getRouteOrBuilder();
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

    FacilityEntity facility = facilityDao.findByFacilityUid(facilityUid)
        .orElseThrow(() -> new IllegalArgumentException("Facility not registered."));

    FacilityPriceMapEntity posted = FacilityPriceMapEntity
        .entityForMessage(priceMapCharacteristics.getPriceMapOrBuilder());

    // verify signature
    // @formatter:off
    validateMessageSignature(cryptoHelper, route.getSignature(), exchangeKeyPair,
        facility.publicKey(),
        asList(exchangeUid, 
            facilityUid,
            posted));
    // @formatter:on

    FacilityPriceMapEntity pm = facility.getPriceMap();
    if (pm == null) {
      pm = new FacilityPriceMapEntity(Instant.now(), facility);
      facility.setPriceMap(pm);
    }
    pm.populateFromMessage(priceMapCharacteristics.getPriceMapOrBuilder());

    log.info("Saving facility {} price map: {}", facilityUid, pm);
    facilityDao.save(facility);
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
