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

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;

import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.esi.domain.DerProgramType;
import net.solarnetwork.esi.simple.fac.service.FacilityCharacteristicsService;

/**
 * Shell commands for the ESI Facility program functions.
 * 
 * @author matt
 * @version 1.0
 */
@SshShellComponent
@ShellCommandGroup("Characteristics")
public class ProgramCommands {

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
  public ProgramCommands(SshShellHelper shell,
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
  @ShellMethod("Show the currently active program types.")
  public void programsListActive() {
    Set<String> programs = characteristicsService.activeProgramTypes();
    showProgramTypes(programs);
  }

  /**
   * Configure the list of active programs.
   */
  @ShellMethod("Configure the active program types.")
  public void programsChoose() {
    List<String> allPrograms = Arrays.stream(DerProgramType.values())
        .filter(e -> e != DerProgramType.UNRECOGNIZED).map(e -> e.name()).collect(toList());
    showProgramTypes(allPrograms);
    Set<String> choices = new HashSet<>(allPrograms.size());
    while (true) {
      String selection = shell
          .read(messageSource.getMessage("programs.active.ask", null, Locale.getDefault()));
      String[] selections = selection.trim().split("\\s*[, ]+\\s*");
      for (String s : selections) {
        try {
          choices.add(allPrograms.get(Integer.parseInt(s) - 1));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
          shell.printError(messageSource.getMessage("programs.error.invalid", new Object[] { s },
              Locale.getDefault()));
          break;
        }
      }
      shell.print(
          messageSource.getMessage("programs.active.confirm.title", null, Locale.getDefault()));
      showProgramTypes(choices);
      if (shell.confirm(
          messageSource.getMessage("programs.active.confirm.msg", null, Locale.getDefault()))) {
        characteristicsService.saveActiveProgramTypes(choices);
        shell.print(messageSource.getMessage("programs.active.saved", null, Locale.getDefault()));
      }
      return;
    }
  }

  private void showProgramTypes(Collection<String> programs) {
    List<String> programDisplayNames = sortedProgramDisplayNames(programs);
    for (ListIterator<String> itr = programDisplayNames.listIterator(); itr.hasNext();) {
      String p = itr.next();
      shell.print(messageSource.getMessage("programs.list.item",
          new Object[] { itr.previousIndex() + 1, p }, Locale.getDefault()));
    }
  }

  private List<String> sortedProgramDisplayNames(Collection<String> programs) {
    String unrecognizedMsg = messageSource.getMessage("programs.UNRECOGNIZED.label", null,
        Locale.getDefault());
    return programs.stream().map(p -> {
      return messageSource.getMessage(format("programs.%s.label", p), new Object[] { p },
          unrecognizedMsg, Locale.getDefault());
    }).sorted(String.CASE_INSENSITIVE_ORDER).collect(toList());
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
