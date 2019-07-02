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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.type.Money;

import io.grpc.testing.GrpcCleanupRule;
import net.solarnetwork.esi.domain.DerCharacteristics;
import net.solarnetwork.esi.domain.DerProgramSet;
import net.solarnetwork.esi.domain.DerProgramType;
import net.solarnetwork.esi.domain.DerRoute;
import net.solarnetwork.esi.domain.DurationRange;
import net.solarnetwork.esi.domain.PowerComponents;
import net.solarnetwork.esi.domain.PriceComponents;
import net.solarnetwork.esi.domain.PriceMap;
import net.solarnetwork.esi.domain.PriceMapCharacteristics;
import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.domain.jpa.PowerComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceComponentsEmbed;
import net.solarnetwork.esi.simple.xchg.dao.FacilityEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.FacilityResourceCharacteristicsEntityDao;
import net.solarnetwork.esi.simple.xchg.domain.FacilityEntity;
import net.solarnetwork.esi.simple.xchg.domain.FacilityResourceCharacteristicsEntity;
import net.solarnetwork.esi.simple.xchg.domain.PriceMapEntity;
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
    FacilityEntity facility = new FacilityEntity(Instant.now(), UUID.fromString(facilityUid));
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
        .setResponseTime(DurationRange.newBuilder()
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
                        derCharacteristicsBuilder)))
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
        equalTo(derCharacteristics.getResponseTime().getMin().getSeconds()));
    assertThat("Response time max", result.getResponseTime().getMax().getSeconds(),
        equalTo(derCharacteristics.getResponseTime().getMax().getSeconds()));
  }

  @Test
  public void updateResourceCharacteristicsForFacility() {
    // given
    FacilityEntity facility = new FacilityEntity(Instant.now(), UUID.fromString(facilityUid));
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
        .setResponseTime(DurationRange.newBuilder()
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
                        derCharacteristicsBuilder)))
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
        equalTo(derCharacteristics.getResponseTime().getMin().getSeconds()));
    assertThat("Response time max", result.getResponseTime().getMax().getSeconds(),
        equalTo(derCharacteristics.getResponseTime().getMax().getSeconds()));
  }

  @Test
  public void programTypesForFacility() {
    // given
    Set<DerProgramType> programTypes = new LinkedHashSet<>(
        asList(DerProgramType.ARTIFICIAL_INERTIA, DerProgramType.PEAK_CAPACITY_MANAGEMENT));
    Set<String> programs = programTypes.stream().map(DerProgramType::name)
        .collect(Collectors.toSet());
    FacilityEntity facility = new FacilityEntity(Instant.now(), UUID.fromString(facilityUid));
    facility.setFacilityUid(facilityUid);
    facility.setFacilityPublicKey(facilityKeyPair.getPublic().getEncoded());
    facility.setProgramTypes(programs);
    given(facilityDao.findByFacilityUid(facilityUid)).willReturn(Optional.of(facility));

    // when
    Set<DerProgramType> types = service.activeProgramTypes(facilityUid);

    // then
    assertThat("Returned types", types, equalTo(programTypes));
  }

  @Test
  public void setProgramTypesForFacility() {
    // given
    List<DerProgramType> programTypes = asList(DerProgramType.ARTIFICIAL_INERTIA,
        DerProgramType.PEAK_CAPACITY_MANAGEMENT);
    ByteBuffer signatureData = ByteBuffer.allocate(Integer.BYTES * programTypes.size());
    programTypes.stream().forEachOrdered(e -> signatureData.putInt(e.getNumber()));

    FacilityEntity facility = new FacilityEntity(Instant.now(), UUID.fromString(facilityUid));
    facility.setFacilityUid(facilityUid);
    facility.setFacilityPublicKey(facilityKeyPair.getPublic().getEncoded());
    facility.setProgramTypes(new HashSet<>());
    given(facilityDao.findByFacilityUid(facilityUid)).willReturn(Optional.of(facility));

    ArgumentCaptor<FacilityEntity> facilityCaptor = ArgumentCaptor.forClass(FacilityEntity.class);
    given(facilityDao.save(facilityCaptor.capture()))
        .willAnswer(invocationArg(0, FacilityEntity.class));

    // when
    // @formatter:off
    DerProgramSet programSet = DerProgramSet.newBuilder()
        .addAllType(programTypes)
        .setRoute(DerRoute.newBuilder()
            .setExchangeUid(exchangeUid)
            .setFacilityUid(facilityUid)
            .setSignature(generateMessageSignature(CryptoUtils.STANDARD_HELPER, 
                facilityKeyPair, exchangeKeyPair.getPublic(), asList(
                    exchangeUid,
                    facilityUid,
                    signatureData))
                )
            .build())
        .build();
    // @formatter:on
    service.saveActiveProgramTypes(programSet);

    // then
    assertThat("Facility same as persisted", facility, sameInstance(facilityCaptor.getValue()));
    assertThat("Facility programs", facility.getProgramTypes(),
        equalTo(programTypes.stream().map(e -> e.name()).collect(Collectors.toSet())));
  }

  @Test
  public void priceMapForFacility() {
    // given
    FacilityEntity facility = new FacilityEntity(Instant.now());
    PriceMapEntity priceMap = new PriceMapEntity(Instant.now());
    facility.setPriceMap(priceMap);

    given(facilityDao.findByFacilityUid(facilityUid)).willReturn(Optional.of(facility));

    // when
    PriceMapEntity result = service.priceMap(facilityUid);

    // then
    assertThat("Result available", result, equalTo(priceMap));
    assertThat("Result is copy", result, not(sameInstance(priceMap)));
  }

  @Test
  public void priceMapForFacilityNotFound() {
    // given
    FacilityEntity facility = new FacilityEntity(Instant.now());

    given(facilityDao.findByFacilityUid(facilityUid)).willReturn(Optional.of(facility));

    // when
    PriceMapEntity result = service.priceMap(facilityUid);

    // then
    assertThat("Result not available", result, nullValue());
  }

  @Test
  public void createPriceMapForFacility() {
    // given
    FacilityEntity facility = new FacilityEntity(Instant.now(), UUID.fromString(facilityUid));
    facility.setFacilityUid(facilityUid);
    facility.setFacilityPublicKey(facilityKeyPair.getPublic().getEncoded());

    given(facilityDao.findByFacilityUid(facilityUid)).willReturn(Optional.of(facility));

    ArgumentCaptor<FacilityEntity> facilityCaptor = ArgumentCaptor.forClass(FacilityEntity.class);
    given(facilityDao.save(facilityCaptor.capture()))
        .willAnswer(invocationArg(0, FacilityEntity.class));

    // when
    // @formatter:off
    PriceMapCharacteristics.Builder priceMapBuilder = PriceMapCharacteristics.newBuilder()
        .setPriceMap(PriceMap.newBuilder()
            .setPowerComponents(PowerComponents.newBuilder()
                .setRealPower(1L)
                .setReactivePower(2L)
                .build())
            .setDuration(com.google.protobuf.Duration.newBuilder()
                .setSeconds(1234L)
                .setNanos(456000000)
                .build())
            .setResponseTime(DurationRange.newBuilder()
                .setMin(com.google.protobuf.Duration.newBuilder()
                    .setSeconds(234L)
                    .setNanos(567000000)
                    .build())
                .setMax(com.google.protobuf.Duration.newBuilder()
                    .setSeconds(345L)
                    .setNanos(678000000)
                    .build())
                .build())
            .setPrice(PriceComponents.newBuilder()
                .setRealEnergyPrice(Money.newBuilder()
                    .setCurrencyCode("USD")
                    .setUnits(9L)
                    .setNanos(99)
                    .build())
                .setApparentEnergyPrice(Money.newBuilder()
                    .setCurrencyCode("USD")
                    .setUnits(99L)
                    .setNanos(88)
                    .build())
                .build())
            .build());
    
    PriceMapCharacteristics priceMapMessage = priceMapBuilder
        .setRoute(DerRoute.newBuilder()
            .setExchangeUid(exchangeUid)
            .setFacilityUid(facilityUid)
            .setSignature(generateMessageSignature(CryptoUtils.STANDARD_HELPER, 
                facilityKeyPair, exchangeKeyPair.getPublic(), asList(
                    exchangeUid,
                    facilityUid,
                    PriceMapEntity.entityForMessage(
                        priceMapBuilder.getPriceMapOrBuilder())))
                )
            .build())
        .build();
    // @formatter:on
    service.savePriceMap(priceMapMessage);

    // then
    assertThat("Facility persisted", facilityCaptor.getValue(), sameInstance(facility));

    PriceMapEntity entity = facilityCaptor.getValue().getPriceMap();
    assertThat("Facility persisted with price map", entity, notNullValue());

    assertThat("Power components", entity.getPowerComponents(),
        equalTo(new PowerComponentsEmbed(1L, 2L)));
    assertThat("Duration", entity.getDuration(), equalTo(Duration.ofSeconds(1234L, 456000000)));
    assertThat("Response time", entity.getResponseTime(),
        equalTo(new DurationRangeEmbed(Duration.ofSeconds(234L, 567000000),
            Duration.ofSeconds(345L, 678000000))));
    assertThat("Price components", entity.getPriceComponents().scaledExactly(2),
        equalTo(new PriceComponentsEmbed(Currency.getInstance("USD"), new BigDecimal("9.99"),
            new BigDecimal("99.88")).scaledExactly(2)));
  }

}
