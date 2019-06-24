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

package net.solarnetwork.esi.simple.fac.cli;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;

import com.github.fonimus.ssh.shell.PromptColor;
import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.esi.domain.DerFacilityExchangeInfo;
import net.solarnetwork.esi.domain.FormData;
import net.solarnetwork.esi.simple.fac.service.ExchangeRegistrationService;

/**
 * Shell commands for the ESI Facility Registry client.
 * 
 * @author matt
 * @version 1.0
 */
@SshShellComponent
@ShellCommandGroup("Registry")
public class RegistryCommands {

  private final SshShellHelper shell;
  private final ExchangeRegistrationService exchangeRegistrationService;

  /**
   * Constructor.
   * 
   * @param shell
   *        the shell helper
   * @param exchangeRegistrationService
   *        the exchange registration service
   */
  @Autowired
  public RegistryCommands(SshShellHelper shell,
      ExchangeRegistrationService exchangeRegistrationService) {
    super();
    this.shell = shell;
    this.exchangeRegistrationService = exchangeRegistrationService;
  }

  /**
   * List the available facilities in the registry.
   */
  @ShellMethod("List available facility exchanges.")
  public void exchangeRegistryList() {
    listExchanges();
  }

  private List<DerFacilityExchangeInfo> listExchanges() {
    List<DerFacilityExchangeInfo> result = new ArrayList<>(8);
    Iterator<DerFacilityExchangeInfo> itr = exchangeRegistrationService.listExchanges(null);
    int i = 0;
    String fmt = "  %-10s %s";
    while (itr.hasNext()) {
      i += 1;
      DerFacilityExchangeInfo info = itr.next();
      result.add(info);
      shell.print("Facility Exchange " + i, PromptColor.MAGENTA);
      shell.print(String.format(fmt, "Name", info.getName()));
      shell.print(String.format(fmt, "ID", info.getUid()));
      shell.print(String.format(fmt, "URI", info.getEndpointUri()));
    }
    shell.print("");
    return result;
  }

  /**
   * Choose a facility exchange.
   */
  @ShellMethod("Choose a facility exchange to connect to.")
  public void exchangeRegistryChoose() {
    List<DerFacilityExchangeInfo> exchanges = listExchanges();
    while (true) {
      String choice = shell.read("Which exchange would you like to use?");
      try {
        int idx = Integer.parseInt(choice);
        if (idx > 0 && idx <= exchanges.size()) {
          DerFacilityExchangeInfo exchange = exchanges.get(idx - 1);
          if (shell.confirm(String.format("You chose %s @ %s, is that correct?", exchange.getName(),
              exchange.getEndpointUri()))) {
            shell.printInfo(String.format("Sweet as, you'll need to register with %s now.",
                exchange.getName()));
            FormData.Builder formData = FormData.newBuilder();

            exchangeRegistrationService.registerWithExchange(exchange, formData.build());
            break;
          }
        } else {
          shell.printError("That number is out of range, please try again.");
        }
      } catch (NumberFormatException e) {
        shell.printError("Please enter a number.");
      }
    }
  }

}
