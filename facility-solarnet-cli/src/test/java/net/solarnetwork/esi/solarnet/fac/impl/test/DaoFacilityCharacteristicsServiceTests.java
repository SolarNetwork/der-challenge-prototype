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

import static net.solarnetwork.esi.util.CryptoUtils.STANDARD_HELPER;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.time.Instant;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.grpc.testing.GrpcCleanupRule;
import net.solarnetwork.esi.grpc.InProcessChannelProvider;
import net.solarnetwork.esi.solarnet.fac.dao.FacilityPriceMapDao;
import net.solarnetwork.esi.solarnet.fac.dao.FacilityResourceDao;
import net.solarnetwork.esi.solarnet.fac.domain.ExchangeEntity;
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
  public void provideDerCharacteristics() throws IOException {
    // TODO
  }

  @Test
  public void provideSupportedDerPrograms() throws IOException {
    // TODO
  }

}
