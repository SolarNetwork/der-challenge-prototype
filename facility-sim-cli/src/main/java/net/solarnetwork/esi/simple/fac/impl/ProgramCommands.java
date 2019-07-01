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
import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
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
public class ProgramCommands extends BaseFacilityCharacteristicsShell {

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
    super(shell, characteristicsService);
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
    List<ProgramName> allPrograms = showProgramTypes(Arrays.stream(DerProgramType.values())
        .filter(e -> e != DerProgramType.UNRECOGNIZED).map(e -> e.name()).collect(toList()));
    Set<ProgramName> choices = new TreeSet<>(PROGRAM_DISPLAY_ORDER);
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
        }
      }
      shell.print(
          messageSource.getMessage("programs.active.confirm.title", null, Locale.getDefault()));
      showProgramNames(choices);
      if (shell.confirm(
          messageSource.getMessage("programs.active.confirm.msg", null, Locale.getDefault()))) {
        characteristicsService
            .saveActiveProgramTypes(choices.stream().map(e -> e.name).collect(toSet()));
        shell.printSuccess(
            messageSource.getMessage("programs.active.saved", null, Locale.getDefault()));
      }
      return;
    }
  }

  private List<ProgramName> showProgramTypes(Collection<String> programs) {
    List<ProgramName> programDisplayNames = sortedProgramDisplayNames(programs);
    showProgramNames(programDisplayNames);
    return programDisplayNames;
  }

  private void showProgramNames(Iterable<ProgramName> programDisplayNames) {
    int idx = 1;
    for (Iterator<ProgramName> itr = programDisplayNames.iterator(); itr.hasNext();) {
      ProgramName p = itr.next();
      shell.print(messageSource.getMessage("programs.list.item", new Object[] { idx, p },
          Locale.getDefault()));
      idx++;
    }
  }

  private List<ProgramName> sortedProgramDisplayNames(Collection<String> programs) {
    return programs.stream().map(p -> new ProgramName(p)).sorted(PROGRAM_DISPLAY_ORDER)
        .collect(toList());
  }

  /**
   * A program paired with its display name.
   */
  private class ProgramName {

    private final String name;
    private final String displayName;

    private ProgramName(String name) {
      this.name = name;
      this.displayName = messageSource.getMessage(format("programs.%s.label", name),
          new Object[] { name },
          messageSource.getMessage("programs.UNRECOGNIZED.label", null, Locale.getDefault()),
          Locale.getDefault());
    }

    @Override
    public String toString() {
      return displayName;
    }

  }

  // CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINE
  private static final ProgramNameDisplayComparator PROGRAM_DISPLAY_ORDER = new ProgramNameDisplayComparator();

  /**
   * Compare programs by their display names.
   */
  private static final class ProgramNameDisplayComparator implements Comparator<ProgramName> {

    @Override
    public int compare(ProgramName o1, ProgramName o2) {
      return o1.displayName.compareToIgnoreCase(o2.displayName);
    }

  }

}
