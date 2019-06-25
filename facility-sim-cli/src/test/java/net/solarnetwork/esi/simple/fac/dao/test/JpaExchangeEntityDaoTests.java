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

import java.net.URI;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.jdbc.JdbcTestUtils;

import com.google.protobuf.ByteString;

import net.solarnetwork.esi.simple.fac.dao.ExchangeEntityDao;
import net.solarnetwork.esi.simple.fac.domain.ExchangeEntity;
import net.solarnetwork.esi.simple.fac.test.SpringTestSupport;

/**
 * Test cases for the JPA {@link ExchangeEntityDao} implementation.
 * 
 * @author matt
 * @version 1.0
 */
@DataJpaTest
@FlywayTest(invokeCleanDB = false)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    TransactionalTestExecutionListener.class, FlywayTestExecutionListener.class })
public class JpaExchangeEntityDaoTests extends SpringTestSupport {

  private static final String TEST_ENDPOINT_URI = "dns:///localhost:9090";
  private static final byte[] TEST_KEY = new byte[] { 1, 3, 5, 7 };

  @Autowired
  private EntityManager em;

  private JdbcTemplate jdbcTemplate;
  private ExchangeEntityDao dao;

  private ExchangeEntity last;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @Before
  public void setup() {
    RepositoryFactorySupport factory = new JpaRepositoryFactory(em);
    dao = factory.getRepository(ExchangeEntityDao.class);
  }

  private void assertExchangeRowCountEqualTo(final int expected) {
    assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "EXCHANGES"), equalTo(expected));
  }

  private ExchangeEntity doInsert() {
    ExchangeEntity obj = new ExchangeEntity(Instant.now());
    obj.setId(UUID.randomUUID().toString());
    obj.setExchangeEndpointUri(TEST_ENDPOINT_URI);
    obj.setExchangePublicKey(TEST_KEY);
    ExchangeEntity entity = dao.save(obj);
    this.last = entity;
    em.flush();
    em.clear();
    return entity;
  }

  @Test
  public void insert() {
    ExchangeEntity entity = doInsert();
    assertExchangeRowCountEqualTo(1);
    assertThat("ID", entity.getId(), notNullValue());
    assertThat("Created set", entity.getCreated(), notNullValue());
    assertThat("Modified set", entity.getModified(), notNullValue());
    assertThat("Exchange UID", entity.getId(), equalTo(last.getId()));
    assertThat("Exchange endpoint", entity.getExchangeEndpointUri(), equalTo(TEST_ENDPOINT_URI));
    assertThat("Exchange key", ByteString.copyFrom(entity.getExchangePublicKey()),
        equalTo(ByteString.copyFrom(TEST_KEY)));
  }

  @Test
  public void getById() {
    doInsert();
    ExchangeEntity entity = dao.findById(last.getId()).get();
    assertThat("Different instance", entity, not(sameInstance(last)));
    assertThat("ID", entity.getId(), equalTo(last.getId()));
    assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
    assertThat("Modified", entity.getModified(), equalTo(last.getModified()));
    assertThat("Exchange UID", entity.getId(), equalTo(last.getId()));
    assertThat("Exchange endpoint URI", entity.getExchangeEndpointUri(),
        equalTo(last.getExchangeEndpointUri()));
    assertThat("Exchange key", ByteString.copyFrom(entity.getExchangePublicKey()),
        equalTo(ByteString.copyFrom(last.getExchangePublicKey())));
  }

  @Test
  public void update() {
    doInsert();
    ExchangeEntity entity = dao.findById(last.getId()).get();
    URI newUri = URI.create("//test-modified");
    entity.setExchangeEndpointUri(newUri.toString());
    entity = dao.save(entity);
    em.flush();
    em.clear();
    ExchangeEntity updated = dao.findById(last.getId()).get();
    assertThat("Different instance", updated, not(sameInstance(entity)));
    assertThat("ID", updated.getId(), equalTo(last.getId()));
    assertThat("Exchange endpoint URI", updated.getExchangeEndpointUri(),
        equalTo(newUri.toString()));
  }

  @Test
  public void findNewestOfNone() {
    Page<ExchangeEntity> p = dao.findAll(PageRequest.of(0, 1, Direction.DESC, "created"));
    assertThat("Page returned", p, notNullValue());
    assertThat("Page size", p.getNumber(), equalTo(0));
    assertThat("No iteration", p.iterator().hasNext(), equalTo(false));
  }

  @Test
  public void findNewestOfOne() {
    doInsert();
    Page<ExchangeEntity> p = dao.findAll(PageRequest.of(0, 1, Direction.DESC, "created"));
    assertThat("Page returned", p, notNullValue());
    assertThat("Page size", p.getNumberOfElements(), equalTo(1));
    ExchangeEntity entity = p.iterator().next();
    assertThat("Returned entity", entity, equalTo(last));
  }

  @Test
  public void findNewestOfMulti() throws InterruptedException {
    doInsert();
    Thread.sleep(300);
    doInsert();
    Thread.sleep(300);
    doInsert();
    assertExchangeRowCountEqualTo(3);

    Page<ExchangeEntity> p = dao.findAll(PageRequest.of(0, 1, Direction.DESC, "created"));
    assertThat("Page returned", p, notNullValue());
    assertThat("Page size", p.getNumberOfElements(), equalTo(1));
    assertThat("Total size", p.getTotalElements(), equalTo(3L));
    ExchangeEntity entity = p.iterator().next();
    assertThat("Returned entity", entity, equalTo(last));
  }
}
