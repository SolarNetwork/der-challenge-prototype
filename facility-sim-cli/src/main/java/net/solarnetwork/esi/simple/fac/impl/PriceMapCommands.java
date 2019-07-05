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

import static net.solarnetwork.esi.cli.ShellUtils.wrap;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.fonimus.ssh.shell.PromptColor;
import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.esi.cli.BaseShellSupport;
import net.solarnetwork.esi.cli.ShellUtils;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferEventEntity;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferExecutionState;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferNotification.PriceMapOfferAccepted;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferNotification.PriceMapOfferCountered;
import net.solarnetwork.esi.simple.fac.domain.PriceMapOfferNotification.PriceMapOfferExecutionStateChanged;
import net.solarnetwork.esi.simple.fac.service.PriceMapService;

/**
 * Shell commands for managing price map events.
 * 
 * @author matt
 * @version 1.0
 */
@SshShellComponent
@ShellCommandGroup("Price map events")
public class PriceMapCommands extends BaseShellSupport {

  @Autowired
  private EntityManager em;

  private final PriceMapService priceMapService;

  /**
   * Constructor.
   * 
   * @param shell
   *        the shell to use
   * @param priceMapService
   *        the price map service to use
   */
  @Autowired
  public PriceMapCommands(SshShellHelper shell, PriceMapService priceMapService) {
    super(shell);
    this.priceMapService = priceMapService;
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
  @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
  public void handlePriceMapOfferAccepted(PriceMapOfferAccepted event) {
    PriceMapOfferEventEntity entity = em.merge(event.getOfferEvent());

    DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
    LocalDateTime offerEventDate = LocalDateTime.ofInstant(entity.getStartDate(),
        ZoneId.systemDefault());

    String msg = wrap(
        messageSource.getMessage(
            "priceMap.event.accepted", new Object[] { entity.getId(),
                entity.offerPriceMap().getInfo(), dtf.format(offerEventDate) },
            Locale.getDefault()),
        ShellUtils.SHELL_MAX_COLS);
    String details = entity.toDetailedInfoString(messageSource);
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
  @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
  public void handlePriceMapOfferExecutionStateChanged(PriceMapOfferExecutionStateChanged event) {
    PriceMapOfferEventEntity entity = em.merge(event.getOfferEvent());

    PriceMapOfferExecutionState oldState = event.getOldState();
    PriceMapOfferExecutionState newState = event.getNewState();

    String msg = wrap(
        messageSource.getMessage("priceMap.event.stateChanged",
            new Object[] { entity.getId(), oldState, newState }, Locale.getDefault()),
        ShellUtils.SHELL_MAX_COLS);
    String details = entity.toDetailedInfoString(messageSource);
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
  @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
  public void handlePriceMapOfferCountered(PriceMapOfferCountered event) {
    PriceMapOfferEventEntity entity = em.merge(event.getOfferEvent());

    DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
    LocalDateTime offerEventDate = LocalDateTime.ofInstant(entity.getStartDate(),
        ZoneId.systemDefault());

    String msg = wrap(messageSource.getMessage("priceMap.event.countered",
        new Object[] { entity.getId(), entity.getPriceMap().getInfo(), dtf.format(offerEventDate),
            entity.getCounterOffer().getPriceMap().getInfo() },
        Locale.getDefault()), ShellUtils.SHELL_MAX_COLS);
    wallBanner(msg, PromptColor.MAGENTA);
  }

}
