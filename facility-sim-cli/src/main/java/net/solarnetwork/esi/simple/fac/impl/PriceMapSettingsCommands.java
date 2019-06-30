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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;

import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.esi.domain.jpa.DurationRangeEmbed;
import net.solarnetwork.esi.domain.jpa.PowerComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceComponentsEmbed;
import net.solarnetwork.esi.simple.fac.domain.PriceMapEntity;
import net.solarnetwork.esi.simple.fac.service.FacilityCharacteristicsService;

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
  @ShellMethod("Show the current facility price map settings.")
  public void priceMapShow() {
    PriceMapEntity priceMap = characteristicsService.priceMap();
    showPriceMap(priceMap);
  }

  /**
   * Configure the facility price map settings.
   */
  @ShellMethod("Configure the facility price map settings.")
  public void priceMapEdit() {
    PriceMapEntity priceMap = characteristicsService.priceMap();

    BigDecimal n;

    PowerComponentsEmbed p = priceMap.getPowerComponents();
    if (p == null) {
      p = new PowerComponentsEmbed(0L, 0L);
    }

    n = readNumber("priceMap.power.real", "kW", scaled(p.getRealPower(), -3), 0L,
        Long.MAX_VALUE / 1000);
    p.setRealPower(scaled(n, 3).longValue());

    n = readNumber("priceMap.power.reactive", "kVAR", scaled(p.getReactivePower(), -3), 0L,
        Long.MAX_VALUE / 1000);
    p.setReactivePower(scaled(n, 3).longValue());
    priceMap.setPowerComponents(p);

    n = readNumber("priceMap.duration", "s", scaled(priceMap.getDuration().toMillis(), -3), 0L,
        Long.MAX_VALUE / 1000);
    priceMap.setDuration(Duration.ofMillis(scaled(n, 3).longValue()));

    DurationRangeEmbed responseTime = priceMap.getResponseTime();
    if (responseTime == null) {
      responseTime = new DurationRangeEmbed(Duration.ofMillis(0), Duration.ofMillis(0));
    }
    Number min = readNumber("priceMap.responseTime.min", "s",
        scaled(responseTime.getMin().toMillis(), -3), 0L, Integer.MAX_VALUE);
    responseTime.setMin(Duration.ofMillis(scaled(min, 3).longValue()));

    n = readNumber("priceMap.responseTime.max", "s", scaled(responseTime.getMax().toMillis(), -3),
        min, Integer.MAX_VALUE);
    responseTime.setMax(Duration.ofMillis(scaled(n, 3).longValue()));
    priceMap.setResponseTime(responseTime);

    PriceComponentsEmbed pr = priceMap.getPriceComponents();
    if (pr == null) {
      pr = new PriceComponentsEmbed("USD", null, null);
    }

    String s = readString("priceMap.price.currency", "3-character code", pr.getCurrencyCode());
    pr.setCurrencyCode(s);

    n = readNumber("priceMap.price.real", pr.getCurrencyCode() + "/kWh",
        scaled(pr.getRealEnergyPrice(), -3), 0L, Long.MAX_VALUE / 1000);
    pr.setRealEnergyPrice(scaled(n, 3));

    n = readNumber("priceMap.price.apparent", pr.getCurrencyCode() + "/kVAh",
        scaled(pr.getApparentEnergyPrice(), -3), 0L, Long.MAX_VALUE / 1000);
    pr.setApparentEnergyPrice(scaled(n, 3));
    priceMap.setPowerComponents(p);

    shell.print(messageSource.getMessage("edit.confirm.title", null, Locale.getDefault()));
    showPriceMap(priceMap);
    if (shell.confirm(messageSource.getMessage("edit.confirm.ask", null, Locale.getDefault()))) {
      characteristicsService.savePriceMap(priceMap);
      shell
          .printSuccess(messageSource.getMessage("priceMap.edit.saved", null, Locale.getDefault()));
    }
  }

  private void showPriceMap(PriceMapEntity priceMap) {
    String fmt = "%-25s : %.3f %s";
    PowerComponentsEmbed p = priceMap.getPowerComponents();
    shell.print(String.format(fmt,
        messageSource.getMessage("priceMap.power.real", null, Locale.getDefault()),
        p.getRealPower() / 1000.0, "kW"));
    shell.print(String.format(fmt,
        messageSource.getMessage("priceMap.power.reactive", null, Locale.getDefault()),
        p.getReactivePower() / 1000.0, "kVAR"));
    shell.print(
        String.format(fmt, messageSource.getMessage("priceMap.duration", null, Locale.getDefault()),
            scaled(priceMap.getDuration().toMillis(), -3), "s"));
    shell.print(String.format(fmt,
        messageSource.getMessage("priceMap.responseTime.min", null, Locale.getDefault()),
        scaled(priceMap.getResponseTime().getMin().toMillis(), -3), "s"));
    shell.print(String.format(fmt,
        messageSource.getMessage("priceMap.responseTime.max", null, Locale.getDefault()),
        scaled(priceMap.getResponseTime().getMax().toMillis(), -3), "s"));

    PriceComponentsEmbed pr = priceMap.getPriceComponents();
    shell.print(String.format(fmt,
        messageSource.getMessage("priceMap.price.real", null, Locale.getDefault()),
        scaled(pr.getRealEnergyPrice(), -3), pr.getCurrencyCode() + "/kWh"));
    shell.print(String.format(fmt,
        messageSource.getMessage("priceMap.price.apparent", null, Locale.getDefault()),
        scaled(pr.getApparentEnergyPrice(), -3), pr.getCurrencyCode() + "/kVAh"));
    shell.print("");
  }

}
