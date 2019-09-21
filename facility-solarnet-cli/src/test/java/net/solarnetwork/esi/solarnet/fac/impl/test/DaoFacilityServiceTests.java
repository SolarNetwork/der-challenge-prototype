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
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import net.solarnetwork.esi.domain.DerProgramType;
import net.solarnetwork.esi.solarnet.fac.dao.ExchangeEntityDao;
import net.solarnetwork.esi.solarnet.fac.dao.FacilityPriceMapDao;
import net.solarnetwork.esi.solarnet.fac.dao.FacilityProgramDao;
import net.solarnetwork.esi.solarnet.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityProgram;
import net.solarnetwork.esi.solarnet.fac.impl.DaoFacilityService;
import net.solarnetwork.esi.util.CryptoUtils;

/**
 * Test cases for the {@link DaoFacilityService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoFacilityServiceTests {

  private static final String TEST_UID = UUID.randomUUID().toString();
  private static final URI TEST_URI = URI.create("//test-exchange");

  private ExchangeEntityDao exchangeDao;
  private FacilityProgramDao programDao;
  private FacilityPriceMapDao priceMapDao;
  private KeyPair exchangeKeyPair;
  private DaoFacilityService service;

  @Before
  public void setup() {
    exchangeDao = mock(ExchangeEntityDao.class);
    programDao = mock(FacilityProgramDao.class);
    priceMapDao = mock(FacilityPriceMapDao.class);
    exchangeKeyPair = CryptoUtils.STANDARD_HELPER.generateKeyPair();
    service = new DaoFacilityService(TEST_UID, TEST_URI, true, exchangeKeyPair,
        CryptoUtils.STANDARD_HELPER, exchangeDao, programDao, priceMapDao);
  }

  @Test
  public void uid() {
    assertThat("UID", service.getUid(), equalTo(TEST_UID));
  }

  @Test
  public void uri() {
    assertThat("URI", service.getUri(), equalTo(TEST_URI));
  }

  @Test
  public void keyPair() {
    assertThat("Key pair", service.getKeyPair(), sameInstance(exchangeKeyPair));
  }

  @Test
  public void cryptoHelper() {
    assertThat("Crypto helper", service.getCryptoHelper(),
        sameInstance(CryptoUtils.STANDARD_HELPER));
  }

  @Test
  public void exchangeMissing() {
    // given
    ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
    given(exchangeDao.findAll(pageCaptor.capture())).willReturn(new PageImpl<>(emptyList()));

    // when
    ExchangeEntity result = service.getExchange();

    // then
    assertThat("Exchange not avaiable", result, nullValue());
  }

  private void assertFindFirstByProperty(Pageable page, String property, Direction direction) {
    assertThat("Page offset", page.getOffset(), equalTo(0L));
    assertThat("Page size", page.getPageSize(), equalTo(1));
    List<Sort.Order> orders = page.getSort().stream().collect(toList());
    assertThat("Sort orders", orders, hasSize(1));
    assertThat("Sort by created", orders.get(0).getProperty(), equalTo(property));
    assertThat("Sort direction", orders.get(0).getDirection(), equalTo(direction));
  }

  @Test
  public void exchangeAvailable() {
    // given
    ExchangeEntity exchange = new ExchangeEntity(Instant.now());
    ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
    given(exchangeDao.findAll(pageCaptor.capture())).willReturn(new PageImpl<>(asList(exchange)));

    // when
    ExchangeEntity result = service.getExchange();

    // then
    assertThat("Exchange avaiable", result, sameInstance(exchange));
    assertFindFirstByProperty(pageCaptor.getValue(), "created", Direction.DESC);
  }

  @Test
  public void getProgramTypesNoSettings() {
    // given
    // TODO

    // when
    Set<String> programs = service.getEnabledProgramTypes();

    // then
    assertThat("Programs empty", programs, hasSize(0));
  }

  @Test
  public void getProgramTypesEmptySettings() {
    // given
    // TODO

    // when
    Set<String> programs = service.getEnabledProgramTypes();

    // then
    assertThat("Programs empty", programs, hasSize(0));
  }

  @Test
  public void getProgramTypesWithSettings() {
    // given
    List<FacilityProgram> facPrograms = Arrays.asList(
        new FacilityProgram(DerProgramType.ARTIFICIAL_INERTIA),
        new FacilityProgram(DerProgramType.VOLTAGE_MANAGEMENT));
    given(programDao.findAll()).willReturn(facPrograms);

    // when
    Set<String> programs = service.getEnabledProgramTypes();

    // then
    Set<String> expectedPrograms = facPrograms.stream().map(p -> p.getProgramType().name())
        .collect(toSet());
    assertThat("Programs", programs, equalTo(expectedPrograms));
  }

}
