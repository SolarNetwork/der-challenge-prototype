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
import static java.util.stream.Collectors.toCollection;
import static net.solarnetwork.esi.domain.support.ProtobufUtils.decimalValue;
import static net.solarnetwork.esi.util.CryptoUtils.STANDARD_HELPER;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.protobuf.Empty;

import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import net.solarnetwork.esi.domain.DerCharacteristics;
import net.solarnetwork.esi.domain.DerProgramSet;
import net.solarnetwork.esi.domain.DerProgramType;
import net.solarnetwork.esi.domain.PriceMap;
import net.solarnetwork.esi.domain.PriceMapCharacteristics;
import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.domain.jpa.PowerComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceComponentsEmbed;
import net.solarnetwork.esi.grpc.InProcessChannelProvider;
import net.solarnetwork.esi.grpc.StaticInProcessChannelProvider;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc.DerFacilityExchangeImplBase;
import net.solarnetwork.esi.simple.fac.dao.PriceMapEntityDao;
import net.solarnetwork.esi.simple.fac.dao.ResourceCharacteristicsEntityDao;
import net.solarnetwork.esi.simple.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.simple.fac.domain.PriceMapEntity;
import net.solarnetwork.esi.simple.fac.domain.ResourceCharacteristicsEntity;
import net.solarnetwork.esi.simple.fac.impl.DaoFacilityCharacteristicsService;
import net.solarnetwork.esi.simple.fac.service.FacilityService;
import net.solarnetwork.esi.util.CryptoUtils;

/**
 * Test cases for the {@link DaoFacilityCharacteristicsService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoFacilityCharacteristicsServiceTests {

  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private FacilityService facilityService;
  private String facilityUid;
  private URI facilityUri;
  private KeyPair facilityKeyPair;
  private String exchangeUid;
  private KeyPair exchangeKeyPair;
  private PriceMapEntityDao priceMapDao;
  private ResourceCharacteristicsEntityDao resourceCharacteristicsDao;
  private DaoFacilityCharacteristicsService service;
  private ExchangeEntity exchangeEntity;

  @Before
  public void setup() {
    facilityUid = UUID.randomUUID().toString();
    facilityUri = URI.create("//test-facility");
    facilityKeyPair = STANDARD_HELPER.generateKeyPair();
    facilityService = mock(FacilityService.class);
    exchangeUid = UUID.randomUUID().toString();
    exchangeKeyPair = CryptoUtils.STANDARD_HELPER.generateKeyPair();
    priceMapDao = mock(PriceMapEntityDao.class);
    resourceCharacteristicsDao = mock(ResourceCharacteristicsEntityDao.class);
    service = new DaoFacilityCharacteristicsService(facilityService, priceMapDao,
        resourceCharacteristicsDao);
    service.setExchangeChannelProvider(new InProcessChannelProvider(true));
    exchangeEntity = new ExchangeEntity(Instant.now());
    exchangeEntity.setId(exchangeUid);
    exchangeEntity.setExchangePublicKey(exchangeKeyPair.getPublic().getEncoded());
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
  public void provideDerCharacteristics() throws IOException {
    // given
    String exchangeServerName = InProcessServerBuilder.generateName();
    URI exchangeUri = URI.create("//" + exchangeServerName);
    givenDefaultFacilityService(exchangeUri);

    ResourceCharacteristicsEntity characteristics = new ResourceCharacteristicsEntity(
        Instant.now());
    characteristics.setLoadPowerMax(1L);
    characteristics.setLoadPowerFactor(0.2f);
    characteristics.setSupplyPowerMax(3L);
    characteristics.setSupplyPowerFactor(0.4f);
    characteristics.setStorageEnergyCapacity(5L);
    characteristics
        .setResponseTime(new DurationRangeEmbed(Duration.ofSeconds(6L), Duration.ofSeconds(7L)));

    DerFacilityExchangeImplBase exchangeService = new DerFacilityExchangeImplBase() {

      @Override
      public StreamObserver<DerCharacteristics> provideDerCharacteristics(
          StreamObserver<Empty> responseObserver) {
        return new StreamObserver<DerCharacteristics>() {

          @Override
          public void onNext(DerCharacteristics value) {
            // @formatter:off
            CryptoUtils.validateMessageSignature(CryptoUtils.STANDARD_HELPER,
                value.getRoute().getSignature(), exchangeKeyPair, facilityKeyPair.getPublic(),
                asList(
                    exchangeUid, 
                    facilityUid,
                    characteristics));
            // @formatter:on
            assertThat("Load power max", value.getLoadPowerMax(),
                equalTo(characteristics.getLoadPowerMax()));
            assertThat("Load power factor", value.getLoadPowerFactor(),
                equalTo(characteristics.getLoadPowerFactor()));
            assertThat("Supply power max", value.getSupplyPowerMax(),
                equalTo(characteristics.getSupplyPowerMax()));
            assertThat("Supply power factor", value.getSupplyPowerFactor(),
                equalTo(characteristics.getSupplyPowerFactor()));
            assertThat("Storage energy capcity", value.getStorageEnergyCapacity(),
                equalTo(characteristics.getStorageEnergyCapacity()));
            assertThat("Respones time min", value.getResponseTime().getMin().getSeconds(),
                equalTo(characteristics.getResponseTime().getMin().getSeconds()));
            assertThat("Respones time max", value.getResponseTime().getMax().getSeconds(),
                equalTo(characteristics.getResponseTime().getMax().getSeconds()));
          }

          @Override
          public void onError(Throwable t) {
            fail(t.toString());
          }

          @Override
          public void onCompleted() {
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
          }
        };
      }

    };

    grpcCleanup.register(InProcessServerBuilder.forName(exchangeServerName).directExecutor()
        .addService(exchangeService).build().start());

    service
        .setExchangeChannelProvider(new StaticInProcessChannelProvider(exchangeServerName, true));

    // when
    service.saveResourceCharacteristics(characteristics);

    // then
    verify(resourceCharacteristicsDao, times(1)).save(characteristics);
  }

  @Test
  public void provideSupportedDerPrograms() throws IOException {
    // given
    String exchangeServerName = InProcessServerBuilder.generateName();
    URI exchangeUri = URI.create("//" + exchangeServerName);
    givenDefaultFacilityService(exchangeUri);

    List<DerProgramType> types = Arrays.asList(DerProgramType.ARTIFICIAL_INERTIA,
        DerProgramType.PEAK_CAPACITY_MANAGEMENT);

    ByteBuffer signatureData = ByteBuffer.allocate(Integer.BYTES * 2);
    types.forEach(e -> signatureData.putInt(e.getNumber()));

    DerFacilityExchangeImplBase exchangeService = new DerFacilityExchangeImplBase() {

      @Override
      public StreamObserver<DerProgramSet> provideSupportedDerPrograms(
          StreamObserver<Empty> responseObserver) {
        return new StreamObserver<DerProgramSet>() {

          @Override
          public void onNext(DerProgramSet value) {
            // @formatter:off
            CryptoUtils.validateMessageSignature(CryptoUtils.STANDARD_HELPER,
                value.getRoute().getSignature(), exchangeKeyPair, facilityKeyPair.getPublic(),
                asList(
                    exchangeUid, 
                    facilityUid,
                    signatureData));
            // @formatter:on
            assertThat("Program types count", value.getTypeCount(), equalTo(2));
            assertThat("Progame types", value.getTypeList(), equalTo(types));
          }

          @Override
          public void onError(Throwable t) {
            fail(t.toString());
          }

          @Override
          public void onCompleted() {
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
          }
        };
      }

    };

    grpcCleanup.register(InProcessServerBuilder.forName(exchangeServerName).directExecutor()
        .addService(exchangeService).build().start());

    service
        .setExchangeChannelProvider(new StaticInProcessChannelProvider(exchangeServerName, true));

    // when
    Set<String> programs = types.stream().map(DerProgramType::name)
        .collect(toCollection(LinkedHashSet::new));
    service.saveActiveProgramTypes(programs);

    // then
    verify(facilityService, times(1)).setEnabledProgramTypes(programs);
  }

  @Test
  public void savePriceMap() throws IOException {
    // given
    String exchangeServerName = InProcessServerBuilder.generateName();
    URI exchangeUri = URI.create("//" + exchangeServerName);
    givenDefaultFacilityService(exchangeUri);

    PriceMapEntity priceMap = new PriceMapEntity(Instant.now());
    priceMap.setPowerComponents(new PowerComponentsEmbed(1L, 2L));
    priceMap.setDuration(Duration.ofMillis(12345L));
    priceMap.setResponseTime(
        new DurationRangeEmbed(Duration.ofMillis(2456L), Duration.ofMillis(4567L)));
    priceMap.setPriceComponents(
        new PriceComponentsEmbed(Currency.getInstance("USD"), new BigDecimal("234.23456")));

    given(facilityService.getPriceMaps()).willReturn(Collections.singleton(priceMap));

    DerFacilityExchangeImplBase exchangeService = new DerFacilityExchangeImplBase() {

      @Override
      public StreamObserver<PriceMapCharacteristics> providePriceMaps(
          StreamObserver<Empty> responseObserver) {
        return new StreamObserver<PriceMapCharacteristics>() {

          @Override
          public void onNext(PriceMapCharacteristics value) {
            // @formatter:off
            CryptoUtils.validateMessageSignature(CryptoUtils.STANDARD_HELPER,
                value.getRoute().getSignature(), exchangeKeyPair, facilityKeyPair.getPublic(),
                asList(
                    exchangeUid, 
                    facilityUid,
                    priceMap));
            // @formatter:on
            PriceMap pm = value.getPriceMap(0);
            assertThat("Real power", pm.getPowerComponents().getRealPower(),
                equalTo(priceMap.getPowerComponents().getRealPower()));
            assertThat("Reactive power", pm.getPowerComponents().getReactivePower(),
                equalTo(priceMap.getPowerComponents().getReactivePower()));
            assertThat("Duration seconds", pm.getDuration().getSeconds(),
                equalTo(priceMap.getDuration().getSeconds()));
            assertThat("Duration nanos", pm.getDuration().getNanos(),
                equalTo(priceMap.getDuration().getNano()));
            assertThat("Response time min seconds", pm.getResponseTime().getMin().getSeconds(),
                equalTo(priceMap.getResponseTime().getMin().getSeconds()));
            assertThat("Response time min nanos", pm.getResponseTime().getMin().getNanos(),
                equalTo(priceMap.getResponseTime().getMin().getNano()));
            assertThat("Response time max seconds", pm.getResponseTime().getMax().getSeconds(),
                equalTo(priceMap.getResponseTime().getMax().getSeconds()));
            assertThat("Response time max nanos", pm.getResponseTime().getMax().getNanos(),
                equalTo(priceMap.getResponseTime().getMax().getNano()));
            assertThat("Apparent energy price currency code",
                pm.getPrice().getApparentEnergyPrice().getCurrencyCode(),
                equalTo(priceMap.getPriceComponents().getCurrency().getCurrencyCode()));
            assertThat("Apparent energy price",
                decimalValue(pm.getPrice().getApparentEnergyPrice()),
                equalTo(priceMap.getPriceComponents().getApparentEnergyPrice()));
          }

          @Override
          public void onError(Throwable t) {
            fail(t.toString());
          }

          @Override
          public void onCompleted() {
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
          }
        };
      }

    };

    grpcCleanup.register(InProcessServerBuilder.forName(exchangeServerName).directExecutor()
        .addService(exchangeService).build().start());

    service
        .setExchangeChannelProvider(new StaticInProcessChannelProvider(exchangeServerName, true));

    // when
    service.savePriceMap(priceMap);

    // then
    verify(facilityService, times(1)).savePriceMap(priceMap);
  }

}
