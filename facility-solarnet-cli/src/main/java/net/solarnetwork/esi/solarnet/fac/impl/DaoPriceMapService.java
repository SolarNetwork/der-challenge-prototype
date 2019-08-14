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

import static java.util.Arrays.asList;
import static net.solarnetwork.esi.util.CryptoUtils.validateMessageSignature;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.solarnetwork.esi.domain.DerRouteOrBuilder;
import net.solarnetwork.esi.domain.PriceMapOffer;
import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
import net.solarnetwork.esi.solarnet.fac.dao.PriceMapOfferEventEntityDao;
import net.solarnetwork.esi.solarnet.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityPriceMap;
import net.solarnetwork.esi.solarnet.fac.domain.PriceMapEntity;
import net.solarnetwork.esi.solarnet.fac.domain.PriceMapOfferEventEntity;
import net.solarnetwork.esi.solarnet.fac.domain.PriceMapOfferExecutionState;
import net.solarnetwork.esi.solarnet.fac.domain.PriceMapOfferNotification;
import net.solarnetwork.esi.solarnet.fac.service.FacilityService;
import net.solarnetwork.esi.solarnet.fac.service.PriceMapService;

/**
 * DAO-based implementation of {@link PriceMapService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoPriceMapService implements PriceMapService {

  private final FacilityService facilityService;
  private final PriceMapOfferEventEntityDao offerEventDao;
  private ApplicationEventPublisher eventPublisher;

  private static final Logger log = LoggerFactory.getLogger(DaoPriceMapService.class);

  /**
   * Constructor.
   * 
   * @param facilityService
   *        the facility service
   * @param offerEventDao
   *        the offer event DAO
   */
  public DaoPriceMapService(FacilityService facilityService,
      PriceMapOfferEventEntityDao offerEventDao) {
    super();
    this.facilityService = facilityService;
    this.offerEventDao = offerEventDao;
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public PriceMapOfferEventEntity receivePriceMapOffer(PriceMapOffer offer) {
    final DerRouteOrBuilder route = offer.getRouteOrBuilder();
    if (route == null) {
      throw new IllegalArgumentException("Route missing.");
    }

    final ExchangeEntity exchange = facilityService.getExchange();
    if (exchange == null || !exchange.getId().equals(route.getExchangeUid())) {
      throw new IllegalArgumentException("Exchange UID not valid.");
    }

    if (!facilityService.getUid().equals(route.getFacilityUid())) {
      throw new IllegalArgumentException("Facility UID not valid.");
    }

    // TODO: check if event already created for this offer ID

    PriceMapOfferEventEntity event = PriceMapOfferEventEntity.entityForMessage(offer);

    validateMessageSignature(facilityService.getCryptoHelper(), route.getSignature(),
        facilityService.getKeyPair(), exchange.publicKey(),
        asList(exchange.getId(), facilityService.getUid(), event));

    PriceMapEmbed match = null;
    for (FacilityPriceMap configuredPriceMap : facilityService.getPriceMaps()) {
      match = acceptablePriceMapForEvent(configuredPriceMap, event);
      if (match != null) {
        break;
      }
    }
    if (match == null) {
      // decline offer because no match
      event.setAccepted(false);
      event.setExecutionState(PriceMapOfferExecutionState.DECLINED);
    } else {
      if (match.equals(event.getPriceMap().getPriceMap())) {
        // no counter offer, so let's accept this one
        event.setAccepted(true);
        event.setExecutionState(PriceMapOfferExecutionState.WAITING);
      } else {
        // we've got a counter offer to propose
        event.setAccepted(false);
        event.setCounterOffer(new PriceMapEntity(Instant.now(), match));
        event.setExecutionState(PriceMapOfferExecutionState.COUNTERED);
      }
    }
    event = offerEventDao.save(event);
    if (event.getCounterOffer() != null) {
      publishEvent(new PriceMapOfferNotification.PriceMapOfferCountered(event));
    } else if (event.isAccepted()) {
      publishEvent(new PriceMapOfferNotification.PriceMapOfferAccepted(event));
    }
    return event;
  }

  private PriceMapEmbed acceptablePriceMapForEvent(FacilityPriceMap supportedPriceMap,
      PriceMapOfferEventEntity event) {
    final PriceMapEmbed priceMap = supportedPriceMap.priceMap();
    final PriceMapEmbed offerPriceMap = event.getPriceMap().getPriceMap();
    if (offerPriceMap.duration().compareTo(priceMap.duration()) > 0) {
      // too long to support
      log.info("Price map offer {} not supported by price map {} because duration too long",
          event.getId(), supportedPriceMap.getId());
      return null;
    }

    if ((offerPriceMap.powerComponents().isRealPowerNegative() != priceMap.powerComponents()
        .isRealPowerNegative())
        || (offerPriceMap.powerComponents().isReactivePowerNegative() != priceMap.powerComponents()
            .isReactivePowerNegative())) {
      log.info(
          "Price map offer {} not supported by price map {} because of power direction mismatch",
          event.getId(), supportedPriceMap.getId());
      return null;
    }
    if (offerPriceMap.powerComponents().derivedApparentPower() > priceMap.powerComponents()
        .derivedApparentPower()) {
      // too much power requested
      log.info("Price map offer {} not supported by price map {} because power to large",
          event.getId(), supportedPriceMap.getId());
      return null;
    }

    if (offerPriceMap.responseTime().min().compareTo(priceMap.responseTime().min()) < 0) {
      // minimum response time too short
      log.info(
          "Price map offer {} not supported by price map {} because response time minmum too short",
          event.getId(), supportedPriceMap.getId());
      return null;
    }
    if (offerPriceMap.responseTime().max().compareTo(priceMap.responseTime().max()) < 0) {
      // minimum response time too short
      // CHECKSTYLE IGNORE LineLength FOR NEXT 2 LINES
      log.info(
          "Price map offer {} not supported by price map {} because response time maximum too short",
          event.getId(), supportedPriceMap.getId());
      return null;
    }
    if (offerPriceMap.priceComponents().apparentEnergyPrice()
        .compareTo(priceMap.priceComponents().apparentEnergyPrice()) < 0) {
      // price too low
      log.info("Price map offer {} not supported by price map {} because price too low; countering",
          event.getId(), supportedPriceMap.getId());

      // heck, let's propose our price, and see what happens
      PriceMapEmbed counterOffer = offerPriceMap.copy();
      counterOffer.priceComponents()
          .setApparentEnergyPrice(priceMap.priceComponents().apparentEnergyPrice());

      return counterOffer;
    }
    // sure, we can accept this offer
    log.info("Price map offer [{}] is accptable to [{}]", offerPriceMap.getInfo(),
        priceMap.getInfo());
    return offerPriceMap;
  }

  private void publishEvent(ApplicationEvent event) {
    if (eventPublisher != null) {
      eventPublisher.publishEvent(event);
    }
  }

  /**
   * Set an event publisher to use.
   * 
   * <p>
   * <b>Note</b> consider using a transaction-aware publisher so that events are published after the
   * transactions that emit them are committed.
   * </p>
   * 
   * @param eventPublisher
   *        the event publisher to set
   */
  public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

}
