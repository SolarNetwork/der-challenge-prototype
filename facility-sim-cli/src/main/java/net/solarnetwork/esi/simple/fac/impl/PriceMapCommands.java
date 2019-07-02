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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;

import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.esi.cli.BaseShellSupport;
import net.solarnetwork.esi.simple.fac.service.PriceMapService;

/**
 * Shell commands for managing price map events.
 * 
 * @author matt
 * @version 1.0
 */
@SshShellComponent
@ShellCommandGroup("Price maps")
public class PriceMapCommands extends BaseShellSupport {

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

}
