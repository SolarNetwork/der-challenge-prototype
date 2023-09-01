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

package net.solarnetwork.esi.solarnet.fac.impl.test;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static net.solarnetwork.esi.domain.support.ProtobufUtils.decimalValue;
import static net.solarnetwork.esi.util.CryptoUtils.STANDARD_HELPER;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.protobuf.Empty;

import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import net.solarnetwork.esi.domain.PriceMap;
import net.solarnetwork.esi.domain.PriceMapCharacteristics;
import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.domain.jpa.PowerComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceComponentsEmbed;
import net.solarnetwork.esi.grpc.InProcessChannelProvider;
import net.solarnetwork.esi.grpc.StaticInProcessChannelProvider;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc.DerFacilityExchangeImplBase;
import net.solarnetwork.esi.solarnet.fac.dao.FacilityPriceMapDao;
import net.solarnetwork.esi.solarnet.fac.dao.FacilityResourceDao;
import net.solarnetwork.esi.solarnet.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityPriceMap;
import net.solarnetwork.esi.solarnet.fac.impl.DaoFacilityCharacteristicsService;
import net.solarnetwork.esi.solarnet.fac.service.FacilityService;
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
  private FacilityPriceMapDao priceMapDao;
  private FacilityResourceDao resourceCharacteristicsDao;
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
    priceMapDao = mock(FacilityPriceMapDao.class);
    resourceCharacteristicsDao = mock(FacilityResourceDao.class);
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
  public void registerPriceMap() throws IOException {
    // given
    String exchangeServerName = InProcessServerBuilder.generateName();
    URI exchangeUri = URI.create("//" + exchangeServerName);
    givenDefaultFacilityService(exchangeUri);

    FacilityPriceMap priceMap = new FacilityPriceMap(UUID.randomUUID().toString());
    priceMap.setPowerComponents(new PowerComponentsEmbed(1L, 2L));
    priceMap.setDuration(Duration.ofMillis(12345L));
    priceMap.setResponseTime(
        new DurationRangeEmbed(Duration.ofMillis(2456L), Duration.ofMillis(4567L)));
    priceMap.setPriceComponents(
        new PriceComponentsEmbed(Currency.getInstance("USD"), new BigDecimal("234.23456")));

    given(facilityService.getPriceMaps()).willReturn(singleton(priceMap));

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
    service.registerPriceMap(priceMap);

    // then
  }

}
