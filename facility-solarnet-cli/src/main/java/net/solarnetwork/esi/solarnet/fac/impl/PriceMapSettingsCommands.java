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

import static java.util.Comparator.comparing;
import static net.solarnetwork.esi.util.NumberUtils.scaled;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;

import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.domain.jpa.PowerComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceMapEmbed;
import net.solarnetwork.esi.solarnet.fac.domain.FacilityPriceMap;
import net.solarnetwork.esi.solarnet.fac.service.FacilityCharacteristicsService;

/**
 * Shell commands for configuring the ESI Facility price map.
 * 
 * @author matt
 * @version 1.0
 */
@SshShellComponent
@ShellCommandGroup("Characteristics")
public class PriceMapSettingsCommands extends BaseFacilityCharacteristicsShell {

  /**
   * Constructor.
   * 
   * @param shell
   *        the shell helper
   * @param characteristicsService
   *        the characteristics service
   */
  @Autowired
  public PriceMapSettingsCommands(SshShellHelper shell,
      FacilityCharacteristicsService characteristicsService) {
    super(shell, characteristicsService);
  }

  /**
   * List the current facility price map settings.
   */
  @ShellMethod("List the current facility price map settings.")
  public void priceMapList() {
    Iterable<FacilityPriceMap> priceMaps = characteristicsService.priceMaps();
    List<FacilityPriceMap> sorted = StreamSupport.stream(priceMaps.spliterator(), false)
        .sorted(comparing(FacilityPriceMap::getDuration).thenComparing(FacilityPriceMap::getId))
        .collect(Collectors.toList());
    showNumberedObjectList(sorted, "priceMap.list.item", "id", new String[] { "info" }, (k, v) -> {
      shell.print(v.priceMap().toDetailedInfoString(messageSource));
      shell.print("");
    });
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
      while (true) {
        String s = readString("priceMap.price.currency", "3-character code",
            pr.getCurrency().getCurrencyCode());
        try {
          pr.setCurrency(Currency.getInstance(s));
          break;
        } catch (IllegalArgumentException e) {
          shell.printError(messageSource.getMessage("answer.error.invalidCurrencyCode", null,
              Locale.getDefault()));
        }
      }

      n = readNumber("priceMap.price.apparent", pr.getCurrency().getCurrencyCode() + "/kVAh",
          scaled(pr.getApparentEnergyPrice(), 3), 0L, Long.MAX_VALUE / 1000);
      pr.setApparentEnergyPrice(scaled(n, -3));

      shell.print(
          messageSource.getMessage("priceMap.edit.confirm.title", null, Locale.getDefault()));
      shell.print(priceMap.toDetailedInfoString(messageSource));
      shell.print("");
      if (shell.confirm(
          messageSource.getMessage("priceMap.edit.confirm.ask", null, Locale.getDefault()))) {
        return priceMap;
      }
    }
  }

  private Long promptForPriceMapIdFromList() {
    Iterable<FacilityPriceMap> priceMaps = characteristicsService.priceMaps();
    List<FacilityPriceMap> sorted = StreamSupport.stream(priceMaps.spliterator(), false)
        .sorted(comparing(FacilityPriceMap::getDuration).thenComparing(FacilityPriceMap::getId))
        .collect(Collectors.toList());
    return promptForNumberedObjectListItem(sorted, "priceMap.list", "id", new String[] { "info" },
        (k, v) -> {
          shell.print(v.priceMap().toDetailedInfoString(messageSource));
          shell.print("");
        });
  }
}
