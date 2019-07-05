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

package net.solarnetwork.esi.simple.xchg.domain;

import static java.lang.String.format;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.springframework.context.MessageSource;

import net.solarnetwork.esi.domain.PriceMapOfferStatus;
import net.solarnetwork.esi.domain.jpa.BaseUuidEntity;
import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
import net.solarnetwork.esi.domain.support.SignableMessage;

/**
 * A price map offer entity for a specific facility.
 * 
 * @author matt
 * @version 1.0
 */
@Entity
@Table(name = "FACILITY_PRICE_MAP_OFFERS")
public class FacilityPriceMapOfferEntity extends BaseUuidEntity implements SignableMessage {

  /**
   * The info code prefix to use in {@link #toDetailedInfoString(MessageSource)}.
   */
  public static final String STANDARD_DETAILED_INFO_CODE_PREFIX = "offer";

  /**
   * The property format used in {@link #toDetailedInfoString(MessageSource)} for string values.
   */
  public static final String STANDARD_DETAILED_INFO_FORMAT_S = "%-25s : %s";

  private static final long serialVersionUID = -8102992644415632405L;

  // @formatter:off
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "FACILITY_ID", nullable = false, insertable = true, updatable = false,
      foreignKey = @ForeignKey(name = "FACILITY_PRICE_MAP_OFFERS_FACILITY_FK"))
  private FacilityEntity facility;
  // @formatter:on

  // @formatter:off
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFERING_ID", nullable = false, insertable = true, updatable = false,
      foreignKey = @ForeignKey(name = "FACILITY_PRICE_MAP_OFFERS_OFFERING_FK"))
  private PriceMapOfferingEntity offering;
  // @on

  // @formatter:off
  @OneToOne(optional = true, fetch = FetchType.LAZY, cascade = CascadeType.ALL,
      orphanRemoval = true)
  @JoinColumn(name = "PRICE_MAP_ID", nullable = true, 
      foreignKey = @ForeignKey(name = "FACILITY_PRICE_MAP_OFFERS_PRICE_MAP_FK"))
  private PriceMapEntity priceMap;
  // @formatter:on

  @Basic
  @Column(name = "IS_PROPOSED", nullable = false, insertable = true, updatable = true)
  private boolean proposed;

  @Basic
  @Column(name = "IS_ACCEPTED", nullable = false, insertable = true, updatable = true)
  private boolean accepted;

  @Basic
  @Column(name = "IS_CONFIRMED", nullable = false, insertable = true, updatable = true)
  private boolean confirmed;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, insertable = true, updatable = true, length = 16)
  private PriceMapOfferStatus.Status status;

  /**
   * Default constructor.
   */
  public FacilityPriceMapOfferEntity() {
    super();
  }

  /**
   * Construct with creation date.
   * 
   * @param created
   *        the creation date
   */
  public FacilityPriceMapOfferEntity(Instant created) {
    super(created);
  }

  /**
   * Construct with creation date and ID.
   * 
   * @param created
   *        the creation date
   * @param id
   *        the ID
   */
  public FacilityPriceMapOfferEntity(Instant created, UUID id) {
    super(created, id);
  }

  /**
   * Construct with values.
   * 
   * @param created
   *        the creation date
   * @param facility
   *        the facility
   */
  public FacilityPriceMapOfferEntity(Instant created, FacilityEntity facility) {
    super(created);
    setFacility(facility);
  }

  /**
   * Get the price map applicable to this offer.
   * 
   * <p>
   * The price map we sign comes from this entity, if available, or else the one provided by the
   * parent offering.
   * </p>
   * 
   * @return the price map that applies to this offer
   */
  @Nonnull
  public PriceMapEmbed offerPriceMap() {
    PriceMapEntity pm = getPriceMap();
    PriceMapOfferingEntity off = getOffering();
    if (pm != null) {
      return pm.priceMap();
    } else if (off != null) {
      return off.priceMap().priceMap();
    } else {
      return new PriceMapEmbed();
    }
  }

  @Override
  public int signatureMessageBytesSize() {
    PriceMapEmbed pm = offerPriceMap();
    return SignableMessage.uuidSignatureMessageSize()
        + SignableMessage.instantSignatureMessageSize() + pm.signatureMessageBytesSize();
  }

  @Override
  public void addSignatureMessageBytes(ByteBuffer buf) {
    SignableMessage.addUuidSignatureMessageBytes(buf, getId());
    SignableMessage.addInstantSignatureMessageBytes(buf, offering.getStartDate());

    PriceMapEmbed pm = offerPriceMap();
    pm.addSignatureMessageBytes(buf);
  }

  /**
   * Get a detailed informational string using the default locale and standard formatting.
   * 
   * @param messageSource
   *        the message source
   * @return the detail string
   * @see #toDetailedInfoString(Locale, MessageSource, String, String)
   */
  public String toDetailedInfoString(MessageSource messageSource) {
    return toDetailedInfoString(Locale.getDefault(), messageSource,
        PriceMapEmbed.STANDARD_DETAILED_INFO_FORMAT, STANDARD_DETAILED_INFO_FORMAT_S,
        STANDARD_DETAILED_INFO_CODE_PREFIX,
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT), ZoneId.systemDefault(),
        PriceMapEmbed.STANDARD_DETAILED_INFO_CODE_PREFIX);
  }

  /**
   * Get a detailed informational string.
   * 
   * <p>
   * The resulting string will contain one line per property in this price map. Each property will
   * be printed using a key/unit/value format, using {@code messageFormat} as the string format. The
   * key unit parameters will be strings. The value will a number.
   * </p>
   * 
   * @param locale
   *        the locale to render messages with
   * @param messageSource
   *        the message source
   * @param messageFormat
   *        the detailed message property format for number values
   * @param stringMessageFormat
   *        the detailed message property format for string values
   * @param codePrefix
   *        a message source code prefix
   * @param priceMapCodePrefix
   *        a message source code prefix to use for price map details
   * @return the string
   */
  public String toDetailedInfoString(Locale locale, MessageSource messageSource,
      String messageFormat, String stringMessageFormat, String codePrefix, DateTimeFormatter dtf,
      ZoneId tz, String priceMapCodePrefix) {
    // use PrintWriter for proper line.separator support
    try (StringWriter buf = new StringWriter(); PrintWriter out = new PrintWriter(buf)) {

      out.println(format(stringMessageFormat,
          messageSource.getMessage(codePrefix + ".startDate", null, locale),
          dtf.format(LocalDateTime.ofInstant(getOffering().getStartDate(), tz))));
      out.println(format(stringMessageFormat,
          messageSource.getMessage(codePrefix + ".endDate", null, locale), dtf.format(LocalDateTime
              .ofInstant(getOffering().getStartDate().plus(offerPriceMap().duration()), tz))));

      out.print(offerPriceMap().toDetailedInfoString(locale, messageSource, messageFormat,
          priceMapCodePrefix));
      // note last line *no* println() because shell.print() does that
      out.flush();
      return buf.toString();
    } catch (IOException e) {
      throw new RuntimeException("Error rendering price map offer: " + e.getMessage(), e);
    }
  }

  /**
   * Get the associated facility.
   * 
   * @return the facility the facility
   */
  public FacilityEntity getFacility() {
    return facility;
  }

  /**
   * Set the associated facility.
   * 
   * @param facility
   *        the facility to set
   */
  public void setFacility(FacilityEntity facility) {
    this.facility = facility;
  }

  /**
   * Get the price map offering.
   * 
   * @return the offering
   */
  public PriceMapOfferingEntity getOffering() {
    return offering;
  }

  /**
   * Set the price map offering.
   * 
   * @param offering
   *        the offering to set
   */
  public void setOffering(PriceMapOfferingEntity offering) {
    this.offering = offering;
  }

  /**
   * Get the price map details.
   * 
   * @return the price map details
   */
  public PriceMapEntity getPriceMap() {
    return priceMap;
  }

  /**
   * Set the price map details.
   * 
   * @param priceMap
   *        the price map details to set
   */
  public void setPriceMap(PriceMapEntity priceMap) {
    this.priceMap = priceMap;
  }

  /**
   * Get an optional of the price map details.
   * 
   * @return the optional price map details
   */
  @Nonnull
  public Optional<PriceMapEntity> priceMapOpt() {
    return Optional.ofNullable(getPriceMap());
  }

  /**
   * Get the price map details, creating a new one if it doesn't already exist.
   * 
   * @return the price map details
   */
  @Nonnull
  public PriceMapEntity priceMap() {
    PriceMapEntity pm = getPriceMap();
    if (pm == null) {
      pm = new PriceMapEntity();
      setPriceMap(pm);
    }
    return pm;
  }

  /**
   * Get the proposed flag.
   * 
   * @return {@literal true} if the facility has proposed the offer
   */
  public boolean isProposed() {
    return proposed;
  }

  /**
   * Set the proposed flag.
   * 
   * @param proposed
   *        the proposed to set
   */
  public void setProposed(boolean proposed) {
    this.proposed = proposed;
  }

  /**
   * Get the accepted flag.
   * 
   * @return {@literal true} if the facility has accepted the offer
   */
  public boolean isAccepted() {
    return accepted;
  }

  /**
   * Set the accepted flag.
   * 
   * @param accepted
   *        the accepted to set
   */
  public void setAccepted(boolean accepted) {
    this.accepted = accepted;
  }

  /**
   * Get the confirmed flag.
   * 
   * @return {@literal true} if the facility has confirmed the offer
   */
  public boolean isConfirmed() {
    return confirmed;
  }

  /**
   * Set the confirmed flag.
   * 
   * @param confirmed
   *        the confirmed to set
   */
  public void setConfirmed(boolean confirmed) {
    this.confirmed = confirmed;
  }

  /**
   * Get the offer status.
   * 
   * @return the status
   */
  public PriceMapOfferStatus.Status getStatus() {
    return status;
  }

  /**
   * Set the offer status.
   * 
   * @param status
   *        the status to set
   */
  public void setStatus(PriceMapOfferStatus.Status status) {
    this.status = status;
  }

}
