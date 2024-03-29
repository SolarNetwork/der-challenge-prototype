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

package net.solarnetwork.esi.simple.fac.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import net.solarnetwork.esi.domain.jpa.BaseLongEntity;

/**
 * Overall settings for a facility.
 * 
 * <p>
 * Generally just one of these entities is assumed to exist.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
@Entity
@Table(name = "FAC_SETTINGS")
public class FacilitySettingsEntity extends BaseLongEntity {

  private static final long serialVersionUID = -718415775150394699L;

  // @formatter:off
  @ElementCollection(fetch = FetchType.EAGER)
  @Column(name = "PROGRAM", nullable = false, length = 64)
  @CollectionTable(name = "PROGRAM_TYPES", 
      joinColumns = @JoinColumn(name = "FAC_SETTING_ID", nullable = false), 
      foreignKey = @ForeignKey(name = "PROGRAM_TYPES_FAC_SETTING_FK"),
      uniqueConstraints = @UniqueConstraint(name = "PROGRAM_TYPES_PK",
          columnNames = { "FAC_SETTING_ID", "PROGRAM" }))
  private Set<String> programTypes;
  // @formatter:on

  // @formatter:off
  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinTable(name = "FACILITY_PRICE_MAPS",
      joinColumns = @JoinColumn(name = "FAC_SETTING_ID", referencedColumnName = "ID",
          foreignKey = @ForeignKey(name = "FACILITY_PRICE_MAPS_FAC_SETTING_FK")),
      inverseJoinColumns = @JoinColumn(name = "PRICE_MAP_ID", referencedColumnName = "ID",
          foreignKey = @ForeignKey(name = "FACILITY_PRICE_MAPS_PRICE_MAP_FK")),
      uniqueConstraints = @UniqueConstraint(name = "FACILITY_PRICE_MAPS_PRICE_MAP_UNQ",
          columnNames = "PRICE_MAP_ID"))
  private Set<PriceMapEntity> priceMaps;
  // @formatter:on

  /**
   * Default constructor.
   */
  public FacilitySettingsEntity() {
    super();
  }

  /**
   * Construct with creation date.
   * 
   * @param created
   *        the creation date
   */
  public FacilitySettingsEntity(Instant created) {
    super(created);
  }

  /**
   * Construct with values.
   * 
   * @param created
   *        the creation date
   * @param id
   *        the primary key
   */
  public FacilitySettingsEntity(Instant created, Long id) {
    super(created, id);
  }

  /**
   * Get the program types.
   * 
   * @return the program types
   */
  public Set<String> getProgramTypes() {
    return programTypes;
  }

  /**
   * Set the program types.
   * 
   * @param programTypes
   *        the program types to set
   */
  public void setProgramTypes(Set<String> programTypes) {
    this.programTypes = programTypes;
  }

  /**
   * Add a program type.
   * 
   * @param type
   *        the type to add
   */
  public void addProgramType(String type) {
    Set<String> tags = getProgramTypes();
    if (tags == null) {
      tags = new HashSet<>(4);
      setProgramTypes(tags);
    }
    tags.add(type);
  }

  /**
   * Remove a program type.
   * 
   * @param type
   *        the type to remove
   */
  public void removeProgramType(String type) {
    Set<String> tags = getProgramTypes();
    if (tags != null) {
      tags.remove(type);
    }
  }

  /**
   * Get the price map.
   * 
   * @return the price maps, or {@literal null} if not available
   */
  public Set<PriceMapEntity> getPriceMaps() {
    return priceMaps;
  }

  /**
   * SEt the price map.
   * 
   * @param priceMaps
   *        the price maps to set
   */
  public void setPriceMaps(Set<PriceMapEntity> priceMaps) {
    this.priceMaps = priceMaps;
  }

  /**
   * Add a price map.
   * 
   * @param priceMap
   *        the price map to add
   */
  public void addPriceMap(PriceMapEntity priceMap) {
    Set<PriceMapEntity> set = getPriceMaps();
    if (set == null) {
      set = new HashSet<>(4);
      setPriceMaps(set);
    }
    set.add(priceMap);
  }

  /**
   * Remove a price map.
   * 
   * @param priceMap
   *        the price map to remove
   */
  public void removePriceMap(PriceMapEntity priceMap) {
    Set<PriceMapEntity> set = getPriceMaps();
    if (set != null) {
      set.remove(priceMap);
    }
  }

  /**
   * Remove all price maps from this facility.
   */
  public void clearPriceMaps() {
    Set<PriceMapEntity> set = getPriceMaps();
    if (set == null || set.isEmpty()) {
      return;
    }
    set.clear();
  }

}
