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
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;

import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.esi.simple.fac.domain.ResourceCharacteristicsEntity;
import net.solarnetwork.esi.simple.fac.service.FacilityCharacteristicsService;

/**
 * Shell commands for the ESI Facility resource characteristic functions.
 * 
 * @author matt
 * @version 1.0
 */
@SshShellComponent
@ShellCommandGroup("Characteristics")
public class ResourceCharacteristicsCommands {

  private final SshShellHelper shell;
  private final FacilityCharacteristicsService characteristicsService;
  private MessageSource messageSource;

  /**
   * Constructor.
   * 
   * @param shell
   *        the shell helper
   * @param characteristicsService
   *        the characteristics service
   */
  @Autowired
  public ResourceCharacteristicsCommands(SshShellHelper shell,
      FacilityCharacteristicsService characteristicsService) {
    super();
    this.shell = shell;
    this.characteristicsService = characteristicsService;

    ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
    ms.setBasename(getClass().getName());
    setMessageSource(ms);
  }

  /**
   * List the current resource characteristics.
   */
  @ShellMethod("Show the current resource characteristics.")
  public void resourceShow() {
    ResourceCharacteristicsEntity characteristics = characteristicsService
        .resourceCharacteristics();
    showResourceCharacteristics(characteristics);
  }

  /**
   * Edit the resource characteristics.
   */
  @ShellMethod("Show the current resource characteristics.")
  public void resourceEdit() {
    ResourceCharacteristicsEntity characteristics = characteristicsService
        .resourceCharacteristics();

    BigDecimal n;

    n = readNumber("rsrc.char.loadPowerMax", "kW", scaled(characteristics.getLoadPowerMax(), -3),
        0L, Long.MAX_VALUE / 1000);
    characteristics.setLoadPowerMax(scaled(n, 3).longValue());

    n = readNumber("rsrc.char.loadPowerFactor", "-1..1",
        scaled(characteristics.getLoadPowerFactor(), 0), -1, 1);
    characteristics.setLoadPowerFactor(n.floatValue());

    n = readNumber("rsrc.char.supplyPowerMax", "kW",
        scaled(characteristics.getSupplyPowerMax(), -3), 0L, Long.MAX_VALUE / 1000);
    characteristics.setSupplyPowerMax(scaled(n, 3).longValue());

    n = readNumber("rsrc.char.supplyPowerFactor", "-1..1",
        scaled(characteristics.getSupplyPowerFactor(), 0), -1, 1);
    characteristics.setSupplyPowerFactor(n.floatValue());

    n = readNumber("rsrc.char.storageEnergyCapacity", "kWh",
        scaled(characteristics.getStorageEnergyCapacity(), -3), 0L, Long.MAX_VALUE / 1000);
    characteristics.setStorageEnergyCapacity(scaled(n, 3).longValue());

    Number min = readNumber("rsrc.char.responseTime.min", "s",
        scaled(characteristics.getResponseTime().getMin().toMillis(), -3), 0L, Integer.MAX_VALUE);
    characteristics.getResponseTime().setMin(Duration.ofMillis(scaled(min, 3).longValue()));

    n = readNumber("rsrc.char.responseTime.max", "s",
        scaled(characteristics.getResponseTime().getMax().toMillis(), -3), min, Integer.MAX_VALUE);
    characteristics.getResponseTime().setMax(Duration.ofMillis(scaled(n, 3).longValue()));

    shell.print(messageSource.getMessage("rsrc.edit.confirm.title", null, Locale.getDefault()));
    showResourceCharacteristics(characteristics);
    if (shell
        .confirm(messageSource.getMessage("rsrc.edit.confirm.ask", null, Locale.getDefault()))) {
      characteristicsService.saveResourceCharacteristics(characteristics);
      shell.printSuccess(messageSource.getMessage("rsrc.edit.saved", null, Locale.getDefault()));
    }
  }

  private void showResourceCharacteristics(ResourceCharacteristicsEntity characteristics) {
    String fmt = "%-25s : %.3f %s";
    shell.print(String.format(fmt,
        messageSource.getMessage("rsrc.char.loadPowerMax", null, Locale.getDefault()),
        characteristics.getLoadPowerMax() / 1000.0, "kW"));
    shell.print(String.format(fmt,
        messageSource.getMessage("rsrc.char.loadPowerFactor", null, Locale.getDefault()),
        characteristics.getLoadPowerFactor(), ""));
    shell.print(String.format(fmt,
        messageSource.getMessage("rsrc.char.supplyPowerMax", null, Locale.getDefault()),
        characteristics.getSupplyPowerMax() / 1000.0, "kW"));
    shell.print(String.format(fmt,
        messageSource.getMessage("rsrc.char.supplyPowerFactor", null, Locale.getDefault()),
        characteristics.getSupplyPowerFactor(), ""));
    shell.print(String.format(fmt,
        messageSource.getMessage("rsrc.char.storageEnergyCapacity", null, Locale.getDefault()),
        characteristics.getStorageEnergyCapacity() / 1000.0, "kWh"));
    shell.print(String.format(fmt,
        messageSource.getMessage("rsrc.char.responseTime.min", null, Locale.getDefault()),
        scaled(characteristics.getResponseTime().getMin().toMillis(), -3), "s"));
    shell.print(String.format(fmt,
        messageSource.getMessage("rsrc.char.responseTime.max", null, Locale.getDefault()),
        scaled(characteristics.getResponseTime().getMax().toMillis(), -3), "s"));
    shell.print("");
  }

  private static BigDecimal scaled(Number num, int scale) {
    BigDecimal n = (num instanceof BigDecimal ? (BigDecimal) num : new BigDecimal(num.toString()));
    if (scale == 0) {
      return n;
    } else if (scale < 0) {
      return n.movePointLeft(-scale);
    } else {
      return n.movePointRight(scale);
    }
  }

  private BigDecimal readNumber(String propName, String unit, BigDecimal currValue, Number min,
      Number max) {
    while (true) {
      String val = shell.read(messageSource.getMessage(
          "rsrc.edit.property", new Object[] {
              messageSource.getMessage(propName, null, Locale.getDefault()), unit, currValue },
          Locale.getDefault()));
      if (val == null || val.trim().isEmpty()) {
        return currValue;
      }
      try {
        BigDecimal num = new BigDecimal(val);
        if ((min != null && num.doubleValue() < min.doubleValue())
            || (max != null && num.doubleValue() > max.doubleValue())) {
          shell.printError(messageSource.getMessage("rsrc.error.numOutOfRange",
              new Object[] { min, max }, Locale.getDefault()));
        } else {
          return num;
        }
      } catch (NumberFormatException e) {
        shell.printError(
            messageSource.getMessage("rsrc.error.enterNumber", null, Locale.getDefault()));
      }
    }
  }

  /**
   * Configure the message source.
   * 
   * @param messageSource
   *        the message source to set
   */
  public void setMessageSource(MessageSource messageSource) {
    this.messageSource = messageSource;
  }
}
