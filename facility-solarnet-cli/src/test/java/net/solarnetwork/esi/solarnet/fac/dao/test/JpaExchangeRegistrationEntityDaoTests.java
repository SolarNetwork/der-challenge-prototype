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

package net.solarnetwork.esi.solarnet.fac.dao.test;

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

import net.solarnetwork.esi.solarnet.fac.dao.ExchangeRegistrationEntityDao;
import net.solarnetwork.esi.solarnet.fac.domain.ExchangeRegistrationEntity;
import net.solarnetwork.esi.solarnet.fac.test.SpringTestSupport;

/**
 * Test cases for the JPA {@link ExchangeRegistrationEntityDao} implementation.
 * 
 * @author matt
 * @version 1.0
 */
@DataJpaTest
@FlywayTest(invokeCleanDB = false)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    TransactionalTestExecutionListener.class, FlywayTestExecutionListener.class })
public class JpaExchangeRegistrationEntityDaoTests extends SpringTestSupport {

  private static final String TEST_UID = UUID.randomUUID().toString();
  private static final String TEST_ENDPOINT_URI = "dns:///localhost:9090";
  private static final byte[] TEST_KEY = new byte[] { 1, 3, 5, 7 };
  private static final byte[] TEST_EXCHANGE_NONCE = new byte[] { 2, 4, 6, 8 };
  private static final byte[] TEST_FACILITY_NONCE = new byte[] { 8, 6, 4, 2 };

  @Autowired
  private EntityManager em;

  private JdbcTemplate jdbcTemplate;
  private ExchangeRegistrationEntityDao dao;

  private ExchangeRegistrationEntity last;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @Before
  public void setup() {
    RepositoryFactorySupport factory = new JpaRepositoryFactory(em);
    dao = factory.getRepository(ExchangeRegistrationEntityDao.class);
  }

  private void assertExchangeRegistrationRowCountEqualTo(final int expected) {
    assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "EXCHANGE_REGS"), equalTo(expected));
  }

  @Test
  public void insert() {
    ExchangeRegistrationEntity obj = new ExchangeRegistrationEntity(Instant.now());
    obj.setId(TEST_UID);
    obj.setExchangeEndpointUri(TEST_ENDPOINT_URI);
    obj.setExchangePublicKey(TEST_KEY);
    obj.setExchangeNonce(TEST_EXCHANGE_NONCE);
    obj.setFacilityNonce(TEST_FACILITY_NONCE);
    ExchangeRegistrationEntity entity = dao.save(obj);
    this.last = entity;
    em.flush();
    assertExchangeRegistrationRowCountEqualTo(1);
    assertThat("ID", entity.getId(), notNullValue());
    assertThat("Created set", entity.getCreated(), notNullValue());
    assertThat("Modified set", entity.getModified(), notNullValue());
    assertThat("Exchange UID", entity.getId(), equalTo(TEST_UID));
    assertThat("Exchange endpoint", entity.getExchangeEndpointUri(), equalTo(TEST_ENDPOINT_URI));
    assertThat("Exchange key", ByteString.copyFrom(entity.getExchangePublicKey()),
        equalTo(ByteString.copyFrom(TEST_KEY)));
    assertThat("Exchange nonce", ByteString.copyFrom(entity.getExchangeNonce()),
        equalTo(ByteString.copyFrom(TEST_EXCHANGE_NONCE)));
    assertThat("Facility nonce", ByteString.copyFrom(entity.getFacilityNonce()),
        equalTo(ByteString.copyFrom(TEST_FACILITY_NONCE)));
    em.clear();
  }

  @Test
  public void getById() {
    insert();
    ExchangeRegistrationEntity entity = dao.findById(last.getId()).get();
    assertThat("Different instance", entity, not(sameInstance(last)));
    assertThat("ID", entity.getId(), equalTo(last.getId()));
    assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
    assertThat("Modified", entity.getModified(), equalTo(last.getModified()));
    assertThat("Exchange UID", entity.getId(), equalTo(last.getId()));
    assertThat("Exchange endpoint URI", entity.getExchangeEndpointUri(),
        equalTo(last.getExchangeEndpointUri()));
    assertThat("Exchange key", ByteString.copyFrom(entity.getExchangePublicKey()),
        equalTo(ByteString.copyFrom(last.getExchangePublicKey())));
    assertThat("Exchange nonce", ByteString.copyFrom(entity.getExchangeNonce()),
        equalTo(ByteString.copyFrom(last.getExchangeNonce())));
    assertThat("Facility nonce", ByteString.copyFrom(entity.getFacilityNonce()),
        equalTo(ByteString.copyFrom(last.getFacilityNonce())));
  }

}
