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

package net.solarnetwork.esi.solarnet.fac.domain;

import java.nio.ByteBuffer;

import net.solarnetwork.esi.domain.DerProgramType;
import net.solarnetwork.esi.domain.support.BaseIdentity;
import net.solarnetwork.esi.domain.support.SignableMessage;

/**
 * A facility-configured program.
 * 
 * @author matt
 * @version 1.0
 */
public class FacilityProgram extends BaseIdentity<String>
    implements SignableMessage, SolarNodeMetadataEntity {

  private static final long serialVersionUID = -4567434562049429502L;

  private Long nodeId;
  private DerProgramType programType;
  private String priceMapId;
  private String priceMapGroupUid;
  private String resourceId;

  /**
   * Default constructor.
   */
  public FacilityProgram() {
    super();
  }

  /**
   * Construct with values.
   * 
   * @param id
   *        the primary key
   */
  public FacilityProgram(String id) {
    super(id);
  }

  /**
   * Construct with values.
   * 
   * @param programType
   *        the program type
   */
  public FacilityProgram(DerProgramType programType) {
    super();
    setProgramType(programType);
  }

  /**
   * Construct with values.
   * 
   * @param id
   *        the primary key
   * @param programType
   *        the program type
   */
  public FacilityProgram(String id, DerProgramType programType) {
    super(id);
    setProgramType(programType);
  }

  /**
   * Create a copy of this instance.
   * 
   * <p>
   * All properties are copied onto the new instance.
   * </p>
   * 
   * @return the copy
   */
  public FacilityProgram copy() {
    FacilityProgram c = new FacilityProgram(getId(), getProgramType());
    c.setPriceMapId(getPriceMapId());
    c.setPriceMapGroupUid(getPriceMapGroupUid());
    c.setResourceId(getResourceId());
    return c;
  }

  @Override
  public int signatureMessageBytesSize() {
    return Integer.BYTES;
  }

  @Override
  public void addSignatureMessageBytes(ByteBuffer buf) {
    DerProgramType t = getProgramType();
    buf.putInt(t != null && t != DerProgramType.UNRECOGNIZED ? t.getNumber() : -1);
  }

  /**
   * Get the node ID.
   * 
   * @return the nodeId
   */
  public Long getNodeId() {
    return nodeId;
  }

  /**
   * Set the node ID.
   * 
   * @param nodeId
   *        the nodeId to set
   */
  @Override
  public void setNodeId(Long nodeId) {
    this.nodeId = nodeId;
  }

  /**
   * Get the DER program type.
   * 
   * @return the program type
   */
  public DerProgramType getProgramType() {
    return programType;
  }

  /**
   * Set the DER program type.
   * 
   * @param programType
   *        the program type to set
   */
  public void setProgramType(DerProgramType programType) {
    this.programType = programType;
  }

  /**
   * Get the DER program type as a number.
   * 
   * @return the program type number
   */
  public int getProgramTypeNumber() {
    DerProgramType t = getProgramType();
    return (t != null ? t.getNumber() : -1);
  }

  /**
   * Set the DER program type via its number.
   * 
   * @param programTypeNumber
   *        the program type number to set
   */
  public void setProgramTypeNumber(int programTypeNumber) {
    setProgramType(DerProgramType.forNumber(programTypeNumber));
  }

  /**
   * Get the price map ID to use with this program.
   * 
   * @return the priceMapId the price map ID
   */
  public String getPriceMapId() {
    return priceMapId;
  }

  /**
   * Set the price map ID to use with this program.
   * 
   * @param priceMapId
   *        the price map ID to set
   */
  public void setPriceMapId(String priceMapId) {
    this.priceMapId = priceMapId;
  }

  /**
   * Get the price map group UID to use with this program.
   * 
   * @return the price map group UID
   */
  public String getPriceMapGroupUid() {
    return priceMapGroupUid;
  }

  /**
   * Set the price map group UID to use with this program.
   * 
   * @param priceMapGroupUid
   *        the price map group UID to set
   */
  public void setPriceMapGroupUid(String priceMapGroupUid) {
    this.priceMapGroupUid = priceMapGroupUid;
  }

  /**
   * Get the resource ID to use with this program.
   * 
   * @return the resource ID
   */
  public String getResourceId() {
    return resourceId;
  }

  /**
   * Set the resource ID to use with this program.
   * 
   * @param resourceId
   *        the resource ID to set
   */
  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

}
