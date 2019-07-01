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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.esi.cli.ShellUtils;
import net.solarnetwork.esi.domain.jpa.PowerComponentsEmbed;
import net.solarnetwork.esi.domain.jpa.PriceComponentsEmbed;
import net.solarnetwork.esi.simple.xchg.domain.FacilityInfo;
import net.solarnetwork.esi.simple.xchg.domain.PriceMapEntity;
import net.solarnetwork.esi.simple.xchg.service.FacilityCharacteristicsService;

/**
 * Shell commands for the ESI Facility Exchange price map functions.
 * 
 * @author matt
 * @version 1.0
 */
@SshShellComponent
@ShellCommandGroup("Price Maps")
public class PriceMapCommands extends BaseFacilityCharacteristicsShell {

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
      FacilityCharacteristicsService characteristicsService) {
    super(shell, characteristicsService);
  }

  /**
   * List the current resource characteristics.
   */
  @ShellMethod("Show the price map characteristics for a facility.")
  public void priceMapShow(
      @ShellOption(value = { "--facility", "-f" }, defaultValue = "") String facilityUid) {
    if (facilityUid == null || facilityUid.trim().isEmpty()) {
      facilityUid = promptForFacilityUidFromList();
    }
    if (facilityUid == null) {
      return;
    }
    try {
      FacilityInfo info = characteristicsService.facilityInfo(facilityUid);
      PriceMapEntity priceMap = characteristicsService.priceMap(facilityUid);
      shell.print(ShellUtils.getBold(messageSource.getMessage("priceMap.title",
          new Object[] { facilityUid, info.getCustomerId() }, Locale.getDefault())));
      showPriceMap(priceMap);
    } catch (IllegalArgumentException e) {
      shell.printError(messageSource.getMessage("list.facility.missing",
          new Object[] { facilityUid }, Locale.getDefault()));
    }
  }

  private String promptForFacilityUidFromList() {
    Iterable<FacilityInfo> facilities = characteristicsService.listFacilities();
    int idx = 0;
    List<String> uids = new ArrayList<>();
    for (FacilityInfo info : facilities) {
      idx++;
      uids.add(info.getFacilityUid());
      shell.print(messageSource.getMessage("list.facility.item",
          new Object[] { idx, info.getCustomerId(), info.getFacilityUid() }, Locale.getDefault()));
    }
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
        scaled(pr.getRealEnergyPrice(), -3), pr.getCurrency().getCurrencyCode() + "/kWh"));
    shell.print(String.format(fmt,
        messageSource.getMessage("priceMap.price.apparent", null, Locale.getDefault()),
        scaled(pr.getApparentEnergyPrice(), -3), pr.getCurrency().getCurrencyCode() + "/kVAh"));
    shell.print("");
  }
}
