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
import static net.solarnetwork.esi.util.CryptoUtils.STANDARD_HELPER;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.protobuf.Empty;

import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import net.solarnetwork.esi.domain.DerCharacteristics;
import net.solarnetwork.esi.domain.DurationRangeEmbed;
import net.solarnetwork.esi.grpc.InProcessChannelProvider;
import net.solarnetwork.esi.grpc.StaticInProcessChannelProvider;
import net.solarnetwork.esi.service.DerFacilityExchangeGrpc.DerFacilityExchangeImplBase;
import net.solarnetwork.esi.simple.fac.dao.ResourceCharacteristicsEntityDao;
import net.solarnetwork.esi.simple.fac.domain.ExchangeEntity;
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
    resourceCharacteristicsDao = mock(ResourceCharacteristicsEntityDao.class);
    service = new DaoFacilityCharacteristicsService(facilityService, resourceCharacteristicsDao);
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
                    characteristics.toSignatureBytes()));
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
  }
}
