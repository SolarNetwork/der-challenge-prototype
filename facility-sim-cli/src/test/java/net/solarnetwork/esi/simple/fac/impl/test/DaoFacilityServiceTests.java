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
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static net.solarnetwork.esi.simple.fac.test.TestUtils.invocationArg;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.security.KeyPair;
import java.time.Instant;
import java.util.HashSet;
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

import net.solarnetwork.esi.simple.fac.dao.ExchangeEntityDao;
import net.solarnetwork.esi.simple.fac.dao.FacilitySettingsEntityDao;
import net.solarnetwork.esi.simple.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.simple.fac.domain.FacilitySettingsEntity;
import net.solarnetwork.esi.simple.fac.impl.DaoFacilityService;
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
  private FacilitySettingsEntityDao settingsDao;
  private KeyPair exchangeKeyPair;
  private DaoFacilityService service;

  @Before
  public void setup() {
    exchangeDao = mock(ExchangeEntityDao.class);
    settingsDao = mock(FacilitySettingsEntityDao.class);
    exchangeKeyPair = CryptoUtils.STANDARD_HELPER.generateKeyPair();
    service = new DaoFacilityService(TEST_UID, TEST_URI, true, exchangeKeyPair,
        CryptoUtils.STANDARD_HELPER, exchangeDao, settingsDao);
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
    ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
    given(settingsDao.findAll(pageCaptor.capture())).willReturn(new PageImpl<>(emptyList()));

    // when
    Set<String> programs = service.getEnabledProgramTypes();

    // then
    assertThat("Programs empty", programs, hasSize(0));
    assertFindFirstByProperty(pageCaptor.getValue(), "created", Direction.DESC);
  }

  @Test
  public void getProgramTypesEmptySettings() {
    // given
    FacilitySettingsEntity settings = new FacilitySettingsEntity(Instant.now());
    settings.setProgramTypes(new HashSet<>());
    ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
    given(settingsDao.findAll(pageCaptor.capture())).willReturn(new PageImpl<>(asList(settings)));

    // when
    Set<String> programs = service.getEnabledProgramTypes();

    // then
    assertThat("Programs empty", programs, hasSize(0));
    assertFindFirstByProperty(pageCaptor.getValue(), "created", Direction.DESC);
  }

  @Test
  public void getProgramTypesWithSettings() {
    // given
    Set<String> settingsPrograms = new HashSet<>(asList("a", "b", "c"));
    FacilitySettingsEntity settings = new FacilitySettingsEntity(Instant.now());
    settings.setProgramTypes(settingsPrograms);
    ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
    given(settingsDao.findAll(pageCaptor.capture())).willReturn(new PageImpl<>(asList(settings)));

    // when
    Set<String> programs = service.getEnabledProgramTypes();

    // then
    assertThat("Programs", programs, equalTo(settingsPrograms));
    assertThat("Programs copied", programs, not(sameInstance(settingsPrograms)));
    assertFindFirstByProperty(pageCaptor.getValue(), "created", Direction.DESC);
  }

  @Test
  public void saveProgramTypesNoSettings() {
    // given
    ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
    given(settingsDao.findAll(pageCaptor.capture())).willReturn(new PageImpl<>(emptyList()));

    ArgumentCaptor<FacilitySettingsEntity> settingsCaptor = ArgumentCaptor
        .forClass(FacilitySettingsEntity.class);
    given(settingsDao.save(settingsCaptor.capture()))
        .willAnswer(invocationArg(0, FacilitySettingsEntity.class));

    // when
    Set<String> settingsPrograms = new HashSet<>(asList("a", "b", "c"));
    service.setEnabledProgramTypes(settingsPrograms);

    // then
    assertFindFirstByProperty(pageCaptor.getValue(), "created", Direction.DESC);
    FacilitySettingsEntity entity = settingsCaptor.getValue();
    assertThat("Saved programs", entity.getProgramTypes(), equalTo(settingsPrograms));
  }

  @Test
  public void saveProgramTypesWithEmptySettings() {
    // given
    FacilitySettingsEntity settings = new FacilitySettingsEntity(Instant.now());
    settings.setProgramTypes(new HashSet<>());
    ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
    given(settingsDao.findAll(pageCaptor.capture())).willReturn(new PageImpl<>(asList(settings)));

    ArgumentCaptor<FacilitySettingsEntity> settingsCaptor = ArgumentCaptor
        .forClass(FacilitySettingsEntity.class);
    given(settingsDao.save(settingsCaptor.capture()))
        .willAnswer(invocationArg(0, FacilitySettingsEntity.class));

    // when
    Set<String> programs = new HashSet<>(asList("a", "b", "c"));
    service.setEnabledProgramTypes(programs);

    // then
    assertFindFirstByProperty(pageCaptor.getValue(), "created", Direction.DESC);
    FacilitySettingsEntity entity = settingsCaptor.getValue();
    assertThat("Entity saved", entity, sameInstance(settings));
    assertThat("Saved programs", entity.getProgramTypes(), equalTo(programs));
  }

  @Test
  public void saveProgramTypesWithUpdatedSettings() {
    // given
    Set<String> settingsPrograms = new HashSet<>(asList("a", "b", "c"));
    FacilitySettingsEntity settings = new FacilitySettingsEntity(Instant.now());
    settings.setProgramTypes(settingsPrograms);
    ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
    given(settingsDao.findAll(pageCaptor.capture())).willReturn(new PageImpl<>(asList(settings)));

    ArgumentCaptor<FacilitySettingsEntity> settingsCaptor = ArgumentCaptor
        .forClass(FacilitySettingsEntity.class);
    given(settingsDao.save(settingsCaptor.capture()))
        .willAnswer(invocationArg(0, FacilitySettingsEntity.class));

    // when
    Set<String> programs = new HashSet<>(asList("b", "d"));
    service.setEnabledProgramTypes(programs);

    // then
    assertFindFirstByProperty(pageCaptor.getValue(), "created", Direction.DESC);
    FacilitySettingsEntity entity = settingsCaptor.getValue();
    assertThat("Entity saved", entity, sameInstance(settings));
    assertThat("Saved programs", entity.getProgramTypes(), equalTo(programs));
  }
}
