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

package net.solarnetwork.esi.simple.xchg.dao.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.jdbc.JdbcTestUtils;

import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.simple.xchg.dao.FacilityEntityDao;
import net.solarnetwork.esi.simple.xchg.dao.FacilityResourceCharacteristicsEntityDao;
import net.solarnetwork.esi.simple.xchg.domain.FacilityEntity;
import net.solarnetwork.esi.simple.xchg.domain.FacilityResourceCharacteristicsEntity;
import net.solarnetwork.esi.simple.xchg.test.SpringTestSupport;

/**
 * Test cases for the {@link FacilityResourceCharacteristicsEntityDao} JPA implementation.
 * 
 * @author matt
 * @version 1.0
 */
@DataJpaTest
@FlywayTest(invokeCleanDB = false)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    TransactionalTestExecutionListener.class, FlywayTestExecutionListener.class })
public class JpaFacilityResourceCharacteristicsEntityDaoTests extends SpringTestSupport {

  private static final String TEST_CUSTOMER_ID = "A123456789";
  private static final String TEST_UICI = "123-1234-12345";
  private static final String TEST_UID = UUID.randomUUID().toString();
  private static final String TEST_ENDPOINT_URI = "dns:///localhost:9090";
  private static final byte[] TEST_KEY = new byte[] { 1, 3, 5, 7 };

  @Autowired
  private EntityManager em;

  private JdbcTemplate jdbcTemplate;
  private FacilityEntityDao facilityDao;
  private FacilityResourceCharacteristicsEntityDao dao;

  private FacilityEntity lastFacility;
  private FacilityResourceCharacteristicsEntity last;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @Before
  public void setup() {
    RepositoryFactorySupport factory = new JpaRepositoryFactory(em);
    facilityDao = factory.getRepository(FacilityEntityDao.class);
    dao = factory.getRepository(FacilityResourceCharacteristicsEntityDao.class);
  }

  private void assertFacilityResourceCharacteristicsRowCountEqualTo(final int expected) {
    assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "FACILITY_RESOURCE_CHARS"),
        equalTo(expected));
  }

  private FacilityEntity setupFacility() {
    FacilityEntity obj = new FacilityEntity(Instant.now(), UUID.randomUUID());
    obj.setCustomerId(TEST_CUSTOMER_ID);
    obj.setUici(TEST_UICI);
    obj.setFacilityUid(TEST_UID);
    obj.setFacilityEndpointUri(TEST_ENDPOINT_URI);
    obj.setFacilityPublicKey(TEST_KEY);
    FacilityEntity entity = facilityDao.save(obj);
    this.lastFacility = entity;
    return entity;
  }

  @Test
  public void insert() {
    FacilityEntity facility = setupFacility();
    FacilityResourceCharacteristicsEntity obj = new FacilityResourceCharacteristicsEntity(
        Instant.now(), facility);
    obj.setLoadPowerMax(1L);
    obj.setLoadPowerFactor(0.2f);
    obj.setSupplyPowerMax(3L);
    obj.setSupplyPowerFactor(0.4f);
    obj.setStorageEnergyCapacity(5L);
    obj.setResponseTime(new DurationRangeEmbed(Duration.ofSeconds(6L), Duration.ofSeconds(7L)));
    FacilityResourceCharacteristicsEntity entity = dao.save(obj);
    this.last = entity;
    em.flush();
    assertThat("ID", entity.getId(), notNullValue());
    assertThat("Created set", entity.getCreated(), notNullValue());
    assertThat("Modified set", entity.getModified(), notNullValue());
    assertFacilityResourceCharacteristicsRowCountEqualTo(1);
    em.clear();
  }

  @Test
  public void getById() {
    insert();
    FacilityResourceCharacteristicsEntity entity = dao.findById(last.getId()).get();
    assertThat("Different instance", entity, not(sameInstance(last)));
    assertThat("ID", entity.getId(), equalTo(last.getId()));
    assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
    assertThat("Modified", entity.getModified(), equalTo(last.getModified()));
    assertThat("Load power max", entity.getLoadPowerMax(), equalTo(last.getLoadPowerMax()));
    assertThat("Load power factor", entity.getLoadPowerFactor(),
        equalTo(last.getLoadPowerFactor()));
    assertThat("Supply power max", entity.getSupplyPowerMax(), equalTo(last.getSupplyPowerMax()));
    assertThat("Supply power factor", entity.getSupplyPowerFactor(),
        equalTo(last.getSupplyPowerFactor()));
    assertThat("Storage energy capacity", entity.getStorageEnergyCapacity(),
        equalTo(last.getStorageEnergyCapacity()));
    assertThat("Response time min", entity.getResponseTime().getMin(),
        equalTo(last.getResponseTime().getMin()));
    assertThat("Response time max", entity.getResponseTime().getMax(),
        equalTo(last.getResponseTime().getMax()));
  }

  @Test
  public void getByFacilityUid() {
    insert();
    FacilityResourceCharacteristicsEntity entity = dao
        .findByFacility_FacilityUid(lastFacility.getFacilityUid()).get();
    assertThat("Result found", entity, equalTo(last));
  }

}
