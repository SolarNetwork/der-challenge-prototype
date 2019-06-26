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

import static net.solarnetwork.esi.simple.fac.impl.ShellUtils.getBold;
import static net.solarnetwork.esi.simple.fac.impl.ShellUtils.getBoldColored;
import static net.solarnetwork.esi.simple.fac.impl.ShellUtils.getFaint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.davidmoten.text.utils.WordWrap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
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
import net.solarnetwork.esi.domain.FormSetting.FormSettingType;
import net.solarnetwork.esi.simple.fac.domain.ExchangeRegistrationEntity;
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

  private static final int SHELL_MAX_COLS = 80;

  private final SshShellHelper shell;
  private final ExchangeRegistrationService exchangeRegistrationService;
  private MessageSource messageSource;

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

    ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
    ms.setBasename(getClass().getName());
    setMessageSource(ms);
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
    Iterable<DerFacilityExchangeInfo> infos = exchangeRegistrationService.listExchanges(null);
    int i = 0;
    String fmt = "  %-10s %s";
    for (DerFacilityExchangeInfo info : infos) {
      i += 1;
      result.add(info);
      shell.print(messageSource.getMessage("reg.registry.entry.title", new Object[] { i },
          Locale.getDefault()), PromptColor.MAGENTA);
      shell.print(String.format(fmt,
          messageSource.getMessage("reg.registry.entry.name", null, Locale.getDefault()),
          info.getName()));
      shell.print(String.format(fmt,
          messageSource.getMessage("reg.registry.entry.id", null, Locale.getDefault()),
          info.getUid()));
      shell.print(String.format(fmt,
          messageSource.getMessage("reg.registry.entry.uri", null, Locale.getDefault()),
          info.getEndpointUri()));
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
      String choice = shell
          .read(messageSource.getMessage("reg.registry.choose", null, Locale.getDefault()));
      try {
        int idx = Integer.parseInt(choice);
        if (idx > 0 && idx <= exchanges.size()) {
          DerFacilityExchangeInfo exchange = exchanges.get(idx - 1);
          if (shell.confirm(messageSource.getMessage("reg.registry.exchangeConfirm",
              new Object[] { exchange.getName(), exchange.getEndpointUri() },
              Locale.getDefault()))) {
            shell.printSuccess(messageSource.getMessage("reg.registry.exchangeSelected",
                new Object[] { exchange.getName() }, Locale.getDefault()));
            shell.print("");
            shell.print(messageSource.getMessage("reg.form.intro",
                new Object[] { exchange.getName() }, Locale.getDefault()));
            shell.print("");

            DerFacilityRegistrationForm regForm = exchangeRegistrationService
                .getExchangeRegistrationForm(exchange, Locale.getDefault());
            handleRegisterForm(exchange, regForm);
            break;
          }
        } else {
          shell.printError(
              messageSource.getMessage("reg.error.numOutOfRange", null, Locale.getDefault()));
        }
      } catch (NumberFormatException e) {
        shell.printError(
            messageSource.getMessage("reg.error.enterNumber", null, Locale.getDefault()));
      }
    }
  }

  private void handleRegisterForm(DerFacilityExchangeInfo exchange,
      DerFacilityRegistrationForm regForm) {
    Form form = regForm.getForm();
    FormData.Builder answers = FormData.newBuilder();
    answers.setKey(form.getKey());

    // execute within loop so can re-try while preserving previously-entered answer values
    boolean keepGoing = true;
    while (keepGoing) {
      // capture answers for all form fields
      fillInForm(form, answers);

      // print out all answers and allow user to re-enter them if desired
      shell.print(messageSource.getMessage("reg.form.confirm.intro", null, Locale.getDefault()));
      shell.print("");
      showForm(form, answers);
      shell.print("");
      if (!shell
          .confirm(messageSource.getMessage("reg.form.confirm.edit", null, Locale.getDefault()))) {

        // give user change to cancel submitting the form
        if (shell.confirm(
            messageSource.getMessage("reg.form.confirm.submit", null, Locale.getDefault()))) {
          // we're all clear to submit the form now, so do so
          try {
            ExchangeRegistrationEntity reg = exchangeRegistrationService
                .registerWithExchange(exchange, answers.build());
            keepGoing = false;
            // if we're here, the submission was successful
            shell.printSuccess(WordWrap
                .from(messageSource.getMessage("reg.success",
                    new Object[] { exchange.getName(), reg.getId() }, Locale.getDefault()))
                .maxWidth(SHELL_MAX_COLS).wrap());
            shell.print("");
          } catch (IllegalArgumentException e) {
            // there was some error submitting the form; allow the user the chance to re-try
            shell.printError(e.getMessage());
            if (shell.confirm(
                messageSource.getMessage("reg.form.tryAgain", null, Locale.getDefault()))) {
              shell.printError(WordWrap
                  .from(messageSource.getMessage("reg.error.tryAgain", null, Locale.getDefault()))
                  .maxWidth(SHELL_MAX_COLS).wrap());
              shell.print("");
            } else {
              keepGoing = false;
            }
          }
        } else {
          keepGoing = false;
        }
      }
    }
  }

  private void showForm(Form form, FormData.Builder answers) {
    int i = 1;
    for (FormSetting field : form.getSettingsList()) {
      if (field.getType() == FormSettingType.INFO) {
        continue;
      }
      shell.print(messageSource.getMessage(
          "reg.form.field.display", new Object[] { ShellUtils.getBold(String.valueOf(i)),
              getBold(field.getLabel()), answers.getDataOrDefault(field.getKey(), "") },
          Locale.getDefault()));
      i += 1;
    }
    shell.print("");
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
    shell.printInfo(WordWrap.from(field.getCaption()).maxWidth(SHELL_MAX_COLS).wrap());
    shell.print("");
  }

  private void fillInFormSetting(FormSetting field, FormData.Builder answers, int index) {
    final String key = field.getKey();
    final String curr = answers.getDataOrDefault(key, null);
    shell.print(getBoldColored(messageSource.getMessage("reg.form.field.title",
        new Object[] { index, field.getLabel() }, Locale.getDefault()), PromptColor.BLACK));
    if (field.getCaption() != null && !field.getCaption().isEmpty()) {
      shell.printInfo(field.getCaption());
    }
    if (field.getPlaceholder() != null && !field.getPlaceholder().isEmpty()) {
      shell.print(getFaint(messageSource.getMessage("reg.form.field.caption",
          new Object[] { field.getPlaceholder() }, Locale.getDefault())));
    }
    String ans = null;
    while (ans == null || ans.trim().isEmpty()) {
      if (curr != null && !curr.isEmpty()) {
        ans = shell.read(String.format("? [%s] ", curr));
        if (ans == null || ans.trim().isEmpty()) {
          ans = curr;
        }
      } else {
        ans = shell.read("? ");
      }
      if (ans == null || ans.trim().isEmpty()) {
        messageSource.getMessage("reg.error.enterValue", null, Locale.getDefault());
      }
    }
    answers.putData(key, ans);
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
