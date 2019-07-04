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

package net.solarnetwork.esi.simple.xchg.impl.cli;

import static java.lang.String.format;
import static java.util.stream.Collectors.toCollection;
import static net.solarnetwork.esi.cli.ShellUtils.SHELL_MAX_COLS;
import static net.solarnetwork.esi.cli.ShellUtils.getBold;
import static net.solarnetwork.esi.cli.ShellUtils.wall;
import static net.solarnetwork.esi.cli.ShellUtils.wrap;
import static net.solarnetwork.esi.util.NumberUtils.scaled;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.fonimus.ssh.shell.PromptColor;
import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.domain.jpa.PowerComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
import net.solarnetwork.esi.simple.xchg.domain.FacilityInfo;
import net.solarnetwork.esi.simple.xchg.domain.FacilityPriceMapOfferEntity;
import net.solarnetwork.esi.simple.xchg.domain.PriceMapEntity;
import net.solarnetwork.esi.simple.xchg.domain.PriceMapOfferingEntity;
import net.solarnetwork.esi.simple.xchg.domain.PriceMapOfferingEvent.FacilityPriceMapOfferCompleted;
import net.solarnetwork.esi.simple.xchg.service.FacilityCharacteristicsService;
import net.solarnetwork.esi.simple.xchg.service.PriceMapOfferingService;

/**
 * Shell commands for the ESI Facility Exchange price map functions.
 * 
 * @author matt
 * @version 1.0
 */
@SshShellComponent
@ShellCommandGroup("Price Maps")
public class PriceMapCommands extends BaseFacilityCharacteristicsShell {

  private static final String PRICE_MAP_PROP_FORMAT = "%-25s : %.3f %s";
  private static final String OFFER_PROP_FORMAT = "%-25s : %s";
  private static final Pattern RELATIVE_TIME_PAT = Pattern.compile("\\+\\s*(\\d+)\\s*(\\w)");

  private final PriceMapOfferingService offerService;

  @Autowired
  private EntityManager em;

  /**
   * Constructor.
   * 
   * @param shell
   *        the shell helper
   * @param characteristicsService
   *        the characteristics service
   */
  @Autowired
  public PriceMapCommands(SshShellHelper shell,
      FacilityCharacteristicsService characteristicsService, PriceMapOfferingService offerService) {
    super(shell, characteristicsService);
    this.offerService = offerService;
  }

  /**
   * List the current resource characteristics.
   * 
   * @param facilityUid
   *        an optional facility UID to view the characteristics for; if not provided a list of
   *        available facilities to choose from will be shown
   */
  @ShellMethod("Show the price map characteristics for a facility.")
  public void priceMapsShow(
      @ShellOption(value = { "--facility", "-f" }, defaultValue = "") String facilityUid) {
    if (facilityUid == null || facilityUid.trim().isEmpty()) {
      facilityUid = promptForFacilityUidFromList();
    }
    if (facilityUid == null) {
      return;
    }
    try {
      FacilityInfo info = characteristicsService.facilityInfo(facilityUid);
      shell.print(getBold(messageSource.getMessage("priceMap.list.title",
          new Object[] { facilityUid, info.getCustomerId() }, Locale.getDefault())));
      Iterable<PriceMapEntity> priceMaps = characteristicsService.priceMaps(facilityUid);
      int idx = 1;
      for (PriceMapEntity priceMap : priceMaps) {
        shell.print(getBold(messageSource.getMessage("priceMap.list.item",
            new Object[] { idx, priceMap.getId() }, Locale.getDefault())));
        showPriceMap(priceMap.priceMap());
        shell.print("");
      }
    } catch (IllegalArgumentException e) {
      shell.printError(messageSource.getMessage("list.facility.missing",
          new Object[] { facilityUid }, Locale.getDefault()));
    }
  }

  /**
   * Create a price map offer for a set of facilities.
   * 
   * @param facilityUids
   *        the facility UIDs to create the offer for if not provided a list of available facilities
   *        to choose from will be shown
   */
  @ShellMethod("Create a price map offer for facilities.")
  public void priceMapOfferCreate(
      @ShellOption(value = { "--facility", "-f" }, defaultValue = "") List<String> facilityUids) {
    DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
    while (true) {
      final Instant startDate = promptForInstant("offer.startDate.ask", dtf);
      Set<String> uids;
      if (facilityUids == null || facilityUids.isEmpty()) {
        uids = promptForFacilityUidsFromList();
      } else {
        uids = facilityUids.stream().collect(toCollection(LinkedHashSet::new));
      }
      PriceMapEmbed priceMap = promptForPriceMap(null);

      shell
          .print(messageSource.getMessage("offer.create.confirm.title", null, Locale.getDefault()));

      showPriceMap(priceMap);
      shell.print(String.format(OFFER_PROP_FORMAT,
          messageSource.getMessage("offer.startDate", null, Locale.getDefault()),
          dtf.format(LocalDateTime.ofInstant(startDate, ZoneId.systemDefault()))));

      Iterator<String> itr = uids.iterator();
      if (itr.hasNext()) {
        shell.print(String.format(OFFER_PROP_FORMAT,
            messageSource.getMessage("offer.facilities", null, Locale.getDefault()), itr.next()));
      }
      while (itr.hasNext()) {
        shell.print(String.format(OFFER_PROP_FORMAT, "", itr.next()));
      }
      if (shell.confirm(
          messageSource.getMessage("offer.create.confirm.ask", null, Locale.getDefault()))) {
        if (shell.confirm(
            messageSource.getMessage("offer.create.submit.ask", null, Locale.getDefault()))) {
          PriceMapOfferingEntity offering = offerService.createPriceMapOffering(priceMap,
              startDate);
          shell.printSuccess(wrap(messageSource.getMessage("offering.create.submit.success",
              new Object[] { offering.getId(),
                  dtf.format(LocalDateTime.ofInstant(startDate, ZoneId.systemDefault())) },
              Locale.getDefault())));
          shell.print("");
          offerService.makeOfferToFacilities(offering.getId(), uids);
          shell.printSuccess(wrap(messageSource.getMessage("offering.offers.submit.success",
              new Object[] { uids.size() }, Locale.getDefault())));
        }
        return;
      }
    }
  }

  private Instant promptForInstant(String code, DateTimeFormatter dtf) {
    while (true) {
      String ans = shell.read(wrap(messageSource.getMessage(code,
          new Object[] { dtf.format(LocalDateTime.now()) }, Locale.getDefault())));
      if (ans == null) {
        continue;
      }
      Matcher m = RELATIVE_TIME_PAT.matcher(ans);
      if (m.matches()) {
        int offset = Integer.parseInt(m.group(1));
        char unit = m.group(2).toLowerCase().charAt(0);
        long seconds = 0;
        switch (unit) {
          case 's':
            seconds = offset;
            break;
          case 'm':
            seconds = TimeUnit.MINUTES.toSeconds(offset);
            break;
          case 'h':
            seconds = TimeUnit.HOURS.toSeconds(offset);
            break;
          default:
            shell.printError(messageSource.getMessage("error.timeOffset.unknown",
                new Object[] { unit }, Locale.getDefault()));
        }
        if (seconds > 0) {
          return Instant.now().plusSeconds(seconds);
        }
      } else {
        try {
          return dtf.parse(ans, Instant::from);
        } catch (DateTimeParseException e) {
          shell.printError(messageSource.getMessage("error.datetime.parse",
              new Object[] { ans, e.getLocalizedMessage() }, Locale.getDefault()));
        }
      }
    }
  }

  private PriceMapEmbed promptForPriceMap(PriceMapEmbed defaults) {
    PriceMapEmbed priceMap = (defaults != null ? defaults.copy() : new PriceMapEmbed());
    while (true) {
      BigDecimal n;

      PowerComponentsEmbed p = priceMap.powerComponents();
      n = readNumber("priceMap.power.real", "kW", scaled(p.getRealPower(), -3), 0L,
          Long.MAX_VALUE / 1000);
      p.setRealPower(scaled(n, 3).longValue());

      n = readNumber("priceMap.power.reactive", "kVAR", scaled(p.getReactivePower(), -3), 0L,
          Long.MAX_VALUE / 1000);
      p.setReactivePower(scaled(n, 3).longValue());
      priceMap.setPowerComponents(p);

      n = readNumber("priceMap.duration", "s", scaled(priceMap.duration().toMillis(), -3), 0L,
          Long.MAX_VALUE / 1000);
      priceMap.setDuration(Duration.ofMillis(scaled(n, 3).longValue()));

      DurationRangeEmbed responseTime = priceMap.responseTime();
      Number min = readNumber("priceMap.responseTime.min", "s",
          scaled(responseTime.min().toMillis(), -3), 0L, Integer.MAX_VALUE);
      responseTime.setMin(Duration.ofMillis(scaled(min, 3).longValue()));

      n = readNumber("priceMap.responseTime.max", "s", scaled(responseTime.max().toMillis(), -3),
          min, Integer.MAX_VALUE);
      responseTime.setMax(Duration.ofMillis(scaled(n, 3).longValue()));

      PriceComponentsEmbed pr = priceMap.priceComponents();
      String s = readString("priceMap.price.currency", "3-character code",
          pr.getCurrency().getCurrencyCode());
      pr.setCurrency(Currency.getInstance(s));

      n = readNumber("priceMap.price.apparent", pr.getCurrency().getCurrencyCode() + "/kVAh",
          scaled(pr.getApparentEnergyPrice(), 3), 0L, Long.MAX_VALUE / 1000);
      pr.setApparentEnergyPrice(scaled(n, -3));

      shell.print(
          messageSource.getMessage("priceMap.edit.confirm.title", null, Locale.getDefault()));
      showPriceMap(priceMap);
      shell.print("");
      if (shell.confirm(
          messageSource.getMessage("priceMap.edit.confirm.ask", null, Locale.getDefault()))) {
        return priceMap;
      }
    }
  }

  /**
   * Handle a price map offer completed event.
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
  public void handleFacilityPriceMapOfferCompletedEvent(FacilityPriceMapOfferCompleted event) {
    FacilityPriceMapOfferEntity offer = event.getOffer();
    offer = em.merge(offer);
    PriceMapEmbed priceMap = offer.offerPriceMap();
    String armor = messageSource.getMessage("event.armor", null, Locale.getDefault());
    String msg = String.format("\n%s\n%s\n\n%s\n%s\n", armor,
        wrap(messageSource.getMessage(
            event.isSuccess() ? "offer.event.completed.accepted" : "offer.event.completed.declined",
            new Object[] { offer.getId(), offer.getFacility().getFacilityUid() },
            Locale.getDefault()), SHELL_MAX_COLS),
        renderPriceMap(priceMap), armor);

    // broadcast message to all available registered terminals
    wall(shell.getColored(msg, event.isSuccess() ? PromptColor.GREEN : PromptColor.RED));
  }

  private String promptForFacilityUidFromList() {
    Iterable<FacilityInfo> facilities = characteristicsService.listFacilities();
    List<String> uids = showFacilityList(facilities);
    if (uids.isEmpty()) {
      shell.printWarning(messageSource.getMessage("list.facility.none", null, Locale.getDefault()));
      return null;
    }

    while (true) {
      String ans = shell
          .read(messageSource.getMessage("list.facility.pick", null, Locale.getDefault()));
      try {
        int index = Integer.parseInt(ans) - 1;
        return uids.get(index);
      } catch (NumberFormatException e) {
        shell.printError(
            messageSource.getMessage("answer.error.enterNumber", null, Locale.getDefault()));
      } catch (ArrayIndexOutOfBoundsException e) {
        shell.printError(messageSource.getMessage("answer.error.numOutOfRange",
            new Object[] { 1, uids.size() }, Locale.getDefault()));
      }
    }
  }

  private List<String> showFacilityList(Iterable<FacilityInfo> facilities) {
    int idx = 0;
    List<String> uids = new ArrayList<>();
    for (FacilityInfo info : facilities) {
      idx++;
      uids.add(info.getFacilityUid());
      shell.print(messageSource.getMessage("list.facility.item",
          new Object[] { idx, info.getCustomerId(), info.getFacilityUid() }, Locale.getDefault()));
    }
    return uids;
  }

  @Nonnull
  private Set<String> promptForFacilityUidsFromList() {
    while (true) {
      Iterable<FacilityInfo> facilities = characteristicsService.listFacilities();
      List<String> uids = showFacilityList(facilities);
      if (uids.isEmpty()) {
        shell.printWarning(
            messageSource.getMessage("list.facility.none", null, Locale.getDefault()));
        return Collections.emptySet();
      }

      String ans = shell
          .read(messageSource.getMessage("facilities.list.pick", null, Locale.getDefault()));
      if (ans == null || ans.trim().isEmpty()) {
        return Collections.emptySet();
      }
      String[] words = ans.trim().split("\\s*[, ]+\\s*");
      Set<String> choices = new TreeSet<>(new Comparator<String>() {

        @Override
        public int compare(String o1, String o2) {
          int l = uids.indexOf(o1);
          int r = uids.indexOf(o2);
          return (l < r ? -1 : l > r ? 1 : 0);
        }
      });
      for (String word : words) {
        try {
          int index = Integer.parseInt(word) - 1;
          choices.add(uids.get(index));
        } catch (NumberFormatException e) {
          shell.printError(
              messageSource.getMessage("answer.error.enterNumber", null, Locale.getDefault()));
        } catch (ArrayIndexOutOfBoundsException e) {
          shell.printError(messageSource.getMessage("answer.error.numOutOfRange",
              new Object[] { 1, uids.size() }, Locale.getDefault()));
        }
      }
      shell.print(
          messageSource.getMessage("facilities.list.confirm.title", null, Locale.getDefault()));
      showFacilityList(StreamSupport.stream(facilities.spliterator(), false)
          .filter(e -> uids.contains(e.getFacilityUid())).collect(Collectors.toList()));
      if (shell.confirm(
          messageSource.getMessage("facilities.list.confirm.msg", null, Locale.getDefault()))) {
        return choices;
      }
    }
  }

  private void showPriceMap(PriceMapEmbed priceMap) {
    shell.print(renderPriceMap(priceMap));
  }

  private String renderPriceMap(PriceMapEmbed priceMap) {
    // use PrintWriter for proper line.separator support
    try (StringWriter buf = new StringWriter(); PrintWriter out = new PrintWriter(buf)) {

      PowerComponentsEmbed p = priceMap.powerComponents();
      out.println(format(PRICE_MAP_PROP_FORMAT,
          messageSource.getMessage("priceMap.power.real", null, Locale.getDefault()),
          p.getRealPower() / 1000.0, "kW"));
      out.println(format(PRICE_MAP_PROP_FORMAT,
          messageSource.getMessage("priceMap.power.reactive", null, Locale.getDefault()),
          p.getReactivePower() / 1000.0, "kVAR"));
      out.println(format(PRICE_MAP_PROP_FORMAT,
          messageSource.getMessage("priceMap.duration", null, Locale.getDefault()),
          scaled(priceMap.duration().toMillis(), -3), "s"));
      out.println(format(PRICE_MAP_PROP_FORMAT,
          messageSource.getMessage("priceMap.responseTime.min", null, Locale.getDefault()),
          scaled(priceMap.responseTime().min().toMillis(), -3), "s"));
      out.println(String.format(PRICE_MAP_PROP_FORMAT,
          messageSource.getMessage("priceMap.responseTime.max", null, Locale.getDefault()),
          scaled(priceMap.responseTime().max().toMillis(), -3), "s"));

      PriceComponentsEmbed pr = priceMap.priceComponents();
      out.print(String.format(PRICE_MAP_PROP_FORMAT,
          messageSource.getMessage("priceMap.price.apparent", null, Locale.getDefault()),
          scaled(pr.apparentEnergyPrice(), 3), pr.currency().getCurrencyCode() + "/kVAh"));
      // note last line *no* println() because shell.print() does that
      out.flush();
      return buf.toString();
    } catch (IOException e) {
      log.error("Error rendering price map", e);
      return "";
    }
  }
}
