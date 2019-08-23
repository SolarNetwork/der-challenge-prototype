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

package net.solarnetwork.esi.solarnet.fac.impl;

import static net.solarnetwork.esi.cli.ShellUtils.wrap;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;

import com.github.fonimus.ssh.shell.PromptColor;
import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.esi.cli.BaseShellSupport;
import net.solarnetwork.esi.cli.ShellUtils;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityPriceMapOfferEvent;
import net.solarnetwork.esi.solarnet.fac.domain.PriceMapOfferExecutionState;
import net.solarnetwork.esi.solarnet.fac.domain.PriceMapOfferNotification.PriceMapOfferAccepted;
import net.solarnetwork.esi.solarnet.fac.domain.PriceMapOfferNotification.PriceMapOfferCountered;
import net.solarnetwork.esi.solarnet.fac.domain.PriceMapOfferNotification.PriceMapOfferExecutionStateChanged;

/**
 * Shell commands for managing price map events.
 * 
 * @author matt
 * @version 1.0
 */
@SshShellComponent
@ShellCommandGroup("Price map events")
public class PriceMapCommands extends BaseShellSupport {

  /**
   * Constructor.
   * 
   * @param shell
   *        the shell to use
   */
  @Autowired
  public PriceMapCommands(SshShellHelper shell) {
    super(shell);
  }

  /**
   * List the current price map offers.
   */
  @ShellMethod("Show the available price map offers.")
  public void priceMapOffersShow() {
    // TODO
  }

  /**
   * Handle a price map offer accepted event.
   * 
   * <p>
   * This will print a status message to the shell.
   * </p>
   * 
   * @param event
   *        the event
   */
  @Async
  @EventListener
  public void handlePriceMapOfferAccepted(PriceMapOfferAccepted event) {
    FacilityPriceMapOfferEvent offerEvent = event.getOfferEvent();

    DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
    LocalDateTime offerEventDate = LocalDateTime.ofInstant(offerEvent.getStartDate(),
        ZoneId.systemDefault());

    String msg = wrap(messageSource.getMessage(
        "priceMap.event.accepted", new Object[] { offerEvent.getId(),
            offerEvent.offerPriceMap().getInfo(), dtf.format(offerEventDate) },
        Locale.getDefault()), ShellUtils.SHELL_MAX_COLS);
    String details = offerEvent.toDetailedInfoString(messageSource);
    wallBanner(msg + "\n" + details, PromptColor.GREEN);
  }

  /**
   * Handle a price map offer accepted event.
   * 
   * <p>
   * This will print a status message to the shell.
   * </p>
   * 
   * @param event
   *        the event
   */
  @Async
  @EventListener
  public void handlePriceMapOfferExecutionStateChanged(PriceMapOfferExecutionStateChanged event) {
    FacilityPriceMapOfferEvent offerEvent = event.getOfferEvent();

    PriceMapOfferExecutionState oldState = event.getOldState();
    PriceMapOfferExecutionState newState = event.getNewState();

    String msg = wrap(
        messageSource.getMessage("priceMap.event.stateChanged",
            new Object[] { offerEvent.getId(), oldState, newState }, Locale.getDefault()),
        ShellUtils.SHELL_MAX_COLS);
    String details = offerEvent.toDetailedInfoString(messageSource);
    wallBanner(msg + "\n" + details, PromptColor.CYAN);
  }

  /**
   * Handle a price map offer countered event.
   * 
   * <p>
   * This will print a status message to the shell.
   * </p>
   * 
   * @param event
   *        the event
   */
  @Async
  @EventListener
  public void handlePriceMapOfferCountered(PriceMapOfferCountered event) {
    FacilityPriceMapOfferEvent offerEvent = event.getOfferEvent();

    DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
    LocalDateTime offerEventDate = LocalDateTime.ofInstant(offerEvent.getStartDate(),
        ZoneId.systemDefault());

    String msg = wrap(messageSource.getMessage("priceMap.event.countered",
        new Object[] { offerEvent.getId(), offerEvent.getPriceMap().getInfo(),
            dtf.format(offerEventDate), offerEvent.getCounterOffer().getInfo() },
        Locale.getDefault()), ShellUtils.SHELL_MAX_COLS);
    wallBanner(msg, PromptColor.MAGENTA);
  }

}
