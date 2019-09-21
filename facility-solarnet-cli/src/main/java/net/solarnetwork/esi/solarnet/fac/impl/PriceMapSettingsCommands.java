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

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;

import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

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

  /**
   * Post a price map to the exchange.
   */
  @ShellMethod("Post a price map to the exchange.")
  public void priceMapRegister() {
    FacilityPriceMap priceMap = promptForPriceMapFromList();
    if (priceMap == null) {
      return;
    }
    if (shell.confirm(
        messageSource.getMessage("priceMap.register.confirm.ask", null, Locale.getDefault()))) {
      characteristicsService.registerPriceMap(priceMap);
      shell.printSuccess(
          messageSource.getMessage("priceMap.register.registered", null, Locale.getDefault()));
    }
  }

  private FacilityPriceMap promptForPriceMapFromList() {
    Iterable<FacilityPriceMap> priceMaps = characteristicsService.priceMaps();
    List<FacilityPriceMap> sorted = StreamSupport.stream(priceMaps.spliterator(), false)
        .sorted(comparing(FacilityPriceMap::getDuration).thenComparing(FacilityPriceMap::getId))
        .collect(Collectors.toList());
    String id = promptForNumberedObjectListItem(sorted, "priceMap.list", "id",
        new String[] { "info" }, (k, v) -> {
          shell.print(v.priceMap().toDetailedInfoString(messageSource));
          shell.print("");
        });
    if (id == null) {
      return null;
    }
    return sorted.stream().filter(p -> id.equals(p.getId())).findFirst().orElseGet(null);
  }
}
