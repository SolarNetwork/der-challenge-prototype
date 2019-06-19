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
import static org.hamcrest.Matchers.notNullValue;

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

import net.solarnetwork.esi.simple.xchg.dao.FacilityEntityDao;
import net.solarnetwork.esi.simple.xchg.domain.FacilityEntity;
import net.solarnetwork.esi.simple.xchg.test.SpringTestSupport;

/**
 * Test cases for the {@link FacilityEntityDao} JPA implementation.
 * 
 * @author matt
 * @version 1.0
 */
@DataJpaTest
@FlywayTest(invokeCleanDB = false)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    TransactionalTestExecutionListener.class, FlywayTestExecutionListener.class })
public class JpaFacilityDaoTests extends SpringTestSupport {

  private static final String TEST_CUSTOMER_ID = "A123456789";
  private static final String TEST_UICI = "123-1234-12345";

  @Autowired
  private EntityManager em;

  private JdbcTemplate jdbcTemplate;

  private FacilityEntityDao dao;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @Before
  public void setup() {
    RepositoryFactorySupport factory = new JpaRepositoryFactory(em);
    dao = factory.getRepository(FacilityEntityDao.class);
  }

  private void assertGuestRowCountEqualTo(final int expected) {
    assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "FACILITIES"), equalTo(expected));
  }

  @Test
  public void insert() {
    FacilityEntity obj = new FacilityEntity(Instant.now());
    obj.setCustomerId(TEST_CUSTOMER_ID);
    obj.setUici(TEST_UICI);
    FacilityEntity entity = dao.save(obj);
    em.flush();
    assertThat(entity.getId(), notNullValue());
    assertThat(entity.getCreated(), notNullValue());
    assertThat(entity.getModified(), notNullValue());
    assertThat(entity.getCustomerId(), equalTo(TEST_CUSTOMER_ID));
    assertThat(entity.getUici(), equalTo(TEST_UICI));
    assertGuestRowCountEqualTo(1);
  }

}
