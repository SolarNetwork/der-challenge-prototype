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
import static net.solarnetwork.esi.simple.xchg.test.TestUtils.invocationArg;
import static net.solarnetwork.esi.util.CryptoUtils.STANDARD_HELPER;
import static net.solarnetwork.esi.util.CryptoUtils.generateMessageSignature;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import io.grpc.testing.GrpcCleanupRule;
import net.solarnetwork.esi.domain.DerCharacteristics;
import net.solarnetwork.esi.domain.DerRoute;
import net.solarnetwork.esi.domain.DurationRange;
import net.solarnetwork.esi.domain.DurationRangeEmbed;
import net.solarnetwork.esi.simple.xchg.dao.FacilityEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.FacilityResourceCharacteristicsEntityDao;
import net.solarnetwork.esi.simple.xchg.domain.FacilityEntity;
import net.solarnetwork.esi.simple.xchg.domain.FacilityResourceCharacteristicsEntity;
import net.solarnetwork.esi.simple.xchg.impl.DaoFacilityCharacteristicsService;
import net.solarnetwork.esi.util.CryptoUtils;

/**
 * Test cases for the {@link DaoFacilityCharacteristicsService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoFacilityCharacteristicsServiceTests {

  private String exchangeUid;
  private KeyPair exchangeKeyPair;
  private String facilityUid;
  private KeyPair facilityKeyPair;
  private DaoFacilityCharacteristicsService service;
  private FacilityEntityDao facilityDao;
  private FacilityResourceCharacteristicsEntityDao resourceCharacteristicsDao;

  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  @Before
  public void setUp() throws Exception {

    exchangeUid = UUID.randomUUID().toString();
    exchangeKeyPair = STANDARD_HELPER.generateKeyPair();
    facilityUid = UUID.randomUUID().toString();
    facilityKeyPair = STANDARD_HELPER.generateKeyPair();
    facilityDao = mock(FacilityEntityDao.class);
    resourceCharacteristicsDao = mock(FacilityResourceCharacteristicsEntityDao.class);

    service = new DaoFacilityCharacteristicsService(exchangeUid, exchangeKeyPair,
        CryptoUtils.STANDARD_HELPER);
    service.setFacilityDao(facilityDao);
    service.setResourceCharacteristicsDao(resourceCharacteristicsDao);
  }

  @Test
  public void resourceCharacteristicsForFacility() {
    // given
    String facilityUid = UUID.randomUUID().toString();

    // CHECKSTYLE IGNORE LineLength FOR NEXT 2 LINES
    FacilityResourceCharacteristicsEntity characteristics = new FacilityResourceCharacteristicsEntity(
        Instant.now());

    given(resourceCharacteristicsDao.findByFacility_FacilityUid(facilityUid))
        .willReturn(Optional.of(characteristics));

    // when
    FacilityResourceCharacteristicsEntity result = service.resourceCharacteristics(facilityUid);

    // then
    assertThat("Result available", result, sameInstance(characteristics));
  }

  @Test
  public void resourceCharacteristicsForFacilityNotFound() {
    // given
    String facilityUid = UUID.randomUUID().toString();

    given(resourceCharacteristicsDao.findByFacility_FacilityUid(facilityUid))
        .willReturn(Optional.ofNullable(null));

    // when
    FacilityResourceCharacteristicsEntity result = service.resourceCharacteristics(facilityUid);

    // then
    assertThat("Result not available", result, nullValue());
  }

  @Test
  public void createResourceCharacteristicsForFacility() {
    // given
    FacilityEntity facility = new FacilityEntity(Instant.now(), UUID.randomUUID());
    facility.setFacilityUid(facilityUid);
    facility.setFacilityPublicKey(facilityKeyPair.getPublic().getEncoded());
    given(facilityDao.findByFacilityUid(facilityUid)).willReturn(Optional.of(facility));

    ArgumentCaptor<FacilityResourceCharacteristicsEntity> characteristicsCaptor = ArgumentCaptor
        .forClass(FacilityResourceCharacteristicsEntity.class);
    given(resourceCharacteristicsDao.save(characteristicsCaptor.capture()))
        .willAnswer(invocationArg(0, FacilityResourceCharacteristicsEntity.class));

    // when
    // @formatter:off
    DerCharacteristics.Builder derCharacteristicsBuilder = DerCharacteristics.newBuilder()
        .setLoadPowerMax(1L)
        .setLoadPowerFactor(0.2f)
        .setSupplyPowerMax(3L)
        .setSupplyPowerFactor(0.4f)
        .setStorageEnergyCapacity(5L)
        .setReponseTime(DurationRange.newBuilder()
            .setMin(com.google.protobuf.Duration.newBuilder()
                .setSeconds(6L)
                .build())
            .setMax(com.google.protobuf.Duration.newBuilder()
                .setSeconds(7L)
                .build())
            .build());
    DerCharacteristics derCharacteristics = derCharacteristicsBuilder
        .setRoute(DerRoute.newBuilder()
            .setExchangeUid(exchangeUid)
            .setFacilityUid(facilityUid)
            .setSignature(generateMessageSignature(CryptoUtils.STANDARD_HELPER, 
                facilityKeyPair, exchangeKeyPair.getPublic(), asList(
                    exchangeUid,
                    facilityUid,
                    FacilityResourceCharacteristicsEntity.entityForMessage(
                        derCharacteristicsBuilder).toSignatureBytes()))
                )
            .build())
        .build();
    // @formatter:on
    FacilityResourceCharacteristicsEntity result = service
        .saveResourceCharacteristics(derCharacteristics);

    // then
    assertThat("Result available", result, notNullValue());
    assertThat("Result same as persisted", result, sameInstance(characteristicsCaptor.getValue()));
    assertThat("Load power max", result.getLoadPowerMax(),
        equalTo(derCharacteristics.getLoadPowerMax()));
    assertThat("Load power factor", result.getLoadPowerFactor(),
        equalTo(derCharacteristics.getLoadPowerFactor()));
    assertThat("Supply power max", result.getSupplyPowerMax(),
        equalTo(derCharacteristics.getSupplyPowerMax()));
    assertThat("Supply power factor", result.getSupplyPowerFactor(),
        equalTo(derCharacteristics.getSupplyPowerFactor()));
    assertThat("Storage energy capacity", result.getStorageEnergyCapacity(),
        equalTo(derCharacteristics.getStorageEnergyCapacity()));
    assertThat("Response time min", result.getResponseTime().getMin().getSeconds(),
        equalTo(derCharacteristics.getReponseTime().getMin().getSeconds()));
    assertThat("Response time max", result.getResponseTime().getMax().getSeconds(),
        equalTo(derCharacteristics.getReponseTime().getMax().getSeconds()));
  }

  @Test
  public void updateResourceCharacteristicsForFacility() {
    // given
    FacilityEntity facility = new FacilityEntity(Instant.now(), UUID.randomUUID());
    facility.setFacilityUid(facilityUid);
    facility.setFacilityPublicKey(facilityKeyPair.getPublic().getEncoded());

    // CHECKSTYLE IGNORE LineLength FOR NEXT 2 LINES
    FacilityResourceCharacteristicsEntity resourceCharacteristics = new FacilityResourceCharacteristicsEntity(
        Instant.now(), facility);
    resourceCharacteristics.setLoadPowerMax(10L);
    resourceCharacteristics.setLoadPowerMax(11L);
    resourceCharacteristics.setLoadPowerFactor(1.2f);
    resourceCharacteristics.setSupplyPowerMax(13L);
    resourceCharacteristics.setSupplyPowerFactor(1.4f);
    resourceCharacteristics.setStorageEnergyCapacity(15L);
    resourceCharacteristics
        .setResponseTime(new DurationRangeEmbed(Duration.ofSeconds(16L), Duration.ofSeconds(17L)));

    given(resourceCharacteristicsDao.findByFacility_FacilityUid(facilityUid))
        .willReturn(Optional.of(resourceCharacteristics));

    ArgumentCaptor<FacilityResourceCharacteristicsEntity> characteristicsCaptor = ArgumentCaptor
        .forClass(FacilityResourceCharacteristicsEntity.class);
    given(resourceCharacteristicsDao.save(characteristicsCaptor.capture()))
        .willAnswer(invocationArg(0, FacilityResourceCharacteristicsEntity.class));

    // when
    // @formatter:off
    DerCharacteristics.Builder derCharacteristicsBuilder = DerCharacteristics.newBuilder()
        .setLoadPowerMax(1L)
        .setLoadPowerFactor(0.2f)
        .setSupplyPowerMax(3L)
        .setSupplyPowerFactor(0.4f)
        .setStorageEnergyCapacity(5L)
        .setReponseTime(DurationRange.newBuilder()
            .setMin(com.google.protobuf.Duration.newBuilder()
                .setSeconds(6L)
                .build())
            .setMax(com.google.protobuf.Duration.newBuilder()
                .setSeconds(7L)
                .build())
            .build());
    DerCharacteristics derCharacteristics = derCharacteristicsBuilder
        .setRoute(DerRoute.newBuilder()
            .setExchangeUid(exchangeUid)
            .setFacilityUid(facilityUid)
            .setSignature(generateMessageSignature(CryptoUtils.STANDARD_HELPER, 
                facilityKeyPair, exchangeKeyPair.getPublic(), asList(
                    exchangeUid,
                    facilityUid,
                    FacilityResourceCharacteristicsEntity.entityForMessage(
                        derCharacteristicsBuilder).toSignatureBytes()))
                )
            .build())
        .build();
    // @formatter:on
    FacilityResourceCharacteristicsEntity result = service
        .saveResourceCharacteristics(derCharacteristics);

    // then
    assertThat("Result available", result, notNullValue());
    assertThat("Result same as persisted", result, sameInstance(characteristicsCaptor.getValue()));
    assertThat("Result is existing entity", result, sameInstance(resourceCharacteristics));
    assertThat("Load power max", result.getLoadPowerMax(),
        equalTo(derCharacteristics.getLoadPowerMax()));
    assertThat("Load power factor", result.getLoadPowerFactor(),
        equalTo(derCharacteristics.getLoadPowerFactor()));
    assertThat("Supply power max", result.getSupplyPowerMax(),
        equalTo(derCharacteristics.getSupplyPowerMax()));
    assertThat("Supply power factor", result.getSupplyPowerFactor(),
        equalTo(derCharacteristics.getSupplyPowerFactor()));
    assertThat("Storage energy capacity", result.getStorageEnergyCapacity(),
        equalTo(derCharacteristics.getStorageEnergyCapacity()));
    assertThat("Response time min", result.getResponseTime().getMin().getSeconds(),
        equalTo(derCharacteristics.getReponseTime().getMin().getSeconds()));
    assertThat("Response time max", result.getResponseTime().getMax().getSeconds(),
        equalTo(derCharacteristics.getReponseTime().getMax().getSeconds()));
  }
}
