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

import com.google.protobuf.ByteString;

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
  private static final String TEST_UID = UUID.randomUUID().toString();
  private static final String TEST_ENDPOINT_URI = "dns:///localhost:9090";
  private static final byte[] TEST_KEY = new byte[] { 1, 3, 5, 7 };

  @Autowired
  private EntityManager em;

  private JdbcTemplate jdbcTemplate;
  private FacilityEntityDao dao;

  private FacilityEntity last;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @Before
  public void setup() {
    RepositoryFactorySupport factory = new JpaRepositoryFactory(em);
    dao = factory.getRepository(FacilityEntityDao.class);
  }

  private void assertFacilityRowCountEqualTo(final int expected) {
    assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "FACILITIES"), equalTo(expected));
  }

  @Test
  public void insert() {
    FacilityEntity obj = new FacilityEntity(Instant.now());
    obj.setCustomerId(TEST_CUSTOMER_ID);
    obj.setUici(TEST_UICI);
    obj.setFacilityUid(TEST_UID);
    obj.setFacilityEndpointUri(TEST_ENDPOINT_URI);
    obj.setFacilityPublicKey(TEST_KEY);
    FacilityEntity entity = dao.save(obj);
    this.last = entity;
    em.flush();
    assertThat("ID", entity.getId(), notNullValue());
    assertThat("Created set", entity.getCreated(), notNullValue());
    assertThat("Modified set", entity.getModified(), notNullValue());
    assertThat("Customer ID", entity.getCustomerId(), equalTo(TEST_CUSTOMER_ID));
    assertThat("UICI", entity.getUici(), equalTo(TEST_UICI));
    assertThat("Facility UID", entity.getFacilityUid(), equalTo(TEST_UID));
    assertThat("Facility endpoint", entity.getFacilityEndpointUri(), equalTo(TEST_ENDPOINT_URI));
    assertFacilityRowCountEqualTo(1);
    assertThat("Facility key", ByteString.copyFrom(entity.getFacilityPublicKey()),
        equalTo(ByteString.copyFrom(TEST_KEY)));
    em.clear();
  }

  @Test
  public void getById() {
    insert();
    FacilityEntity entity = dao.findById(last.getId()).get();
    assertThat("Different instance", entity, not(sameInstance(last)));
    assertThat("ID", entity.getId(), equalTo(last.getId()));
    assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
    assertThat("Modified", entity.getModified(), equalTo(last.getModified()));
    assertThat("Customer ID", entity.getCustomerId(), equalTo(last.getCustomerId()));
    assertThat("UICI", entity.getUici(), equalTo(last.getUici()));
    assertThat("Facility UID", entity.getFacilityUid(), equalTo(last.getFacilityUid()));
    assertThat("Facility endpoint URI", entity.getFacilityEndpointUri(),
        equalTo(last.getFacilityEndpointUri()));
    assertThat("Facility key", ByteString.copyFrom(entity.getFacilityPublicKey()),
        equalTo(ByteString.copyFrom(last.getFacilityPublicKey())));
  }

}