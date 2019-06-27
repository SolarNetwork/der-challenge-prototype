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
   * List the available facilities in the registry.
   */
  @ShellMethod("Show the current resource characteristics.")
  public void resourceShow() {
    ResourceCharacteristicsEntity characteristics = characteristicsService
        .resourceCharacteristics();
    String fmt = "%-25s : %.1f %s";
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
        (double) characteristics.getResponseTime().getMin().getSeconds(), "s"));
    shell.print(String.format(fmt,
        messageSource.getMessage("rsrc.char.responseTime.max", null, Locale.getDefault()),
        (double) characteristics.getResponseTime().getMax().getSeconds(), "s"));
    shell.print("");
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
