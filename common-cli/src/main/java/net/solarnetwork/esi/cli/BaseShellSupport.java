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

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

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
   * Scale a number by a power of 10.
   * 
   * @param num
   *        the number to scale
   * @param scale
   *        the power of 10 to scale by; a negative value shifts the decimal point left this many
   *        places; a positive value shifts the decimcal point right this many places
   * @return the scaled value
   */
  public static BigDecimal scaled(Number num, int scale) {
    if (num == null) {
      return null;
    }
    BigDecimal n = (num instanceof BigDecimal ? (BigDecimal) num : new BigDecimal(num.toString()));
    if (scale == 0) {
      return n;
    } else if (scale < 0) {
      return n.movePointLeft(-scale);
    } else {
      return n.movePointRight(scale);
    }
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
   * Prompt the shell to read a number value.
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

}
