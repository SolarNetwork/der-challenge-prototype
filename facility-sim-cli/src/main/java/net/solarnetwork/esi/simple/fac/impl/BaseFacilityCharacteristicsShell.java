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

import com.github.fonimus.ssh.shell.SshShellHelper;

import net.solarnetwork.esi.cli.BaseShellSupport;
import net.solarnetwork.esi.simple.fac.service.FacilityCharacteristicsService;

/**
 * Basic support for a shell service using the {@link FacilityCharacteristicsService}.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseFacilityCharacteristicsShell extends BaseShellSupport {

  /** The facility characteristics service. */
  protected final FacilityCharacteristicsService characteristicsService;

  /**
   * Constructor.
   * 
   * @param shell
   *        the shell helper
   * @param characteristicsService
   *        the characteristics service
   */
  public BaseFacilityCharacteristicsShell(SshShellHelper shell,
      FacilityCharacteristicsService characteristicsService) {
    super(shell);
    this.characteristicsService = characteristicsService;
  }

}
