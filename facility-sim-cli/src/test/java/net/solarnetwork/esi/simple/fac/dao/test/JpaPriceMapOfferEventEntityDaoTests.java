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
import static org.springframework.test.jdbc.JdbcTestUtils.countRowsInTable;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
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

import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.domain.jpa.PowerComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceComponentsEmbed;
import net.solarnetwork.esi.simple.fac.dao.PriceMapOfferEventEntityDao;
import net.solarnetwork.esi.simple.fac.domain.PriceMapEntity;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferEventEntity;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferExecutionState;
import net.solarnetwork.esi.simple.fac.test.SpringTestSupport;

/**
 * Test cases for the JPA {@link PriceMapOfferEventEntityDao} implementation.
 * 
 * @author matt
 * @version 1.0
 */
@DataJpaTest
@FlywayTest(invokeCleanDB = false)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
    TransactionalTestExecutionListener.class, FlywayTestExecutionListener.class })
public class JpaPriceMapOfferEventEntityDaoTests extends SpringTestSupport {

  @Autowired
  private EntityManager em;

  private JdbcTemplate jdbcTemplate;
  private PriceMapOfferEventEntityDao dao;

  private PriceMapOfferEventEntity last;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @Before
  public void setup() {
    RepositoryFactorySupport factory = new JpaRepositoryFactory(em);
    dao = factory.getRepository(PriceMapOfferEventEntityDao.class);
  }

  private void assertPriceMapOfferEventRowCountEqualTo(final int expected) {
    assertThat(countRowsInTable(jdbcTemplate, "PRICE_MAP_OFFER_EVENTS"), equalTo(expected));
  }

  private void assertPriceMapRowCountEqualTo(final int expected) {
    assertThat(countRowsInTable(jdbcTemplate, "PRICE_MAPS"), equalTo(expected));
  }

  @Test
  public void insert() {
    UUID id = UUID.randomUUID();
    PriceMapOfferEventEntity obj = new PriceMapOfferEventEntity(Instant.now(), id);
    obj.setAccepted(true);
    obj.setCompletedSuccessfully(true);
    obj.setExecutionState(PriceMapOfferExecutionState.COMPLETED);
    obj.setMessage("This is a message");
    obj.setStartDate(Instant.now().plusSeconds(300));

    PriceMapEntity priceMap = new PriceMapEntity(Instant.now());
    priceMap.setDuration(Duration.ofHours(1));
    priceMap.setPowerComponents(new PowerComponentsEmbed(1L, 2L));
    priceMap.setPriceComponents(
        new PriceComponentsEmbed(Currency.getInstance("USD"), new BigDecimal("2.34")));
    priceMap
        .setResponseTime(new DurationRangeEmbed(Duration.ofSeconds(3L), Duration.ofSeconds(4L)));
    obj.setPriceMap(priceMap);

    PriceMapOfferEventEntity entity = dao.save(obj);
    this.last = entity;
    em.flush();
    assertPriceMapOfferEventRowCountEqualTo(1);
    assertPriceMapRowCountEqualTo(1);
    assertThat("ID", entity.getId(), equalTo(id));
    assertThat("Created set", entity.getCreated(), notNullValue());
    assertThat("Modified set", entity.getModified(), notNullValue());
    em.clear();
  }

  @Test
  public void getById() {
    insert();
    PriceMapOfferEventEntity entity = dao.findById(last.getId()).get();
    assertThat("Different instance", entity, not(sameInstance(last)));
    assertThat("ID", entity.getId(), equalTo(last.getId()));
    assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
    assertThat("Modified", entity.getModified(), equalTo(last.getModified()));
    assertThat("Accepted", entity.isAccepted(), equalTo(last.isAccepted()));
    assertThat("Completed successfully", entity.isCompletedSuccessfully(),
        equalTo(last.isCompletedSuccessfully()));
    assertThat("Execution state", entity.getExecutionState(), equalTo(last.getExecutionState()));
    assertThat("Message", entity.getMessage(), equalTo(last.getMessage()));
    assertThat("Start date", entity.getStartDate(), equalTo(last.getStartDate()));

    assertThat("Duration", entity.getPriceMap().getDuration(),
        equalTo(last.getPriceMap().getDuration()));
    assertThat("Power components", entity.getPriceMap().getPowerComponents(),
        equalTo(last.getPriceMap().getPowerComponents()));
    assertThat("Price components", entity.getPriceMap().getPriceComponents().scaledExactly(2),
        equalTo(last.getPriceMap().getPriceComponents().scaledExactly(2)));
    assertThat("Response time", entity.getPriceMap().getResponseTime(),
        equalTo(last.getPriceMap().getResponseTime()));
  }

}
