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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

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

import net.solarnetwork.esi.simple.fac.dao.FacilitySettingsEntityDao;
import net.solarnetwork.esi.simple.fac.domain.FacilitySettingsEntity;
import net.solarnetwork.esi.simple.fac.test.SpringTestSupport;

/**
 * Test cases for the JPA {@link FacilitySettingsEntityDao} implementation.
 * 
 * @author matt
 * @version 1.0
 */
@DataJpaTest
@FlywayTest(invokeCleanDB = false)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    TransactionalTestExecutionListener.class, FlywayTestExecutionListener.class })
public class JpaFacilitySettingsEntityDaoTests extends SpringTestSupport {

  @Autowired
  private EntityManager em;

  private JdbcTemplate jdbcTemplate;
  private FacilitySettingsEntityDao dao;

  private FacilitySettingsEntity last;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @Before
  public void setup() {
    RepositoryFactorySupport factory = new JpaRepositoryFactory(em);
    dao = factory.getRepository(FacilitySettingsEntityDao.class);
  }

  private void assertFacilitySettingsRowCountEqualTo(final int expected) {
    assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "FAC_SETTINGS"), equalTo(expected));
  }

  private void assertProgramTypesRowCountEqualTo(final int expected) {
    assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "PROGRAM_TYPES"), equalTo(expected));
  }

  @Test
  public void insert() {
    FacilitySettingsEntity obj = new FacilitySettingsEntity(Instant.now());
    obj.addProgramType("a");
    obj.addProgramType("b");
    obj.addProgramType("c");
    FacilitySettingsEntity entity = dao.save(obj);
    this.last = entity;
    em.flush();
    assertFacilitySettingsRowCountEqualTo(1);
    assertProgramTypesRowCountEqualTo(3);
    assertThat("ID", entity.getId(), notNullValue());
    assertThat("Created set", entity.getCreated(), notNullValue());
    assertThat("Modified set", entity.getModified(), notNullValue());
    assertThat("Program types", entity.getProgramTypes(), containsInAnyOrder("a", "b", "c"));
    em.clear();
  }

  @Test
  public void getById() {
    insert();
    FacilitySettingsEntity entity = dao.findById(last.getId()).get();
    assertThat("Different instance", entity, not(sameInstance(last)));
    assertThat("ID", entity.getId(), equalTo(last.getId()));
    assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
    assertThat("Modified", entity.getModified(), equalTo(last.getModified()));
    assertThat("Program types", entity.getProgramTypes(), equalTo(last.getProgramTypes()));
  }

  @Test
  public void updateProgramTypes() {
    insert();
    FacilitySettingsEntity entity = dao.findById(last.getId()).get();
    entity.removeProgramType("b");
    entity.addProgramType("d");
    dao.save(entity);
    em.flush();
    em.clear();

    FacilitySettingsEntity updated = dao.findById(last.getId()).get();
    assertThat("Different instance", updated, not(sameInstance(entity)));
    assertThat("Program types", updated.getProgramTypes(), equalTo(entity.getProgramTypes()));
  }
}
