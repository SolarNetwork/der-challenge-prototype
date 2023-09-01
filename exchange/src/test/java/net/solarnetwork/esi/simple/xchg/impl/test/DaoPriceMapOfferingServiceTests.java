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

package net.solarnetwork.esi.simple.xchg.impl.test;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static net.solarnetwork.esi.simple.xchg.test.TestUtils.invocationArg;
import static net.solarnetwork.esi.util.CryptoUtils.STANDARD_HELPER;
import static net.solarnetwork.esi.util.CryptoUtils.generateMessageSignature;
import static net.solarnetwork.esi.util.CryptoUtils.validateMessageSignature;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer1;

import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import net.solarnetwork.esi.domain.DerRoute;
import net.solarnetwork.esi.domain.PriceMap;
import net.solarnetwork.esi.domain.PriceMapOffer;
import net.solarnetwork.esi.domain.PriceMapOfferResponse;
import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.domain.jpa.PowerComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
import net.solarnetwork.esi.domain.support.ProtobufUtils;
import net.solarnetwork.esi.domain.support.SignableMessage;
import net.solarnetwork.esi.grpc.StaticInProcessChannelProvider;
import net.solarnetwork.esi.service.DerFacilityServiceGrpc.DerFacilityServiceImplBase;
import net.solarnetwork.esi.simple.xchg.dao.FacilityEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.FacilityPriceMapOfferEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.PriceMapOfferingEntityDao;
import net.solarnetwork.esi.simple.xchg.domain.FacilityEntity;
import net.solarnetwork.esi.simple.xchg.domain.FacilityPriceMapOfferEntity;
import net.solarnetwork.esi.simple.xchg.domain.PriceMapEntity;
import net.solarnetwork.esi.simple.xchg.domain.PriceMapOfferingEntity;
import net.solarnetwork.esi.simple.xchg.impl.DaoPriceMapOfferingService;
import net.solarnetwork.esi.util.CryptoUtils;

/**
 * Test cases for the {@link DaoPriceMapOfferingService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoPriceMapOfferingServiceTests {

  private String exchangeUid;
  private KeyPair exchangeKeyPair;
  private String facilityUid;
  private KeyPair facilityKeyPair;
  private DaoPriceMapOfferingService service;
  private FacilityEntityDao facilityDao;
  private FacilityPriceMapOfferEntityDao priceMapOfferDao;
  private PriceMapOfferingEntityDao offeringDao;

  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  @Before
  public void setUp() throws Exception {

    exchangeUid = UUID.randomUUID().toString();
    exchangeKeyPair = STANDARD_HELPER.generateKeyPair();
    facilityUid = UUID.randomUUID().toString();
    facilityKeyPair = STANDARD_HELPER.generateKeyPair();
    facilityDao = mock(FacilityEntityDao.class);
    priceMapOfferDao = mock(FacilityPriceMapOfferEntityDao.class);
    offeringDao = mock(PriceMapOfferingEntityDao.class);

    service = new DaoPriceMapOfferingService(exchangeUid, exchangeKeyPair,
        CryptoUtils.STANDARD_HELPER);
    service.setFacilityDao(facilityDao);
    service.setOfferingDao(offeringDao);
    service.setPriceMapOfferDao(priceMapOfferDao);
  }

  @Test
  public void createOffering() {
    // given
    PriceMapEmbed priceMap = new PriceMapEmbed();

    ArgumentCaptor<PriceMapOfferingEntity> offeringCaptor = ArgumentCaptor
        .forClass(PriceMapOfferingEntity.class);
    given(offeringDao.save(offeringCaptor.capture()))
        .willAnswer(invocationArg(0, PriceMapOfferingEntity.class));

    // when
    Instant startDate = Instant.now().plusSeconds(60);
    PriceMapOfferingEntity offering = service.createPriceMapOffering(priceMap, startDate);

    // then
    assertThat("Offering returned", offering, notNullValue());
    assertThat("Offering persisted", offeringCaptor.getValue(), sameInstance(offering));
    assertThat("Start date", offering.getStartDate(), equalTo(startDate));
    assertThat("Price map entity created", offering.getPriceMap(), notNullValue());
    assertThat("Price map", offering.getPriceMap().getPriceMap(), equalTo(priceMap));
  }

  @Test
  public void makeOfferYouCantRefuse() throws Exception {
    // given
    PriceMapEmbed priceMap = new PriceMapEmbed();
    priceMap.setPowerComponents(new PowerComponentsEmbed(1L, 2L));
    priceMap.setDuration(Duration.ofMillis(3456L));
    priceMap.setResponseTime(
        new DurationRangeEmbed(Duration.ofMillis(4567L), Duration.ofMillis(5678L)));
    priceMap.setPriceComponents(
        new PriceComponentsEmbed(Currency.getInstance("USD"), new BigDecimal("9.87")));
    UUID offeringId = UUID.randomUUID();
    PriceMapOfferingEntity offering = new PriceMapOfferingEntity(Instant.now(), offeringId);
    offering.setPriceMap(new PriceMapEntity(Instant.now(), priceMap));
    given(offeringDao.findById(offeringId)).willReturn(Optional.of(offering));

    String facilityServerName = InProcessServerBuilder.generateName();
    URI facilityUri = URI.create("//" + facilityServerName);
    UUID facilityId = UUID.randomUUID();
    FacilityEntity facility = new FacilityEntity(Instant.now(), facilityId);
    facility.setFacilityUid(facilityUid);
    facility.setFacilityPublicKey(facilityKeyPair.getPublic().getEncoded());
    facility.setFacilityEndpointUri(facilityUri.toString());
    given(facilityDao.findAllByFacilityUidIn(singleton(facilityUid)))
        .willReturn(singleton(facility));

    ArgumentCaptor<FacilityPriceMapOfferEntity> offerCaptor = ArgumentCaptor
        .forClass(FacilityPriceMapOfferEntity.class);
    given(priceMapOfferDao.save(offerCaptor.capture()))
        .willAnswer(invocationArg(0, FacilityPriceMapOfferEntity.class));

    ArgumentCaptor<UUID> offerIdCaptor = ArgumentCaptor.forClass(UUID.class);
    given(priceMapOfferDao.findById(offerIdCaptor.capture()))
        .willAnswer(answer(new Answer1<Optional<FacilityPriceMapOfferEntity>, UUID>() {

          @Override
          public Optional<FacilityPriceMapOfferEntity> answer(UUID arg) {
            assertThat("UUID is for offer", arg, equalTo(offerCaptor.getValue().getId()));
            return Optional.of(offerCaptor.getValue());
          }
        }));

    DerFacilityServiceImplBase facilityService = new DerFacilityServiceImplBase() {

      @Override
      public StreamObserver<PriceMapOffer> proposePriceMapOffer(
          StreamObserver<PriceMapOfferResponse> responseObserver) {
        return new StreamObserver<PriceMapOffer>() {

          @Override
          public void onNext(PriceMapOffer request) {
            assertThat("Route provided", request.getRoute(), notNullValue());
            assertThat("Route exchange UID", request.getRoute().getExchangeUid(),
                equalTo(exchangeUid));
            assertThat("Route facility UID", request.getRoute().getFacilityUid(),
                equalTo(facilityUid));
            FacilityPriceMapOfferEntity offer = offerCaptor.getValue();
            ByteBuffer signatureData = ByteBuffer.allocate(offer.signatureMessageBytesSize());
            SignableMessage.addUuidSignatureMessageBytes(signatureData, offer.getId());
            SignableMessage.addInstantSignatureMessageBytes(signatureData, offering.getStartDate());
            priceMap.addSignatureMessageBytes(signatureData);
            // @formatter:off
            validateMessageSignature(CryptoUtils.STANDARD_HELPER,
                request.getRoute().getSignature(), facilityKeyPair, exchangeKeyPair.getPublic(),
                asList(
                    exchangeUid, 
                    facilityUid, 
                    signatureData));
            responseObserver.onNext(PriceMapOfferResponse.newBuilder()
                .setAccept(true)
                .setOfferId(request.getOfferId())
                .setRoute(DerRoute.newBuilder()
                    .setExchangeUid(exchangeUid)
                    .setFacilityUid(facilityUid)
                    .setSignature(generateMessageSignature(CryptoUtils.STANDARD_HELPER, 
                        facilityKeyPair, exchangeKeyPair.getPublic(), asList(
                            exchangeUid,
                            facilityUid,
                            true)))
                    .build())
                .build());
            // @formatter:on
            responseObserver.onCompleted();
          }

          @Override
          public void onError(Throwable t) {
            fail(t.toString());
          }

          @Override
          public void onCompleted() {
            // nothing
          }
        };
      }

    };

    grpcCleanup.register(InProcessServerBuilder.forName(facilityServerName).directExecutor()
        .addService(facilityService).build().start());

    service
        .setFacilityChannelProvider(new StaticInProcessChannelProvider(facilityServerName, true));

    // when
    Set<String> facilityUids = Collections.singleton(facilityUid);
    Future<Iterable<FacilityPriceMapOfferEntity>> future = service.makeOfferToFacilities(offeringId,
        facilityUids);

    // then
    assertThat("Future returned", future, notNullValue());
    Iterable<FacilityPriceMapOfferEntity> results = future.get(1, TimeUnit.MINUTES);
    List<FacilityPriceMapOfferEntity> offerResults = StreamSupport
        .stream(results.spliterator(), false).collect(Collectors.toList());
    assertThat("1 offer per facility", offerResults, hasSize(1));
    FacilityPriceMapOfferEntity offer = offerResults.get(0);
    assertThat("Offer was persisted", offer, sameInstance(offerCaptor.getValue()));
    assertThat("Offer ID", offer.getId(), equalTo(offerIdCaptor.getValue()));
    assertThat("Offer facility", offer.getFacility(), sameInstance(facility));
    assertThat("Offer offering", offer.getOffering(), sameInstance(offering));
    assertThat("Offer is proposed", offer.isProposed(), equalTo(true));
    assertThat("Offer is accepted", offer.isAccepted(), equalTo(true));
    assertThat("Offer is not confirmed", offer.isConfirmed(), equalTo(true));
    assertThat("No counter-offer price map available", offer.getPriceMap(), nullValue());
  }

  @Test
  public void makeOfferWithCounterOffer() throws Exception {
    // given
    PriceMapEmbed priceMap = new PriceMapEmbed();
    priceMap.setPowerComponents(new PowerComponentsEmbed(1L, 2L));
    priceMap.setDuration(Duration.ofMillis(3456L));
    priceMap.setResponseTime(
        new DurationRangeEmbed(Duration.ofMillis(4567L), Duration.ofMillis(5678L)));
    priceMap.setPriceComponents(
        new PriceComponentsEmbed(Currency.getInstance("USD"), new BigDecimal("9.87")));
    UUID offeringId = UUID.randomUUID();
    PriceMapOfferingEntity offering = new PriceMapOfferingEntity(Instant.now(), offeringId);
    offering.setPriceMap(new PriceMapEntity(Instant.now(), priceMap));
    given(offeringDao.findById(offeringId)).willReturn(Optional.of(offering));

    String facilityServerName = InProcessServerBuilder.generateName();
    URI facilityUri = URI.create("//" + facilityServerName);
    UUID facilityId = UUID.randomUUID();
    FacilityEntity facility = new FacilityEntity(Instant.now(), facilityId);
    facility.setFacilityUid(facilityUid);
    facility.setFacilityPublicKey(facilityKeyPair.getPublic().getEncoded());
    facility.setFacilityEndpointUri(facilityUri.toString());
    given(facilityDao.findAllByFacilityUidIn(singleton(facilityUid)))
        .willReturn(singleton(facility));

    ArgumentCaptor<FacilityPriceMapOfferEntity> offerCaptor = ArgumentCaptor
        .forClass(FacilityPriceMapOfferEntity.class);
    given(priceMapOfferDao.save(offerCaptor.capture()))
        .willAnswer(invocationArg(0, FacilityPriceMapOfferEntity.class));

    ArgumentCaptor<UUID> offerIdCaptor = ArgumentCaptor.forClass(UUID.class);
    given(priceMapOfferDao.findById(offerIdCaptor.capture()))
        .willAnswer(answer(new Answer1<Optional<FacilityPriceMapOfferEntity>, UUID>() {

          @Override
          public Optional<FacilityPriceMapOfferEntity> answer(UUID arg) {
            assertThat("UUID is for offer", arg, equalTo(offerCaptor.getValue().getId()));
            return Optional.of(offerCaptor.getValue());
          }
        }));

    DerFacilityServiceImplBase facilityService = new DerFacilityServiceImplBase() {

      @Override
      public StreamObserver<PriceMapOffer> proposePriceMapOffer(
          StreamObserver<PriceMapOfferResponse> responseObserver) {
        return new StreamObserver<PriceMapOffer>() {

          private int offerCount = 0;

          @Override
          public void onNext(PriceMapOffer request) {
            offerCount++;
            assertThat("Route provided", request.getRoute(), notNullValue());
            assertThat("Route exchange UID", request.getRoute().getExchangeUid(),
                equalTo(exchangeUid));
            assertThat("Route facility UID", request.getRoute().getFacilityUid(),
                equalTo(facilityUid));
            if (offerCount == 1) {
              // this is the initial offer, to which we will counter
              FacilityPriceMapOfferEntity offer = offerCaptor.getValue();
              ByteBuffer signatureData = ByteBuffer.allocate(offer.signatureMessageBytesSize());
              SignableMessage.addUuidSignatureMessageBytes(signatureData, offer.getId());
              SignableMessage.addInstantSignatureMessageBytes(signatureData,
                  offering.getStartDate());
              priceMap.addSignatureMessageBytes(signatureData);

              validateMessageSignature(CryptoUtils.STANDARD_HELPER,
                  request.getRoute().getSignature(), facilityKeyPair, exchangeKeyPair.getPublic(),
                  asList(exchangeUid, facilityUid, offer));

              PriceMap.Builder counterOffer = request.getPriceMap().toBuilder();
              counterOffer.getPriceBuilder().getApparentEnergyPriceBuilder().setUnits(999);

              responseObserver.onNext(PriceMapOfferResponse.newBuilder()
                  .setOfferId(request.getOfferId()).setCounterOffer(counterOffer.build())
                  .setRoute(DerRoute.newBuilder().setExchangeUid(exchangeUid)
                      .setFacilityUid(facilityUid)
                      .setSignature(generateMessageSignature(CryptoUtils.STANDARD_HELPER,
                          exchangeKeyPair, facilityKeyPair.getPublic(), asList(exchangeUid,
                              facilityUid, ProtobufUtils.priceMapEmbedValue(counterOffer))))
                      .build())
                  .build());
              // @formatter:on
            } else {
              // re-offer! we'll accept this
              // @formatter:off
              responseObserver.onNext(PriceMapOfferResponse.newBuilder()
                  .setOfferId(request.getOfferId())
                  .setAccept(true)
                  .setRoute(DerRoute.newBuilder()
                      .setExchangeUid(exchangeUid)
                      .setFacilityUid(facilityUid)
                      .setSignature(generateMessageSignature(CryptoUtils.STANDARD_HELPER, 
                          exchangeKeyPair,
                          facilityKeyPair.getPublic(),
                          asList(
                              exchangeUid, 
                              facilityUid, 
                              true)))
                      .build())
                  .build());
              // @formatter:on
              responseObserver.onCompleted();
            }
          }

          @Override
          public void onError(Throwable t) {
            fail(t.toString());
          }

          @Override
          public void onCompleted() {
            // nothing
          }
        };
      }

    };

    grpcCleanup.register(InProcessServerBuilder.forName(facilityServerName).directExecutor()
        .addService(facilityService).build().start());

    service
        .setFacilityChannelProvider(new StaticInProcessChannelProvider(facilityServerName, true));

    // when
    Set<String> facilityUids = Collections.singleton(facilityUid);
    Future<Iterable<FacilityPriceMapOfferEntity>> future = service.makeOfferToFacilities(offeringId,
        facilityUids);

    // then
    assertThat("Future returned", future, notNullValue());
    Iterable<FacilityPriceMapOfferEntity> results = future.get(1, TimeUnit.MINUTES);
    List<FacilityPriceMapOfferEntity> offerResults = StreamSupport
        .stream(results.spliterator(), false).collect(Collectors.toList());
    assertThat("1 offer per facility", offerResults, hasSize(1));
    FacilityPriceMapOfferEntity offer = offerResults.get(0);
    assertThat("Offer was persisted", offer, sameInstance(offerCaptor.getValue()));
    assertThat("Offer ID", offer.getId(), equalTo(offerIdCaptor.getValue()));
    assertThat("Offer facility", offer.getFacility(), sameInstance(facility));
    assertThat("Offer offering", offer.getOffering(), sameInstance(offering));
    assertThat("Offer price map available", offer.getPriceMap(), notNullValue());
    assertThat("Offer is proposed", offer.isProposed(), equalTo(true));
    assertThat("Offer is accepted", offer.isAccepted(), equalTo(true));
    assertThat("Offer is not confirmed", offer.isConfirmed(), equalTo(true));

    PriceMapEmbed counterPriceMap = priceMap.copy();
    counterPriceMap.getPriceComponents().setApparentEnergyPrice(new BigDecimal("999.87"));
    assertThat("Counter-offer price map is extortion", offer.getPriceMap().getPriceMap(),
        equalTo(counterPriceMap));
  }

}
