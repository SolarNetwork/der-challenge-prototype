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

package net.solarnetwork.esi.simple.fac.dao.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

import java.time.Duration;
import java.time.Instant;

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

import net.solarnetwork.esi.domain.DurationRangeEmbed;
import net.solarnetwork.esi.simple.fac.dao.ResourceCharacteristicsEntityDao;
import net.solarnetwork.esi.simple.fac.domain.ResourceCharacteristicsEntity;
import net.solarnetwork.esi.simple.fac.test.SpringTestSupport;

/**
 * Test cases for the JPA {@link ResourceCharacteristicsEntityDao} implementation.
 * 
 * @author matt
 * @version 1.0
 */
@DataJpaTest
@FlywayTest(invokeCleanDB = false)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    TransactionalTestExecutionListener.class, FlywayTestExecutionListener.class })
public class JpaResourceCharacteristicsEntityDaoTests extends SpringTestSupport {

  @Autowired
  private EntityManager em;

  private JdbcTemplate jdbcTemplate;
  private ResourceCharacteristicsEntityDao dao;

  private ResourceCharacteristicsEntity last;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @Before
  public void setup() {
    RepositoryFactorySupport factory = new JpaRepositoryFactory(em);
    dao = factory.getRepository(ResourceCharacteristicsEntityDao.class);
  }

  private void assertResourceCharacteristicsRowCountEqualTo(final int expected) {
    assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "RESOURCE_CHARS"), equalTo(expected));
  }

  private void assertPropertiesEqual(ResourceCharacteristicsEntity entity,
      ResourceCharacteristicsEntity expected) {
    assertThat("ID", entity.getId(), notNullValue());
    assertThat("Created set", entity.getCreated(), notNullValue());
    assertThat("Modified set", entity.getModified(), notNullValue());
    assertThat("Load power factor", entity.getLoadPowerFactor(),
        equalTo(expected.getLoadPowerFactor()));
    assertThat("Load power max", entity.getLoadPowerMax(), equalTo(expected.getLoadPowerMax()));
    assertThat("Response time", entity.getResponseTime(), equalTo(expected.getResponseTime()));
    assertThat("Storage energy capacity", entity.getStorageEnergyCapacity(),
        equalTo(expected.getStorageEnergyCapacity()));
    assertThat("Suppply power factor", entity.getSupplyPowerFactor(),
        equalTo(expected.getSupplyPowerFactor()));
    assertThat("Supply power max", entity.getSupplyPowerMax(),
        equalTo(expected.getSupplyPowerMax()));
  }

  private void assertEqual(ResourceCharacteristicsEntity entity,
      ResourceCharacteristicsEntity expected) {
    assertThat("ID", entity.getId(), equalTo(expected.getId()));
    assertThat("ID", entity.getCreated(), equalTo(expected.getCreated()));
    assertThat("ID", entity.getModified(), equalTo(expected.getModified()));
    assertPropertiesEqual(entity, expected);
  }

  @Test
  public void insert() {
    ResourceCharacteristicsEntity obj = new ResourceCharacteristicsEntity(Instant.now());
    obj.setLoadPowerFactor((float) Math.random());
    obj.setLoadPowerMax((long) (Math.random() * Long.MAX_VALUE));
    obj.setResponseTime(
        new DurationRangeEmbed(Duration.ofSeconds((long) (Math.random() * Integer.MAX_VALUE)),
            Duration.ofSeconds((long) (Math.random() * Integer.MAX_VALUE + Integer.MAX_VALUE))));
    obj.setStorageEnergyCapacity((long) (Math.random() * Long.MAX_VALUE));
    obj.setSupplyPowerFactor((float) Math.random());
    obj.setSupplyPowerMax((long) (Math.random() * Long.MAX_VALUE));
    ResourceCharacteristicsEntity entity = dao.save(obj);
    this.last = entity;
    em.flush();
    assertResourceCharacteristicsRowCountEqualTo(1);
    assertPropertiesEqual(entity, obj);
    em.clear();
  }

  @Test
  public void getById() {
    insert();
    ResourceCharacteristicsEntity entity = dao.findById(last.getId()).get();
    assertThat("Different instance", entity, not(sameInstance(last)));
    assertEqual(entity, last);
  }

}
