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
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.springframework.context.MessageSource;

import net.solarnetwork.esi.domain.PriceMapOfferOrBuilder;
import net.solarnetwork.esi.domain.jpa.BaseUuidEntity;
import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
import net.solarnetwork.esi.domain.support.ProtobufUtils;
import net.solarnetwork.esi.domain.support.SignableMessage;

/**
 * Entity for a price map based event.
 * 
 * <p>
 * The {@link #getId()} value represents the UUID of the price map offer this event is associated
 * with.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
@Entity
@Table(name = "PRICE_MAP_OFFER_EVENTS")
public class PriceMapOfferEventEntity extends BaseUuidEntity implements SignableMessage {

  /**
   * The info code prefix to use in {@link #toDetailedInfoString(MessageSource)}.
   */
  public static final String STANDARD_DETAILED_INFO_CODE_PREFIX = "offer";

  /**
   * The property format used in {@link #toDetailedInfoString(MessageSource)} for string values.
   */
  public static final String STANDARD_DETAILED_INFO_FORMAT_S = "%-25s : %s";

  private static final long serialVersionUID = -8175511305516942720L;

  @Basic
  @Column(name = "START_AT")
  private Instant startDate;

  @Basic
  @Column(name = "IS_ACCEPTED", nullable = false, insertable = true, updatable = true)
  private boolean accepted;

  @Basic
  @Column(name = "IS_SUCCESS", nullable = false, insertable = true, updatable = true)
  private boolean completedSuccessfully;

  @Enumerated(EnumType.STRING)
  @Column(name = "EXEC_STATE", nullable = false, insertable = true, updatable = true, length = 12)
  private PriceMapOfferExecutionState executionState;

  @Basic
  @Column(name = "MSG", nullable = true, insertable = true, updatable = true, length = 255)
  private String message;

  // @formatter:off
  @OneToOne(optional = false, fetch = FetchType.EAGER, cascade = CascadeType.ALL,
      orphanRemoval = true)
  @JoinColumn(name = "PRICE_MAP_ID", nullable = false, 
      foreignKey = @ForeignKey(name = "PRICE_MAP_OFFER_EVENTS_PRICE_MAP_FK"))
  private PriceMapEntity priceMap;
  // @formatter:on

  // @formatter:off
  @OneToOne(optional = true, fetch = FetchType.EAGER, cascade = CascadeType.ALL,
      orphanRemoval = true)
  @JoinColumn(name = "COUNTER_OFFER_ID", nullable = true, 
      foreignKey = @ForeignKey(name = "PRICE_MAP_OFFER_EVENTS_COUNTER_OFFER_FK"))
  private PriceMapEntity counterOffer;
  // @formatter:on

  // CHECKSTYLE IGNORE LineLength FOR NEXT 2 LINES
  @Basic
  @Column(name = "FAC_PRICE_MAP_ID", nullable = true, insertable = true, updatable = false, length = 64)
  private String facilityPriceMapId;

  /**
   * Default constructor.
   */
  public PriceMapOfferEventEntity() {
    super();
  }

  /**
   * Construct with creation date.
   * 
   * @param created
   *        the creation date
   */
  public PriceMapOfferEventEntity(Instant created) {
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
  public PriceMapOfferEventEntity(Instant created, UUID id) {
    super(created, id);
  }

  /**
   * Construct with values.
   * 
   * @param created
   *        the creation date
   * @param id
   *        the ID
   * @param startDate
   *        the start date
   * @param priceMap
   *        map the price map details
   */
  public PriceMapOfferEventEntity(Instant created, UUID id, Instant startDate,
      PriceMapEmbed priceMap) {
    super(created, id);
    setStartDate(startDate);
    priceMap().setPriceMap(priceMap);
  }

  @Override
  public int signatureMessageBytesSize() {
    return SignableMessage.uuidSignatureMessageSize()
        + SignableMessage.instantSignatureMessageSize() + priceMap().signatureMessageBytesSize();
  }

  @Override
  public void addSignatureMessageBytes(ByteBuffer buf) {
    SignableMessage.addUuidSignatureMessageBytes(buf, getId());
    SignableMessage.addInstantSignatureMessageBytes(buf, getStartDate());
    priceMap().addSignatureMessageBytes(buf);
  }

  /**
   * Create a resource characteristics entity out of a source message.
   * 
   * @param message
   *        the message to copy the properties from
   * @return the new entity
   */
  public static PriceMapOfferEventEntity entityForMessage(PriceMapOfferOrBuilder message) {
    UUID id = ProtobufUtils.uuidValue(message.getOfferId());
    PriceMapOfferEventEntity entity = new PriceMapOfferEventEntity(Instant.now(), id);
    entity.populateFromMessage(message);
    return entity;
  }

  /**
   * Update the properties of this object from equivalent properties in a source message.
   * 
   * @param message
   *        the message to copy the properties from
   */
  public void populateFromMessage(PriceMapOfferOrBuilder message) {
    setStartDate(ProtobufUtils.instantValue(message.getWhen()));
    setPriceMap(PriceMapEntity.entityForMessage(message.getPriceMap()));
  }

  /**
   * Get the price map applicable to this offer.
   * 
   * <p>
   * The price map we sign comes from {@link #getCounterOffer()}, if available, or else the one
   * provided by the {@link #getPriceMap()}. If neither are available, a new instance is returned.
   * </p>
   * 
   * @return the price map that applies to this offer
   */
  @Nonnull
  public PriceMapEmbed offerPriceMap() {
    PriceMapEntity counter = getCounterOffer();
    PriceMapEntity pm = getPriceMap();
    if (counter != null) {
      return counter.priceMap();
    } else if (pm != null) {
      return pm.priceMap();
    } else {
      return new PriceMapEmbed();
    }
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
          dtf.format(LocalDateTime.ofInstant(getStartDate(), tz))));
      out.println(format(stringMessageFormat,
          messageSource.getMessage(codePrefix + ".endDate", null, locale), dtf.format(
              LocalDateTime.ofInstant(getStartDate().plus(offerPriceMap().duration()), tz))));

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
   * Get the price map details.
   * 
   * @return the counter offer details
   */
  public PriceMapEntity getCounterOffer() {
    return counterOffer;
  }

  /**
   * Set the price map details.
   * 
   * @param counterOffer
   *        the counter offer details to set
   */
  public void setCounterOffer(PriceMapEntity counterOffer) {
    this.counterOffer = counterOffer;
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
      pm = new PriceMapEntity(Instant.now());
      setPriceMap(pm);
    }
    return pm;
  }

  /**
   * Get the offer start date.
   * 
   * @return the start date
   */
  public Instant getStartDate() {
    return startDate;
  }

  /**
   * Set the offer start date.
   * 
   * @param startDate
   *        the date to set
   */
  public void setStartDate(Instant startDate) {
    this.startDate = startDate;
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
   * Get the success flag.
   * 
   * @return {@literal true} if the facility has executed the offer successfully
   */
  public boolean isCompletedSuccessfully() {
    return completedSuccessfully;
  }

  /**
   * Set the success flag.
   * 
   * @param completedSuccessfully
   *        the success to set
   */
  public void setCompletedSuccessfully(boolean completedSuccessfully) {
    this.completedSuccessfully = completedSuccessfully;
  }

  /**
   * Get the execution state.
   * 
   * @return the execution state
   */
  public PriceMapOfferExecutionState getExecutionState() {
    return executionState;
  }

  /**
   * Set the execution state.
   * 
   * @param executionState
   *        the execution state to set
   */
  public void setExecutionState(PriceMapOfferExecutionState executionState) {
    this.executionState = executionState;
  }

  /**
   * Get the execution state, creating a new one if it doesn't already exist.
   * 
   * @return the execution state
   */
  @Nonnull
  public PriceMapOfferExecutionState executionState() {
    PriceMapOfferExecutionState e = getExecutionState();
    if (e == null) {
      e = PriceMapOfferExecutionState.UNKNOWN;
      setExecutionState(e);
    }
    return e;
  }

  /**
   * Get a general message.
   * 
   * @return the message; if {@link #isCompletedSuccessfully()} returns {@literal false} this
   *         message should be related to the cause of the failure
   */
  public String getMessage() {
    return message;
  }

  /**
   * Set a general message.
   * 
   * @param message
   *        the message to set
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * Get the ID of the associated facility price map.
   * 
   * @return the facilityPriceMapId the facility price map ID
   */
  public String getFacilityPriceMapId() {
    return facilityPriceMapId;
  }

  /**
   * Set the ID of the associated facility price map.
   * 
   * <p>
   * This must be configured if the offer is accepted.
   * </p>
   * 
   * @param facilityPriceMapId
   *        the facilityPriceMapId to set
   */
  public void setFacilityPriceMapId(String facilityPriceMapId) {
    this.facilityPriceMapId = facilityPriceMapId;
  }

}
