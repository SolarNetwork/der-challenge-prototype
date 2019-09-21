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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.springframework.context.MessageSource;

import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
import net.solarnetwork.esi.domain.support.BaseIdentity;
import net.solarnetwork.esi.domain.support.SignableMessage;

/**
 * A facility price map offer event.
 * 
 * <p>
 * This is similar to {@link PriceMapOfferEventEntity} but not a JPA entity so the data can be more
 * easily shared across threads without needing a JPA context.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class FacilityPriceMapOfferEvent extends BaseIdentity<UUID> implements SignableMessage {

  private static final long serialVersionUID = 2561602084927389530L;

  /**
   * The info code prefix to use in {@link #toDetailedInfoString(MessageSource)}.
   */
  public static final String STANDARD_DETAILED_INFO_CODE_PREFIX = "offer";

  /**
   * The property format used in {@link #toDetailedInfoString(MessageSource)} for string values.
   */
  public static final String STANDARD_DETAILED_INFO_FORMAT_S = "%-25s : %s";

  private Instant startDate;
  private boolean accepted;
  private boolean completedSuccessfully;
  private PriceMapOfferExecutionState executionState;
  private String message;
  private PriceMapEmbed priceMap;
  private PriceMapEmbed counterOffer;
  private String facilityPriceMapId;

  /**
   * Default constructor.
   */
  public FacilityPriceMapOfferEvent() {
    super();
  }

  /**
   * Construct with creation date and ID.
   * 
   * @param id
   *        the ID
   */
  public FacilityPriceMapOfferEvent(UUID id) {
    super(id);
  }

  /**
   * Construct with values.
   * 
   * @param id
   *        the ID
   * @param startDate
   *        the start date
   * @param priceMap
   *        map the price map details
   */
  public FacilityPriceMapOfferEvent(UUID id, Instant startDate, PriceMapEmbed priceMap) {
    super(id);
    setStartDate(startDate);
    setPriceMap(priceMap);
  }

  /**
   * Construct from a price map offer event entity.
   * 
   * @param entity
   *        the entity
   */
  public FacilityPriceMapOfferEvent(PriceMapOfferEventEntity entity) {
    this(entity.getId());
    setStartDate(entity.getStartDate());
    if (entity.getPriceMap() != null) {
      setPriceMap(entity.getPriceMap().getPriceMap());
    }
    setAccepted(entity.isAccepted());
    setCompletedSuccessfully(entity.isCompletedSuccessfully());
    setExecutionState(entity.getExecutionState());
    setMessage(entity.getMessage());
    if (entity.getCounterOffer() != null) {
      setCounterOffer(entity.getCounterOffer().getPriceMap());
    }
    setFacilityPriceMapId(entity.getFacilityPriceMapId());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(accepted, completedSuccessfully, counterOffer,
        executionState, facilityPriceMapId, message, priceMap, startDate);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof FacilityPriceMapOfferEvent)) {
      return false;
    }
    FacilityPriceMapOfferEvent other = (FacilityPriceMapOfferEvent) obj;
    return accepted == other.accepted && completedSuccessfully == other.completedSuccessfully
        && Objects.equals(counterOffer, other.counterOffer)
        && executionState == other.executionState
        && Objects.equals(facilityPriceMapId, other.facilityPriceMapId)
        && Objects.equals(message, other.message) && Objects.equals(priceMap, other.priceMap)
        && Objects.equals(startDate, other.startDate);
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
    PriceMapEmbed counter = getCounterOffer();
    PriceMapEmbed pm = getPriceMap();
    if (counter != null) {
      return counter;
    } else if (pm != null) {
      return pm;
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
  public PriceMapEmbed getPriceMap() {
    return priceMap;
  }

  /**
   * Set the price map details.
   * 
   * @param priceMap
   *        the price map details to set
   */
  public void setPriceMap(PriceMapEmbed priceMap) {
    this.priceMap = priceMap;
  }

  /**
   * Get the price map details.
   * 
   * @return the counter offer details
   */
  public PriceMapEmbed getCounterOffer() {
    return counterOffer;
  }

  /**
   * Set the price map details.
   * 
   * @param counterOffer
   *        the counter offer details to set
   */
  public void setCounterOffer(PriceMapEmbed counterOffer) {
    this.counterOffer = counterOffer;
  }

  /**
   * Get an optional of the price map details.
   * 
   * @return the optional price map details
   */
  @Nonnull
  public Optional<PriceMapEmbed> priceMapOpt() {
    return Optional.ofNullable(getPriceMap());
  }

  /**
   * Get the price map details, creating a new one if it doesn't already exist.
   * 
   * @return the price map details
   */
  @Nonnull
  public PriceMapEmbed priceMap() {
    PriceMapEmbed pm = getPriceMap();
    if (pm == null) {
      pm = new PriceMapEmbed();
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
