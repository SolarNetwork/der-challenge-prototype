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

package net.solarnetwork.esi.simple.fac.impl.test;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static net.solarnetwork.esi.simple.fac.test.TestUtils.invocationArg;
import static net.solarnetwork.esi.util.CryptoUtils.STANDARD_HELPER;
import static net.solarnetwork.esi.util.CryptoUtils.generateMessageSignature;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.net.URI;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import io.grpc.inprocess.InProcessServerBuilder;
import net.solarnetwork.esi.domain.DerRoute;
import net.solarnetwork.esi.domain.PriceMapOffer;
import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.domain.jpa.PowerComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
import net.solarnetwork.esi.domain.support.ProtobufUtils;
import net.solarnetwork.esi.simple.fac.dao.PriceMapOfferEventEntityDao;
import net.solarnetwork.esi.simple.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.simple.fac.domain.PriceMapEntity;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferEventEntity;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferExecutionState;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferNotification.PriceMapOfferAccepted;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferNotification.PriceMapOfferCountered;
import net.solarnetwork.esi.simple.fac.impl.DaoPriceMapService;
import net.solarnetwork.esi.simple.fac.service.FacilityService;
import net.solarnetwork.esi.util.CryptoUtils;

/**
 * Test cases for the {@link DaoPriceMapService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoPriceMapServiceTests {

  private String facilityUid;
  private URI facilityUri;
  private KeyPair facilityKeyPair;
  private String exchangeUid;
  private KeyPair exchangeKeyPair;
  private ExchangeEntity exchangeEntity;

  private FacilityService facilityService;
  private PriceMapOfferEventEntityDao offerEventDao;
  private ApplicationEventPublisher eventPublisher;

  private DaoPriceMapService service;

  @Before
  public void setup() {
    facilityUid = UUID.randomUUID().toString();
    facilityUri = URI.create("//test-facility");
    facilityKeyPair = STANDARD_HELPER.generateKeyPair();
    exchangeUid = UUID.randomUUID().toString();
    exchangeKeyPair = CryptoUtils.STANDARD_HELPER.generateKeyPair();

    exchangeEntity = new ExchangeEntity(Instant.now(), exchangeUid);
    exchangeEntity.setExchangePublicKey(exchangeKeyPair.getPublic().getEncoded());

    facilityService = mock(FacilityService.class);
    offerEventDao = mock(PriceMapOfferEventEntityDao.class);
    eventPublisher = mock(ApplicationEventPublisher.class);

    service = new DaoPriceMapService(facilityService, offerEventDao);
    service.setEventPublisher(eventPublisher);
  }

  private void givenDefaultFacilityService(URI exchangeUri) {
    given(facilityService.getCryptoHelper()).willReturn(STANDARD_HELPER);
    given(facilityService.getKeyPair()).willReturn(facilityKeyPair);
    given(facilityService.getUid()).willReturn(facilityUid);
    given(facilityService.getUri()).willReturn(facilityUri);

    exchangeEntity.setExchangeEndpointUri(exchangeUri.toString());
    given(facilityService.getExchange()).willReturn(exchangeEntity);
  }

  @Test
  public void receivePriceMapOffer_NoPriceMapsConfigured() {
    // given
    final UUID offerId = UUID.randomUUID();
    final Instant offerDate = Instant.now().plusSeconds(360);
    final PriceMapEmbed offerPriceMap = new PriceMapEmbed();
    offerPriceMap.setPowerComponents(new PowerComponentsEmbed(1L, 2L));
    offerPriceMap.setDuration(Duration.ofSeconds(3));
    offerPriceMap.setResponseTime(DurationRangeEmbed.ofSeconds(4, 5));
    offerPriceMap.setPriceComponents(PriceComponentsEmbed.of("USD", "6.78"));

    String exchangeServerName = InProcessServerBuilder.generateName();
    URI exchangeUri = URI.create("//" + exchangeServerName);
    givenDefaultFacilityService(exchangeUri);

    ArgumentCaptor<PriceMapOfferEventEntity> offerEventCaptor = ArgumentCaptor
        .forClass(PriceMapOfferEventEntity.class);
    given(offerEventDao.save(offerEventCaptor.capture()))
        .willAnswer(invocationArg(0, PriceMapOfferEventEntity.class));

    // @formatter:off
    PriceMapOffer offer = PriceMapOffer.newBuilder()
        .setOfferId(ProtobufUtils.uuidForUuid(offerId))
        .setWhen(ProtobufUtils.timestampForInstant(offerDate))
        .setPriceMap(ProtobufUtils.priceMapForPriceMapEmbed(offerPriceMap))
        .setRoute(DerRoute.newBuilder()
            .setExchangeUid(exchangeUid)
            .setFacilityUid(facilityUid)
            .setSignature(generateMessageSignature(CryptoUtils.STANDARD_HELPER, 
                exchangeKeyPair, facilityKeyPair.getPublic(), 
                asList(
                    exchangeUid,
                    facilityUid,
                    new PriceMapOfferEventEntity(Instant.now(), offerId, offerDate, offerPriceMap))
                ))
            .build())
        .build();
    // @formatter:on

    // when
    PriceMapOfferEventEntity result = service.receivePriceMapOffer(offer);

    // then
    assertThat("Result avaialble", result, notNullValue());
    assertThat("Offer accepted", result.isAccepted(), equalTo(false));
    assertThat("Offer price map", result.offerPriceMap(), equalTo(offerPriceMap));
    assertThat("Offer waiting to execute", result.getExecutionState(),
        equalTo(PriceMapOfferExecutionState.DECLINED));
    assertThat("Offer not completed", result.isCompletedSuccessfully(), equalTo(false));

    verify(eventPublisher, times(0)).publishEvent(any());
  }

  @Test
  public void receivePriceMapOffer_Accepted() {
    // given
    final UUID offerId = UUID.randomUUID();
    final Instant offerDate = Instant.now().plusSeconds(360);
    final PriceMapEmbed offerPriceMap = new PriceMapEmbed();
    offerPriceMap.setPowerComponents(new PowerComponentsEmbed(1L, 2L));
    offerPriceMap.setDuration(Duration.ofSeconds(3));
    offerPriceMap.setResponseTime(DurationRangeEmbed.ofSeconds(4, 5));
    offerPriceMap.setPriceComponents(PriceComponentsEmbed.of("USD", "6.78"));

    String exchangeServerName = InProcessServerBuilder.generateName();
    URI exchangeUri = URI.create("//" + exchangeServerName);
    givenDefaultFacilityService(exchangeUri);

    PriceMapEntity facPriceMap = new PriceMapEntity(Instant.now(), offerPriceMap.copy());
    given(facilityService.getPriceMaps()).willReturn(singleton(facPriceMap));

    ArgumentCaptor<PriceMapOfferEventEntity> offerEventCaptor = ArgumentCaptor
        .forClass(PriceMapOfferEventEntity.class);
    given(offerEventDao.save(offerEventCaptor.capture()))
        .willAnswer(invocationArg(0, PriceMapOfferEventEntity.class));

    // @formatter:off
    PriceMapOffer offer = PriceMapOffer.newBuilder()
        .setOfferId(ProtobufUtils.uuidForUuid(offerId))
        .setWhen(ProtobufUtils.timestampForInstant(offerDate))
        .setPriceMap(ProtobufUtils.priceMapForPriceMapEmbed(offerPriceMap))
        .setRoute(DerRoute.newBuilder()
            .setExchangeUid(exchangeUid)
            .setFacilityUid(facilityUid)
            .setSignature(generateMessageSignature(CryptoUtils.STANDARD_HELPER, 
                exchangeKeyPair, facilityKeyPair.getPublic(), 
                asList(
                    exchangeUid,
                    facilityUid,
                    new PriceMapOfferEventEntity(Instant.now(), offerId, offerDate, offerPriceMap))
                ))
            .build())
        .build();
    // @formatter:on

    // when
    PriceMapOfferEventEntity result = service.receivePriceMapOffer(offer);

    // then
    assertThat("Result avaialble", result, notNullValue());
    assertThat("Offer accepted", result.isAccepted(), equalTo(true));
    assertThat("Offer price map", result.offerPriceMap(), equalTo(offerPriceMap));
    assertThat("Offer waiting to execute", result.getExecutionState(),
        equalTo(PriceMapOfferExecutionState.WAITING));
    assertThat("Offer not completed", result.isCompletedSuccessfully(), equalTo(false));

    ArgumentCaptor<PriceMapOfferAccepted> eventCaptor = ArgumentCaptor
        .forClass(PriceMapOfferAccepted.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

    PriceMapOfferAccepted evt = eventCaptor.getValue();
    assertThat("Event entity same as persisted", evt.getSource(),
        sameInstance(offerEventCaptor.getValue()));
  }

  @Test
  public void receivePriceMapOffer_AcceptedFromMultiple() {
    // given
    final UUID offerId = UUID.randomUUID();
    final Instant offerDate = Instant.now().plusSeconds(360);
    final PriceMapEmbed offerPriceMap = new PriceMapEmbed();
    offerPriceMap.setPowerComponents(new PowerComponentsEmbed(1L, 2L));
    offerPriceMap.setDuration(Duration.ofSeconds(3));
    offerPriceMap.setResponseTime(DurationRangeEmbed.ofSeconds(4, 5));
    offerPriceMap.setPriceComponents(PriceComponentsEmbed.of("USD", "6.78"));

    String exchangeServerName = InProcessServerBuilder.generateName();
    URI exchangeUri = URI.create("//" + exchangeServerName);
    givenDefaultFacilityService(exchangeUri);

    // create a price map that can't be accepted because the min response time
    PriceMapEntity facPriceMapReject = new PriceMapEntity(Instant.now(), offerPriceMap.copy());
    facPriceMapReject.priceMap().responseTime().setMin(offerPriceMap.responseTime().getMax());

    // create another price map that can be accepted, with lower price so can tell
    // difference in result
    PriceMapEntity facPriceMapOk = new PriceMapEntity(Instant.now(), offerPriceMap.copy());
    facPriceMapReject.priceMap().priceComponents().setApparentEnergyPrice(new BigDecimal("1.11"));
    given(facilityService.getPriceMaps())
        .willReturn(Arrays.asList(facPriceMapReject, facPriceMapOk));

    ArgumentCaptor<PriceMapOfferEventEntity> offerEventCaptor = ArgumentCaptor
        .forClass(PriceMapOfferEventEntity.class);
    given(offerEventDao.save(offerEventCaptor.capture()))
        .willAnswer(invocationArg(0, PriceMapOfferEventEntity.class));

    // @formatter:off
    PriceMapOffer offer = PriceMapOffer.newBuilder()
        .setOfferId(ProtobufUtils.uuidForUuid(offerId))
        .setWhen(ProtobufUtils.timestampForInstant(offerDate))
        .setPriceMap(ProtobufUtils.priceMapForPriceMapEmbed(offerPriceMap))
        .setRoute(DerRoute.newBuilder()
            .setExchangeUid(exchangeUid)
            .setFacilityUid(facilityUid)
            .setSignature(generateMessageSignature(CryptoUtils.STANDARD_HELPER, 
                exchangeKeyPair, facilityKeyPair.getPublic(), 
                asList(
                    exchangeUid,
                    facilityUid,
                    new PriceMapOfferEventEntity(Instant.now(), offerId, offerDate, offerPriceMap))
                ))
            .build())
        .build();
    // @formatter:on

    // when
    PriceMapOfferEventEntity result = service.receivePriceMapOffer(offer);

    // then
    assertThat("Result avaialble", result, notNullValue());
    assertThat("Offer accepted", result.isAccepted(), equalTo(true));
    assertThat("Offer price map", result.offerPriceMap(), equalTo(offerPriceMap));
    assertThat("Offer waiting to execute", result.getExecutionState(),
        equalTo(PriceMapOfferExecutionState.WAITING));
    assertThat("Offer not completed", result.isCompletedSuccessfully(), equalTo(false));

    ArgumentCaptor<PriceMapOfferAccepted> eventCaptor = ArgumentCaptor
        .forClass(PriceMapOfferAccepted.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

    PriceMapOfferAccepted evt = eventCaptor.getValue();
    assertThat("Event entity same as persisted", evt.getSource(),
        sameInstance(offerEventCaptor.getValue()));
  }

  @Test
  public void receivePriceMapOffer_CounteredByPrice() {
    // given
    final UUID offerId = UUID.randomUUID();
    final Instant offerDate = Instant.now().plusSeconds(360);
    final PriceMapEmbed offerPriceMap = new PriceMapEmbed();
    offerPriceMap.setPowerComponents(new PowerComponentsEmbed(1L, 2L));
    offerPriceMap.setDuration(Duration.ofSeconds(3));
    offerPriceMap.setResponseTime(DurationRangeEmbed.ofSeconds(4, 5));
    offerPriceMap.setPriceComponents(PriceComponentsEmbed.of("USD", "6.78"));

    String exchangeServerName = InProcessServerBuilder.generateName();
    URI exchangeUri = URI.create("//" + exchangeServerName);
    givenDefaultFacilityService(exchangeUri);

    // create a price map that will be countered with a higher price
    PriceMapEntity facPriceMapExtort = new PriceMapEntity(Instant.now(), offerPriceMap.copy());
    facPriceMapExtort.priceMap().priceComponents().setApparentEnergyPrice(new BigDecimal("9.99"));
    given(facilityService.getPriceMaps()).willReturn(singleton(facPriceMapExtort));

    ArgumentCaptor<PriceMapOfferEventEntity> offerEventCaptor = ArgumentCaptor
        .forClass(PriceMapOfferEventEntity.class);
    given(offerEventDao.save(offerEventCaptor.capture()))
        .willAnswer(invocationArg(0, PriceMapOfferEventEntity.class));

    // @formatter:off
    PriceMapOffer offer = PriceMapOffer.newBuilder()
        .setOfferId(ProtobufUtils.uuidForUuid(offerId))
        .setWhen(ProtobufUtils.timestampForInstant(offerDate))
        .setPriceMap(ProtobufUtils.priceMapForPriceMapEmbed(offerPriceMap))
        .setRoute(DerRoute.newBuilder()
            .setExchangeUid(exchangeUid)
            .setFacilityUid(facilityUid)
            .setSignature(generateMessageSignature(CryptoUtils.STANDARD_HELPER, 
                exchangeKeyPair, facilityKeyPair.getPublic(), 
                asList(
                    exchangeUid,
                    facilityUid,
                    new PriceMapOfferEventEntity(Instant.now(), offerId, offerDate, offerPriceMap))
                ))
            .build())
        .build();
    // @formatter:on

    // when
    PriceMapOfferEventEntity result = service.receivePriceMapOffer(offer);

    // then
    assertThat("Result avaialble", result, notNullValue());
    assertThat("Offer accepted", result.isAccepted(), equalTo(false));
    assertThat("Offer price map", result.offerPriceMap(), equalTo(facPriceMapExtort.priceMap()));
    assertThat("Offer waiting to execute", result.getExecutionState(),
        equalTo(PriceMapOfferExecutionState.COUNTERED));
    assertThat("Offer not completed", result.isCompletedSuccessfully(), equalTo(false));

    ArgumentCaptor<PriceMapOfferCountered> eventCaptor = ArgumentCaptor
        .forClass(PriceMapOfferCountered.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

    PriceMapOfferCountered evt = eventCaptor.getValue();
    assertThat("Event entity same as persisted", evt.getSource(),
        sameInstance(offerEventCaptor.getValue()));
  }

}
