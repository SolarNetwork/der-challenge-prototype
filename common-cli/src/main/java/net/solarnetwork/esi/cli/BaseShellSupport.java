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

package net.solarnetwork.esi.cli;

import static net.solarnetwork.esi.cli.ShellUtils.getBoldColored;
import static net.solarnetwork.esi.cli.ShellUtils.wall;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

import com.github.fonimus.ssh.shell.PromptColor;
import com.github.fonimus.ssh.shell.SshShellHelper;

/**
 * Base shell support class.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseShellSupport extends BaseMessageSourceSupport {

  /** The shell helper. */
  protected final SshShellHelper shell;

  /** A class-level logger. */
  protected final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Constructor.
   * 
   * @param shell
   *        the shell helper
   */
  public BaseShellSupport(SshShellHelper shell) {
    super(null);
    this.shell = shell;
    setMessageSource(defaultMessageSource(getClass()));
  }

  private static MessageSource defaultMessageSource(Class<?> clazz) {
    ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
    ms.setBasenames(BaseShellSupport.class.getName(), clazz.getName());
    return ms;
  }

  /**
   * Prompt the shell to read a number value.
   * 
   * @param propName
   *        the message key for the property to read a number value for
   * @param unit
   *        the desired unit of the number, or general qualification hint
   * @param currValue
   *        the current value, or {@literal null}
   * @param min
   *        a minimum required value, or {@literal null} for no minimum
   * @param max
   *        a maximum required value, or {@literal null} for no maximum
   * @return the answer
   */
  protected BigDecimal readNumber(String propName, String unit, BigDecimal currValue, Number min,
      Number max) {
    while (true) {
      String val = shell.read(messageSource.getMessage(
          "ask.qualified.property", new Object[] {
              messageSource.getMessage(propName, null, Locale.getDefault()), unit, currValue },
          Locale.getDefault()));
      if (val == null || val.trim().isEmpty()) {
        return currValue;
      }
      try {
        BigDecimal num = new BigDecimal(val);
        if ((min != null && num.doubleValue() < min.doubleValue())
            || (max != null && num.doubleValue() > max.doubleValue())) {
          shell.printError(messageSource.getMessage("answer.error.numOutOfRange",
              new Object[] { min, max }, Locale.getDefault()));
        } else {
          return num;
        }
      } catch (NumberFormatException e) {
        shell.printError(
            messageSource.getMessage("answer.error.enterNumber", null, Locale.getDefault()));
      }
    }
  }

  /**
   * Prompt the shell to read a string value.
   * 
   * @param propName
   *        the message key for the property to read a number value for
   * @param unit
   *        the desired unit of the number, or general qualification hint
   * @param currValue
   *        the current value, or {@literal null}
   * @return the answer
   */
  protected String readString(String propName, String unit, String currValue) {
    while (true) {
      String val = shell.read(messageSource.getMessage(
          "ask.qualified.property", new Object[] {
              messageSource.getMessage(propName, null, Locale.getDefault()), unit, currValue },
          Locale.getDefault()));
      if (val == null || val.trim().isEmpty()) {
        return currValue;
      }
      return val;
    }
  }

  /**
   * Show a numbered list of objects, returning a list of one of their property values.
   * 
   * <p>
   * This method renders a numbered list of items based on property values from a list of objects.
   * This can be used to render a list to pick an item from. The method returns a list of "key"
   * values from each object. Typically these would be a unique value, such as a primary key of an
   * entity, that can be used to uniquely identify each object.
   * </p>
   * 
   * @param <K>
   *        the item property value return type
   * @param <V>
   *        the item type
   * @param list
   *        the list of objects to show
   * @param itemCode
   *        the message key to use for each list item; will be passed a parameter list including the
   *        1-based list index and any {@code propName} values
   * @param keyPropName
   *        the property name to return the values of
   * @param propNames
   *        property names to include as list item message parameters
   * @param itemCallback
   *        an optional callback to invoke for each list item, after its message has been printed to
   *        the shell
   * @return the list of {@code keyPropName} property values
   */
  protected <K, V> List<K> showNumberedObjectList(Iterable<V> list, String itemCode,
      String keyPropName, String[] propNames, BiConsumer<Integer, V> itemCallback) {
    int idx = 0;
    List<K> ids = new ArrayList<>();
    for (V info : list) {
      idx++;
      PropertyAccessor bean = PropertyAccessorFactory.forBeanPropertyAccess(info);

      @SuppressWarnings("unchecked")
      K key = (K) bean.getPropertyValue(keyPropName);
      ids.add(key);

      Object[] params = new Object[(propNames != null ? propNames.length : 0) + 1];
      params[0] = idx;
      if (propNames != null) {
        for (int i = 0; i < propNames.length; i++) {
          Object val = null;
          try {
            val = bean.getPropertyValue(propNames[i]);
          } catch (BeansException e) {
            log.warn("Error reading bean property [{}] on [{}]: {}", propNames[i], info,
                e.toString());
          }
          params[i + 1] = val;
        }
      }
      String msg = messageSource.getMessage(itemCode, params, Locale.getDefault());
      if (itemCallback != null) {
        msg = ShellUtils.getBold(msg);
      }
      shell.print(msg);
      if (itemCallback != null) {
        itemCallback.accept(idx, info);
      }
    }
    return ids;
  }

  /**
   * Show a list of items and prompt to pick one of the, returning the chosen item's key.
   * 
   * @param <K>
   *        the item property value return type
   * @param <V>
   *        the item type
   * @param list
   *        the list of items to choose from
   * @param codePrefix
   *        a message code prefix to prepend to all prompt messages
   * @param keyPropName
   *        the item property name that provides the key value to use
   * @param propNames
   *        an optional additional list of item property names to pass to the item list message,
   *        after the list item's 1-based index
   * @param itemCallback
   *        an optional callback to invoke after each list item is printed
   * @return the key of the chosen item
   * @see #showNumberedObjectList(Iterable, String, String, String[], BiConsumer)
   */
  protected <K, V> K promptForNumberedObjectListItem(Iterable<V> list, String codePrefix,
      String keyPropName, String[] propNames, BiConsumer<Integer, V> itemCallback) {
    List<K> ids = showNumberedObjectList(list, codePrefix + ".item", keyPropName, propNames,
        itemCallback);
    if (ids.isEmpty()) {
      shell.printWarning(messageSource.getMessage(codePrefix + ".none", null, Locale.getDefault()));
      return null;
    }

    while (true) {
      String ans = shell
          .read(messageSource.getMessage(codePrefix + ".pick", null, Locale.getDefault()));
      try {
        int index = Integer.parseInt(ans) - 1;
        return ids.get(index);
      } catch (NumberFormatException e) {
        shell.printError(
            messageSource.getMessage("answer.error.enterNumber", null, Locale.getDefault()));
      } catch (IndexOutOfBoundsException e) {
        shell.printError(messageSource.getMessage("answer.error.numOutOfRange",
            new Object[] { 1, ids.size() }, Locale.getDefault()));
      }
    }
  }

  // @formatter:off
  private static final DateTimeFormatter BANNER_DATE_FORMATTER = new DateTimeFormatterBuilder()
      .append(DateTimeFormatter.ISO_LOCAL_DATE)
      .appendLiteral(' ')
      .append(DateTimeFormatter.ISO_LOCAL_TIME)
      .toFormatter();
  // @formatter:on

  /**
   * Broadcast a banner message to all attached shells.
   * 
   * @param msg
   *        the message to broadcast
   * @param color
   *        the color to use
   */
  public void wallBanner(String msg, PromptColor color) {
    String now = BANNER_DATE_FORMATTER.format(LocalDateTime.now());
    String armor = messageSource.getMessage("event.armor", null, Locale.getDefault());
    String headArmor = armor;
    if (armor.length() > (now.length() + 3)) {
      headArmor = getBoldColored(armor.substring(0, 3), color)
          + shell.getColored(now, PromptColor.BLACK)
          + getBoldColored(armor.substring(now.length() + 3), color);
    }
    String banner = String.format("\n%s\n%s\n%s", headArmor, shell.getColored(msg, color),
        getBoldColored(armor, color));

    // broadcast message to all available registered terminals
    wall(banner);

  }
}
