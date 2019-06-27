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

package net.solarnetwork.esi.simple.fac.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.solarnetwork.esi.domain.DurationRangeEmbed;
import net.solarnetwork.esi.simple.fac.dao.ResourceCharacteristicsEntityDao;
import net.solarnetwork.esi.simple.fac.domain.ResourceCharacteristicsEntity;
import net.solarnetwork.esi.simple.fac.service.FacilityCharacteristicsService;

/**
 * DAO based implementation of {@link FacilityCharacteristicsService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoFacilityCharacteristicsService implements FacilityCharacteristicsService {

  private final ResourceCharacteristicsEntityDao resourceCharacteristicsDao;

  /**
   * Constructor.
   * 
   * @param resourceCharacteristicsDao
   *        the resource characteristics DAO
   */
  public DaoFacilityCharacteristicsService(
      ResourceCharacteristicsEntityDao resourceCharacteristicsDao) {
    super();
    this.resourceCharacteristicsDao = resourceCharacteristicsDao;
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public ResourceCharacteristicsEntity resourceCharacteristics() {
    Iterator<ResourceCharacteristicsEntity> itr = resourceCharacteristicsDao.findAll().iterator();
    ResourceCharacteristicsEntity entity = (itr.hasNext() ? itr.next() : null);
    if (entity == null) {
      entity = new ResourceCharacteristicsEntity(Instant.now());
      entity.setLoadPowerMax(0L);
      entity.setLoadPowerFactor(0f);
      entity.setSupplyPowerMax(0L);
      entity.setSupplyPowerFactor(0f);
      entity.setStorageEnergyCapacity(0L);
      entity.setResponseTime(new DurationRangeEmbed(Duration.ofMillis(0), Duration.ofMillis(0)));
    }
    return entity;
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public void saveResourceCharacteristics(ResourceCharacteristicsEntity characteristics) {
    ResourceCharacteristicsEntity entity = resourceCharacteristics();
    if (characteristics.getLoadPowerMax() != null) {
      entity.setLoadPowerMax(characteristics.getLoadPowerMax());
    }
    if (characteristics.getLoadPowerFactor() != null) {
      entity.setLoadPowerFactor(characteristics.getLoadPowerFactor());
    }
    if (characteristics.getSupplyPowerMax() != null) {
      entity.setSupplyPowerMax(characteristics.getSupplyPowerMax());
    }
    if (characteristics.getSupplyPowerFactor() != null) {
      entity.setSupplyPowerFactor(characteristics.getSupplyPowerFactor());
    }
    if (characteristics.getStorageEnergyCapacity() != null) {
      entity.setStorageEnergyCapacity(characteristics.getStorageEnergyCapacity());
    }
    if (characteristics.getResponseTime() != null) {
      if (entity.getResponseTime() == null) {
        entity.setResponseTime(characteristics.getResponseTime());
      } else {
        if (characteristics.getResponseTime().getMin() != null) {
          entity.getResponseTime().setMin(characteristics.getResponseTime().getMin());
        }
        if (characteristics.getResponseTime().getMax() != null) {
          entity.getResponseTime().setMax(characteristics.getResponseTime().getMax());
        }
      }
    }
    resourceCharacteristicsDao.save(entity);
  }

}
