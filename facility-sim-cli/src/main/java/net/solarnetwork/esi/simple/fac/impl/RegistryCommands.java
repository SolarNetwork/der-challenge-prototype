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
import static net.solarnetwork.esi.cli.ShellUtils.getBold;
import static net.solarnetwork.esi.cli.ShellUtils.getBoldColored;
import static net.solarnetwork.esi.cli.ShellUtils.getFaint;
import static net.solarnetwork.esi.cli.ShellUtils.wall;
import static net.solarnetwork.esi.cli.ShellUtils.wrap;

import java.util.Locale;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;

import com.github.fonimus.ssh.shell.PromptColor;
import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.esi.cli.BaseShellSupport;
import net.solarnetwork.esi.cli.ShellUtils;
import net.solarnetwork.esi.domain.DerFacilityExchangeInfo;
import net.solarnetwork.esi.domain.DerFacilityRegistrationForm;
import net.solarnetwork.esi.domain.Form;
import net.solarnetwork.esi.domain.FormData;
import net.solarnetwork.esi.domain.FormSetting;
import net.solarnetwork.esi.domain.FormSetting.FormSettingType;
import net.solarnetwork.esi.simple.fac.domain.ExchangeRegistrationEntity;
import net.solarnetwork.esi.simple.fac.domain.ExchangeRegistrationEvent.ExchangeRegistrationCompleted;
import net.solarnetwork.esi.simple.fac.service.ExchangeRegistrationService;

/**
 * Shell commands for the ESI Facility registration functions.
 * 
 * @author matt
 * @version 1.0
 */
@SshShellComponent
@ShellCommandGroup("Registry")
public class RegistryCommands extends BaseShellSupport {

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
    super(shell);
    this.exchangeRegistrationService = exchangeRegistrationService;
  }

  /**
   * List the available facilities in the registry.
   */
  @ShellMethod("List available facility exchanges.")
  public void exchangeList() {
    listExchanges();
  }

  /**
   * Choose a facility exchange.
   */
  @ShellMethod("Choose a facility exchange to connect to.")
  public void exchangeChoose() {
    Iterable<DerFacilityExchangeInfo> infos = exchangeRegistrationService.listExchanges(null);
    String uid = promptForExchangeIdFromList(infos);
    DerFacilityExchangeInfo exchange = StreamSupport.stream(infos.spliterator(), false)
        .filter(e -> uid.equals(e.getUid())).findAny().orElse(null);
    if (exchange == null) {
      return;
    }
    if (shell.confirm(messageSource.getMessage("exchange.register.confirm.ask",
        new Object[] { exchange.getName(), exchange.getEndpointUri() }, Locale.getDefault()))) {
      shell.printSuccess(messageSource.getMessage("exchange.register.success",
          new Object[] { exchange.getName() }, Locale.getDefault()));
      shell.print("");
      shell.print(messageSource.getMessage("reg.form.intro", new Object[] { exchange.getName() },
          Locale.getDefault()));
      shell.print("");

      DerFacilityRegistrationForm regForm = exchangeRegistrationService
          .getExchangeRegistrationForm(exchange, Locale.getDefault());
      handleRegisterForm(exchange, regForm);
    }
  }

  /**
   * List registrations.
   */
  @ShellMethod("List pending registrations.")
  public void exchangeRegistrationList() {
    Iterable<ExchangeRegistrationEntity> list = exchangeRegistrationService
        .listExchangeRegistrations();
    int count = 0;
    for (ExchangeRegistrationEntity reg : list) {
      count++;
      shell.print(messageSource.getMessage("reg.list.item.title",
          new Object[] { count, reg.getId(), reg.getExchangeEndpointUri() }, Locale.getDefault()),
          PromptColor.MAGENTA);
      shell.print(getFaint(messageSource.getMessage("reg.list.item.caption",
          new Object[] { reg.getCreated() }, Locale.getDefault())));
    }
    shell.print(
        messageSource.getMessage("reg.list.count", new Object[] { count }, Locale.getDefault()));
  }

  /**
   * Handle a registration completed event.
   * 
   * <p>
   * This will print a status message to the shell.
   * </p>
   * 
   * @param event
   *        the event
   */
  @Async
  @EventListener
  public void handleExchangeRegistrationCompletedEvent(ExchangeRegistrationCompleted event) {
    ExchangeRegistrationEntity reg = event.getExchangeRegistration();
    String armor = messageSource.getMessage("reg.event.armor", null, Locale.getDefault());
    String msg = String.format("\n%s\n%s\n%s\n", armor,
        wrap(
            messageSource.getMessage(
                event.isSuccess() ? "reg.event.completed.success" : "reg.event.completed.error",
                new Object[] { reg.getId(), reg.getExchangeEndpointUri() }, Locale.getDefault()),
            ShellUtils.SHELL_MAX_COLS),
        armor);

    // broadcast message to all available registered terminals
    wall(shell.getColored(msg, event.isSuccess() ? PromptColor.GREEN : PromptColor.RED));
  }

  private void listExchanges() {
    Iterable<DerFacilityExchangeInfo> infos = exchangeRegistrationService.listExchanges(null);
    showNumberedObjectList(infos, "exchange.list.item", "uid", new String[] { "name" }, (k, v) -> {
      showRegistry(v);
      shell.print("");
    });
  }

  private String promptForExchangeIdFromList(Iterable<DerFacilityExchangeInfo> infos) {
    return promptForNumberedObjectListItem(infos, "exchange.list", "uid", new String[] { "name" },
        (k, v) -> {
          showRegistry(v);
          shell.print("");
        });
  }

  private void showRegistry(DerFacilityExchangeInfo info) {
    String fmt = "  %-10s %s";
    shell.print(format(fmt, messageSource.getMessage("exchange.name", null, Locale.getDefault()),
        info.getName()));
    shell.print(format(fmt, messageSource.getMessage("exchange.uid", null, Locale.getDefault()),
        info.getUid()));
    shell.print(format(fmt, messageSource.getMessage("exchange.uri", null, Locale.getDefault()),
        info.getEndpointUri()));
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
            shell.printSuccess(wrap(
                messageSource.getMessage("reg.success",
                    new Object[] { exchange.getName(), reg.getId() }, Locale.getDefault()),
                ShellUtils.SHELL_MAX_COLS));
            shell.print("");
          } catch (IllegalArgumentException e) {
            // there was some error submitting the form; allow the user the chance to re-try
            shell.printError(e.getMessage());
            if (shell.confirm(
                messageSource.getMessage("reg.form.tryAgain", null, Locale.getDefault()))) {
              shell.printError(
                  wrap(messageSource.getMessage("reg.error.tryAgain", null, Locale.getDefault()),
                      ShellUtils.SHELL_MAX_COLS));
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
          "reg.form.field.display", new Object[] { getBold(String.valueOf(i)),
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
    shell.printInfo(wrap(field.getCaption(), ShellUtils.SHELL_MAX_COLS));
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

}
