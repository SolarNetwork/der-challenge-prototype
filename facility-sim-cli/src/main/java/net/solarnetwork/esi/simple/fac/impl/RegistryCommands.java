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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.davidmoten.text.utils.WordWrap;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;

import com.github.fonimus.ssh.shell.PromptColor;
import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.esi.domain.DerFacilityExchangeInfo;
import net.solarnetwork.esi.domain.DerFacilityRegistrationForm;
import net.solarnetwork.esi.domain.Form;
import net.solarnetwork.esi.domain.FormData;
import net.solarnetwork.esi.domain.FormSetting;
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
            shell.printInfo(
                format("Sweet as, you'll need to register with %s now.", exchange.getName()));
            shell.print(format("\nPlease fill in the following form to register with %s.",
                exchange.getName()));

            DerFacilityRegistrationForm regForm = exchangeRegistrationService
                .getExchangeRegistrationForm(exchange, Locale.getDefault());
            handleRegisterForm(exchange, regForm);
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

  private void handleRegisterForm(DerFacilityExchangeInfo exchange,
      DerFacilityRegistrationForm regForm) {
    Form form = regForm.getForm();
    FormData.Builder answers = FormData.newBuilder();
    answers.setKey(form.getKey());
    while (true) {
      fillInForm(form, answers);
      try {
        exchangeRegistrationService.registerWithExchange(exchange, answers.build());
      } catch (IllegalArgumentException e) {
        shell.printError(e.getMessage());
        // CHECKSTYLE IGNORE LineLength FOR NEXT 2 LINES
        shell.printError(WordWrap.from(
            "Please try again. Your previous answers will be shown within [] at each prompt. You can re-used a previous answer by typing Enter at the prompt.\n")
            .maxWidth(80).wrap());
      }
    }

  }

  private void fillInForm(Form form, FormData.Builder answers) {
    int i = 1;
    for (FormSetting field : form.getSettingsList()) {
      switch (field.getType()) {
        case INFO:
          showFieldInfo(field);
          break;

        case TEXT:
        case SECURE_TEXT:
          fillInFormSetting(field, answers, i++);
          break;

        default:
          // ignore
      }
    }
  }

  private void showFieldInfo(FormSetting field) {
    shell.printInfo(WordWrap.from(field.getCaption()).maxWidth(80).wrap());
    shell.print("");
  }

  private void fillInFormSetting(FormSetting field, FormData.Builder answers, int index) {
    final String key = field.getKey();
    final String curr = answers.getDataOrDefault(key, null);
    shell.print(getBoldColored(format("%d) %s", index, field.getLabel()), PromptColor.BLACK));
    if (field.getCaption() != null && !field.getCaption().isEmpty()) {
      shell.printInfo(field.getCaption());
    }
    if (field.getPlaceholder() != null && !field.getPlaceholder().isEmpty()) {
      shell.print(getFaint(format("e.g. %s", field.getPlaceholder())));
    }
    String ans = null;
    while (ans == null || ans.trim().isEmpty()) {
      if (curr != null && !curr.isEmpty()) {
        ans = shell.read(String.format("? [%s] ", curr));
      } else {
        ans = shell.read("? ");
      }
      if (ans == null || ans.trim().isEmpty()) {
        ans = curr;
      }
    }
    answers.putData(key, ans);
  }

  /**
   * Color a bold message with given color.
   *
   * @param message
   *        message to return
   * @param color
   *        color to print
   * @return colored message
   */
  public String getBoldColored(String message, PromptColor color) {
    return new AttributedStringBuilder()
        .append(message, AttributedStyle.BOLD.foreground(color.toJlineAttributedStyle())).toAnsi();
  }

  /**
   * Color a faint message.
   *
   * @param message
   *        message to return
   * @return colored message
   */
  public String getFaint(String message) {
    return new AttributedStringBuilder().append(message, AttributedStyle.DEFAULT.faint()).toAnsi();
  }
}
